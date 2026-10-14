package io.fluxreader.app.reader

import android.app.ActivityManager
import android.content.Context
import android.util.LruCache
import io.fluxreader.app.data.BookFormat
import io.fluxreader.app.data.BookOutline
import io.fluxreader.app.data.BookRecord
import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ChapterRef
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

class BookContentRepository(context: Context) {
    private companion object {
        const val PARSE_LOCK_STRIPES = 64
    }

    private val cacheRoot = File(context.cacheDir, "reader-v2").apply { mkdirs() }
    private val memoryClassMb = context.getSystemService(ActivityManager::class.java)?.memoryClass ?: 256
    private val chapterCacheMaxKb = ReaderMemoryBudget.chapterCacheKb(memoryClassMb)
    private val outlineCache = object : LruCache<String, BookOutline>(ReaderMemoryBudget.OUTLINE_CACHE_BOOKS) {}
    private val chapterCache = object : LruCache<String, ChapterContent>(chapterCacheMaxKb) {
        override fun sizeOf(key: String, value: ChapterContent): Int = estimateChapterKb(value)
    }
    private val parsedDiskCache = ParsedBookDiskCache(File(cacheRoot, "parsed-v1"))
    private val parseLocks = Array(PARSE_LOCK_STRIPES) { Mutex() }
    private val pdfRenderMutex = Mutex()
    private val pdfMaxBitmapPixels = ReaderMemoryBudget.pdfMaxBitmapPixels(memoryClassMb)
    private val pdfTextEngine = PdfTextEngine(context.applicationContext)
    private val ocrEngine = OcrEngine(context.applicationContext)

    suspend fun outline(book: BookRecord, refresh: Boolean = false): BookOutline = withContext(Dispatchers.IO) {
        val file = File(book.filePath)
        require(file.isFile) { "Файл книги не найден" }
        val sourceToken = parsedDiskCache.sourceToken(file, book.contentHash)
        val cacheKey = "${book.id}:$sourceToken"
        if (refresh) {
            outlineCache.snapshot().keys.filter { it.startsWith("${book.id}:") }.forEach(outlineCache::remove)
            chapterCache.snapshot().keys.filter { it.startsWith("${book.id}:") }.forEach(chapterCache::remove)
            parsedDiskCache.clear(book.id)
        } else {
            outlineCache.get(cacheKey)?.let { return@withContext it }
        }

        parseLock(cacheKey).withLock {
                if (!refresh) {
                    outlineCache.get(cacheKey)?.let { return@withLock it }
                    if (book.format == BookFormat.EPUB || book.format == BookFormat.FB2) {
                        parsedDiskCache.readOutline(book.id, sourceToken)?.let { cached ->
                            outlineCache.put(cacheKey, cached)
                            return@withLock cached
                        }
                    }
                }

                val loaded = when (book.format) {
                    BookFormat.EPUB -> EpubParser.outline(file)
                    BookFormat.FB2 -> Fb2Parser.prepare(file, book.id, cacheRoot)
                    BookFormat.TXT -> TxtParser.outline(file)
                    BookFormat.PDF -> BookOutline(book.title, book.author, emptyList())
                }
                outlineCache.put(cacheKey, loaded)
            if (book.format == BookFormat.EPUB || book.format == BookFormat.FB2) {
                runCatching { parsedDiskCache.writeOutline(book.id, sourceToken, loaded) }
            }
            loaded
        }
    }

