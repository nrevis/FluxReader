package io.fluxreader.app.audio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AudiobookFormatPolicyTest {
    @Test fun acceptsCommonAudiobookFormatsCaseInsensitively() {
        listOf("BOOK.MP3", "book.m4b", "audio.M4A", "track.flac", "book.ogg", "book.wav")
            .forEach { assertTrue(it, AudiobookFormatPolicy.isAccepted(it)) }
        assertFalse(AudiobookFormatPolicy.isAccepted("document.epub"))
        assertFalse(AudiobookFormatPolicy.isAccepted("book.pdf"))
    }

    @Test fun seekClampsPositionsAndHandlesShortFiles() {
        assertEquals(0L, AudiobookFormatPolicy.seekTarget(-9000, 60_000))
        assertEquals(59_500L, AudiobookFormatPolicy.seekTarget(90_000, 60_000))
        assertEquals(0L, AudiobookFormatPolicy.seekTarget(100, 300))
    }
}
