# LunaLayout 后台布局改动归档（2026-09-12）

## 一句话定位

登录后后台统一使用 `LunaLayout`：无 Logo、无顶部 ToolHeader；侧栏可完全隐藏；原 ToolHeader 收纳到侧栏底部「更多」竖排弹出框；支持默认收起 / 悬停展开 / 移出收起 / 路由跳转收起；`/index` 为空占位页。

关联计划副本：

- [lunalayout_shell_362746b5.plan.md](./lunalayout_shell_362746b5.plan.md)
- [luna_menu_auto_collapse_295ee8e4.plan.md](./luna_menu_auto_collapse_295ee8e4.plan.md)

索引：[README-Luna布局归档.md](./README-Luna布局归档.md)

---

## 已确认产品决策

| 项 | 结论 |
|---|---|
| 范围 | 全局替换 Layout（静态路由 + `generateRoute`） |
| ToolHeader | 侧栏底部「更多」弹出，不保留顶栏 |
| 折叠形态 | `collapse === true` → 侧栏宽度 0（完全隐藏），内容全宽 |
| Footer | 不改，继续用 `AppView` |
| 自动折叠 | ① 默认收起 ② 悬停展开 / 移出收起 ③ 点菜单（路由变化）后收起 |
| 布局模式 | 首期只做 classic 竖侧栏 |

---

## 最终架构

```text
routerHelper.Layout
  → layout/luna/LunaLayout.vue
      useLunaAutoCollapse() + provide
      useRenderLunaLayout(autoCollapse)   ← API 参数直传（不能同组件 inject）
        展开侧栏:
          Collapse（仅图标，菜单上方）
          Menu
          LunaToolPanel（更多 Popover，竖排+文字）
        折叠:
          LunaMenuReopen（悬停/点击展开）
        内容区:
          TagsView + AppView → router-view
```

接入点 [`luna-ui/src/utils/routerHelper.ts`](../luna-ui/src/utils/routerHelper.ts)：

```ts
export const Layout = () => import('@/layout/luna/LunaLayout.vue')
```

---

## 分阶段改动记录

### 阶段 A — 全局壳替换

- 新增 `layout/luna/`：`LunaLayout` / `useRenderLunaLayout` / `LunaToolPanel` / `LunaMenuReopen`
- `Layout` 懒加载改指 Luna 壳；原 `Layout.vue` 保留不接入
- 清空 `views/Home/Index.vue`（保留 `name: 'Index'`）
- Footer 不做特殊处理

### 阶段 B — 交互微调

- 折叠入口从「更多」弹出框移到**菜单上方**，去掉「折叠菜单」文字，仅保留图标
- 「更多」内工具从横向改为**竖向列表**；纯图标项补充文字（项目配置 / 全屏 / 搜索 / 尺寸 / 语言 / 消息 / 即时通讯）
- UserInfo 已有头像+昵称，不重复加字

### 阶段 C — 自动折叠

- 新增 `useLunaAutoCollapse.ts`
- 挂载默认 `setCollapse(true)`
- 悬停 `LunaMenuReopen` 展开；侧栏 `mouseleave` 延时收起（约 280ms，刚展开有宽限）
- 「更多」`onShow`/`onHide` 锁定，打开时不因 leave 收起；`teleported=false`
- `watch(route.fullPath)` 跳转后收起
- 移动端：禁用悬停收起/展开，保留点击 + 遮罩

### 阶段 D — Bug 修复（移出不收起）

- **现象**：悬停可展开，鼠标移出菜单区域不隐藏
- **原因**：在 `LunaLayout` 的 render 里 `inject` 同组件 `provide` 的值，inject 只向上找父级，得到 `null`
- **修复**：`provide` 给子组件；同时 `useRenderLunaLayout(autoCollapse)` **参数直传**给侧栏 mouse 事件

---

## 最终文件清单

### 新增

```
luna-ui/src/layout/luna/
├── LunaLayout.vue
├── useRenderLunaLayout.tsx
├── useLunaAutoCollapse.ts
├── LunaToolPanel.vue
└── LunaMenuReopen.vue
```

### 修改

| 文件 | 改动 |
|---|---|
| `luna-ui/src/utils/routerHelper.ts` | `Layout` → LunaLayout |
| `luna-ui/src/views/Home/Index.vue` | 清空为占位 |

### 刻意未改

- `layout/Layout.vue`、`useRenderLayout.tsx`、`ToolHeader.vue`、`AppView.vue`
- 共享 `Menu` / `TagsView` / `UserInfo` / `Collapse` 等（复用）

---

## 桌面端行为速查

| 场景 | 行为 |
|------|------|
| 进入后台 | 侧栏默认隐藏 |
| 悬停左侧展开条 | 展开侧栏 |
| 鼠标离开侧栏 | ~280ms 后隐藏（更多打开中除外） |
| 点菜单 / 换路由 | 隐藏侧栏 |
| 更多弹出打开 | 不因移入面板而误关 |

---

## 路由关系

| 路径 | 组件 | 外壳 |
|------|------|------|
| `/` | PublicHome | 无 |
| `/index` | Home/Index（空） | LunaLayout |
| 其它后台页 | views/... | LunaLayout |

---

## 验证清单

- [x] `/index` 空内容、无 Logo、无顶栏
- [x] 更多竖排 + 图标配文字
- [x] 折叠图标在菜单上方（无「折叠菜单」文案）
- [x] 默认收起；悬停展开；移出收起
- [x] 路由跳转后收起
- [x] 更多打开时不误收起
- [x] 移出菜单可正常隐藏（provide/inject 修复后）
