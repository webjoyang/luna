# 01: SQL 基建（DDL + 6 个 dp 索引 + 租户感知回填 + 角色基线）

**What to build:** 执行单文件 `luna-srv/sql/postgresql/lunaerp-data-permission-wiring.sql`（登记进 install `20 商业化` 段、approval-engine 之后）后，客户/合同/发货 3 表新增 `salesperson_user_id`/`dept_id` 列与 6 个 `WHERE deleted=0` 部分索引就位，空 owner/dept 按租户感知口径只填空回填，`common`/`test-dp` 角色迁移、11 角色 `data_scope` 基线对齐。脚本在空库与存量库重跑均不报错、不丢数据。

**Blocked by:** None（可立即开工）

**Status:** ready-for-agent

- [ ] 客户表加 `salesperson_user_id int8 NULL` + `dept_id int8 NULL`（带 COMMENT）；合同表、发货表各加 `dept_id int8 NULL`（带 COMMENT）。全部 `ADD COLUMN IF NOT EXISTS` 幂等。报价两表 DDL 由 05 票建表脚本带列，本文件不含其 ALTER
- [ ] 6 个 dp 部分索引就位：`idx_lb_customer_dp_dept` / `idx_lb_customer_dp_user` / `idx_lb_sales_contract_dp_dept` / `idx_lb_sales_contract_dp_user` / `idx_lb_shipment_dp_dept` / `idx_lb_shipment_dp_user`，均为 `(tenant_id, <列>) WHERE deleted=0`，`CREATE INDEX IF NOT EXISTS`
- [ ] 回填全部 `目标列 IS NULL` 守卫只填空：客户 owner（creator 用户→租户 admin 兜底）、客户 dept（owner 当前部门→creator 用户部门→租户根部门）、合同/发货 dept（已有 salesperson 当前部门→creator 用户部门→兜底用户部门→租户根部门；不动 `salesperson_user_id`）。兜底用户/部门按子查询定位（CTE `DISTINCT ON (tenant_id) … ORDER BY (r.code='tenant_admin') DESC, u.id`；根部门 `system_dept.parent_id=0 AND deleted=0` 取最小 id），禁止写死 id；`creator ~ '^[0-9]+$'` 守卫非数字串
- [ ] 角色对齐：`common` data_scope `2→5`、清 `data_scope_dept_ids`；`test-dp` `2→1`、清 `data_scope_dept_ids`；11 seed 角色按 code 幂等对齐（boss/finance/planner/purchaser/warehouse_manager/warehouse_keeper/inspector=1；sales_manager=4；salesperson/workshop_worker/workshop_inspector=5）；`data_scope_dept_ids` 对齐角色统一置 `''`
- [ ] install `20 商业化` 段在 approval-engine 之后登记本脚本；`_order.md` 已就位（21 位），无需改动
- [ ] 空库（10 段先建表）与存量库重跑两次均幂等、零报错、零数据覆盖；多租户各归各的 admin/根部门
- [ ] EXPLAIN 见 dp 索引走 bitmap index scan（受限档 BitmapOr），ALL 档零影响、现有查询计划不变
