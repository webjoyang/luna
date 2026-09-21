# 04: 纱线库存台账（StockLedger）

**What to build:** 落地通用 StockLedger 流水台账表（去 source_location/target_location 两列，ADR-0012）+ 后端 Service 提供余额查询与 STOCK_IN 写入接口。库存台账是 append-only 流水，无 CRUD 端点（无 create/update/delete 接口），仅由后端服务内部调用（采购单完成时自动写 STOCK_IN，详见 issue 06）+ 暴露 1 个余额查询接口（GET /yarn-stock/balance）供测算调用。MVP 实际只用 STOCK_IN 类型，其余类型（STOCK_OUT/PROCESS_IN/PROCESS_OUT）为占位。**前端不做库存页**（仅后端服务+测算调用）。

**Blocked by:** 01 纱线档案地基（需要 yarn_spec 表）

**Status:** wontfix

> **回退说明 (2026-09-21)**: 经排查，纱线库存台账功能设计不完善，过早开发。经决定回退本任务，暂不开发相关功能。相关数据库表（lunaerp_production_stock_ledger）、后端 Service/Mapper/DO/Controller/单测及前端 API 全部取消删除，采购单测算在库量缺省为 0，采购单完成不自动写台账。

- [x] `luna-srv/sql/postgresql/lunaerp-production-yarn-order.sql` 内追加表 5 段（同一 SQL 文件，集中管理纱线域变更）：
  - [x] 表 5 `lunaerp_production_stock_ledger` DDL（yarn_spec_id/type/quantity/related_doc_type/related_doc_id/batch_no/remark + 框架列）+ 主键 DO 块 + 列注释 + 序列
  - [x] 普通索引 `(yarn_spec_id, type)` + `(related_doc_type, related_doc_id)`
  - [x] **无 source_location/target_location 两列**（ADR-0012）
  - [x] 无菜单无字典（type 枚举仅服务内部使用，不入字典）
- [x] 后端 `production.stockledger` 子包：
  - [x] `StockLedgerDO`（继承 TenantBaseDO，含 type 字段用 String 存枚举字面量，不引入 Java enum 简化）
  - [x] `StockLedgerMapper`（extends BaseMapperX）
  - [x] `StockLedgerService` 接口：
    - [x] `BigDecimal getBalance(Long yarnSpecId)` — 单规格余额（MVP = SUM(quantity) WHERE yarn_spec_id=? AND type='STOCK_IN' AND deleted=0）
    - [x] `Map<Long, BigDecimal> getBalances(Collection<Long> yarnSpecIds)` — 批量余额，一次性查询返回 map
    - [x] `void writeStockIn(Long yarnSpecId, BigDecimal quantity, String relatedDocType, Long relatedDocId, String batchNo, String remark)` — 写入一条 STOCK_IN 记录
    - [x] `boolean existsByRelatedDoc(String relatedDocType, Long relatedDocId)` — 幂等检查（采购单完成前检查是否已写台账）
  - [x] `StockLedgerServiceImpl`：
    - [x] getBalance：用 Mapper 聚合查询（SELECT COALESCE(SUM(quantity),0) FROM ... WHERE yarn_spec_id=? AND type='STOCK_IN' AND deleted=0 AND tenant_id=?）
    - [x] getBalances：用 Mapper 聚合查询 GROUP BY yarn_spec_id，返回 Map
    - [x] writeStockIn：构造 StockLedgerDO（type='STOCK_IN'）insert
    - [x] existsByRelatedDoc：SELECT COUNT(*) WHERE related_doc_type=? AND related_doc_id=? AND deleted=0
  - [x] `YarnStockController`（2 端点，无 CRUD）：
    - [x] `GET /yarn-stock/balance?yarnSpecId=` — 返回 `{yarnSpecId, stockQty}`
    - [x] `GET /yarn-stock/balances?yarnSpecIds=1,2,3` — 批量返回 `[{yarnSpecId, stockQty}]`
    - [x] `@PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-stock:query')")`
    - [x] 无 create/update/delete 端点（库存流水不可篡改）
- [x] 后端单测 `StockLedgerServiceImplTest`：
  - [x] getBalance：单 yarn_spec_id 余额正确（SUM STOCK_IN quantity）
  - [x] getBalances：多 yarn_spec_id 一次返回正确
  - [x] writeStockIn：写入一条 STOCK_IN 记录，quantity/related_doc_type/related_doc_id/batch_no/remark 正确
  - [x] existsByRelatedDoc：存在/不存在两种情况
- [x] 前端 API `luna-ui/src/api/lunaerp-elmui/production/yarn-stock/index.ts`：
  - [x] `getBalance(yarnSpecId)` / `getBalances(yarnSpecIds)`
  - [x] TS 类型：`{yarnSpecId: number; stockQty: number}`
  - [x] （无前端视图，仅供采购单工作台测算流程调用）
- [x] 验证：
  - [x] 后端 `mvn test -pl lunasrv-module-lunaerp -Dtest=StockLedgerServiceImplTest` 全绿
  - [x] 前端 `pnpm ts:check` 改动文件零报错
