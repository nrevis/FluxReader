package io.fluxreader.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.fluxreader.app.AppViewModel
import io.fluxreader.app.PdfUiState
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.ReaderThemeMode
import io.fluxreader.app.data.useWastelandDesign
import io.fluxreader.app.data.useCyberpunkDesign
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    state: PdfUiState,
    title: String,
    settings: ReaderSettings,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onSpeed: () -> Unit,
) {
    var zoom by rememberSaveable { mutableFloatStateOf(1f) }
    var showSearch by remember { mutableStateOf(false) }
    var chromeVisible by rememberSaveable(state.bookId) { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    val search by viewModel.search.collectAsState()

    DisposableEffect(state.bitmap) {
        val ownedBitmap = state.bitmap
        onDispose {
            if (ownedBitmap != null && viewModel.pdf.value.bitmap !== ownedBitmap && !ownedBitmap.isRecycled) {
                ownedBitmap.recycle()
            }
        }
    }

    LaunchedEffect(chromeVisible, interactionTick, settings.chromeAutoHideSeconds, showSearch) {
        val seconds = settings.chromeAutoHideSeconds
        if (chromeVisible && !showSearch && seconds > 0) {
            delay(seconds * 1_000L)
            chromeVisible = false
        }
    }

    fun previousPage() {
        interactionTick++
        viewModel.pdfPrevious()
    }

    fun nextPage() {
        interactionTick++
        viewModel.pdfNext()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (chromeVisible) {
                if ((settings.useWastelandDesign || settings.useCyberpunkDesign)) {
                    ExperimentalTopBar(
                        title = "PDF ${if (state.pageCount > 0) state.pageIndex + 1 else 0}/${state.pageCount}",
                        subtitle = title,
                        navigation = {
                            IconButton(onClick = onBack) {
                                FluxThemedIcon(FluxIconRole.BACK, Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                interactionTick++
                                viewModel.clearSearch()
                                showSearch = true
                            }, enabled = state.pageCount > 0 && !state.loading) {
                                FluxThemedIcon(FluxIconRole.SEARCH, Icons.Default.Search, contentDescription = "Поиск")
                            }
                            IconButton(onClick = onSpeed, enabled = state.pageCount > 0 && !state.loading) {
                                FluxThemedIcon(FluxIconRole.SPEED, Icons.Default.PlayArrow, contentDescription = "Скорочтение")
                            }
                        },
                    )
                } else {
                    TopAppBar(
                        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                interactionTick++
                                viewModel.clearSearch()
                                showSearch = true
                            }, enabled = state.pageCount > 0 && !state.loading) {
                                Icon(Icons.Default.Search, contentDescription = "Поиск")
                            }
                            IconButton(onClick = onSpeed, enabled = state.pageCount > 0 && !state.loading) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Скорочтение")
                            }
                        },
                    )
                }
            }
        },
        bottomBar = {
            if (chromeVisible) {
                PdfBottomBar(
                    state = state,
                    zoom = zoom,
                    onZoom = {
                        interactionTick++
                        zoom = it
                    },
                    onPrevious = ::previousPage,
                    onNext = ::nextPage,
                    viewModel = viewModel,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when {
            state.error != null -> Box(
                Modifier.fillMaxSize().padding(padding).padding(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(state.error, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    Button(onClick = viewModel::pdfRetry) { Text("Повторить") }
                }
            }
            else -> BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .readerGestures(
                        edgeFraction = settings.edgeTapFraction,
                        edgeTapsEnabled = settings.edgeTapZonesEnabled && zoom <= 1.01f,
                        centerTapEnabled = true,
                        swipeEnabled = settings.swipeNavigationEnabled && zoom <= 1.01f,
                        onPrevious = ::previousPage,
                        onNext = ::nextPage,
                        onCenterTap = {
                            interactionTick++
                            chromeVisible = !chromeVisible
                        },
                    ),
            ) {
                val density = LocalDensity.current
                val viewportWidthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(320)
                val renderWidthPx = (viewportWidthPx * zoom).toInt().coerceIn(320, 2800)
                val renderWidthDp = with(density) { renderWidthPx.toDp() }
                val vertical = rememberScrollState()
                val horizontal = rememberScrollState()

                LaunchedEffect(state.pageIndex) {
                    vertical.scrollTo(0)
                    horizontal.scrollTo(0)
                }
                LaunchedEffect(state.pageIndex, state.pageCount, renderWidthPx) {
                    if (state.pageCount > 0) viewModel.renderPdf(renderWidthPx)
                }

                Box(
                    Modifier.fillMaxSize().horizontalScroll(horizontal).verticalScroll(vertical),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    when {
                        state.bitmap != null -> Image(
                            bitmap = state.bitmap.asImageBitmap(),
                            contentDescription = "Страница PDF ${state.pageIndex + 1}",
                            modifier = Modifier.width(renderWidthDp),
                            contentScale = ContentScale.FillWidth,
                        )
                        state.loading || state.pageCount == 0 -> CircularProgressIndicator(Modifier.padding(48.dp))
                    }
                }
            }
        }
    }

    if (showSearch) {
        BookSearchSheet(
            state = search,
            pdfMode = true,
            onSearch = viewModel::searchBook,
            onHit = { hit ->
                showSearch = false
                viewModel.openSearchHit(hit)
            },
            onDismiss = {
                viewModel.clearSearch()
                showSearch = false
            },
        )
    }
}

@Composable
private fun PdfBottomBar(
    state: PdfUiState,
    zoom: Float,
    onZoom: (Float) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    viewModel: AppViewModel,
) {
    var pageDraft by remember(state.pageIndex, state.pageCount) { mutableFloatStateOf(state.pageIndex.toFloat()) }
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        val progress = if (state.pageCount <= 1) 0f else state.pageIndex.toFloat() / (state.pageCount - 1).toFloat()
        ExperimentalPanel(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 5.dp),
            contentPadding = 6.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {
                ExperimentalProgress(progress, Modifier.fillMaxWidth())
                Row(
                    Modifier.fillMaxWidth().padding(top = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = onPrevious, enabled = state.pageIndex > 0 && !state.loading) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущая страница", tint = LocalFluxThemeMode.current.chromeAccent)
                    }
                    Text(if (state.pageCount > 0) "${state.pageIndex + 1}/${state.pageCount}" else "—", color = LocalFluxThemeMode.current.chromeText)
                    IconButton(onClick = onNext, enabled = state.pageCount > 0 && state.pageIndex < state.pageCount - 1 && !state.loading) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Следующая страница", tint = LocalFluxThemeMode.current.chromeAccent)
                    }
                    IconButton(onClick = { onZoom((zoom - 0.25f).coerceAtLeast(1f)) }, enabled = zoom > 1f) {
                        Icon(Icons.Default.ZoomOut, contentDescription = "Уменьшить PDF", tint = LocalFluxThemeMode.current.chromeText)
                    }
                    Text("${(zoom * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = LocalFluxThemeMode.current.chromeAccent)
                    IconButton(onClick = { onZoom((zoom + 0.25f).coerceAtMost(2.5f)) }, enabled = zoom < 2.5f) {
                        Icon(Icons.Default.ZoomIn, contentDescription = "Увеличить PDF", tint = LocalFluxThemeMode.current.chromeText)
                    }
                }
                if (state.pageCount > 1) {
                    Slider(
                        value = pageDraft.coerceIn(0f, (state.pageCount - 1).toFloat()),
                        onValueChange = { pageDraft = it },
                        onValueChangeFinished = { viewModel.pdfPage(pageDraft.toInt()) },
                        valueRange = 0f..(state.pageCount - 1).toFloat(),
                    )
                }
            }
        }
        return
    }
    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth()) {
            val progress = if (state.pageCount <= 1) 0f else state.pageIndex.toFloat() / (state.pageCount - 1).toFloat()
            LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onPrevious, enabled = state.pageIndex > 0 && !state.loading) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущая страница")
                }
                Text(if (state.pageCount > 0) "${state.pageIndex + 1}/${state.pageCount}" else "—")
                IconButton(onClick = onNext, enabled = state.pageCount > 0 && state.pageIndex < state.pageCount - 1 && !state.loading) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Следующая страница")
                }
                IconButton(onClick = { onZoom((zoom - 0.25f).coerceAtLeast(1f)) }, enabled = zoom > 1f) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Уменьшить PDF")
                }
                Text("${(zoom * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = { onZoom((zoom + 0.25f).coerceAtMost(2.5f)) }, enabled = zoom < 2.5f) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Увеличить PDF")
                }
            }
            if (state.pageCount > 1) {
                Slider(
                    value = pageDraft.coerceIn(0f, (state.pageCount - 1).toFloat()),
                    onValueChange = { pageDraft = it },
                    onValueChangeFinished = { viewModel.pdfPage(pageDraft.toInt()) },
                    valueRange = 0f..(state.pageCount - 1).toFloat(),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}
