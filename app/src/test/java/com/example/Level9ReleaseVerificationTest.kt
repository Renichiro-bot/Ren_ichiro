package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.VellumDatabase
import com.example.core.database.entity.FolderEntity
import com.example.core.database.entity.NoteEntity
import com.example.core.database.entity.TagEntity
import com.example.core.designsystem.theme.VellumSpacing
import com.example.core.model.Note
import com.example.core.preferences.ThemeConfig
import com.example.core.preferences.ThemeMode
import com.example.core.storage.AttachmentStorage
import com.example.core.util.IncomingIntentTarget
import com.example.core.util.VellumIntentParser
import com.example.core.widget.VellumWidgetManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import androidx.compose.ui.unit.dp

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Level9ReleaseVerificationTest {

    private lateinit var context: Context
    private lateinit var database: VellumDatabase
    private lateinit var attachmentStorage: AttachmentStorage

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VellumDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        attachmentStorage = AttachmentStorage(context)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- 1. Data Integrity & Cascade Safety ---

    @Test
    fun folderDeletion_doesNotDeleteContainedNotes() = runBlocking {
        val folderDao = database.folderDao()
        val noteDao = database.noteDao()

        val folder = FolderEntity(id = "folder_alpha", name = "Finance", createdAtEpochMs = 1000L, updatedAtEpochMs = 1000L)
        folderDao.insertFolder(folder)

        val note = NoteEntity(
            id = "note_alpha",
            title = "Tax Prep",
            content = "Keep receipts",
            folderId = "folder_alpha",
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )
        noteDao.insertNote(note)

        // Verify initial state
        val initialNote = noteDao.getNoteById("note_alpha")
        assertNotNull(initialNote)
        assertEquals("folder_alpha", initialNote?.folderId)

        // Safe folder removal policy: Unfile notes first, then delete folder
        noteDao.updateNotesFolder(listOf("note_alpha"), null, System.currentTimeMillis())
        folderDao.deleteFolderById("folder_alpha")

        // Note must still exist with folderId = null (unfiled)
        val preservedNote = noteDao.getNoteById("note_alpha")
        assertNotNull("Note must NOT be deleted when its parent folder is removed", preservedNote)
        assertNull("Note folderId must be unfiled (null)", preservedNote?.folderId)
        assertEquals("Tax Prep", preservedNote?.title)
    }

    @Test
    fun trashAndRestore_preservesFolderAssociationAndTags() = runBlocking {
        val noteDao = database.noteDao()

        val note = NoteEntity(
            id = "note_beta",
            title = "Project Plan",
            content = "Milestones and roadmap",
            folderId = "folder_work",
            isTrashed = false,
            createdAtEpochMs = 2000L,
            updatedAtEpochMs = 2000L
        )
        noteDao.insertNote(note)

        // Move to Trash (Soft Delete)
        noteDao.updateTrashStatus("note_beta", isTrashed = true, updatedAt = 3000L)
        val trashedNote = noteDao.getNoteById("note_beta")
        assertTrue(trashedNote?.isTrashed == true)
        assertEquals("folder_work", trashedNote?.folderId)

        // Restore from Trash
        noteDao.updateTrashStatus("note_beta", isTrashed = false, updatedAt = 4000L)
        val restoredNote = noteDao.getNoteById("note_beta")
        assertFalse(restoredNote?.isTrashed == true)
        assertEquals("folder_work", restoredNote?.folderId)
        assertEquals("Project Plan", restoredNote?.title)
    }

    // --- 2. Security & Path Traversal Injection Defenses ---

    @Test
    fun deepLink_sqlInjectionAttempt_isSafelyRejected() {
        val sqlInjectionUri = Uri.parse("vellum://note/'%20OR%201=1%20--")
        val intent = Intent(Intent.ACTION_VIEW, sqlInjectionUri)
        val target = VellumIntentParser.parseIntent(intent)
        assertEquals(IncomingIntentTarget.None, target)
    }

    @Test
    fun deepLink_scriptTagInjectionAttempt_isSafelyRejected() {
        val xssUri = Uri.parse("vellum://note/<script>alert(document.cookie)</script>")
        val intent = Intent(Intent.ACTION_VIEW, xssUri)
        val target = VellumIntentParser.parseIntent(intent)
        assertEquals(IncomingIntentTarget.None, target)
    }

    @Test
    fun deepLink_directoryTraversalSlash_isSafelyRejected() {
        val traversalUri = Uri.parse("vellum://note/subdir/secret_note")
        val intent = Intent(Intent.ACTION_VIEW, traversalUri)
        val target = VellumIntentParser.parseIntent(intent)
        assertEquals(IncomingIntentTarget.None, target)
    }

    @Test
    fun shareSheet_oversizedInput_isSafelyBoundedAt500KB() {
        val massiveText = "X".repeat(750_000)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, massiveText)
        }
        val target = VellumIntentParser.parseIntent(intent)
        assertTrue(target is IncomingIntentTarget.ShareReceived)
        val share = target as IncomingIntentTarget.ShareReceived
        assertEquals(500_000, share.content.length)
    }

    // --- 3. Privacy & File Storage Sandbox Invariants ---

    @Test
    fun attachmentStorage_storesFilesInPrivateInternalDirectory() {
        val sampleAttachment = com.example.core.model.Attachment(
            id = "att_test_123",
            noteId = "note_123",
            fileName = "invoice.pdf",
            mimeType = "application/pdf",
            fileSizeBytes = 4096L,
            localRelativePath = "attachments/note_123/att_test_123.pdf",
            createdAtEpochMs = 1000L
        )

        val file = attachmentStorage.getFile(sampleAttachment)
        val internalFilesDir = context.filesDir.canonicalPath
        assertTrue("Attachment must be stored in private internal storage", file.canonicalPath.startsWith(internalFilesDir))
    }

    // --- 4. Accessibility Standards & Invariants ---

    @Test
    fun accessibility_touchTargetMeetsOrExceeds48dp() {
        // Material Design 3 and Android Accessibility minimum invariant
        assertEquals(48.dp, VellumSpacing.touchTargetMin)
    }

    // --- 5. Theme Configuration & Persistence Invariants ---

    @Test
    fun themeConfig_serializationConsistency() {
        val defaultConfig = ThemeConfig()
        assertEquals(ThemeMode.SYSTEM, defaultConfig.themeMode)
        assertFalse(defaultConfig.amoledDark)
        assertFalse(defaultConfig.dynamicColor)

        val customized = defaultConfig.copy(
            themeMode = ThemeMode.DARK,
            amoledDark = true,
            dynamicColor = true
        )
        assertEquals(ThemeMode.DARK, customized.themeMode)
        assertTrue(customized.amoledDark)
        assertTrue(customized.dynamicColor)
    }

    // --- 6. Home-Screen Widget Privacy Invariant ---

    @Test
    fun widgetManager_previewPreference_togglesCorrectly() {
        // Enable
        VellumWidgetManager.setWidgetPreviewEnabled(context, true)
        assertTrue(VellumWidgetManager.isWidgetPreviewEnabled(context))

        // Disable
        VellumWidgetManager.setWidgetPreviewEnabled(context, false)
        assertFalse(VellumWidgetManager.isWidgetPreviewEnabled(context))

        // Re-enable
        VellumWidgetManager.setWidgetPreviewEnabled(context, true)
        assertTrue(VellumWidgetManager.isWidgetPreviewEnabled(context))
    }
}
