package com.example.core.backup

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.withTransaction
import com.example.core.database.VellumDatabase
import com.example.core.database.entity.AttachmentEntity
import com.example.core.database.entity.FolderEntity
import com.example.core.database.entity.NoteEntity
import com.example.core.database.entity.NoteTagCrossRef
import com.example.core.database.entity.TagEntity
import com.example.core.model.Reminder
import com.example.core.notification.ReminderScheduler
import com.example.core.storage.AttachmentStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class BackupManager(
    private val context: Context,
    private val database: VellumDatabase,
    private val attachmentStorage: AttachmentStorage
) {

    /**
     * Creates a verified ZIP backup containing manifest.json, data.json, and all attachment files,
     * then streams it to destinationUri.
     */
    suspend fun createBackup(destinationUri: Uri): Result<BackupManifest> = withContext(Dispatchers.IO) {
        val tempBackupFile = File(context.cacheDir, "vellum_backup_${System.currentTimeMillis()}.tmp.zip")
        try {
            // 1. Gather all local data safely from database
            val notes = database.noteDao().getAllNotesSync()
            val folders = database.folderDao().getAllFoldersSync()
            val tags = database.tagDao().getAllTagsSync()
            val crossRefs = database.tagDao().getAllCrossRefsSync()
            val tasks = database.taskDao().getAllTasksSync()
            val reminders = database.reminderDao().getAllRemindersSync()
            val events = database.calendarEventDao().getAllEventsSync()
            val templates = database.templateDao().getAllTemplatesSync()
            val attachments = database.attachmentDao().getAllAttachmentsSync()

            var totalAttachmentBytes = 0L
            attachments.forEach { att ->
                val f = File(context.filesDir, att.localRelativePath)
                if (f.exists()) {
                    totalAttachmentBytes += f.length()
                }
            }

            val manifest = BackupManifest(
                formatVersion = BackupManifest.CURRENT_FORMAT_VERSION,
                appId = context.packageName,
                appVersionName = "1.0",
                createdAtEpochMs = System.currentTimeMillis(),
                noteCount = notes.size,
                folderCount = folders.size,
                tagCount = tags.size,
                noteTagCrossRefCount = crossRefs.size,
                taskCount = tasks.size,
                reminderCount = reminders.size,
                calendarEventCount = events.size,
                templateCount = templates.size,
                attachmentCount = attachments.size,
                totalAttachmentSizeBytes = totalAttachmentBytes
            )

            val backupData = BackupData(
                manifest = manifest,
                notes = notes,
                folders = folders,
                tags = tags,
                noteTagCrossRefs = crossRefs,
                tasks = tasks,
                reminders = reminders,
                calendarEvents = events,
                templates = templates,
                attachments = attachments
            )

            val manifestJson = BackupSerializer.serializeManifest(manifest)
            val dataJson = BackupSerializer.serializeData(backupData)

            // 2. Write to temporary ZIP file
            ZipOutputStream(BufferedOutputStream(FileOutputStream(tempBackupFile))).use { zos ->
                // Write manifest.json
                val manifestEntry = ZipEntry("manifest.json")
                zos.putNextEntry(manifestEntry)
                zos.write(manifestJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Write data.json
                val dataEntry = ZipEntry("data.json")
                zos.putNextEntry(dataEntry)
                zos.write(dataJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Write attachment files
                val buffer = ByteArray(8192)
                attachments.forEach { att ->
                    val file = File(context.filesDir, att.localRelativePath)
                    if (file.exists() && file.isFile) {
                        val entryName = "attachments/${att.id}/${file.name}"
                        val zipEntry = ZipEntry(entryName)
                        zos.putNextEntry(zipEntry)
                        FileInputStream(file).use { fis ->
                            var read: Int
                            while (fis.read(buffer).also { read = it } != -1) {
                                zos.write(buffer, 0, read)
                            }
                        }
                        zos.closeEntry()
                    }
                }
            }

            // 3. Verify backup integrity before reporting success or writing to destination
            verifyZipArchive(tempBackupFile, manifest)

            // 4. Stream verified backup to user-selected destination
            context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
                FileInputStream(tempBackupFile).use { inStream ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (inStream.read(buffer).also { read = it } != -1) {
                        outStream.write(buffer, 0, read)
                    }
                    outStream.flush()
                }
            } ?: throw IllegalStateException("Failed to open output stream for destination: $destinationUri")

            Result.success(manifest)
        } catch (e: Exception) {
            Log.e("BackupManager", "Backup failed", e)
            Result.failure(e)
        } finally {
            if (tempBackupFile.exists()) {
                tempBackupFile.delete()
            }
        }
    }

    /**
     * Inspects and validates a user-selected backup file without modifying the database.
     * Enforces Zip-Slip path traversal protection.
     */
    suspend fun validateBackup(sourceUri: Uri): BackupValidationResult = withContext(Dispatchers.IO) {
        val tempStagingFile = File(context.cacheDir, "validate_stage_${System.currentTimeMillis()}.zip")
        try {
            // Stage incoming stream to local temporary file
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempStagingFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            } ?: return@withContext BackupValidationResult.Error("Could not open backup file URI")

            if (tempStagingFile.length() == 0L) {
                return@withContext BackupValidationResult.Error("Selected backup file is empty (0 bytes)")
            }

            ZipFile(tempStagingFile).use { zip ->
                // Check for Zip-Slip attacks (path traversal)
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.contains("..") || entry.name.startsWith("/") || entry.name.startsWith("\\")) {
                        return@withContext BackupValidationResult.Error("Security violation: backup contains unsafe path '${entry.name}'")
                    }
                }

                // Check manifest.json
                val manifestEntry = zip.getEntry("manifest.json")
                    ?: return@withContext BackupValidationResult.Error("Invalid backup: missing 'manifest.json'")
                val manifestJson = zip.getInputStream(manifestEntry).bufferedReader().use { it.readText() }
                val manifest = BackupSerializer.deserializeManifest(manifestJson)

                if (manifest.formatVersion > BackupManifest.CURRENT_FORMAT_VERSION) {
                    return@withContext BackupValidationResult.Error(
                        "Unsupported backup format version: ${manifest.formatVersion} (App supports up to ${BackupManifest.CURRENT_FORMAT_VERSION})"
                    )
                }

                // Check data.json
                val dataEntry = zip.getEntry("data.json")
                    ?: return@withContext BackupValidationResult.Error("Invalid backup: missing 'data.json'")
                val dataJson = zip.getInputStream(dataEntry).bufferedReader().use { it.readText() }
                val backupData = BackupSerializer.deserializeData(dataJson, manifest)

                // Validate attachments present in zip
                var attachmentsFound = 0
                val missingAttachments = mutableListOf<String>()
                backupData.attachments.forEach { att ->
                    val fileName = File(att.localRelativePath).name
                    val expectedPath = "attachments/${att.id}/$fileName"
                    if (zip.getEntry(expectedPath) != null) {
                        attachmentsFound++
                    } else {
                        missingAttachments.add(att.fileName)
                    }
                }

                val warnings = mutableListOf<String>()
                if (missingAttachments.isNotEmpty()) {
                    warnings.add("${missingAttachments.size} attachment files are missing from the backup archive")
                }

                BackupValidationResult.Success(
                    manifest = manifest,
                    data = backupData,
                    attachmentFilesPresent = attachmentsFound,
                    warnings = warnings
                )
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Validation error", e)
            BackupValidationResult.Error("Backup validation failed: ${e.localizedMessage ?: e.javaClass.simpleName}", e)
        } finally {
            if (tempStagingFile.exists()) {
                tempStagingFile.delete()
            }
        }
    }

    /**
     * Executes backup restoration using specified mode (MERGE or REPLACE).
     */
    suspend fun restoreBackup(
        sourceUri: Uri,
        mode: RestoreMode
    ): RestoreResult = withContext(Dispatchers.IO) {
        val tempStagingFile = File(context.cacheDir, "restore_stage_${System.currentTimeMillis()}.zip")
        try {
            // Stage incoming backup
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempStagingFile).use { output ->
                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                    }
                    output.flush()
                }
            } ?: return@withContext RestoreResult(isSuccess = false, mode = mode, message = "Could not read backup file")

            ZipFile(tempStagingFile).use { zip ->
                val manifestEntry = zip.getEntry("manifest.json")
                    ?: return@withContext RestoreResult(isSuccess = false, mode = mode, message = "Missing manifest.json")
                val manifest = BackupSerializer.deserializeManifest(
                    zip.getInputStream(manifestEntry).bufferedReader().use { it.readText() }
                )

                val dataEntry = zip.getEntry("data.json")
                    ?: return@withContext RestoreResult(isSuccess = false, mode = mode, message = "Missing data.json")
                val backupData = BackupSerializer.deserializeData(
                    zip.getInputStream(dataEntry).bufferedReader().use { it.readText() },
                    manifest
                )

                when (mode) {
                    RestoreMode.MERGE -> executeMergeRestore(zip, backupData)
                    RestoreMode.REPLACE -> executeReplaceRestore(zip, backupData)
                }
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Restore execution failed", e)
            RestoreResult(
                isSuccess = false,
                mode = mode,
                message = "Restore failed: ${e.localizedMessage ?: e.javaClass.simpleName}",
                error = e
            )
        } finally {
            if (tempStagingFile.exists()) {
                tempStagingFile.delete()
            }
        }
    }

    private suspend fun executeMergeRestore(zip: ZipFile, data: BackupData): RestoreResult {
        // 1. Folders: Check by unique name (case-insensitive) to prevent duplicates
        val existingFolders = database.folderDao().getAllFoldersSync()
        val folderIdMap = mutableMapOf<String, String>() // backupFolderId -> resolvedFolderId

        var restoredFolders = 0
        data.folders.forEach { backupFolder ->
            val existingByName = existingFolders.find { it.name.trim().equals(backupFolder.name.trim(), ignoreCase = true) }
            val existingById = existingFolders.find { it.id == backupFolder.id }

            val targetFolderId = when {
                existingByName != null -> {
                    folderIdMap[backupFolder.id] = existingByName.id
                    existingByName.id
                }
                existingById != null -> {
                    folderIdMap[backupFolder.id] = existingById.id
                    if (backupFolder.updatedAtEpochMs > existingById.updatedAtEpochMs) {
                        database.folderDao().updateFolder(backupFolder)
                    }
                    existingById.id
                }
                else -> {
                    database.folderDao().insertFolder(backupFolder)
                    folderIdMap[backupFolder.id] = backupFolder.id
                    restoredFolders++
                    backupFolder.id
                }
            }
        }

        // 2. Tags: Match by unique name (case-insensitive)
        val existingTags = database.tagDao().getAllTagsSync()
        val tagIdMap = mutableMapOf<String, String>() // backupTagId -> resolvedTagId

        var restoredTags = 0
        data.tags.forEach { backupTag ->
            val existingByName = existingTags.find { it.name.trim().equals(backupTag.name.trim(), ignoreCase = true) }
            if (existingByName != null) {
                tagIdMap[backupTag.id] = existingByName.id
            } else {
                database.tagDao().insertTag(backupTag)
                tagIdMap[backupTag.id] = backupTag.id
                restoredTags++
            }
        }

        // 3. Notes: Merge with deterministic conflict rule: Keep whichever has newer updatedAt
        val existingNotes = database.noteDao().getAllNotesSync()
        val existingNotesMap = existingNotes.associateBy { it.id }

        var restoredNotes = 0
        data.notes.forEach { backupNote ->
            val resolvedFolderId = if (backupNote.folderId != null) {
                folderIdMap[backupNote.folderId] ?: backupNote.folderId
            } else null

            val noteToApply = backupNote.copy(folderId = resolvedFolderId)
            val existing = existingNotesMap[backupNote.id]

            if (existing == null) {
                database.noteDao().insertNote(noteToApply)
                restoredNotes++
            } else if (noteToApply.updatedAtEpochMs > existing.updatedAtEpochMs) {
                database.noteDao().updateNote(noteToApply)
                restoredNotes++
            }
        }

        // 4. NoteTagCrossRefs
        data.noteTagCrossRefs.forEach { ref ->
            val resolvedTagId = tagIdMap[ref.tagId] ?: ref.tagId
            val crossRef = NoteTagCrossRef(noteId = ref.noteId, tagId = resolvedTagId)
            database.tagDao().insertCrossRef(crossRef)
        }

        // 5. Tasks
        val existingTasks = database.taskDao().getAllTasksSync().associateBy { it.id }
        var restoredTasks = 0
        data.tasks.forEach { backupTask ->
            val existing = existingTasks[backupTask.id]
            if (existing == null) {
                database.taskDao().insertTask(backupTask)
                restoredTasks++
            } else if (backupTask.updatedAt > existing.updatedAt) {
                database.taskDao().updateTask(backupTask)
                restoredTasks++
            }
        }

        // 6. Reminders
        val existingReminders = database.reminderDao().getAllRemindersSync().associateBy { it.id }
        var restoredReminders = 0
        data.reminders.forEach { backupReminder ->
            val existing = existingReminders[backupReminder.id]
            if (existing == null) {
                database.reminderDao().insertReminder(backupReminder)
                restoredReminders++
            } else if (backupReminder.createdAt >= existing.createdAt) {
                database.reminderDao().insertReminder(backupReminder)
                restoredReminders++
            }
        }

        // 7. Calendar Events
        val existingEvents = database.calendarEventDao().getAllEventsSync().associateBy { it.id }
        var restoredEvents = 0
        data.calendarEvents.forEach { backupEvent ->
            val existing = existingEvents[backupEvent.id]
            if (existing == null) {
                database.calendarEventDao().insertEvent(backupEvent)
                restoredEvents++
            } else if (backupEvent.updatedAt > existing.updatedAt) {
                database.calendarEventDao().updateEvent(backupEvent)
                restoredEvents++
            }
        }

        // 8. Templates (never overwrite built-in templates)
        val existingTemplates = database.templateDao().getAllTemplatesSync().associateBy { it.id }
        var restoredTemplates = 0
        data.templates.forEach { backupTemplate ->
            if (backupTemplate.isBuiltIn == 0) {
                val existing = existingTemplates[backupTemplate.id]
                if (existing == null) {
                    database.templateDao().insertTemplate(backupTemplate)
                    restoredTemplates++
                } else if (backupTemplate.updatedAt > existing.updatedAt) {
                    database.templateDao().updateTemplate(backupTemplate)
                    restoredTemplates++
                }
            }
        }

        // 9. Attachments: Extract attachment files into private storage and insert DB record
        var restoredAttachments = 0
        data.attachments.forEach { att ->
            val targetFile = File(context.filesDir, att.localRelativePath)
            if (!targetFile.exists()) {
                targetFile.parentFile?.mkdirs()
                val entryName = "attachments/${att.id}/${targetFile.name}"
                val zipEntry = zip.getEntry(entryName)
                if (zipEntry != null) {
                    zip.getInputStream(zipEntry).use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
            database.attachmentDao().insertAttachment(att)
            restoredAttachments++
        }

        // 10. Reconcile reminders with notification scheduler
        reconcileReminders()

        return RestoreResult(
            isSuccess = true,
            mode = RestoreMode.MERGE,
            restoredNotesCount = restoredNotes,
            restoredFoldersCount = restoredFolders,
            restoredTagsCount = restoredTags,
            restoredTasksCount = restoredTasks,
            restoredRemindersCount = restoredReminders,
            restoredCalendarEventsCount = restoredEvents,
            restoredTemplatesCount = restoredTemplates,
            restoredAttachmentsCount = restoredAttachments,
            message = "Backup successfully merged with existing data."
        )
    }

    private suspend fun executeReplaceRestore(zip: ZipFile, data: BackupData): RestoreResult {
        // Safe replacement: Wrap in transaction
        database.withTransaction {
            // 1. Clear existing database records
            database.noteDao().emptyTrash()
            database.noteDao().deleteAllNotes()
            database.folderDao().deleteAllFolders()
            database.tagDao().deleteAllTags()
            database.taskDao().deleteAllTasks()
            database.reminderDao().deleteAllReminders()
            database.calendarEventDao().deleteAllEvents()
            database.attachmentDao().deleteAllAttachments()

            // 2. Insert backup records
            data.folders.forEach { database.folderDao().insertFolder(it) }
            data.tags.forEach { database.tagDao().insertTag(it) }
            data.notes.forEach { database.noteDao().insertNote(it) }
            data.noteTagCrossRefs.forEach { database.tagDao().insertCrossRef(it) }
            data.tasks.forEach { database.taskDao().insertTask(it) }
            data.reminders.forEach { database.reminderDao().insertReminder(it) }
            data.calendarEvents.forEach { database.calendarEventDao().insertEvent(it) }
            data.templates.forEach {
                if (it.isBuiltIn == 0) database.templateDao().insertTemplate(it)
            }
            data.attachments.forEach { database.attachmentDao().insertAttachment(it) }
        }

        // 3. Extract all attachment files
        var restoredAttachments = 0
        data.attachments.forEach { att ->
            val targetFile = File(context.filesDir, att.localRelativePath)
            targetFile.parentFile?.mkdirs()
            val entryName = "attachments/${att.id}/${targetFile.name}"
            val zipEntry = zip.getEntry(entryName)
            if (zipEntry != null) {
                zip.getInputStream(zipEntry).use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                restoredAttachments++
            }
        }

        // 4. Reconcile active reminders
        reconcileReminders()

        return RestoreResult(
            isSuccess = true,
            mode = RestoreMode.REPLACE,
            restoredNotesCount = data.notes.size,
            restoredFoldersCount = data.folders.size,
            restoredTagsCount = data.tags.size,
            restoredTasksCount = data.tasks.size,
            restoredRemindersCount = data.reminders.size,
            restoredCalendarEventsCount = data.calendarEvents.size,
            restoredTemplatesCount = data.templates.size,
            restoredAttachmentsCount = restoredAttachments,
            message = "Application data successfully restored from backup."
        )
    }

    private suspend fun reconcileReminders() {
        try {
            val activeReminders = database.reminderDao().getUpcomingActiveRemindersList(System.currentTimeMillis())
            activeReminders.forEach { entity ->
                ReminderScheduler.scheduleReminder(context, entity.toDomainModel())
            }
        } catch (e: Exception) {
            Log.e("BackupManager", "Reminder reconciliation error", e)
        }
    }

    private fun verifyZipArchive(zipFile: File, manifest: BackupManifest) {
        ZipFile(zipFile).use { zip ->
            val manifestEntry = zip.getEntry("manifest.json")
                ?: throw IllegalStateException("Backup archive verification failed: manifest.json is missing")
            val dataEntry = zip.getEntry("data.json")
                ?: throw IllegalStateException("Backup archive verification failed: data.json is missing")
            if (manifestEntry.size == 0L || dataEntry.size == 0L) {
                throw IllegalStateException("Backup archive verification failed: archive files are empty")
            }
        }
    }
}
