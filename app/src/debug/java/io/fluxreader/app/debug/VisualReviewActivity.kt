package io.fluxreader.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import io.fluxreader.app.AppViewModel
import io.fluxreader.app.data.ReaderThemeMode
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
            speedDialogueEnabled = true,
            speedDialogueShortReplies = scene != "ellipsis",
            speedMode = SpeedMode.RSVP,
            speedFontSizeSp = 35f,
            speedHorizontalFocusFraction = 0.50f,
            speedWpm = 250,
            speedLowerSegmentMode = SpeedLowerSegmentMode.OFF,
            speedChapterIntroMs = 0,
        ) }
        setContent { FluxReaderApp(viewModel) }
        if (scene == "settings") {
            viewModel.showSettings()
            return
        }
        lifecycleScope.launch {
            val fixtureName = "Flux-Visual-Review.txt"
            val fixture = File(File(cacheDir, "updates").apply { mkdirs() }, fixtureName)
            fixture.writeText("""
                — Добрый вечер! — сказала Анна.

                — Садитесь, садитесь, прошу вас! И побыстрее, сегодня у нас много работы!

                — …то отсюда следует, что всё получится.

                Анна закрыла книгу. В комнате стало тихо.
            """.trimIndent())
            if (viewModel.books.value.none { it.originalName == fixtureName }) {
                viewModel.importBook(FileProvider.getUriForFile(this@VisualReviewActivity,
                    "$packageName.updates.fileprovider", fixture))
            }
            val book = withTimeout(15_000) {
                viewModel.books.first { list -> list.any { it.originalName == fixtureName } }
                    .first { it.originalName == fixtureName }
            }
            viewModel.openSpeedAt(book.id, 0, 0)
            val state = withTimeout(15_000) {
                viewModel.speed.first { !it.loading && it.bookId == book.id && it.chapterText.isNotBlank() }
            }
            val target = when (scene) { "long" -> "Садитесь"; "ellipsis" -> "…то"; else -> "Добрый" }
            viewModel.speedSeekToOffset(state.chapterText.indexOf(target).coerceAtLeast(0))
            viewModel.speedPause()
        }
    }
}
