package io.fluxreader.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.semantics.Role
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
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.fluxreader.app.data.ReaderThemeMode
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
    namedSpeaker: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val themed = LocalFluxThemeMode.current.isExperimentalChrome
    val themePalette = LocalExperimentalColors.current
    val light = listOf(Color(0xFFB7A3FA), Color(0xFF75D0D1), Color(0xFFFFC18A), Color(0xFFA8D9A0), Color(0xFFEBADD4))
    val dark = listOf(Color(0xFF5935A2), Color(0xFF00686A), Color(0xFF8B4200), Color(0xFF31652C), Color(0xFF92396D))
    val palette = if (background.luminance() < .35f) light else dark
    val chosen = when (if (namedSpeaker) settings.speedSpeakerColor else SpeedSpeakerColor.THEME) {
        SpeedSpeakerColor.CHARACTER -> {
            val characterPalette = if (themed) listOf(themePalette.text, themePalette.accent, themePalette.secondary) else palette
            characterPalette[(speaker.lowercase().hashCode() and Int.MAX_VALUE) % characterPalette.size]
        }
        SpeedSpeakerColor.THEME -> accent
        SpeedSpeakerColor.BLUE -> if (background.luminance() < .35f) Color(0xFF9CCAFF) else Color(0xFF1D5599)
        SpeedSpeakerColor.GREEN -> palette[3]
        SpeedSpeakerColor.AMBER -> palette[2]
        SpeedSpeakerColor.ROSE -> palette[4]
        SpeedSpeakerColor.VIOLET -> palette[0]
    }
    val frameColor = if (themed) themePalette.accent else chosen
    val fill = if (themed) frameColor.copy(alpha = settings.speedSpeakerBackgroundAlpha * .5f).compositeOver(themePalette.panel)
        else chosen.copy(alpha = settings.speedSpeakerBackgroundAlpha).compositeOver(background)
    val text = speakerLabelTextColor(chosen, background, fill, settings.speedSpeakerStyle)
    val bubble = settings.speedSpeakerStyle == SpeedSpeakerStyle.BUBBLE
    val above = settings.speedSpeakerPosition == SpeedSpeakerPosition.ABOVE
    val bodyShape = if (themed) MaterialTheme.shapes.large else RoundedCornerShape(if (bubble) 18.dp else 50.dp)
    val shape = if (bubble) SpeakerBubbleShape(bodyShape, above) else bodyShape
    val label: @Composable () -> Unit = {
        Box {
            Text(speaker, color = text, style = speedSpeakerTextStyle(settings),
                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 16.dp, end = if (onClick != null) 34.dp else 16.dp,
                    top = 8.dp, bottom = 8.dp))
            if (onClick != null) Icon(Icons.Default.Edit, contentDescription = null, tint = text.copy(alpha = .6f),
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp).size(14.dp))
        }
    }
    Column(modifier.then(if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick))
        .semantics(mergeDescendants = true) { contentDescription = if (namedSpeaker) "Говорит: $speaker" else speaker },
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (settings.speedSpeakerStyle == SpeedSpeakerStyle.TEXT) label()
        else Surface(color = fill, shape = shape, border = BorderStroke(1.dp, frameColor.copy(alpha = .65f))) {
            Column(Modifier.padding(top = if (bubble && !above) 8.dp else 0.dp,
                bottom = if (bubble && above) 8.dp else 0.dp)) { label() }
        }
    }
}

@Composable
internal fun speedSpeakerTextStyle(settings: ReaderSettings) = MaterialTheme.typography.bodyLarge.copy(
    fontSize = settings.speedSpeakerFontSizeSp.sp,
    lineHeight = (settings.speedSpeakerFontSizeSp * 1.25f).sp,
    fontWeight = if (settings.speedSpeakerBold) FontWeight.Bold else FontWeight.Normal,
)

/** A single outline keeps the tail, fill and border joined in every skin and position. */
private class SpeakerBubbleShape(private val body: Shape, private val above: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val tailHeight = with(density) { 8.dp.toPx() }.coerceAtMost(size.height / 3f)
        val halfTail = with(density) { 7.dp.toPx() }.coerceAtMost(size.width / 4f)
        val bodyHeight = (size.height - tailHeight).coerceAtLeast(0f)
        val bodyPath = Path().apply {
            when (val outline = body.createOutline(Size(size.width, bodyHeight), layoutDirection, density)) {
                is Outline.Rectangle -> addRect(outline.rect)
                is Outline.Rounded -> addRoundRect(outline.roundRect)
                is Outline.Generic -> addPath(outline.path)
            }
            if (!above) translate(Offset(0f, tailHeight))
        }
        val tail = Path().apply {
            val center = size.width / 2f
            val base = if (above) bodyHeight - 1f else tailHeight + 1f
            moveTo(center - halfTail, base)
            lineTo(center + halfTail, base)
            lineTo(center, if (above) size.height else 0f)
            close()
        }
        return Outline.Generic(Path.combine(PathOperation.Union, bodyPath, tail))
    }
}

internal fun speakerLabelTextColor(chosen: Color, background: Color, fill: Color, style: SpeedSpeakerStyle): Color {
    val actualBackground = if (style == SpeedSpeakerStyle.TEXT) background else fill
    return if ((maxOf(chosen.luminance(), actualBackground.luminance()) + .05f) /
        (minOf(chosen.luminance(), actualBackground.luminance()) + .05f) >= 4.5f) chosen
        else if (actualBackground.luminance() > .179f) Color.Black else Color.White
}
