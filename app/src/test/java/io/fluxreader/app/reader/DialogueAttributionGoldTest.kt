package io.fluxreader.app.reader

import java.util.Base64
import org.junit.Assert.*
import org.junit.Test

class DialogueAttributionGoldTest {
    @Test fun independentlyAnnotatedControlsHaveNoWrongNamedPredictions() {
        val builder = DialogueCharacterRoster.Builder()
        val rosterText = "— Да, — сказала Нина.\n\n— Да, — сказала Ольга.\n\n— Да, — сказал Марк.\n\n— Да, — сказал Игорь."
        builder.observe(rosterText, BookDialogueAnalyzer.local(rosterText))
        val roster = builder.build()
        val lines = javaClass.getResourceAsStream("/dialogue-attribution-gold.tsv")!!.bufferedReader().use { it.readLines() }
        var correct = 0; var abstain = 0
        for (line in lines.filter { it.isNotBlank() && !it.startsWith('#') }) {
            val parts = line.split('\t')
            fun value(index: Int) = String(Base64.getDecoder().decode(parts[index]), Charsets.UTF_8)
            val text = value(1); val query = value(2); val expected = value(3).ifBlank { null }
            val offset = text.indexOf(query); assertTrue(parts[0], offset >= 0)
            val index = BookDialogueAnalyzer.enrich(text, BookDialogueAnalyzer.local(text), roster)
            val actual = index.at(offset)?.speaker
            if (actual != null) { assertEquals(parts[0], expected, actual); correct++ }
            else abstain++
            assertTrue(index.matches(text))
        }
        assertTrue("coverage: correct=$correct abstain=$abstain", correct >= 14)
    }
}
