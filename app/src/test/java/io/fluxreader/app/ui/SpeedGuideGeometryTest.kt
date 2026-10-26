package io.fluxreader.app.ui

import org.junit.Assert.*
import org.junit.Test

class SpeedGuideGeometryTest {
    @Test fun nameAndContextLeaveVisibleMarkersOutsideTheReadingLine() {
        val b = SpeedGuideGeometry.bounds(600f, 300f, 35f, 4f, 90f, 20f, 22f)
        assertEquals(261f, b.top, .01f)
        assertEquals(339f, b.bottom, .01f)
        assertTrue(b.upperLength > 0f)
        assertTrue(b.lowerLength > 0f)
        assertTrue(b.top - b.upperLength > 300f - 35f - 20f)
        assertTrue(b.bottom + b.lowerLength < 300f + 35f + 22f)
    }
    @Test fun smallestNameGapStillLeavesALineAndBothMarkers() {
        val b = SpeedGuideGeometry.bounds(600f, 300f, 80f, 4f, 90f, 12f, 12f)
        assertEquals(4f, b.upperLength, .01f)
        assertEquals(4f, b.lowerLength, .01f)
        assertTrue(b.top < 220f)
        assertTrue(b.bottom > 380f)
    }
    @Test fun railStopsAtViewportEdges() {
        val b = SpeedGuideGeometry.bounds(100f, 20f, 10f, 4f, 90f, 160f, 160f)
        assertEquals(0f, b.top - b.upperLength, .01f)
        assertEquals(100f, b.bottom + b.lowerLength, .01f)
    }
    @Test fun veryLargeTextNeverProducesNegativeRailLengths() {
        val b = SpeedGuideGeometry.bounds(80f, 40f, 100f, 4f, 90f, 12f, 12f)
        assertEquals(0f, b.top, .01f)
        assertEquals(80f, b.bottom, .01f)
        assertEquals(0f, b.upperLength, .01f)
        assertEquals(0f, b.lowerLength, .01f)
    }
}
