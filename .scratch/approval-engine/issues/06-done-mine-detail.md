# 06: 我已审批 + 我发起的 + 实例详情

**What to build:** 用户在「审批中心 → 我已审批」查看自己操作过的历史审批记录（单据、操作类型、意见、时间）；在「我发起的」查看自己发起的审批实例及其当前进度（当前节点、状态）；点进实例详情可看到完整节点进度条与操作记录时间线。横向越权：非本人操作/发起的实例不可见。

**Blocked by:** 05 (审批操作；依赖已有操作记录与终态实例)

**Status:** ready-for-agent

- [ ] `GET /lunaerp/approval/task/done-page`：按当前用户 operator_id 从 `lunaerp_approval_record` 反查，分页返回操作过的实例（单据、操作类型、意见、时间）
- [ ] `GET /lunaerp/approval/instance/my-page`：按 submitter_id=当前用户分页，返回实例当前状态、当前节点
- [ ] `GET /lunaerp/approval/instance/{id}`：返回实例基本信息 + 节点进度（每节点状态：已通过/当前/待审）+ 操作记录时间线（操作人、动作、意见、时间、转交接收人）
- [ ] 越权过滤：done-page 仅本人操作记录、my-page 仅本人发起；instance 详情对非 submitter 且非任一 task approver 的用户返回 404（复用 `1_030_030_002` 或越权码）
- [ ] 前端：我已审批列表、我发起的列表、实例详情页（进度条 + 记录时间线）
- [ ] 列表展示单据类型、单号、发起人、状态、时间，点行进入实例详情
