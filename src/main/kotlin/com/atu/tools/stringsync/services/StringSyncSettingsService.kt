package com.atu.tools.stringsync.services

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros

@Service(Service.Level.PROJECT)
@State(name = "StringSyncSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class StringSyncSettingsService : PersistentStateComponent<StringSyncSettingsService.State> {

    data class State(
        var lastUrl: String = "",
        var lastMode: String = "UPDATE_ALL",
        var lastKeys: String = "",
        var lastSelectedLocales: String = "",
        var lastSelectedModules: String = ""
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }
}
