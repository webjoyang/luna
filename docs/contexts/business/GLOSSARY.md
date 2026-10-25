# 业务管理域术语表（business）

本文件是 Luna ERP **业务管理域（business）**的权威术语表，收录销售合同、成本卡与报价、发货管理、销售退货、出口单证等业务单据的领域概念。
仓库领域文档总入口见根目录 `GLOSSARY-MAP.md`；跨域平台约定（租户、软删除、前后端模块约定、错误码段位总表）见 `docs/contexts/shared/GLOSSARY.md`。
当对话或代码中含糊使用某个词时，以本文件为准。

## 业务管理：销售合同

**销售合同 (SalesContract)**：
与客户签订的布匹销售约定，记载交易条款与商品明细，是业务管理域的第一个单据。
_避免_：合同（无前缀时与未来的采购合同歧义）、订单。

**合同明细 (SalesContractItem)**：
一份销售合同中，按单个品种（客户色号 / 门幅 / 克重组合）逐行记录的数量、单价、其他费用与金额条目。一份合同含 0~N 条明细，允许先建档后补明细。明细是合同聚合的一部分，随合同整体保存、级联删除，没有独立操作界面（与客户联系人的独立资源模式刻意相反，见 ADR-0003）。
_避免_：合同商品、明细行（口语）。

**贸易类型 (TradeType)**：
`DOMESTIC` 内销（缺省）/ `EXPORT` 外销，区分销售方向。外销合同在基本信息之外多一组可空"外贸条款"（incoterm 贸易术语、起运港、目的港、付款方式，仅表单 EXPORT 时显示），供出口单证生成时带出；买方英文名称/英文地址不在合同上，归客户档案。

**订单属性 (OrderType)**：
`BULK` 大货（缺省）/ `SAMPLE` 样布，区分批量生产订单与打样。

**合同状态 (SalesContractStatus)**：
`ACTIVE` 进行中（新建缺省）/ `COMPLETED` 已完成 / `CANCELLED` 已取消。MVP 无审批流，由编辑页手动切换，不做流转约束。旧系统的审批态（draft/submitted/approved/rejected）、业务进度 progress、回款状态 paymentStatus 三套状态在 MVP 均不引入。

**业务员 (Salesperson)**：
负责该合同的内部销售人员，关联 system `User`，可空。区别于客户侧的客户联系人。

**客户名称快照 (clientName)**：
建单时从客户档案复制到合同上的客户公司名。客户档案后续改名不影响既有合同；客户为必选（`customerId` 逻辑关联，不建数据库外键），联系人与地址选客户时带出但可手工修改。

**合同编号 (ContractNo)**：
形如 `CH20260001`：`CH` + 年份 + 4 位顺序号（超 9999 自然扩位），租户内唯一，新建缺省由服务端生成、也可手填（撞号报错）。编号在软删除后也不释放、不重用（见 ADR-0004）。

**已下达量 (ReleasedQty)**：
销售合同明细行已向生产环节下达的数量（成品口径，单位同合同明细）。由「下达生产」工作台下达时在同事务内原子累加（`WHERE released_qty + #{releaseQty} <= quantity`），下游胚布单、染色单执行与完工零回写合同明细（ADR-0040）。

**剩余可下达量 (RemainingQty)**：
销售合同明细行当前仍可下达生产的额度，动态计算公式为 `contractQty - releasedQty`。作为下达生产工作台预填与校验依据，不可为负数。

## 业务管理：成本卡与报价

**成本卡 (CostCard)**：
某产品在某一时点按"纱线原料 + 工序费用"测算出的**单位成本凭证**，是报价单的取数源，业务管理域单据（落 business 子包，原料价格等主数据由生产域/基础信息域提供）。`lunaerp_business_cost_card` 主表含产品规格快照、织物结构/估算模式、取价价类、克重门幅与每米重量、税率、basis（针织 KG/梭织 METER）、双口径（每 KG/每 M）不含税与含税四个单位成本、缺价纱线数；两张结构化子表：纱线行（规格/取价快照/比例/方向/密度/织缩/损耗/每米用量/行贡献）与费用行。成本卡算价的唯一权威实现是后端纯函数（ADR-0039），前端只调 `/trial` 与保存接口。
_避免_：核价单、估价单（老系统 CostPricingRequest 协作流二期才做）、把成本挂在产品档案上（产品成本字段原地改无历史，已否决）。

