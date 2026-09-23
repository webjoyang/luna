# 05: 检验单工作台 3 Tab（基本信息 + 录检明细 + 操作日志占位）

**What to build:** 工作台页 `workspace/index.vue` 落地 3 Tab 容器：① 基本信息 Tab（02 票已建主表表单与状态操作按钮）② 录检明细 Tab（04 票已建独立子资源 CRUD 表格）③ 操作日志 Tab（占位"开发中"，后续接操作日志模块）。同步入库按钮放录检明细 Tab 但默认禁用（ADR-0013 占位）。只读模式（COMPLETED/CANCELLED）全部控件 disabled。

**Blocked by:** 02, 04

**Status:** ready-for-human（代码实现与构建打包完成，运行时验收待人工执行，见 Comments）

- [x] 工作台页 `views/lunaerp-elmui/production/inspection-order/workspace/index.vue`：el-tabs 容器，3 Tab
- [x] 基本信息 Tab：复用 02 票主表表单与状态操作按钮；按状态控制可编辑性（DRAFT 可编辑、ACTIVE 主表只读但录检明细可编辑、COMPLETED/CANCELLED 全只读）
- [x] 录检明细 Tab：复用 04 票独立子资源 CRUD 表格；表格列含卷号/缸号/实称重量/米数/机台号/等级(字典下拉)/颜色/色号/款号/匹数/备注/操作；底部"添加明细"打开新增表单（单卷新增）；行内编辑或弹窗编辑；仅 DRAFT/ACTIVE 可编辑
- [x] 同步入库按钮：放录检明细 Tab，权限码 `:sync`（10264 菜单已注册），前端默认禁用 + tooltip "功能开发中"（ADR-0013）
- [x] 操作日志 Tab：占位"开发中"（el-empty + 描述文字）
- [x] 工作台 URL 直接访问（/production/inspection-order/workspace/:id?）正常工作（无 id 新建、有 id 查看编辑）
- [x] 列表行"查看/编辑"跳转到工作台

## Comments

- 2026-09-23（agent）：代码实现、静态检查与构建打包全部完成。
  - 工作台容器：在 `views/lunaerp-elmui/production/inspection-order/workspace/index.vue` 落地 3 Tab 完整结构，支持 direct URL 访问与路由参数响应。
  - 基本信息 Tab：`BasicInfoTab.vue` 按单据状态严格控制编辑权限（DRAFT 态全字段可编辑，ACTIVE 态只读展示，COMPLETED/CANCELLED 及 view 模式全部 disabled）。
  - 录检明细 Tab：落地 `ItemsTab.vue`，实现独立子资源逐卷 CRUD；支持 12 列完整字段（卷号、缸号、实称重量、米数、机台号、等级下拉字典、颜色、色号、款号、匹数、备注、操作）；支持弹窗新增与编辑；仅 DRAFT/ACTIVE 态可编辑；首卷录入后触发父容器刷新状态并使主表只读；放置默认禁用状态的"同步入库"按钮，包含 ADR-0013 占位 Tooltip 与 `:sync` 权限控制。
  - 操作日志 Tab：落地 `LogsTab.vue`，以 `el-empty` 占位呈现"操作日志功能建设中（后续接操作日志模块）"。
  - 静态检查与构建：`npx eslint` 检查零警告零错误；`pnpm build:local` 全量编译打包通过（ItemsTab / BasicInfoTab / workspace 均成功生成 chunk）；后端 51 项检验单相关单测全绿通过。
  - 双轴 Code Review（Standards & Spec）执行完成，已消除生命周期重叠与冗余导出。
