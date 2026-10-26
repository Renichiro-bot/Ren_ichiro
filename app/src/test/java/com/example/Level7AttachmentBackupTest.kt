package com.example

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.core.backup.BackupData
import com.example.core.backup.BackupManager
import com.example.core.backup.BackupManifest
import com.example.core.backup.BackupSerializer
import com.example.core.backup.BackupValidationResult
import com.example.core.backup.NoteImportExportManager
import com.example.core.backup.RestoreMode
import com.example.core.database.VellumDatabase
import com.example.core.database.entity.AttachmentEntity
import com.example.core.database.entity.FolderEntity
import com.example.core.database.entity.NoteEntity
import com.example.core.database.entity.TagEntity
import com.example.core.model.Attachment
import com.example.core.model.ChecklistItem
import com.example.core.model.Note
import com.example.core.model.NoteColorTheme
import com.example.core.storage.AttachmentStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Level7AttachmentBackupTest {

    private lateinit var context: Context
    private lateinit var database: VellumDatabase
    private lateinit var attachmentStorage: AttachmentStorage
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, VellumDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        attachmentStorage = AttachmentStorage(context)
        backupManager = BackupManager(context, database, attachmentStorage)
    }

    @After
    fun tearDown() {
        database.close()
    }

    // --- 1. Room Migration 3 to 4 Verification ---
    @Test
    fun migration_3_to_4_createsAttachmentsTableWithoutDataLoss() {
        val dbName = "test_migration_3_4.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(3) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create Version 3 Schema (Notes, Folders, Tags, Tasks, Reminders, Events, Templates)
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS notes (
                            id TEXT NOT NULL PRIMARY KEY,
                            title TEXT NOT NULL,
                            content TEXT NOT NULL,
                            is_pinned INTEGER NOT NULL DEFAULT 0,
                            is_favorite INTEGER NOT NULL DEFAULT 0,
                            is_archived INTEGER NOT NULL DEFAULT 0,
                            is_trashed INTEGER NOT NULL DEFAULT 0,
                            is_checklist INTEGER NOT NULL DEFAULT 0,
                            checklist_json TEXT NOT NULL DEFAULT '',
                            folder_id TEXT,
                            color_theme TEXT NOT NULL DEFAULT 'default',
                            created_at INTEGER NOT NULL,
                            updated_at INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val helper = FrameworkSQLiteOpenHelperFactory().create(config)
        val v3Db = helper.writableDatabase

        // Insert a note in v3
        val now = System.currentTimeMillis()
        v3Db.execSQL(
            "INSERT INTO notes (id, title, content, created_at, updated_at) VALUES ('n1', 'Important Note', 'Preserve this note', $now, $now)"
        )

        // Apply MIGRATION_3_4 directly
        VellumDatabase.MIGRATION_3_4.migrate(v3Db)

        // 1. Verify existing note survived
        val cursor = v3Db.query("SELECT * FROM notes WHERE id = 'n1'")
        assertTrue("Note n1 should survive migration 3->4", cursor.moveToFirst())
        assertEquals("Important Note", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        cursor.close()

        // 2. Verify attachments table was created and can receive records
        v3Db.execSQL(
            """
            INSERT INTO attachments (id, note_id, file_name, mime_type, file_size_bytes, local_relative_path, created_at)
            VALUES ('att_test', 'n1', 'spec.pdf', 'application/pdf', 2048, 'attachments/n1/att_test.pdf', $now)
            """.trimIndent()
        )
        val attCursor = v3Db.query("SELECT * FROM attachments WHERE id = 'att_test'")
        assertTrue("Attachment record should be queried successfully", attCursor.moveToFirst())
        assertEquals("spec.pdf", attCursor.getString(attCursor.getColumnIndexOrThrow("file_name")))
        assertEquals(2048L, attCursor.getLong(attCursor.getColumnIndexOrThrow("file_size_bytes")))
        attCursor.close()

        // 3. Verify index exists
        val indexCursor = v3Db.query("PRAGMA index_list('attachments')")
        var hasIndex = false
        while (indexCursor.moveToNext()) {
            val indexName = indexCursor.getString(indexCursor.getColumnIndexOrThrow("name"))
            if (indexName == "index_attachments_note_id") {
                hasIndex = true
            }
        }
        indexCursor.close()
        assertTrue("index_attachments_note_id must be created", hasIndex)

        v3Db.close()
        context.deleteDatabase(dbName)
    }

    // --- 2. Attachment DAO and Model Operations ---
    @Test
    fun attachmentDao_crudOperationsWorkReliably() {
        runBlocking {
            val noteId = UUID.randomUUID().toString()
            val att1 = AttachmentEntity(
                id = "att1",
                noteId = noteId,
                fileName = "screenshot.png",
                mimeType = "image/png",
                fileSizeBytes = 1024L,
                localRelativePath = "attachments/$noteId/att1.png",
                createdAtEpochMs = System.currentTimeMillis()
            )
            val att2 = AttachmentEntity(
                id = "att2",
                noteId = noteId,
                fileName = "document.pdf",
                mimeType = "application/pdf",
                fileSizeBytes = 5000L,
                localRelativePath = "attachments/$noteId/att2.pdf",
                createdAtEpochMs = System.currentTimeMillis()
            )

            database.attachmentDao().insertAttachment(att1)
            database.attachmentDao().insertAttachment(att2)

            val attachments = database.attachmentDao().getAttachmentsForNote(noteId).first()
            assertEquals(2, attachments.size)
            assertTrue(attachments.any { it.fileName == "screenshot.png" && it.toDomainModel().isImage })
            assertTrue(attachments.any { it.fileName == "document.pdf" && it.toDomainModel().isPdf })

            // Delete single attachment
            database.attachmentDao().deleteAttachment("att1")
            val remaining = database.attachmentDao().getAttachmentsForNote(noteId).first()
            assertEquals(1, remaining.size)
            assertEquals("document.pdf", remaining[0].fileName)
        }
    }

    // --- 3. Path Traversal (Zip-Slip) Attack Rejection ---
    @Test
    fun backupManager_rejectsMaliciousZipSlipArchive() {
        runBlocking {
            val maliciousZipFile = File(context.cacheDir, "evil_zip_slip.zip")
            val manifest = BackupManifest(
                formatVersion = 1,
                appId = "com.aistudio.vellumnotes",
                appVersionName = "1.0",
                createdAtEpochMs = System.currentTimeMillis(),
                noteCount = 0,
                folderCount = 0,
                tagCount = 0,
                noteTagCrossRefCount = 0,
                taskCount = 0,
                reminderCount = 0,
                calendarEventCount = 0,
                templateCount = 0,
                attachmentCount = 1,
                totalAttachmentSizeBytes = 100L
            )
            ZipOutputStream(FileOutputStream(maliciousZipFile)).use { zos ->
                // Add a normal manifest
                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write(BackupSerializer.serializeManifest(manifest).toByteArray())
                zos.closeEntry()

                // Add an empty data.json
                val data = BackupData(
                    manifest = manifest,
                    notes = emptyList(),
                    folders = emptyList(),
                    tags = emptyList(),
                    noteTagCrossRefs = emptyList(),
                    tasks = emptyList(),
                    reminders = emptyList(),
                    calendarEvents = emptyList(),
                    templates = emptyList(),
                    attachments = emptyList()
                )
                zos.putNextEntry(ZipEntry("data.json"))
                zos.write(BackupSerializer.serializeData(data).toByteArray())
                zos.closeEntry()

                // Malicious entry attempting path traversal outside sandbox
                zos.putNextEntry(ZipEntry("attachments/../../../etc/malicious.sh"))
                zos.write("echo pwned".toByteArray())
                zos.closeEntry()
            }

            val validation = backupManager.validateBackup(Uri.fromFile(maliciousZipFile))
            assertTrue("Archive with path traversal must be rejected!", validation is BackupValidationResult.Error)
            val errorMessage = (validation as BackupValidationResult.Error).message
            assertTrue(
                "Errors should mention unsafe path or security violation",
                errorMessage.contains("unsafe path", ignoreCase = true) || errorMessage.contains("Security violation", ignoreCase = true)
            )

            maliciousZipFile.delete()
        }
    }

    // --- 4. Backup Creation and Manifest Validation ---
    @Test
    fun backupManager_createsValidBackupZipAndManifest() {
        runBlocking {
            // Insert sample note, folder, tag
            val folder = FolderEntity("f1", "Projects", System.currentTimeMillis(), System.currentTimeMillis())
            database.folderDao().insertFolder(folder)

            val note = NoteEntity(
                id = "n1",
                title = "Alpha Architecture",
                content = "High-level notes",
                folderId = "f1",
                colorTheme = "sage",
                createdAtEpochMs = 1000L,
                updatedAtEpochMs = 2000L
            )
            database.noteDao().insertNote(note)

            val tag = TagEntity("t1", "architecture", System.currentTimeMillis())
            database.tagDao().insertTag(tag)

            val backupFile = File(context.cacheDir, "test_valid_backup.zip")
            if (backupFile.exists()) backupFile.delete()

            val backupResult = backupManager.createBackup(Uri.fromFile(backupFile))
            assertTrue("Backup creation should succeed", backupResult.isSuccess)
            val manifest = backupResult.getOrNull()
            assertNotNull(manifest)
            assertEquals(1, manifest?.noteCount)
            assertEquals(1, manifest?.folderCount)
            assertEquals(1, manifest?.tagCount)
            assertEquals(1, manifest?.formatVersion)

            // Validate the generated archive
            val validation = backupManager.validateBackup(Uri.fromFile(backupFile))
            assertTrue("Generated backup must pass validation", validation is BackupValidationResult.Success)
            val success = validation as BackupValidationResult.Success
            assertEquals(1, success.manifest.noteCount)

            backupFile.delete()
        }
    }

    // --- 5. Deterministic Conflict Resolution (Merge Mode) ---
    @Test
    fun backupManager_mergeModeResolvesConflictsByNewerTimestamp() {
        runBlocking {
            // Local existing note (older updatedAt)
            val localNote = NoteEntity(
                id = "n_conflict",
                title = "Draft v1",
                content = "Older content",
                createdAtEpochMs = 1000L,
                updatedAtEpochMs = 1500L
            )
            database.noteDao().insertNote(localNote)

            // Backup note has same ID but newer updatedAt
            val backupNote = NoteEntity(
                id = "n_conflict",
                title = "Draft v2 (Restored)",
                content = "Updated content from backup",
                createdAtEpochMs = 1000L,
                updatedAtEpochMs = 2500L
            )

            // Create a backup archive containing this newer note
            val backupFile = File(context.cacheDir, "test_conflict_backup.zip")
            ZipOutputStream(FileOutputStream(backupFile)).use { zos ->
                val manifest = BackupManifest(
                    formatVersion = 1,
                    appId = "com.aistudio.vellumnotes",
                    appVersionName = "1.0",
                    createdAtEpochMs = System.currentTimeMillis(),
                    noteCount = 1,
                    folderCount = 0,
                    tagCount = 0,
                    noteTagCrossRefCount = 0,
                    taskCount = 0,
                    reminderCount = 0,
                    calendarEventCount = 0,
                    templateCount = 0,
                    attachmentCount = 0,
                    totalAttachmentSizeBytes = 0L
                )
                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write(BackupSerializer.serializeManifest(manifest).toByteArray())
                zos.closeEntry()

                val data = BackupData(
                    manifest = manifest,
                    notes = listOf(backupNote),
                    folders = emptyList(),
                    tags = emptyList(),
                    noteTagCrossRefs = emptyList(),
                    tasks = emptyList(),
                    reminders = emptyList(),
                    calendarEvents = emptyList(),
                    templates = emptyList(),
                    attachments = emptyList()
                )
                zos.putNextEntry(ZipEntry("data.json"))
                zos.write(BackupSerializer.serializeData(data).toByteArray())
                zos.closeEntry()
            }

            // Restore using MERGE mode
            val restoreResult = backupManager.restoreBackup(Uri.fromFile(backupFile), RestoreMode.MERGE)
            assertTrue("Restore merge should succeed", restoreResult.isSuccess)

            // Check note in database has been updated with the newer version
            val updatedNote = database.noteDao().getNoteById("n_conflict")
            assertNotNull(updatedNote)
            assertEquals("Draft v2 (Restored)", updatedNote?.title)
            assertEquals("Updated content from backup", updatedNote?.content)
            assertEquals(2500L, updatedNote?.updatedAtEpochMs)

            backupFile.delete()
        }
    }

    // --- 6. Import/Export Markdown Parsing and Preview ---
    @Test
    fun noteImportExport_parsesMarkdownChecklistsAndHeadersAccurately() {
        runBlocking {
            val markdownText = """
                # Sprint Goals
                
                Here are the primary objectives for this iteration:
                
                - [ ] Complete Level 7 attachments
                - [x] Configure non-destructive Room migration
                - [ ] Test backup and restore integrity
            """.trimIndent()

            val tempImportFile = File(context.cacheDir, "sample_import.md")
            tempImportFile.writeText(markdownText)

            val importManager = NoteImportExportManager(
                context = context,
                noteRepository = com.example.core.data.repository.NoteRepositoryImpl(
                    noteDao = database.noteDao(),
                    folderDao = database.folderDao(),
                    tagDao = database.tagDao()
                )
            )

            val parseResult = importManager.parseImportFile(Uri.fromFile(tempImportFile))
            assertTrue("Import parser should succeed", parseResult.isSuccess)
            val preview = parseResult.getOrNull()
            assertNotNull(preview)
            assertEquals("Sprint Goals", preview?.suggestedTitle)
            assertTrue("Should detect checklist items", preview?.isChecklist == true)
            assertEquals(3, preview?.checklistItems?.size)
            assertEquals("Complete Level 7 attachments", preview?.checklistItems?.get(0)?.text)
            assertFalse(preview?.checklistItems?.get(0)?.isDone ?: true)
            assertTrue(preview?.checklistItems?.get(1)?.isDone ?: false)

            // Save imported note into repository
            val importSaveResult = importManager.importNoteFromPreview(preview!!)
            assertTrue("Saving imported note should succeed", importSaveResult.isSuccess)
            val savedNote = importSaveResult.getOrNull()
            assertNotNull(savedNote)
            assertEquals("Sprint Goals", savedNote?.title)
            assertEquals(3, savedNote?.checklistItems?.size)

            tempImportFile.delete()
        }
    }
}
