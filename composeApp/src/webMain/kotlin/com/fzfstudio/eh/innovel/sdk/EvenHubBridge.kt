@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.innovel.sdk

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny

/**
 * Web bridge API (implemented separately for Kotlin/JS and Kotlin/WasmJs).
 *
 * This is kept as expect/actual because Kotlin/Wasm JS interop has stricter type requirements than Kotlin/JS.
 *
 * 步骤1：在 `composeApp/build.gradle.kts` 的 `webMain` 中添加本地 npm 依赖 `even_hub_sdk`。
 * 步骤2：在业务使用前先调用 `ensureEvenAppBridge()`，等待 SDK 内部 bridge 就绪。
 * 步骤3：调用原生能力使用 `callEvenApp("method", params)`；需要参数时优先用 `callEvenAppJson("method", "{...}")`。
 * 步骤4：监听设备状态变化使用 `observeDeviceStatus { ... }`。
 *
 * 注意：callEvenApp 数据结构中，消息的 data 字段直接等于 params。
 * 消息结构：{ type: "call_even_app_method", method: method, data: params }
 */
expect suspend fun ensureEvenAppBridge()

/**
 * 调用 Even App 方法
 * 
 * 注意：params 会直接作为消息的 data 字段传递。
 * JS SDK 内部会构建消息结构：{ type: "call_even_app_method", method: method, data: params }
 * 
 * @param method 方法名称
 * @param params 方法参数（可选，JsAny 对象，会直接作为 data 传递。无参数时传 null 或不传）
 * @return 方法执行结果
 */
expect suspend fun callEvenApp(method: String, params: JsAny? = null): JsAny?

/**
 * Convenience overload for shared `webMain` code: pass params as a JSON string (object/array/literal).
 * 
 * 注意：paramsJson 会被解析为 JS 对象，然后直接作为消息的 data 字段传递。
 * 
 * Example: `callEvenAppJson("setLocalStorage", "{\"key\":\"k\",\"value\":\"v\"}")`
 * 这会构建消息：{ type: "call_even_app_method", method: "setLocalStorage", data: { key: "k", value: "v" } }
 */
expect suspend fun callEvenAppJson(method: String, paramsJson: String): JsAny?

/**
 * 获取用户信息
 */
expect suspend fun getUserInfo(): UserInfo?

/**
 * 获取设备信息（眼镜/戒指信息）
 */
expect suspend fun getDeviceInfo(): DeviceInfo?


/**
 * EvenHub - PB 接口（对齐宿主 BleG2CmdProtoEvenHubExt）
 *
 * 说明：统一以 JSON 字符串传参，避免在 shared 层直接构建 JsAny。
 */
expect suspend fun createStartUpPageContainer(container: CreateStartUpPageContainer): Int?

expect suspend fun rebuildPageContainer(container: RebuildPageContainer): Boolean

expect suspend fun updateImageRawData(data: ImageRawDataUpdate): Boolean

expect suspend fun textContainerUpgrade(container: TextContainerUpgrade): Boolean

expect suspend fun shutDownPageContainer(container: ShutDownContainer): Boolean

/**
 * 音频控制（MIC 控制）
 * @param isOpen true=打开麦克风，false=关闭麦克风
 * @return 是否成功
 */
expect suspend fun audioControl(isOpen: Boolean): Boolean

/**
 * 监听设备状态变化
 * @param onChange 状态变化时的回调函数，参数为完整的设备状态对象
 * @return 取消监听的函数
 */
expect fun observeDeviceStatus(onChange: (DeviceStatus?) -> Unit): () -> Unit

/**
 * 监听 EvenHub 事件
 * @param onChange 事件变化时的回调函数，参数为 EvenHubEvent 对象
 * @return 取消监听的函数
 */
expect fun observeEvenHubEvent(onChange: (EvenHubEvent?) -> Unit): () -> Unit
