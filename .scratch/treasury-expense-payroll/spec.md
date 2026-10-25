# 财务域 - 资金账户/收支总览/报销/工资初稿（W3 资金闭环·资金侧）

Status: ready-for-agent

## Problem Statement

W3 资金闭环缺资金侧四块基础能力：① 多类型资金账户（现金/银行/微信/支付宝，含私户）与统一资金流水；② 面向老板的收支总览（资金视角 + 管理口径毛利参考）；③ 员工报销（费用类型+附件，审批通过即付款入流水）；④ 工资初稿（质检次品扣款联动 + 手工计件/工资项，审批后出纳发放登记入流水，不含考勤算薪）。

 grilling 两轮 + 与并行定稿的 10 号 spec 冲突仲裁后锁定（Q1–Q19），关键事实约束：

1. 老系统（linaagi）P0 根因是 Transaction/FinancialEntry/lunaLedgerEntry 三本账冗余 + 删库全量重建（17 号研究总纲①）。10 号 spec 已定稿并回填 GLOSSARY：**一张 `lunaerp_finance_ledger_entry` 承载资金流水与往来分录**（account_id 非空即资金流水视图）、`lunaerp_finance_account` 带 `current_balance` 冗余余额列、`lunaerp_finance_exchange_rate` 汇率主档（菜单 10780），三表 DDL 归 10 号交付。本 spec 是这些域公共件的**消费方 + 增补方**（ALTER 扩列、管理页、统一过账服务）。
2. **余额口径仲裁（Q18=A）**：沿用 10 号定稿的 `current_balance` + 条件更新；本 spec 新建 ADR-0043 把该口径与"一张分录账"一起钉死。11 号 spec 对账户服务的契约（扣款失败报 `023_004`、账户停用报 `020_101`）不变。
3. **汇率主档仲裁（Q19=A）**：表/接口/菜单 10780/错误码 020_200~202 全部以 10 号定稿为准；本 spec 只消费取价服务，MONTH_END 与结汇/汇兑损益全部二期。
4. 02 号审批 spec：报销（`biz_type=expense`）通过即写流水；工资（`biz_type=payroll`）"写工资发放流水"。grilling 裁定：**报销一步制**（通过同事务过账），**工资两步制**（通过 Handler 为空，发放登记才过账，对齐 11 号付款先例）。
5. 质检确认 MVP 明示不做 payrollSynced 标记、不写工资数据；只读 `payroll-summary` 口径 = confirmed_at 当日 + CONFIRMED + OPERATOR，REVOKED 天然排除，按 worker_user_id 聚合。本 spec 以**区间取数 + 指纹防重**消费该口径，不回写质检表。
6. 附件先例：infra 文件 URL 存 JSONB（样品分析 photos）；芋道字典不租户隔离（18 号研究），私有化一期可接受。

## Solution

在 finance 域落地 account（增量部分）/expense/payroll 三个子包，**9 张新表 + 2 处域公共表 ALTER 扩列**：

- `finance.account`：账户管理页（10 号仅建表+simple-list）、账户间转账、其他收支、统一过账服务、收支总览
- `finance.expense`：报销单主从（审批通过即写 OUT_EXPENSE 分录）
- `finance.expense` 附件：主表 attachments jsonb
- `finance.payroll`：工资单+员工汇总行+统一款项行+次品引用子表+发放批次（写 OUT_SALARY 分录）

核心规则：

- **一种分录账 + 单通道过账**：除红冲二期外只 INSERT；过账 = 条件更新 current_balance（防透支/停用）+ INSERT 分录，同事务；统一服务 `FinancePostingService.post(...)` 供 10/11/12/13 消费，他模块禁止直接操作账户余额或分录 Mapper。
- **余额真相**：current_balance 是受控制的物化缓存，经统一通道维护，无手工改余额端点；账户无分录时可改期初（10 号"仅创建可写"的页面级放宽，有分录后 020_104）；可用余额=current_balance 实时展示。
- **账户单币种**：currency 建户后不改；转账严格同币种；外币业务取 10 号 SPOT 汇率（业务日最近生效）快照到单据/分录，取不到报 020_200；RMB 内置 1。
- **报销一步、工资两步**：报销付款账户必填、一次全额；工资可多次分批发放（银行批量+现金尾款），Σ 发放=实发合计派生"已发清"。
- **次品扣款逐卷可追溯**：统一款项行 + 每卷一条 defect_ref 快照；指纹防重（已被非作废工资单引用的确认记录不再入导入，作废/删草稿释放）；不回写质检表、不引入 payrollSyncedAt。

## User Stories

### 资金账户与期初

1. 作为财务，我想在"财务 → 资金账户"维护多个账户：名称、类型（字典 2277 现金/银行/微信/支付宝）、账户性质（公户/私户）、账号或标识、币种（复用销售合同币种字典）、备注。
2. 作为财务，我想新建账户时录入期初余额（缺省 0，10 号表 opening_balance）；账户还没有任何业务分录时我可以修改期初，有分录后期初只读，改时系统报 020_104。
3. 作为财务，我想停用账户后它不再出现在收款/付款/报销/工资/转账/其他收支的下拉中，历史流水与统计不受影响；停用后可重新启用。
4. 作为财务，我想账户名称租户内唯一（含已删除行，020_102）；银行/微信/支付宝账号标识必填（020_105），现金可空；币种建户后不可改。
5. 作为财务，我想没有任何分录的账户可以删除（进回收站）；已有分录引用的账户禁止删除（020_103），只能停用。

