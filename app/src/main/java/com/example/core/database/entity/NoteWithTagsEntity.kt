package com.example.core.database.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.core.model.Note

data class NoteWithTagsEntity(
    @Embedded
    val note: NoteEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = NoteTagCrossRef::class,
            parentColumn = "note_id",
            entityColumn = "tag_id"
        )
    )
    val tags: List<TagEntity> = emptyList()
)

fun NoteWithTagsEntity.toDomain(): Note = note.toDomain(tags = tags.map { it.name })
