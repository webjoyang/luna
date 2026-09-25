# 项目术语表

本文件是 Luna ERP 仓库的权威术语表。这里只收录领域概念，不写实现细节。
当对话或代码中含糊使用某个词时，以此文件为准。

## 客户与联系人

**客户 (Customer)**：
企业或个人，业务销售/服务的对象。区别于 `User`（系统登录身份）：客户是市场侧实体，User 是系统侧身份。一个客户有 12 个基础档案字段（companyName、shortName、phone、address、clientType、industry、scale、source、cooperationStatus、unifiedSocialCreditCode、remark、status），并可拥有多条 `客户联系人`。
_避免_：Client、BusinessClient、客户主体。旧系统 Prisma 模型叫 `BusinessClient`，新系统正名为 `customer`，理由是中文"客户"对应 `customer` 更直接，同时避开旧系统 `clientType` 字段被业务部隔离逻辑占用导致的命名冲突。公司名字段最初为 `company`，后与供应商模块统一正名为 `companyName`（列 `company_name`）。

**客户联系人 (CustomerContact)**：
关联到某个客户的人（如采购、财务、仓管）。P1 含 6 个字段：name、position、phone、email、remark、isPrimary。作为独立子资源处理，有自己的 CRUD 端点与权限码，不嵌在父级客户的保存载荷里整体替换。
_避免_：ClientContact、联系人（无前缀时歧义）。

**客户工作台 (CustomerWorkspace)**：
查看/编辑单个客户的多 Tab 容器。7 个 Tab 与旧系统 tab-config 对齐：基础档案 / 联系人 / 合同 / 寄样 / 画像分析 / 位置分布 / 拜访记录。P1 激活"基础档案"与"联系人"，其余 5 个为"开发中"占位。

## 供应商与联系人

**供应商 (Supplier)**：
向本企业供货或提供加工服务的外部工厂、公司或档口（如纱线厂、坯布厂、染厂、门市部等），是采购入库与付款的往来主体。区别于 `客户`（销售侧）与 `User`（系统登录身份）。一个供应商 MVP 含 9 个基础档案字段：supplierNo、companyName、shortName、phone、address、supplierType、machineType、remark、status，并可拥有多条 `供应商联系人`。
_避免_：Vendor、厂商（口语）、供货方。旧系统 Prisma 模型即名 `Supplier`（英文沿用），但旧字段 `code` 正名为 `supplierNo`、公司名字段与客户统一为 `companyName`。旧系统的银行信息(bankName/bankAccount)、经纬度、工厂照片与付款资料附件、财务统计(totalPaid/totalOwed)、登录账号关联、businessOwnership、extensionData 自定义字段、草稿/审批态均不进入 MVP。

**供应商联系人 (SupplierContact)**：
关联到某个供应商的人（如接单业务员、财务、档口人员）。字段与独立子资源模式与 `客户联系人` 完全同构：name、position、phone、email、remark、isPrimary，拥有独立 CRUD 端点与权限码，不随父级整体保存替换。
_避免_：VendorContact、SupplierLinkman。

**供应商编号 (SupplierNo)**：
形如 `SUP20260001`：`SUP` + 年份 + 4 位顺序号（超 9999 自然扩位），租户内全量唯一（含软删行、不重用），新建缺省由服务端生成、也可手填（撞号报错），并提供只读的"预览下一个编号"接口。规则与合同编号同构，见 ADR-0005。
_避免_：code、supplierCode（旧字段名）。

**供应商类型 (SupplierType)**：
供应商的经营类别，字典 `lunaerp_supplier_type`。MVP seed 18 项：纱线厂、坯布厂、工贸一体、绣花厂、染厂、扎染、印花厂、烫金厂、压花厂、复合厂、外贸公司、检验仓库、拉毛摇粒厂、水洗厂、小缸厂、门市部、检测中心、货运。字段可空，后续由系统字典后台维护（MVP 不做旧系统的前端自助增删与待审核）。

**供应商工作台 (SupplierWorkspace)**：
查看/编辑单个供应商的多 Tab 容器。MVP 激活"基础档案"与"联系人"两个 Tab；"入库记录 / 付款记录 / 关联品种 / 位置分布"为"开发中"占位，分别对应旧系统的 StockIn、PaymentRequest、Product/GreigeBatch、地图定位关联。

**主营机型 (machineType)**：
供应商主要设备/加工能力的自由文本说明（如"34寸28针""喷水织机"），可空。区别于 `供应商类型`（分类枚举）。

## 业务管理：销售合同

**销售合同 (SalesContract)**：
与客户签订的布匹销售约定，记载交易条款与商品明细，是业务管理域的第一个单据。
_避免_：合同（无前缀时与未来的采购合同歧义）、订单。

