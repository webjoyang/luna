# 业务管理 - 发货管理 MVP（发货单聚合 CRUD + 卷清单 + 从合同带入 + 审核出库过账/红冲 + 轻量签收登记 + 打印）

Status: ready-for-agent

## Problem Statement

业务侧需要在新 Luna ERP 系统里管理向客户发货。旧系统（linaagi，Prisma + Next.js/React）已有完整但沉重的发货链：`Shipment`（PENDING_APPROVAL/DRAFT/PENDING/STAGED/SHIPPED/DELIVERED/REJECTED/CANCELLED 多态、审批工作流、承运商/运单号、发货收货地址、运费、H5 电子签收 token 与签名）+ `ShipmentItem`（产品/颜色/缸号/数量/匹数/单价/金额，卷信息以 `rollDetails` JSON 存行内）+ `ShipmentPickRecord`（STAGED 预出库拣货扫码锁卷）+ `ShipmentApprovalEffectOutbox`（审批通过后异步执行五步副作用：来源库存标记已发、应收台账、订单进度、客户画像、通知）+ `shipment-sign`（客户 H5 签收/拒签回链）。旧系统与新系统（Spring Boot + MyBatis-Plus + PostgreSQL + Vue3 + Element Plus）技术栈不兼容，且模型沉重。

新系统已具备发货所需的全部地基：销售合同（business.salescontract，含合同明细）、客户与联系人（baseinfo.customer）、产品档案、仓库与库位（stock.warehouse / warehouselocation）、通用库存流水（stock.stockledger）以及入库单的过账/红冲标杆（stock.stockin，ADR-0016/0017）。但**库存账的"出库"另一半仍完全缺失**——目前除领纱发纱（YARN_OUT）外没有任何面向客户的出库凭证，货物发给客户后账面库存不会减少。

业务侧希望先用新栈把"业务管理-发货管理"的最小可用闭环跑通：发货单主表 + 发货明细的聚合增删改查 + 逐卷登记的卷清单 + 从销售合同带入明细（含合同未发量）+ **审核即出库过账写负库存流水、取消已发货单红冲** + 一单一仓库与库存不足硬拦截 + 轻量内部签收登记 + 浏览器打印发货单/装箱清单 + 编号（SH-YYYYMMDD-NNN 按天取号）+ 字典/权限/菜单注册。这是业务管理域（business）继销售合同之后的第二个单据。

## Solution

在 luna-srv 的 `lunasrv-module-lunaerp` 的 `business`（业务管理）子包下新增 `shipment`，落地 **3 张表**：

1. `lunaerp_business_shipment` 发货单主表（SH 单号、三态严格过账状态机、客户必选、合同可选、一单一仓库、物流与运费、业务员、签收登记字段）
2. `lunaerp_business_shipment_item` 发货明细（产品 + 客户色号 + 缸号聚合行，聚合保存，ADR-0003 模式：随整单整体替换、级联逻辑删除，无独立 Controller/权限码）
3. `lunaerp_business_shipment_roll` 卷清单（逐卷记录卷号/实称重量/米数，独立子表而非旧系统行内 JSON，随单聚合保存，ADR-0030）

审核与红冲**复用既有通用库存流水表** `lunaerp_stock_ledger`：审核在同事务写 `SHIPMENT_OUT` 负流水，取消已发货单按原流水逐条写 `SHIPMENT_OUT_REVERSAL` 正流水（ADR-0029）。前端在 `views/lunaerp-elmui/business/shipment/` 落地 1 个菜单页：发货管理（列表页 + 工作台 4 Tab：基本信息 / 发货明细含卷清单弹窗 / 卷清单总览 / 操作日志）。配套 PostgreSQL 脚本完成 3 表 DDL、10120 发货管理菜单及按钮权限、2 个字典（发货状态/签收状态）初始化。

**发货单即出库凭证**（ADR-0029）：不另建销售出库单，不引入"发货通知单→拣货单→出库单"多单结构。三态 `DRAFT/COMPLETED/CANCELLED` 严格守卫，镜像入库单 ADR-0016——仅草稿可改可删；审核（发货）是瞬时动作、同事务写负流水且不可逆；已发货单只读，唯一写操作是"取消"（红冲）与"签收登记"；草稿取消不产生流水。一张发货单只能从一个仓库发货，审核时按"产品 + 单位 + 仓库"聚合校验可用余额，**不足硬拦截**（返回需求量/可用量，杜绝负库存）。客户必选、销售合同可选（样品/现货/补单常无合同）；选合同后可从合同明细带入产品、客户色号、单价与实时计算的"合同未发量"。所有关联只做存在性校验 + ID/名称快照，**零回写、零联动**——不写应收、不回写合同状态进度、不做发票关联。

