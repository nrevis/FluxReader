package io.fluxreader.app

import android.content.ComponentCallbacks2
import android.app.ActivityManager
import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.SystemClock
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.fluxreader.app.audio.AudiobookController
import io.fluxreader.app.update.GitHubUpdateManager
import io.fluxreader.app.data.BookFormat
import io.fluxreader.app.data.BookMetadata
import io.fluxreader.app.data.BookOutline
import io.fluxreader.app.data.ReadingPositionPolicy
import io.fluxreader.app.data.ReadingPositionSource
import io.fluxreader.app.data.BookRecord
import io.fluxreader.app.data.ChapterContent
import io.fluxreader.app.data.LibraryRepository
import io.fluxreader.app.data.LibraryFolderTree
import io.fluxreader.app.data.LibraryCoverCache
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.ImportPhase
import io.fluxreader.app.data.SourceImportProgress
import io.fluxreader.app.data.ReaderSettingsRepository
import io.fluxreader.app.data.ReaderAnnotationsRepository
import io.fluxreader.app.data.ReaderAnnotation
import io.fluxreader.app.data.ReaderAnnotationKind
import io.fluxreader.app.data.SpeedMode
import io.fluxreader.app.reader.BookContentRepository
import io.fluxreader.app.reader.BookDialogueRepository
import io.fluxreader.app.reader.ChapterDialogueIndex
import io.fluxreader.app.reader.ReaderMemoryBudget
import io.fluxreader.app.reader.ChapterSpeedOffset
import io.fluxreader.app.reader.ReaderProgressMath
import io.fluxreader.app.reader.SpeedChunk
import io.fluxreader.app.reader.SpeedHeadingCue
import io.fluxreader.app.reader.SpeedMediaCue
import io.fluxreader.app.reader.SpeedDialoguePlayback
import io.fluxreader.app.reader.SpeedReaderCursor
import io.fluxreader.app.reader.SearchEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import java.io.File

/** One route, one state holder. Heavy work never runs inside Compose. */
enum class AppRoute { LIBRARY, READER, PDF, SPEED, SETTINGS, AUDIOBOOKS }

data class ReaderUiState(
    val bookId: String? = null,
    val loading: Boolean = false,
    val outline: BookOutline? = null,
    val chapterIndex: Int = 0,
    val chapter: ChapterContent? = null,
    val initialBlockIndex: Int = 0,
    val initialBlockOffsetPx: Int = 0,
    val initialBlockCharOffset: Int = 0,
    val error: String? = null,
)

data class PdfUiState(
    val bookId: String? = null,
    val loading: Boolean = false,
    val pageIndex: Int = 0,
    val pageCount: Int = 0,
    val bitmap: Bitmap? = null,
    val renderWidthPx: Int = 0,
    val error: String? = null,
)

data class SearchHit(
    val chapterIndex: Int,
    val blockIndex: Int,
    val title: String,
    val snippet: String,
)

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val scanned: Int = 0,
    val total: Int = 0,
    val hits: List<SearchHit> = emptyList(),
    val error: String? = null,
)

data class ImportUiState(
    val active: Boolean = false,
    val title: String = "",
    val detail: String = "",
    val current: Int = 0,
    val total: Int = 0,
    val progress: Float? = null,
)

data class SpeedPosition(val chapterIndex: Int, val offset: Int)

data class SpeedUiState(
    val bookId: String? = null,
    val loading: Boolean = false,
    val outline: BookOutline? = null,
    val chapterIndex: Int = 0,
    val chapterText: String = "",
    val dialogueIndex: ChapterDialogueIndex? = null,
    val dialogueAnalysisStatus: String? = null,
    val chunk: SpeedChunk? = null,
    val mediaCues: List<SpeedMediaCue> = emptyList(),
    val headingCues: List<SpeedHeadingCue> = emptyList(),
    val activeMedia: SpeedMediaCue? = null,
    val lastPresentedMediaOffset: Int = -1,
    val mediaResumePending: Boolean = false,
    val playing: Boolean = false,
    val history: List<SpeedPosition> = emptyList(),
    val status: String? = null,
    val error: String? = null,
)

