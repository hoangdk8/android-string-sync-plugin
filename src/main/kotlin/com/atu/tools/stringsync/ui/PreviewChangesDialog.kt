package com.atu.tools.stringsync.ui

import com.atu.tools.stringsync.model.FileChangePreview
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.RowFilter
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter
import kotlin.math.max

class PreviewChangesDialog(changes: List<FileChangePreview>) : DialogWrapper(true) {

    private val tableModel = object : DefaultTableModel(
        arrayOf("Module", "Ngôn ngữ", "Key", "Hành động", "Giá trị cũ", "Giá trị mới", "File", "Ghi chú"),
        0
    ) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }

    private val table = JTable(tableModel)
    private val sorter = TableRowSorter(tableModel)
    private val searchField = JBTextField()
    private val moduleFilter = JComboBox<FilterOption>()
    private val languageFilter = JComboBox<FilterOption>()
    private val actionFilter = JComboBox<FilterOption>()
    private val fileFilter = JComboBox<FilterOption>()
    private val noteFilter = JComboBox<FilterOption>()
    private val resultLabel = JLabel()
    private val panel = JPanel(BorderLayout(8, 8))

    init {
        title = "Xem trước thay đổi String"
        table.rowSorter = sorter
        table.fillsViewportHeight = true
        table.autoResizeMode = JTable.AUTO_RESIZE_OFF
        table.preferredScrollableViewportSize = Dimension(1500, 520)

        changes.forEach { change ->
            tableModel.addRow(
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

        configureFilters(changes)
        panel.add(buildFilterPanel(), BorderLayout.NORTH)
        panel.add(JBScrollPane(table), BorderLayout.CENTER)
        panel.add(buildFooterPanel(), BorderLayout.SOUTH)

        refreshFilter()
        init()
    }

    override fun createCenterPanel(): JComponent = panel

    private fun buildFilterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints().apply {
            insets = Insets(4, 4, 4, 4)
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
            gridy = 0
        }

        addFilterRow(panel, constraints, 0, "Tìm kiếm", searchField)
        addFilterRow(panel, constraints, 1, "Module", moduleFilter)
        addFilterRow(panel, constraints, 2, "Ngôn ngữ", languageFilter)
        addFilterRow(panel, constraints, 3, "Hành động", actionFilter)
        addFilterRow(panel, constraints, 4, "File", fileFilter)
        addFilterRow(panel, constraints, 5, "Ghi chú", noteFilter)

        return panel
    }

    private fun buildFooterPanel(): JComponent {
        val footer = JPanel(BorderLayout())
        footer.add(resultLabel, BorderLayout.WEST)
        return footer
    }

    private fun addFilterRow(
        parent: JPanel,
        baseConstraints: GridBagConstraints,
        row: Int,
        label: String,
        component: JComponent
    ) {
        val labelConstraints = baseConstraints.clone() as GridBagConstraints
        labelConstraints.gridx = 0
        labelConstraints.gridy = row
        labelConstraints.weightx = 0.0
        parent.add(JLabel(label), labelConstraints)

        val fieldConstraints = baseConstraints.clone() as GridBagConstraints
        fieldConstraints.gridx = 1
        fieldConstraints.gridy = row
        fieldConstraints.weightx = 1.0
        parent.add(component, fieldConstraints)
    }

    private fun configureFilters(changes: List<FileChangePreview>) {
        moduleFilter.setOptions(changes.map { it.moduleName })
        languageFilter.setOptions(changes.map { it.locale })
        actionFilter.setOptions(changes.map { it.action.label })
        fileFilter.setOptions(changes.map { it.filePath })
        noteFilter.setOptions(changes.map { it.message.orEmpty() })

        moduleFilter.addActionListener { refreshFilter() }
        languageFilter.addActionListener { refreshFilter() }
        actionFilter.addActionListener { refreshFilter() }
        fileFilter.addActionListener { refreshFilter() }
        noteFilter.addActionListener { refreshFilter() }
        searchField.document.addDocumentListener(SimpleDocumentListener { refreshFilter() })
    }

    private fun refreshFilter() {
        val keyword = searchField.text.trim()
        val selectedModule = moduleFilter.selectedOptionOrNull()
        val selectedLanguage = languageFilter.selectedOptionOrNull()
        val selectedAction = actionFilter.selectedOptionOrNull()
        val selectedFile = fileFilter.selectedOptionOrNull()
        val selectedNote = noteFilter.selectedOptionOrNull()

        sorter.rowFilter = object : RowFilter<DefaultTableModel, Int>() {
            override fun include(entry: Entry<out DefaultTableModel, out Int>): Boolean {
                if (selectedModule != null && entry.getStringValue(COL_MODULE) != selectedModule) return false
                if (selectedLanguage != null && entry.getStringValue(COL_LANGUAGE) != selectedLanguage) return false
                if (selectedAction != null && entry.getStringValue(COL_ACTION) != selectedAction) return false
                if (selectedFile != null && entry.getStringValue(COL_FILE) != selectedFile) return false
                if (selectedNote != null && entry.getStringValue(COL_NOTE) != selectedNote) return false
                if (keyword.isNotEmpty() && !matchesKeyword(entry, keyword)) return false
                return true
            }
        }

        resultLabel.text = "Hiển thị ${table.rowCount} / ${tableModel.rowCount} dòng"
    }

    private fun matchesKeyword(entry: RowFilter.Entry<out DefaultTableModel, out Int>, keyword: String): Boolean {
        val normalizedKeyword = keyword.lowercase()
        for (column in 0 until entry.valueCount) {
            if (entry.getStringValue(column).lowercase().contains(normalizedKeyword)) return true
        }
        return false
    }

    private fun JComboBox<FilterOption>.setOptions(values: List<String>) {
        removeAllItems()
        addItem(FilterOption("Tất cả", null))
        values
            .asSequence()
            .map { it.ifBlank { "(Trống)" } }
            .distinct()
            .sortedWith(String.CASE_INSENSITIVE_ORDER)
            .forEach { addItem(FilterOption(it, if (it == "(Trống)") "" else it)) }
        selectedIndex = 0
    }

    private fun JComboBox<FilterOption>.selectedOptionOrNull(): String? {
        val option = selectedItem as? FilterOption ?: return null
        return option.value
    }

    private data class FilterOption(
        val label: String,
        val value: String?
    ) {
        override fun toString(): String = label
    }

    private companion object {
        private const val COL_MODULE = 0
        private const val COL_LANGUAGE = 1
        private const val COL_ACTION = 3
        private const val COL_FILE = 6
        private const val COL_NOTE = 7
    }
}
