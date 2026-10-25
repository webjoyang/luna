# 12: 账实零差异专项 checker（库存台账对账 + 经营分录 append-only 实证）

**What to build:** G5 的自动化部分：四条链跑完后，一条对账命令证明库存余额与单据累计逐键相等、红冲/退货/发纱符号正确；经营分录无任何 update/delete 端点、直改抛 LEDGER_IMMUTABLE、物化账户余额在重跑收/付款后仍等于流水聚合；产出 0 差异对账报告作为签字附件。

**Blocked by:** 08, 09, 10, 11

**Status:** ready-for-agent

- [ ] 库存对账：按"产品/纱线规格+单位+仓库"键，`lunaerp_stock_ledger` 实时 SUM 余额与各业务单据累计量逐键相等；红冲/退货/发纱符号正确（STOCK_IN/SHIPMENT_OUT/YARN_OUT 及其红冲）
- [ ] 流水模型核对：台账只存流水、余额=实时 SUM，不存在余额物化旁路写入口
- [ ] 经营分录 append-only 实证：`lunaerp_finance_ledger_entry` 无 update/delete 端点（存在即报 P1）；尝试直改抛 LEDGER_IMMUTABLE
- [ ] 账户余额=条件更新物化值：重跑收款/付款后余额仍等于流水聚合；转账双账户一增一减守恒
- [ ] checker 纳入 `luna-srv/script/acceptance/smoke/`，输出可归档 Markdown 对账报告；任一差异 nonzero 退出失败并定位到键与差额
