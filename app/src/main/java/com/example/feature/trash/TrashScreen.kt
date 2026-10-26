package com.example.feature.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.example.core.model.Note
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trashedNotes by viewModel.trashedNotes.collectAsState()
    val selectedIds by viewModel.selectedNoteIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var noteToDeletePermanently by remember { mutableStateOf<Note?>(null) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }

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
                            modifier = Modifier.testTag("trash_selection_close")
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Exit selection")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (selectedIds.size == trashedNotes.size) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAll()
                                }
                            },
                            modifier = Modifier.testTag("trash_select_all")
                        ) {
                            Icon(Icons.Rounded.SelectAll, contentDescription = "Select all")
                        }
                        IconButton(
                            onClick = {
                                viewModel.restoreSelected {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Restored selected notes.")
                                    }
                                }
                            },
                            modifier = Modifier.testTag("trash_bulk_restore")
                        ) {
                            Icon(Icons.Rounded.Restore, contentDescription = "Restore selected")
                        }
                        IconButton(
                            onClick = { showBulkDeleteDialog = true },
                            modifier = Modifier.testTag("trash_bulk_permanent_delete")
                        ) {
                            Icon(Icons.Rounded.DeleteForever, contentDescription = "Delete permanently")
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "Trash",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    },
                    navigationIcon = {
                        VellumIconButton(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back to Notes",
                            onClick = onNavigateBack,
                            testTag = "trash_back_button"
                        )
                    },
                    actions = {
                        if (trashedNotes.isNotEmpty()) {
                            IconButton(
                                onClick = { showEmptyTrashDialog = true },
                                modifier = Modifier.testTag("trash_empty_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteSweep,
                                    contentDescription = "Empty Trash",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
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
            if (trashedNotes.isEmpty()) {
                VellumEmptyState(
                    title = "Trash is Empty",
                    description = "Items moved to trash will appear here before being permanently removed.",
                    modifier = Modifier.testTag("trash_empty_state")
                )
            } else {
                // Informational Notice Banner
                Card(
                    shape = VellumShapes.cornerM,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VellumSpacing.l, vertical = VellumSpacing.s)
                ) {
                    Text(
                        text = "Notes in trash are soft-deleted. Restore to place them back into active notes, or empty trash to permanently remove them.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(VellumSpacing.m)
                    )
                }

                LazyColumn(
                    contentPadding = PaddingValues(VellumSpacing.l),
                    verticalArrangement = Arrangement.spacedBy(VellumSpacing.m),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("trash_list")
                ) {
                    items(trashedNotes, key = { it.id }) { note ->
                        val isSelected = selectedIds.contains(note.id)
                        Card(
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
                                .testTag("trashed_note_${note.id}")
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

                                    // Restore Single Note Action
                                    TextButton(
                                        onClick = {
                                            viewModel.restoreNote(note.id)
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("Note restored to active notes.")
                                            }
                                        },
                                        modifier = Modifier.testTag("restore_note_${note.id}")
                                    ) {
                                        Icon(Icons.Rounded.Restore, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                                        Text("Restore")
                                    }

                                    // Permanently Delete Single Note Action
                                    TextButton(
                                        onClick = { noteToDeletePermanently = note },
                                        modifier = Modifier.testTag("permanent_delete_${note.id}")
                                    ) {
                                        Icon(
                                            Icons.Rounded.DeleteForever,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        Text("Delete", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Confirmation Dialog: Empty Trash
    if (showEmptyTrashDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text("Empty Trash?") },
            text = { Text("All items in the Trash will be permanently erased. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showEmptyTrashDialog = false
                        viewModel.emptyTrash {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Trash emptied permanently.")
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_empty_trash_button")
                ) {
                    Text("Empty Trash", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog: Delete Single Note Permanently
    noteToDeletePermanently?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDeletePermanently = null },
            title = { Text("Delete Permanently?") },
            text = { Text("\"${note.title.ifBlank { "Untitled Note" }}\" will be permanently removed from your device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = note.id
                        noteToDeletePermanently = null
                        viewModel.deletePermanently(id)
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Note permanently deleted.")
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_permanently_button")
                ) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDeletePermanently = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Confirmation Dialog: Bulk Delete Permanently
    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("Delete ${selectedIds.size} Notes Permanently?") },
            text = { Text("The selected notes will be permanently removed from your device. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBulkDeleteDialog = false
                        viewModel.deletePermanentlySelected {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Selected notes permanently deleted.")
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_bulk_permanent_delete_button")
                ) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
