# 02: 检验单主表新建/编辑 + 编号取号 + 关联快照

**What to build:** 在工作台"基本信息"Tab 录入并保存检验单主表。新建时由服务端生成 INS-YYYYMMDD-NNN 编号（按天取号，租户内全量唯一含软删行，ADR-0015），也可手填（撞号报 1_030_010_001）；提供 get-next-no 预览接口。必填类型（GREIGE/FINISHED）与产品，可选承检方/胚布单或染色单/销售合同/目标重量。关联字段均存 ID + 名称快照。新建缺省 DRAFT 状态。仅 DRAFT 态可编辑主表字段。

**Blocked by:** 01

**Status:** ready-for-human（代码实现与单测完成，运行时验收待人工执行，见 Comments）

- [x] 主表 DO `InspectionOrderDO`（继承 TenantBaseDO，字段见 spec 数据模型表）+ Mapper `InspectionOrderMapper`（继承 BaseMapperX，含分页查询、编号取号扫描绕过逻辑删除的手写 SQL，仿 SupplierMapper）
- [x] VO 套装：`InspectionOrderSaveReqVO`（含 jakarta.validation 注解）、`InspectionOrderPageReqVO`、`InspectionOrderRespVO`、`InspectionOrderSimpleRespVO`（列表用，不含明细）
- [x] 错误码常量追加到 `ErrorCodeConstants`：1_030_010_000/001/002/003/004/100
- [x] 编号生成逻辑：`INS-YYYYMMDD-NNN` 按天取号，租户内全量唯一含软删行，查重跳过已用、撞号重试（仿 SupplierServiceImpl 机制，仅格式串不同）；提供 `GET /get-next-no` 只读预览接口
- [x] 创建：status 缺省 DRAFT；code 自动生成或手填校验；关联产品必填（不存在报 1_030_010_003，从产品带出 productCode/productName 快照）；承检方/胚布单/染色单/合同可选（若传则校验存在并保存名称快照）
- [x] 更新：仅 DRAFT 可更新主表，非 DRAFT 报 1_030_010_002；不存在报 1_030_010_000
- [x] 详情 get：返回主表 RespVO（不含明细，明细由 04 票的独立端点查）
- [x] 前端工作台"基本信息"Tab：类型单选/产品远程选择/承检方远程选择/胚布单或染色单远程选择（按 type 切换）/销售合同远程选择/目标重量/单据日期/备注；操作栏按状态显示按钮（草稿态[保存草稿][完成][取消][删除]，录检中态[完成][取消]，其余只读）
- [x] 测试覆盖（InspectionOrderServiceImplTest）：编号取号格式与查重重试、关联校验与快照写入、status 缺省 DRAFT、更新仅 DRAFT、不存在报错

## Comments

- 2026-09-21（agent）：代码实现与单元测试全部完成。
  - VO 套装：新增 `InspectionOrderSaveReqVO`（含 jakarta.validation 注解）与 `InspectionOrderSimpleRespVO`（列表精简用）。
  - 后端 Service/Controller：落地 `createInspectionOrder`（按天生成编号 INS-YYYYMMDD-NNN，查重顺延重试，关联产品/承检方/胚布单/染色单/合同校验与快照写入）、`updateInspectionOrder`（仅 DRAFT 允许修改主表字段、编号变更查重）、`getInspectionOrder`（单据详情）、`getNextInspectionOrderNo`（只读预览下一个单号）。
  - 单测覆盖：`InspectionOrderServiceImplTest` 扩展至 21 个单元测试用例，覆盖自动取号按天流水与扩位、手填查重、撞号有限次重试与失败抛错、产品必填及不存在报错、各关联对象不存在报错、各字段快照回填、非 DRAFT 状态禁止修改、单据不存在报错等，全部绿色通过；`lunasrv-module-lunaerp` 全量单测 232 例全部通过。
  - 前端工作台：落地 `views/lunaerp-elmui/production/inspection-order/workspace/` 下 `BasicInfoTab.vue` 与 `index.vue`。支持 3 Tab（基本信息 / 录检明细占位 / 操作日志占位）；基本信息支持检验类型单选（按类型动态切换胚布单/染色单选择器）、产品远程选择、承检方远程选择、胚布单/染色单远程选择、销售合同远程选择、目标重量、单据日期与备注；操作栏按状态动态展示按钮（草稿态[保存草稿][完成][取消][删除]，录检中态[完成][取消]，其余只读）；只读态全部控件 disabled。
  - 静态检查：前端新增文件通过 vue-tsc 类型检查，零错误。
