package io.fluxreader.app.data

enum class ReadingPositionSource { READER, SPEED, PDF }

/** The latest active mode owns resume; distance through the book is not a timestamp. */
object ReadingPositionPolicy {
    fun readerCheckpoint(book: BookRecord, chapter: Int, block: Int, pixels: Int, char: Int): BookRecord =
        book.copy(readerChapter = chapter.coerceAtLeast(0), readerBlockIndex = block.coerceAtLeast(0),
            readerBlockOffsetPx = pixels.coerceAtLeast(0), readerBlockCharOffset = char.coerceAtLeast(0),
            readingPositionSource = ReadingPositionSource.READER)

    fun speedCheckpoint(book: BookRecord, chapter: Int, offset: Int): BookRecord =
        book.copy(speedChapter = chapter.coerceAtLeast(0), speedOffset = offset.coerceAtLeast(0),
            readingPositionSource = ReadingPositionSource.SPEED)

    fun source(book: BookRecord): ReadingPositionSource = book.readingPositionSource ?: when {
        book.format == BookFormat.PDF -> ReadingPositionSource.PDF
        book.readerChapter > 0 || book.readerBlockIndex > 0 || book.readerBlockCharOffset > 0 ||
            book.readerBlockOffsetPx > 0 -> ReadingPositionSource.READER
        book.speedChapter > 0 || book.speedOffset > 0 -> ReadingPositionSource.SPEED
        else -> ReadingPositionSource.READER
    }

    fun chapter(book: BookRecord): Int = when (source(book)) {
        ReadingPositionSource.READER -> book.readerChapter
        ReadingPositionSource.SPEED -> book.speedChapter
        ReadingPositionSource.PDF -> book.pdfPage
    }.coerceAtLeast(0)

    fun acceptsCheckpoint(
        active: Boolean, loading: Boolean,
        activeBookId: String?, activeChapter: Int,
        writerBookId: String?, writerChapter: Int,
    ): Boolean = active && !loading && writerBookId != null &&
        activeBookId == writerBookId && activeChapter == writerChapter
}
