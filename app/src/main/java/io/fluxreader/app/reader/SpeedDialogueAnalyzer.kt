package io.fluxreader.app.reader

/** Original chapter offsets are preserved, including author's remarks between speech segments. */
data class SpeedDialogueSpan(
    val start: Int,
    val endExclusive: Int,
    val speaker: String?,
    val continuation: Boolean = false,
) {
    fun contains(offset: Int): Boolean = offset >= start && offset < endExclusive
}

/** Conservative local rules: explicit attribution only; no alternation-based speaker guessing. */
private data class DialogueAttribution(val speaker: String?)

object SpeedDialogueAnalyzer {
    private const val SPACE = "[\\s\\p{Zs}]"
    private const val VERB = "(?:сказал(?:а|и)?|говорил(?:а|и)?|спросил(?:а|и)?|ответил(?:а|и)?|" +
        "произн[её]с(?:ла|ли)?|проговорил(?:а|и)?|шепнул(?:а|и)?|прошептал(?:а|и)?|" +
        "крикнул(?:а|и)?|воскликнул(?:а|и)?|заметил(?:а|и)?|буркнул(?:а|и)?|добавил(?:а|и)?|" +
        "возразил(?:а|и)?|переспросил(?:а|и)?|прошипел(?:а|и)?|продолжил(?:а|и)?|" +
        "продолжал(?:а|и)?|пробормотал(?:а|и)?|промолвил(?:а|и)?|пролепетал(?:а|и)?|" +
        "отозвал(?:ся|ась|ись)|поинтересовал(?:ся|ась|ись)|осведомил(?:ся|ась|ись)|" +
        "пояснил(?:а|и)?|объяснил(?:а|и)?|уточнил(?:а|и)?|повторил(?:а|и)?|подтвердил(?:а|и)?|" +
        "прибавил(?:а|и)?|вставил(?:а|и)?|заключил(?:а|и)?|выпалил(?:а|и)?|прервал(?:а|и)?|" +
        "рявкнул(?:а|и)?|заявил(?:а|и)?|проворчал(?:а|и)?|простонал(?:а|и)?|протянул(?:а|и)?|" +
        "прохрипел(?:а|и)?|прорычал(?:а|и)?|отрезал(?:а|и)?|выдохнул(?:а|и)?)"
    private val speechVerb = Regex("(?<![\\p{L}])$VERB(?![\\p{L}])", RegexOption.IGNORE_CASE)
    private val actionVerb = Regex("(?<![\\p{L}])(?:улыбнул(?:ся|ась)|вздохнул(?:а)?|пожал(?:а)?|" +
        "кивнул(?:а)?|покачал(?:а)?|отвернул(?:ся|ась)|встал(?:а)?|уш[её]л|ушла|" +
        "засмеял(?:ся|ась)|нахмурил(?:ся|ась)|просиял(?:а)?|усмехнул(?:ся|ась))(?![\\p{L}])", RegexOption.IGNORE_CASE)
    // Case sensitivity is deliberate: IGNORE_CASE on this pattern used to accept ordinary nouns.
    private val name = Regex("(?<![\\p{L}])[\\p{Lu}][\\p{Ll}’'-]{1,29}(?:$SPACE+[\\p{Lu}][\\p{Ll}’'-]{1,29}){0,2}")
    private val notNames = setOf("Он", "Она", "Они", "Оно", "Это", "Этот", "Эта", "Один", "Одна", "Кто", "Как", "Что", "Его", "Её", "Ее", "Все", "Всё", "Тот", "Тогда", "Потом", "Затем", "Да", "Нет", "Ну", "Но", "Или", "Не", "Вы", "Ты", "Их", "Мой", "Моя", "Наш", "Наша", "Сам", "Наконец", "После", "Через", "На", "Взяв", "Покачав", "Подумав", "Оставшись", "Понизив", "Увидев", "Поправив", "Пожав", "Подняв", "Опустившись", "Неожиданно", "Вскоре", "Однажды", "Наутро", "Примерно", "Немного", "Очень", "Совершенно", "Тихо", "Стараясь", "Замолчав", "Помолчав", "Выходя", "Оторвавшись", "Сдержав", "Придав", "Наладив", "Сев", "Голос", "Губы", "Дрожащей", "Резким", "Старческий")
    private val attributionAdverbs = setOf("тихо", "громко", "коротко", "спокойно", "сухо", "снова", "вновь", "неожиданно", "наконец", "сразу", "быстро", "вдруг", "устало", "тяжело", "решительно", "задумчиво", "насмешливо", "лукаво", "мягко", "презрительно", "скептически", "довольно", "резко")
    private val obliqueName = Regex("(?iu).*(?:ого|его|ому|ему|ыми|ими|ых|их|у|ю|е)$")
    private val words = Regex("[^\\s\\p{Zs}]+")
    private val quotePairs = listOf('«' to '»', '“' to '”', '"' to '"')
    // Only small paragraph strings are retained, never a chapter/book. Synchronised for UI/IO use.
    private val cache = LinkedHashMap<Pair<String, String>, List<SpeedDialogueSpan>>(16, .75f, true)

