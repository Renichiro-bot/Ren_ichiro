package com.example.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import com.example.core.designsystem.component.VellumBadge
import com.example.core.designsystem.component.VellumButton
import com.example.core.designsystem.component.VellumCard
import com.example.core.designsystem.component.VellumIconButton
import com.example.core.designsystem.component.VellumSettingsRadioRow
import com.example.core.designsystem.component.VellumSettingsSwitchRow
import com.example.core.designsystem.component.VellumTopAppBar
import com.example.core.designsystem.theme.VellumSpacing
import com.example.core.preferences.ThemeConfig
import com.example.core.preferences.ThemeMode
import com.example.core.preferences.ThemeViewModel

@Composable
fun AppearanceScreen(
    viewModel: ThemeViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeConfig by viewModel.themeConfig.collectAsState()

    Scaffold(
        topBar = {
            VellumTopAppBar(
                title = "Appearance",
                navigationIcon = {
                    VellumIconButton(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Navigate Back to Home",
                        onClick = onNavigateBack,
                        testTag = "appearance_back_button"
                    )
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Live Token Preview Card
            TokenPreviewCard(
                themeConfig = themeConfig,
                modifier = Modifier.padding(VellumSpacing.l)
            )

            Text(
                text = "THEME MODE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = VellumSpacing.l, vertical = VellumSpacing.s)
            )

            VellumSettingsRadioRow(
                title = "System Default",
                subtitle = "Follow your Android system settings",
                selected = themeConfig.themeMode == ThemeMode.SYSTEM,
                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                testTag = "theme_mode_system"
            )

            VellumSettingsRadioRow(
                title = "Light (Warm Alabaster)",
                subtitle = "Comfortable daytime contrast with subtle warmth",
                selected = themeConfig.themeMode == ThemeMode.LIGHT,
                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                testTag = "theme_mode_light"
            )

            VellumSettingsRadioRow(
                title = "Dark (Obsidian)",
                subtitle = "Deep neutral surfaces with refined ambient depth",
                selected = themeConfig.themeMode == ThemeMode.DARK,
                onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                testTag = "theme_mode_dark"
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(vertical = VellumSpacing.m)
            )

            Text(
                text = "SURFACE CONTRAST",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = VellumSpacing.l, vertical = VellumSpacing.s)
            )

            VellumSettingsSwitchRow(
                title = "Pure Black (AMOLED)",
                subtitle = "Utilize zero-luminance pixels for maximum battery efficiency in dark theme",
                checked = themeConfig.amoledDark,
                enabled = themeConfig.themeMode != ThemeMode.LIGHT,
                onCheckedChange = { viewModel.setAmoledDark(it) },
                testTag = "theme_switch_amoled"
            )

            VellumSettingsSwitchRow(
                title = "Dynamic Color (Material You)",
                subtitle = "Harmonize accents with your Android system wallpaper (Android 12+)",
                checked = themeConfig.dynamicColor,
                onCheckedChange = { viewModel.setDynamicColor(it) },
                testTag = "theme_switch_dynamic"
            )

            Spacer(modifier = Modifier.height(VellumSpacing.huge))
        }
    }
}

@Composable
private fun TokenPreviewCard(
    themeConfig: ThemeConfig,
    modifier: Modifier = Modifier
) {
    VellumCard(modifier = modifier) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(VellumSpacing.s))
                    Text(
                        text = "Vellum Design Preview",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                val modeLabel = when (themeConfig.themeMode) {
                    ThemeMode.SYSTEM -> "System"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> if (themeConfig.amoledDark) "AMOLED" else "Dark"
                }
                VellumBadge(
                    text = modeLabel,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.height(VellumSpacing.m))

            Text(
                text = "Tactile Monolith Design System",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(VellumSpacing.xs))

            Text(
                text = "Dynamic WCAG 2.1 AA compliant typography and contrast tokens. Surfaces adjust automatically to light and dark settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(VellumSpacing.l))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(VellumSpacing.s)
            ) {
                VellumButton(
                    text = "Primary Accent",
                    onClick = {}
                )
                VellumBadge(
                    text = "Editorial Amber",
                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f),
                    contentColor = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
