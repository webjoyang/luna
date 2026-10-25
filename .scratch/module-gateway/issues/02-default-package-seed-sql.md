# 02: 默认「核心包」套餐种子数据 SQL

**What to build:** 执行种子 SQL 后，`system_tenant_package` 存在一条「核心包」套餐，`menuIds` 含基础信息(10000) / 销售(10100) / 生产(10200) / 库存(10300) / 样品(10600) / 财务(10700) / 审批中心(10800) / 系统管理 + 首页（芋道内置）。脚本幂等可重复执行。套餐管理页列表可见「核心包」并支持新租户默认绑定。

> 说明：10700 由 01-finance-domain-adr 定义、随 W3 各财务模块建表脚本落地；10800 由 02-approval-engine 的 01-sql-bootstrap 落地。本 ticket 的 SQL 只存储 `menuIds` ID 集合，10700/10800 未落地时运行时 `MenuServiceImpl` 自动过滤悬挂 ID，不影响 SQL 本身落地与回显。新租户默认绑定核心包的配置随本 SQL 一并就位。

**Blocked by:** None（可立即开始；`menuIds` 仅存 ID，悬挂菜单由运行时过滤）

**Status:** completed

- [x] 「核心包」套餐入库 `system_tenant_package`，`menuIds` 含全部 9 项（基础信息 / 销售 / 生产 / 库存 / 样品 / 财务 / 审批中心 / 系统管理 / 首页）
- [x] 种子脚本幂等可重复执行（事务内 UPSERT）
- [x] 套餐管理页（`views/system/tenantPackage/index.vue`）列表可见「核心包」
- [x] 套餐菜单树勾选回显 9 个一级目录，已落地菜单正常显示，未落地者（10700/10800 未落地时）被自动过滤不报错
- [x] 新租户默认绑定核心包（`system_tenant.package_id` 指向核心包 id）
