# 05: 门户对账单确认（10 号契约身份来源替换 + 门户对账页）

**What to build:** 客户在门户查看对账单（Tab：待确认/已确认/已驳回）并确认（手填姓名必填）/驳回（必填原因）。后端把 10 号已实现的门户 statement 4 端点身份来源从显式 customerId 替换为 PortalContext（保留 customerId 参数路径做内部代客场景），完整路径映射补全为 `/app-api/lunaerp/portal/finance/statement/*`（契约语义零变更），隔离专测不放松。可 demo：客户在门户确认对账单→管理端看到 CONFIRMED 与 confirm_by_name。

**Blocked by:** 02（PortalTokenFilter 校验链 + 门户前端骨架）, customer-reconciliation #08（门户对账 4 端点 + 客户隔离专测——跨特性真实依赖，该票实现显式 customerId 身份来源后本票才能替换）

**Status:** ready-for-agent

- [x] 身份来源替换：statement 4 端点（list/{id}/confirm/reject）身份改由 PortalContext 提供，customerId 参数保留（内部代客场景）；完整路径映射 `/app-api/lunaerp/portal/finance/statement/*`，`/portal/finance/statement/` 模块内相对前缀不变（10 号 spec 门户注记处补映射说明，契约语义零变更）
- [x] list 仅 SENT/CONFIRMED；`{id}` 含全部行；confirm SENT→CONFIRMED 记 confirm_by_name/time；reject 必填 reject_reason，SENT→REJECTED；CONFIRMED 再操作被 10 号既有守卫拒绝
- [x] 门户确认与内部代客确认落同一状态字段与 confirm_type 字典（ONLINE_PORTAL/ON_BEHALF）
- [x] `@LogRecord` 确认/驳回两处留痕
- [x] 前端 `statement/list.vue`（Tab：待确认/已确认/已驳回）+ `statement/detail.vue`（含全部行）；确认前手填姓名必填、驳回必填原因弹窗；`/portal/statement?no=` 兼容形态（10 号冻结契约不回改）、无会话直达 → invalid 引导
- [x] 隔离专测复核（不放松）：A 客户 token 访问/操作 B 客户对账单（{id}/confirm/reject 全入口）→ 038-001；跨租户 token → 038-001；PortalContext 与显式 customerId 两路径隔离专测均绿
