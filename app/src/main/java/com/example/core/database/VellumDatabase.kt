package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.database.dao.AttachmentDao
import com.example.core.database.dao.CalendarEventDao
import com.example.core.database.dao.FolderDao
import com.example.core.database.dao.NoteDao
import com.example.core.database.dao.ReminderDao
import com.example.core.database.dao.TagDao
import com.example.core.database.dao.TaskDao
import com.example.core.database.dao.TemplateDao
import com.example.core.database.entity.AttachmentEntity
import com.example.core.database.entity.CalendarEventEntity
import com.example.core.database.entity.FolderEntity
import com.example.core.database.entity.NoteEntity
import com.example.core.database.entity.NoteTagCrossRef
import com.example.core.database.entity.ReminderEntity
import com.example.core.database.entity.TagEntity
import com.example.core.database.entity.TaskEntity
import com.example.core.database.entity.TemplateEntity

@Database(
    entities = [
        NoteEntity::class,
        FolderEntity::class,
        TagEntity::class,
        NoteTagCrossRef::class,
        TaskEntity::class,
        ReminderEntity::class,
        CalendarEventEntity::class,
        TemplateEntity::class,
        AttachmentEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class VellumDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun folderDao(): FolderDao
    abstract fun tagDao(): TagDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun calendarEventDao(): CalendarEventDao
    abstract fun templateDao(): TemplateDao
    abstract fun attachmentDao(): AttachmentDao

    companion object {
        @Volatile
        private var INSTANCE: VellumDatabase? = null

        /**
         * Explicit non-destructive migration from Version 1 to Version 2 (Level 5).
         * Adds `is_favorite` and `folder_id` to notes, and creates `folders`, `tags`,
         * and `note_tag_cross_ref` tables without dropping or modifying existing user records.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add Level 5 columns to existing notes table
                db.execSQL("ALTER TABLE notes ADD COLUMN is_favorite INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE notes ADD COLUMN folder_id TEXT DEFAULT NULL")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_is_favorite ON notes(is_favorite)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_notes_folder_id ON notes(folder_id)")

                // 2. Create folders table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS folders (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 3. Create tags table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tags (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_tags_name ON tags(name)")

                // 4. Create junction table for note-tag many-to-many relationship
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_tag_cross_ref (
                        note_id TEXT NOT NULL,
                        tag_id TEXT NOT NULL,
                        PRIMARY KEY(note_id, tag_id)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_tag_cross_ref_tag_id ON note_tag_cross_ref(tag_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_note_tag_cross_ref_note_id ON note_tag_cross_ref(note_id)")
            }
        }

        /**
         * Explicit non-destructive migration from Version 2 to Version 3 (Level 6).
         * Adds `tasks`, `reminders`, `calendar_events`, and `note_templates` tables.
         * Existing notes, folders, and tags are 100% preserved.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create tasks table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tasks (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        is_completed INTEGER NOT NULL DEFAULT 0,
                        priority TEXT NOT NULL DEFAULT 'none',
                        due_date_epoch_ms INTEGER,
                        associated_note_id TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_is_completed ON tasks(is_completed)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_due_date_epoch_ms ON tasks(due_date_epoch_ms)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_tasks_associated_note_id ON tasks(associated_note_id)")

                // 2. Create reminders table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS reminders (
                        id TEXT NOT NULL PRIMARY KEY,
                        target_type TEXT NOT NULL,
                        target_id TEXT NOT NULL,
                        title TEXT NOT NULL,
                        message TEXT NOT NULL DEFAULT '',
                        trigger_at_epoch_ms INTEGER NOT NULL,
                        is_enabled INTEGER NOT NULL DEFAULT 1,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_trigger_at_epoch_ms ON reminders(trigger_at_epoch_ms)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_is_enabled ON reminders(is_enabled)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_reminders_target_id ON reminders(target_id)")

                // 3. Create calendar_events table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS calendar_events (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        start_epoch_ms INTEGER NOT NULL,
                        end_epoch_ms INTEGER NOT NULL,
                        is_all_day INTEGER NOT NULL DEFAULT 0,
                        associated_task_id TEXT,
                        associated_note_id TEXT,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_calendar_events_start_epoch_ms ON calendar_events(start_epoch_ms)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_calendar_events_end_epoch_ms ON calendar_events(end_epoch_ms)")

                // 4. Create note_templates table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS note_templates (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        icon_name TEXT NOT NULL DEFAULT 'description',
                        default_title TEXT NOT NULL DEFAULT '',
                        content_template TEXT NOT NULL DEFAULT '',
                        is_checklist INTEGER NOT NULL DEFAULT 0,
                        checklist_template_json TEXT DEFAULT NULL,
                        is_built_in INTEGER NOT NULL DEFAULT 0,
                        created_at INTEGER NOT NULL,
                        updated_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // 5. Seed default built-in starter templates safely
                seedBuiltInTemplates(db)
            }
        }

        fun seedBuiltInTemplates(db: SupportSQLiteDatabase) {
            BuiltInTemplates.defaultTemplates.forEach { template ->
                val sql = """
                    INSERT OR IGNORE INTO note_templates (
                        id, name, description, icon_name, default_title, content_template,
                        is_checklist, checklist_template_json, is_built_in, created_at, updated_at
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
                val bindArgs = arrayOf(
                    template.id,
                    template.name,
                    template.description,
                    template.iconName,
                    template.defaultTitle,
                    template.contentTemplate,
                    template.isChecklist,
                    template.checklistTemplateJson,
                    template.isBuiltIn,
                    template.createdAt,
                    template.updatedAt
                )
                db.execSQL(sql, bindArgs)
            }
        }

        /**
         * Explicit non-destructive migration from Version 3 to Version 4 (Level 7).
         * Creates `attachments` table with foreign association to notes.
         * All existing notes, folders, tags, tasks, reminders, calendar events,
         * and templates are 100% preserved.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS attachments (
                        id TEXT NOT NULL PRIMARY KEY,
                        note_id TEXT NOT NULL,
                        file_name TEXT NOT NULL,
                        mime_type TEXT NOT NULL,
                        file_size_bytes INTEGER NOT NULL,
                        local_relative_path TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_attachments_note_id ON attachments(note_id)")
            }
        }

        fun getInstance(context: Context): VellumDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VellumDatabase::class.java,
                    "vellum_notes.db"
                )
                // Strict adherence to data-safety rules: explicit migrations only, zero destructive wipes
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedBuiltInTemplates(db)
                    }
                })
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
