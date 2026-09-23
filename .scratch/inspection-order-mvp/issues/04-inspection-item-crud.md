# 04: 录检明细独立子资源 CRUD + 卷号查重 + 第一卷转 ACTIVE

**What to build:** 录检明细作为独立子资源（ADR-0014，刻意不一致 ADR-0003 聚合保存模式），有自己的 Controller/Service/Mapper/DO + 4 个 CRUD 端点 + 4 个权限码，逐卷增删改。单内 (vatNo, rollNo) 唯一性由 service 层校验（不建 DB 唯一约束，用户决策）。第一卷明细保存时由后端便利地把主表 DRAFT→ACTIVE（不强约束，仅便利）。本期不做 defects 疵点字段。

**Blocked by:** 02

**Status:** ready-for-human（代码实现与单测完成，运行时验收待人工执行，见 Comments）

- [x] 明细 DO `InspectionOrderItemDO`（继承 TenantBaseDO，字段见 spec 数据模型表，无 defects）+ Mapper `InspectionOrderItemMapper`（继承 BaseMapperX，分页按 inspectionOrderId 过滤 + 按 id DESC）
- [x] VO 套装：`InspectionOrderItemSaveReqVO`（含 inspectionOrderId + jakarta.validation）、`InspectionOrderItemPageReqVO`、`InspectionOrderItemRespVO`
- [x] 独立 Controller `/lunaerp/production/inspection-order-item`：page/get/create/update/delete 5 端点（page 含 get 共 5 个，仿 supplier-contact Controller）
- [x] create：校验所属检验单存在（不存在报 1_030_010_100）；单内 (vatNo, rollNo) 重复报 1_030_010_004（service 层查同 inspectionOrderId 下同组合）；第一卷时便利调主表 Service 把 DRAFT→ACTIVE（若主表为 DRAFT，非 DRAFT 不动）；返回 id
- [x] update：校验所属检验单存在；单内唯一排除自身（同 inspectionOrderId 下同组合但 id != 当前）；不存在报 1_030_010_100
- [x] delete：删除单卷明细
- [x] 前端 API `api/lunaerp-elmui/production/inspection-order-item/index.ts`；TS 类型
- [x] 测试覆盖（InspectionOrderItemServiceImplTest）：单内卷号缸号查重（create 重复报 1_030_010_004、update 排除自身）；所属检验单不存在报 1_030_010_100；第一卷便利转 ACTIVE（verify 调主表 Service 更新 status 为 ACTIVE 且仅当原为 DRAFT）；分页按 inspectionOrderId 过滤

## Comments

- 2026-09-23（agent）：代码实现与单元测试全部完成。
  - 数据模型与持久层：落地 `InspectionOrderItemDO`（继承 `TenantBaseDO`，包含 rollNo/vatNo/measuredWeight/lengthM/machineNo/grade/colorName/colorNo/styleNo/productCode/productName/pieceCount/remark，无 defects 字段）与 `InspectionOrderItemMapper`（分页按 inspectionOrderId 过滤且按 id 倒序排序，支持按单内缸号卷号查询与计数）。
  - VO 套装：落地 `InspectionOrderItemSaveReqVO`（含 `@NotNull`、`@NotBlank` 等 validation 约束）、`InspectionOrderItemPageReqVO`、`InspectionOrderItemRespVO`。
  - 业务层与控制层：落地 `InspectionOrderItemService` / `InspectionOrderItemServiceImpl` 与 `InspectionOrderItemController`（`/lunaerp/production/inspection-order-item` 路径下 `create`/`update`/`delete`/`get`/`page` 5 个端点，携带 `lunaerp:production:inspection-order-item:*` 独立权限控制）。
  - 核心业务行为：
    - create：校验所属检验单存在（不存在抛 `1_030_010_100`）；单内 `(vatNo, rollNo)` 查重（重复抛 `1_030_010_004`）；第一卷录入且主表为 DRAFT 时便利调用主表 Service 将状态更新为 ACTIVE（非 DRAFT 状态或非首卷时不流转）；缺省 pieceCount 为 1；返回新增明细 id。
    - update：校验明细及所属检验单存在；单内缸号卷号查重时排除自身；更新时固定原所属检验单 ID 防止越单篡改。
    - delete / get / page：明细不存在报 `1_030_010_100`；支持单卷删除、详情查询及分页查询。
  - 单元测试：`InspectionOrderItemServiceImplTest` 新增 20 例测试（覆盖检验单不存在、缸号卷号单内查重与自身排除、首卷 DRAFT 自动转 ACTIVE 及非 DRAFT 不流转、VO 校验等），全量通过；`lunasrv-module-lunaerp` 全量单测 261 例全部通过。
  - 前端 API：落地 `luna-ui/src/api/lunaerp-elmui/production/inspection-order-item/index.ts`，导出明细 TS 类型与 5 个 CRUD API 函数，并通过 ESLint 检查。
  - 双轴 Code Review（Standards & Spec）已执行，已修复父单据 ID 防篡改问题。
