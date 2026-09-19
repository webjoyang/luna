# 客户信息（中心）模块 - P1 CRUD

Status: ready-for-agent

## Problem Statement

业务侧需要在新的 Luna ERP 系统里管理客户档案。旧系统（oldserver/oldweb）的客户中心承载了客户基础档案、联系人、合同、寄样、画像分析、位置分布、拜访记录等多 Tab 工作台，但旧系统是 Prisma + React 实现，与新系统（Spring Boot + MyBatis-Plus + Vue3 + Element Plus）技术栈不兼容。业务侧希望先用新栈把客户基础档案 + 联系人 CRUD 跑通，验证后端服务、字典、权限、菜单注册等链路通畅，作为后续 7-Tab 工作台完整迁移的起点。

## Solution

在新项目 luna-srv 的 `lunasrv-module-lunaerp` 模块下新增 `baseinfo.customer` 与 `baseinfo.customercontact` 两个子包，落地客户与联系人两套独立 CRUD；在 luna-ui 的 `views/lunaerp-elmui/`（先行快速实现版目录，区别于未来正式 UI 的 `views/lunaerp/`）下落地客户列表页 + 客户工作台（多 Tab 容器，激活"基础档案"与"联系人"两个 Tab，其余 5 Tab 占位"开发中"）。配套 SQL 脚本完成 DDL、菜单注册、字典数据初始化。

## User Stories

1. 作为销售，我想新建客户档案（公司名、客户类型、电话、地址等），以便把潜在客户信息录入系统。
2. 作为销售，我想在新建客户时只填公司名和客户类型两项必填字段，其它字段可选，以便快速建档不阻塞流程。
3. 作为销售，我想新建客户时如果不填客户简称，系统自动用公司名作为简称，以便减少重复输入。
4. 作为销售，我想在客户列表页通过搜索框输入公司名/简称/电话/统一社会信用代码任一关键词智能搜索客户，以便快速定位客户。
5. 作为销售，我想在客户列表页按状态（启用/停用）筛选客户，以便只看当前活跃客户。
6. 作为销售，我想在客户列表页看到公司名（含简称副标题）、客户类型、电话、合作状态、创建时间，以便一眼识别客户。
7. 作为销售，我想点列表行的"查看"按钮进入客户工作台，以便在多 Tab 容器里查看客户完整信息。
8. 作为销售，我想点列表行的"编辑"按钮直接进入工作台的"基础档案" Tab，以便快速修改客户信息。
9. 作为销售，我想点列表行的"删除"按钮删除客户，删除前有二次确认，以便避免误删。
10. 作为销售，我想在列表页勾选多行后批量删除，以便清理无效数据。
11. 作为销售，我想列表分页显示，每页默认 20 条，以便应对大量客户数据。
12. 作为销售，我想新建客户时客户类型/行业/规模/客户来源/合作状态字段是下拉选择（来自系统字典），以便保证数据规范性。
13. 作为销售，我想在工作台"基础档案" Tab 编辑客户所有字段并保存，以便维护客户最新信息。
14. 作为销售，我想在客户工作台切换到"联系人" Tab，看到该客户的所有联系人列表，以便掌握客户组织里的人。
15. 作为销售，我想在联系人 Tab 新增联系人（姓名、职位、电话、邮箱、备注、是否主联系人），以便记录客户方对接人。
16. 作为销售，我想在联系人 Tab 编辑单条联系人，以便更新联系人变动（如升职、换电话）。
17. 作为销售，我想在联系人 Tab 删除单条联系人（二次确认），以便清理无效联系人。
18. 作为销售，我想标记某个联系人为"主联系人"（isPrimary），以便在列表/详情突出显示首选对接人。
19. 作为销售，我想客户工作台显示 7 个 Tab（基础档案/联系人/合同/寄样/画像分析/位置分布/拜访记录），其中后 5 个标注"开发中"，以便了解未来功能规划。
20. 作为销售，我想通过浏览器直接访问客户工作台 URL（如 `/lunaerp-elmui/customer/workspace/123`）打开特定客户，以便收藏或分享链接。
21. 作为系统管理员，我想通过执行 SQL 脚本一次性建表、注册菜单、初始化字典，以便部署模块时不需手工配菜单字典。
22. 作为系统管理员，我想 SQL 脚本可重复执行（CREATE IF NOT EXISTS + DELETE 后 INSERT），以便部署失败后重跑不报错。
23. 作为系统管理员，我想客户与联系人菜单在"基本信息"目录下作为子菜单显示，以便业务用户在导航树里找到入口。
24. 作为系统管理员，我想客户与联系人有独立权限码（query/create/update/delete），以便按角色分权（如销售只能看不能删）。
25. 作为系统管理员，我想客户删除时先把状态置为 INACTIVE 再逻辑删除，以便忽略 deleted 列的审计查询仍能看到生命周期变化。
26. 作为开发，我想客户与联系人 DO 继承 TenantBaseDO，以便多租户隔离由框架自动处理。
27. 作为开发，我想联系人作为独立 Controller 而非嵌套在客户保存载荷里整体替换，以便支持单条联系人的增量编辑与独立权限分权。
28. 作为开发，我想前端视图落在 `views/lunaerp-elmui/` 而非 `views/lunaerp/`，以便明确这是先行快速实现版，未来正式 UI 重写到 `views/lunaerp/` 时不污染正式落点。
29. 作为开发，我想客户状态用两态 ACTIVE/INACTIVE 而非旧系统的三态（带审批），以便首期不引入审批流复杂度。
30. 作为开发，我想客户表无 code 字段、无唯一约束（除主键），以便与旧系统对齐（旧系统无 code）并允许同名客户。
31. 作为开发，我想客户字段不含 `businessOwnership`，以便首期不引入业务部隔离逻辑。
32. 作为开发，我想错误码段位与产品模块 `1-030-001-xxx` 平级且全模块唯一（customer 与 customer-contact 共用）。（落地修订：原设计记为 `1-030-002-xxx`，发现该段已被胚布品种 `GreigeErrorCodeConstants` 先行占用，客户改用 `1-030-004-xxx`，详见文末「错误码段重号修订」。）

