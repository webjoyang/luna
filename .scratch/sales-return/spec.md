# 业务域 - 客户销售退货（W3 资金闭环·退货）

Status: ready-for-agent

## Problem Statement

商业化一期 W3 要补齐"发货 → 退货 → 应收冲减"闭环：客户把已发出的布匹退回，企业需要回答四件事——**退什么（血缘与防超退）、货怎么回来（质检与入库）、钱怎么冲（应收冲减）、谁批准（审批链）**。一期仅客户销售退货；纱线采购退货二期（charting Q14-c）。

老系统（linaagi）的 `ReturnOrder` 是其财务域完成度最高的模型（17 号研究 f 节）：发货明细血缘 + 卷级明细、防超退上限、财务确认单事务冲应收并回补库存、ledgerEntryId/stockInId 回写可撤销、红冲标记、外币按发货入账汇率冲减。可直接作为范本；但有一条 P0 必须杜绝——**items 血缘为空时生成 0 明细入库单，"钱冲了货一克没进库存"**。

新栈与老系统的两处事实差异，决定了不能照抄：

1. **库存侧已有唯一正数写入口约定**：ADR-0017 规定通用库存台账的正数流水只由入库单审核写入，入库类型字典 `lunaerp_stock_in_type`（id=2234）早已预留 `RETURN=退货入库`。退货不应在入库单之外开第二个正数写入口（不新增 `RETURN_IN` 台账类型，见 ADR-0042）。
2. **客户侧应收模型由 10 号客户对账 spec 定稿**：应收在发货审核时成立（finance 监听 `ShipmentAuditedEvent` 写 AR_INC 分录），10 号冻结了退货冲减接口 `LedgerService.recordArReturnReversal(...)`（category=`AR_RETURN_REVERSAL`，按原发货入账汇率折本位额），退货侧不得自建应收台账/余额表，也不做红字对账单。

依赖：02 号审批引擎（`biz_type=sales_return` 已在 02 清单预留）、04 号通用件（打印/回收站/脱敏/消息）、10 号客户对账（冲减接口与汇率口径）。

## Grilling 定稿决策（13 号 ticket，Q1–Q10）

1. **Q1 库存回冲=复用退货入库单，不新增台账类型**：审批通过自动生成 `inType=RETURN` 的 DRAFT 入库单，仓管到货质检后审核写标准 `STOCK_IN` 正流水；取消退货级联取消入库单（DRAFT 取消无流水）。给 `lunaerp_stock_in` 加通用来源列 `source_doc_type/source_doc_id`（二期采购退货复用），退货单回写 `stock_in_id`（ADR-0042）。
2. **Q2 业务状态 + approval_status 双列七态**：业务态 `DRAFT/APPROVED/RECEIVED/POSTED/CANCELLED`（字典 2279），审批态沿用 02 五态；到货登记与质检判定合并为一步；财务确认是独立手动动作（职责分离）；仅 DRAFT/APPROVED 可作废，**过账后红冲一期不做**。
3. **Q3 质检二态、废品不入库**：逐卷判 `RESTOCK`（可二次入库，入退货单头选定仓库+库位）/ `SCRAP`（废品，不写库存台账，仅留质检处置记录）；废品仓/降级品库存二期。
4. **Q4 行血缘强制、卷血缘可选、双维度防超退**：退货行必挂发货明细行；卷能对上原发货卷就勾选（带回实称重/米数快照），对不上的散退手填卷号；数量与金额双维度防超退；单价缺省带原发货行快照、可改（协议折让），但行金额 ≤ 可退量×原发货单价。
5. **Q5 直接反向分录、不建红字对账单**：POSTED 时 business 发 `SalesReturnPostedEvent`，finance 监听调冻结接口写 `AR_RETURN_REVERSAL`，汇率取**原发货入账汇率**（10 号 Q1/Q15 定稿，替代 grilling 初选的 POSTED 日即期汇率）；已收款客户冲完余额转贷方，由 10 号预收/退款模型承接，退货单不建退款单；红字税务发票一期不做。
6. **Q6 工程落点**：business 域 `salesreturn` 子包，3 张新表 + stock_in 加 2 来源列；单号 `RT-YYYYMMDD-NNN`；错误码 `1-030-037`；字典 2279–2281（grilling 初选 2272–2274，落稿核 10 号已占 2272–2278，顺延）；菜单 10141 + 按钮 10142–10149；A4 套打 `SALES_RETURN_DOC` 接 04 内核；回收站 handler 仅收 DRAFT；金额挂 04 amount 脱敏位。
7. **Q7 严格一单对一发货单**：客户/合同/币种/发货仓库从头单快照带出不可改；退货原因挂**行级必填**（字典 2280），头级只有总备注。
8. **Q8 提交即逐卷、行费卷量软对齐**：提交审批时每行至少一卷，卷号必填、重量/米数至少一项；行退货数量（计费口径）手填，卷合计与行数量**软对齐只提示不强拦**（沿用发货软齐套先例）；`restock_qty` 由 RESTOCK 卷按行单位（KG/M）汇总服务端重算。
9. **Q9 入库金额恒 0、全废自动取消入库单**：入库明细 unit_price/amount 一律 0（退货库存成本价属二期成本核算，禁止把销售价填成库存成本）；到货时部分判废→重写入库单仅留 RESTOCK 明细再审核；全部判废→同事务自动取消入库单，退货仍可 RECEIVED→POSTED（货没入库、钱照冲），取消单留痕。
10. **Q10 事件方向 business 发 finance 收；APPROVING 即占量**：business 不 import finance（同 10 号 shipment 事件化同构）；防超退占量态 = `approval_status ∈ {APPROVING, APPROVED} 且 status ≠ CANCELLED`，DRAFT(PENDING/REJECTED) 不占，驳回/撤回/作废释放。

