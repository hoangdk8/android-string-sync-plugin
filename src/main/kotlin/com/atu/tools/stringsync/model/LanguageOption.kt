package com.atu.tools.stringsync.model

data class LanguageOption(
    val code: String,
    val name: String,
    val aliases: Set<String> = emptySet()
)
