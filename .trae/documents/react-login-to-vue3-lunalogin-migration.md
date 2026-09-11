# React 登录页迁移至 Vue3 (LunaLogin) 实施计划

## Context（背景）

`oldweb`（Next.js / React）项目的登录页 `app/auth/login/page.tsx` 部署于 `https://www.lunaagi.cn/auth/login`，是一套深色极光背景 + 玻璃拟态登录卡的视觉设计，使用 Framer Motion 做入场动画、鼠标柔光与错误提示高度过渡。

需将其完整迁移到 Vue3 项目 `luna-ui` 中，作为官网首页（PublicHome）"员工登录"点击后的登录界面，命名为 **LunaLogin**。要求：
- 视觉、交互、特效 1:1 复刻（用 Motion Vue = `motion-v` 替代 Framer Motion）
- 登录逻辑与现有 `src/views/Login/components/LoginForm.vue` 保持一致
- 不影响/覆盖已有 `/login` 路由与 `Login.vue`

经探索确认：`motion-v@^2.4.2` 已在 `luna-ui` 中安装并在 `HomeHero.vue`/`SeriesPanel.vue` 中使用，无需新增依赖。

## 已锁定的关键决策（来自用户确认）

| 议题 | 决策 |
|------|------|
| 登录字段 | 改为**通用用户名**输入框（占位符「请输入用户名」，不使用手机号正则），对齐 Login.vue 的 `username` 字段 |
| 验证码 + 租户 | **全部纳入**：集成滑块验证码（Verify，由 `VITE_APP_CAPTCHA_ENABLE` 控制）+ 移户名输入（由 `VITE_APP_TENANT_ENABLE` 控制） |
| Passkey（生物识别） | **移除**：不迁移 Passkey 模式切换与 WebAuthn 调用，仅保留账号+密码单表单 |
| 强制改密弹窗 | 不迁移（Login.vue 逻辑中无此流程） |
| 登录逻辑来源 | 复用 `LoginForm.vue` 的 `handleLogin` 全流程（同 API、同 auth 工具、同重定向） |

## 目标文件结构

新增（`src/views/LunaLogin/`）：
- `LunaLogin.vue` — 主页面（迁移自 `oldweb/app/auth/login/page.tsx`）
- `components/LoginAura.vue` — 极光背景（迁移自 `oldweb/components/auth/LoginAura.tsx`）
- `components/LoginCursorField.vue` — Canvas 鼠标光晕（迁移自 `oldweb/components/auth/LoginCursorField.tsx`）
- `components/LoginHero.vue` — 首屏主视觉（迁移自 `oldweb/components/auth/LoginHero.tsx`）
- `components/LoginLogo.vue` — 顶部字标（迁移自 `oldweb/components/auth/LoginLogo.tsx`）
- `components/LoginOrb.vue` — **删除**（不再需要包装层，LoginHero 直接用 vendored `ThinkingOrb`）

vendored 共享组件（新增，独立于 LunaLogin，可全局复用）：
- `src/components/thinking-orb/` — 从 `cxk1993/thinking-orb-vue` 源码 vendor 进来的 Vue3 点阵光球组件（详见步骤 0）

修改：
- `src/router/modules/remaining.ts` — 在 `/login` 路由后新增 `/luna-login` 路由
- `src/views/PublicHome/components/HomeNav.vue` — "员工登录" `RouterLink` 的 `to` 由 `/login` 改为 `/luna-login`（`/login` 路由保持不变）

## 实施步骤

### 0. Vendor `thinking-orb-vue` 光球组件
经查证：`cxk1993/thinking-orb-vue`（Vue3 原生端口，MIT）**未发布到 npm**（registry 404），其 README"集成到 OpenSquilla"也明确要求复制 `src/` 到目标项目。因此采用 **vendor 源码**方式集成，不 npm install。

要 vendor 的文件（来自 `thinking-orb-vue/src/`，**排除** `src/demo/` 与 `src/env.d.ts`）：
```
luna-ui/src/components/thinking-orb/
├── ThinkingOrb.vue          # 主组件
├── index.ts                 # 库入口（导出 ThinkingOrb 等）
├── presets.ts               # 9 状态配置 + 密度插值
├── registry.ts              # 模式插件注册系统
├── engine/
│   ├── core.ts              # 多层渲染管线 + 3D 数学
│   ├── clock.ts             # 全局共享时钟 + 自适应帧率
│   └── types.ts             # AnimationMode 接口
├── modes/                   # 9 个模式实现
│   ├── OrbitsMode.ts  GlobeMode.ts  RubikMode.ts
│   ├── WaveMode.ts    WebMode.ts    BraidMode.ts
│   └── RibbonMode.ts  RingMode.ts    MorphMode.ts
└── composables/
    ├── useOrbTheme.ts       # 主题检测（data-theme/class/prefers-color-scheme）
    └── useOrbAnimation.ts   # 动画循环 + IntersectionObserver/visibilitychange
```

