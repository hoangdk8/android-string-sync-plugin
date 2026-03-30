package com.atu.tools.stringsync.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.RowFilter
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableRowSorter

class LanguageSelectionPanel(
    private val onDetectExisting: () -> Unit
) : JPanel(BorderLayout()) {

    private data class LanguageRow(
        val code: String,
        var selected: Boolean
    )

    private class Model : AbstractTableModel() {
        private val rows = mutableListOf<LanguageRow>()

        fun setLanguages(items: List<String>) {
            rows.clear()
            rows += items.sorted().map { LanguageRow(it, selected = false) }
            fireTableDataChanged()
        }

        fun setSelected(codes: Set<String>) {
            rows.forEach { it.selected = codes.contains(it.code) }
            fireTableDataChanged()
        }

        fun selected(): Set<String> = rows.filter { it.selected }.map { it.code }.toSet()

        override fun getRowCount(): Int = rows.size

        override fun getColumnCount(): Int = 2

        override fun getColumnName(column: Int): String = if (column == 0) "Use" else "Language"

        override fun getColumnClass(columnIndex: Int): Class<*> = if (columnIndex == 0) java.lang.Boolean::class.java else String::class.java

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean = columnIndex == 0

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val row = rows[rowIndex]
            return if (columnIndex == 0) row.selected else row.code
        }

        override fun setValueAt(aValue: Any?, rowIndex: Int, columnIndex: Int) {
            if (columnIndex == 0 && aValue is Boolean) {
                rows[rowIndex].selected = aValue
                fireTableCellUpdated(rowIndex, 0)
            }
        }

        fun selectAll() {
            rows.forEach { it.selected = true }
            fireTableDataChanged()
        }

        fun deselectAll() {
            rows.forEach { it.selected = false }
            fireTableDataChanged()
        }
    }

    private val model = Model()
    private val table = JTable(model)
    private val sorter = TableRowSorter(model)
    private val searchField = JBTextField()

    init {
        table.rowSorter = sorter

        val topPanel = JPanel()
        val selectAllButton = JButton("Select all")
        val deselectAllButton = JButton("Deselect all")
        val detectExistingButton = JButton("Detect Existing")

        selectAllButton.addActionListener { model.selectAll() }
        deselectAllButton.addActionListener { model.deselectAll() }
        detectExistingButton.addActionListener { onDetectExisting() }

        searchField.emptyText.text = "Search language..."
        searchField.document.addDocumentListener(SimpleDocumentListener {
            val text = searchField.text.trim()
            sorter.rowFilter = if (text.isBlank()) null else RowFilter.regexFilter("(?i)$text", 1)
        })

        topPanel.add(searchField)
        topPanel.add(selectAllButton)
        topPanel.add(deselectAllButton)
        topPanel.add(detectExistingButton)

        add(topPanel, BorderLayout.NORTH)
        add(JBScrollPane(table), BorderLayout.CENTER)
    }

    fun setLanguages(languages: List<String>) = model.setLanguages(languages)

    fun setSelectedLocales(locales: Set<String>) = model.setSelected(locales)

    fun selectedLocales(): Set<String> = model.selected()
}
