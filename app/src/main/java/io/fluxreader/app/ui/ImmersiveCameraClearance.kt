package io.fluxreader.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The Pixel 9 Pro has a physical centered hole-punch, not an always-visible
 * status bar. When bars disappear, reserve only that small camera lane.
 * Never use safeDrawing here: it may retain a status/navigation inset and
 * cause empty bars in immersive mode.
 */
internal object ImmersiveCameraClearance {
    @Composable
    fun current(): Dp {
        // A small fallback protects the hole when an Android vendor returns a
        // zero displayCutout inset after hiding the system bars. The fallback
        // is a camera guard, not a status-bar reservation.
        val physicalCutout = WindowInsets.displayCutout.asPaddingValues().calculateTopPadding()
        return ImmersiveLayoutPolicy.cameraGuardDp(physicalCutout.value).dp
    }
}
