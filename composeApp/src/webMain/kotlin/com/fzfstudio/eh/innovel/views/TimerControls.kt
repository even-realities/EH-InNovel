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
import com.fzfstudio.eh.innovel.models.TimerState

@Composable
fun TimerControls(
    timerState: TimerState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when (timerState) {
            TimerState.IDLE -> {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onStart,
                ) {
                    Text("Start")
                }
            }
            TimerState.RUNNING -> {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onPause,
                ) {
                    Text("Pause")
                }
            }
            TimerState.PAUSED -> {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onStart,
                ) {
                    Text("Resume")
                }
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = onReset,
                ) {
                    Text("Reset")
                }
            }
            TimerState.FINISHED -> {
                Button(
                    modifier = Modifier.weight(1f),
                    onClick = onReset,
                ) {
                    Text("Reset")
                }
            }
        }
    }
}
