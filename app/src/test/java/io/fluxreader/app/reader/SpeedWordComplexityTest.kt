package io.fluxreader.app.reader

import io.fluxreader.app.data.SpeedPacingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedWordComplexityTest {
    @Test fun theReportedNamesReceiveAnObservableExtraWindowIncludingShortNames() {
        for (name in listOf("Дамблдор", "Слизнорт", "Ксенофилиус", "Макгонагалл", "Хагрид", "Флитвик")) {
            assertTrue(name, SpeedWordComplexity.score(name) >= 1.0)
            val chunk = SpeedChunk(listOf(name), 0, name.length, 0)
            val plain = delay(chunk, 1500, 0)
            assertTrue(name, delay(chunk, 1500, 150) - plain >= 150L)
        }
    }

    @Test fun shortOrdinaryWordsAndOrdinarySmallHyphenatedWordsHaveNoSurcharge() {
        for (word in listOf("дом", "слово", "книга", "сегодня", "просто", "хорошо", "кто-то", "из-за")) {
            assertEquals(word, 0.0, SpeedWordComplexity.score(word), .0)
            val chunk = SpeedChunk(listOf(word), 0, word.length, 0)
            assertEquals(word, delay(chunk, 700, 0), delay(chunk, 700, 800))
        }
    }

    @Test fun lengthTiersAndAdditionalSpellingSignalsAreBoundedAndIgnoreWrappingPunctuation() {
        assertTrue(SpeedWordComplexity.score("протокол") >= 1.0)
        assertTrue(SpeedWordComplexity.score("распознавание") >= 1.5)
        assertEquals(2.0, SpeedWordComplexity.score("гиперконфигурационный"), .0)
        assertTrue(SpeedWordComplexity.score("всплеск") >= 1.0)
        assertTrue(SpeedWordComplexity.score("северо-восточный") >= 1.0)
        assertTrue(SpeedWordComplexity.score("2026") >= 1.0)
        assertTrue(SpeedWordComplexity.score("https://example.com") >= 1.5)
        assertEquals(SpeedWordComplexity.score("Дамблдор"), SpeedWordComplexity.score("«Дамблдор,»"), .0)
        for (token in listOf("А".repeat(10_000), "1".repeat(10_000), "слово-".repeat(1_000), "…", "")) {
            assertTrue(SpeedWordComplexity.score(token) in 0.0..2.0)
        }
    }

    @Test fun millisecondsRemainEquallyObservableAtEverySupportedReadingSpeed() {
        val word = SpeedChunk(listOf("Слизнорт"), 0, 8, 0)
        for (wpm in listOf(250, 350, 700, 1500, 3000)) {
            val plain = delay(word, wpm, 0)
            assertEquals("wpm=$wpm", 150L, delay(word, wpm, 150) - plain)
            assertEquals("wpm=$wpm", 600L, delay(word, wpm, 600) - plain)
        }
    }

    @Test fun eachComplexTokenGetsItsOwnSurchargeInAPhrase() {
        val words = listOf("Хагрид", "Слизнорт", "Макгонагалл")
        val chunk = SpeedChunk(words, 0, words.joinToString(" ").length, 1)
        assertEquals(450L, delay(chunk, 350, 150) - delay(chunk, 350, 0))
        assertEquals(1_800L, delay(chunk, 350, 600) - delay(chunk, 350, 0))
    }

    @Test fun zeroDisablesAllComplexityCuesAndConstantPacingAlwaysKeepsFixedWpm() {
        val chunk = SpeedChunk(listOf("Хагрид", "2026", "user@example.com"), 0, 28, 0)
        assertEquals(360L, delay(chunk, 500, 0))
        assertEquals(delay(chunk, 500, 150), delay(chunk, 500, 150, SpeedPacingMode.DEEP))
        assertEquals(360L, delay(chunk, 500, 800, SpeedPacingMode.CONSTANT))
    }

    @Test fun changingComplexityTimeDoesNotChangeDeepSemanticPauseScaling() {
        val chunk = SpeedChunk(listOf("Слизнорт."), 0, 9, 0)
        fun sentence(mode: SpeedPacingMode) = SpeedReaderCursor.delayMillis(350, chunk, pacingMode = mode,
            paragraphPause = false, microPausesEnabled = true, adaptiveSentencePause = false,
            sentencePauseMs = 200, complexWordPauseMs = 150)
        assertEquals(56L, sentence(SpeedPacingMode.DEEP) - sentence(SpeedPacingMode.SMART))
    }

    @Test fun adaptiveFixationsUseTheSameShortNameComplexitySignal() {
        val chunk = SpeedReaderCursor.next("Хагрид взял книгу", 0, 2, adaptiveWordCount = true)!!
        assertEquals(listOf("Хагрид"), chunk.tokens)
    }

    private fun delay(chunk: SpeedChunk, wpm: Int, extraMs: Int,
        mode: SpeedPacingMode = SpeedPacingMode.SMART) = SpeedReaderCursor.delayMillis(wpm, chunk,
        pacingMode = mode, paragraphPause = false, microPausesEnabled = false, complexWordPauseMs = extraMs)
}
