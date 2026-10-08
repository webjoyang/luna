# 07: 审批通过副作用机制（事件 + 策略 Handler）

**What to build:** 审批通过（最后节点通过）时，审批引擎在同一事务内发布 `ApprovalCompletedEvent`，各业务模块通过实现 `ApprovalHandler` 策略 Bean（按 biz_type 路由）执行自身生效/过账动作。销售合同生效（`status=ACTIVE`）作为落地示例。若业务侧过账抛异常，事务回滚，审批不生效——保证「审批通过=业务生效」原子性。APPROVED 后不可撤回。

**Blocked by:** 05 (审批操作；依赖 instance 能走到 APPROVED)

**Status:** ready-for-agent

- [ ] 定义 `ApprovalCompletedEvent(bizType, bizId)` 事件类
- [ ] `@TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)` 监听器：事件触发时按 biz_type 查找 `ApprovalHandler` 并调用，与审批更新在同一事务
- [ ] `ApprovalHandler` 策略接口：`boolean supports(String bizType)`、`void onApproved(Long bizId)`；Spring 自动收集所有实现 Bean，按 biz_type 路由
- [ ] 销售合同 handler：`sales_contract` → 把合同 `status` 置 ACTIVE（生效）
- [ ] 为 payment/expense/payroll/sales_return 预留 handler 注册点（接口与路由就绪，具体实现由对应 feature 票接入，不在本票实现）
- [ ] APPROVED 终态守卫：instance.status=APPROVED 后撤回接口报 `1_030_030_004`
- [ ] 集成测试：模拟某 biz_type 的 ApprovalHandler 抛异常，验证审批 instance 回滚为 APPROVING（或事务整体回滚），证明同事务原子性
- [ ] 与 ADR-0017「显式审核驱动写台账」对齐：审批引擎不直接写业务账目，副作用由各业务 handler 自处理