## Solution

在 `lunasrv-module-lunaerp` 的 business 域新增 `salesreturn` 子包，落地 **3 张新表 + 1 处 stock 域增量**：

1. `lunaerp_business_sales_return` 退货主表（业务五态 + 02 审批五态双列）
2. `lunaerp_business_sales_return_item` 退货行（强制发货明细血缘、行级原因、计费数量/入库数量双量）
3. `lunaerp_business_sales_return_roll` 退货卷（可选发货卷血缘、逐卷质检判定）
4. stock 域增量：`lunaerp_stock_in` 加通用来源列 `source_doc_type/source_doc_id` + 部分唯一索引（1:1 回指）

核心业务规则：

- **一单对一发货单**：只能选 `COMPLETED` 发货单发起退货；客户、合同、币种、原发货仓库从头单快照带出，退货仓库缺省=原发货仓库可改（草稿态）。
- **钱货分流、职责分离**：审批通过只生成待入库草稿；仓管到货+质检+库位分配后入库（RECEIVED）；财务确认才冲应收（POSTED）。废品卷不入库但不阻塞应收冲减。
- **防超退常量化**：唯一聚合服务按发货明细行实时计算可退量/可退额，在途审批单同样占量，同事务行锁串行化并发退货。
- **钱账唯一出口**：应收冲减只经 finance 域冻结接口，退货模块不碰分录账表。

## User Stories

### 制单与提交

1. 作为销售内勤，我想在"业务管理 → 销售退货"点新建，选择一张已发货（COMPLETED）的发货单，系统自动带出客户、合同、币种、原发货仓库与发货明细行（产品/色号/缸号/原发货数量/单价/可退量），未审核/已取消发货单不可选。
2. 作为销售内勤，我想按发货明细行录入退货数量（≤该行可退量）、退货原因（必填：质量问题/发错货/多发/客户原因/其他），单价默认带原发货单价可改，行金额服务端重算；行金额超过"可退量×原单价"时报错。
3. 作为销售内勤，我想逐卷登记退货：能勾选出原发货卷清单（带回原卷号/实称重量/米数，允许改成客户实际退回的重量/米数），也能手填散退卷号（对不上原卷）；每行至少一卷，卷号必填、重量与米数至少填一项；卷合计与行数量不一致时顶部黄色提示但不拦截保存。
4. 作为销售内勤，我想保存草稿（可改可删进回收站），确认无误后提交审批；提交后在审批中心可撤回，驳回后可改再提（重新走全流程）。
5. 作为销售内勤，我想对同一发货明细重复退货时看到精确的可退量被实时扣减；另一张退货单尚在审批中时其数量同样占用，防止两单并行通过后超退。

### 审批

6. 作为审批人，我想销售退货走 02 号审批中心固定链（待我审批/我已审批/我发起的），通过/驳回/撤回语义与其他单据完全一致；审批通过后系统自动生成一张"退货入库"DRAFT 入库单并在退货单上回显入库单号。

### 到货质检与入库

7. 作为仓管，我想在退货单上执行"到货质检"：逐卷判定 可二次入库/废品（缺省可二次入库，可批量设置），给可入库卷分配退货仓库下的库位（可空，沿用"先入账后上架"），确认后退货单变为已收货、入库单被审核，库存台账增加 STOCK_IN 正流水。
8. 作为仓管，我想部分卷判废时只有可入库卷进入入库单（废品卷只留质检记录：判定人/时间/备注）；当全部卷判废时入库单被自动取消（不留库存账），但退货流程照常走向财务确认。
9. 作为仓管，我想如果入库单已被人工提前审核或取消，到货质检被拒绝并提示入库单状态异常，避免绕过质检直接入账。

### 财务确认

