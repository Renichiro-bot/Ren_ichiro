package com.example

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.backup.BackupManager
import com.example.core.backup.NoteImportExportManager
import com.example.core.data.repository.AttachmentRepository
import com.example.core.data.repository.AttachmentRepositoryImpl
import com.example.core.data.repository.CalendarRepository
import com.example.core.data.repository.CalendarRepositoryImpl
import com.example.core.data.repository.NoteRepository
import com.example.core.data.repository.NoteRepositoryImpl
import com.example.core.data.repository.ReminderRepository
import com.example.core.data.repository.ReminderRepositoryImpl
import com.example.core.data.repository.TaskRepository
import com.example.core.data.repository.TaskRepositoryImpl
import com.example.core.data.repository.TemplateRepository
import com.example.core.data.repository.TemplateRepositoryImpl
import com.example.core.database.VellumDatabase
import com.example.core.security.AppLockManager
import com.example.core.storage.AttachmentStorage

val Context.securityDataStore: DataStore<Preferences> by preferencesDataStore(name = "vellum_security_prefs")

class VellumApplication : Application() {

    val database: VellumDatabase by lazy {
        VellumDatabase.getInstance(this)
    }

    val attachmentStorage: AttachmentStorage by lazy {
        AttachmentStorage(this)
    }

    val attachmentRepository: AttachmentRepository by lazy {
        AttachmentRepositoryImpl(
            attachmentDao = database.attachmentDao(),
            attachmentStorage = attachmentStorage
        )
    }

    val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(
            noteDao = database.noteDao(),
            folderDao = database.folderDao(),
            tagDao = database.tagDao(),
            attachmentDao = database.attachmentDao(),
            attachmentStorage = attachmentStorage,
            context = this
        )
    }

    val taskRepository: TaskRepository by lazy {
        TaskRepositoryImpl(
            taskDao = database.taskDao()
        )
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepositoryImpl(
            reminderDao = database.reminderDao(),
            context = this
        )
    }

    val calendarRepository: CalendarRepository by lazy {
        CalendarRepositoryImpl(
            calendarEventDao = database.calendarEventDao()
        )
    }

    val templateRepository: TemplateRepository by lazy {
        TemplateRepositoryImpl(
            templateDao = database.templateDao()
        )
    }

    val backupManager: BackupManager by lazy {
        BackupManager(
            context = this,
            database = database,
            attachmentStorage = attachmentStorage
        )
    }

    val importExportManager: NoteImportExportManager by lazy {
        NoteImportExportManager(
            context = this,
            noteRepository = noteRepository
        )
    }

    val appLockManager: AppLockManager by lazy {
        AppLockManager(
            context = this,
            dataStore = securityDataStore
        )
    }
}
