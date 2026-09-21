# 生产管理 - 检验单 MVP（主表 CRUD + 录检明细独立子资源 + 状态操作）

Status: ready-for-agent

## Problem Statement

业务侧需要在新 Luna ERP 系统里管理检验单。旧系统（linaagi，Prisma + Next.js/React）已有检验指示单（`InspectionInstruction`）+ 录检结果（`inspectionResult`，逐卷 JSON blob）+ 同步入库服务（`InspectionSyncService`）的完整链路：检验指示单 → 逐卷录检（卷号/缸号/重量/米数/等级/疵点）→ 同步服务按"缸号+卷号"聚合 → 自动创建/更新 DRAFT 状态的入库单（`stockIn` + `stockInItem`），并带有金额重算、单价保留、财务锁、发货血缘保护等复杂守卫。但旧系统与新系统（Spring Boot + MyBatis-Plus + PostgreSQL + Vue3 + Element Plus）技术栈不兼容。

业务侧希望先用新栈把"生产管理-检验单"的最小可用闭环跑通：检验单主表的增删改查 + 录检明细的**逐卷独立录入** + 状态操作（草稿→录检中→录检完成/已取消）+ 编号（INS-YYYYMMDD-NNN 按天取号）+ 字典/权限/菜单注册。**同步入库功能本期延后**（ADR-0013）——待入库单模块就绪后补做，本期不建入库单模块、不实现同步逻辑、`SYNCED` 状态不入状态机（避免不可达死状态）。这是生产管理域继胚布单、染色单、纱线采购之后的第四个单据，也是检验环节的第一个单据。

## Solution

在 luna-srv 的 `lunasrv-module-lunaerp` 模块新增 `production.inspectionorder`（主表）+ `production.inspectionorder.item`（录检明细，独立子资源）两个子包，落地检验单主表与录检明细两张表、两套独立的 CRUD 服务（主表 4 端点 + 状态操作、明细 4 端点）；在 luna-ui 的 `views/lunaerp-elmui/production/inspection-order/` 下落地列表页与工作台页（3 Tab：基本信息 / 录检明细 / 操作日志）。配套 PostgreSQL 脚本完成 DDL、"生产管理"目录下的检验单菜单/按钮权限（主表 10261-10263、明细 10264-10267）、状态字典与等级字典初始化。

录检明细**作为独立子资源处理**（刻意不一致 ADR-0003 聚合保存模式，理由见 ADR-0014）：有自己的 CRUD 端点与权限码，逐卷增删改，不随主表整体保存替换；删主表时级联逻辑删除明细（与合同明细/胚布颜色级联模式一致）。检验单有业务编号 `INS-YYYYMMDD-NNN`（**带连字符分隔**，刻意不一致项目其他单据无连字符的惯例，见 ADR-0015），按天取号，租户内全量唯一含软删行。状态为四态 DRAFT/ACTIVE/COMPLETED/CANCELLED（仿染色单/纱线采购单），无审批流，编辑页手动切换不做流转约束（与合同/胚布单/染色单 MVP 惯例一致）。`type` 字段区分 `GREIGE` 胚布检验（关联 `greigeOrderId`）/ `FINISHED` 成品检验（关联 `dyeingOrderId`），上游单据均已存在。

## User Stories

### 检验单列表与检索

1. 作为生产计划员，我想在"生产管理 → 检验单"菜单看到检验单列表（检验单号、单据日期、类型、产品编码、产品名称、承检方、目标重量、已录卷数、状态、创建人、创建时间），以便掌握所有检验单。
2. 作为生产计划员，我想在搜索框输入关键词同时匹配检验单号、产品编码、产品名称、承检方名称，以便只记其一也能找到检验单。
3. 作为生产计划员，我想按检验单状态（草稿/录检中/录检完成/已取消）筛选，以便聚焦在执行中的检验单。
4. 作为生产计划员，我想按类型（胚布检验/成品检验）筛选，以便分别查看两类检验单。
5. 作为生产计划员，我想按承检方筛选，以便只看某供应商承接的检验单。
6. 作为生产计划员，我想按单据日期区间筛选，以便定位特定时间段的检验单。
7. 作为生产计划员，我想列表默认按单据日期倒序、分页展示，以便最新检验单在最前。
8. 作为生产计划员，我想列表每行有查看/编辑/删除操作，删除前弹二次确认，以便避免误删。
9. 作为生产计划员，我想状态列用 DictTag 渲染（草稿 warning、录检中 success、录检完成 info、已取消 danger），以便一眼识别状态。
10. 作为生产计划员，我想类型列用标签区分（胚布检验/成品检验），以便区分两类检验单。

