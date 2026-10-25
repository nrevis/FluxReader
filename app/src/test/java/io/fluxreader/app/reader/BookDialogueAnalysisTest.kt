package io.fluxreader.app.reader

import org.junit.Assert.*
import org.junit.Test

class BookDialogueAnalysisTest {
    private fun enriched(text: String, names: List<String>) = BookDialogueAnalyzer.enrich(
        text, BookDialogueAnalyzer.local(text), DialogueCharacterRoster(names),
    )

    private fun learnedRoster(vararg chapters: String): DialogueCharacterRoster {
        val builder = DialogueCharacterRoster.Builder()
        chapters.forEach { builder.observe(it, BookDialogueAnalyzer.local(it)) }
        return builder.build()
    }

    @Test fun pronounUsesImmediatelyPrecedingNarrationAndLearnedGender() {
        val roster = learnedRoster("— Да, — сказала Гермиона.", "— Хорошо, — сказал Гарри.")
        val text = "Гермиона, однако, придерживалась менее радужных взглядов.\n\n" +
            "— Если даже Дамблдор не сумел, — негромко сказала она, стоя с Гарри во дворе. — Крестражи…\n\n— Не слышала?"
        val index = BookDialogueAnalyzer.enrich(text, BookDialogueAnalyzer.local(text), roster)
        assertEquals("Гермиона", index.at(text.indexOf("Если"))?.speaker)
        assertEquals("Гермиона", index.at(text.indexOf("Крестражи"))?.speaker)
        assertEquals(DialogueEvidence.PRONOUN_ANTECEDENT, index.entries.first().evidence)
        assertNull(index.at(text.indexOf("сказала")))
        assertNull(index.at(text.indexOf("Не слышала"))?.speaker)
        assertTrue(index.matches(text))
    }

    @Test fun pronounAbstainsOnAmbiguityWrongGenderSceneBreakAndQuotedNames() {
        val roster = learnedRoster("— Да, — сказала Анна.", "— Да, — сказала Мария.", "— Да, — сказал Борис.")
        for (prefix in listOf("Анна и Мария остались во дворе.", "Борис остался во дворе.",
            "Анна заговорила:\n\n***", "— Анна осталась во дворе.", "Анна читала «Борис».", "Она посмотрела в окно.", "В комнате ждали Анна.")) {
            val text = "$prefix\n\n— Довольно! — сказала она. — Идём."
            assertTrue(prefix, BookDialogueAnalyzer.enrich(text, BookDialogueAnalyzer.local(text), roster).entries.takeLast(2).all { it.span.speaker == null })
        }
    }

    @Test fun unknownOrConflictingGenderDoesNotGuessFromNameEnding() {
        val text = "Саша остановилась.\n\n— Привет! — сказала она."
        for (roster in listOf(DialogueCharacterRoster(listOf("Саша")),
            learnedRoster("— Да, — сказал Саша.", "— Нет, — сказала Саша."))) {
            assertNull(BookDialogueAnalyzer.enrich(text, BookDialogueAnalyzer.local(text), roster).entries.single().span.speaker)
        }
        assertEquals("Саша", BookDialogueAnalyzer.enrich(text, BookDialogueAnalyzer.local(text),
            learnedRoster("— Да, — сказала Саша.")).entries.single().span.speaker)
    }

    @Test fun masculineAndInvertedPronounsKeepOriginalOffsetsWithUnicodeSpaces() {
        val roster = learnedRoster("— Да, — ответил Борис.")
        val text = "Борис посмотрел в окно.\n\n— Идём! — он тихо ответил. — Уже пора.".replace(' ', '\u202f')
        val index = BookDialogueAnalyzer.enrich(text, BookDialogueAnalyzer.local(text), roster)
        assertTrue(index.entries.all { it.span.speaker == "Борис" })
        assertEquals(BookDialogueAnalyzer.local(text).entries.map { it.span.start to it.span.endExclusive },
            index.entries.map { it.span.start to it.span.endExclusive })
    }

    @Test fun quotedPronounsDoNotAssignNamesButBoundedSceneNarrativeMayRetainReferent() {
        val roster = learnedRoster("— Да, — сказала Анна.")
        val quoted = "Анна остановилась.\n\n— Она сказала, что всё хорошо."
        assertTrue(BookDialogueAnalyzer.enrich(quoted, BookDialogueAnalyzer.local(quoted), roster).entries.all { it.span.speaker == null })
        val narrative = "Анна остановилась.\n\nЗа окном светало.\n\n— Да, — сказала она."
        assertEquals("Анна", BookDialogueAnalyzer.enrich(narrative, BookDialogueAnalyzer.local(narrative), roster).entries.single().span.speaker)
    }

    @Test fun genderEvidenceIsPartOfRosterCacheIdentity() {
        assertNotEquals(learnedRoster("— Да, — сказала Саша.").fingerprint,
            learnedRoster("— Да, — сказал Саша.").fingerprint)
    }

