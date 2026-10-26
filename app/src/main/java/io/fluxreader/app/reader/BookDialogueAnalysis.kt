package io.fluxreader.app.reader

import java.security.MessageDigest

enum class DialogueEvidence { EXPLICIT, SCENE_INTRODUCTION, PRONOUN_ANTECEDENT, SCENE_PRONOUN, GRAMMATICAL_SUBJECT, NARRATIVE_CONTINUATION, AFFECTED_TOPIC, SELF_INTRODUCTION }

enum class DialogueGender { MASCULINE, FEMININE }

data class IndexedDialogue(val span: SpeedDialogueSpan, val evidence: DialogueEvidence = DialogueEvidence.EXPLICIT)

/** Immutable lookup for one chapter, tied to the exact original text. */
class ChapterDialogueIndex(val textFingerprint: String, val entries: List<IndexedDialogue>) {
    fun at(offset: Int): SpeedDialogueSpan? {
        var low = 0
        var high = entries.lastIndex
        while (low <= high) {
            val mid = (low + high).ushr(1)
            val span = entries[mid].span
            when {
                offset < span.start -> high = mid - 1
                offset >= span.endExclusive -> low = mid + 1
                else -> return span
            }
        }
        return null
    }

    fun matches(text: String) = textFingerprint == fingerprint(text)

    companion object {
        fun fingerprint(text: String): String = MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }
}

/** Only names observed in explicit attributions enter the roster. Name merging needs morphology. */
class DialogueCharacterRoster(
    names: Collection<String>,
    genderHints: Map<String, Set<DialogueGender>> = emptyMap(),
) {
    val names = names.filter { it.length in 2..100 }.distinct().sorted().take(2048)
    private val genders = this.names.mapNotNull { name ->
        genderHints[name]?.singleOrNull()?.let { name to it }
    }.toMap()
    val fingerprint = ChapterDialogueIndex.fingerprint(this.names.joinToString("\n") { "$it:${genders[it]}" })
    internal fun knownForms(): Set<String> = names.toSet()
    internal fun gender(name: String): DialogueGender? = genders[name]

    /** Bounded streaming collection: conflicting gender evidence is retained as ambiguity. */
    class Builder {
        private val names = linkedSetOf<String>()
        private val genders = mutableMapOf<String, MutableSet<DialogueGender>>()
        fun observe(text: String, index: ChapterDialogueIndex) {
            for ((position, entry) in index.entries.withIndex()) {
                val speaker = entry.span.speaker ?: continue
                if (names.size >= 2048 && speaker !in names) continue
                names += speaker
                val end = text.indexOf('\n', entry.span.endExclusive).let { if (it < 0) text.length else it }
                val next = index.entries.getOrNull(position + 1)?.span?.start ?: end
                val remarkEnd = minOf(end, next)
                if (remarkEnd - entry.span.endExclusive !in 1..512) continue
                val remark = text.substring(entry.span.endExclusive, remarkEnd)
                SpeedDialogueAnalyzer.authorGender(remark, speaker)?.let {
                    genders.getOrPut(speaker) { linkedSetOf() } += it
                }
            }
        }
        fun build() = DialogueCharacterRoster(names, genders)
        val size: Int get() = names.size
    }
}

/**
 * Whole-book roster, scene-local evidence. We deliberately do not carry a speaker over arbitrary
 * paragraphs or alternate A/B: entrances, groups, vocatives and scene changes make that unsafe.
 */
object BookDialogueAnalyzer {
    private val speechCue = SpeedDialogueAnalyzer.speechVerb
    private val orderCue = Regex("(?iu)(?<![\\p{L}])призвал(?:а)?[\\s\\p{Zs}]+(?:всех[\\s\\p{Zs}]+)?к[\\s\\p{Zs}]+тишине(?![\\p{L}])")
    private val safeBridge = Regex("(?iu)^(?:(?:затем|снова|вновь|тихо|громко|быстро|вдруг|наконец|спокойно|неожиданно|сразу|решительно)[\\s\\p{Zs}]*)*$")
    private val mannerBridge = Regex("(?u)^со?[\\s\\p{Zs}]+(?:[\\p{Ll}]+[\\s\\p{Zs}]+){0,2}(?:скоростью|улыбкой|вздохом|усмешкой|трудом)$", RegexOption.IGNORE_CASE)
    private val indirect = Regex("(?iu)(?<![\\p{L}])(?:что|чтобы|будто|как|если)(?![\\p{L}])")
    private val title = Regex("(?iu)^(?:профессор|мистер|миссис|мисс|доктор|капитан|сэр)[\\s\\p{Zs}]+$")

