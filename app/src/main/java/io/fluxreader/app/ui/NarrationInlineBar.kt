package io.fluxreader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.fluxreader.app.reader.NarrationStatus

@Composable
fun NarrationInlineBar(
    status: NarrationStatus,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onAdvanced: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = onPrevious) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Предыдущая фраза")
            }
            IconButton(onClick = onPlayPause) {
                Icon(if (status.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (status.playing) "Пауза" else "Озвучить")
            }
            Text(status.currentText.ifBlank {
                status.error ?: if (status.ready) "Озвучивание готово" else "Загрузка голосов…"
            }, modifier = Modifier.weight(1f), maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall)
            IconButton(onClick = onNext) {
                Icon(Icons.Default.SkipNext, contentDescription = "Следующая фраза")
            }
            IconButton(onClick = onAdvanced) {
                Icon(Icons.Default.Tune, contentDescription = "Выбор голоса и параметры")
            }
        }
    }
}
