# InNovel — Even Hub Web Demo

A **Kotlin Multiplatform** Even Hub Web demo using **Compose Multiplatform** for the UI, targeting **Kotlin/JS** and **Kotlin/Wasm**. It is a **simple novel reader**: bookshelf, chapter list, and reading view, plus Even Hub integration for device and page containers.

## Features

- **User & device**: User info, device status (glasses, ring, etc.)
- **Bookshelf & reading**: Book list, chapter selection, reading dialog
- **Image test**: Custom-size canvas, 3D preview, screenshot upload to Even Hub container
- **Even Hub**: Create/rebuild page containers, update image raw data, text container upgrade, shut down page container

## SDK

- **Even Hub SDK**: npm package [`@evenrealities/even_hub_sdk`](https://www.npmjs.com/package/@evenrealities/even_hub_sdk) (version `^0.0.6`)
- Kotlin/JS and Kotlin/Wasm expect/actual bridge; shared `webMain` logic

Bridge and helpers:

| Description      | Path |
|------------------|------|
| Bridge API       | `composeApp/src/webMain/kotlin/.../sdk/EvenHubBridge.kt` |
| JS/Wasm impl     | `composeApp/src/jsMain/.../sdk/EvenHubBridge.js.kt`, `.../wasmJsMain/.../EvenHubBridge.wasmJs.kt` |
| Types & parsing  | `EvenHubTypes.kt`, `EvenHubJsParsers.kt` |
| JS interop       | `EvenHubJsInterop.kt`, `JsInteropUtils.kt` |

**Usage**: Call `ensureEvenAppBridge()` first; then use **Basic API** for user/device, and **Even Hub API** for page containers and events.

---

## Basic API

Bridge setup and user/device access (in `EvenHubBridge.kt`). Call these before or alongside Even Hub features.

| API | Description |
|-----|-------------|
| `ensureEvenAppBridge()` | Initialize and wait for Even App bridge. **Must be called once before any other SDK usage.** |
| `getUserInfo(): UserInfo?` | Get current user info |
| `getDeviceInfo(): DeviceInfo?` | Get device info (glasses, ring, etc.) |
| `observeDeviceStatus(onChange): () -> Unit` | Subscribe to device status; returns unsubscribe |

---

## Even Hub API

Page containers and event subscription (aligned with host BleG2CmdProtoEvenHubExt). **Use these for Even Hub–specific features; do not call low-level generic methods.**

### Event

| API | Description |
|-----|-------------|
| `observeEvenHubEvent(onChange): () -> Unit` | Subscribe to EvenHub events (list/text/system); returns unsubscribe |

### Page containers (PB)

| API | Description |
|-----|-------------|
| `createStartUpPageContainer(container): StartUpPageCreateResult` | Create startup page container (text/list/image); aligned with SDK `StartUpPageCreateResult` |
| `rebuildPageContainer(container): Boolean` | Rebuild page container |
| `updateImageRawData(data): ImageRawDataUpdateResult` | Update image raw data (e.g. `imageData` as number[]); use `isSuccess` extension |
| `textContainerUpgrade(container): Boolean` | Upgrade text container content |
| `shutDownPageContainer(container): Boolean` | Shut down page container (exit Even Hub page) |

### Data types (EvenHubTypes.kt)

- **CreateStartUpPageContainer**: `containerTotalNum`, `listObject`, `textObject`, `imageObject`
- **RebuildPageContainer**: same fields, for rebuild
- **ImageRawDataUpdate**: `containerID`, `containerName`, `imageData` (prefer number[])
- **TextContainerUpgrade**: `containerID`, `containerName`, `content`, etc.
- **ShutDownContainer**: `exitMode` (e.g. 0)

Construct these data classes and pass them to the **Even Hub API**; serialization is in `EvenHubJsParsers.kt`.

## Requirements

- **JDK**: 24 (see `jvmToolchain(24)` in `composeApp/build.gradle.kts`)
- **Node.js**: for Webpack and npm (Gradle uses it automatically)

## Install & run

### 1. Clone and enter project

```bash
git clone <repository-url>
cd InNovel
```

### 2. Run dev server

Default port: **2000**.

**Wasm** (recommended, modern browsers):

- macOS / Linux:
  ```bash
  ./gradlew :composeApp:wasmJsBrowserDevelopmentRun
  ```
- Windows:
  ```bash
  .\gradlew.bat :composeApp:wasmJsBrowserDevelopmentRun
  ```

**JS** (older browsers):

- macOS / Linux:
  ```bash
  ./gradlew :composeApp:jsBrowserDevelopmentRun
  ```
- Windows:
  ```bash
  .\gradlew.bat :composeApp:jsBrowserDevelopmentRun
  ```

### 3. Open in browser

- **http://localhost:2000**

(Change `devServer.port` in `composeApp/build.gradle.kts` if the port is in use.)

### 4. Build only (no server)

- Wasm: `./gradlew :composeApp:wasmJsBrowserDevelopmentWebpack`
- JS: `./gradlew :composeApp:jsBrowserDevelopmentWebpack`

## Project layout

- **`composeApp/src/commonMain`**: Shared Compose theme, etc.
- **`composeApp/src/webMain`**: Web shared logic and UI (bookshelf, reading, image test, SDK bridge and types)
- **`composeApp/src/jsMain`**: Kotlin/JS platform impl (Bridge, Interop)
- **`composeApp/src/wasmJsMain`**: Kotlin/Wasm platform impl (Bridge, Interop)

## Links

- [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
- [Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform)
- [Kotlin/Wasm](https://kotl.in/wasm/)

Feedback: [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web). Issues: [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).

**[中文文档](README.zh-CN.md)**
