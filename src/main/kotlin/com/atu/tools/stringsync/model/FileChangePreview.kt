package com.atu.tools.stringsync.model

data class FileChangePreview(
    val moduleName: String,
    val locale: String,
    val key: String,
    val action: ChangeAction,
    val oldValue: String? = null,
    val newValue: String? = null,
    val filePath: String,
    val message: String? = null
)
