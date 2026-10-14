package io.fluxreader.app.ui

/** Pure calculation used by both experimental device frames and cutout padding. */
internal object ImmersiveLayoutPolicy {
    const val MIN_CAMERA_GUARD_DP = 22f

    fun reserveSystemInsets(immersive: Boolean): Boolean = !immersive

    fun cameraGuardDp(reportedPhysicalCutoutDp: Float): Float =
        maxOf(MIN_CAMERA_GUARD_DP, reportedPhysicalCutoutDp.coerceAtLeast(0f))

    // The physical camera remains visible, but the status/navigation bars do not.
    fun contentTopDp(immersive: Boolean, reportedPhysicalCutoutDp: Float, safeTopDp: Float): Float =
        if (immersive) cameraGuardDp(reportedPhysicalCutoutDp) else safeTopDp.coerceAtLeast(0f)
}
