# 01: 发货域 SQL 基建（3 表 DDL + 10120 菜单 + 2 字典）

**What to build:** 执行发货建表脚本后，发货单主表、发货明细、卷清单 3 张表连同序列与索引就位；"业务管理"目录下出现"发货管理"菜单（10120）及创建/修改/删除/审核/打印五个按钮权限（10121-10125）；发货状态（DRAFT/COMPLETED/CANCELLED）、签收状态（PENDING/SIGNED/REFUSED）2 个字典（2245/2246）可在字典管理查看。脚本可重复执行。

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] 3 张表 DDL 全部落库（`lunaerp_business_shipment` / `lunaerp_business_shipment_item` / `lunaerp_business_shipment_roll`，含主键 DO 块判存、列注释、序列、标准 TenantBaseDO 字段）
- [x] 唯一索引就位：发货单号 `(tenant_id, shipment_no)` 含软删行
- [x] 辅助索引就位：主表 status/customer_id/sales_contract_id/warehouse_id/shipping_date/sign_status；明细 shipment_id/product_id/source_stock_in_item_id；卷清单 shipment_id/shipment_item_id
- [x] 10120 发货管理菜单（挂 10100，sort 2，query 权限 + 组件路径）+ 10121-10125 按钮权限注册
- [x] 字典 2245（lunaerp_shipment_status）/ 2246（lunaerp_shipment_sign_status）及 dict_data 初始化，标签颜色与 spec 一致
- [x] 脚本可重复执行（事务内先 DELETE 再 INSERT，幂等），仅做语法自查、不直连共享库
- [x] 明细单位复用 lunaerp_stock_in_unit、币种复用 lunaerp_sales_contract_currency；承运商与流水 txn_type 不入字典

## Comments

- 创建 SQL 文件 `luna-srv/sql/postgresql/lunaerp-business-shipment.sql`：
  - 发货单主表 `lunaerp_business_shipment`（31 个字段，含 TenantBaseDO 标准列、主键 DO 块判存约束 `pk_lb_shipment`、全列注释）
  - 发货明细表 `lunaerp_business_shipment_item`（21 个字段，含主键约束 `pk_lb_shipment_item`、全列注释）
  - 发货卷清单表 `lunaerp_business_shipment_roll`（13 个字段，含主键约束 `pk_lb_shipment_roll`、全列注释）
  - 主键序列：`lunaerp_business_shipment_seq`、`lunaerp_business_shipment_item_seq`、`lunaerp_business_shipment_roll_seq`
  - 唯一索引：`uk_lb_shipment_tenant_no` ON `(tenant_id, shipment_no)`（含软删行）
  - 辅助索引：主表 6 个索引（status, customer_id, sales_contract_id, warehouse_id, shipping_date, sign_status）；明细 3 个索引（shipment_id, product_id, source_stock_in_item_id）；卷清单 2 个索引（shipment_id, shipment_item_id）
  - 菜单注册：10120 发货管理（parent=10100，sort=2，path=shipment，component=lunaerp-elmui/business/shipment/index，permission=`lunaerp:business:shipment:query`）+ 10121-10125 五个按钮权限（create/update/delete/audit/print）
  - 字典注册：2245 发货单状态 `lunaerp_shipment_status`（DRAFT warning / COMPLETED success / CANCELLED danger，dict_data 3690-3692）+ 2246 发货单签收状态 `lunaerp_shipment_sign_status`（PENDING info / SIGNED success / REFUSED danger，dict_data 3693-3695）
  - 幂等性：事务内先 DELETE 再 INSERT，头部含 2245 避让平移至 2247 的防重幂等 DO 块
- 全局避让调整：
  - 更新 `lunaerp-stock.sql` 将 `lunaerp_stock_item_type` 统一配置为 2247，避免执行时占用发货域 2245
- 权威文档与错误码：
  - `CONTEXT.md`：增加"业务管理：发货管理"术语概念；补充发货管理字典说明；注册 `1-030-016-xxx` 发货单错误码段位
  - `ErrorCodeConstants.java`：同步落地 016 段错误码常量定义
- 前端字典常量：
  - `luna-ui/src/utils/dict.ts`：追加 `LUNAERP_SHIPMENT_STATUS` 与 `LUNAERP_SHIPMENT_SIGN_STATUS` 定义

