package io.fluxreader.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPageGridTest {
    @Test
    fun keepsWholeSafeViewportInsteadOfDiscardingRemainder() {
        val grid = ReaderPageGridCalculator.calculate(
            viewportHeightPx = 880,
            topPaddingPx = 20,
            bottomPaddingPx = 20,
            nominalLineHeightPx = 30,
            bottomSafetyPx = 15,
        )
        assertEquals(27, grid.nominalLineBudget)
        assertEquals(825, grid.contentHeightPx)
        assertEquals(15, grid.nominalRemainderPx)
    }

    @Test
    fun largerFontAutomaticallyReducesNominalRows() {
        val normal = ReaderPageGridCalculator.calculate(880, 20, 20, 30, 15)
        val large = ReaderPageGridCalculator.calculate(880, 20, 20, 40, 15)
        assertEquals(27, normal.nominalLineBudget)
        assertEquals(20, large.nominalLineBudget)
        assertTrue(large.nominalLineBudget < normal.nominalLineBudget)
    }

    @Test
    fun largerVerticalPaddingReducesAvailableMeasuredHeight() {
        val normal = ReaderPageGridCalculator.calculate(880, 20, 20, 30, 15)
        val padded = ReaderPageGridCalculator.calculate(880, 80, 80, 30, 15)
        assertEquals(825, normal.contentHeightPx)
        assertEquals(705, padded.contentHeightPx)
        assertTrue(padded.contentHeightPx < normal.contentHeightPx)
    }

    @Test
    fun topAndBottomPaddingAreIndependent() {
        val balanced = ReaderPageGridCalculator.calculate(880, 20, 20, 30, 15)
        val moreBottom = ReaderPageGridCalculator.calculate(880, 20, 50, 30, 15)
        assertEquals(825, balanced.contentHeightPx)
        assertEquals(795, moreBottom.contentHeightPx)
    }

    @Test
    fun bottomSafetyTracksFontMetricsAndHasMinimum() {
        assertEquals(28, ReaderPageGridCalculator.bottomSafetyPx(100, 12))
        assertEquals(18, ReaderPageGridCalculator.bottomSafetyPx(40, 18))
    }

    @Test
    fun measurementCapProvidesHeadroomWithoutBecomingTheBoundary() {
        assertEquals(30, ReaderPageGridCalculator.measurementLineCap(825, 30))
        assertEquals(23, ReaderPageGridCalculator.measurementLineCap(825, 40))
        assertTrue(ReaderPageGridCalculator.measurementLineCap(100, 30) >= 4)
    }
}
