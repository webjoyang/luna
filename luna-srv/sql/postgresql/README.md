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
- **菜单 DELETE 覆盖 INSERT**：`system_menu` / `system_role_menu` 先 DELETE 再 INSERT 时，DELETE 的 id 清单必须覆盖该段全部 INSERT 的 id（共享父目录除外——用 `ON CONFLICT (id) DO NOTHING` 或 `WHERE NOT EXISTS` 保证幂等），否则重复执行报主键冲突
- 新文件创建后，**执行完毕立即更新本文档状态表**

---

## 二、执行状态表（2026-10-15 全量执行一轮后复扫）

图例：✅ 已执行 ｜ ⚠️ 部分执行（早期版本已跑，后续增量未应用）｜ ❌ 未执行

| # | 文件 | 首次提交 | 状态 | 缺失对象 / 备注 |
|---|------|----------|------|----------------|
| 1 | ruoyi-vue-pro.sql | 2026-09-13 | ✅ | 仅 demo 示例表未建（`lunasrv_demo01/02/03_*`），正常 |
| 2 | quartz.sql | 2026-09-13 | ❌ | 11 张 `qrtz_*` 表全部不存在；local 已禁用 Quartz，按需执行 |
| 3 | lunaerp-baseinfo-product.sql | 2026-09-13 | ✅ | |
| 4 | lunaerp-baseinfo-customer.sql | 2026-09-18 | ✅ | |
| 5 | lunaerp-business-sales-contract.sql | 2026-09-18 | ✅ | |
| 6 | lunaerp-baseinfo-supplier.sql | 2026-09-19 | ✅ | |
| 7 | lunaerp-production-greige-order.sql | 2026-09-19 | ✅ | |
| 8 | lunaerp-production-dyeing-order.sql | 2026-09-19 | ✅ | |
| 9 | lunaerp-production-yarn-master.sql | 2026-09-20 | ✅ | |
| 10 | lunaerp-production-yarn-order.sql | 2026-09-20 | ✅ | |
| 11 | lunaerp-production-yarn-recipe-bom.sql | 2026-09-20 | ✅ | |
| 12 | lunaerp-production-inspection-order.sql | 2026-09-21 | ✅ | |
| 13 | lunaerp-stock.sql | 2026-09-24 | ✅ | |
| 14 | lunaerp-production-weaving.sql | 2026-09-26 | ✅ | 2026-10-15 重跑成功（本轮曾修复授权缺 id 与菜单 DELETE 漏 10299 两处报错） |
| 15 | lunaerp-production-iot.sql | 2026-09-26 | ✅ | |
| 16 | lunaerp-production-inspection-confirmation.sql | 2026-09-27 | ✅ | |
| 17 | lunaerp-business-shipment.sql | 2026-09-27 | ✅ | |
| 18 | lunaerp-production-progress-feedback.sql | 2026-09-28 | ✅ | |
| 19 | lunaerp-production-greige-dispatch-record.sql | 2026-09-29 | ✅ | |
| 20 | lunaerp-production-progress.sql | 2026-09-29 | ✅ | |
| 21 | lunaerp-sample-analysis.sql | 2026-10-04 | ✅ | |
| 22 | lunaerp-data-permission-wiring.sql | 2026-10-09 | ✅ | |
| 23 | lunaerp-common-foundation.sql | 2026-10-09 | ✅ | `system_notify_template` 行数为 0（如需要站内信模板需补种子）；布票打印（10300）与回收站（10900-10903）授权复扫确认已落库 |
| 24 | lunaerp-default-package-seed.sql | 2026-10-09 | ✅ | |
| 25 | lunaerp-approval.sql | 2026-10-09 | ✅ | |
| 26 | lunaerp-menu-ia-rename.sql | 2026-10-09 | ✅ | |
| 27 | lunaerp-data-permission-fixture.sql | 2026-10-10 | ✅ | 备注：fixture 已跑（customer×2 / contract×1）；`customer_contact`、`shipment` 当前 0 行 |
| 28 | lunaerp-business-export-doc.sql | 2026-10-10 | ✅ | |
| 29 | lunaerp-business-cost-card-quotation.sql | 2026-10-10 | ✅ | |
| 30 | lunaerp-production-color-instruction.sql | 2026-10-13 | ⚠️ | 表/菜单/字典已落库；复扫发现**文件缺角色授权段**（菜单 10230-10235 授权 0 行），2026-10-15 已补 4.1 节授权段，**重跑本文件可补齐**（幂等） |
| 31 | lunaerp-production-release-production.sql | 2026-10-14 | ✅ | |
| 32 | lunaerp-production-scheduling-material-check.sql | 2026-10-14 | ✅ | 复扫确认菜单 10510/10520 与授权均已落库（各 2 行） |
| 33 | lunaerp-stock-in-yarn-order-ref.sql | 2026-10-15 | ✅ | |
| 34 | lunaerp-finance-vendor-recon.sql | 2026-10-15 | ✅ | |
| 35 | lunaerp-finance-payment.sql | 2026-10-15 | ✅ | |
| 36 | lunaerp-finance-customer-recon.sql | 2026-10-16 | ✅ | 客户对账、经营分录台账、资金账户、发票申请、汇率主档；共享表 IN 扩列、客户扩列、字典 2272-2278、菜单 10720/10770/10780 与审批流 CUSTOMER_RECEIPT |

### 状态汇总（2026-10-16 复扫）

- ✅ 已执行：**34 个**
- ⚠️ 部分执行：**1 个**（#30 color-instruction —— 授权段已补入文件，**重跑一次即全绿**）
- ❌ 未执行：**1 个**（#2 quartz —— local 已禁用，按需）
- 授权抽查：`system_role_menu` 898 → 1050 行，抽样 9 个关键菜单（织造/库存/付款/审批/出口/回收站等）授权均各 2 行（super_admin + admin）

> 注意：⚠️ 与 ❌ 的缺失增量若对应功能已上线，说明运行中的应用可能正在读写缺对象的数据表——当前仅剩 #30 重跑一次授权段。

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
