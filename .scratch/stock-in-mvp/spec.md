# 库存管理 - 入库管理 MVP（入库单聚合 CRUD + 仓库库位 + 逐匹库位分配 + 审核过账/红冲 + 通用库存流水台账）

Status: ready-for-agent

## Problem Statement

业务侧需要在新 Luna ERP 系统里管理入库。旧系统（linaagi，Prisma + Next.js/React）已有完整但沉重的仓储链：`StockIn`（8 种纺织细分类型、DRAFT/QC_PENDING/审批态、金额/应付/对账锁、合同/胚布指示单/染色指示单/纱线采购单多关联、批次联动）+ `StockInItem`（卷/匹/KG/米/单价）+ `InventoryPlacement`（明细→库位放置）+ `WarehouseMap`（2D 平面图画布）→ `WarehouseZone`（多边形库区/容量）→ `WarehouseLocation`（行列层 xyz 坐标库位）→ `InventoryMovementLog`（移库流水）→ 库存余额/预警；审核生效时还会记供应商应付、回写订单进度、批次转可用。旧系统与新系统（Spring Boot + MyBatis-Plus + PostgreSQL + Vue3 + Element Plus）技术栈不兼容。

新系统库存域此前完全空白：无仓库/入库/库存任何表；纱线库存台账（ADR-0012）已于 2026-09-21 因设计不完善整体回退；ADR-0013 检验同步入库明确写着"待入库单模块就绪后补做"——**本期就是这个被等待的入库单模块**，但检验同步本身仍属后续独立 issue，本期只建模块、不做同步。

业务侧希望先用新栈把"库存管理-入库管理"的最小可用闭环跑通：入库单主表 + 入库明细的聚合增删改查 + **基本的仓库/库位主数据** + **逐匹（逐明细行）分配库位** + **审核即过账写库存流水、取消已审核单红冲** + 库存余额与流水只读查询 + 编号（IN-YYYYMMDD-NNN 按天取号）+ 字典/权限/菜单注册。这是库存管理域（stock）的第一个模块，也是继 baseinfo/business/production 之后的第四个顶级业务域。

## Solution

在 luna-srv 的 `lunasrv-module-lunaerp` 新增与 baseinfo/business/production 平级的 `stock`（库存管理）子包，落地 **6 张表**：

1. `lunaerp_stock_warehouse` 仓库档案（编号/名称/地址/状态）
2. `lunaerp_stock_warehouse_location` 库位（仓库内编码唯一，两级简版，不建库区/平面图/坐标/容量，ADR-0018）
3. `lunaerp_stock_in` 入库单主表（IN 单号、四类型、三态严格过账状态机、一单一仓库、供应商与四类上游单据快照关联）
4. `lunaerp_stock_in_item` 入库明细（聚合保存，ADR-0003 模式：随整单保存整体替换、级联逻辑删除，无独立 Controller/权限码）
5. `lunaerp_stock_in_item_placement` 库位分配放置记录（一条明细可拆放多个库位，随入库单聚合保存）
6. `lunaerp_stock_ledger` 通用库存流水台账（只存流水、不存余额；审核写正数 STOCK_IN、红冲写负数 STOCK_IN_REVERSAL；按"产品+单位+仓库"实时 SUM 聚合余额，ADR-0017）

前端在 `views/lunaerp-elmui/stock/` 下落地 3 个菜单页：入库管理（列表页 + 工作台 3 Tab）、库存查询（余额 + 流水两个 Tab）、仓库管理（仓库/库位主从同页 CRUD）。配套 PostgreSQL 脚本完成 6 表 DDL、10300 库存管理目录及 10310/10320/10330 菜单与按钮权限、3 个字典（入库状态/入库类型/入库单位）初始化。

**过账状态机（ADR-0016）刻意不一致现有单据 MVP 的"编辑页自由切态"惯例**：入库单是库存账的凭证，三态 DRAFT/COMPLETED/CANCELLED 严格守卫——仅草稿可改可删；审核在同事务写流水且不可逆（无 ACTIVE/QC_PENDING/审批流）；已审核单只能"取消"（红冲，写负数流水，原流水永不物理删除），取消后为终态不可重新审核；草稿取消不产生流水。审核/红冲权限（`:audit`）与制单权限（`:create/:update`）独立分权。上游单据（合同/胚布单/染色单/胚布品种）仅做存在性校验 + ID/名称快照，**零回写、零联动**——检验同步、采购自动入库、订单进度回写全部留给后续 issue。

## User Stories

### 入库单列表与检索

1. 作为仓管员，我想在"库存管理 → 入库管理"菜单看到入库单列表（入库单号、单据日期、入库类型、仓库、供应商、金额合计、状态、创建人、创建时间），以便掌握所有入库单。
2. 作为仓管员，我想在搜索框输入关键词同时匹配入库单号、产品编码、产品名称、供应商名称、合同号，以便只记其一也能找到单据。
3. 作为仓管员，我想按入库状态（草稿/已审核/已取消）筛选，以便聚焦待处理单据。
4. 作为仓管员，我想按入库类型（采购入库/生产入库/退货入库/其他入库）筛选，以便分类查看。
5. 作为仓管员，我想按仓库筛选，以便分仓查看入库情况。
6. 作为仓管员，我想按供应商筛选、按单据日期区间筛选，以便定位特定往来或时段的入库单。
7. 作为仓管员，我想列表默认按单据日期倒序、分页展示，最新单据在最前。
8. 作为仓管员，我想列表每行有查看/编辑/删除操作，删除前二次确认，避免误删；非草稿单不允许删除（按钮置灰或后端报错）。
9. 作为仓管员，我想状态列用 DictTag 渲染（草稿 warning、已审核 success、已取消 danger），类型列用 DictTag 区分，以便一眼识别。

