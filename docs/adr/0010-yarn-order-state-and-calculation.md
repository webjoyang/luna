# 0010 — 纱线采购单状态四态、测算口径与后端测算接口

## 状态

accepted

## 上下文与决定

纱线采购单（YarnOrder）状态为四态：`DRAFT` 草稿（新建缺省）/ `ACTIVE` 进行中 / `COMPLETED` 已完成 / `CANCELLED` 已取消。沿用染色单（ADR-0008）四态口径：保存时默认为草稿，点"下达"转 ACTIVE；编辑/删除仅 DRAFT 可，ACTIVE 只可"完成/取消"。状态字段 `varchar(16)` 字符串枚举，走字典 `lunaerp_yarn_order_status`（DRAFT/ACTIVE/COMPLETED/CANCELLED）。

测算口径（基于参考系统 linaagi `web/lib/yarn-purchase-calc.ts` buildPurchaseRows L13-52）：

- `ratio` 与 `lossRate` 均用**百分比数字**（49 = 49%，5 = 5%），与小数无关
- 基础量公式：`baseQty = Σ(胚布单成品量 × 配比%)`，按纱线规格合并多单需求
- 需求量公式：`requiredQty = baseQty × (1 + lossRate/100)`
- 实采量公式：`purchaseQty = max(0, requiredQty - stockQty)`
- 金额公式：`amount = unitPrice × purchaseQty`（单价可手改，服务端保存时重算）

测算接口：**后端提供 `POST /admin-api/lunaerp/production/yarn-order/calculate`**，前端只展示测算结果。与参考系统"纯前端 buildPurchaseRows"模式刻意不一致——理由是金额（unitPrice/totalAmount）放后端算避免前端篡改、单源真理、与产品档案历史价回填同源。

## 理由

1. **四态仿染色单**：纱线采购同样存在"先打草稿反复确认、再正式下达给纱线厂"的形态。DRAFT 态承载"未正式下达"语义，避免半成品单据污染已下达数据。
2. **沿用参考系统测算口径**：ratio/lossRate 用百分比数字与参考系统代码完全对齐，移植成本低。新项目胚布单 `loss_rate` 字段是小数（0.05 = 5%），与采购单 `lossRate` 百分比数字（5 = 5%）口径不一致是**刻意的**——两者业务语义不同（胚布单 `demand_qty = 成品量 ÷ (1 - loss_rate)` vs 采购单 `requiredQty = baseQty × (1 + lossRate/100)`），单位差异与算法差异同步保留。
3. **后端测算接口**：测算涉及金额计算，前端纯算有篡改风险；后端算保证金额口径统一、便于历史价回填、便于后续追加测算日志。

## 考虑过的备选

- **三态 ACTIVE/COMPLETED/CANCELLED（与合同/胚布单完全一致）**：全系统状态枚举统一，认知成本低。但纱线采购的"草稿反复修改"需求无法表达，参考染色单 ADR-0008 决策。
- **测算口径统一胚布单（小数 + 除法 `÷(1-loss)`）**：与胚布单口径一致，认知成本最低。但移植成本高（需重写测算逻辑而非直接复用参考系统），且数学差异极小（损耗 5% 时差异约 0.26%）。刻意保留两套口径。
- **测算纯前端算（参考系统模式）**：与参考系统完全对齐。但金额计算放前端有篡改风险，且后端保存时仍需重算金额。后端测算接口更安全。

## 后果

- 全系统出现三套单据状态枚举：三态（合同/胚布单）+ 四态（染色单 + 纱线采购单）。CONTEXT.md 已显式记录该刻意不一致，未来工程师看到纱线采购单有 DRAFT 不应"修正"为三态。
- 全系统出现两套损耗率口径：胚布单 `loss_rate` 用小数（0.05 = 5%）+ 除法公式；纱线采购单 `lossRate` 用百分比数字（5 = 5%）+ 乘法公式。CONTEXT.md 已显式记录该刻意不一致，未来工程师**不应**统一两套口径。
- 前端编辑页需根据状态切换按钮显示与只读模式：DRAFT 显示"保存/下达/删除"，ACTIVE 显示"完成/取消"，COMPLETED/CANCELLED 全部只读。
- 后端 `update` / `delete` 接口需校验状态，非 DRAFT 拒绝操作并返回 `1_030_009_002`。
- 测算接口 `POST /yarn-order/calculate` 入参为胚布单 IDs 列表 + 损耗率，出参为测算后的明细行（含 `yarnTypeId` / `yarnSpecId` / `ratio` / `baseQty` / `lossRate` / `requiredQty` / `stockQty` / `purchaseQty` / `refUnitPrice`）。
