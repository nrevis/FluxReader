package io.fluxreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderProgressMathTest {
    @Test
    fun characterAnchorRefinesProgressInsideLongBlock() {
        val start = ReaderProgressMath.chapterFraction(
            blockIndex = 2,
            blockCount = 5,
            blockCharOffset = 0,
            blockCharCount = 10_000,
        )
        val middle = ReaderProgressMath.chapterFraction(
            blockIndex = 2,
            blockCount = 5,
            blockCharOffset = 5_000,
            blockCharCount = 10_000,
        )
        assertEquals(0.4f, start, 0.0001f)
        assertEquals(0.5f, middle, 0.0001f)
    }

    @Test
    fun bookProgressCombinesChapterAndLocalFraction() {
        assertEquals(
            0.375f,
            ReaderProgressMath.bookFraction(
                chapterIndex = 1,
                chapterCount = 4,
                chapterFraction = 0.5f,
            ),
            0.0001f,
        )
    }

    @Test
    fun malformedOffsetsAreClamped() {
        assertEquals(0f, ReaderProgressMath.chapterFraction(-9, 3, -50, 100), 0.0001f)
        assertEquals(1f, ReaderProgressMath.bookFraction(99, 3, 1f), 0.0001f)
    }
}
