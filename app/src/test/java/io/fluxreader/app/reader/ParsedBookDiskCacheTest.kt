package io.fluxreader.app.reader

import io.fluxreader.app.data.BookOutline
import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ChapterRef
import io.fluxreader.app.data.ReaderBlock
import io.fluxreader.app.data.ReaderLink
import io.fluxreader.app.data.TextRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class ParsedBookDiskCacheTest {
    @Test
    fun roundTripsOutlineAndRichChapter() {
        val root = createTempDirectory("flux-parsed-cache-").toFile()
        try {
            val cache = ParsedBookDiskCache(root, maxBytes = 32L * 1024L * 1024L)
            val source = File(root, "book.epub").apply { writeText("source") }
            val token = cache.sourceToken(source, "abc123")
            val ref = ChapterRef(3, "Глава 4", "OPS/chapter4.xhtml", listOf("chapter-4"))
            val outline = BookOutline("Книга", "Автор", listOf(ref))
            val chapter = ChapterContent(
                index = 3,
                title = "Глава 4",
                blocks = listOf(
                    ReaderBlock.Text(
                        text = "Текст со ссылкой",
                        role = TextRole.PARAGRAPH,
                        anchors = listOf("p1"),
                        links = listOf(ReaderLink(9, 16, "OPS/chapter5.xhtml#next", false)),
                        listDepth = 2,
                        listMarker = "◦",
                    ),
                    ReaderBlock.Image("OPS/img/pic.jpg", "Иллюстрация", listOf("pic")),
                ),
            )

            cache.writeOutline("book-id", token, outline)
            cache.writeChapter("book-id", token, ref, chapter)

            assertEquals(outline, cache.readOutline("book-id", token))
            assertEquals(chapter, cache.readChapter("book-id", token, ref))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun changedSourceTokenInvalidatesOldEntry() {
        val root = createTempDirectory("flux-parsed-cache-").toFile()
        try {
            val cache = ParsedBookDiskCache(root, maxBytes = 32L * 1024L * 1024L)
            val ref = ChapterRef(0, "One", "one.xhtml")
            cache.writeOutline("book-id", "token-v1", BookOutline("One", "A", listOf(ref)))
            assertNull(cache.readOutline("book-id", "token-v2"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun diskBudgetPrunesOldBooksButKeepsActiveBook() {
        val root = createTempDirectory("flux-parsed-cache-").toFile()
        try {
            val cache = ParsedBookDiskCache(root, maxBytes = 2048L)
            val largeText = "x".repeat(1800)
            fun write(bookId: String, age: Long) {
                val ref = ChapterRef(0, "One", "one.xhtml")
                cache.writeOutline(bookId, "token", BookOutline(bookId, "A", listOf(ref)))
                cache.writeChapter(bookId, "token", ref, ChapterContent(0, "One", listOf(ReaderBlock.Text(largeText))))
                root.listFiles().orEmpty().filter { it.isDirectory }.forEach { it.setLastModified(age) }
            }
            write("old", 1L)
            write("active", 2L)
            cache.prune(excludeBookId = "active")
            assertTrue(cache.readOutline("active", "token") != null)
        } finally {
            root.deleteRecursively()
        }
    }
}
