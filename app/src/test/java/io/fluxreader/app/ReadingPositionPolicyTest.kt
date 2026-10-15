package io.fluxreader.app

import io.fluxreader.app.data.*
import io.fluxreader.app.reader.ChapterSpeedOffset
import org.junit.Assert.*
import org.junit.Test

class ReadingPositionPolicyTest {
    private fun book() = BookRecord("book", "Title", "Author", "/test/book.epub", "book.epub", BookFormat.EPUB)

    @Test fun imageBetweenParagraphsResumesAtTheFollowingText() {
        val chapter = ChapterContent(0, "Title", listOf(ReaderBlock.Text("Before."), ReaderBlock.Image("picture"), ReaderBlock.Text("After.")))
        val offset = ChapterSpeedOffset.forBlock(chapter, 1, 0)
        val anchor = ChapterSpeedOffset.toReaderAnchor(chapter, offset)
        assertEquals(2, anchor.blockIndex)
        assertEquals(0, anchor.sourceCharOffset)
    }

    @Test fun normalReadingInNextChapterReplacesAnOldSpeedCheckpoint() {
        var book = ReadingPositionPolicy.speedCheckpoint(book(), 2, 900)
        book = ReadingPositionPolicy.readerCheckpoint(book, 3, 8, 0, 120)
        assertEquals(3, ReadingPositionPolicy.chapter(book))
        assertEquals(ReadingPositionSource.READER, ReadingPositionPolicy.source(book))
        assertEquals(120, book.readerBlockCharOffset)
    }

    @Test fun latestVoiceOrSpeedPositionWinsEvenWhenItIsEarlierInTheBook() {
        var book = ReadingPositionPolicy.readerCheckpoint(book(), 8, 10, 0, 400)
        book = ReadingPositionPolicy.speedCheckpoint(book, 3, 725)
        assertEquals(3, ReadingPositionPolicy.chapter(book))
        assertEquals(ReadingPositionSource.SPEED, ReadingPositionPolicy.source(book))
        assertEquals(725, book.speedOffset)
    }

    @Test fun readerSpeedVoiceReaderRoundTripPreservesTextInTheSameChapter() {
        val chapter = ChapterContent(4, "Heading", listOf(ReaderBlock.Text("  First paragraph.  "), ReaderBlock.Text("  Second paragraph with many words.  ")))
        var book = ReadingPositionPolicy.readerCheckpoint(book(), 4, 1, 0, 10)
        val offset = ChapterSpeedOffset.forBlock(chapter, book.readerBlockIndex, book.readerBlockCharOffset)
        book = ReadingPositionPolicy.speedCheckpoint(book, ReadingPositionPolicy.chapter(book), offset)
        val anchor = ChapterSpeedOffset.toReaderAnchor(chapter, book.speedOffset)
        book = ReadingPositionPolicy.readerCheckpoint(book, ReadingPositionPolicy.chapter(book), anchor.blockIndex, 0, anchor.sourceCharOffset)
        assertEquals(4, book.readerChapter)
        assertEquals(1, book.readerBlockIndex)
        assertEquals(10, book.readerBlockCharOffset)
    }

    @Test fun legacyPositionsPreferNormalReaderWithoutDeletingOldSpeedData() {
        val legacy = book().copy(readerChapter = 8, speedChapter = 2, speedOffset = 725)
        assertEquals(8, ReadingPositionPolicy.chapter(legacy))
        assertEquals(725, legacy.speedOffset)
    }

    @Test fun legacySpeedOnlyPositionIsRetained() {
        assertEquals(5, ReadingPositionPolicy.chapter(book().copy(speedChapter = 5, speedOffset = 725)))
    }

    @Test fun returningToTheBeginningIsAnIntentionalNewPosition() {
        val old = ReadingPositionPolicy.speedCheckpoint(book(), 8, 750)
        val updated = ReadingPositionPolicy.readerCheckpoint(old, 0, 0, 0, 0)
        assertEquals(0, ReadingPositionPolicy.chapter(updated))
        assertEquals(ReadingPositionSource.READER, ReadingPositionPolicy.source(updated))
    }

    @Test fun inactiveModeCannotOverwriteTheSharedPosition() {
        assertFalse(ReadingPositionPolicy.acceptsCheckpoint(false, false, "book", 4, "book", 4))
    }

    @Test fun staleChapterOrBookCallbacksCannotWrite() {
        assertFalse(ReadingPositionPolicy.acceptsCheckpoint(true, false, "book", 5, "book", 4))
        assertFalse(ReadingPositionPolicy.acceptsCheckpoint(true, false, "other", 4, "book", 4))
    }

    @Test fun loadingCannotPublishAnInitialZeroCursor() {
        assertFalse(ReadingPositionPolicy.acceptsCheckpoint(true, true, "book", 0, "book", 0))
        assertTrue(ReadingPositionPolicy.acceptsCheckpoint(true, false, "book", 4, "book", 4))
    }

    @Test fun pdfUsesItsPageUnlessSpeedHasSinceAdvanced() {
        val pdf = book().copy(format = BookFormat.PDF, pdfPage = 7)
        assertEquals(7, ReadingPositionPolicy.chapter(pdf))
        assertEquals(9, ReadingPositionPolicy.chapter(ReadingPositionPolicy.speedCheckpoint(pdf, 9, 100)))
    }
}
