package io.fluxreader.app.reader

data class SearchOccurrence(
    val startOffset: Int,
    val endOffset: Int,
    val snippet: String,
)

/** Pure text search used by Reader/PDF search. No Android or Compose dependency. */
object SearchEngine {
    fun find(query: String, text: String, limit: Int = 6): List<SearchOccurrence> {
        val needle = query.trim()
        if (needle.length < 2 || text.isBlank() || limit <= 0) return emptyList()
        val haystackFolded = text.lowercase()
        val needleFolded = needle.lowercase()
        val result = ArrayList<SearchOccurrence>(minOf(limit, 6))
        var cursor = 0
        while (cursor < haystackFolded.length && result.size < minOf(limit, 6)) {
            val index = haystackFolded.indexOf(needleFolded, cursor)
            if (index < 0) break
            val end = (index + needle.length).coerceAtMost(text.length)
            val from = (index - 54).coerceAtLeast(0)
            val to = (end + 72).coerceAtMost(text.length)
            val compact = text.substring(from, to)
                .replace(Regex("""\s+"""), " ")
                .trim()
            result += SearchOccurrence(
                startOffset = index,
                endOffset = end,
                snippet = (if (from > 0) "…" else "") + compact + (if (to < text.length) "…" else ""),
            )
            cursor = (index + needleFolded.length.coerceAtLeast(1)).coerceAtMost(haystackFolded.length)
        }
        return result
    }
}
