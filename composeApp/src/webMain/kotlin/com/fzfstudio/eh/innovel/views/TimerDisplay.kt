package com.fzfstudio.eh.innovel.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fzfstudio.eh.innovel.models.TimerPreset
import com.fzfstudio.eh.innovel.models.TimerState

@Composable
fun TimerDisplay(
    remainingSeconds: Int,
    timerState: TimerState,
    selectedPreset: TimerPreset,
) {
    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val timeStr = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"

    val stateLabel = when (timerState) {
        TimerState.IDLE -> "Ready"
        TimerState.RUNNING -> "Focus"
        TimerState.PAUSED -> "Paused"
        TimerState.FINISHED -> "Done!"
    }

    val stateColor = when (timerState) {
        TimerState.IDLE -> MaterialTheme.colorScheme.onSurfaceVariant
        TimerState.RUNNING -> Color(0xFF34C759)
        TimerState.PAUSED -> Color(0xFFFF9500)
        TimerState.FINISHED -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = stateColor,
            )
            Text(
                text = timeStr,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = selectedPreset.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
