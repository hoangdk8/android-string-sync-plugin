package com.atu.tools.stringsync.model

enum class SyncMode(val uiLabel: String) {
    ADD_OR_UPDATE("Add / Update selected keys"),
    ADD_ONLY_MISSING("Add only missing keys"),
    PREVIEW_ONLY("Preview only (no write)");

    override fun toString(): String = uiLabel
}
