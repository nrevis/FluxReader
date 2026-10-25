package io.fluxreader.app.data

enum class BookFormat { EPUB, FB2, TXT, PDF }

enum class ReaderThemeMode {
    OLED,
    DARK,
    PAPER,
    SEPIA,
    NORD,
    DRACULA,
    SOLARIZED_DARK,
    CATPPUCCIN,
    GRUVBOX,
    ROSE_PINE_MOON,
    CMX_PURPLE,
    WASTELAND_TERMINAL,
    CYBERPUNK_NEON,
    MONO,
    MONO_SOFT,
    CUSTOM,
}

enum class ThemeAppearance { SYSTEM, LIGHT, DARK }

enum class LibraryViewMode { GRID, COMPACT, LIST }
enum class LibrarySortMode { RECENT, TITLE, AUTHOR, SERIES, ADDED, PROGRESS }
enum class LibraryGroupMode { NONE, FOLDER, CUSTOM, SERIES, AUTHOR }
enum class PageTurnAnimation { NONE, CURL, SLIDE, FADE, ZOOM }

enum class SpeedMode { RSVP, PHRASE, FOCUS }
enum class SpeedSpeakerStyle { TEXT, CHIP, BUBBLE }
enum class SpeedSpeakerPosition { ABOVE, BELOW }
enum class SpeedSpeakerColor { CHARACTER, THEME, BLUE, GREEN, AMBER, ROSE, VIOLET }
enum class SpeedFocusStyle { CHARACTER, WORD }
enum class SpeedPacingMode { CONSTANT, SMART, DEEP }
enum class SpeedLowerSegmentMode { OFF, CURRENT, CONTEXT }
enum class SpeedProgressMode { CHAPTER, BOOK, BOTH }
enum class ReaderNavigationMode { SCROLL, SCREEN_PAGING }
enum class ReaderFontMode { SYSTEM, SERIF, SANS, MONO, JETBRAINS_NERD }
enum class ReaderTextAlignment { LEFT, JUSTIFY }

data class BookRecord(
    val id: String,
    val title: String,
    val author: String,
    val filePath: String,
    val originalName: String,
    val format: BookFormat,
    val series: String = "",
    val seriesIndex: Float = 0f,
    val groups: List<String> = emptyList(),
    val folderPaths: List<String> = emptyList(),
    val coverPath: String = "",
    val description: String = "",
    val publisher: String = "",
    val language: String = "",
    val year: String = "",
    val contentHash: String = "",
    val metadataLocked: Boolean = false,
    val progress: Float = 0f,
    val readerChapter: Int = 0,
    val readerBlockIndex: Int = 0,
    val readerBlockOffsetPx: Int = 0,
    val readerBlockCharOffset: Int = 0,
    val speedChapter: Int = 0,
    val speedOffset: Int = 0,
    val readingPositionSource: ReadingPositionSource? = null,
    val pdfPage: Int = 0,
    val chapterCount: Int = 0,
    val lastOpenedAt: Long = 0L,
    val addedAt: Long = 0L,
)

/**
 * Experimental UI is opt-in; when enabled an explicit frame wins over ordinary theme.
 * Without an explicit frame, older Wasteland/Cyberpunk choices are migrated; all other
 * ordinary palettes default to Wasteland while preserving the ordinary palette value.
 */
val ReaderSettings.experimentalFrameTheme: ReaderThemeMode
    get() = experimentalFrameStyle ?: if (theme == ReaderThemeMode.CYBERPUNK_NEON) {
        ReaderThemeMode.CYBERPUNK_NEON
    } else ReaderThemeMode.WASTELAND_TERMINAL

val ReaderSettings.useWastelandDesign: Boolean
    get() = experimentalDesignEnabled && experimentalFrameTheme == ReaderThemeMode.WASTELAND_TERMINAL

val ReaderSettings.useCyberpunkDesign: Boolean
    get() = experimentalDesignEnabled && experimentalFrameTheme == ReaderThemeMode.CYBERPUNK_NEON

/** Used by all themed icons and chrome: disabled experimental UI looks like regular Flux. */
val ReaderSettings.activeUiTheme: ReaderThemeMode
    get() = if (experimentalDesignEnabled) experimentalFrameTheme else when (theme) {
        ReaderThemeMode.WASTELAND_TERMINAL, ReaderThemeMode.CYBERPUNK_NEON -> ReaderThemeMode.OLED
        else -> theme
    }

