package io.fluxreader.app.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import io.fluxreader.app.AppViewModel
import io.fluxreader.app.BuildConfig
import io.fluxreader.app.data.LibraryGroupMode
import io.fluxreader.app.data.LibraryViewMode
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.ReaderThemeMode
import io.fluxreader.app.data.useWastelandDesign
import io.fluxreader.app.data.useCyberpunkDesign
import io.fluxreader.app.data.SpeedMode
import io.fluxreader.app.data.SpeedPacingMode
import io.fluxreader.app.data.ThemeAppearance

private enum class SettingsSection(val title: String) {
    APPEARANCE("Оформление"),
    READING("Чтение"),
    LIBRARY("Библиотека"),
    SPEED("Скорочтение"),
    OCR("OCR"),
    ABOUT("О программе"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: ReaderSettings,
    viewModel: AppViewModel,
    onBack: () -> Unit,
) {
    var sectionName by rememberSaveable { mutableStateOf(SettingsSection.APPEARANCE.name) }
    val section = runCatching { SettingsSection.valueOf(sectionName) }.getOrDefault(SettingsSection.APPEARANCE)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            if (settings.useWastelandDesign || settings.useCyberpunkDesign) {
                ExperimentalTopBar(
                    title = "Настройки",
                    subtitle = section.title,
                    navigation = {
                        IconButton(onClick = onBack) {
                            FluxThemedIcon(FluxIconRole.BACK, Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text("Настройки")
                            Text(section.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    },
                )
            }
        },
        containerColor = if (settings.useWastelandDesign || settings.useCyberpunkDesign) LocalExperimentalColors.current.background else MaterialTheme.colorScheme.background,
        contentColor = if (settings.useWastelandDesign || settings.useCyberpunkDesign) LocalExperimentalColors.current.text else MaterialTheme.colorScheme.onBackground,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            SettingsSectionTabs(
                selected = section,
                onSelect = { sectionName = it.name },
            )
            key(section) {
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (section) {
                        SettingsSection.APPEARANCE -> ReaderPreferencesControls(
                            settings = settings,
                            onUpdate = viewModel::updateSettings,
                            includeTheme = true,
                            includeProfiles = false,
                            includeNavigation = false,
                            includeText = false,
                        )
                        SettingsSection.READING -> ReaderPreferencesControls(
                            settings = settings,
                            onUpdate = viewModel::updateSettings,
                            includeTheme = false,
                            includeProfiles = true,
                            includeNavigation = true,
                            includeText = true,
                        )
                        SettingsSection.LIBRARY -> LibrarySettings(settings, viewModel)
                        SettingsSection.SPEED -> SpeedSettings(settings, viewModel)
                        SettingsSection.OCR -> OcrSettings(settings, viewModel)
                        SettingsSection.ABOUT -> AboutSettings(viewModel)
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }
}


@Composable
private fun SettingsSectionTabs(
    selected: SettingsSection,
    onSelect: (SettingsSection) -> Unit,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val stepPx = with(LocalDensity.current) { 220.dp.roundToPx() }

    val leadingRail = if (scrollState.canScrollBackward) 34.dp else 0.dp
    val trailingRail = if (scrollState.canScrollForward) 34.dp else 0.dp

    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        Box(Modifier.fillMaxWidth()) {
            ExperimentalPanel(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                contentPadding = 5.dp,
            ) {
                Row(
                    Modifier.fillMaxWidth()
                        .padding(start = leadingRail, end = trailingRail)
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    SettingsSection.entries.forEach { item ->
                        val bringIntoView = remember { BringIntoViewRequester() }
                        LaunchedEffect(selected) {
                            if (item == selected) bringIntoView.bringIntoView()
                        }
                        ExperimentalTab(
                            modifier = Modifier.bringIntoViewRequester(bringIntoView),
                            label = item.title,
                            selected = item == selected,
                            onClick = { onSelect(item) },
                        )
                    }
                }
            }
            if (scrollState.canScrollBackward) {
                IconButton(
                    onClick = { scope.launch { scrollState.animateScrollTo((scrollState.value - stepPx).coerceAtLeast(0)) } },
                    modifier = Modifier.align(Alignment.CenterStart).size(36.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Предыдущие разделы",
                        tint = LocalFluxThemeMode.current.chromeAccent)
                }
            }
            if (scrollState.canScrollForward) {
                IconButton(
                    onClick = { scope.launch { scrollState.animateScrollTo((scrollState.value + stepPx).coerceAtMost(scrollState.maxValue)) } },
                    modifier = Modifier.align(Alignment.CenterEnd).size(36.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Следующие разделы",
                        tint = LocalFluxThemeMode.current.chromeAccent)
                }
            }
        }
        return
    }

    Box(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = leadingRail, end = trailingRail)
                .horizontalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            SettingsSection.entries.forEach { item ->
                val bringIntoView = remember { BringIntoViewRequester() }
                LaunchedEffect(selected) { if (item == selected) bringIntoView.bringIntoView() }
                FilterChip(
                    modifier = Modifier.bringIntoViewRequester(bringIntoView),
                    selected = item == selected,
                    onClick = { onSelect(item) },
                    leadingIcon = { FluxThemedIcon(sectionRole(item), sectionIcon(item), contentDescription = null, modifier = Modifier.size(18.dp)) },
                    label = { Text(item.title) },
                )
            }
        }

        if (scrollState.canScrollBackward) {
            IconButton(
                onClick = {
                    scope.launch {
                        scrollState.animateScrollTo((scrollState.value - stepPx).coerceAtLeast(0))
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 2.dp)
                    .size(30.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Есть разделы слева",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                )
            }
        }

        if (scrollState.canScrollForward) {
            IconButton(
                onClick = {
                    scope.launch {
                        scrollState.animateScrollTo((scrollState.value + stepPx).coerceAtMost(scrollState.maxValue))
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 2.dp)
                    .size(30.dp),
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Есть разделы справа",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.68f),
                )
            }
        }
    }
    HorizontalDivider()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibrarySettings(settings: ReaderSettings, viewModel: AppViewModel) {
    SectionTitle(Icons.Default.GridView, FluxIconRole.LIBRARY, "Библиотека")
    Text("Вид", style = MaterialTheme.typography.labelLarge)
    LibraryViewSelector(settings, viewModel)
    Text("Группировка", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            LibraryGroupMode.entries.forEach { mode ->
                FilterChip(
                    selected = settings.libraryGroupMode == mode,
                    onClick = { viewModel.updateSettings { it.copy(libraryGroupMode = mode) } },
                    label = { Text(when (mode) {
                        LibraryGroupMode.NONE -> "Нет"
                        LibraryGroupMode.FOLDER -> "Папки"
                        LibraryGroupMode.CUSTOM -> "Группы"
                        LibraryGroupMode.SERIES -> "Серии"
                        LibraryGroupMode.AUTHOR -> "Авторы"
                    }) },
                )
            }
    }
    if (settings.libraryGroupMode == LibraryGroupMode.FOLDER) {
        Text(
            "Папки повторяют структуру импортированного каталога.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    SettingsToggleRow("Продолжить", "", settings.libraryShowContinue) { enabled ->
        viewModel.updateSettings { it.copy(libraryShowContinue = enabled) }
    }
    SectionTitle(Icons.Default.Refresh, FluxIconRole.APPEARANCE, "Обложки")
    OutlinedButton(onClick = viewModel::refreshBookCovers, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Default.Refresh, contentDescription = null)
        Text("  Обновить")
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LibraryViewSelector(settings: ReaderSettings, viewModel: AppViewModel) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        LibraryViewMode.entries.forEach { mode ->
            FilterChip(
                selected = settings.libraryViewMode == mode,
                onClick = { viewModel.updateSettings { it.copy(libraryViewMode = mode) } },
                label = { Text(when (mode) {
                    LibraryViewMode.GRID -> "Сетка"
                    LibraryViewMode.COMPACT -> "Компактно"
                    LibraryViewMode.LIST -> "Список"
                }) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpeedSettings(settings: ReaderSettings, viewModel: AppViewModel) {
    SectionTitle(Icons.Default.Speed, "Скорочтение")
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        SpeedMode.entries.forEach { mode ->
            FilterChip(
                selected = settings.speedMode == mode,
                onClick = { viewModel.updateSettings { it.copy(speedMode = mode) } },
                leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null) },
                label = { Text(speedModeLabel(mode), maxLines = 1, softWrap = false) },
            )
        }
    }
    Text("Темп показа", style = MaterialTheme.typography.labelLarge)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        SpeedPacingMode.entries.forEach { mode ->
            FilterChip(
                selected = settings.speedPacingMode == mode,
                onClick = { viewModel.updateSettings { it.copy(speedPacingMode = mode) } },
                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                label = { Text(pacingLabel(mode), maxLines = 1, softWrap = false) },
            )
        }
    }
    SettingsValueSlider("Скорость", settings.speedWpm.toFloat(), 100f..3000f, { "${it.toInt()} WPM" }) { value ->
        viewModel.updateSettings { it.copy(speedWpm = value.toInt()) }
    }
    SettingsValueSlider("Размер текста", settings.speedFontSizeSp, 24f..104f, { "${it.toInt()} sp" }) { value ->
        viewModel.updateSettings { it.copy(speedFontSizeSp = value) }
    }
    SettingsValueSlider("Слов за фиксацию", settings.speedWordsPerFixation.toFloat(), 1f..7f, { it.toInt().toString() }) { value ->
        viewModel.updateSettings { it.copy(speedWordsPerFixation = value.toInt()) }
    }
    SettingsToggleRow(
        "Умная разбивка фраз",
        "",
        settings.speedSmartChunking,
    ) { enabled -> viewModel.updateSettings { it.copy(speedSmartChunking = enabled) } }
    SettingsToggleRow(
        "Микропаузы",
        "На пунктуации.",
        settings.speedMicroPausesEnabled,
    ) { enabled -> viewModel.updateSettings { it.copy(speedMicroPausesEnabled = enabled) } }
    SettingsToggleRow(
        "Пауза между абзацами",
        "",
        settings.speedParagraphPause,
    ) { enabled -> viewModel.updateSettings { it.copy(speedParagraphPause = enabled) } }
    SettingsToggleRow(
        "Плавный разгон",
        "",
        settings.speedGradualAcceleration,
    ) { enabled -> viewModel.updateSettings { it.copy(speedGradualAcceleration = enabled) } }
    SettingsToggleRow(
        "Автовозврат после паузы",
        "К началу предложения после долгой паузы.",
        settings.speedAutoRewindAfterPause,
    ) { enabled -> viewModel.updateSettings { it.copy(speedAutoRewindAfterPause = enabled) } }
    Text("Сложные слова", style = MaterialTheme.typography.titleMedium)
    SpeedComplexWordSettings(settings, viewModel::updateSettings)
    Text("Экспериментальные функции", style = MaterialTheme.typography.titleMedium)
    SpeedDialogueSettings(settings, viewModel::updateSettings)
}

@Composable
private fun OcrSettings(settings: ReaderSettings, viewModel: AppViewModel) {
    SectionTitle(Icons.Default.DocumentScanner, FluxIconRole.OCR, "PDF OCR")
    SettingsToggleRow(
        "OCR для сканированных страниц",
        "Запускается только если в PDF нет встроенного текста.",
        settings.ocrEnabled,
    ) { enabled -> viewModel.updateSettings { it.copy(ocrEnabled = enabled) } }
    Text("Языки распознавания", style = MaterialTheme.typography.labelLarge)
    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf("rus", "ukr", "eng").forEach { language ->
            FilterChip(
                selected = language in settings.ocrLanguages,
                onClick = {
                    viewModel.updateSettings { current ->
                        val next = current.ocrLanguages.toMutableSet().apply {
                            if (language in this) remove(language) else add(language)
                        }
                        current.copy(ocrLanguages = next)
                    }
                },
                label = { Text(language) },
            )
        }
    }
    val installedOcr = remember { viewModel.installedOcrLanguages() }
    Text(
        "Языковые модели: ${if (installedOcr.isEmpty()) "не установлены" else installedOcr.joinToString(" + ")}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text("Распознавание: ${viewModel.ocrBackendDescription()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    SettingsToggleRow("Распознавание латиницы", "Без языковых моделей.", settings.ocrUseMlKitFallback) { enabled ->
        viewModel.updateSettings { it.copy(ocrUseMlKitFallback = enabled) }
    }
    Text("Для русского и украинского нужны языковые модели.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun AboutSettings(viewModel: AppViewModel) {
    SectionTitle(Icons.Default.Info, FluxIconRole.ABOUT, "О программе")
    Text("Flux Reader", style = MaterialTheme.typography.headlineSmall)
    Text(
        "Версия ${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}",
        style = MaterialTheme.typography.labelLarge,
    )
    Text(
        "Автор: n.revis",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    HorizontalDivider()
    GitHubUpdateControls(viewModel)
    HorizontalDivider()
    Text(
        "Офлайн-читалка EPUB, FB2, TXT и PDF.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SectionTitle(icon: ImageVector, text: String) {
    SectionTitle(icon, FluxIconRole.APPEARANCE, text)
}

@Composable
private fun SectionTitle(icon: ImageVector, role: FluxIconRole, text: String) {
    if (LocalFluxThemeMode.current.isExperimentalChrome) {
        ExperimentalSectionHeader(text, role = role, fallback = icon)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            FluxThemedIcon(role, icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Text(text, style = MaterialTheme.typography.titleLarge)
        }
    }
}

private fun sectionRole(section: SettingsSection): FluxIconRole = when (section) {
    SettingsSection.APPEARANCE -> FluxIconRole.APPEARANCE
    SettingsSection.READING -> FluxIconRole.READING
    SettingsSection.LIBRARY -> FluxIconRole.LIBRARY
    SettingsSection.SPEED -> FluxIconRole.SPEED
    SettingsSection.OCR -> FluxIconRole.OCR
    SettingsSection.ABOUT -> FluxIconRole.ABOUT
}

private fun sectionIcon(section: SettingsSection): ImageVector = when (section) {
    SettingsSection.APPEARANCE -> Icons.Default.Palette
    SettingsSection.READING -> Icons.Default.AutoStories
    SettingsSection.LIBRARY -> Icons.Default.GridView
    SettingsSection.SPEED -> Icons.Default.Speed
    SettingsSection.OCR -> Icons.Default.DocumentScanner
    SettingsSection.ABOUT -> Icons.Default.Info
}

private fun speedModeLabel(mode: SpeedMode): String = when (mode) {
    SpeedMode.RSVP -> "RSVP"
    SpeedMode.PHRASE -> "Фраза"
    SpeedMode.FOCUS -> "Фокус"
}

private fun pacingLabel(mode: SpeedPacingMode): String = when (mode) {
    SpeedPacingMode.CONSTANT -> "Ровно"
    SpeedPacingMode.SMART -> "Умно"
    SpeedPacingMode.DEEP -> "Глубоко"
}
