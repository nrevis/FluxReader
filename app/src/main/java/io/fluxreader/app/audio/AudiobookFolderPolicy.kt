package io.fluxreader.app.audio

/** Paths of SAF-backed audio entries follow the same virtual folder hierarchy as books. */
object AudiobookFolderPolicy {
    fun normalize(path: String): String = path.trim().trim('/').split('/').filter { it.isNotBlank() }.joinToString("/")

    /** Return a rebased path only if this entry belongs to the renamed/moved folder. */
    fun rebase(current: String, from: String, to: String): String? {
        val parent = normalize(current)
        val source = normalize(from)
        if (source.isBlank() || (parent != source && !parent.startsWith("$source/"))) return null
        val suffix = parent.removePrefix(source).trimStart('/')
        return listOf(normalize(to), suffix).filter(String::isNotBlank).joinToString("/")
    }
}
