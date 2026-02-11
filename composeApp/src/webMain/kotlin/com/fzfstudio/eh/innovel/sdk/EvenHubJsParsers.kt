@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")

package com.fzfstudio.eh.innovel.sdk

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.js

/**
 * Parse JS SDK returns into Kotlin models.
 *
 * Uses JsInterop to avoid duplicate JS/Wasm implementations.
 */
internal fun userInfoFromJs(raw: JsAny?): UserInfo? {
    if (raw == null) return null
    return UserInfo(
        uid = JsInteropUtils.getIntProperty(raw, "uid"),
        name = JsInteropUtils.getStringProperty(raw, "name") ?: "",
        avatar = JsInteropUtils.getStringProperty(raw, "avatar") ?: "",
        country = JsInteropUtils.getStringProperty(raw, "country") ?: "",
    )
}

internal fun deviceInfoFromJs(raw: JsAny?): DeviceInfo? {
    if (raw == null) return null
    val sn = JsInteropUtils.getStringProperty(raw, "sn") ?: ""
    val status = deviceStatusFromJs(JsInteropUtils.getProperty(raw, "status"))
    return DeviceInfo(
        model = DeviceModel.fromString(JsInteropUtils.getStringProperty(raw, "model")),
        sn = sn,
        status = status,
    )
}

internal fun deviceStatusFromJs(raw: JsAny?): DeviceStatus? {
    if (raw == null) return null
    val sn = JsInteropUtils.getStringProperty(raw, "sn") ?: ""
    return DeviceStatus(
        sn = sn,
        connectType = DeviceConnectType.fromString(JsInteropUtils.getStringProperty(raw, "connectType")),
        isWearing = JsInteropUtils.getBooleanProperty(raw, "isWearing"),
        batteryLevel = JsInteropUtils.getIntProperty(raw, "batteryLevel"),
        isCharging = JsInteropUtils.getBooleanProperty(raw, "isCharging"),
        isInCase = JsInteropUtils.getBooleanProperty(raw, "isInCase"),
    )
}

internal fun evenHubEventFromJs(raw: JsAny?): EvenHubEvent? {
    if (raw == null) return null
    
    // 新的结构：{ listEvent?, textEvent?, sysEvent?, audioEvent?, jsonData? }
    val listEventRaw = JsInteropUtils.getProperty(raw, "listEvent")
    val textEventRaw = JsInteropUtils.getProperty(raw, "textEvent")
    val sysEventRaw = JsInteropUtils.getProperty(raw, "sysEvent")
    val audioEventRaw = JsInteropUtils.getProperty(raw, "audioEvent")
    val jsonDataRaw = JsInteropUtils.getProperty(raw, "jsonData")
    
    val listEvent = if (listEventRaw != null) listItemEventFromJs(listEventRaw) else null
    val textEvent = if (textEventRaw != null) textItemEventFromJs(textEventRaw) else null
    val sysEvent = if (sysEventRaw != null) sysItemEventFromJs(sysEventRaw) else null
    val audioEvent = if (audioEventRaw != null) audioEventFromJs(audioEventRaw) else null
    
    val jsonData = if (jsonDataRaw != null) {
        JsInteropUtils.stringify(jsonDataRaw)
    } else {
        if (listEvent != null || textEvent != null || sysEvent != null || audioEvent != null) {
            JsInteropUtils.stringify(raw)
        } else {
            null
        }
    }
    
    return EvenHubEvent(
        listEvent = listEvent,
        textEvent = textEvent,
        sysEvent = sysEvent,
        audioEvent = audioEvent,
        jsonData = jsonData,
    )
}

/**
 * 从 JS 解析音频事件 payload。
 * 兼容：audioPcm 为 number[]、Uint8Array 或 base64 字符串（与 SDK AudioEventPayload 一致）。
 */
internal fun audioEventFromJs(raw: JsAny?): AudioEventPayload? {
    if (raw == null) return null
    val pcmRaw = JsInteropUtils.getProperty(raw, "audioPcm")
        ?: JsInteropUtils.getProperty(raw, "audio_pcm")
        ?: JsInteropUtils.getProperty(raw, "pcm")
    val bytes = parseAudioPcmToIntList(pcmRaw) ?: return null
    return AudioEventPayload(audioPcm = bytes)
}

