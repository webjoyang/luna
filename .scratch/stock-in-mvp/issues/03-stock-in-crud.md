# 03: 入库单 CRUD（IN 单号 + 关联快照 + 聚合明细）

**What to build:** 打开"库存管理 → 入库管理"列表页，可查看入库单列表（入库单号、单据日期、类型、仓库、供应商、金额合计、状态、创建人、创建时间）、分页、多条件检索（关键词/类型/状态/仓库/供应商/日期区间）、新建。点击"新建入库单"进入工作台，在"基本信息"Tab 选择入库类型、入库仓库（必选）、供应商（可空）、关联销售合同/胚布单/染色单/胚布品种（全部可空，仅存快照），填写单据日期与备注；在"入库明细"Tab 以表格网格录入明细（产品必选带出快照、批次、卷号、匹数、数量、单位 KG/M、单价、金额自动算），随整单聚合保存。草稿态可编辑/删除，删除时级联逻辑删除明细。操作日志 Tab 占位。入库单号 IN-YYYYMMDD-NNN 按天取号、可手填、提供预览。

**Blocked by:** 01 (SQL 基建)、02 (仓库管理)

**Status:** ready-for-human

- [x] 入库单主表 + 入库明细 DO/Mapper/VO/Service/Controller 就位
- [x] IN-YYYYMMDD-NNN 按天取号 + 手填撞号报错 + get-next-no 预览
- [x] 关联校验与快照：仓库/供应商/销售合同/胚布单/染色单/胚布品种/产品全部存在性校验 + 名称快照回填
- [x] 聚合保存：明细整表替换 + 金额合计服务端权威重算；零明细草稿允许
- [x] 级联删除：删主表级联逻辑删除明细
- [x] 前端入库列表页（检索/分页/操作列按权限显示）+ 工作台 3 Tab（基本信息/入库明细/操作日志占位）+ 隐藏路由
- [x] Service 层 Mockito 单测：StockInServiceImplTest 创建/更新/删除/取号/关联校验用例

## Comments

- 2026-09-24（agent）：
  - 持久层与数据对象：
    1. 实体：`StockInItemDO` 继承 `TenantBaseDO` 并配置 `@KeySequence("lunaerp_stock_in_item_seq")`。
    2. Mapper：`StockInItemMapper`（按入库单查明细、按入库单批量删除）；`StockInItemPlacementMapper` 增补按入库单删除与查询方法。
  - 控制层与 VO：
    1. VO：`StockInPageReqVO`、`StockInSaveReqVO`、`StockInRespVO`、`StockInItemSaveReqVO`、`StockInItemRespVO`。
    2. 控制层：`StockInController` 提供 `/create`、`/update`、`/delete`、`/get`、`/page`、`/get-next-no` 6个标准端点，并匹配精确权限码。
  - 业务逻辑实现（`StockInServiceImpl`）：
    1. 取号：`IN-YYYYMMDD-NNN` 按天取号、租户内全量唯一含软删行，支持手填撞号校验与自动号顺延重试；提供 `getNextInNo` 只读预览接口。
    2. 关联校验与快照：仓库必填且必须为 ACTIVE（否则报 `1_030_011_003`），回填仓库名称；供应商/合同/胚布单/染色单/胚布品种选填但校验存在并回填快照；每行明细产品必须存在并回填编码名称快照。
    3. 聚合保存与重算：整表替换明细，服务端权威重算明细金额 `quantity * unitPrice` 与整单 `totalAmount`；允许零明细保存草稿。
    4. 状态守卫：仅 `DRAFT` 草稿态可编辑/删除，非草稿态抛出 `1_030_011_002`（`STOCK_IN_STATUS_NOT_ALLOWED`）。
    5. 级联逻辑删除：删除主表级联逻辑删除明细与放置记录。
  - 单元测试（纯 Mockito 单测）：
    1. `StockInServiceImplTest`：17 个测试用例覆盖自动取号、手填查重、关联引用校验、明细金额重算、草稿态守卫、级联删除与分页查询。
    2. 全模块 308 个单测全部通过。
  - 前端全栈页面与路由：
    1. API：`src/api/lunaerp-elmui/stock/stock-in/index.ts`。
    2. 路由：`src/router/modules/remaining.ts` 注册隐藏路由 `/stock/stock-in/workspace/:id?`。
    3. 列表页：`src/views/lunaerp-elmui/stock/stock-in/index.vue`（多条件检索、金额格式化、字典标签与权限操作列）。
    4. 工作台：`src/views/lunaerp-elmui/stock/stock-in/workspace/`（`index.vue`、`BasicInfoTab.vue`、`ItemsTab.vue`、`LogsTab.vue`）。
    5. Vite 构建顺利通过，编译无异常。
