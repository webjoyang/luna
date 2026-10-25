# 17-legacy-finance-research: 老系统财务/资金/退货模型深挖

Type: research
Status: resolved
Blocked by: 

## Question

深挖老系统 linaagi（`D:\work\luna\go0829\linaagi`）财务与资金相关实现，为 W3 的 10/11/12/13 号决策提供事实（AFK，research 子代理直接解决）：

1. 客户/供应商对账：取数口径（签收 vs 发货 vs 出库）、对账单生成与确认流程、表结构；
2. 收款/付款与核销模型：核销粒度（合同/对账单/发票）、部分核销、预收预付；
3. 开票登记字段与流程；
4. 资金账户、收支、账龄算法；
5. 报销与工资模型（尤其质检扣款联动）；
6. 退货单：库存回冲方式、应收冲减方式；
7. 外币与结汇处理。

产出：在 `## Answer` 中按子域逐节记录"老系统做法 + 值得借鉴 / 应抛弃（含理由）"，给 10–13 号 ticket 引用。

## Answer

证据主源：`server/prisma/schema.prisma`（行号指该文件）、`server/src/routes/*`、`server/src/services/finance/*`、`server/src/lib/shipment-billable-status.ts`、`docs/architecture/modules/*.md`。老系统前端为 Next.js，业务逻辑集中在 `server/src`（Express + Prisma）。

### a. 客户/供应商对账

**客户对账单** — `CustomerStatement`（schema.prisma:5914）+ `services/customer-statement.service.ts`（`buildStatement` 是唯一取数真相）+ `routes/customer-statement.ts`。
- 取数口径：**发货单 ShipmentItem 明细**（非签收单、非出库单）。状态口径用两份名单：`UNAPPROVED_SHIPMENT_STATUSES`（DRAFT/PENDING_APPROVAL 不算钱）+ `SHIPPED_STATUSES = ['SHIPPED','DELIVERED','RECEIVED','SIGNED','COMPLETED','CONFIRMED']`，另有 `onlyShipped` 开关进一步过滤。签收（DELIVERED）只是发货状态之一，不是独立口径。
- 金额：`itemAmount = item.amount（为 0 保留 0，支持赠品/样品）否则 qty×unitPrice`，加 `shipment.extraFee`，round2。
- 生成时冻结完整 JSON `snapshot` + 生成条件 `sourceFilter`；预览与生成共用同一套计算。状态机 `DRAFT→PENDING_APPROVAL→APPROVED→SENT→CONFIRMED`，`REJECTED` 可改再提，`VOID` 终态；审批走 Universal Workflow（`BOSS_ONLY` 策略）。
- `paidAmount` 落库恒 0 占位——"累计已收的准确口径属方案二（三段式应收引擎）"（routes/customer-statement.ts:38），**未实现**。

**供应商对账** — `Reconciliation` / `ReconciliationItem` / `SupplierReconProfile`（schema.prisma:5844–5912）+ `routes/reconciliation.ts`。
- 取数口径：**StockIn（收货入库）**。AI 解析供应商 Excel（表头识别 + 指纹缓存），与入库记录做**四层匹配漏斗：L1 确定键 / L2 高分自动 / L3 AI 仲裁 / L4 待定**；行级留存 `matchTier`/`confidence`/`evidence`。
- 状态 `DRAFT→SUBMITTED→CONFIRMED→PAYMENT_PENDING→PAID`，confirm 后可直接生成付款申请（`paymentRequestId`）；`SupplierReconProfile` 沉淀列模板/别名库/计价规则。
- 完整向导流程见 `docs/architecture/modules/reconciliation.md`（parse→compare→POST /records→confirm→payment 全程落库）。

- **值得借鉴**：明细级取数 + 快照冻结 + sourceFilter 可追溯；预览/生成同源；行级匹配证据；供应商对账画像记忆；对账→付款直连。
- **应抛弃/警惕**：客户名松散匹配出 `temp-` stub（代码里已多处打补丁拦截）；`paidAmount` 占位 0 不可信；快照与底层发货单会脱节（by design）；AI 表头解析脆弱（docs 自评"incorrect matching / marker drift"为主要风险）。新栈应要求对账单挂正式客户外键、取数口径全局唯一常量（老系统正是因白名单散落 8 处而出的 P0，见 `lib/shipment-billable-status.ts` 头注释）。

