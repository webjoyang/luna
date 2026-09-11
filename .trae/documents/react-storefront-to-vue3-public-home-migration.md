# React 首页迁移至 Vue3（PublicHome 公开官网首页）实施计划

## 概述（Summary）

将 `oldweb`（Next.js + React）的 storefront 官网首页完整迁移到 `luna-ui`（Vue 3.5 + Vite + ElementPlus + UnoCSS + TS）项目中，作为**免登录的公开官网首页**。用户访问根路径 `/` 先看到该页面；原后台工作台移至 `/index` 保持不变。

- 参考效果：https://www.lunaagi.cn/#top（即 oldweb 部署版，源码即迁移源）
- 滚动动画：使用 **motion-v**（Motion for Vue，官方 Vue3 版）替代 framer-motion，API 一一对应
- 数据：内置静态示例产品数据（原 `/api/public/products`、`/api/public/categories` 由静态数据替代，代码中保留 TODO 标记后续接入）
- 品牌：保留多租户品牌系统（`useBrand` composable，localStorage 缓存 + `/api/tenant/brand` 拉取失败回退默认 lunaagi 预设，支持 `?tenant=` 参数）

## 现状分析（Current State）

### 源（oldweb React）结构
- `oldweb/app/page.tsx` — `StorefrontHomePage` = Nav + Narrative + Library + Footer
- `oldweb/app/components/storefront/StorefrontNav.tsx` — 吸顶毛玻璃导航（原生 scroll 事件，logo 失败回退英文名，锚点菜单，员工登录）
- `oldweb/app/components/storefront/StorefrontNarrative.tsx` — 核心滚动叙事：
  - 右侧固定进度点（IntersectionObserver 跟踪 section）
  - 首屏：大字模糊浮现（`riseBlur`: opacity/y/filter blur + delay）
  - 大图滚动放大铺满：`useScroll({ target, offset: ['start start','end end'] })` + `useTransform` 驱动 width 58%→100%、height 74vh→100vh、radius 40→0、遮罩 opacity/y
  - 系列屏 `SeriesPanel`：左文右图奇偶翻转、`whileInView` 淡入、图片视差（`useTransform(scrollYProgress,[0,1],['-7%','7%'])`）
  - 黑底收尾 CTA：`whileInView` 标题/副文案/按钮，mailto 链接
- `oldweb/app/components/storefront/StorefrontLibrary.tsx` — 面料库：防抖搜索(350ms)、分类 chips、产品网格（2/3/4 列）、加载更多分页、空态品牌视觉展示
- `oldweb/app/components/storefront/StorefrontFooter.tsx` — 页脚（品牌全称/版权/备案链接）
- `oldweb/app/components/storefront/shared.ts` — `PublicProduct` 类型、`resolvePublicAssetUrl`、`useTenantPreviewQuery`
- `oldweb/hooks/useBrand.ts` + `oldweb/lib/brand-config.ts` — 品牌类型/预设表/多租户 hook
- 样式：Tailwind（大量 arbitrary values 与 `min-[961px]:`/`max-[960px]:` 变体）
- 静态资源：`oldweb/public/presentation/assets/fabric.png`（兜底面料图）、`oldweb/public/logo.svg`、`logo_white.svg`（lunaagi logo）

### 目标（luna-ui Vue3）现状
- `src/router/modules/remaining.ts` L53-72：`/` → Layout 包裹 `/index`（工作台）
- `src/permission.ts`：登录守卫白名单 `whiteList`；已登录访问 `/login` 重定向回 `/`
- `src/views/Login/{LunaLogin.vue:267, LoginForm.vue:266, SocialLogin.vue:261,307}`：登录成功默认跳 `/`
- 样式方案 **UnoCSS presetUno**（Tailwind 兼容语法），`uno.config.ts` 无特殊配置
- 包管理器 **pnpm**；未安装 motion-v
- `src/views/Home/Index.vue` 为现有工作台首页（保持不动）

## 关键 API 映射（framer-motion → motion-v）

