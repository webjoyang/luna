# 10-customer-reconciliation: 客户对账/开票申请/收款核销

Type: grilling
Status: resolved
Blocked by: 01, 02, 04, 17

## Question

设计客户应收闭环并产出 spec（W3），grilling 时先调 Skill "grilling" 与 "domain-modeling"，并先读 17 号研究结论：

1. 对账口径：取数 = 签收数量还是发货数量（Q：签收前是否可对账）？单价取合同价；对账单生成（按客户+账期）与客户确认状态（对接 14 号客户门户的对账确认）；
2. 开票申请：抬头/税号/税率/开票类型；只做申请与登记，不做税盘/金税对接（写进非目标）；
3. 收款登记与核销：收款单（资金账户入账）、核销规则（一对多核销对账单/合同、部分核销、预收款）；
4. 账龄分桶口径（30/60/90/180+？）与账龄汇总页；
5. 审批与打印：收款走 02 号审批链；对账单打印走 04 号统一打印；
6. 外币：销售合同已含多币种，本币折算与汇率（汇率主档在 12 号还是本 spec？需决定）。

产出：`.scratch/customer-reconciliation/spec.md`（按统一 DoD）。

## Answer

三轮 grilling 17 问（Q1–Q17）全部定稿，spec 落 `.scratch/customer-reconciliation/spec.md`（ready-for-agent）。要点：

- **应收成立点=发货审核**（Q1=A）：shipment 审核/红冲同事务发事件（前置 session 承载，不回改 shipment spec），finance 写 AR_INC/AR_INC_REVERSAL（逐明细行+运费头级）；对账单降级为期间汇总+确认+打印凭证——五态 DRAFT/SENT/CONFIRMED/REJECTED/VOID，行级占用，多币种分单，签收前可对账，CONFIRMED 后源单红冲/退货不回改。
- **收款单走 02 审批**（biz_type=CUSTOMER_RECEIPT）一步过账；核销锚定发货明细分录（严格同币种、FIFO 自动分配可改、三视角选单）；预收=ADVANCE 分型，转核销不建新单；已过账唯一逆向=红冲（反向分录+核销自动重开）。
- **账龄五桶**（0-30/31-60/61-90/91-180/181+）FIFO 按客户实时算；credit_days 逾期静态筛选；**预警 job 不做**（Q6 否决第 3 点，LUNA_AR_PERIOD_WARNING 一期不接线）。
- **发票申请不接审批引擎**（四态 DRAFT/SUBMITTED/ISSUED/REJECTED），挂发货单+累计开票守卫，数电票字段、单头单税率，与收款核销完全解耦，不做打印。
- **汇率主档归本票**（SPOT，租户+币种+生效日唯一）；核销同币种，汇兑损益分录非目标。
- **域公共表** ledger_entry/account/exchange_rate DDL 由本票先行落地，11/12 号消费扩列；payment 共表 IN 侧扩列收口 11 号冲突清单 12 项；13 号加 Blocked by 10 并冻结 recordArReturnReversal 契约。
- 字典 2272–2278、错误码 020/021/023(100–199)/026、菜单 10720/10740/10770 按钮段+**新增 10780 汇率**（ADR-0037 已增补）；前端落 `lunaerp-elmui/finance/`；`_order.md` 序号 26；门户 4 端点契约冻结给 14 号。

## Comments

- **2026-10-07 12 号合并收口**（spec 已定稿）：两处并行冲突经用户仲裁以本票为准——① 余额维持 current_balance 条件更新（12 号 Q18=A，ADR-0043 统一钉死，全域过账收口到 12 号 `FinancePostingService.post()`，收款 handler 实现时改调该服务，SQL 语义零变化）；② 汇率表/10780/错误码 200–202 归本票（12 号 Q19=A，12 号纯消费）。12 号对公共表的增量：account ALTER +3 列（account_no/account_nature/remark）、ledger_entry ALTER +2 列（party_name/ref_no）、向字典 2274 幂等追加 6 值（OUT_EXPENSE/OUT_SALARY/TRANSFER_IN/TRANSFER_OUT/IN_OTHER/OUT_OTHER）；账户删除与期初修改由 12 号页面落地（无分录可删/可改，复用 020_103/104）；10780 sort 以 12 号菜单总表校正为 9（本票 spec 写 8 与 10770 冲突）。详见 `.scratch/treasury-expense-payroll/spec.md` 收口清单 12 项。
