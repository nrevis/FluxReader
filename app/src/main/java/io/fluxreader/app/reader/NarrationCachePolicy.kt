package io.fluxreader.app.reader

import java.security.MessageDigest

/** Separate cache identity for every voice, pronunciation, rate and pitch change. */
internal object NarrationCachePolicy {
    fun key(text: String, voice: String, language: String, rate: Float, pitch: Float): String {
        val input = listOf("wav-v1", voice, language, rate.toString(), pitch.toString(), text).joinToString("\u0000")
        return MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
