package io.fluxreader.app.reader

import org.junit.Assert.*
import org.junit.Test

class DialogueRulesRegressionTest {
    private val roster = DialogueCharacterRoster.Builder().apply {
        observe("— Да, — сказала Нина.\n— Нет, — сказал Игорь.\n— Верно, — сказал Марк.",
            BookDialogueAnalyzer.local("— Да, — сказала Нина.\n— Нет, — сказал Игорь.\n— Верно, — сказал Марк."))
    }.build()
    private fun index(text: String) = BookDialogueAnalyzer.enrich(text, BookDialogueAnalyzer.local(text), roster)

    @Test fun chapterAnalysisLearnsLaterAttributionsWithoutScanningOtherChapters() {
        val text = "Нина остановилась у двери.\n\n— Пора, — сказала она.\n\n— Я готова, — сказала Нина."
        assertNull(BookDialogueAnalyzer.local(text).at(text.indexOf("Пора"))?.speaker)
        val index = BookDialogueAnalyzer.chapter(text)
        assertEquals("Нина", index.at(text.indexOf("Пора"))?.speaker)
        assertTrue(index.matches(text))
        assertNull(index.at(text.indexOf("сказала она")))
    }

    @Test fun explicitSpeakerChangeInOneParagraphStartsANewReply() {
        val text = "— Да, — сказала Нина. — Нет, — ответил Игорь. — Я останусь здесь."
        val first = SpeedDialogueAnalyzer.at(text, text.indexOf("Да"))!!
        val second = SpeedDialogueAnalyzer.at(text, text.indexOf("Нет"))!!
        val third = SpeedDialogueAnalyzer.at(text, text.indexOf("Я останусь"))!!
        assertEquals("Нина", first.speaker)
        assertEquals("Игорь", second.speaker)
        assertFalse(second.continuation)
        assertEquals("Игорь", third.speaker)
        assertTrue(third.continuation)
        assertNotEquals(DialogueReplyLocator.at(text, first.start), DialogueReplyLocator.at(text, second.start))
        assertEquals(DialogueReplyLocator.at(text, second.start), DialogueReplyLocator.at(text, third.start))
        assertNull(SpeedDialogueAnalyzer.at(text, text.indexOf("ответил")))
    }

    @Test fun aDifferentSubjectPronounCannotInheritThePreviousNamedSpeaker() {
        val text = "Игорь стоял у двери.\n\n— Да, — сказала Нина. — Нет, — ответил он."
        val reply = index(text).at(text.indexOf("Нет"))!!
        assertEquals("Игорь", reply.speaker)
        assertFalse(reply.continuation)
    }

    @Test fun aSecondUnknownSpeakerDoesNotBorrowTheFirstName() {
        val text = "— Да, — сказала Нина. — Нет, — ответил он."
        assertEquals("Нина", index(text).at(text.indexOf("Да"))?.speaker)
        assertNull(index(text).at(text.indexOf("Нет"))?.speaker)
    }

    @Test fun openingClosingAndCallingRemarksIdentifyTheirOwnSubject() {
        for ((remark, speaker) in listOf("начал Игорь" to "Игорь", "закончил Игорь" to "Игорь",
            "Игорь закончил" to "Игорь", "начала Нина" to "Нина", "закончил профессор Игорь" to "Игорь",
            "вдруг окликнул его Марк" to "Марк", "восклицал Игорь" to "Игорь")) {
            val text = "— Постой! — $remark."
            assertEquals(remark, speaker, SpeedDialogueAnalyzer.at(text, 2)?.speaker)
            assertNull(remark, SpeedDialogueAnalyzer.at(text, text.indexOf(remark)))
        }
    }

    @Test fun completionOfAnObjectDoesNotBecomeAnAttribution() {
        for (remark in listOf("Игорь начал собирать вещи", "Игорь закончил работу", "Нина начала ужин",
            "начал готовить Игорь", "закончил письмо Марк")) {
            val text = "— Ты готов? — $remark."
            assertNull(remark, SpeedDialogueAnalyzer.at(text, 2)?.speaker)
        }
        for (prefix in listOf("Игорь закончил работу:", "Нина начала ужин:")) {
            val text = "$prefix\n\n— Хорошо!"
            assertNull(prefix, index(text).at(text.indexOf("Хорошо"))?.speaker)
        }
    }

    @Test fun closingPronounUsesLearnedGenderButDoesNotInventAName() {
        val text = "Нина остановилась.\n\n— До завтра! — закончила она."
        assertEquals("Нина", index(text).at(text.indexOf("До завтра"))?.speaker)
        assertNull(index(text).at(text.indexOf("закончила")))
    }

    @Test fun ellipsisResumesSpeechOnlyBetweenMatchingExplicitAnchorsAndNarrativeReference() {
        val text = "— Если всё верно… — сказал Игорь. — Продолжим рассуждение…\n\n" +
            "Марк задумался над смыслом дальнейших слов Игоря.\n\n" +
            "— …то ответ становится очевидным.\n\n" +
            "Марк открыл блокнот.\n\n— Итак, — закончил Игорь, — проверим ответ."
        val result = index(text)
        assertEquals("Игорь", result.at(text.indexOf("…то ответ"))?.speaker)
        assertEquals(DialogueEvidence.NARRATIVE_CONTINUATION,
            result.entries.first { it.span.contains(text.indexOf("…то ответ")) }.evidence)
        assertNull(result.at(text.indexOf("Марк задумался")))
        assertTrue(result.matches(text))
    }

    @Test fun ellipsisAloneOrConflictingNarrativeNeverTransfersTheLastSpeaker() {
        val variants = listOf(
            "Марк задумался над смыслом дальнейших слов Нины.",
            "Марк посмотрел на часы.",
            "Марк вспомнил: «Слова Игоря были важны».",
            "На следующий день Марк вспомнил дальнейшие слова Игоря.",
            "***\n\nМарк задумался над дальнейшими словами Игоря.")
        for (narration in variants) {
            val text = "— Продолжим рассуждение… — сказал Игорь. — Если это верно…\n\n" +
                "$narration\n\n— …то всё получится.\n\n— Итак, — закончил Игорь."
            assertNull(narration, index(text).at(text.indexOf("…то всё"))?.speaker)
        }
        val changed = "— Если это верно… — сказал Игорь.\n\nМарк не понял дальнейших слов Игоря.\n\n" +
            "— …то всё получится.\n\n— Итак, — закончил Марк."
        assertNull(index(changed).at(changed.indexOf("…то всё"))?.speaker)
    }

    @Test fun aDepartedImmediateAntecedentDoesNotSpeakAgainByPronoun() {
        val text = "Нина ушла.\n\n— Пора, — сказала она."
        assertNull(index(text).at(text.indexOf("Пора"))?.speaker)
    }

    @Test fun nonBreakingSpacesPreserveOffsetsWithNewAttributions() {
        val text = "— Итак, — закончил Игорь. — Всё готово.".replace(' ', '\u202f')
        val result = index(text)
        assertEquals("Игорь", result.at(text.indexOf("Итак"))?.speaker)
        assertEquals("Игорь", result.at(text.indexOf("Всё"))?.speaker)
        for (entry in result.entries) assertEquals(entry.span, SpeedDialogueAnalyzer.at(text, entry.span.start))
    }
}
