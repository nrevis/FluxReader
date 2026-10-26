package io.fluxreader.app.reader

import org.junit.Assert.*
import org.junit.Test

class SpeedPausePreviewWindowTest {
    private fun fits(columns: Int, lines: Int): (SpeedTextWindow) -> Boolean = { window ->
        window.text.split('\n').sumOf { line ->
            (line.codePointCount(0, line.length).coerceAtLeast(1) + columns - 1) / columns
        } <= lines
    }

    private fun assertOriginal(text: String, window: SpeedTextWindow, start: Int, end: Int) {
        assertEquals(window.text, text.substring(window.startOffset, window.startOffset + window.text.length))
        assertTrue("focus start missing: $start / $window", window.startOffset <= start)
        assertTrue("focus end missing: $end / $window", window.startOffset + window.text.length >= end)
        assertEquals(text.substring(start, end), window.text.substring(start - window.startOffset, end - window.startOffset))
    }

    @Test fun aLongParagraphTracksTheCurrentWordInsteadOfItsOpening() {
        val text = "С добрым утром. " + List(85) { "предложение$it" }.joinToString(" ") + " Двукрест. " +
            List(85) { "продолжение$it" }.joinToString(" ")
        val start = text.indexOf("Двукрест.")
        val window = SpeedPausePreviewWindow.fit(text, start, start + 9, fits = fits(22, 3))
        assertOriginal(text, window, start, start + 9)
        assertTrue(fits(22, 3)(window))
        assertFalse(window.text.contains("С добрым утром"))
        assertTrue(window.startOffset > text.length / 4)
    }

    @Test fun startMiddleAndEndAllRetainTheirWholeFixation() {
        val text = List(180) { "слово$it" }.joinToString(" ")
        for (word in listOf("слово0", "слово89", "слово179")) {
            val start = text.indexOf(word)
            val window = SpeedPausePreviewWindow.fit(text, start, start + word.length, fits = fits(20, 4))
            assertOriginal(text, window, start, start + word.length)
            assertTrue(fits(20, 4)(window))
            assertTrue(window.text.length <= 80)
        }
    }

    @Test fun theSingleLetterAtAnAdvancedPositionIsNotLostBehindOldContext() {
        val text = List(110) { "длинный$it" }.joinToString(" ") + " К южному окну подошли гости."
        val start = text.indexOf("К южному")
        val window = SpeedPausePreviewWindow.fit(text, start, start + 1, fits = fits(14, 3))
        assertOriginal(text, window, start, start + 1)
        assertTrue(fits(14, 3)(window))
        assertTrue(window.startOffset > start - 50)
    }

    @Test fun changingTheChunkEndAtTheSameStartRetainsEveryNewWord() {
        val text = "Начало. " + List(25) { "добавочное$it" }.joinToString(" ") + " Конец."
        val start = text.indexOf("добавочное10")
        val shortEnd = start + "добавочное10".length
        val longEnd = text.indexOf("добавочное17") - 1
        val shortWindow = SpeedPausePreviewWindow.fit(text, start, shortEnd, fits = fits(18, 3))
        val longWindow = SpeedPausePreviewWindow.fit(text, start, longEnd, fits = fits(18, 3))
        assertOriginal(text, shortWindow, start, shortEnd)
        assertOriginal(text, longWindow, start, longEnd)
        assertEquals(text.substring(start, longEnd), longWindow.text)
    }

    @Test fun narrowWidthsAndLargeFontsDiscardContextBeforeActiveWords() {
        val text = List(30) { "прошлое" }.joinToString(" ") + " важная новая фиксация " + List(30) { "будущее" }.joinToString(" ")
        val start = text.indexOf("важная")
        val end = start + "важная новая фиксация".length
        // Fewer columns model a narrower viewport or a larger accessibility font.
        for (columns in listOf(40, 20, 10)) {
            val window = SpeedPausePreviewWindow.fit(text, start, end, fits = fits(columns, 3))
            assertOriginal(text, window, start, end)
            assertTrue(fits(columns, 3)(window))
        }
    }

    @Test fun anOversizedFixationStaysIntactEvenWhenItNeedsAdditionalLines() {
        val text = "Раньше. " + List(7) { "длинноеслово$it" }.joinToString(" ") + " Потом."
        val start = text.indexOf("длинноеслово0")
        val end = text.indexOf(" Потом")
        val window = SpeedPausePreviewWindow.fit(text, start, end, maxChars = 40, fits = fits(10, 3))
        assertEquals(text.substring(start, end), window.text)
        assertOriginal(text, window, start, end)
        assertFalse(fits(10, 3)(window))
    }

    @Test fun aMultiTokenFixationMayCrossSourceNewlinesWithoutLosingOffsets() {
        val text = "Ранее.\n\nОчень важная\u00a0новая\nфиксация из нескольких слов.\n\nПозже."
        val start = text.indexOf("важная")
        val end = text.indexOf(" слов")
        val window = SpeedPausePreviewWindow.fit(text, start, end, fits = fits(18, 4))
        assertOriginal(text, window, start, end)
        assertFalse(window.text.contains("Ранее"))
        assertFalse(window.text.contains("Позже"))
    }

