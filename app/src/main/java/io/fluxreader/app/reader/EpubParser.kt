package io.fluxreader.app.reader

import io.fluxreader.app.data.BookMetadata
import io.fluxreader.app.data.BookOutline
import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ChapterRef
import io.fluxreader.app.data.ReaderBlock
import io.fluxreader.app.data.ReaderLink
import io.fluxreader.app.data.TextRole
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.File
import java.io.InputStream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

object EpubParser {
    private data class ManifestItem(val id: String, val href: String, val mediaType: String, val properties: String)

    data class PackageInfo(
        val metadata: BookMetadata,
        val chapters: List<ChapterRef>,
    )

    fun outline(file: File): BookOutline = ZipFile(file).use { zip ->
        val packageInfo = parsePackage(zip)
        BookOutline(packageInfo.metadata.title, packageInfo.metadata.author, packageInfo.chapters)
    }

    fun loadChapter(file: File, ref: ChapterRef): ChapterContent = ZipFile(file).use { zip ->
        val entry = zip.getEntry(ref.sourceRef) ?: error("EPUB: глава не найдена: ${ref.sourceRef}")
        val document = zip.getInputStream(entry).use(::parseXml)
        val body = findFirst(document.documentElement, "body") ?: document.documentElement
        val blocks = mutableListOf<ReaderBlock>()
        walkBlocks(
            node = body,
            currentPath = ref.sourceRef,
            baseDir = ref.sourceRef.substringBeforeLast('/', ""),
            inheritedAnchors = emptyList(),
            out = blocks,
        )
        val cleaned = blocks.filterNot { block -> block is ReaderBlock.Text && block.text.isBlank() }
        val normalized = normalizeChapterHeadingRoles(cleaned, ref.title)
        ChapterContent(
            index = ref.index,
            title = normalized.filterIsInstance<ReaderBlock.Text>()
                .firstOrNull { it.role == TextRole.TITLE }
                ?.text
                ?.take(180)
                ?: normalized.filterIsInstance<ReaderBlock.Text>()
                    .firstOrNull { it.role == TextRole.HEADING }
                    ?.text
                    ?.take(180)
                ?: ref.title,
            blocks = if (normalized.isNotEmpty()) normalized else listOf(
                ReaderBlock.Text(document.documentElement.textContent.normalizeSpace(), TextRole.PARAGRAPH)
            ),
        )
    }


    /**
     * Some EPUBs encode visible chapter headings as plain <p> elements and rely entirely on CSS.
     * Flux Reader intentionally does not execute publisher CSS, so recover the common semantic
     * pattern at the very beginning of a spine chapter: "Глава N" followed by a short title.
     */
    internal fun normalizeChapterHeadingRoles(blocks: List<ReaderBlock>, navigationTitle: String): List<ReaderBlock> {
        if (blocks.isEmpty()) return blocks
        val result = blocks.toMutableList()
        val textIndexes = result.indices.filter { result[it] is ReaderBlock.Text }.take(4)
        if (textIndexes.isEmpty()) return blocks

        fun textAt(index: Int): ReaderBlock.Text = result[index] as ReaderBlock.Text
        fun normalize(value: String): String = value.trim().replace(Regex("""\s+"""), " ")
        fun isChapterLabel(value: String): Boolean = Regex(
            """(?iu)^\s*(?:глава|chapter|часть|part)\s+(?:[0-9]+|[ivxlcdm]+|[а-яё]+)\s*[.:—-]?\s*$"""
        ).matches(value)
        fun looksLikeShortTitle(value: String): Boolean {
            val text = normalize(value)
            if (text.length !in 2..140) return false
            val letters = text.filter { it.isLetter() }
            if (letters.length < 3) return false
            val upperRatio = letters.count { it.isUpperCase() }.toFloat() / letters.length.toFloat()
            val nav = normalize(navigationTitle)
            return upperRatio >= 0.72f || (nav.isNotBlank() && text.equals(nav, ignoreCase = true))
        }

        val firstIndex = textIndexes[0]
        val first = textAt(firstIndex)
        var hasChapterLabel = first.role == TextRole.HEADING || isChapterLabel(first.text)
        if (first.role == TextRole.PARAGRAPH && isChapterLabel(first.text)) {
            result[firstIndex] = first.copy(role = TextRole.HEADING)
            hasChapterLabel = true
        }

        val secondIndex = textIndexes.getOrNull(1)
        if (secondIndex != null) {
            val second = textAt(secondIndex)
            if (second.role == TextRole.PARAGRAPH && hasChapterLabel && looksLikeShortTitle(second.text)) {
                result[secondIndex] = second.copy(role = TextRole.TITLE)
            } else if (second.role == TextRole.PARAGRAPH && looksLikeShortTitle(second.text)) {
                val nav = normalize(navigationTitle)
                if (nav.isNotBlank() && normalize(second.text).equals(nav, ignoreCase = true)) {
                    result[secondIndex] = second.copy(role = TextRole.TITLE)
                }
            }
        }
        return result
    }

