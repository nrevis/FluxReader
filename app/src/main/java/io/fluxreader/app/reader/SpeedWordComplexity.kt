package io.fluxreader.app.reader

/** Bounded spelling cues, not a vocabulary/familiarity or named-entity classifier. */
object SpeedWordComplexity {
    private const val VOWELS = "aeiouyаеёиоуыэюя"
    private const val CONSONANTS = "bcdfghjklmnpqrstvwxzбвгджзйклмнпрстфхцчшщ"

    /** Units of the user-selected extra milliseconds. Plain short words cost zero; all cues
     * share one 2-unit ceiling, so long numbers, names and URLs cannot multiply pauses. */
    fun score(token: String): Double {
        val core = token.trim { !it.isLetterOrDigit() }
        if (core.isEmpty()) return 0.0
        val letters = core.filter(Char::isLetter)
        val length = letters.length
        var score = when {
            length >= 18 -> 2.0
            length >= 12 -> 1.5
            length >= 8 -> 1.0
            else -> 0.0
        }
        // Include sentence-initial names without pretending capitalization proves an identity.
        val titlecase = letters.firstOrNull()?.isUpperCase() == true && letters.any(Char::isLowerCase)
        if (titlecase && length >= 6) score = maxOf(score, 1.0)
        if (length >= 4 && letters.all(Char::isUpperCase)) score = maxOf(score, 1.0)

        var vowels = 0
        var consonants = 0
        var longestConsonants = 0
        for (character in core) {
            val lower = character.lowercaseChar()
            if (lower in VOWELS) vowels++
            consonants = if (lower in CONSONANTS) consonants + 1 else 0
            longestConsonants = maxOf(longestConsonants, consonants)
        }
        if (length >= 6 && longestConsonants >= 4) score = maxOf(score, 1.0) + 0.25
        if (length >= 10 && vowels >= 5) score = maxOf(score, 1.0) + 0.25

        val digits = core.count(Char::isDigit)
        if (digits >= 3 || digits > 0 && length >= 3) score = maxOf(score, 1.0)
        if (digits >= 8) score = maxOf(score, 1.5)
        if (digits >= 13) score = 2.0
        if ("://" in core || '@' in core && length >= 3) score = maxOf(score, 1.5)
        val compound = core.split('-', '‑', '–', '—').count { part -> part.count(Char::isLetter) >= 4 } >= 2
        if (compound) score = maxOf(score, 1.0) + 0.25
        return score.coerceIn(0.0, 2.0)
    }
}
