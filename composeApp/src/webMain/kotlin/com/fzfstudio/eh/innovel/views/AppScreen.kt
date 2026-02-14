package com.fzfstudio.eh.innovel.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fzfstudio.eh.innovel.models.AppUiState
import com.fzfstudio.eh.innovel.models.TimerPreset
import com.fzfstudio.eh.innovel.models.TimerState

@Composable
fun AppScreen(
    uiState: AppUiState,
    onSelectPreset: (TimerPreset) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onReset: () -> Unit,
    onExit: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.errorMessage != null) {
            ErrorBanner(uiState.errorMessage)
        }

        // Header: user + device status
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TimerHeaderCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                userInfo = uiState.userInfo,
            )
            Spacer(modifier = Modifier.width(12.dp))
            DeviceInfoCard(
                modifier = Modifier.width(60.dp).height(30.dp),
                deviceInfo = uiState.deviceInfo,
                deviceStatus = uiState.deviceStatus,
            )
        }

        // Timer display
        TimerDisplay(
            remainingSeconds = uiState.remainingSeconds,
            timerState = uiState.timerState,
            selectedPreset = uiState.selectedPreset,
        )

        // Preset selector
        PresetSelector(
            selected = uiState.selectedPreset,
            enabled = uiState.timerState != TimerState.RUNNING,
            onSelect = onSelectPreset,
        )

        // Control buttons
        TimerControls(
            timerState = uiState.timerState,
            onStart = onStart,
            onPause = onPause,
            onReset = onReset,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Exit button
        Button(
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
            onClick = onExit,
        ) {
            Text("Exit Plugin")
        }
    }
}
