package io.fluxreader.app.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.reader.NarrationStatus
import io.fluxreader.app.reader.NarrationVoice
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NarrationSheet(
    status: NarrationStatus,
    settings: ReaderSettings,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPreview: () -> Unit,
    onUpdate: ((ReaderSettings) -> ReaderSettings) -> Unit,
) {
    val context = LocalContext.current
    FluxModalBottomSheet(onDismissRequest = onDismiss) {
        ModalSheetHeader("Озвучивание книги", onClose = onDismiss, modifier = Modifier.padding(horizontal = 20.dp))

        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            status.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                IconButton(onClick = onPrevious, enabled = status.ready) { Icon(Icons.Default.SkipPrevious, "Предыдущее предложение") }
                IconButton(onClick = if (status.playing) onPause else onPlay, enabled = status.ready) {
                    Icon(if (status.playing) Icons.Default.Pause else Icons.Default.PlayArrow, if (status.playing) "Пауза" else "Воспроизвести")
                }
                IconButton(onClick = onStop, enabled = status.ready) { Icon(Icons.Default.Stop, "Остановить") }
                IconButton(onClick = onNext, enabled = status.ready) { Icon(Icons.Default.SkipNext, "Следующее предложение") }
            }
            if (status.preparing) Text("Подготовка озвучки…", style = MaterialTheme.typography.bodySmall)
            if (status.currentText.isNotBlank()) {
                Text(status.currentText.take(180), maxLines = 3, style = MaterialTheme.typography.bodySmall)
            }
            Text("Скорость: ${String.format(Locale.US, "%.2f", settings.narrationRate)}×")
            Slider(value = settings.narrationRate, onValueChange = { rate -> onUpdate { it.copy(narrationRate = rate) } }, valueRange = 0.6f..1.4f)
            Text("Высота голоса: ${String.format(Locale.US, "%.2f", settings.narrationPitch)}×")
            Slider(value = settings.narrationPitch, onValueChange = { pitch -> onUpdate { it.copy(narrationPitch = pitch) } }, valueRange = 0.75f..1.25f)
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Checkbox(settings.narrationAutoNextChapter, onCheckedChange = { selected -> onUpdate { it.copy(narrationAutoNextChapter = selected) } })
                Text("Автоматически читать следующую главу")
            }
            HorizontalDivider()
            VoiceSourceSettings(settings, onUpdate, onPreview, status.ready, status.preparing, androidVoices = {
                Text("Сетевые голоса могут отправлять текст речевому сервису.", style = MaterialTheme.typography.bodySmall)
            Text("Выбор голоса (${status.currentLanguage})", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { onUpdate { it.copy(narrationVoiceId = "") } }) { Text(if (settings.narrationVoiceId.isBlank()) "✓ Автоматически — лучший доступный" else "Автоматически — лучший доступный") }
            Column(Modifier.height(220.dp).verticalScroll(rememberScrollState())) {
                val sorted = status.voices.filter { it.language.startsWith(status.currentLanguage) }
                sorted.forEach { voice: NarrationVoice ->
                    TextButton(modifier = Modifier.fillMaxWidth(), onClick = { onUpdate { it.copy(narrationVoiceId = voice.name) } }) {
                        val label = if (settings.narrationVoiceId == voice.name) "✓ " else ""
                        val quality = if (voice.quality >= android.speech.tts.Voice.QUALITY_HIGH) "высокое качество" else "обычное качество"
                        Text("$label${voice.name} · ${voice.language} · $quality${if (voice.needsNetwork) " · сеть" else " · офлайн"}", modifier = Modifier.weight(1f))
                    }
                }
            }
            })
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!io.fluxreader.app.reader.BuiltInVoiceCatalog.isBuiltIn(settings.narrationVoiceId)) Button(onClick = onPreview, enabled = status.ready) { Text("Проба голоса") }
                if (!io.fluxreader.app.reader.BuiltInVoiceCatalog.isBuiltIn(settings.narrationVoiceId)) TextButton(onClick = {
                    runCatching { context.startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
                        .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) } }
                }) { Text("Настройки голосов Android") }
            }
        }
    }
}
