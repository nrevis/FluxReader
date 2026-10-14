package io.fluxreader.app.ui

enum class SpeedTapAction { NONE, TOGGLE_PLAYBACK, TOGGLE_FULLSCREEN, DISMISS_MEDIA }

/**
 * One gesture map for both normal and immersive Speed Reader layouts.
 * Single tap controls playback; double tap controls fullscreen. Media temporarily owns taps.
 */
object SpeedTapPolicy {
    fun singleTap(
        activeMedia: Boolean,
        tapToToggle: Boolean,
        contentInteractive: Boolean,
    ): SpeedTapAction = when {
        activeMedia -> SpeedTapAction.DISMISS_MEDIA
        contentInteractive && tapToToggle -> SpeedTapAction.TOGGLE_PLAYBACK
        else -> SpeedTapAction.NONE
    }

    fun doubleTap(activeMedia: Boolean, contentInteractive: Boolean): SpeedTapAction = when {
        activeMedia -> SpeedTapAction.DISMISS_MEDIA
        contentInteractive -> SpeedTapAction.TOGGLE_FULLSCREEN
        else -> SpeedTapAction.NONE
    }
}
