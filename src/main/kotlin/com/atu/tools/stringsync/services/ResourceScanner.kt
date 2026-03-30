package com.atu.tools.stringsync.services

import java.nio.file.Files
import java.nio.file.Path
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
