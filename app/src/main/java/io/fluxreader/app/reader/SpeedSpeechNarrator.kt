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

/** Speech and visual RSVP use the same chapter text and UTF-16 cursor.
 * onRangeStart provides accurate word boundaries if the installed engine supports it.
 * Never starts classic WPM playback at the same time as TTS. */
class SpeedSpeechNarrator(context: Context) {
    private val appContext = context.applicationContext
    data class AvailableVoice(val id: String, val language: String, val quality: Int)
    data class Status(
        val voices: List<AvailableVoice> = emptyList(),
        val ready: Boolean = false,
        val playing: Boolean = false,
        val offset: Int = 0,
        val error: String? = null,
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

    private fun ensureEngine() {
        if (tts != null) return
        tts = TextToSpeech(appContext) { outcome ->
            main.post {
                val engine = tts ?: return@post
                if (outcome != TextToSpeech.SUCCESS) {
                    _status.value = _status.value.copy(error = "Не удалось запустить офлайн-озвучивание")
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
                    override fun onError(utteranceId: String) { main.post {
                        if (utteranceId == activeUtterance) {
                            _status.value = _status.value.copy(playing = false, error = "Ошибка воспроизведения TTS")
                        }
                    } }
                })
                _status.value = _status.value.copy(
                    ready = true,
                    error = null,
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
        rate = settings.narrationRate
        pitch = settings.narrationPitch
        voiceId = settings.narrationVoiceId
        if (changes && _status.value.playing) pause()
        if (_status.value.ready) configureVoice()
    }

    private fun configureVoice() {
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
        if (!_status.value.ready) {
            pendingPlay = true
            ensureEngine()
            return
        }
        speakFrom(at)
    }

    private fun speakFrom(at: Int) {
        val piece = SpeedSpeechText.next(source, at)
        if (piece == null) {
            currentPiece = null
            activeUtterance = ""
            _status.value = _status.value.copy(playing = false)
            onFinished?.invoke()
            return
        }
        val engine = tts ?: return
        currentPiece = piece
        activeUtterance = "speed-${++generation}"
        _status.value = _status.value.copy(playing = true, offset = piece.start)
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
        _status.value = _status.value.copy(playing = false, offset = offset.coerceAtLeast(0), error = null)
    }

    fun pause() {
        val wasSpeaking = _status.value.playing
        val wasPending = pendingPlay
        pendingPlay = false
        activeUtterance = ""
        generation++
        tts?.stop()
        _status.value = _status.value.copy(playing = false)
        // An idle TTS instance has an initial cursor of zero. Never publish that
        // cursor when Compose disposes/recreates the narrator or changes chapters.
        if (SpeedSpeechCheckpointPolicy.commitOnPause(wasSpeaking, wasPending)) {
            onSettledPosition?.invoke(_status.value.offset)
        }
    }

    fun close() {
        pendingPlay = false
        activeUtterance = ""
        tts?.stop()
        tts?.shutdown()
        tts = null
        onSettledPosition = null
        onFinished = null
    }
}
