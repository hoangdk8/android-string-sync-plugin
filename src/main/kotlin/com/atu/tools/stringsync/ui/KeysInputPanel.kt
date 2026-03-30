package com.atu.tools.stringsync.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel

class KeysInputPanel : JPanel(BorderLayout()) {
    private val textArea = JBTextArea(8, 40)

    init {
        textArea.emptyText.text = "Dán danh sách key (cách nhau bằng dấu phẩy hoặc xuống dòng). Để trống để đồng bộ toàn bộ key từ sheet."
        val topPanel = JPanel()
        val clearButton = JButton("Xóa nội dung")
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
