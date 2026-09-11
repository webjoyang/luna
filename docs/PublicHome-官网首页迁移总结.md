# PublicHome 官网首页迁移总结（2026-09-11）

## 一句话定位

把 `oldweb/`（Next.js + React + framer-motion + Tailwind）的 storefront 官网首页完整迁移到 `luna-ui/`（Vue 3.5 + Vite + ElementPlus + UnoCSS + TS），作为**免登录公开首页**，实现方案是「motion-v 替代 framer-motion，UnoCSS 承接 Tailwind arbitrary values，本地静态数据对齐旧接口形状」。

参考效果：https://www.lunaagi.cn/

---

## 最终路由结构

| 路径 | 组件 | Layout | 登录要求 | 说明 |
|---|---|---|---|---|
| `/` | `PublicHome/Index.vue` | **无**（全屏独立页） | 免登录 | 公开官网首页 |
| `/index` | `Home/Index.vue` | 后台 Layout（侧边栏） | 需登录 | 原工作台，保持不动 |
| `/login` | 登录页 | — | — | 已登录访问会重定向 `/index` |

登录成功默认跳转 `/index`（不是 `/`，否则登录后会看到官网）。

---

## 文件清单

### 新增（10 个源码文件 + 3 份静态资源）

```
luna-ui/
├── public/
│   ├── logo.svg                              (从 oldweb 复制)
│   ├── logo_white.svg                        (从 oldweb 复制)
│   └── presentation/
│       ├── assets/fabric.png                 (兜底灰度面料图，CTA 黑底区做氛围)
│       └── assets/products/
│           ├── product-001.jpg  ~ product-008.jpg  (从 lunaagi.cn 下载的真实面料照片)
└── src/views/PublicHome/
    ├── Index.vue                              主入口（挂载/卸载时临时解除 App.vue 的 overflow:hidden）
    ├── config/brand.ts                        BrandConfig 接口 + BRAND_PRESETS 预设 + resolveBrand()
    ├── hooks/useBrand.ts                      模块级 ref + localStorage 缓存 + /api/tenant/brand 拉取（失败回退默认）
    ├── data/storefront.ts                     PublicProduct 类型 + 8 款静态产品 + 分类
    └── components/
        ├── StorefrontNav.vue                  吸顶毛玻璃导航（smooth scroll + 滚动阴影 + 登录链接）
        ├── StorefrontNarrative.vue            核心滚动叙事：首屏大字模糊浮现 + 大图滚动放大铺满 + 系列屏 + 黑底 CTA
        ├── SeriesPanel.vue                    单个系列屏（奇偶翻转布局 + whileInView 淡入 + 图片视差）
        ├── StorefrontLibrary.vue              面料库（防抖搜索 + 分类 chips + 分页网格 + 空态）
        └── StorefrontFooter.vue               页脚（id="contact" 锚点）
```

### 修改（7 个现有文件）

| 文件 | 改动 |
|---|---|
| `package.json` | 新增 `motion-v@2.4.2` 依赖 |
| `src/router/modules/remaining.ts` | `/` 从 Layout+redirect 改为 PublicHome 独立页；工作台移至 `/index` |
| `src/router/index.ts` | `scrollBehavior` 支持 hash 锚点平滑滚动（原版无条件回顶部覆盖了锚点） |
| `src/permission.ts` | 白名单加 `/`；已登录访问 `/login` 重定向 `/index` |
| `src/views/Login/components/LoginForm.vue` | 登录成功默认跳转 `/` → `/index` |
| `src/views/Login/LunaLogin.vue` | 同上 |
| `src/views/Login/SocialLogin.vue` | 同上（两处） |

---

## 技术映射（React → Vue）

### 动画库：framer-motion → motion-v（官方 Vue 版，`npm i motion-v`）

| React | Vue | 备注 |
|---|---|---|
| `import { motion, useScroll, useTransform } from 'framer-motion'` | `import { motion, useScroll, useTransform } from 'motion-v'` | 同名 API |
| `<motion.div animate={{...}}>` | `<motion.div :animate="{...}">` | props 前加 `:` |
| `whileInView={{...}} viewport={{ once, amount }}` | `:whileInView {...}` + `:inViewOptions="{ once: true, amount }"` | **viewport 改名 inViewOptions** |
| `useScroll({ target: ref, offset: [...] })` | 相同，target 传 Vue template ref | 返回 MotionValue |
| `useTransform(v, [0,0.72], ['58%','100%'])` | 相同 | 支持字符串单位 |
| `{...riseBlur(0.15)}` props 展开 | `v-bind="riseBlur(0.15)"` | 返回 `{ initial, animate, transition }` |
| `style={{ y: imgY }}`（MotionValue） | `:style="{ y: imgY }"` | 一致 |

