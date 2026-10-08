# 09: 后端 H5 包骨架 + my-tasks + 前端报工 Tab 列表

**What to build:** 落成后端 H5 横切接入层骨架与报工列表端点。新包 `com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop`（Controller/VO）+ `service/h5/workshop`（编排 Service），URL 前缀 `/lunaerp/h5/workshop`（网关后完整路径 `/admin-api/lunaerp/h5/workshop/**`）；H5 Service 内强制数据范围（不信任前端传 userId/workerId），写库一律调既有生产域 Service，不复制状态机/库存规则，成功出参用 H5 精简 VO（只含现场需要字段），错误直接抛既有错误码。GET `/my-tasks`（status 可选默认 PENDING/IN_PROGRESS/HALTED、分页；硬过滤 `machine_task.worker_user_id=登录人`；每任务附织造单号/品名快照/计划匹数/actualQty/布票各状态计数；`@PreAuthorize lunaerp:workshop:h5:report`）。前端 `src/views/h5/report.vue`（Tab 报工：大扫码按钮 + 我的机台任务列表，调用 `src/api/h5/workshop/`）。可 demo：登录→看到自己的机台任务列表（只看自己）。

**Blocked by:** 06（H5Layout 与路由守卫已就位）

**Status:** ready-for-agent

- [ ] 后端 `com.luna.lunasrv.module.lunaerp.controller.admin.h5.workshop` 包骨架（Controller/VO）
- [ ] 后端 `service/h5/workshop` 包骨架（编排 Service）
- [ ] URL 前缀 `/lunaerp/h5/workshop`（网关后完整路径 `/admin-api/lunaerp/h5/workshop/**`）
- [ ] H5 Service 内强制数据范围（不信任前端传 userId/workerId）
- [ ] 写库一律调既有生产域 Service，不复制状态机/库存规则
- [ ] 成功出参用 H5 精简 VO（只含现场需要字段，不输出任何成本/内部备注字段）
- [ ] 错误直接抛既有错误码（015 布票/领纱、010 检验、039 H5 自有）
- [ ] GET `/my-tasks`（status 可选默认 PENDING/IN_PROGRESS/HALTED、分页；硬过滤 `worker_user_id=登录人`；每任务附织造单号/品名快照/计划匹数/actualQty/布票各状态计数）
- [ ] `@PreAuthorize lunaerp:workshop:h5:report`
- [ ] 前端 `src/views/h5/report.vue`（Tab 报工：大扫码按钮 + 我的机台任务列表）
- [ ] 前端 `src/api/h5/workshop/` API 模块
- [ ] demo：登录→看到自己的机台任务列表（只看自己）
