package io.fluxreader.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration

internal data class SpeechColorRun(val start: Int, val end: Int, val color: Color)

/** Apply after fixation styling: keep its weight, and keep the entire speech in the name's color. */
internal fun colorSpeedSpeech(text: AnnotatedString, runs: List<SpeechColorRun>, focusCharacter: Int? = null): AnnotatedString {
    if (runs.isEmpty()) return text
    return AnnotatedString.Builder(text).apply {
        runs.forEach { run ->
            if (run.start >= 0 && run.end <= text.length && run.start < run.end) {
                addStyle(SpanStyle(color = run.color), run.start, run.end)
                if (focusCharacter != null && focusCharacter in run.start until run.end) {
                    addStyle(SpanStyle(textDecoration = TextDecoration.Underline), focusCharacter, focusCharacter + 1)
                }
            }
        }
    }.toAnnotatedString()
}
