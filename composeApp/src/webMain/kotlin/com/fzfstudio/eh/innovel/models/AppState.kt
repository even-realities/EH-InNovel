@file:OptIn(ExperimentalWasmJsInterop::class)

package com.fzfstudio.eh.innovel.models

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fzfstudio.eh.innovel.sdk.*
import kotlin.js.ExperimentalWasmJsInterop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 页面 UI 状态聚合，供视图层渲染。
 */
data class AppUiState(
    /** SDK 桥接是否已初始化完成 */
    val isBridgeReady: Boolean = false,
    /** 用户信息（可能为空） */
    val userInfo: UserInfo? = null,
    /** 设备基础信息（可能为空） */
    val deviceInfo: DeviceInfo? = null,
    /** 设备状态（可能为空） */
    val deviceStatus: DeviceStatus? = null,
    /** 书架图书列表 */
    val books: List<BookModel> = emptyList(),
    /** 页面级错误提示（可为空） */
    val errorMessage: String? = null,
    /** 是否在大屏阅读 */
    val isFullScreenReading: Boolean = false,
    /** 音频事件展示行（Test Audio 用，最近 N 条） */
    val audioEventDisplayLines: List<String> = emptyList(),
)

/**
 * 页面业务状态与数据拉取逻辑。
 */
class AppState {
    /** 页面 UI 状态 */
    var uiState by mutableStateOf(AppUiState(books = emptyList()))
        private set
    
    /** 取消设备状态监听的函数 */
    private var unsubscribeDeviceStatus: (() -> Unit)? = null
    /** 取消 EvenHubEvent 监听的函数 */
    private var unsubscribeEvenHubEvent: (() -> Unit)? = null
    
    /** 当前阅读的书本 ID，默认为 book_001 */
    private var currentReadingBookId: String = "book_001"
    
    /** 当前阅读的章节索引，默认为 0 */
    private var currentChapterIndex: Int = 0
    
    /** 阅读片段列表（按 1KB 拆分） */
    private var readingFragments: List<String> = emptyList()
    
    /** 当前阅读的片段索引 */
    private var readingFragmentIndex: Int = 0
    
    /** 协程作用域，用于在事件处理中调用 suspend 函数 */
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * 初始化 SDK 桥接并拉取用户/设备信息。
     */
    suspend fun initialize() {
        if (uiState.isBridgeReady) return
        try {
            ensureEvenAppBridge()
            val userInfo = runCatching { getUserInfo() }
                .getOrElse { error ->
                    uiState = uiState.copy(errorMessage = "Failed to get user info: ${error.message}")
                    null
                }
            val deviceInfo = runCatching { getDeviceInfo() }
                .getOrElse { error ->
                    uiState = uiState.copy(errorMessage = "Failed to get device info: ${error.message}")
                    null
                }
            // 从 JSON 文件加载图书数据
            val books = runCatching { loadBooksFromJson() }
                .getOrElse { error ->
                    // 如果加载失败，使用默认数据
                    defaultBooks()
                }
            uiState = uiState.copy(
                isBridgeReady = true,
                userInfo = userInfo,
                deviceInfo = deviceInfo,
                deviceStatus = deviceInfo?.status,
                books = books,
            )
            // 初始化完成后，设置设备状态监听
            setupDeviceStatusObserver()
            // 设置 EvenHubEvent 监听
            setupEvenHubEventObserver()
        } catch (e: Exception) {
            uiState = uiState.copy(errorMessage = "Failed to initialize bridge: ${e.message}")
        }
    }
    
    /**
     * 设置设备状态监听
     */
    private fun setupDeviceStatusObserver() {
        // 取消之前的监听（如果存在）
        unsubscribeDeviceStatus?.invoke()
        // 设置新的监听
        unsubscribeDeviceStatus = observeDeviceStatus { status ->
            if (status != null) {
                // 更新设备状态
                updateDeviceStatus(status)
            }
        }
    }
    
