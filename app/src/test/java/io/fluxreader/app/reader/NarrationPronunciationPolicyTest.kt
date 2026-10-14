package io.fluxreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class NarrationPronunciationPolicyTest {
    @Test fun expandsRussianAbbreviations() {
        val spoken = NarrationPronunciationPolicy.forSpeech("№ 12 и т. д. …", Locale.forLanguageTag("ru-RU"))
        assertTrue(spoken.contains("номер 12"))
        assertTrue(spoken.contains("так далее"))
        assertTrue(!spoken.contains("…"))
    }
    @Test fun leavesOtherLanguagesUntouched() {
        val source = "No. 12 …"
        assertEquals(source, NarrationPronunciationPolicy.forSpeech(source, Locale.US))
    }
}
