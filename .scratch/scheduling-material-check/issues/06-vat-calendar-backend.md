# 06: 排缸日历后端——月历扁平缸数据

**What to build:** 进度反馈新增只读 calendar 端点：GET /progress-feedback/calendar（权限复用 `lunaerp:production:progress-feedback:query`），入参 month（或 from/to 一对日期，二选一）、可选 dyeingFactoryId/status，返回区间内**扁平缸列表**（非缸卡墙的颜色行骨架结构）：缸号/排缸日/状态/数量/件数/单位 + 客户色号/色名/合同号/染厂快照（展示字段实时 join 染色单与颜色行，零展示快照原则不变）。口径：排缸日落在区间内即返回（含 COMPLETED 用于回看，status 筛选可过滤）；仅 ACTIVE/COMPLETED 染色单（沿用进度反馈既有骨架口径），CANCELLED 排除。新增 Service 只读方法 + Mapper 查询，不改表不加列。

**Blocked by:** None (can start immediately)

**Status:** closed

**参考**：本目录上级 spec.md §5.1；ADR-0031（进度反馈覆盖层）

- [x] month 区间只返回排缸日落月内的缸；status/dyeingFactoryId 筛选生效
- [x] COMPLETED 缸默认返回可回看且可被筛选；CANCELLED 染色单的缸不返回
- [x] 快照字段 join 正确（客户色号/色名/合同号/染厂名）
- [x] 纯 Mockito 单测（ProgressFeedbackCalendarTest）覆盖以上场景