集成要点：
- 放在 `src/components/thinking-orb/`（共享目录，符合 luna-ui 约定，光球作为通用加载指示器可在 AI 对话等场景复用）
- 该库**零外部依赖**：仅 `import { ref, computed, ... } from 'vue'`，主题/可见性检测用原生 `matchMedia`/`MutationObserver`/`IntersectionObserver`，不引入 @vueuse 或 React
- 所有内部导入都是相对路径（`./composables/...`、`../engine/...`），vendor 后无需改路径
- Vendor 后在 `package.json` 末尾或 README 注明来源：`// Vendored from https://github.com/cxk1993/thinking-orb-vue (MIT)` 保留版权
- TS 严格性微调：`useOrbAnimation.ts` 中 `typeof devicePixelRatio` 引用全局，luna-ui 的 tsconfig 下可能告警，实施时按需补 `window.` 前缀

### 1. 路由注册
在 `src/router/modules/remaining.ts` 的 `/login` 路由块后追加：
```ts
{
  path: '/luna-login',
  component: () => import('@/views/LunaLogin/LunaLogin.vue'),
  name: 'LunaLogin',
  meta: { hidden: true, title: '员工登录', noTagsView: true }
}
```

### 2. HomeNav 接线
`HomeNav.vue` 第 32-37 行 `<RouterLink to="/login">` 改为 `to="/luna-login"`。其余不动。

### 3. 装饰子组件迁移（纯视觉，无业务逻辑）

**LoginLogo.vue** — 直接迁移：`<img src="/logo_white.svg">` + `logo-glow` 关键帧脉动 + reduced-motion 降级。`<style scoped>` 替换 React `style jsx`。

**LoginCursorField.vue** — 直接迁移 Canvas 2D 鼠标光晕逻辑：`onMounted` 启动 rAF 绘制循环、监听 mousemove/resize，`onBeforeUnmount` 清理。仅在 `pointer: fine` 且非 reduced-motion 时启用。`mix-blend-mode: overlay`。

**LoginAura.vue** — 直接迁移：46 条 `.aura-col` + `aura-flow` 关键帧（蓝→青→薄荷→紫→品红循环）、3 个 `.aura-glow` 极光层、底部暖色渐隐、顶部压暗层。内嵌 `<LoginCursorField>`。全部 CSS 动画迁到 `<style scoped>`，reduced-motion 降级保留。

**LoginOrb.vue** — **不再创建此文件**。光球直接用步骤 0 vendored 的 `ThinkingOrb` 组件，在 `LoginHero.vue` 中 `<ThinkingOrb state="breathing" :size="150" theme="dark" />` 一行即可。原 `LunaOrb` 数百行手写 Canvas 代码被完全替换为声明式调用，是本次简化的核心收益。

**LoginHero.vue** — 迁移结构：`<ThinkingOrb state="breathing" :size="150" theme="dark" />`（从 `@/components/thinking-orb` 导入）+ 磨砂白标签（客户 LOGO 或英文名 `LUNAAGI`）+ 双层标题「让智能 接管工作」（上层 `--glow` 模糊呼吸，下层 `--above` 实色）+ 副文案「让 AI 驱动每一天的业务协作」。`login-title--glow` 的 `title-breathe` 关键帧 + reduced-motion 降级。Props: `logoUrl`, `brandNameEn`，复用原 `isGenericLuna` 判断（logo 为默认 `/logo(_white)?.svg` 时回退英文名）。`state="breathing"`（ring 模式，缓慢脉动）最贴近原 `LunaOrb` 的 idle 装饰用途；`theme="dark"` 强制浅色点阵（登录极光背景是深蓝，需要亮墨）。

### 4. 主页面 LunaLogin.vue

**模板结构**（1:1 对齐 React page.tsx）：
- 根 `div.relative.min-h-screen.w-full.overflow-hidden.bg-[#f5f8ff].font-sans.text-slate-900.antialiased` + `@mousemove`
- `<LoginAura>` 绝对背景
- `header.relative.z-20.flex.items-center.px-6.py-5.sm:px-10` 内含 `<LoginLogo>`
- `main.relative.z-10.flex.min-h-[calc(100vh-80px)].flex-col.items-center.just-center.px-4.pb-12`
  - `<motion.div>` hero 入场（initial `{opacity:0,y:16}` → animate `{opacity:1,y:0}`，transition `{duration:0.7, ease:[0.16,1,0.3,1]}`）内含 `<LoginHero>`
  - `<motion.div>` 玻璃卡入场（同上，duration 0.8, delay 0.1）`class="group relative w-full max-w-[360px]"`
    - 鼠标柔光 `<motion.div class="pointer-events-none absolute -inset-px rounded-[2rem] opacity-0 transition-opacity duration-500 group-hover:opacity-100" :style="{ background: mouseGradient }">`
    - 内层含 `<h2>登入系统</h2>` + 表单
