package com.fzfstudio.eh.innovel.views

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fzfstudio.eh.innovel.sdk.SystemCapability
import com.fzfstudio.eh.innovel.sdk.SystemCapabilityStatus
import com.fzfstudio.eh.innovel.sdk.launchSystemCapability
import kotlinx.coroutines.launch

@Composable
fun SystemPermissionView() {
    val coroutineScope = rememberCoroutineScope()
    val resultLines = remember { mutableStateListOf<String>() }
    val running = remember { mutableStateOf<SystemCapability?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "系统能力调用",
                style = MaterialTheme.typography.titleSmall,
            )

            Text(
                text = "优先使用手机上更容易拉起的系统能力：拍照、相册、录像、录音、文件选择、定位。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SystemCapability.entries.forEach { capability ->
                    val isRunning = running.value == capability
                    Button(
                        onClick = {
                            running.value = capability
                            coroutineScope.launch {
                                try {
                                    val result = launchSystemCapability(capability)
                                    resultLines.add(
                                        0,
                                        "${capability.title}: ${result.status.toDisplayText()}${result.detail.takeIf { it.isNotBlank() }?.let { " - $it" } ?: ""}",
                                    )
                                    if (resultLines.size > 20) {
                                        resultLines.removeLast()
                                    }
                                } catch (e: Throwable) {
                                    resultLines.add(0, "${capability.title}: 出错 - ${e.message ?: "未知异常"}")
                                } finally {
                                    running.value = null
                                }
                            }
                        },
                    ) {
                        Text(if (isRunning) "${capability.title}..." else capability.title)
                    }
                }
            }

            Text(
                text = "调用日志",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp, max = 200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (resultLines.isEmpty()) {
                    Text(
                        text = "点击按钮后，这里会记录系统能力是否被拉起、是否取消或是否不支持。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    resultLines.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

private fun SystemCapabilityStatus.toDisplayText(): String = when (this) {
    SystemCapabilityStatus.Success -> "已调用"
    SystemCapabilityStatus.Cancelled -> "已取消"
    SystemCapabilityStatus.Unsupported -> "不支持"
    SystemCapabilityStatus.Error -> "出错"
}
