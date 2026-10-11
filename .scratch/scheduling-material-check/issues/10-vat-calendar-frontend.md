# 10: 排缸日历前端——进度反馈页内月历视图

**What to build:** 进度反馈页顶部加"缸卡墙/排缸日历"segmented 切换（不单独开菜单），新增 VatCalendar 组件：el-calendar 月历，日期格内渲染当日缸点（缸号+数量，圆点/边框取状态字典色，COMPLETED 灰化），支持月切换/今天回位/染厂与状态筛选；缸点点击弹改期小弹窗（el-date-picker 选择新排缸日，调**现有**反馈缸更新接口，COMPLETED 缸前端禁用+后端既有状态守卫兜底），成功后局部刷新当月数据；缸卡墙现有交互零改动。

**Blocked by:** 06: 排缸日历后端——月历扁平缸数据

**Status:** resolved

**参考**：本目录上级 spec.md §6.3、§5.2；ADR-0031

- [x] 月切换/今天回位/染厂与状态筛选生效；缸点状态色正确、COMPLETED 灰化
- [x] 点缸改期成功后当月数据局部刷新；COMPLETED 缸禁改（前端禁用+后端守卫兜底）
- [x] 缸卡墙原有交互零回归；ts:check、lint 通过

## Comments
- **排缸日历前端月历视图与改期交互**（Commit: `165eec3d`，Merge: `bf801d25`）：
  - API 封装：在 `src/api/lunaerp-elmui/production/progress-feedback/index.ts` 补充 `getVatCalendar(params: VatCalendarReqVO): Promise<VatCalendarRespVO>` 端点与相关 VO 类型。
  - 核心辅助工具：创建 `src/views/lunaerp-elmui/production/progress-feedback/components/vat-calendar-helper.ts`，封装 `getVatStatusTheme`（对齐 `lunaerp_progress_feedback_status` 字典，COMPLETED 强制灰化）、`groupVatsByDate`（按排缸日期聚合槽位）、`filterVats`（承染厂与状态多维过滤）、`canRescheduleVat`（COMPLETED 前端禁用守卫）、`shiftMonth`、`buildReschedulePayload` 等纯函数。
  - 组件实现：创建 `src/views/lunaerp-elmui/production/progress-feedback/components/VatCalendar.vue`：
    - 基于 `el-calendar` 月历视图，日期格内以缸点/徽章形式渲染当日缸（缸号、数量、匹数），结合字典状态色（已排缸 blue / 工序中 warning / 已完成 grey）；
    - 支持月份切换（上个月/今天/下个月/年月选择器）、承染厂远程搜索下拉、进度状态下拉及即时图例；
    - 点击非 COMPLETED 缸点弹出改期小弹窗（`el-date-picker`），确认后调用现有 `updateProgressFeedback` 接口；COMPLETED 缸前端视觉置灰且完全禁用点击改期；改期成功后局部刷新当月排缸数据。
  - 页面集成：在 `src/views/lunaerp-elmui/production/progress-feedback/index.vue` 顶部引入 `el-segmented` 视图切换（"缸卡墙"（默认）/ "排缸日历"），排缸日历内嵌展示，原缸卡墙统计条、物料单元定位条、筛选栏及卡片工序推进等交互 100% 保持完整未受影响。
  - 单元测试：新增 `src/views/lunaerp-elmui/production/progress-feedback/__tests__/vat-calendar.spec.ts`，19 个单元测试完整覆盖 API 调用、月历分组、承染厂与状态过滤、字典状态色与 COMPLETED 灰化样式、COMPLETED 禁用守卫及改期成功后局部刷新流程，测试全部通过；ESLint 与 Prettier 格式校验全部通过。
