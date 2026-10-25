# 07: 通用扫码组件（html5-qrcode + 键盘楔入 + HTTPS 检测）

**What to build:** 落成 H5 全场景复用的扫码组件 `src/views/h5/components/Scanner.vue`。每个扫码场景提供两条输入路径：① 摄像头组件（html5-qrcode，布票二维码；成品卷支持一维 Code128 切换），失败/无权限/非安全上下文显示降级提示；② 常驻自动聚焦输入框 + 软回车按钮，PDA 头扫与 USB 扫码枪以键盘楔入回车触发（HTTP 局域网环境的唯一保证路径）。部署兼容：`isSecureContext=false` 或 `http://192.168.*` 下自动隐藏摄像头入口、只留楔入输入，不报错卡死；HTTPS 或 localhost 下摄像头路径可用。企微/钉钉/手机 Safari/Chrome 均使用系统 WebView 标准能力，无 JS-SDK 依赖。可 demo：HTTPS 下摄像头扫码成功 / HTTP 下楔入输入触发回车。

**Blocked by:** 06（H5Layout 已就位，组件挂在 H5 子树）

**Status:** closed

- [x] `src/views/h5/components/Scanner.vue`（html5-qrcode 摄像头二维码 + Code128 一维码切换）
- [x] 常驻自动聚焦 input + 软回车按钮（PDA 头扫/USB 扫码枪键盘楔入回车触发）
- [x] HTTPS 检测：`isSecureContext=false` 或 `http://192.168.*` 下自动隐藏摄像头入口只留楔入输入，不报错卡死
- [x] HTTPS 或 localhost 下摄像头路径可用，无权限/失败显示降级提示
- [x] 企微/钉钉/手机 Safari/Chrome 均使用系统 WebView 标准能力，无 JS-SDK 依赖
- [x] 摄像头必须 HTTPS（内网域名/自签证书）或 localhost 的部署说明写进 spec 与上线手册
- [x] demo：HTTPS 下摄像头扫码成功 / HTTP 下楔入输入触发回车
