package com.example.feature.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.component.VellumBadge
import com.example.core.designsystem.component.VellumButton
import com.example.core.designsystem.component.VellumEmptyState
import com.example.core.designsystem.component.VellumIconButton
import com.example.core.designsystem.component.VellumNoteCard
import com.example.core.designsystem.component.VellumOutlinedButton
import com.example.core.designsystem.component.VellumSearchField
import com.example.core.designsystem.theme.VellumShapes
import com.example.core.designsystem.theme.VellumSpacing
import com.example.core.model.Folder
import com.example.core.model.Note
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToSearch: () -> Unit = {},
    onOpenNote: (noteId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val notes by viewModel.notes.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val isGridLayout by viewModel.isGridLayout.collectAsState()
    val currentSort by viewModel.sortOrder.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val selectedFolder by viewModel.selectedFolder.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val selectedNoteIds by viewModel.selectedNoteIds.collectAsState()
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSortMenu by remember { mutableStateOf(false) }
    var showBulkDeleteDialog by remember { mutableStateOf(false) }
    var showBulkFolderDialog by remember { mutableStateOf(false) }
    var showBulkTagDialog by remember { mutableStateOf(false) }

    // Intercept back button when in selection mode to clear selection
    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    // Intercept back button when search query is active to clear search query
    BackHandler(enabled = !isSelectionMode && searchQuery.isNotEmpty()) {
        viewModel.clearSearchQuery()
    }

    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
    val otherNotes = remember(notes) { notes.filterNot { it.isPinned } }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // Contextual Selection Action Bar
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectedNoteIds.size} selected",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.clearSelection() },
                            modifier = Modifier.testTag("selection_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Exit selection mode",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    actions = {
                        // Select All / Deselect
                        IconButton(
                            onClick = {
                                if (selectedNoteIds.size == notes.size) {
                                    viewModel.clearSelection()
                                } else {
                                    viewModel.selectAll()
                                }
                            },
                            modifier = Modifier.testTag("selection_select_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SelectAll,
                                contentDescription = "Select all notes",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Bulk Pin / Unpin
                        val anyUnpinned = notes.filter { it.id in selectedNoteIds }.any { !it.isPinned }
                        IconButton(
                            onClick = {
                                viewModel.bulkTogglePinSelected {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (anyUnpinned) "Selected notes pinned." else "Selected notes unpinned."
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.testTag("selection_pin_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (anyUnpinned) Icons.Filled.PushPin else Icons.Rounded.PushPin,
                                contentDescription = if (anyUnpinned) "Pin selected notes" else "Unpin selected notes",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Bulk Favorite / Unfavorite
                        val anyUnfavorited = notes.filter { it.id in selectedNoteIds }.any { !it.isFavorite }
                        IconButton(
                            onClick = {
                                viewModel.bulkToggleFavoriteSelected {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(
                                            if (anyUnfavorited) "Added to Favorites." else "Removed from Favorites."
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.testTag("selection_favorite_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (anyUnfavorited) Icons.Filled.Star else Icons.Rounded.Star,
                                contentDescription = if (anyUnfavorited) "Favorite selected notes" else "Unfavorite selected notes",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Bulk Move to Folder
                        IconButton(
                            onClick = { showBulkFolderDialog = true },
                            modifier = Modifier.testTag("selection_folder_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Folder,
                                contentDescription = "Move to folder",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Bulk Add Tag
                        IconButton(
                            onClick = { showBulkTagDialog = true },
                            modifier = Modifier.testTag("selection_tag_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Tag,
                                contentDescription = "Tag selected notes",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Bulk Archive
                        IconButton(
                            onClick = {
                                viewModel.bulkArchiveSelected {
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Selected notes archived.")
                                    }
                                }
                            },
                            modifier = Modifier.testTag("selection_archive_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Archive,
                                contentDescription = "Archive selected notes",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Bulk Move to Trash
                        IconButton(
                            onClick = { showBulkDeleteDialog = true },
                            modifier = Modifier.testTag("selection_delete_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = "Move selected notes to trash",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                // Top App Bar: Side Menu Button + Search Bar with View Toggle
                TopAppBar(
                    navigationIcon = {
                        VellumIconButton(
                            imageVector = Icons.Rounded.Menu,
                            contentDescription = "Open Navigation Menu",
                            onClick = onOpenDrawer,
                            testTag = "home_menu_button"
                        )
                    },
                    title = {
                        VellumSearchField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = "Search notes, checklists...",
                            onClear = { viewModel.clearSearchQuery() },
                            trailingContent = {
                                IconButton(
                                    onClick = { viewModel.toggleLayout() },
                                    modifier = Modifier.testTag("home_layout_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (isGridLayout) Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                                        contentDescription = if (isGridLayout) "Switch to List View" else "Switch to Grid View",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = VellumSpacing.s),
                            testTag = "home_search_trigger"
                        )
                    },
                    actions = {},
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = { onOpenNote("new") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Rounded.Add, contentDescription = "Create Note Icon") },
                    text = { Text("New Note", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("home_create_note_fab")
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {

            // Active Folder or Tag Filter Pill
            if (selectedFolder != null || selectedTag != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VellumSpacing.l, vertical = 4.dp)
                ) {
                    selectedFolder?.let { f ->
                        Surface(
                            shape = VellumShapes.cornerFull,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.clickable { viewModel.selectFolder(null) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(f.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Rounded.Close, contentDescription = "Clear folder filter", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    selectedTag?.let { t ->
                        Surface(
                            shape = VellumShapes.cornerFull,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.clickable { viewModel.selectTag(null) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Rounded.Tag, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("#${t.name}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Rounded.Close, contentDescription = "Clear tag filter", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { viewModel.clearFolderAndTagFilters() }) {
                        Text("Clear", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Filtering and Sorting Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VellumSpacing.l, vertical = VellumSpacing.xs)
                    .horizontalScroll(rememberScrollState())
            ) {
                // Filter Chips
                HomeFilter.entries.forEach { filter ->
                    val isSelected = currentFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier
                            .padding(end = VellumSpacing.s)
                            .testTag("filter_chip_${filter.name}")
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Sort Dropdown Button
                Box {
                    TextButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier.testTag("home_sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = "Sort Options",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = currentSort.label,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        NoteSortOrder.entries.forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort.label) },
                                trailingIcon = {
                                    if (currentSort == sort) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    viewModel.setSortOrder(sort)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            if (notes.isEmpty()) {
                // Empty State
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 72.dp)
                ) {
                    VellumEmptyState(
                        title = if (searchQuery.isNotBlank()) {
                            "No Search Results"
                        } else if (currentFilter != HomeFilter.ALL || selectedFolder != null || selectedTag != null) {
                            "No Matching Notes"
                        } else {
                            "Your Canvas is Clean"
                        },
                        description = if (searchQuery.isNotBlank()) {
                            "No notes found matching \"$searchQuery\"."
                        } else if (selectedFolder != null) {
                            "This folder has no notes yet. Move notes here from the note menu or selection actions."
                        } else if (selectedTag != null) {
                            "No notes are tagged with #${selectedTag?.name}."
                        } else if (currentFilter != HomeFilter.ALL) {
                            "No notes matched the '${currentFilter.label}' filter. Try selecting 'All Notes'."
                        } else {
                            "Capture an idea, outline a thought, or record meeting insights on tactile digital vellum."
                        },
                        actionButton = {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(VellumSpacing.m)
                            ) {
                                if (searchQuery.isNotBlank()) {
                                    VellumButton(
                                        text = "Clear Search",
                                        onClick = { viewModel.clearSearchQuery() },
                                        testTag = "empty_state_clear_search_button"
                                    )
                                } else if (currentFilter != HomeFilter.ALL || selectedFolder != null || selectedTag != null) {
                                    VellumButton(
                                        text = "Show All Notes",
                                        onClick = {
                                            viewModel.clearFolderAndTagFilters()
                                            viewModel.setFilter(HomeFilter.ALL)
                                        },
                                        testTag = "empty_state_all_notes_button"
                                    )
                                } else {
                                    VellumButton(
                                        text = "Draft a Note",
                                        onClick = { onOpenNote("new") },
                                        testTag = "empty_state_draft_button"
                                    )
                                    VellumOutlinedButton(
                                        text = "Load Sample Notes",
                                        onClick = { viewModel.loadSampleNotes() },
                                        testTag = "empty_state_load_samples_button"
                                    )
                                }
                            }
                        }
                    )
                }
            } else {
                // Notes Feed: Responsive Staggered Grid or Linear List
                if (isGridLayout) {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        contentPadding = PaddingValues(
                            start = VellumSpacing.l,
                            end = VellumSpacing.l,
                            top = VellumSpacing.s,
                            bottom = 96.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(VellumSpacing.m),
                        verticalItemSpacing = VellumSpacing.m,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("home_notes_staggered_grid")
                    ) {
                        // Pinned Notes Section (if any)
                        if (pinnedNotes.isNotEmpty()) {
                            item(span = StaggeredGridItemSpan.FullLine) {
                                SectionHeader(title = "PINNED", count = pinnedNotes.size)
                            }
                            items(pinnedNotes, key = { it.id }) { note ->
                                val folderName = folders.find { it.id == note.folderId }?.name
                                VellumNoteCard(
                                    note = note,
                                    folderName = folderName,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = note.id in selectedNoteIds,
                                    onClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleNoteSelection(note.id)
                                        } else {
                                            onOpenNote(note.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            viewModel.startSelection(note.id)
                                        } else {
                                            viewModel.toggleNoteSelection(note.id)
                                        }
                                    },
                                    onTogglePin = { viewModel.togglePin(note.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(note.id) },
                                    onArchive = {
                                        viewModel.archiveNote(note.id) {
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Note archived.") }
                                        }
                                    },
                                    onDelete = {
                                        viewModel.deleteNote(note.id)
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Note moved to trash.") }
                                    },
                                    onToggleChecklistItem = { itemId ->
                                        viewModel.toggleChecklistItem(note.id, itemId)
                                    }
                                )
                            }
                        }

                        // Other Notes Section
                        if (otherNotes.isNotEmpty()) {
                            if (pinnedNotes.isNotEmpty()) {
                                item(span = StaggeredGridItemSpan.FullLine) {
                                    SectionHeader(title = "OTHERS", count = otherNotes.size)
                                }
                            }
                            items(otherNotes, key = { it.id }) { note ->
                                val folderName = folders.find { it.id == note.folderId }?.name
                                VellumNoteCard(
                                    note = note,
                                    folderName = folderName,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = note.id in selectedNoteIds,
                                    onClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleNoteSelection(note.id)
                                        } else {
                                            onOpenNote(note.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            viewModel.startSelection(note.id)
                                        } else {
                                            viewModel.toggleNoteSelection(note.id)
                                        }
                                    },
                                    onTogglePin = { viewModel.togglePin(note.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(note.id) },
                                    onArchive = {
                                        viewModel.archiveNote(note.id) {
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Note archived.") }
                                        }
                                    },
                                    onDelete = {
                                        viewModel.deleteNote(note.id)
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Note moved to trash.") }
                                    },
                                    onToggleChecklistItem = { itemId ->
                                        viewModel.toggleChecklistItem(note.id, itemId)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Linear List Mode
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start = VellumSpacing.l,
                            end = VellumSpacing.l,
                            top = VellumSpacing.s,
                            bottom = 96.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(VellumSpacing.m),
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("home_notes_linear_list")
                    ) {
                        // Pinned Notes Section (if any)
                        if (pinnedNotes.isNotEmpty()) {
                            item {
                                SectionHeader(title = "PINNED", count = pinnedNotes.size)
                            }
                            items(pinnedNotes, key = { it.id }) { note ->
                                val folderName = folders.find { it.id == note.folderId }?.name
                                VellumNoteCard(
                                    note = note,
                                    folderName = folderName,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = note.id in selectedNoteIds,
                                    onClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleNoteSelection(note.id)
                                        } else {
                                            onOpenNote(note.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            viewModel.startSelection(note.id)
                                        } else {
                                            viewModel.toggleNoteSelection(note.id)
                                        }
                                    },
                                    onTogglePin = { viewModel.togglePin(note.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(note.id) },
                                    onArchive = {
                                        viewModel.archiveNote(note.id) {
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Note archived.") }
                                        }
                                    },
                                    onDelete = {
                                        viewModel.deleteNote(note.id)
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Note moved to trash.") }
                                    },
                                    onToggleChecklistItem = { itemId ->
                                        viewModel.toggleChecklistItem(note.id, itemId)
                                    }
                                )
                            }
                        }

                        // Other Notes Section
                        if (otherNotes.isNotEmpty()) {
                            if (pinnedNotes.isNotEmpty()) {
                                item {
                                    SectionHeader(title = "OTHERS", count = otherNotes.size)
                                }
                            }
                            items(otherNotes, key = { it.id }) { note ->
                                val folderName = folders.find { it.id == note.folderId }?.name
                                VellumNoteCard(
                                    note = note,
                                    folderName = folderName,
                                    isSelectionMode = isSelectionMode,
                                    isSelected = note.id in selectedNoteIds,
                                    onClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleNoteSelection(note.id)
                                        } else {
                                            onOpenNote(note.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (!isSelectionMode) {
                                            viewModel.startSelection(note.id)
                                        } else {
                                            viewModel.toggleNoteSelection(note.id)
                                        }
                                    },
                                    onTogglePin = { viewModel.togglePin(note.id) },
                                    onToggleFavorite = { viewModel.toggleFavorite(note.id) },
                                    onArchive = {
                                        viewModel.archiveNote(note.id) {
                                            coroutineScope.launch { snackbarHostState.showSnackbar("Note archived.") }
                                        }
                                    },
                                    onDelete = {
                                        viewModel.deleteNote(note.id)
                                        coroutineScope.launch { snackbarHostState.showSnackbar("Note moved to trash.") }
                                    },
                                    onToggleChecklistItem = { itemId ->
                                        viewModel.toggleChecklistItem(note.id, itemId)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Bulk Delete Confirmation Dialog (soft delete to trash)
    if (showBulkDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showBulkDeleteDialog = false },
            title = { Text("Move ${selectedNoteIds.size} notes to Trash?") },
            text = { Text("Selected notes will be moved to the Trash. You can restore them anytime.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBulkDeleteDialog = false
                        val count = selectedNoteIds.size
                        viewModel.bulkDeleteSelected {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Moved $count notes to Trash.")
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_bulk_delete_button")
                ) {
                    Text("Move to Trash", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showBulkDeleteDialog = false },
                    modifier = Modifier.testTag("cancel_bulk_delete_button")
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bulk Move to Folder Dialog
    if (showBulkFolderDialog) {
        var chosenFolderId by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showBulkFolderDialog = false },
            title = { Text("Move to Folder") },
            text = {
                Column {
                    // Option: Unfiled / Remove from Folder
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(VellumShapes.cornerM)
                            .clickable { chosenFolderId = null }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = chosenFolderId == null,
                            onClick = { chosenFolderId = null }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("No Folder (Unfiled)", style = MaterialTheme.typography.bodyMedium)
                    }

                    // Existing Folders
                    folders.forEach { f ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(VellumShapes.cornerM)
                                .clickable { chosenFolderId = f.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = chosenFolderId == f.id,
                                onClick = { chosenFolderId = f.id }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(f.name, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBulkFolderDialog = false
                        viewModel.bulkMoveSelectedToFolder(chosenFolderId) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Moved notes to folder.")
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_bulk_move_folder_button")
                ) {
                    Text("Move")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Bulk Add Tag Dialog
    if (showBulkTagDialog) {
        var tagInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showBulkTagDialog = false },
            title = { Text("Add Tag to Selected Notes") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        label = { Text("Tag name (e.g. work, ideas)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bulk_tag_input")
                    )

                    if (tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Existing tags:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            tags.forEach { t ->
                                FilterChip(
                                    selected = tagInput.trim().equals(t.name, ignoreCase = true),
                                    onClick = { tagInput = t.name },
                                    label = { Text("#${t.name}", fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val clean = tagInput.trim().removePrefix("#").trim()
                        if (clean.isNotBlank()) {
                            showBulkTagDialog = false
                            viewModel.bulkAddTagToSelected(clean) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Tag #$clean added to selected notes.")
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_bulk_tag_button")
                ) {
                    Text("Add Tag")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBulkTagDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = VellumSpacing.xs)
            .semantics { heading() }
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(VellumSpacing.s))
        VellumBadge(
            text = count.toString(),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
