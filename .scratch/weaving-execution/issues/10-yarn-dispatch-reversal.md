# 10: 仓库发纱（YARN_OUT + 不足拦截 + 状态推导）与撤销红冲

**What to build:** 领纱明细发行端点：选仓库（库位可空）+ 发放量，同事务写库存流水 `YARN_OUT`（负数，物品=yarn_spec_id，关联领纱单/织造单），回加 dispatchedQty 并推导领纱单状态：首次发→PARTIAL、各行 dispatchedQty≥requiredQty→DISPATCHED。**不足拦截**：同仓同规格余额 < 发放量时报 1-030-015-311（返回 required/available），由人减量/分批/换仓，不允许负库存。**撤销发纱**：按发纱记录红冲，写 `YARN_OUT_REVERSAL` 正数、回减 dispatchedQty、状态回退（DISPATCHED→PARTIAL/ACTIVE）；只允许冲本系统登记的发纱记录。前端领纱 Tab：每行"发纱"弹窗（显示可用量）、发纱记录列表与"撤销"（二次确认）；统计 Tab 用纱量取发纱汇总。错误码 312（状态不允许发纱）。

**Blocked by:** 09

**Status:** ready-for-agent

- [ ] 发纱端点：同事务 YARN_OUT + dispatchedQty + 状态推导
- [ ] 库存不足拦截（311，返回可用量）
- [ ] 撤销红冲（YARN_OUT_REVERSAL、状态回退）
- [ ] 前端发纱弹窗（可用量）/撤销与确认
- [ ] 单测：正常发、不足拦截、红冲回退、并发
