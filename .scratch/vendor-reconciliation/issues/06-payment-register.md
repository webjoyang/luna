# 06: 付款登记（出纳执行·OUT 侧：多申请核销 + 预付抵扣 + 账户扣款 + 分录）

**What to build:** 出纳在"收付款"对一张或多张同一供应商的已审批申请做一次付款登记：选资金账户、填本次实付（可小于申请额，差额继续挂应付）、可动用预付余额抵扣，系统按申请创建顺序把钱分配到各申请（每张可部分核销）。登记成功是唯一的资金动作——同事务内写付款单与核销行、扣减资金账户并记流出流水、追加一条 OUT_SUPPLIER 经营分录，四步原子；预付超额、账户余额不足、账户停用都有明确错误。登记后的付款单是写账凭证，不可改不可删（红冲二期）。

**Blocked by:** 05（付款申请与审批状态；预付余额聚合公共方法）

**Status:** resolved

- [x] `POST /payment/register` 入参：supplier_id、核销明细 `[{apply_id, allocate_amount}]`、account_id、pay_amount、prepay_used_amount（缺省 0）
- [x] 守卫：各申请须 APPROVED 且属于同一供应商（否则 023_001/023_007）；每笔 allocate_amount ≤ 该申请剩余额（amount − 已核销聚合），超报 023_003；Σ allocate_amount ≤ pay_amount + prepay_used_amount，钱不够分报 023_007
- [x] 预付守卫：prepay_used_amount ≤ 供应商预付余额（023_005）；账户守卫：账户存在且未停用（023_006），经 finance.account 服务条件更新扣减 pay_amount，余额不足/更新失败报 023_004；12 号未落地前以 Service 内 mock seam 表达，测试 mock 之
- [x] 同事务四步原子：写 payment(direction=OUT, status=COMPLETED) + 每申请一条 payment_ref（apply_no 快照）→ 调账户服务写流出流水 pay_amount → append 一条 OUT_SUPPLIER 经营分录（金额 Σ(pay_amount+prepay_used)，refType=PAYMENT/refId=payment.id，备注含 prepay_used）；任一步失败整体回滚
- [x] 多申请按申请创建时间顺序分配；部分核销后申请剩余额、供应商预付余额经既有聚合方法实时反映；payment_no 取号与 10 号 IN 侧共享前缀，冲突点代码注释挂账共享件清单
- [x] `GET /payment/page`（direction=OUT 固定，筛选供应商/账户/时间）与 `GET /get`（内嵌 ref 核销行：申请号/本次分配额）；不提供 update/delete 端点
- [x] 权限码 payment:register；登记 @LogRecord；实付/抵扣金额 @Sensitive(amount)
- [x] Mockito 单测：多单顺序分配、单笔部分核销、分配超剩余 003、预付超额 005、账户停用 006、账户条件更新失败 004、金额非法 007、四步原子回滚、核销后剩余额与预付余额聚合、纵向越权（无 :register 权限 403）

## Comments
- **付款登记与核销实现完成**：
  1. `PaymentController` 交付 `/register`、`/page`、`/get` 接口，无 update/delete 接口（登记即写账凭证，红冲二期）。
  2. `PaymentServiceImpl` 实现四步原子同事务逻辑：
     - 写 `PaymentDO` (OUT, COMPLETED) 与 `PaymentRefDO`（apply_no 快照）；
     - 调 `FinanceAccountSeam` 扣减余额并记录账户流出流水；
     - 调 `FinanceLedgerSeam` 追加 `OUT_SUPPLIER` 经营分录；
     - 异常时整体回滚。
  3. 严格守卫：
     - 申请必须 `APPROVED` 且同一供应商；
     - 每笔 allocate ≤ 申请剩余额（超报 023_003）；
     - 预付抵扣 ≤ 预付余额（超报 023_005）；
     - 账户停用报 023_006，扣减失败/余额不足报 023_004；
     - 总核销额 ≤ 实付 + 预付抵扣（超报 023_007）。
  4. 单元测试 `PaymentServiceImplTest`（11 个用例）与 `PaymentControllerTest`（3 个用例）全部通过。
