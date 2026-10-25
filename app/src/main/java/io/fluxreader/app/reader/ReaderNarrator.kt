package io.fluxreader.app.reader

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ReaderBlock
import io.fluxreader.app.data.ReaderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.Locale

/** Android voices and a separately downloaded in-process offline backend. */
data class NarrationVoice(val name: String, val language: String, val quality: Int, val needsNetwork: Boolean)

data class NarrationStatus(
    val ready: Boolean = false,
    val playing: Boolean = false,
    val paused: Boolean = false,
    val currentBlock: Int = 0,
    val currentCharOffset: Int = 0,
    val currentText: String = "",
    val voices: List<NarrationVoice> = emptyList(),
    val currentLanguage: String = "ru",
    val error: String? = null,
    val preparing: Boolean = false,
)

class ReaderNarrator(context: Context) {
    private val appContext = context.applicationContext
    private val store = BuiltInVoiceStore.get(appContext)
    private val builtIn = BuiltInSpeechPlayer(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var androidReady = false
    private val main = Handler(Looper.getMainLooper())
    private val _status = MutableStateFlow(NarrationStatus())
    val status: StateFlow<NarrationStatus> = _status.asStateFlow()
    private var engine: TextToSpeech? = null
    private val clipCache = NarrationClipCache(context.applicationContext)
    private var cachedPlayer: MediaPlayer? = null
    private var segments: List<NarrationSegment> = emptyList()
    private var currentIndex = 0
    private var rangeOffset = 0
    private var requestBaseOffset = 0
    private var generation = 0L
    private var activeRequest = ""
    private var currentBookId: String? = null
    private var chapterIndex = -1
    private var isClosing = false
    private var pendingPlay = false
    private var autoNext = false
    private var preferred = ReaderSettings()
    private var speechLocale: Locale = Locale.forLanguageTag("ru-RU")
    var onSegmentPosition: ((Int, Int) -> Unit)? = null
    var onChapterComplete: (() -> Unit)? = null

    init {
        scope.launch {
            store.state.collect {
                if (BuiltInVoiceCatalog.isBuiltIn(preferred.narrationVoiceId)) {
                    if (!it.installed && _status.value.playing) pause()
                    _status.value = _status.value.copy(ready = it.installed && !it.busy)
                }
            }
        }
        engine = TextToSpeech(context.applicationContext) { code ->
            main.post {
                if (isClosing) return@post
                if (code == TextToSpeech.SUCCESS) {
                    val tts = engine ?: return@post
                    tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String) = Unit
                        override fun onDone(utteranceId: String) {
                            main.post { if (utteranceId == activeRequest && !isClosing) advance() }
                        }
                        @Deprecated("Android TTS compatibility")
                        override fun onError(utteranceId: String) {
                            main.post { if (utteranceId == activeRequest && !isClosing) fail("Ошибка речевого движка") }
                        }
                        override fun onError(utteranceId: String, errorCode: Int) {
                            main.post { if (utteranceId == activeRequest && !isClosing) fail("Ошибка синтеза: $errorCode") }
                        }
                        override fun onRangeStart(utteranceId: String, start: Int, end: Int, frame: Int) {
                            main.post range@{
                                if (utteranceId != activeRequest || isClosing) return@range
                                val segment = segments.getOrNull(currentIndex) ?: return@range
                                rangeOffset = (requestBaseOffset + start).coerceIn(0, segment.text.length)
                                _status.value = _status.value.copy(
                                    currentBlock = segment.blockIndex,
                                    currentCharOffset = segment.startOffset + rangeOffset,
                                )
                            }
                        }
                    })
                    tts.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build(),
                    )
                    val voices = tts.voices.orEmpty().map {
                        NarrationVoice(it.name, it.locale.toLanguageTag(), it.quality, it.isNetworkConnectionRequired)
                    }.sortedWith(compareBy<NarrationVoice> { it.needsNetwork }.thenByDescending { it.quality }.thenBy { it.language }.thenBy { it.name })
                    androidReady = true
                    _status.value = _status.value.copy(ready = if (usesBuiltIn()) store.state.value.installed else true, voices = voices,
                        error = if (usesBuiltIn()) _status.value.error else null)
                    configure()
                    if (pendingPlay && !usesBuiltIn()) { pendingPlay = false; play() }
                } else {
                    if (!usesBuiltIn()) fail("Голос Android недоступен. Можно скачать встроенные голоса")
                }
            }
        }
    }

    fun setPreferences(settings: ReaderSettings) {
        val changed = preferred.narrationVoiceId != settings.narrationVoiceId ||
            preferred.narrationRate != settings.narrationRate || preferred.narrationPitch != settings.narrationPitch
        if (changed) { pause(); builtIn.stop() }
        preferred = settings
        _status.value = _status.value.copy(ready = if (usesBuiltIn()) store.state.value.installed && !store.state.value.busy else androidReady,
            error = if (changed) null else _status.value.error, preparing = if (changed) false else _status.value.preparing)
        if (_status.value.ready) configure()
    }

    private fun usesBuiltIn() = BuiltInVoiceCatalog.isBuiltIn(preferred.narrationVoiceId)

    private fun configure() {
        if (usesBuiltIn()) return
        val tts = engine ?: return
        tts.setSpeechRate(preferred.narrationRate.coerceIn(0.6f, 1.4f))
        tts.setPitch(preferred.narrationPitch.coerceIn(0.75f, 1.25f))
        val selected = tts.voices?.firstOrNull { it.name == preferred.narrationVoiceId }
        if (selected != null && selected.locale.language == speechLocale.language) {
            if (tts.setVoice(selected) == TextToSpeech.ERROR) tts.language = speechLocale
        } else {
            tts.language = speechLocale
            val best = tts.voices.orEmpty()
                .filter { it.locale.language == speechLocale.language }
                .sortedWith(compareBy<android.speech.tts.Voice> { it.isNetworkConnectionRequired }.thenByDescending { it.quality })
                .firstOrNull { !it.isNetworkConnectionRequired }
            if (best != null) tts.voice = best
        }
    }

    /** Replacing a chapter is safe; recomposition with the same chapter does not restart audio. */
    fun setDocument(bookId: String?, index: Int, chapter: ChapterContent?, block: Int, charOffset: Int) {
        if (bookId == null || chapter == null) return
        if (currentBookId == bookId && chapterIndex == index) return
        stopEngine()
        currentBookId = bookId
        chapterIndex = index
        // Inspect only a small prefix: building plainText() for the whole chapter would
        // allocate a second copy of potentially megabytes of EPUB/FB2 chapter text.
        val sample = chapter.blocks.asSequence()
            .filterIsInstance<ReaderBlock.Text>()
            .take(10)
            .joinToString(" ") { it.text.take(180) }
            .take(1400)
        val cyrillic = sample.count { it in 'А'..'я' || it == 'ё' || it == 'Ё' }
        val latin = sample.count { it in 'A'..'Z' || it in 'a'..'z' }
        speechLocale = if (cyrillic >= latin) Locale.forLanguageTag("ru-RU") else Locale.US
        segments = NarrationSegments.fromChapter(chapter, speechLocale)
        currentIndex = NarrationSegments.seek(segments, block, charOffset)
        rangeOffset = 0
        _status.value = _status.value.copy(
            playing = false, paused = false, currentLanguage = speechLocale.language,
            currentBlock = segments.getOrNull(currentIndex)?.blockIndex ?: 0,
            currentCharOffset = segments.getOrNull(currentIndex)?.startOffset ?: 0,
            currentText = "", error = if (segments.isEmpty()) "В главе нет текста для озвучивания" else null,
        )
        if (_status.value.ready) configure()
        if (autoNext) { autoNext = false; play() }
    }

    fun play() {
        if (segments.isEmpty()) { fail("Нет текста для озвучивания"); return }
        if (usesBuiltIn() && (!store.state.value.installed || store.state.value.busy)) { fail("Скачайте встроенные голоса в настройках озвучки"); return }
        if (!_status.value.ready) { pendingPlay = true; return }
        val eligible = engine?.voices.orEmpty().filter { it.locale.language == speechLocale.language }
        if (!usesBuiltIn() && preferred.narrationVoiceId.isBlank() && eligible.isNotEmpty() && eligible.none { !it.isNetworkConnectionRequired }) {
            fail("Офлайн-голос для этого языка не найден. Выберите сетевой голос вручную")
            return
        }
        if (_status.value.playing) return
        _status.value = _status.value.copy(playing = true, paused = false, error = null)
        speakCurrent()
    }

    fun preview() {
        if (!_status.value.ready) { fail("Речевой движок ещё не готов"); return }
        stopEngine()
        _status.value = _status.value.copy(playing = false, paused = false)
        val example = if (speechLocale.language == "ru")
            "Ветер стихал. Наступал вечер, и на горизонте зажигались первые огни."
        else "The wind settled. Evening arrived, and the first lights appeared on the horizon."
        if (usesBuiltIn()) {
            _status.value = _status.value.copy(preparing = true)
            builtIn.speak(BuiltInVoiceCatalog.PREVIEW, preferred.narrationVoiceId, preferred.narrationRate, preferred.narrationPitch,
                onStarted = { _status.value = _status.value.copy(preparing = false) },
                onError = ::fail)
            return
        }
        engine?.speak(example, TextToSpeech.QUEUE_FLUSH, null, "flux-preview-${++generation}")
    }

    fun pause() {
        if (!_status.value.playing && !pendingPlay) return
        pendingPlay = false
        stopEngine()
        _status.value = _status.value.copy(playing = false, paused = true)
    }

    fun stop() {
        pendingPlay = false
        autoNext = false
        stopEngine()
        currentIndex = 0
        rangeOffset = 0
        _status.value = _status.value.copy(playing = false, paused = false, currentText = "")
    }

    fun restartAtCurrentReaderPosition(block: Int, charOffset: Int) {
        val wasPlaying = _status.value.playing
        stopEngine()
        currentIndex = NarrationSegments.seek(segments, block, charOffset)
        rangeOffset = 0
        _status.value = _status.value.copy(playing = false, paused = false)
        if (wasPlaying) play()
    }

    fun skipPrevious() {
        val wasPlaying = _status.value.playing
        stopEngine()
        currentIndex = (currentIndex - 1).coerceAtLeast(0)
        rangeOffset = 0
        updatePosition()
        _status.value = _status.value.copy(playing = false, paused = false)
        if (wasPlaying) play()
    }

    fun skipNext() {
        val wasPlaying = _status.value.playing
        stopEngine()
        currentIndex = (currentIndex + 1).coerceAtMost(segments.lastIndex.coerceAtLeast(0))
        rangeOffset = 0
        updatePosition()
        _status.value = _status.value.copy(playing = false, paused = false)
        if (wasPlaying) play()
    }

    private fun speakCurrent() {
        val segment = segments.getOrNull(currentIndex) ?: run { finishChapter(); return }
        val builtInPiece = if (usesBuiltIn()) SpeedSpeechText.next(segment.text, rangeOffset, 260) else null
        val speech = NarrationPronunciationPolicy.forSpeech(
            builtInPiece?.text ?: segment.text.drop(rangeOffset).trimStart(), speechLocale)
        if (speech.isBlank()) { advance(); return }
        val id = "flux-tts-${++generation}"
        activeRequest = id
        requestBaseOffset = rangeOffset
        updatePosition()
        _status.value = _status.value.copy(currentText = speech, playing = true, paused = false)
        if (usesBuiltIn()) {
            _status.value = _status.value.copy(preparing = true)
            val nextPiece = builtInPiece?.let { SpeedSpeechText.next(segment.text, it.end, 260) }
            val next = listOfNotNull(nextPiece?.text) + segments.asSequence().drop(currentIndex + 1).take(1).map {
                NarrationPronunciationPolicy.forSpeech(SpeedSpeechText.next(it.text, 0, 260)?.text.orEmpty(), speechLocale)
            }.filter { it.isNotBlank() && it.length <= 700 }
            builtIn.speak(speech, preferred.narrationVoiceId, preferred.narrationRate, preferred.narrationPitch, next,
                onStarted = { if (activeRequest == id) _status.value = _status.value.copy(preparing = false) },
                onDone = { if (!isClosing && activeRequest == id) {
                    if (builtInPiece != null && builtInPiece.end < segment.text.length) {
                        rangeOffset = builtInPiece.end; speakCurrent()
                    } else advance()
                } },
                onError = { if (activeRequest == id) fail(it) })
            return
        }
        val voice = engine?.voice
        // Never cache network voices. Only audio generated by an on-device TTS engine is stored.
        if (voice != null && !voice.isNetworkConnectionRequired && rangeOffset == 0) {
            val rate = preferred.narrationRate.coerceIn(0.6f, 1.4f)
            val pitch = preferred.narrationPitch.coerceIn(0.75f, 1.25f)
            val cached = clipCache.available(speech, voice.name, speechLocale.toLanguageTag(), rate, pitch)
            if (cached != null && playCached(cached.absolutePath, id)) {
                prefetchNext()
                return
            }
            clipCache.request(speech, voice.name, speechLocale.toLanguageTag(), rate, pitch)
            prefetchNext()
        }
        if (engine?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, id) != TextToSpeech.SUCCESS) {
            fail("Голос недоступен. Проверьте движок и языковые пакеты в настройках Android")
        }
    }

    private fun playCached(path: String, requestId: String): Boolean {
        val player = MediaPlayer()
        return try {
        player.setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
        player.setDataSource(path)
        player.setOnCompletionListener {
            it.release()
            if (cachedPlayer === it) cachedPlayer = null
            main.post { if (!isClosing && activeRequest == requestId) advance() }
        }
        player.prepare()
        cachedPlayer = player
        player.start()
        true
        } catch (e: Exception) {
            runCatching { player.release() }
            if (cachedPlayer === player) cachedPlayer = null
            false
        }
    }

    private fun prefetchNext() {
        val voice = engine?.voice ?: return
        if (voice.isNetworkConnectionRequired) return
        val rate = preferred.narrationRate.coerceIn(0.6f, 1.4f)
        val pitch = preferred.narrationPitch.coerceIn(0.75f, 1.25f)
        for (i in currentIndex + 1 .. minOf(currentIndex + 2, segments.lastIndex)) {
            val text = NarrationPronunciationPolicy.forSpeech(segments[i].text, speechLocale)
            clipCache.request(text, voice.name, speechLocale.toLanguageTag(), rate, pitch)
        }
    }

    private fun updatePosition() {
        val segment = segments.getOrNull(currentIndex) ?: return
        val offset = (segment.startOffset + rangeOffset).coerceAtLeast(0)
        _status.value = _status.value.copy(currentBlock = segment.blockIndex, currentCharOffset = offset)
        onSegmentPosition?.invoke(segment.blockIndex, offset)
    }

    private fun advance() {
        if (!_status.value.playing) return
        val previous = segments.getOrNull(currentIndex)
        currentIndex++
        rangeOffset = 0
        if (currentIndex >= segments.size) {
            finishChapter()
        } else {
            val next = segments[currentIndex]
            val delayMs = if (previous?.blockIndex != next.blockIndex) 270L else 115L
            val requestGeneration = generation
            main.postDelayed({
                if (!isClosing && _status.value.playing && generation == requestGeneration) speakCurrent()
            }, delayMs)
        }
    }

    private fun finishChapter() {
        segments.lastOrNull()?.let { segment ->
            onSegmentPosition?.invoke(segment.blockIndex, segment.startOffset + segment.text.length)
        }
        stopEngine()
        _status.value = _status.value.copy(playing = false, paused = false, currentText = "")
        if (preferred.narrationAutoNextChapter && onChapterComplete != null) {
            autoNext = true
            onChapterComplete?.invoke()
        } else {
            currentIndex = 0
            rangeOffset = 0
        }
    }

    private fun stopEngine() {
        activeRequest = ""
        generation++
        cachedPlayer?.let { runCatching { it.stop(); it.release() } }
        cachedPlayer = null
        engine?.stop()
        builtIn.stop()
        _status.value = _status.value.copy(preparing = false)
    }

    private fun fail(message: String) {
        pendingPlay = false
        stopEngine()
        _status.value = _status.value.copy(playing = false, paused = false, error = message)
    }

    fun close() {
        isClosing = true
        stop()
        engine?.shutdown()
        engine = null
        clipCache.close()
        builtIn.close()
        scope.cancel()
    }
}
