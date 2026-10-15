package io.fluxreader.app.ui

internal data class ReaderPageLocation(
    val chapterIndex: Int,
    val blockIndex: Int,
    val blockOffsetPx: Int,
    val blockCharOffset: Int = 0,
)

/** Keep a source-text location across every intermediate shell/inset measurement. */
internal object ReaderViewportAnchor {
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
