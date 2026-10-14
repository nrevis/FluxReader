package io.fluxreader.app.reader

/** Text-to-speech chunks retain original UTF-16 offsets for word-level highlighting. */
object SpeedSpeechText {
    private const val LIMIT = 340

    data class Piece(val start: Int, val end: Int, val text: String)

    fun next(text: String, from: Int, limit: Int = LIMIT): Piece? {
        var start = from.coerceIn(0, text.length)
        while (start < text.length && text[start].isWhitespace()) start++
        if (start == text.length) return null
        val maximum = (start + limit.coerceIn(80, 700)).coerceAtMost(text.length)
        var end = maximum
        if (maximum < text.length) {
            val breakAt = ((start + (maximum - start) / 2)..maximum).lastOrNull {
                text[it - 1] == '.' || text[it - 1] == '!' || text[it - 1] == '?' || text[it - 1] == '…' || text[it - 1] == '\n'
            }
            if (breakAt != null) end = breakAt
            else {
                val whitespace = ((start + (maximum - start) / 2)..maximum).lastOrNull { text[it - 1].isWhitespace() }
                if (whitespace != null) end = whitespace
            }
            if (end > start && end < text.length && Character.isHighSurrogate(text[end - 1]) && Character.isLowSurrogate(text[end])) end--
        }
        if (end <= start) end = (start + 1).coerceAtMost(text.length)
        val raw = text.substring(start, end)
        val actualEnd = start + raw.trimEnd().length
        return Piece(start, actualEnd, text.substring(start, actualEnd))
    }
}
