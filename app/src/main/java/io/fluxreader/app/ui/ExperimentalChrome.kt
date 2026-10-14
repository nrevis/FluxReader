package io.fluxreader.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.fluxreader.app.R
import io.fluxreader.app.data.ReaderThemeMode

internal val CyberCyan = Color(0xFF37E7FF)
internal val CyberMagenta = Color(0xFFF35BFF)
internal val CyberInk = Color(0xFF070A19)
internal val CyberPanel = Color(0xFF10172B)
internal val CyberText = Color(0xFFE1F7FF)

/** This mode comes from activeUiTheme and can never be set without the opt-in checkbox. */
internal val ReaderThemeMode.isExperimentalChrome: Boolean
    get() = this == ReaderThemeMode.WASTELAND_TERMINAL || this == ReaderThemeMode.CYBERPUNK_NEON

internal val ReaderThemeMode.chromeAccent: Color
    get() = if (this == ReaderThemeMode.WASTELAND_TERMINAL) WastelandAmber else CyberCyan

internal val ReaderThemeMode.chromeText: Color
    get() = if (this == ReaderThemeMode.WASTELAND_TERMINAL) WastelandPhosphor else CyberText

/** No pointer input in this shell: texture and ornamental shapes cannot intercept gestures. */
@Composable
internal fun CyberpunkDeviceFrame(
    immersive: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val pal = LocalExperimentalColors.current
    // Immersive draws only the active display, not decorative lateral chassis rails.
    Box(Modifier.fillMaxSize().background(pal.background)) {
        // Decorations change but the child route keeps its composition identity.
        if (!immersive) {
        Image(
            painter = painterResource(R.drawable.cyberpunk_surface_texture),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.23f,
        )
        Canvas(Modifier.matchParentSize()) {
            val inset = 7.dp.toPx()
            drawRect(pal.accent.copy(alpha = 0.72f), Offset(inset, inset),
                Size(size.width - inset * 2, size.height - inset * 2), style = Stroke(1.dp.toPx()))
            drawLine(pal.secondary.copy(alpha = 0.75f), Offset(0f, 26.dp.toPx()),
                Offset(0f, 90.dp.toPx()), strokeWidth = 3.dp.toPx())
            drawLine(pal.accent.copy(alpha = 0.75f), Offset(size.width, size.height - 96.dp.toPx()),
                Offset(size.width, size.height - 20.dp.toPx()), strokeWidth = 3.dp.toPx())
        }
        } // Decorative rails/grid only.
        Box(
            Modifier.fillMaxSize()
                .padding(horizontal = if (immersive) 0.dp else 8.dp,
                    vertical = if (immersive) 0.dp else 8.dp)
                .then(if (ImmersiveLayoutPolicy.reserveSystemInsets(immersive)) Modifier.windowInsetsPadding(WindowInsets.safeDrawing) else Modifier),
            propagateMinConstraints = true,
            content = content,
        )
    }
}

@Composable
internal fun ExperimentalPanel(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    contentPadding: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val pal = LocalExperimentalColors.current
    when (LocalFluxThemeMode.current) {
        ReaderThemeMode.WASTELAND_TERMINAL -> WastelandPanel(modifier, accent, contentPadding, content)
        ReaderThemeMode.CYBERPUNK_NEON -> {
            val shape = CutCornerShape(topStart = 0.dp, topEnd = 10.dp, bottomEnd = 0.dp, bottomStart = 7.dp)
            Box(
                modifier.background(pal.panel, shape)
                    .border(if (accent) 2.dp else 1.dp, if (accent) pal.secondary else pal.accent.copy(alpha = 0.58f), shape)
                    .padding(contentPadding),
            ) {
                Image(
                    painter = painterResource(R.drawable.cyberpunk_surface_texture),
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                    contentScale = ContentScale.Crop,
                    alpha = 0.18f,
                )
                content()
            }
        }
        else -> Box(modifier.padding(contentPadding), content = content)
    }
}

