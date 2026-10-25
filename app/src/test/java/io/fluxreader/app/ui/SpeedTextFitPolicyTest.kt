package io.fluxreader.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedTextFitPolicyTest {
    @Test fun ordinaryWordKeepsRequestedSizeWithoutSearch() {
        var calls = 0
        assertEquals(35f, SpeedTextFitPolicy.largestFitting(35f) { calls++; true }, 0f)
        assertEquals(1, calls)
    }

    @Test fun longWordFitsBothSidesOfFixedFocus() {
        val size = SpeedTextFitPolicy.largestFitting(40f) { sp -> sp * 4 <= 100 && sp * 12 <= 180 }
        assertTrue(size <= 15f)
        assertTrue(size > 14.9f)
    }

    @Test fun measuredFitHandlesNonlinearFontScalingAndReplyGroups() {
        val size = SpeedTextFitPolicy.largestFitting(40f) { sp -> sp * sp * .7f <= 360f }
        assertTrue(size * size * .7f <= 360f)
        assertTrue(size > 22.6f)
    }
}