## User Stories

### 发货单列表与检索

1. 作为业务员，我想在"业务管理 → 发货管理"菜单看到发货单列表（发货单号、客户、销售合同号、发货仓库、发货日期、总匹数、货物金额合计、运费、签收状态、单据状态、创建人、创建时间），以便掌握所有发货单。
2. 作为业务员，我想在一个关键词搜索框输入内容同时匹配发货单号、销售合同号、客户名称、卷号，以便只记其一也能找到单据。
3. 作为业务员，我想按单据状态（草稿/已发货/已取消）筛选，以便聚焦待处理单据。
4. 作为业务员，我想按客户筛选、按发货仓库筛选，以便分客户或分仓查看发货情况。
5. 作为业务员，我想按销售合同号筛选、按发货日期区间筛选，以便定位特定合同或时段的发货单。
6. 作为业务员，我想列表默认按发货日期/创建时间倒序、分页展示，最新单据在最前。
7. 作为业务员，我想列表每行有查看/编辑/删除操作，删除前二次确认；非草稿单不允许删除（按钮置灰或后端报错）。
8. 作为业务员，我想单据状态列用 DictTag 渲染（草稿 warning、已发货 success、已取消 danger），签收状态用独立 DictTag/el-tag（待签收 info、已签收 success、拒签 danger），以便一眼识别。

### 发货单新建/编辑（基本信息）

9. 作为业务员，我想点"新建发货单"进入工作台"基本信息"Tab，录入完整发货单。
10. 作为业务员，我想新建时必选客户（从客户档案远程搜索），选中后保存客户 ID + 客户名称快照，并可带出该客户的默认联系人与收货地址（允许手工修改）。
11. 作为业务员，我想可选关联销售合同（远程搜索，按当前客户过滤更佳），选中后保存合同 ID + 合同号快照；不强制有合同也能发货（样品/现货/补单场景）。
12. 作为业务员，我想必选发货仓库（只列启用状态仓库），保存仓库 ID + 仓库名称快照；一张发货单只能从一个仓库发货。
13. 作为业务员，我想填写发货地址与收货地址（选客户/合同时带出，可改）、承运商/物流公司（自由文本）、物流单号、发货日期（缺省今天）、预计到货日期。
14. 作为业务员，我想登记运费金额（独立字段，不并入货物金额），并可选择业务员（关联系统用户，存 ID + 姓名快照，可空）。
15. 作为业务员，我想填写币种（缺省 RMB，复用币种字典）与备注。
16. 作为业务员，我想新建保存时默认为草稿（DRAFT），服务端自动生成发货单号（SH-YYYYMMDD-NNN 按天取号），也可手填（撞号报错），并提供"预览下一个编号"。
17. 作为业务员，我想仅草稿态可编辑主表、明细与卷清单；已发货/已取消单全部只读（签收登记除外），防止过账后被篡改。
18. 作为业务员，我想仅草稿态可删除发货单（级联逻辑删除明细与卷清单）；已发货单不可删，只能红冲取消。

### 发货明细（聚合保存）

19. 作为业务员，我想在"发货明细"Tab 以可编辑表格一次录入多行明细并随整单保存（聚合保存、整体替换，ADR-0003），以便按发货安排一次性录单。
20. 作为业务员，我想每行明细必选产品（远程搜索产品档案），选中后保存产品 ID + 产品编码 + 产品名称快照；MVP 不支持无产品档案的杂项发货，也不支持发纱线规格。
21. 作为业务员，我想每行明细可填：客户色号、颜色名称、缸号/批次号（自由文本）、数量（必填，>0）、匹数（缺省 1）、单位（KG/M，允许同一单不同行混用单位）、单价、金额（=数量×单价，服务端权威重算）、备注。
22. 作为业务员，我想允许保存零明细草稿（先建单头后补明细），但审核时至少要有一条明细，否则后端报错。
23. 作为业务员，我想主表货物金额合计 = Σ 明细金额、总匹数 = Σ 明细匹数，均由服务端重算；主表不设单一数量合计（KG/M 可混用，跨单位求和无意义）。
24. 作为业务员，我想删除发货单时其全部明细与卷清单一并逻辑删除，不残留孤儿数据。