**合同明细 (SalesContractItem)**：
一份销售合同中，按单个品种（客户色号 / 门幅 / 克重组合）逐行记录的数量、单价、其他费用与金额条目。一份合同含 0~N 条明细，允许先建档后补明细。明细是合同聚合的一部分，随合同整体保存、级联删除，没有独立操作界面（与客户联系人的独立资源模式刻意相反，见 ADR-0003）。
_避免_：合同商品、明细行（口语）。

**贸易类型 (TradeType)**：
`DOMESTIC` 内销（缺省）/ `EXPORT` 外销，区分销售方向。MVP 仅作标记；外贸专属字段（贸易术语、港口、英文抬头等）与出口单证在后续版本补。

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

## 生产管理：胚布单

**胚布单 (GreigeOrder)**：
向坯布厂下达的织布委托单，记载品种规格、投胚/成品数量、损耗与交期，是生产管理域的第一个单据。
_避免_：胚布指示单、GreigeInstruction（旧系统名称，保留以追溯）、订单。

**颜色投胚分配 (GreigeOrderColor)**：
一张胚布单中按颜色逐行拆分成品下单量与损耗，并据此折算投胚需求的条目。一份胚布单含 0~N 行，随单聚合保存、级联删除，没有独立操作界面（同合同明细模式，见 ADR-0003）。
_避免_：颜色明细（口语）、sourceColors（旧系统 JSON 字段名）。

**承织坯布厂 (GreigeFactory)**：
承担本单织造任务的供应商，必填；一张胚布单一个承织方。引用供应商档案并保存公司名快照，供应商改名不回写。

**指定染厂 (DyeingFactory)**：
本单预设的后续染色供应商，可空——下单时染厂可能尚未确定。同样引用供应商档案并保存名称快照。

**计划投胚量 (PlannedQty)**：
下达给坯布厂的总投胚量，属工厂经验口径。它与颜色投胚分配按成品量折算的投胚需求之和**刻意不强制相等**：两者口径不同，强一致会挡住正常单据。

**胚布单号 (GreigeOrderNo)**：
形如 `PB0919001`：`PB` + 月日 + 3 位顺序号（超 999 自然扩位），**按天取号**，租户内全量唯一、软删不释放（见 ADR-0006）。新建缺省由服务端生成、也可手填（撞号报错）。

**胚布单状态 (GreigeOrderStatus)**：
`ACTIVE` 进行中（新建缺省）/ `COMPLETED` 已完成 / `CANCELLED` 已取消，与销售合同状态口径一致。MVP 无审批流，由编辑页手动切换，不做流转约束。

## 生产管理：染色单

**染色单 (DyeingOrder)**：
向染厂下达的染色委托单，记载客户/合同/产品规格/染色要求与颜色明细，是生产管理域的第二个单据（继胚布单之后）。MVP 不接审批流、不做坯布库存扣减、不联动染缸批次卡与检验单。无业务编号（与胚布单/合同不同，染色单不带 order_no）。关联字段全部存 ID + 名称快照：`salesContractId`+`contractNo`（合同）、`customerId`+`clientName`（客户，从合同带出）、`dyeingFactoryId`+`dyeingFactoryName`（承染方，供应商快照），与项目其他单据惯例一致。
_避免_：染色指示单、DyeingInstruction（旧系统名称，保留以追溯）、染单（口语）。

**染色颜色明细 (DyeingColor)**：
一张染色单中按颜色逐行拆分染色要求与数量的条目。一份染色单含 0~N 行，随单聚合保存、级联删除，没有独立操作界面（同合同明细与胚布单颜色分配模式，见 ADR-0003）。明细字段：customerColorNo（客户色号，从合同明细 customerColor 带出可改）、colorName（颜色名称，手填）、quantity（本单染色数量，成品口径）、pieces（匹数）、dyeingInputQty（投染数，含损耗的实际投入量）、dyeingInputUnit（投染数单位，复用字典 lunaerp_sales_contract_unit：KG/M）、contractQty（合同数量，从合同明细 quantity 带出只读对比）。
_避免_：颜色明细（口语）、colors（旧系统 JSON 字段名）。

**染色技术规格与色牢度 (Dyeing technical specs & fastness)**：
染色单主表上的一组工艺/质量要求字段，与参考项目 DyeingInstruction 对齐：loss（损耗率）、ph（pH 值）、shrinkage（缩水率）、fastnessWash（水洗牢度）、fastnessSoap（皂洗牢度）、fastnessDryRub（干摩擦牢度）、fastnessWetRub（湿摩擦牢度）、fastnessLight（日晒牢度）、fastnessPerspiration（汗渍牢度）、fastnessDryClean（干洗牢度）、fastnessSixFiber（六千沾色牢度）、otherReq（其他要求）。均为可空文本，不建字典。

