package io.fluxreader.app.reader

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class BookSpeakerColorStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private val names = listOf("Анна", "Борис", "Вера", "Глеб", "Дина", "Егор", "Жанна", "Зоя")

    @Test fun firstPaletteHasDistinctColorsThatSurviveRestartAndChapterOrder() {
        val store = BookSpeakerColorStore(folder.root)
        val first = store.ensure("book", names)
        assertEquals(8, first.entries.values.map { it.automatic }.toSet().size)
        val reloaded = BookSpeakerColorStore(folder.root).ensure("book", names.reversed() + "Иван")
        names.forEach { assertEquals(first.forName(it), reloaded.forName(it)) }
        assertEquals(reloaded, store.ensure("book", names.reversed()))
        assertTrue(reloaded.revision > first.revision)
    }

    @Test fun customColorWinsUntilResetWithoutShufflingAutomaticAssignments() {
        val store = BookSpeakerColorStore(folder.root)
        val original = store.ensure("book", names)
        store.set("book", "Анна", SpeakerTint.ROSE)
        val changed = store.ensure("book", names + "Иван")
        assertEquals(SpeakerTint.ROSE, BookSpeakerColorStore(folder.root).read("book").forName("Анна")?.custom)
        assertEquals(original.forName("Борис"), changed.forName("Борис"))
        val reset = store.set("book", "Анна", null)
        assertEquals(original.forName("Анна"), reset.forName("Анна"))
        assertTrue(reset.revision > changed.revision)
        assertTrue(folder.root.listFiles().orEmpty().none { it.extension == "tmp" })
    }

    @Test fun namesAreBookScopedAndNormalizedWithoutGuessingAliases() {
        val store = BookSpeakerColorStore(folder.root)
        store.set("one", "Анна\u202fСоколова", SpeakerTint.BLUE)
        val colors = store.ensure("one", listOf("АННА  СОКОЛОВА", "Анна", "Соколова"))
        assertEquals(3, colors.entries.size)
        assertEquals(SpeakerTint.BLUE, colors.forName(" анна соколова ")?.custom)
        assertNull(colors.forName("Анна")?.custom)
        assertTrue(store.read("two").entries.isEmpty())
        store.set("two", "Анна Соколова", SpeakerTint.GREEN)
        assertEquals(SpeakerTint.BLUE, store.read("one").forName("Анна Соколова")?.custom)
    }

    @Test fun invalidNamesDoNotEraseSavedColorsAndCorruptFilesFailSafely() {
        val store = BookSpeakerColorStore(folder.root)
        val good = store.set("book", "Анна", SpeakerTint.VIOLET)
        for (name in listOf("", " ", "Анна\nБорис", "А".repeat(101))) {
            try { store.set("book", name, SpeakerTint.BLUE); fail(name) } catch (_: IllegalArgumentException) { }
        }
        assertEquals(good, store.read("book"))
        folder.root.listFiles()!!.single().writeBytes(byteArrayOf(0, 1, 2))
        assertTrue(store.read("book").entries.isEmpty())
    }
}
