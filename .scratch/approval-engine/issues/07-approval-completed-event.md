# 07: 审批通过副作用机制（事件 + 策略 Handler）

**What to build:** 审批通过（最后节点通过）时，审批引擎在同一事务内发布 `ApprovalCompletedEvent`，各业务模块通过实现 `ApprovalHandler` 策略 Bean（按 biz_type 路由）执行自身生效/过账动作。销售合同生效（`status=ACTIVE`）作为落地示例。若业务侧过账抛异常，事务回滚，审批不生效——保证「审批通过=业务生效」原子性。APPROVED 后不可撤回。

**Blocked by:** 05 (审批操作；依赖 instance 能走到 APPROVED)

**Status:** ready-for-human

- [x] 定义 `ApprovalCompletedEvent(bizType, bizId)` 事件类
- [x] `@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)` 监听器：事件触发时按 biz_type 查找 `ApprovalHandler` 并调用，与审批更新在同一事务
- [x] `ApprovalHandler` 策略接口：`boolean supports(String bizType)`、`void onApproved(Long bizId)`；Spring 自动收集所有实现 Bean，按 biz_type 路由
- [x] 销售合同 handler：`sales_contract` → 把合同 `status` 置 ACTIVE（生效）
- [x] 为 payment/expense/payroll/sales_return 预留 handler 注册点（接口与路由就绪，具体实现由对应 feature 票接入，不在本票实现）
- [x] APPROVED 终态守卫：instance.status=APPROVED 后撤回接口报 `1_030_030_004`
- [x] 集成测试：模拟某 biz_type 的 ApprovalHandler 抛异常，验证审批 instance 回滚为 APPROVING（或事务整体回滚），证明同事务原子性
- [x] 与 ADR-0017「显式审核驱动写台账」对齐：审批引擎不直接写业务账目，副作用由各业务 handler 自处理

## Comments
- **事件与策略 Handler 设计落地**：
  - 增强 `ApprovalCompletedEvent(bizType, bizId)`，支持带事件源与纯参数构造器，在终节点审批通过时发布。
  - 定义 `ApprovalHandler` SPI 策略接口（`supports(String bizType)` 与 `onApproved(Long bizId)`），声明 `sales_contract`、`payment`、`expense`、`payroll`、`sales_return` 预留点常量。
  - 实现 `ApprovalCompletedEventListener`，基于 `@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT, fallbackExecution = true)` 监听事件，自动注入并按 `bizType` 路由所有 `ApprovalHandler` Bean。若未注册对应 Handler 则安全放行，若 Handler 执行抛出异常则向上透传触发事务回滚。
  - 实现 `SalesContractApprovalHandler`，精确捕获 `sales_contract` 审批通过并将销售合同主表 `status` 字段置为 `ACTIVE`（生效）。
  - 终态撤回守卫：已通过（`APPROVED`）的实例调用撤回接口严格拦截并抛出 `1_030_030_004`（`APPROVAL_INSTANCE_STATUS_ERROR`）。
  - 架构对齐：审批引擎只负责审批流转与单据 approvalStatus 状态更新，单据生效及未来资金/库存过账（ADR-0017）全部由各业务领域 Handler 独立承接。
- **单测与集成测试原子性验证**：
  - `ApprovalCompletedEventListenerTest`（5 用例）：覆盖精确路由、空参数防御、未接入单据安全放行、Handler 列表为空安全放行、Handler 异常向上透传。
  - `SalesContractApprovalHandlerTest`（2 用例）：覆盖 `supports("sales_contract")` 匹配与 `status=ACTIVE` 数据库更新。
  - `ApprovalCompletedEventIntegrationTest`（3 用例）：基于真实 Spring 事务管理器与 `@TransactionalEventListener(phase = BEFORE_COMMIT)`，验证终节点通过同事务触发生效并提交（`Connection.commit` 触发）；模拟 Handler 业务异常验证 Spring 事务全局回滚（`Connection.rollback` 触发，`commit` 绝不触发），证明强原子性；验证预留点动态扩展。
  - 全模块审批套件 78 个单元与集成测试全部 PASS。
