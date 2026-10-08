# 17: 领纱单列表 page + 详情 get + 仓库/余额查询 + 领纱 Tab 列表

**What to build:** 落成移动领纱的列表、详情与辅助查询端点及领纱 Tab。GET `/lunaerp/h5/workshop/requisition/page`（领纱单→织造单→机台任务 EXISTS `worker_user_id=登录人` 去重；状态默认 ACTIVE/PARTIAL；DISPATCHED/CLOSED/DRAFT 不列；`@PreAuthorize lunaerp:workshop:h5:dispatch`）。GET `/lunaerp/h5/workshop/requisition/get`（id；归属校验同上不符 039-006；返回单头+逐行规格名/需求/已发/单位+默认仓库 id）。GET `/lunaerp/h5/workshop/warehouse/simple-list`（启用中仓库 id+name H5 精简 VO，供默认仓库缺失时手选）。GET `/lunaerp/h5/workshop/yarn-balance`（warehouseId, yarnSpecId；复用既有余额查询 service 与 PC 端点同源）。前端 `src/views/h5/requisition.vue`（Tab 领纱：我的待领纱单列表）+ `src/views/h5/requisition/detail.vue` 骨架（逐行填量、改仓/查余额入口占位）。可 demo：待领纱单只列自己任务→进单看详情。

**Blocked by:** 06（H5Layout 已就位）

**Status:** ready-for-agent

- [ ] GET `/lunaerp/h5/workshop/requisition/page`（领纱单→织造单→机台任务 EXISTS `worker_user_id=登录人` 去重；状态默认 ACTIVE/PARTIAL；DISPATCHED/CLOSED/DRAFT 不列）
- [ ] GET `/lunaerp/h5/workshop/requisition/get`（id；归属校验同上不符 039-006；返回单头+逐行规格名/需求/已发/单位+默认仓库 id）
- [ ] GET `/lunaerp/h5/workshop/warehouse/simple-list`（启用中仓库 id+name H5 精简 VO）
- [ ] GET `/lunaerp/h5/workshop/yarn-balance`（warehouseId, yarnSpecId；复用既有余额查询 service 与 PC 端点同源）
- [ ] `@PreAuthorize lunaerp:workshop:h5:dispatch`
- [ ] 前端 `src/views/h5/requisition.vue`（Tab 领纱：我的待领纱单列表）
- [ ] 前端 `src/views/h5/requisition/detail.vue` 骨架（逐行填量、改仓/查余额入口占位，18 填充发纱表单）
- [ ] 测试：横向越权操作 B 的领纱单 → 039-006；纵向越权无 dispatch 权限调端点 → 403；租户隔离跨租户构造 id → 查无
- [ ] demo：待领纱单只列自己任务→进单看详情