### 卷清单（逐卷登记）

25. 作为仓管员，我想在明细行点"卷清单"打开弹窗，逐卷登记这一批实际发出的布卷（卷号、实称重量 KG、米数、备注），以便实物追溯。
26. 作为仓管员，我想一个明细行可以登记 0~N 条卷记录，卷记录随发货单聚合保存（无独立端点/权限码）。
27. 作为仓管员，我想在明细区看到"已录卷重 X / 行数量 Y"的软提示，但系统不强制卷重合计等于明细数量，也允许零卷清单审核（散匹、拼卷、先单后补卷号是常态）。
28. 作为仓管员，我想在"卷清单总览"Tab 跨所有明细行平铺查看全部卷（卷号、所属产品/缸号、重量、米数、备注），以便仓管集中核对。
29. 作为仓管员，我想已发货/已取消单也能只读查看全部卷清单记录。

### 从合同 / 入库明细带入

30. 作为业务员，我想选定销售合同后能一键"从合同明细带入"：按合同明细行生成发货明细（产品、客户色号、单价），数量缺省填该明细行的"合同未发量"，我可改可删，以减少录入。
31. 作为业务员，我想"合同未发量"由系统实时计算（合同数量 − 该合同已发数量，按发货流水聚合），而不是存在合同上的固定值，以便看到的永远是最新缺口。
32. 作为业务员，我想在明细行可选择"来源入库明细"（选择该产品在本仓库的在库批次），选中后带出缸号/批次并保存来源入库明细 ID 快照，用于追溯"这批发的是哪批入的货"；该选择不占用、不锁定该批次库存。

### 审核过账与库存扣减

33. 作为仓主管，我想在草稿态点"审核"把发货单转为已发货（COMPLETED），后端先校验库存，再在同一事务按明细行逐条写负数出库流水（SHIPMENT_OUT，携带产品、仓库、缸号/批次、单位、单价、金额与本单关联），以便库存账真实减少。
34. 作为仓主管，我想审核时若某产品在该仓库的可用余额小于本单待发数量（同产品多行合并计算），后端硬拦截报错并返回需求量与可用量，而不是允许负库存，由人决定改量或分批。
35. 作为仓主管，我想审核操作需要独立审核权限（`:audit`），与制单权限分开，以便制单与审核分权。
36. 作为仓主管，我想审核要求至少一条明细，否则后端报错。
37. 作为业务员，我想非草稿单调用审核、已取消单调用任何写操作时，后端返回"当前状态不允许该操作"错误。

### 取消与红冲

38. 作为业务员，我想在草稿态点"取消"仅作废单据、不产生任何流水；已取消为终态，不可重新审核、不可编辑。
39. 作为仓主管，我想对已发货单点"取消"进行红冲：同事务按关联单号反查原 SHIPMENT_OUT 流水，逐条生成正数反向流水（SHIPMENT_OUT_REVERSAL），原流水永不物理删除、不可编辑，单据转为已取消终态；红冲需要 `:audit` 权限并二次确认。
40. 作为仓主管，我想对一张已签收的发货单执行取消（退货场景）时，系统二次确认提示"该单已签收"，确认后照常红冲。

### 签收登记

41. 作为文员，我想在已发货单上登记签收信息：签收状态（待签收/已签收/拒签）、签收人、签收日期、备注，以便回答"这单签了没、谁签的、什么时候"。
42. 作为文员，我想新建发货单时签收状态缺省为"待签收"；仅已发货（COMPLETED）单可登记或修改签收，已取消单禁登记。
43. 作为文员，我想登记"拒签"时系统只记录状态、不自动红冲库存；如要退货，由人执行"取消"。
44. 作为业务员，我想签收状态在列表与工作台都可见，无需进入详情即可分辨待签收/已签收/拒签。

### 打印

45. 作为业务员，我想对已发货单点"打印发货单"，通过浏览器打印随货同行的发货单（客户、地址、物流信息、按明细行汇总的品种/数量/匹数/金额）。
46. 作为仓管员，我想点"打印装箱清单"，按卷打印装箱清单（卷号、所属产品/缸号、每卷重量/米数），以便收货方清点。
47. 作为业务员，我想打印操作受独立打印权限（`:print`）控制；打印为前端浏览器打印，不依赖服务端生成 PDF。

