package io.fluxreader.app.reader

import java.io.DataOutputStream
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

    @Test fun legacyCombinedReplyIsSkippedWithoutLosingOtherEditsOrAssigningBothNewSpeakers() {
        val chapter = "— Вчера, — сказала Ольга.\n\n" +
            "— Да, — сказала Нина. — Нет, — ответил Марк.\n\n" +
            "— Позже, — сказал Игорь."
        val before = DialogueReplyLocator.at(chapter, chapter.indexOf("Вчера"))!!
        val after = DialogueReplyLocator.at(chapter, chapter.indexOf("Позже"))!!
        // Version 1 files from the old parser combined both speech fragments in this paragraph.
        // Build that binary layout directly: update() must not create an obsolete boundary.
        val legacy = DialogueReply(chapter.indexOf("— Да,"), chapter.indexOf("— ответил Марк."))
        val first = DialogueReplyLocator.at(chapter, chapter.indexOf("Да,"))!!
        val second = DialogueReplyLocator.at(chapter, chapter.indexOf("Нет,"))!!
        assertNotEquals(legacy, first)
        assertNotEquals(legacy, second)
        assertNotEquals(first, second)
        val expected = listOf(DialogueCorrection(before, "Наблюдатель"), DialogueCorrection(after, null))
        writeVersionOneFixture(chapter, listOf(expected.first(), DialogueCorrection(legacy, "Нина"), expected.last()))

        val edits = DialogueCorrectionStore(folder.root).read("legacy", 3, chapter)
        assertEquals(expected, edits)
        val automatic = BookDialogueAnalyzer.local(chapter)
        assertEquals("Наблюдатель", DialogueReplyLocator.resolve(chapter, chapter.indexOf("Вчера"), automatic, edits)?.speaker)
        assertEquals("Нина", DialogueReplyLocator.resolve(chapter, chapter.indexOf("Да,"), automatic, edits)?.speaker)
        assertEquals("Марк", DialogueReplyLocator.resolve(chapter, chapter.indexOf("Нет,"), automatic, edits)?.speaker)
        assertNull(DialogueReplyLocator.resolve(chapter, chapter.indexOf("Позже"), automatic, edits)?.speaker)
    }

    @Test fun obsoleteParserBoundaryCannotHideAnOverlappingSerializedEntry() {
        val chapter = "— Да, — сказала Нина. — Нет, — ответил Марк.\n\n— Позже, — сказал Игорь."
        val legacy = DialogueReply(0, chapter.indexOf("— ответил Марк."))
        val nested = DialogueReplyLocator.at(chapter, chapter.indexOf("Нет,"))!!
        val after = DialogueReplyLocator.at(chapter, chapter.indexOf("Позже"))!!
        assertNotEquals(legacy, DialogueReplyLocator.at(chapter, 0))
        writeVersionOneFixture(chapter, listOf(
            DialogueCorrection(legacy, "Нина"), DialogueCorrection(nested, "Марк"), DialogueCorrection(after, "Игорь"),
        ))
        assertTrue(DialogueCorrectionStore(folder.root).read("legacy", 3, chapter).isEmpty())
    }

    @Test fun malformedBinaryRangesStillRejectTheWholeFile() {
        val first = DialogueReplyLocator.at(text, text.indexOf("Привет"))!!
        val second = DialogueReplyLocator.at(text, text.indexOf("Пора"))!!
        for (invalid in listOf(
            DialogueReply(-1, 1),
            DialogueReply(second.start, second.start),
            DialogueReply(second.start + 1, second.start),
            DialogueReply(second.start, text.length + 1),
            DialogueReply(first.start, first.endExclusive),
        )) {
            writeVersionOneFixture(text, listOf(DialogueCorrection(first, "Нина"), DialogueCorrection(invalid, "Марк")))
            assertTrue(invalid.toString(), DialogueCorrectionStore(folder.root).read("legacy", 3, text).isEmpty())
        }
        writeVersionOneFixture(text, listOf(DialogueCorrection(second, "Марк"), DialogueCorrection(first, "Нина")))
        assertTrue(DialogueCorrectionStore(folder.root).read("legacy", 3, text).isEmpty())
    }

    private fun writeVersionOneFixture(chapter: String, entries: List<DialogueCorrection>) {
        val digest = ChapterDialogueIndex.fingerprint(chapter)
        val file = File(folder.root, ChapterDialogueIndex.fingerprint("legacy:3:$digest") + ".bin")
        DataOutputStream(file.outputStream().buffered()).use { output ->
            output.writeInt(1)
            output.writeUTF(digest)
            output.writeInt(entries.size)
            for (entry in entries) {
                output.writeInt(entry.reply.start)
                output.writeInt(entry.reply.endExclusive)
                output.writeBoolean(entry.speaker != null)
                entry.speaker?.let(output::writeUTF)
            }
        }
    }
}
