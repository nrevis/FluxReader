package io.fluxreader.app.reader

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Original offsets identify one reply, including continuations around author's remarks. */
data class DialogueReply(val start: Int, val endExclusive: Int)
data class DialogueCorrection(val reply: DialogueReply, val speaker: String?)

object DialogueReplyLocator {
    fun at(text: String, offset: Int): DialogueReply? {
        val active = SpeedDialogueAnalyzer.at(text, offset) ?: return null
        val begin = text.lastIndexOf('\n', (active.start - 1).coerceAtLeast(0)) + 1
        val end = text.indexOf('\n', active.start).let { if (it < 0) text.length else it }
        val spans = SpeedDialogueAnalyzer.spans(text.substring(begin, end))
        val selected = spans.indexOfFirst { it.contains(offset - begin) }
        if (selected < 0) return DialogueReply(active.start, active.endExclusive)
        var first = selected
        while (first > 0 && spans[first].continuation) first--
        var last = selected
        while (last + 1 < spans.size && spans[last + 1].continuation) last++
        return DialogueReply(begin + spans[first].start, begin + spans[last].endExclusive)
    }

    fun resolve(text: String, offset: Int, index: ChapterDialogueIndex?, corrections: List<DialogueCorrection>): SpeedDialogueSpan? {
        val automatic = index?.at(offset) ?: SpeedDialogueAnalyzer.at(text, offset) ?: return null
        val correction = corrections.firstOrNull {
            automatic.start >= it.reply.start && automatic.endExclusive <= it.reply.endExclusive
        }
        return if (correction == null) automatic else automatic.copy(speaker = correction.speaker)
    }
}

/** User edits live in filesDir, not the disposable analysis cache. No network or book rewriting. */
class DialogueCorrectionStore(private val root: File) {
    @Synchronized
    fun read(bookId: String, chapter: Int, text: String): List<DialogueCorrection> {
        val digest = ChapterDialogueIndex.fingerprint(text)
        val file = file(bookId, chapter, digest)
        if (!file.isFile || file.length() !in 1..MAX_BYTES) return emptyList()
        return try {
            DataInputStream(file.inputStream().buffered()).use { input ->
                require(input.readInt() == VERSION && input.readUTF() == digest)
                val count = input.readInt()
                require(count in 0..MAX_EDITS)
                val entries = ArrayList<DialogueCorrection>(count)
                repeat(count) {
                    val reply = DialogueReply(input.readInt(), input.readInt())
                    val speaker = if (input.readBoolean()) input.readUTF() else null
                    require(speaker == null || validName(speaker))
                    require(reply.start >= 0 && reply.endExclusive <= text.length && reply.start < reply.endExclusive)
                    require(entries.lastOrNull()?.reply?.endExclusive?.let { it <= reply.start } != false)
                    require(DialogueReplyLocator.at(text, reply.start) == reply)
                    entries += DialogueCorrection(reply, speaker)
                }
                require(input.read() == -1)
                entries
            }
        } catch (_: Exception) { emptyList() }
    }

    @Synchronized
    fun update(bookId: String, chapter: Int, text: String, reply: DialogueReply, speaker: String?, automatic: Boolean): List<DialogueCorrection> {
        require(DialogueReplyLocator.at(text, reply.start) == reply) { "Текст реплики изменился" }
        val normalized = speaker?.trim()
        require(normalized == null || validName(normalized)) { "Имя должно содержать от 1 до 80 символов" }
        val entries = read(bookId, chapter, text).filterNot { it.reply.start == reply.start }.toMutableList()
        if (!automatic) entries += DialogueCorrection(reply, normalized)
        entries.sortBy { it.reply.start }
        require(entries.size <= MAX_EDITS) { "Слишком много исправлений в одной главе" }
        val digest = ChapterDialogueIndex.fingerprint(text)
        val destination = file(bookId, chapter, digest)
        root.mkdirs()
        val temporary = File.createTempFile("dialogue-", ".tmp", root)
        try {
            DataOutputStream(temporary.outputStream().buffered()).use { output ->
                output.writeInt(VERSION); output.writeUTF(digest); output.writeInt(entries.size)
                for (entry in entries) {
                    output.writeInt(entry.reply.start); output.writeInt(entry.reply.endExclusive)
                    output.writeBoolean(entry.speaker != null)
                    entry.speaker?.let(output::writeUTF)
                }
            }
            try {
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                Files.move(temporary.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } finally { temporary.delete() }
        return entries
    }

    private fun file(bookId: String, chapter: Int, digest: String) = File(root,
        ChapterDialogueIndex.fingerprint("$bookId:$chapter:$digest") + ".bin")

    private fun validName(value: String) = value.length in 1..80 && value.none(Char::isISOControl) && value.isNotBlank()
    private companion object { const val VERSION = 1; const val MAX_EDITS = 5000; const val MAX_BYTES = 2L * 1024 * 1024 }
}
