# 轻量排产与齐套检查（Scheduling & Material Check）

Status: resolved（grilling 两轮 22 问全部按推荐锁定，源自 `.scratch/lunaerp-commercialization/issues/09-scheduling-and-material-check.md`；ADR-0041）

## Problem Statement

商业化 W2 链路上，「合同→下达生产」（release-production）生成胚布/染色/织造等单据之后，车间侧缺三块能力：

1. **织造排产无全局视图、无冲突防护**：现有机台任务（`lunaerp_production_machine_task`）已承载"哪台机织多少匹、计划起止日期"，但只能从织造单工作台逐单进入；无跨单列表、无甘特；创建/更新只有 `end ≥ start` 校验，**同织机时段重叠可随意保存**，双占机台只能靠工长记忆。
2. **染色排缸无日历视图**：外发染色的缸级数据（缸号/排缸日/六态/数量）已在进度反馈覆盖层（ADR-0031）中，但缸卡墙按颜色行组织，工长/跟单员无法按"日历"视角看某天哪些缸在染、排缸是否拥挤。
3. **开工前无齐套核算**：纱线有没有备齐、缺口多少、要不要补采，全靠人工翻台账与采购单。库存流水台账（ADR-0017）与配比 BOM（ADR-0011）、领纱测算口径（ADR-0027）均已就绪，缺一个只读核算入口。

charting Q10=A 已锁定：**轻量排产 + 齐套检查，仅规则计算，不做 AI 优化排程**。本 spec 落地该决策。

## Solution 总览

grilling 两轮 22 问锁定要点：

### 织造排产（Q1–Q4、Q13–Q16）

1. **零新表、零加列、零状态机变更**：织造排产 = 现有机台任务之上的**查询视图层 + Service 冲突校验层**，不建排产单/排产计划实体（对齐 ADR-0034"进度是投影不是表"精神，ADR-0041）。
2. **同织机计划时段冲突硬拦截**：create/update 机台任务时，同 `loom_id`、计划日期闭区间相交、状态为 PENDING/IN_PROGRESS/HALTED 的他任务存在即报错（错误码 `1-030_036_000`，返回冲突任务号与日期）。COMPLETED 任务不拦截新排；无日期任务不参与。**存量已重叠数据不回刷**，甘特与列表以红色冲突标记呈现。
3. **不做机台能力矩阵**：织机不扩可织幅宽/品类字段，仅保留"仅 ACTIVE 织机可选"现状校验。
4. **排产中心页**：开源甘特库 **vue-ganttastic（MIT）** 渲染资源型甘特（行=织机、条=机台任务），支持日/周粒度、条带平移改期、右边缘拉伸改结束日；跨行拖拽若库能力不稳则降级为弹窗织机下拉转机；乐观更新（先动条带→调现有 update 接口→失败回滚并弹冲突错误）。页内含**待排池**（无日期任务 + 未排满织造单聚合）与跨单任务列表。
5. 甘特条带编辑守卫：COMPLETED 只读；HALTED 锁定（须先恢复）；IN_PROGRESS 可改期/转机；无日期任务只进待排池。

### 染缸排缸（Q5–Q6、Q18）

6. **不建染缸设备台账**：染色外发，缸是染厂的设备，建本厂染缸主数据业务不成立（已核实老系统 `DyeingVat` 同样是缸次记录而非设备台账）。排缸日历所需字段反馈缸表已全有，**不加表不加列**。
7. **排缸日历**：进度反馈页内新增"缸卡墙/月历"视图切换（不单独开菜单）。月历按天展示缸点（状态色），点缸用 el-date-picker 改排缸日（复用现有反馈缸 update 接口与权限，非 COMPLETED 缸可改）。el-calendar 不支持跨格拖拽，刻意不做拖拽改期。

### 齐套检查（Q7–Q10、Q17）

8. **纯实时试算、不落检查记录表**：对象=用户勾选织造单（默认提供"全选未完工"DRAFT/ACTIVE），需求口径复用领纱测算（织造单计划重量 × 配比 ratio/100，按 yarnSpecId 聚合）；外发胚布单（OUTSOURCE）天然不参与（无织造单）。
9. **可用库存口径**：台账实时余额，默认全仓库合计（仓库可筛选）；领纱 YARN_OUT 已实时扣减，不重复扣"已领"；**不做库存预留/冻结**，排产不锁库存，齐套是时点只读快照。
10. **在途纱线**：= Σ ACTIVE（进行中）纱线采购单明细 `purchase_qty`（DRAFT 未正式下达不计、COMPLETED 视为完结、CANCELLED 排除）；页面默认"不含在途"，需求量/可用/在途/两个缺口列常驻展示。该口径无法扣减部分到货（入库单与采购单无关联字段），依赖人工及时把收齐的采购单置 COMPLETED——已知限制写入 spec。
11. **建议采购量** = max(0, 需求 − 可用 − 在途)（含在途缺口，避免对已下单量重复建议采购）；缺料行提供"去纱线采购单"跳转，**不自动建采购单、不做 Excel 导出**（避免形成采购单第二套生成路径）。
12. 无配比 BOM、无计划重量的织造单分别列入"无法试算"区并提供跳转。

