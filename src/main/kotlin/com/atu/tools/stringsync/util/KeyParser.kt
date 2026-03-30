package com.atu.tools.stringsync.util

object KeyParser {
    private val separators = Regex("[\\n,;]+")

    fun parse(input: String): Set<String> {
        return input
            .split(separators)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }
}
