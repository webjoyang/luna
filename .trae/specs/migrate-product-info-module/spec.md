# 产品信息模块迁移 Spec

## Why
将 oldweb（React）与 oldserver（Express/Prisma）中的「基本信息-产品信息」模块迁移到目标技术栈：前端迁入 luna-ui（Vue 3 + Element Plus），后端迁入 luna-srv（SpringBoot 3.x 芋道模板），实现功能一致、体验一致，并适配芋道代码规范。

## 命名与归属规范（用户确认）
- 「产品信息」属于 lunaerp 业务域中「基本信息」分组下的一个菜单模块。
- lunaerp 作为独立业务域从芋道源码原有模块中区分出来：后端独立 Maven 模块，前端独立目录；但 **菜单显示上不出现 lunaerp 这一层**（lunaerp 仅体现在代码文件路径、接口路径与权限标识中）。

## What Changes

### 范围（已与用户确认）
- ✅ 迁移核心产品管理：列表（网格/表格双视图）、搜索/筛选/分页、新建/编辑/删除、批量删除、产品标签、胚布品种（GreigeType）、产品图片、下一编号。
- ✅ 包含胚布品种独立管理页面（列表/新增/编辑/删除 + 产品表单弹窗选择器）。
- ❌ 排除：审批流（创建/编辑/删除直接生效，保留 status 语义）、AI 智能搜索（仅保留经典分词搜索）、拍照建档(CAPTURE)、Lookbook、标签打印模板中心、AI设计图、打印/二维码、移动端分享、样布登记、海报生成、batch-upsert/batch-check/batch-get/match-image 等 AI/批量辅助接口。

### 后端（luna-srv，新建 Maven 模块 lunasrv-module-lunaerp）
- 新建 `lunasrv-module-lunaerp` 模块，package 根 `com.luna.lunasrv.module.lunaerp`，接入根 pom / lunasrv-dependencies / lunasrv-server pom。
- 产品业务置于 `baseinfo.product`，各层目录结构（注意：lunaerp 模块的 Controller 层不带 `admin` 段）：
  - `controller/baseinfo/product/`（Controller + vo/）
  - `service/baseinfo/product/`
  - `dal/dataobject/baseinfo/product/`
  - `dal/mysql/baseinfo/product/`
  - product-tag、greige-type 同层并列于 `baseinfo/` 下（如 `controller/baseinfo/producttag/`、`baseinfo/greigetype/`）
- 接口遵循芋道规范：`CommonResult` 包装、`PageParam/PageResult` 分页（pageNo/pageSize）、`@PreAuthorize`、`BeanUtils` 转换、Swagger 注解。
- 接口清单（URL 前缀 `/lunaerp/baseinfo`）：
  - ProductController `/lunaerp/baseinfo/product`：`page`（分页+搜索/筛选）、`get`、`get-by-code`、`create`、`update`、`delete`、`delete-list`（批量删除）、`next-code`（编号生成，沿用"前缀+数字递增、排除已删除"规则）、`update-tags`（单独更新标签）。
  - ProductTagController `/lunaerp/baseinfo/product-tag`：`create`、`update`、`delete`、`list`（租户内标签，供标签区选择/自动建标签）。
  - GreigeTypeController `/lunaerp/baseinfo/greige-type`：`page`、`simple-list`、`get`、`create`、`update`、`delete`；创建时自动生成流水号 `GT-0001` 与 matchKey（布种+成分+纱支+坯克重+坯门幅 拼接，与原 `buildGreigeMatchKey` 一致），`matchKey` 租户内唯一用于查重。
- 权限标识（`lunaerp:baseinfo` 前缀以区分芋道原模块）：`lunaerp:baseinfo:product:query/create/update/delete`、`lunaerp:baseinfo:product:tag:query/create/update/delete`（标签接口，无独立菜单）、`lunaerp:baseinfo:product:greige:query/create/update/delete`（胚布品种）。
- 业务语义保持：
  - 列表仅查询 ACTIVE / DRAFT（沿用原逻辑），排序 createdAt desc；`search` 关键词分词后 AND 逐词 OR 匹配 code/name/supplierCode/composition/type（忽略大小写），支持 code/name/composition/type 独立过滤与克重区间（minWeight/maxWeight，单值 weightGsm 按 ±10 匹配）。
  - 编号 `(tenant_id, code)` 租户级唯一，冲突返回明确错误提示（"编号已被占用，请重新获取编号"）。
  - 删除为逻辑删除（Yudao `deleted` 标记）并置 status='DELETED'；status 语义保留 ACTIVE/DRAFT/ARCHIVED/DELETED。
  - 产品标签在同一事务内 co-update：body 传 `tags: [{name, color}]`，不存在同名标签则自动创建（租户内同名共享颜色）。
  - 图片：`coverImageUrl` + `images: [{url, sortOrder}]`，文件上传复用 infra 文件接口，后端仅存储关系。
  - 简化项（记录差异）：权限动作统一为 query/create/update/delete（原 CREATE/EDIT_AFTER_SUBMIT/DELETE_DRAFT/DELETE_SUBMITTED 的分流不再保留）；costItems 直接可写（原 HD/RO/RW 闸门不迁移）；supplier_id 保留为可空字段（无外键），供应商模块后续迁移时接入。
