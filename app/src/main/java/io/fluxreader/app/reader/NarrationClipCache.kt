package io.fluxreader.app.reader

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.ArrayDeque
import java.util.Locale

/**
 * Optional offline-only WAV cache. A separate Android TTS instance pre-generates two phrases
 * while the primary narrator keeps playing. Works with locally installed sherpa-onnx Android
 * TTS engines as well as other offline voices. No cloud requests are issued by this cache.
 */
internal class NarrationClipCache(context: Context) {
    private data class Job(val key: String, val text: String, val voice: String,
                           val language: String, val rate: Float, val pitch: Float)
    private val main = Handler(Looper.getMainLooper())
    private val appContext = context.applicationContext
    private val directory = File(context.cacheDir, "narration-wav-v1").apply { mkdirs() }
    private val queued = ArrayDeque<Job>()
    private val inFlight = mutableSetOf<String>()
    private var current: Job? = null
    private var ready = false
    private var closed = false
    private var tts: TextToSpeech? = null

    /** Do not start an additional TTS engine merely because a user opened a book. */
    private fun ensureEngine() {
        if (tts != null || closed) return
        tts = TextToSpeech(appContext) { result ->
            main.post {
                if (closed) return@post
                ready = result == TextToSpeech.SUCCESS
                if (ready) {
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String) = Unit
                        override fun onDone(utteranceId: String) = main.post { complete(utteranceId, true) }.let { Unit }
                        @Deprecated("Android compatibility")
                        override fun onError(utteranceId: String) = main.post { complete(utteranceId, false) }.let { Unit }
                        override fun onError(utteranceId: String, errorCode: Int) = main.post { complete(utteranceId, false) }.let { Unit }
                    })
                    processNext()
                }
            }
        }
    }

    fun fileFor(text: String, voice: String, language: String, rate: Float, pitch: Float): File {
        val name = NarrationCachePolicy.key(text, voice, language, rate, pitch)
        return File(directory, "$name.wav")
    }

    fun available(text: String, voice: String, language: String, rate: Float, pitch: Float): File? {
        val file = fileFor(text, voice, language, rate, pitch)
        if (file.isFile && file.length() > 44L) {
            file.setLastModified(System.currentTimeMillis())
            return file
        }
        return null
    }

    fun request(text: String, voice: String, language: String, rate: Float, pitch: Float) {
        if (closed || text.isBlank()) return
        val file = fileFor(text, voice, language, rate, pitch)
        if (file.isFile && file.length() > 44L) return
        val key = file.nameWithoutExtension
        if (!inFlight.add(key)) return
        // A user can scrub quickly: never accumulate a queue of obsolete phrases.
        while (queued.size >= 6) inFlight.remove(queued.removeFirst().key)
        queued.add(Job(key, text, voice, language, rate, pitch))
        ensureEngine()
        processNext()
    }

    private fun processNext() {
        if (!ready || closed || current != null || queued.isEmpty()) return
        val job = queued.removeFirst()
        val e = tts ?: return
        val voice = e.voices.orEmpty().firstOrNull { it.name == job.voice &&
            !it.isNetworkConnectionRequired && it.locale.language == Locale.forLanguageTag(job.language).language }
        if (voice == null || e.setVoice(voice) == TextToSpeech.ERROR) {
            inFlight.remove(job.key)
            processNext()
            return
        }
        e.setSpeechRate(job.rate)
        e.setPitch(job.pitch)
        current = job
        val part = File(directory, "${job.key}.part")
        part.delete()
        val result = e.synthesizeToFile(job.text, Bundle(), part, job.key)
        if (result == TextToSpeech.ERROR) complete(job.key, false)
    }

    private fun complete(key: String, success: Boolean) {
        val job = current ?: return
        if (job.key != key) return
        val part = File(directory, "$key.part")
        val target = File(directory, "$key.wav")
        if (success && part.isFile && part.length() > 44L) {
            if (!part.renameTo(target)) part.delete()
            trim()
        } else part.delete()
        inFlight.remove(key)
        current = null
        processNext()
    }

    private fun trim() {
        val files = directory.listFiles { f -> f.extension == "wav" }?.sortedByDescending { it.lastModified() } ?: return
        var sum = 0L
        for (file in files) {
            sum += file.length()
            if (sum > 64L * 1024L * 1024L) file.delete()
        }
    }

    fun close() {
        closed = true
        queued.clear()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
