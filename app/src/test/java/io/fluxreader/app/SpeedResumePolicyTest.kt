package io.fluxreader.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeedResumePolicyTest {
    @Test fun savedOffsetInFirstChapterWinsOverNormalReader() {
        val target = SpeedResumePolicy.choose(0, 725, 8)
        assertNull(target.preferredChapter)
        assertNull(target.preferredOffset)
    }

    @Test fun savedLaterChapterWinsOverNormalReader() {
        val target = SpeedResumePolicy.choose(5, 0, 2)
        assertNull(target.preferredChapter)
    }

    @Test fun firstSpeedSessionCanStartAtCurrentReaderChapter() {
        val target = SpeedResumePolicy.choose(0, 0, 3)
        assertEquals(3, target.preferredChapter)
    }

    @Test fun openingFromLibraryUsesSpeedCheckpoint() {
        val target = SpeedResumePolicy.choose(0, 0, null)
        assertNull(target.preferredChapter)
    }
}
