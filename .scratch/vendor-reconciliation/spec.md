# 财务域 - 供应商对账/付款申请（W3 资金闭环·应付侧）

Status: ready-for-agent

## Problem Statement

新栈 W3 要形成供应商应付闭环：承织/承染加工费对账、纱线采购货款对账、对账单确认、付款申请走审批、审批通过后付款登记扣资金账户。老系统（linaagi）供应商对账以 StockIn 收货为唯一取数源，但新栈两处事实不同：

1. **加工费（织费/染费）取数源是检验单而非入库单**：承织厂挂在胚布单、承染方挂在染色单，而检验单主表自带`承检方`（供应商）+ 类型（GREIGE 胚布检验 / FINISHED 成品检验），录检明细逐卷存实称重量与米数。业务口径 = **检验确认数量 × 加工单价**（charting 已锁），即以检验单 COMPLETED 后的录检数量为计费依据，入库只是物流动作。
2. **加工单价不存在于任何生产单**：胚布单只有总额性 `processing_fee`，染色单无费用字段，产品档案加工费三列是成本估算用途。单价必须在对账环节落。

老系统教训（17 号研究结论）：三本账冗余 + `paidAmount` 占位 0 + 取数白名单散落 8 处是 P0 根因。本 spec 对应三条总纲：对账不落"已付"状态字段（实时聚合）、取数口径收敛为单一服务、金额一律 DECIMAL。

供应商门户二期（Q13=A），对账确认走线下（打印对账单给供应商对账盖章）。付款申请已在 02 号审批引擎强制审批清单内（Q14-d）。

## Solution

在 luna-srv 的 `lunasrv-module-lunaerp` 新增 `finance.supplierrecon` 与 `finance.payment` 两个子包（包骨架见 ADR-0037），落地 **4 张新表 + 1 处 stock 域增量**：

1. `lunaerp_finance_supplier_recon` 供应商对账单主表（三态 DRAFT/CONFIRMED/CANCELLED）
2. `lunaerp_finance_supplier_recon_item` 对账单行表（三类行：加工费行/采购货款行/调整项行；行表即快照，生成时从源单复制，零回写）
3. `lunaerp_finance_payment_apply` 付款申请单（RECON 对账付款 / PREPAY 预付付款，走 02 号审批链）
4. `lunaerp_finance_payment` + `lunaerp_finance_payment_ref` 收付款单与核销子表（**共享表**：客户侧 IN 由 10 号定义，本 spec 定义 OUT 侧付款登记行为，结构合并时收口）
5. stock 域增量：`lunaerp_stock_in` 加"纱线采购单"可选上游引用（第 5 个，ID+快照零回写，仿现有四类）

核心业务规则：

- **取数口径常量化**：唯一取数服务产出两类候选——加工费候选（COMPLETED 检验单按承检方汇总 Σ实称重量/Σ米数，全等级计入）+ 采购货款候选（已审核 PURCHASE 入库明细，按供应商）；已被有效对账单（DRAFT/CONFIRMED）引用的源明细不再进入候选，对账单作废后自动释放。
- **对账单 = 快照凭证**：手工选"供应商 + 区间 → 预览 → 生成"，预览/生成同源；确认后行表冻结，源单后续变化不影响已生成对账单。
- **付款两步制**：付款申请（审批）→ 付款登记（出纳执行）。审批通过 ≠ 钱已付；付款登记显式扣资金账户、可部分付款、可一次核销多个申请、可用预付抵扣。
- **预付余额独立表达**：实时聚合（两条 SUM 口径常量化），不复用"余额转负"老套路；质保金/暂扣不做实体，用对账单调整项行 + 实付小于申请额表达。

## User Stories

### 对账单生成与维护

1. 作为财务/采购，我想在"财务 → 供应商对账"菜单看到对账单列表（对账单号、供应商、对账区间、确认金额、已申请金额、状态、确认人、创建时间），以便掌握各供应商应付对账进度。
2. 作为财务，我想点"生成对账单"选供应商 + 对账区间（缺省上月），系统取数出**预览**（加工费候选行 + 采购货款候选行），我逐行确认单价后提交生成，预览与生成数据完全一致。
3. 作为财务，我想加工费行自动带出检验单号、类型（GREIGE=织费 / FINISHED=染费）、Σ实称重量、Σ米数、全等级卷数，单价按行基准（GREIGE 缺省按米、FINISHED 缺省按 KG，可改）由我手工填写并预填产品档案加工费作参考。
4. 作为财务，我想采购货款行自动带出入库单号、批次/缸号、产品、数量+单位、入库单价与金额，单价默认取入库明细单价可改（改价不影响入库单）。
5. 作为财务，我想添加调整项行（负数质量扣款/正数运费补差，必填备注），以便对账单反映真实结算额。
6. 作为财务，我想被某供应商当期无任何可对账明细时提示"区间内无可对账明细"，而不是生成空单。
7. 作为财务，我想已被其他对账单引用的检验单/入库明细不出现在候选里，且能看到"该明细已被对账单【单号】引用"的明确报错，避免重复对账。
8. 作为财务，我想草稿态对账单可整体重取（重新预览替换全部取数行，调整项行保留）、可编辑行单价/删除行，确认后全部只读。

