# 横切基础 - 轻量审批引擎（固定审批链 + 审批中心）

Status: ready-for-agent（grilling 定稿，源自 `.scratch/lunaerp-commercialization/issues/02-approval-engine.md`）

## Problem Statement

商业化一期要求关键业务单据（销售合同生效、付款申请、报销、工资单、销售退货）走审批流程，且需一个"审批中心"统一处理待办。芋道底座的 BPM 模块未迁入（`lunasrv-module-bpm` 被注释、无 `bpm_` 表，仅 `flowable.version=8.0.0` 依赖就绪，见 18 号研究结论），回迁成本中-高且一期用不上 BPMN 灵活编排。

现有 MVP 单据（合同/胚布/染色/检验）无审批态，编辑页自由切态；入库单（ADR-0016）已采用严格三态过账状态机并明确预留审批态插入点："未来如引入审批流，在 DRAFT→COMPLETED 之间增加审批态即可"。

需要一个**轻量自研审批引擎**：固定审批链（逐级/或签/会签/驳回）、记录表结构兼容未来迁 Flowable、审批通过触发业务副作用（记账/库存/台账）的统一挂接点对齐 ADR-0017"显式审核驱动写台账"、一个"审批中心"一级菜单（待我审批/我已审批/我发起的）。非强制审批单据（入库单/检验单等生产单据）保持 ADR-0016 三态，不叠审批。

## Solution

在 luna-srv 的 `lunasrv-module-lunaerp` 新增**横切子包 `approval`**（与 baseinfo/business/production/stock/sample/finance 平级，不归入任一业务域），落地 **5 张表**：

1. `lunaerp_approval_config` 审批流程定义（按 `biz_type` 一条启用流程）
2. `lunaerp_approval_config_node` 流程节点定义（顺序/类型/审批人来源）
3. `lunaerp_approval_instance` 审批实例（对应 Flowable `ACT_HI_PROCINST`，含流程快照）
4. `lunaerp_approval_task` 审批任务（对应 `ACT_RU_TASK`+`ACT_HI_TASKINST` 合并，含 `version` 乐观锁）
5. `lunaerp_approval_record` 审批操作记录（对应 `ACT_HI_ACTINST`）

**核心设计决策**（grilling 两轮锁定）：

- 配置表与记录表均放 lunaerp 模块、继承 `TenantBaseDO` 自动租户隔离（不放芋道 system 表，因其字典等表不租户隔离）
- 审批链类型：逐级 SEQUENTIAL / 或签 OR_SIGN / 会签 AND_SIGN；或签"任一通过即过、任一驳回即驳"，会签"全部通过才过、任一驳回即驳"（一票否决）
- 审批人来源：指定用户 / 指定岗位 / 指定角色 / 部门负责人 / 提交人直属上级；**发起时全流程解析快照**存入 instance，在途审批不受组织架构变动影响
- 驳回=终止当前实例，修改后重新提交=新建实例走全流程（不做"回到驳回节点继续"）
- `approval_status` 是业务单据上的跨切面字段：`PENDING`→`APPROVING`→`APPROVED`/`REJECTED`/`CANCELLED`；`APPROVED` 通过 `ApprovalCompletedEvent`（`BEFORE_COMMIT` 同事务）触发各业务单据自身的生效/过账动作，审批引擎不直接写业务账目
- 撤回（发起人主动）：`APPROVING`→`CANCELLED`，业务单据回 DRAFT；`APPROVED` 后不可撤回
- 超时：一期仅记录超时时长，不做自动转交/通过；转交（代审）支持，保留转交链
- 兼容 BPM：instance/task 表预留 `flowable_proc_inst_id`/`flowable_task_id` 可空列，流程快照 JSON 保留 BPMN 关键属性

强制审批单据（一期）：销售合同生效、付款申请、报销、工资单、销售退货。其余单据无审批链则保持 MVP 现状。

## User Stories

### 审批链配置（管理员）

1. 作为系统管理员，我想在"审批中心 → 审批链配置"页按单据类型（销售合同/付款申请/报销/工资单/销售退货）配置审批流程，以便不同单据走不同审批链。
2. 作为系统管理员，我想为每个流程添加多个节点，指定节点顺序、节点类型（逐级/或签/会签）、节点名称，以便搭建审批链。
3. 作为系统管理员，我想为每个节点指定审批人来源：指定用户（多选）/ 指定岗位 / 指定角色 / 部门负责人 / 提交人直属上级，以便灵活配置审批人。
4. 作为系统管理员，我想启用/停用某条流程；同一 `biz_type` 同一时刻只允许一条启用流程，停用旧流程后才能启用新流程。
5. 作为系统管理员，我想删除未被引用的流程定义；已产生审批实例的流程不允许物理删除，只能停用（软删）。

