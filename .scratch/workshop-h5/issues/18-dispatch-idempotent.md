# 18: 发纱 dispatch（含 @Idempotent + requestId）+ 领纱详情页发纱表单

**What to build:** 落成移动发纱端点与领纱详情页发纱表单。POST `/lunaerp/h5/workshop/requisition-item/dispatch`（H5 专用 VO 带 `requestId` 前端 UUID 必填；Controller 注解 `@Idempotent(timeout=30, timeUnit=SECONDS, keyResolver=ExpressionIdempotentKeyResolver.class, keyArg="#reqVO.requestId", message="发纱正在处理，请勿重复提交")`；窗口内同 requestId 重复提交直接拦截/返回首次结果，**YARN_OUT 与库存扣减只发生一次**；服务端复用 `YarnRequisitionItemService.dispatchYarn`：ACTIVE/PARTIAL 守卫、余额不足 015-311、同事务 YARN_OUT+dispatchedQty+状态推导（ADR-0027）全部不变；`warehouseId` 必填校验；不开放撤销发纱（reverse-dispatch 留 PC，纠错需仓管介入）；`@PreAuthorize lunaerp:workshop:h5:dispatch`；`@LogRecord`）。前端 `src/views/h5/requisition/detail.vue` 发纱表单（逐行填量、改仓/查余额、发纱成功；requestId 一次生成）。可 demo：逐行填量→发纱成功。

**Blocked by:** 17（领纱单列表/详情与辅助查询已就位）, 02（infra_config 默认仓库 seed 已落）

**Status:** ready-for-agent

- [ ] POST `/lunaerp/h5/workshop/requisition-item/dispatch`（H5 专用 VO 带 `requestId` 前端 UUID 必填）
- [ ] Controller 注解 `@Idempotent(timeout=30, timeUnit=SECONDS, keyResolver=ExpressionIdempotentKeyResolver.class, keyArg="#reqVO.requestId", message="发纱正在处理，请勿重复提交")`
- [ ] 窗口内同 requestId 重复提交直接拦截/返回首次结果，YARN_OUT 与库存扣减只发生一次
- [ ] 服务端复用 `YarnRequisitionItemService.dispatchYarn`：ACTIVE/PARTIAL 守卫、余额不足 015-311、同事务 YARN_OUT+dispatchedQty+状态推导（ADR-0027）全部不变
- [ ] `warehouseId` 必填校验
- [ ] 不开放撤销发纱（reverse-dispatch 留 PC，纠错需仓管介入）
- [ ] `@PreAuthorize lunaerp:workshop:h5:dispatch`；`@LogRecord` 留痕
- [ ] 前端 `src/views/h5/requisition/detail.vue` 发纱表单（逐行填量、改仓/查余额、发纱成功；requestId 一次生成）
- [ ] 测试：发纱幂等同 requestId 连发两次（含模拟首请求超时重放）→ 一条 YARN_OUT、库存只扣一次、dispatchedQty 正确；不同 requestId 正常二次发纱；余额不足 → 015-311 且幂等键释放可重试
- [ ] demo：逐行填量→发纱成功
