package io.fluxreader.app.reader

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import io.fluxreader.app.data.ReaderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Speech and visual RSVP use the same chapter text and UTF-16 cursor.
 * onRangeStart provides accurate word boundaries if the installed engine supports it.
 * Never starts classic WPM playback at the same time as TTS. */
class SpeedSpeechNarrator(context: Context) {
    private val appContext = context.applicationContext
    private val store = BuiltInVoiceStore.get(appContext)
    private val builtIn = BuiltInSpeechPlayer(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var androidReady = false
    private var closed = false
    data class AvailableVoice(val id: String, val language: String, val quality: Int)
    data class Status(
        val voices: List<AvailableVoice> = emptyList(),
        val ready: Boolean = false,
        val playing: Boolean = false,
        val offset: Int = 0,
        val error: String? = null,
        val preparing: Boolean = false,
    )

    private val main = Handler(Looper.getMainLooper())
    private val _status = MutableStateFlow(Status())
    val status: StateFlow<Status> = _status.asStateFlow()
    private var tts: TextToSpeech? = null
    private var source = ""
    private var rate = 0.94f
    private var pitch = 1f
    private var voiceId = ""
    private var generation = 0L
    private var currentPiece: SpeedSpeechText.Piece? = null
    private var activeUtterance = ""
    private var pendingPlay = false
    var onSettledPosition: ((Int) -> Unit)? = null
    var onFinished: (() -> Unit)? = null

    init {
        scope.launch { store.state.collect {
            if (usesBuiltIn()) {
                if (!it.installed && _status.value.playing) pause()
                _status.value = _status.value.copy(ready = it.installed && !it.busy)
            }
        } }
    }
    private fun usesBuiltIn() = BuiltInVoiceCatalog.isBuiltIn(voiceId)

    private fun ensureEngine() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { outcome ->
            main.post {
                if (closed) return@post
                val engine = tts ?: return@post
                if (outcome != TextToSpeech.SUCCESS) {
                    if (!usesBuiltIn()) _status.value = _status.value.copy(error = "Голос Android недоступен. Можно скачать встроенные голоса")
                    pendingPlay = false
                    return@post
                }
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) { main.post {
                        if (utteranceId == activeUtterance) {
                            currentPiece?.let { piece -> _status.value = _status.value.copy(playing = true, offset = piece.start) }
                        }
                    } }
                    override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
                        main.post {
                            if (utteranceId == activeUtterance) currentPiece?.let { piece ->
                                _status.value = _status.value.copy(offset =
                                    (piece.start + start).coerceIn(piece.start, piece.end))
                            }
                        }
                    }
                    override fun onDone(utteranceId: String) { main.post done@{
                        if (utteranceId != activeUtterance || !_status.value.playing) return@done
                        val finished = currentPiece ?: return@done
                        _status.value = _status.value.copy(offset = finished.end)
                        onSettledPosition?.invoke(finished.end)
                        speakFrom(finished.end)
                    } }
                    @Deprecated("Android compatibility")
                    override fun onError(utteranceId: String) { main.post {
                        if (utteranceId == activeUtterance) {
                            _status.value = _status.value.copy(playing = false, error = "Ошибка воспроизведения TTS")
                        }
                    } }
                })
                androidReady = true
                _status.value = _status.value.copy(
                    ready = if (usesBuiltIn()) store.state.value.installed else true,
                    error = if (usesBuiltIn()) _status.value.error else null,
                    voices = engine.voices.orEmpty()
                        .filter { !it.isNetworkConnectionRequired }
                        .sortedWith(compareByDescending<android.speech.tts.Voice> { it.locale.language == "ru" }
                            .thenByDescending { it.quality })
                        .map { AvailableVoice(it.name, it.locale.toLanguageTag(), it.quality) },
                )
                configureVoice()
                if (pendingPlay) {
                    pendingPlay = false
                    if (_status.value.error == null) speakFrom(_status.value.offset)
                }
            }
        }
    }

    fun loadVoices() {
        ensureEngine()
    }

    fun configure(settings: ReaderSettings) {
        val changes = rate != settings.narrationRate || pitch != settings.narrationPitch || voiceId != settings.narrationVoiceId
        if (changes) { pause(); builtIn.stop() }
        rate = settings.narrationRate
        pitch = settings.narrationPitch
        voiceId = settings.narrationVoiceId
        _status.value = _status.value.copy(ready = if (usesBuiltIn()) store.state.value.installed && !store.state.value.busy else androidReady,
            error = if (changes) null else _status.value.error)
        if (_status.value.ready) configureVoice()
    }

    private fun configureVoice() {
        if (usesBuiltIn()) return
        val engine = tts ?: return
        val offline = engine.voices.orEmpty().filter { !it.isNetworkConnectionRequired }
        val explicit = offline.firstOrNull { it.name == voiceId }
        val russian = offline.filter { it.locale.language == "ru" }.maxByOrNull { it.quality }
        val chosen = explicit ?: russian ?: offline.maxByOrNull { it.quality }
        if (chosen == null) {
            _status.value = _status.value.copy(error = "Установите офлайн-голос в Android TTS")
            return
        }
        engine.language = chosen.locale
        engine.voice = chosen
        engine.setSpeechRate(rate.coerceIn(0.6f, 1.4f))
        engine.setPitch(pitch.coerceIn(0.7f, 1.3f))
        _status.value = _status.value.copy(error = null)
    }

    fun start(text: String, offset: Int) {
        source = text
        generation++
        val at = offset.coerceIn(0, source.length)
        _status.value = _status.value.copy(offset = at, error = null)
        if (usesBuiltIn() && (!store.state.value.installed || store.state.value.busy)) {
            _status.value = _status.value.copy(playing = false, error = "Скачайте встроенные голоса в настройках озвучки")
            return
        }
        if (!_status.value.ready) {
            pendingPlay = true
            ensureEngine()
            return
        }
        speakFrom(at)
    }

    private fun speakFrom(at: Int) {
        val piece = SpeedSpeechText.next(source, at, if (usesBuiltIn()) 260 else 340)
        if (piece == null) {
            currentPiece = null
            activeUtterance = ""
            _status.value = _status.value.copy(playing = false)
            onFinished?.invoke()
            return
        }
        currentPiece = piece
        activeUtterance = "speed-${++generation}"
        _status.value = _status.value.copy(playing = true, offset = piece.start)
        if (usesBuiltIn()) {
            val id = activeUtterance
            _status.value = _status.value.copy(preparing = true)
            val next = SpeedSpeechText.next(source, piece.end, 260)?.text
            builtIn.speak(piece.text, voiceId, rate, pitch, listOfNotNull(next),
                onStarted = { if (activeUtterance == id) _status.value = _status.value.copy(preparing = false) },
                onDone = {
                    if (activeUtterance == id && _status.value.playing) {
                        _status.value = _status.value.copy(offset = piece.end)
                        onSettledPosition?.invoke(piece.end)
                        speakFrom(piece.end)
                    }
                }, onError = {
                    if (activeUtterance == id) _status.value = _status.value.copy(playing = false, preparing = false, error = it)
                })
            return
        }
        val engine = tts ?: return
        val result = engine.speak(piece.text, TextToSpeech.QUEUE_FLUSH, null, activeUtterance)
        if (result == TextToSpeech.ERROR) {
            _status.value = _status.value.copy(playing = false, error = "Не удалось прочитать фразу")
        }
    }

    fun setPosition(offset: Int) {
        // Repositioning is not a pause/save event. Persisting the previous speech cursor here
        // would jump the Speed Reader back to 0 when the voice button is first enabled.
        pendingPlay = false
        activeUtterance = ""
        generation++
        tts?.stop()
        builtIn.stop()
        _status.value = _status.value.copy(playing = false, preparing = false, offset = offset.coerceAtLeast(0), error = null)
    }

    fun pause() {
        val wasSpeaking = _status.value.playing
        val wasPending = pendingPlay
        pendingPlay = false
        activeUtterance = ""
        generation++
        tts?.stop()
        builtIn.stop()
        _status.value = _status.value.copy(playing = false, preparing = false)
        // An idle TTS instance has an initial cursor of zero. Never publish that
        // cursor when Compose disposes/recreates the narrator or changes chapters.
        if (SpeedSpeechCheckpointPolicy.commitOnPause(wasSpeaking, wasPending)) {
            onSettledPosition?.invoke(_status.value.offset)
        }
    }

    fun close() {
        closed = true
        pendingPlay = false
        activeUtterance = ""
        tts?.stop()
        tts?.shutdown()
        tts = null
        builtIn.close()
        scope.cancel()
        onSettledPosition = null
        onFinished = null
    }

    fun preview() {
        pause()
        if (usesBuiltIn()) {
            _status.value = _status.value.copy(preparing = true, error = null)
            builtIn.speak(BuiltInVoiceCatalog.PREVIEW, voiceId, rate, pitch,
                onStarted = { _status.value = _status.value.copy(preparing = false) },
                onError = { _status.value = _status.value.copy(preparing = false, error = it) })
        } else {
            ensureEngine()
            if (androidReady) tts?.speak(BuiltInVoiceCatalog.PREVIEW, TextToSpeech.QUEUE_FLUSH, null, "flux-speed-preview")
        }
    }
}
