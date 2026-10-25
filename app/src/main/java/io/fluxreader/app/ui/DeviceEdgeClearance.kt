package io.fluxreader.app.ui

import android.os.Build
import android.view.RoundedCorner
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/** Insets for hardware corners even when Android's system bars are hidden. */
internal object DeviceEdgeClearance {
    @Composable
    fun immersivePadding(): PaddingValues {
        val density = LocalDensity.current
        val insets = LocalView.current.rootWindowInsets
        val radius = if (Build.VERSION.SDK_INT >= 31) listOf(
            RoundedCorner.POSITION_TOP_LEFT, RoundedCorner.POSITION_TOP_RIGHT,
            RoundedCorner.POSITION_BOTTOM_LEFT, RoundedCorner.POSITION_BOTTOM_RIGHT,
        ).maxOf { insets?.getRoundedCorner(it)?.radius ?: 0 } else 0
        val edge = cornerGuardDp(with(density) { radius.toDp().value }).dp
        val cutout = WindowInsets.displayCutout.asPaddingValues()
        return PaddingValues(start = maxOf(edge, cutout.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)),
            end = maxOf(edge, cutout.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)),
            top = maxOf(edge, ImmersiveCameraClearance.current()),
            bottom = maxOf(edge, cutout.calculateBottomPadding()))
    }

    // Half a corner radius on each axis lies inside the circular safe region. The fallback
    // covers older devices/vendors that omit RoundedCorner metadata. Never draw controls flush.
    fun cornerGuardDp(radiusDp: Float): Float = maxOf(20f, radiusDp.takeIf { it.isFinite() }?.coerceAtLeast(0f)?.times(.5f) ?: 0f)
}
