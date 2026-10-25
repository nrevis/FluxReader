package io.fluxreader.app.reader

/**
 * Experimental, deterministic dialogue recognition. Offsets stay on the ORIGINAL chapter text
 * axis: no source text is modified and Reader <-> Speed Reader bookmarks remain valid.
 * A name is shown only when the adjacent attribution explicitly contains one.
 */
data class SpeedDialogueSpan(
    val start: Int,
    val endExclusive: Int,
    val speaker: String?,
) {
    fun contains(offset: Int): Boolean = offset >= start && offset < endExclusive
}

object SpeedDialogueAnalyzer {
    private const val VERB = "(?:сказал(?:а|и)?|спросил(?:а|и)?|ответил(?:а|и)?|" +
        "произн[её]с(?:ла|ли)?|проговорил(?:а|и)?|шепнул(?:а|и)?|" +
        "прошептал(?:а|и)?|крикнул(?:а|и)?|воскликнул(?:а|и)?|" +
        "заметил(?:а|и)?|буркнул(?:а|и)?|добавил(?:а|и)?|" +
        "возразил(?:а|и)?|переспросил(?:а|и)?|прошипел(?:а|и)?)"
    private const val NAME = "([\\p{Lu}][\\p{L}’'-]{1,29})"
    private val afterDash = Regex("\\s+[—–-]\\s+(?=$VERB\\s+|$NAME\\s+$VERB(?:\\s|[.,!?]|$))", RegexOption.IGNORE_CASE)
    private val verbName = Regex("$VERB\\s+$NAME", RegexOption.IGNORE_CASE)
    private val nameVerb = Regex("$NAME\\s+$VERB", RegexOption.IGNORE_CASE)
    private val quotes = listOf('«' to '»', '“' to '”')
    private val wordPattern = Regex("\\S+")

    /** Looks only at the current paragraph line rather than scanning the whole book per word. */
    fun at(text: String, offset: Int): SpeedDialogueSpan? {
        if (text.isEmpty() || offset < 0 || offset >= text.length) return null
        // Bound work even for broken EPUB/PDF extraction containing one enormous "line".
        // This executes on every RSVP fixation, so unbounded indexOf/lastIndexOf is unsafe.
        val limit = 4096
        var lineStart = offset
        while (lineStart > 0 && offset - lineStart < limit && text[lineStart - 1] != '\n') lineStart--
        if (lineStart > 0 && text[lineStart - 1] != '\n') return null
        var lineEnd = offset
        while (lineEnd < text.length && lineEnd - offset < limit && text[lineEnd] != '\n') lineEnd++
        if (lineEnd < text.length && text[lineEnd] != '\n') return null
        if (lineStart >= lineEnd || lineEnd - lineStart > limit) return null
        val line = text.substring(lineStart, lineEnd)
        val first = line.indexOfFirst { !it.isWhitespace() }
        if (first < 0) return null

        if (line[first] in charArrayOf('—', '–', '-')) {
            val start = lineStart + first
            val leadingEnd = (first + 1 until line.length).firstOrNull { !line[it].isWhitespace() } ?: return null
            // A dash between two words in a prose line is NOT the beginning of a new reply.
            val attribution = afterDash.find(line, leadingEnd)
            val end = attribution?.range?.first?.plus(lineStart) ?: lineEnd
            if (end <= start || offset >= end || offset < start) return null
            val name = attribution?.let { findSpeaker(line.substring(it.range.first)) }
                ?: findSpeaker(line.take(first))
            return SpeedDialogueSpan(start, end, name)
        }

        // Quotation marks are ambiguous (titles, irony); require a nearby speech verb.
        // Do not turn book titles like «Война и мир» into a character conversation.
        if (!verbName.containsMatchIn(line) && !nameVerb.containsMatchIn(line)) return null
        for ((open, close) in quotes) {
            var pos = line.indexOf(open, first)
            while (pos >= 0) {
                val finish = line.indexOf(close, pos + 1)
                if (finish < 0) break
                val start = lineStart + pos
                val end = lineStart + finish + 1
                if (offset in start until end) {
                    val name = findSpeaker(line.take(pos)) ?: findSpeaker(line.substring(finish + 1))
                    return SpeedDialogueSpan(start, end, name)
                }
                pos = line.indexOf(open, finish + 1)
            }
        }
        return null
    }

    private fun findSpeaker(text: String): String? =
        (verbName.find(text)?.groupValues?.get(1)
            ?: nameVerb.find(text)?.groupValues?.get(1))
            ?.takeIf { it.firstOrNull()?.isUpperCase() == true }

    /** Only merge a COMPLETE, very short reply. Never consume an author's attribution. */
    fun shortReplyWordCount(text: String, offset: Int, limit: Int = 6): Int? {
        val span = at(text, offset) ?: return null
        if (offset != span.start && offset != SpeedDialoguePlayback.readableOffset(text, span.start)) return null
        val reply = text.substring(SpeedDialoguePlayback.readableOffset(text, span.start), span.endExclusive).trim()
        val tokens = wordPattern.findAll(reply).map { it.value }.take(limit + 1).toList()
        if (tokens.size !in 1..limit) return null
        val last = tokens.last()
        // Avoid absorbing open-ended dialogue into the narrator when punctuation is missing.
        if ((last.lastOrNull() ?: return null) !in charArrayOf('.', '!', '?', '…', '»', '”')) return null
        return tokens.size
    }

    fun isReplyStart(text: String, offset: Int): Boolean {
        val span = at(text, offset) ?: return false
        return offset == span.start || offset == SpeedDialoguePlayback.readableOffset(text, span.start)
    }
}
