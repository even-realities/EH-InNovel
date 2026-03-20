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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fzfstudio.eh.innovel.sdk.ImuReportPace
import com.fzfstudio.eh.innovel.sdk.imuControl
import kotlinx.coroutines.launch

/**
 * IMU 测试：开关上报，并展示 `sysEvent.imuData` 解析后的文本行（与 [TextAudioView] 展示风格一致）。
 *
 * @param displayLines 由 [com.fzfstudio.eh.innovel.models.AppState] 根据 EvenHub `sysEvent` 维护
 */
@Composable
fun TextImuView(
    displayLines: List<String> = emptyList(),
) {
    val coroutineScope = rememberCoroutineScope()
    val imuEnabled = remember { mutableStateOf(false) }
    val selectedReportFrq = remember { mutableStateOf(ImuReportPace.P100) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "IMU 上报",
                    style = MaterialTheme.typography.titleSmall,
                )
                Switch(
                    checked = imuEnabled.value,
                    onCheckedChange = { wantOn ->
                        coroutineScope.launch {
                            val ok = imuControl(
                                isOpen = wantOn,
                                reportFrq = selectedReportFrq.value,
                            )
                            if (ok) {
                                imuEnabled.value = wantOn
                                if (wantOn) {
                                    println("IMU reporting on (pace=${selectedReportFrq.value.value})")
                                } else {
                                    println("IMU reporting off")
                                }
                            } else {
                                println("imuControl failed (requested ${if (wantOn) "on" else "off"})")
                            }
                        }
                    },
                )
            }

            Text(
                text = "report pace: ${selectedReportFrq.value.value}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ImuReportPace.entries.forEach { frequency ->
                    val isSelected = selectedReportFrq.value == frequency
                    Button(
                        modifier = Modifier,
                        onClick = {
                            selectedReportFrq.value = frequency
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            contentColor = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        ),
                    ) {
                        Text("${frequency.value}")
                    }
                }
            }

            Text(
                text = "IMU 数据 (sysEvent.imuData)",
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
                if (displayLines.isEmpty()) {
                    Text(
                        text = "暂无数据。打开开关后，宿主推送的 x/y/z 会在此显示。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    displayLines.forEach { line ->
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
