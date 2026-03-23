@file:OptIn(ExperimentalWasmJsInterop::class)
@file:JsModule("@evenrealities/even_hub_sdk")
@file:JsNonModule

package com.fzfstudio.eh.innovel.sdk

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.definedExternally

/**
 * @evenrealities/even_hub_sdk 的 Kotlin/JS externals（只声明，不写实现逻辑）。
 *
 * 步骤1：保持本文件只有 `external` 声明（因为使用了 `@JsModule`）。
 * 步骤2：实际业务调用请使用 `EvenHubBridge.js.kt` 里的 `actual` 封装方法。
 * 步骤3：SDK 方法签名以 TypeScript SDK 为准（npm 包 `@evenrealities/even_hub_sdk`）。
 */
external class EvenAppBridge {
    val ready: Boolean
    // Generic bridge entry: method name + optional params.
    fun callEvenApp(method: String, params: JsAny? = definedExternally): Promise<JsAny?>
    // Convenience wrappers provided by the JS SDK.
    fun getUserInfo(): Promise<JsAny?>
    fun getDeviceInfo(): Promise<JsAny?>
    fun createStartUpPageContainer(container: JsAny?): Promise<JsAny?>
    fun rebuildPageContainer(container: JsAny?): Promise<JsAny?>
    fun updateImageRawData(data: JsAny?): Promise<JsAny?>
    fun textContainerUpgrade(container: JsAny?): Promise<JsAny?>
    fun audioControl(isOpen: Boolean): Promise<JsAny?>
    fun imuControl(isOpen: Boolean, reportFrq: Int = definedExternally): Promise<JsAny?>
    fun shutDownPageContainer(exitMode: Int = definedExternally): Promise<JsAny?>
    fun onDeviceStatusChanged(callback: (status: JsAny?) -> Unit): () -> Unit
    fun onEvenHubEvent(callback: (event: JsAny?) -> Unit): () -> Unit
    fun onLaunchSource(callback: (source: String) -> Unit): () -> Unit

    companion object {
        fun getInstance(): EvenAppBridge
    }
}

external fun waitForEvenAppBridge(): Promise<EvenAppBridge>
