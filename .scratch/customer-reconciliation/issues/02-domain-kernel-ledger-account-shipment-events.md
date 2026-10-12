# 02: 域公共内核 — LedgerService + AccountService + 发货事件 AR 记账

**What to build:** 发货单审核出库（DRAFT→COMPLETED）同事务在经营分录账追加 AR_INC 分录（逐明细行一条 + 运费头级行一条，refType=SHIPMENT_ITEM/SHIPMENT_FREIGHT）；发货红冲（COMPLETED→CANCELLED）追加 AR_INC_REVERSAL（reversal_of_id 回指原分录）。分录账 append-only，应用层禁改禁删（LEDGER_IMMUTABLE）。资金账户 opening_balance 仅创建时可写，current_balance 仅经单据过账条件更新改变，账户启用/停用两态。汇率取价=生效日≤业务日的最新一条 ENABLE，RMB 内置 rate=1 不建行。business 侧发 Spring 事件，finance 监听记账，business 不反向依赖 finance。

**Blocked by:** 01 SQL 基建 + 错误码常量 + 客户档案+5列

**Status:** completed

- [x] `service.finance.ledger.LedgerService.append(LedgerEntryDO)` 唯一写入通道；无 update/delete 端点，应用层抛 1_030_020_000 LEDGER_IMMUTABLE
- [x] `service.finance.account.AccountService`：create（opening_balance 仅此可写）、update（仅备注/状态）、无删除（有分录引用报 ACCOUNT_HAS_ENTRIES_DELETE_FORBIDDEN）；余额变更唯一通道 `UPDATE ... SET current_balance = current_balance + #{amt} WHERE id=#{id} AND status='ENABLE'`
- [x] `service.finance.exchangerate.ExchangeRateService.getRate(currency, bizDate)`：生效日≤业务日最新 ENABLE，RMB 内置 rate=1，无则抛 EXCHANGE_RATE_NOT_FOUND
- [x] business.shipment 审核事务内 publish `ShipmentAuditedEvent`（BEFORE_COMMIT 同事务语义，载荷含 shipmentId/shipmentNo/customerId/currency/shipmentDate/freightFee + 明细行快照数组）
- [x] business.shipment 红冲事务内 publish `ShipmentCancelledEvent`（载荷含原分录回指所需行）
- [x] finance listener 同事务写 AR_INC（逐明细行 + freightFee>0 时运费一条，按发货日生效汇率折本位币）/ AR_INC_REVERSAL（reversal_of_id 回指）；记账失败同事务回滚发货审核
- [x] 事件载荷用 business 自有 DTO，business 不 import finance 任何类
- [x] 测试：发货审核→逐行 AR_INC+运费分录断言（金额/汇率/本位额/ref 回指）；红冲→AR_INC_REVERSAL+reversal_of_id；分录不可变守卫；账户余额条件更新；汇率取价（最新≤业务日、停用剔除、RMB=1、缺汇率报错）