### 资金流水与统一过账服务

6. 作为出纳，我想在账户上查看流水分页（业务日期、借/贷、类别 DictTag、往来主体、原币金额、汇率、本位币金额、来源单号、备注），可按账户/期间/类别/方向/主体/来源单号筛选，来源单号可跳转源单。
7. 作为 10/11/13 号等业务模块，我注入同一个 `FinancePostingService.post(req)`：校验存在/启用 → 条件更新余额（流出超额/停用失败）→ 按 10 号定稿字段写一条分录；任何模块不得自行 UPDATE 余额或 INSERT 分录。
8. 作为财务，我想所有写账动作（收/付款、报销、工资、转账、其他收支）在流水页用同一套类别标签（2274 全集）与 DEBIT/CREDIT 方向，资金口径全域一致。

### 账户间转账与其他收支

9. 作为出纳，我想登记账户间转账（银行提现、私户转公户）：转出/转入账户不可相同（020_107）、必须同币种（020_106）、填金额/日期/备注，提交即写两条 TRANSFER 分录并双边条件更新余额，生成 ZZ 单。
10. 作为出纳，我想登记无业务单据的其他收支：利息/废品/手续费/工本费/其他，选账户、方向、类别（类别与方向必须匹配，020_110）、金额、日期、备注，提交即写 IN_OTHER/OUT_OTHER 分录，生成 QT 单。
11. 作为财务主管，我想转账单与其他收支单登记后不可改删（写账凭证，错账红冲二期），列表可查、详情可看。

### 收支总览

12. 作为老板，我想在"财务 → 收支总览"选期间（缺省本月）：按账户看期初/本期流入/本期流出/期末（金额=opening 与 current_balance 及分录聚合），合计行汇总；下方按类别汇总本期借贷发生额，点类别可钻取流水。
13. 作为老板，我想同页看到"经营毛利参考（管理口径）"卡片：本期收入=区间内发货日口径的应收分录净额（AR_INC−AR_INC_REVERSAL，取 ledger_entry，事实由 10 号 shipment 审核事件化保证，12 号不复制发货白名单）；本期成本=确认日落区间的供应商已确认对账单 total_amount（11 号）；展示差额/毛利率，永久标注"管理参考、期间错配、非法定报表"。应收侧未上线（无 AR 分录）时收入区显示空态说明。

### 报销

14. 作为员工，我想新建报销单：报销人缺省我自己但可代他人填报、付款账户必填、多行费用（费用类型字典 2284、金额、发生日期、必填说明）、附件 ≤10（infra URL 存 jsonb，024_005），总额服务端重算，草稿可存。
15. 作为员工，我想草稿提交走 02 号审批链（强制单据 expense）；审批中可撤回、驳回可改再提；审批中心三视图复用 02 号。
16. 作为审批人，我想通过即同事务过账：统一过账服务在指定账户条件扣款 + 写 CREDIT/OUT_EXPENSE 分录（party=EMPLOYEE 报销人，biz_date=通过日，外币按通过日 SPOT 快照），余额不足回滚整个审批；APPROVED 为终态不可改删作废。
17. 作为财务，我想按报销人/审批状态/期间/费用类型筛选；APPROVED 可 A4 套打（报销人+账户+行表+附件名清单+合计+签章区）。

### 工资初稿与次品扣款

18. 作为人事，我想按月新建工资单（period=YYYY-MM）：同租户同期间仅一张有效单（部分唯一索引兜底，025_002）；员工行手工增删（UserSelect 全员可选、不按岗位过滤），同单员工不重复（025_003）。
19. 作为人事，我想给每个员工录款项行：收入项字典 2285（基本/计件/提成/奖金/补贴/其他，计件支持数量×单价）、扣款项字典 2286（次品扣款/其他）；应发/扣款/实发服务端重算，员工实发不可为负（025_011）。
20. 作为人事，我想点【导入次品扣款】：按工资月取确认记录（confirmed_at ∈ [月首,次月首) + CONFIRMED + OPERATOR，REVOKED 天然排除），按员工聚合卷数；弹窗填统一扣款单价（必填，仅本次使用不存配置，025_010），生成次品扣款行；行金额可改、行可删，展开可看逐卷快照（确认单号/卷号/等级/确认日）。
21. 作为人事，我想已被其他有效工资单（非 CANCELLED）引用的确认记录不重复导入（025_005 指明占用单号）；工资单作废或草稿删除后指纹释放。
22. 作为人事，我想提交走 02 号审批链（强制单据 payroll）；APPROVED 不发钱；APPROVED 且零发放记录可作废（原因必填，有发放记录报 025_006，作废后指纹释放、审批记录保留）。
23. 作为出纳，我想对 APPROVED 工资单发放登记：账户、本次金额（缺省未发余额）、发放日、备注；可多次登记跨账户；累计不超实发合计（025_007），每次写 GF 批次 + CREDIT/OUT_SALARY 分录（party=NONE，备注带工资单号+批次序号），余额不足走统一通道拦截；批次不可改删。
24. 作为老板/人事，我想列表直接看到派生态：未发放/部分发放/已发清（Σ 批次 vs net_amount，不落状态字段）；可 A4 套打工资表（员工一行：应发/扣款/实发+签收栏+合计），不做个人工资条。