    /**
     * 设置 EvenHubEvent 监听
     */
    private fun setupEvenHubEventObserver() {
        // 取消之前的监听（如果存在）
        unsubscribeEvenHubEvent?.invoke()
        // 设置新的监听并解析事件数据
        unsubscribeEvenHubEvent = observeEvenHubEvent { event ->
            if (event != null) {
                // 新的结构直接包含解析后的事件对象
                // 只需要判断哪个属性不为空，就可以直接使用对应的事件对象
                
                when {
                    event.listEvent != null -> {
                        val listEvent = event.listEvent
                        println("[EvenHubEvent] ListItemEvent - ContainerID: ${listEvent.containerID}, " +
                                "ContainerName: ${listEvent.containerName}, " +
                                "ItemIndex: ${listEvent.currentSelectItemIndex}, " +
                                "ItemName: ${listEvent.currentSelectItemName}, " +
                                "EventType: ${listEvent.eventType}")
                        handleListItemEvent(listEvent)
                    }
                    event.textEvent != null -> {
                        val textEvent = event.textEvent
                        println("[EvenHubEvent] TextItemEvent - ContainerID: ${textEvent.containerID}, " +
                                "ContainerName: ${textEvent.containerName}, " +
                                "EventType: ${textEvent.eventType}")
                        handleTextItemEvent(textEvent)
                    }
                    event.sysEvent != null -> {
                        val sysEvent = event.sysEvent
                        println("[EvenHubEvent] SysItemEvent - EventType: ${sysEvent.eventType}")
                        handleSysItemEvent(sysEvent)
                    }
                    event.audioEvent != null -> {
                        val audioEvent = event.audioEvent
                        appendAudioEventDisplay(audioEvent)
                    }
                    else -> {
                        println("[EvenHubEvent] No event data found. jsonData: ${event.jsonData}")
                    }
                }
            }
        }
    }
    
    /** 音频事件展示行最大条数 */
    private val maxAudioEventDisplayLines = 100
    
    /**
     * 将收到的音频事件转成展示文本并追加到 UI 状态。
     */
    private fun appendAudioEventDisplay(payload: AudioEventPayload) {
        val pcm = payload.audioPcm
        val len = pcm.size
        val previewBytes = 32
        val hexPreview = pcm.take(previewBytes).joinToString(" ") { b ->
            (b and 0xff).toString(16).padStart(2, '0').uppercase()
        }
        val line = if (len <= previewBytes) {
            "PCM 长度: $len，hex: $hexPreview"
        } else {
            "PCM 长度: $len，前 $previewBytes 字节 hex: $hexPreview …"
        }
        uiState = uiState.copy(
            audioEventDisplayLines = (uiState.audioEventDisplayLines + line).takeLast(maxAudioEventDisplayLines)
        )
    }
    
    /**
     * 处理列表项事件
     */
    private fun handleListItemEvent(event: ListItemEvent) {
        // 如果接收到数据带有 currentSelectItemIndex，更新章节内容
        val itemIndex = event.currentSelectItemIndex
        if (itemIndex != null && itemIndex >= 0) {
            // 通过当前阅读的书本 ID 获取书本
            val book = uiState.books.find { it.id == currentReadingBookId }
            if (book != null && itemIndex < book.chapters.size) {
                // 获取对应章节
                val chapter = book.chapters[itemIndex]
                // 更新当前章节索引
                currentChapterIndex = itemIndex
                // 在协程作用域中调用 updateChapterInfo 更新章节内容
                coroutineScope.launch {
                    updateChapterInfo(chapter)
                }
            } else {
                println("[ListItemEvent] Book not found or invalid chapter index: bookId=$currentReadingBookId, index=$itemIndex")
            }
        }
    }
    
    /**
     * 处理文本项事件
     */
    private fun handleTextItemEvent(event: TextItemEvent) {
        // 只在全屏阅读模式下处理滚动事件
        if (!uiState.isFullScreenReading) {
            println("[TextEvent] Scroll event: Not in full screen reading mode")
            return
        }
        
        when (event.eventType) {
            OsEventTypeList.SCROLL_BOTTOM_EVENT -> {
                println("[TextEvent] Scroll bottom event: Total fragments: ${readingFragments.size}, Current index: ${readingFragmentIndex}")
                // 向下滚动，显示下一个片段
                readingFragmentIndex+=1
                if (readingFragmentIndex > readingFragments.size) {
                    readingFragmentIndex = readingFragments.size - 1
                    println("[TextEvent] Scroll bottom event: Reached end of fragments")
                    return
                }
                coroutineScope.launch {
                    updateReadingFragment(readingFragmentIndex)
                }
            }
            OsEventTypeList.SCROLL_TOP_EVENT -> {
                println("[TextEvent] Scroll top event: Total fragments: ${readingFragments.size}, Current index: ${readingFragmentIndex}")
                // 向上滚动，显示上一个片段
                readingFragmentIndex-=1
                if (readingFragmentIndex < 0) {
                    readingFragmentIndex = 0
                    println("[TextEvent] Scroll top event: Reached start of fragments")
                    return
                }
                coroutineScope.launch {
                    updateReadingFragment(readingFragmentIndex)
                }
            }
            else -> {
                // 其他事件类型不处理
            }
        }
    }
    
