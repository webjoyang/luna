# 07: 排产中心前端——页面骨架 + 甘特区 + 待排池

**What to build:** 10510 排产中心页面上线（视图 scheduling/index.vue）：顶部统一筛选（日期窗口/织机/状态）与日/周粒度切换；**甘特区**按 03 spike 结论落地——行=ACTIVE 织机（机台号+型号）、条=机台任务（条内织造单号尾号+计划匹数，条色按任务状态对齐字典，conflict=true 红描边+斜纹）；拖动平移起止/右边缘拉伸结束日走乐观更新（先动条带→调现有 update 接口→失败回滚条带位置并弹冲突错误含任务号）；COMPLETED 条带禁一切拖动、HALTED 禁拖提示先恢复、无日期任务不渲染仅进待排池；条带点击侧拉抽屉（任务详情+跳转织造单工作台）；跨行拖拽不可靠则按 spike 定案弹窗织机下拉转机；**待排池区**（甘特上方可折叠横条或左侧栏）两组展示（未排日期任务/未排满织造单），行内"派工"按钮弹派工弹窗（织造单远程搜索仅 ACTIVE 未取消、组二行带入单 ID 与默认匹数=remainingQty、织机仅 ACTIVE），后端 000 冲突保持弹窗打开红字展示冲突任务号，派工成功刷新甘特与池；新增 scheduling API 目录并在现有 machine-task API 扩展参数。

**Blocked by:** 01: SQL 基建——10510/10520 菜单与角色授权；03: vue-ganttastic 选型 spike；04: 排产中心后端——甘特/待排池/跨单分页

**Status:** closed

**参考**：本目录上级 spec.md §6.1、§3.4

- [x] 甘特按织机分行渲染任务条，状态色正确，存量冲突条红描边+斜纹
- [x] 拖动改期/拉伸成功乐观更新；制造冲突后拖动被回滚并弹冲突任务号
- [x] COMPLETED 不可拖；HALTED 不可拖并提示恢复；无日期任务只出现在待排池
- [x] 待排池派工：默认匹数=剩余未排量；同织机同日双占报 000 且弹窗保持；超量仍只警告可保存
- [x] 日/周粒度切换、织机/状态筛选生效；派工成功后甘特与池刷新；条带抽屉跳转织造单工作台正确

---

## Comments

### 实现记录与测试交付
1. **API 模块**：
   - 新增 `luna-ui/src/api/lunaerp-elmui/production/scheduling/index.ts`（甘特与待排池接口及对应 VO）。
   - 扩展 `luna-ui/src/api/lunaerp-elmui/production/machine-task/index.ts`（跨单分页入参 `conflictOnly` 与 VO 快照字段）。
   - 强化 `luna-ui/src/config/axios/service.ts` 错误对象保留 code 与 msg，确保 000 冲突拦截能精准回显红字。
2. **状态与适配核心（TDD 驱动）**：
   - `model/gantt-adapter.ts`：纯状态组装 `buildGanttRows`、条带单号截取 `formatBarLabel` 与乐观更新及冲突回滚 `optimisticBarUpdate`。
   - `model/pending-pool.ts`：待排池组一与组二派工预填、表单校验与 000 冲突错误隔离 retention。
   - 单测套件：`gantt-adapter.spec.ts`（7 项）与 `pending-pool.spec.ts`（8 项），全量覆盖通过。
3. **UI 组件架构**：
   - `components/PendingPoolSection.vue`：折叠横向看板，双分组（未排满织造单带剩余匹数、未排期任务带织机），一键发起派工。
   - `components/DispatchDialog.vue`：派工弹窗，支持远程搜索、织机/挡车工下拉、排期与匹数输入；拦截 000 冲突时保持弹窗打开并显式红字展示冲突任务号；支持超量软警告。
   - `components/TaskDetailDrawer.vue`：条带侧拉抽屉，展示完整快照、跳转织造单工作台（`/production/weaving-order/workspace/:id`），并按 spike 定案落地下拉转机。
   - `components/TaskListPlaceholder.vue`：为 Issue 08 跨单任务列表 Tab 预留骨架。
   - `index.vue`：排产中心页面骨架，统一顶部筛选（≤92 天窗口校验、织机与四态多选）、日/周粒度切换、甘特渲染与拖拽改期乐观更新。
