# 业务管理 - 出口单证（单证套：PI 形式发票 / CI 商业发票 / PL 装箱单）

Status: ready-for-agent（07 号 research 产出草案，20 号 grilling 两轮 11 问全部定稿，可按 session 施工）

## Problem Statement

外销业务需要向客户/银行/报关行提供正式出口单证：PI（Proforma Invoice 形式发票，报价订金阶段）、CI（Commercial Invoice 商业发票，出货结算与报关收汇核心）、PL（Packing List 装箱单，出口装箱明细）。三单数据 80% 重合（买卖双方、合同号、贸易术语、货物行），实务硬要求"一套单据必须互相吻合"。

老系统（linaagi）已有完整实现：`ExportDocSet`（出口单证套）+ `ExportDocLine` 数据模型，从外销合同/发货单一键带出 + 人工补录 + 重新同步（只填空字段），HTML 模板渲染 + PDF/Excel 导出。关键设计取舍：**不做平行"外贸模块"，单证套共享抬头 + 共享明细作为单一真相来源，每份单据只叠加自己的专属 meta**。

新栈已有销售合同（`business.salescontract`，含 trade_type=EXPORT 标记、多币种、合同明细）与发货单（`business.shipment`，含卷清单逐卷实称重量）。研究定论：新栈数据**不足以一键带出完整单证但不致命**——老系统核心工作模式本就是"部分带出 + 人工补录"，新栈沿用该模式，并通过本票定稿的两处已落地模块增量改动（合同补 4 个外贸条款列、客户档案补 2 个英文列）提高一键带出率。

## Grilling 定稿决策（20 号 ticket，Q1–Q11）

1. **G1 合同字段分类回补（Q1）**：7 个缺口字段按语义分三处落——① **销售合同 +4 可空列**（incoterm/port_of_loading/port_of_discharge/payment_term，条款是合同要约的一部分，表单仅 trade_type=EXPORT 显示"外贸条款"区块）；② **客户档案 +2 可空列**（name_en/address_en，买方英文名址是客户稳定属性不是合同条款，经 customer_id 带出）；③ **买方银行信息不回补**，随单变化（L/C 开证行行行不同），保留为单证套共享抬头手填字段。
2. **G2 卖方出口配置新建租户级单行表（Q2）**：草案的 `infra_config` 方案作废——`ConfigDO` 标注 `@TenantIgnore`，infra_config 全局不做租户隔离，多租户共用一个卖方身份不可接受。新建 `lunaerp_business_export_seller_config`（tenant_id 唯一、一租户一行、upsert 无删除）。维护入口不进系统管理：出口单证列表页"卖方出口资料"按钮 + 弹窗，独立权限位。
3. **错误码段位 1-030-033（Q3）**：grilling 当口推荐 032，定稿落稿时发现并行会话（06 打色指示单）已先行登记 032，按段位总表避让规则改取 **033**；段内分区 000 单证套 / 100 明细 / 200 卖方配置。
4. **打印全面接入 04 号统一打印内核（Q4）**：三模板注册 `EXPORT_DOC_PI/CI/PL`，经 PrintDispatcher/QrSlot/PrintPaper/usePrintHtml 渲染；独立 print-data 接口 + 独立打印 VO（豁免脱敏）+ `@LogRecord` 留痕；草案中的 inspection-confirmation 老 PrintDialog 先例废弃。760px A4 版式、三单内容结构、签章区、PL 手填汇总优先等设计保留。
5. **二维码印完整门户 URL（Q5）**：内容 `{luna.portal.base-url}/portal/export-doc?no={doc_no}`，复用 04 冻结的 `luna.portal.base-url` infra_config 键；一期门户无单证查看页（Q13=A 最小集不含），链接可不通，基址未配置时**不阻断打印**、二维码位给提示（与发货单同规则），格式不欠债。
6. **补 resync 端点（Q6）**：`PUT /resync-from-source?id=`，仅 DRAFT 且来源为合同/发货单可用；只填当前为空的字段、永不覆盖已编辑值；优先级 发货单 > 合同 > 卖方配置；明细行按来源行匹配补空。工作台放"重新同步"按钮。
7. **定稿守卫与草稿打印（Q7）**：finalize 要求至少 1 条明细（对齐 SHIPMENT_AUDIT_NO_ITEMS/STOCK_IN_ITEM_EMPTY 惯例）；DRAFT 与 FINALIZED 均可打印（PI 草稿常需先发客户确认定金），FINALIZED 只锁编辑/删除不锁打印。
8. **回收站（Q8）**：声明 RecycleBinHandler，bizType=`EXPORT_DOC`，仅收 DRAFT 单证套（级联恢复 items），FINALIZED 须先 revoke；恢复不补账（本模块无库存/账务副作用）。
9. **跨模块改动归属（Q9）**：不回改历史 spec、不新建 ticket，本 spec 单文档承载"前置改动（已落地模块）"一章，作为本模块第 0 个前置 session。
10. **客户 Excel 导入同步加列（Q10）**：英文名/英文地址两可空列同步加入 04 号 common-foundation 客户导入模板（W0 尚未施工，现在改零成本）。
11. **单证套号 4 位流水（Q11）**：`ED{yyyy}{4位流水}`（如 ED20260001），租户内全量唯一含软删行，取号照 ADR-0004 数字 max+1 扫描 + 撞号重试，规避老系统字符串排序 101 套起建号失败的坑。

