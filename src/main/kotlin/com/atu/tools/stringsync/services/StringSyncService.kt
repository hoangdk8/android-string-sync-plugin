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
            ?: return SyncResult(emptyList(), listOf("Không xác định được ngôn ngữ gốc trong dữ liệu sheet"), 0, 0, 0, 0)

        val keys = if (request.keys.isEmpty()) request.payload.allKeys() else request.keys
        val changes = mutableListOf<FileChangePreview>()
        val errors = mutableListOf<String>()

        for (module in request.modules) {
            val moduleDefaultStrings = parseModuleDefaultStrings(module)
            for (locale in request.targetLocales) {
                val targetFile = resolveTargetFile(module, locale, baseLocale)
                if (targetFile == null) {
                    errors += "Không tìm thấy thư mục res cho module ${module.moduleName}"
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
                            message = "Key phải đúng định dạng ^[a-z0-9_]+$"
                        )
                        continue
                    }

                    val defaultBaseValue = moduleDefaultStrings[key]
                    val sheetBaseValue = request.payload.translation(baseLocale, key)
                    val baseValue = defaultBaseValue ?: sheetBaseValue
                    var newValue = request.payload.translation(locale, key)
                    var fallbackMessage: String? = null
                    if (newValue == null && baseValue != null) {
                        newValue = baseValue
                        fallbackMessage = if (defaultBaseValue != null) {
                            "Sheet thiếu bản dịch locale. Dùng fallback từ values/strings.xml của module."
                        } else {
                            "Sheet thiếu bản dịch locale. Dùng fallback từ ngôn ngữ gốc $baseLocale."
                        }
                    }
                    if (newValue == null) {
                        changes += FileChangePreview(
                            moduleName = module.moduleName,
                            locale = locale,
                            key = key,
                            action = ChangeAction.SKIP_MISSING_IN_SHEET,
                            filePath = targetFile.toString(),
                            message = "Sheet không có bản dịch cho locale này"
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
                            message = "Phát hiện nội dung có thể gây lỗi XML injection"
                        )
                        continue
                    }

                    if (baseValue != null && !PlaceholderValidator.isCompatible(baseValue, newValue)) {
                        changes += FileChangePreview(
                            moduleName = module.moduleName,
                            locale = locale,
                            key = key,
                            action = ChangeAction.ERROR_PLACEHOLDER_MISMATCH,
                            oldValue = baseValue,
                            newValue = newValue,
                            filePath = targetFile.toString(),
                            message = "Placeholder không khớp với ngôn ngữ gốc $baseLocale"
                        )
                        continue
                    }

                    val oldValue = existing[key]
                    val action = when (request.mode) {
                        SyncMode.ADD_ALL,
                        SyncMode.ADD_MISSING -> if (oldValue == null) ChangeAction.ADD else ChangeAction.SKIP_ALREADY_EXISTS

                        SyncMode.UPDATE_ALL -> when {
                            oldValue == null -> ChangeAction.ADD
                            oldValue != newValue -> ChangeAction.UPDATE
                            else -> ChangeAction.SKIP_ALREADY_EXISTS
                        }

                        SyncMode.UPDATE_CHANGED -> when {
                            oldValue == null -> ChangeAction.SKIP_NOT_EXISTS_FOR_UPDATE
                            oldValue != newValue -> ChangeAction.UPDATE
                            else -> ChangeAction.SKIP_ALREADY_EXISTS
                        }
                    }

                    changes += FileChangePreview(
                        moduleName = module.moduleName,
                        locale = locale,
                        key = key,
                        action = action,
                        oldValue = oldValue,
                        newValue = newValue,
                        filePath = targetFile.toString(),
                        message = fallbackMessage
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
                it.action == ChangeAction.SKIP_ALREADY_EXISTS ||
                    it.action == ChangeAction.SKIP_NOT_EXISTS_FOR_UPDATE ||
                    it.action == ChangeAction.SKIP_MISSING_IN_SHEET
            }
        )
    }

    fun apply(request: SyncRequest): SyncResult {
        val preview = preview(request)
        val extraErrors = mutableListOf<String>()
        val grouped = preview.changes.groupBy { it.filePath }
        for ((filePath, fileChanges) in grouped) {
            val targetPath = Path.of(filePath)
            val updates = linkedMapOf<String, String>()
            for (change in fileChanges) {
                if ((change.action == ChangeAction.ADD || change.action == ChangeAction.UPDATE) && change.newValue != null) {
                    updates[change.key] = change.newValue
                }
            }
            if (updates.isEmpty()) continue
            runCatching {
                writer.upsert(targetPath, updates)
            }.onFailure { t ->
                extraErrors += "Không thể ghi file ${targetPath.toAbsolutePath()}: ${t.message}"
            }
        }

        return preview.copy(errors = preview.errors + extraErrors)
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

    private fun parseModuleDefaultStrings(module: ModuleTarget): Map<String, String> {
        val defaultFile = module.resDirectories
            .asSequence()
            .map { Path.of(it).resolve("values").resolve("strings.xml") }
            .firstOrNull { it.exists() }
            ?: return emptyMap()
        return parser.parse(defaultFile)
    }
}
