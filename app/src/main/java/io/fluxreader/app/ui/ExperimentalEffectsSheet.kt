package io.fluxreader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.fluxreader.app.data.ReaderSettings

/** Effects are opt-in visual cosmetics and never affect text/layout/tap regions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExperimentalEffectsSheet(
    settings: ReaderSettings,
    onUpdate: ((ReaderSettings) -> ReaderSettings) -> Unit,
    onDismiss: () -> Unit,
    onBack: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = onBack == null),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ModalSheetHeader(title = "Экранные эффекты", onBack = onBack, onClose = onDismiss)
            Text(
                "Применяется к Wasteland и Cyberpunk только при включённом экспериментальном дизайне.",
                style = MaterialTheme.typography.bodySmall,
            )
            SettingsToggleRow(
                title = "CRT-эффект",
                subtitle = "Главный переключатель всех экранных эффектов.",
                checked = settings.experimentalCrtEnabled,
            ) { enabled -> onUpdate { it.copy(experimentalCrtEnabled = enabled) } }
            if (settings.experimentalCrtEnabled) {
                SettingsToggleRow(
                    title = "Линии развёртки",
                    subtitle = "Тонкие горизонтальные линии на экране.",
                    checked = settings.experimentalScanlinesEnabled,
                ) { enabled -> onUpdate { it.copy(experimentalScanlinesEnabled = enabled) } }
                SettingsToggleRow(
                    title = "Свечение люминофора",
                    subtitle = "Небольшая подсветка поверхности экрана.",
                    checked = settings.experimentalGlowEnabled,
                ) { enabled -> onUpdate { it.copy(experimentalGlowEnabled = enabled) } }
                SettingsToggleRow(
                    title = "Шум",
                    subtitle = "Статичная лёгкая зернистость.",
                    checked = settings.experimentalNoiseEnabled,
                ) { enabled -> onUpdate { it.copy(experimentalNoiseEnabled = enabled) } }
                SettingsToggleRow(
                    title = "Затемнение краёв",
                    subtitle = "Слабая виньетка по бокам.",
                    checked = settings.experimentalVignetteEnabled,
                ) { enabled -> onUpdate { it.copy(experimentalVignetteEnabled = enabled) } }
                SettingsValueSlider(
                    title = "Интенсивность",
                    value = settings.experimentalEffectIntensity,
                    range = 0f..1f,
                    format = { "${(it * 100).toInt()}%" },
                ) { level -> onUpdate { it.copy(experimentalEffectIntensity = level) } }
            }
        }
    }
}
