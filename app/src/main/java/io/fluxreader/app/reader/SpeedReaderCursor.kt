package io.fluxreader.app.reader

import io.fluxreader.app.data.SpeedPacingMode
import kotlin.math.abs
import kotlin.math.roundToInt

/** A measured Speed Reader unit. Offsets always point into the original chapter text. */
data class SpeedChunk(
    val tokens: List<String>,
    val startOffset: Int,
    val endOffset: Int,
    val focusTokenIndex: Int,
)

data class SpeedPauseContext(
    val before: String,
    val after: String,
)

data class SpeedTextWindow(
    val text: String,
    val startOffset: Int,
)

object SpeedReaderCursor {
    private val sentenceEnd = Regex("[.!?…]+[\\\"'»”’)\\]}]*$")
    private val clauseEnd = Regex("[,;:]+[\\\"'»”’)\\]}]*$")
    private val trailingDash = Regex("[—–-]+[\\\"'»”’)\\]}]*$")
    // Match the whitespace accepted by the streaming cursor, including EPUB NBSP/narrow NBSP.
    private val tokenPattern = Regex("[^\\s\\p{Zs}\\p{Zl}\\p{Zp}\\u001C-\\u001F]+")
    private val paragraphBoundary = Regex("\n[\\p{Zs}\\t\\r]*\n+")
    private val commonAbbreviation = Regex(
        "(?iu)^(?:т\\.д|т\\.п|т\\.е|т\\.к|и\\.т\\.д|и\\.т\\.п|г|гг|стр|рис|табл|см|им|ул|д|кв|корп|ред|пер|ок|проф|доц|акад|тов|г-н|г-жа)\\.$"
    )
    private val initials = Regex("(?iu)^[A-ZА-ЯЁ]\\.$")
    private val weakTrailingWords = setOf(
        "а", "без", "в", "во", "для", "до", "за", "и", "из", "к", "как", "ко", "на",
        "не", "ни", "но", "о", "об", "от", "по", "при", "с", "со", "у", "что", "чтобы",
        "если", "когда", "или",
    )

    fun next(
        text: String,
        offset: Int,
        wordCount: Int,
        smartChunking: Boolean = false,
        adaptiveWordCount: Boolean = false,
        pacingMode: SpeedPacingMode = SpeedPacingMode.SMART,
    ): SpeedChunk? {
        if (text.isEmpty()) return null
        var cursor = offset.coerceIn(0, text.length)
        while (cursor < text.length && text[cursor].isWhitespace()) cursor++
        if (cursor >= text.length) return null

        val start = cursor
        val preferredCount = wordCount.coerceIn(1, 7)
        val count = if (adaptiveWordCount && preferredCount > 1) {
            adaptiveFixationCount(text, cursor, preferredCount, pacingMode)
        } else {
            preferredCount
        }
        val tokens = ArrayList<String>(count)
        val minimumBeforeSoftBreak = if (count <= 2) 1 else 2

        while (tokens.size < count && cursor < text.length) {
            while (cursor < text.length && text[cursor].isWhitespace()) cursor++
            if (cursor >= text.length) break
            val tokenStart = cursor
            while (cursor < text.length && !text[cursor].isWhitespace()) cursor++
            val token = text.substring(tokenStart, cursor)
            if (token.isBlank()) continue
            tokens += token

            if (smartChunking) {
                val gapEnd = nextNonWhitespace(text, cursor)
                val gap = text.substring(cursor, gapEnd)
                val paragraphBreak = gap.count { it == '\n' } >= 2
                val hardBreak = isSentenceBoundary(token) || paragraphBreak
                val softBreak = tokens.size >= minimumBeforeSoftBreak &&
                    (clauseEnd.containsMatchIn(token) || trailingDash.containsMatchIn(token))
                if (hardBreak || softBreak) break
            }
        }
        // In phrase/focus modes do not leave a short connector dangling at the end of a fixation.
        // Pull exactly one following token when it keeps a natural Russian phrase together. RSVP
        // (wordCount = 1) intentionally remains one-token-at-a-time.
        if (smartChunking && count > 1 && tokens.size in 1..6 && shouldGlueNextToken(tokens.last()) &&
            !hasParagraphBreakAfter(text, cursor)) {
            var extraCursor = cursor
            while (extraCursor < text.length && text[extraCursor].isWhitespace()) extraCursor++
            if (extraCursor < text.length) {
                var extraEnd = extraCursor
                while (extraEnd < text.length && !text[extraEnd].isWhitespace()) extraEnd++
                val extra = text.substring(extraCursor, extraEnd)
                if (extra.isNotBlank()) {
                    tokens += extra
                    cursor = extraEnd
                }
            }
        }
        if (tokens.isEmpty()) return null
        return SpeedChunk(
            tokens = tokens,
            startOffset = start,
            endOffset = cursor,
            focusTokenIndex = visualFocusToken(tokens),
        )
    }

