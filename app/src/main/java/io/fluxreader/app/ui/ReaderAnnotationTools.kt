package io.fluxreader.app.ui

import io.fluxreader.app.data.ReaderAnnotation
import io.fluxreader.app.data.ReaderAnnotationKind

enum class ReaderAnnotationScope { CURRENT_BOOK, ALL_BOOKS }
enum class ReaderAnnotationSortMode { DATE, BOOK, CHAPTER }
enum class ReaderAnnotationExportFormat { MARKDOWN, TEXT }

object ReaderAnnotationTools {
    fun filterAndSort(
        annotations: List<ReaderAnnotation>,
        currentBookId: String,
        scope: ReaderAnnotationScope,
        kind: ReaderAnnotationKind?,
        query: String,
        sortMode: ReaderAnnotationSortMode,
        bookTitle: (String) -> String,
    ): List<ReaderAnnotation> {
        val q = query.trim().lowercase()
        val filtered = annotations.asSequence()
            .filter { scope == ReaderAnnotationScope.ALL_BOOKS || it.bookId == currentBookId }
            .filter { kind == null || it.kind == kind }
            .filter {
                q.isBlank() || listOf(it.excerpt, it.note, bookTitle(it.bookId)).any { text -> text.lowercase().contains(q) }
            }
            .toList()
        return when (sortMode) {
            ReaderAnnotationSortMode.DATE -> filtered.sortedByDescending { it.updatedAt }
            ReaderAnnotationSortMode.BOOK -> filtered.sortedWith(compareBy<ReaderAnnotation> { bookTitle(it.bookId).lowercase() }
                .thenBy { it.chapterIndex }.thenBy { it.startBlockIndex }.thenBy { it.startCharOffset })
            ReaderAnnotationSortMode.CHAPTER -> filtered.sortedWith(compareBy<ReaderAnnotation> { it.chapterIndex }
                .thenBy { it.startBlockIndex }.thenBy { it.startCharOffset }.thenByDescending { it.updatedAt })
        }
    }

    fun export(
        items: List<ReaderAnnotation>,
        format: ReaderAnnotationExportFormat,
        bookTitle: (String) -> String,
        chapterTitle: (ReaderAnnotation) -> String,
        dateLabel: (Long) -> String,
    ): String = when (format) {
        ReaderAnnotationExportFormat.MARKDOWN -> buildString {
            appendLine("# Flux Reader — аннотации")
            appendLine()
            items.forEach { item ->
                appendLine("## ${bookTitle(item.bookId).ifBlank { "Книга" }}")
                val chapter = chapterTitle(item).ifBlank { "Глава ${item.chapterIndex + 1}" }
                appendLine("**$chapter · ${kindLabel(item.kind)} · ${dateLabel(item.updatedAt)}**")
                appendLine()
                if (item.excerpt.isNotBlank()) appendLine("> ${item.excerpt.replace("\n", "\n> ")}")
                if (item.note.isNotBlank()) {
                    appendLine()
                    appendLine("Заметка: ${item.note}")
                }
                appendLine()
            }
        }
        ReaderAnnotationExportFormat.TEXT -> buildString {
            appendLine("Flux Reader — аннотации")
            appendLine("========================")
            appendLine()
            items.forEach { item ->
                appendLine(bookTitle(item.bookId).ifBlank { "Книга" })
                appendLine("${chapterTitle(item).ifBlank { "Глава ${item.chapterIndex + 1}" }} · ${kindLabel(item.kind)} · ${dateLabel(item.updatedAt)}")
                if (item.excerpt.isNotBlank()) appendLine(item.excerpt)
                if (item.note.isNotBlank()) appendLine("Заметка: ${item.note}")
                appendLine()
            }
        }
    }

    fun kindLabel(kind: ReaderAnnotationKind): String = when (kind) {
        ReaderAnnotationKind.HIGHLIGHT -> "Выделение"
        ReaderAnnotationKind.QUOTE -> "Цитата"
        ReaderAnnotationKind.NOTE -> "Заметка"
        ReaderAnnotationKind.BOOKMARK -> "Закладка"
    }
}