### 排产与进度联动（Q11）

13. **不加持久化字段、不改织造单状态机**；排产标记（未排/部分排/已排满/生产中/已完工）全部实时派生。机台任务完成→布票推导→织造单状态推进的现有链路（ADR-0024）不变，排产模块零回写。

### 工程落点（Q19–Q22）

14. 新增两个生产管理菜单：**10510 排产中心**、**10520 齐套检查**；排缸日历挂进度反馈页内。错误码段位 `1-030-036`（仅落 000 一个冲突码）；**无新字典**（排产标记前端派生不入字典）；无新表故无新序列。
15. DoD 裁剪：本模块无独立单据实体，不进回收站、不做套打模板（浏览器打印兜底），理由见"统一 DoD 对照"。

---

## 1. 数据模型变更

**零新表、零加列。** 全部数据来自既有表：

- `lunaerp_production_machine_task`（机台任务：loom_id / planned_start / planned_end / planned_qty / status / weaving_order_id）
- `lunaerp_production_loom`（织机 ACTIVE 行）
- `lunaerp_production_weaving_order`（织造单 planned_qty / planned_weight / status）
- `lunaerp_stock_ledger`（余额实时聚合）
- `lunaerp_production_yarn_recipe_bom` + `_item`（配比，ratio 百分比数字）
- `lunaerp_production_yarn_order` + `_item`（在途：ACTIVE 单 purchase_qty）
- `lunaerp_production_progress_feedback`（排缸日历数据源）

### 1.1 索引增强（可选，随性能验证）

冲突检测查询形如：

```sql
SELECT id, code, planned_start, planned_end
FROM lunaerp_production_machine_task
WHERE tenant_id = ? AND deleted = 0 AND loom_id = ?
  AND status IN ('PENDING','IN_PROGRESS','HALTED')
  AND planned_start IS NOT NULL
  AND planned_start <= #{newEnd} AND planned_end >= #{newStart}
  AND id <> #{excludeId}
```

现有 `idx_lb_machine_task_loom_id` 已可支撑（单租户织机百台级、每机任务千级）。仅当 P95 劣化时再加复合索引 `(tenant_id, loom_id, planned_start, planned_end)`，一期不预建。

### 1.2 不做数据库排他约束的已知取舍

时段重叠的严格保证可用 PostgreSQL EXCLUDE 约束（btree_gist + daterange），但：① 依赖 `btree_gist` 扩展，私有化交付环境不可控；② 任务日期可空、软删行需排除、COMPLETED 需豁免，约束谓词复杂；③ 排产是低频写（单厂每天数十次）。故一期以**应用层校验**为准，极端并发下双占由甘特冲突红标 + 人工改期兜底，不引入排他约束（ADR-0041）。

---

## 2. 织造排产：冲突校验（现有 Service 增强）

### 2.1 校验规则

在 `MachineTaskServiceImpl#createMachineTask`（步骤 4 之后）与 `#updateMachineTask`（步骤 4 之后、updateById 之前）增加 `validateLoomTimeConflict`：

- 参与冲突的任务（库内既有行）：同租户、未删、同 `loom_id`、`status ∈ (PENDING, IN_PROGRESS, HALTED)`、`planned_start/planned_end` 至少一个非空、`id ≠` 自身（update 时）。
- 本次提交的有效区间：`effStart = coalesce(plannedStart, plannedEnd)`，`effEnd = coalesce(plannedEnd, plannedStart)`；两端皆空 → 跳过冲突校验（进待排池，不占时段）。
- 相交判定（**闭区间，同日即冲突**）：`existing.effStart ≤ effEnd AND existing.effEnd ≥ effStart`。
- 命中首条冲突即抛 `1_030_036_000`，消息参数：冲突任务编号、冲突任务起止日期。
- 其余既有校验（织造单状态、ACTIVE 织机、正整数匹数、end≥start、编号唯一、超量软警告）顺序与行为不变。**超量仍为软警告不拦截**。

### 2.2 影响面（行为变更，显式确认）

- 织造单工作台现有机台任务 Tab（TasksTab）的新建/编辑**同步受硬拦截**——同一套 Service，不提供排产中心专用旁路。
- COMPLETED 任务的历史区间永不拦截新排；存量重叠数据不迁移、不报错，仅在甘特/列表红标。
- HALTED 任务占机台：与 HALTED 冲突同样拦截（须先恢复/改期）。

