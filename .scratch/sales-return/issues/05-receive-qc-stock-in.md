# 05: 到货质检事务 — 入库重写/审核/全废取消

**What to build:** 仓管对已审批退货单执行「到货质检」：逐卷判定 RESTOCK（可二次入库）或 SCRAP（废品），给 RESTOCK 卷分配退货仓库下库位（可空）；确认后系统按行重算 restock_qty/scrap_qty，重写入库单仅留 RESTOCK 卷明细并审核（库存台账写标准 STOCK_IN 正流水，不出现新 txn_type）；全部卷判废时同事务自动取消 DRAFT 入库单（不留库存账），退货单仍进入 RECEIVED。入库单已被人工提前审核/取消时拒绝质检。

**Blocked by:** 04 (审批通过 Handler — 生成退货入库单)

**Status:** ready-for-agent

- [ ] `POST /receive`（body: id + rolls[{id, qcResult, locationId, qcRemark}]）：守卫 status=APPROVED、入库单存在且仍为 DRAFT（否则 009）、每卷 qc_result 必录（010）、RESTOCK 卷库位须属于退货仓库（可空）、RESTOCK 卷行单位对应量非空（011）
- [ ] 重算各行 restock_qty：KG 行取 RESTOCK 卷 Σweight，M 行取 Σlength_m；scrap_qty=卷合计同口径 − restock_qty
- [ ] 组装入库单：每个 RESTOCK 卷一条入库明细（itemType=PRODUCT、rollNo、quantity=行单位对应量、pieceCount=1、unit_price/amount=0）+ 每卷一条 placement（locationId 可空）；SCRAP 卷不产生明细/placement
- [ ] 全废分支：入库单明细为空 → 调 stock-in DRAFT 取消（置 CANCELLED，无流水）；部分/全可入分支：整单替换明细后调 stock-in 审核（写 STOCK_IN 正流水）
- [ ] 落每卷 qc_by/qc_at/qc_remark/location 快照；退货单 status APPROVED→RECEIVED、arrived_by/at 落库
- [ ] 库存动作失败整事务回滚；台账写入类型断言为 STOCK_IN，不得出现 RETURN_IN
- [ ] 前端：到货质检弹窗（卷清单逐行下拉判定、批量"全部可入库/全部废品"、RESTOCK 卷库位选择器按退货仓库过滤），提交即不可逆