## Solution

在 luna-srv 的 `lunasrv-module-lunaerp` 的 `business` 子包下新增 `exportdoc`，落地 **3 张新表**：

1. `lunaerp_business_export_doc` 出口单证套主表（ED 单号、两态状态机、来源合同/发货单快照、买卖双方抬头、贸易条款、三个单据专属 meta JSON）；
2. `lunaerp_business_export_doc_item` 单证明细行（共享货物明细，随套聚合保存，ADR-0003 模式）；
3. `lunaerp_business_export_seller_config` 租户卖方出口配置（一租户一行，英文抬头/地址/收款银行信息）。

另含**两处已落地模块的可空加列**（见"前置改动"章）：销售合同 +4 外贸条款列、客户档案 +2 英文列。

配套：单证套从合同（按 trade_type=EXPORT 过滤）或发货单一键带出草稿、抬头/明细聚合编辑、每单据专属 meta 编辑、重新同步（只填空）、**前端固定套打三模板接入 W0 统一打印内核 + 完整 URL 二维码**（Q12=A）、卖方配置弹窗、字典/权限/菜单注册、回收站 handler。

一期只做 PI/CI/PL 三单；报关单/CO 不做（Q12=A 不对接报关），数据模型不做专门预留（老系统证明 meta JSON 模式下后补成本极低，一期不落 customsMeta/coMeta 字段）。

## 前置改动（已落地模块，第 0 个前置 session）

不回改 sales-contract-mvp / customer-info-module 的历史 spec，改动在本 spec 承载；均为**可空加列**，无数据迁移、对存量功能零影响。

### 销售合同（`lunaerp_business_sales_contract` +4 列）

| 列 | 类型 | 说明 |
|---|---|---|
| incoterm | varchar(16) | 字典 lunaerp_incoterm（FOB/CIF/CFR/EXW/…），可空 |
| port_of_loading | varchar(64) | 起运港，可空 |
| port_of_discharge | varchar(64) | 目的港，可空 |
| payment_term | varchar(16) | 字典 lunaerp_payment_term（TT/LC/DP/DA/OA），可空 |

- DO `SalesContractDO` 同步加 4 字段；create/update VO + 表单 + 详情只读展示。
- 前端合同表单新增"外贸条款"区块，**仅 trade_type=EXPORT 时显示与校验**（非必填，仅控制显隐）；内销合同完全无感。
- 字典复用本模块新建的 lunaerp_incoterm / lunaerp_payment_term（合同与单证共用一套，不重复建字典）。
- 不做历史合同数据回填。

### 客户档案（`lunaerp_baseinfo_customer` +2 列）

| 列 | 类型 | 说明 |
|---|---|---|
| name_en | varchar(128) | 客户英文名称，可空 |
| address_en | varchar(512) | 客户英文地址，可空 |

- 客户表单/详情加两个可空输入；客户 Excel 导入模板同步加"英文名称/英文地址"两可空列（同步小改 04 号 common-foundation spec 客户模板列，W0 施工时生效）。
- 出口单证从合同/发货单生成时，经 customer_id 读客户档案英文名址带出；为空则单证套上手填。

## 老系统实现调研（证据与结论）

证据源：`linaagi/docs/architecture/modules/export-documents.md`、`linaagi/docs/superpowers/specs/2026-06-18-foreign-trade-export-documents-design.md`、`linaagi/server/src/lib/export-doc-mapping.ts`、`linaagi/web/app/staff/components/business/export-docs/`（ExportDocTypes.ts / templates/PiTemplate.tsx / CiTemplate.tsx / PlTemplate.tsx / GoodsTable.tsx）。

### 老系统数据模型

- `ExportDocSet`：setNo（`ED+年份+2位序号`，全局序列）；来源 contractId/shipmentId（可空 = 手工新建）；共享抬头 sellerName/sellerAddress/sellerBankInfo（← `Tenant.brandConfig.exportInfo`）+ buyerName/buyerAddress/buyerBankInfo（← 合同 buyerNameEn/buyerAddressEn/buyerBankInfo）；contractNo/incoterm/portOfLoading/portOfDischarge/countryOfDest/transportMode/paymentTerm/currency（默认 USD）/shippingMark（唛头）/remarks；status（DRAFT/FINALIZED）；5 个 meta JSON（piMeta/ciMeta/plMeta/customsMeta/coMeta，前端 TS 约束、后端透传）。
- `ExportDocLine`：seq/nameCn/nameEn/hsCode/spec/quantity/unit/unitPrice/amount/netWeight/grossWeight/packages/volume/origin。
- 重新同步（resync）：**只填空字段、永不覆盖已编辑值**；优先级：发货单（客户名/合同号以实发为准）> 合同（贸易条款/买方信息）> 企业配置（卖方信息）。
- 单证号教训（老系统踩坑）：`ED` 序号曾因字符串排序导致第 101 套起永久建号失败；新栈取号沿用 ADR-0004"数字后缀扫描 max+1 + 撞号重试"成熟方案，4 位流水。

