package io.fluxreader.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import io.fluxreader.app.AppViewModel
import io.fluxreader.app.ReaderUiState
import io.fluxreader.app.data.PageTurnAnimation
import io.fluxreader.app.data.ReaderAnnotation
import io.fluxreader.app.data.ReaderAnnotationKind
import io.fluxreader.app.data.ReaderBlock
import io.fluxreader.app.data.ReaderNavigationMode
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.ReaderThemeMode
import io.fluxreader.app.data.useWastelandDesign
import io.fluxreader.app.data.useCyberpunkDesign
import io.fluxreader.app.data.TextRole
import io.fluxreader.app.reader.ChapterSpeedOffset
import io.fluxreader.app.reader.ReaderPageAnchor
import io.fluxreader.app.reader.ReaderPageItem
import io.fluxreader.app.reader.ReaderPageMath
import io.fluxreader.app.reader.ReaderNarrator
import io.fluxreader.app.reader.SpeedReaderCursor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class ReaderOverlay { NONE, TOC, SEARCH, SETTINGS, ANNOTATIONS, NARRATION, EFFECTS }
private enum class ReaderSettingsPanel { MAIN, ADVANCED }

private data class ReaderTextSelection(
    val startBlockIndex: Int,
    val startCharOffset: Int,
    val endBlockIndex: Int,
    val endCharOffsetExclusive: Int,
    val text: String,
    val word: String,
    val sentence: String,
    val paragraph: String,
    val menuPositionInWindow: Offset,
    val menuVisible: Boolean = true,
)

