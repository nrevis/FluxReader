package io.fluxreader.app.reader

import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DialogueCorrectionStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private val text = "Нина посмотрела в окно.\n\n— Привет, — сказала она. — Я вернусь.\n\n— Пора, — ответил Марк."
    @Test fun replyIncludesContinuationsButNotNextSpeaker() {
        val reply = DialogueReplyLocator.at(text, text.indexOf("Привет"))!!
        assertEquals(reply, DialogueReplyLocator.at(text, text.indexOf("Я вернусь")))
        assertNotEquals(reply, DialogueReplyLocator.at(text, text.indexOf("Пора")))
        assertNull(DialogueReplyLocator.at(text, text.indexOf("сказала")))
    }
    @Test fun correctionsSurviveNewStoreAndRespectBookChapterAndExactText() {
        val reply = DialogueReplyLocator.at(text, text.indexOf("Привет"))!!
        DialogueCorrectionStore(folder.root).update("one", 2, text, reply, "Нина", false)
        val store = DialogueCorrectionStore(folder.root)
        assertEquals("Нина", store.read("one", 2, text).single().speaker)
        assertTrue(store.read("two", 2, text).isEmpty())
        assertTrue(store.read("one", 3, text).isEmpty())
        assertTrue(store.read("one", 2, " " + text).isEmpty())
    }
    @Test fun manualNameWinsOverReanalysisAndResetRestoresAutomaticResult() {
        val reply = DialogueReplyLocator.at(text, text.indexOf("Пора"))!!
        val store = DialogueCorrectionStore(folder.root)
        var edits = store.update("one", 0, text, reply, "Нина", false)
        val auto = BookDialogueAnalyzer.local(text)
        assertEquals("Нина", DialogueReplyLocator.resolve(text, text.indexOf("Пора"), auto, edits)?.speaker)
        edits = store.update("one", 0, text, reply, null, true)
        assertEquals("Марк", DialogueReplyLocator.resolve(text, text.indexOf("Пора"), auto, edits)?.speaker)
    }
    @Test fun explicitlyUnknownIsDifferentFromAutomatic() {
        val store = DialogueCorrectionStore(folder.root)
        val reply = DialogueReplyLocator.at(text, text.indexOf("Пора"))!!
        val edits = store.update("one", 0, text, reply, null, false)
        assertEquals(1, edits.size)
        assertNull(DialogueReplyLocator.resolve(text, text.indexOf("Пора"), BookDialogueAnalyzer.local(text), edits)?.speaker)
        assertEquals(edits, DialogueCorrectionStore(folder.root).read("one", 0, text))
    }
    @Test fun aCorrectionCannotTurnAuthorNarrationIntoDirectSpeech() {
        val reply = DialogueReplyLocator.at(text, text.indexOf("Привет"))!!
        val edits = listOf(DialogueCorrection(reply, "Нина"))
        assertNull(DialogueReplyLocator.resolve(text, text.indexOf("сказала"), null, edits))
        assertEquals("Нина", DialogueReplyLocator.resolve(text, text.indexOf("Я вернусь"), null, edits)?.speaker)
    }
    @Test fun emptyAndControlCharacterNamesAreRejectedWithoutErasingPreviousEdit() {
        val store = DialogueCorrectionStore(folder.root)
        val reply = DialogueReplyLocator.at(text, text.indexOf("Пора"))!!
        store.update("one", 0, text, reply, "Марк", false)
        for (name in listOf(" ", "Нина\nМарк", "x".repeat(81))) {
            try { store.update("one", 0, text, reply, name, false); fail("Invalid name accepted") }
            catch (_: IllegalArgumentException) { }
        }
        assertEquals("Марк", store.read("one", 0, text).single().speaker)
        assertFalse(folder.root.listFiles()!!.any { it.extension == "tmp" })
    }
}
