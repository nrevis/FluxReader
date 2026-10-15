package io.fluxreader.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderFullscreenAnchorPolicyTest {
    private fun layout(width: Int) = PreparedReaderPagination(
        chapterIndex = 4,
        layoutKey = "width-$width",
        pages = (0 until 6000 step width).map { start ->
            PreparedReaderPage(listOf(PreparedReaderPageItem.TextSlice(0, start, start + width, 100)))
        },
        contentHeightPx = 100,
    )

    @Test
    fun repeatedShellAndSystemBarMeasurementsKeepTheSourceAnchor() {
        val original = ReaderPageLocation(4, 0, 0, 2050)
        val initial = ReaderPageLocation(4, 0, 0, 0)
        var visible = original
        var pending: ReaderPageLocation? = original
        // Each full-screen toggle can publish a complete layout before the shell and bars
        // have finished resizing. Rounding to each new page start used to drift back to zero.
        repeat(20) {
            for (width in listOf(610, 780, 900, 650)) {
                val target = ReaderViewportAnchor.forPagination(4, pending, original, visible, initial)
                assertEquals(original, target)
                val pagination = layout(width)
                val index = findPreparedPageIndex(pagination, target.blockIndex, target.blockCharOffset)
                val page = pagination.pages[index]
                assertEquals(2050 / width, index)
                visible = ReaderPageLocation(4, page.startBlockIndex, 0, page.startCharOffset)
                pending = null // first complete layout consumed the pending restore
            }
        }
    }

    @Test
    fun pageNavigationReleasesTheFullscreenAnchor() {
        val visible = ReaderPageLocation(4, 0, 0, 3250)
        val initial = ReaderPageLocation(4, 0, 0, 0)
        assertEquals(visible, ReaderViewportAnchor.forPagination(4, null, null, visible, initial))
    }

    @Test
    fun anchorsFromThePreviousChapterCannotResetTheNewChapter() {
        val stale = ReaderPageLocation(3, 0, 0, 10)
        val initial = ReaderPageLocation(4, 7, 0, 300)
        assertEquals(initial, ReaderViewportAnchor.forPagination(4, stale, stale, stale, initial))
    }

    @Test
    fun characterAnchorSurvivesDifferentPageGeometry() {
        fun page(start: Int, end: Int) = PreparedReaderPage(
            listOf(
                PreparedReaderPageItem.TextSlice(
                    blockIndex = 0,
                    startCharOffset = start,
                    endCharOffset = end,
                    measuredHeightPx = 100,
                )
            )
        )

        val fullscreen = PreparedReaderPagination(
            chapterIndex = 4,
            layoutKey = "fullscreen",
            pages = listOf(page(0, 80), page(80, 160), page(160, 240), page(240, 320)),
            contentHeightPx = 100,
            isComplete = true,
        )

        assertEquals(
            2,
            findPreparedPageIndex(
                pagination = fullscreen,
                blockIndex = 0,
                charOffset = 205,
            ),
        )
    }
}