- 表单（el-form 包裹以复用 Login.vue 的校验规则；通过 `:deep` 把 el-input 样式覆盖为 React 玻璃胶囊外观）：
  - 租户名输入（`v-if="tenantEnable === 'true'"`，图标 `ep:house`，React 玻璃胶囊样式）
  - 用户名输入（`ep:user`，占位「请输入用户名」，required 校验）
  - 密码输入（`ep:lock`，show-password 切换 `ep:eye`/`ep:eye-off`，React 玻璃胶囊样式）
  - 行：左侧 `<el-checkbox v-model="rememberMe">记住我</el-checkbox>` + 右侧 `<RouterLink to="/login">忘记密码？</RouterLink>`（跳到现有 Login.vue 的忘记密码流程）
  - `<AnimatePresence>` 包裹的错误提示 `<motion.div v-if="errorMessage" :initial="{opacity:0,height:0}" :animate="{opacity:1,height:'auto'}" :exit="{opacity:0,height:0}" class="overflow-hidden">` 内含 `<div class="rounded-2xl bg-rose-50 px-4 py-2.5 text-xs font-medium text-rose-500">{{ errorMessage }}</div>`
  - `<Verify v-if="captchaEnable === 'true'" ref="verify" :captchaType="captchaType" :imgSize="{width:'400px',height:'200px'}" mode="pop" @success="handleLogin" />`（全局组件，与 LoginForm.vue 同用法）
  - 提交按钮（React 白色胶囊样式 `shadow-[0_18px_50px_-12px_rgba(80,90,200,0.6)]` + `hover:-translate-y-0.5`）：loading 时转圈 spinner，否则 `<ArrowRight/> 登录系统`
- 底部安全脚注：`<ShieldCheck>` 图标 + "仅限授权人员访问 · 已通过安全加密"

**Motion Vue 用法**（遵循 `HomeHero.vue` 约定）：
```ts
import { motion, AnimatePresence, useMotionValue, useMotionTemplate } from 'motion-v'
const mouseX = useMotionValue(0)
const mouseY = useMotionValue(0)
const mouseGradient = useMotionTemplate`radial-gradient(420px circle at ${mouseX}px ${mouseY}px, rgba(99,102,241,0.10), transparent 70%)`
function onMouseMove(e) {
  const { left, top } = e.currentTarget.getBoundingClientRect()
  mouseX.set(e.clientX - left); mouseY.set(e.clientY - top)
}
```
模板用 `<motion.div>` / `<motion.p>`，绑定 `:initial` `:animate` `:exit` `:transition`。图标用项目既有 `useIcon`（`@/hooks/web/useIcon`），避免引入 lucide-react。

**登录逻辑**（`<script setup>` 内联，与 `LoginForm.vue` 同源同构）：
```ts
import * as LoginApi from '@/api/login'
import * as authUtil from '@/utils/auth'
import { usePermissionStore } from '@/store/modules/permission'
import { ElLoading } from 'element-plus'
import { useBrand } from '@/views/PublicHome/hooks/useBrand'  // 复用品牌配置
```
完整复刻 `LoginForm.vue` 的：
- `getCode()`：captcha 关闭则直连 `handleLogin({})`，否则 `verify.value.show()`
- `handleLogin(params)`：`getTenantId()` → `validForm()` → `LoginApi.login({tenantName,username,password,captchaVerification,rememberMe})` → `ElLoading.service` → `setLoginForm`/`removeLoginForm` → `authUtil.setToken(res)` → 重定向 `redirect.value || '/index'`（SSO 特例保留，replace 模式适配 `/luna-login?redirect=`）→ `push({path: redirect || permissionStore.addRouters[0].path})`
- `getTenantId()` / `getTenantByWebsite()` / `getLoginFormCache()`：同 LoginForm.vue
- `onMounted`：`getLoginFormCache()` + `getTenantByWebsite()`
- `watch(currentRoute)`：读 `?redirect=` 参数
- 品牌数据用 `useBrand()` 的 `globalBrandState`（已与官网首页共享），传 `logoUrl`/`nameEn` 给 `<LoginHero>`

