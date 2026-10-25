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
    @Test fun nonBreakingSpacesAndFullNamesAreRecognised() {
        val text = "–\u00a0Понятно,\u00a0– так же тихо ответил Пётр Соколов."
        val span = SpeedDialogueAnalyzer.at(text, text.indexOf("Понятно"))!!
        assertEquals("Пётр Соколов", span.speaker)
        assertEquals("–\u00a0Понятно,", text.substring(span.start, span.endExclusive).trim())
        assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf("тихо")))
    }

    @Test fun interruptedReplyResumesWithSameSpeakerWithoutSecondReplyPause() {
        val text = "— Да, — сказала Анна Соколова, — я обязательно приду."
        assertEquals("Анна Соколова", SpeedDialogueAnalyzer.at(text, text.indexOf("Да"))?.speaker)
        val resumed = text.indexOf("я обязательно")
        assertEquals("Анна Соколова", SpeedDialogueAnalyzer.at(text, resumed)?.speaker)
        assertTrue(SpeedDialogueAnalyzer.at(text, resumed)!!.continuation)
        assertFalse(SpeedDialogueAnalyzer.isReplyStart(text, resumed))
        assertNull(SpeedDialogueAnalyzer.shortReplyWordCount(text, resumed))
        assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf("сказала")))
    }

    @Test fun compactTypographyDoesNotAbsorbAuthorRemark() {
        val text = "- Возможно,- коротко ответил Павел.- Но решение ещё впереди."
        assertEquals("Павел", SpeedDialogueAnalyzer.at(text, text.indexOf("Возможно"))?.speaker)
        assertEquals("Павел", SpeedDialogueAnalyzer.at(text, text.indexOf("Но решение"))?.speaker)
        assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf("коротко")))
    }

    @Test fun explicitIntroductionCanBeInPreviousParagraph() {
        val text = "Анна спросила:\n\n— Ты придёшь?\n\n— Возможно."
        assertEquals("Анна", SpeedDialogueAnalyzer.at(text, text.indexOf("Ты придёшь"))?.speaker)
        assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf("Возможно"))?.speaker)
    }

    @Test fun eachQuotedReplyUsesItsOwnAttribution() {
        val text = "Анна сказала: «Да». Павел спросил: «Почему?»"
        assertEquals("Анна", SpeedDialogueAnalyzer.at(text, text.indexOf("Да"))?.speaker)
        assertEquals("Павел", SpeedDialogueAnalyzer.at(text, text.indexOf("Почему"))?.speaker)
        val ascii = "\"Вернусь завтра!\", — продолжил Николай."
        assertEquals("Николай", SpeedDialogueAnalyzer.at(ascii, 0)?.speaker)
    }

    @Test fun titlesPronounsAndListsDoNotBecomeNamedCharacters() {
        val titles = "Анна сказала, что прочитала «Первый снег»."
        assertNull(SpeedDialogueAnalyzer.at(titles, titles.indexOf("Первый")))
        for (text in listOf("— Хорошо, — ответил он.", "— Хорошо, — Он ответил тихо.")) {
            assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf("Хорошо"))?.speaker)
        }
        assertNull(SpeedDialogueAnalyzer.at("- 12 страниц", 0))
        assertNull(SpeedDialogueAnalyzer.at("- ---", 0))
        assertNull(SpeedDialogueAnalyzer.at("Время — деньги.", 8))
    }

    @Test fun actionRemarkAfterReplyIsNotSpeech() {
        val text = "— До завтра! — Анна улыбнулась."
        assertEquals("Анна", SpeedDialogueAnalyzer.at(text, 0)?.speaker)
        assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf("улыбнулась")))
    }

    @Test fun adverbsAndAddresseesAreNotSpeakerNames() {
        for (intro in listOf("Наконец он сказал:", "Подумав, он спросил:", "Не торопясь, он продолжил:")) {
            val text = "$intro\n\n— Ты согласен?"
            assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf("Ты согласен"))?.speaker)
        }
        val text = "— Да, — сказал Павел Анне."
        assertEquals("Павел", SpeedDialogueAnalyzer.at(text, 0)?.speaker)
        val prefixed = "Наконец Селдон спросил:\n— Почему?"
        assertEquals("Селдон", SpeedDialogueAnalyzer.at(prefixed, prefixed.indexOf("Почему"))?.speaker)
    }

    @Test fun scholarlyListAndIndirectQuotationAreNotDialogue() {
        for (text in listOf("– равносторонний", "– равнобедренный")) assertNull(SpeedDialogueAnalyzer.at(text, 0))
        val text = "Автор говорил, что есть два рода понятий: «общее» и «частное»."
        assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf("общее")))
    }

    @Test fun quotedSpeechMayContainItsOwnAttribution() {
        val text = "«Да, — сказала Анна, — я приду завтра.»"
        assertEquals("Анна", SpeedDialogueAnalyzer.at(text, text.indexOf("Да"))?.speaker)
        assertEquals("Анна", SpeedDialogueAnalyzer.at(text, text.indexOf("я приду"))?.speaker)
        assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf("сказала")))
        assertFalse(SpeedDialogueAnalyzer.isReplyStart(text, text.indexOf("я приду")))
    }

    @Test fun typographicDashMayTouchTheFirstWord() {
        val text = "—Привет! —сказал Павел."
        assertEquals("Павел", SpeedDialogueAnalyzer.at(text, 1)?.speaker)
        assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf("сказал")))
    }

    @Test fun explicitAttributionDisambiguatesNumbersAndLowerCaseReplies() {
        for (text in listOf("— 250, — ответил Павел.", "— хорошо, — ответил Павел.")) {
            assertEquals("Павел", SpeedDialogueAnalyzer.at(text, 2)?.speaker)
        }
        val text = "Мягко ответил он:\n— Приходи завтра."
        assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf("Приходи"))?.speaker)
    }

    @Test fun explicitNamesMayFollowAdverbsOrObjectPronouns() {
        for ((remark, speaker) in listOf(
            "сказал наконец Сальватор" to "Сальватор",
            "ответил спокойно Цзи Цзы" to "Цзи Цзы",
            "спросил его Зурита" to "Зурита",
        )) {
            val text = "— Ты придёшь? — $remark."
            assertEquals(speaker, SpeedDialogueAnalyzer.at(text, 2)?.speaker)
            assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf(remark)))
        }
        val text = "— Хорошо, — сказал он Павлу."
        assertNull(SpeedDialogueAnalyzer.at(text, 2)?.speaker)
    }
    @Test fun expressiveRemarksAndTitlesStillIdentifySpeaker() {
        for ((remark, speaker) in listOf(
            "просиял Слизнорт" to "Слизнорт",
            "рявкнула профессор Макгонагалл" to "Макгонагалл",
            "проворчал доктор Иванов" to "Иванов",
        )) {
            val text = "— Конечно! — $remark. — Всё готово."
            assertEquals(speaker, SpeedDialogueAnalyzer.at(text, 2)?.speaker)
            assertEquals(speaker, SpeedDialogueAnalyzer.at(text, text.indexOf("Всё готово"))?.speaker)
            assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf(remark)))
        }
    }
}
