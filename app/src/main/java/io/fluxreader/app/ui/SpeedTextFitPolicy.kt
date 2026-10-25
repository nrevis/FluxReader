package io.fluxreader.app.ui

/** Rare overflow fallback. Normal words keep their requested size and their ORP position. */
internal object SpeedTextFitPolicy {
    fun largestFitting(requestedSp: Float, fits: (Float) -> Boolean): Float {
        val requested = requestedSp.takeIf { it.isFinite() }?.coerceAtLeast(1f) ?: 35f
        if (fits(requested)) return requested
        var low = 1f
        var high = requested
        repeat(10) {
            val middle = (low + high) / 2f
            if (fits(middle)) low = middle else high = middle
        }
        return low
    }
}
