package io.fluxreader.app.data

import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.Base64
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

/** Lightweight import-time metadata and cover discovery. Failures deliberately fall back to filename metadata. */
object SmartBookImport {
    data class Result(
        val metadata: BookMetadata,
        val coverBytes: ByteArray? = null,
    )

    fun inspect(file: File, format: BookFormat, originalName: String): Result {
        val fallback = metadataFromFilename(originalName)
        return runCatching {
            when (format) {
                BookFormat.EPUB -> inspectEpub(file, fallback)
                BookFormat.FB2 -> inspectFb2(file, fallback)
                BookFormat.TXT, BookFormat.PDF -> Result(fallback)
            }
        }.getOrElse { Result(fallback) }
    }

    private fun inspectEpub(file: File, fallback: BookMetadata): Result = ZipFile(file).use { zip ->
        val container = zip.getEntry("META-INF/container.xml") ?: return@use Result(fallback)
        val containerDoc = zip.getInputStream(container).use(::parseXml)
        val rootfile = elements(containerDoc, "rootfile").firstOrNull() ?: return@use Result(fallback)
        val opfPath = rootfile.getAttribute("full-path").trim()
        val opfEntry = zip.getEntry(opfPath) ?: return@use Result(fallback)
        val opf = zip.getInputStream(opfEntry).use(::parseXml)
        val baseDir = opfPath.substringBeforeLast('/', "")

        val title = firstText(opf, "title").ifBlank { fallback.title }
        val author = firstText(opf, "creator").ifBlank { fallback.author }
        val publisher = firstText(opf, "publisher")
        val language = firstText(opf, "language")
        val description = firstText(opf, "description")
        val year = firstText(opf, "date").take(4).takeIf { it.all(Char::isDigit) }.orEmpty()

        var series = ""
        var seriesIndex = 0f
        val metas = elements(opf, "meta")
        metas.forEach { meta ->
            val name = meta.getAttribute("name").trim().lowercase()
            val property = meta.getAttribute("property").trim().lowercase()
            when {
                name == "calibre:series" -> series = meta.getAttribute("content").trim()
                name == "calibre:series_index" -> seriesIndex = meta.getAttribute("content").trim().toFloatOrNull() ?: seriesIndex
                property == "belongs-to-collection" && series.isBlank() -> series = meta.textContent.normalizeSpace()
                property == "group-position" && seriesIndex == 0f -> seriesIndex = meta.textContent.normalizeSpace().toFloatOrNull() ?: 0f
            }
        }
        if (series.isBlank()) series = fallback.series
        if (seriesIndex == 0f) seriesIndex = fallback.seriesIndex

        val manifest = elements(opf, "item")
        val coverBytes = findEpubCover(zip, opf, baseDir, manifest, metas)

        Result(
            BookMetadata(
                title = title,
                author = author,
                series = series,
                seriesIndex = seriesIndex,
                description = description.take(8000),
                publisher = publisher,
                language = language,
                year = year,
            ),
            coverBytes,
        )
    }


    private fun findEpubCover(
        zip: ZipFile,
        opf: Document,
        baseDir: String,
        manifest: List<Element>,
        metas: List<Element>,
    ): ByteArray? {
        val coverId = metas.firstOrNull { it.getAttribute("name").equals("cover", true) }
            ?.getAttribute("content")?.trim().orEmpty()

        val preferred = buildList {
            manifest.firstOrNull { item ->
                item.getAttribute("properties").split(Regex("\\s+")).any { it.equals("cover-image", true) }
            }?.let(::add)
            if (coverId.isNotBlank()) manifest.firstOrNull { it.getAttribute("id") == coverId }?.let(::add)
        }.distinctBy { it.getAttribute("href") }

        preferred.forEach { item ->
            readEpubCoverTarget(zip, baseDir, item.getAttribute("href"), item.getAttribute("media-type"))?.let { return it }
        }

        // EPUB 2 often points to an XHTML cover page from <guide><reference type="cover" .../>.
        elements(opf, "reference")
            .firstOrNull { it.getAttribute("type").trim().equals("cover", true) }
            ?.getAttribute("href")
            ?.takeIf { it.isNotBlank() }
            ?.let { href -> readEpubCoverTarget(zip, baseDir, href, "") }
            ?.let { return it }

        // Real-world EPUBs frequently omit the official cover markers. Prefer image items whose
        // id/path looks like a cover, then fall back to the largest plausible raster image.
        val rasterItems = manifest.filter { item ->
            isRasterImage(item.getAttribute("media-type"), item.getAttribute("href"))
        }
        val ranked = rasterItems.sortedWith(
            compareByDescending<Element> { item -> coverHintScore(item.getAttribute("id"), item.getAttribute("href")) }
                .thenByDescending { item ->
                    val path = normalizeZipPath(baseDir, item.getAttribute("href").substringBefore('#'))
                    zip.getEntry(path)?.size?.coerceAtMost(MAX_COVER_BYTES) ?: 0L
                },
        )
        ranked.take(8).forEach { item ->
            readEpubCoverTarget(zip, baseDir, item.getAttribute("href"), item.getAttribute("media-type"))?.let { return it }
        }
        return null
    }

