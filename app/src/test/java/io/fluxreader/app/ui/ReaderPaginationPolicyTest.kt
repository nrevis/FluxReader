package io.fluxreader.app.ui

import io.fluxreader.app.data.TextRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPaginationPolicyTest {
    @Test
    fun paragraphDoesNotStartWithOneLonelyLineAtPageBottom() {
        assertTrue(
            ReaderPaginationPolicy.moveSingleOpeningLine(
                role = TextRole.PARAGRAPH,
                atBlockStart = true,
                pageAlreadyHasContent = true,
                fittedLineCount = 1,
                blockContinues = true,
            )
        )
        assertFalse(
            ReaderPaginationPolicy.moveSingleOpeningLine(
                role = TextRole.PARAGRAPH,
                atBlockStart = true,
                pageAlreadyHasContent = false,
                fittedLineCount = 1,
                blockContinues = true,
            )
        )
    }

    @Test
    fun headingMovesWhenThereIsNoRoomForTwoBodyLines() {
        assertTrue(
            ReaderPaginationPolicy.keepHeadingWithBody(
                role = TextRole.HEADING,
                atBlockStart = true,
                pageAlreadyHasContent = true,
                headingFitsCompletely = true,
                remainingHeightPx = 70,
                requiredBodyHeightPx = 80,
            )
        )
        assertFalse(
            ReaderPaginationPolicy.keepHeadingWithBody(
                role = TextRole.HEADING,
                atBlockStart = true,
                pageAlreadyHasContent = true,
                headingFitsCompletely = true,
                remainingHeightPx = 80,
                requiredBodyHeightPx = 80,
            )
        )
    }

    @Test
    fun oneLineRemainderIsOnlyPulledForShortParagraphs() {
        assertTrue(
            ReaderPaginationPolicy.pullSingleTrailingLine(
                role = TextRole.PARAGRAPH,
                fittedLineCount = 3,
                remainderLineCount = 1,
                remainderOverflows = false,
            )
        )
        assertFalse(
            ReaderPaginationPolicy.pullSingleTrailingLine(
                role = TextRole.PARAGRAPH,
                fittedLineCount = 8,
                remainderLineCount = 1,
                remainderOverflows = false,
            )
        )
        assertFalse(
            ReaderPaginationPolicy.pullSingleTrailingLine(
                role = TextRole.CODE,
                fittedLineCount = 3,
                remainderLineCount = 1,
                remainderOverflows = false,
            )
        )
    }

    @Test
    fun paginationCacheIsLruBounded() {
        val cache = PreparedReaderPaginationCache(maxEntries = 2)
        fun page(key: String, chapter: Int) = PreparedReaderPagination(
            chapterIndex = chapter,
            layoutKey = key,
            pages = listOf(PreparedReaderPage(emptyList())),
            contentHeightPx = 100,
        )

        cache.put(page("a", 0))
        cache.put(page("b", 1))
        assertEquals(2, cache.sizeForTest())
        assertEquals("a", cache.get("a")?.layoutKey) // refresh a as most recently used
        cache.put(page("c", 2))

        assertNull(cache.get("b"))
        assertEquals("a", cache.get("a")?.layoutKey)
        assertEquals("c", cache.get("c")?.layoutKey)
        assertEquals(2, cache.sizeForTest())
    }

    @Test
    fun speedFixationRespectsLowerSegmentReserve() {
        val position = SpeedViewportGeometry.clampFixationFraction(
            requestedFraction = 0.82f,
            viewportHeightPx = 1000f,
            mainHalfHeightPx = 80f,
            edgeClearancePx = 24f,
            lowerContentReservePx = 280f,
        )
        assertTrue(position <= 0.616f)
        assertTrue(position >= 0.12f)
    }

    @Test
    fun speedFixationGracefullyHandlesImpossibleShortViewport() {
        val position = SpeedViewportGeometry.clampFixationFraction(
            requestedFraction = 0.80f,
            viewportHeightPx = 220f,
            mainHalfHeightPx = 100f,
            edgeClearancePx = 28f,
            lowerContentReservePx = 180f,
        )
        assertTrue(position.isFinite())
        assertTrue(position in 0.12f..0.88f)
    }
    @Test
    fun speedHorizontalFocusDefaultsToLeftThird() {
        val x = SpeedViewportGeometry.horizontalFocusPx(1000f, 0.30f)
        assertEquals(300f, x, 0.001f)
    }

    @Test
    fun speedHorizontalFocusIsClampedToSafeRange() {
        assertEquals(180f, SpeedViewportGeometry.horizontalFocusPx(1000f, 0.02f), 0.001f)
        assertEquals(500f, SpeedViewportGeometry.horizontalFocusPx(1000f, 0.90f), 0.001f)
    }

}
