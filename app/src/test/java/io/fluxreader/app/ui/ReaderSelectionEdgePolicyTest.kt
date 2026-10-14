package io.fluxreader.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderSelectionEdgePolicyTest {
    @Test
    fun edgeDirectionOnlyTriggersInsideViewportRails() {
        assertEquals(-1, ReaderSelectionEdgePolicy.direction(32f, 1000f, 72f))
        assertEquals(0, ReaderSelectionEdgePolicy.direction(500f, 1000f, 72f))
        assertEquals(1, ReaderSelectionEdgePolicy.direction(950f, 1000f, 72f))
    }

    @Test
    fun pageContinuationStopsAtDocumentEdges() {
        assertEquals(3, ReaderSelectionEdgePolicy.adjacentPageIndex(2, 5, 1))
        assertEquals(1, ReaderSelectionEdgePolicy.adjacentPageIndex(2, 5, -1))
        assertNull(ReaderSelectionEdgePolicy.adjacentPageIndex(0, 5, -1))
        assertNull(ReaderSelectionEdgePolicy.adjacentPageIndex(5, 5, 1))
    }
}
