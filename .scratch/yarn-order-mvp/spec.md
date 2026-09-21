# 生产管理 - 纱线采购 MVP（材质/规格主数据 + 配比BOM + 采购单 + 核心测算流）

Status: ready-for-agent

## Problem Statement

业务侧需要在新 Luna ERP 系统里管理"生产管理-纱线采购"功能。核心业务流是：**勾选胚布指示单 → 后端按纱线配比 BOM 测算需求量 → 生成采购明细 → 保存下达 → 完成**。（注：纱线库存台账功能已于 2026-09-21 撤销回退，暂不开发）。

旧系统（linaagi，Prisma + Next.js/React）已有完整实现：
- 纱线配比存在 `Product.yarnSpecs` JSON 字段（数组：`{yarnSpec:'32S 棉', spinMethod, ratio:49, remark, price}`），胚布单 `GreigeInstruction` 也有 `yarnSpecs` 快照
- 测算在前端跑（`web/lib/yarn-purchase-calc.ts` `buildPurchaseRows`）：`baseQty = Σ(成品量 × ratio/100)`，`requiredQty = baseQty × (1 + loss/100)`，`purchaseQty = max(0, requiredQty - stockQty)`
- 库存走 `StockLedger` 流水台账（原规划移植，后于 2026-09-21 撤销回退）
- 入库是独立"纱线入库单"流程触发 STOCK_IN 台账

新系统（Spring Boot + MyBatis-Plus + PostgreSQL + Vue3 + Element Plus + lunaerp-elmui）已有：胚布指示单（production-greigeorder 模块 + `lunaerp_production_greige_order` 表，无 product_id 强关联、无 yarn_specs 字段）、产品档案（baseinfo-product 模块，含 yarn_specs 字段但已被本功能废弃不用）。

业务侧希望：
1. 在新栈下完整复刻核心业务流，跑通"勾选胚布指示单 → AI 按配比测算 → 生成采购明细 → 下达 → 完成"
2. 配比 BOM 独立成表（不依赖产品档案 yarn_specs），按产品档案挂配比
3. 纱线库存台账（经排查设计不完善，已于 2026-09-21 撤销回退，暂不开发；在库量缺省 0）
4. 纱线档案做管理页（材质+规格两级主数据 + 参考价）
5. 采购单多供应商挂在明细行，明细保留三量字段（需求量/在库量/实采量），无 line_no 行号
6. 采购单完成（原自动写 STOCK_IN 台账已取消）

## Solution

在 luna-srv 的 `lunasrv-module-lunaerp` 模块 `production` 子包下新增 4 个并列子包：

- `production.yarntype` — 纱线材质 CRUD（无字典，seed 表）
- `production.yarnspec` — 纱线规格 CRUD + cascade-options 两级级联数据源
- `production.yarnrecipebom` — 配比 BOM 主从表 CRUD + get-by-product-id
- `production.yarnorder` — 采购单主从表 CRUD + 状态操作 + calculate 测算接口（原 stockledger 已撤销回退）

在 luna-ui 的 `views/lunaerp-elmui/production/` 下新增 3 个视图：`yarn-master/`（材质+规格合并管理页，两级级联带搜索）、`yarn-recipe-bom/`（配比 BOM 管理页，按产品挂配比）、`yarn-order/`（采购单列表+编辑页 + 核心流程交互）。

配套 PostgreSQL 脚本（拆 2 个文件）+ 修改 1 个既有文件（胚布单加 product_id）+ 菜单（10230/10240/10250 ID 段）+ 1 个字典（2230 lunaerp_yarn_order_status）+ 错误码追加（1-030-008/009 段，已落 ErrorCodeConstants.java）。

### 核心设计决策（已确认）

| 决策点 | 选择 | ADR |
|--------|------|-----|
| 配比挂载点 | 新建独立 `yarn_recipe_bom` 表，product_id 强关联产品档案 | ADR-0011 |
| 产品档案 yarn_specs 字段 | 废弃不用，配比权威源是 yarn_recipe_bom 表 | ADR-0011 |
| 胚布单加 product_id 字段 | 是，强关联产品档案（用于测算时查配比 BOM） | ADR-0011 |
| 库存表设计 | 暂不开发（2026-09-21 已回退撤销） | ADR-0012（deprecated） |
| 库存表去库位字段 | 暂不开发（2026-09-21 已回退撤销） | ADR-0012（deprecated） |
| 测算口径 | 沿用参考系统 - 百分比数字（49=49%, 5=5%）+ 乘法公式 `requiredQty = baseQty × (1 + loss/100)`，stockQty 缺省 0 | ADR-0010 |
| 测算接口 | 后端 `POST /lunaerp/production/yarn-order/calculate`（前端只展示） | ADR-0010 |
| 入库来源 | 暂不开发（2026-09-21 已回退撤销） | ADR-0012（deprecated） |
| 多供应商挂载 | 明细行挂 supplier_id+公司名快照，主表不放供应商 | （用户决策） |
| 明细三量字段 | required_qty/stock_qty/purchase_qty 三量字段全保留（stock_qty 缺省 0） | （用户决策） |
| 金额重算 | 服务端权威重算 total_amount = unit_price × purchase_qty | （用户决策） |
| 状态机 | 四态 DRAFT/ACTIVE/COMPLETED/CANCELLED，仿染色单 ADR-0008 | ADR-0010 |
| 历史价回填 | 选规格后自动带出（最近已下达采购单明细 unit_price > yarn_spec.ref_price 兜底），可手改 | （用户决策） |
| 明细无 line_no | 不设行号字段，按 yarn_spec_id 排序展示 | （用户决策） |
| 命名 | yarnorder（不使用 yarnpurchase） | （用户决策） |

## User Stories

### 纱线主数据管理（材质+规格）

1. 作为生产计划员，我想在"生产管理 → 纱线档案"菜单看到纱线材质列表（材质名、排序、状态），并能新建/编辑/停用材质，以便维护材质分类。
2. 作为生产计划员，我想在材质列表选中某行后，下方展示该材质下的规格列表（规格名、纺纱工艺、光泽、标准名、参考单价、状态），以便维护具体规格。
3. 作为生产计划员，我想新建规格时选好材质后，规格名/标准名/参考单价/纺纱工艺/光泽等字段可填，标准名在 (材质 + 标准名) 维度租户内唯一（撞名报错），以便避免重复建档。
4. 作为生产计划员，我想在采购单编辑页选纱线规格时，能用两级级联下拉（先选材质再选规格），下拉带搜索框支持模糊搜索规格名，以便快速定位。

