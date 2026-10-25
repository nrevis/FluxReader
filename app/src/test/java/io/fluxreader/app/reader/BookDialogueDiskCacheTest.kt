package io.fluxreader.app.reader

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BookDialogueDiskCacheTest {
    @get:Rule val folder = TemporaryFolder()
    private val text = "Анна затараторила:\n\n— Добрый вечер!"

    @Test fun roundTripKeepsOffsetsNamesAndEvidence() {
        val cache = BookDialogueDiskCache(folder.root)
        val index = BookDialogueAnalyzer.enrich(text, BookDialogueAnalyzer.local(text), DialogueCharacterRoster(listOf("Анна")))
        cache.write("book-v1", "chapter-1", "roster-v1", index)
        val read = cache.read("book-v1", "chapter-1", "roster-v1", text)!!
        assertEquals(index.entries, read.entries)
        assertEquals("Анна", read.at(text.indexOf("Добрый"))?.speaker)
        assertNull(cache.read("book-v2", "chapter-1", "roster-v1", text))
        assertNull(cache.read("book-v1", "chapter-2", "roster-v1", text))
        assertNull(cache.read("book-v1", "chapter-1", "roster-v2", text))
    }

    @Test fun changedTextAndDamagedFilesAreCacheMisses() {
        val cache = BookDialogueDiskCache(folder.root)
        cache.write("book", "1", "local", BookDialogueAnalyzer.local(text))
        assertNull(cache.read("book", "1", "local", text + "Изменение"))
        cache.write("book", "1", "local", BookDialogueAnalyzer.local(text))
        folder.root.listFiles()!!.single().writeBytes(byteArrayOf(0, 1, 2))
        assertNull(cache.read("book", "1", "local", text))
    }

    @Test fun cacheHasABoundedDiskBudgetAndNoTemporaryFiles() {
        val cache = BookDialogueDiskCache(folder.root, maxBytes = 600)
        repeat(20) { cache.write("book", "$it", "local", BookDialogueAnalyzer.local(text)) }
        assertTrue(folder.root.listFiles()!!.sumOf { it.length() } <= 600)
        assertTrue(folder.root.listFiles()!!.all { it.extension == "bin" })
    }
}
