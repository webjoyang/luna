# 05: 审批操作（通过/驳回/转交）+ 待我审批（审批人视角）

**What to build:** 审批人在「审批中心 → 待我审批」看到分配给自己的待办任务，进入审批工作台查看单据详情（只读跳转原单据详情页）与审批进度条，填写意见后点「通过」或「驳回」。逐级节点通过后推进下一节点；或签节点任一通过即过、其他同节点待办自动关闭；会签节点全部通过才过。任一驳回即终止实例回到发起人。审批人可把待办转交给他人。

**Blocked by:** 04 (发起审批与撤回；依赖已有 instance/task)

**Status:** ready-for-agent

- [ ] `GET /lunaerp/approval/task/todo-page`：按当前用户 approver_id + status=PENDING 分页，返回单据类型、单号、发起人、发起时间、超时时长
- [ ] `PUT /lunaerp/approval/task/{id}/approve`（body: comment）：
  - 仅当前 task 的 approver_id=本人可操作，否则报 `1_030_030_005`
  - task 必须 PENDING，否则报 `1_030_030_004`
  - 逐级/或签：当前 task 置 DONE+DONE，本节点通过；或签同节点其他 PENDING task 置 DONE+action=null（自动关闭）
  - 会签：当前 task 置 DONE+DONE，检查同节点是否全部 DONE，是则本节点通过
  - 节点通过后：有下一节点则创建下一节点 task(s) 并通知；无下一节点则 instance.status=APPROVED（触发副作用事件，见 07）
  - 写 APPROVE 记录
- [ ] `PUT /lunaerp/approval/task/{id}/reject`（body: comment 必填）：
  - comment 为空报 `1_030_030_006`
  - 当前 task 置 DONE+REJECTED，同节点其他 PENDING task 置 DONE+action=null
  - instance.status=REJECTED，业务单据 `approval_status=REJECTED`（回到可编辑）
  - 写 REJECT 记录
- [ ] `PUT /lunaerp/approval/task/{id}/transfer`（body: toUserId, comment）：
  - toUserId=本人报 `1_030_030_009`
  - 当前 task 置 DONE+TRANSFERRED，新建 task 给接收人（同 node_sort、PENDING），通知接收人
  - 写 TRANSFER 记录（transfer_to_id）
- [ ] `task.version` 乐观锁：会签并发通过时版本不匹配抛异常，防覆盖
- [ ] 终态守卫：instance 非 APPROVING 时不允许任何 task 操作，报 `1_030_030_004`
- [ ] 前端：待我审批列表 + 审批工作台（单据详情 iframe/跳转 + 进度条展示已通过/当前/待审节点 + 通过/驳回/转交弹窗）
- [ ] 集成测试：逐级通过、或签任一通过、会签全部通过、驳回终止、转交、会签并发覆盖各一条