### 入库单新建/编辑（基本信息）

10. 作为仓管员，我想点"新建入库单"进入工作台"基本信息"Tab，录入完整入库单。
11. 作为仓管员，我想新建时必选入库类型（PURCHASE 采购入库 / PRODUCTION 生产入库 / RETURN 退货入库 / OTHER 其他入库，字典 `lunaerp_stock_in_type`），以便标明货从哪来。
12. 作为仓管员，我想必选入库仓库（从启用状态的仓库档案远程/下拉选择），选中后保存仓库 ID + 仓库名称快照；一张入库单只能入一个仓库。
13. 作为仓管员，我想可选供应商（从供应商档案远程搜索），选中后保存供应商 ID + 公司名快照（采购/退货场景典型使用，MVP 不按类型强制必填）。
14. 作为仓管员，我想可选关联销售合同（远程搜索，带出合同号快照）、关联胚布单（远程搜索，带出胚布单号快照）、关联染色单（远程搜索；染色单无业务编号，仅存 ID）、所属胚布品种（从胚布品种档案选择，带出名称快照），以便追溯业务来源；这些关联全部可空、不做条件必填。
15. 作为仓管员，我想填写单据日期（缺省今天）与备注。
16. 作为仓管员，我想新建保存时默认存为草稿（DRAFT），服务端自动生成入库单号（IN-YYYYMMDD-NNN 按天取号），也可手填（撞号报错），以便编号唯一可追溯；提供"预览下一个编号"。
17. 作为仓管员，我想仅草稿态可编辑主表与明细；已审核/已取消单全部只读，防止入账后被篡改。
18. 作为仓管员，我想仅草稿态可删除入库单（级联逻辑删除明细与放置记录）；已审核单不可删，只能红冲取消。

### 入库明细（聚合保存）

19. 作为仓管员，我想在"入库明细"Tab 以表格网格一次录入多行明细并随整单保存（聚合保存，整体替换，ADR-0003），以便按装箱单一次性录单。
20. 作为仓管员，我想每行明细必选产品（远程搜索产品档案），选中后保存产品 ID + 产品编码 + 产品名称快照；不支持无产品档案的杂项入库。
21. 作为仓管员，我想每行明细可填：批次号/缸号（自由文本）、卷号、匹数（缺省 1）、数量（必填，>0）、单位（KG/M，允许同一单不同行混用单位）、单价、金额（=数量×单价，服务端权威重算）、备注。
22. 作为仓管员，我想允许保存零明细草稿（先建单头后补明细），但审核时至少要有一条明细，否则后端报错。
23. 作为仓管员，我想主表金额合计 = Σ 明细金额，由服务端重算，前端不可信；主表**不设数量合计字段**（KG/M 可混用，跨单位求和无意义），列表不展示数量合计列。
24. 作为仓管员，我想删除入库单时其全部明细与放置记录一并逻辑删除，不残留孤儿数据。

### 逐匹分配库位

25. 作为仓管员，我想在明细表格每行点"分配库位"打开弹层，把该明细的货逐批分配到具体库位，以便记录实物上架位置。
26. 作为仓管员，我想一条明细可以添加多条放置记录：库位（下拉只列本单所选仓库下"启用"状态的库位）+ 匹数 + 数量 + 备注；例如同一产品行 10 匹可 6 匹放 A-01、4 匹放 A-02。
27. 作为仓管员，我想弹层显示"已分配 X / 共 Y 匹、已分配数量 / 明细数量"的对比提示，但 MVP 不强制分配齐套（允许部分上架/先入账后补，草稿态可改）。
28. 作为仓管员，我想放置记录随入库单聚合保存（无独立端点/权限码）；未分配库位的明细审核时照常过账，流水带仓库、库位为空。
29. 作为仓管员，我想工作台以只读方式展示每条明细的库位分配去向（库位编码 + 匹数 + 数量），已审核/已取消单也能查看。

### 审核过账与红冲

30. 作为仓主管，我想在草稿态点"审核"把入库单转为已审核（COMPLETED），后端在同一事务内按"每条放置记录一条正数流水（STOCK_IN）；明细无放置记录时按明细行一条（库位为空）"写库存流水，以便库存账真实增加。
31. 作为仓主管，我想审核操作需要独立的审核权限（`:audit`），与制单权限分开，以便制单与审核分权。
32. 作为仓管主管，我想对已审核单点"取消"进行红冲：同事务按原流水逐条生成负数反向流水（STOCK_IN_REVERSAL），原流水永不物理删除、不可编辑，单据转为已取消终态；红冲同样需要 `:audit` 权限。
33. 作为仓管员，我想草稿态点"取消"仅作废单据、不产生任何流水（该操作使用 `:update` 权限）；已取消为终态，不可重新审核、不可编辑。
34. 作为仓管主管，我想非草稿单调用审核/已取消单调用任何写操作时后端返回"当前状态不允许该操作"错误。

### 仓库与库位主数据

