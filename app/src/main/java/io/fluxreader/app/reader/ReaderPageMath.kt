package io.fluxreader.app.reader

data class ReaderPageItem(
    val index: Int,
    val offsetPx: Int,
    val sizePx: Int,
)

data class ReaderPageAnchor(
    val blockIndex: Int,
    val blockOffsetPx: Int,
)

/** Pure page-anchor math. Compose only supplies the current visible item geometry. */
object ReaderPageMath {
    fun next(
        currentIndex: Int,
        currentOffsetPx: Int,
        viewportStartPx: Int,
        viewportEndPx: Int,
        visibleItems: List<ReaderPageItem>,
        snapStepPx: Int = 1,
    ): ReaderPageAnchor? {
        if (visibleItems.isEmpty()) return null
        val viewportHeight = (viewportEndPx - viewportStartPx).coerceAtLeast(1)
        val targetY = viewportStartPx + viewportHeight - 1
        val targetItem = visibleItems.firstOrNull { item ->
            targetY >= item.offsetPx && targetY < item.offsetPx + item.sizePx
        } ?: visibleItems.last()

        var targetIndex = targetItem.index
        var targetOffset = (targetY - targetItem.offsetPx)
            .coerceIn(0, (targetItem.sizePx - 1).coerceAtLeast(0))
        targetOffset = snapOffset(targetOffset, snapStepPx)

        if (targetIndex < currentIndex || (targetIndex == currentIndex && targetOffset <= currentOffsetPx)) {
            val current = visibleItems.firstOrNull { it.index == currentIndex }
            if (current != null && current.sizePx > currentOffsetPx + 1) {
                targetIndex = currentIndex
                val raw = (currentOffsetPx + viewportHeight).coerceAtMost(current.sizePx - 1)
                targetOffset = snapOffset(raw, snapStepPx)
                if (targetOffset <= currentOffsetPx) {
                    targetOffset = (currentOffsetPx + snapStepPx.coerceAtLeast(1))
                        .coerceAtMost(current.sizePx - 1)
                }
            } else {
                val next = visibleItems.firstOrNull { it.index > currentIndex } ?: return null
                targetIndex = next.index
                targetOffset = 0
            }
        }

        return ReaderPageAnchor(targetIndex, targetOffset)
    }

    fun previous(
        currentIndex: Int,
        currentOffsetPx: Int,
        viewportHeightPx: Int,
        previousVisibleItem: ReaderPageItem?,
        snapStepPx: Int = 1,
    ): ReaderPageAnchor? {
        val viewportHeight = viewportHeightPx.coerceAtLeast(1)
        if (currentOffsetPx > 0) {
            return ReaderPageAnchor(
                blockIndex = currentIndex,
                blockOffsetPx = snapOffset((currentOffsetPx - viewportHeight).coerceAtLeast(0), snapStepPx),
            )
        }
        if (currentIndex <= 0) return null
        return if (previousVisibleItem != null) {
            ReaderPageAnchor(
                blockIndex = previousVisibleItem.index,
                blockOffsetPx = snapOffset((previousVisibleItem.sizePx - viewportHeight).coerceAtLeast(0), snapStepPx),
            )
        } else {
            ReaderPageAnchor(currentIndex - 1, 0)
        }
    }

    fun snapOffset(valuePx: Int, stepPx: Int): Int {
        val value = valuePx.coerceAtLeast(0)
        val step = stepPx.coerceAtLeast(1)
        return if (step <= 1 || value <= 0) value else (value / step) * step
    }

    /**
     * Snap a scroll offset to a real text-layout line top. Falling back to an estimated line
     * step keeps old cached positions usable until Compose has measured the paragraph.
     */
    fun snapToMeasuredLine(valuePx: Int, lineTopsPx: IntArray?, fallbackStepPx: Int): Int {
        val value = valuePx.coerceAtLeast(0)
        if (lineTopsPx != null && lineTopsPx.isNotEmpty()) {
            var snapped = 0
            for (top in lineTopsPx) {
                if (top > value) break
                snapped = top
            }
            return snapped.coerceAtLeast(0)
        }
        return snapOffset(value, fallbackStepPx)
    }
}
