package com.example.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.data.repository.NoteRepository
import com.example.core.model.Folder
import com.example.core.model.Note
import com.example.core.model.Tag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class NoteSortOrder(val label: String) {
    UPDATED_DESC("Recently Updated"),
    CREATED_DESC("Recently Created"),
    TITLE_ASC("Title (A–Z)")
}

enum class HomeFilter(val label: String) {
    ALL("All Notes"),
    FAVORITES("Favorites"),
    PINNED("Pinned"),
    CHECKLISTS("Checklists")
}

class HomeViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    private val _isGridLayout = MutableStateFlow(true)
    val isGridLayout: StateFlow<Boolean> = _isGridLayout.asStateFlow()

    private val _sortOrder = MutableStateFlow(NoteSortOrder.UPDATED_DESC)
    val sortOrder: StateFlow<NoteSortOrder> = _sortOrder.asStateFlow()

    private val _filter = MutableStateFlow(HomeFilter.ALL)
    val filter: StateFlow<HomeFilter> = _filter.asStateFlow()

    private val _selectedFolder = MutableStateFlow<Folder?>(null)
    val selectedFolder: StateFlow<Folder?> = _selectedFolder.asStateFlow()

    private val _selectedTag = MutableStateFlow<Tag?>(null)
    val selectedTag: StateFlow<Tag?> = _selectedTag.asStateFlow()

    val folders: StateFlow<List<Folder>> = repository.getAllFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val tags: StateFlow<List<Tag>> = repository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _selectedNoteIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedNoteIds: StateFlow<Set<String>> = _selectedNoteIds.asStateFlow()

    val isSelectionMode: StateFlow<Boolean> = _selectedNoteIds
        .map { it.isNotEmpty() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private data class HomeFilterState(
        val sort: NoteSortOrder = NoteSortOrder.UPDATED_DESC,
        val filter: HomeFilter = HomeFilter.ALL,
        val folder: Folder? = null,
        val tag: Tag? = null,
        val query: String = ""
    )

    private val _filterState = combine(
        _sortOrder,
        _filter,
        _selectedFolder,
        _selectedTag,
        _searchQuery
    ) { sort, filter, folder, tag, query ->
        HomeFilterState(sort, filter, folder, tag, query)
    }

    val notes: StateFlow<List<Note>> = combine(
        repository.getActiveNotes(),
        _filterState
    ) { allNotes, filterState ->
        var list = allNotes

        // 1. Filter by folder if active
        if (filterState.folder != null) {
            list = list.filter { it.folderId == filterState.folder.id }
        }

        // 2. Filter by tag if active
        if (filterState.tag != null) {
            list = list.filter { note ->
                note.tags.any { it.equals(filterState.tag.name, ignoreCase = true) }
            }
        }

        // 3. Filter by filter preset
        val filtered = when (filterState.filter) {
            HomeFilter.ALL -> list
            HomeFilter.FAVORITES -> list.filter { it.isFavorite }
            HomeFilter.PINNED -> list.filter { it.isPinned }
            HomeFilter.CHECKLISTS -> list.filter { it.isChecklist }
        }

        // 4. Real-time in-memory search filtering (instant, no screen transitions)
        val searchFiltered = if (filterState.query.isNotBlank()) {
            val q = filterState.query.trim()
            filtered.filter { note ->
                note.title.contains(q, ignoreCase = true) ||
                note.content.contains(q, ignoreCase = true) ||
                note.checklistItems.any { it.text.contains(q, ignoreCase = true) } ||
                note.tags.any { it.contains(q, ignoreCase = true) }
            }
        } else {
            filtered
        }

        // 5. Sort: pinned notes always remain on top when present
        searchFiltered.sortedWith(
            compareByDescending<Note> { it.isPinned }
                .then(
                    when (filterState.sort) {
                        NoteSortOrder.UPDATED_DESC -> compareByDescending { it.updatedAtEpochMs }
                        NoteSortOrder.CREATED_DESC -> compareByDescending { it.createdAtEpochMs }
                        NoteSortOrder.TITLE_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title.ifBlank { "zzz" } }
                    }
                )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
    }

    fun setSortOrder(order: NoteSortOrder) {
        _sortOrder.value = order
    }

    fun setFilter(filter: HomeFilter) {
        _filter.value = filter
    }

    fun selectFolder(folder: Folder?) {
        _selectedFolder.value = folder
    }

    fun selectTag(tag: Tag?) {
        _selectedTag.value = tag
    }

    fun clearFolderAndTagFilters() {
        _selectedFolder.value = null
        _selectedTag.value = null
    }

    fun toggleLayout() {
        _isGridLayout.update { !it }
    }

    fun togglePin(noteId: String) {
        viewModelScope.launch {
            repository.togglePin(noteId)
        }
    }

    fun toggleFavorite(noteId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(noteId)
        }
    }

    fun archiveNote(noteId: String, onArchived: (() -> Unit)? = null) {
        viewModelScope.launch {
            repository.archiveNote(noteId)
            onArchived?.invoke()
        }
    }

    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            repository.deleteNote(noteId)
        }
    }

    fun toggleChecklistItem(noteId: String, itemId: String) {
        viewModelScope.launch {
            repository.toggleChecklistItem(noteId, itemId)
        }
    }

    // --- Folder Operations ---

    fun createFolder(name: String, onResult: (Result<Folder>) -> Unit) {
        viewModelScope.launch {
            val result = repository.createFolder(name)
            onResult(result)
        }
    }

    fun renameFolder(id: String, newName: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = repository.renameFolder(id, newName)
            onResult(result)
        }
    }

    fun deleteFolderSafely(id: String) {
        viewModelScope.launch {
            if (_selectedFolder.value?.id == id) {
                _selectedFolder.value = null
            }
            repository.deleteFolderSafely(id)
        }
    }

    // --- Tag Operations ---

    fun createTag(name: String, onResult: (Result<Tag>) -> Unit) {
        viewModelScope.launch {
            val result = repository.createTag(name)
            onResult(result)
        }
    }

    fun deleteTagSafely(id: String) {
        viewModelScope.launch {
            if (_selectedTag.value?.id == id) {
                _selectedTag.value = null
            }
            repository.deleteTagSafely(id)
        }
    }

    fun loadSampleNotes() {
        viewModelScope.launch {
            repository.loadSampleNotes()
        }
    }

    fun clearAllNotes() {
        viewModelScope.launch {
            repository.clearAllNotes()
        }
    }

    // --- Bulk / Selection Operations ---

    fun startSelection(noteId: String) {
        _selectedNoteIds.update { setOf(noteId) }
    }

    fun toggleNoteSelection(noteId: String) {
        _selectedNoteIds.update { current ->
            if (current.contains(noteId)) current - noteId else current + noteId
        }
    }

    fun selectAll() {
        val currentNotes = notes.value
        _selectedNoteIds.value = currentNotes.map { it.id }.toSet()
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun bulkDeleteSelected(onComplete: (() -> Unit)? = null) {
        val idsToDelete = _selectedNoteIds.value.toList()
        if (idsToDelete.isEmpty()) return
        viewModelScope.launch {
            repository.deleteNotes(idsToDelete)
            clearSelection()
            onComplete?.invoke()
        }
    }

    fun bulkTogglePinSelected(onComplete: (() -> Unit)? = null) {
        val selectedIds = _selectedNoteIds.value.toList()
        if (selectedIds.isEmpty()) return
        val currentNotes = notes.value
        val selectedNotes = currentNotes.filter { it.id in _selectedNoteIds.value }
        val shouldPin = selectedNotes.any { !it.isPinned }

        viewModelScope.launch {
            repository.setPinStatusForNotes(selectedIds, shouldPin)
            clearSelection()
            onComplete?.invoke()
        }
    }

    fun bulkToggleFavoriteSelected(onComplete: (() -> Unit)? = null) {
        val selectedIds = _selectedNoteIds.value.toList()
        if (selectedIds.isEmpty()) return
        val currentNotes = notes.value
        val selectedNotes = currentNotes.filter { it.id in _selectedNoteIds.value }
        val shouldFavorite = selectedNotes.any { !it.isFavorite }

        viewModelScope.launch {
            repository.setFavoriteStatusForNotes(selectedIds, shouldFavorite)
            clearSelection()
            onComplete?.invoke()
        }
    }

    fun bulkArchiveSelected(onComplete: (() -> Unit)? = null) {
        val selectedIds = _selectedNoteIds.value.toList()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            repository.archiveNotes(selectedIds)
            clearSelection()
            onComplete?.invoke()
        }
    }

    fun bulkMoveSelectedToFolder(folderId: String?, onComplete: (() -> Unit)? = null) {
        val selectedIds = _selectedNoteIds.value.toList()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            repository.moveNotesToFolder(selectedIds, folderId)
            clearSelection()
            onComplete?.invoke()
        }
    }

    fun bulkAddTagToSelected(tagName: String, onComplete: (() -> Unit)? = null) {
        val selectedIds = _selectedNoteIds.value.toList()
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            repository.addTagToNotes(selectedIds, tagName)
            clearSelection()
            onComplete?.invoke()
        }
    }

    companion object {
        fun provideFactory(repository: NoteRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return HomeViewModel(repository) as T
                }
            }
    }
}