    /** Linear chapter pass; used off the playback thread. Never changes original offsets. */
    fun spans(text: String, checkpoint: () -> Unit = {}): List<SpeedDialogueSpan> {
        val result = ArrayList<SpeedDialogueSpan>()
        var begin = 0
        var prefix = ""
        while (begin < text.length) {
            checkpoint()
            val end = text.indexOf('\n', begin).let { if (it < 0) text.length else it }
            val line = text.substring(begin, end)
            if (line.length <= 4096 && line.isNotBlank()) {
                result += parse(line, prefix).map { it.copy(start = it.start + begin, endExclusive = it.endExclusive + begin) }
            }
            if (line.isNotBlank()) prefix = line.takeLast(512).trimEnd()
            begin = end + 1
        }
        return result
    }

    fun at(text: String, offset: Int): SpeedDialogueSpan? {
        if (offset !in text.indices) return null
        var begin = offset
        while (begin > 0 && offset - begin < 4096 && text[begin - 1] != '\n') begin--
        if (begin > 0 && text[begin - 1] != '\n') return null
        var end = offset
        while (end < text.length && end - offset < 4096 && text[end] != '\n') end++
        if (end < text.length && text[end] != '\n' || end - begin > 4096) return null
        // The preceding paragraph may explicitly introduce a reply ("Анна спросила:").
        val prefix = text.substring((begin - 512).coerceAtLeast(0), begin).trimEnd().substringAfterLast('\n')
        val key = text.substring(begin, end) to prefix
        val spans = synchronized(cache) {
            cache.getOrPut(key) { parse(key.first, key.second) }.also {
                if (cache.size > 12) cache.remove(cache.keys.first())
            }
        }
        return spans.firstOrNull { it.contains(offset - begin) }?.let {
            it.copy(start = it.start + begin, endExclusive = it.endExclusive + begin)
        }
    }

    private fun parse(line: String, prefix: String): List<SpeedDialogueSpan> {
        val first = line.indexOfFirst { !it.isWhitespace() }
        if (first < 0) return emptyList()
        if (line[first] in "—–-" && first + 1 < line.length && (line[first + 1].isWhitespace() || line[first] in "—–" && line[first + 1].isLetter())) {
            val body = line.substring(first + 1).trimStart()
            // Numeric/list bullets and formula fragments are not character dialogue.
            if (body.none(Char::isLetterOrDigit)) return emptyList()
            val intro = prefix.takeIf { it.endsWith(':') && !it.trimStart().startsWithAnyDash() }
                ?.let { attribution(it.takeLast(180), preferLast = true) }
            val segments = speechSegments(line, first, line.length, intro?.speaker)
            val hasRemark = segments.any { it.endExclusive < line.length || it.continuation }
            if ((body.firstOrNull()?.isDigit() == true || body.firstOrNull()?.isLowerCase() == true) &&
                intro == null && !hasRemark) return emptyList()
            return segments
        }
        val result = mutableListOf<SpeedDialogueSpan>()
        for ((open, close) in quotePairs) {
            var pos = line.indexOf(open)
            while (pos >= 0) {
                val finish = line.indexOf(close, pos + 1)
                if (finish < 0) break
                if (result.none { it.contains(pos) }) {
                    val before = line.substring(0, pos).trimEnd().takeLast(180)
                    val after = line.substring(finish + 1).trimStart().take(180)
                    val preceding = before.takeIf { it.endsWith(':') }?.let { attribution(it, preferLast = true) }
                    val following = after.takeIf { it.startsWith(',') || it.startsWithAnyDash() }
                        ?.trimStart(',', ' ', '\u00a0', '—', '–', '-')?.let { attribution(it) }
                    val internal = (pos + 1 until finish).firstNotNullOfOrNull { index ->
                        if (line[index] in "—–-" && (line[index - 1].isWhitespace() || line[index - 1] in ",.!?…")) {
                            val remark = line.substring(index + 1, finish).substringBefore('—').substringBefore('–')
                            attribution(remark.take(220), allowAction = true)
                        } else null
                    }
                    val attr = preceding ?: following ?: internal
                    if (attr != null) {
                        result += speechSegments(line, pos, finish + 1, attr.speaker)
                    }
                }
                pos = line.indexOf(open, finish + 1)
            }
        }
        return result.sortedBy { it.start }
    }

    private fun String.startsWithAnyDash() = firstOrNull() in listOf('—', '–', '-')

