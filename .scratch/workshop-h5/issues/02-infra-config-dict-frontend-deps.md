# 02: infra_config seed + 前端字典常量 + package.json/vite 依赖配置

**What to build:** 落盘 H5 工程地基的小配置项。`infra_config` seed 键 `luna.workshop.default-warehouse-id`（空值占位，租户级参数，系统管理→参数设置维护，零新页面零新表）；前端 `luna-ui/src/utils/dict.ts` 加既有字典的 H5 引用常量（异常类型 2244、检验等级 2232、布票/任务/领纱/检验单状态字典，零新增字典，仅暴露 H5 子树需要的常量名）；`package.json` 加 `vant@^4` + `html5-qrcode@^2`（MIT）；`vite.config.ts` 加 `manualChunks`（vant、html5-qrcode 独立 split chunk，PC 首屏零增量）+ `unplugin-vue-components` 的 VantResolver 按需引入。本票不交付用户行为，是 06 工程脚手架与 19 默认仓库消费的前置依赖。

**Blocked by:** 01（错误码与术语已落，避免重复横切改动）

**Status:** closed

- [x] `infra_config` seed 键 `luna.workshop.default-warehouse-id`（空值占位，租户级参数），脚本可重复执行
- [x] `luna-ui/src/utils/dict.ts` 加既有字典的 H5 引用常量：异常类型 2244、检验等级 2232、布票状态、机台任务状态、领纱单状态、检验单状态（零新增字典，仅常量名暴露）
- [x] `luna-ui/package.json` 加 `vant@^4` + `html5-qrcode@^2`
- [x] `luna-ui/vite.config.ts` 加 `manualChunks`（vant、html5-qrcode 独立 split chunk）+ `unplugin-vue-components` VantResolver
- [x] 全局只在 `/h5` 子树引入 Vant 样式与移动 rem/viewport 约定（不引入 postcss-px-to-viewport，与门户 max-width 方案并存不冲突）
- [x] 移动子树禁止引用 `lunaerp-elmui/**` PC 组件与 ElementPlus 业务组件（van-/el- 前缀不冲突，但隔离依赖方向）
