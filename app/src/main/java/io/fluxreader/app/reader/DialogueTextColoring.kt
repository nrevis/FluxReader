package io.fluxreader.app.reader

/** UTF-16 positions relative to the exact displayed substring, not to the whole chapter. */
data class DialogueTextColorSegment(
    val start: Int,
    val endExclusive: Int,
    val speaker: String?,
    /** Per-book palette data; the UI still applies the same theme/mode rules as the name label. */
    val tint: SpeakerTint?,
)

/**
 * Immutable chapter snapshot for a chunk and its context. Create once when the text, analysis,
 * corrections or palette changes. Author remarks are holes between speech segments, never colored.
 */
class DialogueTextColoring(
    private val text: String,
    index: ChapterDialogueIndex? = null,
    corrections: List<DialogueCorrection> = emptyList(),
    colors: BookSpeakerColors = BookSpeakerColors(),
) {
    private data class Resolved(val start: Int, val end: Int, val speaker: String?, val tint: SpeakerTint?)
    private val speech: List<Resolved>

    init {
        // A stale chapter index must not reuse a different text's names or boundaries.
        val automatic = if (index != null && index.matches(text)) index.entries.map { it.span }
            else SpeedDialogueAnalyzer.spans(text)
        speech = automatic.map { span ->
            // This is the same containing-reply rule used by DialogueReplyLocator.resolve.
            val correction = corrections.firstOrNull {
                span.start >= it.reply.start && span.endExclusive <= it.reply.endExclusive
            }
            val speaker = (if (correction == null) span.speaker else correction.speaker)
                ?.takeIf { it.isNotBlank() }
            val palette = speaker?.let(colors::forName)
            Resolved(span.start, span.endExclusive, speaker, palette?.let { it.custom ?: it.automatic })
        }
    }

    /** Positions are relative to text.substring(start, endExclusive); unknown speech stays neutral. */
    fun segments(start: Int, endExclusive: Int): List<DialogueTextColorSegment> {
        require(start in 0..text.length && endExclusive in start..text.length) { "Invalid chapter text range" }
        if (start == endExclusive) return emptyList()
        var low = 0
        var high = speech.size
        while (low < high) {
            val mid = (low + high).ushr(1)
            if (speech[mid].end <= start) low = mid + 1 else high = mid
        }
        val result = mutableListOf<DialogueTextColorSegment>()
        for (position in low until speech.size) {
            val span = speech[position]
            if (span.start >= endExclusive) break
            val begin = maxOf(start, span.start)
            val end = minOf(endExclusive, span.end)
            if (begin < end) result += DialogueTextColorSegment(begin - start, end - start, span.speaker, span.tint)
        }
        return result
    }

    /**
     * Positions are relative to chunk.tokens.joinToString(" "). Actual chapter whitespace may have
     * collapsed; intersect each original token before projecting its positions. Inserted spaces are
     * neutral. Mismatched tokens fail closed rather than color a nearby occurrence of the same word.
     */
    fun segments(chunk: SpeedChunk): List<DialogueTextColorSegment> {
        if (chunk.startOffset !in 0..text.length || chunk.endOffset !in chunk.startOffset..text.length) return emptyList()
        if (chunk.tokens.isEmpty() || chunk.tokens.any { it.isEmpty() }) return emptyList()
        val ranges = whitespaceTokenRanges(chunk) ?: confirmedHyphenTokenRanges(chunk) ?: return emptyList()
        val result = mutableListOf<DialogueTextColorSegment>()
        var displayed = 0
        for ((position, range) in ranges.withIndex()) {
            for (segment in segments(range.first, range.last + 1)) {
                result += segment.copy(start = displayed + segment.start, endExclusive = displayed + segment.endExclusive)
            }
            displayed += chunk.tokens[position].length
            if (position < chunk.tokens.lastIndex) displayed++
        }
        return result
    }

    private fun whitespaceTokenRanges(chunk: SpeedChunk): List<IntRange>? {
        val ranges = mutableListOf<IntRange>()
        var source = chunk.startOffset
        for (token in chunk.tokens) {
            while (source < chunk.endOffset && text[source].isWhitespace()) source++
            val end = source + token.length
            if (end > chunk.endOffset || !text.regionMatches(source, token, 0, token.length)) return null
            ranges += source until end
            source = end
        }
        while (source < chunk.endOffset && text[source].isWhitespace()) source++
        return ranges.takeIf { source == chunk.endOffset }
    }

    private fun confirmedHyphenTokenRanges(chunk: SpeedChunk): List<IntRange>? {
        // Only the existing literary rapid-speech parser may consume/split '-' separators. Do not
        // reinterpret a normal hyphenated word, or guess a mapping from a nearby matching token.
        val run = SpeedDialogueHyphenRun.at(text, chunk.startOffset) ?: return null
        val expected = run.chunk(text, chunk.startOffset, chunk.tokens.size)
        if (expected.startOffset != chunk.startOffset || expected.endOffset != chunk.endOffset ||
            expected.tokens != chunk.tokens) return null
        return run.pieces.drop(run.pieceIndex(chunk.startOffset)).take(chunk.tokens.size)
    }
}