    /**
     * 处理系统事件
     */
    private fun handleSysItemEvent(event: SysItemEvent) {
        // 处理系统级事件，比如进入前台、退出前台等
        when (event.eventType) {
            OsEventTypeList.DOUBLE_CLICK_EVENT -> {
                if (!uiState.isFullScreenReading) {
                    // 如果不在大屏阅读，进入全屏阅读
                    val book = uiState.books.find { it.id == currentReadingBookId }
                    if (book != null && currentChapterIndex < book.chapters.size) {
                        val chapter = book.chapters[currentChapterIndex]
                        // 在协程作用域中调用 fullScreenReading 并设置大屏状态为 true
                        coroutineScope.launch {
                            fullScreenReading(chapter)
                            uiState = uiState.copy(isFullScreenReading = true)
                        }
                    } else {
                        println("[SysEvent] Book not found or invalid chapter index: bookId=$currentReadingBookId, index=$currentChapterIndex")
                    }
                } else {
                    // 如果已经在大屏阅读，再次双击则退出大屏，重建阅读页面
                    val book = uiState.books.find { it.id == currentReadingBookId }
                    if (book != null) {
                        // 在协程作用域中调用 startReadingBook，isRebuild 为 true，并设置大屏状态为 false
                        coroutineScope.launch {
                            startReadingBook(book, isRebuild = true)
                            uiState = uiState.copy(isFullScreenReading = false)
                        }
                    } else {
                        println("[SysEvent] Book not found: bookId=$currentReadingBookId")
                    }
                }
            }
            OsEventTypeList.FOREGROUND_ENTER_EVENT -> {
                println("[SysEvent] App entered foreground")
            }
            OsEventTypeList.FOREGROUND_EXIT_EVENT -> {
                println("[SysEvent] App exited foreground")
            }
            OsEventTypeList.ABNORMAL_EXIT_EVENT -> {
                println("[SysEvent] App abnormal exit")
            }
            else -> {
                // 其他系统事件
            }
        }
    }
    
    /**
     * 清理资源，取消所有监听
     */
    fun dispose() {
        unsubscribeDeviceStatus?.invoke()
        unsubscribeDeviceStatus = null
        unsubscribeEvenHubEvent?.invoke()
        unsubscribeEvenHubEvent = null
    }

    /**
     * 监听到设备状态更新时刷新 UI。
     */
    fun updateDeviceStatus(status: DeviceStatus?) {
        if (status == null) return
        val deviceSn = uiState.deviceInfo?.sn ?: return
        if (status.sn != deviceSn) return
        uiState = uiState.copy(deviceStatus = status)
    }

