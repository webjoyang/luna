# 06: 客户对账单 — 生成/列表/详情/DRAFT 增删改

**What to build:** 按客户+币种+起止日期生成对账单（多币种客户分单生成；区间手工选，缺省上一自然月）。取数=区间内发货日落区间的已审核发货明细分录（不看签收状态，sign_status 仅展示标记）+ 区间内核销/退货贷方分录；期初=起始日前未核销 AR 余额；期末=期初+借−贷，服务端重算落库快照。一条分录最多被一张 CONFIRMED 对账单占用（行级占用），DRAFT 可手工追加未被 CONFIRMED 占用的历史明细行。红冲单按分录净额自然呈现。

**Blocked by:** 02 域公共内核 — LedgerService + AccountService + 发货事件 AR 记账

**Status:** closed

- [x] `controller.finance.customerrecon` + `service.finance.customerrecon`：`/statement` page/get/create/update/delete（DRAFT）
- [x] 生成取数：区间内发货日的 AR_INC 分录（含运费，未红冲或红冲后净额）+ 区间内 RECEIPT_WRITE_OFF / ADVANCE_WRITE_OFF / AR_RETURN_REVERSAL 贷方分录
- [x] 期初=起始日前未核销 AR 余额；期末=期初+借−贷，生成后行快照冻结、源单零回写
- [x] 行级占用判定：ledger_entry_id 已被任一非 VOID 对账单占用即排除候选（CONFIRMED 强占用，DRAFT/SENT 弱占用提示）
- [x] `POST /add-entry`（DRAFT 手工追加未占用分录，守卫客户/币种一致 + 未占用）
- [x] `GET /prefetch-lines?customerId&currency&periodStart&periodEnd`：生成预览与追加选择器
- [x] 多币种分单拒绝：STATEMENT_CURRENCY_MIXED；起止日期非法：STATEMENT_PERIOD_INVALID
- [x] CS 取号（CS{yyyy}{4位流水}，唯一索引含软删行）
- [x] 前端 `finance/customer-reconciliation/`：列表、生成向导（客户+币种+起止日期）、详情页（期初/本期借方/本期贷方/期末汇总 + 行表格）、DRAFT 编辑（手工追加分录）
- [x] 测试：取数过滤（区间/客户/币种、签收前可对账、红冲净额呈现）；期初/期末重算；行占用守卫（已被 CONFIRMED 占用拒绝、手工追加校验）；多币种分单；期间无可对账分录报错
