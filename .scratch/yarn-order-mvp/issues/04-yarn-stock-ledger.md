# 04: 纱线库存台账（StockLedger）

**What to build:** 落地通用 StockLedger 流水台账表（去 source_location/target_location 两列，ADR-0012）+ 后端 Service 提供余额查询与 STOCK_IN 写入接口。库存台账是 append-only 流水，无 CRUD 端点（无 create/update/delete 接口），仅由后端服务内部调用（采购单完成时自动写 STOCK_IN，详见 issue 06）+ 暴露 1 个余额查询接口（GET /yarn-stock/balance）供测算调用。MVP 实际只用 STOCK_IN 类型，其余类型（STOCK_OUT/PROCESS_IN/PROCESS_OUT）为占位。**前端不做库存页**（仅后端服务+测算调用）。

**Blocked by:** 01 纱线档案地基（需要 yarn_spec 表）

**Status:** ready-for-agent

- [ ] `luna-srv/sql/postgresql/lunaerp-production-yarn-order.sql` 内追加表 5 段（同一 SQL 文件，集中管理纱线域变更）：
  - [ ] 表 5 `lunaerp_production_stock_ledger` DDL（yarn_spec_id/type/quantity/related_doc_type/related_doc_id/batch_no/remark + 框架列）+ 主键 DO 块 + 列注释 + 序列
  - [ ] 普通索引 `(yarn_spec_id, type)` + `(related_doc_type, related_doc_id)`
  - [ ] **无 source_location/target_location 两列**（ADR-0012）
  - [ ] 无菜单无字典（type 枚举仅服务内部使用，不入字典）
- [ ] 后端 `production.stockledger` 子包：
  - [ ] `StockLedgerDO`（继承 TenantBaseDO，含 type 字段用 String 存枚举字面量，不引入 Java enum 简化）
  - [ ] `StockLedgerMapper`（extends BaseMapperX）
  - [ ] `StockLedgerService` 接口：
    - [ ] `BigDecimal getBalance(Long yarnSpecId)` — 单规格余额（MVP = SUM(quantity) WHERE yarn_spec_id=? AND type='STOCK_IN' AND deleted=0）
    - [ ] `Map<Long, BigDecimal> getBalances(Collection<Long> yarnSpecIds)` — 批量余额，一次性查询返回 map
    - [ ] `void writeStockIn(Long yarnSpecId, BigDecimal quantity, String relatedDocType, Long relatedDocId, String batchNo, String remark)` — 写入一条 STOCK_IN 记录
    - [ ] `boolean existsByRelatedDoc(String relatedDocType, Long relatedDocId)` — 幂等检查（采购单完成前检查是否已写台账）
  - [ ] `StockLedgerServiceImpl`：
    - [ ] getBalance：用 Mapper 聚合查询（SELECT COALESCE(SUM(quantity),0) FROM ... WHERE yarn_spec_id=? AND type='STOCK_IN' AND deleted=0 AND tenant_id=?）
    - [ ] getBalances：用 Mapper 聚合查询 GROUP BY yarn_spec_id，返回 Map
    - [ ] writeStockIn：构造 StockLedgerDO（type='STOCK_IN'）insert
    - [ ] existsByRelatedDoc：SELECT COUNT(*) WHERE related_doc_type=? AND related_doc_id=? AND deleted=0
  - [ ] `YarnStockController`（2 端点，无 CRUD）：
    - [ ] `GET /yarn-stock/balance?yarnSpecId=` — 返回 `{yarnSpecId, stockQty}`
    - [ ] `GET /yarn-stock/balances?yarnSpecIds=1,2,3` — 批量返回 `[{yarnSpecId, stockQty}]`
    - [ ] `@PreAuthorize("@ss.hasPermission('lunaerp:production:yarn-stock:query')")`
    - [ ] 无 create/update/delete 端点（库存流水不可篡改）
- [ ] 后端单测 `StockLedgerServiceImplTest`：
  - [ ] getBalance：单 yarn_spec_id 余额正确（SUM STOCK_IN quantity）
  - [ ] getBalances：多 yarn_spec_id 一次返回正确
  - [ ] writeStockIn：写入一条 STOCK_IN 记录，quantity/related_doc_type/related_doc_id/batch_no/remark 正确
  - [ ] existsByRelatedDoc：存在/不存在两种情况
- [ ] 前端 API `luna-ui/src/api/lunaerp-elmui/production/yarn-stock/index.ts`：
  - [ ] `getBalance(yarnSpecId)` / `getBalances(yarnSpecIds)`
  - [ ] TS 类型：`{yarnSpecId: number; stockQty: number}`
  - [ ] （无前端视图，仅供采购单工作台测算流程调用）
- [ ] 验证：
  - [ ] 后端 `mvn test -pl lunasrv-module-lunaerp -Dtest=StockLedgerServiceImplTest` 全绿
  - [ ] 前端 `pnpm ts:check` 改动文件零报错
