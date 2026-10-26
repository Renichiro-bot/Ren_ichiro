package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.database.entity.AttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentDao {

    @Query("SELECT * FROM attachments WHERE note_id = :noteId ORDER BY created_at DESC")
    fun getAttachmentsForNote(noteId: String): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments WHERE note_id = :noteId ORDER BY created_at DESC")
    suspend fun getAttachmentsForNoteSync(noteId: String): List<AttachmentEntity>

    @Query("SELECT * FROM attachments ORDER BY created_at DESC")
    fun getAllAttachments(): Flow<List<AttachmentEntity>>

    @Query("SELECT * FROM attachments ORDER BY created_at DESC")
    suspend fun getAllAttachmentsSync(): List<AttachmentEntity>

    @Query("SELECT * FROM attachments WHERE id = :id LIMIT 1")
    suspend fun getAttachmentById(id: String): AttachmentEntity?

    @Query("SELECT COUNT(*) FROM attachments")
    fun getAttachmentCount(): Flow<Int>

    @Query("SELECT SUM(file_size_bytes) FROM attachments")
    fun getTotalAttachmentSizeBytes(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: AttachmentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<AttachmentEntity>)

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachment(id: String)

    @Query("DELETE FROM attachments WHERE note_id = :noteId")
    suspend fun deleteAttachmentsForNote(noteId: String)

    @Query("DELETE FROM attachments")
    suspend fun deleteAllAttachments()
}