### 管理与分权

48. 作为系统管理员，我想执行一份 SQL 脚本即可建 3 张表、建发货管理菜单及按钮、初始化 2 个字典，脚本可重复执行，部署无手工配置。
49. 作为系统管理员，我想发货单有独立的 query/create/update/delete/audit/print 权限码，以便按角色分权（业务员制单、仓主管审核）。
50. 作为开发，我想全部 3 张表的 DO 均继承 TenantBaseDO，多租户隔离由框架自动处理。
51. 作为开发，我想客户/合同/仓库/产品/来源入库明细/业务员关联均存 ID + 名称快照，列表展示无需 JOIN，对方档案改名或删除后本单信息不丢。
52. 作为开发，我想关联只做存在性校验，不做任何回写联动（不写应收、不回写合同进度、不触发通知），保持发货与财务/合同域单向依赖。

## Implementation Decisions

### 模块与落点

- 后端模块：`lunasrv-module-lunaerp`（已存在，在 `business` 子包下新增 `shipment`，与 `salescontract` 同层）。
- 后端逻辑包：`...module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.business.shipment`；主表 + 明细 + 卷清单三个 DO 同包，聚合保存，仅主表有 Controller。
- 前端视图区块：`lunaerp-elmui/business/shipment`（列表页 + 工作台 4 Tab）；前端 API 区块镜像于 `api/lunaerp-elmui/business/shipment`。沿用 ADR-0001 先行快速版目录。
- URL 前缀：`/lunaerp/business/shipment`；权限码前缀：`lunaerp:business:shipment:`。

### 数据模型

**发货单主表 `lunaerp_business_shipment`**（继承 TenantBaseDO 标准列）：

| 分组 | 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|------|
| 单号 | shipment_no | varchar(32) | ✓ | SH-YYYYMMDD-NNN，租户内全量唯一含软删行 |
| 状态 | status | varchar(16) | ✓ | 字典 lunaerp_shipment_status，缺省 DRAFT |
| 客户 | customer_id | int8 | ✓ | 逻辑关联客户（必填） |
| | customer_name | varchar(128) | - | 客户名称快照 |
| 合同 | sales_contract_id | int8 | - | 逻辑关联销售合同（可空） |
| | contract_no | varchar(32) | - | 合同号快照 |
| 仓库 | warehouse_id | int8 | ✓ | 逻辑关联发货仓库（必填） |
| | warehouse_name | varchar(128) | - | 仓库名称快照 |
| 地址 | shipping_address | varchar(255) | - | 发货地址 |
| | receive_address | varchar(255) | - | 收货地址 |
| 物流 | carrier | varchar(128) | - | 承运商，自由文本，不建字典 |
| | tracking_number | varchar(64) | - | 物流单号 |
| 日期 | shipping_date | date | - | 发货日期 |
| | expected_delivery_date | date | - | 预计到货日期 |
| 费用 | freight_fee | numeric(18,2) | - | 运费，独立于货物金额，缺省 0 |
| 合计 | total_amount | numeric(18,2) | - | Σ 明细货物金额，服务端重算 |
| | total_pieces | int4 | - | Σ 明细匹数，服务端重算 |
| 业务员 | salesperson_user_id | int8 | - | 关联系统用户，可空 |
| | salesperson_name | varchar(64) | - | 业务员姓名快照 |
| 币种 | currency | varchar(8) | ✓ | 缺省 RMB，复用币种字典 |
| 签收 | sign_status | varchar(16) | ✓ | 字典 lunaerp_shipment_sign_status，缺省 PENDING |
| | signed_by_name | varchar(64) | - | 签收人 |
| | signed_at | timestamp | - | 签收时间 |
| | sign_remark | varchar(500) | - | 签收备注 |
| 备注 | remark | varchar(1000) | - | |

索引：唯一索引 `(tenant_id, shipment_no)` 含软删行；辅助索引 `(status)`、`(customer_id)`、`(sales_contract_id)`、`(warehouse_id)`、`(shipping_date)`、`(sign_status)`。

