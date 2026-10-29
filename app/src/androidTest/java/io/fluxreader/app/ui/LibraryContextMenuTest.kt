package io.fluxreader.app.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.fluxreader.app.ImportUiState
import io.fluxreader.app.audio.AudiobookState
import io.fluxreader.app.data.*
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import java.io.File

@RunWith(Parameterized::class)
class LibraryContextMenuTest(private val theme: String, private val view: LibraryViewMode) {
    @get:Rule val compose = createComposeRule()

    @Test fun longPressInSelectionOpensGroupActionsAndKeepsBothBooks() {
        showLibrary()
        compose.onNodeWithText("Роман один").performTouchInput { longClick() }
        compose.onNodeWithText("Выбрано: 1", substring = true, ignoreCase = true).assertExists()
        compose.onNodeWithText("Роман два").performClick()
        compose.onNodeWithText("Выбрано: 2", substring = true, ignoreCase = true).assertExists()
        compose.onNodeWithText("Роман один").performTouchInput { longClick() }
        compose.onNodeWithText("Создать папку с выбранными").assertIsDisplayed()
        compose.onNodeWithText("Редактировать книгу").assertDoesNotExist()
        captureDialog("group")
        compose.onNodeWithText("Переместить в папку").performClick()
        compose.onNodeWithText("Переместить выбранные (2) в…").assertExists()
    }

    @Test fun emptySpaceMenuCreatesFolderInsideTheOpenSubfolderEvenDuringSearch() {
        var created: Pair<String, String>? = null
        showLibrary(onCreate = { parent, name -> created = parent to name })
        compose.onNodeWithText("Поиск").performTextInput("Роман")
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.waitForIdle()
        compose.onNodeWithTag("library-content").performTouchInput {
            longClick(Offset(width / 2f, height - 12f))
        }
        captureDialog("empty")
        compose.onNodeWithText("Новая папка").performClick()
        compose.onNodeWithText("Название папки").performTextInput("Новая серия")
        compose.onNodeWithText("Создать", useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals("Книги/Фэнтези" to "Новая серия", created) }
    }

    private fun showLibrary(onCreate: (String, String) -> Unit = { _, _ -> }) {
        compose.setContent {
            var settings by remember { mutableStateOf(ReaderSettings(
                libraryGroupMode = LibraryGroupMode.FOLDER,
                libraryViewMode = view,
                libraryShowContinue = false,
                experimentalDesignEnabled = theme != "standard",
                experimentalFrameStyle = if (theme == "cyberpunk") ReaderThemeMode.CYBERPUNK_NEON else ReaderThemeMode.WASTELAND_TERMINAL,
                experimentalCrtEnabled = false,
            )) }
            FluxTheme(settings) {
                LibraryScreen(
                    books = listOf(book("one", "Роман один"), book("two", "Роман два")),
                    folders = listOf("Книги/Фэнтези"), currentFolder = "Книги/Фэнтези", onFolderChanged = {},
                    audiobooks = AudiobookState(), onImportAudio = { _, _ -> }, onPlayAudio = {}, onMoveAudio = { _, _ -> }, onRemoveAudio = {},
                    settings = settings, importState = ImportUiState(), onCancelImport = {}, onImport = { _, _ -> }, onImportFolder = { _, _ -> },
                    onOpen = {}, onSpeed = {}, onDelete = {}, onDeleteBooks = {}, onCopyBook = { _, _ -> }, onCopyBooks = { _, _ -> },
                    onMoveBook = { _, _, _ -> }, onMoveBooks = { _, _, _ -> }, onDuplicateBook = { _, _ -> }, onExportBook = { _, _ -> },
                    onCreateFolder = onCreate, onCreateFolderWithBooks = { _, _, _, _ -> null }, onRenameFolder = { _, _ -> },
                    onCopyFolder = { _, _ -> }, onMoveFolder = { _, _ -> }, onDeleteFolder = {}, onEditMetadata = { _, _, _ -> },
                    onSetCover = { _, _ -> }, onRemoveCover = {}, canUndoLibraryOperation = false, onUndoLibraryOperation = {}, onSettings = {},
                    onUpdateSettings = { update -> settings = update(settings) },
                )
            }
        }
    }

    private fun book(id: String, title: String) = BookRecord(id, title, "Автор", "", "$title.txt", BookFormat.TXT,
        folderPaths = listOf("Книги/Фэнтези"))

    private fun captureDialog(kind: String) {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "library111-menu-review").apply { mkdirs() }
        val bitmap = compose.onNode(isDialog()).captureToImage().asAndroidBitmap()
        File(root, "$kind-$theme-$view.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}-{1}")
        fun configurations() = listOf("standard", "wasteland", "cyberpunk").flatMap { theme ->
            LibraryViewMode.entries.map { view -> arrayOf<Any>(theme, view) }
        }
    }
}
