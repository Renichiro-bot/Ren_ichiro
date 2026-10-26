package com.example.core.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

/**
 * Builds an AnnotatedString highlighting all case-insensitive occurrences of [query] within [text].
 */
@Composable
fun rememberHighlightedText(
    text: String,
    query: String?,
    highlightBackground: Color = MaterialTheme.colorScheme.primaryContainer,
    highlightTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
): AnnotatedString {
    val cleanQuery = query?.trim() ?: ""
    if (cleanQuery.isBlank() || text.isBlank()) {
        return AnnotatedString(text)
    }

    return buildAnnotatedString {
        var startIndex = 0
        val lowerText = text.lowercase()
        val lowerQuery = cleanQuery.lowercase()

        while (startIndex < text.length) {
            val matchIndex = lowerText.indexOf(lowerQuery, startIndex)
            if (matchIndex == -1) {
                append(text.substring(startIndex))
                break
            }

            if (matchIndex > startIndex) {
                append(text.substring(startIndex, matchIndex))
            }

            val endIndex = matchIndex + cleanQuery.length
            val highlightSpan = SpanStyle(
                background = highlightBackground,
                color = highlightTextColor,
                fontWeight = FontWeight.Bold
            )
            pushStyle(highlightSpan)
            append(text.substring(matchIndex, endIndex))
            pop()

            startIndex = endIndex
        }
    }
}
