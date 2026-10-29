package io.fluxreader.app.ui

import io.fluxreader.app.data.LibraryFolderTree
import io.fluxreader.app.data.LibraryGroupMode

internal object LibraryInteractionPolicy {
    /** Search filters the current view; it must not redirect new content to the root. */
    fun destinationFolder(groupMode: LibraryGroupMode, currentFolder: String): String =
        if (groupMode == LibraryGroupMode.FOLDER) LibraryFolderTree.normalize(currentFolder) else ""

    data class BookLongPress(val selectedIds: Set<String>, val showActions: Boolean)

    fun longPressBook(selectedIds: Set<String>, selectionMode: Boolean, bookId: String) =
        BookLongPress(selectedIds + bookId, showActions = selectionMode)

    /** A selected parent already includes its children; never process a subtree twice. */
    fun topLevelFolders(paths: Set<String>): List<String> {
        val normalized = paths.map(LibraryFolderTree::normalize).filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        return normalized.filter { path ->
            normalized.none { other -> !other.equals(path, ignoreCase = true) && LibraryFolderTree.isSameOrDescendant(path, other) }
        }
    }

    fun folderDestinations(knownFolders: List<String>, selectedFolders: Set<String>): List<String> =
        knownFolders.filter { target -> selectedFolders.none { LibraryFolderTree.isSameOrDescendant(target, it) } }

}
