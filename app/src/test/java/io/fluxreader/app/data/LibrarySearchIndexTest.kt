package io.fluxreader.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LibrarySearchIndexTest {
    private fun book(
        id: String,
        title: String,
        author: String = "",
        series: String = "",
        groups: List<String> = emptyList(),
        folders: List<String> = emptyList(),
    ) = BookRecord(
        id = id,
        title = title,
        author = author,
        filePath = "/tmp/$id.epub",
        originalName = "$id.epub",
        format = BookFormat.EPUB,
        series = series,
        groups = groups,
        folderPaths = folders,
    )

    @Test
    fun prebuiltTextSearchCoversMetadataAndFolders() {
        val entries = LibrarySearchIndex.build(
            listOf(
                book("1", "Solaris", author = "Stanisław Lem", folders = listOf("Sci-Fi/Polish")),
                book("2", "Dune", author = "Frank Herbert"),
            ),
        )
        assertEquals(listOf("1"), LibrarySearchIndex.filter(entries, "polish", "", LibraryGroupMode.NONE).map { it.id })
        assertEquals(listOf("2"), LibrarySearchIndex.filter(entries, "herbert", "", LibraryGroupMode.NONE).map { it.id })
    }

    @Test
    fun normalizedGroupAndSeriesFiltersAreCaseInsensitive() {
        val entries = LibrarySearchIndex.build(
            listOf(
                book("1", "One", series = "Foundation", groups = listOf("Favorites")),
                book("2", "Two", series = "Other", groups = listOf("Archive")),
            ),
        )
        assertEquals(listOf("1"), LibrarySearchIndex.filter(entries, "", "g:favorites", LibraryGroupMode.CUSTOM).map { it.id })
        assertEquals(listOf("1"), LibrarySearchIndex.filter(entries, "", "s:foundation", LibraryGroupMode.SERIES).map { it.id })
    }
}
