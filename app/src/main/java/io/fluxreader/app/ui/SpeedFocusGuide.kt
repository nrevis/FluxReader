package io.fluxreader.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.SpeedGuideStyle
import io.fluxreader.app.data.useWastelandDesign
import io.fluxreader.app.data.useCyberpunkDesign
import kotlin.math.min

/** Positions are adjacent to the reading line, inside the gap before names and context. */
internal data class SpeedGuideBounds(val top: Float, val bottom: Float, val upperLength: Float, val lowerLength: Float)

internal object SpeedGuideGeometry {
    fun bounds(height: Float, center: Float, halfText: Float, gap: Float,
        desiredLength: Float, upperRoom: Float, lowerRoom: Float): SpeedGuideBounds {
        val safeHeight = height.coerceAtLeast(0f)
        val top = (center - halfText.coerceAtLeast(0f) - gap).coerceIn(0f, safeHeight)
        val bottom = (center + halfText.coerceAtLeast(0f) + gap).coerceIn(0f, safeHeight)
        return SpeedGuideBounds(top, bottom,
            min(desiredLength, min(top, (upperRoom - gap * 2).coerceAtLeast(0f))),
            min(desiredLength, min(safeHeight - bottom, (lowerRoom - gap * 2).coerceAtLeast(0f))))
    }
}

@Composable
internal fun SpeedFocusGuide(
    settings: ReaderSettings,
    guideColor: Color,
    focusPosition: Float,
    horizontalFocusPosition: Float,
    mainHalfHeightPx: Float,
    upperRoomDp: Float = 160f,
    lowerRoomDp: Float = 160f,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val x = size.width * horizontalFocusPosition.coerceIn(0f, 1f)
        val bounds = SpeedGuideGeometry.bounds(size.height, size.height * focusPosition, mainHalfHeightPx,
            4.dp.toPx(), min(size.height * settings.speedGuideHeightFraction / 2f, 90.dp.toPx()),
            upperRoomDp.dp.toPx(), lowerRoomDp.dp.toPx())
        val stroke = settings.speedGuideWidthDp.dp.toPx()
        val tick = 9.dp.toPx()
        fun line(x1: Float, y1: Float, x2: Float, y2: Float) = drawLine(guideColor,
            Offset(x1, y1), Offset(x2, y2), strokeWidth = stroke, cap = StrokeCap.Round)
        when (settings.speedGuideStyle) {
            SpeedGuideStyle.TICKS -> {
                line(x - tick, bounds.top, x + tick, bounds.top)
                line(x - tick, bounds.bottom, x + tick, bounds.bottom)
            }
            SpeedGuideStyle.RAIL -> {
                if (bounds.upperLength > 0f) line(x, bounds.top - bounds.upperLength, x, bounds.top)
                if (bounds.lowerLength > 0f) line(x, bounds.bottom, x, bounds.bottom + bounds.lowerLength)
                if (settings.speedShowFocusTicks) {
                    line(x - tick, bounds.top, x + tick, bounds.top)
                    line(x - tick, bounds.bottom, x + tick, bounds.bottom)
                }
            }
            SpeedGuideStyle.BRACKETS -> {
                val width = 13.dp.toPx()
                val arm = 5.dp.toPx()
                for (side in listOf(-1, 1)) {
                    line(x + side * width, bounds.top, x + side * (width - arm), bounds.top)
                    line(x + side * width, bounds.top, x + side * width, bounds.top - min(arm, bounds.upperLength))
                    line(x + side * width, bounds.bottom, x + side * (width - arm), bounds.bottom)
                    line(x + side * width, bounds.bottom, x + side * width, bounds.bottom + min(arm, bounds.lowerLength))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpeedGuideChoices(settings: ReaderSettings, onUpdate: ((ReaderSettings) -> ReaderSettings) -> Unit) {
    val pal = LocalExperimentalColors.current
    val themed = settings.useWastelandDesign || settings.useCyberpunkDesign
    val background = if (themed) pal.background else if (settings.speedUseReaderTheme) MaterialTheme.colorScheme.background else Color(settings.speedBackgroundColorArgb)
    val text = if (themed) pal.text else if (settings.speedUseReaderTheme) MaterialTheme.colorScheme.onBackground else Color(settings.speedTextColorArgb)
    val color = when {
        settings.useWastelandDesign -> pal.accent.copy(alpha = .70f)
        settings.useCyberpunkDesign -> pal.secondary.copy(alpha = .72f)
        settings.speedUseReaderTheme -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .42f)
        else -> Color(settings.speedGuideColorArgb)
    }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SpeedGuideStyle.entries.forEach { style ->
            FilterChip(selected = settings.speedGuideStyle == style,
                onClick = { onUpdate { it.copy(speedGuideStyle = style) } },
                label = { Text(when (style) {
                    SpeedGuideStyle.TICKS -> "Метки"
                    SpeedGuideStyle.RAIL -> "Линия"
                    SpeedGuideStyle.BRACKETS -> "Скобки"
                }) })
        }
    }
    Box(Modifier.fillMaxWidth().height(128.dp).background(background, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
        SpeedFocusGuide(settings, color, .5f, .5f, with(androidx.compose.ui.platform.LocalDensity.current) { 16.dp.toPx() },
            modifier = Modifier.fillMaxSize())
        Text("Фокус", color = text, style = MaterialTheme.typography.titleLarge)
    }
}
