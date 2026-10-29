package io.fluxreader.app.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import io.fluxreader.app.data.BookMetadata
import io.fluxreader.app.data.BookRecord
import io.fluxreader.app.data.LibraryGroupMode
import io.fluxreader.app.data.LibraryCoverCache
import io.fluxreader.app.data.LibrarySearchIndex
import io.fluxreader.app.data.LibraryFolderTree
import io.fluxreader.app.data.LibraryFolderSummary
import io.fluxreader.app.data.LibrarySortMode
import io.fluxreader.app.data.LibraryViewMode
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.ReaderThemeMode
import io.fluxreader.app.data.useWastelandDesign
import io.fluxreader.app.data.useCyberpunkDesign
import io.fluxreader.app.ImportUiState
import io.fluxreader.app.audio.AudiobookState
import io.fluxreader.app.audio.AudiobookEntry
import io.fluxreader.app.audio.AudiobookFormatPolicy

private enum class TransferMode { COPY, MOVE }
private enum class WastelandLibraryFilter { ALL, BOOKS, PDF, TXT, AUDIO }

private data class BookTransferRequest(
    val book: BookRecord,
    val mode: TransferMode,
    val sourceFolder: String?,
)

private data class FolderTransferRequest(
    val folderPath: String,
    val mode: TransferMode,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    books: List<BookRecord>,
    folders: List<String>,
    currentFolder: String,
    onFolderChanged: (String) -> Unit,
    audiobooks: AudiobookState,
    onImportAudio: (List<Uri>, String) -> Unit,
    onPlayAudio: (String) -> Unit,
    onMoveAudio: (String, String) -> Unit,
    onRemoveAudio: (String) -> Unit,
    settings: ReaderSettings,
    importState: ImportUiState,
    onCancelImport: () -> Unit,
    onImport: (List<Uri>, String) -> Unit,
    onImportFolder: (Uri, String) -> Unit,
    onOpen: (String) -> Unit,
    onSpeed: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteBooks: (List<String>) -> Unit,
    onCopyBook: (String, String) -> Unit,
    onCopyBooks: (List<String>, String) -> Unit,
    onMoveBook: (String, String?, String) -> Unit,
    onMoveBooks: (List<String>, String?, String) -> Unit,
    onDuplicateBook: (String, String?) -> Unit,
    onExportBook: (String, Uri) -> Unit,
    onCreateFolder: (String, String) -> Unit,
    onCreateFolderWithBooks: (String, String, List<String>, String?) -> String?,
    onRenameFolder: (String, String) -> Unit,
    onCopyFolder: (String, String) -> Unit,
    onMoveFolder: (String, String) -> Unit,
    onDeleteFolder: (String) -> Unit,
    onEditMetadata: (String, BookMetadata, List<String>) -> Unit,
    onSetCover: (String, Uri) -> Unit,
    onRemoveCover: (String) -> Unit,
    canUndoLibraryOperation: Boolean,
    onUndoLibraryOperation: () -> Unit,
    onSettings: () -> Unit,
    onUpdateSettings: ((ReaderSettings) -> ReaderSettings) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    var wastelandFilterName by rememberSaveable { mutableStateOf(WastelandLibraryFilter.ALL.name) }
    val wastelandFilter = runCatching { WastelandLibraryFilter.valueOf(wastelandFilterName) }.getOrDefault(WastelandLibraryFilter.ALL)
    val libraryFocusManager = LocalFocusManager.current
    val libraryKeyboardController = LocalSoftwareKeyboardController.current
    var selectedGroup by remember { mutableStateOf("") }
    // The folder is owned and persisted by AppViewModel; never reset it when LibraryScreen leaves Composition.
    var compactFiltersExpanded by remember { mutableStateOf(false) }
    var editingBook by remember { mutableStateOf<BookRecord?>(null) }
    var managingBook by remember { mutableStateOf<BookRecord?>(null) }
    var managingFolder by remember { mutableStateOf<String?>(null) }
    var importChooserVisible by rememberSaveable { mutableStateOf(false) }
    var wastelandSearchRequested by remember { mutableStateOf(false) }
    var createFolderParent by remember { mutableStateOf<String?>(null) }
    var renameFolderPath by remember { mutableStateOf<String?>(null) }
    var bookTransfer by remember { mutableStateOf<BookTransferRequest?>(null) }
    var folderTransfer by remember { mutableStateOf<FolderTransferRequest?>(null) }
    var deleteBookConfirm by remember { mutableStateOf<BookRecord?>(null) }
    var deleteFolderConfirm by remember { mutableStateOf<String?>(null) }
    var exportingBookId by remember { mutableStateOf<String?>(null) }
    var selectedBookIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedFolderPaths by remember { mutableStateOf<Set<String>>(emptySet()) }
    val selectedCount = selectedBookIds.size + selectedFolderPaths.size
    var selectionMode by rememberSaveable { mutableStateOf(false) }
    var selectionActionsVisible by remember { mutableStateOf(false) }
    var emptyAreaActionsVisible by remember { mutableStateOf(false) }
    val destinationFolder = LibraryInteractionPolicy.destinationFolder(settings.libraryGroupMode, currentFolder)
    // Freeze the destination before leaving the app for any system document picker.
    var pendingImportFolder by rememberSaveable { mutableStateOf("") }
    var batchTransferMode by remember { mutableStateOf<TransferMode?>(null) }
    var batchDeleteConfirm by remember { mutableStateOf(false) }
    var createSelectedFolderParent by remember { mutableStateOf<String?>(null) }
    var movingAudioId by remember { mutableStateOf<String?>(null) }
    var deletingAudioId by remember { mutableStateOf<String?>(null) }
    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) onImport(uris, pendingImportFolder)
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            onImportAudio(uris, pendingImportFolder)
        }
    }
    val archivePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) onImport(listOf(uri), pendingImportFolder)
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) onImportFolder(uri, pendingImportFolder)
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val book = editingBook
        if (uri != null && book != null) onSetCover(book.id, uri)
    }
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val bookId = exportingBookId
        exportingBookId = null
        if (uri != null && bookId != null) onExportBook(bookId, uri)
    }

    LaunchedEffect(wastelandSearchRequested) {
        if (wastelandSearchRequested) {
            searchFocusRequester.requestFocus()
            libraryKeyboardController?.show()
            wastelandSearchRequested = false
        }
    }

    // Include audio-only folders in the navigation tree. Group-mode changes must NOT erase the
    // remembered folder: returning from Settings/Reader or switching themes should keep it.
    val navigationFolders = remember(books, folders, audiobooks.entries) {
        LibraryFolderTree.allFolders(books, folders + audiobooks.entries.map { it.folderPath })
    }
    LaunchedEffect(settings.libraryGroupMode, books, navigationFolders, currentFolder) {
        if (settings.libraryGroupMode == LibraryGroupMode.FOLDER) {
            selectedGroup = ""
            var candidate = LibraryFolderTree.normalize(currentFolder)
            while (candidate.isNotBlank() && !LibraryFolderTree.containsFolder(books, candidate, navigationFolders)) {
                candidate = LibraryFolderTree.parent(candidate)
            }
            if (candidate != currentFolder) onFolderChanged(candidate)
        }
    }

    val groupFilters = remember(books) {
        buildList {
            books.flatMap { it.groups }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
                .forEach { add("g:$it" to it) }
            books.map { it.series }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }
                .forEach { add("s:$it" to "Серия · $it") }
        }
    }
    val searchIndex = remember(books) { LibrarySearchIndex.build(books) }
    val baseVisibleBooks = remember(searchIndex, query, selectedGroup, settings.librarySortMode, settings.libraryGroupMode) {
        val filtered = LibrarySearchIndex.filter(
            entries = searchIndex,
            query = query,
            selectedGroup = selectedGroup,
            groupMode = settings.libraryGroupMode,
        )
        sortBooks(filtered, settings.librarySortMode)
    }
    val visibleBooks = remember(baseVisibleBooks, wastelandFilter) {
        baseVisibleBooks.filter { book ->
                when (wastelandFilter) {
                    WastelandLibraryFilter.ALL -> true
                    WastelandLibraryFilter.BOOKS -> book.format.name == "EPUB" || book.format.name == "FB2"
                    WastelandLibraryFilter.PDF -> book.format.name == "PDF"
                    WastelandLibraryFilter.TXT -> book.format.name == "TXT"
                    WastelandLibraryFilter.AUDIO -> false
                }
        }
    }
    val libraryBottomContentPadding = if ((settings.useWastelandDesign || settings.useCyberpunkDesign)) 18.dp else 104.dp
    val libraryCompactBottomContentPadding = if ((settings.useWastelandDesign || settings.useCyberpunkDesign)) 18.dp else 96.dp
    val visibleAudio = remember(audiobooks.entries, query, wastelandFilter) {
        audiobooks.entries.filter { entry ->
            (query.isBlank() || entry.name.contains(query, ignoreCase = true)) &&
                (wastelandFilter == WastelandLibraryFilter.ALL || wastelandFilter == WastelandLibraryFilter.AUDIO)
        }
    }
    val continueBook = remember(books) {
        books.asSequence()
            .filter { it.lastOpenedAt > 0L && it.progress in 0.001f..0.999f }
            .maxByOrNull { it.lastOpenedAt }
    }
    val knownFolders = navigationFolders
    val selectableBookIds = remember(visibleBooks, settings.libraryGroupMode, currentFolder, query) {
        val selectable = if (settings.libraryGroupMode == LibraryGroupMode.FOLDER && query.isBlank()) {
            LibraryFolderTree.directBooks(visibleBooks, currentFolder)
        } else {
            visibleBooks
        }
        selectable.mapTo(linkedSetOf()) { it.id }
    }
    val selectableFolderPaths = remember(visibleBooks, knownFolders, currentFolder, query, settings.libraryGroupMode) {
        if (settings.libraryGroupMode == LibraryGroupMode.FOLDER && query.isBlank()) {
            LibraryFolderTree.childFolders(visibleBooks, currentFolder, knownFolders).mapTo(linkedSetOf()) { it.path }
        } else emptySet()
    }
    val toggleFolderSelection: (String) -> Unit = { path ->
        selectedBookIds = emptySet()
        selectedFolderPaths = if (path in selectedFolderPaths) selectedFolderPaths - path else selectedFolderPaths + path
        selectionMode = selectedFolderPaths.isNotEmpty()
    }
    val manageFolder: (String) -> Unit = { path ->
        if (selectionMode) {
            selectedBookIds = emptySet()
            selectedFolderPaths = selectedFolderPaths + path
            selectionActionsVisible = true
        } else managingFolder = path
    }
    val toggleSelection: (BookRecord) -> Unit = { book ->
        selectedFolderPaths = emptySet()
        selectedBookIds = if (book.id in selectedBookIds) selectedBookIds - book.id else selectedBookIds + book.id
    }
    val startSelection: (BookRecord) -> Unit = { book ->
        selectedFolderPaths = emptySet()
        val result = LibraryInteractionPolicy.longPressBook(selectedBookIds, selectionMode, book.id)
        selectedBookIds = result.selectedIds
        selectionActionsVisible = result.showActions
        selectionMode = true
        libraryFocusManager.clearFocus(force = true)
        libraryKeyboardController?.hide()
    }

    BackHandler(enabled = selectionMode) {
        selectedBookIds = emptySet()
        selectedFolderPaths = emptySet()
        selectionMode = false
        batchTransferMode = null
        batchDeleteConfirm = false
        selectionActionsVisible = false
    }

    LaunchedEffect(books, knownFolders) {
        val existing = books.mapTo(hashSetOf()) { it.id }
        selectedBookIds = selectedBookIds.filterTo(linkedSetOf()) { it in existing }
        selectedFolderPaths = selectedFolderPaths.filterTo(linkedSetOf()) { it in knownFolders }
        if (selectionMode && selectedBookIds.isEmpty() && selectedFolderPaths.isEmpty()) {
            selectionMode = false
            selectionActionsVisible = false
        }
    }

    if (importChooserVisible) {
        AlertDialog(
            onDismissRequest = { importChooserVisible = false },
            title = { Text("Добавить") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LibraryActionButton(Icons.AutoMirrored.Filled.InsertDriveFile, "Книги", "EPUB, FB2, PDF или TXT") {
                        importChooserVisible = false
                        pendingImportFolder = destinationFolder
                        importPicker.launch(arrayOf("*/*"))
                    }
                    LibraryActionButton(Icons.Default.FolderZip, "Архив ZIP", "Импорт всех поддерживаемых книг внутри") {
                        importChooserVisible = false
                        pendingImportFolder = destinationFolder
                        archivePicker.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
                    }
                    LibraryActionButton(Icons.Default.MusicNote, "Аудиокниги", "MP3, M4B, M4A, WAV, FLAC и другие") {
                        importChooserVisible = false
                        pendingImportFolder = destinationFolder
                        audioPicker.launch(arrayOf("audio/*", "application/octet-stream"))
                    }
                    LibraryActionButton(Icons.Default.FolderOpen, "Папка", "Импорт с подпапками") {
                        importChooserVisible = false
                        pendingImportFolder = destinationFolder
                        folderPicker.launch(null)
                    }
                    LibraryActionButton(
                        Icons.Default.CreateNewFolder,
                        "Новая папка",
                        if (destinationFolder.isNotBlank()) "В текущей папке" else "В библиотеке",
                    ) {
                        importChooserVisible = false
                        createFolderParent = destinationFolder
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { importChooserVisible = false }) { Text("Закрыть") }
            },
        )
    }

    val libraryContent: @Composable (PaddingValues) -> Unit = { padding ->
        Column(Modifier.fillMaxSize().padding(padding).testTag("library-content").pointerInput(Unit) {
            // Child controls/cards consume their gestures; only unused library space reaches here.
            detectTapGestures(onLongPress = { emptyAreaActionsVisible = true })
        }) {
            if (importState.active) {
                ImportProgressCard(
                    state = importState,
                    onCancel = onCancelImport,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            if (books.isEmpty() && folders.isEmpty() && audiobooks.entries.isEmpty()) {
                EmptyLibrary(Modifier.weight(1f))
            } else {
                Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                            WastelandLibraryFilter.entries.forEach { filter ->
                                val label = when (filter) {
                                        WastelandLibraryFilter.ALL -> "Все"
                                        WastelandLibraryFilter.BOOKS -> "Книги"
                                        WastelandLibraryFilter.PDF -> "PDF"
                                        WastelandLibraryFilter.TXT -> "TXT"
                                        WastelandLibraryFilter.AUDIO -> "Аудио"
                                    }
                                if (LocalFluxThemeMode.current.isExperimentalChrome) {
                                    ExperimentalTab(label, wastelandFilter == filter) { wastelandFilterName = filter.name }
                                } else {
                                    FilterChip(selected = wastelandFilter == filter,
                                        onClick = { wastelandFilterName = filter.name }, label = { Text(label) })
                                }
                            }
                    }
                    if (settings.libraryShowContinue && wastelandFilter != WastelandLibraryFilter.AUDIO && continueBook != null && continueBook in visibleBooks) {
                        ContinueCard(
                            book = continueBook,
                            onOpen = onOpen,
                            onSpeed = onSpeed,
                            selected = continueBook.id in selectedBookIds,
                            selectionMode = selectionMode,
                            onToggleSelection = { toggleSelection(continueBook) },
                            onStartSelection = { startSelection(continueBook) },
                        )
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
                        singleLine = true,
                        leadingIcon = { FluxThemedIcon(FluxIconRole.SEARCH, Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            IconButton(onClick = { compactFiltersExpanded = !compactFiltersExpanded }) {
                                FluxThemedIcon(FluxIconRole.FILTER, Icons.Default.Tune, contentDescription = if (compactFiltersExpanded) "Скрыть фильтры" else "Фильтры")
                            }
                        },
                        placeholder = { Text("Поиск") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                libraryFocusManager.clearFocus(force = true)
                                libraryKeyboardController?.hide()
                            },
                        ),
                    )
                    if (compactFiltersExpanded) {
                        SortControls(settings.librarySortMode) { mode ->
                            onUpdateSettings { it.copy(librarySortMode = mode) }
                        }
                        GroupControls(settings.libraryGroupMode) { mode ->
                            onUpdateSettings { it.copy(libraryGroupMode = mode) }
                        }
                        if (settings.libraryGroupMode != LibraryGroupMode.FOLDER && groupFilters.isNotEmpty()) {
                            Row(
                                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                FilterChip(selected = selectedGroup.isBlank(), onClick = { selectedGroup = "" }, label = { Text("Все") })
                                groupFilters.forEach { (key, label) ->
                                    FilterChip(selected = selectedGroup == key, onClick = { selectedGroup = key }, label = { Text(label, maxLines = 1) })
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (settings.libraryGroupMode == LibraryGroupMode.FOLDER) {
                    FolderLibrary(
                        books = visibleBooks,
                        audioEntries = visibleAudio,
                        onPlayAudio = onPlayAudio,
                        onMoveAudio = { movingAudioId = it },
                        onRemoveAudio = { deletingAudioId = it },
                        folders = knownFolders,
                        currentFolder = currentFolder,
                        queryActive = query.isNotBlank(),
                        sortMode = settings.librarySortMode,
                        viewMode = settings.libraryViewMode,
                        onFolder = onFolderChanged,
                        onOpen = onOpen,
                        onSpeed = onSpeed,
                        onManageBook = { managingBook = it },
                        onManageFolder = manageFolder,
                        selectedFolderPaths = selectedFolderPaths,
                        onToggleFolderSelection = toggleFolderSelection,
                        selectedBookIds = selectedBookIds,
                        selectionMode = selectionMode,
                        onToggleSelection = toggleSelection,
                        onStartSelection = startSelection,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                } else if (visibleBooks.isEmpty() && visibleAudio.isEmpty()) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("Ничего не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else if (settings.libraryGroupMode != LibraryGroupMode.NONE && visibleBooks.isNotEmpty()) {
                    GroupedLibrary(
                        books = visibleBooks,
                        audioEntries = visibleAudio,
                        onPlayAudio = onPlayAudio,
                        onMoveAudio = { movingAudioId = it },
                        onRemoveAudio = { deletingAudioId = it },
                        mode = settings.libraryGroupMode,
                        viewMode = settings.libraryViewMode,
                        onOpen = onOpen,
                        onSpeed = onSpeed,
                        onManage = { managingBook = it },
                        selectedBookIds = selectedBookIds,
                        selectionMode = selectionMode,
                        onToggleSelection = toggleSelection,
                        onStartSelection = startSelection,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                    )
                } else if (settings.libraryViewMode == LibraryViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(136.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = libraryBottomContentPadding),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(visibleBooks, key = { it.id }) { book ->
                            BookGridCard(
                                book = book, onOpen = onOpen, onSpeed = onSpeed, onManage = { managingBook = book },
                                selected = book.id in selectedBookIds,
                                selectionMode = selectionMode,
                                onToggleSelection = { toggleSelection(book) },
                                onStartSelection = { startSelection(book) },
                            )
                        }
                        items(visibleAudio, key = { "audio:${it.id}" }) { audio ->
                            AudioLibraryCard(audio, LibraryViewMode.GRID, { onPlayAudio(audio.id) },
                                { movingAudioId = audio.id }, { deletingAudioId = audio.id })
                        }
                    }
                } else if (settings.libraryViewMode == LibraryViewMode.COMPACT) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(96.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 2.dp, bottom = libraryCompactBottomContentPadding),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(visibleBooks, key = { it.id }) { book ->
                            CompactBookCard(
                                book = book, onOpen = onOpen, onSpeed = onSpeed, onManage = { managingBook = book },
                                selected = book.id in selectedBookIds,
                                selectionMode = selectionMode,
                                onToggleSelection = { toggleSelection(book) },
                                onStartSelection = { startSelection(book) },
                            )
                        }
                        items(visibleAudio, key = { "audio:${it.id}" }) { audio ->
                            AudioLibraryCard(audio, LibraryViewMode.COMPACT, { onPlayAudio(audio.id) },
                                { movingAudioId = audio.id }, { deletingAudioId = audio.id })
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(14.dp, 4.dp, 14.dp, libraryBottomContentPadding),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(visibleBooks, key = { it.id }) { book ->
                            BookListCard(
                                book = book, onOpen = onOpen, onSpeed = onSpeed, onManage = { managingBook = book },
                                selected = book.id in selectedBookIds,
                                selectionMode = selectionMode,
                                onToggleSelection = { toggleSelection(book) },
                                onStartSelection = { startSelection(book) },
                            )
                        }
                        items(visibleAudio, key = { "audio:${it.id}" }) { audio ->
                            AudioLibraryCard(audio, LibraryViewMode.LIST, { onPlayAudio(audio.id) },
                                { movingAudioId = audio.id }, { deletingAudioId = audio.id })
                        }
                    }
                }
            }
        }
    }

    if ((settings.useWastelandDesign || settings.useCyberpunkDesign)) {
        // The Wasteland library deliberately does not use Material Scaffold.
        // alpha.59-alpha.62 showed that Scaffold's separately measured bars could collapse or
        // end up on top of the CRT content inside the physical device shell on edge-to-edge
        // phones. A simple constrained Column gives every region a real, non-overlapping slot
        // and keeps the hardware buttons' hit boxes exactly where they are drawn.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalExperimentalColors.current.background),
        ) {
            ExperimentalTopBar(
                title = if (selectionMode) "Выбрано: $selectedCount" else "Библиотека",
                subtitle = if (selectionMode) { if (selectedFolderPaths.isNotEmpty()) "Выбор папок" else "Выбор книг" } else "Книг: ${books.size} · ${libraryViewLabel(settings.libraryViewMode)}",
                actions = {
                    if (selectionMode) {
                        LibrarySelectionActionsMenu(onClick = { selectionActionsVisible = true })
                        IconButton(onClick = { selectedBookIds = emptySet(); selectedFolderPaths = emptySet(); selectionMode = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Закончить выделение", tint = LocalFluxThemeMode.current.chromeText)
                        }
                    } else {
                        IconButton(onClick = {
                            onUpdateSettings { current ->
                                current.copy(
                                    libraryViewMode = when (current.libraryViewMode) {
                                        LibraryViewMode.GRID -> LibraryViewMode.COMPACT
                                        LibraryViewMode.COMPACT -> LibraryViewMode.LIST
                                        LibraryViewMode.LIST -> LibraryViewMode.GRID
                                    },
                                )
                            }
                        }) {
                            Icon(
                                if (settings.libraryViewMode == LibraryViewMode.LIST) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                                contentDescription = "Вид библиотеки",
                                tint = LocalExperimentalColors.current.text,
                            )
                        }
                    }
                },
            )

            // The library owns the only weighted viewport. Nothing decorative is allowed to
            // occupy or overlay this Box, so book cards keep their real click targets.
            Box(Modifier.weight(1f).fillMaxWidth()) {
                libraryContent(PaddingValues(0.dp))
            }

            if (!selectionMode) ExperimentalHardwareRail(
                items = listOf(
                    WastelandNavItem("Книги", FluxIconRole.LIBRARY, Icons.AutoMirrored.Filled.MenuBook, selected = true) {
                        query = ""
                        wastelandFilterName = WastelandLibraryFilter.ALL.name
                        selectedGroup = ""
                        onFolderChanged("")
                        compactFiltersExpanded = false
                        selectedBookIds = emptySet()
                        selectedFolderPaths = emptySet()
                        selectionMode = false
                        libraryFocusManager.clearFocus(force = true)
                        libraryKeyboardController?.hide()
                    },
                    WastelandNavItem("Поиск", FluxIconRole.SEARCH, Icons.Default.Search) { wastelandSearchRequested = true },
                    WastelandNavItem("Добавить", FluxIconRole.ADD, Icons.Default.Add, enabled = !importState.active) { importChooserVisible = true },
                    WastelandNavItem("Настройки", FluxIconRole.SETTINGS, Icons.Default.Settings) { onSettings() },
                ),
            )
        }
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        if (selectionMode) {
                            Text("Выбрано: $selectedCount", fontWeight = FontWeight.SemiBold)
                        } else {
                            Column {
                                Text("Библиотека", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${books.size} книг",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                    actions = {
                        if (selectionMode) {
                            LibrarySelectionActionsMenu(onClick = { selectionActionsVisible = true })
                            IconButton(onClick = { selectedBookIds = emptySet(); selectedFolderPaths = emptySet(); selectionMode = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Закончить выделение")
                            }
                        } else {
                            if (canUndoLibraryOperation) {
                                IconButton(onClick = onUndoLibraryOperation) {
                                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Отменить последнее перемещение книг")
                                }
                            }
                            IconButton(
                                onClick = {
                                    onUpdateSettings { current ->
                                        current.copy(
                                            libraryViewMode = when (current.libraryViewMode) {
                                                LibraryViewMode.GRID -> LibraryViewMode.COMPACT
                                                LibraryViewMode.COMPACT -> LibraryViewMode.LIST
                                                LibraryViewMode.LIST -> LibraryViewMode.GRID
                                            },
                                        )
                                    }
                                },
                            ) {
                                Icon(
                                    when (settings.libraryViewMode) {
                                        LibraryViewMode.GRID -> Icons.Default.GridView
                                        LibraryViewMode.COMPACT -> Icons.Default.GridView
                                        LibraryViewMode.LIST -> Icons.AutoMirrored.Filled.List
                                    },
                                    contentDescription = "Вид библиотеки: ${libraryViewLabel(settings.libraryViewMode)}",
                                )
                            }
                            IconButton(onClick = onSettings) {
                                FluxThemedIcon(FluxIconRole.SETTINGS, Icons.Default.Settings, contentDescription = "Настройки")
                            }
                        }
                    },
                )
            },
            floatingActionButton = {
                if (!selectionMode && !importState.active) {
                    FloatingActionButton(onClick = { importChooserVisible = true }) {
                        FluxThemedIcon(FluxIconRole.ADD, Icons.Default.Add, contentDescription = "Добавить книги")
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background,
        ) { padding ->
            libraryContent(padding)
        }
    }


    if (emptyAreaActionsVisible) {
        LibraryActionsDialog(onDismiss = { emptyAreaActionsVisible = false }) {
            LibraryActionButton(Icons.Default.Add, "Добавить книги", enabled = !importState.active) {
                emptyAreaActionsVisible = false
                importChooserVisible = true
            }
            LibraryActionButton(Icons.Default.CreateNewFolder, "Новая папка") {
                emptyAreaActionsVisible = false
                createFolderParent = destinationFolder
            }
            LibraryActionButton(Icons.Default.SelectAll, "Выделить все видимые", enabled = selectableBookIds.isNotEmpty()) {
                emptyAreaActionsVisible = false
                selectedFolderPaths = emptySet()
                selectedBookIds = selectedBookIds + selectableBookIds
                selectionMode = true
            }
            LibraryActionButton(Icons.Default.Folder, "Выделить все папки", enabled = selectableFolderPaths.isNotEmpty()) {
                emptyAreaActionsVisible = false
                selectedBookIds = emptySet()
                selectedFolderPaths = selectableFolderPaths
                selectionMode = true
            }
            if (canUndoLibraryOperation) {
                LibraryActionButton(Icons.AutoMirrored.Filled.Undo, "Отменить последнее действие") {
                    emptyAreaActionsVisible = false
                    onUndoLibraryOperation()
                }
            }
        }
    }

    if (selectionActionsVisible && selectedCount > 0) {
        fun runSelectionAction(action: () -> Unit) {
            selectionActionsVisible = false
            action()
        }
        LibrarySelectionActionsDialog(
            selectedCount = selectedCount,
            foldersSelected = selectedFolderPaths.isNotEmpty(),
            allVisibleSelected = if (selectedFolderPaths.isNotEmpty()) selectedFolderPaths.containsAll(selectableFolderPaths)
                else selectableBookIds.isNotEmpty() && selectedBookIds.containsAll(selectableBookIds),
            onDismiss = { selectionActionsVisible = false },
            onSelectAll = {
                runSelectionAction {
                    if (selectedFolderPaths.isNotEmpty()) {
                        selectedFolderPaths = if (selectedFolderPaths.containsAll(selectableFolderPaths)) selectedFolderPaths - selectableFolderPaths
                            else selectedFolderPaths + selectableFolderPaths
                    } else {
                        selectedBookIds = if (selectableBookIds.isNotEmpty() && selectedBookIds.containsAll(selectableBookIds)) {
                            selectedBookIds - selectableBookIds
                        } else selectedBookIds + selectableBookIds
                    }
                    if (selectedBookIds.isEmpty() && selectedFolderPaths.isEmpty()) selectionMode = false
                }
            },
            onCopy = { runSelectionAction { batchTransferMode = TransferMode.COPY } },
            onCreateFolder = { runSelectionAction { createSelectedFolderParent = destinationFolder } },
            onMove = { runSelectionAction { batchTransferMode = TransferMode.MOVE } },
            onDelete = { runSelectionAction { batchDeleteConfirm = true } },
            onEditSingle = { runSelectionAction {
                books.firstOrNull { it.id in selectedBookIds }?.let { editingBook = it }
                selectedBookIds = emptySet(); selectedFolderPaths = emptySet(); selectionMode = false
            } },
            onManageSingle = { runSelectionAction {
                books.firstOrNull { it.id in selectedBookIds }?.let { managingBook = it }
                selectedBookIds = emptySet(); selectedFolderPaths = emptySet(); selectionMode = false
            } },
        )
    }

    movingAudioId?.let { id ->
        FolderDestinationDialog(
            title = "Переместить аудиокнигу в…",
            folders = knownFolders,
            includeRoot = true,
            excludedFolders = emptySet(),
            onDismiss = { movingAudioId = null },
            onSelect = { folder -> onMoveAudio(id, folder); movingAudioId = null },
        )
    }
    deletingAudioId?.let { id ->
        DestructiveConfirmDialog(
            title = "Убрать аудиокнигу?",
            message = "Запись будет убрана из библиотеки; исходный аудиофайл останется на устройстве.",
            onConfirm = { onRemoveAudio(id); deletingAudioId = null },
            onDismiss = { deletingAudioId = null },
        )
    }

    editingBook?.let { selected ->
        val book = books.firstOrNull { it.id == selected.id } ?: selected
        MetadataEditorDialog(
            book = book,
            onDismiss = { editingBook = null },
            onSave = { metadata, groups ->
                onEditMetadata(book.id, metadata, groups)
                editingBook = null
            },
            onPickCover = { coverPicker.launch(arrayOf("image/*")) },
            onRemoveCover = { onRemoveCover(book.id) },
            onDelete = {
                deleteBookConfirm = book
                editingBook = null
            },
        )
    }


    managingBook?.let { selected ->
        val book = books.firstOrNull { it.id == selected.id } ?: selected
        val sourceFolder = if (settings.libraryGroupMode == LibraryGroupMode.FOLDER && query.isBlank()) {
            LibraryFolderTree.normalize(currentFolder)
        } else null
        BookActionsDialog(
            onDismiss = { managingBook = null },
            onEdit = {
                managingBook = null
                editingBook = book
            },
            onCopy = {
                managingBook = null
                bookTransfer = BookTransferRequest(book, TransferMode.COPY, sourceFolder)
            },
            onMove = {
                managingBook = null
                bookTransfer = BookTransferRequest(book, TransferMode.MOVE, sourceFolder)
            },
            onDuplicate = {
                managingBook = null
                val target = if (settings.libraryGroupMode == LibraryGroupMode.FOLDER && query.isBlank()) currentFolder else null
                onDuplicateBook(book.id, target)
            },
            onExport = {
                managingBook = null
                exportingBookId = book.id
                exportPicker.launch(book.originalName.substringAfterLast('/').ifBlank { "${book.title}.${book.format.name.lowercase()}" })
            },
            onDelete = {
                managingBook = null
                deleteBookConfirm = book
            },
        )
    }

    managingFolder?.let { folderPath ->
        FolderActionsDialog(
            folderPath = folderPath,
            onDismiss = { managingFolder = null },
            onSelect = {
                managingFolder = null
                selectedBookIds = emptySet()
                selectedFolderPaths = setOf(folderPath)
                selectionMode = true
            },
            onOpen = {
                managingFolder = null
                onFolderChanged(folderPath)
            },
            onCreateSubfolder = {
                managingFolder = null
                createFolderParent = folderPath
            },
            onRename = {
                managingFolder = null
                renameFolderPath = folderPath
            },
            onCopy = {
                managingFolder = null
                folderTransfer = FolderTransferRequest(folderPath, TransferMode.COPY)
            },
            onMove = {
                managingFolder = null
                folderTransfer = FolderTransferRequest(folderPath, TransferMode.MOVE)
            },
            onDelete = {
                managingFolder = null
                deleteFolderConfirm = folderPath
            },
        )
    }

    createSelectedFolderParent?.let { parentPath ->
        FolderNameDialog(
            title = "Папка из выбранных книг (${selectedBookIds.size})",
            initialName = "",
            confirmLabel = "Создать и переместить",
            onDismiss = { createSelectedFolderParent = null },
            onConfirm = { name ->
                val source = if (settings.libraryGroupMode == LibraryGroupMode.FOLDER && query.isBlank()) currentFolder else null
                val created = onCreateFolderWithBooks(parentPath, name, selectedBookIds.toList(), source)
                if (created != null) {
                    onFolderChanged(created)
                    onUpdateSettings { it.copy(libraryGroupMode = LibraryGroupMode.FOLDER) }
                    selectedBookIds = emptySet()
                    selectionMode = false
                    createSelectedFolderParent = null
                }
            },
        )
    }

    createFolderParent?.let { parentPath ->
        FolderNameDialog(
            title = "Новая папка",
            initialName = "",
            confirmLabel = "Создать",
            onDismiss = { createFolderParent = null },
            onConfirm = { name ->
                onCreateFolder(parentPath, name)
                onUpdateSettings { it.copy(libraryGroupMode = LibraryGroupMode.FOLDER) }
                createFolderParent = null
            },
        )
    }

    renameFolderPath?.let { folderPath ->
        FolderNameDialog(
            title = "Переименовать папку",
            initialName = LibraryFolderTree.name(folderPath),
            confirmLabel = "Переименовать",
            onDismiss = { renameFolderPath = null },
            onConfirm = { name ->
                onRenameFolder(folderPath, name)
                renameFolderPath = null
            },
        )
    }

    bookTransfer?.let { request ->
        FolderDestinationDialog(
            title = if (request.mode == TransferMode.COPY) "Копировать книгу в…" else "Переместить книгу в…",
            folders = knownFolders,
            includeRoot = request.mode == TransferMode.MOVE,
            excludedFolders = if (request.mode == TransferMode.COPY) request.book.folderPaths.toSet() else emptySet(),
            onDismiss = { bookTransfer = null },
            onSelect = { target ->
                if (request.mode == TransferMode.COPY) {
                    onCopyBook(request.book.id, target)
                } else {
                    onMoveBook(request.book.id, request.sourceFolder, target)
                }
                bookTransfer = null
            },
        )
    }

    folderTransfer?.let { request ->
        val sourceParent = LibraryFolderTree.parent(request.folderPath)
        FolderDestinationDialog(
            title = if (request.mode == TransferMode.COPY) "Копировать папку в…" else "Переместить папку в…",
            folders = knownFolders.filterNot { LibraryFolderTree.isSameOrDescendant(it, request.folderPath) },
            includeRoot = request.mode == TransferMode.COPY || sourceParent.isNotBlank(),
            excludedFolders = if (request.mode == TransferMode.MOVE) setOf(sourceParent) else emptySet(),
            onDismiss = { folderTransfer = null },
            onSelect = { target ->
                if (request.mode == TransferMode.COPY) onCopyFolder(request.folderPath, target)
                else onMoveFolder(request.folderPath, target)
                folderTransfer = null
            },
        )
    }

    batchTransferMode?.let { mode ->
        val sourceFolder = if (settings.libraryGroupMode == LibraryGroupMode.FOLDER && query.isBlank()) {
            LibraryFolderTree.normalize(currentFolder)
        } else null
        FolderDestinationDialog(
            title = if (mode == TransferMode.COPY) {
                "Добавить выбранные ($selectedCount) в…"
            } else {
                "Переместить выбранные ($selectedCount) в…"
            },
            folders = LibraryInteractionPolicy.folderDestinations(knownFolders, selectedFolderPaths),
            includeRoot = mode == TransferMode.MOVE || selectedFolderPaths.isNotEmpty(),
            excludedFolders = emptySet(),
            onDismiss = { batchTransferMode = null },
            onSelect = { target ->
                val paths = LibraryInteractionPolicy.topLevelFolders(selectedFolderPaths)
                val ids = selectedBookIds.toList()
                paths.forEach { path ->
                    if (mode == TransferMode.COPY) onCopyFolder(path, target) else onMoveFolder(path, target)
                }
                if (mode == TransferMode.COPY && ids.isNotEmpty()) {
                    onCopyBooks(ids, target)
                } else if (ids.isNotEmpty()) {
                    onMoveBooks(ids, sourceFolder, target)
                }
                selectedFolderPaths = emptySet()
                selectedBookIds = emptySet()
                selectionMode = false
                batchTransferMode = null
            },
        )
    }

    if (batchDeleteConfirm && selectedCount > 0) {
        val count = selectedCount
        DestructiveConfirmDialog(
            title = if (selectedFolderPaths.isNotEmpty()) "Удалить выбранные папки?" else "Удалить выбранные книги?",
            message = if (selectedFolderPaths.isNotEmpty()) "Будет удалено папок: $count. Содержимое поднимется на уровень выше. Книги останутся в библиотеке." else "Будет удалено книг: $count. Внутренние копии файлов и обложек тоже будут удалены. Это действие нельзя отменить.",
            confirmLabel = "Удалить $count",
            onConfirm = {
                LibraryInteractionPolicy.topLevelFolders(selectedFolderPaths).forEach(onDeleteFolder)
                if (selectedBookIds.isNotEmpty()) onDeleteBooks(selectedBookIds.toList())
                selectedFolderPaths = emptySet()
                selectedBookIds = emptySet()
                selectionMode = false
                batchDeleteConfirm = false
            },
            onDismiss = { batchDeleteConfirm = false },
        )
    }

    deleteBookConfirm?.let { selected ->
        val book = books.firstOrNull { it.id == selected.id } ?: selected
        DestructiveConfirmDialog(
            title = "Удалить книгу?",
            message = "«${book.title}» и внутренняя копия файла будут удалены. Отменить это нельзя.",
            onConfirm = {
                onDelete(book.id)
                deleteBookConfirm = null
            },
            onDismiss = { deleteBookConfirm = null },
        )
    }

    deleteFolderConfirm?.let { folderPath ->
        DestructiveConfirmDialog(
            title = "Удалить папку?",
            message = "«${LibraryFolderTree.name(folderPath)}» будет удалена. Содержимое поднимется на уровень выше.",
            onConfirm = {
                onDeleteFolder(folderPath)
                deleteFolderConfirm = null
            },
            onDismiss = { deleteFolderConfirm = null },
        )
    }
}

@Composable
private fun LibraryActionsDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val actions: @Composable () -> Unit = {
            Column(Modifier.heightIn(max = 540.dp).verticalScroll(rememberScrollState()).padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(1.dp)) { content() }
        }
        if (LocalFluxThemeMode.current.isExperimentalChrome) {
            ExperimentalPanel(Modifier.width(292.dp), accent = true, contentPadding = 6.dp) { actions() }
        } else {
            Surface(modifier = Modifier.width(292.dp), shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 6.dp) { actions() }
        }
    }
}

@Composable
private fun BookActionsDialog(
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    LibraryActionsDialog(onDismiss) {
        LibraryActionButton(Icons.Default.Edit, "Изменить", onClick = onEdit)
        LibraryActionButton(Icons.Default.ContentCopy, "Копировать", onClick = onCopy)
        LibraryActionButton(Icons.AutoMirrored.Filled.DriveFileMove, "Переместить", onClick = onMove)
        LibraryActionButton(Icons.Default.FileCopy, "Дубликат", onClick = onDuplicate)
        LibraryActionButton(Icons.Default.IosShare, "Экспорт", onClick = onExport)
        LibraryActionButton(Icons.Default.DeleteOutline, "Удалить", destructive = true, onClick = onDelete)
    }
}

@Composable
private fun FolderActionsDialog(
    folderPath: String,
    onDismiss: () -> Unit,
    onSelect: () -> Unit,
    onOpen: () -> Unit,
    onCreateSubfolder: () -> Unit,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    LibraryActionsDialog(onDismiss) {
        LibraryActionButton(Icons.Default.Check, "Выделить", onClick = onSelect)
        LibraryActionButton(Icons.Default.FolderOpen, "Открыть", onClick = onOpen)
        LibraryActionButton(Icons.Default.CreateNewFolder, "Новая подпапка", onClick = onCreateSubfolder)
        LibraryActionButton(Icons.Default.Edit, "Переименовать", onClick = onRename)
        LibraryActionButton(Icons.Default.ContentCopy, "Копировать", onClick = onCopy)
        LibraryActionButton(Icons.AutoMirrored.Filled.DriveFileMove, "Переместить", onClick = onMove)
        LibraryActionButton(Icons.Default.FolderDelete, "Удалить", destructive = true, onClick = onDelete)
    }
}

@Composable
private fun LibraryActionButton(
    icon: ImageVector,
    label: String,
    supporting: String = "",
    destructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val actionColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    val themed = LocalFluxThemeMode.current.isExperimentalChrome
    val shape = if (themed) MaterialTheme.shapes.small else RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (themed) Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f), shape) else Modifier)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 10.dp, vertical = if (supporting.isBlank()) 8.dp else 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp),
            tint = (if (themed && !destructive) LocalFluxThemeMode.current.chromeAccent else actionColor)
                .copy(alpha = if (enabled) 1f else 0.38f))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = (if (destructive) actionColor else MaterialTheme.colorScheme.onSurface)
                    .copy(alpha = if (enabled) 1f else 0.38f),
            )
            if (supporting.isNotBlank()) {
                Text(
                    supporting,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (destructive) actionColor.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun FolderNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember(initialName) {
        mutableStateOf(
            TextFieldValue(
                text = initialName,
                selection = TextRange(0, initialName.length),
            ),
        )
    }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val normalizedName = LibraryFormPolicy.normalizedFolderName(name.text)

    fun dismissForm() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onDismiss()
    }

    fun submitForm() {
        if (!LibraryFormPolicy.canSubmitFolderName(name.text)) return
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onConfirm(normalizedName)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    AlertDialog(
        onDismissRequest = ::dismissForm,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название папки") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { submitForm() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        },
        confirmButton = {
            Button(onClick = ::submitForm, enabled = LibraryFormPolicy.canSubmitFolderName(name.text)) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = ::dismissForm) { Text("Отмена") } },
    )
}

