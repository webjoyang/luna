# 04: 排产中心后端——甘特 / 待排池 / 跨单分页

**What to build:** 新增 production.scheduling 只读子包（无 DO、无新表，复用织机/机台任务/织造单既有 Mapper）：① 甘特数据端点——窗口 ≤92 天，返回全部 ACTIVE 织机行（含窗口内无任务的空行，INACTIVE 不返回）+ 任务条带（织造单/产品/匹数/挡车工/状态快照，条带日期与窗口相交即返回，conflict 红标按 02 同口径计算且不计自身，COMPLETED 参与展示但不令他任务标冲突），入参 start/end/loomIds/statuses；② 待排池端点——组一无日期 PENDING 任务（含织机快照），组二未排满 DRAFT/ACTIVE 织造单（scheduledQty=占用态任务匹数合计，remainingQty>0 才入池，完全无任务单 scheduledQty=0 也入池，COMPLETED/CANCELLED 排除）；③ 现有机台任务分页增强——响应行补织造单号/产品名快照（批量查询禁 N+1），加 conflictOnly 只看冲突过滤，确认排序 planned_start 升、创建时间降。权限复用 weaving-order-task:query。派工不新增写接口（复用现有 create）。

**Blocked by:** 02: 机台任务同织机时段冲突硬校验（后端）——甘特 conflict 口径须与冲突校验同源

**Status:** resolved

**参考**：本目录上级 spec.md §3、§7；ADR-0041

- [x] 甘特返回含空行的全部 ACTIVE 织机，INACTIVE 不返回；窗口外条带不返回；conflict 标记存量互叠任务均 true、COMPLETED 不影响他任务
- [x] 待排池两组口径正确：无日期 PENDING 入组一；Σ占用态匹数 < plannedQty 入组二且 remainingQty 正确；无任务单入池；COMPLETED/CANCELLED 单排除
- [x] 跨单分页 weavingOrderId 缺省返回多单数据；loomId/conflictOnly 过滤生效；快照字段批量查询无 N+1；排序符合预期
- [x] 纯 Mockito 单测（SchedulingServiceImplTest）覆盖以上全部场景

## Comments
- **排产中心后端甘特、待排池与跨单分页**（Commit: `5ccbd0df`, Merge: `fde31256`）：
  - 新增包 `com.luna.lunasrv.module.lunaerp.controller.production.scheduling` 与 `service.production.scheduling`，无新表无 DO，纯只读聚合层。
  - VO 设计：`GanttRespVO`（looms: List<GanttLoomVO>, bars: List<GanttBarVO>）、`GanttQueryReqVO`（支持 start/end、跨度 <= 92 天校验、loomIds、statuses）、`PendingPoolRespVO`（unDatedTasks: List<PendingTaskVO>, unScheduledOrders: List<PendingOrderVO>）。
  - 甘特数据端点 `GET /lunaerp/production/weaving-order-task/gantt`（权限 `lunaerp:production:weaving-order-task:query`）：
    1. 查询全部 ACTIVE 织机行（包含窗口内无任务的空行，INACTIVE 织机排除，支持 loomIds 过滤）；
    2. 提取与窗口相交的条带（闭区间相交判定，支持单端日期 coalesce）；
    3. conflict 标记与 Issue 02 冲突校验同源（`SchedulingConflictUtils`）：存量互叠的占用态任务（PENDING/IN_PROGRESS/HALTED）两两均标 true，COMPLETED 任务展示但自身 conflict=false 且不令他任务标冲突；
    4. 批量查询织造单号/产品快照，杜绝 N+1 查询。
  - 待排池端点 `GET /lunaerp/production/weaving-order-task/pending-pool`（权限同上）：
    1. 组一（unDatedTasks）：无日期 PENDING 任务（起止皆空），附带织机与织造单快照；
    2. 组二（unScheduledOrders）：未排满 DRAFT/ACTIVE 织造单（排除 COMPLETED/CANCELLED），scheduledQty=占用态任务匹数求和，remainingQty=plannedQty - scheduledQty > 0 入池，0 任务单 scheduledQty=0 亦入池。
  - 现有机台任务分页与详情增强：
    1. `MachineTaskPageReqVO` 的 `weavingOrderId` 可选，新增可选 `loomId` 与 `conflictOnly`（boolean）；
    2. 排序调整为 `planned_start ASC, id DESC`；
    3. `MachineTaskRespVO` 补充 `weavingOrderCode`, `productCode`, `productName`, `conflict` 快照字段；
    4. 批量回填织造单与冲突标记，无 N+1 查询。
  - 单元测试与验证：新增 `SchedulingServiceImplTest`（覆盖 Testing Decisions 7-10 全部场景）和 `SchedulingControllerTest`，全部用例通过。
