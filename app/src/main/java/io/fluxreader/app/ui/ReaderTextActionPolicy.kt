package io.fluxreader.app.ui

/** Pure text-boundary helpers for Reader long-press/range actions. */
object ReaderTextActionPolicy {
    data class Selection(
        val wordStart: Int,
        val wordEndExclusive: Int,
        val word: String,
        val sentence: String,
        val paragraph: String,
    )

    data class RangeSelection(
        val start: Int,
        val endExclusive: Int,
        val text: String,
    )

    data class DocumentRangeSelection(
        val startBlockIndex: Int,
        val startCharOffset: Int,
        val endBlockIndex: Int,
        val endCharOffsetExclusive: Int,
        val text: String,
    )

    fun select(text: String, tappedOffset: Int): Selection? {
        if (text.isBlank()) return null
        val offset = tappedOffset.coerceIn(0, text.lastIndex)
        val wordStart = wordStart(text, offset)
        val wordEnd = wordEndExclusive(text, offset)
        if (wordStart >= wordEnd) return null
        val sentenceStart = sentenceStart(text, wordStart)
        val sentenceEnd = sentenceEndExclusive(text, wordEnd)
        return Selection(
            wordStart = wordStart,
            wordEndExclusive = wordEnd,
            word = text.substring(wordStart, wordEnd).trim(),
            sentence = text.substring(sentenceStart, sentenceEnd).trim(),
            paragraph = text.trim(),
        )
    }

    /**
     * Normalizes a drag range across chapter text blocks. The list is indexed by the chapter's real
     * block index; non-text/image entries can be null. Endpoint blocks snap to whole words while
     * intermediate text blocks are included in full.
     */
    fun selectDocumentRange(
        blockTexts: List<String?>,
        anchorBlockIndex: Int,
        anchorOffset: Int,
        currentBlockIndex: Int,
        currentOffset: Int,
    ): DocumentRangeSelection? {
        if (blockTexts.isEmpty()) return null
        val aBlock = anchorBlockIndex.coerceIn(0, blockTexts.lastIndex)
        val cBlock = currentBlockIndex.coerceIn(0, blockTexts.lastIndex)
        val anchorText = blockTexts[aBlock] ?: return null
        val currentText = blockTexts[cBlock] ?: return null
        if (anchorText.isBlank() || currentText.isBlank()) return null

        val forward = aBlock < cBlock || (aBlock == cBlock && anchorOffset <= currentOffset)
        val startBlock = if (forward) aBlock else cBlock
        val endBlock = if (forward) cBlock else aBlock
        val startRaw = if (forward) anchorOffset else currentOffset
        val endRaw = if (forward) currentOffset else anchorOffset
        val startText = blockTexts[startBlock] ?: return null
        val endText = blockTexts[endBlock] ?: return null
        val start = wordStart(startText, startRaw.coerceIn(0, startText.lastIndex))
        val end = wordEndExclusive(endText, endRaw.coerceIn(0, endText.lastIndex))
        if (startBlock == endBlock && start >= end) return null

        val excerpt = buildString {
            for (blockIndex in startBlock..endBlock) {
                val text = blockTexts.getOrNull(blockIndex) ?: continue
                val from = if (blockIndex == startBlock) start.coerceIn(0, text.length) else 0
                val to = if (blockIndex == endBlock) end.coerceIn(from, text.length) else text.length
                val part = text.substring(from, to).trim()
                if (part.isBlank()) continue
                if (isNotEmpty()) append("\n\n")
                append(part)
            }
        }.trim()
        if (excerpt.isBlank()) return null
        return DocumentRangeSelection(startBlock, start, endBlock, end, excerpt)
    }

    /** Expands both drag endpoints to whole-word boundaries. */
    fun selectRange(text: String, anchorOffset: Int, currentOffset: Int): RangeSelection? {
        if (text.isBlank()) return null
        val max = text.lastIndex
        val a = anchorOffset.coerceIn(0, max)
        val b = currentOffset.coerceIn(0, max)
        val low = minOf(a, b)
        val high = maxOf(a, b)
        val start = wordStart(text, low)
        val end = wordEndExclusive(text, high)
        if (start >= end) return null
        return RangeSelection(start, end, text.substring(start, end).trim())
    }

    private fun wordStart(text: String, offset: Int): Int {
        var index = offset.coerceIn(0, text.lastIndex)
        if (!isWordChar(text[index])) {
            val right = (index until text.length).firstOrNull { isWordChar(text[it]) }
            val left = (index downTo 0).firstOrNull { isWordChar(text[it]) }
            index = when {
                right == null -> left ?: return offset
                left == null -> right
                right - index <= index - left -> right
                else -> left
            }
        }
        while (index > 0 && isWordChar(text[index - 1])) index--
        return index
    }

    private fun wordEndExclusive(text: String, offset: Int): Int {
        var index = offset.coerceIn(0, text.lastIndex)
        if (!isWordChar(text[index])) {
            val right = (index until text.length).firstOrNull { isWordChar(text[it]) }
            val left = (index downTo 0).firstOrNull { isWordChar(text[it]) }
            index = when {
                right == null -> left ?: return (offset + 1).coerceAtMost(text.length)
                left == null -> right
                right - index <= index - left -> right
                else -> left
            }
        }
        var end = index + 1
        while (end < text.length && isWordChar(text[end])) end++
        return end
    }

    private fun sentenceStart(text: String, offset: Int): Int {
        var index = offset.coerceIn(0, text.length)
        while (index > 0) {
            val previous = text[index - 1]
            if (previous == '.' || previous == '!' || previous == '?' || previous == '\n') break
            index--
        }
        while (index < text.length && text[index].isWhitespace()) index++
        return index
    }

    private fun sentenceEndExclusive(text: String, offset: Int): Int {
        var index = offset.coerceIn(0, text.length)
        while (index < text.length) {
            val char = text[index]
            index++
            if (char == '.' || char == '!' || char == '?' || char == '\n') break
        }
        return index
    }

    private fun isWordChar(char: Char): Boolean = char.isLetterOrDigit() || char == '\'' || char == '’' || char == '-'
}
