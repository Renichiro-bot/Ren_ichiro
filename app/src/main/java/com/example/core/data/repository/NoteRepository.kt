package com.example.core.data.repository

import com.example.core.model.Folder
import com.example.core.model.Note
import com.example.core.model.Tag
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    // --- Notes Streams & Operations ---
    fun getActiveNotes(): Flow<List<Note>>
    fun getArchivedNotes(): Flow<List<Note>>
    fun getTrashedNotes(): Flow<List<Note>>
    fun getNoteByIdStream(id: String): Flow<Note?>
    suspend fun getNoteById(id: String): Note?
    suspend fun saveNote(note: Note)
    suspend fun searchNotes(query: String): Flow<List<Note>>

    // --- Soft Delete (Move to Trash) & Restorations ---
    suspend fun deleteNote(id: String)
    suspend fun deleteNotes(ids: List<String>)
    suspend fun restoreNote(id: String)
    suspend fun restoreNotes(ids: List<String>)

    // --- Permanent Deletion (Purge) ---
    suspend fun deleteNotePermanently(id: String)
    suspend fun deleteNotesPermanently(ids: List<String>)
    suspend fun emptyTrash()

    // --- Pin & Favorite Operations ---
    suspend fun togglePin(id: String)
    suspend fun setPinStatusForNotes(ids: List<String>, isPinned: Boolean)
    suspend fun toggleFavorite(id: String)
    suspend fun setFavoriteStatusForNotes(ids: List<String>, isFavorite: Boolean)

    // --- Archive & Unarchive Operations ---
    suspend fun archiveNote(id: String)
    suspend fun unarchiveNote(id: String)
    suspend fun archiveNotes(ids: List<String>)
    suspend fun unarchiveNotes(ids: List<String>)

    // --- Checklists ---
    suspend fun toggleChecklistItem(noteId: String, itemId: String)

    // --- Folders ---
    fun getAllFolders(): Flow<List<Folder>>
    suspend fun createFolder(name: String): Result<Folder>
    suspend fun renameFolder(id: String, newName: String): Result<Unit>
    suspend fun deleteFolderSafely(id: String)
    suspend fun moveNoteToFolder(noteId: String, folderId: String?)
    suspend fun moveNotesToFolder(noteIds: List<String>, folderId: String?)

    // --- Tags ---
    fun getAllTags(): Flow<List<Tag>>
    suspend fun createTag(name: String): Result<Tag>
    suspend fun deleteTagSafely(id: String)
    suspend fun addTagToNote(noteId: String, tagName: String)
    suspend fun removeTagFromNote(noteId: String, tagName: String)
    suspend fun addTagToNotes(noteIds: List<String>, tagName: String)

    // --- Sample & Maintenance ---
    suspend fun loadSampleNotes()
    suspend fun clearAllNotes()
    suspend fun getAllNotesSync(): List<Note>
    suspend fun getAllFoldersSync(): List<Folder>
    suspend fun getAllTagsSync(): List<Tag>
}
