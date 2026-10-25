package io.fluxreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DialogueSceneExitTest {
    private fun scene(): DialogueScene {
        val seed = "— Да, — сказала Нина.\n\n— Да, — сказала Ольга.\n\n" +
            "— Да, — сказал Марк.\n\n— Да, — сказал Игорь."
        val roster = DialogueCharacterRoster.Builder().apply {
            observe(seed, BookDialogueAnalyzer.local(seed))
        }.build()
        return DialogueScene(roster, DialogueNameForms(roster))
    }

    @Test fun explicitNarrativeDepartureIsRecognizedWithBoundedAdverbs() {
        val scene = scene()
        for (text in listOf("Нина ушла.", "Нина тихо ушла.", "Нина тут же вышла из комнаты.", "Нина удалилась.")) {
            assertEquals(text, setOf("Нина"), scene.departedNames(text))
        }
        assertEquals(setOf("Игорь"), scene.departedNames("Игорь покинул кабинет."))
    }

    @Test fun departureInAuthorCommentAfterSpeechIsRecognized() {
        assertEquals(setOf("Нина"), scene().departedNames("— Я пойду домой, — сказала Нина и ушла."))
    }

    @Test fun aSpokenDepartureStatementDoesNotChangeTheScene() {
        val scene = scene()
        for (text in listOf(
            "— Нина ушла вчера, — сказал Игорь.",
            "— Я ушла бы раньше, — сказала Нина.",
            "Марк прочитал: «Нина ушла».")) {
            assertTrue(text, scene.departedNames(text).isEmpty())
        }
    }

    @Test fun negationAndHypotheticalDepartureAreNotEvents() {
        val scene = scene()
        for (text in listOf(
            "Нина не ушла.", "Нина почти ушла.", "Нина едва не вышла.",
            "Если Нина ушла, всё изменилось.", "Нина ушла бы раньше.",
            "Разве Нина ушла?")) {
            assertTrue(text, scene.departedNames(text).isEmpty())
        }
    }

    @Test fun objectsAndIndirectMentionsDoNotCreateDepartures() {
        val scene = scene()
        for (text in listOf(
            "Письмо Нины ушло по почте.", "Из-за Нины ушёл Марк.",
            "Марк прочитал про Нину: она ушла.", "Нину проводили, и она ушла.")) {
            assertTrue(text, "Нина" !in scene.departedNames(text))
        }
    }

    @Test fun nonSpatialMeaningsDoNotEvictCharacters() {
        val scene = scene()
        for (text in listOf("Нина вышла из себя.", "Нина ушла в раздумья.", "Марк вышел из положения.")) {
            assertTrue(text, scene.departedNames(text).isEmpty())
        }
    }

    @Test fun removingOneParticipantPreservesTheOther() {
        val scene = scene()
        scene.record("Нина", 0)
        scene.record("Ольга", 0)
        val text = "Нина вышла из комнаты."
        scene.advance(text, text.length)
        assertEquals("Ольга", scene.resolve("она", emptySet()))
    }

    @Test fun quotedNameCannotRestoreAnExitedParticipant() {
        val scene = scene()
        scene.record("Нина", 0)
        val text = "Нина ушла.\nМарк прочитал: «Нина ждёт у моста»."
        scene.advance(text, text.length)
        assertEquals(setOf("Нина"), scene.departedNames(text))
        assertNull(scene.resolve("она", emptySet()))
    }

    @Test fun anObjectMentionCannotRestoreAnExitedParticipant() {
        val scene = scene()
        scene.record("Нина", 0)
        val text = "Нина ушла.\nМарк вспомнил Нину."
        scene.advance(text, text.length)
        assertNull(scene.resolve("она", emptySet()))
    }

    @Test fun explicitReturnRestoresTheParticipant() {
        val scene = scene()
        scene.record("Нина", 0)
        val text = "Нина ушла.\nНина вскоре вернулась."
        scene.advance(text, text.length)
        assertTrue(scene.departedNames(text).isEmpty())
        assertEquals("Нина", scene.resolve("она", emptySet()))
    }
}