    private fun shouldGlueNextToken(token: String): Boolean {
        val lexical = lexicalCore(token).lowercase()
        return lexical in weakTrailingWords &&
            !isSentenceBoundary(token) &&
            !clauseEnd.containsMatchIn(token) &&
            !trailingDash.containsMatchIn(token)
    }

    /** Returns the beginning of the token that contains [offset]. If the tap lands in whitespace,
     *  the next token is preferred, matching the visual word the user touched in the reader. */
    fun wordStartOffset(text: String, offset: Int): Int {
        if (text.isEmpty()) return 0
        var cursor = offset.coerceIn(0, text.length)
        if (cursor == text.length) cursor = (cursor - 1).coerceAtLeast(0)
        if (text[cursor].isWhitespace()) {
            while (cursor < text.length && text[cursor].isWhitespace()) cursor++
            if (cursor >= text.length) {
                cursor = (offset.coerceAtMost(text.length) - 1).coerceAtLeast(0)
                while (cursor > 0 && text[cursor].isWhitespace()) cursor--
            }
        }
        while (cursor > 0 && !text[cursor - 1].isWhitespace()) cursor--
        return cursor.coerceIn(0, text.length)
    }

    /** Peripheral words shown to the right of the fixed RSVP word in Reedy-like flow mode. */
    fun followingTokens(text: String, offset: Int, limit: Int): List<String> {
        if (text.isBlank() || limit <= 0) return emptyList()
        val result = ArrayList<String>(limit.coerceAtMost(6))
        var cursor = offset.coerceIn(0, text.length)
        while (result.size < limit.coerceIn(0, 6) && cursor < text.length) {
            while (cursor < text.length && text[cursor].isWhitespace()) cursor++
            if (cursor >= text.length) break
            val start = cursor
            while (cursor < text.length && !text[cursor].isWhitespace()) cursor++
            if (cursor > start) result += text.substring(start, cursor)
        }
        return result
    }

    fun focusCharacterIndex(token: String): Int {
        if (token.isEmpty()) return 0
        val trailing = setOf('“', '”', '«', '»', ')', ']', '}', '\'', '"', ',', ';', ':', '.', '!', '?', '…')
        val lexicalStart = token.indexOfFirst { it.isLetterOrDigit() }.let { if (it < 0) 0 else it }
        val lexicalEndExclusive = token.indexOfLast { it !in trailing }.let { if (it < lexicalStart) token.length else it + 1 }
        val cleanLength = (lexicalEndExclusive - lexicalStart).coerceAtLeast(1)
        val withinWord = when {
            cleanLength <= 1 -> 0
            cleanLength <= 5 -> 1
            cleanLength <= 9 -> 2
            cleanLength <= 13 -> 3
            else -> 4
        }
        val target = (lexicalStart + withinWord).coerceIn(0, token.lastIndex)
        // Hyphens, fraction bars and combining accents can fall inside the lexical range.
        if (token[target].isLetterOrDigit()) return target
        for (distance in 1..token.lastIndex) {
            val left = target - distance
            if (left >= 0 && token[left].isLetterOrDigit()) return left
            val right = target + distance
            if (right < token.length && token[right].isLetterOrDigit()) return right
        }
        return target
    }