- 数据库（PostgreSQL，新增 `sql/postgresql/lunaerp-baseinfo-product.sql`）：5 张表，表名以 `lunaerp_baseinfo_product` 前缀与芋道原表区分，含芋道标准字段（creator/create_time/updater/update_time/deleted int2/tenant_id int8），唯一约束用 `WHERE deleted = 0` 的部分唯一索引：
  - `lunaerp_baseinfo_product`（编号、名称、类型、成分、门幅/克重、封面图、供应商、状态、is_public/is_featured/featured_order、yarn_specs/cost_items JSON 文本、机型/产量/备注/原厂编号/临时编号/工艺类型/颜色/花型号/标准米长/算长系数、greige_type_id、cost_price/cost_status 等）
  - `lunaerp_baseinfo_product_image`（product_id、url、sort_order）
  - `lunaerp_baseinfo_product_tag`（name、color；`（tenant_id,name）`唯一）
  - `lunaerp_baseinfo_product_tag_rel`（id 主键、product_id、tag_id、sort_order，`（product_id,tag_id）` 唯一约束防重）
  - `lunaerp_baseinfo_product_greige_type`（code、name、match_key、fabric_type、composition、yarn_spec、weight_gsm、width_cm、craft_type、machine_type、remark、status；`（tenant_id,code）`、`（tenant_id,match_key）`唯一）
- 菜单 SQL（芋道 system_menu 插入，**菜单显示不含 lunaerp 层**）：
  - **菜单 id 号段约定**：lunaerp 业务域使用 10000-10999（现有种子数据最大 id 6364，无冲突；10000 为「基本信息」目录，产品信息占 10010-10019，胚布品种占 10020-10029，后续基本信息子菜单从 10030 起顺延）。
  - id=10000：目录「基本信息」（type=1, parent_id=0, path=`/baseinfo`, icon=`ep:folder-opened`）
  - id=10010：菜单「产品信息」（type=2, parent_id=10000, path=`product`, component=`lunaerp/baseinfo/product/index`, component_name=`LunaerpBaseinfoProduct`, permission=`lunaerp:baseinfo:product:query`）
    - id=10011「产品创建」type=3 permission=`lunaerp:baseinfo:product:create`
    - id=10012「产品修改」type=3 permission=`lunaerp:baseinfo:product:update`
    - id=10013「产品删除」type=3 permission=`lunaerp:baseinfo:product:delete`
  - id=10020：菜单「胚布品种」（type=2, parent_id=10000, path=`greige-type`, component=`lunaerp/baseinfo/greige-type/index`, component_name=`LunaerpBaseinfoGreigeType`, permission=`lunaerp:baseinfo:product:greige:query`）
    - id=10021「胚布品种创建」type=3 permission=`lunaerp:baseinfo:product:greige:create`
    - id=10022「胚布品种修改」type=3 permission=`lunaerp:baseinfo:product:greige:update`
    - id=10023「胚布品种删除」type=3 permission=`lunaerp:baseinfo:product:greige:delete`
  - 说明：query 权限直接挂在菜单行（控制菜单访问），按钮节点仅建 create/update/delete，不重复建 query 按钮；产品标签（lunaerp:baseinfo:product:tag:*）无独立菜单节点。
  - 即：侧边栏仅显示「基本信息 → 产品信息/胚布品种」，lunaerp 层体现在组件路径与接口路径中，不作为菜单节点。

### 前端（luna-ui，Vue 3 + Element Plus）
- API 层：`src/api/lunaerp/baseinfo/product/index.ts`、`product-tag.ts`、`greige-type.ts`，遵循 `request from '@/config/axios'` 封装。
- 页面（文件路径含 lunaerp 层）：`src/views/lunaerp/baseinfo/product/index.vue`（+ 组件目录）与 `src/views/lunaerp/baseinfo/greige-type/index.vue`，路由由菜单数据动态驱动（URL `/baseinfo/product`、`/baseinfo/greige-type`）。
- 体验对齐 oldweb `ProductInfoTab`：
  - 灵动岛工具栏（搜索框、grid/table 视图切换、新建按钮）、状态分段筛选（在售 ACTIVE / 草稿 DRAFT）、网格卡片视图与表格视图、分页控件。
  - 编辑弹窗：基本信息分区、纱线规格（动态行：纱线规格/纺法/比例/备注/单价）、成本项（动态行：供应商/工序/单价/损耗%）、标签区（选择/新建，同名同色）、图片上传（复用 infra 上传，封面选择 + 排序）、胚布品种弹窗选择器。
  - 动画（入场、展开、弹窗过渡等）使用 motion-v（framer-motion 的 Vue 实现），Element Plus 承载表单/表格/弹窗/上传基础组件。
  - 权限控制使用 `v-hasPermi` 指令（lunaerp:baseinfo:product:create/update/delete 等）。

