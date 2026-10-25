# 04: BOM 行 5 列端到端（DO/VO/前端配比管理页）

**What to build:** 用户能在配比 BOM 管理页为每个 `yarn_recipe_bom_item` 维护 5 个新列：损耗率（`waste_rate numeric(7,2)`）、方向（`direction varchar(16)`，GENERAL/WARP/WEFT 三选一）、密度（`density numeric(18,2)`）、织缩率（`crimp_rate numeric(7,2)`）、每米用量（`consumption_kg_per_meter numeric(18,6)`）；存量行默认 null，保存后这些列可被成本卡创建时拉取作为经纬精算参数。SQL ALTER 列定义已在 02 票落库（`ADD COLUMN IF NOT EXISTS`），本票只接 DO/VO + 前端维护表单。这 5 列是 06 票成本卡梭织经纬精算模式的入参来源，刻意单独成票以便 06 票依赖其 DO 形状稳定。

**Blocked by:** 02（SQL 基建）

**Status:** closed

- [x] `YarnRecipeBomItemDO` + VO（SaveReqVO/RespVO）加 5 个新字段，Swagger 注解完备
- [x] 现有配比 BOM 管理页表单加 5 个字段维护控件（损耗率/方向/密度/织缩率/每米用量），方向为下拉（GENERAL/WARP/WEFT）
- [x] 存量行迁移后 5 列为 null，编辑保存不报错
- [x] 单测覆盖 5 字段持久化与默认值（null 不被业务逻辑视为 0）
- [x] 验收：能在配比 BOM 管理页录入并保存 5 个新列；重开页面完整回显；存量行打开编辑不报错
