# 基础信息-供应商信息（档案）模块 - MVP CRUD

Status: ready-for-agent

## Problem Statement

业务侧需要在新的 Luna ERP 系统里管理供应商档案。旧系统（linaagi，Prisma + Next.js/React）已有供应商档案：供应商主表、联系人子表、`SUP-` 编号、供应商类型字典（18 项经营类别）、机型、银行信息、定位地图、工厂照片与付款资料、财务统计、草稿/审批、批量导入等，但与新系统（Spring Boot + MyBatis-Plus + PostgreSQL + Vue3 + Element Plus）技术栈不兼容。业务侧希望先用新栈把供应商基础档案 + 联系人 CRUD 与自动编号跑通，作为后续采购入库、付款记录等业务环节引用供应商主数据的起点；同时要求与几天前刚落地的客户信息模块保持同构（列表页、多 Tab 工作台、独立联系人资源、多租户、软删除、字典、菜单权限同一套模式）。

## Solution

在新项目 luna-srv 的 `lunasrv-module-lunaerp` 模块下新增 `baseinfo.supplier` 与 `baseinfo.suppliercontact` 两个子包，落地供应商与供应商联系人两套独立 CRUD，并提供 `SUP+年份+4位序号` 的自动编号（可手填、租户内全量唯一、软删不重用，机制与销售合同编号同构，见 ADR-0005）；在 luna-ui 的 `views/lunaerp-elmui/`（先行快速实现版目录，见 ADR-0001）下落地供应商列表页 + 供应商工作台（6 Tab：激活"基础档案"与"联系人"，"入库记录/付款记录/关联品种/位置分布"为"开发中"占位）。配套 SQL 脚本完成 DDL、序列、菜单注册、供应商类型字典 18 项初始化。

本次同时完成一次跨模块命名统一：客户模块的公司名字段 `company` 正名为 `companyName`（列 `company_name`），销售合同模块引用客户公司名做名称快照的 3 处联动同步修改，使客户/供应商两个孪生档案模块字段命名一致。

## User Stories

