package io.fluxreader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.fluxreader.app.data.ReaderThemeMode

/** Shared setting row used by both global settings and in-reader settings sheets. */
@Composable
internal fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val body: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (LocalFluxThemeMode.current.isExperimentalChrome) LocalFluxThemeMode.current.chromeText else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) { body() }
    } else {
        body()
    }
}

/** Shared committed slider: UI follows the draft immediately, persistence happens on release. */
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (LocalFluxThemeMode.current.isExperimentalChrome) LocalFluxThemeMode.current.chromeText else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    format(draft),
                    color = if (LocalFluxThemeMode.current.isExperimentalChrome) LocalFluxThemeMode.current.chromeAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(
                value = draft.coerceIn(range.start, range.endInclusive),
                onValueChange = { draft = it },
                onValueChangeFinished = { onCommit(draft) },
                valueRange = range,
            )
        }
    }
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) { body() }
    } else {
        body()
    }
}
