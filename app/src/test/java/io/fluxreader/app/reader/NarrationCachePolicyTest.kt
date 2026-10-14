package io.fluxreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NarrationCachePolicyTest {
    @Test fun keyIsStableAndSensitiveToVoiceSettings() {
        val original = NarrationCachePolicy.key("Привет", "Irina", "ru-RU", 1f, 1f)
        assertEquals(64, original.length)
        assertEquals(original, NarrationCachePolicy.key("Привет", "Irina", "ru-RU", 1f, 1f))
        assertNotEquals(original, NarrationCachePolicy.key("Привет", "Dmitri", "ru-RU", 1f, 1f))
        assertNotEquals(original, NarrationCachePolicy.key("Привет", "Irina", "ru-RU", 1.1f, 1f))
        assertNotEquals(original, NarrationCachePolicy.key("Привет", "Irina", "ru-RU", 1f, 0.9f))
        assertNotEquals(original, NarrationCachePolicy.key("Мир", "Irina", "ru-RU", 1f, 1f))
    }
}
