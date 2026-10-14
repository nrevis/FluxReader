package io.fluxreader.app.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ReaderBlock
import io.fluxreader.app.data.ReaderLink
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.ReaderTextAlignment
import io.fluxreader.app.data.TextRole
import kotlinx.coroutines.yield
import kotlin.math.ceil

internal fun buildReaderTextStyle(
    block: ReaderBlock.Text,
    settings: ReaderSettings,
    typography: Typography,
    family: FontFamily,
    density: Density,
    continuation: Boolean,
): TextStyle {
    val indent = with(density) { settings.firstLineIndentDp.dp.toSp() }
    val bodyAlignment = when (settings.textAlignment) {
        ReaderTextAlignment.LEFT -> TextAlign.Start
        ReaderTextAlignment.JUSTIFY -> TextAlign.Justify
    }
    val bodySize = settings.fontSizeSp
    val roleFontSize = when (block.role) {
        TextRole.TITLE -> bodySize * settings.chapterTitleScale
        TextRole.HEADING -> bodySize * settings.chapterHeadingScale
        TextRole.TABLE, TextRole.CODE -> (bodySize - 1f).coerceAtLeast(12f)
        else -> bodySize
    }
    val roleLineHeight = when (block.role) {
        TextRole.TITLE -> roleFontSize * 1.20f
        TextRole.HEADING -> roleFontSize * 1.24f
        else -> bodySize * settings.lineHeightMultiplier
    }
    val baseStyle = when (block.role) {
        TextRole.TITLE -> typography.headlineMedium.copy(
            fontSize = roleFontSize.sp,
            fontWeight = FontWeight.ExtraBold,
            fontSynthesis = FontSynthesis.Weight,
            letterSpacing = 0.2.sp,
        )
        TextRole.HEADING -> typography.headlineSmall.copy(
            fontSize = roleFontSize.sp,
            fontWeight = FontWeight.Bold,
            fontSynthesis = FontSynthesis.Weight,
            letterSpacing = 0.1.sp,
        )
        TextRole.LIST_ITEM -> typography.bodyLarge.copy(fontSize = roleFontSize.sp)
        TextRole.QUOTE -> typography.bodyLarge.copy(fontSize = roleFontSize.sp, fontStyle = FontStyle.Italic)
        TextRole.POEM -> typography.bodyLarge.copy(fontSize = roleFontSize.sp, fontStyle = FontStyle.Italic)
        TextRole.TABLE -> typography.bodyMedium.copy(fontSize = roleFontSize.sp, fontFamily = FontFamily.Monospace)
        TextRole.CODE -> typography.bodyLarge.copy(fontSize = roleFontSize.sp, fontFamily = FontFamily.Monospace)
        TextRole.PARAGRAPH -> typography.bodyLarge.copy(fontSize = roleFontSize.sp)
    }
    val resolvedAlignment = when (block.role) {
        TextRole.TITLE, TextRole.HEADING -> if (settings.centerChapterHeadings) TextAlign.Center else TextAlign.Start
        TextRole.CODE, TextRole.TABLE, TextRole.POEM, TextRole.LIST_ITEM -> TextAlign.Start
        else -> bodyAlignment
    }
    return baseStyle.copy(
        lineHeight = roleLineHeight.sp,
        fontFamily = if (block.role == TextRole.CODE || block.role == TextRole.TABLE) FontFamily.Monospace else family,
        textAlign = resolvedAlignment,
        textIndent = if (
            block.role == TextRole.PARAGRAPH &&
            settings.firstLineIndentDp > 0f &&
            !continuation
        ) {
            TextIndent(firstLine = indent)
        } else {
            TextIndent(firstLine = 0.sp)
        },
        lineBreak = when (block.role) {
            TextRole.TITLE, TextRole.HEADING -> LineBreak.Heading
            else -> LineBreak.Paragraph
        },
        hyphens = if (settings.hyphenationEnabled) {
            when (block.role) {
                TextRole.PARAGRAPH, TextRole.LIST_ITEM, TextRole.QUOTE -> Hyphens.Auto
                else -> Hyphens.None
            }
        } else {
            Hyphens.None
        },
    )
}

internal fun readerTextWidthPx(
    block: ReaderBlock.Text,
    settings: ReaderSettings,
    density: Density,
    viewportWidthPx: Int,
): Int {
    val horizontal = with(density) { settings.horizontalPaddingDp.dp.roundToPx() }
    var width = viewportWidthPx - horizontal * 2
    if (block.role == TextRole.LIST_ITEM) {
        val depth = block.listDepth.coerceIn(0, 8)
        val depthPx = with(density) { (depth * 18f).dp.roundToPx() }
        val markerPx = with(density) { 24.dp.roundToPx() }
        width -= depthPx + markerPx
    }
    return width.coerceAtLeast(with(density) { 56.dp.roundToPx() })
}

