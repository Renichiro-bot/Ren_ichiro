package com.example.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class FormattingUtilsTest {

    @Test
    fun applyBold_withSelectedText_wrapsWithDoubleAsterisks() {
        // "Hello World", selecting "World"
        val initial = TextFieldValue(
            text = "Hello World",
            selection = TextRange(6, 11)
        )
        val result = FormattingUtils.applyBold(initial)
        assertEquals("Hello **World**", result.text)
        assertEquals(15, result.selection.start)
    }

    @Test
    fun applyBold_withCollapsedCursor_insertsAsterisksAndPlacesCursorInside() {
        val initial = TextFieldValue(
            text = "Hello ",
            selection = TextRange(6)
        )
        val result = FormattingUtils.applyBold(initial)
        assertEquals("Hello ****", result.text)
        assertEquals(8, result.selection.start) // cursor between ** and **
    }

    @Test
    fun applyItalic_withSelectedText_wrapsWithSingleAsterisk() {
        val initial = TextFieldValue(
            text = "Important Note",
            selection = TextRange(0, 9)
        )
        val result = FormattingUtils.applyItalic(initial)
        assertEquals("*Important* Note", result.text)
        assertEquals(11, result.selection.start)
    }

    @Test
    fun applyHeading_insertsHeadingAtLineStart() {
        val initial = TextFieldValue(
            text = "Section Title",
            selection = TextRange(5)
        )
        val result = FormattingUtils.applyHeading(initial)
        assertEquals("# Section Title", result.text)
    }

    @Test
    fun applyBulletList_insertsBulletAtLineStart() {
        val initial = TextFieldValue(
            text = "First item\nSecond item",
            selection = TextRange(14) // inside Second item
        )
        val result = FormattingUtils.applyBulletList(initial)
        assertEquals("First item\n• Second item", result.text)
    }

    @Test
    fun applyNumberedList_insertsNumberAtLineStart() {
        val initial = TextFieldValue(
            text = "Draft item",
            selection = TextRange(3)
        )
        val result = FormattingUtils.applyNumberedList(initial)
        assertEquals("1. Draft item", result.text)
    }

    @Test
    fun applyQuote_insertsBlockquoteAtLineStart() {
        val initial = TextFieldValue(
            text = "A wise saying",
            selection = TextRange(2)
        )
        val result = FormattingUtils.applyQuote(initial)
        assertEquals("> A wise saying", result.text)
    }

    @Test
    fun applyCode_wrapsSelectedCodeWithBackticks() {
        val initial = TextFieldValue(
            text = "val x = 42",
            selection = TextRange(4, 5) // "x"
        )
        val result = FormattingUtils.applyCode(initial)
        assertEquals("val `x` = 42", result.text)
    }
}
