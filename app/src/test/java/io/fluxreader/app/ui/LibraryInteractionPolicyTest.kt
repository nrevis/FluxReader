package io.fluxreader.app.ui

import io.fluxreader.app.data.LibraryGroupMode
import org.junit.Assert.*
import org.junit.Test

class LibraryInteractionPolicyTest {
    @Test fun firstLongPressSelectsButDoesNotOpenTheGroupMenu() {
        val result = LibraryInteractionPolicy.longPressBook(emptySet(), false, "one")
        assertEquals(setOf("one"), result.selectedIds)
        assertFalse(result.showActions)
    }

    @Test fun longPressInSelectionKeepsTheWholeGroupAndIncludesThePressedBook() {
        val selected = setOf("one", "two")
        assertEquals(selected, LibraryInteractionPolicy.longPressBook(selected, true, "one").selectedIds)
        val result = LibraryInteractionPolicy.longPressBook(selected, true, "three")
        assertEquals(setOf("one", "two", "three"), result.selectedIds)
        assertTrue(result.showActions)
    }

    @Test fun additionsUseTheOpenFolderEvenWhenOtherViewsRememberAnOldPath() {
        assertEquals("Книги/Фэнтези", LibraryInteractionPolicy.destinationFolder(LibraryGroupMode.FOLDER, " /Книги//Фэнтези/ "))
        assertEquals("", LibraryInteractionPolicy.destinationFolder(LibraryGroupMode.NONE, "Книги/Фэнтези"))
        assertEquals("", LibraryInteractionPolicy.destinationFolder(LibraryGroupMode.FOLDER, ""))
    }
}