### 纱线配比 BOM 管理

5. 作为生产工艺员，我想在"生产管理 → 纱线配比BOM"菜单看到所有产品的配比 BOM 列表（产品编码、产品名称、明细行数、状态），并能新建/编辑/删除配比 BOM。
6. 作为生产工艺员，我想新建配比 BOM 时必选产品档案（远程搜索），选中后保存产品 ID + 产品编码 + 产品名称快照，以便按产品挂配比。
7. 作为生产工艺员，我想在配比 BOM 编辑页能增删明细行，每行选纱线规格（带材质级联）+ 填 ratio（百分比数字 49=49%）+ spin_method（纺纱工艺，可空）+ remark + ref_price（参考价，可空时取规格 ref_price 兜底），以便完整描述该产品的纱线配比。
8. 作为生产工艺员，我想一份配比 BOM 的 ratio 之和不强制等于 100（业务上可能存在 5% 损耗余量等情况），以便灵活建模。

### 胚布指示单加 product_id 关联

9. 作为生产计划员，我想在胚布单编辑页选产品档案（远程搜索），选中后保存 product_id + product_code + product_name 快照，以便后续测算时按 product_id 查配比 BOM。
10. 作为生产计划员，我想既有的胚布单（无 product_id）也能补填 product_id，以便统一接入测算流程。

### 纱线库存台账

11. 作为后端服务，我想在采购单完成（ACTIVE→COMPLETED）时自动按明细的 yarn_spec_id 拆分写入 STOCK_IN 台账（related_doc_type='yarn_order'、related_doc_id=采购单 ID、quantity=purchase_qty），以便记录纱线入库流水。
12. 作为后端服务，我想暴露 `GET /lunaerp/production/yarn-stock/balance?yarnSpecId=` 余额查询接口，返回某纱线规格的当前在库量（= SUM(quantity) WHERE yarn_spec_id AND type='STOCK_IN'，MVP 简化只汇总 STOCK_IN），以便测算时取在库量。
13. 作为后端服务，我想 StockLedger 仅作为流水台账（append-only），不允许从外部 API 修改/删除单条台账记录（无 CRUD 端点），以便保证库存流水不可篡改。

### 纱线采购单列表

14. 作为生产计划员，我想在"生产管理 → 纱线采购"菜单看到采购单列表（采购单号、单据日期、交货日期、状态、关联胚布单号（多单号逗号拼接）、明细数、总金额、创建人、创建时间），以便掌握所有采购单。
15. 作为生产计划员，我想在搜索框输入关键词同时匹配采购单号 + 关联胚布单号，以便只记其一也能找到采购单。
16. 作为生产计划员，我想按采购单状态（草稿/进行中/已完成/已取消）筛选，以便聚焦在执行的采购单。
17. 作为生产计划员，我想按单据日期区间、交货日期区间筛选，以便定位特定时间段的采购单。
18. 作为生产计划员，我想列表默认按单据日期倒序、分页展示，以便最新采购单在最前。
19. 作为生产计划员，我想列表每行有查看/编辑/删除操作，删除前弹二次确认。
20. 作为生产计划员，我想状态列用 DictTag 渲染（草稿 warning、进行中 success、已完成 info、已取消 danger）。

### 纱线采购单编辑（核心流程）

21. 作为生产计划员，我想点"新建采购单"进入编辑页，先选胚布指示单（多选，远程搜索，按产品分组），选完后页面展示所选胚布单列表（胚布单号、产品、成品量、损耗预设），以便核对需求源头。
22. 作为生产计划员，我想选完胚布单后填损耗率 loss_rate（百分比数字，缺省取胚布单预设损耗的最大值或 5），然后点"AI 测算"按钮调用后端 `/yarn-order/calculate`，后端按所选胚布单的 product_id 查配比 BOM 拿配比、按 yarn_spec_id 合并多单需求、按 StockLedger 拿在库量，返回采购明细行（含三量、ratio、base_qty、ref_unit_price 历史价），前端展示在明细表格中。
23. 作为生产计划员，我想测算结果明细行可以编辑：调供应商（每行独立选，远程搜索供应商档案）、调单价（默认带出 ref_unit_price，可手改）、调实采量（默认等于测算 purchaseQty，可手改增减），以便人工修正。
24. 作为生产计划员，我想单价/实采量改动时前端实时重算该行 total_amount（unit_price × purchase_qty），保存时服务端权威重算并覆盖前端值，以便金额权威。
25. 作为生产计划员，我想明细表格底部展示"合计实采量 / 合计金额"汇总行，以便一眼看到采购规模。
26. 作为生产计划员，我想新建保存时默认存为草稿（DRAFT），以便反复修改后再正式下达。
27. 作为生产计划员，我想新建时可以直接点"下达"保存为进行中（ACTIVE），以便确认无误的采购单直接生效。
28. 作为生产计划员，我想编辑采购单时仅草稿态可编辑，进行中/已完成/已取消态只读，防止已下达的指令被篡改。
29. 作为生产计划员，我想删除采购单时仅草稿态可删，进行中态不可删（只能取消），已完成/已取消不可删。

### 状态操作

30. 作为生产计划员，我想在草稿态点"下达"将采购单转为进行中，以便正式把指令发给纱线厂。
31. 作为生产计划员，我想在进行中态点"完成"将采购单转为已完成，**并自动写一条 STOCK_IN 台账**（按明细 yarn_spec_id 拆分多行），以便记录纱线入库。
32. 作为生产计划员，我想在进行中态点"取消"将采购单转为已取消，以便作废不再执行的指令。
33. 作为生产计划员，我想非法状态流转（如已完成再下达、已取消再完成）得到明确报错。

### 查看与删除

34. 作为生产计划员，我想查看采购单时看到主表全部字段、关联胚布单列表、明细列表（只读），以便核对采购单内容。
35. 作为生产计划员，我想删除采购单后主表与其明细一并逻辑删除、列表不再显示；**已完成态的采购单禁止删除**（已写入台账不可逆）。
36. 作为生产计划员，我想通过浏览器直接访问编辑页 URL（如 /production/yarn-order/workspace/123）打开特定采购单，以便收藏或分享链接。

### 管理与分权

