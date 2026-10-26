package io.fluxreader.app.reader

import io.fluxreader.app.data.SpeedPacingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedReaderCursorTest {
    @Test
    fun readsTextIncrementallyWithoutWholeBookTokenList() {
        val text = "Первое второе третье четвёрто пятое."
        val first = requireNotNull(SpeedReaderCursor.next(text, 0, 2))
        assertEquals(listOf("Первое", "второе"), first.tokens)
        val second = requireNotNull(SpeedReaderCursor.next(text, first.endOffset, 2))
        assertEquals(listOf("третье", "четвёрто"), second.tokens)
        val third = requireNotNull(SpeedReaderCursor.next(text, second.endOffset, 2))
        assertEquals(listOf("пятое."), third.tokens)
        assertNull(SpeedReaderCursor.next(text, third.endOffset, 2))
    }

    @Test
    fun smartChunkingStopsAtSemanticBoundary() {
        val text = "Он остановился, затем посмотрел в окно. Следующая мысль."
        val first = requireNotNull(SpeedReaderCursor.next(text, 0, 6, smartChunking = true))
        assertEquals(listOf("Он", "остановился,"), first.tokens)
        val second = requireNotNull(SpeedReaderCursor.next(text, first.endOffset, 6, smartChunking = true))
        assertEquals(listOf("затем", "посмотрел", "в", "окно."), second.tokens)
    }

    @Test
    fun smartPhraseDoesNotLeaveShortConnectorDangling() {
        val text = "Она вошла в старый дом и закрыла дверь."
        val first = requireNotNull(SpeedReaderCursor.next(text, 0, 3, smartChunking = true))
        assertEquals(listOf("Она", "вошла", "в", "старый"), first.tokens)
        assertFalse(first.tokens.last().lowercase() in setOf("в", "и", "на", "к", "с"))
    }

    @Test
    fun abbreviationDoesNotBecomeFalseSentencePause() {
        val text = "См. следующую главу."
        val first = requireNotNull(SpeedReaderCursor.next(text, 0, 4, smartChunking = true))
        assertEquals(listOf("См.", "следующую", "главу."), first.tokens)
        val abbreviationOnly = requireNotNull(SpeedReaderCursor.next("См. далее", 0, 1))
        assertFalse(SpeedReaderCursor.hasSemanticPause(abbreviationOnly, "См. далее"))
    }

    @Test
    fun orpSkipsOpeningPunctuation() {
        val token = "«Премьер-министр»"
        val index = SpeedReaderCursor.focusCharacterIndex(token)
        assertTrue(index > 0)
        assertTrue(token[index].isLetter())
    }

    @Test
    fun smartAndDeepGiveMoreTimeToComplexSentenceEnd() {
        val text = "Гиперконфигурационный протокол завершён.\n\nНовый абзац"
        val chunk = requireNotNull(SpeedReaderCursor.next(text, 0, 3, smartChunking = true))
        val constant = SpeedReaderCursor.delayMillis(400, chunk, text, SpeedPacingMode.CONSTANT, true)
        val smart = SpeedReaderCursor.delayMillis(400, chunk, text, SpeedPacingMode.SMART, true)
        val deep = SpeedReaderCursor.delayMillis(400, chunk, text, SpeedPacingMode.DEEP, true)
        assertTrue(smart > constant)
        assertTrue(deep > smart)
    }


    @Test
    fun sentenceMicroPauseIsAddedAfterTheDisplayedSentenceEnd() {
        val text = "Короткое предложение. Следующее"
        val chunk = requireNotNull(SpeedReaderCursor.next(text, 0, 2, smartChunking = true))
        val without = SpeedReaderCursor.delayMillis(500, chunk, text, SpeedPacingMode.SMART, microPausesEnabled = false, paragraphPause = false)
        val withPause = SpeedReaderCursor.delayMillis(
            500, chunk, text, SpeedPacingMode.SMART, paragraphPause = false,
            microPausesEnabled = true, sentencePauseMs = 180, clausePauseMs = 0,
            adaptiveSentencePause = false,
        )
        assertTrue(withPause - without >= 175L)
    }

    @Test
    fun longerSentenceGetsLargerAdaptiveEndPause() {
        val shortText = "Это коротко."
        val longText = "Это заметно более длинное предложение в котором есть много слов чтобы мозг получил дополнительное время на объединение смысла после финальной точки."
        val shortChunk = requireNotNull(SpeedReaderCursor.next(shortText, 0, 7, smartChunking = true))
        var offset = 0
        var longChunk = requireNotNull(SpeedReaderCursor.next(longText, 0, 7, smartChunking = true))
        while (longChunk.endOffset < longText.length) {
            offset = longChunk.endOffset
            longChunk = requireNotNull(SpeedReaderCursor.next(longText, offset, 7, smartChunking = true))
        }
        val shortDelay = SpeedReaderCursor.delayMillis(450, shortChunk, shortText, SpeedPacingMode.SMART, paragraphPause = false, sentenceLengthBonusMs = 200)
        val longDelay = SpeedReaderCursor.delayMillis(450, longChunk, longText, SpeedPacingMode.SMART, paragraphPause = false, sentenceLengthBonusMs = 200)
        assertTrue(longDelay > shortDelay)
    }

    @Test
    fun gradualAccelerationReachesTargetWithoutOvershoot() {
        assertEquals(250, SpeedReaderCursor.rampedWpm(500, 0, enabled = true, startPercent = 50, durationSeconds = 4))
        val halfway = SpeedReaderCursor.rampedWpm(500, 2_000, enabled = true, startPercent = 50, durationSeconds = 4)
        assertTrue(halfway in 251..499)
        assertEquals(500, SpeedReaderCursor.rampedWpm(500, 4_000, enabled = true, startPercent = 50, durationSeconds = 4))
        assertEquals(500, SpeedReaderCursor.rampedWpm(500, 0, enabled = false, startPercent = 50, durationSeconds = 4))
    }

    @Test
    fun handlesLargeCyrillicStream() {
        val text = buildString { repeat(120_000) { append("слово").append(it).append(' ') } }
        var offset = 0
        var chunks = 0
        while (true) {
            val chunk = SpeedReaderCursor.next(text, offset, 3) ?: break
            offset = chunk.endOffset
            chunks++
        }
        assertEquals(40_000, chunks)
        assertTrue(offset <= text.length)
    }
    @Test
    fun sentenceNavigationFindsStableSemanticStarts() {
        val text = "Первое предложение. Второе предложение длиннее! Третье?"
        val secondStart = requireNotNull(SpeedReaderCursor.nextSentenceOffset(text, 5))
        assertEquals(text.indexOf("Второе"), secondStart)
        val thirdStart = requireNotNull(SpeedReaderCursor.nextSentenceOffset(text, secondStart + 8))
        assertEquals(text.indexOf("Третье"), thirdStart)
        assertEquals(secondStart, SpeedReaderCursor.previousSentenceOffset(text, thirdStart))
        assertEquals(secondStart, SpeedReaderCursor.sentenceStartOffset(text, secondStart + 10))
    }

    @Test
    fun paragraphNavigationAndPauseContextStayLocal() {
        val text = "Первый абзац с текстом.\n\nВторой абзац здесь.\n\nТретий абзац."
        val second = requireNotNull(SpeedReaderCursor.nextParagraphOffset(text, 0))
        assertEquals(text.indexOf("Второй"), second)
        val third = requireNotNull(SpeedReaderCursor.nextParagraphOffset(text, second))
        assertEquals(text.indexOf("Третий"), third)
        assertEquals(second, SpeedReaderCursor.previousParagraphOffset(text, third))
        val chunk = requireNotNull(SpeedReaderCursor.next(text, second, 2))
        val context = SpeedReaderCursor.pauseContext(text, chunk.startOffset, chunk.endOffset, wordsEachSide = 3)
        assertTrue(context.before.contains("текстом"))
        assertTrue(context.after.contains("здесь"))
    }

    @Test
    fun adaptivePhraseShrinksForComplexTokensAndCanGrowForShortOnes() {
        val complex = "простые слова гиперконфигурационный HTTPS://EXAMPLE.COM/VERY/LONG/PATH следующие"
        val complexChunk = requireNotNull(
            SpeedReaderCursor.next(complex, 0, 4, adaptiveWordCount = true, pacingMode = SpeedPacingMode.DEEP)
        )
        assertTrue(complexChunk.tokens.size <= 3)

        val simple = "я и ты мы все тут уже давно"
        val simpleChunk = requireNotNull(
            SpeedReaderCursor.next(simple, 0, 3, adaptiveWordCount = true, pacingMode = SpeedPacingMode.SMART)
        )
        assertTrue(simpleChunk.tokens.size >= 3)
    }

    @Test
    fun tappedOffsetSnapsToWholeWordAndPeripheralContextStartsAfterCurrentWord() {
        val text = "Первое второе третье четвёрто"
        val insideSecond = text.indexOf("второе") + 3
        assertEquals(text.indexOf("второе"), SpeedReaderCursor.wordStartOffset(text, insideSecond))
        assertEquals(
            listOf("третье", "четвёрто"),
            SpeedReaderCursor.followingTokens(text, text.indexOf("третье"), 3),
        )
    }

    @Test
    fun pauseTextWindowKeepsAbsoluteOffsetsForTapToSeek() {
        val text = "Первый абзац с текстом.\n\nВторой абзац содержит выбранное слово и продолжение.\n\nТретий."
        val center = text.indexOf("выбранное") + 4
        val window = SpeedReaderCursor.textWindow(text, center, maxChars = 52)
        val local = window.text.indexOf("выбранное") + 2
        assertTrue(local >= 2)
        assertEquals(text.indexOf("выбранное"), window.startOffset + SpeedReaderCursor.wordStartOffset(window.text, local))
    }

    @Test
    fun reedyRangeSupportsThreeThousandWpm() {
        assertEquals(3000, SpeedReaderCursor.rampedWpm(3000, 10_000, enabled = false))
        val chunk = requireNotNull(SpeedReaderCursor.next("быстро", 0, 1))
        assertTrue(SpeedReaderCursor.delayMillis(3000, chunk, "быстро", SpeedPacingMode.CONSTANT) <= 25L)
    }

    @Test fun constantTimingHonorsBothEndsOfTheConfiguredWpmRange() {
        val text = List(7) { "слово" }.joinToString(" ")
        val phrase = requireNotNull(SpeedReaderCursor.next(text, 0, 7))
        assertEquals(4_200L, SpeedReaderCursor.delayMillis(100, phrase, text, SpeedPacingMode.CONSTANT))
        val word = requireNotNull(SpeedReaderCursor.next(text, 0, 1))
        assertEquals(20L, SpeedReaderCursor.delayMillis(3000, word, text, SpeedPacingMode.CONSTANT))
    }

    @Test fun complexWordSlowdownIsNotClippedForLongPhrasesAtLowSpeed() {
        val text = List(7) { "электромагнитный" }.joinToString(" ")
        val phrase = requireNotNull(SpeedReaderCursor.next(text, 0, 7))
        fun delay(percent: Int, mode: SpeedPacingMode) = SpeedReaderCursor.delayMillis(
            100, phrase, text, mode, paragraphPause = false, microPausesEnabled = false,
            longWordSlowdownPercent = percent,
        )
        assertEquals(4_200L, delay(0, SpeedPacingMode.SMART))
        assertTrue(delay(30, SpeedPacingMode.SMART) > delay(0, SpeedPacingMode.SMART))
        assertTrue(delay(60, SpeedPacingMode.SMART) > delay(30, SpeedPacingMode.SMART))
        assertTrue(delay(60, SpeedPacingMode.DEEP) > delay(60, SpeedPacingMode.SMART))
        assertTrue(delay(60, SpeedPacingMode.DEEP) > 7_000L)
    }

    @Test fun aComplexWordStillReceivesExtraTimeAtThreeThousandWpm() {
        val word = requireNotNull(SpeedReaderCursor.next("электромагнитный", 0, 1))
        val plain = SpeedReaderCursor.delayMillis(3000, word, pacingMode = SpeedPacingMode.SMART,
            paragraphPause = false, microPausesEnabled = false, longWordSlowdownPercent = 0)
        val slower = SpeedReaderCursor.delayMillis(3000, word, pacingMode = SpeedPacingMode.SMART,
            paragraphPause = false, microPausesEnabled = false, longWordSlowdownPercent = 18)
        assertTrue(slower > plain)
    }

    @Test fun adaptiveTwoWordFixationCanIsolateOneComplexWord() {
        val text = "электромагнитный протокол готов"
        val chunk = requireNotNull(SpeedReaderCursor.next(text, 0, 2,
            adaptiveWordCount = true, pacingMode = SpeedPacingMode.SMART))
        assertEquals(listOf("электромагнитный"), chunk.tokens)
        assertEquals(listOf("протокол"), SpeedReaderCursor.next(text, chunk.endOffset, 1)?.tokens)
    }

    @Test fun aDanglingConnectorNeverPullsTheNextParagraphIntoTheFixation() {
        val text = "Он думал о\n\u00a0\nбудущем."
        val chunk = requireNotNull(SpeedReaderCursor.next(text, 0, 3, smartChunking = true))
        assertEquals(listOf("Он", "думал", "о"), chunk.tokens)
        assertTrue(SpeedReaderCursor.hasSemanticPause(chunk, text))
        assertEquals(listOf("будущем."), SpeedReaderCursor.next(text, chunk.endOffset, 3, smartChunking = true)?.tokens)
    }

    @Test fun epubNonBreakingSpacesHaveTheSameNavigationAndAdaptiveTokensAsOrdinarySpaces() {
        val text = "Первое.\u00a0Второе\u202fсодержательное\u00a0предложение!\u202fТретье."
        val second = text.indexOf("Второе")
        val third = text.indexOf("Третье")
        assertEquals(second, SpeedReaderCursor.nextSentenceOffset(text, "Первое.".length))
        assertEquals(second, SpeedReaderCursor.sentenceStartOffset(text, text.indexOf("содержательное") + 4))
        assertEquals(second, SpeedReaderCursor.previousSentenceOffset(text, third))
        val short = "я\u00a0и\u202fты\u00a0мы"
        assertEquals(listOf("я", "и", "ты"), SpeedReaderCursor.next(short, 0, 2, adaptiveWordCount = true)?.tokens)
        val paragraphs = "Первый.\r\n\u202f\r\nВторой.\r\n\u00a0\r\nТретий."
        assertEquals(paragraphs.indexOf("Второй"), SpeedReaderCursor.nextParagraphOffset(paragraphs, 0))
        assertEquals(paragraphs.indexOf("Второй"), SpeedReaderCursor.previousParagraphOffset(paragraphs, paragraphs.indexOf("Третий")))
    }

    @Test fun sentenceNavigationNeverTreatsAPartialDecimalAsAFinalPeriod() {
        val text = "Версия 3.14 готова. Потом проверим."
        val inside = text.indexOf("3.14") + 2
        assertEquals(0, SpeedReaderCursor.sentenceStartOffset(text, inside))
        assertEquals(text.indexOf("Потом"), SpeedReaderCursor.nextSentenceOffset(text, inside))
        // A preceding completed sentence must not make a seek inside the next word return mid-word.
        assertNull(SpeedReaderCursor.nextSentenceOffset(text, text.indexOf("Потом") + 2))
    }

    @Test fun closingBracketsAndAsciiEllipsisKeepTheirSemanticPauses() {
        for (closing in listOf("]", "}")) {
            val text = "Всё готово.$closing Следующее предложение."
            val chunk = requireNotNull(SpeedReaderCursor.next(text, 0, 7, smartChunking = true))
            assertEquals(listOf("Всё", "готово.$closing"), chunk.tokens)
            assertTrue(SpeedReaderCursor.hasSemanticPause(chunk, text))
        }
        fun delay(word: String): Long {
            val chunk = requireNotNull(SpeedReaderCursor.next(word, 0, 1))
            return SpeedReaderCursor.delayMillis(400, chunk, word, paragraphPause = false, adaptiveSentencePause = false)
        }
        assertEquals(delay("Пауза…"), delay("Пауза..."))
    }

    @Test fun focusCharacterAvoidsInternalHyphensFractionsAndCombiningAccents() {
        for (token in listOf("а-б", "1/2", "а\u0301")) {
            assertTrue(token, token[SpeedReaderCursor.focusCharacterIndex(token)].isLetterOrDigit())
        }
    }

}