**承染方 (DyeingFactory)**：
承担本单染色任务的供应商，必填；一张染色单一个承染方。引用供应商档案并保存公司名快照。与胚布单的"指定染厂"是同一概念的复用——胚布单下单时若已确定染厂则填指定染厂，染色单下单时落实为本单的承染方。

**染色单状态 (DyeingOrderStatus)**：
四态：`DRAFT` 草稿（缺省）/ `ACTIVE` 进行中 / `COMPLETED` 已完成 / `CANCELLED` 已取消。比销售合同/胚布单多一个 `DRAFT` 态，因为染色单业务上存在"先打草稿反复确认、再下达给染厂"的形态，这是与合同/胚布单的刻意不一致。保存时默认为草稿，点"下达"转 ACTIVE；编辑页可手动切换 COMPLETED/CANCELLED。

## 生产管理：纱线采购

**纱线材质 (YarnType)**：
纱线按材质类型归档的第一级主数据，是纱线规格的父级。`lunaerp_production_yarn_type` 主表仅含 name/sort/status + 框架列，seed 7 项：棉/涤/人棉/氨纶/锦纶/粘胶/混纺。**材质类型不入字典**（数量稳定、枚举式直接 seed 表，见 ADR-0011）。
_避免_：纱线类型、YarnCategory（旧系统术语保留追溯）。

**纱线规格 (YarnSpec)**：
具体可采购的纱线规格，挂在材质之下，是采购明细行直接关联的实体。`lunaerp_production_yarn_spec` 子表含 yarn_type_id 关联 + spec_name（如"32S 棉"）+ spin_method（纺纱工艺）+ luster（光泽）+ standard_name（标准名称，唯一索引 [tenant_id, yarn_type_id, standard_name]）+ ref_price（参考单价）+ remark/status。**ref_price 是测算时历史价回填的兜底来源**（ADR-0011）。

**纱线配比 BOM (YarnRecipeBom)**：
某产品品种生产时所需的纱线配比清单，是纱线需求量测算的**唯一权威源**。`lunaerp_production_yarn_recipe_bom` 主表通过 product_id 强关联产品档案（含 product_code/product_name 快照），明细表 `lunaerp_production_yarn_recipe_bom_item` 含 yarn_type_id + yarn_spec_id + ratio（**百分比数字，49 = 49%**）+ spin_method + remark + ref_price。
_避免_：复用产品档案 yarn_specs 字段（旧系统方案，已废弃，见 ADR-0011）。配比权威源独立成表是因为配比与产品档案生命周期不同，且需单独维护与版本化。

**纱线采购单 (YarnOrder)**：
向纱线供应商下达的纱线采购委托单，记载一批胚布指示单共同驱动的纱线采购需求。`lunaerp_production_yarn_order` 主表通过 `source_instructions`（Text JSON 数组）关联多张胚布指示单 ID，多供应商挂在明细行（不在主表），含 order_date/delivery_date/loss_rate（**百分比数字，5 = 5%**）/status/remark。命名 `yarnorder`（不使用 yarnpurchase，见用户决策）。
_避免_：纱线采购指令、YarnPurchase（命名已纠正）。

**纱线采购明细 (YarnOrderItem)**：
采购单中按纱线规格逐行展开的采购条目。`lunaerp_production_yarn_order_item` 明细表关联 yarn_type_id + yarn_spec_id + supplier_id（多供应商挂在明细行 + 公司名快照），含三量字段 `required_qty`/`stock_qty`/`purchase_qty`（需求量/在库量/实采量）+ ratio（百分比数字）+ loss_rate（百分比数字）+ base_qty（基础量）+ unit_price + total_amount + ref_unit_price（历史价回填）+ remark。**无 line_no 行号字段**（用户决策）。明细随主表聚合保存、级联逻辑删除（同 ADR-0003 模式）。

**纱线采购单号 (YarnOrderNo)**：
形如 `YP20260920001`：`YP` + 年月日 + 3 位日流水（超 999 自然扩位），**按天取号**，租户内全量唯一、软删不释放（仿胚布单 ADR-0006 模式）。新建缺省由服务端生成、也可手填（撞号报错）。

**纱线采购单状态 (YarnOrderStatus)**：
四态：`DRAFT` 草稿（缺省）/ `ACTIVE` 进行中 / `COMPLETED` 已完成 / `CANCELLED` 已取消，仿染色单 ADR-0008。MVP 无审批流。（原规划完成时写 STOCK_IN 台账已于 2026-09-21 撤销回退，库存台账暂缓开发）。

