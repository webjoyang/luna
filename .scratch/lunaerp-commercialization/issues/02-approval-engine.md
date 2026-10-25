# 02-approval-engine: 轻量审批引擎设计

Type: grilling
Status: resolved
Blocked by: 18

## Question

设计轻量自研审批引擎并产出 spec（对应 Q8=A），grilling 时先调 Skill "grilling" 与 "domain-modeling"：

1. 审批链配置模型：按单据模块配置审批人/角色链；支持逐级、或签、会签、驳回到提交人；配置存哪里（芋道 system 扩展表 vs lunaerp 新表）？
2. 审批记录表结构：审批实例/节点/操作记录，字段设计需兼容未来迁芋道 BPM（Flowable）时的映射（见 18 号研究结论）；
3. 单据集成方式：`approval_status` 字段约定、审批通过触发副作用（记账/库存/台账）的统一挂接点（对照 ADR-0017 的审核驱动写台账模式）；
4. 一期强制审批的单据清单（建议：销售合同生效、付款申请、报销、工资单、退货；其余可配置）；
5. 超时/代审/撤回规则；
6. "审批中心"一级菜单（Q18）的功能范围：待我审批/我已审批/我发起的。

产出：`.scratch/approval-engine/spec.md`（按统一 DoD）。术语（审批链/或签/会签/审批实例）进 GLOSSARY。

## Answer

两轮 grilling 全部锁定，spec 已落 `.scratch/approval-engine/spec.md`，术语进 `docs/contexts/shared/GLOSSARY.md` 审批引擎节。核心决策：

1. **配置存储**：lunaerp 新表 `lunaerp_approval_config` + `lunaerp_approval_config_node`，继承 `TenantBaseDO` 自动租户隔离（不放芋道 system 表，因其字典等不租户隔离）。节点类型 SEQUENTIAL/OR_SIGN/AND_SIGN，审批人来源支持指定用户/岗位/角色/部门负责人/提交人直属上级。
2. **记录三表**：`lunaerp_approval_instance`（对应 ACT_HI_PROCINST）+ `lunaerp_approval_task`（ACT_RU/HI_TASK 合并，含 version 乐观锁）+ `lunaerp_approval_record`（ACT_HI_ACTINST）。预留 `flowable_proc_inst_id`/`flowable_task_id` 可空列 + `process_snapshot` JSON，兼容未来迁 Flowable。
3. **单据集成**：`approval_status`（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）为跨切面字段；`APPROVED` 发布 `ApprovalCompletedEvent`（`BEFORE_COMMIT` 同事务），各业务单据 `ApprovalHandler` 自实现生效/过账（审批引擎不直接写业务账目，对齐 ADR-0017）。驳回/撤回后业务单据回 DRAFT，重新提交=新建实例走全流程。
4. **强制审批清单**：销售合同生效、付款申请、报销、工资单、销售退货。入库单/检验单等生产单据不加 approval_status，沿用 ADR-0016 三态。
5. **超时/代审/撤回**：超时一期仅记录时长不自动动作；支持转交（留痕）；发起人在 APPROVING 可撤回（回 DRAFT），APPROVED 后不可撤回。
6. **审批中心**：新建一级菜单（与芋道 BPM 遗留 1185/1200 解耦），三视图待我审批/我已审批/我发起的，待办角标复用芋道 notify WebSocket。

或签/会签语义：或签"任一通过即过、任一驳回即驳"；会签"全部通过才过、任一驳回即驳"（一票否决）。审批人发起时全流程解析快照，在途不受组织变动影响。错误码段位 `1-030-030`。

本 ticket 解阻塞 13 号（销售退货）等依赖审批的单据。
