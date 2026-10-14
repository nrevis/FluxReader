package io.fluxreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderMemoryBudgetTest {
    @Test
    fun imageCacheIsBounded() {
        assertEquals(12 * 1024, ReaderMemoryBudget.embeddedImageCacheKb(64))
        assertEquals(16 * 1024, ReaderMemoryBudget.embeddedImageCacheKb(256))
        assertEquals(32 * 1024, ReaderMemoryBudget.embeddedImageCacheKb(1024))
    }

    @Test
    fun chapterCacheScalesButStaysSmall() {
        assertEquals(2 * 1024, ReaderMemoryBudget.chapterCacheKb(64))
        assertEquals(4 * 1024, ReaderMemoryBudget.chapterCacheKb(256))
        assertEquals(8 * 1024, ReaderMemoryBudget.chapterCacheKb(1024))
        assertTrue(ReaderMemoryBudget.PDF_MAX_BITMAP_PIXELS <= 8_000_000L)
    }

    @Test
    fun libraryCoverCacheAndPrefetchAreBounded() {
        assertEquals(4 * 1024, ReaderMemoryBudget.libraryCoverCacheKb(64))
        assertEquals(8 * 1024, ReaderMemoryBudget.libraryCoverCacheKb(256))
        assertEquals(12 * 1024, ReaderMemoryBudget.libraryCoverCacheKb(1024))
        assertEquals(1, ReaderMemoryBudget.chapterPrefetchRadius(128))
        assertEquals(2, ReaderMemoryBudget.chapterPrefetchRadius(256))
        assertEquals(3, ReaderMemoryBudget.chapterPrefetchRadius(512))
        assertEquals(1, ReaderMemoryBudget.embeddedImageDecodeParallelism(128))
        assertEquals(2, ReaderMemoryBudget.embeddedImageDecodeParallelism(256))
        assertEquals(3, ReaderMemoryBudget.embeddedImageDecodeParallelism(512))
        assertEquals(3_000_000L, ReaderMemoryBudget.pdfMaxBitmapPixels(128))
        assertEquals(5_000_000L, ReaderMemoryBudget.pdfMaxBitmapPixels(256))
        assertEquals(ReaderMemoryBudget.PDF_MAX_BITMAP_PIXELS, ReaderMemoryBudget.pdfMaxBitmapPixels(512))
    }
}
