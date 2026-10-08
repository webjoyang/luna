# 04: 门户签收（整单粒度，复用既有签收守卫，零新状态）

**What to build:** 客户在门户发货单详情对 PENDING 单据登记签收或拒收：手填签收人必填 + 二次确认弹窗。后端新开 portal 端点但服务层复用既有发货单签收逻辑（仅 COMPLETED、PENDING/SIGNED/REFUSED 条件更新），不改状态机、零新状态。可 demo：客户在详情点签收→弹窗填姓名→确认→列表状态变已签收；管理端同单签收不冲突。

**Blocked by:** 03（门户发货单只读——详情页承载签收按钮）

**Status:** ready-for-agent

- [ ] `PUT /app-api/lunaerp/portal/shipment/sign`：body `{id, signStatus(SIGNED|REFUSED), signedByName(必填), signRemark?}`；signedAt=服务器时间
- [ ] 服务层复用既有签收守卫（仅 COMPLETED 可签、signStatus ∈ PENDING/SIGNED/REFUSED 条件更新）；portal 层只做身份注入与归属校验（customerId 不符 → 038-001），不暴露内部 `@PreAuthorize` 接口
- [ ] 与内部管理端签收的并发条件更新无双写竞争（复用现有守卫语义）；REFUSED 不产生任何库存/账务副作用（与现有 update-sign 一致）
- [ ] `@LogRecord` 签收留痕
- [ ] 前端：签收按钮仅 PENDING 状态显示；二次确认弹窗手填签收人必填、拒收可填备注
- [ ] 专测：仅 COMPLETED 可签；signedByName 必填校验；signedAt=服务器时间；SIGNED/REFUSED 两态落库；与内部管理端并发签收条件更新；归属越权 → 038-001；REFUSED 零副作用
