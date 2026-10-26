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
import org.json.JSONArray
import org.json.JSONObject

object BackupSerializer {

    fun serializeManifest(manifest: BackupManifest): String {
        val json = JSONObject().apply {
            put("formatVersion", manifest.formatVersion)
            put("appId", manifest.appId)
            put("appVersionName", manifest.appVersionName)
            put("createdAtEpochMs", manifest.createdAtEpochMs)
            put("noteCount", manifest.noteCount)
            put("folderCount", manifest.folderCount)
            put("tagCount", manifest.tagCount)
            put("noteTagCrossRefCount", manifest.noteTagCrossRefCount)
            put("taskCount", manifest.taskCount)
            put("reminderCount", manifest.reminderCount)
            put("calendarEventCount", manifest.calendarEventCount)
            put("templateCount", manifest.templateCount)
            put("attachmentCount", manifest.attachmentCount)
            put("totalAttachmentSizeBytes", manifest.totalAttachmentSizeBytes)
        }
        return json.toString(2)
    }

    fun deserializeManifest(jsonString: String): BackupManifest {
        val obj = JSONObject(jsonString)
        return BackupManifest(
            formatVersion = obj.optInt("formatVersion", 1),
            appId = obj.optString("appId", "com.aistudio.vellumnotes"),
            appVersionName = obj.optString("appVersionName", "1.0"),
            createdAtEpochMs = obj.optLong("createdAtEpochMs", System.currentTimeMillis()),
            noteCount = obj.optInt("noteCount", 0),
            folderCount = obj.optInt("folderCount", 0),
            tagCount = obj.optInt("tagCount", 0),
            noteTagCrossRefCount = obj.optInt("noteTagCrossRefCount", 0),
            taskCount = obj.optInt("taskCount", 0),
            reminderCount = obj.optInt("reminderCount", 0),
            calendarEventCount = obj.optInt("calendarEventCount", 0),
            templateCount = obj.optInt("templateCount", 0),
            attachmentCount = obj.optInt("attachmentCount", 0),
            totalAttachmentSizeBytes = obj.optLong("totalAttachmentSizeBytes", 0L)
        )
    }

    fun serializeData(data: BackupData): String {
        val root = JSONObject()

        // 1. Notes
        val notesArray = JSONArray()
        data.notes.forEach { note ->
            val obj = JSONObject().apply {
                put("id", note.id)
                put("title", note.title)
                put("content", note.content)
                put("created_at", note.createdAtEpochMs)
                put("updated_at", note.updatedAtEpochMs)
                put("is_pinned", note.isPinned)
                put("is_archived", note.isArchived)
                put("is_trashed", note.isTrashed)
                put("is_checklist", note.isChecklist)
                put("checklist_json", note.checklistJson)
                put("is_favorite", note.isFavorite)
                put("color_theme", note.colorTheme)
                if (note.folderId != null) {
                    put("folder_id", note.folderId)
                }
            }
            notesArray.put(obj)
        }
        root.put("notes", notesArray)

        // 2. Folders
        val foldersArray = JSONArray()
        data.folders.forEach { folder ->
            val obj = JSONObject().apply {
                put("id", folder.id)
                put("name", folder.name)
                put("created_at", folder.createdAtEpochMs)
                put("updated_at", folder.updatedAtEpochMs)
            }
            foldersArray.put(obj)
        }
        root.put("folders", foldersArray)

        // 3. Tags
        val tagsArray = JSONArray()
        data.tags.forEach { tag ->
            val obj = JSONObject().apply {
                put("id", tag.id)
                put("name", tag.name)
                put("created_at", tag.createdAtEpochMs)
            }
            tagsArray.put(obj)
        }
        root.put("tags", tagsArray)

        // 4. NoteTagCrossRefs
        val crossRefArray = JSONArray()
        data.noteTagCrossRefs.forEach { ref ->
            val obj = JSONObject().apply {
                put("note_id", ref.noteId)
                put("tag_id", ref.tagId)
            }
            crossRefArray.put(obj)
        }
        root.put("note_tag_cross_refs", crossRefArray)

        // 5. Tasks
        val tasksArray = JSONArray()
        data.tasks.forEach { task ->
            val obj = JSONObject().apply {
                put("id", task.id)
                put("title", task.title)
                put("description", task.description)
                put("is_completed", task.isCompleted)
                put("priority", task.priority)
                if (task.dueDateEpochMs != null) {
                    put("due_date_epoch_ms", task.dueDateEpochMs)
                }
                if (task.associatedNoteId != null) {
                    put("associated_note_id", task.associatedNoteId)
                }
                put("created_at", task.createdAt)
                put("updated_at", task.updatedAt)
            }
            tasksArray.put(obj)
        }
        root.put("tasks", tasksArray)

        // 6. Reminders
        val remindersArray = JSONArray()
        data.reminders.forEach { reminder ->
            val obj = JSONObject().apply {
                put("id", reminder.id)
                put("target_type", reminder.targetType)
                put("target_id", reminder.targetId)
                put("title", reminder.title)
                put("message", reminder.message)
                put("trigger_at_epoch_ms", reminder.triggerAtEpochMs)
                put("is_enabled", reminder.isEnabled)
                put("created_at", reminder.createdAt)
            }
            remindersArray.put(obj)
        }
        root.put("reminders", remindersArray)

        // 7. Calendar Events
        val calendarArray = JSONArray()
        data.calendarEvents.forEach { event ->
            val obj = JSONObject().apply {
                put("id", event.id)
                put("title", event.title)
                put("description", event.description)
                put("start_epoch_ms", event.startEpochMs)
                put("end_epoch_ms", event.endEpochMs)
                put("is_all_day", event.isAllDay)
                if (event.associatedTaskId != null) {
                    put("associated_task_id", event.associatedTaskId)
                }
                if (event.associatedNoteId != null) {
                    put("associated_note_id", event.associatedNoteId)
                }
                put("created_at", event.createdAt)
                put("updated_at", event.updatedAt)
            }
            calendarArray.put(obj)
        }
        root.put("calendar_events", calendarArray)

        // 8. Templates
        val templatesArray = JSONArray()
        data.templates.forEach { tmpl ->
            val obj = JSONObject().apply {
                put("id", tmpl.id)
                put("name", tmpl.name)
                put("description", tmpl.description)
                put("icon_name", tmpl.iconName)
                put("default_title", tmpl.defaultTitle)
                put("content_template", tmpl.contentTemplate)
                put("is_checklist", tmpl.isChecklist)
                if (tmpl.checklistTemplateJson != null) {
                    put("checklist_template_json", tmpl.checklistTemplateJson)
                }
                put("is_built_in", tmpl.isBuiltIn)
                put("created_at", tmpl.createdAt)
                put("updated_at", tmpl.updatedAt)
            }
            templatesArray.put(obj)
        }
        root.put("templates", templatesArray)

        // 9. Attachments
        val attachmentsArray = JSONArray()
        data.attachments.forEach { att ->
            val obj = JSONObject().apply {
                put("id", att.id)
                put("note_id", att.noteId)
                put("file_name", att.fileName)
                put("mime_type", att.mimeType)
                put("file_size_bytes", att.fileSizeBytes)
                put("local_relative_path", att.localRelativePath)
                put("created_at", att.createdAtEpochMs)
            }
            attachmentsArray.put(obj)
        }
        root.put("attachments", attachmentsArray)

        return root.toString(2)
    }