| React (framer-motion) | Vue (motion-v) | 备注 |
|---|---|---|
| `import { motion, useScroll, useTransform } from 'framer-motion'` | `from 'motion-v'` | 同名 API |
| `<motion.div animate={{...}}>` | `<motion.div :animate="{...}">` | 组件形态一致 |
| `initial/animate/transition` props | `:initial/:animate/:transition` | 一致 |
| `whileInView={{...}} viewport={{ once, amount }}` | `:whileInView` + `:inViewOptions="{ once, amount }"` | **viewport 改名 inViewOptions** |
| `useScroll({ target: ref, offset: [...] })` | 相同，target 传 Vue template ref | 返回 `scrollYProgress` |
| `useTransform(v, [0,0.72], ['58%','100%'])` | 相同 | 支持字符串单位 |
| `animate={{ scaleY:[0.6,1,0.6] }} transition={{ repeat: Infinity }}` | 相同 | 关键帧数组支持 |
| `{...riseBlur(0.15)}` props 展开 | `v-bind="riseBlur(0.15)"` | 返回 `{ initial, animate, transition }` |
| `style={{ y: imgY }}`（MotionValue） | `:style="{ y: imgY }"` | 一致 |

## 实施步骤（Proposed Changes）

### 第 1 步：安装依赖 + 复制静态资源

- `luna-ui` 下执行 `pnpm add motion-v`
- 复制（保留原路径以便代码一致）：
  - `oldweb/public/presentation/assets/fabric.png` → `luna-ui/public/presentation/assets/fabric.png`
  - `oldweb/public/logo.svg` → `luna-ui/public/logo.svg`
  - `oldweb/public/logo_white.svg` → `luna-ui/public/logo_white.svg`

### 第 2 步：新增品牌与数据层（3 个新文件）

**2.1 `luna-ui/src/views/PublicHome/config/brand.ts`**（迁自 `oldweb/lib/brand-config.ts`）
- `BrandConfig` 接口（完整保留字段：clientId/name/nameEn/fullName/slogan/description/logoUrl/logoWhiteUrl/primaryColor/secondaryColor/aiGreeting/footer/heroTitle/heroSubtitle/zoomTitle/zoomSubtitle/ctaTitle/ctaSubtitle 等）
- `BRAND_PRESETS` 预设表（至少保留 lunaagi 预设，其余租户预设按需保留）
- `resolveBrand()`：读 `import.meta.env.VITE_BRAND_CODE`（可选）回退 `lunaagi`；env 覆盖字段映射为 `import.meta.env.VITE_BRAND_*`
- 导出 `defaultBrand`

**2.2 `luna-ui/src/views/PublicHome/hooks/useBrand.ts`**（迁自 `oldweb/hooks/useBrand.ts`）
- 模块级全局状态：`ref<BrandConfig>` + `Set<listener>` 通知
- `initGlobalBrand()`：按 `host:tenantSlug` 组装缓存 key → 读 localStorage → fetch `${VITE_STOREFRONT_API_BASE || ''}/api/tenant/brand?tenant=...`（带 token header）→ 合并更新并写缓存；**失败静默回退默认预设**（luna-ui 后端暂无该接口，页面用 lunaagi 预设正常渲染）
- `?tenant=`/`?tenantPreview=`/`?_tenant=` 参数解析（迁移 `normalizeTenantPreviewSlug`）
- 监听 `brand-config-updated` CustomEvent
- 简化项（记录在代码注释）：不迁移 `scrubCachedBrandConfig`（oldweb 特有上传缓存清理）；`normalizeBrandLogoUrl` 简化为透传

**2.3 `luna-ui/src/views/PublicHome/data/storefront.ts`**
- `PublicProduct`/`PublicProductImage` 接口（照搬 shared.ts）
- 内置 4 款示例针织面料（id/code/name/type/composition/widthCm/weightGsm/coverImageUrl，coverImageUrl 用 `/presentation/assets/fabric.png`），分类数组（如：`['卫衣布','毛圈布','罗纹','珠地']`）
- 导出 `FEATURED_PRODUCTS`、`ALL_PRODUCTS`（可多于 4 款以演示分页，如 8 款）、`CATEGORIES`
- 顶部注释 `TODO: 后续接入 /api/public/products、/api/public/categories 真实接口`

