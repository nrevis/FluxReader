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
}
