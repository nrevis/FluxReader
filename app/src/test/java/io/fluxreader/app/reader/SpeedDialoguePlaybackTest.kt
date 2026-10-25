package io.fluxreader.app.reader

import io.fluxreader.app.data.ReaderSettings
import org.junit.Assert.*
import org.junit.Test

class SpeedDialoguePlaybackTest {
    private val enabled = ReaderSettings(speedDialogueEnabled = true)
    private val rapidSpeech = "Третий-закон-Голпалотта-гласит-что-противоядие-от-составного-зелья-не-сводится-к-набору-противоядий-для-отдельных-его-компонентов."

    @Test fun rapidSpeechIsReadableWithoutLosingAnyPartOrOriginalOffset() {
        val text = "— $rapidSpeech\n\nОна закрыла книгу."
        var offset = 0
        val observed = mutableListOf<String>()
        while (true) {
            val chunk = SpeedDialoguePlayback.next(text, offset, enabled, 1) ?: break
            assertTrue(chunk.endOffset > offset)
            if (chunk.startOffset < text.indexOf("Она")) {
                assertEquals(chunk.tokens.single(), text.substring(chunk.startOffset, chunk.endOffset).trimEnd('-'))
            }
            observed += chunk.tokens
            offset = chunk.endOffset
        }
        assertEquals(rapidSpeech.split('-') + listOf("Она", "закрыла", "книгу."), observed)
    }

    @Test fun resumeInsideRapidSpeechKeepsTheCurrentPart() {
        val text = "— $rapidSpeech"
        val inside = text.indexOf("противоядие") + 3
        val start = SpeedDialoguePlayback.wordStartOffset(text, inside, enabled)
        assertEquals(text.indexOf("противоядие"), start)
        assertEquals(listOf("противоядие"), SpeedDialoguePlayback.next(text, start, enabled, 1)?.tokens)
        assertEquals(text.indexOf("Третий"), SpeedDialoguePlayback.wordStartOffset(text, inside, ReaderSettings()))
    }

    @Test fun rapidSpeechAfterOrdinaryWordsIsNotMergedIntoAShortReply() {
        val text = "— Внимание: $rapidSpeech"
        val first = SpeedDialoguePlayback.next(text, 0, enabled, 1)!!
        assertEquals(listOf("Внимание:"), first.tokens)
        assertEquals(listOf("Третий"), SpeedDialoguePlayback.next(text, first.endOffset, enabled, 1)?.tokens)
        val phrase = enabled.copy(speedMode = io.fluxreader.app.data.SpeedMode.PHRASE)
        val initial = SpeedDialoguePlayback.next(text, 0, phrase, 7)!!
        assertEquals(listOf("Внимание:"), initial.tokens)
        assertEquals(rapidSpeech.split('-').take(7), SpeedDialoguePlayback.next(text, initial.endOffset, phrase, 7)?.tokens)
    }

    @Test fun regularHyphenatedWordsAndNarrationRemainIntact() {
        assertEquals(listOf("Кто-то"), SpeedDialoguePlayback.next("— Кто-то пришёл домой вчера вечером очень поздно.", 0, enabled, 1)?.tokens)
        assertEquals(listOf(rapidSpeech), SpeedDialoguePlayback.next(rapidSpeech, 0, enabled, 1)?.tokens)
        val text = "— $rapidSpeech"
        val disabled = ReaderSettings(speedDialogueEnabled = false)
        assertEquals(SpeedReaderCursor.next(text, 0, 1, smartChunking = true), SpeedDialoguePlayback.next(text, 0, disabled, 1))
    }

    @Test fun pauseWindowIncludesLongHyphenatedParagraphInsteadOfPreviousScene() {
        val text = "Предыдущая сцена.\n\n— $rapidSpeech\n\nСледующая сцена."
        val start = text.indexOf("Третий")
        val window = SpeedReaderCursor.textWindow(text, start, 700)
        assertEquals("— $rapidSpeech", window.text)
        assertEquals(text.indexOf('—'), window.startOffset)
    }