internal suspend fun prepareReaderChapterPagination(
    chapterIndex: Int,
    chapter: ChapterContent,
    settings: ReaderSettings,
    textMeasurer: TextMeasurer,
    typography: Typography,
    family: FontFamily,
    density: Density,
    viewportWidthPx: Int,
    viewportHeightPx: Int,
    layoutKey: String,
): PreparedReaderPagination {
    val topPaddingPx = with(density) { settings.topPaddingDp.dp.roundToPx() }
    val bottomPaddingPx = with(density) { settings.bottomPaddingDp.dp.roundToPx() }
    val paragraphSpacingPx = with(density) { settings.paragraphSpacingDp.dp.roundToPx() }
    val nominalLineHeightPx = with(density) {
        (settings.fontSizeSp * settings.lineHeightMultiplier).sp.roundToPx().coerceAtLeast(1)
    }
    // Compose can place glyph descenders a few physical pixels below a nominal line box, and
    // OEM font metrics vary. Reserve a fraction of a real line instead of a fixed 6 dp so the
    // last visible row is never clipped by the page viewport / gesture inset.
    val bottomSafetyPx = ReaderPageGridCalculator.bottomSafetyPx(
        nominalLineHeightPx = nominalLineHeightPx,
        minimumSafetyPx = with(density) { 6.dp.roundToPx() },
    )
    // Keep the entire safe viewport. The old alpha.22 grid deliberately threw away the fractional
    // remainder and also capped every block by a nominal body-line count. With real font metrics
    // that could leave enough room for another complete row visibly unused. We now measure actual
    // line bottoms and only reject a row when it really crosses the safe page boundary.
    val pageGrid = ReaderPageGridCalculator.calculate(
        viewportHeightPx = viewportHeightPx,
        topPaddingPx = topPaddingPx,
        bottomPaddingPx = bottomPaddingPx,
        nominalLineHeightPx = nominalLineHeightPx,
        bottomSafetyPx = bottomSafetyPx,
    )
    val contentHeightPx = pageGrid.contentHeightPx

    val pages = mutableListOf<PreparedReaderPage>()
    var currentItems = mutableListOf<PreparedReaderPageItem>()
    var usedHeightPx = 0

    fun flushPage() {
        if (currentItems.isNotEmpty()) {
            pages += PreparedReaderPage(currentItems.toList())
            currentItems = mutableListOf()
            usedHeightPx = 0
        }
    }

    chapter.blocks.forEachIndexed { blockIndex, block ->
        if (blockIndex % 3 == 0) yield()

        when (block) {
            is ReaderBlock.Image -> {
                // Image height is only known after decoding. A dedicated page with ContentScale.Fit
                // guarantees that image loading can never push text outside the prepared viewport.
                flushPage()
                pages += PreparedReaderPage(listOf(PreparedReaderPageItem.Image(blockIndex)))
            }

            is ReaderBlock.Text -> {
                if (block.text.isEmpty()) return@forEachIndexed
                var start = 0

                while (start < block.text.length) {
                    yield()
                    val continuation = start > 0
                    val style = buildReaderTextStyle(block, settings, typography, family, density, continuation)
                    val textWidthPx = readerTextWidthPx(block, settings, density, viewportWidthPx)

                    val spacingBeforePx = if (currentItems.isEmpty()) 0 else paragraphSpacingPx
                    var availableHeightPx = contentHeightPx - usedHeightPx - spacingBeforePx

                    if (availableHeightPx < nominalLineHeightPx && currentItems.isNotEmpty()) {
                        flushPage()
                        availableHeightPx = contentHeightPx
                    }

                    // Measure a small text window first and only grow it when the entire probe
                    // really fits on the page. Typical phone pages stay around 4-12K chars instead
                    // of allocating a fresh 32K substring for every physical page of a huge block.
                    // The legacy 32K ceiling remains as a correctness fallback for pathological
                    // tiny fonts / very wide viewports / long unbroken strings.
                    fun measureWindow(heightPx: Int): Pair<String, TextLayoutResult> {
                        val maxLines = ReaderPageGridCalculator.measurementLineCap(
                            availableHeightPx = heightPx,
                            nominalLineHeightPx = nominalLineHeightPx,
                        )
                        var windowChars = ReaderPaginationWindowPolicy.initialWindowChars(maxLines)
                        while (true) {
                            val measurementEnd = minOf(block.text.length, start + windowChars)
                            val probe = block.text.substring(start, measurementEnd)
                            val measured = textMeasurer.measure(
                                text = probe,
                                style = style,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                                softWrap = true,
                                maxLines = maxLines,
                                constraints = Constraints(maxWidth = textWidthPx),
                            )
                            val completeSource = measurementEnd >= block.text.length
                            val enoughForPage = measured.hasVisualOverflow
                            if (completeSource || enoughForPage || windowChars >= ReaderPaginationWindowPolicy.MAX_WINDOW_CHARS) {
                                return probe to measured
                            }
                            windowChars = ReaderPaginationWindowPolicy.nextWindowChars(windowChars)
                        }
                    }

                    var measuredWindow = measureWindow(availableHeightPx)
                    var remainingText = measuredWindow.first
                    var layout = measuredWindow.second
                    var fitLine = lastLineThatFits(layout, availableHeightPx)
                    if (fitLine < 0 && currentItems.isNotEmpty()) {
                        flushPage()
                        availableHeightPx = contentHeightPx
                        measuredWindow = measureWindow(availableHeightPx)
                        remainingText = measuredWindow.first
                        layout = measuredWindow.second
                        fitLine = lastLineThatFits(layout, availableHeightPx)
                    }

                    // A pathological font can report a first row a few pixels higher than the
                    // nominal viewport. Keep forward progress, but this branch should be rare.
                    if (fitLine < 0) fitLine = 0

                    fun boundaryFor(line: Int): Pair<Int, Int> {
                        val measuredEnd = layout.getLineEnd(line, visibleEnd = false)
                            .coerceIn(1, remainingText.length)
                        val relativeEnd = avoidWordSplitAtPageBoundary(
                            text = remainingText,
                            measuredEnd = measuredEnd,
                            lastLineStart = layout.getLineStart(line),
                        )
                        return relativeEnd to (start + relativeEnd).coerceIn(start + 1, block.text.length)
                    }

                    var end = boundaryFor(fitLine).second
                    var measuredHeightPx = ceil(layout.getLineBottom(fitLine).toDouble()).toInt().coerceAtLeast(1)
                    val startsBlock = start == 0
                    val pageHasContent = currentItems.isNotEmpty()
                    val blockContinues = end < block.text.length
                    val headingFitsCompletely = end >= block.text.length
                    val remainingAfterBlockPx = availableHeightPx - measuredHeightPx
                    val moveStart = ReaderPaginationPolicy.moveSingleOpeningLine(
                        role = block.role,
                        atBlockStart = startsBlock,
                        pageAlreadyHasContent = pageHasContent,
                        fittedLineCount = fitLine + 1,
                        blockContinues = blockContinues,
                    ) || ReaderPaginationPolicy.keepHeadingWithBody(
                        role = block.role,
                        atBlockStart = startsBlock,
                        pageAlreadyHasContent = pageHasContent,
                        headingFitsCompletely = headingFitsCompletely,
                        remainingHeightPx = remainingAfterBlockPx,
                        requiredBodyHeightPx = paragraphSpacingPx + nominalLineHeightPx * 2,
                    )

                    if (moveStart) {
                        flushPage()
                        availableHeightPx = contentHeightPx
                        measuredWindow = measureWindow(availableHeightPx)
                        remainingText = measuredWindow.first
                        layout = measuredWindow.second
                        fitLine = lastLineThatFits(layout, availableHeightPx).coerceAtLeast(0)
                        val boundary = boundaryFor(fitLine)
                        end = boundary.second
                        measuredHeightPx = ceil(layout.getLineBottom(fitLine).toDouble()).toInt().coerceAtLeast(1)
                    }

                    // Keep a one-line paragraph tail from becoming a typographic orphan on the
                    // next page. This extra measurement only runs close to the end of a prose block.
                    if (fitLine >= 1 && end < block.text.length && block.text.length - end <= 2_048) {
                        val tail = block.text.substring(end)
                        if (tail.isNotBlank()) {
                            val tailStyle = buildReaderTextStyle(block, settings, typography, family, density, continuation = true)
                            val tailLayout = textMeasurer.measure(
                                text = tail,
                                style = tailStyle,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                                softWrap = true,
                                maxLines = 2,
                                constraints = Constraints(maxWidth = textWidthPx),
                            )
                            if (ReaderPaginationPolicy.pullSingleTrailingLine(
                                    role = block.role,
                                    fittedLineCount = fitLine + 1,
                                    remainderLineCount = tailLayout.lineCount,
                                    remainderOverflows = tailLayout.hasVisualOverflow,
                                )
                            ) {
                                fitLine -= 1
                                val boundary = boundaryFor(fitLine)
                                end = boundary.second
                                measuredHeightPx = ceil(layout.getLineBottom(fitLine).toDouble()).toInt().coerceAtLeast(1)
                            }
                        }
                    }

                    if (currentItems.isNotEmpty()) usedHeightPx += paragraphSpacingPx
                    currentItems += PreparedReaderPageItem.TextSlice(
                        blockIndex = blockIndex,
                        startCharOffset = start,
                        endCharOffset = end,
                        measuredHeightPx = measuredHeightPx,
                    )
                    usedHeightPx += measuredHeightPx
                    start = end

                    if (start < block.text.length) {
                        // A text block continues on the next physical page. Do not let Compose
                        // decide the boundary later by scrolling a clipped paragraph.
                        flushPage()
                    }
                }
            }
        }
    }

    flushPage()
    if (pages.isEmpty()) pages += PreparedReaderPage(emptyList())

    return PreparedReaderPagination(
        chapterIndex = chapterIndex,
        layoutKey = layoutKey,
        pages = pages,
        contentHeightPx = contentHeightPx,
    )
}

