package io.fluxreader.app.reader

import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ReaderBlock
import io.fluxreader.app.data.TextRole

/** Non-text elements that live on the same logical offset axis as [SpeedReaderDocument.text]. */
data class SpeedMediaCue(
    val offset: Int,
    val ref: String,
    val alt: String = "",
)

data class SpeedHeadingCue(
    val startOffset: Int,
    val endOffset: Int,
    val text: String,
    val role: TextRole,
)

data class SpeedReaderDocument(
    val text: String,
    val mediaCues: List<SpeedMediaCue> = emptyList(),
    val headingCues: List<SpeedHeadingCue> = emptyList(),
) {
    /** Skip the leading chapter-heading cluster after it has been shown as a chapter intro. */
    fun readableOffset(preferredOffset: Int): Int {
        if (text.isEmpty() || headingCues.isEmpty()) return preferredOffset.coerceIn(0, text.length)
        val preferred = preferredOffset.coerceIn(0, text.length)
        val headings = headingCues.sortedBy { it.startOffset }
        val containing = headings.indexOfFirst { preferred in it.startOffset..it.endOffset }
        var index: Int
        var cursor: Int
        if (containing >= 0) {
            index = containing + 1
            cursor = headings[containing].endOffset.coerceIn(0, text.length)
        } else if (preferred == 0 && headings.first().startOffset == 0) {
            index = 0
            cursor = 0
        } else {
            return preferred
        }
        while (index < headings.size) {
            val cue = headings[index]
            if (cue.endOffset <= cursor) {
                index++
                continue
            }
            if (cue.startOffset < cursor) {
                cursor = maxOf(cursor, cue.endOffset).coerceAtMost(text.length)
                index++
                continue
            }
            val gap = text.substring(cursor, cue.startOffset.coerceAtMost(text.length))
            if (gap.isNotBlank()) break
            cursor = cue.endOffset.coerceAtMost(text.length)
            index++
        }
        return cursor.coerceIn(0, text.length)
    }
}

/**
 * Builds the speed-reader stream without flattening away document semantics.
 * Text offsets intentionally mirror ChapterContent.plainText(): this keeps saved positions and
 * "start Speed Reader from tapped word" compatible with the normal reader.
 */
object SpeedReaderDocumentBuilder {
    fun fromChapter(chapter: ChapterContent): SpeedReaderDocument {
        val text = StringBuilder()
        val media = mutableListOf<SpeedMediaCue>()
        val headings = mutableListOf<SpeedHeadingCue>()

        fun appendText(value: String, role: TextRole?) {
            val clean = value.trim()
            if (clean.isBlank()) return
            val start = text.length
            text.append(clean).append("\n\n")
            if (role == TextRole.TITLE || role == TextRole.HEADING) {
                headings += SpeedHeadingCue(
                    startOffset = start,
                    endOffset = start + clean.length,
                    text = clean,
                    role = role,
                )
            }
        }

        if (chapter.title.isNotBlank()) appendText(chapter.title, TextRole.TITLE)
        chapter.blocks.forEach { block ->
            when (block) {
                is ReaderBlock.Text -> appendText(block.text, block.role)
                is ReaderBlock.Image -> media += SpeedMediaCue(text.length, block.ref, block.alt)
            }
        }

        return SpeedReaderDocument(
            text = text.toString().trimEnd(),
            mediaCues = media,
            headingCues = headings.distinctBy { Triple(it.startOffset, it.endOffset, it.text) },
        )
    }
}
