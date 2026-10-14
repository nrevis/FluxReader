package io.fluxreader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.fluxreader.app.data.PageTurnAnimation
import io.fluxreader.app.data.ReaderFontMode
import io.fluxreader.app.data.ReaderNavigationMode
import io.fluxreader.app.data.ReaderSettings
import io.fluxreader.app.data.ReaderTextAlignment
import io.fluxreader.app.data.ReaderThemeMode
import io.fluxreader.app.data.ThemeAppearance
import io.fluxreader.app.data.experimentalFrameTheme

@Composable
fun ReaderPreferencesControls(
    settings: ReaderSettings,
    onUpdate: ((ReaderSettings) -> ReaderSettings) -> Unit,
    includeTheme: Boolean = true,
    includeProfiles: Boolean = true,
    includeNavigation: Boolean = true,
    includeNavigationBasics: Boolean = true,
    includeNavigationAdvanced: Boolean = true,
    includeText: Boolean = true,
    includeBasicText: Boolean = true,
    includeAdvancedText: Boolean = true,
) {
    val focusManager = LocalFocusManager.current
    // Accessibility: Material onSurface can be near-black on the Wasteland CRT background.
    val settingsInk = if (LocalFluxThemeMode.current.isExperimentalChrome) LocalExperimentalColors.current.text else MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (includeProfiles) {
        PreferenceTitle(Icons.Default.AutoStories, "Профили")
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FilterChip(selected = false, onClick = {
                onUpdate { it.copy(fontSizeSp = 18f, lineHeightMultiplier = 1.32f, horizontalPaddingDp = 18f, topPaddingDp = 14f, bottomPaddingDp = 14f, paragraphSpacingDp = 6f, firstLineIndentDp = 0f, fontMode = ReaderFontMode.SANS, textAlignment = ReaderTextAlignment.LEFT, hyphenationEnabled = false) }
            }, leadingIcon = { Icon(Icons.Default.FormatSize, contentDescription = null, modifier = Modifier.size(18.dp)) }, label = { Text("Плотно") })
            FilterChip(selected = false, onClick = {
                onUpdate { it.copy(fontSizeSp = 20f, lineHeightMultiplier = 1.48f, horizontalPaddingDp = 24f, topPaddingDp = 20f, bottomPaddingDp = 20f, paragraphSpacingDp = 8f, firstLineIndentDp = 18f, fontMode = ReaderFontMode.SERIF, textAlignment = ReaderTextAlignment.LEFT, hyphenationEnabled = false) }
            }, label = { Text("Книга") })
            FilterChip(selected = false, onClick = {
                onUpdate { it.copy(fontSizeSp = 27f, lineHeightMultiplier = 1.55f, horizontalPaddingDp = 24f, topPaddingDp = 20f, bottomPaddingDp = 20f, paragraphSpacingDp = 14f, firstLineIndentDp = 0f, fontMode = ReaderFontMode.SANS, textAlignment = ReaderTextAlignment.LEFT, hyphenationEnabled = false) }
            }, label = { Text("Крупно") })
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(
                selected = false,
                onClick = {
                    onUpdate {
                        it.copy(
                            fontSizeSp = 18.5f,
                            lineHeightMultiplier = 1.40f,
                            horizontalPaddingDp = 22f,
                            topPaddingDp = 18f,
                            bottomPaddingDp = 18f,
                            paragraphSpacingDp = 8f,
                            firstLineIndentDp = 0f,
                            fontMode = ReaderFontMode.JETBRAINS_NERD,
                            textAlignment = ReaderTextAlignment.LEFT,
                            hyphenationEnabled = true,
                        )
                    }
                },
                label = { Text("JetBrains") },
            )
            Text(
                "Моноширинный профиль: равные поля, спокойное левое выравнивание и автопереносы слов.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        }

        if (includeTheme) {
            PreferenceTitle(Icons.Default.Palette, "Тема")
            Text("Режим", style = MaterialTheme.typography.labelLarge, color = settingsInk)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                ThemeAppearance.entries.forEach { appearance ->
                    FilterChip(
                        selected = settings.themeAppearance == appearance,
                        onClick = { onUpdate { it.copy(themeAppearance = appearance) } },
                        leadingIcon = {
                            Icon(
                                imageVector = when (appearance) {
                                    ThemeAppearance.SYSTEM -> Icons.Default.BrightnessAuto
                                    ThemeAppearance.LIGHT -> Icons.Default.LightMode
                                    ThemeAppearance.DARK -> Icons.Default.DarkMode
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = settingsInk,
                            )
                        },
                        label = { Text(appearanceLabel(appearance), maxLines = 1, softWrap = false, color = settingsInk) },
                    )
                }
            }
            Text(
                if (settings.themeAppearance == ThemeAppearance.SYSTEM) "Автоматически следует светлой/тёмной теме Android." else "Зафиксированный режим независимо от темы Android.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Checkbox(
                    checked = settings.experimentalDesignEnabled,
                    onCheckedChange = { checked ->
                        onUpdate { current -> current.copy(experimentalDesignEnabled = checked) }
                    },
                )
                Column(Modifier.weight(1f)) {
                    Text("Экспериментальный дизайн", style = MaterialTheme.typography.labelLarge, color = settingsInk)
                    Text(
                        "Включает выбранную оболочку. Стиль корпуса и палитра настраиваются независимо. Без галочки сохраняется обычный интерфейс.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (settings.experimentalDesignEnabled) {
                Text("Экспериментальная оболочка", style = MaterialTheme.typography.titleMedium, color = settingsInk)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(ReaderThemeMode.WASTELAND_TERMINAL, ReaderThemeMode.CYBERPUNK_NEON).forEach { frame ->
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = settings.experimentalFrameTheme == frame,
                            onClick = { onUpdate { it.copy(experimentalFrameStyle = frame) } },
                            label = { Text(if (frame == ReaderThemeMode.WASTELAND_TERMINAL) "Wasteland" else "Cyberpunk", maxLines = 1) },
                        )
                    }
                }
            }
            // One palette catalogue shared between the standard and experimental shells.
            // Selecting a swatch writes BOTH fields so switching shells never reveals an
            // unexpected stale palette. The optional original screen colors stay available.
            Text("Цветовая схема", style = MaterialTheme.typography.titleMedium, color = settingsInk)
            Text("Одна палитра для обычного и экспериментального оформления. Оболочка меняется отдельно.",
                style = MaterialTheme.typography.bodySmall, color = settingsInk.copy(alpha = 0.8f))
            if (settings.experimentalDesignEnabled) {
                FilterChip(
                    selected = settings.experimentalPalette == null,
                    onClick = { onUpdate { it.copy(experimentalPalette = null) } },
                    label = { Text("Оригинальные цвета корпуса") },
                )
            }
            val palettes = ReaderThemeMode.entries
            palettes.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    row.forEach { palette ->
                        val active = if (settings.experimentalDesignEnabled) {
                            settings.experimentalPalette == palette
                        } else settings.theme == palette
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = active,
                            onClick = { onUpdate { it.copy(theme = palette, experimentalPalette = palette) } },
                            leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null,
                                modifier = Modifier.size(16.dp), tint = settingsInk) },
                            label = { Text(if (palette == ReaderThemeMode.CUSTOM) settings.customThemeName else themeLabel(palette),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = settingsInk,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal) },
                        )
                    }
                    if (row.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                }
            }
            if (settings.experimentalDesignEnabled) {
                ExperimentalThemePreview(settings.experimentalFrameTheme)
                Text(
                    if (settings.experimentalFrameTheme == ReaderThemeMode.WASTELAND_TERMINAL)
                        "Полный Wasteland-скин: ржавый металл и потёртости, CRT/scanlines, янтарно-фосфорная палитра, отдельная индустриальная пиктограмма и физическая рамка терминала."
                    else
                        "Полный Cyberpunk-скин: HUD/microgrid-подложка, cyan/magenta/violet, отдельная неоновая пиктограмма, угловатые панели и статические glitch-акценты.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text("Экранные эффекты", style = MaterialTheme.typography.titleMedium, color = settingsInk)
                    SettingsToggleRow(
                        "CRT-эффект",
                        "Все дополнительные экранные эффекты можно выключить.",
                        settings.experimentalCrtEnabled,
                    ) { checked -> onUpdate { it.copy(experimentalCrtEnabled = checked) } }
                    if (settings.experimentalCrtEnabled) {
                        SettingsToggleRow(
                            "Линии развёртки",
                            "Горизонтальные полосы.",
                            settings.experimentalScanlinesEnabled,
                        ) { checked -> onUpdate { it.copy(experimentalScanlinesEnabled = checked) } }
                        SettingsToggleRow(
                            "Свечение",
                            "Слабое люминофорное свечение.",
                            settings.experimentalGlowEnabled,
                        ) { checked -> onUpdate { it.copy(experimentalGlowEnabled = checked) } }
                        SettingsToggleRow(
                            "Шум",
                            "Статичная зернистость.",
                            settings.experimentalNoiseEnabled,
                        ) { checked -> onUpdate { it.copy(experimentalNoiseEnabled = checked) } }
                        SettingsToggleRow(
                            "Виньетка",
                            "Затемнение краёв.",
                            settings.experimentalVignetteEnabled,
                        ) { checked -> onUpdate { it.copy(experimentalVignetteEnabled = checked) } }
                        SettingsValueSlider(
                            "Интенсивность эффектов",
                            settings.experimentalEffectIntensity,
                            0f..1f,
                            { "${(it * 100).toInt()}%" },
                        ) { value -> onUpdate { it.copy(experimentalEffectIntensity = value) } }
                    }
            }
            if (settings.theme == ReaderThemeMode.CUSTOM) {
                Text("Своя палитра", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = settings.customThemeName,
                    onValueChange = { name -> onUpdate { it.copy(customThemeName = name) } },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Название") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(force = true) }),
                )
                ThemeRgbEditor("Фон", settings.customBackgroundColorArgb) { color -> onUpdate { it.copy(customBackgroundColorArgb = color) } }
                ThemeRgbEditor("Поверхность", settings.customSurfaceColorArgb) { color -> onUpdate { it.copy(customSurfaceColorArgb = color) } }
                ThemeRgbEditor("Текст", settings.customTextColorArgb) { color -> onUpdate { it.copy(customTextColorArgb = color) } }
                ThemeRgbEditor("Акцент", settings.customAccentColorArgb) { color -> onUpdate { it.copy(customAccentColorArgb = color) } }
            }
        }

        if (includeNavigation) {
        PreferenceTitle(Icons.Default.Swipe, "Навигация")
        if (includeNavigationBasics) {
            SettingSwitch(
                title = "Автоматически скрывать панели",
                subtitle = "Выключено по умолчанию. Переключение полного экрана доступно касанием области чтения.",
                checked = settings.readerFullscreenEnabled,
                onCheckedChange = { enabled -> onUpdate { it.copy(readerFullscreenEnabled = enabled) } },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ReaderNavigationMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.navigationMode == mode,
                        onClick = { onUpdate { it.copy(navigationMode = mode) } },
                        label = { Text(if (mode == ReaderNavigationMode.SCROLL) "Прокрутка" else "Страницы") },
                    )
                }
            }
            SettingSwitch(
                title = "Свайп страниц",
                subtitle = "Предыдущая / следующая страница.",
                checked = settings.swipeNavigationEnabled,
                onCheckedChange = { enabled -> onUpdate { it.copy(swipeNavigationEnabled = enabled) } },
            )
            SettingSwitch(
                title = "Тап по краям",
                subtitle = "Левый край — назад, правый — вперёд; центр показывает панели.",
                checked = settings.edgeTapZonesEnabled,
                onCheckedChange = { enabled -> onUpdate { it.copy(edgeTapZonesEnabled = enabled) } },
            )
        }
        if (includeNavigationAdvanced) {
            if (settings.edgeTapZonesEnabled) {
                PreferenceSlider("Ширина зон", settings.edgeTapFraction, 0.12f..0.36f, { "${(it * 100).toInt()}%" }) { value -> onUpdate { it.copy(edgeTapFraction = value) } }
                TapZonePreview(settings.edgeTapFraction)
            }
            if (settings.readerFullscreenEnabled) {
                PreferenceSlider("Автоскрытие панелей", settings.chromeAutoHideSeconds.toFloat(), 0f..15f,
                    { if (it.toInt() == 0) "не скрывать" else "${it.toInt()} с" }) { value ->
                    onUpdate { it.copy(chromeAutoHideSeconds = value.toInt()) }
                }
            }

            Text("Анимация страниц", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(PageTurnAnimation.CURL, PageTurnAnimation.SLIDE, PageTurnAnimation.NONE).forEach { animation ->
                    FilterChip(
                        selected = settings.pageTurnAnimation == animation,
                        onClick = { onUpdate { it.copy(pageTurnAnimation = animation) } },
                        label = { Text(pageAnimationLabel(animation)) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(PageTurnAnimation.FADE, PageTurnAnimation.ZOOM).forEach { animation ->
                    FilterChip(
                        selected = settings.pageTurnAnimation == animation,
                        onClick = { onUpdate { it.copy(pageTurnAnimation = animation) } },
                        label = { Text(pageAnimationLabel(animation)) },
                    )
                }
            }
            if (settings.pageTurnAnimation != PageTurnAnimation.NONE) {
                PreferenceSlider("Скорость анимации", settings.pageTurnDurationMs.toFloat(), 80f..700f, { "${it.toInt()} мс" }) { value -> onUpdate { it.copy(pageTurnDurationMs = value.toInt()) } }
            }
            SettingSwitch(
                title = "Анимация полного экрана",
                subtitle = "Плавный вход в полноэкранное чтение и возврат обратно.",
                checked = settings.fullscreenTransitionEnabled,
                onCheckedChange = { enabled -> onUpdate { it.copy(fullscreenTransitionEnabled = enabled) } },
            )
            if (settings.fullscreenTransitionEnabled) {
                PreferenceSlider("Скорость полного экрана", settings.fullscreenTransitionDurationMs.toFloat(), 100f..600f, { "${it.toInt()} мс" }) { value ->
                    onUpdate { it.copy(fullscreenTransitionDurationMs = value.toInt()) }
                }
            }
            SettingSwitch(
                title = "Плавная прокрутка",
                subtitle = "Плавно двигает текст в режиме прокрутки.",
                checked = settings.smoothPageTurns,
                onCheckedChange = { enabled -> onUpdate { it.copy(smoothPageTurns = enabled) } },
            )
        }
        }

        if (includeText) {
        PreferenceTitle(Icons.Default.FormatSize, if (includeAdvancedText && !includeBasicText) "Точное форматирование" else "Текст")
        if (includeBasicText) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(ReaderFontMode.SYSTEM, ReaderFontMode.SERIF, ReaderFontMode.SANS).forEach { font ->
                    FilterChip(selected = settings.fontMode == font, onClick = { onUpdate { it.copy(fontMode = font) } }, label = { Text(fontLabel(font)) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                listOf(ReaderFontMode.MONO, ReaderFontMode.JETBRAINS_NERD).forEach { font ->
                    FilterChip(selected = settings.fontMode == font, onClick = { onUpdate { it.copy(fontMode = font) } }, label = { Text(fontLabel(font)) })
                }
            }
            if (settings.fontMode == ReaderFontMode.JETBRAINS_NERD) JetBrainsNerdFontImporter(settings = settings, onUpdate = onUpdate)
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ReaderTextAlignment.entries.forEach { alignment ->
                    FilterChip(selected = settings.textAlignment == alignment, onClick = { onUpdate { it.copy(textAlignment = alignment) } }, label = { Text(if (alignment == ReaderTextAlignment.LEFT) "По левому" else "По ширине") })
                }
            }
            PreferenceSlider("Размер текста", settings.fontSizeSp, 13f..38f, { "${it.toInt()} sp" }) { value -> onUpdate { it.copy(fontSizeSp = value) } }
            PreferenceSlider("Межстрочный интервал", settings.lineHeightMultiplier, 1.05f..2.2f, { String.format("%.2f", it) }) { value -> onUpdate { it.copy(lineHeightMultiplier = value) } }
            PreferenceSlider("Поля по бокам", settings.horizontalPaddingDp, 6f..64f, { "${it.toInt()} dp" }) { value -> onUpdate { it.copy(horizontalPaddingDp = value) } }
            PreferenceSlider("Поле сверху", settings.topPaddingDp, 0f..72f, { "${it.toInt()} dp" }) { value -> onUpdate { it.copy(topPaddingDp = value) } }
            PreferenceSlider("Поле снизу", settings.bottomPaddingDp, 0f..72f, { "${it.toInt()} dp" }) { value -> onUpdate { it.copy(bottomPaddingDp = value) } }
        }
        if (includeAdvancedText) {
            SettingSwitch(
                title = "Автоперенос слов",
                subtitle = "Типографские переносы. В профиле JetBrains включаются автоматически.",
                checked = settings.hyphenationEnabled,
                onCheckedChange = { enabled -> onUpdate { it.copy(hyphenationEnabled = enabled) } },
            )
            PreferenceSlider("Интервал между абзацами", settings.paragraphSpacingDp, 0f..32f, { "${it.toInt()} dp" }) { value -> onUpdate { it.copy(paragraphSpacingDp = value) } }
            PreferenceSlider("Красная строка", settings.firstLineIndentDp, 0f..40f, { "${it.toInt()} dp" }) { value -> onUpdate { it.copy(firstLineIndentDp = value) } }

            Text("Заголовки глав", style = MaterialTheme.typography.titleMedium)
            SettingSwitch(
                title = "По центру",
                subtitle = "Одинаково применяется в обычном и полноэкранном режимах.",
                checked = settings.centerChapterHeadings,
                onCheckedChange = { enabled -> onUpdate { it.copy(centerChapterHeadings = enabled) } },
            )
            PreferenceSlider("Название главы", settings.chapterTitleScale, 1.15f..2.40f, { "×${String.format("%.2f", it)}" }) { value -> onUpdate { it.copy(chapterTitleScale = value) } }
            PreferenceSlider("Номер / подзаголовок", settings.chapterHeadingScale, 1.05f..2.00f, { "×${String.format("%.2f", it)}" }) { value -> onUpdate { it.copy(chapterHeadingScale = value) } }
            Text(
                "Заголовок измеряется тем же движком, что и основной текст, поэтому пагинация учитывает его реальную высоту и не обрезает следующую строку.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        }
    }
}

@Composable
private fun TapZonePreview(edgeFraction: Float) {
    val edge = edgeFraction.coerceIn(0.12f, 0.36f)
    val center = (1f - edge * 2f).coerceAtLeast(0.2f)
    Row(Modifier.fillMaxWidth().height(42.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(modifier = Modifier.weight(edge), color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text("‹", textAlign = TextAlign.Center) }
        }
        Surface(modifier = Modifier.weight(center), color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text("панели", style = MaterialTheme.typography.labelSmall) }
        }
        Surface(modifier = Modifier.weight(edge), color = MaterialTheme.colorScheme.primaryContainer) {
            Box(Modifier.fillMaxWidth().padding(vertical = 10.dp), contentAlignment = Alignment.Center) { Text("›", textAlign = TextAlign.Center) }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val body: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = if (LocalFluxThemeMode.current == ReaderThemeMode.WASTELAND_TERMINAL) LocalExperimentalColors.current.text else MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
    if (LocalFluxThemeMode.current == ReaderThemeMode.WASTELAND_TERMINAL) {
        WastelandPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) { body() }
    } else {
        body()
    }
}

@Composable
private fun PreferenceSlider(title: String, value: Float, range: ClosedFloatingPointRange<Float>, format: (Float) -> String, onCommit: (Float) -> Unit) {
    var draft by remember(value) { mutableFloatStateOf(value) }
    val body: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(title, style = MaterialTheme.typography.labelLarge, color = if (LocalFluxThemeMode.current == ReaderThemeMode.WASTELAND_TERMINAL) LocalExperimentalColors.current.text else MaterialTheme.colorScheme.onSurface)
                Text(format(draft), color = if (LocalFluxThemeMode.current == ReaderThemeMode.WASTELAND_TERMINAL) LocalExperimentalColors.current.accent else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Slider(value = draft.coerceIn(range.start, range.endInclusive), onValueChange = { draft = it }, onValueChangeFinished = { onCommit(draft) }, valueRange = range)
        }
    }
    if (LocalFluxThemeMode.current == ReaderThemeMode.WASTELAND_TERMINAL) {
        WastelandPanel(modifier = Modifier.fillMaxWidth(), contentPadding = 8.dp) { body() }
    } else {
        body()
    }
}

@Composable
private fun ThemeRgbEditor(title: String, argb: Int, onCommit: (Int) -> Unit) {
    var draft by remember(argb) { mutableIntStateOf(argb) }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("#%08X".format(draft), style = MaterialTheme.typography.labelSmall)
                Surface(modifier = Modifier.size(28.dp), color = Color(draft), shape = MaterialTheme.shapes.small) {}
            }
        }
        listOf("R", "G", "B").forEachIndexed { index, label ->
            val shift = 16 - index * 8
            val value = (draft ushr shift) and 0xFF
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(label, modifier = Modifier.padding(end = 6.dp))
                Slider(
                    value = value.toFloat(),
                    onValueChange = { newValue ->
                        val mask = 0xFF shl shift
                        draft = (draft and mask.inv()) or ((newValue.toInt().coerceIn(0, 255)) shl shift)
                    },
                    onValueChangeFinished = { onCommit(draft) },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f),
                )
                Text(value.toString(), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

private fun themeLabel(theme: ReaderThemeMode): String = when (theme) {
    ReaderThemeMode.OLED -> "OLED"
    ReaderThemeMode.DARK -> "Flux"
    ReaderThemeMode.PAPER -> "Бумага"
    ReaderThemeMode.SEPIA -> "Сепия"
    ReaderThemeMode.NORD -> "Nord"
    ReaderThemeMode.DRACULA -> "Dracula"
    ReaderThemeMode.SOLARIZED_DARK -> "Solarized"
    ReaderThemeMode.CATPPUCCIN -> "Catppuccin"
    ReaderThemeMode.GRUVBOX -> "Gruvbox"
    ReaderThemeMode.ROSE_PINE_MOON -> "Rosé Pine Moon"
    ReaderThemeMode.CMX_PURPLE -> "CMX Purple"
    ReaderThemeMode.WASTELAND_TERMINAL -> "Wasteland CRT"
    ReaderThemeMode.CYBERPUNK_NEON -> "Cyberpunk"
    ReaderThemeMode.MONO -> "Ч/Б"
    ReaderThemeMode.MONO_SOFT -> "Ч/Б мягкая"
    ReaderThemeMode.CUSTOM -> "Моя"
}

private fun appearanceLabel(appearance: ThemeAppearance): String = when (appearance) {
    ThemeAppearance.SYSTEM -> "Системная"
    ThemeAppearance.LIGHT -> "Светлая"
    ThemeAppearance.DARK -> "Тёмная"
}

@Composable
private fun PreferenceTitle(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    if (LocalFluxThemeMode.current == ReaderThemeMode.WASTELAND_TERMINAL) {
        WastelandSectionHeader(text, fallback = icon)
    } else {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun pageAnimationLabel(animation: PageTurnAnimation): String = when (animation) {
    PageTurnAnimation.NONE -> "Нет"
    PageTurnAnimation.CURL -> "Лист"
    PageTurnAnimation.SLIDE -> "Сдвиг"
    PageTurnAnimation.FADE -> "Fade"
    PageTurnAnimation.ZOOM -> "Zoom"
}

private fun fontLabel(font: ReaderFontMode): String = when (font) {
    ReaderFontMode.SYSTEM -> "Система"
    ReaderFontMode.SERIF -> "Книжный"
    ReaderFontMode.SANS -> "Sans"
    ReaderFontMode.MONO -> "Mono"
    ReaderFontMode.JETBRAINS_NERD -> "JetBrains Nerd"
}
