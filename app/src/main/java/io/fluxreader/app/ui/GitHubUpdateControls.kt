package io.fluxreader.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.fluxreader.app.AppViewModel
import io.fluxreader.app.update.GitHubUpdateState
import io.fluxreader.app.update.UpdateRelease
import io.fluxreader.app.update.RELEASES_WEB_URL
import io.fluxreader.app.update.UpdatePhase

@Composable
internal fun GitHubUpdateControls(viewModel: AppViewModel) {
    val state by viewModel.githubUpdates.collectAsState()
    val context = LocalContext.current
    GitHubUpdateContent(state, viewModel::setAutoUpdateCheck, viewModel::setPreReleaseUpdates,
        viewModel::checkForUpdates, viewModel::downloadGitHubUpdate, viewModel::installGitHubUpdate,
        onGitHub = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_WEB_URL))) })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GitHubUpdateContent(
    state: GitHubUpdateState,
    onAutoCheck: (Boolean) -> Unit,
    onPreReleases: (Boolean) -> Unit,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onGitHub: () -> Unit,
) {
    var notesTag by remember { mutableStateOf<String?>(null) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text("Обновления · GitHub", style = MaterialTheme.typography.titleLarge)
        SettingsToggleRow("Проверять при запуске", "Раз в сутки.", state.autoCheck, onAutoCheck)
        SettingsToggleRow("Тестовые alpha-релизы", "", state.includePreReleases, onPreReleases)
        Text(state.message, style = MaterialTheme.typography.bodyMedium)
        if (state.phase == UpdatePhase.DOWNLOADING) {
            LinearProgressIndicator(progress = { state.progressPercent / 100f }, modifier = Modifier.fillMaxWidth())
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCheck,
                enabled = state.phase != UpdatePhase.CHECKING && state.phase != UpdatePhase.DOWNLOADING) { Text("Проверить") }
            OutlinedButton(onClick = onGitHub) { Text("GitHub") }
        }
        state.release?.let { release ->
            Text(release.tag, style = MaterialTheme.typography.titleMedium)
            if (release.notes.isNotBlank()) {
                SettingsMenuRow("Список изменений", onClick = { notesTag = release.tag })
            }
            when (state.phase) {
                UpdatePhase.AVAILABLE, UpdatePhase.ERROR -> Button(onClick = onDownload) { Text("Скачать APK") }
                UpdatePhase.READY -> Button(onClick = onInstall) { Text("Установить обновление") }
                else -> Unit
            }
        }
    }
    state.release?.takeIf { it.tag == notesTag }?.let { release ->
        ReleaseNotesSheet(release, onDismiss = { notesTag = null })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReleaseNotesSheet(release: UpdateRelease, onDismiss: () -> Unit) {
    FluxModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().heightIn(max = 640.dp)
            .padding(horizontal = 20.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ModalSheetHeader("Список изменений", onClose = onDismiss, subtitle = release.tag)
            HorizontalDivider()
            SelectionContainer(Modifier.weight(1f, fill = false)) {
                Text(release.notes, modifier = Modifier.verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
