package io.fluxreader.app.reader

import io.fluxreader.app.data.SpeedPacingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Acceptance checks frozen independently of the alpha.107 timing formula. The examples are single
 * words from Potter 6 (mainly chapter 18); no book passages or book files are included. Frequency in that
 * chapter is provenance, not a dictionary-based judgement of lexical rarity.
 */
class SpeedComplexWordTimingTest {
    private val speeds = listOf(300, 700, 1500)
    private val pauses = listOf(0, 150, 800)
    private val hardNames = listOf("Голпалотта", "Макгонагалл", "Слизнортом", "Флитвиком", "Гермиона")
    private val shortNames = listOf("Флитвик", "Хагрид")
    private val longWords = listOf("идентификации", "трансгрессировать", "воспользовавшегося")
    private val controls = listOf("он", "она", "дверь", "сказал", "просто", "сегодня", "Гарри", "Рон")

    private fun delay(
        tokens: List<String>,
        wpm: Int,
        pauseMs: Int,
        mode: SpeedPacingMode = SpeedPacingMode.SMART,
    ): Long {
        val text = tokens.joinToString(" ")
        return SpeedReaderCursor.delayMillis(
            wpm, SpeedChunk(tokens, 0, text.length, 0), text, mode,
            paragraphPause = false, microPausesEnabled = false,
            complexWordPauseMs = pauseMs,
        )
    }

    private fun extra(tokens: List<String>, wpm: Int, pauseMs: Int, mode: SpeedPacingMode = SpeedPacingMode.SMART): Long =
        delay(tokens, wpm, pauseMs, mode) - delay(tokens, wpm, 0, mode)

    @Test fun realLongNamesReceiveTheConfiguredMillisecondsAtEveryRequestedSpeed() {
        for (name in hardNames) for (wpm in speeds) {
            assertTrue("$name at $wpm WPM / 150 ms", extra(listOf(name), wpm, 150) >= 150L)
            assertTrue("$name at $wpm WPM / 800 ms", extra(listOf(name), wpm, 800) >= 800L)
        }
    }

    @Test fun shortTitlecaseNamesAlsoReceiveNoticeableExtraTime() {
        for (name in shortNames) for (wpm in speeds) for (pause in pauses.drop(1)) {
            assertTrue("$name at $wpm WPM / $pause ms", extra(listOf(name), wpm, pause) >= pause.toLong())
        }
    }

    @Test fun increasingTheSliderStrictlyIncreasesTheActualDelayOfDifficultWords() {
        for (word in hardNames + shortNames + longWords) for (wpm in speeds) {
            val values = pauses.map { delay(listOf(word), wpm, it) }
            assertTrue("$word at $wpm WPM: $values", values[0] < values[1] && values[1] < values[2])
        }
    }

    @Test fun ordinaryShortWordsAndShortCommonNamesDoNotChangeWithTheSlider() {
        for (word in controls) for (wpm in speeds) {
            val baseline = delay(listOf(word), wpm, 0)
            for (pause in pauses) assertEquals("$word at $wpm WPM / $pause ms", baseline, delay(listOf(word), wpm, pause))
        }
    }

    @Test fun threeAndSevenWordPhrasesKeepTheSumOfTheIndividualExtraMilliseconds() {
        val words = listOf("Голпалотта", "дверь", "Хагрид", "сказал", "Макгонагалл", "просто", "трансгрессировать")
        for (count in listOf(3, 7)) for (wpm in speeds) for (pause in pauses) {
            val phrase = words.take(count)
            val expected = phrase.sumOf { extra(listOf(it), wpm, pause) }
            val actual = extra(phrase, wpm, pause)
            assertTrue("$count tokens at $wpm WPM / $pause ms: expected=$expected actual=$actual",
                kotlin.math.abs(actual - expected) <= 2L)
        }
    }

    @Test fun constantPacingIgnoresComplexWordPausesForWordsAndPhrases() {
        val fixtures = (hardNames + shortNames + longWords + controls).map { listOf(it) } +
            listOf(listOf("Голпалотта", "дверь", "Хагрид"), List(7) { "Макгонагалл" })
        for (tokens in fixtures) for (wpm in speeds) {
            val baseline = delay(tokens, wpm, 0, SpeedPacingMode.CONSTANT)
            for (pause in pauses) assertEquals("$tokens at $wpm WPM / $pause ms", baseline,
                delay(tokens, wpm, pause, SpeedPacingMode.CONSTANT))
        }
    }