private data class LoadedSpeedChapter(
    val chapterIndex: Int,
    val text: String,
    val chunk: SpeedChunk,
    val mediaCues: List<SpeedMediaCue>,
    val headingCues: List<SpeedHeadingCue>,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val updateManager = GitHubUpdateManager(application, viewModelScope)
    val githubUpdates = updateManager.state
    fun checkForUpdates() = updateManager.check()
    fun setAutoUpdateCheck(enabled: Boolean) = updateManager.setAutoCheck(enabled)
    fun setPreReleaseUpdates(enabled: Boolean) = updateManager.setIncludePreReleases(enabled)
    fun downloadGitHubUpdate() = updateManager.download()
    fun installGitHubUpdate() = updateManager.requestInstall()

    private val libraryRepository = LibraryRepository(application)
    private val settingsRepository = ReaderSettingsRepository(application)
    private val contentRepository = BookContentRepository(application)
    private val bookDialogueRepository = BookDialogueRepository(application.cacheDir, contentRepository)
    private val annotationsRepository = ReaderAnnotationsRepository(application)
    private val memoryClassMb = application.getSystemService(ActivityManager::class.java)?.memoryClass ?: 256
    private val imageCacheMaxKb = ReaderMemoryBudget.embeddedImageCacheKb(memoryClassMb)
    private val readerPrefetchRadius = ReaderMemoryBudget.chapterPrefetchRadius(memoryClassMb)
    private val imageDecodeSemaphore = Semaphore(ReaderMemoryBudget.embeddedImageDecodeParallelism(memoryClassMb))
    private val imageDecodeLocks = Array(32) { Mutex() }
    private val imageCache = object : LruCache<String, Bitmap>(imageCacheMaxKb) {
        override fun sizeOf(key: String, value: Bitmap): Int = (value.byteCount / 1024).coerceAtLeast(1)
    }

    private val audiobookController = AudiobookController(application)
    val audiobooks = audiobookController.state

    val books = libraryRepository.books
    val folders = libraryRepository.folders
    private val libraryUiPrefs = application.getSharedPreferences("library_ui_state", Application.MODE_PRIVATE)
    private val _libraryFolder = MutableStateFlow(
        LibraryFolderTree.normalize(libraryUiPrefs.getString("opened_folder_v1", "").orEmpty())
    )
    val libraryFolder: StateFlow<String> = _libraryFolder.asStateFlow()

    /** Persist the library navigation independently of the composable screen's lifecycle. */
    fun setLibraryFolder(path: String) {
        val normalized = LibraryFolderTree.normalize(path)
        if (_libraryFolder.value == normalized) return
        _libraryFolder.value = normalized
        libraryUiPrefs.edit().putString("opened_folder_v1", normalized).apply()
    }

    private fun rebaseLibraryFolder(source: String, destination: String) {
        val old = _libraryFolder.value
        if (LibraryFolderTree.isSameOrDescendant(old, source)) {
            setLibraryFolder(LibraryFolderTree.replacePrefix(old, source, destination))
        }
    }
    val settings = settingsRepository.settings
    val annotations = annotationsRepository.annotations

    private val _route = MutableStateFlow(AppRoute.LIBRARY)
    val route: StateFlow<AppRoute> = _route.asStateFlow()

    private val _reader = MutableStateFlow(ReaderUiState())
    val reader: StateFlow<ReaderUiState> = _reader.asStateFlow()

    private val _pdf = MutableStateFlow(PdfUiState())
    val pdf: StateFlow<PdfUiState> = _pdf.asStateFlow()

    private val _speed = MutableStateFlow(SpeedUiState())
    val speed: StateFlow<SpeedUiState> = _speed.asStateFlow()

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _importState = MutableStateFlow(ImportUiState())
    val importState: StateFlow<ImportUiState> = _importState.asStateFlow()

    private val _canUndoLibraryOperation = MutableStateFlow(false)
    val canUndoLibraryOperation: StateFlow<Boolean> = _canUndoLibraryOperation.asStateFlow()
    private var libraryFolderUndoSnapshot: Map<String, List<String>>? = null

    private var readerJob: Job? = null
    private var readerPrefetchJob: Job? = null
    private var speedJob: Job? = null
    private var bookDialogueJob: Job? = null
    private var bookDialogueGeneration = 0L
    private var speedPausedAtRealtimeMs: Long = 0L
    private var pdfJob: Job? = null
    private var pdfRenderGeneration: Long = 0L
    private var pdfPendingPage: Int = -1
    private var pdfPendingWidthPx: Int = 0
    private var searchJob: Job? = null
    private var importJob: Job? = null
    private var routeBeforeSpeed: AppRoute = AppRoute.LIBRARY
    private var routeBeforeSettings: AppRoute = AppRoute.LIBRARY

    fun clearMessage() { _message.value = null }

    fun addReaderAnnotation(
        bookId: String,
        chapterIndex: Int,
        blockIndex: Int,
        startCharOffset: Int,
        endCharOffsetExclusive: Int,
        excerpt: String,
        kind: ReaderAnnotationKind,
        colorArgb: Int = 0,
        note: String = "",
    ): ReaderAnnotation = annotationsRepository.add(
        bookId = bookId,
        chapterIndex = chapterIndex,
        blockIndex = blockIndex,
        startCharOffset = startCharOffset,
        endCharOffsetExclusive = endCharOffsetExclusive,
        excerpt = excerpt,
        kind = kind,
        colorArgb = colorArgb,
        note = note,
    )

    fun addReaderAnnotationRange(
        bookId: String,
        chapterIndex: Int,
        startBlockIndex: Int,
        startCharOffset: Int,
        endBlockIndex: Int,
        endCharOffsetExclusive: Int,
        excerpt: String,
        kind: ReaderAnnotationKind,
        colorArgb: Int = 0,
        note: String = "",
    ): ReaderAnnotation = annotationsRepository.addRange(
        bookId = bookId,
        chapterIndex = chapterIndex,
        startBlockIndex = startBlockIndex,
        startCharOffset = startCharOffset,
        endBlockIndex = endBlockIndex,
        endCharOffsetExclusive = endCharOffsetExclusive,
        excerpt = excerpt,
        kind = kind,
        colorArgb = colorArgb,
        note = note,
    )

    fun updateReaderAnnotationNote(id: String, note: String) = annotationsRepository.updateNote(id, note)

    fun updateReaderAnnotationColor(id: String, colorArgb: Int) = annotationsRepository.updateColor(id, colorArgb)

    fun updateReaderAnnotationRange(
        id: String,
        startBlockIndex: Int,
        startCharOffset: Int,
        endBlockIndex: Int,
        endCharOffsetExclusive: Int,
        excerpt: String,
    ) = annotationsRepository.updateRange(
        id = id,
        startBlockIndex = startBlockIndex,
        startCharOffset = startCharOffset,
        endBlockIndex = endBlockIndex,
        endCharOffsetExclusive = endCharOffsetExclusive,
        excerpt = excerpt,
    )

    fun openReaderAt(
        bookId: String,
        chapterIndex: Int,
        blockIndex: Int,
        blockCharOffset: Int,
    ) {
        val book = libraryRepository.find(bookId) ?: return
        if (book.format == BookFormat.PDF) {
            openPdf(bookId)
            return
        }
        libraryRepository.updateReaderPosition(
            bookId,
            chapterIndex.coerceAtLeast(0),
            blockIndex.coerceAtLeast(0),
            0,
            book.progress,
            blockCharOffset.coerceAtLeast(0),
        )
        openReader(bookId)
    }

    fun deleteReaderAnnotation(id: String) = annotationsRepository.delete(id)


    fun handleTrimMemory(level: Int) {
        when {
            level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                readerPrefetchJob?.cancel()
                readerPrefetchJob = null
                LibraryCoverCache.clear()
                imageCache.trimToSize((imageCache.maxSize() / 16).coerceAtLeast(256))
                contentRepository.trimSessionCaches()
            }
            level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN -> {
                readerPrefetchJob?.cancel()
                readerPrefetchJob = null
                LibraryCoverCache.trimForReader()
                imageCache.trimToSize((imageCache.maxSize() / 4).coerceAtLeast(512))
                contentRepository.trimSessionCaches()
            }
        }
    }

    private fun cancelPdfRender() {
        pdfRenderGeneration++
        pdfJob?.cancel()
        pdfJob = null
        pdfPendingPage = -1
        pdfPendingWidthPx = 0
    }

    fun showLibrary() {
        readerJob?.cancel()
        readerPrefetchJob?.cancel()
        cancelPdfRender()
        speedJob?.cancel()
        cancelBookDialogueAnalysis()
        searchJob?.cancel()
        releaseRouteStatesForLibrary()
        _route.value = AppRoute.LIBRARY
    }

    private val _narratorLaunchRequest = MutableStateFlow(false)
    val narratorLaunchRequest: StateFlow<Boolean> = _narratorLaunchRequest.asStateFlow()

    /** Switch modes within the common reading workspace without losing the anchor. */
    fun openReaderFromSpeed() {
        speedPause()
        restoreReaderFromSpeedPosition()
    }

    fun openNarrationFromReader() {
        _narratorLaunchRequest.value = true
    }

    fun openNarrationFromSpeed() {
        speedPause()
        _narratorLaunchRequest.value = true
        restoreReaderFromSpeedPosition()
    }

    fun consumeNarratorLaunchRequest() { _narratorLaunchRequest.value = false }

    fun showAudiobooks() {
        cancelBookDialogueAnalysis()
        if (_route.value == AppRoute.SPEED) speedPause()
        _route.value = AppRoute.AUDIOBOOKS
    }

    fun importAudiobooks(uris: List<Uri>, folderPath: String = "") {
        val imported = audiobookController.import(uris, folderPath)
        _message.value = if (imported > 0) "Добавлено аудиокниг: $imported" else "Аудиофайлы не добавлены"
    }

    fun playAudiobook(id: String) = audiobookController.play(id)
    fun toggleAudiobook() {
        val state = audiobookController.state.value
        if (state.playing) audiobookController.pause()
        else state.activeId?.let { audiobookController.play(it) }
    }
    fun skipAudiobook(deltaMs: Long) = audiobookController.skip(deltaMs)
    fun seekAudiobook(ms: Long) = audiobookController.seek(ms)
    fun removeAudiobook(id: String) = audiobookController.remove(id)
    fun moveAudiobook(id: String, folderPath: String) = audiobookController.move(id, folderPath)
    fun openAudiobook(id: String) {
        audiobookController.play(id)
        showAudiobooks()
    }

    fun showSettings() {
        cancelBookDialogueAnalysis()
        speedPause()
        if (_route.value != AppRoute.SETTINGS) routeBeforeSettings = _route.value
        _route.value = AppRoute.SETTINGS
    }

    fun navigateBack() {
        val current = _route.value
        val destination = AppBackDestinationPolicy.destination(current, routeBeforeSpeed, routeBeforeSettings)
        when (current) {
            AppRoute.SPEED -> {
                speedJob?.cancel()
                cancelBookDialogueAnalysis()
                speedPause()
                if (destination == AppRoute.READER) {
                    restoreReaderFromSpeedPosition()
                    return
                }
                if (destination == AppRoute.PDF) {
                    _speed.value.bookId?.let { openPdf(it) }
                    return
                }
                if (destination == AppRoute.LIBRARY) releaseRouteStatesForLibrary()
            }
            AppRoute.READER -> {
                readerJob?.cancel()
                readerPrefetchJob?.cancel()
                releaseRouteStatesForLibrary()
            }
            AppRoute.PDF -> {
                cancelPdfRender()
                releaseRouteStatesForLibrary()
            }
            AppRoute.AUDIOBOOKS -> audiobookController.pause()
            AppRoute.SETTINGS, AppRoute.LIBRARY -> Unit
        }
        _route.value = destination
        if (destination == AppRoute.SPEED) scheduleBookDialogueAnalysis()
    }

    private fun publishImportProgress(
        progress: SourceImportProgress,
        outerIndex: Int = 0,
        outerTotal: Int = 1,
    ) {
        val phaseTitle = when (progress.phase) {
            ImportPhase.PREPARING -> "Подготавливаю импорт"
            ImportPhase.COPYING_ARCHIVE -> "Открываю ZIP-архив"
            ImportPhase.IMPORTING_BOOKS -> "Импортирую книги"
            ImportPhase.SCANNING_FOLDER -> "Сканирую папку"
        }
        val fraction = when {
            progress.total > 0 -> progress.current.toFloat() / progress.total.toFloat()
            progress.bytesTotal > 0L -> progress.bytesCopied.toFloat() / progress.bytesTotal.toFloat()
            else -> null
        }?.coerceIn(0f, 1f)
        val outer = if (outerTotal > 1) "Файл ${outerIndex + 1}/$outerTotal · " else ""
        _importState.value = ImportUiState(
            active = true,
            title = outer + phaseTitle,
            detail = progress.label,
            current = progress.current,
            total = progress.total,
            progress = fraction,
        )
    }

    fun cancelImport() {
        val wasActive = importJob?.isActive == true || _importState.value.active
        importJob?.cancel()
        importJob = null
        _importState.value = ImportUiState()
        if (wasActive) _message.value = "Импорт отменён"
    }

    fun importBook(uri: Uri) = importBooks(listOf(uri))

    fun importBooks(uris: List<Uri>) {
        if (uris.isEmpty()) return
        if (importJob?.isActive == true) {
            _message.value = "Импорт уже выполняется"
            return
        }
        _importState.value = ImportUiState(active = true, title = "Подготавливаю импорт")
        importJob = launchHandled(
            onError = {
                _importState.value = ImportUiState()
                _message.value = it.userMessage("Не удалось импортировать книги")
            },
        ) {
            try {
                var added = 0
                var linkedExisting = 0
                var skippedCount = 0
                var archiveCount = 0
                var importedFolders = 0
                val errors = mutableListOf<String>()
                val distinctUris = uris.distinct()
                distinctUris.forEachIndexed { index, uri ->
                    currentCoroutineContext().ensureActive()
                    try {
                        val result = libraryRepository.importSource(uri) { progress ->
                            publishImportProgress(progress, index, distinctUris.size)
                        }
                        added += result.added
                        linkedExisting += result.linkedExisting
                        skippedCount += result.skipped
                        importedFolders += result.folderCount
                        if (result.archive) archiveCount++
                        if (result.firstError.isNotBlank()) errors += result.firstError
                        result.addedBookIds.forEach { id -> libraryRepository.find(id)?.let(::prepareMetadata) }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        skippedCount++
                        errors += error.userMessage("Не удалось импортировать файл")
                    }
                }
                if (importedFolders > 0) {
                    settingsRepository.update { it.copy(libraryGroupMode = io.fluxreader.app.data.LibraryGroupMode.FOLDER) }
                }
                _message.value = buildString {
                    append("Импорт: добавлено ").append(added)
                    if (archiveCount > 0) append(", архивов ").append(archiveCount)
                    if (linkedExisting > 0) append(", привязано существующих ").append(linkedExisting)
                    if (skippedCount > 0) append(", пропущено ").append(skippedCount)
                    if (errors.isNotEmpty()) append(" · ").append(errors.first())
                }
            } finally {
                _importState.value = ImportUiState()
            }
        }
    }

    fun importFolder(treeUri: Uri) {
        if (importJob?.isActive == true) {
            _message.value = "Импорт уже выполняется"
            return
        }
        _importState.value = ImportUiState(active = true, title = "Сканирую папку")
        importJob = launchHandled(
            onError = {
                _importState.value = ImportUiState()
                _message.value = it.userMessage("Не удалось импортировать папку")
            },
        ) {
            try {
                val result = libraryRepository.importFolder(treeUri) { progress ->
                    publishImportProgress(progress)
                }
                result.addedBookIds.forEach { id -> libraryRepository.find(id)?.let(::prepareMetadata) }
                if (result.folderCount > 0) {
                    settingsRepository.update { it.copy(libraryGroupMode = io.fluxreader.app.data.LibraryGroupMode.FOLDER) }
                }
                _message.value = buildString {
                    append("Папка: добавлено ").append(result.added)
                    if (result.linkedExisting > 0) append(", привязано существующих ").append(result.linkedExisting)
                    append(", папок ").append(result.folderCount)
                    if (result.skipped > 0) append(", ошибок ").append(result.skipped)
                    if (result.firstError.isNotBlank()) append(" · ").append(result.firstError)
                }
            } finally {
                _importState.value = ImportUiState()
            }
        }
    }

    fun editBookMetadata(bookId: String, metadata: BookMetadata, groups: List<String>) {
        libraryRepository.editMetadata(bookId, metadata, groups)
        _message.value = "Метаданные сохранены"
    }

    fun setBookCover(bookId: String, uri: Uri) {
        launchHandled(
            onError = { _message.value = it.userMessage("Не удалось установить обложку") },
        ) {
            libraryRepository.setCover(bookId, uri)
            _message.value = "Обложка обновлена"
        }
    }

    fun removeBookCover(bookId: String) {
        libraryRepository.removeCover(bookId)
        _message.value = "Возвращаю автоматическую обложку…"
    }

    fun refreshBookCovers() {
        launchHandled(
            onError = { _message.value = it.userMessage("Не удалось обновить обложки") },
        ) {
            val repaired = libraryRepository.repairMissingCovers(force = true)
            _message.value = if (repaired > 0) {
                "Обложки обновлены: $repaired"
            } else {
                "Обложки уже актуальны"
            }
        }
    }

    fun deleteBook(bookId: String) {
        val isActive = _reader.value.bookId == bookId || _pdf.value.bookId == bookId || _speed.value.bookId == bookId
        if (isActive) showLibrary()
        contentRepository.clear(bookId)
        imageCache.snapshot().keys.filter { it.startsWith("$bookId:") }.forEach(imageCache::remove)
        annotationsRepository.deleteForBook(bookId)
        libraryRepository.delete(bookId)
    }

    fun deleteBooks(bookIds: List<String>) {
        val ids = bookIds.distinct()
        if (ids.isEmpty()) return
        val activeBookId = _reader.value.bookId ?: _pdf.value.bookId ?: _speed.value.bookId
        if (activeBookId != null && activeBookId in ids) showLibrary()
        ids.forEach { bookId ->
            contentRepository.clear(bookId)
            imageCache.snapshot().keys.filter { it.startsWith("$bookId:") }.forEach(imageCache::remove)
            annotationsRepository.deleteForBook(bookId)
        }
        val removed = libraryRepository.deleteBooks(ids)
        _message.value = when (removed) {
            0 -> "Выбранные книги уже отсутствуют"
            1 -> "Книга удалена"
            else -> "Удалено книг: $removed"
        }
    }

    fun createLibraryFolder(parentPath: String, name: String) {
        runCatching { libraryRepository.createFolder(parentPath, name) }
            .onSuccess { path -> _message.value = "Папка создана: $path" }
            .onFailure { error -> _message.value = error.userMessage("Не удалось создать папку") }
    }

    fun renameLibraryFolder(folderPath: String, newName: String) {
        runCatching { libraryRepository.renameFolder(folderPath, newName) }
            .onSuccess { path ->
                audiobookController.rebaseFolder(folderPath, path)
                rebaseLibraryFolder(folderPath, path)
                _message.value = "Папка переименована: ${io.fluxreader.app.data.LibraryFolderTree.name(path)}"
            }
            .onFailure { error -> _message.value = error.userMessage("Не удалось переименовать папку") }
    }

    fun copyLibraryFolder(folderPath: String, targetParentPath: String) {
        runCatching { libraryRepository.copyFolder(folderPath, targetParentPath) }
            .onSuccess { path -> _message.value = "Папка скопирована: ${io.fluxreader.app.data.LibraryFolderTree.name(path)}" }
            .onFailure { error -> _message.value = error.userMessage("Не удалось скопировать папку") }
    }

    fun moveLibraryFolder(folderPath: String, targetParentPath: String) {
        runCatching { libraryRepository.moveFolder(folderPath, targetParentPath) }
            .onSuccess { path ->
                audiobookController.rebaseFolder(folderPath, path)
                rebaseLibraryFolder(folderPath, path)
                _message.value = "Папка перемещена: $path"
            }
            .onFailure { error -> _message.value = error.userMessage("Не удалось переместить папку") }
    }

    fun deleteLibraryFolder(folderPath: String) {
        runCatching { libraryRepository.deleteFolder(folderPath) }
            .onSuccess { affected ->
                audiobookController.rebaseFolder(folderPath, LibraryFolderTree.parent(folderPath))
                rebaseLibraryFolder(folderPath, LibraryFolderTree.parent(folderPath))
                _message.value = if (affected > 0) "Папка удалена · книг перемещено выше: $affected" else "Папка удалена"
            }
            .onFailure { error -> _message.value = error.userMessage("Не удалось удалить папку") }
    }

    /** Create a folder and move the selected books as one user action. */
    fun createFolderWithBooks(parentPath: String, name: String, bookIds: List<String>, sourceFolder: String?): String? {
        val ids = bookIds.distinct()
        if (ids.isEmpty()) return null
        val snapshot = libraryRepository.folderMembershipSnapshot(ids)
        var createdPath: String? = null
        return runCatching {
            require(ids.size == snapshot.size) { "Часть выбранных книг больше не существует" }
            val target = libraryRepository.createFolder(parentPath, name)
            createdPath = target
            val changed = libraryRepository.moveBooksToFolder(ids, sourceFolder, target)
            require(changed == ids.size) { "Не все выбранные книги удалось переместить" }
            setLibraryFolder(target)
            setLibraryFolderUndo(snapshot)
            _message.value = "Папка создана · перемещено книг: $changed"
            target
        }.getOrElse { error ->
            libraryRepository.restoreFolderMembership(snapshot)
            createdPath?.let { created -> runCatching { libraryRepository.deleteFolder(created) } }
            _message.value = error.userMessage("Не удалось создать папку с книгами")
            null
        }
    }

    fun copyBookToFolder(bookId: String, targetFolderPath: String) {
        val snapshot = libraryRepository.folderMembershipSnapshot(listOf(bookId))
        runCatching { libraryRepository.copyBookToFolder(bookId, targetFolderPath) }
            .onSuccess {
                setLibraryFolderUndo(snapshot)
                _message.value = "Книга добавлена в папку"
            }
            .onFailure { error -> _message.value = error.userMessage("Не удалось скопировать книгу") }
    }

    fun copyBooksToFolder(bookIds: List<String>, targetFolderPath: String) {
        val ids = bookIds.distinct()
        if (ids.isEmpty()) return
        val snapshot = libraryRepository.folderMembershipSnapshot(ids)
        runCatching { libraryRepository.copyBooksToFolder(ids, targetFolderPath) }
            .onSuccess { changed ->
                if (changed > 0) setLibraryFolderUndo(snapshot)
                _message.value = if (changed == 1) "Книга добавлена в папку" else "Добавлено в папку книг: $changed"
            }
            .onFailure { error -> _message.value = error.userMessage("Не удалось скопировать книги") }
    }

    fun moveBookToFolder(bookId: String, sourceFolderPath: String?, targetFolderPath: String) {
        val snapshot = libraryRepository.folderMembershipSnapshot(listOf(bookId))
        runCatching { libraryRepository.moveBookToFolder(bookId, sourceFolderPath, targetFolderPath) }
            .onSuccess {
                setLibraryFolderUndo(snapshot)
                _message.value = "Книга перемещена"
            }
            .onFailure { error -> _message.value = error.userMessage("Не удалось переместить книгу") }
    }

    fun moveBooksToFolder(bookIds: List<String>, sourceFolderPath: String?, targetFolderPath: String) {
        val ids = bookIds.distinct()
        if (ids.isEmpty()) return
        val snapshot = libraryRepository.folderMembershipSnapshot(ids)
        runCatching { libraryRepository.moveBooksToFolder(ids, sourceFolderPath, targetFolderPath) }
            .onSuccess { changed ->
                if (changed > 0) setLibraryFolderUndo(snapshot)
                _message.value = if (changed == 1) "Книга перемещена" else "Перемещено книг: $changed"
            }
            .onFailure { error -> _message.value = error.userMessage("Не удалось переместить книги") }
    }

    fun undoLibraryOperation() {
        val snapshot = libraryFolderUndoSnapshot ?: return
        val restored = libraryRepository.restoreFolderMembership(snapshot)
        libraryFolderUndoSnapshot = null
        _canUndoLibraryOperation.value = false
        _message.value = if (restored > 0) "Последнее перемещение книг отменено" else "Нечего отменять"
    }

    private fun setLibraryFolderUndo(snapshot: Map<String, List<String>>) {
        libraryFolderUndoSnapshot = snapshot.takeIf { it.isNotEmpty() }
        _canUndoLibraryOperation.value = libraryFolderUndoSnapshot != null
    }

    fun duplicateBook(bookId: String, targetFolderPath: String? = null) {
        launchHandled(
            onError = { _message.value = it.userMessage("Не удалось создать дубликат") },
        ) {
            val duplicate = libraryRepository.duplicateBook(bookId, targetFolderPath)
            _message.value = "Создан дубликат: ${duplicate.title}"
        }
    }

    fun exportBook(bookId: String, destination: Uri) {
        launchHandled(
            onError = { _message.value = it.userMessage("Не удалось экспортировать книгу") },
        ) {
            libraryRepository.exportBook(bookId, destination)
            _message.value = "Книга экспортирована"
        }
    }

    private fun retainImageCacheForBook(bookId: String) {
        val prefix = "$bookId:"
        imageCache.snapshot().keys.asSequence()
            .filterNot { it.startsWith(prefix) }
            .forEach(imageCache::remove)
    }

    private fun trimReaderCachesForLibrary() {
        // Reader -> Library should release heavyweight illustration references promptly. A small
        // warm tail is enough for an immediate reopen; parsed disk cache still makes cold misses cheap.
        imageCache.trimToSize((imageCache.maxSize() / 8).coerceAtLeast(512))
        contentRepository.trimSessionCaches()
    }

    private fun releaseRouteStatesForLibrary() {
        _reader.value = ReaderUiState()
        _pdf.value = PdfUiState()
        _speed.value = SpeedUiState()
        _search.value = SearchUiState()
        trimReaderCachesForLibrary()
    }

    fun openBook(bookId: String) {
        val book = libraryRepository.find(bookId) ?: return
        if (book.format == BookFormat.PDF) openPdf(bookId) else openReader(bookId)
    }

    fun openReader(bookId: String) {
        val book = libraryRepository.find(bookId) ?: return
        LibraryCoverCache.trimForReader()
        retainImageCacheForBook(bookId)
        if (book.format == BookFormat.PDF) {
            openPdf(bookId)
            return
        }

        cancelPdfRender()
        speedJob?.cancel()
        cancelBookDialogueAnalysis()
        speedPause()
        readerJob?.cancel()
        readerPrefetchJob?.cancel()
        _route.value = AppRoute.READER
        _reader.value = ReaderUiState(bookId = bookId, loading = true)

        readerJob = launchHandled(
            onError = { error ->
                if (_reader.value.bookId == bookId) {
                    _reader.value = ReaderUiState(bookId = bookId, error = error.userMessage("Не удалось открыть книгу"))
                }
            },
        ) {
            val outline = within(60_000, "Слишком долго читается структура книги") { contentRepository.outline(book) }
            require(outline.chapters.isNotEmpty()) { "В книге не найдено читаемых глав" }
            val index = ReadingPositionPolicy.chapter(book).coerceIn(0, outline.chapters.lastIndex)
            val chapter = within(60_000, "Слишком долго открывается глава") { contentRepository.chapter(book, outline.chapters[index]) }
            val fromSpeed = ReadingPositionPolicy.source(book) == ReadingPositionSource.SPEED
            val sharedAnchor = if (fromSpeed) ChapterSpeedOffset.toReaderAnchor(chapter, book.speedOffset) else null

            libraryRepository.updateMetadata(bookId, BookMetadata(outline.title, outline.author), outline.chapters.size)
            libraryRepository.touch(bookId)

            if (_route.value == AppRoute.READER && _reader.value.bookId == bookId) {
                _reader.value = ReaderUiState(
                    bookId = bookId,
                    outline = outline,
                    chapterIndex = index,
                    chapter = chapter,
                    initialBlockIndex = (sharedAnchor?.blockIndex ?: book.readerBlockIndex).coerceIn(0, chapter.blocks.lastIndex.coerceAtLeast(0)),
                    initialBlockOffsetPx = if (fromSpeed) 0 else book.readerBlockOffsetPx.coerceAtLeast(0),
                    initialBlockCharOffset = (sharedAnchor?.sourceCharOffset ?: book.readerBlockCharOffset).coerceAtLeast(0),
                )
            }
            scheduleReaderPrefetch(book, outline, index)
        }
    }

    fun readerChapter(index: Int) = readerChapterAt(index, blockIndex = 0, blockOffsetPx = 0)

    fun readerChapterAt(index: Int, blockIndex: Int, blockOffsetPx: Int, blockCharOffset: Int = 0) {
        val current = _reader.value
        val outline = current.outline ?: return
        val book = current.bookId?.let(libraryRepository::find) ?: return
        if (outline.chapters.isEmpty()) return
        val target = index.coerceIn(0, outline.chapters.lastIndex)
        val requestedBlock = blockIndex.coerceAtLeast(0)
        val requestedOffset = blockOffsetPx.coerceAtLeast(0)
        val requestedCharOffset = blockCharOffset.coerceAtLeast(0)

        if (target == current.chapterIndex && current.chapter != null) {
            val safeBlock = requestedBlock.coerceIn(0, current.chapter.blocks.lastIndex.coerceAtLeast(0))
            _reader.value = current.copy(
                initialBlockIndex = safeBlock,
                initialBlockOffsetPx = requestedOffset,
                initialBlockCharOffset = requestedCharOffset,
            )
            return
        }

        readerJob?.cancel()
        readerPrefetchJob?.cancel()
        _reader.value = current.copy(
            loading = true,
            error = null,
            initialBlockIndex = requestedBlock,
            initialBlockOffsetPx = requestedOffset,
            initialBlockCharOffset = requestedCharOffset,
        )
        readerJob = launchHandled(
            onError = { error ->
                val latest = _reader.value
                if (latest.bookId == book.id) {
                    _reader.value = latest.copy(loading = false, error = error.userMessage("Не удалось открыть главу"))
                }
            },
        ) {
            val chapter = within(60_000, "Слишком долго открывается глава") { contentRepository.chapter(book, outline.chapters[target]) }
            val safeBlock = requestedBlock.coerceIn(0, chapter.blocks.lastIndex.coerceAtLeast(0))
            val targetBlockCharCount = (chapter.blocks.getOrNull(safeBlock) as? io.fluxreader.app.data.ReaderBlock.Text)?.text?.length ?: 0
            val progress = ReaderProgressMath.bookFraction(
                chapterIndex = target,
                chapterCount = outline.chapters.size,
                chapterFraction = ReaderProgressMath.chapterFraction(
                    blockIndex = safeBlock,
                    blockCount = chapter.blocks.size.coerceAtLeast(1),
                    blockCharOffset = requestedCharOffset,
                    blockCharCount = targetBlockCharCount,
                ),
            )
            val latest = _reader.value
            if (_route.value == AppRoute.READER && latest.bookId == book.id) {
                libraryRepository.updateReaderPosition(
                    book.id, target, safeBlock, requestedOffset, progress, requestedCharOffset,
                )
                _reader.value = latest.copy(
                    loading = false,
                    chapterIndex = target,
                    chapter = chapter,
                    initialBlockIndex = safeBlock,
                    initialBlockOffsetPx = requestedOffset,
                    initialBlockCharOffset = requestedCharOffset,
                    error = null,
                )
            }
            scheduleReaderPrefetch(book, outline, target)
        }
    }

    private fun scheduleReaderPrefetch(book: BookRecord, outline: BookOutline, centerIndex: Int) {
        readerPrefetchJob?.cancel()
        readerPrefetchJob = viewModelScope.launch {
            // Let the current chapter publish its anchor preview before background parsing competes
            // for CPU/IO. prefetchAdjacentChapters is next-first, then previous, then wider radius.
            delay(180L)
            contentRepository.prefetchAdjacentChapters(
                book = book,
                outline = outline,
                centerIndex = centerIndex,
                radius = readerPrefetchRadius,
            )
        }
    }

    /** Returns a neighbouring chapter without mutating ReaderUiState; used only for layout prewarm. */
    suspend fun readerChapterForPaginationPrewarm(index: Int): ChapterContent? {
        val current = _reader.value
        val outline = current.outline ?: return null
        val book = current.bookId?.let(libraryRepository::find) ?: return null
        if (_route.value != AppRoute.READER || index !in outline.chapters.indices) return null
        return try {
            contentRepository.chapter(book, outline.chapters[index])
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Throwable) {
            null
        }
    }

    fun openReaderLink(target: String) {
        val current = _reader.value
        val outline = current.outline ?: return
        val book = current.bookId?.let(libraryRepository::find) ?: return
        val targetPath = target.substringBefore('#').trim()
        val fragment = target.substringAfter('#', "").trim()
        if (targetPath.isBlank() && fragment.isBlank()) return

        fun exactBlock(chapter: ChapterContent): Int? {
            if (fragment.isBlank()) return 0
            return chapter.blocks.indexOfFirst { fragment in it.anchors }.takeIf { it >= 0 }
        }

        val currentRef = outline.chapters.getOrNull(current.chapterIndex)
        val mayTargetCurrent = targetPath.isBlank() || currentRef?.sourceRef == targetPath
        if (mayTargetCurrent && current.chapter != null) {
            val block = exactBlock(current.chapter)
            if (block != null) {
                _reader.value = current.copy(initialBlockIndex = block, initialBlockOffsetPx = 0, initialBlockCharOffset = 0, error = null)
                val currentBlockCharCount = (current.chapter.blocks.getOrNull(block) as? io.fluxreader.app.data.ReaderBlock.Text)?.text?.length ?: 0
                val progress = ReaderProgressMath.bookFraction(
                    chapterIndex = current.chapterIndex,
                    chapterCount = outline.chapters.size,
                    chapterFraction = ReaderProgressMath.chapterFraction(
                        blockIndex = block,
                        blockCount = current.chapter.blocks.size.coerceAtLeast(1),
                        blockCharOffset = 0,
                        blockCharCount = currentBlockCharCount,
                    ),
                )
                libraryRepository.updateReaderPosition(book.id, current.chapterIndex, block, 0, progress)
                return
            }
        }

        val indexedTarget = when {
            targetPath.isNotBlank() -> outline.chapters.indexOfFirst { it.sourceRef == targetPath }
            fragment.isNotBlank() -> outline.chapters.indexOfFirst { fragment in it.anchors }
            else -> -1
        }

        if (indexedTarget < 0 && targetPath.isNotBlank()) {
            _message.value = "Ссылка ведёт на раздел вне оглавления книги"
            return
        }

        readerJob?.cancel()
        readerPrefetchJob?.cancel()
        _reader.value = current.copy(loading = true, error = null)
        readerJob = launchHandled(
            onError = { error ->
                val latest = _reader.value
                if (latest.bookId == book.id) {
                    _reader.value = latest.copy(loading = false, error = error.userMessage("Не удалось открыть ссылку"))
                }
            },
        ) {
            var resolvedIndex = indexedTarget
            var resolvedChapter: ChapterContent? = null
            var resolvedBlock: Int? = null

            if (resolvedIndex >= 0) {
                val loaded = within(60_000, "Слишком долго открывается ссылка") {
                    contentRepository.chapter(book, outline.chapters[resolvedIndex])
                }
                resolvedChapter = loaded
                resolvedBlock = exactBlock(loaded)
                if (resolvedBlock == null && fragment.isBlank()) resolvedBlock = 0
            }

            // FB2 footnotes commonly use fragment-only links (#note42). Fresh FB2 caches
            // index anchors in ChapterRef, but this fallback also handles older/malformed books.
            if (resolvedBlock == null && targetPath.isBlank() && fragment.isNotBlank()) {
                val order = buildList {
                    for (index in current.chapterIndex + 1 until outline.chapters.size) add(index)
                    for (index in 0 until current.chapterIndex) add(index)
                }
                for (index in order) {
                    if (index == resolvedIndex) continue
                    val loaded = within(60_000, "Слишком долго ищется примечание") {
                        contentRepository.chapter(book, outline.chapters[index])
                    }
                    val block = exactBlock(loaded)
                    if (block != null) {
                        resolvedIndex = index
                        resolvedChapter = loaded
                        resolvedBlock = block
                        break
                    }
                }
            }

            val chapter = resolvedChapter
            val block = resolvedBlock
            if (resolvedIndex < 0 || chapter == null || block == null) {
                val latest = _reader.value
                if (latest.bookId == book.id) _reader.value = latest.copy(loading = false)
                _message.value = if (fragment.isNotBlank()) "Якорь ссылки не найден в книге" else "Раздел ссылки не найден"
                return@launchHandled
            }

            val resolvedBlockCharCount = (chapter.blocks.getOrNull(block) as? io.fluxreader.app.data.ReaderBlock.Text)?.text?.length ?: 0
            val progress = ReaderProgressMath.bookFraction(
                chapterIndex = resolvedIndex,
                chapterCount = outline.chapters.size,
                chapterFraction = ReaderProgressMath.chapterFraction(
                    blockIndex = block,
                    blockCount = chapter.blocks.size.coerceAtLeast(1),
                    blockCharOffset = 0,
                    blockCharCount = resolvedBlockCharCount,
                ),
            )
            libraryRepository.updateReaderPosition(book.id, resolvedIndex, block, 0, progress)
            val latest = _reader.value
            if (_route.value == AppRoute.READER && latest.bookId == book.id) {
                _reader.value = latest.copy(
                    loading = false,
                    chapterIndex = resolvedIndex,
                    chapter = chapter,
                    initialBlockIndex = block,
                    initialBlockOffsetPx = 0,
                    initialBlockCharOffset = 0,
                    error = null,
                )
            }
            scheduleReaderPrefetch(book, outline, resolvedIndex)
        }
    }

    fun readerPrevious() = readerChapter(_reader.value.chapterIndex - 1)
    fun readerNext() = readerChapter(_reader.value.chapterIndex + 1)

    fun saveReaderPosition(expectedBookId: String?, expectedChapter: Int, blockIndex: Int, blockOffsetPx: Int, blockCharOffset: Int = 0) {
        val state = _reader.value
        if (!ReadingPositionPolicy.acceptsCheckpoint(
                _route.value == AppRoute.READER, state.loading, state.bookId, state.chapterIndex,
                expectedBookId, expectedChapter,
            )) return
        val outline = state.outline ?: return
        val bookId = state.bookId ?: return
        val blocks = state.chapter?.blocks.orEmpty()
        val blockCount = blocks.size.coerceAtLeast(1)
        val safeIndex = blockIndex.coerceIn(0, blockCount - 1)
        val blockCharCount = (blocks.getOrNull(safeIndex) as? io.fluxreader.app.data.ReaderBlock.Text)?.text?.length ?: 0
        val local = ReaderProgressMath.chapterFraction(
            blockIndex = safeIndex,
            blockCount = blockCount,
            blockCharOffset = blockCharOffset,
            blockCharCount = blockCharCount,
        )
        val progress = ReaderProgressMath.bookFraction(
            chapterIndex = state.chapterIndex,
            chapterCount = outline.chapters.size,
            chapterFraction = local,
        )
        libraryRepository.updateReaderPosition(
            bookId = bookId,
            chapter = state.chapterIndex,
            blockIndex = safeIndex,
            blockOffsetPx = blockOffsetPx.coerceAtLeast(0),
            progress = progress,
            blockCharOffset = blockCharOffset.coerceAtLeast(0),
        )
    }

    fun flushReadingPosition() = libraryRepository.flushPendingPositions()

    fun readerRetry() {
        _reader.value.bookId?.let(::openReader)
    }

    suspend fun loadImage(bookId: String, ref: String, targetWidthPx: Int): Bitmap? {
        val book = libraryRepository.find(bookId) ?: return null
        val width = targetWidthPx.coerceIn(240, 2400)
        val bucket = ((width + 255) / 256) * 256
        val sourceFile = File(book.filePath)
        val revision = "${book.contentHash}:${sourceFile.length()}:${sourceFile.lastModified()}"
        val key = "$bookId:$revision:$ref:$bucket"
        imageCache.get(key)?.let { cached -> if (!cached.isRecycled) return cached }

        val lock = imageDecodeLocks[(key.hashCode() and Int.MAX_VALUE) % imageDecodeLocks.size]
        return lock.withLock {
            imageCache.get(key)?.let { cached -> if (!cached.isRecycled) return@withLock cached }
            imageDecodeSemaphore.withPermit {
                // Reading from EPUB/FB2 and bitmap decoding are both memory-heavy. Keep them under
                // one bounded permit so illustrated books cannot launch an unbounded decode burst.
                val bytes = contentRepository.image(book, ref) ?: return@withPermit null
                val decoded = withContext(Dispatchers.Default) { decodeSampledBitmap(bytes, bucket) }
                    ?: return@withPermit null
                imageCache.put(key, decoded)
                decoded
            }
        }
    }

    private fun decodeSampledBitmap(bytes: ByteArray, targetWidthPx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= targetWidthPx) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample.coerceAtLeast(1)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    fun openPdf(bookId: String) {
        val book = libraryRepository.find(bookId) ?: return
        LibraryCoverCache.trimForReader()
        readerJob?.cancel()
        readerPrefetchJob?.cancel()
        speedJob?.cancel()
        cancelBookDialogueAnalysis()
        speedPause()
        cancelPdfRender()
        _route.value = AppRoute.PDF
        _pdf.value = PdfUiState(bookId = bookId, loading = true, pageIndex = ReadingPositionPolicy.chapter(book))

        pdfJob = launchHandled(
            onError = { error ->
                if (_pdf.value.bookId == bookId) {
                    _pdf.value = PdfUiState(bookId = bookId, error = error.userMessage("Не удалось открыть PDF"))
                }
            },
        ) {
            val count = within(30_000, "Слишком долго открывается PDF") { contentRepository.pdfPageCount(book) }
            require(count > 0) { "PDF не содержит страниц" }
            val page = ReadingPositionPolicy.chapter(book).coerceIn(0, count - 1)
            libraryRepository.updateMetadata(book.id, BookMetadata(book.title, book.author), count)
            libraryRepository.touch(book.id)
            if (_route.value == AppRoute.PDF && _pdf.value.bookId == bookId) {
                _pdf.value = PdfUiState(bookId = bookId, pageIndex = page, pageCount = count)
            }
        }
    }

    fun renderPdf(widthPx: Int) {
        val state = _pdf.value
        val book = state.bookId?.let(libraryRepository::find) ?: return
        if (state.pageCount <= 0 || widthPx < 100) return
        if (state.bitmap != null && state.renderWidthPx == widthPx && !state.loading) return
        if (state.loading && pdfPendingPage == state.pageIndex && pdfPendingWidthPx == widthPx) return

        cancelPdfRender()
        val requestGeneration = pdfRenderGeneration
        val requestedPage = state.pageIndex
        pdfPendingPage = requestedPage
        pdfPendingWidthPx = widthPx
        _pdf.value = state.copy(loading = true, error = null)
        pdfJob = launchHandled(
            onError = { error ->
                val latest = _pdf.value
                if (
                    requestGeneration == pdfRenderGeneration &&
                    latest.bookId == book.id &&
                    latest.pageIndex == requestedPage
                ) {
                    pdfPendingPage = -1
                    pdfPendingWidthPx = 0
                    _pdf.value = latest.copy(loading = false, error = error.userMessage("Не удалось отрисовать страницу"))
                }
            },
        ) {
            val rendered = within(30_000, "Слишком долго отрисовывается страница PDF") { contentRepository.renderPdf(book, requestedPage, widthPx) }
            val latest = _pdf.value
            if (
                requestGeneration == pdfRenderGeneration &&
                _route.value == AppRoute.PDF &&
                latest.bookId == book.id &&
                latest.pageIndex == requestedPage
            ) {
                pdfPendingPage = -1
                pdfPendingWidthPx = 0
                _pdf.value = latest.copy(
                    loading = false,
                    pageIndex = rendered.pageIndex,
                    pageCount = rendered.pageCount,
                    bitmap = rendered.bitmap,
                    renderWidthPx = widthPx,
                    error = null,
                )
            } else if (!rendered.bitmap.isRecycled) {
                rendered.bitmap.recycle()
            }
        }
    }

    fun pdfPage(index: Int) {
        val state = _pdf.value
        val bookId = state.bookId ?: return
        if (state.pageCount <= 0) return
        val target = index.coerceIn(0, state.pageCount - 1)
        if (target == state.pageIndex && state.bitmap != null) return

        cancelPdfRender()
        val progress = if (state.pageCount <= 1) 0f else target.toFloat() / (state.pageCount - 1).toFloat()
        libraryRepository.updatePdfPosition(bookId, target, progress)
        _pdf.value = state.copy(
            loading = false,
            pageIndex = target,
            bitmap = null,
            renderWidthPx = 0,
            error = null,
        )
    }

    fun pdfPrevious() = pdfPage(_pdf.value.pageIndex - 1)
    fun pdfNext() = pdfPage(_pdf.value.pageIndex + 1)

    fun pdfRetry() {
        _pdf.value.bookId?.let(::openPdf)
    }

    private fun restoreReaderFromSpeedPosition() {
        cancelBookDialogueAnalysis()
        val speedState = _speed.value
        val bookId = speedState.bookId ?: run {
            _route.value = AppRoute.READER
            return
        }
        val speedOffset = speedState.chunk?.startOffset ?: 0
        val targetChapter = speedState.chapterIndex.coerceAtLeast(0)
        val book = libraryRepository.find(bookId) ?: run {
            _route.value = AppRoute.READER
            return
        }
        val outline = _reader.value.outline ?: speedState.outline ?: run {
            _route.value = AppRoute.READER
            openReader(bookId)
            return
        }
        val safeChapter = targetChapter.coerceIn(0, outline.chapters.lastIndex.coerceAtLeast(0))

        fun publish(chapter: ChapterContent) {
            val anchor = ChapterSpeedOffset.toReaderAnchor(chapter, speedOffset)
            val blockCount = chapter.blocks.size.coerceAtLeast(1)
            val safeBlock = anchor.blockIndex.coerceIn(0, blockCount - 1)
            val blockText = (chapter.blocks.getOrNull(safeBlock) as? io.fluxreader.app.data.ReaderBlock.Text)?.text.orEmpty()
            val safeChar = anchor.sourceCharOffset.coerceIn(0, blockText.length)
            val chapterFraction = ReaderProgressMath.chapterFraction(
                blockIndex = safeBlock,
                blockCount = blockCount,
                blockCharOffset = safeChar,
                blockCharCount = blockText.length,
            )
            val progress = ReaderProgressMath.bookFraction(
                chapterIndex = safeChapter,
                chapterCount = outline.chapters.size,
                chapterFraction = chapterFraction,
            )
            libraryRepository.updateReaderPosition(bookId, safeChapter, safeBlock, 0, progress, safeChar)
            _reader.value = ReaderUiState(
                bookId = bookId,
                outline = outline,
                chapterIndex = safeChapter,
                chapter = chapter,
                initialBlockIndex = safeBlock,
                initialBlockOffsetPx = 0,
                initialBlockCharOffset = safeChar,
            )
            scheduleReaderPrefetch(book, outline, safeChapter)
        }

        _route.value = AppRoute.READER
        val currentReader = _reader.value
        if (currentReader.bookId == bookId && currentReader.chapterIndex == safeChapter && currentReader.chapter != null) {
            publish(currentReader.chapter)
            return
        }

        readerJob?.cancel()
        readerPrefetchJob?.cancel()
        _reader.value = ReaderUiState(
            bookId = bookId,
            loading = true,
            outline = outline,
            chapterIndex = safeChapter,
        )
        readerJob = launchHandled(
            onError = { error ->
                if (_route.value == AppRoute.READER && _reader.value.bookId == bookId) {
                    _reader.value = _reader.value.copy(loading = false, error = error.userMessage("Не удалось вернуться к месту скорочтения"))
                }
            },
        ) {
            val chapter = within(60_000, "Слишком долго возвращаюсь к месту чтения") {
                contentRepository.chapter(book, outline.chapters[safeChapter])
            }
            if (_route.value == AppRoute.READER && _reader.value.bookId == bookId) publish(chapter)
        }
    }

    fun openSpeed(bookId: String) {
        LibraryCoverCache.trimForReader()
        val preferredChapter = when (_route.value) {
            AppRoute.READER -> {
                if (_reader.value.loading || _reader.value.chapter == null) {
                    _message.value = "Дождитесь открытия главы"
                    return
                }
                _reader.value.chapterIndex
            }
            AppRoute.PDF -> {
                if (_pdf.value.loading || _pdf.value.pageCount <= 0) {
                    _message.value = "Дождитесь открытия PDF"
                    return
                }
                _pdf.value.pageIndex
            }
            else -> null
        }
        if (_route.value != AppRoute.SPEED) routeBeforeSpeed = _route.value
        val book = libraryRepository.find(bookId) ?: return
        // A live Reader/PDF entry always wins over the old mode-specific cursor.
        val targetChapter = preferredChapter ?: ReadingPositionPolicy.chapter(book)
        val source = if (preferredChapter != null) {
            if (_route.value == AppRoute.PDF) ReadingPositionSource.PDF else ReadingPositionSource.READER
        } else ReadingPositionPolicy.source(book)
        startSpeed(bookId, targetChapter, if (source == ReadingPositionSource.SPEED) book.speedOffset else null,
            resumeFromReader = source == ReadingPositionSource.READER)
    }

    fun openSpeedAt(bookId: String, chapterIndex: Int, chapterOffset: Int) {
        if (_route.value != AppRoute.SPEED) routeBeforeSpeed = _route.value
        startSpeed(
            bookId = bookId,
            preferredChapter = chapterIndex.coerceAtLeast(0),
            preferredOffset = chapterOffset.coerceAtLeast(0),
        )
    }

    private fun startSpeed(
        bookId: String,
        preferredChapter: Int? = null,
        preferredOffset: Int? = null,
        resumeFromReader: Boolean = false,
    ) {
        val book = libraryRepository.find(bookId) ?: return
        retainImageCacheForBook(bookId)
        readerJob?.cancel()
        readerPrefetchJob?.cancel()
        cancelPdfRender()
        speedJob?.cancel()
        cancelBookDialogueAnalysis()
        // A fresh Speed Reader session starts at its stored word.  Do not reuse
        // the previous session's elapsed pause time: auto-rewind would otherwise
        // jump to the start of an old/possibly very long sentence on first Play.
        speedPausedAtRealtimeMs = 0L
        _route.value = AppRoute.SPEED
        _speed.value = SpeedUiState(bookId = bookId, loading = true, status = "Готовлю текст…")
        speedJob = launchHandled(
            onError = { error ->
                if (_speed.value.bookId == bookId) {
                    _speed.value = SpeedUiState(bookId = bookId, status = null, error = error.userMessage("Не удалось подготовить Speed Reader"))
                }
            },
        ) {
            val outline = within(60_000, "Слишком долго читается структура книги") { contentRepository.speedOutline(book) }
            require(outline.chapters.isNotEmpty()) { "В книге не найдено текста" }
            val words = wordsForMode(settings.value.speedMode, settings.value.speedWordsPerFixation)
            val startChapter = (preferredChapter ?: book.speedChapter).coerceIn(0, outline.chapters.lastIndex)
            val savedOrChapterStart = if (preferredChapter == null || preferredChapter == book.speedChapter) book.speedOffset else 0
            val fallbackOffset = if (book.format == BookFormat.PDF && preferredChapter != null) 0 else savedOrChapterStart
            val startOffset = if (resumeFromReader && book.format != BookFormat.PDF) {
                val chapter = contentRepository.chapter(book, outline.chapters[startChapter])
                ChapterSpeedOffset.forBlock(chapter, book.readerBlockIndex, book.readerBlockCharOffset)
            } else (preferredOffset ?: fallbackOffset)
            val loaded = within(
                60_000L,
                if (book.format == BookFormat.PDF) "Не удалось подготовить текст PDF за 60 секунд" else "Слишком долго готовится Speed Reader",
            ) {
                val primary = findReadableSpeedChapter(book, outline, startChapter, startOffset, words)
                val fallback = if (primary == null && startChapter != 0) {
                    findReadableSpeedChapter(book, outline, 0, 0, words)
                } else {
                    null
                }
                primary ?: fallback ?: error(
                    if (book.format == BookFormat.PDF)
                        "Не удалось получить текст PDF рядом с текущей страницей. Проверьте OCR-языки или повторите попытку."
                    else
                        "В книге не найдено текста для скорочтения"
                )
            }

            libraryRepository.updateMetadata(book.id, BookMetadata(outline.title, outline.author), outline.chapters.size)
            libraryRepository.touch(book.id)

            if (_route.value == AppRoute.SPEED && _speed.value.bookId == bookId) {
                _speed.value = SpeedUiState(
                    bookId = bookId,
                    outline = outline,
                    chapterIndex = loaded.chapterIndex,
                    chapterText = loaded.text,
                    dialogueIndex = null,
                    dialogueAnalysisStatus = null,
                    chunk = loaded.chunk,
                    mediaCues = loaded.mediaCues,
                    headingCues = loaded.headingCues,
                    activeMedia = null,
                    lastPresentedMediaOffset = -1,
                    mediaResumePending = false,
                    status = null,
                )
                // Ready-state collectors may already have sought to a newer word. Persist the
                // displayed cursor instead of overwriting it with the initial load offset.
                val published = _speed.value
                published.chunk?.let { saveSpeedProgress(published, it.startOffset) }
                scheduleBookDialogueAnalysis()
            }
        }
    }

    fun speedToggle() {
        val state = _speed.value
        if (state.loading || state.chunk == null || state.error != null) return
        if (state.playing) {
            speedPause()
            return
        }

        val config = settings.value
        var resumed = state
        val pausedFor = if (speedPausedAtRealtimeMs > 0L) {
            SystemClock.elapsedRealtime() - speedPausedAtRealtimeMs
        } else {
            0L
        }
        if (config.speedAutoRewindAfterPause &&
            pausedFor >= config.speedAutoRewindThresholdSeconds * 1_000L
        ) {
            val current = state.chunk
            val sentenceStart = SpeedReaderCursor.sentenceStartOffset(state.chapterText, current.startOffset)
            val rewindOffset = if (sentenceStart < current.startOffset) {
                sentenceStart
            } else {
                SpeedReaderCursor.previousSentenceOffset(state.chapterText, current.startOffset) ?: sentenceStart
            }
            speedChunkAt(state.chapterText, rewindOffset, config)?.let { rebuilt ->
                resumed = state.copy(chunk = rebuilt)
                saveSpeedProgress(resumed, rebuilt.startOffset)
            }
        }
        val leadingMedia = nextSpeedMediaCue(resumed, 0, resumed.chunk?.startOffset ?: 0)
        if (leadingMedia != null) {
            speedPausedAtRealtimeMs = 0L
            _speed.value = resumed.copy(
                activeMedia = leadingMedia,
                lastPresentedMediaOffset = leadingMedia.offset,
                mediaResumePending = true,
                playing = false,
            )
            return
        }
        speedPausedAtRealtimeMs = 0L
        _speed.value = resumed.copy(playing = true)
    }

    fun speedPause() {
        val state = _speed.value
        if (state.playing) {
            speedPausedAtRealtimeMs = SystemClock.elapsedRealtime()
            _speed.value = state.copy(playing = false)
        }
        // Commit the exact displayed word on pause and navigation, rather than
        // relying solely on the debounced write from the last automatic advance.
        if (state.bookId != null && state.chunk != null && state.outline != null) {
            saveSpeedProgress(state, state.chunk.startOffset)
            libraryRepository.flushPendingPositions()
        }
    }

    fun speedAdvance() {
        val state = _speed.value
        val chunk = state.chunk ?: return
        val outline = state.outline ?: return
        val config = settings.value
        val leadingMedia = nextSpeedMediaCue(state, 0, chunk.startOffset)
        if (leadingMedia != null) {
            _speed.value = state.copy(
                activeMedia = leadingMedia,
                lastPresentedMediaOffset = leadingMedia.offset,
                mediaResumePending = state.playing,
                playing = false,
            )
            return
        }
        val next = speedChunkAt(state.chapterText, chunk.endOffset, config)

        if (next != null) {
            val media = nextSpeedMediaCue(state, chunk.endOffset, next.startOffset)
            if (media != null) {
                _speed.value = state.copy(
                    activeMedia = media,
                    lastPresentedMediaOffset = media.offset,
                    mediaResumePending = state.playing,
                    playing = false,
                )
                return
            }
            val history = (state.history + SpeedPosition(state.chapterIndex, chunk.startOffset)).takeLast(256)
            _speed.value = state.copy(chunk = next, history = history)
            saveSpeedProgress(_speed.value, next.startOffset)
            return
        }

        val trailingMedia = nextSpeedMediaCue(state, chunk.endOffset, state.chapterText.length)
        if (trailingMedia != null) {
            _speed.value = state.copy(
                activeMedia = trailingMedia,
                lastPresentedMediaOffset = trailingMedia.offset,
                mediaResumePending = state.playing,
                playing = false,
            )
            return
        }

        if (state.chapterIndex >= outline.chapters.lastIndex) {
            _speed.value = state.copy(playing = false)
            return
        }

        loadSpeedChapter(
            startChapter = state.chapterIndex + 1,
            offset = 0,
            appendHistory = SpeedPosition(state.chapterIndex, chunk.startOffset),
        )
    }

    fun speedBack() {
        val state = _speed.value
        val previous = state.history.lastOrNull() ?: return
        val remaining = state.history.dropLast(1)
        if (previous.chapterIndex == state.chapterIndex) {
            val chunk = speedChunkAt(state.chapterText, previous.offset) ?: return
            _speed.value = state.copy(chunk = chunk, history = remaining, playing = false)
            saveSpeedProgress(_speed.value, chunk.startOffset)
        } else {
            loadSpeedChapter(
                startChapter = previous.chapterIndex,
                offset = previous.offset,
                historyOverride = remaining,
                forcePause = true,
            )
        }
    }

    fun speedPreviousSentence() {
        val state = _speed.value
        val chunk = state.chunk ?: return
        speedPause()
        val offset = SpeedReaderCursor.previousSentenceOffset(state.chapterText, chunk.startOffset)
        if (offset != null) {
            jumpSpeedWithinChapter(offset)
        } else {
            speedBack()
        }
    }

    fun speedNextSentence() {
        val state = _speed.value
        val chunk = state.chunk ?: return
        val outline = state.outline ?: return
        speedPause()
        val offset = SpeedReaderCursor.nextSentenceOffset(state.chapterText, chunk.endOffset)
        if (offset != null) {
            jumpSpeedWithinChapter(offset)
        } else if (state.chapterIndex < outline.chapters.lastIndex) {
            loadSpeedChapter(
                startChapter = state.chapterIndex + 1,
                offset = 0,
                appendHistory = SpeedPosition(state.chapterIndex, chunk.startOffset),
                forcePause = true,
            )
        }
    }

    fun speedPreviousParagraph() {
        val state = _speed.value
        val chunk = state.chunk ?: return
        speedPause()
        SpeedReaderCursor.previousParagraphOffset(state.chapterText, chunk.startOffset)?.let(::jumpSpeedWithinChapter)
    }

    fun speedNextParagraph() {
        val state = _speed.value
        val chunk = state.chunk ?: return
        speedPause()
        SpeedReaderCursor.nextParagraphOffset(state.chapterText, chunk.endOffset)?.let(::jumpSpeedWithinChapter)
    }

    private fun jumpSpeedWithinChapter(offset: Int) {
        val state = _speed.value
        val current = state.chunk ?: return
        val rebuilt = speedChunkAt(state.chapterText, offset) ?: return
        val history = if (rebuilt.startOffset == current.startOffset) {
            state.history
        } else {
            (state.history + SpeedPosition(state.chapterIndex, current.startOffset)).takeLast(256)
        }
        _speed.value = state.copy(chunk = rebuilt, history = history, playing = false)
        speedPausedAtRealtimeMs = SystemClock.elapsedRealtime()
        saveSpeedProgress(_speed.value, rebuilt.startOffset)
    }

    fun commitSpeechPosition(bookId: String?, chapterIndex: Int, offset: Int) {
        val state = _speed.value
        if (!ReadingPositionPolicy.acceptsCheckpoint(
                _route.value == AppRoute.SPEED, state.loading, state.bookId, state.chapterIndex,
                bookId, chapterIndex,
            )) return
        val chunk = speedChunkAt(state.chapterText, offset.coerceAtLeast(0)) ?: return
        _speed.value = state.copy(chunk = chunk, playing = false)
        saveSpeedProgress(_speed.value, chunk.startOffset)
    }

    fun speedSeekToOffset(offset: Int) {
        speedPause()
        jumpSpeedWithinChapter(offset.coerceAtLeast(0))
    }

    fun speedDismissMedia(resume: Boolean = true) {
        val state = _speed.value
        if (state.activeMedia == null) return
        val shouldResume = resume && state.mediaResumePending
        speedPausedAtRealtimeMs = if (shouldResume) 0L else SystemClock.elapsedRealtime()
        _speed.value = state.copy(
            activeMedia = null,
            mediaResumePending = false,
            playing = shouldResume,
        )
    }

    private fun nextSpeedMediaCue(state: SpeedUiState, fromOffset: Int, toOffset: Int): SpeedMediaCue? {
        val from = fromOffset.coerceAtLeast(0)
        val to = toOffset.coerceAtLeast(from)
        return state.mediaCues.firstOrNull { cue ->
            cue.offset > state.lastPresentedMediaOffset && cue.offset in from..to
        }
    }

    fun speedRetry() {
        val bookId = _speed.value.bookId ?: return
        val preferred = _speed.value.chapterIndex.takeIf { it >= 0 }
        startSpeed(bookId, preferred)
    }

    private fun loadSpeedChapter(
        startChapter: Int,
        offset: Int,
        appendHistory: SpeedPosition? = null,
        historyOverride: List<SpeedPosition>? = null,
        forcePause: Boolean = false,
    ) {
        val initial = _speed.value
        val outline = initial.outline ?: return
        val book = initial.bookId?.let(libraryRepository::find) ?: return
        val words = wordsForMode(settings.value.speedMode, settings.value.speedWordsPerFixation)

        speedJob?.cancel()
        cancelBookDialogueAnalysis()
        _speed.value = initial.copy(loading = true, status = "Открываю следующий фрагмент…", error = null, playing = if (forcePause) false else initial.playing)
        speedJob = launchHandled(
            onError = { error ->
                val latest = _speed.value
                if (latest.bookId == book.id) {
                    _speed.value = latest.copy(
                        loading = false,
                        playing = false,
                        status = null,
                        error = error.userMessage("Не удалось открыть следующую главу"),
                    )
                }
            },
        ) {
            val loaded = findReadableSpeedChapter(book, outline, startChapter, offset, words)
                ?: error("В следующих главах нет текста")
            val latest = _speed.value
            if (_route.value == AppRoute.SPEED && latest.bookId == book.id) {
                val history = historyOverride ?: (latest.history + listOfNotNull(appendHistory)).takeLast(256)
                _speed.value = latest.copy(
                    loading = false,
                    chapterIndex = loaded.chapterIndex,
                    chapterText = loaded.text,
                    dialogueIndex = null,
                    dialogueAnalysisStatus = null,
                    chunk = loaded.chunk,
                    mediaCues = loaded.mediaCues,
                    headingCues = loaded.headingCues,
                    activeMedia = null,
                    lastPresentedMediaOffset = -1,
                    mediaResumePending = false,
                    history = history,
                    status = null,
                    error = null,
                    playing = if (forcePause) false else latest.playing,
                )
                // Ready-state collectors may already have sought to a newer word. Persist the
                // displayed cursor instead of overwriting it with the initial load offset.
                val published = _speed.value
                published.chunk?.let { saveSpeedProgress(published, it.startOffset) }
                scheduleBookDialogueAnalysis()
            }
        }
    }

    private suspend fun findReadableSpeedChapter(
        book: BookRecord,
        outline: BookOutline,
        startChapter: Int,
        offset: Int,
        words: Int,
    ): LoadedSpeedChapter? {
        if (outline.chapters.isEmpty()) return null
        var index = startChapter.coerceIn(0, outline.chapters.lastIndex)
        while (index <= outline.chapters.lastIndex) {
            val pdfScanLimit = if (settings.value.ocrEnabled) 12 else 48
            if (book.format == BookFormat.PDF && index - startChapter >= pdfScanLimit) break
            if (book.format == BookFormat.PDF && _route.value == AppRoute.SPEED && _speed.value.bookId == book.id) {
                val backend = if (settings.value.ocrEnabled) contentRepository.ocrBackendDescription(settings.value.ocrLanguages) else "text layer"
                _speed.value = _speed.value.copy(status = "Страница ${index + 1} · $backend")
            }
            val timeout = if (book.format == BookFormat.PDF && settings.value.ocrEnabled) 30_000L else if (book.format == BookFormat.PDF) 12_000L else 60_000L
            val document = within(timeout, if (book.format == BookFormat.PDF) "Слишком долго извлекается текст PDF" else "Слишком долго открывается глава") {
                contentRepository.speedDocument(
                    book = book,
                    ref = outline.chapters[index],
                    allowOcr = book.format == BookFormat.PDF && settings.value.ocrEnabled,
                    ocrLanguages = settings.value.ocrLanguages,
                    allowMlKitFallback = settings.value.ocrUseMlKitFallback,
                )
            }
            val text = document.text
            val requestedStart = if (index == startChapter) offset.coerceIn(0, text.length) else 0
            val semanticStart = document.readableOffset(requestedStart)
            val config = settings.value
            val start = SpeedDialoguePlayback.wordStartOffset(text, semanticStart, config)
            val chunk = SpeedDialoguePlayback.next(text, start, config, words)
            if (chunk != null) return LoadedSpeedChapter(index, text, chunk, document.mediaCues, document.headingCues)
            index++
        }
        return null
    }

    private fun saveSpeedProgress(state: SpeedUiState, offset: Int) {
        val active = _speed.value
        if (!ReadingPositionPolicy.acceptsCheckpoint(
                _route.value == AppRoute.SPEED, state.loading || active.loading, active.bookId, active.chapterIndex,
                state.bookId, state.chapterIndex,
            )) return
        val outline = state.outline ?: return
        val bookId = state.bookId ?: return
        val chapterProgress = if (state.chapterText.isBlank()) 0f else offset.toFloat() / state.chapterText.length.toFloat()
        val progress = ((state.chapterIndex.toFloat() + chapterProgress) / outline.chapters.size.coerceAtLeast(1).toFloat())
            .coerceIn(0f, 1f)
        libraryRepository.updateSpeedPosition(bookId, state.chapterIndex, offset, progress)
    }

    fun clearSearch() {
        searchJob?.cancel()
        _search.value = SearchUiState()
    }

    fun searchBook(query: String) {
        val clean = query.trim()
        if (clean.length < 2) {
            _search.value = SearchUiState(query = clean, error = "Введите минимум 2 символа")
            return
        }
        val book = when (_route.value) {
            AppRoute.READER -> _reader.value.bookId?.let(libraryRepository::find)
            AppRoute.PDF -> _pdf.value.bookId?.let(libraryRepository::find)
            else -> null
        } ?: return

        searchJob?.cancel()
        _search.value = SearchUiState(query = clean, loading = true)
        searchJob = launchHandled(
            onError = { error ->
                _search.value = _search.value.copy(
                    loading = false,
                    error = error.userMessage("Не удалось выполнить поиск"),
                )
            },
        ) {
            val outline = if (book.format == BookFormat.PDF) contentRepository.speedOutline(book) else contentRepository.outline(book)
            val hits = ArrayList<SearchHit>()
            val total = outline.chapters.size
            var scanned = 0
            for ((chapterIndex, ref) in outline.chapters.withIndex()) {
                currentCoroutineContext().ensureActive()
                if (hits.size >= MAX_SEARCH_HITS) break
                if (book.format == BookFormat.PDF) {
                    val text = try {
                        within(12_000, "Слишком долго читается текст PDF") {
                            contentRepository.speedText(book, ref, allowOcr = false)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Throwable) {
                        ""
                    }
                    hits += findOccurrences(clean, text, chapterIndex, 0, ref.title, MAX_SEARCH_HITS - hits.size)
                } else {
                    val chapter = try {
                        within(60_000, "Слишком долго открывается глава при поиске") {
                            contentRepository.chapter(book, ref)
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Throwable) {
                        null
                    }
                    chapter?.blocks?.forEachIndexed { blockIndex, block ->
                        if (hits.size >= MAX_SEARCH_HITS) return@forEachIndexed
                        val text = (block as? io.fluxreader.app.data.ReaderBlock.Text)?.text ?: return@forEachIndexed
                        hits += findOccurrences(clean, text, chapterIndex, blockIndex, ref.title, MAX_SEARCH_HITS - hits.size)
                    }
                }
                scanned = chapterIndex + 1
                _search.value = SearchUiState(
                    query = clean,
                    loading = true,
                    scanned = scanned,
                    total = total,
                    hits = hits.toList(),
                )
            }
            _search.value = SearchUiState(
                query = clean,
                loading = false,
                scanned = scanned,
                total = total,
                hits = hits.toList(),
            )
        }
    }

    fun openSearchHit(hit: SearchHit) {
        clearSearch()
        when (_route.value) {
            AppRoute.READER -> readerJump(hit.chapterIndex, hit.blockIndex)
            AppRoute.PDF -> pdfPage(hit.chapterIndex)
            else -> Unit
        }
    }

    private fun readerJump(chapterIndex: Int, blockIndex: Int) {
        val current = _reader.value
        val outline = current.outline ?: return
        val book = current.bookId?.let(libraryRepository::find) ?: return
        if (outline.chapters.isEmpty()) return
        val target = chapterIndex.coerceIn(0, outline.chapters.lastIndex)
        readerJob?.cancel()
        readerPrefetchJob?.cancel()
        _reader.value = current.copy(loading = true, error = null)
        readerJob = launchHandled(
            onError = { error ->
                val latest = _reader.value
                if (latest.bookId == book.id) _reader.value = latest.copy(loading = false, error = error.userMessage("Не удалось перейти к результату"))
            },
        ) {
            val chapter = within(60_000, "Слишком долго открывается найденная глава") {
                contentRepository.chapter(book, outline.chapters[target])
            }
            val safeBlock = blockIndex.coerceIn(0, chapter.blocks.lastIndex.coerceAtLeast(0))
            val progress = if (outline.chapters.isEmpty()) 0f else target.toFloat() / outline.chapters.size.toFloat()
            libraryRepository.updateReaderPosition(book.id, target, safeBlock, 0, progress)
            val latest = _reader.value
            if (_route.value == AppRoute.READER && latest.bookId == book.id) {
                _reader.value = latest.copy(
                    loading = false,
                    chapterIndex = target,
                    chapter = chapter,
                    initialBlockIndex = safeBlock,
                    initialBlockOffsetPx = 0,
                    initialBlockCharOffset = 0,
                    error = null,
                )
            }
            scheduleReaderPrefetch(book, outline, target)
        }
    }

    private fun findOccurrences(
        query: String,
        text: String,
        chapterIndex: Int,
        blockIndex: Int,
        title: String,
        limit: Int,
    ): List<SearchHit> {
        return SearchEngine.find(query, text, limit).map { occurrence ->
            SearchHit(
                chapterIndex = chapterIndex,
                blockIndex = blockIndex,
                title = title,
                snippet = occurrence.snippet,
            )
        }
    }

    fun installedOcrLanguages(): List<String> = contentRepository.installedOcrLanguages()

    fun ocrBackendDescription(): String = contentRepository.ocrBackendDescription(settings.value.ocrLanguages)

    fun updateSettings(transform: (ReaderSettings) -> ReaderSettings) {
        val before = settings.value
        settingsRepository.update(transform)
        val after = settings.value

        if (_route.value == AppRoute.SPEED &&
            (before.speedMode != after.speedMode ||
                before.speedWordsPerFixation != after.speedWordsPerFixation ||
                before.speedSmartChunking != after.speedSmartChunking ||
                before.speedAdaptiveChunkSize != after.speedAdaptiveChunkSize ||
                before.speedPacingMode != after.speedPacingMode ||
                before.speedDialogueEnabled != after.speedDialogueEnabled ||
                before.speedDialogueShortReplies != after.speedDialogueShortReplies)
        ) {
            val state = _speed.value
            val start = state.chunk?.startOffset ?: 0
            val rebuilt = speedChunkAt(state.chapterText, start, after)
            _speed.value = state.copy(
                chunk = rebuilt,
                playing = false,
                error = if (rebuilt == null && state.chapterText.isNotBlank()) "Нет текста для текущего режима" else null,
            )
        }
        if (_route.value == AppRoute.SPEED &&
            (before.speedDialogueEnabled != after.speedDialogueEnabled ||
                before.speedDialogueBookAnalysis != after.speedDialogueBookAnalysis)) {
            scheduleBookDialogueAnalysis()
        }
    }

    private fun cancelBookDialogueAnalysis() {
        bookDialogueGeneration++
        bookDialogueJob?.cancel()
        bookDialogueJob = null
    }

    private fun scheduleBookDialogueAnalysis() {
        cancelBookDialogueAnalysis()
        val state = _speed.value
        val config = settings.value
        if (_route.value != AppRoute.SPEED || !config.speedDialogueEnabled || !config.speedDialogueBookAnalysis) {
            _speed.value = state.copy(dialogueIndex = null, dialogueAnalysisStatus = null)
            return
        }
        val book = state.bookId?.let(libraryRepository::find) ?: return
        val outline = state.outline ?: return
        if (book.format == BookFormat.PDF) {
            _speed.value = state.copy(dialogueAnalysisStatus = "Анализ всей книги пока доступен для EPUB, FB2 и TXT")
            return
        }
        val generation = bookDialogueGeneration
        bookDialogueJob = viewModelScope.launch {
            try {
                val displayedFingerprint = withContext(Dispatchers.Default) {
                    ChapterDialogueIndex.fingerprint(state.chapterText)
                }
                bookDialogueRepository.analyze(book, outline, state.chapterIndex,
                    onChapter = { chapter, index ->
                        withContext(Dispatchers.Main.immediate) {
                            val latest = _speed.value
                            if (generation == bookDialogueGeneration && _route.value == AppRoute.SPEED &&
                                latest.bookId == book.id && !latest.loading && latest.chapterIndex == chapter &&
                                index.textFingerprint == displayedFingerprint) {
                                _speed.value = latest.copy(dialogueIndex = index)
                            }
                        }
                    },
                    onProgress = { progress ->
                        withContext(Dispatchers.Main.immediate) {
                            if (generation == bookDialogueGeneration && _route.value == AppRoute.SPEED && _speed.value.bookId == book.id) {
                                _speed.value = _speed.value.copy(dialogueAnalysisStatus = progress.label)
                            }
                        }
                    },
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                if (generation == bookDialogueGeneration && _route.value == AppRoute.SPEED && _speed.value.bookId == book.id) {
                    _speed.value = _speed.value.copy(dialogueAnalysisStatus = "Не удалось завершить анализ. Базовая маркировка доступна.")
                }
            }
        }
    }

    private fun speedChunkAt(
        text: String,
        offset: Int,
        config: ReaderSettings = settings.value,
    ): SpeedChunk? {
        return SpeedDialoguePlayback.next(
            text, offset, config, wordsForMode(config.speedMode, config.speedWordsPerFixation),
        )
    }

    private fun wordsForMode(mode: SpeedMode, configured: Int): Int = when (mode) {
        SpeedMode.RSVP -> 1
        SpeedMode.PHRASE -> configured.coerceIn(2, 7)
        SpeedMode.FOCUS -> configured.coerceIn(3, 7)
    }

    private fun prepareMetadata(book: BookRecord) {
        launchHandled(onError = { /* metadata is optional; importing the book already succeeded */ }) {
            if (book.format == BookFormat.PDF) {
                val count = within(30_000, "Слишком долго открывается PDF") { contentRepository.pdfPageCount(book) }
                libraryRepository.updateMetadata(book.id, BookMetadata(book.title, book.author), count)
            } else {
                val outline = within(60_000, "Слишком долго читается структура книги") { contentRepository.outline(book) }
                libraryRepository.updateMetadata(book.id, BookMetadata(outline.title, outline.author), outline.chapters.size)
            }
        }
    }

    private fun launchHandled(
        onError: (Throwable) -> Unit,
        block: suspend () -> Unit,
    ): Job = viewModelScope.launch {
        try {
            block()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            onError(error)
        }
    }

    private suspend fun <T> within(timeoutMillis: Long, timeoutMessage: String, block: suspend () -> T): T =
        withTimeoutOrNull(timeoutMillis) { block() } ?: error(timeoutMessage)

    override fun onCleared() {
        audiobookController.close()
        readerPrefetchJob?.cancel()
        readerJob?.cancel()
        cancelPdfRender()
        speedJob?.cancel()
        cancelBookDialogueAnalysis()
        searchJob?.cancel()
        importJob?.cancel()
        _pdf.value.bitmap?.let { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
        imageCache.snapshot().values.forEach { bitmap -> if (!bitmap.isRecycled) bitmap.recycle() }
        imageCache.evictAll()
        LibraryCoverCache.clear()
        contentRepository.close()
        super.onCleared()
    }

    private fun Throwable.userMessage(fallback: String): String =
        message?.takeIf { it.isNotBlank() } ?: fallback

    companion object {
        private const val MAX_SEARCH_HITS = 80
    }
}
