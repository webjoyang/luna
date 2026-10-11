# 05: 付款申请（RECON/PREPAY）+ 02 号审批引擎集成

**What to build:** 采购/财务能从一张已确认对账单一键发起付款申请（默认全额、可改小），系统保证同一对账单累计申请额绝不超过确认金额；也能发起不挂对账单的预付付款申请（供应商+金额+必填用途）。申请提交后进入 02 号审批中心走配置好的审批链，驳回可改后重新提交（重走全流程）、审批中可撤回；关键语义：**审批通过不发生任何资金动作**，Handler 只回写状态，钱要等下一张票的付款登记。申请列表可查每个供应商的实时预付余额。

**Blocked by:** 04（确认/作废守卫；申请挂靠 CONFIRMED 对账单）

**Status:** resolved

- [x] `GET /payment-apply/page`（筛选 supplierId/applyType/approvalStatus/keyword）与 `GET /get`（内嵌只读已核销金额聚合 = Σ payment_ref.allocate_amount）
- [x] `POST /create` RECON：recon 必须存在且 CONFIRMED，否则报 023_002；供应商快照取对账单；金额 >0；同事务重查"Σ 已申请（排除 REJECTED/CANCELLED）+ 本次 ≤ total_amount"并条件持久化防并发超申，超出报 022_007（消息带累计额）
- [x] `POST /create` PREPAY：purpose 必填、金额 >0；bank_name/bank_account 从供应商档案快照带出仅展示，序列化标 @Sensitive(contact)
- [x] apply_no 按 ADR-0006 按天取号（FK 前缀）；`PUT /update` 仅 DRAFT/REJECTED；`DELETE` 仅 DRAFT 软删进回收站（bizType=PAYMENT_APPLY，恢复回 DRAFT）
- [x] `POST /submit` 按 `lunaerp_approval_config` 付款申请链创建审批实例（强制审批单据）；订阅 02 号 ApprovalCompletedEvent（BEFORE_COMMIT 同事务）：APPROVED 仅置 approval_status，Handler 内**零账务写入**；REJECTED 回置走引擎标准流程；`POST /cancel-approve` 仅 APPROVING 可撤回回 PENDING/DRAFT
- [x] 审批中心三视图（待我审批/我已审批/我发起的）、审批日志、到达/通过/驳回消息全部复用 02 号，本票零新建消息模板
- [x] `GET /prepay-balance?supplierId=`：预付余额 = Σ(PREPAY 类申请被核销的 allocate_amount) − Σ(所有付款登记 prepay_used_amount)，聚合 SQL 仅此 Service 公共方法一处
- [x] 权限码 payment:apply；提交/撤回 @LogRecord；Mockito：非 CONFIRMED 报 002、并发超申报 007、PREPAY 缺 purpose、REJECTED 改再提重走全流程、APPROVED Handler 无任何账务写入、余额聚合口径、状态外操作报 001
