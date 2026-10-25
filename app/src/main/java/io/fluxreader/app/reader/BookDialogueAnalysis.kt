package io.fluxreader.app.reader

import java.security.MessageDigest

enum class DialogueEvidence { EXPLICIT, SCENE_INTRODUCTION }

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
class DialogueCharacterRoster(names: Collection<String>) {
    val names = names.filter { it.length in 2..100 }.distinct().sorted().take(2048)
    val fingerprint = ChapterDialogueIndex.fingerprint(this.names.joinToString("\n"))
    internal fun knownForms(): Set<String> = names.toSet()
}

/**
 * Whole-book roster, scene-local evidence. We deliberately do not carry a speaker over arbitrary
 * paragraphs or alternate A/B: entrances, groups, vocatives and scene changes make that unsafe.
 */
object BookDialogueAnalyzer {
    private val speechCue = Regex("(?iu)(?<![\\p{L}])(?:сказал(?:а)?|спросил(?:а)?|ответил(?:а)?|" +
        "произн[её]с(?:ла)?|проговорил(?:а)?|прошептал(?:а)?|крикнул(?:а)?|воскликнул(?:а)?|" +
        "затараторил(?:а)?|заговорил(?:а)?|начал(?:а)?|продолжил(?:а)?|объявил(?:а)?|" +
        "скомандовал(?:а)?|позвал(?:а)?)(?![\\p{L}])")
    private val orderCue = Regex("(?iu)(?<![\\p{L}])призвал(?:а)?[\\s\\p{Zs}]+(?:всех[\\s\\p{Zs}]+)?к[\\s\\p{Zs}]+тишине(?![\\p{L}])")
    private val safeBridge = Regex("(?iu)^(?:(?:затем|снова|вновь|тихо|громко|быстро|вдруг|наконец|спокойно|неожиданно|сразу|решительно)[\\s\\p{Zs}]*)*$")
    private val mannerBridge = Regex("(?u)^со?[\\s\\p{Zs}]+(?:[\\p{Ll}]+[\\s\\p{Zs}]+){0,2}(?:скоростью|улыбкой|вздохом|усмешкой|трудом)$", RegexOption.IGNORE_CASE)
    private val indirect = Regex("(?iu)(?<![\\p{L}])(?:что|чтобы|будто|как|если)(?![\\p{L}])")
    private val title = Regex("(?iu)^(?:профессор|мистер|миссис|мисс|доктор|капитан|сэр)[\\s\\p{Zs}]+$")

    fun local(text: String, checkpoint: () -> Unit = {}): ChapterDialogueIndex = ChapterDialogueIndex(
        ChapterDialogueIndex.fingerprint(text), SpeedDialogueAnalyzer.spans(text, checkpoint).map(::IndexedDialogue),
    )

    fun enrich(text: String, local: ChapterDialogueIndex, roster: DialogueCharacterRoster, checkpoint: () -> Unit = {}): ChapterDialogueIndex {
        require(local.matches(text)) { "Dialogue offsets belong to different text" }
        val forms = roster.knownForms()
        // Match a bounded token sequence, never a substring of another name or a declined form.
        val mentions = if (forms.isEmpty()) null else Regex(
            "(?<![\\p{L}’'-])(?:" + forms.sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) } + ")(?![\\p{L}’'-])",
        )
        var paragraph = -1
        var introduction: String? = null
        val result = local.entries.map { entry ->
            checkpoint()
            val span = entry.span
            if (span.speaker != null) {
                entry
            } else {
                val paragraphEnd = text.indexOf('\n', span.start).let { if (it < 0) text.length else it }
                // An unresolved author's remark may introduce another speaker (e.g. «сказал он»).
                // Do not override it using the preceding narrative introduction.
                if (span.continuation || span.endExclusive < paragraphEnd) return@map entry
                val begin = text.lastIndexOf('\n', (span.start - 1).coerceAtLeast(0)).let { if (it < 0) 0 else it + 1 }
                if (begin != paragraph) {
                    paragraph = begin
                    var previousEnd = begin
                    while (previousEnd > 0 && text[previousEnd - 1].isWhitespace()) previousEnd--
                    val previousStart = text.lastIndexOf('\n', previousEnd - 1) + 1
                    val prefix = if (previousEnd - previousStart <= 4096) text.substring(previousStart, previousEnd) else ""
                    introduction = if (mentions == null) null else introducedSpeaker(prefix, mentions, roster)
                }
                if (introduction == null) entry else entry.copy(
                    span = span.copy(speaker = introduction), evidence = DialogueEvidence.SCENE_INTRODUCTION,
                )
            }
        }
        return ChapterDialogueIndex(local.textFingerprint, result)
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