### 检验单新建/编辑

11. 作为生产计划员，我想点"新建检验单"进入工作台"基本信息"Tab，以便录入信息完整的检验单。
12. 作为生产计划员，我想新建时必填检验类型（胚布检验 GREIGE / 成品检验 FINISHED），以便区分检验环节。
13. 作为生产计划员，我想新建时必选产品（远程搜索），选中后保存产品 ID + 产品编码 + 产品名称快照，以便记录被检验的品种。
14. 作为生产计划员，我想 type=GREIGE 时可选关联胚布单（远程搜索），type=FINISHED 时可选关联染色单（远程搜索），以便追溯到上游生产单据。
15. 作为生产计划员，我想承检方（染厂/坯布厂/检验仓库等供应商）可选，从供应商档案中选择，选中后保存供应商 ID + 公司名快照。
16. 作为生产计划员，我想可选关联销售合同（远程搜索），选中后保存合同 ID + 合同号快照。
17. 作为生产计划员，我想填写目标重量（KG，可空），以便后续对比录检总量。
18. 作为生产计划员，我想填写备注，以便补充说明。
19. 作为生产计划员，我想新建保存时默认存为草稿（DRAFT），以便反复修改后再正式开始录检。
20. 作为生产计划员，我想新建保存时由服务端自动生成检验单号（INS-YYYYMMDD-NNN 按天取号），也可手填（撞号报错），以便编号唯一可追溯。
21. 作为生产计划员，我想编辑检验单时仅草稿态可编辑主表字段，录检中/录检完成/已取消态主表只读（录检明细在录检中态可继续编辑），防止已开始录检的主表信息被篡改。
22. 作为生产计划员，我想删除检验单时仅草稿态可删（且级联逻辑删除录检明细），录检中态不可删（只能取消），录检完成/已取消不可删，以便保护已录检的数据。

### 录检明细（独立子资源）

23. 作为检验员，我想在检验单工作台"录检明细"Tab 逐卷录入检验结果，每卷保存一次，以便支持高频逐卷录入。
24. 作为检验员，我想每卷明细包含：卷号（必填）、缸号（必填）、实称重量 KG、米数、机台号（可空）、等级（字典 A/B/C/DEFECTIVE，可空）、颜色名称、产品色号、款号、产品编码（快照）、产品名称（快照）、匹数（缺省 1）、备注，以便完整描述每卷检验结果。
25. 作为检验员，我想明细有独立的增删改查端点，逐卷增删改不必整张明细表重传，以便单卷操作轻量、支持多人并行录检。
26. 作为检验员，我想同一检验单内"缸号+卷号"组合不能重复，重复时后端报错明确，以便避免重复录卷。
27. 作为检验员，我想第一卷明细保存时，后端便利地把检验单状态从 DRAFT 转为 ACTIVE（录检中），以便状态自动反映录检进度（不强约束，仅便利）。
28. 作为检验员，我想允许保存零明细检验单，以便先建检验单头、后补录检数据。
29. 作为检验员，我想删除检验单主表时，其全部录检明细一并逻辑删除，以便清理作废草稿不残留孤儿明细。

### 状态操作

30. 作为生产计划员，我想在草稿态或录检中态点"完成"将检验单转为录检完成（COMPLETED），以便标记本单录检已完成。
31. 作为生产计划员，我想在草稿态或录检中态点"取消"将检验单转为已取消（CANCELLED），以便作废不再执行的检验单。
32. 作为生产计划员，我想状态操作只改变状态字段、不修改其他业务字段，以便操作语义清晰。
33. 作为生产计划员，我想本期状态操作不做严格流转约束（任意态可手动切换，与合同/胚布单/染色单 MVP 惯例一致），以便 MVP 灵活。