/**
 * 将宿主下发的 audioPcm 转为 List<Int>。
 * 兼容：number[]、Uint8Array（length + [i]）、base64 字符串。
 */
private fun parseAudioPcmToIntList(raw: JsAny?): List<Int>? {
    if (raw == null) return null
    // Uint8Array / number[]：有 length 且可下标访问（含 TypedArray，isArray 可能为 false）
    var length = JsInteropUtils.getArrayLength(raw)
    if (length <= 0) {
        val lengthProp = JsInteropUtils.toIntOrNull(JsInteropUtils.getProperty(raw, "length"))
        if (lengthProp != null && lengthProp > 0) length = lengthProp
    }
    if (length > 0) {
        val list = mutableListOf<Int>()
        for (i in 0 until length) {
            val el = JsInteropUtils.getArrayElement(raw, i) ?: JsInteropUtils.getProperty(raw, i.toString())
            list.add(JsInteropUtils.toIntOrNull(el)?.and(0xff) ?: 0)
        }
        return list
    }
    // base64 字符串
    val str = JsInteropUtils.toStringOrNull(raw)
    if (!str.isNullOrEmpty()) {
        return try {
            @Suppress("UNCHECKED_CAST")
            val atob = js("(function(s){ return atob(s); })") as (String) -> String
            val binary = atob(str)
            binary.map { it.code and 0xff }
        } catch (_: Throwable) {
            null
        }
    }
    return null
}

/**
 * 从 JS 解析 OsEventTypeList 枚举值
 */
internal fun osEventTypeListFromJs(raw: JsAny?): OsEventTypeList? {
    if (raw == null) return null
    
    // 如果 raw 本身是数字（OsEventTypeList 枚举值就是数字）
    val directInt = JsInteropUtils.toIntOrNull(raw)
    if (directInt != null && directInt >= 0 && directInt <= 6) {
        return OsEventTypeList.fromInt(directInt)
    }
    
    // 尝试从对象的 value 属性获取
    val intValue = JsInteropUtils.getIntProperty(raw, "value")
        ?: JsInteropUtils.toIntOrNull(raw)
    if (intValue != null && intValue >= 0 && intValue <= 6) {
        return OsEventTypeList.fromInt(intValue)
    }
    
    // 再尝试作为字符串解析
    val stringValue = JsInteropUtils.toStringOrNull(raw)
    if (stringValue != null) {
        return OsEventTypeList.fromString(stringValue)
    }
    
    return null
}

/**
 * 从 JS 解析 ListItemEvent
 * 兼容多种字段名格式：camelCase 和 protoName (如 Container_ID)
 */
internal fun listItemEventFromJs(raw: JsAny?): ListItemEvent? {
    if (raw == null) return null
    
    // 尝试多种字段名格式
    val containerID = JsInteropUtils.getIntProperty(raw, "containerID")
        ?: JsInteropUtils.getIntProperty(raw, "ContainerID")
        ?: JsInteropUtils.getIntProperty(raw, "Container_ID")
    
    val containerName = JsInteropUtils.getStringProperty(raw, "containerName")
        ?: JsInteropUtils.getStringProperty(raw, "ContainerName")
        ?: JsInteropUtils.getStringProperty(raw, "Container_Name")
    
    val currentSelectItemName = JsInteropUtils.getStringProperty(raw, "currentSelectItemName")
        ?: JsInteropUtils.getStringProperty(raw, "CurrentSelectItemName")
        ?: JsInteropUtils.getStringProperty(raw, "CurrentSelect_ItemName")
    
    val currentSelectItemIndex = JsInteropUtils.getIntProperty(raw, "currentSelectItemIndex")
        ?: JsInteropUtils.getIntProperty(raw, "CurrentSelectItemIndex")
        ?: JsInteropUtils.getIntProperty(raw, "CurrentSelect_ItemIndex")
    
    val eventTypeRaw = JsInteropUtils.getProperty(raw, "eventType")
        ?: JsInteropUtils.getProperty(raw, "EventType")
        ?: JsInteropUtils.getProperty(raw, "Event_Type")
    
    val eventType = osEventTypeListFromJs(eventTypeRaw)
    
    return ListItemEvent(
        containerID = containerID,
        containerName = containerName,
        currentSelectItemName = currentSelectItemName,
        currentSelectItemIndex = currentSelectItemIndex,
        eventType = eventType,
    )
}

