# InNovel — Even Hub Web 演示

基于 **Kotlin Multiplatform** 的 Even Hub Web 演示应用，使用 **Compose Multiplatform** 构建界面，支持 **Kotlin/JS** 与 **Kotlin/Wasm** 两种 Web 目标。本应用是一个**简易小说阅读器**：调用Even Hub API在眼镜绘制容器展示选中书本基础信息，章节列表和全屏阅读。

## 功能简介

- **用户与设备**：展示用户信息、设备状态（眼镜/戒指等）
- **书架与阅读**：图书列表、章节选择、阅读对话框
- **图片测试**：自定义宽高画布、立体图预览、截图上传至 Even Hub 容器
- **Even Hub 能力**：创建/重建页面容器、更新图片原始数据、文本容器升级、退出页面容器

## 使用的 SDK

- **Even Hub SDK**：npm 包 [`@evenrealities/even_hub_sdk`](https://www.npmjs.com/package/@evenrealities/even_hub_sdk)（当前版本 `^0.0.6`）
- 通过 Kotlin/JS 与 Kotlin/Wasm 的 expect/actual 桥接调用 SDK，共享 `webMain` 业务逻辑

主要桥接与工具代码：

| 说明           | 路径 |
|----------------|------|
| Bridge API     | `composeApp/src/webMain/kotlin/.../sdk/EvenHubBridge.kt` |
| JS/Wasm 实现   | `composeApp/src/jsMain/.../sdk/EvenHubBridge.js.kt`、`.../wasmJsMain/.../EvenHubBridge.wasmJs.kt` |
| 类型与解析     | `EvenHubTypes.kt`、`EvenHubJsParsers.kt` |
| JS 互操作工具  | `EvenHubJsInterop.kt`、`JsInteropUtils.kt` |

**使用约定**：先调用 `ensureEvenAppBridge()` 等待桥接就绪；用户与设备使用**基础 API**，页面容器与事件使用 **Even Hub API**。

---

## 基础 API

桥接初始化及用户/设备能力（定义在 `EvenHubBridge.kt`）。在使用 Even Hub 能力前或同时调用。

| 接口 | 说明 |
|------|------|
| `ensureEvenAppBridge()` | 初始化并等待 Even App 桥接就绪。**使用其它 SDK 能力前必须先调用一次。** |
| `getUserInfo(): UserInfo?` | 获取当前用户信息 |
| `getDeviceInfo(): DeviceInfo?` | 获取设备信息（眼镜/戒指等） |
| `observeDeviceStatus(onChange): () -> Unit` | 监听设备状态变化，返回取消监听的函数 |

---

## Even Hub API

页面容器与事件订阅（对齐宿主 BleG2CmdProtoEvenHubExt）。**Even Hub 相关能力请直接使用下列接口，勿调用底层通用方法。**

### 事件

| 接口 | 说明 |
|------|------|
| `observeEvenHubEvent(onChange): () -> Unit` | 监听 EvenHub 事件（列表/文本/系统等），返回取消监听的函数 |

### 页面容器（PB 接口）

| 接口 | 说明 |
|------|------|
| `createStartUpPageContainer(container): StartUpPageCreateResult` | 创建启动页容器（文本/列表/图片），与 SDK `StartUpPageCreateResult` 对齐 |
| `rebuildPageContainer(container): Boolean` | 重建页面容器 |
| `updateImageRawData(data): ImageRawDataUpdateResult` | 更新图片原始数据（如 `imageData` 为 number[]）；用 `isSuccess` 判断是否成功 |
| `textContainerUpgrade(container): Boolean` | 升级文本容器内容 |
| `shutDownPageContainer(container): Boolean` | 关闭页面容器（退出 Even Hub 页面） |

### 主要数据类（EvenHubTypes.kt）

- **CreateStartUpPageContainer**：`containerTotalNum`、`listObject`、`textObject`、`imageObject`
- **RebuildPageContainer**：同上，用于重建
- **ImageRawDataUpdate**：`containerID`、`containerName`、`imageData`（建议 number[]）
- **TextContainerUpgrade**：`containerID`、`containerName`、`content` 等
- **ShutDownContainer**：`exitMode`（如 0）

在共享代码中构造上述数据类后传入 **Even Hub API** 即可；内部序列化见 `EvenHubJsParsers.kt`。

## 环境要求

- **JDK**：24（见 `composeApp/build.gradle.kts` 中的 `jvmToolchain(24)`）
- **Node.js**：用于 Webpack 与 npm 依赖（Gradle 会自动使用）

## 安装与运行

### 1. 克隆并进入项目

```bash
git clone <repository-url>
cd InNovel
```

### 2. 运行开发服务器

开发服务器默认端口：**2000**。

**Wasm 目标**（推荐，现代浏览器）：

- macOS / Linux：
  ```bash
  ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
  ```
- Windows：
  ```bash
  .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
  ```

**JS 目标**（兼容旧版浏览器）：

- macOS / Linux：
  ```bash
  ./gradlew :composeApp:jsBrowserDevelopmentRun
  ```
- Windows：
  ```bash
  .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
  ```

### 3. 在浏览器中打开

启动成功后，在浏览器中访问：

- **http://localhost:2000**

（若端口被占用，可在 `composeApp/build.gradle.kts` 的 `devServer.port` 中修改。）

### 4. 仅构建不运行

- Wasm：仅构建可执行 `./gradlew :composeApp:wasmJsBrowserDevelopmentWebpack`
- JS：对应任务为 `./gradlew :composeApp:jsBrowserDevelopmentWebpack`

## 项目结构（简要）

- **`composeApp/src/commonMain`**：Compose 主题等跨平台共享代码
- **`composeApp/src/webMain`**：Web 共享业务与 UI（书架、阅读、图片测试、SDK 桥接与类型）
- **`composeApp/src/jsMain`**：Kotlin/JS 平台实现（Bridge、Interop）
- **`composeApp/src/wasmJsMain`**：Kotlin/Wasm 平台实现（Bridge、Interop）

## 相关链接

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)
- [Kotlin/Wasm](https://kotl.in/wasm/)

Compose/Web 与 Kotlin/Wasm 反馈可至 [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web)；问题可提交 [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP)。

**[English](README.md)**
