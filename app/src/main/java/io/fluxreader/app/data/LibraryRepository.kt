package io.fluxreader.app.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.DocumentsContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.ArrayDeque
import java.util.zip.ZipFile
import io.fluxreader.app.reader.PdfRendererEngine
import kotlin.math.min

class LibraryRepository(private val context: Context) {
    private val libraryFile = File(context.filesDir, "library.json")
    private val foldersFile = File(context.filesDir, "library-folders.json")
    private val booksDir = File(context.filesDir, "books").apply { mkdirs() }
    private val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var persistJob: Job? = null
    private var folderPersistJob: Job? = null

    private val _books = MutableStateFlow(loadLibrary())
    val books: StateFlow<List<BookRecord>> = _books.asStateFlow()
    private val _folders = MutableStateFlow(loadFolders(_books.value))
    val folders: StateFlow<List<String>> = _folders.asStateFlow()

    init {
        // Older library entries may predate cover extraction. Repair only missing files in the
        // background so upgrading FluxReader makes artwork appear without re-importing books.
        ioScope.launch { runCatching { repairMissingCovers(force = false) } }
    }

    private data class ImportOutcome(
        val book: BookRecord,
        val added: Boolean,
        val folderLinked: Boolean,
    )

    private data class FolderCursor(val documentId: String, val path: String, val depth: Int)
    private data class TreeEntry(val documentId: String, val name: String, val mimeType: String)

    suspend fun importBook(uri: Uri): BookRecord = withContext(Dispatchers.IO) {
        val outcome = importBookInternal(uri, folderPath = "")
        if (!outcome.added) error("Книга уже есть в библиотеке: ${outcome.book.title}")
        outcome.book
    }

    suspend fun importSource(
        uri: Uri,
        onProgress: (SourceImportProgress) -> Unit = {},
    ): SourceImportResult = withContext(Dispatchers.IO) {
        val name = displayName(uri).orEmpty().ifBlank { "book_${System.currentTimeMillis()}" }
        val mime = context.contentResolver.getType(uri)
        onProgress(SourceImportProgress(ImportPhase.PREPARING, label = name))
        if (ArchiveImportPolicy.isGenericZip(name, mime)) {
            importArchiveInternal(uri, name, onProgress)
        } else {
            onProgress(SourceImportProgress(ImportPhase.IMPORTING_BOOKS, label = name, current = 0, total = 1))
            val outcome = importBookInternal(uri, folderPath = "")
            if (!outcome.added) error("Книга уже есть в библиотеке: ${outcome.book.title}")
            onProgress(SourceImportProgress(ImportPhase.IMPORTING_BOOKS, label = name, current = 1, total = 1))
            SourceImportResult(added = 1, addedBookIds = listOf(outcome.book.id))
        }
    }

