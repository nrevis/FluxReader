package io.fluxreader.app.reader

import org.junit.Assert.*
import org.junit.Test

class DialogueTextColoringTest {
    private val chapter = "Над станцией взошла луна.\n\n" +
        "— Привет, — сказала Нина. — Я вернусь.\n\n" +
        "— Пора, — ответил Марк.\n\nДверь тихо закрылась."
    private val colors = BookSpeakerColors(entries = mapOf(
        speakerColorKey("Нина") to SpeakerColorEntry("Нина", SpeakerTint.BLUE, SpeakerTint.ROSE),
        speakerColorKey("Марк") to SpeakerColorEntry("Марк", SpeakerTint.GREEN),
    ))
    private fun coloring(text: String = chapter, edits: List<DialogueCorrection> = emptyList()) =
        DialogueTextColoring(text, BookDialogueAnalyzer.local(text), edits, colors)

    @Test fun allSpeechPartsHaveTheCorrectNameAndAuthorRemarksRemainUnstyled() {
        val segments = coloring().segments(0, chapter.length)
        assertEquals(listOf("Нина", "Нина", "Марк"), segments.map { it.speaker })
        assertEquals(listOf(SpeakerTint.ROSE, SpeakerTint.ROSE, SpeakerTint.GREEN), segments.map { it.tint })
        for (word in listOf("Над", "сказала", "ответил", "Дверь")) {
            val offset = chapter.indexOf(word)
            assertTrue(word, segments.none { offset in it.start until it.endExclusive })
        }
    }

    @Test fun clippedContextOffsetsAreRelativeToTheVisibleOriginalSubstring() {
        val begin = chapter.indexOf("ивет")
        val end = chapter.indexOf("Пора") + 2
        val segments = coloring().segments(begin, end)
        assertEquals(0, segments.first().start)
        assertEquals(end - begin, segments.last().endExclusive)
        assertEquals(listOf("Нина", "Нина", "Марк"), segments.map { it.speaker })
        assertTrue(segments.all { it.start >= 0 && it.endExclusive <= end - begin })
    }

    @Test fun twoSpeakersInOneParagraphAreNotMerged() {
        val text = "— Да, — сказала Нина. — Нет, — ответил Марк."
        val segments = coloring(text).segments(0, text.length)
        assertEquals(listOf("Нина", "Марк"), segments.map { it.speaker })
        assertTrue(segments.first().endExclusive <= text.indexOf("сказала"))
        assertTrue(segments.last().start > text.indexOf("сказала"))
        assertEquals(SpeakerTint.GREEN, segments.last().tint)
    }

    @Test fun manualCorrectionChangesSpeechAndContinuationToTheSameNameAsTheLabel() {
        val reply = DialogueReplyLocator.at(chapter, chapter.indexOf("Привет"))!!
        val edits = listOf(DialogueCorrection(reply, "Марк"))
        val index = BookDialogueAnalyzer.local(chapter)
        val segments = DialogueTextColoring(chapter, index, edits, colors).segments(0, chapter.length)
        assertEquals(listOf("Марк", "Марк", "Марк"), segments.map { it.speaker })
        assertTrue(segments.all { it.tint == SpeakerTint.GREEN })
        for (query in listOf("Привет", "Я вернусь", "Пора")) {
            val offset = chapter.indexOf(query)
            assertEquals(DialogueReplyLocator.resolve(chapter, offset, index, edits)?.speaker,
                segments.single { offset in it.start until it.endExclusive }.speaker)
        }
    }

    @Test fun explicitlyUnknownManualCorrectionRemainsNeutral() {
        val reply = DialogueReplyLocator.at(chapter, chapter.indexOf("Пора"))!!
        val segments = coloring(edits = listOf(DialogueCorrection(reply, null))).segments(reply.start, reply.endExclusive)
        assertTrue(segments.isNotEmpty())
        assertTrue(segments.all { it.speaker == null && it.tint == null })
    }

    @Test fun unidentifiedSpeechDoesNotInheritThePreviousSpeakersTint() {
        val text = "— Слушай, — сказала Нина.\n\n***\n\n— Ты слышишь?"
        val segments = coloring(text).segments(text.indexOf("— Ты"), text.length)
        assertTrue(segments.isNotEmpty())
        assertTrue(segments.all { it.speaker == null && it.tint == null })
    }