### 通用（DoD）

25. 作为管理员，我想关键操作全部 @LogRecord、审批走 02 号三表；金额 @Sensitive(AMOUNT)、账号 @Sensitive(CONTACT)，打印 VO 豁免；草稿单据与无分录账户进统一回收站；写接口租户隔离与横向/纵向越权必测。

## Implementation Decisions

### 模块与包路径

- 后端（ADR-0037 骨架）：
  - `….finance.account`：账户管理页接口（10 号 /account page·get·create·simple-list 保留，本 spec 扩 update 字段与 delete）、`FinancePostingService`（**域公共唯一过账通道，10/11/12/13 收口于此**）、转账、其他收支、流水查询、收支总览
  - `….finance.exchangerate`：**不建代码**（表/菜单/接口归 10 号；本域只注入其取价查询）
  - `….finance.expense`、`….finance.payroll`
  - 枚举入 `enums.finance.FinanceErrorCodeConstants`
- 跨模块：`AdminUserApi`（报销人/员工校验+昵称快照）、审批引擎（02 号事件+Handler）、infra 文件（只存 URL）、质检确认 Mapper（production.inspectionconfirmation，同 module 只读）、汇率取价 Service（10 号 exchangerate）、供应商对账 Mapper（11 号 confirmed 单 total_amount/confirmed_at）
- 前端（ADR-0001，沿用 11 号快速版选择）：
  - `luna-ui/src/views/lunaerp-elmui/finance/account/`（列表+流水抽屉+转账弹窗+其他收支弹窗；**不含汇率设置**，那是 10780 独立页）
  - `views/lunaerp-elmui/finance/overview/`、`views/lunaerp-elmui/finance/expense/`、`views/lunaerp-elmui/finance/payroll/`
  - API 镜像 `src/api/lunaerp-elmui/finance/<子模块>/index.ts`

### 域公共表的增量（10 号 3 表之后，ALTER 扩列）

`lunaerp_finance_account` 由 10 号建表（account_name/account_type/currency/opening_balance/current_balance/status ENABLE·DISABLE），本 spec `ADD COLUMN IF NOT EXISTS`：

| 新增列 | 类型 | 说明 |
|---|---|---|
| account_no | varchar(64) | 账号/微信标识/支付宝标识；BANK/WECHAT/ALIPAY 必填（020_105），CASH 可空；@Sensitive(CONTACT) |
| account_nature | varchar(16) | PUBLIC 公户 / PRIVATE 私户，字典 2282，缺省 PUBLIC |
| remark | varchar(500) | 备注 |

`lunaerp_finance_ledger_entry` 由 10 号建表（biz_date/party_type/party_id/account_id/category/direction DEBIT·CREDIT/currency/exchange_rate numeric(20,8)/original_amount/base_amount/ref_type/ref_id/ref_item_id/reversal_of_id/审计；不提供软删），本 spec 扩 2 列：

| 新增列 | 类型 | 说明 |
|---|---|---|
| party_name | varchar(64) | 主体名称快照（EMPLOYEE 员工昵称/NONE 时空），免 join system_users |
| ref_no | varchar(32) | 来源单号快照（BX/GZ/GF/ZZ/QT 等），流水页免跳查 |

索引沿用 10 号（(tenant_id, account_id, biz_date) 等），不加。

### 数据模型（9 张新表）

金额 numeric(18,2)、汇率 numeric(20,8)（与 10 号一致）、数量 numeric(18,2)；全部 TenantBaseDO。

**表 1 `lunaerp_finance_transfer` 账户间转账单**

transfer_no（`ZZ`+YYYYMMDD+3 位日流水，唯一含软删行，ADR-0006 模式）、from_account_id/from_account_name、to_account_id/to_account_name（快照）、currency、amount、transfer_date、status（COMPLETED 固定，登记即生效）、remark。索引 `(tenant_id, transfer_date)`、transfer_no 唯一。登记同事务：两账户按 id 升序经统一通道各过一笔——from=CREDIT/TRANSFER_OUT，to=DEBIT/TRANSFER_IN（ref_type=TRANSFER+本单，ref_no=transfer_no）。

**表 2 `lunaerp_finance_other_txn` 其他收支单**

txn_no（`QT`+YYYYMMDD+3 位，唯一含软删行）、account_id/account_name 快照、direction（DEBIT 流入/CREDIT 流出）、category（字典 2287：INTEREST_IN/WASTE_IN/OTHER_IN/FEE_OUT/COST_OUT/OTHER_OUT，方向匹配后端校验 020_110）、currency/original_amount/exchange_rate/base_amount（外币账户按登记日 SPOT 取价，RMB=1）、txn_date、remark。登记即写 DEBIT/IN_OTHER 或 CREDIT/OUT_OTHER（ref_type=OTHER_TXN）；登记后不可改删。索引 `(tenant_id, txn_date)`、txn_no 唯一。

**表 3 `lunaerp_finance_expense` 报销单 + 表 4 `lunaerp_finance_expense_item` 报销行**

