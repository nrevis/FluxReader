package io.fluxreader.app.ui

import android.graphics.Typeface as AndroidTypeface
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.fluxreader.app.data.ReaderFontMode
import io.fluxreader.app.data.ReaderSettings
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val importedFontCache = ConcurrentHashMap<String, FontFamily>()

fun resolveReaderFontFamily(mode: ReaderFontMode, customFontPath: String): FontFamily = when (mode) {
    ReaderFontMode.SYSTEM -> FontFamily.Default
    ReaderFontMode.SERIF -> FontFamily.Serif
    ReaderFontMode.SANS -> FontFamily.SansSerif
    ReaderFontMode.MONO -> FontFamily.Monospace
    ReaderFontMode.JETBRAINS_NERD -> resolveImportedJetBrainsFont(customFontPath) ?: FontFamily.Monospace
}

private fun resolveImportedJetBrainsFont(path: String): FontFamily? {
    if (path.isBlank()) return null
    val file = File(path)
    if (!file.isFile || file.length() <= 0L) return null
    val key = "${file.absolutePath}:${file.length()}:${file.lastModified()}"
    return importedFontCache[key] ?: runCatching {
        FontFamily(AndroidTypeface.createFromFile(file))
    }.getOrNull()?.also { importedFontCache[key] = it }
}

@Composable
fun JetBrainsNerdFontImporter(
    settings: ReaderSettings,
    onUpdate: ((ReaderSettings) -> ReaderSettings) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember(settings.customFontPath, settings.customFontName) { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val resolver = context.contentResolver
                    val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) cursor.getString(0) else null
                    }?.takeIf { it.isNotBlank() } ?: "JetBrains Nerd Font"
                    val dir = File(context.filesDir, "fonts").apply { mkdirs() }
                    val temp = File(dir, "jetbrains_nerd_user_font.tmp")
                    val target = File(dir, "jetbrains_nerd_user_font.font")
                    resolver.openInputStream(uri)?.use { input ->
                        temp.outputStream().buffered().use { output -> input.copyTo(output, 64 * 1024) }
                    } ?: error("Не удалось открыть файл шрифта")
                    require(temp.length() in 4_096L..64L * 1024L * 1024L) { "Некорректный размер файла шрифта" }
                    AndroidTypeface.createFromFile(temp)
                    if (target.exists() && !target.delete()) error("Не удалось заменить старый шрифт")
                    if (!temp.renameTo(target)) {
                        temp.copyTo(target, overwrite = true)
                        temp.delete()
                    }
                    target.absolutePath to displayName
                }
            }
            result.onSuccess { (path, displayName) ->
                importedFontCache.clear()
                onUpdate { it.copy(customFontPath = path, customFontName = displayName) }
                status = "Шрифт подключён"
            }.onFailure { error ->
                status = error.message ?: "Не удалось подключить шрифт"
            }
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text("JetBrains Nerd Font", style = MaterialTheme.typography.labelLarge)
        Text(
            if (settings.customFontPath.isBlank()) {
                "Не подключён. Используется системный моноширинный шрифт."
            } else {
                "Подключён: ${settings.customFontName.ifBlank { "пользовательский шрифт" }}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                launcher.launch(
                    arrayOf(
                        "font/ttf",
                        "font/otf",
                        "application/x-font-ttf",
                        "application/x-font-opentype",
                        "application/octet-stream",
                    )
                )
            }) {
                Text(if (settings.customFontPath.isBlank()) "Импортировать" else "Заменить")
            }
            if (settings.customFontPath.isNotBlank()) {
                OutlinedButton(onClick = {
                    runCatching { File(settings.customFontPath).delete() }
                    importedFontCache.clear()
                    onUpdate { it.copy(customFontPath = "", customFontName = "") }
                    status = "Пользовательский шрифт отключён"
                }) {
                    Text("Сбросить")
                }
            }
        }
        status?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}
