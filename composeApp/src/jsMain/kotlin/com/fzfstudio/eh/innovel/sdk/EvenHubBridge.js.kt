@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.innovel.sdk

import kotlinx.coroutines.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.jsTypeOf

/**
 * even_hub_sdk 的 Kotlin/JS 实现（actual）。
 *
 * 步骤1：通过 `waitForEvenAppBridge().await()` 等待 JS 侧 bridge ready。
 * 步骤2：调用方法：`callEvenApp("getUserInfo")` 或 `callEvenApp("method", params)`。
 * 步骤3：带参调用：先把参数组织成 JS 对象（或使用 `callEvenAppJson` 传 JSON 字符串）。
 * 步骤4：监听设备状态变化：`observeDeviceStatus { ... }`。
 *
 * 注意：callEvenApp 数据结构中，params 会直接作为消息的 data 字段传递。
 * JS SDK 内部构建的消息结构：{ type: "call_even_app_method", method: method, data: params }
 */
actual suspend fun ensureEvenAppBridge() {
    waitForEvenAppBridge().await()
}

// Call with params packaged as a JSON string for shared code.
actual suspend fun callEvenApp(method: String, params: JsAny?): JsAny? =
    EvenAppBridge.getInstance().callEvenApp(method, params).await()

actual suspend fun callEvenAppJson(method: String, paramsJson: String): JsAny? =
    callEvenApp(method, jsParseJson(paramsJson))

actual suspend fun getUserInfo(): UserInfo? =
    userInfoFromJs(EvenAppBridge.getInstance().getUserInfo().await())

// Parse SDK returns into Kotlin models at the boundary.
actual suspend fun getDeviceInfo(): DeviceInfo? =
    deviceInfoFromJs(EvenAppBridge.getInstance().getDeviceInfo().await())

actual suspend fun createStartUpPageContainer(container: CreateStartUpPageContainer): StartUpPageCreateResult {
    val result = EvenAppBridge.getInstance()
        .createStartUpPageContainer(jsParseJson(container.toJsonString()))
        .await()
    return startUpPageCreateResultFromJs(result)
}

actual suspend fun rebuildPageContainer(container: RebuildPageContainer): Boolean {
    val result = EvenAppBridge.getInstance()
        .rebuildPageContainer(jsParseJson(container.toJsonString()))
        .await()
    return jsToBoolOrNull(result) ?: false
}

actual suspend fun updateImageRawData(data: ImageRawDataUpdate): ImageRawDataUpdateResult {
    val result = EvenAppBridge.getInstance()
        .updateImageRawData(jsParseJson(data.toJsonString()))
        .await()
    return imageRawDataUpdateResultFromJs(result)
}

actual suspend fun textContainerUpgrade(container: TextContainerUpgrade): Boolean {
    val result = EvenAppBridge.getInstance()
        .textContainerUpgrade(jsParseJson(container.toJsonString()))
        .await()
    return jsToBoolOrNull(result) ?: false
}

actual suspend fun shutDownPageContainer(container: ShutDownContainer): Boolean {
    val result = EvenAppBridge.getInstance()
        .shutDownPageContainer(container.exitMode)
        .await()
    return jsToBoolOrNull(result) ?: false
}

actual suspend fun audioControl(isOpen: Boolean): Boolean {
    val result = EvenAppBridge.getInstance().audioControl(isOpen).await()
    return jsToBoolOrNull(result) == true
}

actual suspend fun imuControl(isOpen: Boolean, reportFrq: ImuReportPace): Boolean {
    val result = EvenAppBridge.getInstance().imuControl(isOpen, reportFrq.value).await()
    return jsToBoolOrNull(result) == true
}

actual fun observeDeviceStatus(onChange: (DeviceStatus?) -> Unit): () -> Unit =
    EvenAppBridge.getInstance().onDeviceStatusChanged { status ->
        onChange(deviceStatusFromJs(status))
    }

// Bridge SDK events to Kotlin-friendly model.
actual fun observeEvenHubEvent(onChange: (EvenHubEvent?) -> Unit): () -> Unit =
    EvenAppBridge.getInstance().onEvenHubEvent { event ->
        onChange(evenHubEventFromJs(event))
    }

actual fun observeLaunchSource(onChange: (LaunchSource) -> Unit): () -> Unit =
    EvenAppBridge.getInstance().onLaunchSource { source ->
        onChange(LaunchSource.fromString(source))
    }

actual fun getEvenHubAppId(): String? {
    val value = js("typeof window !== 'undefined' ? window.__EVEN_HUB_APP_ID__ : undefined")
    return if (jsTypeOf(value) == "undefined" || value == null) null else value.toString()
}
