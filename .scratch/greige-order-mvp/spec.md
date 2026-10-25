# 生产管理 - 胚布单 MVP（主表 CRUD + 颜色投胚分配聚合）

Status: ready-for-agent

## Problem Statement

业务侧需要在新 Luna ERP 系统里管理"生产管理-胚布单"。旧系统（`D:\work\luna\go0829\linaagi\`）已有完整实现：Prisma 模型 `GreigeInstruction`（胚布指示单）含约 35 个字段，颜色明细 `sourceColors` 以 JSON 文本存储，并串联了 AI 按合同拆单、智能导入、收胚入库、胚布检验、QR 打印、标签模板等长链路。但旧系统（Prisma + Next.js/React）与新系统（Spring Boot + MyBatis-Plus + PostgreSQL + Vue3 + Element Plus）技术栈不兼容，且旧实现大量字段为 String 文本、背负审批/完成率等多重状态。

业务侧希望先用新栈把"生产管理-胚布单"的最小可用闭环跑通：胚布单主表的增删改查 + 颜色投胚分配的聚合录入 + 自动编号 + 引用供应商/合同/胚布品种档案 + 字典/权限/菜单注册，作为生产管理域（区别于已落地的基础信息域、业务管理域）的第一个单据，也为二期 AI 拆单、收胚入库、检验等链路提供干净的聚合根。

## Solution

在 luna-srv 的 `lunasrv-module-lunaerp` 模块新增 `production.greigeorder` 子包（与 `baseinfo` / `business` 平级），落地胚布单主表与颜色投胚分配两张表、一套以胚布单为聚合根的 CRUD 服务；在 luna-ui 的 `views/lunaerp-elmui/production/greige-order/` 下落地列表页与整页新建/编辑表单（基本信息分区 + 颜色分配可编辑表格）。配套 PostgreSQL 脚本完成 DDL、"生产管理"顶级目录与菜单/按钮权限、1 个状态字典的初始化。

颜色分配不作为独立资源（沿用合同明细 ADR-0003 模式）：随单整体保存、级联逻辑删除、投胚需求由服务端按"成品量 ÷ (1−损耗)"权威重算。胚布单号 `PB+月日+3 位日流水` 按天取号、全量唯一、软删不释放（ADR-0006）。坯布厂/染厂引用供应商档案，选择器不做类型强过滤（ADR-0007）。

## User Stories

### 胚布单列表与检索

1. 作为生产调度，我想在"生产管理 → 胚布单"菜单看到单据列表（胚布单号、合同号、产品名称、坯布厂、指定染厂、计划投胚、交货日期、状态），以便掌握所有胚布单。
2. 作为生产调度，我想在搜索框输入关键词同时匹配胚布单号、合同号、产品名称、产品编号，以便只记其一也能找到单据。
3. 作为生产调度，我想按单据状态（进行中/已完成/已取消）筛选，以便聚焦执行中的单据。
4. 作为生产调度，我想列表默认按单据日期倒序、分页展示（每页默认 10/20 条），以便最新单据在最前。
5. 作为生产调度，我想列表每行有查看/编辑/删除操作，删除前弹二次确认，以便避免误删。

### 胚布单新建/编辑

6. 作为生产调度，我想点"新建胚布单"进入整页表单，以便录入完整信息。
7. 作为生产调度，我想新建时胚布单号自动生成（形如 PB0919001），也允许手工填写编号，以便兼容线下先编号再补录的场景。
8. 作为生产调度，我想手填编号与已有单据（含已删除单据）撞号时得到明确报错，以便避免编号重复。
9. 作为生产调度，我想在表单上点"获取编号"预览即将分配的自动编号，以便决定用自动号还是手填号。
10. 作为生产调度，我想坯布厂为必选，通过远程搜索从供应商档案中选择，选中后保存供应商 id 与公司名快照，以便单据与供应商档案关联。
11. 作为生产调度，我想指定染厂通过远程搜索选择但可留空，以便下单时染厂未定也能先建单。
12. 作为生产调度，我想坯布厂/染厂的远程搜索不限制供应商类型、选项显示类型标签辅助辨认，以便不会漏掉染整/后整理等类型字典中细分错位的供应商（ADR-0007）。
13. 作为生产调度，我想即使供应商档案日后改名，历史胚布单上仍显示建单时的公司名，以便保留建单事实。
14. 作为生产调度，我想通过远程搜索选择销售合同（可空），选中后保存合同 id 与合同号快照，以便胚布单与合同档案关联。
15. 作为生产调度，我想通过远程搜索选择所属胚布品种（可空、手工选择、不做自动识别），以便与胚布品种档案关联。
16. 作为生产调度，我想单据日期必填且缺省为今天、交货日期可选，以便快速建档。
17. 作为生产调度，我想录入产品编号、产品名称、成分、幅宽、克重、缩水率，以便完整描述织造品种规格。
18. 作为生产调度，我想录入匹重（数值、单位 KG、缺省 25），以便计算匹数。
19. 作为生产调度，我想填写计划投胚量后系统按 ceil(投胚量 ÷ 匹重) 自动算出匹数并回填，但仍允许手工修改匹数。
20. 作为生产调度，我想录入下单成品数量、预设损耗（百分比）、加工费，以便描述生产计划。
21. 作为生产调度，我想填写生产工艺备注与普通备注，以便记录织造指令。
22. 作为生产调度，我想编辑单据状态（进行中/已完成/已取消），以便手动维护执行状态。

### 颜色投胚分配

23. 作为生产调度，我想在单据表单里维护多行颜色投胚分配，以便逐颜色记录成品下单量与损耗。
24. 作为生产调度，我想每行包含：颜色、成品下单量(KG)、损耗率、投胚需求(KG)、备注，以便完整描述颜色级拆分。
25. 作为生产调度，我想行损耗率留空时缺省取主单预设损耗，以便减少重复录入。
26. 作为生产调度，我想投胚需求按"成品量 ÷ (1−损耗率)"实时计算，保存时由服务端权威重算落库，以便任何客户端都无法写入不一致的数值。
27. 作为生产调度，我想计划投胚量与颜色投胚需求合计不强制相等（表单可展示合计供参考），因为两者业务口径不同。
28. 作为生产调度，我想允许保存零颜色分配的单据，以便先建单头、后补拆分。
29. 作为生产调度，我想能新增/删除颜色行、调整行顺序（line_no），编辑已有单据时看到全部颜色行并整体修改后一次保存。

### 查看与删除

30. 作为生产调度，我想查看单据时看到主表全部字段与颜色分配列表（只读），以便核对内容。
31. 作为生产调度，我想删除单据后主表与其颜色行一并逻辑删除、列表不再显示，以便清理作废单据。
32. 作为生产调度，我想已删除单据的编号不会被新单据重新使用，以便编号永远指向同一份单据。
33. 作为生产调度，我想通过浏览器直接访问编辑页 URL（如 /production/greige-order/edit/123）打开特定单据，以便收藏或分享链接。

### 管理与分权

34. 作为系统管理员，我想执行一份 SQL 脚本即可建表、建"生产管理"目录与胚布单菜单/按钮、初始化状态字典，且脚本可重复执行，以便部署无手工配置。
35. 作为系统管理员，我想胚布单有独立权限码 query/create/update/delete，以便按角色分权。
36. 作为系统管理员，我想单据状态来自系统字典并用 DictTag 渲染，以便业务方后续可维护。
37. 作为开发，我想主表与颜色行 DO 继承 TenantBaseDO，以便多租户隔离由框架自动处理。

## Implementation Decisions

### 模块与包路径

- 后端模块：`lunasrv-module-lunaerp`（已存在，新增子包）
- 后端包：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.production.greigeorder`；颜色行 DO/Mapper 同包或 `.color` 子包，无独立 Controller/Service
- 前端视图：`luna-ui/src/views/lunaerp-elmui/production/greige-order/`（`index.vue` 列表页 + `edit/index.vue` 整页表单）
- 前端 API：`luna-ui/src/api/lunaerp-elmui/production/greige-order/index.ts`
- 沿用 ADR-0001：落在 `lunaerp-elmui/` 先行快速版目录，未来正式 UI 重写到 `views/lunaerp/`

