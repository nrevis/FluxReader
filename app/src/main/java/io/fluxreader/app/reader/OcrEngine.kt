package io.fluxreader.app.reader

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Offline OCR used only when a PDF page has no embedded text layer.
 *
 * Preferred backend: local Tesseract traineddata (rus/ukr/eng).
 * Zero-setup fallback: bundled ML Kit Latin recognizer.
 */
class OcrEngine(context: Context) {
    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private val mlKit = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val dataRoot = File(appContext.filesDir, "tesseract").apply { mkdirs() }
    private val tessdataDir = File(dataRoot, "tessdata").apply { mkdirs() }
    private val cache = object : LruCache<String, String>(ReaderMemoryBudget.OCR_TEXT_CACHE_CHARS) {
        override fun sizeOf(key: String, value: String): Int = value.length.coerceAtLeast(1)
    }
    private var tessApi: TessBaseAPI? = null
    private var tessLanguages: String = ""
    @Volatile private var closed = false

    fun installedLanguages(): List<String> {
        val bundled = bundledLanguageNames()
        return SUPPORTED_LANGUAGES.filter { language ->
            File(tessdataDir, "$language.traineddata").isFile || "$language.traineddata" in bundled
        }
    }

    fun backendDescription(requested: Set<String>): String {
        val local = requested.filter { it in installedLanguages() }
        return if (local.isNotEmpty()) "Tesseract · ${local.joinToString("+")}" else "ML Kit · Latin"
    }

    suspend fun recognizePage(
        bookId: String,
        pageIndex: Int,
        bitmap: Bitmap,
        requestedLanguages: Set<String>,
        allowMlKitFallback: Boolean,
    ): String {
        check(!closed) { "OCR engine закрыт" }
        val normalizedRequested = requestedLanguages.filter { it in SUPPORTED_LANGUAGES }.toSortedSet()
        val signature = normalizedRequested.joinToString("+") + ":" + allowMlKitFallback + ":" + installedSignature()
        val key = "$bookId:$pageIndex:$signature"
        cache.get(key)?.let { return it }

        val text = mutex.withLock {
            cache.get(key)?.let { return@withLock it }
            ensureBundledLanguagePacks(normalizedRequested)
            val available = normalizedRequested.filter { File(tessdataDir, "$it.traineddata").isFile }
            val recognized = when {
                available.isNotEmpty() -> runCatching { recognizeWithTesseract(bitmap, available) }
                    .getOrNull()
                    .orEmpty()
                    .takeIf { it.isNotBlank() }
                    ?: if (allowMlKitFallback) recognizeWithMlKit(bitmap) else ""
                allowMlKitFallback -> recognizeWithMlKit(bitmap)
                else -> ""
            }.normalizeOcrText()
            cache.put(key, recognized)
            recognized
        }
        return text
    }

    fun clear(bookId: String) {
        cache.snapshot().keys.filter { it.startsWith("$bookId:") }.forEach(cache::remove)
    }

    fun trimMemory() {
        cache.trimToSize(ReaderMemoryBudget.OCR_TEXT_CACHE_CHARS / 4)
        runCatching { tessApi?.recycle() }
        tessApi = null
        tessLanguages = ""
    }

    fun close() {
        if (closed) return
        closed = true
        cache.evictAll()
        runCatching { mlKit.close() }
        runCatching { tessApi?.recycle() }
        tessApi = null
    }

    private suspend fun recognizeWithTesseract(bitmap: Bitmap, languages: List<String>): String =
        withContext(Dispatchers.Default) {
            val langString = languages.joinToString("+")
            val api = if (tessApi == null || tessLanguages != langString) {
                tessApi?.recycle()
                TessBaseAPI().also { created ->
                    val initialized = created.init(dataRoot.absolutePath, langString, TessBaseAPI.OEM_LSTM_ONLY)
                    check(initialized) { "Не удалось инициализировать Tesseract: $langString" }
                    created.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO)
                    tessApi = created
                    tessLanguages = langString
                }
            } else tessApi!!
            api.setImage(bitmap)
            try {
                api.getUTF8Text().orEmpty()
            } finally {
                api.clear()
            }
        }

    private suspend fun recognizeWithMlKit(bitmap: Bitmap): String {
        val result = mlKit.process(InputImage.fromBitmap(bitmap, 0)).awaitResult()
        return result.text.orEmpty()
    }

    private suspend fun ensureBundledLanguagePacks(requested: Set<String>) = withContext(Dispatchers.IO) {
        val names = bundledLanguageNames()
        requested.asSequence()
            .filter { it in SUPPORTED_LANGUAGES }
            .map { "$it.traineddata" }
            .filter { it in names }
            .forEach { name ->
                val target = File(tessdataDir, name)
                if (target.isFile && target.length() >= MIN_TRAINEDDATA_BYTES) return@forEach
                val temporary = File(tessdataDir, ".$name.copying")
                runCatching {
                    appContext.assets.open("tessdata/$name").use { input ->
                        temporary.outputStream().buffered().use { output -> input.copyTo(output) }
                    }
                    check(temporary.length() >= MIN_TRAINEDDATA_BYTES) { "$name повреждён" }
                    if (target.exists() && !target.delete()) error("Не удалось заменить $name")
                    check(temporary.renameTo(target)) { "Не удалось сохранить $name" }
                }.onFailure {
                    runCatching { temporary.delete() }
                    throw it
                }
            }
    }

    private fun bundledLanguageNames(): Set<String> = runCatching {
        appContext.assets.list("tessdata")?.toSet().orEmpty()
    }.getOrDefault(emptySet())

    private fun installedSignature(): String = installedLanguages().sorted().joinToString("+")

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
        addOnCanceledListener { continuation.resumeWithException(IllegalStateException("OCR отменён")) }
    }

    private fun String.normalizeOcrText(): String =
        replace('\u0000', ' ')
            .lineSequence()
            .map { line -> line.replace(Regex("""[\t ]+"""), " ").trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()

    companion object {
        val SUPPORTED_LANGUAGES = listOf("rus", "ukr", "eng")
        private const val MIN_TRAINEDDATA_BYTES = 100_000L
    }
}
