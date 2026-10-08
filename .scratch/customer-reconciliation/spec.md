# 财务域 - 客户对账/收款核销/发票申请/汇率主档（W3 资金闭环·应收侧）

Status: ready-for-agent（10 号 grilling 三轮 17 问全部定稿，可按 session 施工）

## Problem Statement

W3 资金闭环的应收侧。老系统财务的 P0 根因（17 号研究）：三本账冗余（Transaction/FinancialEntry/LedgerEntry 各记一遍靠 `recalculateStanding` 删库重建）+ "三段式应收"（对账单挂应收、paidAmount 恒 0 烂尾）。新栈 ADR-0037 已锁定 finance 域骨架：唯一 append-only 经营分录账、包结构、错误码段 020–029、菜单 10700 系。

本 spec 落地应收侧四件事：**客户对账单与确认**（对接 14 号门户）、**收款单**（02 号审批过账 + 核销 + 预收）、**发票申请与登记**（不对接金税）、**汇率主档**（域公共，12 号复用）；并把"应收何时成立"钉死为**发货审核**（Q1=A 口径），以 business.shipment 事件化作为前置改动。应付侧（供应商对账/付款）由 11 号 vendor-reconciliation 并行定稿，payment 共表按其冲突清单收口（见"共享件收口清单"章）。

## Grilling 定稿决策（10 号 ticket，Q1–Q17）

1. **Q1 应收成立点=发货审核**：发货单 COMPLETED（审核出库）同事务在经营分录账追加 AR_INC 分录（逐明细行 + 运费头级行），红冲追加 AR_INC_REVERSAL。对账单只是期间汇总+确认+打印凭证，不产生应收；账龄起算日=发货日；13 号退货冲减直接反向分录。business 侧发 Spring 事件（`ShipmentAuditedEvent`/`ShipmentCancelledEvent`），finance 监听记账，business 不反向依赖 finance。
2. **Q2 汇率主档归本票**：`lunaerp_finance_exchange_rate` 占 020 域公共段，一期只做 SPOT 即期牌价（租户+币种+生效日唯一，RMB 内置 rate=1 不建行）；应收按发货日生效汇率折本位币；**核销严格同币种**；收款与应收本位币差额一期不做汇兑损益分录，往来查询以"汇率差异"只读列展示。
3. **Q3 收款单强制审批**：`biz_type=CUSTOMER_RECEIPT` 增量写入 02 号清单；APPROVED 事件 handler 同事务过账（账户余额条件更新+分录+核销生效）；APPROVING 不冻结应收但提交时校验可核销余额，过账条件更新防重复核销；预收=同单 receipt_type=ADVANCE 分型同审批；已过账唯一逆向=红冲（反向分录+核销自动重开），不做删除。
4. **Q4 发票不接审批引擎**：内置状态机 DRAFT→SUBMITTED→ISSUED/REJECTED，登记回填发票号即财务复核点（独立 `:issue` 权限+日志）；字段按数电票为主兼容旧票（20 位号码必填、代码可空）；单头单税率（字典 13/9/6/3/1/0/免税，纺织默认 13%、外贸 0%），价税合计取所选发货单汇总，不含税额/税额服务端反算；开票≠收款，发票不参与核销与账龄。
5. **Q5 域公共表由本票落地**：`ledger_entry`/`account`/`exchange_rate` 三表 DDL 随本票 SQL 交付；**资金流水=分录账 account_id 非空的账户维度视图、客户往来=party 维度视图，不建第二本账**；11/12 号只消费+扩列（12 号扩外币双余额/账面加权汇率）。
6. **Q6 客户档案+5 可空列**：开票抬头/开票地址/开户行/开户行账号（同步 04 号 Excel 导入模板 +4 列）+ `credit_days` 信用账期天数（空=不设账期）；**账期预警 job 不做**（Q6 第 3 点否决）：04 号冻结模板 `LUNA_AR_PERIOD_WARNING` 一期不接线，逾期仅账龄页静态筛选。
7. **Q7 对账单口径**：按 客户+币种+起止日期 生成（多币种客户分单生成；区间手工选，缺省上一自然月）；取数=区间内**发货日**落区间的已审核发货明细分录**不看签收状态**（sign_status 仅展示标记）；行=完整往来账（期初欠款/本期借方=发货分录/本期贷方=收款核销+预收转核销+退货冲减/期末=期初+借−贷，服务端重算）；一条分录最多被一张 CONFIRMED 对账单占用（行级占用），DRAFT 可手工追加未被 CONFIRMED 占用的历史明细行；红冲单按分录净额自然呈现。
8. **Q8 对账单状态机不接审批**：DRAFT→SENT→CONFIRMED（终态快照冻结）；SENT→REJECTED（必填异议原因）→改后回 DRAFT 重新发出；非终态可 VOID（释放行占用，不删分录）；CONFIRMED 不可作废。确认后源单红冲/退货**不回改不阻塞**，冲减发生在下期对账单；SENT 期源单红冲门户行标红，客户可据此 REJECTED。
9. **Q9 门户契约冻结**：`/portal/finance/statement/` 前缀（list/{id}/PUT confirm/PUT reject），客户身份由 14 号 token 注入、本票只认 customerId 并做隔离专测；内部"**代客确认**"（`:confirm-on-behalf` 权限，必填线下方式+备注）与门户确认落同一状态字段；对账单打印二维码 `{luna.portal.base-url}/portal/statement?no={对账单号}`，基址未配置不阻断打印；异议可回复备注后重新 SENT。
10. **Q10 核销模型**：核销行落 payment 共表子表（见数据模型），核销本身写分录（RECEIPT_WRITE_OFF/ADVANCE_WRITE_OFF）；收款单=头+拟核销分配数组；三视角选单（CONFIRMED 对账单/发货单/合同，底层都拆到发货明细分录）+ 预收余额作可分配来源；"自动分配"按发货日 FIFO 凑平可改可删；守卫：严格同币种、单行核销≤该分录未核销余额、合计≤收款额、过账条件更新防重复核销；预收转核销不建新单（在原 ADVANCE 单上追加核销行，`:writeoff` 权限不审批）；红冲=反向分录+账户回加+核销行全部自动重开。
11. **Q11 账龄五桶**：0–30/31–60/61–90/91–180/181+；FIFO 冲抵（应收减少分录按时间消耗最老的应收增加分录）、按客户分别计算不跨客户互抵、纯函数实时计算不落快照；预收余额单列不进桶；汇总页按客户一行（合计/五桶/预收/其中已逾期=发货日+credit_days<查询日），可下钻未核销明细；金额走 `@Sensitive(AMOUNT)`；不做导出 Excel/坏账计提/预警 job。
12. **Q12 发票锚定发货单**：多选 COMPLETED 发货单（同客户同币种，禁混选），快照客户开票四要素；支持部分开票（行带"本次开票金额"，默认=该单未开票余额），守卫同一发货单累计开票（ISSUED+SUBMITTED 在途聚合）≤ total_amount+freight_fee；不建发票-合同分摊表（经 shipment.sales_contract_id 只读聚合）；ISSUED 回填发票号/日期/附件；开票与收款核销完全解耦；发票不做打印模板。
13. **Q13 前端落 `lunaerp-elmui/finance/`**（临时区，与 W1/W2 模块一致；菜单/权限不随目录变化）；四个页面目录：customer-reconciliation/（列表+工作台+账龄汇总页）、payment/、invoice-application/、exchange-rate/。
14. **Q14 单号**：对账单 `CS{yyyy}{4位流水}`、收款单 `RC-{yyyyMMdd}-{3位日流水}`（OUT 侧付款单前缀 FK-，由 11 号采纳）、发票申请 `IA{yyyy}{4位流水}`；核销行不编号；取号按 ADR-0004（含软删行扫描+撞号重试+可手填）。
15. **Q15 13 号依赖边与冲减契约**：13 号票加 Blocked by 10；冲减=**直接追加反向分录**（不做红字对账单），冻结接口 `LedgerService.recordArReturnReversal(...)`（category=AR_RETURN_REVERSAL，按原发货入账汇率折本位额），13 号不得自建应收台账/余额表；发货事件契约为本票第 0 前置 session。
16. **Q16 菜单收口**：10740 收付款页面=三 Tab（收款｜付款申请｜付款登记），收款 Tab 本票实做、OUT 两 Tab 11 号实做；**新增 10780 汇率**二级菜单（sort 8，parent 10700）回写 ADR-0037；按钮权限位见字典/菜单章。
17. **Q17 资金账户期初**：`opening_balance` 仅创建时录入一次（此后只读），余额只由单据过账经条件更新改变；一期不做手工调账单/账户间转账（12 号若需另开决策）；账户启用/停用两态，停用不可被新收款单选，有分录引用不可删。

