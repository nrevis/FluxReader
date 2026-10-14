package io.fluxreader.app.reader

/** Prevents an idle/new TTS engine from overwriting the user's saved RSVP position.
 * Pure JVM policy: lifecycle regressions can be tested without an Android emulator.
 */
object SpeedSpeechCheckpointPolicy {
    /** No active or pending voice session means there is no spoken position to save. */
    fun commitOnPause(wasSpeaking: Boolean, wasPending: Boolean): Boolean =
        wasSpeaking || wasPending

    /** A narrator may update the visual cursor only when the user enabled voice mode. */
    fun acceptNarratorOffset(voiceMode: Boolean, textLength: Int, offset: Int): Boolean =
        voiceMode && textLength > 0 && offset >= 0 && offset < textLength
}