37. 作为系统管理员，我想执行 SQL 脚本即可建表、建菜单/按钮、初始化字典与材质 seed 数据，且脚本可重复执行，以便部署无手工配置。
38. 作为系统管理员，我想采购单与档案各有独立权限码（query/create/update/delete），以便按角色分权。
39. 作为系统管理员，我想采购单状态来自系统字典 `lunaerp_yarn_order_status` 并用 DictTag 渲染。
40. 作为开发，我想所有表 DO 继承 TenantBaseDO，多租户隔离由框架自动处理。
41. 作为开发，我想配比 BOM 与采购单明细均随主表聚合保存（create/update 整体替换明细、delete 级联逻辑删除），以便与合同明细/胚布单颜色分配/染色单明细模式一致。
42. 作为开发，我想关联字段（胚布单、纱线规格、供应商、产品）均存 ID + 名称快照，以便列表展示无需 JOIN、对方单据删除/改名后本单信息不丢。

## Implementation Decisions

### 模块与包路径

- 后端模块：`lunasrv-module-lunaerp`（已存在，新增子包）
- 后端包：
  - `com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.production.yarntype` — 材质 CRUD，无 VO 子包
  - `com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.production.yarnspec` — 规格 CRUD + cascade-options
  - `com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.production.yarnrecipebom` — 配比 BOM 主从表 CRUD + get-by-product-id（明细 DO/Mapper 在同包或 `.item` 子包，无独立 Controller/Service）
  - `com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.production.stockledger` — 余额查询，无 CRUD
  - `com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.production.yarnorder` — 采购单主从表 CRUD + 状态操作 + calculate 测算接口 + 完成时写台账（明细 DO/Mapper 在同包或 `.item` 子包，无独立 Controller/Service）
- 修改：`production.greigeorder` 加 productId 字段到 GreigeOrderDO/GreigeOrderSaveReqVO/GreigeOrderRespVO + 前端编辑页加产品档案远程下拉
- 前端视图：`luna-ui/src/views/lunaerp-elmui/production/{yarn-master,yarn-recipe-bom,yarn-order}/`
- 前端 API：`luna-ui/src/api/lunaerp-elmui/production/{yarn-master,yarn-recipe-bom,yarn-order,yarn-stock}/index.ts`
- 沿用 ADR-0001：落在 `lunaerp-elmui/` 先行快速版目录

### 数据模型（7 张表）

#### 表 1：`lunaerp_production_yarn_type` 纱线材质主表

继承 TenantBaseDO（id/creator/create_time/updater/update_time/deleted/tenant_id）。

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| name | varchar(64) | ✓ | 材质名（棉/涤/人棉/氨纶/锦纶/粘胶/混纺），租户内唯一 |
| sort | int4 | - | 排序值，缺省 0 |
| status | varchar(16) | ✓ | ACTIVE/INACTIVE，缺省 ACTIVE |

索引：唯一索引 `(tenant_id, name)`（不含 deleted，软删不释放材质名）、普通索引 `(status)`。

#### 表 2：`lunaerp_production_yarn_spec` 纱线规格子表

继承 TenantBaseDO。

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| yarn_type_id | int8 | ✓ | 所属材质 ID（lunaerp_production_yarn_type.id） |
| spec_name | varchar(128) | ✓ | 规格名（如"32S 棉"），展示用 |
| standard_name | varchar(128) | ✓ | 标准名，租户内+材质内唯一 |
| spin_method | varchar(64) | - | 纺纱工艺（自由文本：环锭纺/转杯纺/喷气纺等） |
| luster | varchar(64) | - | 光泽（自由文本：半光/全光/消光等） |
| ref_price | numeric(18,4) | - | 参考单价（金额），测算历史价回填兜底 |
| remark | varchar(256) | - | 备注 |
| status | varchar(16) | ✓ | ACTIVE/INACTIVE，缺省 ACTIVE |

索引：唯一索引 `(tenant_id, yarn_type_id, standard_name)`、普通索引 `(yarn_type_id)`、`(status)`。

#### 表 3：`lunaerp_production_yarn_recipe_bom` 配比 BOM 主表

继承 TenantBaseDO。

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| product_id | int8 | ✓ | 强关联产品档案（lunaerp_baseinfo_product.id），租户内唯一（一产品一 BOM） |
| product_code | varchar(64) | - | 产品编码快照 |
| product_name | varchar(128) | - | 产品名称快照 |
| remark | varchar(256) | - | 备注 |
| status | varchar(16) | ✓ | ACTIVE/INACTIVE，缺省 ACTIVE |

索引：唯一索引 `(tenant_id, product_id)`（一产品一 BOM，软删后不释放）、普通索引 `(status)`。

#### 表 4：`lunaerp_production_yarn_recipe_bom_item` 配比 BOM 明细表

继承 TenantBaseDO。

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| recipe_bom_id | int8 | ✓ | 所属配比 BOM ID |
| yarn_type_id | int8 | ✓ | 纱线材质 ID（与 yarn_spec_id 对齐） |
| yarn_spec_id | int8 | ✓ | 纱线规格 ID（lunaerp_production_yarn_spec.id） |
| ratio | numeric(6,2) | ✓ | 配比百分比数字（49 = 49%），不强制和为 100 |
| spin_method | varchar(64) | - | 纺纱工艺快照（可空，覆盖规格默认值） |
| remark | varchar(256) | - | 备注 |
| ref_price | numeric(18,4) | - | 参考价快照（可空时测算兜底取规格 ref_price） |

索引：普通索引 `(recipe_bom_id)`。

明细不作为独立资源（同 ADR-0003 模式）：随配比 BOM 整体保存、级联逻辑删除。无 line_no（与采购明细一致决策）。

#### 表 5：`lunaerp_production_stock_ledger` 纱线库存台账（已于 2026-09-21 撤销回退，暂不开发）

> [!NOTE]
> 经排查，纱线库存台账功能设计不完善，过早开发。2026-09-21 已回退该表及相关前后端程序（对应 Issue 04 标记 wontfix，ADR-0012 标记 deprecated）。本表 DDL 已从 SQL 脚本中移除。

（原设计保留归档供未来参考）：

继承 TenantBaseDO。

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| yarn_spec_id | int8 | ✓ | 纱线规格 ID |
| type | varchar(16) | ✓ | 枚举 STOCK_IN/STOCK_OUT/PROCESS_IN/PROCESS_OUT（MVP 实际只用 STOCK_IN，其余占位） |
| quantity | numeric(18,2) | ✓ | 数量（STOCK_IN 为正、STOCK_OUT 为正记负或符号约定后补；MVP 简化为始终正数 + type 区分方向） |
| related_doc_type | varchar(32) | - | 关联单据类型（如 'yarn_order'） |
| related_doc_id | int8 | - | 关联单据 ID（如采购单 ID） |
| batch_no | varchar(64) | - | 批次号 |
| remark | varchar(256) | - | 备注 |

