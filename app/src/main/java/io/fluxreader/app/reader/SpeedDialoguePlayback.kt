package io.fluxreader.app.reader

import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.SpeedMode

/** Playback offsets remain on the original text, including punctuation hidden from fixations. */
object SpeedDialoguePlayback {
    private val words = Regex("\\S+")

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
        if (config.speedDialogueEnabled && config.speedDialogueShortReplies && config.speedMode == SpeedMode.RSVP) {
            val span = SpeedDialogueAnalyzer.at(text, start)
            if (span != null && readableOffset(text, span.start) == start) {
                val end = text.substring(start, span.endExclusive).trimEnd().length + start
                val reply = text.substring(start, end)
                val tokens = words.findAll(reply).map { it.value }.filter { token -> token.any(Char::isLetterOrDigit) }.take(7).toList()
                if (tokens.size in 2..6 && reply.lastOrNull() in listOf('.', '!', '?', '…', '»', '”')) {
                    // Read the complete bounded reply. General smart chunking would break at
                    // its leading dash or first comma, defeating the short-reply mode.
                    return SpeedChunk(tokens, start, end, focusTokenIndex = 0)
                }
            }
        }
        return SpeedReaderCursor.next(
            text, start, wordCount, config.speedSmartChunking,
            adaptiveWordCount = config.speedAdaptiveChunkSize && config.speedMode != SpeedMode.RSVP,
            pacingMode = config.speedPacingMode,
        )
    }
}
