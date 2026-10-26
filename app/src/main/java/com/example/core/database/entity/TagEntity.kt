package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.Tag

@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class TagEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAtEpochMs: Long
)

fun TagEntity.toDomain(noteCount: Int = 0): Tag = Tag(
    id = id,
    name = name,
    createdAtEpochMs = createdAtEpochMs,
    noteCount = noteCount
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    createdAtEpochMs = createdAtEpochMs
)
