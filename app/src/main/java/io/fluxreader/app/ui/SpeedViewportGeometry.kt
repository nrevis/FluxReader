package io.fluxreader.app.ui

/** Geometry used by Speed Reader to keep the fixation block and secondary text on screen. */
internal object SpeedViewportGeometry {
    fun clampFixationFraction(
        requestedFraction: Float,
        viewportHeightPx: Float,
        mainHalfHeightPx: Float,
        edgeClearancePx: Float,
        lowerContentReservePx: Float,
        upperContentReservePx: Float = 0f,
        preferredMinFraction: Float = 0.12f,
        preferredMaxFraction: Float = 0.88f,
    ): Float {
        val viewport = viewportHeightPx.takeIf { it.isFinite() && it > 0f } ?: return 0.5f
        val mainHalf = mainHalfHeightPx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val clearance = edgeClearancePx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val lower = lowerContentReservePx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val requested = requestedFraction.takeIf { it.isFinite() } ?: 0.5f

        val upper = upperContentReservePx.takeIf { it.isFinite() }?.coerceAtLeast(0f) ?: 0f
        val hardMin = (mainHalf + clearance + upper) / viewport
        val hardMax = 1f - (mainHalf + clearance + lower) / viewport
        val preferredMin = preferredMinFraction.coerceIn(0f, 1f)
        val preferredMax = preferredMaxFraction.coerceIn(preferredMin, 1f)
        val safeMin = maxOf(preferredMin, hardMin)
        val safeMax = minOf(preferredMax, hardMax)

        if (safeMin <= safeMax) return requested.coerceIn(safeMin, safeMax)

        // On an extremely short viewport no position can fit everything. Center between the two
        // hard limits to distribute the unavoidable overflow instead of pinning text to an edge.
        return ((hardMin + hardMax) * 0.5f).coerceIn(preferredMin, preferredMax)
    }
    fun clampHorizontalFocusFraction(
        requestedFraction: Float,
        preferredMinFraction: Float = 0.18f,
        preferredMaxFraction: Float = 0.50f,
    ): Float {
        val min = preferredMinFraction.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0.18f
        val max = preferredMaxFraction.takeIf { it.isFinite() }?.coerceIn(min, 1f) ?: 0.50f
        val requested = requestedFraction.takeIf { it.isFinite() } ?: 0.30f
        return requested.coerceIn(min, max)
    }

    fun horizontalFocusPx(viewportWidthPx: Float, requestedFraction: Float): Float {
        val width = viewportWidthPx.takeIf { it.isFinite() && it > 0f } ?: return 0f
        return width * clampHorizontalFocusFraction(requestedFraction)
    }

}
