package com.example.feature.archive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.component.VellumEmptyState
import com.example.core.designsystem.component.VellumIconButton
import com.example.core.designsystem.theme.VellumShapes
import com.example.core.designsystem.theme.VellumSpacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: ArchiveViewModel,
    onNavigateBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val archivedNotes by viewModel.archivedNotes.collectAsState()
    val selectedIds by viewModel.selectedNoteIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier.testTag("archive_selection_close")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Exit selection")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (selectedIds.size == archivedNotes.size) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAll()
                                }
                            },
                            modifier = Modifier.testTag("archive_select_all")
                        ) {
                            Icon(Icons.Rounded.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(
                            onClick = {
                                viewModel.unarchiveSelected {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Unarchived selected notes.")
                                    }
                                }
                            },
                            modifier = Modifier.testTag("archive_bulk_unarchive")
                        ) {
                            Icon(Icons.Rounded.Unarchive, contentDescription = "Unarchive selected")
                        }
                        IconButton(
                            onClick = {
                                viewModel.moveSelectedToTrash {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Moved selected notes to Trash.")
                                    }
                                }
                            },
                            modifier = Modifier.testTag("archive_bulk_trash")
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Move to trash")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "Archive",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        VellumIconButton(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Notes",
                            onClick = onNavigateBack,
                            testTag = "archive_back_button"
                        )
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (archivedNotes.isEmpty()) {
                VellumEmptyState(
                    title = "Archive is Empty",
                    description = "Archived notes are stored here safely out of your active feed.",
                    modifier = Modifier.testTag("archive_empty_state")
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(VellumSpacing.l),
                    verticalArrangement = Arrangement.spacedBy(VellumSpacing.m),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("archive_list")
                ) {
                    items(archivedNotes, key = { it.id }) { note ->
                        val isSelected = selectedIds.contains(note.id)
                        Card(
                            onClick = { onOpenNote(note.id) },
                            shape = VellumShapes.cornerM,
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                } else {
                                    MaterialTheme.colorScheme.surface
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("archived_note_${note.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(VellumSpacing.m)
                            ) {
                                Text(
                                    text = note.title.ifBlank { "Untitled Note" },
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (note.previewText.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = note.previewText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.height(VellumSpacing.m))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = note.formattedTimestamp,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Unarchive single note
                                    TextButton(
                                        onClick = {
                                            viewModel.unarchiveNote(note.id)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Note unarchived to active notes.")
                                            }
                                        },
                                        modifier = Modifier.testTag("unarchive_note_${note.id}")
                                    ) {
                                        Icon(Icons.Rounded.Unarchive, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                        Text("Unarchive")
                                    }

                                    // Move to Trash
                                    IconButton(
                                        onClick = {
                                            viewModel.moveNoteToTrash(note.id)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Note moved to trash.")
                                            }
                                        },
                                        modifier = Modifier.testTag("trash_archived_note_${note.id}")
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteOutline,
                                            contentDescription = "Move to trash",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
