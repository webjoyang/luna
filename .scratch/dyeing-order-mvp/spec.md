# 生产管理 - 染色单 MVP（主表 CRUD + 颜色明细聚合 + 状态操作）

Status: ready-for-agent

## Problem Statement

业务侧需要在新 Luna ERP 系统里管理染色单。旧系统（linaagi，Prisma + Next.js/React）已有染色单实现：`DyeingInstruction` 主表含约 40 个字段（产品规格、技术参数、色牢度、损耗、包装等）+ `DyeingColor` 颜色明细表，并串联了审批工作流、胚布库存扣减、染缸批次卡（DyeingVat）、检验单联动、字段配置中心、AI 导入等长链路。但旧系统与新系统（Spring Boot + MyBatis-Plus + PostgreSQL + Vue3 + Element Plus）技术栈不兼容。

业务侧希望先用新栈把"生产管理-染色单"的最小可用闭环跑通：染色单主表的增删改查 + 颜色明细的聚合录入 + 状态操作（草稿→下达→完成/取消）+ 关联销售合同自动带出明细 + 字典/权限/菜单注册，作为生产管理域（区别于已落地的胚布单）的第二个单据，也为二期胚布扣减、染缸批次、检验单等链路提供干净的聚合根。

## Solution

在 luna-srv 的 `lunasrv-module-lunaerp` 模块新增 `production.dyeingorder` 子包（与 `production.greigeorder` 平级），落地染色单主表与染色颜色明细两张表、一套以染色单为聚合根的 CRUD + 状态操作服务；在 luna-ui 的 `views/lunaerp-elmui/production/dyeing-order/` 下落地列表页与工作台页（多 Tab：激活"基础信息"含主表表单 + 可编辑颜色明细表格，其余 Tab 占位"开发中"）。配套 PostgreSQL 脚本完成 DDL、"生产管理"目录下的染色单菜单/按钮权限、状态字典初始化。

颜色明细不作为独立资源（同合同明细与胚布单颜色分配模式，见 ADR-0003）：随染色单整体保存、级联逻辑删除。染色单无业务编号（刻意与合同/胚布单/供应商不同，见 ADR-0009），对外标识靠 id + 合同号快照。状态为四态 DRAFT/ACTIVE/COMPLETED/CANCELLED（刻意比其他单据多一个草稿态，见 ADR-0008）。

## User Stories

### 染色单列表与检索

1. 作为生产计划员，我想在"生产管理 → 染色单"菜单看到染色单列表（单据日期、合同号、客户名称、承染方、产品编码、产品名称、状态、交货日期、创建人、创建时间），以便掌握所有染色指令。
2. 作为生产计划员，我想在搜索框输入关键词同时匹配合同号、客户名称、产品编码、产品名称，以便只记其一也能找到染色单。
3. 作为生产计划员，我想按染色单状态（草稿/进行中/已完成/已取消）筛选，以便聚焦在执行的染色单。
4. 作为生产计划员，我想按客户筛选，以便只看某客户的染色单。
5. 作为生产计划员，我想按承染方（染厂）筛选，以便只看某染厂承接的染色单。
6. 作为生产计划员，我想按单据日期区间、交货日期区间筛选，以便定位特定时间段的染色单。
7. 作为生产计划员，我想列表默认按单据日期倒序、分页展示，以便最新染色单在最前。
8. 作为生产计划员，我想列表每行有查看/编辑/删除操作，删除前弹二次确认，以便避免误删。
9. 作为生产计划员，我想状态列用 DictTag 渲染（草稿 warning 色、进行中 success、已完成 info、已取消 danger），以便一眼识别状态。

### 染色单新建/编辑

