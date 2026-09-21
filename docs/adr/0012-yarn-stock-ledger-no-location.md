# 0012 — 纱线库存通用流水台账（MVP 去库位字段，采购单完成自动写 STOCK_IN）

## 状态

deprecated（2026-09-21 回退：经排查功能设计不完善，过早开发，前后端程序和表全部取消）

## 上下文与决定

纱线库存用通用流水台账表 `lunaerp_production_stock_ledger`（参考系统 linaagi `StockLedger` 模式），存流水而非余额。表字段：

- `yarn_spec_id`（关联纱线规格子表；参考系统用 `productCode` 字符串，新项目改强关联 ID）
- `type`（枚举 `STOCK_IN` / `STOCK_OUT` / `PROCESS_IN` / `PROCESS_OUT`；MVP 实际只有 `STOCK_IN` 用得到，其余为未来扩展预留）
- `quantity`（数量，可正可负；`STOCK_IN` 正、`STOCK_OUT` 负、`PROCESS_IN` 正、`PROCESS_OUT` 负）
- `related_doc_type`（关联单据类型，如 `YARN_ORDER`）
- `related_doc_id`（关联单据 ID）
- `batch_no`（批号，可空）
- `remark`（备注，可空）
- 框架列（id / creator / create_time / updater / update_time / deleted / tenant_id）

**去掉 `source_location` / `target_location` 两列**（参考系统有，新项目 MVP 无库位概念）。

余额算法：

- `余额 = SUM(quantity) WHERE yarn_spec_id = ? AND type IN ('STOCK_IN', 'STOCK_OUT', 'PROCESS_IN', 'PROCESS_OUT')`
- MVP 阶段实际只有 `STOCK_IN`，余额只增不减

入库来源：**采购单 `ACTIVE → COMPLETED` 时后端自动写一条 `STOCK_IN` 台账**（`related_doc_type = YARN_ORDER`，`related_doc_id = 采购单 ID`，`quantity = 采购单明细实采量`）。**不做独立"纱线入库单"流程**（参考系统 `YarnStockIn` 模式 MVP 不引入）。

无"初始库存"概念——账户从 0 开始，采购单完成时累积入库。

## 理由

1. **用流水台账而非余额表理由**：流水台账保留审计能力，未来支持出库、盘点、调拨等场景只需追加 `type` 枚举值；余额表无法追溯历史变动。MVP 阶段虽只有 `STOCK_IN`，但表结构按流水设计避免未来重构。
2. **去库位字段理由**：参考系统 `source_location` / `target_location` 配套生产工序流转（`YARN_WAREHOUSE` / `PROCESS_WORKSHOP` 等），新项目 MVP 无生产工序流转概念，引入库位字段必为空值。未来要加再追加列。
3. **采购单完成自动写 STOCK_IN 理由**：参考系统通过独立"纱线入库单"流程触发（`yarn-stock-in-ledger.service.ts`），但 MVP 不引入入库单流程避免多一个完整子模块。采购单完成自动入库的业务语义合理（采购完成即入库）。
4. **无初始库存概念理由**：MVP 阶段不维护历史库存导入，账户从 0 开始。后续如有历史库存导入需求，可追加 `type = INITIAL` 枚举值或建独立导入工具。

## 考虑过的备选

- **简化 YarnStock 余额表**：直接存余额，最简。但无流水审计能力，未来支持出库/盘点需重构表结构。
- **保留库位字段（参考系统完全对齐）**：与参考系统一致。但 MVP 阶段两列必为空值，徒增表结构复杂度。未来要加再追加列。
- **独立纱线入库单流程（参考系统 YarnStockIn 模式）**：完整复刻参考系统入库链路。但 MVP 多一个完整子模块（入库单 CRUD + Controller + 前端页面），增量过重。
- **手工维护初始库存（type=INITIAL）**：可保留 INITIAL 入库类型支持初始库存导入。但 MVP 阶段无历史库存，账户从 0 开始即可。后续如需可追加。

## 后果

- 表 `lunaerp_production_stock_ledger` 单表，无主从结构。
- `type` 枚举值保留 4 个（`STOCK_IN` / `STOCK_OUT` / `PROCESS_IN` / `PROCESS_OUT`），MVP 实际只用 `STOCK_IN`，其余枚举值为未来扩展预留。
- 余额查询 API：`GET /lunaerp/production/yarn-stock/balance?yarnSpecId=` 返回 `{yarnSpecId, stockQty, ...}`。
- 采购单 `ServiceImpl` 完成状态流转时事务内追加写 `StockLedger` 流水（一条采购单 N 条明细行写 N 条 `STOCK_IN` 台账）。
- 测算时后端按 `yarn_spec_id` 查 `StockLedger` `SUM(quantity)` 拿 `stockQty`。
- 错误码段位 `1-030-008` 给纱线档案 + 配比 BOM 共用，`1-030-009` 给纱线采购单 + 明细共用。
