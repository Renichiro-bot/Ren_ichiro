package com.example.feature.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.data.repository.NoteRepository
import com.example.core.model.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ArchiveViewModel(
    private val repository: NoteRepository
) : ViewModel() {

    val archivedNotes: StateFlow<List<Note>> = repository.getArchivedNotes()
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

    fun startSelection(noteId: String) {
        _selectedNoteIds.update { setOf(noteId) }
    }

    fun toggleSelection(noteId: String) {
        _selectedNoteIds.update { current ->
            if (current.contains(noteId)) current - noteId else current + noteId
        }
    }

    fun selectAll() {
        val allIds = archivedNotes.value.map { it.id }.toSet()
        _selectedNoteIds.value = allIds
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun unarchiveNote(id: String) {
        viewModelScope.launch {
            repository.unarchiveNote(id)
        }
    }

    fun unarchiveSelected(onComplete: (() -> Unit)? = null) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.unarchiveNotes(ids)
            clearSelection()
            onComplete?.invoke()
        }
    }

    fun moveNoteToTrash(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
        }
    }

    fun moveSelectedToTrash(onComplete: (() -> Unit)? = null) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            repository.deleteNotes(ids)
            clearSelection()
            onComplete?.invoke()
        }
    }

    companion object {
        fun provideFactory(repository: NoteRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return ArchiveViewModel(repository) as T
                }
            }
    }
}
