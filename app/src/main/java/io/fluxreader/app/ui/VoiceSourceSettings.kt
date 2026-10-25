package io.fluxreader.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.reader.BuiltInVoiceCatalog
import io.fluxreader.app.reader.BuiltInVoiceStore

/** Shared by ordinary reading and RSVP. Download/voice state is app-scoped. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VoiceSourceSettings(
    settings: ReaderSettings,
    onUpdate: ((ReaderSettings) -> ReaderSettings) -> Unit,
    onPreview: () -> Unit,
    previewReady: Boolean,
    preparing: Boolean,
    androidVoices: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val store = remember(context) { BuiltInVoiceStore.get(context) }
    val pack by store.state.collectAsState()
    val builtin = BuiltInVoiceCatalog.isBuiltIn(settings.narrationVoiceId)
    var downloadDialog by remember { mutableStateOf(false) }
    var licenseDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = !builtin, onClick = { onUpdate { it.copy(narrationVoiceId = "") } }, label = { Text("Android") })
        FilterChip(selected = builtin, onClick = {
            onUpdate { it.copy(narrationVoiceId = BuiltInVoiceCatalog.DEFAULT_VOICE) }
        }, label = { Text("Встроенные") })
    }
    if (!builtin) androidVoices() else {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Supertonic 3 · русский", style = MaterialTheme.typography.titleMedium)
                Text(if (pack.installed) "10 голосов · без интернета" else "10 голосов · загрузка 123 МБ · бесплатно",
                    style = MaterialTheme.typography.bodySmall)
                when {
                    pack.busy -> {
                        if (pack.removing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("Удаление голосов…", style = MaterialTheme.typography.bodySmall)
                        } else if (pack.installing) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("Проверка и установка…", style = MaterialTheme.typography.bodySmall)
                        } else if (!pack.installed) {
                            LinearProgressIndicator(progress = { (pack.downloaded.toFloat() / BuiltInVoiceCatalog.DOWNLOAD_BYTES).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                            Text("Скачано ${pack.downloaded * 100 / BuiltInVoiceCatalog.DOWNLOAD_BYTES}%", style = MaterialTheme.typography.bodySmall)
                            TextButton(onClick = store::cancel) { Text("Отменить загрузку") }
                        }
                    }
                    pack.installed -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = onPreview, enabled = previewReady && !preparing) { Text(if (preparing) "Подготовка…" else "Проба голоса") }
                        TextButton(onClick = { licenseDialog = true }) { Text("Лицензия") }
                        TextButton(onClick = { deleteDialog = true }) { Text("Удалить голоса") }
                    }
                    else -> Button(onClick = { downloadDialog = true }) { Text(if (pack.error == null) "Скачать голоса" else "Повторить загрузку") }
                }
                pack.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        }
        if (pack.installed && !pack.busy) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BuiltInVoiceCatalog.voices.forEach { voice ->
                    FilterChip(selected = settings.narrationVoiceId == voice.id,
                        onClick = { onUpdate { it.copy(narrationVoiceId = voice.id) } }, label = { Text(voice.label) })
                }
            }
        }
    }
    if (downloadDialog) AlertDialog(onDismissRequest = { downloadDialog = false },
        title = { Text("Скачать встроенные голоса?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Загрузка — 123 МБ. Для установки нужно 420 МБ свободного места. После загрузки озвучка работает без сети.")
            Text("Использование модели регулируется OpenRAIL-M. Нажимая «Принять и скачать», вы принимаете условия лицензии.", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { licenseDialog = true }) { Text("Прочитать лицензию") }
        } }, confirmButton = { TextButton(onClick = { downloadDialog = false; store.download() }) { Text("Принять и скачать") } },
        dismissButton = { TextButton(onClick = { downloadDialog = false }) { Text("Отмена") } }, shape = MaterialTheme.shapes.large)
    if (licenseDialog) {
        val license = remember { context.assets.open(BuiltInVoiceCatalog.LICENSE_ASSET).bufferedReader().use { it.readText() } }
        AlertDialog(onDismissRequest = { licenseDialog = false }, title = { Text("Лицензия модели · OpenRAIL-M") },
            text = { Text(license, modifier = Modifier.heightIn(max = 430.dp).verticalScroll(rememberScrollState()), style = MaterialTheme.typography.bodySmall) },
            confirmButton = { TextButton(onClick = { licenseDialog = false }) { Text("Закрыть") } }, shape = MaterialTheme.shapes.large)
    }
    if (deleteDialog) DestructiveConfirmDialog(title = "Удалить встроенные голоса?",
        message = "Освободится около 139 МБ. Позже голоса можно скачать снова.", confirmLabel = "Удалить",
        onConfirm = { deleteDialog = false; store.delete() }, onDismiss = { deleteDialog = false })
}
