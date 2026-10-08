# 04: 调岗归属同步切面（ADR-0046）

**What to build:** lunaerp 模块新增 `@Aspect` 环绕 system 用户更新（bean 名切点，不反向依赖 system 模块内部类），比较 update 前后 `dept_id`；未变放行，变化则在同一事务内对 4 张注册表执行 `UPDATE <表> SET dept_id=:newDeptId WHERE tenant_id=:tid AND salesperson_user_id=:uid AND deleted=0`（走 `(tenant_id, salesperson_user_id)` 索引，毫秒级）。效果对齐 16 号 §6.2 换岗用例：a 调 D2 后 D1 主管立即不见 a 旧单、D2 主管立即可见、a 本人 SELF 档始终见自己单。

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] lunaerp 模块新增 `@Aspect`，bean 名切点环绕 `AdminUserServiceImpl#updateUser`（签名以施工时源码为准），不反向依赖 system 模块内部类
- [ ] 比较 update 前后 `dept_id`：未变化 → 直接放行；发生变化 → 在同一事务内执行 4 表刷列
- [ ] 刷列 SQL：`UPDATE <表> SET dept_id = :newDeptId WHERE tenant_id = :tenantId AND salesperson_user_id = :userId AND deleted = 0`，对 4 张注册表（客户/合同/发货 + 报价待 05 票建表后补挂，本票只覆盖已存的 3 张）
- [ ] 切面通过 `@DataPermission(enable=false)` 上下文（或 lunaerp 侧 Mapper 显式关闭方法）执行刷列，绕过数据权限规则；租户条件照加
- [ ] owner 列（`salesperson_user_id`）不变，故 SELF 档不受调岗影响（a 本人始终见自己单）
- [ ] 同事务回滚造一次失败：刷列与用户更新在同一事务，失败回滚不半刷（造数：触发一个会失败的 update，断言 4 表 `dept_id` 未变）
- [ ] 迁移脚本附一条幂等"归属校正 SQL"（注释形态随脚本交付，默认注释）：按 owner 当前部门全量重刷 `dept_id`，供排障/补刷使用（`IS NULL` 不适用此条，此条为显式校正工具）
- [ ] 用户删除（停用不删）不在切面范围
- [ ] 万级数据量下按 `(tenant_id, salesperson_user_id)` 索引更新为毫秒级（perf 由 08 实证）
- [ ] §9.2 用例 5 通过：a 调 D2 后重查——m 立即不见 a 的单、D2 主管立即可见、a 本人仍见；同事务回滚造失败验证不半刷
