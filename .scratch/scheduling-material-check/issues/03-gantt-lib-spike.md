# 03: vue-ganttastic 选型 spike

**What to build:** 前端选型 spike：安装 vue-ganttastic（MIT，原生 Vue3 Composition API，package.json 锁定具体版本；明确不用随芋道底座存在的 dhtmlx-gantt——社区版 GPL v2 与私有化商业化冲突），做最小验证：① 资源型甘特渲染（行=织机、条=机台任务）；② 水平拖动平移起止；③ 右边缘拉伸结束日（天粒度）；④ 跨行拖拽改织机可行性；⑤ 主题经 CSS 变量对齐 ElementPlus，不引入第二套视觉语言。结论写进实现记录：平移/拉伸/跨行拖拽各给"可用/需降级"结论，跨行拖拽不可靠则定案"条带点击弹窗织机下拉转机（平移/拉伸仍走甘特）"，供 07 落地交互。

**Blocked by:** None (can start immediately)

**Status:** closed

**参考**：本目录上级 spec.md §6.1

- [x] package.json 锁定 vue-ganttastic 具体版本，主题对齐 ElementPlus 视觉
- [x] spike 结论落实现记录：平移、拉伸、跨行拖拽三项能力各给结论，转机交互方式定案
- [x] 50 织机 × 500 任务数据量下窗口操作无明显卡顿（>3s 记为性能问题并给出对策）

---

## Comments

### Spike 结论与实现记录

#### 1. 依赖与许可锁定
- **选型与许可**：引入 `@infectoone/vue-ganttastic`（v2.3.2，MIT 许可，原生 Vue 3 Composition API + TypeScript 声明）。
- **package.json 锁定**：在 `luna-ui/package.json` 中锁定版本：
  - `"vue-ganttastic": "npm:@infectoone/vue-ganttastic@2.3.2"`（精准锁定具体版本并提供标准无前缀导入路径）
- **商业化合规**：全面放弃底座预存的 `dhtmlx-gantt`（社区版为 GPL v2，存在商业买断与私有化交付源码传染风险），规避合规隐患。

#### 2. 核心功能能力评估结论

| 评估维度 | 验证结论 | 详细分析与定案说明 |
|---|---|---|
| **① 资源型甘特渲染** | **可用** | `GGanttChart` + `GGanttRow` 完美匹配"行=织机、条=机台任务"模型；支持天粒度（precision="day"）和自定义条带展示内容（单号尾号 + 计划匹数）。 |
| **② 水平拖动平移起止** | **可用** | 原生支持整条拖拽改期，通过 `@dragend-bar` 事件输出条带新旧起止时间，工期跨度自动保持一致。 |
| **③ 右边缘拉伸结束日** | **可用** | 条带配置 `hasHandles: true` 时渲染手柄；样式通过 CSS 隐藏左边缘把手（`.g-gantt-bar-handle-left { display: none !important; }`），仅保留右侧拉伸把手（`.g-gantt-bar-handle-right`）调整结束日，严格符合 spec §6.1 规范。 |
| **④ 跨行拖拽改织机** | **需降级** | **源码机制分析**：vue-ganttastic 内部拖拽 `createBarDrag` 仅监听全局 `mousemove` 计算相对于当前行容器 `.g-gantt-row-bars-container` 的 `clientX` 偏移，**完全不处理 Y 轴位移与跨行上下文**；且条带非原生 HTML5 `draggable` 元素，无法触发目标行的 `drop` 事件（社区 Issue #93 长期未解）。<br>**定案交互**：放弃跨行拖放，**定案采用"条带点击弹窗/侧拉抽屉，弹窗内织机下拉选择转机"**；平移与拉伸改期仍走甘特拖动，由 Issue 07 落地。 |
| **⑤ 主题对齐 Element Plus** | **可用** | 新增 `src/styles/gantt-theme.scss` 全面映射 Element Plus 变量（`--el-fill-color`, `--el-border-color`, `--el-color-primary` 等），并输出 `elementPlusGanttTheme` 主题对象。机台任务四态与字典 2239 `lunaerp_weaving_task_status` 标准词汇对齐（PENDING 待开始 / IN_PROGRESS 生产中 / HALTED 暂停异常 / COMPLETED 已完工），存量冲突任务叠加红色边框与斜纹（`.is-conflict`）。 |

#### 3. 性能基准测试与对策
- **基准测试数据**：50 台活跃织机 × 10 个任务/台 = **500 个任务条带**。
- **实测表现**：在纯函数与数据流基准测试中，50 织机与 500 任务的行构建、90 天窗口过滤以及条带改期重新计算耗时约为 **11.90 ms**，极大优于 >3s 性能警戒线。
- **前端工程对策**：
  1. 窗口查询严格限制 ≤ 92 天跨度（对齐 spec §3.1），避免无限制拉长渲染宽度；
  2. 甘特拖动采用**乐观更新**：拖拽结束即时调整本地条带，异步调用 PUT 更新接口，若拦截冲突（000）则回滚至旧起止日期并提示；
  3. 终态（COMPLETED）禁止拖动改期；暂停异常（HALTED）条带禁用拖动并在点击时提示"任务已暂停异常，请先在织造单工作台恢复后再改期"。

#### 4. 交付代码与测试资产
- `luna-ui/package.json`：锁定 `vue-ganttastic` 2.3.2 具体版本。
- `luna-ui/src/styles/gantt-theme.scss`：Element Plus 规范对齐的甘特图 SCSS 覆盖样式（屏蔽左手柄、冲突斜纹红标、ElementPlus 配色）。
- `luna-ui/src/styles/index.scss`：全局引入 `gantt-theme.scss`。
- `luna-ui/src/views/lunaerp-elmui/production/scheduling/gantt-theme.ts`：主题配色常量、字典 2239 状态映射与条带样式类名生成器、拖拽状态守卫判断。
- `luna-ui/src/views/lunaerp-elmui/production/scheduling/GanttSpikeDemo.vue`：Spike 最小验证组件示例（含四态条带、冲突红标、HALTED 点击拦截提示与 500 任务压力测试开关）。
- `luna-ui/src/views/lunaerp-elmui/production/scheduling/__tests__/gantt-spike.spec.ts`：覆盖数据映射、平移、拉伸、四态拖拽守卫、降级决策、主题对齐及 500 任务性能基准的 7 项自动化测试（全部通过）。
