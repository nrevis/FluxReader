package io.fluxreader.app.reader

/** Narrow, local evidence that never rewrites explicit names or crosses an original reply turn. */
internal object DialogueAdditionalEvidence {
    private const val SPACE = "[\\s\\p{Zs}]"
    private val affected = Regex("(?iu)^(?:это|известие|новость|сказанное|происшедшее|произошедшее)$SPACE+(?:(?:очень|сильно|настолько|явно|совсем)$SPACE+){0,2}(?:потрясло|поразило|удивило|обрадовало|огорчило|расстроило|возмутило|разозлило|испугало|смутило)(?![\\p{L}])")
    private val hypothetical = Regex("(?iu)(?<![\\p{L}])(?:если|бы|казалось|представил|представила|вообразил|вообразила)(?![\\p{L}])")
    private val selfName = Regex("(?:^|(?<=[.!…])$SPACE+)Меня$SPACE+зовут$SPACE+([\\p{Lu}][\\p{Ll}’'-]{1,29}(?:$SPACE+[\\p{Lu}][\\p{Ll}’'-]{1,29}){0,2})(?=$SPACE*[,!.…:;]|$SPACE*$)")
    private val notNames = setOf("Не", "Он", "Она", "Они", "Это", "Меня", "Тебя", "Его", "Ее", "Её", "Нас", "Вас", "Никто", "Никак")

    /** An explicit affected person plus a resumptive subject pronoun, not a mere object mention. */
    fun affectedTopic(prefix: String, pronoun: String, roster: DialogueCharacterRoster,
        forms: DialogueNameForms, excluded: Set<String>): String? {
        val line = prefix.trim()
        if (line.isEmpty() || line.length > 4096 || line.first() in "—–-«\"“" || line.any { it in "«»\"“”" } || hypothetical.containsMatchIn(line)) return null
        val gender = if (pronoun == "она") DialogueGender.FEMININE else DialogueGender.MASCULINE
        val mentions = forms.mentions(line)
        val candidates = mentions.map { it.name }.distinct().filter { it !in excluded && roster.gender(it) == gender }
        val candidate = candidates.singleOrNull() ?: return null
        val resume = Regex("(?iu)(?:[.!?;]$SPACE*|(?:что|но|и|поэтому)$SPACE+)$pronoun(?![\\p{L}])")
        val departure = Regex("(?iu)(?<![\\p{L}])$pronoun$SPACE+(?:(?:тихо|быстро|сразу|молча|потом|затем)$SPACE+){0,2}(?:уш[её]л|ушла|вышел|вышла|удалился|удалилась|покинул|покинула)(?![\\p{L}])")
        if (departure.containsMatchIn(line)) return null
        for (mention in mentions.filter { it.name == candidate && !it.nominative }) {
            val tail = line.substring(mention.end).trimStart()
            val emotion = affected.find(tail) ?: continue
            if (resume.containsMatchIn(tail.substring(emotion.range.last + 1))) return candidate
        }
        return null
    }

    /** Self-introduction is evidence only within the same already parsed turn. */
    fun selfIntroductions(text: String, entries: List<IndexedDialogue>, checkpoint: () -> Unit): List<IndexedDialogue> {
        val result = entries.toMutableList()
        var begin = 0
        while (begin < entries.size) {
            checkpoint()
            var end = begin + 1
            while (end < entries.size && entries[end].span.continuation &&
                text.substring(entries[end - 1].span.endExclusive, entries[end].span.start).none { it == '\n' }) end++
            val first = entries[begin].span
            val last = entries[end - 1].span
            if (last.endExclusive - first.start <= 4096) {
                val names = (begin until end).flatMap { index ->
                    val span = entries[index].span
                    val spoken = text.substring(span.start, span.endExclusive).trimStart('—', '–', '-', ' ', '\u00a0', '\u202f')
                    selfName.findAll(spoken).mapNotNull { match ->
                        // A name inside an inner quotation belongs to its quoted speaker.
                        val before = spoken.substring(0, match.range.first)
                        val insideQuote = before.count { it == '«' } > before.count { it == '»' } ||
                            before.count { it == '“' } > before.count { it == '”' } ||
                            before.count { it == '"' } % 2 != 0
                        if (insideQuote) null else match.groupValues[1].replace(Regex("$SPACE+"), " ")
                            .takeUnless { name -> name.split(' ').any { it in notNames } }
                    }.toList()
                }.distinct()
                val name = names.singleOrNull()
                val explicit = entries.subList(begin, end).mapNotNull { it.span.speaker }.distinct()
                if (name != null && (explicit.isEmpty() || explicit.singleOrNull() == name)) {
                    for (i in begin until end) if (result[i].span.speaker == null) {
                        result[i] = result[i].copy(span = result[i].span.copy(speaker = name), evidence = DialogueEvidence.SELF_INTRODUCTION)
                    }
                }
            }
            begin = end
        }
        return result
    }
}
