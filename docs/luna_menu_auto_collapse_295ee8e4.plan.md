---
name: Luna Menu Auto Collapse
overview: 在 LunaLayout 中实现侧栏三种自动折叠行为：进入后台默认收起、悬停展开/移出收起、路由跳转后收起；移动端仅保留点击展开与遮罩收起，避免悬停误触。
todos:
  - id: auto-collapse-composable
    content: 新增 useLunaAutoCollapse.ts（默认折叠、路由折叠、悬停延时、更多面板锁）
    status: completed
  - id: wire-luna-layout
    content: LunaLayout provide + useRenderLunaLayout 侧栏 mouseenter/leave
    status: completed
  - id: reopen-hover
    content: LunaMenuReopen 支持 mouseenter 展开
    status: completed
  - id: toolpanel-popover
    content: LunaToolPanel 同步弹出开关 + teleported=false
    status: completed
  - id: verify-auto-collapse
    content: 桌面/移动交互自测
    status: completed
isProject: false
---

# Luna 菜单自动折叠

> 来源：`C:\Users\Luna\.cursor\plans\luna_menu_auto_collapse_295ee8e4.plan.md`（Cursor plans 存档副本）

## 行为（桌面端）

| 场景 | 行为 |
|------|------|
| 进入 / 挂载 LunaLayout | `setCollapse(true)`，侧栏默认完全隐藏 |
| 悬停左侧 `LunaMenuReopen` | `setCollapse(false)` 展开 |
| 鼠标离开侧栏 | 延迟约 280ms 后 `setCollapse(true)`（避免划向弹出层时立刻关掉） |
| 「更多」弹出框打开中 | 禁止因 mouseleave 自动收起 |
| 路由 `fullPath` 变化 | `setCollapse(true)`（覆盖点菜单、点 TagsView 等跳转） |

移动端：不用悬停；继续用点击 `LunaMenuReopen` 展开 + 遮罩点击收起；路由变化仍自动收起。

## 实现要点

### 1. 新增 composable [`luna-ui/src/layout/luna/useLunaAutoCollapse.ts`](../luna-ui/src/layout/luna/useLunaAutoCollapse.ts)

集中管理：

- `onMounted` → 默认折叠
- `watch(route.fullPath)` → 折叠（`flush: 'post'`）
- `expandOnHover` / `scheduleCollapse` / `cancelCollapse`（timer）
- `setMorePanelOpen(open: boolean)`：更多面板打开时取消待折叠、并跳过 leave 折叠
- `isMobile` 时 `scheduleCollapse` / hover expand 直接 no-op（展开仍靠点击）

### 2. 改 [`LunaLayout.vue`](../luna-ui/src/layout/luna/LunaLayout.vue)

- `setup` 里调用 `useLunaAutoCollapse()`
- `provide(LUNA_AUTO_COLLAPSE_KEY, api)` 给子组件（`LunaMenuReopen` / `LunaToolPanel`）用
- **将 api 作为参数传入** `useRenderLunaLayout(autoCollapse)`（不可在同组件 render 里 inject 自己的 provide）

### 3. 改 [`useRenderLunaLayout.tsx`](../luna-ui/src/layout/luna/useRenderLunaLayout.tsx)

侧栏根节点绑定：

- `onMouseenter` → `cancelCollapse`
- `onMouseleave` → `scheduleCollapse`

### 4. 改 [`LunaMenuReopen.vue`](../luna-ui/src/layout/luna/LunaMenuReopen.vue)

- `@mouseenter` → `expandOnHover`（桌面）
- 保留 `@click` → 展开（移动端 / 兜底）

### 5. 改 [`LunaToolPanel.vue`](../luna-ui/src/layout/luna/LunaToolPanel.vue)

- `ElPopover` 增加 `onShow` / `onHide` 通知 `setMorePanelOpen`
- `teleported={false}`，弹出层留在侧栏 DOM 内

## 已知问题与修复

| 问题 | 原因 | 修复 |
|------|------|------|
| 鼠标移出菜单不收起 | `inject` 写在 `LunaLayout` 自己的 render 中，拿不到同组件 `provide` | `useRenderLunaLayout(autoCollapse)` 参数直传 |

## 不改动

- 不改全局 `appStore` 默认 `collapse`
- 不改共享 `Menu.vue`（用路由 watch 覆盖菜单点击）

## 验证

- 登录进 `/index`：侧栏默认隐藏，仅见左侧展开条
- 鼠标悬停展开条：侧栏展开；移出内容区：自动收起
- 展开后点业务菜单：跳转后侧栏收起
- 打开「更多」并在面板内操作：侧栏不因离开菜单区而关掉；关闭面板后再移出可收起
- 窄屏/移动：悬停不触发；点展开条展开，点遮罩或换路由收起
