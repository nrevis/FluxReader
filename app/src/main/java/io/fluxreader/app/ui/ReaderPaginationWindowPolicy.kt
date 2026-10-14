package io.fluxreader.app.ui

/**
 * Bounds temporary text slices used while measuring one physical reader page.
 *
 * Most phone pages only need a few thousand UTF-16 chars. Starting with a 32K substring for
 * every page creates avoidable allocations on very large paragraphs. We begin small and grow
 * only when the whole probe genuinely fits in the current viewport.
 */
internal object ReaderPaginationWindowPolicy {
    const val MIN_WINDOW_CHARS = 4_096
    const val MAX_WINDOW_CHARS = 32_768

    fun initialWindowChars(maxLines: Int): Int =
        (maxLines.coerceAtLeast(1) * 192)
            .coerceIn(MIN_WINDOW_CHARS, 12_288)

    fun nextWindowChars(currentChars: Int): Int =
        (currentChars.coerceAtLeast(MIN_WINDOW_CHARS) * 2)
            .coerceAtMost(MAX_WINDOW_CHARS)
}
