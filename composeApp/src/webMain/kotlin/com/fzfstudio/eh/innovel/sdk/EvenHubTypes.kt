package com.fzfstudio.eh.innovel.sdk

/**
 * 从 JS SDK 返回值解析出的 Kotlin 端模型。
 *
 * 我们将这些保留为纯 Kotlin 模型（没有 `external`），以避免
 * 同步所有 JS 声明并避免外部枚举弃用。
 */
data class UserInfo(
    /** 用户 ID */
    val uid: Int?,
    /** 用户名 */
    val name: String,
    /** 头像（图片原始数据） */
    val avatar: String,
    /** 国家 */
    val country: String,
)

/**
 * 设备连接状态。
 */
enum class DeviceConnectType {
    None,
    Connecting,
    Connected,
    Disconnected,
    ConnectionFailed;

    companion object {
        fun fromString(value: String?): DeviceConnectType = when (value?.lowercase()) {
            "connecting" -> Connecting
            "connected" -> Connected
            "disconnected" -> Disconnected
            "connectionfailed" -> ConnectionFailed
            else -> None
        }
    }
}

/**
 * 设备状态信息。
 */
data class DeviceStatus(
    /** 设备序列号 */
    val sn: String,
    /** 连接类型 */
    val connectType: DeviceConnectType,
    /** 是否佩戴中 */
    val isWearing: Boolean?,
    /** 电量 (0-100) */
    val batteryLevel: Int?,
    /** 是否充电中 */
    val isCharging: Boolean?,
    /** 是否在充电盒中 */
    val isInCase: Boolean?,
)

/**
 * 设备型号类型。
 */
enum class DeviceModel {
    G1,
    G2,
    Ring1;

    companion object {
        fun fromString(value: String?): DeviceModel = when (value?.lowercase()) {
            "g2" -> G2
            "ring1" -> Ring1
            else -> G1
        }
    }
}

/**
 * 聚合的设备信息。
 */
data class DeviceInfo(
    /** 设备型号 */
    val model: DeviceModel,
    /** 设备序列号 */
    val sn: String,
    /** 当前设备状态 */
    var status: DeviceStatus?,
) {
    /**
     * 如果 SN 匹配，则更新设备状态。
     */
    fun updateStatus(status: DeviceStatus) {
        if (status.sn == sn) {
           this.status = status
        }
    }
}

/**
 * 操作系统事件类型枚举。
 */
enum class OsEventTypeList(val value: Int) {
    CLICK_EVENT(0),
    SCROLL_TOP_EVENT(1),
    SCROLL_BOTTOM_EVENT(2),
    DOUBLE_CLICK_EVENT(3),
    FOREGROUND_ENTER_EVENT(4),
    FOREGROUND_EXIT_EVENT(5),
    ABNORMAL_EXIT_EVENT(6);
    
    companion object {
        fun fromInt(value: Int?): OsEventTypeList? {
            return values().find { it.value == value }
        }
        
        fun fromString(value: String?): OsEventTypeList? {
            if (value == null) return null
            return values().find { 
                it.name.equals(value, ignoreCase = true) ||
                it.name.replace("_EVENT", "").equals(value, ignoreCase = true)
            }
        }
    }
}

/**
 * 列表项事件。
 */
data class ListItemEvent(
    /** 容器 ID */
    val containerID: Int? = null,
    /** 容器名称 */
    val containerName: String? = null,
    /** 当前选中的项名称 */
    val currentSelectItemName: String? = null,
    /** 当前选中的项索引 */
    val currentSelectItemIndex: Int? = null,
    /** 事件类型 */
    val eventType: OsEventTypeList? = null,
)

/**
 * 文本项事件。
 */
data class TextItemEvent(
    /** 容器 ID */
    val containerID: Int? = null,
    /** 容器名称 */
    val containerName: String? = null,
    /** 事件类型 */
    val eventType: OsEventTypeList? = null,
)

/**
 * 系统项事件。
 */
data class SysItemEvent(
    /** 事件类型 */
    val eventType: OsEventTypeList? = null,
)

/**
 * 音频事件 payload：宿主通过 onAudioData 推送的 PCM 字节码。
 * 与 @evenrealities/even_hub_sdk 的 AudioEventPayload 对齐。
 */
data class AudioEventPayload(
    /** PCM 原始字节（0–255） */
    val audioPcm: List<Int>,
)

/**
 * EvenHub 发出的事件。
 * 
 * 新的结构直接包含解析后的事件对象，而不是原始的 JSON 字符串。
 * 开发者只需要判断哪个属性不为空，就可以直接使用对应的事件对象。
 */
data class EvenHubEvent(
    /** 列表事件（如果存在） */
    val listEvent: ListItemEvent? = null,
    /** 文本事件（如果存在） */
    val textEvent: TextItemEvent? = null,
    /** 系统事件（如果存在） */
    val sysEvent: SysItemEvent? = null,
    /** 音频事件（PCM 字节码，如果存在） */
    val audioEvent: AudioEventPayload? = null,
    /** 原始 JSON 数据（可选，便于调试/回放） */
    val jsonData: String? = null,
)