### 查看与删除

34. 作为生产计划员，我想查看检验单时看到主表全部字段与录检明细列表（只读），以便核对检验单内容。
35. 作为生产计划员，我想通过浏览器直接访问工作台 URL（如 /production/inspection-order/workspace/123）打开特定检验单，以便收藏或分享链接。

### 管理与分权

36. 作为系统管理员，我想执行一份 SQL 脚本即可建表、建检验单菜单/按钮、初始化状态字典与等级字典，且脚本可重复执行，以便部署无手工配置。
37. 作为系统管理员，我想检验单主表有独立权限码 query/create/update/delete，录检明细也有独立权限码 query/create/update/delete，以便按角色分权（检验员只授明细权限、计划员授主表权限）。
38. 作为系统管理员，我想检验单状态来自系统字典 `lunaerp_inspection_order_status` 并用 DictTag 渲染，检验等级来自 `lunaerp_inspection_grade`，以便业务方后续可扩充枚举。
39. 作为开发，我想检验单主表与明细 DO 均继承 TenantBaseDO，以便多租户隔离由框架自动处理。
40. 作为开发，我想录检明细作为独立子资源有自己的 CRUD 端点（刻意不一致 ADR-0003 聚合保存模式，理由见 ADR-0014），以便契合录检高频逐卷操作的业务特点。
41. 作为开发，我想关联字段（产品、胚布单、染色单、承检方、销售合同）均存 ID + 名称快照，以便列表展示无需 JOIN、对方单据删除/改名后本单信息不丢。
42. 作为开发，我想"同步入库"按钮与权限码 `lunaerp:production:inspection-order:sync` 在 SQL 中预留占位但前端默认禁用，待同步功能上线时启用（ADR-0013），以便后续接入无需再改菜单。

## Implementation Decisions

### 模块与包路径

- 后端模块：`lunasrv-module-lunaerp`（已存在，新增子包）
- 后端包：
  - 主表：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.production.inspectionorder`
  - 明细（独立子资源，有自己的 Controller/Service/Mapper/DO）：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.production.inspectionorder.item`
- 前端视图：`luna-ui/src/views/lunaerp-elmui/production/inspection-order/`（`index.vue` 列表页 + `workspace/index.vue` 工作台页）
- 前端 API：`luna-ui/src/api/lunaerp-elmui/production/inspection-order/index.ts`（主表）+ `inspection-order-item/index.ts`（明细）
- 沿用 ADR-0001：落在 `lunaerp-elmui/` 先行快速版目录

### 数据模型

**检验单主表 `lunaerp_production_inspection_order`**（继承 TenantBaseDO：id/creator/create_time/updater/update_time/deleted/tenant_id）：

| 分组 | 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|------|
| 单号 | code | varchar(32) | ✓ | INS-YYYYMMDD-NNN，租户内全量唯一含软删行（ADR-0015） |
| 类型 | type | varchar(16) | ✓ | GREIGE 胚布检验 / FINISHED 成品检验（后端枚举，不入字典） |
| 状态 | status | varchar(16) | ✓ | 字典 lunaerp_inspection_order_status，缺省 DRAFT |
| 关联产品 | product_id | int8 | ✓ | 逻辑关联产品档案，不建数据库外键 |
| | product_code | varchar(64) | - | 产品编码快照 |
| | product_name | varchar(128) | - | 产品名称快照 |
| 关联胚布单 | greige_order_id | int8 | - | type=GREIGE 时填，逻辑关联胚布单 |
| 关联染色单 | dyeing_order_id | int8 | - | type=FINISHED 时填，逻辑关联染色单 |
| 承检方 | inspection_factory_id | int8 | - | 供应商 ID（命名 inspectionFactory 非 dyeingFactory，避免 GREIGE 场景歧义） |
| | inspection_factory_name | varchar(128) | - | 供应商公司名快照 |
| 关联合同 | sales_contract_id | int8 | - | 逻辑关联销售合同 |
| | contract_no | varchar(32) | - | 合同号快照 |
| 目标量 | target_weight | numeric(18,2) | - | 目标重量 KG，可空 |
| 备注 | remark | varchar(1000) | - | 备注 |
| 单据日期 | order_date | date | ✓ | 缺省今天 |

