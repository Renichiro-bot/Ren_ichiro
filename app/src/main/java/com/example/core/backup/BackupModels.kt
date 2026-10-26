package com.example.core.backup

import com.example.core.database.entity.AttachmentEntity
import com.example.core.database.entity.CalendarEventEntity
import com.example.core.database.entity.FolderEntity
import com.example.core.database.entity.NoteEntity
import com.example.core.database.entity.NoteTagCrossRef
import com.example.core.database.entity.ReminderEntity
import com.example.core.database.entity.TagEntity
import com.example.core.database.entity.TaskEntity
import com.example.core.database.entity.TemplateEntity

data class BackupManifest(
    val formatVersion: Int = CURRENT_FORMAT_VERSION,
    val appId: String = "com.aistudio.vellumnotes",
    val appVersionName: String = "1.0",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val noteCount: Int = 0,
    val folderCount: Int = 0,
    val tagCount: Int = 0,
    val noteTagCrossRefCount: Int = 0,
    val taskCount: Int = 0,
    val reminderCount: Int = 0,
    val calendarEventCount: Int = 0,
    val templateCount: Int = 0,
    val attachmentCount: Int = 0,
    val totalAttachmentSizeBytes: Long = 0L
) {
    companion object {
        const val CURRENT_FORMAT_VERSION = 1
    }
}

data class BackupData(
    val manifest: BackupManifest,
    val notes: List<NoteEntity> = emptyList(),
    val folders: List<FolderEntity> = emptyList(),
    val tags: List<TagEntity> = emptyList(),
    val noteTagCrossRefs: List<NoteTagCrossRef> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val calendarEvents: List<CalendarEventEntity> = emptyList(),
    val templates: List<TemplateEntity> = emptyList(),
    val attachments: List<AttachmentEntity> = emptyList()
)

sealed interface BackupValidationResult {
    data class Success(
        val manifest: BackupManifest,
        val data: BackupData,
        val attachmentFilesPresent: Int,
        val warnings: List<String> = emptyList()
    ) : BackupValidationResult

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : BackupValidationResult
}

enum class RestoreMode {
    MERGE,
    REPLACE
}

data class RestoreResult(
    val isSuccess: Boolean,
    val mode: RestoreMode,
    val restoredNotesCount: Int = 0,
    val restoredFoldersCount: Int = 0,
    val restoredTagsCount: Int = 0,
    val restoredTasksCount: Int = 0,
    val restoredRemindersCount: Int = 0,
    val restoredCalendarEventsCount: Int = 0,
    val restoredTemplatesCount: Int = 0,
    val restoredAttachmentsCount: Int = 0,
    val message: String = "",
    val error: Throwable? = null
)
