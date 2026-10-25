package io.fluxreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedDialogueAnalyzerTest {
    @Test fun resolvesExplicitSpeakerAndIgnoresAttributionProse() {
        val chapter = "— Привет! — сказала Анна.\n— Здравствуй! — ответил Павел."
        assertEquals("Анна", SpeedDialogueAnalyzer.at(chapter, 0)?.speaker)
        val second = chapter.indexOf("— Здравствуй")
        assertEquals("Павел", SpeedDialogueAnalyzer.at(chapter, second)?.speaker)
        assertNull(SpeedDialogueAnalyzer.at(chapter, chapter.indexOf("сказала")))
        assertNull(SpeedDialogueAnalyzer.at(chapter, chapter.indexOf("ответил")))
    }

    @Test fun quotedSpeechRequiresAttribution() {
        val chapter = "Анна сказала: «Пойдём домой!» Он согласился."
        assertEquals("Анна", SpeedDialogueAnalyzer.at(chapter, chapter.indexOf('«'))?.speaker)
        assertNull(SpeedDialogueAnalyzer.at(chapter, chapter.indexOf("Он согласился")))
        assertNull(SpeedDialogueAnalyzer.at("Он читал «Войну и мир».", 9))
    }

    @Test fun unnamedSpeakerRemainsUnnamed() {
        val chapter = "— Не уходи!\nОна отвернулась."
        assertNull(SpeedDialogueAnalyzer.at(chapter, 0)?.speaker)
        assertNull(SpeedDialogueAnalyzer.at(chapter, chapter.indexOf("Она")))
    }

    @Test fun shortReplyNeverConsumesNarration() {
        assertEquals(1, SpeedDialogueAnalyzer.shortReplyWordCount("— Нет. — ответил Павел.", 0))
        assertEquals(3, SpeedDialogueAnalyzer.shortReplyWordCount("— Ну как дела? — спросил Павел.", 0))
        assertNull(SpeedDialogueAnalyzer.shortReplyWordCount("— Просто длинная незавершённая реплика", 0))
        assertNull(SpeedDialogueAnalyzer.shortReplyWordCount("— Сегодня мы совершенно не планируем идти за грибами!", 0))
    }

    @Test fun switchingFixationsDoesNotLoseOrDuplicateOriginalTokens() {
        val chapter = "— Да! — сказала Анна.\n— А ты уверен, что всё получится? — спросил Павел.\nПосле разговора они вышли."
        var offset = 0
        val observed = mutableListOf<String>()
        while (true) {
            var nextNonWhitespace = offset
            while (nextNonWhitespace < chapter.length && chapter[nextNonWhitespace].isWhitespace()) nextNonWhitespace++
            val replyWords = if (nextNonWhitespace < chapter.length) SpeedDialogueAnalyzer.shortReplyWordCount(chapter, nextNonWhitespace) else null
            val chunk = SpeedReaderCursor.next(chapter, offset, replyWords ?: 1, smartChunking = replyWords != null) ?: break
            assertTrue(chunk.endOffset > offset)
            observed.addAll(chunk.tokens)
            offset = chunk.endOffset
            assertTrue(observed.size < 200)
        }
        assertEquals(Regex("\\S+").findAll(chapter).map { it.value }.toList(), observed)
        assertFalse(SpeedDialogueAnalyzer.isReplyStart(chapter, chapter.indexOf("сказала")))
    }

    @Test fun excessivelyLongLinesAreSafelySkipped() {
        val unbroken = "— " + "а".repeat(9000)
        assertNull(SpeedDialogueAnalyzer.at(unbroken, 5000))
    }
}