## Solution

`lunasrv-module-lunaerp` 的 finance 域落地 **5 张新表 + 2 张共享表 IN 侧扩列 + 客户 5 列**：

1. `lunaerp_finance_ledger_entry` 经营分录账（域公共，append-only，唯一账本）；
2. `lunaerp_finance_account` 资金账户（域公共最小集，12 号扩列）；
3. `lunaerp_finance_exchange_rate` 汇率主档（域公共，12/13 号复用）；
4. `lunaerp_finance_statement` + `lunaerp_finance_statement_item` 客户对账单主表/行快照；
5. `lunaerp_finance_payment`（11 号 OUT 定稿共表，IN 侧扩列）+ `lunaerp_finance_payment_ref`（核销行，ref_type 扩值）；
6. `lunaerp_finance_invoice_application` + `lunaerp_finance_invoice_application_item` 发票申请主表/行；
7. `lunaerp_baseinfo_customer` +5 可空列。

配套：business.shipment 审核/红冲事件化（前置 session）、02 号审批清单增量、CS/RC/IA 取号、对账单打印接 04 号内核、门户 4 端点契约（14 号接页面）、字典 2272–2278、菜单 10720/10740 按钮段 + 10780 新增、错误码 020/021/023(100–199)/026 细分。

## 前置改动（已落地模块，第 0 个前置 session）

不回改 shipment-mvp / customer-info-module / approval-engine 历史 spec，改动在本 spec 承载，均为可空加列/纯增量事件，对存量功能零影响。

### business.shipment 事件化（应收成立点，Q1/Q15）