### 对账单确认与作废

9. 作为财务主管，我想对草稿对账单执行"确认"（二次确认提示），确认后单据与行数据冻结、可打印、可生成付款申请。
10. 作为财务主管，我想对 CONFIRMED 对账单"作废"（二次确认），作废后其引用的源明细释放回候选池；但若存在审批中/已审批的付款申请则被拒绝并提示。
11. 作为审计，我想对账单详情看到每行来源（检验单/入库单可点跳转）、确认人/时间、作废人/时间，且删除仅对草稿开放（进回收站）。

### 付款申请与审批

12. 作为采购/财务，我想对 CONFIRMED 对账单点"生成付款申请"（可改申请金额，默认全额），累计申请额不超对账确认金额，超了报明确错误。
13. 作为采购，我想发起"预付付款申请"（选供应商 + 金额 + 用途备注，如"纱线采购定金"），不挂对账单。
14. 作为审批人，我想付款申请走 02 号审批中心（待我审批/我已审批/我发起的），驳回后申请人可改再提（重新走全流程）。
15. 作为财务，我想在付款申请列表看到每个供应商的当前预付余额（只读），申请预付时参考。

### 付款登记（出纳执行）

16. 作为出纳，我想在"财务 → 收付款"对 APPROVED 的付款申请执行"付款登记"：选资金账户、填本次实付金额（可小于申请额，差额继续挂应付）、可选使用预付抵扣。
17. 作为出纳，我想一次登记同时核销同一供应商的多个 APPROVED 申请（金额按申请创建顺序分配，每笔申请可部分核销）。
18. 作为出纳，我想预付抵扣额不超过该供应商预付余额，资金实付额不超过资金账户可用余额，超了分别报错。
19. 作为出纳/财务，我想付款登记后看到：付款单列表（付款单号、供应商、资金账户、本次实付、预付抵扣、核销的申请单号）、每个申请的已核销金额（实时聚合）。
20. 作为财务主管，我想已登记的付款单不可修改删除（写账凭证），错付走整单红冲取消（二期，一期错付联系管理员处理并留痕）。

### 打印与通用

21. 作为财务，我想对 CONFIRMED 对账单一键打印 A4 套打对账单（抬头+供应商+区间+行表+合计+签章区），供供应商盖章回传。
22. 作为管理员，我想对账单/付款申请的操作（生成/确认/作废/申请/登记）全部留操作日志与审批日志。

## Implementation Decisions

### 模块与包路径

