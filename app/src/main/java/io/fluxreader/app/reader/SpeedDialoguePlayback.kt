package io.fluxreader.app.reader

import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.SpeedMode

/** Playback offsets remain on the original text, including punctuation hidden from fixations. */
object SpeedDialoguePlayback {
    private val words = Regex("[^\\s\\p{Zs}]+")

    /** Resume inside a literary hyphen chain at the current part, preserving the saved position. */
    fun wordStartOffset(text: String, offset: Int, config: ReaderSettings): Int {
        val ordinary = SpeedReaderCursor.wordStartOffset(text, offset)
        val candidate = offset.takeIf { it in text.indices && !text[it].isWhitespace() } ?: ordinary
        val run = if (config.speedDialogueEnabled) SpeedDialogueHyphenRun.at(text, candidate) else null
        return run?.pieces?.get(run.pieceIndex(candidate))?.first ?: ordinary
    }

    fun readableOffset(text: String, offset: Int): Int {
        var cursor = offset.coerceIn(0, text.length)
        while (cursor < text.length) {
            while (cursor < text.length && text[cursor].isWhitespace()) cursor++
            val start = cursor
            while (cursor < text.length && !text[cursor].isWhitespace()) cursor++
            if ((start until cursor).any { text[it].isLetterOrDigit() }) return start
        }
        return text.length
    }

    fun next(text: String, offset: Int, config: ReaderSettings, wordCount: Int): SpeedChunk? {
        val start = if (config.speedDialogueEnabled) readableOffset(text, offset) else offset
        if (config.speedDialogueEnabled) SpeedDialogueHyphenRun.at(text, start)?.let { run ->
            return run.chunk(text, start, if (config.speedMode == SpeedMode.RSVP) 1 else wordCount)
        }
        if (config.speedDialogueEnabled && config.speedDialogueShortReplies && config.speedMode == SpeedMode.RSVP) {
            val span = SpeedDialogueAnalyzer.at(text, start)
            if (span != null && !span.continuation && readableOffset(text, span.start) == start) {
                val end = text.substring(start, span.endExclusive).trimEnd().length + start
                val reply = text.substring(start, end)
                val tokens = words.findAll(reply).map { it.value }.filter { token -> token.any(Char::isLetterOrDigit) }.take(7).toList()
                val hasHyphenRun = words.findAll(reply).any { SpeedDialogueHyphenRun.at(text, start + it.range.first) != null }
                if (!hasHyphenRun && tokens.size in 2..6 && reply.lastOrNull() in listOf('.', '!', '?', '…', '»', '”')) {
                    // Read the complete bounded reply. General smart chunking would break at
                    // its leading dash or first comma, defeating the short-reply mode.
                    return SpeedChunk(tokens, start, end, focusTokenIndex = 0)
                }
            }
        }
        val chunk = SpeedReaderCursor.next(
            text, start, wordCount, config.speedSmartChunking,
            adaptiveWordCount = config.speedAdaptiveChunkSize && config.speedMode != SpeedMode.RSVP,
            pacingMode = config.speedPacingMode,
        ) ?: return null
        if (!config.speedDialogueEnabled) return chunk
        val initial = SpeedDialogueAnalyzer.at(text, start)
        // Phrase/focus mode must not join speech and an author's remark in one fixation.
        val matches = words.findAll(text.substring(start, chunk.endOffset)).toList()
        val tokenBoundary = matches.firstOrNull { match ->
            val at = start + match.range.first
            match.value.any(Char::isLetterOrDigit) && SpeedDialogueAnalyzer.at(text, at) != initial
        }?.range?.first?.plus(start) ?: chunk.endOffset
        // EPUBs often glue a marker to punctuation ("Сир,- ответил..."). Its boundary
        // can lie inside a whitespace token, so token starts alone are insufficient.
        val hyphenBoundary = matches.firstOrNull { match ->
            val at = start + match.range.first
            at > start && SpeedDialogueHyphenRun.at(text, at) != null
        }?.range?.first?.plus(start) ?: chunk.endOffset
        val boundary = minOf(tokenBoundary, initial?.endExclusive ?: chunk.endOffset, hyphenBoundary)
        val tokens = words.findAll(text.substring(start, boundary)).map { it.value }
            .filter { it.any(Char::isLetterOrDigit) }.toList()
        return if (tokens.isEmpty()) chunk else chunk.copy(tokens = tokens, endOffset = boundary,
            focusTokenIndex = chunk.focusTokenIndex.coerceAtMost(tokens.lastIndex))
    }
}
