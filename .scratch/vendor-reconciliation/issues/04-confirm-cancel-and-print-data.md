# 04: 对账单确认/作废守卫 + 打印数据接口

**What to build:** 财务主管对草稿对账单执行确认（界面二次确认），单据与行数据当场冻结成快照凭证——此后检验单/入库单再怎么改，对账单详情原样不变，可打印、可生成付款申请；已确认单可作废（二次确认），作废后它占用的源明细释放回候选池，但若已存在审批中或已审批的付款申请则拒绝作废并说明。同时交付 A4 套打所需的 print-data 接口（金额豁免脱敏）。

**Blocked by:** 03（草稿链路）

**Status:** resolved

- [x] `PUT /confirm`：条件更新 `WHERE status='DRAFT'` 防并发双确认（影响行数 0 报 001）；total_amount 为 0 或无有效行拒绝确认；写 confirmed_by/confirmed_at
- [x] 冻结语义：CONFIRMED 后 update/re-fetch/delete 全部拒绝（001）；详情返回行表快照值，源检验单/入库单事后修改不影响返回内容（行表即快照、零回写的验证用例）
- [x] `PUT /cancel`：CONFIRMED→CANCELLED 条件更新；存在挂靠且 approval_status ∈ {PENDING, APPROVING, APPROVED} 的付款申请时拒绝报 006（REJECTED/CANCELLED 申请不阻断）；写作废人/时间留痕；CANCELLED 为终态不可复活，其指纹在后续取数中自动释放
- [x] `GET /print-data?id=`：遵循 04 号通用件 print-data 契约，bizType=VENDOR_RECON；出参抬头/供应商/区间/三类行/合计/确认信息；打印 VO 豁免金额脱敏（列表与业务 VO 仍脱敏）
- [x] 确认/作废均 @LogRecord；权限码 :confirm/:print 生效
- [x] Mockito 单测：并发双确认只成功一次、确认后冻结、作废守卫的申请状态矩阵（5 态各一）、作废后指纹回到候选、终态单再作废/确认报 001

## 交付说明
1. **接口**：在 `SupplierReconController` 交付 `PUT /confirm`、`PUT /cancel`、`GET /print-data` 接口并绑定权限码 `lunaerp:finance:supplierrecon:confirm` 和 `lunaerp:finance:supplierrecon:print`。
2. **打印 VO**：新建 `SupplierReconPrintRespVO` 与 `SupplierReconPrintItemRespVO`，涵盖抬头、对账区间、三类明细行快照及确认作废人等字段，豁免 `@Sensitive` 金额脱敏。
3. **确认/作废业务与防并发**：在 `SupplierReconService` / `SupplierReconServiceImpl` 中实现 `confirmRecon`、`cancelRecon` 和 `getPrintData`。在 `SupplierReconMapper` 及 XML 中实现带租户与前置状态条件的 `updateToConfirmed` 与 `updateToCancelled`，防止并发冲突；作废前通过 `PaymentApplyMapper.selectListByReconId` 校验挂靠付款申请状态，存在 `PENDING`/`APPROVING`/`APPROVED` 状态时报错 006 阻断，已驳回或已撤回申请不阻断；确认与作废方法添加 `@LogRecord` 日志注解。
4. **单元测试**：在 `SupplierReconServiceImplTest` 补充 9 个测试用例，覆盖并发双确认竞争、零金额/空行拒绝、确认后冻结拦截、付款申请审批状态矩阵（5态+无申请）、指纹作废释放、终态单再次操作拦截以及打印数据组装。全部 29 个单测执行通过。
