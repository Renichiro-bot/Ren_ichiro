package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.repository.NoteRepository
import com.example.core.data.repository.NoteRepositoryImpl
import com.example.core.database.VellumDatabase
import com.example.core.model.Note
import com.example.core.model.NoteColorTheme
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Level5FolderTagTrashTest {

    private lateinit var database: VellumDatabase
    private lateinit var repository: NoteRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VellumDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NoteRepositoryImpl(
            noteDao = database.noteDao(),
            folderDao = database.folderDao(),
            tagDao = database.tagDao()
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // Scenario 1: Folder creation with valid name
    @Test
    fun scenario01_folderCreationWithValidName() = runBlocking {
        val result = repository.createFolder("Work Projects")
        assertTrue("Valid folder creation should succeed", result.isSuccess)
        val folder = result.getOrNull()
        assertNotNull(folder)
        assertEquals("Work Projects", folder?.name)

        val folders = repository.getAllFolders().first()
        assertTrue(folders.any { it.name == "Work Projects" })
    }

    // Scenario 2: Folder duplicate name rejection (case-insensitive)
    @Test
    fun scenario02_folderDuplicateNameRejection() = runBlocking {
        repository.createFolder("Personal")
        val duplicateResult = repository.createFolder("personal ")
        assertTrue("Duplicate folder name must fail", duplicateResult.isFailure)
        assertTrue(duplicateResult.exceptionOrNull()?.message?.contains("already exists") == true)
    }

    // Scenario 3: Folder blank/whitespace name rejection
    @Test
    fun scenario03_folderBlankNameRejection() = runBlocking {
        val blankResult = repository.createFolder("   ")
        assertTrue("Blank folder name must fail", blankResult.isFailure)
        assertTrue(blankResult.exceptionOrNull()?.message?.contains("empty") == true)
    }

    // Scenario 4: Folder renaming
    @Test
    fun scenario04_folderRenaming() = runBlocking {
        val created = repository.createFolder("Drafts").getOrThrow()
        val renameResult = repository.renameFolder(created.id, "Published")
        assertTrue(renameResult.isSuccess)

        val folders = repository.getAllFolders().first()
        assertTrue(folders.any { it.name == "Published" })
        assertFalse(folders.any { it.name == "Drafts" })
    }

    // Scenario 5: Move note into folder
    @Test
    fun scenario05_moveNoteIntoFolder() = runBlocking {
        val folder = repository.createFolder("Finance").getOrThrow()
        val note = Note(id = "note-f1", title = "Budget 2026", content = "Quarterly plan")
        repository.saveNote(note)

        repository.moveNoteToFolder(note.id, folder.id)
        val retrieved = repository.getNoteById(note.id)
        assertEquals(folder.id, retrieved?.folderId)
    }

    // Scenario 6: Move note out of folder
    @Test
    fun scenario06_moveNoteOutOfFolder() = runBlocking {
        val folder = repository.createFolder("Temp").getOrThrow()
        val note = Note(id = "note-f2", title = "Scratchpad", folderId = folder.id)
        repository.saveNote(note)

        repository.moveNoteToFolder(note.id, null)
        val retrieved = repository.getNoteById(note.id)
        assertNull("Note moved out of folder must have null folderId", retrieved?.folderId)
    }

    // Scenario 7: Delete folder safely (notes moved to unfiled, NEVER deleted)
    @Test
    fun scenario07_deleteFolderSafelyPreservesNotes() = runBlocking {
        val folder = repository.createFolder("Important").getOrThrow()
        val note1 = Note(id = "note-safe-1", title = "Contract", folderId = folder.id)
        val note2 = Note(id = "note-safe-2", title = "Receipt", folderId = folder.id)
        repository.saveNote(note1)
        repository.saveNote(note2)

        repository.deleteFolderSafely(folder.id)

        // Verify folder is gone
        val folders = repository.getAllFolders().first()
        assertFalse(folders.any { it.id == folder.id })

        // Verify notes STILL EXIST and have folderId = null
        val retrieved1 = repository.getNoteById("note-safe-1")
        val retrieved2 = repository.getNoteById("note-safe-2")
        assertNotNull("Note 1 must NOT be deleted", retrieved1)
        assertNotNull("Note 2 must NOT be deleted", retrieved2)
        assertNull("Note 1 must be moved to unfiled", retrieved1?.folderId)
        assertNull("Note 2 must be moved to unfiled", retrieved2?.folderId)
    }

    // Scenario 8: Empty folder state
    @Test
    fun scenario08_emptyFolderState() = runBlocking {
        val folder = repository.createFolder("Empty Folder").getOrThrow()
        val allNotes = repository.getActiveNotes().first()
        val folderNotes = allNotes.filter { it.folderId == folder.id }
        assertTrue("New folder must contain 0 notes initially", folderNotes.isEmpty())
    }

    // Scenario 9: Tag creation & assignment to note
    @Test
    fun scenario09_tagCreationAndAssignment() = runBlocking {
        val note = Note(id = "note-tag-1", title = "Architecture Idea", tags = listOf("architecture", "v5"))
        repository.saveNote(note)

        val retrieved = repository.getNoteById("note-tag-1")
        assertNotNull(retrieved)
        assertTrue(retrieved!!.tags.contains("architecture"))
        assertTrue(retrieved.tags.contains("v5"))

        val allTags = repository.getAllTags().first()
        assertTrue(allTags.any { it.name == "architecture" })
    }

    // Scenario 10: Tag removal from note
    @Test
    fun scenario10_tagRemovalFromNote() = runBlocking {
        val note = Note(id = "note-tag-2", title = "Tagged Note", tags = listOf("keep", "remove-me"))
        repository.saveNote(note)

        repository.removeTagFromNote("note-tag-2", "remove-me")
        val retrieved = repository.getNoteById("note-tag-2")
        assertNotNull(retrieved)
        assertTrue(retrieved!!.tags.contains("keep"))
        assertFalse(retrieved.tags.contains("remove-me"))
    }

    // Scenario 11: Tag filtering
    @Test
    fun scenario11_tagFiltering() = runBlocking {
        repository.saveNote(Note(id = "tag-n1", title = "Kotlin Note", tags = listOf("coding")))
        repository.saveNote(Note(id = "tag-n2", title = "Grocery Note", tags = listOf("shopping")))

        val activeNotes = repository.getActiveNotes().first()
        val codingNotes = activeNotes.filter { it.tags.any { tag -> tag.equals("coding", ignoreCase = true) } }
        assertEquals(1, codingNotes.size)
        assertEquals("Kotlin Note", codingNotes.first().title)
    }

    // Scenario 12: Tag deletion does not delete notes
    @Test
    fun scenario12_tagDeletionPreservesNotes() = runBlocking {
        repository.saveNote(Note(id = "tag-n3", title = "Design Sprint", tags = listOf("ux")))
        val allTags = repository.getAllTags().first()
        val uxTag = allTags.first { it.name == "ux" }

        repository.deleteTagSafely(uxTag.id)

        val activeNotes = repository.getActiveNotes().first()
        val note = activeNotes.find { it.id == "tag-n3" }
        assertNotNull("Note must remain in database after tag deletion", note)
    }

    // Scenario 13: Favorite toggle on note (independent of pin)
    @Test
    fun scenario13_favoriteToggleIndependentOfPin() = runBlocking {
        val note = Note(id = "fav-1", title = "Starred Note", isPinned = false, isFavorite = false)
        repository.saveNote(note)

        repository.toggleFavorite("fav-1")
        var retrieved = repository.getNoteById("fav-1")
        assertTrue(retrieved?.isFavorite == true)
        assertFalse(retrieved?.isPinned == true) // Pin remains unchanged

        repository.togglePin("fav-1")
        retrieved = repository.getNoteById("fav-1")
        assertTrue(retrieved?.isFavorite == true) // Favorite remains true
        assertTrue(retrieved?.isPinned == true)   // Pin is now true
    }

    // Scenario 14: Pinning maintains notes at top of list
    @Test
    fun scenario14_pinningMaintainsNotesAtTop() = runBlocking {
        val now = System.currentTimeMillis()
        repository.saveNote(Note(id = "unpinned-recent", title = "Unpinned Recent", isPinned = false, updatedAtEpochMs = now))
        repository.saveNote(Note(id = "pinned-older", title = "Pinned Older", isPinned = true, updatedAtEpochMs = now - 50000L))

        val activeNotes = repository.getActiveNotes().first()
        assertEquals("Pinned Older", activeNotes.first().title)
    }

    // Scenario 15: Archive note and unarchive note
    @Test
    fun scenario15_archiveAndUnarchive() = runBlocking {
        val note = Note(id = "arc-1", title = "Old Project")
        repository.saveNote(note)

        // Archive
        repository.archiveNote("arc-1")
        val activeNotesAfterArchive = repository.getActiveNotes().first()
        assertFalse("Archived note must not appear in active notes", activeNotesAfterArchive.any { it.id == "arc-1" })

        val archivedNotes = repository.getArchivedNotes().first()
        assertTrue("Archived note must appear in archived notes", archivedNotes.any { it.id == "arc-1" })

        // Unarchive
        repository.unarchiveNote("arc-1")
        val activeNotesAfterUnarchive = repository.getActiveNotes().first()
        assertTrue("Unarchived note must return to active notes", activeNotesAfterUnarchive.any { it.id == "arc-1" })
    }

    // Scenario 16: Move note to trash (soft delete), verify excluded from active notes
    @Test
    fun scenario16_softDeleteMovesToTrash() = runBlocking {
        val note = Note(id = "trash-1", title = "To Be Deleted")
        repository.saveNote(note)

        repository.deleteNote("trash-1")

        val activeNotes = repository.getActiveNotes().first()
        assertFalse("Trashed note must not appear in active notes", activeNotes.any { it.id == "trash-1" })

        val trashedNotes = repository.getTrashedNotes().first()
        assertTrue("Trashed note must appear in trash", trashedNotes.any { it.id == "trash-1" })
    }

    // Scenario 17: Restore note from trash & permanent delete note with empty trash
    @Test
    fun scenario17_restoreFromTrashAndEmptyTrash() = runBlocking {
        val note1 = Note(id = "t-1", title = "Restore Me")
        val note2 = Note(id = "t-2", title = "Permanently Delete Me")
        repository.saveNote(note1)
        repository.saveNote(note2)

        repository.deleteNote("t-1")
        repository.deleteNote("t-2")

        // 1. Restore note1
        repository.restoreNote("t-1")
        val activeNotes = repository.getActiveNotes().first()
        assertTrue("Restored note must return to active notes", activeNotes.any { it.id == "t-1" })

        // 2. Empty trash to permanently delete note2
        repository.emptyTrash()
        val trashedAfterEmpty = repository.getTrashedNotes().first()
        assertTrue("Trash must be empty after emptyTrash()", trashedAfterEmpty.isEmpty())

        val permDeleted = repository.getNoteById("t-2")
        assertNull("Permanently deleted note must no longer exist in database", permDeleted)
    }
}
