package io.fluxreader.app.ui

import androidx.compose.ui.graphics.Color
import io.fluxreader.app.data.SpeedSpeakerStyle
import io.fluxreader.app.reader.SpeakerTint
import io.fluxreader.app.reader.SpeakerColorEntry
import io.fluxreader.app.data.SpeedSpeakerColor
import org.junit.Assert.assertNull
import androidx.compose.ui.graphics.compositeOver
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeakerLabelContrastTest {
    @Test fun switchingFromDenseBubbleToTextKeepsWhiteNameOnBlackPage() {
        val fill = Color(0xFF808080)
        assertEquals(Color.Black, speakerLabelTextColor(Color.White, Color.Black, fill, SpeedSpeakerStyle.BUBBLE))
        assertEquals(Color.White, speakerLabelTextColor(Color.White, Color.Black, fill, SpeedSpeakerStyle.TEXT))
    }
    @Test fun paleCustomAccentGetsReadableFallbackOnLightPage() {
        assertEquals(Color.Black, speakerLabelTextColor(Color(0xFFFFFFCC), Color.White, Color.Black, SpeedSpeakerStyle.TEXT))
    }

    @Test fun everyCharacterTintKeepsItsHueWithReadableTextOnLightAndDarkPages() {
        for (background in listOf(Color.Black, Color.White)) {
            for (tint in SpeakerTint.entries) {
                val chosen = speakerTintColor(tint, background)
                val fill = chosen.copy(alpha = .12f).compositeOver(background)
                for (style in SpeedSpeakerStyle.entries) {
                    assertEquals("$tint / $background / $style", chosen, speakerLabelTextColor(chosen, background, fill, style))
                }
            }
        }
    }

    @Test fun customColorsOverrideCommonColorButUnknownSpeechKeepsTheme() {
        val entry = SpeakerColorEntry("Анна", SpeakerTint.GREEN, SpeakerTint.BLUE)
        assertEquals(SpeakerTint.BLUE, speakerNameTint("Анна", true, SpeedSpeakerColor.THEME, entry))
        assertNull(speakerNameTint("Прямая речь", false, SpeedSpeakerColor.CHARACTER, entry))
        assertNull(speakerNameTint("Анна", true, SpeedSpeakerColor.THEME, entry.copy(custom = null)))
        assertEquals(SpeakerTint.GREEN, speakerNameTint("Анна", true, SpeedSpeakerColor.CHARACTER, entry.copy(custom = null)))
    }
}
