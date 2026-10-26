package com.example

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.test.core.app.ApplicationProvider
import com.example.core.database.VellumDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Level5RoomMigrationTest {

    @Test
    fun migration_1_to_2_preservesExistingDataAndAddsNewSchema() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "test_migration_1_2.db"
        context.deleteDatabase(dbName)

        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    // Create Version 1 Schema
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS notes (
                            id TEXT NOT NULL PRIMARY KEY,
                            title TEXT NOT NULL,
                            content TEXT NOT NULL,
                            is_pinned INTEGER NOT NULL DEFAULT 0,
                            is_archived INTEGER NOT NULL DEFAULT 0,
                            is_trashed INTEGER NOT NULL DEFAULT 0,
                            is_checklist INTEGER NOT NULL DEFAULT 0,
                            checklist_json TEXT NOT NULL DEFAULT '',
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
        val v1Db = helper.writableDatabase

        // Insert records into Version 1
        val now = System.currentTimeMillis()
        v1Db.execSQL(
            """
            INSERT INTO notes (id, title, content, is_pinned, is_archived, is_trashed, is_checklist, checklist_json, color_theme, created_at, updated_at)
            VALUES ('v1-note-1', 'Legacy User Note', 'Important content written before Level 5.', 1, 0, 0, 0, '', 'sand', $now, $now)
            """.trimIndent()
        )

        // Run explicit MIGRATION_1_2
        VellumDatabase.MIGRATION_1_2.migrate(v1Db)

        // 1. Verify legacy note is completely intact
        val cursor = v1Db.query("SELECT * FROM notes WHERE id = 'v1-note-1'")
        assertTrue("Legacy note must still exist after migration", cursor.moveToFirst())
        assertEquals("Legacy User Note", cursor.getString(cursor.getColumnIndexOrThrow("title")))
        assertEquals("Important content written before Level 5.", cursor.getString(cursor.getColumnIndexOrThrow("content")))
        assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("is_pinned")))
        assertEquals("sand", cursor.getString(cursor.getColumnIndexOrThrow("color_theme")))

        // 2. Verify new Level 5 columns added with proper defaults
        assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("is_favorite")))
        assertTrue(cursor.isNull(cursor.getColumnIndexOrThrow("folder_id")))
        cursor.close()

        // 3. Verify new folders table exists and is operational
        v1Db.execSQL("INSERT INTO folders (id, name, created_at, updated_at) VALUES ('f1', 'Work', $now, $now)")
        val folderCursor = v1Db.query("SELECT * FROM folders WHERE id = 'f1'")
        assertTrue(folderCursor.moveToFirst())
        assertEquals("Work", folderCursor.getString(folderCursor.getColumnIndexOrThrow("name")))
        folderCursor.close()

        // 4. Verify new tags table exists and is operational
        v1Db.execSQL("INSERT INTO tags (id, name, created_at) VALUES ('t1', 'design', $now)")
        val tagCursor = v1Db.query("SELECT * FROM tags WHERE id = 't1'")
        assertTrue(tagCursor.moveToFirst())
        assertEquals("design", tagCursor.getString(tagCursor.getColumnIndexOrThrow("name")))
        tagCursor.close()

        // 5. Verify junction table exists
        v1Db.execSQL("INSERT INTO note_tag_cross_ref (note_id, tag_id) VALUES ('v1-note-1', 't1')")
        val crossRefCursor = v1Db.query("SELECT * FROM note_tag_cross_ref WHERE note_id = 'v1-note-1'")
        assertTrue(crossRefCursor.moveToFirst())
        assertEquals("t1", crossRefCursor.getString(crossRefCursor.getColumnIndexOrThrow("tag_id")))
        crossRefCursor.close()

        v1Db.close()
        context.deleteDatabase(dbName)
    }
}
