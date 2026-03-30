package com.atu.tools.stringsync.util

object PlaceholderValidator {
    private val placeholderRegex = Regex("%(?:\\d+\\$)?[sdif]")

    fun placeholders(value: String): Set<String> = placeholderRegex.findAll(value).map { it.value }.toSet()

    fun isCompatible(base: String, target: String): Boolean = placeholders(base) == placeholders(target)
}
