# 05: 算价纯函数 CostCardPricingCalculator + 单测（prefactor）

**What to build:** 落 `service/business/costcard/pricing/CostCardPricingCalculator.java` 纯函数（Spring 无状态组件或静态纯函数类），入参为纱线行/费用行/设置值对象，出参对齐历史 TS `product-yarn-cost.ts` 结果集并扩展双口径费用/税额。BigDecimal 运算，**金额一律 4 位小数四舍五入**（HALF_UP），比率保留 6 位；损耗率负值钳 0、≥100% 钳 99.9%（与前端 TS 函数一致）。覆盖三模式（与 spec「算价规则」节一致）：

1. 每米重量 `kgPerMeter = gsm × widthCm / 100000`
2. 投入损耗 `毛额 = 净额 ÷ (1 − 损耗率)`，纱线与费用同口径
3. 纱线成本（basis）：
   - 针织 KG：`yarnCostPerKg = Σ 损耗(price × ratio/100)`
   - 梭织简易 METER：`blendedPricePerKg = Σ 损耗(price × ratio/100)`，`yarnCostPerMeter = kgPerMeter × blendedPricePerKg`
   - 梭织经纬精算 METER：按 WARP/WEFT 行独立累加 `Σ 损耗(price × consumptionKgPerMeter)`
   - 纱线另一口径换算：针织 `yarnCostPerMeter = yarnCostPerKg × kgPerMeter`（缺克重/门幅 null）；梭织 `yarnCostPerKg = yarnCostPerMeter ÷ kgPerMeter`（kgPerMeter=0 报 `1_030_034_005` COST_CARD_KG_METER_CONVERSION_MISSING）
4. 费用行毛额 `gross = price ÷ (1 − lossRate/100)`；归一：PER_KG 行贡献/kg=gross、PER_METER 行贡献/m=gross；跨口径换算用 kgPerMeter，条件不满足报 005
5. 汇总：`单位成本(不含税) = 纱线成本 + Σ 费用行贡献`（包装/运费均为费用行参与合计）；`含税 = 不含税 × (1 + taxRate)`
6. 报价行折币 `costUnitPrice = costUnitPriceRmb ÷ exchangeRate`（RMB 汇率 1）
7. 毛利反算 `unitPrice = costUnitPrice ÷ (1 − targetMarginRate)`、反手 `marginRate = 1 − costAmount ÷ amount`、整单 `avgMarginRate = (totalAmount − totalCostAmount) ÷ totalAmount`（totalAmount=0 时 null）

`CostCardPricingCalculatorTest` 纯函数单测先红后绿，与历史 TS 边界用例对照（空数组/全零/多纱线/含损耗/无价格/超 100% 钳制/经纬分组/双口径换算缺失）。本票不交付用户行为，是 06/08 票的金额逻辑地基，刻意单独成票以保证金额逻辑测试在成本卡/报价单 CRUD 落地前已全绿。

**Blocked by:** 02（SQL 基建，确保 VO 形状与 cost_card_yarn schema 一致）

**Status:** ready-for-agent

- [ ] `CostCardPricingCalculator.java` 落 `service/business/costcard/pricing/`，纯函数（无 Spring 上下文依赖、无 Mapper 调用）
- [ ] 三模式公式实现完整（针织 KG / 梭织简易 METER / 梭织经纬精算 METER），与 spec「算价规则」节一致
- [ ] BigDecimal 4 位 HALF_UP；损耗率负值钳 0、≥100% 钳 99.9%
- [ ] 双口径费用行归一与跨口径换算：克重/门幅缺失报 `1_030_034_005`
- [ ] 报价行折币、毛利正反算、整单加权毛利纯函数覆盖
- [ ] `CostCardPricingCalculatorTest` 单测先红后绿，覆盖 spec「Testing Decisions 1」列出的全部边界用例
- [ ] 与历史 TS 边界用例（空数组/全零/多纱线/含损耗/无价格/超 100% 钳制/经纬分组）结果一致
- [ ] 不启 Spring、不连库
