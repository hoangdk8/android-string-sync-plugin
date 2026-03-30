package com.atu.tools.stringsync.ui

import com.atu.tools.stringsync.model.FileChangePreview
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class PreviewChangesDialog(changes: List<FileChangePreview>) : DialogWrapper(true) {

    private val panel = JPanel(BorderLayout())

    init {
        title = "String Sync Preview"
        val model = DefaultTableModel(
            arrayOf("Module", "Locale", "Key", "Action", "Old", "New", "File", "Message"),
            0
        )
        for (change in changes) {
            model.addRow(
                arrayOf(
                    change.moduleName,
                    change.locale,
                    change.key,
                    change.action.name,
                    change.oldValue ?: "",
                    change.newValue ?: "",
                    change.filePath,
                    change.message ?: ""
                )
            )
        }
        panel.add(JBScrollPane(JTable(model)), BorderLayout.CENTER)
        init()
    }

    override fun createCenterPanel(): JComponent = panel
}