### b. 收款/付款与核销

- **收款**（`routes/financial.ts:1434` `POST /invoice-requests/:id/receive`）：核销粒度 = **开票申请 InvoiceRequest**，支持**部分收款**（显式 amount 或默认收剩余；金额按整数分比较防浮点误差；收满才置 COMPLETED）。Serializable 单事务四写：Transaction（资金流水）+ FinanceAccount 余额 + FinancialEntry（IN_CUSTOMER, CONFIRMED）+ LedgerEntry（DEBT_DEC/INCOME → EntityStanding）。幂等判据 = LedgerEntry 求和与 FinancialEntry 求和**取 max**（因台账全量重建会改 sourceId，只认其一曾致全额重复收款 P0）。
- **付款**（`PaymentRequest` schema.prisma:3977 + `financial.ts` pay/revoke-payment）：申请（type=MATERIAL/EXPRESS/SAMPLE/RENT/OTHER，公/私账，加急级）→ 审批 → 执行时**条件更新扣余额**（`accountWrite.count!==1` 即余额不足）→ FinancialEntry(OUT_SUPPLIER) → **进项发票 `settleInvoicePayment(apply)` 同事务核销** → 置 PAID + 联动 `StockIn.isPaid`。核销粒度 = 付款申请，发票经 `paymentRequestId` 挂靠。
- **发票↔合同分摊** `InvoiceContractAllocation`（schema.prisma:4138）：发票直接锚定合同（source=SHIPMENT 自动推出 / MANUAL 人工认领），`lib/invoice-contract-allocation.ts` 守门保证"一张发票的分摊合计==票面额"。
- **往来台账**：`EntityStanding`（receivable/payable/received/paidTotal + balance，正=对方欠我）+ `LedgerEntry`（DEBT_INC/DEBT_DEC，原币+汇率+baseAmount）。`services/ledger.service.ts`：`recordEntry` / `recordReturnReversal`（退货不计 receivedTotal）/ `recalculateStanding`（**deleteMany 后按 FinancialEntry 全量重建**——危险操作，sourceId 会漂移）。
- **预收/预付**：**无独立实体**。balance 转负即"客户贷方/预收余额"（ledger.service.ts:234；balance-sheet.service.ts:48 自注"本阶段不拆分处理"）；`lib/credit-guard.ts` 对负 outstanding 不夹 0。AI 核销推荐（`services/ai-accounting.service.ts`，FIFO 推荐分配）因状态口径分裂，注释自认"匹配到的发货单恒为空"——死亡功能。

- **值得借鉴**：四写原子 + 整数分比较 + 部分收/付款剩余额模型 + 发票-合同分摊守门 + 余额条件更新。
- **应抛弃**：**三本账冗余**（Transaction / FinancialEntry / LedgerEntry 各记一遍，靠补丁对齐）；**台账全量重建**（recalculateStanding 删了重算，是多次 P0 的根因）——新栈应只留一种分录账，append-only、不重建；预收预付应有独立实体/科目而非负余额重载。

### c. 开票登记字段与流程

- 销项：`InvoiceRequest`（schema.prisma:3928）：displayId（INV-REQ-*）、totalAmount/**deductionAmount 扣款**/taxRate/taxAmount、status PENDING→APPROVED→REJECTED→COMPLETED、AI 风控 `aiRiskScore`/`aiAnalysis`（JSON：信用风险、未决退货数）、挂 `shipments[]`、attachments、`extensionData` 动态表单。开票产出 `Invoice`（schema.prisma:4075）：number/code 联合唯一（租户内）、type OUTPUT/INPUT、`amount`+**`receivedAmount` 累计已收（分期收款，收齐才 PAID）**、OCR（ocrData/checkCode/buyerName/sellerName）、`deduction`/`deductionRemark` 客户扣款、attachments。
- 进项：Invoice type=INPUT 挂 PaymentRequest，付款事务内核销（见 b）。
- **值得借鉴**：申请（业务流程）与发票登记（法定凭证）分离；进项/销项同表分型；发票-合同分摊；AI 风控与 OCR 留痕；receivedAmount 累计已收字段。

