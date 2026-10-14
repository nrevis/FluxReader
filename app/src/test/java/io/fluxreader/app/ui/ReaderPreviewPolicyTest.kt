package io.fluxreader.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderPreviewPolicyTest {
    @Test fun boundedEnd_neverExceedsPreviewWindow() {
        assertEquals(8_192, ReaderPreviewPolicy.boundedEnd(0, 80_000))
        assertEquals(18_192, ReaderPreviewPolicy.boundedEnd(10_000, 80_000))
        assertEquals(80_000, ReaderPreviewPolicy.boundedEnd(79_000, 80_000))
    }

    @Test fun boundedEnd_clampsInvalidStart() {
        assertEquals(0, ReaderPreviewPolicy.boundedEnd(-10, 0))
        assertEquals(10, ReaderPreviewPolicy.boundedEnd(50, 10))
    }
}
