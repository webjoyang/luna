# 07: 排产中心前端——页面骨架 + 甘特区 + 待排池

**What to build:** 10510 排产中心页面上线（视图 scheduling/index.vue）：顶部统一筛选（日期窗口/织机/状态）与日/周粒度切换；**甘特区**按 03 spike 结论落地——行=ACTIVE 织机（机台号+型号）、条=机台任务（条内织造单号尾号+计划匹数，条色按任务状态对齐字典，conflict=true 红描边+斜纹）；拖动平移起止/右边缘拉伸结束日走乐观更新（先动条带→调现有 update 接口→失败回滚条带位置并弹冲突错误含任务号）；COMPLETED 条带禁一切拖动、HALTED 禁拖提示先恢复、无日期任务不渲染仅进待排池；条带点击侧拉抽屉（任务详情+跳转织造单工作台）；跨行拖拽不可靠则按 spike 定案弹窗织机下拉转机；**待排池区**（甘特上方可折叠横条或左侧栏）两组展示（未排日期任务/未排满织造单），行内"派工"按钮弹派工弹窗（织造单远程搜索仅 ACTIVE 未取消、组二行带入单 ID 与默认匹数=remainingQty、织机仅 ACTIVE），后端 000 冲突保持弹窗打开红字展示冲突任务号，派工成功刷新甘特与池；新增 scheduling API 目录并在现有 machine-task API 扩展参数。

**Blocked by:** 01: SQL 基建——10510/10520 菜单与角色授权；03: vue-ganttastic 选型 spike；04: 排产中心后端——甘特/待排池/跨单分页

**Status:** resolved

**参考**：本目录上级 spec.md §6.1、§3.4

- [x] 甘特按织机分行渲染任务条，状态色正确，存量冲突条红描边+斜纹
- [x] 拖动改期/拉伸成功乐观更新；制造冲突后拖动被回滚并弹冲突任务号
- [x] COMPLETED 不可拖；HALTED 不可拖并提示恢复；无日期任务只出现在待排池
- [x] 待排池派工：默认匹数=剩余未排量；同织机同日双占报 000 且弹窗保持；超量仍只警告可保存
- [x] 日/周粒度切换、织机/状态筛选生效；派工成功后甘特与池刷新；条带抽屉跳转织造单工作台正确

## Comments
- **排产中心前端页面骨架、甘特区与待排池落地**（Commit: `f9b9dfe`）：
  - 新增 API 模块 `luna-ui/src/api/lunaerp-elmui/production/scheduling/index.ts`，导出 `getGanttData`、`getPendingPool` 及相关 VO 类型；在 `machine-task` API 中补充快照字段与 `conflictOnly` 参数。
  - 新增排产辅助模块 `luna-ui/src/views/lunaerp-elmui/production/scheduling/helper.ts`，实现 ElementPlus 主题变量映射（`resolveBarTheme`）、条带模型转换（`transformToGanttBar`）、织机行装配（`buildGanttRows`）、乐观更新回滚（`rollbackBar`）及默认窗口计算（`getDefaultWindow`）。
  - 新增页面视图 `luna-ui/src/views/lunaerp-elmui/production/scheduling/index.vue`：
    1. 顶部统一筛选：计划起止日期范围、织机多选、状态多选、日/周粒度切换、搜索/重置/新建派工。
    2. 待排池区：可折叠横条展示两组数据，组一（无日期任务）支持行内快捷排期；组二（未排满织造单）支持行内派工并自动带入剩余待排匹数。
    3. 甘特区：集成 `@infectoone/vue-ganttastic`，行=织机，条=机台任务，状态色与字典对齐；冲突条带呈现红色描边与警示图标；支持拖拽平移与右边缘拉伸改期，乐观更新并在冲突时回滚条带位置并报错；COMPLETED 与 HALTED 任务设置 immobile 禁用拖动。
    4. 侧拉抽屉与转机弹窗：条带点击拉出任务详情抽屉，支持跳转织造单工作台；按 Issue 03 spike 定案落地弹窗转机交互，选择目标织机后调接口更换。
    5. 派工弹窗：支持新建派工与从待排池带入，命中后端 `1_030_036_000` 冲突时保持弹窗打开并红字回显冲突任务信息。
  - 单元测试：新增 `scheduling-gantt.spec.ts`（5 个用例全部通过），结合 `gantt-spike.spec.ts` 共 11 个用例全绿；通过 eslint 检查。
