package io.fluxreader.app.reader

import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ReaderBlock
import org.junit.Assert.assertEquals
import org.junit.Test

class ChapterSpeedOffsetTest {
    @Test
    fun mapsVisibleBlockCharacterIntoPlainText() {
        val chapter = ChapterContent(
            index = 0,
            title = "Глава",
            blocks = listOf(
                ReaderBlock.Text("  Первый текст  "),
                ReaderBlock.Text("Второй текст"),
            ),
        )
        val plain = chapter.plainText()
        val sourceOffset = chapter.blocks[1].let { it as ReaderBlock.Text }.text.indexOf("текст")
        val mapped = ChapterSpeedOffset.forBlock(chapter, 1, sourceOffset)
        assertEquals(plain.indexOf("текст", plain.indexOf("Второй")), mapped)
    }

    @Test
    fun mapsSpeedOffsetBackToReaderBlockAndCharacter() {
        val chapter = ChapterContent(
            index = 0,
            title = "Глава",
            blocks = listOf(
                ReaderBlock.Text("  Первый текст  "),
                ReaderBlock.Image("cover"),
                ReaderBlock.Text("  Второй текст  "),
            ),
        )
        val target = chapter.plainText().indexOf("текст", chapter.plainText().indexOf("Второй"))
        val anchor = ChapterSpeedOffset.toReaderAnchor(chapter, target)
        val raw = chapter.blocks[anchor.blockIndex] as ReaderBlock.Text
        assertEquals(2, anchor.blockIndex)
        assertEquals(raw.text.indexOf("текст"), anchor.sourceCharOffset)
    }
}