**纱线库存台账 (StockLedger)**：
（已于 2026-09-21 撤销回退，暂不开发；前后端程序与表全部取消）。

**测算口径 (Yarn demand calculation)**：
按"勾选胚布指示单 → 查 yarn_recipe_bom 拿配比 → 按纱线规格合并多单需求 → 算实采量"的核心业务流测算。算法：
- `baseQty = Σ(胚布单成品量 × ratio/100)`，按 yarn_spec_id 合并多张胚布单
- `requiredQty = baseQty × (1 + lossRate/100)`（lossRate 为百分比数字 5 = 5%）
- `purchaseQty = requiredQty`（库存台账未启用，在库量 stockQty 缺省 0）
- `totalAmount = unitPrice × purchaseQty`（金额服务端权威重算）

**与胚布单 loss_rate 小数+除法公式刻意不一致**：胚布单用 `demand_qty = finished_qty ÷ (1 - loss_rate)`（loss_rate 为小数 0.05 = 5%）。这是两个单据业务语义不同（胚布单是投胚需求折算、纱线采购单是含损耗的采购量），刻意不一致（ADR-0010）。

测算接口为后端 `POST /lunaerp/production/yarn-order/calculate`，前端只展示结果不做计算（ADR-0010）。

**入库来源 (StockInSource)**：
（已于 2026-09-21 撤销回退，暂不开发）。

## 生产管理：检验单

**检验单 (InspectionOrder)**：
对胚布或成品布逐卷检验的委托单，记载目标量、承检方与每卷录检结果，是生产管理域的检验环节单据。MVP **只做检验单自身**（主表 + 录检明细 + 基本 CRUD + 状态机 + 编号），**不实现同步入库**——同步入库作为后续独立 issue，待入库单模块就绪后补做（ADR-0013）。`type` 区分两类：`GREIGE` 胚布检验（关联 `greigeOrderId`）/ `FINISHED` 成品检验（关联 `dyeingOrderId`），上游单据均已存在。MVP 不支持"从染色/胚布单带入生成"，纯手工新建。
_避免_：检验指示单、InspectionInstruction（旧系统名称，保留以追溯）。

**录检明细 (InspectionOrderItem)**：
检验单下逐卷录入的检验结果条目。字段：`rollNo`/`vatNo`（卷号/缸号，必填）/ `measuredWeight`（实称重量 KG）/ `lengthM`（米数）/ `machineNo`（机台号，可空）/ `grade`（等级，字典 `lunaerp_inspection_grade`，可空）/ `colorName`/`colorNo`/`styleNo`（颜色/色号/款号，可空）/ `productCode`+`productName`（冗余快照，为后续同步预留）/ `pieceCount`（匹数，缺省 1）/ `remarks`。本期不做 `defects` 疵点数组。**作为独立子资源处理**，有自己的 CRUD 端点与权限码（刻意不一致 ADR-0003 聚合保存模式，理由见 ADR-0014），逐卷增删改；删主表时级联逻辑删除明细（与合同明细/胚布颜色级联模式一致）。**单内 `(vatNo, rollNo)` 唯一性由 service 层校验，不建数据库唯一约束**（用户决策）。

**承检方 (InspectionFactory)**：
承担本单检验任务的供应商，可空；引用供应商档案并保存公司名快照（`inspectionFactoryId`+`inspectionFactoryName`）。命名 `inspectionFactory` 而非 `dyeingFactory`，因 `type=GREIGE` 胚布检验时承检方可能是坯布厂/检验仓库/检测中心（供应商字典含这些类型），未必是染厂，`dyeingFactory` 在胚布检验场景下有命名歧义。

**检验单号 (InspectionOrderNo)**：
形如 `INS-20260921-001`：`INS` + `-` + 年月日 8 位 + `-` + 3 位日流水（超 999 自然扩位），**按天取号**，租户内全量唯一、软删不释放。**带连字符分隔**，与项目其他单据编号（合同/胚布单/纱线采购单/供应商均无连字符）刻意不一致，理由见 ADR-0015。新建缺省由服务端生成、也可手填（撞号报错）。

**检验单状态 (InspectionOrderStatus)**：
四态：`DRAFT` 草稿（新建缺省）/ `ACTIVE` 录检中 / `COMPLETED` 录检完成 / `CANCELLED` 已取消，仿染色单/纱线采购单四态。MVP 无审批流，编辑页手动切换，不做流转约束（与合同/胚布单/染色单 MVP 惯例一致）。第一卷录检保存时由后端便利地把 DRAFT 转 ACTIVE（不强约束）。**不含 `SYNCED` 态**——同步入库功能本期延后（ADR-0013），`SYNCED` 留给同步上线时插入，避免出现不可达死状态。

