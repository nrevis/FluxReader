package io.fluxreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchEngineTest {
    @Test
    fun findsCyrillicCaseInsensitively() {
        val text = "Первый абзац. Скорочтение работает. Ещё СКОРОЧТЕНИЕ здесь."
        val hits = SearchEngine.find("скорочтение", text, 10)
        assertEquals(2, hits.size)
        assertTrue(hits.all { it.snippet.contains("корочтен", ignoreCase = true) })
    }

    @Test
    fun shortQueryReturnsNoHits() {
        assertTrue(SearchEngine.find("я", "я иду", 5).isEmpty())
    }
}
