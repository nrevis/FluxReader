package io.fluxreader.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.fluxreader.app.AppViewModel
import io.fluxreader.app.audio.AudiobookState
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.useCyberpunkDesign
import io.fluxreader.app.data.useWastelandDesign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudiobooksScreen(viewModel: AppViewModel, settings: ReaderSettings, onBack: () -> Unit) {
    val state by viewModel.audiobooks.collectAsState()
    val experimental = settings.useWastelandDesign || settings.useCyberpunkDesign
    var deletingId by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) viewModel.importAudiobooks(uris)
    }
    val importFiles = { picker.launch(arrayOf("*/*")) }
    val header: @Composable () -> Unit = {
        if (experimental) {
            ExperimentalTopBar(title = "AUDIOBOOKS", subtitle = "${state.entries.size} FILES",
                navigation = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") } },
                actions = { IconButton(onClick = importFiles) { Icon(Icons.Default.Add, contentDescription = "Добавить аудиокнигу") } })
        } else {
            TopAppBar(title = { Text("Аудиокниги") }, navigationIcon = {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад") }
            }, actions = {
                IconButton(onClick = importFiles) { Icon(Icons.Default.Add, contentDescription = "Добавить аудиокнигу") }
            })
        }
    }
    val page: @Composable (Modifier) -> Unit = { contentModifier ->
        Column(contentModifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.entries.isEmpty()) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(24.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Headphones, contentDescription = null)
                    Text("Здесь будут ваши аудиокниги")
                    Text("Добавьте MP3, M4A, M4B, AAC, OGG, FLAC или WAV.", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = importFiles) { Text("Добавить аудиокниги") }
                }
            } else {
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(state.entries, key = { it.id }) { entry ->
                        val row: @Composable () -> Unit = {
                            Row(Modifier.fillMaxWidth().clickable { viewModel.playAudiobook(entry.id) }.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Default.Headphones, contentDescription = null)
                                Column(Modifier.weight(1f)) {
                                    Text(entry.name, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                        color = if (state.activeId == entry.id) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                    Text("${clock(entry.positionMs)} / ${clock(entry.durationMs)}", style = MaterialTheme.typography.labelSmall)
                                }
                                IconButton(onClick = { deletingId = entry.id }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Убрать из аудиобиблиотеки")
                                }
                            }
                        }
                        if (experimental) ExperimentalPanel(Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentPadding = 2.dp) { row() }
                        else row()
                    }
                }
            }
            state.error?.let { Text(it, modifier = Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.error) }
            if (state.activeId != null) {
                AudiobookPlayerControls(state, onToggle = { viewModel.toggleAudiobook() },
                    onSkip = { viewModel.skipAudiobook(it) }, onSeek = { viewModel.seekAudiobook(it) })
            }
        }
    }
    if (experimental) {
        Column(Modifier.fillMaxSize()) { header(); page(Modifier.weight(1f)) }
    } else {
        Scaffold(topBar = header) { padding -> page(Modifier.padding(padding)) }
    }
    deletingId?.let { id ->
        AlertDialog(onDismissRequest = { deletingId = null }, title = { Text("Убрать аудиокнигу?") },
            text = { Text("Файл на устройстве не удаляется. Запись и её позиция будут удалены из Flux Reader.") },
            confirmButton = { TextButton(onClick = { viewModel.removeAudiobook(id); deletingId = null }) { Text("Убрать") } },
            dismissButton = { TextButton(onClick = { deletingId = null }) { Text("Отмена") } })
    }
}

@Composable
private fun AudiobookPlayerControls(
    state: AudiobookState,
    onToggle: () -> Unit,
    onSkip: (Long) -> Unit,
    onSeek: (Long) -> Unit,
) {
    var dragging by remember(state.activeId) { mutableStateOf(false) }
    var fraction by remember(state.activeId) { mutableFloatStateOf(0f) }
    LaunchedEffect(state.positionMs, state.durationMs, state.activeId) {
        if (!dragging) fraction = if (state.durationMs > 0) (state.positionMs.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
    }
    ExperimentalPanel(Modifier.fillMaxWidth().padding(8.dp), accent = true) {
        Column(Modifier.fillMaxWidth()) {
            Text(state.entries.firstOrNull { it.id == state.activeId }?.name.orEmpty(), maxLines = 1,
                overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall)
            Slider(value = fraction, onValueChange = { dragging = true; fraction = it },
                onValueChangeFinished = {
                    onSeek((fraction * state.durationMs).toLong()); dragging = false
                }, enabled = !state.loading && state.durationMs > 0L)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(clock(if (dragging) (fraction * state.durationMs).toLong() else state.positionMs))
                Text(clock(state.durationMs))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSkip(-15_000L) }, enabled = !state.loading) {
                    Icon(Icons.Default.SkipPrevious, contentDescription = "Назад на 15 секунд")
                }
                Button(onClick = onToggle, enabled = !state.loading) {
                    Icon(if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                    Text(if (state.loading) "Загрузка" else if (state.playing) "Пауза" else "Играть")
                }
                IconButton(onClick = { onSkip(15_000L) }, enabled = !state.loading) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Вперёд на 15 секунд")
                }
            }
        }
    }
}

private fun clock(ms: Long): String {
    val seconds = (ms / 1_000L).coerceAtLeast(0L)
    val h = seconds / 3600
    val m = (seconds / 60) % 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
