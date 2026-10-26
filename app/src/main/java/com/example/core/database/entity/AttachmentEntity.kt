package com.example.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.model.Attachment

@Entity(
    tableName = "attachments",
    indices = [
        Index(value = ["note_id"])
    ]
)
data class AttachmentEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "note_id")
    val noteId: String,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "mime_type")
    val mimeType: String,

    @ColumnInfo(name = "file_size_bytes")
    val fileSizeBytes: Long,

    @ColumnInfo(name = "local_relative_path")
    val localRelativePath: String,

    @ColumnInfo(name = "created_at")
    val createdAtEpochMs: Long
) {
    fun toDomainModel(): Attachment = Attachment(
        id = id,
        noteId = noteId,
        fileName = fileName,
        mimeType = mimeType,
        fileSizeBytes = fileSizeBytes,
        localRelativePath = localRelativePath,
        createdAtEpochMs = createdAtEpochMs
    )

    companion object {
        fun fromDomainModel(domain: Attachment): AttachmentEntity = AttachmentEntity(
            id = domain.id,
            noteId = domain.noteId,
            fileName = domain.fileName,
            mimeType = domain.mimeType,
            fileSizeBytes = domain.fileSizeBytes,
            localRelativePath = domain.localRelativePath,
            createdAtEpochMs = domain.createdAtEpochMs
        )
    }
}
