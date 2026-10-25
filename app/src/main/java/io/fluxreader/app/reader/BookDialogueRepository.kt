package io.fluxreader.app.reader

import io.fluxreader.app.data.BookFormat
import io.fluxreader.app.data.BookOutline
import io.fluxreader.app.data.BookRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File

data class BookDialogueProgress(val stage: Int, val completed: Int, val total: Int, val characters: Int = 0) {
    val label: String get() = when (stage) {
        1 -> "Персонажи: $completed из $total глав"
        2 -> "Диалоги: $completed из $total глав · имён: $characters"
        else -> "Анализ книги завершён · имён: $characters"
    }
}

/** Two streaming passes; keeps at most one chapter and a bounded roster in memory. */
class BookDialogueRepository(cacheRoot: File, private val content: BookContentRepository) {
    private val cache = BookDialogueDiskCache(File(cacheRoot, "book-dialogues-v1"))

    suspend fun analyze(
        book: BookRecord,
        outline: BookOutline,
        currentChapter: Int,
        onChapter: suspend (Int, ChapterDialogueIndex) -> Unit,
        onProgress: suspend (BookDialogueProgress) -> Unit,
    ) = withContext(Dispatchers.IO) {
        require(book.format != BookFormat.PDF) { "Первый вариант анализа поддерживает EPUB, FB2 и TXT" }
        val file = File(book.filePath)
        fun sourceToken() = ChapterDialogueIndex.fingerprint("${book.id}:${book.contentHash}:${file.length()}:${file.lastModified()}")
        val source = sourceToken()
        val order = outline.chapters.indices.sortedBy { if (it == currentChapter) -1 else it }
        val names = linkedSetOf<String>()
        val context = currentCoroutineContext()
        val checkpoint = { context.ensureActive() }
        // First render of the selected chapter does not wait for the remaining book.
        for ((done, index) in order.withIndex()) {
            checkpoint()
            val ref = outline.chapters[index]
            val text = content.speedDocument(book, ref).text
            val chapterKey = "${ref.index}:${ref.sourceRef}"
            val local = cache.read(source, chapterKey, "local", text) ?: withContext(Dispatchers.Default) {
                BookDialogueAnalyzer.local(text, checkpoint)
            }.also { cache.write(source, chapterKey, "local", it) }
            for (entry in local.entries) {
                val speaker = entry.span.speaker
                if (speaker != null && names.size < 2048) names += speaker
            }
            if (index == currentChapter) onChapter(index, local)
            onProgress(BookDialogueProgress(1, done + 1, order.size))
            yield()
        }
        val roster = DialogueCharacterRoster(names)
        for ((done, index) in order.withIndex()) {
            checkpoint()
            check(sourceToken() == source) { "Файл книги изменился. Откройте книгу повторно." }
            val ref = outline.chapters[index]
            val chapterKey = "${ref.index}:${ref.sourceRef}"
            val text = content.speedDocument(book, ref).text
            val enriched = cache.read(source, chapterKey, roster.fingerprint, text) ?: run {
                val local = cache.read(source, chapterKey, "local", text) ?: withContext(Dispatchers.Default) {
                    BookDialogueAnalyzer.local(text, checkpoint)
                }
                withContext(Dispatchers.Default) {
                    BookDialogueAnalyzer.enrich(text, local, roster, checkpoint)
                }.also { cache.write(source, chapterKey, roster.fingerprint, it) }
            }
            onChapter(index, enriched)
            onProgress(BookDialogueProgress(2, done + 1, order.size, roster.names.size))
            yield()
        }
        onProgress(BookDialogueProgress(3, order.size, order.size, roster.names.size))
    }
}
