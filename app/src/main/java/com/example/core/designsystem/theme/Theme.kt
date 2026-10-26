package com.example.core.designsystem.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.core.preferences.ThemeConfig
import com.example.core.preferences.ThemeMode

private val VellumDarkColors = darkColorScheme(
    primary = VellumAmberDark,
    onPrimary = VellumDarkBase,
    primaryContainer = VellumAmberContainerDark,
    onPrimaryContainer = VellumDarkTextPrimary,
    secondary = VellumJadeDark,
    onSecondary = VellumDarkBase,
    background = VellumDarkBase,
    surface = VellumDarkSurface,
    surfaceVariant = VellumDarkElevated,
    onBackground = VellumDarkTextPrimary,
    onSurface = VellumDarkTextPrimary,
    onSurfaceVariant = VellumDarkTextSecondary,
    outline = VellumDarkBorder,
    outlineVariant = VellumDarkBorderSubtle,
    error = VellumCrimsonDark
)

private val VellumAmoledColors = VellumDarkColors.copy(
    background = VellumAmoledBase,
    surface = VellumAmoledSurface,
    surfaceVariant = VellumAmoledElevated
)

private val VellumLightColors = lightColorScheme(
    primary = VellumAmberLight,
    onPrimary = VellumLightSurface,
    primaryContainer = VellumAmberContainerLight,
    onPrimaryContainer = VellumLightTextPrimary,
    secondary = VellumJadeLight,
    onSecondary = VellumLightSurface,
    background = VellumLightBase,
    surface = VellumLightSurface,
    surfaceVariant = VellumLightElevated,
    onBackground = VellumLightTextPrimary,
    onSurface = VellumLightTextPrimary,
    onSurfaceVariant = VellumLightTextSecondary,
    outline = VellumLightBorder,
    outlineVariant = VellumLightBorderSubtle,
    error = VellumCrimsonLight
)

@Composable
fun VellumTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeConfig.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        themeConfig.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark && themeConfig.amoledDark -> VellumAmoledColors
        isDark -> VellumDarkColors
        else -> VellumLightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VellumTypography,
        content = content
    )
}
