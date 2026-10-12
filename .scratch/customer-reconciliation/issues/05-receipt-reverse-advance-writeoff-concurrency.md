# 05: 收款单 — 红冲 + 预收转核销 + 并发核销专测

**What to build:** 已过账收款单（status=COMPLETED）唯一逆向=红冲（`:reverse` 权限）：反向分录 + 账户余额回加 + 核销行全部自动重开 + status=REVERSED，不做删除。预收转核销（`:writeoff` 权限，不审批）：在原 ADVANCE 单上追加核销行（同币种、≤预收余额、≤分录未核销余额），不建新单。并发重复核销专测：两事务同时过账同一 AR 分录，条件更新失败回滚 RECEIPT_POST_CONFLICT，天然防重复核销。

**Blocked by:** 04 收款单 — 草稿/提交/审批过账同事务（含自动分配核销）

**Status:** completed

- [x] `PUT /reverse?id=`（`:reverse` 权限）：同事务——RECEIPT_REVERSAL 分录（reversal_of_id 回指）+ 账户 `current_balance -= amt` 条件更新 + 该单核销行全部自动重开（未核销余额恢复）+ status=REVERSED
- [x] `POST /advance-writeoff`（`:writeoff` 权限）：paymentId=已过账 ADVANCE 单 + 分配数组；在原单追加核销行（ref_type=AR_ENTRY），写 ADVANCE_WRITE_OFF 分录；守卫同币种、≤预收余额（ADVANCE 入账 − ADVANCE_WRITE_OFF）、≤分录未核销余额
- [x] 过账时逐行条件更新重验，冲突抛 1_030_023_105 RECEIPT_POST_CONFLICT（可重试）
- [x] 红冲后核销行重开：原核销行对应分录未核销余额恢复（可被后续收款单再次核销）
- [x] 红冲幂等：REVERSED 状态不可再红冲（RECEIPT_REVERSAL_NOT_ALLOW）
- [x] 测试：红冲四写断言（反向分录+余额回加+核销行重开+未核销余额恢复）；预收转核销余额口径/守卫；**并发重复核销专测**（两事务过账同一分录，条件更新失败回滚 RECEIPT_POST_CONFLICT）；红冲幂等
