package io.fluxreader.app.data

/** Pure archive rules kept outside Android I/O so safety behavior is unit-testable. */
object ArchiveImportPolicy {
    const val MAX_ENTRIES = 5_000
    const val MAX_BOOKS = 1_000
    const val MAX_PATH_DEPTH = 32
    const val MAX_ARCHIVE_BYTES = 1L * 1024L * 1024L * 1024L
    const val MAX_BOOK_BYTES = 512L * 1024L * 1024L
    const val MAX_TOTAL_BOOK_BYTES = 2L * 1024L * 1024L * 1024L

    data class BookEntry(
        val originalPath: String,
        val fileName: String,
        val parentSegments: List<String>,
        val format: BookFormat,
    )

    fun isGenericZip(displayName: String, mimeType: String?): Boolean {
        val lower = displayName.trim().lowercase()
        if (lower.endsWith(".fb2.zip")) return false
        if (lower.endsWith(".zip")) return true
        return mimeType?.lowercase() in setOf(
            "application/zip",
            "application/x-zip-compressed",
            "application/x-zip",
        )
    }

    /**
     * Returns a supported book entry or null. Absolute paths, parent traversal and absurdly deep
     * paths are rejected before any bytes are copied, so ZIP entries can never escape the virtual
     * archive folder or influence filesystem destinations.
     */
    fun bookEntry(rawName: String): BookEntry? {
        if (rawName.isBlank() || '\u0000' in rawName) return null
        val normalized = rawName.replace('\\', '/')
        if (normalized.startsWith('/')) return null
        val segments = normalized.split('/').filter { it.isNotBlank() && it != "." }
        if (segments.isEmpty() || segments.size > MAX_PATH_DEPTH + 1) return null
        if (segments.any { it == ".." }) return null
        val fileName = segments.last()
        val format = formatForName(fileName) ?: return null
        return BookEntry(
            originalPath = normalized,
            fileName = fileName,
            parentSegments = segments.dropLast(1),
            format = format,
        )
    }

    fun formatForName(name: String): BookFormat? {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".epub") -> BookFormat.EPUB
            lower.endsWith(".fb2") || lower.endsWith(".fb2.zip") -> BookFormat.FB2
            lower.endsWith(".txt") -> BookFormat.TXT
            lower.endsWith(".pdf") -> BookFormat.PDF
            else -> null
        }
    }
}
