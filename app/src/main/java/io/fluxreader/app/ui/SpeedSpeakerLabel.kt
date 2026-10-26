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
import io.fluxreader.app.reader.SpeakerColorEntry
import io.fluxreader.app.reader.SpeakerTint
import io.fluxreader.app.reader.speakerColorKey

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
    characterColor: SpeakerColorEntry? = null,
) {
    val themed = LocalFluxThemeMode.current.isExperimentalChrome
    val themePalette = LocalExperimentalColors.current
    val individual = speakerNameTint(speaker, namedSpeaker, settings.speedSpeakerColor, characterColor)
    val chosen = speedSpeakerColor(speaker, namedSpeaker, settings, background, accent, characterColor)
    val frameColor = if (themed && individual == null) themePalette.accent else chosen
    val fill = if (themed) frameColor.copy(alpha = settings.speedSpeakerBackgroundAlpha * .5f).compositeOver(themePalette.panel)
        else chosen.copy(alpha = settings.speedSpeakerBackgroundAlpha).compositeOver(background)
    val text = if (namedSpeaker && settings.speedDialogueTextColor) speedSpeakerReadingInk(speaker, settings, background, accent,
        characterColor, themePalette.panel.takeIf { themed })
        else speakerLabelTextColor(chosen, background, fill, settings.speedSpeakerStyle)
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

/** Shared by names and speech text, including per-book overrides and light/dark contrast. */
internal fun speedSpeakerColor(name: String, named: Boolean, settings: ReaderSettings,
    background: Color, accent: Color, entry: SpeakerColorEntry? = null): Color {
    val individual = speakerNameTint(name, named, settings.speedSpeakerColor, entry)
    val dark = background.luminance() < .35f
    return individual?.let { speakerTintColor(it, background) } ?: when (if (named) settings.speedSpeakerColor else SpeedSpeakerColor.THEME) {
        SpeedSpeakerColor.THEME, SpeedSpeakerColor.CHARACTER -> accent
        SpeedSpeakerColor.BLUE -> if (dark) Color(0xFF9CCAFF) else Color(0xFF1D5599)
        SpeedSpeakerColor.GREEN -> if (dark) Color(0xFFA8D9A0) else Color(0xFF31652C)
        SpeedSpeakerColor.AMBER -> if (dark) Color(0xFFFFC18A) else Color(0xFF8B4200)
        SpeedSpeakerColor.ROSE -> if (dark) Color(0xFFEBADD4) else Color(0xFF92396D)
        SpeedSpeakerColor.VIOLET -> if (dark) Color(0xFFB7A3FA) else Color(0xFF5935A2)
    }
}

/** A colored reply and its label share one ink, readable on both the page and label fill. */
internal fun speedSpeakerReadingInk(name: String, settings: ReaderSettings, background: Color, accent: Color,
    entry: SpeakerColorEntry? = null, themedPanel: Color? = null): Color {
    val chosen = speedSpeakerColor(name, true, settings, background, accent, entry)
    val individual = speakerNameTint(name, true, settings.speedSpeakerColor, entry)
    val frame = if (themedPanel != null && individual == null) accent else chosen
    val fill = if (settings.speedSpeakerStyle == SpeedSpeakerStyle.TEXT) background
        else if (themedPanel != null) frame.copy(alpha = settings.speedSpeakerBackgroundAlpha * .5f).compositeOver(themedPanel)
        else chosen.copy(alpha = settings.speedSpeakerBackgroundAlpha).compositeOver(background)
    fun contrast(color: Color, surface: Color) = (maxOf(color.luminance(), surface.luminance()) + .05f) /
        (minOf(color.luminance(), surface.luminance()) + .05f)
    fun minimumContrast(color: Color) = minOf(contrast(color, background), contrast(color, fill))
    return if (minimumContrast(chosen) >= 4.5f) chosen
        else if (minimumContrast(Color.Black) > minimumContrast(Color.White)) Color.Black else Color.White
}

internal fun speakerNameTint(name: String, namedSpeaker: Boolean, mode: SpeedSpeakerColor, entry: SpeakerColorEntry?): SpeakerTint? {
    if (!namedSpeaker) return null
    return entry?.custom ?: if (mode == SpeedSpeakerColor.CHARACTER) entry?.automatic
        ?: SpeakerTint.entries[(speakerColorKey(name).hashCode() and Int.MAX_VALUE) % SpeakerTint.entries.size] else null
}

internal fun speakerTintColor(tint: SpeakerTint, background: Color): Color {
    val dark = background.luminance() < .35f
    return Color(when (tint) {
        SpeakerTint.BLUE -> if (dark) 0xFF9CCAFF else 0xFF1D5599
        SpeakerTint.CYAN -> if (dark) 0xFF75D0D1 else 0xFF00686A
        SpeakerTint.GREEN -> if (dark) 0xFFA8D9A0 else 0xFF31652C
        SpeakerTint.LIME -> if (dark) 0xFFD0E58A else 0xFF486000
        SpeakerTint.AMBER -> if (dark) 0xFFFFC18A else 0xFF8B5A00
        SpeakerTint.ORANGE -> if (dark) 0xFFFFA486 else 0xFF9C3E14
        SpeakerTint.ROSE -> if (dark) 0xFFEBADD4 else 0xFF92396D
        SpeakerTint.VIOLET -> if (dark) 0xFFB7A3FA else 0xFF5935A2
    })
}

internal val SpeakerTint.label: String get() = when (this) {
    SpeakerTint.BLUE -> "Синий"; SpeakerTint.CYAN -> "Бирюзовый"; SpeakerTint.GREEN -> "Зелёный"; SpeakerTint.LIME -> "Лаймовый"
    SpeakerTint.AMBER -> "Янтарный"; SpeakerTint.ORANGE -> "Оранжевый"; SpeakerTint.ROSE -> "Розовый"; SpeakerTint.VIOLET -> "Фиолетовый"
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
