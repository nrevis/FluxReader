package io.fluxreader.app.audio

/** Supported import extensions; actual decoding depends on the Android media codecs. */
object AudiobookFormatPolicy {
    val supported = setOf("mp3", "m4a", "m4b", "aac", "ogg", "oga", "flac", "wav", "opus", "3gp")
    fun isAccepted(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in supported
    fun seekTarget(positionMs: Long, durationMs: Long): Long =
        positionMs.coerceIn(0L, (durationMs - 500L).coerceAtLeast(0L))
}
