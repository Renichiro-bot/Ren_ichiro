package com.example.core.shortcut

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.MainActivity
import com.example.R
import com.example.core.model.Note

object VellumShortcutManager {

    private const val MAX_DYNAMIC_SHORTCUTS = 3

    /**
     * Updates launcher dynamic shortcuts with recent notes.
     */
    fun updateDynamicShortcuts(context: Context, notes: List<Note>) {
        if (!ShortcutManagerCompat.isRequestPinShortcutSupported(context)) return

        val recentNotes = notes
            .filter { !it.isTrashed && !it.isArchived }
            .sortedWith(
                compareByDescending<Note> { it.isPinned }
                    .thenByDescending { it.updatedAtEpochMs }
            )
            .take(MAX_DYNAMIC_SHORTCUTS)

        val shortcuts = recentNotes.map { note ->
            val label = note.title.ifBlank { "Untitled Note" }
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse("vellum://note/${note.id}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            ShortcutInfoCompat.Builder(context, "note_${note.id}")
                .setShortLabel(label.take(25))
                .setLongLabel(label.take(50))
                .setIcon(IconCompat.createWithResource(context, R.drawable.ic_widget_notes))
                .setIntent(intent)
                .build()
        }

        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (_: Exception) {
            // Ignore if platform restrictions apply
        }
    }
}