35. 作为系统管理员，我想在"仓库管理"页主从同页管理仓库（左列表 + 右库位列表）：仓库字段为编号（WH+年份+4 位流水，服务端取号/可手填）、名称（必填）、地址、状态（ACTIVE 启用/INACTIVE 停用，不建字典）、备注。
36. 作为系统管理员，我想库位字段为编码（如 A-01-02，手填，同一仓库内唯一）、名称、排序、状态、备注；库位是两级简版，没有库区、平面图、坐标、容量、二维码。
37. 作为系统管理员，我想删除仓库时若其下仍有库位或已被入库单引用则禁止删除（报错并引导改用停用）；删除库位时若已被放置记录引用则禁止删除。
38. 作为仓管员，我想停用的仓库/库位不会出现在新入库单的选择项中，但历史入库单上的仓库/库位名称快照照常展示。
39. 作为系统管理员，我想仓库提供"预览下一个编号"接口；仓库与库位均为租户隔离（TenantBaseDO）、逻辑删除。

### 库存余额与流水查询

40. 作为仓管员，我想在"库存查询"菜单看到库存余额列表：按"产品 + 单位 + 仓库"聚合的 SUM 数量（实时聚合，无余额表），列含产品编码、产品名称、仓库、单位、库存数量；支持按产品关键词与仓库筛选、分页。
41. 作为仓管员，我想在"库存流水"Tab 查看流水明细分页（产品、仓库、库位、收支类型[入库/红冲]、数量（带正负号）、单位、单价、金额、关联入库单号、批次、时间），支持按产品/仓库/收支类型/关联单号/时间区间筛选。
42. 作为仓管员，我想在余额列表点某行能联动筛选该产品+仓库的流水，以便核对余额构成。
43. 作为仓管员，我想流水只可读、不可手工新增或修改（库存账只能由入库单审核/红冲驱动）。

### 管理与分权

44. 作为系统管理员，我想执行一份 SQL 脚本即可建 6 张表、建库存管理目录与入库管理/库存查询/仓库管理菜单及按钮、初始化 3 个字典，脚本可重复执行，部署无手工配置。
45. 作为系统管理员，我想入库单有独立的 query/create/update/delete/audit 权限码，仓库与库位各有 query/create/update/delete 权限码，库存余额/流水仅 query 权限码，以便按角色分权（仓管员制单、仓主管审核）。
46. 作为系统管理员，我想入库状态、入库类型、入库单位均来自系统字典并用 DictTag 渲染，以便业务方后续扩充枚举。
47. 作为开发，我想全部 6 张表的 DO 均继承 TenantBaseDO，多租户隔离由框架自动处理。
48. 作为开发，我想产品/供应商/仓库/库位/合同/胚布单/染色单/胚布品种关联均存 ID + 名称快照，列表展示无需 JOIN，对方档案删除或改名后本单信息不丢。
49. 作为开发，我想上游单据关联只做存在性校验，不做任何回写联动（不回写订单进度、不转批次可用、不触发应付），保持库存域与生产/业务域单向依赖。

## Implementation Decisions

### 模块与包路径

- 后端模块：`lunasrv-module-lunaerp`（已存在，新增 stock 顶级子包，与 baseinfo/business/production 平级）
- 后端包：
  - 入库单（主表 + 明细 + 放置三个 DO 同包，聚合保存，只有主表有 Controller）：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.stock.stockin`
  - 库存台账与余额（只读查询）：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.stock.stockledger`
  - 仓库：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.stock.warehouse`
  - 库位：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.stock.warehouselocation`
- 前端视图：
  - `luna-ui/src/views/lunaerp-elmui/stock/stock-in/`（`index.vue` 列表页 + `workspace/index.vue` 工作台 + `workspace/BasicInfoTab.vue` + `workspace/ItemsTab.vue` + `workspace/PlacementDialog.vue` + `workspace/LogsTab.vue`）
  - `luna-ui/src/views/lunaerp-elmui/stock/inventory-balance/index.vue`（余额 + 流水两个 Tab）
  - `luna-ui/src/views/lunaerp-elmui/stock/warehouse/index.vue`（仓库/库位主从同页）
- 前端 API：`luna-ui/src/api/lunaerp-elmui/stock/{stock-in,stock-ledger,inventory-balance,warehouse,warehouse-location}/index.ts`
- 沿用 ADR-0001：落在 `lunaerp-elmui/` 先行快速版目录

### 数据模型

**仓库表 `lunaerp_stock_warehouse`**（继承 TenantBaseDO：id/creator/create_time/updater/update_time/deleted/tenant_id）：

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| warehouse_no | varchar(32) | ✓ | WH+年份+4 位流水（WH20260001），租户内全量唯一含软删行，仿 ADR-0005 |
| name | varchar(128) | ✓ | 仓库名称 |
| address | varchar(255) | - | 地址 |
| status | varchar(16) | ✓ | ACTIVE 启用（缺省）/ INACTIVE 停用，不建字典，仿客户/供应商 |
| remark | varchar(500) | - | 备注 |

索引：唯一索引 `(tenant_id, warehouse_no)` 含软删行；普通索引 `(status)`。

**库位表 `lunaerp_stock_warehouse_location`**（继承 TenantBaseDO）：

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| warehouse_id | int8 | ✓ | 所属仓库 |
| code | varchar(32) | ✓ | 库位编码（手填，如 A-01-02），同仓库内唯一含软删行 |
| name | varchar(64) | - | 库位名称 |
| sort | int4 | - | 排序，缺省 0 |
| status | varchar(16) | ✓ | ACTIVE（缺省）/ INACTIVE，不建字典 |
| remark | varchar(500) | - | 备注 |

