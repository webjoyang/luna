# 10: 采购链 7 步 API 自动冒烟（纱线采购→入库→齐套→领纱）

**What to build:** §5.3 采购链路独立重建验收库跑通：纱线档案与 BOM 配比、采购单 ACTIVE 量进入齐套在途近似口径、YARN_SPEC 入库流水、齐套试算两缺口与手算一致、领纱 requestId 幂等、余额不足拦截与幂等键释放、撤销发纱回库，并验证跨租户不可见。

**Blocked by:** 07

**Status:** ready-for-agent

- [ ] 步骤 1：纱线档案+规格、产品 BOM 配比行、纱线采购单 DRAFT→ACTIVE；编号/明细正确；ACTIVE 量进入齐套"在途纱线"近似口径
- [ ] 步骤 2：纱线入库单（引用采购单，可选）仓主管审核——itemType=YARN_SPEC 的 STOCK_IN 正流水、按规格余额可查、供应商对账候选可取该明细
- [ ] 步骤 3：采购单 COMPLETED 状态正确（不自动写台账，2026-09-21 已回退口径）
- [ ] 步骤 4：织造单齐套试算——需求=计划重量×ratio；可用=台账正负流水分仓库 SUM（含 YARN_OUT 扣减）；在途=ACTIVE 采购量；两缺口/建议量与手算一致；跨租户织造单/库存/采购不可见
- [ ] 步骤 5：领纱单测算→生成→H5/PC 发纱 dispatch（带 requestId）——同 requestId 连发两次只产生一条 YARN_OUT、库存只扣一次、dispatchedQty 正确；推导 PARTIAL→DISPATCHED
- [ ] 步骤 6：余额不足发纱返回 015-311 拦截；幂等键释放后可重试成功
- [ ] 步骤 7：撤销发纱 reverse-dispatch——YARN_OUT 红冲回库、dispatchedQty 回退