## Implementation Decisions

### 模块与包路径

- 后端模块：`lunasrv-module-lunaerp`（已存在，本次新增子包）
- 后端包：`com.luna.lunasrv.module.lunaerp.{controller,service,dal.mysql,dal.dataobject}.baseinfo.customer` 与 `...baseinfo.customercontact`（一段式命名，与 `producttag` 风格一致）
- 前端视图：`luna-ui/src/views/lunaerp-elmui/baseinfo/customer/`
- 前端 API：`luna-ui/src/api/lunaerp-elmui/baseinfo/customer/`
- `lunaerp-elmui` 是先行快速实现版目录，未来正式 UI 仍落 `views/lunaerp/`（详见 ADR-0001）

### 数据模型

**客户表 `lunaerp_baseinfo_customer`**（继承 `TenantBaseDO`，标准列 creator/create_time/updater/update_time/deleted/tenant_id）：

| 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|
| id | int8 | - | 主键，`@KeySequence("lunaerp_baseinfo_customer_seq")` |
| company | varchar | ✓ | 公司名称 |
| short_name | varchar | - | 客户简称；后端 service 层若为空则用 company 回填 |
| phone | varchar | - | 公司电话 |
| address | varchar | - | 地址 |
| client_type | varchar | ✓ | 字典 `lunaerp_customer_client_type` |
| industry | varchar | - | 字典 `lunaerp_customer_industry` |
| scale | varchar | - | 字典 `lunaerp_customer_scale` |
| source | varchar | - | 字典 `lunaerp_customer_source` |
| cooperation_status | varchar | - | 字典 `lunaerp_customer_cooperation_status` |
| unified_social_credit_code | varchar | - | 统一社会信用代码，无唯一约束 |
| remark | varchar | - | 备注 |
| status | varchar | - | 缺省 `ACTIVE`；删除时 service 层先置 `INACTIVE` 再逻辑删除 |

无 `code` 字段，无 `businessOwnership` 字段，无任何业务唯一约束（除主键与租户/软删除框架约束）。

**客户联系人表 `lunaerp_baseinfo_customer_contact`**（继承 `TenantBaseDO`）：

| 字段 | 类型 | 必填 | 备注 |
|---|---|---|---|
| id | int8 | - | 主键，`@KeySequence("lunaerp_baseinfo_customer_contact_seq")` |
| customer_id | int8 | ✓ | 外键，关联 `lunaerp_baseinfo_customer.id`（直接 Long，无历史包袱） |
| name | varchar | ✓ | 姓名 |
| position | varchar | - | 职位 |
| phone | varchar | - | 电话/手机 |
| email | varchar | - | 邮箱 |
| remark | varchar | - | 备注 |
| is_primary | int2 | - | 是否主联系人，缺省 0 |

