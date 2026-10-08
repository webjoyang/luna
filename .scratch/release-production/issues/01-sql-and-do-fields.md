# 01: SQL 基建 + DO 字段变更（3 表加列 + 10114 按钮权限）

**What to build:** 执行变更脚本后，合同行有「已下达量」列、胚布颜色行与染色颜色行有合同行关联列；销售合同菜单下出现「下达生产」按钮权限。对应后端 DO 字段同步就位。脚本幂等可重复执行。

**Blocked by:** None (can start immediately)

**Status:** ready-for-agent

- [ ] `lunaerp_business_sales_contract_item` 加 `released_qty` numeric(18,2) NOT NULL DEFAULT 0（已下达生产数量）
- [ ] `lunaerp_production_greige_order_color` 加 `contract_line_id` int8 可空、`contract_line_no` int4 可空
- [ ] `lunaerp_production_dyeing_color` 加 `contract_line_id` int8 可空、`contract_line_no` int4 可空
- [ ] 销售合同菜单 10110 下注册按钮权限 10114：`lunaerp:production:release-production:query` 与 `:release`（按钮挂合同菜单、权限码归生产域）
- [ ] 后端 DO 同步加字段：`SalesContractItemDO.releasedQty`、`GreigeOrderColorDO.contractLineId/contractLineNo`、`DyeingColorDO.contractLineId/contractLineNo`
- [ ] ALTER 语句 DO 块判列存在，脚本可重复执行
- [ ] 加列不影响既有数据（released_qty 缺省 0，关联列可空）
