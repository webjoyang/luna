# 0001 — lunaerp-elmui 作为先行快速实现版与 lunaerp 正式 UI 分目录

## 状态

accepted

## 上下文与决定

客户信息模块的前端视图先行落到 `luna-ui/src/views/lunaerp-elmui/` 与 `luna-ui/src/api/lunaerp-elmui/`，与既有的 `views/lunaerp/`（产品/坯布品种等已迁移视图）平级。**`lunaerp-elmui/` 是一个临时快速实现版**：用 Element Plus 快速搭出业务核心，目的是先跑通前后端服务与关键流程。**未来 lunaerp 业务视图的正式 UI 仍然落到默认目录 `views/lunaerp/`**，`lunaerp-elmui/` 只承担"先行验证"的角色。

## 理由

仓库原本只有 `views/lunaerp/`，产品/坯布品种等视图已经按 `views/lunaerp/baseinfo/<feature>/` 落地，这是 lunaerp 的正式 UI 默认落点。客户模块本次刻意拆出 `lunaerp-elmui/`（"elmui" = Element Plus UI），定位为先行快速实现版：

1. **快速验证优先**：客户信息是首次落地的业务核心模块，重点是用 Element Plus 标准组件最快搭出能跑的 CRUD + 工作台，验证后端服务、字典、权限、菜单注册等链路通畅，UI 精致度让位于速度。
2. **不污染正式 UI 落点**：正式 UI 仍以 `views/lunaerp/` 为默认目录（产品/坯布品种已在此）。临时实现放 `lunaerp-elmui/`，等业务核心跑通后再以正式设计重写到 `views/lunaerp/baseinfo/customer/`，到时再删除 `lunaerp-elmui/customer/`。
3. **命名明确语义**：`elmui` 后缀直接说明"这是 Element Plus 临时实现"，避免后续工程师误把它当成 lunaerp 视图的主落点。

## 考虑过的备选

- **直接落 `views/lunaerp/baseinfo/customer/`**：跳过临时实现版，一步到位写正式 UI。被否：客户模块业务链路（工作台/联系人子资源/字典/菜单 SQL）首次跑通有不确定性，先用快速版验证更稳。
- **独立子应用 `luna-ui/apps/lunaerp-elmui/`**：物理隔离更彻底，但需要新构建配置/路由懒加载分包，成本远超目录级拆分；快速实现版不必要。

## 后果

- 路由 name 与菜单 `component_name` 需带 `LunaerpElmui` 前缀（如 `LunaerpElmuiBaseinfoCustomer`），与未来正式版的 `LunaerpBaseinfoCustomer` 区分。
- 菜单 `component` 字段路径需写 `lunaerp-elmui/baseinfo/customer/index`，框架按此解析。
- **未来迁移**：业务核心跑通、正式 UI 落到 `views/lunaerp/baseinfo/customer/` 后，需批量改路由 name、菜单 component 路径、并删除 `lunaerp-elmui/customer/` 目录。迁移成本可控但不是零。
- 后续其它 lunaerp 新模块若也走"先行快速实现"路径，可复用 `lunaerp-elmui/` 目录；正式版仍落 `views/lunaerp/`。
