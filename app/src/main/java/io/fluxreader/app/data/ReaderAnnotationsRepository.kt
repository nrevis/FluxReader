package io.fluxreader.app.data

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class ReaderAnnotationKind { HIGHLIGHT, QUOTE, NOTE, BOOKMARK }

data class ReaderAnnotation(
    val id: String,
    val bookId: String,
    val chapterIndex: Int,
    val startBlockIndex: Int,
    val startCharOffset: Int,
    val endBlockIndex: Int,
    val endCharOffsetExclusive: Int,
    val excerpt: String,
    val kind: ReaderAnnotationKind,
    val colorArgb: Int = 0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
)

/** Persistent reader-owned annotations. Book source files are never modified. */
class ReaderAnnotationsRepository(context: Context) {
    private val file = File(context.filesDir, "reader-annotations.json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var persistJob: Job? = null
    private val _annotations = MutableStateFlow(load())
    val annotations: StateFlow<List<ReaderAnnotation>> = _annotations.asStateFlow()

    fun add(
        bookId: String,
        chapterIndex: Int,
        blockIndex: Int,
        startCharOffset: Int,
        endCharOffsetExclusive: Int,
        excerpt: String,
        kind: ReaderAnnotationKind,
        colorArgb: Int = 0,
        note: String = "",
    ): ReaderAnnotation = addRange(
        bookId = bookId,
        chapterIndex = chapterIndex,
        startBlockIndex = blockIndex,
        startCharOffset = startCharOffset,
        endBlockIndex = blockIndex,
        endCharOffsetExclusive = endCharOffsetExclusive,
        excerpt = excerpt,
        kind = kind,
        colorArgb = colorArgb,
        note = note,
    )

    fun addRange(
        bookId: String,
        chapterIndex: Int,
        startBlockIndex: Int,
        startCharOffset: Int,
        endBlockIndex: Int,
        endCharOffsetExclusive: Int,
        excerpt: String,
        kind: ReaderAnnotationKind,
        colorArgb: Int = 0,
        note: String = "",
    ): ReaderAnnotation {
        val safeStartBlock = startBlockIndex.coerceAtLeast(0)
        val safeEndBlock = endBlockIndex.coerceAtLeast(safeStartBlock)
        val start = startCharOffset.coerceAtLeast(0)
        val end = endCharOffsetExclusive.coerceAtLeast(if (safeStartBlock == safeEndBlock) start + 1 else 1)
        val now = System.currentTimeMillis()
        val item = ReaderAnnotation(
            id = UUID.randomUUID().toString(),
            bookId = bookId,
            chapterIndex = chapterIndex.coerceAtLeast(0),
            startBlockIndex = safeStartBlock,
            startCharOffset = start,
            endBlockIndex = safeEndBlock,
            endCharOffsetExclusive = end,
            excerpt = excerpt.trim().take(8_000),
            kind = kind,
            colorArgb = colorArgb,
            note = note.trim().take(12_000),
            createdAt = now,
            updatedAt = now,
        )
        _annotations.value = (_annotations.value + item).sortedByDescending { it.updatedAt }
        persistSoon()
        return item
    }

    fun updateNote(id: String, note: String) {
        val now = System.currentTimeMillis()
        _annotations.value = _annotations.value.map { item ->
            if (item.id == id) item.copy(note = note.trim().take(12_000), updatedAt = now) else item
        }.sortedByDescending { it.updatedAt }
        persistSoon()
    }

    fun updateColor(id: String, colorArgb: Int) {
        val now = System.currentTimeMillis()
        _annotations.value = _annotations.value.map { item ->
            if (item.id == id) item.copy(colorArgb = colorArgb, updatedAt = now) else item
        }.sortedByDescending { it.updatedAt }
        persistSoon()
    }

    fun updateRange(
        id: String,
        startBlockIndex: Int,
        startCharOffset: Int,
        endBlockIndex: Int,
        endCharOffsetExclusive: Int,
        excerpt: String,
    ) {
        val safeStartBlock = startBlockIndex.coerceAtLeast(0)
        val safeEndBlock = endBlockIndex.coerceAtLeast(safeStartBlock)
        val safeStart = startCharOffset.coerceAtLeast(0)
        val safeEnd = endCharOffsetExclusive.coerceAtLeast(if (safeStartBlock == safeEndBlock) safeStart + 1 else 1)
        val now = System.currentTimeMillis()
        _annotations.value = _annotations.value.map { item ->
            if (item.id == id) item.copy(
                startBlockIndex = safeStartBlock,
                startCharOffset = safeStart,
                endBlockIndex = safeEndBlock,
                endCharOffsetExclusive = safeEnd,
                excerpt = excerpt.trim().take(8_000),
                updatedAt = now,
            ) else item
        }.sortedByDescending { it.updatedAt }
        persistSoon()
    }

    fun delete(id: String) {
        val next = _annotations.value.filterNot { it.id == id }
        if (next.size == _annotations.value.size) return
        _annotations.value = next
        persistSoon()
    }

    fun deleteForBook(bookId: String) {
        val next = _annotations.value.filterNot { it.bookId == bookId }
        if (next.size == _annotations.value.size) return
        _annotations.value = next
        persistSoon()
    }

    private fun load(): List<ReaderAnnotation> = runCatching {
        if (!file.isFile) return@runCatching emptyList()
        val array = JSONArray(file.readText())
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val kind = runCatching { ReaderAnnotationKind.valueOf(obj.optString("kind")) }.getOrNull() ?: continue
                val bookId = obj.optString("bookId")
                if (bookId.isBlank()) continue
                val start = obj.optInt("startCharOffset", 0).coerceAtLeast(0)
                val end = obj.optInt("endCharOffsetExclusive", start + 1).coerceAtLeast(start + 1)
                add(
                    ReaderAnnotation(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        bookId = bookId,
                        chapterIndex = obj.optInt("chapterIndex", 0).coerceAtLeast(0),
                        startBlockIndex = obj.optInt("startBlockIndex", 0).coerceAtLeast(0),
                        startCharOffset = start,
                        endBlockIndex = obj.optInt("endBlockIndex", obj.optInt("startBlockIndex", 0)).coerceAtLeast(0),
                        endCharOffsetExclusive = end,
                        excerpt = obj.optString("excerpt"),
                        kind = kind,
                        colorArgb = obj.optInt("colorArgb", 0),
                        note = obj.optString("note"),
                        createdAt = obj.optLong("createdAt", 0L),
                        updatedAt = obj.optLong("updatedAt", obj.optLong("createdAt", 0L)),
                    )
                )
            }
        }.sortedByDescending { it.updatedAt }
    }.getOrDefault(emptyList())

    private fun persistSoon() {
        val snapshot = _annotations.value
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(120)
            save(snapshot)
        }
    }

    private fun save(items: List<ReaderAnnotation>) {
        runCatching {
            val array = JSONArray()
            items.forEach { item ->
                array.put(
                    JSONObject()
                        .put("id", item.id)
                        .put("bookId", item.bookId)
                        .put("chapterIndex", item.chapterIndex)
                        .put("startBlockIndex", item.startBlockIndex)
                        .put("startCharOffset", item.startCharOffset)
                        .put("endBlockIndex", item.endBlockIndex)
                        .put("endCharOffsetExclusive", item.endCharOffsetExclusive)
                        .put("excerpt", item.excerpt)
                        .put("kind", item.kind.name)
                        .put("colorArgb", item.colorArgb)
                        .put("note", item.note)
                        .put("createdAt", item.createdAt)
                        .put("updatedAt", item.updatedAt)
                )
            }
            val tmp = File(file.parentFile, "reader-annotations.json.tmp")
            tmp.writeText(array.toString())
            if (file.exists() && !file.delete()) return@runCatching
            if (!tmp.renameTo(file)) {
                file.writeText(tmp.readText())
                tmp.delete()
            }
        }
    }
}