**检验等级 (InspectionGrade)**：
字典 `lunaerp_inspection_grade`（id=2232），4 项：`A` 一等品 / `B` 二等品 / `C` 三等品 / `DEFECTIVE` 等外品。本期无同步入库，grade 仅作记录与列表筛选，不映射到入库卷状态（rollStatus 是同步入库的概念，本期不入字典）。

## 库存管理

**入库单 (StockIn)**：
登记货物实际入库的库存凭证，是库存管理域（stock）的第一个单据，也是继 baseinfo/business/production 之后第四个顶级业务域。入库单是**库存账的凭证**：审核即在同事务写库存流水（过账），已审核单只能取消（红冲），状态机严格守卫（ADR-0016），刻意不一致合同/胚布/染色/检验单 MVP 的"编辑页自由切态"惯例。主表含入库类型、入库仓库（一单一仓库、必填）、供应商（可空）、销售合同/胚布单/染色单/胚布品种四类上游关联（**全部可空、仅存在性校验与名称快照、零回写零联动**）、金额合计、单据日期。主表**不设数量合计**：明细单位允许 KG/M 混用，跨单位求和无意义。
_避免_：入库指示单、收货单。旧系统 Prisma 模型即名 `StockIn`（英文沿用），旧系统的审批态/QC_PENDING、应付付款/对账财务锁、订单进度回写、批次转可用、OCR 智能录入、多合同分摊均不进入 MVP。

**入库类型 (StockInType)**：
字典 `lunaerp_stock_in_type`（id=2234），回答"货从哪来"的四类稳定场景：`PURCHASE` 采购入库 / `PRODUCTION` 生产入库（胚布、成品布生产回库）/ `RETURN` 退货入库 / `OTHER` 其他入库。不沿用旧系统的纺织链路细分 8 型（GREIGE/YARN/DYEING/WASHING/POST/PRODUCT/INSPECTION/SPOT）——"是什么货"由明细产品表达，细分类型是为旧系统批次联动服务的，本期联动不做。采购退货（出库方向）不属本类型集，随出库模块再引入。

**入库明细 (StockInItem)**：
入库单中按产品逐行登记的实收条目。字段：产品（必填，关联产品档案 + 编码/名称快照，不支持无产品档案的杂项入库）、batchNo 批次号/缸号（自由文本）、rollNo 卷号、pieceCount 匹数（缺省 1）、quantity 数量（>0）、unit 单位（KG/M，**允许同单不同行混用单位**）、unitPrice 单价、amount 金额（=数量×单价，服务端权威重算）、remark。明细**随入库单聚合保存、级联删除，无独立端点/权限码**（ADR-0003 模式；与检验单录检明细的独立子资源 ADR-0014 刻意不同）。允许零明细草稿，审核时至少一条明细。

**库位分配 (StockInPlacement)**：
入库明细到库位的放置记录（`lunaerp_stock_in_item_placement`）。一条明细可拆多条放置：库位（必须属于本单入库仓库的启用库位）+ 匹数 + 数量 + 备注，表达"同一产品行 10 匹分放两个库位"。随入库单聚合保存，无独立端点。MVP **不强制齐套**：不校验放置匹数合计等于明细（前端仅提示"已分配 X/共 Y 匹"），未分配明细审核照常过账（流水库位为空），支持先入账后上架。放置记录是库存流水按库位展开的数据来源（ADR-0017/0018）。

**仓库 (Warehouse)**：
存放货物的物理仓库主数据。字段：仓库编号 warehouseNo（`WH`+年份+4 位流水，如 WH20260001，服务端取号/可手填，租户内全量唯一含软删行）、名称必填、地址、状态（ACTIVE/INACTIVE，不建字典）、备注。删除守卫：下有库位或已被入库单引用则禁止删除、引导停用；停用仓库不可被新单选择，历史快照不变。一张入库单只能入一个仓库。
_避免_：WarehouseMap（旧系统含 2D 平面图画布/尺寸/版本，MVP 不做）。

**库位 (WarehouseLocation)**：
仓库内的具体存放位置，两级简版（仓库→库位，无库区）。字段：所属仓库、code 编码（手填如 `A-01-02`，**同一仓库内唯一**含软删行）、名称、排序、状态、备注。明确不建旧系统的库区（WarehouseZone）、平面图、行列层/xyz 坐标、容量、二维码（ADR-0018）。被放置记录引用的库位禁止删除。

**库存台账/流水 (StockLedger)**：
产品通用的库存流水账（`lunaerp_stock_ledger`），只存流水不存余额（ADR-0017，**supersedes 已废弃的纱线专用台账 ADR-0012**）。由入库单审核/红冲在同事务写入：STOCK_IN 正数、STOCK_IN_REVERSAL 负数红冲；流水只增不改不删。携带产品、仓库（必填）、库位（可空）、批次、带符号数量/金额、单位、关联入库单（类型+ID+单号）。收支类型 txn_type 为后端内部枚举，不入字典。

