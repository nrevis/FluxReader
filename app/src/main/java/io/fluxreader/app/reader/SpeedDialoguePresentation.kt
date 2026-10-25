package io.fluxreader.app.reader

/** Recognition and its visible indicator are independent of whether a name is known. */
object SpeedDialoguePresentation {
    data class Label(val text: String, val namedSpeaker: Boolean)

    fun label(span: SpeedDialogueSpan?, showNames: Boolean, showIndicator: Boolean): Label? {
        if (span == null) return null
        if (showNames && !span.speaker.isNullOrBlank()) return Label(span.speaker, true)
        return if (showIndicator) Label("Прямая речь", false) else null
    }
}
