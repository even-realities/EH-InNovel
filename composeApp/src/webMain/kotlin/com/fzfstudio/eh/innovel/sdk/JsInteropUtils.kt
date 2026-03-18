@file:OptIn(ExperimentalWasmJsInterop::class)
@file:Suppress("UNCHECKED_CAST", "CAST_NEVER_SUCCEEDS")

package com.fzfstudio.eh.innovel.sdk

import kotlin.js.ExperimentalWasmJsInterop
import kotlin.js.JsAny
import kotlin.js.Promise
import kotlin.js.js
import kotlinx.coroutines.await

// ==================== Fetch API 外部声明（必须在顶层）====================

/**
 * JavaScript fetch 函数
 */
external fun fetch(url: String): Promise<JsAny>

/**
 * JavaScript 互操作工具类
 * 
 * 封装了所有与 JavaScript 交互的常用功能，包括：
 * - 类型转换（JsAny 到 Kotlin 类型）
 * - Promise 处理
 * - JSON 序列化/反序列化
 * - 数组和对象访问
 * - Fetch API 封装
 */
object JsInteropUtils {
    
    // ==================== 类型转换 ====================
    
    /**
     * 将 JsAny 转换为 String，如果为 null 或 undefined 则返回 null
     */
    fun toStringOrNull(value: JsAny?): String? = jsToStringOrNull(value)
    
    /**
     * 将 JsAny 转换为 String，如果为 null 或 undefined 则返回空字符串
     */
    fun toString(value: JsAny?): String = toStringOrNull(value) ?: ""
    
    /**
     * 将 JsAny 转换为 Int，如果为 null 或 undefined 则返回 null
     */
    fun toIntOrNull(value: JsAny?): Int? = jsToDoubleOrNull(value)?.toInt()
    
    /**
     * 将 JsAny 转换为 Int，如果为 null 或 undefined 则返回 0
     */
    fun toInt(value: JsAny?): Int = toIntOrNull(value) ?: 0
    
    /**
     * 将 JsAny 转换为 Double，如果为 null 或 undefined 则返回 null
     */
    fun toDoubleOrNull(value: JsAny?): Double? = jsToDoubleOrNull(value)
    
    /**
     * 将 JsAny 转换为 Boolean，如果为 null 或 undefined 则返回 null
     */
    fun toBooleanOrNull(value: JsAny?): Boolean? = jsToBoolOrNull(value)
    
    // ==================== 对象属性访问 ====================
    
    /**
     * 安全地获取 JavaScript 对象的属性值
     * @param obj JavaScript 对象
     * @param key 属性名
     * @return 属性值，如果不存在则返回 null
     */
    fun getProperty(obj: JsAny?, key: String): JsAny? = jsGet(obj, key)
    
    /**
     * 获取对象的字符串属性
     */
    fun getStringProperty(obj: JsAny?, key: String): String? = 
        toStringOrNull(getProperty(obj, key))
    
    /**
     * 获取对象的整数属性
     */
    fun getIntProperty(obj: JsAny?, key: String): Int? = 
        toIntOrNull(getProperty(obj, key))
    
    /**
     * 获取对象的布尔属性
     */
    fun getBooleanProperty(obj: JsAny?, key: String): Boolean? = 
        toBooleanOrNull(getProperty(obj, key))
    
    // ==================== 类型检查 ====================
    
    /**
     * 获取 JavaScript 值的类型字符串
     * @param value JavaScript 值
     * @return 类型字符串（'number', "string", "boolean", "object", "null", "undefined"）
     */
    fun getType(value: JsAny?): String {
        if (value == null) return "null"
        return when (value) {
            is Number -> "number"
            is Boolean -> "boolean"
            is String -> "string"
            else -> {
                // 对于对象，检查是否有 length 属性来判断是否是数组
                val length = getProperty(value, "length")
                if (length != null && length is Number) {
                    "object" // 可能是数组
                } else {
                    "object"
                }
            }
        }
    }
    