### 老系统字段映射（带出规则）

**从合同带出**（`mapContractToDocSet`）：买方英文名址/buyerBankInfo/incoterm/起运港/目的港/paymentTerm/currency；行：name→nameCn、composition/width→spec、quantity/unit/unitPrice/amount。**netWeight/grossWeight/packages/volume/origin 一律留空**（合同 weight 是克重 GSM，不是净重）。

**从发货单带出**（`mapShipmentToDocSet`）：customerName→buyerName（实发优先于合同抬头）、contractNumber→contractNo；行：productName→nameCn、color→spec、quantity/unit/unitPrice/amount、actualWeight→netWeight；grossWeight/packages/volume 留空人工补。

新栈差异：买方英文名址经客户档案 name_en/address_en 带出（非合同列）；incoterm/两港/paymentTerm 从合同新增 4 列带出；buyerBankInfo 仍手填。

### 老系统模板内容结构（760px A4、Arial 12px，三单同构）

- **PI**：居中大标题 `PROFORMA INVOICE` → 左区 `To:` 买方名/地址，右区 `PI No.`（缺省 `PI-{setNo}`）/ `Valid Until` / `Contract` → 行区 `Incoterm` / `Port of Loading` / `Destination` / `Shipment date` → 货物表（`#` / `Description` / `HS Code` / `Qty` / `Unit` / `Unit Price` / `Amount` + `TOTAL` 行，金额带币种符号）→ `Payment:`（含 `% deposit` 定金）→ `Beneficiary bank:`（银行名 / `A/C:` / `SWIFT:` / 银行地址）→ `Partial shipment:` / `Transshipment:`（Allowed / Not allowed）→ `Commission:` → `Remarks:`。
- **CI**：标题 `COMMERCIAL INVOICE` → 右区 `Invoice No.` / `Date` / `L/C No.` / `Contract` → 行区 Incoterm/港口 → 同款货物表 → `Payment:` → 原产地声明（缺省 `COUNTRY OF ORIGIN: CHINA`）→ 固定认证句 `WE HEREBY CERTIFY THIS INVOICE IS TRUE AND CORRECT.`。
- **PL**：标题 `PACKING LIST` → 右区 `PL No.`（缺省 `PL-{setNo}`）/ `Date` / `Contract` → 行区 `Shipping mark`（唛头）/ 港口 → 货物表（`#` / `Description` / `Packages` / `Qty` / `Unit` / `N.W.(KG)` / `G.W.(KG)` / `CBM` + `TOTAL` 行）→ `Remark:`。TOTAL 汇总**优先取手填汇总、空则按行求和**（实务尾箱凑整需要）。
- 三单 Description 优先英文名、回退中文名（`nameEn || nameCn`）。

### 外贸单证惯例补充（老系统证据 + 领域常识）

- PI 用于报价/订金确认（有效期、预计装运期、付款与收款银行、分批/转运条款、佣金/定金比例）；CI 用于出货结算与报关收汇（发票号日期、L/C 引用、原产地与真实性声明）；PL 用于清点（箱件数、毛净重、体积、唛头）。
- 全英文栏位、标题全大写；正文表格金额须带币种符号；三单同一套单证号/日期/数量必须能互相对上。
- 签章位：老系统模板无显式签章区（实务打印后手签盖章）。新栈模板**底部增加签章区**：`For and on behalf of` + 卖方英文名 + 签字/公司章留白 + 日期，成本为零且更符合实务。

## 三单证字段映射清单（单证字段 ← 新栈数据来源）

图例：✅ 可直接带出｜🟡 带出后需人工确认/补英文｜✍️ 一期手工录入（新栈无源数据，见缺口清单）

### 共享抬头（三单共用）

| 单证字段 | 来源 | 说明 |
|---|---|---|
| Seller（卖方英文名） | 🟡 租户卖方配置表缺省带出，可改 | 一租户一行，列表页弹窗维护 |
| Seller address / Bank info（银行名/A/C/SWIFT/银行地址） | 🟡 同上 | |
| To: Buyer name（买方英文名） | 🟡 客户档案 `name_en` 带出；为空回退合同 `client_name` 中文快照 | 可改快照 |
| Buyer address | 🟡 客户档案 `address_en` 带出；为空回退合同 `client_address` | 可改快照 |
| Buyer bank info | ✍️ 手填 | 随单变化，不进任何主数据 |
| Contract No. | ✅ 合同 `contract_no` 快照 / 发货单 `contract_no` 快照 | |
| Incoterm（FOB/CIF/CFR/EXW…） | 🟡 合同新增列带出（字典 lunaerp_incoterm） | 手工建套/老合同为空时手填 |
| Port of Loading / Port of Discharge | 🟡 合同新增列带出 | 同上 |
| Country of destination / Transport mode | ✍️ 手填 | |
| Payment term（T/T、L/C、D/P…） | 🟡 合同新增列带出（字典 lunaerp_payment_term） | 同上 |
| Currency | ✅ 合同/发货单 `currency`（RMB/USD/EUR/HKD 字典） | |
| Shipping mark（唛头） | ✍️ 手填 | |
| Remarks | ✍️ 手填 | |

