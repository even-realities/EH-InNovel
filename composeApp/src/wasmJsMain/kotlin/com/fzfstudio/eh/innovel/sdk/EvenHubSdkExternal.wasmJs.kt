@file:OptIn(ExperimentalWasmJsInterop::class)
@file:JsModule("@evenrealities/even_hub_sdk")

package com.fzfstudio.eh.innovel.sdk

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.definedExternally

/**
 * @evenrealities/even_hub_sdk 的 Kotlin/WasmJs externals。
 *
 * 步骤1：本文件只放 `external` 声明，映射 npm 包 `@evenrealities/even_hub_sdk` 的导出。
 * 步骤2：Wasm 的 JS interop 类型更严格，参数/返回值使用 `JsAny?`。
 * 步骤3：业务调用统一走 `EvenHubBridge.wasmJs.kt` 的 `actual` 封装，避免到处处理 Promise。
 */
external class EvenAppBridge : JsAny {
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
    fun onLaunchSource(callback: (source: JsAny?) -> Unit): () -> Unit

    companion object {
        fun getInstance(): EvenAppBridge
    }
}

external fun waitForEvenAppBridge(): Promise<EvenAppBridge>
