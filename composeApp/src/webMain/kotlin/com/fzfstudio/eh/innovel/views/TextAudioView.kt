package com.fzfstudio.eh.innovel.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fzfstudio.eh.innovel.sdk.audioControl
import kotlinx.coroutines.launch

/**
 * 音频测试视图组件
 * 提供打开/关闭 Audio 的测试按钮，并展示通过 audioEvent 接收到的音频数据（转成文本）。
 *
 * @param displayLines 由 EvenHubEvent.audioEvent 解析出的展示行（PCM 长度、hex 等），由 AppState 维护
 */
@Composable
fun TextAudioView(
    displayLines: List<String> = emptyList(),
) {
    val coroutineScope = rememberCoroutineScope()
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    coroutineScope.launch {
                        val ok = audioControl(true)
                        if (ok) println("Audio opened") else println("Audio open failed")
                    }
                }
            ) {
                Text("打开 Audio")
            }
            Button(
                modifier = Modifier.weight(1f),
                onClick = {
                    coroutineScope.launch {
                        val ok = audioControl(false)
                        if (ok) println("Audio closed") else println("Audio close failed")
                    }
                }
            ) {
                Text("关闭 Audio")
            }
        }

        // 将接收到的音频数据转成文本并展示
        Text(
            text = "音频事件 (audioEvent)",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp, max = 200.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .verticalScroll(rememberScrollState())
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (displayLines.isEmpty()) {
                Text(
                    text = "暂无数据。打开 Audio 后，宿主推送的 PCM 会在此显示。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                displayLines.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