    fun local(text: String, checkpoint: () -> Unit = {}): ChapterDialogueIndex = ChapterDialogueIndex(
        ChapterDialogueIndex.fingerprint(text), SpeedDialogueAnalyzer.spans(text, checkpoint).map(::IndexedDialogue),
    )

    /** Chapter-local names and grammatical genders are available without scanning the whole book. */
    fun chapter(text: String, checkpoint: () -> Unit = {}): ChapterDialogueIndex {
        val local = local(text, checkpoint)
        checkpoint()
        val roster = DialogueCharacterRoster.Builder().apply { observe(text, local) }.build()
        return enrich(text, local, roster, checkpoint)
    }

    fun enrich(text: String, local: ChapterDialogueIndex, roster: DialogueCharacterRoster, checkpoint: () -> Unit = {}): ChapterDialogueIndex {
        require(local.matches(text)) { "Dialogue offsets belong to different text" }
        val forms = roster.knownForms()
        // Match a bounded token sequence, never a substring of another name or a declined form.
        val mentions = if (forms.isEmpty()) null else Regex(
            "(?<![\\p{L}’'-])(?:" + forms.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) } + ")(?![\\p{L}’'-])",
        )
        val nameForms = DialogueNameForms(roster)
        val scene = DialogueScene(roster, nameForms)
        var paragraph = -1
        var introduction: String? = null
        var pronounSpeaker: String? = null
        var pronounEvidence = DialogueEvidence.PRONOUN_ANTECEDENT
        var roleSpeaker: String? = null
        var roleAmbiguous = false
        val result = local.entries.mapIndexed { position, entry ->
            checkpoint()
            val span = entry.span
            val paragraphEnd = text.indexOf('\n', span.start).let { if (it < 0) text.length else it }
            val begin = text.lastIndexOf('\n', (span.start - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
            if (begin != paragraph) {
                paragraph = begin
                var previousEnd = begin
                while (previousEnd > 0 && text[previousEnd - 1].isWhitespace()) previousEnd--
                val previousStart = text.lastIndexOf('\n', previousEnd - 1) + 1
                val prefix = if (previousEnd - previousStart <= 4096) text.substring(previousStart, previousEnd) else ""
                introduction = if (mentions == null) null else introducedSpeaker(prefix, mentions, roster)
                scene.advance(text, begin, checkpoint)
            }
            if (!span.continuation) {
                pronounSpeaker = null; roleSpeaker = null; roleAmbiguous = false
                var previousEnd = begin
                while (previousEnd > 0 && text[previousEnd - 1].isWhitespace()) previousEnd--
                val previousStart = text.lastIndexOf('\n', previousEnd - 1) + 1
                val prefix = if (previousEnd - previousStart <= 4096) text.substring(previousStart, previousEnd) else ""
                val next = local.entries.getOrNull(position + 1)?.span?.start ?: paragraphEnd
                val remarkEnd = minOf(next, paragraphEnd)
                if (remarkEnd - span.endExclusive in 1..512) {
                    val remark = text.substring(span.endExclusive, remarkEnd)
                    val roles = RussianDialogueRoles.parse(remark, nameForms)
                    roleSpeaker = roles.speaker
                    roleAmbiguous = roles.ambiguous
                    val pronoun = roles.pronoun ?: SpeedDialogueAnalyzer.authorPronoun(remark)
                    if (pronoun != null) {
                        pronounSpeaker = if (mentions == null) null else pronounAntecedent(prefix, pronoun, mentions, roster)
                        if (pronounSpeaker in roles.addressees || pronounSpeaker in scene.departedNames(prefix) ||
                            nameForms.mentions(prefix).map { it.name }.distinct().size > 1) pronounSpeaker = null
                        pronounEvidence = DialogueEvidence.PRONOUN_ANTECEDENT
                        if (pronounSpeaker == null) {
                            pronounSpeaker = DialogueAdditionalEvidence.affectedTopic(prefix, pronoun, roster, nameForms, roles.addressees)
                                ?.takeUnless { it in scene.departedNames(prefix) || scene.isDeparted(it) }
                            if (pronounSpeaker != null) pronounEvidence = DialogueEvidence.AFFECTED_TOPIC
                        }
                        if (pronounSpeaker == null) {
                            pronounSpeaker = scene.resolve(pronoun, roles.addressees)
                            pronounEvidence = DialogueEvidence.SCENE_PRONOUN
                        }
                    }
                }
            }
            val resolved = when {
                roleAmbiguous -> entry.copy(span = span.copy(speaker = null), evidence = DialogueEvidence.GRAMMATICAL_SUBJECT)
                roleSpeaker != null && span.speaker != roleSpeaker -> entry.copy(span = span.copy(speaker = roleSpeaker), evidence = DialogueEvidence.GRAMMATICAL_SUBJECT)
                pronounSpeaker != null -> entry.copy(span = span.copy(speaker = pronounSpeaker), evidence = pronounEvidence)
                span.speaker != null -> entry
                span.continuation || span.endExclusive < paragraphEnd || introduction == null -> entry
                else -> entry.copy(span = span.copy(speaker = introduction), evidence = DialogueEvidence.SCENE_INTRODUCTION)
            }
            resolved.span.speaker?.let { scene.record(it, span.start) }
            resolved
        }
        val selfNamed = DialogueAdditionalEvidence.selfIntroductions(text, result, checkpoint)
        return ChapterDialogueIndex(local.textFingerprint, resumeInterruptedNarration(text, selfNamed, nameForms, checkpoint))
    }

    /** An ellipsis can resume a monologue only with matching explicit anchors on both sides
     * and a single intervening narrative paragraph referring to that person's words. */
    private fun resumeInterruptedNarration(
        text: String, entries: List<IndexedDialogue>, forms: DialogueNameForms, checkpoint: () -> Unit,
    ): List<IndexedDialogue> {
        val speechReference = Regex("(?iu)(?<![\\p{L}])(?:слов(?:а|ами)?|реч(?:ь|и)|рассказ(?:а)?|объяснени(?:я|й)|реплик(?:а|и)|фраз(?:а|ы))(?:[\\s\\p{Zs}]+(?:профессора|доктора|капитана))?[\\s\\p{Zs}]+$")
        val sceneChange = Regex("(?iu)^(?:наутро|на следующий день|тем временем|позже|через|вечером|глава)\\b")
        val continuationStart = Regex("^[—–-]?[\\s\\p{Zs}]*(?:…|\\.{3})[\\s\\p{Zs}]*[\\p{Ll}]")
        return entries.mapIndexed { i, entry ->
            checkpoint()
            val span = entry.span
            val previous = entries.getOrNull(i - 1)
            val next = entries.getOrNull(i + 1)
            val speaker = previous?.span?.speaker
            if (span.speaker != null || span.continuation || speaker == null || next?.span?.speaker != speaker ||
                next.evidence !in setOf(DialogueEvidence.EXPLICIT, DialogueEvidence.GRAMMATICAL_SUBJECT) ||
                previous.evidence == DialogueEvidence.NARRATIVE_CONTINUATION ||
                span.start - previous.span.endExclusive !in 1..1400 || next.span.start - span.endExclusive !in 1..1600 ||
                !continuationStart.containsMatchIn(text.substring(span.start, minOf(span.endExclusive, span.start + 30)))) return@mapIndexed entry
            val precedingSpeech = text.substring(previous.span.start, previous.span.endExclusive).trimEnd().trimEnd('»', '”', '"')
            if (!precedingSpeech.endsWith('…') && !precedingSpeech.endsWith("...")) return@mapIndexed entry
            val gap = text.substring(previous.span.endExclusive, span.start)
            val narration = gap.lineSequence().map(String::trim).filter(String::isNotEmpty).toList().singleOrNull()
                ?: return@mapIndexed entry
            if (narration.any { it in "«»\"“”" } || narration.first() in "—–-*" || sceneChange.containsMatchIn(narration)) return@mapIndexed entry
            val sources = forms.mentions(narration).filter { mention ->
                speechReference.containsMatchIn(narration.substring(0, mention.start))
            }.map { it.name }.distinct()
            if (sources.singleOrNull() == speaker) entry.copy(span = span.copy(speaker = speaker), evidence = DialogueEvidence.NARRATIVE_CONTINUATION)
            else entry
        }
    }

    private fun pronounAntecedent(prefix: String, pronoun: String, mentions: Regex, roster: DialogueCharacterRoster): String? {
        val line = prefix.trim()
        if (line.isBlank() || line.first() in "—–-«\"“" || line.any { it in "«»\"“”" }) return null
        // One immediately preceding narrative paragraph, one known character. No search through
        // earlier scenes, inferred turn-taking, or choosing the nearest name among several people.
        val found = mentions.findAll(line).toList()
        val candidate = found.map { it.value }.distinct().singleOrNull() ?: return null
        // A sole name used as an object ("В комнате ждали Анну") is not a subject anchor.
        val leading = line.substring(0, found.first().range.first)
        if (leading.isNotBlank() && !title.matches(leading)) return null
        val expected = if (pronoun == "она") DialogueGender.FEMININE else DialogueGender.MASCULINE
        return candidate.takeIf { roster.gender(it) == expected }
    }

    private fun introducedSpeaker(prefix: String, mentions: Regex, roster: DialogueCharacterRoster): String? {
        var line = prefix.trim()
        if (line.isEmpty() || line.first() in "«\"“" || line.any { it in "«»\"“”" }) return null
        if (line.first() in "—–-") {
            // A preceding reply may end in narration introducing a different speaker.
            // Only its author's suffix can supply evidence; never the quoted words themselves.
            val speechEnd = SpeedDialogueAnalyzer.spans(line).lastOrNull()?.endExclusive ?: return null
            if (speechEnd >= line.length) return null
            line = line.substring(speechEnd).trimStart('—', '–', '-', ' ', '\u00a0', '\u202f')
        }
        // An explicit colon, or a narrowly recognised call for silence, introduces the next reply.
        val content = line.trimEnd(':', '.', '!', '?', '…', ' ')
        val sentence = content.substringAfterLast('.').substringAfterLast('!').substringAfterLast('?')
        val order = orderCue.findAll(sentence).lastOrNull()
        val cue = if (line.endsWith(':')) speechCue.findAll(sentence).lastOrNull() ?: order else order
        cue ?: return null
        val after = sentence.substring(cue.range.last + 1).trim()
        if (indirect.containsMatchIn(after)) return null
        val before = sentence.substring(0, cue.range.first)
        val subject = mentions.findAll(before).lastOrNull()
        if (subject != null) {
            val bridge = before.substring(subject.range.last + 1).trim()
            val preceding = before.substring(0, subject.range.first).trimEnd()
                .split(Regex("[\\s\\p{Zs}]+")).lastOrNull().orEmpty().lowercase()
            if (preceding in setOf("к", "ко", "у", "на", "за", "от", "для", "про", "об", "словам", "голосу", "вслед")) return null
            if (safeBridge.matches(bridge) || mannerBridge.matches(bridge)) return subject.value
            if (order != null && bridge == "и" && preceding in setOf("вошел", "вошёл", "вошла", "появился", "появилась")) {
                return subject.value
            }
        }
        // Inverted attribution: «Затараторила Гермиона:». No search across objects or clauses.
        val afterName = mentions.find(after) ?: return null
        val bridge = after.substring(0, afterName.range.first)
        if (bridge.isNotBlank() && !title.matches(bridge)) return null
        val tail = after.substring(afterName.range.last + 1).trim()
        return if (tail.isBlank()) afterName.value else null
    }
}