**成本卡费用行 (CostCardFee)**：
成本卡上逐行维护的工序费用条目，六项走字典 `lunaerp_cost_card_fee_type`：织造/染色/定型/检验/包装/运费（字典可扩充，公式只认计费口径不认费用类型）；每行含 fee_unit（PER_KG 元/KG、PER_METER 元/M）、单价、损耗率%、双口径行贡献；可缺行、运费可空。一期不建工序费用主档，费用一卡一录（选品时由产品档案加工费字段预填初始行）。

**成本卡状态 (CostCardStatus)**：
字典 `lunaerp_cost_card_status`（id=2262）：`DRAFT` 草稿（可改可删进回收站）/ `EFFECTIVE` 生效（全字段冻结，报价取数源；同一产品同时仅一张，部分唯一索引兜底）/ `EXPIRED` 失效（终态只读）。新生效卡同事务把同产品旧生效卡顶为失效；主动停用走手工「失效」；调价走「复制新建」。成本卡号 `CC{yyyy}NNNN`，全量唯一含软删行（ADR-0004）。

**报价单 (Quotation)**：
销售向客户出具的价格承诺单，业务管理域单据。`lunaerp_business_quotation` 主表含客户与联系人快照、币种/汇率、有效期、总数量/总金额/总成本额/整单毛利率（均服务端重算）与状态；明细行含产品规格快照、数量/单位、成本卡引用与**成本快照 JSON**、成本单价（RMB 与折报价币）、目标毛利率、报价单价、行金额/成本额/反算实际毛利率、成本来源（CARD 成本卡/MANUAL 手工）。无生效成本卡的行允许成本 0 挂"无成本卡"警示；成本/毛利字段按 W0 amount 脱敏位在序列化层抹除。
_避免_：报价记录表（老系统 QuotationRecord 无版本无有效期，已超越）、客户协议价表（一期报价单自身承载一客一价）。

**报价版本 (QuotationVersion)**：
同一报价主题共享一个报价号 `QT{yyyy}NNNN`，每次「修订」生成同号新版本（version_no+1，页面展示 `QT20260001-v2`）：旧 CONFIRMED 版本同事务置 SUPERSEDED 只读，新版本为 DRAFT；同一报价号同时至多一张草稿（部分唯一索引）。`(quotation_no, version_no)` 全量唯一含软删行。仅最新 CONFIRMED 版本可修订、取消、转合同。

**报价有效期 (QuotationValidity)**：
报价单头的 valid_from（缺省今天）/ valid_to（可空=长期）。"已失效"是 `CONFIRMED 且 valid_to < 当天` 的**派生展示徽章**，不落存储状态（避免定时任务与日期改回滚）；过期单转合同仅弹警告不硬锁。

**报价单状态 (QuotationStatus)**：
字典 `lunaerp_quotation_status`（id=2267）：`DRAFT` 草稿 / `CONFIRMED` 已确认 / `SUPERSEDED` 已修订（终态）/ `CANCELLED` 已取消（终态）；另含 `EXPIRED` 已失效字典项，**仅用于派生徽章展示，禁止写入**。一期无审批流，「确认报价」即生效。

**毛利率 (Gross Margin)**：
本系统报价口径采用真毛利率而非老系统的成本加成率：报价反算 `报价单价 = 成本单价 ÷ (1 − 目标毛利率)`，手工改价反算 `毛利率 = 1 − 成本额 ÷ 金额`，整单毛利率按金额加权 `(总金额 − 总成本额) ÷ 总金额`。毛利率有效区间 (−100%, 100%)，等于 100% 数学不可反算。刻意不沿用老系统 `成本×(1+加成率)`，以与 W3 经营账毛利口径一致（ADR-0039）。外币报价时成本按快照汇率折报价币后在同币种内计算。

**转合同 (QuotationToContract)**：
从最新已确认报价单生成销售合同草稿的动作：预填合同头（客户/联系人/币种/贸易类型）与明细（产品规格快照/数量/报价单价→合同单价），合同保存回写 `source_quotation_id/source_quotation_no` 两列。是"预填 + 人工再编辑"而非自动建单；币种与报价锁定一致；一张报价允许多次转合同，不锁数量、不追踪已转量、不回写报价状态。