### 发起审批（业务单据提交）

6. 作为业务用户，我想在销售合同/付款申请/报销/工资单/销售退货单据上点"提交审批"，后端创建审批实例并把当前节点的待办推送给审批人，以便启动审批。
7. 作为业务用户，我想提交后单据 `approval_status` 变为 `APPROVING`，单据变为只读（不可编辑），直到审批结束。
8. 作为业务用户，我想在审批中随时点"撤回"，撤回后单据回到 DRAFT 可修改再提交。

### 审批中心 - 待我审批

9. 作为审批人，我想在"审批中心 → 待我审批"看到所有分配给我的待办任务（单据类型、单号、发起人、发起时间、超时时长），以便集中处理。
10. 作为审批人，我想点某条待办进入审批工作台，查看单据详情（只读跳转原单据详情页）与审批进度条（已通过/当前/待审节点），填写审批意见后点"通过"或"驳回"。
11. 作为审批人，我想或签节点下我通过后，其他同节点审批人的待办自动关闭；会签节点下需等所有人通过才推进。
12. 作为审批人，我想把待办"转交给"其他用户（指定接收人），转交后该待办归属接收人，转交记录留痕。
13. 作为审批人，我想驳回时必须填写驳回意见，驳回到发起人。

### 审批中心 - 我已审批 / 我发起的

14. 作为用户，我想在"我已审批"查看我操作过的历史审批记录（单据、操作类型、意见、时间）。
15. 作为用户，我想在"我发起的"查看我发起的审批实例及其当前进度（当前节点、状态），以便跟踪。

### 审批通过触发副作用

16. 作为业务，我想审批通过后，对应业务单据自动执行生效/过账动作（合同生效、付款写流水、报销写流水、工资发放写流水、退货写库存回冲+应收冲减），与审批在同一事务，保证一致性。
17. 作为业务，我想审批驳回后，单据 `approval_status` 变为 `REJECTED`，单据回到可编辑状态，发起人可修改后重新提交。

## Data Model

### `lunaerp_approval_config` 审批流程定义

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| tenant_id | bigint | 租户（TenantBaseDO） |
| biz_type | varchar(32) | 单据类型：sales_contract/payment/expense/payroll/sales_return，字典 `lunaerp_approval_biz_type` |
| name | varchar(64) | 流程名称 |
| status | varchar(16) | ENABLED/DISABLED，同 biz_type 仅一条 ENABLED |
| remark | varchar(255) | |
| creator/create_time/updater/update_time/deleted | | BaseDO |

索引：`(tenant_id, biz_type)` 唯一（含软删行，ADR-0004 模式）；`(tenant_id, status)`。

### `lunaerp_approval_config_node` 流程节点定义

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| tenant_id | bigint | |
| config_id | bigint | 关联 approval_config.id |
| sort | int | 节点顺序（从 1 开始） |
| name | varchar(64) | 节点名称（如"部门主管审批"） |
| node_type | varchar(16) | SEQUENTIAL/OR_SIGN/AND_SIGN |
| approver_type | varchar(16) | USER/POST/ROLE/DEPT_LEADER/SUBMITTER_LEADER |
| approver_ids | varchar(512) | approver_type=USER 时存用户 ID 列表 JSON；其余存岗位/角色 ID |
| creator/create_time/updater/update_time/deleted | | |

索引：`(config_id, sort)`。

### `lunaerp_approval_instance` 审批实例

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| tenant_id | bigint | |
| biz_type | varchar(32) | 单据类型 |
| biz_id | bigint | 业务单据 ID |
| biz_no | varchar(64) | 业务单号快照（展示用） |
| process_snapshot | text | 流程定义全量快照 JSON（含每节点审批人 ID 列表），发起时解析 |
| status | varchar(16) | APPROVING/APPROVED/REJECTED/CANCELLED |
| submitter_id | bigint | 发起人用户 ID |
| current_node_sort | int | 当前节点序号（-1 表示已结束） |
| flowable_proc_inst_id | varchar(64) | 预留，迁 BPM 时回填 |
| start_time/end_time | datetime | |
| creator/create_time/updater/update_time/deleted | | |

索引：`(tenant_id, biz_type, biz_id)`；`(tenant_id, submitter_id, status)`；`(tenant_id, status)`。