### 2.3 删除守卫

沿用现状，本 spec **不改变**删除规则：仅 PENDING 可删；删除时其下属布票按现有 `weavingTicketService.deleteByMachineTaskId` 级联处理；已开工（IN_PROGRESS/HALTED/COMPLETED）不可删。甘特页与排产列表**不放置删除按钮**，删除操作仍只在织造单工作台机台任务 Tab 进行。

---

## 3. 织造排产：查询接口

新增子包 `production.scheduling`（只读查询 + 聚合，无 DO、无 Mapper 新表；复用 Loom/MachineTask/WeavingOrder 的 Mapper）：

```
com.luna.lunasrv.module.lunaerp.{controller,service}.production.scheduling
├── SchedulingController.java
├── SchedulingService.java / SchedulingServiceImpl.java
└── vo/
    ├── GanttRespVO.java            # 织机行 + 任务条带
    ├── GanttBarVO.java
    ├── PendingPoolRespVO.java
    └── PendingOrderVO.java / PendingTaskVO.java
```

### 3.1 甘特数据

`GET /lunaerp/production/weaving-order-task/gantt`

权限：`lunaerp:production:weaving-order-task:query`（与现有机台任务查询同权，不新增权限码）。

入参：

| 参数 | 必填 | 说明 |
|---|---|---|
| start | 是 | 窗口起（yyyy-MM-dd） |
| end | 是 | 窗口止（yyyy-MM-dd），跨度 ≤ 92 天 |
| loomIds | 否 | 织机筛选；缺省全部 ACTIVE 织机 |
| statuses | 否 | 任务状态筛选（PENDING/IN_PROGRESS/HALTED/COMPLETED） |

返回：

```jsonc
{
  "looms": [
    { "loomId": 1, "loomNo": "L-01", "model": "喷气 230" }  // 含窗口内无任务的空行；INACTIVE 织机不返回
  ],
  "bars": [
    {
      "taskId": 10, "taskCode": "MT-WO2026100701-01",
      "loomId": 1,
      "weavingOrderId": 7, "weavingOrderCode": "WO2026100701",
      "productCode": "P001", "productName": "纯棉汗布",
      "plannedQty": 30, "actualQty": 12,
      "workerName": "张三",
      "plannedStart": "2026-10-08", "plannedEnd": "2026-10-12",
      "status": "IN_PROGRESS",
      "conflict": false        // 与其他占用态任务时段相交即 true（存量红标数据源）
    }
  ]
}
```

规则：条带取两日期皆非空（或仅一非空时按 §2.1 eff 区间与窗口相交即返回）；`conflict` 由后端按 §2.1 同口径计算（不计自身）。窗口跨度非法走框架参数校验。

### 3.2 跨单任务分页

现有 `GET /lunaerp/production/weaving-order-task/page` 的 `weavingOrderId` 由必填改**可选**（VO 上去掉 @NotNull，Mapper 已为 eqIfPresent 则无需改 SQL；确认排序按 planned_start 升、创建时间降）。响应行补充织造单号/产品名快照字段（join 织造单取，Mapper XML 或二次批量查询，禁止 N+1）。增加可选参数 `loomId`、`conflictOnly`（只看冲突红标任务）。

### 3.3 待排池

`GET /lunaerp/production/weaving-order-task/pending-pool`（权限同上）

返回两组：

```jsonc
{
  "unDatedTasks": [            // 组一：有任务但没排日期（status=PENDING）
    { "taskId": 11, "taskCode": "MT-...-02", "weavingOrderId": 7, "weavingOrderCode": "...",
      "loomId": 1, "loomNo": "L-01", "plannedQty": 10 }
  ],
  "unScheduledOrders": [       // 组二：未排满的织造单（DRAFT/ACTIVE，未取消未完工）
    { "weavingOrderId": 8, "weavingOrderCode": "...", "productCode": "P002", "productName": "...",
      "plannedQty": 50, "scheduledQty": 30, "remainingQty": 20 }
  ]
}
```

- `scheduledQty` = 该织造单下 PENDING/IN_PROGRESS/HALTED 任务 plannedQty 合计；`remainingQty = plannedQty − scheduledQty > 0` 才入池。
- 完全没有任务的 DRAFT/ACTIVE 织造单也入组二（scheduledQty=0）。
- COMPLETED/CANCELLED 织造单排除。

### 3.4 派工写入口