### 数据模型

**主表 `lunaerp_production_greige_order`**（继承 TenantBaseDO：id/creator/create_time/updater/update_time/deleted/tenant_id）：

| 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|
| id | int8 | - | 主键，序列 `lunaerp_production_greige_order_seq` |
| order_no | varchar(32) | ✓ | 胚布单号，全量唯一（ADR-0006） |
| sales_contract_id | int8 | - | 逻辑关联销售合同，不建数据库外键 |
| contract_no | varchar(32) | - | 合同号快照 |
| greige_factory_id | int8 | ✓ | 承织坯布厂（供应商 id） |
| greige_factory_name | varchar(128) | ✓ | 坯布厂公司名快照 |
| dyeing_factory_id | int8 | - | 指定染厂（供应商 id），可空 |
| dyeing_factory_name | varchar(128) | - | 染厂公司名快照 |
| order_date | date | ✓ | 单据日期，缺省今天 |
| delivery_date | date | - | 交货日期 |
| product_code | varchar(64) | - | 产品编号 |
| product_name | varchar(128) | - | 产品名称 |
| composition | varchar(128) | - | 成分 |
| width | varchar(64) | - | 幅宽（保留文本，兼容"160cm 有效"等写法） |
| gsm | varchar(64) | - | 克重（保留文本） |
| shrinkage | varchar(64) | - | 缩水率（描述性文本） |
| piece_weight | numeric(10,2) | ✓ | 匹重 KG，缺省 25 |
| piece_count | int4 | - | 匹数；前端按 ceil(投胚量÷匹重) 自动算，可手改；服务端不重算 |
| planned_qty | numeric(18,2) | ✓ | 计划投胚量 KG |
| finished_qty | numeric(18,2) | - | 下单成品数量 KG |
| suggested_loss | numeric(6,4) | - | 预设损耗，小数（0.05 = 5%），缺省 0 |
| processing_fee | numeric(18,2) | - | 加工费 |
| greige_type_id | varchar(64) | - | 所属胚布品种（档案 id 为字符串），手选、不自动识别 |
| production_remark | varchar(1000) | - | 生产工艺备注 |
| remark | varchar(1000) | - | 备注 |
| status | varchar(16) | ✓ | 字典 lunaerp_greige_order_status，缺省 ACTIVE |

