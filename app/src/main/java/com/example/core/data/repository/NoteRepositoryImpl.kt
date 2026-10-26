package com.example.core.data.repository

import com.example.core.database.dao.FolderDao
import com.example.core.database.dao.NoteDao
import com.example.core.database.dao.TagDao
import com.example.core.database.entity.FolderEntity
import com.example.core.database.entity.NoteTagCrossRef
import com.example.core.database.entity.TagEntity
import com.example.core.database.entity.toDomain
import com.example.core.database.entity.toEntity
import com.example.core.model.ChecklistItem
import com.example.core.model.Folder
import com.example.core.model.Note
import com.example.core.model.NoteColorTheme
import com.example.core.model.Tag
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import com.example.core.database.dao.AttachmentDao
import com.example.core.storage.AttachmentStorage
import kotlinx.coroutines.flow.map
import android.content.Context
import com.example.core.widget.VellumWidgetManager
import kotlinx.coroutines.withContext
import java.util.UUID

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val folderDao: FolderDao? = null,
    private val tagDao: TagDao? = null,
    private val attachmentDao: AttachmentDao? = null,
    private val attachmentStorage: AttachmentStorage? = null,
    private val context: Context? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : NoteRepository {

    constructor(
        noteDao: NoteDao,
        ioDispatcher: CoroutineDispatcher
    ) : this(
        noteDao = noteDao,
        folderDao = null,
        tagDao = null,
        attachmentDao = null,
        attachmentStorage = null,
        context = null,
        ioDispatcher = ioDispatcher
    )

    private fun notifyWidgets() {
        context?.let { VellumWidgetManager.notifyWidgetsDataChanged(it) }
    }

    override fun getActiveNotes(): Flow<List<Note>> {
        return noteDao.getActiveNotesWithTagsStream()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun getArchivedNotes(): Flow<List<Note>> {
        return noteDao.getArchivedNotesWithTagsStream()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun getTrashedNotes(): Flow<List<Note>> {
        return noteDao.getTrashedNotesWithTagsStream()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)
    }

    override fun getNoteByIdStream(id: String): Flow<Note?> {
        return noteDao.getNoteWithTagsByIdStream(id)
            .map { it?.toDomain() }
            .flowOn(ioDispatcher)
    }

    override suspend fun getNoteById(id: String): Note? = withContext(ioDispatcher) {
        noteDao.getNoteWithTagsById(id)?.toDomain()
    }

    override suspend fun saveNote(note: Note): Unit = withContext(ioDispatcher) {
        val existing = noteDao.getNoteById(note.id)
        val noteToSave = if (existing != null) {
            note.copy(
                createdAtEpochMs = existing.createdAtEpochMs,
                updatedAtEpochMs = if (note.updatedAtEpochMs > existing.updatedAtEpochMs) {
                    note.updatedAtEpochMs
                } else {
                    System.currentTimeMillis()
                }
            )
        } else {
            note.copy(
                updatedAtEpochMs = if (note.updatedAtEpochMs > 0) {
                    note.updatedAtEpochMs
                } else {
                    System.currentTimeMillis()
                }
            )
        }
        noteDao.upsertNote(noteToSave.toEntity())

        // Synchronize tags if tagDao is available
        if (tagDao != null) {
            tagDao.clearTagsForNote(note.id)
            for (rawTag in note.tags.distinct()) {
                val cleanTag = cleanTagName(rawTag)
                if (cleanTag.isNotBlank()) {
                    var tagEntity = tagDao.getTagByName(cleanTag)
                    if (tagEntity == null) {
                        val newTag = TagEntity(
                            id = UUID.randomUUID().toString(),
                            name = cleanTag,
                            createdAtEpochMs = System.currentTimeMillis()
                        )
                        tagDao.insertTag(newTag)
                        tagEntity = tagDao.getTagByName(cleanTag) ?: newTag
                    }
                    tagDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId = note.id, tagId = tagEntity.id))
                }
            }
        }
        notifyWidgets()
    }

    // --- Soft Delete (Move to Trash) & Restorations ---

    override suspend fun deleteNote(id: String): Unit = withContext(ioDispatcher) {
        noteDao.updateTrashStatus(id, isTrashed = true, System.currentTimeMillis())
        notifyWidgets()
    }

    override suspend fun deleteNotes(ids: List<String>): Unit = withContext(ioDispatcher) {
        if (ids.isNotEmpty()) {
            noteDao.updateTrashStatusForNotes(ids, isTrashed = true, System.currentTimeMillis())
            notifyWidgets()
        }
    }

    override suspend fun restoreNote(id: String): Unit = withContext(ioDispatcher) {
        val existing = noteDao.getNoteById(id) ?: return@withContext
        // If note belongs to a folder, check if that folder still exists
        val verifiedFolderId = if (existing.folderId != null && folderDao != null) {
            val folder = folderDao.getFolderById(existing.folderId)
            if (folder != null) existing.folderId else null
        } else {
            existing.folderId
        }
        if (verifiedFolderId != existing.folderId) {
            noteDao.updateNoteFolder(id, verifiedFolderId, System.currentTimeMillis())
        }
        noteDao.updateTrashStatus(id, isTrashed = false, System.currentTimeMillis())
        notifyWidgets()
    }

    override suspend fun restoreNotes(ids: List<String>): Unit = withContext(ioDispatcher) {
        ids.forEach { restoreNote(it) }
        notifyWidgets()
    }

    // --- Permanent Deletion (Purge) ---

    override suspend fun deleteNotePermanently(id: String): Unit = withContext(ioDispatcher) {
        tagDao?.clearTagsForNote(id)
        attachmentDao?.deleteAttachmentsForNote(id)
        attachmentStorage?.deleteFilesForNote(id)
        noteDao.deleteNoteById(id)
        notifyWidgets()
    }

    override suspend fun deleteNotesPermanently(ids: List<String>): Unit = withContext(ioDispatcher) {
        if (ids.isNotEmpty()) {
            tagDao?.clearTagsForNotes(ids)
            ids.forEach { id ->
                attachmentDao?.deleteAttachmentsForNote(id)
                attachmentStorage?.deleteFilesForNote(id)
            }
            noteDao.deleteNotesByIds(ids)
            notifyWidgets()
        }
    }

    override suspend fun emptyTrash(): Unit = withContext(ioDispatcher) {
        val trashedNotes = noteDao.getAllNotesSync().filter { it.isTrashed }
        trashedNotes.forEach { note ->
            tagDao?.clearTagsForNote(note.id)
            attachmentDao?.deleteAttachmentsForNote(note.id)
            attachmentStorage?.deleteFilesForNote(note.id)
        }
        noteDao.emptyTrash()
        notifyWidgets()
    }

    // --- Pin & Favorite Operations ---

    override suspend fun togglePin(id: String): Unit = withContext(ioDispatcher) {
        val existing = noteDao.getNoteById(id)
        if (existing != null) {
            val newPinStatus = !existing.isPinned
            noteDao.updatePinStatus(id, newPinStatus, System.currentTimeMillis())
            notifyWidgets()
        }
    }

    override suspend fun setPinStatusForNotes(ids: List<String>, isPinned: Boolean): Unit = withContext(ioDispatcher) {
        if (ids.isNotEmpty()) {
            noteDao.updatePinStatusForNotes(ids, isPinned, System.currentTimeMillis())
            notifyWidgets()
        }
    }

    override suspend fun toggleFavorite(id: String): Unit = withContext(ioDispatcher) {
        val existing = noteDao.getNoteById(id)
        if (existing != null) {
            val newFavStatus = !existing.isFavorite
            noteDao.updateFavoriteStatus(id, newFavStatus, System.currentTimeMillis())
        }
    }

    override suspend fun setFavoriteStatusForNotes(ids: List<String>, isFavorite: Boolean): Unit = withContext(ioDispatcher) {
        if (ids.isNotEmpty()) {
            noteDao.updateFavoriteStatusForNotes(ids, isFavorite, System.currentTimeMillis())
        }
    }

    // --- Archive & Unarchive Operations ---

    override suspend fun archiveNote(id: String): Unit = withContext(ioDispatcher) {
        noteDao.updateArchiveStatus(id, isArchived = true, System.currentTimeMillis())
        notifyWidgets()
    }

    override suspend fun unarchiveNote(id: String): Unit = withContext(ioDispatcher) {
        noteDao.updateArchiveStatus(id, isArchived = false, System.currentTimeMillis())
        notifyWidgets()
    }

    override suspend fun archiveNotes(ids: List<String>): Unit = withContext(ioDispatcher) {
        if (ids.isNotEmpty()) {
            noteDao.updateArchiveStatusForNotes(ids, isArchived = true, System.currentTimeMillis())
            notifyWidgets()
        }
    }

    override suspend fun unarchiveNotes(ids: List<String>): Unit = withContext(ioDispatcher) {
        if (ids.isNotEmpty()) {
            noteDao.updateArchiveStatusForNotes(ids, isArchived = false, System.currentTimeMillis())
            notifyWidgets()
        }
    }

    // --- Checklists ---

    override suspend fun toggleChecklistItem(noteId: String, itemId: String): Unit = withContext(ioDispatcher) {
        val existingEntity = noteDao.getNoteById(noteId) ?: return@withContext
        val domainNote = existingEntity.toDomain()
        val updatedItems = domainNote.checklistItems.map { item ->
            if (item.id == itemId) item.copy(isDone = !item.isDone) else item
        }
        val updatedNote = domainNote.copy(
            checklistItems = updatedItems,
            updatedAtEpochMs = System.currentTimeMillis()
        )
        noteDao.upsertNote(updatedNote.toEntity())
    }

    override suspend fun searchNotes(query: String): Flow<List<Note>> {
        val cleanQuery = query.trim()
        return if (cleanQuery.isBlank()) {
            getActiveNotes()
        } else {
            noteDao.searchNotesWithTagsStream(cleanQuery)
                .map { entities -> entities.map { it.toDomain() } }
                .flowOn(ioDispatcher)
        }
    }

    // --- Folders ---

    override fun getAllFolders(): Flow<List<Folder>> {
        val fDao = folderDao ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return fDao.getAllFoldersStream().map { entities ->
            entities.map { entity ->
                val count = fDao.getActiveNoteCountInFolder(entity.id)
                entity.toDomain(noteCount = count)
            }
        }.flowOn(ioDispatcher)
    }

    override suspend fun createFolder(name: String): Result<Folder> = withContext(ioDispatcher) {
        val fDao = folderDao ?: return@withContext Result.failure(IllegalStateException("FolderDao unavailable"))
        val trimmed = name.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Folder name cannot be empty."))
        }
        if (trimmed.length > 50) {
            return@withContext Result.failure(IllegalArgumentException("Folder name cannot exceed 50 characters."))
        }
        val existing = fDao.getFolderByName(trimmed)
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("A folder named '$trimmed' already exists."))
        }
        val now = System.currentTimeMillis()
        val folder = Folder(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        fDao.upsertFolder(folder.toEntity())
        Result.success(folder)
    }

    override suspend fun renameFolder(id: String, newName: String): Result<Unit> = withContext(ioDispatcher) {
        val fDao = folderDao ?: return@withContext Result.failure(IllegalStateException("FolderDao unavailable"))
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Folder name cannot be empty."))
        }
        val existing = fDao.getFolderByName(trimmed)
        if (existing != null && existing.id != id) {
            return@withContext Result.failure(IllegalArgumentException("A folder named '$trimmed' already exists."))
        }
        fDao.renameFolder(id, trimmed, System.currentTimeMillis())
        Result.success(Unit)
    }

    override suspend fun deleteFolderSafely(id: String): Unit = withContext(ioDispatcher) {
        folderDao?.deleteFolderSafely(id)
    }

    override suspend fun moveNoteToFolder(noteId: String, folderId: String?): Unit = withContext(ioDispatcher) {
        noteDao.updateNoteFolder(noteId, folderId, System.currentTimeMillis())
    }

    override suspend fun moveNotesToFolder(noteIds: List<String>, folderId: String?): Unit = withContext(ioDispatcher) {
        if (noteIds.isNotEmpty()) {
            noteDao.updateNotesFolder(noteIds, folderId, System.currentTimeMillis())
        }
    }

    // --- Tags ---

    override fun getAllTags(): Flow<List<Tag>> {
        val tDao = tagDao ?: return kotlinx.coroutines.flow.flowOf(emptyList())
        return tDao.getAllTagsStream().map { entities ->
            entities.map { entity ->
                val count = tDao.getActiveNoteCountForTag(entity.id)
                entity.toDomain(noteCount = count)
            }
        }.flowOn(ioDispatcher)
    }

    override suspend fun createTag(name: String): Result<Tag> = withContext(ioDispatcher) {
        val tDao = tagDao ?: return@withContext Result.failure(IllegalStateException("TagDao unavailable"))
        val clean = cleanTagName(name)
        if (clean.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Tag name cannot be empty."))
        }
        val existing = tDao.getTagByName(clean)
        if (existing != null) {
            return@withContext Result.success(existing.toDomain())
        }
        val newTag = Tag(
            id = UUID.randomUUID().toString(),
            name = clean,
            createdAtEpochMs = System.currentTimeMillis()
        )
        tDao.insertTag(newTag.toEntity())
        Result.success(newTag)
    }

    override suspend fun deleteTagSafely(id: String): Unit = withContext(ioDispatcher) {
        tagDao?.deleteTagSafely(id)
    }

    override suspend fun addTagToNote(noteId: String, tagName: String): Unit = withContext(ioDispatcher) {
        val tDao = tagDao ?: return@withContext
        val clean = cleanTagName(tagName)
        if (clean.isBlank()) return@withContext
        var tag = tDao.getTagByName(clean)
        if (tag == null) {
            val newTag = TagEntity(id = UUID.randomUUID().toString(), name = clean, createdAtEpochMs = System.currentTimeMillis())
            tDao.insertTag(newTag)
            tag = tDao.getTagByName(clean) ?: newTag
        }
        tDao.insertNoteTagCrossRef(NoteTagCrossRef(noteId = noteId, tagId = tag.id))
    }

    override suspend fun removeTagFromNote(noteId: String, tagName: String): Unit = withContext(ioDispatcher) {
        val tDao = tagDao ?: return@withContext
        val clean = cleanTagName(tagName)
        val tag = tDao.getTagByName(clean) ?: return@withContext
        tDao.removeTagFromNote(noteId, tag.id)
    }

    override suspend fun addTagToNotes(noteIds: List<String>, tagName: String): Unit = withContext(ioDispatcher) {
        noteIds.forEach { addTagToNote(it, tagName) }
    }

    // --- Sample & Maintenance ---

    override suspend fun loadSampleNotes(): Unit = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()

        // Create sample folders
        val workFolder = Folder(id = "folder-work", name = "Work & Architecture", createdAtEpochMs = now, updatedAtEpochMs = now)
        val designFolder = Folder(id = "folder-design", name = "Design System", createdAtEpochMs = now, updatedAtEpochMs = now)
        val readingFolder = Folder(id = "folder-reading", name = "Reading & Books", createdAtEpochMs = now, updatedAtEpochMs = now)

        folderDao?.let {
            it.upsertFolder(workFolder.toEntity())
            it.upsertFolder(designFolder.toEntity())
            it.upsertFolder(readingFolder.toEntity())
        }

        // Create sample tags
        val sampleTags = listOf("architecture", "planning", "design", "books", "editorial", "monolith")
        sampleTags.forEach { tagName ->
            createTag(tagName)
        }

        val samples = generateCuratedSampleNotes(workFolder.id, designFolder.id, readingFolder.id)
        samples.forEach { saveNote(it) }
    }

    override suspend fun clearAllNotes(): Unit = withContext(ioDispatcher) {
        noteDao.deleteAllNotes()
        notifyWidgets()
    }

    private fun cleanTagName(raw: String): String {
        return raw.trim().removePrefix("#").trim()
    }

    companion object {
        fun generateCuratedSampleNotes(
            workFolderId: String? = null,
            designFolderId: String? = null,
            readingFolderId: String? = null
        ): List<Note> {
            val now = System.currentTimeMillis()
            return listOf(
                Note(
                    id = "sample-1",
                    title = "Architecture Roadmap & Milestones",
                    content = "Level 5 delivers robust folders, relational tags, favorites, pinning, archive, soft-delete trash with restore, and safe Room migration MIGRATION_1_2.",
                    isPinned = true,
                    isFavorite = true,
                    isChecklist = false,
                    folderId = workFolderId,
                    tags = listOf("architecture", "planning"),
                    colorTheme = NoteColorTheme.DEFAULT,
                    createdAtEpochMs = now - 3600000L,
                    updatedAtEpochMs = now - 600000L
                ),
                Note(
                    id = "sample-2",
                    title = "Design System Quality Audit",
                    content = "",
                    isPinned = true,
                    isFavorite = true,
                    isChecklist = true,
                    folderId = designFolderId,
                    tags = listOf("design", "editorial"),
                    checklistItems = listOf(
                        ChecklistItem(text = "WCAG 2.1 AA Contrast Checked (>= 4.5:1)", isDone = true),
                        ChecklistItem(text = "48dp Interactive Touch Targets Enforced", isDone = true),
                        ChecklistItem(text = "Edge-to-Edge System Bar Insets Respected", isDone = true),
                        ChecklistItem(text = "AMOLED Pure Black & Dynamic Color Support", isDone = true),
                        ChecklistItem(text = "Responsive 2-Column Grid / 1-Column List Toggle", isDone = true),
                        ChecklistItem(text = "Accessible Content Descriptions on All Icons", isDone = true)
                    ),
                    colorTheme = NoteColorTheme.SAGE,
                    createdAtEpochMs = now - 7200000L,
                    updatedAtEpochMs = now - 1800000L
                ),
                Note(
                    id = "sample-3",
                    title = "Typography & Spatial Hierarchy",
                    content = "Built upon an exact 4dp spatial grid (8dp, 16dp, 24dp, 32dp). Display headings feature negative tracking (-0.5sp) for tight editorial headlines, paired with 24sp body line-height for effortless reading.",
                    isPinned = false,
                    isFavorite = false,
                    isChecklist = false,
                    folderId = designFolderId,
                    tags = listOf("design", "editorial"),
                    colorTheme = NoteColorTheme.SAND,
                    createdAtEpochMs = now - 86400000L,
                    updatedAtEpochMs = now - 86400000L
                ),
                Note(
                    id = "sample-4",
                    title = "Essential Software Books",
                    content = "",
                    isPinned = false,
                    isFavorite = true,
                    isChecklist = true,
                    folderId = readingFolderId,
                    tags = listOf("books"),
                    checklistItems = listOf(
                        ChecklistItem(text = "Designing Data-Intensive Applications", isDone = true),
                        ChecklistItem(text = "Clean Architecture by Robert C. Martin", isDone = true),
                        ChecklistItem(text = "Refactoring by Martin Fowler", isDone = false),
                        ChecklistItem(text = "The Pragmatic Programmer", isDone = false)
                    ),
                    colorTheme = NoteColorTheme.LAVENDER,
                    createdAtEpochMs = now - 172800000L,
                    updatedAtEpochMs = now - 172800000L
                ),
                Note(
                    id = "sample-5",
                    title = "Local Database Architecture",
                    content = "Data flows strictly via Unidirectional Data Flow:\n• UI → ViewModel → Repository → Room DAO → SQLite\n• Reactive streams: SQLite → DAO Flow → StateFlow → Compose collectAsStateWithLifecycle\n• Zero database queries on the main UI thread.",
                    isPinned = false,
                    isFavorite = false,
                    isChecklist = false,
                    folderId = workFolderId,
                    tags = listOf("architecture"),
                    colorTheme = NoteColorTheme.OCEAN,
                    createdAtEpochMs = now - 259200000L,
                    updatedAtEpochMs = now - 259200000L
                ),
                Note(
                    id = "sample-6",
                    title = "Tactile Monolith Design Notes",
                    content = "The physical metaphor of digital Vellum: carbon obsidian base layers, delicate amber honey accents, and subtle 1dp structural borders that provide physical depth without visual clutter.",
                    isPinned = false,
                    isFavorite = false,
                    isChecklist = false,
                    folderId = designFolderId,
                    tags = listOf("monolith", "design"),
                    colorTheme = NoteColorTheme.TERRACOTTA,
                    createdAtEpochMs = now - 345600000L,
                    updatedAtEpochMs = now - 345600000L
                )
            )
        }
    }

    override suspend fun getAllNotesSync(): List<Note> = withContext(ioDispatcher) {
        noteDao.getAllNotesSync().map { it.toDomain() }
    }

    override suspend fun getAllFoldersSync(): List<Folder> = withContext(ioDispatcher) {
        folderDao?.getAllFoldersSync()?.map { it.toDomain() } ?: emptyList()
    }

    override suspend fun getAllTagsSync(): List<Tag> = withContext(ioDispatcher) {
        tagDao?.getAllTagsSync()?.map { it.toDomain() } ?: emptyList()
    }
}