索引：普通索引 `(yarn_spec_id, type)`、`(related_doc_type, related_doc_id)`。

**无 source_location/target_location 两列**（ADR-0012）。无独立 CRUD 端点，仅由 Service 内部调用写入（采购单完成自动写）+ 1 个余额查询接口。

#### 表 6：`lunaerp_production_yarn_order` 纱线采购单主表

继承 TenantBaseDO。

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| order_no | varchar(32) | ✓ | 采购单号（YP+年月日+3位日流水，按天取号，租户内全量唯一含已删除行，见 ADR-0006 模式） |
| source_instructions | text | - | 关联胚布指示单 ID 数组（JSON 字符串，如 "[123,456]"），多单驱动 |
| order_date | date | ✓ | 单据日期，缺省今天 |
| delivery_date | date | - | 交货日期 |
| loss_rate | numeric(6,2) | - | 损耗率百分比数字（5 = 5%），缺省 0 |
| status | varchar(16) | ✓ | 字典 lunaerp_yarn_order_status，缺省 DRAFT |
| remark | varchar(1000) | - | 备注 |

索引：唯一索引 `(tenant_id, order_no)`（含软删行）、普通索引 `(status)`、`(order_date)`。

**主表不放供应商**（用户决策：多供应商挂在明细行）。source_instructions 用 text 存 JSON 数组（不建独立中间表，胚布单 ID 数量有限，JSON 查询足够）。

#### 表 7：`lunaerp_production_yarn_order_item` 纱线采购明细表

继承 TenantBaseDO。

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| order_id | int8 | ✓ | 所属采购单 ID |
| yarn_type_id | int8 | ✓ | 纱线材质 ID |
| yarn_spec_id | int8 | ✓ | 纱线规格 ID |
| supplier_id | int8 | - | 供应商 ID（可空，用户可在测算后逐步补） |
| supplier_name | varchar(128) | - | 供应商公司名快照 |
| ratio | numeric(6,2) | - | 配比百分比数字快照（49 = 49%，从 BOM 复制） |
| loss_rate | numeric(6,2) | - | 损耗率百分比数字快照（从主单复制，可逐行覆盖） |
| base_qty | numeric(18,2) | - | 基础需求量（Σ 胚布单成品量 × ratio/100） |
| required_qty | numeric(18,2) | - | 含损耗需求量 = base_qty × (1 + loss_rate/100) |
| stock_qty | numeric(18,2) | - | 在库量快照（测算时的 StockLedger 余额） |
| purchase_qty | numeric(18,2) | - | 实采量 = max(0, required_qty - stock_qty)，可手改 |
| unit_price | numeric(18,4) | - | 单价（默认 ref_unit_price，可手改） |
| total_amount | numeric(18,2) | - | 金额 = unit_price × purchase_qty（服务端权威重算） |
| ref_unit_price | numeric(18,4) | - | 历史价回填（最近已下达采购单 unit_price > 规格 ref_price 兜底） |
| remark | varchar(256) | - | 备注 |

索引：普通索引 `(order_id)`、`(yarn_spec_id)`。

**无 line_no 行号字段**（用户决策）。明细随主表聚合保存、级联逻辑删除。

### 状态机与状态操作（ADR-0010）

状态枚举：`DRAFT` 草稿（缺省）/ `ACTIVE` 进行中 / `COMPLETED` 已完成 / `CANCELLED` 已取消。

状态约束：
- 编辑：仅 DRAFT 可编辑，ACTIVE/COMPLETED/CANCELLED 只读
- 删除：仅 DRAFT 可删；ACTIVE 不可删，只能"取消"；COMPLETED/CANCELLED 不可删（COMPLETED 已写台账不可逆）
- 流转：DRAFT → ACTIVE（下达 effect）/ ACTIVE → COMPLETED（完成 complete，自动写 STOCK_IN 台账）/ ACTIVE → CANCELLED（取消 cancel）
- 非法流转（如 ACTIVE→DRAFT、COMPLETED→ACTIVE、CANCELLED→ACTIVE）报 `1_030_009_002`

新建时通过 status 字段控制：传 DRAFT 存草稿，传 ACTIVE 直接下达。

### 聚合保存（同 ADR-0003 模式）

- `YarnOrderSaveReqVO` 内嵌 `items: List<YarnOrderItemSaveReqVO>`，无明细独立端点。
- create/update 均在一个事务内：保存主表 → 按 order_id 删除旧明细（逻辑删除）→ 插入本次提交的全部明细。
- 零明细合法（草稿态允许先建头后补明细）。
- 删除采购单：逻辑删除主表，并按 order_id 逻辑删除全部明细。
- `get` 详情嵌套返回 items；`page` 列表不返回明细。
- 配比 BOM 同模式：`YarnRecipeBomSaveReqVO` 内嵌 `items: List<YarnRecipeBomItemSaveReqVO>`。

### 测算接口 POST /yarn-order/calculate 契约

**入参** `YarnOrderCalculateReqVO`：

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| greigeOrderIds | List<Long> | ✓ | 勾选的胚布指示单 ID 列表（至少 1 个） |
| lossRate | BigDecimal | - | 损耗率百分比数字（5 = 5%），缺省 0 |

**算法**（服务端权威，前端只展示）：

```
1. 按 greigeOrderIds 查胚布单（GreigeOrderDO），收集 (productId, finishedQty) 列表
2. 按 productId 查 yarn_recipe_bom + 明细，拿到每个产品的配比行
3. 按 yarn_spec_id 合并多单需求：
   for each greigeOrder:
     for each bomItem in product's bom:
       baseQty[yarnSpecId] += greigeOrder.finishedQty × bomItem.ratio / 100
4. 按 yarn_spec_id 查 StockLedger SUM(quantity) WHERE type='STOCK_IN'，拿到 stockQty[yarnSpecId]
5. 算 requiredQty = baseQty × (1 + lossRate / 100)
6. 算 purchaseQty = max(0, requiredQty - stockQty)
7. 按 yarn_spec_id 查最近一条已下达（ACTIVE/COMPLETED）采购单明细的 unit_price，拿到历史价；无则取 yarn_spec.ref_price 兜底
8. 输出 items 列表
```

**出参** `YarnOrderCalculateRespVO`：

