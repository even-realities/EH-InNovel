/*
 * Webpack configuration for Kotlin/JS + Kotlin/Wasm webpack bundles.
 *
 * 背景：dev/localsdk 分支优先使用本地 Even Hub SDK，否则回退到 node_modules 中的 npm 包。
 * 本地 SDK 路径：/Users/whiskee/Workspace/EvenGlass/App_New/plugins/even_hub_sdk（含 dist 构建产物）
 */

const path = require("path");
const fs = require("fs");

config.resolve = config.resolve || {};
config.resolve.alias = config.resolve.alias || {};

// 本地 Even Hub SDK 路径（dev/localsdk）
const LOCAL_SDK_PATH = "/Users/whiskee/Workspace/EvenGlass/App_New/plugins/even_hub_sdk";

// 查找项目根目录（包含 node_modules 的目录）
function findProjectRoot(startPath) {
    let currentPath = startPath;
    let depth = 0;
    const maxDepth = 10;
    while (currentPath !== path.dirname(currentPath) && depth < maxDepth) {
        const nodeModulesPath = path.join(currentPath, "node_modules");
        if (fs.existsSync(nodeModulesPath)) {
            return currentPath;
        }
        currentPath = path.dirname(currentPath);
        depth++;
    }
    const fallbackRoot = path.resolve(__dirname, "../../../../../../..");
    if (fs.existsSync(path.join(fallbackRoot, "node_modules"))) {
        return fallbackRoot;
    }
    return path.resolve(__dirname, "../../../..");
}

const projectRoot = findProjectRoot(__dirname);
const rootNodeModules = path.join(projectRoot, "node_modules");
const npmSdkPath = path.join(rootNodeModules, "@evenrealities", "even_hub_sdk");

// 优先使用本地 SDK，若不存在则使用 node_modules 中的包
const sdkPath = fs.existsSync(LOCAL_SDK_PATH) ? LOCAL_SDK_PATH : npmSdkPath;

if (fs.existsSync(sdkPath)) {
    config.resolve.alias["@evenrealities/even_hub_sdk"] = sdkPath;
    if (sdkPath === LOCAL_SDK_PATH) {
        console.log("[resolve-even-hub-sdk] Using local Even Hub SDK:", LOCAL_SDK_PATH);
    }
    config.resolve.modules = config.resolve.modules || ["node_modules"];
    if (!config.resolve.modules.includes(rootNodeModules)) {
        config.resolve.modules.push(rootNodeModules);
    }
} else {
    console.warn(`Warning: Could not find @evenrealities/even_hub_sdk at ${sdkPath}`);
    console.warn(`Checked: local ${LOCAL_SDK_PATH}, npm ${npmSdkPath}`);
}