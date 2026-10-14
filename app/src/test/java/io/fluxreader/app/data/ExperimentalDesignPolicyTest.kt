package io.fluxreader.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperimentalDesignPolicyTest {
    @Test fun `previous settings and fresh installs default to standard UI`() {
        val defaults = ReaderSettings()
        assertFalse(defaults.experimentalDesignEnabled)
        assertFalse(defaults.useWastelandDesign)
        assertFalse(defaults.useCyberpunkDesign)
    }

    @Test fun `selecting an experimental palette alone never enables the redesign`() {
        for (theme in listOf(ReaderThemeMode.WASTELAND_TERMINAL, ReaderThemeMode.CYBERPUNK_NEON)) {
            val settings = ReaderSettings(theme = theme)
            assertFalse(settings.useWastelandDesign)
            assertFalse(settings.useCyberpunkDesign)
            assertEquals(ReaderThemeMode.OLED, settings.activeUiTheme)
        }
    }

    @Test fun `Wasteland and Cyberpunk layouts are independently enabled`() {
        val wasteland = ReaderSettings(theme = ReaderThemeMode.WASTELAND_TERMINAL, experimentalDesignEnabled = true)
        assertTrue(wasteland.useWastelandDesign)
        assertFalse(wasteland.useCyberpunkDesign)
        assertEquals(ReaderThemeMode.WASTELAND_TERMINAL, wasteland.activeUiTheme)
        val cyberpunk = wasteland.copy(theme = ReaderThemeMode.CYBERPUNK_NEON)
        assertFalse(cyberpunk.useWastelandDesign)
        assertTrue(cyberpunk.useCyberpunkDesign)
        assertEquals(ReaderThemeMode.CYBERPUNK_NEON, cyberpunk.activeUiTheme)
    }

    @Test fun `removing the opt in instantly restores standard UI without changing selected palette`() {
        val selected = ReaderSettings(theme = ReaderThemeMode.WASTELAND_TERMINAL, experimentalDesignEnabled = true)
        val disabled = selected.copy(experimentalDesignEnabled = false)
        assertEquals(ReaderThemeMode.WASTELAND_TERMINAL, disabled.theme)
        assertFalse(disabled.useWastelandDesign)
        assertEquals(ReaderThemeMode.OLED, disabled.activeUiTheme)
    }

    @Test fun `ordinary palettes do not prevent explicitly enabled experimental shell`() {
        // Alpha.87+ separates ordinary palette, experimental palette and frame geometry.
        // With opt-in enabled and no explicit frame, Wasteland is the default frame.
        for (ordinaryTheme in listOf(ReaderThemeMode.PAPER, ReaderThemeMode.OLED, ReaderThemeMode.NORD)) {
            val settings = ReaderSettings(theme = ordinaryTheme, experimentalDesignEnabled = true)
            assertTrue(settings.useWastelandDesign)
            assertFalse(settings.useCyberpunkDesign)
            assertEquals(ReaderThemeMode.WASTELAND_TERMINAL, settings.activeUiTheme)
            assertEquals(ordinaryTheme, settings.theme) // Preserve ordinary palette.
        }
    }

    @Test fun `explicit experimental frame is independent of ordinary theme`() {
        val settings = ReaderSettings(
            theme = ReaderThemeMode.PAPER,
            experimentalDesignEnabled = true,
            experimentalFrameStyle = ReaderThemeMode.CYBERPUNK_NEON,
        )
        assertFalse(settings.useWastelandDesign)
        assertTrue(settings.useCyberpunkDesign)
        assertEquals(ReaderThemeMode.CYBERPUNK_NEON, settings.activeUiTheme)
        assertEquals(ReaderThemeMode.PAPER, settings.theme)
        val disabled = settings.copy(experimentalDesignEnabled = false)
        assertFalse(disabled.useCyberpunkDesign)
        assertFalse(disabled.useWastelandDesign)
        assertEquals(ReaderThemeMode.PAPER, disabled.activeUiTheme)
        assertEquals(ReaderThemeMode.CYBERPUNK_NEON, disabled.experimentalFrameStyle)
    }

    @Test fun `experimental palette does not enable or change frame geometry`() {
        val disabled = ReaderSettings(
            theme = ReaderThemeMode.PAPER,
            experimentalDesignEnabled = false,
            experimentalFrameStyle = ReaderThemeMode.CYBERPUNK_NEON,
            experimentalPalette = ReaderThemeMode.ROSE_PINE_MOON,
        )
        assertFalse(disabled.useWastelandDesign)
        assertFalse(disabled.useCyberpunkDesign)
        assertEquals(ReaderThemeMode.PAPER, disabled.activeUiTheme)
        val enabled = disabled.copy(experimentalDesignEnabled = true)
        assertTrue(enabled.useCyberpunkDesign)
        assertEquals(ReaderThemeMode.CYBERPUNK_NEON, enabled.activeUiTheme)
        val otherPalette = enabled.copy(experimentalPalette = ReaderThemeMode.CMX_PURPLE)
        assertTrue(otherPalette.useCyberpunkDesign)
        assertEquals(ReaderThemeMode.CYBERPUNK_NEON, otherPalette.activeUiTheme)
        assertEquals(ReaderThemeMode.CMX_PURPLE, otherPalette.experimentalPalette)
    }

    @Test fun `turning experimental mode off preserves both independent palette choices`() {
        val selected = ReaderSettings(
            theme = ReaderThemeMode.DRACULA,
            experimentalDesignEnabled = true,
            experimentalFrameStyle = ReaderThemeMode.WASTELAND_TERMINAL,
            experimentalPalette = ReaderThemeMode.NORD,
        )
        val disabled = selected.copy(experimentalDesignEnabled = false)
        assertEquals(ReaderThemeMode.DRACULA, disabled.activeUiTheme)
        assertEquals(ReaderThemeMode.NORD, disabled.experimentalPalette)
        assertEquals(ReaderThemeMode.WASTELAND_TERMINAL, disabled.experimentalFrameStyle)
        assertFalse(disabled.useWastelandDesign)
    }
}
