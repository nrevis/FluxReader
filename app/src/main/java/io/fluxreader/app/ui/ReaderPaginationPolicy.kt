package io.fluxreader.app.ui

import io.fluxreader.app.data.TextRole

/** Typographic page-boundary rules kept pure so they are cheap to unit-test. */
internal object ReaderPaginationPolicy {
    fun isProse(role: TextRole): Boolean = when (role) {
        TextRole.PARAGRAPH, TextRole.LIST_ITEM, TextRole.QUOTE, TextRole.POEM -> true
        else -> false
    }

    /** Avoid a lonely first line of a new paragraph at the bottom of an otherwise used page. */
    fun moveSingleOpeningLine(
        role: TextRole,
        atBlockStart: Boolean,
        pageAlreadyHasContent: Boolean,
        fittedLineCount: Int,
        blockContinues: Boolean,
    ): Boolean = isProse(role) && atBlockStart && pageAlreadyHasContent && blockContinues && fittedLineCount <= 1

    /** A heading should not sit at the page bottom without room for spacing + two body lines. */
    fun keepHeadingWithBody(
        role: TextRole,
        atBlockStart: Boolean,
        pageAlreadyHasContent: Boolean,
        headingFitsCompletely: Boolean,
        remainingHeightPx: Int,
        requiredBodyHeightPx: Int,
    ): Boolean =
        (role == TextRole.TITLE || role == TextRole.HEADING) &&
            atBlockStart && pageAlreadyHasContent && headingFitsCompletely &&
            remainingHeightPx < requiredBodyHeightPx

    /**
     * For a short paragraph, avoid leaving a single trailing line alone on the next page.
     * Long paragraphs stay packed: pulling a line from an 8–15 line paragraph created exactly
     * the conspicuous empty strip users reported at the bottom of otherwise full pages.
     */
    fun pullSingleTrailingLine(
        role: TextRole,
        fittedLineCount: Int,
        remainderLineCount: Int,
        remainderOverflows: Boolean,
    ): Boolean = isProse(role) && fittedLineCount in 2..4 && remainderLineCount == 1 && !remainderOverflows
}