砍掉的旧字段：isDemo、price（与 processing_fee 重复）、completionRate（无收胚数据无法计算）、suggestedLoss 之外的 AI 字段、suitableLoomModels 适用机型、sourceColors JSON（规范化为子表）、contractAllocations 多合同分配 JSON、businessOwnership、extensionData、date 之外的时间戳冗余。

**颜色投胚分配表 `lunaerp_production_greige_order_color`**（继承 TenantBaseDO）：

| 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|
| id | int8 | - | 主键，序列 `lunaerp_production_greige_order_color_seq` |
| order_id | int8 | ✓ | 所属胚布单 |
| line_no | int4 | ✓ | 行序号（保存时按数组顺序重排） |
| color | varchar(64) | ✓ | 颜色 |
| finished_qty | numeric(18,2) | - | 该颜色成品下单量 KG，null 按 0 |
| loss_rate | numeric(6,4) | - | 行损耗率（小数）；为空时取主单 suggestedLoss，再缺省 0 |
| required_qty | numeric(18,2) | - | 投胚需求，保存时服务端重算 |
| remark | varchar(256) | - | 行备注 |

砍掉的旧 JSON 字段：unit（统一 KG）、originalQtyM、calculatedKg（同 finished_qty）、qty（同 finished_qty 语义）。

索引：主表普通唯一索引 `(tenant_id, order_no)`（**不带 WHERE deleted=0**，ADR-0006）；颜色行普通索引 `(order_id)`；主表辅助索引 `(status)`、`(order_date)`。

### 编号规则（ADR-0006）

- 格式：`PB` + 两位月 + 两位日 + 顺序号 3 位起（如 PB0919001，超 999 自然扩位）。
- 自动生成：以服务器当天日期构造前缀，租户内扫描该前缀的最大数字后缀 +1；**扫描范围包含已删除单据**，需绕过 MyBatis-Plus 逻辑删除过滤（自定义 Mapper 查询），代码处注释说明意图。
- 手填：非空时按原值；与任意单据（含已删除）撞号报 GREIGE_ORDER_NO_DUPLICATE，不顺延。
- 自动号并发撞唯一索引时有限次顺延重试。
- 不使用 Redis、不使用数据库序列取号（序列仅用于主键）。

