# 03: 纱线成本计算纯函数 + 单元测试

**What to build:** 从旧系统 `product-yarn-cost.ts` 迁移 `calculateProductYarnCost` 纯函数到新系统前端。函数接收纱线行数组 + 成本设置对象，输出成本汇总（ratioTotal/missingPriceCount/yarnCostPerKg/yarnCostPerMeter/processingCost/totalCost/kgPerMeter/metersPerKg/basis）。支持三种模式：针织（basis=KG，总成本=纱线成本/KG + 针织加工费/KG）、梭织简易估算（basis=METER，每米重量=克重×门幅/100000，总成本=纱线成本/M + 织造费 + 其他加工费）、梭织经纬精算（basis=METER，按经纱/纬纱分组，每米用量×单价/(1-损耗率)）。损耗口径：投入损耗=净耗用÷(1-损耗率)，损耗率超 100% 钳制为 99.9%。金额精度 4 位小数。包含完整单元测试覆盖空数组、全零、多纱线、含损耗、无价格、超 100% 损耗钳制等边界。

**Blocked by:** None (can start immediately)

**Status:** done

- [x] calculateProductYarnCost 纯函数迁移完成，不依赖 Vue 响应式
- [x] 针织模式计算正确：纱线成本=Σ(单价×比例%/(1-损耗率))，总成本=纱线成本+针织加工费
- [x] 梭织简易估算正确：每米重量=克重×门幅/100000，纱线成本=每米重量×混纺单价
- [x] 梭织经纬精算正确：按经纱/纬纱分组，纱线成本=Σ(每米用量×单价/(1-损耗率))
- [x] 损耗率超 100% 钳制为 99.9%，负值钳制为 0
- [x] 金额精度 4 位小数（Math.round(value×10000)/10000）
- [x] missingPriceCount 正确统计未设置价格的纱线数量
- [x] 单元测试覆盖：空数组、全零值、多纱线含损耗、无价格、超 100% 损耗、经纬精算经纱/纬纱分组
