package io.fluxreader.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFormPolicyTest {
    @Test
    fun folderNameIsTrimmedAndBlankNameCannotBeSubmitted() {
        assertEquals("Фантастика", LibraryFormPolicy.normalizedFolderName("  Фантастика  "))
        assertTrue(LibraryFormPolicy.canSubmitFolderName(" Книги "))
        assertFalse(LibraryFormPolicy.canSubmitFolderName("   "))
    }

    @Test
    fun metadataRequiresANonBlankTitle() {
        assertEquals("Дюна", LibraryFormPolicy.normalizedBookTitle("  Дюна  "))
        assertTrue(LibraryFormPolicy.canSaveBookMetadata("Дюна"))
        assertFalse(LibraryFormPolicy.canSaveBookMetadata(" \n\t "))
    }
}
