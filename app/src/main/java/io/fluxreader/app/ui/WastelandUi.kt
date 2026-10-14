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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.fluxreader.app.R

internal val WastelandAmber = Color(0xFFFFB229)
internal val WastelandPhosphor = Color(0xFFB8DB7B)
internal val WastelandDarkGreen = Color(0xFF071107)
internal val WastelandPanelGreen = Color(0xFF0D190D)
internal val WastelandMetal = Color(0xFF3A2918)
internal val WastelandRust = Color(0xFFA84D20)
internal val WastelandOutline = Color(0xFF7A6537)

/**
 * Full-screen experimental shell for the Wasteland theme.
 * It intentionally reserves a small physical bezel around the real UI rather than merely
 * recolouring Material components. The layer is static and has no pointer input.
 */
@Composable
internal fun WastelandDeviceFrame(
    modifier: Modifier = Modifier,
    immersive: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val pal = LocalExperimentalColors.current
    // Immersive means edge-to-edge glass: NO side metal, screws, hazard stripes or bezel.
    // The Reader/Speed Reader itself handles the actual camera cutout.
    Box(
        modifier
            .fillMaxSize()
            .background(if (immersive) pal.background else Color(0xFF171007)),
    ) {
        // Keep the routed Reader/SpeedReader in this SAME slot when immersive
        // toggles; a separate fullscreen branch destroyed its rememberSaveable state.
        if (!immersive) {
        Image(
            painter = painterResource(R.drawable.wasteland_surface_texture),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.36f,
        )
        Canvas(Modifier.fillMaxSize()) {
            val px = density
            val bezel = 10f * px
            val inner = 13f * px
            val screw = 3.4f * px
            val amber = pal.accent.copy(alpha = 0.52f)
            val dark = Color(0xFF0A0805)
            val metal = WastelandMetal.copy(alpha = 0.86f)
            val rust = WastelandRust.copy(alpha = 0.55f)

            // Thick physical shell.
            drawRect(metal, Offset.Zero, Size(size.width, bezel))
            drawRect(metal, Offset(0f, size.height - bezel), Size(size.width, bezel))
            drawRect(metal, Offset.Zero, Size(bezel, size.height))
            drawRect(metal, Offset(size.width - bezel, 0f), Size(bezel, size.height))
            drawRect(
                color = Color(0xFF11150B),
                topLeft = Offset(inner, inner),
                size = Size(size.width - inner * 2f, size.height - inner * 2f),
                style = Stroke(width = 1.6f * px),
            )
            drawRect(
                color = amber,
                topLeft = Offset(inner + 1.5f * px, inner + 1.5f * px),
                size = Size(size.width - (inner + 1.5f * px) * 2f, size.height - (inner + 1.5f * px) * 2f),
                style = Stroke(width = 0.9f * px),
            )

            // Top/bottom vent slots, matching the reference's industrial terminal enclosure.
            val ventYTop = 2.7f * px
            val ventYBottom = size.height - 6.2f * px
            val ventStart = size.width * 0.35f
            val ventEnd = size.width * 0.65f
            val ventGap = 4.4f * px
            var x = ventStart
            while (x <= ventEnd) {
                drawLine(dark, Offset(x, ventYTop), Offset(x, ventYTop + 4.2f * px), strokeWidth = 2.1f * px)
                drawLine(dark, Offset(x, ventYBottom), Offset(x, ventYBottom + 3.5f * px), strokeWidth = 2.1f * px)
                x += ventGap
            }

            // Four screws with a rusted slot.
            listOf(
                Offset(5.1f * px, 5.1f * px),
                Offset(size.width - 5.1f * px, 5.1f * px),
                Offset(5.1f * px, size.height - 5.1f * px),
                Offset(size.width - 5.1f * px, size.height - 5.1f * px),
            ).forEach { c ->
                drawCircle(dark, screw + 1.1f * px, c)
                drawCircle(Color(0xFF8B7448), screw, c, style = Stroke(0.9f * px))
                drawLine(rust, Offset(c.x - 1.8f * px, c.y), Offset(c.x + 1.8f * px, c.y), 0.9f * px)
            }

            // Amber power lamp centered at the bottom.
            val lampW = 27f * px
            val lampH = 3.7f * px
            val lampX = (size.width - lampW) / 2f
            val lampY = size.height - 7.1f * px
            drawRoundRect(Color(0xFF1D1204), Offset(lampX - 2f * px, lampY - 1.2f * px), Size(lampW + 4f * px, lampH + 2.4f * px), CornerRadius(1.3f * px, 1.3f * px))
            drawRoundRect(pal.accent.copy(alpha = 0.90f), Offset(lampX, lampY), Size(lampW, lampH), CornerRadius(1.1f * px, 1.1f * px))

            // Sparse hazard stripes on shell corners.
            val hazard = pal.accent.copy(alpha = 0.55f)
            val stripe = 9f * px
            repeat(4) { i ->
                val y = 18f * px + i * stripe
                drawLine(hazard, Offset(0f, y), Offset(bezel, y + 6f * px), 2.5f * px)
                drawLine(hazard, Offset(size.width - bezel, size.height - y), Offset(size.width, size.height - y - 6f * px), 2.5f * px)
            }
        }

        } // Decorative chassis only; do not reconstruct the routed subtree.

        // The active CRT surface is inset from the metal shell.
        Box(
            modifier = Modifier
                .fillMaxSize()
                // In ordinary mode the OS owns the status/navigation zones. In
                // immersive mode those bars vanish: do NOT reserve their insets in
                // the decorative shell. The individual reader guards only the
                // real center camera cutout using its own minimal content padding.
                .padding(horizontal = if (immersive) 0.dp else 10.dp,
                    vertical = if (immersive) 0.dp else 10.dp)
                .then(if (ImmersiveLayoutPolicy.reserveSystemInsets(immersive)) Modifier.windowInsetsPadding(WindowInsets.safeDrawing) else Modifier)
                .background(pal.background),
            // Critical for route roots such as Scaffold: Box normally measures children with
            // loose minimum constraints. That allowed the alpha.59 Scaffold to collapse to the
            // height of its bars, placing the bottom rail near the top and leaving no viewport
            // for the library. Propagating the full minimum constraints makes every routed
            // screen occupy the complete CRT viewport.
            propagateMinConstraints = true,
            content = content,
        )
    }
}