主表：expense_no（`BX`+YYYYMMDD+3 位，唯一含软删行）、applicant_user_id/applicant_name（缺省当前用户可代填，AdminUserApi 校验+昵称快照）、account_id/account_name（**必填**）、currency/original_total/exchange_rate/base_total（账户外币时按**审批通过日** SPOT 汇率折算快照；改账户联动币种）、total_amount（服务端=Σ行，账户为 RMB 时与 original_total 相同）、expense_date（取行内最大发生日期，展示/筛选用）、approval_status（02 号五态，草稿=PENDING）、submit_time/approve_time、attachments jsonb（infra URL 有序数组，≤10，删除仅移除数组元素）、remark。
索引：`(tenant_id, applicant_user_id, approval_status)`、`(tenant_id, account_id)`、expense_no 唯一。

行表：expense_id、category（字典 2284，值集后端校验）、original_amount（>0，024_004；账户外币时原币=账户币种）、occur_date（不晚于提交日，024_007）、remark（必填）。至少 1 行（024_002）。

**表 5 `lunaerp_finance_payroll` 工资单**

payroll_no（`GZ`+YYYYMM+3 位月流水，唯一含软删行）、period char(7)（YYYY-MM）、title（缺省"{period} 工资表"可改）、approval_status（五态，草稿 PENDING；**PAID 不落库**，已发清实时聚合派生）、earning_total/deduction_total/net_amount（服务端重算快照，工资单一期仅 RMB）、submit_time/approve_time、cancelled_by/cancelled_at/cancel_reason、remark。
索引：`(tenant_id, period)` 部分唯一 `WHERE deleted=0 AND approval_status <> 'CANCELLED'`、payroll_no 唯一。

**表 6 `lunaerp_finance_payroll_employee` 员工汇总行**

payroll_id、user_id/nickname（校验+昵称快照）、`(tenant_id, payroll_id, user_id)` 唯一、earning_total/deduction_total/net_amount 服务端重算、remark。

**表 7 `lunaerp_finance_payroll_line_item` 款项行**

payroll_id、employee_row_id、item_kind（EARNING/DEDUCTION 后端枚举）、item_type（EARNING 字典 2285；DEDUCTION 字典 2286，DEFECT=次品扣款）、qty/unit_price（可空；有则 amount=qty×price 服务端算）、amount（>0，025_004）、remark。索引 `(tenant_id, payroll_id, employee_row_id)`。

**表 8 `lunaerp_finance_payroll_defect_ref` 次品引用子表**

line_item_id（必须挂 DEDUCTION/DEFECT 行）、user_id/nickname 快照、confirmation_id/confirmation_no、roll_no、grade、confirmed_at（全部自质检确认记录快照，不可手填）、deduct_price（导入单价快照）。索引 `(tenant_id, line_item_id)`、`(tenant_id, confirmation_id)`（普通索引，作废释放语义，不建唯一）。

**表 9 `lunaerp_finance_payroll_payment` 工资发放批次**

payment_no（`GF`+YYYYMMDD+3 位，唯一含软删行）、payroll_id/payroll_no 快照、period、account_id/account_name 快照、currency/original_amount/exchange_rate/base_amount、pay_date、remark。索引 `(tenant_id, payroll_id)`、payment_no 唯一。**不挂员工、不入 10/11 共享 payment 表**（Q10：批次不分到人）。

### 统一过账服务（域公共契约，ADR-0043）

`FinancePostingService`（10 号收款 handler、11 号付款登记、本 spec 报销/工资/转账/其他收支**唯一**资金写入口；13 号往来分录账可仅 INSERT 不带账户，另走只读 append 接口）：

```
post(PostingReq{ accountId, direction: DEBIT/CREDIT, category, partyType, partyId, partyName,
                 currency, originalAmount, exchangeRate, bizDate, refType, refId, refItemId?, refNo, remark })
```

1. 查账户：不存在 020_100；status≠ENABLE 020_101；分录币种=账户币种，不一致 020_106。
2. 条件更新（10 号定稿 SQL 形态）：
   - 流入：`UPDATE … SET current_balance = current_balance + #{amt} WHERE id=#{id} AND status='ENABLE'`
   - 流出：`… SET current_balance = current_balance - #{amt} WHERE id=#{id} AND status='ENABLE' AND current_balance >= #{amt}`
   - updateCount=0：账户停用（020_101）或余额不足（**统一复用 023_004**，先查状态区分）。
3. INSERT ledger_entry（字段以 10 号定稿为准 + party_name/ref_no 扩列；base_amount=round(original_amount×exchange_rate,2)，RMB rate=1）。
4. 全程同事务；调用方异常整体回滚（报销审批 handler 余额不足即审批回滚）。
- 兼容 11 号契约：payment 层 catch 到的余额不足就是 023_004、停用就是 020_101，11 号既有 mock seam 单测不变。
- 10 号收款 handler 实现时收口到本服务（条件更新 SQL 以 10 号定稿为准，语义零变化）。

### 次品扣款取数与指纹（口径常量化）

`PayrollDefectService`（只读质检库，唯一取数真相）：

- 候选：`lunaerp_production_inspection_confirmation` 中 status=CONFIRMED AND responsibility=OPERATOR AND deleted=0 AND confirmed_at ∈ [period-01, 次月-01)，按 worker_user_id 聚合卷数+逐卷快照。
- 排除已被有效工资单（status<>'CANCELLED'，deleted=0）defect_ref 引用的 confirmation_id；CANCELLED/草稿删除后释放。应用守卫+普通索引（同 11 号指纹模式）。
- 流程：GET defect-preview（不落库）→ 弹窗统一单价 → 行与 refs 随工资单整体保存；保存时服务端重查候选，并发被抢报 025_005（指明工资单号）。
- DEFECT 行只能整行删除（连 refs），删后指纹立即释放；不允许手工添加 ref。
- REVOKED 链：草稿保存/提交时重验每个 ref 对应行仍 CONFIRMED，失效（被撤销换链）报 025_005 要求重新导入；APPROVED 后的工资单不受事后撤销影响（快照即凭证，争议款下月其他扣款项调整）。

