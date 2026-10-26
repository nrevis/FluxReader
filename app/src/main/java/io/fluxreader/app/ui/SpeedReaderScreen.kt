@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package io.fluxreader.app.ui

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.fluxreader.app.AppViewModel
import io.fluxreader.app.SpeedUiState
import io.fluxreader.app.data.ReaderFontMode
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.ReaderThemeMode
import io.fluxreader.app.data.useWastelandDesign
import io.fluxreader.app.data.useCyberpunkDesign
import io.fluxreader.app.data.SpeedSpeakerPosition
import io.fluxreader.app.data.SpeedSpeakerStyle
import io.fluxreader.app.data.SpeedFocusStyle
import io.fluxreader.app.data.SpeedLowerSegmentMode
import io.fluxreader.app.data.SpeedMode
import io.fluxreader.app.data.SpeedPacingMode
import io.fluxreader.app.data.SpeedProgressMode
import io.fluxreader.app.reader.DialogueReply
import io.fluxreader.app.reader.DialogueReplyLocator
import io.fluxreader.app.reader.SpeedDialoguePlayback
import io.fluxreader.app.reader.SpeedDialoguePresentation
import io.fluxreader.app.reader.SpeedDialogueAnalyzer
import io.fluxreader.app.reader.SpeedReaderCursor
import io.fluxreader.app.reader.SpeedSpeechCheckpointPolicy
import io.fluxreader.app.reader.SpeedSpeechNarrator
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import io.fluxreader.app.reader.SpeedMediaCue
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.fluxreader.app.reader.DialogueTextColoring
import io.fluxreader.app.reader.SpeedChunk
import kotlin.math.min
import kotlin.math.roundToInt

private fun traceSpeedTiming(event: String, state: SpeedUiState, timeMs: Long, requestedMs: Long, settings: ReaderSettings) {
    if (io.fluxreader.app.BuildConfig.DEBUG && System.getProperty("fluxreader.speedTimingTrace") == "true") {
        android.util.Log.d("FluxSpeedTiming", "$event|$timeMs|${state.chapterIndex}|${state.chunk?.startOffset}|${state.chunk?.endOffset}|$requestedMs|${settings.speedWpm}|${settings.speedComplexWordPauseMs}|${settings.speedPacingMode}")
    }
}

private enum class SpeedSettingsPanel { NONE, HUB, DISPLAY, BEHAVIOR, VOICE, EFFECTS, ADVANCED, DIALOGUE, COLORS, COMPLEX_WORDS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedReaderScreen(
    state: SpeedUiState,
    settings: ReaderSettings,
    viewModel: AppViewModel,
    title: String,
    onBack: () -> Unit,
    workspaceEmbedded: Boolean = false,
    onImmersiveChanged: (Boolean) -> Unit = {},
) {
    val appScheme = MaterialTheme.colorScheme
    val pal = LocalExperimentalColors.current
    val wastelandTheme = settings.useWastelandDesign
    val cyberTheme = settings.useCyberpunkDesign
    val background = when {
        wastelandTheme -> pal.background
        cyberTheme -> pal.background
        settings.speedUseReaderTheme -> appScheme.background
        else -> Color(settings.speedBackgroundColorArgb)
    }
    val textColor = when {
        wastelandTheme -> pal.text
        cyberTheme -> pal.text
        settings.speedUseReaderTheme -> appScheme.onBackground
        else -> Color(settings.speedTextColorArgb)
    }
    val focusColor = when {
        wastelandTheme -> pal.accent
        cyberTheme -> pal.accent
        settings.speedUseReaderTheme -> appScheme.primary
        else -> Color(settings.speedFocusColorArgb)
    }
    val guideColor = when {
        wastelandTheme -> pal.accent.copy(alpha = 0.70f)
        cyberTheme -> pal.secondary.copy(alpha = 0.72f)
        settings.speedUseReaderTheme -> appScheme.onSurfaceVariant.copy(alpha = 0.42f)
        else -> Color(settings.speedGuideColorArgb)
    }
    var settingsPanel by remember { mutableStateOf(SpeedSettingsPanel.NONE) }
    var speakerEditorReply by remember(state.bookId, state.chapterIndex) { mutableStateOf<DialogueReply?>(null) }
    var isFullscreen by rememberSaveable { mutableStateOf(false) }
    var speedTuningExpanded by rememberSaveable { mutableStateOf(false) }
    var introVisible by remember { mutableStateOf(false) }
    var introChapterIndex by rememberSaveable(state.bookId) { mutableIntStateOf(-1) }
    var playStartedAtMs by remember { mutableLongStateOf(0L) }
    val localView = LocalView.current
    val context = LocalContext.current
    val speech = remember(context, state.bookId) { SpeedSpeechNarrator(context.applicationContext) }
    val speechStatus by speech.status.collectAsState()
    var voiceMode by rememberSaveable(state.bookId) { mutableStateOf(false) }
    val phraseVoice = voiceMode && io.fluxreader.app.reader.BuiltInVoiceCatalog.isBuiltIn(settings.narrationVoiceId)
    val visualChunk = if (voiceMode && state.chapterText.isNotBlank()) {
        if (phraseVoice) io.fluxreader.app.reader.SpeedSpeechText.next(state.chapterText, speechStatus.offset, 260)
            ?.let(io.fluxreader.app.reader.SpeedSpeechText::displayChunk)
        else SpeedReaderCursor.next(state.chapterText, speechStatus.offset, 1)
    } else null
    val displayState = if (voiceMode && visualChunk != null) {
        state.copy(chunk = visualChunk, playing = speechStatus.playing)
    } else state
    val showShortDialogue = !voiceMode && settings.speedDialogueEnabled && settings.speedDialogueShortReplies &&
        settings.speedMode == SpeedMode.RSVP && (displayState.chunk?.tokens?.size ?: 0) > 1
    val displaySettings = when {
        phraseVoice -> settings.copy(speedMode = SpeedMode.PHRASE, speedMaxLines = 5,
            speedFixedFontSize = false, speedAutoFitText = true)
        voiceMode -> settings.copy(speedMode = SpeedMode.RSVP)
        showShortDialogue -> settings.copy(
            speedMode = SpeedMode.PHRASE,
            speedFixedFontSize = false,
            speedAutoFitText = true,
        )
        else -> settings
    }

    // TTS callbacks are valid checkpoints ONLY during an explicitly enabled voice session.
    // A freshly-created narrator starts at offset 0: applying that offset while loading
    // a saved Speed Reader chapter would overwrite the saved word on every entry.
    speech.onSettledPosition = { at ->
        if (SpeedSpeechCheckpointPolicy.acceptNarratorOffset(voiceMode, state.chapterText.length, at)) {
            viewModel.commitSpeechPosition(state.bookId, state.chapterIndex, at)
        }
    }
    speech.onFinished = {
        if (voiceMode && state.chapterText.isNotEmpty()) {
            viewModel.commitSpeechPosition(state.bookId, state.chapterIndex, state.chapterText.lastIndex.coerceAtLeast(0))
        }
    }
    LaunchedEffect(settings.narrationVoiceId, settings.narrationRate, settings.narrationPitch) {
        speech.configure(settings)
    }
    // Chapter/book switches must discard the old TTS cursor WITHOUT committing it.
    // pause() emits a settled-position event and previously saved offset 0 here.
    LaunchedEffect(state.bookId, state.chapterIndex) {
        speech.setPosition(state.chunk?.startOffset ?: 0)
        voiceMode = false
    }
    LaunchedEffect(state.chunk?.startOffset) {
        if (voiceMode && !speech.status.value.playing) speech.setPosition(state.chunk?.startOffset ?: 0)
    }
    LaunchedEffect(voiceMode, speechStatus.playing, speechStatus.offset, state.bookId, state.chapterIndex) {
        if (speechStatus.playing && SpeedSpeechCheckpointPolicy.acceptNarratorOffset(voiceMode, state.chapterText.length, speechStatus.offset)) {
            viewModel.commitSpeechPosition(state.bookId, state.chapterIndex, speechStatus.offset)
        }
    }
    val leaveSpeed: () -> Unit = {
        // Commit speech before changing the route, while this session still owns the cursor.
        if (voiceMode) speech.pause()
        viewModel.speedPause()
        onBack()
    }
    BackHandler(enabled = !isFullscreen && settingsPanel == SpeedSettingsPanel.NONE) { leaveSpeed() }

    val togglePlayback: () -> Unit = {
        if (voiceMode) {
            if (speechStatus.playing) speech.pause()
            else {
                viewModel.speedPause()
                speech.start(state.chapterText, speechStatus.offset.coerceIn(0, state.chapterText.length))
            }
        } else {
            viewModel.speedToggle()
        }
    }
    LaunchedEffect(voiceMode, speechStatus.error) {
        if (voiceMode) speechStatus.error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
        }
    }
    val toggleVoice: () -> Unit = {
        if (voiceMode) {
            speech.pause()
            voiceMode = false
        } else {
            viewModel.speedPause()
            speech.setPosition(state.chunk?.startOffset ?: 0)
            voiceMode = true
            if (!speechStatus.ready) settingsPanel = SpeedSettingsPanel.VOICE
        }
    }
    val editSpeaker: () -> Unit = {
        if (voiceMode) speech.pause()
        viewModel.speedPause()
        displayState.chunk?.let { chunk ->
            DialogueReplyLocator.at(state.chapterText, chunk.startOffset)?.let {
                settingsPanel = SpeedSettingsPanel.NONE
                speakerEditorReply = it
            }
        }
    }
    val previousSentence: () -> Unit = {
        if (voiceMode) speech.pause()
        viewModel.speedPreviousSentence()
    }
    val nextSentence: () -> Unit = {
        if (voiceMode) speech.pause()
        viewModel.speedNextSentence()
    }

    // Speed reading is an active reading session: never let Android dim/lock the display here.
    DisposableEffect(localView) {
        val previousKeepScreenOn = localView.keepScreenOn
        localView.keepScreenOn = true
        onDispose { localView.keepScreenOn = previousKeepScreenOn }
    }

    BackHandler(enabled = isFullscreen) { isFullscreen = false }
    FullscreenSystemBarsEffect(isFullscreen)
    LaunchedEffect(isFullscreen) { onImmersiveChanged(isFullscreen) }
    DisposableEffect(onImmersiveChanged) {
        onDispose { onImmersiveChanged(false) }
    }

    LaunchedEffect(state.playing) {
        playStartedAtMs = if (state.playing) SystemClock.elapsedRealtime() else 0L
    }

    LaunchedEffect(state.activeMedia?.offset, settings.speedImagePauseMs) {
        if (state.activeMedia != null) {
            delay(settings.speedImagePauseMs.toLong())
            viewModel.speedDismissMedia(resume = true)
        }
    }

    LaunchedEffect(state.bookId, state.chapterIndex, state.loading, state.chunk != null, settings.speedChapterIntroMs, voiceMode) {
        if (!voiceMode && !state.loading && state.chunk != null && state.chapterIndex != introChapterIndex && settings.speedChapterIntroMs > 0) {
            introChapterIndex = state.chapterIndex
            introVisible = true
            try {
                delay(settings.speedChapterIntroMs.toLong())
            } finally {
                introVisible = false
            }
        }
    }

    DisposableEffect(speech) {
        onDispose {
            // The TTS word cursor can be ahead of the visual SpeedUiState chunk.
            // Stop speech first so onSettledPosition commits the spoken word,
            // then flush the updated Speed Reader checkpoint before leaving.
            if (speech.status.value.playing) speech.pause()
            viewModel.speedPause()
            speech.close()
        }
    }

