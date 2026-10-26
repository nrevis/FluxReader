package io.fluxreader.app.reader

/** A timer owns only the fixation it displayed. Cancellation by Compose can follow a user action,
 * so the ViewModel validates the current session again before advancing its cursor. */
internal object SpeedPlaybackAdvancePolicy {
    fun accepts(
        routeActive: Boolean,
        playing: Boolean,
        loading: Boolean,
        blocked: Boolean,
        currentBookId: String?,
        currentChapterIndex: Int,
        currentChunk: SpeedChunk?,
        expectedBookId: String?,
        expectedChapterIndex: Int,
        expectedStartOffset: Int,
        expectedEndOffset: Int,
    ): Boolean {
        if (!routeActive || !playing || loading || blocked || currentBookId == null || currentChunk == null) return false
        return currentBookId == expectedBookId && currentChapterIndex == expectedChapterIndex &&
            expectedChapterIndex >= 0 && expectedStartOffset >= 0 && expectedEndOffset > expectedStartOffset &&
            currentChunk.startOffset == expectedStartOffset && currentChunk.endOffset == expectedEndOffset
    }
}
