# 09-scheduling-and-material-check: 排产与齐套检查

Type: grilling
Status: resolved
Blocked by: 

## Question

设计轻量排产与齐套检查并产出 spec（W2，Q10=A），grilling 时先调 Skill "grilling" 与 "domain-modeling"：

1. 织造排产：机台+计划时段占用模型；与现有机台任务（weaving-execution 的 machine task scheduling，见 `.scratch/weaving-execution/issues/04-machine-task-scheduling.md`）的关系——扩展现有机台任务还是新排产表？必须先读现有实现再定；冲突校验（时段重叠/机台能力）；列表+甘特视图；
2. 染缸排缸：染缸台账+排缸日历（对接染色单颜色明细）；
3. 齐套检查：口径 = 配比 BOM × 计划量 − 可用库存（ADR-0017 台账余额）；需决定是否含在途纱线（在途的定义：已下单未入库？）；缺料时输出建议采购量；
4. 排产与进度联动：机台任务完成后排产状态的推进方式；
5. 智能化边界：仅规则计算（Q10=A），AI 优化排程写进非目标。

产出：`.scratch/scheduling-material-check/spec.md`（按统一 DoD）。术语（排产/齐套/缸次）进生产域 GLOSSARY。

## Answer

grilling 两轮 22 问全部按推荐锁定（Q4 改选开源甘特库，定稿 vue-ganttastic/MIT，弃用底座自带的 GPL 版 dhtmlx-gantt）。产出：

- spec：[`.scratch/scheduling-material-check/spec.md`](../../scheduling-material-check/spec.md)（ready-for-agent，含统一 DoD 对照、17 项单测决策、前端验收与波次冒烟）
- ADR：[ADR-0041](../../../../docs/adr/0041-lightweight-scheduling-readonly-view-no-stock-freeze.md)（只读视图+既有实体校验，不建排产表/染缸台账/库存冻结；含排他约束否决与二期路径）
- 术语：排产、待排池、排产覆盖状态、齐套检查、在途纱线、排缸日历、缸次进 [production GLOSSARY](../../../../docs/contexts/production/GLOSSARY.md)；机台任务条目补冲突拦截口径；错误码 `1-030-036` 入段位总表（shared）与生产域错误码节（顺带补登 035）。

核心结论：

1. **织造排产零新表零加列**：机台任务是唯一机台占用事实源；新增甘特/待排池/跨单分页只读接口（`production.scheduling`）+ 在现有 MachineTaskService 加同织机计划时段闭区间相交硬拦截（PENDING/IN_PROGRESS/HALTED 参与、COMPLETED 豁免、无日期进待排池、同日即冲突；存量重叠不回刷只红标；不做 btree_gist 排他约束）。派工/改期复用现有 create/update，织单工作台入口同受校验。
2. **染缸侧零新表**：外发模式不建染缸设备台账；排缸日历是进度反馈页内月历视图（新只读 calendar 端点，点击 el-date-picker 改期复用反馈缸 update，不做跨格拖拽）。
3. **齐套只读试算不落表**：需求=织造单计划重量×配比 ratio/100（按纱线规格聚合），可用=台账实时余额（YARN_OUT 已扣，不重复扣已领，不冻结）；在途=ACTIVE 纱线采购单 purchase_qty 合计（近似口径与限制写入 spec）；建议采购量=含在途缺口，只展示+跳转，不自动建单、不导出；无 BOM/无计划重量单列无法试算区。
4. **进度联动零回写**：排产覆盖标记实时派生，不改织造单状态机；菜单 10510 排产中心（复用 weaving-order-task 权限，不设按钮位）、10520 齐套检查（新 material-check:query）；无新字典；唯一错误码 1_030_036_000。
5. 非目标：AI 优化排程/自动派工、机台能力矩阵、库存预留冻结、精确在途核销、小时级粒度与排他约束、染缸设备台账、排缸拖拽、自动建采购单/导出。
