package com.atu.tools.stringsync.services

import com.atu.tools.stringsync.util.XmlEscaper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class StringXmlWriter {

    fun write(file: Path, values: Map<String, String>) {
        Files.createDirectories(file.parent)
        val sorted = values.toSortedMap()
        val xml = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            appendLine("<resources>")
            for ((key, value) in sorted) {
                append("    <string name=\"")
                append(key)
                append("\">")
                append(XmlEscaper.escape(value))
                appendLine("</string>")
            }
            appendLine("</resources>")
        }
        Files.writeString(file, xml, StandardCharsets.UTF_8)
    }
}