/**
 * EvenHub - PB 接口参数模型（对齐宿主 BleG2CmdProtoEvenHubExt）
 * 列表项容器属性。
 */
data class ListItemContainerProperty(
    /** 列表项数量 */
    val itemCount: Int? = null,
    /** 单个项宽度 */
    val itemWidth: Int? = null,
    /** 是否启用项选择边框 (1: 启用, 0: 禁用) */
    val isItemSelectBorderEn: Int? = null,
    /** 项名称列表 */
    val itemName: List<String>? = null,
)

/**
 * 列表容器属性。
 */
data class ListContainerProperty(
    /** X 坐标位置 */
    val xPosition: Int? = null,
    /** Y 坐标位置 */
    val yPosition: Int? = null,
    /** 容器宽度 */
    val width: Int? = null,
    /** 容器高度 */
    val height: Int? = null,
    /** 边框宽度 */
    val borderWidth: Int? = null,
    /** 边框颜色值 */
    val borderColor: Int? = null,
    /** 边框圆角半径 */
    val borderRdaius: Int? = null,
    /** 内边距长度 */
    val paddingLength: Int? = null,
    /** 唯一容器 ID */
    val containerID: Int? = null,
    /** 容器名称 */
    val containerName: String? = null,
    /** 列表内的项属性 */
    val itemContainer: ListItemContainerProperty? = null,
    /** 是否捕获事件 */
    val isEventCapture: Int? = null,
)

/**
 * 文本容器属性。
 */
data class TextContainerProperty(
    /** X 坐标位置 */
    val xPosition: Int? = null,
    /** Y 坐标位置 */
    val yPosition: Int? = null,
    /** 容器宽度 */
    val width: Int? = null,
    /** 容器高度 */
    val height: Int? = null,
    /** 边框宽度 */
    val borderWidth: Int? = null,
    /** 边框颜色值 */
    val borderColor: Int? = null,
    /** 边框圆角半径 */
    val borderRdaius: Int? = null,
    /** 内边距长度 */
    val paddingLength: Int? = null,
    /** 唯一容器 ID */
    val containerID: Int? = null,
    /** 容器名称 */
    val containerName: String? = null,
    /** 是否捕获事件 */
    val isEventCapture: Int? = null,
    /** 文本内容 */
    val content: String? = null,
)

/**
 * 图片容器属性。
 */
data class ImageContainerProperty(
    /** X 坐标位置 */
    val xPosition: Int? = null,
    /** Y 坐标位置 */
    val yPosition: Int? = null,
    /** 容器宽度 */
    val width: Int? = null,
    /** 容器高度 */
    val height: Int? = null,
    /** 唯一容器 ID */
    val containerID: Int? = null,
    /** 容器名称 */
    val containerName: String? = null,
)

/**
 * 创建启动页容器的数据结构。
 */
data class CreateStartUpPageContainer(
    /** 容器总数 */
    val containerTotalNum: Int? = null,
    /** 列表容器对象列表 */
    val listObject: List<ListContainerProperty>? = null,
    /** 文本容器对象列表 */
    val textObject: List<TextContainerProperty>? = null,
    /** 图片容器对象列表 */
    val imageObject: List<ImageContainerProperty>? = null,
)

/**
 * 重建页面容器的数据结构。
 */
data class RebuildPageContainer(
    /** 容器总数 */
    val containerTotalNum: Int? = null,
    /** 列表容器对象列表 */
    val listObject: List<ListContainerProperty>? = null,
    /** 文本容器对象列表 */
    val textObject: List<TextContainerProperty>? = null,
    /** 图片容器对象列表 */
    val imageObject: List<ImageContainerProperty>? = null,
)

/**
 * 更新图片原始数据的数据结构。
 * 
 * 对应宿主 Dart：`EvenHubImageContainer`
 * 
 * 说明：
 * - 这里只做字段模型 + JSON 映射，不做 protobuf bytes 编解码
 * - `imageData` 建议传 **number[]**（宿主 `List<int>` 最好接）
 * - 若传 Uint8Array/ArrayBuffer，会在 `toJson` 时转换为 number[]
 */
data class ImageRawDataUpdate(
    /** 容器 ID */
    val containerID: Int? = null,
    /** 容器名称 */
    val containerName: String? = null,
    /** 图片数据（可以是 number[]、string、Uint8Array 或 ArrayBuffer） */
    val imageData: Any? = null,
)

/**
 * 升级文本容器内容的数据结构。
 */
data class TextContainerUpgrade(
    /** 容器 ID */
    val containerID: Int? = null,
    /** 容器名称 */
    val containerName: String? = null,
    /** 内容偏移量 */
    val contentOffset: Int? = null,
    /** 内容长度 */
    val contentLength: Int? = null,
    /** 新的文本内容 */
    val content: String? = null,
)

/**
 * 关闭容器的数据结构。
 */
data class ShutDownContainer(
    /** 退出模式 (默认 0) */
    val exitMode: Int = 0,
)