/**
 * 从 JS 解析 TextItemEvent
 * 兼容多种字段名格式：camelCase 和 protoName
 */
internal fun textItemEventFromJs(raw: JsAny?): TextItemEvent? {
    if (raw == null) return null
    
    val containerID = JsInteropUtils.getIntProperty(raw, "containerID")
        ?: JsInteropUtils.getIntProperty(raw, "ContainerID")
        ?: JsInteropUtils.getIntProperty(raw, "Container_ID")
    
    val containerName = JsInteropUtils.getStringProperty(raw, "containerName")
        ?: JsInteropUtils.getStringProperty(raw, "ContainerName")
        ?: JsInteropUtils.getStringProperty(raw, "Container_Name")
    
    val eventTypeRaw = JsInteropUtils.getProperty(raw, "eventType")
        ?: JsInteropUtils.getProperty(raw, "EventType")
        ?: JsInteropUtils.getProperty(raw, "Event_Type")
    
    val eventType = osEventTypeListFromJs(eventTypeRaw)
    
    return TextItemEvent(
        containerID = containerID,
        containerName = containerName,
        eventType = eventType,
    )
}

/**
 * 从 JS 解析 SysItemEvent
 * 兼容多种字段名格式：camelCase 和 protoName
 */
internal fun sysItemEventFromJs(raw: JsAny?): SysItemEvent? {
    if (raw == null) return null
    
    val eventTypeRaw = JsInteropUtils.getProperty(raw, "eventType")
        ?: JsInteropUtils.getProperty(raw, "EventType")
        ?: JsInteropUtils.getProperty(raw, "Event_Type")
    
    val eventType = osEventTypeListFromJs(eventTypeRaw)
    
    return SysItemEvent(
        eventType = eventType,
    )
}

internal fun CreateStartUpPageContainer.toJsonString(): String =
    JsInteropUtils.buildJsonObject(
        "containerTotalNum" to containerTotalNum,
        "listObject" to listObject,
        "textObject" to textObject,
        "imageObject" to imageObject,
    )

internal fun RebuildPageContainer.toJsonString(): String =
    JsInteropUtils.buildJsonObject(
        "containerTotalNum" to containerTotalNum,
        "listObject" to listObject,
        "textObject" to textObject,
        "imageObject" to imageObject,
    )

internal fun ImageRawDataUpdate.toJsonString(): String {
    // 规范化 imageData：将 Uint8Array/ArrayBuffer 转换为 number[]
    val normalizedImageData = normalizeImageData(imageData)
    return JsInteropUtils.buildJsonObject(
        "containerID" to containerID,
        "containerName" to containerName,
        "imageData" to normalizedImageData,
    )
}

/**
 * 规范化图片数据：将 Uint8Array/ArrayBuffer 转换为 number[]
 * 对应 TypeScript 的 `ImageRawDataUpdate.normalizeImageData`
 */
