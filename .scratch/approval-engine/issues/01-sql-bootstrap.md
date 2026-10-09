# 01: 审批引擎 SQL 基建（5 表 DDL + 菜单 + 字典）

**What to build:** 执行 `lunaerp-approval.sql` 后，审批流程定义、流程节点、审批实例、审批任务、审批操作记录 5 张表连同序列与索引就位；系统管理中出现「审批中心」一级目录及待我审批、我已审批、我发起的、审批链配置 4 个子菜单与按钮权限；单据类型、节点类型、审批人来源、实例状态、任务动作、记录动作 6 个字典可在字典管理查看。脚本可重复执行。

**Blocked by:** None (can start immediately)

**Status:** ready-for-human

- [x] 5 张表 DDL 全部落库（`lunaerp_approval_config` / `_config_node` / `_instance` / `_task` / `_record`），均含标准 TenantBaseDO 字段、DO 块主键判存、全列注释
- [x] 5 个主键序列就位
- [x] 唯一索引含软删行：`(tenant_id, biz_type)` 上 config 的启用态唯一（同 biz_type 仅一条 ENABLED）；其余辅助索引按 spec 就位（instance 按 biz、submitter、status；task 按 approver+status、instance_id；record 按 instance+operate_time、operator+action）
- [x] 预留列就位：`instance.flowable_proc_inst_id`、`task.flowable_task_id` 可空，`task.version` 乐观锁 int
- [x] 「审批中心」一级目录（sort 109，path `/approval`）+ 4 子菜单（待我审批 10/我已审批 20/我发起的 30/审批链配置 40）及 `lunaerp:approval:task:query`、`lunaerp:approval:config:*` 按钮权限注册
- [x] 6 个字典初始化：`lunaerp_approval_biz_type`（sales_contract/payment/expense/payroll/sales_return）、节点类型 SEQUENTIAL/OR_SIGN/AND_SIGN、审批人来源 USER/POST/ROLE/DEPT_LEADER/SUBMITTER_LEADER、实例状态 APPROVING/APPROVED/REJECTED/CANCELLED、任务动作 DONE/REJECTED/TRANSFERRED、记录动作 APPROVE/REJECT/TRANSFER/WITHDRAW/SUBMIT
- [x] 脚本幂等可重复执行（事务内 DELETE 再 INSERT）
