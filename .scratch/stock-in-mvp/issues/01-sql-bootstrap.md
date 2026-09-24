# 01: 库存域 SQL 基建（6 表 DDL + 10300 菜单 + 3 字典）

**What to build:** 执行 `lunaerp-stock.sql` 后，仓库、库位、入库单主表、入库明细、库位放置、库存流水 6 张表连同序列与索引就位；系统管理中出现"库存管理"目录及入库管理、库存查询、仓库管理菜单与按钮权限；入库状态（DRAFT/COMPLETED/CANCELLED）、入库类型（PURCHASE/PRODUCTION/RETURN/OTHER）、入库单位（KG/M）3 个字典可在字典管理查看。脚本可重复执行。

**Blocked by:** None (can start immediately)

**Status:** ready-for-human

- [x] 6 张表 DDL 全部落库（含主键 DO 块判存、列注释、序列）
- [x] 唯一索引就位：入库单号、仓库编号含软删行；库位编码同仓库内唯一含软删行
- [x] 10300 库存管理目录 + 10310/10320/10330 菜单及 10311-10314、10331-10336 按钮权限注册
- [x] 字典 2233/2234/2235 + dict_data 3592-3600 初始化
- [x] 脚本可重复执行（幂等）

## Comments

- 2026-09-24（agent）：
  - 落地 `luna-srv/sql/postgresql/lunaerp-stock.sql`：
    1. 6 张表 DDL（`lunaerp_stock_warehouse`、`lunaerp_stock_warehouse_location`、`lunaerp_stock_in`、`lunaerp_stock_in_item`、`lunaerp_stock_in_item_placement`、`lunaerp_stock_ledger`），均包含标准 TenantBaseDO 字段、DO 块主键判存与全列注释。
    2. 6 个主键序列（`lunaerp_stock_warehouse_seq`、`lunaerp_stock_warehouse_location_seq`、`lunaerp_stock_in_seq`、`lunaerp_stock_in_item_seq`、`lunaerp_stock_in_item_placement_seq`、`lunaerp_stock_ledger_seq`）。
    3. 唯一索引全量含软删行：仓库编号 `uk_lb_stock_warehouse_tenant_no`、库位编码同仓唯一 `uk_lb_stock_location_tenant_wh_code`、入库单号 `uk_lb_stock_in_tenant_in_no`。
    4. 辅助索引：流水台账按 `(product_id, warehouse_id, unit)`（余额聚合主路径）、`(related_doc_type, related_doc_id)`（红冲反查原流水）、`(warehouse_id)`、`(create_time)`；入库单主表、明细表、放置表及仓库库位状态关联索引齐全。
    5. 菜单权限注册：10300 顶级库存管理目录（sort 103）+ 10310 入库管理菜单与 10311-10314 按钮 + 10320 库存查询菜单 + 10330 仓库管理菜单与 10331-10336 仓库/库位按钮。
    6. 字典初始化：2233 `lunaerp_stock_in_status`（3592-3594，草稿/已审核/已取消）、2234 `lunaerp_stock_in_type`（3595-3598，采购入库/生产入库/退货入库/其他入库）、2235 `lunaerp_stock_in_unit`（3599-3600，千克/米）。
    7. 事务内先 DELETE 再 INSERT，幂等可重复执行。
  - 前端字典常量：`luna-ui/src/utils/dict.ts` 追加 3 个字典类型常量枚举。
