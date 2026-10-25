package io.fluxreader.app.reader

/** Literary rapid speech such as «Третий-закон-...». Ordinary hyphenated words stay intact. */
internal data class SpeedDialogueHyphenRun(val pieces: List<IntRange>) {
    fun pieceIndex(offset: Int): Int = pieces.indexOfFirst { offset <= it.last }.coerceAtLeast(0)

    fun chunk(text: String, offset: Int, wordCount: Int): SpeedChunk {
        val selected = pieces.drop(pieceIndex(offset)).take(wordCount.coerceIn(1, 7))
        val end = selected.last().last + 1
        return SpeedChunk(
            tokens = selected.map { text.substring(it.first, it.last + 1) },
            startOffset = selected.first().first,
            // Consume the separator, while every saved offset still refers to the source text.
            endOffset = if (end < text.length && text[end] == '-') end + 1 else end,
            focusTokenIndex = 0,
        )
    }

    companion object {
        private val part = Regex("[^-]+")
        private val punctuation = charArrayOf('«', '»', '“', '”', '"', ',', '.', '!', '?', '…', ';', ':')

        fun at(text: String, offset: Int): SpeedDialogueHyphenRun? {
            if (offset !in text.indices) return null
            var start = offset
            var end = offset
            while (start > 0 && offset - start < 4096 && !text[start - 1].isWhitespace()) start--
            while (end < text.length && end - start <= 4096 && !text[end].isWhitespace()) end++
            if (end - start !in 50..4096) return null
            val token = text.substring(start, end)
            if (token.count { it == '-' } < 7) return null
            val matches = part.findAll(token).toList()
            if (matches.size < 8 || token.startsWith('-') || token.endsWith('-') || "--" in token) return null
            if (matches.any { match ->
                val lexical = match.value.trim(*punctuation)
                lexical.length !in 1..35 || !lexical.all(Char::isLetter)
            }) return null
            val speech = SpeedDialogueAnalyzer.at(text, offset) ?: return null
            if (start < speech.start || end > speech.endExclusive) return null
            return SpeedDialogueHyphenRun(matches.map { (start + it.range.first)..(start + it.range.last) })
        }
    }
}
