package io.fluxreader.app.reader

import io.fluxreader.app.data.ReaderSettings
import org.junit.Assert.*
import org.junit.Test

class SpeedDialoguePlaybackTest {
    private val enabled = ReaderSettings(speedDialogueEnabled = true)

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
}
