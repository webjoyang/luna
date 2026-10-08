# ADR-0042：销售退货库存回冲复用入库单——通用台账不新增 RETURN_IN 类型

**状态**：Accepted
**日期**：2026-10-07
**关联**：wayfinder 13-sales-return grilling（Q1/Q9）；ADR-0016（入库过账状态机）、ADR-0017（通用库存流水台账）、ADR-0018（两级库位）、ADR-0029（发货出库过账）、ADR-0038（回收站/红冲边界）；spec `.scratch/sales-return/spec.md`

## 背景

W3 客户销售退货要把客户退回的布匹回补库存。13 号 grilling 面对一个枚举设计问题：通用库存台账 `lunaerp_stock_ledger.txn_type` 目前按"业务动作成对"扩展（`STOCK_IN`/`STOCK_IN_REVERSAL`、`SHIPMENT_OUT`/`SHIPMENT_OUT_REVERSAL`、`YARN_OUT`/`YARN_OUT_REVERSAL`、`PROCESS_OUT`/`PROCESS_OUT_REVERSAL`），退货正数流水是新增 `RETURN_IN` 类型，还是复用既有入库通道？

三条已查清的事实：

1. ADR-0017 明确：正数库存流水**只由入库单审核写入**（决策 3"显式审核驱动"），库位放置展开（决策 4）、红冲按 related_doc 反查取反都是入库单语义；ADR"未来扩展"一节列举的是出库/调拨/盘点等**新动作**，退货入库不是新动作。
2. 入库类型字典 `lunaerp_stock_in_type`（dict 2234）在建库之初就预留了 `RETURN=退货入库`（dict_data 3597），仓管工作台、库位放置（placement）、入库审核事务、红冲取消全部现成。
3. 老系统（linaagi）退货财务确认时调 `createReturnStockIn` 生成的就是一张 `type='RETURN'` 的**入库单**（COMPLETED），而非绕过入库单直写台账；其 P0（空血缘生成 0 明细入库单、钱冲了货没进）是血缘缺失问题，不是通道选择问题。

## 决策

客户销售退货的库存回冲**复用入库单**，通用台账不新增 `RETURN_IN`（或任何退货专属）txn_type：

1. 退货审批通过（02 号 ApprovalHandler，同事务）自动生成一张 `in_type=RETURN` 的 **DRAFT** 入库单，明细/卷携带发货血缘，`lunaerp_stock_in` 以新增通用来源列 `source_doc_type='SALES_RETURN'` + `source_doc_id=退货单ID` 1:1 回指（部分唯一索引兜底）。
2. 仓管到货、逐卷质检判定（RESTOCK 可二次入库 / SCRAP 废品）、分配库位后，由退货模块调用 stock-in 既有服务**审核**该入库单：RESTOCK 卷写标准 `STOCK_IN` 正流水（每条 placement 一条，沿用 ADR-0017 决策 4）；SCRAP 卷不产生明细与流水。
3. 全部卷判废时，同事务把仍为 DRAFT 的入库单取消（DRAFT 取消不写流水），退货流程照常 RECEIVED→POSTED——货不入库，钱照冲。
4. 退货单作废（仅 DRAFT/APPROVED 可作废）：APPROVED 态级联取消 DRAFT 入库单（无流水可红冲）；RECEIVED/POSTED 后一期不提供撤销，红冲体系二期单独立项。
5. 入库明细 `unit_price/amount` 恒为 0：退货库存成本价（成本卡/加权成本）属二期成本核算，一期台账只表达数量。

## 理由

1. **不破坏唯一正数写入口**：ADR-0017 的核心约束是"流水只由入库单审核（正数）与红冲取消（负数）写入"。为退货单开 `RETURN_IN` 直写入口，会让库位放置展开、红冲反查、台账余额口径出现第二套实现，未来调拨/盘点/委外退货都有借口绕过入库单。
2. **入库类型语义已预留**：RETURN 是入库单自己的分型维度（为什么入库），txn_type 是台账动作维度（入/出/红冲方向）。"退货导致的入库"用 `in_type=RETURN + txn_type=STOCK_IN` 两个正交维度表达，比再造一个方向类型更准确。
3. **实物控制点不能省**：退货有"审批通过→货到→质检→上架"的时间差。DRAFT 入库单天然承载这个时间差与仓管显式审核（ADR-0017 决策 3 的理由同样适用：不能审批即自动入账），库位分配直接复用 placement，零新增库存交互。
4. **来源列是通用增量**：`source_doc_type/source_doc_id` 不绑死退货，二期纱线采购退货（退供应商入库侧）及未来其他来源入库同样复用，避免逐场景加外键列。
5. **全废分支不制造假库存**：废品卷不入库、入库单自动取消，库存余额永远等于可售库存；同时退货单与取消单各自留痕，审计链不断。

## 考虑过的备选

- **新增 `RETURN_IN` 台账正类型，退货财务确认时直写**：老系统资金侧的直觉做法。但会成为入库单之外的第二个正数写入口，绕过 placement 与显式审核，且红冲要再发明 `RETURN_IN_REVERSAL` 与反查规则。否决。
- **审批通过即生成 COMPLETED 入库单（货到即入账）**：老系统做法。剥夺仓管"实物到货+质检后才入账"的确认环节，客户退货存在少件、废品、裁剪退回等常态，自动入账造成账实不符。否决（用 DRAFT + 到货审核替代）。
- **SCRAP 卷入废品仓/废品库位**：需要客户维护废品仓，一单混合质检要拆两张入库单；且台账余额聚合不含库位（ADR-0017 决策 5），废品混在同仓会污染可售库存口径。一期否决，废品仅登记判定，废品仓二期与成本损失一起做。
- **退货入库带销售单价作为库存金额**：销售价不是库存成本，会污染未来成本核算；入库金额留空（0），成本结转二期。否决。

## 后果

- `lunaerp_stock_ledger.txn_type` 一期取值保持 `STOCK_IN/STOCK_IN_REVERSAL/SHIPMENT_OUT/SHIPMENT_OUT_REVERSAL/YARN_OUT/YARN_OUT_REVERSAL/PROCESS_OUT/PROCESS_OUT_REVERSAL`，退货不产生新枚举值；台账分页"退货入库"以 `related_doc_type=SALES_RETURN` 或入库单 in_type=RETURN 识别。
- `lunaerp_stock_in` 增加两个可空通用来源列与部分唯一索引；手工入库单两列为空，存量数据与脚本零影响。
- 二期纱线采购退货沿用本 ADR 通道（in_type=RETURN + 来源列），其应付冲减另按 11 号供应商侧模型设计；过账后退货红冲单（钱货双向反向）二期统一设计，本 ADR 不预留半成品状态。
- 若未来引入成本核算，退货入库单需补成本价字段/分录，届时变更集中在入库单与成本模块，不影响本 ADR 的通道选择。
