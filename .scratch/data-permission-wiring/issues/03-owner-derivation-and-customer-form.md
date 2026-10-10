# 03: 服务端 `dept_id` 推导（create/update）+ 客户 VO owner 字段 + 客户表单归属业务员选择器

**What to build:** 客户/合同/发货在 create/update 时由服务端按最终归属人的 `system_users.dept_id` 实时推导 `dept_id` 落库，前端只提交 `salesperson_user_id`（客户 VO 新增该字段，缺省登录用户、可改、可清空），任何 VO 不暴露 `dept_id`。客户 Excel 导入归属操作人。前端客户新建/编辑页加「归属业务员」用户选择器；客户列表可按归属业务员筛选（非强制）。报价随 05 票同规则落地。

**Blocked by:** 01

**Status:** ready-for-human

- [x] create：`salesperson_user_id` 缺省 = 当前登录用户（前端可改）；owner 为空 → 两列皆空；owner 非空 → `dept_id` = owner 当前 `system_users.dept_id`，服务端推导、不信前端入参
- [x] update：归属人变更 → 同事务重算 `dept_id`（含清空归属人则两列清空）；归属人未变 → 不动 `dept_id`
- [x] 客户 `CustomerSaveReqVO` 新增 `salesperson_user_id` 字段；任何客户 VO 不暴露 `dept_id`，即使前端传入也后端忽略
- [x] 归属列不设 NOT NULL、不设外键（沿用全仓逻辑关联约定）；空归属行仅 ALL 档可见（在 02 验证）
- [x] 客户 Excel 导入（04 票模板）：导入行 owner = 执行导入的登录用户、`dept_id` = 其当前部门（服务端推导，与新建同路径）。已在 04 票 spec §3.2 落注
- [x] 前端 luna-ui：客户新建/编辑页加「归属业务员」用户选择组件（缺省当前登录用户、可改、可清空）
- [x] 前端 luna-ui（非强制）：客户列表按归属业务员筛选
- [x] 跨归属引用存在性校验现状不变（selectById 已被规则自动过滤）：合同/报价引用不可见客户 → `CUSTOMER_NOT_EXISTS`（1_030_004_000）；发货引用不可见合同 → `SHIPMENT_RELATED_REF_NOT_EXISTS`（1_030_016_003）。不新增任何错误码
- [x] 带单继承（§3.2）维持现状不动：合同建发货 owner 缺省带合同 owner（前端可改）、`dept_id` 服务端推导；客户改归属不回刷下游
- [x] 报价→合同/合同→发货的带单继承与报价建表 DDL 随 05 票同批落地，本票只覆盖客户/合同/发货 3 表与客户前端选择器

## Comments
- 服务端推导：在 `CustomerServiceImpl`、`SalesContractServiceImpl`、`ShipmentServiceImpl` 的新建与更新路径中，统一通过 `AdminUserApi.getUser(salespersonUserId).getDeptId()` 由服务端权威推导 `dept_id`。
- 清空支持：在 `CustomerDO`、`SalesContractDO` 的 `salespersonUserId` 与 `deptId` 上补充 `@TableField(updateStrategy = FieldStrategy.ALWAYS)`（ShipmentDO 已具备），确保清空归属人时 MyBatis Plus 正确将 DB 字段更新为 NULL。
- 客户 VO 契约：`CustomerSaveReqVO` 新增 `salespersonUserId`；`CustomerRespVO` 移除 `deptId`；`CustomerPageReqVO` 新增 `salespersonUserId` 筛选；`CustomerMapper` 补充对应查询条件。
- 前端实现：`BasicProfileTab.vue` 新增 `UserSelect` 组件（支持清空，新建缺省当前登录用户）；`customer/index.vue` 搜索栏新增业务员筛选。前端打包构建 `pnpm build:local` 校验通过。
- 单元测试：新增 `OwnerDerivationWiringTest`（15 个契约与行为测试用例全部通过），并联测 `CustomerServiceImplTest`、`SalesContractServiceImplTest`、`ShipmentServiceImplTest`、`LunaerpDataPermissionWiringTest`，共 129 个单测全部通过。
