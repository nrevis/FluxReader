package io.fluxreader.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import io.fluxreader.app.AppViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.fluxreader.app.ui.*
import io.fluxreader.app.update.*
import io.fluxreader.app.data.SpeedSpeakerColor
import io.fluxreader.app.data.ReaderThemeMode
import io.fluxreader.app.data.SpeedSpeakerStyle
import io.fluxreader.app.data.SpeedSpeakerPosition
import io.fluxreader.app.data.SpeedMode
import io.fluxreader.app.data.SpeedLowerSegmentMode
import io.fluxreader.app.ui.FluxReaderApp
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Deterministic entry into the real app for local visual review. Absent from release APKs. */
class VisualReviewActivity : ComponentActivity() {
    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val scene = intent.getStringExtra("scene") ?: "settings"
        val theme = intent.getStringExtra("theme") ?: "wasteland"
        viewModel.setAutoUpdateCheck(false)
        viewModel.updateSettings { it.copy(
            experimentalDesignEnabled = theme != "standard",
            experimentalFrameStyle = if (theme == "cyberpunk") ReaderThemeMode.CYBERPUNK_NEON else ReaderThemeMode.WASTELAND_TERMINAL,
            experimentalPalette = null,
            experimentalCrtEnabled = intent.getBooleanExtra("effects", false),
            speedDialogueEnabled = intent.getBooleanExtra("dialogues", true),
            speedDialogueTextColor = intent.getBooleanExtra("speechColor", false),
            speedShowGuide = intent.getBooleanExtra("guide", true),
            speedGuideStyle = intent.getStringExtra("guideStyle")?.let { name ->
                runCatching { io.fluxreader.app.data.SpeedGuideStyle.valueOf(name.uppercase()) }.getOrNull()
            } ?: io.fluxreader.app.data.SpeedGuideStyle.TICKS,
            speedDialogueBookAnalysis = intent.getBooleanExtra("bookAnalysis", false),
            speedSpeakerColor = when (intent.getStringExtra("speakerColor")) {
                "theme" -> SpeedSpeakerColor.THEME
                "character" -> SpeedSpeakerColor.CHARACTER
                "blue" -> SpeedSpeakerColor.BLUE
                "auto" -> SpeedSpeakerColor.CHARACTER
                else -> it.speedSpeakerColor
            },
            speedSpeakerStyle = when (intent.getStringExtra("label")) { "text" -> SpeedSpeakerStyle.TEXT; "chip" -> SpeedSpeakerStyle.CHIP; else -> SpeedSpeakerStyle.BUBBLE },
            speedSpeakerFontSizeSp = intent.getIntExtra("nameSize", 24).toFloat(),
            speedSpeakerPosition = if (intent.getBooleanExtra("below", false)) SpeedSpeakerPosition.BELOW else SpeedSpeakerPosition.ABOVE,
            speedDialogueShortReplies = scene != "ellipsis",
            speedMode = intent.getStringExtra("speedMode")?.let { name -> runCatching { SpeedMode.valueOf(name.uppercase()) }.getOrNull() } ?: SpeedMode.RSVP,
            speedWordsPerFixation = intent.getIntExtra("words", 1),
            speedFontSizeSp = 35f,
            speedHorizontalFocusFraction = 0.50f,
            speedWpm = 250,
            speedLowerSegmentMode = SpeedLowerSegmentMode.OFF,
            speedChapterIntroMs = intent.getIntExtra("introMs", 0),
        ) }
        setContent { FluxReaderApp(viewModel) }
        if (scene == "voices") {
            intent.getStringExtra("voice")?.let { voice -> viewModel.updateSettings { it.copy(narrationVoiceId = voice) } }
            setContent {
                val settings by viewModel.settings.collectAsState()
                val narrator = remember { io.fluxreader.app.reader.ReaderNarrator(applicationContext) }
                val status by narrator.status.collectAsState()
                DisposableEffect(narrator) { onDispose { narrator.close() } }
                LaunchedEffect(settings) { narrator.setPreferences(settings) }
                LaunchedEffect(Unit) {
                    narrator.setDocument("visual-speech", 0, io.fluxreader.app.data.ChapterContent(0, "Проверка озвучки", listOf(
                        io.fluxreader.app.data.ReaderBlock.Text("Ветер стихал. Наступал вечер, и на горизонте зажигались первые огни."),
                        io.fluxreader.app.data.ReaderBlock.Text("— Добрый вечер! — сказала Анна. — Мы встретимся завтра, в девять часов."))), 0, 0)
                }
                LaunchedEffect(status.ready) { if (status.ready && intent.getBooleanExtra("autoSpeech", false)) narrator.play() }
                FluxTheme(settings) {
                    NarrationSheet(status, settings, { finish() }, narrator::play, narrator::pause, narrator::stop,
                        narrator::skipPrevious, narrator::skipNext, narrator::preview, viewModel::updateSettings)
                }
            }
            return
        }
        if (scene == "updates") {
            val phase = runCatching { UpdatePhase.valueOf(intent.getStringExtra("phase") ?: "AVAILABLE") }.getOrDefault(UpdatePhase.AVAILABLE)
            setContent {
                val settings by viewModel.settings.collectAsState()
                var state by remember { mutableStateOf(GitHubUpdateState(phase = phase, autoCheck = false,
                    progressPercent = 50, message = when (phase) {
                        UpdatePhase.ERROR -> "Не удалось проверить обновления"
                        UpdatePhase.CHECKING -> "Проверяем обновления…"
                        UpdatePhase.CURRENT -> "Установлена актуальная версия"
                        UpdatePhase.DOWNLOADING -> "Скачивание: 50%"
                        UpdatePhase.READY -> "Обновление готово к установке"
                        else -> "Доступна новая версия"
                    }, release = if (phase in listOf(UpdatePhase.IDLE, UpdatePhase.CHECKING, UpdatePhase.CURRENT)) null else UpdateRelease(
                        "v0.2.0-alpha.102", "Flux Reader alpha.102", "Flux Reader alpha.102\n\n• Оформление диалогов в цветах темы.\n• Крупные кнопки полного экрана.\n• Компактные меню и отдельный список изменений.",
                        RELEASES_WEB_URL, "", "preview.apk", 0L, null, null, null))) }
                FluxTheme(settings) {
                    val content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit = {
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModalSheetHeader("Обновления · проверка", onClose = { finish() })
                            GitHubUpdateContent(state, { state = state.copy(autoCheck = it) },
                                { state = state.copy(includePreReleases = it) },
                                { state = state.copy(phase = UpdatePhase.CURRENT, message = "Установлена актуальная версия", release = null) },
                                { state = state.copy(phase = UpdatePhase.DOWNLOADING, message = "Скачивание: 50%") },
                                { state = state.copy(message = "Проверка кнопки установки") }, {})
                        }
                    }
                    if (theme == "wasteland") WastelandDeviceFrame(content = content)
                    else if (theme == "cyberpunk") CyberpunkDeviceFrame(content = content)
                    else Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), content = content)
                    DeviceStatusCrown(false, theme != "standard")
                }
            }
            return
        }
        if (scene == "audio") {
            lifecycleScope.launch {
                val audio = File(File(cacheDir, "updates").apply { mkdirs() }, "Flux-Visual-Review.wav")
                if (!audio.exists()) {
                    val bytes = java.nio.ByteBuffer.allocate(64044).order(java.nio.ByteOrder.LITTLE_ENDIAN)
                    bytes.put("RIFF".toByteArray()).putInt(64036).put("WAVEfmt ".toByteArray()).putInt(16)
                    bytes.putShort(1).putShort(1).putInt(16000).putInt(32000).putShort(2).putShort(16)
                    bytes.put("data".toByteArray()).putInt(64000)
                    audio.writeBytes(bytes.array())
                }
                // Audiobooks require a persistable SAF grant. The visual harness copies this
                // silent file to Downloads and selects it through the real document picker.
                viewModel.showAudiobooks()
            }
            return
        }
        if (scene == "library") return
        if (scene == "settings") {
            viewModel.showSettings()
            return
        }
        lifecycleScope.launch {
            val localBook = intent.getStringExtra("bookFile")?.takeIf { File(it).name == it }
            val fixtureName = localBook ?: when (scene) {
                "pdf" -> "Flux-Visual-Review.pdf"
                "colors" -> "Flux-Visual-Review-103-colors.txt"
                else -> "Flux-Visual-Review-98.txt"
            }
            val fixture = File(File(cacheDir, "updates").apply { mkdirs() }, fixtureName)
            if (scene == "pdf" && localBook == null) {
                val document = android.graphics.pdf.PdfDocument()
                repeat(3) { pageNumber ->
                    val page = document.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(612, 792, pageNumber + 1).create())
                    val paint = android.graphics.Paint().apply { color = android.graphics.Color.BLACK; textSize = 22f }
                    page.canvas.drawColor(android.graphics.Color.WHITE)
                    page.canvas.drawText("Flux Reader - PDF ${pageNumber + 1}", 40f, 70f, paint)
                    paint.textSize = 16f
                    repeat(16) { line -> page.canvas.drawText("Interface review, page ${pageNumber + 1}, line ${line + 1}.", 40f, 120f + line * 28f, paint) }
                    document.finishPage(page)
                }
                fixture.outputStream().use { document.writeTo(it) }; document.close()
            }
            if (localBook == null && scene != "pdf") fixture.writeText("""
                — Добрый вечер! — сказала Анна.

                — Садитесь, садитесь, прошу вас! И побыстрее, сегодня у нас много работы!

                — …то отсюда следует, что всё получится.

                — Да, — сказала Анна Соколова, — я обязательно приду завтра.

                Павел спросил:
                — Ты останешься здесь?

                Анна закрыла книгу. В комнате стало тихо.
            """.trimIndent())
            if (scene == "colors" && localBook == null) fixture.appendText("\n\nПроверка цветов alpha.103.")
            if (viewModel.books.value.none { it.originalName == fixtureName }) {
                viewModel.importBook(FileProvider.getUriForFile(this@VisualReviewActivity,
                    "$packageName.updates.fileprovider", fixture))
            }
            val book = withTimeout(15_000) {
                viewModel.books.first { list -> list.any { it.originalName == fixtureName } }
                    .first { it.originalName == fixtureName }
            }
            if (scene == "reader" || scene == "pdf") {
                viewModel.openReader(book.id)
                return@launch
            }
            val chapter = intent.getIntExtra("chapter", 0)
            viewModel.openSpeedAt(book.id, chapter, 0)
            val state = withTimeout(15_000) {
                viewModel.speed.first { !it.loading && it.bookId == book.id && it.chapterIndex == chapter && it.chapterText.isNotBlank() }
            }
            val target = intent.getStringExtra("findText") ?: when (scene) { "unknown", "long" -> "Садитесь"; "ellipsis" -> "…то"; "interrupted" -> "я обязательно"; "prefix" -> "Ты останешься"; else -> "Добрый" }
            viewModel.speedSeekToOffset(state.chapterText.indexOf(target).coerceAtLeast(0))
            viewModel.speedPause()
        }
    }
}