private fun avoidWordSplitAtPageBoundary(
    text: String,
    measuredEnd: Int,
    lastLineStart: Int,
): Int {
    if (measuredEnd <= 0 || measuredEnd >= text.length) return measuredEnd.coerceAtLeast(1)
    fun wordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'
    if (!wordChar(text[measuredEnd - 1]) || !wordChar(text[measuredEnd])) return measuredEnd

    var wordStart = measuredEnd - 1
    while (wordStart > 0 && wordChar(text[wordStart - 1])) wordStart--
    // If the whole visual line is one very long word, keep Compose's legal wrap point so that
    // pagination always advances. Otherwise move the whole word to the next physical page.
    return if (wordStart > lastLineStart) wordStart else measuredEnd
}

private fun lastLineThatFits(layout: TextLayoutResult, availableHeightPx: Int): Int {
    if (layout.lineCount <= 0) return -1
    var answer = -1
    for (line in 0 until layout.lineCount) {
        if (ceil(layout.getLineBottom(line).toDouble()).toInt() <= availableHeightPx) answer = line else break
    }
    return answer
}


/**
 * Builds a tiny, fast page around the saved reading anchor. The result is intentionally marked
 * incomplete and must never be put in [PreparedReaderPaginationCache]. Exact chapter pagination
 * continues immediately afterwards and replaces this preview while preserving the same anchor.
 *
 * We cap the synthetic source aggressively so reopening a book in the middle of a huge paragraph
 * never waits for the rest of that paragraph just to paint useful text.
 */
