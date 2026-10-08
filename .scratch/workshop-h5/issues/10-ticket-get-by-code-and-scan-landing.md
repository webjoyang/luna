# 10: ticket/get-by-code + 扫码落点分流页

**What to build:** 落成扫票定位端点与扫码落点分流页。GET `/lunaerp/h5/workshop/ticket/get-by-code`（code；扫码/手输定位；返回布票卡（票号/织造单/机台/品名颜色/状态/任务）+ `claimable` 标志；范围：票非 VOID，任务 worker 为空或=登录人，否则 039-002；票不存在 039-001；`@PreAuthorize lunaerp:workshop:h5:report`）。前端 `src/views/h5/scan/weaving-ticket.vue` 扫码落点分流（冻结二维码落点 `/h5/scan/weaving-ticket?code=`，登录后按角色分流：仅 report → 票卡；仅 inspection → 走 ticket-scan 进胚布录检；两者都有 → ActionSheet 选择并记住上次选择 localStorage，可在"我的"里重置）。可 demo：扫票/手输码定位→看到布票卡占位（开工/完工/异常按钮占位）。

**Blocked by:** 07（扫码组件已就位用于扫票输入）, 09（后端 H5 包骨架与报工 Tab 已就位）

**Status:** ready-for-agent

- [ ] GET `/lunaerp/h5/workshop/ticket/get-by-code`（code；返回布票卡 + `claimable` 标志）
- [ ] 票不存在/VOID → 039-001；任务 worker 为他人 → 039-002；任务 worker 为空或=登录人 → `claimable=true`
- [ ] `@PreAuthorize lunaerp:workshop:h5:report`
- [ ] 前端 `src/views/h5/scan/weaving-ticket.vue`（扫码落点分流）
- [ ] 冻结二维码落点 `/h5/scan/weaving-ticket?code=`，登录后按角色分流：仅 report → `/h5/ticket`；仅 inspection → 走 ticket-scan 进胚布录检；两者都有 → ActionSheet 选择并记 localStorage 上次选择
- [ ] "我的"里可重置扫码落点上次选择
- [ ] demo：扫票/手输码定位→看到布票卡占位（开工/完工/异常按钮占位）
