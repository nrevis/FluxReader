package io.fluxreader.app.reader

import io.fluxreader.app.data.ReaderBlock
import io.fluxreader.app.data.TextRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class EpubParserTest {
    @Test
    fun readsSpineNavigationTitlesAndOnlyRequestedChapter() {
        val file = File.createTempFile("flux-test-", ".epub")
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                fun put(name: String, text: String) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(text.toByteArray())
                    zip.closeEntry()
                }
                put("META-INF/container.xml", """<?xml version="1.0"?><container xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
                put("OEBPS/content.opf", """<?xml version="1.0"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Test Book</dc:title><dc:creator>Author</dc:creator></metadata><manifest><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" properties="nav"/><item id="c1" href="c1.xhtml" media-type="application/xhtml+xml"/><item id="c2" href="c2.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="c1"/><itemref idref="c2"/></spine></package>""")
                put("OEBPS/nav.xhtml", """<html xmlns="http://www.w3.org/1999/xhtml"><body><nav><ol><li><a href="c1.xhtml#start">Начало</a></li><li><a href="c2.xhtml">Продолжение</a></li></ol></nav></body></html>""")
                put("OEBPS/c1.xhtml", """<html xmlns="http://www.w3.org/1999/xhtml"><body><h1>One</h1><p id="start">Hello <a href="c2.xhtml#note-1">footnote</a>.</p><div class="poem"><p>Line one</p><p>Line two</p></div><table><tbody><tr id="row-1"><td>A</td><td><a href="#start">B</a></td></tr></tbody></table></body></html>""")
                put("OEBPS/c2.xhtml", """<html xmlns="http://www.w3.org/1999/xhtml"><body><h1>Two</h1><p>Second chapter.</p><aside id="note-1"><p>Footnote body.</p></aside></body></html>""")
            }
            val outline = EpubParser.outline(file)
            assertEquals("Test Book", outline.title)
            assertEquals(2, outline.chapters.size)
            assertEquals("Начало", outline.chapters[0].title)
            assertEquals("Продолжение", outline.chapters[1].title)
            val first = EpubParser.loadChapter(file, outline.chapters[0])
            val linked = first.blocks.filterIsInstance<ReaderBlock.Text>().first { it.text.contains("footnote") }
            assertEquals(listOf("start"), linked.anchors)
            assertEquals(1, linked.links.size)
            assertEquals("OEBPS/c2.xhtml#note-1", linked.links.single().target)
            assertEquals("footnote", linked.text.substring(linked.links.single().start, linked.links.single().end))
            val poemLines = first.blocks.filterIsInstance<ReaderBlock.Text>().filter { it.role == TextRole.POEM }
            assertEquals(listOf("Line one", "Line two"), poemLines.map { it.text })
            val tableRow = first.blocks.filterIsInstance<ReaderBlock.Text>().first { it.role == TextRole.TABLE }
            assertEquals("A │ B", tableRow.text)
            assertTrue("row-1" in tableRow.anchors)
            assertEquals("OEBPS/c1.xhtml#start", tableRow.links.single().target)
            assertEquals("B", tableRow.text.substring(tableRow.links.single().start, tableRow.links.single().end))

            val chapter = EpubParser.loadChapter(file, outline.chapters[1])
            assertEquals(1, chapter.index)
            assertTrue(chapter.blocks.filterIsInstance<ReaderBlock.Text>().any { it.text.contains("Second chapter") })
            val footnote = chapter.blocks.filterIsInstance<ReaderBlock.Text>().first { it.text.contains("Footnote body") }
            assertTrue("note-1" in footnote.anchors)
        } finally {
            file.delete()
        }
    }
    @Test
    fun preservesAnnotationParagraphAndNestedListsWithoutFlattening() {
        val file = File.createTempFile("flux-annotation-", ".epub")
        try {
            ZipOutputStream(file.outputStream()).use { zip ->
                fun put(name: String, text: String) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(text.toByteArray())
                    zip.closeEntry()
                }
                put("META-INF/container.xml", """<?xml version="1.0"?><container xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
                put("OPS/content.opf", """<?xml version="1.0"?><package xmlns="http://www.idpf.org/2007/opf" version="3.0"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:title>Stories</dc:title><dc:creator>Author</dc:creator></metadata><manifest><item id="ann" href="annotation.xhtml" media-type="application/xhtml+xml"/></manifest><spine><itemref idref="ann"/></spine></package>""")
                put(
                    "OPS/annotation.xhtml",
                    """<html xmlns="http://www.w3.org/1999/xhtml"><body>
                        <h1>Annotation</h1>
                        <div class="annotation">В книге представлено <em>собрание прозы</em> автора.</div>
                        <ul>
                          <li><a href="author.xhtml">Татьяна Толстая</a>
                            <ul>
                              <li><a href="story.xhtml">Как работает рассказ Толстой</a></li>
                              <li><a href="kys.xhtml">Кысь</a>
                                <ul><li><a href="az.xhtml">Аз</a></li><li><a href="buki.xhtml">Буки</a></li></ul>
                              </li>
                            </ul>
                          </li>
                        </ul>
                    </body></html>""",
                )
            }
            val outline = EpubParser.outline(file)
            val chapter = EpubParser.loadChapter(file, outline.chapters.single())
            val texts = chapter.blocks.filterIsInstance<ReaderBlock.Text>()
            assertTrue(texts.any { it.role == TextRole.PARAGRAPH && it.text == "В книге представлено собрание прозы автора." })
            val items = texts.filter { it.role == TextRole.LIST_ITEM }
            assertEquals(5, items.size)
            assertEquals(
                listOf("Татьяна Толстая", "Как работает рассказ Толстой", "Кысь", "Аз", "Буки"),
                items.map { it.text },
            )
            assertEquals(listOf(0, 1, 1, 2, 2), items.map { it.listDepth })
            assertEquals(listOf("•", "◦", "◦", "▪", "▪"), items.map { it.listMarker })
            assertEquals("OPS/author.xhtml", items.first().links.single().target)
            assertEquals("Татьяна Толстая", items.first().text.substring(items.first().links.single().start, items.first().links.single().end))
            assertTrue(items.none { it.text.contains("КысьАз") || it.text.contains("ТолстаяКак") })
        } finally {
            file.delete()
        }
    }

    @Test
    fun promotesCssOnlyChapterLabelAndUppercaseTitle() {
        val blocks = listOf<ReaderBlock>(
            ReaderBlock.Text("Глава 16", TextRole.PARAGRAPH),
            ReaderBlock.Text("ОЧЕНЬ ХОЛОДНОЕ РОЖДЕСТВО", TextRole.PARAGRAPH),
            ReaderBlock.Text("— Так, значит, Снегг предлагал ему помощь?", TextRole.PARAGRAPH),
        )
        val normalized = EpubParser.normalizeChapterHeadingRoles(blocks, "Очень холодное рождество")
        val texts = normalized.filterIsInstance<ReaderBlock.Text>()
        assertEquals(TextRole.HEADING, texts[0].role)
        assertEquals(TextRole.TITLE, texts[1].role)
        assertEquals(TextRole.PARAGRAPH, texts[2].role)
    }


}