    /**
     * Smoothly ramps from a quieter start speed to the requested WPM after Play.
     * The target WPM is never exceeded and remains the user's canonical speed.
     */
    fun rampedWpm(
        targetWpm: Int,
        elapsedMs: Long,
        enabled: Boolean = true,
        startPercent: Int = 72,
        durationSeconds: Int = 5,
    ): Int {
        val target = targetWpm.coerceIn(100, 3000)
        if (!enabled) return target
        val durationMs = durationSeconds.coerceIn(1, 20) * 1_000L
        if (elapsedMs <= 0L) return (target * startPercent.coerceIn(45, 100) / 100.0).roundToInt().coerceAtLeast(100)
        if (elapsedMs >= durationMs) return target
        val start = target * startPercent.coerceIn(45, 100) / 100.0
        val raw = elapsedMs.toDouble() / durationMs.toDouble()
        // Smoothstep avoids a visible speed jump immediately after pressing Play.
        val eased = raw * raw * (3.0 - 2.0 * raw)
        return (start + (target - start) * eased).roundToInt().coerceIn(100, target)
    }

    /**
     * Keeps WPM as the baseline while adding small processing windows at semantic boundaries.
     * Sentence/clause pauses are fixed milliseconds rather than WPM fractions so they remain
     * perceptible at high speeds. DEEP scales semantic/complexity adjustments upward.
     */
    fun delayMillis(
        wpm: Int,
        chunk: SpeedChunk,
        chapterText: String = "",
        pacingMode: SpeedPacingMode = SpeedPacingMode.SMART,
        paragraphPause: Boolean = true,
        microPausesEnabled: Boolean = true,
        sentencePauseMs: Int = 170,
        clausePauseMs: Int = 65,
        paragraphPauseMs: Int = 260,
        adaptiveSentencePause: Boolean = true,
        sentenceLengthBonusMs: Int = 120,
        longWordSlowdownPercent: Int = 18,
    ): Long {
        val base = 60_000.0 / wpm.coerceIn(100, 3000).toDouble()
        if (pacingMode == SpeedPacingMode.CONSTANT) {
            return (base * chunk.tokens.size.coerceAtLeast(1)).toLong().coerceAtLeast(20L)
        }

        val deep = pacingMode == SpeedPacingMode.DEEP
        val complexityScale = if (deep) 1.30 else 1.0
        val configuredLongWord = longWordSlowdownPercent.coerceIn(0, 60) / 100.0
        var wordUnits = 0.0
        for (token in chunk.tokens) {
            val lexical = lexicalCore(token)
            val length = lexical.length
            var weight = when {
                length <= 3 -> 0.92
                length <= 7 -> 1.0
                length <= 11 -> 1.0 + configuredLongWord * 0.45 * complexityScale
                length <= 16 -> 1.0 + configuredLongWord * 0.90 * complexityScale
                else -> 1.0 + configuredLongWord * 1.35 * complexityScale
            }
            if (lexical.any(Char::isDigit)) weight += 0.10 * complexityScale
            if (lexical.length >= 3 && lexical.count(Char::isUpperCase) >= lexical.length * 2 / 3) {
                weight += 0.08 * complexityScale
            }
            if ('/' in lexical || '@' in lexical || lexical.startsWith("http", ignoreCase = true)) {
                weight += 0.16 * complexityScale
            }
            if (lexical.any { !it.isLetterOrDigit() && it != '-' && it != '–' && it != '—' }) {
                weight += 0.04 * complexityScale
            }
            wordUnits += weight
        }

        var totalMs = base * wordUnits
        val last = chunk.tokens.lastOrNull().orEmpty()
        val paragraphBreak = paragraphPause && hasParagraphBreakAfter(chapterText, chunk.endOffset)
        val semanticScale = if (deep) 1.28 else 1.0

        if (microPausesEnabled) {
            var microPause = when {
                isSentenceBoundary(last) -> {
                    val normalized = last.trimEnd('”', '’', '»', ')', ']', '}', '\'', '"')
                    val punctuationScale = when {
                        normalized.endsWith("…") || normalized.endsWith("...") -> 1.25
                        normalized.endsWith("!") || normalized.endsWith("?") -> 1.10
                        else -> 1.0
                    }
                    sentencePauseMs.coerceIn(0, 700) * punctuationScale
                }
                clauseEnd.containsMatchIn(last) -> {
                    val normalized = last.trimEnd('”', '’', '»', ')', ']', '}', '\'', '"')
                    val punctuationScale = if (normalized.endsWith(';') || normalized.endsWith(':')) 1.25 else 1.0
                    clausePauseMs.coerceIn(0, 350) * punctuationScale
                }
                trailingDash.containsMatchIn(last) -> clausePauseMs.coerceIn(0, 350) * 0.80
                else -> 0.0
            }
            if (isSentenceBoundary(last) && adaptiveSentencePause && sentenceLengthBonusMs > 0) {
                val words = sentenceWordCountEndingAt(chapterText, chunk.endOffset)
                // Short sentences need little extra integration time; long ones asymptotically
                // approach the configured bonus instead of creating dramatic stalls.
                val ratio = ((words - 10).coerceAtLeast(0) / 26.0).coerceIn(0.0, 1.0)
                microPause += sentenceLengthBonusMs.coerceIn(0, 400) * ratio
            }
            totalMs += microPause * semanticScale
        }

        if (paragraphBreak) {
            totalMs += paragraphPauseMs.coerceIn(0, 1_200) * semanticScale
        }
        // 20 ms preserves 3000 WPM. A fixed upper cap would silently accelerate long phrases
        // at low WPM and erase the configured complex-word slowdown.
        return totalMs.toLong().coerceAtLeast(20L)
    }