### 聚合保存与投胚需求重算（沿用 ADR-0003）

- `GreigeOrderSaveReqVO` 内嵌 `colors: List<GreigeOrderColorSaveReqVO>`，无颜色行独立端点。
- create/update 均在一个事务内：保存主表 → 按 order_id 逻辑删除旧颜色行 → 插入本次提交的全部行。
- 保存时逐行确定损耗率（行值为空取主单 suggestedLoss，再缺省 0），重算 `required_qty = finished_qty ÷ (1 − loss_rate)`（finished_qty null 按 0；损耗为 null 按 0；损耗 ≥1 时按业务非法处理或兜底为 0，实现以 BigDecimal 精确计算、保留两位小数），重排 line_no。
- **planned_qty 独立保存**，不与颜色 required_qty 合计做一致性校验；前端可展示合计供参考。
- piece_count 以前端传值为准，服务端不重算（允许人工值）。
- 零颜色行合法。
- 删除单据：逻辑删除主表，并按 order_id 逻辑删除全部颜色行；不区分当前状态。
- `get` 详情嵌套返回 colors；`page` 列表不返回颜色行。

### 引用校验

保存（create/update）时校验，任一不过抛对应错误码：

1. greige_factory_id：供应商必须存在且属本租户；必填。
2. dyeing_factory_id 非空：供应商必须存在；不校验供应商类型（ADR-0007）。
3. sales_contract_id 非空：销售合同必须存在。
4. greige_type_id 非空：胚布品种必须存在。
5. 手填 order_no 查重（含已删除）。

坯布厂/染厂选择器列表附加 `status=ACTIVE`（停用供应商不进新单选项）；后端引用存在性校验不限状态，与既有模块校验口径对齐（实现时以实际引用的 service 方法为准）。

### Controller 与 API 契约

基础路径 `/lunaerp/production/greige-order`：

- `GET /page` — 参数 pageNo/pageSize/keyword/status；keyword 对 order_no / contract_no / product_name / product_code 做 OR 模糊（PostgreSQL LOWER+LIKE）；默认 order_date DESC；返回 PageResult（不含颜色行）
- `GET /get?id=` — 详情，RespVO 内嵌 colors，按 line_no 排序
- `POST /create` — body 为 GreigeOrderSaveReqVO（colors 内嵌）；orderNo 为空走自动号；返回 id
- `PUT /update` — 同结构（含 id）；不存在报 GREIGE_ORDER_NOT_EXISTS
- `DELETE /delete?id=` — 级联逻辑删除
- `GET /get-next-no` — 返回下一个自动编号供前端预览

所有端点 `@PreAuthorize("@ss.hasPermission('lunaerp:production:greige-order:<action>')")`，action ∈ {query, create, update, delete}；get-next-no 复用 query。返回 CommonResult / PageResult，VO 带 Swagger 与 jakarta.validation 注解，风格对齐 customer/sales-contract 模块。

### 权限码

`lunaerp:production:greige-order:query` / `:create` / `:update` / `:delete`。

### 错误码（1-030-006 段，主表与颜色行共用）

- `1_030_006_000` — GREIGE_ORDER_NOT_EXISTS（胚布单不存在）
- `1_030_006_001` — GREIGE_ORDER_NO_DUPLICATE（胚布单号【{}】已存在）
- `1_030_006_002` — GREIGE_ORDER_REFERENCE_NOT_FOUND（关联的{}不存在或不可用）

在 lunaerp 模块 ErrorCodeConstants 追加。占位符区分引用对象（坯布厂/染厂/销售合同/胚布品种），避免错误码膨胀。

### 字典（1 个，system_dict_* 机制）

- `lunaerp_greige_order_status`：ACTIVE 进行中(success) / COMPLETED 已完成(info) / CANCELLED 已取消(danger)

前端在 `utils/dict.ts` 增加对应 DICT_TYPE 枚举；列表与表单统一 DictTag / getStrDictOptions。

