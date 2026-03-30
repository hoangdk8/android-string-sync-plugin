package com.atu.tools.stringsync.model

data class SyncResult(
    val changes: List<FileChangePreview>,
    val errors: List<String>,
    val filesChanged: Int,
    val keysAdded: Int,
    val keysUpdated: Int,
    val skipped: Int
)