非主键唯一约束：无。同一客户可有多条 is_primary=1（业务约束由前端管理，后端不强校验）。

### Controller 与 API 契约

**客户 Controller**（`/admin-api/lunaerp/baseinfo/customer`）：

- `GET /page` — 分页查询，参数 `pageNo/pageSize/search/status`；`search` 智能词对 company/short_name/phone/unified_social_credit_code 做 OR ILIKE（PostgreSQL 用 `LOWER(...) LIKE LOWER({0})`）；`status` 缺省查 ACTIVE
- `GET /get?id=` — 单条详情（带 contacts 嵌套返回用于展示）
- `POST /create` — 新建，body 为 `CustomerSaveReqVO`；service 层 shortName 为空时回填 company；status 缺省 ACTIVE
- `PUT /update` — 修改，body 为 `CustomerSaveReqVO`（含 id）
- `DELETE /delete?id=` — 单条删除（先置 status=INACTIVE 再逻辑删除）
- `DELETE /delete-list?ids=1,2,3` — 批量删除

**客户联系人 Controller**（`/admin-api/lunaerp/baseinfo/customer-contact`）：

- `GET /page` — 分页查询，参数 `pageNo/pageSize/customerId`（必填）；按 customerId 过滤
- `GET /get?id=` — 单条详情
- `POST /create` — 新建，body 为 `CustomerContactSaveReqVO`（含 customerId）
- `PUT /update` — 修改
- `DELETE /delete?id=` — 单条删除（逻辑删除，无 INACTIVE 联动）

所有端点带 `@PreAuthorize("@ss.hasPermission('lunaerp:baseinfo:customer[:contact]:<action>')")` 注解，action ∈ {query, create, update, delete}。

### 权限码

- `lunaerp:baseinfo:customer:query` / `:create` / `:update` / `:delete`
- `lunaerp:baseinfo:customer-contact:query` / `:create` / `:update` / `:delete`

### 错误码

`1-030-004-xxx` 段位（customer 与 customer-contact 共用）：

- `1_030_004_000` — CUSTOMER_NOT_EXISTS（客户不存在）
- `1_030_004_001` — CUSTOMER_CONTACT_NOT_EXISTS（客户联系人不存在）

追加到 `lunasrv-module-lunaerp` 的 `ErrorCodeConstants.java` 末尾。

#### 错误码段重号修订（2026-09-18）

lunaerp 段位全表：`1-030-001-xxx` 产品 / `1-030-002-xxx` 胚布品种（`GreigeErrorCodeConstants`，随「产品模块迁移」commit `985a7ec` 先行上线）/ `1-030-004-xxx` 客户。原设计把客户记在 002 段，落地时发现与胚布品种的 `1_030_002_000/001/002` 整段重号；胚布品种既已随主线发布、其常量类注释也明确记载「001 产品、002 胚布品种」，故由后开发的客户模块迁至 004 段（003 段在早期销售合同 WIP 中曾被使用，直接跳号避免再次撞段）。框架无错误码唯一校验，重号不影响运行期，但日志/问题定位会混淆两个模块，故在客户模块发布前完成改段。

### 字典（5 个）

接入 yudao `system_dict_*` 机制，字典 type 前缀 `lunaerp_customer_`：

- `lunaerp_customer_client_type`：客户类型，初始值 潜在/正式/流失
- `lunaerp_customer_industry`：行业，初始值 纺织/服装/贸易/电商（业务方可自行扩充）
- `lunaerp_customer_scale`：规模，初始值 大型/中型/小型/微型
- `lunaerp_customer_source`：客户来源，初始值 主动开发/客户转介绍/展会/线上咨询
- `lunaerp_customer_cooperation_status`：合作状态，初始值 未合作/合作中/已暂停/已结束

前端用 `<DictTag>` 渲染，新建/编辑表单用 `<el-select>` + 字典 options。

### 前端路由

- 列表页：path `customer`（相对基本信息目录 `/baseinfo`），name `LunaerpElmuiBaseinfoCustomer`，component `lunaerp-elmui/baseinfo/customer/index`
- 工作台：path `customer/workspace/:id`，name `LunaerpElmuiBaseinfoCustomerWorkspace`，component `lunaerp-elmui/baseinfo/customer/workspace`

### 前端视图结构

