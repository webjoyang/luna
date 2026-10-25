# 06: 我已审批 + 我发起的 + 实例详情

**What to build:** 用户在「审批中心 → 我已审批」查看自己操作过的历史审批记录（单据、操作类型、意见、时间）；在「我发起的」查看自己发起的审批实例及其当前进度（当前节点、状态）；点进实例详情可看到完整节点进度条与操作记录时间线。横向越权：非本人操作/发起的实例不可见。

**Blocked by:** 05 (审批操作；依赖已有操作记录与终态实例)

**Status:** ready-for-human

- [x] `GET /lunaerp/approval/task/done-page`：按当前用户 operator_id 从 `lunaerp_approval_record` 反查，分页返回操作过的实例（单据、操作类型、意见、时间）
- [x] `GET /lunaerp/approval/instance/my-page`：按 submitter_id=当前用户分页，返回实例当前状态、当前节点
- [x] `GET /lunaerp/approval/instance/{id}`：返回实例基本信息 + 节点进度（每节点状态：已通过/当前/待审）+ 操作记录时间线（操作人、动作、意见、时间、转交接收人）
- [x] 越权过滤：done-page 仅本人操作记录、my-page 仅本人发起；instance 详情对非 submitter 且非任一 task approver 的用户返回 404（复用 `1_030_030_002` 或越权码）
- [x] 前端：我已审批列表、我发起的列表、实例详情页（进度条 + 记录时间线）
- [x] 列表展示单据类型、单号、发起人、状态、时间，点行进入实例详情

## Comments
- **后端架构与接口落地**：
  - 新增 `GET /lunaerp/approval/task/done-page`：在 `ApprovalTaskController` 与 `ApprovalTaskServiceImpl` 落地，通过 `ApprovalRecordMapper.selectDonePage` 多表联查过滤 `operator_id = loginUserId`，支持单据类型、单号与动作筛选；聚合返回实例信息、审批意见、操作时间、节点名称与用户昵称。
  - 新增 `GET /lunaerp/approval/instance/my-page`：在 `ApprovalInstanceController` 与 `ApprovalInstanceServiceImpl` 落地，通过 `ApprovalInstanceMapper.selectMyPage` 强制绑定 `submitter_id = loginUserId` 租户隔离与越权阻断；返回实例当前状态与当前节点名称。
  - 完善 `GET /lunaerp/approval/instance/{id}`：
    - 严格水平越权防护：对非 submitter 且非任一 task approver 的用户，一律抛出 `1_030_030_002`（`APPROVAL_INSTANCE_NOT_EXISTS`，语义对应 404 不存在），杜绝横向探测单据。
    - 结构化节点进度推导：依据实例状态与当前节点序号，精确推导每个节点的 `PASSED`（已通过）、`CURRENT`（审批中）、`PENDING`（待审）、`REJECTED`（已驳回）或 `CANCELLED`（已撤回）状态与审批人姓名列表。
    - 操作记录时间线：完整输出操作人、动作、意见、时间及转交接收人姓名。
- **前端页面与交互实现**：
  - 在 `luna-ui/src/api/approval/` 补齐 `done-page`、`my-page`、`nodeProgress` 等全部 TypeScript 类型定义与接口封装。
  - 在 `luna-ui/src/router/modules/remaining.ts` 注册 `/approval/done`、`/approval/mine`、`/approval/detail` 路由。
  - 我已审批（`views/approval/done/index.vue`）：单据类型/单号/操作类型筛选，展示单据类型、单号、发起人、实例状态、审批节点、我的操作、意见与操作时间，点击单号/点击行/详情按钮无缝进入实例详情。
  - 我发起的（`views/approval/mine/index.vue`）：单据类型/单号/审批状态筛选，展示单据类型、单号、发起人、当前节点、审批状态、发起与结束时间；支持审批中实例一键撤回确认弹窗与重新提交导向。
  - 实例详情（`views/approval/detail/index.vue`）：包含流程节点进度条（`el-steps` 动态状态色与审批人回显）、单据核心信息卡片（支持原单据如销售合同跳转）、审批操作记录时间线（`el-timeline` 动作标签、转交人与意见卡片），并优雅处理越权/404 友好提示。
- **自动化测试保障**：
  - 后端：`ApprovalInstanceServiceImplTest`、`ApprovalInstanceControllerTest`、`ApprovalTaskServiceImplTest`、`ApprovalTaskControllerTest` 全面覆盖，包含已办聚合、发起聚合、任一节点审批人放行、横向越权拦截抛 404、节点状态推导及未登录校验等用例，61 个审批引擎单测全部 PASS。
  - 前端：新增 `approval-done.spec.ts`、`approval-mine.spec.ts`、`approval-detail.spec.ts`，配合既有测试共 50 个 Vitest 单元测试全部 PASS；Vite `build:dev` 打包 0 错误。
