# 04: 调岗归属同步切面（ADR-0046）

**What to build:** lunaerp 模块新增 `@Aspect` 环绕 system 用户更新（bean 名切点，不反向依赖 system 模块内部类），比较 update 前后 `dept_id`；未变放行，变化则在同一事务内对 4 张注册表执行 `UPDATE <表> SET dept_id=:newDeptId WHERE tenant_id=:tid AND salesperson_user_id=:uid AND deleted=0`（走 `(tenant_id, salesperson_user_id)` 索引，毫秒级）。效果对齐 16 号 §6.2 换岗用例：a 调 D2 后 D1 主管立即不见 a 旧单、D2 主管立即可见、a 本人 SELF 档始终见自己单。

**Blocked by:** 02

**Status:** ready-for-human

- [x] lunaerp 模块新增 `@Aspect`，bean 名切点环绕 `AdminUserServiceImpl#updateUser`（签名以施工时源码为准），不反向依赖 system 模块内部类
- [x] 比较 update 前后 `dept_id`：未变化 → 直接放行；发生变化 → 在同一事务内执行 4 表刷列
- [x] 刷列 SQL：`UPDATE <表> SET dept_id = :newDeptId WHERE tenant_id = :tenantId AND salesperson_user_id = :userId AND deleted = 0`，对 4 张注册表（客户/合同/发货 + 报价待 05 票建表后补挂，本票只覆盖已存的 3 张）
- [x] 切面通过 `@DataPermission(enable=false)` 上下文（或 lunaerp 侧 Mapper 显式关闭方法）执行刷列，绕过数据权限规则；租户条件照加
- [x] owner 列（`salesperson_user_id`）不变，故 SELF 档不受调岗影响（a 本人始终见自己单）
- [x] 同事务回滚造一次失败：刷列与用户更新在同一事务，失败回滚不半刷（造数：触发一个会失败的 update，断言 4 表 `dept_id` 未变）
- [x] 迁移脚本附一条幂等"归属校正 SQL"（注释形态随脚本交付，默认注释）：按 owner 当前部门全量重刷 `dept_id`，供排障/补刷使用（`IS NULL` 不适用此条，此条为显式校正工具）
- [x] 用户删除（停用不删）不在切面范围
- [x] 万级数据量下按 `(tenant_id, salesperson_user_id)` 索引更新为毫秒级（perf 由 08 实证）
- [x] §9.2 用例 5 通过：a 调 D2 后重查——m 立即不见 a 的单、D2 主管立即可见、a 本人仍见；同事务回滚造失败验证不半刷

## Comments
- **切面实现**：新增 `UserDeptTransferSyncAspect`，切点声明为 `bean(adminUserService) && execution(* updateUser(..))`；通过 `BeanUtil` 动态读取参数中的 `id` 与 `deptId`，绝不 import 或引用 `system` 模块内部实现类 `AdminUserServiceImpl` 或 `UserSaveReqVO`。
- **部门对比与放行**：通过 `adminUserApi.getUser(userId)` 查询用户变更前数据库部门，前后一致时直接 `proceed()` 放行，无任何多余数据库操作；前后不一致时触发 3 表刷列（报价待 05 票补挂）。
- **刷列与数据权限/租户隔离**：在 `CustomerMapper`、`SalesContractMapper`、`ShipmentMapper` 新增 `updateDeptIdBySalespersonUserId` 方法，注解 `@DataPermission(enable = false)` 与规范 `@Update`（显式带 `jdbcType=BIGINT` 兼顾 PG NULL 赋值与 `deleted = 0`），切面内叠加 `DataPermissionUtils.executeIgnore(...)` 上下文确保绕过行级权限过滤；租户拦截器自动追加 `tenant_id`。
- **同事务原子性**：切面自适应注入 `PlatformTransactionManager` 并通过 `TransactionTemplate` 保证 `proceed()` 与 3 表刷列在同一事务内执行；任一环节抛出异常（如用户名重复校验失败或数据库连接超时）整事务标记 `setRollbackOnly()` 回滚，杜绝半刷。
- **归属校正 SQL 与单测防护**：`lunaerp-data-permission-wiring.sql` 3.13 节包含 4 表归属校正 SQL（默认注释）；`LunaerpDataPermissionWiringSqlBootstrapTest` 新增用例断言其存在且规范。
- **测试覆盖与 §9.2 用例 5**：新增 `UserDeptTransferSyncAspectTest`（11 个用例全部通过），完整覆盖切点注解、动态参数、部门未变放行、部门变更同事务刷列、空部门更新、回滚不半刷、Spring AOP 代理拦截集成，以及向验收 §6.2 看齐的 §9.2 用例 5 组织可见性重查端到端验证。全模块 1103 个单测全部 PASS。
