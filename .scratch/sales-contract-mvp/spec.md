# 业务管理 - 销售合同 MVP（主表 CRUD + 商品明细聚合）

Status: ready-for-agent

## Problem Statement

业务侧需要在新 Luna ERP 系统里管理销售合同。旧系统（oldserver/oldweb）已有完整的销售合同实现：Prisma 模型 `SalesContract` 含约 40 个字段、商品明细以 JSON 字符串存储，并串联了审批、回款催收、总指示、出口单证、发货/发票分摊等长链路。但旧系统（Prisma + React）与新系统（Spring Boot + MyBatis-Plus + PostgreSQL + Vue3 + Element Plus）技术栈不兼容，且旧实现背负多重状态（审批 status / 业务进度 progress / 回款 paymentStatus）与大量历史兼容字段。

业务侧希望先用新栈把"业务管理-销售合同"的最小可用闭环跑通：合同主表的增删改查 + 商品明细的聚合录入 + 自动编号 + 字典/权限/菜单注册，作为业务管理域（区别于已落地的基础信息域）的第一个单据，也为二期审批、回款、出口单证等链路提供干净的聚合根。

## Solution

在 luna-srv 的 `lunasrv-module-lunaerp` 模块新增 `business.salescontract` 子包（与 `baseinfo` 平级），落地销售合同主表与合同明细两张表、一套以合同为聚合根的 CRUD 服务；在 luna-ui 的 `views/lunaerp-elmui/business/sales-contract/` 下落地列表页与整页新建/编辑表单（基本信息分区 + 可编辑明细表格）。配套 PostgreSQL 脚本完成 DDL、"业务管理"顶级目录与菜单/按钮权限、5 个字典的初始化。

明细不作为独立资源（刻意与客户联系人相反，见 ADR-0003）：随合同整体保存、级联逻辑删除、汇总金额由服务端按明细权威重算。合同编号全量唯一、软删不释放（刻意与产品编号的部分唯一索引相反，见 ADR-0004）。

## User Stories

### 合同列表与检索

1. 作为销售，我想在"业务管理 → 销售合同"菜单看到合同列表（合同号、客户、贸易类型、签订日期、总数量、总金额、状态），以便掌握所有销售合同。
2. 作为销售，我想列表金额列显示币种与金额（如 RMB 12,500.00），以便区分多币种合同。
3. 作为销售，我想在搜索框输入关键词同时匹配合同号、客户名称、客户合同号，以便只记其一也能找到合同。
4. 作为销售，我想按合同状态（进行中/已完成/已取消）筛选，以便聚焦在执行的合同。
5. 作为销售，我想按贸易类型（内销/外销）筛选，以便区分内外贸业务。
6. 作为销售，我想列表默认按签订日期倒序、分页展示（每页默认 10/20 条），以便最新合同在最前。
7. 作为销售，我想列表每行有查看/编辑/删除操作，删除前弹二次确认，以便避免误删。

### 合同新建/编辑

8. 作为销售，我想点"新建合同"进入整页表单，以便录入信息完整的合同。
9. 作为销售，我想新建时合同编号自动生成（形如 CH20260001），也允许我手工填写编号，以便兼容线下先编号再补录的场景。
10. 作为销售，我想手填编号与已有合同（含已删除合同）撞号时得到明确报错，以便避免编号重复。
11. 作为销售，我想在表单上点"获取编号"预览即将分配的自动编号，以便决定用自动号还是手填号。
12. 作为销售，我想客户为必选，通过远程搜索从客户档案中选择客户，以便合同与客户档案关联。
13. 作为销售，我想选中客户后自动带出客户名称（快照）、联系人、地址，且允许我手工修改联系人和地址，以便减少重复录入又能记录合同上的特殊约定。
14. 作为销售，我想选择客户后即使该客户档案日后改名，历史合同上仍显示签约时的客户名称，以便保留签约事实。
15. 作为销售，我想填写客户合同号/PO 号，以便对照客户方的采购单号。
16. 作为销售，我想通过远程搜索选择业务员（系统用户，可空），以便记录合同负责人。
17. 作为销售，我想签订日期必填且缺省为今天、约定交期可选，以便快速建档。
18. 作为销售，我想选择计价单位（KG/M，缺省 KG）与币种（RMB/USD/EUR/HKD，缺省 RMB），以便正确计价。
19. 作为销售，我想选择贸易类型（内销缺省/外销），以便标记业务方向。
20. 作为销售，我想选择订单属性（大货缺省/样布），以便区分批量订单与打样。
21. 作为销售，我想编辑合同状态（进行中/已完成/已取消），以便手动维护履约状态。