**发货明细表 `lunaerp_business_shipment_item`**（继承 TenantBaseDO，聚合保存）：

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| shipment_id | int8 | ✓ | 所属发货单 |
| product_id | int8 | ✓ | 逻辑关联产品（必填） |
| product_code | varchar(64) | - | 产品编码快照 |
| product_name | varchar(128) | - | 产品名称快照 |
| customer_color_no | varchar(64) | - | 客户色号 |
| color_name | varchar(64) | - | 颜色名称 |
| vat_no | varchar(64) | - | 缸号/批次号，自由文本 |
| quantity | numeric(18,2) | ✓ | 数量，>0 |
| pieces | int4 | - | 匹数，缺省 1 |
| unit | varchar(8) | ✓ | 单位，复用 lunaerp_stock_in_unit：KG/M，允许同行混用 |
| unit_price | numeric(18,2) | - | 单价 |
| amount | numeric(18,2) | - | 金额=quantity×unit_price，服务端重算 |
| source_stock_in_item_id | int8 | - | 来源入库明细快照，可空，零回写 |
| remark | varchar(500) | - | 备注 |

索引：普通索引 `(shipment_id)`、`(product_id)`、`(source_stock_in_item_id)`。

**卷清单表 `lunaerp_business_shipment_roll`**（继承 TenantBaseDO，聚合保存，ADR-0030）：

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| shipment_id | int8 | ✓ | 所属发货单（冗余，便于级联与查询） |
| shipment_item_id | int8 | ✓ | 所属发货明细 |
| roll_no | varchar(64) | - | 卷号 |
| weight | numeric(18,2) | - | 实称重量 KG |
| length_m | numeric(18,2) | - | 米数，可空 |
| remark | varchar(500) | - | 备注 |

索引：普通索引 `(shipment_id)`、`(shipment_item_id)`。不建卷号唯一约束（卷号为现场录入文本，单内唯一性由业务自律，不设 DB 约束）。

### 发货单号

`SH-YYYYMMDD-NNN`（如 `SH-20260927-001`）：`SH` + `-` + 年月日 8 位 + `-` + 3 位日流水（超 999 自然扩位），按天取号，租户内全量唯一含软删行。沿用 IN-/INS- 连字符凭证风格与入库单取号/查重/有限次重试机制，仅格式串不同。提供只读"预览下一个编号"。

### 状态机与过账（ADR-0029）

状态枚举（字典 `lunaerp_shipment_status`）：`DRAFT` 草稿（缺省）/ `COMPLETED` 已发货 / `CANCELLED` 已取消。无"待发货/在途"中间态（发货是瞬时动作，在途由物流字段承载）、无审批流。

严格守卫：

- 更新主表/明细/卷清单：仅 DRAFT；非 DRAFT 报状态不允许。
- 删除：仅 DRAFT（级联逻辑删除 item + roll）；非 DRAFT 报状态不允许。
- 审核：仅 DRAFT → COMPLETED；要求至少一条明细；先做库存硬校验，同事务写负流水。
- 取消：
  - DRAFT → CANCELLED：仅作废，不写流水；
  - COMPLETED → CANCELLED：红冲，同事务按 `(related_doc_type='SHIPMENT', related_doc_id=本单)` 反查全部原 SHIPMENT_OUT 流水逐条插入反向行（SHIPMENT_OUT_REVERSAL，数量/金额取正、库位/单价/批次等快照沿用），原流水保留；
  - CANCELLED 为终态。
- 签收登记：仅 COMPLETED 可登记/修改；CANCELLED 禁登记。

### 过账流水展开与库存校验

- 审核过账**按明细行展开**：每条明细一条 SHIPMENT_OUT 负流水，数量取 −item.quantity、金额取 −item.amount，携带产品快照、仓库（必填）、缸号/批次、单位、关联凭证（related_doc_type='SHIPMENT'、related_doc_id、related_doc_no=shipment_no）。卷清单不展开成流水；发货不选库位，流水 location 为空。
- 库存硬校验：审核前按"产品 + 单位 + 仓库"对本单明细分组合并需求量，与库存余额（流水实时 SUM）比较；任一组合需求量 > 可用余额则拦截，错误信息返回该组合的需求量与可用量。同事务内完成校验与过账，防止并发透支。
- 红冲不额外校验余额：红冲只把数量加回，不会制造负库存。

### 合同未发量与从合同带入