索引：唯一索引 `(tenant_id, warehouse_id, code)` 含软删行；普通索引 `(warehouse_id)`、`(status)`。

**入库单主表 `lunaerp_stock_in`**（继承 TenantBaseDO）：

| 分组 | 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|------|
| 单号 | in_no | varchar(32) | ✓ | IN-YYYYMMDD-NNN，租户内全量唯一含软删行（沿用 ADR-0015 连字符风格） |
| 类型 | in_type | varchar(16) | ✓ | 字典 lunaerp_stock_in_type：PURCHASE/PRODUCTION/RETURN/OTHER |
| 状态 | status | varchar(16) | ✓ | 字典 lunaerp_stock_in_status，缺省 DRAFT |
| 入库仓库 | warehouse_id | int8 | ✓ | 逻辑关联仓库档案 |
| | warehouse_name | varchar(128) | - | 仓库名称快照 |
| 供应商 | supplier_id | int8 | - | 逻辑关联供应商档案 |
| | supplier_name | varchar(128) | - | 供应商公司名快照 |
| 关联合同 | sales_contract_id | int8 | - | 逻辑关联销售合同 |
| | contract_no | varchar(32) | - | 合同号快照 |
| 关联胚布单 | greige_order_id | int8 | - | 逻辑关联胚布单 |
| | greige_order_no | varchar(32) | - | 胚布单号快照 |
| 关联染色单 | dyeing_order_id | int8 | - | 逻辑关联染色单（染色单无业务编号，ADR-0009，不存编号快照） |
| 胚布品种 | greige_type_id | int8 | - | 逻辑关联胚布品种档案 |
| | greige_type_name | varchar(128) | - | 胚布品种名称快照 |
| 金额合计 | total_amount | numeric(18,2) | - | Σ 明细金额，服务端权威重算；**不设数量合计**（单位可 KG/M 混用） |
| 单据日期 | in_date | date | ✓ | 缺省今天 |
| 备注 | remark | varchar(1000) | - | |

索引：唯一索引 `(tenant_id, in_no)` 含软删行；辅助索引 `(status)`、`(in_date)`、`(warehouse_id)`、`(supplier_id)`、`(in_type)`。

**入库明细表 `lunaerp_stock_in_item`**（继承 TenantBaseDO，聚合保存）：

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| stock_in_id | int8 | ✓ | 所属入库单 |
| product_id | int8 | ✓ | 逻辑关联产品档案（必填，不支持无档案入库） |
| product_code | varchar(64) | - | 产品编码快照 |
| product_name | varchar(128) | - | 产品名称快照 |
| batch_no | varchar(64) | - | 批次号/缸号，自由文本 |
| roll_no | varchar(64) | - | 卷号，可空 |
| piece_count | int4 | - | 匹数/件数，缺省 1 |
| quantity | numeric(18,2) | ✓ | 数量，>0 |
| unit | varchar(8) | ✓ | 单位，字典 lunaerp_stock_in_unit：KG/M，允许单行各自单位 |
| unit_price | numeric(18,2) | - | 单价 |
| amount | numeric(18,2) | - | 金额=quantity×unit_price，服务端重算 |
| remark | varchar(500) | - | 备注 |

索引：普通索引 `(stock_in_id)`、`(product_id)`。

**库位分配放置表 `lunaerp_stock_in_item_placement`**（继承 TenantBaseDO，聚合保存，ADR-0018）：

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| stock_in_id | int8 | ✓ | 所属入库单（冗余，便于级联与过账查询） |
| stock_in_item_id | int8 | ✓ | 所属入库明细 |
| location_id | int8 | ✓ | 逻辑关联库位（必须属于主表 warehouse_id，service 校验，否则报 1_030_011_004） |
| location_code | varchar(32) | - | 库位编码快照 |
| piece_count | int4 | - | 本放置匹数，可空（按数量放置的场景） |
| quantity | numeric(18,2) | ✓ | 本放置数量，>0 |
| unit | varchar(8) | ✓ | 单位，跟随明细 |
| remark | varchar(500) | - | 备注 |

索引：普通索引 `(stock_in_id)`、`(stock_in_item_id)`、`(location_id)`。不建"放置匹数合计=明细匹数"的 DB/服务端强制校验（仅前端提示）。

**库存流水台账表 `lunaerp_stock_ledger`**（继承 TenantBaseDO，只存流水不存余额，ADR-0017）：

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| product_id | int8 | ✓ | 关联产品 |
| product_code | varchar(64) | - | 产品编码快照 |
| product_name | varchar(128) | - | 产品名称快照 |
| warehouse_id | int8 | ✓ | 仓库（来自入库单头，必填） |
| warehouse_name | varchar(128) | - | 仓库名称快照 |
| location_id | int8 | - | 库位（可空：明细未分配库位时过账） |
| location_code | varchar(32) | - | 库位编码快照 |
| batch_no | varchar(64) | - | 批次/缸号（来自明细） |
| txn_type | varchar(16) | ✓ | STOCK_IN 入库（正数）/ STOCK_IN_REVERSAL 红冲（负数），后端枚举不入字典 |
| quantity | numeric(18,2) | ✓ | 带符号数量：入库正、红冲负 |
| unit | varchar(8) | ✓ | KG/M |
| unit_price | numeric(18,2) | - | 单价快照 |
| amount | numeric(18,2) | - | 金额快照（红冲行同样为负） |
| related_doc_type | varchar(32) | ✓ | 关联单据类型，本期固定 STOCK_IN |
| related_doc_id | int8 | ✓ | 关联入库单 ID |
| related_doc_no | varchar(32) | - | 入库单号快照 |
| remark | varchar(500) | - | 备注 |

