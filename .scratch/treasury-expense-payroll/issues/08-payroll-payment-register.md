# 08: 工资发放登记（GF 批次 + OUT_SALARY 过账）

**What to build:** 出纳可对 APPROVED 工资单做发放登记：选账户、填本次金额（缺省未发余额）、发放日、备注，可多次登记跨账户（银行批量+现金尾款）；每次同事务写 GF 批次 + 经统一过账服务扣款并写 CREDIT/OUT_SALARY 分录（party=NONE，备注带工资单号+批次序号），累计不超实发合计；批次不可改删；Σ 批次驱动列表派生态（未发/部分/发清）。不挂员工、不使用 10/11 共享 payment 表（Q10：批次不分到人）。

**Blocked by:** 06、02

**Status:** closed

- [x] `POST /payroll/payment/register`：仅 APPROVED（025_001）；本次金额>0 且 Σ已有批次+本次 ≤ net_amount（025_007）；缺省填未发余额；经统一通道扣款（余额不足 023_004、停用 020_101）
- [x] 同事务：写 `lunaerp_finance_payroll_payment`（payment_no=`GF`+YYYYMMDD+3 位唯一含软删行、payroll_id/payroll_no/period/account_id/account_name 快照、currency/exchange_rate/base_amount、pay_date、remark）+ CREDIT/OUT_SALARY 分录（party=NONE、ref_type=PAYROLL_PAYMENT、ref_no=payment_no、备注含工资单号+批次序号）；批次不可改删（无 update/delete 端点）
- [x] `GET /payroll/payment/page?payrollId=`：批次列表；`GET /payroll/get` 已发放聚合与列表派生态（未发放/部分发放/已发清）由此真实生效（06 票算式已就位）
- [x] 权限码 payroll:pay；发放登记 @LogRecord；金额 @Sensitive(AMOUNT)
- [x] Mockito 单测：超额 025_007/余额不足 023_004 走统一通道/非 APPROVED 拒绝 025_001/多次登记跨账户累计正确/OUT_SALARY 分录 party=NONE 与 ref/备注正确/批次不可改删/派生态三态翻转/横向越权报不存在
