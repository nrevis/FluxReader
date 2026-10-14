package io.fluxreader.app

/** A stored Speed Reader cursor takes precedence over the normal Reader chapter.
 * Explicit selection of a word uses openSpeedAt() and bypasses this policy.
 */
internal data class SpeedOpenTarget(val preferredChapter: Int?, val preferredOffset: Int?)

internal object SpeedResumePolicy {
    fun choose(savedChapter: Int, savedOffset: Int, readerChapter: Int?): SpeedOpenTarget =
        if (savedChapter > 0 || savedOffset > 0) {
            SpeedOpenTarget(preferredChapter = null, preferredOffset = null)
        } else {
            SpeedOpenTarget(preferredChapter = readerChapter, preferredOffset = null)
        }
}