索引：普通索引 `(product_id, warehouse_id, unit)`（余额聚合主路径）、`(related_doc_type, related_doc_id)`（红冲反查原流水）、`(warehouse_id)`、`(create_time)`。流水无业务主键、不可修改不可删除，故不建唯一索引。

### 入库单号

`IN-YYYYMMDD-NNN`（如 `IN-20260923-001`）：`IN` + `-` + 年月日 8 位 + `-` + 3 位日流水（超 999 自然扩位），按天取号，租户内全量唯一含软删行（沿用 ADR-0015 连字符风格与 ADR-0005/0006 取号机制）。新建缺省服务端生成、也可手填（撞号报 `1_030_011_001`）。提供只读 `GET /get-next-no`。取号/查重/重试复用 `InspectionOrderServiceImpl`/`SupplierServiceImpl` 既有机制，仅格式串不同。

### 状态机与过账（ADR-0016）

状态枚举（字典 `lunaerp_stock_in_status`）：`DRAFT` 草稿（缺省）/ `COMPLETED` 已审核 / `CANCELLED` 已取消。**无 ACTIVE**（入库无"执行中"语义，审核是瞬时动作）、无 QC_PENDING（质检归检验单域）、无审批流。

严格守卫：

- 编辑主表/明细/放置：仅 DRAFT；非 DRAFT 报 `1_030_011_002`
- 删除：仅 DRAFT（级联逻辑删除 item + placement）；非 DRAFT 报 `1_030_011_002`
- 审核 `PUT /audit`（权限 `:audit`）：仅 DRAFT → COMPLETED；要求至少一条明细（否则报 `1_030_011_005`）；同事务写流水
- 取消 `PUT /cancel`：
  - DRAFT → CANCELLED：仅作废，**不写流水**，权限 `:update`
  - COMPLETED → CANCELLED：**红冲**，同事务按 `(related_doc_type='STOCK_IN', related_doc_id=本单)` 反查全部原流水逐条插入反向行（txn_type=STOCK_IN_REVERSAL，quantity/amount 取负，库位/单价/批次等快照沿用），原流水保留，权限 `:audit`
  - CANCELLED 为终态：不可再审核/编辑/删除
- 过账流水展开规则：明细有 placement → 每条 placement 一条（数量取 placement.quantity，带 location）；明细无 placement → 该明细一条（数量取 item.quantity，location 空）
- MVP 不做负库存校验（本期无出库模块，除红外冲余额只增不减）；红冲不校验余额是否已被后续业务占用

### 聚合保存与库位分配

- 入库单 create/update 接收主表字段 + items 数组（每行内嵌 placements 数组），service 在事务内：校验关联 → 明细整表逻辑删除后重插（同 ADR-0003 销售合同/胚布/染色/纱线模式）→ placement 同步整表替换 → 重算 totalAmount。
- 明细与放置**没有独立 Controller/权限码**（刻意不沿 ADR-0014 独立子资源：入库是整单装箱录入，非逐卷高频并行）。
- placement.location_id 必须属于主表 warehouse_id 下的库位（且库位存在），否则报 `1_030_011_004`；不要求 placement 匹数/数量合计等于明细（仅前端提示）。
- 删主表级联逻辑删除 items + placements（流水不动；但仅 DRAFT 可删，草稿无流水）。

### Controller 与 API 契约

**入库单** 基础路径 `/lunaerp/stock/stock-in`：

- `GET /page` — pageNo/pageSize/keyword/inType/status/warehouseId/supplierId/inDateStart/inDateEnd；keyword 对 in_no / product_code / product_name / supplier_name / contract_no OR 模糊（LOWER+LIKE）；默认 in_date DESC；PageResult（不含明细）
- `GET /get?id=` — 详情 RespVO（含 items 及其 placements）
- `GET /get-next-no` — 预览下一个入库单号
- `POST /create` — StockInSaveReqVO（主表 + items[].placements）；status 缺省 DRAFT；返回 id
- `PUT /update` — 同结构含 id；仅 DRAFT，否则 1_030_011_002
- `DELETE /delete?id=` — 仅 DRAFT，级联删除 items/placements
- `PUT /audit?id=` — DRAFT → COMPLETED 并过账（`:audit`）
- `PUT /cancel?id=` — DRAFT 作废 或 COMPLETED 红冲 → CANCELLED

**仓库** `/lunaerp/stock/warehouse`：`GET /page`、`GET /get`、`GET /get-next-no`、`GET /list-all`（启用仓库简表，供下拉；支持仅查 ACTIVE）、`POST /create`、`PUT /update`、`DELETE /delete`（有库位或被入库单引用时报 1_030_013_002/003）。

**库位** `/lunaerp/stock/warehouse-location`：`GET /page`（warehouseId 过滤）、`GET /list-by-warehouse?warehouseId=&status=`（启用库位下拉）、`GET /get`、`POST /create`（仓内 code 唯一，重 1_030_013_101）、`PUT /update`、`DELETE /delete`（被 placement 引用报 1_030_013_102）。