索引：主表唯一索引 `(tenant_id, code)` 含软删行（编号不释放，仿 ADR-0005/ADR-0006）；辅助索引 `(status)`、`(order_date)`、`(product_id)`、`(inspection_factory_id)`。

**录检明细表 `lunaerp_production_inspection_order_item`**（继承 TenantBaseDO）：

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| id | int8 | - | 主键，序列 |
| inspection_order_id | int8 | ✓ | 所属检验单 |
| roll_no | varchar(64) | ✓ | 卷号（必填） |
| vat_no | varchar(64) | ✓ | 缸号（必填） |
| measured_weight | numeric(18,2) | - | 实称重量 KG |
| length_m | numeric(18,2) | - | 米数 |
| machine_no | varchar(64) | - | 机台号 |
| grade | varchar(16) | - | 等级，字典 lunaerp_inspection_grade |
| color_name | varchar(128) | - | 颜色名称 |
| color_no | varchar(64) | - | 产品色号 |
| style_no | varchar(64) | - | 款号 |
| product_code | varchar(64) | - | 产品编码快照（为后续同步预留） |
| product_name | varchar(128) | - | 产品名称快照（为后续同步预留） |
| piece_count | int4 | - | 匹数，缺省 1 |
| remark | varchar(1000) | - | 备注 |

索引：明细表普通索引 `(inspection_order_id)`、`(vat_no, roll_no)`（辅助 service 层查重查询，非唯一约束）。**不建唯一约束**——单内 `(vat_no, roll_no)` 唯一性由 service 层校验（用户决策）。本期不做 `defects` 疵点数组字段。

### 检验单号（ADR-0015）

`INS-YYYYMMDD-NNN`（如 `INS-20260921-001`）：`INS` + `-` + 年月日 8 位 + `-` + 3 位日流水（超 999 自然扩位），按天取号，租户内全量唯一含软删行（唯一索引含 deleted，仿 ADR-0005/ADR-0006）。新建缺省由服务端生成、也可手填（撞号报 `1_030_010_001`）。带连字符分隔，刻意不一致项目其他单据无连字符惯例。提供只读 `GET /get-next-no` 预览下一个编号接口（仿供应商）。取号/查重/重试逻辑复用 `SupplierServiceImpl`/`YarnOrderServiceImpl` 既有机制，仅格式串不同。

### 状态机与状态操作

状态枚举（字典 `lunaerp_inspection_order_status`）：`DRAFT` 草稿（缺省）/ `ACTIVE` 录检中 / `COMPLETED` 录检完成 / `CANCELLED` 已取消。仿染色单/纱线采购单四态。

状态约束（MVP 不做严格流转约束，对齐合同/胚布单/染色单 MVP 惯例）：
- 编辑主表字段：仅 DRAFT 可编辑，ACTIVE/COMPLETED/CANCELLED 主表只读（录检明细在 ACTIVE 态可继续编辑）
- 删除：仅 DRAFT 可删（级联逻辑删除明细）；ACTIVE 不可删，只能取消；COMPLETED/CANCELLED 不可删
- 流转：任意态可手动切换（MVP 不约束），第一卷明细保存时由后端便利地把 DRAFT 转 ACTIVE（不强约束，仅便利）
- 状态操作端点：`PUT /complete?id=`（DRAFT/ACTIVE → COMPLETED）、`PUT /cancel?id=`（任意态 → CANCELLED）；本期不设 effect（DRAFT→ACTIVE 由录检自动触发，无显式按钮）

**不含 `SYNCED` 态**——同步入库本期延后（ADR-0013），SYNCED 留给同步功能上线时插入，避免不可达死状态。

### 录检明细独立子资源（ADR-0014）