    /**
     * 检查值是否是 JavaScript 数组
     * @param value JavaScript 值
     * @return 如果是数组则返回 true
     */
    fun isArray(value: JsAny?): Boolean {
        if (value == null) return false
        val length = getProperty(value, "length")
        return length != null && length is Number && getType(length) == "number"
    }
    
    /**
     * 获取数组长度
     * @param array JavaScript 数组
     * @return 数组长度，如果不是数组则返回 0
     */
    fun getArrayLength(array: JsAny?): Int {
        if (!isArray(array)) return 0
        return toIntOrNull(getProperty(array, "length")) ?: 0
    }
    
    /**
     * 获取数组元素
     * @param array JavaScript 数组
     * @param index 索引
     * @return 数组元素，如果不存在则返回 null
     */
    fun getArrayElement(array: JsAny?, index: Int): JsAny? {
        if (!isArray(array)) return null
        return getProperty(array, index.toString())
    }
    
    // ==================== JSON 处理 ====================
    
    /**
     * 将 JavaScript 对象序列化为 JSON 字符串
     * @param obj JavaScript 对象
     * @return JSON 字符串
     */
    fun stringify(obj: JsAny?): String = jsStringify(obj)
    
    /**
     * 将 JSON 字符串解析为 JavaScript 对象
     * @param text JSON 字符串
     * @return JavaScript 对象
     */
    fun parseJson(text: String): JsAny = jsParseJson(text)
    
    // ==================== Promise 处理 ====================
    
    // 注意：使用 kotlinx.coroutines.await 扩展函数，而不是自定义实现
    // 这样可以避免在 WebView 环境中的链接错误
    
    // ==================== Fetch API ====================
    
    /**
     * 使用 Fetch API 获取文本内容
     * @param url 资源 URL
     * @return 文本内容
     */
    suspend fun fetchText(url: String): String {
        val response = fetch(url).await()
        // 使用 js() 函数调用 response.text()
        // 注意：js() 函数需要一个字符串表达式，不能直接使用变量
        // 所以我们使用一个包装函数
        @Suppress("UNCHECKED_CAST")
        val callText = js("(function(r) { return r.text(); })") as (JsAny) -> Promise<JsAny>
        val textPromise = callText(response)
        val textJs = textPromise.await()
        return toStringOrNull(textJs) ?: ""
    }
    
