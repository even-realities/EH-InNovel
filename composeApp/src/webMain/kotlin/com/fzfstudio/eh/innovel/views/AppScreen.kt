package com.fzfstudio.eh.innovel.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fzfstudio.eh.innovel.models.AppUiState
import com.fzfstudio.eh.innovel.models.BookModel
import com.fzfstudio.eh.innovel.sdk.ShutDownContainer
import com.fzfstudio.eh.innovel.views.ReadingDialog
import com.fzfstudio.eh.innovel.sdk.shutDownPageContainer
import kotlinx.coroutines.launch

/**
 * 应用主屏幕组件。
 * 包含用户信息、设备信息和图书列表。
 *
 * @param uiState 应用 UI 状态
 * @param onStartReading 开始阅读的回调函数
 */
@Composable
fun AppScreen(
    uiState: AppUiState,
    onStartReading: (BookModel) -> Unit,
    onExitReading: () -> Unit
) {
    val readingBook = remember { mutableStateOf<BookModel?>(null) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.errorMessage != null) {
            ErrorBanner(uiState.errorMessage)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            UserBookshelfCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                userInfo = uiState.userInfo
            )
            Spacer(modifier = Modifier.width(12.dp))
            DeviceInfoCard(
                modifier = Modifier
                    .width(60.dp)
                    .height(30.dp),
                deviceInfo = uiState.deviceInfo,
                deviceStatus = uiState.deviceStatus
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            uiState.books.forEach { book ->
                BookItem(
                    book = book,
                    onStartReading = {
                        onStartReading(book)
                        readingBook.value = book
                    }
                )
            }
        }

        // Test Panel：集中放置各 Test 视图
        Text(
            text = "Test Panel",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextImageView()
            TextAudioView(displayLines = uiState.audioEventDisplayLines)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 退出按钮：固定在页面最下方，便于退出，不属于任何 Test 视图
        val coroutineScope = rememberCoroutineScope()
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                coroutineScope.launch {
                    try {
                        val success = shutDownPageContainer(ShutDownContainer(exitMode = 0))
                        if (success) {
                            println("EvenHub exited successfully")
                        } else {
                            println("Failed to exit EvenHub")
                        }
                    } catch (e: Exception) {
                        println("Error on exit: ${e.message}")
                        e.printStackTrace()
                    }
                }
            }
        ) {
            Text("退出 EvenHub")
        }
    }
    val currentBook = readingBook.value
    if (currentBook != null) {
        ReadingDialog(
            show = true,
            book = currentBook,
            onExit = {
                readingBook.value = null
                // 在协程作用域中调用 suspend 函数
                onExitReading()
            }
        )
    }
}
