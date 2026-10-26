package com.example.core.util

import android.content.Intent
import android.net.Uri

sealed class IncomingIntentTarget {
    data object None : IncomingIntentTarget()
    data class OpenNote(val noteId: String) : IncomingIntentTarget()
    data object NewNote : IncomingIntentTarget()
    data object Search : IncomingIntentTarget()
    data object Favorites : IncomingIntentTarget()
    data class ShareReceived(val title: String, val content: String) : IncomingIntentTarget()
}

object VellumIntentParser {

    private const val MAX_SHARED_TEXT_LENGTH = 500_000 // 500 KB limit for security

    /**
     * Parses and safely validates incoming Android intents (Deep links and Share sheet).
     * Enforces strict parameter validation and path-traversal protection.
     */
    fun parseIntent(intent: Intent?): IncomingIntentTarget {
        if (intent == null) return IncomingIntentTarget.None

        // 1. Share sheet integration (ACTION_SEND with text/plain)
        if (intent.action == Intent.ACTION_SEND) {
            val mimeType = intent.type ?: ""
            if (mimeType.startsWith("text/")) {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim() ?: ""
                val sharedSubject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim() ?: ""

                if (sharedText.isNotBlank() || sharedSubject.isNotBlank()) {
                    val safeContent = if (sharedText.length > MAX_SHARED_TEXT_LENGTH) {
                        sharedText.take(MAX_SHARED_TEXT_LENGTH)
                    } else {
                        sharedText
                    }

                    val safeTitle = if (sharedSubject.isNotBlank()) {
                        sharedSubject.take(150)
                    } else {
                        // Extract first non-empty line as title, or "Shared Note"
                        safeContent.lineSequence().firstOrNull { it.isNotBlank() }?.take(80) ?: "Shared Note"
                    }

                    return IncomingIntentTarget.ShareReceived(
                        title = safeTitle,
                        content = safeContent
                    )
                }
            }
            return IncomingIntentTarget.None
        }

        // 2. Deep links (ACTION_VIEW with vellum:// scheme)
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data ?: return IncomingIntentTarget.None
            return parseDeepLinkUri(uri)
        }

        return IncomingIntentTarget.None
    }

    /**
     * Safely validates and parses a vellum:// deep link URI.
     */
    fun parseDeepLinkUri(uri: Uri): IncomingIntentTarget {
        if (uri.scheme?.lowercase() != "vellum") return IncomingIntentTarget.None
        val uriStr = uri.toString()
        if (uriStr.contains("..")) return IncomingIntentTarget.None

        val host = uri.host?.lowercase() ?: ""
        val path = uri.path ?: ""

        when (host) {
            "new" -> return IncomingIntentTarget.NewNote
            "search" -> return IncomingIntentTarget.Search
            "favorites" -> return IncomingIntentTarget.Favorites
            "home" -> return IncomingIntentTarget.None
            "note" -> {
                // Format: vellum://note/{noteId} or vellum://note?id={noteId}
                val segments = uri.pathSegments
                if (segments.size > 1) return IncomingIntentTarget.None

                val rawId = segments.firstOrNull()?.trim()
                    ?: uri.getQueryParameter("id")?.trim()
                    ?: return IncomingIntentTarget.None

                val sanitizedId = sanitizeNoteId(rawId)
                return if (sanitizedId != null) {
                    IncomingIntentTarget.OpenNote(sanitizedId)
                } else {
                    IncomingIntentTarget.None
                }
            }
            else -> {
                // If format is vellum:///note/{noteId}
                if (path.startsWith("/note/")) {
                    val rawId = path.removePrefix("/note/").trim()
                    if (rawId.contains("/")) return IncomingIntentTarget.None
                    val sanitizedId = sanitizeNoteId(rawId)
                    return if (sanitizedId != null) {
                        IncomingIntentTarget.OpenNote(sanitizedId)
                    } else {
                        IncomingIntentTarget.None
                    }
                }
                if (path == "/new") return IncomingIntentTarget.NewNote
                if (path == "/search") return IncomingIntentTarget.Search
                if (path == "/favorites") return IncomingIntentTarget.Favorites
                return IncomingIntentTarget.None
            }
        }
    }

    /**
     * Sanitizes note identifiers to reject path traversal, control characters, or malicious input.
     */
    fun sanitizeNoteId(rawId: String): String? {
        val trimmed = rawId.trim()
        if (trimmed.isBlank() || trimmed.length > 100) return null
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\")) return null
        // Must contain valid identifier characters (letters, digits, hyphens, underscores)
        if (!trimmed.matches(Regex("^[a-zA-Z0-9_-]+$"))) return null
        return trimmed
    }
}
