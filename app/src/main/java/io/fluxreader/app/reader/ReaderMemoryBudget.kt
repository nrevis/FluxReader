package io.fluxreader.app.reader

/** Memory budgets used by the hot reader path. Values are intentionally bounded. */
internal object ReaderMemoryBudget {
    private const val MIN_IMAGE_CACHE_KB = 12 * 1024
    private const val MAX_IMAGE_CACHE_KB = 32 * 1024
    private const val MIN_CHAPTER_CACHE_KB = 2 * 1024
    private const val MAX_CHAPTER_CACHE_KB = 8 * 1024
    private const val MIN_LIBRARY_COVER_CACHE_KB = 4 * 1024
    private const val MAX_LIBRARY_COVER_CACHE_KB = 12 * 1024

    const val OUTLINE_CACHE_BOOKS = 4
    const val PDF_TEXT_CACHE_CHARS = 512 * 1024
    const val OCR_TEXT_CACHE_CHARS = 384 * 1024
    const val PDF_MAX_BITMAP_PIXELS = 8_000_000L

    fun embeddedImageCacheKb(memoryClassMb: Int): Int =
        ((memoryClassMb.coerceAtLeast(64) * 1024) / 16)
            .coerceIn(MIN_IMAGE_CACHE_KB, MAX_IMAGE_CACHE_KB)

    fun chapterCacheKb(memoryClassMb: Int): Int =
        ((memoryClassMb.coerceAtLeast(64) * 1024) / 64)
            .coerceIn(MIN_CHAPTER_CACHE_KB, MAX_CHAPTER_CACHE_KB)

    fun libraryCoverCacheKb(memoryClassMb: Int): Int =
        ((memoryClassMb.coerceAtLeast(64) * 1024) / 32)
            .coerceIn(MIN_LIBRARY_COVER_CACHE_KB, MAX_LIBRARY_COVER_CACHE_KB)

    fun chapterPrefetchRadius(memoryClassMb: Int): Int = when {
        memoryClassMb < 192 -> 1
        memoryClassMb < 512 -> 2
        else -> 3
    }

    fun embeddedImageDecodeParallelism(memoryClassMb: Int): Int = when {
        memoryClassMb < 192 -> 1
        memoryClassMb < 512 -> 2
        else -> 3
    }

    fun pdfMaxBitmapPixels(memoryClassMb: Int): Long = when {
        memoryClassMb < 192 -> 3_000_000L
        memoryClassMb < 512 -> 5_000_000L
        else -> PDF_MAX_BITMAP_PIXELS
    }
}
