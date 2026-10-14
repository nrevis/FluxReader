package io.fluxreader.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderAnnotationHitPolicyTest {
    @Test
    fun multiBlockAnnotationHitUsesExactBoundaryCharacters() {
        assertFalse(ReaderAnnotationHitPolicy.contains(2, 4, 2, 5, 4, 8))
        assertTrue(ReaderAnnotationHitPolicy.contains(2, 5, 2, 5, 4, 8))
        assertTrue(ReaderAnnotationHitPolicy.contains(3, 0, 2, 5, 4, 8))
        assertTrue(ReaderAnnotationHitPolicy.contains(4, 7, 2, 5, 4, 8))
        assertFalse(ReaderAnnotationHitPolicy.contains(4, 8, 2, 5, 4, 8))
    }
}