### 商品明细

22. 作为销售，我想在合同表单里维护多行商品明细，以便逐行记录品种、数量与价格。
23. 作为销售，我想每行明细包含：产品编号、品名、客户色号、成分、门幅、克重、数量、单价、其他费用、行金额、备注，以便完整描述布匹品种与计价。
24. 作为销售，我想在明细行通过产品编号远程搜索产品库，选中后自动回填品名/成分/门幅/克重/单价，以便快速录入且与产品档案一致。
25. 作为销售，我想回填后的明细字段仍可手工修改，也允许直接手填产品库中不存在的编号，以便兼容定制品种。
26. 作为销售，我想行金额按"数量 × 单价 + 其他费用"实时计算、合同总数量与总金额在表单底部实时汇总预览，以便边录边核。
27. 作为销售，我想保存时服务端按明细重算汇总（即使前端传了汇总值也以服务端为准），以便任何客户端都无法写入与明细不一致的金额。
28. 作为销售，我想允许保存零明细合同（汇总为 0），以便先建合同头、后补品种明细。
29. 作为销售，我想能新增/删除明细行、调整行顺序（line_no），以便整理合同内容。
30. 作为销售，我想编辑已有合同时看到其全部明细并整体修改后一次保存，以便无需逐行调接口。

### 查看与删除

31. 作为销售，我想查看合同时看到主表全部字段与明细列表（只读），以便核对合同内容。
32. 作为销售，我想删除合同后主表与其明细一并逻辑删除、列表不再显示，以便清理作废合同。
33. 作为销售，我想已删除合同的编号不会被新合同重新使用，以便对外沟通中编号永远指向同一份合同。
34. 作为销售，我想通过浏览器直接访问编辑页 URL（如 /business/sales-contract/edit/123）打开特定合同，以便收藏或分享链接。

### 管理与分权

35. 作为系统管理员，我想执行一份 SQL 脚本即可建表、建"业务管理"目录与销售合同菜单/按钮、初始化字典，且脚本可重复执行，以便部署无手工配置。
36. 作为系统管理员，我想销售合同有独立权限码 query/create/update/delete，以便按角色分权（如销售只看不删）。
37. 作为系统管理员，我想合同状态/贸易类型/订单属性/单位/币种均来自系统字典并用 DictTag 渲染，以便业务方后续可扩充枚举。
38. 作为开发，我想合同与明细 DO 继承 TenantBaseDO，以便多租户隔离由框架自动处理。
39. 作为开发，我想合同分页接口预留 customerId 过滤参数，以便二期客户工作台"合同"Tab 零改动接线（本期不做该 Tab UI）。
40. 作为开发，我想编号生成复用产品模块已验证的"租户内前缀扫描 + 数字后缀取 max+1"方案（但扫描包含已删除行），以便不引入 Redis/序列取号的额外复杂度。

## Implementation Decisions

### 模块与包路径

