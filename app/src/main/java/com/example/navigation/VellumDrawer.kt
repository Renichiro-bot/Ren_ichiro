package com.example.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.component.VellumBadge
import com.example.core.designsystem.theme.VellumShapes
import com.example.core.designsystem.theme.VellumSpacing
import com.example.core.model.Folder
import com.example.core.model.Tag

@Composable
fun VellumDrawerContent(
    currentRoute: String,
    folders: List<Folder> = emptyList(),
    tags: List<Tag> = emptyList(),
    selectedFolder: Folder? = null,
    selectedTag: Tag? = null,
    onNavigate: (VellumDestination) -> Unit,
    onSelectFolder: (Folder?) -> Unit = {},
    onSelectTag: (Tag?) -> Unit = {},
    onSelectFavorites: () -> Unit = {},
    onCreateFolderClick: () -> Unit = {},
    onRenameFolderClick: (Folder) -> Unit = {},
    onDeleteFolderClick: (Folder) -> Unit = {},
    onResetSampleNotes: () -> Unit,
    onClearAllNotes: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .testTag("vellum_drawer_content")
            .fillMaxHeight()
            .width(310.dp)
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(scrollState)
            .padding(VellumSpacing.l)
    ) {
        // Brand Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = VellumSpacing.m)
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(28.dp)
                    .background(MaterialTheme.colorScheme.primary, VellumShapes.cornerFull)
            )
            Spacer(modifier = Modifier.width(VellumSpacing.m))
            Column {
                Text(
                    text = "Vellum",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Level 5 • Folders & Tags",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = VellumSpacing.m)
        )

        // Core Views
        DrawerItem(
            label = "All Notes",
            icon = Icons.Rounded.Description,
            selected = currentRoute == VellumDestination.Home.route && selectedFolder == null && selectedTag == null,
            onClick = {
                onSelectFolder(null)
                onSelectTag(null)
                onNavigate(VellumDestination.Home)
            },
            testTag = "drawer_item_all_notes"
        )

        DrawerItem(
            label = "Favorites",
            icon = Icons.Rounded.Star,
            selected = false,
            onClick = {
                onSelectFavorites()
                onNavigate(VellumDestination.Home)
            },
            testTag = "drawer_item_favorites"
        )

        DrawerItem(
            label = "Search Notes",
            icon = Icons.Rounded.Search,
            selected = currentRoute == VellumDestination.Search.route,
            onClick = { onNavigate(VellumDestination.Search) },
            testTag = "drawer_item_search"
        )

        DrawerItem(
            label = "Archive",
            icon = Icons.Rounded.Archive,
            selected = currentRoute == VellumDestination.Archive.route,
            onClick = { onNavigate(VellumDestination.Archive) },
            testTag = "drawer_item_archive"
        )

        DrawerItem(
            label = "Trash",
            icon = Icons.Rounded.Delete,
            selected = currentRoute == VellumDestination.Trash.route,
            onClick = { onNavigate(VellumDestination.Trash) },
            testTag = "drawer_item_trash"
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = VellumSpacing.m)
        )

        // Folders Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = VellumSpacing.m, end = VellumSpacing.xs, bottom = VellumSpacing.xs)
        ) {
            Text(
                text = "FOLDERS",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            IconButton(
                onClick = onCreateFolderClick,
                modifier = Modifier
                    .size(28.dp)
                    .testTag("drawer_add_folder_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Create folder",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (folders.isEmpty()) {
            Text(
                text = "No folders created yet",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = VellumSpacing.m, top = 2.dp, bottom = 6.dp)
            )
        } else {
            folders.forEach { folder ->
                FolderDrawerItem(
                    folder = folder,
                    isSelected = selectedFolder?.id == folder.id,
                    onClick = {
                        onSelectFolder(folder)
                        onNavigate(VellumDestination.Home)
                    },
                    onRename = { onRenameFolderClick(folder) },
                    onDelete = { onDeleteFolderClick(folder) }
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = VellumSpacing.m)
        )

        // Tags Section Header
        Text(
            text = "TAGS",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = VellumSpacing.m, bottom = VellumSpacing.xs)
        )

        if (tags.isEmpty()) {
            Text(
                text = "No tags yet. Add tags in the note editor.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(start = VellumSpacing.m, top = 2.dp, bottom = 6.dp)
            )
        } else {
            tags.forEach { tag ->
                TagDrawerItem(
                    tag = tag,
                    isSelected = selectedTag?.id == tag.id,
                    onClick = {
                        onSelectTag(if (selectedTag?.id == tag.id) null else tag)
                        onNavigate(VellumDestination.Home)
                    }
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = VellumSpacing.m)
        )

        // Settings / Appearance
        DrawerItem(
            label = "Appearance",
            icon = Icons.Rounded.ColorLens,
            selected = currentRoute == VellumDestination.Appearance.route,
            onClick = { onNavigate(VellumDestination.Appearance) },
            testTag = "drawer_item_appearance"
        )

        DrawerItem(
            label = "Backup & Privacy",
            icon = Icons.Rounded.Security,
            selected = currentRoute == VellumDestination.BackupPrivacy.route,
            onClick = { onNavigate(VellumDestination.BackupPrivacy) },
            testTag = "drawer_item_backup_privacy"
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = VellumSpacing.m)
        )

        // Prototype Tools Section
        Text(
            text = "PROTOTYPE TOOLS",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = VellumSpacing.m, bottom = VellumSpacing.s)
        )

        DrawerActionItem(
            label = "Reset Sample Notes",
            icon = Icons.Rounded.RestartAlt,
            onClick = onResetSampleNotes,
            testTag = "drawer_reset_samples_action"
        )

        DrawerActionItem(
            label = "Clear All Notes",
            icon = Icons.Rounded.DeleteSweep,
            onClick = onClearAllNotes,
            testTag = "drawer_clear_all_action"
        )
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    NavigationDrawerItem(
        label = { Text(text = label, style = MaterialTheme.typography.titleMedium) },
        icon = { Icon(imageVector = icon, contentDescription = null) },
        selected = selected,
        onClick = onClick,
        shape = VellumShapes.cornerM,
        colors = NavigationDrawerItemDefaults.colors(
            selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            selectedTextColor = MaterialTheme.colorScheme.primary,
            selectedIconColor = MaterialTheme.colorScheme.primary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurface,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
            .testTag(testTag)
            .padding(vertical = VellumSpacing.xxs)
    )
}

@Composable
private fun FolderDrawerItem(
    folder: Folder,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(VellumShapes.cornerM)
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .testTag("drawer_folder_${folder.id}")
            .padding(horizontal = VellumSpacing.m, vertical = VellumSpacing.xs)
    ) {
        Icon(
            imageVector = Icons.Rounded.Folder,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(VellumSpacing.m))
        Text(
            text = folder.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (folder.noteCount > 0) {
            VellumBadge(
                text = "${folder.noteCount}",
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(
                onClick = { showMenu = true },
                modifier = Modifier
                    .size(28.dp)
                    .testTag("folder_menu_button_${folder.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "Folder options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                    onClick = {
                        showMenu = false
                        onRename()
                    },
                    modifier = Modifier.testTag("folder_rename_action_${folder.id}")
                )
                DropdownMenuItem(
                    text = { Text("Delete Safely", color = MaterialTheme.colorScheme.error) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    onClick = {
                        showMenu = false
                        onDelete()
                    },
                    modifier = Modifier.testTag("folder_delete_action_${folder.id}")
                )
            }
        }
    }
}

@Composable
private fun TagDrawerItem(
    tag: Tag,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(VellumShapes.cornerM)
            .background(if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .testTag("drawer_tag_${tag.id}")
            .padding(horizontal = VellumSpacing.m, vertical = VellumSpacing.xs)
    ) {
        Icon(
            imageVector = Icons.Rounded.Tag,
            contentDescription = null,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(VellumSpacing.m))
        Text(
            text = tag.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (tag.noteCount > 0) {
            VellumBadge(
                text = "${tag.noteCount}",
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DrawerActionItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(VellumShapes.cornerM)
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(horizontal = VellumSpacing.m, vertical = VellumSpacing.s)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(VellumSpacing.m))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