internal suspend fun prepareReaderAnchorPreviewPagination(
    chapterIndex: Int,
    chapter: ChapterContent,
    startBlockIndex: Int,
    startCharOffset: Int,
    settings: ReaderSettings,
    textMeasurer: TextMeasurer,
    typography: Typography,
    family: FontFamily,
    density: Density,
    viewportWidthPx: Int,
    viewportHeightPx: Int,
    layoutKey: String,
): PreparedReaderPagination? {
    if (chapter.blocks.isEmpty()) return null
    val firstBlockIndex = startBlockIndex.coerceIn(0, chapter.blocks.lastIndex)
    val previewBlocks = mutableListOf<ReaderBlock>()
    val originalIndexes = mutableListOf<Int>()
    val originalCharBases = mutableListOf<Int>()

    for (originalIndex in firstBlockIndex until minOf(chapter.blocks.size, firstBlockIndex + ReaderPreviewPolicy.MAX_BLOCKS)) {
        val original = chapter.blocks[originalIndex]
        when (original) {
            is ReaderBlock.Image -> {
                previewBlocks += original
                originalIndexes += originalIndex
                originalCharBases += 0
            }
            is ReaderBlock.Text -> {
                val base = if (originalIndex == firstBlockIndex) {
                    startCharOffset.coerceIn(0, original.text.length)
                } else {
                    0
                }
                if (base >= original.text.length) continue
                val end = ReaderPreviewPolicy.boundedEnd(base, original.text.length)
                previewBlocks += sliceReaderTextBlock(original, base, end)
                originalIndexes += originalIndex
                originalCharBases += base
            }
        }
        // A single image is already a complete visual preview page. Avoid scanning extra blocks.
        if (previewBlocks.lastOrNull() is ReaderBlock.Image) break
    }
    if (previewBlocks.isEmpty()) return null

    val synthetic = ChapterContent(index = chapter.index, title = chapter.title, blocks = previewBlocks)
    val prepared = prepareReaderChapterPagination(
        chapterIndex = chapterIndex,
        chapter = synthetic,
        settings = settings,
        textMeasurer = textMeasurer,
        typography = typography,
        family = family,
        density = density,
        viewportWidthPx = viewportWidthPx,
        viewportHeightPx = viewportHeightPx,
        layoutKey = "$layoutKey:preview",
    )
    val firstPage = prepared.pages.firstOrNull() ?: return null
    val remapped = firstPage.items.mapNotNull { item ->
        val originalIndex = originalIndexes.getOrNull(item.blockIndex) ?: return@mapNotNull null
        val charBase = originalCharBases.getOrElse(item.blockIndex) { 0 }
        when (item) {
            is PreparedReaderPageItem.Image -> PreparedReaderPageItem.Image(originalIndex)
            is PreparedReaderPageItem.TextSlice -> PreparedReaderPageItem.TextSlice(
                blockIndex = originalIndex,
                startCharOffset = charBase + item.startCharOffset,
                endCharOffset = charBase + item.endCharOffset,
                measuredHeightPx = item.measuredHeightPx,
            )
        }
    }
    if (remapped.isEmpty()) return null
    return PreparedReaderPagination(
        chapterIndex = chapterIndex,
        layoutKey = layoutKey,
        pages = listOf(PreparedReaderPage(remapped)),
        contentHeightPx = prepared.contentHeightPx,
        isComplete = false,
    )
}

