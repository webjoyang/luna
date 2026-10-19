# 06: 财务确认过账 + 应收冲减事件

**What to build:** 财务对已收货退货单执行「财务确认」，系统按原发货入账汇率把逐行退货金额追加为应收反向分录（客户往来余额立即减少），退货单变为已过账终态 POSTED。已过账单不可修改、不可作废。有 RESTOCK 卷时入库单必须已 COMPLETED；查不到原发货汇率时报错阻断。全废单（无 RESTOCK 卷）可正常过账。

**Blocked by:** 05 (到货质检事务)

**Status:** closed

- [x] `POST /post?id=`：条件更新 `WHERE status='RECEIVED'` 防并发双过账；守卫：存在 RESTOCK 卷时入库单必须 COMPLETED（否则 012）
- [x] 同事务 publish `SalesReturnPostedEvent`（BEFORE_COMMIT），载荷：returnId/returnNo/customerId/currency/returnDate/逐行 `{shipmentItemId, amount 原币}`
- [x] finance listener（本票内独立测试，实现由 10 号 session 承接）调 `LedgerService.recordArReturnReversal(...)`：category=AR_RETURN_REVERSAL、refType=SALES_RETURN、refId=退货单 id、原币+原发货入账汇率+本位额
- [x] 原发货汇率查不到报 014，整事务回滚；listener 抛错退货过账回滚
- [x] 落 posted_by/at；POSTED 终态，无反向事件、无红冲端点
- [x] POSTED 态后端拒绝 update/delete/cancel/receive 等所有写操作（写账凭证不可改）
- [x] 前端：列表已收货状态显示「财务确认」按钮，过账后变终态只读；详情页过账人与时间留痕

