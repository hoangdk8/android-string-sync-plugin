package com.atu.tools.stringsync.util

object XmlEscaper {
    fun escape(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "\\'")
    }

    fun hasInjectionPattern(value: String): Boolean {
        val lower = value.lowercase()
        return lower.contains("<string") || lower.contains("</resources>")
    }
}
