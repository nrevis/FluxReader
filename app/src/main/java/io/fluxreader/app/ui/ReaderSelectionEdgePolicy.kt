package io.fluxreader.app.ui

/** Pure geometry helpers for selection auto-scroll / cross-page continuation. */
object ReaderSelectionEdgePolicy {
    fun direction(windowY: Float, viewportHeight: Float, edgeMarginPx: Float): Int = when {
        viewportHeight <= 0f || edgeMarginPx <= 0f -> 0
        windowY <= edgeMarginPx -> -1
        windowY >= viewportHeight - edgeMarginPx -> 1
        else -> 0
    }

    fun adjacentPageIndex(current: Int, lastIndex: Int, direction: Int): Int? {
        if (lastIndex < 0 || direction == 0) return null
        val next = (current + direction.sign()).coerceIn(0, lastIndex)
        return next.takeIf { it != current }
    }

    private fun Int.sign(): Int = when {
        this < 0 -> -1
        this > 0 -> 1
        else -> 0
    }
}
