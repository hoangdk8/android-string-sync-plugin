package com.atu.tools.stringsync.services

import com.atu.tools.stringsync.model.ChangeAction
import com.atu.tools.stringsync.model.FileChangePreview
import com.atu.tools.stringsync.model.ModuleTarget
import com.atu.tools.stringsync.model.SyncMode
import com.atu.tools.stringsync.model.SyncRequest
import com.atu.tools.stringsync.model.SyncResult
import com.atu.tools.stringsync.util.LocaleMapper
import com.atu.tools.stringsync.util.PlaceholderValidator
import com.atu.tools.stringsync.util.XmlEscaper
import java.nio.file.Path
import kotlin.io.path.exists

class StringSyncService(
    private val parser: StringXmlParser = StringXmlParser(),
    private val writer: StringXmlWriter = StringXmlWriter()
) {
    private val keyRegex = Regex("^[a-z0-9_]+$")

    fun preview(request: SyncRequest): SyncResult {
        val baseLocale = request.payload.baseLocale()
            ?: return SyncResult(emptyList(), listOf("Cannot detect base locale in payload"), 0, 0, 0, 0)

        val keys = if (request.keys.isEmpty()) request.payload.allKeys() else request.keys
        val changes = mutableListOf<FileChangePreview>()
        val errors = mutableListOf<String>()

        for (module in request.modules) {
            for (locale in request.targetLocales) {
                val targetFile = resolveTargetFile(module, locale, baseLocale)
                if (targetFile == null) {
                    errors += "No res directory available for module ${module.moduleName}"
                    continue
                }
                val existing = parser.parse(targetFile)
                for (key in keys) {
                    if (!keyRegex.matches(key)) {
                        changes += FileChangePreview(
                            moduleName = module.moduleName,
                            locale = locale,
                            key = key,
                            action = ChangeAction.ERROR_INVALID_KEY,
                            filePath = targetFile.toString(),
                            message = "Key must match ^[a-z0-9_]+$"
                        )
                        continue
                    }

                    val newValue = request.payload.translation(locale, key)
                    if (newValue == null) {
                        changes += FileChangePreview(
                            moduleName = module.moduleName,
                            locale = locale,
                            key = key,
                            action = ChangeAction.SKIP_MISSING_IN_SHEET,
                            filePath = targetFile.toString(),
                            message = "Missing translation in sheet for locale"
                        )
                        continue
                    }

                    if (XmlEscaper.hasInjectionPattern(newValue)) {
                        changes += FileChangePreview(
                            moduleName = module.moduleName,
                            locale = locale,
                            key = key,
                            action = ChangeAction.ERROR_XML_INJECTION,
                            filePath = targetFile.toString(),
                            message = "Potential XML injection pattern"
                        )
                        continue
                    }

                    val baseValue = request.payload.translation(baseLocale, key)
                    if (baseValue != null && !PlaceholderValidator.isCompatible(baseValue, newValue)) {
                        changes += FileChangePreview(
                            moduleName = module.moduleName,
                            locale = locale,
                            key = key,
                            action = ChangeAction.ERROR_PLACEHOLDER_MISMATCH,
                            oldValue = baseValue,
                            newValue = newValue,
                            filePath = targetFile.toString(),
                            message = "Placeholder mismatch with base locale $baseLocale"
                        )
                        continue
                    }

                    val oldValue = existing[key]
                    val action = when (request.mode) {
                        SyncMode.ADD_ONLY_MISSING -> {
                            if (oldValue == null) ChangeAction.ADD else ChangeAction.SKIP_ALREADY_EXISTS
                        }

                        SyncMode.ADD_OR_UPDATE,
                        SyncMode.PREVIEW_ONLY -> {
                            when {
                                oldValue == null -> ChangeAction.ADD
                                oldValue != newValue -> ChangeAction.UPDATE
                                else -> ChangeAction.SKIP_ALREADY_EXISTS
                            }
                        }
                    }

                    changes += FileChangePreview(
                        moduleName = module.moduleName,
                        locale = locale,
                        key = key,
                        action = action,
                        oldValue = oldValue,
                        newValue = newValue,
                        filePath = targetFile.toString()
                    )
                }
            }
        }

        return SyncResult(
            changes = changes,
            errors = errors,
            filesChanged = changes.filter { it.action == ChangeAction.ADD || it.action == ChangeAction.UPDATE }
                .map { it.filePath }
                .toSet()
                .size,
            keysAdded = changes.count { it.action == ChangeAction.ADD },
            keysUpdated = changes.count { it.action == ChangeAction.UPDATE },
            skipped = changes.count {
                it.action == ChangeAction.SKIP_ALREADY_EXISTS || it.action == ChangeAction.SKIP_MISSING_IN_SHEET
            }
        )
    }

    fun apply(request: SyncRequest): SyncResult {
        val preview = preview(request)
        if (request.mode == SyncMode.PREVIEW_ONLY) return preview

        val grouped = preview.changes.groupBy { it.filePath }
        for ((filePath, fileChanges) in grouped) {
            val targetPath = Path.of(filePath)
            val current = parser.parse(targetPath).toMutableMap()
            for (change in fileChanges) {
                when (change.action) {
                    ChangeAction.ADD,
                    ChangeAction.UPDATE -> {
                        if (change.newValue != null) {
                            current[change.key] = change.newValue
                        }
                    }

                    else -> Unit
                }
            }
            if (fileChanges.any { it.action == ChangeAction.ADD || it.action == ChangeAction.UPDATE }) {
                writer.write(targetPath, current)
            }
        }

        return preview
    }

    private fun resolveTargetFile(module: ModuleTarget, locale: String, baseLocale: String): Path? {
        if (module.resDirectories.isEmpty()) return null
        val folder = LocaleMapper.toValuesFolder(locale, baseLocale)
        val candidate = module.resDirectories
            .map { Path.of(it).resolve(folder).resolve("strings.xml") }
            .firstOrNull { it.exists() }
        if (candidate != null) return candidate
        return Path.of(module.resDirectories.first()).resolve(folder).resolve("strings.xml")
    }
}
