# 05: 齐套检查后端——只读试算

**What to build:** 新增 production.materialcheck 只读子包：POST material-check/run（纯只读试算，无写操作、不落库、无日志），权限 `lunaerp:production:material-check:query`。入参 weavingOrderIds（空列表=试算全部 DRAFT/ACTIVE 织造单）/warehouseIds（空=全仓库合计）/includeInTransit（仅影响汇总徽标口径，缺口两列常驻计算）。口径：需求 = Σ(plannedWeight × ratio/100) 按 yarnSpecId 跨单合并（2 位小数 HALF_UP），每行带 contributions 贡献展开行；无配比 BOM 单入 missingBomOrders、缺 plannedWeight/plannedQty 单入 missingWeightOrders（均不污染聚合行）；可用库存 = 台账实时余额（YARN_OUT 负流水自然参与求和，新增批量 IN+GROUP BY 聚合 Mapper 方法，禁逐规格循环）；在途 = Σ ACTIVE 纱线采购单明细 purchase_qty（一条 join 主子表 GROUP BY 聚合，DRAFT/COMPLETED/CANCELLED 排除）；缺口（不含在途）= max(0, 需求−可用)、缺口（含在途）= max(0, 需求−可用−在途)、建议采购量 = 缺口（含在途）。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

**参考**：本目录上级 spec.md §4；ADR-0017（台账）、ADR-0011（配比 BOM）、ADR-0027（领纱测算口径）、ADR-0041

- [ ] 两织造单同产品按 yarnSpecId 合并需求 = Σ(plannedWeight × ratio/100)，贡献行可展开
- [ ] 可用库存=台账正负流水求和（含 YARN_OUT 扣减）；仓库筛选生效、缺省全仓；批量聚合无 N+1
- [ ] 在途仅计 ACTIVE 采购单 purchase_qty；两缺口与建议采购量（含在途口径）计算正确，齐套/在途可覆盖/缺料三态数据源齐备（状态标签由前端派生，不入字典）
- [ ] 缺 BOM、缺计划重量单分别入两个分区且不进聚合行
- [ ] 租户隔离：他租户织造单/库存/采购单不进试算；传入不存在或他租户织造单 ID 抛不存在类错误
- [ ] 纯 Mockito 单测（MaterialCheckServiceImplTest）覆盖以上全部场景
