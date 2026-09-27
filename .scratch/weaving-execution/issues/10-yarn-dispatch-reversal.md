# 10: 仓库发纱（YARN_OUT + 不足拦截 + 状态推导）与撤销红冲

**What to build:** 领纱明细发行端点：选仓库（库位可空）+ 发放量，同事务写库存流水 `YARN_OUT`（负数，物品=yarn_spec_id，关联领纱单/织造单），回加 dispatchedQty 并推导领纱单状态：首次发→PARTIAL、各行 dispatchedQty≥requiredQty→DISPATCHED。**不足拦截**：同仓同规格余额 < 发放量时报 1-030-015-311（返回 required/available），由人减量/分批/换仓，不允许负库存。**撤销发纱**：按发纱记录红冲，写 `YARN_OUT_REVERSAL` 正数、回减 dispatchedQty、状态回退（DISPATCHED→PARTIAL/ACTIVE）；只允许冲本系统登记的发纱记录。前端领纱 Tab：每行"发纱"弹窗（显示可用量）、发纱记录列表与"撤销"（二次确认）；统计 Tab 用纱量取发纱汇总。错误码 312（状态不允许发纱）。

**Blocked by:** 09

**Status:** resolved

- [x] 发纱端点：同事务 YARN_OUT + dispatchedQty + 状态推导
- [x] 库存不足拦截（311，返回可用量）
- [x] 撤销红冲（YARN_OUT_REVERSAL、状态回退）
- [x] 前端发纱弹窗（可用量）/撤销与确认
- [x] 单测：正常发、不足拦截、红冲回退、并发

## Answer
- 后端实现：
  1. `StockLedgerMapper` 与 `StockLedgerService` 扩展 `sumQuantityByWarehouseAndYarnSpec` 和 `getYarnSpecBalance` 查询同仓同规格纱线当前结存。
  2. `YarnRequisitionItemServiceImpl.dispatchYarn` 发纱实现：
     - 校验领纱单处于 ACTIVE 或 PARTIAL 状态（否则报 312）。
     - 检查可用结存，结存不足报 `1_030_015_311`，带需求量与可用量。
     - 事务写入 `StockLedgerDO` 流水：`txnType = "YARN_OUT"`，数量为负数（`quantity = -dispatchQty`），物品类型 `YARN`，业务关联织造单领纱明细。
     - 累加明细 `dispatchedQty`，推导明细与领纱单状态（首次发纱推导为 `PARTIAL`，全部明细发齐推导为 `DISPATCHED`）。
  3. `YarnRequisitionItemServiceImpl.reverseDispatch` 撤销红冲实现：
     - 校验原流水存在且为 `YARN_OUT` 且属于当前系统关联记录。
     - 校验是否已被撤销（通过 `REVERSE_LEDGER_{origId}` 标记防重复撤销）。
     - 写入红冲流水 `txnType = "YARN_OUT_REVERSAL"`，数量为正数（`orig.getQuantity().negate()`）。
     - 扣减明细 `dispatchedQty`，推导明细与领纱单状态回退（如全部为 0 回退为 `ACTIVE`，部分则为 `PARTIAL`）。
  4. `WeavingOrderServiceImpl.getWeavingOrderStats` 汇总用纱统计与总发纱重量。
  5. 单元测试覆盖率：`YarnRequisitionItemServiceImplTest` 编写 15 个单测，覆盖发纱部分完成、发纱全完成、库存不足拦截（311）、状态不允许（312）、撤销回退为 PARTIAL、撤销回退为 ACTIVE、防重复红冲、并发余额模拟等，全部测试通过。
- 前端实现：
  1. `RequisitionTab.vue`：
     - 明细表格增加操作列【发纱】（ACTIVE/PARTIAL状态显示）和【发纱记录】按钮。
     - "发纱登记"弹窗：自动查询选择仓库下的当前可用纱线结存，显示结存标签与不足提示，限制最大发纱量。
     - "发纱记录与撤销"弹窗：表格展示历史发纱记录、红冲状态标签，支持对未红冲的记录执行【撤销】（带有 `ElMessageBox.confirm` 二次防误触确认）。
  2. `StatsTab.vue`：
     - 增加"用纱统计与发纱汇总"卡片，展示织造单累计已发纱总重量、纱线明细消耗进度与汇总表格。
  3. 编译验证：`vue-tsc` 与 `vite build` 均 0 错误构建通过。

