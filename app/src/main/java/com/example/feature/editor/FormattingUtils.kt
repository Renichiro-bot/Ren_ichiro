package com.example.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Utility functions for formatting Markdown in the Vellum note editor without
 * disrupting cursor positions or losing text context.
 */
object FormattingUtils {

    fun applyBold(current: TextFieldValue): TextFieldValue {
        return wrapOrInsert(current, prefix = "**", suffix = "**")
    }

    fun applyItalic(current: TextFieldValue): TextFieldValue {
        return wrapOrInsert(current, prefix = "*", suffix = "*")
    }

    fun applyHeading(current: TextFieldValue): TextFieldValue {
        return insertAtLineStart(current, prefix = "# ")
    }

    fun applyBulletList(current: TextFieldValue): TextFieldValue {
        return insertAtLineStart(current, prefix = "• ")
    }

    fun applyNumberedList(current: TextFieldValue): TextFieldValue {
        return insertAtLineStart(current, prefix = "1. ")
    }

    fun applyQuote(current: TextFieldValue): TextFieldValue {
        return insertAtLineStart(current, prefix = "> ")
    }

    fun applyCode(current: TextFieldValue): TextFieldValue {
        return wrapOrInsert(current, prefix = "`", suffix = "`")
    }

    private fun wrapOrInsert(current: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
        val text = current.text
        val selection = current.selection
        return if (!selection.collapsed) {
            val start = selection.min
            val end = selection.max
            val selectedText = text.substring(start, end)
            val newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
            val newCursor = start + prefix.length + selectedText.length + suffix.length
            TextFieldValue(text = newText, selection = TextRange(newCursor))
        } else {
            val cursor = selection.start
            val safeCursor = cursor.coerceIn(0, text.length)
            val newText = text.substring(0, safeCursor) + prefix + suffix + text.substring(safeCursor)
            val newCursor = safeCursor + prefix.length
            TextFieldValue(text = newText, selection = TextRange(newCursor))
        }
    }

    private fun insertAtLineStart(current: TextFieldValue, prefix: String): TextFieldValue {
        val text = current.text
        val cursor = current.selection.start
        val safeCursor = cursor.coerceIn(0, text.length)
        val lastNewLine = text.lastIndexOf('\n', (safeCursor - 1).coerceAtLeast(0))
        val lineStart = if (lastNewLine == -1 || safeCursor == 0) 0 else lastNewLine + 1
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        val newCursor = safeCursor + prefix.length
        return TextFieldValue(text = newText, selection = TextRange(newCursor))
    }
}