**库存余额 (StockBalance)**：
不是表，而是流水实时聚合结果：`SUM(quantity) GROUP BY 产品 + 单位 + 仓库`。本期提供只读"库存查询"（余额列表 + 流水明细分页两个 Tab）。库位级余额、负库存校验、初始库存导入本期均不做（库存账户从 0 起）。

**入库单号 (StockInNo)**：
形如 `IN-20260923-001`：`IN` + `-` + 年月日 8 位 + `-` + 3 位日流水（超 999 自然扩位），**按天取号**，租户内全量唯一、软删不释放。沿用检验单号 ADR-0015 的连字符风格（与 SUP/CH/PB/YP 无连字符惯例刻意不同，本期第二个带连字符编号）。新建缺省服务端生成、也可手填（撞号报错），提供预览下一个编号接口。

**入库单状态 (StockInStatus)**：
三态严格状态机（ADR-0016，字典 `lunaerp_stock_in_status`，id=2233）：`DRAFT` 草稿（缺省，可改可删）/ `COMPLETED` 已审核（过账完成，只读，只能红冲取消）/ `CANCELLED` 已取消（终态）。无 ACTIVE（审核是瞬时动作）、无 QC_PENDING（质检归检验单）、无审批流。草稿取消不写流水；已审核取消按原流水逐条写红冲。审核/红冲权限 `:audit` 与制单 `:create/:update` 独立分权。

## 状态与生命周期

**客户状态 (Status)**：
两态：`ACTIVE`（启用，新建缺省） / `INACTIVE`（停用）。旧系统的 `PENDING_APPROVAL` / `REJECTED` 审批态在 P1 不引入（无审批流），后续上审批流时再追加。供应商状态与此完全同构（旧系统另有 `DRAFT` 草稿态，MVP 不引入）。

**客户简称 (shortName)**：
客户与供应商共用的展示缩写。用户未填时由后端自动用 `companyName` 回填。列表展示优先用 `shortName || companyName`。

## 模块边界

**模块落点 (Module placement)**：
客户功能落在 `lunaerp` 模块的 `baseinfo`（基本信息）子包下，前后端一致：

- 后端：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.baseinfo.customer[.contact]`
- 前端视图：`luna-ui/src/views/lunaerp-elmui/baseinfo/customer/`
- 前端 API：`luna-ui/src/api/lunaerp-elmui/baseinfo/customer/`

`lunaerp-elmui`（lunaerp 的 Element-Plus UI 区块）是 `views/` 下的新目录，与既有的 `views/lunaerp/` 平级。它是一个**临时快速实现版**：用 Element Plus 快速搭出业务核心，先跑通前后端服务与关键流程。**未来 lunaerp 业务视图的正式 UI 仍然落到默认目录 `views/lunaerp/`**，`lunaerp-elmui/` 只承担"先行验证"的角色。详见 ADR-0001。

销售合同落在 `business`（业务管理）子包，与 `baseinfo` 平级：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.business.salescontract[.item]`，前端 `lunaerp-elmui/business/sales-contract/`（视图与 API 目录镜像）。

胚布单落在 `production`（生产管理）子包，与 `baseinfo` / `business` 平级：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.production.greigeorder[.color]`，前端 `lunaerp-elmui/production/greige-order/`（视图与 API 目录镜像）。

纱线采购域同样落在 `production` 子包下，包含 4 个并列子包：`production.yarntype`（材质 CRUD，无字典）、`production.yarnspec`（规格 CRUD + cascade-options 两级级联数据源）、`production.yarnrecipebom`（配比 BOM 主从表 CRUD + get-by-product-id）、`production.yarnorder`（采购单主从表 CRUD + 状态操作 + calculate 测算接口；原 stockledger 台账已于 2026-09-21 撤销回退）。前端视图 `lunaerp-elmui/production/yarn-master/`（材质+规格合并管理页）、`yarn-recipe-bom/`（配比 BOM 管理页）、`yarn-order/`（采购单列表+编辑页）。URL 前缀 `/lunaerp/production/{yarn-type|yarn-spec|yarn-recipe-bom|yarn-order}`，权限码前缀 `lunaerp:production:<子域>:`。详见 ADR-0011。

供应商与客户同落 `baseinfo` 子包：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.baseinfo.supplier[.suppliercontact]`；前端视图 `luna-ui/src/views/lunaerp-elmui/baseinfo/supplier/`、前端 API `luna-ui/src/api/lunaerp-elmui/baseinfo/supplier/`（含 supplier-contact）。URL 前缀 `/lunaerp/baseinfo/supplier[-contact]`，权限码前缀 `lunaerp:baseinfo:supplier[-contact]:`。

