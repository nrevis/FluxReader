package io.fluxreader.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.fluxreader.app.AppRoute
import io.fluxreader.app.AppViewModel
import io.fluxreader.app.data.ReaderThemeMode
import io.fluxreader.app.data.activeUiTheme
import io.fluxreader.app.data.useWastelandDesign
import io.fluxreader.app.data.useCyberpunkDesign

@Composable
fun FluxReaderApp(viewModel: AppViewModel) {
    val route by viewModel.route.collectAsState()
    val books by viewModel.books.collectAsState()
    val audiobooks by viewModel.audiobooks.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val libraryFolder by viewModel.libraryFolder.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val reader by viewModel.reader.collectAsState()
    val pdf by viewModel.pdf.collectAsState()
    val speed by viewModel.speed.collectAsState()
    val message by viewModel.message.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val canUndoLibraryOperation by viewModel.canUndoLibraryOperation.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        viewModel.clearMessage()
    }

    if (route != AppRoute.LIBRARY) BackHandler { viewModel.navigateBack() }

    FluxTheme(settings) {
        // Reader and Speed Reader explicitly report their immersive state to the
        // ancestor device shell. System-bar visibility alone is not an inset contract:
        // safeDrawing may still reserve the display-cutout/status band.
        var immersiveReader by remember(route) { mutableStateOf(false) }
        val reportImmersive: (Boolean) -> Unit = remember(route) {
            { enabled -> immersiveReader = enabled }
        }
        val routeContent: @Composable () -> Unit = {
            when (route) {
                AppRoute.LIBRARY -> LibraryScreen(
                    books = books,
                    folders = folders,
                    currentFolder = libraryFolder,
                    onFolderChanged = viewModel::setLibraryFolder,
                    audiobooks = audiobooks,
                    onImportAudio = viewModel::importAudiobooks,
                    onPlayAudio = viewModel::openAudiobook,
                    onMoveAudio = viewModel::moveAudiobook,
                    onRemoveAudio = viewModel::removeAudiobook,
                    settings = settings,
                    importState = importState,
                    onCancelImport = viewModel::cancelImport,
                    onImport = viewModel::importBooks,
                    onImportFolder = viewModel::importFolder,
                    onOpen = viewModel::openBook,
                    onSpeed = viewModel::openSpeed,
                    onDelete = viewModel::deleteBook,
                    onDeleteBooks = viewModel::deleteBooks,
                    onCopyBook = viewModel::copyBookToFolder,
                    onCopyBooks = viewModel::copyBooksToFolder,
                    onMoveBook = viewModel::moveBookToFolder,
                    onMoveBooks = viewModel::moveBooksToFolder,
                    onDuplicateBook = viewModel::duplicateBook,
                    onExportBook = viewModel::exportBook,
                    onCreateFolder = viewModel::createLibraryFolder,
                    onCreateFolderWithBooks = viewModel::createFolderWithBooks,
                    onRenameFolder = viewModel::renameLibraryFolder,
                    onCopyFolder = viewModel::copyLibraryFolder,
                    onMoveFolder = viewModel::moveLibraryFolder,
                    onDeleteFolder = viewModel::deleteLibraryFolder,
                    onEditMetadata = viewModel::editBookMetadata,
                    onSetCover = viewModel::setBookCover,
                    onRemoveCover = viewModel::removeBookCover,
                    canUndoLibraryOperation = canUndoLibraryOperation,
                    onUndoLibraryOperation = viewModel::undoLibraryOperation,
                    onSettings = viewModel::showSettings,
                    onUpdateSettings = viewModel::updateSettings,
                )
                AppRoute.READER -> ReaderScreen(
                    state = reader,
                    settings = settings,
                    viewModel = viewModel,
                    onBack = viewModel::navigateBack,
                    onSpeed = { reader.bookId?.let(viewModel::openSpeed) },
                    onImmersiveChanged = reportImmersive,
                )
                AppRoute.SPEED -> SpeedReaderScreen(
                    state = speed,
                    settings = settings,
                    viewModel = viewModel,
                    title = speed.bookId?.let { id -> books.firstOrNull { it.id == id }?.title }.orEmpty(),
                    onBack = viewModel::navigateBack,
                    onImmersiveChanged = reportImmersive,
                )
                AppRoute.PDF -> PdfReaderScreen(
                    state = pdf,
                    title = pdf.bookId?.let { id -> books.firstOrNull { it.id == id }?.title }.orEmpty(),
                    settings = settings,
                    viewModel = viewModel,
                    onBack = viewModel::navigateBack,
                    onSpeed = { pdf.bookId?.let(viewModel::openSpeed) },
                )
                AppRoute.AUDIOBOOKS -> AudiobooksScreen(viewModel = viewModel, settings = settings, onBack = viewModel::navigateBack)
                AppRoute.SETTINGS -> SettingsScreen(
                    settings = settings,
                    viewModel = viewModel,
                    onBack = viewModel::navigateBack,
                )
            }
        }

        Box(Modifier.fillMaxSize()) {
            when {
                settings.useWastelandDesign -> WastelandDeviceFrame(immersive = immersiveReader) {
                    routeContent()
                    // Glass belongs to the measured green display, not to the metal chassis.
                    // Draw AFTER every route (library, folders, reading, settings, audiobooks).
                    ExperimentalUiOverlay(settings.activeUiTheme, settings, immersiveReader)
                }
                settings.useCyberpunkDesign -> CyberpunkDeviceFrame(immersive = immersiveReader) {
                    routeContent()
                    ExperimentalUiOverlay(settings.activeUiTheme, settings, immersiveReader)
                }
                else -> routeContent()
            }
            SnackbarHost(
                hostState = snackbar,
                modifier = Modifier.align(Alignment.BottomCenter).padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = if (settings.activeUiTheme.isExperimentalChrome) 18.dp else 16.dp,
                ),
            )
        }
    }
}
