package io.fluxreader.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Shared by global preferences and reader sheets, with a single accessible switch target. */
@Composable
internal fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val body: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 56.dp)
                .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = null)
        }
    }
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) { body() }
    } else body()
}

/** UI follows the draft immediately; persistence happens after the gesture finishes. */
@Composable
internal fun SettingsValueSlider(
    title: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    format: (Float) -> String,
    onCommit: (Float) -> Unit,
) {
    var draft by remember(value) { mutableFloatStateOf(value) }
    val body: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface)
                Text(format(draft), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.End, maxLines = 1, softWrap = false)
            }
            SettingsSlider(value = draft.coerceIn(range.start, range.endInclusive),
                onValueChange = { draft = it }, onValueChangeFinished = { onCommit(draft) }, valueRange = range)
        }
    }
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 12.dp) { body() }
    } else body()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    steps: Int = 0,
    colors: SliderColors = SliderDefaults.colors(),
) {
    Slider(value = value, onValueChange = onValueChange, onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled, steps = steps, colors = colors,
        thumb = { Box(Modifier.size(20.dp).background(if (enabled) colors.thumbColor else MaterialTheme.colorScheme.onSurface.copy(alpha = .38f), CircleShape)) },
        track = { state -> SliderDefaults.Track(sliderState = state, modifier = Modifier.height(6.dp), enabled = enabled, colors = colors,
            thumbTrackGapSize = 3.dp, drawStopIndicator = null) })
}

/** One navigation treatment for global settings and nested reader menus. */
@Composable
internal fun SettingsMenuRow(title: String, subtitle: String = "", onClick: () -> Unit) {
    val themed = LocalFluxThemeMode.current.isExperimentalChrome
    val pal = LocalExperimentalColors.current
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = if (themed) pal.background else MaterialTheme.colorScheme.surfaceVariant,
        border = if (themed) androidx.compose.foundation.BorderStroke(1.dp, pal.border.copy(alpha = .65f)) else null) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall,
                    color = if (themed) pal.accent else MaterialTheme.colorScheme.onSurface)
                if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = if (themed) pal.accent else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
