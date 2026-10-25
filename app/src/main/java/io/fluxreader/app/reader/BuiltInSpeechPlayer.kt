package io.fluxreader.app.reader

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Handler
import android.os.Looper
import com.k2fsa.sherpa.onnx.GenerationConfig
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsSupertonicModelConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/** One lazy native model per process. Allocation, inference and release share the same gate. */
internal object BuiltInSynthesis {
    private val gate = Mutex()
    private var model: OfflineTts? = null
    suspend fun withReleasedModel(action: () -> Unit) = gate.withLock {
        model?.release(); model = null
        action()
    }

    suspend fun render(context: Context, text: String, voiceId: String): File = withContext(Dispatchers.IO) {
        val voice = BuiltInVoiceCatalog.voice(voiceId) ?: error("Выберите встроенный голос")
        require(text.isNotBlank() && text.length <= 700)
        gate.withLock {
            ensureActive()
            val store = BuiltInVoiceStore.get(context)
            check(store.state.value.installed && !store.state.value.busy) { "Скачайте встроенные голоса" }
            val cache = File(context.cacheDir, "builtin-speech-v1").apply { mkdirs() }
            val hash = MessageDigest.getInstance("SHA-256").digest(
                (BuiltInVoiceCatalog.PACK_ID + "|5|ru|" + voiceId + "|" + text).toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
            val output = File(cache, "$hash.wav")
            if (output.isFile && output.length() > 44) {
                output.setLastModified(System.currentTimeMillis())
                return@withLock output
            }
            val native = model ?: run {
                val d = store.directory
                fun path(name: String) = File(d, name).absolutePath
                val config = OfflineTtsConfig(model = OfflineTtsModelConfig(
                    supertonic = OfflineTtsSupertonicModelConfig(
                        durationPredictor = path("duration_predictor.int8.onnx"),
                        textEncoder = path("text_encoder.int8.onnx"),
                        vectorEstimator = path("vector_estimator.int8.onnx"),
                        vocoder = path("vocoder.int8.onnx"),
                        ttsJson = path("tts.json"), unicodeIndexer = path("unicode_indexer.bin"),
                        voiceStyle = path("voice.bin")),
                    numThreads = 2, provider = "cpu"), maxNumSentences = 1)
                OfflineTts(config = config).also { model = it }
            }
            val owner = currentCoroutineContext()
            val started = android.os.SystemClock.elapsedRealtime()
            val audio = native.generateWithConfigAndCallback(text,
                GenerationConfig(sid = voice.speaker, speed = 1f, numSteps = 5,
                    extra = mapOf("lang" to "ru")), NativeSpeechCallback(owner))
            ensureActive()
            check(audio.sampleRate > 0 && audio.samples.isNotEmpty()) { "Не удалось озвучить фразу" }
            val part = File(cache, "$hash.part")
            try {
                check(audio.save(part.absolutePath)) { "Не удалось сохранить озвученную фразу" }
                ensureActive()
                check(part.renameTo(output))
            } finally { part.delete() }
            android.util.Log.i("FluxOfflineSpeech", "Generated ${audio.samples.size / audio.sampleRate.toFloat()}s in ${android.os.SystemClock.elapsedRealtime() - started}ms; speaker=${voice.speaker}")
            var bytes = 0L
            cache.listFiles { f -> f.extension == "wav" }?.sortedByDescending { it.lastModified() }?.forEach {
                bytes += it.length()
                if (bytes > 64L * 1024 * 1024 && it != output) it.delete()
            }
            output
        }
    }
}

internal data class BuiltInPlaybackState(val preparing: Boolean = false, val playing: Boolean = false, val error: String? = null)

/** Main-thread transport. Cancellation never frees a native model while inference is running. */
internal class BuiltInSpeechPlayer(context: Context) {
    private val app = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(BuiltInPlaybackState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private var prefetch: Job? = null
    private var player: MediaPlayer? = null
    private var generation = 0L
    private var closed = false
    private val audioManager = app.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private var focus: AudioFocusRequest? = null

    init {
        scope.launch {
            BuiltInVoiceStore.get(app).state.collect { if (!it.installed && (state.value.preparing || state.value.playing)) stop() }
        }
    }

    fun speak(text: String, voiceId: String, rate: Float, pitch: Float,
              following: List<String> = emptyList(), onStarted: () -> Unit = {},
              onDone: () -> Unit = {}, onError: (String) -> Unit = {}) {
        stop()
        if (closed) return
        val token = generation
        mutableState.value = BuiltInPlaybackState(preparing = true)
        job = scope.launch {
            try {
                val wav = BuiltInSynthesis.render(app, text, voiceId)
                ensureActive()
                if (generation != token) return@launch
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(attributes).setOnAudioFocusChangeListener({ change ->
                        if (generation == token && change <= AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                            stop(); onError("Воспроизведение прервано другим приложением")
                        }
                    }, Handler(Looper.getMainLooper())).build()
                check(audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { "Звук сейчас занят другим приложением" }
                focus = request
                val media = MediaPlayer()
                player = media
                media.setAudioAttributes(attributes)
                media.setDataSource(wav.absolutePath)
                media.setOnPreparedListener {
                    if (generation != token || closed) return@setOnPreparedListener
                    try {
                        it.playbackParams = PlaybackParams().setSpeed(rate.coerceIn(0.6f, 1.4f)).setPitch(pitch.coerceIn(0.75f, 1.25f))
                        it.start()
                        mutableState.value = BuiltInPlaybackState(playing = true)
                        onStarted()
                        prefetch = scope.launch {
                            for (next in following.take(2)) {
                                if (!isActive || generation != token) break
                                try { BuiltInSynthesis.render(app, next, voiceId) }
                                catch (c: CancellationException) { throw c }
                                catch (_: Exception) { break } // A later Play reports a real synthesis failure.
                            }
                        }
                    } catch (e: Exception) { finishError(token, e.message, onError) }
                }
                media.setOnCompletionListener {
                    if (generation == token && !closed) { stop(); onDone() }
                }
                media.setOnErrorListener { _, _, _ ->
                    if (generation == token && !closed) finishError(token, "Не удалось воспроизвести встроенный голос", onError)
                    true
                }
                media.prepareAsync()
            } catch (c: CancellationException) { throw c }
            catch (e: Exception) { finishError(token, e.message, onError) }
            catch (_: LinkageError) { finishError(token, "Встроенный движок не поддерживается на этом устройстве", onError) }
        }
    }

    private fun finishError(token: Long, message: String?, onError: (String) -> Unit) {
        if (token != generation || closed) return
        stop()
        val error = message ?: "Не удалось запустить встроенную озвучку"
        mutableState.value = BuiltInPlaybackState(error = error)
        onError(error)
    }

    fun stop() {
        generation++
        job?.cancel(); job = null
        prefetch?.cancel(); prefetch = null
        player?.let { runCatching { it.setOnCompletionListener(null); it.setOnErrorListener(null); it.release() } }
        player = null
        focus?.let { audioManager.abandonAudioFocusRequest(it) }; focus = null
        mutableState.value = BuiltInPlaybackState()
    }

    fun close() {
        stop(); closed = true; scope.cancel()
        // Release on a separate scope after an in-flight native call exits its mutex.
        CoroutineScope(Dispatchers.IO).launch { BuiltInSynthesis.withReleasedModel {} }
    }
}
