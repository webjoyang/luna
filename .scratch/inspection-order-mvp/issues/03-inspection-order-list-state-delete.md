# 03: 检验单列表搜索 + 状态操作 + 删除级联

**What to build:** 列表页搜索条件全量打通（关键词/类型/状态/承检方/日期区间），状态操作端点（complete/cancel），删除端点（仅 DRAFT 可删 + 级联逻辑删除录检明细）。本期状态操作不做严格流转约束（任意态可手动切换，对齐合同/胚布单/染色单 MVP 惯例）。

**Blocked by:** 02

**Status:** ready-for-human（代码实现与单测完成，运行时验收待人工执行，见 Comments）

- [x] 分页查询：keyword 对 code/product_code/product_name/inspection_factory_name 做 OR 模糊（PostgreSQL LOWER+LIKE）；type/status/inspectionFactoryId/orderDateStart/orderDateEnd 透传到 Mapper；默认 order_date DESC；回填 rollCount
- [x] 状态操作端点：`PUT /complete?id=`（DRAFT/ACTIVE→COMPLETED）、`PUT /cancel?id=`（任意→CANCELLED）；状态操作只改 status 字段不改其他业务字段；复用 update 权限
- [x] 删除端点：仅 DRAFT 可删（非 DRAFT 报 1_030_010_002）；级联逻辑删除录检明细（按 inspection_order_id 批量逻辑删除，调 InspectionOrderItemMapper）；不存在报 1_030_010_000
- [x] 前端列表页搜索区与表格列全部激活；状态 DictTag 渲染（DRAFT warning / ACTIVE success / COMPLETED info / CANCELLED danger）；类型标签区分（GREIGE 胚布检验 / FINISHED 成品检验）
- [x] 前端列表操作列：查看/编辑（跳工作台）、删除二次确认（仅 DRAFT 可见删除按钮）；分页组件
- [x] 测试覆盖：分页条件透传；complete/cancel 状态切换；删除仅 DRAFT；删除级联逻辑删除明细（verify 调明细 Mapper 批量逻辑删除）；不存在报错

## Comments

- 2026-09-23（agent）：代码实现与单元测试全部完成。
  - 后端：
    - `InspectionOrderController` 补齐 `complete`（`PUT /complete?id=`）、`cancel`（`PUT /cancel?id=`）、`delete`（`DELETE /delete?id=`）端点及 Swagger 注解与权限控制。
    - `InspectionOrderService` / `InspectionOrderServiceImpl` 落地状态操作与级联删除：
      - `completeInspectionOrder`: 仅 DRAFT/ACTIVE 允许完成，非此状态报 `1_030_010_002`，只修改 `status` 字段为 `COMPLETED`。
      - `cancelInspectionOrder`: 校验单据存在性后，仅修改 `status` 字段为 `CANCELLED`。
      - `deleteInspectionOrder`: 仅 DRAFT 允许删除（非 DRAFT 报 `1_030_010_002`），删除主表并级联逻辑删除明细（调 `InspectionOrderItemMapper.deleteByInspectionOrderId`）。
      - `getInspectionOrderPage` 与 `getInspectionOrder`: 查出明细数量并回填 `rollCount`。
    - 落地 `InspectionOrderItemDO` 与 `InspectionOrderItemMapper`（含批量逻辑删除 `deleteByInspectionOrderId` 与数量查询 `selectCountByInspectionOrderId`）。
  - 前端：
    - 激活 `views/lunaerp-elmui/production/inspection-order/index.vue` 列表页搜索区（关键词、类型、状态、承检方远程搜索、单据日期区间）与 11 列结构。
    - 状态列通过 `DictTag` 渲染（warning/success/info/danger）。
    - 类型列标签区分（GREIGE 胚布检验 warning / FINISHED 成品检验 primary）。
    - 操作列控制：查看（有 query 权限）、编辑（仅 DRAFT/ACTIVE 态展示，有 update 权限）、删除（仅 DRAFT 态展示，带二次确认弹窗，有 delete 权限）。
    - 初始挂载时预加载承检方（供应商）下拉候选项。
  - 测试：
    - `InspectionOrderServiceImplTest` 用例扩充至 30 例，全面覆盖分页条件透传、complete/cancel 状态切换、非合法状态限制、单据不存在报错、删除仅限 DRAFT、删除级联逻辑删除明细子表等场景。
    - `lunasrv-module-lunaerp` 全量单测 261 例全部绿色通过。
    - 前端 ESLint 检查通过，vue-tsc 检查目标文件零错误。