不新增写接口：待排池/页头"派工"弹窗统一调现有 `POST /weaving-order-task`（create），字段：织造单（远程搜索，仅 ACTIVE 未取消；组二行带入单 ID 与默认匹数=remainingQty）、织机（仅 ACTIVE）、起止日期、计划匹数、挡车工。冲突由 §2.1 拦截，前端收到 000 后保持弹窗打开并红字展示冲突任务号。

---

## 4. 齐套检查：只读试算

新增子包：

```
com.luna.lunasrv.module.lunaerp.{controller,service}.production.materialcheck
├── MaterialCheckController.java
├── MaterialCheckService.java / MaterialCheckServiceImpl.java
└── vo/
    ├── MaterialCheckReqVO.java          # weavingOrderIds / warehouseIds / includeInTransit
    ├── MaterialCheckRowVO.java          # 纱线规格聚合行
    ├── MaterialCheckContributionVO.java # 织造单贡献展开行
    └── MaterialCheckRespVO.java         # rows + missingBomOrders + missingWeightOrders
```

`POST /lunaerp/production/material-check/run`（纯只读试算，无写操作、不落库）；权限 `lunaerp:production:material-check:query`。

### 4.1 入参

| 字段 | 说明 |
|---|---|
| weavingOrderIds | 织造单 ID 列表；空列表=试算全部 DRAFT/ACTIVE 织造单（前端"全选未完工"） |
| warehouseIds | 仓库筛选；空=全仓库合计 |
| includeInTransit | 是否含在途（缺省 false；仅影响汇总徽标口径，缺口两列常驻） |

### 4.2 试算口径

对每张入选织造单（须存在、未删、租户内、状态 ∈ DRAFT/ACTIVE）：

1. 按 `productId` 取 `yarn_recipe_bom_item`（ratio 百分比数字，49=49%）。**无 BOM** → 整单入 `missingBomOrders`，不参与行聚合。
2. `plannedWeight` 为空或 `plannedQty` 为空/0 → 整单入 `missingWeightOrders`。
3. 单匹计划重量 = plannedWeight / plannedQty；某纱线规格需求 KG = plannedQty × 单匹计划重量 × ratio/100 = **plannedWeight × ratio/100**（全单口径，2 位小数 HALF_UP）。
4. 按 `yarnSpecId` 跨单合并需求量；每行保留 `contributions`（织造单号/产品/本单需求 KG）供展开。

可用库存：`SUM(stock_ledger.quantity)` WHERE item_type='YARN_SPEC' AND yarn_spec_id=? AND deleted=0（仓库 ID 在 warehouseIds 内或缺省全仓），负数流水（YARN_OUT 等）自然参与求和；新增 Mapper 批量聚合方法（IN yarnSpecIds + 可选 warehouseIds，GROUP BY yarn_spec_id），禁止逐规格循环查余额。

在途：Σ `yarn_order_item.purchase_qty` WHERE 主表 status='ACTIVE' AND tenant、按 yarn_spec_id 聚合（一条 SQL join 主子表 GROUP BY）。

### 4.3 输出行

| 列 | 口径 |
|---|---|
| 需求量 | Σ 织造单需求 |
| 可用库存 | 台账实时余额 |
| 在途量 | ACTIVE 采购单 purchase_qty 合计 |
| 缺口（不含在途） | max(0, 需求 − 可用) |
| 缺口（含在途） | max(0, 需求 − 可用 − 在途) |
| 建议采购量 | = 缺口（含在途），2 位小数；为 0 显示"已齐套/在途中" |
| 齐套状态 | 前端派生标签：齐套（可用≥需求）/ 在途可覆盖（含在途缺口=0）/ 缺料（含在途缺口>0）；**不入字典** |

`missingBomOrders`：织造单号/产品名 + "去配比 BOM"跳转（yarn-recipe-bom 管理页按 productId 过滤）。
`missingWeightOrders`：织造单号 + "去织造单"跳转（补计划重量/匹数）。

### 4.4 已知口径限制（写入页面说明 tooltip）

- 在途以"采购单状态"近似：部分到货无法自动扣减（入库单不关联采购单），需人工及时把收齐的 ACTIVE 采购单置 COMPLETED；未及时完结会高估在途。
- 试算为时点快照，不预留/不冻结库存；试算后库存变动不会通知。
- 需求按计划重量理论折算，不含实际损耗差异；胚布单 `loss_rate` 口径（ADR-0010）不参与织造齐套。

---

## 5. 排缸日历（进度反馈视图扩展）

### 5.1 后端

`GET /lunaerp/production/progress-feedback/calendar`（权限复用 `lunaerp:production:progress-feedback:query`）

入参 `month=2026-10`（或 from/to 一对日期，二选一）、可选 `dyeingFactoryId`、`status`。返回月内（或区间内）**扁平缸列表**（非缸卡墙的颜色行骨架结构）：

