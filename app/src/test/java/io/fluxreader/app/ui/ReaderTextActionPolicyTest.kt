package io.fluxreader.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ReaderTextActionPolicyTest {
    @Test
    fun selectsWordWithoutDialoguePunctuation() {
        val text = "— За окном медленно плыли снежинки, а капусты. остались на столе."
        val selected = ReaderTextActionPolicy.select(text, text.indexOf("капусты") + 3)
        assertNotNull(selected)
        assertEquals("капусты", selected!!.word)
        assertEquals(text, selected.paragraph)
    }

    @Test
    fun sentenceStopsAtTerminalPunctuation() {
        val text = "Первое предложение. Второе с нужным словом! Третье."
        val selected = ReaderTextActionPolicy.select(text, text.indexOf("нужным"))!!
        assertEquals("Второе с нужным словом!", selected.sentence)
    }

    @Test
    fun punctuationPressFindsNearbyWord() {
        val text = "слово, другое"
        val selected = ReaderTextActionPolicy.select(text, text.indexOf(','))!!
        assertEquals("слово", selected.word)
    }

    @Test fun rangeSelectionExpandsToWholeWords() {
        val text = "Один два три четыре"
        val range = ReaderTextActionPolicy.selectRange(text, 6, 10)!!
        assertEquals("два три", range.text)
        assertEquals(5, range.start)
        assertEquals(12, range.endExclusive)
    }

    @Test fun reverseDragKeepsOrderedRange() {
        val text = "Один два три четыре"
        val range = ReaderTextActionPolicy.selectRange(text, 15, 6)!!
        assertEquals("два три четыре", range.text)
    }
    @Test fun documentRangeSpansParagraphsInBothDirections() {
        val blocks = listOf<String?>(
            "Первый абзац заканчивается здесь",
            null,
            "Второй абзац продолжается дальше",
            "Третий абзац завершает мысль",
        )
        val forward = ReaderTextActionPolicy.selectDocumentRange(
            blocks, 0, blocks[0]!!.indexOf("абзац") + 2, 2, blocks[2]!!.indexOf("продолжается") + 3,
        )!!
        assertEquals(0, forward.startBlockIndex)
        assertEquals(2, forward.endBlockIndex)
        assertEquals("абзац заканчивается здесь\n\nВторой абзац продолжается", forward.text)

        val reverse = ReaderTextActionPolicy.selectDocumentRange(
            blocks, 3, blocks[3]!!.indexOf("завершает") + 2, 2, blocks[2]!!.indexOf("абзац") + 1,
        )!!
        assertEquals(2, reverse.startBlockIndex)
        assertEquals(3, reverse.endBlockIndex)
        assertEquals("абзац продолжается дальше\n\nТретий абзац завершает", reverse.text)
    }

}