- 录检明细有独立 Controller/Service/Mapper/DO，4 个 CRUD 端点 + 4 个权限码，逐卷增删改。
- 单内 `(vatNo, rollNo)` 唯一性由 service 层校验（create/update 时查同单同组合，存在则报 `1_030_010_003`），不建数据库唯一约束（用户决策）。
- 删主表时级联逻辑删除全部明细（与合同明细/胚布颜色级联模式一致）。
- 明细不随主表整体保存替换——主表 create/update 不接收明细数组，明细完全由独立端点管理。

### Controller 与 API 契约

**主表** 基础路径 `/lunaerp/production/inspection-order`：

- `GET /page` — 参数 pageNo/pageSize/keyword/type/status/inspectionFactoryId/orderDateStart/orderDateEnd；keyword 对 code / product_code / product_name / inspection_factory_name 做 OR 模糊（PostgreSQL LOWER+LIKE）；默认 order_date DESC；返回 PageResult（不含明细）
- `GET /get?id=` — 详情，主表 RespVO（不含明细，明细由独立端点查）
- `GET /get-next-no` — 预览下一个检验单号（只读）
- `POST /create` — body 为 InspectionOrderSaveReqVO；status 缺省 DRAFT；生成/校验 code；返回 id
- `PUT /update` — 同结构（含 id）；仅 DRAFT 可更新主表，否则报 1_030_010_002；不存在报 1_030_010_000
- `DELETE /delete?id=` — 仅 DRAFT 可删，否则报 1_030_010_002；级联逻辑删除明细
- `PUT /complete?id=` — DRAFT/ACTIVE → COMPLETED
- `PUT /cancel?id=` — 任意态 → CANCELLED

**明细** 基础路径 `/lunaerp/production/inspection-order-item`：

- `GET /page` — 参数 pageNo/pageSize/inspectionOrderId；返回 PageResult；按 id DESC
- `GET /get?id=` — 单卷明细详情
- `POST /create` — body 为 InspectionOrderItemSaveReqVO（含 inspectionOrderId）；校验所属检验单存在 + 单内(vatNo,rollNo)唯一；第一卷时便利转 DRAFT→ACTIVE；返回 id
- `PUT /update` — 同结构（含 id）；校验所属检验单存在 + 单内唯一（排除自身）；返回
- `DELETE /delete?id=` — 删除单卷明细

所有端点 `@PreAuthorize("@ss.hasPermission('lunaerp:production:inspection-order[-item]:<action>')")`，action ∈ {query, create, update, delete}；状态操作复用 update 权限。返回 CommonResult / PageResult，VO 带 Swagger 与 jakarta.validation 注解，风格对齐 supplier / dyeingorder 模块。

### 关联校验与快照

- 创建/更新主表时校验 productId 对应的产品存在（不存在报 1_030_010_002），从产品带出 productCode/productName 快照写入主表。
- 校验 inspectionFactoryId（若传）对应的供应商存在，保存 inspectionFactoryName 快照。
- 校验 greigeOrderId（type=GREIGE 时，若传）对应的胚布单存在；校验 dyeingOrderId（type=FINISHED 时，若传）对应的染色单存在。
- 校验 salesContractId（若传）对应的销售合同存在，保存 contractNo 快照。
- 明细 create/update 时校验 inspectionOrderId 对应的检验单存在（不存在报 1_030_010_100）。

### 权限码

主表：`lunaerp:production:inspection-order:query` / `:create` / `:update` / `:delete` / `:sync`（sync 预留占位，前端默认禁用，ADR-0013）。
明细：`lunaerp:production:inspection-order-item:query` / `:create` / `:update` / `:delete`。

### 错误码（1-030-010 段，主表与明细共用）