- `ShipmentAuditedEvent`：DRAFT→COMPLETED 审核事务内 publish（`ApplicationEventPublisher`，BEFORE_COMMIT 同事务语义，对齐 02 号契约）。载荷：shipmentId/shipmentNo/customerId/currency/shipmentDate/freightFee + 明细行快照数组（shipmentItemId/productName/quantity/unit/unitPrice/amount）。
- `ShipmentCancelledEvent`：COMPLETED→CANCELLED 红冲事务内 publish，载荷含原分录回指所需行（shipmentItemId+amount+currency）。
- finance 侧 listener 写 AR_INC（逐明细行一条 + freightFee>0 时头级运费一条，refType=SHIPMENT_ITEM/SHIPMENT_FREIGHT）/AR_INC_REVERSAL（reversal_of_id 回指原分录）；记账失败同事务回滚发货审核。
- business 不 import finance 任何类；事件载荷用自有 DTO 放 business 包（或横切 event 包），与 02 号"业务侧自过账"方向相反但同构（business 发、finance 收）。

### 客户档案（`lunaerp_baseinfo_customer` +5 可空列）

| 列 | 类型 | 说明 |
|---|---|---|
| invoice_title | varchar(256) | 开票抬头，缺省取公司名 |
| invoice_address | varchar(512) | 开票地址（电话可并入备注） |
| bank_name | varchar(256) | 开户银行 |
| bank_account_no | varchar(64) | 开户行账号 |
| credit_days | int4 | 信用账期天数，空=不设账期 |

- 客户表单/详情加可空输入（"开票信息"区块）；税号**不新增列**，发票单直接用 `unified_social_credit_code` 带出、单上可改快照。
- 04 号 common-foundation 客户 Excel 导入模板同步 +4 开票列（credit_days 属业务策略字段不进导入模板）。

### 02 号审批引擎增量

- 强制审批清单 +`CUSTOMER_RECEIPT`（收款单）；`lunaerp_approval_biz_type` 字典与默认链配置项随本票 SQL 插入；提交/撤回/审批复用 02 号既有端点与 `approval_status` 字典（11 号冲突清单 #7 同口径）。

## Implementation Decisions

### 模块与包路径（ADR-0037 骨架）

- 后端：`controller.finance.{customerrecon,payment,invoice,exchangerate,account,ledger}` + `service.finance.<同>` + `dal.mysql.finance.<同>` + `dal.dataobject.finance.<同>`；错误码常量入 `enums.finance.FinanceErrorCodeConstants`（独立文件）。
- URL 前缀 `/lunaerp/finance/<子模块>/...`；权限 `lunaerp:finance:<子模块>:<动作>`；表前缀 `lunaerp_finance_*`；金额一律 DECIMAL(18,2)、汇率 DECIMAL(20,8)，守卫比较整数分。
- 前端：`luna-ui/src/views/lunaerp-elmui/finance/{customer-reconciliation,payment,invoice-application,exchange-rate}/`，API 镜像 `api/lunaerp-elmui/finance/`（Q13）。

### 数据模型

**表 1 `lunaerp_finance_ledger_entry` 经营分录账**（域公共，append-only：无 update/delete 端点，应用层禁改）：

| 字段 | 类型 | 说明 |
|---|---|---|
| biz_date | date | 业务日期（AR=发货日；收款=收款日；核销=操作日） |
| party_type / party_id | varchar(16) / int8 | 一期 CUSTOMER 固定 / customer_id |
| account_id | int8 可空 | 资金账户（非空即资金流水视图行） |
| category | varchar(32) | 分录类别，字典 2274（见下） |
| direction | varchar(8) | DEBIT 往来增加 / CREDIT 往来减少 |
| currency / exchange_rate / original_amount / base_amount | varchar(8) / numeric(20,8) / numeric(18,2) / numeric(18,2) | 原币+汇率+本位额（ADR-0037 决策 1） |
| ref_type / ref_id / ref_item_id | varchar(32) / int8 / int8 可空 | 业务回指：SHIPMENT / SHIPMENT_ITEM / SHIPMENT_FREIGHT / RETURN_ORDER |
| reversal_of_id | int8 可空 | 红冲回指原分录 |
| remark + 审计字段 | — | TenantBaseDO；不提供软删 |

- 索引：(tenant_id, party_type, party_id, biz_date)、(tenant_id, ref_type, ref_id)、(reversal_of_id)、(tenant_id, account_id, biz_date)。
- category 值域（一期全集，11/12 号复用不另立）：AR_INC / AR_INC_REVERSAL / RECEIPT（收款入账）/ RECEIPT_REVERSAL / RECEIPT_WRITE_OFF / ADVANCE_WRITE_OFF / AR_RETURN_REVERSAL（13 号）/ OUT_SUPPLIER（11 号）/ OUT_SUPPLIER_REVERSAL。

**表 2 `lunaerp_finance_account` 资金账户**（域公共最小集，Q5/Q17；12 号扩外币双余额/账面加权汇率互写引用）：

| 字段 | 类型 | 说明 |
|---|---|---|
| account_name | varchar(64) | 租户内唯一 |
| account_type | varchar(16) | 字典 2277：CASH/BANK/WECHAT/ALIPAY |
| currency | varchar(8) | 账户币种（一期账户单币种） |
| opening_balance | numeric(18,2) | 仅创建可写（Q17） |
| current_balance | numeric(18,2) | 冗余余额，仅经单据过账条件更新 |
| status | varchar(16) | ENABLE/DISABLE，停用不可被新收款单选 |

