package com.atu.tools.stringsync.services

import com.atu.tools.stringsync.util.XmlEscaper
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

class StringXmlWriter {

    fun upsert(file: Path, updates: Map<String, String>) {
        if (updates.isEmpty()) return
        Files.createDirectories(file.parent)
        if (!Files.exists(file)) {
            val body = buildString {
                appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
                appendLine("<resources>")
                for ((key, value) in updates) {
                    append("    <string name=\"")
                    append(key)
                    append("\">")
                    append(XmlEscaper.escape(value))
                    appendLine("</string>")
                }
                appendLine("</resources>")
            }
            Files.writeString(file, body, StandardCharsets.UTF_8)
            return
        }

        var content = Files.readString(file, StandardCharsets.UTF_8)
        if (!content.contains("</resources>")) {
            error("File khong hop le (thieu </resources>): ${file.toAbsolutePath()}")
        }

        val missing = linkedMapOf<String, String>()
        for ((key, value) in updates) {
            val escapedValue = XmlEscaper.escape(value)
            val keyPattern = Regex.escape(key)
            val regex = Regex(
                "(<string\\b[^>]*\\bname\\s*=\\s*\"$keyPattern\"[^>]*>)(.*?)(</string>)",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            val match = regex.find(content)
            if (match != null) {
                val open = match.groupValues[1]
                val close = match.groupValues[3]
                val replacement = "$open$escapedValue$close"
                content = content.replaceRange(match.range, replacement)
            } else {
                missing[key] = escapedValue
            }
        }

        if (missing.isNotEmpty()) {
            val closingIndex = content.lastIndexOf("</resources>")
            val insertPrefix = if (closingIndex > 0 && content[closingIndex - 1] != '\n') "\n" else ""
            val appendBlock = buildString {
                append(insertPrefix)
                for ((key, escapedValue) in missing) {
                    append("    <string name=\"")
                    append(key)
                    append("\">")
                    append(escapedValue)
                    appendLine("</string>")
                }
            }
            content = content.substring(0, closingIndex) + appendBlock + content.substring(closingIndex)
        }

        Files.writeString(file, content, StandardCharsets.UTF_8)
    }
}
