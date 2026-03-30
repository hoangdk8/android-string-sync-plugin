package com.atu.tools.stringsync.actions

import com.atu.tools.stringsync.ui.StringSyncDialog
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent

class OpenStringSyncDialogAction : AnAction("String Sync from Google Sheet") {
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return
        StringSyncDialog(project).show()
    }
}
