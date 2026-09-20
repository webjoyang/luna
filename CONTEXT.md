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
四态：`DRAFT` 草稿（缺省）/ `ACTIVE` 进行中 / `COMPLETED` 已完成 / `CANCELLED` 已取消，仿染色单 ADR-0008。完成（ACTIVE→COMPLETED）时后端自动写一条 STOCK_IN 台账记录采购入库，是采购单与库存台账的联动接缝（ADR-0012）。MVP 无审批流。

**纱线库存台账 (StockLedger)**：
所有纱线库存变动的单式流水台账，`lunaerp_production_stock_ledger` 单表含 yarn_spec_id + type（枚举 STOCK_IN/STOCK_OUT/PROCESS_IN/PROCESS_OUT）+ quantity（正负值，STOCK_IN 为正）+ related_doc_type/related_doc_id（关联单据类型与 ID，如 yarn_order）+ batch_no + remark。**MVP 实际只用 STOCK_IN**（采购单完成自动写入），其余类型为未来扩展占位。**去掉 source_location/target_location 两列**（MVP 无库位概念，ADR-0012）。余额查询 = 按 yarn_spec_id 聚合 SUM(quantity)。
_避免_：纱线库存表（旧系统 StockLedger 单一类型枚举）；仓库/库位概念。

**测算口径 (Yarn demand calculation)**：
按"勾选胚布指示单 → 查 yarn_recipe_bom 拿配比 → 按纱线规格合并多单需求 → 查 StockLedger 拿在库量 → 算实采量"的核心业务流测算。算法：
- `baseQty = Σ(胚布单成品量 × ratio/100)`，按 yarn_spec_id 合并多张胚布单
- `requiredQty = baseQty × (1 + lossRate/100)`（lossRate 为百分比数字 5 = 5%）
- `purchaseQty = max(0, requiredQty - stockQty)`
- `totalAmount = unitPrice × purchaseQty`（金额服务端权威重算）

**与胚布单 loss_rate 小数+除法公式刻意不一致**：胚布单用 `demand_qty = finished_qty ÷ (1 - loss_rate)`（loss_rate 为小数 0.05 = 5%）。这是两个单据业务语义不同（胚布单是投胚需求折算、纱线采购单是含损耗的采购量），刻意不一致（ADR-0010）。

测算接口为后端 `POST /admin-api/lunaerp/production/yarn-order/calculate`，前端只展示结果不做计算（ADR-0010）。

**入库来源 (StockInSource)**：
采购单 ACTIVE→COMPLETED 时后端自动写一条 STOCK_IN 台账，related_doc_type='yarn_order'、related_doc_id=采购单 ID、quantity=该采购单全部明细 purchase_qty 之和（按 yarn_spec_id 拆分多行）。**不做独立"纱线入库单"流程**（用户决策，ADR-0012）。

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

纱线采购域同样落在 `production` 子包下，新增 5 个并列子包：`production.yarntype`（材质 CRUD，无字典）、`production.yarnspec`（规格 CRUD + cascade-options 两级级联数据源）、`production.yarnrecipebom`（配比 BOM 主从表 CRUD + get-by-product-id）、`production.stockledger`（库存台账仅余额查询，无 CRUD 端点）、`production.yarnorder`（采购单主从表 CRUD + 状态操作 + calculate 测算接口 + 完成时自动写 STOCK_IN）。前端视图 `lunaerp-elmui/production/yarn-master/`（材质+规格合并管理页）、`yarn-recipe-bom/`（配比 BOM 管理页）、`yarn-order/`（采购单列表+编辑页）。URL 前缀 `/admin-api/lunaerp/production/{yarn-type|yarn-spec|yarn-recipe-bom|yarn-order}`、`/admin-api/lunaerp/production/yarn-stock/balance`，权限码前缀 `lunaerp:production:<子域>:`。详见 ADR-0011。

供应商与客户同落 `baseinfo` 子包：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.baseinfo.supplier[.suppliercontact]`；前端视图 `luna-ui/src/views/lunaerp-elmui/baseinfo/supplier/`、前端 API `luna-ui/src/api/lunaerp-elmui/baseinfo/supplier/`（含 supplier-contact）。URL 前缀 `/admin-api/lunaerp/baseinfo/supplier[-contact]`，权限码前缀 `lunaerp:baseinfo:supplier[-contact]:`。

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

## 错误码

**错误码段位全表**：`1-030-001-xxx` 产品、`1-030-002-xxx` 胚布品种（`GreigeErrorCodeConstants`，随产品模块迁移先行占用）、`1-030-003-xxx` 销售合同（主表与明细共用）、`1-030-004-xxx` 客户（customer 与 customer-contact 共用）、`1-030-005-xxx` 供应商（supplier 与 supplier-contact 共用：000 供应商不存在 / 001 供应商编号重复 / 002 供应商联系人不存在）、`1-030-006-xxx` 胚布单（主表与颜色分配共用：000 胚布单不存在 / 001 胚布单号重复 / 002 关联引用不存在）、`1-030-007-xxx` 染色单（主表与颜色明细共用：000 染色单不存在 / 001 状态不允许该操作 / 002 关联引用不存在）、`1-030-008-xxx` 纱线档案（材质+规格+配比BOM+库存台账共用：000 材质不存在 / 001 材质名重复 / 002 规格不存在 / 003 规格名重复 / 004 配比BOM不存在 / 005 配比BOM明细不存在 / 006 库存台账不存在 / 007 该产品已存在纱线配比BOM）、`1-030-009-xxx` 纱线采购单（主表+明细共用：000 采购单不存在 / 001 采购单号重复 / 002 状态不允许该操作 / 003 关联引用不存在）。

客户最初设计误记为 002 段，落地时发现与胚布品种整段重号，因胚布品种已随主线先行上线，客户（后开发模块）改用 004 段。胚布单设计时误记 005 段空闲，落地前核实发现供应商已占用 005 段，胚布单顺延为 006 段。
