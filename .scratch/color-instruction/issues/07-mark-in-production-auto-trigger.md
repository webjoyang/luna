# 07: 投产自动触发

**What to build:** 打色单被有效下游引用时自动从 CONFIRMED 转 IN_PRODUCTION。胚布单保存（创建即 ACTIVE）引用打色单后，同进程调用 `ColorInstructionService.markInProduction(id)`；染色单 DRAFT 保存引用**不触发**，"下达"转 ACTIVE 时触发，已 ACTIVE 单补填引用立即触发。实现为条件更新 `UPDATE ... SET status='IN_PRODUCTION' WHERE id=? AND tenant_id=? AND status='CONFIRMED' AND deleted=0`，影响行数 0 即已投产/已作废/已回退，**幂等静默**，不报错。下游作废/删除**不回退**打色单——"曾为此色投产"是事实，IN_PRODUCTION 为终态。

**Blocked by:** 06 下游引用列与保存校验

**Status:** ready-for-agent

- [ ] `ColorInstructionService.markInProduction(id)` 方法：条件更新 `WHERE id=? AND tenant_id=? AND status='CONFIRMED' AND deleted=0`，影响行数 0 幂等静默不报错（已投产/已作废/已回退均安全）
- [ ] 胚布单保存（创建即 ACTIVE）路径：保存事务提交后同进程调用 `markInProduction(colorInstructionId)`（若引用非空）；调用点在胚布单 Service 内，不污染打色单模块
- [ ] 染色单路径：DRAFT 保存引用**不触发**；"下达" DRAFT→ACTIVE 时触发；已 ACTIVE 单补填引用（update 时新引用非空且原引用为空）立即触发
- [ ] 下游作废/删除**不回退**打色单（IN_PRODUCTION 终态，且可能有其他下游）；不实现反向回退逻辑
- [ ] 触发点事务边界：下游保存事务提交后再调 `markInProduction`（避免下游事务回退后打色单已投产的不一致；或同事务但需保证下游失败时打色单不投产——按胚布单/染色单现有事务模式选定， documenting 选择）
- [ ] 测试覆盖：胚布单保存引用触发 CONFIRMED→IN_PRODUCTION、染色单 DRAFT 保存不触发、染色单下达触发、已 ACTIVE 补填引用触发、重复触发幂等静默（影响行数 0 不报错）、下游作废不回退、并发触发安全（条件更新）