### 第 3 步：迁移 5 个展示组件

目录 `luna-ui/src/views/PublicHome/components/`，全部 `<script lang="ts" setup>`，显式 `import { motion, useScroll, useTransform } from 'motion-v'`。

**3.1 `StorefrontNav.vue`**
- `scrolled` ref + onMounted 挂 scroll 监听（passive）、onBeforeUnmount 移除
- **`document.documentElement.style.scrollBehavior='smooth'` 需在 onBeforeUnmount 清除**（避免污染后台页面滚动行为）
- logo `onerror` 回退英文名；`Link` → `router-link`；员工登录 → `/login`；logo → `/`
- Tailwind class 原样保留（UnoCSS 兼容），`shadow-[0_1px_0_rgba(0,0,0,0.06)]` 下划线写法可用

**3.2 `StorefrontNarrative.vue`**（核心）
- 数据改为本地静态 `FEATURED_PRODUCTS`（删除 fetch effect，保留注释 TODO）
- 进度点 IntersectionObserver 逻辑保留（watch products 变化重建）
- `riseBlur(delay)` 帮助函数 → 返回 `{ initial, animate, transition }`，模板 `v-bind="riseBlur(0.15)"`
- 大图缩放区：`const zoomRef = useTemplateRef<HTMLDivElement>('zoomRef')`（或 `ref(null)`），`useScroll({ target: zoomRef, offset: ['start start','end end'] })`，5 个 `useTransform` 原样
- 遮罩/标题用 `:style="{ opacity: capOpacity, y: capY }"` 绑定 MotionValue
- 系列屏渲染 `<SeriesPanel v-for .../>`；无产品降级品牌介绍段保留（`v-else-if="loaded"` 语义改为直接 v-else）
- `[text-shadow:0_2px_24px_rgba(0,0,0,0.25)]` **改为 inline style**（UnoCSS 不保证 arbitrary property 语法）
- CTA 区 `whileInView` + `:inViewOptions="{ once: true, amount: 0.4 }"`

**3.3 `SeriesPanel.vue`**
- props: `{ product: PublicProduct; index: number; accent: string }`
- 自身 `useScroll({ target, offset: ['start end','end start'] })` + `imgY` 视差
- 大号序号水印、奇偶翻转/底色、GSM·CM 标签胶囊、渐变兜底图原样
- "查看详情 ›" 链接：静态数据无详情路由，渲染为**无 href 的 `<a>`**（保留 accent 色与 hover:underline 视觉，注释说明）

**3.4 `StorefrontLibrary.vue`**
- 状态：products/categories/activeCategory/searchInput/searchQuery/searchFocused/page/total/loading/loadingMore 全部 ref
- 搜索防抖：`watch(searchInput, ...)` + setTimeout 350ms
- **数据源本地分页**：`fetchProducts(pageNum, query, category)` 改为对 `ALL_PRODUCTS` 做内存过滤（search 匹配 code/name/composition；category 匹配 type）+ `slice((pageNum-1)*30, pageNum*30)` 模拟分页与 total（保留函数结构与 TODO 注释，便于后续无缝换回 HTTP）
- `EMPTY_SHOWCASE` 空态品牌视觉 + 骨架屏 animate-pulse + "没有找到匹配的面料" 三分支保留
- lucide Search 图标 → **内联 SVG**（circle+line 两个 path，不引入新依赖）
- 分类 chips 滚动条隐藏：`[-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden` 改为组件内 scoped CSS 类（UnoCSS 对 arbitrary property 支持不确定，稳妥起见手写）
- 产品卡 hover 上浮/图片缩放 transition 原样

**3.5 `StorefrontFooter.vue`**
- 直译模板，props 接收 brand

### 第 4 步：主入口 `luna-ui/src/views/PublicHome/Index.vue`

- `<script setup>`：`const { brand } = useBrand()`；`accent = computed(() => brand.primaryColor || '#0066cc')`
- 模板复刻 `StorefrontHomePage`：根 div（`min-h-screen bg-white text-[#1d1d1f] antialiased` + **scoped/inline 声明 Tailwind 默认字体栈** `font-family: ui-sans-serif, system-ui, -apple-system, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'PingFang SC', 'Microsoft YaHei', sans-serif`，保证与 React 版一致）+ Nav / main(Narrative+Library) / Footer