- `1_030_010_000` — INSPECTION_ORDER_NOT_EXISTS（检验单不存在）
- `1_030_010_001` — INSPECTION_ORDER_NO_DUPLICATE（检验单号【{}】已存在）
- `1_030_010_002` — INSPECTION_ORDER_STATUS_NOT_ALLOWED（当前状态不允许该操作）
- `1_030_010_003` — INSPECTION_ORDER_REFERENCE_NOT_FOUND（关联的{}不存在或不可用）
- `1_030_010_004` — INSPECTION_ORDER_ROLL_DUPLICATE（单内卷号【{}】缸号【{}】已存在）
- `1_030_010_100` — INSPECTION_ORDER_ITEM_NOT_EXISTS（录检明细不存在，明细段从 100 起避开主表）

在 lunaerp 模块 `ErrorCodeConstants` 追加。段位全表已更新于 CONTEXT.md。

### 字典（2 个）

- `lunaerp_inspection_order_status`（dict_type id=2231）：DRAFT 草稿(warning) / ACTIVE 录检中(success) / COMPLETED 录检完成(info) / CANCELLED 已取消(danger)
- `lunaerp_inspection_grade`（dict_type id=2232）：A 一等品(success) / B 二等品(info) / C 三等品(warning) / DEFECTIVE 等外品(danger)

`type` 字段（GREIGE/FINISHED）不入字典，仅后端枚举使用。本期无同步入库，不建入库卷状态（rollStatus）字典。

### 菜单与路由

SQL 菜单（ID 段 10260 起，可重复执行，先 DELETE 再 INSERT，父菜单 10200 生产管理目录已由胚布单脚本创建）：

- 10260 `检验单` 菜单（type=2，parent=10200，component `lunaerp-elmui/production/inspection-order/index`，componentName `LunaerpElmuiProductionInspectionOrder`，承载 query 权限）
- 10261/10262/10263 主表 创建/修改/删除 按钮（type=3）
- 10264 `检验单同步入库` 按钮（type=3，权限 `:sync`，预留占位，前端默认禁用，ADR-0013）
- 10265/10266/10267/10268 录检明细 query/create/update/delete 按钮（type=3，挂 10260 下，仿供应商联系人 10060-10063 同构）

工作台不进菜单：参照供应商/染色单工作台模式在前端静态隐藏路由注册 `/production/inspection-order/workspace/:id?`（无 id 为新建、有 id 为查看/编辑）；列表行"查看/编辑"跳转到工作台。

### 前端页面结构与交互

- 列表页：搜索区为关键词输入 + 类型下拉 + 状态下拉 + 承检方下拉 + 单据日期区间；列含检验单号/单据日期/类型/产品编码/产品名称/承检方/目标重量/已录卷数/状态 DictTag/创建人/创建时间/操作；新建、编辑、删除二次确认按钮带 v-hasPermi；分页。
- 工作台页：3 Tab 容器（el-tabs）：
  - **基本信息** Tab：单据类型单选（GREIGE/FINISHED）、产品远程选择（带出编码/名称快照）、承检方远程选择（供应商）、胚布单/染色单远程选择（按 type 切换显示对应选择器）、销售合同远程选择（带出合同号快照）、目标重量、单据日期、备注；操作栏（按状态显示按钮）：草稿态[保存草稿][完成][取消][删除]、录检中态[完成][取消]、录检完成/已取消全只读。
  - **录检明细** Tab：独立子资源 CRUD（调 inspection-order-item 端点）；表格列含卷号/缸号/实称重量/米数/机台号/等级(字典下拉)/颜色/色号/款号/匹数/备注/操作（删行）；底部"添加明细"打开新增表单（单卷新增）；行内编辑或弹窗编辑。仅 DRAFT/ACTIVE 态可编辑，COMPLETED/CANCELLED 只读。同步入库按钮放此 Tab 但默认禁用（占位）。
  - **操作日志** Tab：占位"开发中"（后续接操作日志模块）。
- 只读模式（非草稿/录检中态查看）：全部控件 disabled，隐藏编辑按钮。

### SQL 脚本

文件：`luna-srv/sql/postgresql/lunaerp-production-inspection-order.sql`，参照 `lunaerp-production-dyeing-order.sql` / `lunaerp-baseinfo-supplier.sql` 体例：