### d. 资金账户、收支记录、账龄算法

- `FinanceAccount`（schema.prisma:3907）：type BANK/CASH/ALIPAY/WECHAT、balance、**foreignBalance 外币余额 + bookRate 账面加权汇率**。
- `FinancialEntry`（schema.prisma:5419）：type IN/OUT，category 细分（IN_CUSTOMER/OUT_SUPPLIER/OUT_REIMBURSE/OUT_DAILY/OUT_SALARY/OUT_TAX/OUT_FX_GAIN/OUT_FX_LOSS…），party（CLIENT/SUPPLIER/EMPLOYEE）、refType/refId 关联业务单据、公私账标记、status PENDING/CONFIRMED/VOIDED、exchangeRate/baseAmount、settlementId 回指结汇——付款侧已"单轨化"为台账唯一真相源（financial.ts:2048 注释）。
- **账龄**：`services/finance/aging.util.ts` `computeAgingWithSettlement`——**FIFO 冲抵**（DEBT_DEC 消耗最老的 DEBT_INC），分桶 0-30/31-60/61-90/90+，按主体分别计算后 `sumBuckets` 聚合（**不允许跨主体互抵**）；基于 LedgerEntry.transactionDate。另有 `finance-cockpit`（驾驶舱）与 reports/（资产负债表、现金流量表、利润表、fx 敞口）。
- **值得借鉴**：FIFO 账龄 + 分主体不互抵；FinancialEntry 的 category/party/refType 维度设计；外币账户双余额。
- **应抛弃**：金额全用 **Float**（逼得全代码到处"整数分"补丁）——新栈必须 DECIMAL/整数分；PENDING/CONFIRMED/VOIDED 与台账重建并存的复杂状态补偿。

### e. 报销与工资模型（质检次品扣款联动）

