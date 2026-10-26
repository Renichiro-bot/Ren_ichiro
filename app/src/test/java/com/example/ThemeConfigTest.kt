package com.example

import com.example.core.preferences.ThemeConfig
import com.example.core.preferences.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeConfigTest {

    @Test
    fun defaultThemeConfig_isSystemWithAMOLEDDisabled() {
        val config = ThemeConfig()
        assertEquals(ThemeMode.SYSTEM, config.themeMode)
        assertFalse(config.amoledDark)
        assertFalse(config.dynamicColor)
    }

    @Test
    fun themeConfig_copyUpdatesValuesCorrectly() {
        val initial = ThemeConfig()
        val updated = initial.copy(
            themeMode = ThemeMode.DARK,
            amoledDark = true,
            dynamicColor = true
        )

        assertEquals(ThemeMode.DARK, updated.themeMode)
        assertTrue(updated.amoledDark)
        assertTrue(updated.dynamicColor)
    }

    @Test
    fun themeMode_enumValuesArePreserved() {
        assertEquals("SYSTEM", ThemeMode.SYSTEM.name)
        assertEquals("LIGHT", ThemeMode.LIGHT.name)
        assertEquals("DARK", ThemeMode.DARK.name)
    }
}
