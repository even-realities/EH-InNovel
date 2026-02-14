@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.innovel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fzfstudio.eh.innovel.models.AppState
import com.fzfstudio.eh.innovel.theme.InNovelTheme
import com.fzfstudio.eh.innovel.views.AppScreen
import kotlin.js.ExperimentalWasmJsInterop

@Composable
fun App() {
    InNovelTheme {
        val appState = remember { AppState() }
        val uiState = appState.uiState
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            appState.initialize()
        }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!uiState.isBridgeReady) {
                Text(
                    text = "Initializing bridge...",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                AppScreen(
                    uiState = uiState,
                    onSelectPreset = { appState.selectPreset(it) },
                    onStart = { appState.startTimer() },
                    onPause = { appState.pauseTimer() },
                    onReset = { appState.resetTimer() },
                    onExit = {
                        coroutineScope.launch { appState.exitTimer() }
                    },
                )
            }
        }
    }
}

expect fun formatJsObject(obj: Any?): String
