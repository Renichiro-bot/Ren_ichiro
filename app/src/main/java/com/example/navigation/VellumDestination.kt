package com.example.navigation

sealed class VellumDestination(val route: String) {
    data object Home : VellumDestination("home")
    data object Search : VellumDestination("search")
    data object Appearance : VellumDestination("appearance")
    data object Archive : VellumDestination("archive")
    data object Trash : VellumDestination("trash")
    data object BackupPrivacy : VellumDestination("backup_privacy")
    data object Editor : VellumDestination("editor/{noteId}") {
        fun createRoute(noteId: String? = null): String {
            return if (!noteId.isNullOrBlank()) "editor/$noteId" else "editor/new"
        }
    }
}