- 后端：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.finance.supplierrecon`（对账单主从，行 DO/Mapper 同包）与 `.finance.payment`（付款申请 + 收付款单 + 核销子表）；枚举入 `enums.finance.FinanceErrorCodeConstants`
- 跨模块依赖（只走 API/Service 层，不直连他人 Mapper）：检验单/录检明细 Mapper（production.inspectionorder，同 module 内注入）、入库单/明细 Mapper（stock.stockin）、供应商校验（baseinfo 既有 Service）、审批引擎（approval，02 号）、资金账户服务（finance.account，12 号落地件——未落地前 Service 内 mock seam，见测试节）
- 前端：`luna-ui/src/views/lunaerp-elmui/finance/supplier-recon/`（列表+工作台）与 `luna-ui/src/views/lunaerp-elmui/finance/payment/`（付款申请 Tab + 付款登记 Tab）；API 镜像 `api/lunaerp-elmui/finance/{supplier-recon,payment}/`
- 沿用 ADR-0001：落在 `lunaerp-elmui/` 先行快速版目录

### 取数口径（单一服务，常量化）

`SupplierReconSourceService` 是**唯一**取数真相（禁止在他处复制口径，老系统 P0 教训）：

- **加工费候选**（line_type=PROCESS，按检验单汇总一行）：
  - 源：`lunaerp_production_inspection_order`，条件 = `status=COMPLETED`（录检完成）AND `deleted=0` AND 承检方 = 目标供应商 AND 单据日期 ∈ 对账区间
  - 数量 = 该单录检明细 `Σ(measured_weight)`（KG）、`Σ(length_m)`（M）、卷数；**等级不过滤**（A/B/C/DEFECTIVE 全计入，劣品扣款走调整项行）
  - 计价基准 `pricing_basis`：GREIGE 缺省 `METER`（梭织织费按米）、FINISHED 缺省 `KG`（染费按公斤），预览时可逐行改为 KG/METER；`bill_qty` = 基准对应的量
  - 单价：预览行手工填；前端按产品带出产品档案加工费参考值（针织加工费/KG、织造费/M，仅预填不强制）
- **采购货款候选**（line_type=PURCHASE，一条入库明细一行）：
  - 源：`lunaerp_stock_in` + `_item`，条件 = 主表 `status=COMPLETED`（已审核）AND `in_type=PURCHASE` AND 供应商 = 目标供应商 AND 单据日期 ∈ 对账区间
  - 行带：入库单号、批次/缸号、产品快照、`quantity`+`unit`（KG/M）、明细单价（缺省取之，可改）、金额 = 数量×单价（服务端重算）
  - 退货入库（RETURN）**不进候选**：采购退货冲减走对账单调整项行（负数+备注引用退货入库单号）
- **防重复取数指纹**：`source_ref_type`（INSPECTION / STOCK_IN_ITEM）+ `source_ref_id`（+ `source_ref_item_id`）。候选生成时排除已被**有效对账单**（status ∈ DRAFT/CONFIRMED）引用的指纹；对账单 CANCELLED 后指纹自动释放。应用层守卫 + 普通索引（不建 DB 唯一索引，因作废释放语义）
- **零回写**：对账单生成不锁定、不回写检验单/入库单；行表即快照，源单事后修改不影响已生成对账单

### 数据模型（4 张新表 + 1 处 stock 增量）

金额一律 `numeric(18,2)`、单价 `numeric(18,4)`、数量 `numeric(18,2)`；**仅人民币**（Q8），无币种/汇率列。全部继承 TenantBaseDO。

**表 1 `lunaerp_finance_supplier_recon` 对账单主表**

| 分组 | 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|---|
| 编号 | recon_no | varchar(32) | ✓ | `SR`+YYYYMMDD+3 位日流水，按天取号（ADR-0006 模式），租户内全量唯一含软删行 |
| 供应商 | supplier_id / supplier_name | int8 / varchar(128) | ✓ | 逻辑关联供应商档案，快照 |
| 区间 | period_start / period_end | date | ✓ | 对账区间（含端点），按源单**单据日期**过滤 |
| 状态 | status | varchar(16) | ✓ | DRAFT/CONFIRMED/CANCELLED，字典 2269，缺省 DRAFT |
| 金额 | total_amount | numeric(18,2) | ✓ | = Σ 行 line_amount（服务端权威重算，含负数调整项） |
| 确认 | confirmed_by / confirmed_at | int8 / timestamp | - | 确认人/时间 |
| 作废 | cancelled_by / cancelled_at | varchar/int8/timestamp | - | 作废留痕 |
| 其他 | remark | varchar(1000) | - | 备注 |

**不落已申请/已付金额字段**——两者为只读实时聚合（见"聚合口径"节），杜绝老系统 paidAmount 脱节 P0。

**表 2 `lunaerp_finance_supplier_recon_item` 对账单行表**

| 分组 | 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|---|
| 归属 | recon_id | int8 | ✓ | 所属对账单 |
| 行类型 | line_type | varchar(16) | ✓ | PROCESS/PURCHASE/ADJUST（后端枚举，不入字典） |
| 源指纹 | source_ref_type / source_ref_id / source_ref_item_id | varchar(32) / int8 / int8 | 取数行✓ | INSPECTION+检验单 id / STOCK_IN_ITEM+入库单 id+明细 id；ADJUST 为空 |
| 加工源 | inspection_order_id / inspection_order_no / inspection_type | int8 / varchar(32) / varchar(16) | PROCESS✓ | 检验单快照（GREIGE/FINISHED） |
| 采购源 | stock_in_id / stock_in_no / yarn_order_id / yarn_order_no | int8 / varchar(32) / int8 / varchar(32) | PURCHASE✓ | 入库单快照；纱线采购单引用自入库单快照带出，可空 |
| 产品 | product_id / product_code / product_name | int8 / varchar(64) / varchar(128) | 取数行✓ | 产品快照 |
| 批次 | batch_no | varchar(64) | - | 采购行批次/缸号快照 |
| 数量 | qty_kg / qty_m | numeric(18,2) | - | PROCESS：Σ实称重量/Σ米数；PURCHASE：数量+`qty_unit`（KG/M） |
| 计价 | pricing_basis / bill_qty | varchar(16) / numeric(18,2) | 取数行✓ | KG/METER（PROCESS 可改）；bill_qty=基准对应量 |
| 金额 | unit_price / line_amount | numeric(18,4) / numeric(18,2) | 取数行✓ | line_amount = bill_qty × unit_price（服务端重算）；ADJUST 行直接填金额（正/负） |
| 调整 | adjust_reason | varchar(500) | ADJUST✓ | 调整原因（必填） |
| 其他 | remark | varchar(500) | - | 备注 |

索引：`(tenant_id, recon_id)`、`(tenant_id, source_ref_type, source_ref_id)`、`(tenant_id, stock_in_item_id)`。

**表 3 `lunaerp_finance_payment_apply` 付款申请单**

| 分组 | 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|---|
| 编号 | apply_no | varchar(32) | ✓ | `FK`+YYYYMMDD+3 位日流水（ADR-0006 模式），租户内唯一含软删行 |
| 类型 | apply_type | varchar(16) | ✓ | RECON 对账付款 / PREPAY 预付付款，字典 2270 |
| 供应商 | supplier_id / supplier_name | int8 / varchar(128) | ✓ | 快照 |
| 对账源 | recon_id / recon_no | int8 / varchar(32) | RECON✓ | 必须为 CONFIRMED 对账单 |
| 金额 | amount | numeric(18,2) | ✓ | 本次申请额，>0 |
| 预付用途 | purpose / bank_name / bank_account | varchar(500) / varchar(128) / varchar(64) | - | PREPAY 用途必填；银行信息申请时从供应商档案快照（展示用，contact 脱敏位） |
| 审批 | approval_status | varchar(16) | ✓ | 02 号跨切面五态 PENDING/APPROVING/APPROVED/REJECTED/CANCELLED；提交走审批引擎，APPROVED 不触发任何过账（付款登记才是过账动作） |
| 其他 | remark | varchar(1000) | - | 备注 |

索引：`(tenant_id, supplier_id, approval_status)`、`(tenant_id, recon_id)`、`(tenant_id, apply_no)` 唯一。

**表 4 `lunaerp_finance_payment` 收付款单 + 表 5 `lunaerp_finance_payment_ref` 核销子表（共享表）**

> 共享件：主表结构由 10（IN 侧）/本 spec（OUT 侧）合并收口，下列为本 spec 需要的最小列集。

`lunaerp_finance_payment`：payment_no（付款单号，前缀入共享冲突清单）、`direction`（IN/OUT，本 spec 只写 OUT）、supplier_id/customer_id（按 direction 二选一）、account_id（资金账户，12 号表）、pay_amount（本次资金实付）、prepay_used_amount（预付抵扣额）、status（COMPLETED 登记/REVERSED 红冲预留）、remark、登记人/时间。索引 `(tenant_id, supplier_id)`、`(tenant_id, payment_no)` 唯一。

`lunaerp_finance_payment_ref`：payment_id、ref_type（PAY_APPLY，一期唯一值）、ref_id（付款申请 id）、apply_no 快照、allocate_amount（该申请本次核销分配额）。索引 `(tenant_id, ref_id)`、`(tenant_id, payment_id)`。

**stock 域增量**：`lunaerp_stock_in` 加 `yarn_order_id int8` + `yarn_order_no varchar(32)` 可空列 + 索引；工作台基本信息加"关联纱线采购单"远程搜索（存在性校验+快照，零回写），仿现有合同/胚布单/染色单引用模式；错误码复用 stock 域"引用不存在"既有码。

### 聚合口径（常量化，只读不落库）

- **对账单已申请金额** = Σ( 挂该对账单且 approval_status ∉ {REJECTED, CANCELLED} 的申请.amount )；列表/详情实时聚合展示
- **付款申请已核销金额** = Σ( payment_ref 中该申请的 allocate_amount )
- **供应商预付余额** = Σ( PREPAY 类申请被核销的 allocate_amount ) − Σ( 所有付款登记的 prepay_used_amount )
- 聚合 SQL 各一处实现（Service 公共方法），前端不自行计算

### 状态机与守卫

**对账单** `DRAFT → CONFIRMED → CANCELLED`：

| 守卫 | 规则 |
|---|---|
| 编辑/重取/删行/删除 | 仅 DRAFT；删除走回收站（RecycleBinHandler） |
| 确认 | DRAFT → CONFIRMED，条件更新 `WHERE status='DRAFT'` 防并发双确认；total_amount 必须非 0 |
| 作废 | CONFIRMED → CANCELLED 条件更新；存在 approval_status ∈ {PENDING, APPROVING, APPROVED} 的挂靠申请时拒绝（错误码 006） |
| 生成付款申请 | 仅 CONFIRMED；Σ 已申请 + 本次 ≤ total_amount（同事务重查 + 条件更新防并发超申，错误码 007） |
| 终态 | CANCELLED 为终态不可复活；确认/作废均 @LogRecord |

**付款申请** `DRAFT →(提交) APPROVING → APPROVED / REJECTED`，REJECTED 可改再提（= 回 DRAFT 重新提交走全流程，02 号标准语义）；APPROVING 可撤回（回 DRAFT）；仅 DRAFT 可删（回收站）。APPROVED 的 ApprovalHandler **为空实现**（不做任何过账——付款登记才是资金动作）。

**付款登记**（对 APPROVED 申请，OUT 方向）：

- 入参：supplier_id、申请核销明细 `[{apply_id, allocate_amount}]`、account_id、pay_amount、prepay_used_amount（缺省 0）
- 守卫：申请须 APPROVED 且同一供应商；Σ allocate_amount ≤ pay_amount + prepay_used_amount（钱要够分）；各申请 allocate ≤ 其剩余额（amount − 已核销）；prepay_used ≤ 供应商预付余额（005）；pay_amount ≤ 账户可用余额（004，调用 finance.account 条件更新，余额不足报错）
- 同事务副作用：写 payment + ref 行 → 写资金账户流出流水（pay_amount，经 finance.account 服务）→ 写经营分录账一条 OUT_SUPPLIER 分录（Σ(pay_amount+prepay_used)，refType=PAYMENT，refId=payment.id，备注含 prepay_used）
- 已登记付款单不可改不可删；红冲二期

### 付款申请与审批引擎集成（02 号契约）

- 提交申请时按 `lunaerp_approval_config` 配置的付款申请审批链创建实例（强制审批单据之一，Q14-d）
- 02 号 `ApprovalCompletedEvent`（BEFORE_COMMIT 同事务）→ 本模块 ApprovalHandler：APPROVED 仅置 approval_status；REJECTED 回置由引擎标准流程处理
- 审批日志/审批中心三视图全部复用 02 号，本 spec 零新建

### Controller 与 API 契约

前缀 `/lunaerp/finance`，统一 CommonResult/PageResult：

**supplier-recon（对账单）**

- `GET /supplier-recon/page` — 筛选：supplierId、status、区间、keyword（对账单号/供应商名）
- `GET /supplier-recon/get?id=` — 详情（内嵌 items + 只读聚合：已申请金额）
- `POST /supplier-recon/preview` — 取数预览（入参 supplierId/periodStart/periodEnd；出参候选行，不落库）
- `POST /supplier-recon/create` — 生成（入参同预览 + 行编辑结果；服务端以同一取数服务重取校验一致性，源明细被抢引用时报 004）
- `PUT /supplier-recon/update` — 草稿编辑行（单价/基准/删行/调整项）；`PUT /supplier-recon/re-fetch` — 草稿整体重取
- `PUT /supplier-recon/confirm?id=` / `PUT /supplier-recon/cancel?id=` — 确认/作废
- `GET /supplier-recon/print-data?id=` — 打印数据（04 号 print-data 契约，豁免脱敏）
- `DELETE /supplier-recon/delete?id=` — 仅 DRAFT，进回收站

**payment-apply（付款申请）**

- `GET /payment-apply/page` — 筛选：supplierId、applyType、approvalStatus、keyword
- `GET /payment-apply/get?id=` — 详情（内嵌只读：已核销金额）
- `POST /payment-apply/create` — 新建（RECON 校验对账单 CONFIRMED + 金额上限；PREPAY 校验 purpose）
- `PUT /payment-apply/update` — 仅 DRAFT/REJECTED
- `POST /payment-apply/submit?id=` — 提交审批；`POST /payment-apply/cancel-approve?id=` — 撤回（APPROVING）
- `DELETE /payment-apply/delete?id=` — 仅 DRAFT
- `GET /payment-apply/prepay-balance?supplierId=` — 供应商预付余额（只读聚合）

**payment（收付款·OUT 侧）**

- `GET /payment/page` — 付款单列表（direction=OUT 固定）
- `POST /payment/register` — 付款登记（见状态机节契约）
- `GET /payment/get?id=` — 详情（内嵌 ref 核销行）

### 权限码与菜单

- `lunaerp:finance:supplierrecon:` query / create / update / delete / confirm / generate（生成付款申请）/ print
- `lunaerp:finance:payment:` query / apply（付款申请发起与编辑提交）/ register（付款登记）

菜单（随 SQL 脚本交付，不提前插空菜单）：

- **10730 供应商对账**（ADR-0037 预留位）：10731 生成对账单(:create)、10732 编辑/重取(:update)、10733 确认/作废(:confirm)、10734 生成付款申请(:generate)、10735 打印(:print)、10736 删除(:delete)
- **10740 收付款**：Tab1 付款申请 + Tab2 付款登记；10741 发起申请(:apply)、10742 删除申请(:apply)、10743 付款登记(:register)、10744 删除付款单(不做——红冲二期，不设此按钮)

### 字典（3 个，id 段 2269–2271，紧接成本卡 2262–2268）

- 2269 `lunaerp_supplier_recon_status`：DRAFT 草稿(warning) / CONFIRMED 已确认(success) / CANCELLED 已作废(info)
- 2270 `lunaerp_payment_apply_type`：RECON 对账付款(primary) / PREPAY 预付付款(success)
- 2271 `lunaerp_finance_payment_direction`：IN 收款(success) / OUT 付款(warning)（共享字典，10 号复用）
- 付款申请审批状态展示复用 02 号审批引擎 approval_status 字典（若 02 未定义则本 spec 补建，冲突清单挂账）

### 错误码（1-030-022 supplierrecon 段 / 1-030-023 payment 段）

**1-030-022（本 spec 独占）**

- `022_000` SUPPLIER_RECON_NOT_EXISTS（对账单不存在）
- `022_001` SUPPLIER_RECON_STATUS_NOT_ALLOWED（当前状态不允许该操作）
- `022_002` SUPPLIER_RECON_SUPPLIER_NOT_EXISTS（供应商不存在）
- `022_003` SUPPLIER_RECON_NO_SOURCE_ITEMS（对账区间内无可对账明细）
- `022_004` SUPPLIER_RECON_SOURCE_REFERENCED（源明细已被对账单【{}】引用）
- `022_005` SUPPLIER_RECON_ADJUST_LINE_INVALID（调整项行不合法：金额为 0 或缺备注）
- `022_006` SUPPLIER_RECON_HAS_PAYMENT_APPLY（对账单存在付款申请，禁止作废）
- `022_007` SUPPLIER_RECON_APPLY_AMOUNT_EXCEED（累计申请金额【{}】超出对账确认金额）

**1-030-023（payment 共享段，与 10 号避让合并时对齐，本 spec 先占 000–007）**

- `023_000` PAYMENT_APPLY_NOT_EXISTS（付款申请不存在）
- `023_001` PAYMENT_APPLY_STATUS_NOT_ALLOWED（付款申请当前状态不允许该操作）
- `023_002` PAYMENT_APPLY_RECON_NOT_CONFIRMED（对账单未确认，不可生成对账付款申请）
- `023_003` PAYMENT_ALLOCATE_EXCEED（核销分配金额超过付款申请剩余额）
- `023_004` PAYMENT_ACCOUNT_BALANCE_NOT_ENOUGH（资金账户余额不足）
- `023_005` PAYMENT_PREPAY_BALANCE_NOT_ENOUGH（供应商预付余额不足）
- `023_006` PAYMENT_ACCOUNT_NOT_AVAILABLE（资金账户不存在或已停用）
- `023_007` PAYMENT_AMOUNT_INVALID（付款金额不合法）

### 统一 DoD 落点（04/02 号通用件声明）

- **回收站**：声明 `RecycleBinHandler` 两个 bizType——`VENDOR_RECON`（仅收 DRAFT 对账单，恢复回 DRAFT，不补账——对账单本身不写账）、`PAYMENT_APPLY`（仅收 DRAFT 申请，恢复回 DRAFT）；CANCELLED/CONFIRMED/已提交审批单不进站，走作废/终态
- **打印**：对账单模板接 04 号打印内核 + 模板注册表（bizType `VENDOR_RECON`，A4 套打，签章区）；print-data 独立接口豁免金额脱敏；付款申请打印不做
- **脱敏**：对账金额/申请金额/实付金额/预付余额标 `@Sensitive(amount)`；银行账户快照标 `@Sensitive(contact)`；打印 VO 豁免
- **日志**：生成/确认/作废/重取/申请提交/付款登记 @LogRecord；审批日志全走 02 号引擎三表
- **消息**：审批到达/通过/驳回复用 04 号 `LUNA_APPROVAL_*` 三模板，零新增模板

### SQL 脚本（3 个文件，均可重复执行）

1. `luna-srv/sql/postgresql/lunaerp-finance-vendor-recon.sql` — 表 1/2 DDL + 索引 + 字典 2269 + 菜单 10730 段
2. `luna-srv/sql/postgresql/lunaerp-finance-payment.sql` — 表 3/4/5 DDL + 索引 + 字典 2270/2271 + 菜单 10740 段按钮
3. `luna-srv/sql/postgresql/lunaerp-stock-in-yarn-order-ref.sql` — stock_in 加列 ALTER + 注释 + 索引

### 前端页面结构与交互

- **供应商对账**（列表页 + 工作台）：
  - 列表：搜索区（供应商远程选择/状态/区间/关键词）+ 列（对账单号/供应商/区间/确认金额/已申请金额(实时聚合)/状态 DictTag/创建时间/操作）；操作按状态显隐：草稿[编辑][删除]、已确认[生成付款申请][打印][作废]
  - 工作台：头部（供应商远程选择 + 区间 + 备注）+ [取数预览]按钮 → 行表（三类行混排，行类型标签区分；加工行展示检验单号/类型/ΣKG/ΣM/基准切换/单价可编辑；采购行展示入库单号/批次/数量/单价可编辑；底部[添加调整项]）；合计行展示确认金额；草稿态[保存][重取][确认]，确认后只读
  - 详情抽屉：行来源单据可点跳转（检验单工作台/入库单工作台）
- **收付款**（单页两 Tab）：
  - Tab1 付款申请：搜索区（供应商/类型/审批状态）+ 列（申请单号/类型/供应商/申请金额/已核销金额/审批状态/创建时间）+ 操作（草稿[编辑][提交][删除]、APPROVING[撤回]、APPROVED 不可操作——去 Tab2 登记）；[发起付款申请]按钮选类型分叉表单（RECON 选 CONFIRMED 对账单带出余额上限 / PREPAY 填用途）
  - Tab2 付款登记：APPROVED 且未核销完的申请列表 → [付款登记]弹窗（资金账户下拉、预填剩余可核销申请多选带分配额可改、实付金额、预付抵扣额+当前预付余额提示）→ 确认后刷新；下方付款单列表（付款单号/供应商/账户/实付/预付抵扣/核销申请号）
- 前端 API 与 TS 类型以本 spec 契约为唯一事实来源；金额展示千分位、 DictTag 渲染状态

## Testing Decisions

### 测试哲学与 seam

只测外部业务行为；Service 层纯 Mockito 单元测试（不启动 Spring、不连库），金额逻辑**必有测试**（统一 DoD 硬指标）。Mock 依赖：本域 Mapper×5、检验单/明细 Mapper、入库单/明细 Mapper、审批引擎 Service、finance.account 服务（余额条件更新 seam）、登录用户上下文、ID/单号生成器。

### 覆盖的行为（`SupplierReconServiceImplTest` / `PaymentServiceImplTest`）

1. **取数**：仅 COMPLETED 检验单入候选且按承检方过滤；ΣKG/ΣM/卷数聚合正确；GREIGE/FINISHED 缺省基准正确；仅已审核 PURCHASE 入库明细入候选且 RETURN 排除；被 DRAFT/CONFIRMED 对账单引用的指纹被排除、CANCELLED 后释放回候选；区间过滤边界
2. **生成/编辑**：预览与 create 重取不一致（源明细被抢引用）报 004；行金额服务端重算覆盖前端值；调整项金额为 0 或缺备注报 005；区间无明细报 003；单号按天取号与撞号
3. **确认/作废**：条件更新防并发双确认；确认后行冻结（源单数据后续变化不影响详情返回）；有 PENDING/APPROVING/APPROVED 申请时作废报 006、仅 CANCELLED 申请不阻断；作废后指纹释放
4. **付款申请**：RECON 挂非 CONFIRMED 对账单报 002；累计申请+本次 > total_amount 报 007（含并发同事务重查）；PREPAY 缺 purpose 报错；REJECTED 改再提重走审批；APPROVED 的 ApprovalHandler 不产生任何账务写入
5. **付款登记**：多申请顺序分配正确；分配超剩余额报 003；prepay_used > 预付余额报 005；账户余额不足报 004（验证 account 服务条件更新失败路径）；同事务写 payment/ref/账户流水/分录账四步原子；部分核销后申请剩余额与预付余额聚合正确
6. **聚合口径**：已申请金额排除 REJECTED/CANCELLED；预付余额 = ΣPREPAY 核销 − Σprepay_used 两条 SUM 口径
7. **租户与越权**：写接口 TenantContext 隔离断言（DoD 硬指标）；横向越权（他人租户 id 访问报不存在）、纵向越权（无 :confirm/:register 权限调用 403）
8. **分页查询**：筛选参数透传、默认排序

## Out of Scope

- **外币**：供应商对账/付款仅人民币；原币/汇率/本位额三金额列由 ledger 全域设计统一承载（10/12 号）
- **质保金/暂扣实体**（比例暂扣+到期释放自动分录）：用调整项行 + 实付<申请额表达，二期
- **加工价格主档**（供应商×产品×工序×生效期）：一期对账行手工填单价，二期
- **进项发票登记与付款核销联动**：老系统付款事务内核销进项发票的模式不迁，进项发票二期随 invoice 扩展
- **采购退货自动冲应付**：RETURN 入库不进对账候选，冲减走调整项行手工表达
- **付款单红冲/错付更正**：一期登记即终态，红冲二期
- **自动月结对账任务、账龄分桶、应付账龄报表**：手工生成，账龄在 10 号应收侧先行，应付侧二期
- **对账单快照与源单差异比对报告**：零回写原则下不提供
- **供应商门户对账确认**：二期（Q13=A）
- **Excel 导入导出、模板设计器**（走 04 号既有边界）

## Further Notes

### 与并行 spec 的共享件冲突清单（合并时收口）

| # | 共享件 | 本 spec 立场 | 对齐对象 |
|---|---|---|---|
| 1 | `lunaerp_finance_payment` 主表结构 | 定义 OUT 侧最小列集；direction 分型 | 10 号（IN 侧）；表结构以先定稿者为基础合并 |
| 2 | 核销子表 `lunaerp_finance_payment_ref` | ref_type 一期仅 PAY_APPLY | 10 号若需核销对账单/合同则扩 ref_type，不改结构 |
| 3 | 付款单号前缀 | 未定（表结构收口时一并定） | 10 号 |
| 4 | 分录账 `ledger` 写入 | 付款登记写 OUT_SUPPLIER 分录 | 10/12 号共用 append-only 分录账（GLOSSARY 域公共件） |
| 5 | 资金账户余额校验与流水 | 调用 finance.account 服务（12 号落地件） | 12 号；未落地前测试 mock seam |
| 6 | 字典 2271 direction | 本 spec 建，10 号复用 | 10 号 |
| 7 | 审批状态字典 | 复用 02 号 | 02 号 |
| 8 | 错误码 1-030-023 段 | 先占 000–007 | 10 号避让后续段 |

### 术语归位（finance GLOSSARY 回填）

供应商对账单（状态机/取数口径/三类行回填）、付款申请单、付款登记、供应商预付余额、对账取数口径（检验确认数量）、调整项行——随本 spec 定稿回填 `docs/contexts/finance/GLOSSARY.md`；错误码 022/023 段回填 GLOSSARY 错误码节。**不新建 ADR**：取数口径偏离老系统（检验单 vs StockIn）已在 charting 锁定且理由记录于本 spec Problem Statement；两步付款制与预付独立表达是 17 号研究结论的直接应用。

### 实现顺序建议

1. SQL 脚本 ×3（建表+索引+字典+菜单；stock 增量 ALTER）
2. DO×5 + Mapper×5 + 枚举（LineType/ApplyType/Direction/ReconStatus + 错误码常量）
3. `SupplierReconSourceService` 取数服务（纯函数化候选装配，先于其余服务）
4. 对账单 Service/Controller（预览/生成/编辑/确认/作废/打印数据）
5. 付款申请 Service/Controller（02 号引擎集成，空 ApprovalHandler）
6. 付款登记 Service/Controller（四步同事务 + 聚合口径公共方法）
7. ServiceImpl 单元测试两套
8. stock-in 域增量（DO/VO/工作台纱线采购单下拉）
9. 前端 API/TS + 对账单列表与工作台 + 收付款两 Tab 页

### 参考

- 17 号研究：`.scratch/lunaerp-commercialization/issues/17-legacy-finance-research.md`（三条总纲：一种分录账/口径常量化/可抄模型）
- 02 号审批引擎 spec：`.scratch/approval-engine/spec.md`（approval_status 五态、ApprovalCompletedEvent 契约）
- 04 号通用件 spec：`.scratch/common-foundation/spec.md`（打印内核/回收站 SPI/脱敏/消息 Helper）
- 01 号 ADR-0037：finance 域包骨架/错误码段/菜单骨架
- 同构先例：stock-in（严格三态守卫+同事务写流水）、inspection-order（独立子资源与快照）、yarn-order（ADR-0006 按天取号）、cost-card（KG/M 双口径先例）
