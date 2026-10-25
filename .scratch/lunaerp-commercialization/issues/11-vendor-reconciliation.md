# 11-vendor-reconciliation: 供应商对账/付款申请

Type: grilling
Status: resolved
Blocked by: 01, 02, 04, 17

## Question

设计供应商应付闭环并产出 spec（W3），grilling 时先调 Skill "grilling" 与 "domain-modeling"，并先读 17 号研究结论：

1. 承织/承染加工对账：取数口径 = 检验确认数量 × 加工单价（现有 inspection-confirmation 的确认数量衔接）；
2. 纱线采购对账：对接纱线采购单（yarn-order-mvp）的到货入库数量；
3. 对账单生成与确认状态；
4. 付款申请单：走 02 号审批链（Q14-d）；审批通过后与资金账户支付的联动（付款登记 vs 直接支付，需决定）；
5. 预付/暂扣（质保金）规则；
6. 打印与审批日志（走 04 号通用件）。

产出：`.scratch/vendor-reconciliation/spec.md`（按统一 DoD）。

## Answer

两轮 grilling（10 问）全部锁定，spec 落 `.scratch/vendor-reconciliation/spec.md`，术语回填 `docs/contexts/finance/GLOSSARY.md`。核心决策：

1. **取数口径事实修正 + 常量化**：承织/承染加工对账取数源 = **COMPLETED 检验单**（自带承检方）按单汇总录检明细 Σ实称重量/Σ米数，全等级计入、劣品扣款走调整项行；计价基准 GREIGE（织费）缺省按米、FINISHED（染费）缺省按公斤、逐行可改。偏离老系统 StockIn 取数源的理由记录于 spec Problem Statement。口径收敛唯一取数服务，防重复取数靠源指纹（被有效对账单引用即出候选、作废即释放）。
2. **加工单价缺口**：生产单无单价字段（胚布单只有总额 processing_fee、染色单没有），一期对账行手工填单价、产品档案加工费仅预填参考；价格主档二期。不改生产域 MVP。
3. **纱线采购对账**：入库单加第 5 个可选上游引用"纱线采购单"（ID+快照零回写，仿现有四类）；对账聚合仍按供应商维度 PURCHASE 入库明细（不强制挂单），退货入库不进候选、走调整项行。
4. **对账单三态** DRAFT→CONFIRMED→CANCELLED：行表即快照（零回写）、确认冻结、可打印 A4 套打；**不落已申请/已付金额字段**（实时聚合，杜绝老系统 paidAmount 占位 P0）；三类行 = 加工费行（一张检验单一行）/采购货款行（一条入库明细一行）/调整项行（手工正负+必填原因）。
5. **付款两步制**：付款申请（RECON 对账付款挂 CONFIRMED 对账单、累计申请≤确认额条件更新防并发超申；PREPAY 预付付款挂供应商+用途）走 02 号审批链，APPROVED 的 ApprovalHandler 空实现不过账；**付款登记**由出纳显式执行（选资金账户、可部分付款、一次核销多申请、可用预付抵扣），同事务四写（付款单+核销行/账户流水/OUT_SUPPLIER 分录）。
6. **预付余额独立表达**：实时聚合两条 SUM（ΣPREPAY 核销 − Σ预付抵扣），不复用"余额转负"；质保金实体不做（非目标），用调整项行+实付<申请额表达。
7. **一期仅人民币**，外币三金额列由 ledger 全域设计统一承载（10/12 号）。
8. 错误码 `1-030-022`（独占 000–007）+ `1-030-023`（payment 共享段先占 000–007，10 号避让）；字典 2269–2271；菜单 10730/10740 按钮段；回收站声明 VENDOR_RECON/PAYMENT_APPLY 两 bizType（仅 DRAFT）；与 10/12 号共享件冲突清单（8 项）在 spec Further Notes 显式列出，合并时收口。
