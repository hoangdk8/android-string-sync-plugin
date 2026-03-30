package com.atu.tools.stringsync.services

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.exists
import kotlin.io.path.name
import kotlin.streams.toList

class ResourceScanner {

    fun detectLocales(resDir: Path): Set<String> {
        if (!resDir.exists() || !Files.isDirectory(resDir)) return emptySet()
        return Files.list(resDir).use { stream ->
            stream.toList()
                .filter { Files.isDirectory(it) && it.name.startsWith("values") }
                .mapNotNull { folder ->
                    val stringsFile = folder.resolve("strings.xml")
                    if (!stringsFile.exists() || !stringsFile.isRegularFile()) return@mapNotNull null
                    val content = runCatching { Files.readString(stringsFile) }.getOrNull() ?: return@mapNotNull null
                    if (!content.contains("<string")) return@mapNotNull null

                    val folderName = folder.name
                    when {
                        folderName == "values" -> "en"
                        folderName.startsWith("values-") -> folderName.removePrefix("values-")
                        else -> null
                    }
                }
                .toList()
                .toSet()
        }
    }
}