    fun deserializeData(jsonString: String, manifest: BackupManifest): BackupData {
        val root = JSONObject(jsonString)

        val notes = mutableListOf<NoteEntity>()
        val notesArray = root.optJSONArray("notes")
        if (notesArray != null) {
            for (i in 0 until notesArray.length()) {
                val obj = notesArray.getJSONObject(i)
                notes.add(
                    NoteEntity(
                        id = obj.getString("id"),
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        isPinned = obj.optBoolean("is_pinned", false),
                        isFavorite = obj.optBoolean("is_favorite", false),
                        isArchived = obj.optBoolean("is_archived", false),
                        isTrashed = obj.optBoolean("is_trashed", false),
                        isChecklist = obj.optBoolean("is_checklist", false),
                        checklistJson = obj.optString("checklist_json", ""),
                        folderId = if (obj.has("folder_id") && !obj.isNull("folder_id")) obj.optString("folder_id") else null,
                        colorTheme = obj.optString("color_theme", "default"),
                        createdAtEpochMs = obj.optLong("created_at", System.currentTimeMillis()),
                        updatedAtEpochMs = obj.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        val folders = mutableListOf<FolderEntity>()
        val foldersArray = root.optJSONArray("folders")
        if (foldersArray != null) {
            for (i in 0 until foldersArray.length()) {
                val obj = foldersArray.getJSONObject(i)
                folders.add(
                    FolderEntity(
                        id = obj.getString("id"),
                        name = obj.optString("name", "Unnamed Folder"),
                        createdAtEpochMs = obj.optLong("created_at", System.currentTimeMillis()),
                        updatedAtEpochMs = obj.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        val tags = mutableListOf<TagEntity>()
        val tagsArray = root.optJSONArray("tags")
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                val obj = tagsArray.getJSONObject(i)
                tags.add(
                    TagEntity(
                        id = obj.getString("id"),
                        name = obj.optString("name", "tag"),
                        createdAtEpochMs = obj.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }
        }

        val crossRefs = mutableListOf<NoteTagCrossRef>()
        val crossRefArray = root.optJSONArray("note_tag_cross_refs")
        if (crossRefArray != null) {
            for (i in 0 until crossRefArray.length()) {
                val obj = crossRefArray.getJSONObject(i)
                crossRefs.add(
                    NoteTagCrossRef(
                        noteId = obj.getString("note_id"),
                        tagId = obj.getString("tag_id")
                    )
                )
            }
        }

        val tasks = mutableListOf<TaskEntity>()
        val tasksArray = root.optJSONArray("tasks")
        if (tasksArray != null) {
            for (i in 0 until tasksArray.length()) {
                val obj = tasksArray.getJSONObject(i)
                tasks.add(
                    TaskEntity(
                        id = obj.getString("id"),
                        title = obj.optString("title", "Task"),
                        description = obj.optString("description", ""),
                        isCompleted = obj.optInt("is_completed", 0),
                        priority = obj.optString("priority", "none"),
                        dueDateEpochMs = if (obj.has("due_date_epoch_ms") && !obj.isNull("due_date_epoch_ms")) obj.optLong("due_date_epoch_ms") else null,
                        associatedNoteId = if (obj.has("associated_note_id") && !obj.isNull("associated_note_id")) obj.optString("associated_note_id") else null,
                        createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        val reminders = mutableListOf<ReminderEntity>()
        val remindersArray = root.optJSONArray("reminders")
        if (remindersArray != null) {
            for (i in 0 until remindersArray.length()) {
                val obj = remindersArray.getJSONObject(i)
                reminders.add(
                    ReminderEntity(
                        id = obj.getString("id"),
                        targetType = obj.optString("target_type", "note"),
                        targetId = obj.optString("target_id", ""),
                        title = obj.optString("title", "Reminder"),
                        message = obj.optString("message", ""),
                        triggerAtEpochMs = obj.optLong("trigger_at_epoch_ms", System.currentTimeMillis()),
                        isEnabled = obj.optInt("is_enabled", 1),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }
        }

        val calendarEvents = mutableListOf<CalendarEventEntity>()
        val calendarArray = root.optJSONArray("calendar_events")
        if (calendarArray != null) {
            for (i in 0 until calendarArray.length()) {
                val obj = calendarArray.getJSONObject(i)
                calendarEvents.add(
                    CalendarEventEntity(
                        id = obj.getString("id"),
                        title = obj.optString("title", "Event"),
                        description = obj.optString("description", ""),
                        startEpochMs = obj.optLong("start_epoch_ms", System.currentTimeMillis()),
                        endEpochMs = obj.optLong("end_epoch_ms", System.currentTimeMillis() + 3600_000L),
                        isAllDay = obj.optInt("is_all_day", 0),
                        associatedTaskId = if (obj.has("associated_task_id") && !obj.isNull("associated_task_id")) obj.optString("associated_task_id") else null,
                        associatedNoteId = if (obj.has("associated_note_id") && !obj.isNull("associated_note_id")) obj.optString("associated_note_id") else null,
                        createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        val templates = mutableListOf<TemplateEntity>()
        val templatesArray = root.optJSONArray("templates")
        if (templatesArray != null) {
            for (i in 0 until templatesArray.length()) {
                val obj = templatesArray.getJSONObject(i)
                templates.add(
                    TemplateEntity(
                        id = obj.getString("id"),
                        name = obj.optString("name", "Template"),
                        description = obj.optString("description", ""),
                        iconName = obj.optString("icon_name", "description"),
                        defaultTitle = obj.optString("default_title", ""),
                        contentTemplate = obj.optString("content_template", ""),
                        isChecklist = obj.optInt("is_checklist", 0),
                        checklistTemplateJson = if (obj.has("checklist_template_json") && !obj.isNull("checklist_template_json")) obj.optString("checklist_template_json") else null,
                        isBuiltIn = obj.optInt("is_built_in", 0),
                        createdAt = obj.optLong("created_at", System.currentTimeMillis()),
                        updatedAt = obj.optLong("updated_at", System.currentTimeMillis())
                    )
                )
            }
        }

        val attachments = mutableListOf<AttachmentEntity>()
        val attachmentsArray = root.optJSONArray("attachments")
        if (attachmentsArray != null) {
            for (i in 0 until attachmentsArray.length()) {
                val obj = attachmentsArray.getJSONObject(i)
                attachments.add(
                    AttachmentEntity(
                        id = obj.getString("id"),
                        noteId = obj.getString("note_id"),
                        fileName = obj.optString("file_name", "attachment"),
                        mimeType = obj.optString("mime_type", "application/octet-stream"),
                        fileSizeBytes = obj.optLong("file_size_bytes", 0L),
                        localRelativePath = obj.optString("local_relative_path", ""),
                        createdAtEpochMs = obj.optLong("created_at", System.currentTimeMillis())
                    )
                )
            }
        }

        return BackupData(
            manifest = manifest,
            notes = notes,
            folders = folders,
            tags = tags,
            noteTagCrossRefs = crossRefs,
            tasks = tasks,
            reminders = reminders,
            calendarEvents = calendarEvents,
            templates = templates,
            attachments = attachments
        )
    }
}
