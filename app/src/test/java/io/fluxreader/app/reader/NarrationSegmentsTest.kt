package io.fluxreader.app.reader

import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ReaderBlock
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NarrationSegmentsTest {
    @Test fun segmentOffsetsReferToOriginalUnmodifiedText() {
        val text = "   Ветер стих. Наступил вечер.   И вдруг зазвонил телефон. "
        val chapter = ChapterContent(0, "Глава", listOf(ReaderBlock.Text(text)))
        val segments = NarrationSegments.fromChapter(chapter, Locale.forLanguageTag("ru-RU"))
        assertFalse(segments.isEmpty())
        assertTrue(segments.all { segment ->
            text.substring(segment.startOffset, segment.startOffset + segment.text.length) == segment.text
        })
        assertEquals(0, NarrationSegments.seek(segments, 0, 0))
    }

    @Test fun chunksRemainBoundedAndNeverSplitSurrogatePairs() {
        val text = ("Слова и мысли ".repeat(80) + "😀").repeat(14)
        val chapter = ChapterContent(2, "Большая", listOf(ReaderBlock.Text(text)))
        val segments = NarrationSegments.fromChapter(chapter, Locale.forLanguageTag("ru-RU"))
        assertTrue(segments.size > 2)
        assertTrue(segments.all { it.text.length <= 700 })
        assertTrue(segments.all { segment ->
            text.substring(segment.startOffset, segment.startOffset + segment.text.length) == segment.text
        })
        assertTrue(segments.none { segment ->
            segment.text.firstOrNull()?.isLowSurrogate() == true ||
                segment.text.lastOrNull()?.isHighSurrogate() == true
        })
        assertEquals(segments.lastIndex, NarrationSegments.seek(segments, 2, text.length + 100))
    }
}