- `Reimbursement`（schema.prisma:4199）：AI 智能报销——发票指纹 `invoiceParams` 查重、`riskLevel/riskReason` 风控、关联合同/产品/发样项、PENDING→APPROVED/REJECTED→PAID、软删（资金与审计痕迹保留）、extensionData；另有 reimbursementImport 批量导入。
- 工资三件套：`SalaryStructure`（FIXED/COMMISSION/PIECE_RATE、底薪/绩效/补贴/**压工资 withheldSalary**、commissionRate 百分数口径——曾因 0.05 vs 5 注释错误放大 100 倍、考勤/社保豁免、workCycle DAYS_26/22/30）→ `CompanyPayrollPolicy`（按事业部 ALL/TRADE/FACTORY，考勤扣款旋钮、`leaveTypeRates` 假期扣款系数、发薪日）→ `ManualAttendance`（月汇总/逐天 JSON）→ `PayrollRecord`（应发/实发、attendanceDeduction/**otherDeduction 单一标量**、提成/计件、companyCost、DRAFT→CONFIRMED→PAID）。
- **质检扣款联动**：`InspectionConfirmation`（responsibility=OPERATOR、confirmedAt、revokedAt、`payrollSyncedAt`）→ `services/inspection-confirmation.service.ts`：`getPayrollSummary` 按挡车工聚合次品数（未识别 worker 的逐条进 unassigned 区可追溯），`draftPayroll` 按日幂等标记 payrollSyncedAt。**但注释明确：PayrollRecord.otherDeduction 只是汇总标量，无"按员工×日期扣款行"实体**，扣款明细挂不上，靠人事对照手工录入——半自动半人工（inspection-confirmation.service.ts:371–383）。
- **值得借鉴**：次品责任确认 → payrollSyncedAt 幂等标记 → 工资同步这条链路模式；AI 查重/风控前置。
- **应抛弃**：扣款只进 otherDeduction 标量——新栈应做**工资扣款明细行实体**（可挂次品确认单 ID）；提成比例"百分数存储"口径混乱要引以为戒（定死小数或百分比其一）。

### f. 退货单：库存回冲与应收冲减

`ReturnOrder`/`ReturnItem`（schema.prisma:7536–7590）+ `services/return-order.service.ts` + `routes/return-order.ts`。老系统这套是**全财务域完成度最高**的：
- 状态 `DRAFT→ARRIVED→QC_DONE→FINANCE_CONFIRMED→CLOSED/CANCELLED`；returnType SALES（客户退）/PURCHASE（退供应商）；reason QUALITY/WRONG/EXCESS/CUSTOMER/OTHER；ReturnItem 带 `sourceShipmentItemId` 血缘（校验退量≤发量）、unitPrice 默认带原发货单价、amount 冗余存储。
- **财务确认单事务**：① LedgerEntry 冲减——SALES 冲客户应收（`recordReturnReversal`：不计 receivedTotal；已收款客户自然转贷方/预收），PURCHASE 冲供应商应付（永不记库存侧应付，C3 决策④）；② `createReturnStockIn`：type='RETURN'、status='COMPLETED' 入库单**回补库存**；③ 回写 `ledgerEntryId` + `stockInId` 供撤销回滚；④ `invoiceImpact` NONE/RED_NEEDED/RED_DONE 红冲标记。外币按**发货入账时汇率**折本位额冲减（防少冲/多冲）。
- **防超退**：refundAmount >（源发货台账金额 − 已退整单额）→ 拒（service:76–149）。撤销 = 软删回补入库单 + 反向台账分录，同事务。
- **值得借鉴**：整套——血缘明细、防超退上限、ledgerEntryId/stockInId 回写可撤销、红冲标记、"钱/货/账"同事务。可直接作为新栈 ReturnOrder 设计范本。
- **警惕**：曾有 P0——items 血缘为空时生成 0 明细入库单，"钱冲了货一克没进库存"（service:392 注释）；新栈需对"无血缘退货"显式建模而非放行。

### g. 外币与结汇

- `ExchangeRate`（schema.prisma:7776）：rateType **SPOT 即期 / SETTLE 结汇牌价 / MONTH_END 期末** 三率分离，source MANUAL/ER_API，按租户+币种对+类型+生效日唯一。
- `FinanceAccount.foreignBalance` + `bookRate`（账面加权汇率）。
- `ForexSettlement`（schema.prisma:7791）+ `services/finance/forex-settlement.service.ts` + `forex.util.ts`：confirm 单事务——外币户 foreignBalance/balance 同步减、人民币户加 netCredit（毛额−费）、自动生成 FinancialEntry（IN_FX_GAIN/OUT_FX_LOSS，settlementId 回指）；`fxGainLoss = 原币×(结汇牌价 − 账面加权汇率)`。
- 所有分录（LedgerEntry/FinancialEntry）均存**原币 + exchangeRate + baseAmount**，EntityStanding 按 baseAmount（本位币）汇总；reports/fx-exposure.service 出敞口报表。
- **值得借鉴**：bookRate 加权汇率 + 汇兑损益自动分录 + 三率分离 + "原币/本位币双金额"贯穿全部分录——新栈财务域应原生保留这一设计。

### 给 10–13 号 ticket 的三条总纲

1. **一种分录账**：老系统 Transaction/FinancialEntry/LedgerEntry 三本账冗余 + recalculateStanding 全量重建，是绝大多数 P0 的根因；新栈只保留 append-only 分录账（含原币/汇率/本位额、refType/refId、冲销与红冲标记），永不删除重建。
2. **口径常量化**：发货"算不算钱"（billable）、对账取数、账龄冲抵（FIFO、不跨主体）必须收敛为单一共享常量/服务；金额一律 DECIMAL/整数分。
3. **可完成的模型直接抄**：ReturnOrder（血缘+防超退+可撤销冲减+红冲）、InvoiceContractAllocation（分摊守门）、ForexSettlement（加权汇率+损益自动分录）、四层对账漏斗、payrollSyncedAt 幂等联动模式；需重新设计的：预收/预付实体、工资扣款明细行、客户对账单 paidAmount（三段式应收引擎）。