    val fixationKey = remember(state.bookId, state.chapterIndex, state.chunk?.startOffset, state.chunk?.endOffset,
        state.playing, introVisible, voiceMode, state.activeMedia, state.loading, state.error) { Any() }
    var chunkShownAtMs by remember(fixationKey) { mutableLongStateOf(0L) }
    LaunchedEffect(fixationKey) {
        if (state.playing && !voiceMode && !introVisible && !state.loading && state.error == null && state.activeMedia == null) {
            androidx.compose.runtime.withFrameNanos { }
            chunkShownAtMs = SystemClock.elapsedRealtime()
            traceSpeedTiming("shown", state, chunkShownAtMs, 0L, settings)
        }
    }
    LaunchedEffect(
        fixationKey, chunkShownAtMs,
        state.bookId, state.loading, state.error, state.activeMedia, introVisible, voiceMode,
        state.playing,
        state.chapterIndex,
        state.chunk?.startOffset,
        state.chunk?.endOffset,
        playStartedAtMs,
        settings.speedWpm,
        settings.speedPacingMode,
        settings.speedParagraphPause,
        settings.speedMicroPausesEnabled,
        settings.speedSentencePauseMs,
        settings.speedClausePauseMs,
        settings.speedParagraphPauseMs,
        settings.speedAdaptiveSentencePause,
        settings.speedSentenceLengthBonusMs,
        settings.speedComplexWordPauseMs,
        settings.speedGradualAcceleration,
        settings.speedAccelerationSeconds,
        settings.speedAccelerationStartPercent,
        settings.speedDialogueEnabled,
        settings.speedDialoguePauseEnabled,
        settings.speedDialoguePauseMs,
        state.chapterText,
    ) {
        val chunk = state.chunk
        if (chunkShownAtMs > 0L && state.playing && !voiceMode && !introVisible && state.activeMedia == null && chunk != null && !state.loading && state.error == null) {
            val elapsed = if (playStartedAtMs > 0L) (SystemClock.elapsedRealtime() - playStartedAtMs).coerceAtLeast(0L) else 0L
            val effectiveWpm = SpeedReaderCursor.rampedWpm(
                targetWpm = settings.speedWpm,
                elapsedMs = elapsed,
                enabled = settings.speedGradualAcceleration && settings.speedPacingMode != SpeedPacingMode.CONSTANT,
                startPercent = settings.speedAccelerationStartPercent,
                durationSeconds = settings.speedAccelerationSeconds,
            )
            val dialoguePause = if (settings.speedDialogueEnabled && settings.speedDialoguePauseEnabled &&
                SpeedDialogueAnalyzer.isReplyStart(state.chapterText, chunk.startOffset)) {
                settings.speedDialoguePauseMs.toLong()
            } else 0L
            val durationMs = SpeedReaderCursor.delayMillis(
                    wpm = effectiveWpm,
                    chunk = chunk,
                    chapterText = state.chapterText,
                    pacingMode = settings.speedPacingMode,
                    paragraphPause = settings.speedParagraphPause,
                    microPausesEnabled = settings.speedMicroPausesEnabled,
                    sentencePauseMs = settings.speedSentencePauseMs,
                    clausePauseMs = settings.speedClausePauseMs,
                    paragraphPauseMs = settings.speedParagraphPauseMs,
                    adaptiveSentencePause = settings.speedAdaptiveSentencePause,
                    sentenceLengthBonusMs = settings.speedSentenceLengthBonusMs,
                    complexWordPauseMs = settings.speedComplexWordPauseMs,
                ) + dialoguePause
            delay((durationMs - (SystemClock.elapsedRealtime() - chunkShownAtMs)).coerceAtLeast(0L))
            traceSpeedTiming("advance", state, SystemClock.elapsedRealtime(), durationMs, settings)
            viewModel.speedAdvance(state.bookId, state.chapterIndex, chunk.startOffset, chunk.endOffset)
        }
    }

    if (isFullscreen) {
        // Fullscreen has distinct, measured progress / viewport / controls regions.
        // Nothing is drawn on top of the interactive context preview.
        Column(
            // The system bars are hidden. Reserve only the actual top camera
            // cutout, never the full safeDrawing/status/navigation band.
            Modifier.fillMaxSize().background(background)
                .padding(DeviceEdgeClearance.immersivePadding())
        ) {
            if (settings.speedShowProgress) {
                SpeedProgress(displayState, displaySettings, focusColor, textColor)
            }
            SpeedViewport(
                state = displayState,
                settings = displaySettings,
                textColor = textColor,
                focusColor = focusColor,
                guideColor = guideColor,
                viewModel = viewModel,
                introVisible = introVisible,
                onRetry = viewModel::speedRetry,
                onEditSpeaker = editSpeaker,
                immersiveMode = true,
                onTogglePlayback = togglePlayback,
                onToggleFullscreen = { isFullscreen = false },
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FullscreenSpeedButton(onClick = toggleVoice, active = voiceMode, color = textColor,
                    icon = Icons.Default.RecordVoiceOver,
                    description = if (voiceMode) "Выключить озвучку" else "Включить озвучку")
                FullscreenSpeedButton(onClick = togglePlayback, active = true, color = focusColor,
                    icon = if (displayState.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    description = if (displayState.playing) "Пауза" else "Старт")
                FullscreenSpeedButton(onClick = { isFullscreen = false }, active = false, color = focusColor,
                    icon = Icons.Default.FullscreenExit, description = "Свернуть полный экран")
            }
        }
    } else {
        // Wasteland/Cyberpunk device frames already consume safeDrawing.  The
        // standard theme has no such ancestor; give its Scaffold the *same*
        // safe viewport before measuring the shared 56 dp Speed Reader bar.
        // Do not apply this in fullscreen: Android bars are hidden there.
        val standaloneSafeArea = if (!workspaceEmbedded && !wastelandTheme && !cyberTheme) {
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing)
        } else Modifier
        Scaffold(
            modifier = Modifier.fillMaxSize().then(standaloneSafeArea),
            topBar = {
                val section = state.outline?.chapters?.getOrNull(state.chapterIndex)?.title.orEmpty()
                if (workspaceEmbedded) {
                    Row(
                        Modifier.fillMaxWidth().height(48.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { viewModel.speedPause(); settingsPanel = SpeedSettingsPanel.HUB }) {
                            Icon(Icons.Default.Tune, contentDescription = "Настройки скорочтения")
                        }
                    }
                } else {
                    // Shared compact layout for all palettes. The parent above
                    // handles system bars only for the standard, standalone UI.
                    val themedChrome = settings.useWastelandDesign || settings.useCyberpunkDesign
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)
                            .background(background).padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = leaveSpeed, modifier = Modifier.size(48.dp)) {
                            if (themedChrome) {
                                FluxThemedIcon(FluxIconRole.BACK, Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Назад", modifier = Modifier.size(22.dp))
                            } else {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад",
                                    tint = textColor, modifier = Modifier.size(22.dp))
                            }
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(title.ifBlank { "Скорочтение" }, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = textColor.copy(alpha = .62f), style = MaterialTheme.typography.labelSmall)
                            Text(section.ifBlank { "Скорочтение" }, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                color = textColor, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        }
                        IconButton(
                            onClick = { viewModel.speedPause(); settingsPanel = SpeedSettingsPanel.HUB },
                            modifier = Modifier.size(48.dp),
                        ) {
                            if (themedChrome) {
                                FluxThemedIcon(FluxIconRole.SPEED, Icons.Default.Tune,
                                    contentDescription = "Настройки скорочтения", modifier = Modifier.size(22.dp))
                            } else {
                                Icon(Icons.Default.Tune, contentDescription = "Настройки скорочтения",
                                    tint = textColor, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            },
            containerColor = background,
        ) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding).background(background),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (settings.speedShowProgress) SpeedProgress(displayState, displaySettings, focusColor, textColor)
                SpeedViewport(
                    state = displayState,
                    settings = displaySettings,
                    textColor = textColor,
                    focusColor = focusColor,
                    guideColor = guideColor,
                    viewModel = viewModel,
                    introVisible = introVisible,
                    onRetry = viewModel::speedRetry,
                    onEditSpeaker = editSpeaker,
                    onTogglePlayback = togglePlayback,
                    onToggleFullscreen = { isFullscreen = !isFullscreen },
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                )

                if (state.error == null && state.chunk != null) {
                    PlaybackControls(displayState, viewModel, textColor, focusColor,
                        showFullscreen = true,
                        onFullscreen = { isFullscreen = true },
                        voiceEnabled = voiceMode,
                        onTogglePlay = togglePlayback,
                        onPrevious = previousSentence,
                        onNext = nextSentence,
                        onNarration = toggleVoice)
                    if (!voiceMode) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 34.dp),
                            horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                            Text("${settings.speedWpm} WPM  ·  ${if (speedTuningExpanded) "Скрыть настройки" else "Настроить скорость"}",
                                modifier = Modifier.clickable { speedTuningExpanded = !speedTuningExpanded }.padding(horizontal = 12.dp, vertical = 6.dp),
                                textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelMedium, color = focusColor)
                        }
                        if (speedTuningExpanded) SpeedControls(settings, viewModel, textColor)
                    }
                }
            }
        }
    }

    LaunchedEffect(settingsPanel) {
        if (settingsPanel == SpeedSettingsPanel.VOICE) speech.loadVoices()
    }

    when (settingsPanel) {
        SpeedSettingsPanel.NONE -> Unit
        SpeedSettingsPanel.HUB -> SpeedSettingsHub(
            settings = settings,
            viewModel = viewModel,
            onOpen = { settingsPanel = it },
            onDismiss = { settingsPanel = SpeedSettingsPanel.NONE },
        )
        SpeedSettingsPanel.DISPLAY -> SpeedDisplaySheet(
            settings = settings,
            viewModel = viewModel,
            onBack = { settingsPanel = SpeedSettingsPanel.HUB },
            onDismiss = { settingsPanel = SpeedSettingsPanel.NONE },
        )
        SpeedSettingsPanel.COMPLEX_WORDS -> SpeedComplexWordSheet(settings, viewModel::updateSettings,
            onBack = { settingsPanel = SpeedSettingsPanel.HUB }, onDismiss = { settingsPanel = SpeedSettingsPanel.NONE })
        SpeedSettingsPanel.BEHAVIOR -> SpeedBehaviorSheet(
            settings = settings,
            viewModel = viewModel,
            onBack = { settingsPanel = SpeedSettingsPanel.HUB },
            onDismiss = { settingsPanel = SpeedSettingsPanel.NONE },
        )
        SpeedSettingsPanel.DIALOGUE -> SpeedDialogueSheet(
            onEditSpeaker = editSpeaker,
            onColors = { settingsPanel = SpeedSettingsPanel.COLORS },
            settings = settings,
            viewModel = viewModel,
            onBack = { settingsPanel = SpeedSettingsPanel.HUB },
            onDismiss = { settingsPanel = SpeedSettingsPanel.NONE },
        )
        SpeedSettingsPanel.COLORS -> SpeakerColorsSheet(state.speakerColors, settings,
            onColor = viewModel::setSpeakerColor,
            onAutomatic = { enabled -> viewModel.updateSettings { it.copy(speedSpeakerColor = if (enabled) io.fluxreader.app.data.SpeedSpeakerColor.CHARACTER else io.fluxreader.app.data.SpeedSpeakerColor.THEME) } },
            onBack = { settingsPanel = SpeedSettingsPanel.DIALOGUE }, onDismiss = { settingsPanel = SpeedSettingsPanel.NONE })
        SpeedSettingsPanel.VOICE -> SpeedVoiceSettingsSheet(
            settings = settings,
            voices = speechStatus.voices,
            voiceReady = speechStatus.ready,
            preparing = speechStatus.preparing,
            voiceError = speechStatus.error,
            onPreview = speech::preview,
            viewModel = viewModel,
            onBack = { settingsPanel = SpeedSettingsPanel.HUB },
            onDismiss = { settingsPanel = SpeedSettingsPanel.NONE },
        )
        SpeedSettingsPanel.EFFECTS -> ExperimentalEffectsSheet(
            settings = settings,
            onUpdate = viewModel::updateSettings,
            onBack = { settingsPanel = SpeedSettingsPanel.HUB },
            onDismiss = { settingsPanel = SpeedSettingsPanel.NONE },
        )
        SpeedSettingsPanel.ADVANCED -> SpeedAdvancedSheet(
            settings = settings,
            viewModel = viewModel,
            onBack = { settingsPanel = SpeedSettingsPanel.HUB },
            onDismiss = { settingsPanel = SpeedSettingsPanel.NONE },
        )
    }

    speakerEditorReply?.let { reply ->
        val automatic = state.dialogueIndex?.at(reply.start) ?: SpeedDialogueAnalyzer.at(state.chapterText, reply.start)
        val current = DialogueReplyLocator.resolve(state.chapterText, reply.start,
            state.dialogueIndex, state.dialogueCorrections)
        val candidates = remember(state.dialogueIndex, state.dialogueCorrections) {
            (state.dialogueIndex?.entries.orEmpty().mapNotNull { it.span.speaker } +
                state.dialogueCorrections.mapNotNull { it.speaker }).distinct().sorted()
        }
        DialogueSpeakerEditor(current?.speaker, automatic?.speaker, candidates,
            onApply = { name, auto -> viewModel.correctDialogueSpeaker(reply, name, auto); speakerEditorReply = null },
            settings = settings, colors = state.speakerColors, onColor = viewModel::setSpeakerColor,
            onDismiss = { speakerEditorReply = null })
    }

    BackHandler(enabled = settingsPanel != SpeedSettingsPanel.NONE) {
        settingsPanel = when (settingsPanel) {
            SpeedSettingsPanel.HUB -> SpeedSettingsPanel.NONE
            SpeedSettingsPanel.COLORS -> SpeedSettingsPanel.DIALOGUE
            SpeedSettingsPanel.DISPLAY,
            SpeedSettingsPanel.BEHAVIOR,
            SpeedSettingsPanel.COMPLEX_WORDS,
            SpeedSettingsPanel.VOICE,
            SpeedSettingsPanel.EFFECTS,
            SpeedSettingsPanel.ADVANCED,
            SpeedSettingsPanel.DIALOGUE -> SpeedSettingsPanel.HUB
            SpeedSettingsPanel.NONE -> SpeedSettingsPanel.NONE
        }
    }
}

