package io.fluxreader.app.data

data class LibraryFolderSummary(
    val path: String,
    val name: String,
    val bookCount: Int,
)

/** Pure folder-tree logic shared by the SAF importer, repository operations, and Compose library UI. */
object LibraryFolderTree {
    fun normalize(path: String): String = path
        .replace('\\', '/')
        .split('/')
        .map { it.trim() }
        .filter { it.isNotBlank() && it != "." }
        .fold(mutableListOf<String>()) { out, part ->
            if (part == "..") {
                if (out.isNotEmpty()) out.removeAt(out.lastIndex)
            } else {
                out += part
            }
            out
        }
        .joinToString("/")

    fun join(parent: String, child: String): String = normalize(
        listOf(parent, child).filter { it.isNotBlank() }.joinToString("/"),
    )

    fun parent(path: String): String {
        val clean = normalize(path)
        return clean.substringBeforeLast('/', missingDelimiterValue = "")
    }

    fun name(path: String): String = normalize(path).substringAfterLast('/')

    fun breadcrumbs(path: String): List<Pair<String, String>> {
        val clean = normalize(path)
        if (clean.isBlank()) return emptyList()
        val result = mutableListOf<Pair<String, String>>()
        var current = ""
        clean.split('/').forEach { part ->
            current = join(current, part)
            result += part to current
        }
        return result
    }

    fun ancestors(path: String, includeSelf: Boolean = true): List<String> {
        var cursor = normalize(path)
        if (cursor.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        if (includeSelf) result += cursor
        while (cursor.isNotBlank()) {
            cursor = parent(cursor)
            if (cursor.isNotBlank()) result += cursor
        }
        return result.asReversed()
    }

    fun allFolders(books: List<BookRecord>, explicitFolders: Collection<String> = emptyList()): List<String> {
        val folders = linkedSetOf<String>()
        explicitFolders.forEach { raw ->
            val clean = normalize(raw)
            if (clean.isNotBlank()) ancestors(clean).forEach(folders::add)
        }
        books.forEach { book ->
            book.folderPaths.forEach { raw ->
                val clean = normalize(raw)
                if (clean.isNotBlank()) ancestors(clean).forEach(folders::add)
            }
        }
        return folders.sortedWith(compareBy<String> { it.count { ch -> ch == '/' } }.thenBy { it.lowercase() })
    }

    fun directBooks(books: List<BookRecord>, folderPath: String): List<BookRecord> {
        val folder = normalize(folderPath)
        return books.filter { book ->
            if (book.folderPaths.isEmpty()) folder.isBlank()
            else book.folderPaths.any { normalize(it) == folder }
        }
    }

    fun childFolders(
        books: List<BookRecord>,
        folderPath: String,
        explicitFolders: Collection<String> = emptyList(),
    ): List<LibraryFolderSummary> {
        val folder = normalize(folderPath)
        val prefix = if (folder.isBlank()) "" else "$folder/"
        val childPaths = linkedSetOf<String>()
        allFolders(books, explicitFolders).forEach { path ->
            val remainder = when {
                folder.isBlank() -> path
                path == folder -> ""
                path.startsWith(prefix) -> path.removePrefix(prefix)
                else -> return@forEach
            }
            if (remainder.isBlank()) return@forEach
            childPaths += join(folder, remainder.substringBefore('/'))
        }
        return childPaths.map { childPath ->
            val childPrefix = "$childPath/"
            val ids = books.asSequence()
                .filter { book ->
                    book.folderPaths.any { raw ->
                        val path = normalize(raw)
                        path == childPath || path.startsWith(childPrefix)
                    }
                }
                .map { it.id }
                .toSet()
            LibraryFolderSummary(childPath, name(childPath), ids.size)
        }.sortedBy { it.name.lowercase() }
    }

    fun containsFolder(
        books: List<BookRecord>,
        folderPath: String,
        explicitFolders: Collection<String> = emptyList(),
    ): Boolean {
        val folder = normalize(folderPath)
        if (folder.isBlank()) return true
        if (allFolders(books, explicitFolders).any { it.equals(folder, ignoreCase = true) }) return true
        val prefix = "$folder/"
        return books.any { book ->
            book.folderPaths.any { raw ->
                val path = normalize(raw)
                path == folder || path.startsWith(prefix)
            }
        }
    }

    fun isSameOrDescendant(path: String, ancestor: String): Boolean {
        val cleanPath = normalize(path)
        val cleanAncestor = normalize(ancestor)
        if (cleanAncestor.isBlank()) return true
        return cleanPath == cleanAncestor || cleanPath.startsWith("$cleanAncestor/")
    }

    fun replacePrefix(path: String, oldPrefix: String, newPrefix: String): String {
        val cleanPath = normalize(path)
        val old = normalize(oldPrefix)
        val replacement = normalize(newPrefix)
        if (!isSameOrDescendant(cleanPath, old) || old.isBlank()) return cleanPath
        val suffix = cleanPath.removePrefix(old).removePrefix("/")
        return join(replacement, suffix)
    }

    fun siblingNameExists(folders: Collection<String>, parentPath: String, candidateName: String, excludingPath: String = ""): Boolean {
        val parent = normalize(parentPath)
        val excluded = normalize(excludingPath)
        val candidate = candidateName.trim()
        return folders.any { raw ->
            val path = normalize(raw)
            path != excluded && parent(path) == parent && name(path).equals(candidate, ignoreCase = true)
        }
    }

    fun uniqueSiblingName(folders: Collection<String>, parentPath: String, preferredName: String): String {
        val cleanPreferred = preferredName.trim().ifBlank { "Новая папка" }
        if (!siblingNameExists(folders, parentPath, cleanPreferred)) return cleanPreferred
        var index = 2
        while (true) {
            val candidate = "$cleanPreferred $index"
            if (!siblingNameExists(folders, parentPath, candidate)) return candidate
            index++
        }
    }
}
