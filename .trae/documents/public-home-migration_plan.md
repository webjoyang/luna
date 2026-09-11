# PublicHome 官网首页从 React 迁移到 Vue3（已实施完成）

> 实施日期：2026-09-11
> 命名：最终采用 **PublicHome** 命名（开发中先暂用 Storefront，后统一改为 Public/Home 风格，与后台 Home 明确区分）

## 仓库研究结论

### oldweb（源项目）
- **技术栈**: Next.js 14 + React + TypeScript + Tailwind CSS + framer-motion
- **首页结构**: `app/page.tsx` → `StorefrontHomePage` → 4 个组件
  - `StorefrontNav.tsx`: 吸顶毛玻璃导航，左 logo、中锚点菜单、右员工登录
  - `StorefrontNarrative.tsx`: 滚动叙事区（首屏大字模糊浮现 + 大图滚动放大铺满 + 产品系列屏 × N + 黑底 CTA）
  - `StorefrontLibrary.tsx`: 面料库区（标题 + 搜索 + 分类 chips + 产品网格 + 加载更多）
  - `StorefrontFooter.tsx`: 页脚
- **品牌配置**: `lib/brand-config.ts` — `BrandConfig` 接口 + `BRAND_PRESETS` 预设表 + 环境变量覆盖
- **数据 API**: 从 `/api/public/products` 和 `/api/public/categories` 获取公开产品
- **动画**: 依赖 `framer-motion` 的 `useScroll`, `useTransform`, `motion` 组件

### luna-ui（目标项目）
- **技术栈**: Vue 3.5 + Vite 8 + Element Plus 2.13 + TypeScript + UnoCSS（Tailwind 兼容）+ Pinia
- **原首页**: `views/Home/Index.vue` — 后台管理风格 Dashboard
- **路由结构**（实施前）:
  - `/` → Layout（侧边栏+头部）→ redirect 到 `/index`（后台首页）
- **路由结构**（实施后）:
  - `/` → `PublicHome.vue`（**无 Layout**，公开页面，`meta.noAuth: true`）
  - `/index` → Layout → `Home/Index.vue`（后台首页，需登录）
- **API 请求**: axios，base_url = `VITE_BASE_URL + VITE_API_URL`，`request.get({ url, params, headers: { isToken: false } })`

## 最终文件清单

### 新增文件（已创建）

| 文件 | 说明 |
|------|------|
| `src/views/PublicHome/PublicHome.vue` | 官网首页主容器（Nav + Narrative + Library + Footer） |
| `src/views/PublicHome/PublicNav.vue` | 吸顶毛玻璃导航 |
| `src/views/PublicHome/PublicNarrative.vue` | 滚动叙事区（含 requestAnimationFrame 驱动的大图放大铺满动画） |
| `src/views/PublicHome/PublicLibrary.vue` | 面料库区（搜索 debounce + 分类 chips + 产品网格 + 加载更多） |
| `src/views/PublicHome/PublicFooter.vue` | 页脚 |
| `src/views/PublicHome/SeriesPanel.vue` | 单个产品系列屏（视差 + IntersectionObserver 进场） |
| `src/config/brand.ts` | 品牌配置（port 自 oldweb，`NEXT_PUBLIC_*` → `VITE_*`） |
| `src/api/public/index.ts` | 公开产品 API 封装（带 `headers: { isToken: false }`） |

### 修改文件

| 文件 | 变更 |
|------|------|
| `src/router/modules/remaining.ts` | `/` 路由改为 PublicHome（无 Layout，`meta.noAuth: true`）；后台首页从嵌套改为 `/index` Layout 路由 |
| `src/router/index.ts` | `resetWhiteNameList` 加入 `'PublicHome'` 和 `'Index'` |
| `src/permission.ts` | 路由守卫新增 `meta.noAuth === true` 跳过登录校验；登录后 redirect 目标改为 `/index` |

### 未创建的文件（实际不需要）

| 计划里的文件 | 为什么没建 |
|---|---|
| `src/composables/useScrollTransform.ts` | 大图放大动画直接写在组件内（`requestAnimationFrame` loop + `getBoundingClientRect`），独立 composable 增加间接层反而降低可读性。如需在多处复用再抽 |

## 关键技术决策（计划 vs 实施的差异）

| 计划方案 | 实施方案 | 原因 |
|---|---|---|
| `composables/useScrollTransform.ts` 抽出来 | 动画逻辑留在 `PublicNarrative.vue` 内 | 仅一处使用，组件内 `requestAnimationFrame` loop + `getBoundingClientRect` 更直观 |
| API 层 `import request from '@/config/axios/service'` 并手动解构 `res.data` | 用项目统一的 `import request from '@/config/axios'` | 该封装已自动返回 `res.data`，只需加 `headers: { isToken: false }` |
| 进场动画用 `@vueuse/motion` | 纯 CSS `@keyframes` + scoped style | 零依赖，动画都在组件内可见可控 |
| Next.js `Link` 改 `router-link` | 全部用原生 `<a href="#...">` | 官网是单页全锚点滚动，不需要 SPA 路由跳转 |
| `resetRouter` 白名单改 `'Home'` → `'PublicHome'` | 同时加入 `'PublicHome'` 和 `'Index'` | 登出后不能让 resetRouter 删掉公开首页路由，也不能删掉新命名的后台首页路由 |

## 依赖与注意事项

- **动画方案**: 进场用 CSS `@keyframes`（`riseBlur`, `fadeUp` 等），滚动联动（大图放大、视差）用 `requestAnimationFrame` + `getBoundingClientRect`，仅操作 `transform / opacity / borderRadius` 以保持流畅
- **API 返回格式**: `src/config/axios` 已自动解包 `res.data`，直接拿到后端业务数据
- **公开 API 无 token**: 所有 `/public/*` 接口请求加 `headers: { isToken: false }`，避免 401
- **品牌 Logo**: 当前用 `/brand_logo_black.svg`（public 目录已存在），多租户通过 `VITE_BRAND_CODE` 环境变量切换
- **路由守卫**: 官网路由 `meta: { noAuth: true }` 跳过 permission.ts 校验；后台路由正常走登录流程

## 验证结果

- `GetDiagnostics` 全部文件零 TypeScript 错误 ✅
- `grep 'Storefront|storefront' src/` 零残留 ✅
- 路由 name 统一为 `PublicHome`，resetRouter 白名单同步 ✅

## 遗留待办

- [ ] API 接口 `/public/products` / `/public/categories` 需后端实现（当前前端会降级显示品牌介绍）
- [ ] Vite dev proxy 如需调用本地后端，需在 `vite.config.ts` 配置 `/public` 路径代理
