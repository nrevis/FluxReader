package io.fluxreader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import io.fluxreader.app.data.SpeedSpeakerStyle
import io.fluxreader.app.data.SpeedSpeakerPosition
import io.fluxreader.app.data.SpeedSpeakerColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.fluxreader.app.data.ReaderSettings

/** Feature flag is independent of experimental themes: works in OLED, custom, and themed UIs. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SpeedDialogueSettings(
    settings: ReaderSettings,
    onUpdate: ((ReaderSettings) -> ReaderSettings) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsToggleRow(
            title = "Распознавание диалогов (эксперимент)",
            subtitle = "Выключено по умолчанию. Определяет прямую речь локально, без сети и ИИ.",
            checked = settings.speedDialogueEnabled,
        ) { value -> onUpdate { it.copy(speedDialogueEnabled = value) } }
        if (settings.speedDialogueEnabled) {
            SettingsToggleRow(
                title = "Имя говорящего",
                subtitle = "Показывает имя при явном указании в тексте. Неизвестный говорящий остаётся без подписи.",
                checked = settings.speedDialogueNames,
            ) { value -> onUpdate { it.copy(speedDialogueNames = value) } }
            if (settings.speedDialogueNames) {
                Text("Оформление имени", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(SpeedSpeakerStyle.TEXT to "Текст", SpeedSpeakerStyle.CHIP to "Плашка", SpeedSpeakerStyle.BUBBLE to "Бабл").forEach { (style, title) ->
                        FilterChip(selected = settings.speedSpeakerStyle == style, onClick = { onUpdate { it.copy(speedSpeakerStyle = style) } }, label = { Text(title) })
                    }
                }
                Box(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background, RoundedCornerShape(24.dp)).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (settings.speedSpeakerPosition == SpeedSpeakerPosition.ABOVE) {
                            SpeedSpeakerLabel("Анна Соколова", settings, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(settings.speedSpeakerGapDp.dp))
                        }
                        Text("Добрый вечер!", style = MaterialTheme.typography.titleLarge)
                        if (settings.speedSpeakerPosition == SpeedSpeakerPosition.BELOW) {
                            Spacer(Modifier.height(settings.speedSpeakerGapDp.dp))
                            SpeedSpeakerLabel("Анна Соколова", settings, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Text("Предпросмотр имени", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SettingsValueSlider("Размер имени", settings.speedSpeakerFontSizeSp, 12f..40f, { "${it.toInt()} sp" }) { value -> onUpdate { it.copy(speedSpeakerFontSizeSp = value) } }
                SettingsToggleRow("Жирный шрифт имени", "Выделяет имя говорящего.", settings.speedSpeakerBold) { value -> onUpdate { it.copy(speedSpeakerBold = value) } }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(SpeedSpeakerPosition.ABOVE to "Над репликой", SpeedSpeakerPosition.BELOW to "Под репликой").forEach { (position, title) ->
                        FilterChip(selected = settings.speedSpeakerPosition == position, onClick = { onUpdate { it.copy(speedSpeakerPosition = position) } }, label = { Text(title) })
                    }
                }
                Text("Цвет имени", style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(SpeedSpeakerColor.CHARACTER to "По персонажу", SpeedSpeakerColor.THEME to "Цвет темы", SpeedSpeakerColor.BLUE to "Синий", SpeedSpeakerColor.GREEN to "Зелёный", SpeedSpeakerColor.AMBER to "Янтарный", SpeedSpeakerColor.ROSE to "Розовый", SpeedSpeakerColor.VIOLET to "Фиолетовый").forEach { (color, title) ->
                        FilterChip(selected = settings.speedSpeakerColor == color, onClick = { onUpdate { it.copy(speedSpeakerColor = color) } }, label = { Text(title) })
                    }
                }
                if (settings.speedSpeakerStyle != SpeedSpeakerStyle.TEXT) {
                    SettingsValueSlider("Плотность фона", settings.speedSpeakerBackgroundAlpha, 0f..0.5f, { "${(it * 100).toInt()} %" }) { value -> onUpdate { it.copy(speedSpeakerBackgroundAlpha = value) } }
                }
                SettingsValueSlider("Отступ от реплики", settings.speedSpeakerGapDp, 12f..64f, { "${it.toInt()} dp" }) { value -> onUpdate { it.copy(speedSpeakerGapDp = value) } }
            }
            SettingsToggleRow(
                title = "Короткие реплики целиком",
                subtitle = "Только RSVP: реплики до 6 слов. Длинные остаются пословными.",
                checked = settings.speedDialogueShortReplies,
            ) { value -> onUpdate { it.copy(speedDialogueShortReplies = value) } }
            SettingsToggleRow(
                title = "Дополнительная пауза на новой реплике",
                subtitle = "Не заменяет обычные паузы на пунктуации.",
                checked = settings.speedDialoguePauseEnabled,
            ) { value -> onUpdate { it.copy(speedDialoguePauseEnabled = value) } }
            if (settings.speedDialoguePauseEnabled) {
                SettingsValueSlider("Пауза диалога", settings.speedDialoguePauseMs.toFloat(), 0f..500f,
                    { "${it.toInt()} мс" }) { value ->
                    onUpdate { it.copy(speedDialoguePauseMs = value.toInt()) }
                }
            }
            Text(
                "Тире, кавычки и реплики со словами автора внутри. " +
                    "Если говорящий не указан в тексте, приложение не пытается угадать его.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