```jsonc
{
  "vats": [
    { "feedbackId": 3, "dyeingOrderId": 9, "dyeingColorId": 21,
      "vatNo": "A-12", "scheduledDate": "2026-10-09",
      "status": "DYEING", "quantity": 500.00, "pieces": 20, "unit": "KG",
      "customerColorNo": "C101", "colorName": "藏青",
      "contractNo": "CH2026-001", "dyeingFactoryId": 5, "dyeingFactoryName": "永盛染厂" }
  ]
}
```

口径：`scheduled_date` 落在区间内即返回（含 COMPLETED，用于回看；status 筛选可过滤）；仅 ACTIVE/COMPLETED 染色单（沿用进度反馈既有骨架口径，CANCELLED 排除）；零展示快照原则不变，展示字段实时 join 染色单/颜色行。新增 Service 只读方法 + Mapper 查询，不改表。

### 5.2 改期

点击缸点 → 弹窗 el-date-picker 选择新排缸日 → 调**现有**反馈缸更新接口（`PUT /progress-feedback`，权限 10402）：COMPLETED 缸禁改（前端禁用 + 后端现有状态守卫兜底）；同单同色缸号唯一性与缸记录不受影响（只改日期）。成功后局部刷新当月数据。

---

## 6. 前端

### 6.1 排产中心（菜单 10510，视图 `production/scheduling/index.vue`）

页面三区，顶部统一筛选（日期窗口、织机、状态）与"日/周"粒度切换：

1. **甘特区**（vue-ganttastic）：
   - `GGanttChart` 行 = ACTIVE 织机（机台号+型号），条 = 机台任务；条内文字：织造单号尾号 + 计划匹数；条色按任务状态（PENDING info / IN_PROGRESS primary / HALTED danger / COMPLETED success，与字典色一致），`conflict=true` 叠加红色描边+斜纹。
   - 交互：水平拖动平移起止、右边缘拉伸结束日（天粒度）；拖动结束乐观更新调 update 接口，失败（000 冲突等）回滚条带位置并弹错误（含冲突任务号）；COMPLETED 条带禁用一切拖动；HALTED 条带禁用拖动，提示先恢复。
   - **转机降级方案**：若实测 vue-ganttastic 跨行拖拽不可靠，条带点击弹出任务弹窗，弹窗内织机下拉转机（平移/拉伸仍走甘特）；实现前先做最小 spike 验证，结论写进实现记录。
   - 无日期任务不渲染在甘特，仅出现在待排池。
   - 点击条带侧拉抽屉：任务详情（织造单/匹数/挡车工/状态）+ 跳转织造单工作台。
2. **待排池区**（甘特上方可折叠横条或左侧栏）：两个分组（未排日期任务 / 未排满织造单），行内"派工"按钮弹派工弹窗（§3.4）；派工成功后刷新甘特与池。
3. **任务列表区**（el-table 切换 Tab）：跨单分页（§3.2），列=任务号/织造单/产品/织机/挡车工/计划匹数/实织/起止/状态标签/冲突标记；只提供编辑（弹窗）与跳转，不放删除/停机按钮（仍归织造单工作台）。

依赖：新增 `vue-ganttastic`（MIT，原生 Vue3 Composition API；安装时在 package.json 锁定具体版本）。**不使用**已随芋道底座存在的 `dhtmlx-gantt`——其社区版为 GPL v2，与一期私有化商业化买断的发布方式冲突；vue-ganttastic 主题经 CSS 变量对齐 ElementPlus，不引入第二套视觉语言。

API 目录：`src/api/lunaerp-elmui/production/scheduling/index.ts`（gantt/pending-pool），并在现有 `api/.../machine-task/index.ts` 扩展跨单分页参数；派工复用现有 create。

### 6.2 齐套检查（菜单 10520，视图 `production/material-check/index.vue`）

- 顶部筛选：织造单多选（el-select multiple 远程搜索，旁置"全选未完工"按钮）、仓库多选（缺省全部）、"含在途"开关（缺省关，旁置 ⓘ 说明在途口径与限制）。
- 结果表（el-table，可展开行）：纱线材质/规格、需求量、可用库存、在途量、缺口（不含在途）、缺口（含在途）、建议采购量、状态标签；展开显示各织造单贡献量。
- 顶部汇总徽标：缺料规格数 / 缺料合计 KG（口径随"含在途"开关切换）。
- 两个无法试算分区（el-alert 列表）：缺配比（跳配比 BOM）、缺计划重量（跳织造单）。
- 缺料行按钮"去纱线采购单"（跳转 yarn-order 列表页，带纱线规格关键词）；不做一键建单、不做导出。
- API：`src/api/lunaerp-elmui/production/material-check/index.ts`。

