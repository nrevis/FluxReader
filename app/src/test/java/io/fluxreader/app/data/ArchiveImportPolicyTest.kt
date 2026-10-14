package io.fluxreader.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveImportPolicyTest {
    @Test fun genericZipDoesNotCaptureFb2Zip() {
        assertTrue(ArchiveImportPolicy.isGenericZip("books.zip", "application/zip"))
        assertFalse(ArchiveImportPolicy.isGenericZip("novel.fb2.zip", "application/zip"))
        assertFalse(ArchiveImportPolicy.isGenericZip("novel.epub", "application/epub+zip"))
    }

    @Test fun acceptsBooksAndPreservesSafeParentSegments() {
        val entry = ArchiveImportPolicy.bookEntry("Series/Author/Book.epub")!!
        assertEquals(BookFormat.EPUB, entry.format)
        assertEquals("Book.epub", entry.fileName)
        assertEquals(listOf("Series", "Author"), entry.parentSegments)
    }

    @Test fun rejectsTraversalAndUnsupportedEntries() {
        assertNull(ArchiveImportPolicy.bookEntry("../escape.epub"))
        assertNull(ArchiveImportPolicy.bookEntry("/absolute/book.pdf"))
        assertNull(ArchiveImportPolicy.bookEntry("covers/cover.jpg"))
    }
}