    fun loadImage(file: File, ref: String): ByteArray? = runCatching {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry(ref) ?: return@use null
            if (entry.size > 40L * 1024L * 1024L) return@use null
            zip.getInputStream(entry).use { it.readBytes() }
        }
    }.getOrNull()

    private fun parsePackage(zip: ZipFile): PackageInfo {
        val containerEntry = zip.getEntry("META-INF/container.xml") ?: error("EPUB: отсутствует container.xml")
        val container = zip.getInputStream(containerEntry).use(::parseXml)
        val rootfile = elements(container.documentElement, "rootfile").firstOrNull()
            ?: error("EPUB: rootfile не найден")
        val opfPath = rootfile.getAttribute("full-path").trim()
        require(opfPath.isNotEmpty()) { "EPUB: пустой OPF path" }
        val opfEntry = zip.getEntry(opfPath) ?: error("EPUB: OPF не найден")
        val opf = zip.getInputStream(opfEntry).use(::parseXml)
        val baseDir = opfPath.substringBeforeLast('/', "")

        val title = elements(opf.documentElement, "title").firstOrNull()?.textContent?.normalizeSpace().orEmpty()
        val author = elements(opf.documentElement, "creator").firstOrNull()?.textContent?.normalizeSpace().orEmpty()

        val manifest = elements(opf.documentElement, "item")
            .mapNotNull { item ->
                val id = item.getAttribute("id").trim()
                val href = item.getAttribute("href").trim()
                if (id.isBlank() || href.isBlank()) null else ManifestItem(
                    id = id,
                    href = href,
                    mediaType = item.getAttribute("media-type").trim(),
                    properties = item.getAttribute("properties").trim(),
                )
            }
            .associateBy { it.id }
        val spine = findFirst(opf.documentElement, "spine")
        val spineIds = elements(opf.documentElement, "itemref")
            .map { it.getAttribute("idref") }
            .filter { it.isNotBlank() }

        val titleMap = readNavigationTitles(
            zip = zip,
            baseDir = baseDir,
            manifest = manifest,
            ncxId = spine?.getAttribute("toc").orEmpty(),
        )

        val refs = spineIds.mapNotNull { id -> manifest[id]?.href }
            .map { href -> normalizeZipPath(baseDir, href.substringBefore('#')) }
            .filter { it.isNotBlank() }
            .distinct()
            .mapIndexed { index, path ->
                ChapterRef(index, titleMap[path].orEmpty().ifBlank { "Глава ${index + 1}" }, path)
            }

        require(refs.isNotEmpty()) { "EPUB: spine пуст" }
        return PackageInfo(
            metadata = BookMetadata(title = title.ifBlank { fileTitle(zip) }, author = author),
            chapters = refs,
        )
    }

    private fun readNavigationTitles(
        zip: ZipFile,
        baseDir: String,
        manifest: Map<String, ManifestItem>,
        ncxId: String,
    ): Map<String, String> {
        val titles = linkedMapOf<String, String>()

        val navItem = manifest.values.firstOrNull { item ->
            item.properties.split(Regex("\\s+")).any { it.equals("nav", ignoreCase = true) }
        }
        if (navItem != null) {
            val navPath = normalizeZipPath(baseDir, navItem.href.substringBefore('#'))
            runCatching {
                val entry = zip.getEntry(navPath) ?: return@runCatching
                val document = zip.getInputStream(entry).use(::parseXml)
                val navBase = navPath.substringBeforeLast('/', "")
                elements(document.documentElement, "a").forEach { anchor ->
                    val href = anchor.getAttribute("href").substringBefore('#').trim()
                    val label = anchor.textContent.normalizeSpace()
                    if (href.isNotBlank() && label.isNotBlank()) {
                        titles.putIfAbsent(normalizeZipPath(navBase, href), label.take(180))
                    }
                }
            }
        }

        if (titles.isEmpty()) {
            val ncxItem = manifest[ncxId].takeIf { ncxId.isNotBlank() }
                ?: manifest.values.firstOrNull { it.mediaType.equals("application/x-dtbncx+xml", ignoreCase = true) }
            if (ncxItem != null) {
                val ncxPath = normalizeZipPath(baseDir, ncxItem.href.substringBefore('#'))
                runCatching {
                    val entry = zip.getEntry(ncxPath) ?: return@runCatching
                    val document = zip.getInputStream(entry).use(::parseXml)
                    val navBase = ncxPath.substringBeforeLast('/', "")
                    elements(document.documentElement, "navPoint").forEach { point ->
                        val content = elements(point, "content").firstOrNull()
                        val label = elements(point, "text").firstOrNull()?.textContent?.normalizeSpace().orEmpty()
                        val src = content?.getAttribute("src").orEmpty().substringBefore('#').trim()
                        if (src.isNotBlank() && label.isNotBlank()) {
                            titles.putIfAbsent(normalizeZipPath(navBase, src), label.take(180))
                        }
                    }
                }
            }
        }
        return titles
    }

    private fun fileTitle(zip: ZipFile): String = zip.name.substringAfterLast('/').substringBeforeLast('.').ifBlank { "EPUB" }

    private data class RichText(val text: String, val links: List<ReaderLink>)

    private data class LinkTarget(val target: String, val external: Boolean)

    private fun walkBlocks(
        node: Node,
        currentPath: String,
        baseDir: String,
        inheritedAnchors: List<String>,
        out: MutableList<ReaderBlock>,
        forcedRole: TextRole? = null,
        listDepth: Int = 0,
    ) {
        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType != Node.ELEMENT_NODE) continue
            val element = child as Element
            val tag = localName(element)
            val ownAnchors = ownAnchorIds(element)
            val nestedAnchors = (inheritedAnchors + ownAnchors).distinct()
            when (tag) {
                "h1", "title" -> {
                    emitTextBlock(element, TextRole.TITLE, currentPath, baseDir, nestedAnchors, out)
                    appendImages(element, baseDir, nestedAnchors, out)
                }
                "h2", "h3", "h4", "h5", "h6" -> {
                    emitTextBlock(element, TextRole.HEADING, currentPath, baseDir, nestedAnchors, out)
                    appendImages(element, baseDir, nestedAnchors, out)
                }
                "p", "dt", "dd", "figcaption" -> {
                    emitTextBlock(element, forcedRole ?: TextRole.PARAGRAPH, currentPath, baseDir, nestedAnchors, out)
                    appendImages(element, baseDir, nestedAnchors, out)
                }
                "ul", "ol" -> emitList(
                    list = element,
                    currentPath = currentPath,
                    baseDir = baseDir,
                    inheritedAnchors = nestedAnchors,
                    out = out,
                    depth = listDepth,
                    ordered = tag == "ol",
                )
                "li" -> emitListItem(
                    item = element,
                    currentPath = currentPath,
                    baseDir = baseDir,
                    inheritedAnchors = nestedAnchors,
                    out = out,
                    depth = listDepth,
                    marker = bulletForDepth(listDepth),
                )
                "blockquote" -> {
                    val direct = richTextDirect(element, currentPath, baseDir)
                    if (direct.text.isNotBlank()) {
                        out += ReaderBlock.Text(
                            text = direct.text,
                            role = TextRole.QUOTE,
                            anchors = nestedAnchors,
                            links = direct.links,
                        )
                    }
                    walkBlocks(
                        node = element,
                        currentPath = currentPath,
                        baseDir = baseDir,
                        inheritedAnchors = nestedAnchors,
                        out = out,
                        forcedRole = TextRole.QUOTE,
                        listDepth = listDepth,
                    )
                    appendDirectImages(element, baseDir, nestedAnchors, out)
                }
                "table" -> emitTable(element, currentPath, baseDir, nestedAnchors, out)
                "pre", "code" -> {
                    val text = element.textContent.trim()
                    if (text.isNotBlank()) {
                        out += ReaderBlock.Text(
                            text = text,
                            role = TextRole.CODE,
                            anchors = (nestedAnchors + descendantAnchorIds(element)).distinct(),
                        )
                    }
                }
                "img", "image" -> imageRef(element, baseDir, nestedAnchors)?.let(out::add)
                "br", "hr" -> Unit
                else -> {
                    val semanticRole = semanticContainerRole(element) ?: forcedRole
                    emitDirectInlineBlock(
                        element = element,
                        currentPath = currentPath,
                        baseDir = baseDir,
                        inheritedAnchors = nestedAnchors,
                        out = out,
                        role = semanticRole ?: TextRole.PARAGRAPH,
                    )
                    walkBlocks(
                        node = element,
                        currentPath = currentPath,
                        baseDir = baseDir,
                        inheritedAnchors = nestedAnchors,
                        out = out,
                        forcedRole = semanticRole,
                        listDepth = listDepth,
                    )
                }
            }
        }
    }

    private fun emitList(
        list: Element,
        currentPath: String,
        baseDir: String,
        inheritedAnchors: List<String>,
        out: MutableList<ReaderBlock>,
        depth: Int,
        ordered: Boolean,
    ) {
        var ordinal = 0
        val children = list.childNodes
        for (index in 0 until children.length) {
            val item = children.item(index) as? Element ?: continue
            if (localName(item) != "li") continue
            ordinal += 1
            emitListItem(
                item = item,
                currentPath = currentPath,
                baseDir = baseDir,
                inheritedAnchors = (inheritedAnchors + ownAnchorIds(item)).distinct(),
                out = out,
                depth = depth,
                marker = if (ordered) "$ordinal." else bulletForDepth(depth),
            )
        }
    }

    private fun emitListItem(
        item: Element,
        currentPath: String,
        baseDir: String,
        inheritedAnchors: List<String>,
        out: MutableList<ReaderBlock>,
        depth: Int,
        marker: String,
    ) {
        var rich = richTextDirect(item, currentPath, baseDir)
        var labelParagraph: Element? = null
        // Some EPUB generators wrap the actual item label in a paragraph. Preserve it as the
        // list item rather than dropping the label or flattening the entire nested list.
        if (rich.text.isBlank()) {
            labelParagraph = directChildElements(item).firstOrNull { localName(it) == "p" }
            if (labelParagraph != null) rich = richText(labelParagraph, currentPath, baseDir)
        }
        if (rich.text.isNotBlank()) {
            out += ReaderBlock.Text(
                text = rich.text,
                role = TextRole.LIST_ITEM,
                anchors = (inheritedAnchors + directInlineAnchorIds(item)).distinct(),
                links = rich.links,
                listDepth = depth.coerceAtLeast(0),
                listMarker = marker,
            )
        }
        appendDirectImages(item, baseDir, inheritedAnchors, out)
        directChildElements(item).forEach { child ->
            when (localName(child)) {
                "ul" -> emitList(child, currentPath, baseDir, inheritedAnchors, out, depth + 1, ordered = false)
                "ol" -> emitList(child, currentPath, baseDir, inheritedAnchors, out, depth + 1, ordered = true)
                // A paragraph used as the label was already represented above. Additional block
                // paragraphs remain separate so multi-paragraph list items keep their structure.
                "p" -> if (child !== labelParagraph) {
                    emitTextBlock(child, TextRole.PARAGRAPH, currentPath, baseDir, inheritedAnchors, out)
                }
            }
        }
    }

    private fun emitDirectInlineBlock(
        element: Element,
        currentPath: String,
        baseDir: String,
        inheritedAnchors: List<String>,
        out: MutableList<ReaderBlock>,
        role: TextRole = TextRole.PARAGRAPH,
    ) {
        if (!isStructuralContainer(localName(element))) return
        val rich = richTextDirect(element, currentPath, baseDir)
        if (rich.text.isBlank()) return
        out += ReaderBlock.Text(
            text = rich.text,
            role = role,
            anchors = (inheritedAnchors + directInlineAnchorIds(element)).distinct(),
            links = rich.links,
        )
    }

    private fun bulletForDepth(depth: Int): String = when (depth % 3) {
        0 -> "•"
        1 -> "◦"
        else -> "▪"
    }

    private fun isStructuralContainer(tag: String): Boolean = when (tag) {
        "body", "div", "section", "article", "aside", "main", "nav", "header", "footer",
        "figure", "address", "details", "summary" -> true
        else -> false
    }

    private fun isNestedBlockTag(tag: String): Boolean = when (tag) {
        "h1", "h2", "h3", "h4", "h5", "h6", "p", "div", "section", "article", "aside",
        "main", "nav", "header", "footer", "figure", "figcaption", "blockquote", "pre", "table",
        "ul", "ol", "li", "dl", "dt", "dd" -> true
        else -> false
    }

    private fun directChildElements(element: Element): List<Element> = buildList {
        val children = element.childNodes
        for (index in 0 until children.length) (children.item(index) as? Element)?.let(::add)
    }

    private fun directInlineAnchorIds(element: Element): List<String> = buildList {
        directChildElements(element).forEach { child ->
            if (!isNestedBlockTag(localName(child))) {
                ownAnchorIds(child).forEach { if (it !in this) add(it) }
                descendantAnchorIds(child).forEach { if (it !in this) add(it) }
            }
        }
    }

    private fun appendDirectImages(
        element: Element,
        baseDir: String,
        inheritedAnchors: List<String>,
        out: MutableList<ReaderBlock>,
    ) {
        directChildElements(element).forEach { child ->
            if (localName(child) == "img" || localName(child) == "image") {
                imageRef(child, baseDir, (inheritedAnchors + ownAnchorIds(child)).distinct())?.let(out::add)
            }
        }
    }

    private fun emitTable(
        table: Element,
        currentPath: String,
        baseDir: String,
        inheritedAnchors: List<String>,
        out: MutableList<ReaderBlock>,
    ) {
        val rows = mutableListOf<Element>()

        fun collectRows(parent: Element) {
            val children = parent.childNodes
            for (index in 0 until children.length) {
                val child = children.item(index) as? Element ?: continue
                when (localName(child)) {
                    "tr" -> rows += child
                    "thead", "tbody", "tfoot" -> collectRows(child)
                }
            }
        }

        collectRows(table)
        rows.forEach { row ->
            val rowText = StringBuilder()
            val rowLinks = mutableListOf<ReaderLink>()
            val rowAnchors = linkedSetOf<String>()
            rowAnchors += inheritedAnchors
            rowAnchors += ownAnchorIds(row)

            val cells = row.childNodes
            for (index in 0 until cells.length) {
                val cell = cells.item(index) as? Element ?: continue
                if (localName(cell) != "td" && localName(cell) != "th") continue
                val rich = richText(cell, currentPath, baseDir)
                if (rich.text.isBlank()) continue
                if (rowText.isNotEmpty()) rowText.append(" │ ")
                val offset = rowText.length
                rowText.append(rich.text)
                rich.links.forEach { link ->
                    rowLinks += link.copy(start = link.start + offset, end = link.end + offset)
                }
                rowAnchors += ownAnchorIds(cell)
                rowAnchors += descendantAnchorIds(cell)
            }

            if (rowText.isNotBlank()) {
                out += ReaderBlock.Text(
                    text = rowText.toString(),
                    role = TextRole.TABLE,
                    anchors = rowAnchors.toList(),
                    links = rowLinks,
                )
            }
        }
        appendImages(table, baseDir, inheritedAnchors, out)
    }

    private fun semanticContainerRole(element: Element): TextRole? {
        val semantics = sequenceOf(
            element.getAttribute("class"),
            element.getAttribute("type"),
            element.getAttribute("epub:type"),
        ).joinToString(" ").lowercase()
        return when {
            Regex("(^|[\\s_-])(poem|poetry|verse|stanza)([\\s_-]|$)").containsMatchIn(semantics) -> TextRole.POEM
            else -> null
        }
    }

    private fun emitTextBlock(
        element: Element,
        role: TextRole,
        currentPath: String,
        baseDir: String,
        inheritedAnchors: List<String>,
        out: MutableList<ReaderBlock>,
    ) {
        val rich = richText(element, currentPath, baseDir)
        if (rich.text.isBlank()) return
        out += ReaderBlock.Text(
            text = rich.text,
            role = role,
            anchors = (inheritedAnchors + descendantAnchorIds(element)).distinct(),
            links = rich.links,
        )
    }

    private fun richText(element: Element, currentPath: String, baseDir: String): RichText {
        val text = StringBuilder()
        val links = mutableListOf<ReaderLink>()
        var pendingSpace = false
        var activeTarget: String? = null
        var activeExternal = false
        var activeStart = -1

        fun closeActiveLink() {
            val target = activeTarget
            if (target != null && activeStart >= 0 && activeStart < text.length) {
                links += ReaderLink(activeStart, text.length, target, activeExternal)
            }
            activeTarget = null
            activeExternal = false
            activeStart = -1
        }

        fun switchLink(link: LinkTarget?) {
            if (activeTarget == link?.target && activeExternal == (link?.external ?: false)) return
            closeActiveLink()
            if (link != null) {
                activeTarget = link.target
                activeExternal = link.external
                activeStart = text.length
            }
        }

        fun appendChar(char: Char, link: LinkTarget?) {
            switchLink(link)
            text.append(char)
        }

        fun appendText(raw: String, link: LinkTarget?) {
            raw.forEach { char ->
                if (char.isWhitespace()) {
                    if (text.isNotEmpty()) pendingSpace = true
                } else {
                    if (pendingSpace && text.isNotEmpty() && text.last() != ' ') {
                        val sameLink = activeTarget == link?.target && activeExternal == (link?.external ?: false)
                        appendChar(' ', if (sameLink) link else null)
                    }
                    pendingSpace = false
                    appendChar(char, link)
                }
            }
        }

        fun visit(node: Node, inheritedLink: LinkTarget?) {
            when (node.nodeType) {
                Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> appendText(node.nodeValue.orEmpty(), inheritedLink)
                Node.ELEMENT_NODE -> {
                    val child = node as Element
                    val tag = localName(child)
                    if (tag == "img" || tag == "image") return
                    if (tag == "br") {
                        pendingSpace = true
                        return
                    }
                    val link = if (tag == "a") {
                        normalizeLinkTarget(
                            currentPath = currentPath,
                            baseDir = baseDir,
                            raw = child.getAttribute("href").ifBlank { child.getAttribute("xlink:href") },
                        ) ?: inheritedLink
                    } else inheritedLink
                    val nodes = child.childNodes
                    for (index in 0 until nodes.length) visit(nodes.item(index), link)
                }
            }
        }

        val nodes = element.childNodes
        for (index in 0 until nodes.length) visit(nodes.item(index), null)
        closeActiveLink()
        return RichText(text.toString().trim(), links.filter { it.start < it.end && it.end <= text.length })
    }

    private fun richTextDirect(element: Element, currentPath: String, baseDir: String): RichText {
        val text = StringBuilder()
        val links = mutableListOf<ReaderLink>()
        var pendingSpace = false
        var activeTarget: String? = null
        var activeExternal = false
        var activeStart = -1

        fun closeActiveLink() {
            val target = activeTarget
            if (target != null && activeStart >= 0 && activeStart < text.length) {
                links += ReaderLink(activeStart, text.length, target, activeExternal)
            }
            activeTarget = null
            activeExternal = false
            activeStart = -1
        }

        fun switchLink(link: LinkTarget?) {
            if (activeTarget == link?.target && activeExternal == (link?.external ?: false)) return
            closeActiveLink()
            if (link != null) {
                activeTarget = link.target
                activeExternal = link.external
                activeStart = text.length
            }
        }

        fun appendText(raw: String, link: LinkTarget?) {
            raw.forEach { char ->
                if (char.isWhitespace()) {
                    if (text.isNotEmpty()) pendingSpace = true
                } else {
                    if (pendingSpace && text.isNotEmpty() && text.last() != ' ') {
                        val sameLink = activeTarget == link?.target && activeExternal == (link?.external ?: false)
                        switchLink(if (sameLink) link else null)
                        text.append(' ')
                    }
                    pendingSpace = false
                    switchLink(link)
                    text.append(char)
                }
            }
        }

        fun visit(node: Node, inheritedLink: LinkTarget?) {
            when (node.nodeType) {
                Node.TEXT_NODE, Node.CDATA_SECTION_NODE -> appendText(node.nodeValue.orEmpty(), inheritedLink)
                Node.ELEMENT_NODE -> {
                    val child = node as Element
                    val tag = localName(child)
                    if (tag == "img" || tag == "image" || isNestedBlockTag(tag)) return
                    if (tag == "br") {
                        pendingSpace = true
                        return
                    }
                    val link = if (tag == "a") {
                        normalizeLinkTarget(
                            currentPath = currentPath,
                            baseDir = baseDir,
                            raw = child.getAttribute("href").ifBlank { child.getAttribute("xlink:href") },
                        ) ?: inheritedLink
                    } else inheritedLink
                    val children = child.childNodes
                    for (index in 0 until children.length) visit(children.item(index), link)
                }
            }
        }

        val children = element.childNodes
        for (index in 0 until children.length) visit(children.item(index), null)
        closeActiveLink()
        return RichText(text.toString().trim(), links.filter { it.start < it.end && it.end <= text.length })
    }

    private fun normalizeLinkTarget(currentPath: String, baseDir: String, raw: String): LinkTarget? {
        val href = raw.trim()
        if (href.isBlank()) return null
        if (href.startsWith("javascript:", ignoreCase = true) || href.startsWith("data:", ignoreCase = true)) return null
        val external = Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(href)
        if (external) return LinkTarget(href, true)

        val pathPart = href.substringBefore('#').trim()
        val fragment = href.substringAfter('#', "").trim()
        val targetPath = if (pathPart.isBlank()) currentPath else normalizeZipPath(baseDir, pathPart)
        if (targetPath.isBlank()) return null
        val target = if (fragment.isBlank()) targetPath else "$targetPath#$fragment"
        return LinkTarget(target, false)
    }

    private fun appendImages(
        element: Element,
        baseDir: String,
        inheritedAnchors: List<String>,
        out: MutableList<ReaderBlock>,
    ) {
        val nodes = element.getElementsByTagName("*")
        for (i in 0 until nodes.length) {
            val node = nodes.item(i) as? Element ?: continue
            if (localName(node) == "img" || localName(node) == "image") {
                imageRef(node, baseDir, (inheritedAnchors + ownAnchorIds(node)).distinct())?.let(out::add)
            }
        }
    }

    private fun imageRef(element: Element, baseDir: String, anchors: List<String> = emptyList()): ReaderBlock.Image? {
        val raw = sequenceOf(
            element.getAttribute("src"),
            element.getAttribute("href"),
            element.getAttribute("xlink:href"),
        ).firstOrNull { it.isNotBlank() } ?: return null
        val path = normalizeZipPath(baseDir, raw.substringBefore('#'))
        return ReaderBlock.Image(path, element.getAttribute("alt"), anchors)
    }

    private fun ownAnchorIds(element: Element): List<String> = buildList {
        sequenceOf(
            element.getAttribute("id"),
            element.getAttribute("xml:id"),
            if (localName(element) == "a") element.getAttribute("name") else "",
        ).map { it.trim() }.filter { it.isNotBlank() }.forEach { if (it !in this) add(it) }
    }

    private fun descendantAnchorIds(element: Element): List<String> = buildList {
        val nodes = element.getElementsByTagName("*")
        for (i in 0 until nodes.length) {
            val child = nodes.item(i) as? Element ?: continue
            ownAnchorIds(child).forEach { if (it !in this) add(it) }
        }
    }

    private fun normalizeZipPath(baseDir: String, raw: String): String {
        val decoded = runCatching { URLDecoder.decode(raw, StandardCharsets.UTF_8.name()) }.getOrDefault(raw)
        val stack = ArrayDeque<String>()
        val combined = if (baseDir.isBlank()) decoded else "$baseDir/$decoded"
        combined.replace('\\', '/').split('/').forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeLast()
                else -> stack.addLast(part)
            }
        }
        return stack.joinToString("/")
    }

    private fun parseXml(input: InputStream) = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
    }.newDocumentBuilder().parse(input)

    private fun elements(root: Element, name: String): List<Element> {
        val list = root.getElementsByTagNameNS("*", name)
        if (list.length > 0) return buildList {
            for (i in 0 until list.length) (list.item(i) as? Element)?.let(::add)
        }
        val fallback = root.getElementsByTagName(name)
        return buildList { for (i in 0 until fallback.length) (fallback.item(i) as? Element)?.let(::add) }
    }

    private fun findFirst(root: Element, name: String): Element? = elements(root, name).firstOrNull()

    private fun localName(element: Element): String = (element.localName ?: element.tagName.substringAfter(':')).lowercase()

    private fun String.normalizeSpace(): String = replace(Regex("\\s+"), " ").trim()
}
