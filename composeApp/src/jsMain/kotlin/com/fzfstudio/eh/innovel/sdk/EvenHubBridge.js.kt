@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.innovel.sdk

import kotlinx.coroutines.await
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

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

actual suspend fun createStartUpPageContainer(container: CreateStartUpPageContainer): Int? {
    val result = callEvenAppJson("createStartUpPageContainer", container.toJsonString())
    return jsToDoubleOrNull(result)?.toInt()
}

actual suspend fun rebuildPageContainer(container: RebuildPageContainer): Boolean {
    val result = callEvenAppJson("rebuildPageContainer", container.toJsonString())
    return jsToBoolOrNull(result) ?: false
}

actual suspend fun updateImageRawData(data: ImageRawDataUpdate): Boolean {
    val result = callEvenAppJson("updateImageRawData", data.toJsonString())
    return jsToBoolOrNull(result) ?: false
}

actual suspend fun textContainerUpgrade(container: TextContainerUpgrade): Boolean {
    val result = callEvenAppJson("textContainerUpgrade", container.toJsonString())
    return jsToBoolOrNull(result) ?: false
}

actual suspend fun shutDownPageContainer(container: ShutDownContainer): Boolean {
    val result = callEvenAppJson("shutDownPageContainer", container.toJsonString())
    return jsToBoolOrNull(result) ?: false
}

actual suspend fun audioControl(isOpen: Boolean): Boolean {
    val result = callEvenAppJson("audioControl", "{\"isOpen\":$isOpen}")
    return jsToBoolOrNull(result) ?: false
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