package com.example.core.preferences

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class ThemeConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val amoledDark: Boolean = false,
    val dynamicColor: Boolean = false
)
