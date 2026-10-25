# 04: 收款单 — 草稿/提交/审批过账同事务（含自动分配核销）

**What to build:** 收款单 PENDING 草稿可改可删；提交进 02 号审批引擎（biz_type=CUSTOMER_RECEIPT）；APPROVED 事件 handler 同事务过账：资金账户余额条件更新 + RECEIPT/ADVANCE 入账分录 + 核销行落库 + status=COMPLETED。收款单=头+拟核销分配数组，三视角选单（CONFIRMED 对账单/发货单/合同，底层拆到发货明细分录）+ 预收余额作可分配来源；「自动分配」按发货日 FIFO 凑平可改可删。守卫：严格同币种、单行核销≤该分录未核销余额、合计≤收款额、过账条件更新防重复核销。

**Blocked by:** 02 域公共内核 — LedgerService + AccountService + 发货事件 AR 记账

**Status:** closed

- [x] `controller.finance.payment` + `service.finance.payment`：`/payment` create/update/delete（仅 approval_status=PENDING）/submit
- [x] 提交校验：分配合计≤收款额、逐行≤未核销余额（试算口径）、严格同币种（RECEIPT_CURRENCY_MISMATCH）
- [x] `CustomerReceiptApprovalHandler` 监听 `ApprovalCompletedEvent`（BEFORE_COMMIT）：同事务四写——账户 `current_balance += amt` 条件更新 + RECEIPT/ADVANCE 入账分录 + 核销行 payment_ref（ref_type=AR_ENTRY，ref_id=发货明细/运费分录 id）落库 + status=COMPLETED；REJECTED/CANCELLED 无副作用
- [x] `POST /allocate-preview`：按发货日 FIFO 自动分配试算，不落库
- [x] `GET /ar-balance?customerId&currency`：未核销明细分录列表（三视角选单数据源，含运费分录）
- [x] `GET /account/simple-list`：收款选账户下拉（仅 ENABLE）
- [x] 核销行未核销余额口径：分录 original_amount − Σ AR_INC_REVERSAL − Σ 已核销 allocate_amount − Σ AR_RETURN_REVERSAL
- [x] RC 取号（RC-{yyyyMMdd}-{3位日流水}，ADR-0004 软删行扫描+撞号重试+可手填）
- [x] 错误码 023 段 IN 侧 100–107（100 RECEIPT_NOT_EXISTS / 101 RECEIPT_STATUS_NOT_ALLOW / 102 RECEIPT_CUSTOMER_MISMATCH / 103 RECEIPT_CURRENCY_MISMATCH / 104 RECEIPT_ALLOCATE_EXCEED / 105 RECEIPT_POST_CONFLICT / 106 RECEIPT_ADVANCE_BALANCE_NOT_ENOUGH / 107 RECEIPT_REVERSAL_NOT_ALLOW）
- [x] 前端 `finance/payment/` 收款 Tab：草稿编辑页（客户/账户/币种/金额/收款日/经手人 + 核销分配表格 + 自动分配按钮 + 三视角选单弹窗）、列表
- [x] 测试：审批过账同事务四写断言；余额不足/账户停用拒绝；同币种守卫；分配合计≤收款额；FIFO 自动分配