    suspend fun importFolder(
        treeUri: Uri,
        onProgress: (SourceImportProgress) -> Unit = {},
    ): FolderImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        runCatching { resolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        val rootId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }
            .getOrElse { throw IllegalArgumentException("Не удалось открыть выбранную папку") }
        val rootUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, rootId)
        val rootName = sanitizeFolderSegment(displayName(rootUri).orEmpty().ifBlank { "Папка" })
        onProgress(SourceImportProgress(ImportPhase.SCANNING_FOLDER, label = rootName))
        val foldersSeen = linkedSetOf(rootName)
        val addedIds = mutableListOf<String>()
        var added = 0
        var linkedExisting = 0
        var skipped = 0
        var firstError = ""
        var visitedDocuments = 0
        val stack = ArrayDeque<FolderCursor>()
        stack.add(FolderCursor(rootId, rootName, 0))

        while (stack.isNotEmpty()) {
            currentCoroutineContext().ensureActive()
            val folder = stack.removeLast()
            val children = try {
                queryTreeChildren(treeUri, folder.documentId)
            } catch (error: Throwable) {
                skipped++
                if (firstError.isBlank()) firstError = error.message ?: "Не удалось прочитать папку ${folder.path}"
                continue
            }
            for (entry in children) {
                currentCoroutineContext().ensureActive()
                visitedDocuments++
                onProgress(
                    SourceImportProgress(
                        ImportPhase.SCANNING_FOLDER,
                        label = entry.name.ifBlank { folder.path },
                        current = visitedDocuments,
                    ),
                )
                if (visitedDocuments > MAX_TREE_DOCUMENTS) {
                    if (firstError.isBlank()) firstError = "Остановлено после $MAX_TREE_DOCUMENTS объектов: папка слишком большая"
                    stack.clear()
                    break
                }
                val safeName = sanitizeFolderSegment(entry.name)
                if (entry.mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    if (folder.depth >= MAX_FOLDER_DEPTH) {
                        skipped++
                        if (firstError.isBlank()) firstError = "Слишком глубокая структура папок"
                        continue
                    }
                    val childPath = LibraryFolderTree.join(folder.path, safeName)
                    foldersSeen += childPath
                    stack.add(FolderCursor(entry.documentId, childPath, folder.depth + 1))
                    continue
                }
                val format = detectFormat(entry.name) ?: detectMime(entry.mimeType) ?: continue
                val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, entry.documentId)
                try {
                    val outcome = importBookInternal(documentUri, folder.path, forcedFormat = format, forcedName = entry.name)
                    if (outcome.added) {
                        added++
                        addedIds += outcome.book.id
                    } else if (outcome.folderLinked) {
                        linkedExisting++
                    }
                } catch (error: Throwable) {
                    skipped++
                    if (firstError.isBlank()) firstError = error.message ?: "Не удалось импортировать ${entry.name}"
                }
            }
        }

        registerFolders(foldersSeen)
        val representedFolderCount = foldersSeen.size
        FolderImportResult(
            added = added,
            linkedExisting = linkedExisting,
            skipped = skipped,
            folderCount = representedFolderCount,
            firstError = firstError,
            addedBookIds = addedIds,
        )
    }

    private suspend fun importBookInternal(
        uri: Uri,
        folderPath: String,
        forcedFormat: BookFormat? = null,
        forcedName: String? = null,
    ): ImportOutcome {
        val originalName = forcedName?.takeIf { it.isNotBlank() } ?: displayName(uri) ?: "book_${System.currentTimeMillis()}"
        val format = forcedFormat
            ?: detectFormat(originalName)
            ?: detectMime(context.contentResolver.getType(uri))
            ?: throw IllegalArgumentException("Поддерживаются EPUB, FB2, FB2.ZIP, TXT и PDF")
        return context.contentResolver.openInputStream(uri)?.use { input ->
            importBookStreamInternal(
                input = input,
                originalName = originalName,
                folderPath = folderPath,
                format = format,
            )
        } ?: error("Не удалось открыть выбранный файл")
    }

    private suspend fun importBookStreamInternal(
        input: InputStream,
        originalName: String,
        folderPath: String,
        format: BookFormat,
        maxBytes: Long = Long.MAX_VALUE,
        onBytesCopied: (Int) -> Unit = {},
    ): ImportOutcome {
        val normalizedFolder = LibraryFolderTree.normalize(folderPath)
        val id = UUID.randomUUID().toString()
        val extension = when {
            originalName.lowercase().endsWith(".fb2.zip") -> "fb2.zip"
            format == BookFormat.EPUB -> "epub"
            format == BookFormat.FB2 -> "fb2"
            format == BookFormat.TXT -> "txt"
            else -> "pdf"
        }
        val destination = File(booksDir, "$id.$extension")
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            var copied = 0L
            destination.outputStream().buffered().use { output ->
                val buffer = ByteArray(128 * 1024)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (read == 0) continue
                    copied += read.toLong()
                    if (copied > maxBytes) throw IllegalArgumentException("Файл в архиве слишком большой")
                    onBytesCopied(read)
                    digest.update(buffer, 0, read)
                    output.write(buffer, 0, read)
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            val existing = _books.value.firstOrNull { stored ->
                when {
                    stored.contentHash.isNotBlank() -> stored.contentHash == hash
                    File(stored.filePath).length() != destination.length() -> false
                    else -> runCatching { sha256(File(stored.filePath)) == hash }.getOrDefault(false)
                }
            }
            if (existing != null) {
                destination.delete()
                val nextFolders = if (normalizedFolder.isBlank()) existing.folderPaths else {
                    (existing.folderPaths + normalizedFolder)
                        .map(LibraryFolderTree::normalize)
                        .filter { it.isNotBlank() }
                        .distinctBy { it.lowercase() }
                }
                val updated = existing.copy(
                    contentHash = existing.contentHash.ifBlank { hash },
                    folderPaths = nextFolders,
                )
                val changed = updated != existing
                if (changed) {
                    _books.value = _books.value.map { if (it.id == existing.id) updated else it }
                    persistSoon()
                }
                return ImportOutcome(updated, added = false, folderLinked = nextFolders != existing.folderPaths)
            }

            val smart = SmartBookImport.inspect(destination, format, originalName)
            val now = System.currentTimeMillis()
            val metadata = smart.metadata
            val resolvedTitle = metadata.title.ifBlank { originalName.removeBookExtension() }
            val coverPath = createAutomaticCover(
                bookId = id,
                source = destination,
                format = format,
                embeddedBytes = smart.coverBytes,
                title = resolvedTitle,
                author = metadata.author,
            )
            val record = BookRecord(
                id = id,
                title = resolvedTitle,
                author = metadata.author,
                filePath = destination.absolutePath,
                originalName = originalName,
                format = format,
                series = metadata.series,
                seriesIndex = metadata.seriesIndex,
                folderPaths = normalizedFolder.takeIf { it.isNotBlank() }?.let(::listOf).orEmpty(),
                coverPath = coverPath,
                description = metadata.description,
                publisher = metadata.publisher,
                language = metadata.language,
                year = metadata.year,
                contentHash = hash,
                lastOpenedAt = 0L,
                addedAt = now,
            )
            _books.value = _books.value + record
            persistSoon()
            return ImportOutcome(record, added = true, folderLinked = false)
        } catch (error: Throwable) {
            runCatching { destination.delete() }
            coversDir.listFiles()?.filter { it.name.startsWith("$id.") }?.forEach { runCatching { it.delete() } }
            throw error
        }
    }

    private suspend fun importArchiveInternal(
        uri: Uri,
        displayName: String,
        onProgress: (SourceImportProgress) -> Unit,
    ): SourceImportResult {
        val archiveRoot = sanitizeFolderSegment(
            displayName.replace(Regex("(?i)\\.zip$"), "").ifBlank { "Архив" },
        )
        val foldersSeen = linkedSetOf<String>()
        val addedIds = mutableListOf<String>()
        var added = 0
        var linkedExisting = 0
        var skipped = 0
        var firstError = ""
        var supportedCount = 0
        var totalBookBytes = 0L

        val sourceBytes = contentLength(uri)
        val tempDir = File(context.cacheDir, "archive-import").apply { mkdirs() }
        val tempArchive = File(tempDir, "${UUID.randomUUID()}.zip")
        try {
            onProgress(SourceImportProgress(ImportPhase.COPYING_ARCHIVE, label = displayName, bytesTotal = sourceBytes))
            val raw = context.contentResolver.openInputStream(uri) ?: error("Не удалось открыть ZIP-архив")
            raw.use { input ->
                tempArchive.outputStream().buffered().use { output ->
                    val buffer = ByteArray(128 * 1024)
                    var copied = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        if (read == 0) continue
                        copied += read.toLong()
                        if (copied > ArchiveImportPolicy.MAX_ARCHIVE_BYTES) {
                            throw IllegalArgumentException("ZIP-архив слишком большой")
                        }
                        output.write(buffer, 0, read)
                        onProgress(
                            SourceImportProgress(
                                ImportPhase.COPYING_ARCHIVE,
                                label = displayName,
                                bytesCopied = copied,
                                bytesTotal = sourceBytes,
                            ),
                        )
                    }
                }
            }

            ZipFile(tempArchive).use { zip ->
                val entries = zip.entries().asSequence().toList()
                if (entries.size > ArchiveImportPolicy.MAX_ENTRIES) {
                    throw IllegalArgumentException("Архив содержит слишком много файлов")
                }
                val supportedEntries = entries.asSequence()
                    .filterNot { it.isDirectory }
                    .mapNotNull { entry -> ArchiveImportPolicy.bookEntry(entry.name)?.let { entry to it } }
                    .toList()
                supportedCount = supportedEntries.size
                if (supportedCount == 0) {
                    throw IllegalArgumentException("В ZIP-архиве не найдено EPUB, FB2, FB2.ZIP, TXT или PDF")
                }
                if (supportedCount > ArchiveImportPolicy.MAX_BOOKS) {
                    skipped++
                    if (firstError.isBlank()) firstError = "Остановлено после ${ArchiveImportPolicy.MAX_BOOKS} книг"
                }
                val limitedEntries = supportedEntries.take(ArchiveImportPolicy.MAX_BOOKS)
                for ((entryIndex, pair) in limitedEntries.withIndex()) {
                    currentCoroutineContext().ensureActive()
                    val (entry, bookEntry) = pair
                    onProgress(
                        SourceImportProgress(
                            ImportPhase.IMPORTING_BOOKS,
                            label = bookEntry.fileName,
                            current = entryIndex,
                            total = limitedEntries.size,
                        ),
                    )
                    if (entry.size > ArchiveImportPolicy.MAX_BOOK_BYTES) {
                        skipped++
                        if (firstError.isBlank()) firstError = "${bookEntry.fileName}: файл слишком большой"
                        continue
                    }
                    val declared = entry.size.coerceAtLeast(0L)
                    if (declared > 0L && totalBookBytes + declared > ArchiveImportPolicy.MAX_TOTAL_BOOK_BYTES) {
                        skipped++
                        if (firstError.isBlank()) firstError = "Суммарный размер книг в архиве слишком большой"
                        break
                    }

                    val safeParents = bookEntry.parentSegments
                        .take(ArchiveImportPolicy.MAX_PATH_DEPTH)
                        .map(::sanitizeFolderSegment)
                    var folder = archiveRoot
                    safeParents.forEach { segment -> folder = LibraryFolderTree.join(folder, segment) }
                    foldersSeen += folder

                    try {
                        zip.getInputStream(entry).use { entryInput ->
                            val outcome = importBookStreamInternal(
                                input = entryInput,
                                originalName = bookEntry.fileName,
                                folderPath = folder,
                                format = bookEntry.format,
                                maxBytes = ArchiveImportPolicy.MAX_BOOK_BYTES,
                                onBytesCopied = { read ->
                                    totalBookBytes += read.toLong()
                                    if (totalBookBytes > ArchiveImportPolicy.MAX_TOTAL_BOOK_BYTES) {
                                        throw IllegalArgumentException("Суммарный размер книг в архиве слишком большой")
                                    }
                                },
                            )
                            if (outcome.added) {
                                added++
                                addedIds += outcome.book.id
                            } else if (outcome.folderLinked) {
                                linkedExisting++
                            } else {
                                skipped++
                            }
                        }
                    } catch (error: Throwable) {
                        skipped++
                        if (firstError.isBlank()) firstError = "${bookEntry.fileName}: ${error.message ?: "ошибка импорта"}"
                        if (totalBookBytes > ArchiveImportPolicy.MAX_TOTAL_BOOK_BYTES) break
                    }
                    onProgress(
                        SourceImportProgress(
                            ImportPhase.IMPORTING_BOOKS,
                            label = bookEntry.fileName,
                            current = entryIndex + 1,
                            total = limitedEntries.size,
                        ),
                    )
                }
            }
        } finally {
            runCatching { tempArchive.delete() }
            if (tempDir.listFiles().isNullOrEmpty()) runCatching { tempDir.delete() }
        }

        registerFolders(foldersSeen)
        return SourceImportResult(
            added = added,
            linkedExisting = linkedExisting,
            skipped = skipped,
            folderCount = foldersSeen.size,
            firstError = firstError,
            addedBookIds = addedIds,
            archive = true,
        )
    }

    fun find(bookId: String): BookRecord? = _books.value.firstOrNull { it.id == bookId }

    /** Parsed metadata may enrich imported data, but never overwrites fields the user explicitly edited. */
    fun updateMetadata(bookId: String, metadata: BookMetadata, chapterCount: Int) {
        _books.value = _books.value.map { book ->
            if (book.id != bookId) book else if (book.metadataLocked) {
                book.copy(chapterCount = chapterCount.coerceAtLeast(0))
            } else {
                book.copy(
                    title = metadata.title.ifBlank { book.title },
                    author = metadata.author.ifBlank { book.author },
                    series = metadata.series.ifBlank { book.series },
                    seriesIndex = metadata.seriesIndex.takeIf { it > 0f } ?: book.seriesIndex,
                    description = metadata.description.ifBlank { book.description },
                    publisher = metadata.publisher.ifBlank { book.publisher },
                    language = metadata.language.ifBlank { book.language },
                    year = metadata.year.ifBlank { book.year },
                    chapterCount = chapterCount.coerceAtLeast(0),
                )
            }
        }
        persistSoon()
    }

    fun editMetadata(bookId: String, metadata: BookMetadata, groups: List<String>) {
        val cleanGroups = groups.map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.take(24)
        _books.value = _books.value.map { book ->
            if (book.id != bookId) book else book.copy(
                title = metadata.title.trim().ifBlank { book.title },
                author = metadata.author.trim(),
                series = metadata.series.trim(),
                seriesIndex = metadata.seriesIndex.coerceAtLeast(0f),
                groups = cleanGroups,
                description = metadata.description.trim().take(8000),
                publisher = metadata.publisher.trim().take(240),
                language = metadata.language.trim().take(40),
                year = metadata.year.trim().take(24),
                metadataLocked = true,
            )
        }
        persistSoon()
        if (find(bookId)?.coverPath?.contains(".generated", ignoreCase = true) == true) {
            ioScope.launch { runCatching { repairBookCover(bookId, force = true) } }
        }
    }

    suspend fun setCover(bookId: String, uri: Uri) = withContext(Dispatchers.IO) {
        val book = find(bookId) ?: return@withContext
        val bytes = context.contentResolver.openInputStream(uri)?.use { input -> readLimited(input, 20 * 1024 * 1024) }
            ?: error("Не удалось открыть изображение")
        val target = File(coversDir, "$bookId.custom.jpg")
        require(writeNormalizedCover(bytes, target)) { "Выбранный файл не является поддерживаемым изображением" }
        book.coverPath.takeIf { it.isNotBlank() && it != target.absolutePath }?.let { old -> runCatching { File(old).delete() } }
        _books.value = _books.value.map { if (it.id == bookId) it.copy(coverPath = target.absolutePath) else it }
        persistSoon()
    }

    fun removeCover(bookId: String) {
        val book = find(bookId) ?: return
        book.coverPath.takeIf { it.isNotBlank() }?.let { path -> ioScope.launch { runCatching { File(path).delete() } } }
        _books.value = _books.value.map { if (it.id == bookId) it.copy(coverPath = "") else it }
        persistSoon()
        ioScope.launch { runCatching { repairBookCover(bookId, force = true) } }
    }

    suspend fun repairMissingCovers(force: Boolean = false): Int = withContext(Dispatchers.IO) {
        var repaired = 0
        _books.value.map { book ->
            val existing = book.coverPath.takeIf { it.isNotBlank() }?.let(::File)
            val isCustom = existing?.name?.contains(".custom", ignoreCase = true) == true
            if (isCustom || (!force && existing?.isFile == true)) return@map book
            val updated = repairBookCoverRecord(book, force)
            if (updated.coverPath != book.coverPath && updated.coverPath.isNotBlank()) repaired++
            updated
        }.let { updated ->
            _books.value = updated
            saveLibrary(updated)
        }
        repaired
    }

    private suspend fun repairBookCover(bookId: String, force: Boolean): Boolean = withContext(Dispatchers.IO) {
        val book = find(bookId) ?: return@withContext false
        val updated = repairBookCoverRecord(book, force)
        if (updated == book) return@withContext false
        _books.value = _books.value.map { if (it.id == bookId) updated else it }
        saveLibrary(_books.value)
        true
    }

    private fun repairBookCoverRecord(book: BookRecord, force: Boolean): BookRecord {
        val source = File(book.filePath)
        if (!source.isFile) return book
        val existing = book.coverPath.takeIf { it.isNotBlank() }?.let(::File)
        if (!force && existing?.isFile == true) return book
        if (existing?.name?.contains(".custom", ignoreCase = true) == true && existing.isFile) return book

        val smart = SmartBookImport.inspect(source, book.format, book.originalName)
        val metadata = smart.metadata
        val title = if (book.metadataLocked) book.title else metadata.title.ifBlank { book.title }
        val author = if (book.metadataLocked) book.author else metadata.author.ifBlank { book.author }
        val cover = createAutomaticCover(book.id, source, book.format, smart.coverBytes, title, author)
        if (cover.isBlank()) return book
        if (existing != null && existing.absolutePath != cover && existing.isFile) runCatching { existing.delete() }
        return if (book.metadataLocked) {
            book.copy(coverPath = cover)
        } else {
            book.copy(
                coverPath = cover,
                title = title,
                author = author,
                series = metadata.series.ifBlank { book.series },
                seriesIndex = metadata.seriesIndex.takeIf { it > 0f } ?: book.seriesIndex,
                description = metadata.description.ifBlank { book.description },
                publisher = metadata.publisher.ifBlank { book.publisher },
                language = metadata.language.ifBlank { book.language },
                year = metadata.year.ifBlank { book.year },
            )
        }
    }

    private fun createAutomaticCover(
        bookId: String,
        source: File,
        format: BookFormat,
        embeddedBytes: ByteArray?,
        title: String,
        author: String,
    ): String {
        embeddedBytes?.takeIf { it.isNotEmpty() }?.let { bytes ->
            val target = File(coversDir, "$bookId.auto.jpg")
            if (writeNormalizedCover(bytes, target)) return target.absolutePath
        }
        if (format == BookFormat.PDF) {
            createPdfCover(bookId, source)?.let { return it }
        }
        return createGeneratedCover(bookId, title, author, format)
    }

    private fun createPdfCover(bookId: String, source: File): String? = runCatching {
        val rendered = PdfRendererEngine.render(source, 0, 720)
        val target = File(coversDir, "$bookId.pdf.jpg")
        target.outputStream().buffered().use { output ->
            check(rendered.bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output))
        }
        rendered.bitmap.recycle()
        target.absolutePath
    }.getOrNull()

    private fun writeNormalizedCover(bytes: ByteArray, target: File): Boolean = runCatching {
        require(bytes.isNotEmpty() && bytes.size <= 20 * 1024 * 1024)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        require(bounds.outWidth in 1..16_384 && bounds.outHeight in 1..16_384)
        var sample = 1
        while (bounds.outWidth / sample > 1800 || bounds.outHeight / sample > 2400) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(
            bytes, 0, bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample.coerceAtLeast(1); inPreferredConfig = Bitmap.Config.ARGB_8888 },
        ) ?: return@runCatching false
        target.parentFile?.mkdirs()
        target.outputStream().buffered().use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output))
        }
        bitmap.recycle()
        target.isFile && target.length() > 0L
    }.getOrElse { false }

    private fun createGeneratedCover(bookId: String, title: String, author: String, format: BookFormat): String {
        val target = File(coversDir, "$bookId.generated.jpg")
        runCatching {
            val width = 720
            val height = 1050
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val seed = (title + author).hashCode()
            val hue = ((seed.toLong() and 0x7fffffffL) % 360L).toFloat()
            val top = Color.HSVToColor(floatArrayOf(hue, 0.44f, 0.42f))
            val bottom = Color.HSVToColor(floatArrayOf((hue + 28f) % 360f, 0.62f, 0.20f))
            val background = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), top, bottom, Shader.TileMode.CLAMP)
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), background)
            val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(55, 255, 255, 255) }
            canvas.drawCircle(width * 0.82f, height * 0.16f, 210f, accent)
            canvas.drawCircle(width * 0.12f, height * 0.88f, 260f, accent)

            val formatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(205, 255, 255, 255); textSize = 34f; isFakeBoldText = true }
            canvas.drawText(format.name, 58f, 82f, formatPaint)
            val initialPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 210f; isFakeBoldText = true }
            val initial = title.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "F"
            canvas.drawText(initial, 54f, 350f, initialPaint)
            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 54f; isFakeBoldText = true }
            drawWrapped(canvas, title.ifBlank { "Книга" }, titlePaint, 58f, 505f, width - 116f, 4, 66f)
            if (author.isNotBlank()) {
                val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(215, 255, 255, 255); textSize = 34f }
                drawWrapped(canvas, author, authorPaint, 58f, height - 118f, width - 116f, 2, 44f)
            }
            target.outputStream().buffered().use { output -> check(bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output)) }
            bitmap.recycle()
        }
        return target.takeIf { it.isFile && it.length() > 0L }?.absolutePath.orEmpty()
    }

    private fun drawWrapped(canvas: Canvas, text: String, paint: Paint, x: Float, y: Float, maxWidth: Float, maxLines: Int, lineHeight: Float) {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        var line = ""
        var lineIndex = 0
        fun flush() {
            if (line.isBlank() || lineIndex >= maxLines) return
            canvas.drawText(line.trim(), x, y + lineIndex * lineHeight, paint)
            lineIndex++
            line = ""
        }
        for (word in words) {
            val candidate = if (line.isBlank()) word else "$line $word"
            if (paint.measureText(candidate) <= maxWidth) line = candidate else {
                flush()
                if (lineIndex >= maxLines) break
                line = word
            }
        }
        flush()
    }

    private fun readLimited(input: java.io.InputStream, limit: Int): ByteArray {
        val out = java.io.ByteArrayOutputStream(min(limit, 256 * 1024))
        val buffer = ByteArray(64 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            total += read
            require(total <= limit) { "Обложка слишком большая" }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    fun updateReaderPosition(
        bookId: String,
        chapter: Int,
        blockIndex: Int,
        blockOffsetPx: Int,
        progress: Float,
        blockCharOffset: Int = 0,
    ) {
        updatePosition(bookId, progress) { book ->
            ReadingPositionPolicy.readerCheckpoint(book, chapter, blockIndex, blockOffsetPx, blockCharOffset)
        }
    }

    fun updateSpeedPosition(bookId: String, chapter: Int, offset: Int, progress: Float) {
        updatePosition(bookId, progress) { book ->
            ReadingPositionPolicy.speedCheckpoint(book, chapter, offset)
        }
    }

    fun updatePdfPosition(bookId: String, page: Int, progress: Float) {
        updatePosition(bookId, progress) { book -> book.copy(pdfPage = page.coerceAtLeast(0),
            readingPositionSource = ReadingPositionSource.PDF) }
    }

    private fun updatePosition(bookId: String, progress: Float, transform: (BookRecord) -> BookRecord) {
        val current = _books.value
        val index = current.indexOfFirst { it.id == bookId }
        if (index < 0) return
        val previous = current[index]
        val updated = transform(previous).copy(
            progress = progress.coerceIn(0f, 1f),
            lastOpenedAt = System.currentTimeMillis(),
        )
        if (updated == previous) return
        val next = current.toMutableList()
        next[index] = updated
        _books.value = next
        persistSoon()
    }

    fun touch(bookId: String) {
        val current = _books.value
        val index = current.indexOfFirst { it.id == bookId }
        if (index < 0) return
        val previous = current[index]
        val next = current.toMutableList()
        next[index] = previous.copy(lastOpenedAt = System.currentTimeMillis())
        _books.value = next
        persistSoon()
    }

    fun delete(bookId: String) {
        val book = _books.value.firstOrNull { it.id == bookId } ?: return
        ioScope.launch {
            runCatching { File(book.filePath).delete() }
            runCatching { if (book.coverPath.isNotBlank()) File(book.coverPath).delete() }
        }
        _books.value = _books.value.filterNot { it.id == bookId }
        persistSoon()
    }

    fun deleteBooks(bookIds: Collection<String>): Int {
        val ids = bookIds.toHashSet()
        if (ids.isEmpty()) return 0
        val current = _books.value
        val removed = current.filter { it.id in ids }
        if (removed.isEmpty()) return 0
        ioScope.launch {
            removed.forEach { book ->
                runCatching { File(book.filePath).delete() }
                runCatching { if (book.coverPath.isNotBlank()) File(book.coverPath).delete() }
            }
        }
        _books.value = current.filterNot { it.id in ids }
        persistSoon()
        return removed.size
    }

    fun folderMembershipSnapshot(bookIds: Collection<String>): Map<String, List<String>> {
        val ids = bookIds.toHashSet()
        if (ids.isEmpty()) return emptyMap()
        return _books.value.asSequence()
            .filter { it.id in ids }
            .associate { it.id to it.folderPaths.toList() }
    }

    fun restoreFolderMembership(snapshot: Map<String, List<String>>): Int {
        if (snapshot.isEmpty()) return 0
        var changed = 0
        val known = LibraryFolderTree.allFolders(_books.value, _folders.value).map { it.lowercase() }.toSet()
        _books.value = _books.value.map { book ->
            val previous = snapshot[book.id] ?: return@map book
            val restored = previous
                .map(LibraryFolderTree::normalize)
                .filter { it.isNotBlank() && it.lowercase() in known }
                .distinctBy { it.lowercase() }
            if (restored == book.folderPaths) book else {
                changed++
                book.copy(folderPaths = restored)
            }
        }
        if (changed > 0) persistSoon()
        return changed
    }

    fun createFolder(parentPath: String, requestedName: String): String {
        val parent = LibraryFolderTree.normalize(parentPath)
        require(parent.isBlank() || LibraryFolderTree.containsFolder(_books.value, parent, _folders.value)) {
            "Родительская папка больше не существует"
        }
        val cleanName = sanitizeFolderSegment(requestedName)
        val known = LibraryFolderTree.allFolders(_books.value, _folders.value)
        require(!LibraryFolderTree.siblingNameExists(known, parent, cleanName)) {
            "Папка «$cleanName» уже существует"
        }
        val path = LibraryFolderTree.join(parent, cleanName)
        registerFolders(listOf(path))
        return path
    }

    fun renameFolder(folderPath: String, requestedName: String): String {
        val source = LibraryFolderTree.normalize(folderPath)
        require(source.isNotBlank()) { "Корневую папку переименовать нельзя" }
        val known = LibraryFolderTree.allFolders(_books.value, _folders.value)
        require(known.any { it.equals(source, ignoreCase = true) }) { "Папка больше не существует" }
        val parent = LibraryFolderTree.parent(source)
        val cleanName = sanitizeFolderSegment(requestedName)
        require(!LibraryFolderTree.siblingNameExists(known, parent, cleanName, excludingPath = source)) {
            "Папка «$cleanName» уже существует"
        }
        val destination = LibraryFolderTree.join(parent, cleanName)
        if (destination == source) return source
        remapFolderPrefix(source, destination)
        return destination
    }

    fun moveFolder(folderPath: String, targetParentPath: String): String {
        val source = LibraryFolderTree.normalize(folderPath)
        val targetParent = LibraryFolderTree.normalize(targetParentPath)
        require(source.isNotBlank()) { "Корневую папку переместить нельзя" }
        val known = LibraryFolderTree.allFolders(_books.value, _folders.value)
        require(known.any { it.equals(source, ignoreCase = true) }) { "Папка больше не существует" }
        require(targetParent.isBlank() || known.any { it.equals(targetParent, ignoreCase = true) }) {
            "Папка назначения больше не существует"
        }
        require(!LibraryFolderTree.isSameOrDescendant(targetParent, source)) {
            "Нельзя переместить папку внутрь самой себя"
        }
        val destination = LibraryFolderTree.join(targetParent, LibraryFolderTree.name(source))
        if (destination == source) return source
        require(!known.any { it.equals(destination, ignoreCase = true) }) {
            "В папке назначения уже есть «${LibraryFolderTree.name(source)}»"
        }
        remapFolderPrefix(source, destination)
        return destination
    }

    fun copyFolder(folderPath: String, targetParentPath: String): String {
        val source = LibraryFolderTree.normalize(folderPath)
        val targetParent = LibraryFolderTree.normalize(targetParentPath)
        require(source.isNotBlank()) { "Корневую папку копировать нельзя" }
        val known = LibraryFolderTree.allFolders(_books.value, _folders.value)
        require(known.any { it.equals(source, ignoreCase = true) }) { "Папка больше не существует" }
        require(targetParent.isBlank() || known.any { it.equals(targetParent, ignoreCase = true) }) {
            "Папка назначения больше не существует"
        }
        require(!LibraryFolderTree.isSameOrDescendant(targetParent, source)) {
            "Нельзя копировать папку внутрь самой себя"
        }
        val preferred = "${LibraryFolderTree.name(source)} — копия"
        val copyName = LibraryFolderTree.uniqueSiblingName(known, targetParent, preferred)
        val destinationRoot = LibraryFolderTree.join(targetParent, copyName)
        val sourceFolders = known.filter { LibraryFolderTree.isSameOrDescendant(it, source) }
        val copiedFolders = sourceFolders.map { LibraryFolderTree.replacePrefix(it, source, destinationRoot) }
        val updatedBooks = _books.value.map { book ->
            val extra = book.folderPaths
                .map(LibraryFolderTree::normalize)
                .filter { LibraryFolderTree.isSameOrDescendant(it, source) }
                .map { LibraryFolderTree.replacePrefix(it, source, destinationRoot) }
            if (extra.isEmpty()) book else book.copy(
                folderPaths = (book.folderPaths + extra)
                    .map(LibraryFolderTree::normalize)
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase() },
            )
        }
        _books.value = updatedBooks
        _folders.value = LibraryFolderTree.allFolders(updatedBooks, _folders.value + copiedFolders)
        persistSoon()
        persistFoldersSoon()
        return destinationRoot
    }

    /** Removes a virtual folder but preserves its books by moving its whole subtree one level up. */
    fun deleteFolder(folderPath: String): Int {
        val source = LibraryFolderTree.normalize(folderPath)
        require(source.isNotBlank()) { "Корневую папку удалить нельзя" }
        val known = LibraryFolderTree.allFolders(_books.value, _folders.value)
        require(known.any { it.equals(source, ignoreCase = true) }) { "Папка больше не существует" }
        val parent = LibraryFolderTree.parent(source)
        var affected = 0
        val updatedBooks = _books.value.map { book ->
            var changed = false
            val next = book.folderPaths.map { raw ->
                val path = LibraryFolderTree.normalize(raw)
                if (LibraryFolderTree.isSameOrDescendant(path, source)) {
                    changed = true
                    LibraryFolderTree.replacePrefix(path, source, parent)
                } else path
            }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
            if (changed) {
                affected++
                book.copy(folderPaths = next)
            } else book
        }
        val remainingExplicit = _folders.value.mapNotNull { raw ->
            val path = LibraryFolderTree.normalize(raw)
            when {
                path == source -> null
                LibraryFolderTree.isSameOrDescendant(path, source) ->
                    LibraryFolderTree.replacePrefix(path, source, parent).takeIf { it.isNotBlank() }
                else -> path
            }
        }
        _books.value = updatedBooks
        _folders.value = LibraryFolderTree.allFolders(updatedBooks, remainingExplicit)
        persistSoon()
        persistFoldersSoon()
        return affected
    }

    fun copyBookToFolder(bookId: String, targetFolderPath: String) {
        val target = LibraryFolderTree.normalize(targetFolderPath)
        require(target.isNotBlank()) {
            "Для отдельной копии в корне используйте «Дублировать»"
        }
        require(LibraryFolderTree.containsFolder(_books.value, target, _folders.value)) {
            "Папка назначения больше не существует"
        }
        _books.value = _books.value.map { book ->
            if (book.id != bookId) book else book.copy(
                folderPaths = (book.folderPaths + target)
                    .map(LibraryFolderTree::normalize)
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase() },
            )
        }
        persistSoon()
    }

    fun copyBooksToFolder(bookIds: Collection<String>, targetFolderPath: String): Int {
        val ids = bookIds.toHashSet()
        if (ids.isEmpty()) return 0
        val target = LibraryFolderTree.normalize(targetFolderPath)
        require(target.isNotBlank()) {
            "Для отдельных копий в корне используйте «Дублировать»"
        }
        require(LibraryFolderTree.containsFolder(_books.value, target, _folders.value)) {
            "Папка назначения больше не существует"
        }
        var changed = 0
        _books.value = _books.value.map { book ->
            if (book.id !in ids || book.folderPaths.any { it.equals(target, ignoreCase = true) }) return@map book
            changed++
            book.copy(
                folderPaths = (book.folderPaths + target)
                    .map(LibraryFolderTree::normalize)
                    .filter { it.isNotBlank() }
                    .distinctBy { it.lowercase() },
            )
        }
        if (changed > 0) persistSoon()
        return changed
    }

    fun moveBookToFolder(bookId: String, sourceFolderPath: String?, targetFolderPath: String) {
        val source = sourceFolderPath?.let(LibraryFolderTree::normalize)
        val target = LibraryFolderTree.normalize(targetFolderPath)
        require(target.isBlank() || LibraryFolderTree.containsFolder(_books.value, target, _folders.value)) {
            "Папка назначения больше не существует"
        }
        _books.value = _books.value.map { book ->
            if (book.id != bookId) return@map book
            val current = book.folderPaths.map(LibraryFolderTree::normalize).filter { it.isNotBlank() }
            val retained = when {
                source == null -> emptyList()
                source.isBlank() && current.isEmpty() -> emptyList()
                source.isNotBlank() -> current.filterNot { it.equals(source, ignoreCase = true) }
                else -> current
            }
            val next = (retained + listOfNotNull(target.takeIf { it.isNotBlank() }))
                .distinctBy { it.lowercase() }
            book.copy(folderPaths = next)
        }
        persistSoon()
    }

    fun moveBooksToFolder(bookIds: Collection<String>, sourceFolderPath: String?, targetFolderPath: String): Int {
        val ids = bookIds.toHashSet()
        if (ids.isEmpty()) return 0
        val source = sourceFolderPath?.let(LibraryFolderTree::normalize)
        val target = LibraryFolderTree.normalize(targetFolderPath)
        require(target.isBlank() || LibraryFolderTree.containsFolder(_books.value, target, _folders.value)) {
            "Папка назначения больше не существует"
        }
        var changed = 0
        _books.value = _books.value.map { book ->
            if (book.id !in ids) return@map book
            val current = book.folderPaths.map(LibraryFolderTree::normalize).filter { it.isNotBlank() }
            val retained = when {
                source == null -> emptyList()
                source.isBlank() && current.isEmpty() -> emptyList()
                source.isNotBlank() -> current.filterNot { it.equals(source, ignoreCase = true) }
                else -> current
            }
            val next = (retained + listOfNotNull(target.takeIf { it.isNotBlank() }))
                .distinctBy { it.lowercase() }
            if (next == current) book else {
                changed++
                book.copy(folderPaths = next)
            }
        }
        if (changed > 0) persistSoon()
        return changed
    }

    suspend fun duplicateBook(bookId: String, targetFolderPath: String? = null): BookRecord = withContext(Dispatchers.IO) {
        val sourceBook = find(bookId) ?: error("Книга больше не существует")
        val sourceFile = File(sourceBook.filePath)
        require(sourceFile.isFile) { "Исходный файл книги не найден" }
        val targetFolder = targetFolderPath?.let(LibraryFolderTree::normalize)
        if (!targetFolder.isNullOrBlank()) {
            require(LibraryFolderTree.containsFolder(_books.value, targetFolder, _folders.value)) {
                "Папка назначения больше не существует"
            }
        }
        val newId = UUID.randomUUID().toString()
        val suffix = sourceFile.name.removePrefix(sourceBook.id).takeIf { it.startsWith('.') }
            ?: ".${sourceFile.extension.ifBlank { sourceBook.format.name.lowercase() }}"
        val targetFile = File(booksDir, "$newId$suffix")
        sourceFile.copyTo(targetFile, overwrite = false)
        var copiedCover = ""
        try {
            if (sourceBook.coverPath.isNotBlank()) {
                val coverSource = File(sourceBook.coverPath)
                if (coverSource.isFile) {
                    val coverSuffix = coverSource.name.substringAfter(sourceBook.id, missingDelimiterValue = "")
                        .takeIf { it.startsWith('.') } ?: ".jpg"
                    val coverTarget = File(coversDir, "$newId$coverSuffix")
                    coverSource.copyTo(coverTarget, overwrite = false)
                    copiedCover = coverTarget.absolutePath
                }
            }
            val duplicateTitle = uniqueDuplicateTitle(sourceBook.title)
            val duplicate = sourceBook.copy(
                id = newId,
                title = duplicateTitle,
                filePath = targetFile.absolutePath,
                originalName = duplicateFileName(sourceBook.originalName),
                folderPaths = when {
                    targetFolderPath == null -> sourceBook.folderPaths
                    targetFolder.isNullOrBlank() -> emptyList()
                    else -> listOf(targetFolder)
                },
                coverPath = copiedCover,
                progress = 0f,
                readerChapter = 0,
                readerBlockIndex = 0,
                readerBlockOffsetPx = 0,
                readerBlockCharOffset = 0,
                speedChapter = 0,
                speedOffset = 0,
                readingPositionSource = null,
                pdfPage = 0,
                lastOpenedAt = 0L,
                addedAt = System.currentTimeMillis(),
            )
            _books.value = _books.value + duplicate
            persistSoon()
            duplicate
        } catch (error: Throwable) {
            runCatching { targetFile.delete() }
            if (copiedCover.isNotBlank()) runCatching { File(copiedCover).delete() }
            throw error
        }
    }

    suspend fun exportBook(bookId: String, destination: Uri) = withContext(Dispatchers.IO) {
        val book = find(bookId) ?: error("Книга больше не существует")
        val source = File(book.filePath)
        require(source.isFile) { "Файл книги не найден" }
        context.contentResolver.openOutputStream(destination, "w")?.use { output ->
            source.inputStream().buffered().use { input -> input.copyTo(output, 128 * 1024) }
        } ?: error("Не удалось открыть файл назначения")
    }

    private fun remapFolderPrefix(source: String, destination: String) {
        val updatedBooks = _books.value.map { book ->
            val next = book.folderPaths.map { raw ->
                val path = LibraryFolderTree.normalize(raw)
                if (LibraryFolderTree.isSameOrDescendant(path, source)) {
                    LibraryFolderTree.replacePrefix(path, source, destination)
                } else path
            }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }
            if (next == book.folderPaths) book else book.copy(folderPaths = next)
        }
        val remappedExplicit = _folders.value.map { raw ->
            val path = LibraryFolderTree.normalize(raw)
            if (LibraryFolderTree.isSameOrDescendant(path, source)) {
                LibraryFolderTree.replacePrefix(path, source, destination)
            } else path
        }
        _books.value = updatedBooks
        _folders.value = LibraryFolderTree.allFolders(updatedBooks, remappedExplicit)
        persistSoon()
        persistFoldersSoon()
    }

    private fun registerFolders(paths: Collection<String>) {
        _folders.value = LibraryFolderTree.allFolders(_books.value, _folders.value + paths)
        persistFoldersSoon()
    }

    private fun uniqueDuplicateTitle(title: String): String {
        val existing = _books.value.map { it.title.lowercase() }.toSet()
        val base = "$title — копия"
        if (base.lowercase() !in existing) return base
        var index = 2
        while (true) {
            val candidate = "$title — копия $index"
            if (candidate.lowercase() !in existing) return candidate
            index++
        }
    }

    private fun duplicateFileName(name: String): String {
        val lower = name.lowercase()
        val extension = when {
            lower.endsWith(".fb2.zip") -> name.takeLast(8)
            name.contains('.') -> ".${name.substringAfterLast('.')}"
            else -> ""
        }
        val stem = if (extension.isBlank()) name else name.dropLast(extension.length)
        return "$stem — копия$extension"
    }

    private fun queryTreeChildren(treeUri: Uri, parentDocumentId: String): List<TreeEntry> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        return buildList {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                require(idIndex >= 0 && nameIndex >= 0 && mimeIndex >= 0) {
                    "Провайдер файлов вернул неполные сведения о папке"
                }
                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIndex).orEmpty()
                    if (id.isBlank()) continue
                    add(
                        TreeEntry(
                            documentId = id,
                            name = cursor.getString(nameIndex).orEmpty().ifBlank { "Без имени" },
                            mimeType = cursor.getString(mimeIndex).orEmpty(),
                        ),
                    )
                }
            } ?: error("Провайдер файлов не вернул содержимое папки")
        }
    }

    private fun sanitizeFolderSegment(value: String): String = value
        .replace(Regex("[\\/\\p{Cntrl}]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(120)
        .ifBlank { "Папка" }

    private fun contentLength(uri: Uri): Long = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0).coerceAtLeast(0L) else 0L
        } ?: 0L
    }.getOrDefault(0L)

    private fun displayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun detectMime(mime: String?): BookFormat? = when (mime?.lowercase()) {
        "application/epub+zip" -> BookFormat.EPUB
        "application/pdf" -> BookFormat.PDF
        "application/x-fictionbook+xml", "application/fb2+xml" -> BookFormat.FB2
        "text/plain" -> BookFormat.TXT
        else -> null
    }

    private fun detectFormat(name: String): BookFormat? = ArchiveImportPolicy.formatForName(name)

    private fun loadLibrary(): List<BookRecord> = runCatching {
        if (!libraryFile.isFile) return@runCatching emptyList()
        val array = JSONArray(libraryFile.readText())
        buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val path = obj.optString("filePath")
                if (path.isBlank() || !File(path).isFile) continue
                val format = runCatching { BookFormat.valueOf(obj.optString("format")) }.getOrNull()
                    ?: detectFormat(obj.optString("originalName"))
                    ?: continue
                val legacyChapter = obj.optInt("currentChapter", 0).coerceAtLeast(0)
                val legacyBlockIndex = if (obj.has("readerBlockIndex")) obj.optInt("readerBlockIndex", 0) else 0
                add(
                    BookRecord(
                        id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        title = obj.optString("title").ifBlank { File(path).nameWithoutExtension },
                        author = obj.optString("author"),
                        filePath = path,
                        originalName = obj.optString("originalName").ifBlank { File(path).name },
                        format = format,
                        series = obj.optString("series"),
                        seriesIndex = obj.optDouble("seriesIndex", 0.0).toFloat().coerceAtLeast(0f),
                        groups = obj.optJSONArray("groups").toStringList(),
                        folderPaths = obj.optJSONArray("folderPaths").toStringList()
                            .map(LibraryFolderTree::normalize)
                            .filter { it.isNotBlank() }
                            .distinctBy { it.lowercase() },
                        coverPath = obj.optString("coverPath").takeIf { it.isNotBlank() && File(it).isFile }.orEmpty(),
                        description = obj.optString("description"),
                        publisher = obj.optString("publisher"),
                        language = obj.optString("language"),
                        year = obj.optString("year"),
                        contentHash = obj.optString("contentHash"),
                        metadataLocked = obj.optBoolean("metadataLocked", false),
                        progress = obj.optDouble("progress", 0.0).toFloat().coerceIn(0f, 1f),
                        readerChapter = obj.optInt("readerChapter", legacyChapter).coerceAtLeast(0),
                        readerBlockIndex = legacyBlockIndex.coerceAtLeast(0),
                        readerBlockOffsetPx = obj.optInt("readerBlockOffsetPx", 0).coerceAtLeast(0),
                        readerBlockCharOffset = obj.optInt("readerBlockCharOffset", 0).coerceAtLeast(0),
                        speedChapter = obj.optInt("speedChapter", legacyChapter).coerceAtLeast(0),
                        speedOffset = obj.optInt("speedOffset", 0).coerceAtLeast(0),
                        readingPositionSource = ReadingPositionSource.entries.firstOrNull {
                            it.name == obj.optString("readingPositionSource")
                        },
                        pdfPage = obj.optInt("pdfPage", legacyChapter).coerceAtLeast(0),
                        chapterCount = obj.optInt("chapterCount", 0).coerceAtLeast(0),
                        lastOpenedAt = obj.optLong("lastOpenedAt", 0L),
                        addedAt = obj.optLong("addedAt", obj.optLong("lastOpenedAt", 0L)),
                    )
                )
            }
        }.sortedByDescending { it.lastOpenedAt }
    }.getOrDefault(emptyList())

    private fun loadFolders(books: List<BookRecord>): List<String> = runCatching {
        val stored = if (foldersFile.isFile) {
            val array = JSONArray(foldersFile.readText())
            buildList {
                for (i in 0 until array.length()) {
                    array.optString(i).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        } else emptyList()
        LibraryFolderTree.allFolders(books, stored)
    }.getOrElse {
        LibraryFolderTree.allFolders(books)
    }

    private fun persistFoldersSoon() {
        val snapshot = _folders.value
        folderPersistJob?.cancel()
        folderPersistJob = ioScope.launch {
            delay(180)
            saveFolders(snapshot)
        }
    }

    private fun saveFolders(items: List<String>) {
        runCatching {
            val normalized = LibraryFolderTree.allFolders(_books.value, items)
            val array = JSONArray()
            normalized.forEach(array::put)
            val tmp = File(foldersFile.parentFile, "library-folders.json.tmp")
            tmp.writeText(array.toString())
            if (foldersFile.exists() && !foldersFile.delete()) return@runCatching
            if (!tmp.renameTo(foldersFile)) {
                foldersFile.writeText(tmp.readText())
                tmp.delete()
            }
        }
    }

    /** Flush a reading checkpoint without the 180 ms debounce used for live progress.
     * The I/O still runs off the UI thread. A pending debounced snapshot cannot
     * overwrite this newer one because both writes use the same persistJob.
     */
    fun flushPendingPositions() {
        val snapshot = _books.value
        persistJob?.cancel()
        persistJob = ioScope.launch { saveLibrary(snapshot) }
    }

    private fun persistSoon() {
        val snapshot = _books.value
        persistJob?.cancel()
        persistJob = ioScope.launch {
            delay(180)
            saveLibrary(snapshot)
        }
    }

    private fun saveLibrary(items: List<BookRecord>) {
        runCatching {
            val array = JSONArray()
            items.forEach { book ->
                array.put(
                    JSONObject()
                        .put("id", book.id)
                        .put("title", book.title)
                        .put("author", book.author)
                        .put("filePath", book.filePath)
                        .put("originalName", book.originalName)
                        .put("format", book.format.name)
                        .put("series", book.series)
                        .put("seriesIndex", book.seriesIndex.toDouble())
                        .put("groups", JSONArray(book.groups))
                        .put("folderPaths", JSONArray(book.folderPaths))
                        .put("coverPath", book.coverPath)
                        .put("description", book.description)
                        .put("publisher", book.publisher)
                        .put("language", book.language)
                        .put("year", book.year)
                        .put("contentHash", book.contentHash)
                        .put("metadataLocked", book.metadataLocked)
                        .put("progress", book.progress.toDouble())
                        .put("currentChapter", book.readerChapter)
                        .put("currentOffset", book.readerBlockIndex)
                        .put("readerChapter", book.readerChapter)
                        .put("readerBlockIndex", book.readerBlockIndex)
                        .put("readerBlockOffsetPx", book.readerBlockOffsetPx)
                        .put("readerBlockCharOffset", book.readerBlockCharOffset)
                        .put("speedChapter", book.speedChapter)
                        .put("speedOffset", book.speedOffset)
                        .put("readingPositionSource", book.readingPositionSource?.name.orEmpty())
                        .put("pdfPage", book.pdfPage)
                        .put("chapterCount", book.chapterCount)
                        .put("lastOpenedAt", book.lastOpenedAt)
                        .put("addedAt", book.addedAt)
                )
            }
            val tmp = File(libraryFile.parentFile, "library.json.tmp")
            tmp.writeText(array.toString())
            if (libraryFile.exists() && !libraryFile.delete()) return@runCatching
            if (!tmp.renameTo(libraryFile)) {
                libraryFile.writeText(tmp.readText())
                tmp.delete()
            }
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(128 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                if (read > 0) digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val MAX_TREE_DOCUMENTS = 20_000
        const val MAX_FOLDER_DEPTH = 64
    }

    private fun String.removeBookExtension(): String = this
        .replace(Regex("(?i)\\.fb2\\.zip$"), "")
        .replace(Regex("(?i)\\.(epub|fb2|txt|pdf)$"), "")
}
