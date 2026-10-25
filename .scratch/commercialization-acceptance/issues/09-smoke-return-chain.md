# 09: 辅链 7 步 API 自动冒烟（销售退货→质检→入库→应收冲减）

**What to build:** §5.2 退货链路在 08 号主链产出的 COMPLETED 发货单上跑通：行血缘建单、审批占量串行、到货逐卷 RESTOCK/SCRAP 判废、入库金额恒 0、POSTED 写 AR_RETURN_REVERSAL、全废分支自动取消入库单、终态不可逆。直接复用主链发货单，不另建前置数据。

**Blocked by:** 08（spec 5.2 原文"对 5.1 的 COMPLETED 发货单建退货单"，复用主链连续上下文）

**Status:** ready-for-agent

- [ ] 步骤 1：对主链 COMPLETED 发货单建退货单：行血缘强制、卷可选、行级原因、数量+金额双维度；DRAFT；重复卷/超可退量拦截
- [ ] 步骤 2：提交审批→通过（APPROVING 即占量、行锁串行）；自动生成 inType=RETURN 的 DRAFT 入库单，stock_in.source_doc_type/id 回指
- [ ] 步骤 3：到货质检逐卷 RESTOCK/SCRAP 部分判废；RESTOCK 卷必须有单位量（否则 011）；SCRAP 不入库
- [ ] 步骤 4：确认入库——审核写 STOCK_IN 正流水（不出现任何新 txn_type）；退货单 RECEIVED→POSTED；入库金额恒 0
- [ ] 步骤 5：全废分支另一单——入库单明细为空时自动取消 DRAFT 入库单（无流水），退货单停在 RECEIVED
- [ ] 步骤 6：POSTED 核账——finance 写 AR_RETURN_REVERSAL（按原发货汇率折本位额、refItem 回指）；原发货汇率缺失报 037_014 且整事务回滚（造一例）；AR 未核销余额下降、账龄重算
- [ ] 步骤 7：终态守卫——POSTED 不可逆；确认无红冲/退款单/红字发票端点存在