### PI 专属（pi_meta JSON，前端 TS 约束、后端透传）

| 字段 | 来源 |
|---|---|
| piNo（缺省取单证套号） | ✍️ meta，空则模板显示 `{doc_no}` |
| validUntil 有效期 / shipmentDate 预计装运期 | ✍️ 手填 |
| depositPct 定金 % / commissionPct 佣金 % | ✍️ 手填 |
| partialShipment / transshipment（ALLOWED/NOT_ALLOWED） | ✍️ 手填（前端 TS 联合类型下拉，不建字典） |
| bankName/bankAccount/bankSwift/bankAddress 收款银行 | 🟡 缺省取卖方配置，meta 可覆盖 |
| notes 备注 | ✍️ 手填 |

### CI 专属（ci_meta JSON）

| 字段 | 来源 |
|---|---|
| ciNo（缺省取单证套号）/ invoiceDate | ✍️ 手填 |
| lcNo 信用证号 | ✍️ 手填 |
| contractRef 合同引用 | ✅ 缺省取共享抬头合同号 |
| originDeclaration 原产地声明 | 缺省 `COUNTRY OF ORIGIN: CHINA`，可改 |

### PL 专属（pl_meta JSON）

| 字段 | 来源 |
|---|---|
| plNo（缺省取单证套号）/ plDate | ✍️ 手填 |
| totalPackages / totalNetWeight / totalGrossWeight 汇总 | ✅ 服务端按行求和；meta 手填值优先（尾箱凑整惯例） |
| remark | ✍️ 手填 |

### 共享货物明细行（三单共用，ExportDocItem）

| 行字段 | 来源 |
|---|---|
| nameCn 中文品名 | ✅ 合同明细 `product_name` / 发货明细 `product_name` 快照 |
| nameEn 英文品名 | ✍️ 手填（模板 `nameEn \|\| nameCn` 回退） |
| hsCode HS 编码 | ✍️ 手填 |
| spec 规格 | 🟡 合同明细 `composition`+`width` 拼接 / 发货明细 `color_name` |
| quantity / unit | ✅ 合同明细 / 发货明细 |
| unitPrice / amount | ✅ 合同明细；从发货单带出时取发货明细单价金额；服务端重算 amount=quantity×unit_price |
| netWeight 净重 | ✅ 从发货单带出时 = 该明细行卷清单实称重量之和（`Σ shipment_roll.weight`，比老系统行级 actualWeight 更细）；合同带出留空手填 |
| grossWeight 毛重 / packages 箱数 / volume 体积 CBM | ✍️ 手填（PL 行编辑区必须暴露 packages/netWeight/grossWeight/volume，对齐老系统教训） |

## 新栈数据缺口清单（grilling 定稿后）

| # | 缺口 | 一期处置 | 致命性 |
|---|---|---|---|
| G1 | 合同外贸条款字段 | **本票回补 4 列**（incoterm/两港/paymentTerm）；买方英文名址改归客户档案（G3）；buyerBankInfo 留单证套手填 | 已消解 |
| G2 | 无租户级卖方出口配置 | **本票新建 `lunaerp_business_export_seller_config` 租户单行表**（infra_config 因 @TenantIgnore 不可用） | 已消解（必须随模块落地） |
| G3 | 客户档案无英文名/英文地址 | **本票给客户档案 +name_en/address_en 两可空列 + Excel 导入加列** | 已消解 |
| G4 | 发货明细无毛重/体积/箱数列；卷清单只有实称重量与米数 | 净重从卷清单聚合带出；毛重/箱数/体积单证行手填；发货表加列二期评估 | 否 |
| G5 | 合同行无英文名/HS 编码/原产地字段 | 单证行自带字段手填；产品档案补 HS 编码二期 | 否 |
| G6 | 币种字典仅 RMB/USD/EUR/HKD，无 GBP/JPY 等 | 字典可自由扩充（system_dict_data 加行即可），非阻塞 | 否 |
| G7 | 无唛头字段（合同/发货均无） | 单证套 shipping_mark 字段解决 | 否 |

**总判断：无致命缺口。** G1/G2/G3 由本票定稿决策消解（2 处可空加列 + 1 张租户配置表）；G4–G7 沿用"部分带出 + 人工补录 + resync 只填空"模式消化，只影响一键带出率。

## Implementation Decisions（已定稿）

### 模块与落点

