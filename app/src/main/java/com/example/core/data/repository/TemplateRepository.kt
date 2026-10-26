package com.example.core.data.repository

import com.example.core.database.dao.TemplateDao
import com.example.core.database.entity.TemplateEntity
import com.example.core.model.NoteTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

interface TemplateRepository {
    fun getAllTemplates(): Flow<List<NoteTemplate>>
    suspend fun getTemplateById(id: String): NoteTemplate?
    suspend fun createCustomTemplate(
        name: String,
        description: String = "",
        iconName: String = "description",
        defaultTitle: String = "",
        contentTemplate: String = "",
        isChecklist: Boolean = false,
        checklistItemsTemplate: List<String> = emptyList()
    ): NoteTemplate
    suspend fun deleteTemplate(id: String)
}

class TemplateRepositoryImpl(
    private val templateDao: TemplateDao
) : TemplateRepository {

    override fun getAllTemplates(): Flow<List<NoteTemplate>> {
        return templateDao.getAllTemplates().map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun getTemplateById(id: String): NoteTemplate? {
        return templateDao.getTemplateById(id)?.toDomainModel()
    }

    override suspend fun createCustomTemplate(
        name: String,
        description: String,
        iconName: String,
        defaultTitle: String,
        contentTemplate: String,
        isChecklist: Boolean,
        checklistItemsTemplate: List<String>
    ): NoteTemplate {
        val trimmedName = name.trim()
        require(trimmedName.isNotBlank()) { "Template name cannot be blank" }

        val template = NoteTemplate(
            id = UUID.randomUUID().toString(),
            name = trimmedName,
            description = description.trim(),
            iconName = iconName,
            defaultTitle = defaultTitle.trim(),
            contentTemplate = contentTemplate,
            isChecklist = isChecklist,
            checklistItemsTemplate = checklistItemsTemplate.filter { it.isNotBlank() },
            isBuiltIn = false,
            createdAtEpochMs = System.currentTimeMillis(),
            updatedAtEpochMs = System.currentTimeMillis()
        )
        templateDao.insertTemplate(TemplateEntity.fromDomainModel(template))
        return template
    }

    override suspend fun deleteTemplate(id: String) {
        val existing = templateDao.getTemplateById(id)
        if (existing != null && existing.isBuiltIn == 0) {
            templateDao.deleteTemplate(id)
        }
    }
}