- 余额变更唯一通道：收款过账 `UPDATE ... SET current_balance = current_balance + #{amt} WHERE id=#{id} AND status='ENABLE'`；红冲反向；无任何直接改余额端点。

**表 3 `lunaerp_finance_exchange_rate` 汇率主档**（SPOT，Q2）：

| 字段 | 类型 | 说明 |
|---|---|---|
| currency | varchar(8) | 外币；唯一索引 (tenant_id, currency, effective_date) |
| rate | numeric(20,8) | 1 外币 = rate 本位币；RMB 不建行，代码内置 rate=1 |
| effective_date | date | 生效日；取价=生效日≤业务日的最新一条且 ENABLE |
| status | varchar(16) | ENABLE/DISABLE（失效=停用，无删除，Q16） |

**表 4/5 `lunaerp_finance_statement` / `lunaerp_finance_statement_item` 客户对账单**：

主表：statement_no（CS{yyyy}{4位流水}，唯一索引含软删行）、customer_id/customer_name 快照、currency、period_start/period_end、opening_balance/period_debit/period_credit/closing_balance（服务端重算落库快照）、status（DRAFT/SENT/CONFIRMED/REJECTED/VOID，字典 2272）、confirm_type（ONLINE_PORTAL/ON_BEHALF，字典 2278）、confirm_by_name/confirm_time/confirm_ip/confirm_method、reject_reason、reply_remark、remark、审计。索引 (tenant_id, customer_id)、(tenant_id, status)。

行表：statement_id/line_no、ledger_entry_id（被引用分录）、direction（DEBIT/CREDIT）、展示快照列（biz_date/ref_type/ref_no/summary 品名或事由/debit_amount/credit_amount/sign_status 标记）、remark。索引 (statement_id)、(tenant_id, ledger_entry_id)。期初/期末不落行（主表字段）；行级占用判定：ledger_entry_id 已被任一非 VOID 对账单占用即排除候选（CONFIRMED 强占用，DRAFT/SENT 弱占用提示，Q7）。

**表 6/7 `lunaerp_finance_payment` / `lunaerp_finance_payment_ref`（共享表，IN 侧扩列收口）**：

11 号 OUT 最小列集保留不动（payment_no/direction/supplier_id/customer_id/account_id/pay_amount/prepay_used_amount/status COMPLETED·REVERSED/remark/登记人时间）。IN 侧 `ADD COLUMN IF NOT EXISTS` 扩列：

| 扩列 | 说明 |
|---|---|
| receipt_type | varchar(16)，字典 2273：RECEIPT 货款 / ADVANCE 预收；OUT 侧恒空 |
| currency / exchange_rate | 收款币种与过账日汇率（OUT 一期人民币可空） |
| receipt_date / bank_ref_no / handler_name | 收款日/银行回单号/经手人 |
| approval_status | 五态（02 号跨切面字段），OUT 侧不走审批恒空 |

payment_ref 的 ref_type 扩值：OUT=PAY_APPLY（11 号，一期唯一）；IN=**AR_ENTRY**（ref_id=发货明细/运费 AR_INC 分录 id），IN 扩列：entry_no/biz_date 快照、base_amount 本位额快照、ledger_entry_id 核销分录回指（OUT 恒空）。不改 11 号结构（冲突清单 #2 收口）。索引沿用 + (tenant_id, ref_id)。

- IN 侧状态语义：approval_status 五态管审批（PENDING 草稿可删可改）；status：APPROVED 过账同事务置 COMPLETED，红冲置 REVERSED——不改 11 号 status 取值域。
- 未核销余额口径（常量化实时聚合）：某 AR_INC 分录未核销余额 = 该分录 original_amount − Σ(其 AR_INC_REVERSAL) − Σ(ref_type=AR_ENTRY 且指该分录且所属收款单 status=COMPLETED 的 allocate_amount) − Σ(AR_RETURN_REVERSAL 指 ref_item_id=该分录的行)。

**表 8/9 `lunaerp_finance_invoice_application` / `_item` 发票申请**：

主表：invoice_no（IA{yyyy}{4位流水}）、customer_id/customer_name、currency、tax_rate（字典 2276）、invoice_kind（字典 2275：数电专票/数电普票/增值税专票/增值税普票）、electronic bit、抬头四要素快照（invoice_title/tax_no=统一社会信用代码快照/address/bank_info，创建时从客户档案带出可改）、status（DRAFT/SUBMITTED/ISSUED/REJECTED）、official_invoice_no（登记回填，20 位）、invoice_code 可空、invoice_date、attachment_url、total_amount_excl_tax/total_tax/total_amount_incl_tax（服务端按税率反算）、issue_by/issue_time、submit_by/submit_time、remark。索引 (tenant_id, customer_id)、(tenant_id, status)、official_invoice_no 唯一（非空时，含软删行）。

行表：application_id/line_no、shipment_id/shipment_no 快照、shipment_amount 快照（total_amount+freight_fee）、amount（本次开票金额，默认=该单未开票余额可改小）。守卫：Σ(该发货单全部 SUBMITTED+ISSUED 申请行 amount) + 本次 ≤ shipment_amount（整数分，条件更新防并发超开，REJECTED 释放占用）。

**客户 +5 列**：见前置改动章。

### 取数与计算口径（常量化，纯函数）