- `views/lunaerp-elmui/baseinfo/customer/index.vue` — 列表页（5 列精简 + 搜索 + 状态筛选 + 分页 + 批量删除）
- `views/lunaerp-elmui/baseinfo/customer/workspace/index.vue` — 工作台容器（7 Tab）
- `views/lunaerp-elmui/baseinfo/customer/workspace/BasicProfileTab.vue` — 基础档案 Tab（表单）
- `views/lunaerp-elmui/baseinfo/customer/workspace/ContactsTab.vue` — 联系人 Tab（内联表格 + 新建/编辑弹窗）
- `views/lunaerp-elmui/baseinfo/customer/workspace/PlaceholderTab.vue` — 占位 Tab（"开发中"）
- `api/lunaerp-elmui/baseinfo/customer/index.ts` — 客户 API
- `api/lunaerp-elmui/baseinfo/customer/contact.ts` — 联系人 API

### 列表页行为

- 5 列：company（主显示）+ shortName（副标题）/ clientType（DictTag） / phone / cooperationStatus（DictTag） / createTime + 操作列
- 操作列：查看（路由跳工作台）/ 编辑（跳工作台基础档案 Tab）/ 删除（二次确认）
- 搜索：单 `search` 输入框 + 状态下拉筛选 + 分页
- 顶部工具栏：新建按钮 + 批量删除按钮（带 `v-hasPermi` 权限指令）

### SQL 脚本

文件：`luna-srv/sql/postgresql/lunaerp-baseinfo-customer.sql`

参照 `lunaerp-baseinfo-product.sql` 模式，包含：

1. 2 张表 DDL（`CREATE TABLE IF NOT EXISTS` + 主键 DO 块判存 + 索引 + 全列 `COMMENT`）
2. 2 个序列（`lunaerp_baseinfo_customer_seq` / `lunaerp_baseinfo_customer_contact_seq`）
3. `system_menu` 事务性 INSERT（先 DELETE 再 INSERT，可重复执行）：
   - 10030 客户信息菜单（type=2，parent_id=10000，component=`lunaerp-elmui/baseinfo/customer/index`，component_name=`LunaerpElmuiBaseinfoCustomer`，icon=`ep:office-building`）
   - 10031-10033 customer 按钮（query/create/update/delete 三个）
   - 10040-10043 customer-contact query/创建/修改/删除 四个按钮节点（type=3、path/component 为空，全部平铺直接挂 10030；2026-09-19 由"10040 type=2 隐藏菜单"修正——type=2 会被前端生成子路由，叠加父菜单 always_show 把客户信息渲染成空展开父菜单，见供应商 issue 04）
4. `system_dict_type` + `system_dict_data` INSERT：5 个字典 type，每个 type 配初始枚举值（ID 段建议 11000+）

### 软删除与状态机

- 客户删除：service 层先 `UPDATE status='INACTIVE'` 再走 `mapper.deleteByIds`（逻辑删除），与 product 模块模式一致
- 联系人删除：直接逻辑删除，无状态联动
- 客户新建/编辑：status 字段缺省 ACTIVE，前端可在表单里切换 ACTIVE/INACTIVE

### 不引入

- `code` 字段（与旧系统对齐）
- `businessOwnership` 字段（首期不做事业部隔离）
- 任何业务唯一约束（除主键）
- 审批流（status 两态足够）
- 联系人 photo / landlinePhone / businessCardAttachments（OCR 名片附件属后续阶段）
- 客户位置（latitude/longitude）
- AI 画像评分
- 付款规则字段
- 公海 ownerId 概念
- 联系人 isPrimary 单点约束（业务允许同客户多条主联系人，前端管理）

## Testing Decisions

### 测试哲学

只测外部行为（HTTP 契约），不测 Service/Mapper 实现细节。实现细节易变，契约稳定。

### 测试 seam

**唯一的测试 seam：后端 REST API 集成测试**

- 框架：Spring Boot `@SpringBootTest` + `MockMvc`，每个 Controller 一个测试类
- 优先复用 yudao 框架的测试基类（若 `BaseDbUnitTest`/`BaseDbAndRedisUnitTest` 可用），不重复造测试基建
- 数据库：嵌入式 PostgreSQL 或测试库（与产品模块未来落地测试时一致）

### 测试覆盖

**`CustomerControllerTest`**：

