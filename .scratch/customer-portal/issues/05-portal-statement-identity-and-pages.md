# 05: 门户对账单确认（10 号契约身份来源替换 + 门户对账页）

**What to build:** 客户在门户查看对账单（Tab：待确认/已确认/已驳回）并确认（手填姓名必填）/驳回（必填原因）。后端把 10 号已实现的门户 statement 4 端点身份来源从显式 customerId 替换为 PortalContext（保留 customerId 参数路径做内部代客场景），完整路径映射补全为 `/app-api/lunaerp/portal/finance/statement/*`（契约语义零变更），隔离专测不放松。可 demo：客户在门户确认对账单→管理端看到 CONFIRMED 与 confirm_by_name。

**Blocked by:** 02（PortalTokenFilter 校验链 + 门户前端骨架）, customer-reconciliation #08（门户对账 4 端点 + 客户隔离专测——跨特性真实依赖，该票实现显式 customerId 身份来源后本票才能替换）

**Status:** resolved

- [x] 身份来源替换：statement 4 端点（list/{id}/confirm/reject）身份改由 PortalContext 提供，customerId 参数保留（内部代客场景）；完整路径映射 `/app-api/lunaerp/portal/finance/statement/*`，`/portal/finance/statement/` 模块内相对前缀不变（10 号 spec 门户注记处补映射说明，契约语义零变更）
- [x] list 仅 SENT/CONFIRMED；`{id}` 含全部行；confirm SENT→CONFIRMED 记 confirm_by_name/time；reject 必填 reject_reason，SENT→REJECTED；CONFIRMED 再操作被 10 号既有守卫拒绝
- [x] 门户确认与内部代客确认落同一状态字段与 confirm_type 字典（ONLINE_PORTAL/ON_BEHALF）
- [x] `@LogRecord` 确认/驳回两处留痕
- [x] 前端 `statement/list.vue`（Tab：待确认/已确认/已驳回）+ `statement/detail.vue`（含全部行）；确认前手填姓名必填、驳回必填原因弹窗；`/portal/statement?no=` 兼容形态（10 号冻结契约不回改）、无会话直达 → invalid 引导
- [x] 隔离专测复核（不放松）：A 客户 token 访问/操作 B 客户对账单（{id}/confirm/reject 全入口）→ 038-001；跨租户 token → 038-001；PortalContext 与显式 customerId 两路径隔离专测均绿

## Answer

### 实现概要
1. **后端 API & 安全（身份来源替换与 4 端点实现）**：
   - 在 [`PortalStatementController`](file:///D:/work/luna/20261011/luna/luna-srv/lunasrv-module-lunaerp/src/main/java/com/luna/lunasrv/module/lunaerp/portal/controller/portal/statement/PortalStatementController.java) 暴露完整映射路径 `/app-api/lunaerp/portal/finance/statement/*`（包括 `list`、`{id}`、`confirm`、`reject`）。
   - 服务层 [`PortalStatementServiceImpl`](file:///D:/work/luna/20261011/luna/luna-srv/lunasrv-module-lunaerp/src/main/java/com/luna/lunasrv/module/lunaerp/service/portal/PortalStatementServiceImpl.java)：
     - 身份来源优先从 `PortalSecurityFrameworkUtils.getRequiredCustomerId()` 获取，同时保留 `customerId` 入参供内部代客场景使用。
     - 列表端点仅对门户暴露 `SENT` 与 `CONFIRMED` 状态对账单。
     - 详情端点加载主表信息及全部明细行 (`CustomerStatementItemDO`)，支持 `id` 与 `no` 双重寻址兼容。
     - 确认操作限制必须在 `SENT` 状态，确认人 `confirmedByName` 必填，记录服务器时间，状态更新为 `CONFIRMED`，`confirmType` 区分 `ONLINE_PORTAL` 与 `ON_BEHALF`；驳回操作限制状态 `SENT`，`rejectReason` 必填，更新为 `REJECTED`。
     - 已 `CONFIRMED` 的单据二次确认或驳回被既有状态守卫拒绝；使用 `@LogRecord` 实现确认与驳回审计留痕。
     - 越权与隔离校验：A 客户 token 访问 B 客户对账单抛出 `PORTAL_CUSTOMER_MISMATCH` (038-001)；跨租户访问抛出 `PORTAL_CUSTOMER_MISMATCH` (038-001)。
2. **前端门户对账单页面**：
   - [`api/portal/statement.ts`](file:///D:/work/luna/20261011/luna/luna-ui/src/api/portal/statement.ts)：封装对账单列表、详情、确认、驳回接口。
   - [`views/portal/statement/list.vue`](file:///D:/work/luna/20261011/luna/luna-ui/src/views/portal/statement/list.vue)：支持 Tab 切换（全部/待确认/已确认/已驳回）、对账单号搜索与分页。
   - [`views/portal/statement/detail.vue`](file:///D:/work/luna/20261011/luna/luna-ui/src/views/portal/statement/detail.vue)：支持 `id` 与 URL query `no` 兼容跳转；展示完整对账单明细行；弹窗手填确认人二次确认、弹窗必填原因驳回；无会话直达引导至 invalid 提示。
3. **测试覆盖**：
   - 后端测试：[`PortalStatementServiceImplTest`](file:///D:/work/luna/20261011/luna/luna-srv/lunasrv-module-lunaerp/src/test/java/com/luna/lunasrv/module/lunaerp/service/portal/PortalStatementServiceImplTest.java)（26 项）与 [`PortalStatementControllerTest`](file:///D:/work/luna/20261011/luna/luna-srv/lunasrv-module-lunaerp/src/test/java/com/luna/lunasrv/module/lunaerp/portal/controller/portal/statement/PortalStatementControllerTest.java)（4 项）共 30 项全部通过。
   - 前端测试：[`portal-statement.spec.ts`](file:///D:/work/luna/20261011/luna/luna-ui/src/views/portal/__tests__/portal-statement.spec.ts) 12 项测试全部通过。

### Commit Hash
- Ticket 分支提交：`183ab72`
- 合并提交：`c1de72c`

