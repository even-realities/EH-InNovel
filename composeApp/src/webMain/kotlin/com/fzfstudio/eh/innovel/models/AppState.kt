@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.innovel.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fzfstudio.eh.innovel.sdk.*
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Timer running state.
 */
enum class TimerState { IDLE, RUNNING, PAUSED, FINISHED }

/**
 * Aggregated UI state for the Pomodoro timer.
 */
data class AppUiState(
    val isBridgeReady: Boolean = false,
    val userInfo: UserInfo? = null,
    val deviceInfo: DeviceInfo? = null,
    val deviceStatus: DeviceStatus? = null,
    val errorMessage: String? = null,
    /** Currently selected preset */
    val selectedPreset: TimerPreset = TimerPreset.FIVE_MIN,
    /** Seconds remaining on the timer */
    val remainingSeconds: Int = TimerPreset.FIVE_MIN.seconds,
    /** Current timer state */
    val timerState: TimerState = TimerState.IDLE,
)

/**
 * Pomodoro timer business logic and SDK integration.
 */
class AppState {
    var uiState by mutableStateOf(AppUiState())
        private set

    private var unsubscribeDeviceStatus: (() -> Unit)? = null
    private var unsubscribeEvenHubEvent: (() -> Unit)? = null

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tickJob: Job? = null

    // ---- Initialization ----

    suspend fun initialize() {
        if (uiState.isBridgeReady) return
        try {
            ensureEvenAppBridge()
            val userInfo = runCatching { getUserInfo() }.getOrNull()
            val deviceInfo = runCatching { getDeviceInfo() }.getOrNull()
            uiState = uiState.copy(
                isBridgeReady = true,
                userInfo = userInfo,
                deviceInfo = deviceInfo,
                deviceStatus = deviceInfo?.status,
            )
            setupDeviceStatusObserver()
            setupEvenHubEventObserver()
            pushTimerToGlasses()
        } catch (e: Exception) {
            uiState = uiState.copy(errorMessage = "Failed to initialize bridge: ${e.message}")
        }
    }

    // ---- Timer controls ----

    fun selectPreset(preset: TimerPreset) {
        if (uiState.timerState == TimerState.RUNNING) return
        uiState = uiState.copy(
            selectedPreset = preset,
            remainingSeconds = preset.seconds,
            timerState = TimerState.IDLE,
        )
        coroutineScope.launch { pushTimerToGlasses() }
    }

    fun startTimer() {
        if (uiState.timerState == TimerState.RUNNING) return
        if (uiState.remainingSeconds <= 0) {
            uiState = uiState.copy(remainingSeconds = uiState.selectedPreset.seconds)
        }
        uiState = uiState.copy(timerState = TimerState.RUNNING)
        tickJob?.cancel()
        tickJob = coroutineScope.launch {
            while (uiState.remainingSeconds > 0 && uiState.timerState == TimerState.RUNNING) {
                delay(1000)
                if (uiState.timerState != TimerState.RUNNING) break
                uiState = uiState.copy(remainingSeconds = uiState.remainingSeconds - 1)
                updateTimerDisplay()
            }
            if (uiState.remainingSeconds <= 0) {
                uiState = uiState.copy(timerState = TimerState.FINISHED)
                updateTimerDisplay()
            }
        }
    }

    fun pauseTimer() {
        if (uiState.timerState != TimerState.RUNNING) return
        uiState = uiState.copy(timerState = TimerState.PAUSED)
        tickJob?.cancel()
        coroutineScope.launch { updateTimerDisplay() }
    }

    fun resetTimer() {
        tickJob?.cancel()
        uiState = uiState.copy(
            remainingSeconds = uiState.selectedPreset.seconds,
            timerState = TimerState.IDLE,
        )
        coroutineScope.launch { pushTimerToGlasses() }
    }

    // ---- Glasses display ----

