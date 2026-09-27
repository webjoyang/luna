# 01: 质检确认 SQL 基建（1 表 DDL + 2 字典 + 挡车工岗位 + 10270 菜单）

**What to build:** 执行一份幂等 SQL 后，质检确认表 `lunaerp_production_inspection_confirmation`（含主键序列、列注释、5 个普通索引）就位；系统字典管理出现质检确认状态字典（PENDING 待确认 / CONFIRMED 已认定 / REVOKED 已撤销）与责任类型字典（EQUIPMENT 设备故障（免扣）/ OPERATOR 操作失误（扣款）/ WAIVED 免扣）；岗位管理出现编码 `LUNA_WORKER` 的"挡车工"岗位；"生产管理"目录下出现"质检确认"菜单及手工登记/生成/责任认定/批量打印/工资扣款汇总五个按钮权限。脚本可重复执行。

**Blocked by:** None (can start immediately)

**Status:** closed

- [x] 质检确认表 DDL 落库，字段与 spec 数据模型完全一致（认定链 chain_key、来源 source、状态 status、来源检验单 id/code、来源录检明细 id、卷快照组、责任/挡车工组、认定人时间备注、撤销人时间），继承 TenantBaseDO 标准字段，含主键 DO 块判存与全列注释
- [x] 序列 `lunaerp_production_inspection_confirmation_seq` 就位
- [x] 5 个普通索引就位：(tenant_id,status)、(tenant_id,chain_key)、(tenant_id,inspection_order_id,inspection_order_item_id)、(tenant_id,create_time)、(tenant_id,confirmed_at)
- [x] 字典 2236 `lunaerp_inspection_confirmation_status`（3 项）+ 2237 `lunaerp_inspection_responsibility`（3 项）及 dict_data 初始化（dict_type/dict_data id 段位避让已用值，脚本内先查后插或固定空位）
- [x] `system_post` 幂等 seed：code=LUNA_WORKER、名称"挡车工"、启用；按 code 存在则跳过/更新名称，不固定 id
- [x] 菜单 10270（type=2，parent=10200，path `inspection-confirmation`，component `lunaerp-elmui/production/inspection-confirmation/index`，componentName `LunaerpElmuiProductionInspectionConfirmation`，承载 :query）+ 10271 手工登记(:create)/10272 生成质检确认(:generate)/10273 责任认定(:confirm)/10274 批量打印(:print)/10275 工资扣款汇总(:payroll) 五个 type=3 按钮
- [x] 事务内先 DELETE 再 INSERT，整脚本可重复执行
- [x] 前端 `dict.ts` 追加两个字典类型常量

## Comments

- 创建 SQL 文件 `luna-srv/sql/postgresql/lunaerp-production-inspection-confirmation.sql`：
  - 质检确认表 `lunaerp_production_inspection_confirmation`（含 34 个字段、主键 DO 块判存约束、全列注释）
  - 序列 `lunaerp_production_inspection_confirmation_seq`
  - 5 个普通索引：`(tenant_id, status)`、`(tenant_id, chain_key)`、`(tenant_id, inspection_order_id, inspection_order_item_id)`、`(tenant_id, create_time)`、`(tenant_id, confirmed_at)`
  - 字典类型 2236 `lunaerp_inspection_confirmation_status`（字典项 3603-3605：PENDING/CONFIRMED/REVOKED）与 2237 `lunaerp_inspection_responsibility`（字典项 3606-3608：EQUIPMENT/OPERATOR/WAIVED），并在脚本头部加 DO 块判存转移以避让 2236
  - 更新 `lunaerp-stock.sql` 的 `lunaerp_stock_item_type` 为 2245，确保全局无冲突
  - 挡车工岗位 seed：code=`LUNA_WORKER`、名称"挡车工"，DO 块判存按 code 幂等更新/插入
  - 菜单 10270 质检确认（parent=10200，sort=7）与 10271-10275 五个按钮（create/generate/confirm/print/payroll），附带管理员角色权限关联
- 前端 `luna-ui/src/utils/dict.ts`：追加 `LUNAERP_INSPECTION_CONFIRMATION_STATUS` 与 `LUNAERP_INSPECTION_RESPONSIBILITY` 常量定义