### 6.3 排缸日历（进度反馈页内）

`production/progress-feedback/index.vue` 顶部加"缸卡墙 / 排缸日历"segmented 切换；新增 `components/VatCalendar.vue`：el-calendar 月历，日期格内渲染当日缸点（缸号+数量，圆点/边框取状态字典色，COMPLETED 灰化），月切换/今天回位/染厂与状态筛选；缸点点击弹改期小弹窗（el-date-picker，COMPLETED 禁用）。缸卡墙现有交互零改动。

---

## 7. API 汇总

| 方法 | 路径 | 权限码 | 说明 |
|---|---|---|---|
| GET | `/lunaerp/production/weaving-order-task/gantt` | `weaving-order-task:query` | 甘特数据（织机行+条带+冲突标记） |
| GET | `/lunaerp/production/weaving-order-task/page` | 同上 | 现有分页增强：weavingOrderId 改可选，加 loomId/conflictOnly，补织造单/产品快照 |
| GET | `/lunaerp/production/weaving-order-task/pending-pool` | 同上 | 待排池两组聚合 |
| POST | `/lunaerp/production/weaving-order-task` | `weaving-order-task:create` | 现有派工接口（新增冲突硬校验，无新端点） |
| PUT | `/lunaerp/production/weaving-order-task` | `weaving-order-task:update` | 现有更新接口（甘特拖拽/转机落此，新增冲突硬校验） |
| POST | `/lunaerp/production/material-check/run` | `material-check:query` | 齐套只读试算 |
| GET | `/lunaerp/production/progress-feedback/calendar` | `progress-feedback:query` | 排缸月历扁平缸数据 |
| PUT | `/lunaerp/production/progress-feedback` | `progress-feedback:update`（10402） | 现有反馈缸更新（日历改期复用） |

写动作（create/update 机台任务）挂通用操作日志（随 04 号商业地基通用件日志契约；未落地前沿用控制器现有日志实践，不自定义日志表）。齐套试算与所有查询无日志。

## 8. 菜单与权限

新建 SQL：`luna-srv/sql/postgresql/lunaerp-production-scheduling-material-check.sql`（仅菜单与角色授权，无建表/无字典）。

| ID | 名称 | 类型 | 权限码 | 位置 |
|---|---|---|---|---|
| 10510 | 排产中心 | 菜单（sort 12） | `lunaerp:production:weaving-order-task:query` | 10200 生产管理 |
| 10520 | 齐套检查 | 菜单（sort 13） | `lunaerp:production:material-check:query` | 10200 生产管理 |

- 排产中心**不新增按钮位**：页内派工/改期/转机复用 `weaving-order-task:create/update`（现有权限或织造单 update 兜底，沿用 MachineTaskController 现有 @PreAuthorize 组合）。
- 齐套检查纯只读单菜单，无按钮位。
- 排缸日历不占菜单、不加权限（进度反馈页内视图）。
- 组件路径：`lunaerp-elmui/production/scheduling/index`、`lunaerp-elmui/production/material-check/index`；图标建议 `ep:calendar` / `ep:files`。
- 超管角色（super_admin/admin，角色 1/2）授权沿用各 SQL 现有 `system_role_menu` 模式。

## 9. 错误码（段位 `1-030-036`，避让 035 下达工作台）

| 码 | 说明 |
|---|---|
| 1_030_036_000 | 织机【{loomNo}】在 {start}~{end} 已排任务【{taskCode}】，计划时段冲突，请改期或转机 |

段位内 001+ 预留。齐套/日历/甘特均为只读查询，参数非法走框架校验，不设业务错误码。

## 10. 字典

**无新增字典。** 任务状态复用 `lunaerp_weaving_task_status`（2239），反馈缸状态复用 `lunaerp_progress_feedback_status`（2247）；排产覆盖状态（未排/部分排/已排满/生产中/已完工）与齐套状态（齐套/在途可覆盖/缺料）均为前端实时派生标签，不持久化、不入字典（与生产进度预警 ADR-0034 同口径）。

## 11. 回收站与打印

- **不注册回收站 handler**：本模块无独立主数据/单据实体。机台任务不进回收站（派工记录非主数据，删除沿用既有 PENDING 守卫）；齐套试算与日历查询无落库数据。
- **不做套打模板**：排产中心/齐套/日历均为交互看板，无固定纸质单据形态；确需纸质场景由浏览器打印兜底。打印内核（04 号通用件）无本模块契约。

## Testing Decisions

只测外部行为，纯 Mockito（`SchedulingServiceImplTest`、`MachineTaskConflictTest`、`MaterialCheckServiceImplTest`、`ProgressFeedbackCalendarTest`）。