- 后端：`lunasrv-module-lunaerp` 的 `business.exportdoc` 子包（controller/service/dal 分层，与 shipment 同层）；卖方配置同子包下 `sellerconfig` 资源。
- 前端：`luna-ui/src/views/lunaerp-elmui/business/export-doc/`（列表页 + 工作台编辑页 + 卖方配置弹窗 + print 模板组件目录 `components/print/`）；API 镜像 `api/lunaerp-elmui/business/export-doc/`。
- URL 前缀 `/lunaerp/business/export-doc`；权限码前缀 `lunaerp:business:export-doc:`。

### 数据模型

**单证套主表 `lunaerp_business_export_doc`**（继承 TenantBaseDO）：

| 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|
| doc_no | varchar(32) | ✓ | 单证套号 `ED{yyyy}{4位流水}`（按年取号，含软删行，ADR-0004），唯一索引 (tenant_id, doc_no) |
| status | varchar(16) | ✓ | 字典 lunaerp_export_doc_status（2258）：DRAFT 草稿（缺省）/ FINALIZED 已定稿 |
| source_type | varchar(16) | ✓ | CONTRACT / SHIPMENT / MANUAL（缺省 MANUAL），仅记录来源用途，不控制编辑 |
| contract_id / contract_no | int8 / varchar(32) | - | 来源合同（快照） |
| shipment_id / shipment_no | int8 / varchar(32) | - | 来源发货单（快照） |
| customer_id / customer_name | int8 / varchar(128) | ✓ | 买方客户（ID + 快照） |
| seller_name / seller_address / seller_bank_info | varchar(256)/varchar(512)/varchar(512) | - | 卖方抬头，创建时缺省从租户卖方配置带出（仅创建时带出，此后可改不回写） |
| buyer_name / buyer_address / buyer_bank_info | varchar(256)/varchar(512)/varchar(512) | - | 买方英文名/英文地址/银行（快照可改；英文名址缺省取客户档案 name_en/address_en） |
| incoterm | varchar(16) | - | 字典 lunaerp_incoterm（2259） |
| port_of_loading / port_of_discharge | varchar(64) | - | 起运港/目的港（来源合同列带出） |
| country_of_dest | varchar(64) | - | 目的国 |
| transport_mode | varchar(16) | - | 字典 lunaerp_transport_mode（2261，SEA/AIR/LAND/EXPRESS） |
| payment_term | varchar(16) | - | 字典 lunaerp_payment_term（2260，TT/LC/DP/DA/OA） |
| currency | varchar(8) | ✓ | 缺省 USD，复用 lunaerp_sales_contract_currency 字典（G6 可扩） |
| shipping_mark | varchar(500) | - | 唛头 |
| pi_meta / ci_meta / pl_meta | varchar(2000) | - | 各单据专属 JSON，后端透传不解析、前端 TS interface 约束 |
| remarks | varchar(1000) | - | |

**明细行表 `lunaerp_business_export_doc_item`**（继承 TenantBaseDO，聚合保存）：

| 字段 | 类型 | 备注 |
|---|---|---|
| doc_id / line_no | int8 / int4 | 所属套 / 行序 |
| name_cn / name_en | varchar(128) | 中文/英文品名 |
| hs_code | varchar(32) | HS 编码 |
| spec | varchar(256) | 规格（成分/门幅/颜色） |
| quantity / unit | numeric(18,2) / varchar(8) | 数量 / 单位（KG/M 等，自由文本+常用字典） |
| unit_price / amount | numeric(18,2) | 单价 / 行金额（服务端重算 amount=quantity×unit_price） |
| net_weight / gross_weight | numeric(18,2) | 净重/毛重 KG |
| packages | int4 | 箱数/件数 |
| volume | numeric(18,3) | 体积 CBM |
| origin | varchar(32) | 原产地，缺省 CHINA |
| remark | varchar(256) | |

索引：明细 (doc_id)；主表 (status)、(customer_id)、(contract_id)、(shipment_id)。

**卖方出口配置表 `lunaerp_business_export_seller_config`**（继承 TenantBaseDO，一租户一行）：

| 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|
| seller_name | varchar(256) | ✓ | 卖方英文名（签章/抬头） |
| seller_address | varchar(512) | - | 卖方英文地址 |
| bank_name | varchar(256) | - | 收款银行名 |
| bank_account | varchar(128) | - | 银行账号 A/C |
| bank_swift | varchar(32) | - | SWIFT 码 |
| bank_address | varchar(512) | - | 银行地址 |
| remark | varchar(256) | - | |

唯一索引 (tenant_id)；不提供删除端点，PUT 为 upsert（不存在即建、存在即更新）；列表页弹窗维护。

