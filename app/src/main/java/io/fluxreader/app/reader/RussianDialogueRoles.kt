package io.fluxreader.app.reader

/** Bounded Russian name forms, not a general morphological or syntactic parser. */
internal class DialogueNameForms(private val roster: DialogueCharacterRoster) {
    data class Mention(val name: String, val nominative: Boolean, val start: Int, val end: Int)
    private val forms = mutableMapOf<String, MutableSet<Pair<String, Boolean>>>()
    private val fullNames = roster.names.filter { ' ' in it }.sortedByDescending { it.length }
        .joinToString("|") { name -> name.split(' ').joinToString("[\\s\\p{Zs}]+") { Regex.escape(it) } }
    private val token = Regex("(?<![\\p{L}])(?:" +
        (if (fullNames.isEmpty()) "" else "$fullNames|") + "[\\p{Lu}][\\p{Ll}’'-]{1,29})(?![\\p{L}])")
    init {
        for (name in roster.names) {
            add(name, name, true)
            // Keep an explicitly observed full name atomic. Its parts are not aliases.
            if (' ' in name) continue
            val stem = name.dropLast(1)
            val variants = when {
                name.endsWith("а") -> listOf(stem + if (stem.last() in "гкхжчшщ") "и" else "ы", stem + "е", stem + "у", stem + "ой", stem + "ою")
                name.endsWith("я") -> listOf(stem + "и", stem + if (name.endsWith("ия")) "и" else "е", stem + "ю", stem + "ей", stem + "ею")
                roster.gender(name) != DialogueGender.MASCULINE -> emptyList()
                name.endsWith("й") || name.endsWith("ь") -> listOf(stem + "я", stem + "ю", stem + "ем", stem + "е")
                name.last() in "бвгджзклмнпрстфхцчшщ" -> listOf(name + "а", name + "у", name + "ом", name + "е")
                else -> emptyList()
            }
            for (variant in variants) add(variant, name, false)
        }
    }
    private fun add(form: String, name: String, nominative: Boolean) { forms.getOrPut(form) { linkedSetOf() } += name to nominative }
    fun mentions(text: String): List<Mention> = token.findAll(text).mapNotNull { match ->
        val candidates = forms[match.value.replace(Regex("[\\s\\p{Zs}]+"), " ")].orEmpty()
        val names = candidates.map { it.first }.distinct()
        if (names.size != 1) null else Mention(names.single(), candidates.any { it.second }, match.range.first, match.range.last + 1)
    }.toList()
}

internal data class DialogueRoles(val speaker: String? = null, val pronoun: String? = null,
    val addressees: Set<String> = emptySet(), val ambiguous: Boolean = false)

