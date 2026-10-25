package io.fluxreader.app.reader

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.Normalizer
import java.util.Locale

enum class SpeakerTint { BLUE, CYAN, GREEN, LIME, AMBER, ORANGE, ROSE, VIOLET }

data class SpeakerColorEntry(val name: String, val automatic: SpeakerTint, val custom: SpeakerTint? = null)
data class BookSpeakerColors(val revision: Long = 0, val entries: Map<String, SpeakerColorEntry> = emptyMap()) {
    fun forName(name: String) = entries[speakerColorKey(name)]
}

fun speakerColorKey(name: String): String = Normalizer.normalize(name.trim(), Normalizer.Form.NFC)
    .replace(Regex("[\\s\\p{Zs}]+"), " ").lowercase(Locale.ROOT)

/** Per-book colours survive chapter changes and reanalysis. No guesses about name aliases. */
class BookSpeakerColorStore(private val root: File) {
    @Synchronized
    fun read(bookId: String): BookSpeakerColors {
        val file = file(bookId)
        if (!file.isFile || file.length() !in 1..MAX_BYTES) return BookSpeakerColors()
        return try {
            DataInputStream(file.inputStream().buffered()).use { input ->
                require(input.readInt() == 1)
                val revision = input.readLong(); require(revision > 0)
                val count = input.readInt(); require(count in 0..MAX_NAMES)
                val entries = linkedMapOf<String, SpeakerColorEntry>()
                repeat(count) {
                    val name = input.readUTF(); require(validName(name))
                    val auto = SpeakerTint.valueOf(input.readUTF())
                    val custom = if (input.readBoolean()) SpeakerTint.valueOf(input.readUTF()) else null
                    require(entries.put(speakerColorKey(name), SpeakerColorEntry(name, auto, custom)) == null)
                }
                require(input.read() == -1)
                BookSpeakerColors(revision, entries)
            }
        } catch (_: Exception) { BookSpeakerColors() }
    }

    @Synchronized
    fun ensure(bookId: String, names: Collection<String>): BookSpeakerColors {
        val old = read(bookId)
        val entries = old.entries.toMutableMap()
        for (name in names) {
            if (entries.size >= MAX_NAMES) break
            if (validName(name)) add(entries, name)
        }
        return if (entries == old.entries) old else write(bookId, BookSpeakerColors(old.revision + 1, entries))
    }

    @Synchronized
    fun set(bookId: String, name: String, tint: SpeakerTint?): BookSpeakerColors {
        require(validName(name)) { "Некорректное имя" }
        val old = read(bookId)
        val entries = old.entries.toMutableMap()
        require(entries.size < MAX_NAMES || speakerColorKey(name) in entries) { "Слишком много имён" }
        add(entries, name)
        val key = speakerColorKey(name)
        entries[key] = entries.getValue(key).copy(custom = tint)
        return if (entries == old.entries) old else write(bookId, BookSpeakerColors(old.revision + 1, entries))
    }

    private fun add(entries: MutableMap<String, SpeakerColorEntry>, name: String) {
        val key = speakerColorKey(name)
        if (key in entries) return
        val palette = SpeakerTint.entries
        val start = (key.hashCode() and Int.MAX_VALUE) % palette.size
        val usage = entries.values.groupingBy { it.custom ?: it.automatic }.eachCount()
        val chosen = palette.indices.map { palette[(start + it) % palette.size] }.minBy { usage[it] ?: 0 }
        entries[key] = SpeakerColorEntry(name.trim().replace(Regex("[\\s\\p{Zs}]+"), " "), chosen)
    }

    private fun write(bookId: String, colors: BookSpeakerColors): BookSpeakerColors {
        root.mkdirs()
        val temporary = File.createTempFile("colors-", ".tmp", root)
        try {
            DataOutputStream(temporary.outputStream().buffered()).use { output ->
                output.writeInt(1); output.writeLong(colors.revision); output.writeInt(colors.entries.size)
                for (entry in colors.entries.values) {
                    output.writeUTF(entry.name); output.writeUTF(entry.automatic.name)
                    output.writeBoolean(entry.custom != null); entry.custom?.let { output.writeUTF(it.name) }
                }
            }
            try {
                Files.move(temporary.toPath(), file(bookId).toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), file(bookId).toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally { temporary.delete() }
        return colors
    }

    private fun validName(name: String) = name.length in 1..100 && name.isNotBlank() && name.none(Char::isISOControl)
    private fun file(bookId: String) = File(root, ChapterDialogueIndex.fingerprint(bookId) + ".bin")
    private companion object { const val MAX_NAMES = 4096; const val MAX_BYTES = 2L * 1024 * 1024 }
}
