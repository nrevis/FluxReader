package io.fluxreader.app.ui

internal sealed interface PreparedReaderPageItem {
    val blockIndex: Int

    data class TextSlice(
        override val blockIndex: Int,
        val startCharOffset: Int,
        val endCharOffset: Int,
        val measuredHeightPx: Int,
    ) : PreparedReaderPageItem

    data class Image(
        override val blockIndex: Int,
    ) : PreparedReaderPageItem
}

internal data class PreparedReaderPage(
    val items: List<PreparedReaderPageItem>,
) {
    val startBlockIndex: Int
        get() = items.firstOrNull()?.blockIndex ?: 0

    val startCharOffset: Int
        get() = (items.firstOrNull() as? PreparedReaderPageItem.TextSlice)?.startCharOffset ?: 0
}

internal data class PreparedReaderPagination(
    val chapterIndex: Int,
    val layoutKey: String,
    val pages: List<PreparedReaderPage>,
    val contentHeightPx: Int,
    /** False only for the fast anchor preview shown while exact pagination continues. */
    val isComplete: Boolean = true,
)

/** Small session cache: revisiting a chapter with the same viewport should be instant. */
internal class PreparedReaderPaginationCache(maxEntries: Int = 8) {
    private val capacity = maxEntries.coerceIn(2, 16)
    private val entries = object : LinkedHashMap<String, PreparedReaderPagination>(capacity, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, PreparedReaderPagination>?): Boolean =
            size > capacity
    }

    fun get(layoutKey: String): PreparedReaderPagination? = entries[layoutKey]

    fun put(value: PreparedReaderPagination) {
        // Preview pages are deliberately ephemeral: caching one would make a later revisit look
        // complete even though only the anchor page was prepared.
        if (value.isComplete) entries[value.layoutKey] = value
    }

    fun clear() = entries.clear()

    internal fun sizeForTest(): Int = entries.size
}
internal object ReaderPaginationCommitPolicy {
    /**
     * A pagination result may be published only for the viewport/style key that is still active.
     * This protects rotation/fullscreen/font-size stress cases where one non-cancellable text
     * measurement finishes just after the surrounding coroutine has been superseded.
     */
    fun shouldCommit(requestLayoutKey: String, currentLayoutKey: String): Boolean =
        requestLayoutKey == currentLayoutKey
}

