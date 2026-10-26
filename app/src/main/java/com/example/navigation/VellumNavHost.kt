package com.example.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.core.data.repository.NoteRepository
import com.example.core.model.Folder
import com.example.core.preferences.ThemeViewModel
import com.example.feature.archive.ArchiveScreen
import com.example.feature.archive.ArchiveViewModel
import com.example.feature.editor.NoteEditorScreen
import com.example.feature.editor.NoteEditorViewModel
import com.example.feature.home.HomeFilter
import com.example.feature.home.HomeScreen
import com.example.feature.home.HomeViewModel
import androidx.compose.ui.platform.LocalContext
import com.example.VellumApplication
import com.example.feature.backup.BackupPrivacyScreen
import com.example.feature.backup.BackupPrivacyViewModel
import com.example.feature.search.SearchScreen
import com.example.feature.search.SearchViewModel
import com.example.feature.settings.AppearanceScreen
import com.example.feature.trash.TrashScreen
import com.example.feature.trash.TrashViewModel
import com.example.core.shortcut.VellumShortcutManager
import com.example.core.util.IncomingIntentTarget
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch

@Composable
fun VellumNavHost(
    themeViewModel: ThemeViewModel,
    repository: NoteRepository,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    incomingTarget: IncomingIntentTarget = IncomingIntentTarget.None,
    onConsumeIncomingTarget: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: VellumDestination.Home.route
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var sharedNoteData by remember { mutableStateOf<Pair<String, String>?>(null) }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.provideFactory(repository)
    )

    val folders by homeViewModel.folders.collectAsState()
    val tags by homeViewModel.tags.collectAsState()
    val selectedFolder by homeViewModel.selectedFolder.collectAsState()
    val selectedTag by homeViewModel.selectedTag.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }
    var folderInputText by remember { mutableStateOf("") }
    var folderInputError by remember { mutableStateOf<String?>(null) }

    val notes by homeViewModel.notes.collectAsState()

    // Sync launcher dynamic shortcuts with recent notes
    LaunchedEffect(notes) {
        VellumShortcutManager.updateDynamicShortcuts(context, notes)
    }

    // Handle deep links, launcher shortcuts, and share-sheet text intents
    LaunchedEffect(incomingTarget) {
        when (incomingTarget) {
            is IncomingIntentTarget.None -> {}
            is IncomingIntentTarget.NewNote -> {
                onConsumeIncomingTarget()
                navController.navigate(VellumDestination.Editor.createRoute(null)) {
                    launchSingleTop = true
                }
            }
            is IncomingIntentTarget.Search -> {
                onConsumeIncomingTarget()
                navController.navigate(VellumDestination.Search.route) {
                    launchSingleTop = true
                }
            }
            is IncomingIntentTarget.Favorites -> {
                onConsumeIncomingTarget()
                homeViewModel.clearFolderAndTagFilters()
                homeViewModel.setFilter(HomeFilter.FAVORITES)
                if (currentRoute != VellumDestination.Home.route) {
                    navController.navigate(VellumDestination.Home.route) {
                        popUpTo(VellumDestination.Home.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
            is IncomingIntentTarget.OpenNote -> {
                val targetNoteId = incomingTarget.noteId
                onConsumeIncomingTarget()
                coroutineScope.launch {
                    val note = repository.getNoteById(targetNoteId)
                    if (note != null && !note.isTrashed) {
                        navController.navigate(VellumDestination.Editor.createRoute(targetNoteId)) {
                            launchSingleTop = true
                        }
                    } else {
                        snackbarHostState.showSnackbar("Note not found or has been moved to Trash.")
                    }
                }
            }
            is IncomingIntentTarget.ShareReceived -> {
                val pair = incomingTarget.title to incomingTarget.content
                onConsumeIncomingTarget()
                sharedNoteData = pair
                navController.navigate(VellumDestination.Editor.createRoute(null)) {
                    launchSingleTop = true
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = currentRoute == VellumDestination.Home.route,
        drawerContent = {
            VellumDrawerContent(
                currentRoute = currentRoute,
                folders = folders,
                tags = tags,
                selectedFolder = selectedFolder,
                selectedTag = selectedTag,
                onNavigate = { destination ->
                    coroutineScope.launch {
                        drawerState.close()
                    }
                    if (currentRoute != destination.route) {
                        navController.navigate(destination.route) {
                            popUpTo(VellumDestination.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                onSelectFolder = { folder ->
                    coroutineScope.launch { drawerState.close() }
                    homeViewModel.selectFolder(folder)
                    homeViewModel.selectTag(null)
                },
                onSelectTag = { tag ->
                    coroutineScope.launch { drawerState.close() }
                    homeViewModel.selectTag(tag)
                },
                onSelectFavorites = {
                    coroutineScope.launch { drawerState.close() }
                    homeViewModel.clearFolderAndTagFilters()
                    homeViewModel.setFilter(HomeFilter.FAVORITES)
                },
                onCreateFolderClick = {
                    folderInputText = ""
                    folderInputError = null
                    showCreateFolderDialog = true
                },
                onRenameFolderClick = { folder ->
                    folderToRename = folder
                    folderInputText = folder.name
                    folderInputError = null
                },
                onDeleteFolderClick = { folder ->
                    folderToDelete = folder
                },
                onResetSampleNotes = {
                    coroutineScope.launch {
                        drawerState.close()
                        homeViewModel.loadSampleNotes()
                        snackbarHostState.showSnackbar("Curated sample notes loaded into Room database.")
                    }
                },
                onClearAllNotes = {
                    coroutineScope.launch {
                        drawerState.close()
                        homeViewModel.clearAllNotes()
                        snackbarHostState.showSnackbar("All notes cleared from database. Canvas is clean.")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) {
        NavHost(
            navController = navController,
            startDestination = VellumDestination.Home.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(VellumDestination.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onOpenDrawer = {
                        coroutineScope.launch {
                            drawerState.open()
                        }
                    },
                    onNavigateToSearch = {
                        navController.navigate(VellumDestination.Search.route)
                    },
                    onOpenNote = { noteId ->
                        navController.navigate(VellumDestination.Editor.createRoute(noteId))
                    }
                )
            }
            composable(VellumDestination.Search.route) {
                val searchViewModel: SearchViewModel = viewModel(
                    factory = SearchViewModel.provideFactory(repository)
                )
                SearchScreen(
                    viewModel = searchViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onOpenNote = { noteId ->
                        navController.navigate(VellumDestination.Editor.createRoute(noteId))
                    }
                )
            }
            composable(
                route = VellumDestination.Editor.route,
                arguments = listOf(
                    navArgument("noteId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = "new"
                    }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getString("noteId")
                val app = LocalContext.current.applicationContext as VellumApplication
                val initialTitle = if (noteId == "new" && sharedNoteData != null) sharedNoteData!!.first else ""
                val initialContent = if (noteId == "new" && sharedNoteData != null) sharedNoteData!!.second else ""
                val viewModelKey = if (initialTitle.isNotBlank() || initialContent.isNotBlank()) {
                    "editor_${noteId}_${System.currentTimeMillis()}"
                } else {
                    "editor_$noteId"
                }
                val editorViewModel: NoteEditorViewModel = viewModel(
                    key = viewModelKey,
                    factory = NoteEditorViewModel.provideFactory(
                        noteId = noteId,
                        repository = repository,
                        attachmentRepository = app.attachmentRepository,
                        importExportManager = app.importExportManager,
                        initialTitle = initialTitle,
                        initialContent = initialContent
                    )
                )
                if (sharedNoteData != null) {
                    sharedNoteData = null
                }
                NoteEditorScreen(
                    viewModel = editorViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(VellumDestination.Appearance.route) {
                AppearanceScreen(
                    viewModel = themeViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(VellumDestination.BackupPrivacy.route) {
                val app = LocalContext.current.applicationContext as VellumApplication
                val backupViewModel: BackupPrivacyViewModel = viewModel(
                    factory = BackupPrivacyViewModel.provideFactory(
                        backupManager = app.backupManager,
                        importExportManager = app.importExportManager,
                        noteRepository = repository,
                        attachmentRepository = app.attachmentRepository,
                        taskRepository = app.taskRepository,
                        reminderRepository = app.reminderRepository,
                        appLockManager = app.appLockManager
                    )
                )
                BackupPrivacyScreen(
                    viewModel = backupViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
            composable(VellumDestination.Archive.route) {
                val archiveViewModel: ArchiveViewModel = viewModel(
                    factory = ArchiveViewModel.provideFactory(repository)
                )
                ArchiveScreen(
                    viewModel = archiveViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onOpenNote = { noteId ->
                        navController.navigate(VellumDestination.Editor.createRoute(noteId))
                    }
                )
            }
            composable(VellumDestination.Trash.route) {
                val trashViewModel: TrashViewModel = viewModel(
                    factory = TrashViewModel.provideFactory(repository)
                )
                TrashScreen(
                    viewModel = trashViewModel,
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }

    // Dialog: Create Folder
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = folderInputText,
                    onValueChange = {
                        folderInputText = it
                        folderInputError = null
                    },
                    label = { Text("Folder Name") },
                    isError = folderInputError != null,
                    supportingText = if (folderInputError != null) { { Text(folderInputError ?: "") } } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("create_folder_name_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        homeViewModel.createFolder(folderInputText) { result ->
                            if (result.isSuccess) {
                                showCreateFolderDialog = false
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Folder '${result.getOrNull()?.name}' created.")
                                }
                            } else {
                                folderInputError = result.exceptionOrNull()?.message ?: "Invalid folder name"
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_folder_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Rename Folder
    folderToRename?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            title = { Text("Rename Folder") },
            text = {
                OutlinedTextField(
                    value = folderInputText,
                    onValueChange = {
                        folderInputText = it
                        folderInputError = null
                    },
                    label = { Text("New Folder Name") },
                    isError = folderInputError != null,
                    supportingText = if (folderInputError != null) { { Text(folderInputError ?: "") } } else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_folder_name_input")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        homeViewModel.renameFolder(folder.id, folderInputText) { result ->
                            if (result.isSuccess) {
                                folderToRename = null
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Folder renamed to '$folderInputText'.")
                                }
                            } else {
                                folderInputError = result.exceptionOrNull()?.message ?: "Invalid folder name"
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_rename_folder_button")
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToRename = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Dialog: Delete Folder Safely
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Folder?") },
            text = {
                Text("Deleting \"${folder.name}\" will not delete any notes. Notes inside this folder will remain safely in your general notes.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val folderName = folder.name
                        homeViewModel.deleteFolderSafely(folder.id)
                        folderToDelete = null
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Folder '$folderName' deleted. Notes moved to unfiled.")
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_folder_button")
                ) {
                    Text("Delete Folder")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
