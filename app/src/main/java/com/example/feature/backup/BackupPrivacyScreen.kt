package com.example.feature.backup

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.ui.platform.LocalContext
import com.example.core.widget.VellumWidgetManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.backup.BackupValidationResult
import com.example.core.backup.ImportPreview
import com.example.core.backup.RestoreMode
import com.example.core.designsystem.component.VellumBadge
import com.example.core.designsystem.component.VellumIconButton
import com.example.core.designsystem.theme.VellumShapes
import com.example.core.designsystem.theme.VellumSpacing
import com.example.core.model.Folder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupPrivacyScreen(
    viewModel: BackupPrivacyViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollState = rememberScrollState()

    val storageStats by viewModel.storageStats.collectAsState()
    val isBusy by viewModel.isBusy.collectAsState()
    val busyMessage by viewModel.busyMessage.collectAsState()
    val validationResult by viewModel.validationResult.collectAsState()
    val importPreview by viewModel.importPreview.collectAsState()
    val isAppLockEnabled by viewModel.isAppLockEnabled.collectAsState()
    val availableFolders by viewModel.availableFolders.collectAsState()

    val context = LocalContext.current
    var showWidgetPreviews by remember { mutableStateOf(VellumWidgetManager.isWidgetPreviewEnabled(context)) }

    // Restore mode selection state for the validation modal
    var selectedRestoreMode by remember { mutableStateOf(RestoreMode.MERGE) }
    var selectedImportFolderId by remember { mutableStateOf<String?>(null) }
    var showFolderDropdown by remember { mutableStateOf(false) }

    // System File Pickers
    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.createBackup(uri) { res ->
                coroutineScope.launch {
                    if (res.isSuccess) {
                        val manifest = res.getOrNull()
                        snackbarHostState.showSnackbar(
                            "Backup verified & saved: ${manifest?.noteCount ?: 0} notes, ${manifest?.attachmentCount ?: 0} attachments."
                        )
                    } else {
                        snackbarHostState.showSnackbar("Backup error: ${res.exceptionOrNull()?.message}")
                    }
                }
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.inspectBackup(uri)
        }
    }

    val openImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.inspectImportFile(uri) { errorMsg ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Import inspection failed: $errorMsg")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Backup & Privacy",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    VellumIconButton(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Return",
                        onClick = onNavigateBack,
                        testTag = "backup_privacy_back_button"
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = VellumSpacing.m, vertical = VellumSpacing.s),
            verticalArrangement = Arrangement.spacedBy(VellumSpacing.l)
        ) {
            // Privacy Architecture Transparency Banner
            PrivacyArchitectureCard()

            // On-Device OCR Transparency Card
            OcrPrivacyCard()

            // Storage Breakdown Card
            StorageBreakdownCard(
                stats = storageStats,
                onRefresh = { viewModel.refreshStorageStats() }
            )

            // Home-Screen Widget Privacy Controls
            WidgetPrivacySection(
                showPreviews = showWidgetPreviews,
                onTogglePreviews = { enabled ->
                    showWidgetPreviews = enabled
                    VellumWidgetManager.setWidgetPreviewEnabled(context, enabled)
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (enabled) "Widget note previews enabled." else "Widget note previews hidden."
                        )
                    }
                }
            )

            // Backup & Restore Controls
            BackupRestoreSection(
                onCreateBackup = {
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                    createBackupLauncher.launch("vellum_backup_$timeStamp.zip")
                },
                onRestoreBackup = {
                    openBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                }
            )

            // Import Note Section
            ImportSection(
                onImportClick = {
                    openImportLauncher.launch(arrayOf("text/plain", "text/markdown", "*/*"))
                }
            )

            // App Security / Device Lock Section
            SecurityLockSection(
                isEnabled = isAppLockEnabled,
                isDeviceSecure = viewModel.isDeviceSecure,
                onToggle = { viewModel.toggleAppLock(it) }
            )

            Spacer(modifier = Modifier.height(VellumSpacing.xl))
        }
    }

    // Modal: Progress / Busy Overlay
    if (isBusy) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Processing Data") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = busyMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }

    // Modal: Pre-Restore Validation & Confirmation
    validationResult?.let { result ->
        when (result) {
            is BackupValidationResult.Error -> {
                AlertDialog(
                    onDismissRequest = { viewModel.dismissValidationDialog() },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(32.dp)
                        )
                    },
                    title = { Text("Invalid Backup File") },
                    text = {
                        Text(
                            text = result.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = { viewModel.dismissValidationDialog() },
                            modifier = Modifier.testTag("dismiss_validation_error_button")
                        ) {
                            Text("Dismiss")
                        }
                    }
                )
            }
            is BackupValidationResult.Success -> {
                val manifest = result.manifest
                val dateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(manifest.createdAtEpochMs))

                AlertDialog(
                    onDismissRequest = { viewModel.dismissValidationDialog() },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Backup Validated")
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "Created: $dateStr • Format v${manifest.formatVersion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Item summary counts
                            Surface(
                                shape = VellumShapes.cornerM,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("• ${manifest.noteCount} Notes", style = MaterialTheme.typography.bodyMedium)
                                    Text("• ${manifest.folderCount} Folders • ${manifest.tagCount} Tags", style = MaterialTheme.typography.bodyMedium)
                                    Text("• ${manifest.taskCount} Tasks • ${manifest.reminderCount} Reminders", style = MaterialTheme.typography.bodyMedium)
                                    Text("• ${result.attachmentFilesPresent} of ${manifest.attachmentCount} Attachment Files", style = MaterialTheme.typography.bodyMedium)
                                }
                            }

                            if (result.warnings.isNotEmpty()) {
                                Surface(
                                    shape = VellumShapes.cornerM,
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = result.warnings.joinToString("\n"),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }

                            Text(
                                text = "Choose Restore Mode:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            // Option: Merge
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(VellumShapes.cornerM)
                                    .clickable { selectedRestoreMode = RestoreMode.MERGE }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedRestoreMode == RestoreMode.MERGE,
                                    onClick = { selectedRestoreMode = RestoreMode.MERGE }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Merge Backup (Safe)",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = "Imports records and preserves any newer or existing notes on this device.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Option: Replace
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(VellumShapes.cornerM)
                                    .clickable { selectedRestoreMode = RestoreMode.REPLACE }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedRestoreMode == RestoreMode.REPLACE,
                                    onClick = { selectedRestoreMode = RestoreMode.REPLACE }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Replace All Data",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    )
                                    Text(
                                        text = "Replaces current data with this backup. Existing notes not in the backup will be removed.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.executeRestore(selectedRestoreMode) { restoreRes ->
                                    coroutineScope.launch {
                                        if (restoreRes.isSuccess) {
                                            snackbarHostState.showSnackbar(
                                                "Restored: ${restoreRes.restoredNotesCount} notes, ${restoreRes.restoredAttachmentsCount} attachments."
                                            )
                                        } else {
                                            snackbarHostState.showSnackbar("Restore error: ${restoreRes.message}")
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedRestoreMode == RestoreMode.REPLACE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("confirm_restore_button")
                        ) {
                            Text(if (selectedRestoreMode == RestoreMode.REPLACE) "Replace & Restore" else "Merge & Restore")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.dismissValidationDialog() },
                            modifier = Modifier.testTag("cancel_restore_button")
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }

    // Modal: Note Import Preview Dialog
    importPreview?.let { preview ->
        var editedTitle by remember { mutableStateOf(preview.suggestedTitle) }

        AlertDialog(
            onDismissRequest = { viewModel.dismissImportPreview() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Note Preview")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Note Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("import_title_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VellumBadge(
                            text = "${preview.characterCount} chars",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        VellumBadge(
                            text = "${preview.lineCount} lines",
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (preview.isChecklist) {
                            VellumBadge(
                                text = "${preview.checklistItems.size} checklist items",
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Content snippet preview
                    Surface(
                        shape = VellumShapes.cornerM,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = preview.body.take(280) + if (preview.body.length > 280) "..." else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(10.dp)
                        )
                    }

                    // Optional folder destination
                    Box {
                        OutlinedButton(
                            onClick = { showFolderDropdown = true },
                            modifier = Modifier.fillMaxWidth().testTag("import_folder_select_button")
                        ) {
                            val folderName = availableFolders.find { it.id == selectedImportFolderId }?.name ?: "No Folder (General)"
                            Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Folder: $folderName")
                        }
                        DropdownMenu(
                            expanded = showFolderDropdown,
                            onDismissRequest = { showFolderDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No Folder (General)") },
                                onClick = {
                                    selectedImportFolderId = null
                                    showFolderDropdown = false
                                }
                            )
                            availableFolders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name) },
                                    onClick = {
                                        selectedImportFolderId = folder.id
                                        showFolderDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalPreview = preview.copy(suggestedTitle = editedTitle.ifBlank { "Imported Note" })
                        viewModel.confirmImport(selectedImportFolderId) { success, msg ->
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(msg)
                            }
                        }
                    },
                    modifier = Modifier.testTag("confirm_import_button")
                ) {
                    Text("Import Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissImportPreview() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun PrivacyArchitectureCard() {
    Card(
        shape = VellumShapes.cornerL,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("privacy_architecture_card")
    ) {
        Column(
            modifier = Modifier.padding(VellumSpacing.l),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Local Sandboxing & Privacy",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "• 100% On-Device & Offline: All notes, attachments, tags, and tasks are stored in your device's private SQLite database. Zero telemetry and zero cloud database connections.\n" +
                    "• Private Sandboxing: Attachments are kept in private app storage, completely shielded from other applications.\n" +
                    "• Zero Broad Permissions: Vellum uses Android's zero-permission system photo picker and SAF file providers.\n" +
                    "• Safe Sharing: FileProvider temporarily grants read-only access only when you explicitly tap to open or share an attachment.\n" +
                    "• Trash Retention: Trashed notes keep their files so you can recover them anytime. Emptying Trash purges all files permanently.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
private fun StorageBreakdownCard(
    stats: StorageStats,
    onRefresh: () -> Unit
) {
    Card(
        shape = VellumShapes.cornerL,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("storage_breakdown_card")
    ) {
        Column(
            modifier = Modifier.padding(VellumSpacing.l),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Storage Breakdown",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StorageStatItem(label = "Notes", count = "${stats.totalNotes}")
                StorageStatItem(label = "Folders", count = "${stats.totalFolders}")
                StorageStatItem(label = "Tags", count = "${stats.totalTags}")
                StorageStatItem(label = "Attachments", count = "${stats.totalAttachments}")
            }
        }
    }
}

@Composable
private fun StorageStatItem(label: String, count: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = count,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun BackupRestoreSection(
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit
) {
    Card(
        shape = VellumShapes.cornerL,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("backup_restore_card")
    ) {
        Column(
            modifier = Modifier.padding(VellumSpacing.l),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Backup & Restore",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Backups are self-contained, standard ZIP archives containing your notes, folders, tags, tasks, reminders, and attachment files. You can save and restore them completely offline.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCreateBackup,
                    shape = VellumShapes.cornerM,
                    modifier = Modifier.weight(1f).testTag("create_backup_button")
                ) {
                    Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Create Backup")
                }

                OutlinedButton(
                    onClick = onRestoreBackup,
                    shape = VellumShapes.cornerM,
                    modifier = Modifier.weight(1f).testTag("restore_backup_button")
                ) {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore Backup")
                }
            }
        }
    }
}

@Composable
private fun ImportSection(
    onImportClick: () -> Unit
) {
    Card(
        shape = VellumShapes.cornerL,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("import_section_card")
    ) {
        Column(
            modifier = Modifier.padding(VellumSpacing.l),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Import External Notes",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Import plain text (.txt) or Markdown (.md) documents directly into Vellum. Headings and markdown checklist items (- [ ] / - [x]) are detected automatically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onImportClick,
                shape = VellumShapes.cornerM,
                modifier = Modifier.fillMaxWidth().testTag("import_note_button")
            ) {
                Icon(Icons.Rounded.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Select .txt or .md File to Import")
            }
        }
    }
}

@Composable
private fun SecurityLockSection(
    isEnabled: Boolean,
    isDeviceSecure: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        shape = VellumShapes.cornerL,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("security_lock_card")
    ) {
        Column(
            modifier = Modifier.padding(VellumSpacing.l),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                        contentDescription = null,
                        tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device Screen Lock",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("app_lock_switch")
                )
            }

            Text(
                text = "When active, Vellum requires your device PIN, pattern, or biometric authentication to open. Note: this provides casual physical snooping protection; it is front-end access control and does not encrypt SQLite database files.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isDeviceSecure && isEnabled) {
                Surface(
                    shape = VellumShapes.cornerM,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Warning: No secure screen lock (PIN/Password/Biometrics) is currently configured in Android device settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPrivacySection(
    showPreviews: Boolean,
    onTogglePreviews: (Boolean) -> Unit
) {
    Card(
        shape = VellumShapes.cornerL,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("widget_privacy_card")
    ) {
        Column(
            modifier = Modifier.padding(VellumSpacing.l),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Widgets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Home-Screen Widgets",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = showPreviews,
                    onCheckedChange = onTogglePreviews,
                    modifier = Modifier.testTag("widget_previews_switch")
                )
            }

            Text(
                text = if (showPreviews) {
                    "Note body previews are visible on your home screen widgets. Turn off to hide previews and only display note titles for privacy against shoulder-surfing."
                } else {
                    "Note previews are hidden on home screen widgets. Only note titles and pin indicators are shown."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OcrPrivacyCard() {
    Card(
        shape = VellumShapes.cornerL,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("ocr_privacy_card")
    ) {
        Column(
            modifier = Modifier.padding(VellumSpacing.l),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.DocumentScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "On-Device OCR & Document Scanning",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "• 100% On-Device Processing: Text recognition runs locally on your device via Google ML Kit. Scanned receipts, whiteboards, and documents are never sent over the internet.\n" +
                    "• Interactive Text Review: Extracted text is always presented in an interactive review modal first. You choose whether to append, replace, or cancel before any text touches your note.\n" +
                    "• Zero Cloud AI / Zero Data Logging: No analytics, tracking, or cloud models are invoked during text extraction.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )
        }
    }
}