- **对账单生成**：候选=区间内发货日落区间的该客户该币种 AR_INC 分录（含运费分录，未红冲或红冲后净额呈现）+ 区间内核销/退货贷方分录；期初=起始日前未核销 AR 余额；期末=期初+借−贷；生成后行快照冻结、源单零回写（对齐 11 号快照模式）。
- **FIFO 账龄**（Q11）：按客户独立计算；所有应收减少分录（RECEIPT_WRITE_OFF/ADVANCE_WRITE_OFF/AR_RETURN_REVERSAL）按发生时间从最老 AR_INC 消耗；账龄=查询日−该笔剩余额对应发货日，落五桶；逾期=发货日+credit_days<查询日（未设 credit_days 不判逾期）；预收余额（ADVANCE 入账−ADVANCE_WRITE_OFF）单列不进桶。实时聚合不落快照表（ADR-0017/0037 模式）。
- **汇率差异只读列**：核销行 base_amount（按收款入账汇率）与被核销 AR 分录 base_amount 等比额之差，往来查询展示，不生成分录（Q2 非目标）。

### 状态机与守卫（统一 DoD）

- **对账单** DRAFT→SENT→CONFIRMED / SENT→REJECTED→（修改）→DRAFT→SENT… / 非终态→VOID；CONFIRMED/VOID 终态。全部写端点状态白名单守卫（STATEMENT_STATUS_NOT_ALLOW），send/void/confirm 用条件 UPDATE 防并发。
- **收款单**：PENDING（草稿，可改可删进回收站）→提交→APPROVING→APPROVED（handler 同事务：账户条件更新+RECEIPT/ADVANCE 入账分录+核销行落库+status=COMPLETED）/REJECTED（可改重提，新建实例走全流程）/CANCELLED（撤回）。COMPLETED 唯一逆向=红冲（`:reverse`，反向分录+账户回加+核销行全部重开+status=REVERSED）。
- **发票申请**：DRAFT→SUBMITTED→ISSUED（回填发票号）/REJECTED（可改重提）；ISSUED 只读；不做红字/作废（非目标）。
- 删除进回收站仅限：对账单 DRAFT、收款单 approval_status=PENDING、发票 DRAFT；已过账/已发出单据走作废/红冲不进回收站（ADR-0038 边界）。RecycleBinHandler ×3 注册（bizType=STATEMENT/PAYMENT/INVOICE_APPLICATION），恢复不补账。

### API（前缀 `/lunaerp/finance/<子模块>`）

| 子模块 | 端点要点 |
|---|---|
| customerrecon | `/statement` page/get/create/update/delete；`PUT /send?id=`、`PUT /void?id=`、`PUT /confirm-on-behalf?id=`（必填 confirm_method）、`PUT /reject-reply?id=`（回备注）、`POST /add-entry`（DRAFT 手工追加未占用分录，守卫客户/币种一致+未占用）、`GET /prefetch-lines?customerId&currency&periodStart&periodEnd`（生成预览与追加选择器）、`GET /{id}/print-data`（打印 VO 豁免脱敏+@LogRecord+qrUrl）、`GET /aging/summary?agingDate&customerIds&salesmanId`、`GET /aging/detail?customerId&agingDate` |
| payment | `/payment` page/get/create（IN：customer/account/currency/amount/receipt_date/receipt_type/分配数组）/update/delete（仅 PENDING）；`PUT /submit?id=`（进 02 审批）；`PUT /reverse?id=`（红冲）；`POST /allocate-preview`（自动分配 FIFO 试算，不落库）；`POST /advance-writeoff`（paymentId=已过账 ADVANCE 单 + 分配数组）；`GET /ar-balance?customerId&currency`（未核销明细分录列表，三视角选单数据源）；`GET /account/simple-list`（收款选账户下拉，账户管理页归 12 号） |
| invoice | `/invoice` page/get/create/update/delete（DRAFT）；`PUT /submit?id=`、`PUT /issue?id=`（回填 official_invoice_no/invoice_date/attachment）、`PUT /reject?id=`（可空原因） |
| exchangerate | `/exchange-rate` page/get（query）/create/update（停用）；无删除 |
| account | `/account` page/get/create（opening_balance 仅此可写）/update（仅备注/状态）；无删除（有分录引用） |
| ledger | `/ledger/page`、`/ledger/customer-balance`（party 维度只读视图查询，含汇率差异列） |

**门户契约（14 号接页面，Q9）**：`/portal/finance/statement/` — `GET /list`（仅 SENT/CONFIRMED）、`GET /{id}`（含全部行）、`PUT /confirm`（SENT→CONFIRMED，记录 confirm_by_name/time/ip）、`PUT /reject`（必填 reject_reason）。客户身份 14 号 token 注入；本票实现按显式 customerId 并完成隔离专测（A 客户不可见/不可操作 B 客户单、不可遍历 id），14 号接入时仅替换身份来源。

### 审批集成（02 号契约）

- `biz_type=CUSTOMER_RECEIPT`：提交→APPROVING；`ApprovalCompletedEvent`（BEFORE_COMMIT）→ `CustomerReceiptApprovalHandler` 同事务过账（账户条件更新+入账分录+核销行+status=COMPLETED）；REJECTED/CANCELLED 无副作用（APPROVING 期间应收不冻结）。
- 提交时校验：分配合计≤收款额、逐行≤未核销余额（试算口径）；过账时逐行条件更新重验，冲突抛 RECEIPT_POST_CONFLICT（可重试）。

