package io.fluxreader.app.reader

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class BuiltInVoiceState(
    val installed: Boolean = false,
    val busy: Boolean = false,
    val downloaded: Long = 0,
    val installing: Boolean = false,
    val removing: Boolean = false,
    val error: String? = null,
)

/** App-scoped downloads survive closing a settings sheet, but never start implicitly. */
class BuiltInVoiceStore private constructor(context: Context) {
    private val app = context.applicationContext
    private val root = File(app.noBackupFilesDir, "tts-models-v1").apply { mkdirs() }
    val directory = File(root, BuiltInVoiceCatalog.PACK_ID)
    private val part = File(root, "download.part")
    private val staging = File(root, "installing")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var operation: Job? = null
    @Volatile private var activeConnection: HttpURLConnection? = null
    private val mutableState = MutableStateFlow(BuiltInVoiceState(installed = VoicePackageInstaller.installed(directory)))
    val state = mutableState.asStateFlow()

    @Synchronized fun download() {
        if (operation?.isCompleted == false || mutableState.value.installed) return
        mutableState.value = BuiltInVoiceState(busy = true)
        operation = scope.launch {
            try {
                // Archive + unpacked model + bounded WAV cache, with room for atomic install.
                part.delete(); staging.deleteRecursively()
                require(root.usableSpace >= 420L * 1024 * 1024) { "Для загрузки нужно 420 МБ свободного места" }
                val connection = openDownload()
                try {
                    var received = 0L
                    var published = 0L
                    connection.inputStream.buffered().use { input -> part.outputStream().buffered().use { out ->
                        val buffer = ByteArray(65536)
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            received += n
                            require(received <= BuiltInVoiceCatalog.DOWNLOAD_BYTES) { "Неверный размер пакета" }
                            out.write(buffer, 0, n)
                            if (received - published >= 512 * 1024) {
                                published = received
                                mutableState.value = BuiltInVoiceState(busy = true, downloaded = received)
                            }
                        }
                    } }
                } finally { connection.disconnect() }
                mutableState.value = BuiltInVoiceState(busy = true, downloaded = part.length(), installing = true)
                VoicePackageInstaller.verify(part, BuiltInVoiceCatalog.DOWNLOAD_BYTES, BuiltInVoiceCatalog.SHA256) { coroutineContext.ensureActive() }
                part.inputStream().use { VoicePackageInstaller.extract(it, staging, BuiltInVoiceCatalog.ARCHIVE_ROOT,
                    BuiltInVoiceCatalog.files) { coroutineContext.ensureActive() } }
                // Keep both the code notice in the archive and the actual model license.
                app.assets.open(BuiltInVoiceCatalog.LICENSE_ASSET).use { input -> File(staging, "MODEL-LICENSE.txt").outputStream().use { input.copyTo(it) } }
                File(staging, "verified.sha256").writeText(BuiltInVoiceCatalog.SHA256)
                coroutineContext.ensureActive()
                // Once the commit starts, report its real result even if Cancel races with it.
                synchronized(this@BuiltInVoiceStore) {
                    directory.deleteRecursively()
                    Files.move(staging.toPath(), directory.toPath(), StandardCopyOption.ATOMIC_MOVE)
                    mutableState.value = BuiltInVoiceState(installed = true)
                }
            } catch (cancelled: CancellationException) {
                mutableState.value = BuiltInVoiceState(installed = VoicePackageInstaller.installed(directory))
                throw cancelled
            } catch (e: Exception) {
                coroutineContext.ensureActive() // Disconnect after Cancel is not a network error.
                mutableState.value = BuiltInVoiceState(installed = VoicePackageInstaller.installed(directory),
                    error = if (e is java.io.IOException) "Не удалось скачать голос. Проверьте соединение и повторите" else e.message ?: "Не удалось установить голос")
            } finally {
                activeConnection?.disconnect(); activeConnection = null
                part.delete(); staging.deleteRecursively()
            }
        }
        operation?.invokeOnCompletion { cause ->
            // A cancellation before Dispatchers.IO even starts must also clear the spinner.
            if (cause is CancellationException && mutableState.value.busy) {
                mutableState.value = BuiltInVoiceState(installed = VoicePackageInstaller.installed(directory))
            }
        }
    }

    @Synchronized fun cancel() {
        operation?.cancel()
        val connection = activeConnection
        scope.launch { connection?.disconnect() }
    }

    @Synchronized fun delete() {
        if (operation?.isCompleted == false) return
        mutableState.value = BuiltInVoiceState(busy = true, removing = true)
        operation = scope.launch {
            try {
                BuiltInSynthesis.withReleasedModel {
                    check(directory.deleteRecursively()) { "Не удалось удалить голос" }
                    File(app.cacheDir, "builtin-speech-v1").deleteRecursively()
                }
                mutableState.value = BuiltInVoiceState()
            } catch (e: Exception) { mutableState.value = BuiltInVoiceState(installed = VoicePackageInstaller.installed(directory), error = e.message) }
        }
    }

    private fun openDownload(): HttpURLConnection {
        var url = URI(BuiltInVoiceCatalog.URL).toURL()
        repeat(6) {
            require(url.protocol == "https")
            val c = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 20000; readTimeout = 20000; instanceFollowRedirects = false
                setRequestProperty("User-Agent", "FluxReader/" + io.fluxreader.app.BuildConfig.VERSION_NAME)
            }
            activeConnection = c
            val response = c.responseCode
            if (response in listOf(301, 302, 303, 307, 308)) {
                val location = c.getHeaderField("Location")
                c.disconnect()
                require(!location.isNullOrBlank())
                url = URI(url.toString()).resolve(location).toURL()
            } else {
                if (response != 200) { c.disconnect(); throw java.io.IOException("HTTP $response") }
                val size = c.contentLengthLong
                if (size > 0 && size != BuiltInVoiceCatalog.DOWNLOAD_BYTES) { c.disconnect(); error("Неверный размер пакета") }
                return c
            }
        }
        error("Слишком много перенаправлений при загрузке")
    }

    companion object {
        @Volatile private var instance: BuiltInVoiceStore? = null
        fun get(context: Context): BuiltInVoiceStore = instance ?: synchronized(this) {
            instance ?: BuiltInVoiceStore(context).also { instance = it }
        }
    }
}
