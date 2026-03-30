package com.atu.tools.stringsync.ui

import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun interface SimpleDocumentListener : DocumentListener {
    fun textChanged(event: DocumentEvent)

    override fun insertUpdate(event: DocumentEvent) = textChanged(event)

    override fun removeUpdate(event: DocumentEvent) = textChanged(event)

    override fun changedUpdate(event: DocumentEvent) = textChanged(event)
}
