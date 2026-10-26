package io.fluxreader.app.ui

import io.fluxreader.app.data.migratedComplexWordPauseMs
import org.junit.Assert.assertEquals
import org.junit.Test

class SpeedTimingMigrationTest {
    @Test fun newInstallUsesRecommendedTimeAndOldOptOutIsKept() {
        assertEquals(150, migratedComplexWordPauseMs(null))
        assertEquals(0, migratedComplexWordPauseMs(0))
    }
    @Test fun oldStrengthBecomesANoticeableBoundedMillisecondAllowance() {
        assertEquals(90, migratedComplexWordPauseMs(18))
        assertEquals(140, migratedComplexWordPauseMs(28))
        assertEquals(300, migratedComplexWordPauseMs(60))
        assertEquals(300, migratedComplexWordPauseMs(Int.MAX_VALUE))
        assertEquals(0, migratedComplexWordPauseMs(Int.MIN_VALUE))
    }
}