internal fun findPreparedPageIndex(
    pagination: PreparedReaderPagination,
    blockIndex: Int,
    charOffset: Int,
    legacyPixelOffsetPx: Int = 0,
): Int {
    if (pagination.pages.isEmpty()) return 0
    if (blockIndex == Int.MAX_VALUE) return pagination.pages.lastIndex

    val safeBlock = blockIndex.coerceAtLeast(0)
    val targetChar = charOffset.coerceAtLeast(0)
    val matchingPages = pagination.pages.indices.filter { pageIndex ->
        pagination.pages[pageIndex].items.any { it.blockIndex == safeBlock }
    }
    if (matchingPages.isEmpty()) {
        return pagination.pages.indexOfLast { it.startBlockIndex <= safeBlock }.coerceAtLeast(0)
    }

    if (charOffset == Int.MAX_VALUE) return matchingPages.last()

    if (targetChar > 0) {
        matchingPages.forEach { pageIndex ->
            pagination.pages[pageIndex].items.forEach { item ->
                val slice = item as? PreparedReaderPageItem.TextSlice ?: return@forEach
                if (slice.blockIndex == safeBlock && targetChar in slice.startCharOffset until slice.endCharOffset) {
                    return pageIndex
                }
            }
        }
        return matchingPages.lastOrNull { pageIndex ->
            pagination.pages[pageIndex].items.any { item ->
                item is PreparedReaderPageItem.TextSlice &&
                    item.blockIndex == safeBlock &&
                    item.startCharOffset <= targetChar
            }
        } ?: matchingPages.first()
    }

    // One-time migration from alpha.16.x pixel anchors. Long paragraphs accumulated their
    // LazyColumn pixel offset as pages advanced, so page-height buckets preserve the old place
    // surprisingly well until the first new character anchor is saved.
    if (legacyPixelOffsetPx > 0 && pagination.contentHeightPx > 0) {
        val approximate = legacyPixelOffsetPx / pagination.contentHeightPx
        return matchingPages[approximate.coerceIn(0, matchingPages.lastIndex)]
    }

    return matchingPages.first()
}

internal fun sliceReaderTextBlock(
    block: ReaderBlock.Text,
    startCharOffset: Int,
    endCharOffset: Int,
): ReaderBlock.Text {
    val start = startCharOffset.coerceIn(0, block.text.length)
    val end = endCharOffset.coerceIn(start, block.text.length)
    val slicedLinks = block.links.mapNotNull { link ->
        val overlapStart = maxOf(start, link.start)
        val overlapEnd = minOf(end, link.end)
        if (overlapStart >= overlapEnd) null else ReaderLink(
            start = overlapStart - start,
            end = overlapEnd - start,
            target = link.target,
            external = link.external,
        )
    }
    return block.copy(
        text = block.text.substring(start, end),
        anchors = if (start == 0) block.anchors else emptyList(),
        links = slicedLinks,
        // Keep the marker column on continuation pages without drawing a second bullet/number.
        listMarker = if (start == 0) block.listMarker else "\u00A0",
    )
}
