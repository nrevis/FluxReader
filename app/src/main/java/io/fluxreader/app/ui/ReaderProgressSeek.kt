package io.fluxreader.app.ui

import kotlin.math.floor

/** Book-level seeking with uniform chapter weights (the same scale as ReaderBottomBar). */
internal object ReaderProgressSeek {
    data class Target(val chapterIndex: Int, val chapterFraction: Float)

    fun target(progress: Float, chapterCount: Int): Target {
        if (chapterCount <= 0) return Target(0, 0f)
        val safe = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f
        val scaled = safe * chapterCount
        val chapter = floor(scaled).toInt().coerceIn(0, chapterCount - 1)
        return Target(chapter, (scaled - chapter).coerceIn(0f, 1f))
    }

    fun itemIndex(chapterFraction: Float, itemCount: Int): Int {
        if (itemCount <= 0) return 0
        val safe = if (chapterFraction.isFinite()) chapterFraction.coerceIn(0f, 1f) else 0f
        return floor(safe * itemCount).toInt().coerceIn(0, itemCount - 1)
    }
}