    @Test fun callForSilenceChangesSpeakerAfterPreviousConversation() {
        val text = "— Что случилось? — спросил Рон.\n\nПрежде чем Гарри успел ответить, в класс вошёл Слизнорт и призвал всех к тишине.\n\n— Садитесь, прошу вас!"
        val index = enriched(text, listOf("Рон", "Гарри", "Слизнорт"))
        assertEquals("Рон", index.at(text.indexOf("Что"))?.speaker)
        assertEquals("Слизнорт", index.at(text.indexOf("Садитесь"))?.speaker)
        assertEquals(DialogueEvidence.SCENE_INTRODUCTION, index.entries.last().evidence)
        assertNull(index.at(text.indexOf("вошёл")))
    }

    @Test fun nameLearnedElsewhereIntroducesSpeechInCurrentChapter() {
        val otherChapter = BookDialogueAnalyzer.local("— Всё верно, — сказала Гермиона.")
        val roster = DialogueCharacterRoster(otherChapter.entries.mapNotNull { it.span.speaker })
        val text = "Гермиона со страшной скоростью затараторила:\n\n— Третий закон гласит…"
        val local = BookDialogueAnalyzer.local(text)
        assertNull(local.entries.single().span.speaker)
        assertEquals("Гермиона", BookDialogueAnalyzer.enrich(text, local, roster).at(text.indexOf("Третий"))?.speaker)
    }

    @Test fun unknownNamesAreNotInventedFromCapitalizedWords() {
        val text = "В комнату вошла Анна и призвала всех к тишине.\n\n— Добрый день!"
        assertNull(enriched(text, listOf("Борис")).entries.single().span.speaker)
    }

    @Test fun nonBreakingSpacesFromEpubDoNotHideSpeakerIntroduction() {
        val text = "В класс вошёл Слизнорт и призвал всех к тишине.\n\n— Садитесь!".replace(' ', '\u00a0')
        assertEquals("Слизнорт", enriched(text, listOf("Слизнорт")).entries.single().span.speaker)
        val hermione = "Гермиона со страшной скоростью затараторила:\n\n— Закон гласит…".replace(' ', '\u202f')
        assertEquals("Гермиона", enriched(hermione, listOf("Гермиона")).entries.single().span.speaker)
    }

    @Test fun adjacentIndependentCharactersCannotBecomeABookWideAlias() {
        val text = "— Верно, — сказал Слизнорт.\n\n— Хорошо, — ответил Гарри."
        val index = enriched(text, listOf("Слизнорт", "Гарри", "Слизнорт Гарри", "Гарри Рона"))
        assertEquals(listOf("Слизнорт", "Гарри"), index.entries.map { it.span.speaker })
    }

    @Test fun narrationAfterPreviousReplyCanIntroduceAnotherSpeaker() {
        val text = "— Что случилось? — шепотом спросил Рон, но, прежде чем Гарри успел ответить, в класс вошел Слизнорт и призвал всех к тишине.\n\n— Садитесь!"
        assertEquals("Слизнорт", enriched(text, listOf("Рон", "Гарри", "Слизнорт")).entries.last().span.speaker)
        val quoted = "— Слизнорт вошёл в класс и призвал всех к тишине.\n\n— Садитесь!"
        assertNull(enriched(quoted, listOf("Слизнорт")).entries.last().span.speaker)
    }

    @Test fun lastMentionCanBeAnObjectRatherThanTheSpeaker() {
        for (intro in listOf("Юноша поверил словам Зурита и воскликнул:", "Селдон повернулся к Дорс и спросил:")) {
            val text = "$intro\n\n— Что делать дальше?"
            assertNull(enriched(text, listOf("Зурита", "Дорс", "Селдон")).entries.last().span.speaker)
        }
    }

    @Test fun explicitAttributionOverridesPriorIntroduction() {
        val text = "Анна заговорила:\n\n— Довольно! — сказал Борис. — Идём."
        val index = enriched(text, listOf("Анна", "Борис"))
        assertTrue(index.entries.all { it.span.speaker == "Борис" })
        assertTrue(index.entries.all { it.evidence == DialogueEvidence.EXPLICIT })
        assertNull(index.at(text.indexOf("сказал")))
    }

    @Test fun unresolvedRemarkIsNotOverriddenByAnotherCharactersIntroduction() {
        val text = "Анна заговорила:\n\n— Довольно! — сказал он. — Пойдём."
        assertTrue(enriched(text, listOf("Анна")).entries.all { it.span.speaker == null })
    }

