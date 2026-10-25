# 03: 汇率主档 CRUD（前后端）

**What to build:** 汇率主档新增/查询/停用（无删除，失效即停用），同租户同币种同生效日唯一、本位币（RMB）禁止配置、缺生效汇率报 EXCHANGE_RATE_NOT_FOUND；前端 `lunaerp-elmui/finance/exchange-rate/` 页面，菜单 10780。

**Blocked by:** 01 SQL 基建 + 错误码常量 + 客户档案+5列

**Status:** closed

- [x] `controller.finance.exchangerate` + `service.finance.exchangerate` + DAL：`/exchange-rate` page/get/create/update（停用，无删除）
- [x] 唯一索引 (tenant_id, currency, effective_date) 校验，撞号报 EXCHANGE_RATE_NO_DUPLICATE
- [x] RMB 不建行，代码内置 rate=1；对 RMB 配置报 EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN
- [x] `getRate(currency, bizDate)` 取价：生效日≤业务日的最新一条 ENABLE（供 02 号内核复用）
- [x] 错误码：200 EXCHANGE_RATE_NOT_FOUND / 201 EXCHANGE_RATE_NO_DUPLICATE / 202 EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN
- [x] 前端页面 `lunaerp-elmui/finance/exchange-rate/index`：列表（币种/生效日/汇率/状态）、新增（币种+生效日+汇率）、停用；金额/汇率字段展示规范
- [x] 菜单 10780 汇率（sort 8, parent 10700, path exchange-rate）权限 `:query` + 10781 :create / 10782 :update
- [x] 测试：生效日取价（最新≤业务日、停用剔除、RMB=1）、同币种同生效日唯一、本位币禁止、缺汇率报错