    private fun readEpubCoverTarget(zip: ZipFile, baseDir: String, rawHref: String, mediaType: String): ByteArray? {
        val href = rawHref.trim().substringBefore('#')
        if (href.isBlank()) return null
        val path = normalizeZipPath(baseDir, href)
        if (isRasterImage(mediaType, href)) return readZipCover(zip, path)

        val lowerType = mediaType.lowercase()
        val lowerPath = path.lowercase()
        val isMarkup = lowerType.contains("html") || lowerType.contains("xhtml") ||
            lowerPath.endsWith(".xhtml") || lowerPath.endsWith(".html") || lowerPath.endsWith(".htm") || lowerPath.endsWith(".xml")
        if (!isMarkup) return null
        val entry = zip.getEntry(path) ?: return null
        val markup = zip.getInputStream(entry).use { it.readBytesLimited(MAX_COVER_MARKUP_BYTES) } ?: return null
        val imageHref = coverReferenceFromMarkup(markup) ?: return null
        val wrapperDir = path.substringBeforeLast('/', "")
        return readZipCover(zip, normalizeZipPath(wrapperDir, imageHref.substringBefore('#')))
    }

    private fun coverReferenceFromMarkup(bytes: ByteArray): String? {
        val text = bytes.toString(StandardCharsets.UTF_8)
        val patterns = listOf(
            Regex("""(?is)<img\b[^>]*?\bsrc\s*=\s*[\"']([^\"']+)[\"']"""),
            Regex("""(?is)<image\b[^>]*?(?:xlink:href|href)\s*=\s*[\"']([^\"']+)[\"']"""),
            Regex("""(?is)<object\b[^>]*?\bdata\s*=\s*[\"']([^\"']+)[\"']"""),
        )
        return patterns.firstNotNullOfOrNull { it.find(text)?.groupValues?.getOrNull(1) }
    }

    private fun readZipCover(zip: ZipFile, path: String): ByteArray? {
        val entry = zip.getEntry(path) ?: return null
        if (entry.isDirectory || entry.size == 0L || entry.size > MAX_COVER_BYTES) return null
        return zip.getInputStream(entry).use { it.readBytesLimited(MAX_COVER_BYTES.toInt()) }
    }

    private fun isRasterImage(mediaType: String, href: String): Boolean {
        val type = mediaType.trim().lowercase()
        if (type == "image/svg+xml") return false
        if (type.startsWith("image/") && type !in setOf("image/svg", "image/svg+xml")) return true
        val lower = href.substringBefore('?').substringBefore('#').lowercase()
        return RASTER_EXTENSIONS.any(lower::endsWith)
    }

    private fun coverHintScore(id: String, href: String): Int {
        val value = "$id $href".lowercase()
        return when {
            "cover" in value -> 100
            "front" in value -> 80
            "titlepage" in value || "title-page" in value -> 70
            "folder" in value -> 0
            else -> 10
        }
    }

    private fun inspectFb2(file: File, fallback: BookMetadata): Result {
        val sourceBytes = openFb2(file).use { input -> input.readBytesLimited(MAX_FB2_IMPORT_BYTES) } ?: return Result(fallback)
        val doc = parseXml(ByteArrayInputStream(sourceBytes))
        val titleInfo = elements(doc, "title-info").firstOrNull()
        val title = titleInfo?.let { firstText(it, "book-title") }.orEmpty().ifBlank { fallback.title }
        val author = titleInfo?.let(::fb2Author).orEmpty().ifBlank { fallback.author }
        val sequence = titleInfo?.let { elements(it, "sequence").firstOrNull() }
        val series = sequence?.getAttribute("name")?.trim().orEmpty().ifBlank { fallback.series }
        val seriesIndex = sequence?.getAttribute("number")?.trim()?.toFloatOrNull() ?: fallback.seriesIndex
        val language = titleInfo?.let { firstText(it, "lang") }.orEmpty()
        val annotation = titleInfo?.let { elements(it, "annotation").firstOrNull()?.textContent?.normalizeSpace() }.orEmpty()
        val publishInfo = elements(doc, "publish-info").firstOrNull()
        val publisher = publishInfo?.let { firstText(it, "publisher") }.orEmpty()
        val year = publishInfo?.let { firstText(it, "year") }.orEmpty()

        val coverHref = titleInfo?.let { info ->
            elements(info, "coverpage").firstOrNull()?.let { cover ->
                elements(cover, "image").firstOrNull()?.let(::hrefAttribute)
            }
        }.orEmpty().removePrefix("#")
        val coverBytes = if (coverHref.isBlank()) null else {
            elements(doc, "binary").firstOrNull { it.getAttribute("id") == coverHref }?.textContent
                ?.filterNot(Char::isWhitespace)
                ?.takeIf { it.length <= MAX_COVER_BASE64_CHARS }
                ?.let { encoded -> runCatching { Base64.getDecoder().decode(encoded) }.getOrNull() }
                ?.takeIf { it.size <= MAX_COVER_BYTES }
        }

        return Result(
            BookMetadata(
                title = title,
                author = author,
                series = series,
                seriesIndex = seriesIndex,
                description = annotation.take(8000),
                publisher = publisher,
                language = language,
                year = year,
            ),
            coverBytes,
        )
    }

