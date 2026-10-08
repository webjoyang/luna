# 13-sales-return: 客户销售退货

Type: grilling
Status: resolved
Blocked by: 02, 04, 10, 17

## Question

设计客户销售退货并产出 spec（W3，Q14-c），grilling 时先调 Skill "grilling" 与 "domain-modeling"，并先读 17 号研究结论：

1. 退货单模型：关联发货单/合同行、卷级退货明细、退货原因字典、质检判定（退回即废品还是可二次入库）；
2. 审批：退货走 02 号审批链；
3. 库存回冲：写 ADR-0017 通用台账的何种 type（STOCK_IN_REVERSAL 还是新增 RETURN_IN——需与现有台账 type 枚举核对后定）；退回仓库/库位分配（复用 stock-in placement）；
4. 应收冲减：与 10 号对账/收款核销的关联（红字对账单 or 直接冲减应收余额，需决定）；
5. 边界：一期仅客户销售退货，纱线采购退货二期（写进非目标）；
6. 打印与操作日志（走 04 号通用件）。

产出：`.scratch/sales-return/spec.md`（按统一 DoD）。

## Comments

- 2026-10-07（10 号 customer-reconciliation grilling 定稿时加依赖边）：第 4 点"应收冲减"已在 10 号 spec 冻结为**直接追加反向分录**（不做红字对账单）——退货审批通过 handler 同事务调 `LedgerService.recordArReturnReversal(customerId, shipmentItemId, returnQty, origAmount, currency, 原发货汇率快照, returnOrderId)`（category=AR_RETURN_REVERSAL，按原发货入账汇率折本位额），自动参与下期对账单贷方行与 FIFO 账龄消耗；退货模块不得自建应收台账/余额表。故本票 Blocked by 加 10，该项无需重新决策。
- 2026-10-07（12 号定稿后补）：`recordArReturnReversal` 落点明确为 12 号 `FinancePostingService` 的**往来分录 append 路径**（account_id 为空，只 INSERT 不动 current_balance，不经余额条件更新）；方法签名以本票冻结为准。施工顺序：27 的 finance listener 依赖 28 的 ledger 服务（同批次或 28 先行）。另号段避让：ADR-0042/字典 2279–2281 归本票，12 号 ADR 用 0043、字典顺延 2282–2287。

## Answer

产出：[.scratch/sales-return/spec.md](../../../sales-return/spec.md) + [ADR-0042](../../../docs/adr/0042-sales-return-reuse-stock-in.md)；术语/号源已回填 business/shared GLOSSARY，实现顺序登记为 27（10 号后）。两轮 grilling（Q1–Q10）定稿：

1. **库存回冲（第 3 点）= 复用入库单，不新增台账类型**。核代码确认现有 txn_type 为 STOCK_IN/STOCK_IN_REVERSAL、SHIPMENT_OUT/_REVERSAL、YARN_OUT、PROCESS_OUT 四对，入库类型字典 2234 早预留 RETURN。审批通过生成 `inType=RETURN` 的 DRAFT 入库单（`lunaerp_stock_in` 加通用 source_doc_type/source_doc_id 两列+部分唯一索引，二期采购退货复用），仓管到货质检、复用 placement 分配库位后审核写标准 STOCK_IN 正流水；理由=ADR-0017 唯一正数写入口，理由与备选见 ADR-0042。
2. **状态机**：业务态 DRAFT/APPROVED/RECEIVED/POSTED/CANCELLED + 02 审批五态双列；审批通过只建待入库草稿，仓管到货+质检一步到 RECEIVED，财务独立确认才 POSTED；仅 DRAFT/APPROVED 可作废（APPROVED 级联取消 DRAFT 入库单），过账后红冲一期不做（非目标）。
3. **退货模型（第 1 点）**：严格一单对一 COMPLETED 发货单（客户/合同/币种/仓库快照）；行血缘强制挂发货明细（杜绝老系统空血缘 0 明细入库 P0），卷血缘可选（勾选原卷/手填散退），行级退货原因字典 2280 必填；逐卷质检 RESTOCK/SCRAP——废品**不写库存台账**仅留判定记录，全废自动取消入库单但钱照冲；入库金额恒 0（退货成本价二期）。
4. **防超退**：唯一聚合服务，可退量=发货量−Σ占量（APPROVING 即占量，驳回/撤回/作废释放），数量+金额（≤可退量×原单价）双维度守卫，提交时行锁串行并发；行费卷量软对齐不强拦。
5. **应收冲减（第 4 点）**：遵 10 号冻结契约——POSTED 同事务 business 发 `SalesReturnPostedEvent`（business 不 import finance），finance listener 逐行调 `recordArReturnReversal`（载荷含 customerId/shipmentItemId/returnQty/origAmount/currency/returnOrderId；**汇率参数由 finance 按原发货 AR_INC 分录解析**，business 不读汇率主档，查不到报 037_014 回滚）；不建红字对账单/退款单，已收款余额转贷方归 10 号预收模型；红字税务发票非目标。
6. **审批/打印/日志（第 2、6 点）**：biz_type=SALES_RETURN（02 已预留，本票补字典行+默认链）；A4 套打 SALES_RETURN_DOC 注册 04 内核+独立 print-data；回收站 handler 仅 DRAFT；金额挂 amount 脱敏位；关键动作 @LogRecord。
7. **边界（第 5 点）**：非目标=纱线采购退货（二期，复用本通道）、过账后撤销/退货红冲、红字发票、客户退款/换货、废品仓、退货库存成本价、运费红冲、门户申请/批量导入。

号源：错误码 `1-030-037`（000–014）；字典 2279–2281（避让 10 号 2272–2278），dict_data 3850 块；菜单 10141+按钮 10142–10149（避让报价 10132–10140）；单号 RT-YYYYMMDD-NNN；SQL 两文件（建表脚本 + stock_in 幂等 ALTER，与 11 号 yarn_order ALTER 并行不冲突）。
