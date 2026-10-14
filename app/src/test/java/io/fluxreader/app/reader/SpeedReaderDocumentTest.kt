package io.fluxreader.app.reader

import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ReaderBlock
import io.fluxreader.app.data.TextRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeedReaderDocumentTest {
    @Test
    fun preservesPlainTextAxisAndCues() {
        val chapter = ChapterContent(
            index = 0,
            title = "Глава 1",
            blocks = listOf(
                ReaderBlock.Text("Начало текста", TextRole.PARAGRAPH),
                ReaderBlock.Image("img/1.jpg", "Схема"),
                ReaderBlock.Text("Подзаголовок", TextRole.HEADING),
                ReaderBlock.Text("Продолжение", TextRole.PARAGRAPH),
            ),
        )
        val doc = SpeedReaderDocumentBuilder.fromChapter(chapter)
        assertEquals(chapter.plainText(), doc.text)
        assertEquals(1, doc.mediaCues.size)
        assertTrue(doc.mediaCues.first().offset > doc.text.indexOf("Начало текста"))
        assertTrue(doc.headingCues.any { it.text == "Глава 1" && it.startOffset == 0 })
        assertTrue(doc.headingCues.any { it.text == "Подзаголовок" })
        assertEquals(doc.text.indexOf("Начало текста"), SpeedReaderCursor.wordStartOffset(doc.text, doc.readableOffset(0)))
    }

    @Test
    fun readableOffsetSkipsConsecutiveChapterHeadingsButNotBody() {
        val chapter = ChapterContent(
            index = 15,
            title = "Глава 16",
            blocks = listOf(
                ReaderBlock.Text("Глава 16", TextRole.HEADING),
                ReaderBlock.Text("ОЧЕНЬ ХОЛОДНОЕ РОЖДЕСТВО", TextRole.TITLE),
                ReaderBlock.Text("Так, значит, Снегг предлагал ему помощь?", TextRole.PARAGRAPH),
            ),
        )
        val doc = SpeedReaderDocumentBuilder.fromChapter(chapter)
        val expected = doc.text.indexOf("Так, значит")
        assertEquals(expected, SpeedReaderCursor.wordStartOffset(doc.text, doc.readableOffset(0)))
        assertEquals(expected, SpeedReaderCursor.wordStartOffset(doc.text, doc.readableOffset(doc.text.indexOf("ХОЛОДНОЕ"))))
    }
}
