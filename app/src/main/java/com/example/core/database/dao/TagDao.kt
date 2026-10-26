package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.core.database.entity.NoteTagCrossRef
import com.example.core.database.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    fun getAllTagsStream(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllTags(): List<TagEntity>

    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAllTagsSync(): List<TagEntity>

    @Query("SELECT * FROM note_tag_cross_ref")
    suspend fun getAllCrossRefsSync(): List<NoteTagCrossRef>

    @Query("SELECT * FROM tags WHERE id = :id LIMIT 1")
    suspend fun getTagById(id: String): TagEntity?

    @Query("SELECT * FROM tags WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun getTagByName(name: String): TagEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("DELETE FROM tags WHERE id = :id")
    suspend fun deleteTagById(id: String)

    @Query("DELETE FROM tags")
    suspend fun deleteAllTags()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: NoteTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTagCrossRef(crossRef: NoteTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNoteTagCrossRefs(crossRefs: List<NoteTagCrossRef>)

    @Query("DELETE FROM note_tag_cross_ref WHERE note_id = :noteId AND tag_id = :tagId")
    suspend fun removeTagFromNote(noteId: String, tagId: String)

    @Query("DELETE FROM note_tag_cross_ref WHERE note_id = :noteId")
    suspend fun clearTagsForNote(noteId: String)

    @Query("DELETE FROM note_tag_cross_ref WHERE note_id IN (:noteIds)")
    suspend fun clearTagsForNotes(noteIds: List<String>)

    @Query("DELETE FROM note_tag_cross_ref WHERE tag_id = :tagId")
    suspend fun clearNoteRefsForTag(tagId: String)

    @Query("""
        SELECT t.* FROM tags t
        INNER JOIN note_tag_cross_ref ref ON t.id = ref.tag_id
        WHERE ref.note_id = :noteId
        ORDER BY t.name COLLATE NOCASE ASC
    """)
    suspend fun getTagsForNote(noteId: String): List<TagEntity>

    @Query("""
        SELECT COUNT(DISTINCT ref.note_id) FROM note_tag_cross_ref ref 
        INNER JOIN notes n ON ref.note_id = n.id 
        WHERE ref.tag_id = :tagId AND n.is_trashed = 0
    """)
    suspend fun getActiveNoteCountForTag(tagId: String): Int

    /**
     * Deletes a tag safely by clearing cross-references first, preserving notes.
     */
    @Transaction
    suspend fun deleteTagSafely(tagId: String) {
        clearNoteRefsForTag(tagId)
        deleteTagById(tagId)
    }
}