### API 与端点（前缀 `/lunaerp/business/export-doc`）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/page` | query | keyword/status/customerId/contractNo 过滤 |
| GET | `/get?id=` | query | 含 items + 三个 meta 原文 |
| POST | `/create` | create | 聚合保存主表+items |
| PUT | `/update` | update | 仅 DRAFT；聚合整体替换 items、line_no 重排 |
| DELETE | `/delete?id=` | delete | 仅 DRAFT，级联软删 items，进回收站 |
| PUT | `/finalize?id=` | update | DRAFT→FINALIZED，守卫：明细非空（EXPORT_DOC_ITEM_EMPTY） |
| PUT | `/revoke?id=` | update | FINALIZED→DRAFT，条件 UPDATE |
| GET | `/from-contract?contractId=` | create | 按合同预填草稿（含 4 条款列、客户英文名址、行快照；非 EXPORT 合同拒绝 EXPORT_DOC_CONTRACT_NOT_EXPORT） |
| GET | `/from-shipment?shipmentId=` | create | 按发货预填（卷清单净重 Σ 聚合、实发客户名优先），联动合同条款 |
| PUT | `/resync-from-source?id=` | update | 仅 DRAFT 且 source 非 MANUAL；只填空字段/补空行，永不覆盖已编辑值；优先级 发货单 > 合同 > 卖方配置 |
| GET | `/seller-config` | seller-config | 读本租户配置（不存在返回空对象，不报错） |
| PUT | `/seller-config` | seller-config | upsert 本租户配置 |
| GET | `/{id}/print-data?template=PI\|CI\|PL` | print | 独立打印 VO（豁免脱敏，含拼好的 qrUrl + 银行全量字段），`@LogRecord(type="打印", …)` 留痕；DRAFT/FINALIZED 均可调 |

权限码：`query` / `create` / `update` / `delete` / `print` / `seller-config`（finalize/revoke/resync 复用 update）。

错误码（段位 `1-030-033`，登记 shared GLOSSARY 段位总表；段内分区 000 单证套 / 100 明细 / 200 卖方配置）：

- 000 `EXPORT_DOC_NOT_EXISTS` 单证套不存在
- 001 `EXPORT_DOC_NO_DUPLICATE` 单证套号【{}】已存在
- 002 `EXPORT_DOC_STATUS_NOT_ALLOW` 当前状态不允许该操作
- 003 `EXPORT_DOC_REFERENCE_NOT_FOUND` 关联的{}不存在或不可用（客户/合同/发货单）
- 004 `EXPORT_DOC_ITEM_EMPTY` 定稿失败：单证套没有货物明细
- 005 `EXPORT_DOC_CONTRACT_NOT_EXPORT` 该合同不是外销合同（trade_type≠EXPORT），不能生成出口单证
- 006 `EXPORT_DOC_SOURCE_REQUIRED` 手工新建的单证套没有来源单据，不能重新同步
- 100 `EXPORT_DOC_ITEM_NOT_EXISTS` 明细行不存在
- 101 `EXPORT_DOC_ITEM_INVALID_ID` 明细行 ID【{}】不属于本单证套，禁止跨单修改
- 200 段：卖方配置读缺省返回空不报错，仅保留 `EXPORT_DOC_SELLER_CONFIG_SAVE_FAILED`（200，并发 upsert 兜底）一个码位。

### 状态机与守卫（统一 DoD）

- 两态：`DRAFT`（可编辑可删可 resync）/ `FINALIZED`（只读，仅供打印）；`revoke` 回退 DRAFT。无审批流。
- 全部写端点做状态条件守卫（非 DRAFT 的 create 外写操作报 EXPORT_DOC_STATUS_NOT_ALLOW）；finalize/revoke 用条件 UPDATE 防并发。
- finalize 额外守卫明细非空。
- 删除进回收站（软删 + 级联软删 items），与全局回收站链路对齐：bizType=`EXPORT_DOC`，**仅 DRAFT 进站**，FINALIZED 须先 revoke；恢复级联复活 items、不补账（本模块无库存/账务副作用）；实现并注册 `RecycleBinHandler`。

### 打印模板与二维码（接入 04 号统一打印内核，Q12=A）

- 前端三模板注册到 W0 静态模板注册表：`EXPORT_DOC_PI` / `EXPORT_DOC_CI` / `EXPORT_DOC_PL`，组件位于 `views/lunaerp-elmui/business/export-doc/components/print/`（ExportDocPrintPi.vue / Ci.vue / Pl.vue + 共用 ExportDocGoodsTable.vue），经 `<PrintDispatcher biz-type="EXPORT_DOC" :biz-id="id" />` 弹版式选择，PrintPaper/QrSlot 外壳 + usePrintHtml 打印。
- A4 纵向、内容宽 760px、Arial；结构照"老系统模板内容结构"一节，新增两处：
  1. **底部签章区**：`For and on behalf of {seller_name}` + 手签/公司章留白框 + 日期（三单同构）。
  2. **二维码**：经 QrSlot 渲染于右下角，内容由 print-data 接口拼为完整 URL `{luna.portal.base-url}/portal/export-doc?no={doc_no}`；基址取 infra_config 键 `luna.portal.base-url`，未配置时不阻断打印、二维码位渲染"门户地址未配置"提示（与发货单二维码同规则）。
