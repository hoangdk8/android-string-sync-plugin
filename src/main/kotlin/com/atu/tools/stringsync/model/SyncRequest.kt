package com.atu.tools.stringsync.model

data class SyncRequest(
    val sourceUrl: String,
    val modules: List<ModuleTarget>,
    val targetLocales: Set<String>,
    val keys: Set<String>,
    val mode: SyncMode,
    val payload: SheetPayload
)
