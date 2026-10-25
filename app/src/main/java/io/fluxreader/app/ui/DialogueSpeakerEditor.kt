package io.fluxreader.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
internal fun DialogueSpeakerEditor(
    currentName: String?,
    automaticName: String?,
    candidates: List<String>,
    onApply: (String?, Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName.orEmpty()) }
    val normalized = name.trim()
    val valid = normalized.length in 1..80 && normalized.none(Char::isISOControl)
    FluxModalBottomSheet(onDismissRequest = onDismiss) {
        ModalSheetHeader("Говорящий", onClose = onDismiss, subtitle = "Для этой реплики",
            modifier = Modifier.padding(horizontal = 20.dp))
        Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text("Имя") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOfNotNull(currentName, automaticName) + candidates).asSequence()
                    .filter { normalized == currentName || it.contains(normalized, ignoreCase = true) || it == currentName }
                    .distinct().take(24).forEach { candidate ->
                        FilterChip(selected = normalized == candidate, onClick = { name = candidate }, label = { Text(candidate) })
                    }
            }
            Button(onClick = { onApply(normalized, false) }, enabled = valid, modifier = Modifier.fillMaxWidth()) { Text("Сохранить") }
            TextButton(onClick = { onApply(null, false) }) { Text("Говорящий неизвестен") }
            TextButton(onClick = { onApply(null, true) }) {
                Text("Автоматически${automaticName?.let { ": $it" }.orEmpty()}", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