```java
{
  items: [
    {
      yarnTypeId, yarnSpecId, yarnSpecName,
      supplierId: null,           // 测算时不带供应商，由用户在明细行手选
      supplierName: null,
      ratio,                       // 百分比数字，从 BOM 复制
      baseQty,
      lossRate,                    // 入参 lossRate
      requiredQty,
      stockQty,
      purchaseQty,
      refUnitPrice,                // 历史价回填
      sourceGreigeOrderIds: [],    // 该规格涉及的胚布单 ID 列表
      sourceGreigeOrderNos: []     // 该规格涉及的胚布单号列表
    }
  ],
  sourceGreigeOrders: [            // 选中胚布单的展示快照
    { id, orderNo, productId, productCode, productName, finishedQty, suggestedLoss }
  ]
}
```

**校验**：
- greigeOrderIds 不能为空（至少 1 个）
- 每个胚布单必须存在（不存在报 `1_030_006_000`）
- 每个胚布单必须有 product_id（无则报 `1_030_009_003` "关联的产品档案不存在"，提示先在胚布单补填产品档案）
- 每个胚布单的 product_id 必须有对应的 yarn_recipe_bom（无则报 `1_030_008_004` "配比BOM不存在"，提示先建配比）

### 入库写入点（采购单完成）

采购单 ACTIVE→COMPLETED 时（`complete()` 方法内）：

```
1. 校验状态流转合法（ACTIVE → COMPLETED）
2. 更新主表 status = COMPLETED
3. 按采购单 ID 查全部明细（按 yarn_spec_id 聚合求和 purchase_qty，应对同规格多行情况）
4. 为每个 yarn_spec_id 写一条 StockLedger：
   type = 'STOCK_IN'
   quantity = sum(purchase_qty)
   related_doc_type = 'yarn_order'
   related_doc_id = 采购单 ID
   batch_no = 采购单号（便于反查）
   remark = '采购单完成自动入库'
5. 整个流程在事务内
```

**幂等性**：complete 操作前先检查是否已存在 related_doc_type='yarn_order' AND related_doc_id=采购单 ID 的台账记录，存在则跳过写入（防止重复入库）。但状态流转本身仍按状态校验报错——已 COMPLETED 不能再 complete。

### Controller 与 API 契约

所有路径前缀 `/lunaerp/production`，所有端点 `@PreAuthorize("@ss.hasPermission('lunaerp:production:<子域>:<action>')")`。

#### yarn-type（材质 CRUD）

- `GET /yarn-type/page` — 分页查材质
- `GET /yarn-type/get?id=` — 详情
- `POST /yarn-type/create` — 新建
- `PUT /yarn-type/update` — 编辑
- `DELETE /yarn-type/delete?id=` — 删除（停用替代删除，逻辑删）

权限码：`lunaerp:production:yarn-type:query/create/update/delete`。

#### yarn-spec（规格 CRUD + 级联）

- `GET /yarn-spec/page` — 分页查规格（支持 yarnTypeId 过滤）
- `GET /yarn-spec/get?id=` — 详情
- `POST /yarn-spec/create` — 新建
- `PUT /yarn-spec/update` — 编辑
- `DELETE /yarn-spec/delete?id=` — 删除
- **`GET /yarn-spec/cascade-options`** — 两级级联下拉数据源，返回 `[{yarnTypeId, yarnTypeName, specs: [{id, specName, standardName, refPrice, spinMethod, luster}]}]`，支持 `keyword` 参数做规格名模糊过滤

权限码：`lunaerp:production:yarn-spec:query/create/update/delete`。cascade-options 复用 query 权限。

#### yarn-recipe-bom（配比 BOM 主从表 CRUD）

- `GET /yarn-recipe-bom/page` — 分页查 BOM（支持 productId/productCode 过滤）
- `GET /yarn-recipe-bom/get?id=` — 详情（内嵌 items）
- `POST /yarn-recipe-bom/create` — 新建（含 items，聚合保存）
- `PUT /yarn-recipe-bom/update` — 编辑（含 items，整体替换）
- `DELETE /yarn-recipe-bom/delete?id=` — 删除（级联逻辑删除明细）
- **`GET /yarn-recipe-bom/get-by-product-id?productId=`** — 按产品 ID 查配比 BOM（用于测算时调用），返回 BOM + items

权限码：`lunaerp:production:yarn-recipe-bom:query/create/update/delete`。

#### yarn-stock（库存台账余额查询，已于 2026-09-21 撤销回退，暂不开发）

> [!NOTE]
> 经排查，纱线库存台账功能设计不完善，过早开发。2026-09-21 已回退撤销相关接口与实现（对应 Issue 04 标记 wontfix）。

- ~~`GET /yarn-stock/balance?yarnSpecId=`~~
- ~~`GET /yarn-stock/balances?yarnSpecIds=1,2,3`~~

#### yarn-order（采购单主从表 CRUD + 状态操作 + 测算）

- `GET /yarn-order/page` — 分页查采购单
- `GET /yarn-order/get?id=` — 详情（内嵌 items + sourceGreigeOrders 展示快照）
- `POST /yarn-order/create` — 新建（含 items）
- `PUT /yarn-order/update` — 编辑（仅 DRAFT 可改）
- `DELETE /yarn-order/delete?id=` — 删除（仅 DRAFT 可删）
- `PUT /yarn-order/effect?id=` — DRAFT → ACTIVE
- `PUT /yarn-order/complete?id=` — ACTIVE → COMPLETED，状态流转（注：原自动写 STOCK_IN 台账设计已于 2026-09-21 回退取消）
- `PUT /yarn-order/cancel?id=` — ACTIVE → CANCELLED
- **`POST /yarn-order/calculate`** — 核心测算接口（入参 YarnOrderCalculateReqVO，出参 YarnOrderCalculateRespVO）
- `GET /yarn-order/get-next-no` — 预览下一个采购单号（仿供应商/胚布单模式，可选实现）

权限码：`lunaerp:production:yarn-order:query/create/update/delete`。状态操作复用 update 权限。calculate 复用 create 权限（因为通常是新建流程的前置步骤）。

### 关联校验与快照

- 采购单创建/更新时：source_instructions 中的胚布单 ID 必须存在（不存在报 `1_030_009_003`）
- 采购明细的 yarn_spec_id 必须存在（不存在报 `1_030_009_003`）
- 采购明细的 supplier_id（若传）必须存在（不存在报 `1_030_009_003`），保存 supplier_name 快照
- 采购单号撞号报 `1_030_009_001`
- 配比 BOM 创建时：product_id 必须存在（不存在报 `1_030_008_004` 段相关错误），product_id 租户内唯一（已存在 BOM 报错或改为更新）
- 配比 BOM 明细的 yarn_spec_id 必须存在（不存在报 `1_030_008_002`）
- 规格创建时：yarn_type_id 必须存在（不存在报 `1_030_008_000`），standard_name 在 (yarn_type_id, standard_name) 维度租户内唯一（撞名报 `1_030_008_003`）

