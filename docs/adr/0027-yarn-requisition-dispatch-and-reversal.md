# ADR-0027：领纱单独立子资源 + BOM 测算，发纱写 YARN_OUT、不足拦截、支持红冲

**状态**：Accepted
**日期**：2026-09-25
**关联**：生产管理-织造执行；ADR-0014（独立子资源先例）、ADR-0026（纱线库存）、ADR-0010（测算端点先例）

## 背景

参考系统有完整领纱链路：领纱单（`YarnRequisition`）→ 仓库发纱（`YarnDispatch`，扣减纱线库存）
→ 可撤销发纱。新项目纱线库存已通过 ADR-0026 具备，需要把车间用纱的申请与发放流程补齐。

## 决策

### 1. 领纱单与明细

- 主表挂织造单（`weaving_order_id`），字段：`code`、`status`、`remark`。
- 编号采用**派生式**：`YS-{织造单号去符号}-{单内 2 位序号}`，与织造执行全模块编号哲学一致；
  服务端生成、可手填、软删不释放。
- 状态五态（字典 `lunaerp_yarn_requisition_status`）：
  `DRAFT` 草稿（缺省）/ `ACTIVE` 待发纱 / `PARTIAL` 部分发纱 / `DISPATCHED` 已发纱 / `CLOSED` 已关闭。
  明细定稿（提交）后进入 ACTIVE；首次发纱→PARTIAL；各行发齐（dispatchedQty ≥ requiredQty）→DISPATCHED；
  CLOSED 手动、为终态。
- 明细（`yarnTypeId`+`yarnSpecId`+`yarnSpecName` 快照、`requiredQty`、`dispatchedQty`、`unit=KG`、`remark`）
  **作为独立子资源**：有独立端点与权限码，逐行维护、逐行发纱（理由同检验明细 ADR-0014——发纱是高频逐条动作，不随整单替换）。

### 2. BOM 测算（后端 calculate，前端不计算）

`某纱线规格需求 KG = 织造单计划匹数 × 单匹计划重量 × ratio / 100`

- 单匹计划重量 = 织造单 `plannedWeight / plannedQty`；
- 配比 BOM 取该产品的 `yarn_recipe_bom`（及明细，ratio 为百分比数字，49=49%），按 `yarnSpecId` 合并；
- 无 BOM / 缺计划重量时返回提示，由人工填写；测算结果只填充/对比，可手工修改。

### 3. 发纱

- 端点按领纱明细行发纱：在同事务写库存流水 `YARN_OUT`（负数，物品身份为 yarn_spec_id，
  仓库必选、库位可空），回加该行 `dispatchedQty` 并推导领纱单状态。
- **库存不足拦截**：可用量（同仓同规格流水余额）小于本次发放量时报错，错误返回需求量/可用量；
  由人决定减量、分批或换仓——不允许账面负库存。
- 幂等：发纱不做自动重试，重复点击靠余额拦截兜底。

### 4. 撤销发纱（红冲）

- 已发出的发纱记录可撤销：写 `YARN_OUT_REVERSAL` 正数流水，回减 `dispatchedQty`，
  领纱单状态从 DISPATCHED/PARTIAL 相应回退（DISPATCHED→PARTIAL/ACTIVE）。
- 红冲同样受余额约束的反向保护：撤销后该仓该规格余额不得为负之外的异常——即只允许撤销本系统登记过的发纱，
  沿用入库红冲"按原记录逐条冲"的口径。

## 后果

- 领纱、发纱、用纱在数据上闭环：发纱量有流水可查，撤销有红冲留痕，流水只增不改。
- 车间与仓库分权：领纱申请与仓库发纱使用不同权限码（`:create` / `:dispatch`），与入库单制单/审核分权同构。
- 本期不做"按布票/产量回算实际用纱、对比领纱差异"——差异分析待后续；统计 Tab 的用纱量取发纱流水汇总。