**注意**：motion-v 的 MotionValue 可以直接绑定到 `:style` 上，不需要 `.value`（和 reactive ref 不同）。

### 样式：Tailwind → UnoCSS（预设 presetUno，基本兼容）

- 大部分 Tailwind 原子类原样可用（`min-[961px]:`/`max-[960px]:` 断点变体、`bg-[#f5f5f7]` arbitrary values 等都已验证生成正确媒体查询）
- **UnoCSS 不保证支持 Tailwind 的 `[text-shadow:0_2px_24px_rgba(...)]` arbitrary property 语法**——迁移时改为 inline `style="text-shadow: 0 2px 24px rgba(0,0,0,0.25)"`
- 分类 chips 隐藏滚动条：UnoCSS 不支持 `[-ms-overflow-style:none] [scrollbar-width:none] [&::-webkit-scrollbar]:hidden` 这组 arbitrary property，**改为组件内 scoped CSS**（`.chips-scroll { -ms-overflow-style:none; scrollbar-width:none; }` + `::-webkit-scrollbar { display:none }`）

### 路由：next/link → vue-router

- `<Link href="/staff">` → `<RouterLink to="/login">`
- `<a href="#library">` 锚点导航需要 router/index.ts 的 `scrollBehavior` 配合（见坑点 #2）

### 数据层

- 原 React 版：`useEffect` + `fetch()` 调用 `/api/public/products?featured=1&pageSize=4`、`/api/public/categories`、`/api/tenant/brand`
- Vue 迁移版：**内置静态数据**（8 款产品 + 4 个分类），代码结构完全对齐原接口形状（`PublicProduct` 接口字段一致，`resolvePublicAssetUrl` 逻辑一致），TODO 标记后续换回 HTTP 即可无缝对接
- 产品封面图：最初用灰度 fabric.png 导致全页黑白 → 换 picsum.photos 占位图 → 最终从 https://www.lunaagi.cn/ 真实产品抓取下载了 8 张（`public/presentation/assets/products/product-001.jpg ~ product-008.jpg`）

---

## 坑点清单（重要，后续维护必看）

### 坑 #1：App.vue 的 overflow:hidden 锁死了文档滚动容器

**现象**：官网首页（不走 Layout，全屏独立页）滚动时 html scrollHeight 只有 490px，所有滚动动画（sticky + useScroll）失效。

**根因**：`src/App.vue` L41-47 全局定义了 `html, body { height: 100%; overflow: hidden }`——这是后台 Layout 用 el-scrollbar__wrap 内部滚动容器的策略，但官网首页依赖原生文档流滚动（sticky + window.scrollY + useScroll）。

**修复**：`PublicHome/Index.vue` 的 `onMounted` 中临时解除，`onBeforeUnmount` 中还原：
```ts
onMounted(() => {
  const html = document.documentElement, body = document.body
  savedHtmlOverflow = html.style.overflow
  savedBodyOverflow = body.style.overflow
  savedHtmlHeight = html.style.height
  savedBodyHeight = body.style.height
  html.style.overflow = 'visible'
  body.style.overflow = 'visible'
  html.style.height = 'auto'
  body.style.height = 'auto'
})
onBeforeUnmount(() => { /* 还原 saved* */ })
```

### 坑 #2：router scrollBehavior 覆盖了锚点导航

**现象**：点击右侧进度点（`a[href="#cta"]`）后 URL hash 变了，但页面仍停在顶部。

**根因**：`router/index.ts` 的 `scrollBehavior: () => { ... return { left:0, top:0 } }` 无条件回顶部，覆盖了浏览器原生的锚点平滑滚动。

**修复**：
```ts
scrollBehavior: (to) => {
  if (to.hash) return { el: to.hash, behavior: 'smooth' }  // 锚点交给浏览器
  // 后台 Layout 仍然回顶部
  ... return { left: 0, top: 0 }
}
```

### 坑 #3：StorefrontNarrative.vue 忘记 import SeriesPanel

