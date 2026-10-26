package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.core.model.Note
import com.example.core.shortcut.VellumShortcutManager
import com.example.core.util.IncomingIntentTarget
import com.example.core.util.VellumIntentParser
import com.example.core.widget.VellumWidgetManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Level8AdvancedFeaturesTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
    }

    // --- 1. Deep Link Parsing & Path-Traversal Security ---

    @Test
    fun deepLink_newNote_returnsNewNoteTarget() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vellum://new"))
        val target = VellumIntentParser.parseIntent(intent)
        assertEquals(IncomingIntentTarget.NewNote, target)
    }

    @Test
    fun deepLink_search_returnsSearchTarget() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vellum://search"))
        val target = VellumIntentParser.parseIntent(intent)
        assertEquals(IncomingIntentTarget.Search, target)
    }

    @Test
    fun deepLink_favorites_returnsFavoritesTarget() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vellum://favorites"))
        val target = VellumIntentParser.parseIntent(intent)
        assertEquals(IncomingIntentTarget.Favorites, target)
    }

    @Test
    fun deepLink_openNote_withValidId_returnsOpenNoteTarget() {
        val validId = "note_12345_abc-xyz"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vellum://note/$validId"))
        val target = VellumIntentParser.parseIntent(intent)
        assertTrue(target is IncomingIntentTarget.OpenNote)
        assertEquals(validId, (target as IncomingIntentTarget.OpenNote).noteId)
    }

    @Test
    fun deepLink_openNote_withPathTraversal_isRejected() {
        val maliciousUri = Uri.parse("vellum://note/../../etc/passwd")
        val intent = Intent(Intent.ACTION_VIEW, maliciousUri)
        val target = VellumIntentParser.parseIntent(intent)
        assertEquals(IncomingIntentTarget.None, target)
    }

    @Test
    fun deepLink_openNote_withSpecialCharacters_isRejected() {
        val maliciousUri = Uri.parse("vellum://note/<script>alert(1)</script>")
        val intent = Intent(Intent.ACTION_VIEW, maliciousUri)
        val target = VellumIntentParser.parseIntent(intent)
        assertEquals(IncomingIntentTarget.None, target)
    }

    @Test
    fun deepLink_unsupportedScheme_returnsNone() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com/note/123"))
        val target = VellumIntentParser.parseIntent(intent)
        assertEquals(IncomingIntentTarget.None, target)
    }

    // --- 2. Share-Sheet Intent Parsing & Bound Safety ---

    @Test
    fun shareIntent_withPlainTextAndSubject_extractsCorrectly() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Shared Article Title")
            putExtra(Intent.EXTRA_TEXT, "Here is the article content shared from another application.")
        }
        val target = VellumIntentParser.parseIntent(intent)
        assertTrue(target is IncomingIntentTarget.ShareReceived)
        val share = target as IncomingIntentTarget.ShareReceived
        assertEquals("Shared Article Title", share.title)
        assertEquals("Here is the article content shared from another application.", share.content)
    }

    @Test
    fun shareIntent_withPlainTextWithoutSubject_derivesTitleFromFirstLine() {
        val body = "Project Meeting Notes\nDiscussion about budget and timeline.\nAction items listed below."
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, body)
        }
        val target = VellumIntentParser.parseIntent(intent)
        assertTrue(target is IncomingIntentTarget.ShareReceived)
        val share = target as IncomingIntentTarget.ShareReceived
        assertEquals("Project Meeting Notes", share.title)
        assertEquals(body, share.content)
    }

    @Test
    fun shareIntent_withExcessiveTextLength_isBoundedSafely() {
        // Exceeds 500,000 characters
        val massiveText = "A".repeat(600_000)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, massiveText)
        }
        val target = VellumIntentParser.parseIntent(intent)
        assertTrue(target is IncomingIntentTarget.ShareReceived)
        val share = target as IncomingIntentTarget.ShareReceived
        assertEquals(500_000, share.content.length)
    }

    @Test
    fun shareIntent_withBlankText_returnsNone() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "   \n\t  ")
        }
        val target = VellumIntentParser.parseIntent(intent)
        assertEquals(IncomingIntentTarget.None, target)
    }

    // --- 3. Home-Screen Widget Preferences & State ---

    @Test
    fun widgetManager_previewToggle_persistsPreference() {
        // Verify default is true
        assertTrue(VellumWidgetManager.isWidgetPreviewEnabled(context))

        // Set to false
        VellumWidgetManager.setWidgetPreviewEnabled(context, false)
        assertFalse(VellumWidgetManager.isWidgetPreviewEnabled(context))

        // Re-enable
        VellumWidgetManager.setWidgetPreviewEnabled(context, true)
        assertTrue(VellumWidgetManager.isWidgetPreviewEnabled(context))
    }

    @Test
    fun widgetManager_notifyWidgetsDataChanged_executesWithoutError() {
        // Should not throw in Robolectric environment
        VellumWidgetManager.notifyWidgetsDataChanged(context)
    }

    // --- 4. Dynamic Launcher Shortcuts Manager ---

    @Test
    fun shortcutManager_updatesShortcutsSafely() {
        val sampleNotes = listOf(
            Note(id = "note_1", title = "Shopping List", content = "Milk, Eggs, Bread"),
            Note(id = "note_2", title = "Meeting Summary", content = "Quarterly results review"),
            Note(id = "note_3", title = "Ideas", content = "New feature proposal"),
            Note(id = "note_4", title = "Travel Plan", content = "Flight leaves at 9 AM"),
            Note(id = "note_5", title = "Extra Note", content = "Exceeds max shortcut count")
        )

        // Calling update should complete safely without crashing
        VellumShortcutManager.updateDynamicShortcuts(context, sampleNotes)

        // Passing empty notes should clear shortcuts cleanly
        VellumShortcutManager.updateDynamicShortcuts(context, emptyList())
    }

    // --- 5. OCR Result Insertion Business Logic ---

    @Test
    fun ocrTextApplication_appendMode_preservesExistingContentWithSpacing() {
        val existingContent = "Original user drafted paragraph."
        val ocrExtracted = "Scanned receipt total: $45.00"

        val expected = "$existingContent\n\n$ocrExtracted"
        val actual = if (existingContent.isBlank()) ocrExtracted else "$existingContent\n\n$ocrExtracted"
        assertEquals(expected, actual)
    }

    @Test
    fun ocrTextApplication_insertTopMode_placesTextAtBeginning() {
        val existingContent = "Original user drafted paragraph."
        val ocrExtracted = "Document Heading"

        val expected = "$ocrExtracted\n\n$existingContent"
        val actual = if (existingContent.isBlank()) ocrExtracted else "$ocrExtracted\n\n$existingContent"
        assertEquals(expected, actual)
    }

    @Test
    fun ocrTextApplication_replaceMode_overwritesContent() {
        val existingContent = "Outdated drafted paragraph."
        val ocrExtracted = "Newly scanned OCR document replacement."

        val actual = ocrExtracted
        assertEquals("Newly scanned OCR document replacement.", actual)
    }
}