1. 作为采购，我想新建供应商档案（公司名称必填），以便把纱线厂、染厂、门市部等往来单位录入系统。
2. 作为采购，我想新建供应商时系统自动生成形如 `SUP20260001` 的供应商编号（SUP+年份+4位顺序号），以便无需自己操心编号规则。
3. 作为采购，我想在新建页只读预览"下一个编号"，以便在保存前知道将分配的编号。
4. 作为采购，我想在新建时手工填写编号，以便承接旧系统的历史编号。
5. 作为采购，我想手填编号与已有编号（含已删除供应商的编号）重复时得到明确报错，以便换号后重试。
6. 作为采购，我想删除供应商后其编号不会被新供应商重新使用，以便纸质单据、聊天记录里出现过的编号永远只指向一家供应商。
7. 作为采购，我想新建时不填简称，系统自动用公司名称回填简称，以便减少重复输入。
8. 作为采购，我想在供应商列表用一个搜索词匹配公司名称/简称/编号/电话任一关键词，以便快速定位供应商。
9. 作为采购，我想在列表按供应商类型（18 种经营类别）筛选，以便只看某一类工厂。
10. 作为采购，我想在列表按状态（启用/停用）筛选，以便默认只看当前合作中的供应商。
11. 作为采购，我想在列表看到编号、公司名称（简称副标题）、类型、联系人/电话、地址、状态，以便一眼识别供应商。
12. 作为采购，我想列表分页显示，以便应对大量供应商数据。
13. 作为采购，我想新建/编辑时供应商类型是下拉选择（18 项来自系统字典），以便保证分类规范统一。
14. 作为采购，我想供应商类型可以不选（可空），以便建档时还不确定类别的情况下不被阻塞。
15. 作为采购，我想填写"主营机型/设备说明"自由文本，以便记录供应商的设备能力（如 34寸28针、喷水织机）。
16. 作为采购，我想填写供应商电话、地址、备注，以便记录基础往来信息。
17. 作为采购，我想在表单里手动切换供应商状态 ACTIVE/INACTIVE，以便停用不再合作的供应商。
18. 作为采购，我想点列表行"查看"进入供应商工作台，以便在多 Tab 容器里查看完整信息。
19. 作为采购，我想点列表行"编辑"直接进入工作台"基础档案"Tab，以便快速修改供应商信息。
20. 作为采购，我想修改供应商时可以变更编号，变更后服务端重新做唯一性校验，以便纠正录错的编号。
21. 作为采购，我想删除供应商前有二次确认，以便避免误删。
22. 作为采购，我想在列表勾选多行后批量删除，以便清理无效数据。
23. 作为采购，我想在工作台"联系人"Tab 看到该供应商的全部联系人，以便掌握对方对接人。
24. 作为采购，我想在联系人 Tab 新增联系人（姓名必填、职位/电话/邮箱/备注/是否主联系人），以便记录接单、财务、档口等不同对接人。
25. 作为采购，我想在联系人 Tab 编辑、删除单条联系人（删除有二次确认），以便应对人员变动。
26. 作为采购，我想标记某个联系人为主联系人，以便突出首选对接人。
27. 作为采购，我想联系人是单条独立增删改、不需要随供应商表单整体保存，以便在 Tab 内增量维护。
28. 作为采购，我想在工作台看到"入库记录""付款记录""关联品种""位置分布"四个 Tab 标注"开发中"，以便了解未来功能规划。
29. 作为采购，我想通过浏览器 URL 直接打开特定供应商的工作台，以便收藏或分享链接。
30. 作为系统管理员，我想执行一个 SQL 脚本一次性完成建表、序列、菜单、字典初始化，以便部署模块时无需手工配置。
31. 作为系统管理员，我想 SQL 脚本可重复执行（CREATE IF NOT EXISTS、菜单/字典先 DELETE 再 INSERT、列迁移用 DO 块判存），以便在已有客户模块的库上重跑不报错。
32. 作为系统管理员，我想客户表存量列 `company` 在执行新脚本时自动迁移为 `company_name`，以便客户模块与供应商模块命名统一且不丢数据。
33. 作为系统管理员，我想"供应商信息"菜单挂在"基本信息"目录下与"客户信息"并列，以便业务用户在导航树找到入口。
34. 作为系统管理员，我想供应商与供应商联系人拥有各自独立的 query/create/update/delete 权限码，以便按角色分权。
35. 作为开发，我想供应商与联系人 DO 继承 TenantBaseDO，以便多租户隔离由框架自动处理。
36. 作为开发，我想供应商编号取号与查重扫描显式包含已删除行，以便与 ADR-0005 的全量唯一口径一致。
37. 作为开发，我想公司名称字段在客户与供应商两个模块统一为 `companyName`，以便两个孪生模块代码与 API 字段一致、降低未来维护成本。
38. 作为开发，我想统一重命名后销售合同的客户名称快照(clientName)回填逻辑行为不变，以便合同单据上的客户名展示不受影响。
39. 作为开发，我想删除供应商时先置 INACTIVE 再逻辑删除，与客户模块删除模式一致，以便忽略 deleted 列的审计查询仍能看到生命周期变化。

## Implementation Decisions

### 模块与包坐标

