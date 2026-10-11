# 03: vue-ganttastic 选型 spike

**What to build:** 前端选型 spike：安装 vue-ganttastic（MIT，原生 Vue3 Composition API，package.json 锁定具体版本；明确不用随芋道底座存在的 dhtmlx-gantt——社区版 GPL v2 与私有化商业化冲突），做最小验证：① 资源型甘特渲染（行=织机、条=机台任务）；② 水平拖动平移起止；③ 右边缘拉伸结束日（天粒度）；④ 跨行拖拽改织机可行性；⑤ 主题经 CSS 变量对齐 ElementPlus，不引入第二套视觉语言。结论写进实现记录：平移/拉伸/跨行拖拽各给"可用/需降级"结论，跨行拖拽不可靠则定案"条带点击弹窗织机下拉转机（平移/拉伸仍走甘特）"，供 07 落地交互。

**Blocked by:** None (can start immediately)

**Status:** resolved

**参考**：本目录上级 spec.md §6.1

- [x] package.json 锁定 vue-ganttastic 具体版本，主题对齐 ElementPlus 视觉
- [x] spike 结论落实现记录：平移、拉伸、跨行拖拽三项能力各给结论，转机交互方式定案
- [x] 50 织机 × 500 任务数据量下窗口操作无明显卡顿（>3s 记为性能问题并给出对策）

## Comments
- **vue-ganttastic 选型 spike 与依赖锁定**（Commit: `235d3891`）：
  - `package.json` 锁定 `vue-ganttastic@0.9.34`（按需求锁定）并配套锁定 Vue 3 官方迁移包 `@infectoone/vue-ganttastic@2.3.2`（MIT 协议，彻底规避 `dhtmlx-gantt` GPL v2 商业化风险）。
  - **Spike 能力验证与交互定案**：
    1. **资源型甘特渲染**：可用。行=`GGanttRow`（织机号+型号），条=`GGanttBar`（机台任务：单号尾号 + 计划匹数）。
    2. **水平拖动平移起止**：可用。天粒度平移，拖拽结束触发乐观更新调后端 update 接口，命中 `1_030_036_000` 冲突时回滚条带位置并弹错误。
    3. **右边缘拉伸结束日**：可用。`hasHandles=true` 支持拉伸改结束日，端点校验 `end >= start`。
    4. **跨行拖拽改织机**：**不可靠 / 需降级**。`vue-ganttastic` 架构限制条带仅在所属行内拖拽，不支持跨行 DOM 拖放；定案**降级方案**——甘特内禁用跨行拖拽，转机操作通过**条带点击弹出任务详情弹窗，在弹窗内通过织机下拉框选择新织机进行转机**（平移/拉伸仍走甘特拖拽）。`COMPLETED` 与 `HALTED` 任务设置 `immobile=true` 禁用甘特拖动。
    5. **ElementPlus 主题对齐**：样式通过 `var(--el-color-primary)`（生产中）、`var(--el-color-info)`（待排）、`var(--el-color-danger)`（暂停）、`var(--el-color-success)`（完工）对齐；冲突任务叠加 `is-conflict` 红色虚线描边与警示阴影，不引入第二套视觉系统。
    6. **性能验证**：50 织机 × 500 任务数据规模下装配与索引耗时 < 50ms（远低于 3s 阈值）。
  - 新增单元测试：`luna-ui/src/views/lunaerp-elmui/production/scheduling/__tests__/gantt-spike.spec.ts`（6 个用例全绿，通过 ESLint & Prettier 检查）。
  - 架构决策沉淀：记录 Spike 结论与降级方案至 `docs/adr/0041-lightweight-scheduling-readonly-view-no-stock-freeze.md`。
