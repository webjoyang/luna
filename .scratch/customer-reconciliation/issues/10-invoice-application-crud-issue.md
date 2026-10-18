# 10: 发票申请 — 创建/提交/登记/驳回

**What to build:** 多选 COMPLETED 发货单（同客户同币种，禁混选）创建发票申请，快照客户开票四要素（创建时从客户档案带出可改）；支持部分开票（行带「本次开票金额」，默认=该单未开票余额），守卫同一发货单累计开票（ISSUED+SUBMITTED 在途聚合）≤ total_amount+freight_fee，条件更新防并发超开，REJECTED 释放占用。状态机 DRAFT→SUBMITTED→ISSUED（登记回填 20 位发票号）/REJECTED（可改重提）；税额服务端反算（不含税额/税额/价税合计）。开票≠收款，发票不参与核销与账龄。不接审批引擎，登记回填发票号即财务复核点（独立 `:issue` 权限+日志）。

**Blocked by:** 01 SQL 基建 + 错误码常量 + 客户档案+5列

**Status:** closed

- [x] `controller.finance.invoice` + `service.finance.invoice`：`/invoice` page/get/create/update/delete（DRAFT/REJECTED 可改重提）；`GET /candidate-shipments` 候选发货单与未开票余额查询
- [x] `PUT /submit?id=`：DRAFT/REJECTED→SUBMITTED
- [x] `PUT /issue?id=`（`:issue` 权限）：SUBMITTED→ISSUED，回填 official_invoice_no（20 位必填）/ invoice_date / attachment_url，记录 issue_by/issue_time + @LogRecord
- [x] `PUT /reject?id=`（可空原因）：SUBMITTED→REJECTED，释放该单发货行开票占用
- [x] 回收站处理器：`InvoiceApplicationRecycleBinHandler` 注册（bizType=INVOICE_APPLICATION，仅 DRAFT 进回收站，恢复级联复活明细）
- [x] 创建守卫：多选发货单同客户同币种（INVOICE_CURRENCY_MIXED / INVOICE_CUSTOMER_MISMATCH）；快照客户开票四要素（invoice_title/tax_no=统一社会信用代码快照/address/bank_info，可改）
- [x] 超开守卫：Σ(该发货单全部 SUBMITTED+ISSUED 申请行 amount) + 本次 ≤ shipment_amount（整数分，条件更新防并发超开 INVOICE_EXCEED_SHIPMENT_AMOUNT，REJECTED 释放占用）
- [x] 税额服务端反算：total_amount_excl_tax / total_tax / total_amount_incl_tax 按税率反算；字典 2276（13/9/6/3/1/0/免税），纺织默认 13%、外贸 0%
- [x] 发票号唯一：official_invoice_no 唯一索引（非空时，含软删行），20 位必填
- [x] IA 取号（IA{yyyy}{4位流水}）
- [x] 前端 `finance/invoice-application/`：列表、创建（多选发货单弹窗 + 部分开票金额 + 税率选择 + 抬头四要素可改）、登记回填弹窗
- [x] 测试：超开守卫（跨申请聚合含 SUBMITTED 在途、REJECTED 释放）；币种混选拒绝；税额反算（金额逻辑必有测试）；发票号唯一；抬头快照带出与可改；状态机流转