@Composable
private fun FolderDestinationDialog(
    title: String,
    folders: List<String>,
    includeRoot: Boolean,
    excludedFolders: Set<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val excluded = remember(excludedFolders) { excludedFolders.map(LibraryFolderTree::normalize).map { it.lowercase() }.toSet() }
    val candidates = remember(folders, excluded) {
        folders.map(LibraryFolderTree::normalize)
            .filter { it.isNotBlank() && it.lowercase() !in excluded }
            .distinctBy { it.lowercase() }
            .sortedWith(compareBy<String> { it.count { ch -> ch == '/' } }.thenBy { it.lowercase() })
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(320.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                if (includeRoot && "" !in excluded) {
                    item(key = "destination-root") {
                        LibraryActionButton(Icons.Default.Home, "Библиотека") { onSelect("") }
                    }
                }
                items(candidates, key = { "destination:$it" }) { path ->
                    LibraryActionButton(Icons.Default.Folder, LibraryFolderTree.name(path), path) { onSelect(path) }
                }
                if (!includeRoot && candidates.isEmpty()) {
                    item {
                        Text(
                            "Нет другой папки. Сначала создайте её через +.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Закрыть") } },
    )
}

@Composable
private fun EmptyLibrary(modifier: Modifier = Modifier) {
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        Box(modifier.fillMaxWidth().padding(18.dp), contentAlignment = Alignment.Center) {
            ExperimentalPanel(Modifier.fillMaxWidth(), accent = true, contentPadding = 24.dp) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    FluxThemedIcon(FluxIconRole.LIBRARY, Icons.AutoMirrored.Filled.MenuBook,
                        contentDescription = null, modifier = Modifier.size(40.dp))
                    Text("Библиотека пуста", color = LocalFluxThemeMode.current.chromeAccent,
                        style = MaterialTheme.typography.titleLarge)
                    Text("Добавьте книги или папку.",
                        color = LocalFluxThemeMode.current.chromeText,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        return
    }
    Box(modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FluxThemedIcon(FluxIconRole.LIBRARY, Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(12.dp))
            Text("Здесь пока пусто", style = MaterialTheme.typography.titleLarge)
            Text("Добавьте книги или папку.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ContinueCard(
    book: BookRecord,
    onOpen: (String) -> Unit,
    onSpeed: (String) -> Unit,
    selected: Boolean,
    selectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onStartSelection: () -> Unit,
) {
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalPanel(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (selectionMode) onToggleSelection() else onOpen(book.id) },
                    onLongClick = onStartSelection,
                ),
            accent = selected || !selectionMode,
            contentPadding = 0.dp,
        ) {
            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                ExperimentalCoverFrame(Modifier.width(64.dp).aspectRatio(0.68f)) {
                    BookCover(book, Modifier.fillMaxSize(), maxSidePx = 320)
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text("Продолжить", style = MaterialTheme.typography.labelLarge, color = LocalFluxThemeMode.current.chromeAccent)
                    Text(book.title, style = MaterialTheme.typography.titleMedium, color = LocalFluxThemeMode.current.chromeText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val byline = listOf(book.author, book.series).filter { it.isNotBlank() }.joinToString(" · ")
                    if (byline.isNotBlank()) {
                        Text(byline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        ExperimentalProgress(book.progress, Modifier.weight(1f))
                        Text("${(book.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = LocalFluxThemeMode.current.chromeText)
                    }
                }
                if (selected) SelectionBadge(Modifier.padding(end = 5.dp))
                if (!selectionMode) SpeedCardButton(onClick = { onSpeed(book.id) })
            }
        }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                    onClick = { if (selectionMode) onToggleSelection() else onOpen(book.id) },
                    onLongClick = onStartSelection,
                ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            BookCover(book, Modifier.width(62.dp).aspectRatio(0.68f), maxSidePx = 320)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text("Продолжить", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val byline = listOf(book.author, book.series).filter { it.isNotBlank() }.joinToString(" · ")
                if (byline.isNotBlank()) Text(byline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { book.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            }
            if (selected) SelectionBadge(Modifier.padding(end = 5.dp))
            if (!selectionMode) SpeedCardButton(onClick = { onSpeed(book.id) })
        }
    }
}

@Composable
private fun ImportProgressCard(
    state: ImportUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(state.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    if (state.detail.isNotBlank()) Text(state.detail, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (state.total > 0) Text("${state.current.coerceAtMost(state.total)}/${state.total}",
                    style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Отменить импорт", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            val fraction = state.progress
            if (fraction == null) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            else ExperimentalProgress(fraction, Modifier.fillMaxWidth())
        }
    }
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalPanel(modifier, accent = true, contentPadding = 2.dp) { content() }
        return
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        tonalElevation = 2.dp,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(state.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    if (state.detail.isNotBlank()) {
                        Text(
                            state.detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.76f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (state.total > 0) {
                    Text(
                        "${state.current.coerceAtMost(state.total)}/${state.total}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Отменить импорт", modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            val fraction = state.progress
            if (fraction == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun SortControls(selected: LibrarySortMode, onSelect: (LibrarySortMode) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        LibrarySortMode.entries.forEach { mode ->
            if (LocalFluxThemeMode.current.isExperimentalChrome) {
                ExperimentalTab(sortLabel(mode), selected == mode) { onSelect(mode) }
            } else {
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                    leadingIcon = { Icon(sortIcon(mode), contentDescription = null, modifier = Modifier.size(17.dp)) },
                    label = { Text(sortLabel(mode)) },
                )
            }
        }
    }
}

@Composable
private fun GroupControls(selected: LibraryGroupMode, onSelect: (LibraryGroupMode) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        LibraryGroupMode.entries.forEach { mode ->
            val label = when (mode) {
                LibraryGroupMode.NONE -> "Все"
                LibraryGroupMode.FOLDER -> "Папки"
                LibraryGroupMode.CUSTOM -> "Группы"
                LibraryGroupMode.SERIES -> "Серии"
                LibraryGroupMode.AUTHOR -> "Авторы"
            }
            if (LocalFluxThemeMode.current.isExperimentalChrome) {
                ExperimentalTab(label, selected == mode) { onSelect(mode) }
            } else {
                FilterChip(
                    selected = selected == mode,
                    onClick = { onSelect(mode) },
                    leadingIcon = { Icon(groupIcon(mode), contentDescription = null, modifier = Modifier.size(17.dp)) },
                    label = { Text(label) },
                )
            }
        }
    }
}

private fun sortIcon(mode: LibrarySortMode): ImageVector = when (mode) {
    LibrarySortMode.RECENT -> Icons.Default.Schedule
    LibrarySortMode.TITLE -> Icons.Default.SortByAlpha
    LibrarySortMode.AUTHOR -> Icons.Default.Person
    LibrarySortMode.SERIES -> Icons.Default.CollectionsBookmark
    LibrarySortMode.ADDED -> Icons.Default.FolderSpecial
    LibrarySortMode.PROGRESS -> Icons.AutoMirrored.Filled.TrendingUp
}

private fun groupIcon(mode: LibraryGroupMode): ImageVector = when (mode) {
    LibraryGroupMode.NONE -> Icons.AutoMirrored.Filled.MenuBook
    LibraryGroupMode.FOLDER -> Icons.Default.Folder
    LibraryGroupMode.CUSTOM -> Icons.Default.FolderSpecial
    LibraryGroupMode.SERIES -> Icons.Default.CollectionsBookmark
    LibraryGroupMode.AUTHOR -> Icons.Default.Person
}

/** The toolbar and long press share one themed dialog. */
@Composable
private fun LibrarySelectionActionsMenu(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(Icons.Default.MoreVert, contentDescription = "Действия с выделенными книгами",
            tint = if (LocalFluxThemeMode.current.isExperimentalChrome) LocalFluxThemeMode.current.chromeText
                else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LibrarySelectionActionsDialog(
    selectedCount: Int,
    foldersSelected: Boolean,
    allVisibleSelected: Boolean,
    onDismiss: () -> Unit,
    onSelectAll: () -> Unit,
    onCopy: () -> Unit,
    onCreateFolder: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
    onEditSingle: () -> Unit,
    onManageSingle: () -> Unit,
) {
    LibraryActionsDialog(onDismiss) {
        Text("Выбрано: $selectedCount", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(10.dp))
        LibraryActionButton(Icons.Default.SelectAll,
            if (allVisibleSelected) "Снять выделение с видимых" else "Выделить все видимые", onClick = onSelectAll)
        if (selectedCount == 1 && !foldersSelected) {
            LibraryActionButton(Icons.Default.Edit, "Редактировать книгу", onClick = onEditSingle)
            LibraryActionButton(Icons.Default.MoreVert, "Другие действия с книгой", onClick = onManageSingle)
        }
        LibraryActionButton(Icons.Default.ContentCopy, if (foldersSelected) "Копировать папки" else "Добавить в папку", onClick = onCopy)
        if (!foldersSelected) LibraryActionButton(Icons.Default.CreateNewFolder, "Создать папку с выбранными", onClick = onCreateFolder)
        LibraryActionButton(Icons.AutoMirrored.Filled.DriveFileMove, "Переместить в папку", onClick = onMove)
        LibraryActionButton(Icons.Default.DeleteOutline, "Удалить", destructive = true, onClick = onDelete)
    }
}

@Composable
private fun FolderLibrary(
    books: List<BookRecord>,
    audioEntries: List<AudiobookEntry>,
    onPlayAudio: (String) -> Unit,
    onMoveAudio: (String) -> Unit,
    onRemoveAudio: (String) -> Unit,
    folders: List<String>,
    currentFolder: String,
    queryActive: Boolean,
    sortMode: LibrarySortMode,
    viewMode: LibraryViewMode,
    onFolder: (String) -> Unit,
    onOpen: (String) -> Unit,
    onSpeed: (String) -> Unit,
    onManageBook: (BookRecord) -> Unit,
    onManageFolder: (String) -> Unit,
    selectedFolderPaths: Set<String>,
    onToggleFolderSelection: (String) -> Unit,
    selectedBookIds: Set<String>,
    selectionMode: Boolean,
    onToggleSelection: (BookRecord) -> Unit,
    onStartSelection: (BookRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val normalizedFolder = LibraryFolderTree.normalize(currentFolder)
    val childFolders = remember(books, folders, normalizedFolder, queryActive) {
        if (queryActive) emptyList() else LibraryFolderTree.childFolders(books, normalizedFolder, folders)
    }
    val directBooks = remember(books, normalizedFolder, queryActive, sortMode) {
        val source = if (queryActive) books else LibraryFolderTree.directBooks(books, normalizedFolder)
        sortBooks(source, sortMode)
    }
    val directAudio = remember(audioEntries, normalizedFolder, queryActive) {
        audioEntries.filter { queryActive || LibraryFolderTree.normalize(it.folderPath) == normalizedFolder }
    }
    val experimentalMode = LocalFluxThemeMode.current
    val wasteland = experimentalMode.isExperimentalChrome
    val bottomContentPadding = if (wasteland) 18.dp else 104.dp

    when (viewMode) {
        LibraryViewMode.GRID, LibraryViewMode.COMPACT -> {
            val compact = viewMode == LibraryViewMode.COMPACT
            LazyVerticalGrid(
                columns = GridCells.Adaptive(if (compact) 96.dp else 136.dp),
                modifier = modifier,
                contentPadding = PaddingValues(
                    start = if (compact) 10.dp else 14.dp,
                    end = if (compact) 10.dp else 14.dp,
                    top = 4.dp,
                    bottom = bottomContentPadding,
                ),
                horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 14.dp),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "folder-breadcrumb:$normalizedFolder") {
                    FolderBreadcrumb(normalizedFolder, onFolder)
                }
                childFolders.forEach { folder ->
                    item(key = "folder:${folder.path}") {
                        FolderTile(folder, compact = compact, selected = folder.path in selectedFolderPaths,
                            onClick = { if (selectionMode) onToggleFolderSelection(folder.path) else onFolder(folder.path) },
                            onManage = { onManageFolder(folder.path) })
                    }
                }
                items(directBooks, key = { "folder-book:${it.id}" }) { book ->
                    if (compact) {
                        CompactBookCard(
                            book = book, onOpen = onOpen, onSpeed = onSpeed, onManage = { onManageBook(book) },
                            showFolderPath = queryActive,
                            selected = book.id in selectedBookIds,
                            selectionMode = selectionMode,
                            onToggleSelection = { onToggleSelection(book) },
                            onStartSelection = { onStartSelection(book) },
                        )
                    } else {
                        BookGridCard(
                            book = book, onOpen = onOpen, onSpeed = onSpeed, onManage = { onManageBook(book) },
                            showFolderPath = queryActive,
                            selected = book.id in selectedBookIds,
                            selectionMode = selectionMode,
                            onToggleSelection = { onToggleSelection(book) },
                            onStartSelection = { onStartSelection(book) },
                        )
                    }
                }
                items(directAudio, key = { "folder-audio:${it.id}" }) { audio ->
                    AudioLibraryCard(audio, viewMode, { onPlayAudio(audio.id) },
                        { onMoveAudio(audio.id) }, { onRemoveAudio(audio.id) })
                }
                if (!queryActive && directBooks.isEmpty() && directAudio.isEmpty() && childFolders.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }, key = "folder-empty:$normalizedFolder") {
                        Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                            Text("В этой папке нет поддерживаемых книг", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        LibraryViewMode.LIST -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = bottomContentPadding),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "folder-breadcrumb:$normalizedFolder") { FolderBreadcrumb(normalizedFolder, onFolder) }
                items(childFolders, key = { "folder:${it.path}" }) { folder ->
                    FolderRow(folder = folder, selected = folder.path in selectedFolderPaths,
                        onClick = { if (selectionMode) onToggleFolderSelection(folder.path) else onFolder(folder.path) },
                        onManage = { onManageFolder(folder.path) })
                }
                items(directBooks, key = { "folder-book:${it.id}" }) { book ->
                    BookListCard(
                        book = book, onOpen = onOpen, onSpeed = onSpeed, onManage = { onManageBook(book) },
                        showFolderPath = queryActive,
                        selected = book.id in selectedBookIds,
                        selectionMode = selectionMode,
                        onToggleSelection = { onToggleSelection(book) },
                        onStartSelection = { onStartSelection(book) },
                    )
                }
                items(directAudio, key = { "folder-audio:${it.id}" }) { audio ->
                    AudioLibraryCard(audio, LibraryViewMode.LIST, { onPlayAudio(audio.id) },
                        { onMoveAudio(audio.id) }, { onRemoveAudio(audio.id) })
                }
                if (!queryActive && directBooks.isEmpty() && directAudio.isEmpty() && childFolders.isEmpty()) {
                    item(key = "folder-empty:$normalizedFolder") {
                        Box(Modifier.fillMaxWidth().padding(28.dp), contentAlignment = Alignment.Center) {
                            Text("В этой папке нет поддерживаемых книг", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderBreadcrumb(currentFolder: String, onFolder: (String) -> Unit) {
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalPanel(Modifier.fillMaxWidth(), contentPadding = 5.dp) {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                ExperimentalTab("Библиотека", currentFolder.isBlank(), onClick = { onFolder("") })
                LibraryFolderTree.breadcrumbs(currentFolder).forEach { (name, path) ->
                    Text("/", color = LocalFluxThemeMode.current.chromeAccent)
                    ExperimentalTab(name, false, onClick = { onFolder(path) })
                }
            }
        }
        return
    }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = { onFolder("") }) {
            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(5.dp))
            Text("Библиотека")
        }
        LibraryFolderTree.breadcrumbs(currentFolder).forEach { (name, path) ->
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onFolder(path) }) { Text(name, maxLines = 1) }
        }
    }
}

@Composable
private fun FolderTile(
    folder: LibraryFolderSummary,
    compact: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onManage: () -> Unit,
) {
    val themed = LocalFluxThemeMode.current.isExperimentalChrome
    val tileContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth()
                .height(if (compact) 112.dp else 150.dp)
                .combinedClickable(onClick = onClick, onLongClick = onManage)
                .padding(if (compact) 8.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            FluxThemedIcon(FluxIconRole.FOLDER, Icons.Default.Folder,
                contentDescription = null, modifier = Modifier.size(if (compact) 34.dp else 56.dp))
            Text(folder.name, maxLines = 2, overflow = TextOverflow.Ellipsis,
                minLines = 2, textAlign = TextAlign.Center,
                color = if (themed) LocalFluxThemeMode.current.chromeText else MaterialTheme.colorScheme.onSurface,
                style = if (compact) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold)
            Text("${folder.bookCount} книг", style = MaterialTheme.typography.labelSmall,
                color = if (themed) LocalFluxThemeMode.current.chromeAccent else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Box(Modifier.testTag("folder:${folder.path}")) {
        if (themed) ExperimentalPanel(Modifier.fillMaxWidth(), accent = selected, contentPadding = 0.dp) { tileContent() }
        else Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { tileContent() }
        if (selected) SelectionBadge(Modifier.align(Alignment.TopStart).padding(6.dp))
    }
}

@Composable
private fun AudioLibraryCard(
    audio: AudiobookEntry,
    viewMode: LibraryViewMode,
    onPlay: () -> Unit,
    onMove: () -> Unit,
    onRemove: () -> Unit,
) {
    val compact = viewMode == LibraryViewMode.COMPACT
    val listing = viewMode == LibraryViewMode.LIST
    val format = audio.name.substringAfterLast('.', "Аудио").uppercase()
    var actions by remember(audio.id) { mutableStateOf(false) }
    val tileContent: @Composable () -> Unit = {
        if (listing) {
            Row(
                Modifier.fillMaxWidth().combinedClickable(onClick = onPlay, onLongClick = { actions = true })
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(Icons.Default.MusicNote, contentDescription = "Аудиокнига", modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.tertiary)
                Column(Modifier.weight(1f)) {
                    Text(audio.name.substringBeforeLast('.'), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(format, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
                IconButton(onClick = { actions = true }) { Icon(Icons.Default.MoreVert, "Действия с аудиокнигой") }
            }
        } else {
            Column(
                Modifier.fillMaxWidth().combinedClickable(onClick = onPlay, onLongClick = { actions = true })
                    .padding(if (compact) 8.dp else 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(0.85f).background(
                    MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, contentDescription = "Аудиокнига",
                        modifier = Modifier.size(if (compact) 36.dp else 58.dp),
                        tint = MaterialTheme.colorScheme.tertiary)
                }
                Text(audio.name.substringBeforeLast('.'), maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall)
                Text(format, color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
    Box {
        if (LocalFluxThemeMode.current.isExperimentalChrome) {
            ExperimentalPanel(Modifier.fillMaxWidth(), contentPadding = 0.dp) { tileContent() }
        } else {
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { tileContent() }
        }
        DropdownMenu(expanded = actions, onDismissRequest = { actions = false }) {
            DropdownMenuItem(text = { Text("Воспроизвести") }, onClick = { actions = false; onPlay() })
            DropdownMenuItem(text = { Text("Переместить в папку") }, onClick = { actions = false; onMove() })
            DropdownMenuItem(text = { Text("Убрать из библиотеки") }, onClick = { actions = false; onRemove() })
        }
    }
}

@Composable
private fun FolderRow(folder: LibraryFolderSummary, selected: Boolean, onClick: () -> Unit, onManage: () -> Unit) {
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalPanel(
            modifier = Modifier.testTag("folder:${folder.path}").fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = onManage),
            contentPadding = 10.dp,
            accent = selected,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (selected) SelectionBadge()
                FluxThemedIcon(FluxIconRole.FOLDER, Icons.Default.Folder,
                    contentDescription = null, modifier = Modifier.size(34.dp))
                Column(Modifier.weight(1f)) {
                    Text(folder.name, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold, color = LocalFluxThemeMode.current.chromeText,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${folder.bookCount} книг", style = MaterialTheme.typography.labelMedium,
                        color = LocalFluxThemeMode.current.chromeAccent)
                }
                IconButton(onClick = onManage) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Действия с папкой",
                        tint = LocalFluxThemeMode.current.chromeAccent)
                }
                Icon(Icons.Default.ChevronRight, contentDescription = "Открыть папку",
                    tint = LocalFluxThemeMode.current.chromeAccent)
            }
        }
        return
    }
    Card(
        modifier = Modifier.testTag("folder:${folder.path}")
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onManage,
            ),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) SelectionBadge(Modifier.padding(end = 8.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.padding(9.dp).size(24.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(folder.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${folder.bookCount} книг", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onManage) {
                Icon(Icons.Default.MoreVert, contentDescription = "Действия с папкой", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Открыть папку", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GroupedLibrary(
    books: List<BookRecord>,
    audioEntries: List<AudiobookEntry>,
    onPlayAudio: (String) -> Unit,
    onMoveAudio: (String) -> Unit,
    onRemoveAudio: (String) -> Unit,
    mode: LibraryGroupMode,
    viewMode: LibraryViewMode,
    onOpen: (String) -> Unit,
    onSpeed: (String) -> Unit,
    onManage: (BookRecord) -> Unit,
    selectedBookIds: Set<String>,
    selectionMode: Boolean,
    onToggleSelection: (BookRecord) -> Unit,
    onStartSelection: (BookRecord) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sections = remember(books, mode) {
        val map = linkedMapOf<String, MutableList<BookRecord>>()
        books.forEach { book ->
            val keys = when (mode) {
                LibraryGroupMode.NONE -> listOf("Все книги")
                LibraryGroupMode.FOLDER -> book.folderPaths.ifEmpty { listOf("Без папки") }
                LibraryGroupMode.CUSTOM -> book.groups.ifEmpty { listOf("Без группы") }
                LibraryGroupMode.SERIES -> listOf(book.series.ifBlank { "Без серии" })
                LibraryGroupMode.AUTHOR -> listOf(book.author.ifBlank { "Без автора" })
            }
            keys.distinct().forEach { key -> map.getOrPut(key) { mutableListOf() }.add(book) }
        }
        map.entries.sortedBy { it.key.lowercase() }.map { it.key to it.value.toList() }
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(top = 4.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(sections, key = { it.first }) { (name, sectionBooks) ->
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                        color = if (LocalFluxThemeMode.current.isExperimentalChrome) LocalFluxThemeMode.current.chromeText else MaterialTheme.colorScheme.onSurface)
                    Text(sectionBooks.size.toString(), style = MaterialTheme.typography.labelMedium,
                        color = if (LocalFluxThemeMode.current.isExperimentalChrome) LocalFluxThemeMode.current.chromeAccent else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(sectionBooks, key = { "${name}:${it.id}" }) { book ->
                        if (viewMode == LibraryViewMode.COMPACT) {
                            Box(Modifier.width(104.dp)) {
                                CompactBookCard(
                                    book = book, onOpen = onOpen, onSpeed = onSpeed, onManage = { onManage(book) },
                                    selected = book.id in selectedBookIds,
                                    selectionMode = selectionMode,
                                    onToggleSelection = { onToggleSelection(book) },
                                    onStartSelection = { onStartSelection(book) },
                                )
                            }
                        } else {
                            Box(Modifier.width(154.dp)) {
                                BookGridCard(
                                    book = book, onOpen = onOpen, onSpeed = onSpeed, onManage = { onManage(book) },
                                    selected = book.id in selectedBookIds,
                                    selectionMode = selectionMode,
                                    onToggleSelection = { onToggleSelection(book) },
                                    onStartSelection = { onStartSelection(book) },
                                )
                            }
                        }
                    }
                }
            }
        }
        if (audioEntries.isNotEmpty()) {
            item(key = "audiobooks-group") {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Аудиокниги", modifier = Modifier.padding(horizontal = 14.dp),
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    audioEntries.forEach { audio ->
                        AudioLibraryCard(audio, LibraryViewMode.LIST,
                            { onPlayAudio(audio.id) }, { onMoveAudio(audio.id) },
                            { onRemoveAudio(audio.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BookGridCard(
    book: BookRecord,
    onOpen: (String) -> Unit,
    onSpeed: (String) -> Unit,
    onManage: () -> Unit,
    showFolderPath: Boolean = false,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onStartSelection: () -> Unit = {},
) {
    val experimentalMode = LocalFluxThemeMode.current
    val wasteland = experimentalMode.isExperimentalChrome
    if (wasteland) {
        ExperimentalPanel(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (selectionMode) onToggleSelection() else onOpen(book.id) },
                    onLongClick = onStartSelection,
                ),
            accent = selected,
            contentPadding = 5.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {
                Box {
                    ExperimentalCoverFrame(
                        modifier = Modifier.fillMaxWidth().aspectRatio(0.68f),
                        selected = selected,
                    ) {
                        BookCover(book, Modifier.fillMaxSize())
                    }
                    if (selected) SelectionBadge(Modifier.align(Alignment.TopStart).padding(5.dp))
                    if (!selectionMode) {
                        SpeedCardButton(
                            onClick = { onSpeed(book.id) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
                            compact = true,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(book.title, style = MaterialTheme.typography.titleSmall, color = LocalFluxThemeMode.current.chromeAccent, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (book.author.isNotBlank()) {
                    Text(book.author, style = MaterialTheme.typography.bodySmall, color = LocalFluxThemeMode.current.chromeText.copy(alpha = 0.84f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (book.series.isNotBlank()) Text(seriesLabel(book), style = MaterialTheme.typography.labelSmall, color = LocalFluxThemeMode.current.chromeText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (showFolderPath && book.folderPaths.isNotEmpty()) Text(book.folderPaths.first(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    ExperimentalProgress(book.progress, Modifier.weight(1f))
                    Text("${(book.progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = LocalFluxThemeMode.current.chromeText)
                }
            }
        }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelection() else onOpen(book.id) },
                onLongClick = onStartSelection,
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(7.dp)) {
            Box {
                Card(modifier = Modifier.fillMaxWidth().aspectRatio(0.68f), shape = RoundedCornerShape(12.dp)) {
                    BookCover(book, Modifier.fillMaxSize())
                }
                if (selected) SelectionBadge(Modifier.align(Alignment.TopStart).padding(6.dp))
                if (!selectionMode) {
                    SpeedCardButton(
                        onClick = { onSpeed(book.id) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(5.dp),
                    )
                }
            }
            Spacer(Modifier.height(7.dp))
            Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (book.author.isNotBlank()) Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (book.series.isNotBlank()) Text(seriesLabel(book), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (showFolderPath && book.folderPaths.isNotEmpty()) Text(book.folderPaths.first(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(5.dp))
            LinearProgressIndicator(progress = { book.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CompactBookCard(
    book: BookRecord,
    onOpen: (String) -> Unit,
    onSpeed: (String) -> Unit,
    onManage: () -> Unit,
    showFolderPath: Boolean = false,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onStartSelection: () -> Unit = {},
) {
    val experimentalMode = LocalFluxThemeMode.current
    val wasteland = experimentalMode.isExperimentalChrome
    if (wasteland) {
        ExperimentalPanel(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (selectionMode) onToggleSelection() else onOpen(book.id) },
                    onLongClick = onStartSelection,
                ),
            accent = selected,
            contentPadding = 4.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {
                Box {
                    ExperimentalCoverFrame(Modifier.fillMaxWidth().aspectRatio(0.68f), selected = selected) {
                        BookCover(book, Modifier.fillMaxSize(), maxSidePx = 320)
                    }
                    if (selected) SelectionBadge(Modifier.align(Alignment.TopStart).padding(4.dp))
                    if (!selectionMode) {
                        SpeedCardButton(
                            onClick = { onSpeed(book.id) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                            compact = true,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(book.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = LocalFluxThemeMode.current.chromeAccent, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (book.author.isNotBlank()) Text(book.author, style = MaterialTheme.typography.labelSmall, color = LocalFluxThemeMode.current.chromeText.copy(alpha = 0.82f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (showFolderPath && book.folderPaths.isNotEmpty()) Text(book.folderPaths.first(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(3.dp))
                ExperimentalProgress(book.progress, Modifier.fillMaxWidth())
            }
        }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelection() else onOpen(book.id) },
                onLongClick = onStartSelection,
            ),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.68f)),
    ) {
        Column(Modifier.fillMaxWidth().padding(5.dp)) {
            Box {
                Card(modifier = Modifier.fillMaxWidth().aspectRatio(0.68f), shape = RoundedCornerShape(10.dp)) {
                    BookCover(book, Modifier.fillMaxSize(), maxSidePx = 320)
                }
                if (selected) SelectionBadge(Modifier.align(Alignment.TopStart).padding(5.dp))
                if (!selectionMode) {
                    SpeedCardButton(
                        onClick = { onSpeed(book.id) },
                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                        compact = true,
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(book.title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (book.author.isNotBlank()) Text(book.author, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (showFolderPath && book.folderPaths.isNotEmpty()) Text(book.folderPaths.first(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            LinearProgressIndicator(progress = { book.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(2.dp).padding(top = 1.dp))
        }
    }
}

@Composable
private fun BookListCard(
    book: BookRecord,
    onOpen: (String) -> Unit,
    onSpeed: (String) -> Unit,
    onManage: () -> Unit,
    showFolderPath: Boolean = false,
    selected: Boolean = false,
    selectionMode: Boolean = false,
    onToggleSelection: () -> Unit = {},
    onStartSelection: () -> Unit = {},
) {
    val experimentalMode = LocalFluxThemeMode.current
    val wasteland = experimentalMode.isExperimentalChrome
    if (wasteland) {
        ExperimentalPanel(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (selectionMode) onToggleSelection() else onOpen(book.id) },
                    onLongClick = onStartSelection,
                ),
            accent = selected,
            contentPadding = 7.dp,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (selected) SelectionBadge(Modifier.padding(end = 7.dp))
                ExperimentalCoverFrame(Modifier.width(68.dp).aspectRatio(0.68f), selected = selected) {
                    BookCover(book, Modifier.fillMaxSize(), maxSidePx = 320)
                }
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(book.title, style = MaterialTheme.typography.titleMedium, color = LocalFluxThemeMode.current.chromeAccent, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (book.author.isNotBlank()) Text(book.author, color = LocalFluxThemeMode.current.chromeText.copy(alpha = 0.82f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (book.series.isNotBlank()) Text(seriesLabel(book), style = MaterialTheme.typography.labelMedium, color = LocalFluxThemeMode.current.chromeText)
                    if (showFolderPath && book.folderPaths.isNotEmpty()) Text(book.folderPaths.first(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${book.format.name} · ${(book.progress * 100).toInt()}%${if (book.chapterCount > 0) " · ${book.chapterCount} CH" else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(5.dp))
                    ExperimentalProgress(book.progress, Modifier.fillMaxWidth())
                }
                if (!selectionMode) SpeedCardButton(onClick = { onSpeed(book.id) })
            }
        }
        return
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (selectionMode) onToggleSelection() else onOpen(book.id) },
                onLongClick = onStartSelection,
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selected) SelectionBadge(Modifier.padding(end = 8.dp))
            BookCover(book, Modifier.width(72.dp).aspectRatio(0.68f), maxSidePx = 320)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(book.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (book.author.isNotBlank()) Text(book.author, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (book.series.isNotBlank()) Text(seriesLabel(book), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (showFolderPath && book.folderPaths.isNotEmpty()) Text(book.folderPaths.first(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${book.format.name} · ${(book.progress * 100).toInt()}%${if (book.chapterCount > 0) " · ${book.chapterCount} гл." else ""}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(progress = { book.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            }
            if (!selectionMode) SpeedCardButton(onClick = { onSpeed(book.id) })
        }
    }
}

@Composable
private fun SpeedCardButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalPanel(modifier = modifier, accent = true, contentPadding = 0.dp) {
            IconButton(onClick = onClick, modifier = Modifier.size(if (compact) 30.dp else 36.dp)) {
                FluxThemedIcon(
                    FluxIconRole.SPEED,
                    Icons.Default.PlayArrow,
                    contentDescription = "Запустить скорочтение",
                    modifier = Modifier.size(if (compact) 17.dp else 20.dp),
                )
            }
        }
        return
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        tonalElevation = 2.dp,
    ) {
        IconButton(onClick = onClick, modifier = Modifier.size(if (compact) 32.dp else 38.dp)) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Запустить скорочтение",
                modifier = Modifier.size(if (compact) 18.dp else 21.dp),
            )
        }
    }
}

@Composable
private fun SelectionBadge(modifier: Modifier = Modifier) {
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalPanel(modifier = modifier, accent = true, contentPadding = 3.dp) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Выбрано",
                tint = LocalFluxThemeMode.current.chromeAccent,
                modifier = Modifier.size(15.dp),
            )
        }
        return
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = "Выбрано",
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(5.dp).size(16.dp),
        )
    }
}

@Composable
private fun BookCover(book: BookRecord, modifier: Modifier = Modifier, maxSidePx: Int = 480) {
    val context = LocalContext.current.applicationContext
    val bitmap by produceState(
        initialValue = LibraryCoverCache.peek(context, book.coverPath, maxSidePx),
        key1 = book.coverPath,
        key2 = maxSidePx,
    ) {
        value = LibraryCoverCache.load(context, book.coverPath, maxSidePx)
    }
    val coverShape = if (LocalFluxThemeMode.current.isExperimentalChrome) MaterialTheme.shapes.extraSmall else RoundedCornerShape(14.dp)
    Box(modifier.clip(coverShape), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = "Обложка ${book.title}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceVariant)),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
                    Text(book.title.take(1).uppercase(), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(6.dp))
                    Text(book.format.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun MetadataEditorDialog(
    book: BookRecord,
    onDismiss: () -> Unit,
    onSave: (BookMetadata, List<String>) -> Unit,
    onPickCover: () -> Unit,
    onRemoveCover: () -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember(book.id) { mutableStateOf(book.title) }
    var author by remember(book.id) { mutableStateOf(book.author) }
    var series by remember(book.id) { mutableStateOf(book.series) }
    var seriesIndex by remember(book.id) { mutableStateOf(if (book.seriesIndex > 0f) book.seriesIndex.toString().removeSuffix(".0") else "") }
    var groups by remember(book.id) { mutableStateOf(book.groups.joinToString(", ")) }
    var publisher by remember(book.id) { mutableStateOf(book.publisher) }
    var year by remember(book.id) { mutableStateOf(book.year) }
    var language by remember(book.id) { mutableStateOf(book.language) }
    var description by remember(book.id) { mutableStateOf(book.description) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val canSave = LibraryFormPolicy.canSaveBookMetadata(title)
    val nextAction = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) })

    fun dismissForm() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onDismiss()
    }

    fun saveForm() {
        if (!canSave) return
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onSave(
            BookMetadata(
                title = LibraryFormPolicy.normalizedBookTitle(title),
                author = author.trim(),
                series = series.trim(),
                seriesIndex = seriesIndex.replace(',', '.').toFloatOrNull() ?: 0f,
                description = description.trim(),
                publisher = publisher.trim(),
                language = language.trim(),
                year = year.trim(),
            ),
            groups.split(',').map { it.trim() }.filter { it.isNotBlank() },
        )
    }

    AlertDialog(
        onDismissRequest = ::dismissForm,
        title = { Text("Метаданные книги") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                item {
                    OutlinedTextField(
                        title, { title = it }, label = { Text("Название") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                        keyboardActions = nextAction,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        author, { author = it }, label = { Text("Автор") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        keyboardActions = nextAction,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            series, { series = it }, label = { Text("Серия") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Next),
                            keyboardActions = nextAction,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            seriesIndex,
                            { seriesIndex = it.filter { ch -> ch.isDigit() || ch == '.' || ch == ',' } },
                            label = { Text("№") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            keyboardActions = nextAction,
                            modifier = Modifier.width(82.dp),
                        )
                    }
                }
                if (book.folderPaths.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Папки", style = MaterialTheme.typography.labelLarge)
                            book.folderPaths.forEach { path ->
                                Text(path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        groups, { groups = it }, label = { Text("Группы через запятую") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        keyboardActions = nextAction,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            publisher, { publisher = it }, label = { Text("Издательство") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                            keyboardActions = nextAction,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            year, { year = it.take(24) }, label = { Text("Год") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = nextAction,
                            modifier = Modifier.width(90.dp),
                        )
                    }
                }
                item {
                    OutlinedTextField(
                        language, { language = it }, label = { Text("Язык") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                        keyboardActions = nextAction,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    OutlinedTextField(
                        description, { description = it }, label = { Text("Описание") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Default),
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        BookCover(book, Modifier.width(74.dp).aspectRatio(0.68f), maxSidePx = 640)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                onPickCover()
                            }) {
                                Icon(Icons.Default.Palette, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Выбрать обложку")
                            }
                            TextButton(onClick = onRemoveCover) { Text("Вернуть автообложку") }
                        }
                    }
                }
                item {
                    TextButton(
                        onClick = {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            onDelete()
                        },
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text("Удалить книгу", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = ::saveForm, enabled = canSave) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = ::dismissForm) { Text("Отмена") } },
    )
}

private fun libraryViewLabel(mode: LibraryViewMode): String = when (mode) {
    LibraryViewMode.GRID -> "Сетка"
    LibraryViewMode.COMPACT -> "Компактно"
    LibraryViewMode.LIST -> "Список"
}

private fun sortBooks(books: List<BookRecord>, mode: LibrarySortMode): List<BookRecord> = when (mode) {
    LibrarySortMode.RECENT -> books.sortedByDescending { it.lastOpenedAt }
    LibrarySortMode.TITLE -> books.sortedBy { it.title.lowercase() }
    LibrarySortMode.AUTHOR -> books.sortedWith(compareBy<BookRecord> { it.author.lowercase() }.thenBy { it.title.lowercase() })
    LibrarySortMode.SERIES -> books.sortedWith(compareBy<BookRecord> { it.series.ifBlank { "~" }.lowercase() }.thenBy { it.seriesIndex }.thenBy { it.title.lowercase() })
    LibrarySortMode.ADDED -> books.sortedByDescending { it.addedAt }
    LibrarySortMode.PROGRESS -> books.sortedByDescending { it.progress }
}

private fun sortLabel(mode: LibrarySortMode): String = when (mode) {
    LibrarySortMode.RECENT -> "Недавние"
    LibrarySortMode.TITLE -> "Название"
    LibrarySortMode.AUTHOR -> "Автор"
    LibrarySortMode.SERIES -> "Серия"
    LibrarySortMode.ADDED -> "Добавлено"
    LibrarySortMode.PROGRESS -> "Прогресс"
}

private fun seriesLabel(book: BookRecord): String = buildString {
    append(book.series)
    if (book.seriesIndex > 0f) append(" · #").append(book.seriesIndex.toString().removeSuffix(".0"))
}
