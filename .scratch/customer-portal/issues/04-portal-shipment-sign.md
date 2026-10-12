# 04: 门户签收（整单粒度，复用既有签收守卫，零新状态）

**What to build:** 客户在门户发货单详情对 PENDING 单据登记签收或拒收：手填签收人必填 + 二次确认弹窗。后端新开 portal 端点但服务层复用既有发货单签收逻辑（仅 COMPLETED、PENDING/SIGNED/REFUSED 条件更新），不改状态机、零新状态。可 demo：客户在详情点签收→弹窗填姓名→确认→列表状态变已签收；管理端同单签收不冲突。

**Blocked by:** 03（门户发货单只读——详情页承载签收按钮）

**Status:** resolved

- [x] `PUT /app-api/lunaerp/portal/shipment/sign`：body `{id, signStatus(SIGNED|REFUSED), signedByName(必填), signRemark?}`；signedAt=服务器时间
- [x] 服务层复用既有签收守卫（仅 COMPLETED 可签、signStatus ∈ PENDING/SIGNED/REFUSED 条件更新）；portal 层只做身份注入与归属校验（customerId 不符 → 038-001），不暴露内部 `@PreAuthorize` 接口
- [x] 与内部管理端签收的并发条件更新无双写竞争（复用现有守卫语义）；REFUSED 不产生任何库存/账务副作用（与现有 update-sign 一致）
- [x] `@LogRecord` 签收留痕
- [x] 前端：签收按钮仅 PENDING 状态显示；二次确认弹窗手填签收人必填、拒收可填备注
- [x] 专测：仅 COMPLETED 可签；signedByName 必填校验；signedAt=服务器时间；SIGNED/REFUSED 两态落库；与内部管理端并发签收条件更新；归属越权 → 038-001；REFUSED 零副作用

## Answer

### 实现概要
1. **后端 API & 安全**：
   - 在 [`PortalShipmentController`](file:///D:/work/luna/20261011/luna/luna-srv/lunasrv-module-lunaerp/src/main/java/com/luna/lunasrv/module/lunaerp/portal/controller/portal/shipment/PortalShipmentController.java) 新增 `PUT /app-api/lunaerp/portal/shipment/sign` 端点，入参 `PortalShipmentSignReqVO`（`id`, `signStatus`, `signedByName` 必填，`signRemark` 选填）。
   - 服务层 [`PortalShipmentServiceImpl.signShipment`](file:///D:/work/luna/20261011/luna/luna-srv/lunasrv-module-lunaerp/src/main/java/com/luna/lunasrv/module/lunaerp/service/portal/PortalShipmentServiceImpl.java) 实现多租户与当前客户越权校验（客户不符抛出 `PORTAL_CUSTOMER_MISMATCH` 038-001）。
   - 签收守卫与核心逻辑：仅 `COMPLETED` 出库状态允许签收，只接受 `SIGNED` 或 `REFUSED` 目标签收状态；使用服务器时间作为 `signedAt`。
   - 调用 `shipmentMapper.update(updateObj, wrapper)` 实现整单条件更新（乐观更新），与管理端签收逻辑完全一致，零新状态，拒绝不产生额外库存/财务副作用；添加 `@LogRecord` 操作日志留痕。
2. **前端门户发货单签收**：
   - [`api/portal/shipment.ts`](file:///D:/work/luna/20261011/luna/luna-ui/src/api/portal/shipment.ts) 增加 `signPortalShipment` API。
   - [`views/portal/shipment/detail.vue`](file:///D:/work/luna/20261011/luna/luna-ui/src/views/portal/shipment/detail.vue) 发货单详情头部仅在 `signStatus === 'PENDING'` 且出库状态为 `COMPLETED` 时展示「确认签收」和「拒收」操作按钮；提供二次确认弹窗，要求签收人必填、拒收支持备注，操作成功后刷新发货单详情。
3. **测试覆盖**：
   - 后端单测 [`PortalShipmentServiceImplTest`](file:///D:/work/luna/20261011/luna/luna-srv/lunasrv-module-lunaerp/src/test/java/com/luna/lunasrv/module/lunaerp/service/portal/PortalShipmentServiceImplTest.java) 与 [`PortalShipmentControllerTest`](file:///D:/work/luna/20261011/luna/luna-srv/lunasrv-module-lunaerp/src/test/java/com/luna/lunasrv/module/lunaerp/portal/controller/portal/shipment/PortalShipmentControllerTest.java)，覆盖非 COMPLETED 状态拦截、签收人必填、服务器时间落库、越权 038-001、SIGNED/REFUSED 状态流转等场景。
   - 前端组件测试 [`portal-shipment-sign.spec.ts`](file:///D:/work/luna/20261011/luna/luna-ui/src/views/portal/__tests__/portal-shipment-sign.spec.ts) 9 项测试全部通过。

### Commit Hash
- Ticket 分支提交：`b5937b5`
- 合并提交：`f25f430`