private fun normalizeImageData(raw: Any?): Any? {
    if (raw == null) return null
    if (raw is String) return raw
    
    // 使用 js() 函数创建包装器来访问属性
    @Suppress("UNCHECKED_CAST")
    val isArray = js("(function(obj) { return Array.isArray(obj); })") as (Any?) -> Boolean
    @Suppress("UNCHECKED_CAST")
    val getLength = js("(function(obj) { return obj != null && typeof obj.length === 'number' ? obj.length : null; })") as (Any?) -> Int?
    @Suppress("UNCHECKED_CAST")
    val getByteLength = js("(function(obj) { return obj != null && typeof obj.byteLength === 'number' ? obj.byteLength : null; })") as (Any?) -> Int?
    @Suppress("UNCHECKED_CAST")
    val getArrayElement = js("(function(arr, index) { return arr != null ? arr[index] : null; })") as (Any?, Int) -> Any?
    @Suppress("UNCHECKED_CAST")
    val createUint8ArrayFromBuffer = js("(function(buffer) { return new Uint8Array(buffer); })") as (Any?) -> Any?
    
    // 检查是否是 JavaScript 数组（避免使用 Array<*> 类型检查以避免 Cloneable 错误）
    if (isArray(raw)) {
        val length = getLength(raw) ?: return raw
        return (0 until length).map { index ->
            val value = getArrayElement(raw, index) as? Number
            value?.toInt()?.and(0xff) ?: 0
        }
    }
    
    // 检查是否是 Uint8Array（通过检查是否有 length 属性）
    try {
        val length = getLength(raw)
        if (length != null && length >= 0) {
            // 可能是 Uint8Array，尝试转换为数组
            return (0 until length).map { index ->
                val value = getArrayElement(raw, index) as? Number
                value?.toInt()?.and(0xff) ?: 0
            }
        }
    } catch (e: Exception) {
        // 忽略错误，继续检查 ArrayBuffer
    }
    
    // 检查是否是 ArrayBuffer（通过检查 byteLength 属性）
    try {
        val byteLength = getByteLength(raw)
        if (byteLength != null && byteLength >= 0) {
            // 是 ArrayBuffer，转换为 Uint8Array 再转换为数组
            val uint8ArrayFromBuffer = createUint8ArrayFromBuffer(raw) ?: return raw
            val length = getLength(uint8ArrayFromBuffer) ?: return raw
            return (0 until length).map { index ->
                val value = getArrayElement(uint8ArrayFromBuffer, index) as? Number
                value?.toInt() ?: 0
            }
        }
    } catch (e: Exception) {
        // 忽略错误
    }
    
    return raw
}

internal fun TextContainerUpgrade.toJsonString(): String =
    JsInteropUtils.buildJsonObject(
        "containerID" to containerID,
        "containerName" to containerName,
        "contentOffset" to contentOffset,
        "contentLength" to contentLength,
        "content" to content,
    )

internal fun ShutDownContainer.toJsonString(): String =
    JsInteropUtils.buildJsonObject("exitMode" to exitMode)

// 注意：JSON 构建功能已移至 JsInteropUtils，这里只保留容器属性的扩展函数

// 扩展函数：将容器属性转换为 JSON Map（用于内部序列化）
// 注意：这些函数需要访问 JsInteropUtils.buildJsonObject，但由于需要处理特殊类型（如 ListItemContainerProperty），
// 暂时保留在这里。未来可以考虑将这些类型也移到 JsInteropUtils 中处理。

// 注意：这些 toJsonMap 函数需要能够处理 ListItemContainerProperty 等特殊类型
// 由于 JsInteropUtils.buildJsonObject 已经处理了基本类型，这里只需要调用它
// 但需要确保 ListItemContainerProperty 等类型能够正确序列化
private fun ListItemContainerProperty.toJsonMap(): String =
    JsInteropUtils.buildJsonObject(
        "itemCount" to itemCount,
        "itemWidth" to itemWidth,
        "isItemSelectBorderEn" to isItemSelectBorderEn,
        "itemName" to itemName,
    )

private fun ListContainerProperty.toJsonMap(): String =
    JsInteropUtils.buildJsonObject(
        "xPosition" to xPosition,
        "yPosition" to yPosition,
        "width" to width,
        "height" to height,
        "borderWidth" to borderWidth,
        "borderColor" to borderColor,
        "borderRdaius" to borderRdaius,
        "paddingLength" to paddingLength,
        "containerID" to containerID,
        "containerName" to containerName,
        "itemContainer" to itemContainer,
        "isEventCapture" to isEventCapture,
    )

private fun TextContainerProperty.toJsonMap(): String =
    JsInteropUtils.buildJsonObject(
        "xPosition" to xPosition,
        "yPosition" to yPosition,
        "width" to width,
        "height" to height,
        "borderWidth" to borderWidth,
        "borderColor" to borderColor,
        "borderRdaius" to borderRdaius,
        "paddingLength" to paddingLength,
        "containerID" to containerID,
        "containerName" to containerName,
        "isEventCapture" to isEventCapture,
        "content" to content,
    )

private fun ImageContainerProperty.toJsonMap(): String =
    JsInteropUtils.buildJsonObject(
        "xPosition" to xPosition,
        "yPosition" to yPosition,
        "width" to width,
        "height" to height,
        "containerID" to containerID,
        "containerName" to containerName,
    )