1. 2 张表 DDL（CREATE TABLE IF NOT EXISTS + 主键 DO 块判存 + 列注释 + 2 个序列 + 索引：主表唯一索引 `(tenant_id, code)` 含软删行、明细表普通索引）
2. system_menu：10260 菜单 + 10261-10263 主表按钮 + 10264 同步按钮（预留）+ 10265-10268 明细按钮（事务内先 DELETE 再 INSERT）
3. system_dict_type（id 2231 状态、2232 等级）+ system_dict_data（状态 4 项、等级 4 项，紧接纱线采购单状态 2230 之后）

## Testing Decisions

### 测试哲学

只测外部业务行为（Service 的可观察语义），不断言内部调用次数等实现细节；测试命名描述行为而非方法。前端不做测试（lunaerp-elmui 为先行快速版，ADR-0001）。

### 测试 seam（唯一接缝）

**Service 层纯 Mockito 单元测试**，两个测试类：
- `InspectionOrderServiceImplTest`：主表 Service（编号取号/查重/重试、关联校验与快照、状态操作、级联删除）
- `InspectionOrderItemServiceImplTest`：明细 Service（单内卷号缸号查重、第一卷便利转 ACTIVE、所属检验单存在校验）

不启动 Spring 上下文、不连数据库，Mock 各 Mapper + 产品/供应商/胚布单/染色单/销售合同校验依赖。不新建 MockMvc/嵌入式 PostgreSQL 测试基建。

Prior art：`SupplierServiceImplTest`（编号取号/查重/重试模式）、`DyeingOrderServiceImplTest`（状态操作与关联校验模式）、`CustomerServiceImplTest`/`SalesContractServiceImplTest`（聚合保存与关联快照模式）。目标测试规模对齐供应商模块（~87 测试）。

### 覆盖的行为

主表：
1. 创建：status 缺省 DRAFT；code 自动生成（INS-YYYYMMDD-NNN 格式，按天取号）；code 手填撞号报 1_030_010_001；关联产品/供应商/胚布单/染色单/合同不存在报 1_030_010_003；产品带出 productCode/productName 快照写入主表。
2. 更新：仅 DRAFT 可更新主表，非 DRAFT 报 1_030_010_002；不存在报 1_030_010_000。
3. 删除：仅 DRAFT 可删，非 DRAFT 报 1_030_010_002；级联逻辑删除明细（调明细 Mapper 按 inspectionOrderId 批量逻辑删除）。
4. 状态操作：complete（DRAFT/ACTIVE→COMPLETED）、cancel（任意→CANCELLED）。
5. 分页查询条件：keyword/type/status/inspectionFactoryId/日期区间正确透传到 Mapper 查询构造。
6. 详情：get 返回主表（不含明细）。
7. get-next-no：返回按天取号的下一个编号（查重跳过已用）。

明细：
8. 创建：校验所属检验单存在（不存在报 1_030_010_100）；单内(vatNo,rollNo)重复报 1_030_010_004；第一卷时便利把主表 DRAFT→ACTIVE（调主表 Service 更新 status）。
9. 更新：校验所属检验单存在；单内唯一排除自身；不存在报 1_030_010_100。
10. 删除：删除单卷明细。
11. 分页：按 inspectionOrderId 过滤，id DESC。

## Out of Scope

- **同步入库功能**（ADR-0013）：不建入库单模块、不实现同步逻辑、`SYNCED` 状态不入状态机、同步按钮预留占位但禁用
- **入库单模块**（stockIn/stockInItem）：本期不建，待同步功能上线时一起补
- **疵点（defects）数据**：本期不做，后续同步上线时补 defects JSON 字段
- **从上游单据带入生成检验单**：本期纯手工新建，不接"从染色/胚布单自动带入"
- **自动同步触发**：本期无同步，更无自动触发；同步功能上线后先做手动触发，自动触发再后续
- 审批工作流与审批状态
- 检验单与发货/应付台账/对账的联动（依赖尚未存在的 shipment/ledger 模块）
- 财务锁（reconStatus/isPriceConfirmed）守卫（依赖尚未存在的对账模块）
- 发货明细血缘保护（依赖尚未存在的 shipment 模块）
- 打印模板、Excel 导入导出、AI 智能导入
- 字段配置中心（extensionData 动态表单扩展字段）
- 多产品（productsJson）检验单：本期一单一产品
- 操作日志模块（工作台"操作日志"Tab 占位）
- 并发编辑乐观锁/版本号
- 前端单元/组件测试；REST 集成测试与测试数据库基建
- 正式 UI（views/lunaerp/）版本
- 胚布单改 4 态（Q18 撤销——强加 DRAFT 是死状态，与胚布单业务形态不符）