### 权限码

- `lunaerp:production:yarn-type:query` / `:create` / `:update` / `:delete`
- `lunaerp:production:yarn-spec:query` / `:create` / `:update` / `:delete`
- `lunaerp:production:yarn-recipe-bom:query` / `:create` / `:update` / `:delete`
- `lunaerp:production:yarn-order:query` / `:create` / `:update` / `:delete`
- ~~`lunaerp:production:yarn-stock:query`~~（已随 Issue 04 回退移除）

### 错误码（已落 ErrorCodeConstants.java）

- `1_030_008_000` — YARN_TYPE_NOT_EXISTS（纱线材质不存在）
- `1_030_008_001` — YARN_TYPE_NAME_DUPLICATE（纱线材质名【{}】已存在）
- `1_030_008_002` — YARN_SPEC_NOT_EXISTS（纱线规格不存在）
- `1_030_008_003` — YARN_SPEC_NAME_DUPLICATE（纱线规格【{}】已存在）
- `1_030_008_004` — YARN_RECIPE_BOM_NOT_EXISTS（纱线配比BOM不存在）
- `1_030_008_005` — YARN_RECIPE_BOM_ITEM_NOT_EXISTS（纱线配比BOM明细不存在）
- ~~`1_030_008_006` — STOCK_LEDGER_NOT_EXISTS（库存台账记录不存在）~~（已于 2026-09-21 随台账回退移除）
- `1_030_009_000` — YARN_ORDER_NOT_EXISTS（纱线采购单不存在）
- `1_030_009_001` — YARN_ORDER_NO_DUPLICATE（纱线采购单号【{}】已存在）
- `1_030_009_002` — YARN_ORDER_STATUS_NOT_ALLOWED（当前状态不允许该操作）
- `1_030_009_003` — YARN_ORDER_REFERENCE_NOT_FOUND（关联的{}不存在或不可用）

### 字典（1 个）

- `lunaerp_yarn_order_status`：DRAFT 草稿(warning) / ACTIVE 进行中(success) / COMPLETED 已完成(info) / CANCELLED 已取消(danger)

材质不入字典（seed 表）。规格的 spin_method/luster 自由文本。原 StockLedger type 枚举已随台账回退移除。

### 菜单与路由

SQL 菜单（ID 段 10230/10240/10250，可重复执行，先 DELETE 再 INSERT，父菜单 10200 已由胚布单脚本创建）：

- **10230 `纱线采购` 菜单**（type=2，parent=10200，component `lunaerp-elmui/production/yarn-order/index`，componentName `LunaerpElmuiProductionYarnOrder`，承载 query 权限）
- 10231/10232/10233 创建/修改/删除按钮（type=3）
- **10240 `纱线档案` 菜单**（type=2，parent=10200，component `lunaerp-elmui/production/yarn-master/index`，componentName `LunaerpElmuiProductionYarnMaster`）
- 10241/10242/10243/10244 创建/修改/删除/查询按钮（含规格操作复用同菜单权限码）
- **10250 `纱线配比BOM` 菜单**（type=2，parent=10200，component `lunaerp-elmui/production/yarn-recipe-bom/index`，componentName `LunaerpElmuiProductionYarnRecipeBom`）
- 10251/10252/10253 创建/修改/删除按钮

工作台不进菜单：参照供应商/染色单工作台模式在前端静态隐藏路由注册 `/production/yarn-order/workspace/:id?`（无 id 为新建、有 id 为查看/编辑）；列表行"查看/编辑"跳转到工作台。配比 BOM 编辑可在列表页内联弹窗或单独工作台路径 `/production/yarn-recipe-bom/workspace/:id?`，建议用工作台模式（明细较多）。

### 前端页面结构与交互

#### 列表页（3 个）

1. **纱线采购单列表** `yarn-order/index.vue`：
   - 搜索区：关键词输入（匹配 order_no + source_instructions 反查胚布单号）+ 状态下拉 + 单据日期区间 + 交货日期区间
   - 列：采购单号 / 单据日期 / 交货日期 / 状态 DictTag / 关联胚布单号（多单号逗号拼接展示）/ 明细数 / 总金额 / 创建人 / 创建时间 / 操作
   - 操作：查看（跳工作台）/ 编辑（跳工作台，仅草稿态可点）/ 删除（二次确认，仅草稿态）
   - 新建按钮跳工作台无 id 路径

2. **纱线档案管理页** `yarn-master/index.vue`：
   - 上下两栏布局：上方材质列表（el-table 单选）+ 新建/编辑/停用材质按钮；下方规格列表（按选中材质过滤）+ 新建/编辑/删除规格按钮
   - 材质新建/编辑用 el-dialog 弹窗（name/sort/status 三字段）
   - 规格新建/编辑用 el-dialog 弹窗（yarn_type_id 只读带入 + spec_name/standard_name/spin_method/luster/ref_price/remark/status）

3. **纱线配比 BOM 列表** `yarn-recipe-bom/index.vue`：
   - 搜索区：产品编码/产品名关键词 + 状态下拉
   - 列：产品编码 / 产品名称 / 明细行数 / 状态 / 创建人 / 创建时间 / 操作
   - 操作：编辑（跳工作台）/ 删除（二次确认）

#### 工作台/编辑页（2 个）

4. **纱线采购单工作台** `yarn-order/workspace/index.vue`（核心流程页）：
   - 顶部基本信息区：采购单号（新建时显示"待生成"或调 get-next-no 预览）+ 单据日期 + 交货日期 + 损耗率 + 备注 + 状态 DictTag
   - 关联胚布单区（el-table 多选 + 远程搜索）：
     - 远程搜索框输入胚布单号/产品名模糊匹配，候选列表展示（胚布单号/产品/成品量/损耗预设）
     - 已选胚布单表格：胚布单号 / 产品 / 成品量 / 损耗预设 / 操作（移除）
   - **AI 测算按钮**（高亮 primary）：点击调 `POST /yarn-order/calculate`，入参 greigeOrderIds + lossRate
   - 采购明细表格（测算结果展示 + 可编辑）：
      - 列：纱线规格（材质+规格名级联展示）/ 配比 ratio（只读）/ 基础量 base_qty（只读）/ 需求量 required_qty（只读）/ 在库量 stock_qty（只读，因台账回退固定返回 0.00）/ 实采量 purchase_qty（可编辑，默认等于 required_qty）/ 供应商（每行独立远程搜索下拉）/ 单价 unit_price（可编辑，默认 ref_unit_price）/ 金额 total_amount（只读 = unit_price × purchase_qty 前端实时算）/ 历史 价 ref_unit_price（只读）/ 备注 / 操作（删行）
      - 底部汇总行：合计实采量 / 合计金额
    - 操作栏（按状态显示）：
      - 草稿态：[保存草稿][下达][删除]
      - 进行中态：[完成][取消]
      - 已完成/已取消：全只读
    - 只读模式（非草稿态查看）：全部控件 disabled，隐藏编辑按钮

