package io.fluxreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedSpeechTextTest {
    @Test fun chunksRespectOriginalUtf16Offsets() {
        val text = "  Привет, мир!  Это длинное предложение.  😊 Следующее предложение."
        var offset = 0
        var count = 0
        while (true) {
            val part = SpeedSpeechText.next(text, offset, 80) ?: break
            assertEquals(part.text, text.substring(part.start, part.end))
            assertFalse(part.text.first().isWhitespace())
            assertTrue(part.end > offset)
            assertTrue(part.end <= text.length)
            if (part.end < text.length) {
                assertFalse(Character.isHighSurrogate(text[part.end - 1]) && Character.isLowSurrogate(text[part.end]))
            }
            offset = part.end
            count++
            assertTrue(count < 20)
        }
        assertTrue(count > 0)
        assertNull(SpeedSpeechText.next("", 0))
        assertNull(SpeedSpeechText.next("  ", 0))
    }

    @Test fun resumeInsidePhraseKeepsExactOffset() {
        val text = "Первое предложение. Второе предложение. Третье предложение."
        val initial = SpeedSpeechText.next(text, 0, 80)!!
        val following = SpeedSpeechText.next(text, initial.end, 80)
        assertTrue(following == null || following.start >= initial.end)
    }
}
