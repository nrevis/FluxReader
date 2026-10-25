package io.fluxreader.app.ui

import androidx.compose.ui.graphics.Color
import io.fluxreader.app.data.SpeedSpeakerStyle
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
}