    suspend fun chapter(book: BookRecord, ref: ChapterRef): ChapterContent = withContext(Dispatchers.IO) {
        val file = File(book.filePath)
        require(file.isFile) { "Файл книги не найден" }
        val sourceToken = parsedDiskCache.sourceToken(file, book.contentHash)
        val cacheKey = "${book.id}:$sourceToken:${ref.index}:${ref.sourceRef}"
        chapterCache.get(cacheKey)?.let { return@withContext it }

        parseLock(cacheKey).withLock {
                chapterCache.get(cacheKey)?.let { return@withLock it }
                if (book.format == BookFormat.EPUB || book.format == BookFormat.FB2) {
                    parsedDiskCache.readChapter(book.id, sourceToken, ref)?.let { cached ->
                        chapterCache.put(cacheKey, cached)
                        return@withLock cached
                    }
                }

                val loaded = when (book.format) {
                    BookFormat.EPUB -> EpubParser.loadChapter(file, ref)
                    BookFormat.FB2 -> Fb2Parser.loadChapter(book.id, cacheRoot, ref)
                    BookFormat.TXT -> TxtParser.loadChapter(file)
                    BookFormat.PDF -> error("PDF использует отдельный reader")
                }
                chapterCache.put(cacheKey, loaded)
            if (book.format == BookFormat.EPUB || book.format == BookFormat.FB2) {
                runCatching { parsedDiskCache.writeChapter(book.id, sourceToken, ref, loaded) }
            }
            loaded
        }
    }


    /**
     * Warms the small chapter LRU around the current chapter. Navigation keeps priority: the
     * caller owns cancellation and failures in neighbouring chapters never break the open page.
     */
    suspend fun prefetchAdjacentChapters(
        book: BookRecord,
        outline: BookOutline,
        centerIndex: Int,
        radius: Int = 2,
    ) {
        if (book.format == BookFormat.PDF || outline.chapters.isEmpty()) return
        val center = centerIndex.coerceIn(0, outline.chapters.lastIndex)
        val safeRadius = radius.coerceIn(1, 3)
        val candidates = buildList {
            for (distance in 1..safeRadius) {
                val next = center + distance
                val previous = center - distance
                if (next <= outline.chapters.lastIndex) add(next)
                if (previous >= 0) add(previous)
            }
        }
        for (index in candidates) {
            currentCoroutineContext().ensureActive()
            try {
                chapter(book, outline.chapters[index])
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                // Prefetch is opportunistic. The normal chapter loader reports a real failure if
                // the user actually navigates to this chapter later.
            }
        }
    }


    suspend fun speedOutline(book: BookRecord): BookOutline = withContext(Dispatchers.IO) {
        if (book.format != BookFormat.PDF) return@withContext outline(book)
        val count = PdfRendererEngine.pageCount(File(book.filePath))
        require(count > 0) { "PDF не содержит страниц" }
        BookOutline(
            title = book.title,
            author = book.author,
            chapters = List(count) { index ->
                ChapterRef(index = index, title = "Страница ${index + 1}", sourceRef = index.toString())
            },
        )
    }

    suspend fun speedDocument(
        book: BookRecord,
        ref: ChapterRef,
        allowOcr: Boolean = false,
        ocrLanguages: Set<String> = setOf("rus", "eng"),
        allowMlKitFallback: Boolean = true,
    ): SpeedReaderDocument = withContext(Dispatchers.IO) {
        when (book.format) {
            BookFormat.PDF -> SpeedReaderDocument(
                text = speedText(
                    book = book,
                    ref = ref,
                    allowOcr = allowOcr,
                    ocrLanguages = ocrLanguages,
                    allowMlKitFallback = allowMlKitFallback,
                ),
            )
            else -> SpeedReaderDocumentBuilder.fromChapter(chapter(book, ref))
        }
    }

    suspend fun speedText(
        book: BookRecord,
        ref: ChapterRef,
        allowOcr: Boolean = false,
        ocrLanguages: Set<String> = setOf("rus", "eng"),
        allowMlKitFallback: Boolean = true,
    ): String = withContext(Dispatchers.IO) {
        when (book.format) {
            BookFormat.PDF -> {
                val file = File(book.filePath)
                val embedded = pdfTextEngine.pageText(book.id, file, ref.index)
                if (embedded.isNotBlank() || !allowOcr) {
                    embedded
                } else {
                    val rendered = PdfRendererEngine.render(file, ref.index, 1800, pdfMaxBitmapPixels)
                    try {
                        ocrEngine.recognizePage(
                            bookId = book.id,
                            pageIndex = ref.index,
                            bitmap = rendered.bitmap,
                            requestedLanguages = ocrLanguages,
                            allowMlKitFallback = allowMlKitFallback,
                        )
                    } finally {
                        if (!rendered.bitmap.isRecycled) rendered.bitmap.recycle()
                    }
                }
            }
            else -> chapter(book, ref).plainText()
        }
    }

