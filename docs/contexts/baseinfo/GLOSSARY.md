# 基本信息域术语表（baseinfo）

本文件是 Luna ERP **基本信息域（baseinfo）**的权威术语表，收录客户、供应商、产品三类核心主数据的领域概念。
仓库领域文档总入口见根目录 `GLOSSARY-MAP.md`；跨域平台约定（租户、软删除、前后端模块约定、错误码段位总表）见 `docs/contexts/shared/GLOSSARY.md`。
当对话或代码中含糊使用某个词时，以本文件为准。

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

## 基本信息：产品信息

**产品 (Product)**：
面料/胚布的产品档案，是销售、生产、库存各域引用的核心主数据。表 `lunaerp_baseinfo_product`。前端编辑表单按 5 个分区排列：图片、基础信息、技术规格、纱线规格、成本结构明细（ADR-0035）。

- **基础信息**：产品编号（code，租户内唯一）、品名（name）、类别（type，字典 `PRODUCT_CATEGORY` 下拉，存 dict value + label 冗余）、供应商样号（supplierCode）、成分（composition）、备注（remark）、产品标签、对外展示/首页推荐。
- **技术规格**：门幅（widthCm，CM）、克重（weightGsm，g/m²）、颜色（color）、工艺（craftType）、机型（machineType）、布种（fabric_type，字典 `FABRIC_TYPE` 下拉，存 dict value + label 冗余）、胚布品种（greigeTypeId，关联胚布品种表）、标准米长（standardLength）、梭织参数（5 个独立列：woven_total_ends 总经根数 / woven_reed_no 筘号 / woven_warp_density 经密 / woven_weft_density 纬密 / woven_loom_weft_density 上机纬密）。
- **纱线规格**（yarnSpecs，JSON 字符串数组）：织物结构双 Tab（fabric_construction: KNIT 针织 / WOVEN 梭织）；梭织下分简易估算（woven_calculation_mode: SIMPLE）与经纬精算（PROFESSIONAL）。每行含纱线规格 ID（yarnSpecId，两级下拉从纱线档案选择，存 id + name 冗余）、单价、比例、损耗率等字段，列随模式动态变化。
- **成本结构明细**（costItems，JSON 字符串数组）：成本项表格（供应商 id+name 冗余 / 工序 / 单价 / 损耗率）+ 加工费设置（7 个独立列：knit_processing_fee_per_kg 针织加工费 / weaving_fee_per_meter 织造费 / other_processing_fee_per_meter 其他加工费 / greige_weight_gsm 坯布克重 / greige_width_cm 坯布门幅 / fabric_construction / woven_calculation_mode）+ 包装费（packaging_fee）+ 税率（tax_rate）+ 成本汇总展示。计算逻辑从旧系统 `product-yarn-cost.ts` 迁移（ADR-0035）。
_避免_：产量（output）、花型号（patternNo）、临时编号（tempCode）、算长系数（conversionRate）——已删除（ADR-0035）。旧系统将这些与梭织参数存于 extensionData JSON，新系统改用独立列。

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

（`lunaerp-elmui` 临时 UI 区的定义与前后端包结构总约定见 `docs/contexts/shared/GLOSSARY.md`，详见 ADR-0001。）

产品信息落 `baseinfo` 子包（与客户/供应商同层）：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.baseinfo.product`；前端视图 `luna-ui/src/views/lunaerp/baseinfo/product/`、前端 API `luna-ui/src/api/lunaerp/baseinfo/product/`。URL 前缀 `/lunaerp/baseinfo/product`，权限码前缀 `lunaerp:baseinfo:product:`，错误码段 `1-030-001-xxx`。建表 SQL：`luna-srv/sql/postgresql/lunaerp-baseinfo-product.sql`。纱线规格两级下拉数据源来自生产管理域的纱线档案（`lunaerp_production_yarn_type` + `lunaerp_production_yarn_spec`，ADR-0011），跨域只读引用。详见 ADR-0035。

供应商与客户同落 `baseinfo` 子包：后端 `com.luna.lunasrv.module.lunaerp.{controller,service,dal.*}.baseinfo.supplier[.suppliercontact]`；前端视图 `luna-ui/src/views/lunaerp-elmui/baseinfo/supplier/`、前端 API `luna-ui/src/api/lunaerp-elmui/baseinfo/supplier/`（含 supplier-contact）。URL 前缀 `/lunaerp/baseinfo/supplier[-contact]`，权限码前缀 `lunaerp:baseinfo:supplier[-contact]:`。

## 字典

**客户字典 (Customer dictionaries)**：
客户的 `clientType` / `industry` / `scale` / `source` / `cooperationStatus` 五个枚举性质字段统一接入 yudao 框架的 `system_dict_*` 机制，字典 type 命名前缀 `lunaerp_customer_`：`lunaerp_customer_client_type` / `lunaerp_customer_industry` / `lunaerp_customer_scale` / `lunaerp_customer_source` / `lunaerp_customer_cooperation_status`。前端用 `<DictTag>` 渲染。

**产品字典 (Product dictionaries)**：
产品信息域建两个字典：`lunaerp_product_category`（产品类别，如成品/胚布/半成品，存入 product.type 列 + product.type_label 冗余列）、`lunaerp_fabric_type`（布种，如针织/梭织/经编/纬编，存入 product.fabric_type 列 + product.fabric_type_label 冗余列）。两个字典值均由后台管理界面配置，前端用 DictSelect 下拉。详见 ADR-0035。

**供应商字典 (Supplier dictionaries)**：
供应商枚举字段走 `system_dict_*`，前缀 `lunaerp_supplier_`：MVP 仅 `lunaerp_supplier_type`（18 项经营类别，见 `供应商类型`）。状态 ACTIVE/INACTIVE 不建字典，与客户一致。

## 错误码

错误码段位总表与新模块取段规则见 `docs/contexts/shared/GLOSSARY.md`。本域占用段位：

- `1-030-001-xxx` 产品
- `1-030-002-xxx` 胚布品种（`GreigeErrorCodeConstants`，随产品模块迁移先行占用）
- `1-030-004-xxx` 客户（customer 与 customer-contact 共用）
- `1-030-005-xxx` 供应商（supplier 与 supplier-contact 共用：000 供应商不存在 / 001 供应商编号重复 / 002 供应商联系人不存在）

客户最初设计误记为 002 段，落地时发现与胚布品种整段重号，因胚布品种已随主线先行上线，客户（后开发模块）改用 004 段（005 段供应商与 006 段胚布单的避让历史见 shared 错误码段位说明）。
