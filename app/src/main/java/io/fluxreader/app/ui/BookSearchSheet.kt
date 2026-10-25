package io.fluxreader.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.fluxreader.app.SearchHit
import io.fluxreader.app.SearchUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchSheet(
    state: SearchUiState,
    initialQuery: String = state.query,
    pdfMode: Boolean = false,
    onSearch: (String) -> Unit,
    onHit: (SearchHit) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun dismissSearch() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onDismiss()
    }

    fun submitSearch() {
        if (query.trim().length < 2 || state.loading) return
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        onSearch(query)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    FluxModalBottomSheet(onDismissRequest = ::dismissSearch) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModalSheetHeader(
                title = "Поиск по книге",
                onClose = ::dismissSearch,
            )
            if (pdfMode) {
                Text(
                    "Поиск по текстовому слою PDF.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.weight(1f).focusRequester(focusRequester),
                    singleLine = true,
                    label = { Text("Текст") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { submitSearch() }),
                )
                Button(onClick = { submitSearch() }, enabled = query.trim().length >= 2 && !state.loading) {
                    Text("Найти")
                }
            }
            if (state.loading) {
                val progress = if (state.total > 0) state.scanned.toFloat() / state.total.toFloat() else 0f
                LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                Text(
                    if (state.total > 0) "Проверено ${state.scanned} из ${state.total}" else "Ищу…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (!state.loading && state.query.isNotBlank() && state.error == null && state.hits.isEmpty()) {
                Text("Совпадений не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.hits.isNotEmpty()) {
                Text("Найдено: ${state.hits.size}", style = MaterialTheme.typography.labelLarge)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 430.dp)) {
                    items(state.hits) { hit ->
                        Column(
                            Modifier.fillMaxWidth().clickable { onHit(hit) }.padding(vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                hit.title.ifBlank { if (pdfMode) "Страница ${hit.chapterIndex + 1}" else "Глава ${hit.chapterIndex + 1}" },
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(hit.snippet, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
