package io.fluxreader.app.reader

import java.util.Locale

/** Transform only speech input; stored book text and navigation offsets stay unchanged. */
internal object NarrationPronunciationPolicy {
    fun forSpeech(source: String, locale: Locale): String {
        if (locale.language != "ru") return source
        return source
            .replace(Regex("№\\s*(?=\\d)"), "номер ")
            .replace(Regex("(?iu)(?<![\\p{L}])т\\.\\s*д\\."), "так далее")
            .replace(Regex("(?iu)(?<![\\p{L}])т\\.\\s*п\\."), "тому подобное")
            .replace(Regex("(?iu)(?<![\\p{L}])т\\.\\s*е\\."), "то есть")
            .replace(Regex("(?iu)(?<![\\p{L}])и\\.\\s*т\\.\\s*д\\."), "и так далее")
            .replace('…', '.')
            .replace(Regex("[ \\t]{2,}"), " ")
    }
}