### 第 5 步：路由与鉴权改造（4 个文件修改）

**5.1 `src/router/modules/remaining.ts`**
- 原 `/`（Layout + redirect '/index' + children index）拆为：
  - `{ path: '/', name: 'PublicHome', component: () => import('@/views/PublicHome/Index.vue'), meta: { hidden: true, noTagsView: true } }`（**无 Layout，独立全屏页**）
  - `{ path: '/index', component: Layout, name: 'Home', redirect: '/index', meta: { hidden: true }, children: [{ path: '', component: () => import('@/views/Home/Index.vue'), name: 'Index', meta: { title: t('router.home'), icon: 'ep:home-filled', noCache: false, affix: true } }] }`

**5.2 `src/permission.ts`**
- `whiteList` 增加 `'/'`（注意：startsWith(`${path}/`) = `'//'` 不会误放行其它路径）
- L34 `next({ path: '/' })` → `next({ path: '/index' })`（已登录用户访问 /login 回工作台而非官网）

**5.3 登录成功默认跳转 `'/' → '/index'`**
- `src/views/Login/components/LoginForm.vue` L266
- `src/views/Login/LunaLogin.vue` L267
- `src/views/Login/SocialLogin.vue` L261、L307

## 假设与决策（Assumptions & Decisions）

1. `/` = 公开官网（免登录），`/index` = 登录后工作台；登录成功落 `/index`
2. 产品/分类数据内置静态（8 款示例、4 类分类），API 结构对齐 `PublicProduct`，留 TODO
3. 品牌系统完整迁移但接口失败回退 lunaagi 预设（页面视觉以 lunaagi.cn 为准：主色 `#0052D4`）
4. 产品"查看详情"无目标页（oldweb 的 `/p/:id` 不在迁移范围），保留视觉不导航
5. motion-v 未覆盖的行为（若个别动画属性差异）以 scoped CSS keyframes 兜底，验证阶段确认
6. 不引入 Tailwind；沿用 UnoCSS，个别 arbitrary property（text-shadow、scrollbar 隐藏）转 inline style/scoped CSS
7. 不迁移 oldweb 的 `tenant-preview` 写参数逻辑（仅保留读 `?tenant=` 切品牌）
8. 公告：PublicHome 仅浅色模式（与原版一致，硬编码色值）

## 验证（Verification）

1. **构建**：`pnpm dev`（env.local）启动无报错；`motion-v` 正常打包
2. **路由**：未登录访问 `/` 直接显示官网（不跳 /login）；访问 `/index` 跳 /login；登录成功落 `/index` 工作台；已登录访问 `/` 也可看官网；官网"员工登录"进 /login
3. **视觉复刻**（对照 https://www.lunaagi.cn/#top + 多视口截图对比）：
   - 首屏大字模糊浮现节奏（0.15/0.35/0.55/0.8s 延迟）与 SCROLL 呼吸线条
   - 大图 58%→100%、74vh→100vh、40→0 圆角滚动放大，遮罩标题后段淡入
   - 系列屏奇偶翻转布局、大号数字水印、图片 ±7% 视差、GSM·CM 胶囊
   - 黑底 CTA（grayscale+brightness 背景、按钮 hover）
   - 面料库搜索（输入防抖 350ms、回车即时）、分类 chips 过滤、2/3/4 列网格、加载更多、已显示 x/y
   - 右侧进度点随滚动高亮（threshold 0.5）、锚点平滑滚动、吸顶导航毛玻璃+滚动阴影
   - 页脚锚点 #contact
4. **响应式**：DevTools 375/768/961/1280/1920 宽度检查断点（961px 系列屏纵→横、导航菜单显隐；721px 网格 2→3 列）
5. **回归**：后台工作台 `/index` 原功能不受影响（ElementPlus 组件、滚动行为无 smooth 污染——离开官网后 documentElement scrollBehavior 已清除）
