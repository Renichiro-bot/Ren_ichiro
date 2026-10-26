package com.example.core.model

import java.util.UUID

data class NoteTemplate(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String,
    val iconName: String = "description",
    val defaultTitle: String = "",
    val contentTemplate: String = "",
    val isChecklist: Boolean = false,
    val checklistItemsTemplate: List<String> = emptyList(),
    val isBuiltIn: Boolean = false,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
    val updatedAtEpochMs: Long = System.currentTimeMillis()
)
