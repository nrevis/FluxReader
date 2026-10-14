package io.fluxreader.app.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedSpeechCheckpointPolicyTest {
    @Test fun idleNarratorOnInitialCompositionDoesNotResetSavedWord() {
        assertFalse(SpeedSpeechCheckpointPolicy.commitOnPause(wasSpeaking = false, wasPending = false))
        assertFalse(SpeedSpeechCheckpointPolicy.acceptNarratorOffset(false, 3200, 0))
    }

    @Test fun chapterChangeIgnoresOldNarratorWhenVoiceDisabled() {
        assertFalse(SpeedSpeechCheckpointPolicy.acceptNarratorOffset(false, 850, 0))
        assertFalse(SpeedSpeechCheckpointPolicy.acceptNarratorOffset(false, 850, 400))
    }

    @Test fun realVoicePauseKeepsSpokenWord() {
        assertTrue(SpeedSpeechCheckpointPolicy.commitOnPause(wasSpeaking = true, wasPending = false))
        assertTrue(SpeedSpeechCheckpointPolicy.acceptNarratorOffset(true, 850, 400))
    }

    @Test fun pendingVoiceStartAndInvalidOffsets() {
        assertTrue(SpeedSpeechCheckpointPolicy.commitOnPause(wasSpeaking = false, wasPending = true))
        assertFalse(SpeedSpeechCheckpointPolicy.acceptNarratorOffset(true, 850, -1))
        assertFalse(SpeedSpeechCheckpointPolicy.acceptNarratorOffset(true, 850, 850))
        assertFalse(SpeedSpeechCheckpointPolicy.acceptNarratorOffset(true, 0, 0))
    }
}
