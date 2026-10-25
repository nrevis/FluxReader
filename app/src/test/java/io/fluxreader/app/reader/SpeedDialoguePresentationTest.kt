package io.fluxreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedDialoguePresentationTest {
    @Test fun unnamedRecognisedReplyStillHasVisibleIndicator() {
        val text = "— Садитесь, прошу вас!"
        val label = SpeedDialoguePresentation.label(SpeedDialogueAnalyzer.at(text, 2), true, true)!!
        assertEquals("Прямая речь", label.text)
        assertFalse(label.namedSpeaker)
    }

    @Test fun explicitNameReplacesGenericIndicator() {
        val text = "— Добрый вечер! — сказала Анна."
        val label = SpeedDialoguePresentation.label(SpeedDialogueAnalyzer.at(text, 2), true, true)!!
        assertEquals("Анна", label.text)
        assertTrue(label.namedSpeaker)
    }

    @Test fun hidingNamesDoesNotDisableDialogueFormatting() {
        val span = SpeedDialogueSpan(0, 20, "Анна")
        assertEquals("Прямая речь", SpeedDialoguePresentation.label(span, false, true)?.text)
        assertEquals("Анна", SpeedDialoguePresentation.label(span, true, false)?.text)
        assertNull(SpeedDialoguePresentation.label(span, false, false))
    }

    @Test fun authorProseDoesNotReceiveDialogueLabel() {
        val text = "— Добрый вечер! — сказала Анна."
        assertNull(SpeedDialoguePresentation.label(SpeedDialogueAnalyzer.at(text, text.indexOf("сказала")), true, true))
    }
}