5. **纱线配比 BOM 工作台** `yarn-recipe-bom/workspace/index.vue`：
    - 顶部：产品档案远程搜索（必选，选中带出 productId/productCode/productName 快照）+ 状态 + 备注
    - 配比明细表格（可编辑行）：
      - 列：纱线规格（两级级联带搜索下拉，选规格自动带出 yarn_type_id+yarn_spec_id+ref_price 兜底）/ 配比 ratio（必填，百分比数字 49=49%）/ 纺纱工艺 spin_method（可空，覆盖默认）/ 参考价 ref_price（可空）/ 备注 / 操作（删行）
      - 底部"添加明细行"按钮
      - 配比之和不强制等于 100（展示当前合计值，仅提示不阻断）
    - 操作栏：[保存][删除]（仅新建/编辑态可操作，已停用只读）

#### 修改：胚布单编辑页加产品档案远程下拉

- 修改 `greige-order/edit/index.vue`（或 workspace/index.vue，按既有实现）：在基本信息区追加"产品档案"远程搜索下拉，选中后保存 productId + product_code + product_name 快照（产品档案已有 yarn_specs 字段但本功能废弃不用，仅用作 product_id 关联）

### SQL 脚本

文件拆 3 个 + 修改 1 个既有文件：

1. **`luna-srv/sql/postgresql/lunaerp-production-yarn-master.sql`**：
   - 表 1 `lunaerp_production_yarn_type` DDL + 主键 DO 块 + 列注释 + 序列
   - 表 2 `lunaerp_production_yarn_spec` DDL + 主键 DO 块 + 列注释 + 序列 + 唯一索引
   - 材质 seed 7 项（棉/涤/人棉/氨纶/锦纶/粘胶/混纺，sort 1-7，status ACTIVE）
   - 菜单 10240 + 按钮 10241-10244
   - 无字典（材质不入字典决策）

2. **`luna-srv/sql/postgresql/lunaerp-production-yarn-recipe-bom.sql`**：
   - 表 3 `lunaerp_production_yarn_recipe_bom` DDL + 主键 DO 块 + 列注释 + 序列 + 唯一索引
   - 表 4 `lunaerp_production_yarn_recipe_bom_item` DDL + 主键 DO 块 + 列注释 + 序列 + 索引
   - 菜单 10250 + 按钮 10251-10253
   - 无字典

3. **`luna-srv/sql/postgresql/lunaerp-production-yarn-order.sql`**：
   - （原表 5 `lunaerp_production_stock_ledger` DDL 已于 2026-09-21 移除）
   - 表 6 `lunaerp_production_yarn_order` DDL + 主键 DO 块 + 列注释 + 序列 + 唯一索引
   - 表 7 `lunaerp_production_yarn_order_item` DDL + 主键 DO 块 + 列注释 + 序列 + 索引
   - 菜单 10230 + 按钮 10231-10233
   - 字典 type 2230 `lunaerp_yarn_order_status` + 字典数据 3580-3583（DRAFT/ACTIVE/COMPLETED/CANCELLED，紧接染色单 3571-3574）
   - **末尾追加 ALTER TABLE**：`ALTER TABLE lunaerp_production_greige_order ADD COLUMN IF NOT EXISTS product_id int8 NULL;` + `COMMENT ON COLUMN lunaerp_production_greige_order.product_id IS '产品档案ID（强关联，用于纱线测算时查配比BOM，见 ADR-0011）';` + 索引 `CREATE INDEX IF NOT EXISTS idx_lb_greige_order_product_id ON lunaerp_production_greige_order (product_id);`

## Testing Decisions

### 测试哲学

只测外部业务行为（Service 的可观察语义），不断言内部调用次数等实现细节；测试命名描述行为而非方法。

### 测试 seam

**Service 层纯 Mockito 单元测试**：不启动 Spring 上下文、不连数据库，Mock Mapper + 关联 Service 依赖。不新建 MockMvc/嵌入式 PostgreSQL 测试基建。前端不做测试（lunaerp-elmui 为先行快速版，ADR-0001）。

Prior art：`CustomerServiceImplTest` / `SupplierServiceImplTest` / `SalesContractServiceImplTest` / `DyeingOrderServiceImplTest` / `GreigeOrderServiceImplTest`（同模块已落地的纯 Mockito 模式）。

### 覆盖的行为

#### YarnTypeServiceImplTest

1. 创建：name 缺省必填；撞名报 `1_030_008_001`
2. 更新：不存在报 `1_030_008_000`
3. 删除：逻辑删除（置 deleted=1）
4. 分页查询条件透传

#### YarnSpecServiceImplTest

1. 创建：yarn_type_id 不存在报 `1_030_008_000`；standard_name 在 (yarn_type_id, standard_name) 维度撞名报 `1_030_008_003`
2. 更新：不存在报 `1_030_008_002`
3. 删除：逻辑删除
4. cascade-options：返回两级结构，keyword 过滤 spec_name 模糊匹配

#### YarnRecipeBomServiceImplTest

1. 创建：product_id 不存在报错；product_id 已存在 BOM 报错或转为更新
2. 聚合保存：create/update 整体替换明细（先删后插）；零明细合法
3. 删除：级联逻辑删除明细
4. get-by-product-id：按 product_id 查 BOM + items

#### StockLedgerServiceImplTest（已随 Issue 04 回退移除）

#### YarnOrderServiceImplTest（核心）