### 菜单与路由

SQL 菜单（ID 段 10200 起，可重复执行，先 DELETE 再 INSERT）：

- 10200 `生产管理` 目录（type=1，path `/production`，icon `ep:set-up`，sort 102，与 10000 基本信息、10100 业务管理平级）
- 10210 `胚布单` 菜单（type=2，parent=10200，component `lunaerp-elmui/production/greige-order/index`，componentName `LunaerpElmuiProductionGreigeOrder`，承载 query 权限）
- 10211/10212/10213 创建/修改/删除按钮（type=3）

整页表单不进菜单：在前端静态隐藏路由（remaining.ts）注册 `/production/greige-order/edit/:id?`（name `LunaerpElmuiProductionGreigeOrderEdit`），无 id 为新建、有 id 为编辑；列表页"查看"以只读模式打开同一页。

### 前端页面结构与交互

- 列表页：对齐 sales-contract/index.vue；搜索区为关键词输入 + 状态下拉；8 列（胚布单号/合同号/产品名称/坯布厂/指定染厂/计划投胚/交货日期/状态 + 操作）；新建按钮与操作按钮带 v-hasPermi。
- 编辑页：顶部标题与返回；分区：
  1. 基本信息（编号 + 获取编号、合同远程搜索、单据/交货日期）
  2. 坯布厂与染厂（远程 el-select，选项显示"供应商编号 公司名（类型标签）"，不做类型过滤，附加 ACTIVE；坯布厂必填、染厂可空）
  3. 品种规格（产品编号远程搜索产品档案并回填品名/成分/幅宽/克重，回填后可改；缩水率；匹重）
  4. 生产计划（计划投胚量输入后自动 ceil 算匹数并回填可改；成品数量、预设损耗百分比、加工费；所属胚布品种手选）
  5. 颜色投胚分配（el-table 可编辑行：颜色、成品量、损耗%（留空显示"同主单"）、投胚需求只读实时计算、行备注、行删除、添加行；底部展示投胚需求合计仅供参考）
  6. 生产工艺备注 + 备注 + 状态
  - 保存/取消；只读模式（查看）：全部控件 disabled，隐藏保存按钮。
- 远程搜索数据源：供应商分页 `/lunaerp/baseinfo/supplier/page`（search + status=ACTIVE，不传 supplierType）；销售合同分页；产品分页；胚布品种列表接口。
- 数值精度：数量/金额两位小数、千分位展示；损耗前端按百分比、传输为小数；用字符串/number 与后端 BigDecimal 传输。

### SQL 脚本

文件：`luna-srv/sql/postgresql/lunaerp-production-greige-order.sql`，参照 lunaerp-business-sales-contract.sql 体例：

1. 2 张表 DDL（CREATE TABLE IF NOT EXISTS + 主键 DO 块判存 + 列注释 + 2 个序列 + 索引）
2. system_menu：10200 目录、10210 菜单、10211-10213 按钮（事务内先 DELETE 再 INSERT）
3. system_dict_type（id 2221）+ system_dict_data（id 3568-3570，共 3 条）

## Testing Decisions

### 测试哲学

只测外部业务行为（Service 的可观察语义），不断言内部调用次数等实现细节；测试命名描述行为而非方法。

### 测试 seam（唯一接缝）

**Service 层纯 Mockito 单元测试 `GreigeOrderServiceImplTest`**：不启动 Spring 上下文、不连数据库，Mock 胚布单/颜色 Mapper 与引用的供应商/合同/胚布品种校验入口。不新建 MockMvc/嵌入式 PostgreSQL 测试基建。前端不做测试（lunaerp-elmui 为先行快速版，ADR-0001）。

Prior art：`SalesContractServiceImplTest` / `CustomerServiceImplTest`（同模块已落地的纯 Mockito 模式）。

### 覆盖的行为

