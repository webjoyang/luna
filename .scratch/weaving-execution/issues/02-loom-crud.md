# 02: 织机档案 CRUD（轻量机台主数据 + 删除守卫）

**What to build:** 新建 `production.loom` 子包，落 `lunaerp_production_loom` 表与全套 CRUD（分页/详情/新建/更新）。字段：`loomNo`（机台号，租户内唯一含软删行，服务端取号/可手填）、`model`（型号）、`status`（ACTIVE/INACTIVE，不建字典，缺省 ACTIVE）、`remark` + TenantBaseDO。**不提供物理删除**：被机台任务引用时报 1-030-015-202、引导停用；停用织机不可被新机台任务选择，历史快照不变。前端独立管理页 `views/lunaerp-elmui/production/loom/`（表格 CRUD；IoT Tab 在 issue 12）。菜单 10285 + 10286-10288 按钮，权限码 `lunaerp:production:loom:`。建表 SQL：`lunaerp-production-weaving.sql`（与织造核心同文件，见 03）。

**Status:** closed

- [x] 织机表 + CRUD 端点（分页/详情/创建/更新）
- [x] loomNo 租户唯一（含软删）、可手填
- [x] 删除守卫：引用禁删（202）、停用机台不可新排产
- [x] 前端织机管理页 + 菜单/权限注册（10285-10288）
- [x] 单测：唯一、删除守卫、停用过滤
