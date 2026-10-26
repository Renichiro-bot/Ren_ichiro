package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.NoteTemplate
import org.json.JSONArray

@Entity(tableName = "note_templates")
data class TemplateEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description", defaultValue = "''")
    val description: String = "",

    @ColumnInfo(name = "icon_name", defaultValue = "'description'")
    val iconName: String = "description",

    @ColumnInfo(name = "default_title", defaultValue = "''")
    val defaultTitle: String = "",

    @ColumnInfo(name = "content_template", defaultValue = "''")
    val contentTemplate: String = "",

    @ColumnInfo(name = "is_checklist", defaultValue = "0")
    val isChecklist: Int = 0,

    @ColumnInfo(name = "checklist_template_json", defaultValue = "NULL")
    val checklistTemplateJson: String? = null,

    @ColumnInfo(name = "is_built_in", defaultValue = "0")
    val isBuiltIn: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toDomainModel(): NoteTemplate {
        val checklistItems = parseChecklistJson(checklistTemplateJson)
        return NoteTemplate(
            id = id,
            name = name,
            description = description,
            iconName = iconName,
            defaultTitle = defaultTitle,
            contentTemplate = contentTemplate,
            isChecklist = isChecklist == 1,
            checklistItemsTemplate = checklistItems,
            isBuiltIn = isBuiltIn == 1,
            createdAtEpochMs = createdAt,
            updatedAtEpochMs = updatedAt
        )
    }

    companion object {
        fun fromDomainModel(template: NoteTemplate): TemplateEntity {
            return TemplateEntity(
                id = template.id,
                name = template.name,
                description = template.description,
                iconName = template.iconName,
                defaultTitle = template.defaultTitle,
                contentTemplate = template.contentTemplate,
                isChecklist = if (template.isChecklist) 1 else 0,
                checklistTemplateJson = serializeChecklist(template.checklistItemsTemplate),
                isBuiltIn = if (template.isBuiltIn) 1 else 0,
                createdAt = template.createdAtEpochMs,
                updatedAt = template.updatedAtEpochMs
            )
        }

        private fun parseChecklistJson(jsonStr: String?): List<String> {
            if (jsonStr.isNullOrBlank()) return emptyList()
            return try {
                val array = JSONArray(jsonStr)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                list
            } catch (e: Exception) {
                emptyList()
            }
        }

        private fun serializeChecklist(items: List<String>): String? {
            if (items.isEmpty()) return null
            val array = JSONArray()
            items.forEach { array.put(it) }
            return array.toString()
        }
    }
}