10. 作为生产计划员，我想点"新建染色单"进入工作台"基础信息"Tab，以便录入信息完整的染色单。
11. 作为生产计划员，我想新建时必选销售合同（远程搜索），选中后自动带出合同号快照、客户 ID + 客户名快照，并把该合同的全部明细行（产品编码、产品名称、客户色号、合同数量）拷到染色单颜色明细行，以便减少重复录入。
12. 作为生产计划员，我想带出的合同明细行可以删除不需要的行、修改数量/颜色/投染数，以便只保留本次要染色的颜色。
13. 作为生产计划员，我想承染方（染厂）为必选，从供应商档案中选择，选中后保存染厂 ID + 公司名快照，以便记录承接本单染色的工厂。
14. 作为生产计划员，我想单据日期必填且缺省为今天、交货日期可选，以便快速建档。
15. 作为生产计划员，我想填写产品编码、产品名称、成分、幅宽、克重、缩水率（产品规格），以便描述本单染色的品种规格。
16. 作为生产计划员，我想填写损耗率、pH 值、缩水率、水洗牢度、皂洗牢度、干摩擦牢度、湿摩擦牢度、日晒牢度、汗渍牢度、干洗牢度、六千沾色牢度、其他要求（技术规格与色牢度），以便下达染色工艺与质量要求。
17. 作为生产计划员，我想技术规格与色牢度字段均为可空文本，不做字典约束，以便灵活填写各种工艺指标。
18. 作为生产计划员，我想填写备注，以便补充说明。
19. 作为生产计划员，我想新建保存时默认存为草稿（DRAFT），以便反复修改后再正式下达。
20. 作为生产计划员，我想新建时可以直接点"下达"保存为进行中（ACTIVE），以便确认无误的染色单直接生效。
21. 作为生产计划员，我想编辑染色单时仅草稿态可编辑，进行中/已完成/已取消态只读，防止已下达的指令被篡改。
22. 作为生产计划员，我想删除染色单时仅草稿态可删，进行中态不可删（只能取消），已完成/已取消不可删，以便保护已下达的数据。

### 颜色明细

23. 作为生产计划员，我想在染色单工作台维护多行颜色明细，以便逐行记录每个颜色的染色要求。
24. 作为生产计划员，我想每行明细包含：客户色号（从合同明细带出可改）、颜色名称（手填）、本单染色数量（成品口径）、匹数、投染数（含损耗的实际投入量）、投染数单位（复用合同单位字典 KG/M）、合同数量（从合同明细带出只读对比），以便完整描述颜色与数量。
25. 作为生产计划员，我想能新增/删除明细行，以便整理染色单内容。
26. 作为生产计划员，我想编辑已有染色单时看到其全部明细并整体修改后一次保存，以便无需逐行调接口。
27. 作为生产计划员，我想允许保存零明细染色单，以便先建染色单头、后补颜色明细。

### 状态操作

28. 作为生产计划员，我想在草稿态点"下达"将染色单转为进行中，以便正式把指令发给染厂。
29. 作为生产计划员，我想在进行中态点"完成"将染色单转为已完成，以便标记本单染色已完成。
30. 作为生产计划员，我想在进行中态点"取消"将染色单转为已取消，以便作废不再执行的指令。
31. 作为生产计划员，我想状态操作只改变状态字段、不修改其他业务字段，以便操作语义清晰。
32. 作为生产计划员，我想非法状态流转（如已完成再下达、已取消再完成）得到明确报错，以便防止状态错乱。

### 查看与删除

33. 作为生产计划员，我想查看染色单时看到主表全部字段与明细列表（只读），以便核对染色单内容。
34. 作为生产计划员，我想删除染色单后主表与其明细一并逻辑删除、列表不再显示，以便清理作废草稿。
35. 作为生产计划员，我想通过浏览器直接访问工作台 URL（如 /production/dyeing-order/workspace/123）打开特定染色单，以便收藏或分享链接。

### 管理与分权

36. 作为系统管理员，我想执行一份 SQL 脚本即可建表、建染色单菜单/按钮、初始化状态字典，且脚本可重复执行，以便部署无手工配置。
37. 作为系统管理员，我想染色单有独立权限码 query/create/update/delete，以便按角色分权。
38. 作为系统管理员，我想染色单状态来自系统字典 `lunaerp_dyeing_order_status` 并用 DictTag 渲染，以便业务方后续可扩充枚举。
39. 作为开发，我想染色单与明细 DO 继承 TenantBaseDO，以便多租户隔离由框架自动处理。
40. 作为开发，我想染色单颜色明细随主表聚合保存（create/update 整体替换明细、delete 级联逻辑删除），以便与合同明细/胚布单颜色分配模式一致。
41. 作为开发，我想关联字段（合同、客户、染厂）均存 ID + 名称快照，以便列表展示无需 JOIN、对方单据删除/改名后本单信息不丢。

## Implementation Decisions

### 模块与包路径