### 状态机与守卫

**报销单**（02 号标准语义）：

| 守卫 | 规则 |
|---|---|
| 编辑/附件/删除 | 仅 PENDING/REJECTED；删除进回收站（恢复不补账） |
| 提交 | 至少 1 行（024_002）、账户 ENABLE、附件 ≤10、发生日期合法；建审批实例 biz_type=expense |
| 撤回 | APPROVING → PENDING（发起人） |
| 通过 | Handler 同事务统一通道过账（CREDIT/OUT_EXPENSE，一次全额，汇率取通过日）→ APPROVED+approve_time；失败回滚审批 |
| 终态 | APPROVED 不可改/删/作废（红冲二期） |

**工资单**：

| 守卫 | 规则 |
|---|---|
| 编辑/删除 | 仅 PENDING/REJECTED；员工行/款项行/refs 整体替换保存；删除进回收站 |
| 提交 | ≥1 员工行（025_009）；期间唯一（025_002）；全员实发 ≥0（025_011）；refs 重验（025_005）；biz_type=payroll |
| 撤回 | APPROVING → PENDING |
| 通过 | Handler **空实现**（引擎置 APPROVED，不写账） |
| 作废 | 仅 APPROVED 且发放批次=0 → CANCELLED（原因必填，025_006）；释放指纹；审批记录保留 |
| 发放登记 | 仅 APPROVED；金额 >0 且 Σ批次+本次 ≤ net_amount（025_007）；统一通道扣款+CREDIT/OUT_SALARY（party=NONE，ref=PAYROLL_PAYMENT）；批次不可改删 |
| 派生态 | 已发放=Σ批次 original_amount：0 未发放 / 区间内 部分发放 / =net 已发清；不落字段 |

**转账单/其他收支单**：无状态机，登记即 COMPLETED 写账，不可改删（红冲二期）。

### 审批引擎集成（02 号契约）

- biz_type=`expense`、`payroll`（02 号 config 字典值）；无启用流程提交报 030_030_001。
- ExpenseApprovalHandler=过账；PayrollApprovalHandler=空实现（Q6/A：02 号"工资写发放流水"的动作即发放登记，非审批）。REJECTED/CANCELLED 回置走引擎标准流程。
- 审批日志/三视图/角标全复用 02 号；消息复用 04 号 LUNA_APPROVAL_* 三模板。

### Controller 与 API 契约

前缀 `/lunaerp/finance`：

**account**

- 10 号既有：`GET /account/page`、`GET /account/get`（含 current_balance 可用余额）、`POST /account/create`（opening 仅此一次写入）、`GET /account/simple-list`
- 本 spec 扩：`PUT /account/update`（备注/状态/account_no/account_nature 可改；**opening_balance 仅在无分录时可改**，否则 020_104；币种/类型建后不改）、`DELETE /account/delete?id=`（无分录，进回收站）
- `GET /ledger/page` — 流水分页（10 号 ledger 查询的账户视图参数化：accountId/category/direction/party/期间/keyword=ref_no·party_name；默认 biz_date DESC, id DESC）
- `POST /transfer/create`、`GET /transfer/page`、`GET /transfer/get?id=`
- `POST /other-txn/create`、`GET /other-txn/page`、`GET /other-txn/get?id=`
- `GET /overview/summary?periodStart=&periodEnd=&accountId=`（账户期初/流入/流出/期末 + 类别汇总）
- `GET /overview/gross-margin?periodStart=&periodEnd=`（收入=AR_INC 净额；成本=供应商 confirmed 单；永远 200，无 enabled 开关，无 AR 数据时空态）

**expense**

- `GET /expense/page`、`GET /expense/get?id=`（items+attachments+审批进度摘要）
- `POST /expense/create`、`PUT /expense/update`、`DELETE /expense/delete?id=`（草稿）
- `POST /expense/submit?id=`、`POST /expense/cancel-approve?id=`（撤回）
- `GET /expense/print-data?id=`（仅 APPROVED，04 号契约，豁免脱敏）

**payroll**

- `GET /payroll/page`、`GET /payroll/get?id=`（员工行+款项行+refs+批次+已发放聚合）
- `POST /payroll/create`、`PUT /payroll/update`、`DELETE /payroll/delete?id=`（草稿）
- `GET /payroll/defect-preview?period=`
- `POST /payroll/submit?id=`、`POST /payroll/cancel-approve?id=`、`PUT /payroll/cancel?id=`（原因入参）
- `POST /payroll/payment/register`、`GET /payroll/payment/page?payrollId=`
- `GET /payroll/print-data?id=`（仅 APPROVED）

### 权限码

- `lunaerp:finance:account:` query / create / update / delete / transfer / other-txn
- `lunaerp:finance:overview:query`
- `lunaerp:finance:expense:` query / create / update / delete / submit / print
- `lunaerp:finance:payroll:` query / create / update / delete / submit / import-defect / pay / cancel / print
- 汇率权限 `lunaerp:finance:exchangerate:*` 归 10 号菜单 10780，本 spec 不设。

