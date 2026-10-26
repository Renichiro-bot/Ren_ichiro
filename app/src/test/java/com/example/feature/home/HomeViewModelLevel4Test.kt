package com.example.feature.home

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.data.repository.NoteRepository
import com.example.core.data.repository.NoteRepositoryImpl
import com.example.core.database.VellumDatabase
import com.example.core.model.ChecklistItem
import com.example.core.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeViewModelLevel4Test {

    private val testScheduler = TestCoroutineScheduler()
    private val testDispatcher = StandardTestDispatcher(testScheduler)

    private lateinit var database: VellumDatabase
    private lateinit var repository: NoteRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VellumDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NoteRepositoryImpl(database.noteDao(), testDispatcher)
        viewModel = HomeViewModel(repository)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun filtering_pinnedAndChecklistsFiltersWorkCorrectly() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.notes.collect()
        }

        val now = System.currentTimeMillis()
        repository.saveNote(Note(id = "n-1", title = "Alpha", content = "Normal", isPinned = false, isChecklist = false, createdAtEpochMs = now, updatedAtEpochMs = now))
        repository.saveNote(Note(id = "n-2", title = "Beta", content = "Pinned", isPinned = true, isChecklist = false, createdAtEpochMs = now, updatedAtEpochMs = now))
        repository.saveNote(Note(id = "n-3", title = "Gamma", content = "", isPinned = false, isChecklist = true, checklistItems = listOf(ChecklistItem(text = "Task 1")), createdAtEpochMs = now, updatedAtEpochMs = now))

        advanceUntilIdle()

        // Filter ALL - wait for all 3 notes to be processed
        viewModel.setFilter(HomeFilter.ALL)
        advanceUntilIdle()
        val allNotes = viewModel.notes.first { it.size == 3 }
        assertEquals(3, allNotes.size)

        // Filter PINNED - only 1 note is pinned
        viewModel.setFilter(HomeFilter.PINNED)
        advanceUntilIdle()
        val pinnedOnly = viewModel.notes.first { it.size == 1 && it.first().id == "n-2" }
        assertEquals("n-2", pinnedOnly.first().id)

        // Filter CHECKLISTS - only 1 note is a checklist
        viewModel.setFilter(HomeFilter.CHECKLISTS)
        advanceUntilIdle()
        val checklistsOnly = viewModel.notes.first { it.size == 1 && it.first().id == "n-3" }
        assertEquals("n-3", checklistsOnly.first().id)
    }

    @Test
    fun sorting_byUpdated_byCreated_byTitleAlphabetical() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.notes.collect()
        }

        val t0 = 1000L
        repository.saveNote(Note(id = "n-1", title = "Zebra", content = "Content", isPinned = false, createdAtEpochMs = t0 + 100, updatedAtEpochMs = t0 + 300))
        repository.saveNote(Note(id = "n-2", title = "Apple", content = "Content", isPinned = false, createdAtEpochMs = t0 + 300, updatedAtEpochMs = t0 + 100))
        repository.saveNote(Note(id = "n-3", title = "Mango", content = "Content", isPinned = false, createdAtEpochMs = t0 + 200, updatedAtEpochMs = t0 + 200))

        advanceUntilIdle()

        // Wait until all 3 are in the feed
        viewModel.notes.first { it.size == 3 }

        // Recently Updated (Default)
        viewModel.setSortOrder(NoteSortOrder.UPDATED_DESC)
        advanceUntilIdle()
        val sortedByUpdate = viewModel.notes.first { it.first().id == "n-1" }
        assertEquals("n-1", sortedByUpdate[0].id) // 1300
        assertEquals("n-3", sortedByUpdate[1].id) // 1200
        assertEquals("n-2", sortedByUpdate[2].id) // 1100

        // Recently Created
        viewModel.setSortOrder(NoteSortOrder.CREATED_DESC)
        advanceUntilIdle()
        val sortedByCreated = viewModel.notes.first { it.first().id == "n-2" }
        assertEquals("n-2", sortedByCreated[0].id) // 1300
        assertEquals("n-3", sortedByCreated[1].id) // 1200
        assertEquals("n-1", sortedByCreated[2].id) // 1100

        // Title Alphabetical (A-Z)
        viewModel.setSortOrder(NoteSortOrder.TITLE_ASC)
        advanceUntilIdle()
        val sortedByTitle = viewModel.notes.first { it.first().title == "Apple" }
        assertEquals("Apple", sortedByTitle[0].title)
        assertEquals("Mango", sortedByTitle[1].title)
        assertEquals("Zebra", sortedByTitle[2].title)
    }

    @Test
    fun selectionMode_selectionAndBulkOperations() = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.notes.collect()
        }

        repository.saveNote(Note(id = "n-1", title = "Note 1", content = "Content", isPinned = false))
        repository.saveNote(Note(id = "n-2", title = "Note 2", content = "Content", isPinned = false))
        repository.saveNote(Note(id = "n-3", title = "Note 3", content = "Content", isPinned = false))

        advanceUntilIdle()
        viewModel.notes.first { it.size == 3 }

        // Initially no selection
        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedNoteIds.value.isEmpty())

        // Start selection on n-1
        viewModel.startSelection("n-1")
        advanceUntilIdle()
        assertTrue(viewModel.isSelectionMode.value)
        assertEquals(setOf("n-1"), viewModel.selectedNoteIds.value)

        // Toggle n-2 (both n-1 and n-2 selected)
        viewModel.toggleNoteSelection("n-2")
        advanceUntilIdle()
        assertEquals(setOf("n-1", "n-2"), viewModel.selectedNoteIds.value)

        // Select All
        viewModel.selectAll()
        advanceUntilIdle()
        assertEquals(3, viewModel.selectedNoteIds.value.size)

        // Clear Selection
        viewModel.clearSelection()
        advanceUntilIdle()
        assertFalse(viewModel.isSelectionMode.value)
        assertTrue(viewModel.selectedNoteIds.value.isEmpty())

        // Bulk Pin Selected
        viewModel.toggleNoteSelection("n-1")
        viewModel.toggleNoteSelection("n-2")
        advanceUntilIdle()
        viewModel.bulkTogglePinSelected()
        advanceUntilIdle()

        val updatedNotes = repository.getActiveNotes().first { notes -> notes.any { it.isPinned } }
        assertTrue(updatedNotes.first { it.id == "n-1" }.isPinned)
        assertTrue(updatedNotes.first { it.id == "n-2" }.isPinned)
        assertFalse(updatedNotes.first { it.id == "n-3" }.isPinned)

        // Bulk Delete Selected
        viewModel.toggleNoteSelection("n-1")
        viewModel.toggleNoteSelection("n-2")
        advanceUntilIdle()
        viewModel.bulkDeleteSelected()
        advanceUntilIdle()

        val remainingNotes = repository.getActiveNotes().first { it.size == 1 }
        assertEquals(1, remainingNotes.size)
        assertEquals("n-3", remainingNotes.first().id)
        assertFalse(viewModel.isSelectionMode.value)
    }
}
