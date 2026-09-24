# 02: 仓库管理（仓库+库位主从 CRUD 全栈）

**What to build:** 打开"库存管理 → 仓库管理"页，可新建仓库（编号自动取号 WH+年份+4 位流水，也可手填，撞号报错）、录入名称/地址/备注；选中仓库后管理其库位（编码手填，同一仓库内唯一，撞号报错）。停用仓库/库位后不再出现在入库单选择项中，但历史单据快照照常展示。删除守卫生效：仓库下有库位或被入库单引用时报错并引导停用；库位被放置记录引用时报错并引导停用。提供 list-all（启用仓库简表）与 list-by-warehouse（启用库位列表）两个下拉接口，供入库单选择使用。

**Blocked by:** 01 (SQL 基建)

**Status:** ready-for-human

- [x] 仓库 CRUD：取号、手填编号撞号、列表、详情、更新、删除守卫（有库位/被引用禁止）
- [x] 库位 CRUD：仓内编码唯一、删除守卫（被放置引用禁止）
- [x] 前端仓库管理主从同页：左侧仓库列表（新建/编辑/停用/删除），右侧选中仓库的库位列表（新建/编辑/停用/删除）
- [x] 下拉接口：list-all（仅启用仓库）、list-by-warehouse（仅启用库位）
- [x] Service 层 Mockito 单测：WarehouseServiceImplTest + WarehouseLocationServiceImplTest（取号、唯一、删除守卫）

## Comments

- 2026-09-24（agent）：
  - 错误码扩展（`ErrorCodeConstants.java`）：
    1. 仓库/库位错误码 1_030_013_000~1_030_013_102。
    2. 同步登记入库单错误码 1_030_011_000~1_030_011_005。
  - 数据模型与持久层：
    1. 实体：`WarehouseDO`、`WarehouseLocationDO`、`StockInDO`、`StockInItemPlacementDO` 均继承 `TenantBaseDO` 并配置 `@KeySequence`。
    2. Mapper：`WarehouseMapper`（分页模糊检索、前缀含软删行扫描、编号含软删查重）、`WarehouseLocationMapper`（分页检索、同仓编码含软删查重、按仓库统计数量）、`StockInMapper`、`StockInItemPlacementMapper`。
  - 业务逻辑实现（Service & ServiceImpl）：
    1. 仓库：编号自动取号（WH+年份+4位流水顺延并支持重试，手填撞号报错 `1_030_013_001`）、删除双重守卫（有库位报 `1_030_013_002`，被入库单引用报 `1_030_013_003`）、启用停用切换、下拉简表（`list-all` 默认仅返回 ACTIVE）。
    2. 库位：编码同仓唯一（重名报 `1_030_013_101`）、删除守卫（被入库放置引用报 `1_030_013_102`）、按仓库查询启用列表（`list-by-warehouse` 默认仅返回 ACTIVE）。
  - 控制层：
    1. `WarehouseController`：`/lunaerp/stock/warehouse` 7个端点，配套精确权限码。
    2. `WarehouseLocationController`：`/lunaerp/stock/warehouse-location` 6个端点，配套精确权限码。
  - 单元测试（纯 Mockito，无 Spring/DB 依赖）：
    1. `WarehouseServiceImplTest`：14 个测试用例覆盖自动取号、手填查重、更新、删除守卫（有库位/被引用）、状态流转与下拉。
    2. `WarehouseLocationServiceImplTest`：11 个测试用例覆盖创建校验、仓内唯一查重、更新、删除守卫（放置引用）与下拉。
    3. 全模块 287 个单测全部通过。
  - 前端全栈页面与 API：
    1. API：`src/api/lunaerp-elmui/stock/warehouse/index.ts`、`src/api/lunaerp-elmui/stock/warehouse-location/index.ts`。
    2. 页面：`src/views/lunaerp-elmui/stock/warehouse/index.vue`，实现左侧仓库列表与右侧库位列表主从同页管理，支持高亮联动、新建/编辑/停用/启用/删除及错误友好提示。
