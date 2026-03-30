package com.atu.tools.stringsync.model

data class ModuleTarget(
    val moduleName: String,
    val resDirectories: List<String>,
    val existingLocales: Set<String>
)
