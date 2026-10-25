# 03: 统一打印内核 + 布票标签模板

**What to build:** 车间管理员在织造单工作台勾选多张布票一次打印不干胶标签（100×60mm），贴在每匹布上，工人用手机扫码直接进报工占位页。交付前端统一打印内核（从现有 `PrintShipmentDialog.vue` 的隐藏 iframe + `contentWindow.print()` 提取为公共 composable，不含模板组织约定与二维码的孤立现状一并解决）：`PrintDispatcher.vue`（按 bizType+templateCode 分发）、`PrintPaper.vue`（纸张/边距/打印按钮外壳）、`QrSlot.vue`（二维码槽位，包装现有 Qrcode 组件）、`usePrintHtml.ts` composable、`registry.ts` 静态注册表（不做自动发现、不做模板设计器）；新建 `WEAVING_TICKET_LABEL` 模板（不干胶 100×60mm `@page { size: 100mm 60mm }`，内容：布票 code 二维码 + 布票号 + 织造单号 + 机台任务号 + 品名/颜色/匹序/数量）；后端 weaving-ticket print-data 接口（独立 VO，豁免脱敏，含拼好的 qrUrl）+ `@LogRecord` 留痕入 `system_operate_log`；infra_config 种子 `luna.h5.base-url`（缺失时打印接口报错提示管理员补配置，不允许打出空域名二维码）；织造单工作台机台任务页提供"打印布票（批量勾选）"入口与 `lunaerp:production:weaving-ticket:print` 权限码。二维码编码完整 URL：`{luna.h5.base-url}/h5/scan/weaving-ticket?code={布票code}`，布票 code 用现有 `WeavingTicketDO.code`（租户级唯一）。

**Blocked by:** 01

**Status:** closed

- [x] 前端打印内核 5 件套落在 `src/components/Print/`：PrintDispatcher / PrintPaper / QrSlot / usePrintHtml composable / registry.ts（静态数组注册表，模板组件 + fetchApi 取数）
- [x] `WEAVING_TICKET_LABEL` 模板组件落在织造业务就近目录，100×60mm 固定版式，含二维码 + 布票号 + 织造单号 + 机台任务号 + 品名/颜色/匹序/数量
- [x] 后端 `GET /lunaerp/production/weaving-ticket/{id}/print-data`（权限 `lunaerp:production:weaving-ticket:print`）返回打印 VO（含拼好的 qrUrl，豁免脱敏），方法上加 `@LogRecord(type="打印", success="打印了布票标签 {{#no}}")`
- [x] infra_config 种子 `luna.h5.base-url` 默认空，打印接口强校验：缺失报错提示管理员补配置，不允许打出空域名二维码
- [x] 二维码编码完整 URL `{luna.h5.base-url}/h5/scan/weaving-ticket?code={布票code}`（不加签名，鉴权交 H5 登录态）；布票 code 用 `WeavingTicketDO.code`
- [x] 织造单工作台机台任务页"打印布票（批量勾选）"入口
- [x] 测试：批量打印、二维码码格式冻结、扫码跳转 W4 H5 报工占位页（15 号接页面，本 ticket 落占位）、打印 `@LogRecord` 入操作日志
