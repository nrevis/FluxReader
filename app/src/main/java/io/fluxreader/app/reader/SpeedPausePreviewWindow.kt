package io.fluxreader.app.reader

/** A measured pause preview always contains its current fixation, at unchanged chapter offsets. */
object SpeedPausePreviewWindow {
    /**
     * [fits] measures the complete candidate at the actual display width/font. Context is expendable;
     * the active range is not. If that range alone exceeds the viewport budget, return it intact so
     * the UI can allow more lines rather than ellipsize the current words away.
     */
    fun fit(
        text: String,
        focusStart: Int,
        focusEnd: Int,
        maxChars: Int = 700,
        fits: (SpeedTextWindow) -> Boolean,
    ): SpeedTextWindow {
        if (text.isBlank()) return SpeedTextWindow("", 0)
        var start = focusStart.coerceIn(0, text.length)
        var end = focusEnd.coerceIn(start, text.length)
        if (start == text.length) {
            start = (start - 1).coerceAtLeast(0)
        }
        if (start == end) end = (start + 1).coerceAtMost(text.length)
        if (start > 0 && start < text.length && text[start].isLowSurrogate() && text[start - 1].isHighSurrogate()) start--
        if (end > 0 && end < text.length && text[end - 1].isHighSurrogate() && text[end].isLowSurrogate()) end++
        val focusLength = end - start
        val maximum = maxChars.coerceIn(1, 1600).coerceAtLeast(focusLength)
        val paragraphStart = if (start == 0) 0 else text.lastIndexOf('\n', start - 1) + 1
        val paragraphEnd = text.indexOf('\n', end).let { if (it < 0) text.length else it }

        fun window(budget: Int): SpeedTextWindow {
            val context = (budget - focusLength).coerceAtLeast(0)
            var begin = (start - context / 2).coerceAtLeast(paragraphStart)
            var finish = (end + context - context / 2).coerceAtMost(paragraphEnd)
            // Context words may be dropped, but never move either boundary inside the fixation.
            if (begin > paragraphStart && begin < start && !text[begin - 1].isWhitespace()) {
                while (begin < start && !text[begin].isWhitespace()) begin++
            }
            while (begin < start && text[begin].isWhitespace()) begin++
            if (finish < paragraphEnd && finish > end && !text[finish].isWhitespace()) {
                while (finish > end && !text[finish - 1].isWhitespace()) finish--
            }
            while (finish > end && text[finish - 1].isWhitespace()) finish--
            if (begin > 0 && begin < start && text[begin].isLowSurrogate() && text[begin - 1].isHighSurrogate()) begin++
            if (finish < text.length && finish > end && text[finish - 1].isHighSurrogate() && text[finish].isLowSurrogate()) finish--
            return SpeedTextWindow(text.substring(begin, finish), begin)
        }

        var best = window(focusLength)
        if (!fits(best)) return best
        var low = focusLength + 1
        var high = maximum
        while (low <= high) {
            val middle = low + (high - low) / 2
            val candidate = window(middle)
            if (fits(candidate)) {
                best = candidate
                low = middle + 1
            } else {
                high = middle - 1
            }
        }
        return best
    }
}