@Composable
internal fun WastelandPanel(
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    contentPadding: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val pal = LocalExperimentalColors.current
    val shape = CutCornerShape(topStart = 2.dp, topEnd = 5.dp, bottomEnd = 2.dp, bottomStart = 5.dp)
    Box(
        modifier
            .background(if (accent) pal.panel else pal.panel.copy(alpha = 0.95f), shape)
            .border(1.dp, if (accent) pal.accent.copy(alpha = 0.90f) else pal.border.copy(alpha = 0.85f), shape)
            .padding(contentPadding),
    ) {
        // Decoration must never participate in measuring this wrap-content panel.
        // fillMaxSize() here made every WastelandPanel request the parent's full height,
        // stretching top bars/cards and pushing the real library viewport off-screen.
        Image(
            painter = painterResource(R.drawable.wasteland_surface_texture),
            contentDescription = null,
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Crop,
            alpha = if (accent) 0.10f else 0.055f,
        )
        content()
    }
}

@Composable
internal fun WastelandTopBar(
    title: String,
    subtitle: String? = null,
    navigation: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val pal = LocalExperimentalColors.current
    WastelandPanel(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
        accent = false,
        contentPadding = 8.dp,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            navigation?.invoke()
            Column(Modifier.weight(1f)) {
                Text(
                    title.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = pal.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = pal.accent.copy(alpha = 0.92f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), content = actions)
        }
    }
}

