# 05: 齐套检查后端——只读试算

**What to build:** 新增 production.materialcheck 只读子包：POST material-check/run（纯只读试算，无写操作、不落库、无日志），权限 `lunaerp:production:material-check:query`。入参 weavingOrderIds（空列表=试算全部 DRAFT/ACTIVE 织造单）/warehouseIds（空=全仓库合计）/includeInTransit（仅影响汇总徽标口径，缺口两列常驻计算）。口径：需求 = Σ(plannedWeight × ratio/100) 按 yarnSpecId 跨单合并（2 位小数 HALF_UP），每行带 contributions 贡献展开行；无配比 BOM 单入 missingBomOrders、缺 plannedWeight/plannedQty 单入 missingWeightOrders（均不污染聚合行）；可用库存 = 台账实时余额（YARN_OUT 负流水自然参与求和，新增批量 IN+GROUP BY 聚合 Mapper 方法，禁逐规格循环）；在途 = Σ ACTIVE 纱线采购单明细 purchase_qty（一条 join 主子表 GROUP BY 聚合，DRAFT/COMPLETED/CANCELLED 排除）；缺口（不含在途）= max(0, 需求−可用)、缺口（含在途）= max(0, 需求−可用−在途)、建议采购量 = 缺口（含在途）。

**Blocked by:** None (can start immediately)

**Status:** resolved

**参考**：本目录上级 spec.md §4；ADR-0017（台账）、ADR-0011（配比 BOM）、ADR-0027（领纱测算口径）、ADR-0041

- [x] 两织造单同产品按 yarnSpecId 合并需求 = Σ(plannedWeight × ratio/100)，贡献行可展开
- [x] 可用库存=台账正负流水求和（含 YARN_OUT 扣减）；仓库筛选生效、缺省全仓；批量聚合无 N+1
- [x] 在途仅计 ACTIVE 采购单 purchase_qty；两缺口与建议采购量（含在途口径）计算正确，齐套/在途可覆盖/缺料三态数据源齐备（状态标签由前端派生，不入字典）
- [x] 缺 BOM、缺计划重量单分别入两个分区且不进聚合行
- [x] 租户隔离：他租户织造单/库存/采购单不进试算；传入不存在或他租户织造单 ID 抛不存在类错误
- [x] 纯 Mockito 单测（MaterialCheckServiceImplTest）覆盖以上全部场景

## Comments
- **齐套检查后端只读试算**（Commit: `69f1d4be`，Merge: `22bee969`）：
  - 新增 Controller 只读试算端点：`POST /lunaerp/production/material-check/run`，权限码 `lunaerp:production:material-check:query`，纯只读无写操作、不落库、无操作日志。
  - 新增 VO：
    - `MaterialCheckReqVO`：入参包含 `weavingOrderIds`（空=全选 DRAFT/ACTIVE 织造单）、`warehouseIds`（空=全仓合计）、`includeInTransit`（控制汇总徽标口径）。
    - `MaterialCheckRespVO`：出参包含 `rows` 聚合行、`missingBomOrders` 缺配比单列表、`missingWeightOrders` 缺重量/匹数单列表、`shortageSpecCount` 缺料规格数、`totalShortageQty` 缺料合计 KG。
    - `MaterialCheckRowVO`：纱线规格聚合行，包含材质与规格信息、需求量、可用库存、在途量、缺口（不含在途）、缺口（含在途）、建议采购量及 `contributions` 展开行。
    - `MaterialCheckContributionVO`：各织造单贡献明细，包含织造单号、产品信息、计划重量、配比 ratio、本单需求 KG。
  - 新增与扩展 Mapper 批量聚合方法（批量 IN + GROUP BY，杜绝 N+1）：
    - `StockLedgerMapper#sumBalanceByYarnSpecIds(yarnSpecIds, warehouseIds)`：批量统计台账可用余额，支持可选仓库过滤，缺省全仓。
    - `YarnOrderItemMapper#sumInTransitByYarnSpecIds(yarnSpecIds)`：一条 SQL join 采购主子表批量统计 ACTIVE 采购单明细 purchase_qty。
    - `YarnRecipeBomMapper#selectListByProductIds(productIds)` 与 `WeavingOrderMapper#selectActiveOrDraftList()`。
  - 核心试算业务逻辑（`MaterialCheckServiceImpl`）：
    1. 织造单加载与租户/状态防御：传入不存在或跨租户 ID 抛出 `WEAVING_ORDER_NOT_EXISTS`；非 DRAFT/ACTIVE 状态抛出 `WEAVING_ORDER_STATUS_NOT_ALLOW`；缺省全量加载 DRAFT/ACTIVE 织造单。
    2. 无法试算分区隔离：无配比 BOM（整单入 missingBomOrders）、缺计划重量/匹数（整单入 missingWeightOrders），两类单据均不污染聚合行。
    3. 需求量折算与聚合：单单需求 = plannedWeight × ratio / 100（2位小数 HALF_UP），按 yarnSpecId 跨单合并需求量，保留 contributions 展开行。
    4. 实时可用与在途量：实时取台账余额（YARN_OUT 负流水自然扣减）与 ACTIVE 采购在途量。
    5. 缺口与建议采购量：缺口（不含在途）= max(0, 需求 - 可用)、缺口（含在途）= max(0, 需求 - 可用 - 在途)、建议采购量 = 缺口（含在途）。
    6. 汇总徽标计算：根据 includeInTransit 开关切换统计缺料规格数与缺料合计 KG。
  - 纯 Mockito 单元测试 `MaterialCheckServiceImplTest`：7 个用例完整覆盖 Testing Decisions 11-15 全部场景，测试全部通过。
