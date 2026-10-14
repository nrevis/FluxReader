package io.fluxreader.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPaginationWindowPolicyTest {
    @Test
    fun startsSmallForNormalPhonePages() {
        assertEquals(4_096, ReaderPaginationWindowPolicy.initialWindowChars(12))
        assertTrue(ReaderPaginationWindowPolicy.initialWindowChars(40) < ReaderPaginationWindowPolicy.MAX_WINDOW_CHARS)
    }

    @Test
    fun growsGeometricallyButNeverPastLegacySafetyCap() {
        assertEquals(8_192, ReaderPaginationWindowPolicy.nextWindowChars(4_096))
        assertEquals(16_384, ReaderPaginationWindowPolicy.nextWindowChars(8_192))
        assertEquals(32_768, ReaderPaginationWindowPolicy.nextWindowChars(20_000))
        assertEquals(32_768, ReaderPaginationWindowPolicy.nextWindowChars(32_768))
    }
}
