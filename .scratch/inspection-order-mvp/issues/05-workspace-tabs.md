# 05: 检验单工作台 3 Tab（基本信息 + 录检明细 + 操作日志占位）

**What to build:** 工作台页 `workspace/index.vue` 落地 3 Tab 容器：① 基本信息 Tab（02 票已建主表表单与状态操作按钮）② 录检明细 Tab（04 票已建独立子资源 CRUD 表格）③ 操作日志 Tab（占位"开发中"，后续接操作日志模块）。同步入库按钮放录检明细 Tab 但默认禁用（ADR-0013 占位）。只读模式（COMPLETED/CANCELLED）全部控件 disabled。

**Blocked by:** 02, 04

**Status:** ready-for-agent

- [ ] 工作台页 `views/lunaerp-elmui/production/inspection-order/workspace/index.vue`：el-tabs 容器，3 Tab
- [ ] 基本信息 Tab：复用 02 票主表表单与状态操作按钮；按状态控制可编辑性（DRAFT 可编辑、ACTIVE 主表只读但录检明细可编辑、COMPLETED/CANCELLED 全只读）
- [ ] 录检明细 Tab：复用 04 票独立子资源 CRUD 表格；表格列含卷号/缸号/实称重量/米数/机台号/等级(字典下拉)/颜色/色号/款号/匹数/备注/操作；底部"添加明细"打开新增表单（单卷新增）；行内编辑或弹窗编辑；仅 DRAFT/ACTIVE 可编辑
- [ ] 同步入库按钮：放录检明细 Tab，权限码 `:sync`（10264 菜单已注册），前端默认禁用 + tooltip "功能开发中"（ADR-0013）
- [ ] 操作日志 Tab：占位"开发中"（el-empty + 描述文字）
- [ ] 工作台 URL 直接访问（/production/inspection-order/workspace/:id?）正常工作（无 id 新建、有 id 查看编辑）
- [ ] 列表行"查看/编辑"跳转到工作台