**冲突校验**
1. 新建任务与同织机 PENDING 任务日期闭区间相交（含同日）→ 000，消息含冲突任务号；事务回滚。
2. 不相交/不同织机/他任务 COMPLETED/自身（update 排除 id）→ 通过。
3. 仅填一个日期：按 coalesce 单日区间判定；两端皆空 → 不校验。
4. 与 HALTED 任务相交 → 拦截；与 IN_PROGRESS 相交 → 拦截。
5. update COMPLETED 任务本身仍被既有守卫拦截（101）。
6. 超量软警告仍返回 warning 且不拦截（回归既有行为）；织造单工作台入口同样被硬拦截（同一 Service）。

**甘特/待排池**
7. 甘特返回全部 ACTIVE 织机（含窗口内无任务空行），INACTIVE 织机不返回；窗口外条带不返回。
8. conflict 标记：存量互相重叠的两条任务均标 true；COMPLETED 参与展示但不令他任务标冲突。
9. 待排池：无日期 PENDING 任务入组一；Σ占用态任务匹数 < plannedQty 的 DRAFT/ACTIVE 织造单入组二且 remainingQty 正确；COMPLETED/CANCELLED 单排除。
10. 跨单分页：weavingOrderId 缺省返回多单数据；conflictOnly 过滤生效；快照字段无 N+1（批量查询验证）。

**齐套试算**
11. 两织造单同产品 → 按 yarnSpecId 合并需求 = Σ(plannedWeight × ratio/100)，贡献行可展开。
12. 可用库存=台账正负流水求和（含 YARN_OUT 扣减）；仓库筛选生效；缺省全仓。
13. 在途仅计 ACTIVE 采购单 purchase_qty（DRAFT/COMPLETED/CANCELLED 排除）；两个缺口列与建议采购量（含在途口径）计算正确；齐套/在途可覆盖/缺料三态。
14. 无 BOM 单入 missingBomOrders；缺 plannedWeight 单入 missingWeightOrders；均不污染聚合行。
15. 租户隔离：他租户织造单/库存/采购单不进入试算；入选不存在或他租户织造单 ID 抛不存在类错误。

**排缸日历**
16. month 区间只返回 scheduled_date 落在月内的缸；COMPLETED 缸默认返回（回看）且 status 筛选可过滤；CANCELLED 染色单的缸不返回；展示快照字段 join 正确。

**并发（已知边界）**
17. 不做并发双占的数据库级测试（一期无排他约束）；Service 校验为单事务内先查后写，该取舍见 ADR-0041。

## 前端验收清单

- [x] 排产中心：甘特按织机分行渲染任务条，状态色正确，存量冲突条红描边
- [x] 拖动改期/拉伸：成功乐观更新；制造冲突后拖动被回滚并弹冲突任务号
- [x] COMPLETED 不可拖；HALTED 不可拖并提示恢复；无日期任务只出现在待排池
- [x] 待排池派工：默认匹数=剩余未排量；同织机同日双占报 000；超量仍只警告可保存
- [x] 织造单工作台机台任务 Tab 新建/编辑冲突同样被拦截
- [x] 日/周粒度切换、织机/状态筛选、跨单任务列表（含冲突筛选）
- [x] 齐套：勾选织造单试算，需求量/库存/在途/两缺口/建议量与手工台账核对一致；展开贡献行正确
- [x] 缺配比、缺计划重量分区与跳转；缺料行跳纱线采购单
- [x] 含在途开关只改汇总徽标，两缺口列始终可见
- [x] 排缸日历：月切换/今天/染厂状态筛选；点缸改期成功刷新；COMPLETED 缸禁改
- [x] 无 10510/10520 菜单权限的角色不可见且直连接口 403/越权拦截
- [x] ts:check、lint 通过；甘特在 50 织机 × 500 任务数据量下窗口操作无明显卡顿（>3s 报性能问题）

## 波次冒烟（W2）

合同→下达生产（release-production）→胚布/织造单据生成→织机档案就绪→排产中心派工（含冲突拦截验证）→齐套检查核对→（染色侧）建缸→排缸日历改期，全链走通；与生产进度页（ADR-0034）数据互不回写、口径一致。

## Out of Scope

