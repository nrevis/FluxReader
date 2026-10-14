package io.fluxreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderPageMathTest {
    @Test
    fun next_moves_inside_long_block_by_one_viewport() {
        val result = ReaderPageMath.next(
            currentIndex = 4,
            currentOffsetPx = 200,
            viewportStartPx = 0,
            viewportEndPx = 800,
            visibleItems = listOf(ReaderPageItem(index = 4, offsetPx = -200, sizePx = 2600)),
        )
        assertEquals(ReaderPageAnchor(4, 999), result)
    }

    @Test
    fun next_anchors_into_block_at_bottom_of_page() {
        val result = ReaderPageMath.next(
            currentIndex = 2,
            currentOffsetPx = 0,
            viewportStartPx = 0,
            viewportEndPx = 800,
            visibleItems = listOf(
                ReaderPageItem(2, 0, 300),
                ReaderPageItem(3, 310, 900),
            ),
        )
        assertEquals(ReaderPageAnchor(3, 489), result)
    }

    @Test
    fun previous_rewinds_inside_same_long_block() {
        val result = ReaderPageMath.previous(
            currentIndex = 7,
            currentOffsetPx = 1700,
            viewportHeightPx = 800,
            previousVisibleItem = null,
        )
        assertEquals(ReaderPageAnchor(7, 900), result)
    }

    @Test
    fun previous_at_document_start_is_null() {
        assertNull(ReaderPageMath.previous(0, 0, 800, null))
    }
    @Test
    fun next_snaps_inside_text_block_to_line_boundary() {
        val result = ReaderPageMath.next(
            currentIndex = 2,
            currentOffsetPx = 0,
            viewportStartPx = 0,
            viewportEndPx = 800,
            visibleItems = listOf(
                ReaderPageItem(2, 0, 300),
                ReaderPageItem(3, 310, 900),
            ),
            snapStepPx = 40,
        )
        assertEquals(ReaderPageAnchor(3, 480), result)
    }

    @Test
    fun previous_snaps_to_line_boundary() {
        val result = ReaderPageMath.previous(
            currentIndex = 7,
            currentOffsetPx = 1700,
            viewportHeightPx = 800,
            previousVisibleItem = null,
            snapStepPx = 40,
        )
        assertEquals(ReaderPageAnchor(7, 880), result)
    }

    @Test
    fun restored_offset_snaps_down_without_skipping_text() {
        assertEquals(480, ReaderPageMath.snapOffset(489, 40))
        assertEquals(0, ReaderPageMath.snapOffset(-15, 40))
    }

    @Test
    fun measured_line_tops_beat_estimated_line_height() {
        val actual = intArrayOf(0, 38, 77, 116, 155, 194, 233, 272, 311, 350, 389, 428, 467, 506)
        assertEquals(467, ReaderPageMath.snapToMeasuredLine(489, actual, 40))
        assertEquals(480, ReaderPageMath.snapToMeasuredLine(489, null, 40))
    }

    @Test
    fun measured_line_snap_never_skips_forward() {
        val lines = intArrayOf(0, 41, 83, 124, 166)
        assertEquals(83, ReaderPageMath.snapToMeasuredLine(100, lines, 40))
        assertEquals(0, ReaderPageMath.snapToMeasuredLine(-5, lines, 40))
    }
}