### 打印·脱敏·消息·回收站（04 号契约）

- 打印：模板 `FINANCE_STATEMENT` 注册 04 号内核，A4 纵向；print-data 独立 VO（豁免脱敏，含客户四要素/全量行/qrUrl），@LogRecord 留痕；QrSlot 二维码 `{luna.portal.base-url}/portal/statement?no={statement_no}`，基址未配置降级提示不阻断（与发货单/出口单证同规则）。发票不打印。
- 脱敏：对账单/收款/账龄/发票金额字段 `@Sensitive(AMOUNT)`；客户联系人 `@Sensitive(CONTACT)`；打印 VO 豁免。
- 消息：对账单 REJECTED 时通知制单财务（LunaNotifyHelper，失败不阻塞事务）；不做账期预警（Q6）。
- 回收站：见状态机章。

### 字典/菜单/权限/单号

- 字典（type id **2272–2278**，紧接 11 号 2269–2271；direction 字典 2271 共享复用；approval_status 复用 02 号）：

| id | type | 内容 |
|---|---|---|
| 2272 | lunaerp_finance_statement_status | DRAFT/SENT/CONFIRMED/REJECTED/VOID |
| 2273 | lunaerp_finance_receipt_type | RECEIPT 货款 / ADVANCE 预收 |
| 2274 | lunaerp_finance_ledger_category | 一期全集 9 值（见分录账 category） |
| 2275 | lunaerp_finance_invoice_kind | 数电专票/数电普票/增值税专票/增值税普票 |
| 2276 | lunaerp_finance_tax_rate | 13%/9%/6%/3%/1%/0%/免税 |
| 2277 | lunaerp_finance_account_type | CASH/BANK/WECHAT/ALIPAY（域公共，12 号复用） |
| 2278 | lunaerp_finance_statement_confirm_type | ONLINE_PORTAL / ON_BEHALF |

- 菜单（10720 客户对账、10740 收付款、10770 发票申请 ADR-0037 已锁；**新增 10780**）：

| id | 名称 | type | parent | 权限/说明 |
|---|---|---|---|---|
| 10720 | 客户对账 | 2 | 10700 | `:query` 挂菜单（ADR-0037） |
| 10721–10727 | 对账按钮 | 3 | 10720 | :create/:update/:delete/:send/:void/:confirm-on-behalf/:print |
| 10740 | 收付款 | 2 | 10700 | 三 Tab 页面（收款｜付款申请｜付款登记）；OUT 按钮 10741–10743 归 11 号 |
| 10745–10749 | 收款按钮 | 3 | 10740 | :create（含提交）/ :update（含撤回重提）/ :delete / :writeoff（预收转核销）/ :reverse（红冲） |
| 10770 | 发票申请 | 2 | 10700 | `:query` 挂菜单 |
| 10771–10774 | 发票按钮 | 3 | 10770 | :create/:update/:delete/:issue（reject 复用 :issue） |
| 10780 | 汇率 | 2 | 10700 | sort 8，path `exchange-rate`，component `lunaerp-elmui/finance/exchange-rate/index`，`:query` 挂菜单；10781 :create / 10782 :update |

- 单号（Q14，ADR-0004 取号含软删行+撞号重试+可手填）：CS/IA 按年 4 位流水；RC-{yyyyMMdd}-{3位日流水}；OUT 付款单前缀 **FK-**（11 号冲突清单 #3 收口采纳值）。
- 10710 资金账户菜单/管理页归 12 号；本票仅建表 + simple-list 端点。

### 错误码（`FinanceErrorCodeConstants`，登记 shared GLOSSARY 段位总表）

- `1-030-020` 域公共（段内分区 000 分录账 / 100 资金账户 / 200 汇率）：000 LEDGER_IMMUTABLE 分录账禁止修改或删除 / 100 ACCOUNT_NOT_EXISTS / 101 ACCOUNT_DISABLED / 102 ACCOUNT_NAME_DUPLICATE / 103 ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN / 104 ACCOUNT_OPENING_BALANCE_IMMUTABLE / 200 EXCHANGE_RATE_NOT_FOUND 无生效汇率 / 201 EXCHANGE_RATE_NO_DUPLICATE 同币种同生效日已存在 / 202 EXCHANGE_RATE_BASE_CURRENCY_FORBIDDEN 本位币无需配置
- `1-030-021` 客户对账：000 STATEMENT_NOT_EXISTS / 001 STATEMENT_STATUS_NOT_ALLOW / 002 STATEMENT_NO_PERIOD_ENTRIES 期间无可对账分录 / 003 STATEMENT_ENTRY_OCCUPIED 明细已被其他对账单占用 / 004 STATEMENT_CONFIRMED_NOT_VOID 已确认不可作废 / 005 STATEMENT_CURRENCY_MIXED 多币种需分单生成 / 006 STATEMENT_CUSTOMER_MISMATCH / 007 STATEMENT_PERIOD_INVALID 起止日期非法
- `1-030-023` payment 段：11 号已占 000–007（003 核销超额/004 余额不足/006 账户不可用为 IN/OUT 共享复用）；**IN 侧取 100–199**：100 RECEIPT_NOT_EXISTS / 101 RECEIPT_STATUS_NOT_ALLOW / 102 RECEIPT_CUSTOMER_MISMATCH / 103 RECEIPT_CURRENCY_MISMATCH 核销严格同币种 / 104 RECEIPT_ALLOCATE_EXCEED 分配合计超收款额 / 105 RECEIPT_POST_CONFLICT 过账并发冲突 / 106 RECEIPT_ADVANCE_BALANCE_NOT_ENOUGH 预收余额不足 / 107 RECEIPT_REVERSAL_NOT_ALLOW
- `1-030-026` 发票：000 INVOICE_NOT_EXISTS / 001 INVOICE_STATUS_NOT_ALLOW / 002 INVOICE_EXCEED_SHIPMENT_AMOUNT 累计开票超发货可开票额 / 003 INVOICE_CURRENCY_MIXED 发货单跨币种混选 / 004 INVOICE_CUSTOMER_MISMATCH / 005 INVOICE_OFFICIAL_NO_REQUIRED 登记必填发票号 / 006 INVOICE_OFFICIAL_NO_DUPLICATE / 007 INVOICE_TAX_RATE_INVALID

