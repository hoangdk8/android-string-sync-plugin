package com.atu.tools.stringsync.model

data class SheetPayload(
    val byLocale: Map<String, Map<String, String>>
) {
    fun locales(): Set<String> = byLocale.keys

    fun baseLocale(): String? {
        val english = byLocale.keys.firstOrNull { it.startsWith("en", ignoreCase = true) }
        return english ?: byLocale.keys.firstOrNull()
    }

    fun translation(locale: String, key: String): String? {
        val resolved = resolveLocale(locale) ?: return null
        return byLocale[resolved]?.get(key)
    }

    fun allKeys(): Set<String> = byLocale.values.flatMap { it.keys }.toSet()

    private fun resolveLocale(requested: String): String? {
        val normalizedRequested = normalizeLocale(requested)

        val exact = byLocale.keys.firstOrNull { normalizeLocale(it) == normalizedRequested }
        if (exact != null) return exact

        val requestedLang = normalizedRequested.substringBefore('-')
        return byLocale.keys.firstOrNull { normalizeLocale(it).substringBefore('-') == requestedLang }
    }

    private fun normalizeLocale(value: String): String {
        return value.trim().replace('_', '-').lowercase()
    }
}