    private suspend fun pushTimerToGlasses() {
        val presets = TimerPreset.values().toList()

        val presetList = ListContainerProperty(
            containerID = 1,
            containerName = "presets",
            xPosition = 0,
            yPosition = 0,
            width = 130,
            height = 235,
            borderWidth = 1,
            borderColor = 13,
            borderRdaius = 6,
            paddingLength = 5,
            isEventCapture = 1,
            itemContainer = ListItemContainerProperty(
                itemCount = presets.size,
                itemWidth = 120,
                isItemSelectBorderEn = 1,
                itemName = presets.map { it.label },
            ),
        )

        val timerText = TextContainerProperty(
            containerID = 2,
            containerName = "timer",
            xPosition = 140,
            yPosition = 0,
            width = 390,
            height = 235,
            borderWidth = 1,
            borderColor = 13,
            borderRdaius = 6,
            paddingLength = 12,
            isEventCapture = 1,
            content = buildTimerContent(),
        )

        runCatching {
            createStartUpPageContainer(
                CreateStartUpPageContainer(
                    containerTotalNum = 2,
                    listObject = listOf(presetList),
                    textObject = listOf(timerText),
                )
            )
        }.onFailure { error ->
            uiState = uiState.copy(errorMessage = "Failed to create timer view: ${error.message}")
        }
    }

    private suspend fun updateTimerDisplay() {
        runCatching {
            textContainerUpgrade(
                TextContainerUpgrade(
                    containerID = 2,
                    containerName = "timer",
                    content = buildTimerContent(),
                )
            )
        }.onFailure { error ->
            uiState = uiState.copy(errorMessage = "Failed to update timer: ${error.message}")
        }
    }

    private fun buildTimerContent(): String {
        val minutes = uiState.remainingSeconds / 60
        val seconds = uiState.remainingSeconds % 60
        val timeStr = "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        val stateLabel = when (uiState.timerState) {
            TimerState.IDLE -> "Ready"
            TimerState.RUNNING -> "Focus"
            TimerState.PAUSED -> "Paused"
            TimerState.FINISHED -> "Done!"
        }
        val hint = when (uiState.timerState) {
            TimerState.IDLE -> "Double-click to start"
            TimerState.RUNNING -> "Double-click to pause"
            TimerState.PAUSED -> "Double-click to resume"
            TimerState.FINISHED -> "Double-click to reset"
        }
        return "$stateLabel\n\n$timeStr\n\n$hint"
    }

    // ---- Event handling ----

    private fun setupDeviceStatusObserver() {
        unsubscribeDeviceStatus?.invoke()
        unsubscribeDeviceStatus = observeDeviceStatus { status ->
            if (status != null) updateDeviceStatus(status)
        }
    }

    private fun setupEvenHubEventObserver() {
        unsubscribeEvenHubEvent?.invoke()
        unsubscribeEvenHubEvent = observeEvenHubEvent { event ->
            if (event != null) {
                when {
                    event.listEvent != null -> handleListItemEvent(event.listEvent)
                    event.sysEvent != null -> handleSysItemEvent(event.sysEvent)
                    else -> {}
                }
            }
        }
    }

    private fun handleListItemEvent(event: ListItemEvent) {
        val index = event.currentSelectItemIndex ?: return
        val presets = TimerPreset.values()
        if (index in presets.indices) {
            selectPreset(presets[index])
        }
    }

    private fun handleSysItemEvent(event: SysItemEvent) {
        when (event.eventType) {
            OsEventTypeList.DOUBLE_CLICK_EVENT -> {
                when (uiState.timerState) {
                    TimerState.IDLE -> startTimer()
                    TimerState.RUNNING -> pauseTimer()
                    TimerState.PAUSED -> startTimer()
                    TimerState.FINISHED -> resetTimer()
                }
            }
            OsEventTypeList.FOREGROUND_ENTER_EVENT -> {
                println("[Pomodoro] App entered foreground")
            }
            OsEventTypeList.FOREGROUND_EXIT_EVENT -> {
                println("[Pomodoro] App exited foreground")
            }
            else -> {}
        }
    }

    fun updateDeviceStatus(status: DeviceStatus?) {
        if (status == null) return
        val deviceSn = uiState.deviceInfo?.sn ?: return
        if (status.sn != deviceSn) return
        uiState = uiState.copy(deviceStatus = status)
    }

    suspend fun exitTimer() {
        tickJob?.cancel()
        runCatching {
            shutDownPageContainer(ShutDownContainer(exitMode = 0))
        }.onFailure { error ->
            uiState = uiState.copy(errorMessage = "Failed to exit: ${error.message}")
        }
    }

    fun dispose() {
        tickJob?.cancel()
        unsubscribeDeviceStatus?.invoke()
        unsubscribeDeviceStatus = null
        unsubscribeEvenHubEvent?.invoke()
        unsubscribeEvenHubEvent = null
    }
}