    @Test fun noAlternationOrNameCarryAcrossUnrelatedNarrativeAndSceneBreaks() {
        for (text in listOf(
            "— Да, — сказал Борис.\n\n— Нет!",
            "Анна заговорила:\n\n***\n\n— Добрый день!",
            "Анна посмотрела на Бориса.\n\n— Добрый день!",
            "Анна сказала, что пора идти:\n\n— Добрый день!",
            "— Анна заговорила:\n\n— Добрый день!",
        )) assertNull(text, enriched(text, listOf("Анна", "Борис")).entries.last().span.speaker)
    }

    @Test fun addresseeAndIndirectSpeechDoNotBecomeSpeaker() {
        val text = "Борис попросил Анну передать, что придёт позже.\n\n— Анна, садитесь!"
        assertNull(enriched(text, listOf("Анна", "Борис")).entries.single().span.speaker)
    }

    @Test fun sharedNamesAreNotSilentlyReplacedWithAFullName() {
        val text = "Анна затараторила:\n\n— Добрый вечер!"
        val index = enriched(text, listOf("Анна", "Анна Соколова", "Анна Иванова"))
        assertEquals("Анна", index.entries.single().span.speaker)
        // A locally explicit short name is preserved, never silently expanded from a roster.
        assertEquals("Анна", enriched(text, listOf("Анна Соколова")).entries.single().span.speaker)
    }

    @Test fun fullExplicitNamesRemainAtomicWhenTheirPartsAreAlsoKnown() {
        for (name in listOf("Педро Зурита", "Хари Селдон", "Первый Оратор")) {
            val text = "— Хорошо, — сказал $name. — Продолжим."
            val names = listOf(name) + name.split(' ')
            val index = enriched(text, names)
            assertTrue(name, index.entries.all { it.span.speaker == name })
        }
        val text = "— Хорошо, — сказала Анна\u202fСоколова."
        assertEquals("Анна Соколова", enriched(text, listOf("Анна", "Соколова", "Анна Соколова")).entries.single().span.speaker)
    }

    @Test fun sceneNarrativeScanIsCancellableBeforeNextReply() {
        val text = "Анна остановилась.\n\n" + "За окном светало.\n\n".repeat(100) + "— Да, — сказала она."
        var checks = 0
        try {
            BookDialogueAnalyzer.enrich(text, BookDialogueAnalyzer.local(text), learnedRoster("— Да, — сказала Анна.")) {
                if (++checks == 10) throw InterruptedException("cancel")
            }
            fail("Expected cancellation during narrative scan")
        } catch (_: InterruptedException) { assertEquals(10, checks) }
    }

    @Test fun laterNarrativeNamesDoNotMakeAnExplicitSubjectAmbiguous() {
        val roster = learnedRoster("— Да, — сказал Игорь.", "— Да, — сказал Алексей.", "— Да, — сказал Борис.")
        for (remark in listOf("сказал Игорь в ответ на взгляд Бориса", "спросил Игорь (Алексей рассердился)",
            "сообщил Игорь Алексею и Борису", "прошипел Игорь и побежал догонять Алексея")) {
            val text = "— Да, — $remark. — Продолжим."
            assertTrue(remark, BookDialogueAnalyzer.enrich(text, BookDialogueAnalyzer.local(text), roster).entries.all { it.span.speaker == "Игорь" })
        }
        val chorus = "— Да, — хором ответили Игорь и Алексей."
        assertNull(BookDialogueAnalyzer.enrich(chorus, BookDialogueAnalyzer.local(chorus), roster).entries.single().span.speaker)
    }

    @Test fun longParagraphsUnicodeAndOffsetsRemainStable() {
        val text = "Заголовок 😀\n\n" + "текст ".repeat(900) + "\n\nАнна затараторила:\n\n— Привет, мир!"
        val index = enriched(text, listOf("Анна"))
        val span = index.at(text.indexOf("Привет"))!!
        assertEquals("— Привет, мир!", text.substring(span.start, span.endExclusive))
        assertTrue(index.matches(text))
        assertNull(index.at(-1))
        assertNull(index.at(text.length))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOffsetsFromAnotherText() {
        BookDialogueAnalyzer.enrich("Другой текст", BookDialogueAnalyzer.local("— Привет!"), DialogueCharacterRoster(emptyList()))
    }

    @Test fun chapterPassIsCancellableBetweenParagraphs() {
        var checkpoints = 0
        try {
            BookDialogueAnalyzer.local("— Привет!\n\n— Ещё раз!") {
                if (++checkpoints == 2) throw InterruptedException("cancel")
            }
            fail("Expected cancellation")
        } catch (_: InterruptedException) {
            assertEquals(2, checkpoints)
        }
    }

    @Test fun chapterPassMatchesPlaybackBoundaries() {
        val text = "Анна сказала:\n\n— Добрый вечер!\n\n— Уже поздно, — ответил Борис. — Пойдём домой.\n\nОна закрыла дверь."
        val index = BookDialogueAnalyzer.local(text)
        for (offset in text.indices) assertEquals("offset=$offset", SpeedDialogueAnalyzer.at(text, offset), index.at(offset))
    }
}
