# 03: 检验单列表搜索 + 状态操作 + 删除级联

**What to build:** 列表页搜索条件全量打通（关键词/类型/状态/承检方/日期区间），状态操作端点（complete/cancel），删除端点（仅 DRAFT 可删 + 级联逻辑删除录检明细）。本期状态操作不做严格流转约束（任意态可手动切换，对齐合同/胚布单/染色单 MVP 惯例）。

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] 分页查询：keyword 对 code/product_code/product_name/inspection_factory_name 做 OR 模糊（PostgreSQL LOWER+LIKE）；type/status/inspectionFactoryId/orderDateStart/orderDateEnd 透传到 Mapper；默认 order_date DESC
- [ ] 状态操作端点：`PUT /complete?id=`（DRAFT/ACTIVE→COMPLETED）、`PUT /cancel?id=`（任意→CANCELLED）；状态操作只改 status 字段不改其他业务字段；复用 update 权限
- [ ] 删除端点：仅 DRAFT 可删（非 DRAFT 报 1_030_010_002）；级联逻辑删除录检明细（按 inspection_order_id 批量逻辑删除，调 InspectionOrderItemMapper）；不存在报 1_030_010_000
- [ ] 前端列表页搜索区与表格列全部激活；状态 DictTag 渲染（DRAFT warning / ACTIVE success / COMPLETED info / CANCELLED danger）；类型标签区分（GREIGE 胚布检验 / FINISHED 成品检验）
- [ ] 前端列表操作列：查看/编辑（跳工作台）、删除二次确认（仅 DRAFT 可见删除按钮）；分页组件
- [ ] 测试覆盖：分页条件透传；complete/cancel 状态切换；删除仅 DRAFT；删除级联逻辑删除明细（verify 调明细 Mapper 批量逻辑删除）；不存在报错
