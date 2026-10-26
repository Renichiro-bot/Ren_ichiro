package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "note_tag_cross_ref",
    primaryKeys = ["note_id", "tag_id"],
    indices = [
        Index(value = ["tag_id"]),
        Index(value = ["note_id"])
    ]
)
data class NoteTagCrossRef(
    @ColumnInfo(name = "note_id")
    val noteId: String,

    @ColumnInfo(name = "tag_id")
    val tagId: String
)
