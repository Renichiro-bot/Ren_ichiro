package com.example.feature.editor

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.core.designsystem.theme.NoteCardTints
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudSync
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatItalic
import androidx.compose.material.icons.rounded.FormatListNumbered
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.component.VellumBadge
import com.example.core.designsystem.component.VellumIconButton
import com.example.core.designsystem.theme.VellumShapes
import com.example.core.designsystem.theme.VellumSpacing
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import coil.compose.AsyncImage
import com.example.core.model.Attachment
import com.example.core.model.ChecklistItem
import com.example.core.model.NoteColorTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val isDark = isSystemInDarkTheme()

    val title by viewModel.title.collectAsState()
    val content by viewModel.content.collectAsState()
    val isPinned by viewModel.isPinned.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val isChecklist by viewModel.isChecklist.collectAsState()
    val checklistItems by viewModel.checklistItems.collectAsState()
    val folderId by viewModel.folderId.collectAsState()
    val tags by viewModel.tags.collectAsState()
    val availableFolders by viewModel.availableFolders.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val selectedColor by viewModel.colorTheme.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val isLoaded by viewModel.isLoaded.collectAsState()

    val attachments by viewModel.attachments.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showColorPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showAddTagDialog by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }
    var attachmentToDelete by remember { mutableStateOf<Attachment?>(null) }
    var tagInputText by remember { mutableStateOf("") }
    var newChecklistText by remember { mutableStateOf("") }

    // Activity Result Launchers
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.addAttachment(uri) { res ->
                coroutineScope.launch {
                    if (res.isSuccess) {
                        snackbarHostState.showSnackbar("Attached '${res.getOrNull()?.fileName}'.")
                    } else {
                        snackbarHostState.showSnackbar("Failed to attach file: ${res.exceptionOrNull()?.message}")
                    }
                }
            }
        }
    }

    val exportMarkdownLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportAsMarkdown(uri) { res ->
                coroutineScope.launch {
                    if (res.isSuccess) {
                        snackbarHostState.showSnackbar("Exported as Markdown successfully.")
                    } else {
                        snackbarHostState.showSnackbar("Export error: ${res.exceptionOrNull()?.message}")
                    }
                }
            }
        }
    }

    val exportTextLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportAsPlainText(uri) { res ->
                coroutineScope.launch {
                    if (res.isSuccess) {
                        snackbarHostState.showSnackbar("Exported as Plain Text successfully.")
                    } else {
                        snackbarHostState.showSnackbar("Export error: ${res.exceptionOrNull()?.message}")
                    }
                }
            }
        }
    }

    val pickImageForOcrLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.extractTextFromImage(context, uri)
        }
    }

    // Content TextFieldValue for cursor/selection-aware formatting
    var contentFieldValue by remember { mutableStateOf(TextFieldValue(content)) }

    // Sync contentFieldValue when initial content is loaded from database
    LaunchedEffect(isLoaded) {
        if (isLoaded && contentFieldValue.text.isEmpty() && content.isNotEmpty()) {
            contentFieldValue = TextFieldValue(
                text = content,
                selection = TextRange(content.length)
            )
        }
    }

    val (canvasColor, _) = resolveEditorColors(selectedColor, isDark)

    val handleBack = {
        viewModel.flushSave()
        onNavigateBack()
    }

    BackHandler(onBack = handleBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (viewModel.noteId != null && viewModel.noteId != "new") "Edit Note" else "New Note",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Subtle persistence status indicator
                        SaveStatusIndicator(saveStatus = saveStatus)
                    }
                },
                navigationIcon = {
                    VellumIconButton(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Save and return to notes",
                        onClick = handleBack,
                        testTag = "editor_back_button"
                    )
                },
                actions = {
                    // Pin toggle
                    IconButton(
                        onClick = { viewModel.togglePin() },
                        modifier = Modifier.testTag("editor_pin_toggle")
                    ) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Rounded.PushPin,
                            contentDescription = if (isPinned) "Unpin note" else "Pin note",
                            tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Archive action
                    IconButton(
                        onClick = {
                            viewModel.archiveCurrentNote {
                                onNavigateBack()
                            }
                        },
                        modifier = Modifier.testTag("editor_archive_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Archive,
                            contentDescription = "Archive note",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Palette color selector toggle (Appearance)
                    IconButton(
                        onClick = { showColorPicker = !showColorPicker },
                        modifier = Modifier.testTag("editor_color_palette_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Palette,
                            contentDescription = "Change note appearance",
                            tint = if (selectedColor != NoteColorTheme.DEFAULT) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 3-dot overflow menu
                    Box {
                        IconButton(
                            onClick = { showOverflowMenu = true },
                            modifier = Modifier.testTag("editor_more_options_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = "More options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            // Favorite toggle
                            DropdownMenuItem(
                                text = { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isFavorite) Icons.Filled.Star else Icons.Rounded.StarBorder,
                                        contentDescription = null,
                                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleFavorite()
                                },
                                modifier = Modifier.testTag("editor_favorite_toggle")
                            )

                            // Checklist mode toggle
                            DropdownMenuItem(
                                text = { Text(if (isChecklist) "Switch to text mode" else "Switch to checklist mode") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isChecklist) Icons.AutoMirrored.Rounded.Notes else Icons.AutoMirrored.Rounded.FormatListBulleted,
                                        contentDescription = null,
                                        tint = if (isChecklist) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    viewModel.toggleChecklistMode()
                                    contentFieldValue = TextFieldValue(
                                        text = viewModel.content.value,
                                        selection = TextRange(viewModel.content.value.length)
                                    )
                                },
                                modifier = Modifier.testTag("editor_checklist_toggle")
                            )

                            // Attach File button
                            DropdownMenuItem(
                                text = { Text("Add Attachment") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.AttachFile,
                                        contentDescription = null,
                                        tint = if (attachments.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    openDocumentLauncher.launch(arrayOf("image/*", "application/pdf", "text/*", "*/*"))
                                },
                                modifier = Modifier.testTag("editor_attach_file_button")
                            )

                            // Extract Text from Image (OCR) button
                            DropdownMenuItem(
                                text = { Text("Scan Document / OCR") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.DocumentScanner,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    pickImageForOcrLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.testTag("editor_ocr_button")
                            )

                            HorizontalDivider()

                            // Share Note
                            DropdownMenuItem(
                                text = { Text("Share Note") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    val shareIntent = viewModel.createShareIntent()
                                    if (shareIntent != null) {
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Note"))
                                    }
                                },
                                modifier = Modifier.testTag("action_share_note")
                            )

                            // Export as Markdown (.md)
                            DropdownMenuItem(
                                text = { Text("Export as Markdown (.md)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Description,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    val safeTitle = title.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "note" }
                                    exportMarkdownLauncher.launch("$safeTitle.md")
                                },
                                modifier = Modifier.testTag("action_export_markdown")
                            )

                            // Export as Plain Text (.txt)
                            DropdownMenuItem(
                                text = { Text("Export as Plain Text (.txt)") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Notes,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    val safeTitle = title.trim().replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "note" }
                                    exportTextLauncher.launch("$safeTitle.txt")
                                },
                                modifier = Modifier.testTag("action_export_plaintext")
                            )

                            // Delete action (if existing note)
                            if (viewModel.noteId != null && viewModel.noteId != "new") {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = { Text("Delete Note", color = MaterialTheme.colorScheme.error) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.DeleteOutline,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    },
                                    onClick = {
                                        showOverflowMenu = false
                                        showDeleteDialog = true
                                    },
                                    modifier = Modifier.testTag("editor_delete_button")
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = canvasColor
                )
            )
        },
        bottomBar = {
            if (!isChecklist) {
                // Docked Formatting Bar for Text Mode
                EditorFormattingBar(
                    onBold = {
                        contentFieldValue = FormattingUtils.applyBold(contentFieldValue)
                        viewModel.onContentChange(contentFieldValue.text)
                    },
                    onItalic = {
                        contentFieldValue = FormattingUtils.applyItalic(contentFieldValue)
                        viewModel.onContentChange(contentFieldValue.text)
                    },
                    onHeading = {
                        contentFieldValue = FormattingUtils.applyHeading(contentFieldValue)
                        viewModel.onContentChange(contentFieldValue.text)
                    },
                    onBulletList = {
                        contentFieldValue = FormattingUtils.applyBulletList(contentFieldValue)
                        viewModel.onContentChange(contentFieldValue.text)
                    },
                    onNumberedList = {
                        contentFieldValue = FormattingUtils.applyNumberedList(contentFieldValue)
                        viewModel.onContentChange(contentFieldValue.text)
                    },
                    onQuote = {
                        contentFieldValue = FormattingUtils.applyQuote(contentFieldValue)
                        viewModel.onContentChange(contentFieldValue.text)
                    },
                    onCode = {
                        contentFieldValue = FormattingUtils.applyCode(contentFieldValue)
                        viewModel.onContentChange(contentFieldValue.text)
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = canvasColor,
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Color picker row (animated expand/collapse)
            AnimatedVisibility(visible = showColorPicker) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(vertical = VellumSpacing.s, horizontal = VellumSpacing.l)
                ) {
                    Text(
                        text = "NOTE COLOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = VellumSpacing.xs)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(VellumSpacing.m),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(NoteColorTheme.entries) { theme ->
                            ColorThemeChip(
                                theme = theme,
                                isSelected = selectedColor == theme,
                                isDark = isDark,
                                onSelect = {
                                    viewModel.onColorSelect(theme)
                                }
                            )
                        }
                    }
                }
            }

            // Editor Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = VellumSpacing.xl)
            ) {
                // Title Field
                item {
                    TextField(
                        value = title,
                        onValueChange = { viewModel.onTitleChange(it) },
                        placeholder = {
                            Text(
                                text = "Title",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            )
                        },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_title_input")
                            .padding(vertical = VellumSpacing.s)
                    )
                }

                // Folder & Tag Metadata Bar
                item {
                    val currentFolder = availableFolders.find { it.id == folderId }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .horizontalScroll(rememberScrollState())
                    ) {
                        // Folder Selector Chip
                        Surface(
                            shape = VellumShapes.cornerFull,
                            color = if (currentFolder != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .clickable { showFolderPicker = true }
                                .testTag("editor_folder_picker_trigger")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (currentFolder != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentFolder?.name ?: "No Folder",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = if (currentFolder != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Tag Chips
                        tags.forEach { tag ->
                            Surface(
                                shape = VellumShapes.cornerFull,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.testTag("editor_tag_chip_$tag")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Remove tag $tag",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(VellumShapes.cornerFull)
                                            .clickable { viewModel.removeTag(tag) },
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }

                        // Add Tag Button Chip
                        Surface(
                            shape = VellumShapes.cornerFull,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clickable {
                                    tagInputText = ""
                                    showAddTagDialog = true
                                }
                                .testTag("editor_add_tag_trigger")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Add Tag",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Add Attachment Button Chip
                        Surface(
                            shape = VellumShapes.cornerFull,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .clickable {
                                    openDocumentLauncher.launch(arrayOf("image/*", "application/pdf", "text/*", "*/*"))
                                }
                                .testTag("editor_add_attachment_chip")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AttachFile,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Attach File",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                // Attachments List in Editor
                if (attachments.isNotEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = "ATTACHMENTS (${attachments.size})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(
                                    onClick = {
                                        openDocumentLauncher.launch(arrayOf("image/*", "application/pdf", "text/*", "*/*"))
                                    }
                                ) {
                                    Text("+ Add File", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(attachments, key = { it.id }) { attachment ->
                                    EditorAttachmentCard(
                                        attachment = attachment,
                                        onOpen = {
                                            val intent = viewModel.getViewIntent(attachment)
                                            if (intent != null) {
                                                try {
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    coroutineScope.launch {
                                                        snackbarHostState.showSnackbar("No app available on device to open this file type.")
                                                    }
                                                }
                                            } else {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("File not found on device storage.")
                                                }
                                            }
                                        },
                                        onDelete = {
                                            attachmentToDelete = attachment
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Subtitle: Metadata row (Word count & format indicator)
                item {
                    val wordCount = if (isChecklist) {
                        checklistItems.sumOf { if (it.text.isBlank()) 0 else it.text.trim().split("\\s+".toRegex()).size }
                    } else {
                        if (content.isBlank()) 0 else content.trim().split("\\s+".toRegex()).size
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isChecklist) "${checklistItems.size} checklist items" else "$wordCount words • ${maxOf(1, wordCount / 200)} min read",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        VellumBadge(
                            text = if (isChecklist) "Interactive Checklist" else "Markdown Text",
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        modifier = Modifier.padding(top = VellumSpacing.m, bottom = VellumSpacing.l)
                    )
                }

                // Checklist Mode vs Body Text Mode
                if (isChecklist) {
                    itemsIndexed(checklistItems, key = { _, item -> item.id }) { _, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.onChecklistItemToggle(item.id)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (item.isDone) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                    contentDescription = if (item.isDone) "Mark not done" else "Mark done",
                                    tint = if (item.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            TextField(
                                value = item.text,
                                onValueChange = { newText ->
                                    viewModel.onChecklistItemTextChange(item.id, newText)
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    textDecoration = if (item.isDone) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("checklist_item_input_${item.id}")
                            )

                            IconButton(
                                onClick = { viewModel.onChecklistItemRemove(item.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove item",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Add new checklist item row
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = VellumSpacing.m)
                        ) {
                            OutlinedTextField(
                                value = newChecklistText,
                                onValueChange = { newChecklistText = it },
                                placeholder = { Text("Add checklist item...") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (newChecklistText.isNotBlank()) {
                                            viewModel.onChecklistItemAdd(newChecklistText.trim())
                                            newChecklistText = ""
                                        }
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = VellumShapes.cornerM,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_checklist_input")
                            )

                            Spacer(modifier = Modifier.width(VellumSpacing.s))

                            IconButton(
                                onClick = {
                                    if (newChecklistText.isNotBlank()) {
                                        viewModel.onChecklistItemAdd(newChecklistText.trim())
                                        newChecklistText = ""
                                    }
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("add_checklist_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add item to checklist",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                } else {
                    // Plain / Markdown Text Body Field
                    item {
                        TextField(
                            value = contentFieldValue,
                            onValueChange = { newValue ->
                                contentFieldValue = newValue
                                viewModel.onContentChange(newValue.text)
                            },
                            placeholder = {
                                Text(
                                    text = "Start typing your note here...\n\nUse the toolbar below for bold, italic, headings, lists, and quotes.",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        lineHeight = 24.sp
                                    )
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 24.sp
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("editor_body_input")
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(140.dp))
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Move Note to Trash?") },
            text = { Text("Are you sure you want to move '${title.ifBlank { "Untitled Note" }}' to the Trash?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteCurrentNote {
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_note_button")
                ) {
                    Text("Move to Trash", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Folder Picker Dialog
    if (showFolderPicker) {
        AlertDialog(
            onDismissRequest = { showFolderPicker = false },
            title = { Text("Select Folder") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(VellumShapes.cornerM)
                            .clickable {
                                viewModel.setFolderById(null)
                                showFolderPicker = false
                            }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = folderId == null,
                            onClick = {
                                viewModel.setFolderById(null)
                                showFolderPicker = false
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("No Folder (Unfiled)", style = MaterialTheme.typography.bodyMedium)
                    }

                    availableFolders.forEach { f ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(VellumShapes.cornerM)
                                .clickable {
                                    viewModel.setFolderById(f.id)
                                    showFolderPicker = false
                                }
                                .padding(vertical = 6.dp)
                        ) {
                            RadioButton(
                                selected = folderId == f.id,
                                onClick = {
                                    viewModel.setFolderById(f.id)
                                    showFolderPicker = false
                                }
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
                TextButton(onClick = { showFolderPicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Add Tag Dialog
    if (showAddTagDialog) {
        AlertDialog(
            onDismissRequest = { showAddTagDialog = false },
            title = { Text("Add Tag") },
            text = {
                Column {
                    OutlinedTextField(
                        value = tagInputText,
                        onValueChange = { tagInputText = it },
                        label = { Text("Tag name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_tag_name_input")
                    )

                    if (availableTags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Suggestions:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            availableTags.forEach { t ->
                                FilterChip(
                                    selected = tagInputText.trim().equals(t.name, ignoreCase = true),
                                    onClick = { tagInputText = t.name },
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
                        val clean = tagInputText.trim().removePrefix("#").trim()
                        if (clean.isNotBlank()) {
                            viewModel.addTag(clean)
                            showAddTagDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_tag_button")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTagDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Attachment Delete Confirmation Dialog
    attachmentToDelete?.let { att ->
        AlertDialog(
            onDismissRequest = { attachmentToDelete = null },
            title = { Text("Delete Attachment?") },
            text = {
                Text("Are you sure you want to remove \"${att.fileName}\" from this note? The file will be safely deleted from internal storage.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toDelete = att
                        attachmentToDelete = null
                        viewModel.deleteAttachment(toDelete) { res ->
                            coroutineScope.launch {
                                if (res.isSuccess) {
                                    snackbarHostState.showSnackbar("Attachment '${toDelete.fileName}' deleted.")
                                } else {
                                    snackbarHostState.showSnackbar("Failed to delete attachment.")
                                }
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_delete_attachment_button")
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { attachmentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // OCR State Dialogs (Feature C: On-Device OCR)
    val ocrState by viewModel.ocrState.collectAsState()

    when (val state = ocrState) {
        is OcrState.Processing -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissOcrState() },
                title = { Text("Extracting Text") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Text(
                            text = "Analyzing image on-device using ML Kit…",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissOcrState() }) {
                        Text("Cancel")
                    }
                }
            )
        }
        is OcrState.Success -> {
            var reviewText by remember(state.result) { mutableStateOf(state.result.fullText) }
            AlertDialog(
                onDismissRequest = { viewModel.dismissOcrState() },
                title = {
                    Column {
                        Text("Extracted Text (On-Device OCR)")
                        Text(
                            text = "${state.result.characterCount} characters • ${state.result.lineCount} lines • 100% On-Device",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Review and edit recognized text before inserting:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = reviewText,
                            onValueChange = { reviewText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .testTag("ocr_review_text_input"),
                            textStyle = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                confirmButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                viewModel.applyOcrAppend(reviewText)
                                contentFieldValue = TextFieldValue(
                                    text = viewModel.content.value,
                                    selection = TextRange(viewModel.content.value.length)
                                )
                            },
                            modifier = Modifier.testTag("ocr_append_button")
                        ) {
                            Text("Append")
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.applyOcrReplace(reviewText)
                                contentFieldValue = TextFieldValue(
                                    text = reviewText,
                                    selection = TextRange(reviewText.length)
                                )
                            },
                            modifier = Modifier.testTag("ocr_replace_button")
                        ) {
                            Text("Replace")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissOcrState() }) {
                        Text("Cancel")
                    }
                }
            )
        }
        is OcrState.Error -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissOcrState() },
                title = { Text("Text Extraction") },
                text = { Text(state.message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissOcrState() }) {
                        Text("OK")
                    }
                }
            )
        }
        OcrState.Idle -> { /* Idle */ }
    }
}

@Composable
private fun EditorFormattingBar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onHeading: () -> Unit,
    onBulletList: () -> Unit,
    onNumberedList: () -> Unit,
    onQuote: () -> Unit,
    onCode: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("editor_formatting_toolbar")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VellumSpacing.s, vertical = 4.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            IconButton(onClick = onBold, modifier = Modifier.testTag("format_bold_button")) {
                Icon(
                    imageVector = Icons.Rounded.FormatBold,
                    contentDescription = "Format Bold (**text**)",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onItalic, modifier = Modifier.testTag("format_italic_button")) {
                Icon(
                    imageVector = Icons.Rounded.FormatItalic,
                    contentDescription = "Format Italic (*text*)",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onHeading, modifier = Modifier.testTag("format_heading_button")) {
                Icon(
                    imageVector = Icons.Rounded.Title,
                    contentDescription = "Insert Heading (# )",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onBulletList, modifier = Modifier.testTag("format_bullet_button")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.FormatListBulleted,
                    contentDescription = "Insert Bullet List (• )",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onNumberedList, modifier = Modifier.testTag("format_numbered_button")) {
                Icon(
                    imageVector = Icons.Rounded.FormatListNumbered,
                    contentDescription = "Insert Numbered List (1. )",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onQuote, modifier = Modifier.testTag("format_quote_button")) {
                Icon(
                    imageVector = Icons.Rounded.FormatQuote,
                    contentDescription = "Insert Blockquote (> )",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            IconButton(onClick = onCode, modifier = Modifier.testTag("format_code_button")) {
                Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = "Format Inline Code (`code`)",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SaveStatusIndicator(saveStatus: SaveStatus) {
    val (label, icon, color) = when (saveStatus) {
        SaveStatus.SAVED -> Triple(
            "Saved",
            Icons.Rounded.CloudDone,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
        )
        SaveStatus.SAVING -> Triple(
            "Saving...",
            Icons.Rounded.CloudSync,
            MaterialTheme.colorScheme.tertiary
        )
        SaveStatus.UNSAVED -> Triple(
            "Editing",
            Icons.Rounded.CloudSync,
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(VellumShapes.cornerFull)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .testTag("editor_save_status_indicator")
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(12.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = color
        )
    }
}

@Composable
private fun ColorThemeChip(
    theme: NoteColorTheme,
    isSelected: Boolean,
    isDark: Boolean,
    onSelect: () -> Unit
) {
    val (fillColor, borderColor) = resolveEditorColors(theme, isDark)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(fillColor)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else borderColor,
                shape = CircleShape
            )
            .clickable(onClick = onSelect)
            .testTag("color_chip_${theme.id}")
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "${theme.label} selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun resolveEditorColors(theme: NoteColorTheme, isDark: Boolean): Pair<Color, Color> {
    return when (theme) {
        NoteColorTheme.DEFAULT -> {
            MaterialTheme.colorScheme.background to MaterialTheme.colorScheme.outlineVariant
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

@Composable
private fun EditorAttachmentCard(
    attachment: Attachment,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = VellumShapes.cornerM,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .width(170.dp)
            .clip(VellumShapes.cornerM)
            .clickable(onClick = onOpen)
            .testTag("attachment_item_${attachment.id}")
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Preview thumbnail for images or icon for PDF/Doc
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(VellumShapes.cornerS)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (attachment.isImage) {
                    val app = LocalContext.current.applicationContext as? com.example.VellumApplication
                    val file = app?.attachmentStorage?.getFile(attachment)
                    if (file != null && file.exists()) {
                        AsyncImage(
                            model = file,
                            contentDescription = attachment.fileName,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.AttachFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (attachment.isPdf) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.PictureAsPdf,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "PDF",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Rounded.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "DOC",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.fileName,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = attachment.formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("delete_attachment_button_${attachment.id}")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove attachment",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