## Impact
- Affected specs: 无（首个 spec）。
- Affected code:
  - 新增：`luna-srv/lunasrv-module-lunaerp/**`、`luna-srv/sql/postgresql/lunaerp-baseinfo-product.sql`、`luna-ui/src/api/lunaerp/baseinfo/**`、`luna-ui/src/views/lunaerp/baseinfo/product/**`、`luna-ui/src/views/lunaerp/baseinfo/greige-type/**`
  - 修改：`luna-srv/pom.xml`、`luna-srv/lunasrv-dependencies/pom.xml`、`luna-srv/lunasrv-server/pom.xml`（模块接线）

## ADDED Requirements

### Requirement: 产品分页查询
系统 SHALL 提供产品分页接口（`GET /lunaerp/baseinfo/product/page`），支持 pageNo、pageSize（默认 20、上限 100）、search 关键词（分词 AND/OR 多字段模糊）、code/name/composition/type 过滤、克重区间过滤、status（仅 ACTIVE/DRAFT），按创建时间倒序返回，附带 images、标签、供应商名称。

#### Scenario: 关键词分词搜索
- **WHEN** 前端以 search="TR 罗马 260" 请求分页
- **THEN** 返回的 items 中每条产品满足：每个分词至少命中 code/name/supplierCode/composition/type 之一（忽略大小写）

#### Scenario: 状态筛选
- **WHEN** 请求 status=DRAFT
- **THEN** 仅返回草稿状态产品；其他 status 值按 ACTIVE 处理

### Requirement: 产品创建与编号
系统 SHALL 提供产品创建接口，校验 `(tenant_id, code)` 租户级唯一（冲突返回"编号已被占用，请重新获取编号"），支持 yarnSpecs/costItems/tags/images 字段写入，标签在同一事务内自动建档关联。

#### Scenario: 创建成功
- **WHEN** 提交合法的 create 请求
- **THEN** 返回创建后的产品详情（含 id、tags、images），且 tags 中新标签已自动创建

#### Scenario: 编号冲突
- **WHEN** 提交的 code 已存在
- **THEN** 返回明确错误信息"编号已被占用，请重新获取编号"

### Requirement: 下一编号
系统 SHALL 提供下一编号接口（`GET /lunaerp/baseinfo/product/next-code`），沿用原"前缀+数字递增、排除已删除产品"规则。

#### Scenario: 取号
- **WHEN** 调用 next-code
- **THEN** 返回当前租户内可用的下一个产品编号

### Requirement: 产品更新与删除
系统 SHALL 提供更新接口（含标签 co-update、图片整体替换）与删除接口（单条/批量），删除为逻辑删除并置 status=DELETED。

#### Scenario: 标签同步
- **WHEN** 更新产品时传入新的 tags 数组
- **THEN** 产品标签关联被整体替换，新标签自动建档，旧关联被移除

#### Scenario: 批量删除
- **WHEN** 提交 ids 列表调用 delete-list
- **THEN** 所有目标产品被逻辑删除

### Requirement: 产品标签管理
系统 SHALL 提供租户内标签列表接口（供标签区展示/选择），并提供标签新增/修改/删除接口；同一租户内同名标签唯一且共用颜色。

### Requirement: 胚布品种管理
系统 SHALL 提供胚布品种 CRUD 与 simple-list 接口：创建时自动生成流水号（GT-XXXX）与 matchKey，matchKey 租户内唯一用于查重；产品表单通过弹窗选择器引用品种。

#### Scenario: 重复品种查重
- **WHEN** 创建与既有记录五属性（布种/成分/纱支/坯克重/坯门幅）完全一致的品种
- **THEN** 返回查重错误提示

### Requirement: 前端列表页体验
luna-ui 的产品信息页 SHALL 复刻 oldweb ProductInfoTab 的布局与交互：灵动岛工具栏（搜索、视图切换、新建）、状态分段筛选（在售/草稿）、网格与表格双视图、分页、动画（motion-v），并接入 v-hasPermi 权限指令。

#### Scenario: 视图切换
- **WHEN** 用户在工具栏切换 grid/table
- **THEN** 列表区以对应视图重新渲染当前页数据

#### Scenario: 编辑保存
- **WHEN** 用户在编辑弹窗提交合法表单
- **THEN** 调用 create/update 接口成功后，列表缓存按"新增插入当前页 / 编辑替换原位"策略更新，与原 React 行为一致

### Requirement: 胚布品种管理页
luna-ui SHALL 提供胚布品种管理页（列表/新增/编辑/删除），并从产品编辑弹窗可打开品种选择器。

## 简化与差异记录（非破坏性决策）
1. 审批流不迁移：创建/编辑/删除直接生效；status 字段语义保留。
2. AI 智能搜索不迁移：仅实现经典分词搜索。
3. 权限动作收敛为 query/create/update/delete。
4. costItems 写入不再有 HD/RO/RW 闸门。
5. supplier_id 为可空独立字段，无外键、无供应商选择器（供应商模块后续迁移接入）。
6. 回收站（原删除写回收站）不迁移，删除即逻辑删除。
