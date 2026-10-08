# 02: PortalTokenFilter 校验链 + 门户前端骨架（免登路由区/布局/invalid 页）

**What to build:** 门户 walking skeleton。客户拿 01 号生成的链接打开 `/portal/shipments?t=xx`，进入门户框架（空列表占位即可）；吊销/伪造/过期→「链接已失效」页；限流 429。后端落 `PortalTokenFilter`（校验/续期/上下文注入/清理）+ `PortalRateLimiter`，前端落 `/portal/*` 免登路由区 + `PortalLayout` + 独立 axios 实例 + `?t=` 引导。可 demo：带 token 进门户看到框架与客户名；管理端吊销后刷新→invalid 页。

**Blocked by:** 01（管理端门户链接签发闭环——需要真实 token 驱动校验链）

**Status:** ready-for-agent

- [ ] `PortalTokenFilter` 仅拦 `/app-api/lunaerp/portal/**`：解析 header `X-Portal-Token` → GET 主键；未命中（无效/过期/吊销/伪造）统一 401 + 038-000 不区分文案；命中则双键 EXPIRE 续期 → 设置 TenantContextHolder（租户插件生效）+ PortalContext（ThreadLocal：tenantId/customerId）→ 请求结束 finally 清理
- [ ] 失效兜底：PortalContext 校验客户存在且未删除（deleted=0），否则按 038-001 拒绝（客户进回收站后门户不可见，不新增 RecycleBinHandler）
- [ ] `PortalRateLimiter`：`luna_portal:rl:{token}` Redis 固定窗口 60s 计数，>60 返回 429 + 038-002（仅对有效 token 限流，不做 IP 限流/验证码）
- [ ] 前端路由：constantRoutes 静态注册（不挂菜单）`/portal/shipments`、`/portal/shipment/:id`、`/portal/statements`、`/portal/statement/:id`、`/portal/invalid`，全部懒加载；`permission.ts` whiteList 增加 `'/portal'` 一条（path 前缀 startsWith 覆盖全部子路径）
- [ ] 独立 axios 实例（`src/config/axios/` 下 portal 专属）：baseURL 同 `/app-api`、header `X-Portal-Token`、不引 user store、不带 Authorization；响应 401 → 跳 `/portal/invalid`（文案：链接已失效，请联系业务员重新获取）；429 → toast 稍后再试
- [ ] `PortalLayout`：移动优先，页面容器 max-width 480px 居中兜底 PC 扫码；顶部标题栏（门户名+客户名）+ 底部 Tab（发货单/对账单）；ElementPlus 现有组件 + 自定义 CSS，零新依赖（不引 vant、不做 px-to-viewport、主站不做响应式）
- [ ] token 引导：`?t=` 进入列表页时存 sessionStorage → `history.replaceState` 清地址栏 → 进列表；直接打开无 token 的门户路由 → invalid 页引导先从业务员链接进入
- [ ] `src/views/portal/`（正式目录，非 elmui 临时区）+ `src/api/portal/` 落位；本票交付 `invalid.vue` 与列表占位页，业务页面由 03/05 填充
- [ ] 专测：有效请求前后主键 TTL 变化（续期）；61 次/分钟→429、窗口重置恢复；伪造/未带 header/过期/吊销统一 401-000；并发请求 ThreadLocal 不串数据（请求结束清理）