    @Test fun knownNamesWithoutPaletteEntriesRetainTheirNamesWithoutInventingColors() {
        val segments = DialogueTextColoring(chapter, BookDialogueAnalyzer.local(chapter)).segments(0, chapter.length)
        assertEquals(listOf("Нина", "Нина", "Марк"), segments.map { it.speaker })
        assertTrue(segments.all { it.tint == null })
    }

    @Test fun indexForDifferentTextFallsBackToCurrentSpeechBoundariesAndNames() {
        val original = "— Привет, — сказала Нина."
        val current = "— Привет, — сказал Марк."
        val segments = DialogueTextColoring(current, BookDialogueAnalyzer.local(original), colors = colors)
            .segments(0, current.length)
        assertEquals("Марк", segments.single().speaker)
        assertEquals(SpeakerTint.GREEN, segments.single().tint)
        assertTrue(segments.none { current.indexOf("сказал") in it.start until it.endExclusive })
    }

    @Test fun noIndexStillUsesTheLocalDirectSpeechBoundaries() {
        assertEquals(coloring().segments(0, chapter.length),
            DialogueTextColoring(chapter, colors = colors).segments(0, chapter.length))
    }

    @Test fun proseAndEmptyWindowsProduceNoColorSegments() {
        assertTrue(coloring().segments(0, chapter.indexOf('\n')).isEmpty())
        val remark = chapter.indexOf("сказала")
        assertTrue(coloring().segments(remark, remark + "сказала Нина".length).isEmpty())
        assertTrue(coloring().segments(0, 0).isEmpty())
        assertTrue(coloring().segments(chapter.length, chapter.length).isEmpty())
    }

    @Test fun paletteLookupUsesTheSameNormalizedNameAsBookSpeakerColors() {
        val reply = DialogueReplyLocator.at(chapter, chapter.indexOf("Пора"))!!
        val color = coloring(edits = listOf(DialogueCorrection(reply, "  НИНА  ")))
            .segments(reply.start, reply.endExclusive).single()
        assertEquals("  НИНА  ", color.speaker)
        assertEquals(SpeakerTint.ROSE, color.tint)
    }

    @Test fun tokenProjectionHandlesCollapsedUnicodeWhitespaceAndAuthorText() {
        val text = "🌙 Свет погас.\n\n— Привет,\u00a0\u00a0— сказала\tНина. — Я\nвернусь."
        // The displayed token text collapses NBSP/tab/newline gaps, but offsets refer to the chapter.
        val begin = text.indexOf("— Привет")
        val end = text.indexOf("Нина.") + "Нина.".length
        val tokens = listOf("—", "Привет,", "—", "сказала", "Нина.")
        val chunk = SpeedChunk(tokens, begin, end, 0)
        val displayed = tokens.joinToString(" ")
        val segments = coloring(text).segments(chunk)
        val hi = displayed.indexOf("Привет")
        assertTrue(segments.any { hi in it.start until it.endExclusive })
        val author = displayed.indexOf("сказала")
        assertTrue(segments.none { author in it.start until it.endExclusive })
        assertTrue(segments.all { it.speaker == "Нина" && it.tint == SpeakerTint.ROSE })
    }

    @Test fun repeatedTokensUseTheirExactSourcePositions() {
        val text = "— Да, — сказала Нина.\n\n— Да, — ответил Марк."
        val begin = text.lastIndexOf("Да,")
        val chunk = SpeedChunk(listOf("Да,"), begin, begin + 3, 0)
        assertEquals("Марк", coloring(text).segments(chunk).single().speaker)
    }

    @Test fun wrongChunkTokensDoNotColorNearbyMatchingWords() {
        val begin = chapter.indexOf("Привет")
        assertTrue(coloring().segments(SpeedChunk(listOf("Пора,"), begin, begin + 5, 0)).isEmpty())
        assertTrue(coloring().segments(SpeedChunk(listOf("Привет,"), begin, begin + 6, 0)).isEmpty())
        assertTrue(coloring().segments(SpeedChunk(listOf("Привет,"), -1, 7, 0)).isEmpty())
    }