## 业务管理：发货管理

**发货单 (Shipment)**：
向客户交付布匹货物的出库凭证，是业务管理域的第二个单据，也是库存出库的权威凭证（见 ADR-0029）。三态严格过账状态机（`DRAFT` / `COMPLETED` / `CANCELLED`）。一张发货单对应单一发货仓库。审核即出库过账写负库存流水（`SHIPMENT_OUT`），取消已发货单逐条红冲正流水（`SHIPMENT_OUT_REVERSAL`）。
_避免_：销售出库单（本系统以发货单兼作销售出库凭证，不另设销售出库单）、送货单。

**发货明细 (ShipmentItem)**：
一张发货单中按产品 + 客户色号 + 缸号聚合的品类汇总行，记录数量、匹数、单位、单价与金额。随发货单整单聚合保存、级联逻辑删除（见 ADR-0003）。行金额由服务端重算（数量 × 单价）。可关联来源入库明细（追溯用，零回写、不锁库）。

**卷清单 (ShipmentRoll)**：
发货明细下逐卷登记的布卷记录（卷号、实称重量 KG、米数、备注），独立子表存储，随发货单整单聚合保存，软齐套不锁卷（见 ADR-0030）。不设卷号数据库唯一约束。

**发货单号 (ShipmentNo)**：
形如 `SH-YYYYMMDD-NNN`：`SH` + 年月日 8 位 + 3 位日流水（超 999 自然扩位），按天取号，租户内全量唯一含软删行（ADR-0029）。

**发货单状态 (ShipmentStatus)**：
字典 `lunaerp_shipment_status`（id=2245）：`DRAFT` 草稿（缺省）/ `COMPLETED` 已发货 / `CANCELLED` 已取消。仅草稿可编辑删除；审核（发货）同事务写负流水；取消已发货单逐条红冲正流水；取消草稿仅作废不写流水。

**签收状态 (ShipmentSignStatus)**：
字典 `lunaerp_shipment_sign_status`（id=2246）：`PENDING` 待签收（缺省）/ `SIGNED` 已签收 / `REFUSED` 拒签。文员轻量内部登记，仅 COMPLETED 单可登记/修改；拒签不自动红冲库存。

**运费 (FreightFee)**：
发货物流产生的运费金额，独立字段记录，不并入货物金额合计。

**合同未发量 (ContractUnshippedQuantity)**：
销售合同某明细行的合同数量减去该合同已生效的发货出库数量（按 SHIPMENT_OUT 流水实时聚合），只读动态计算，不落库。

## 业务管理：销售退货

**销售退货单 (SalesReturn)**：
客户将已发货布匹退回的业务单据，业务管理域单据（一期仅客户销售退货；纱线采购退货二期）。一张退货单**严格对应一张 COMPLETED 发货单**（客户/合同/币种/原发货仓库从头单快照带出）。业务状态 `DRAFT/APPROVED/RECEIVED/POSTED/CANCELLED` 与 02 号审批五态双列并存：审批通过自动生成 `inType=RETURN` 的 DRAFT 入库单（回写 stock_in_id），仓管到货质检并审核入库单后到 RECEIVED，财务确认写应收反向分录后到 POSTED 终态。仅 DRAFT/APPROVED 可作废（APPROVED 级联取消 DRAFT 入库单），过账后不提供撤销。库存回冲复用入库单通道、不新增台账类型，见 ADR-0042；单号 `RT-YYYYMMDD-NNN`（按天取号，租户内全量唯一含软删行）。
_避免_：退货申请单（门户二期）、采购退货（二期，不同方向）。

**退货行 (SalesReturnItem)**：
退货单中**强制挂发货明细行**血缘的品类行（`shipment_item_id` 必填，杜绝老系统空血缘导致"钱冲了货没进"的 P0），记录退货计费数量、匹数、单位、单价（缺省=原发货单价，可改表达协议折让）、服务端重算金额、行级退货原因（必填）。一行可有 N 个退货卷；`restock_qty`/`scrap_qty` 为到货时按 RESTOCK/SCRAP 卷汇总的双量（入库口径 vs 计费口径）。

