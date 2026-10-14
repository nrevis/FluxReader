package io.fluxreader.app.ui

/** Small bounded source used only to paint the saved reading anchor before full pagination. */
internal object ReaderPreviewPolicy {
    const val MAX_BLOCKS = 3
    const val MAX_TEXT_CHARS_PER_BLOCK = 8_192

    fun boundedEnd(start: Int, textLength: Int): Int {
        val safeLength = textLength.coerceAtLeast(0)
        val safeStart = start.coerceIn(0, safeLength)
        return minOf(safeLength, safeStart + MAX_TEXT_CHARS_PER_BLOCK)
    }
}
