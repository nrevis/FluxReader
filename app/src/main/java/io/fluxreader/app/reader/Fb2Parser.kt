package io.fluxreader.app.reader

import io.fluxreader.app.data.BookMetadata
import io.fluxreader.app.data.BookOutline
import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ChapterRef
import io.fluxreader.app.data.ReaderBlock
import io.fluxreader.app.data.ReaderLink
import io.fluxreader.app.data.TextRole
import org.json.JSONArray
import org.json.JSONObject
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.InputStream
import java.util.Base64
import java.util.Locale
import java.util.zip.ZipFile
import javax.xml.parsers.SAXParserFactory

object Fb2Parser {
    private const val CACHE_VERSION = 4
    private const val MAX_IMAGE_BYTES = 40 * 1024 * 1024
    private const val MAX_BASE64_CHARS = 56 * 1024 * 1024

    private data class LinkTarget(val target: String, val external: Boolean)

    private class RichParagraph {
        private val text = StringBuilder()
        private val links = mutableListOf<ReaderLink>()
        private var pendingSpace = false
        private var activeLink: LinkTarget? = null
        private var activeStart = -1

        fun append(raw: String) {
            raw.forEach { char ->
                if (char.isWhitespace()) {
                    if (text.isNotEmpty()) pendingSpace = true
                } else {
                    if (pendingSpace && text.isNotEmpty() && text.last() != ' ') text.append(' ')
                    pendingSpace = false
                    text.append(char)
                }
            }
        }

        fun startLink(link: LinkTarget?) {
            if (pendingSpace && text.isNotEmpty() && text.last() != ' ') {
                text.append(' ')
                pendingSpace = false
            }
            closeLink()
            if (link != null) {
                activeLink = link
                activeStart = text.length
            }
        }

        fun endLink() = closeLink()

        fun finish(): Pair<String, List<ReaderLink>> {
            closeLink()
            return text.toString() to links.filter { it.start < it.end && it.end <= text.length }
        }

        private fun closeLink() {
            val link = activeLink
            if (link != null && activeStart >= 0 && activeStart < text.length) {
                links += ReaderLink(activeStart, text.length, link.target, link.external)
            }
            activeLink = null
            activeStart = -1
        }
    }

    fun prepare(file: File, bookId: String, cacheRoot: File): BookOutline {
        val dir = File(cacheRoot, "fb2/$bookId")
        val marker = File(dir, "outline.json")
        readOutline(marker, file)?.let { return it }
        dir.deleteRecursively()
        dir.mkdirs()
        File(dir, "images").mkdirs()
        return openFb2(file).use { input -> parseToCache(input, file, dir, marker) }
    }

    fun loadChapter(bookId: String, cacheRoot: File, ref: ChapterRef): ChapterContent {
        val file = File(cacheRoot, "fb2/$bookId/${ref.sourceRef}")
        val root = JSONObject(file.readText())
        val blocksArray = root.optJSONArray("blocks") ?: JSONArray()
        val blocks = buildList {
            for (i in 0 until blocksArray.length()) {
                val item = blocksArray.optJSONObject(i) ?: continue
                when (item.optString("type")) {
                    "text" -> add(
                        ReaderBlock.Text(
                            text = item.optString("text"),
                            role = runCatching { TextRole.valueOf(item.optString("role")) }.getOrDefault(TextRole.PARAGRAPH),
                            anchors = item.optJSONArray("anchors").toStringList(),
                            links = item.optJSONArray("links").toReaderLinks(),
                        )
                    )
                    "image" -> add(
                        ReaderBlock.Image(
                            ref = item.optString("ref"),
                            alt = item.optString("alt"),
                            anchors = item.optJSONArray("anchors").toStringList(),
                        )
                    )
                }
            }
        }
        return ChapterContent(ref.index, root.optString("title", ref.title), blocks)
    }

    fun loadImage(bookId: String, cacheRoot: File, ref: String): ByteArray? {
        val file = File(cacheRoot, "fb2/$bookId/images/${safeName(ref)}")
        return file.takeIf { it.isFile && it.length() <= MAX_IMAGE_BYTES.toLong() }?.readBytes()
    }