- 后端模块：`lunasrv-module-lunaerp`（已存在，新增子包）
- 后端包：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.business.salescontract`；明细 DO/Mapper 与合同同包或 `.item` 子包，无独立 Controller/Service
- 前端视图：`luna-ui/src/views/lunaerp-elmui/business/sales-contract/`（`index.vue` 列表页 + `edit/index.vue` 整页表单）
- 前端 API：`luna-ui/src/api/lunaerp-elmui/business/sales-contract/index.ts`
- 沿用 ADR-0001：落在 `lunaerp-elmui/` 先行快速版目录，未来正式 UI 重写到 `views/lunaerp/`

### 数据模型

**合同主表 `lunaerp_business_sales_contract`**（继承 TenantBaseDO：id/creator/create_time/updater/update_time/deleted/tenant_id）：

| 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|
| id | int8 | - | 主键，序列 `lunaerp_business_sales_contract_seq` |
| contract_no | varchar(32) | ✓ | 合同编号，全量唯一（见下） |
| customer_id | int8 | ✓ | 逻辑关联客户档案，不建数据库外键 |
| client_name | varchar(128) | ✓ | 客户公司名快照（建单时复制，客户改名不回写） |
| client_contact | varchar(64) | - | 客户联系人（选客户带出，可改） |
| client_address | varchar(256) | - | 客户地址（选客户带出，可改） |
| client_contract_no | varchar(64) | - | 客户合同号 / PO 号 |
| salesperson_user_id | int8 | - | 业务员，关联 system 用户 |
| sign_date | date | ✓ | 签订日期，缺省今天 |
| delivery_date | date | - | 约定交期 |
| unit | varchar(16) | ✓ | 计价单位，字典 lunaerp_sales_contract_unit，缺省 KG |
| currency | varchar(8) | ✓ | 币种，字典 lunaerp_sales_contract_currency，缺省 RMB |
| trade_type | varchar(16) | ✓ | 字典 lunaerp_sales_contract_trade_type，缺省 DOMESTIC |
| order_type | varchar(16) | ✓ | 字典 lunaerp_sales_contract_order_type，缺省 BULK |
| total_quantity | numeric(18,2) | ✓ | 总数量，服务端按明细重算，缺省 0 |
| total_amount | numeric(18,2) | ✓ | 总金额，服务端按明细重算，缺省 0 |
| remarks | varchar(1000) | - | 备注 |
| status | varchar(16) | ✓ | 字典 lunaerp_sales_contract_status，缺省 ACTIVE |

砍掉的旧字段：头部客户款号（款号语义归明细行）、附件 attachment_url、合同条款 JSON、动态扩展字段、7 个外贸专属字段（INCOTERM/付款方式/起运港/目的港/英文名称/英文地址/银行信息）、审批/进度/回款三套状态字段。

**合同明细表 `lunaerp_business_sales_contract_item`**（继承 TenantBaseDO）：

| 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|
| id | int8 | - | 主键，序列 `lunaerp_business_sales_contract_item_seq` |
| contract_id | int8 | ✓ | 所属合同 |
| line_no | int4 | ✓ | 行序号（保存时按数组顺序重排） |
| product_code | varchar(64) | - | 产品编号（可来自产品库或手填） |
| product_name | varchar(128) | - | 品名 |
| customer_color | varchar(64) | - | 客户色号 |
| composition | varchar(128) | - | 成分 |
| width | varchar(64) | - | 门幅 |
| weight | varchar(64) | - | 克重 |
| quantity | numeric(18,2) | - | 数量（允许空，缺省按 0 参与汇总） |
| unit_price | numeric(18,2) | - | 单价 |
| other_fees | numeric(18,2) | - | 其他费用 |
| amount | numeric(18,2) | - | 行金额，保存时服务端重算 |
| remark | varchar(256) | - | 行备注 |

砍掉的旧行字段：image、machineFee、smallVatFee、pieces、supplierCode、行级 unit/客户合同号。

索引：主表普通唯一索引 `(tenant_id, contract_no)`（**不带 WHERE deleted=0**，ADR-0004）；明细表普通索引 `(contract_id)`；主表辅助索引 `(status)`、`(sign_date)`。

### 编号规则

- 格式：`CH` + 4 位年份 + 顺序号 4 位起（如 CH20260001，超 9999 自然扩位）。
- 自动生成：租户内扫描当前年份 `CH{year}` 前缀的最大数字后缀 +1；**扫描范围包含已删除合同**，需绕过 MyBatis-Plus 逻辑删除过滤（自定义 Mapper 查询），并在代码处注释说明意图（ADR-0004），防止被当疏漏"修复"。
- 手填：非空且不为占位符时按原值；与任意合同（含已删除）撞号报 SALES_CONTRACT_NO_DUPLICATE，不顺延。
- 自动号并发撞唯一索引时有限次顺延重试。
- 取号逻辑沿用产品模块 getNextCode 的数字后缀比较方式，不使用 Redis、不使用数据库序列取号（序列仅用于主键）。

### 聚合保存与金额重算（ADR-0003）

- `SalesContractSaveReqVO` 内嵌 `items: List<SalesContractItemSaveReqVO>`，无明细独立端点。
- create/update 均在一个事务内：保存主表 → 按 contract_id 删除旧明细（逻辑删除）→ 插入本次提交的全部明细。
- 保存时逐行重算 `amount = quantity × unit_price + other_fees`（null 按 0），重排 line_no；主表 `total_quantity = Σ quantity`、`total_amount = Σ amount`；**前端传入的主表汇总值一律忽略**。
- 零明细合法：汇总为 0。
- 删除合同：逻辑删除主表，并按 contract_id 逻辑删除全部明细；不做状态联动（无 INACTIVE 语义），不区分合同当前状态。
- `get` 详情嵌套返回 items；`page` 列表不返回明细。

### Controller 与 API 契约

基础路径 `/admin-api/lunaerp/business/sales-contract`：

- `GET /page` — 参数 pageNo/pageSize/keyword/status/tradeType/customerId(预留)；keyword 对 contract_no / client_name / client_contract_no 做 OR 模糊（PostgreSQL LOWER+LIKE）；默认 sign_date DESC；返回 PageResult（不含 items）
- `GET /get?id=` — 详情，RespVO 内嵌 items，按 line_no 排序
- `POST /create` — body 为 SalesContractSaveReqVO（items 内嵌）；contractNo 为空走自动号；返回 id
- `PUT /update` — 同结构（含 id）；不存在报 SALES_CONTRACT_NOT_EXISTS
- `DELETE /delete?id=` — 级联逻辑删除
- `GET /get-next-no` — 返回下一个自动编号供前端预览

所有端点 `@PreAuthorize("@ss.hasPermission('lunaerp:business:sales-contract:<action>')")`，action ∈ {query, create, update, delete}；get-next-no 复用 query。返回 CommonResult / PageResult，VO 带 Swagger 与 jakarta.validation 注解，风格对齐 customer 模块。

### 权限码

`lunaerp:business:sales-contract:query` / `:create` / `:update` / `:delete`。

### 错误码（1-030-003 段，主表与明细共用）

- `1_030_003_000` — SALES_CONTRACT_NOT_EXISTS（销售合同不存在）
- `1_030_003_001` — SALES_CONTRACT_NO_DUPLICATE（销售合同编号【{}】已存在）

在 lunaerp 模块 ErrorCodeConstants 追加。段位全表：001 产品 / 002 胚布品种 / 003 销售合同（本模块）/ 004 客户。

### 字典（5 个，system_dict_* 机制）

- `lunaerp_sales_contract_status`：ACTIVE 进行中(success) / COMPLETED 已完成(info) / CANCELLED 已取消(danger)
- `lunaerp_sales_contract_trade_type`：DOMESTIC 内销 / EXPORT 外销
- `lunaerp_sales_contract_order_type`：BULK 大货 / SAMPLE 样布
- `lunaerp_sales_contract_unit`：KG 千克 / M 米
- `lunaerp_sales_contract_currency`：RMB 人民币 / USD 美元 / EUR 欧元 / HKD 港币

前端在 `utils/dict.ts` 增加对应 DICT_TYPE 枚举；列表与表单统一 DictTag / getStrDictOptions。

### 菜单与路由

SQL 菜单（ID 段 10100 起，可重复执行，先 DELETE 再 INSERT）：

- 10100 `业务管理` 目录（type=1，path `/business`，icon `ep:briefcase`，与 10000 基本信息平级）
- 10110 `销售合同` 菜单（type=2，parent=10100，component `lunaerp-elmui/business/sales-contract/index`，componentName `LunaerpElmuiBusinessSalesContract`，承载 query 权限）
- 10111/10112/10113 创建/修改/删除按钮（type=3）

整页表单不进菜单：参照客户工作台模式在前端静态隐藏路由（remaining.ts）注册 `/business/sales-contract/edit/:id?`（name `LunaerpElmuiBusinessSalesContractEdit`），无 id 为新建、有 id 为编辑；列表页"查看"以只读模式打开同一页。

### 前端页面结构与交互

- 列表页：useTable + useCrudSchemas 模式（对齐 customer/index.vue）；搜索区为关键词输入 + 状态下拉 + 贸易类型下拉；7 列（合同号/客户/贸易类型/签订日期/总数量/总金额含币种/状态 + 操作）；新建/删除按钮带 v-hasPermi；订单属性不进列表。
- 编辑页：顶部标题与返回；"基本信息"分区（el-form 两/三列栅格）+ "商品明细"分区（el-table 可编辑行：产品编号远程 el-select 调产品分页接口并回填、数量/单价/其他费用 el-input-number、行金额只读实时计算、行删除、底部"添加明细行"）+ 底部汇总栏（总数量/总金额实时预览）+ 备注 + 保存/取消。
- 客户选择：远程 el-select 调客户分页接口；选中后回填 clientName/clientContact/clientAddress，后两者可改。
- 业务员：远程下拉调 system 用户 simple-list（可空）。
- 编号输入框旁"获取编号"按钮调 get-next-no 回填；用户手工输入则不覆盖。
- 数值精度统一两位小数、千分位展示；金额输入与汇总用 decimal 语义（避免浮点误差，可用字符串/number 与后端 BigDecimal 传输）。
- 只读模式（查看）：全部控件 disabled，隐藏保存按钮。

### SQL 脚本

文件：`luna-srv/sql/postgresql/lunaerp-business-sales-contract.sql`，参照 lunaerp-baseinfo-customer.sql 体例：

1. 2 张表 DDL（CREATE TABLE IF NOT EXISTS + 主键 DO 块判存 + 列注释 + 2 个序列 + 索引）
2. system_menu：10100 目录、10110 菜单、10111-10113 按钮（事务内先 DELETE 再 INSERT）
3. system_dict_type（id 2210-2214）+ system_dict_data（id 3530 起，共 13 条：3+2+2+2+4）

## Testing Decisions

### 测试哲学

只测外部业务行为（Service 的可观察语义），不断言内部调用次数等实现细节；测试命名描述行为而非方法。

### 测试 seam（唯一接缝）

**Service 层纯 Mockito 单元测试 `SalesContractServiceImplTest`**：不启动 Spring 上下文、不连数据库，Mock 合同/明细 Mapper。不新建 MockMvc/嵌入式 PostgreSQL 测试基建（lunaerp 模块当前无该基建，product 无测试）。前端不做测试（lunaerp-elmui 为先行快速版，ADR-0001）。

Prior art：`CustomerServiceImplTest` / `CustomerContactServiceImplTest`（同模块已落地的纯 Mockito 模式：@InjectMocks + ArgumentCaptor 验证落库对象语义、模拟 insert 回填主键）。

### 覆盖的行为

1. 自动编号：contractNo 为空时生成 `CH{当前年}xxxx`，序号 = 扫描到的当年最大后缀 +1；扫描 Mock 含已删除记录（验证取号调用的是含已删除的查询入口）。
2. 手填编号：原值保留；查重命中（含命中已删除合同）时抛 1_030_003_001。
3. create/update 不存在校验：更新/删除/获取不存在合同抛 1_030_003_000。
4. 明细聚合：create 时主表 insert 后明细按合同 id 批量插入并重排 line_no；update 时先按 contract_id 逻辑删除旧明细再插新明细。
5. 汇总重算：多行明细下 totalQuantity/totalAmount 等于服务端重算值；请求体携带的汇总被覆盖；零明细时汇总为 0。
6. 行金额：amount = quantity × unit_price + other_fees，null 数值按 0。
7. 默认值：signDate 缺省今天、status 缺省 ACTIVE、tradeType 缺省 DOMESTIC、orderType 缺省 BULK、unit 缺省 KG、currency 缺省 RMB。
8. 删除：删除合同时明细随主表级联逻辑删除。
9. 分页查询条件对象：keyword/status/tradeType/customerId 正确传入 Mapper 查询构造（customerId 为二期预留，本期也要保证透传）。

## Out of Scope

- 审批流与审批状态（旧系统 draft/submitted/approved/rejected）
- 业务进度 progress、回款状态 paymentStatus、回款策略、催收记录（CollectionRecord）
- 总指示/生产指示 MasterPlan/MasterPlanItem 及其联动
- 出口单证全套（报关/清关/物流）与 7 个外贸专属字段（tradeType 仅标记）
- 发货、发票分摊（InvoiceContractAllocation）、入库与合同财务指标统计
- 合同附件上传（infra 文件）、合同条款 JSON、动态自定义字段（extensionData）
- 旧的内/外销双模式表单分区（本期外销仅多一个枚举值）
- 客户工作台"合同"Tab UI（占位保留；仅预留 page 接口 customerId 参数）
- 商品明细的独立端点/独立权限（ADR-0003 明确不做）
- 并发编辑乐观锁/版本号
- Excel 导入导出、打印模板、时间线、批量删除
- 前端单元/组件测试；REST 集成测试与测试数据库基建
- 正式 UI（views/lunaerp/）版本

## Further Notes

### 已落地文档

- [CONTEXT.md](../../CONTEXT.md) — 已新增"业务管理：销售合同"术语分区：销售合同、合同明细、贸易类型、订单属性、合同状态、业务员、客户名称快照、合同编号；并更新模块落点、字典、错误码段位表
- [ADR-0003](../../docs/adr/0003-sales-contract-item-aggregate-persistence.md) — 明细聚合整体保存、不设独立 Controller（与 ADR-0002 联系人刻意相反）
- [ADR-0004](../../docs/adr/0004-sales-contract-no-uniqueness-includes-deleted.md) — 编号全量唯一、软删不释放、取号含已删除行（与 product 部分唯一索引刻意相反）
- [ADR-0001](../../docs/adr/0001-lunaerp-elmui-frontend-split.md)、[ADR-0002](../../docs/adr/0002-customer-contact-independent-controller.md) — 前置背景

### 实现顺序建议

1. SQL 脚本（2 表 + 序列/索引 + 10100/10110 菜单 + 5 字典）
2. 后端 DO×2 + Mapper×2（含"含已删除行"的取号/查重查询）
3. VO 套装 + 错误码常量
4. Service（编号生成/查重、聚合保存、汇总重算、级联删除）+ Controller
5. ServiceImpl 纯 Mockito 单元测试
6. 前端 API 与 TS 类型、dict.ts 枚举、remaining.ts 隐藏路由
7. 前端列表页
8. 前端整页编辑表单（基本信息 + 明细可编辑表格 + 汇总 + 只读模式）

### 参考

- 旧系统模型：`oldserver/prisma/schema.prisma`（SalesContract 及其关联）、`oldserver/src/routes/sales-contracts.ts`（编号生成与列表/保存逻辑）
- 旧系统前端：`oldweb/app/staff/components/business/SalesContractCreateBasic.tsx`、`SalesContractCreateProducts.tsx`、`SalesContractTypes.ts`、`SalesContractTab.tsx`
- 新系统后端模板：lunaerp 模块 `baseinfo/customer`（分层/VO/权限/错误码）、`baseinfo/product`（编号 getNextCode 数字后缀扫描）
- 新系统前端模板：`luna-ui/src/views/lunaerp-elmui/baseinfo/customer/`（index.vue 列表、workspace 整页路由模式）
- SQL 模板：`luna-srv/sql/postgresql/lunaerp-baseinfo-customer.sql`