internal object RussianDialogueRoles {
    private val words = Regex("[\\p{L}’'-]+")
    private val objects = setOf("к", "ко", "у", "с", "со", "для", "от", "про", "об", "перед", "на", "за", "в", "во", "через", "вместо")
    private val objectPronouns = setOf("ему", "ей", "мне", "нам", "тебе", "вам", "им", "меня", "тебя", "его", "её", "ее")
    private val titles = setOf("профессор", "доктор", "мистер", "миссис", "мисс", "сэр", "капитан", "господин", "госпожа")
    fun parse(raw: String, forms: DialogueNameForms): DialogueRoles {
        val text = raw.trim().trimStart('—', '–', '-', ' ', '\u00a0', '\u202f')
        val verb = SpeedDialogueAnalyzer.attributionVerb(text) ?: return DialogueRoles()
        if (verb.range.first > 120) return DialogueRoles()
        val end = text.indexOfAny(charArrayOf(',', '.', '!', '?', '…', ';', '('), verb.range.last + 1).let { if (it < 0) text.length else it }
        val clause = text.substring(0, end)
        val mentions = forms.mentions(clause)
        val excluded = linkedSetOf<String>()
        val before = mutableListOf<String>(); val after = mutableListOf<String>()
        var previousAfter: DialogueNameForms.Mention? = null
        var previousWasObject = false
        for (mention in mentions) {
            val preceding = words.findAll(clause.substring(0, mention.start)).lastOrNull()?.value?.lowercase()
            if (!mention.nominative || preceding in objects) {
                excluded += mention.name
                if (mention.start > verb.range.last) { previousAfter = mention; previousWasObject = true }
                continue
            }
            if (mention.end <= verb.range.first) {
                val bridge = words.findAll(clause.substring(mention.end, verb.range.first)).map { it.value.lowercase() }.toList()
                val leading = words.findAll(clause.substring(0, mention.start)).map { it.value.lowercase() }.toList()
                if (bridge.all { it in SpeedDialogueAnalyzer.attributionAdverbs } && leading.all { it in titles }) before += mention.name
            } else if (mention.start > verb.range.last) {
                val bridge = clause.substring(previousAfter?.end ?: (verb.range.last + 1), mention.start).trim().lowercase()
                val bridgeWords = words.findAll(bridge).map { it.value }.toList()
                when {
                    after.isEmpty() && bridgeWords.all { it in SpeedDialogueAnalyzer.attributionAdverbs || it in titles || it in objectPronouns } -> after += mention.name
                    previousWasObject -> excluded += mention.name
                    after.isNotEmpty() && (bridge.isEmpty() || bridge in setOf("и", "или")) -> after += mention.name
                }
                previousAfter = mention; previousWasObject = mention.name in excluded
            }
        }
        val tail = words.findAll(clause.substring(verb.range.last + 1)).map { it.value.lowercase() }
            .dropWhile { it in SpeedDialogueAnalyzer.attributionAdverbs || it in titles }.firstOrNull()
        val head = words.findAll(clause.substring(0, verb.range.first)).map { it.value.lowercase() }.toList()
        val pronoun = tail?.takeIf { it in setOf("он", "она") }
            ?: head.firstOrNull()?.takeIf { it in setOf("он", "она") && head.drop(1).all { w -> w in SpeedDialogueAnalyzer.attributionAdverbs } }
        if (pronoun != null) return DialogueRoles(pronoun = pronoun,
            addressees = excluded + mentions.filter { it.start > verb.range.last }.map { it.name })
        val candidates = (if (before.isNotEmpty()) before else after).distinct()
        return DialogueRoles(speaker = candidates.singleOrNull(), addressees = excluded, ambiguous = candidates.size > 1)
    }
}

/** Names in quoted words never enter this bounded discourse state. */
internal class DialogueScene(private val roster: DialogueCharacterRoster, private val forms: DialogueNameForms) {
    private data class Seen(val name: String, val paragraph: Int, val offset: Int)
    private val seen = mutableMapOf<String, Seen>()
    private var cursor = 0
    private var paragraph = 0
    private var blankLines = 0
    private val breakWords = Regex("(?iu)^(?:наутро|на следующий день|тем временем|позже|через несколько часов|вечером|глава[\\s\\p{Zs}]+[0-9]+)\\b")
    fun advance(text: String, end: Int, checkpoint: () -> Unit = {}) {
        while (cursor < end) {
            checkpoint()
            val next = text.indexOf('\n', cursor).let { if (it < 0 || it > end) end else it }
            val line = text.substring(cursor, next).trim()
            if (line.isEmpty()) {
                if (++blankLines >= 3) seen.clear()
            } else {
                blankLines = 0; paragraph++
                if (line.matches(Regex("(?:\\*[\\s\\p{Zs}]*){3,}|[—–-]{3,}")) || breakWords.containsMatchIn(line)) seen.clear()
                if (line.length <= 4096 && line.first() !in "—–-«\"“" && line.none { it in "«»\"“”" }) {
                    val mentions = forms.mentions(line)
                    val head = mentions.firstOrNull { mention ->
                        mention.nominative && line.substring(0, mention.start).substringAfterLast(',').trim().isBlank()
                    }
                    // A new referent needs a grammatical narrative anchor, not a stray capitalized object.
                    if (head != null) for (mention in mentions) record(mention.name, cursor)
                }
            }
            cursor = next + 1
        }
        seen.entries.removeAll { paragraph - it.value.paragraph > 6 || end - it.value.offset > 2400 }
    }
    fun record(name: String, offset: Int) { seen[name] = Seen(name, paragraph, offset) }
    fun resolve(pronoun: String, excluded: Set<String>): String? {
        val gender = if (pronoun == "она") DialogueGender.FEMININE else DialogueGender.MASCULINE
        return seen.keys.filter { it !in excluded && roster.gender(it) == gender }.singleOrNull()
    }
}
