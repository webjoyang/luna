-- =============================================================================
-- 模块：LunaERP - 财务域 - 客户对账/收款核销/发票申请/汇率主档（SQL基建）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + ADD COLUMN IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT，字典 data id 自 3800 起判占用平移避让。
-- 避让声明：错误码 1-030-020（域公共）、1-030-021（客户对账）、1-030-023（IN 侧 100–199）、1-030-026（发票）；
--           菜单 10720（按钮 10721–10727）、10740（收款按钮 10745–10749）、10770（按钮 10771–10774）、10780（按钮 10781–10782）；
--           字典类型 2272–2278；审批单据类型 CUSTOMER_RECEIPT。
-- 注意：本脚本仅做语法自查与迁移记录，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 表 1：财务域公共 - 经营分录账表 lunaerp_finance_ledger_entry
--    append-only，唯一真实账本（禁止 update/delete）；
--    业务回指发货明细/运费/收款/退货；红冲通过 reversal_of_id 关联原分录。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_ledger_entry (
    id              int8          NOT NULL,
    biz_date        date          NOT NULL,
    party_type      varchar(16)   NOT NULL DEFAULT 'CUSTOMER',
    party_id        int8          NOT NULL,
    account_id      int8          NULL,
    category        varchar(32)   NOT NULL,
    direction       varchar(8)    NOT NULL,
    currency        varchar(8)    NOT NULL DEFAULT 'RMB',
    exchange_rate   numeric(20,8) NOT NULL DEFAULT 1.00000000,
    original_amount numeric(18,2) NOT NULL DEFAULT 0,
    base_amount     numeric(18,2) NOT NULL DEFAULT 0,
    ref_type        varchar(32)   NOT NULL,
    ref_id          int8          NOT NULL,
    ref_item_id     int8          NULL,
    reversal_of_id  int8          NULL,
    remark          varchar(1000) NULL,
    creator         varchar(64)   NULL DEFAULT '',
    create_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64)   NULL DEFAULT '',
    update_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2          NOT NULL DEFAULT 0,
    tenant_id       int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_ledger_entry') THEN
        ALTER TABLE lunaerp_finance_ledger_entry ADD CONSTRAINT pk_lunaerp_finance_ledger_entry PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_ledger_entry_seq;

-- 经营分录账索引
CREATE INDEX IF NOT EXISTS idx_lfle_party_biz_date
    ON lunaerp_finance_ledger_entry (tenant_id, party_type, party_id, biz_date);

CREATE INDEX IF NOT EXISTS idx_lfle_ref
    ON lunaerp_finance_ledger_entry (tenant_id, ref_type, ref_id);

CREATE INDEX IF NOT EXISTS idx_lfle_reversal_of
    ON lunaerp_finance_ledger_entry (tenant_id, reversal_of_id);

CREATE INDEX IF NOT EXISTS idx_lfle_account_biz_date
    ON lunaerp_finance_ledger_entry (tenant_id, account_id, biz_date);

COMMENT ON TABLE lunaerp_finance_ledger_entry IS '财务管理-经营分录账表（域公共，append-only 唯一账本）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.id IS '分录ID';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.biz_date IS '业务日期（发货日/收款日/操作日）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.party_type IS '往来方类型（CUSTOMER 客户 / SUPPLIER 供应商）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.party_id IS '往来方ID（客户ID或供应商ID）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.account_id IS '资金账户ID（非空表示资金流水视图行）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.category IS '分录类别（字典 lunaerp_finance_ledger_category）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.direction IS '往来方向（DEBIT 往来增加借 / CREDIT 往来减少贷）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.currency IS '币种（RMB/USD/EUR/HKD等）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.exchange_rate IS '入账汇率（1外币 = 汇率本位币）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.original_amount IS '原币金额（元）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.base_amount IS '本位币金额（元）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.ref_type IS '业务回指类型（SHIPMENT/SHIPMENT_ITEM/SHIPMENT_FREIGHT/RECEIPT等）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.ref_id IS '业务回指主单ID';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.ref_item_id IS '业务回指明细ID';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.reversal_of_id IS '红冲回指原分录ID';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.deleted IS '是否删除（append-only 逻辑占位，应用层禁删）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 表 2：财务域公共 - 资金账户表 lunaerp_finance_account
--    资金账户最小集（期初余额仅创建可写、余额仅单据过账条件更新改变）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_account (
    id              int8          NOT NULL,
    account_name    varchar(64)   NOT NULL,
    account_type    varchar(16)   NOT NULL,
    currency        varchar(8)    NOT NULL DEFAULT 'RMB',
    opening_balance numeric(18,2) NOT NULL DEFAULT 0,
    current_balance numeric(18,2) NOT NULL DEFAULT 0,
    status          varchar(16)   NOT NULL DEFAULT 'ENABLE',
    remark          varchar(1000) NULL,
    creator         varchar(64)   NULL DEFAULT '',
    create_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64)   NULL DEFAULT '',
    update_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2          NOT NULL DEFAULT 0,
    tenant_id       int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_account') THEN
        ALTER TABLE lunaerp_finance_account ADD CONSTRAINT pk_lunaerp_finance_account PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_account_seq;

-- 账户名租户内全量唯一含软删行
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfa_tenant_account_name
    ON lunaerp_finance_account (tenant_id, account_name);

-- 状态查询索引
CREATE INDEX IF NOT EXISTS idx_lfa_tenant_status
    ON lunaerp_finance_account (tenant_id, status);

