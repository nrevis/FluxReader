package io.fluxreader.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.fluxreader.app.AppViewModel
import io.fluxreader.app.update.RELEASES_WEB_URL
import io.fluxreader.app.update.UpdatePhase

@Composable
internal fun GitHubUpdateControls(viewModel: AppViewModel) {
    val state by viewModel.githubUpdates.collectAsState()
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Обновления · GitHub", style = MaterialTheme.typography.titleLarge)
        Text("Источник: github.com/nrevis/FluxReader", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Проверять раз в сутки при запуске", modifier = Modifier.weight(1f).padding(end = 8.dp))
            Switch(checked = state.autoCheck, onCheckedChange = viewModel::setAutoUpdateCheck)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Включая тестовые alpha-релизы", modifier = Modifier.weight(1f).padding(end = 8.dp))
            Switch(checked = state.includePreReleases, onCheckedChange = viewModel::setPreReleaseUpdates)
        }
        Text(state.message, style = MaterialTheme.typography.bodyMedium)
        if (state.phase == UpdatePhase.DOWNLOADING) {
            LinearProgressIndicator(progress = { state.progressPercent / 100f }, modifier = Modifier.fillMaxWidth())
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = viewModel::checkForUpdates,
                enabled = state.phase != UpdatePhase.CHECKING && state.phase != UpdatePhase.DOWNLOADING,
            ) { Text("Проверить") }
            OutlinedButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_WEB_URL)))
            }) { Text("GitHub") }
        }
        val release = state.release
        if (release != null) {
            Text("${release.title} (${release.tag})", style = MaterialTheme.typography.titleMedium)
            if (release.notes.isNotBlank()) {
                Text(release.notes, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            when (state.phase) {
                UpdatePhase.AVAILABLE, UpdatePhase.ERROR -> Button(onClick = viewModel::downloadGitHubUpdate) {
                    Text("Скачать APK")
                }
                UpdatePhase.READY -> Button(onClick = viewModel::installGitHubUpdate) {
                    Text("Установить обновление")
                }
                else -> Unit
            }
        }
        Text("Установка открывает системный установщик Android и требует подтверждения. " +
            "Для обновления поверх текущего приложения APK должен иметь ту же цифровую подпись.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
