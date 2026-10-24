package io.fluxreader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.fluxreader.app.data.ReaderSettings

/** Feature flag is independent of experimental themes: works in OLED, custom, and themed UIs. */
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
                title = "Персонажи над текстом",
                subtitle = "Показывает имя только при явной авторской атрибуции; иначе «Прямая речь».",
                checked = settings.speedDialogueNames,
            ) { value -> onUpdate { it.copy(speedDialogueNames = value) } }
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
                "Первый этап: тире в начале реплики и кавычки с речевыми ремарками. " +
                    "Если говорящий не указан в тексте, приложение не пытается угадать его.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