data class ReaderSettings(
    val theme: ReaderThemeMode = ReaderThemeMode.OLED,
    // Opt-in: picking a palette must NEVER silently enable experimental layouts/icons.
    // False for existing installations on migration as well as for new users.
    val experimentalDesignEnabled: Boolean = false,
    // Null values preserve the exact alpha.86 Wasteland/Cyberpunk styling after migration.
    // Frame geometry and palette are separately selectable; neither changes reading positions.
    val experimentalFrameStyle: ReaderThemeMode? = null,
    val experimentalPalette: ReaderThemeMode? = null,
    // Decorative screen effects are independent of the experimental layout.
    val experimentalCrtEnabled: Boolean = true,
    val experimentalScanlinesEnabled: Boolean = true,
    val experimentalGlowEnabled: Boolean = true,
    val experimentalNoiseEnabled: Boolean = false,
    val experimentalVignetteEnabled: Boolean = true,
    val experimentalEffectIntensity: Float = 0.65f,
    val narrationVoiceId: String = "",
    val narrationRate: Float = 0.94f,
    val narrationPitch: Float = 1.0f,
    val narrationAutoNextChapter: Boolean = true,
    val themeAppearance: ThemeAppearance = ThemeAppearance.SYSTEM,
    val customThemeName: String = "Моя",
    val customBackgroundColorArgb: Int = 0xFF101014.toInt(),
    val customSurfaceColorArgb: Int = 0xFF18181F.toInt(),
    val customTextColorArgb: Int = 0xFFF2F2F4.toInt(),
    val customAccentColorArgb: Int = 0xFF8FA7FF.toInt(),
    val libraryViewMode: LibraryViewMode = LibraryViewMode.GRID,
    val librarySortMode: LibrarySortMode = LibrarySortMode.RECENT,
    val libraryGroupMode: LibraryGroupMode = LibraryGroupMode.NONE,
    val libraryShowContinue: Boolean = true,
    val fontSizeSp: Float = 20f,
    val lineHeightMultiplier: Float = 1.45f,
    val horizontalPaddingDp: Float = 22f,
    val topPaddingDp: Float = 20f,
    val bottomPaddingDp: Float = 20f,
    val paragraphSpacingDp: Float = 10f,
    val firstLineIndentDp: Float = 0f,
    val fontMode: ReaderFontMode = ReaderFontMode.SERIF,
    val textAlignment: ReaderTextAlignment = ReaderTextAlignment.LEFT,
    val hyphenationEnabled: Boolean = false,
    val chapterTitleScale: Float = 1.55f,
    val chapterHeadingScale: Float = 1.35f,
    val centerChapterHeadings: Boolean = true,
    val navigationMode: ReaderNavigationMode = ReaderNavigationMode.SCROLL,
    val edgeTapZonesEnabled: Boolean = true,
    val swipeNavigationEnabled: Boolean = true,
    val edgeTapFraction: Float = 0.22f,
    val chromeAutoHideSeconds: Int = 0,
    val readerFullscreenEnabled: Boolean = false,
    val smoothPageTurns: Boolean = true,
    val pageTurnAnimation: PageTurnAnimation = PageTurnAnimation.CURL,
    val pageTurnDurationMs: Int = 260,
    val fullscreenTransitionEnabled: Boolean = false,
    val fullscreenTransitionDurationMs: Int = 220,
    val speedMode: SpeedMode = SpeedMode.RSVP,
    // All dialogue features are experimental and disabled until the main switch is enabled.
    val speedDialogueEnabled: Boolean = false,
    val speedDialogueNames: Boolean = true,
    val speedDialogueBookAnalysis: Boolean = false,
    val speedDialogueIndicator: Boolean = true,
    val speedSpeakerStyle: SpeedSpeakerStyle = SpeedSpeakerStyle.BUBBLE,
    val speedSpeakerPosition: SpeedSpeakerPosition = SpeedSpeakerPosition.ABOVE,
    val speedSpeakerColor: SpeedSpeakerColor = SpeedSpeakerColor.CHARACTER,
    val speedSpeakerFontSizeSp: Float = 20f,
    val speedSpeakerBold: Boolean = true,
    val speedSpeakerBackgroundAlpha: Float = 0.16f,
    val speedSpeakerGapDp: Float = 20f,
    val speedDialogueShortReplies: Boolean = true,
    val speedDialoguePauseEnabled: Boolean = true,
    val speedDialoguePauseMs: Int = 220,
    val speedWpm: Int = 350,
    val speedWordsPerFixation: Int = 1,
    val speedPacingMode: SpeedPacingMode = SpeedPacingMode.SMART,
    val speedSmartChunking: Boolean = true,
    val speedParagraphPause: Boolean = true,
    val speedMicroPausesEnabled: Boolean = true,
    val speedSentencePauseMs: Int = 170,
    val speedClausePauseMs: Int = 65,
    val speedParagraphPauseMs: Int = 260,
    val speedAdaptiveSentencePause: Boolean = true,
    val speedSentenceLengthBonusMs: Int = 120,
    val speedLongWordSlowdownPercent: Int = 18,
    val speedGradualAcceleration: Boolean = true,
    val speedAccelerationSeconds: Int = 5,
    val speedAccelerationStartPercent: Int = 72,
    val speedAdaptiveChunkSize: Boolean = true,
    val speedPauseContextEnabled: Boolean = true,
    val speedLowerSegmentMode: SpeedLowerSegmentMode = SpeedLowerSegmentMode.CURRENT,
    val speedReedyFlowEnabled: Boolean = true,
    val speedPeripheralWords: Int = 3,
    val speedTapToToggle: Boolean = true,
    val speedVolumeKeysEnabled: Boolean = true,
    val speedShowPausePreview: Boolean = true,
    val speedImagePauseMs: Int = 2800,
    val speedChapterIntroMs: Int = 1800,
    val speedAutoRewindAfterPause: Boolean = true,
    val speedAutoRewindThresholdSeconds: Int = 8,
    val speedTextColorArgb: Int = 0xFFF4F4F4.toInt(),
    val speedFocusColorArgb: Int = 0xFFFF5D73.toInt(),
    val speedBackgroundColorArgb: Int = 0xFF050507.toInt(),
    val speedGuideColorArgb: Int = 0x66FFFFFF,
    val speedFontSizeSp: Float = 54f,
    val speedFixedFontSize: Boolean = true,
    val speedMinFontSizeSp: Float = 24f,
    val speedFontMode: ReaderFontMode = ReaderFontMode.SANS,
    val speedFontWeight: Int = 650,
    val speedLetterSpacingSp: Float = 0f,
    val speedLineHeightMultiplier: Float = 1.16f,
    val speedTextWidthFraction: Float = 0.90f,
    val speedHorizontalFocusFraction: Float = 0.30f,
    val speedVerticalPosition: Float = 0.50f,
    val speedMaxLines: Int = 1,
    val speedAutoFitText: Boolean = true,
    val speedFocusStyle: SpeedFocusStyle = SpeedFocusStyle.CHARACTER,
    val speedContextOpacity: Float = 0.45f,
    val speedShowGuide: Boolean = true,
    val speedShowFocusTicks: Boolean = true,
    val speedGuideHeightFraction: Float = 0.40f,
    val speedGuideWidthDp: Float = 1f,
    val speedShowProgress: Boolean = true,
    val speedProgressMode: SpeedProgressMode = SpeedProgressMode.CHAPTER,
    val speedFocusBandEnabled: Boolean = false,
    val speedFocusBandAlpha: Float = 0.08f,
    val speedUseReaderTheme: Boolean = false,
    val customFontPath: String = "",
    val customFontName: String = "",
    val ocrEnabled: Boolean = true,
    val ocrLanguages: Set<String> = setOf("rus", "eng"),
    val ocrUseMlKitFallback: Boolean = true,
)


