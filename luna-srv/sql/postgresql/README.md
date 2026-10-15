# lunaerp PostgreSQL 脚本执行约定与状态表

> 状态核对时间：**2026-10-15**
> 核对库：`application-local.yaml` 指向的 `ruoyi-vue-pro`（腾讯云 PostgreSQL）
> 核对方式：只读查询 `information_schema.tables / columns` 比对各文件声明的对象 + 种子表行数抽查

---

## 一、执行顺序约定

### 1. 新环境从零初始化（固定顺序）

1. `ruoyi-vue-pro.sql` —— 底座 schema（yudao 系统表 + 基础数据），必须最先执行
2. `quartz.sql` —— Quartz 集群表（**按需**：local 环境已排除 Quartz 自动装配，可不执行）
3. 其余 `lunaerp-*.sql` —— 按下方状态表中「首次提交」时间**从早到晚**依次执行

### 2. 日常新增变更的约定

- **一域一文件**：新表/新列追加到对应业务域文件**末尾**，并在语句上方注释 `-- [YYYY-MM-DD] 工单/说明`
- **必须幂等**：`CREATE TABLE IF NOT EXISTS`、`ADD COLUMN IF NOT EXISTS`、种子 `INSERT ... ON CONFLICT DO NOTHING`，保证重复执行不出错
- **PostgreSQL 无自增**：插入 `system_menu` / `system_role_menu` 等系统表时必须显式提供 `id`（菜单用固定 id；`system_role_menu` 用 `nextval('system_role_menu_seq')`，插入前用 `setval(seq, GREATEST(max(id),1))` 对齐序列防主键冲突。MySQL 自增写法在 PG 会报 `null value in column "id"`）
- **跨文件依赖**：若新文件依赖其他文件的表，在文件头部注释 `-- DEPENDS ON: lunaerp-xxx.sql`
- 新文件创建后，**执行完毕立即更新本文档状态表**

---

## 二、执行状态表（2026-10-15 核对）

图例：✅ 已执行 ｜ ⚠️ 部分执行（早期版本已跑，后续增量未应用）｜ ❌ 未执行