1. 自动编号：orderNo 为空时生成 `PB{MM}{DD}xxx`，序号 = 扫描到的当天前缀最大后缀 +1；扫描 Mock 含已删除记录。
2. 手填编号：原值保留；查重命中（含命中已删除单据）抛 1_030_006_001。
3. create/update/delete/get 不存在校验：抛 1_030_006_000。
4. 引用校验：坯布厂不存在/染厂不存在/合同不存在/胚布品种不存在抛 1_030_006_002。
5. 颜色行聚合：create 主表 insert 后按单 id 批量插入并重排 line_no；update 先按 order_id 逻辑删除旧行再插新行。
6. 投胚需求重算：required_qty = finished_qty ÷ (1−loss_rate)；行损耗为空取主单 suggestedLoss；finished_qty null 按 0；请求体携带的 required_qty 被覆盖。
7. plannedQty 不被颜色合计覆盖；pieceCount 原值保留（服务端不重算）。
8. 默认值：orderDate 缺省今天、status 缺省 ACTIVE、pieceWeight 缺省 25、suggestedLoss 缺省 0。
9. 删除：主表与颜色行级联逻辑删除。
10. 分页查询条件：keyword/status 正确传入 Mapper 查询构造。

## Out of Scope

- AI 按合同拆单（analyze-contract/analyze-contracts）与多合同合并分配（contractAllocations）
- AI 智能导入向导
- QR 打印 / 批量打印 / 标签模板中心
- 收胚入库联动（StockIn / GreigeBatch 库存）与胚布库存资源池
- 胚布检验 / 匹级检验入口
- 完成率 completionRate（无收胚数据）
- 适用机型 suitableLoomModels 与 AI 排产
- 审批流与审批状态（二期追加）
- isDemo / businessOwnership / extensionData 动态自定义字段
- greige/stats 统计接口
- 坯布厂/染厂的供应商类型强过滤（ADR-0007 明确不做）
- plannedQty 与颜色合计强一致
- 并发编辑乐观锁/版本号；Excel 导入导出；批量删除
- 前端单元/组件测试；REST 集成测试与测试数据库基建
- 正式 UI（views/lunaerp/）版本

## Further Notes

### 已落地文档

- [GLOSSARY.md](../../docs/contexts/production/GLOSSARY.md) — 已新增"生产管理：胚布单"术语分区：胚布单、颜色投胚分配、承织坯布厂、指定染厂、计划投胚量、胚布单号、胚布单状态；更新模块落点、字典、错误码段位表
- [ADR-0006](../../docs/adr/0006-greige-order-no-daily-sequence.md) — 胚布单号按日取号（刻意区别于 CH/SUP 按年）
- [ADR-0007](../../docs/adr/0007-supplier-role-reference-no-type-filter.md) — 供应商角色引用不做类型强过滤

### 实现顺序建议

1. SQL 脚本（2 表 + 序列/索引 + 10200/10210 菜单 + 1 字典）
2. 后端 DO×2 + Mapper×2（含含已删除行的取号/查重查询）
3. VO 套装 + 错误码常量
4. Service（编号生成/查重、引用校验、聚合保存、投胚需求重算、级联删除）+ Controller
5. ServiceImpl 纯 Mockito 单元测试
6. 前端 API 与 TS 类型、dict.ts 枚举、remaining.ts 隐藏路由
7. 前端列表页
8. 前端整页编辑表单（基本信息/工厂/规格/计划 + 颜色可编辑表格 + 只读模式）

### 参考

- 旧系统模型：`D:\work\luna\go0829\linaagi\server\prisma\schema.prisma`（GreigeInstruction，L2150 起）
- 旧系统后端：`D:\work\luna\go0829\linaagi\server\src\routes\production.ts`（L697-1130 胚布单 CRUD）、`server\src\lib\greige-payload.ts`、`server\src\lib\greige-instruction-no.ts`
- 旧系统前端：`D:\work\luna\go0829\linaagi\web\app\staff\components\production\GreigeEditModal.tsx`、`GreigeInputs.tsx`、`greige\GreigeListView.tsx`、`greigeDyeingFactoryOptions.ts`
- 新系统后端模板：lunaerp 模块 `business.salescontract`（聚合保存/编号/重算）、`baseinfo.supplier`（供应商引用数据）
- 新系统前端模板：`luna-ui/src/views/lunaerp-elmui/business/sales-contract/`
- SQL 模板：`luna-srv/sql/postgresql/lunaerp-business-sales-contract.sql`
