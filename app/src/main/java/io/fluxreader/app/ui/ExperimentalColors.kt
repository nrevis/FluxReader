package io.fluxreader.app.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.activeUiTheme
import io.fluxreader.app.data.ReaderThemeMode

/** The hardware frame (Wasteland/Cyberpunk) is independent of the color palette. */
internal data class ExperimentalColors(
    val background: Color,
    val panel: Color,
    val text: Color,
    val accent: Color,
    val border: Color,
    val secondary: Color,
    val selectedText: Color,
    val overridden: Boolean = false,
)

internal val LocalExperimentalColors = staticCompositionLocalOf {
    ExperimentalColors(
        background = Color(0xFF071107), panel = Color(0xFF0D190D),
        text = Color(0xFFB8DB7B), accent = Color(0xFFFFB229),
        border = Color(0xFF7A6537), secondary = Color(0xFFA84D20),
        selectedText = Color(0xFF241500),
    )
}

internal fun experimentalColors(settings: ReaderSettings, scheme: ColorScheme): ExperimentalColors {
    val frame = settings.activeUiTheme
    if (settings.experimentalPalette == null) return when (frame) {
        ReaderThemeMode.CYBERPUNK_NEON -> ExperimentalColors(
            background = Color(0xFF070A19), panel = Color(0xFF10172B),
            text = Color(0xFFE1F7FF), accent = Color(0xFF37E7FF),
            border = Color(0xFF37E7FF).copy(alpha = 0.7f), secondary = Color(0xFFF35BFF),
            selectedText = Color(0xFF070A19),
        )
        else -> LocalExperimentalColorsDefault
    }
    val primary = scheme.primary
    // The selected-chip label must stay legible on bright yellow/green AND on dark accents.
    val selectedText = if (primary.luminance() >= 0.40f) Color(0xFF10131A) else Color.White
    return ExperimentalColors(
        background = scheme.background,
        panel = scheme.surface,
        text = scheme.onSurface,
        accent = primary,
        border = scheme.secondary,
        secondary = scheme.secondary,
        selectedText = selectedText,
        overridden = true,
    )
}

private val LocalExperimentalColorsDefault = ExperimentalColors(
    background = Color(0xFF071107), panel = Color(0xFF0D190D),
    text = Color(0xFFB8DB7B), accent = Color(0xFFFFB229),
    border = Color(0xFF7A6537), secondary = Color(0xFFA84D20),
    selectedText = Color(0xFF241500),
)
