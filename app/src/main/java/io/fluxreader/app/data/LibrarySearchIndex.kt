package io.fluxreader.app.data

import java.util.Locale

/** Pre-normalized library search data, built once per library snapshot instead of once per keypress. */
internal data class LibrarySearchEntry(
    val book: BookRecord,
    val searchableText: String,
    val groupKeys: Set<String>,
    val seriesKey: String,
)

internal object LibrarySearchIndex {
    fun build(books: List<BookRecord>): List<LibrarySearchEntry> = books.map { book ->
        LibrarySearchEntry(
            book = book,
            searchableText = buildString {
                append(book.title)
                append('\u0000').append(book.author)
                append('\u0000').append(book.series)
                append('\u0000').append(book.publisher)
                book.groups.forEach { append('\u0000').append(it) }
                book.folderPaths.forEach { append('\u0000').append(it) }
            }.lowercase(Locale.ROOT),
            groupKeys = book.groups.asSequence().map { it.lowercase(Locale.ROOT) }.toSet(),
            seriesKey = book.series.lowercase(Locale.ROOT),
        )
    }

    fun filter(
        entries: List<LibrarySearchEntry>,
        query: String,
        selectedGroup: String,
        groupMode: LibraryGroupMode,
    ): List<BookRecord> {
        val q = query.trim().lowercase(Locale.ROOT)
        val groupKey = selectedGroup.removePrefix("g:").lowercase(Locale.ROOT)
        val seriesKey = selectedGroup.removePrefix("s:").lowercase(Locale.ROOT)
        return entries.asSequence()
            .filter { entry ->
                val matchesQuery = q.isBlank() || q in entry.searchableText
                val matchesGroup = if (groupMode == LibraryGroupMode.FOLDER) true else when {
                    selectedGroup.isBlank() -> true
                    selectedGroup.startsWith("g:") -> groupKey in entry.groupKeys
                    selectedGroup.startsWith("s:") -> entry.seriesKey == seriesKey
                    else -> true
                }
                matchesQuery && matchesGroup
            }
            .map { it.book }
            .toList()
    }
}