### SQL 脚本

新建 `luna-srv/sql/postgresql/lunaerp-finance-customer-recon.sql`（shipment 同体例：CREATE TABLE IF NOT EXISTS + 列注释 + 索引，单号唯一索引含软删行，可重复执行，仅语法自查禁止直连共享库）：

1. 域公共 3 表 DDL（ledger_entry/account/exchange_rate）+ 序列 + 索引；
2. statement 2 表 + invoice 2 表 DDL；`lunaerp_finance_payment`/`_ref` ADD COLUMN IF NOT EXISTS（IN 侧扩列）；
3. `lunaerp_baseinfo_customer` ADD COLUMN IF NOT EXISTS 5 列；
4. system_menu 10721–10727 / 10745–10749 / 10771–10774 / 10780–10782（先 DELETE 再 INSERT）；
5. system_dict_type 2272–2278 + dict_data（data id 自 3800 起按既有"DO 块判占用平移"幂等模式避让）；
6. `lunaerp_approval_biz_type` 字典 + CUSTOMER_RECEIPT 默认链配置项。

### 与并行 spec 的共享件收口清单（对齐 11 号冲突清单）

| # | 共享件 | 本 spec 立场 | 对齐对象 |
|---|---|---|---|
| 1 | `lunaerp_finance_payment` 主表 | 11 号 DDL 先行，本票 ADD COLUMN 扩 IN 侧 7 列 | 11 号（收口完成） |
| 2 | `lunaerp_finance_payment_ref` | ref_type 扩值 AR_ENTRY + IN 侧 4 扩列，不改结构 | 11 号 #2 |
| 3 | 单号前缀 | IN=RC-、OUT=FK-（建议值） | 11 号 #3 |
| 4 | 分录账 | 本票落地 ledger_entry DDL 与 LedgerService；11 号 OUT_SUPPLIER 分录消费同表 | 11 号 #4、12 号 |
| 5 | 资金账户 | **account DDL+AccountService 随本票落地**（10 号先于 12 号施工，11 号 #5 的 mock seam 替换为真件） | 11 号 #5、12 号 |
| 6 | 字典 2271 direction | 复用 11 号 | 11 号 #6 |
| 7 | 审批状态字典 | 复用 02 号 | 11 号 #7、02 号 |
| 8 | 错误码 023 段 | IN 侧取 100–199，复用共享 003/004/006 | 11 号 #8 |
| 9 | 10740 页面 | 三 Tab 组件由本票重构，OUT 两 Tab 实现归 11 号 | 11 号 |
| 10 | ledger category 字典 2274 | 本票建全集，11/12/13 复用不另立 | 11/12/13 号 |
| 11 | 汇率主档 | 本票落地，12 号扩 MONTH_END 期末牌价 | 12 号 |
| 12 | 13 号退货冲减 | recordArReturnReversal 冻结接口（Q15） | 13 号 |

## Testing Decisions

沿用纯 Mockito Service 单测接缝（Prior art：ShipmentServiceImplTest / 11 号 PaymentServiceImplTest），前端不做单测（ADR-0001）：

1. **分录账**：发货审核事件→逐行 AR_INC+运费分录断言（金额/汇率/本位额/ref 回指）；红冲→AR_INC_REVERSAL+reversal_of_id；分录不可变守卫（无 update 端点+应用层抛 LEDGER_IMMUTABLE）。
2. **对账单**：生成取数（区间/客户/币种过滤、签收前可对账、红冲净额呈现）；期初/期末重算；行占用守卫（已被 CONFIRMED 占用拒绝、手工追加校验）；状态机白名单+条件更新；多币种分单（STATEMENT_CURRENCY_MIXED）；作废释放占用。
3. **收款**：审批过账同事务（账户条件更新+入账分录+核销行）；余额不足/账户停用拒绝（复用 023-004/006）；严格同币种；分配合计≤收款额；**并发重复核销专测**（两事务过账同一分录，条件更新失败回滚 RECEIPT_POST_CONFLICT）；预收转核销（余额口径/守卫）；红冲（反向分录+余额回加+核销行重开+未核销余额恢复断言）。
4. **账龄**：FIFO 消耗顺序（跨月多笔收款/冲减）；五桶边界值（30/31/60/61/90/91/180/181）；跨客户不互抵；credit_days 逾期判定（空值不判逾期）；预收单列。
5. **发票**：超开守卫（跨申请聚合含 SUBMITTED 在途、REJECTED 释放）；币种混选拒绝；税额反算（金额逻辑必有测试）；登记发票号唯一；抬头快照带出与可改。
6. **汇率**：生效日取价（最新≤业务日、停用剔除、RMB=1）；缺汇率报 EXCHANGE_RATE_NOT_FOUND。
7. **门户契约**：确认/驳回状态流转与字段记录；**客户隔离专测**（横向越权 A↔B、遍历 id）；代客确认与门户确认同字段落库。
8. **租户与权限**：所有查询强制 tenant_id；横向/纵向越权必测（含 print-data、portal、aging）；权限码逐个断言。
9. **取号**：CS/RC/IA 格式、含软删行扫描、撞号重试、手填撞号报错。
10. **回收站**：三类 handler 仅 DRAFT/PENDING 进站、级联恢复、恢复不补账。