- **AI 优化排程**：自动派工、智能排程算法、最优机台推荐、交期预测（Q10=A/Q5=A，二期增值包）
- **排产独立实体**：排产单/排产计划/排产版本表，产能日历、工作日历、班次、工时模型
- **机台能力矩阵**：织机可织幅宽/品类/克重约束与能力校验（二期随设备域）
- **库存预留/冻结/锁定**：排产占料、齐套快照留痕、缺料自动请购、安全库存模型
- **自动生成纱线采购单 / 齐套结果导出 Excel / 齐套检查记录留档**
- **精确在途**：采购入库逐单核销（依赖入库单↔采购单关联改造，二期随采购链深化）
- **染缸设备台账、本厂染色排产**（本期染色全部外发；染缸是染厂资源）
- **排缸日历拖拽改期**（el-calendar 能力限制，点击改期已满足）
- **小时/班次级时段粒度**、排他约束（btree_gist）
- **甘特依赖连线/关键路径/里程碑**、dhtmlx-gantt（GPL 不可用）等重型甘特能力
- 机台任务删除入口搬迁（仍归织造单工作台）、机台任务回收站/套打

## 统一 DoD 对照

- [x] 状态机白名单+终态守卫：本模块不引入新状态机；甘特编辑守卫完全复用机台任务四态（ADR-0024）与反馈缸六态（ADR-0031）既有守卫，COMPLETED/HALTED 禁改由 Service 兜底
- [x] 并发条件更新：冲突为读校验+插入（低频写）；已评估排他约束并显式否决（ADR-0041），甘特红标兜底存量/并发双占；本模块无余额/数量累加类字段，不存在覆盖写
- [x] 芋道权限+脱敏：排产复用 weaving-order-task 权限位，齐套新菜单 `material-check:query`，日历复用 progress-feedback:query；本模块无金额字段、无联系人字段，不涉及脱敏
- [x] 列表/详情/删除进回收站：无独立单据实体，显式不进回收站（见 §11）；删除沿用机台任务既有守卫且入口不搬迁
- [x] 打印+操作/审批日志：无套打场景（浏览器打印兜底，显式裁剪）；写操作挂通用操作日志契约，无审批流
- [x] 核心状态流转与库存/账务副作用测试：本模块无写账副作用；核心规则测试为冲突校验、待排池聚合、齐套试算口径、日历口径（Testing Decisions 1–16）
- [x] 金额逻辑必有测试：N/A（无金额字段；建议采购量为数量 KG 非金额，仍纳入口径测试）
- [x] 前端验收清单+波次冒烟：见上节
- [x] 性能：甘特窗口 ≤92 天、织机百台级，单租户任务万级内 P95<1s（复用 loom 索引）；齐套批量聚合禁止 N+1；甘特前端 50×500 数据量操作流畅
- [x] 写接口租户隔离测试：本模块写入口全部复用既有端点（隔离已由 TenantBaseDO 保证并在既有模块测试）；新增试算/聚合查询测试覆盖跨租户织造单、库存、采购单不可见
- [x] 横向/纵向越权：无菜单权限 403、菜单不可见；齐套试算传他租户织造单 ID 报不存在；甘特/日历不暴露他租户织机/缸
- [x] 术语与错误码归位：排产/待排池/齐套检查/在途纱线/排缸日历/缸次进 production GLOSSARY；`1-030-036` 入 shared 段位总表与 production 错误码节

## Further Notes

### 已落地/关联文档

- [ADR-0041](../../docs/adr/0041-lightweight-scheduling-readonly-view-no-stock-freeze.md) — 轻量排产与齐套：只读视图+既有实体校验，不建排产表/染缸台账/库存冻结
- [ADR-0017](../../docs/adr/0017-universal-stock-ledger.md) — 通用库存流水台账（余额实时聚合）
- [ADR-0011](../../docs/adr/0011-yarn-recipe-bom-independent-table.md) — 配比 BOM 独立表
- [ADR-0024](../../docs/adr/0024-weaving-ticket-state-machine.md) — 织造布票/机台任务状态推导
- [ADR-0027](../../docs/adr/0027-yarn-requisition-dispatch-and-reversal.md) — 领纱测算口径与 YARN_OUT 扣减
- [ADR-0031](../../docs/adr/0031-progress-feedback-overlay-on-dyeing-color.md) — 染色缸级进度覆盖层（排缸日历数据源）
- [ADR-0034](../../docs/adr/0034-production-progress-read-only-aggregate-view.md) — 生产进度只读聚合（"投影不是表"先例）

### 实现顺序建议

1. SQL：10510/10520 菜单与角色授权（无表无字典）
2. 后端冲突校验：MachineTaskService validateLoomTimeConflict + 错误码常量 `1_030_036_000` + 冲突/回归单测
3. `production.scheduling`：gantt / pending-pool / 跨单分页增强
4. `production.materialcheck`：试算服务（含库存批量聚合 Mapper、在途聚合）+ 单测
5. progress-feedback calendar 只读端点
6. 前端：vue-ganttastic 选型 spike（跨行拖拽可行性，定转机交互）→ 排产中心三区 → 齐套页 → 排缸日历
7. GLOSSARY + 段位总表更新
