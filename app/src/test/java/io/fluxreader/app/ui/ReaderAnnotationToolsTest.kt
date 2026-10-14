package io.fluxreader.app.ui

import io.fluxreader.app.data.ReaderAnnotation
import io.fluxreader.app.data.ReaderAnnotationKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderAnnotationToolsTest {
    private fun item(id: String, book: String, chapter: Int, excerpt: String, note: String = "", updated: Long = 1L) = ReaderAnnotation(
        id = id, bookId = book, chapterIndex = chapter,
        startBlockIndex = 0, startCharOffset = 0, endBlockIndex = 0, endCharOffsetExclusive = 4,
        excerpt = excerpt, kind = ReaderAnnotationKind.HIGHLIGHT, note = note, updatedAt = updated,
    )

    @Test fun searchScopeAndBookSortWorkTogether() {
        val items = listOf(item("1", "b", 2, "gamma"), item("2", "a", 1, "alpha", "needle"))
        val result = ReaderAnnotationTools.filterAndSort(
            items, "a", ReaderAnnotationScope.ALL_BOOKS, null, "needle", ReaderAnnotationSortMode.BOOK,
        ) { if (it == "a") "Alpha Book" else "Beta Book" }
        assertEquals(listOf("2"), result.map { it.id })
    }

    @Test fun markdownExportIncludesAttachedNote() {
        val text = ReaderAnnotationTools.export(
            listOf(item("1", "a", 0, "Selected text", "My note")), ReaderAnnotationExportFormat.MARKDOWN,
            bookTitle = { "Book" }, chapterTitle = { "Chapter" }, dateLabel = { "date" },
        )
        assertTrue(text.contains("> Selected text"))
        assertTrue(text.contains("Заметка: My note"))
    }
}
