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

    @Test fun contextMenuSelectsSeveralFoldersAndMovesTheWholeGroup() {
        val moved = mutableListOf<Pair<String, String>>()
        showLibrary(folderFixture = true, onMoveFolder = { source, target -> moved += source to target })
        compose.onNodeWithTag("folder:Книги/Фэнтези/Серия один").performTouchInput { longClick() }
        compose.onNodeWithText("Выделить", useUnmergedTree = true).performClick()
        compose.onNodeWithTag("folder:Книги/Фэнтези/Серия два").performClick()
        compose.onNodeWithText("Выбрано: 2", substring = true, ignoreCase = true).assertExists()
        captureDialog("folder-selection", dialog = false)
        compose.onNodeWithTag("folder:Книги/Фэнтези/Серия один").performTouchInput { longClick() }
        compose.onNodeWithText("Копировать папки").assertIsDisplayed()
        compose.onNodeWithText("Редактировать книгу").assertDoesNotExist()
        captureDialog("folders")
        compose.onNodeWithText("Переместить в папку").performClick()
        compose.onNodeWithText("Переместить выбранные (2) в…").assertExists()
        compose.onNode(hasText("Серия один") and hasAnyAncestor(isDialog()), useUnmergedTree = true).assertDoesNotExist()
        compose.onNode(hasText("Серия два") and hasAnyAncestor(isDialog()), useUnmergedTree = true).assertDoesNotExist()
        compose.onNode(hasText("Вложенная") and hasAnyAncestor(isDialog()), useUnmergedTree = true).assertDoesNotExist()
        compose.onNode(hasText("Библиотека") and hasAnyAncestor(isDialog()), useUnmergedTree = true).performClick()
        compose.runOnIdle { assertEquals(listOf("Книги/Фэнтези/Серия один" to "", "Книги/Фэнтези/Серия два" to ""), moved) }
        compose.onNodeWithText("Выбрано: 2", substring = true, ignoreCase = true).assertDoesNotExist()
    }

    @Test fun emptyAreaSelectsAllFoldersAndDeletionDoesNotDeleteBooks() {
        val deleted = mutableListOf<String>()
        var bookDeletionCalled = false
        showLibrary(folderFixture = true, onDeleteFolder = { deleted += it }, onDeleteBooks = { bookDeletionCalled = true })
        compose.onNodeWithTag("library-content").performTouchInput { longClick(Offset(width / 2f, height - 12f)) }
        compose.onNodeWithText("Выделить все папки").performClick()
        compose.onNodeWithText("Выбрано: 2", substring = true, ignoreCase = true).assertExists()
        compose.onNodeWithTag("folder:Книги/Фэнтези/Серия один").performTouchInput { longClick() }
        compose.onNodeWithText("Удалить", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Удалить выбранные папки?").assertExists()
        compose.onNodeWithText("Удалить 2", useUnmergedTree = true).performClick()
        compose.runOnIdle {
            assertEquals(setOf("Книги/Фэнтези/Серия один", "Книги/Фэнтези/Серия два"), deleted.toSet())
            assertEquals(false, bookDeletionCalled)
        }
    }

    private fun showLibrary(
        onCreate: (String, String) -> Unit = { _, _ -> },
        folderFixture: Boolean = false,
        onMoveFolder: (String, String) -> Unit = { _, _ -> },
        onDeleteFolder: (String) -> Unit = {},
        onDeleteBooks: (List<String>) -> Unit = {},
    ) {
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
                    folders = if (folderFixture) listOf("Книги/Фэнтези/Серия один/Вложенная", "Книги/Фэнтези/Серия два")
                        else listOf("Книги/Фэнтези"), currentFolder = "Книги/Фэнтези", onFolderChanged = {},
                    audiobooks = AudiobookState(), onImportAudio = { _, _ -> }, onPlayAudio = {}, onMoveAudio = { _, _ -> }, onRemoveAudio = {},
                    settings = settings, importState = ImportUiState(), onCancelImport = {}, onImport = { _, _ -> }, onImportFolder = { _, _ -> },
                    onOpen = {}, onSpeed = {}, onDelete = {}, onDeleteBooks = onDeleteBooks, onCopyBook = { _, _ -> }, onCopyBooks = { _, _ -> },
                    onMoveBook = { _, _, _ -> }, onMoveBooks = { _, _, _ -> }, onDuplicateBook = { _, _ -> }, onExportBook = { _, _ -> },
                    onCreateFolder = onCreate, onCreateFolderWithBooks = { _, _, _, _ -> null }, onRenameFolder = { _, _ -> },
                    onCopyFolder = { _, _ -> }, onMoveFolder = onMoveFolder, onDeleteFolder = onDeleteFolder, onEditMetadata = { _, _, _ -> },
                    onSetCover = { _, _ -> }, onRemoveCover = {}, canUndoLibraryOperation = false, onUndoLibraryOperation = {}, onSettings = {},
                    onUpdateSettings = { update -> settings = update(settings) },
                )
            }
        }
    }

    private fun book(id: String, title: String) = BookRecord(id, title, "Автор", "", "$title.txt", BookFormat.TXT,
        folderPaths = listOf("Книги/Фэнтези"))

    private fun captureDialog(kind: String, dialog: Boolean = true) {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "library112-menu-review").apply { mkdirs() }
        val bitmap = (if (dialog) compose.onNode(isDialog()) else compose.onRoot()).captureToImage().asAndroidBitmap()
        File(root, "$kind-$theme-$view.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}-{1}")
        fun configurations() = listOf("standard", "wasteland", "cyberpunk").flatMap { theme ->
            LibraryViewMode.entries.map { view -> arrayOf<Any>(theme, view) }
        }
    }
}
