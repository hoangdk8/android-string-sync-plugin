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
        title = "Xem trước thay đổi String"
        val model = DefaultTableModel(
            arrayOf("Module", "Ngôn ngữ", "Key", "Hành động", "Giá trị cũ", "Giá trị mới", "File", "Ghi chú"),
            0
        )
        for (change in changes) {
            model.addRow(
                arrayOf(
                    change.moduleName,
                    change.locale,
                    change.key,
                    change.action.label,
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