### 菜单（ADR-0037 + 10 号 10780 之后）

| ID | 名称 | sort | 归属/说明 |
|---|---|---|---|
| 10700 | 财务 | 107 | 目录 |
| 10710 | 资金账户 | 1 | 管理页归本 spec；按钮 10711 新增/10712 编辑/10713 删除/10716 账户间转账(:transfer)/10717 其他收支(:other-txn) |
| 10715 | 收支总览 | 2 | **本 spec 新增**，perm `finance:overview:query`，component `lunaerp-elmui/finance/overview/index` |
| 10720 | 客户对账 | 3 | 10 号 |
| 10730 | 供应商对账 | 4 | 11 号 |
| 10740 | 收付款 | 5 | 10/11 号三 Tab |
| 10750 | 报销 | 6 | 按钮 10751 新建/10752 编辑/10753 提交(:submit)/10754 删除/10755 打印 |
| 10760 | 工资 | 7 | 按钮 10761 新建/10762 编辑/10763 提交/10764 导入次品/10765 发放登记(:pay)/10766 作废/10767 删除/10768 打印 |
| 10770 | 发票申请 | 8 | 10 号 |
| 10780 | 汇率 | 9 | 10 号新增（其 spec 写 sort 8，与 10770 冲突；以本表 sort 9 为准，10 号 SQL 交付时采用） |

### 字典（2282–2287，紧接 10 号 2272–2278；另向 2274 追加 6 值）

- **向 2274 `lunaerp_finance_ledger_category` 追加**（10 号建字典，本 spec SQL 幂等 INSERT dict_data）：OUT_EXPENSE / OUT_SALARY / TRANSFER_IN / TRANSFER_OUT / IN_OTHER / OUT_OTHER
- 2282 `lunaerp_finance_account_nature`：PUBLIC 公户(primary)/PRIVATE 私户(warning)
- 2283 `lunaerp_finance_account_status`：ENABLE 启用(success)/DISABLE 停用(info)（对齐 10 号账户状态值）
- 2284 `lunaerp_expense_category`：TRAVEL 差旅费/OFFICE 办公费/TRANSPORT 运输费/ENTERTAIN 业务招待费/RENT 房租/UTILITY 水电费/COMM 通讯费/OTHER 其他
- 2285 `lunaerp_payroll_earning_type`：BASE 基本工资/PIECE 计件工资/COMMISSION 提成/BONUS 奖金/SUBSIDY 补贴/OTHER 其他
- 2286 `lunaerp_payroll_deduction_type`：DEFECT 次品扣款(danger)/OTHER 其他扣款
- 2287 `lunaerp_other_txn_category`：INTEREST_IN 利息收入/WASTE_IN 废品收入/OTHER_IN 其他收入/FEE_OUT 手续费/COST_OUT 工本费/OTHER_OUT 其他支出
- 复用：2271 direction（IN/OUT 渲染习惯）——分录方向实际值 DEBIT/CREDIT 随 10 号；账户类型 2277；币种用 `lunaerp_sales_contract_currency`（销售合同既有，RMB/USD/EUR/HKD，不新建）；审批态复用 02 号字典。

### 错误码

**1-030-020 域公共（10 号已占 000/100–104/200–202；本 spec 续占 105–110，余额不足统一复用 023_004）**

- `020_105` ACCOUNT_NO_REQUIRED（该类型账户必须填写账号/标识）
- `020_106` ACCOUNT_CURRENCY_MISMATCH（账户币种不一致，跨币种操作不支持）
- `020_107` TRANSFER_ACCOUNT_SAME（转出转入账户不能相同）
- `020_108` TRANSFER_NOT_EXISTS
- `020_109` OTHER_TXN_NOT_EXISTS
- `020_110` OTHER_TXN_CATEGORY_DIRECTION_MISMATCH（收支类别与方向不一致）
- 复用：020_100 不存在 / 020_101 停用 / 020_102 名称重复 / 020_103 有分录禁删 / 020_104 期初不可改 / 020_200 无生效汇率 / 023_004 余额不足

**1-030-024 expense**

- 024_000 EXPENSE_NOT_EXISTS / 024_001 EXPENSE_STATUS_NOT_ALLOWED / 024_002 EXPENSE_ITEM_REQUIRED / 024_003 EXPENSE_ACCOUNT_INVALID（不存在或停用）/ 024_004 EXPENSE_AMOUNT_INVALID / 024_005 EXPENSE_ATTACHMENT_LIMIT / 024_006 EXPENSE_USER_NOT_EXISTS / 024_007 EXPENSE_OCCUR_DATE_INVALID

**1-030-025 payroll**

- 025_000 PAYROLL_NOT_EXISTS / 025_001 PAYROLL_STATUS_NOT_ALLOWED / 025_002 PAYROLL_PERIOD_DUPLICATE / 025_003 PAYROLL_EMPLOYEE_DUPLICATE / 025_004 PAYROLL_LINE_AMOUNT_INVALID / 025_005 PAYROLL_DEFECT_ALREADY_REFERENCED（次品记录【{}】已被工资单【{}】引用或已失效）/ 025_006 PAYROLL_HAS_PAYMENT_NO_CANCEL / 025_007 PAYROLL_PAYMENT_AMOUNT_EXCEED / 025_008 PAYROLL_PAYMENT_NOT_EXISTS / 025_009 PAYROLL_EMPTY / 025_010 PAYROLL_DEFECT_PRICE_REQUIRED / 025_011 PAYROLL_NET_AMOUNT_NEGATIVE / 025_012 PAYROLL_USER_NOT_EXISTS

