@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.innovel

import com.fzfstudio.eh.innovel.sdk.DeviceInfo
import com.fzfstudio.eh.innovel.sdk.DeviceStatus
import com.fzfstudio.eh.innovel.sdk.UserInfo
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
actual fun formatJsObject(obj: Any?): String {
    if (obj == null) return "null"

    return try {
        // Render Kotlin models in a readable, multi-line format.
        formatKnownModel(obj)
    } catch (e: Exception) {
        obj.toString()
    }
}

private fun formatKnownModel(obj: Any?): String {
    if (obj == null) return "null"
    return when (obj) {
        is UserInfo -> jsonObject(
            "uid" to obj.uid,
            "name" to obj.name,
            "avatar" to obj.avatar,
            "country" to obj.country,
        )
        is DeviceInfo -> jsonObject(
            "model" to obj.model.toString(),
            "sn" to obj.sn,
            "status" to obj.status?.let { JsonRaw(formatKnownModel(it)) },
        )
        is DeviceStatus -> jsonObject(
            "sn" to obj.sn,
            "connectType" to obj.connectType.toString(),
            "isWearing" to obj.isWearing,
            "batteryLevel" to obj.batteryLevel,
            "isCharging" to obj.isCharging,
            "isInCase" to obj.isInCase,
        )
        else -> {
            // Fallback to JSON.stringify for raw JS objects.
            val jsTarget = obj as? JsAny
            jsTarget?.let { JSON.stringify(it) } ?: obj.toString()
        }
    }
}

private fun jsonObject(vararg fields: Pair<String, Any?>): String {
    val body = fields.joinToString(",") { (key, value) ->
        "\"${escapeJson(key)}\":${jsonValue(value)}"
    }
    return "{$body}"
}

private fun jsonValue(value: Any?): String = when (value) {
    null -> "null"
    is Number, is Boolean -> value.toString()
    is String -> "\"${escapeJson(value)}\""
    is JsonRaw -> value.value
    else -> value.toString()
}

private data class JsonRaw(val value: String)

private fun escapeJson(value: String): String {
    val sb = StringBuilder(value.length + 8)
    for (ch in value) {
        when (ch) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            else -> sb.append(ch)
        }
    }
    return sb.toString()
}