    /**
     * 创建图书视图窗口
     */
    suspend fun startReadingBook(book: BookModel, isRebuild: Boolean = false) {
        // 更新当前阅读的书本 ID
        currentReadingBookId = book.id
        // 如果不是重建，重置章节索引为 0；如果是重建，保持当前章节索引
        if (!isRebuild) {
            currentChapterIndex = 0
        }
        //  1、创建页面属性：
        //  - 1.1、图书说明
        val bookInfo = TextContainerProperty(
            containerID = 2,
            containerName = "info",
            xPosition = 0,
            yPosition = 0,
            width = 530,
            height = 30,
            borderWidth = 1,
            borderColor = 13,
            borderRdaius = 6,
            paddingLength = 0,
            content = "《${book.title}》--作者:${book.author}",
        )
        //  - 1.2、章节列表
        val bookChapters = listOf(
            ListContainerProperty(
                containerID = 1,
                containerName = "chapters",
                xPosition = 0,
                yPosition = 35,
                width = 110,
                height = 200,
                borderWidth = 1,
                borderColor = 13,
                borderRdaius = 6,
                paddingLength = 5,
                isEventCapture = 1,
                itemContainer = ListItemContainerProperty(
                    itemCount = book.totalChapters,
                    itemWidth = 100,
                    isItemSelectBorderEn = 1,
                    itemName = book.chapters.map { "第${it.index}章" }
                )
            )
        )
        //  - 1.3、章节概要（使用当前章节索引）
        val currentChapter = if (currentChapterIndex < book.chapters.size) {
            book.chapters[currentChapterIndex]
        } else {
            book.chapters[0] // 如果索引无效，使用第一章
        }
        val chapterInfo = TextContainerProperty(
            containerID = 3,
            containerName = "content",
            xPosition = 115,
            yPosition = 35,
            width = 415,
            height = 200,
            borderWidth = 1,
            borderColor = 13,
            borderRdaius = 6,
            paddingLength = 12,
            content = "${currentChapter.title}\n\n${currentChapter.displayContent}\n\n双击全屏阅读>>",
        )
        runCatching {
            if (isRebuild) {
                rebuildPageContainer(RebuildPageContainer(
                    containerTotalNum = 3,
                    listObject = bookChapters,
                    textObject = listOf(
                        bookInfo,
                        chapterInfo
                    )
                ))
            } else {
                createStartUpPageContainer(CreateStartUpPageContainer(
                    containerTotalNum = 3,
                    listObject = bookChapters,
                    textObject = listOf(
                        bookInfo,
                        chapterInfo
                    )
                ))
            }
        }.onFailure { error ->
            uiState = uiState.copy(errorMessage = "Failed to create book view: ${error.message}")
        }
    }

    /**
     * 更新章节概要
     */
    suspend fun updateChapterInfo(chapter: BookChapterModel) {
        //  1、初始化更新的对象
        val update = TextContainerUpgrade(
            containerID = 3,
            containerName = "content",
            content = "${chapter.title}\n\n${chapter.displayContent}\n\n双击全屏阅读>>"
        )
        runCatching {
            textContainerUpgrade(update)
        }.onFailure { error ->
            uiState = uiState.copy(errorMessage = "Failed to update chapter view: ${error.message}")
        }
    }

    /**
     * 全屏阅读
     */
    suspend fun fullScreenReading(chapter: BookChapterModel) {
        // 将章节内容按字数拆分成片段，每个片段最大200字
        readingFragments = splitContentBySize(chapter.content, 200)
        readingFragmentIndex = 0
        
        // 显示第一个片段
        val currentFragment = if (readingFragments.isNotEmpty()) {
            readingFragments[readingFragmentIndex]
        } else {
            chapter.content
        }
        
        val container = RebuildPageContainer(
            containerTotalNum = 1,
            textObject = listOf(
                TextContainerProperty(
                    containerID = 4,
                    containerName = "chapter",
                    content = currentFragment,
                    xPosition = 0,
                    yPosition = 0,
                    width = 500,
                    height = 235,
                    borderWidth = 1,
                    borderColor = 13,
                    borderRdaius = 6,
                    paddingLength = 12,
                    isEventCapture = 1,
                )
            ),
        )
        runCatching {
            rebuildPageContainer(container)
        }.onFailure { error ->
            uiState = uiState.copy(errorMessage = "Failed to rebuild page view: ${error.message}")
        }
    }
    
    /**
     * 更新阅读片段
     */
    suspend fun updateReadingFragment(index: Int) {
        // 确保索引在有效范围内
        if (index < 0 || index >= readingFragments.size) {
            println("[ReadingFragment] Invalid index: $index, fragments size: ${readingFragments.size}")
            return
        }
        println("[ReadingFragment] Updating fragment: ${index}, Total fragments: ${readingFragments.size}")
        val fragment = readingFragments[index]
        val update = TextContainerUpgrade(
            containerID = 4,
            containerName = "chapter",
            content = fragment
        )
        runCatching {
            textContainerUpgrade(update)
        }.onFailure { error ->
            uiState = uiState.copy(errorMessage = "Failed to update reading fragment: ${error.message}")
        }
    }
    
