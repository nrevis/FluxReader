package io.fluxreader.app.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.fluxreader.app.data.ReaderAnnotation
import io.fluxreader.app.data.ReaderAnnotationKind
import java.text.DateFormat
import java.util.Date

private enum class ReaderAnnotationFilter(val label: String, val kind: ReaderAnnotationKind?) {
    ALL("Все", null),
    HIGHLIGHT("Выделения", ReaderAnnotationKind.HIGHLIGHT),
    QUOTE("Цитаты", ReaderAnnotationKind.QUOTE),
    NOTE("Заметки", ReaderAnnotationKind.NOTE),
    BOOKMARK("Закладки", ReaderAnnotationKind.BOOKMARK),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderAnnotationsSheet(
    annotations: List<ReaderAnnotation>,
    currentBookId: String,
    bookTitle: (String) -> String,
    chapterTitle: (ReaderAnnotation) -> String,
    onOpen: (ReaderAnnotation) -> Unit,
    onDelete: (ReaderAnnotation) -> Unit,
    onEditNote: (ReaderAnnotation) -> Unit,
    onChangeColor: (ReaderAnnotation, Color) -> Unit,
    highlightColors: List<Color>,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var scope by remember { mutableStateOf(ReaderAnnotationScope.CURRENT_BOOK) }
    var filter by remember { mutableStateOf(ReaderAnnotationFilter.ALL) }
    var sortMode by remember { mutableStateOf(ReaderAnnotationSortMode.DATE) }
    var query by remember { mutableStateOf("") }
    var sortMenu by remember { mutableStateOf(false) }
    var exportMenu by remember { mutableStateOf(false) }
    var pendingExportText by remember { mutableStateOf("") }

    val visible = remember(annotations, currentBookId, scope, filter, sortMode, query) {
        ReaderAnnotationTools.filterAndSort(
            annotations = annotations,
            currentBookId = currentBookId,
            scope = scope,
            kind = filter.kind,
            query = query,
            sortMode = sortMode,
            bookTitle = bookTitle,
        )
    }
    val dateFormat = remember { DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT) }
    val markdownLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) writeAnnotationExport(context, uri, pendingExportText)
    }
    val textLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) writeAnnotationExport(context, uri, pendingExportText)
    }

    fun prepareExport(format: ReaderAnnotationExportFormat) {
        pendingExportText = ReaderAnnotationTools.export(
            items = visible,
            format = format,
            bookTitle = bookTitle,
            chapterTitle = chapterTitle,
            dateLabel = { dateFormat.format(Date(it)) },
        )
        val base = if (scope == ReaderAnnotationScope.CURRENT_BOOK) {
            bookTitle(currentBookId).ifBlank { "annotations" }
        } else "flux-reader-annotations"
        val safe = base.replace(Regex("""[\\/:*?"<>|]+"""), "_").take(72).ifBlank { "annotations" }
        if (format == ReaderAnnotationExportFormat.MARKDOWN) markdownLauncher.launch("$safe.md")
        else textLauncher.launch("$safe.txt")
    }

    FluxModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
            ModalSheetHeader(title = "Заметки и выделения", onClose = onDismiss)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = scope == ReaderAnnotationScope.CURRENT_BOOK,
                    onClick = { scope = ReaderAnnotationScope.CURRENT_BOOK },
                    label = { Text("Эта книга") },
                )
                FilterChip(
                    selected = scope == ReaderAnnotationScope.ALL_BOOKS,
                    onClick = { scope = ReaderAnnotationScope.ALL_BOOKS },
                    label = { Text("Все книги") },
                )
                Spacer(Modifier.weight(1f))
                androidx.compose.foundation.layout.Box {
                    IconButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Sort, contentDescription = "Сортировка") }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        listOf(
                            ReaderAnnotationSortMode.DATE to "По дате",
                            ReaderAnnotationSortMode.BOOK to "По книге",
                            ReaderAnnotationSortMode.CHAPTER to "По главе",
                        ).forEach { (mode, label) ->
                            DropdownMenuItem(text = { Text(label) }, onClick = { sortMode = mode; sortMenu = false })
                        }
                    }
                }
                androidx.compose.foundation.layout.Box {
                    IconButton(onClick = { exportMenu = true }, enabled = visible.isNotEmpty()) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Экспорт")
                    }
                    DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                        DropdownMenuItem(text = { Text("Markdown (.md)") }, onClick = {
                            exportMenu = false; prepareExport(ReaderAnnotationExportFormat.MARKDOWN)
                        })
                        DropdownMenuItem(text = { Text("Текст (.txt)") }, onClick = {
                            exportMenu = false; prepareExport(ReaderAnnotationExportFormat.TEXT)
                        })
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Поиск по тексту, заметкам и книгам") },
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ReaderAnnotationFilter.entries.forEach { item ->
                    val scoped = if (scope == ReaderAnnotationScope.CURRENT_BOOK) annotations.filter { it.bookId == currentBookId } else annotations
                    val count = item.kind?.let { kind -> scoped.count { it.kind == kind } } ?: scoped.size
                    FilterChip(
                        selected = filter == item,
                        onClick = { filter = item },
                        label = { Text("${item.label} · $count") },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            if (visible.isEmpty()) {
                Text(
                    if (query.isBlank()) "В этом разделе пока нет записей." else "Ничего не найдено.",
                    modifier = Modifier.padding(vertical = 28.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(visible, key = { it.id }) { item ->
                        ReaderAnnotationRow(
                            item = item,
                            showBook = scope == ReaderAnnotationScope.ALL_BOOKS,
                            bookTitle = bookTitle(item.bookId),
                            chapterTitle = chapterTitle(item),
                            highlightColors = highlightColors,
                            onOpen = { onOpen(item) },
                            onDelete = { onDelete(item) },
                            onEditNote = { onEditNote(item) },
                            onChangeColor = { color -> onChangeColor(item, color) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    }
                    item { Spacer(Modifier.height(22.dp)) }
                }
            }
        }
    }
}

private fun writeAnnotationExport(context: Context, uri: android.net.Uri, text: String) {
    runCatching {
        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(text) }
    }
}

@Composable
private fun ReaderAnnotationRow(
    item: ReaderAnnotation,
    showBook: Boolean,
    bookTitle: String,
    chapterTitle: String,
    highlightColors: List<Color>,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onEditNote: () -> Unit,
    onChangeColor: (Color) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            when (item.kind) {
                ReaderAnnotationKind.HIGHLIGHT -> Icons.Default.Highlight
                ReaderAnnotationKind.QUOTE -> Icons.Default.FormatQuote
                ReaderAnnotationKind.NOTE -> Icons.AutoMirrored.Filled.Notes
                ReaderAnnotationKind.BOOKMARK -> Icons.Default.Bookmark
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                buildString {
                    append(ReaderAnnotationTools.kindLabel(item.kind))
                    if (showBook && bookTitle.isNotBlank()) append(" · ").append(bookTitle)
                    if (chapterTitle.isNotBlank()) append(" · ").append(chapterTitle)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(item.excerpt.ifBlank { "Без текста" }, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
            if (item.note.isNotBlank()) {
                Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (item.kind == ReaderAnnotationKind.HIGHLIGHT) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Цвет", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    highlightColors.forEach { color ->
                        Surface(
                            modifier = Modifier.size(24.dp).clickable { onChangeColor(color) },
                            shape = CircleShape,
                            color = color,
                            border = if (item.colorArgb == color.toArgb()) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface)
                            else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {}
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onOpen) { Text("Перейти") }
                AssistChip(
                    onClick = onEditNote,
                    label = { Text(if (item.note.isBlank()) "Заметка" else "Изменить") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Удалить") }
            }
        }
    }
}

@Composable
fun ReaderNoteDialog(initialText: String = "", onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var value by remember(initialText) { mutableStateOf(initialText) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialText.isBlank()) "Новая заметка" else "Изменить заметку") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 10,
                label = { Text("Текст заметки") },
            )
        },
        confirmButton = { TextButton(onClick = { onSave(value) }, enabled = value.isNotBlank()) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
    )
}