## Further Notes

### 已落地文档

- [CONTEXT.md](../../CONTEXT.md) — 已新增"生产管理：检验单"术语分区：检验单、录检明细、承检方、检验单号、检验单状态、检验等级；并更新模块边界、字典、错误码段位表（新增 1-030-010 段）
- [ADR-0013](../../docs/adr/0013-inspection-sync-to-stockin-deferred.md) — 检验同步入库本期延后
- [ADR-0014](../../docs/adr/0014-inspection-item-independent-subresource.md) — 录检明细作为独立子资源（刻意不一致 ADR-0003 聚合保存模式）
- [ADR-0015](../../docs/adr/0015-inspection-order-no-with-dashes.md) — 检验单号带连字符分隔（刻意不一致项目编号惯例）
- [ADR-0001](../../docs/adr/0001-lunaerp-elmui-frontend-split.md) — 前端落在 lunaerp-elmui 先行快速版目录
- [ADR-0005](../../docs/adr/0005-supplier-no-full-uniqueness.md) — 编号租户内全量唯一含软删行（检验单号沿用）
- [ADR-0006](../../docs/adr/0006-greige-order-no-daily-sequence.md) — 按天取号模式（检验单号沿用）

### 实现顺序建议

1. SQL 脚本（2 表 + 序列/索引 + 10260 菜单 + 10264 同步按钮占位 + 10265-10268 明细按钮 + 状态字典 2231 + 等级字典 2232）
2. 后端 DO×2（主表 + 明细）+ Mapper×2
3. VO 套装（主表 SaveReqVO/PageReqVO/RespVO；明细 SaveReqVO/PageReqVO/RespVO）+ 错误码常量（1-030-010 段）
4. 主表 Service（编号取号/查重/重试、关联校验与快照、状态操作、级联删除）+ Controller（8 端点含 get-next-no）
5. 明细 Service（单内卷号缸号查重、第一卷便利转 ACTIVE、所属检验单校验）+ Controller（4 端点）
6. ServiceImpl 纯 Mockito 单元测试（两个测试类，~87 测试规模）
7. 前端 API 与 TS 类型（主表 + 明细）、dict.ts 枚举、隐藏路由
8. 前端列表页
9. 前端工作台页（3 Tab：基本信息 + 录检明细独立子资源 CRUD + 操作日志占位 + 状态操作按钮）

### 参考

- 旧系统模型：`linaagi/server/prisma/schema.prisma`（InspectionInstruction + inspectionResult）、`linaagi/server/src/services/inspection-sync.service.ts`（同步服务，本期不实现但作为后续同步 issue 的参考）
- 旧系统前端：`linaagi/web/app/staff/components/production/inspection/`
- 新系统后端模板：lunaerp 模块 `production/dyeingorder`（同域包结构、状态字典、4 态状态机）、`baseinfo/supplier`（编号取号/查重/重试机制、独立子资源模式——supplier-contact 是独立子资源先例）、`business/salescontract`（关联快照）
- 新系统前端模板：`luna-ui/src/views/lunaerp-elmui/production/dyeing-order/`（列表页 + 工作台多 Tab）、`luna-ui/src/views/lunaerp-elmui/baseinfo/supplier/`（列表页 + 工作台多 Tab + 独立子资源联系人 CRUD）
- SQL 模板：`luna-srv/sql/postgresql/lunaerp-production-dyeing-order.sql`（主从表 + 状态字典）、`luna-srv/sql/postgresql/lunaerp-baseinfo-supplier.sql`（唯一索引含软删行 + 独立子资源菜单按钮 10060-10063 模式）