检验单落 `production` 子包（与胚布单/染色单/纱线同层）：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.production.inspectionorder[.item]`；前端视图 `luna-ui/src/views/lunaerp-elmui/production/inspection-order/`、前端 API `luna-ui/src/api/lunaerp-elmui/production/inspection-order/`（含 inspection-order-item）。URL 前缀 `/lunaerp/production/inspection-order[-item]`，权限码前缀 `lunaerp:production:inspection-order[-item]:`，错误码段 `1-030-010-xxx`（主表与明细共用）。**录检明细作为独立子资源**（刻意不一致 ADR-0003，理由见 ADR-0014）。菜单挂 10200 生产管理目录下：10260 检验单菜单 + 10261-10263 主表按钮 + 10264-10267 明细按钮（仿供应商 10050-53/10060-63 同构）。

库存管理落新建的 `stock` 顶级子包（与 baseinfo/business/production 平级），含 4 个并列子包：`stock.stockin`（入库单主表 + 明细 + 库位分配三个 DO 同包，聚合保存，仅主表有 Controller）、`stock.stockledger`（库存流水写入与余额/流水只读查询）、`stock.warehouse`（仓库 CRUD + 取号 + 删除守卫）、`stock.warehouselocation`（库位 CRUD + 仓内唯一 + 删除守卫）。前端视图 `lunaerp-elmui/stock/stock-in/`（列表 + 工作台：基本信息/入库明细含 PlacementDialog/操作日志占位）、`stock/inventory-balance/`（余额 + 流水两 Tab）、`stock/warehouse/`（仓库/库位主从同页）；前端 API 镜像于 `api/lunaerp-elmui/stock/`。URL 前缀 `/lunaerp/stock/{stock-in|stock-ledger|inventory-balance|warehouse|warehouse-location}`，权限码前缀 `lunaerp:stock:<子域>:`（入库单含独立 `:audit` 审核/红冲权限），错误码段 `1-030-011`（入库单/明细/放置）、`1-030-012`（台账/余额，本期只读预留）、`1-030-013`（仓库/库位）。菜单新建 10300 库存管理目录（sort 103）：10310 入库管理 + 10311-10314（含审核按钮）、10320 库存查询、10330 仓库管理 + 10331-10336。

**租户 (Tenant)**：
多租户通过后端 `TenantBaseDO` 强制（自动注入 `tenant_id` 列、自动过滤查询）。Customer/CustomerContact 与 Supplier/SupplierContact 都继承 `TenantBaseDO`。

**软删除 (Soft delete)**：
记录通过 `BaseDO`/`TenantBaseDO` 继承的 `deleted` int2 列做逻辑删除，查询自动过滤 `deleted = 0`。客户与供应商删除时 service 层都先置 `status = 'INACTIVE'` 再走逻辑删除（与 product 模块模式一致），便于忽略 `deleted` 的审计/列表查询仍能看到生命周期变化。供应商编号的唯一索引与取号扫描显式包含已删除行（ADR-0005），是该过滤机制的刻意例外。

## 字典

**客户字典 (Customer dictionaries)**：
客户的 `clientType` / `industry` / `scale` / `source` / `cooperationStatus` 五个枚举性质字段统一接入 yudao 框架的 `system_dict_*` 机制，字典 type 命名前缀 `lunaerp_customer_`：`lunaerp_customer_client_type` / `lunaerp_customer_industry` / `lunaerp_customer_scale` / `lunaerp_customer_source` / `lunaerp_customer_cooperation_status`。前端用 `<DictTag>` 渲染。

**销售合同字典 (Sales contract dictionaries)**：
合同的枚举字段同样走 `system_dict_*`，前缀 `lunaerp_sales_contract_`：`lunaerp_sales_contract_status`（ACTIVE/COMPLETED/CANCELLED）、`lunaerp_sales_contract_trade_type`（DOMESTIC/EXPORT）、`lunaerp_sales_contract_order_type`（BULK/SAMPLE）、`lunaerp_sales_contract_unit`（KG/M）、`lunaerp_sales_contract_currency`（RMB/USD/EUR/HKD）。

**胚布单字典 (Greige order dictionaries)**：
胚布单 MVP 只有一个枚举字段走字典：`lunaerp_greige_order_status`（ACTIVE/COMPLETED/CANCELLED）。数量单位统一 KG，不建单位字典。

**供应商字典 (Supplier dictionaries)**：
供应商枚举字段走 `system_dict_*`，前缀 `lunaerp_supplier_`：MVP 仅 `lunaerp_supplier_type`（18 项经营类别，见 `供应商类型`）。状态 ACTIVE/INACTIVE 不建字典，与客户一致。

**纱线采购域字典 (Yarn domain dictionaries)**：
纱线采购域只建一个字典：`lunaerp_yarn_order_status`（DRAFT/ACTIVE/COMPLETED/CANCELLED，仿染色单）。**纱线材质不入字典**（直接 seed 表，见 ADR-0011）。纱线规格的 spin_method/luster 等均为自由文本不建字典。库存台账 type 枚举不入字典（仅后端服务内部使用）。

**检验单字典 (Inspection order dictionaries)**：
检验单域建两个字典：`lunaerp_inspection_order_status`（id=2231，DRAFT/ACTIVE/COMPLETED/CANCELLED，仿染色单/纱线采购单四态）、`lunaerp_inspection_grade`（id=2232，A 一等品 / B 二等品 / C 三等品 / DEFECTIVE 等外品，4 项）。type 字段（GREIGE/FINISHED）不入字典，仅后端枚举使用。本期无同步入库，不建入库卷状态（rollStatus）字典。

**库存管理字典 (Stock dictionaries)**：
库存域建三个字典：`lunaerp_stock_in_status`（id=2233，DRAFT/COMPLETED/CANCELLED 三态严格过账，ADR-0016）、`lunaerp_stock_in_type`（id=2234，PURCHASE 采购入库/PRODUCTION 生产入库/RETURN 退货入库/OTHER 其他入库）、`lunaerp_stock_in_unit`（id=2235，KG/M；库存域独立单位字典，不复用 lunaerp_sales_contract_unit）。仓库/库位的 ACTIVE/INACTIVE 状态不建字典（对齐客户/供应商）；流水 txn_type（STOCK_IN/STOCK_IN_REVERSAL）为后端内部枚举，不入字典（同 ADR-0012 先例）。

## 错误码

**错误码段位全表**：`1-030-001-xxx` 产品、`1-030-002-xxx` 胚布品种（`GreigeErrorCodeConstants`，随产品模块迁移先行占用）、`1-030-003-xxx` 销售合同（主表与明细共用）、`1-030-004-xxx` 客户（customer 与 customer-contact 共用）、`1-030-005-xxx` 供应商（supplier 与 supplier-contact 共用：000 供应商不存在 / 001 供应商编号重复 / 002 供应商联系人不存在）、`1-030-006-xxx` 胚布单（主表与颜色分配共用：000 胚布单不存在 / 001 胚布单号重复 / 002 关联引用不存在）、`1-030-007-xxx` 染色单（主表与颜色明细共用：000 染色单不存在 / 001 状态不允许该操作 / 002 关联引用不存在）、`1-030-008-xxx` 纱线档案（材质+规格+配比BOM+库存台账共用：000 材质不存在 / 001 材质名重复 / 002 规格不存在 / 003 规格名重复 / 004 配比BOM不存在 / 005 配比BOM明细不存在 / 006 库存台账不存在 / 007 该产品已存在纱线配比BOM）、`1-030-009-xxx` 纱线采购单（主表+明细共用：000 采购单不存在 / 001 采购单号重复 / 002 状态不允许该操作 / 003 关联引用不存在）。

客户最初设计误记为 002 段，落地时发现与胚布品种整段重号，因胚布品种已随主线先行上线，客户（后开发模块）改用 004 段。胚布单设计时误记 005 段空闲，落地前核实发现供应商已占用 005 段，胚布单顺延为 006 段。

`1-030-010-xxx` 检验单（主表与明细共用，本期新增）：000 检验单不存在 / 001 检验单号重复 / 002 状态不允许该操作 / 003 关联引用不存在 / 004 单内卷号缸号重复 / 100 录检明细不存在（主表段 000-004，明细段从 100 起避开主表）。

`1-030-011-xxx` 入库单（主表/明细/放置共用，库存域）：000 入库单不存在 / 001 入库单号重复 / 002 状态不允许该操作 / 003 关联引用不存在（仓库/供应商/合同/胚布单/染色单/胚布品种/产品/库位）/ 004 库位不属于入库仓库 / 005 审核失败单无明细。`1-030-012-xxx` 库存台账/余额（本期只读查询，预留段位，暂不定义具体错误码）。`1-030-013-xxx` 仓库与库位：000 仓库不存在 / 001 仓库编号重复 / 002 仓库下有库位不能删除 / 003 仓库被入库单引用不能删除 / 100 库位不存在 / 101 同仓库内库位编码重复 / 102 库位被放置记录引用不能删除（主表段 000-003，库位段从 100 起）。
