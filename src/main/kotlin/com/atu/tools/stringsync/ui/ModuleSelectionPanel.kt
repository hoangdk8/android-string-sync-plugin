package com.atu.tools.stringsync.ui

import com.atu.tools.stringsync.model.ModuleTarget
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.AbstractTableModel

class ModuleSelectionPanel(
    private val onAutoDetect: () -> Unit
) : JPanel(BorderLayout()) {

    private data class ModuleRow(
        var selected: Boolean,
        val target: ModuleTarget
    )

    private class Model : AbstractTableModel() {
        private val rows = mutableListOf<ModuleRow>()

        fun setRows(items: List<ModuleTarget>) {
            rows.clear()
            rows += items.map { ModuleRow(selected = true, target = it) }
            fireTableDataChanged()
        }

        fun selectedModules(): List<ModuleTarget> = rows.filter { it.selected }.map { it.target }

        override fun getRowCount(): Int = rows.size

        override fun getColumnCount(): Int = 4

        override fun getColumnName(column: Int): String = when (column) {
            0 -> "Use"
            1 -> "Module"
            2 -> "Res directories"
            else -> "Existing locales"
        }

        override fun getColumnClass(columnIndex: Int): Class<*> = when (columnIndex) {
            0 -> java.lang.Boolean::class.java
            else -> String::class.java
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex == 0

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val row = rows[rowIndex]
            return when (columnIndex) {
                0 -> row.selected
                1 -> row.target.moduleName
                2 -> row.target.resDirectories.joinToString("; ")
                else -> row.target.existingLocales.sorted().joinToString(", ")
            }
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            if (columnIndex == 0 && aValue is Boolean) {
                rows[rowIndex].selected = aValue
                fireTableCellUpdated(rowIndex, columnIndex)
            }
        }

        fun deselectAll() {
            rows.forEach { it.selected = false }
            fireTableDataChanged()
        }

        fun clearAll() {
            rows.clear()
            fireTableDataChanged()
        }
    }

    private val model = Model()
    private val table = JTable(model)

    init {
        val buttonPanel = JPanel()
        val autoDetectButton = JButton("Auto-detect")
        val deselectButton = JButton("Deselect all")
        val removeAllButton = JButton("Remove all")

        autoDetectButton.addActionListener { onAutoDetect() }
        deselectButton.addActionListener { model.deselectAll() }
        removeAllButton.addActionListener { model.clearAll() }

        buttonPanel.add(autoDetectButton)
        buttonPanel.add(deselectButton)
        buttonPanel.add(removeAllButton)

        add(buttonPanel, BorderLayout.NORTH)
        add(JBScrollPane(table), BorderLayout.CENTER)
    }

    fun setModules(modules: List<ModuleTarget>) = model.setRows(modules)

    fun selectedModules(): List<ModuleTarget> = model.selectedModules()

    fun existingLocalesOfSelection(): Set<String> = selectedModules().flatMap { it.existingLocales }.toSet()
}
