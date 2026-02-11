package com.fzfstudio.eh.innovel.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fzfstudio.eh.innovel.sdk.*
import kotlinx.coroutines.launch
import kotlin.js.js

/**
 * 文本图片测试视图组件
 * 包含可调整宽高的画布（立体长方形预览）、测试上传按钮
 */
@Composable
fun TextImageView() {
    val coroutineScope = rememberCoroutineScope()
    var containerId by remember { mutableStateOf<Int?>(null) }
    
    // 宽高状态，默认 90
    var width by remember { mutableStateOf(90) }
    var height by remember { mutableStateOf(90) }
    var widthText by remember { mutableStateOf("90") }
    var heightText by remember { mutableStateOf("90") }
    
    // 计算显示尺寸（取宽高中的较大值，确保画布能完整显示）
    val displaySize = maxOf(width, height).coerceAtLeast(50).coerceAtMost(200)
    
    Column(
        modifier = Modifier
            .width((displaySize + 100).dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 宽高输入框
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("宽 x 高", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 宽度输入框
                Column(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = widthText,
                        onValueChange = { newValue ->
                            // 只允许输入数字
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                widthText = newValue
                                newValue.toIntOrNull()?.let { w ->
                                    if (w > 0 && w <= 500) {
                                        width = w
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (widthText.isEmpty()) {
                                Text(
                                    text = "90",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            innerTextField()
                        }
                    )
                }
                Text("x", modifier = Modifier.padding(horizontal = 4.dp))
                // 高度输入框
                Column(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = heightText,
                        onValueChange = { newValue ->
                            // 只允许输入数字
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                heightText = newValue
                                newValue.toIntOrNull()?.let { h ->
                                    if (h > 0 && h <= 500) {
                                        height = h
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusable()
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (heightText.isEmpty()) {
                                Text(
                                    text = "90",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }
        
        // 画布（显示用，使用计算后的显示尺寸）
        Canvas(
            modifier = Modifier
                .size(displaySize.dp)
        ) {
            draw3DRectangleWithSize(width, height)
        }
        
        // 测试按钮
        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                coroutineScope.launch {
                    handleTestButtonClick(width, height) { id ->
                        containerId = id
                    }
                }
            }
        ) {
            Text("测试上传 (${width}x${height})")
        }
    }
}

/**
 * 绘制立体长方形（使用实际宽高）
 */
private fun DrawScope.draw3DRectangleWithSize(actualWidth: Int, actualHeight: Int) {
    val canvasWidth = size.width
    val canvasHeight = size.height
    
    // 计算缩放比例，使内容适应画布大小
    val scaleX = canvasWidth / actualWidth.toFloat()
    val scaleY = canvasHeight / actualHeight.toFloat()
    val scale = minOf(scaleX, scaleY)
    
    // 计算居中偏移
    val offsetX = (canvasWidth - actualWidth * scale) / 2f
    val offsetY = (canvasHeight - actualHeight * scale) / 2f
    
    val padding = 10f * scale
    val rectWidth = actualWidth * scale - padding * 2
    val rectHeight = actualHeight * scale - padding * 2
    val rectX = offsetX + padding
    val rectY = offsetY + padding
    
    // 主矩形（浅色）
    drawRect(
        color = Color(0xFF4A90E2),
        topLeft = Offset(rectX, rectY),
        size = Size(rectWidth, rectHeight)
    )
    
    // 绘制立体效果 - 顶部和左侧（亮面）
    val depth = 8f * scale
    
    // 顶部亮面
    drawRect(
        color = Color(0xFF6BA3E8),
        topLeft = Offset(rectX, rectY),
        size = Size(rectWidth, depth)
    )
    
    // 左侧亮面
    drawRect(
        color = Color(0xFF6BA3E8),
        topLeft = Offset(rectX, rectY),
        size = Size(depth, rectHeight)
    )
    
    // 底部和右侧（暗面）
    // 底部暗面
    drawRect(
        color = Color(0xFF2E5C8A),
        topLeft = Offset(rectX, rectY + rectHeight - depth),
        size = Size(rectWidth, depth)
    )
    
    // 右侧暗面
    drawRect(
        color = Color(0xFF2E5C8A),
        topLeft = Offset(rectX + rectWidth - depth, rectY),
        size = Size(depth, rectHeight)
    )
}

/**
 * 处理测试按钮点击事件
 * @param width 图片宽度（整数）
 * @param height 图片高度（整数）
 */
private suspend fun handleTestButtonClick(
    width: Int,
    height: Int,
    onContainerCreated: (Int?) -> Unit
) {
    try {
        // 1. 创建 HTML Canvas 并绘制内容（使用输入的宽高，整数尺寸）
        @Suppress("UNCHECKED_CAST")
        val createCanvas = js("(function(w, h) { var c = document.createElement('canvas'); c.width = w; c.height = h; return c; })") as (Int, Int) -> Any?
        val canvas = createCanvas(width, height) ?: throw Exception("Failed to create canvas")
        
        @Suppress("UNCHECKED_CAST")
        val getContext = js("(function(canvas) { return canvas.getContext('2d'); })") as (Any?) -> Any?
        val ctx = getContext(canvas) ?: throw Exception("Failed to get 2D context")
        
        // 绘制立体长方形（使用输入的宽高）
        draw3DRectangleOnCanvas(ctx, width, height)
        
        // 2. 将画布转换为图片数据（base64）
        @Suppress("UNCHECKED_CAST")
        val toDataURL = js("(function(canvas) { return canvas.toDataURL('image/png'); })") as (Any?) -> String
        val imageDataUrl = toDataURL(canvas)
        
        // 3. 将 base64 转换为 ArrayBuffer
        val base64Data = imageDataUrl.substringAfter(",")
        @Suppress("UNCHECKED_CAST")
        val atob = js("(function(str) { return window.atob(str); })") as (String) -> String
        val binaryString = atob(base64Data)
        val length = binaryString.length
        
        @Suppress("UNCHECKED_CAST")
        val createUint8Array = js("(function(len) { return new Uint8Array(len); })") as (Int) -> Any?
        val bytes = createUint8Array(length) ?: throw Exception("Failed to create Uint8Array")
        
        @Suppress("UNCHECKED_CAST")
        val setByte = js("(function(arr, index, value) { arr[index] = value; })") as (Any?, Int, Byte) -> Unit
        for (i in 0 until length) {
            setByte(bytes, i, binaryString[i].code.toByte())
        }
        
        @Suppress("UNCHECKED_CAST")
        val getBuffer = js("(function(arr) { return arr.buffer; })") as (Any?) -> Any?
        val arrayBuffer = getBuffer(bytes)
        
        // 4. 创建图片容器（使用输入的宽高，整数尺寸）
        val imageContainer = ImageContainerProperty(
            containerID = 100, // 使用一个唯一的 ID
            containerName = "testImage",
            xPosition = 0,
            yPosition = 0,
            width = width,  // 使用输入的宽度
            height = height  // 使用输入的高度
        )
        
        val container = CreateStartUpPageContainer(
            containerTotalNum = 1,
            imageObject = listOf(imageContainer)
        )
        
        val createdContainerId = createStartUpPageContainer(container)
        onContainerCreated(createdContainerId)
        
        if (createdContainerId != null) {
            // 5. 上传图片数据
            val imageUpdate = ImageRawDataUpdate(
                containerID = 100,
                containerName = "testImage",
                imageData = arrayBuffer // 可以是 ArrayBuffer，会在 toJson 时转换为 number[]
            )
            
            val success = updateImageRawData(imageUpdate)
            if (success) {
                println("Image uploaded successfully")
            } else {
                println("Failed to upload image")
            }
        }
        
    } catch (e: Exception) {
        println("Error in handleTestButtonClick: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 在 HTML Canvas 上绘制立体长方形
 * @param ctx Canvas 2D 上下文
 * @param width 画布宽度（整数）
 * @param height 画布高度（整数）
 */
private fun draw3DRectangleOnCanvas(
    ctx: Any?,
    width: Int,
    height: Int
) {
    if (ctx == null) return
    
    // 使用整数计算，确保没有小数点
    val padding = 10
    val rectWidth = width - padding * 2
    val rectHeight = height - padding * 2
    val rectX = padding
    val rectY = padding
    val depth = 8
    
    @Suppress("UNCHECKED_CAST")
    val setFillStyle = js("(function(ctx, color) { ctx.fillStyle = color; })") as (Any?, String) -> Unit
    @Suppress("UNCHECKED_CAST")
    val fillRect = js("(function(ctx, x, y, w, h) { ctx.fillRect(Math.round(x), Math.round(y), Math.round(w), Math.round(h)); })") as (Any?, Double, Double, Double, Double) -> Unit
    
    // 绘制主矩形（前面）
    setFillStyle(ctx, "#4A90E2")
    fillRect(ctx, rectX.toDouble(), rectY.toDouble(), rectWidth.toDouble(), rectHeight.toDouble())
    
    // 顶部亮面
    setFillStyle(ctx, "#6BA3E8")
    fillRect(ctx, rectX.toDouble(), rectY.toDouble(), rectWidth.toDouble(), depth.toDouble())
    
    // 左侧亮面
    setFillStyle(ctx, "#6BA3E8")
    fillRect(ctx, rectX.toDouble(), rectY.toDouble(), depth.toDouble(), rectHeight.toDouble())
    
    // 底部暗面
    setFillStyle(ctx, "#2E5C8A")
    fillRect(ctx, rectX.toDouble(), (rectY + rectHeight - depth).toDouble(), rectWidth.toDouble(), depth.toDouble())
    
    // 右侧暗面
    setFillStyle(ctx, "#2E5C8A")
    fillRect(ctx, (rectX + rectWidth - depth).toDouble(), rectY.toDouble(), depth.toDouble(), rectHeight.toDouble())
}

