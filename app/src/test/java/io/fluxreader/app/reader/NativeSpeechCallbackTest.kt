package io.fluxreader.app.reader

import kotlinx.coroutines.Job
import org.junit.Assert.*
import org.junit.Test

class NativeSpeechCallbackTest {
    @Test fun offlineDisplayUsesTheActualPhraseWithoutInventedWordFocus() {
        val source = "  Ветер стихал. Наступал вечер.  "
        val piece = SpeedSpeechText.next(source, 0, 260)!!
        val display = SpeedSpeechText.displayChunk(piece)
        assertEquals(source.substring(display.startOffset, display.endOffset), display.tokens.joinToString(" "))
        assertEquals(-1, display.focusTokenIndex)
        assertEquals(listOf("Ветер", "стихал.", "Наступал", "вечер."), display.tokens)
    }
    @Test fun callbackHasTheExactMethodLookedUpBySherpaJni() {
        val method = NativeSpeechCallback::class.java.getDeclaredMethod("invoke", FloatArray::class.java)
        assertEquals(Int::class.javaObjectType, method.returnType)
        val owner = Job()
        val callback = NativeSpeechCallback(owner)
        assertEquals(1, method.invoke(callback, floatArrayOf(0.1f)))
        owner.cancel()
        assertEquals(0, method.invoke(callback, floatArrayOf(0.1f)))
    }
}
