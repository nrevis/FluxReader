package io.fluxreader.app.ui

import android.os.Build
import android.graphics.RectF

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/** Cosmetic only. The real system insets still own all content measurement and touch targets. */
@Composable
internal fun DeviceStatusCrown(immersive: Boolean, experimental: Boolean, backgroundOverride: Color? = null) {
    val decorateCamera = Build.MANUFACTURER.equals("Google", ignoreCase = true) &&
        (Build.MODEL.equals("Pixel", ignoreCase = true) || Build.MODEL.startsWith("Pixel ", ignoreCase = true))
    val density = LocalDensity.current
    val safeTop = WindowInsets.safeDrawing.getTop(density).toFloat()
    val safeBottom = WindowInsets.safeDrawing.getBottom(density).toFloat()
    // Reading Compose's cutout inset also invalidates this layer after rotation/overlay changes.
    val cutoutTop = WindowInsets.displayCutout.getTop(density)
    val cutout = LocalView.current.rootWindowInsets?.displayCutout
    val cutouts = cutout?.boundingRects.orEmpty().map(::RectF)
    // boundingRects includes the unsafe lane all the way to the screen edge for a hole-punch.
    // The physical path gives the actual hole, avoiding a large fake notch around a small lens.
    val physicalPath = if (Build.VERSION.SDK_INT >= 31) cutout?.cutoutPath else null
    val physicalBounds = physicalPath?.let { path -> RectF().also { path.computeBounds(it, true) } }
        ?.takeUnless { it.isEmpty }
    val cameraShapes = if (cutouts.size == 1 && physicalBounds != null) listOf(physicalBounds) else cutouts
    val palette = LocalExperimentalColors.current
    val background = if (experimental) palette.background else backgroundOverride ?: MaterialTheme.colorScheme.background
    val accent = if (experimental) palette.accent else MaterialTheme.colorScheme.primary
    Canvas(Modifier.fillMaxSize()) {
        if (!immersive && safeTop > 0f) {
            drawRect(background, size = Size(size.width, safeTop))
            // A quiet divider unifies the system row with the reader header.
            drawLine(accent.copy(alpha = .14f), Offset(24.dp.toPx(), safeTop),
                Offset(size.width - 24.dp.toPx(), safeTop), 1.dp.toPx())
        }
        if (!immersive && safeBottom > 0f) drawRect(background,
            topLeft = Offset(0f, size.height - safeBottom), size = Size(size.width, safeBottom))
        if (decorateCamera && (cutoutTop > 0 || cutouts.isNotEmpty())) for (rect in cameraShapes) {
            if (rect.top > 0 && rect.top > safeTop || rect.bottom > size.height / 3f) continue
            val gap = 4.dp.toPx()
            val left = (rect.left - gap).coerceAtLeast(0f)
            val top = (rect.top - gap).coerceAtLeast(0f)
            val right = (rect.right + gap).coerceAtMost(size.width)
            val bottom = rect.bottom + gap
            if (right <= left || bottom <= top) continue
            val radius = CornerRadius(minOf(right - left, bottom - top) / 2f)
            drawRoundRect(Color.Black, Offset(left, top), Size(right - left, bottom - top), radius)
            drawRoundRect(accent.copy(alpha = .42f), Offset(left, top), Size(right - left, bottom - top),
                radius, style = Stroke(1.dp.toPx()))
        }
    }
}

@Composable
internal fun DeviceFrameRails() {
    val density = LocalDensity.current
    val insets = WindowInsets.safeDrawing
    val topInset = insets.getTop(density).toFloat()
    val bottomInset = insets.getBottom(density).toFloat()
    val pal = LocalExperimentalColors.current
    Canvas(Modifier.fillMaxSize()) {
        val edge = 5.dp.toPx()
        val top = topInset + 3.dp.toPx()
        val bottom = size.height - bottomInset - 3.dp.toPx()
        if (bottom <= top) return@Canvas
        drawRoundRect(pal.accent.copy(alpha = .24f), Offset(edge, top),
            Size(size.width - edge * 2, bottom - top), CornerRadius(20.dp.toPx()), style = Stroke(1.dp.toPx()))
        // Side ticks remain inside the safe screen, away from camera and rounded device corners.
        val length = 24.dp.toPx()
        drawLine(pal.accent.copy(alpha = .64f), Offset(edge, top + 24.dp.toPx()),
            Offset(edge, top + 24.dp.toPx() + length), 2.dp.toPx())
        drawLine(pal.secondary.copy(alpha = .64f), Offset(size.width - edge, bottom - 24.dp.toPx() - length),
            Offset(size.width - edge, bottom - 24.dp.toPx()), 2.dp.toPx())
    }
}