**库存余额** `/lunaerp/stock/inventory-balance`：`GET /page` — 按 product_id, unit, warehouse_id 分组 SUM(quantity)（JOIN 产品/仓库仅取展示名，优先用流水快照字段免 JOIN），参数 productKeyword/warehouseId；分页返回聚合行。

**库存流水** `/lunaerp/stock/stock-ledger`：`GET /page`（只读）— productKeyword/warehouseId/locationId/txnType/relatedDocNo/时间区间；create_time DESC。

所有写端点 `@PreAuthorize("@ss.hasPermission('lunaerp:stock:<domain>:<action>')")`，返回 CommonResult/PageResult，VO 带 Swagger 与 jakarta.validation 注解，风格对齐 inspectionorder/supplier。

### 关联校验与快照

create/update 入库单时校验并回填快照：

- warehouseId 必填且为 ACTIVE（不存在报 1_030_011_003），回填 warehouseName
- supplierId（若传）存在，回填 supplierName
- salesContractId（若传）存在，回填 contractNo
- greigeOrderId（若传）存在，回填 greigeOrderNo
- dyeingOrderId（若传）存在（无编号快照）
- greigeTypeId（若传）存在，回填 greigeTypeName
- 每行 item 的 productId 必填且存在，回填 productCode/productName
- 每行 placement 的 locationId 存在且归属本单 warehouseId，回填 locationCode/unit

### 权限码

- 入库单：`lunaerp:stock:stock-in:query` / `:create` / `:update` / `:delete` / `:audit`
- 仓库：`lunaerp:stock:warehouse:query` / `:create` / `:update` / `:delete`
- 库位：`lunaerp:stock:warehouse-location:query` / `:create` / `:update` / `:delete`
- 库存查询：`lunaerp:stock:inventory-balance:query`（余额与流水共用）

### 错误码

`1_030_011_xxx` 入库单（主表/明细/放置共用）：

- `1_030_011_000` STOCK_IN_NOT_EXISTS（入库单不存在）
- `1_030_011_001` STOCK_IN_NO_DUPLICATE（入库单号【{}】已存在）
- `1_030_011_002` STOCK_IN_STATUS_NOT_ALLOWED（当前状态不允许该操作）
- `1_030_011_003` STOCK_IN_REFERENCE_NOT_FOUND（关联的{}不存在或不可用）
- `1_030_011_004` STOCK_IN_LOCATION_NOT_IN_WAREHOUSE（库位【{}】不属于入库仓库【{}】）
- `1_030_011_005` STOCK_IN_ITEM_EMPTY（审核失败：入库单没有明细）

`1_030_012_xxx` 库存台账/余额（本期只读，预留段位，暂不定义具体错误码）。

`1_030_013_xxx` 仓库与库位：

- `1_030_013_000` WAREHOUSE_NOT_EXISTS
- `1_030_013_001` WAREHOUSE_NO_DUPLICATE（仓库编号【{}】已存在）
- `1_030_013_002` WAREHOUSE_HAS_LOCATION（仓库下存在库位，不能删除，请停用）
- `1_030_013_003` WAREHOUSE_REFERENCED_BY_STOCK_IN（仓库已有入库单引用，不能删除，请停用）
- `1_030_013_100` WAREHOUSE_LOCATION_NOT_EXISTS
- `1_030_013_101` WAREHOUSE_LOCATION_CODE_DUPLICATE（库位编码【{}】在同一仓库下已存在）
- `1_030_013_102` WAREHOUSE_LOCATION_IN_USE（库位已被入库放置记录引用，不能删除，请停用）

在 lunaerp 模块 `ErrorCodeConstants` 追加；GLOSSARY.md 段位全表同步更新。

### 字典（3 个）

- `lunaerp_stock_in_status`（dict_type id=2233）：DRAFT 草稿(warning) / COMPLETED 已审核(success) / CANCELLED 已取消(danger)
- `lunaerp_stock_in_type`（dict_type id=2234）：PURCHASE 采购入库(primary) / PRODUCTION 生产入库(success) / RETURN 退货入库(warning) / OTHER 其他入库(info)
- `lunaerp_stock_in_unit`（dict_type id=2235）：KG 千克 / M 米（库存域独立单位字典，不复用 lunaerp_sales_contract_unit）
- dict_data id 3592-3600（紧接检验单 3584-3591 之后）
- 仓库/库位 status（ACTIVE/INACTIVE）与流水 txn_type 不入字典（前者对齐客户/供应商惯例，后者为内部枚举，同 ADR-0012 先例）

### 菜单与路由

SQL 菜单（10300 段，事务内先 DELETE 再 INSERT，可重复执行）：

| ID | 名称 | type | parent | 权限/组件 |
|----|------|------|--------|-----------|
| 10300 | 库存管理 | 1 目录 | 0 | path `/stock`，icon `ep:box`，sort 103 |
| 10310 | 入库管理 | 2 菜单 | 10300 | `lunaerp:stock:stock-in:query`，component `lunaerp-elmui/stock/stock-in/index`，componentName `LunaerpElmuiStockStockIn` |
| 10311 | 入库创建 | 3 | 10310 | `:create` |
| 10312 | 入库修改 | 3 | 10310 | `:update` |
| 10313 | 入库删除 | 3 | 10310 | `:delete` |
| 10314 | 入库审核 | 3 | 10310 | `:audit`（审核 + 红冲取消） |
| 10320 | 库存查询 | 2 菜单 | 10300 | `lunaerp:stock:inventory-balance:query`，component `lunaerp-elmui/stock/inventory-balance/index` |
| 10330 | 仓库管理 | 2 菜单 | 10300 | `lunaerp:stock:warehouse:query`，component `lunaerp-elmui/stock/warehouse/index` |
| 10331-10333 | 仓库 创建/修改/删除 | 3 | 10330 | `lunaerp:stock:warehouse:*` |
| 10334-10336 | 库位 创建/修改/删除 | 3 | 10330 | `lunaerp:stock:warehouse-location:*` |

