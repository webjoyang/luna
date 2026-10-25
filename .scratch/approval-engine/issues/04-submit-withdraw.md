# 04: 发起审批与撤回（提交人视角）

**What to build:** 业务用户在强制审批单据（销售合同/付款申请/报销/工资单/销售退货）上点「提交审批」，后端按单据类型找到启用的审批流程，解析全部节点的审批人来源为快照存入审批实例，创建首节点的待办任务并通知审批人；单据 `approval_status` 变为 APPROVING 且只读。审批中发起人可点「撤回」，实例置 CANCELLED，单据回到 DRAFT 可编辑。

**Blocked by:** 03 (审批链配置页前端；依赖配置数据才能发起)

**Status:** ready-for-human

- [x] `POST /lunaerp/approval/instance/submit`（body: bizType, bizId）：
  - 校验该 biz_type 有启用流程，无则报 `1_030_030_001`
  - 解析审批人来源为快照：USER→用户 ID 列表、POST→岗位下用户、ROLE→角色下用户、DEPT_LEADER→提交人部门负责人、SUBMITTER_LEADER→提交人直属上级；解析结果存入 `instance.process_snapshot` JSON
  - 创建 instance（status=APPROVING, current_node_sort=1）+ 首节点 task(s)（status=PENDING）+ SUBMIT 操作记录
  - 更新业务单据 `approval_status=APPROVING`
- [x] `PUT /lunaerp/approval/instance/{id}/withdraw`：
  - 仅发起人本人可撤回，非本人报 `1_030_030_005`
  - 仅 APPROVING 态可撤回，否则报 `1_030_030_004`
  - instance.status=CANCELLED、PENDING task 置 DONE+action=null、写 WITHDRAW 记录、业务单据 `approval_status` 回 DRAFT
- [x] 业务单据表加 `approval_status` 列（varchar(16)）：PENDING/APPROVING/APPROVED/REJECTED/CANCELLED；APPROVING 期间后端拒绝编辑
- [x] 创建 task 时发站内信给审批人（复用芋道 notify 模块），待办角标联动
- [x] 前端：业务单据编辑页根据 `approval_status` 控制「提交审批」/「撤回」按钮显示与只读态
- [x] 租户隔离与越权：submit 校验 bizId 归属当前租户；withdraw 校验 submitter_id=当前用户
