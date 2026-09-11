# Luna 后台布局改动归档总览（2026-09-12）

本目录归档本次会话中与 **登录后后台 LunaLayout** 相关的全部方案与落地说明（不含官网 `PublicHome` 迁移本身，官网见 [`PublicHome-官网首页迁移总结.md`](./PublicHome-官网首页迁移总结.md)）。

---

## 文档索引

| 文档 | 说明 |
|------|------|
| [LunaLayout-后台布局改动归档.md](./LunaLayout-后台布局改动归档.md) | **总归档**：背景、分阶段改动、最终文件清单、行为、踩坑 |
| [LunaLayout-后台布局替换总结.md](./LunaLayout-后台布局替换总结.md) | 第一阶段：全局壳替换总结（已同步后续增量） |
| [lunalayout_shell_362746b5.plan.md](./lunalayout_shell_362746b5.plan.md) | 计划副本：LunaLayout 全局替换 |
| [luna_menu_auto_collapse_295ee8e4.plan.md](./luna_menu_auto_collapse_295ee8e4.plan.md) | 计划副本：菜单自动折叠 |
| [首页迁移需求.md](./首页迁移需求.md) | 原始需求摘录（含 Layout 调整条目） |

Cursor 原件路径（仅本机）：

- `C:\Users\Luna\.cursor\plans\lunalayout_shell_362746b5.plan.md`
- `C:\Users\Luna\.cursor\plans\luna_menu_auto_collapse_295ee8e4.plan.md`

---

## 改动阶段一览

1. **LunaLayout 全局替换**：去掉 Logo / 顶栏；ToolHeader → 侧栏「更多」；折叠全隐；清空 `/index`
2. **交互微调**：折叠图标上移到菜单上方（仅图标）；「更多」内容竖排并补文字
3. **自动折叠**：默认收起 + 悬停展开/移出收起 + 路由变化收起
4. **Bug 修复**：移出菜单不收起（provide/inject 同组件不可见 → 参数直传）
