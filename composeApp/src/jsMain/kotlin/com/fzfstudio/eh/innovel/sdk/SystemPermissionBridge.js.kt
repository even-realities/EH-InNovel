package com.fzfstudio.eh.innovel.sdk

import kotlinx.coroutines.await
import kotlin.js.Promise

actual suspend fun launchSystemCapability(capability: SystemCapability): SystemCapabilityResult {
    val result = launchSystemCapabilityImpl(capability.name).await()
    val status = jsToStringOrNull(jsGet(result, "status"))
    val detail = jsToStringOrNull(jsGet(result, "detail")).orEmpty()
    return SystemCapabilityResult(
        capability = capability,
        status = parseSystemCapabilityStatus(status),
        detail = detail,
    )
}

@Suppress("UNCHECKED_CAST_TO_EXTERNAL_INTERFACE")
private fun launchSystemCapabilityImpl(capabilityName: String): Promise<kotlin.js.JsAny?> {
    val requester = js(
        """
        function(capabilityName) {
          function ok(detail) { return { status: "success", detail: detail }; }
          function cancelled(detail) { return { status: "cancelled", detail: detail }; }
          function unsupported(detail) { return { status: "unsupported", detail: detail }; }
          function fail(detail) { return { status: "error", detail: detail }; }
          function isSecure() {
            return typeof window !== "undefined" && (window.isSecureContext || window.location.protocol === "https:" || window.location.hostname === "localhost");
          }
          const pickFile = function(options) { return new Promise(function(resolve) {
            let settled = false;
            const accept = options && options.accept ? options.accept : "*/*";
            const capture = options ? options.capture : undefined;
            const multiple = options && options.multiple ? true : false;
            const input = document.createElement("input");
            function finish(result) {
              if (settled) return;
              settled = true;
              if (typeof window !== "undefined") {
                window.removeEventListener("focus", handleFocus);
              }
              input.onchange = null;
              input.remove();
              resolve(result);
            }
            function handleFocus() {
              setTimeout(function() {
                const fileCount = input.files ? input.files.length : 0;
                if (!settled && fileCount === 0) {
                  finish(cancelled("未选择文件"));
                }
              }, 300);
            }
            input.type = "file";
            input.accept = accept;
            input.multiple = multiple;
            if (capture) input.setAttribute("capture", capture);
            input.style.position = "fixed";
            input.style.left = "-9999px";
            input.style.top = "-9999px";
            input.onchange = function() {
              const fileCount = input.files ? input.files.length : 0;
              const file = fileCount > 0 ? input.files[0] : null;
              if (file) {
                finish(ok("已选择 " + fileCount + " 个文件，首个文件: " + file.name));
              } else {
                finish(cancelled("未选择文件"));
              }
            };
            document.body.appendChild(input);
            if (typeof window !== "undefined") {
              window.addEventListener("focus", handleFocus, { once: true });
            }
            input.click();
          }); };
          try {
            if (typeof navigator === "undefined") {
              return Promise.resolve(unsupported("navigator 不可用"));
            }
            switch (capabilityName) {
              case "TakePhoto": {
                if (typeof document !== "undefined") {
                  return pickFile({ accept: "image/*", capture: "environment" });
                }
                return Promise.resolve(unsupported("当前环境不支持系统相机拉起"));
              }
              case "PickImage": {
                if (typeof document === "undefined") {
                  return Promise.resolve(unsupported("当前环境不支持文件选择器"));
                }
                return pickFile({ accept: "image/*" });
              }
              case "RecordVideo": {
                if (typeof document === "undefined") {
                  return Promise.resolve(unsupported("当前环境不支持系统录像拉起"));
                }
                return pickFile({ accept: "video/*", capture: "environment" });
              }
              case "RecordAudio": {
                if (typeof document === "undefined") {
                  return Promise.resolve(unsupported("当前环境不支持系统录音拉起"));
                }
                return pickFile({ accept: "audio/*", capture: "user" });
              }
              case "PickFile": {
                if (typeof document === "undefined") {
                  return Promise.resolve(unsupported("当前环境不支持文件选择器"));
                }
                return pickFile({ accept: "*/*" });
              }
              case "GetLocation": {
                if (!navigator.geolocation) {
                  return Promise.resolve(unsupported("当前环境不支持 geolocation"));
                }
                if (!isSecure()) {
                  return Promise.resolve(unsupported("定位需要 HTTPS 或 localhost 环境"));
                }
                return new Promise((resolve) => {
                  navigator.geolocation.getCurrentPosition(
                    () => resolve(ok("定位调用成功")),
                    (error) => resolve(cancelled(error && error.message ? error.message : "定位调用被拒绝或失败"))
                  );
                });
              }
              default:
                return Promise.resolve(unsupported("未识别的能力类型: " + capabilityName));
            }
          } catch (error) {
            const message = error && error.message ? error.message : String(error);
            if (message && (message.includes("denied") || message.includes("Permission denied") || message.includes("NotAllowedError") || message.includes("cancelled") || message.includes("AbortError") || message.includes("NotFoundError"))) {
              return Promise.resolve(cancelled(message));
            }
            return Promise.resolve(fail(message || "未知错误"));
          }
        }
        """
    )
    return requester(capabilityName) as Promise<kotlin.js.JsAny?>
}

private fun parseSystemCapabilityStatus(value: String?): SystemCapabilityStatus = when (value) {
    "success" -> SystemCapabilityStatus.Success
    "cancelled" -> SystemCapabilityStatus.Cancelled
    "unsupported" -> SystemCapabilityStatus.Unsupported
    else -> SystemCapabilityStatus.Error
}