@Composable
internal fun ExperimentalTopBar(
    title: String,
    subtitle: String? = null,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val pal = LocalExperimentalColors.current
    if (LocalFluxThemeMode.current == ReaderThemeMode.WASTELAND_TERMINAL) {
        WastelandTopBar(title, subtitle, navigation, actions)
        return
    }
    ExperimentalPanel(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp), accent = true, contentPadding = 10.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            navigation?.invoke()
            Column(Modifier.weight(1f)) {
                Text("// ${title.uppercase()}", color = pal.text, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (!subtitle.isNullOrBlank()) Text(subtitle.uppercase(), color = pal.accent,
                    style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            actions()
        }
    }
}

@Composable
internal fun ExperimentalTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val pal = LocalExperimentalColors.current
    if (LocalFluxThemeMode.current == ReaderThemeMode.WASTELAND_TERMINAL) {
        WastelandTab(label, selected, modifier, onClick)
        return
    }
    val shape = CutCornerShape(topStart = 0.dp, topEnd = 7.dp, bottomStart = 5.dp, bottomEnd = 0.dp)
    Box(
        modifier.background(if (selected) pal.panel else pal.panel, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) pal.secondary else pal.accent.copy(alpha = 0.44f), shape)
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label.uppercase(), color = if (selected) pal.accent else pal.text,
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun ExperimentalProgress(progress: Float, modifier: Modifier = Modifier) {
    val pal = LocalExperimentalColors.current
    if (LocalFluxThemeMode.current == ReaderThemeMode.WASTELAND_TERMINAL) {
        WastelandProgress(progress, modifier)
    } else {
        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = modifier.height(4.dp),
            color = pal.accent, trackColor = pal.panel)
    }
}

@Composable
internal fun ExperimentalCoverFrame(modifier: Modifier = Modifier, selected: Boolean = false, content: @Composable BoxScope.() -> Unit) {
    val pal = LocalExperimentalColors.current
    if (LocalFluxThemeMode.current == ReaderThemeMode.WASTELAND_TERMINAL) {
        WastelandCoverFrame(modifier, selected, content)
    } else {
        val shape = CutCornerShape(topEnd = 7.dp, bottomStart = 7.dp)
        Box(modifier.background(pal.panel, shape).border(if (selected) 2.dp else 1.dp,
            if (selected) pal.secondary else pal.accent.copy(alpha = 0.7f), shape).padding(3.dp), content = content)
    }
}

@Composable
internal fun ExperimentalHardwareRail(items: List<WastelandNavItem>, modifier: Modifier = Modifier) {
    val pal = LocalExperimentalColors.current
    if (LocalFluxThemeMode.current == ReaderThemeMode.WASTELAND_TERMINAL) {
        WastelandBottomRail(items, modifier)
        return
    }
    ExperimentalPanel(modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp), contentPadding = 5.dp) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            items.forEach { item ->
                val shape = CutCornerShape(topEnd = 8.dp, bottomStart = 6.dp)
                Column(
                    Modifier.weight(1f).background(if (item.selected) pal.panel else pal.background, shape)
                        .border(1.dp, if (item.selected) pal.secondary else pal.accent.copy(alpha = 0.65f), shape)
                        .clickable(enabled = item.enabled, onClick = item.onClick)
                        .padding(horizontal = 2.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    FluxThemedIcon(item.role, item.fallback, contentDescription = item.label, modifier = Modifier.size(22.dp))
                    Text(item.label.uppercase(), color = if (item.selected) pal.secondary else pal.accent,
                        style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun ExperimentalSectionHeader(title: String, role: FluxIconRole, fallback: ImageVector) {
    val pal = LocalExperimentalColors.current
    if (LocalFluxThemeMode.current == ReaderThemeMode.WASTELAND_TERMINAL) {
        WastelandSectionHeader(title, role = role, fallback = fallback)
    } else {
        ExperimentalPanel(Modifier.fillMaxWidth(), accent = true, contentPadding = 7.dp) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FluxThemedIcon(role, fallback, contentDescription = null, modifier = Modifier.size(20.dp))
                Text("// ${title.uppercase()}", color = pal.accent, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.weight(1f))
                Text("●", color = pal.secondary)
            }
        }
    }
}