    private fun speechSegments(line: String, start: Int, end: Int, intro: String?): List<SpeedDialogueSpan> {
        val dashes = (start + 1 until end).filter { i ->
            line[i] in "—–-" && (line[i - 1].isWhitespace() || line[i - 1] in ",.!?…»\"") &&
                (i + 1 == end || line[i + 1].isWhitespace() || line[i] in "—–" || line[i - 1] in ",.!?…")
        }
        val ranges = mutableListOf<Pair<Int, Int>>()
        var current = start
        var speaker = intro
        var index = 0
        while (index < dashes.size) {
            val dash = dashes[index]
            val next = dashes.getOrNull(index + 1) ?: end
            val author = attribution(line.substring(dash + 1, next).take(220), allowAction = true)
            if (author != null) {
                if (dash > current) ranges += current to dash
                if (speaker == null) speaker = author.speaker
                current = if (next < end) next + 1 else end
                index += 2
            } else index++
        }
        if (current < end) ranges += current to end
        return ranges.filter { (a, b) -> line.substring(a, b).any(Char::isLetterOrDigit) }
            .mapIndexed { i, (a, b) -> SpeedDialogueSpan(a, b, speaker, continuation = i > 0) }
    }

    private fun attribution(raw: String, allowAction: Boolean = false, preferLast: Boolean = false): DialogueAttribution? {
        val text = raw.trim()
        val verb = (if (preferLast) speechVerb.findAll(text).lastOrNull() else speechVerb.find(text)) ?: if (allowAction) actionVerb.find(text) else null
        if (verb == null || verb.range.first > 120) return null
        val before = text.substring(0, verb.range.first).let { if (preferLast) it.substringAfterLast('»').substringAfterLast('”').substringAfterLast('!').substringAfterLast('?').substringAfterLast('.') else it }
        // Do not reach into the next narrative sentence looking for any speech verb.
        if (before.any { it in ".!?…:;" } || words.findAll(before).take(17).count() > 16) return null
        val after = text.substring(verb.range.last + 1)
        // A colon after indirect speech/list prose does not introduce the verb's own words.
        if (preferLast && Regex("(?iu)\\b(?:что|чтобы|котор\\p{L}*|есть|два|три)\\b").containsMatchIn(after)) return null
        val preceding = name.findAll(before).lastOrNull()
        val between = preceding?.let { before.substring(it.range.last + 1).trim() }.orEmpty()
        val beforeName = preceding?.takeIf {
            between.isEmpty() || words.findAll(between).all { word -> word.value.lowercase() in attributionAdverbs }
        }?.value?.let(::cleanName)
        // Adverbs and object pronouns can separate the speech verb and its subject:
        // «ответил спокойно Павел», «спросил его Зурита». Never skip subject pronouns.
        val afterWords = words.findAll(after).toList()
        val subjectStart = afterWords.firstOrNull { word ->
            val value = word.value.trimEnd(',').lowercase()
            value !in attributionAdverbs && value !in setOf("его", "её", "ее", "их", "меня", "тебя", "нас", "вас", "профессор", "капитан", "доктор", "мистер", "мисс", "миссис", "сэр", "господин", "госпожа")
        }?.range?.first ?: after.length
        val subject = after.substring(subjectStart)
        val afterName = name.find(subject)?.takeIf { it.range.first == 0 }?.value?.let(::cleanName)
        return DialogueAttribution(beforeName ?: afterName)
    }

    private fun cleanName(raw: String): String? {
        val parts = raw.split(Regex(SPACE + "+")).dropWhile { it in notNames || it.lowercase() in attributionAdverbs }
        if (parts.isEmpty() || parts.first() in notNames || obliqueName.matches(parts.first())) return null
        // Stop before an explicitly declined addressee; do not glue it to the speaker's name.
        return parts.takeWhile { it !in notNames && !obliqueName.matches(it) }.joinToString(" ").takeIf { it.isNotBlank() }
    }

    fun shortReplyWordCount(text: String, offset: Int, limit: Int = 6): Int? {
        val span = at(text, offset) ?: return null
        if (span.continuation || offset != span.start && offset != SpeedDialoguePlayback.readableOffset(text, span.start)) return null
        val reply = text.substring(SpeedDialoguePlayback.readableOffset(text, span.start), span.endExclusive).trim()
        val tokens = words.findAll(reply).map { it.value }.take(limit + 1).toList()
        if (tokens.size !in 1..limit || tokens.last().lastOrNull() !in listOf('.', '!', '?', '…', '»', '”', '"')) return null
        return tokens.size
    }

    fun isReplyStart(text: String, offset: Int): Boolean {
        val span = at(text, offset) ?: return false
        return !span.continuation && (offset == span.start || offset == SpeedDialoguePlayback.readableOffset(text, span.start))
    }
}
