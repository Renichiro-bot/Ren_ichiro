package com.example.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.theme.NoteCardTints
import com.example.core.designsystem.theme.VellumShapes
import com.example.core.designsystem.theme.VellumSpacing
import com.example.core.model.NoteColorTheme
import com.example.core.model.PrototypeNote

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VellumNoteCard(
    note: PrototypeNote,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    highlightQuery: String? = null,
    folderName: String? = null,
    onTogglePin: (() -> Unit)? = null,
    onToggleFavorite: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onToggleChecklistItem: ((itemId: String) -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val (containerColor, borderColor) = resolveCardColors(note.colorTheme, isDark)
    var showMenu by remember { mutableStateOf(false) }

    val activeBorder = if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, borderColor)
    }

    Surface(
        shape = VellumShapes.cornerL,
        color = containerColor,
        border = activeBorder,
        modifier = modifier
            .testTag("note_card_${note.id}")
            .fillMaxWidth()
            .clip(VellumShapes.cornerL)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(VellumSpacing.l)
        ) {
            // Header Row: Title & Action Icons
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                val titleText = note.title.ifBlank { "Untitled Note" }
                val highlightedTitle = rememberHighlightedText(text = titleText, query = highlightQuery)

                Text(
                    text = highlightedTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        lineHeight = 22.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = VellumSpacing.xs)
                )

                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Selected note ${note.title}" else "Unselected note ${note.title}",
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("note_card_select_${note.id}")
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Pin Icon
                        if (note.isPinned) {
                            Icon(
                                imageVector = Icons.Filled.PushPin,
                                contentDescription = "Pinned Note",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 4.dp)
                            )
                        }

                        // Favorite Star Toggle
                        if (onToggleFavorite != null) {
                            IconButton(
                                onClick = onToggleFavorite,
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("note_card_favorite_${note.id}")
                            ) {
                                Icon(
                                    imageVector = if (note.isFavorite) Icons.Filled.Star else Icons.Rounded.StarBorder,
                                    contentDescription = if (note.isFavorite) "Remove from favorites" else "Add to favorites",
                                    tint = if (note.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else if (note.isFavorite) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Favorite Note",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 4.dp)
                            )
                        }

                        // More Options Menu
                        if (onTogglePin != null || onArchive != null || onDelete != null) {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier
                                    .size(28.dp)
                                    .testTag("note_card_menu_${note.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MoreVert,
                                    contentDescription = "Note actions for ${note.title}",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                if (onTogglePin != null) {
                                    DropdownMenuItem(
                                        text = { Text(if (note.isPinned) "Unpin Note" else "Pin to Top") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (note.isPinned) Icons.Rounded.PushPin else Icons.Filled.PushPin,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onTogglePin()
                                        }
                                    )
                                }
                                if (onToggleFavorite != null) {
                                    DropdownMenuItem(
                                        text = { Text(if (note.isFavorite) "Remove Favorite" else "Mark as Favorite") },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (note.isFavorite) Icons.Rounded.StarBorder else Icons.Filled.Star,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onToggleFavorite()
                                        }
                                    )
                                }
                                if (onArchive != null) {
                                    DropdownMenuItem(
                                        text = { Text("Archive") },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.Archive, contentDescription = null)
                                        },
                                        onClick = {
                                            showMenu = false
                                            onArchive()
                                        }
                                    )
                                }
                                if (onDelete != null) {
                                    DropdownMenuItem(
                                        text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        },
                                        onClick = {
                                            showMenu = false
                                            onDelete()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(VellumSpacing.xs))

            // Body Content or Checklist Preview
            if (note.isChecklist) {
                val previewItems = note.checklistItems.take(4)
                Column(modifier = Modifier.fillMaxWidth()) {
                    previewItems.forEach { item ->
                        val highlightedItem = rememberHighlightedText(text = item.text, query = highlightQuery)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                                .clickable(enabled = onToggleChecklistItem != null && !isSelectionMode) {
                                    onToggleChecklistItem?.invoke(item.id)
                                }
                        ) {
                            Icon(
                                imageVector = if (item.isDone) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                contentDescription = if (item.isDone) "Completed" else "Incomplete",
                                tint = if (item.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(VellumSpacing.s))
                            Text(
                                text = highlightedItem,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                color = if (item.isDone) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (note.checklistItems.size > 4) {
                        Text(
                            text = "+${note.checklistItems.size - 4} more items",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp, start = 24.dp)
                        )
                    }
                }
            } else if (note.content.isNotBlank()) {
                val highlightedContent = rememberHighlightedText(text = note.content, query = highlightQuery)
                Text(
                    text = highlightedContent,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        lineHeight = 20.sp,
                        fontSize = 14.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tags and Folder Badges Row
            if (folderName != null || note.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(VellumSpacing.s))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    if (folderName != null) {
                        VellumBadge(
                            text = "📁 $folderName",
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    note.tags.take(3).forEach { tag ->
                        VellumBadge(
                            text = "#$tag",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (note.tags.size > 3) {
                        Text(
                            text = "+${note.tags.size - 3}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(VellumSpacing.m))

            // Footer Row: Metadata & Badges
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = note.timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (note.isChecklist && note.checklistItems.isNotEmpty()) {
                    VellumBadge(
                        text = "${note.completedChecklistCount}/${note.checklistItems.size} done",
                        containerColor = if (note.completedChecklistCount == note.checklistItems.size) {
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        contentColor = if (note.completedChecklistCount == note.checklistItems.size) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                } else if (note.wordCount > 0) {
                    Text(
                        text = "${note.wordCount} words",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
private fun resolveCardColors(theme: NoteColorTheme, isDark: Boolean): Pair<Color, Color> {
    return when (theme) {
        NoteColorTheme.DEFAULT -> {
            MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.outlineVariant
        }
        NoteColorTheme.SAND -> {
            if (isDark) NoteCardTints.SandDarkSurface to NoteCardTints.SandDarkBorder
            else NoteCardTints.SandLightSurface to NoteCardTints.SandLightBorder
        }
        NoteColorTheme.SAGE -> {
            if (isDark) NoteCardTints.SageDarkSurface to NoteCardTints.SageDarkBorder
            else NoteCardTints.SageLightSurface to NoteCardTints.SageLightBorder
        }
        NoteColorTheme.OCEAN -> {
            if (isDark) NoteCardTints.OceanDarkSurface to NoteCardTints.OceanDarkBorder
            else NoteCardTints.OceanLightSurface to NoteCardTints.OceanLightBorder
        }
        NoteColorTheme.LAVENDER -> {
            if (isDark) NoteCardTints.LavenderDarkSurface to NoteCardTints.LavenderDarkBorder
            else NoteCardTints.LavenderLightSurface to NoteCardTints.LavenderLightBorder
        }
        NoteColorTheme.TERRACOTTA -> {
            if (isDark) NoteCardTints.TerracottaDarkSurface to NoteCardTints.TerracottaDarkBorder
            else NoteCardTints.TerracottaLightSurface to NoteCardTints.TerracottaLightBorder
        }
    }
}
