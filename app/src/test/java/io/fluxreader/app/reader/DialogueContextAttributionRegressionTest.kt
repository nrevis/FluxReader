package io.fluxreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** Original synthetic controls frozen separately from the implementation. No book extracts. */
class DialogueContextAttributionRegressionTest {
    private val roster = DialogueCharacterRoster.Builder().apply {
        val seed = "— Да, — сказала Нина.\n\n— Да, — сказала Ольга.\n\n" +
            "— Да, — сказал Игорь.\n\n— Да, — сказал Марк."
        observe(seed, BookDialogueAnalyzer.local(seed))
    }.build()

    private fun analyze(text: String) = BookDialogueAnalyzer.enrich(text, BookDialogueAnalyzer.local(text), roster)

    private fun assertSpeaker(text: String, query: String, expected: String?) {
        val offset = text.indexOf(query)
        check(offset >= 0 && text.indexOf(query, offset + 1) == -1) { "Control query must be unique: $query" }
        val span = analyze(text).at(offset)
        assertNotNull("Speech disappeared: $query", span)
        assertEquals(query, expected, span?.speaker)
    }

    @Test fun affectedNarrativeTopicCanAnchorASubjectPronounWithoutUsingTheMaleAddressee() {
        val text = "Тем временем выяснилось, что архив закрыт. Нину это потрясло, и она убрала блокнот.\n\n" +
            "— Ничего не получилось! — сказала она Игорю. — Придётся вернуться завтра."
        assertSpeaker(text, "Ничего не получилось", "Нина")
        assertSpeaker(text, "Придётся вернуться", "Нина")
        assertNull(analyze(text).at(text.indexOf("сказала")))
    }

    @Test fun aDeclinedFemaleAddresseeDoesNotCompeteWithTheSubjectPronoun() {
        val text = "Нина остановилась рядом с Ольгой.\n\n— Подожди, — сказала она Ольге. — Я скоро вернусь."
        assertSpeaker(text, "Подожди", "Нина")
        assertSpeaker(text, "Я скоро вернусь", "Нина")
    }

    @Test fun affectedFemaleTopicDoesNotResolveAMasculineAuthorPronoun() {
        val text = "Нину это потрясло.\n\n— Я останусь, — сказал он Игорю."
        assertSpeaker(text, "Я останусь", null)
    }

    @Test fun twoAffectedWomenRemainAmbiguous() {
        val text = "Нину новость потрясла, а Ольгу расстроила.\n\n— Неожиданно, — сказала она Игорю."
        assertSpeaker(text, "Неожиданно", null)
    }

    @Test fun conditionalOrQuotedAffectedTopicsDoNotIntroduceAnActualSceneParticipant() {
        for (prefix in listOf(
            "Если бы Нину это потрясло, пришлось бы ждать.",
            "В заметке было написано: «Нину это потрясло».",
            "В заметке было написано: \"Нину это потрясло\".",
        )) {
            val text = "$prefix\n\n— Всё ясно, — сказала она Игорю."
            assertSpeaker(text, "Всё ясно", null)
        }
    }

    @Test fun anAffectedTopicCannotCrossAnExplicitSceneBreak() {
        val text = "Нину это потрясло.\n\n***\n\n— Всё ясно, — сказала она Игорю."
        assertSpeaker(text, "Всё ясно", null)
    }

    @Test fun aLaterNamedWomanDoesNotBackfillAnEarlierDifferentTurnInTheSameParagraph() {
        val text = "— Дождись меня, — сказала она Игорю. — Хорошо, — ответила Нина."
        assertSpeaker(text, "Дождись меня", null)
        assertSpeaker(text, "Хорошо", "Нина")
    }

    @Test fun selfIntroductionNamesTheCurrentGenericRoleAndItsOwnContinuation() {
        val text = "— Здравствуйте, — сказал дежурный смотритель. — Меня зовут Кирилл Ветров. Я покажу дорогу."
        assertSpeaker(text, "Здравствуйте", "Кирилл Ветров")
        assertSpeaker(text, "Меня зовут", "Кирилл Ветров")
        assertSpeaker(text, "Я покажу дорогу", "Кирилл Ветров")
        assertNull(analyze(text).at(text.indexOf("сказал")))
    }

    @Test fun selfIntroductionKeepsNonBreakingSpacesAndOriginalOffsets() {
        val text = "— Добрый день, — сказал смотритель. — Меня зовут Кирилл Ветров.".replace(' ', '\u00a0')
        assertSpeaker(text, "Добрый\u00a0день", "Кирилл Ветров")
        val index = analyze(text)
        val start = text.indexOf("Меня")
        assertEquals(BookDialogueAnalyzer.local(text).at(start)?.start, index.at(start)?.start)
        assertEquals(BookDialogueAnalyzer.local(text).at(start)?.endExclusive, index.at(start)?.endExclusive)
    }

    @Test fun aSelfIntroductionDoesNotLeakIntoTheNextExplicitSpeakerWithinTheSameParagraph() {
        val text = "— Меня зовут Кирилл Ветров, — сказал посетитель. — Очень приятно, — ответила Нина."
        assertSpeaker(text, "Меня зовут", "Кирилл Ветров")
        assertSpeaker(text, "Очень приятно", "Нина")
    }

    @Test fun aSelfIntroductionDoesNotNameAnEarlierUnknownReplyInAnotherParagraph() {
        val text = "— Здравствуйте, — сказал дежурный.\n\n" +
            "— Меня зовут Кирилл Ветров, — сообщил посетитель."
        assertSpeaker(text, "Здравствуйте", null)
        assertSpeaker(text, "Меня зовут", "Кирилл Ветров")
    }

    @Test fun anExplicitAuthorNameWinsOverAConflictingClaimedIdentity() {
        val text = "— Меня зовут Кирилл Ветров, — сказал Игорь."
        assertSpeaker(text, "Меня зовут", "Игорь")
    }

    @Test fun quotedIdentityInsideAReplyDoesNotNameItsReader() {
        for (quote in listOf("«Меня зовут Кирилл Ветров»", "\"Меня зовут Кирилл Ветров\"")) {
            val text = "— В инструкции было: $quote, — сказал посетитель."
            assertSpeaker(text, "В инструкции", null)
        }
    }

    @Test fun negatedThirdPersonOrQuestionedIdentityIsNotAnAffirmativeSelfIntroduction() {
        for (words in listOf(
            "Меня зовут не Кирилл Ветров",
            "Меня не зовут Кирилл Ветров",
            "Не меня зовут Кирилл Ветров",
            "Его зовут Кирилл Ветров",
            "Её зовут Нина",
            "Меня зовут Кирилл Ветров?",
        )) {
            val text = "— $words — сказал посетитель."
            assertSpeaker(text, words, null)
        }
    }

    @Test fun conflictingSelfIntroductionsRemainUnnamed() {
        val text = "— Меня зовут Кирилл Ветров. Меня зовут Денис Орлов, — сказал посетитель."
        assertSpeaker(text, "Кирилл Ветров", null)
        assertSpeaker(text, "Денис Орлов", null)
    }
}
