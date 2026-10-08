# 07: 打印 / 回收站 / 脱敏 / 操作日志 + 详情页

**What to build:** 非作废退货单可打印 A4 退货单（抬头+客户+原发货单号+行表+卷清单+质检结果+合计+签章区）；草稿进回收站可恢复；列表/详情金额对无权限同事脱敏（打印 VO 豁免）；关键动作有操作日志。详情页展示头/行/卷只读、入库单号可跳转、全链路时间线（提交/审批/到货/过账）。

**Blocked by:** 06 (财务确认过账 + 应收冲减事件)

**Status:** ready-for-agent

- [ ] 打印：注册模板 `SALES_RETURN_DOC`（A4，bizType SALES_RETURN）到 04 模板注册表；`GET /print-data?id=` 返回全量字段（客户联系人/金额不脱敏）；打印接口加 `@LogRecord(type="打印")`
- [ ] 回收站：注册 `RecycleBinHandler(bizType=SALES_RETURN)`，仅收 DRAFT（approval_status=PENDING/REJECTED），恢复回 DRAFT 并按父→子顺序复活行/卷；CANCELLED 永不进站；恢复不补账
- [ ] 脱敏：列表/详情 VO 的 unit_price/amount/total_amount 标 `@Sensitive(amount)`（W0 amount 权限位）；print-data VO 豁免
- [ ] 操作日志：submit/withdraw/receive/post/cancel 动作均 `@LogRecord`；审批操作日志走 02 引擎记录表
- [ ] 前端：详情页（头/行/卷只读 + 入库单号可点跳转 stock-in 工作台 + 时间线：提交/审批/到货/过账人与时间 + 打印按钮）；列表金额千分位展示
- [ ] 打印范围：非 CANCELLED 均可打印（草稿水印由模板呈现）
