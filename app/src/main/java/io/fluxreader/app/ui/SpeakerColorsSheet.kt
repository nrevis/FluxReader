package io.fluxreader.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.SpeedSpeakerColor
import io.fluxreader.app.data.useCyberpunkDesign
import io.fluxreader.app.data.useWastelandDesign
import io.fluxreader.app.reader.BookSpeakerColors
import io.fluxreader.app.reader.SpeakerTint

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SpeakerColorsSheet(
    colors: BookSpeakerColors,
    settings: ReaderSettings,
    initialName: String? = null,
    onColor: (String, SpeakerTint?) -> Unit,
    onAutomatic: (Boolean) -> Unit,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(initialName) { mutableStateOf(initialName) }
    var query by remember { mutableStateOf("") }
    val back: () -> Unit = { if (selected != null && initialName == null) selected = null else onBack() }
    val experimental = settings.useWastelandDesign || settings.useCyberpunkDesign
    val palette = LocalExperimentalColors.current
    val background = if (experimental) palette.background else if (settings.speedUseReaderTheme) MaterialTheme.colorScheme.background else Color(settings.speedBackgroundColorArgb)
    val accent = if (experimental) palette.accent else if (settings.speedUseReaderTheme) MaterialTheme.colorScheme.primary else Color(settings.speedFocusColorArgb)
    FluxModalBottomSheet(onDismissRequest = onDismiss, onBack = back,
        sheetState = key(selected) { rememberModalBottomSheetState(skipPartiallyExpanded = true) }) {
        Column(Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ModalSheetHeader(if (selected == null) "Цвета персонажей" else "Цвет имени", onBack = back,
                onClose = onDismiss, subtitle = selected)
            val name = selected
            if (name == null) {
                SettingsToggleRow("Автоцвета персонажей", "", settings.speedSpeakerColor == SpeedSpeakerColor.CHARACTER, onAutomatic)
                OutlinedTextField(query, onValueChange = { query = it }, singleLine = true,
                    label = { Text("Найти имя") }, modifier = Modifier.fillMaxWidth())
                val names = remember(colors, query) {
                    colors.entries.values.filter { it.name.contains(query.trim(), true) }.sortedBy { it.name }
                }
                LazyColumn(Modifier.weight(1f, fill = false), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(names, key = { it.name }) { entry ->
                        val tint = entry.custom ?: entry.automatic.takeIf { settings.speedSpeakerColor == SpeedSpeakerColor.CHARACTER }
                        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().clickable { selected = entry.name }) {
                            Row(Modifier.padding(14.dp).heightIn(min = 32.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(Modifier.size(20.dp).background(tint?.let { speakerTintColor(it, background) } ?: accent, CircleShape))
                                Column(Modifier.weight(1f)) {
                                    Text(entry.name, style = MaterialTheme.typography.titleSmall)
                                    Text(entry.custom?.label ?: if (tint != null) "Автоматически" else "Общий цвет",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    if (names.isEmpty()) item {
                        Text(if (colors.entries.isEmpty()) "Имена появятся после распознавания диалогов." else "Имя не найдено.",
                            style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Во всей книге", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(Modifier.fillMaxWidth().background(background, MaterialTheme.shapes.large).padding(24.dp), contentAlignment = Alignment.Center) {
                        SpeedSpeakerLabel(name, settings, background, accent, characterColor = colors.forName(name))
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = colors.forName(name)?.custom == null, onClick = { onColor(name, null) }, label = { Text("По настройкам") })
                        SpeakerTint.entries.forEach { tint ->
                            FilterChip(selected = colors.forName(name)?.custom == tint, onClick = { onColor(name, tint) },
                                leadingIcon = { Box(Modifier.size(14.dp).background(speakerTintColor(tint, background), CircleShape)) },
                                label = { Text(tint.label) })
                        }
                    }
                }
            }
        }
    }
}
