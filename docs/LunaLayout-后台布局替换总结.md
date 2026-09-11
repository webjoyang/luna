# LunaLayout 后台布局替换总结（2026-09-12）

> 完整分阶段归档见：[LunaLayout-后台布局改动归档.md](./LunaLayout-后台布局改动归档.md)  
> 索引：[README-Luna布局归档.md](./README-Luna布局归档.md)

## 一句话定位

将登录后后台全局外壳从原 `Layout.vue` 切换为新壳 `LunaLayout`：去掉 Logo 与顶部 ToolHeader，侧栏折叠时完全隐藏，原 ToolHeader 能力收纳到侧栏底部「更多」弹出框；并支持自动折叠（默认收起 / 悬停展开 / 移出与路由收起）；`/index` 工作台内容清空为空白占位。原 `Layout` 源码保留作回退参考，但不接入路由。

计划存档：

- 壳替换：[`docs/lunalayout_shell_362746b5.plan.md`](./lunalayout_shell_362746b5.plan.md)
- 自动折叠：[`docs/luna_menu_auto_collapse_295ee8e4.plan.md`](./luna_menu_auto_collapse_295ee8e4.plan.md)

---

## 已确认决策

| 项 | 结论 |
|---|---|
| 范围 | 全局替换：静态路由 + `generateRoute` 动态路由统一走 LunaLayout |
| ToolHeader | 侧栏底部「更多/设置」按钮 → 弹出面板承载原能力（竖排 + 文字） |
| 折叠 | `collapse === true` 时侧栏宽度为 0（完全隐藏），内容区全宽 |
| 自动折叠 | 默认收起；悬停展开条展开；移出延时收起；路由变化收起 |
| Footer | 不做专门处理，继续复用 `AppView.vue` |
| 布局模式 | 首期只实现 classic 竖侧栏 |

---

## 架构

```text
router component: Layout（符号名不变）
  → LunaLayout.vue
    → useLunaAutoCollapse + provide（给子组件）
    → useRenderLunaLayout(autoCollapse)  // 参数直传，勿同组件 inject
      → 侧栏: Collapse图标 + Menu + LunaToolPanel
      → 内容区: TagsView + AppView
折叠时 → LunaMenuReopen（悬停/点击展开）
```

接入点：[`luna-ui/src/utils/routerHelper.ts`](../luna-ui/src/utils/routerHelper.ts)

```ts
export const Layout = () => import('@/layout/luna/LunaLayout.vue')
```

---

## 文件清单

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
| `luna-ui/src/utils/routerHelper.ts` | `Layout` 懒加载指向 `LunaLayout.vue` |
| `luna-ui/src/views/Home/Index.vue` | 清空内容，保留 `defineOptions({ name: 'Index' })` |

### 未改（刻意保留）

- `luna-ui/src/layout/Layout.vue`
- `luna-ui/src/layout/components/useRenderLayout.tsx`
- `luna-ui/src/layout/components/ToolHeader.vue`
- `luna-ui/src/layout/components/AppView.vue`

---

## 行为说明

### 侧栏

- **展开**：菜单上方仅图标折叠钮 + `Menu` + 底部「更多」
- **折叠**：完全隐藏；`LunaMenuReopen` 悬停/点击可展开
- **更多面板**：竖排；纯图标项带文字；打开时不触发自动收起
- **自动收起**：鼠标离开侧栏延时收起；路由变化立即收起

### `/index`

- 空占位；登录仍跳转 `/index`

---

## 与官网首页的关系

| 路径 | 组件 | 外壳 | 说明 |
|---|---|---|---|
| `/` | `PublicHome/Index.vue` | 无 | 免登录官网 |
| `/index` | `Home/Index.vue`（空） | **LunaLayout** | 登录后默认首页 |
| 其它后台业务路由 | 各 `views/...` | **LunaLayout** | 统一新壳 |

---

## 验证清单

- [x] 登录后 `/index`：空内容、无 Logo、无顶栏
- [x] 更多竖排 + 文字；折叠图标在菜单上方
- [x] 默认收起 / 悬停展开 / 移出收起 / 路由收起
- [x] 移出菜单可正常隐藏（API 参数直传修复后）