    fun hasSemanticPause(chunk: SpeedChunk, chapterText: String = ""): Boolean {
        val last = chunk.tokens.lastOrNull().orEmpty()
        return isSentenceBoundary(last) || clauseEnd.containsMatchIn(last) ||
            trailingDash.containsMatchIn(last) || hasParagraphBreakAfter(chapterText, chunk.endOffset)
    }


    /**
     * Returns a stable beginning of the sentence containing [offset]. This is used when playback
     * resumes after a long interruption so the reader gets one short re-entry instead of continuing
     * in the middle of a thought.
     */
    fun sentenceStartOffset(text: String, offset: Int): Int {
        if (text.isBlank()) return 0
        val bounded = offset.coerceIn(0, text.length)
        // Never classify a truncated current token (for example the "3." prefix of "3.14").
        val tokens = tokenPattern.findAll(text).takeWhile { it.range.last < bounded }.toList()
        for (match in tokens.asReversed()) {
            if (isSentenceBoundary(match.value)) {
                return nextNonWhitespace(text, match.range.last + 1).coerceAtMost(text.length)
            }
        }
        return nextNonWhitespace(text, 0)
    }

    fun previousSentenceOffset(text: String, offset: Int): Int? {
        if (text.isBlank() || offset <= 0) return null
        val bounded = offset.coerceIn(0, text.length)
        val currentStart = sentenceStartOffset(text, bounded)
        // When the cursor is already inside a sentence, first return to its beginning.
        if (bounded - currentStart > 2) return currentStart
        if (currentStart <= 0) return null
        val before = tokenPattern.findAll(text.substring(0, (currentStart - 1).coerceAtLeast(0))).toList()
        var previousBoundaryEnd = -1
        for (match in before.asReversed()) {
            if (isSentenceBoundary(match.value)) {
                if (previousBoundaryEnd < 0) {
                    previousBoundaryEnd = match.range.last + 1
                } else {
                    return nextNonWhitespace(text, match.range.last + 1)
                }
            }
        }
        return if (previousBoundaryEnd >= 0) nextNonWhitespace(text, 0) else null
    }

