package io.fluxreader.app.reader

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Original scenes and expected labels were frozen before the scene rules were changed. */
class DialogueSceneGoldTest {
    @Test fun independentlyAnnotatedScenesRejectWrongNamesAndKeepConfirmedCoverage() {
        val rosterText = "— Да, — сказала Нина.\n\n— Да, — сказала Ольга.\n\n" +
            "— Да, — сказал Марк.\n\n— Да, — сказал Игорь."
        val roster = DialogueCharacterRoster.Builder().apply {
            observe(rosterText, BookDialogueAnalyzer.local(rosterText))
        }.build()
        val stream = javaClass.getResourceAsStream("/dialogue-attribution-scene-gold.tsv")
        assertNotNull("Independent scene fixture is missing", stream)
        val lines = stream!!.bufferedReader().use { it.readLines() }
        var correct = 0
        var abstain = 0
        var expectedUnknown = 0
        var examples = 0
        val seen = mutableSetOf<String>()
        for (line in lines.filter { it.isNotBlank() && !it.startsWith('#') }) {
            val parts = line.split('\t', limit = 4)
            assertEquals("Scene fixture must have four columns", 4, parts.size)
            val id = parts[0]
            assertTrue("Duplicate scene id: $id", seen.add(id))
            fun value(index: Int) = String(Base64.getDecoder().decode(parts[index]), Charsets.UTF_8)
            val text = value(1)
            val query = value(2)
            val expected = value(3).ifBlank { null }
            val offset = text.indexOf(query)
            assertTrue("Target fragment missing: $id", offset >= 0)
            val index = BookDialogueAnalyzer.enrich(text, BookDialogueAnalyzer.local(text), roster)
            val actual = index.at(offset)?.speaker
            assertTrue("Dialogue positions no longer match the text: $id", index.matches(text))
            if (expected == null) expectedUnknown++
            if (actual != null) {
                assertEquals("Wrong speaker in independent scene: $id", expected, actual)
                correct++
            } else if (expected != null) {
                abstain++
            }
            if (id in confirmedBaseline) {
                assertEquals("Lost previously confirmed attribution: $id", expected, actual)
            }
            examples++
        }
        assertEquals("Frozen scene fixture count", 28, examples)
        assertEquals("Frozen ambiguous scene count", 6, expectedUnknown)
        assertTrue("Missing confirmed baseline cases", seen.containsAll(confirmedBaseline))
        println("Independent scene coverage: correct=$correct abstain=$abstain expectedUnknown=$expectedUnknown")
    }

    companion object {
        // Confirmed with the unchanged alpha.104 Kotlin classes before the scene rule changes.
        private val confirmedBaseline = setOf(
            "scene-continuation-nina",
            "scene-continuation-mark",
            "scene-continuation-olga",
            "scene-continuation-igor",
            "scene-vocative-not-speaker",
            "scene-declined-recipient",
            "scene-third-entry-explicit",
            "scene-two-women-explicit-recipient",
            "scene-quoted-speech-outer-author",
            "scene-quote-command-speaker",
            "scene-action-other-participant",
            "scene-continuation-same-gender-object",
        )
    }
}