## Out of Scope（一期不做）

- 税盘/金税/电子发票平台对接、发票 OCR；发票打印模板与红字发票。
- 账期预警 job 与短信/企微外发（`LUNA_AR_PERIOD_WARNING` 一期不接线，逾期仅账龄页静态筛选）。
- 汇兑损益自动分录、结汇、MONTH_END 期末牌价（往来查询仅"汇率差异"只读列；12 号扩）。
- 账龄导出 Excel、坏账计提、账龄快照表。
- 资金账户手工调账单/账户间转账（12 号若需另开决策）；账户删除（仅停用）。
- 发票-合同分摊表、混合税率发票。
- 供应商侧全部（11 号）、报销/工资（12 号）、门户页面本体（14 号）。
- 门户异议即时通讯回复（仅文本备注）。

## 验收标准（统一 DoD 映射）

- [ ] 前置改动：shipment 两事件同事务发布且 finance 记账联动（审核→AR_INC、红冲→反转）；客户 +5 列 + 04 号导入模板 +4 列；02 号清单 +CUSTOMER_RECEIPT
- [ ] 5 张新表 + 2 张共享表 IN 扩列 + 客户加列，SQL 幂等脚本（DDL/菜单 10720·10740·10770·10780/字典 2272–2278/审批配置）
- [ ] 三状态机白名单+终态守卫+条件更新（对账单/收款/发票），收款过账与红冲的同事务四写测试
- [ ] 核销守卫全绿：同币种/超额/并发重复核销/预收转核销/红冲重开（金额逻辑必有测试）
- [ ] FIFO 账龄纯函数测试（五桶边界/跨客户隔离/credit_days 逾期/预收单列），汇总页+下钻可用
- [ ] 门户 4 端点契约冻结可测（列表/详情/confirm/reject）+ 客户隔离专测 + 代客确认同落库；对账单打印接 04 内核（print-data 豁免脱敏+@LogRecord+二维码降级）
- [ ] 芋道权限码逐位 + 租户隔离与横向/纵向越权测试（含 print-data/portal/aging）
- [ ] 回收站：RecycleBinHandler×3 注册，仅草稿进站、恢复不补账
- [ ] 术语回填 finance GLOSSARY（对账单状态机/核销/预收余额/账龄五桶/收款单/发票申请/汇率主档），错误码 020/021/023/026 回填 finance GLOSSARY 错误码节、shared 段位总表注记
- [ ] ADR-0037 增补注记（10780 汇率菜单、020 段域公共表 DDL 归属、10740 三 Tab）
- [ ] 共享件收口清单 12 项与 11/12/13 号 spec 无冲突
- [ ] 前端验收清单：建汇率→收款单（审批通过过账+自动分配核销）→预收转核销→生成对账单→发送→代客确认→打印（二维码）→账龄汇总下钻→发票申请→登记回填 全流程冒烟
- [ ] 常规列表 P95<1s（万级）；分录账聚合查询走索引（party+biz_date）

## Further Notes

### 跨票契约冻结（后续 spec 直接引用）

- **13 号销售退货**：审批通过 handler 同事务调 `LedgerService.recordArReturnReversal(customerId, shipmentItemId, returnQty, origAmount, currency, 原发货汇率快照, returnOrderId)` → category=AR_RETURN_REVERSAL、按原发货入账汇率折本位额、自动参与下期对账单贷方与 FIFO 消耗；13 号不得自建应收台账/余额表（其票已加 Blocked by 10）。
- **12 号资金/报销/工资**：消费 ledger_entry/account/exchange_rate/category 字典 2274；account 扩外币双余额与账面加权汇率、exchange_rate 扩 MONTH_END、报销/工资发放写分录+资金流水视图；不另建第二本账。
- **14 号客户门户**：`/portal/finance/statement/` 4 端点 + 二维码 URL `/portal/statement?no=` 已冻结；门户对账确认页仅接前端。

### 参考

- 17 号研究（老系统三本账/三段式应收根因、FIFO 账龄、预收重设计）：`.scratch/lunaerp-commercialization/issues/17-legacy-finance-research.md`
- ADR-0037（finance 域骨架）、ADR-0004（取号）、ADR-0017（流水不存余额）、ADR-0038（回收站边界）
- 11 号 vendor-reconciliation spec（payment 共表 OUT 侧与冲突清单）、02 号 approval-engine spec（事件/五态/handler 契约）、04 号 common-foundation spec（打印/回收站/脱敏/消息/Excel 模板）
