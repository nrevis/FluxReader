package io.fluxreader.app.ui

/** Pure hit-test for a persisted annotation against one source text offset. */
object ReaderAnnotationHitPolicy {
    fun contains(
        blockIndex: Int,
        sourceOffset: Int,
        startBlockIndex: Int,
        startCharOffset: Int,
        endBlockIndex: Int,
        endCharOffsetExclusive: Int,
    ): Boolean {
        if (blockIndex !in startBlockIndex..endBlockIndex) return false
        val start = if (blockIndex == startBlockIndex) startCharOffset else 0
        val end = if (blockIndex == endBlockIndex) endCharOffsetExclusive else Int.MAX_VALUE
        return sourceOffset >= start && sourceOffset < end
    }
}
