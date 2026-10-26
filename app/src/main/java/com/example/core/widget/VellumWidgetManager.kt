package com.example.core.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.example.R

object VellumWidgetManager {

    private const val TAG = "VellumWidgetManager"
    private const val PREFS_NAME = "vellum_widget_prefs"
    private const val KEY_SHOW_PREVIEWS = "widget_show_previews"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isWidgetPreviewEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SHOW_PREVIEWS, true)
    }

    fun setWidgetPreviewEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SHOW_PREVIEWS, enabled).apply()
        notifyWidgetsDataChanged(context)
    }

    /**
     * Notifies all active Vellum home-screen widgets to refresh their database records and views.
     */
    fun notifyWidgetsDataChanged(context: Context) {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return

            // 1. Refresh Notes List Widget
            val notesWidgetComponent = ComponentName(context, VellumNotesWidgetProvider::class.java)
            val notesWidgetIds = appWidgetManager.getAppWidgetIds(notesWidgetComponent)
            if (notesWidgetIds != null && notesWidgetIds.isNotEmpty()) {
                appWidgetManager.notifyAppWidgetViewDataChanged(notesWidgetIds, R.id.widget_notes_listview)
                for (widgetId in notesWidgetIds) {
                    VellumNotesWidgetProvider.updateAppWidget(context, appWidgetManager, widgetId)
                }
            }

            // 2. Refresh Quick Note Widget
            val quickWidgetComponent = ComponentName(context, VellumQuickNoteWidgetProvider::class.java)
            val quickWidgetIds = appWidgetManager.getAppWidgetIds(quickWidgetComponent)
            if (quickWidgetIds != null && quickWidgetIds.isNotEmpty()) {
                for (widgetId in quickWidgetIds) {
                    VellumQuickNoteWidgetProvider.updateAppWidget(context, appWidgetManager, widgetId)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to notify widgets data changed", e)
        }
    }
}
