package io.fluxreader.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderFullscreenAnchorPolicyTest {
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