1. 创建：order_no 缺省由服务端生成（按天取号）；撞号报 `1_030_009_001`；source_instructions 中胚布单不存在报 `1_030_009_003`；明细 yarn_spec_id 不存在报 `1_030_009_003`；status 缺省 DRAFT；传 ACTIVE 直接下达
2. 更新：仅 DRAFT 可更新，非 DRAFT 报 `1_030_009_002`；不存在报 `1_030_009_000`；明细整体替换；金额服务端权威重算 total_amount = unit_price × purchase_qty
3. 删除：仅 DRAFT 可删；级联逻辑删除明细
4. 状态操作：effect（DRAFT→ACTIVE）、complete（ACTIVE→COMPLETED，仅流转状态，不写台账）、cancel（ACTIVE→CANCELLED）；非法流转报 `1_030_009_002`
5. complete 幂等：已 COMPLETED 不能再 complete
6. calculate（核心测算）：
   - greigeOrderIds 为空报错
   - 胚布单不存在报 `1_030_006_000`
   - 胚布单无 product_id 报 `1_030_009_003`
   - product_id 无配比 BOM 报 `1_030_008_004`
   - baseQty 合并多单多 BOM 明细正确
   - requiredQty = baseQty × (1 + lossRate/100)
   - stockQty 固定返回 0.00，purchaseQty = requiredQty
   - refUnitPrice 历史价回填（最近已下达采购单 unit_price > 规格 ref_price 兜底）
   - sourceGreigeOrderIds/Nos 正确分组

## Out of Scope

- 库存出库（STOCK_OUT）实际使用场景（MVP 仅写 STOCK_IN，不做出库流程）
- 工序库存移动（PROCESS_IN/PROCESS_OUT）实际使用场景（占位枚举，未来染色/织造流程接入）
- 库存盘点与调整（库存盘点单）
- 仓库/库位管理（MVP 无库位概念，ADR-0012）
- 批次管理与批次追溯（仅存 batch_no 字段，不做批次档案）
- 采购单审批工作流
- 采购单付款流程与应付账款
- 采购单到货检验与部分到货
- 多次入库（采购单完成一次性入库，不支持分批入库）
- 采购退货
- 供应商对账与结算
- 纱线规格的批次价/阶梯价（仅单一参考价 ref_price）
- 配比 BOM 版本化与生效日期
- 配比 BOM 的 ratio 之和校验（业务上不强制 100%）
- 采购单行号（line_no）展示与重排（用户决策不要）
- 采购单并发编辑乐观锁/版本号
- 前端单元/组件测试；REST 集成测试与测试数据库基建
- 正式 UI（views/lunaerp/）版本
- 工作台其余 Tab 的实际功能（仅采购单工作台单 Tab 即可，不做多 Tab 占位）
- 库存管理前端页（MVP 不做库存前端，仅后端服务+测算调用）
- Excel 导入导出、打印模板
- AI 智能导入与字段配置中心

## Further Notes

### 已落地文档

- [CONTEXT.md](../../CONTEXT.md) — 已新增"生产管理：纱线采购"术语分区：纱线材质/纱线规格/纱线配比BOM/纱线采购单/纱线采购明细/纱线采购单号/纱线采购单状态/测算口径/入库来源；并更新模块边界（4 个子包）、字典段位（2230）、错误码段位表（1-030-008/009）
- [ADR-0010](../../docs/adr/0010-yarn-order-state-and-calculation.md) — 纱线采购单状态四态 + 测算口径百分比+乘法公式 + 后端测算接口
- [ADR-0011](../../docs/adr/0011-yarn-recipe-bom-independent-table.md) — 配比独立 yarn_recipe_bom 表 + 废弃产品档案 yarn_specs + 胚布单加 product_id
- [ADR-0012](../../docs/adr/0012-yarn-stock-ledger-no-location.md) — （已于 2026-09-21 废弃 deprecated）通用 StockLedger 流水台账
- [ADR-0003](../../docs/adr/0003-sales-contract-item-aggregate-persistence.md) — 明细聚合整体保存模式（配比 BOM 明细 + 采购单明细沿用）
- [ADR-0006](../../docs/adr/0006-greige-order-number-by-day.md) — 胚布单号按天取号模式（采购单号 YP+年月日+3位 沿用）
- [ADR-0008](../../docs/adr/0008-dyeing-order-draft-state.md) — 染色单四态（采购单状态沿用）
- [ADR-0001](../../docs/adr/0001-lunaerp-elmui-frontend-split.md) — 前端落在 lunaerp-elmui 先行快速版目录

### 实现顺序建议

1. SQL 脚本（3 个新文件 + 修改 1 个既有）—— 最先落，建表+seed+菜单+字典
2. 后端 DO×6 + Mapper×6（含 GreigeOrderDO 加 productId 改造；去 stock_ledger）
3. VO 套装（SaveReqVO 内嵌 items、PageReqVO、RespVO 内嵌 items、CalculateReqVO/RespVO）+ 错误码常量（已落）
4. Service×4（yarntype/yarnspec/yarnrecipebom/yarnorder；stockledger 已回退）+ Controller×4
5. ServiceImpl 纯 Mockito 单元测试（重点 YarnOrderServiceImplTest 覆盖测算+状态）
6. 前端 API 与 TS 类型（3 个 index.ts；yarn-stock 已回退）+ dict.ts 枚举 + 隐藏路由
7. 前端纱线档案管理页（材质+规格双区）
8. 前端配比 BOM 列表+工作台
9. 前端采购单列表页
10. 前端采购单工作台（核心流程交互：选胚布单→AI测算→明细编辑→保存/下达/完成）
11. 修改胚布单编辑页加产品档案远程下拉

### 参考

- 旧系统模型：
  - `linaagi/server/prisma/schema.prisma`（Product.yarnSpecs JSON 字段、GreigeInstruction、StockLedger）
  - `linaagi/web/lib/yarn-purchase-calc.ts`（buildPurchaseRows 测算算法）
  - `linaagi/server/src/lib/yarn-balance.util.ts`（listYarnBalances 余额算法）
  - `linaagi/server/src/services/yarn-stock-in-ledger.service.ts`（入库写入点）
- 新系统后端模板：
  - lunaerp 模块 `production/greigeorder`（同域包结构、状态字典、菜单 ID 段 10210）
  - lunaerp 模块 `production/dyeingorder`（四态状态机、聚合保存模式、隐藏路由）
  - lunaerp 模块 `business/salescontract`（聚合保存模式、关联快照）
  - lunaerp 模块 `baseinfo/supplier`（远程搜索下拉、工作台多 Tab）
- 新系统前端模板：
  - `luna-ui/src/views/lunaerp-elmui/production/greige-order/edit/index.vue`（整页表单+明细行增删+远程搜索下拉范例）
  - `luna-ui/src/views/lunaerp-elmui/production/dyeing-order/workspace/index.vue`（多 Tab 工作台、状态操作按钮）
  - `luna-ui/src/views/lunaerp-elmui/baseinfo/supplier/workspace/index.vue`（多 Tab 工作台模式）
- SQL 模板：`luna-srv/sql/postgresql/lunaerp-production-greige-order.sql`、`lunaerp-production-dyeing-order.sql`
