package io.fluxreader.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import io.fluxreader.app.data.*
import io.fluxreader.app.reader.*
import org.junit.Assert.*
import org.junit.Test

class SpeedSpeechColorsTest {
    @Test fun disabledColoringPreservesTextAndFocusExactly() {
        val original = AnnotatedString("Добрый вечер", listOf(AnnotatedString.Range(SpanStyle(color = Color.Red), 1, 2)))
        assertSame(original, colorSpeedSpeech(original, emptyList(), 1))
        assertFalse(ReaderSettings().speedDialogueTextColor)
    }
    @Test fun coloringPreservesFocusWeightAndLeavesAuthorTextAlone() {
        val original = AnnotatedString("Да, — сказала Анна", listOf(AnnotatedString.Range(SpanStyle(color = Color.Red, fontWeight = FontWeight.Black), 1, 2)))
        val colored = colorSpeedSpeech(original, listOf(SpeechColorRun(0, 3, Color.Blue)), 1)
        assertEquals(original.text, colored.text)
        assertEquals(Color.Blue, colored.spanStyles.last { 1 in it.start until it.end && it.item.color != Color.Unspecified }.item.color)
        assertTrue(colored.spanStyles.any { it.item.fontWeight == FontWeight.Black })
        assertTrue(colored.spanStyles.any { it.start == 1 && it.end == 2 && it.item.textDecoration == TextDecoration.Underline })
        assertFalse(colored.spanStyles.any { it.start <= 5 && it.end > 5 })
    }
    @Test fun speechUsesTheSameCustomOrAutomaticColorAsTheName() {
        val settings = ReaderSettings(speedSpeakerColor = SpeedSpeakerColor.CHARACTER)
        val entry = SpeakerColorEntry("Анна", SpeakerTint.BLUE, SpeakerTint.ROSE)
        assertEquals(speakerTintColor(SpeakerTint.ROSE, Color.Black), speedSpeakerColor("Анна", true, settings, Color.Black, Color.Green, entry))
        assertEquals(speakerTintColor(SpeakerTint.BLUE, Color.Black), speedSpeakerColor("Анна", true, settings, Color.Black, Color.Green, entry.copy(custom = null)))
        assertEquals(Color.Green, speedSpeakerColor("Анна", true, settings.copy(speedSpeakerColor = SpeedSpeakerColor.THEME), Color.Black, Color.Green, entry.copy(custom = null)))
    }
}