### `lunaerp_approval_task` 审批任务

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| tenant_id | bigint | |
| instance_id | bigint | 关联 instance.id |
| node_sort | int | 节点序号 |
| node_name | varchar(64) | 节点名称快照 |
| node_type | varchar(16) | 节点类型快照 |
| approver_id | bigint | 当前审批人用户 ID |
| status | varchar(16) | PENDING/DONE |
| action | varchar(16) | 空/DONE（通过）/REJECTED（驳回）/TRANSFERRED（转交） |
| start_time/end_time | datetime | |
| flowable_task_id | varchar(64) | 预留 |
| version | int | 乐观锁（会签并发） |
| creator/create_time/updater/update_time/deleted | | |

索引：`(tenant_id, approver_id, status)`；`(instance_id)`。

### `lunaerp_approval_record` 审批操作记录

| 字段 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | |
| tenant_id | bigint | |
| instance_id | bigint | |
| task_id | bigint | 可空（撤回等无 task 的操作） |
| operator_id | bigint | 操作人 |
| action | varchar(16) | APPROVE/REJECT/TRANSFER/WITHDRAW/SUBMIT |
| comment | varchar(512) | 审批意见 |
| transfer_to_id | bigint | action=TRANSFER 时的接收人 |
| operate_time | datetime | |
| creator/create_time/updater/update_time/deleted | | |

索引：`(instance_id, operate_time)`；`(tenant_id, operator_id, action)`。

## 状态机

### 实例状态 `instance.status`

```
SUBMIT(创建) → APPROVING
APPROVING → APPROVED  (最后节点通过)
APPROVING → REJECTED  (任一节点驳回)
APPROVING → CANCELLED (发起人撤回)
APPROVED/REJECTED/CANCELLED = 终态
```

### 任务状态 `task.status` + `task.action`

```
节点到达 → 创建 task(s): status=PENDING, action=null
或签: 任一 task APPROVE → 本节点通过，同节点其他 task 置 DONE+action=null(自动关闭)
会签: 全部 task APPROVE → 本节点通过
任一 task REJECT → 实例 REJECTED，同节点其他 PENDING task 置 DONE+action=null
TRANSFER: 当前 task 置 DONE+action=TRANSFERRED，新建 task 给接收人
节点通过 → 下一节点创建 task(s)；无下一节点 → 实例 APPROVED
```

### 业务单据 `approval_status`

```
DRAFT(无审批) → PENDING(可提交) → APPROVING → APPROVED/REJECTED/CANCELLED
REJECTED/CANCELLED → 回到业务 DRAFT 可编辑，重新提交新建 instance
APPROVED = 触发业务生效/过账（事件），之后不可撤回
```

## 审批通过副作用机制（对齐 ADR-0017）

审批引擎**不直接写业务账目**。审批通过（最后节点通过）时：

1. 事务内更新 instance.status=APPROVED、当前 task 状态
2. 发布 `ApprovalCompletedEvent(bizType, bizId)`，使用 `@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)` 保证与审批在**同一事务**
3. 各业务模块实现 `ApprovalHandler` 策略 Bean（按 biz_type 路由），在事件监听中执行自身生效/过账逻辑：
   - 销售合同：`status = ACTIVE`（生效）
   - 付款申请：写经营分录账付款流水
   - 报销：写资金流水
   - 工资单：写工资发放流水
   - 销售退货：写库存回冲流水 + 应收冲减

若业务侧过账抛异常，事务回滚，审批不生效——保证"审批通过=业务生效"原子性。

## API

### 审批链配置（管理员）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/lunaerp/approval/config/page` | 分页查询流程 | `lunaerp:approval:config:query` |
| GET | `/lunaerp/approval/config/{id}` | 流程详情（含节点） | 同上 |
| POST | `/lunaerp/approval/config` | 新建流程（含节点） | `lunaerp:approval:config:create` |
| PUT | `/lunaerp/approval/config/{id}` | 更新流程（含节点整体替换） | `lunaerp:approval:config:update` |
| PUT | `/lunaerp/approval/config/{id}/status` | 启用/停用 | 同上 |
| DELETE | `/lunaerp/approval/config/{id}` | 删除（未引用） | `lunaerp:approval:config:delete` |