**样式**：`<style scoped>` 内用 `:deep(.el-input__wrapper)` 把 el-input 外观改写为 React 玻璃胶囊（`rounded-full` + `bg-white/65` + `backdrop-blur` + `border-white/50` + `focus-within` ring）。根容器背景由 `<LoginAura>` 深蓝覆盖，文字白色。

## 复用清单（避免重复造轮子）

| 复用项 | 路径 |
|--------|------|
| 登录 API | `src/api/login/index.ts` — `login` / `getTenantIdByName` / `getTenantByWebsite` |
| Auth 工具 | `src/utils/auth` — `setToken` / `setTenantId` / `setLoginForm` / `removeLoginForm` / `getLoginForm` / `getTenantId` |
| 用户/权限 store | `src/store/modules/permission.ts` — `usePermissionStore` |
| 品牌配置 | `src/views/PublicHome/config/brand.ts` + `src/views/PublicHome/hooks/useBrand.ts` |
| 图标系统 | `src/hooks/web/useIcon.ts` — `useIcon({icon:'ep:user'})` |
| 验证码组件 | 全局 `<Verify>`（与 LoginForm.vue 同源，无需显式 import） |
| Motion Vue | `motion-v` — `motion` / `AnimatePresence` / `useMotionValue` / `useMotionTemplate` |
| 光球组件 | `src/components/thinking-orb` — vendored `ThinkingOrb`（Vue3 原生，零外部依赖，`state`/`size`/`theme` props） |
| 路由注册模式 | `src/router/modules/remaining.ts` 现有 `meta:{hidden:true,noTagsView:true}` 模式 |

## 验证方式（实施后）

1. **启动**：`cd luna-ui && pnpm dev`（或 `npm run dev`）
2. **路由可达**：访问 `http://localhost:端口/luna-login`，页面正常渲染
3. **首页接线**：访问 `/`（PublicHome），点击右上"员工登录"，跳转到 `/luna-login`
4. **逻辑回归**：在 LunaLogin 输入用户名/密码登录，验证：
   - 登录成功 → 跳转到 `/index`（或 `?redirect=` 指定页）
   - token 写入（`getAccessToken()` 有值）、`wsCache` 用户信息写入
   - 租户开关：`VITE_APP_TENANT_ENABLE=true` 时租户输入框出现、按域名解析租户
   - 验证码开关：`VITE_APP_CAPTCHA_ENABLE=true` 时点登录先弹滑块、验证通过才提交
   - 记住我：勾选后刷新页面，用户名/密码回填
5. **不影响旧登录**：`/login` 仍打开原 `Login.vue`，功能未变
6. **视觉对比**：并排打开 `https://www.lunaagi.cn/auth/login` 与本地 `/luna-login`，逐项比对：极光背景色波流动、Canvas 鼠标光晕拖尾、hero 光球呼吸、标题模糊光晕、玻璃卡入场动画、鼠标柔光径向渐变、错误提示高度过渡、按钮 hover 抬升与阴影变化、响应式（窄屏下 hero 字号收缩、卡片宽度自适应）
7. **reduced-motion**：系统开启"减少动效"时，极光/光球/标题光晕静帧，仅保留入场淡入
8. **类型/构建**：`pnpm run build` 通过，无 TS 报错

## 关键风险与对策

- **el-input 与 React 视觉差异**：el-form 的 el-input 默认外观与 React 玻璃胶囊不同，需 `:deep` 覆写 `el-input__wrapper` 样式。若覆写过复杂，回退用原生 `<input>` + 手动 required 校验（牺牲 el-form 的 rules 一致性，但视觉 100% 复刻）。
- **光球视觉风格变化**：用 vendored `thinking-orb-vue` 的点阵光球（halftone）替代原 `LunaOrb` 的奶白柔光球。两者风格不同（点阵 vs 柔光实心球），但同为深色背景上的发光球体，氛围一致；这是用户主动选择的简化方案。state 选 `breathing` 最贴近原 idle 装饰用途，实施时若观感偏差较大可试 `working`/`listening`。
- **vendored 组件维护成本**：`thinking-orb-vue` 未发布 npm，源码 vendor 进 `src/components/thinking-orb/` 后成为项目自有代码，后续无自动更新。已确认零外部依赖（仅 `vue` + 原生浏览器 API），vendor 风险低。`useOrbAnimation.ts` 中 `typeof devicePixelRatio` 全局引用在严格 tsconfig 下可能告警，实施时补 `window.` 前缀。Vendor 时保留 MIT 版权注明。
- **Tailwind/UnoCSS 类名兼容**：React 用标准 Tailwind 类名（`bg-white/65`、`shadow-[...]`、`sm:` 前缀）；luna-ui 的 UnoCSS 配置需支持这些。`PublicHome` 组件已大量使用同类写法，预期兼容；个别不支持的任意值类名实施时验证。