### 统一 DoD 落点

- **回收站（ADR-0038）**：`FINANCE_ACCOUNT`（仅无分录，恢复回原状态）、`EXPENSE`、`PAYROLL`（仅草稿 PENDING/REJECTED，恢复不补账；工资草稿恢复随之恢复次品指纹占用）。汇率主数据/转账/其他收支/发放批次/分录不进站（汇率按 10 号"停用无删除"）。
- **打印（04 号内核）**：bizType=EXPENSE（报销单 A4）、PAYROLL（工资表 A4）；print-data 豁免金额脱敏，仅 APPROVED 可打。
- **脱敏**：金额 @Sensitive(AMOUNT)；account_no @Sensitive(CONTACT)；打印 VO 豁免。
- **日志**：账户增改/停启用/期初修改/删除、转账、其他收支、报销提交过账、工资提交/作废/发放全部 @LogRecord；审批日志走 02 号。
- **租户/越权**：写接口租户隔离、横向（他租户 id 报不存在）、纵向（无权限 403）必测。

### SQL 脚本（3 个，均可重复执行）

1. `lunaerp-finance-account-ext.sql` — account ALTER 3 列 + ledger ALTER 2 列；transfer/other_txn 建表；2274 追加 6 个 dict_data；字典 2282/2283/2287；菜单 10710 按钮（10711–10713/10716/10717）+10715
2. `lunaerp-finance-expense.sql` — expense/item 建表；字典 2284；菜单 10750 段
3. `lunaerp-finance-payroll.sql` — payroll 四领域 5 张表；字典 2285/2286；菜单 10760 段
> 执行顺序前提：10 号域公共 3 表 SQL 先执行。

### 前端页面结构

- **资金账户**：表格（名称/类型/公私/账号脱敏/币种/期初/可用余额/状态）+ 新建编辑弹窗（期初"无分录可改"提示）+ 流水抽屉（分页+筛选+来源单号跳转）；顶部【账户间转账】【其他收支】；无【汇率设置】（用 10780）；停用/删除二次确认（020_103 友好提示）
- **收支总览**：期间选择→账户汇总+合计→类别借贷汇总（钻取流水）→毛利卡片（三指标+三行口径标注；应收无数据空态）
- **报销**：列表+工作台（报销人远程选择/账户下拉联动币种/行表/附件 ≤10）+详情审批时间线
- **工资**：列表（含派生发放态标签）+工作台（员工行展开款项行+【导入次品扣款】预览弹窗+逐卷明细）+发放登记弹窗+批次列表
- 金额千分位、DictTag；TS 类型以本契约为准。

## Testing Decisions

Service 纯 Mockito（不起 Spring 不连库），金额逻辑必有测试；Mock：本域 Mapper、AdminUserApi、审批引擎、质检确认 Mapper、汇率取价 Service（10 号）、供应商对账 Mapper、登录上下文、单号生成器。

1. **统一过账**：条件更新流出超额→023_004、停用→020_101、币种不符→020_106；base_amount 折算（RMB=1、外币四舍五入）；分录字段（category/direction/party_name/ref_no）正确；11 号 payment 层收到的就是 023_004/020_101
2. **账户守卫**：名称唯一 020_102；账号条件必填 020_105；有分录禁删 020_103；期初有分录禁改 020_104、无分录可改；停用后新过账被拒、历史统计不变
3. **转账**：同账户 020_107、跨币种 020_106；双边两笔分录类别/方向/ref 正确；一边失败整体回滚（无半转）
4. **其他收支**：登记即写分录；类别方向不符 020_110；不可改删
5. **报销**：行/附件/账户守卫（024_002/005/003）；提交建实例；Handler 通过同事务过账（外币取通过日汇率，失败回滚审批）；APPROVED 终态 024_001；REJECTED 改再提
6. **工资单**：期间部分唯一（并发双单 025_002）；员工重复 025_003；空单 025_009；负实发 025_011；金额服务端重算
7. **次品**：月边界聚合；仅 CONFIRMED+OPERATOR（EQUIPMENT/WAIVED/REVOKED/跨月排除）；有效单引用排除（025_005 指明单号）；CANCELLED/删草稿释放；ref 事后 REVOKED 草稿提交报 025_005、APPROVED 不受影响；单价×卷数、改额、删行释放
8. **发放**：payroll Handler 为空；超额 025_007、余额不足 023_004；多次登记派生未发/部分/发清；OUT_SALARY 分录 party=NONE；有发放禁作废 025_006；APPROVED 零发放可作废且指纹释放
9. **总览**：账户期初/流入/流出/期末口径；类别钻取；毛利=AR_INC 净额−confirmed 对账单（11 号 Mapper mock）；无 AR 数据空态不报错
10. **租户越权**：TenantContext 隔离断言；横向报不存在；纵向无权限 403
11. **分页/打印**：筛选透传默认排序；print-data 仅 APPROVED、豁免脱敏

## Out of Scope

