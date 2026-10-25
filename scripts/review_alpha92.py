#!/usr/bin/env python3
"""alpha.92 regression critic for library navigation, themes, audio filters and multi-select.
Checks source invariants only; it does not pretend to validate Android rendering.
"""
from pathlib import Path
import sys
ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'app/src/main/java/io/fluxreader/app'
app = (BASE / 'AppViewModel.kt').read_text()
ui = (BASE / 'ui/LibraryScreen.kt').read_text()
entry = (BASE / 'ui/FluxReaderApp.kt').read_text()
prefs = (BASE / 'ui/ReaderPreferences.kt').read_text()
repo = (BASE / 'data/LibraryRepository.kt').read_text()

def check(name, ok):
    if not ok:
        raise AssertionError(name)
    print('PASS', name)

check('Folder state belongs to ViewModel', 'val libraryFolder: StateFlow<String>' in app and 'val libraryFolder by viewModel.libraryFolder.collectAsState()' in entry)
check('Folder persists in private preferences', 'getSharedPreferences("library_ui_state"' in app and '.putString("opened_folder_v1", normalized).apply()' in app)
check('No per-composable folder reset', 'var currentFolder by rememberSaveable' not in ui and 'if (settings.libraryGroupMode != LibraryGroupMode.FOLDER) {\n            currentFolder = ""' not in ui)
check('Library gets folder plus change callback', 'currentFolder = libraryFolder' in entry and 'onFolderChanged = viewModel::setLibraryFolder' in entry)
check('Reader/Settings switches cannot alter folder by navigation', 'onSettings = viewModel::showSettings' in entry and 'currentFolder: String,' in ui)
check('Folder rename and move rebase active location', app.count('rebaseLibraryFolder(folderPath, path)') == 2)
check('Folder deletion falls back to parent', 'rebaseLibraryFolder(folderPath, LibraryFolderTree.parent(folderPath))' in app)
check('Audio filters exist', 'BOOKS, PDF, TXT, AUDIO' in ui and 'WastelandLibraryFilter.AUDIO -> "Аудио"' in ui)
check('AUDIO excludes textual books', 'WastelandLibraryFilter.AUDIO -> false' in ui)
check('AUDIO includes only audio entries', '(wastelandFilter == WastelandLibraryFilter.ALL || wastelandFilter == WastelandLibraryFilter.AUDIO)' in ui)
check('Audio-only folders contribute to navigation', 'folders + audiobooks.entries.map { it.folderPath }' in ui)
check('New folder from selection available in two shell menus', ui.count('onCreateFolder = { createSelectedFolderParent') == 2)
check('Selection action requires >0 books', 'enabled = selectedCount > 0,\n                onClick = { expanded = false; onCreateFolder() }' in ui)
check('Selection create failure keeps selection', 'if (created != null) {' in ui and 'selectedBookIds = emptySet()' in ui)
check('Atomic folder-create restores memberships on failure', 'restoreFolderMembership(snapshot)' in app and 'createdPath?.let { created -> runCatching { libraryRepository.deleteFolder(created) } }' in app)
check('Unified theme list, no duplicated experimental palette list', prefs.count('Text("Цветовая схема"') == 1 and 'val palettes = ReaderThemeMode.entries' in prefs)
check('Experimental frame independently selected', 'settings.experimentalFrameTheme == frame' in prefs)
header = ui.split('ExperimentalTopBar(',1)[1].split('// The library owns the only weighted viewport',1)[0]
check('No upper-right search action duplicate', 'wastelandSearchRequested = true' not in header and 'IconButton(onClick = { wastelandSearchRequested' not in header)
check('Visible search field stays available', 'OutlinedTextField(\n                        value = query' in ui and 'placeholder = { Text("Поиск") }' in ui)
print('alpha.92 library critic: 19/19')