@Composable
internal fun WastelandSectionHeader(
    title: String,
    role: FluxIconRole? = null,
    fallback: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val pal = LocalExperimentalColors.current
    Row(
        modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2112).copy(alpha = 0.86f), CutCornerShape(topEnd = 6.dp, bottomStart = 6.dp))
            .border(1.dp, pal.border.copy(alpha = 0.75f), CutCornerShape(topEnd = 6.dp, bottomStart = 6.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (role != null && fallback != null) {
            FluxThemedIcon(role, fallback, contentDescription = null, modifier = Modifier.size(20.dp))
        }
        Text(
            title.uppercase(),
            color = pal.accent,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.weight(1f))
        Canvas(Modifier.width(42.dp).height(8.dp)) {
            val c = pal.accent.copy(alpha = 0.55f)
            var x = 0f
            while (x < size.width) {
                drawLine(c, Offset(x, size.height), Offset(x + 6f, 0f), 2f)
                x += 9f
            }
        }
    }
}

@Composable
internal fun WastelandHardwareButton(
    label: String,
    role: FluxIconRole,
    fallback: ImageVector,
    selected: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val pal = LocalExperimentalColors.current
    val shape = CutCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomEnd = 5.dp, bottomStart = 5.dp)
    val border = if (selected) pal.accent else pal.border
    Box(
        modifier
            .background(if (selected) Color(0xFF3C2609) else Color(0xFF17150D), shape)
            .border(if (selected) 2.dp else 1.dp, border.copy(alpha = if (enabled) 0.94f else 0.35f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            FluxThemedIcon(role, fallback, contentDescription = label, modifier = Modifier.size(22.dp))
            Text(
                label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = (if (selected) pal.accent else pal.text).copy(alpha = if (enabled) 1f else 0.35f),
                maxLines = 1,
            )
        }
    }
}

@Composable
internal fun WastelandBottomRail(
    items: List<WastelandNavItem>,
    modifier: Modifier = Modifier,
) {
    val pal = LocalExperimentalColors.current
    WastelandPanel(
        modifier = modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp),
        contentPadding = 5.dp,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            items.forEach { item ->
                WastelandHardwareButton(
                    label = item.label,
                    role = item.role,
                    fallback = item.fallback,
                    selected = item.selected,
                    enabled = item.enabled,
                    modifier = Modifier.weight(1f),
                    onClick = item.onClick,
                )
            }
        }
    }
}

internal data class WastelandNavItem(
    val label: String,
    val role: FluxIconRole,
    val fallback: ImageVector,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

@Composable
internal fun WastelandTab(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val pal = LocalExperimentalColors.current
    val shape = CutCornerShape(topStart = 2.dp, topEnd = 3.dp, bottomEnd = 2.dp, bottomStart = 3.dp)
    Box(
        modifier
            .background(if (selected) pal.accent else pal.panel, shape)
            .border(1.dp, if (selected) pal.accent else pal.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) pal.selectedText else pal.text,
        )
    }
}

@Composable
internal fun WastelandProgress(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val pal = LocalExperimentalColors.current
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier.height(4.dp),
        color = pal.accent,
        trackColor = pal.panel,
    )
}

@Composable
internal fun WastelandCoverFrame(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val pal = LocalExperimentalColors.current
    val shape = CutCornerShape(2.dp)
    Box(
        modifier
            .background(pal.panel, shape)
            .border(if (selected) 2.dp else 1.dp, if (selected) pal.accent else pal.border, shape)
            .padding(3.dp),
        content = content,
    )
}

@Composable
internal fun WastelandCrtGlass(modifier: Modifier = Modifier) {
    val pal = LocalExperimentalColors.current
    Canvas(modifier.fillMaxSize()) {
        val scan = pal.text.copy(alpha = 0.033f)
        var y = 0f
        while (y < size.height) {
            drawLine(scan, Offset(0f, y), Offset(size.width, y), 1f)
            y += 4f
        }
        // Subtle vignette-like edge rails without a shader/animation.
        drawRect(Color.Black.copy(alpha = 0.10f), Offset.Zero, Size(5f, size.height))
        drawRect(Color.Black.copy(alpha = 0.10f), Offset(size.width - 5f, 0f), Size(5f, size.height))
    }
}