    fun installedOcrLanguages(): List<String> = ocrEngine.installedLanguages()

    fun ocrBackendDescription(requested: Set<String>): String = ocrEngine.backendDescription(requested)

    suspend fun image(book: BookRecord, ref: String): ByteArray? = withContext(Dispatchers.IO) {
        val file = File(book.filePath)
        when (book.format) {
            BookFormat.EPUB -> EpubParser.loadImage(file, ref)
            BookFormat.FB2 -> Fb2Parser.loadImage(book.id, cacheRoot, ref)
            else -> null
        }
    }

    suspend fun pdfPageCount(book: BookRecord): Int = withContext(Dispatchers.IO) {
        PdfRendererEngine.pageCount(File(book.filePath))
    }

    suspend fun renderPdf(book: BookRecord, pageIndex: Int, widthPx: Int): PdfRendererEngine.RenderedPage =
        withContext(Dispatchers.IO) {
            pdfRenderMutex.withLock {
                currentCoroutineContext().ensureActive()
                val rendered = PdfRendererEngine.render(
                    file = File(book.filePath),
                    pageIndex = pageIndex,
                    requestedWidthPx = widthPx,
                    maxBitmapPixels = pdfMaxBitmapPixels,
                )
                try {
                    // PdfRenderer.render() is blocking native work. Cancellation may arrive while
                    // it is running; check immediately afterwards and release the now-stale bitmap
                    // before the cancelled coroutine can lose ownership of it.
                    currentCoroutineContext().ensureActive()
                    rendered
                } catch (cancelled: CancellationException) {
                    if (!rendered.bitmap.isRecycled) rendered.bitmap.recycle()
                    throw cancelled
                }
            }
        }

    fun clear(bookId: String) {
        outlineCache.snapshot().keys.filter { it.startsWith("$bookId:") }.forEach(outlineCache::remove)
        chapterCache.snapshot().keys.filter { it.startsWith("$bookId:") }.forEach(chapterCache::remove)
        parsedDiskCache.clear(bookId)
        File(cacheRoot, "fb2/$bookId").deleteRecursively()
        pdfTextEngine.close(bookId)
        ocrEngine.clear(bookId)
    }

    fun trimSessionCaches() {
        // Library cards do not need parsed chapter bodies. Keep at most a tiny warm tail so a
        // reopen stays cheap while releasing most Reader RAM immediately after navigation back.
        outlineCache.trimToSize(1)
        chapterCache.trimToSize((chapterCacheMaxKb / 4).coerceAtLeast(512))
        pdfTextEngine.trimMemory()
        ocrEngine.trimMemory()
    }

    fun close() {
        outlineCache.evictAll()
        chapterCache.evictAll()
        pdfTextEngine.closeAll()
        ocrEngine.close()
    }

    private fun parseLock(key: String): Mutex {
        val index = (key.hashCode() and Int.MAX_VALUE) % parseLocks.size
        return parseLocks[index]
    }

    private fun estimateChapterKb(chapter: ChapterContent): Int {
        var bytes = 32 * 1024L
        chapter.title.let { bytes += it.length * 2L }
        chapter.blocks.forEach { block ->
            bytes += 64L
            when (block) {
                is io.fluxreader.app.data.ReaderBlock.Text -> {
                    bytes += block.text.length * 2L
                    bytes += block.listMarker.length * 2L
                    bytes += block.links.size * 96L
                    bytes += block.anchors.sumOf { it.length * 2L + 24L }
                }
                is io.fluxreader.app.data.ReaderBlock.Image -> {
                    bytes += (block.ref.length + block.alt.length) * 2L + 128L
                    bytes += block.anchors.sumOf { it.length * 2L + 24L }
                }
            }
        }
        return ((bytes + 1023L) / 1024L).coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
    }
}
