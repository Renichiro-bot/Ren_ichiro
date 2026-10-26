package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.repository.NoteRepository
import com.example.core.data.repository.NoteRepositoryImpl
import com.example.core.database.VellumDatabase
import com.example.core.model.Note
import com.example.feature.editor.NoteEditorViewModel
import com.example.feature.editor.SaveStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NoteEditorViewModelTest {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private lateinit var database: VellumDatabase
    private lateinit var repository: NoteRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VellumDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NoteRepositoryImpl(database.noteDao(), testDispatcher)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun emptyNotePolicy_blankNoteIsNotPersisted() = runTest(testDispatcher) {
        val viewModel = NoteEditorViewModel(
            noteId = "new",
            repository = repository,
            ioDispatcher = testDispatcher
        )

        testScheduler.advanceUntilIdle()

        // When user leaves a blank note without typing, flushSave does not write to Room
        viewModel.flushSave()
        testScheduler.advanceUntilIdle()

        val notes = repository.getActiveNotes().first()
        assertTrue("Untouched blank note must not clutter the database", notes.isEmpty())
    }

    @Test
    fun autosave_persistsAfterTyping() = runTest(testDispatcher) {
        val viewModel = NoteEditorViewModel(
            noteId = "new",
            repository = repository,
            ioDispatcher = testDispatcher
        )

        testScheduler.advanceUntilIdle()

        viewModel.onTitleChange("Autosave Title")
        viewModel.onContentChange("Autosave Body Content")

        // Before debounce window finishes
        assertEquals(SaveStatus.UNSAVED, viewModel.saveStatus.value)

        // Advance debounce window (400ms)
        testScheduler.advanceTimeBy(500L)
        testScheduler.advanceUntilIdle()

        val notes = repository.getActiveNotes().first { it.isNotEmpty() }
        assertEquals(1, notes.size)
        assertEquals("Autosave Title", notes.first().title)
        assertEquals("Autosave Body Content", notes.first().content)
        assertEquals(SaveStatus.SAVED, viewModel.saveStatus.value)
    }

    @Test
    fun flushSave_immediatelyPersistsEditsOnExit() = runTest(testDispatcher) {
        val viewModel = NoteEditorViewModel(
            noteId = "new",
            repository = repository,
            ioDispatcher = testDispatcher
        )

        testScheduler.advanceUntilIdle()

        viewModel.onTitleChange("Urgent Exit")
        // User immediately exits (e.g. BackHandler triggers flushSave before debounce fires)
        viewModel.flushSave()
        testScheduler.advanceUntilIdle()

        val notes = repository.getActiveNotes().first { it.isNotEmpty() }
        assertEquals(1, notes.size)
        assertEquals("Urgent Exit", notes.first().title)
    }

    @Test
    fun editingExistingNote_loadsInitialStateCorrectly() = runTest(testDispatcher) {
        val existing = Note(
            id = "existing-1",
            title = "Loaded Title",
            content = "Loaded Content"
        )
        repository.saveNote(existing)

        val viewModel = NoteEditorViewModel(
            noteId = "existing-1",
            repository = repository,
            ioDispatcher = testDispatcher
        )

        testScheduler.advanceUntilIdle()
        viewModel.isLoaded.first { it }

        assertEquals("Loaded Title", viewModel.title.value)
        assertEquals("Loaded Content", viewModel.content.value)
    }
}
