# 08: 库存扩展物品类型（入库明细/流水 itemType + 纱线规格 + 余额查询）

**What to build:** 入库明细表加 `item_type`（PRODUCT 缺省/YARN_SPEC）、`yarn_spec_id`、`yarn_spec_name`；库存流水表加 `yarn_spec_id`——与 product_id **互斥**。入库聚合保存与审核/红冲按物品类型适配：YARN_SPEC 行单位固定 KG、无产品字段；写流水时物品身份随明细；红冲逐条反向不变。库存余额聚合键改为 物品(product/yarn_spec)+单位+仓库；库存查询页（余额列表+流水分页）增加物品类型列与筛选，产品/纱线共用。前端入库单工作台：明细加物品类型切换，YARN_SPEC 时选择纱线规格（材质→规格级联）并隐藏产品列。纱线采购入库手工开单（type=PURCHASE）。错误码复用 1-030-011 段。建表/变更 SQL 追加至 `luna-srv/sql/postgresql/lunaerp-stock.sql`（或新建增量段）。

**Status:** closed

- [x] 入库明细/流水 itemType + yarnSpec 列（可空、缺省 PRODUCT）
- [x] 审核/红冲/放置按物品类型适配
- [x] 余额聚合与库存查询支持纱线（物品类型列/筛选）
- [x] 前端入库单明细类型切换 + 纱线规格级联选择
- [x] 单测：纱线入库过账/红冲、互斥约束、混合单