10. 作为财务，我想对已收货退货单执行"财务确认"：系统按原发货入账汇率把逐行退货金额追加为应收反向分录（客户往来余额立即减少），退货单变为已过账终态；若该发货单为外币但查不到原发货汇率，报错阻断并提示。
11. 作为财务/老板，我想客户已预付/已多付货款时，退货冲减后该客户往来自然转为贷方（预收），后续可在客户对账/收款模块处理，退货单本身不需要做退款单。
12. 作为审计，我想已过账退货单不可修改、不可作废（写账凭证）；详情页看到入库单号、逐卷质检判定与确认人/时间、财务确认人与时间全链路留痕。

### 打印与通用

13. 作为销售内勤，我想对非作废退货单打印 A4 退货单（抬头+客户+原发货单号+行表+卷清单+质检结果+合计+签章区），随货同行或交客户留存。
14. 作为管理员，我想退货单全部关键动作（提交/审批/到货质检/财务确认/作废/打印）有操作日志，审批过程有 02 号审批日志，金额对无权限同事脱敏。

## Implementation Decisions

### 模块与包路径

- 后端：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.business.salesreturn`（主/行/卷 DO、Mapper、VO 同子包）；错误码入 `enums.business.BusinessErrorCodeConstants`（或本域既有错误码常量类，段位 `1-030-037`）。
- 跨模块依赖（只走 Service/事件，不直连他人 Mapper 写）：
  - `business.shipment`：发货单/明细/卷只读查询 Service（校验 COMPLETED、快照带出）；
  - `stock.stockin`：`StockInService` 新建/更新/审核/取消（DRAFT）内部方法级复用，入库 DTO 由退货模块按其 SaveReqVO 组装；
  - `stock.warehouse`：仓库/库位存在性校验（复用既有校验）；
  - `approval`：02 号审批引擎提交/撤回 + 本模块实现 `ApprovalHandler(biz_type=sales_return)`；
  - `finance`：**零编译依赖**，只定义并发布 `SalesReturnPostedEvent`（DTO 放 business.salesreturn 包），finance 侧 listener 调 `LedgerService.recordArReturnReversal(...)`（10 号冻结接口，由 10 号 session 实现）。
- 前端：`luna-ui/src/views/lunaerp-elmui/business/sales-return/`（列表 + 编辑工作台 + 到货质检弹窗 + 详情），API 镜像 `api/lunaerp-elmui/business/sales-return/`；沿用 ADR-0001 临时快速版目录，与发货模块同区。

### 数据模型（3 张新表 + 1 处 stock 增量）

金额一律 `numeric(18,2)`、单价 `numeric(18,4)`、数量/重量/米数 `numeric(18,2)`；全部继承 TenantBaseDO（租户隔离）。

**表 1 `lunaerp_business_sales_return` 退货主表**

| 分组 | 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|---|
| 编号 | return_no | varchar(32) | ✓ | `RT-YYYYMMDD-NNN` 按天取号（ADR-0006/发货 SH 模式），租户内全量唯一含软删行 |
| 业务状态 | status | varchar(16) | ✓ | DRAFT/APPROVED/RECEIVED/POSTED/CANCELLED，字典 2279，缺省 DRAFT |
| 审批状态 | approval_status | varchar(16) | ✓ | 02 号跨切面五态 PENDING/APPROVING/APPROVED/REJECTED/CANCELLED，缺省 PENDING |
| 发货血缘 | shipment_id / shipment_no | int8 / varchar(32) | ✓ | 必选 COMPLETED 发货单，保存后不可改 |
| 客户 | customer_id / customer_name | int8 / varchar(128) | ✓ | 从头单快照 |
| 合同 | sales_contract_id / contract_no | int8 / varchar(32) | - | 从头单快照（发货单可无合同） |
| 退货仓库 | warehouse_id / warehouse_name | int8 / varchar(128) | ✓ | 缺省=原发货仓库，DRAFT 可改；APPROVED 后锁定 |
| 币种 | currency | varchar(8) | ✓ | 从头单快照（RMB/USD/EUR/HKD） |
| 金额 | total_amount | numeric(18,2) | ✓ | = Σ 行金额，服务端权威重算 |
| 数量 | total_pieces | int | ✓ | = Σ 行退货匹数 |
| 入库回指 | stock_in_id | int8 | - | 审批通过回写；全废后其指向 CANCELLED 入库单（仍留痕） |
| 到货 | arrived_by / arrived_at | int8 / timestamp | - | 到货质检确认人/时间（RECEIVED 落） |
| 过账 | posted_by / posted_at | int8 / timestamp | - | 财务确认人/时间（POSTED 落） |
| 作废 | cancelled_by / cancelled_at / cancel_reason | int8 / timestamp / varchar(500) | - | 作废留痕 |
| 日期 | return_date | date | ✓ | 单据日期，缺省当天 |
| 其他 | remark | varchar(1000) | - | 头级总备注 |

索引：`(tenant_id, return_no)` 唯一（含软删行）、`(tenant_id, customer_id, status)`、`(tenant_id, shipment_id)`、`(tenant_id, status, approval_status)`。

**表 2 `lunaerp_business_sales_return_item` 退货行**

| 分组 | 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|---|
| 归属 | return_id | int8 | ✓ | 所属退货单 |
| 发货血缘 | shipment_item_id | int8 | ✓ | **强制**挂发货明细行（必须属于头单 shipment_id），防老系统空血缘 P0 |
| 产品快照 | product_id / product_code / product_name / customer_color_no / color_name / vat_no | int8/varchar | ✓ | 从发货明细复制 |
| 计费数量 | return_qty | numeric(18,2) | ✓ | 退货计费数量（>0），手填，防超退校验 |
| 匹数 | pieces | int | ✓ | 缺省 1 |
| 单位 | unit | varchar(8) | ✓ | KG/M，从发货行快照 |
| 单价 | unit_price | numeric(18,4) | ✓ | 缺省=发货行单价，可改 |
| 金额 | amount | numeric(18,2) | ✓ | = return_qty × unit_price 服务端重算；≤ 可退量×原发货单价 |
| 原因 | return_reason | varchar(16) | ✓ | 字典 2280（QUALITY/WRONG/EXCESS/CUSTOMER/OTHER） |
| 入库数量 | restock_qty | numeric(18,2) | - | 到货时服务端重算：RESTOCK 卷按行单位汇总；草稿期为空/0 |
| 废品数量 | scrap_qty | numeric(18,2) | - | 到货时落：SCRAP 卷按行单位汇总 |
| 其他 | remark | varchar(500) | - | |

索引：`(tenant_id, return_id)`、**`(tenant_id, shipment_item_id)`（防超退聚合用）**。

**表 3 `lunaerp_business_sales_return_roll` 退货卷**

| 分组 | 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|---|
| 归属 | return_id / return_item_id | int8 | ✓ | 必属于某一退货行 |
| 发货卷血缘 | shipment_roll_id | int8 | - | 勾选原发货卷时回指（必须属于该行 shipment_item_id）；散退为空 |
| 卷号 | roll_no | varchar(64) | ✓ | 勾选时带回可改，手填必填；不设 DB 唯一约束（同发货卷先例） |
| 重量 | weight | numeric(18,2) | - | 实称 KG；与 length_m 至少一项非空 |
| 米数 | length_m | numeric(18,2) | - | |
| 质检 | qc_result | varchar(16) | 到货必填 | 字典 2281：RESTOCK/SCRAP；草稿/审批期为空 |
| 质检留痕 | qc_by / qc_at / qc_remark | int8 / timestamp / varchar(500) | - | 判定人/时间/备注 |
| 库位 | location_id / location_code | int8 / varchar(64) | - | 仅 RESTOCK 卷，到货时分配，可空（先入账后上架） |
| 其他 | remark | varchar(500) | - | |

索引：`(tenant_id, return_id)`、`(tenant_id, shipment_roll_id)`、`(tenant_id, return_item_id)`。

**stock 域增量（独立 SQL，幂等 DO 块 ALTER）**：

- `lunaerp_stock_in` 加 `source_doc_type varchar(32)`、`source_doc_id int8` 两可空列 + 注释；
- 部分唯一索引：`CREATE UNIQUE INDEX uk_stock_in_source ON lunaerp_stock_in(source_doc_type, source_doc_id) WHERE deleted=0 AND source_doc_type IS NOT NULL`（保证一张退货单至多一张入库单；手工入库单两列为空不受约束）；
- 普通索引 `(tenant_id, source_doc_type, source_doc_id)`（退货单反查）；
- 与 11 号 spec 的 `lunaerp-stock-in-yarn-order-ref.sql` 同表并行，两个 ALTER 脚本均须 `ADD COLUMN IF NOT EXISTS` 幂等。

### 取数与防超退口径（单一服务，常量化）

`SalesReturnAvailabilityService`（唯一口径，禁止复制）：

- **可退量**（按发货明细行）= 发货明细 `quantity` − Σ 占量退货行 `return_qty`；
  - 占量谓词常量化：`sales_return.status <> 'CANCELLED' AND sales_return.approval_status IN ('APPROVING','APPROVED')` 且 join 行的 `shipment_item_id = X`（软删行排除）；
  - 发货单仅 COMPLETED 可选（取消发货单已红冲库存，不存在可退）。
- **可退额**（行级）= 可退量 × 发货明细**原** unit_price；退货行金额守卫 `amount ≤ 可退量 × 原单价`（允许折让降价，不允许超额授信）。
- **入库量重算**（到货事务内）：按行 unit 汇总——KG 行取 RESTOCK 卷 `Σweight`，M 行取 RESTOCK 卷 `Σlength_m`；RESTOCK 卷在该行单位维度缺值报 037_011；`scrap_qty = 卷合计同口径 − restock_qty`（记录用）。
- **并发串行化**：提交审批事务内对父发货单行 `SELECT ... FOR UPDATE`（或等价悲观锁），同事务重算可退量后条件插入占量，并发第二单等待后重算超退即失败（037_005/006）。
- 卷合计与行 return_qty 仅做前端软提示，后端不拦截（软齐套）。

### 状态机与守卫（统一 DoD）

主表业务态 `status` + 审批态 `approval_status` 双列：

```
新建                status=DRAFT,      approval_status=PENDING
提交审批            status=DRAFT,      PENDING/REJECTED → APPROVING（占量；行锁+重算防超退）
驳回（引擎）        status=DRAFT,      → REJECTED（释放占量；可改再提=重新走全流程）
撤回                status=DRAFT,      APPROVING → PENDING（释放占量）
审批通过(Handler)   DRAFT → APPROVED,  approval_status=APPROVED
                    同事务：生成 inType=RETURN 的 DRAFT 入库单 + 回写 stock_in_id
