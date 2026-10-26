package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.core.database.entity.NoteEntity
import com.example.core.database.entity.NoteWithTagsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Transaction
    @Query(
        """
        SELECT * FROM notes 
        WHERE is_archived = 0 AND is_trashed = 0 
        ORDER BY is_pinned DESC, updated_at DESC
        """
    )
    fun getActiveNotesWithTagsStream(): Flow<List<NoteWithTagsEntity>>

    @Query(
        """
        SELECT * FROM notes 
        WHERE is_archived = 0 AND is_trashed = 0 
        ORDER BY is_pinned DESC, updated_at DESC
        """
    )
    fun getActiveNotesStream(): Flow<List<NoteEntity>>

    @Transaction
    @Query("SELECT * FROM notes WHERE is_archived = 1 AND is_trashed = 0 ORDER BY updated_at DESC")
    fun getArchivedNotesWithTagsStream(): Flow<List<NoteWithTagsEntity>>

    @Transaction
    @Query("SELECT * FROM notes WHERE is_trashed = 1 ORDER BY updated_at DESC")
    fun getTrashedNotesWithTagsStream(): Flow<List<NoteWithTagsEntity>>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteWithTagsByIdStream(id: String): Flow<NoteWithTagsEntity?>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun getNoteByIdStream(id: String): Flow<NoteEntity?>

    @Transaction
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteWithTagsById(id: String): NoteWithTagsEntity?

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: String): NoteEntity?

    @Query("SELECT * FROM notes")
    suspend fun getAllNotesSync(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNote(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotes(notes: List<NoteEntity>)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET is_pinned = :isPinned, updated_at = :updatedAt WHERE id = :id")
    suspend fun updatePinStatus(id: String, isPinned: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET is_pinned = :isPinned, updated_at = :updatedAt WHERE id IN (:ids)")
    suspend fun updatePinStatusForNotes(ids: List<String>, isPinned: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET is_favorite = :isFavorite, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET is_favorite = :isFavorite, updated_at = :updatedAt WHERE id IN (:ids)")
    suspend fun updateFavoriteStatusForNotes(ids: List<String>, isFavorite: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET is_archived = :isArchived, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateArchiveStatus(id: String, isArchived: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET is_archived = :isArchived, updated_at = :updatedAt WHERE id IN (:ids)")
    suspend fun updateArchiveStatusForNotes(ids: List<String>, isArchived: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET is_trashed = :isTrashed, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateTrashStatus(id: String, isTrashed: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET is_trashed = :isTrashed, updated_at = :updatedAt WHERE id IN (:ids)")
    suspend fun updateTrashStatusForNotes(ids: List<String>, isTrashed: Boolean, updatedAt: Long)

    @Query("UPDATE notes SET folder_id = :folderId, updated_at = :updatedAt WHERE id = :id")
    suspend fun updateNoteFolder(id: String, folderId: String?, updatedAt: Long)

    @Query("UPDATE notes SET folder_id = :folderId, updated_at = :updatedAt WHERE id IN (:ids)")
    suspend fun updateNotesFolder(ids: List<String>, folderId: String?, updatedAt: Long)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: String)

    @Query("DELETE FROM notes WHERE id IN (:ids)")
    suspend fun deleteNotesByIds(ids: List<String>)

    @Query("DELETE FROM notes WHERE is_trashed = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Transaction
    @Query(
        """
        SELECT * FROM notes 
        WHERE is_archived = 0 AND is_trashed = 0 AND (
            title LIKE '%' || :query || '%' OR 
            content LIKE '%' || :query || '%' OR
            checklist_json LIKE '%' || :query || '%'
        )
        ORDER BY is_pinned DESC, updated_at DESC
        """
    )
    fun searchNotesWithTagsStream(query: String): Flow<List<NoteWithTagsEntity>>

    @Query(
        """
        SELECT * FROM notes 
        WHERE is_archived = 0 AND is_trashed = 0 AND (
            title LIKE '%' || :query || '%' OR 
            content LIKE '%' || :query || '%' OR
            checklist_json LIKE '%' || :query || '%'
        )
        ORDER BY is_pinned DESC, updated_at DESC
        """
    )
    fun searchNotesStream(query: String): Flow<List<NoteEntity>>

    @Query("SELECT COUNT(*) FROM notes WHERE is_archived = 0 AND is_trashed = 0")
    suspend fun getActiveNotesCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE is_archived = 1 AND is_trashed = 0")
    suspend fun getArchivedNotesCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE is_trashed = 1")
    suspend fun getTrashedNotesCount(): Int
}