    @Test fun textWindowsAfterNonBmpCharactersUseUtf16Offsets() {
        val text = "🌙\n\n— Привет, — сказала Нина."
        val begin = text.indexOf("ивет")
        val end = text.indexOf(",", begin)
        val segment = coloring(text).segments(begin, end).single()
        assertEquals(0, segment.start)
        assertEquals(end - begin, segment.endExclusive)
        assertEquals("Нина", segment.speaker)
    }

    @Test fun oneHyphenRunPartRetainsColorWhileItsConsumedSeparatorStaysOutsideTheDisplay() {
        val runText = "Первый-второй-третий-четвёртый-пятый-шестой-седьмой-восьмой-девятый-десятый."
        val text = "— $runText — сказала Нина."
        val start = text.indexOf("Первый")
        val run = SpeedDialogueHyphenRun.at(text, start)!!
        val chunk = run.chunk(text, start, 1)
        assertEquals("Первый-", text.substring(chunk.startOffset, chunk.endOffset))
        val segment = coloring(text).segments(chunk).single()
        assertEquals("Нина", segment.speaker)
        assertEquals(SpeakerTint.ROSE, segment.tint)
        assertEquals(0, segment.start)
        assertEquals("Первый".length, segment.endExclusive)
    }

    @Test fun fourHyphenRunPartsMapToTheirJoinedDisplayPositions() {
        val runText = "Первый-второй-третий-четвёртый-пятый-шестой-седьмой-восьмой-девятый-десятый."
        val text = "— $runText — сказала Нина."
        val start = text.indexOf("третий")
        val run = SpeedDialogueHyphenRun.at(text, start)!!
        val chunk = run.chunk(text, start, 4)
        val segments = coloring(text).segments(chunk)
        assertEquals(listOf("третий", "четвёртый", "пятый", "шестой"), chunk.tokens)
        assertEquals(4, segments.size)
        val displayed = chunk.tokens.joinToString(" ")
        assertEquals(chunk.tokens, segments.map { displayed.substring(it.start, it.endExclusive) })
        assertTrue(segments.all { it.speaker == "Нина" && it.tint == SpeakerTint.ROSE })
        assertEquals(displayed.length, segments.last().endExclusive)
    }

    @Test fun normalHyphenatedWordsAndAlteredRunOffsetsStillFailClosed() {
        val normal = "— Кто-то постучал, — сказала Нина."
        val start = normal.indexOf("Кто-то")
        assertTrue(coloring(normal).segments(SpeedChunk(listOf("Кто", "то"), start, start + 6, 0)).isEmpty())
        assertTrue(coloring(normal).segments(SpeedChunk(listOf("Кто"), start, start + 4, 0)).isEmpty())
        val runText = "Первый-второй-третий-четвёртый-пятый-шестой-седьмой-восьмой-девятый-десятый."
        val text = "— $runText — сказала Нина."
        val run = SpeedDialogueHyphenRun.at(text, text.indexOf("Первый"))!!
        val chunk = run.chunk(text, text.indexOf("Первый"), 4)
        assertTrue(coloring(text).segments(chunk.copy(tokens = chunk.tokens.reversed())).isEmpty())
        assertTrue(coloring(text).segments(chunk.copy(endOffset = chunk.endOffset + 1)).isEmpty())
    }

    @Test fun builtinVoiceDisplayTokensAndColorsRespectSourceUnicodeWhitespace() {
        val text = "— Привет,\u00a0мир,\u202fмы\u2003вернулись! — сказала Нина.\n\n— Пора,\t— ответил Марк."
        val chunk = SpeedSpeechText.displayChunk(SpeedSpeechText.next(text, 0)!!)
        assertEquals(-1, chunk.focusTokenIndex)
        assertEquals(listOf("—", "Привет,", "мир,", "мы", "вернулись!", "—", "сказала", "Нина.", "—", "Пора,", "—", "ответил", "Марк."), chunk.tokens)
        assertTrue(chunk.tokens.all { token -> token.none(Char::isWhitespace) })
        val displayed = chunk.tokens.joinToString(" ")
        val segments = coloring(text).segments(chunk)
        assertTrue(segments.any { it.speaker == "Нина" && it.tint == SpeakerTint.ROSE })
        assertTrue(segments.any { it.speaker == "Марк" && it.tint == SpeakerTint.GREEN })
        for (word in listOf("сказала", "ответил")) {
            val offset = displayed.indexOf(word)
            assertTrue(segments.none { offset in it.start until it.endExclusive })
        }
    }

}
