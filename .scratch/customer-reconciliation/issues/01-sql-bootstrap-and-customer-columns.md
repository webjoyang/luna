# 01: SQL 基建 + 错误码常量 + 客户档案+5列

**What to build:** 执行幂等脚本后，finance 域 5 张新表 + 2 张共享表 IN 侧扩列 + 客户档案 5 列就位；字典 2272–2278、菜单 10721–27/10745–49/10771–74/10780–82、审批 CUSTOMER_RECEIPT 配置写入；`FinanceErrorCodeConstants`（020/021/023/026 段）常量类定义完成；客户表单/详情新增「开票信息」区块，04 号 common-foundation 客户 Excel 导入模板同步 +4 开票列。

**Blocked by:** None (can start immediately)

**Status:** completed

- [x] 新建 `luna-srv/sql/postgresql/lunaerp-finance-customer-recon.sql`，幂等可重复执行（CREATE TABLE IF NOT EXISTS + ADD COLUMN IF NOT EXISTS，单号唯一索引含软删行，菜单先 DELETE 再 INSERT，字典 data id 自 3800 起按 DO 块判占用平移避让）
- [x] 域公共 3 表 DDL：`lunaerp_finance_ledger_entry`（append-only，索引 party+biz_date / ref_type+ref_id / reversal_of_id / account+biz_date）、`lunaerp_finance_account`（opening_balance 仅创建可写、current_balance 条件更新）、`lunaerp_finance_exchange_rate`（唯一索引 tenant+currency+effective_date）
- [x] `lunaerp_finance_statement` + `_item`、`lunaerp_finance_invoice_application` + `_item` DDL
- [x] `lunaerp_finance_payment` / `lunaerp_finance_payment_ref` ADD COLUMN IF NOT EXISTS（IN 侧扩列：receipt_type / currency / exchange_rate / receipt_date / bank_ref_no / handler_name / approval_status；payment_ref 扩 ref_type=AR_ENTRY + entry_no / biz_date / base_amount / ledger_entry_id）
- [x] `lunaerp_baseinfo_customer` ADD COLUMN IF NOT EXISTS 5 列：invoice_title / invoice_address / bank_name / bank_account_no / credit_days
- [x] 字典 type 2272–2278（statement_status / receipt_type / ledger_category 一期 9 值 / invoice_kind / tax_rate / account_type / statement_confirm_type）
- [x] 菜单：10721–10727 对账按钮、10745–10749 收款按钮、10771–10774 发票按钮、**新增 10780 汇率**二级菜单（sort 8, parent 10700, path exchange-rate）+ 10781/10782
- [x] `lunaerp_approval_biz_type` 字典 + CUSTOMER_RECEIPT 默认链配置项
- [x] `enums.finance.FinanceErrorCodeConstants` 常量类：020 段（000 LEDGER_IMMUTABLE / 100–104 account / 200–202 exchange_rate）、021 段（000–007 statement）、023 段 IN 侧 100–107、026 段 000–007 invoice
- [x] 客户表单/详情 +5 字段（开票抬头/开票地址/开户行/开户行账号/信用账期天数，税号用 unified_social_credit_code 不新增列）
- [x] 04 号客户 Excel 导入模板 +4 开票列（credit_days 属业务策略字段不进模板）
- [x] 脚本可重复执行且不影响存量数据（可空列、缺省值）
