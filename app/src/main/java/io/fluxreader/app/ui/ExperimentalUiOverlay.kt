package io.fluxreader.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.fluxreader.app.R
import io.fluxreader.app.data.ReaderThemeMode
import io.fluxreader.app.data.ReaderSettings

/**
 * Static, non-interactive full-app skin layer. Textures and chrome are intentionally
 * animation-free and have no pointer input, so reader gestures and battery usage stay stable.
 */
@Composable
fun ExperimentalUiOverlay(theme: ReaderThemeMode, settings: ReaderSettings, immersive: Boolean = false) {
    val palette = LocalExperimentalColors.current
    when (theme) {
        ReaderThemeMode.WASTELAND_TERMINAL -> Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.wasteland_surface_texture),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                alpha = 0.07f,
            )
            ExperimentalScreenEffects(settings, palette.accent)
        }
        ReaderThemeMode.CYBERPUNK_NEON -> Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(R.drawable.cyberpunk_surface_texture),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                alpha = 0.07f,
            )
            if (!immersive) CyberpunkHudOverlay()
            ExperimentalScreenEffects(settings, palette.accent)
        }
        else -> Unit
    }
}

/**
 * Cosmetic effects only: never participates in measurement or gesture detection.
 * A single Canvas covers the already measured frame, without animations.
 */