到货质检            APPROVED → RECEIVED（见下"到货质检事务"）
财务确认            RECEIVED → POSTED（见下"过账事务"，终态）
作废                DRAFT/APPROVED → CANCELLED（APPROVING 须先撤回；见守卫）
```

| 动作 | 守卫 |
|---|---|
| 编辑/删除 | 仅 status=DRAFT（approval_status=PENDING/REJECTED）；删除进回收站，级联软删行/卷 |
| 提交 | DRAFT；至少一行；每行至少一卷；卷号必填、重量/米数至少一项；行级原因必填；行锁内重算双维度防超退（005/006）；卷血缘必须属于头单（007） |
| 撤回 | approval_status=APPROVING |
| ApprovalHandler | APPROVED：幂等校验无 stock_in_id；建 DRAFT 退货入库单（明细按"全部可入库"预生成，金额恒 0，source 回写 SALES_RETURN+退货单 id，仓库取退货单头）；REJECTED 不生成任何单据；handler 失败审批事件同事务回滚（02 契约） |
| 到货质检 | status=APPROVED；入库单存在且仍为 DRAFT（被人工审核/取消报 009）；每卷 qc_result 必录（010）；RESTOCK 卷库位须属于退货仓库（可空）；RESTOCK 卷行单位量非空（011） |
| 财务确认 | status=RECEIVED 条件更新 `WHERE status='RECEIVED'` 防重复过账；存在 RESTOCK 卷时入库单必须 COMPLETED（012）；过账事件失败同事务回滚 |
| 作废 | DRAFT：直接置 CANCELLED（无入库单）；APPROVED：入库单必须仍 DRAFT，同事务调 stock-in 取消（DRAFT 取消不写红冲流水）；APPROVING 须先撤回；RECEIVED/POSTED 拒绝（002）；CANCELLED 终态 |
| 打印 | 非 CANCELLED 均可打印（草稿水印由模板呈现，沿用出口单证草稿可打印先例） |

**到货质检事务**（销售退货 → 收货）：

1. 加载退货单 + 卷清单 + DRAFT 入库单（source 回指校验归属）；
2. 落每卷 qc_result/qc_by/qc_at/qc_remark/location 快照；
3. 重算各行 restock_qty/scrap_qty；
4. 组装入库单内容：**每个 RESTOCK 卷一条入库明细**（itemType=PRODUCT、产品快照取退货行、rollNo、quantity=行单位对应量、pieceCount=1、unit_price/amount=0），每卷一条 placement（locationId 可空，quantity 同量）；SCRAP 卷不产生明细/放置；
5. 全废分支：入库单明细为空 → 调 stock-in DRAFT 取消（置 CANCELLED，无流水）；部分/全可入分支：整单替换明细后调 stock-in 审核（写 `STOCK_IN` 正流水、不出现任何新 txn_type）；
6. 退货单 status=APPROVED→RECEIVED、落 arrived_*；库存动作失败整事务回滚。

**过账事务**（财务确认）：

1. 条件更新 RECEIVED→POSTED（防并发双过账）；
2. 同事务 publish `SalesReturnPostedEvent`（BEFORE_COMMIT，载荷：returnId/returnNo/customerId/currency/returnDate/逐行 `{shipmentItemId, amount 原币}`）；
3. finance listener（10 号 session 实现）按原发货 AR_INC 分录解析汇率，调 `LedgerService.recordArReturnReversal(...)` 写 category=AR_RETURN_REVERSAL 反向分录（refType=SALES_RETURN、refId=退货单 id、原币+汇率+本位额）；原发货汇率查不到报 037_014，整事务回滚；
4. 落 posted_by/at；POSTED 终态，无反向事件、无红冲端点。

### 审批引擎集成（02 号契约）

- `lunaerp_approval_biz_type` 字典数据增量 `SALES_RETURN`（02 号 spec 已预留 sales_return 字面值），默认审批链配置项随本票 SQL 插入；
- 提交/撤回/审批中心三视图/审批记录全部复用 02 既有端点与 `approval_status` 字典，零新建审批表；
- 本模块提供 `SalesReturnApprovalHandler`（一个 biz_type 一个 Bean）：仅承载上表 APPROVED 副作用；驳回/撤回的状态回置走引擎标准流程。
- 审批到达/通过/驳回消息复用 04 号 `LUNA_APPROVAL_*` 三模板，零新增消息模板。

### Controller 与 API 契约

前缀 `/lunaerp/business/sales-return`，统一 CommonResult/PageResult：

- `GET /page` — 筛选：customerId、shipmentNo、status、approvalStatus、keyword（退货单号/客户名）、日期区间；默认按创建时间倒序
- `GET /get?id=` — 详情（头 + items + rolls + stockInNo/stockInStatus 只读）
- `GET /shipment-candidates` — 可选发货单（客户远程筛选，仅 COMPLETED、含可退量>0 的行）
- `GET /shipment-detail?shipmentId=` — 发起退货取数：发货头快照 + 逐行（原数量/单价/Σ占量/可退量）+ 卷清单（原卷号/重量/米数）
- `POST /create` / `PUT /update` — 草稿保存（聚合差异保存沿用 ADR-0032 模式：行/卷整单替换；服务端重算金额/汇总）
- `DELETE /delete?id=` — 仅 DRAFT，进回收站
- `POST /submit?id=` / `POST /withdraw?id=`
- `POST /receive` — 到货质检（body：id + rolls[{id, qcResult, locationId, qcRemark}]）
- `POST /post?id=` — 财务确认
- `POST /cancel?id=` — body 含 cancelReason（APPROVED 作废时必填）
- `GET /print-data?id=` — 04 号 print-data 契约（豁免金额脱敏）

权限码：`lunaerp:business:sales-return:` query/create/update/delete/submit（提交与撤回）/receive/post/cancel/print。

### 菜单与权限

随 SQL 幂等插入（先 DELETE 再 INSERT，不提前插空菜单），挂 10100 业务管理目录（sort 4，发货 sort 2、出口单证 sort 3 之后）：

| ID | 名称 | type | parent | 权限 |
|---|---|---|---|---|
| 10141 | 销售退货 | 2 | 10100 | `lunaerp:business:sales-return:query`（component `lunaerp-elmui/business/sales-return/index`，componentName `LunaerpElmuiBusinessSalesReturn`，path `sales-return`，icon `ep:refresh-left`） |
| 10142 | 退货创建 | 3 | 10141 | `:create` |
| 10143 | 退货修改 | 3 | 10141 | `:update` |
| 10144 | 退货删除 | 3 | 10141 | `:delete` |
| 10145 | 退货提交审批 | 3 | 10141 | `:submit` |
| 10146 | 到货质检 | 3 | 10141 | `:receive` |
| 10147 | 财务确认 | 3 | 10141 | `:post` |
| 10148 | 退货作废 | 3 | 10141 | `:cancel` |
| 10149 | 退货打印 | 3 | 10141 | `:print` |

### 字典（3 个，type id 2279–2281，避让 10 号 2272–2278）

- 2279 `lunaerp_sales_return_status`：DRAFT 草稿(warning) / APPROVED 已审批(primary) / RECEIVED 已收货(warning) / POSTED 已过账(success) / CANCELLED 已作废(info)
- 2280 `lunaerp_sales_return_reason`：QUALITY 质量问题(danger) / WRONG 发错货(warning) / EXCESS 多发(primary) / CUSTOMER 客户原因(info) / OTHER 其他(info)
- 2281 `lunaerp_sales_return_qc_result`：RESTOCK 可二次入库(success) / SCRAP 废品(danger)
- 审批状态复用 02 号 approval_status 字典；币种/单位复用销售合同与入库既有字典；dict_data id 取 **3850–3869 块**（避让 10 号自 3800 起块，按 DO 块判占用平移幂等插入）。

### 打印·脱敏·回收站·日志（04 号契约）

- **打印**：注册模板 `SALES_RETURN_DOC`（A4，bizType `SALES_RETURN`）到 04 模板注册表；独立 print-data 接口返回全量字段（客户联系人不脱敏、金额不脱敏）；打印接口加 `@LogRecord(type="打印", ...)`，不建打印日志表。
- **脱敏**：列表/详情 VO 的 unit_price/amount/total_amount 标 `@Sensitive(amount)`（W0 amount 权限位）；打印 VO 豁免。
- **回收站**：注册 `RecycleBinHandler(bizType=SALES_RETURN)`，仅收 DRAFT（approval_status=PENDING/REJECTED），恢复回 DRAFT 并按父→子顺序复活行/卷；CANCELLED 永不进站（红冲/作废留痕）；恢复不补任何账（草稿从未记账）。
- **日志**：submit/withdraw/receive/post/cancel 动作均 `@LogRecord`；审批操作日志走 02 引擎记录表。

### SQL 脚本（2 个文件，可重复执行）

1. `luna-srv/sql/postgresql/lunaerp-business-sales-return.sql` — 3 表 DDL + 索引 + 字典 2279–2281（data 3850 块）+ `lunaerp_approval_biz_type` 增量数据行 + 默认链配置项 + 菜单 10141–10149
2. `luna-srv/sql/postgresql/lunaerp-stock-in-sales-return-ref.sql` — stock_in 加 2 来源列（IF NOT EXISTS）+ 注释 + 部分唯一索引 + 普通索引

### 前端页面结构与交互

- **列表页**：搜索区（客户远程选择/发货单号/业务状态/审批状态/日期区间/关键词）+ 列（退货单号/客户/原发货单号/币种/退货金额(脱敏)/状态双徽章/到货/过账时间/操作）；操作按状态显隐：草稿[编辑][提交][删除]、审批中[撤回]、已审批[到货质检][作废]、已收货[财务确认]、已过账[打印][详情]。
- **编辑工作台**：顶部选发货单（远程搜索 COMPLETED）→ 只读头快照（客户/合同/币种/退货仓库可改）；行表从发货明细勾选带入（展示原量/可退量，录退货量/原因/单价）；卷区按行展开（勾选原卷或手填，重量/米数可编辑）；行费卷量差异黄色提示条；保存草稿/提交审批。
- **到货质检弹窗**：卷清单逐行下拉判定（批量"全部可入库/全部废品"），RESTOCK 卷库位选择器（按退货仓库过滤，可留空）；提交即不可逆。
- **详情页**：头/行/卷只读 + 入库单号可点跳转入库单工作台 + 时间线（提交/审批/到货/过账）+ 打印按钮。
- 金额展示千分位、DictTag 双状态渲染；前端 API 与 TS 类型以本 spec 契约为唯一事实来源。

## Testing Decisions

### 测试哲学与 seam

只测外部业务行为；Service 层 Mockito 单测（不起 Spring、不连库），金额/数量逻辑必有测试（统一 DoD 硬指标）。Mock：本域 Mapper×3、发货只读 Service/Mapper、StockInService、仓库库位 Mapper、审批引擎 Service、`ApplicationEventPublisher`、finance listener 以独立监听测试覆盖、登录上下文、取号器。

### 覆盖行为（`SalesReturnServiceImplTest` / `SalesReturnLedgerListenerTest`）

1. **制单取数**：仅 COMPLETED 发货单可发起；非 COMPLETED/他租户发货单报 003；头快照（客户/合同/币种/仓库）正确带出；行/卷不属于头单报 004/007。
2. **保存校验**：行缺原因、卷缺卷号、卷重量米数双空分别报错；金额服务端重算覆盖前端入参；行费卷量差异不拦截（软提示）。
3. **防超退**：数量超可退报 005、金额超可退额报 006；APPROVING/APPROVED 占量、REJECTED/PENDING/CANCELLED 释放；模拟并发两单（行锁后第二单重算超退失败）。
4. **审批 Handler**：APPROVED 幂等建一张 DRAFT 入库单（inType=RETURN、source 两列回指、金额 0、明细按全 RESTOCK 预生成）；重复事件不重建；REJECTED 不建单；handler 抛错审批事务回滚。
5. **到货质检**：未判定卷报 010；RESTOCK 卷缺行单位量报 011；部分判废重写入库单为仅 RESTOCK 卷并审核，断言台账写入类型为 `STOCK_IN`（不得出现 RETURN_IN/其他新类型）、placement 与卷一一对应；全废自动取消 DRAFT 入库单且退货单→RECEIVED；入库单被人工先审核/取消报 009；restock_qty/scrap_qty KG/M 双口径重算正确。
6. **作废**：DRAFT 作废不触入库；APPROVED 作废同事务取消 DRAFT 入库单且无库存流水；APPROVING 直接作废拒绝；RECEIVED/POSTED 作废报 002。
7. **过账**：RECEIVED→POSTED 条件更新防双过账；有 RESTOCK 卷但入库单非 COMPLETED 报 012；断言发布事件载荷（客户/币种/逐行原币金额）一次；listener 测试：按原发货汇率调 `recordArReturnReversal`（category=AR_RETURN_REVERSAL、refType=SALES_RETURN、原币/汇率/本位额正确）；汇率缺失报 014；listener 抛错退货过账回滚；全废单可正常过账。
8. **租户与越权**：写接口租户隔离断言；横向越权（他租户单据访问报不存在）、纵向越权（无 :receive/:post 权限 403）。
9. **分页性能**：万级退货单常规分页 P95<1s 口径（EXPLAIN 验证占量聚合走 `(tenant_id, shipment_item_id)` 索引）。
10. **回收站/打印**：handler 仅收 DRAFT、级联复活；print-data VO 不脱敏而列表 VO 脱敏。

## Out of Scope

- **纱线采购退货**（退供应商）：二期；stock_in 来源列与 RETURN 入库类型届时复用，应收/应付反向冲减仿本 spec 另立。
- **过账后撤销/退货红冲单**：POSTED 终态不可逆；错账二期随全域红冲体系（收款红冲同理，见 10 号）设计。
- **红字税务发票/数电票冲红**：一期不对接金税（10 号发票非目标同源），退货不做发票冲红标记。
- **客户退款单/退货转换货**：已收款客户冲减后余额转贷方，由 10 号预收与收款模型承接；退款出款、换货流程二期。
- **废品仓/降级品库存/报废处置流程**：SCRAP 卷不进库存台账，仅质检记录；废品入仓、变卖、成本损失二期。
- **退货库存成本价**：入库金额恒 0；加权成本/成本卡口径的退货成本结转属二期成本核算。
- **运费红冲**：发货运费独立于货物金额，退货不自动冲运费；需要时走财务调整。
- **审批流条件分支/按金额分级审批**：02 号一期固定链。
- **门户客户发起退货申请、退货 Excel 批量导入、模板设计器**：二期/04 既有边界。

## Further Notes

### 与并行 spec 的共享件收口清单

| # | 共享件 | 本 spec 立场 | 对齐对象 |
|---|---|---|---|
| 1 | 应收冲减接口 | 只发事件，调 10 号冻结的 `LedgerService.recordArReturnReversal`（AR_RETURN_REVERSAL，原发货汇率），不建分录/余额表 | 10 号（实现方，本票 Blocked by 10） |
| 2 | `lunaerp_stock_in` 加列 | 加 source_doc_type/source_doc_id 两列（通用回指），独立幂等 ALTER 脚本 | 11 号（同表加 yarn_order 两列，脚本互不冲突） |
| 3 | 入库类型字典 RETURN | 复用 2234 既有值（3597 退货入库），不加字典项 | stock-in |
| 4 | 审批 biz_type | sales_return 字面值 02 号已预留，本票补 dict_data 与默认链配置 | 02 号 |
| 5 | 字典号段 | 2279–2281（10 号已占 2272–2278）；dict_data 3850 块 | 10/11 号 |
| 6 | 审批状态/消息模板/打印内核/回收站 SPI/脱敏 | 全部复用 02/04，零新建 | 02/04 号 |
| 7 | `_order.md` | 落稿同时登记 customer-reconciliation(26) 与 sales-return(27) 两行（10 号 ticket 仍 claimed，若其 session 已登记则仅保留本票一行） | 10 号 |

### 术语归位

销售退货单/退货行/退货卷/退货单业务状态/质检判定（可二次入库·废品）/可退量——回填 `docs/contexts/business/GLOSSARY.md`；字典 2279–2281 与错误码 `1-030-037` 同步登记 business GLOSSARY 与 shared 段位总表。

### ADR

新增 **ADR-0042**：销售退货复用入库单作为库存台账唯一正数写入口（不新增 RETURN_IN 台账类型）；理由与备选见 `docs/adr/0042-sales-return-reuse-stock-in.md`。

### 实现顺序建议

1. SQL ×2（3 表 + stock_in 加列 + 字典/菜单/审批配置）
2. DO×3 + Mapper×3 + 枚举（业务状态/原因/质检结果/错误码常量）
3. `SalesReturnAvailabilityService`（可退量/可退额/占量聚合，先于其余服务）
4. 退货 Service/Controller（草稿聚合保存/提交/撤回/作废）
5. 02 号 ApprovalHandler（建退货入库单）
6. 到货质检事务（stock-in 复用：重写/审核/全废取消）
7. `SalesReturnPostedEvent` + 与 finance listener 的联调契约（10 号件就绪前 listener 测试以接口 mock 覆盖）
8. ServiceImpl 单测全套
9. 前端 API/TS + 列表/工作台/质检弹窗/详情 + 打印模板
10. 回收站 handler、print-data、脱敏标注

### 参考

- 17 号研究 f 节：`.scratch/lunaerp-commercialization/issues/17-legacy-finance-research.md`（ReturnOrder 范本与空血缘 P0）
- 10 号 spec：`.scratch/customer-reconciliation/spec.md`（Q1 应收成立点、Q15 冲减接口冻结、字典 2272–2278）
- ADR-0017（通用台账唯一写入口）、ADR-0029/0030（发货过账与卷清单）、ADR-0016（入库过账状态机）、ADR-0018（两级库位）、ADR-0032（聚合差异保存）、ADR-0038（回收站边界）、ADR-0042（本票新增）
- 02 号 spec：`.scratch/approval-engine/spec.md`；04 号 spec：`.scratch/common-foundation/spec.md`