private data class ReaderAnnotationPopupTarget(
    val annotation: ReaderAnnotation,
    val positionInWindow: Offset,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    state: ReaderUiState,
    settings: ReaderSettings,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onSpeed: () -> Unit,
    workspaceEmbedded: Boolean = false,
    listening: Boolean = false,
    onImmersiveChanged: (Boolean) -> Unit = {},
) {
    val chapter = state.chapter
    val outline = state.outline
    var overlay by remember { mutableStateOf(ReaderOverlay.NONE) }
    val narratorLaunchRequest by viewModel.narratorLaunchRequest.collectAsState()
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var chromeVisible by rememberSaveable(state.bookId) { mutableStateOf(true) }
    var selectedText by remember(state.bookId, state.chapterIndex) { mutableStateOf<ReaderTextSelection?>(null) }
    val allAnnotations by viewModel.annotations.collectAsState()
    val libraryBooks by viewModel.books.collectAsState()
    val bookAnnotations = remember(state.bookId, allAnnotations) {
        allAnnotations.filter { it.bookId == state.bookId }
    }
    var pendingNoteSelection by remember(state.bookId, state.chapterIndex) { mutableStateOf<ReaderTextSelection?>(null) }
    var editingAnnotation by remember(state.bookId) { mutableStateOf<ReaderAnnotation?>(null) }
    var editingAnnotationBounds by remember(state.bookId, state.chapterIndex) { mutableStateOf<ReaderAnnotation?>(null) }
    var annotationPopupTarget by remember(state.bookId, state.chapterIndex) { mutableStateOf<ReaderAnnotationPopupTarget?>(null) }
    var pendingDeleteAnnotation by remember(state.bookId) { mutableStateOf<ReaderAnnotation?>(null) }
    var selectionEdgeScrollJob by remember(state.bookId, state.chapterIndex) { mutableStateOf<Job?>(null) }
    var pendingViewportAnchor by remember(state.bookId, state.chapterIndex) { mutableStateOf<ReaderPageLocation?>(null) }
    var viewportAnchorLocked by remember(state.bookId, state.chapterIndex) { mutableStateOf(false) }
    var stableViewportAnchor by remember(state.bookId, state.chapterIndex) {
        mutableStateOf(
            ReaderPageLocation(
                chapterIndex = state.chapterIndex,
                blockIndex = state.initialBlockIndex.coerceAtLeast(0),
                blockOffsetPx = state.initialBlockOffsetPx.coerceAtLeast(0),
                blockCharOffset = state.initialBlockCharOffset.coerceAtLeast(0),
            )
        )
    }
    val context = LocalContext.current
    val narrator = remember(context, state.bookId) { ReaderNarrator(context.applicationContext) }
    val narrationStatus by narrator.status.collectAsState()
    DisposableEffect(narrator) { onDispose { narrator.close() } }
    LaunchedEffect(narrator, settings.narrationVoiceId, settings.narrationRate, settings.narrationPitch, settings.narrationAutoNextChapter) {
        narrator.setPreferences(settings)
    }
    LaunchedEffect(narrator, state.bookId, state.chapterIndex, state.chapter) {
        narrator.setDocument(state.bookId, state.chapterIndex, state.chapter, state.initialBlockIndex, state.initialBlockCharOffset)
    }
    LaunchedEffect(listening, state.bookId, state.chapterIndex, state.chapter) {
        if (listening && state.bookId != null && state.chapter != null) narrator.play()
        else if (!listening) narrator.pause()
    }
    // Single-tap narration entry from Speed Reader. Restore the precise speed anchor before
    // opening the speech controls and start Android TTS only after the chapter is available.
    LaunchedEffect(narratorLaunchRequest, state.bookId, state.chapterIndex, state.chapter) {
        if (narratorLaunchRequest && state.bookId != null && state.chapter != null) {
            narrator.setDocument(state.bookId, state.chapterIndex, state.chapter,
                state.initialBlockIndex, state.initialBlockCharOffset)
            overlay = ReaderOverlay.NARRATION
            viewModel.consumeNarratorLaunchRequest()
            narrator.play()
        }
    }
    narrator.onSegmentPosition = { blockIndex, charOffset ->
        viewModel.saveReaderPosition(state.bookId, state.chapterIndex, blockIndex, 0, charOffset)
    }
    narrator.onChapterComplete = {
        val next = state.chapterIndex + 1
        if (next <= (state.outline?.chapters?.lastIndex ?: -1)) {
            viewModel.readerChapterAt(next, 0, 0)
        } else {
            narrator.stop()
        }
    }
    val hostView = LocalView.current
    val readerHighlightColors = listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.30f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.30f),
        MaterialTheme.colorScheme.error.copy(alpha = 0.22f),
    )
    var interactionTick by remember { mutableIntStateOf(0) }
    val search by viewModel.search.collectAsState()
    val readerDensity = LocalDensity.current
    val pageLineSnapPx = with(readerDensity) {
        (settings.fontSizeSp * settings.lineHeightMultiplier).sp.roundToPx().coerceAtLeast(1)
    }
    val measuredLineTops = remember(state.bookId, state.chapterIndex) { mutableStateMapOf<Int, IntArray>() }
    val measuredLineStarts = remember(state.bookId, state.chapterIndex) { mutableStateMapOf<Int, IntArray>() }
    var restoredScrollCharacter by remember(state.bookId, state.chapterIndex, state.initialBlockIndex, state.initialBlockCharOffset) { mutableStateOf(false) }
    val needsScrollRestore = settings.navigationMode == ReaderNavigationMode.SCROLL &&
        state.initialBlockCharOffset > 0 && state.initialBlockOffsetPx == 0 && !restoredScrollCharacter
    val chapterForNavigation = state.chapter
    val safeInitialBlock = chapterForNavigation?.let {
        state.initialBlockIndex.coerceIn(0, it.blocks.lastIndex.coerceAtLeast(0))
    } ?: 0
    val safeInitialOffset = if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) {
        val raw = state.initialBlockOffsetPx.coerceAtLeast(0)
        ReaderPageMath.snapOffset(raw, pageLineSnapPx)
    } else {
        state.initialBlockOffsetPx.coerceAtLeast(0)
    }
    val listState = chapterForNavigation?.let { chapter ->
        remember(
            state.chapterIndex,
            chapter.blocks.size,
            state.initialBlockIndex,
            safeInitialOffset,
            pageLineSnapPx,
        ) {
            LazyListState(safeInitialBlock, safeInitialOffset)
        }
    }
    val pageHistory = remember(state.bookId) { mutableStateListOf<ReaderPageLocation>() }
    var preparedPagination by remember(state.bookId, state.chapterIndex) { mutableStateOf<PreparedReaderPagination?>(null) }
    var preparedPageIndex by remember(state.bookId, state.chapterIndex) { mutableIntStateOf(0) }
    var pendingProgressSeek by remember(state.bookId) { mutableStateOf<ReaderProgressSeek.Target?>(null) }
    val preparedPaginationCache = remember(state.bookId) { PreparedReaderPaginationCache(maxEntries = 8) }
    val linkHistory = remember(state.bookId) { mutableStateListOf<ReaderPageLocation>() }
    val pageScope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    var pageNavigationJob by remember(state.bookId) { mutableStateOf<Job?>(null) }
    var pageAnimationTick by remember(state.bookId) { mutableIntStateOf(0) }
    var pageAnimationDirection by remember(state.bookId) { mutableIntStateOf(1) }
    var pendingChapterAnimationDirection by remember(state.bookId) { mutableIntStateOf(0) }
    val pageTransition = remember(state.bookId) { Animatable(1f) }
    val fullscreenTransition = remember(state.bookId) { Animatable(1f) }
    var fullscreenTransitionInitialized by remember(state.bookId) { mutableStateOf(false) }
    val pageTranslationPx = with(readerDensity) { 54.dp.toPx() }
    val pageCurlShadowPx = with(readerDensity) { 18.dp.toPx() }
    val pageCameraDistance = 32f * readerDensity.density
    val fullscreenShiftPx = with(readerDensity) { 14.dp.toPx() }

    // Manual fullscreen works regardless of the auto-hide preference. It never starts fullscreen.
    FullscreenSystemBarsEffect(!chromeVisible)
    // The frame must be remeasured when bars are hidden; otherwise its safeDrawing
    // modifier keeps the entire Pixel status-bar band empty in fullscreen.
    LaunchedEffect(chromeVisible) { onImmersiveChanged(!chromeVisible) }
    DisposableEffect(onImmersiveChanged) {
        onDispose { onImmersiveChanged(false) }
    }

    LaunchedEffect(pageAnimationTick, settings.pageTurnAnimation, settings.pageTurnDurationMs) {
        if (pageAnimationTick <= 0 || settings.pageTurnAnimation == PageTurnAnimation.NONE) {
            pageTransition.snapTo(1f)
        } else {
            pageTransition.snapTo(0f)
            pageTransition.animateTo(1f, tween(settings.pageTurnDurationMs))
        }
    }

    LaunchedEffect(chromeVisible, settings.fullscreenTransitionEnabled, settings.fullscreenTransitionDurationMs) {
        if (!fullscreenTransitionInitialized) {
            fullscreenTransition.snapTo(1f)
            fullscreenTransitionInitialized = true
        } else if (!settings.fullscreenTransitionEnabled) {
            fullscreenTransition.snapTo(1f)
        } else {
            fullscreenTransition.snapTo(0f)
            fullscreenTransition.animateTo(1f, tween(settings.fullscreenTransitionDurationMs))
        }
    }

    val fullscreenTransitionModifier = Modifier.graphicsLayer {
        if (!settings.fullscreenTransitionEnabled) {
            alpha = 1f
            scaleX = 1f
            scaleY = 1f
            translationY = 0f
        } else {
            val p = fullscreenTransition.value.coerceIn(0f, 1f)
            alpha = 0.82f + p * 0.18f
            val startScale = if (!chromeVisible) 0.978f else 1.022f
            val scale = startScale + (1f - startScale) * p
            scaleX = scale
            scaleY = scale
            translationY = (if (!chromeVisible) 1f else -1f) * (1f - p) * fullscreenShiftPx
        }
    }

    val pageAnimationModifier = Modifier.graphicsLayer {
        val p = pageTransition.value.coerceIn(0f, 1f)
        alpha = 1f
        translationX = 0f
        scaleX = 1f
        scaleY = 1f
        rotationY = 0f
        rotationZ = 0f
        shadowElevation = 0f
        cameraDistance = pageCameraDistance
        transformOrigin = TransformOrigin.Center
        when (settings.pageTurnAnimation) {
            PageTurnAnimation.NONE -> Unit
            PageTurnAnimation.CURL -> {
                if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) {
                    // A lightweight paper-turn illusion: the incoming page is hinged to the
                    // outer edge, starts in perspective with a soft page shadow, then settles
                    // flat. It avoids bitmap snapshots, so text remains crisp and accessible.
                    transformOrigin = if (pageAnimationDirection > 0) {
                        TransformOrigin(1f, 0.5f)
                    } else {
                        TransformOrigin(0f, 0.5f)
                    }
                    rotationY = -pageAnimationDirection * (1f - p) * 24f
                    rotationZ = pageAnimationDirection * (1f - p) * 0.45f
                    translationX = pageAnimationDirection * (1f - p) * pageTranslationPx * 0.34f
                    scaleX = 0.982f + p * 0.018f
                    alpha = 0.78f + p * 0.22f
                    shadowElevation = (1f - p) * pageCurlShadowPx
                } else {
                    // In continuous scrolling a literal page hinge is visually misleading;
                    // keep the same preset as a restrained paper-like lateral settle.
                    translationX = pageAnimationDirection * (1f - p) * pageTranslationPx * 0.45f
                    alpha = 0.80f + p * 0.20f
                }
            }
            PageTurnAnimation.SLIDE -> {
                translationX = pageAnimationDirection * (1f - p) * pageTranslationPx
                alpha = 0.72f + p * 0.28f
            }
            PageTurnAnimation.FADE -> alpha = 0.30f + p * 0.70f
            PageTurnAnimation.ZOOM -> {
                alpha = 0.48f + p * 0.52f
                scaleX = 0.965f + p * 0.035f
                scaleY = 0.965f + p * 0.035f
            }
        }
    }

    LaunchedEffect(
        settings.navigationMode,
        settings.fontSizeSp,
        settings.lineHeightMultiplier,
        settings.horizontalPaddingDp,
        settings.topPaddingDp,
        settings.bottomPaddingDp,
        settings.paragraphSpacingDp,
        settings.firstLineIndentDp,
        settings.fontMode,
        settings.customFontPath,
        settings.textAlignment,
        settings.hyphenationEnabled,
        settings.chapterTitleScale,
        settings.chapterHeadingScale,
        settings.centerChapterHeadings,
    ) {
        pageNavigationJob?.cancel()
        pageHistory.clear()
    }

    fun currentPreparedLocation(): ReaderPageLocation? {
        val pagination = preparedPagination ?: return null
        if (pagination.chapterIndex != state.chapterIndex) return null
        val page = pagination.pages.getOrNull(preparedPageIndex) ?: return null
        return ReaderPageLocation(
            chapterIndex = state.chapterIndex,
            blockIndex = page.startBlockIndex,
            blockOffsetPx = 0,
            blockCharOffset = page.startCharOffset,
        )
    }

    fun currentReaderLocation(): ReaderPageLocation? = if (needsScrollRestore || viewportAnchorLocked) {
        stableViewportAnchor
    } else if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) {
        currentPreparedLocation()
    } else {
        listState?.let { currentList ->
            ReaderPageLocation(
                chapterIndex = state.chapterIndex,
                blockIndex = currentList.firstVisibleItemIndex,
                blockOffsetPx = currentList.firstVisibleItemScrollOffset,
                blockCharOffset = ReaderViewportAnchor.scrollCharOffset(
                    measuredLineTops[currentList.firstVisibleItemIndex], measuredLineStarts[currentList.firstVisibleItemIndex],
                    currentList.firstVisibleItemScrollOffset,
                ),
            )
        }
    }

    fun commitCurrentReaderLocation() {
        val location = currentReaderLocation() ?: stableViewportAnchor
        viewModel.saveReaderPosition(state.bookId, state.chapterIndex, location.blockIndex, location.blockOffsetPx, location.blockCharOffset)
        viewModel.flushReadingPosition()
    }
    val leaveReader: () -> Unit = { commitCurrentReaderLocation(); onBack() }
    val openSpeedFromReader: () -> Unit = { commitCurrentReaderLocation(); onSpeed() }
    BackHandler(enabled = chromeVisible && selectedText == null && overlay == ReaderOverlay.NONE) { leaveReader() }

    LaunchedEffect(needsScrollRestore, measuredLineStarts[state.initialBlockIndex], listState) {
        if (needsScrollRestore && listState != null) {
            val starts = measuredLineStarts[state.initialBlockIndex]
            val tops = measuredLineTops[state.initialBlockIndex]
            if (starts != null && tops != null) {
                val line = starts.indexOfLast { it <= state.initialBlockCharOffset }.coerceAtLeast(0)
                listState.scrollToItem(state.initialBlockIndex, tops.getOrElse(line) { 0 })
                restoredScrollCharacter = true
            }
        }
    }

    LaunchedEffect(listState) {
        listState?.interactionSource?.interactions?.collect { interaction ->
            if (interaction is DragInteraction.Start) viewportAnchorLocked = false
        }
    }

    fun setChromeVisiblePreservingPosition(visible: Boolean) {
        // Manual fullscreen must remain available even when auto-hide is disabled.
        // readerFullscreenEnabled controls only automatic hiding of chrome.
        if (chromeVisible == visible) return
        val anchor = if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) {
            if (viewportAnchorLocked) {
                stableViewportAnchor
            } else {
                currentPreparedLocation()
                    ?.takeIf { preparedPagination?.isComplete == true }
                    ?: stableViewportAnchor
            }
        } else {
            currentReaderLocation() ?: stableViewportAnchor
        }
        pendingViewportAnchor = anchor
        stableViewportAnchor = anchor
        viewportAnchorLocked = settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING
        viewModel.saveReaderPosition(state.bookId, state.chapterIndex,
            blockIndex = anchor.blockIndex,
            blockOffsetPx = anchor.blockOffsetPx,
            blockCharOffset = anchor.blockCharOffset,
        )
        selectedText = null
        chromeVisible = visible
        interactionTick++
    }

    BackHandler(enabled = selectedText != null) {
        selectedText = null
        editingAnnotationBounds = null
    }
    BackHandler(enabled = !chromeVisible && selectedText == null) {
        setChromeVisiblePreservingPosition(true)
    }

    LaunchedEffect(state.chapterIndex) {
        if (pendingChapterAnimationDirection != 0) {
            pageAnimationDirection = pendingChapterAnimationDirection
            pendingChapterAnimationDirection = 0
            pageAnimationTick++
        }
    }

    LaunchedEffect(
        state.chapterIndex,
        state.initialBlockIndex,
        state.initialBlockCharOffset,
        state.initialBlockOffsetPx,
        settings.navigationMode,
    ) {
        if (pendingViewportAnchor == null) {
            // Merely displaying a page containing the spoken word must not round the shared
            // cursor backwards to that page's first word. Release only on user navigation.
            viewportAnchorLocked = state.initialBlockCharOffset > 0
            stableViewportAnchor = ReaderPageLocation(
                chapterIndex = state.chapterIndex,
                blockIndex = state.initialBlockIndex.coerceAtLeast(0),
                blockOffsetPx = state.initialBlockOffsetPx.coerceAtLeast(0),
                blockCharOffset = state.initialBlockCharOffset.coerceAtLeast(0),
            )
        }
        val pagination = preparedPagination
        if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING && pagination != null) {
            preparedPageIndex = findPreparedPageIndex(
                pagination = pagination,
                blockIndex = state.initialBlockIndex,
                charOffset = state.initialBlockCharOffset,
                legacyPixelOffsetPx = state.initialBlockOffsetPx,
            )
        }
    }

    LaunchedEffect(preparedPagination?.layoutKey, preparedPagination?.isComplete, preparedPageIndex, settings.navigationMode, pendingViewportAnchor) {
        if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING && pendingViewportAnchor == null && !viewportAnchorLocked) {
            val pagination = preparedPagination
            if (pagination?.isComplete == true) {
                currentPreparedLocation()?.let { location ->
                    stableViewportAnchor = location
                    viewModel.saveReaderPosition(state.bookId, state.chapterIndex,
                        blockIndex = location.blockIndex,
                        blockOffsetPx = 0,
                        blockCharOffset = location.blockCharOffset,
                    )
                }
            }
        }
    }

    fun previousPage() {
        selectedText = null
        viewportAnchorLocked = false
        interactionTick++
        pageNavigationJob?.cancel()

        if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) {
            val pagination = preparedPagination ?: return
            if (pagination.chapterIndex != state.chapterIndex) return
            if (preparedPageIndex > 0) {
                preparedPageIndex--
                pageAnimationDirection = -1
                pageAnimationTick++
            } else if (pagination.isComplete && state.chapterIndex > 0) {
                // Never cross a chapter boundary from the one-page fast preview. Exact pagination
                // will replace it shortly and restore normal previous-page navigation.
                // The block sentinel is clamped by AppViewModel; the character sentinel survives
                // that clamp and selects the final prepared page even when the last block spans pages.
                pendingChapterAnimationDirection = -1
                viewModel.readerChapterAt(state.chapterIndex - 1, Int.MAX_VALUE, 0, Int.MAX_VALUE)
            }
            return
        }

        val currentList = listState ?: return
        pageNavigationJob = pageScope.launch {
            navigateReaderPage(
                direction = -1,
                state = state,
                settings = settings,
                listState = currentList,
                history = pageHistory,
                viewModel = viewModel,
                pageSnapPx = pageLineSnapPx,
                measuredLineTops = measuredLineTops,
            )
            pageAnimationDirection = -1
            pageAnimationTick++
        }
    }

    fun nextPage() {
        selectedText = null
        viewportAnchorLocked = false
        interactionTick++
        pageNavigationJob?.cancel()

        if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) {
            val pagination = preparedPagination ?: return
            if (pagination.chapterIndex != state.chapterIndex) return
            if (preparedPageIndex < pagination.pages.lastIndex) {
                preparedPageIndex++
                pageAnimationDirection = 1
                pageAnimationTick++
            } else if (pagination.isComplete) {
                val lastChapter = state.outline?.chapters?.lastIndex ?: state.chapterIndex
                if (state.chapterIndex < lastChapter) {
                    pendingChapterAnimationDirection = 1
                    viewModel.readerChapterAt(state.chapterIndex + 1, 0, 0, 0)
                }
            }
            return
        }

        val currentList = listState ?: return
        pageNavigationJob = pageScope.launch {
            navigateReaderPage(
                direction = 1,
                state = state,
                settings = settings,
                listState = currentList,
                history = pageHistory,
                viewModel = viewModel,
                pageSnapPx = pageLineSnapPx,
                measuredLineTops = measuredLineTops,
            )
            pageAnimationDirection = 1
            pageAnimationTick++
        }
    }

    fun openReaderLink(target: String, external: Boolean) {
        selectedText = null
        interactionTick++
        if (external) {
            runCatching { uriHandler.openUri(target) }
            return
        }
        val current = if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) {
            currentPreparedLocation()
        } else {
            listState?.let { currentList ->
                ReaderPageLocation(
                    chapterIndex = state.chapterIndex,
                    blockIndex = currentList.firstVisibleItemIndex,
                    blockOffsetPx = currentList.firstVisibleItemScrollOffset,
                )
            }
        } ?: return
        if (linkHistory.lastOrNull() != current) linkHistory.add(current)
        while (linkHistory.size > 128) linkHistory.removeAt(0)
        pageNavigationJob?.cancel()
        pageHistory.clear()
        viewModel.openReaderLink(target)
    }

    fun returnFromReaderLink() {
        if (linkHistory.isEmpty()) return
        interactionTick++
        val target = linkHistory.removeAt(linkHistory.lastIndex)
        pageNavigationJob?.cancel()
        pageHistory.clear()

        if (target.chapterIndex == state.chapterIndex &&
            settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING
        ) {
            val pagination = preparedPagination
            if (pagination != null) {
                preparedPageIndex = findPreparedPageIndex(
                    pagination = pagination,
                    blockIndex = target.blockIndex,
                    charOffset = target.blockCharOffset,
                    legacyPixelOffsetPx = target.blockOffsetPx,
                )
                return
            }
        }

        val currentList = listState
        if (target.chapterIndex == state.chapterIndex &&
            settings.navigationMode == ReaderNavigationMode.SCROLL &&
            currentList != null
        ) {
            pageNavigationJob = pageScope.launch { moveToPageAnchor(currentList, target, smooth = false) }
        } else {
            viewModel.readerChapterAt(
                target.chapterIndex,
                target.blockIndex,
                target.blockOffsetPx,
                target.blockCharOffset,
            )
        }
    }

    fun updateReaderTextSelection(
        anchorBlockIndex: Int,
        anchorSourceOffset: Int,
        currentBlockIndex: Int,
        currentSourceOffset: Int,
        menuPositionInWindow: Offset,
        finished: Boolean,
    ) {
        val currentChapter = state.chapter ?: return
        val blockTexts = currentChapter.blocks.map { (it as? ReaderBlock.Text)?.text }
        val range = ReaderTextActionPolicy.selectDocumentRange(
            blockTexts = blockTexts,
            anchorBlockIndex = anchorBlockIndex,
            anchorOffset = anchorSourceOffset,
            currentBlockIndex = currentBlockIndex,
            currentOffset = currentSourceOffset,
        ) ?: return
        val startBlock = currentChapter.blocks.getOrNull(range.startBlockIndex) as? ReaderBlock.Text ?: return
        val wordInfo = ReaderTextActionPolicy.select(startBlock.text, range.startCharOffset)
        selectedText = ReaderTextSelection(
            startBlockIndex = range.startBlockIndex,
            startCharOffset = range.startCharOffset,
            endBlockIndex = range.endBlockIndex,
            endCharOffsetExclusive = range.endCharOffsetExclusive,
            text = range.text,
            word = wordInfo?.word.orEmpty(),
            sentence = wordInfo?.sentence.orEmpty(),
            paragraph = startBlock.text.trim(),
            menuPositionInWindow = menuPositionInWindow,
            menuVisible = finished,
        )
    }

    fun updateSelectionHandle(
        isStart: Boolean,
        blockIndex: Int,
        sourceOffset: Int,
        menuPositionInWindow: Offset,
        finished: Boolean,
    ) {
        val selection = selectedText ?: return
        if (isStart) {
            updateReaderTextSelection(
                anchorBlockIndex = selection.endBlockIndex,
                anchorSourceOffset = (selection.endCharOffsetExclusive - 1).coerceAtLeast(0),
                currentBlockIndex = blockIndex,
                currentSourceOffset = sourceOffset,
                menuPositionInWindow = menuPositionInWindow,
                finished = finished,
            )
        } else {
            updateReaderTextSelection(
                anchorBlockIndex = selection.startBlockIndex,
                anchorSourceOffset = selection.startCharOffset,
                currentBlockIndex = blockIndex,
                currentSourceOffset = sourceOffset,
                menuPositionInWindow = menuPositionInWindow,
                finished = finished,
            )
        }
    }

    fun handleSelectionEdgeDrag(isStartHandle: Boolean?, direction: Int, positionInWindow: Offset) {
        if (direction == 0) return
        val edgeMarginPx = with(readerDensity) { 72.dp.toPx() }
        val edgeDirection = ReaderSelectionEdgePolicy.direction(
            windowY = positionInWindow.y,
            viewportHeight = hostView.height.toFloat(),
            edgeMarginPx = edgeMarginPx,
        )
        if (edgeDirection != direction || selectionEdgeScrollJob?.isActive == true) return

        selectionEdgeScrollJob = pageScope.launch {
            if (settings.navigationMode == ReaderNavigationMode.SCROLL) {
                val currentList = listState ?: return@launch
                val stepPx = with(readerDensity) { 68.dp.toPx() }
                currentList.scrollBy(direction * stepPx)
                delay(36)
                return@launch
            }

            val pagination = preparedPagination ?: return@launch
            if (pagination.chapterIndex != state.chapterIndex || pagination.pages.isEmpty()) return@launch
            val targetPageIndex = ReaderSelectionEdgePolicy.adjacentPageIndex(
                current = preparedPageIndex,
                lastIndex = pagination.pages.lastIndex,
                direction = direction,
            ) ?: return@launch
            val slices = pagination.pages[targetPageIndex].items.filterIsInstance<PreparedReaderPageItem.TextSlice>()
            // Crossing a page boundary should add only the nearest text on the adjacent page,
            // not swallow the entire page at once. The visible handles can then refine it.
            val target = if (direction < 0) slices.lastOrNull() else slices.firstOrNull()
            if (target != null && selectedText != null) {
                val targetSourceOffset = if (direction < 0) {
                    (target.endCharOffset - 1).coerceAtLeast(target.startCharOffset)
                } else {
                    target.startCharOffset
                }
                val adjustStart = isStartHandle ?: (direction < 0)
                updateSelectionHandle(
                    isStart = adjustStart,
                    blockIndex = target.blockIndex,
                    sourceOffset = targetSourceOffset,
                    menuPositionInWindow = positionInWindow,
                    finished = true,
                )
            }
            preparedPageIndex = targetPageIndex
            pageAnimationDirection = direction
            pageAnimationTick++
            delay(90)
        }
    }

    fun openAnnotationPopup(annotation: ReaderAnnotation, positionInWindow: Offset) {
        selectedText = null
        annotationPopupTarget = ReaderAnnotationPopupTarget(annotation, positionInWindow)
    }

    fun beginEditAnnotationBounds(annotation: ReaderAnnotation, positionInWindow: Offset) {
        val currentChapter = state.chapter ?: return
        if (annotation.bookId != state.bookId || annotation.chapterIndex != state.chapterIndex) return
        val blockTexts = currentChapter.blocks.map { (it as? ReaderBlock.Text)?.text }
        val range = ReaderTextActionPolicy.selectDocumentRange(
            blockTexts = blockTexts,
            anchorBlockIndex = annotation.startBlockIndex,
            anchorOffset = annotation.startCharOffset,
            currentBlockIndex = annotation.endBlockIndex,
            currentOffset = (annotation.endCharOffsetExclusive - 1).coerceAtLeast(0),
        ) ?: return
        val startBlock = currentChapter.blocks.getOrNull(range.startBlockIndex) as? ReaderBlock.Text ?: return
        val wordInfo = ReaderTextActionPolicy.select(startBlock.text, range.startCharOffset)
        editingAnnotationBounds = annotation
        annotationPopupTarget = null
        selectedText = ReaderTextSelection(
            startBlockIndex = range.startBlockIndex,
            startCharOffset = range.startCharOffset,
            endBlockIndex = range.endBlockIndex,
            endCharOffsetExclusive = range.endCharOffsetExclusive,
            text = range.text,
            word = wordInfo?.word.orEmpty(),
            sentence = wordInfo?.sentence.orEmpty(),
            paragraph = startBlock.text.trim(),
            menuPositionInWindow = positionInWindow,
            menuVisible = true,
        )
    }

    fun saveEditedAnnotationBounds(selection: ReaderTextSelection) {
        val item = editingAnnotationBounds ?: return
        viewModel.updateReaderAnnotationRange(
            id = item.id,
            startBlockIndex = selection.startBlockIndex,
            startCharOffset = selection.startCharOffset,
            endBlockIndex = selection.endBlockIndex,
            endCharOffsetExclusive = selection.endCharOffsetExclusive,
            excerpt = selection.text,
        )
        editingAnnotationBounds = null
        selectedText = null
    }

    fun copyAnnotationText(annotation: ReaderAnnotation) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText("Flux Reader", annotation.excerpt))
        annotationPopupTarget = null
    }

    fun copyReaderText(label: String, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        selectedText = null
    }

    fun searchSelectedWord(selection: ReaderTextSelection) {
        selectedText = null
        viewModel.clearSearch()
        viewModel.searchBook(selection.text.ifBlank { selection.word })
        overlay = ReaderOverlay.SEARCH
    }

    fun addSelectionAnnotation(
        selection: ReaderTextSelection,
        kind: ReaderAnnotationKind,
        colorArgb: Int,
        note: String = "",
    ) {
        val bookId = state.bookId ?: return
        viewModel.addReaderAnnotationRange(
            bookId = bookId,
            chapterIndex = state.chapterIndex,
            startBlockIndex = selection.startBlockIndex,
            startCharOffset = selection.startCharOffset,
            endBlockIndex = selection.endBlockIndex,
            endCharOffsetExclusive = selection.endCharOffsetExclusive,
            excerpt = selection.text,
            kind = kind,
            colorArgb = colorArgb,
            note = note,
        )
        selectedText = null
    }

    fun startSpeedFromWord(blockIndex: Int, sourceCharOffset: Int) {
        val currentChapter = state.chapter ?: return
        val bookId = state.bookId ?: return
        val block = currentChapter.blocks.getOrNull(blockIndex) as? ReaderBlock.Text ?: return
        val wordStart = SpeedReaderCursor.wordStartOffset(block.text, sourceCharOffset)
        val chapterOffset = ChapterSpeedOffset.forBlock(currentChapter, blockIndex, wordStart)
        selectedText = null
        viewModel.openSpeedAt(bookId, state.chapterIndex, chapterOffset)
    }

    fun acceptPreparedPagination(next: PreparedReaderPagination) {
        if (next.chapterIndex != state.chapterIndex) return
        // System bars and the experimental shell settle in separate measurements. A complete
        // intermediate layout must not replace the locked source anchor with its page start.
        val previous = ReaderViewportAnchor.forPagination(
            chapterIndex = state.chapterIndex,
            pending = pendingViewportAnchor,
            locked = stableViewportAnchor.takeIf { viewportAnchorLocked },
            visible = currentPreparedLocation(),
            initial = ReaderPageLocation(
                state.chapterIndex, state.initialBlockIndex,
                state.initialBlockOffsetPx, state.initialBlockCharOffset,
            ),
        )
        val changed = preparedPagination?.layoutKey != next.layoutKey ||
            preparedPagination?.chapterIndex != next.chapterIndex ||
            preparedPagination?.isComplete != next.isComplete
        preparedPagination = next
        if (changed) {
            val targetBlock = previous.blockIndex
            val targetChar = previous.blockCharOffset
            val legacyPx = previous.blockOffsetPx
            preparedPageIndex = findPreparedPageIndex(
                pagination = next,
                blockIndex = targetBlock,
                charOffset = targetChar,
                legacyPixelOffsetPx = legacyPx,
            )
        } else {
            preparedPageIndex = preparedPageIndex.coerceIn(0, next.pages.lastIndex.coerceAtLeast(0))
        }
        if (next.isComplete && pendingViewportAnchor != null) {
            stableViewportAnchor = pendingViewportAnchor!!
            pendingViewportAnchor = null
        }
    }

    // Commit the drag only when the finger is released. For a different chapter, wait for
    // its content and exact pagination instead of seeking inside the old chapter cache.
    LaunchedEffect(
        pendingProgressSeek, state.chapterIndex, state.loading, state.chapter,
        settings.navigationMode, preparedPagination?.chapterIndex, preparedPagination?.isComplete,
        preparedPagination?.pages?.size, listState,
    ) {
        val request = pendingProgressSeek ?: return@LaunchedEffect
        if (state.loading || state.chapterIndex != request.chapterIndex || state.chapter == null) return@LaunchedEffect
        if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) {
            val pagination = preparedPagination
                ?.takeIf { it.chapterIndex == request.chapterIndex && it.isComplete && it.pages.isNotEmpty() }
                ?: return@LaunchedEffect
            preparedPageIndex = ReaderProgressSeek.itemIndex(request.chapterFraction, pagination.pages.size)
            viewportAnchorLocked = false
        } else {
            val scrollState = listState ?: return@LaunchedEffect
            val blockIndex = ReaderProgressSeek.itemIndex(request.chapterFraction, state.chapter.blocks.size)
            scrollState.scrollToItem(blockIndex)
            viewModel.saveReaderPosition(state.bookId, state.chapterIndex, blockIndex, 0, 0)
        }
        pendingProgressSeek = null
    }

    LaunchedEffect(chromeVisible, settings.navigationMode, pendingViewportAnchor, listState) {
        val anchor = pendingViewportAnchor ?: return@LaunchedEffect
        val currentList = listState ?: return@LaunchedEffect
        if (settings.navigationMode != ReaderNavigationMode.SCROLL || anchor.chapterIndex != state.chapterIndex) return@LaunchedEffect
        // The LazyListState survives the chrome swap, but restore the captured pixel anchor after
        // one scheduling point so changed system-bar/scaffold insets cannot reinterpret it.
        kotlinx.coroutines.yield()
        currentList.scrollToItem(anchor.blockIndex.coerceAtLeast(0), anchor.blockOffsetPx.coerceAtLeast(0))
        stableViewportAnchor = anchor
        viewportAnchorLocked = false
        pendingViewportAnchor = null
    }

    LaunchedEffect(chromeVisible, interactionTick, overlay, settings.chromeAutoHideSeconds) {
        val seconds = settings.chromeAutoHideSeconds
        if (settings.readerFullscreenEnabled && chromeVisible && overlay == ReaderOverlay.NONE && seconds > 0) {
            delay(seconds * 1_000L)
            setChromeVisiblePreservingPosition(false)
        }
    }

    // Standard Material chrome is a separate layout from the experimental device shells.
    // On edge-to-edge Android its top/bottom bars must be INSIDE the safe system region.
    // Do not keep these system insets in immersive mode: only the physical cutout remains.
    val experimentalChrome = settings.useWastelandDesign || settings.useCyberpunkDesign
    val standardChromeInsets = if (chromeVisible && !experimentalChrome) {
        Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
    } else Modifier

    Scaffold(
        modifier = Modifier.fillMaxSize().then(standardChromeInsets),
        topBar = {
            if (chromeVisible) {
                // One quiet toolbar for both standard and experimental skins.
                // Secondary actions belong to overflow, never permanent icon slots.
                Row(
                    modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = leaveReader) {
                        FluxThemedIcon(
                            FluxIconRole.BACK,
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                        )
                    }
                    Text(
                        text = chapter?.title?.takeIf { it.isNotBlank() }
                            ?: outline?.title?.takeIf { it.isNotBlank() }
                            ?: "Чтение",
                        modifier = Modifier.fillMaxWidth(0.72f).clickable(
                            enabled = !outline?.chapters.isNullOrEmpty(),
                        ) {
                            interactionTick++
                            overlay = ReaderOverlay.TOC
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (settings.useWastelandDesign || settings.useCyberpunkDesign)
                            LocalFluxThemeMode.current.chromeText
                        else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Box {
                        IconButton(onClick = { moreMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Меню чтения")
                        }
                        DropdownMenu(
                            expanded = moreMenuExpanded,
                            onDismissRequest = { moreMenuExpanded = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Оглавление") },
                                onClick = { moreMenuExpanded = false; overlay = ReaderOverlay.TOC },
                            )
                            DropdownMenuItem(
                                text = { Text("Поиск") },
                                onClick = {
                                    moreMenuExpanded = false
                                    interactionTick++
                                    viewModel.clearSearch()
                                    overlay = ReaderOverlay.SEARCH
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Закладки и заметки") },
                                onClick = { moreMenuExpanded = false; overlay = ReaderOverlay.ANNOTATIONS },
                            )
                            DropdownMenuItem(
                                text = { Text("Параметры текста") },
                                onClick = { moreMenuExpanded = false; overlay = ReaderOverlay.SETTINGS },
                            )
                            if (settings.useWastelandDesign || settings.useCyberpunkDesign) {
                                DropdownMenuItem(
                                    text = { Text("Экранные эффекты") },
                                    onClick = { moreMenuExpanded = false; overlay = ReaderOverlay.EFFECTS },
                                )
                            }
                            if (linkHistory.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Вернуться по ссылке") },
                                    onClick = { moreMenuExpanded = false; returnFromReaderLink() },
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Column {
            if (chromeVisible && listState != null) {
                ReaderBottomBar(
                    state = state,
                    listState = listState,
                    history = pageHistory,
                    preparedPageIndex = if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) preparedPageIndex else null,
                    preparedPageCount = if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) preparedPagination?.takeIf { it.isComplete }?.pages?.size else null,
                    onSpeed = openSpeedFromReader,
                    speedEnabled = state.bookId != null && state.error == null && !state.loading,
                    onPreviousPage = { previousPage() },
                    onNextPage = { nextPage() },
                    onSeekProgress = { fraction ->
                        val count = state.outline?.chapters?.size ?: 0
                        if (count > 0 && state.chapter != null && !state.loading) {
                            val target = ReaderProgressSeek.target(fraction, count)
                            selectedText = null
                            pendingViewportAnchor = null
                            viewportAnchorLocked = false
                            pageNavigationJob?.cancel()
                            pageHistory.clear()
                            linkHistory.clear()
                            interactionTick++
                            pendingProgressSeek = target
                            if (target.chapterIndex != state.chapterIndex) {
                                viewModel.readerChapterAt(target.chapterIndex, 0, 0, 0)
                            }
                        }
                    },
                )
            }
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { scaffoldPadding ->
        when {
            state.error != null -> ErrorPane(
                message = state.error,
                modifier = Modifier.fillMaxSize().padding(scaffoldPadding),
                onRetry = viewModel::readerRetry,
            )
            state.loading || chapter == null -> Box(
                Modifier.fillMaxSize().padding(scaffoldPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            else -> {
                val activeListState = listState
                if (activeListState != null) {
                    Box(Modifier.fillMaxSize().then(fullscreenTransitionModifier)) {
                        // One composition slot keeps pagination and scroll observers alive while
                        // chrome and the ancestor experimental shell change their geometry.
                        ReaderContent(
                            state = state,
                            settings = settings,
                            modifier = pageAnimationModifier,
                            viewModel = viewModel,
                            listState = activeListState,
                            scaffoldPadding = scaffoldPadding,
                            chromeVisible = chromeVisible,
                            isFullscreen = !chromeVisible,
                            onPreviousPage = { previousPage() },
                            onNextPage = { nextPage() },
                            onCenterTap = { setChromeVisiblePreservingPosition(!chromeVisible) },
                            onOpenLink = ::openReaderLink,
                            selectedText = selectedText,
                            annotations = bookAnnotations,
                            onTextRangeSelection = ::updateReaderTextSelection,
                            onSelectionHandleDrag = ::updateSelectionHandle,
                            onSelectionEdgeDrag = ::handleSelectionEdgeDrag,
                            onAnnotationTap = ::openAnnotationPopup,
                            viewportAnchor = pendingViewportAnchor ?: stableViewportAnchor.takeIf { viewportAnchorLocked },
                            measuredLineTops = measuredLineTops,
                            measuredLineStarts = measuredLineStarts,
                            canSaveScrollPosition = !needsScrollRestore && !viewportAnchorLocked,
                            preparedPagination = preparedPagination,
                            preparedPageIndex = preparedPageIndex,
                            paginationCache = preparedPaginationCache,
                            onPreparedPagination = ::acceptPreparedPagination,
                        )
                        if (!chromeVisible) {
                            // The upper camera lane is an overlay, not a measured extra row.
                            ReaderCutoutStatus(
                                state = state,
                                preparedPageIndex = if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) preparedPageIndex else null,
                                preparedPageCount = if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) preparedPagination?.takeIf { it.isComplete }?.pages?.size else null,
                                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                                safeFrameAlreadyApplied = true,
                            )
                        }
                        selectedText?.takeIf { it.menuVisible }?.let { selection ->
                            val scheme = MaterialTheme.colorScheme
                            ReaderTextContextMenu(
                                selection = selection,
                                highlightColors = readerHighlightColors,
                                editingExisting = editingAnnotationBounds != null,
                                onSaveBounds = { saveEditedAnnotationBounds(selection) },
                                onSpeedFromWord = {
                                    startSpeedFromWord(selection.startBlockIndex, selection.startCharOffset)
                                },
                                onCopy = { copyReaderText("Текст", selection.text) },
                                onHighlight = { color ->
                                    addSelectionAnnotation(selection, ReaderAnnotationKind.HIGHLIGHT, color.toArgb())
                                },
                                onQuote = {
                                    addSelectionAnnotation(
                                        selection,
                                        ReaderAnnotationKind.QUOTE,
                                        scheme.secondaryContainer.copy(alpha = 0.62f).toArgb(),
                                    )
                                },
                                onNote = { pendingNoteSelection = selection; selectedText = null },
                                onBookmark = {
                                    addSelectionAnnotation(
                                        selection,
                                        ReaderAnnotationKind.BOOKMARK,
                                        scheme.primaryContainer.copy(alpha = 0.55f).toArgb(),
                                    )
                                },
                                onSearch = { searchSelectedWord(selection) },
                                onDismiss = {
                                    selectedText = null
                                    editingAnnotationBounds = null
                                },
                            )
                        }
                    }
                }
            }
        }
    }

    when (overlay) {
        ReaderOverlay.EFFECTS -> ExperimentalEffectsSheet(
            settings = settings,
            onUpdate = viewModel::updateSettings,
            onDismiss = { overlay = ReaderOverlay.NONE },
        )
        ReaderOverlay.NARRATION -> NarrationSheet(
            status = narrationStatus,
            settings = settings,
            onDismiss = { overlay = ReaderOverlay.NONE },
            onPlay = {
                if (!narrationStatus.paused) {
                    currentReaderLocation()?.let {
                        narrator.restartAtCurrentReaderPosition(it.blockIndex, it.blockCharOffset)
                    }
                }
                narrator.play()
            },
            onPause = narrator::pause,
            onStop = narrator::stop,
            onPrevious = narrator::skipPrevious,
            onNext = narrator::skipNext,
            onPreview = narrator::preview,
            onUpdate = viewModel::updateSettings,
        )
        ReaderOverlay.TOC -> ReaderTocSheet(
            state = state,
            onDismiss = { overlay = ReaderOverlay.NONE },
            onChapter = { index ->
                overlay = ReaderOverlay.NONE
                pageHistory.clear()
                linkHistory.clear()
                viewModel.readerChapter(index)
            },
        )
        ReaderOverlay.SEARCH -> BookSearchSheet(
            state = search,
            onSearch = viewModel::searchBook,
            onHit = { hit ->
                overlay = ReaderOverlay.NONE
                pageHistory.clear()
                linkHistory.clear()
                viewModel.openSearchHit(hit)
            },
            onDismiss = {
                viewModel.clearSearch()
                overlay = ReaderOverlay.NONE
            },
        )
        ReaderOverlay.SETTINGS -> ReaderSettingsSheet(
            settings = settings,
            viewModel = viewModel,
            onDismiss = { overlay = ReaderOverlay.NONE },
        )
        ReaderOverlay.ANNOTATIONS -> ReaderAnnotationsSheet(
            annotations = allAnnotations,
            currentBookId = state.bookId.orEmpty(),
            bookTitle = { id -> libraryBooks.firstOrNull { it.id == id }?.title.orEmpty() },
            chapterTitle = { item ->
                if (item.bookId == state.bookId) {
                    outline?.chapters?.getOrNull(item.chapterIndex)?.title.orEmpty().ifBlank { "Глава ${item.chapterIndex + 1}" }
                } else {
                    "Глава ${item.chapterIndex + 1}"
                }
            },
            onOpen = { item ->
                overlay = ReaderOverlay.NONE
                selectedText = null
                pageHistory.clear()
                linkHistory.clear()
                if (item.bookId == state.bookId) {
                    pendingViewportAnchor = ReaderPageLocation(
                        chapterIndex = item.chapterIndex,
                        blockIndex = item.startBlockIndex,
                        blockOffsetPx = 0,
                        blockCharOffset = item.startCharOffset,
                    )
                    viewportAnchorLocked = true
                    viewModel.readerChapterAt(item.chapterIndex, item.startBlockIndex, 0, item.startCharOffset)
                } else {
                    viewModel.openReaderAt(item.bookId, item.chapterIndex, item.startBlockIndex, item.startCharOffset)
                }
            },
            onDelete = { item -> viewModel.deleteReaderAnnotation(item.id) },
            onEditNote = { item -> editingAnnotation = item },
            onChangeColor = { item, color -> viewModel.updateReaderAnnotationColor(item.id, color.toArgb()) },
            highlightColors = readerHighlightColors,
            onDismiss = { overlay = ReaderOverlay.NONE },
        )
        ReaderOverlay.NONE -> Unit
    }

    annotationPopupTarget?.let { target ->
        val current = bookAnnotations.firstOrNull { it.id == target.annotation.id } ?: target.annotation
        ReaderAnnotationContextMenu(
            target = target.copy(annotation = current),
            highlightColors = readerHighlightColors,
            onCopy = { copyAnnotationText(current) },
            onEditNote = {
                annotationPopupTarget = null
                editingAnnotation = current
            },
            onEditBounds = { beginEditAnnotationBounds(current, target.positionInWindow) },
            onChangeColor = { color -> viewModel.updateReaderAnnotationColor(current.id, color.toArgb()) },
            onDelete = {
                annotationPopupTarget = null
                pendingDeleteAnnotation = current
            },
            onDismiss = { annotationPopupTarget = null },
        )
    }

    pendingDeleteAnnotation?.let { item ->
        DestructiveConfirmDialog(
            title = "Удалить аннотацию?",
            message = item.excerpt.ifBlank { "Эта запись будет удалена из книги." }.take(180),
            onConfirm = {
                viewModel.deleteReaderAnnotation(item.id)
                pendingDeleteAnnotation = null
            },
            onDismiss = { pendingDeleteAnnotation = null },
        )
    }

    pendingNoteSelection?.let { selection ->
        ReaderNoteDialog(
            onSave = { note ->
                addSelectionAnnotation(
                    selection = selection,
                    kind = ReaderAnnotationKind.HIGHLIGHT,
                    colorArgb = readerHighlightColors.first().toArgb(),
                    note = note,
                )
                pendingNoteSelection = null
            },
            onDismiss = { pendingNoteSelection = null },
        )
    }

    editingAnnotation?.let { item ->
        ReaderNoteDialog(
            initialText = item.note,
            onSave = { note ->
                viewModel.updateReaderAnnotationNote(item.id, note)
                editingAnnotation = null
            },
            onDismiss = { editingAnnotation = null },
        )
    }
}

@Composable
private fun ReaderContent(
    state: ReaderUiState,
    settings: ReaderSettings,
    modifier: Modifier = Modifier,
    viewModel: AppViewModel,
    listState: LazyListState,
    scaffoldPadding: androidx.compose.foundation.layout.PaddingValues,
    chromeVisible: Boolean,
    isFullscreen: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onCenterTap: () -> Unit,
    onOpenLink: (String, Boolean) -> Unit,
    selectedText: ReaderTextSelection?,
    annotations: List<ReaderAnnotation>,
    onTextRangeSelection: ((Int, Int, Int, Int, Offset, Boolean) -> Unit)?,
    onSelectionHandleDrag: ((Boolean, Int, Int, Offset, Boolean) -> Unit)?,
    onSelectionEdgeDrag: ((Boolean?, Int, Offset) -> Unit)?,
    onAnnotationTap: ((ReaderAnnotation, Offset) -> Unit)?,
    viewportAnchor: ReaderPageLocation?,
    measuredLineTops: MutableMap<Int, IntArray>,
    measuredLineStarts: MutableMap<Int, IntArray>,
    canSaveScrollPosition: Boolean,
    preparedPagination: PreparedReaderPagination?,
    preparedPageIndex: Int,
    paginationCache: PreparedReaderPaginationCache,
    onPreparedPagination: (PreparedReaderPagination) -> Unit,
) {
    val chapter = state.chapter ?: return

    // Scroll mode keeps the classic continuous position. Screen-paging mode persists a
    // block + character anchor. Chrome/fullscreen changes must not introduce their own snap.
    LaunchedEffect(listState, state.chapterIndex, chapter.blocks.size, settings.navigationMode, canSaveScrollPosition) {
        if (settings.navigationMode != ReaderNavigationMode.SCROLL || !canSaveScrollPosition) return@LaunchedEffect
        snapshotFlow {
            val index = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            Triple(index, offset, ReaderViewportAnchor.scrollCharOffset(measuredLineTops[index], measuredLineStarts[index], offset))
        }
            .distinctUntilChanged()
            .collectLatest { (index, offset, charOffset) ->
                delay(220)
                viewModel.saveReaderPosition(state.bookId, state.chapterIndex, index, offset, charOffset)
            }
    }

    val layoutDirection = LocalLayoutDirection.current
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()
    val cameraTop = ImmersiveCameraClearance.current()
    val edgePadding = DeviceEdgeClearance.immersivePadding()
    val inExperimentalFrame = settings.useWastelandDesign || settings.useCyberpunkDesign
    val viewportModifier = when {
        isFullscreen -> Modifier.padding(
            start = maxOf(if (inExperimentalFrame) 0.dp else safePadding.calculateStartPadding(layoutDirection),
                edgePadding.calculateStartPadding(layoutDirection) - settings.horizontalPaddingDp.dp, 0.dp),
            end = maxOf(if (inExperimentalFrame) 0.dp else safePadding.calculateEndPadding(layoutDirection),
                edgePadding.calculateEndPadding(layoutDirection) - settings.horizontalPaddingDp.dp, 0.dp),
            // Only the physical centered camera hole is guarded. No status-bar or
            // navigation-bar insets survive immersive mode, including in the theme frame.
            top = cameraTop,
            bottom = (edgePadding.calculateBottomPadding() - settings.bottomPaddingDp.dp).coerceAtLeast(0.dp),
        )
        !chromeVisible -> Modifier.padding(
            start = safePadding.calculateStartPadding(layoutDirection),
            top = safePadding.calculateTopPadding(),
            end = safePadding.calculateEndPadding(layoutDirection),
            bottom = safePadding.calculateBottomPadding(),
        )
        else -> Modifier.padding(scaffoldPadding)
    }

    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .then(viewportModifier)
            .readerGestures(
                edgeFraction = settings.edgeTapFraction,
                edgeTapsEnabled = settings.edgeTapZonesEnabled,
                centerTapEnabled = true,
                swipeEnabled = settings.swipeNavigationEnabled,
                onPrevious = onPreviousPage,
                onNext = onNextPage,
                onCenterTap = onCenterTap,
            ),
    ) {
        val readableMaxWidth = 760.dp
        val sideGutter = if (maxWidth > readableMaxWidth) (maxWidth - readableMaxWidth) / 2 else 0.dp

        if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) {
            ReaderPreparedPageContent(
                state = state,
                settings = settings,
                chapter = chapter,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = sideGutter),
                viewModel = viewModel,
                isFullscreen = isFullscreen,
                viewportWidth = maxWidth - sideGutter * 2,
                viewportHeight = maxHeight,
                preparedPagination = preparedPagination,
                preparedPageIndex = preparedPageIndex,
                paginationCache = paginationCache,
                onPreparedPagination = onPreparedPagination,
                onOpenLink = onOpenLink,
                selectedText = selectedText,
                annotations = annotations,
                onTextRangeSelection = onTextRangeSelection,
                onSelectionHandleDrag = onSelectionHandleDrag,
                onSelectionEdgeDrag = onSelectionEdgeDrag,
                onAnnotationTap = onAnnotationTap,
                viewportAnchor = viewportAnchor,
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = sideGutter)
                    .padding(
                        top = if (isFullscreen) 2.dp else 0.dp,
                        bottom = 0.dp,
                    ),
                userScrollEnabled = true,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    top = settings.topPaddingDp.dp,
                    bottom = (settings.bottomPaddingDp + if (isFullscreen) 2f else 8f).dp,
                ),
                verticalArrangement = Arrangement.spacedBy(settings.paragraphSpacingDp.dp),
            ) {
                itemsIndexed(chapter.blocks, key = { index, block -> blockKey(index, block) }) { index, block ->
                    when (block) {
                        is ReaderBlock.Text -> ReaderTextBlock(
                            block = block,
                            settings = settings,
                            onOpenLink = onOpenLink,
                            onTextLayout = { result ->
                                val tops = IntArray(result.lineCount) { line ->
                                    result.getLineTop(line).roundToInt().coerceAtLeast(0)
                                }
                                if (!measuredLineTops[index].contentEqualsSafe(tops)) measuredLineTops[index] = tops
                                val starts = IntArray(result.lineCount) { result.getLineStart(it) }
                                if (!measuredLineStarts[index].contentEqualsSafe(starts)) measuredLineStarts[index] = starts
                            },
                            continuation = false,
                            selectedWordSourceRange = selectedText
                                ?.takeIf { index in it.startBlockIndex..it.endBlockIndex }
                                ?.let { selection ->
                                    val start = if (index == selection.startBlockIndex) selection.startCharOffset else 0
                                    val end = if (index == selection.endBlockIndex) selection.endCharOffsetExclusive else block.text.length
                                    start until end
                                },
                            sourceBlockIndex = index,
                            activeSelection = selectedText,
                            annotations = annotations.filter {
                                it.chapterIndex == state.chapterIndex &&
                                    index in it.startBlockIndex..it.endBlockIndex
                            },
                            onTextRangeSelection = onTextRangeSelection?.let { callback ->
                                { anchorBlock: Int, anchor: Int, currentBlock: Int, current: Int, position: Offset, finished: Boolean ->
                                    callback(anchorBlock, anchor, currentBlock, current, position, finished)
                                }
                            },
                            onSelectionHandleDrag = onSelectionHandleDrag,
                            onSelectionEdgeDrag = onSelectionEdgeDrag,
                            onAnnotationTap = onAnnotationTap,
                            chapterTextBlocks = chapter.blocks.map { (it as? ReaderBlock.Text)?.text },
                        )
                        is ReaderBlock.Image -> ReaderImageBlock(state.bookId.orEmpty(), block, viewModel, settings)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaderPreparedPageContent(
    state: ReaderUiState,
    settings: ReaderSettings,
    chapter: io.fluxreader.app.data.ChapterContent,
    modifier: Modifier,
    viewModel: AppViewModel,
    isFullscreen: Boolean,
    viewportWidth: androidx.compose.ui.unit.Dp,
    viewportHeight: androidx.compose.ui.unit.Dp,
    preparedPagination: PreparedReaderPagination?,
    preparedPageIndex: Int,
    paginationCache: PreparedReaderPaginationCache,
    onPreparedPagination: (PreparedReaderPagination) -> Unit,
    onOpenLink: (String, Boolean) -> Unit,
    selectedText: ReaderTextSelection?,
    annotations: List<ReaderAnnotation>,
    onTextRangeSelection: ((Int, Int, Int, Int, Offset, Boolean) -> Unit)?,
    onSelectionHandleDrag: ((Boolean, Int, Int, Offset, Boolean) -> Unit)?,
    onSelectionEdgeDrag: ((Boolean?, Int, Offset) -> Unit)?,
    onAnnotationTap: ((ReaderAnnotation, Offset) -> Unit)?,
    viewportAnchor: ReaderPageLocation?,
) {
    val density = LocalDensity.current
    val typography = MaterialTheme.typography
    val family = if (LocalFluxThemeMode.current.isExperimentalChrome) {
        FontFamily.Monospace
    } else {
        resolveReaderFontFamily(settings.fontMode, settings.customFontPath)
    }
    val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()
    val topGuard = if (isFullscreen) 2.dp else 3.dp
    val bottomGuard = if (isFullscreen) 3.dp else 5.dp
    val measuredViewportWidthPx = with(density) { viewportWidth.roundToPx().coerceAtLeast(1) }
    val measuredViewportHeightPx = with(density) {
        (viewportHeight - topGuard - bottomGuard).roundToPx().coerceAtLeast(1)
    }
    fun paginationLayoutKey(chapterIndex: Int): String = buildString {
        append(state.bookId).append(':').append(chapterIndex).append(':')
        append(measuredViewportWidthPx).append('x').append(measuredViewportHeightPx).append(':')
        append(density.fontScale).append(':').append(settings.fontSizeSp).append(':')
        append(settings.lineHeightMultiplier).append(':').append(settings.horizontalPaddingDp).append(':')
        append(settings.topPaddingDp).append(':').append(settings.bottomPaddingDp).append(':')
        append(settings.paragraphSpacingDp).append(':')
        append(settings.firstLineIndentDp).append(':').append(settings.fontMode.name).append(':')
        append(settings.customFontPath.hashCode()).append(':').append(settings.textAlignment.name).append(':')
        append(settings.hyphenationEnabled).append(':')
        append(settings.chapterTitleScale).append(':').append(settings.chapterHeadingScale).append(':')
        append(settings.centerChapterHeadings).append(':')
        append(settings.experimentalDesignEnabled).append(':').append(settings.theme.name)
    }
    val layoutKey = remember(
        state.bookId,
        state.chapterIndex,
        measuredViewportWidthPx,
        measuredViewportHeightPx,
        density.fontScale,
        settings.fontSizeSp,
        settings.lineHeightMultiplier,
        settings.horizontalPaddingDp,
        settings.topPaddingDp,
        settings.bottomPaddingDp,
        settings.paragraphSpacingDp,
        settings.firstLineIndentDp,
        settings.fontMode,
        settings.customFontPath,
        settings.textAlignment,
        settings.hyphenationEnabled,
        settings.chapterTitleScale,
        settings.chapterHeadingScale,
        settings.centerChapterHeadings,
        settings.experimentalDesignEnabled, settings.theme,
    ) {
        paginationLayoutKey(state.chapterIndex)
    }

    val cachedPagination = remember(layoutKey) { paginationCache.get(layoutKey) }
    // Text measurement is mostly cooperative, but a single Compose measure call itself cannot be
    // cancelled. Keep a live key so a result finishing after rotation/fullscreen/viewport changes
    // is discarded instead of entering the cache or replacing the current page.
    val latestLayoutKey by rememberUpdatedState(layoutKey)
    val previewAnchor = remember(
        layoutKey, viewportAnchor, preparedPagination, preparedPageIndex,
        state.initialBlockIndex, state.initialBlockCharOffset,
    ) {
        val previousPage = preparedPagination
            ?.takeIf { it.chapterIndex == state.chapterIndex }
            ?.pages
            ?.getOrNull(preparedPageIndex)
        val blockIndex = viewportAnchor
            ?.takeIf { it.chapterIndex == state.chapterIndex }
            ?.blockIndex
            ?: previousPage?.startBlockIndex
            ?: state.initialBlockIndex
        val charOffset = viewportAnchor
            ?.takeIf { it.chapterIndex == state.chapterIndex }
            ?.blockCharOffset
            ?: previousPage?.startCharOffset
            ?: state.initialBlockCharOffset
        blockIndex to charOffset
    }
    LaunchedEffect(layoutKey, chapter.blocks.size, cachedPagination?.layoutKey) {
        if (preparedPagination?.takeIf { it.layoutKey == layoutKey && it.isComplete } != null) return@LaunchedEffect
        if (cachedPagination != null) {
            onPreparedPagination(cachedPagination)
            return@LaunchedEffect
        }

        // Paint useful text at the saved anchor first. This is intentionally only one page and
        // therefore never reports a fake page count or allows crossing a chapter boundary.
        val preview = prepareReaderAnchorPreviewPagination(
            chapterIndex = state.chapterIndex,
            chapter = chapter,
            startBlockIndex = previewAnchor.first,
            startCharOffset = previewAnchor.second,
            settings = settings,
            textMeasurer = textMeasurer,
            typography = typography,
            family = family,
            density = density,
            viewportWidthPx = measuredViewportWidthPx,
            viewportHeightPx = measuredViewportHeightPx,
            layoutKey = layoutKey,
        )
        if (preview != null) {
            if (!ReaderPaginationCommitPolicy.shouldCommit(layoutKey, latestLayoutKey)) return@LaunchedEffect
            onPreparedPagination(preview)
            // Give Compose a scheduling point so the preview can be committed before the full
            // chapter pagination continues cooperatively.
            kotlinx.coroutines.yield()
        }

        val prepared = prepareReaderChapterPagination(
            chapterIndex = state.chapterIndex,
            chapter = chapter,
            settings = settings,
            textMeasurer = textMeasurer,
            typography = typography,
            family = family,
            density = density,
            viewportWidthPx = measuredViewportWidthPx,
            viewportHeightPx = measuredViewportHeightPx,
            layoutKey = layoutKey,
        )
        if (!ReaderPaginationCommitPolicy.shouldCommit(layoutKey, latestLayoutKey)) return@LaunchedEffect
        paginationCache.put(prepared)
        onPreparedPagination(prepared)

        // Low-priority pagination prewarm for the next chapter. The content itself is normally
        // already in BookContentRepository because AppViewModel prefetch is next-first. Reusing the
        // same viewport/style key makes the chapter boundary instant when the user keeps reading.
        val nextChapterIndex = state.chapterIndex + 1
        val chapterCount = state.outline?.chapters?.size ?: 0
        if (nextChapterIndex < chapterCount) {
            kotlinx.coroutines.delay(120L)
            if (!ReaderPaginationCommitPolicy.shouldCommit(layoutKey, latestLayoutKey)) return@LaunchedEffect
            val nextLayoutKey = paginationLayoutKey(nextChapterIndex)
            if (paginationCache.get(nextLayoutKey) == null) {
                val nextChapter = viewModel.readerChapterForPaginationPrewarm(nextChapterIndex)
                if (nextChapter != null) {
                    val nextPrepared = prepareReaderChapterPagination(
                        chapterIndex = nextChapterIndex,
                        chapter = nextChapter,
                        settings = settings,
                        textMeasurer = textMeasurer,
                        typography = typography,
                        family = family,
                        density = density,
                        viewportWidthPx = measuredViewportWidthPx,
                        viewportHeightPx = measuredViewportHeightPx,
                        layoutKey = nextLayoutKey,
                    )
                    if (!ReaderPaginationCommitPolicy.shouldCommit(layoutKey, latestLayoutKey)) return@LaunchedEffect
                    paginationCache.put(nextPrepared)
                }
            }
        }
    }

    val exactPagination = preparedPagination?.takeIf {
        it.chapterIndex == state.chapterIndex && it.layoutKey == layoutKey
    } ?: cachedPagination
    // During a chrome/fullscreen geometry change keep painting the previous page instead of
    // replacing useful text with a blocking "preparing pages" screen. The new one-page anchor
    // preview/full pagination will replace this fallback as soon as it is measured.
    val active = exactPagination ?: preparedPagination?.takeIf { it.chapterIndex == state.chapterIndex }
    if (active == null) {
        Box(modifier, contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    val usingFallbackLayout = active.layoutKey != layoutKey

    val safePageIndex = preparedPageIndex.coerceIn(0, active.pages.lastIndex.coerceAtLeast(0))
    val page = active.pages.getOrNull(safePageIndex) ?: return

    Box(
        modifier
            .padding(top = topGuard, bottom = bottomGuard),
    ) {
        if (page.items.size == 1 && page.items.first() is PreparedReaderPageItem.Image) {
            val item = page.items.first() as PreparedReaderPageItem.Image
            val image = chapter.blocks.getOrNull(item.blockIndex) as? ReaderBlock.Image
            if (image != null) {
                ReaderImagePageBlock(
                    bookId = state.bookId.orEmpty(),
                    block = image,
                    viewModel = viewModel,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = settings.horizontalPaddingDp.dp)
                        .padding(
                            top = settings.topPaddingDp.dp,
                            bottom = settings.bottomPaddingDp.dp,
                        ),
                )
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(top = settings.topPaddingDp.dp, bottom = settings.bottomPaddingDp.dp),
                verticalArrangement = Arrangement.spacedBy(settings.paragraphSpacingDp.dp),
            ) {
                page.items.forEach { item ->
                    when (item) {
                        is PreparedReaderPageItem.TextSlice -> {
                            val source = chapter.blocks.getOrNull(item.blockIndex) as? ReaderBlock.Text
                            if (source != null) {
                                val slicedBlock = remember(source, item.startCharOffset, item.endCharOffset) {
                                    sliceReaderTextBlock(
                                        block = source,
                                        startCharOffset = item.startCharOffset,
                                        endCharOffset = item.endCharOffset,
                                    )
                                }
                                ReaderTextBlock(
                                    block = slicedBlock,
                                    settings = settings,
                                    onOpenLink = onOpenLink,
                                    onTextLayout = {},
                                    continuation = item.startCharOffset > 0,
                                    sourceCharOffsetBase = item.startCharOffset,
                                    selectedWordSourceRange = selectedText
                                        ?.takeIf { item.blockIndex in it.startBlockIndex..it.endBlockIndex }
                                        ?.let { selection ->
                                            val start = if (item.blockIndex == selection.startBlockIndex) selection.startCharOffset else 0
                                            val end = if (item.blockIndex == selection.endBlockIndex) selection.endCharOffsetExclusive else source.text.length
                                            start until end
                                        },
                                    sourceBlockIndex = item.blockIndex,
                                    activeSelection = selectedText,
                                    annotations = annotations.filter {
                                        it.chapterIndex == state.chapterIndex &&
                                            item.blockIndex in it.startBlockIndex..it.endBlockIndex
                                    },
                                    onTextRangeSelection = onTextRangeSelection?.let { callback ->
                                        { anchorBlock: Int, anchor: Int, currentBlock: Int, current: Int, position: Offset, finished: Boolean ->
                                            callback(anchorBlock, anchor, currentBlock, current, position, finished)
                                        }
                                    },
                                    onSelectionHandleDrag = onSelectionHandleDrag,
                                    onSelectionEdgeDrag = onSelectionEdgeDrag,
                                    onAnnotationTap = onAnnotationTap,
                                    chapterTextBlocks = chapter.blocks.map { (it as? ReaderBlock.Text)?.text },
                                )
                            }
                        }
                        is PreparedReaderPageItem.Image -> Unit
                    }
                }
            }
        }
        if (!active.isComplete || usingFallbackLayout) {
            LinearProgressIndicator(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp),
            )
        }
    }
}

@Composable
private fun ReaderImagePageBlock(
    bookId: String,
    block: ReaderBlock.Image,
    viewModel: AppViewModel,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val targetWidthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(320)
        val bitmap by produceState<Bitmap?>(initialValue = null, key1 = bookId, key2 = block.ref, key3 = targetWidthPx) {
            value = null
            value = viewModel.loadImage(bookId, block.ref, targetWidthPx)
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = block.alt.ifBlank { null },
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else if (block.alt.isNotBlank()) {
            Text(
                block.alt,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun ReaderCutoutStatus(
    state: ReaderUiState,
    preparedPageIndex: Int?,
    preparedPageCount: Int?,
    modifier: Modifier = Modifier,
    safeFrameAlreadyApplied: Boolean = false,
) {
    // Display progress to the left and right of the physical camera hole, without
    // adding another content row. Do not reserve a status bar after hiding it.
    val visibleHudHeight = if (safeFrameAlreadyApplied) ImmersiveCameraClearance.current() else 24.dp
    val chapterCount = state.outline?.chapters?.size ?: 0
    val chapterNumber = (state.chapterIndex + 1).coerceAtLeast(1)
    val pageCount = preparedPageCount?.takeIf { it > 0 }
    val pageNumber = pageCount?.let { (preparedPageIndex ?: 0).coerceIn(0, it - 1) + 1 }
    val chapterFraction = if (pageCount != null && pageNumber != null) {
        pageNumber.toFloat() / pageCount.toFloat()
    } else {
        0.5f
    }
    val progress = if (chapterCount <= 0) 0 else {
        (((state.chapterIndex + chapterFraction) / chapterCount.toFloat()) * 100f)
            .toInt()
            .coerceIn(0, 100)
    }
    val subtle = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f)

    Row(
        modifier = modifier
            .height(visibleHudHeight)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "$progress%",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = subtle,
            textAlign = TextAlign.Start,
        )
        // A fixed empty center lane is intentional: progress never crosses the Pixel-style
        // centered hole-punch even when immersive mode reports a zero status-bar inset.
        Spacer(Modifier.width(96.dp))
        Text(
            when {
                pageCount != null && pageNumber != null -> "$pageNumber / $pageCount"
                chapterCount > 0 -> "$chapterNumber / $chapterCount"
                else -> ""
            },
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = subtle,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun ReaderAnnotationContextMenu(
    target: ReaderAnnotationPopupTarget,
    highlightColors: List<Color>,
    onCopy: () -> Unit,
    onEditNote: () -> Unit,
    onEditBounds: () -> Unit,
    onChangeColor: (Color) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val menuWidth = 268.dp
    val estimatedHeight = if (target.annotation.kind == ReaderAnnotationKind.HIGHLIGHT) 236.dp else 188.dp
    val marginPx = with(density) { 12.dp.roundToPx() }
    val menuWidthPx = with(density) { menuWidth.roundToPx() }
    val menuHeightPx = with(density) { estimatedHeight.roundToPx() }
    val gapPx = with(density) { 10.dp.roundToPx() }
    val maxX = (view.width - menuWidthPx - marginPx).coerceAtLeast(marginPx)
    val x = (target.positionInWindow.x.roundToInt() - menuWidthPx / 2).coerceIn(marginPx, maxX)
    val below = target.positionInWindow.y.roundToInt() + gapPx
    val y = if (below + menuHeightPx <= view.height - marginPx) {
        below
    } else {
        (target.positionInWindow.y.roundToInt() - menuHeightPx - gapPx).coerceAtLeast(marginPx)
    }

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(x, y),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Surface(
            modifier = Modifier.width(menuWidth),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                Text(
                    target.annotation.excerpt.replace("\n", " ").take(88),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Копировать") },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = onCopy,
                )
                DropdownMenuItem(
                    text = { Text(if (target.annotation.note.isBlank()) "Добавить заметку" else "Изменить заметку") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                    onClick = onEditNote,
                )
                if (target.annotation.kind != ReaderAnnotationKind.BOOKMARK) {
                    DropdownMenuItem(
                        text = { Text("Изменить границы") },
                        onClick = onEditBounds,
                    )
                }
                if (target.annotation.kind == ReaderAnnotationKind.HIGHLIGHT) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Цвет", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        highlightColors.forEach { color ->
                            Surface(
                                modifier = Modifier.size(24.dp).clickable { onChangeColor(color) },
                                shape = CircleShape,
                                color = color,
                                border = BorderStroke(
                                    if (target.annotation.colorArgb == color.toArgb()) 2.dp else 1.dp,
                                    if (target.annotation.colorArgb == color.toArgb()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                                ),
                            ) {}
                        }
                    }
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                    onClick = onDelete,
                )
            }
        }
    }
}

@Composable
private fun ReaderTextContextMenu(
    selection: ReaderTextSelection,
    highlightColors: List<Color>,
    editingExisting: Boolean = false,
    onSaveBounds: () -> Unit = {},
    onSpeedFromWord: () -> Unit,
    onCopy: () -> Unit,
    onHighlight: (Color) -> Unit,
    onQuote: () -> Unit,
    onNote: () -> Unit,
    onBookmark: () -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit,
) {
    val density = LocalDensity.current
    val view = LocalView.current
    val menuWidth = 292.dp
    val estimatedHeight = if (editingExisting) 172.dp else 360.dp
    val marginPx = with(density) { 12.dp.roundToPx() }
    val menuWidthPx = with(density) { menuWidth.roundToPx() }
    val menuHeightPx = with(density) { estimatedHeight.roundToPx() }
    val gapPx = with(density) { 12.dp.roundToPx() }
    val maxX = (view.width - menuWidthPx - marginPx).coerceAtLeast(marginPx)
    val x = (selection.menuPositionInWindow.x.roundToInt() - menuWidthPx / 2).coerceIn(marginPx, maxX)
    val below = selection.menuPositionInWindow.y.roundToInt() + gapPx
    val y = if (below + menuHeightPx <= view.height - marginPx) below
    else (selection.menuPositionInWindow.y.roundToInt() - menuHeightPx - gapPx).coerceAtLeast(marginPx)

    Popup(
        alignment = Alignment.TopStart,
        offset = IntOffset(x, y),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        Surface(
            modifier = Modifier.width(menuWidth),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = 14.dp,
        ) {
            Column(Modifier.padding(vertical = 6.dp)) {
                Text(
                    selection.text.replace("\n", " ").take(72),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                HorizontalDivider()
                if (editingExisting) {
                    DropdownMenuItem(text = { Text("Сохранить новые границы") }, onClick = onSaveBounds)
                    DropdownMenuItem(text = { Text("Отмена") }, onClick = onDismiss)
                } else {
                    DropdownMenuItem(
                        text = { Text("Скорочтение отсюда") },
                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        onClick = onSpeedFromWord,
                    )
                    DropdownMenuItem(
                        text = { Text("Копировать") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                        onClick = onCopy,
                    )
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("Выделить", style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                        highlightColors.forEach { color ->
                            Surface(
                                modifier = Modifier.width(28.dp).height(28.dp).clickable { onHighlight(color) },
                                shape = CircleShape,
                                color = color,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
                            ) {}
                        }
                    }
                    DropdownMenuItem(
                        text = { Text("Сохранить как цитату") },
                        leadingIcon = { Icon(Icons.Default.FormatQuote, contentDescription = null) },
                        onClick = onQuote,
                    )
                    DropdownMenuItem(
                        text = { Text("Добавить заметку") },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                        onClick = onNote,
                    )
                    DropdownMenuItem(
                        text = { Text("Добавить закладку") },
                        leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                        onClick = onBookmark,
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Найти в книге") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        onClick = onSearch,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    state: ReaderUiState,
    listState: LazyListState,
    history: List<ReaderPageLocation>,
    preparedPageIndex: Int?,
    preparedPageCount: Int?,
    onSpeed: () -> Unit,
    speedEnabled: Boolean,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    onSeekProgress: (Float) -> Unit,
) {
    val outline = state.outline
    val chapterCount = outline?.chapters?.size ?: 0
    val pageCount = preparedPageCount?.takeIf { it > 0 }
    val pageNumber = pageCount?.let { (preparedPageIndex ?: 0).coerceIn(0, it - 1) + 1 }
    val chapterFraction = when {
        pageCount != null && pageNumber != null -> (pageNumber - 1).toFloat() / pageCount.toFloat()
        state.chapter != null && state.chapter.blocks.isNotEmpty() -> {
            val visible = listState.layoutInfo.visibleItemsInfo.firstOrNull()
            val withinItem = if (visible != null && visible.size > 0) {
                listState.firstVisibleItemScrollOffset.toFloat() / visible.size.toFloat()
            } else 0f
            ((listState.firstVisibleItemIndex + withinItem) / state.chapter.blocks.size.toFloat()).coerceIn(0f, 1f)
        }
        else -> 0f
    }
    val progress = if (chapterCount <= 0) 0f else {
        ((state.chapterIndex.toFloat() + chapterFraction) / chapterCount.toFloat()).coerceIn(0f, 1f)
    }
    var scrubDraft by remember(state.bookId) { mutableFloatStateOf(0f) }
    var scrubbing by remember(state.bookId) { mutableStateOf(false) }
    val displayedProgress = if (scrubbing) scrubDraft else progress
    val canScrub = chapterCount > 0 && !state.loading && state.error == null
    val scrubBar: @Composable (Modifier) -> Unit = { sliderModifier ->
        SettingsSlider(
            value = displayedProgress.coerceIn(0f, 1f),
            onValueChange = { scrubDraft = it; scrubbing = true },
            onValueChangeFinished = {
                if (scrubbing) {
                    val target = scrubDraft
                    scrubbing = false
                    onSeekProgress(target)
                }
            },
            enabled = canScrub,
            valueRange = 0f..1f,
            modifier = sliderModifier,
            colors = SliderDefaults.colors(
                activeTrackColor = LocalFluxThemeMode.current.chromeAccent.takeIf { LocalFluxThemeMode.current.isExperimentalChrome }
                    ?: MaterialTheme.colorScheme.primary,
                thumbColor = LocalFluxThemeMode.current.chromeAccent.takeIf { LocalFluxThemeMode.current.isExperimentalChrome }
                    ?: MaterialTheme.colorScheme.primary,
            ),
        )
    }
    val canPrevious = if (pageCount != null && pageNumber != null) {
        pageNumber > 1 || state.chapterIndex > 0
    } else {
        history.isNotEmpty() || listState.canScrollBackward || state.chapterIndex > 0
    }
    val canNext = if (pageCount != null && pageNumber != null) {
        pageNumber < pageCount || (chapterCount > 0 && state.chapterIndex < chapterCount - 1)
    } else {
        listState.canScrollForward || (chapterCount > 0 && state.chapterIndex < chapterCount - 1)
    }

    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalPanel(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 2.dp),
            accent = false,
            contentPadding = 6.dp,
        ) {
            Column(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${(displayedProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = LocalFluxThemeMode.current.chromeText)
                    scrubBar(Modifier.weight(1f).height(32.dp))
                    Text(
                        when {
                            chapterCount <= 0 -> "—"
                            pageCount != null && pageNumber != null -> "${state.chapterIndex + 1}/$chapterCount · $pageNumber/$pageCount"
                            else -> "${state.chapterIndex + 1}/$chapterCount"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = LocalFluxThemeMode.current.chromeText,
                    )
                }
                Spacer(Modifier.height(1.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ExperimentalPanel(accent = true, contentPadding = 0.dp) {
                        IconButton(onClick = onPreviousPage, enabled = canPrevious && !state.loading, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущая страница", tint = LocalFluxThemeMode.current.chromeAccent)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        ExperimentalPanel(contentPadding = 0.dp) {
                            IconButton(onClick = onSpeed, enabled = speedEnabled, modifier = Modifier.size(40.dp)) {
                                FluxThemedIcon(FluxIconRole.SPEED, Icons.Default.PlayArrow, contentDescription = "Скорочтение", modifier = Modifier.size(22.dp))
                            }
                        }
                        ExperimentalPanel(contentPadding = 5.dp) {
                            Text(
                                "CHAPTER ${state.chapterIndex + 1}",
                                style = MaterialTheme.typography.labelLarge,
                                color = LocalFluxThemeMode.current.chromeText,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    ExperimentalPanel(accent = true, contentPadding = 0.dp) {
                        IconButton(onClick = onNextPage, enabled = canNext && !state.loading, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Следующая страница", tint = LocalFluxThemeMode.current.chromeAccent)
                        }
                    }
                }
            }
        }
        return
    }

    Surface(tonalElevation = 3.dp) {
        Column(Modifier.fillMaxWidth()) {
            scrubBar(Modifier.fillMaxWidth().padding(horizontal = 10.dp).height(40.dp))
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onPreviousPage, enabled = canPrevious && !state.loading) {
                    FluxThemedIcon(FluxIconRole.BACK, Icons.Default.ChevronLeft, contentDescription = "Предыдущая страница")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        when {
                            chapterCount <= 0 -> "—"
                            pageCount != null && pageNumber != null ->
                                "${state.chapterIndex + 1}/$chapterCount  •  $pageNumber/$pageCount"
                            else -> "${state.chapterIndex + 1}/$chapterCount"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Surface(
                        modifier = Modifier
                            .width(42.dp)
                            .height(42.dp)
                            .clickable(enabled = speedEnabled, onClick = onSpeed),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        contentColor = if (speedEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        border = BorderStroke(1.dp, if (speedEnabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 1.dp,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            FluxThemedIcon(FluxIconRole.SPEED, Icons.Default.PlayArrow, contentDescription = "Скорочтение")
                        }
                    }
                }
                IconButton(onClick = onNextPage, enabled = canNext && !state.loading) {
                    FluxThemedIcon(FluxIconRole.BACK, Icons.Default.ChevronRight, contentDescription = "Следующая страница")
                }
            }
        }
    }
}

private suspend fun navigateReaderPage(
    direction: Int,
    state: ReaderUiState,
    settings: ReaderSettings,
    listState: LazyListState,
    history: MutableList<ReaderPageLocation>,
    viewModel: AppViewModel,
    pageSnapPx: Int,
    measuredLineTops: Map<Int, IntArray>,
) {
    if (direction == 0 || state.loading || state.chapter == null) return

    val currentIndex = listState.firstVisibleItemIndex
    val current = ReaderPageLocation(
        chapterIndex = state.chapterIndex,
        blockIndex = currentIndex,
        blockOffsetPx = snapToMeasuredLine(
            blockIndex = currentIndex,
            offsetPx = listState.firstVisibleItemScrollOffset,
            measuredLineTops = measuredLineTops,
            fallbackStepPx = pageSnapPx,
        ),
    )

    if (direction < 0) {
        if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING && history.isNotEmpty()) {
            val target = history.removeAt(history.lastIndex)
            if (target.chapterIndex == state.chapterIndex) {
                moveToPageAnchor(listState, target, smooth = false)
            } else {
                viewModel.readerChapterAt(target.chapterIndex, target.blockIndex, target.blockOffsetPx)
            }
            return
        }

        previousPageAnchor(listState, state.chapterIndex, pageSnapPx, measuredLineTops)?.let { target ->
            moveToPageAnchor(
                listState = listState,
                target = target,
                smooth = settings.navigationMode == ReaderNavigationMode.SCROLL && settings.smoothPageTurns,
            )
            return
        }

        if (state.chapterIndex > 0) {
            val previousChapter = state.chapterIndex - 1
            viewModel.readerChapterAt(previousChapter, Int.MAX_VALUE, 0)
        }
        return
    }

    val target = nextPageAnchor(listState, state.chapterIndex, pageSnapPx, measuredLineTops)
    if (target != null && target != current) {
        if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) {
            if (history.lastOrNull() != current) history.add(current)
            while (history.size > 256) history.removeAt(0)
        }
        moveToPageAnchor(
            listState = listState,
            target = target,
            smooth = settings.navigationMode == ReaderNavigationMode.SCROLL && settings.smoothPageTurns,
        )
        return
    }

    val lastChapter = state.outline?.chapters?.lastIndex ?: state.chapterIndex
    if (state.chapterIndex < lastChapter) {
        if (settings.navigationMode == ReaderNavigationMode.SCREEN_PAGING) {
            if (history.lastOrNull() != current) history.add(current)
            while (history.size > 256) history.removeAt(0)
        }
        viewModel.readerChapterAt(state.chapterIndex + 1, 0, 0)
    }
}

private fun nextPageAnchor(
    listState: LazyListState,
    chapterIndex: Int,
    pageSnapPx: Int,
    measuredLineTops: Map<Int, IntArray>,
): ReaderPageLocation? {
    if (!listState.canScrollForward) return null
    val layout = listState.layoutInfo
    val anchor: ReaderPageAnchor = ReaderPageMath.next(
        currentIndex = listState.firstVisibleItemIndex,
        currentOffsetPx = listState.firstVisibleItemScrollOffset,
        viewportStartPx = layout.viewportStartOffset,
        viewportEndPx = layout.viewportEndOffset,
        visibleItems = layout.visibleItemsInfo.map { item ->
            ReaderPageItem(item.index, item.offset, item.size)
        },
        snapStepPx = 1,
    ) ?: return null
    return ReaderPageLocation(
        chapterIndex,
        anchor.blockIndex,
        snapToMeasuredLine(anchor.blockIndex, anchor.blockOffsetPx, measuredLineTops, pageSnapPx),
    )
}

private fun previousPageAnchor(
    listState: LazyListState,
    chapterIndex: Int,
    pageSnapPx: Int,
    measuredLineTops: Map<Int, IntArray>,
): ReaderPageLocation? {
    if (!listState.canScrollBackward) return null
    val layout = listState.layoutInfo
    val currentIndex = listState.firstVisibleItemIndex
    val previousVisible = layout.visibleItemsInfo.lastOrNull { it.index < currentIndex }?.let { item ->
        ReaderPageItem(item.index, item.offset, item.size)
    }
    val anchor = ReaderPageMath.previous(
        currentIndex = currentIndex,
        currentOffsetPx = listState.firstVisibleItemScrollOffset,
        viewportHeightPx = layout.viewportEndOffset - layout.viewportStartOffset,
        previousVisibleItem = previousVisible,
        snapStepPx = 1,
    ) ?: return null
    return ReaderPageLocation(
        chapterIndex,
        anchor.blockIndex,
        snapToMeasuredLine(anchor.blockIndex, anchor.blockOffsetPx, measuredLineTops, pageSnapPx),
    )
}

private fun snapToMeasuredLine(
    blockIndex: Int,
    offsetPx: Int,
    measuredLineTops: Map<Int, IntArray>,
    fallbackStepPx: Int,
): Int = ReaderPageMath.snapToMeasuredLine(
    valuePx = offsetPx,
    lineTopsPx = measuredLineTops[blockIndex],
    fallbackStepPx = fallbackStepPx,
)

private fun IntArray?.contentEqualsSafe(other: IntArray): Boolean = this != null && contentEquals(other)

private suspend fun moveToPageAnchor(
    listState: LazyListState,
    target: ReaderPageLocation,
    smooth: Boolean,
) {
    val offset = target.blockOffsetPx.coerceAtLeast(0)
    if (smooth) {
        listState.animateScrollToItem(target.blockIndex.coerceAtLeast(0), offset)
    } else {
        listState.scrollToItem(target.blockIndex.coerceAtLeast(0), offset)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderTocSheet(
    state: ReaderUiState,
    onDismiss: () -> Unit,
    onChapter: (Int) -> Unit,
) {
    val chapters = state.outline?.chapters.orEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ModalSheetHeader(
            title = "Оглавление",
            onClose = onDismiss,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(460.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 28.dp),
        ) {
            itemsIndexed(chapters, key = { _, item -> item.sourceRef }) { index, item ->
                Column(
                    Modifier.fillMaxWidth().clickable { onChapter(index) }.padding(horizontal = 20.dp, vertical = 13.dp),
                ) {
                    Text(
                        item.title.ifBlank { "Глава ${index + 1}" },
                        fontWeight = if (index == state.chapterIndex) FontWeight.Bold else FontWeight.Normal,
                        color = if (index == state.chapterIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                    Text("${index + 1} / ${chapters.size}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReaderSettingsSheet(
    settings: ReaderSettings,
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
) {
    var panel by rememberSaveable { mutableStateOf(ReaderSettingsPanel.MAIN) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = panel == ReaderSettingsPanel.MAIN),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (panel == ReaderSettingsPanel.MAIN) {
                ModalSheetHeader(
                    title = "Чтение",
                    subtitle = "Основные параметры одинаково работают в обычном и полноэкранном режиме.",
                    onClose = onDismiss,
                )
                ReaderPreferencesControls(
                    settings = settings,
                    onUpdate = viewModel::updateSettings,
                    includeTheme = false,
                    includeProfiles = true,
                    includeNavigation = false,
                    includeText = true,
                    includeBasicText = true,
                    includeAdvancedText = false,
                )
                ReaderPreferencesControls(
                    settings = settings,
                    onUpdate = viewModel::updateSettings,
                    includeTheme = true,
                    includeProfiles = false,
                    includeNavigation = false,
                    includeText = false,
                )
                ReaderPreferencesControls(
                    settings = settings,
                    onUpdate = viewModel::updateSettings,
                    includeTheme = false,
                    includeProfiles = false,
                    includeNavigation = true,
                    includeNavigationBasics = true,
                    includeNavigationAdvanced = false,
                    includeText = false,
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { panel = ReaderSettingsPanel.ADVANCED },
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Расширенные", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Абзацы, заголовки, переносы, точные зоны тапов и анимации",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                ModalSheetHeader(
                    title = "Расширенные настройки чтения",
                    onBack = { panel = ReaderSettingsPanel.MAIN },
                    onClose = onDismiss,
                )
                ReaderPreferencesControls(
                    settings = settings,
                    onUpdate = viewModel::updateSettings,
                    includeTheme = false,
                    includeProfiles = false,
                    includeNavigation = false,
                    includeText = true,
                    includeBasicText = false,
                    includeAdvancedText = true,
                )
                ReaderPreferencesControls(
                    settings = settings,
                    onUpdate = viewModel::updateSettings,
                    includeTheme = false,
                    includeProfiles = false,
                    includeNavigation = true,
                    includeNavigationBasics = false,
                    includeNavigationAdvanced = true,
                    includeText = false,
                )
            }
        }
    }

    BackHandler(enabled = panel == ReaderSettingsPanel.ADVANCED) {
        panel = ReaderSettingsPanel.MAIN
    }
}

@Composable
private fun ReaderTextBlock(
    block: ReaderBlock.Text,
    settings: ReaderSettings,
    onOpenLink: (String, Boolean) -> Unit,
    onTextLayout: (TextLayoutResult) -> Unit,
    continuation: Boolean = false,
    sourceCharOffsetBase: Int = 0,
    sourceBlockIndex: Int = 0,
    selectedWordSourceRange: IntRange? = null,
    activeSelection: ReaderTextSelection? = null,
    annotations: List<ReaderAnnotation> = emptyList(),
    onTextRangeSelection: ((Int, Int, Int, Int, Offset, Boolean) -> Unit)? = null,
    onSelectionHandleDrag: ((Boolean, Int, Int, Offset, Boolean) -> Unit)? = null,
    onSelectionEdgeDrag: ((Boolean?, Int, Offset) -> Unit)? = null,
    onAnnotationTap: ((ReaderAnnotation, Offset) -> Unit)? = null,
    chapterTextBlocks: List<String?> = emptyList(),
) {
    val density = LocalDensity.current
    // Match the font passed to prepareReaderChapterPagination. Different fonts change
    // line breaks and row heights, so rendering and measurement MUST be identical.
    val family = if (LocalFluxThemeMode.current.isExperimentalChrome) {
        FontFamily.Monospace
    } else {
        resolveReaderFontFamily(settings.fontMode, settings.customFontPath)
    }
    val typography = MaterialTheme.typography
    val style = remember(block.role, settings, typography, family, density, continuation) {
        buildReaderTextStyle(
            block = block,
            settings = settings,
            typography = typography,
            family = family,
            density = density,
            continuation = continuation,
        )
    }
    val linkColor = MaterialTheme.colorScheme.primary
    val selectionBackground = MaterialTheme.colorScheme.secondaryContainer
    val selectionForeground = MaterialTheme.colorScheme.onSecondaryContainer
    fun textBlockAtDistance(direction: Int, steps: Int): Int? {
        if (direction == 0 || steps <= 0 || chapterTextBlocks.isEmpty()) return sourceBlockIndex
        var index = sourceBlockIndex
        repeat(steps.coerceAtMost(8)) {
            val next = if (direction < 0) {
                chapterTextBlocks.indices.take(index).lastOrNull { !chapterTextBlocks[it].isNullOrBlank() }
            } else {
                chapterTextBlocks.indices.drop(index + 1).firstOrNull { !chapterTextBlocks[it].isNullOrBlank() }
            } ?: return index.takeIf { it != sourceBlockIndex }
            index = next
        }
        return index
    }
    val annotated = remember(
        block.text, block.links, linkColor, sourceCharOffsetBase, selectedWordSourceRange, annotations,
        selectionBackground, selectionForeground,
    ) {
        buildAnnotatedString {
            append(block.text)
            annotations.forEach { annotation ->
                if (sourceBlockIndex !in annotation.startBlockIndex..annotation.endBlockIndex) return@forEach
                val sourceStart = if (sourceBlockIndex == annotation.startBlockIndex) annotation.startCharOffset else sourceCharOffsetBase
                val sourceEnd = if (sourceBlockIndex == annotation.endBlockIndex) annotation.endCharOffsetExclusive else sourceCharOffsetBase + block.text.length
                val localStart = (sourceStart - sourceCharOffsetBase).coerceIn(0, block.text.length)
                val localEnd = (sourceEnd - sourceCharOffsetBase).coerceIn(0, block.text.length)
                if (localStart < localEnd) {
                    val annotationColor = Color(annotation.colorArgb)
                    addStyle(
                        SpanStyle(
                            background = annotationColor,
                            textDecoration = if (annotation.kind == ReaderAnnotationKind.NOTE || annotation.kind == ReaderAnnotationKind.BOOKMARK) {
                                TextDecoration.Underline
                            } else null,
                        ),
                        start = localStart,
                        end = localEnd,
                    )
                }
            }
            selectedWordSourceRange?.let { selected ->
                val localStart = (selected.first - sourceCharOffsetBase).coerceIn(0, block.text.length)
                val localEnd = (selected.last + 1 - sourceCharOffsetBase).coerceIn(localStart, block.text.length)
                if (localStart < localEnd) {
                    addStyle(
                        SpanStyle(color = selectionForeground, background = selectionBackground),
                        start = localStart,
                        end = localEnd,
                    )
                }
            }
            block.links.forEach { link ->
                val start = link.start.coerceIn(0, block.text.length)
                val end = link.end.coerceIn(start, block.text.length)
                if (start < end) {
                    addStyle(
                        SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                        start = start,
                        end = end,
                    )
                }
            }
        }
    }
    val readerTextColor = MaterialTheme.colorScheme.onBackground
    val textContent: @Composable (Modifier) -> Unit = { textModifier ->
        if (block.links.isEmpty() && onTextRangeSelection == null && annotations.isEmpty()) {
            Text(
                text = block.text,
                modifier = textModifier,
                style = style,
                color = readerTextColor,
                onTextLayout = onTextLayout,
            )
        } else {
            val layoutHolder = remember(annotated) { arrayOfNulls<TextLayoutResult>(1) }
            var textCoordinates by remember(annotated) { mutableStateOf<LayoutCoordinates?>(null) }
            var rangeAnchorSourceOffset by remember(block.text, sourceCharOffsetBase) { mutableIntStateOf(sourceCharOffsetBase) }
            var rangeAnchorBlockIndex by remember(sourceBlockIndex) { mutableIntStateOf(sourceBlockIndex) }
            Box(modifier = textModifier) {
                BasicText(
                    text = annotated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { textCoordinates = it }
                        .readerTextTapAction(enabled = block.links.isNotEmpty() || annotations.isNotEmpty()) { position ->
                            val layout = layoutHolder[0] ?: return@readerTextTapAction false
                            val offset = layout.getOffsetForPosition(position).coerceIn(0, block.text.length)
                            val tappedLink = block.links.firstOrNull { offset >= it.start && offset < it.end }
                            if (tappedLink != null) {
                                onOpenLink(tappedLink.target, tappedLink.external)
                                true
                            } else {
                                val sourceOffset = sourceCharOffsetBase + offset
                                val tappedAnnotation = annotations.firstOrNull { annotation ->
                                    ReaderAnnotationHitPolicy.contains(
                                        blockIndex = sourceBlockIndex,
                                        sourceOffset = sourceOffset,
                                        startBlockIndex = annotation.startBlockIndex,
                                        startCharOffset = annotation.startCharOffset,
                                        endBlockIndex = annotation.endBlockIndex,
                                        endCharOffsetExclusive = annotation.endCharOffsetExclusive,
                                    )
                                }
                                if (tappedAnnotation != null && onAnnotationTap != null) {
                                    val windowPosition = textCoordinates?.localToWindow(position) ?: position
                                    onAnnotationTap(tappedAnnotation, windowPosition)
                                    true
                                } else {
                                    false
                                }
                            }
                        }
                        .readerTextRangeSelection(
                            enabled = onTextRangeSelection != null,
                            onStart = { position ->
                                layoutHolder[0]?.let { layout ->
                                    val offset = layout.getOffsetForPosition(position).coerceIn(0, block.text.lastIndex.coerceAtLeast(0))
                                    val source = sourceCharOffsetBase + offset
                                    rangeAnchorSourceOffset = source
                                    rangeAnchorBlockIndex = sourceBlockIndex
                                    val windowPosition = textCoordinates?.localToWindow(position) ?: position
                                    onTextRangeSelection?.invoke(sourceBlockIndex, source, sourceBlockIndex, source, windowPosition, false)
                                }
                            },
                            onDrag = { position ->
                                layoutHolder[0]?.let { layout ->
                                    val localLast = block.text.lastIndex.coerceAtLeast(0)
                                    val sameBlockOffset = layout.getOffsetForPosition(position).coerceIn(0, localLast)
                                    val blockHeight = layout.size.height.coerceAtLeast(1).toFloat()
                                    val direction = when {
                                        position.y < -12f -> -1
                                        position.y > layout.size.height + 12f -> 1
                                        else -> 0
                                    }
                                    val steps = when (direction) {
                                        -1 -> (1 + (-position.y / blockHeight).toInt()).coerceIn(1, 8)
                                        1 -> (1 + ((position.y - layout.size.height) / blockHeight).toInt()).coerceIn(1, 8)
                                        else -> 0
                                    }
                                    val edgeBlock = textBlockAtDistance(direction, steps)
                                    val (currentBlock, currentSource) = when {
                                        direction < 0 && edgeBlock != null && edgeBlock != sourceBlockIndex -> edgeBlock to 0
                                        direction > 0 && edgeBlock != null && edgeBlock != sourceBlockIndex -> {
                                            val targetText = chapterTextBlocks[edgeBlock].orEmpty()
                                            edgeBlock to targetText.lastIndex.coerceAtLeast(0)
                                        }
                                        else -> sourceBlockIndex to (sourceCharOffsetBase + sameBlockOffset)
                                    }
                                    val windowPosition = textCoordinates?.localToWindow(position) ?: position
                                    onTextRangeSelection?.invoke(rangeAnchorBlockIndex, rangeAnchorSourceOffset, currentBlock, currentSource, windowPosition, false)
                                    if (direction != 0) onSelectionEdgeDrag?.invoke(null, direction, windowPosition)
                                }
                            },
                            onEnd = { position ->
                                layoutHolder[0]?.let { layout ->
                                    val localLast = block.text.lastIndex.coerceAtLeast(0)
                                    val sameBlockOffset = layout.getOffsetForPosition(position).coerceIn(0, localLast)
                                    val blockHeight = layout.size.height.coerceAtLeast(1).toFloat()
                                    val direction = when {
                                        position.y < -12f -> -1
                                        position.y > layout.size.height + 12f -> 1
                                        else -> 0
                                    }
                                    val steps = when (direction) {
                                        -1 -> (1 + (-position.y / blockHeight).toInt()).coerceIn(1, 8)
                                        1 -> (1 + ((position.y - layout.size.height) / blockHeight).toInt()).coerceIn(1, 8)
                                        else -> 0
                                    }
                                    val edgeBlock = textBlockAtDistance(direction, steps)
                                    val (currentBlock, currentSource) = when {
                                        direction < 0 && edgeBlock != null && edgeBlock != sourceBlockIndex -> edgeBlock to 0
                                        direction > 0 && edgeBlock != null && edgeBlock != sourceBlockIndex -> {
                                            val targetText = chapterTextBlocks[edgeBlock].orEmpty()
                                            edgeBlock to targetText.lastIndex.coerceAtLeast(0)
                                        }
                                        else -> sourceBlockIndex to (sourceCharOffsetBase + sameBlockOffset)
                                    }
                                    val windowPosition = textCoordinates?.localToWindow(position) ?: position
                                    onTextRangeSelection?.invoke(rangeAnchorBlockIndex, rangeAnchorSourceOffset, currentBlock, currentSource, windowPosition, true)
                                }
                            },
                        ),
                    style = style.copy(color = readerTextColor),
                    onTextLayout = { result ->
                        layoutHolder[0] = result
                        onTextLayout(result)
                    },
                )

                val layout = layoutHolder[0]
                val selection = activeSelection
                if (layout != null && selection != null && selection.menuVisible && onSelectionHandleDrag != null) {
                    if (sourceBlockIndex == selection.startBlockIndex) {
                        val localOffset = (selection.startCharOffset - sourceCharOffsetBase)
                        if (localOffset in 0..block.text.length) {
                            ReaderSelectionHandle(
                                layout = layout,
                                localOffset = localOffset,
                                sourceOffsetBase = sourceCharOffsetBase,
                                sourceBlockIndex = sourceBlockIndex,
                                isStart = true,
                                textCoordinates = textCoordinates,
                                onDrag = onSelectionHandleDrag,
                                onEdgeDrag = onSelectionEdgeDrag,
                            )
                        }
                    }
                    if (sourceBlockIndex == selection.endBlockIndex) {
                        val localOffset = (selection.endCharOffsetExclusive - sourceCharOffsetBase)
                        if (localOffset in 0..block.text.length) {
                            ReaderSelectionHandle(
                                layout = layout,
                                localOffset = localOffset,
                                sourceOffsetBase = sourceCharOffsetBase,
                                sourceBlockIndex = sourceBlockIndex,
                                isStart = false,
                                textCoordinates = textCoordinates,
                                onDrag = onSelectionHandleDrag,
                                onEdgeDrag = onSelectionEdgeDrag,
                            )
                        }
                    }
                }
            }
        }
    }
    if (block.role == TextRole.LIST_ITEM) {
        val depth = block.listDepth.coerceIn(0, 8)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = (settings.horizontalPaddingDp + depth * 18f).dp,
                    end = settings.horizontalPaddingDp.dp,
                ),
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = block.listMarker.ifBlank { "•" },
                modifier = Modifier.width(24.dp),
                style = style.copy(textAlign = TextAlign.Center),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            textContent(Modifier.weight(1f))
        }
    } else {
        textContent(Modifier.fillMaxWidth().padding(horizontal = settings.horizontalPaddingDp.dp))
    }
}

@Composable
private fun ReaderSelectionHandle(
    layout: TextLayoutResult,
    localOffset: Int,
    sourceOffsetBase: Int,
    sourceBlockIndex: Int,
    isStart: Boolean,
    textCoordinates: LayoutCoordinates?,
    onDrag: (Boolean, Int, Int, Offset, Boolean) -> Unit,
    onEdgeDrag: ((Boolean?, Int, Offset) -> Unit)?,
) {
    val density = LocalDensity.current
    val color = MaterialTheme.colorScheme.primary
    val cursor = layout.getCursorRect(localOffset.coerceIn(0, layout.layoutInput.text.length))
    val handleSize = 18.dp
    val handleSizePx = with(density) { handleSize.roundToPx() }
    val origin = Offset(cursor.left, cursor.bottom)
    var dragDelta by remember(layout, localOffset, isStart) { mutableStateOf(Offset.Zero) }
    var lastSourceOffset by remember(layout, localOffset, sourceOffsetBase) {
        mutableIntStateOf(sourceOffsetBase + localOffset)
    }
    Box(
        Modifier
            .offset {
                IntOffset(
                    (origin.x - handleSizePx / 2f).roundToInt(),
                    (origin.y - handleSizePx / 4f).roundToInt(),
                )
            }
            .size(handleSize)
            .pointerInput(layout, localOffset, isStart, sourceOffsetBase) {
                detectDragGestures(
                    onDragStart = { dragDelta = Offset.Zero },
                    onDragEnd = {
                        val pointer = origin + dragDelta
                        val windowPosition = textCoordinates?.localToWindow(pointer) ?: pointer
                        onDrag(isStart, sourceBlockIndex, lastSourceOffset, windowPosition, true)
                    },
                    onDragCancel = { dragDelta = Offset.Zero },
                    onDrag = { change, amount ->
                        change.consume()
                        dragDelta += amount
                        val pointer = origin + dragDelta
                        val offset = layout.getOffsetForPosition(pointer).coerceIn(0, layout.layoutInput.text.length.coerceAtLeast(1) - 1)
                        lastSourceOffset = sourceOffsetBase + offset
                        val windowPosition = textCoordinates?.localToWindow(pointer) ?: pointer
                        onDrag(isStart, sourceBlockIndex, lastSourceOffset, windowPosition, false)
                        val direction = when {
                            pointer.y < -12f -> -1
                            pointer.y > layout.size.height + 12f -> 1
                            else -> 0
                        }
                        if (direction != 0) onEdgeDrag?.invoke(isStart, direction, windowPosition)
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(14.dp),
            shape = CircleShape,
            color = color,
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.background),
            shadowElevation = 2.dp,
        ) {}
    }
}

@Composable
private fun ReaderImageBlock(bookId: String, block: ReaderBlock.Image, viewModel: AppViewModel, settings: ReaderSettings) {
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = settings.horizontalPaddingDp.dp)) {
        val density = LocalDensity.current
        val targetWidthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(320)
        val bitmap by produceState<Bitmap?>(initialValue = null, key1 = bookId, key2 = block.ref, key3 = targetWidthPx) {
            value = null
            value = viewModel.loadImage(bookId, block.ref, targetWidthPx)
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = block.alt.ifBlank { null },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentScale = ContentScale.FillWidth,
            )
        } else if (block.alt.isNotBlank()) {
            Text(
                block.alt,
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ErrorPane(message: String, modifier: Modifier = Modifier, onRetry: () -> Unit) {
    Box(modifier.padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            Button(onClick = onRetry) { Text("Повторить") }
        }
    }
}

private fun blockKey(index: Int, block: ReaderBlock): String = when (block) {
    is ReaderBlock.Image -> "$index:image:${block.ref}"
    is ReaderBlock.Text -> "$index:text:${block.role}:${block.text.take(64)}"
}