- 后端模块：`lunasrv-module-lunaerp`（已存在，新增子包）
- 后端包：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.production.dyeingorder`；颜色明细 DO/Mapper 与主表同包或 `.color` 子包，无独立 Controller/Service
- 前端视图：`luna-ui/src/views/lunaerp-elmui/production/dyeing-order/`（`index.vue` 列表页 + `workspace/index.vue` 工作台页）
- 前端 API：`luna-ui/src/api/lunaerp-elmui/production/dyeing-order/index.ts`
- 沿用 ADR-0001：落在 `lunaerp-elmui/` 先行快速版目录

### 数据模型

**染色单主表 `lunaerp_production_dyeing_order`**（继承 TenantBaseDO：id/creator/create_time/updater/update_time/deleted/tenant_id）：

| 分组 | 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|------|
| 关联合同 | sales_contract_id | int8 | - | 逻辑关联销售合同，不建数据库外键 |
| | contract_no | varchar(32) | - | 合同号快照（选中合同时保存） |
| 关联客户 | customer_id | int8 | - | 客户 ID（从合同带出） |
| | client_name | varchar(128) | - | 客户名快照（从合同带出） |
| 承染方 | dyeing_factory_id | int8 | ✓ | 染厂供应商 ID，必填 |
| | dyeing_factory_name | varchar(128) | ✓ | 染厂公司名快照，必填 |
| 日期 | order_date | date | ✓ | 单据日期，缺省今天 |
| | delivery_date | date | - | 交货日期 |
| 产品规格 | product_code | varchar(64) | - | 产品编码 |
| | product_name | varchar(128) | - | 产品名称 |
| | composition | varchar(128) | - | 成分 |
| | width | varchar(64) | - | 幅宽 |
| | gsm | varchar(64) | - | 克重 |
| | shrinkage | varchar(64) | - | 缩水率 |
| 技术规格&色牢度 | loss | varchar(64) | - | 损耗率 |
| | ph | varchar(64) | - | pH 值 |
| | fastness_wash | varchar(64) | - | 水洗牢度 |
| | fastness_soap | varchar(64) | - | 皂洗牢度 |
| | fastness_dry_rub | varchar(64) | - | 干摩擦牢度 |
| | fastness_wet_rub | varchar(64) | - | 湿摩擦牢度 |
| | fastness_light | varchar(64) | - | 日晒牢度 |
| | fastness_perspiration | varchar(64) | - | 汗渍牢度 |
| | fastness_dry_clean | varchar(64) | - | 干洗牢度 |
| | fastness_six_fiber | varchar(64) | - | 六千沾色牢度 |
| | other_req | varchar(1000) | - | 其他要求 |
| 备注 | remark | varchar(1000) | - | 备注 |
| 状态 | status | varchar(16) | ✓ | 字典 lunaerp_dyeing_order_status，缺省 DRAFT |

无业务编号字段（无 order_no，见 ADR-0009）。

**染色颜色明细表 `lunaerp_production_dyeing_color`**（继承 TenantBaseDO）：

| 字段 | 类型 | 必填 | 备注 |
|------|------|------|------|
| id | int8 | - | 主键，序列 |
| dyeing_order_id | int8 | ✓ | 所属染色单 |
| line_no | int4 | ✓ | 行序号（保存时按数组顺序重排） |
| customer_color_no | varchar(64) | - | 客户色号（从合同明细 customerColor 带出可改） |
| color_name | varchar(128) | - | 颜色名称（手填） |
| quantity | numeric(18,2) | ✓ | 本单染色数量（成品口径），缺省 0 |
| pieces | int4 | - | 匹数 |
| dyeing_input_qty | numeric(18,2) | - | 投染数（含损耗的实际投入量） |
| dyeing_input_unit | varchar(16) | - | 投染数单位，复用字典 lunaerp_sales_contract_unit（KG/M） |
| contract_qty | numeric(18,2) | - | 合同数量（从合同明细带出，只读对比） |

索引：明细表普通索引 `(dyeing_order_id)`；主表辅助索引 `(status)`、`(order_date)`、`(sales_contract_id)`。主表无唯一索引（无编号）。

### 无业务编号（ADR-0009）

染色单不设 order_no 字段，不提供 get-next-no 接口。对外标识靠数据库自增 id + 合同号快照（contract_no）。列表页不展示"染色单号"列，定位靠"合同号+客户+日期+承染方"组合。

### 状态机与状态操作（ADR-0008）

状态枚举：`DRAFT` 草稿（缺省）/ `ACTIVE` 进行中 / `COMPLETED` 已完成 / `CANCELLED` 已取消。

状态约束：
- 编辑：仅 DRAFT 可编辑，ACTIVE/COMPLETED/CANCELLED 只读
- 删除：仅 DRAFT 可删；ACTIVE 不可删，只能"取消"；COMPLETED/CANCELLED 不可删
- 流转：DRAFT → ACTIVE（下达 effect）/ ACTIVE → COMPLETED（完成 complete）/ ACTIVE → CANCELLED（取消 cancel）
- 非法流转（如 ACTIVE→DRAFT、COMPLETED→ACTIVE、CANCELLED→ACTIVE）报 `1_030_007_001`

新建时通过 status 字段控制：传 DRAFT 存草稿，传 ACTIVE 直接下达。

### 聚合保存（同 ADR-0003 模式）

- `DyeingOrderSaveReqVO` 内嵌 `colors: List<DyeingColorSaveReqVO>`，无明细独立端点。
- create/update 均在一个事务内：保存主表 → 按 dyeing_order_id 删除旧明细（逻辑删除）→ 插入本次提交的全部明细，重排 line_no。
- 零明细合法。
- 删除染色单：逻辑删除主表，并按 dyeing_order_id 逻辑删除全部明细。
- `get` 详情嵌套返回 colors；`page` 列表不返回明细。

### Controller 与 API 契约

基础路径 `/lunaerp/production/dyeing-order`：

- `GET /page` — 参数 pageNo/pageSize/keyword/status/customerId/dyeingFactoryId/orderDateStart/orderDateEnd/deliveryDateStart/deliveryDateEnd；keyword 对 contract_no / client_name / product_code / product_name 做 OR 模糊（PostgreSQL LOWER+LIKE）；默认 order_date DESC；返回 PageResult（不含 colors）
- `GET /get?id=` — 详情，RespVO 内嵌 colors，按 line_no 排序
- `POST /create` — body 为 DyeingOrderSaveReqVO（colors 内嵌）；status 传 DRAFT 或 ACTIVE；返回 id
- `PUT /update` — 同结构（含 id）；仅 DRAFT 可更新，否则报 1_030_007_001；不存在报 1_030_007_000
- `DELETE /delete?id=` — 仅 DRAFT 可删，否则报 1_030_007_001；级联逻辑删除明细
- `PUT /effect?id=` — DRAFT → ACTIVE
- `PUT /complete?id=` — ACTIVE → COMPLETED
- `PUT /cancel?id=` — ACTIVE → CANCELLED

所有端点 `@PreAuthorize("@ss.hasPermission('lunaerp:production:dyeing-order:<action>')")`，action ∈ {query, create, update, delete}；状态操作复用 update 权限。返回 CommonResult / PageResult，VO 带 Swagger 与 jakarta.validation 注解，风格对齐 greigeorder / salescontract 模块。

### 关联校验与快照

- 创建/更新时校验 salesContractId 对应的销售合同存在（不存在报 1_030_007_002），从合同带出 contractNo、customerId、clientName 快照写入主表。
- 校验 dyeingFactoryId 对应的供应商存在（不存在报 1_030_007_002），保存 dyeingFactoryName 快照。
- 若传了 customerId（非从合同带出场景），校验客户存在。
- 选合同自动带出明细：前端在选中合同后调销售合同详情接口拿到 items，拷贝 productCode/productName/customerColor→customerColorNo、quantity→contractQty 到明细行，用户可改。

### 权限码

`lunaerp:production:dyeing-order:query` / `:create` / `:update` / `:delete`。

### 错误码（1-030-007 段，主表与明细共用）

- `1_030_007_000` — DYEING_ORDER_NOT_EXISTS（染色单不存在）
- `1_030_007_001` — DYEING_ORDER_STATUS_NOT_ALLOWED（当前状态不允许该操作）
- `1_030_007_002` — DYEING_ORDER_REFERENCE_NOT_FOUND（关联的{}不存在或不可用）

在 lunaerp 模块 ErrorCodeConstants 追加。段位全表已更新于 CONTEXT.md。

### 字典（1 个）

- `lunaerp_dyeing_order_status`：DRAFT 草稿(warning) / ACTIVE 进行中(success) / COMPLETED 已完成(info) / CANCELLED 已取消(danger)

投染数单位复用 `lunaerp_sales_contract_unit`（KG 千克 / M 米），不新建字典。

### 菜单与路由

SQL 菜单（ID 段 10220 起，可重复执行，先 DELETE 再 INSERT）：

- 10220 `染色单` 菜单（type=2，parent=10200 生产管理目录，component `lunaerp-elmui/production/dyeing-order/index`，componentName `LunaerpElmuiProductionDyeingOrder`，承载 query 权限）
- 10221/10222/10223 创建/修改/删除按钮（type=3）

工作台不进菜单：参照供应商工作台模式在前端静态隐藏路由注册 `/production/dyeing-order/workspace/:id?`（无 id 为新建、有 id 为查看/编辑）；列表行"查看/编辑"跳转到工作台。

### 前端页面结构与交互

- 列表页：搜索区为关键词输入 + 状态下拉 + 客户下拉 + 承染方下拉 + 单据日期区间 + 交货日期区间；列含单据日期/合同号/客户名称/承染方/产品编码/产品名称/状态 DictTag/交货日期/创建人/创建时间/操作；新建、编辑、删除二次确认按钮带 v-hasPermi；分页。
- 工作台页：多 Tab 容器（el-tabs），"基础信息"Tab 激活含：
  - 基本信息区（el-form 栅格）：销售合同远程选择（带出合同号/客户 + 自动生成明细行）、承染方远程选择（供应商）、单据日期、交货日期、产品编码、产品名称、成分、幅宽、克重、缩水率
  - 技术规格&色牢度区（el-form 栅格）：损耗率、pH 值、缩水率、水洗牢度、皂洗牢度、干摩擦牢度、湿摩擦牢度、日晒牢度、汗渍牢度、干洗牢度、六千沾色牢度、其他要求
  - 颜色明细区（el-table 可编辑行）：客户色号、颜色名称、本单染色数量、匹数、投染数、投染数单位(字典下拉)、合同数量(只读)、操作(删行)；底部"添加明细行"
  - 备注区
  - 操作栏（按状态显示按钮）：草稿态[保存草稿][下达][删除]、进行中态[完成][取消]、已完成/已取消全只读
- 其余 Tab（如 染色进度/检验记录/胚布出库/入库记录）显示"开发中"占位
- 只读模式（非草稿态查看）：全部控件 disabled，隐藏编辑按钮

### SQL 脚本

文件：`luna-srv/sql/postgresql/lunaerp-production-dyeing-order.sql`，参照 lunaerp-production-greige-order.sql 体例：

1. 2 张表 DDL（CREATE TABLE IF NOT EXISTS + 主键 DO 块判存 + 列注释 + 2 个序列 + 索引）
2. system_menu：10220 菜单、10221-10223 按钮（事务内先 DELETE 再 INSERT，父菜单 10200 已由胚布单脚本创建）
3. system_dict_type（id 2222，紧接胚布单 2221）+ system_dict_data（id 3571-3574，紧接胚布单 3568-3570）

## Testing Decisions

### 测试哲学

只测外部业务行为（Service 的可观察语义），不断言内部调用次数等实现细节；测试命名描述行为而非方法。

### 测试 seam（唯一接缝）

**Service 层纯 Mockito 单元测试 `DyeingOrderServiceImplTest`**：不启动 Spring 上下文、不连数据库，Mock DyeingOrderMapper + DyeingColorMapper + 销售合同/客户/供应商校验依赖（若 Service 直接调用对应 Mapper 或 Service）。不新建 MockMvc/嵌入式 PostgreSQL 测试基建。前端不做测试（lunaerp-elmui 为先行快速版，ADR-0001）。

Prior art：`CustomerServiceImplTest` / `SupplierServiceImplTest` / `SalesContractServiceImplTest`（同模块已落地的纯 Mockito 模式：@InjectMocks + ArgumentCaptor 验证落库对象语义、模拟 insert 回填主键）。

### 覆盖的行为

1. 创建：status 缺省 DRAFT；传 ACTIVE 直接下达；orderDate 缺省今天；关联合同/染厂不存在报 1_030_007_002；合同带出 contractNo/customerId/clientName 快照写入主表。
2. 更新：仅 DRAFT 可更新，非 DRAFT 报 1_030_007_001；不存在报 1_030_007_000；明细整体替换（先删后插）并重排 line_no。
3. 删除：仅 DRAFT 可删，非 DRAFT 报 1_030_007_001；级联逻辑删除明细。
4. 状态操作：effect（DRAFT→ACTIVE）、complete（ACTIVE→COMPLETED）、cancel（ACTIVE→CANCELLED）；非法流转报 1_030_007_001。
5. 聚合保存：create 时主表 insert 后明细按 dyeing_order_id 批量插入并重排 line_no；零明细合法。
6. 分页查询条件：keyword/status/customerId/dyeingFactoryId/日期区间正确透传到 Mapper 查询构造。
7. 详情：get 返回主表 + 嵌套 colors（按 line_no 排序）。

## Out of Scope

- 审批工作流与审批状态（旧系统 draft/submitted/approved/rejected）
- 胚布库存扣减与回冲（旧系统审批通过时按颜色明细÷(1-损耗)扣减胚布）
- 染缸批次卡（DyeingVat）与生产进度流转
- 检验单（InspectionInstruction）联动与生成
- 打印模板、Excel 导入导出、AI 智能导入
- 字段配置中心（extensionData 动态表单扩展字段）
- 多合同分配（contractAllocations JSON）
- 胚布品种关联（greigeTypeId）
- 工艺路线（ProcessTemplate / plannedStepsJson / routeConfirmed）
- 色牢度的字典化与单位标准化（本期均为自由文本）
- 生产计划字段（计划开工/完工日期、机台、班次）
- 并发编辑乐观锁/版本号
- 前端单元/组件测试；REST 集成测试与测试数据库基建
- 正式 UI（views/lunaerp/）版本
- 工作台其余 Tab 的实际功能（仅占位）

## Further Notes

### 已落地文档

- [CONTEXT.md](../../CONTEXT.md) — 已新增"生产管理：染色单"术语分区：染色单、染色颜色明细、染色技术规格与色牢度、承染方、染色单状态；并更新错误码段位表
- [ADR-0008](../../docs/adr/0008-dyeing-order-draft-state.md) — 染色单多 DRAFT 草稿态（与合同/胚布单三态刻意不一致）
- [ADR-0009](../../docs/adr/0009-dyeing-order-no-number.md) — 染色单无业务编号（与合同/胚布单/供应商都有编号刻意不一致）
- [ADR-0003](../../docs/adr/0003-sales-contract-item-aggregate-persistence.md) — 明细聚合整体保存模式（颜色明细沿用）
- [ADR-0001](../../docs/adr/0001-lunaerp-elmui-frontend-split.md) — 前端落在 lunaerp-elmui 先行快速版目录

### 实现顺序建议

1. SQL 脚本（2 表 + 序列/索引 + 10220 菜单 + 状态字典）
2. 后端 DO×2 + Mapper×2
3. VO 套装（SaveReqVO 内嵌 colors、PageReqVO、RespVO 内嵌 colors）+ 错误码常量
4. Service（关联校验与快照、聚合保存、状态操作与流转校验、级联删除）+ Controller（8 端点）
5. ServiceImpl 纯 Mockito 单元测试
6. 前端 API 与 TS 类型、dict.ts 枚举、隐藏路由
7. 前端列表页
8. 前端工作台页（基础信息 Tab + 颜色明细可编辑表格 + 状态操作按钮 + 占位 Tab）

### 参考

- 旧系统模型：`linaagi/server/prisma/schema.prisma`（DyeingInstruction + DyeingColor）、`linaagi/server/src/routes/production.ts`（染色单列表/保存逻辑）
- 旧系统前端：`linaagi/web/app/staff/components/production/dyeing/`
- 新系统后端模板：lunaerp 模块 `production/greigeorder`（同域包结构、状态字典、菜单 ID 段）、`business/salescontract`（聚合保存模式、关联快照）
- 新系统前端模板：`luna-ui/src/views/lunaerp-elmui/production/greige-order/`（列表页）、`luna-ui/src/views/lunaerp-elmui/baseinfo/supplier/workspace/`（多 Tab 工作台模式）
- SQL 模板：`luna-srv/sql/postgresql/lunaerp-production-greige-order.sql`