- "合同未发量"为实时只读计算、不落库：以合同明细行的合同数量为基准，减去该合同关联的已生效出库数量（按 SHIPMENT_OUT 流水，依产品/客户色号聚合），结果 ≥0。
- "从合同明细带入"由后端端点返回预填明细集合（产品 ID/快照、客户色号、单价、数量=未发量），前端据此填充可编辑表格；用户可改可删，不直接落库。

### 聚合保存

- 发货单 create/update 接收主表字段 + items 数组（每行内嵌 rolls 数组）；service 在事务内：校验关联与回填快照 → 明细整表逻辑删除后重插（ADR-0003）→ 卷清单同步整表替换 → 重算 total_amount/total_pieces。
- 明细与卷清单没有独立 Controller/权限码（整单录入，非高频并行）。
- 删主表级联逻辑删除 items + rolls（流水不动；仅草稿可删，草稿无流水）。

### Controller 与 API 契约

基础路径 `/lunaerp/business/shipment`：

- `GET /page` — pageNo/pageSize/keyword/status/customerId/warehouseId/contractNo/shippingDateStart/shippingDateEnd；keyword 对 shipment_no / contract_no / customer_name 及卷号 OR 模糊；默认倒序；PageResult（不含明细）。
- `GET /get?id=` — 详情 RespVO（含 items 及其 rolls）。
- `GET /get-next-no` — 预览下一个发货单号。
- `GET /from-contract?contractId=` — 返回从合同明细预填的发货明细集合（含实时未发量）。
- `POST /create` — SaveReqVO（主表 + items[].rolls）；status 缺省 DRAFT；返回 id。
- `PUT /update` — 同结构含 id；仅 DRAFT。
- `DELETE /delete?id=` — 仅 DRAFT，级联删除 items/rolls。
- `PUT /audit?id=` — DRAFT → COMPLETED 并过账（`:audit`）。
- `PUT /cancel?id=` — DRAFT 作废 或 COMPLETED 红冲 → CANCELLED（`:audit`）。
- `PUT /update-sign` — COMPLETED 单登记/修改签收（signStatus/signedByName/signedAt/signRemark）。

所有写端点 `@PreAuthorize("@ss.hasPermission('lunaerp:business:shipment:<action>')")`，返回 CommonResult/PageResult，VO 带 Swagger 与 jakarta.validation 注解，风格对齐 stockin/salescontract。签收登记鉴权复用 `:update` 权限码（对已发货单的限定字段补录），不新增权限码与按钮位。

### 关联校验与快照

- customerId 必填且存在，回填 customerName；warehouseId 必填且为 ACTIVE，回填 warehouseName。
- salesContractId（若传）存在，回填 contractNo；salespersonUserId（若传）存在，回填姓名。
- 每行 productId 必填且存在，回填 productCode/productName；sourceStockInItemId（若传）存在（仅快照）。
- 关联方被停用/删除导致不可用时，按"关联引用不存在"处理。

### 权限码

`lunaerp:business:shipment:query` / `:create` / `:update` / `:delete` / `:audit`（审核 + 红冲）/ `:print`。

### 错误码

`1-030-016-xxx` 发货单（主表/明细/卷清单共用）：

- `...016_000` 发货单不存在
- `...016_001` 发货单号【{}】已存在
- `...016_002` 当前状态不允许该操作
- `...016_003` 关联的{}不存在或不可用（客户/仓库/合同/产品/来源入库明细/业务员）
- `...016_004` 审核失败：发货单没有明细
- `...016_005` 库存不足：{} 需要 {} {}，当前可用 {} {}（审核硬拦截）
- `...016_006` 签收状态非法

在 lunaerp 模块 ErrorCodeConstants 追加；CONTEXT.md 段位表已同步。

### 字典（2 个）

- `lunaerp_shipment_status`（dict_type id=2245）：DRAFT 草稿(warning) / COMPLETED 已发货(success) / CANCELLED 已取消(danger)。
- `lunaerp_shipment_sign_status`（dict_type id=2246）：PENDING 待签收(info) / SIGNED 已签收(success) / REFUSED 拒签(danger)。
- 明细单位复用库存域 `lunaerp_stock_in_unit`、币种复用 `lunaerp_sales_contract_currency`；承运商自由文本、流水 txn_type 与来源 source 均不入字典。

### 菜单与路由

