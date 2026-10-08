# 07: 回收站草稿单据 handlers（7 类 DRAFT 单据）

**What to build:** 7 类草稿单据（销售合同/胚布单/染色单/纱线采购单/入库单/织造单/发货单，均仅 DRAFT 状态进站）误删后能在回收站恢复为 DRAFT 且明细行完整回来；引用的主数据仍在回收站时阻断并提示先恢复。在 05 号 SPI 框架基础上落地 7 个草稿单据 handler，恢复为 DRAFT（草稿从未记账，恢复不补任何库存/账务），级联子表按现有聚合关系父→子顺序批量 `deleted=0`（织造单含机台任务/布票/领纱单及明细，参考现有 `WeavingOrderCascadeDeleteListener` 级联范围做逆向）。引用的主数据仍在回收站报 002。已审核/已生效单据永不进站（红冲取消是唯一路径，单据保留为 CANCELLED）。前端 7 个新 tab 接入。

**Blocked by:** 05

**Status:** ready-for-agent

- [ ] SALES_CONTRACT handler（DRAFT，含合同明细级联复活）
- [ ] GREIGE_ORDER handler（DRAFT，含颜色行级联）
- [ ] DYEING_ORDER handler（DRAFT，含颜色明细级联）
- [ ] YARN_PURCHASE_ORDER handler（DRAFT，含采购明细级联）
- [ ] STOCK_IN handler（DRAFT，含明细/放置记录级联）
- [ ] WEAVING_ORDER handler（DRAFT，含机台任务/布票/领纱单及明细级联——参考 `WeavingOrderCascadeDeleteListener` 逆向）
- [ ] SHIPMENT handler（DRAFT，含发货明细/卷清单级联）
- [ ] 7 个 handler 均走手写 XML、恢复 DRAFT、级联父→子、引用阻断（主数据在站报 002）、幂等（仅 deleted=1 可恢复报 001）、`@DataPermission(enable=false)`（CUSTOMER/SALES_CONTRACT/SHIPMENT 等 21 号票数据范围系统侧调用）、`@LogRecord`
- [ ] 已审核/已生效单据回收站不可见（红冲取消是唯一路径，单据保留 CANCELLED）
- [ ] 前端回收站页新增 7 个 tab
- [ ] 测试：各 handler 级联复活完整（父→子明细行全回来）、引用阻断按依赖链列出对象名称、幂等、并发条件更新、租户隔离、横向越权；织造单级联范围最复杂专测
- [ ] W1–W4 新模块 spec 统一 DoD 增"声明 RecycleBinHandler（红冲类除外）"条目（本 ticket 仅登记契约，落地随各模块 ticket）
