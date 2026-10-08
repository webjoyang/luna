# 02: 机台任务同织机时段冲突硬校验（后端）

**What to build:** 现有机台任务 Service 增强：create（步骤 4 之后）与 update（步骤 4 之后、updateById 之前）新增同织机计划时段冲突校验——同租户、未删、同织机、状态 ∈ PENDING/IN_PROGRESS/HALTED 的库内任务，与本次提交有效区间（coalesce 单端日期，两端皆空跳过）闭区间相交（同日即冲突）即抛错误码 `1_030_036_000`（落 1-030-036 新段位，避让 035 下达工作台），消息含冲突任务编号与起止日期；update 排除自身。COMPLETED 任务不拦截新排；与 HALTED/IN_PROGRESS 相交同样拦截。织造单工作台机台任务 Tab 同一 Service 同步受硬拦截（无排产中心旁路）；超量仍为软警告不拦截；其余既有校验顺序与行为不变。不做数据库排他约束（ADR-0041 取舍）。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

**参考**：本目录上级 spec.md §2、§9；ADR-0041、ADR-0024

- [ ] 新建任务与同织机 PENDING 任务闭区间相交（含同日）→ 报错且事务回滚，消息含冲突任务号
- [ ] 不相交/不同织机/他任务 COMPLETED/update 排除自身 → 通过；仅一端日期按 coalesce 单日判定，两端皆空不校验
- [ ] 与 HALTED、IN_PROGRESS 任务相交均拦截；COMPLETED 任务自身修改仍被既有守卫（101）拦截
- [ ] 超量软警告仍返回 warning 且不拦截（回归既有行为）；织造单工作台入口同样被硬拦截
- [ ] 新增错误码常量；纯 Mockito 单测（MachineTaskConflictTest）覆盖以上全部场景