    fun metadataFromFilename(name: String): BookMetadata {
        val stem = name
            .replace(Regex("(?i)\\.fb2\\.zip$"), "")
            .replace(Regex("(?i)\\.(epub|fb2|txt|pdf)$"), "")
            .replace('_', ' ')
            .trim()
        val parts = stem.split(Regex("\\s+[-–—]\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size >= 3) {
            val seriesRaw = parts[1]
            val indexMatch = Regex("(?:^|\\s)(\\d+(?:[.,]\\d+)?)$").find(seriesRaw)
            val index = indexMatch?.groupValues?.getOrNull(1)?.replace(',', '.')?.toFloatOrNull() ?: 0f
            val series = indexMatch?.let { seriesRaw.removeRange(it.range).trim() }.orEmpty().ifBlank { seriesRaw }
            return BookMetadata(title = parts.drop(2).joinToString(" — "), author = parts[0], series = series, seriesIndex = index)
        }
        if (parts.size == 2) return BookMetadata(title = parts[1], author = parts[0])
        return BookMetadata(title = stem.ifBlank { "Книга" })
    }

    private fun openFb2(file: File): InputStream {
        if (!file.name.lowercase().endsWith(".zip")) return file.inputStream().buffered()
        val zip = ZipFile(file)
        val entry = zip.entries().asSequence().firstOrNull { !it.isDirectory && it.name.lowercase().endsWith(".fb2") }
            ?: run { zip.close(); error("FB2.ZIP: файл .fb2 не найден") }
        val stream = zip.getInputStream(entry)
        return object : InputStream() {
            override fun read(): Int = stream.read()
            override fun read(b: ByteArray, off: Int, len: Int): Int = stream.read(b, off, len)
            override fun close() { stream.close(); zip.close() }
        }
    }

    private fun parseXml(input: InputStream): Document {
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "") }
            runCatching { setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "") }
        }
        return factory.newDocumentBuilder().parse(input)
    }

    private fun elements(document: Document, localName: String): List<Element> = elements(document.documentElement, localName)

    private fun elements(root: Element, localName: String): List<Element> {
        val list = root.getElementsByTagNameNS("*", localName)
        if (list.length > 0) return List(list.length) { list.item(it) as Element }
        val fallback = root.getElementsByTagName(localName)
        return List(fallback.length) { fallback.item(it) as Element }
    }

    private fun firstText(document: Document, localName: String): String =
        elements(document, localName).firstOrNull()?.textContent?.normalizeSpace().orEmpty()

    private fun firstText(root: Element, localName: String): String =
        elements(root, localName).firstOrNull()?.textContent?.normalizeSpace().orEmpty()

    private fun fb2Author(titleInfo: Element): String {
        val author = elements(titleInfo, "author").firstOrNull() ?: return ""
        val first = firstText(author, "first-name")
        val middle = firstText(author, "middle-name")
        val last = firstText(author, "last-name")
        val nick = firstText(author, "nickname")
        return listOf(first, middle, last).filter { it.isNotBlank() }.joinToString(" ").ifBlank { nick }
    }

    private fun hrefAttribute(element: Element): String {
        element.getAttribute("href").takeIf { it.isNotBlank() }?.let { return it }
        val attrs = element.attributes
        for (index in 0 until attrs.length) {
            val attr = attrs.item(index)
            val local = attr.localName?.lowercase().orEmpty()
            val raw = attr.nodeName.substringAfter(':').lowercase()
            if ((local == "href" || raw == "href") && attr.nodeValue.isNotBlank()) return attr.nodeValue
        }
        return ""
    }

    private fun normalizeZipPath(baseDir: String, href: String): String {
        val decoded = runCatching { URLDecoder.decode(href, StandardCharsets.UTF_8.name()) }.getOrDefault(href)
        val stack = ArrayDeque<String>()
        (baseDir.split('/') + decoded.split('/')).forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeLast()
                else -> stack.addLast(part)
            }
        }
        return stack.joinToString("/")
    }

    private fun InputStream.readBytesLimited(limit: Int): ByteArray? {
        val buffer = ByteArray(16 * 1024)
        val out = java.io.ByteArrayOutputStream(minOf(limit, 256 * 1024))
        var total = 0
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > limit) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun String.normalizeSpace(): String = trim().replace(Regex("\\s+"), " ")

    private val RASTER_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp")
    private const val MAX_COVER_BYTES = 12L * 1024L * 1024L
    private const val MAX_COVER_MARKUP_BYTES = 2 * 1024 * 1024
    private const val MAX_COVER_BASE64_CHARS = 17 * 1024 * 1024
    private const val MAX_FB2_IMPORT_BYTES = 32 * 1024 * 1024
}
