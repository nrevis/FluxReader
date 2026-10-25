package io.fluxreader.app.reader

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File

/** Disposable, versioned cache. Source token and exact text digest prevent stale offset reuse. */
internal class BookDialogueDiskCache(private val root: File, private val maxBytes: Long = 64L * 1024 * 1024) {
    fun read(source: String, chapter: String, pass: String, text: String): ChapterDialogueIndex? {
        val file = entry(source, chapter, pass)
        if (!file.isFile || file.length() !in 1..16L * 1024 * 1024) return null
        return try {
            DataInputStream(file.inputStream().buffered()).use { input ->
                require(input.readInt() == VERSION && input.readUTF() == source && input.readUTF() == pass)
                val digest = input.readUTF()
                require(digest == ChapterDialogueIndex.fingerprint(text))
                val count = input.readInt()
                require(count in 0..250_000)
                var end = 0
                val entries = List(count) {
                    val start = input.readInt()
                    val finish = input.readInt()
                    require(start >= end && finish > start && finish <= text.length)
                    end = finish
                    val speaker = input.readUTF().takeIf { it.isNotEmpty() }
                    require(speaker == null || speaker.length <= 100)
                    val continuation = input.readBoolean()
                    val evidence = DialogueEvidence.entries[input.readUnsignedByte()]
                    IndexedDialogue(SpeedDialogueSpan(start, finish, speaker, continuation), evidence)
                }
                require(input.read() == -1)
                file.setLastModified(System.currentTimeMillis())
                ChapterDialogueIndex(digest, entries)
            }
        } catch (_: Exception) {
            file.delete()
            null
        }
    }

    fun write(source: String, chapter: String, pass: String, index: ChapterDialogueIndex) {
        val file = entry(source, chapter, pass)
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, ".${file.name}.${System.nanoTime()}.tmp")
        try {
            DataOutputStream(temporary.outputStream().buffered()).use { out ->
                out.writeInt(VERSION)
                out.writeUTF(source)
                out.writeUTF(pass)
                out.writeUTF(index.textFingerprint)
                out.writeInt(index.entries.size)
                index.entries.forEach { (span, evidence) ->
                    out.writeInt(span.start)
                    out.writeInt(span.endExclusive)
                    out.writeUTF(span.speaker.orEmpty())
                    out.writeBoolean(span.continuation)
                    out.writeByte(evidence.ordinal)
                }
            }
            check(temporary.renameTo(file)) { "Cannot save dialogue cache" }
        } finally {
            temporary.delete()
        }
        prune()
    }

    private fun entry(source: String, chapter: String, pass: String): File = File(root,
        ChapterDialogueIndex.fingerprint("$VERSION:$source:$chapter:$pass") + ".bin")

    private fun prune() {
        val files = root.listFiles()?.filter { it.isFile && it.extension == "bin" } ?: return
        var size = files.sumOf { it.length() }
        if (size <= maxBytes) return
        for (file in files.sortedBy { it.lastModified() }) {
            val bytes = file.length()
            if (file.delete()) size -= bytes
            if (size <= maxBytes) break
        }
    }

    private companion object { const val VERSION = 3 }
}