data class FolderImportResult(
    val added: Int,
    val linkedExisting: Int,
    val skipped: Int,
    val folderCount: Int,
    val firstError: String = "",
    val addedBookIds: List<String> = emptyList(),
)

data class SourceImportResult(
    val added: Int,
    val linkedExisting: Int = 0,
    val skipped: Int = 0,
    val folderCount: Int = 0,
    val firstError: String = "",
    val addedBookIds: List<String> = emptyList(),
    val archive: Boolean = false,
)

enum class ImportPhase { PREPARING, COPYING_ARCHIVE, IMPORTING_BOOKS, SCANNING_FOLDER }

data class SourceImportProgress(
    val phase: ImportPhase,
    val label: String = "",
    val current: Int = 0,
    val total: Int = 0,
    val bytesCopied: Long = 0L,
    val bytesTotal: Long = 0L,
)

data class BookMetadata(
    val title: String,
    val author: String = "",
    val series: String = "",
    val seriesIndex: Float = 0f,
    val description: String = "",
    val publisher: String = "",
    val language: String = "",
    val year: String = "",
)

data class ChapterRef(
    val index: Int,
    val title: String,
    val sourceRef: String,
    val anchors: List<String> = emptyList(),
)

data class BookOutline(
    val title: String,
    val author: String,
    val chapters: List<ChapterRef>,
)

enum class TextRole { TITLE, HEADING, PARAGRAPH, LIST_ITEM, QUOTE, POEM, TABLE, CODE }

data class ReaderLink(
    val start: Int,
    val end: Int,
    val target: String,
    val external: Boolean = false,
)

sealed interface ReaderBlock {
    val anchors: List<String>

    data class Text(
        val text: String,
        val role: TextRole = TextRole.PARAGRAPH,
        override val anchors: List<String> = emptyList(),
        val links: List<ReaderLink> = emptyList(),
        val listDepth: Int = 0,
        val listMarker: String = "",
    ) : ReaderBlock

    data class Image(
        val ref: String,
        val alt: String = "",
        override val anchors: List<String> = emptyList(),
    ) : ReaderBlock
}

data class ChapterContent(
    val index: Int,
    val title: String,
    val blocks: List<ReaderBlock>,
) {
    fun plainText(): String = buildString {
        if (title.isNotBlank()) append(title).append("\n\n")
        blocks.forEach { block ->
            if (block is ReaderBlock.Text && block.text.isNotBlank()) {
                append(block.text.trim()).append("\n\n")
            }
        }
    }.trim()
}
