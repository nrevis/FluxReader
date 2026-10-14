package io.fluxreader.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class SmartBookImportTest {
    @Test
    fun epub2GuideCoverPageResolvesNestedImage() {
        val file = File.createTempFile("flux-cover", ".epub")
        file.deleteOnExit()
        ZipOutputStream(file.outputStream().buffered()).use { zip ->
            fun entry(name: String, text: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
            entry(
                "META-INF/container.xml",
                """<?xml version="1.0"?><container xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""",
            )
            entry(
                "OEBPS/content.opf",
                """<?xml version="1.0"?><package xmlns="http://www.idpf.org/2007/opf"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Test book</dc:title><dc:creator>Test author</dc:creator></metadata><manifest><item id="cover-page" href="cover.xhtml" media-type="application/xhtml+xml"/><item id="front" href="images/front.jpg" media-type="image/jpeg"/></manifest><guide><reference type="cover" href="cover.xhtml"/></guide></package>""",
            )
            entry(
                "OEBPS/cover.xhtml",
                """<!DOCTYPE html><html xmlns="http://www.w3.org/1999/xhtml"><body><img src="images/front.jpg"/></body></html>""",
            )
            zip.putNextEntry(ZipEntry("OEBPS/images/front.jpg"))
            zip.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 1, 2, 3, 0xFF.toByte(), 0xD9.toByte()))
            zip.closeEntry()
        }

        val result = SmartBookImport.inspect(file, BookFormat.EPUB, "fallback.epub")
        assertEquals("Test book", result.metadata.title)
        assertEquals("Test author", result.metadata.author)
        assertNotNull(result.coverBytes)
        assertTrue(result.coverBytes!!.size >= 7)
    }
}