    /**
     * 使用 Fetch API 获取 JSON 内容
     * @param url 资源 URL
     * @return 解析后的 JavaScript 对象
     */
    suspend fun fetchJson(url: String): JsAny? {
        val text = fetchText(url)
        return if (text.isNotEmpty()) {
            try {
                parseJson(text)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    // ==================== JSON 构建工具 ====================
    
    /**
     * 构建 JSON 对象字符串
     * @param fields 字段列表（key-value 对）
     * @return JSON 对象字符串
     */
    @Suppress("UNCHECKED_CAST")
    fun buildJsonObject(vararg fields: Pair<String, Any?>): String {
        // 在 Kotlin/JS 中，vararg 会被转换为 Array
        // 为了避免 Cloneable 错误，我们使用 jsGet 来访问数组
        @Suppress("UNCHECKED_CAST")
        val fieldsJs = fields as? JsAny ?: return "{}"
        
        // 使用 jsGet 获取数组长度
        val length = toIntOrNull(getProperty(fieldsJs, "length")) ?: 0
        val fieldsList = mutableListOf<Pair<String, Any?>>()
        
        // 使用 jsGet 访问数组元素和 Pair 属性
        // 在 Kotlin/JS 中，尝试多种方式访问 Pair 的属性
        for (i in 0 until length) {
            val fieldJs = getProperty(fieldsJs, i.toString())
            if (fieldJs != null) {
                var key: String? = null
                var value: Any? = null
                
                // 方法1: 尝试直接类型转换为 Pair
                try {
                    @Suppress("UNCHECKED_CAST")
                    val pair = fieldJs as? Pair<*, *>
                    if (pair != null) {
                        key = pair.first as? String
                        value = pair.second
                    }
                } catch (e: Exception) {
                    // 继续尝试其他方法
                }
                
                // 方法2: 如果方法1失败，尝试使用 getProperty 访问 "first" 和 "second"
                if (key == null) {
                    key = toStringOrNull(getProperty(fieldJs, "first"))
                    value = getProperty(fieldJs, "second") as? Any?
                }
                
                // 方法3: 如果还是失败，尝试使用 bracket notation (通过 jsGet 访问)
                if (key == null) {
                    // 尝试访问可能的其他属性名
                    key = toStringOrNull(getProperty(fieldJs, "component1"))
                    if (key == null) {
                        value = getProperty(fieldJs, "component2") as? Any?
                    }
                }
                
                if (key != null) {
                    fieldsList.add(key to value)
                }
            }
        }
        
        return buildJsonObjectInternal(fieldsList)
    }
    
    /**
     * 内部实现：使用 List 构建 JSON 对象
     */
    private fun buildJsonObjectInternal(fields: List<Pair<String, Any?>>): String {
        val parts = mutableListOf<String>()
        for (field in fields) {
            val (key, value) = field
            if (value != null) {
                val jsonValueStr = jsonValue(value)
                parts.add("\"${escapeJson(key)}\":$jsonValueStr")
            }
        }
        val body = parts.joinToString(",")
        val result = "{$body}"
        return result
    }
    
    /**
     * 构建 JSON 数组字符串
     * @param values 值列表
     * @return JSON 数组字符串
     */
    fun buildJsonArray(values: List<*>): String {
        return values.joinToString(prefix = "[", postfix = "]") { jsonValue(it) }
    }
    
    /**
     * 将值转换为 JSON 字符串表示
     */
    @Suppress("UNCHECKED_CAST")
    private fun jsonValue(value: Any?): String {
        return when {
            value == null -> "null"
            value is Number -> {
                val asLong = value.toLong()
                if (asLong < 0) {
                    val unsigned = asLong and 0xFFFFFFFFL
                    unsigned.toString()
                } else {
                    asLong.toString()
                }
            }
            value is Boolean -> value.toString()
            value is String -> "\"${escapeJson(value)}\""
            // 先检查是否是 List（在 Kotlin/JS 中，List 在运行时是数组）
            // 注意：这个检查需要在 JsAny 检查之前，因为 List 可能不是 JsAny
            value is List<*> -> {
                buildJsonArray(value)
            }
            // 处理从 jsGet 获取的 JsAny 类型，尝试识别为数组或 List
            // 注意：在 Kotlin/JS 中，List 在运行时是 JavaScript 数组，所以先检查是否是数组
            value is JsAny -> {
                // 检查是否是数组（通过检查 length 属性）
                // 在 Kotlin/JS 中，List 在运行时是数组，所以 isArray 应该能匹配 List
                if (isArray(value)) {
                    val length = getArrayLength(value)
                    val list = mutableListOf<Any?>()
                    for (i in 0 until length) {
                        val element = getArrayElement(value, i)
                        @Suppress("UNCHECKED_CAST")
                        list.add(element as? Any?)
                    }
                    buildJsonArray(list)
                } else {
                    // 尝试识别为容器属性类型
                    val className = value::class.simpleName
                    when (className) {
                        "ListItemContainerProperty" -> {
                            @Suppress("UNCHECKED_CAST")
                            val prop = value as? ListItemContainerProperty
                            if (prop != null) {
                                buildJsonObject(
                                    "itemCount" to prop.itemCount,
                                    "itemWidth" to prop.itemWidth,
                                    "isItemSelectBorderEn" to prop.isItemSelectBorderEn,
                                    "itemName" to prop.itemName,
                                )
                            } else {
                                // 如果类型转换失败，尝试通过属性访问
                                buildJsonObject(
                                    "itemCount" to getIntProperty(value, "itemCount"),
                                    "itemWidth" to getIntProperty(value, "itemWidth"),
                                    "isItemSelectBorderEn" to getIntProperty(value, "isItemSelectBorderEn"),
                                    "itemName" to getProperty(value, "itemName"),
                                )
                            }
                        }
                        "ListContainerProperty" -> {
                            @Suppress("UNCHECKED_CAST")
                            val prop = value as? ListContainerProperty
                            if (prop != null) {
                                buildJsonObject(
                                    "xPosition" to prop.xPosition,
                                    "yPosition" to prop.yPosition,
                                    "width" to prop.width,
                                    "height" to prop.height,
                                    "borderWidth" to prop.borderWidth,
                                    "borderColor" to prop.borderColor,
                                    "borderRadius" to prop.borderRadius,
                                    "paddingLength" to prop.paddingLength,
                                    "containerID" to prop.containerID,
                                    "containerName" to prop.containerName,
                                    "itemContainer" to prop.itemContainer,
                                    "isEventCapture" to prop.isEventCapture,
                                )
                            } else {
                                // 通过属性访问
                                buildJsonObject(
                                    "xPosition" to getIntProperty(value, "xPosition"),
                                    "yPosition" to getIntProperty(value, "yPosition"),
                                    "width" to getIntProperty(value, "width"),
                                    "height" to getIntProperty(value, "height"),
                                    "borderWidth" to getIntProperty(value, "borderWidth"),
                                    "borderColor" to getIntProperty(value, "borderColor"),
                                    "borderRadius" to (getIntProperty(value, "borderRadius")
                                        ?: getIntProperty(value, "borderRadius")),
                                    "paddingLength" to getIntProperty(value, "paddingLength"),
                                    "containerID" to getIntProperty(value, "containerID"),
                                    "containerName" to getStringProperty(value, "containerName"),
                                    "itemContainer" to getProperty(value, "itemContainer"),
                                    "isEventCapture" to getIntProperty(value, "isEventCapture"),
                                )
                            }
                        }
                        "TextContainerProperty" -> {
                            @Suppress("UNCHECKED_CAST")
                            val prop = value as? TextContainerProperty
                            if (prop != null) {
                                buildJsonObject(
                                    "xPosition" to prop.xPosition,
                                    "yPosition" to prop.yPosition,
                                    "width" to prop.width,
                                    "height" to prop.height,
                                    "borderWidth" to prop.borderWidth,
                                    "borderColor" to prop.borderColor,
                                    "borderRadius" to prop.borderRadius,
                                    "paddingLength" to prop.paddingLength,
                                    "containerID" to prop.containerID,
                                    "containerName" to prop.containerName,
                                    "isEventCapture" to prop.isEventCapture,
                                    "content" to prop.content,
                                )
                            } else {
                                // 通过属性访问
                                buildJsonObject(
                                    "xPosition" to getIntProperty(value, "xPosition"),
                                    "yPosition" to getIntProperty(value, "yPosition"),
                                    "width" to getIntProperty(value, "width"),
                                    "height" to getIntProperty(value, "height"),
                                    "borderWidth" to getIntProperty(value, "borderWidth"),
                                    "borderColor" to getIntProperty(value, "borderColor"),
                                    "borderRadius" to (getIntProperty(value, "borderRadius")
                                        ?: getIntProperty(value, "borderRadius")),
                                    "paddingLength" to getIntProperty(value, "paddingLength"),
                                    "containerID" to getIntProperty(value, "containerID"),
                                    "containerName" to getStringProperty(value, "containerName"),
                                    "isEventCapture" to getIntProperty(value, "isEventCapture"),
                                    "content" to getStringProperty(value, "content"),
                                )
                            }
                        }
                        "ImageContainerProperty" -> {
                            @Suppress("UNCHECKED_CAST")
                            val prop = value as? ImageContainerProperty
                            if (prop != null) {
                                buildJsonObject(
                                    "xPosition" to prop.xPosition,
                                    "yPosition" to prop.yPosition,
                                    "width" to prop.width,
                                    "height" to prop.height,
                                    "containerID" to prop.containerID,
                                    "containerName" to prop.containerName,
                                )
                            } else {
                                // 通过属性访问
                                buildJsonObject(
                                    "xPosition" to getIntProperty(value, "xPosition"),
                                    "yPosition" to getIntProperty(value, "yPosition"),
                                    "width" to getIntProperty(value, "width"),
                                    "height" to getIntProperty(value, "height"),
                                    "containerID" to getIntProperty(value, "containerID"),
                                    "containerName" to getStringProperty(value, "containerName"),
                                )
                            }
                        }
                        else -> {
                            // 尝试转换为字符串
                            "\"${escapeJson(value.toString())}\""
                        }
                    }
                }
            }
            // 处理容器属性类型（直接类型检查，适用于非 JsAny 的情况）
            value is ListItemContainerProperty -> buildJsonObject(
                "itemCount" to value.itemCount,
                "itemWidth" to value.itemWidth,
                "isItemSelectBorderEn" to value.isItemSelectBorderEn,
                "itemName" to value.itemName,
                )
            value is ListContainerProperty -> buildJsonObject(
                "xPosition" to value.xPosition,
                "yPosition" to value.yPosition,
                "width" to value.width,
                "height" to value.height,
                "borderWidth" to value.borderWidth,
                "borderColor" to value.borderColor,
                "borderRadius" to value.borderRadius,
                "paddingLength" to value.paddingLength,
                "containerID" to value.containerID,
                "containerName" to value.containerName,
                "itemContainer" to value.itemContainer,
                "isEventCapture" to value.isEventCapture,
                )
            value is TextContainerProperty -> buildJsonObject(
                "xPosition" to value.xPosition,
                "yPosition" to value.yPosition,
                "width" to value.width,
                "height" to value.height,
                "borderWidth" to value.borderWidth,
                "borderColor" to value.borderColor,
                "borderRadius" to value.borderRadius,
                "paddingLength" to value.paddingLength,
                "containerID" to value.containerID,
                "containerName" to value.containerName,
                "isEventCapture" to value.isEventCapture,
                "content" to value.content,
                )
            value is ImageContainerProperty -> buildJsonObject(
                "xPosition" to value.xPosition,
                "yPosition" to value.yPosition,
                "width" to value.width,
                "height" to value.height,
                "containerID" to value.containerID,
                "containerName" to value.containerName,
                )
            else -> {
                // 处理数组类型，使用类型名称字符串检查避免 Cloneable 问题
                val arrayValue = tryConvertToArray(value)
                if (arrayValue != null) {
                    buildJsonArray(arrayValue)
                } else {
                    "\"${escapeJson(value.toString())}\""
                }
            }
        }
    }

    /**
     * 尝试将值转换为 List，用于处理 ByteArray 和 IntArray
     */
    @Suppress("UNCHECKED_CAST")
    private fun tryConvertToArray(value: Any?): List<*>? {
        if (value == null) return null
        return try {
            val className = value::class.simpleName
            when (className) {
                "ByteArray", "IntArray" -> {
                    val jsValue = value as? JsAny
                    if (jsValue != null) {
                        val length = toIntOrNull(getProperty(jsValue, "length"))
                            ?: toIntOrNull(getProperty(jsValue, "size")) ?: 0
                        (0 until length).mapNotNull { index ->
                            toDoubleOrNull(getProperty(jsValue, index.toString()))
                        }
                    } else {
                        null
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 转义 JSON 字符串中的特殊字符
     */
    private fun escapeJson(value: String): String = buildString {
        for (ch in value) {
            when (ch) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(ch)
            }
        }
    }
}