| ID | 名称 | type | parent | 权限/组件 |
|----|------|------|--------|-----------|
| 10120 | 发货管理 | 2 菜单 | 10100 | `lunaerp:business:shipment:query`，component `lunaerp-elmui/business/shipment/index`，sort 2 |
| 10121 | 发货创建 | 3 | 10120 | `:create` |
| 10122 | 发货修改 | 3 | 10120 | `:update` |
| 10123 | 发货删除 | 3 | 10120 | `:delete` |
| 10124 | 发货审核 | 3 | 10120 | `:audit`（审核 + 红冲） |
| 10125 | 发货打印 | 3 | 10120 | `:print` |

发货工作台不进菜单：前端静态隐藏路由 `business/shipment/workspace/:id?`（无 id 新建、有 id 查看/编辑），仿入库/检验单工作台。菜单/字典 SQL 先 DELETE 再 INSERT，可重复执行。

### 前端页面结构与交互

- **发货列表页**：关键词 + 状态 + 客户 + 仓库 + 合同号 + 发货日期区间搜索；列：发货单号/客户/合同号/仓库/发货日期/总匹数/货物金额合计/运费/签收状态 tag/单据状态 DictTag/创建人/创建时间/操作（查看、编辑[仅草稿]、删除[仅草稿]、审核/取消按状态与权限显示）；分页；新建按钮。
- **发货工作台**（el-tabs 4 Tab）：
  - 基本信息：客户远程（必选）、合同远程（可选）、仓库下拉（必选）、发货/收货地址、承运商/运单号/日期/预计到货、运费、业务员、币种、备注；含"签收信息"区（仅 COMPLETED 可编辑）；操作栏按状态：DRAFT [保存][审核][取消][删除]、COMPLETED [取消（红冲，二次确认）][打印]、CANCELLED 全只读。
  - 发货明细：可编辑表格（产品远程带快照、客户色号、颜色、缸号、数量、匹数、单位字典、单价、金额、来源入库明细选择、备注）；行操作"卷清单"打开弹窗（卷号/重量/米数/备注子表，显示"已录卷重 X / 行数量 Y"）；提供"从合同明细带入"；明细/卷随整单保存；非草稿只读。
  - 卷清单总览：跨明细行平铺全部卷只读表格，是装箱清单预览与打印数据源。
  - 操作日志：占位"开发中"。
- **打印**：前端打印样式 + window.print，发货单与装箱清单两套版式。

### SQL 脚本

新建 `luna-srv/sql/postgresql/lunaerp-business-shipment.sql`，参照 `lunaerp-stock.sql` 与 `lunaerp-business-sales-contract.sql` 体例：3 张表 DDL（CREATE TABLE IF NOT EXISTS + 主键 DO 块判存 + 列注释 + 索引，单号唯一索引含软删行）、system_menu（10120-10125）、system_dict_type（2245/2246）与 dict_data。可重复执行，仅语法自查、禁止直连共享库。

## Testing Decisions

### 测试哲学

只测外部业务行为（Service 的可观察语义），不断言内部调用次数等实现细节；测试命名描述行为。前端不做测试（lunaerp-elmui 先行快速版，ADR-0001，与入库单一致）。

### 测试 seam（唯一接缝）

Service 层纯 Mockito 单元测试，单个测试类 `ShipmentServiceImplTest`：不起 Spring 上下文、不连数据库，Mock 各 Mapper 与客户/合同/仓库/产品/入库明细/业务员校验依赖。不新建 MockMvc/嵌入式 PostgreSQL 基建。Prior art：`StockInServiceImplTest`（过账/红冲，最贴近）、`InspectionOrderServiceImplTest`、`SalesContractServiceImplTest`（聚合保存）。库存余额/流水为只读聚合查询，不做单测。

### 覆盖的行为

