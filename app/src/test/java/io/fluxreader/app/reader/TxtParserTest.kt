package io.fluxreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TxtParserTest {
    @Test
    fun splitsParagraphsWithoutBuildingComplexDocument() {
        val file = File.createTempFile("flux-test-", ".txt")
        try {
            file.writeText("Первый абзац.\n\nВторой абзац.")
            val outline = TxtParser.outline(file)
            assertEquals(1, outline.chapters.size)
            val chapter = TxtParser.loadChapter(file)
            assertTrue(chapter.plainText().contains("Первый абзац"))
            assertTrue(chapter.plainText().contains("Второй абзац"))
        } finally {
            file.delete()
        }
    }
}
