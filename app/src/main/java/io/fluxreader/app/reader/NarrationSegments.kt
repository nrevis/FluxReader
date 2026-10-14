package io.fluxreader.app.reader

import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ReaderBlock
import java.text.BreakIterator
import java.util.Locale

/** Small bounded utterances. Offsets always refer to original block text (UTF-16). */
internal data class NarrationSegment(
    val blockIndex: Int,
    val startOffset: Int,
    val text: String,
)

internal object NarrationSegments {
    // Keep short paragraphs together where possible so speech rhythm is less staccato.
    // Offsets remain in the unmodified text and the bound is below Android's TTS input limit.
    private const val MAX_CHARS = 700

    fun fromChapter(chapter: ChapterContent, locale: Locale): List<NarrationSegment> = buildList {
        chapter.blocks.forEachIndexed { blockIndex, block ->
            val original = (block as? ReaderBlock.Text)?.text ?: return@forEachIndexed
            if (original.isBlank()) return@forEachIndexed
            val iterator = BreakIterator.getSentenceInstance(locale)
            iterator.setText(original)
            var pendingStart = -1
            var start = iterator.first()
            var end = iterator.next()
            while (end != BreakIterator.DONE) {
                if (pendingStart < 0) pendingStart = start
                if (end - pendingStart > MAX_CHARS) {
                    // Flush complete prior sentences, not one utterance per sentence.
                    if (start > pendingStart) addPieces(original, blockIndex, pendingStart, start)
                    if (end - start > MAX_CHARS) {
                        addPieces(original, blockIndex, start, end)
                        pendingStart = -1
                    } else {
                        pendingStart = start
                    }
                }
                start = end
                end = iterator.next()
            }
            if (pendingStart >= 0 && start > pendingStart) {
                addPieces(original, blockIndex, pendingStart, start)
            }
        }
    }

    private fun MutableList<NarrationSegment>.addPieces(text: String, blockIndex: Int, from: Int, to: Int) {
        var at = from
        while (at < to) {
            var limit = minOf(at + MAX_CHARS, to)
            if (limit < to) {
                val split = (limit downTo at + MAX_CHARS / 2).firstOrNull { text[it - 1].isWhitespace() }
                if (split != null) limit = split
                // Never split UTF-16 surrogate pairs in the middle of a spoken chunk.
                if (limit < text.length && limit > at &&
                    Character.isHighSurrogate(text[limit - 1]) && Character.isLowSurrogate(text[limit])) limit--
            }
            val piece = text.substring(at, limit)
            val trimLeft = piece.indexOfFirst { !it.isWhitespace() }
            if (trimLeft >= 0) {
                val trimmed = piece.trim()
                if (trimmed.isNotEmpty()) add(NarrationSegment(blockIndex, at + trimLeft, trimmed))
            }
            at = limit
        }
    }

    fun seek(segments: List<NarrationSegment>, blockIndex: Int, charOffset: Int): Int {
        if (segments.isEmpty()) return 0
        return segments.indexOfFirst {
            it.blockIndex > blockIndex || (it.blockIndex == blockIndex && it.startOffset + it.text.length > charOffset)
        }.let { if (it < 0) segments.lastIndex else it }
    }
}
