package io.fluxreader.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedTapPolicyTest {
    @Test fun singleTapTogglesPlaybackOnlyWhenContentIsReady() {
        assertEquals(
            SpeedTapAction.TOGGLE_PLAYBACK,
            SpeedTapPolicy.singleTap(activeMedia = false, tapToToggle = true, contentInteractive = true),
        )
        assertEquals(
            SpeedTapAction.NONE,
            SpeedTapPolicy.singleTap(activeMedia = false, tapToToggle = true, contentInteractive = false),
        )
        assertEquals(
            SpeedTapAction.NONE,
            SpeedTapPolicy.singleTap(activeMedia = false, tapToToggle = false, contentInteractive = true),
        )
    }

    @Test fun doubleTapOwnsFullscreenInBothLayouts() {
        assertEquals(
            SpeedTapAction.TOGGLE_FULLSCREEN,
            SpeedTapPolicy.doubleTap(activeMedia = false, contentInteractive = true),
        )
        assertEquals(
            SpeedTapAction.NONE,
            SpeedTapPolicy.doubleTap(activeMedia = false, contentInteractive = false),
        )
    }

    @Test fun mediaOwnsTapUntilDismissed() {
        assertEquals(
            SpeedTapAction.DISMISS_MEDIA,
            SpeedTapPolicy.singleTap(activeMedia = true, tapToToggle = true, contentInteractive = true),
        )
        assertEquals(
            SpeedTapAction.DISMISS_MEDIA,
            SpeedTapPolicy.doubleTap(activeMedia = true, contentInteractive = true),
        )
    }
}
