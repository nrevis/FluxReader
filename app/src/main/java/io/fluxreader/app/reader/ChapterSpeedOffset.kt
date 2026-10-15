package io.fluxreader.app.reader

import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ReaderBlock

/** Maps a visible text-block character back to ChapterContent.plainText(). */
data class ReaderTextAnchor(val blockIndex: Int, val sourceCharOffset: Int)

object ChapterSpeedOffset {
    fun forBlock(chapter: ChapterContent, blockIndex: Int, sourceCharOffset: Int): Int {
        var offset = 0
        if (chapter.title.isNotBlank()) offset += chapter.title.length + 2

        chapter.blocks.forEachIndexed { index, block ->
            if (block !is ReaderBlock.Text || block.text.isBlank()) return@forEachIndexed
            val raw = block.text
            val first = raw.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) 0 else it }
            val lastExclusive = raw.indexOfLast { !it.isWhitespace() }.let { if (it < first) first else it + 1 }
            val trimmedLength = (lastExclusive - first).coerceAtLeast(0)
            if (index >= blockIndex) {
                // A visible image/empty block resumes at the following text, not chapter end.
                val local = if (index == blockIndex) (sourceCharOffset - first).coerceIn(0, trimmedLength) else 0
                return offset + local
            }
            offset += trimmedLength + 2
        }
        return offset.coerceAtLeast(0)
    }

    /** Maps a ChapterContent.plainText()/SpeedReaderDocument offset back to a visible Reader text block. */
    fun toReaderAnchor(chapter: ChapterContent, chapterOffset: Int): ReaderTextAnchor {
        val textBlocks = chapter.blocks.withIndex().filter { it.value is ReaderBlock.Text && (it.value as ReaderBlock.Text).text.isNotBlank() }
        if (textBlocks.isEmpty()) return ReaderTextAnchor(0, 0)

        var remaining = chapterOffset.coerceAtLeast(0)
        if (chapter.title.isNotBlank()) {
            val titleSpan = chapter.title.length + 2
            if (remaining < titleSpan) {
                val first = textBlocks.first()
                val raw = (first.value as ReaderBlock.Text).text
                val firstVisible = raw.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) 0 else it }
                return ReaderTextAnchor(first.index, firstVisible)
            }
            remaining -= titleSpan
        }

        textBlocks.forEachIndexed { position, indexed ->
            val raw = (indexed.value as ReaderBlock.Text).text
            val first = raw.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) 0 else it }
            val lastExclusive = raw.indexOfLast { !it.isWhitespace() }.let { if (it < first) first else it + 1 }
            val trimmedLength = (lastExclusive - first).coerceAtLeast(0)
            if (remaining <= trimmedLength) {
                return ReaderTextAnchor(indexed.index, first + remaining.coerceIn(0, trimmedLength))
            }
            remaining -= trimmedLength
            if (position != textBlocks.lastIndex) {
                if (remaining <= 2) {
                    val next = textBlocks[position + 1]
                    val nextRaw = (next.value as ReaderBlock.Text).text
                    val nextFirst = nextRaw.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) 0 else it }
                    return ReaderTextAnchor(next.index, nextFirst)
                }
                remaining -= 2
            }
        }

        val last = textBlocks.last()
        val raw = (last.value as ReaderBlock.Text).text
        val lastExclusive = raw.indexOfLast { !it.isWhitespace() }.let { if (it < 0) 0 else it + 1 }
        return ReaderTextAnchor(last.index, lastExclusive)
    }

}
