# 01: 胚布单自织模式改造（productionMode + 承织厂可空 + 只读织造进度）

**What to build:** 胚布单表加 `production_mode`（字典 `lunaerp_greige_production_mode`，SELF/OUTSOURCE，缺省 OUTSOURCE）；表单加模式切换：SELF 时承织坯布厂校验放宽（可空）、OUTSOURCE 维持必填；不引入"本厂"占位供应商。胚布单详情页对 SELF 单展示"织造进度"区：实时聚合其下织造单的 计划匹数/已完工匹数（不落库、不回写胚布单状态），并提供"生成织造单"入口（跳转织造单新建并带胚布单上下文，见 issue 03/06）。DDL 索引：`(tenant_id, greige_factory_id)` 与 SELF 部分索引。胚布单状态机与编号规则不变。

**Status:** closed

- [x] `production_mode` 列与字典（id=2241）落库，存量数据默认 OUTSOURCE
- [x] 胚布单创建/更新：模式与承织厂联动校验（OUTSOURCE 必填、SELF 可空）
- [x] 胚布单详情：SELF 单只读织造进度聚合 + 生成织造单入口
- [x] DDL：承织厂复合索引 + SELF 部分索引
- [x] 单测：模式切换校验、自织单进度聚合
