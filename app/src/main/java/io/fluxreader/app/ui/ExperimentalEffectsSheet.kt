package io.fluxreader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
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
    FluxModalBottomSheet(
        onDismissRequest = onDismiss,
        onBack = onBack,
    ) {
        ModalSheetHeader(title = "Экранные эффекты", onBack = onBack, onClose = onDismiss, modifier = Modifier.padding(horizontal = 20.dp))

        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp).padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SettingsToggleRow(
                title = "CRT-эффект",
                subtitle = "",
                checked = settings.experimentalCrtEnabled,
            ) { enabled -> onUpdate { it.copy(experimentalCrtEnabled = enabled) } }
            if (settings.experimentalCrtEnabled) {
                SettingsToggleRow(
                    title = "Линии развёртки",
                    subtitle = "",
                    checked = settings.experimentalScanlinesEnabled,
                ) { enabled -> onUpdate { it.copy(experimentalScanlinesEnabled = enabled) } }
                SettingsToggleRow(
                    title = "Свечение люминофора",
                    subtitle = "",
                    checked = settings.experimentalGlowEnabled,
                ) { enabled -> onUpdate { it.copy(experimentalGlowEnabled = enabled) } }
                SettingsToggleRow(
                    title = "Шум",
                    subtitle = "",
                    checked = settings.experimentalNoiseEnabled,
                ) { enabled -> onUpdate { it.copy(experimentalNoiseEnabled = enabled) } }
                SettingsToggleRow(
                    title = "Затемнение краёв",
                    subtitle = "",
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
