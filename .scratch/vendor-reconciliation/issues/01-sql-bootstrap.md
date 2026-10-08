# 01: SQL 基建（5 新表 DDL + stock_in 加列 + 字典 2269–2271 + 菜单 10730/10740 + 错误码段）

**What to build:** 交付 3 个可重复执行的 PostgreSQL 脚本后，供应商对账/付款域 5 张新表与索引就位：对账单主表 `lunaerp_finance_supplier_recon` + 行表 `lunaerp_finance_supplier_recon_item`、付款申请 `lunaerp_finance_payment_apply`、共享收付款单 `lunaerp_finance_payment` + 核销子表 `lunaerp_finance_payment_ref`；`lunaerp_stock_in` 增加"纱线采购单"第 5 个可选上游引用列；3 个字典、两段菜单按钮、两个错误码段全部登记。金额一律 `numeric(18,2)`、单价 `numeric(18,4)`、数量 `numeric(18,2)`，仅人民币无币种/汇率列，全部继承 TenantBaseDO 标准列。脚本仿现有 lunaerp-* 脚本体例（CREATE TABLE IF NOT EXISTS + 主键 DO 块判存 + COMMENT + 索引），空库与存量库重跑均幂等、零报错、零数据覆盖。

**Blocked by:** None（可立即开工）

**Status:** ready-for-agent

- [ ] `lunaerp-finance-vendor-recon.sql`：表 1/2 DDL + 全部索引——主表 recon_no 租户内唯一索引含软删行；行表 `(tenant_id, recon_id)`、`(tenant_id, source_ref_type, source_ref_id)`、`(tenant_id, stock_in_item_id)`；含 status/供应商/区间列注释
- [ ] `lunaerp-finance-payment.sql`：表 3/4/5 DDL + 索引——apply_no/payment_no 租户内唯一含软删行；`(tenant_id, supplier_id, approval_status)`、`(tenant_id, recon_id)`；payment `(tenant_id, supplier_id)`；ref `(tenant_id, ref_id)`、`(tenant_id, payment_id)`
- [ ] payment 共享表只建本 spec 最小列集（direction IN/OUT、supplier_id/customer_id 按方向二选一、account_id、pay_amount、prepay_used_amount、status COMPLETED/REVERSED 预留、登记人/时间）；脚本注释标注与 10 号客户对账（IN 侧）合并收口点及付款单号前缀挂账，不提前定死
- [ ] `lunaerp-stock-in-yarn-order-ref.sql`：`ALTER ... ADD COLUMN IF NOT EXISTS yarn_order_id int8 / yarn_order_no varchar(32)` + 注释 + 索引，旧数据 NULL 兼容
- [ ] 字典 2269 对账单三态（DRAFT warning/CONFIRMED success/CANCELLED info）、2270 申请类型（RECON primary/PREPAY success）、2271 收付款方向（IN success/OUT warning，共享字典）；dict_data id 顺延避让现有最大占用；审批状态字典复用 02 号（若 02 未定义则补建并在脚本注释挂账冲突清单）
- [ ] 菜单 10730 供应商对账 + 按钮 10731 生成(:create)/10732 编辑重取(:update)/10733 确认作废(:confirm)/10734 生成付款申请(:generate)/10735 打印(:print)/10736 删除(:delete)；10740 收付款 + 10741 发起申请(:apply)/10742 删除申请(:apply)/10743 付款登记(:register)；不设删除付款单按钮（红冲二期）；均先 DELETE 再 INSERT
- [ ] 错误码段 1-030-022（000–007）与 1-030-023（000–007，共享段先占）登记进错误码段位总表/GLOSSARY，文案含占位符（004 单号、007 金额）
- [ ] 空库与存量库各重跑两次验证幂等、零报错、零数据覆盖