**现象**：页面仅渲染出 logo，所有 section 都没了，控制台报 `<SeriesPanel />` 未注册。

**根因**：React 版是 JSX 自动 import，Vue SFC 必须显式 `import SeriesPanel from './SeriesPanel.vue'`。

**教训**：Vue 组件导入必须显式写在 `<script>` 里，UnoCSS/Vue 不会帮你自动找。

### 坑 #4：面料图全是黑白

**现象**：产品卡片、系列屏图片全部灰度。

**根因**：`oldweb/public/presentation/assets/fabric.png` 本身就是**灰度占位图**（100 采样像素中仅 24 个有微弱色差，是压缩噪声不是彩色），8 款演示产品全部复用这张。

**修复**：从 https://www.lunaagi.cn/ 真实产品抓取下载了 8 张彩色面料照片，存 `public/presentation/assets/products/`，`storefront.ts` 每个产品配不同路径。

**不动的 grayscale**：CTA 黑底区的 `grayscale brightness-[0.45]` 和空态展示区的 `grayscale(1)` 是 React 原版的**设计意图**（暗背景氛围/无产品时的品牌视觉占位），故意保留。

### 坑 #5：useBrand 的 listener 订阅机制在 Vue 中冗余

**现象**：迁移 React 版的 `Set<listener>` 订阅机制到 Vue 时，模块级 ref 本身就是全局响应式的，所有组件读同一个 `globalBrandState` 会自动更新。

**修复**：删除 listeners + subscribe/unsubscribe 逻辑，简化为模块级 `const globalBrandState = ref<BrandConfig>({...defaultBrand})`，`initGlobalBrand()` 幂等执行，监听 `brand-config-updated` CustomEvent 即可。

---

## 当前架构图

```
访问 /  →  permission.ts 白名单放行 →  PublicHome/Index.vue（无 Layout）
  ├─ StorefrontNav.vue         smooth scroll 监听 + 登录链接 → /login
  ├─ StorefrontNarrative.vue   motion-v 动画：riseBlur + 大图滚动缩放 + 系列屏 + CTA
  │   └─ SeriesPanel.vue        × 4，每屏 useScroll 视差 + whileInView 淡入
  ├─ StorefrontLibrary.vue     防抖搜索 + 分类 chips + 2/3/4 列网格
  └─ StorefrontFooter.vue      id="contact" 锚点 + 版权

登录成功 →  permission.ts 已拉 userStore + permissionStore.generateRoutes()
         →  router.push('/index')  →  Home/Index.vue（原工作台，Layout 包裹）
```

---

## 待办（TODO 留在代码里了，这里汇总方便后续 AI 接）

1. **接入真实 API**：`data/storefront.ts` 的静态数据要换回 `/api/public/products`（featured + 分页）和 `/api/public/categories`，`hooks/useBrand.ts` 的 `/api/tenant/brand` 目前失败静默回退（luna-ui 后端暂无该公开接口）
2. **产品详情页**："查看详情 ›" 链接目前无 href（React 版有 `/p/:id` 但不在本次迁移范围）
3. **pnpm store 权限**：`pnpm add motion-v` 在沙箱内会 EPERM（`D:\.pnpm-store` 不在白名单），需要 `dangerouslyDisableSandbox: true` 或手动改 sandbox 配置
4. **生产构建验证**：dev 和类型检查已过，`pnpm build:local` 还没跑过（权限超时）

---

## 原项目参照（迁移源）

```
oldweb/
├── app/page.tsx                                  StorefrontHomePage 入口
├── app/components/storefront/
│   ├── StorefrontNav.tsx                         吸顶毛玻璃导航
│   ├── StorefrontNarrative.tsx                   核心滚动叙事（motion.div + useScroll + useTransform）
│   ├── StorefrontLibrary.tsx                     面料库（防抖搜索 + 分类 chips + 分页网格）
│   ├── StorefrontFooter.tsx                      页脚
│   └── shared.ts                                 PublicProduct 类型 + resolvePublicAssetUrl + useTenantPreviewQuery
├── hooks/useBrand.ts                             品牌配置 hook（多租户 + 缓存）
├── lib/brand-config.ts                           BrandConfig 接口 + BRAND_PRESETS + resolveBrand()
└── public/presentation/assets/fabric.png          兜底面料图（灰度）
```
