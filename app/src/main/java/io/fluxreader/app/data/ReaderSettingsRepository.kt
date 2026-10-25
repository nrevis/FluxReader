package io.fluxreader.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReaderSettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("reader_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ReaderSettings> = _settings.asStateFlow()

    fun update(transform: (ReaderSettings) -> ReaderSettings) {
        val next = sanitize(transform(_settings.value))
        _settings.value = next
        prefs.edit()
            .putString("theme", next.theme.name)
            .putString("narrationVoiceIdV1", next.narrationVoiceId)
            .putFloat("narrationRateV1", next.narrationRate)
            .putFloat("narrationPitchV1", next.narrationPitch)
            .putBoolean("narrationAutoNextChapterV1", next.narrationAutoNextChapter)
            .putBoolean("experimentalDesignEnabledV1", next.experimentalDesignEnabled)
            .putString("experimentalFrameStyleV1", next.experimentalFrameStyle?.name ?: "")
            .putString("experimentalPaletteV1", next.experimentalPalette?.name ?: "")
            .putBoolean("experimentalCrtEnabledV1", next.experimentalCrtEnabled)
            .putBoolean("experimentalScanlinesEnabledV1", next.experimentalScanlinesEnabled)
            .putBoolean("experimentalGlowEnabledV1", next.experimentalGlowEnabled)
            .putBoolean("experimentalNoiseEnabledV1", next.experimentalNoiseEnabled)
            .putBoolean("experimentalVignetteEnabledV1", next.experimentalVignetteEnabled)
            .putFloat("experimentalEffectIntensityV1", next.experimentalEffectIntensity)
            .putString("themeAppearanceV5", next.themeAppearance.name)
            .putString("customThemeNameV4", next.customThemeName)
            .putInt("customBackgroundColorArgbV4", next.customBackgroundColorArgb)
            .putInt("customSurfaceColorArgbV4", next.customSurfaceColorArgb)
            .putInt("customTextColorArgbV4", next.customTextColorArgb)
            .putInt("customAccentColorArgbV4", next.customAccentColorArgb)
            .putString("libraryViewModeV4", next.libraryViewMode.name)
            .putString("librarySortModeV4", next.librarySortMode.name)
            .putString("libraryGroupModeV4", next.libraryGroupMode.name)
            .putBoolean("libraryShowContinueV4", next.libraryShowContinue)
            .putFloat("fontSizeSp", next.fontSizeSp)
            .putFloat("lineHeightMultiplier", next.lineHeightMultiplier)
            .putFloat("horizontalPaddingDp", next.horizontalPaddingDp)
            .putFloat("topPaddingDpV12", next.topPaddingDp)
            .putFloat("bottomPaddingDpV12", next.bottomPaddingDp)
            .putFloat("paragraphSpacingDpV3", next.paragraphSpacingDp)
            .putFloat("firstLineIndentDpV3", next.firstLineIndentDp)
            .putString("fontModeV3", next.fontMode.name)
            .putString("textAlignmentV3", next.textAlignment.name)
            .putBoolean("hyphenationEnabledV8", next.hyphenationEnabled)
            .putFloat("chapterTitleScaleV12", next.chapterTitleScale)
            .putFloat("chapterHeadingScaleV12", next.chapterHeadingScale)
            .putBoolean("centerChapterHeadingsV12", next.centerChapterHeadings)
            .putString("navigationModeV3", next.navigationMode.name)
            .putBoolean("edgeTapZonesEnabledV3", next.edgeTapZonesEnabled)
            .putBoolean("swipeNavigationEnabledV3", next.swipeNavigationEnabled)
            .putFloat("edgeTapFractionV3", next.edgeTapFraction)
            .putInt("chromeAutoHideSecondsV3", next.chromeAutoHideSeconds)
            .putBoolean("readerFullscreenEnabledV1", next.readerFullscreenEnabled)
            .putBoolean("smoothPageTurnsV3", next.smoothPageTurns)
            .putString("pageTurnAnimationV13", next.pageTurnAnimation.name)
            .putInt("pageTurnDurationMsV4", next.pageTurnDurationMs)
            .putBoolean("fullscreenTransitionEnabledV13", next.fullscreenTransitionEnabled)
            .putInt("fullscreenTransitionDurationMsV13", next.fullscreenTransitionDurationMs)
            .putString("speedModeV2", next.speedMode.name)
            .putBoolean("speedDialogueEnabledV1", next.speedDialogueEnabled)
            .putBoolean("speedDialogueNamesV1", next.speedDialogueNames)
            .putBoolean("speedDialogueIndicatorV1", next.speedDialogueIndicator)
            .putString("speedSpeakerStyleV1", next.speedSpeakerStyle.name)
            .putString("speedSpeakerPositionV1", next.speedSpeakerPosition.name)
            .putString("speedSpeakerColorV1", next.speedSpeakerColor.name)
            .putFloat("speedSpeakerFontSizeSpV1", next.speedSpeakerFontSizeSp)
            .putBoolean("speedSpeakerBoldV1", next.speedSpeakerBold)
            .putFloat("speedSpeakerBackgroundAlphaV1", next.speedSpeakerBackgroundAlpha)
            .putFloat("speedSpeakerGapDpV1", next.speedSpeakerGapDp)
            .putBoolean("speedDialogueShortRepliesV1", next.speedDialogueShortReplies)
            .putBoolean("speedDialoguePauseEnabledV1", next.speedDialoguePauseEnabled)
            .putInt("speedDialoguePauseMsV1", next.speedDialoguePauseMs)
            .putInt("speedWpm", next.speedWpm)
            .putInt("speedWordsPerFixation", next.speedWordsPerFixation)
            .putString("speedPacingModeV5", next.speedPacingMode.name)
            .putBoolean("speedSmartChunkingV5", next.speedSmartChunking)
            .putBoolean("speedParagraphPauseV5", next.speedParagraphPause)
            .putBoolean("speedMicroPausesEnabledV6", next.speedMicroPausesEnabled)
            .putInt("speedSentencePauseMsV6", next.speedSentencePauseMs)
            .putInt("speedClausePauseMsV6", next.speedClausePauseMs)
            .putInt("speedParagraphPauseMsV6", next.speedParagraphPauseMs)
            .putBoolean("speedAdaptiveSentencePauseV6", next.speedAdaptiveSentencePause)
            .putInt("speedSentenceLengthBonusMsV6", next.speedSentenceLengthBonusMs)
            .putInt("speedLongWordSlowdownPercentV6", next.speedLongWordSlowdownPercent)
            .putBoolean("speedGradualAccelerationV6", next.speedGradualAcceleration)
            .putInt("speedAccelerationSecondsV6", next.speedAccelerationSeconds)
            .putInt("speedAccelerationStartPercentV6", next.speedAccelerationStartPercent)
            .putBoolean("speedAdaptiveChunkSizeV7", next.speedAdaptiveChunkSize)
            .putBoolean("speedPauseContextEnabledV7", next.speedPauseContextEnabled)
            .putString("speedLowerSegmentModeV8", next.speedLowerSegmentMode.name)
            .putBoolean("speedReedyFlowEnabledV9", next.speedReedyFlowEnabled)
            .putInt("speedPeripheralWordsV9", next.speedPeripheralWords)
            .putBoolean("speedTapToToggleV10", next.speedTapToToggle)
            .putBoolean("speedVolumeKeysEnabledV10", next.speedVolumeKeysEnabled)
            .putBoolean("speedShowPausePreviewV10", next.speedShowPausePreview)
            .putInt("speedImagePauseMsV10", next.speedImagePauseMs)
            .putInt("speedChapterIntroMsV10", next.speedChapterIntroMs)
            .putBoolean("speedAutoRewindAfterPauseV7", next.speedAutoRewindAfterPause)
            .putInt("speedAutoRewindThresholdSecondsV7", next.speedAutoRewindThresholdSeconds)
            .putInt("speedTextColorArgbV2", next.speedTextColorArgb)
            .putInt("speedFocusColorArgbV2", next.speedFocusColorArgb)
            .putInt("speedBackgroundColorArgbV2", next.speedBackgroundColorArgb)
            .putInt("speedGuideColorArgbV2", next.speedGuideColorArgb)
            .putFloat("speedFontSizeSpV3", next.speedFontSizeSp)
            .putBoolean("speedFixedFontSizeV11", next.speedFixedFontSize)
            .putFloat("speedMinFontSizeSpV3", next.speedMinFontSizeSp)
            .putString("speedFontModeV3", next.speedFontMode.name)
            .putInt("speedFontWeightV3", next.speedFontWeight)
            .putFloat("speedLetterSpacingSpV3", next.speedLetterSpacingSp)
            .putFloat("speedLineHeightMultiplierV3", next.speedLineHeightMultiplier)
            .putFloat("speedTextWidthFractionV3", next.speedTextWidthFraction)
            .putFloat("speedHorizontalFocusFractionV11", next.speedHorizontalFocusFraction)
            .putFloat("speedVerticalPositionV3", next.speedVerticalPosition)
            .putInt("speedMaxLinesV3", next.speedMaxLines)
            .putBoolean("speedAutoFitTextV3", next.speedAutoFitText)
            .putString("speedFocusStyleV3", next.speedFocusStyle.name)
            .putFloat("speedContextOpacityV3", next.speedContextOpacity)
            .putBoolean("speedShowGuideV3", next.speedShowGuide)
            .putBoolean("speedShowFocusTicksV3", next.speedShowFocusTicks)
            .putFloat("speedGuideHeightFractionV3", next.speedGuideHeightFraction)
            .putFloat("speedGuideWidthDpV3", next.speedGuideWidthDp)
            .putBoolean("speedShowProgressV3", next.speedShowProgress)
            .putString("speedProgressModeV9", next.speedProgressMode.name)
            .putBoolean("speedFocusBandEnabledV4", next.speedFocusBandEnabled)
            .putFloat("speedFocusBandAlphaV4", next.speedFocusBandAlpha)
            .putBoolean("speedUseReaderThemeV5", next.speedUseReaderTheme)
            .putString("customFontPathV3", next.customFontPath)
            .putString("customFontNameV3", next.customFontName)
            .putBoolean("ocrEnabledV2", next.ocrEnabled)
            .putString("ocrLanguagesV2", next.ocrLanguages.sorted().joinToString("+"))
            .putBoolean("ocrUseMlKitFallbackV2", next.ocrUseMlKitFallback)
            .apply()
    }

    private fun load(): ReaderSettings {
        val theme = enumPref("theme", ReaderThemeMode.OLED)
        val themeAppearance = enumPref("themeAppearanceV5", ThemeAppearance.SYSTEM)
        val libraryViewMode = enumPref("libraryViewModeV4", LibraryViewMode.GRID)
        val librarySortMode = enumPref("librarySortModeV4", LibrarySortMode.RECENT)
        val libraryGroupMode = enumPref("libraryGroupModeV4", LibraryGroupMode.NONE)
        val legacyPageTurnAnimation = enumPref("pageTurnAnimationV4", PageTurnAnimation.SLIDE)
        val pageTurnAnimation = if (prefs.contains("pageTurnAnimationV13")) {
            enumPref("pageTurnAnimationV13", PageTurnAnimation.CURL)
        } else if (legacyPageTurnAnimation == PageTurnAnimation.SLIDE) {
            // SLIDE was the old default. Move default users to the paper-like animation,
            // while preserving explicit NONE/FADE/ZOOM choices.
            PageTurnAnimation.CURL
        } else {
            legacyPageTurnAnimation
        }
        val storedPageTurnDurationMs = prefs.getInt("pageTurnDurationMsV4", 180)
        val pageTurnDurationMs = if (
            !prefs.contains("pageTurnAnimationV13") &&
            legacyPageTurnAnimation == PageTurnAnimation.SLIDE &&
            storedPageTurnDurationMs == 180
        ) 260 else storedPageTurnDurationMs
        val oldSpeedMode = prefs.getString("speedMode", null)
        val speedMode = runCatching {
            SpeedMode.valueOf(prefs.getString("speedModeV2", oldSpeedMode ?: SpeedMode.RSVP.name) ?: SpeedMode.RSVP.name)
        }.getOrDefault(SpeedMode.RSVP)
        val speedFontMode = enumPref("speedFontModeV3", ReaderFontMode.SANS)
        val speedFocusStyle = enumPref("speedFocusStyleV3", SpeedFocusStyle.CHARACTER)
        val speedPacingMode = enumPref("speedPacingModeV5", SpeedPacingMode.SMART)
        val speedLowerSegmentMode = enumPref("speedLowerSegmentModeV8", SpeedLowerSegmentMode.CURRENT)
        val speedProgressMode = enumPref("speedProgressModeV9", SpeedProgressMode.CHAPTER)
        val fontMode = enumPref("fontModeV3", ReaderFontMode.SERIF)
        val textAlignment = enumPref("textAlignmentV3", ReaderTextAlignment.LEFT)
        val navigationMode = enumPref("navigationModeV3", ReaderNavigationMode.SCROLL)
        return sanitize(
            ReaderSettings(
                theme = theme,
                narrationVoiceId = prefs.getString("narrationVoiceIdV1", "").orEmpty(),
                narrationRate = prefs.getFloat("narrationRateV1", 0.94f),
                narrationPitch = prefs.getFloat("narrationPitchV1", 1f),
                narrationAutoNextChapter = prefs.getBoolean("narrationAutoNextChapterV1", true),
                experimentalDesignEnabled = prefs.getBoolean("experimentalDesignEnabledV1", false),
                experimentalFrameStyle = prefs.getString("experimentalFrameStyleV1", null)
                    ?.takeIf { it.isNotBlank() }?.let { runCatching { ReaderThemeMode.valueOf(it) }.getOrNull() },
                experimentalPalette = prefs.getString("experimentalPaletteV1", null)
                    ?.takeIf { it.isNotBlank() }?.let { runCatching { ReaderThemeMode.valueOf(it) }.getOrNull() },
                experimentalCrtEnabled = prefs.getBoolean("experimentalCrtEnabledV1", true),
                experimentalScanlinesEnabled = prefs.getBoolean("experimentalScanlinesEnabledV1", true),
                experimentalGlowEnabled = prefs.getBoolean("experimentalGlowEnabledV1", true),
                experimentalNoiseEnabled = prefs.getBoolean("experimentalNoiseEnabledV1", false),
                experimentalVignetteEnabled = prefs.getBoolean("experimentalVignetteEnabledV1", true),
                experimentalEffectIntensity = prefs.getFloat("experimentalEffectIntensityV1", 0.65f),
                themeAppearance = themeAppearance,
                customThemeName = prefs.getString("customThemeNameV4", "Моя").orEmpty(),
                customBackgroundColorArgb = prefs.getInt("customBackgroundColorArgbV4", 0xFF101014.toInt()),
                customSurfaceColorArgb = prefs.getInt("customSurfaceColorArgbV4", 0xFF18181F.toInt()),
                customTextColorArgb = prefs.getInt("customTextColorArgbV4", 0xFFF2F2F4.toInt()),
                customAccentColorArgb = prefs.getInt("customAccentColorArgbV4", 0xFF8FA7FF.toInt()),
                libraryViewMode = libraryViewMode,
                librarySortMode = librarySortMode,
                libraryGroupMode = libraryGroupMode,
                libraryShowContinue = prefs.getBoolean("libraryShowContinueV4", true),
                fontSizeSp = prefs.getFloat("fontSizeSp", 20f),
                lineHeightMultiplier = prefs.getFloat("lineHeightMultiplier", 1.45f),
                horizontalPaddingDp = prefs.getFloat("horizontalPaddingDp", 22f),
                topPaddingDp = prefs.getFloat("topPaddingDpV12", prefs.getFloat("verticalPaddingDp", 20f)),
                bottomPaddingDp = prefs.getFloat("bottomPaddingDpV12", prefs.getFloat("verticalPaddingDp", 20f)),
                paragraphSpacingDp = prefs.getFloat("paragraphSpacingDpV3", 10f),
                firstLineIndentDp = prefs.getFloat("firstLineIndentDpV3", 0f),
                fontMode = fontMode,
                textAlignment = textAlignment,
                hyphenationEnabled = prefs.getBoolean("hyphenationEnabledV8", false),
                chapterTitleScale = prefs.getFloat("chapterTitleScaleV12", 1.55f),
                chapterHeadingScale = prefs.getFloat("chapterHeadingScaleV12", 1.35f),
                centerChapterHeadings = prefs.getBoolean("centerChapterHeadingsV12", true),
                navigationMode = navigationMode,
                edgeTapZonesEnabled = prefs.getBoolean("edgeTapZonesEnabledV3", true),
                swipeNavigationEnabled = prefs.getBoolean("swipeNavigationEnabledV3", true),
                edgeTapFraction = prefs.getFloat("edgeTapFractionV3", 0.22f),
                chromeAutoHideSeconds = prefs.getInt("chromeAutoHideSecondsV3", 0),
                readerFullscreenEnabled = prefs.getBoolean("readerFullscreenEnabledV1", false),
                smoothPageTurns = prefs.getBoolean("smoothPageTurnsV3", true),
                pageTurnAnimation = pageTurnAnimation,
                pageTurnDurationMs = pageTurnDurationMs,
                fullscreenTransitionEnabled = prefs.getBoolean("fullscreenTransitionEnabledV13", false),
                fullscreenTransitionDurationMs = prefs.getInt("fullscreenTransitionDurationMsV13", 220),
                speedMode = speedMode,
                speedDialogueEnabled = prefs.getBoolean("speedDialogueEnabledV1", false),
                speedDialogueNames = prefs.getBoolean("speedDialogueNamesV1", true),
                speedDialogueIndicator = prefs.getBoolean("speedDialogueIndicatorV1", true),
                speedSpeakerStyle = runCatching { SpeedSpeakerStyle.valueOf(prefs.getString("speedSpeakerStyleV1", null).orEmpty()) }.getOrDefault(SpeedSpeakerStyle.BUBBLE),
                speedSpeakerPosition = runCatching { SpeedSpeakerPosition.valueOf(prefs.getString("speedSpeakerPositionV1", null).orEmpty()) }.getOrDefault(SpeedSpeakerPosition.ABOVE),
                speedSpeakerColor = runCatching { SpeedSpeakerColor.valueOf(prefs.getString("speedSpeakerColorV1", null).orEmpty()) }.getOrDefault(SpeedSpeakerColor.CHARACTER),
                speedSpeakerFontSizeSp = prefs.getFloat("speedSpeakerFontSizeSpV1", 20f),
                speedSpeakerBold = prefs.getBoolean("speedSpeakerBoldV1", true),
                speedSpeakerBackgroundAlpha = prefs.getFloat("speedSpeakerBackgroundAlphaV1", .16f),
                speedSpeakerGapDp = prefs.getFloat("speedSpeakerGapDpV1", 20f),
                speedDialogueShortReplies = prefs.getBoolean("speedDialogueShortRepliesV1", true),
                speedDialoguePauseEnabled = prefs.getBoolean("speedDialoguePauseEnabledV1", true),
                speedDialoguePauseMs = prefs.getInt("speedDialoguePauseMsV1", 220),
                speedWpm = prefs.getInt("speedWpm", 350),
                speedWordsPerFixation = prefs.getInt("speedWordsPerFixation", prefs.getInt("phraseWords", 1)),
                speedPacingMode = speedPacingMode,
                speedSmartChunking = prefs.getBoolean("speedSmartChunkingV5", true),
                speedParagraphPause = prefs.getBoolean("speedParagraphPauseV5", true),
                speedMicroPausesEnabled = prefs.getBoolean("speedMicroPausesEnabledV6", true),
                speedSentencePauseMs = prefs.getInt("speedSentencePauseMsV6", 170),
                speedClausePauseMs = prefs.getInt("speedClausePauseMsV6", 65),
                speedParagraphPauseMs = prefs.getInt("speedParagraphPauseMsV6", 260),
                speedAdaptiveSentencePause = prefs.getBoolean("speedAdaptiveSentencePauseV6", true),
                speedSentenceLengthBonusMs = prefs.getInt("speedSentenceLengthBonusMsV6", 120),
                speedLongWordSlowdownPercent = prefs.getInt("speedLongWordSlowdownPercentV6", 18),
                speedGradualAcceleration = prefs.getBoolean("speedGradualAccelerationV6", true),
                speedAccelerationSeconds = prefs.getInt("speedAccelerationSecondsV6", 5),
                speedAccelerationStartPercent = prefs.getInt("speedAccelerationStartPercentV6", 72),
                speedAdaptiveChunkSize = prefs.getBoolean("speedAdaptiveChunkSizeV7", true),
                speedPauseContextEnabled = prefs.getBoolean("speedPauseContextEnabledV7", true),
                speedLowerSegmentMode = speedLowerSegmentMode,
                speedReedyFlowEnabled = prefs.getBoolean("speedReedyFlowEnabledV9", true),
                speedPeripheralWords = prefs.getInt("speedPeripheralWordsV9", 3),
                speedTapToToggle = prefs.getBoolean("speedTapToToggleV10", true),
                speedVolumeKeysEnabled = prefs.getBoolean("speedVolumeKeysEnabledV10", true),
                speedShowPausePreview = prefs.getBoolean("speedShowPausePreviewV10", true),
                speedImagePauseMs = prefs.getInt("speedImagePauseMsV10", 2800),
                speedChapterIntroMs = prefs.getInt("speedChapterIntroMsV10", 1800),
                speedAutoRewindAfterPause = prefs.getBoolean("speedAutoRewindAfterPauseV7", true),
                speedAutoRewindThresholdSeconds = prefs.getInt("speedAutoRewindThresholdSecondsV7", 8),
                speedTextColorArgb = prefs.getInt("speedTextColorArgbV2", prefs.getInt("speedTextColorArgb", 0xFFF4F4F4.toInt())),
                speedFocusColorArgb = prefs.getInt("speedFocusColorArgbV2", prefs.getInt("speedFocusColorArgb", 0xFFFF5D73.toInt())),
                speedBackgroundColorArgb = prefs.getInt("speedBackgroundColorArgbV2", prefs.getInt("speedBackgroundColorArgb", 0xFF050507.toInt())),
                speedGuideColorArgb = prefs.getInt("speedGuideColorArgbV2", 0x66FFFFFF),
                speedFontSizeSp = prefs.getFloat("speedFontSizeSpV3", prefs.getFloat("speedFontSizeSpV2", 54f)),
                speedFixedFontSize = prefs.getBoolean("speedFixedFontSizeV11", true),
                speedMinFontSizeSp = prefs.getFloat("speedMinFontSizeSpV3", 24f),
                speedFontMode = speedFontMode,
                speedFontWeight = prefs.getInt("speedFontWeightV3", 650),
                speedLetterSpacingSp = prefs.getFloat("speedLetterSpacingSpV3", 0f),
                speedLineHeightMultiplier = prefs.getFloat("speedLineHeightMultiplierV3", 1.16f),
                speedTextWidthFraction = prefs.getFloat("speedTextWidthFractionV3", 0.90f),
                speedHorizontalFocusFraction = prefs.getFloat("speedHorizontalFocusFractionV11", 0.30f),
                speedVerticalPosition = prefs.getFloat("speedVerticalPositionV3", 0.50f),
                speedMaxLines = prefs.getInt("speedMaxLinesV3", 1),
                speedAutoFitText = prefs.getBoolean("speedAutoFitTextV3", true),
                speedFocusStyle = speedFocusStyle,
                speedContextOpacity = prefs.getFloat("speedContextOpacityV3", prefs.getFloat("speedContextOpacityV2", 0.45f)),
                speedShowGuide = prefs.getBoolean("speedShowGuideV3", prefs.getBoolean("speedShowGuideV2", true)),
                speedShowFocusTicks = prefs.getBoolean("speedShowFocusTicksV3", true),
                speedGuideHeightFraction = prefs.getFloat("speedGuideHeightFractionV3", 0.40f),
                speedGuideWidthDp = prefs.getFloat("speedGuideWidthDpV3", 1f),
                speedShowProgress = prefs.getBoolean("speedShowProgressV3", true),
                speedProgressMode = speedProgressMode,
                speedFocusBandEnabled = prefs.getBoolean("speedFocusBandEnabledV4", false),
                speedFocusBandAlpha = prefs.getFloat("speedFocusBandAlphaV4", 0.08f),
                speedUseReaderTheme = prefs.getBoolean("speedUseReaderThemeV5", false),
                customFontPath = prefs.getString("customFontPathV3", "").orEmpty(),
                customFontName = prefs.getString("customFontNameV3", "").orEmpty(),
                ocrEnabled = prefs.getBoolean("ocrEnabledV2", true),
                ocrLanguages = prefs.getString("ocrLanguagesV2", null)?.let { stored ->
                    stored.split('+').map { it.trim().lowercase() }.filter { it in setOf("rus", "ukr", "eng") }.toSet()
                } ?: setOf("rus", "eng"),
                ocrUseMlKitFallback = prefs.getBoolean("ocrUseMlKitFallbackV2", true),
            )
        )
    }

    private inline fun <reified T : Enum<T>> enumPref(key: String, fallback: T): T = runCatching {
        enumValueOf<T>(prefs.getString(key, fallback.name) ?: fallback.name)
    }.getOrDefault(fallback)

    private fun sanitize(value: ReaderSettings): ReaderSettings = value.copy(
        customThemeName = value.customThemeName.take(40).ifBlank { "Моя" },
        experimentalFrameStyle = value.experimentalFrameStyle?.takeIf {
            it == ReaderThemeMode.WASTELAND_TERMINAL || it == ReaderThemeMode.CYBERPUNK_NEON
        },
        experimentalPalette = value.experimentalPalette?.takeUnless {
            it == ReaderThemeMode.WASTELAND_TERMINAL || it == ReaderThemeMode.CYBERPUNK_NEON
        },
        experimentalEffectIntensity = value.experimentalEffectIntensity.coerceIn(0f, 1f),
        narrationVoiceId = value.narrationVoiceId.take(200),
        narrationRate = value.narrationRate.coerceIn(0.6f, 1.4f),
        narrationPitch = value.narrationPitch.coerceIn(0.75f, 1.25f),
        fontSizeSp = value.fontSizeSp.coerceIn(13f, 38f),
        lineHeightMultiplier = value.lineHeightMultiplier.coerceIn(1.05f, 2.2f),
        horizontalPaddingDp = value.horizontalPaddingDp.coerceIn(6f, 64f),
        topPaddingDp = value.topPaddingDp.coerceIn(0f, 72f),
        bottomPaddingDp = value.bottomPaddingDp.coerceIn(0f, 72f),
        paragraphSpacingDp = value.paragraphSpacingDp.coerceIn(0f, 32f),
        firstLineIndentDp = value.firstLineIndentDp.coerceIn(0f, 40f),
        chapterTitleScale = value.chapterTitleScale.coerceIn(1.15f, 2.40f),
        chapterHeadingScale = value.chapterHeadingScale.coerceIn(1.05f, 2.00f),
        edgeTapFraction = value.edgeTapFraction.coerceIn(0.12f, 0.36f),
        chromeAutoHideSeconds = value.chromeAutoHideSeconds.coerceIn(0, 15),
        pageTurnDurationMs = value.pageTurnDurationMs.coerceIn(80, 700),
        fullscreenTransitionDurationMs = value.fullscreenTransitionDurationMs.coerceIn(100, 600),
        speedDialoguePauseMs = value.speedDialoguePauseMs.coerceIn(0, 500),
        speedSpeakerFontSizeSp = value.speedSpeakerFontSizeSp.takeIf { it.isFinite() }?.coerceIn(12f, 40f) ?: 20f,
        speedSpeakerGapDp = value.speedSpeakerGapDp.takeIf { it.isFinite() }?.coerceIn(12f, 64f) ?: 20f,
        speedSpeakerBackgroundAlpha = value.speedSpeakerBackgroundAlpha.takeIf { it.isFinite() }?.coerceIn(0f, .5f) ?: .16f,
        speedWpm = value.speedWpm.coerceIn(100, 3000),
        speedWordsPerFixation = value.speedWordsPerFixation.coerceIn(1, 7),
        speedSentencePauseMs = value.speedSentencePauseMs.coerceIn(0, 700),
        speedClausePauseMs = value.speedClausePauseMs.coerceIn(0, 350),
        speedParagraphPauseMs = value.speedParagraphPauseMs.coerceIn(0, 1_200),
        speedSentenceLengthBonusMs = value.speedSentenceLengthBonusMs.coerceIn(0, 400),
        speedLongWordSlowdownPercent = value.speedLongWordSlowdownPercent.coerceIn(0, 60),
        speedAccelerationSeconds = value.speedAccelerationSeconds.coerceIn(1, 20),
        speedAccelerationStartPercent = value.speedAccelerationStartPercent.coerceIn(45, 100),
        speedAutoRewindThresholdSeconds = value.speedAutoRewindThresholdSeconds.coerceIn(3, 60),
        speedPeripheralWords = value.speedPeripheralWords.coerceIn(0, 6),
        speedImagePauseMs = value.speedImagePauseMs.coerceIn(500, 12_000),
        speedChapterIntroMs = value.speedChapterIntroMs.coerceIn(0, 8_000),
        speedFontSizeSp = value.speedFontSizeSp.coerceIn(24f, 104f),
        speedMinFontSizeSp = value.speedMinFontSizeSp.coerceIn(16f, value.speedFontSizeSp.coerceIn(24f, 104f)),
        speedFontWeight = value.speedFontWeight.coerceIn(300, 900),
        speedLetterSpacingSp = value.speedLetterSpacingSp.coerceIn(-2f, 6f),
        speedLineHeightMultiplier = value.speedLineHeightMultiplier.coerceIn(1.0f, 1.8f),
        speedTextWidthFraction = value.speedTextWidthFraction.coerceIn(0.55f, 1f),
        speedHorizontalFocusFraction = value.speedHorizontalFocusFraction.coerceIn(0.18f, 0.50f),
        speedVerticalPosition = value.speedVerticalPosition.coerceIn(0.25f, 0.75f),
        speedMaxLines = value.speedMaxLines.coerceIn(1, 3),
        speedContextOpacity = value.speedContextOpacity.coerceIn(0.08f, 1f),
        speedGuideHeightFraction = value.speedGuideHeightFraction.coerceIn(0.15f, 0.80f),
        speedGuideWidthDp = value.speedGuideWidthDp.coerceIn(0.5f, 4f),
        speedFocusBandAlpha = value.speedFocusBandAlpha.coerceIn(0.02f, 0.35f),
        customFontPath = value.customFontPath.take(1024),
        customFontName = value.customFontName.take(120),
        ocrLanguages = value.ocrLanguages.filter { it in setOf("rus", "ukr", "eng") }.toSet(),
    )
}
