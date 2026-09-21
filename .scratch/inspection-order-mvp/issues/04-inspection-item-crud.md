# 04: 录检明细独立子资源 CRUD + 卷号查重 + 第一卷转 ACTIVE

**What to build:** 录检明细作为独立子资源（ADR-0014，刻意不一致 ADR-0003 聚合保存模式），有自己的 Controller/Service/Mapper/DO + 4 个 CRUD 端点 + 4 个权限码，逐卷增删改。单内 (vatNo, rollNo) 唯一性由 service 层校验（不建 DB 唯一约束，用户决策）。第一卷明细保存时由后端便利地把主表 DRAFT→ACTIVE（不强约束，仅便利）。本期不做 defects 疵点字段。

**Blocked by:** 02

**Status:** ready-for-agent

- [ ] 明细 DO `InspectionOrderItemDO`（继承 TenantBaseDO，字段见 spec 数据模型表，无 defects）+ Mapper `InspectionOrderItemMapper`（继承 BaseMapperX，分页按 inspectionOrderId 过滤 + 按 id DESC）
- [ ] VO 套装：`InspectionOrderItemSaveReqVO`（含 inspectionOrderId + jakarta.validation）、`InspectionOrderItemPageReqVO`、`InspectionOrderItemRespVO`
- [ ] 独立 Controller `/lunaerp/production/inspection-order-item`：page/get/create/update/delete 5 端点（page 含 get 共 5 个，仿 supplier-contact Controller）
- [ ] create：校验所属检验单存在（不存在报 1_030_010_100）；单内 (vatNo, rollNo) 重复报 1_030_010_004（service 层查同 inspectionOrderId 下同组合）；第一卷时便利调主表 Service 把 DRAFT→ACTIVE（若主表为 DRAFT，非 DRAFT 不动）；返回 id
- [ ] update：校验所属检验单存在；单内唯一排除自身（同 inspectionOrderId 下同组合但 id != 当前）；不存在报 1_030_010_100
- [ ] delete：删除单卷明细
- [ ] 前端 API `api/lunaerp-elmui/production/inspection-order-item/index.ts`；TS 类型
- [ ] 测试覆盖（InspectionOrderItemServiceImplTest）：单内卷号缸号查重（create 重复报 1_030_010_004、update 排除自身）；所属检验单不存在报 1_030_010_100；第一卷便利转 ACTIVE（verify 调主表 Service 更新 status 为 ACTIVE 且仅当原为 DRAFT）；分页按 inspectionOrderId 过滤
