package io.fluxreader.app.ui

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Measured page viewport used by screen paging.
 *
 * We keep the full safe pixel height instead of snapping it down to a nominal body-line grid.
 * The real TextLayoutResult decides which *complete* lines fit. This removes the visible empty
 * strip that could appear at the bottom when the actual font metrics were slightly tighter than
 * the nominal line height, while still never drawing a clipped final row.
 */
internal data class ReaderPageGrid(
    val nominalLineBudget: Int,
    val contentHeightPx: Int,
    val nominalRemainderPx: Int,
)

internal object ReaderPageGridCalculator {
    /**
     * Extra physical-pixel reserve under the last measured line. Android font fallback and OEM
     * rasterizers can paint descenders a little beyond the logical line bottom reported by the
     * paragraph measurement. Reserve roughly a quarter of a body line, with a device-independent
     * minimum supplied by the caller, so the final visible row cannot be shaved by the viewport.
     */
    fun bottomSafetyPx(nominalLineHeightPx: Int, minimumSafetyPx: Int): Int {
        val lineHeight = nominalLineHeightPx.coerceAtLeast(1)
        return max(minimumSafetyPx.coerceAtLeast(0), (lineHeight * 0.28f).roundToInt())
    }

    fun calculate(
        viewportHeightPx: Int,
        topPaddingPx: Int,
        bottomPaddingPx: Int,
        nominalLineHeightPx: Int,
        bottomSafetyPx: Int,
    ): ReaderPageGrid {
        val viewport = viewportHeightPx.coerceAtLeast(1)
        val topPadding = topPaddingPx.coerceAtLeast(0)
        val bottomPadding = bottomPaddingPx.coerceAtLeast(0)
        val lineHeight = nominalLineHeightPx.coerceAtLeast(1)
        val safety = bottomSafetyPx.coerceAtLeast(0)
        val rawContentHeight = (viewport - topPadding - bottomPadding - safety).coerceAtLeast(1)
        val nominalLines = (rawContentHeight / lineHeight).coerceAtLeast(1)
        return ReaderPageGrid(
            nominalLineBudget = nominalLines,
            contentHeightPx = rawContentHeight,
            nominalRemainderPx = (rawContentHeight - nominalLines * lineHeight).coerceAtLeast(0),
        )
    }

    /**
     * Give TextMeasurer a little headroom above the nominal estimate. The final page boundary is
     * still chosen by getLineBottom(), so this cannot introduce a clipped line.
     */
    fun measurementLineCap(availableHeightPx: Int, nominalLineHeightPx: Int): Int {
        val available = availableHeightPx.coerceAtLeast(1)
        val lineHeight = nominalLineHeightPx.coerceAtLeast(1)
        return (ceil(available.toDouble() / lineHeight.toDouble()).toInt() + 2).coerceIn(1, 384)
    }
}