入库工作台不进菜单：前端静态隐藏路由 `/stock/stock-in/workspace/:id?`（无 id 新建、有 id 查看/编辑），仿检验单/染色单工作台。

### 前端页面结构与交互

- **入库列表页**：关键词 + 类型 + 状态 + 仓库下拉 + 供应商远程 + 日期区间搜索；列：入库单号/单据日期/类型 DictTag/仓库/供应商/金额合计/状态 DictTag/创建人/创建时间/操作（查看、编辑[仅草稿]、删除[仅草稿]、审核/取消按状态与权限显示）；分页；新建按钮。
- **入库工作台**（el-tabs 3 Tab）：
  - 基本信息：类型单选、仓库下拉（必选，切换仓库时清空明细 placement）、供应商远程、合同/胚布单/染色单/胚布品种远程选择、单据日期、备注；操作栏按状态：DRAFT [保存草稿][审核][取消][删除]（审核按 `:audit`、取消按 `:update`、删除按 `:delete` 控权）、COMPLETED [取消（红冲）]（`:audit`，二次确认并提示"将生成红冲流水"）、CANCELLED 全只读。
  - 入库明细：可编辑表格（产品远程搜索带快照、批次、卷号、匹数、数量、单位字典下拉、单价、金额自动算但以服务端为准、备注）；行操作"分配库位"打开 PlacementDialog（库位下拉仅本单仓库 ACTIVE 库位 + 匹数 + 数量 + 备注子表，显示已分配/共 X 匹与数量对比）；明细/放置随整单保存；非草稿态全部只读。
  - 操作日志：占位"开发中"。
- **库存查询页**：Tab1 库存余额（产品关键词 + 仓库筛选；列产品编码/名称/仓库/单位/库存数量；行操作"查看流水"切到 Tab2 并带筛选）；Tab2 库存流水（产品/仓库/收支类型/关联单号/时间区间；数量带正负与颜色，入库绿、红冲红/红）。
- **仓库管理页**：左仓库列表（编号/名称/状态 + 新建/编辑/停用启用/删除）+ 右侧选中仓库的库位列表（编码/名称/排序/状态 + CRUD）；删除被引用时展示后端错误文案。
- API 与类型定义镜像检验单/供应商写法，字典通过 DictTag/useCrudSchemas 渲染。

### SQL 脚本

文件：`luna-srv/sql/postgresql/lunaerp-stock.sql`，参照 `lunaerp-production-inspection-order.sql` 体例：

1. 6 张表 DDL（CREATE TABLE IF NOT EXISTS + 主键 DO 块判存 + 列注释 + 6 个序列 + 全部索引，单号/仓库编号/库位编码唯一索引含软删行）
2. system_menu：10300 目录 + 10310-10314 入库 + 10320 库存查询 + 10330-10336 仓库/库位
3. system_dict_type（2233/2234/2235）+ system_dict_data（3592-3600，共 9 项）

## Testing Decisions

### 测试哲学

只测外部业务行为（Service 的可观察语义），不断言内部调用次数等实现细节；测试命名描述行为。前端不做测试（lunaerp-elmui 先行快速版，ADR-0001）。

### 测试 seam（唯一接缝）

Service 层纯 Mockito 单元测试，3 个测试类，不起 Spring 上下文、不连数据库，Mock 各 Mapper + 产品/供应商/合同/胚布单/染色单/胚布品种/仓库/库位校验依赖：

- `StockInServiceImplTest`
- `WarehouseServiceImplTest`
- `WarehouseLocationServiceImplTest`

不新建 MockMvc/嵌入式 PostgreSQL 基建。库存余额/流水为只读聚合查询，不做单测。Prior art：`InspectionOrderServiceImplTest`、`SupplierServiceImplTest`（取号/删除守卫）、`SalesContractServiceImplTest`（聚合保存）。

### 覆盖的行为

入库单：

1. 创建：status 缺省 DRAFT；in_no 自动生成（IN-YYYYMMDD-NNN 按天取号）；手填撞号报 1_030_011_001；仓库必填且存在；供应商/合同/胚布单/染色单/胚布品种/产品不存在报 1_030_011_003；各类名称快照正确回填。
2. 更新：仅 DRAFT 可更新，非 DRAFT 报 1_030_011_002；明细与放置整体替换；totalAmount 由明细金额重算。
3. 删除：仅 DRAFT 可删；级联逻辑删除 items + placements。
4. 审核：非 DRAFT 报 002；零明细报 1_030_011_005；按 placement 展开写正数流水（含库位）、无 placement 明细写库位为空流水；同一事务。
5. 取消：DRAFT 取消不写流水；COMPLETED 取消按原流水逐条写负数红冲（数量/金额为负、快照沿用）；CANCELLED 再任何操作报 002。
6. placement 库位不属于本单仓库报 1_030_011_004；放置匹数合计不等于明细不报错。
7. 分页/详情/get-next-no 行为。