### 审批操作

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/lunaerp/approval/instance/submit` | 提交审批（body: bizType, bizId） | 各业务单据自身提交权限 |
| PUT | `/lunaerp/approval/instance/{id}/withdraw` | 撤回 | 发起人本人 |
| GET | `/lunaerp/approval/task/todo-page` | 待我审批分页 | `lunaerp:approval:task:query` |
| GET | `/lunaerp/approval/task/done-page` | 我已审批分页 | 同上 |
| GET | `/lunaerp/approval/instance/my-page` | 我发起的分页 | 同上 |
| GET | `/lunaerp/approval/instance/{id}` | 实例详情（含进度+记录） | 同上 |
| PUT | `/lunaerp/approval/task/{id}/approve` | 通过（body: comment） | 审批人本人 |
| PUT | `/lunaerp/approval/task/{id}/reject` | 驳回（body: comment 必填） | 审批人本人 |
| PUT | `/lunaerp/approval/task/{id}/transfer` | 转交（body: toUserId, comment） | 审批人本人 |

## 菜单与权限

新建一级菜单 **审批中心**（Q18 规划，与芋道 BPM 遗留菜单 1185/1200 解耦）：

| 菜单 | sort | path | 权限前缀 |
|---|---|---|---|
| 审批中心（一级目录） | 109 | `/approval` | `lunaerp:approval` |
| 待我审批 | 10 | `todo` | `lunaerp:approval:task:query` |
| 我已审批 | 20 | `done` | 同上 |
| 我发起的 | 30 | `mine` | 同上 |
| 审批链配置 | 40 | `config` | `lunaerp:approval:config:*` |

待办角标复用芋道 notify WebSocket：创建 task 时发站内信给审批人。

## 错误码

段位 `1-030-030-xxx`（lunaerp 横切审批模块，shared GLOSSARY 段位表追加）。

| 码 | 说明 |
|---|---|
| 1_030_030_000 | 审批流程不存在 |
| 1_030_030_001 | 该单据类型无启用的审批流程 |
| 1_030_030_002 | 审批实例不存在 |
| 1_030_030_003 | 审批任务不存在 |
| 1_030_030_004 | 当前状态不允许该操作 |
| 1_030_030_005 | 非当前审批人无权操作 |
| 1_030_030_006 | 驳回意见不能为空 |
| 1_030_030_007 | 该单据类型已有启用流程，请先停用 |
| 1_030_030_008 | 流程已被引用，无法删除 |
| 1_030_030_009 | 转交接收人不能是自己 |

## BPM 迁移兼容说明

- instance/task 表的 `flowable_proc_inst_id`/`flowable_task_id` 可空列，未来回迁 Flowable 时可由迁移脚本回填
- `process_snapshot` JSON 保留节点类型/审批人/顺序等 BPMN 关键属性，迁移时可转换为 BPMN XML
- 操作记录表与 Flowable `ACT_HI_ACTINST` 字段语义对齐，迁移时可直接映射
- 芋道现有"工作流程"(1185)>"审批中心"(1200) 菜单一期隐藏，BPM 回迁后再决定是否合并

## 统一 DoD 对照

- [x] 状态机白名单+终态守卫：实例四态、任务两态严格守卫，非当前态操作报 1_030_030_004
- [x] 并发条件更新：task 表 `version` 乐观锁防会签并发覆盖
- [x] 芋道权限+脱敏：审批意见等无敏感字段；权限码按菜单/按钮注册
- [x] 列表/详情/删除进回收站：配置软删，实例/任务/记录不物理删（审计留痕）
- [x] 打印+操作/审批日志：操作记录 = `lunaerp_approval_record`，另用 `@LogRecord` 记录操作日志
- [x] 核心状态流转测试：逐级/或签/会签通过、驳回、撤回、转交各一条集成测试
- [x] 审批通过副作用测试：模拟某 biz_type 的 ApprovalHandler，验证同事务回滚
- [x] 前端验收清单+波次冒烟：审批中心三视图 + 配置页
- [x] 术语与错误码归位：术语进 shared GLOSSARY，错误码 1-030-030
- [x] 性能：待办列表按 approver_id 索引，万级 P95<1s
- [x] 租户隔离：全部表继承 TenantBaseDO，写接口租户隔离测试
- [x] 横向/纵向越权：非本人 task 不可操作（1_030_030_005），非本人 instance 不可见

## Out of Scope

- BPMN 可视化编排、条件分支、子流程（二期或 BPM 回迁）
- 比例会签、票数统计、抄送
- 超时自动转交/自动通过、委托代理（长期委托）
- 按金额/部门分级走不同流程
- 审批代理设置、审批统计报表
- 芋道 BPM 模块回迁（独立 ticket）