@Composable
private fun FullscreenSpeedButton(
    onClick: () -> Unit,
    active: Boolean,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
) {
    val themed = LocalFluxThemeMode.current.isExperimentalChrome
    val shape = if (themed) MaterialTheme.shapes.medium else CircleShape
    Surface(color = color.copy(alpha = if (active) .14f else .055f), shape = shape,
        border = BorderStroke(1.dp, color.copy(alpha = if (active) .65f else .28f))) {
        IconButton(onClick = onClick, modifier = Modifier.size(64.dp)) {
            Icon(icon, contentDescription = description, tint = color, modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun SpeedViewport(
    state: SpeedUiState,
    settings: ReaderSettings,
    textColor: Color,
    focusColor: Color,
    guideColor: Color,
    viewModel: AppViewModel,
    introVisible: Boolean,
    onRetry: () -> Unit,
    onEditSpeaker: (() -> Unit)? = null,
    immersiveMode: Boolean = false,
    onTogglePlayback: () -> Unit = viewModel::speedToggle,
    onToggleFullscreen: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val currentTogglePlayback by rememberUpdatedState(onTogglePlayback)
    fun applyTapAction(action: SpeedTapAction) {
        when (action) {
            SpeedTapAction.NONE -> Unit
            SpeedTapAction.TOGGLE_PLAYBACK -> currentTogglePlayback()
            SpeedTapAction.TOGGLE_FULLSCREEN -> onToggleFullscreen()
            SpeedTapAction.DISMISS_MEDIA -> viewModel.speedDismissMedia(resume = true)
        }
    }
    BoxWithConstraints(
        modifier
            .padding(horizontal = 20.dp)
            .pointerInput(state.activeMedia != null, settings.speedTapToToggle, immersiveMode) {
                detectTapGestures(
                    onTap = {
                        applyTapAction(
                            SpeedTapPolicy.singleTap(
                                activeMedia = state.activeMedia != null,
                                tapToToggle = settings.speedTapToToggle,
                                contentInteractive = state.chunk != null && !state.loading && state.error == null && !introVisible,
                            )
                        )
                    },
                    onDoubleTap = {
                        applyTapAction(
                            SpeedTapPolicy.doubleTap(
                                activeMedia = state.activeMedia != null,
                                contentInteractive = state.chunk != null && !state.loading && state.error == null && !introVisible,
                            )
                        )
                    },
                )
            }
    ) {
        val density = LocalDensity.current
        val dialogue = remember(state.chapterText, state.chunk?.startOffset, state.dialogueIndex, state.dialogueCorrections,
            settings.speedDialogueEnabled, settings.speedDialogueBookAnalysis) {
            if (settings.speedDialogueEnabled) state.chunk?.let {
                DialogueReplyLocator.resolve(state.chapterText, it.startOffset,
                    state.dialogueIndex, state.dialogueCorrections)
            } else null
        }
        val colorInput = remember(state.chapterText, state.dialogueIndex, state.dialogueCorrections, state.speakerColors,
            settings.speedDialogueEnabled, settings.speedDialogueTextColor) { Any() }
        val colorSnapshot by produceState<Pair<Any, DialogueTextColoring>?>(null, colorInput) {
            value = if (settings.speedDialogueEnabled && settings.speedDialogueTextColor) withContext(Dispatchers.Default) {
                colorInput to DialogueTextColoring(state.chapterText, state.dialogueIndex, state.dialogueCorrections, state.speakerColors)
            } else null
        }
        val speechColoring = colorSnapshot?.takeIf { it.first === colorInput }?.second
        val speechBackground = when {
            settings.useWastelandDesign || settings.useCyberpunkDesign -> LocalExperimentalColors.current.background
            settings.speedUseReaderTheme -> MaterialTheme.colorScheme.background
            else -> Color(settings.speedBackgroundColorArgb)
        }
        val themedPanel = LocalExperimentalColors.current.panel.takeIf { settings.useWastelandDesign || settings.useCyberpunkDesign }
        val speakerColor: (String) -> Color = { name ->
            speedSpeakerReadingInk(name, settings, speechBackground, focusColor, state.speakerColors.forName(name), themedPanel)
        }
        val mainTextHeightPx = with(density) {
            (settings.speedFontSizeSp * settings.speedLineHeightMultiplier).sp.toPx() * settings.speedMaxLines.coerceAtLeast(1)
        }
        val mainHalfHeightPx = mainTextHeightPx / 2f
        val segmentFontSp = (settings.speedFontSizeSp * 0.28f).coerceIn(13f, 20f)
        val persistentHeightPx = if (settings.speedLowerSegmentMode == SpeedLowerSegmentMode.OFF) 0f else with(density) {
            (segmentFontSp * 1.30f).sp.toPx() * 2f + 10.dp.toPx()
        }
        // Reserve pause context even while playback is running so pressing Pause never moves the
        // fixation point. The active word remains spatially stable in every UI state.
        val pauseContextHeightPx = if (
            settings.speedPauseContextEnabled && settings.speedLowerSegmentMode != SpeedLowerSegmentMode.CONTEXT
        ) with(density) { 21.sp.toPx() * 2f + 12.dp.toPx() } else 0f
        // Reserve the pause preview even while playback is running. This keeps the ORP perfectly
        // stationary when a tap pauses reading and prevents the full-text card from covering it.
        val pausePreviewReservePx = if (settings.speedShowPausePreview) with(density) {
            (if (immersiveMode) 110.dp else 140.dp).toPx()
        } else 0f
        // Reserve the same name slot throughout a chapter: names appearing/disappearing must
        // never move the fixation. Two lines accommodate long names and system font scaling.
        val speakerMeasurer = rememberTextMeasurer()
        val speakerStyle = speedSpeakerTextStyle(settings)
        val speakerTextHeight = speakerMeasurer.measure(AnnotatedString("Аг\nАг"), style = speakerStyle, maxLines = 2).size.height
        val speakerSlotPx = if (settings.speedDialogueEnabled && (settings.speedDialogueNames || settings.speedDialogueIndicator)) with(density) {
            // Measure two actual lines: Android's nonlinear font scaling means 100.sp is not
            // necessarily twice 50.sp. Multiplying before conversion truncated the second line.
            speakerTextHeight + (18f + if (settings.speedSpeakerStyle == SpeedSpeakerStyle.BUBBLE) 8f else 0f).dp.toPx()
        } else 0f
        val speakerReservePx = if (speakerSlotPx > 0f) speakerSlotPx + with(density) { settings.speedSpeakerGapDp.dp.toPx() } else 0f
        val speakerBelow = settings.speedSpeakerPosition == SpeedSpeakerPosition.BELOW
        val speakerLowerPx = if (speakerBelow) speakerReservePx else 0f
        val topClearancePx = with(density) { 24.dp.toPx() }
        val lowerSegmentReservePx = persistentHeightPx + pauseContextHeightPx + with(density) {
            (if (persistentHeightPx > 0f || pauseContextHeightPx > 0f) 22.dp else 0.dp).toPx()
        }
        val lowerContentReservePx = maxOf(lowerSegmentReservePx + speakerLowerPx, pausePreviewReservePx + speakerLowerPx)
        val viewportHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
        val effectiveVerticalPosition = SpeedViewportGeometry.clampFixationFraction(
            requestedFraction = settings.speedVerticalPosition,
            viewportHeightPx = viewportHeightPx,
            mainHalfHeightPx = mainHalfHeightPx,
            edgeClearancePx = topClearancePx,
            upperContentReservePx = if (speakerBelow) 0f else speakerReservePx,
            lowerContentReservePx = lowerContentReservePx,
        )
        val yOffset = maxHeight * (effectiveVerticalPosition - 0.5f)
        val mainHalfHeightDp = with(density) { mainHalfHeightPx.toDp() }
        val persistentHeightDp = with(density) { persistentHeightPx.toDp() }
        val pauseContextHeightDp = with(density) { pauseContextHeightPx.toDp() }
        val speakerLowerDp = with(density) { speakerLowerPx.toDp() }
        val persistentOffset = mainHalfHeightDp + speakerLowerDp + 22.dp + persistentHeightDp * 0.5f
        val pauseContextOffset = when (settings.speedLowerSegmentMode) {
            SpeedLowerSegmentMode.OFF -> mainHalfHeightDp + speakerLowerDp + 22.dp + pauseContextHeightDp * 0.5f
            SpeedLowerSegmentMode.CURRENT -> persistentOffset + persistentHeightDp * 0.5f + 12.dp + pauseContextHeightDp * 0.5f
            SpeedLowerSegmentMode.CONTEXT -> persistentOffset
        }

        val pinnedRsvp = settings.speedMode == SpeedMode.RSVP && state.chunk?.tokens?.size == 1
        var measuredFocusFraction by remember(state.chunk, settings.speedMode, settings.speedTextWidthFraction,
            settings.speedFontSizeSp, settings.speedMaxLines) { mutableStateOf<Float?>(null) }
        val guideFraction = if (pinnedRsvp) SpeedViewportGeometry.clampHorizontalFocusFraction(settings.speedHorizontalFocusFraction)
            else measuredFocusFraction ?: .5f
        if (settings.speedShowGuide && state.chunk != null && state.error == null && state.activeMedia == null && !introVisible) {
            SpeedFocusGuide(
                settings = settings,
                guideColor = guideColor,
                focusPosition = effectiveVerticalPosition,
                horizontalFocusPosition = guideFraction,
                mainHalfHeightPx = mainHalfHeightPx,
                upperRoomDp = if (speakerReservePx > 0f && !speakerBelow) settings.speedSpeakerGapDp else 160f,
                lowerRoomDp = if (speakerReservePx > 0f && speakerBelow) settings.speedSpeakerGapDp
                    else if (lowerContentReservePx > 0f) 22f else 160f,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (settings.speedFocusBandEnabled && state.chunk != null && state.error == null && state.activeMedia == null && !introVisible) {
            val bandHeight = with(density) { mainTextHeightPx.toDp() } + 34.dp
            Box(
                Modifier
                    .align(Alignment.Center)
                    .offset(y = yOffset)
                    .fillMaxWidth(settings.speedTextWidthFraction)
                    .height(bandHeight)
                    .clip(RoundedCornerShape(24.dp))
                    .background(focusColor.copy(alpha = settings.speedFocusBandAlpha)),
            )
        }
        when {
            state.activeMedia != null -> SpeedMediaFrame(
                bookId = state.bookId.orEmpty(),
                cue = state.activeMedia,
                viewModel = viewModel,
                textColor = textColor,
                focusColor = focusColor,
                displayMs = settings.speedImagePauseMs,
                modifier = Modifier.fillMaxSize(),
            )
            introVisible -> SpeedChapterIntro(
                chapterIndex = state.chapterIndex,
                title = state.headingCues
                    .distinctBy { it.text.trim().lowercase() }
                    .take(2)
                    .joinToString("\n") { it.text.trim() }
                    .ifBlank { state.outline?.chapters?.getOrNull(state.chapterIndex)?.title.orEmpty() },
                textColor = textColor,
                focusColor = focusColor,
                modifier = Modifier.fillMaxSize(),
            )
            state.loading -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = focusColor)
                Spacer(Modifier.height(14.dp))
                Text(
                    state.status ?: "Готовлю текст…",
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }
            state.error != null -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error, color = focusColor, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onRetry) { Text("Повторить") }
            }
            state.chunk != null -> {
                val chunk = state.chunk
                val dialogueLabel = SpeedDialoguePresentation.label(dialogue, settings.speedDialogueNames, settings.speedDialogueIndicator)
                if (dialogueLabel != null) {
                    val slotDp = with(density) { speakerSlotPx.toDp() }
                    val labelOffset = mainHalfHeightDp + settings.speedSpeakerGapDp.dp + slotDp / 2
                    Box(Modifier.align(Alignment.Center).fillMaxWidth().height(slotDp)
                        .offset(y = yOffset + if (speakerBelow) labelOffset else -labelOffset),
                        contentAlignment = if (speakerBelow) Alignment.TopCenter else Alignment.BottomCenter) {
                        SpeedSpeakerLabel(dialogueLabel.text, settings, namedSpeaker = dialogueLabel.namedSpeaker, background = when {
                            settings.useWastelandDesign || settings.useCyberpunkDesign -> LocalExperimentalColors.current.background
                            settings.speedUseReaderTheme -> MaterialTheme.colorScheme.background
                            else -> Color(settings.speedBackgroundColorArgb)
                        }, accent = focusColor, onClick = onEditSpeaker, characterColor = state.speakerColors.forName(dialogueLabel.text),
                            modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
                SpeedChunkText(
                    onFocusFraction = { local ->
                        val width = if (settings.speedMode == SpeedMode.RSVP) 1f else settings.speedTextWidthFraction
                        measuredFocusFraction = (1f - width) / 2f + width * local
                    },
                    speechColoring = speechColoring, speakerColor = speakerColor,
                    state = state,
                    settings = settings,
                    textColor = textColor,
                    focusColor = focusColor,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(y = yOffset)
                        .then(
                            if (settings.speedMode == SpeedMode.RSVP) Modifier.fillMaxWidth()
                            else Modifier.fillMaxWidth(settings.speedTextWidthFraction)
                        ),
                )
                if (settings.speedLowerSegmentMode != SpeedLowerSegmentMode.OFF) {
                    PersistentSpeedSegment(
                        speechColoring = speechColoring, speakerColor = speakerColor,
                        chapterText = state.chapterText,
                        chunk = chunk,
                        mode = settings.speedLowerSegmentMode,
                        textColor = textColor,
                        focusColor = focusColor,
                        fontSizeSp = settings.speedFontSizeSp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = yOffset + persistentOffset)
                            .fillMaxWidth(settings.speedTextWidthFraction),
                    )
                }
                if (!state.playing && settings.speedShowPausePreview) {
                    SpeedPausePreview(
                        speechColoring = speechColoring, speakerColor = speakerColor,
                        chapterText = state.chapterText,
                        chunk = chunk,
                        settings = settings,
                        textColor = textColor,
                        focusColor = focusColor,
                        onWordTap = viewModel::speedSeekToOffset,
                        compact = immersiveMode,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(if (immersiveMode) 0.90f else 0.94f)
                            .padding(bottom = if (immersiveMode) 6.dp else 12.dp),
                    )
                }
                if (!state.playing &&
                    !settings.speedShowPausePreview &&
                    settings.speedPauseContextEnabled &&
                    settings.speedLowerSegmentMode != SpeedLowerSegmentMode.CONTEXT
                ) {
                    val context = remember(state.chapterText, chunk.startOffset, chunk.endOffset) {
                        SpeedReaderCursor.pauseContext(state.chapterText, chunk.startOffset, chunk.endOffset)
                    }
                    val contextText = listOfNotNull(
                        context.before.takeIf { it.isNotBlank() }?.let { "…$it" },
                        context.after.takeIf { it.isNotBlank() }?.let { "$it…" },
                    ).joinToString("   •   ")
                    if (contextText.isNotBlank()) {
                        Text(
                            text = contextText,
                            color = textColor.copy(alpha = 0.32f),
                            fontSize = (settings.speedFontSizeSp * 0.22f).coerceIn(12f, 17f).sp,
                            lineHeight = 21.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .offset(y = yOffset + pauseContextOffset)
                                .fillMaxWidth(settings.speedTextWidthFraction * 0.90f),
                        )
                    }
                }
            }
            else -> Text("Нет текста", color = textColor, modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun SpeedMediaFrame(
    bookId: String,
    cue: SpeedMediaCue,
    viewModel: AppViewModel,
    textColor: Color,
    focusColor: Color,
    displayMs: Int,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val viewportMaxHeight = maxHeight
        val targetWidthPx = with(density) { (maxWidth * 0.90f).roundToPx() }.coerceAtLeast(480)
        val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = bookId, key2 = cue.ref, key3 = targetWidthPx) {
            value = viewModel.loadImage(bookId, cue.ref, targetWidthPx)
        }
        Column(
            Modifier.fillMaxWidth(0.92f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = cue.alt.ifBlank { null },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((viewportMaxHeight * 0.62f).coerceAtLeast(160.dp))
                        .clip(RoundedCornerShape(18.dp)),
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth().height((viewportMaxHeight * 0.42f).coerceAtLeast(140.dp)),
                    shape = RoundedCornerShape(18.dp),
                    color = textColor.copy(alpha = 0.05f),
                ) {
                    Box(contentAlignment = Alignment.Center) { CircularProgressIndicator(color = focusColor) }
                }
            }
            if (cue.alt.isNotBlank()) {
                Text(cue.alt, color = textColor.copy(alpha = 0.78f), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
            }
            Text(
                "Изображение · ${String.format("%.1f", displayMs / 1000f)} с · тап, чтобы продолжить",
                color = textColor.copy(alpha = 0.46f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Composable
private fun SpeedChapterIntro(
    chapterIndex: Int,
    title: String,
    textColor: Color,
    focusColor: Color,
    modifier: Modifier = Modifier,
) {
    val chapterLabel = "Глава ${chapterIndex + 1}"
    val normalizedTitle = title.trim()
    val displayTitle = normalizedTitle
        .replace(Regex("(?iu)^глава\\s+\\d+[\\s:—–.-]*"), "")
        .trim()
        .ifBlank { normalizedTitle.takeUnless { it.equals(chapterLabel, ignoreCase = true) }.orEmpty() }
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            chapterLabel,
            color = focusColor.copy(alpha = 0.88f),
            style = MaterialTheme.typography.titleMedium,
            letterSpacing = 0.8.sp,
        )
        if (displayTitle.isNotBlank()) {
            Spacer(Modifier.height(14.dp))
            Text(
                displayTitle,
                color = textColor,
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.86f),
            )
        }
        Spacer(Modifier.height(22.dp))
        Box(Modifier.width(54.dp).height(2.dp).background(focusColor.copy(alpha = 0.72f)))
    }
}

@Composable
private fun SpeedPausePreview(
    speechColoring: DialogueTextColoring?,
    speakerColor: (String) -> Color,
    chapterText: String,
    chunk: io.fluxreader.app.reader.SpeedChunk,
    settings: ReaderSettings,
    textColor: Color,
    focusColor: Color,
    onWordTap: (Int) -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (chapterText.isBlank()) return
    val measurer = rememberTextMeasurer()
    val previewStyle = MaterialTheme.typography.bodyMedium.copy(
        color = textColor.copy(alpha = 0.84f),
        lineHeight = 22.sp,
        textAlign = TextAlign.Start,
    )
    val density = LocalDensity.current
    Surface(
        modifier = modifier,
        color = textColor.copy(alpha = 0.085f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.14f)),
        shadowElevation = 0.dp,
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = if (compact) 8.dp else 12.dp)) {
            val width = constraints.maxWidth.coerceAtLeast(1)
            val lineBudget = if (compact) 3 else 4
            fun focusedText(window: io.fluxreader.app.reader.SpeedTextWindow): AnnotatedString = buildAnnotatedString {
                append(window.text)
                val localStart = (chunk.startOffset - window.startOffset).coerceIn(0, window.text.length)
                val localEnd = (chunk.endOffset - window.startOffset).coerceIn(localStart, window.text.length)
                if (localStart < localEnd) addStyle(
                    SpanStyle(color = focusColor, fontWeight = FontWeight.SemiBold), localStart, localEnd)
            }
            val window = remember(chapterText, chunk.startOffset, chunk.endOffset, width,
                previewStyle, density.density, density.fontScale, lineBudget, measurer) {
                io.fluxreader.app.reader.SpeedPausePreviewWindow.fit(chapterText, chunk.startOffset, chunk.endOffset) { candidate ->
                    val measured = measurer.measure(
                        text = focusedText(candidate), style = previewStyle,
                        constraints = Constraints(maxWidth = width), maxLines = Int.MAX_VALUE,
                        overflow = TextOverflow.Clip,
                    )
                    measured.lineCount <= lineBudget
                }
            }
            if (window.text.isNotBlank()) {
                val annotated = colorSpeedSpeech(focusedText(window),
                    speechColoring?.segments(window.startOffset, window.startOffset + window.text.length)
                        .orEmpty().mapNotNull { run -> run.speaker?.let { SpeechColorRun(run.start, run.endExclusive, speakerColor(it)) } })
                val layoutHolder = remember(annotated, window.startOffset, previewStyle, width) {
                    arrayOfNulls<androidx.compose.ui.text.TextLayoutResult>(1)
                }
                BasicText(
                    text = annotated,
                    modifier = Modifier.pointerInput(annotated, window.startOffset, settings.speedDialogueEnabled) {
                        detectTapGestures { position ->
                            val layout = layoutHolder[0] ?: return@detectTapGestures
                            val localOffset = layout.getOffsetForPosition(position).coerceIn(0, window.text.lastIndex)
                            val word = SpeedDialoguePlayback.wordStartOffset(chapterText, window.startOffset + localOffset, settings)
                            onWordTap(word)
                        }
                    },
                    style = previewStyle,
                    // The measured window already fits the normal budget. A longer fixation is
                    // allowed to wrap instead of hiding its active words behind an ellipsis.
                    maxLines = Int.MAX_VALUE,
                    overflow = TextOverflow.Clip,
                    onTextLayout = { layoutHolder[0] = it },
                )
            }
        }
    }
}

@Composable
private fun PersistentSpeedSegment(
    speechColoring: DialogueTextColoring?,
    speakerColor: (String) -> Color,
    chapterText: String,
    chunk: io.fluxreader.app.reader.SpeedChunk,
    mode: SpeedLowerSegmentMode,
    textColor: Color,
    focusColor: Color,
    fontSizeSp: Float,
    modifier: Modifier = Modifier,
) {
    val segmentSize = (fontSizeSp * 0.28f).coerceIn(13f, 20f).sp
    val coloredChunk = colorSpeedSpeech(AnnotatedString(chunk.tokens.joinToString(" ")),
        speechColoring?.segments(chunk).orEmpty().mapNotNull { run ->
            run.speaker?.let { SpeechColorRun(run.start, run.endExclusive, speakerColor(it)) }
        })
    when (mode) {
        SpeedLowerSegmentMode.OFF -> Unit
        SpeedLowerSegmentMode.CURRENT -> {
            Text(
                text = coloredChunk,
                color = textColor.copy(alpha = 0.72f),
                fontSize = segmentSize,
                lineHeight = (segmentSize.value * 1.25f).sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier.fillMaxWidth(0.88f),
            )
        }
        SpeedLowerSegmentMode.CONTEXT -> {
            val context = remember(chapterText, chunk.startOffset, chunk.endOffset) {
                SpeedReaderCursor.pauseContext(chapterText, chunk.startOffset, chunk.endOffset, wordsEachSide = 4)
            }
            val annotated = buildAnnotatedString {
                if (context.before.isNotBlank()) {
                    withStyle(SpanStyle(color = textColor.copy(alpha = 0.30f))) { append("…${context.before}  ") }
                }
                withStyle(SpanStyle(color = focusColor.copy(alpha = 0.92f), fontWeight = FontWeight.SemiBold)) {
                    append(coloredChunk)
                }
                if (context.after.isNotBlank()) {
                    withStyle(SpanStyle(color = textColor.copy(alpha = 0.30f))) { append("  ${context.after}…") }
                }
            }
            Text(
                text = annotated,
                fontSize = segmentSize,
                lineHeight = (segmentSize.value * 1.30f).sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = modifier.fillMaxWidth(0.94f),
            )
        }
    }
}

@Composable
private fun PlaybackControls(
    state: SpeedUiState,
    viewModel: AppViewModel,
    textColor: Color,
    focusColor: Color,
    onFullscreen: () -> Unit,
    onNarration: () -> Unit,
    voiceEnabled: Boolean,
    onTogglePlay: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    showFullscreen: Boolean,
    modifier: Modifier = Modifier,
) {
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalPanel(
            modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp),
            contentPadding = 5.dp,
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ExperimentalPanel(accent = true, contentPadding = 0.dp) {
                    IconButton(onClick = onPrevious, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Предыдущее предложение", tint = LocalFluxThemeMode.current.chromeText)
                    }
                }
                ExperimentalPanel(accent = true, contentPadding = 0.dp) {
                    IconButton(onClick = onTogglePlay, modifier = Modifier.size(56.dp)) {
                        Icon(
                            if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (state.playing) "Пауза" else "Старт",
                            tint = LocalFluxThemeMode.current.chromeAccent,
                            modifier = Modifier.size(30.dp),
                        )
                    }
                }
                ExperimentalPanel(accent = true, contentPadding = 0.dp) {
                    IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Следующее предложение", tint = LocalFluxThemeMode.current.chromeText)
                    }
                }
                ExperimentalPanel(contentPadding = 0.dp) {
                    IconButton(onClick = onNarration, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.RecordVoiceOver,
                            contentDescription = if (voiceEnabled) "Выключить озвучку" else "Включить озвучку",
                            tint = if (voiceEnabled) LocalFluxThemeMode.current.chromeAccent else LocalFluxThemeMode.current.chromeText)
                    }
                }
                if (showFullscreen) ExperimentalPanel(contentPadding = 0.dp) {
                    IconButton(onClick = onFullscreen, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Default.Fullscreen, contentDescription = "Полноэкранное скорочтение", tint = LocalFluxThemeMode.current.chromeAccent)
                    }
                }
            }
        }
        return
    }

    Surface(
        modifier = modifier,
        color = textColor.copy(alpha = 0.115f),
        shape = RoundedCornerShape(30.dp),
        border = BorderStroke(1.dp, textColor.copy(alpha = 0.17f)),
        shadowElevation = 10.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.SkipPrevious,
                    contentDescription = "Предыдущее предложение",
                    tint = textColor.copy(alpha = 0.88f),
                )
            }
            Surface(
                shape = CircleShape,
                color = focusColor.copy(alpha = 0.24f),
                border = BorderStroke(1.dp, focusColor.copy(alpha = 0.42f)),
                shadowElevation = 4.dp,
            ) {
                IconButton(
                    onClick = onTogglePlay,
                    modifier = Modifier.size(56.dp),
                ) {
                    Icon(
                        if (state.playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (state.playing) "Пауза" else "Старт",
                        tint = focusColor,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Default.SkipNext,
                    contentDescription = "Следующее предложение",
                    tint = textColor.copy(alpha = 0.88f),
                )
            }
            IconButton(onClick = onNarration, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.RecordVoiceOver,
                    contentDescription = if (voiceEnabled) "Выключить озвучку" else "Включить озвучку",
                    tint = if (voiceEnabled) focusColor else textColor.copy(alpha = 0.65f))
            }
            if (showFullscreen) Surface(
                shape = CircleShape,
                color = Color.Transparent,
                border = BorderStroke(1.dp, focusColor.copy(alpha = 0.58f)),
            ) {
                IconButton(
                    onClick = onFullscreen,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        Icons.Default.Fullscreen,
                        contentDescription = "Полноэкранное скорочтение",
                        tint = focusColor,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedProgress(
    state: SpeedUiState,
    settings: ReaderSettings,
    focusColor: Color,
    textColor: Color,
) {
    val chapterCount = state.outline?.chapters?.size ?: 0
    val chapterProgress = if (state.chapterText.isBlank()) 0f else {
        ((state.chunk?.startOffset ?: 0).toFloat() / state.chapterText.length.toFloat()).coerceIn(0f, 1f)
    }
    val bookProgress = if (chapterCount <= 0) 0f else {
        ((state.chapterIndex + chapterProgress) / chapterCount.toFloat()).coerceIn(0f, 1f)
    }
    val chapterWordCount = remember(state.chapterText) {
        Regex("\\S+").findAll(state.chapterText).count().coerceAtLeast(1)
    }
    val remainingWords = (chapterWordCount * (1f - chapterProgress)).coerceAtLeast(0f)
    val remainingSeconds = ((remainingWords / settings.speedWpm.coerceAtLeast(1)) * 60f).roundToInt().coerceAtLeast(0)
    val eta = when {
        remainingSeconds >= 3600 -> "%d:%02d:%02d".format(remainingSeconds / 3600, (remainingSeconds % 3600) / 60, remainingSeconds % 60)
        else -> "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60)
    }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        if (settings.speedProgressMode == SpeedProgressMode.CHAPTER || settings.speedProgressMode == SpeedProgressMode.BOTH) {
            LinearProgressIndicator(
                progress = { chapterProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = focusColor,
                trackColor = textColor.copy(alpha = 0.14f),
            )
        }
        if (settings.speedProgressMode == SpeedProgressMode.BOOK || settings.speedProgressMode == SpeedProgressMode.BOTH) {
            LinearProgressIndicator(
                progress = { bookProgress },
                modifier = Modifier.fillMaxWidth().height(if (settings.speedProgressMode == SpeedProgressMode.BOTH) 2.dp else 4.dp),
                color = focusColor.copy(alpha = if (settings.speedProgressMode == SpeedProgressMode.BOTH) 0.48f else 1f),
                trackColor = textColor.copy(alpha = 0.12f),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val progressLabel = when (settings.speedProgressMode) {
                SpeedProgressMode.CHAPTER -> "Глава ${(chapterProgress * 100).toInt()}%"
                SpeedProgressMode.BOOK -> "Книга ${(bookProgress * 100).toInt()}%"
                SpeedProgressMode.BOTH -> "Глава ${(chapterProgress * 100).toInt()}% · Книга ${(bookProgress * 100).toInt()}%"
            }
            Text(progressLabel, color = textColor.copy(alpha = 0.64f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
            Text("≈ $eta", color = textColor.copy(alpha = 0.60f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun SpeedChunkText(
    onFocusFraction: (Float) -> Unit,
    speechColoring: DialogueTextColoring?,
    speakerColor: (String) -> Color,
    state: SpeedUiState,
    settings: ReaderSettings,
    textColor: Color,
    focusColor: Color,
    modifier: Modifier = Modifier,
) {
    val chunk = state.chunk ?: return
    val speechRuns = speechColoring?.segments(chunk).orEmpty().mapNotNull { run ->
        run.speaker?.let { SpeechColorRun(run.start, run.endExclusive, speakerColor(it)) }
    }
    val family = resolveReaderFontFamily(settings.speedFontMode, settings.customFontPath)
    val weight = speedFontWeight(settings.speedFontWeight)
    if (settings.speedMode == SpeedMode.RSVP && chunk.tokens.size == 1) {
        RsvpWord(
            speechRuns = speechRuns, speechColoring = speechColoring, speakerColor = speakerColor,
            token = chunk.tokens.first(),
            chapterText = state.chapterText,
            chunkEndOffset = chunk.endOffset,
            settings = settings,
            textColor = textColor,
            focusColor = focusColor,
            family = family,
            weight = weight,
            modifier = modifier,
        )
        return
    }

    val focusOffset = chunk.focusTokenIndex.takeIf { it in chunk.tokens.indices }?.let { tokenIndex ->
        chunk.tokens.take(tokenIndex).sumOf { it.length + 1 } + SpeedReaderCursor.focusCharacterIndex(chunk.tokens[tokenIndex])
    }
    val annotated = colorSpeedSpeech(buildSpeedAnnotatedText(chunk.tokens, chunk.focusTokenIndex, settings, textColor, focusColor),
        speechRuns, focusOffset.takeIf { settings.speedFocusStyle == SpeedFocusStyle.CHARACTER })
    FittedSpeedText(
        focusOffset = focusOffset, onFocusFraction = onFocusFraction,
        text = annotated,
        settings = settings,
        textColor = textColor,
        family = family,
        weight = weight,
        modifier = modifier,
    )
}

@Composable
private fun RsvpWord(
    speechRuns: List<SpeechColorRun>,
    speechColoring: DialogueTextColoring?,
    speakerColor: (String) -> Color,
    token: String,
    chapterText: String,
    chunkEndOffset: Int,
    settings: ReaderSettings,
    textColor: Color,
    focusColor: Color,
    family: FontFamily,
    weight: FontWeight,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.clipToBounds(), contentAlignment = Alignment.CenterStart) {
        val focus = SpeedReaderCursor.focusCharacterIndex(token)
        val density = LocalDensity.current
        val measurer = rememberTextMeasurer()
        val maxWidthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val baseSize = settings.speedFontSizeSp
        val letterSpacing = settings.speedLetterSpacingSp.sp

        fun focusedWord(): AnnotatedString = buildAnnotatedString {
            if (settings.speedFocusStyle == SpeedFocusStyle.WORD) {
                withStyle(SpanStyle(color = focusColor, fontWeight = FontWeight.Black)) { append(token) }
            } else {
                append(token.substring(0, focus))
                token.getOrNull(focus)?.let { character ->
                    withStyle(SpanStyle(color = focusColor, fontWeight = FontWeight.Black)) { append(character) }
                }
                if (focus + 1 < token.length) append(token.substring(focus + 1))
            }
        }

        val baseText = colorSpeedSpeech(focusedWord(), speechRuns, focus.takeIf { settings.speedFocusStyle == SpeedFocusStyle.CHARACTER })
        val baseStyle = TextStyle(
            color = textColor,
            fontFamily = family,
            fontWeight = weight,
            fontSize = baseSize.sp,
            letterSpacing = letterSpacing,
        )
        val baseLayout = measurer.measure(baseText, style = baseStyle, maxLines = 1, softWrap = false)
        val baseFocusBox = if (token.isNotEmpty()) baseLayout.getBoundingBox(focus.coerceIn(0, token.lastIndex)) else null
        val baseFocusCenter = baseFocusBox?.center?.x ?: (baseLayout.size.width / 2f)
        val leftExtent = baseFocusCenter.coerceAtLeast(1f)
        val rightExtent = (baseLayout.size.width - baseFocusCenter).coerceAtLeast(1f)
        val focusTargetPx = SpeedViewportGeometry.horizontalFocusPx(maxWidthPx, settings.speedHorizontalFocusFraction)
        val leftRoom = focusTargetPx * 0.975f
        val rightRoom = (maxWidthPx - focusTargetPx) * 0.975f
        val scale = min(1f, min(leftRoom / leftExtent, rightRoom / rightExtent))
        val requestedSize = baseSize * scale
        val minSize = settings.speedMinFontSizeSp
        val preferredSizeSp = when {
            settings.speedFixedFontSize -> baseSize
            !settings.speedAutoFitText -> baseSize
            requestedSize >= minSize -> requestedSize
            leftExtent * (minSize / baseSize) <= leftRoom && rightExtent * (minSize / baseSize) <= rightRoom -> minSize
            else -> requestedSize.coerceAtLeast(14f)
        }
        val finalText = baseText
        // Even fixed-size mode must not cut letters off at the viewport edges. Check actual
        // glyph bounds: Android font scaling is nonlinear, so a ratio of sp values is unsafe.
        val finalSizeSp = if (preferredSizeSp == baseSize && leftExtent <= leftRoom && rightExtent <= rightRoom) baseSize
        else SpeedTextFitPolicy.largestFitting(preferredSizeSp) { size ->
            val layout = measurer.measure(finalText, style = baseStyle.copy(fontSize = size.sp), maxLines = 1, softWrap = false)
            val focusX = layout.getBoundingBox(focus.coerceIn(0, token.lastIndex)).center.x
            focusX <= leftRoom && layout.size.width - focusX <= rightRoom
        }
        val finalStyle = baseStyle.copy(fontSize = finalSizeSp.sp)
        val finalLayout = if (finalSizeSp == baseSize) baseLayout else measurer.measure(finalText, style = finalStyle, maxLines = 1, softWrap = false)
        val finalFocusBox = if (token.isNotEmpty()) finalLayout.getBoundingBox(focus.coerceIn(0, token.lastIndex)) else null
        val focusCenterPx = finalFocusBox?.center?.x ?: (finalLayout.size.width / 2f)
        // The ORP glyph is pinned to a stable left-biased fixation rail. The rail defaults
        // to 30% of the viewport, leaving most of the line free for gray upcoming words.
        val currentLeftPx = focusTargetPx - focusCenterPx
        val currentLeftDp = with(density) { currentLeftPx.toDp() }

        Text(
            text = finalText,
            color = textColor,
            fontSize = finalSizeSp.sp,
            fontFamily = family,
            fontWeight = weight,
            letterSpacing = letterSpacing,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            modifier = Modifier.align(Alignment.CenterStart).offset(x = currentLeftDp),
        )

        if (settings.speedReedyFlowEnabled && settings.speedPeripheralWords > 0) {
            val following = remember(chapterText, chunkEndOffset, settings.speedPeripheralWords, settings.speedDialogueEnabled) {
                val previewOffset = if (settings.speedDialogueEnabled) SpeedDialoguePlayback.readableOffset(chapterText, chunkEndOffset) else chunkEndOffset
                SpeedReaderCursor.followingTokens(chapterText, previewOffset, settings.speedPeripheralWords)
            }
            if (following.isNotEmpty()) {
                val gapPx = with(density) { 14.dp.toPx() }
                val nextLeftPx = currentLeftPx + finalLayout.size.width + gapPx
                val nextWord = following.first()
                val nextOffset = if (settings.speedDialogueEnabled) SpeedDialoguePlayback.readableOffset(chapterText, chunkEndOffset) else chunkEndOffset
                val nextChunk = SpeedReaderCursor.next(chapterText, nextOffset, 1)
                val nextSpeaker = nextChunk?.let { speechColoring?.segments(it)?.firstOrNull { run -> run.speaker != null }?.speaker }
                val peripheralColor = nextSpeaker?.let(speakerColor) ?: textColor
                val previewStyle = finalStyle.copy(color = peripheralColor.copy(alpha = settings.speedContextOpacity))
                val previewWidthPx = measurer.measure(nextWord, style = previewStyle,
                    maxLines = 1, softWrap = false).size.width
                if (nextLeftPx >= 0f && nextLeftPx + previewWidthPx + gapPx < maxWidthPx) {
                    Text(
                        text = nextWord,
                        color = peripheralColor.copy(alpha = settings.speedContextOpacity * 0.72f),
                        fontSize = finalSizeSp.sp,
                        fontFamily = family, fontWeight = weight, letterSpacing = letterSpacing,
                        maxLines = 1, softWrap = false, overflow = TextOverflow.Clip,
                        modifier = Modifier.align(Alignment.CenterStart)
                            .offset(x = with(density) { nextLeftPx.toDp() }),
                    )
                }
            }
        }
    }
}

private fun buildSpeedAnnotatedText(
    tokens: List<String>,
    focusTokenIndex: Int,
    settings: ReaderSettings,
    textColor: Color,
    focusColor: Color,
): AnnotatedString = buildAnnotatedString {
    tokens.forEachIndexed { index, token ->
        if (index > 0) append(" ")
        val contextAlpha = if (settings.speedMode == SpeedMode.PHRASE) 1f else settings.speedContextOpacity
        if (index != focusTokenIndex) {
            withStyle(SpanStyle(color = textColor.copy(alpha = contextAlpha))) { append(token) }
            return@forEachIndexed
        }
        if (settings.speedFocusStyle == SpeedFocusStyle.WORD) {
            withStyle(SpanStyle(color = focusColor, fontWeight = FontWeight.Black)) { append(token) }
            return@forEachIndexed
        }
        val focus = SpeedReaderCursor.focusCharacterIndex(token)
        append(token.substring(0, focus))
        if (token.isNotEmpty()) {
            withStyle(SpanStyle(color = focusColor, fontWeight = FontWeight.Black)) { append(token[focus]) }
            if (focus + 1 < token.length) append(token.substring(focus + 1))
        }
    }
}

@Composable
private fun FittedSpeedText(
    focusOffset: Int?,
    onFocusFraction: (Float) -> Unit,
    text: AnnotatedString,
    settings: ReaderSettings,
    textColor: Color,
    family: FontFamily,
    weight: FontWeight,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.roundToPx() }.coerceAtLeast(1)
        val textMeasurer = rememberTextMeasurer()
        val maxLines = settings.speedMaxLines
        val targetSize = when (settings.speedMode) {
            SpeedMode.RSVP -> settings.speedFontSizeSp
            SpeedMode.PHRASE -> settings.speedFontSizeSp * 0.90f
            SpeedMode.FOCUS -> settings.speedFontSizeSp * 0.86f
        }
        val letterSpacing = settings.speedLetterSpacingSp.sp
        val fittedSize = remember(text, maxWidthPx, maxLines, targetSize, letterSpacing, weight, family,
            settings.speedLineHeightMultiplier, density.fontScale) {
            SpeedTextFitPolicy.largestFitting(targetSize) { size ->
                val result = textMeasurer.measure(text = text,
                    style = TextStyle(fontFamily = family, fontWeight = weight, fontSize = size.sp,
                        lineHeight = (size * settings.speedLineHeightMultiplier).sp,
                        letterSpacing = letterSpacing, textAlign = TextAlign.Center),
                    maxLines = maxLines, softWrap = maxLines > 1, overflow = TextOverflow.Clip,
                    constraints = Constraints(maxWidth = maxWidthPx))
                !result.hasVisualOverflow
            }
        }
        Text(
            text = text,
            color = textColor,
            fontSize = fittedSize.sp,
            lineHeight = (fittedSize * settings.speedLineHeightMultiplier).sp,
            textAlign = TextAlign.Center,
            fontFamily = family,
            fontWeight = weight,
            letterSpacing = letterSpacing,
            maxLines = maxLines,
            softWrap = maxLines > 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth(),
            onTextLayout = { layout ->
                val position = focusOffset?.takeIf { it in text.text.indices }
                onFocusFraction(if (position == null || layout.size.width == 0) .5f
                    else layout.getBoundingBox(position).center.x / layout.size.width)
            },
        )
    }
}

private fun speedFontWeight(value: Int): FontWeight = when (value.coerceIn(300, 900)) {
    in 0..349 -> FontWeight.W300
    in 350..449 -> FontWeight.W400
    in 450..549 -> FontWeight.W500
    in 550..649 -> FontWeight.W600
    in 650..749 -> FontWeight.W700
    in 750..849 -> FontWeight.W800
    else -> FontWeight.W900
}

@Composable
private fun SpeedControls(settings: ReaderSettings, viewModel: AppViewModel, textColor: Color) {
    var wpmDraft by remember(settings.speedWpm) { mutableFloatStateOf(settings.speedWpm.toFloat()) }
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalPanel(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            contentPadding = 10.dp,
        ) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("СКОРОСТЬ", color = LocalFluxThemeMode.current.chromeText, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Text("${wpmDraft.toInt()} WPM", color = LocalFluxThemeMode.current.chromeAccent, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                SettingsSlider(
                    value = wpmDraft,
                    onValueChange = { wpmDraft = it },
                    onValueChangeFinished = { viewModel.updateSettings { it.copy(speedWpm = wpmDraft.toInt()) } },
                    valueRange = 100f..3000f,
                    steps = 28,
                )
                Text("ТЕМП ПОКАЗА", color = LocalFluxThemeMode.current.chromeText.copy(alpha = 0.84f), style = MaterialTheme.typography.labelMedium)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SpeedPacingMode.entries.forEach { pacing ->
                        ExperimentalTab(
                            label = pacingLabel(pacing),
                            selected = settings.speedPacingMode == pacing,
                            modifier = Modifier.weight(1f),
                        ) {
                            viewModel.updateSettings { it.copy(speedPacingMode = pacing) }
                        }
                    }
                }
            }
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        color = textColor.copy(alpha = 0.045f),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Скорость", color = textColor.copy(alpha = 0.72f), style = MaterialTheme.typography.labelLarge)
                Text("${wpmDraft.toInt()} WPM", color = textColor, style = MaterialTheme.typography.titleMedium)
            }
            SettingsSlider(
                value = wpmDraft,
                onValueChange = { wpmDraft = it },
                onValueChangeFinished = { viewModel.updateSettings { it.copy(speedWpm = wpmDraft.toInt()) } },
                valueRange = 100f..3000f,
                steps = 28,
            )
            Text("Темп показа", color = textColor.copy(alpha = 0.66f), style = MaterialTheme.typography.labelMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                SpeedPacingMode.entries.forEach { pacing ->
                    FilterChip(
                        selected = settings.speedPacingMode == pacing,
                        onClick = {
                            viewModel.updateSettings { it.copy(speedPacingMode = pacing) }
                        },
                        label = { Text(pacingLabel(pacing)) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedSettingsHub(
    settings: ReaderSettings,
    viewModel: AppViewModel,
    onOpen: (SpeedSettingsPanel) -> Unit,
    onDismiss: () -> Unit,
) {
    FluxModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        ModalSheetHeader(
                title = "Скорочтение",
                onClose = onDismiss, modifier = Modifier.padding(horizontal = 20.dp))

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SettingsValueSlider("Размер текста", settings.speedFontSizeSp, 24f..104f, { "${it.toInt()} sp" }) { value ->
                viewModel.updateSettings { it.copy(speedFontSizeSp = value) }
            }
            SpeedSettingsNavRow("Отображение", "") { onOpen(SpeedSettingsPanel.DISPLAY) }
            SpeedSettingsNavRow("Сложные слова", "") { onOpen(SpeedSettingsPanel.COMPLEX_WORDS) }
            SpeedSettingsNavRow("Поведение", "") { onOpen(SpeedSettingsPanel.BEHAVIOR) }
            SpeedSettingsNavRow("Эксперимент: диалоги", "") { onOpen(SpeedSettingsPanel.DIALOGUE) }
            SpeedSettingsNavRow("Голос и озвучка", "") { onOpen(SpeedSettingsPanel.VOICE) }
            if (settings.useWastelandDesign || settings.useCyberpunkDesign) {
                SpeedSettingsNavRow("Экранные эффекты", "") { onOpen(SpeedSettingsPanel.EFFECTS) }
            }
            SpeedSettingsNavRow("Расширенные", "RSVP, тайминги и цвета") { onOpen(SpeedSettingsPanel.ADVANCED) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedDialogueSheet(
    onEditSpeaker: () -> Unit,
    onColors: () -> Unit,
    settings: ReaderSettings,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    FluxModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onBack = onBack) {
        ModalSheetHeader("Эксперимент: диалоги", onBack = onBack, onClose = onDismiss, modifier = Modifier.padding(horizontal = 20.dp))

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val speedState by viewModel.speed.collectAsState()
            if (settings.speedDialogueEnabled && settings.speedDialogueBookAnalysis) {
                speedState.dialogueAnalysisStatus?.let { status ->
                    Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (speedState.chunk?.let { DialogueReplyLocator.at(speedState.chapterText, it.startOffset) } != null) {
                SettingsMenuRow("Говорящий в этой реплике", onClick = onEditSpeaker)
            }
            SettingsMenuRow("Цвета персонажей", onClick = onColors)
            SpeedDialogueSettings(settings, viewModel::updateSettings)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedVoiceSettingsSheet(
    settings: ReaderSettings,
    voices: List<SpeedSpeechNarrator.AvailableVoice>,
    voiceReady: Boolean,
    preparing: Boolean,
    voiceError: String?,
    onPreview: () -> Unit,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    FluxModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
        onBack = onBack,
    ) {
        ModalSheetHeader(title = "Голос и озвучка", onBack = onBack, onClose = onDismiss, modifier = Modifier.padding(horizontal = 20.dp))

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            voiceError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            SettingsValueSlider(
                title = "Скорость речи",
                value = settings.narrationRate,
                range = 0.6f..1.4f,
                format = { "${(it * 100).toInt()}%" },
            ) { value -> viewModel.updateSettings { it.copy(narrationRate = value) } }
            SettingsValueSlider(
                title = "Высота голоса",
                value = settings.narrationPitch,
                range = 0.75f..1.25f,
                format = { "${(it * 100).toInt()}%" },
            ) { value -> viewModel.updateSettings { it.copy(narrationPitch = value) } }
            SettingsToggleRow(
                title = "Следующая глава",
                subtitle = "",
                checked = settings.narrationAutoNextChapter,
            ) { enabled -> viewModel.updateSettings { it.copy(narrationAutoNextChapter = enabled) } }
            VoiceSourceSettings(settings, viewModel::updateSettings, onPreview, voiceReady, preparing, androidVoices = {
            Text("Офлайн-голоса", style = MaterialTheme.typography.titleMedium)
            if (voices.isEmpty()) Text("Офлайн-голоса не найдены.", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.narrationVoiceId.isBlank(),
                    onClick = { viewModel.updateSettings { it.copy(narrationVoiceId = "") } },
                    label = { Text("Автовыбор") },
                )
            }
            voices.forEach { voice ->
                Surface(
                    modifier = Modifier.fillMaxWidth()
                        .clickable { viewModel.updateSettings { it.copy(narrationVoiceId = voice.id) } },
                    color = if (settings.narrationVoiceId == voice.id)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        text = "${if (settings.narrationVoiceId == voice.id) "✓ " else ""}${voice.id} · ${voice.language}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            })
            if (!io.fluxreader.app.reader.BuiltInVoiceCatalog.isBuiltIn(settings.narrationVoiceId)) Button(onClick = onPreview, enabled = voiceReady && !preparing) { Text("Проба голоса") }

        }
    }
}

@Composable
private fun SpeedSettingsNavRow(title: String, subtitle: String, onClick: () -> Unit) {
    SettingsMenuRow(title, subtitle, onClick)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedDisplaySheet(
    settings: ReaderSettings,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    FluxModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
        onBack = onBack,
    ) {
        ModalSheetHeader(
                title = "Отображение",
                onBack = onBack,
                onClose = onDismiss, modifier = Modifier.padding(horizontal = 20.dp))

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsToggleRow(
                title = "Тема читалки",
                subtitle = "",
                checked = settings.speedUseReaderTheme,
            ) { enabled -> viewModel.updateSettings { it.copy(speedUseReaderTheme = enabled) } }
            Text("Шрифт", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(ReaderFontMode.SYSTEM, ReaderFontMode.SANS, ReaderFontMode.SERIF).forEach { FontModeChip(it, settings, viewModel) }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(ReaderFontMode.MONO, ReaderFontMode.JETBRAINS_NERD).forEach { FontModeChip(it, settings, viewModel) }
            }
            if (settings.speedFontMode == ReaderFontMode.JETBRAINS_NERD) {
                JetBrainsNerdFontImporter(settings = settings, onUpdate = viewModel::updateSettings)
            }
            SettingsToggleRow(
                title = "Направляющая ORP",
                subtitle = "Точка фиксации взгляда.",
                checked = settings.speedShowGuide,
            ) { enabled -> viewModel.updateSettings { it.copy(speedShowGuide = enabled) } }
            if (settings.speedShowGuide) {
                SpeedGuideChoices(settings, viewModel::updateSettings)
            }
            SettingsToggleRow(
                title = "Следующие слова",
                subtitle = "",
                checked = settings.speedReedyFlowEnabled,
            ) { enabled -> viewModel.updateSettings { it.copy(speedReedyFlowEnabled = enabled) } }
            Text("Сегмент под словом", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SpeedLowerSegmentMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.speedLowerSegmentMode == mode,
                        onClick = { viewModel.updateSettings { it.copy(speedLowerSegmentMode = mode) } },
                        label = { Text(when (mode) {
                            SpeedLowerSegmentMode.OFF -> "Выкл."
                            SpeedLowerSegmentMode.CURRENT -> "Сегмент"
                            SpeedLowerSegmentMode.CONTEXT -> "Контекст"
                        }) },
                    )
                }
            }
            SettingsToggleRow(
                title = "Прогресс",
                subtitle = "",
                checked = settings.speedShowProgress,
            ) { enabled -> viewModel.updateSettings { it.copy(speedShowProgress = enabled) } }
            if (settings.speedShowProgress) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    SpeedProgressMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.speedProgressMode == mode,
                            onClick = { viewModel.updateSettings { it.copy(speedProgressMode = mode) } },
                            label = { Text(when (mode) {
                                SpeedProgressMode.CHAPTER -> "Глава"
                                SpeedProgressMode.BOOK -> "Книга"
                                SpeedProgressMode.BOTH -> "Оба"
                            }) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedBehaviorSheet(
    settings: ReaderSettings,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    FluxModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
        onBack = onBack,
    ) {
        ModalSheetHeader(
                title = "Поведение",
                onBack = onBack,
                onClose = onDismiss, modifier = Modifier.padding(horizontal = 20.dp))

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SettingsToggleRow(
                title = "Касание: старт / пауза",
                subtitle = "Двойное касание — полный экран.",
                checked = settings.speedTapToToggle,
            ) { enabled -> viewModel.updateSettings { it.copy(speedTapToToggle = enabled) } }
            SettingsToggleRow(
                title = "Кнопки громкости",
                subtitle = "Нажатие — старт / пауза; удержание — переход по предложениям.",
                checked = settings.speedVolumeKeysEnabled,
            ) { enabled -> viewModel.updateSettings { it.copy(speedVolumeKeysEnabled = enabled) } }
            SettingsToggleRow(
                title = "Текст на паузе",
                subtitle = "Касание слова меняет позицию чтения.",
                checked = settings.speedShowPausePreview,
            ) { enabled -> viewModel.updateSettings { it.copy(speedShowPausePreview = enabled) } }
            SettingsToggleRow(
                title = "Умный возврат после паузы",
                subtitle = "После долгой паузы — к началу предложения.",
                checked = settings.speedAutoRewindAfterPause,
            ) { enabled -> viewModel.updateSettings { it.copy(speedAutoRewindAfterPause = enabled) } }
            SettingsValueSlider("Картинка на экране", settings.speedImagePauseMs.toFloat(), 800f..8000f, { String.format("%.1f с", it / 1000f) }) { value ->
                viewModel.updateSettings { it.copy(speedImagePauseMs = value.toInt()) }
            }
            SettingsValueSlider("Заставка новой главы", settings.speedChapterIntroMs.toFloat(), 0f..5000f, { if (it < 100f) "выкл." else String.format("%.1f с", it / 1000f) }) { value ->
                viewModel.updateSettings { it.copy(speedChapterIntroMs = value.toInt()) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedAdvancedSheet(
    settings: ReaderSettings,
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
) {
    FluxModalBottomSheet(
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        onDismissRequest = onDismiss,
        onBack = onBack,
    ) {
        ModalSheetHeader(
                title = "Расширенные",
                onBack = onBack,
                onClose = onDismiss, modifier = Modifier.padding(horizontal = 20.dp))

        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Механика RSVP", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SpeedMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.speedMode == mode,
                        onClick = { viewModel.updateSettings { current ->
                            val count = when (mode) {
                                SpeedMode.RSVP -> 1
                                SpeedMode.PHRASE -> current.speedWordsPerFixation.coerceAtLeast(2)
                                SpeedMode.FOCUS -> current.speedWordsPerFixation.coerceAtLeast(3)
                            }
                            current.copy(speedMode = mode, speedWordsPerFixation = count)
                        } },
                        label = { Text(speedModeLabel(mode)) },
                    )
                }
            }
            if (settings.speedMode != SpeedMode.RSVP) {
                val minWords = if (settings.speedMode == SpeedMode.FOCUS) 3f else 2f
                SettingsValueSlider("Слов за фиксацию", settings.speedWordsPerFixation.toFloat(), minWords..7f, { it.toInt().toString() }) { value ->
                    viewModel.updateSettings { it.copy(speedWordsPerFixation = value.toInt()) }
                }
            }
            SettingsToggleRow(
                title = "Умная разбивка",
                subtitle = "",
                checked = settings.speedSmartChunking,
            ) { enabled -> viewModel.updateSettings { it.copy(speedSmartChunking = enabled) } }
            SettingsToggleRow(
                title = "Адаптивная длина",
                subtitle = "Меньше слов в сложных фрагментах.",
                checked = settings.speedAdaptiveChunkSize,
            ) { enabled -> viewModel.updateSettings { it.copy(speedAdaptiveChunkSize = enabled) } }

            Text("Тайминги", style = MaterialTheme.typography.titleMedium)
            SettingsToggleRow(
                title = "Микропаузы",
                subtitle = "",
                checked = settings.speedMicroPausesEnabled,
            ) { enabled -> viewModel.updateSettings { it.copy(speedMicroPausesEnabled = enabled) } }
            if (settings.speedMicroPausesEnabled) {
                SettingsValueSlider("Конец предложения", settings.speedSentencePauseMs.toFloat(), 0f..700f, { "${it.toInt()} мс" }) { value -> viewModel.updateSettings { it.copy(speedSentencePauseMs = value.toInt()) } }
                SettingsValueSlider("Запятая / ; / :", settings.speedClausePauseMs.toFloat(), 0f..350f, { "${it.toInt()} мс" }) { value -> viewModel.updateSettings { it.copy(speedClausePauseMs = value.toInt()) } }
                SettingsValueSlider("Абзац", settings.speedParagraphPauseMs.toFloat(), 0f..1200f, { "${it.toInt()} мс" }) { value -> viewModel.updateSettings { it.copy(speedParagraphPauseMs = value.toInt()) } }
            }
            SpeedComplexWordSettings(settings, viewModel::updateSettings)
            SettingsToggleRow(
                title = "Плавный разгон",
                subtitle = "",
                checked = settings.speedGradualAcceleration,
            ) { enabled -> viewModel.updateSettings { it.copy(speedGradualAcceleration = enabled) } }
            if (settings.speedGradualAcceleration) {
                SettingsValueSlider("Разгон", settings.speedAccelerationSeconds.toFloat(), 1f..20f, { "${it.toInt()} с" }) { value -> viewModel.updateSettings { it.copy(speedAccelerationSeconds = value.toInt()) } }
                SettingsValueSlider("Начальная скорость", settings.speedAccelerationStartPercent.toFloat(), 45f..100f, { "${it.toInt()}%" }) { value -> viewModel.updateSettings { it.copy(speedAccelerationStartPercent = value.toInt()) } }
            }
            if (settings.speedAutoRewindAfterPause) {
                SettingsValueSlider("Долгая пауза после", settings.speedAutoRewindThresholdSeconds.toFloat(), 3f..60f, { "${it.toInt()} с" }) { value -> viewModel.updateSettings { it.copy(speedAutoRewindThresholdSeconds = value.toInt()) } }
            }

            Text("Геометрия текста", style = MaterialTheme.typography.titleMedium)
            SettingsToggleRow(
                title = "Одинаковый размер слов",
                subtitle = "Длинные слова уменьшаются до ширины экрана.",
                checked = settings.speedFixedFontSize,
            ) { enabled -> viewModel.updateSettings { it.copy(speedFixedFontSize = enabled) } }
            SettingsValueSlider("Позиция точки взгляда", settings.speedHorizontalFocusFraction, 0.18f..0.50f, { "${(it * 100).toInt()}%" }) { value ->
                viewModel.updateSettings { it.copy(speedHorizontalFocusFraction = value) }
            }
            if (!settings.speedFixedFontSize) {
                SettingsValueSlider("Минимум автоподгонки", settings.speedMinFontSizeSp, 16f..settings.speedFontSizeSp, { "${it.toInt()} sp" }) { value -> viewModel.updateSettings { it.copy(speedMinFontSizeSp = value) } }
            }
            SettingsValueSlider("Насыщенность", settings.speedFontWeight.toFloat(), 300f..900f, { it.toInt().toString() }) { value -> viewModel.updateSettings { it.copy(speedFontWeight = (value.toInt() / 100 * 100).coerceIn(300, 900)) } }
            SettingsValueSlider("Межбуквенный интервал", settings.speedLetterSpacingSp, -2f..6f, { String.format("%.1f sp", it) }) { value -> viewModel.updateSettings { it.copy(speedLetterSpacingSp = value) } }
            SettingsValueSlider("Ширина зоны", settings.speedTextWidthFraction, 0.55f..1f, { "${(it * 100).toInt()}%" }) { value -> viewModel.updateSettings { it.copy(speedTextWidthFraction = value) } }
            SettingsValueSlider("Высота фиксации", settings.speedVerticalPosition, 0.25f..0.75f, { "${(it * 100).toInt()}%" }) { value -> viewModel.updateSettings { it.copy(speedVerticalPosition = value) } }
            SettingsValueSlider("Следующих слов", settings.speedPeripheralWords.toFloat(), 0f..6f, { it.toInt().toString() }) { value -> viewModel.updateSettings { it.copy(speedPeripheralWords = value.toInt()) } }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                SpeedFocusStyle.entries.forEach { style ->
                    FilterChip(
                        selected = settings.speedFocusStyle == style,
                        onClick = { viewModel.updateSettings { it.copy(speedFocusStyle = style) } },
                        label = { Text(if (style == SpeedFocusStyle.CHARACTER) "ORP-буква" else "Слово") },
                    )
                }
            }
            if (!settings.speedFixedFontSize) {
                SettingsToggleRow(
                    title = "Автоподгонка",
                    subtitle = "",
                    checked = settings.speedAutoFitText,
                ) { enabled -> viewModel.updateSettings { it.copy(speedAutoFitText = enabled) } }
            }

            if (settings.speedShowGuide) {
                Text("Направляющая", style = MaterialTheme.typography.titleMedium)
                SpeedGuideChoices(settings, viewModel::updateSettings)
                if (settings.speedGuideStyle == io.fluxreader.app.data.SpeedGuideStyle.RAIL) SettingsToggleRow(
                    title = "Поперечные метки",
                    subtitle = "",
                    checked = settings.speedShowFocusTicks,
                ) { enabled -> viewModel.updateSettings { it.copy(speedShowFocusTicks = enabled) } }
                if (settings.speedGuideStyle == io.fluxreader.app.data.SpeedGuideStyle.RAIL) SettingsValueSlider("Длина линии", settings.speedGuideHeightFraction, 0.15f..0.80f, { "${(it * 100).toInt()}%" }) { value -> viewModel.updateSettings { it.copy(speedGuideHeightFraction = value) } }
                SettingsValueSlider("Толщина", settings.speedGuideWidthDp, 0.5f..4f, { String.format("%.1f dp", it) }) { value -> viewModel.updateSettings { it.copy(speedGuideWidthDp = value) } }
            }

            if (!settings.speedUseReaderTheme && !settings.useWastelandDesign && !settings.useCyberpunkDesign) {
            Text("Цвета", style = MaterialTheme.typography.titleMedium)
            RgbEditor("Текст", settings.speedTextColorArgb) { color -> viewModel.updateSettings { it.copy(speedTextColorArgb = color) } }
            RgbEditor("Фокус", settings.speedFocusColorArgb) { color -> viewModel.updateSettings { it.copy(speedFocusColorArgb = color) } }
            RgbEditor("Фон", settings.speedBackgroundColorArgb) { color -> viewModel.updateSettings { it.copy(speedBackgroundColorArgb = color) } }
            RgbEditor("Направляющая", settings.speedGuideColorArgb) { color -> viewModel.updateSettings { it.copy(speedGuideColorArgb = color) } }
            }
        }
    }
}

@Composable
private fun FontModeChip(font: ReaderFontMode, settings: ReaderSettings, viewModel: AppViewModel) {
    FilterChip(
        selected = settings.speedFontMode == font,
        onClick = { viewModel.updateSettings { it.copy(speedFontMode = font) } },
        label = { Text(speedFontLabel(font)) },
    )
}

@Composable
private fun RgbEditor(title: String, argb: Int, onCommit: (Int) -> Unit) {
    var draft by remember(argb) { mutableIntStateOf(argb) }
    val alpha = (draft ushr 24) and 0xFF
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Surface(modifier = Modifier.size(30.dp), color = Color(draft), shape = MaterialTheme.shapes.small) {}
        }
        ColorChannel(
            name = "R",
            value = (draft ushr 16) and 0xFF,
            tint = Color(0xFFE35D6A.toInt()),
            onValueChange = { value -> draft = composeArgb(alpha, value, (draft ushr 8) and 0xFF, draft and 0xFF) },
            onFinished = { onCommit(draft) },
        )
        ColorChannel(
            name = "G",
            value = (draft ushr 8) and 0xFF,
            tint = Color(0xFF55B66D.toInt()),
            onValueChange = { value -> draft = composeArgb(alpha, (draft ushr 16) and 0xFF, value, draft and 0xFF) },
            onFinished = { onCommit(draft) },
        )
        ColorChannel(
            name = "B",
            value = draft and 0xFF,
            tint = Color(0xFF5D7FE3.toInt()),
            onValueChange = { value -> draft = composeArgb(alpha, (draft ushr 16) and 0xFF, (draft ushr 8) and 0xFF, value) },
            onFinished = { onCommit(draft) },
        )
    }
}

@Composable
private fun ColorChannel(
    name: String,
    value: Int,
    tint: Color,
    onValueChange: (Int) -> Unit,
    onFinished: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.width(22.dp), color = tint, fontWeight = FontWeight.Bold)
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt().coerceIn(0, 255)) },
            onValueChangeFinished = onFinished,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
        )
        Text(value.toString(), modifier = Modifier.width(34.dp), textAlign = TextAlign.End)
    }
}

private fun pacingLabel(mode: SpeedPacingMode): String = when (mode) {
    SpeedPacingMode.CONSTANT -> "Ровно"
    SpeedPacingMode.SMART -> "Умно"
    SpeedPacingMode.DEEP -> "Глубоко"
}

private fun speedModeLabel(mode: SpeedMode): String = when (mode) {
    SpeedMode.RSVP -> "RSVP"
    SpeedMode.PHRASE -> "Фраза"
    SpeedMode.FOCUS -> "Фокус"
}

private fun speedFontLabel(font: ReaderFontMode): String = when (font) {
    ReaderFontMode.SYSTEM -> "Система"
    ReaderFontMode.SERIF -> "Книжный"
    ReaderFontMode.SANS -> "Sans"
    ReaderFontMode.MONO -> "Mono"
    ReaderFontMode.JETBRAINS_NERD -> "JetBrains Nerd"
}

private fun composeArgb(a: Int, r: Int, g: Int, b: Int): Int =
    ((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF)

