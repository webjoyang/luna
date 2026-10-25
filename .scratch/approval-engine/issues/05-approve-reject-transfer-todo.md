# 05: 审批操作（通过/驳回/转交）+ 待我审批（审批人视角）

**What to build:** 审批人在「审批中心 → 待我审批」看到分配给自己的待办任务，进入审批工作台查看单据详情（只读跳转原单据详情页）与审批进度条，填写意见后点「通过」或「驳回」。逐级节点通过后推进下一节点；或签节点任一通过即过、其他同节点待办自动关闭；会签节点全部通过才过。任一驳回即终止实例回到发起人。审批人可把待办转交给他人。

**Blocked by:** 04 (发起审批与撤回；依赖已有 instance/task)

**Status:** ready-for-human

- [x] `GET /lunaerp/approval/task/todo-page`：按当前用户 approver_id + status=PENDING 分页，返回单据类型、单号、发起人、发起时间、超时时长
- [x] `PUT /lunaerp/approval/task/{id}/approve`（body: comment）：
  - 仅当前 task 的 approver_id=本人可操作，否则报 `1_030_030_005`
  - task 必须 PENDING，否则报 `1_030_030_004`
  - 逐级/或签：当前 task 置 DONE+DONE，本节点通过；或签同节点其他 PENDING task 置 DONE+action=null（自动关闭）
  - 会签：当前 task 置 DONE+DONE，检查同节点是否全部 DONE，是则本节点通过
  - 节点通过后：有下一节点则创建下一节点 task(s) 并通知；无下一节点则 instance.status=APPROVED（触发副作用事件，见 07）
  - 写 APPROVE 记录
- [x] `PUT /lunaerp/approval/task/{id}/reject`（body: comment 必填）：
  - comment 为空报 `1_030_030_006`
  - 当前 task 置 DONE+REJECTED，同节点其他 PENDING task 置 DONE+action=null
  - instance.status=REJECTED，业务单据 `approval_status=REJECTED`（回到可编辑）
  - 写 REJECT 记录
- [x] `PUT /lunaerp/approval/task/{id}/transfer`（body: toUserId, comment）：
  - toUserId=本人报 `1_030_030_009`
  - 当前 task 置 DONE+TRANSFERRED，新建 task 给接收人（同 node_sort、PENDING），通知接收人
  - 写 TRANSFER 记录（transfer_to_id）
- [x] `task.version` 乐观锁：会签并发通过时版本不匹配抛异常，防覆盖
- [x] 终态守卫：instance 非 APPROVING 时不允许任何 task 操作，报 `1_030_030_004`
- [x] 前端：待我审批列表 + 审批工作台（单据详情 iframe/跳转 + 进度条展示已通过/当前/待审节点 + 通过/驳回/转交弹窗）
- [x] 集成测试：逐级通过、或签任一通过、会签全部通过、驳回终止、转交、会签并发覆盖各一条

## Comments
- **后端架构与服务落地**：
  - 新增 `ApprovalTaskController` 与 `ApprovalTaskService` / `ApprovalTaskServiceImpl`，遵循分层与 SRP 架构，权限前缀与 SQL 菜单注册 `lunaerp:approval:task:query` 严格对齐。
  - 在 `LunasrvMybatisAutoConfiguration` 统一装配 `OptimisticLockerInnerInterceptor`，激活 MyBatis-Plus `@Version` 乐观锁机制；`ApprovalTaskDO.version` 并发更新冲突行数为 0 时拦截并抛出 `1_030_030_004`。
  - 终态守卫：实例非 `APPROVING` 状态下（如已 `APPROVED`/`REJECTED`/`CANCELLED`）执行任何通过、驳回、转交接口均拦截抛出 `1_030_030_004`。
  - 权限与状态守卫：仅当前 task 审批人本人可操作（非本人抛 `1_030_030_005`），非 `PENDING` 任务重复操作拦截抛 `1_030_030_004`；转交本人拦截抛 `1_030_030_009`；驳回意见空拦截抛 `1_030_030_006`。
  - 节点推进与副作用接线：逐级直接过、或签任一过且关闭同节点其余待办、会签全员通过才推进；有下一节点生成下一节点任务并站内信通知；无下一节点更新实例 `APPROVED`、单据 `approval_status=APPROVED` 并发布 `ApprovalCompletedEvent`（无缝衔接 Issue 07 事务内副作用）。
- **前端工作台与组件实现**：
  - 在 `luna-ui/src/api/approval/task/` 封装待我审批分页、通过、驳回、转交全部接口与 TypeScript 类型。
  - 在 `luna-ui/src/views/approval/todo/` 实现待我审批列表与审批工作台一体化视图：
    - 列表态：单据类型/单号搜索，展示单据类型、单号、发起人、节点名称/类型、到达时间、超时时长标签与操作按钮。
    - 工作台态：顶部操作栏（通过/驳回/转交）、节点流程进度条（`el-steps` 计算已通过/当前审批中/待审批/已驳回状态）、业务单据详情摘要与跳转原单据详情页（如销售合同详情）、审批操作历史时间线（`el-timeline` 回显操作人、动作、意见与转交人）。
    - 弹窗交互：通过弹窗（意见选填）、驳回弹窗（意见必填校验）、转交弹窗（接收人下拉过滤本人并校验）。
- **单测与集成测试防护**：
  - 后端：`ApprovalTaskServiceImplTest`（14 个用例）与 `ApprovalTaskControllerTest`（4 个用例）全覆盖，全模块审批引擎 59 个单元测试全部 PASS。
  - 前端：`approval-todo.spec.ts`（12 个用例）覆盖快照节点状态推导、业务跳转路由、超时标签判定、表单校验与 API 客户端调用，全部 PASS。