**退货卷 (SalesReturnRoll)**：
退货行下逐卷登记的实物记录。卷血缘可选：勾选原发货卷则回指 `shipment_roll_id` 并带回卷号/实称重量/米数（可改成实际退回值，应对裁剪后退回），对不上原卷的散退允许手填卷号。卷号必填、重量与米数至少一项；卷合计与行退货数量软对齐（只提示不强拦，沿用发货软齐套先例）。到货时逐卷质检判定。

**质检判定 (Return QC result)**：
退货卷到厂后的二选一处置，字典 `lunaerp_sales_return_qc_result`：`RESTOCK` 可二次入库（进入退货单头选定仓库，审核入库单时写 STOCK_IN 正流水，库位可空）/ `SCRAP` 废品（**不写库存台账**，仅留判定人/时间/备注）。全部卷判废时自动取消 DRAFT 退货入库单，退货仍可财务确认（货不入库、钱照冲）。废品仓/降级品库存二期。

**可退量 / 可退额 (Returnable quantity / amount)**：
防超退的唯一口径（常量化于 `SalesReturnAvailabilityService`）：可退量 = 发货明细数量 − Σ 占量退货行数量；占量谓词 = `status ≠ CANCELLED 且 approval_status ∈ {APPROVING, APPROVED}`（在途审批单即占量，草稿/驳回不占，作废/撤回释放）。可退额 = 可退量 × **原发货单价**；退货行金额（可改折让价）不得超过可退额。提交时对父发货单加行锁串行化并发退货。

**退货应收冲减 (AR return reversal)**：
财务确认（POSTED）事务内 business 侧发布 `SalesReturnPostedEvent`，finance 监听调 10 号冻结接口 `LedgerService.recordArReturnReversal(...)` 写 category=`AR_RETURN_REVERSAL` 的反向分录，按**原发货入账汇率**折本位币；退货模块不建应收台账/红字对账单。已收款客户冲完余额转贷方由 10 号预收模型承接；红字税务发票一期不做。

## 业务管理：出口单证

**出口单证套 (ExportDocSet)**：
外销业务中一次出具的整套外贸单据的载体，是业务管理域的第三个单据。一套 = 一份共享抬头（买卖双方、合同号、贸易条款、港口、唛头、币种）+ 一份共享货物明细 + 三份单据各自的专属 meta，对应 **PI 形式发票 / CI 商业发票 / PL 装箱单**。共享抬头与明细是三单的唯一真相来源，保证"一套单据必须互相吻合"；三单不各自建单。可从外销合同或发货单生成草稿，也可手工新建。
_避免_：外贸模块、发票单（PI/CI 不是国内税务发票）。

**PI / CI / PL**：
`PI`（Proforma Invoice 形式发票）：报价/订金阶段出具，带有效期、定金比例、收款银行、分批/转运条款；`CI`（Commercial Invoice 商业发票）：出货结算与报关收汇核心，带发票号日期、L/C 号、原产地与真实性声明；`PL`（Packing List 装箱单）：清点用，带箱件数、毛净重、体积、唛头，汇总遵循"手填优先、空则按行求和"（尾箱凑整惯例）。

**单证套号 (ExportDocNo)**：
形如 `ED20260001`：`ED` + 年份 + 4 位顺序号（超 9999 自然扩位），租户内全量唯一、软删不释放（见 ADR-0004 取号规则）。PI/CI/PL 各自的单据号缺省回退显示套号，允许在专属 meta 中手填覆盖。

**单证定稿 (ExportDocFinalize)**：
单证套两态 `DRAFT` 草稿（可编辑、可删、可重新同步）/ `FINALIZED` 已定稿（只读，仅供打印）。定稿要求至少一条货物明细；定稿后可"回退"（revoke）为草稿再改，这是实务常态。两态均可打印（PI 草稿常需先发客户确认定金）。无审批流。

**重新同步 (Resync)**：
草稿单证套按其来源合同/发货单重新取数的动作，规则是**只填空字段、永不覆盖已人工录入/编辑的值**，优先级 发货单 > 合同 > 卖方出口配置。手工新建（无来源）的单证套不能同步。这是"部分带出 + 人工补录"工作模式的闭环，区别于"重新生成覆盖"。