    fun nextSentenceOffset(text: String, offset: Int): Int? {
        if (text.isBlank()) return null
        val bounded = offset.coerceIn(0, text.length)
        val previousToken = tokenPattern.findAll(text).takeWhile { it.range.last < bounded }.lastOrNull()
        if (previousToken != null && isSentenceBoundary(previousToken.value) &&
            text.substring(previousToken.range.last + 1, bounded).isBlank()) {
            val direct = nextNonWhitespace(text, bounded)
            return direct.takeIf { it < text.length }
        }
        val scanStart = if (bounded < text.length && !text[bounded].isWhitespace()) wordStartOffset(text, bounded) else bounded
        for (match in tokenPattern.findAll(text, scanStart)) {
            if (isSentenceBoundary(match.value)) {
                val next = nextNonWhitespace(text, match.range.last + 1)
                return next.takeIf { it < text.length }
            }
        }
        return null
    }

    fun previousParagraphOffset(text: String, offset: Int): Int? {
        if (text.isBlank() || offset <= 0) return null
        val bounded = offset.coerceIn(0, text.length)
        val breaks = paragraphBoundary.findAll(text.substring(0, bounded)).toList()
        val currentStart = breaks.lastOrNull()?.range?.last?.plus(1)?.let { nextNonWhitespace(text, it) } ?: 0
        if (bounded - currentStart > 2) return currentStart
        val previous = breaks.dropLast(1).lastOrNull() ?: return if (currentStart > 0) 0 else null
        return nextNonWhitespace(text, previous.range.last + 1)
    }

    fun nextParagraphOffset(text: String, offset: Int): Int? {
        if (text.isBlank()) return null
        val bounded = offset.coerceIn(0, text.length)
        val match = paragraphBoundary.find(text, bounded) ?: return null
        val next = nextNonWhitespace(text, match.range.last + 1)
        return next.takeIf { it < text.length }
    }

    /** A paragraph-sized pause preview that keeps an absolute offset for tap-to-seek. */
    fun textWindow(text: String, centerOffset: Int, maxChars: Int = 620): SpeedTextWindow {
        if (text.isBlank()) return SpeedTextWindow("", 0)
        val center = centerOffset.coerceIn(0, text.length)
        val radius = (maxChars.coerceIn(160, 1_600) / 2).coerceAtLeast(80)
        var start = (center - radius).coerceAtLeast(0)
        var end = (center + radius).coerceAtMost(text.length)

        val paragraphBefore = text.lastIndexOf("\n\n", (center - 1).coerceAtLeast(0))
        if (paragraphBefore >= 0 && center - paragraphBefore <= radius * 2) start = paragraphBefore + 2
        val paragraphAfter = text.indexOf("\n\n", center)
        if (paragraphAfter >= 0 && paragraphAfter - center <= radius * 2) end = paragraphAfter

        if (start > 0 && !text[start - 1].isWhitespace()) {
            while (start < center && !text[start].isWhitespace()) start++
            while (start < center && text[start].isWhitespace()) start++
        }
        if (end < text.length && !text[end].isWhitespace()) {
            while (end > center && !text[end - 1].isWhitespace()) end--
        }
        if (end <= start) {
            start = (center - radius).coerceAtLeast(0)
            end = (center + radius).coerceAtMost(text.length)
        }
        while (start < end && text[start].isWhitespace()) start++
        while (end > start && text[end - 1].isWhitespace()) end--
        return SpeedTextWindow(text.substring(start, end), start)
    }

    fun pauseContext(
        text: String,
        startOffset: Int,
        endOffset: Int,
        wordsEachSide: Int = 7,
    ): SpeedPauseContext {
        if (text.isBlank()) return SpeedPauseContext("", "")
        val count = wordsEachSide.coerceIn(2, 16)
        val beforeMatches = tokenPattern.findAll(text.substring(0, startOffset.coerceIn(0, text.length))).toList()
        val afterStart = endOffset.coerceIn(0, text.length)
        val afterMatches = tokenPattern.findAll(text, afterStart).take(count).toList()
        return SpeedPauseContext(
            before = beforeMatches.takeLast(count).joinToString(" ") { it.value },
            after = afterMatches.joinToString(" ") { it.value },
        )
    }