- 后端模块：`lunasrv-module-lunaerp`（已存在，本次新增子包）
- 后端包：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.baseinfo.supplier` 与 `...baseinfo.suppliercontact`（一段式命名，与 customer/customercontact、producttag 风格一致）
- 前端视图：`luna-ui/src/views/lunaerp-elmui/baseinfo/supplier/`
- 前端 API：`luna-ui/src/api/lunaerp-elmui/baseinfo/supplier/`（含联系人 API 文件）
- 分层与命名完全镜像客户模块：DO / SaveReqVO / PageReqVO / RespVO / Controller / Service / ServiceImpl / Mapper，联系人独立 Controller（模式依据 ADR-0002）
- `lunaerp-elmui` 为先行快速实现版目录，正式 UI 未来仍落 `views/lunaerp/`（ADR-0001）

### 数据模型

**供应商表 `lunaerp_baseinfo_supplier`**（继承 `TenantBaseDO`，标准列 creator/create_time/updater/update_time/deleted/tenant_id）：

| 字段（Java） | 列名 | 类型 | 必填 | 备注 |
|---|---|---|---|---|
| id | id | int8 | - | 主键，`@KeySequence("lunaerp_baseinfo_supplier_seq")` |
| supplierNo | supplier_no | varchar | ✓ | 供应商编号，普通唯一索引 `UNIQUE (tenant_id, supplier_no)`（**不带** deleted 条件，含软删行，见 ADR-0005） |
| companyName | company_name | varchar | ✓ | 公司/档口名称，允许重名，无业务唯一约束 |
| shortName | short_name | varchar | - | 简称；为空时 service 用 companyName 回填 |
| phone | phone | varchar | - | 电话 |
| address | address | varchar | - | 地址 |
| supplierType | supplier_type | varchar | - | 字典 `lunaerp_supplier_type`，**可空** |
| machineType | machine_type | varchar | - | 主营机型/设备说明自由文本 |
| remark | remark | varchar | - | 备注 |
| status | status | varchar | - | 缺省 ACTIVE；删除时先置 INACTIVE 再逻辑删除 |

索引：主键 + `UNIQUE (tenant_id, supplier_no)` 全量唯一 + status 普通索引 + supplier_type 普通索引。

**供应商联系人表 `lunaerp_baseinfo_supplier_contact`**（继承 `TenantBaseDO`）：

| 字段（Java） | 列名 | 类型 | 必填 | 备注 |
|---|---|---|---|---|
| id | id | int8 | - | 主键，序列 `lunaerp_baseinfo_supplier_contact_seq` |
| supplierId | supplier_id | int8 | ✓ | 关联供应商 id（Long，逻辑关联，不建数据库外键） |
| name | name | varchar | ✓ | 姓名 |
| position | position | varchar | - | 职位 |
| phone | phone | varchar | - | 电话 |
| email | email | varchar | - | 邮箱 |
| remark | remark | varchar | - | 备注 |
| isPrimary | is_primary | int2 | - | 是否主联系人，缺省 0；同一供应商允许多条主联系人，后端不强校验唯一 |

联系人作为独立子资源：单条 CRUD 独立端点，不随供应商保存载荷整体替换；供应商删除时不做联系人级联（MVP 无强约束，与客户模块一致）。

### 编号规则（SupplierNo）

- 形如 `SUP20260001`：`SUP` + 年份 + 4 位顺序号，超 9999 自然扩位；租户内唯一。
- 新建缺省服务端生成（supplierNo 传空时）；也接受手填。
- 取号沿用销售合同已验证的数字后缀扫描方案（不引数据库序列/Redis）：在当前租户、当前年份前缀下取数字后缀最大值 +1；自动号撞唯一索引时有限次顺延重试。
- 取号扫描与手填号查重**包含已删除行**，需绕过 MyBatis-Plus 逻辑删除自动过滤（自定义 Mapper 查询，代码处注释标明意图）；手填撞已删除号同样报编号重复错误。
- 更新时允许修改 supplierNo，变更后做同样的全量查重（排除自身）。
- 提供只读、不占号的"预览下一个编号"接口，多次调用在无新建时返回相同值。

### Controller 与 API 契约

**供应商 Controller**：`/lunaerp/baseinfo/supplier`

- `GET /page` — 分页，参数 `pageNo/pageSize/search/supplierType/status`；`search` 对 supplier_no/company_name/short_name/phone 做 OR ILIKE（PostgreSQL `LOWER(...) LIKE LOWER(...)`）；`supplierType` 等值过滤；`status` 等值过滤；按 create_time 倒序
- `GET /get?id=` — 单条详情
- `POST /create` — 新建，body 为 SupplierSaveReqVO；编号空则自动生成、shortName 空则用 companyName 回填、status 缺省 ACTIVE
- `PUT /update` — 修改（含 id）；编号变更时全量查重；shortName 回填规则同新建
- `DELETE /delete?id=` — 单删：校验存在 → 置 INACTIVE → 逻辑删除
- `DELETE /delete-list?ids=...` — 批量删除，逐条置 INACTIVE 后整体逻辑删除
- `GET /get-next-no` — 预览当前租户当年下一个编号（只读不占号）

**供应商联系人 Controller**：`/lunaerp/baseinfo/supplier-contact`

- `GET /page` — 参数 `pageNo/pageSize/supplierId`（必填），主联系人优先排序
- `GET /get?id=` / `POST /create`（body 含 supplierId）/ `PUT /update` / `DELETE /delete?id=`（直接逻辑删除，无状态联动）

所有端点带 `@PreAuthorize` 权限校验，权限码前缀：

- `lunaerp:baseinfo:supplier:{query|create|update|delete}`
- `lunaerp:baseinfo:supplier-contact:{query|create|update|delete}`

### 错误码

新增 `1-030-005-xxx` 段位（supplier 与 supplier-contact 共用），追加到 lunaerp 模块 `ErrorCodeConstants`：

- `1_030_005_000` — SUPPLIER_NOT_EXISTS（供应商不存在）
- `1_030_005_001` — SUPPLIER_NO_DUPLICATE（供应商编号已被占用，请修改编号或重新获取）
- `1_030_005_002` — SUPPLIER_CONTACT_NOT_EXISTS（供应商联系人不存在）

段位全表：001 产品 / 002 胚布品种 / 003 销售合同 / 004 客户 / 005 供应商。

### 字典

新增一个字典 type：`lunaerp_supplier_type`（供应商类型），前端用 `<DictTag>` 渲染、表单用字典 options 下拉。Seed 18 项（value 编码稳定，后续由系统字典后台维护，MVP 不做旧系统的前端自助增删/待审核）：

| label | value |
|---|---|
| 纱线厂 | YARN_MILL |
| 坯布厂 | GREIGE_MILL |
| 工贸一体 | INTEGRATED |
| 绣花厂 | EMBROIDERY_MILL |
| 染厂 | DYEING_MILL |
| 扎染 | TIE_DYEING |
| 印花厂 | PRINTING_MILL |
| 烫金厂 | HOT_STAMPING_MILL |
| 压花厂 | EMBOSSING_MILL |
| 复合厂 | LAMINATING_MILL |
| 外贸公司 | FOREIGN_TRADE |
| 检验仓库 | INSPECTION_WAREHOUSE |
| 拉毛摇粒厂 | BRUSHING_MILL |
| 水洗厂 | WASHING_MILL |
| 小缸厂 | SMALL_VAT_MILL |
| 门市部 | STORE |
| 检测中心 | TESTING_CENTER |
| 货运 | FREIGHT |

状态 ACTIVE/INACTIVE 不建字典（与客户一致）。

### 菜单与字典 ID 分配（已核对现有脚本无冲突）

- 10050 供应商信息菜单（type=2，parent_id=10000 基本信息，path `supplier`，icon `ep:shop`，component `lunaerp-elmui/baseinfo/supplier/index`，component_name `LunaerpElmuiBaseinfoSupplier`，query 权限挂菜单本身，sort 取客户之后）
- 10051-10053 供应商 创建/修改/删除 按钮节点
- 10060-10063 供应商联系人 query/创建/修改/删除 四个按钮节点（type=3、path/component 为空，全部平铺直接挂在 10050 下；type=3 不进前端路由树，不会把供应商信息菜单变成带子菜单的父菜单；2026-09-19 由原"10060 type=2 隐藏节点 + 按钮挂其下"方案修正，见 issue 04）
- system_dict_type id 2220；system_dict_data id 3550-3567（18 项）

### SQL 脚本

新增 `luna-srv/sql/postgresql/lunaerp-baseinfo-supplier.sql`，风格与客户脚本一致、可重复执行：两张表 DDL（主键 DO 块判存 + 索引 + 全列 COMMENT）、两个序列、菜单事务段、字典事务段。脚本头部保留"仅做语法自查，禁止在共享远程库直接执行"的注释约定。

### 跨模块重构：company → companyName

客户模块公司名字段统一正名（DO/VO 字段 `companyName`、数据库列 `company_name`、前端类型字段 `companyName`），影响面：

- 后端客户模块：CustomerDO、CustomerSaveReqVO、CustomerRespVO 字段；CustomerMapper 分页 search 的列引用；CustomerServiceImpl 中 shortName 回填读取。
- 后端销售合同模块：SalesContractServiceImpl 创建/更新时从客户取公司名回填合同 clientName 快照的两处调用（getter 改名，快照语义与结果不变）。
- 前端客户模块：客户 API 类型定义、列表页列绑定与简称副标题、基础档案 Tab 表单字段与校验。
- 前端销售合同：合同编辑页客户选择器的选项 label 字段引用。
- 客户 SQL 脚本：列定义/注释改为 company_name，并增加**可重复执行的列迁移 DO 块**（查 information_schema 判断旧列 company 存在且新列不存在时 `ALTER TABLE ... RENAME COLUMN company TO company_name`），保证全新安装与已有库升级两条路径都成立。
- 测试：同步修订 CustomerServiceImplTest、SalesContractServiceImplTest 中的字段访问与测试数据构造。

### 前端视图结构

- 列表页：搜索词输入框 + 供应商类型下拉 + 状态下拉 + 新建/批量删除按钮（v-hasPermi）；列：编号 / 公司名称（简称副标题）/ 类型(DictTag) / 联系人电话 / 地址 / 状态 / 操作（查看、编辑、删除二次确认）；分页、create_time 倒序。
- 工作台容器：6 Tab —— 基础档案、联系人（激活）；入库记录、付款记录、关联品种、位置分布（共用占位组件，"开发中"）。
- 基础档案 Tab：编号（新建时可通过预览接口带出占位提示、允许手填；编辑时可改）、公司名称、简称、供应商类型、主营机型、电话、地址、状态、备注；保存后返回列表。
- 联系人 Tab：内联表格 + 新建/编辑弹窗 + 单条删除，镜像客户 ContactsTab。
- 路由：列表 name `LunaerpElmuiBaseinfoSupplier`；工作台 `supplier/workspace/:id`，name `LunaerpElmuiBaseinfoSupplierWorkspace`。

## Testing Decisions

### 测试哲学

只测业务外部行为与关键业务语义（编号生成/查重、回填、缺省值、删除顺序），不测实现细节；不依赖 Spring 上下文与数据库。

### 测试 seam（唯一 seam：Service 层纯 Mockito 单元测试）

- 框架：JUnit5 + Mockito（`@Mock` Mapper、`@InjectMocks` Service、ArgumentCaptor 捕获落库对象），与仓库现有三个测试类同构；分页 Wrapper 断言时按 SalesContractServiceImplTest 的做法在 setUp 初始化 MyBatis-Plus TableInfo 缓存。
- Prior art：`CustomerServiceImplTest`、`CustomerContactServiceImplTest`、`SalesContractServiceImplTest`（后两者编号/回填语义直接可参照）。

**新增 SupplierServiceImplTest**：

- 自动取号：编号为空时生成 SUP+年份+4 位序号；序号取扫描 max+1；前置补零、超 9999 自然扩位
- 手填号：按原值保存；与未删除/已删除供应商撞号均抛 1_030_005_001（验证查重入口包含软删行）
- 自动号撞号：模拟取号冲突时顺延重试、超过重试次数报错
- 预览接口：复用扫描入口、不调用 insert（不占号）
- shortName 为空用 companyName 回填、非空保留；status 缺省 ACTIVE、显式 INACTIVE 保留
- get/update 不存在抛 1_030_005_000；update 编号变更撞号抛 1_030_005_001
- delete/deleteList：不存在抛错；存在时验证先 updateById(INACTIVE) 再 deleteByIds 的调用顺序
- 分页：search/supplierType/status 透传查询构造

**新增 SupplierContactServiceImplTest**：镜像 CustomerContactServiceImplTest（supplierId/name 必填校验、不存在抛 1_030_005_002、逻辑删除）。

**同步修订**（重命名联动，保证编译与断言通过）：CustomerServiceImplTest、SalesContractServiceImplTest 中所有 company 访问改为 companyName。

### 前端测试

跳过。lunaerp-elmui 为先行快速实现版（ADR-0001），UI 易变，仓库当前无前端测试基建。

## Out of Scope

- 工作台 4 个占位 Tab 的实际功能（入库记录/付款记录/关联品种/位置分布）
- 银行信息（bankName/bankAccount）与字段级脱敏权限
- 统一社会信用代码、合作状态字段（客户模块有，供应商 MVP 不引入）
- 经纬度、地图定位与"使用当前位置"
- 工厂照片、付款资料等附件上传
- 财务统计字段 totalPaid/totalOwed 及其统计逻辑
- 供应商登录账号关联（userId）、供应商门户
- 草稿态 DRAFT 与审批流（PENDING_APPROVAL/REJECTED 等）
- extensionData 自定义字段与字段配置中心
- 批量 Excel 导入/导出、KPI 看板、按使用频次排序
- businessOwnership 事业部归属与隔离
- 前端供应商类型自助增删与待审核流程（仅通过系统字典后台维护）
- 供应商删除时联系人级联处理（MVP 不建外键、不级联，与客户一致）
- MockMvc 集成测试与嵌入式 PostgreSQL 测试基建
- 前端单元/组件测试
- 正式 UI 版本（views/lunaerp/ 的重写）

## Further Notes

### 已落地文档

- [CONTEXT.md](../../CONTEXT.md) — 已新增"供应商与联系人"术语节（Supplier / SupplierContact / SupplierNo / SupplierType / SupplierWorkspace / machineType），并同步模块落点、租户、软删除、字典、错误码段、companyName 命名统一
- [docs/adr/0005-supplier-no-full-uniqueness.md](../../docs/adr/0005-supplier-no-full-uniqueness.md) — 供应商编号全量唯一、基础档案采用单据级编号规则的决策
- 关联：ADR-0001（lunaerp-elmui 目录拆分）、ADR-0002（联系人独立 Controller）、ADR-0004（合同编号全量唯一，供应商编号机制同构）

### 参考实现

- 旧系统后端：linaagi `server/prisma/schema.prisma`（Supplier / SupplierContact 模型）、`server/src/routes/supplier.ts`、`server/src/services/supplier-list.service.ts`
- 旧系统前端：linaagi `web/app/staff/components/supplier-apple/`（SupplierAppleList、SupplierAppleCreate、supplier-apple.calc.ts 中 18 项 DEFAULT_SUPPLIER_TYPES）
- 新系统同构样板：luna-srv 客户模块（baseinfo.customer/customercontact 全套）与销售合同编号实现（business.salescontract 取号/查重/getNextNo）；luna-ui 客户模块视图与 API

### 实现顺序建议

1. SQL：新增供应商脚本 + 客户脚本 company_name 列迁移块
2. 后端供应商：DO/VO/Mapper/Service/Controller + 错误码 + 编号取号（复用合同机制）
3. 后端供应商联系人独立 CRUD
4. 后端重命名联动：客户模块 company→companyName + 销售合同两处快照回填
5. 后端测试：新增两个 ServiceImplTest + 修订两个既有测试类，编译与全量测试通过
6. 前端 API 层：supplier/index.ts + supplier/contact.ts
7. 前端列表页 + 工作台容器（6 Tab）+ 基础档案 Tab（含编号预览）+ 联系人 Tab + 占位 Tab
8. 前端重命名联动：客户 API/视图 + 合同编辑页选择器
9. 前后端联调验证：编号自动/手填/撞号、搜索筛选、联系人独立维护、删除停用、权限菜单