仓库：

8. 编号自动取号/手填撞号 1_030_013_001；删除时存在库位报 002、被入库单引用报 003；停用后 list-all 不返回。

库位：

9. 同仓库 code 重复报 1_030_013_101（不同仓库可同码）；删除被 placement 引用报 102；list-by-warehouse 仅返回 ACTIVE。

## Out of Scope

- 出库管理、盘点、调拨、移库（InventoryMovementLog）、库存预警/安全库存（InventoryAlert）
- 仓库平面图/2D 画布、库区（WarehouseZone）、行列层坐标、容量管控、库位二维码（ADR-0018 只做两级简版）
- 库位级库存余额报表（本期余额按产品+单位+仓库聚合；库位存量仅在单据放置记录可见）
- 负库存校验与红冲占用检查（本期无出库，余额不会为负）
- 检验单 → 入库自动同步（ADR-0013 后续 issue；本期模块就绪但不实现同步，检验单 :sync 占位按钮维持禁用）
- 纱线采购完成自动入库、"从上游单据带入生成入库单"（关联仅快照、零回写零联动）
- 审批工作流、待质检态（QC_PENDING）、驳回态
- OCR/AI 智能录入、Excel 导入导出、打印模板
- 供应商应付/付款请求、对账状态/财务锁、价格确认、损耗记录
- 批次主数据与批次级库存、胚布批次（GreigeBatch）转可用、卷状态 rollStatus/疵点 defectData
- 多仓库入库（一单一仓库）、跨仓分配
- 初始库存导入（库存账户从 0 开始）；单位换算（KG/M 仅按单位分别聚合）
- 采购退货等出库方向类型（老系统 PURCHASE_RETURN 概念随出库模块再引入）
- 操作日志模块（工作台 Tab 占位）、附件/图片上传、多合同分摊 JSON、动态表单扩展字段
- 并发编辑乐观锁/版本号
- 前端单元/组件测试、REST 集成测试与测试数据库基建
- 正式 UI（views/lunaerp/）版本

## Further Notes

### 已落地文档

- [GLOSSARY.md](../../docs/contexts/stock/GLOSSARY.md) — 新增"库存管理"术语分区（入库单/入库明细/库位分配/仓库/库位/库存台账/库存余额/入库单号/入库类型/入库状态）、stock 模块边界、3 个字典、错误码 1-030-011/012/013 段位
- [ADR-0016](../../docs/adr/0016-stock-in-posting-state-machine.md) — 入库单三态严格过账状态机（审核过账/红冲/分权，刻意不一致现有单据自由切态）
- [ADR-0017](../../docs/adr/0017-universal-stock-ledger.md) — 通用库存流水台账与实时聚合余额（supersedes ADR-0012）
- [ADR-0018](../../docs/adr/0018-warehouse-location-two-level.md) — 仓库/库位两级简版与多库位分配放置
- [ADR-0015](../../docs/adr/0015-inspection-order-no-with-dashes.md) — IN 单号沿用连字符风格
- [ADR-0013](../../docs/adr/0013-inspection-sync-to-stockin-deferred.md) — 追加补记：入库模块已就绪，同步仍为后续 issue

### 实现顺序建议

1. SQL 脚本（6 表 + 序列/索引 + 10300-10336 菜单 + 字典 2233-2235/3592-3600）
2. 仓库 + 库位 DO/Mapper/VO/Service/Controller（含取号、唯一、删除守卫、下拉接口）
3. 入库单 DO×3（主表/明细/放置）+ Mapper + VO 套装 + 错误码常量（011/012/013 段）
4. 台账 DO/Mapper（含余额聚合查询）
5. 入库单 Service（取号、关联校验快照、聚合保存、金额重算、审核过账、红冲、级联删除）+ Controller（8 端点）
6. 余额/流水只读 Controller + Service
7. ServiceImpl 纯 Mockito 单元测试（3 个测试类）
8. 前端 API/TS 类型（5 组）+ dict + 隐藏工作台路由
9. 仓库管理页 → 入库列表页 → 入库工作台（含 PlacementDialog）→ 库存查询页

### 参考

- 旧系统模型：`linaagi/server/prisma/schema.prisma`（StockIn L3564 / StockInItem L3684 / WarehouseMap L3735 / WarehouseZone L3767 / WarehouseLocation L3798 / InventoryPlacement L3839 / InventoryMovementLog L3880）
- 旧系统后端：`linaagi/server/src/routes/stock-in.ts`、`server/src/services/stock-in-effects.service.ts`（副作用本期全部不做）、`server/src/lib/stockin-scope.ts`（类型口径）
- 旧系统前端：`linaagi/web/app/staff/stock-in/manual/`、`web/app/staff/components/stock-in-suite/`
- 新系统后端模板：`production/inspectionorder`（取号/状态机/工作台模式）、`baseinfo/supplier`（取号/子资源删除守卫）、`business/salescontract`（聚合保存 ADR-0003）
- 新系统前端模板：`views/lunaerp-elmui/production/inspection-order/`（列表 + 工作台多 Tab）、`baseinfo/supplier/`（主从档案页）
- SQL 模板：`lunaerp-production-inspection-order.sql`（唯一索引含软删行 + 菜单/字典体例）
