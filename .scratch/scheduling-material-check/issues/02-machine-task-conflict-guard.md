# 02: 机台任务同织机时段冲突硬校验（后端）

**What to build:** 现有机台任务 Service 增强：create（步骤 4 之后）与 update（步骤 4 之后、updateById 之前）新增同织机计划时段冲突校验——同租户、未删、同织机、状态 ∈ PENDING/IN_PROGRESS/HALTED 的库内任务，与本次提交有效区间（coalesce 单端日期，两端皆空跳过）闭区间相交（同日即冲突）即抛错误码 `1_030_036_000`（落 1-030-036 新段位，避让 035 下达工作台），消息含冲突任务编号与起止日期；update 排除自身。COMPLETED 任务不拦截新排；与 HALTED/IN_PROGRESS 相交同样拦截。织造单工作台机台任务 Tab 同一 Service 同步受硬拦截（无排产中心旁路）；超量仍为软警告不拦截；其余既有校验顺序与行为不变。不做数据库排他约束（ADR-0041 取舍）。

**Blocked by:** None (can start immediately)

**Status:** resolved

**参考**：本目录上级 spec.md §2、§9；ADR-0041、ADR-0024

- [x] 新建任务与同织机 PENDING 任务闭区间相交（含同日）→ 报错且事务回滚，消息含冲突任务号
- [x] 不相交/不同织机/他任务 COMPLETED/update 排除自身 → 通过；仅一端日期按 coalesce 单日判定，两端皆空不校验
- [x] 与 HALTED、IN_PROGRESS 任务相交均拦截；COMPLETED 任务自身修改仍被既有守卫（101）拦截
- [x] 超量软警告仍返回 warning 且不拦截（回归既有行为）；织造单工作台入口同样被硬拦截
- [x] 新增错误码常量；纯 Mockito 单测（MachineTaskConflictTest）覆盖以上全部场景

## Comments
- **机台任务同织机时段冲突硬校验**（Commit: `1f7cc49f`）：
  - 新增错误码常量：`ErrorCodeConstants.MACHINE_TASK_LOOM_TIME_CONFLICT = new ErrorCode(1_030_036_000, "织机【{}】在 {}~{} 已排任务【{}】，计划时段冲突，请改期或转机");`，位于 `1-030-036` 段位。
  - 在 `MachineTaskMapper` 中提供 `selectOccupiedTasksByLoomId` 和 `selectListByLoomIdAndStatuses`，查询指定织机下占用态（PENDING / IN_PROGRESS / HALTED）任务。
  - 在 `MachineTaskServiceImpl` 中实现私有校验方法 `validateLoomTimeConflict`：
    1. 本次提交有效区间通过 `effStart = coalesce(plannedStart, plannedEnd)`，`effEnd = coalesce(plannedEnd, plannedStart)` 计算；起止皆空则跳过校验（进待排池，不占时段）。
    2. 遍历同织机库内占用态任务，update 时排除自身 ID，已完工任务（COMPLETED）不参与拦截；库内任务单端日期同样按 coalesce 处理。
    3. 闭区间相交判定：`existing.effStart <= effEnd && existing.effEnd >= effStart`（同日即冲突）。
    4. 命中首条冲突即抛出 `1_030_036_000`，错误提示包含织机机台号、已有任务起止日期与已有任务编号；事务自动回滚。
  - 将 `validateLoomTimeConflict` 分别织入 `createMachineTask`（步骤 4 之后）与 `updateMachineTask`（步骤 4 之后、updateById 之前）；织造单工作台机台任务 Tab 与排产中心派工/改期共用同一校验。
  - 超量软警告逻辑保持不变，超量仅返回 warning 并不拦截保存；COMPLETED 任务自身修改保持既有状态守卫拦截。
  - 新增单元测试 `MachineTaskConflictTest`，共 17 个用例全面覆盖 Testing Decisions 1-6 所有场景（包含同日相交、边界重叠、内部包含、前后紧邻不相交、不同织机、COMPLETED 豁免、update 排除自身、单端日期 coalesce、两端皆空跳过、HALTED/IN_PROGRESS 拦截、COMPLETED 自身守卫、超量软警告回归），46/46 测试全部通过。
