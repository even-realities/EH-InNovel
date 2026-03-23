package com.fzfstudio.eh.innovel.sdk

enum class SystemCapability(
    val title: String,
    val description: String,
) {
    TakePhoto(
        title = "拍照",
        description = "调用系统相机拍照",
    ),
    PickImage(
        title = "选图片",
        description = "调用系统相册/图片选择器",
    ),
    RecordVideo(
        title = "录像",
        description = "调用系统相机录像",
    ),
    RecordAudio(
        title = "录音",
        description = "调用系统录音/麦克风",
    ),
    PickFile(
        title = "选文件",
        description = "调用系统文件选择器",
    ),
    GetLocation(
        title = "定位",
        description = "调用系统定位授权",
    ),
}

enum class SystemCapabilityStatus {
    Success,
    Cancelled,
    Unsupported,
    Error,
}

data class SystemCapabilityResult(
    val capability: SystemCapability,
    val status: SystemCapabilityStatus,
    val detail: String,
)

expect suspend fun launchSystemCapability(capability: SystemCapability): SystemCapabilityResult
