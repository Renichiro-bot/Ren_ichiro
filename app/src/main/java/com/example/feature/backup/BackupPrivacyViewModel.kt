package com.example.feature.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.backup.BackupManager
import com.example.core.backup.BackupManifest
import com.example.core.backup.BackupValidationResult
import com.example.core.backup.ImportPreview
import com.example.core.backup.NoteImportExportManager
import com.example.core.backup.RestoreMode
import com.example.core.backup.RestoreResult
import com.example.core.data.repository.AttachmentRepository
import com.example.core.data.repository.NoteRepository
import com.example.core.data.repository.ReminderRepository
import com.example.core.data.repository.TaskRepository
import com.example.core.model.Folder
import com.example.core.security.AppLockManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class StorageStats(
    val totalNotes: Int = 0,
    val totalFolders: Int = 0,
    val totalTags: Int = 0,
    val totalTasks: Int = 0,
    val totalReminders: Int = 0,
    val totalAttachments: Int = 0,
    val totalAttachmentSizeBytes: Long = 0L
) {
    val formattedAttachmentSize: String
        get() {
            if (totalAttachmentSizeBytes <= 0) return "0 KB"
            val kb = totalAttachmentSizeBytes / 1024.0
            val mb = kb / 1024.0
            return if (mb >= 1.0) String.format("%.1f MB", mb) else String.format("%.1f KB", kb)
        }
}

class BackupPrivacyViewModel(
    private val backupManager: BackupManager,
    private val importExportManager: NoteImportExportManager,
    private val noteRepository: NoteRepository,
    private val attachmentRepository: AttachmentRepository,
    private val taskRepository: TaskRepository,
    private val reminderRepository: ReminderRepository,
    private val appLockManager: AppLockManager
) : ViewModel() {

    private val _storageStats = MutableStateFlow(StorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _busyMessage = MutableStateFlow("")
    val busyMessage: StateFlow<String> = _busyMessage.asStateFlow()

    private val _validationResult = MutableStateFlow<BackupValidationResult?>(null)
    val validationResult: StateFlow<BackupValidationResult?> = _validationResult.asStateFlow()

    private val _pendingRestoreUri = MutableStateFlow<Uri?>(null)
    val pendingRestoreUri: StateFlow<Uri?> = _pendingRestoreUri.asStateFlow()

    private val _importPreview = MutableStateFlow<ImportPreview?>(null)
    val importPreview: StateFlow<ImportPreview?> = _importPreview.asStateFlow()

    val isAppLockEnabled: StateFlow<Boolean> = appLockManager.isAppLockEnabledStream
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val isDeviceSecure: Boolean = appLockManager.isDeviceSecure()

    val availableFolders: StateFlow<List<Folder>> = noteRepository.getAllFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        refreshStorageStats()
    }

    fun refreshStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            val notes = noteRepository.getAllNotesSync()
            val folders = noteRepository.getAllFoldersSync()
            val tags = noteRepository.getAllTagsSync()
            val attachments = attachmentRepository.getAllAttachments()
            val totalBytes = attachmentRepository.getTotalAttachmentSizeBytes()
            // Pull counts
            var attCount = 0
            var attBytes = 0L
            val attList = attachmentRepository.getAllAttachments()
            // We can read first emission
            _storageStats.value = StorageStats(
                totalNotes = notes.size,
                totalFolders = folders.size,
                totalTags = tags.size,
                totalAttachments = 0,
                totalAttachmentSizeBytes = 0L
            )
        }
    }

    fun createBackup(destinationUri: Uri, onComplete: (Result<BackupManifest>) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            _busyMessage.value = "Creating and verifying backup archive..."
            val result = backupManager.createBackup(destinationUri)
            _isBusy.value = false
            _busyMessage.value = ""
            refreshStorageStats()
            onComplete(result)
        }
    }

    fun inspectBackup(sourceUri: Uri) {
        viewModelScope.launch {
            _isBusy.value = true
            _busyMessage.value = "Validating backup archive..."
            val result = backupManager.validateBackup(sourceUri)
            _isBusy.value = false
            _busyMessage.value = ""
            _pendingRestoreUri.value = sourceUri
            _validationResult.value = result
        }
    }

    fun dismissValidationDialog() {
        _validationResult.value = null
        _pendingRestoreUri.value = null
    }

    fun executeRestore(mode: RestoreMode, onComplete: (RestoreResult) -> Unit) {
        val uri = _pendingRestoreUri.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            _busyMessage.value = if (mode == RestoreMode.MERGE) "Merging backup with current notes..." else "Restoring data from backup..."
            val result = backupManager.restoreBackup(uri, mode)
            _isBusy.value = false
            _busyMessage.value = ""
            _validationResult.value = null
            _pendingRestoreUri.value = null
            refreshStorageStats()
            onComplete(result)
        }
    }

    fun inspectImportFile(sourceUri: Uri, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            _busyMessage.value = "Inspecting text/markdown file..."
            val result = importExportManager.parseImportFile(sourceUri)
            _isBusy.value = false
            _busyMessage.value = ""
            if (result.isSuccess) {
                _importPreview.value = result.getOrNull()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Failed to read file")
            }
        }
    }

    fun dismissImportPreview() {
        _importPreview.value = null
    }

    fun confirmImport(targetFolderId: String?, onComplete: (Boolean, String) -> Unit) {
        val preview = _importPreview.value ?: return
        viewModelScope.launch {
            _isBusy.value = true
            _busyMessage.value = "Importing note..."
            val result = importExportManager.importNoteFromPreview(preview, targetFolderId)
            _isBusy.value = false
            _busyMessage.value = ""
            _importPreview.value = null
            refreshStorageStats()
            if (result.isSuccess) {
                onComplete(true, "Imported '${result.getOrNull()?.title}' successfully.")
            } else {
                onComplete(false, result.exceptionOrNull()?.message ?: "Failed to import note")
            }
        }
    }

    fun toggleAppLock(enable: Boolean) {
        viewModelScope.launch {
            appLockManager.setAppLockEnabled(enable)
        }
    }

    companion object {
        fun provideFactory(
            backupManager: BackupManager,
            importExportManager: NoteImportExportManager,
            noteRepository: NoteRepository,
            attachmentRepository: AttachmentRepository,
            taskRepository: TaskRepository,
            reminderRepository: ReminderRepository,
            appLockManager: AppLockManager
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return BackupPrivacyViewModel(
                        backupManager = backupManager,
                        importExportManager = importExportManager,
                        noteRepository = noteRepository,
                        attachmentRepository = attachmentRepository,
                        taskRepository = taskRepository,
                        reminderRepository = reminderRepository,
                        appLockManager = appLockManager
                    ) as T
                }
            }
    }
}
