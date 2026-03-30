package com.atu.tools.stringsync.util

object LocaleMapper {
    fun toValuesFolder(localeCode: String, baseLocale: String?): String {
        if (baseLocale != null && localeCode.equals(baseLocale, ignoreCase = true)) return "values"
        if (localeCode.startsWith("en", ignoreCase = true) && baseLocale == null) return "values"
        val language = localeCode.substringBefore('-').lowercase()
        return if (language == "en") "values" else "values-$language"
    }
}