    private fun parseToCache(input: InputStream, sourceFile: File, dir: File, marker: File): BookOutline {
        val refs = mutableListOf<ChapterRef>()
        var bookTitle = ""
        val authorParts = mutableListOf<String>()

        val handler = object : DefaultHandler() {
            private var inDescription = false
            private var inTitleInfo = false
            private var inBody = false
            private var currentBodyName = ""
            private var currentBodyIsNotes = false
            private var sectionDepth = 0
            private var inTitle = false
            private var quoteDepth = 0
            private var poemDepth = 0
            private var tableDepth = 0
            private var stanzaBlocks: MutableList<ReaderBlock.Text>? = null
            private var paragraph: RichParagraph? = null
            private var paragraphRole = TextRole.PARAGRAPH
            private var paragraphAnchors = mutableListOf<String>()
            private var metadataTag: String? = null
            private var metadataText = StringBuilder()
            private var currentBlocks: MutableList<ReaderBlock>? = null
            private var currentTitle = ""
            private var noteChapterCounter = 0
            private val pendingAnchors = linkedSetOf<String>()
            private var binaryId: String? = null
            private var binaryText: StringBuilder? = null
            private var binaryTooLarge = false

            private fun tag(localName: String, qName: String): String =
                localName.ifBlank { qName.substringAfter(':') }.lowercase(Locale.ROOT)

            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                val name = tag(localName.orEmpty(), qName.orEmpty())
                when (name) {
                    "description" -> inDescription = true
                    "title-info" -> if (inDescription) inTitleInfo = true
                    "book-title", "first-name", "middle-name", "last-name", "nickname" -> if (inTitleInfo) {
                        metadataTag = name
                        metadataText = StringBuilder()
                    }
                    "body" -> {
                        finalizeChapter()
                        inBody = true
                        currentBodyName = attributes?.getValue("name").orEmpty().trim()
                        currentBodyIsNotes = isNotesBody(currentBodyName)
                        sectionDepth = 0
                        pendingAnchors.clear()
                        ownAnchorIds(attributes).forEach(pendingAnchors::add)
                    }
                    "section" -> if (inBody) {
                        sectionDepth++
                        if (sectionDepth == 1) {
                            finalizeChapter()
                            currentBlocks = mutableListOf()
                        }
                        ownAnchorIds(attributes).forEach(pendingAnchors::add)
                    }
                    "title" -> if (inBody) {
                        inTitle = true
                        ownAnchorIds(attributes).forEach(pendingAnchors::add)
                    }
                    "cite", "epigraph" -> if (inBody) {
                        quoteDepth++
                        ownAnchorIds(attributes).forEach(pendingAnchors::add)
                    }
                    "poem" -> if (inBody) {
                        poemDepth++
                        ownAnchorIds(attributes).forEach(pendingAnchors::add)
                    }
                    "table" -> if (inBody) {
                        tableDepth++
                        ownAnchorIds(attributes).forEach(pendingAnchors::add)
                    }
                    "stanza" -> if (inBody) {
                        finalizeParagraph()
                        flushStanza()
                        stanzaBlocks = mutableListOf()
                        ownAnchorIds(attributes).forEach(pendingAnchors::add)
                    }
                    "tr" -> if (inBody) ownAnchorIds(attributes).forEach(pendingAnchors::add)
                    "td", "th" -> if (inBody) startParagraph(TextRole.TABLE, ownAnchorIds(attributes))
                    "subtitle" -> if (inBody) startParagraph(TextRole.HEADING, ownAnchorIds(attributes))
                    "p" -> if (inBody) startParagraph(
                        role = when {
                            inTitle -> TextRole.HEADING
                            quoteDepth > 0 -> TextRole.QUOTE
                            poemDepth > 0 -> TextRole.POEM
                            tableDepth > 0 -> TextRole.TABLE
                            else -> TextRole.PARAGRAPH
                        },
                        anchors = ownAnchorIds(attributes),
                    )
                    "v" -> if (inBody) startParagraph(TextRole.POEM, ownAnchorIds(attributes))
                    "text-author", "date" -> if (inBody) startParagraph(
                        role = if (poemDepth > 0) TextRole.POEM else TextRole.QUOTE,
                        anchors = ownAnchorIds(attributes),
                    )
                    "a" -> if (inBody && paragraph != null) paragraph?.startLink(normalizeLinkTarget(linkHref(attributes)))
                    "image" -> if (inBody) {
                        finalizeParagraph()
                        if (currentBlocks == null) currentBlocks = mutableListOf()
                        val href = linkHref(attributes)?.removePrefix("#")?.trim().orEmpty()
                        if (href.isNotBlank()) {
                            currentBlocks?.add(
                                ReaderBlock.Image(
                                    ref = href,
                                    anchors = consumeAnchors(ownAnchorIds(attributes)),
                                )
                            )
                        }
                    }
                    "binary" -> {
                        binaryId = attributes?.getValue("id")
                        binaryText = StringBuilder()
                        binaryTooLarge = false
                    }
                    else -> if (inBody) {
                        // IDs may legally live on wrappers such as table/tr/td/empty-line.
                        ownAnchorIds(attributes).forEach(pendingAnchors::add)
                    }
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                val text = String(ch, start, length)
                paragraph?.append(text)
                if (metadataTag != null) metadataText.append(text)
                binaryText?.let { buffer ->
                    if (buffer.length + text.length <= MAX_BASE64_CHARS) buffer.append(text)
                    else {
                        binaryTooLarge = true
                        binaryText = null
                    }
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                val name = tag(localName.orEmpty(), qName.orEmpty())
                when (name) {
                    "book-title", "first-name", "middle-name", "last-name", "nickname" -> if (metadataTag == name) {
                        val value = metadataText.toString().normalizeSpace()
                        if (name == "book-title") bookTitle = value
                        else if (value.isNotBlank()) authorParts += value
                        metadataTag = null
                        metadataText = StringBuilder()
                    }
                    "a" -> if (inBody) paragraph?.endLink()
                    "p", "v", "subtitle", "text-author", "date", "td", "th" -> if (inBody) finalizeParagraph()
                    "title" -> if (inBody) inTitle = false
                    "cite", "epigraph" -> if (inBody) quoteDepth = (quoteDepth - 1).coerceAtLeast(0)
                    "stanza" -> if (inBody) {
                        finalizeParagraph()
                        flushStanza()
                    }
                    "poem" -> if (inBody) {
                        flushStanza()
                        poemDepth = (poemDepth - 1).coerceAtLeast(0)
                    }
                    "table" -> if (inBody) tableDepth = (tableDepth - 1).coerceAtLeast(0)
                    "section" -> if (inBody) {
                        if (sectionDepth == 1) finalizeChapter()
                        sectionDepth = (sectionDepth - 1).coerceAtLeast(0)
                    }
                    "body" -> if (inBody) {
                        finalizeChapter()
                        inBody = false
                        currentBodyName = ""
                        currentBodyIsNotes = false
                        sectionDepth = 0
                        pendingAnchors.clear()
                    }
                    "title-info" -> inTitleInfo = false
                    "description" -> inDescription = false
                    "binary" -> {
                        val id = binaryId
                        val payload = binaryText?.toString().orEmpty()
                        if (!binaryTooLarge && !id.isNullOrBlank() && payload.isNotBlank()) {
                            runCatching {
                                val bytes = Base64.getMimeDecoder().decode(payload)
                                if (bytes.size <= MAX_IMAGE_BYTES) {
                                    File(dir, "images/${safeName(id)}").writeBytes(bytes)
                                }
                            }
                        }
                        binaryId = null
                        binaryText = null
                        binaryTooLarge = false
                    }
                }
            }

            private fun startParagraph(role: TextRole, anchors: List<String>) {
                finalizeParagraph()
                if (currentBlocks == null) currentBlocks = mutableListOf()
                paragraph = RichParagraph()
                paragraphRole = role
                paragraphAnchors = anchors.toMutableList()
            }

            private fun finalizeParagraph() {
                val active = paragraph ?: return
                val (text, links) = active.finish()
                if (text.isNotBlank()) {
                    val anchors = consumeAnchors(paragraphAnchors)
                    val resolvedRole = if (
                        paragraphRole == TextRole.HEADING &&
                        inTitle &&
                        !Regex("(?iu)^\\s*глава\\s+[0-9ivxlcdm]+\\b").containsMatchIn(text)
                    ) TextRole.TITLE else paragraphRole
                    val block = ReaderBlock.Text(text, resolvedRole, anchors, links)
                    if (resolvedRole == TextRole.POEM && stanzaBlocks != null) stanzaBlocks?.add(block)
                    else currentBlocks?.add(block)
                    if ((resolvedRole == TextRole.HEADING || resolvedRole == TextRole.TITLE) && currentTitle.isBlank()) currentTitle = text.take(180)
                } else {
                    paragraphAnchors.forEach(pendingAnchors::add)
                }
                paragraph = null
                paragraphAnchors = mutableListOf()
            }

            private fun flushStanza() {
                val lines = stanzaBlocks ?: return
                if (lines.isNotEmpty()) {
                    val text = StringBuilder()
                    val links = mutableListOf<ReaderLink>()
                    val anchors = linkedSetOf<String>()
                    lines.forEachIndexed { index, line ->
                        if (index > 0) text.append('\n')
                        val offset = text.length
                        text.append(line.text)
                        line.links.forEach { link ->
                            links += link.copy(start = link.start + offset, end = link.end + offset)
                        }
                        anchors += line.anchors
                    }
                    currentBlocks?.add(ReaderBlock.Text(text.toString(), TextRole.POEM, anchors.toList(), links))
                }
                stanzaBlocks = null
            }

            private fun consumeAnchors(extra: List<String> = emptyList()): List<String> {
                val result = linkedSetOf<String>()
                result += pendingAnchors
                result += extra.filter { it.isNotBlank() }
                pendingAnchors.clear()
                return result.toList()
            }

            private fun finalizeChapter() {
                finalizeParagraph()
                flushStanza()
                val blocks = currentBlocks ?: return
                if (blocks.isEmpty()) {
                    currentBlocks = null
                    currentTitle = ""
                    pendingAnchors.clear()
                    return
                }
                val index = refs.size
                val title = chapterTitle(index)
                val name = "chapter_${index.toString().padStart(5, '0')}.json"
                writeChapter(File(dir, name), title, blocks)
                val anchors = blocks.flatMap { it.anchors }.distinct()
                refs += ChapterRef(index, title, name, anchors)
                currentBlocks = null
                currentTitle = ""
                pendingAnchors.clear()
            }

            private fun chapterTitle(index: Int): String {
                val raw = currentTitle.trim()
                if (currentBodyIsNotes) {
                    noteChapterCounter++
                    if (raw.isBlank()) return "Примечание $noteChapterCounter"
                    return if (raw.matches(Regex("^[0-9*†‡§№.()\\[\\] -]{1,24}$"))) {
                        "Примечание ${raw.trim().trimEnd('.')}"
                    } else raw
                }
                if (raw.isNotBlank()) return raw
                return currentBodyName.takeIf { it.isNotBlank() }?.let { "$it ${index + 1}" } ?: "Глава ${index + 1}"
            }

            fun finish() {
                finalizeChapter()
            }
        }

        val factory = SAXParserFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        factory.newSAXParser().parse(input, handler)
        handler.finish()

        if (refs.isEmpty()) {
            val name = "chapter_00000.json"
            writeChapter(File(dir, name), "Текст", listOf(ReaderBlock.Text("", TextRole.PARAGRAPH)))
            refs += ChapterRef(0, "Текст", name)
        }
        val metadata = BookMetadata(
            title = bookTitle.ifBlank { sourceFile.name.replace(Regex("(?i)\\.fb2(?:\\.zip)?$"), "") },
            author = authorParts.joinToString(" ").normalizeSpace(),
        )
        writeOutline(marker, sourceFile, metadata, refs)
        return BookOutline(metadata.title, metadata.author, refs)
    }

    private fun writeChapter(file: File, title: String, blocks: List<ReaderBlock>) {
        val array = JSONArray()
        blocks.forEach { block ->
            when (block) {
                is ReaderBlock.Text -> array.put(
                    JSONObject()
                        .put("type", "text")
                        .put("text", block.text)
                        .put("role", block.role.name)
                        .put("anchors", block.anchors.toStringJsonArray())
                        .put("links", block.links.toLinkJsonArray())
                )
                is ReaderBlock.Image -> array.put(
                    JSONObject()
                        .put("type", "image")
                        .put("ref", block.ref)
                        .put("alt", block.alt)
                        .put("anchors", block.anchors.toStringJsonArray())
                )
            }
        }
        file.writeText(JSONObject().put("title", title).put("blocks", array).toString())
    }

    private fun writeOutline(file: File, source: File, metadata: BookMetadata, refs: List<ChapterRef>) {
        val chapters = JSONArray()
        refs.forEach { ref ->
            chapters.put(
                JSONObject()
                    .put("index", ref.index)
                    .put("title", ref.title)
                    .put("sourceRef", ref.sourceRef)
                    .put("anchors", ref.anchors.toStringJsonArray())
            )
        }
        file.writeText(
            JSONObject()
                .put("version", CACHE_VERSION)
                .put("sourceLength", source.length())
                .put("sourceModified", source.lastModified())
                .put("title", metadata.title)
                .put("author", metadata.author)
                .put("chapters", chapters)
                .toString()
        )
    }

    private fun readOutline(file: File, source: File): BookOutline? = runCatching {
        if (!file.isFile) return@runCatching null
        val obj = JSONObject(file.readText())
        if (obj.optInt("version") != CACHE_VERSION) return@runCatching null
        if (obj.optLong("sourceLength") != source.length()) return@runCatching null
        if (obj.optLong("sourceModified") != source.lastModified()) return@runCatching null
        val array = obj.optJSONArray("chapters") ?: return@runCatching null
        val refs = buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    ChapterRef(
                        index = item.optInt("index", i),
                        title = item.optString("title", "Глава ${i + 1}"),
                        sourceRef = item.optString("sourceRef"),
                        anchors = item.optJSONArray("anchors").toStringList(),
                    )
                )
            }
        }
        BookOutline(obj.optString("title"), obj.optString("author"), refs)
    }.getOrNull()

    private fun openFb2(file: File): InputStream {
        if (!file.name.lowercase(Locale.ROOT).endsWith(".zip")) return file.inputStream().buffered()
        val zip = ZipFile(file)
        val entry = zip.entries().asSequence().firstOrNull {
            !it.isDirectory && it.name.lowercase(Locale.ROOT).endsWith(".fb2")
        } ?: run {
            zip.close()
            error("FB2.ZIP: файл .fb2 не найден")
        }
        val stream = zip.getInputStream(entry)
        return object : InputStream() {
            override fun read(): Int = stream.read()
            override fun read(b: ByteArray, off: Int, len: Int): Int = stream.read(b, off, len)
            override fun close() {
                runCatching { stream.close() }
                runCatching { zip.close() }
            }
        }.buffered()
    }

    private fun ownAnchorIds(attributes: Attributes?): List<String> = buildList {
        sequenceOf(
            attributes?.getValue("id"),
            attributes?.getValue("http://www.w3.org/XML/1998/namespace", "id"),
            attributes?.getValue("xml:id"),
        ).map { it.orEmpty().trim() }.filter { it.isNotBlank() }.forEach { if (it !in this) add(it) }
    }

    private fun linkHref(attributes: Attributes?): String? = sequenceOf(
        attributes?.getValue("http://www.w3.org/1999/xlink", "href"),
        attributes?.getValue("xlink:href"),
        attributes?.getValue("href"),
    ).firstOrNull { !it.isNullOrBlank() }

    private fun normalizeLinkTarget(raw: String?): LinkTarget? {
        val href = raw.orEmpty().trim()
        if (href.isBlank()) return null
        if (href.startsWith("javascript:", ignoreCase = true) || href.startsWith("data:", ignoreCase = true)) return null
        val external = Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(href)
        if (external) return LinkTarget(href, true)
        if (href.startsWith('#')) return href.removePrefix("#").trim().takeIf { it.isNotBlank() }?.let { LinkTarget("#$it", false) }
        val fragment = href.substringAfter('#', "").trim()
        if (fragment.isNotBlank()) return LinkTarget("#$fragment", false)
        return LinkTarget(href, false)
    }

    private fun isNotesBody(name: String): Boolean = name.trim().lowercase(Locale.ROOT).let { normalized ->
        normalized == "notes" || normalized == "note" || normalized == "comments" ||
            normalized == "footnotes" || normalized == "endnotes" || normalized == "примечания"
    }

    private fun List<String>.toStringJsonArray(): JSONArray = JSONArray().also { array -> forEach(array::put) }

    private fun List<ReaderLink>.toLinkJsonArray(): JSONArray = JSONArray().also { array ->
        forEach { link ->
            array.put(
                JSONObject()
                    .put("start", link.start)
                    .put("end", link.end)
                    .put("target", link.target)
                    .put("external", link.external)
            )
        }
    }

    private fun JSONArray?.toStringList(): List<String> = buildList {
        val array = this@toStringList ?: return@buildList
        for (i in 0 until array.length()) {
            array.optString(i).trim().takeIf { it.isNotBlank() }?.let { if (it !in this) add(it) }
        }
    }

    private fun JSONArray?.toReaderLinks(): List<ReaderLink> = buildList {
        val array = this@toReaderLinks ?: return@buildList
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val start = item.optInt("start", -1)
            val end = item.optInt("end", -1)
            val target = item.optString("target").trim()
            if (start >= 0 && end > start && target.isNotBlank()) {
                add(ReaderLink(start, end, target, item.optBoolean("external", false)))
            }
        }
    }

    private fun safeName(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(180)
    private fun String.normalizeSpace(): String = replace(Regex("\\s+"), " ").trim()
}