    @Test fun fullShortReplySurvivesLeadingDashAndComma() {
        val text = "— Садитесь, прошу вас! — сказал Павел."
        val chunk = requireNotNull(SpeedDialoguePlayback.next(text, 0, enabled, 1))
        assertEquals(listOf("Садитесь,", "прошу", "вас!"), chunk.tokens)
        assertEquals(text.indexOf("Садитесь"), chunk.startOffset)
        assertEquals(text.indexOf(" — сказал"), chunk.endOffset)
        assertTrue(SpeedDialogueAnalyzer.isReplyStart(text, chunk.startOffset))
        val narration = requireNotNull(SpeedDialoguePlayback.next(text, chunk.endOffset, enabled, 1))
        assertEquals(listOf("сказал"), narration.tokens)
        assertNull(SpeedDialogueAnalyzer.at(text, narration.startOffset))
    }

    @Test fun longReplyStartsWithAWordAndRetainsAllLexicalTokens() {
        val text = "— Садитесь, садитесь, прошу вас! И побыстрее, сегодня у нас много работы!\n— …то отсюда следует, что всё получится."
        var offset = 0
        val observed = mutableListOf<String>()
        while (true) {
            val chunk = SpeedDialoguePlayback.next(text, offset, enabled, 1) ?: break
            assertTrue(chunk.endOffset > offset)
            assertTrue(chunk.tokens.all { token -> token.any(Char::isLetterOrDigit) })
            observed += chunk.tokens
            offset = chunk.endOffset
        }
        assertEquals(Regex("\\S+").findAll(text).map { it.value }.filter { t -> t.any(Char::isLetterOrDigit) }.toList(), observed)
    }

    @Test fun disabledExperimentKeepsExistingPlayback() {
        val text = "— Садитесь, прошу вас!"
        val old = SpeedReaderCursor.next(text, 0, 1, smartChunking = true)
        assertEquals(old, SpeedDialoguePlayback.next(text, 0, ReaderSettings(speedDialogueEnabled = false), 1))
    }

    @Test fun sevenActualWordsAreNotMergedIntoAShortReply() {
        val text = "— Один два три четыре пять шесть семь."
        assertEquals(listOf("Один"), SpeedDialoguePlayback.next(text, 0, enabled, 1)?.tokens)
    }

    @Test fun punctuationIsNotAnOrpLetter() {
        val token = "…то"
        assertTrue(token[SpeedReaderCursor.focusCharacterIndex(token)].isLetter())
    }
    @org.junit.Test fun phraseFixationNeverCrossesAttributionAndRetainsAllLexicalWords() {
        val text = "— Ты точно придёшь, — спросила Анна, — завтра вечером? Потом она вышла."
        val config = io.fluxreader.app.data.ReaderSettings(speedDialogueEnabled = true,
            speedMode = io.fluxreader.app.data.SpeedMode.PHRASE, speedSmartChunking = false,
            speedAdaptiveChunkSize = false)
        val observed = mutableListOf<String>()
        var offset = 0
        while (offset < text.length) {
            val chunk = SpeedDialoguePlayback.next(text, offset, config, 7) ?: break
            org.junit.Assert.assertTrue(chunk.endOffset > offset)
            val identities = Regex("\\S+").findAll(text.substring(chunk.startOffset, chunk.endOffset))
                .filter { it.value.any(Char::isLetterOrDigit) }
                .map { SpeedDialogueAnalyzer.at(text, chunk.startOffset + it.range.first) }.toList()
            org.junit.Assert.assertTrue(identities.distinct().size <= 1)
            observed.addAll(chunk.tokens)
            offset = chunk.endOffset
        }
        org.junit.Assert.assertEquals(Regex("\\S+").findAll(text).map { it.value }
            .filter { it.any(Char::isLetterOrDigit) }.toList(), observed)
    }

    @org.junit.Test fun compactEpubMarkerIsCutAtOriginalCharacterBoundary() {
        val text = "- Да, капитан,- ответил Павел.- Скоро вернусь."
        val config = io.fluxreader.app.data.ReaderSettings(speedDialogueEnabled = true,
            speedMode = io.fluxreader.app.data.SpeedMode.PHRASE, speedSmartChunking = false,
            speedAdaptiveChunkSize = false)
        val speech = SpeedDialoguePlayback.next(text, 0, config, 7)!!
        org.junit.Assert.assertEquals(listOf("Да,", "капитан,"), speech.tokens)
        org.junit.Assert.assertEquals(text.indexOf("- ответил"), speech.endOffset)
        val author = SpeedDialoguePlayback.next(text, speech.endOffset, config, 1)!!
        org.junit.Assert.assertEquals(listOf("ответил"), author.tokens)
        org.junit.Assert.assertEquals(text.indexOf("ответил"), author.startOffset)
    }

}