    @Test fun configuredMillisecondsDoNotDisappearAsWpmIncreases() {
        for (word in hardNames + shortNames + longWords) for (pause in pauses.drop(1)) {
            val values = speeds.map { extra(listOf(word), it, pause) }
            assertTrue("$word / $pause ms across $speeds: $values", values.max() - values.min() <= 2L)
        }
    }

    @Test fun aNameAtTheBeginningOfTheTextGetsTheSameExtraTimeAsInsideAPhrase() {
        for (name in shortNames + hardNames) for (wpm in speeds) for (pause in pauses.drop(1)) {
            val alone = extra(listOf(name), wpm, pause)
            val afterControl = extra(listOf("потом", name), wpm, pause)
            assertTrue("$name at $wpm WPM / $pause ms", kotlin.math.abs(alone - afterControl) <= 1L)
        }
    }

    @Test fun surroundingQuotesAndPunctuationDoNotMasqueradeAsComplexWordExtraTime() {
        for (name in listOf("Голпалотта", "Флитвик")) for (wpm in speeds) for (pause in pauses.drop(1)) {
            val plain = extra(listOf(name), wpm, pause)
            for (decorated in listOf("«$name»,", "\"$name\"!", "($name)", "$name…")) {
                assertTrue("$decorated at $wpm WPM / $pause ms",
                    kotlin.math.abs(plain - extra(listOf(decorated), wpm, pause)) <= 1L)
            }
        }
    }

    @Test fun compoundNamesRetainTheConfiguredPauseWithoutBeingSplitIntoFakeTokens() {
        for (name in listOf("Волан-де-Морту", "Волан-де-Морт")) for (wpm in speeds) for (pause in pauses.drop(1)) {
            val chunk = SpeedReaderCursor.next(name, 0, 1, smartChunking = false, adaptiveWordCount = false)!!
            assertEquals(listOf(name), chunk.tokens)
            assertTrue("$name at $wpm WPM / $pause ms", extra(chunk.tokens, wpm, pause) >= pause.toLong())
        }
    }

    @Test fun unicodeWhitespacePreservesTokensAndTheirTimingContributions() {
        val words = listOf("Голпалотта", "дверь", "Хагрид")
        for (separator in listOf(" ", "\u00a0", "\u202f", "\u2003", "\t", "\n")) {
            val text = words.joinToString(separator)
            val chunk = SpeedReaderCursor.next(text, 0, 3, smartChunking = false, adaptiveWordCount = false)!!
            assertEquals("Whitespace U+${separator[0].code.toString(16)}", words, chunk.tokens)
            for (wpm in speeds) for (pause in pauses) {
                fun actual(ms: Int) = SpeedReaderCursor.delayMillis(wpm, chunk, text, SpeedPacingMode.SMART,
                    paragraphPause = false, microPausesEnabled = false, complexWordPauseMs = ms)
                assertEquals("Whitespace U+${separator[0].code.toString(16)} / $wpm / $pause",
                    extra(words, wpm, pause), actual(pause) - actual(0))
            }
        }
    }

    @Test fun lengthLevelsDoNotMakeTheLongestWordFasterThanAMediumDifficultWord() {
        assertEquals(13, longWords[0].length)
        assertEquals(17, longWords[1].length)
        assertEquals(18, longWords[2].length)
        for (wpm in speeds) for (pause in pauses.drop(1)) {
            val values = longWords.map { extra(listOf(it), wpm, pause) }
            assertTrue("$longWords at $wpm WPM / $pause ms: $values", values[0] <= values[1] && values[1] <= values[2])
            assertTrue(values.all { it >= pause.toLong() })
        }
    }

    @Test fun deepPacingPreservesTheConfiguredComplexPause() {
        for (name in hardNames + shortNames) for (wpm in speeds) for (pause in pauses.drop(1)) {
            val smart = extra(listOf(name), wpm, pause, SpeedPacingMode.SMART)
            val deep = extra(listOf(name), wpm, pause, SpeedPacingMode.DEEP)
            assertTrue("$name at $wpm WPM / $pause ms", deep >= pause.toLong() && deep >= smart - 1L)
        }
    }
}
