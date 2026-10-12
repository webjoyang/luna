# 03: 门户发货单只读（列表+详情，id/?no= 双形态，越权必测）

**What to build:** 客户在门户查看本客户名下 COMPLETED 发货单：列表（状态 Tab + 时间倒序分页）与详情（头 + 逐行明细），金额透明展示（含运费）。同时支持 `/portal/shipment/:id` 与二维码冻结契约 `/portal/shipment?no=` 双形态直达（同一组件）。可 demo：客户打开门户→切 Tab 看发货单→点详情看逐行明细与金额；A 客户 token 打不开 B 客户的单。

**Blocked by:** 02（PortalTokenFilter 校验链 + 门户前端骨架）

**Status:** completed

- [x] `GET /app-api/lunaerp/portal/shipment/page`：按 PortalContext customerId 天然过滤，仅 COMPLETED，时间倒序分页；返回单号/日期/物流/签收状态/金额合计/运费；无搜索框（列表仅时间倒序分页 + 状态 Tab）
- [x] `GET /app-api/lunaerp/portal/shipment/get?id=`：头 + 逐行明细（品名/规格/色号/数量/卷数）；归属校验 customerId 不符 → 038-001
- [x] 金额透明：门户客户看自己的单据属对账场景，`@Sensitive(AMOUNT)` 内部权限位不适用，金额/运费正常透出
- [x] 双形态：`/portal/shipment/:id` 与 `/portal/shipment?no=`（04 号冻结二维码契约不回改）同一组件——有 `:id` 用 id，有 `?no=` 按 no 查询，守卫等价；无会话 `?no=` 直达（无 token）→ invalid 页引导先从业务员链接进入
- [x] `PortalShipmentRespVO` 白名单断言：序列化输出不含客户手机号、内部备注、成本相关字段；金额字段存在且正确
- [x] 前端 `shipment/list.vue`（Tab：待签收/已签收/已拒收/全部）+ `shipment/detail.vue`（逐行明细）；门户不提供打印
- [x] 越权专测：A 客户 token 访问 B 客户发货单（id 与 `?no=` 双入口）→ 038-001；跨租户 token（租户 X token 访问租户 Y 数据）→ 038-001；列表 customerId 过滤断言

## Answer

### 实现总结
1. **后端只读端点与越权守卫**：
   - `PortalShipmentController`：挂在 `/app-api/lunaerp/portal/shipment`，标注 `@PermitAll`。
   - `GET /page`：按 `PortalContext.getCustomerId()` 天然过滤本客户单据，仅查 `status = 'COMPLETED'`，支持 `signStatus`（PENDING / SIGNED / REFUSED / 全部）Tab 过滤，按 `shippingDate DESC, id DESC` 倒序分页。
   - `GET /get`：支持 `id` 与 `no` 双入口。执行严格安全守卫：
     - 单据存在性守卫（空返回 `1-030-038-001`）；
     - 单据状态守卫（非 `COMPLETED` 返回 `1-030-038-001`）；
     - 横向越权守卫（`customerId` 不匹配返回 `1-030-038-001`）；
     - 跨租户守卫（`tenantId` 不匹配返回 `1-030-038-001`）。
   - 聚合组装 `items` 与 `rolls`，金额与运费（`totalAmount`、`freightFee`、`unitPrice`、`amount`）权威透明展示，不触发 `@Sensitive` 掩码脱敏。
   - 严格 VO 白名单：绝不透出 `salesperson`、`deptId`、`remark`（内部发货/明细/卷备注）、成本与佣金等内部敏感信息。
2. **前端发货单模块与双形态直达**：
   - `src/api/portal/shipment.ts`：提供 `getPortalShipmentPage` 与 `getPortalShipmentDetail` API 及严格 TypeScript 契约。
   - `src/views/portal/shipment/list.vue`：包裹在 `PortalLayout` 中，接入 `usePortalAuth()`；支持全部/待签收/已签收/已拒收四 Tab 切换；卡片视图展示发货单号、日期、承运物流、透明货物金额与运费，点击直达详情。
   - `src/views/portal/shipment/detail.vue`：包裹在 `PortalLayout` 中，统一支持 `/portal/shipment/:id` 与二维码冻结直达形态 `/portal/shipment?no=`；展示单据头、物流、签收人与签收备注（如有）、透明金额统计、逐行发货明细（规格/色号/缸号/匹数/单价/金额）与卷清单展开；不暴露任何打印按钮。
3. **测试验证**：
   - 后端 15 个针对性测试（`PortalShipmentServiceImplTest` 13 项 + `PortalShipmentControllerTest` 2 项）全绿：涵盖分页查询、Tab 过滤、id/no 双形态查询、A/B 客户横向越权专测、跨租户专测、非 COMPLETED 拦截、严格白名单 JSON 序列化断言。全量 portal 后端测试 46 项全绿。
   - 前端 8 个针对性 Vitest 测试（`portal-shipment.spec.ts`）全绿：涵盖 API 契约、Tab 切换重载、状态 Tag 映射、金额格式化、id/?no= 双形态加载与路由契约。全量前端 391 个测试全绿。
