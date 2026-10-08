# 03: vue-ganttastic 选型 spike

**What to build:** 前端选型 spike：安装 vue-ganttastic（MIT，原生 Vue3 Composition API，package.json 锁定具体版本；明确不用随芋道底座存在的 dhtmlx-gantt——社区版 GPL v2 与私有化商业化冲突），做最小验证：① 资源型甘特渲染（行=织机、条=机台任务）；② 水平拖动平移起止；③ 右边缘拉伸结束日（天粒度）；④ 跨行拖拽改织机可行性；⑤ 主题经 CSS 变量对齐 ElementPlus，不引入第二套视觉语言。结论写进实现记录：平移/拉伸/跨行拖拽各给"可用/需降级"结论，跨行拖拽不可靠则定案"条带点击弹窗织机下拉转机（平移/拉伸仍走甘特）"，供 07 落地交互。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

**参考**：本目录上级 spec.md §6.1

- [ ] package.json 锁定 vue-ganttastic 具体版本，主题对齐 ElementPlus 视觉
- [ ] spike 结论落实现记录：平移、拉伸、跨行拖拽三项能力各给结论，转机交互方式定案
- [ ] 50 织机 × 500 任务数据量下窗口操作无明显卡顿（>3s 记为性能问题并给出对策）