COMMENT ON TABLE lunaerp_finance_account IS '财务管理-资金账户表（域公共最小集）';
COMMENT ON COLUMN lunaerp_finance_account.id IS '资金账户ID';
COMMENT ON COLUMN lunaerp_finance_account.account_name IS '账户名称（租户内全量唯一）';
COMMENT ON COLUMN lunaerp_finance_account.account_type IS '账户类型（字典 lunaerp_finance_account_type：CASH/BANK/WECHAT/ALIPAY）';
COMMENT ON COLUMN lunaerp_finance_account.currency IS '账户币种（默认 RMB）';
COMMENT ON COLUMN lunaerp_finance_account.opening_balance IS '期初余额（元，仅创建时录入一次，此后只读）';
COMMENT ON COLUMN lunaerp_finance_account.current_balance IS '当前余额（元，冗余字段，仅经单据过账条件更新）';
COMMENT ON COLUMN lunaerp_finance_account.status IS '状态（ENABLE 启用 / DISABLE 停用）';
COMMENT ON COLUMN lunaerp_finance_account.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_finance_account.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_account.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_account.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_account.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_account.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_account.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 表 3：财务域公共 - 汇率主档表 lunaerp_finance_exchange_rate
--    即期汇率主档（SPOT，租户+币种+生效日唯一，RMB 内置 rate=1 不建行）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_exchange_rate (
    id             int8          NOT NULL,
    currency       varchar(8)    NOT NULL,
    rate           numeric(20,8) NOT NULL,
    effective_date date          NOT NULL,
    status         varchar(16)   NOT NULL DEFAULT 'ENABLE',
    remark         varchar(1000) NULL,
    creator        varchar(64)   NULL DEFAULT '',
    create_time    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater        varchar(64)   NULL DEFAULT '',
    update_time    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        int2          NOT NULL DEFAULT 0,
    tenant_id      int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_exchange_rate') THEN
        ALTER TABLE lunaerp_finance_exchange_rate ADD CONSTRAINT pk_lunaerp_finance_exchange_rate PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_exchange_rate_seq;

-- 租户+币种+生效日唯一索引（含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfer_tenant_curr_eff
    ON lunaerp_finance_exchange_rate (tenant_id, currency, effective_date);

CREATE INDEX IF NOT EXISTS idx_lfer_tenant_curr_eff_status
    ON lunaerp_finance_exchange_rate (tenant_id, currency, effective_date, status);

COMMENT ON TABLE lunaerp_finance_exchange_rate IS '财务管理-汇率主档表（域公共，SPOT 即期牌价）';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.id IS '汇率记录ID';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.currency IS '外币币种（USD/EUR/HKD等）';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.rate IS '汇率（1外币 = rate本位币）';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.effective_date IS '生效日期';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.status IS '状态（ENABLE 启用 / DISABLE 停用）';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.tenant_id IS '租户编号';

-- =============================================================================
-- 4. 表 4：财务域 - 客户对账单主表 lunaerp_finance_statement
--    单号 CS+YYYY+4位流水（ADR-0004 模式），租户内全量唯一含软删行；
--    五态状态机 DRAFT/SENT/CONFIRMED/REJECTED/VOID；
--    支持门户在线确认（ONLINE_PORTAL）与代客确认（ON_BEHALF）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_statement (
    id              int8          NOT NULL,
    statement_no    varchar(32)   NOT NULL,
    customer_id     int8          NOT NULL,
    customer_name   varchar(128)  NOT NULL,
    currency        varchar(8)    NOT NULL DEFAULT 'RMB',
    period_start    date          NOT NULL,
    period_end      date          NOT NULL,
    opening_balance numeric(18,2) NOT NULL DEFAULT 0,
    period_debit    numeric(18,2) NOT NULL DEFAULT 0,
    period_credit   numeric(18,2) NOT NULL DEFAULT 0,
    closing_balance numeric(18,2) NOT NULL DEFAULT 0,
    status          varchar(16)   NOT NULL DEFAULT 'DRAFT',
    confirm_type    varchar(16)   NULL,
    confirm_by_name varchar(64)   NULL,
    confirm_time    timestamp     NULL,
    confirm_ip      varchar(64)   NULL,
    confirm_method  varchar(32)   NULL,
    reject_reason   varchar(500)  NULL,
    reply_remark    varchar(500)  NULL,
    remark          varchar(1000) NULL,
    creator         varchar(64)   NULL DEFAULT '',
    create_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64)   NULL DEFAULT '',
    update_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2          NOT NULL DEFAULT 0,
    tenant_id       int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_statement') THEN
        ALTER TABLE lunaerp_finance_statement ADD CONSTRAINT pk_lunaerp_finance_statement PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_statement_seq;

-- 对账单号租户内全量唯一含软删行
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfs_statement_no
    ON lunaerp_finance_statement (tenant_id, statement_no);

-- 客户与状态筛选索引
CREATE INDEX IF NOT EXISTS idx_lfs_customer_id
    ON lunaerp_finance_statement (tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_lfs_status
    ON lunaerp_finance_statement (tenant_id, status);

-- 对账区间索引
CREATE INDEX IF NOT EXISTS idx_lfs_period
    ON lunaerp_finance_statement (tenant_id, period_start, period_end);

COMMENT ON TABLE lunaerp_finance_statement IS '财务管理-客户对账单主表';
COMMENT ON COLUMN lunaerp_finance_statement.id IS '对账单ID';
COMMENT ON COLUMN lunaerp_finance_statement.statement_no IS '对账单号（CS+YYYY+4位流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_statement.customer_id IS '客户ID（lunaerp_baseinfo_customer.id）';
COMMENT ON COLUMN lunaerp_finance_statement.customer_name IS '客户公司名快照';
COMMENT ON COLUMN lunaerp_finance_statement.currency IS '对账币种（多币种分单生成）';
COMMENT ON COLUMN lunaerp_finance_statement.period_start IS '对账区间起始日期（发货/收款日，含端点）';
COMMENT ON COLUMN lunaerp_finance_statement.period_end IS '对账区间截止日期（发货/收款日，含端点）';
COMMENT ON COLUMN lunaerp_finance_statement.opening_balance IS '期初欠款余额（元，服务端权威重算）';
COMMENT ON COLUMN lunaerp_finance_statement.period_debit IS '本期应收发生额（借方合计，发货明细分录汇总）';
COMMENT ON COLUMN lunaerp_finance_statement.period_credit IS '本期收款核销额（贷方合计，收款/预收转核销/退货冲减汇总）';
COMMENT ON COLUMN lunaerp_finance_statement.closing_balance IS '期末应收余额（元，期初+本期借-本期贷）';
COMMENT ON COLUMN lunaerp_finance_statement.status IS '状态（字典 lunaerp_finance_statement_status：DRAFT/SENT/CONFIRMED/REJECTED/VOID）';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_type IS '确认方式（字典 lunaerp_finance_statement_confirm_type：ONLINE_PORTAL/ON_BEHALF）';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_by_name IS '确认人姓名快照';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_time IS '确认时间';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_ip IS '确认客户端IP';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_method IS '线下代客确认方式（微信/电话/邮件/纸质签字等，代客确认必填）';
COMMENT ON COLUMN lunaerp_finance_statement.reject_reason IS '异议/驳回原因（REJECTED必填）';
COMMENT ON COLUMN lunaerp_finance_statement.reply_remark IS '财务回复异议备注';
COMMENT ON COLUMN lunaerp_finance_statement.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_finance_statement.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_statement.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_statement.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_statement.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_statement.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_statement.tenant_id IS '租户编号';

-- =============================================================================
-- 5. 表 5：财务域 - 客户对账单行表 lunaerp_finance_statement_item
--    行表即快照，生成时从分录账引用与冻结，零回写；
--    一条经营分录最多被一张 CONFIRMED 对账单强占用。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_statement_item (
    id              int8          NOT NULL,
    statement_id    int8          NOT NULL,
    line_no         int4          NOT NULL DEFAULT 1,
    ledger_entry_id int8          NOT NULL,
    direction       varchar(8)    NOT NULL,
    biz_date        date          NOT NULL,
    ref_type        varchar(32)   NOT NULL,
    ref_no          varchar(32)   NULL,
    summary         varchar(256)  NULL,
    debit_amount    numeric(18,2) NOT NULL DEFAULT 0,
    credit_amount   numeric(18,2) NOT NULL DEFAULT 0,
    sign_status     varchar(16)   NULL,
    remark          varchar(1000) NULL,
    creator         varchar(64)   NULL DEFAULT '',
    create_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64)   NULL DEFAULT '',
    update_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2          NOT NULL DEFAULT 0,
    tenant_id       int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_statement_item') THEN
        ALTER TABLE lunaerp_finance_statement_item ADD CONSTRAINT pk_lunaerp_finance_statement_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_statement_item_seq;

-- 对账单行索引
CREATE INDEX IF NOT EXISTS idx_lfsi_statement_id
    ON lunaerp_finance_statement_item (tenant_id, statement_id);

CREATE INDEX IF NOT EXISTS idx_lfsi_ledger_entry_id
    ON lunaerp_finance_statement_item (tenant_id, ledger_entry_id);

COMMENT ON TABLE lunaerp_finance_statement_item IS '财务管理-客户对账单明细行快照表';
COMMENT ON COLUMN lunaerp_finance_statement_item.id IS '对账单明细行ID';
COMMENT ON COLUMN lunaerp_finance_statement_item.statement_id IS '所属对账单ID（lunaerp_finance_statement.id）';
COMMENT ON COLUMN lunaerp_finance_statement_item.line_no IS '行号（从1递增）';
COMMENT ON COLUMN lunaerp_finance_statement_item.ledger_entry_id IS '被引用的经营分录账ID（lunaerp_finance_ledger_entry.id）';
COMMENT ON COLUMN lunaerp_finance_statement_item.direction IS '往来方向（DEBIT 借/ CREDIT 贷）';
COMMENT ON COLUMN lunaerp_finance_statement_item.biz_date IS '业务日期快照';
COMMENT ON COLUMN lunaerp_finance_statement_item.ref_type IS '来源单据类型快照（SHIPMENT/RECEIPT等）';
COMMENT ON COLUMN lunaerp_finance_statement_item.ref_no IS '来源业务单号快照';
COMMENT ON COLUMN lunaerp_finance_statement_item.summary IS '摘要（品名或事由）';
COMMENT ON COLUMN lunaerp_finance_statement_item.debit_amount IS '借方金额（元，应收增加）';
COMMENT ON COLUMN lunaerp_finance_statement_item.credit_amount IS '贷方金额（元，应收减少）';
COMMENT ON COLUMN lunaerp_finance_statement_item.sign_status IS '发货单签收状态快照标记（不影响对账成立口径）';
COMMENT ON COLUMN lunaerp_finance_statement_item.remark IS '行备注';
COMMENT ON COLUMN lunaerp_finance_statement_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_statement_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_statement_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_statement_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_statement_item.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_statement_item.tenant_id IS '租户编号';

-- =============================================================================
-- 6. 表 6：财务域 - 发票申请主表 lunaerp_finance_invoice_application
--    单号 IA+YYYY+4位流水（ADR-0004 模式），租户内全量唯一含软删行；
--    四态状态机 DRAFT/SUBMITTED/ISSUED/REJECTED；
--    锚定发货单累计开票额守卫；登记回填 official_invoice_no。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_invoice_application (
    id                     int8          NOT NULL,
    invoice_no             varchar(32)   NOT NULL,
    customer_id            int8          NOT NULL,
    customer_name          varchar(128)  NOT NULL,
    currency               varchar(8)    NOT NULL DEFAULT 'RMB',
    tax_rate               numeric(5,2)  NOT NULL,
    invoice_kind           varchar(32)   NOT NULL,
    electronic             int2          NOT NULL DEFAULT 1,
    invoice_title          varchar(256)  NOT NULL,
    tax_no                 varchar(32)   NULL,
    address                varchar(512)  NULL,
    bank_info              varchar(512)  NULL,
    status                 varchar(16)   NOT NULL DEFAULT 'DRAFT',
    official_invoice_no    varchar(32)   NULL,
    invoice_code           varchar(32)   NULL,
    invoice_date           date          NULL,
    attachment_url         varchar(512)  NULL,
    total_amount_excl_tax  numeric(18,2) NOT NULL DEFAULT 0,
    total_tax              numeric(18,2) NOT NULL DEFAULT 0,
    total_amount_incl_tax  numeric(18,2) NOT NULL DEFAULT 0,
    issue_by               varchar(64)   NULL,
    issue_time             timestamp     NULL,
    submit_by              varchar(64)   NULL,
    submit_time            timestamp     NULL,
    remark                 varchar(1000) NULL,
    creator                varchar(64)   NULL DEFAULT '',
    create_time            timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                varchar(64)   NULL DEFAULT '',
    update_time            timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                int2          NOT NULL DEFAULT 0,
    tenant_id              int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_invoice_application') THEN
        ALTER TABLE lunaerp_finance_invoice_application ADD CONSTRAINT pk_lunaerp_finance_invoice_application PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_invoice_application_seq;

-- 申请单号租户内全量唯一含软删行
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfia_invoice_no
    ON lunaerp_finance_invoice_application (tenant_id, invoice_no);

-- 官方发票号非空唯一含软删行
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfia_official_invoice_no
    ON lunaerp_finance_invoice_application (tenant_id, official_invoice_no)
    WHERE official_invoice_no IS NOT NULL;

-- 客户与状态索引
CREATE INDEX IF NOT EXISTS idx_lfia_customer_id
    ON lunaerp_finance_invoice_application (tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_lfia_status
    ON lunaerp_finance_invoice_application (tenant_id, status);

COMMENT ON TABLE lunaerp_finance_invoice_application IS '财务管理-发票申请主表';
COMMENT ON COLUMN lunaerp_finance_invoice_application.id IS '发票申请ID';
COMMENT ON COLUMN lunaerp_finance_invoice_application.invoice_no IS '发票申请单号（IA+YYYY+4位流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.customer_id IS '客户ID（lunaerp_baseinfo_customer.id）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.customer_name IS '客户公司名快照';
COMMENT ON COLUMN lunaerp_finance_invoice_application.currency IS '币种（同发货单币种，禁混选）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.tax_rate IS '开票税率百分比（字典 2276，如 13.00, 9.00, 0.00）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.invoice_kind IS '发票种类（字典 2275：数电专票/数电普票/增值税专票/增值税普票）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.electronic IS '是否电子发票（1是 0否纸质）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.invoice_title IS '开票抬头快照';
COMMENT ON COLUMN lunaerp_finance_invoice_application.tax_no IS '纳税人识别号/统一社会信用代码快照';
COMMENT ON COLUMN lunaerp_finance_invoice_application.address IS '开票地址电话快照';
COMMENT ON COLUMN lunaerp_finance_invoice_application.bank_info IS '开户行及账号快照';
COMMENT ON COLUMN lunaerp_finance_invoice_application.status IS '状态（DRAFT/SUBMITTED/ISSUED/REJECTED）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.official_invoice_no IS '官方发票号码（登记开票时回填，20位数电号码）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.invoice_code IS '发票代码（传统纸票可选，数电票为空）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.invoice_date IS '开票日期';
COMMENT ON COLUMN lunaerp_finance_invoice_application.attachment_url IS '发票电子附件URL';
COMMENT ON COLUMN lunaerp_finance_invoice_application.total_amount_excl_tax IS '不含税金额合计（元，服务端按税率反算）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.total_tax IS '税额合计（元，服务端反算）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.total_amount_incl_tax IS '价税合计总金额（元，所选发货单本次开票金额汇总）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.issue_by IS '开票登记人';
COMMENT ON COLUMN lunaerp_finance_invoice_application.issue_time IS '开票登记时间';
COMMENT ON COLUMN lunaerp_finance_invoice_application.submit_by IS '提交人';
COMMENT ON COLUMN lunaerp_finance_invoice_application.submit_time IS '提交时间';
COMMENT ON COLUMN lunaerp_finance_invoice_application.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_finance_invoice_application.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_invoice_application.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_invoice_application.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_invoice_application.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_invoice_application.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.tenant_id IS '租户编号';

-- =============================================================================
-- 7. 表 7：财务域 - 发票申请明细行表 lunaerp_finance_invoice_application_item
--    明细行挂接发货单；单发货单累计在途与已开开票额不得超发货总额（含运费）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_invoice_application_item (
    id              int8          NOT NULL,
    application_id  int8          NOT NULL,
    line_no         int4          NOT NULL DEFAULT 1,
    shipment_id     int8          NOT NULL,
    shipment_no     varchar(32)   NOT NULL,
    shipment_amount numeric(18,2) NOT NULL DEFAULT 0,
    amount          numeric(18,2) NOT NULL DEFAULT 0,
    remark          varchar(1000) NULL,
    creator         varchar(64)   NULL DEFAULT '',
    create_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64)   NULL DEFAULT '',
    update_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2          NOT NULL DEFAULT 0,
    tenant_id       int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_invoice_application_item') THEN
        ALTER TABLE lunaerp_finance_invoice_application_item ADD CONSTRAINT pk_lunaerp_finance_invoice_application_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_invoice_application_item_seq;

-- 发票申请明细行索引
CREATE INDEX IF NOT EXISTS idx_lfiai_application_id
    ON lunaerp_finance_invoice_application_item (tenant_id, application_id);

CREATE INDEX IF NOT EXISTS idx_lfiai_shipment_id
    ON lunaerp_finance_invoice_application_item (tenant_id, shipment_id);

COMMENT ON TABLE lunaerp_finance_invoice_application_item IS '财务管理-发票申请明细行表';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.id IS '明细行ID';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.application_id IS '所属发票申请ID（lunaerp_finance_invoice_application.id）';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.line_no IS '行号（从1递增）';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.shipment_id IS '关联发货单ID（lunaerp_business_shipment.id）';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.shipment_no IS '发货单号快照';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.shipment_amount IS '发货单价税合计金额快照（total_amount + freight_fee）';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.amount IS '本次申请开票金额（元，默认=该单未开票余额可改小）';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.remark IS '行备注说明';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.tenant_id IS '租户编号';

-- =============================================================================
-- 8. 共享表 IN 侧扩列：lunaerp_finance_payment / lunaerp_finance_payment_ref
--    收付款主表扩 7 列（receipt_type / currency / exchange_rate / receipt_date / bank_ref_no / handler_name / approval_status）
--    核销子表扩 4 列（entry_no / biz_date / base_amount / ledger_entry_id）
-- =============================================================================
ALTER TABLE lunaerp_finance_payment
    ADD COLUMN IF NOT EXISTS receipt_type varchar(16) NULL,
    ADD COLUMN IF NOT EXISTS currency varchar(8) NULL DEFAULT 'RMB',
    ADD COLUMN IF NOT EXISTS exchange_rate numeric(20,8) NULL DEFAULT 1.00000000,
    ADD COLUMN IF NOT EXISTS receipt_date date NULL,
    ADD COLUMN IF NOT EXISTS bank_ref_no varchar(64) NULL,
    ADD COLUMN IF NOT EXISTS handler_name varchar(64) NULL,
    ADD COLUMN IF NOT EXISTS approval_status varchar(16) NOT NULL DEFAULT 'PENDING';

COMMENT ON COLUMN lunaerp_finance_payment.receipt_type IS '收款单类型（字典 lunaerp_finance_receipt_type：RECEIPT 货款 / ADVANCE 预收；OUT 侧恒空）';
COMMENT ON COLUMN lunaerp_finance_payment.currency IS '收款币种（复用销售合同币种/外币，OUT 一期为 RMB）';
COMMENT ON COLUMN lunaerp_finance_payment.exchange_rate IS '过账日汇率（1外币 = 汇率本位币，RMB 为 1.00000000）';
COMMENT ON COLUMN lunaerp_finance_payment.receipt_date IS '收款日期（进分录账 biz_date）';
COMMENT ON COLUMN lunaerp_finance_payment.bank_ref_no IS '银行回单号/交易流水号';
COMMENT ON COLUMN lunaerp_finance_payment.handler_name IS '经手人姓名';
COMMENT ON COLUMN lunaerp_finance_payment.approval_status IS '审批状态（02号引擎跨切面五态：PENDING/APPROVING/APPROVED/REJECTED/CANCELLED；OUT 侧不走审批）';

ALTER TABLE lunaerp_finance_payment_ref
    ADD COLUMN IF NOT EXISTS entry_no varchar(32) NULL,
    ADD COLUMN IF NOT EXISTS biz_date date NULL,
    ADD COLUMN IF NOT EXISTS base_amount numeric(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS ledger_entry_id int8 NULL;

COMMENT ON COLUMN lunaerp_finance_payment_ref.entry_no IS '分录编号/来源单号快照';
COMMENT ON COLUMN lunaerp_finance_payment_ref.biz_date IS '业务日期快照';
COMMENT ON COLUMN lunaerp_finance_payment_ref.base_amount IS '本位币核销金额快照（元）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.ledger_entry_id IS '核销分录回指ID（lunaerp_finance_ledger_entry.id，OUT 侧恒空）';

-- =============================================================================
-- 9. 客户档案扩列：lunaerp_baseinfo_customer +5 列
--    新增开票信息四要素（抬头/地址/开户行/账号）及信用账期天数；税号复用 unified_social_credit_code。
-- =============================================================================
ALTER TABLE lunaerp_baseinfo_customer
    ADD COLUMN IF NOT EXISTS invoice_title varchar(256) NULL,
    ADD COLUMN IF NOT EXISTS invoice_address varchar(512) NULL,
    ADD COLUMN IF NOT EXISTS bank_name varchar(256) NULL,
    ADD COLUMN IF NOT EXISTS bank_account_no varchar(64) NULL,
    ADD COLUMN IF NOT EXISTS credit_days int4 NULL;

COMMENT ON COLUMN lunaerp_baseinfo_customer.invoice_title IS '开票抬头（缺省取公司名称）';
COMMENT ON COLUMN lunaerp_baseinfo_customer.invoice_address IS '开票地址（含电话信息）';
COMMENT ON COLUMN lunaerp_baseinfo_customer.bank_name IS '开户银行名称';
COMMENT ON COLUMN lunaerp_baseinfo_customer.bank_account_no IS '开户银行账号';
COMMENT ON COLUMN lunaerp_baseinfo_customer.credit_days IS '信用账期天数（整数，空=不设账期）';

-- =============================================================================
-- 10. 字典：type 2272–2278 + 审批业务类型 CUSTOMER_RECEIPT
--    2272: lunaerp_finance_statement_status 对账单状态
--    2273: lunaerp_finance_receipt_type 收款类型
--    2274: lunaerp_finance_ledger_category 经营分录类别（一期9值）
--    2275: lunaerp_finance_invoice_kind 发票种类
--    2276: lunaerp_finance_tax_rate 开票税率
--    2277: lunaerp_finance_account_type 资金账户类型
--    2278: lunaerp_finance_statement_confirm_type 对账单确认方式
--    按 DO 块检测 2272–2278 与 3850 起步数据 ID 是否冲突并平移避让。
-- =============================================================================
-- @formatter:off
DO $$
DECLARE
    v_type_id_stmt int8;
    v_type_id_rcpt int8;
    v_type_id_ledger int8;
    v_type_id_inv_kind int8;
    v_type_id_tax_rate int8;
    v_type_id_acc_type int8;
    v_type_id_stmt_confirm int8;
    v_offset int8;
    v_receipt_biz_data_id int8;
BEGIN
    -- 10.1 清理已有同类型字典
    DELETE FROM system_dict_data WHERE dict_type IN (
        'lunaerp_finance_statement_status',
        'lunaerp_finance_receipt_type',
        'lunaerp_finance_ledger_category',
        'lunaerp_finance_invoice_kind',
        'lunaerp_finance_tax_rate',
        'lunaerp_finance_account_type',
        'lunaerp_finance_statement_confirm_type'
    );
    DELETE FROM system_dict_type WHERE type IN (
        'lunaerp_finance_statement_status',
        'lunaerp_finance_receipt_type',
        'lunaerp_finance_ledger_category',
        'lunaerp_finance_invoice_kind',
        'lunaerp_finance_tax_rate',
        'lunaerp_finance_account_type',
        'lunaerp_finance_statement_confirm_type'
    );

    -- 10.2 字典类型 ID 避让判定（2272-2278 判存平移）
    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2272) THEN
        v_type_id_stmt := 2272;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), 2278), 2278) + 1 INTO v_type_id_stmt FROM system_dict_type;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2273) THEN
        v_type_id_rcpt := 2273;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), v_type_id_stmt), v_type_id_stmt) + 1 INTO v_type_id_rcpt FROM system_dict_type;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2274) THEN
        v_type_id_ledger := 2274;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), v_type_id_rcpt), v_type_id_rcpt) + 1 INTO v_type_id_ledger FROM system_dict_type;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2275) THEN
        v_type_id_inv_kind := 2275;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), v_type_id_ledger), v_type_id_ledger) + 1 INTO v_type_id_inv_kind FROM system_dict_type;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2276) THEN
        v_type_id_tax_rate := 2276;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), v_type_id_inv_kind), v_type_id_inv_kind) + 1 INTO v_type_id_tax_rate FROM system_dict_type;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2277) THEN
        v_type_id_acc_type := 2277;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), v_type_id_tax_rate), v_type_id_tax_rate) + 1 INTO v_type_id_acc_type FROM system_dict_type;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2278) THEN
        v_type_id_stmt_confirm := 2278;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), v_type_id_acc_type), v_type_id_acc_type) + 1 INTO v_type_id_stmt_confirm FROM system_dict_type;
    END IF;

    -- 插入字典类型
    INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
    VALUES
        (v_type_id_stmt, '客户对账单状态', 'lunaerp_finance_statement_status', 0, 'LunaERP 财务管理-客户对账单状态', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, NULL),
        (v_type_id_rcpt, '收款单类型', 'lunaerp_finance_receipt_type', 0, 'LunaERP 财务管理-收款单类型（货款/预收）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, NULL),
        (v_type_id_ledger, '经营分录类别', 'lunaerp_finance_ledger_category', 0, 'LunaERP 财务管理-经营分录账类别（一期全集9值）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, NULL),
        (v_type_id_inv_kind, '发票种类', 'lunaerp_finance_invoice_kind', 0, 'LunaERP 财务管理-发票种类', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, NULL),
        (v_type_id_tax_rate, '开票税率', 'lunaerp_finance_tax_rate', 0, 'LunaERP 财务管理-开票税率', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, NULL),
        (v_type_id_acc_type, '资金账户类型', 'lunaerp_finance_account_type', 0, 'LunaERP 财务管理-资金账户类型（域公共）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, NULL),
        (v_type_id_stmt_confirm, '对账单确认方式', 'lunaerp_finance_statement_confirm_type', 0, 'LunaERP 财务管理-对账单确认方式', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, NULL);

    -- 10.3 字典数据 ID 避让判定（自 3800 起步；若已有非本业务占用则平移避让）
    IF NOT EXISTS (SELECT 1 FROM system_dict_data WHERE id BETWEEN 3800 AND 3833) THEN
        v_offset := 3800;
    ELSIF NOT EXISTS (SELECT 1 FROM system_dict_data WHERE id BETWEEN 3850 AND 3890) THEN
        v_offset := 3850;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), 3890), 3890) + 1 INTO v_offset FROM system_dict_data;
    END IF;

    -- 插入字典数据：2272 对账单状态（5态）
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 0, 1, '草稿', 'DRAFT', 'lunaerp_finance_statement_status', 0, 'info', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 1, 2, '已发出', 'SENT', 'lunaerp_finance_statement_status', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 2, 3, '已确认', 'CONFIRMED', 'lunaerp_finance_statement_status', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 3, 4, '已驳回', 'REJECTED', 'lunaerp_finance_statement_status', 0, 'danger', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 4, 5, '已作废', 'VOID', 'lunaerp_finance_statement_status', 0, 'info', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 插入字典数据：2273 收款单类型（2态）
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 5, 1, '货款收款', 'RECEIPT', 'lunaerp_finance_receipt_type', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 6, 2, '预收账款', 'ADVANCE', 'lunaerp_finance_receipt_type', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 插入字典数据：2274 经营分录类别（一期9值）
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 7,  1, '应收成立', 'AR_INC', 'lunaerp_finance_ledger_category', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 8,  2, '发货红冲应收冲减', 'AR_INC_REVERSAL', 'lunaerp_finance_ledger_category', 0, 'danger', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 9,  3, '收款入账', 'RECEIPT', 'lunaerp_finance_ledger_category', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 10, 4, '收款红冲', 'RECEIPT_REVERSAL', 'lunaerp_finance_ledger_category', 0, 'danger', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 11, 5, '收款核销', 'RECEIPT_WRITE_OFF', 'lunaerp_finance_ledger_category', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 12, 6, '预收转核销', 'ADVANCE_WRITE_OFF', 'lunaerp_finance_ledger_category', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 13, 7, '退货应收冲减', 'AR_RETURN_REVERSAL', 'lunaerp_finance_ledger_category', 0, 'warning', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 14, 8, '供应商付款入账', 'OUT_SUPPLIER', 'lunaerp_finance_ledger_category', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 15, 9, '供应商付款红冲', 'OUT_SUPPLIER_REVERSAL', 'lunaerp_finance_ledger_category', 0, 'danger', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 插入字典数据：2275 发票种类（4类）
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 16, 1, '数电专票', 'DIGITAL_SPECIAL', 'lunaerp_finance_invoice_kind', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 17, 2, '数电普票', 'DIGITAL_NORMAL', 'lunaerp_finance_invoice_kind', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 18, 3, '增值税专用发票', 'VAT_SPECIAL', 'lunaerp_finance_invoice_kind', 0, 'warning', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 19, 4, '增值税普通发票', 'VAT_NORMAL', 'lunaerp_finance_invoice_kind', 0, 'info', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 插入字典数据：2276 开票税率（7档）
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 20, 1, '13%', '13.00', 'lunaerp_finance_tax_rate', 0, 'primary', '', '纺织默认税率', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 21, 2, '9%', '9.00', 'lunaerp_finance_tax_rate', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 22, 3, '6%', '6.00', 'lunaerp_finance_tax_rate', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 23, 4, '3%', '3.00', 'lunaerp_finance_tax_rate', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 24, 5, '1%', '1.00', 'lunaerp_finance_tax_rate', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 25, 6, '0%', '0.00', 'lunaerp_finance_tax_rate', 0, 'info', '', '外贸出口零税率', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 26, 7, '免税', 'EXEMPT', 'lunaerp_finance_tax_rate', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 插入字典数据：2277 资金账户类型（4类）
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 27, 1, '现金', 'CASH', 'lunaerp_finance_account_type', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 28, 2, '银行账户', 'BANK', 'lunaerp_finance_account_type', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 29, 3, '微信支付', 'WECHAT', 'lunaerp_finance_account_type', 0, 'warning', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 30, 4, '支付宝', 'ALIPAY', 'lunaerp_finance_account_type', 0, 'info', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 插入字典数据：2278 对账单确认方式（2类）
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 31, 1, '门户在线确认', 'ONLINE_PORTAL', 'lunaerp_finance_statement_confirm_type', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 32, 2, '线下代客确认', 'ON_BEHALF', 'lunaerp_finance_statement_confirm_type', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 10.4 补充审批引擎业务类型 CUSTOMER_RECEIPT
    DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_approval_biz_type' AND value = 'CUSTOMER_RECEIPT';
    SELECT GREATEST(COALESCE(MAX(id), v_offset + 32), v_offset + 32) + 1 INTO v_receipt_biz_data_id FROM system_dict_data;

    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES (v_receipt_biz_data_id, 6, '客户收款', 'CUSTOMER_RECEIPT', 'lunaerp_approval_biz_type', 0, 'success', '', 'LunaERP 财务管理-客户收款单审批', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

END $$;
-- @formatter:on

-- =============================================================================
-- 11. 菜单与权限：10720 对账 / 10740 收款按钮 / 10770 发票 / 10780 汇率
-- =============================================================================
-- @formatter:off
BEGIN;

-- 11.1 确保 10700 财务一级目录存在
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
)
SELECT 10700, '财务', '', 1, 107, 0, '/finance', 'ep:money', NULL, NULL,
       0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 10700);

