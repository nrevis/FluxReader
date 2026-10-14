package io.fluxreader.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderProgressSeekTest {
    @Test fun mapsPositionInsideChapter() {
        assertEquals(ReaderProgressSeek.Target(2, 0.5f), ReaderProgressSeek.target(0.25f, 10))
    }
    @Test fun boundariesClamp() {
        assertEquals(ReaderProgressSeek.Target(0, 0f), ReaderProgressSeek.target(-5f, 10))
        assertEquals(ReaderProgressSeek.Target(9, 1f), ReaderProgressSeek.target(1f, 10))
        assertEquals(ReaderProgressSeek.Target(0, 0f), ReaderProgressSeek.target(Float.NaN, 0))
    }
    @Test fun indexesClampToLastValidItem() {
        assertEquals(0, ReaderProgressSeek.itemIndex(-2f, 50))
        assertEquals(25, ReaderProgressSeek.itemIndex(0.5f, 50))
        assertEquals(49, ReaderProgressSeek.itemIndex(1f, 50))
        assertEquals(0, ReaderProgressSeek.itemIndex(0.5f, 0))
    }
}