- PL 的 TOTAL 行：显示 pl_meta 手填汇总（若填），否则服务端求和值——"手填优先、空则求和"。
- 金额显示带币种符号（复用合同模块币种映射）；Description 优先英文品名。
- print-data VO 独立于详情 VO：携带卖方银行全量字段，不走 @Sensitive 脱敏（对齐 04 号打印豁免契约）。
- 一期不做服务端 PDF/Excel 导出、不做整套 zip（浏览器打印即得 PDF；导出二期，Q12=A）。

### 前端页面结构与交互

- 列表页：关键词 + 状态 + 客户搜索；列：单证套号/买方/合同号/币种/Incoterm/状态 DictTag/创建时间/操作（查看、编辑[仅草稿]、定稿/回退、删除[仅草稿]、打印）；顶部"新建（空白）/从合同生成/从发货单生成"三个入口（后两者远程选单后调 from-* 接口预填进工作台）；右上"卖方出口资料"按钮（seller-config 权限）打开配置弹窗。
- 工作台编辑页（el-tabs 3 Tab）：**共享抬头**（买卖双方、条款、港口、唛头、备注；卖方抬头区旁"使用租户默认配置"一键带出）→ **货物明细**（可编辑表格：中英品名/HS/规格/数量/单价/金额/净毛重/箱数/体积，服务端重算行金额 + 底部汇总）→ **单据页签**（PI/CI/PL 三子 Tab，各编辑专属 meta + 打印按钮）。
- DRAFT 工作台提供"重新同步"按钮（调 resync-from-source，二次确认提示"仅补充空白内容，不覆盖已填内容"）；MANUAL 来源禁用并提示。
- 打印按钮两态均显示，点击后 PrintDispatcher 选 PI/CI/PL 版式预览。

### 字典与菜单

- 字典（dict_type id 2258–2261，已避让 product-info-align 2249/2250、sample-analysis 2251–2254、color-instruction 2255–2257；脚本按既有"DO 块判占用平移"幂等模式）：

| id | type | 内容 |
|---|---|---|
| 2258 | lunaerp_export_doc_status | DRAFT 草稿 / FINALIZED 已定稿 |
| 2259 | lunaerp_incoterm | FOB / CIF / CFR / EXW（常显文本，后续可加 DDP 等） |
| 2260 | lunaerp_payment_term | TT / LC / DP / DA / OA |
| 2261 | lunaerp_transport_mode | SEA 海运 / AIR 空运 / LAND 陆运 / EXPRESS 快递 |

- 菜单（父菜单 10100 销售，原"业务管理"已由 03 号改名）：

| id | 名称 | type | parent | 权限/组件 |
|---|---|---|---|---|
| 10126 | 出口单证 | 2 | 10100 | `lunaerp:business:export-doc:query`，component `lunaerp-elmui/business/export-doc/index`，sort 3 |
| 10127 | 单证创建 | 3 | 10126 | `:create`（含从合同/发货单生成、resync 不另立） |
| 10128 | 单证修改 | 3 | 10126 | `:update`（含定稿/回退） |
| 10129 | 单证删除 | 3 | 10126 | `:delete` |
| 10130 | 单证打印 | 3 | 10126 | `:print` |
| 10131 | 卖方出口资料 | 3 | 10126 | `:seller-config` |

工作台走前端隐藏路由（仿发货工作台）。

### SQL 脚本

新建 `luna-srv/sql/postgresql/lunaerp-business-export-doc.sql`，参照 shipment/contract 脚本体例（CREATE TABLE IF NOT EXISTS + 主键 DO 块判存 + 列注释 + 索引，单号唯一索引含软删行）：

1. 前置加列：`lunaerp_business_sales_contract` ADD COLUMN IF NOT EXISTS 4 列；`lunaerp_baseinfo_customer` ADD COLUMN IF NOT EXISTS 2 列；
2. 3 张新表 DDL + 序列 + 索引（卖方配置 (tenant_id) 唯一索引）；
3. system_menu 10126–10131（先 DELETE 再 INSERT）；
4. system_dict_type 2258–2261 + dict_data（id 从 3750 起——已避让 sample-analysis 3709 起、product-info-align 3702–3708、color-instruction 3736–3749——按占用情况平移）。

可重复执行，仅语法自查、禁止直连共享库。

## Testing Decisions

沿用纯 Mockito Service 单测接缝（Prior art：ShipmentServiceImplTest / SalesContractServiceImplTest），前端不做单测（ADR-0001）：

