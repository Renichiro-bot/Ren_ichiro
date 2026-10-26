package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.core.database.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    fun getAllFoldersStream(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllFolders(): List<FolderEntity>

    @Query("SELECT * FROM folders ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllFoldersSync(): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE id = :id LIMIT 1")
    suspend fun getFolderById(id: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getFolderByName(name: String): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @androidx.room.Update
    suspend fun updateFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFolder(folder: FolderEntity)

    @Query("UPDATE folders SET name = :newName, updated_at = :updatedAt WHERE id = :id")
    suspend fun renameFolder(id: String, newName: String, updatedAt: Long)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolderById(id: String)

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    @Query("SELECT COUNT(*) FROM notes WHERE folder_id = :folderId AND is_trashed = 0")
    suspend fun getActiveNoteCountInFolder(folderId: String): Int

    @Query("UPDATE notes SET folder_id = NULL WHERE folder_id = :folderId")
    suspend fun unfileNotesFromFolder(folderId: String)

    /**
     * Deletes a folder safely by first unfiling all assigned notes (setting folder_id = NULL)
     * so that notes are NEVER deleted when their folder is removed.
     */
    @Transaction
    suspend fun deleteFolderSafely(folderId: String) {
        unfileNotesFromFolder(folderId)
        deleteFolderById(folderId)
    }
}
