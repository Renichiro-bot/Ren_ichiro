package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.core.model.Folder

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "created_at")
    val createdAtEpochMs: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMs: Long
)

fun FolderEntity.toDomain(noteCount: Int = 0): Folder = Folder(
    id = id,
    name = name,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
    noteCount = noteCount
)

fun Folder.toEntity(): FolderEntity = FolderEntity(
    id = id,
    name = name,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs
)
