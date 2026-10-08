# 06: H5Layout + 路由守卫 + 登录页 + 403 页

**What to build:** 落成 luna-ui 内 `/h5/**` 独立移动路由树与 `H5Layout`（Vant Tabbar 布局）骨架。`src/views/h5/layout/H5Layout.vue`（Vant Tabbar，按角色显隐 报工/质检/领纱/我的 四 Tab，不嵌 PC 菜单框架，路由全部懒加载）；`src/views/h5/login.vue`（账号密码登录，带 redirect 回跳，复用 `POST /admin-api/system/auth/login` 员工 token，成功后 accessToken/refreshToken 存主站既有 localStorage 键，H5 与 PC 同源共享登录态，401 走既有 refresh-token 轮换失败清态）；`src/views/h5/403.vue`（无车间角色提示联系管理员开通）；`src/permission.ts` 改动：`/h5/login` 入白名单；`/h5/**` 无 token → `/h5/login?redirect=...`（不跳 PC `/login`）；有 token 但三个权限码全无 → `/h5/403`；Tab 按 `get-permission-info` 权限位显隐，直接输 URL 同样做按钮级守卫；axios 拦截器 H5 自定义响应分支：401 → `/h5/login?redirect=`（不跳 PC `/login`）。可 demo：登录→看到 Tab 框架；无角色进 403；token 过期自动 refresh；401 回 H5 登录页。

**Blocked by:** 02（前端字典常量与 package.json/vite 依赖配置已落）, 05（菜单/角色 seed 已落用于路由守卫权限码判空）

**Status:** ready-for-agent

- [ ] `src/views/h5/layout/H5Layout.vue`（Vant Tabbar 布局，按角色显隐 报工/质检/领纱/我的 四 Tab，不嵌 PC 菜单框架，路由全部懒加载）
- [ ] `src/views/h5/login.vue`（账号密码登录，带 redirect 回跳，复用 `/admin-api/system/auth/login` 员工 token，token 存主站既有 localStorage 键，H5 与 PC 同源共享登录态）
- [ ] `src/views/h5/403.vue`（无车间角色提示联系管理员开通）
- [ ] `src/permission.ts` 改动：`/h5/login` 入白名单；`/h5/**` 无 token → `/h5/login?redirect=...`（不跳 PC `/login`）；有 token 但三权限码全无 → `/h5/403`；Tab 按 `get-permission-info` 显隐，直接输 URL 同样做按钮级守卫
- [ ] axios 拦截器 H5 自定义响应分支：401 → `/h5/login?redirect=`（不跳 PC `/login`）；401 走既有 refresh-token 轮换失败清态
- [ ] 路由全部懒加载（vant/html5-qrcode 单独 split chunk 已由 02 配置）
- [ ] 全局只在 `/h5` 子树引入 Vant 样式与移动 rem/viewport 约定（不引入 postcss-px-to-viewport）
- [ ] 移动子树禁止引用 `lunaerp-elmui/**` PC 组件与 ElementPlus 业务组件
- [ ] 企微/钉钉/普通手机浏览器一期均为"内置浏览器打开 H5 地址"，不做 corp OAuth 免登、JS-SDK 鉴权、分享配置
- [ ] demo：登录→看到 Tab 框架；无角色进 403；token 过期自动 refresh；401 回 H5 登录页
