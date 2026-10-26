package io.fluxreader.app.reader

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedPlaybackAdvancePolicyTest {
    private val chunk = SpeedChunk(listOf("слово"), 10, 15, 0)

    private fun accepts(
        routeActive: Boolean = true,
        playing: Boolean = true,
        loading: Boolean = false,
        blocked: Boolean = false,
        bookId: String? = "book",
        chapter: Int = 2,
        currentChunk: SpeedChunk? = chunk,
        expectedBook: String? = "book",
        expectedChapter: Int = 2,
        expectedStart: Int = 10,
        expectedEnd: Int = 15,
    ) = SpeedPlaybackAdvancePolicy.accepts(routeActive, playing, loading, blocked, bookId, chapter,
        currentChunk, expectedBook, expectedChapter, expectedStart, expectedEnd)

    @Test fun currentPlayingFixationCanAdvance() {
        assertTrue(accepts())
    }

    @Test fun aTimerWakingAfterPauseNavigationOrLoadCannotAdvance() {
        assertFalse(accepts(playing = false))
        assertFalse(accepts(routeActive = false))
        assertFalse(accepts(loading = true))
        assertFalse(accepts(blocked = true))
        assertFalse(accepts(currentChunk = null))
    }

    @Test fun anOldBookOrChapterTimerCannotAdvanceTheNewSession() {
        assertFalse(accepts(bookId = "other"))
        assertFalse(accepts(chapter = 3))
        assertFalse(accepts(bookId = null))
        assertFalse(accepts(expectedBook = null))
    }

    @Test fun seekAndFixationResizeInvalidateThePreviousTimer() {
        assertFalse(accepts(currentChunk = SpeedChunk(listOf("другое"), 20, 26, 0)))
        assertFalse(accepts(currentChunk = SpeedChunk(listOf("слово", "ещё"), 10, 19, 0)))
        assertFalse(accepts(currentChunk = SpeedChunk(listOf("во"), 13, 15, 0)))
    }

    @Test fun malformedExpectedRangesNeverAuthorizeAnAdvance() {
        assertFalse(accepts(expectedStart = -1))
        assertFalse(accepts(expectedEnd = 10))
        assertFalse(accepts(expectedEnd = 9))
        assertFalse(accepts(expectedChapter = -1))
    }
}
