package com.example.feature.editor

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.backup.NoteImportExportManager
import com.example.core.data.repository.AttachmentRepository
import com.example.core.data.repository.NoteRepository
import com.example.core.model.Attachment
import com.example.core.model.ChecklistItem
import com.example.core.model.Folder
import com.example.core.model.Note
import com.example.core.model.NoteColorTheme
import com.example.core.model.Tag
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.Context
import com.example.core.ocr.OcrExtractionResult
import com.example.core.ocr.VellumOcrManager
import java.util.UUID

enum class SaveStatus {
    SAVED,
    SAVING,
    UNSAVED
}

sealed class OcrState {
    data object Idle : OcrState()
    data object Processing : OcrState()
    data class Success(val result: OcrExtractionResult) : OcrState()
    data class Error(val message: String) : OcrState()
}

@OptIn(FlowPreview::class)
class NoteEditorViewModel(
    val noteId: String?,
    private val repository: NoteRepository,
    private val attachmentRepository: AttachmentRepository? = null,
    private val importExportManager: NoteImportExportManager? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val initialTitle: String = "",
    private val initialContent: String = ""
) : ViewModel() {

    val currentNoteId: String = if (noteId.isNullOrBlank() || noteId == "new") {
        UUID.randomUUID().toString()
    } else {
        noteId
    }

    private var isNewNote: Boolean = noteId.isNullOrBlank() || noteId == "new"
    private var originalCreatedAtMs: Long = System.currentTimeMillis()
    private var hasPersistedAtLeastOnce: Boolean = false

    private val _ocrState = MutableStateFlow<OcrState>(OcrState.Idle)
    val ocrState: StateFlow<OcrState> = _ocrState.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _isPinned = MutableStateFlow(false)
    val isPinned: StateFlow<Boolean> = _isPinned.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    private val _isChecklist = MutableStateFlow(false)
    val isChecklist: StateFlow<Boolean> = _isChecklist.asStateFlow()

    private val _checklistItems = MutableStateFlow<List<ChecklistItem>>(emptyList())
    val checklistItems: StateFlow<List<ChecklistItem>> = _checklistItems.asStateFlow()

    private val _folderId = MutableStateFlow<String?>(null)
    val folderId: StateFlow<String?> = _folderId.asStateFlow()

    private val _tags = MutableStateFlow<List<String>>(emptyList())
    val tags: StateFlow<List<String>> = _tags.asStateFlow()

    private val _colorTheme = MutableStateFlow(NoteColorTheme.DEFAULT)
    val colorTheme: StateFlow<NoteColorTheme> = _colorTheme.asStateFlow()

    private val _saveStatus = MutableStateFlow(SaveStatus.SAVED)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    val availableFolders: StateFlow<List<Folder>> = repository.getAllFolders()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val availableTags: StateFlow<List<Tag>> = repository.getAllTags()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val attachments: StateFlow<List<Attachment>> = (
        attachmentRepository?.getAttachmentsForNote(currentNoteId)
            ?: kotlinx.coroutines.flow.flowOf(emptyList())
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // Debounce trigger flow for autosave
    private val saveTriggerFlow = MutableSharedFlow<Unit>(replay = 1, extraBufferCapacity = 64)

    init {
        // Load existing note if editing
        viewModelScope.launch(ioDispatcher) {
            if (!isNewNote) {
                val existingNote = repository.getNoteById(currentNoteId)
                if (existingNote != null) {
                    _title.value = existingNote.title
                    _content.value = existingNote.content
                    _isPinned.value = existingNote.isPinned
                    _isFavorite.value = existingNote.isFavorite
                    _isChecklist.value = existingNote.isChecklist
                    _checklistItems.value = existingNote.checklistItems
                    _folderId.value = existingNote.folderId
                    _tags.value = existingNote.tags
                    _colorTheme.value = existingNote.colorTheme
                    originalCreatedAtMs = existingNote.createdAtEpochMs
                    hasPersistedAtLeastOnce = true
                }
            } else {
                if (initialTitle.isNotBlank()) _title.value = initialTitle
                if (initialContent.isNotBlank()) _content.value = initialContent
                if (initialTitle.isNotBlank() || initialContent.isNotBlank()) {
                    _saveStatus.value = SaveStatus.UNSAVED
                    triggerAutosave()
                }
            }
            _isLoaded.value = true
        }

        // Setup debounced autosave pipeline (500ms debounce)
        viewModelScope.launch {
            saveTriggerFlow
                .debounce(500L)
                .collect {
                    performSave()
                }
        }
    }

    fun onTitleChange(newTitle: String) {
        if (_title.value != newTitle) {
            _title.value = newTitle
            _saveStatus.value = SaveStatus.UNSAVED
            triggerAutosave()
        }
    }

    fun onContentChange(newContent: String) {
        if (_content.value != newContent) {
            _content.value = newContent
            _saveStatus.value = SaveStatus.UNSAVED
            triggerAutosave()
        }
    }

    fun togglePin() {
        _isPinned.update { !it }
        _saveStatus.value = SaveStatus.UNSAVED
        triggerAutosave()
    }

    fun toggleFavorite() {
        _isFavorite.update { !it }
        _saveStatus.value = SaveStatus.UNSAVED
        triggerAutosave()
    }

    fun toggleChecklistMode() {
        val willBeChecklist = !_isChecklist.value
        _isChecklist.value = willBeChecklist
        if (willBeChecklist && _checklistItems.value.isEmpty() && _content.value.isNotBlank()) {
            val lines = _content.value.lines().filter { it.isNotBlank() }
            val newItems = lines.map {
                ChecklistItem(
                    id = UUID.randomUUID().toString(),
                    text = it.removePrefix("- [ ] ").removePrefix("- [x] ").trim(),
                    isDone = it.startsWith("- [x] ")
                )
            }
            _checklistItems.value = newItems
        } else if (!willBeChecklist && _checklistItems.value.isNotEmpty() && _content.value.isBlank()) {
            val formatted = _checklistItems.value.joinToString("\n") {
                val mark = if (it.isDone) "x" else " "
                "- [$mark] ${it.text}"
            }
            _content.value = formatted
        }
        _saveStatus.value = SaveStatus.UNSAVED
        triggerAutosave()
    }

    fun addChecklistItem(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotBlank()) {
            val newItem = ChecklistItem(
                id = UUID.randomUUID().toString(),
                text = trimmed,
                isDone = false
            )
            _checklistItems.update { it + newItem }
            _saveStatus.value = SaveStatus.UNSAVED
            triggerAutosave()
        }
    }
    fun onChecklistItemAdd(text: String) = addChecklistItem(text)

    fun toggleChecklistItem(itemId: String) {
        _checklistItems.update { list ->
            list.map { if (it.id == itemId) it.copy(isDone = !it.isDone) else it }
        }
        _saveStatus.value = SaveStatus.UNSAVED
        triggerAutosave()
    }
    fun onChecklistItemToggle(itemId: String) = toggleChecklistItem(itemId)

    fun removeChecklistItem(itemId: String) {
        _checklistItems.update { list ->
            list.filter { it.id != itemId }
        }
        _saveStatus.value = SaveStatus.UNSAVED
        triggerAutosave()
    }
    fun onChecklistItemRemove(itemId: String) = removeChecklistItem(itemId)

    fun updateChecklistItemText(itemId: String, newText: String) {
        _checklistItems.update { list ->
            list.map { if (it.id == itemId) it.copy(text = newText) else it }
        }
        _saveStatus.value = SaveStatus.UNSAVED
        triggerAutosave()
    }
    fun onChecklistItemTextChange(itemId: String, newText: String) = updateChecklistItemText(itemId, newText)

    fun setFolder(folder: Folder?) {
        _folderId.value = folder?.id
        _saveStatus.value = SaveStatus.UNSAVED
        triggerAutosave()
    }

    fun setFolderById(folderId: String?) {
        _folderId.value = folderId
        _saveStatus.value = SaveStatus.UNSAVED
        triggerAutosave()
    }

    fun addTag(tagName: String) {
        val trimmed = tagName.trim()
        if (trimmed.isNotBlank() && !_tags.value.contains(trimmed)) {
            _tags.update { it + trimmed }
            _saveStatus.value = SaveStatus.UNSAVED
            triggerAutosave()
        }
    }

    fun removeTag(tagName: String) {
        _tags.update { it - tagName }
        _saveStatus.value = SaveStatus.UNSAVED
        triggerAutosave()
    }

    fun setColorTheme(theme: NoteColorTheme) {
        _colorTheme.value = theme
        _saveStatus.value = SaveStatus.UNSAVED
        triggerAutosave()
    }
    fun onColorSelect(theme: NoteColorTheme) = setColorTheme(theme)

    // --- Attachment Actions ---

    fun addAttachment(sourceUri: Uri, onResult: (Result<Attachment>) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            // First flush current note state so note exists in database
            performSave()
            if (attachmentRepository != null) {
                val result = attachmentRepository.addAttachment(currentNoteId, sourceUri)
                withContext(Dispatchers.Main) {
                    onResult(result)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(IllegalStateException("Attachment repository not available")))
                }
            }
        }
    }

    fun deleteAttachment(attachment: Attachment, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            if (attachmentRepository != null) {
                val result = attachmentRepository.deleteAttachment(attachment)
                withContext(Dispatchers.Main) {
                    onResult(result)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(IllegalStateException("Attachment repository not available")))
                }
            }
        }
    }

    fun getViewIntent(attachment: Attachment): Intent? {
        return attachmentRepository?.getViewIntent(attachment)
    }

    // --- Import & Export Actions ---

    fun exportAsMarkdown(destinationUri: Uri, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            performSave()
            if (importExportManager != null) {
                val note = getCurrentNoteModel()
                val result = importExportManager.exportNoteAsMarkdown(note, destinationUri)
                withContext(Dispatchers.Main) {
                    onResult(result)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(IllegalStateException("Export manager not available")))
                }
            }
        }
    }

    fun exportAsPlainText(destinationUri: Uri, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            performSave()
            if (importExportManager != null) {
                val note = getCurrentNoteModel()
                val result = importExportManager.exportNoteAsPlainText(note, destinationUri)
                withContext(Dispatchers.Main) {
                    onResult(result)
                }
            } else {
                withContext(Dispatchers.Main) {
                    onResult(Result.failure(IllegalStateException("Export manager not available")))
                }
            }
        }
    }

    fun createShareIntent(): Intent? {
        if (importExportManager != null) {
            val note = getCurrentNoteModel()
            return importExportManager.createShareIntent(note)
        }
        return null
    }

    private fun getCurrentNoteModel(): Note {
        return Note(
            id = currentNoteId,
            title = _title.value,
            content = _content.value,
            isPinned = _isPinned.value,
            isFavorite = _isFavorite.value,
            isChecklist = _isChecklist.value,
            checklistItems = _checklistItems.value,
            folderId = _folderId.value,
            tags = _tags.value,
            colorTheme = _colorTheme.value,
            createdAtEpochMs = originalCreatedAtMs,
            updatedAtEpochMs = System.currentTimeMillis()
        )
    }

    private fun triggerAutosave() {
        saveTriggerFlow.tryEmit(Unit)
    }

    private suspend fun performSave() = withContext(ioDispatcher) {
        val title = _title.value
        val content = _content.value
        val items = _checklistItems.value
        val hasAttachments = attachments.value.isNotEmpty()

        // Don't create an empty note record if title, content, and checklist are all blank and no attachments exist
        if (title.isBlank() && content.isBlank() && items.isEmpty() && !hasAttachments && !hasPersistedAtLeastOnce) {
            _saveStatus.value = SaveStatus.SAVED
            return@withContext
        }

        _saveStatus.value = SaveStatus.SAVING

        val note = getCurrentNoteModel()
        repository.saveNote(note)
        hasPersistedAtLeastOnce = true
        _saveStatus.value = SaveStatus.SAVED
    }

    fun flushSave() {
        viewModelScope.launch(NonCancellable + ioDispatcher) {
            performSave()
        }
    }

    fun deleteCurrentNote(onDeleted: () -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            repository.deleteNote(currentNoteId)
            withContext(Dispatchers.Main) {
                onDeleted()
            }
        }
    }

    fun archiveCurrentNote(onArchived: () -> Unit) {
        viewModelScope.launch(ioDispatcher) {
            repository.archiveNote(currentNoteId)
            withContext(Dispatchers.Main) {
                onArchived()
            }
        }
    }

    fun extractTextFromImage(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _ocrState.value = OcrState.Processing
            val result = VellumOcrManager.extractTextFromImageUri(context, imageUri)
            result.fold(
                onSuccess = { extracted ->
                    _ocrState.value = OcrState.Success(extracted)
                },
                onFailure = { error ->
                    _ocrState.value = OcrState.Error(error.message ?: "Could not extract text")
                }
            )
        }
    }

    fun dismissOcrState() {
        _ocrState.value = OcrState.Idle
    }

    fun applyOcrAppend(text: String) {
        val current = _content.value
        val updated = if (current.isBlank()) text else "$current\n\n$text"
        onContentChange(updated)
        _ocrState.value = OcrState.Idle
    }

    fun applyOcrInsertTop(text: String) {
        val current = _content.value
        val updated = if (current.isBlank()) text else "$text\n\n$current"
        onContentChange(updated)
        _ocrState.value = OcrState.Idle
    }

    fun applyOcrReplace(text: String) {
        onContentChange(text)
        _ocrState.value = OcrState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        flushSave()
    }

    companion object {
        fun provideFactory(
            noteId: String?,
            repository: NoteRepository,
            attachmentRepository: AttachmentRepository? = null,
            importExportManager: NoteImportExportManager? = null,
            initialTitle: String = "",
            initialContent: String = ""
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NoteEditorViewModel(
                        noteId = noteId,
                        repository = repository,
                        attachmentRepository = attachmentRepository,
                        importExportManager = importExportManager,
                        initialTitle = initialTitle,
                        initialContent = initialContent
                    ) as T
                }
            }
    }
}