-- 11.2 删除既有菜单与按钮
DELETE FROM system_menu WHERE id IN (
    10720, 10721, 10722, 10723, 10724, 10725, 10726, 10727,
    10745, 10746, 10747, 10748, 10749,
    10770, 10771, 10772, 10773, 10774,
    10780, 10781, 10782
);

-- 二级菜单：10720 客户对账
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10720, '客户对账', 'lunaerp:finance:customerrecon:query', 2, 2, 10700, 'customer-reconciliation', 'ep:document-checked', 'lunaerp-elmui/finance/customer-reconciliation/index', 'LunaerpFinanceCustomerRecon',
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'
);

-- 按钮：10721–10727 对账按钮
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES
(10721, '生成对账单', 'lunaerp:finance:customerrecon:create', 3, 1, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10722, '编辑对账单', 'lunaerp:finance:customerrecon:update', 3, 2, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10723, '删除对账单', 'lunaerp:finance:customerrecon:delete', 3, 3, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10724, '发送对账单', 'lunaerp:finance:customerrecon:send', 3, 4, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10725, '作废对账单', 'lunaerp:finance:customerrecon:void', 3, 5, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10726, '代客确认', 'lunaerp:finance:customerrecon:confirm-on-behalf', 3, 6, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10727, '打印对账单', 'lunaerp:finance:customerrecon:print', 3, 7, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0');

-- 按钮：10745–10749 收款按钮（挂 10740 收付款菜单下）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES
(10745, '创建收款单', 'lunaerp:finance:payment:create', 3, 5, 10740, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10746, '修改收款单', 'lunaerp:finance:payment:update', 3, 6, 10740, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10747, '删除收款单', 'lunaerp:finance:payment:delete', 3, 7, 10740, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10748, '预收转核销', 'lunaerp:finance:payment:writeoff', 3, 8, 10740, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10749, '红冲收款单', 'lunaerp:finance:payment:reverse', 3, 9, 10740, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0');

-- 二级菜单：10770 发票申请
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10770, '发票申请', 'lunaerp:finance:invoice:query', 2, 7, 10700, 'invoice-application', 'ep:tickets', 'lunaerp-elmui/finance/invoice-application/index', 'LunaerpFinanceInvoiceApplication',
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'
);

-- 按钮：10771–10774 发票按钮
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES
(10771, '创建发票申请', 'lunaerp:finance:invoice:create', 3, 1, 10770, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10772, '修改发票申请', 'lunaerp:finance:invoice:update', 3, 2, 10770, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10773, '删除发票申请', 'lunaerp:finance:invoice:delete', 3, 3, 10770, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10774, '发票登记开票', 'lunaerp:finance:invoice:issue', 3, 4, 10770, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0');

-- 二级菜单：10780 汇率
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10780, '汇率管理', 'lunaerp:finance:exchangerate:query', 2, 8, 10700, 'exchange-rate', 'ep:data-analysis', 'lunaerp-elmui/finance/exchange-rate/index', 'LunaerpFinanceExchangeRate',
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'
);

-- 按钮：10781–10782 汇率按钮
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES
(10781, '新增汇率', 'lunaerp:finance:exchangerate:create', 3, 1, 10780, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10782, '修改汇率', 'lunaerp:finance:exchangerate:update', 3, 2, 10780, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0');

-- 11.3 赋权系统管理员角色
CREATE SEQUENCE IF NOT EXISTS system_role_menu_seq;

DO $$
BEGIN
    PERFORM setval('system_role_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role_menu), 1));
END $$;

DELETE FROM system_role_menu WHERE menu_id IN (
    10720, 10721, 10722, 10723, 10724, 10725, 10726, 10727,
    10745, 10746, 10747, 10748, 10749,
    10770, 10771, 10772, 10773, 10774,
    10780, 10781, 10782
);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM (
    SELECT DISTINCT id FROM system_role
    WHERE code IN ('super_admin', 'admin') OR id IN (1, 2)
) r
CROSS JOIN (
    SELECT 10720 AS id UNION ALL SELECT 10721 UNION ALL SELECT 10722 UNION ALL SELECT 10723 UNION ALL SELECT 10724 UNION ALL SELECT 10725 UNION ALL SELECT 10726 UNION ALL SELECT 10727
    UNION ALL SELECT 10745 UNION ALL SELECT 10746 UNION ALL SELECT 10747 UNION ALL SELECT 10748 UNION ALL SELECT 10749
    UNION ALL SELECT 10770 UNION ALL SELECT 10771 UNION ALL SELECT 10772 UNION ALL SELECT 10773 UNION ALL SELECT 10774
    UNION ALL SELECT 10780 UNION ALL SELECT 10781 UNION ALL SELECT 10782
) m;

COMMIT;
-- @formatter:on

-- =============================================================================
-- 12. 审批引擎：CUSTOMER_RECEIPT 默认启用审批流配置
--    默认单节点逐级审批（财务主管角色审批）；如已存在则保持幂等。
-- =============================================================================
DO $$
DECLARE
    v_config_id int8;
BEGIN
    -- 清理已有历史 CUSTOMER_RECEIPT 流程节点与配置
    DELETE FROM lunaerp_approval_config_node WHERE config_id IN (
        SELECT id FROM lunaerp_approval_config WHERE biz_type = 'CUSTOMER_RECEIPT'
    );
    DELETE FROM lunaerp_approval_config WHERE biz_type = 'CUSTOMER_RECEIPT';

    -- 插入默认流程配置
    v_config_id := nextval('lunaerp_approval_config_seq');

    INSERT INTO lunaerp_approval_config (
        id, tenant_id, biz_type, name, status, remark, creator, create_time, updater, update_time, deleted
    ) VALUES (
        v_config_id, 1, 'CUSTOMER_RECEIPT', '客户收款默认审批流', 'ENABLED',
        '客户收款默认单节点审批流程（财务主管审批）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
    );

    -- 插入第 1 节点：财务主管角色审批
    INSERT INTO lunaerp_approval_config_node (
        id, tenant_id, config_id, sort, name, node_type, approver_type, approver_ids,
        creator, create_time, updater, update_time, deleted
    ) VALUES (
        nextval('lunaerp_approval_config_node_seq'), 1, v_config_id, 1, '财务主管审批',
        'SEQUENTIAL', 'ROLE', '2', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
    );
END $$;

-- =============================================================================
-- 13. 站内信模板种子：对账单驳回通知（LUNA_STATEMENT_REJECTED）
-- =============================================================================
INSERT INTO system_notify_template (
    id, name, code, nickname, content, type, params, status, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    2004, '对账单驳回通知', 'LUNA_STATEMENT_REJECTED', '财务系统',
    '您的对账单被驳回：对账单号 {bizNo}，客户 {customerName}，驳回原因：{reason}',
    2, '["bizNo", "customerName", "reason"]', 0, '对账单状态流转为REJECTED时通知制单财务',
    'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    nickname = EXCLUDED.nickname,
    content = EXCLUDED.content,
    type = EXCLUDED.type,
    params = EXCLUDED.params,
    status = EXCLUDED.status,
    remark = EXCLUDED.remark,
    updater = EXCLUDED.updater,
    update_time = EXCLUDED.update_time,
    deleted = 0;

