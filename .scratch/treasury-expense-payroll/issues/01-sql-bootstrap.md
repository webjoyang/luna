# 01: SQL 基建（9 新表 + 2 处 ALTER 扩列 + 字典 2282–2287/2274 追加 + 菜单 10715/10750/10760 + 错误码 020/024/025 段）

**What to build:** 交付 3 个可重复执行的 PostgreSQL 脚本后，资金/报销/工资域全部数据结构就位：`lunaerp_finance_account` 扩 3 列（account_no/account_nature/remark）、`lunaerp_finance_ledger_entry` 扩 2 列（party_name/ref_no）；新建转账单/其他收支单/报销主从/工资 5 表共 9 张表与全部索引；字典 2282–2287 落地、2274 分录类别追加 6 值；菜单 10715 收支总览 + 10710/10750/10760 三段按钮；020 续占（105–110）与 024/025 两段错误码全部登记。金额 `numeric(18,2)`、汇率 `numeric(20,8)`、数量 `numeric(18,2)`，全部继承 TenantBaseDO 标准列；脚本仿现有 lunaerp-* 体例，空库与存量库重跑均幂等、零报错、零数据覆盖。

**Blocked by:** None（可立即开工；执行前提：10 号 customer-reconciliation 的域公共 3 表 ledger/account/exchange_rate SQL 已先行执行）

**Status:** ready-for-agent

- [ ] `lunaerp-finance-account-ext.sql`：account `ADD COLUMN IF NOT EXISTS` account_no varchar(64)/account_nature varchar(16)/remark varchar(500) + COMMENT（account_no 标注 @Sensitive(CONTACT)）；ledger 扩 party_name varchar(64)/ref_no varchar(32) + COMMENT（索引沿用 10 号，不加）；transfer/other_txn 建表——transfer_no/txn_no 租户内唯一含软删行（ADR-0006 模式）、`(tenant_id, transfer_date)`、`(tenant_id, txn_date)` 索引；transfer 含双账户 id/name 快照列与 status COMPLETED 固定，other_txn 含 direction/category/currency/original_amount/exchange_rate/base_amount
- [ ] `lunaerp-finance-expense.sql`：expense/item 建表——expense_no 唯一含软删行、attachments jsonb（infra URL 有序数组）、`(tenant_id, applicant_user_id, approval_status)`、`(tenant_id, account_id)` 索引；行表 expense_id 外键语义、category/original_amount/occur_date/remark
- [ ] `lunaerp-finance-payroll.sql`：payroll/payroll_employee/payroll_line_item/payroll_defect_ref/payroll_payment 5 张表——payroll_no/payment_no 唯一含软删行；`(tenant_id, period)` 部分唯一索引 `WHERE deleted=0 AND approval_status <> 'CANCELLED'`；employee `(tenant_id, payroll_id, user_id)` 唯一；line_item `(tenant_id, payroll_id, employee_row_id)`；defect_ref `(tenant_id, line_item_id)` 与 `(tenant_id, confirmation_id)` 均普通索引（作废释放语义，不建唯一）；payment `(tenant_id, payroll_id)`、period 列
- [ ] 字典：2274 幂等追加 dict_data——OUT_EXPENSE/OUT_SALARY/TRANSFER_IN/TRANSFER_OUT/IN_OTHER/OUT_OTHER（避让现有占用）；2282 账户性质（PUBLIC 公户 primary/PRIVATE 私户 warning，缺省 PUBLIC）、2283 账户状态（ENABLE success/DISABLE info，对齐 10 号账户状态值）、2284 报销费用类型 8 值、2285 工资收入项 6 值（含 PIECE 计件）、2286 工资扣款项（DEFECT danger/OTHER）、2287 其他收支类别 6 值；币种复用 `lunaerp_sales_contract_currency` 不新建；审批态复用 02 号字典
- [ ] 菜单：10715 收支总览（perm `finance:overview:query`，component `lunaerp-elmui/finance/overview/index`，sort 2）；10710 资金账户按钮 10711 新增/10712 编辑/10713 删除/10716 账户间转账(:transfer)/10717 其他收支(:other-txn)；10750 报销按钮 10751 新建/10752 编辑/10753 提交(:submit)/10754 删除/10755 打印；10760 工资按钮 10761–10768（含 10764 导入次品(:import-defect)/10765 发放登记(:pay)/10766 作废）；均先 DELETE 再 INSERT；10780 sort 校正为 9 的说明写入脚本注释（以 10 号 SQL 交付为准）
- [ ] 错误码段：1-030-020 续占 105–110、1-030-024（000–007）、1-030-025（000–012）登记进错误码段位总表与 GLOSSARY 错误码节，文案含占位符（025_005 双占位符：确认记录/工资单号）；复用码（020_100–104/200–202、023_004、030_030_001）不重复登记
- [ ] 空库与存量库各重跑两次验证幂等、零报错、零数据覆盖