    private fun adaptiveFixationCount(
        text: String,
        offset: Int,
        preferred: Int,
        pacingMode: SpeedPacingMode,
    ): Int {
        val base = preferred.coerceIn(2, 7)
        val preview = tokenPattern.findAll(text, offset).take((base + 1).coerceAtMost(7)).map { it.value }.toList()
        if (preview.isEmpty()) return base
        val lexical = preview.map(::lexicalCore)
        val visible = lexical.take(base)
        val averageLength = visible.map { it.length }.average().takeIf { !it.isNaN() } ?: 5.0
        val hasComplex = visible.any { token ->
            token.length >= 13 || token.any(Char::isDigit) || '/' in token || '@' in token || token.startsWith("http", true)
        }
        val deep = pacingMode == SpeedPacingMode.DEEP
        var adjusted = base
        if (hasComplex || averageLength >= if (deep) 7.0 else 8.5) adjusted--
        if (deep && averageLength >= 10.0) adjusted--
        if (!deep && !hasComplex && averageLength <= 4.2 && preview.size > base) adjusted++
        return adjusted.coerceIn(1, 7)
    }

    private fun lexicalCore(token: String): String = token.trim(
        '“', '”', '«', '»', '(', ')', '[', ']', '{', '}', '\'', '"', ',', ';', ':', '.', '!', '?', '…', '—', '–'
    )

    private fun isSentenceBoundary(token: String): Boolean {
        if (!sentenceEnd.containsMatchIn(token)) return false
        val normalized = token.trimEnd('”', '’', '»', ')', ']', '}', '\'', '"')
        if (normalized.endsWith("…") || normalized.endsWith("!") || normalized.endsWith("?")) return true
        if (commonAbbreviation.matches(normalized) || initials.matches(normalized)) return false
        // Avoid an exaggerated sentence pause on version/decimal-like values such as 3.14.
        val core = normalized.dropLastWhile { it == '.' }
        if (core.count { it == '.' } >= 1 && core.any(Char::isDigit)) return false
        return true
    }

    private fun sentenceWordCountEndingAt(text: String, endOffset: Int): Int {
        if (text.isBlank() || endOffset <= 0) return 0
        val end = endOffset.coerceIn(0, text.length)
        val start = (end - 1_600).coerceAtLeast(0)
        val tokens = tokenPattern.findAll(text.substring(start, end)).map { it.value }.toList()
        if (tokens.isEmpty()) return 0
        var count = 0
        var currentSentenceEndSkipped = false
        for (token in tokens.asReversed()) {
            if (currentSentenceEndSkipped && isSentenceBoundary(token)) break
            count++
            currentSentenceEndSkipped = true
            if (count >= 80) break
        }
        return count
    }

    private fun nextNonWhitespace(text: String, offset: Int): Int {
        var cursor = offset.coerceIn(0, text.length)
        while (cursor < text.length && text[cursor].isWhitespace()) cursor++
        return cursor
    }

    private fun hasParagraphBreakAfter(text: String, offset: Int): Boolean {
        if (text.isBlank() || offset >= text.length) return false
        var cursor = offset.coerceAtLeast(0)
        var newlineCount = 0
        while (cursor < text.length && text[cursor].isWhitespace()) {
            if (text[cursor] == '\n') newlineCount++
            if (newlineCount >= 2) return true
            cursor++
        }
        return false
    }

    private fun visualFocusToken(tokens: List<String>): Int {
        if (tokens.size <= 1) return 0
        val widths = tokens.map { it.length.coerceAtLeast(1) }
        val total = widths.sum() + (tokens.size - 1)
        val target = total / 2.0
        var cursor = 0.0
        var best = 0
        var bestDistance = Double.MAX_VALUE
        for (index in tokens.indices) {
            val center = cursor + widths[index] / 2.0
            val distance = abs(center - target)
            if (distance < bestDistance) {
                best = index
                bestDistance = distance
            }
            cursor += widths[index] + 1.0
        }
        return best
    }
}