- `GET /page` — 无数据返回空列表 / 有数据返回分页 / `search` 命中 company / `search` 命中 shortName / `search` 命中 phone / `search` 命中 unifiedSocialCreditCode / `status=INACTIVE` 过滤
- `GET /get?id=` — 存在返回详情（含 contacts 嵌套） / 不存在返回 1_030_002_000 错误
- `POST /create` — 必填校验（company 缺失 / clientType 缺失） / shortName 为空时自动回填 company / status 缺省 ACTIVE / 成功返回 id
- `PUT /update` — 不存在 id 报错 / 成功更新字段
- `DELETE /delete?id=` — 不存在 id 报错 / 成功删除后 status 置 INACTIVE 且 deleted=1 / GET /get 不再返回
- `DELETE /delete-list?ids=` — 批量删除成功
- 权限：无 `lunaerp:baseinfo:customer:query` 权限的用户访问 /page 返回 403（若框架测试支持）

**`CustomerContactControllerTest`**：

- `GET /page?customerId=` — 按 customerId 过滤 / customerId 缺失报错
- `POST /create` — customerId 必填 / name 必填 / 成功返回 id
- `PUT /update` — 不存在 id 报错 / 成功更新
- `DELETE /delete?id=` — 成功逻辑删除
- 权限：无 `lunaerp:baseinfo:customer-contact:query` 权限访问 /page 返回 403

### 前端测试

跳过。lunaerp-elmui 是先行快速实现版（ADR-0001），UI 易变，不值得投入测试基建。

### Prior art

仓库内仅 `ProjectReactor.java` 一个 main 工具，无业务测试先例。本模块测试将作为 luna-srv 业务测试的首个落地点，建立的模式（基类选择、嵌入式 DB 配置、MockMvc 写法）将成为后续模块（产品/坯布品种补测、合同、寄样等）的参考。

## Out of Scope

- 工作台后 5 个 Tab 的实际功能（合同/寄样/画像分析/位置分布/拜访记录）— 仅占位"开发中"
- 客户与联系人的 Excel 导入/导出（旧系统有 `/api/clients-management/import`，首期不做）
- 客户统计接口（旧系统 `/api/clients-management/stats`，首期不做）
- 联系人 OCR 名片识别与附件存储（旧系统 businessCardAttachments，属后续阶段）
- 客户公海 ownerId 与认领/释放流程
- 客户审批流（旧系统 PENDING_APPROVAL/REJECTED 状态）
- 客户事业部数据隔离（businessOwnership 字段相关逻辑）
- 客户位置地图展示（latitude/longitude）
- 客户 AI 画像评分（valueScore/creditScore 等 6 个评分）
- 客户付款规则（defaultPaymentStrategy 等）
- 联系人 photo/landlinePhone 字段
- 前端单元/组件测试
- 正式 UI 版本（落 `views/lunaerp/` 的重写）— 等业务核心跑通后再启动

## Further Notes

### 已落地文档

- [CONTEXT.md](../../CONTEXT.md) — 项目术语表（中文），收录 Customer / CustomerContact / CustomerWorkspace / Status / shortName / 模块落点 / 租户 / 软删除 / 字典 / 错误码 等术语
- [docs/adr/0001-lunaerp-elmui-frontend-split.md](../../docs/adr/0001-lunaerp-elmui-frontend-split.md) — 前端 lunaerp-elmui 目录拆分决策
- [docs/adr/0002-customer-contact-independent-controller.md](../../docs/adr/0002-customer-contact-independent-controller.md) — 联系人独立 Controller 决策

### 实现顺序建议

后续拆 ticket 时建议按以下顺序：

1. SQL 脚本（建表 + 菜单 + 字典）
2. 后端 CustomerDO/Mapper/Service/Controller + VO + 错误码
3. 后端 CustomerContactDO/Mapper/Service/Controller + VO
4. 后端集成测试（CustomerControllerTest + CustomerContactControllerTest）
5. 前端 API 层（customer/index.ts + customer/contact.ts）
6. 前端列表页（index.vue）
7. 前端工作台容器（workspace/index.vue + 7 Tab 框架）
8. 前端基础档案 Tab + 联系人 Tab
9. 前端占位 Tab

### 参考

- 旧系统后端：`oldserver/src/routes/clients-management.ts`、`oldserver/prisma/schema.prisma`（BusinessClient / BusinessClientContact 模型）
- 旧系统前端：`oldweb/app/staff/components/ClientInfoTab.tsx`、`oldweb/app/staff/components/client-v2/tab-config.ts`
- 新项目模板：`luna-srv/lunasrv-module-lunaerp` 下 `product` 子包（Controller/Service/Mapper/DO/VO 全套）、`luna-ui/src/views/lunaerp/baseinfo/product/`（列表页 + 工作台模式参考）
- SQL 模板：`luna-srv/sql/postgresql/lunaerp-baseinfo-product.sql`（DDL + 菜单 + 序列模式）
