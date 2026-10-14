package io.fluxreader.app.reader

/** Pure reading-progress math shared by persistence and UI-facing state. */
internal object ReaderProgressMath {
    /**
     * Position inside one chapter. Block index provides the coarse location; when the current
     * block is text, its character offset refines the fraction without depending on screen size,
     * font metrics, or current pagination.
     */
    fun chapterFraction(
        blockIndex: Int,
        blockCount: Int,
        blockCharOffset: Int,
        blockCharCount: Int,
    ): Float {
        val count = blockCount.coerceAtLeast(1)
        val index = blockIndex.coerceIn(0, count - 1)
        val withinBlock = if (blockCharCount > 0) {
            blockCharOffset.coerceIn(0, blockCharCount).toFloat() / blockCharCount.toFloat()
        } else {
            0f
        }
        return ((index.toFloat() + withinBlock) / count.toFloat()).coerceIn(0f, 1f)
    }

    /** Stable book fraction independent of viewport/pagination. */
    fun bookFraction(
        chapterIndex: Int,
        chapterCount: Int,
        chapterFraction: Float,
    ): Float {
        val count = chapterCount.coerceAtLeast(1)
        val chapter = chapterIndex.coerceIn(0, count - 1)
        val local = chapterFraction.coerceIn(0f, 1f)
        return ((chapter.toFloat() + local) / count.toFloat()).coerceIn(0f, 1f)
    }
}
