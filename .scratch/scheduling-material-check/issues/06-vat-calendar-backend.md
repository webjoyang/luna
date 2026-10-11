# 06: 排缸日历后端——月历扁平缸数据

**What to build:** 进度反馈新增只读 calendar 端点：GET /progress-feedback/calendar（权限复用 `lunaerp:production:progress-feedback:query`），入参 month（或 from/to 一对日期，二选一）、可选 dyeingFactoryId/status，返回区间内**扁平缸列表**（非缸卡墙的颜色行骨架结构）：缸号/排缸日/状态/数量/件数/单位 + 客户色号/色名/合同号/染厂快照（展示字段实时 join 染色单与颜色行，零展示快照原则不变）。口径：排缸日落在区间内即返回（含 COMPLETED 用于回看，status 筛选可过滤）；仅 ACTIVE/COMPLETED 染色单（沿用进度反馈既有骨架口径），CANCELLED 排除。新增 Service 只读方法 + Mapper 查询，不改表不加列。

**Blocked by:** None (can start immediately)

**Status:** resolved

**参考**：本目录上级 spec.md §5.1；ADR-0031（进度反馈覆盖层）

- [x] month 区间只返回排缸日落月内的缸；status/dyeingFactoryId 筛选生效
- [x] COMPLETED 缸默认返回可回看且可被筛选；CANCELLED 染色单的缸不返回
- [x] 快照字段 join 正确（客户色号/色名/合同号/染厂名）
- [x] 纯 Mockito 单测（ProgressFeedbackCalendarTest）覆盖以上场景

## Comments
- **排缸日历后端扁平缸数据端点**（Commit: `b12ca6d5`，Merge: `de933519`）：
  - 新增 Controller 只读端点：`GET /lunaerp/production/progress-feedback/calendar`，权限复用 `@ss.hasPermission('lunaerp:production:progress-feedback:query')`。
  - 新增 VO：
    - `ProgressFeedbackCalendarReqVO`：支持 `month`（yyyy-MM）或 `from`/`to` 区间参数，可选 `dyeingFactoryId` 与 `status` 筛选。
    - `ProgressFeedbackCalendarVatVO`：扁平缸结构，包含缸字段（feedbackId, dyeingOrderId, dyeingColorId, vatNo, scheduledDate, status, quantity, pieces）与实时快照字段（unit, customerColorNo, colorName, contractNo, dyeingFactoryId, dyeingFactoryName）。
    - `ProgressFeedbackCalendarRespVO`：包含 `List<ProgressFeedbackCalendarVatVO> vats`。
  - 新增 Mapper 方法：
    - `ProgressFeedbackMapper#selectCalendarList(startDate, endDate, status)`：按排缸日期区间与可选状态查询反馈缸。
    - `DyeingOrderMapper#selectListByIds(ids)`：按染色单 ID 批量查询染色单。
    - `DyeingColorMapper#selectListByIds(ids)`：按明细 ID 批量查询颜色明细。
  - 在 `ProgressFeedbackServiceImpl#getCalendar` 实现核心口径与内存防御过滤：
    1. 日期解析：支持 `from/to`、`month`、单端日期，缺省当前月；日期反转返回空列表，非法月份格式抛出友好参数异常。
    2. 反馈缸查询与内存防御：过滤软删除行，仅保留排缸日落在区间内且匹配可选状态的缸；COMPLETED 缸默认返回支持历史回看且可被 status 显式筛选。
    3. 关联单据批量查询（零展示快照原则）：批量查询染色单主表与颜色明细行；仅保留 ACTIVE 与 COMPLETED 染色单，CANCELLED/DRAFT/软删染色单的缸一律排除；支持 dyeingFactoryId 承染厂过滤。
    4. 结果装配与排序：按 scheduledDate 升序、vatNo 升序、feedbackId 升序排列。
  - 新增纯 Mockito 单元测试 `ProgressFeedbackCalendarTest`，9 个用例完整覆盖 Testing Decisions 16 及 Issue 06 要求（月度区间/自定义区间/染厂过滤/状态过滤/COMPLETED 回看与过滤/ACTIVE与COMPLETED纳入/CANCELLED与DRAFT排除/快照字段装配/软删排除/异常参数防御），测试全部通过。
