# 04: 审批通过 Handler — 生成退货入库单

**What to build:** 退货单审批通过时，`SalesReturnApprovalHandler` 同事务自动生成一张 `inType=RETURN` 的 DRAFT 入库单（明细按"全部可入库"预生成、金额恒 0、source 回指 SALES_RETURN+退货单 id、仓库取退货单头），并回写退货单 `stock_in_id`；驳回不生成任何单据。已审批（APPROVED）退货单作废时同事务取消该 DRAFT 入库单（不写库存流水）。审批中心三视图与审批记录全部复用 02 号引擎。

**Blocked by:** 03 (提交审批 + 撤回 + 作废(DRAFT))

**Status:** closed

- [x] `SalesReturnApprovalHandler`（biz_type=sales_return）实现 02 号 `ApprovalHandler` 契约：APPROVED 分支幂等校验无 stock_in_id，建 DRAFT 入库单并回写；REJECTED 分支不建单
- [x] 入库单组装：inType=RETURN（复用 2234 字典既有值）、source_doc_type=SALES_RETURN、source_doc_id=退货单 id、warehouse_id=退货单头退货仓库、明细按退货行+卷预生成（每条卷一条明细，itemType=PRODUCT、产品快照取退货行、rollNo、quantity=卷量、pieceCount=1、unit_price=0、amount=0）
- [x] handler 失败则审批事件同事务回滚（02 契约）；重复 APPROVED 事件不重建入库单（幂等）
- [x] `POST /cancel?id=`（APPROVED 分支）：守卫入库单仍为 DRAFT（被人工审核/取消报 009），同事务调 stock-in 取消 DRAFT 入库单（不写红冲流水），退货单置 CANCELLED
- [x] 作废守卫：APPROVING 须先撤回，RECEIVED/POSTED 拒绝作废（002），CANCELLED 终态
- [x] 退货单 status DRAFT→APPROVED、approval_status=APPROVED 在 handler 内同事务落
- [x] 前端：审批中心（待我审批/我已审批/我发起的）走 02 通用页面，退货单详情回显入库单号与状态
