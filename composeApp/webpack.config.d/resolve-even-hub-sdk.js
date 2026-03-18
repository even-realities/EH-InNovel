/*
 * Webpack configuration for Kotlin/JS + Kotlin/Wasm webpack bundles.
 *
 * 背景：dev/localsdk 分支只使用本地 Even Hub SDK，不回退到 node_modules 中的 npm 包。
 * 本地 SDK 路径：/Users/whiskee/Workspace/EvenHub/even_hub_ts_sdk（含 dist 构建产物）
 */

const fs = require("fs");

config.resolve = config.resolve || {};
config.resolve.alias = config.resolve.alias || {};

// 本地 Even Hub SDK 路径（dev/localsdk）
const LOCAL_SDK_PATH = "/Users/whiskee/Workspace/EvenHub/even_hub_ts_sdk";

if (fs.existsSync(LOCAL_SDK_PATH)) {
    config.resolve.alias["@evenrealities/even_hub_sdk"] = LOCAL_SDK_PATH;
    console.log("[resolve-even-hub-sdk] Using local Even Hub SDK:", LOCAL_SDK_PATH);
} else {
    console.warn(`Warning: Local @evenrealities/even_hub_sdk not found at ${LOCAL_SDK_PATH}`);
}
