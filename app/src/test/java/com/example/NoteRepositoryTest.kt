package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.repository.NoteRepository
import com.example.core.data.repository.NoteRepositoryImpl
import com.example.core.database.VellumDatabase
import com.example.core.model.ChecklistItem
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
class NoteRepositoryTest {

    private lateinit var database: VellumDatabase
    private lateinit var repository: NoteRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VellumDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NoteRepositoryImpl(database.noteDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun sampleDataPolicy_initialDatabaseIsEmpty() = runBlocking {
        val notes = repository.getActiveNotes().first()
        assertTrue("Initial database must be clean and not auto-populated", notes.isEmpty())
    }

    @Test
    fun saveNote_insertsNewNoteSuccessfully() = runBlocking {
        val note = Note(
            id = "note-1",
            title = "First Note",
            content = "This is a persisted note.",
            isPinned = false,
            colorTheme = NoteColorTheme.SAND,
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )

        repository.saveNote(note)

        val retrieved = repository.getNoteById("note-1")
        assertNotNull(retrieved)
        assertEquals("First Note", retrieved?.title)
        assertEquals("This is a persisted note.", retrieved?.content)
        assertEquals(NoteColorTheme.SAND, retrieved?.colorTheme)
        assertFalse(retrieved!!.isPinned)
    }

    @Test
    fun saveNote_updatesWithoutDuplicatesAndPreservesCreatedAt() = runBlocking {
        val initial = Note(
            id = "note-stable",
            title = "Original Title",
            content = "Original Content",
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 1000L
        )
        repository.saveNote(initial)

        val updated = Note(
            id = "note-stable",
            title = "Modified Title",
            content = "Modified Content",
            createdAtEpochMs = 1000L,
            updatedAtEpochMs = 2500L
        )
        repository.saveNote(updated)

        val allNotes = repository.getActiveNotes().first()
        assertEquals(1, allNotes.size)

        val retrieved = repository.getNoteById("note-stable")
        assertEquals("Modified Title", retrieved?.title)
        assertEquals("Modified Content", retrieved?.content)
        assertEquals(1000L, retrieved?.createdAtEpochMs)
        assertEquals(2500L, retrieved?.updatedAtEpochMs)
    }

    @Test
    fun getActiveNotes_sortsPinnedFirstThenByUpdatedDesc() = runBlocking {
        val noteA = Note(
            id = "note-a",
            title = "Note A (Unpinned, Older)",
            updatedAtEpochMs = 100L,
            isPinned = false
        )
        val noteB = Note(
            id = "note-b",
            title = "Note B (Unpinned, Newer)",
            updatedAtEpochMs = 200L,
            isPinned = false
        )
        val noteC = Note(
            id = "note-c",
            title = "Note C (Pinned, Older)",
            updatedAtEpochMs = 150L,
            isPinned = true
        )

        repository.saveNote(noteA)
        repository.saveNote(noteB)
        repository.saveNote(noteC)

        val activeNotes = repository.getActiveNotes().first()
        assertEquals(3, activeNotes.size)

        // Pinned note comes first
        assertEquals("note-c", activeNotes[0].id)
        assertTrue(activeNotes[0].isPinned)

        // Newer unpinned note comes second
        assertEquals("note-b", activeNotes[1].id)

        // Older unpinned note comes third
        assertEquals("note-a", activeNotes[2].id)
    }

    @Test
    fun deleteNote_removesFromDatabase() = runBlocking {
        val note = Note(id = "delete-me", title = "To be deleted")
        repository.saveNote(note)
        assertNotNull(repository.getNoteById("delete-me"))

        // Level 5: deleteNote soft-deletes to trash
        repository.deleteNote("delete-me")
        assertTrue("Soft-deleted note must not appear in active notes", repository.getActiveNotes().first().isEmpty())
        assertTrue("Soft-deleted note must be marked as trashed", repository.getNoteById("delete-me")?.isTrashed == true)

        // Permanent deletion purges from database completely
        repository.deleteNotePermanently("delete-me")
        assertNull("Permanently deleted note must not exist in database", repository.getNoteById("delete-me"))
    }

    @Test
    fun togglePin_invertsStatusPersistently() = runBlocking {
        val note = Note(id = "pin-toggle", title = "Pin test", isPinned = false)
        repository.saveNote(note)

        repository.togglePin("pin-toggle")
        assertTrue(repository.getNoteById("pin-toggle")!!.isPinned)

        repository.togglePin("pin-toggle")
        assertFalse(repository.getNoteById("pin-toggle")!!.isPinned)
    }

    @Test
    fun toggleChecklistItem_updatesStatusInJson() = runBlocking {
        val item1 = ChecklistItem(id = "item-1", text = "Write tests", isDone = false)
        val item2 = ChecklistItem(id = "item-2", text = "Build release", isDone = false)
        val note = Note(
            id = "checklist-note",
            title = "Tasks",
            isChecklist = true,
            checklistItems = listOf(item1, item2)
        )
        repository.saveNote(note)

        repository.toggleChecklistItem("checklist-note", "item-1")

        val retrieved = repository.getNoteById("checklist-note")
        assertNotNull(retrieved)
        assertTrue(retrieved!!.checklistItems.first { it.id == "item-1" }.isDone)
        assertFalse(retrieved.checklistItems.first { it.id == "item-2" }.isDone)
    }

    @Test
    fun searchNotes_caseInsensitiveMatches() = runBlocking {
        repository.saveNote(Note(id = "s-1", title = "Kotlin Coroutines", content = "Structured concurrency in Android."))
        repository.saveNote(Note(id = "s-2", title = "Room Database", content = "SQLite object mapping with Flow support."))
        repository.saveNote(
            Note(
                id = "s-3",
                title = "Sprint Planning",
                isChecklist = true,
                checklistItems = listOf(ChecklistItem(text = "Review Architectural Design"))
            )
        )

        // Match title case-insensitively
        val titleResults = repository.searchNotes("coroutines").first()
        assertEquals(1, titleResults.size)
        assertEquals("s-1", titleResults.first().id)

        // Match content case-insensitively
        val contentResults = repository.searchNotes("mapping").first()
        assertEquals(1, contentResults.size)
        assertEquals("s-2", contentResults.first().id)

        // Match checklist item case-insensitively
        val checklistResults = repository.searchNotes("architectural").first()
        assertEquals(1, checklistResults.size)
        assertEquals("s-3", checklistResults.first().id)

        // Non-matching query
        val emptyResults = repository.searchNotes("NonExistentKeywordXYZ").first()
        assertTrue(emptyResults.isEmpty())
    }

    @Test
    fun loadSampleNotes_and_clearAllNotes() = runBlocking {
        repository.loadSampleNotes()
        val loaded = repository.getActiveNotes().first()
        assertEquals(6, loaded.size)
        assertTrue(loaded.any { it.isPinned })
        assertTrue(loaded.any { it.isChecklist })

        repository.clearAllNotes()
        val cleared = repository.getActiveNotes().first()
        assertTrue(cleared.isEmpty())
    }

    @Test
    fun deleteNotes_removesMultipleNotesInSingleTransaction() = runBlocking {
        repository.loadSampleNotes()
        val allNotes = repository.getActiveNotes().first()
        val toDelete = allNotes.take(2).map { it.id }

        repository.deleteNotes(toDelete)
        val remaining = repository.getActiveNotes().first()
        assertEquals(allNotes.size - 2, remaining.size)
        assertTrue(remaining.none { it.id in toDelete })
    }

    @Test
    fun setPinStatusForNotes_updatesPinStateForAllSpecifiedIds() = runBlocking {
        repository.loadSampleNotes()
        val allNotes = repository.getActiveNotes().first()
        val unpinnedIds = allNotes.filterNot { it.isPinned }.map { it.id }

        repository.setPinStatusForNotes(unpinnedIds, true)
        val updated = repository.getActiveNotes().first()
        val updatedTargetNotes = updated.filter { it.id in unpinnedIds }
        assertTrue(updatedTargetNotes.all { it.isPinned })
    }
}