    @Test fun unicodeWhitespaceAndTapOffsetsStayIdenticalToTheChapter() {
        val text = "Начало.\n\nперед\u00a0важным\u202fсловом\tнаходится\u2003нужная фиксация после."
        val start = text.indexOf("нужная")
        val end = start + "нужная фиксация".length
        val window = SpeedPausePreviewWindow.fit(text, start, end, fits = fits(20, 3))
        assertOriginal(text, window, start, end)
        for (token in Regex("[^\\s\\p{Zs}\\p{Zl}\\p{Zp}\\u001C-\\u001F]+").findAll(window.text)) {
            val sourceOffset = window.startOffset + token.range.first
            assertEquals(token.value, text.substring(sourceOffset, sourceOffset + token.value.length))
            assertEquals(sourceOffset, SpeedReaderCursor.wordStartOffset(text, sourceOffset))
        }
    }

    @Test fun contextNeverLeaksAnEarlierParagraphIntoALaterFixation() {
        val text = "Старая сцена с Ниной.\n\n" + List(100) { "новая$it" }.joinToString(" ") + " Марк ответил."
        val start = text.indexOf("новая90")
        val window = SpeedPausePreviewWindow.fit(text, start, start + "новая90".length, fits = fits(24, 4))
        assertFalse(window.text.contains("Старая сцена"))
        assertFalse(window.text.contains("Ниной"))
        assertOriginal(text, window, start, start + "новая90".length)
    }

    @Test fun identicalVisibleWordsStillHaveDifferentAbsoluteWindowsWhenReadingAdvances() {
        val text = List(220) { "слово" }.joinToString(" ")
        val a = 60 * "слово ".length
        val b = 160 * "слово ".length
        val first = SpeedPausePreviewWindow.fit(text, a, a + 5, fits = fits(20, 3))
        val second = SpeedPausePreviewWindow.fit(text, b, b + 5, fits = fits(20, 3))
        assertOriginal(text, first, a, a + 5)
        assertOriginal(text, second, b, b + 5)
        assertTrue(second.startOffset > first.startOffset)
    }

    @Test fun surrogatePairsAreNotCutAtWindowOrFixationBoundaries() {
        val text = "Раньше " + "🌙 ".repeat(25) + "нужная 🌟 фиксация " + "🌙 ".repeat(25) + "позже"
        val star = text.indexOf("🌟")
        for ((start, end) in listOf(star to star + 2, star + 1 to star + 2, star to star + 1)) {
            val window = SpeedPausePreviewWindow.fit(text, start, end, fits = fits(9, 3))
            assertOriginal(text, window, start, end)
            assertFalse(window.text.first().isLowSurrogate())
            assertFalse(window.text.last().isHighSurrogate())
            assertTrue(window.text.contains("🌟"))
        }
    }

    @Test fun aHyphenRunFixationKeepsItsOwnPartsWithoutJumpingToTheTokenOpening() {
        val runText = "Первый-второй-третий-четвёртый-пятый-шестой-седьмой-восьмой-девятый-десятый."
        val text = "— $runText — сказала Нина."
        val start = text.indexOf("седьмой")
        val chunk = SpeedDialogueHyphenRun.at(text, start)!!.chunk(text, start, 2)
        val window = SpeedPausePreviewWindow.fit(text, chunk.startOffset, chunk.endOffset, fits = fits(12, 3))
        assertOriginal(text, window, chunk.startOffset, chunk.endOffset)
        assertTrue(window.text.contains("седьмой-восьмой"))
        assertFalse(window.text.contains("Первый"))
    }

    @Test fun currentSpeechNameAndColorComeFromTheCurrentOriginalOffsets() {
        val first = List(65) { "ранее" }.joinToString(" ")
        val second = List(65) { "позже" }.joinToString(" ")
        val text = "— $first, — сказала Нина.\n\n— $second готово, — ответил Марк."
        val colors = BookSpeakerColors(entries = mapOf(
            speakerColorKey("Нина") to SpeakerColorEntry("Нина", SpeakerTint.ROSE),
            speakerColorKey("Марк") to SpeakerColorEntry("Марк", SpeakerTint.GREEN),
        ))
        val start = text.indexOf("готово")
        val window = SpeedPausePreviewWindow.fit(text, start, start + 6, fits = fits(14, 3))
        val local = start - window.startOffset
        val segments = DialogueTextColoring(text, BookDialogueAnalyzer.local(text), colors = colors)
            .segments(window.startOffset, window.startOffset + window.text.length)
        val active = segments.single { local in it.start until it.endExclusive }
        assertEquals("Марк", active.speaker)
        assertEquals(SpeakerTint.GREEN, active.tint)
        assertFalse(window.text.contains("Нина"))
    }

    @Test fun emptyAndBlankChaptersHaveNoPreview() {
        for (text in listOf("", " ", "\n\t\u00a0")) {
            assertEquals(SpeedTextWindow("", 0), SpeedPausePreviewWindow.fit(text, 0, 0, fits = { true }))
        }
    }
}
