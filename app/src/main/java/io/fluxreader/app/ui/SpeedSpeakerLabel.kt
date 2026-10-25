package io.fluxreader.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.SpeedSpeakerColor
import io.fluxreader.app.data.SpeedSpeakerPosition
import io.fluxreader.app.data.SpeedSpeakerStyle

/** Used by the actual reader and its live settings preview. Names always keep natural case. */
@Composable
internal fun SpeedSpeakerLabel(
    speaker: String,
    settings: ReaderSettings,
    background: Color,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val light = listOf(Color(0xFFB7A3FA), Color(0xFF75D0D1), Color(0xFFFFC18A), Color(0xFFA8D9A0), Color(0xFFEBADD4))
    val dark = listOf(Color(0xFF5935A2), Color(0xFF00686A), Color(0xFF8B4200), Color(0xFF31652C), Color(0xFF92396D))
    val palette = if (background.luminance() < .35f) light else dark
    val chosen = when (settings.speedSpeakerColor) {
        SpeedSpeakerColor.CHARACTER -> palette[(speaker.lowercase().hashCode() and Int.MAX_VALUE) % palette.size]
        SpeedSpeakerColor.THEME -> accent
        SpeedSpeakerColor.BLUE -> if (background.luminance() < .35f) Color(0xFF9CCAFF) else Color(0xFF1D5599)
        SpeedSpeakerColor.GREEN -> palette[3]
        SpeedSpeakerColor.AMBER -> palette[2]
        SpeedSpeakerColor.ROSE -> palette[4]
        SpeedSpeakerColor.VIOLET -> palette[0]
    }
    val fill = chosen.copy(alpha = settings.speedSpeakerBackgroundAlpha).compositeOver(background)
    // A custom theme may supply an accent with poor contrast. Keep speaker text legible there too.
    val text = if ((maxOf(chosen.luminance(), fill.luminance()) + .05f) /
        (minOf(chosen.luminance(), fill.luminance()) + .05f) >= 4.5f) chosen
        else if (fill.luminance() > .179f) Color.Black else Color.White
    val bubble = settings.speedSpeakerStyle == SpeedSpeakerStyle.BUBBLE
    val above = settings.speedSpeakerPosition == SpeedSpeakerPosition.ABOVE
    val label: @Composable () -> Unit = {
        Text(speaker, color = text, style = speedSpeakerTextStyle(settings),
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
    val tail: @Composable () -> Unit = {
        Canvas(Modifier.size(16.dp, 8.dp)) {
            val path = Path().apply {
                if (above) { moveTo(0f, 0f); lineTo(size.width, 0f); lineTo(size.width / 2f, size.height) }
                else { moveTo(size.width / 2f, 0f); lineTo(size.width, size.height); lineTo(0f, size.height) }
                close()
            }
            drawPath(path, fill)
        }
    }
    Column(modifier.semantics(mergeDescendants = true) { contentDescription = "Говорит: $speaker" },
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (bubble && !above) tail()
        if (settings.speedSpeakerStyle == SpeedSpeakerStyle.TEXT) label()
        else Surface(color = fill, shape = RoundedCornerShape(if (bubble) 18.dp else 50.dp),
            border = BorderStroke(1.dp, chosen.copy(alpha = .55f))) { label() }
        if (bubble && above) tail()
    }
}

@Composable
internal fun speedSpeakerTextStyle(settings: ReaderSettings) = MaterialTheme.typography.bodyLarge.copy(
    fontSize = settings.speedSpeakerFontSizeSp.sp,
    lineHeight = (settings.speedSpeakerFontSizeSp * 1.25f).sp,
    fontWeight = if (settings.speedSpeakerBold) FontWeight.Bold else FontWeight.Normal,
)
