package io.fluxreader.app.reader

import io.fluxreader.app.data.BookOutline
import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.ChapterRef
import io.fluxreader.app.data.ReaderBlock
import io.fluxreader.app.data.ReaderLink
import io.fluxreader.app.data.TextRole
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger

/**
 * Persistent cache for fully parsed EPUB/FB2 reader models.
 *
 * The cache lives under Android's cacheDir, so it is always disposable. Every entry carries a
 * source token (content hash + file stat) and a codec version; stale/corrupt entries are ignored
 * and rebuilt from the original book. The compact streaming binary format avoids JSONObject/DOM
 * work on subsequent opens and, unlike Java serialization, keeps the on-disk schema explicit.
 */
internal class ParsedBookDiskCache(
    private val root: File,
    private val maxBytes: Long = DEFAULT_MAX_BYTES,
) {
    private val writesSincePrune = AtomicInteger(0)

    init {
        root.mkdirs()
    }

    fun sourceToken(source: File, contentHash: String): String = buildString {
        append(CODEC_VERSION)
        append(':')
        append(contentHash.ifBlank { "nohash" })
        append(':')
        append(source.length())
        append(':')
        append(source.lastModified())
    }

    fun readOutline(bookId: String, sourceToken: String): BookOutline? {
        val dir = bookDir(bookId)
        val file = File(dir, OUTLINE_FILE)
        val outline = readSafely(file) { input ->
            requireHeader(input, TYPE_OUTLINE, sourceToken)
            val title = input.readString()
            val author = input.readString()
            val count = input.readSafeCount(MAX_CHAPTERS)
            val chapters = ArrayList<ChapterRef>(count)
            repeat(count) {
                chapters += ChapterRef(
                    index = input.readInt(),
                    title = input.readString(),
                    sourceRef = input.readString(),
                    anchors = input.readStringList(MAX_ANCHORS),
                )
            }
            BookOutline(title = title, author = author, chapters = chapters)
        }
        if (outline != null) touch(dir)
        return outline
    }

    fun writeOutline(bookId: String, sourceToken: String, outline: BookOutline) {
        val dir = bookDir(bookId).apply { mkdirs() }
        atomicWrite(File(dir, OUTLINE_FILE)) { output ->
            writeHeader(output, TYPE_OUTLINE, sourceToken)
            output.writeString(outline.title)
            output.writeString(outline.author)
            output.writeInt(outline.chapters.size)
            outline.chapters.forEach { chapter ->
                output.writeInt(chapter.index)
                output.writeString(chapter.title)
                output.writeString(chapter.sourceRef)
                output.writeStringList(chapter.anchors)
            }
        }
        touch(dir)
        pruneEventually(bookId)
    }

    fun readChapter(bookId: String, sourceToken: String, ref: ChapterRef): ChapterContent? {
        val dir = bookDir(bookId)
        val file = chapterFile(dir, ref)
        val chapter = readSafely(file) { input ->
            requireHeader(input, TYPE_CHAPTER, sourceToken)
            val storedIndex = input.readInt()
            val storedSourceRef = input.readString()
            if (storedIndex != ref.index || storedSourceRef != ref.sourceRef) {
                throw CacheMissException()
            }
            val title = input.readString()
            val blockCount = input.readSafeCount(MAX_BLOCKS)
            val blocks = ArrayList<ReaderBlock>(blockCount)
            repeat(blockCount) {
                when (val type = input.readUnsignedByte()) {
                    BLOCK_TEXT -> blocks += ReaderBlock.Text(
                        text = input.readString(),
                        role = input.readTextRole(),
                        anchors = input.readStringList(MAX_ANCHORS),
                        links = input.readLinks(),
                        listDepth = input.readInt().coerceAtLeast(0),
                        listMarker = input.readString(),
                    )
                    BLOCK_IMAGE -> blocks += ReaderBlock.Image(
                        ref = input.readString(),
                        alt = input.readString(),
                        anchors = input.readStringList(MAX_ANCHORS),
                    )
                    else -> error("Unknown parsed-cache block type: $type")
                }
            }
            ChapterContent(index = storedIndex, title = title, blocks = blocks)
        }
        if (chapter != null) touch(dir)
        return chapter
    }

    fun writeChapter(bookId: String, sourceToken: String, ref: ChapterRef, chapter: ChapterContent) {
        val dir = bookDir(bookId).apply { mkdirs() }
        val chaptersDir = File(dir, CHAPTERS_DIR).apply { mkdirs() }
        atomicWrite(chapterFile(dir, ref, chaptersDir)) { output ->
            writeHeader(output, TYPE_CHAPTER, sourceToken)
            output.writeInt(ref.index)
            output.writeString(ref.sourceRef)
            output.writeString(chapter.title)
            output.writeInt(chapter.blocks.size)
            chapter.blocks.forEach { block ->
                when (block) {
                    is ReaderBlock.Text -> {
                        output.writeByte(BLOCK_TEXT)
                        output.writeString(block.text)
                        output.writeString(block.role.name)
                        output.writeStringList(block.anchors)
                        output.writeLinks(block.links)
                        output.writeInt(block.listDepth)
                        output.writeString(block.listMarker)
                    }
                    is ReaderBlock.Image -> {
                        output.writeByte(BLOCK_IMAGE)
                        output.writeString(block.ref)
                        output.writeString(block.alt)
                        output.writeStringList(block.anchors)
                    }
                }
            }
        }
        touch(dir)
        pruneEventually(bookId)
    }

    fun clear(bookId: String) {
        runCatching { bookDir(bookId).deleteRecursively() }
    }

    /** Prunes least-recently-used book directories. The active book is never removed. */
    @Synchronized
    fun prune(excludeBookId: String? = null) {
        if (!root.isDirectory || maxBytes <= 0L) return
        val directories = root.listFiles().orEmpty().filter(File::isDirectory)
        var total = directories.sumOf(::directorySize)
        if (total <= maxBytes) return
        val excluded = excludeBookId?.let(::bookKey)
        directories
            .filter { it.name != excluded }
            .sortedBy { it.lastModified() }
            .forEach { dir ->
                if (total <= maxBytes) return
                val bytes = directorySize(dir)
                if (runCatching { dir.deleteRecursively() }.getOrDefault(false)) total -= bytes
            }
    }

    private fun pruneEventually(activeBookId: String) {
        // Avoid scanning the cache tree on every chapter save. A new session prunes after the first
        // write and then only periodically during long reading sessions.
        val writes = writesSincePrune.incrementAndGet()
        if (writes == 1 || writes % PRUNE_EVERY_WRITES == 0) prune(activeBookId)
    }

    private fun chapterFile(dir: File, ref: ChapterRef, chaptersDir: File = File(dir, CHAPTERS_DIR)): File {
        val refHash = sha256(ref.sourceRef).take(16)
        return File(chaptersDir, "${ref.index}-$refHash.bin")
    }

    private fun bookDir(bookId: String): File = File(root, bookKey(bookId))
    private fun bookKey(bookId: String): String = sha256(bookId).take(32)

    private fun writeHeader(output: DataOutputStream, type: Int, sourceToken: String) {
        output.writeInt(MAGIC)
        output.writeInt(CODEC_VERSION)
        output.writeInt(type)
        output.writeString(sourceToken)
    }

    private fun requireHeader(input: DataInputStream, type: Int, sourceToken: String) {
        if (input.readInt() != MAGIC) throw CacheMissException()
        if (input.readInt() != CODEC_VERSION) throw CacheMissException()
        if (input.readInt() != type) throw CacheMissException()
        if (input.readString() != sourceToken) throw CacheMissException()
    }

    private inline fun <T> readSafely(file: File, reader: (DataInputStream) -> T): T? {
        if (!file.isFile || file.length() <= 0L || file.length() > MAX_ENTRY_BYTES) return null
        return try {
            DataInputStream(BufferedInputStream(file.inputStream(), IO_BUFFER_BYTES)).use(reader)
        } catch (_: CacheMissException) {
            runCatching { file.delete() }
            null
        } catch (_: EOFException) {
            runCatching { file.delete() }
            null
        } catch (_: Throwable) {
            runCatching { file.delete() }
            null
        }
    }

    private inline fun atomicWrite(file: File, writer: (DataOutputStream) -> Unit) {
        file.parentFile?.mkdirs()
        val temporary = File(file.parentFile, ".${file.name}.${System.nanoTime()}.tmp")
        try {
            DataOutputStream(BufferedOutputStream(temporary.outputStream(), IO_BUFFER_BYTES)).use { output ->
                writer(output)
                output.flush()
            }
            if (!temporary.renameTo(file)) {
                temporary.copyTo(file, overwrite = true)
                temporary.delete()
            }
        } finally {
            if (temporary.exists()) temporary.delete()
        }
    }

    private fun DataOutputStream.writeString(value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        require(bytes.size <= MAX_STRING_BYTES) { "Parsed-cache string is too large" }
        writeInt(bytes.size)
        write(bytes)
    }

    private fun DataInputStream.readString(): String {
        val size = readInt()
        if (size < 0 || size > MAX_STRING_BYTES) throw CacheMissException()
        val bytes = ByteArray(size)
        readFully(bytes)
        return bytes.toString(Charsets.UTF_8)
    }

    private fun DataOutputStream.writeStringList(values: List<String>) {
        writeInt(values.size)
        values.forEach { value -> writeString(value) }
    }

    private fun DataInputStream.readStringList(maxCount: Int): List<String> {
        val count = readSafeCount(maxCount)
        return List(count) { readString() }
    }

    private fun DataOutputStream.writeLinks(links: List<ReaderLink>) {
        writeInt(links.size)
        links.forEach { link ->
            writeInt(link.start)
            writeInt(link.end)
            writeString(link.target)
            writeBoolean(link.external)
        }
    }

    private fun DataInputStream.readLinks(): List<ReaderLink> {
        val count = readSafeCount(MAX_LINKS)
        return List(count) {
            ReaderLink(
                start = readInt(),
                end = readInt(),
                target = readString(),
                external = readBoolean(),
            )
        }
    }

    private fun DataInputStream.readTextRole(): TextRole {
        val name = readString()
        return TextRole.values().firstOrNull { it.name == name } ?: throw CacheMissException()
    }

    private fun DataInputStream.readSafeCount(max: Int): Int {
        val count = readInt()
        if (count < 0 || count > max) throw CacheMissException()
        return count
    }

    private fun directorySize(file: File): Long {
        if (!file.exists()) return 0L
        if (file.isFile) return file.length()
        return file.listFiles().orEmpty().sumOf(::directorySize)
    }

    private fun touch(file: File) {
        runCatching { file.setLastModified(System.currentTimeMillis()) }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    private class CacheMissException : RuntimeException()

    companion object {
        private const val MAGIC = 0x46525836 // FRX6
        private const val CODEC_VERSION = 1
        private const val TYPE_OUTLINE = 1
        private const val TYPE_CHAPTER = 2
        private const val BLOCK_TEXT = 1
        private const val BLOCK_IMAGE = 2
        private const val OUTLINE_FILE = "outline.bin"
        private const val CHAPTERS_DIR = "chapters"
        private const val IO_BUFFER_BYTES = 64 * 1024
        private const val PRUNE_EVERY_WRITES = 24
        private const val MAX_CHAPTERS = 100_000
        private const val MAX_BLOCKS = 250_000
        private const val MAX_ANCHORS = 100_000
        private const val MAX_LINKS = 250_000
        private const val MAX_STRING_BYTES = 48 * 1024 * 1024
        private const val MAX_ENTRY_BYTES = 256L * 1024L * 1024L
        const val DEFAULT_MAX_BYTES = 160L * 1024L * 1024L
    }
}
