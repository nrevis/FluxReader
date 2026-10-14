package io.fluxreader.app.reader

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.pdf.PdfDocument
import androidx.pdf.SandboxedPdfLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Lazy PDF text-layer access for Speed Reader.
 *
 * A document is opened only when text is requested. Page text is cached in a small LRU and
 * calls for the same document are serialized so rapid playback cannot pile up service requests.
 */
class PdfTextEngine(context: Context) {
    private val loader = SandboxedPdfLoader(context.applicationContext, Dispatchers.IO)
    private val documents = ConcurrentHashMap<String, PdfDocument>()
    private val pageLocks = ConcurrentHashMap<String, Mutex>()
    private val documentLock = Mutex()
    private val textCache = object : LruCache<String, String>(ReaderMemoryBudget.PDF_TEXT_CACHE_CHARS) {
        override fun sizeOf(key: String, value: String): Int = value.length.coerceAtLeast(1)
    }

    suspend fun pageText(bookId: String, file: File, pageIndex: Int): String {
        require(file.isFile) { "PDF-файл не найден" }
        val key = "$bookId:$pageIndex"
        textCache.get(key)?.let { return it }

        val lock = pageLocks.getOrPut(bookId) { Mutex() }
        return lock.withLock {
            textCache.get(key)?.let { return@withLock it }
            val document = document(bookId, file)
            if (document.pageCount <= 0 || pageIndex !in 0 until document.pageCount) return@withLock ""
            val content = document.getPageContent(pageIndex)
            val text = content?.textContents
                .orEmpty()
                .asSequence()
                .map { it.text.trim() }
                .filter { it.isNotBlank() }
                .joinToString("\n")
                .normalizePdfText()
            textCache.put(key, text)
            text
        }
    }

    suspend fun pageCount(bookId: String, file: File): Int = document(bookId, file).pageCount

    fun close(bookId: String) {
        pageLocks.remove(bookId)
        runCatching { documents.remove(bookId)?.close() }
        textCache.snapshot().keys.filter { it.startsWith("$bookId:") }.forEach(textCache::remove)
    }

    fun trimMemory() {
        documents.values.forEach { document -> runCatching { document.close() } }
        documents.clear()
        pageLocks.clear()
        textCache.trimToSize(ReaderMemoryBudget.PDF_TEXT_CACHE_CHARS / 4)
    }

    fun closeAll() {
        documents.values.forEach { document -> runCatching { document.close() } }
        documents.clear()
        pageLocks.clear()
        textCache.evictAll()
    }

    private suspend fun document(bookId: String, file: File): PdfDocument {
        documents[bookId]?.let { return it }
        return documentLock.withLock {
            documents[bookId]?.let { return@withLock it }
            documents.entries.toList().forEach { (otherBookId, document) ->
                if (otherBookId != bookId) {
                    runCatching { document.close() }
                    documents.remove(otherBookId)
                    pageLocks.remove(otherBookId)
                    textCache.snapshot().keys
                        .filter { it.startsWith("$otherBookId:") }
                        .forEach(textCache::remove)
                }
            }
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            try {
                val uri = Uri.Builder()
                    .scheme("fluxreader")
                    .authority("pdf")
                    .appendPath(bookId)
                    .build()
                loader.openDocument(uri = uri, fileDescriptor = descriptor).also { documents[bookId] = it }
            } catch (error: Throwable) {
                runCatching { descriptor.close() }
                throw error
            }
        }
    }

    private fun String.normalizePdfText(): String =
        lineSequence()
            .map { line -> line.replace(Regex("""[\t ]+"""), " ").trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .trim()
}