    /**
     * 将内容按指定字数拆分成片段
     * @param content 要拆分的内容
     * @param maxChars 每个片段的最大字数（默认 200 字）
     * @return 拆分后的片段列表
     */
    private fun splitContentBySize(content: String, maxChars: Int = 200): List<String> {
        if (content.isEmpty()) return emptyList()
        
        val fragments = mutableListOf<String>()
        var currentFragment = StringBuilder()
        var currentCharCount = 0
        
        for (char in content) {
            // 如果添加当前字符会超过限制，保存当前片段并开始新片段
            if (currentCharCount >= maxChars && currentFragment.isNotEmpty()) {
                fragments.add(currentFragment.toString())
                currentFragment = StringBuilder()
                currentCharCount = 0
            }
            
            // 添加当前字符
            currentFragment.append(char)
            currentCharCount++
        }
        
        // 添加最后一个片段（如果有剩余内容）
        if (currentFragment.isNotEmpty()) {
            fragments.add(currentFragment.toString())
        }
        
        return fragments
    }

    

    /**
     * 退出阅读
     */
    suspend fun exitReading() {
        runCatching {
            shutDownPageContainer(ShutDownContainer(exitMode = 0))
        }.onFailure { error ->
            uiState = uiState.copy(errorMessage = "Failed to close book view: ${error.message}")
        }
    }
}

/**
 * 从 JSON 文件加载图书数据。
 */
private suspend fun loadBooksFromJson(): List<BookModel> {
    val json = JsInteropUtils.fetchJson("books.json") ?: return emptyList()
    
    // 检查是否是数组
    if (JsInteropUtils.getType(json) != "object" || !JsInteropUtils.isArray(json)) return emptyList()
    
    // 使用 JsInteropUtils 访问数组长度和元素
    val length = JsInteropUtils.getArrayLength(json)
    val result = mutableListOf<BookModel>()
    
    for (i in 0 until length) {
        val bookJson = JsInteropUtils.getArrayElement(json, i) ?: continue
        val id = JsInteropUtils.getStringProperty(bookJson, "id") ?: continue
        val title = JsInteropUtils.getStringProperty(bookJson, "title") ?: continue
        val author = JsInteropUtils.getStringProperty(bookJson, "author") ?: continue
        val type = JsInteropUtils.getStringProperty(bookJson, "type") ?: continue
        val readChapters = JsInteropUtils.getIntProperty(bookJson, "readChapters") ?: 0
        
        // 解析章节列表（如果有）
        val chaptersJson = JsInteropUtils.getProperty(bookJson, "chapters")
        val chapters = if (chaptersJson != null && JsInteropUtils.isArray(chaptersJson)) {
            val chaptersLength = JsInteropUtils.getArrayLength(chaptersJson)
            val chaptersList = mutableListOf<BookChapterModel>()
            
            for (j in 0 until chaptersLength) {
                val chapterJson = JsInteropUtils.getArrayElement(chaptersJson, j) ?: continue
                val chapterIndex = JsInteropUtils.getIntProperty(chapterJson, "index") ?: continue
                val chapterTitle = JsInteropUtils.getStringProperty(chapterJson, "title") ?: continue
                val chapterContent = JsInteropUtils.getStringProperty(chapterJson, "content") ?: ""
                val hadRead = JsInteropUtils.getBooleanProperty(chapterJson, "hadRead") ?: false
                
                chaptersList.add(
                    BookChapterModel(
                        bookId = id,
                        index = chapterIndex,
                        title = chapterTitle,
                        content = chapterContent
                    ).apply {
                        this.hadRead = hadRead
                    }
                )
            }
            chaptersList
        } else {
            emptyList()
        }
        
        result.add(
            BookModel(
                id = id,
                title = title,
                author = author,
                type = type,
                chapters = chapters
            ).apply {
                this.readChapters = readChapters
            }
        )
    }
    
    return result
}

/**
 * 书架默认占位数据（当 JSON 加载失败时使用）。
 */
private fun defaultBooks(): List<BookModel> {
    return listOf(
        BookModel(
            id = "book_001",
            title = "其武修魔传",
            author = "王子琦",
            type = "修仙"
        ),
        BookModel(
            id = "book_002",
            title = "山海奇闻录",
            author = "南烟",
            type = "奇幻"
        ),
        BookModel(
            id = "book_003",
            title = "夜行者笔记",
            author = "陆离",
            type = "悬疑"
        ),
    ).onEach {
        it.readChapters = (0..it.totalChapters).random()
    }
}
