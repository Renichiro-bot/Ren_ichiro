package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.ChecklistItem
import com.example.core.model.Note
import com.example.core.model.NoteColorTheme
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["updated_at"]),
        Index(value = ["is_pinned"]),
        Index(value = ["is_favorite"]),
        Index(value = ["is_archived"]),
        Index(value = ["is_trashed"]),
        Index(value = ["folder_id"])
    ]
)
data class NoteEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "content")
    val content: String,

    @ColumnInfo(name = "is_pinned", defaultValue = "0")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "is_favorite", defaultValue = "0")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "is_archived", defaultValue = "0")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "is_trashed", defaultValue = "0")
    val isTrashed: Boolean = false,

    @ColumnInfo(name = "is_checklist", defaultValue = "0")
    val isChecklist: Boolean = false,

    @ColumnInfo(name = "checklist_json", defaultValue = "''")
    val checklistJson: String = "",

    @ColumnInfo(name = "folder_id")
    val folderId: String? = null,

    @ColumnInfo(name = "color_theme", defaultValue = "'default'")
    val colorTheme: String = "default",

    @ColumnInfo(name = "created_at")
    val createdAtEpochMs: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMs: Long
)

fun checklistItemsToJson(items: List<ChecklistItem>): String {
    if (items.isEmpty()) return ""
    val array = JSONArray()
    items.forEach { item ->
        val obj = JSONObject()
        obj.put("id", item.id)
        obj.put("text", item.text)
        obj.put("isDone", item.isDone)
        array.put(obj)
    }
    return array.toString()
}

fun jsonToChecklistItems(json: String): List<ChecklistItem> {
    if (json.isBlank()) return emptyList()
    return try {
        val array = JSONArray(json)
        val list = mutableListOf<ChecklistItem>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                ChecklistItem(
                    id = obj.optString("id", UUID.randomUUID().toString()),
                    text = obj.optString("text", ""),
                    isDone = obj.optBoolean("isDone", false)
                )
            )
        }
        list
    } catch (e: Exception) {
        emptyList()
    }
}

fun NoteEntity.toDomain(tags: List<String> = emptyList()): Note = Note(
    id = id,
    title = title,
    content = content,
    isPinned = isPinned,
    isFavorite = isFavorite,
    isArchived = isArchived,
    isTrashed = isTrashed,
    isChecklist = isChecklist,
    checklistItems = jsonToChecklistItems(checklistJson),
    folderId = folderId,
    tags = tags,
    colorTheme = NoteColorTheme.fromId(colorTheme),
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    isPinned = isPinned,
    isFavorite = isFavorite,
    isArchived = isArchived,
    isTrashed = isTrashed,
    isChecklist = isChecklist,
    checklistJson = checklistItemsToJson(checklistItems),
    folderId = folderId,
    colorTheme = colorTheme.id,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)
