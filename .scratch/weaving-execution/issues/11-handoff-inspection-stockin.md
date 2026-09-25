# 11: 织造完工交接胚布检验（find-or-create 匹装表头）+ 检验生成胚布入库单

**What to build:** ①织造单工作台"交接检验"动作（ADR-0025）：收集本单 COMPLETED 有效票，按胚布单 ID 归一 find-or-create 检验单（type=GREIGE、**packageMode=PIECE**，带产品/合同/规格快照），无完工票拒绝；重复交接幂等复用、返回完工票范围。检验工位在该单逐匹产生明细：pieceNo=布票号、weavingTicketId 回填、machineNo=actualMachine、重量/米数实测；明细有结果后同事务把布票回写 INSPECTED（ADR-0024）。②检验单 COMPLETED 后"生成胚布入库单"显式按钮（独立权限）：从 grade≠DEFECTIVE 的明细生成 type=PRODUCTION 入库单**草稿**（选入库仓库，rollNo/pieceNo/batchNo 带出），幂等跳过已生成明细；仓管在入库单侧补量/删行后按 ADR-0016 审核过账，过账同事务把布票回写 STOCKED；红冲取消不自动回退票态（由人工按需处理，规则实现时在 ADR-0024 框架内补充说明）。不复活旧"同步入库"服务，检验单不加 SYNCED 态。

**Blocked by:** 07, 10

**Status:** ready-for-agent

- [ ] 交接检验：find-or-create PIECE 表头（幂等）
- [ ] 逐匹明细产生与 weavingTicketId 血缘；布票回写 INSPECTED
- [ ] 生成胚布入库单草稿（等级过滤、幂等、选仓）
- [ ] 入库过账回写 STOCKED；红冲语义明确
- [ ] 前端：交接按钮/提示、检验匹装接单、生成入库按钮
- [ ] 单测：幂等交接、血缘回写、生成与过账联调
