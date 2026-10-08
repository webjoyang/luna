# 09: FIFO 账龄（汇总 + 下钻）

**What to build:** 账龄汇总页按客户一行（合计 / 五桶 0–30 / 31–60 / 61–90 / 91–180 / 181+ / 预收余额 / 其中已逾期），可下钻未核销明细。FIFO 消耗：所有应收减少分录（RECEIPT_WRITE_OFF / ADVANCE_WRITE_OFF / AR_RETURN_REVERSAL）按发生时间从最老 AR_INC 消耗；按客户独立计算不跨客户互抵。逾期=发货日+credit_days<查询日（未设 credit_days 不判逾期）。预收余额单列不进桶。纯函数实时计算不落快照表。金额 @Sensitive(AMOUNT)。

**Blocked by:** 05 收款单 — 红冲 + 预收转核销 + 并发核销专测

**Status:** ready-for-agent

- [ ] `GET /aging/summary?agingDate&customerIds&salesmanId`：按客户一行，含合计/五桶/预收/已逾期
- [ ] `GET /aging/detail?customerId&agingDate`：下钻该客户未核销明细分录
- [ ] FIFO 纯函数：应收减少分录按发生时间从最老 AR_INC 逐笔消耗，余额对应发货日算账龄
- [ ] 五桶：0–30 / 31–60 / 61–90 / 91–180 / 181+；账龄=查询日−发货日
- [ ] 逾期判定：发货日 + credit_days < 查询日；credit_days 空值不判逾期
- [ ] 预收余额（ADVANCE 入账 − ADVANCE_WRITE_OFF）单列不进桶
- [ ] 金额字段 @Sensitive(AMOUNT)
- [ ] 前端：账龄汇总页（客户一行，五桶柱状/表格，可下钻）、明细页
- [ ] 测试：FIFO 消耗顺序（跨月多笔收款/冲减）；五桶边界值（30/31/60/61/90/91/180/181）；跨客户不互抵；credit_days 逾期判定（空值不判逾期）；预收单列不进桶
