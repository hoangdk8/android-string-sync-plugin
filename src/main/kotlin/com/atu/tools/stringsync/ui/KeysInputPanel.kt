package com.atu.tools.stringsync.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

class KeysInputPanel : JPanel(BorderLayout()) {
    private val textArea = JBTextArea(8, 40)

    init {
        textArea.emptyText.text = "Paste keys (comma/newline separated). Leave empty to sync all keys from sheet"
        val topPanel = JPanel()
        val clearButton = JButton("Clear")
        clearButton.addActionListener { textArea.text = "" }
        topPanel.add(clearButton)

        add(topPanel, BorderLayout.NORTH)
        add(JBScrollPane(textArea), BorderLayout.CENTER)
    }

    fun text(): String = textArea.text ?: ""

    fun setText(value: String) {
        textArea.text = value
    }
}
