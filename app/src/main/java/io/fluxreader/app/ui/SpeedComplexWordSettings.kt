package io.fluxreader.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.SpeedPacingMode
import io.fluxreader.app.reader.SpeedReaderCursor
import io.fluxreader.app.reader.SpeedChunk
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay

@Composable
internal fun SpeedComplexWordSettings(settings: ReaderSettings, onUpdate: ((ReaderSettings) -> ReaderSettings) -> Unit) {
    var sample by rememberSaveable { mutableStateOf("Макгонагалл") }
    var testing by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf("") }
    val word = sample.trim().ifBlank { "Макгонагалл" }
    fun duration(text: String, extraMs: Int = settings.speedComplexWordPauseMs): Long {
        val tokens = text.split(Regex("[\\s\\p{Zs}\\p{Zl}\\p{Zp}\\u001C-\\u001F]+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return 0
        val chunk = SpeedChunk(tokens, 0, text.length, -1)
        return SpeedReaderCursor.delayMillis(settings.speedWpm, chunk, text, settings.speedPacingMode,
            paragraphPause = false, microPausesEnabled = false, complexWordPauseMs = extraMs)
    }
    val without = duration(word, 0)
    val actual = duration(word)
    val ordinary = duration("слово")
    LaunchedEffect(testing, word, settings.speedWpm, settings.speedComplexWordPauseMs, settings.speedPacingMode) {
        if (!testing) { shown = ""; return@LaunchedEffect }
        if (testing) {
            try {
                repeat(3) {
                    for (next in listOf("слово", word)) {
                        shown = next
                        withFrameNanos { }
                        delay(duration(next))
                    }
                }
            } finally {
                if (currentCoroutineContext().isActive) {
                    shown = ""
                    testing = false
                }
            }
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (settings.speedPacingMode == SpeedPacingMode.CONSTANT) {
            Text("В темпе «Ровно» замедление выключено.", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { onUpdate { it.copy(speedPacingMode = SpeedPacingMode.SMART) } }) { Text("Включить «Умно»") }
        } else {
            SettingsValueSlider("Добавка сложным словам", settings.speedComplexWordPauseMs.toFloat(), 0f..800f,
                { "${it.toInt()} мс" }) { value -> onUpdate { it.copy(speedComplexWordPauseMs = value.toInt()) } }
            Text("Длинные слова и имена. Для очень длинных — больше времени.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        OutlinedTextField(value = sample, onValueChange = { sample = it.replace('\n', ' ').take(80) },
            label = { Text("Проверить слово или фразу") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (shown.isBlank()) word else shown, fontSize = 28.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("$actual мс · добавка ${actual - without} мс", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text("Обычное «слово»: $ordinary мс · ${settings.speedWpm} WPM", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { testing = !testing }) { Text(if (testing) "Остановить" else "Проба показа") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpeedComplexWordSheet(settings: ReaderSettings, onUpdate: ((ReaderSettings) -> ReaderSettings) -> Unit,
    onBack: () -> Unit, onDismiss: () -> Unit) {
    FluxModalBottomSheet(onDismissRequest = onDismiss, onBack = onBack,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        ModalSheetHeader("Сложные слова", onBack = onBack, onClose = onDismiss, modifier = Modifier.padding(horizontal = 20.dp))
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
            SpeedComplexWordSettings(settings, onUpdate)
        }
    }
}
