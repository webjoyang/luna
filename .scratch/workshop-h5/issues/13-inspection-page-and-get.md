# 13: 检验单列表 page + 详情 get + 质检 Tab 列表

**What to build:** 落成移动质检的列表与详情端点及质检 Tab。GET `/lunaerp/h5/workshop/inspection/page`（状态默认 DRAFT/ACTIVE、type 可选、分页；检验员可看全部租户内可录检单不按机台过滤；返回单号/类型标签/承检方/品名/录检进度；`@PreAuthorize lunaerp:workshop:h5:inspection`）。GET `/lunaerp/h5/workshop/inspection/get`（id；表头+逐行含 photos；仅 DRAFT/ACTIVE 可进，COMPLETED 只读）。前端 `src/views/h5/inspection.vue`（Tab 质检：扫码 + 可录检单列表）+ 检验单详情页骨架（`src/views/h5/inspection/order.vue`，胚布/成品两种模式入口占位）。可 demo：看到可录检单列表→进单看详情（含 photos 行）。

**Blocked by:** 06（H5Layout 已就位）, 04（录检明细 `photos` DO/VO 字段已落用于详情展示）

**Status:** closed

- [x] GET `/lunaerp/h5/workshop/inspection/page`（状态默认 DRAFT/ACTIVE、type 可选、分页；检验员可看全部租户内不按机台过滤；返回单号/类型标签/承检方/品名/录检进度）
- [x] GET `/lunaerp/h5/workshop/inspection/get`（id；表头+逐行含 photos；仅 DRAFT/ACTIVE 可进，COMPLETED 只读）
- [x] `@PreAuthorize lunaerp:workshop:h5:inspection`
- [x] 前端 `src/views/h5/inspection.vue`（Tab 质检：扫码 + 可录检单列表）
- [x] 前端 `src/views/h5/inspection/order.vue` 检验单详情页骨架（胚布/成品两种模式入口占位，14/15/16 填充）
- [x] 测试：纵向越权无 inspection 权限调端点 → 403/鉴权异常；未登录 → 401；租户隔离跨租户构造 id → 查无
- [x] demo：看到可录检单列表→进单看详情（含 photos 行）
