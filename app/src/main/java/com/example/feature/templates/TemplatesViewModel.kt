package com.example.feature.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.data.repository.NoteRepository
import com.example.core.data.repository.TemplateRepository
import com.example.core.model.ChecklistItem
import com.example.core.model.Note
import com.example.core.model.NoteTemplate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class TemplatesViewModel(
    private val templateRepository: TemplateRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    val templates: StateFlow<List<NoteTemplate>> = templateRepository.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createNoteFromTemplate(template: NoteTemplate, onNoteCreated: (Note) -> Unit) {
        viewModelScope.launch {
            val checklistItems = if (template.isChecklist) {
                template.checklistItemsTemplate.map { text ->
                    ChecklistItem(id = UUID.randomUUID().toString(), text = text, isDone = false)
                }
            } else {
                emptyList()
            }

            val newNote = Note(
                id = UUID.randomUUID().toString(),
                title = template.defaultTitle.ifBlank { template.name },
                content = template.contentTemplate,
                isChecklist = template.isChecklist,
                checklistItems = checklistItems,
                createdAtEpochMs = System.currentTimeMillis(),
                updatedAtEpochMs = System.currentTimeMillis()
            )
            noteRepository.saveNote(newNote)
            onNoteCreated(newNote)
        }
    }

    fun createCustomTemplate(
        name: String,
        description: String,
        defaultTitle: String,
        contentTemplate: String
    ) {
        viewModelScope.launch {
            templateRepository.createCustomTemplate(
                name = name,
                description = description,
                defaultTitle = defaultTitle,
                contentTemplate = contentTemplate
            )
        }
    }

    fun deleteTemplate(id: String) {
        viewModelScope.launch {
            templateRepository.deleteTemplate(id)
        }
    }

    companion object {
        fun provideFactory(
            templateRepository: TemplateRepository,
            noteRepository: NoteRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TemplatesViewModel(templateRepository, noteRepository) as T
            }
        }
    }
}