**卖方出口配置 (SellerExportConfig)**：
租户出具出口单证时的默认卖方身份：英文公司名、英文地址、收款银行名/账号/SWIFT/银行地址。一租户一行（独立租户级配置表，不用全局的 infra_config），在出口单证列表页维护；新建单证套时带出为可改快照，此后单证上的修改不回写配置。

**唛头 (Shipping Mark)**：
印刷在外包装上的装运标识（收货人、目的港、件号等），PL 的必备栏位。一期为单证套上的手填文本，合同与发货单均无此数据源。

## 模块边界

销售合同、成本卡与报价、发货管理、出口单证均落在 `business`（业务管理）子包，与 `baseinfo` 平级：销售合同后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.business.salescontract[.item]`，前端 `lunaerp-elmui/business/sales-contract/`；出口单证后端 `…business.exportdoc`（卖方配置为同子包 sellerconfig 资源），前端 `lunaerp-elmui/business/export-doc/`（视图与 API 目录镜像）。成本卡与报价为商业化 W1 售前链，后端 `…business.costcard[.yarn|.fee]`（含 `pricing/CostCardPricingCalculator` 纯函数）与 `…business.quotation[.item]`，**前端落正式目录** `luna-ui/src/views/lunaerp/business/{cost-card,quotation}/`（非 lunaerp-elmui 临时区，API 目录镜像）；URL 前缀 `/lunaerp/business/{cost-card,quotation}`，错误码共用 `1-030-034`；菜单 10132 成本卡（按钮 10133-10135）、10136 报价单（按钮 10137-10140）挂销售目录 10100 下，sort 0 排在合同/发货/出口单证之前（ADR-0039；避让 10126–10131 出口单证）。报价单消费生产域纱价（`production.yarnprice` 取价服务）与配比 BOM，不复制主数据所有权。销售退货后端 `…business.salesreturn`（主/行/卷三表 + 发 `SalesReturnPostedEvent` 给 finance，零编译依赖），前端 `lunaerp-elmui/business/sales-return/`；URL 前缀 `/lunaerp/business/sales-return`，错误码 `1-030-037`；菜单 10141 销售退货（按钮 10142–10149）挂销售目录 10100 下、发货与出口单证之后（sort 4）；库存回冲复用 stock 域入库单（`lunaerp_stock_in` 加 source_doc_type/source_doc_id 通用回指，ADR-0042），应收冲减只调 finance 域 10 号冻结接口。

## 字典

**销售合同字典 (Sales contract dictionaries)**：
合同的枚举字段同样走 `system_dict_*`，前缀 `lunaerp_sales_contract_`：`lunaerp_sales_contract_status`（ACTIVE/COMPLETED/CANCELLED）、`lunaerp_sales_contract_trade_type`（DOMESTIC/EXPORT）、`lunaerp_sales_contract_order_type`（BULK/SAMPLE）、`lunaerp_sales_contract_unit`（KG/M）、`lunaerp_sales_contract_currency`（RMB/USD/EUR/HKD）。

**发货管理字典 (Shipment dictionaries)**：
发货域建两个字典：`lunaerp_shipment_status`（id=2245，DRAFT 草稿 / COMPLETED 已发货 / CANCELLED 已取消）、`lunaerp_shipment_sign_status`（id=2246，PENDING 待签收 / SIGNED 已签收 / REFUSED 拒签）。单位复用库存域 `lunaerp_stock_in_unit`、币种复用销售合同 `lunaerp_sales_contract_currency`；承运商为自由文本不入字典；流水 txn_type（`SHIPMENT_OUT` / `SHIPMENT_OUT_REVERSAL`）为后端内部枚举不入字典。

**成本卡与报价字典 (Cost card & quotation dictionaries)**：
六个业务字典（type id 2262–2265、2267–2268；价类字典 2266 落生产域）：`lunaerp_cost_card_status`（2262，DRAFT/EFFECTIVE/EXPIRED）、`lunaerp_cost_card_basis`（2263，KG/METER）、`lunaerp_cost_card_fee_type`（2264，织造/染色/定型/检验/包装/运费）、`lunaerp_cost_fee_unit`（2265，PER_KG/PER_METER）、`lunaerp_quotation_status`（2267，DRAFT/CONFIRMED/SUPERSEDED/CANCELLED + EXPIRED 仅展示）、`lunaerp_quotation_cost_source`（2268，CARD/MANUAL）。报价币种复用 `lunaerp_sales_contract_currency`、单位复用 `lunaerp_sales_contract_unit`。

**销售退货字典 (Sales return dictionaries)**：
`lunaerp_sales_return_status`（id=2279，DRAFT 草稿/APPROVED 已审批/RECEIVED 已收货/POSTED 已过账/CANCELLED 已作废；审批中/驳回走 02 号 approval_status 字典不入本字典）、`lunaerp_sales_return_reason`（id=2280，QUALITY 质量问题/WRONG 发错货/EXCESS 多发/CUSTOMER 客户原因/OTHER 其他，行级必填）、`lunaerp_sales_return_qc_result`（id=2281，RESTOCK 可二次入库/SCRAP 废品，逐卷到货判定）。币种复用销售合同字典、单位复用入库单位字典；审批 biz_type=`SALES_RETURN` 在 02 号 `lunaerp_approval_biz_type` 字典内增量；退货入库复用库存域 `lunaerp_stock_in_type` 的 RETURN 值（3597），不新增入库类型。

**出口单证字典 (Export doc dictionaries)**：
`lunaerp_export_doc_status`（id=2258，DRAFT/FINALIZED）、`lunaerp_incoterm`（id=2259，FOB/CIF/CFR/EXW，外销合同外贸条款与单证共用）、`lunaerp_payment_term`（id=2260，TT/LC/DP/DA/OA，同上共用）、`lunaerp_transport_mode`（id=2261，SEA/AIR/LAND/EXPRESS）。币种复用销售合同字典；PI meta 内分批/转运（ALLOWED/NOT_ALLOWED）为前端 TS 联合类型不建字典。

## 错误码

错误码段位总表与新模块取段规则见 `docs/contexts/shared/GLOSSARY.md`。本域占用段位：

- `1-030-003-xxx` 销售合同（主表与明细共用）
- `1-030-034-xxx` 成本卡与报价（ADR-0039；段内分区：000–099 成本卡 / 100–199 报价单）：000 成本卡不存在 / 001 成本卡号重复 / 002 成本卡状态不允许操作 / 003 存在缺价纱线不能生效 / 004 产品无有效配比 BOM / 005 缺克重门幅无法 KG/M 换算；100 报价单不存在 / 101 报价单号重复 / 102 报价状态不允许操作 / 103 非最新版本禁止修订或转合同 / 104 同号已存在草稿 / 105 外币缺汇率 / 106 毛利率非法（区间外或等于 100%）/ 107 报价与合同币种不一致。
- `1-030-016-xxx` 发货单（主表/明细/卷清单共用）：000 发货单不存在 / 001 发货单号重复 / 002 状态不允许该操作 / 003 关联引用不存在（客户/仓库/合同/产品/来源入库明细/业务员）/ 004 审核失败发货单没有明细 / 005 库存不足（返回需求量与可用量）/ 006 签收状态非法。
- `1-030-033-xxx` 出口单证（段内分区：000 单证套 / 100 明细 / 200 卖方配置）：000 单证套不存在 / 001 套号重复 / 002 状态不允许该操作 / 003 关联引用不存在 / 004 定稿失败没有货物明细 / 005 合同非外销不能生成 / 006 手工单无来源不能重新同步；100 明细不存在 / 101 明细跨单修改。
- `1-030-037-xxx` 销售退货（主表/行/卷共用，ADR-0042）：000 退货单不存在 / 001 退货单号重复 / 002 状态不允许该操作 / 003 发货单不存在或未审核 / 004 退货行血缘非法（未挂发货明细或不属于头单） / 005 退货数量超可退量（返回需求量与可退量） / 006 退货金额超可退额 / 007 退货卷非法（卷号缺失/重量米数双空/卷血缘不属于头单） / 008 审批未通过不可到货或过账 / 009 退货入库单缺失或状态异常（被人工审核/取消） / 010 存在未质检判定的卷 / 011 可入库卷缺行单位数量或入库重算不一致 / 012 有可入库卷但入库单未审核完成，不可财务确认 / 013 退货币种与发货单不一致 / 014 原发货入账汇率缺失无法冲减 / 015 退货原因不能为空且必须为有效枚举值。