@Composable
private fun ExperimentalScreenEffects(settings: ReaderSettings, phosphor: Color) {
    if (!settings.experimentalCrtEnabled) return
    val strength = settings.experimentalEffectIntensity.coerceIn(0f, 1f)
    if (strength <= 0f) return
    Canvas(Modifier.fillMaxSize()) {
        // Paint glass ON TOP of the routed screen. Drawing below routeContent would
        // hide scanlines behind solid green surfaces and make the toggles appear inert.
        val step = 5.dp.toPx().coerceAtLeast(4f)
        if (settings.experimentalGlowEnabled) {
            // Subtle phosphor wash. High enough to see even with the old 30% setting;
            // at 100% still small enough that text remains legible.
            drawRect(phosphor.copy(alpha = 0.085f * strength))
            drawRect(
                brush = Brush.verticalGradient(
                    0f to phosphor.copy(alpha = 0.075f * strength),
                    0.45f to Color.Transparent,
                    1f to phosphor.copy(alpha = 0.025f * strength),
                    startY = 0f,
                    endY = size.height,
                ),
            )
        }
        if (settings.experimentalScanlinesEnabled) {
            var y = 1f
            while (y < size.height) {
                // Alternating dark and bright lines read as CRT raster even over
                // almost-black green, unlike alpha.82's 0.075 * 0.30 bright-only line.
                drawLine(
                    Color.Black.copy(alpha = 0.35f * strength),
                    Offset(0f, y), Offset(size.width, y), 2.dp.toPx(),
                )
                drawLine(
                    phosphor.copy(alpha = 0.20f * strength),
                    Offset(0f, y + 2.dp.toPx()), Offset(size.width, y + 2.dp.toPx()),
                    0.8.dp.toPx(),
                )
                y += step
            }
        }
        if (settings.experimentalVignetteEnabled) {
            drawRect(
                brush = Brush.radialGradient(
                    0f to Color.Transparent,
                    0.52f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.66f * strength),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = maxOf(size.width, size.height) * 0.70f,
                ),
            )
        }
        if (settings.experimentalNoiseEnabled) {
            // Fixed, reproducible grain. No animations, allocation of bitmap
            // frames, randomization or CPU work outside the draw pass.
            val columns = 57
            val rows = 107
            for (yi in 0 until rows) {
                for (xi in 0 until columns) {
                    val hash = (xi * 73 + yi * 151 + xi * yi * 17) % 11
                    if (hash == 0) {
                        drawCircle(
                            phosphor.copy(alpha = 0.20f * strength),
                            radius = 0.8.dp.toPx(),
                            center = Offset(
                                (xi + 0.5f) * size.width / columns,
                                (yi + 0.5f) * size.height / rows,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WastelandCrtOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        // Dark oxidized edge plates.
        val plate = Color(0xFF2B1B0D).copy(alpha = 0.22f)
        drawRect(plate, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(size.width, 9f))
        drawRect(plate, topLeft = Offset(0f, size.height - 11f), size = androidx.compose.ui.geometry.Size(size.width, 11f))
        drawRect(plate, topLeft = Offset.Zero, size = androidx.compose.ui.geometry.Size(7f, size.height))
        drawRect(plate, topLeft = Offset(size.width - 7f, 0f), size = androidx.compose.ui.geometry.Size(7f, size.height))

        // CRT scanlines: visible enough to sell the theme but still readable.
        val scan = Color(0xFFC8E999).copy(alpha = 0.032f)
        var y = 1f
        while (y < size.height) {
            drawLine(scan, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += 4f
        }

        // Amber internal frame and old-terminal corner brackets.
        val amber = Color(0xFFFFB229).copy(alpha = 0.34f)
        val rust = Color(0xFFC76428).copy(alpha = 0.26f)
        val inset = 7f
        val arm = 34f
        val w = 1.8f
        drawRect(amber, topLeft = Offset(inset, inset), size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2), style = Stroke(w))
        fun corner(x: Float, y0: Float, sx: Float, sy: Float) {
            drawLine(amber, Offset(x, y0), Offset(x + arm * sx, y0), 2.5f)
            drawLine(amber, Offset(x, y0), Offset(x, y0 + arm * sy), 2.5f)
        }
        corner(inset, inset, 1f, 1f)
        corner(size.width - inset, inset, -1f, 1f)
        corner(inset, size.height - inset, 1f, -1f)
        corner(size.width - inset, size.height - inset, -1f, -1f)

        // Four bolt heads make the shell feel physical.
        val boltR = 4.4f
        listOf(
            Offset(15f, 15f), Offset(size.width - 15f, 15f),
            Offset(15f, size.height - 15f), Offset(size.width - 15f, size.height - 15f),
        ).forEach { c ->
            drawCircle(Color(0xFF17140D).copy(alpha = 0.86f), boltR + 1.6f, c)
            drawCircle(Color(0xFF9B7A3D).copy(alpha = 0.60f), boltR, c, style = Stroke(1.3f))
            drawLine(rust, Offset(c.x - 2.5f, c.y), Offset(c.x + 2.5f, c.y), 1.2f)
        }

        // Small faded caution ticks at the bottom shell.
        var x = 24f
        while (x < size.width - 40f) {
            drawLine(Color(0xFFFFB229).copy(alpha = 0.20f), Offset(x, size.height - 9f), Offset(x + 9f, size.height - 1f), 4f)
            x += 28f
        }
    }
}

@Composable
private fun CyberpunkHudOverlay() {
    val pal = LocalExperimentalColors.current
    Canvas(Modifier.fillMaxSize()) {
        val cyanGrid = pal.accent.copy(alpha = 0.034f)
        val magenta = pal.secondary.copy(alpha = 0.22f)
        val violet = pal.border.copy(alpha = 0.17f)
        val cell = 64f
        var x = 0f
        while (x <= size.width) {
            drawLine(cyanGrid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += cell
        }
        var y = 0f
        while (y <= size.height) {
            drawLine(cyanGrid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += cell
        }

        val inset = 8f
        val cut = 26f
        val frame = Path().apply {
            moveTo(inset + cut, inset)
            lineTo(size.width - inset - cut, inset)
            lineTo(size.width - inset, inset + cut)
            lineTo(size.width - inset, size.height - inset - cut)
            lineTo(size.width - inset - cut, size.height - inset)
            lineTo(inset + cut, size.height - inset)
            lineTo(inset, size.height - inset - cut)
            lineTo(inset, inset + cut)
            close()
        }
        drawPath(frame, cyanGrid.copy(alpha = 0.50f), style = Stroke(width = 4.5f))
        drawPath(frame, magenta, style = Stroke(width = 1.4f))

        // HUD corner rails and data nodes.
        drawLine(pal.accent.copy(alpha = 0.55f), Offset(24f, 22f), Offset(112f, 22f), 2.2f)
        drawLine(magenta, Offset(size.width - 112f, 22f), Offset(size.width - 24f, 22f), 2.2f)
        drawLine(violet, Offset(24f, size.height - 22f), Offset(130f, size.height - 22f), 2.0f)
        drawCircle(pal.accent.copy(alpha = 0.65f), 2.8f, Offset(118f, 22f))
        drawCircle(pal.secondary.copy(alpha = 0.65f), 2.8f, Offset(size.width - 118f, 22f))

        // Deliberately sparse glitch bars; static, not animated.
        val bars = listOf(
            Rect(18f, size.height * 0.23f, 58f, size.height * 0.23f + 2f),
            Rect(size.width - 72f, size.height * 0.51f, size.width - 20f, size.height * 0.51f + 2f),
            Rect(28f, size.height * 0.78f, 92f, size.height * 0.78f + 1.5f),
        )
        bars.forEachIndexed { index, rect -> drawRect(if (index == 1) magenta else violet, rect.topLeft, rect.size) }
    }
}