1. 创建：status 缺省 DRAFT；shipment_no 自动生成（SH-YYYYMMDD-NNN）；手填撞号报 ...016_001；客户必填、仓库必填且 ACTIVE；合同/产品/来源入库明细/业务员不存在报 ...016_003；各类名称快照正确回填。
2. 更新：仅 DRAFT 可更新，非 DRAFT 报 ...016_002；明细与卷清单整体替换；total_amount/total_pieces 重算。
3. 删除：仅 DRAFT 可删；非 DRAFT 报 002；级联逻辑删除 items + rolls。
4. 审核：非 DRAFT 报 002；零明细报 ...016_004；库存不足报 ...016_005（返回需求量/可用量）；通过时按明细写负数 SHIPMENT_OUT 流水，同事务。
5. 取消：DRAFT 取消不写流水；COMPLETED 取消按原流水逐条写正数 SHIPMENT_OUT_REVERSAL（快照沿用）；CANCELLED 再操作报 002。
6. 签收：仅 COMPLETED 可登记，DRAFT/CANCELLED 登记报 002；非法签收状态报 ...016_006；拒签不产生库存流水。
7. 从合同带入：返回的明细数量等于实时"合同未发量"；合同不存在报 003。

## Out of Scope

- 审批工作流及审批态（待审批/驳回），STAGED 预出库、拣货扫码与卷状态占用/锁库。
- 卷级库存身份与卷档案、从在库卷勾选发货；卷号唯一性 DB 约束。
- H5 电子签收（一次性 token、签名图片、签字回链）、拒签自动回滚；本期仅内部文员轻量登记。
- 应收台账/记账、发票关联（invoiceRequestId/invoicedQuantity）、对账与财务锁、客户画像刷新、自动通知。
- 合同状态/订单进度回写（仅保留未来只读聚合可能）、报价/外采报销/发样（STOCK/PURCHASE source）联动。
- 服务端 PDF 导出、Excel 导入导出、OCR/AI 录入。
- 多仓库发货（一单一仓库）、跨仓发货；纱线规格发货（MVP 仅产品）。
- 负库存放行、初始库存/期初发货导入、单位自动换算（KG/M 分别计量）。
- 采购退货、调拨、其他出库类型及通用"出库单"域；盘点、移库、库存预警/安全库存。
- 操作日志模块（工作台 Tab 占位）、附件/图片上传、动态表单 extensionData、乐观锁/版本号。
- 前端单元/组件测试、REST 集成测试与测试数据库基建；正式 UI（views/lunaerp/）版本。

## Further Notes

### 已落地文档

- [CONTEXT.md](../../CONTEXT.md) — 新增"业务管理：发货管理"术语分区（发货单/发货明细/卷清单/发货单状态/发货单号/签收状态/运费/合同未发量）、流水类型 SHIPMENT_OUT(S)/REVERSAL、shipment 模块边界、2 个字典、错误码 1-030-016 段位。
- [ADR-0029](../../docs/adr/0029-shipment-outbound-voucher-posting.md) — 发货单即出库凭证、三态严格过账、一单一仓库与不足硬拦截、零联动。
- [ADR-0030](../../docs/adr/0030-shipment-roll-independent-child-table.md) — 卷清单独立子表、聚合保存、不锁卷、软齐套。

### 实现顺序建议

1. SQL 脚本（3 表 + 索引 + 10120-10125 菜单 + 字典 2245/2246）。
2. 发货单 DO×3（主表/明细/卷）+ Mapper + VO 套装 + 016 段错误码常量。
3. 发货单 Service（取号、关联校验快照、聚合保存、合计重算、库存硬校验、审核过账、红冲、签收登记、从合同带入）+ Controller。
4. ServiceImpl 纯 Mockito 单元测试（单个测试类）。
5. 前端 API/TS 类型 + 隐藏工作台路由。
6. 发货列表页 → 发货工作台（含卷清单弹窗）→ 卷清单总览与打印。

### 参考

- 旧系统模型：`linaagi/server/prisma/schema.prisma`（Shipment L1481 / ShipmentItem L1587 / ShipmentPickRecord L1575）。
- 旧系统后端：`linaagi/server/src/routes/shipments.ts`、`shipment-sign.ts`、`services/shipment-approval-effects.service.ts`（副作用/电子签收本期不做）。
- 旧系统前端：`linaagi/web/app/staff/components/shipment/`、`web/lib/shipment-badges.ts`、`shipment-rolls.ts`。
- 新系统后端模板：`stock/stockin`（过账/红冲/取号，最贴近）、`stock/stockledger`（流水与余额聚合）、`business/salescontract`（聚合保存 ADR-0003）。
- 新系统前端模板：`views/lunaerp-elmui/stock/stock-in/`（列表 + 工作台多 Tab）、`business/sales-contract/`。
- SQL 模板：`lunaerp-stock.sql`、`lunaerp-business-sales-contract.sql`。