| # | 文件 | 首次提交 | 状态 | 缺失对象 / 备注 |
|---|------|----------|------|----------------|
| 1 | ruoyi-vue-pro.sql | 2026-09-13 | ✅ | 仅 demo 示例表未建（`lunasrv_demo01/02/03_*`），正常 |
| 2 | quartz.sql | 2026-09-13 | ❌ | 11 张 `qrtz_*` 表全部不存在；local 已禁用 Quartz，按需执行 |
| 3 | lunaerp-baseinfo-product.sql | 2026-09-13 | ✅ | |
| 4 | lunaerp-baseinfo-customer.sql | 2026-09-18 | ⚠️ | 缺列：`customer.address_en / name_en / dept_id / salesperson_user_id` |
| 5 | lunaerp-business-sales-contract.sql | 2026-09-18 | ⚠️ | 缺列：`sales_contract.approval_status` |
| 6 | lunaerp-baseinfo-supplier.sql | 2026-09-19 | ✅ | |
| 7 | lunaerp-production-greige-order.sql | 2026-09-19 | ⚠️ | 缺列：`greige_order.color_instruction_id / color_instruction_no / production_mode` |
| 8 | lunaerp-production-dyeing-order.sql | 2026-09-19 | ⚠️ | 缺列：`dyeing_order.color_instruction_id / color_instruction_no` |
| 9 | lunaerp-production-yarn-master.sql | 2026-09-20 | ✅ | |
| 10 | lunaerp-production-yarn-order.sql | 2026-09-20 | ✅ | |
| 11 | lunaerp-production-yarn-recipe-bom.sql | 2026-09-20 | ✅ | |
| 12 | lunaerp-production-inspection-order.sql | 2026-09-21 | ✅ | |
| 13 | lunaerp-stock.sql | 2026-09-24 | ⚠️ | 缺列：`stock_in.inspection_order_id/no`、`stock_in_item.inspection_order_item_id / item_type / piece_no / weaving_ticket_id / yarn_spec_id / yarn_spec_name`、`stock_ledger.item_type / yarn_spec_id / yarn_spec_name` |
| 14 | lunaerp-production-weaving.sql | 2026-09-26 | ❌ | 缺表：`weaving_order / weaving_ticket / loom / machine_task / yarn_requisition(_item)`；2026-10-15 修复角色授权缺 id 的报错（已按 §一 幂等约定改用序列取号），**待重新执行** |
| 15 | lunaerp-production-iot.sql | 2026-09-26 | ❌ | 缺表：`machine_realtime / device_binding / vendor_credential` |
| 16 | lunaerp-production-inspection-confirmation.sql | 2026-09-27 | ✅ | |
| 17 | lunaerp-business-shipment.sql | 2026-09-27 | ✅ | |
| 18 | lunaerp-production-progress-feedback.sql | 2026-09-28 | ✅ | |
| 19 | lunaerp-production-greige-dispatch-record.sql | 2026-09-29 | ✅ | |
| 20 | lunaerp-production-progress.sql | 2026-09-29 | ✅ | |
| 21 | lunaerp-sample-analysis.sql | 2026-10-04 | ❌ | 缺表：`sample_analysis(_yarn_spec / _yarn_arrange / _supplier_quote)` |
| 22 | lunaerp-data-permission-wiring.sql | 2026-10-09 | ❌ | 4 个 `dept_id` / 业务员列全部未加（customer / sales_contract / shipment） |
| 23 | lunaerp-common-foundation.sql | 2026-10-09 | ✅ | 备注：`system_notify_template` 行数为 0（如需要站内信模板需补种子）；首次执行时角色授权 INSERT 曾因缺 id 报错被跳过，**2026-10-15 已修复，重跑本文件可补齐**布票打印（10300）与回收站（10900-10903）授权 |
| 24 | lunaerp-default-package-seed.sql | 2026-10-09 | ✅ | |
| 25 | lunaerp-approval.sql | 2026-10-09 | ❌ | 缺表：`approval_config(_node) / approval_instance / approval_task / approval_record` |
| 26 | lunaerp-menu-ia-rename.sql | 2026-10-09 | ✅ | |
| 27 | lunaerp-data-permission-fixture.sql | 2026-10-10 | ✅ | 备注：fixture 已跑（customer×2 / contract×1）；`customer_contact`、`shipment` 当前 0 行 |
| 28 | lunaerp-business-export-doc.sql | 2026-10-10 | ❌ | 缺表：`export_doc(_item) / export_seller_config`；缺列：`customer.address_en / name_en`、`sales_contract.incoterm / payment_term / port_of_loading / port_of_discharge` |
| 29 | lunaerp-business-cost-card-quotation.sql | 2026-10-10 | ❌ | 缺表：`cost_card(_fee / _yarn) / quotation(_item) / production_yarn_price`；缺列：`customer.default_margin_rate`、`sales_contract.source_quotation_id/no`、`recipe_bom_item` 5 个工艺列 |
| 30 | lunaerp-production-color-instruction.sql | 2026-10-13 | ❌ | 缺表：`color_instruction(_color)` |
| 31 | lunaerp-production-release-production.sql | 2026-10-14 | ⚠️ | 缺列：`sales_contract_item.released_qty`、`dyeing_color.contract_line_id/no`、`greige_order_color.contract_line_id/no` |
| 32 | lunaerp-production-scheduling-material-check.sql | 2026-10-14 | ✅ | |
| 33 | lunaerp-stock-in-yarn-order-ref.sql | 2026-10-15 | ❌ | 缺列：`stock_in.yarn_order_id / yarn_order_no` |
| 34 | lunaerp-finance-vendor-recon.sql | 2026-10-15 | ❌ | 缺表：`finance_supplier_recon(_item)` |
| 35 | lunaerp-finance-payment.sql | 2026-10-15 | ❌ | 缺表：`finance_payment(_apply / _ref)` |

### 状态汇总

- ✅ 已执行：**18 个**
- ⚠️ 部分执行：**6 个**（#4 #5 #7 #8 #13 #31 —— 均为文件后期追加的 `ALTER ADD COLUMN` 未应用）
- ❌ 未执行：**11 个**（#2 #14 #15 #21 #22 #25 #28 #29 #30 #33 #34 #35）

> 注意：⚠️ 与 ❌ 的缺失增量若对应功能已上线，说明运行中的应用可能正在用「缺列/缺表」的库——优先核对 #4 #5 #13（客户/合同/库存）与 #22（数据权限 dept_id 接线）。

---

## 三、如何重新核对状态

```sql
-- 1. 已有表清单
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public';
-- 2. 某表是否已有某列（例：客户表的 dept_id）
SELECT column_name FROM information_schema.columns
WHERE table_schema = 'public' AND table_name = 'lunaerp_baseinfo_customer';
-- 3. 种子是否入过库（例：菜单数）
SELECT count(*) FROM system_menu;
```

将各 SQL 文件中的 `CREATE TABLE / ADD COLUMN / INSERT INTO` 对象与以上三项比对即可。核对完成后更新本文档顶部的「状态核对时间」与状态表。
