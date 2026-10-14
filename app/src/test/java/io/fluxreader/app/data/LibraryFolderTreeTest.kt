package io.fluxreader.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFolderTreeTest {
    private fun book(id: String, vararg folders: String) = BookRecord(
        id = id,
        title = id,
        author = "",
        filePath = "/tmp/$id.epub",
        originalName = "$id.epub",
        format = BookFormat.EPUB,
        folderPaths = folders.toList(),
    )

    @Test
    fun hierarchyPreservesNestedFoldersAndDirectBooks() {
        val books = listOf(
            book("root"),
            book("a", "Books/Fantasy"),
            book("b", "Books/Fantasy/Rowling"),
            book("c", "Books/Science"),
        )
        assertEquals(listOf("Books"), LibraryFolderTree.childFolders(books, "").map { it.name })
        assertEquals(listOf("Fantasy", "Science"), LibraryFolderTree.childFolders(books, "Books").map { it.name })
        assertEquals(listOf("a"), LibraryFolderTree.directBooks(books, "Books/Fantasy").map { it.id })
        assertEquals(listOf("b"), LibraryFolderTree.directBooks(books, "Books/Fantasy/Rowling").map { it.id })
        assertEquals(listOf("root"), LibraryFolderTree.directBooks(books, "").map { it.id })
    }

    @Test
    fun duplicateContentCanBelongToMoreThanOneFolder() {
        val books = listOf(book("same", "Shelf/A", "Shelf/B"))
        assertEquals(listOf("same"), LibraryFolderTree.directBooks(books, "Shelf/A").map { it.id })
        assertEquals(listOf("same"), LibraryFolderTree.directBooks(books, "Shelf/B").map { it.id })
        assertTrue(LibraryFolderTree.containsFolder(books, "Shelf"))
    }

    @Test
    fun pathsAreNormalizedAndBreadcrumbed() {
        assertEquals("Books/Fantasy", LibraryFolderTree.normalize(" /Books//Fantasy/ "))
        assertEquals("Books", LibraryFolderTree.parent("Books/Fantasy"))
        assertEquals(
            listOf("Books" to "Books", "Fantasy" to "Books/Fantasy"),
            LibraryFolderTree.breadcrumbs("Books/Fantasy"),
        )
    }
    @Test
    fun explicitEmptyFoldersRemainVisible() {
        val books = emptyList<BookRecord>()
        val folders = listOf("Shelf/Empty", "Shelf/Another/Nested")
        assertEquals(listOf("Shelf"), LibraryFolderTree.childFolders(books, "", folders).map { it.name })
        assertEquals(listOf("Another", "Empty"), LibraryFolderTree.childFolders(books, "Shelf", folders).map { it.name })
        assertTrue(LibraryFolderTree.containsFolder(books, "Shelf/Empty", folders))
    }

    @Test
    fun prefixRemapPreservesNestedSuffix() {
        assertEquals(
            "Archive/Fantasy/Rowling",
            LibraryFolderTree.replacePrefix("Books/Fantasy/Rowling", "Books", "Archive"),
        )
        assertEquals("Archive", LibraryFolderTree.replacePrefix("Books", "Books", "Archive"))
        assertTrue(LibraryFolderTree.isSameOrDescendant("Books/Fantasy", "Books"))
    }

    @Test
    fun uniqueSiblingNameAvoidsCaseInsensitiveConflicts() {
        val folders = listOf("Shelf/Work", "Shelf/Work — копия", "Shelf/Work — копия 2")
        assertTrue(LibraryFolderTree.siblingNameExists(folders, "Shelf", "work"))
        assertEquals("Work — копия 3", LibraryFolderTree.uniqueSiblingName(folders, "Shelf", "Work — копия"))
    }

}
