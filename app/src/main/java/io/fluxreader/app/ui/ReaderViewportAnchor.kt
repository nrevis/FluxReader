package io.fluxreader.app.ui

internal data class ReaderPageLocation(
    val chapterIndex: Int,
    val blockIndex: Int,
    val blockOffsetPx: Int,
    val blockCharOffset: Int = 0,
)

/** Keep a source-text location across every intermediate shell/inset measurement. */
internal object ReaderViewportAnchor {
    fun scrollCharOffset(lineTops: IntArray?, lineStarts: IntArray?, offsetPx: Int): Int {
        if (lineTops == null || lineStarts == null) return 0
        val line = lineTops.indexOfLast { it <= offsetPx }.coerceAtLeast(0)
        return lineStarts.getOrNull(line)?.coerceAtLeast(0) ?: 0
    }

    fun forPagination(
        chapterIndex: Int,
        pending: ReaderPageLocation?,
        locked: ReaderPageLocation?,
        visible: ReaderPageLocation?,
        initial: ReaderPageLocation,
    ): ReaderPageLocation = sequenceOf(pending, locked, visible, initial)
        .filterNotNull()
        .first { it.chapterIndex == chapterIndex }
}
