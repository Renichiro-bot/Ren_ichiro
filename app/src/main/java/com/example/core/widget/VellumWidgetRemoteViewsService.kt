package com.example.core.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.core.database.VellumDatabase
import com.example.core.database.entity.toDomain
import com.example.core.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VellumWidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return VellumNotesRemoteViewsFactory(applicationContext)
    }
}

class VellumNotesRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private val notesList = mutableListOf<Note>()
    private var showPreviews = true
    private val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

    override fun onCreate() {
        // Initial setup
    }

    override fun onDataSetChanged() {
        try {
            showPreviews = VellumWidgetManager.isWidgetPreviewEnabled(context)
            val db = VellumDatabase.getInstance(context)
            val allNotes = runBlocking(Dispatchers.IO) {
                db.noteDao().getAllNotesSync()
            }

            // Filter active non-trashed, non-archived notes; sort pinned first, then by updatedAt descending
            val filtered = allNotes
                .map { it.toDomain() }
                .filter { !it.isTrashed && !it.isArchived }
                .sortedWith(
                    compareByDescending<Note> { it.isPinned }
                        .thenByDescending { it.updatedAtEpochMs }
                )
                .take(15)

            notesList.clear()
            notesList.addAll(filtered)
        } catch (e: Exception) {
            notesList.clear()
        }
    }

    override fun onDestroy() {
        notesList.clear()
    }

    override fun getCount(): Int = notesList.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= notesList.size) return null

        val note = notesList[position]
        val views = RemoteViews(context.packageName, R.layout.widget_note_item)

        // 1. Title
        val displayTitle = note.title.ifBlank { "Untitled Note" }
        views.setTextViewText(R.id.widget_item_title, displayTitle)

        // 2. Pin indicator
        if (note.isPinned) {
            views.setViewVisibility(R.id.widget_item_pin, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_item_pin, View.GONE)
        }

        // 3. Date
        val formattedDate = timeFormat.format(Date(note.updatedAtEpochMs))
        views.setTextViewText(R.id.widget_item_date, formattedDate)

        // 4. Preview Text (Privacy aware)
        if (showPreviews) {
            val preview = note.previewText.ifBlank { "(Empty note)" }
            views.setTextViewText(R.id.widget_item_preview, preview)
            views.setViewVisibility(R.id.widget_item_preview, View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_item_preview, View.GONE)
        }

        // 5. Fill-in intent with note deep link
        val fillInIntent = Intent().apply {
            data = Uri.parse("vellum://note/${note.id}")
        }
        views.setOnClickFillInIntent(R.id.widget_item_root, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position in 0 until notesList.size) {
            notesList[position].id.hashCode().toLong()
        } else {
            position.toLong()
        }
    }

    override fun hasStableIds(): Boolean = true
}