- **借贷会计全套**（ADR-0037）；金蝶用友实际对接（仅保留分录账出口）
- **红冲/错账更正、凭证导出**：报销/发放/转账/其他收支/10 号收款/11 号付款已写账单一律终态；红冲二期统一设计（reversal_of_id 已由 10 号预留）
- **结汇、加权账面汇率/外币双余额维护、汇兑损益、期末重估 MONTH_END、外部汇率 API、跨币种转账、现金盘点/银行余额调节表**；汇率表与菜单归 10 号，本期不扩
- **考勤算薪、社保公积金、个税、SalaryStructure、压工资、计件工价主档、员工银行账户、按人发放跟踪、个人工资条**（二期随 HR）
- **员工借支/备用金/报销冲借款、报销发票 OCR/查重/AI 风控、预算费控限额**
- **工资行 Excel 批量导入**（一期手工录入+次品导入；需要时走 04 导入框架）
- 销售合同币种字典向全域字典的迁移重构（新代码复用现有字典，旧页面不动）

## Further Notes

### 与并行 spec 的共享件收口清单

| # | 共享件 | 收口结论 | 对齐对象 |
|---|---|---|---|
| 1 | ledger_entry / account / exchange_rate 三表 | **DDL 归 10 号**；本 spec ALTER 扩 account 3 列（account_no/account_nature/remark）、ledger 2 列（party_name/ref_no）；exchange_rate 零改动 | 10 号 SQL 先行 |
| 2 | 余额模型 | current_balance 条件更新（Q18=A）；统一过账服务由本 spec 实现，10 号收款 handler/11 号付款登记收口调用；余额不足统一 023_004、停用 020_101（ADR-0043） | 10/11 号 |
| 3 | 汇率主档 | 表/接口/菜单 10780/错误码 200–202 **归 10 号**（Q19=A）；本 spec 只消费，MONTH_END/结汇二期 | 10 号 |
| 4 | 分录方向/字段名 | 以 10 号定稿为准：DEBIT/CREDIT、biz_date、original_amount、exchange_rate numeric(20,8)、ref_item_id；类别字典 2274 由本 spec 追加 6 值 | 10 号 |
| 5 | 共享 payment 表 | 工资发放不使用，独立 payroll_payment；报销不使用（审批直接过账无支付单） | 10/11 号 |
| 6 | 菜单 | 新增 10715（sort 2）；10780 sort 校正为 9（10 号 spec 写 8 与 10770 冲突，以本 spec 总表为准） | 10 号 SQL |
| 7 | ApprovalHandler | expense=过账；payroll=空实现（发放登记写账，细化 02 号"工资写发放流水"） | 02 号 |
| 8 | 毛利卡片取数 | 直接查 ledger AR_INC/AR_INC_REVERSAL（10 号 shipment 事件化的落账结果），不复制发货白名单；成本查 11 号 confirmed 对账单 | 10/11 号 |
| 9 | 错误码 | 020 续占 105–110；024/025 独占；不取 021/023/026 段 | 10/11 号 |
| 10 | 次品联动 | 只读+指纹，production 域零改动，不引入 payrollSyncedAt（守 MVP 决议） | inspection-confirmation |
| 11 | 账户删除/期初修改 | 10 号定"账户无删除、期初仅创建可写"；本 spec 页面级增量：无分录可删除（回收站）、无分录可改期初（020_103/104 已在 10 号定义） | 10 号 |
| 12 | 号段避让 13 号 | 13 号 sales-return 同期定稿已占字典 2279–2281 与 ADR-0042；本 spec 字典顺延 **2282–2287**、本票 ADR 为 **0043**；13 号 AR_RETURN_REVERSAL 往来分录走统一 append 接口（account_id 为空，不碰余额） | 13 号 |

### 术语与 ADR

- 术语回填 finance GLOSSARY：资金流水（账户视图）、可用余额（current_balance 物化口径）、账户性质（公户/私户）、账户间转账、其他收支单、收支总览、经营毛利参考、报销单（一步过账）、工资款项行、次品引用、工资发放批次、已发清（派生）；错误码 020_105–110、024、025 回填错误码节。
- **新建 ADR-0043**：一张经营分录账 + current_balance 受控物化缓存（只经统一过账通道变更、无手工端点、红冲追加反向行、期初为账户主数据）。

### 实现顺序建议

1. （前提）10 号域公共 3 表 SQL
2. account-ext SQL + DO 扩列 + FinancePostingService 过账内核（先行，全域依赖）
3. 账户管理页接口/转账/其他收支/流水查询
4. 收支总览聚合
5. 报销主从+审批过账
6. 工资主从→次品取数指纹→审批空 Handler→作废→发放
7. ServiceImpl 单测四套
8. 前端四页面
9. ADR-0043/GLOSSARY/段位表回填

### 参考

- 10 号 spec：`.scratch/customer-reconciliation/spec.md`（域公共 3 表定稿、DEBIT/CREDIT、020 段错误码、10780、收款条件更新过账）
- 11 号 spec：`.scratch/vendor-reconciliation/spec.md`（付款登记 seam、指纹防重、两步制）
- 17 号研究（三本账 P0/一种分录账/扣款明细行重设计）；ADR-0037/0017/0021；02 号审批 spec；04 号通用件 spec
- inspection-confirmation MVP spec 07（payroll-summary 口径与"不写工资数据"边界）
