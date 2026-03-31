package com.atu.tools.stringsync.util

object XmlEscaper {
    fun escape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .let(::escapeUnescapedApostrophes)
    }

    private fun escapeUnescapedApostrophes(value: String): String {
        val out = StringBuilder(value.length + 8)
        for (i in value.indices) {
            val c = value[i]
            if (c == '\'') {
                var backslashes = 0
                var j = i - 1
                while (j >= 0 && value[j] == '\\') {
                    backslashes++
                    j--
                }
                if (backslashes % 2 == 0) {
                    out.append('\\')
                }
            }
            out.append(c)
        }
        return out.toString()
    }

    fun hasInjectionPattern(value: String): Boolean {
        val lower = value.lowercase()
        return lower.contains("<string") || lower.contains("</resources>")
    }
}
