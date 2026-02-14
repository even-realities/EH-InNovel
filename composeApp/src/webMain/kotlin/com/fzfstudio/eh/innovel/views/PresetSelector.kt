package com.fzfstudio.eh.innovel.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fzfstudio.eh.innovel.models.TimerPreset

@Composable
fun PresetSelector(
    selected: TimerPreset,
    enabled: Boolean,
    onSelect: (TimerPreset) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TimerPreset.values().forEach { preset ->
            if (preset == selected) {
                Button(
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    onClick = { onSelect(preset) },
                ) {
                    Text(preset.label)
                }
            } else {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    enabled = enabled,
                    onClick = { onSelect(preset) },
                ) {
                    Text(preset.label)
                }
            }
        }
    }
}
