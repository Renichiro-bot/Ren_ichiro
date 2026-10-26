package com.example.core.data.repository

import android.content.Intent
import android.net.Uri
import com.example.core.database.dao.AttachmentDao
import com.example.core.database.entity.AttachmentEntity
import com.example.core.model.Attachment
import com.example.core.storage.AttachmentStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

interface AttachmentRepository {
    fun getAttachmentsForNote(noteId: String): Flow<List<Attachment>>
    fun getAllAttachments(): Flow<List<Attachment>>
    fun getAttachmentCount(): Flow<Int>
    fun getTotalAttachmentSizeBytes(): Flow<Long?>
    suspend fun addAttachment(noteId: String, uri: Uri): Result<Attachment>
    suspend fun deleteAttachment(attachment: Attachment): Result<Unit>
    suspend fun deleteAttachmentsForNote(noteId: String): Result<Unit>
    fun getViewIntent(attachment: Attachment): Intent?
    fun getAttachmentFile(attachment: Attachment): File
}

class AttachmentRepositoryImpl(
    private val attachmentDao: AttachmentDao,
    private val attachmentStorage: AttachmentStorage
) : AttachmentRepository {

    override fun getAttachmentsForNote(noteId: String): Flow<List<Attachment>> {
        return attachmentDao.getAttachmentsForNote(noteId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getAllAttachments(): Flow<List<Attachment>> {
        return attachmentDao.getAllAttachments().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override fun getAttachmentCount(): Flow<Int> {
        return attachmentDao.getAttachmentCount()
    }

    override fun getTotalAttachmentSizeBytes(): Flow<Long?> {
        return attachmentDao.getTotalAttachmentSizeBytes()
    }

    override suspend fun addAttachment(noteId: String, uri: Uri): Result<Attachment> {
        val saveResult = attachmentStorage.saveAttachmentFromUri(noteId, uri)
        if (saveResult.isFailure) {
            return saveResult
        }
        val attachment = saveResult.getOrThrow()
        try {
            attachmentDao.insertAttachment(AttachmentEntity.fromDomainModel(attachment))
            return Result.success(attachment)
        } catch (e: Exception) {
            // Roll back saved file if database insert fails
            attachmentStorage.deleteFile(attachment)
            return Result.failure(e)
        }
    }

    override suspend fun deleteAttachment(attachment: Attachment): Result<Unit> {
        return try {
            attachmentDao.deleteAttachment(attachment.id)
            attachmentStorage.deleteFile(attachment)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAttachmentsForNote(noteId: String): Result<Unit> {
        return try {
            attachmentDao.deleteAttachmentsForNote(noteId)
            attachmentStorage.deleteFilesForNote(noteId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getViewIntent(attachment: Attachment): Intent? {
        return attachmentStorage.createViewIntent(attachment)
    }

    override fun getAttachmentFile(attachment: Attachment): File {
        return attachmentStorage.getFile(attachment)
    }
}