1. 取号：ED{年} 4 位数字后缀 max+1、含软删行、撞号重试、手填撞号报错。
2. 状态守卫：非 DRAFT 写操作被拒；DRAFT/FINALIZED 打印与查看均合法；finalize/revoke 条件更新。
3. 定稿守卫：明细为空拒绝（EXPORT_DOC_ITEM_EMPTY）。
4. 聚合保存：明细整体替换、line_no 重排、amount=quantity×unit_price 服务端重算（金额逻辑必有测试）、跨单明细 ID 拒绝。
5. 从合同带出：4 条款列 + 客户 name_en/address_en + 行快照映射断言；净重留空；非 EXPORT 合同拒绝（EXPORT_DOC_CONTRACT_NOT_EXPORT）。
6. 从发货单带出：卷清单净重按明细行 SUM 聚合、毛重/箱数/体积留空、实发客户名优先；合同条款经发货单的合同联动带出。
7. resync：只填空字段不覆盖已编辑值（抬头/meta/行三层各有用例）、MANUAL 来源拒绝、非 DRAFT 拒绝。
8. 卖方配置：upsert 幂等（首次/再次）、创建单证时配置带出、单证此后编辑不回写、配置缺失降级为空不报错、**租户隔离专测（A 租户读不到 B 租户配置）**。
9. 租户隔离：所有查询强制 tenant_id；横向/纵向越权用例（统一 DoD 硬指标），含 print-data 越权。
10. PL 汇总：meta 手填优先、空则服务端求和。
11. 回收站：DRAFT 单证套软删进回收站、级联 items 一并恢复、FINALIZED 不可删。
12. 二维码：print-data 拼 URL 断言；基址未配置时不报错且返回未配置标记。

## Out of Scope（一期不做）

- 报关单（Customs Declaration）、产地证（CO）——Q12=A 不对接报关；meta JSON 模式下二期后补成本极低，一期不落 customsMeta/coMeta 字段、不做预留列。
- 海关单一窗口/电子口岸报文对接。
- 服务端 PDF/Excel 导出、整套 zip、邮件发送单证；打印模板设计器（Q12=A）。
- 门户出口单证查看页（二维码一期链可不通；14 号门户二期扩展时接 `/portal/export-doc`）。
- 汇率折算与结汇（财务域二期）。
- 发货单加毛重/箱数/体积列（G4）、产品档案 HS 编码/英文品名（G5）——二期评估。
- 买方银行信息进客户/合同主数据（随单变化，刻意不沉淀）。
- AI 补全英文名/HS 编码（Q5=A 一期不做生成式 AI）。
- 单证审批流、版本快照、乐观锁。

## 验收标准（统一 DoD 映射）

- [ ] 前置改动：合同 +4 列（仅 EXPORT 显示）、客户 +2 列 + Excel 导入两列（同步 04 号模板）
- [ ] 3 张新表 + SQL 幂等脚本（加列/DDL/10126–10131 菜单/2258–2261 字典）
- [ ] 状态机白名单 + 终态守卫 + 条件更新（DRAFT/FINALIZED 两态 + 定稿明细非空测试）
- [ ] 芋道权限码 6 个 + 租户隔离与横向/纵向越权测试（含卖方配置与 print-data）
- [ ] 列表/详情/删除进回收站：RecycleBinHandler(EXPORT_DOC) 注册，DRAFT 级联恢复、不补账
- [ ] 打印：EXPORT_DOC_PI/CI/PL 三模板注册进统一内核、print-data 豁免脱敏 + @LogRecord、完整 URL 二维码（未配置降级提示）、签章区、PL 汇总手填优先；DRAFT 可打印
- [ ] resync：抬头/行/meta 只填空不覆盖的全链路冒烟
- [ ] 金额逻辑测试（行金额重算、从发货单带出卷净重 Σ 聚合）
- [ ] 术语归 `docs/contexts/business/GLOSSARY.md`（出口单证套/单证套号/定稿/卖方出口配置/唛头/resync）、错误码 1-030-033 入 shared GLOSSARY 段位总表
- [ ] 前端验收清单：维护卖方资料 → 从外销合同/发货单生成 → 补录 → 重新同步 → 定稿/回退 → 打印三单全流程冒烟
- [ ] 常规列表 P95<1s（万级数据）

## Further Notes

### 参考（老系统证据文件）

- `linaagi/docs/architecture/modules/export-documents.md` — 模块现状、数据流、已知坑（resync 只填空、PL 行编辑必须暴露四字段、手填汇总优先、字符串排序取号坑）
- `linaagi/docs/superpowers/specs/2026-06-18-foreign-trade-export-documents-design.md` — 单证套设计定稿（含"共享抬头是单一真相来源"取舍论证）
- `linaagi/server/src/lib/export-doc-mapping.ts` — 带出映射纯逻辑（合同 weight=GSM 不得当净重的注释）
- `linaagi/web/app/staff/components/business/export-docs/ExportDocTypes.ts`、`templates/{Pi,Ci,Pl}Template.tsx`、`templates/GoodsTable.tsx` — meta TS 结构与模板 DOM 结构

### 新栈先例与契约

- 统一打印内核、print-data + @LogRecord、二维码完整 URL 契约、RecycleBinHandler SPI：`.scratch/common-foundation/spec.md`（W0 先行，本模块只注册接入）
- 聚合保存：ADR-0003；取号（含软删行不释放）：ADR-0004；发货三态过账：ADR-0029（本模块无库存副作用，仅借状态守卫模式）；回收站边界：ADR-0038
- 二维码组件：`luna-ui/src/components/Qrcode/src/Qrcode.vue`（qrcode@^1.5.4，由 W0 QrSlot 包装）
