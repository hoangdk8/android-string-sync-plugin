package com.atu.tools.stringsync.model

data class SheetPayload(
    val byLocale: Map<String, Map<String, String>>
) {
    fun locales(): Set<String> = byLocale.keys

    fun baseLocale(): String? {
        val english = byLocale.keys.firstOrNull { it.startsWith("en", ignoreCase = true) }
        return english ?: byLocale.keys.firstOrNull()
    }

    fun translation(locale: String, key: String): String? = byLocale[locale]?.get(key)

    fun allKeys(): Set<String> = byLocale.values.flatMap { it.keys }.toSet()
}
