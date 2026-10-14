package io.fluxreader.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.fluxreader.app.R
import io.fluxreader.app.data.ReaderThemeMode

val LocalFluxThemeMode = staticCompositionLocalOf { ReaderThemeMode.OLED }

enum class FluxIconRole {
    LIBRARY,
    FOLDER,
    APPEARANCE,
    READING,
    SETTINGS,
    SEARCH,
    ADD,
    FILTER,
    BACK,
    BOOKMARK,
    SPEED,
    OCR,
    ABOUT,
    NOTES,
    TEXT,
    BRIGHTNESS,
    THEME_WASTELAND,
    THEME_CYBER
}

private fun wastelandIcon(role: FluxIconRole): Int = when (role) {
        FluxIconRole.FOLDER -> R.drawable.ic_wasteland_folder
        FluxIconRole.LIBRARY -> R.drawable.ic_wasteland_library
        FluxIconRole.APPEARANCE -> R.drawable.ic_wasteland_appearance
        FluxIconRole.READING -> R.drawable.ic_wasteland_reading
        FluxIconRole.SETTINGS -> R.drawable.ic_wasteland_settings
        FluxIconRole.SEARCH -> R.drawable.ic_wasteland_search
        FluxIconRole.ADD -> R.drawable.ic_wasteland_add
        FluxIconRole.FILTER -> R.drawable.ic_wasteland_filter
        FluxIconRole.BACK -> R.drawable.ic_wasteland_back
        FluxIconRole.BOOKMARK -> R.drawable.ic_wasteland_bookmark
        FluxIconRole.SPEED -> R.drawable.ic_wasteland_speed
        FluxIconRole.OCR -> R.drawable.ic_wasteland_ocr
        FluxIconRole.ABOUT -> R.drawable.ic_wasteland_about
        FluxIconRole.NOTES -> R.drawable.ic_wasteland_notes
        FluxIconRole.TEXT -> R.drawable.ic_wasteland_text
        FluxIconRole.BRIGHTNESS -> R.drawable.ic_wasteland_brightness
        FluxIconRole.THEME_WASTELAND -> R.drawable.ic_wasteland_theme_wasteland
        FluxIconRole.THEME_CYBER -> R.drawable.ic_wasteland_theme_cyber
}

private fun cyberIcon(role: FluxIconRole): Int = when (role) {
        FluxIconRole.FOLDER -> R.drawable.ic_cyber_folder
        FluxIconRole.LIBRARY -> R.drawable.ic_cyber_library
        FluxIconRole.APPEARANCE -> R.drawable.ic_cyber_appearance
        FluxIconRole.READING -> R.drawable.ic_cyber_reading
        FluxIconRole.SETTINGS -> R.drawable.ic_cyber_settings
        FluxIconRole.SEARCH -> R.drawable.ic_cyber_search
        FluxIconRole.ADD -> R.drawable.ic_cyber_add
        FluxIconRole.FILTER -> R.drawable.ic_cyber_filter
        FluxIconRole.BACK -> R.drawable.ic_cyber_back
        FluxIconRole.BOOKMARK -> R.drawable.ic_cyber_bookmark
        FluxIconRole.SPEED -> R.drawable.ic_cyber_speed
        FluxIconRole.OCR -> R.drawable.ic_cyber_ocr
        FluxIconRole.ABOUT -> R.drawable.ic_cyber_about
        FluxIconRole.NOTES -> R.drawable.ic_cyber_notes
        FluxIconRole.TEXT -> R.drawable.ic_cyber_text
        FluxIconRole.BRIGHTNESS -> R.drawable.ic_cyber_brightness
        FluxIconRole.THEME_WASTELAND -> R.drawable.ic_cyber_theme_wasteland
        FluxIconRole.THEME_CYBER -> R.drawable.ic_cyber_theme_cyber
}

@Composable
fun FluxThemedIcon(
    role: FluxIconRole,
    fallback: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalExperimentalColors.current
    val tint = if (colors.overridden) colors.accent else Color.Unspecified
    when (LocalFluxThemeMode.current) {
        ReaderThemeMode.WASTELAND_TERMINAL -> Icon(
            painter = painterResource(wastelandIcon(role)),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
        ReaderThemeMode.CYBERPUNK_NEON -> Icon(
            painter = painterResource(cyberIcon(role)),
            contentDescription = contentDescription,
            modifier = modifier,
            tint = tint,
        )
        else -> Icon(fallback, contentDescription = contentDescription, modifier = modifier)
    }
}

@Composable
fun ExperimentalThemeBadge(theme: ReaderThemeMode, modifier: Modifier = Modifier) {
    val icon = when (theme) {
        ReaderThemeMode.WASTELAND_TERMINAL -> R.drawable.ic_wasteland_theme_wasteland
        ReaderThemeMode.CYBERPUNK_NEON -> R.drawable.ic_cyber_theme_cyber
        else -> null
    }
    if (icon != null) {
        Icon(painterResource(icon), contentDescription = null, modifier = modifier, tint = Color.Unspecified)
    }
}

@Composable
fun ExperimentalThemePreview(theme: ReaderThemeMode) {
    val texture = when (theme) {
        ReaderThemeMode.WASTELAND_TERMINAL -> R.drawable.wasteland_surface_texture
        ReaderThemeMode.CYBERPUNK_NEON -> R.drawable.cyberpunk_surface_texture
        else -> return
    }
    val shape = MaterialTheme.shapes.medium
    Box(
        Modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.72f), shape),
    ) {
        Image(
            painter = painterResource(texture),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
            alpha = if (theme == ReaderThemeMode.WASTELAND_TERMINAL) 0.52f else 0.42f,
        )
        Row(
            Modifier.align(Alignment.CenterStart).padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ExperimentalThemeBadge(theme, Modifier.size(34.dp))
            Text(
                if (theme == ReaderThemeMode.WASTELAND_TERMINAL) "WASTELAND / CRT" else "CYBERPUNK / HUD",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            val roles = listOf(FluxIconRole.LIBRARY, FluxIconRole.READING, FluxIconRole.BOOKMARK, FluxIconRole.SETTINGS)
            roles.forEach { role ->
                Icon(
                    painter = painterResource(if (theme == ReaderThemeMode.WASTELAND_TERMINAL) wastelandIcon(role) else cyberIcon(role)),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = Color.Unspecified,
                )
            }
        }
    }
}
