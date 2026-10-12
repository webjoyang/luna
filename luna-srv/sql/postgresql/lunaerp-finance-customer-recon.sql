-- =============================================================================
-- 模块：LunaERP - 财务管理 - 客户对账、收款核销、发票申请、汇率主档与分录账
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + ADD COLUMN IF NOT EXISTS + 
--             CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT，字典数据按 DO 块判占用平移避让。
-- 避让声明：错误码 1-030-020 (公共域)、1-030-021 (客户对账)、1-030-023 (100–107 IN 侧)、1-030-026 (发票申请)；
--           菜单 10720（客户对账）及按钮 10721–10727、10740 按钮 10745–10749、
--           10770（发票申请）及按钮 10771–10774、10780（汇率主档）及按钮 10781–10782；
--           字典类型 2272–2278，字典数据 3837–3870。
-- =============================================================================

-- =============================================================================
-- 1. 域公共表 1：经营分录账 lunaerp_finance_ledger_entry（append-only 唯一流水账）
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_ledger_entry (
    id                 int8          NOT NULL,
    biz_date           date          NOT NULL,
    party_type         varchar(16)   NOT NULL DEFAULT 'CUSTOMER',
    party_id           int8          NOT NULL,
    account_id         int8          NULL,
    category           varchar(32)   NOT NULL,
    direction          varchar(8)    NOT NULL,
    currency           varchar(8)    NOT NULL,
    exchange_rate      numeric(20,8) NOT NULL DEFAULT 1,
    original_amount    numeric(18,2) NOT NULL DEFAULT 0,
    base_amount        numeric(18,2) NOT NULL DEFAULT 0,
    ref_type           varchar(32)   NOT NULL,
    ref_id             int8          NOT NULL,
    ref_item_id        int8          NULL,
    reversal_of_id     int8          NULL,
    remark             varchar(512)  NULL,
    creator            varchar(64)   NULL DEFAULT '',
    create_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater            varchar(64)   NULL DEFAULT '',
    update_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            int2          NOT NULL DEFAULT 0,
    tenant_id          int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_ledger_entry') THEN
        ALTER TABLE lunaerp_finance_ledger_entry ADD CONSTRAINT pk_lunaerp_finance_ledger_entry PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_ledger_entry_seq;

-- 往来主体与业务日期索引
CREATE INDEX IF NOT EXISTS idx_lfle_party_date
    ON lunaerp_finance_ledger_entry (tenant_id, party_type, party_id, biz_date);

-- 业务单据回指索引
CREATE INDEX IF NOT EXISTS idx_lfle_ref
    ON lunaerp_finance_ledger_entry (tenant_id, ref_type, ref_id);

-- 红冲回指索引
CREATE INDEX IF NOT EXISTS idx_lfle_reversal
    ON lunaerp_finance_ledger_entry (reversal_of_id);

-- 资金账户与业务日期索引（资金流水视图）
CREATE INDEX IF NOT EXISTS idx_lfle_account_date
    ON lunaerp_finance_ledger_entry (tenant_id, account_id, biz_date);

COMMENT ON TABLE lunaerp_finance_ledger_entry IS '财务管理-经营分录账（域公共 append-only 唯一流水账）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.id IS '分录ID';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.biz_date IS '业务日期（发货日/收款日/操作日）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.party_type IS '往来主体类型（CUSTOMER/SUPPLIER）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.party_id IS '往来主体ID（客户ID/供应商ID）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.account_id IS '资金账户ID（非空即资金流水视图行）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.category IS '分录类别（字典 lunaerp_finance_ledger_category）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.direction IS '借贷方向（DEBIT 借方往来增加 / CREDIT 贷方往来减少）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.currency IS '币种';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.exchange_rate IS '入账汇率（1 外币兑人民币）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.original_amount IS '原币金额';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.base_amount IS '本位币金额（人民币）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.ref_type IS '业务源单类型（SHIPMENT/SHIPMENT_ITEM/SHIPMENT_FREIGHT/PAYMENT等）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.ref_id IS '业务源单ID';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.ref_item_id IS '业务源单明细行ID';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.reversal_of_id IS '红冲回指原分录ID';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 域公共表 2：资金账户 lunaerp_finance_account
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_account (
    id                 int8          NOT NULL,
    account_name       varchar(64)   NOT NULL,
    account_type       varchar(16)   NOT NULL,
    currency           varchar(8)    NOT NULL DEFAULT 'CNY',
    opening_balance    numeric(18,2) NOT NULL DEFAULT 0,
    current_balance    numeric(18,2) NOT NULL DEFAULT 0,
    status             varchar(16)   NOT NULL DEFAULT 'ENABLE',
    remark             varchar(512)  NULL,
    creator            varchar(64)   NULL DEFAULT '',
    create_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater            varchar(64)   NULL DEFAULT '',
    update_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            int2          NOT NULL DEFAULT 0,
    tenant_id          int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_account') THEN
        ALTER TABLE lunaerp_finance_account ADD CONSTRAINT pk_lunaerp_finance_account PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_account_seq;

-- 账户名称租户内唯一（含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfa_tenant_account_name
    ON lunaerp_finance_account (tenant_id, account_name);

-- 账户状态查询索引
CREATE INDEX IF NOT EXISTS idx_lfa_tenant_status
    ON lunaerp_finance_account (tenant_id, status);

COMMENT ON TABLE lunaerp_finance_account IS '财务管理-资金账户表（域公共最小集）';
COMMENT ON COLUMN lunaerp_finance_account.id IS '资金账户ID';
COMMENT ON COLUMN lunaerp_finance_account.account_name IS '资金账户名称（租户内唯一）';
COMMENT ON COLUMN lunaerp_finance_account.account_type IS '账户类型（字典 lunaerp_finance_account_type：CASH/BANK/WECHAT/ALIPAY）';
COMMENT ON COLUMN lunaerp_finance_account.currency IS '账户币种（缺省 CNY）';
COMMENT ON COLUMN lunaerp_finance_account.opening_balance IS '期初余额（仅创建时可录入，后续只读）';
COMMENT ON COLUMN lunaerp_finance_account.current_balance IS '当前余额（仅通过单据过账条件更新变更）';
COMMENT ON COLUMN lunaerp_finance_account.status IS '状态（ENABLE 启用 / DISABLE 停用）';
COMMENT ON COLUMN lunaerp_finance_account.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_finance_account.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_account.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_account.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_account.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_account.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_account.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 域公共表 3：汇率主档 lunaerp_finance_exchange_rate（SPOT 即期牌价）
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_exchange_rate (
    id                 int8          NOT NULL,
    currency           varchar(8)    NOT NULL,
    rate               numeric(20,8) NOT NULL,
    effective_date     date          NOT NULL,
    status             varchar(16)   NOT NULL DEFAULT 'ENABLE',
    remark             varchar(512)  NULL,
    creator            varchar(64)   NULL DEFAULT '',
    create_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater            varchar(64)   NULL DEFAULT '',
    update_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            int2          NOT NULL DEFAULT 0,
    tenant_id          int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_exchange_rate') THEN
        ALTER TABLE lunaerp_finance_exchange_rate ADD CONSTRAINT pk_lunaerp_finance_exchange_rate PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_exchange_rate_seq;

-- 汇率租户+币种+生效日唯一索引（含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfer_currency_date
    ON lunaerp_finance_exchange_rate (tenant_id, currency, effective_date);

-- 状态查询索引
CREATE INDEX IF NOT EXISTS idx_lfer_status
    ON lunaerp_finance_exchange_rate (tenant_id, status);

COMMENT ON TABLE lunaerp_finance_exchange_rate IS '财务管理-汇率主档（SPOT 即期牌价）';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.id IS '汇率记录ID';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.currency IS '外币币种（如 USD/EUR，RMB 不建行代码内置 1）';
COMMENT ON COLUMN lunaerp_finance_exchange_rate.rate IS '汇率（1 外币兑人民币）';
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
-- 4. 客户对账单主表与明细行表
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_statement (
    id                 int8          NOT NULL,
    statement_no       varchar(32)   NOT NULL,
    customer_id        int8          NOT NULL,
    customer_name      varchar(128)  NOT NULL,
    currency           varchar(8)    NOT NULL DEFAULT 'CNY',
    period_start       date          NOT NULL,
    period_end         date          NOT NULL,
    opening_balance    numeric(18,2) NOT NULL DEFAULT 0,
    period_debit       numeric(18,2) NOT NULL DEFAULT 0,
    period_credit      numeric(18,2) NOT NULL DEFAULT 0,
    closing_balance    numeric(18,2) NOT NULL DEFAULT 0,
    status             varchar(16)   NOT NULL DEFAULT 'DRAFT',
    confirm_type       varchar(32)   NULL,
    confirm_by_name    varchar(64)   NULL,
    confirm_time       timestamp     NULL,
    confirm_ip         varchar(64)   NULL,
    confirm_method     varchar(256)  NULL,
    reject_reason      varchar(512)  NULL,
    reply_remark       varchar(512)  NULL,
    remark             varchar(1000) NULL,
    creator            varchar(64)   NULL DEFAULT '',
    create_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater            varchar(64)   NULL DEFAULT '',
    update_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            int2          NOT NULL DEFAULT 0,
    tenant_id          int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_statement') THEN
        ALTER TABLE lunaerp_finance_statement ADD CONSTRAINT pk_lunaerp_finance_statement PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_statement_seq;

-- 对账单号租户内全量唯一（含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfs_statement_no
    ON lunaerp_finance_statement (tenant_id, statement_no);

-- 客户查询索引
CREATE INDEX IF NOT EXISTS idx_lfs_customer
    ON lunaerp_finance_statement (tenant_id, customer_id);

-- 状态查询索引
CREATE INDEX IF NOT EXISTS idx_lfs_status
    ON lunaerp_finance_statement (tenant_id, status);

COMMENT ON TABLE lunaerp_finance_statement IS '财务管理-客户对账单主表';
COMMENT ON COLUMN lunaerp_finance_statement.id IS '对账单ID';
COMMENT ON COLUMN lunaerp_finance_statement.statement_no IS '对账单号（CS+YYYY+4位流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_statement.customer_id IS '客户ID（关联 lunaerp_baseinfo_customer.id）';
COMMENT ON COLUMN lunaerp_finance_statement.customer_name IS '客户名称快照';
COMMENT ON COLUMN lunaerp_finance_statement.currency IS '币种';
COMMENT ON COLUMN lunaerp_finance_statement.period_start IS '对账期间开始日期';
COMMENT ON COLUMN lunaerp_finance_statement.period_end IS '对账期间结束日期';
COMMENT ON COLUMN lunaerp_finance_statement.opening_balance IS '期初应收余额';
COMMENT ON COLUMN lunaerp_finance_statement.period_debit IS '本期借方发生额（本期发货增加）';
COMMENT ON COLUMN lunaerp_finance_statement.period_credit IS '本期贷方发生额（本期收款/冲减）';
COMMENT ON COLUMN lunaerp_finance_statement.closing_balance IS '期末应收余额（期初+借方-贷方）';
COMMENT ON COLUMN lunaerp_finance_statement.status IS '状态（字典 lunaerp_finance_statement_status：DRAFT/SENT/CONFIRMED/REJECTED/VOID）';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_type IS '确认方式（字典 lunaerp_finance_statement_confirm_type：ONLINE_PORTAL/ON_BEHALF）';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_by_name IS '确认人姓名';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_time IS '确认时间';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_ip IS '确认客户端IP（门户确认时记录）';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_method IS '线下确认方式/凭证备注（内部代客时填写）';
COMMENT ON COLUMN lunaerp_finance_statement.reject_reason IS '驳回异议原因';
COMMENT ON COLUMN lunaerp_finance_statement.reply_remark IS '财务回复处理备注';
COMMENT ON COLUMN lunaerp_finance_statement.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_finance_statement.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_statement.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_statement.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_statement.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_statement.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_statement.tenant_id IS '租户编号';

CREATE TABLE IF NOT EXISTS lunaerp_finance_statement_item (
    id                 int8          NOT NULL,
    statement_id       int8          NOT NULL,
    line_no            int4          NOT NULL DEFAULT 1,
    ledger_entry_id    int8          NULL,
    direction          varchar(8)    NOT NULL,
    biz_date           date          NOT NULL,
    ref_type           varchar(32)   NULL,
    ref_no             varchar(32)   NULL,
    summary            varchar(256)  NULL,
    debit_amount       numeric(18,2) NOT NULL DEFAULT 0,
    credit_amount      numeric(18,2) NOT NULL DEFAULT 0,
    sign_status        varchar(32)   NULL,
    remark             varchar(512)  NULL,
    creator            varchar(64)   NULL DEFAULT '',
    create_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater            varchar(64)   NULL DEFAULT '',
    update_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            int2          NOT NULL DEFAULT 0,
    tenant_id          int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_statement_item') THEN
        ALTER TABLE lunaerp_finance_statement_item ADD CONSTRAINT pk_lunaerp_finance_statement_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_statement_item_seq;

-- 对账单明细主表索引
CREATE INDEX IF NOT EXISTS idx_lfsi_statement
    ON lunaerp_finance_statement_item (statement_id);

-- 分录占用索引
CREATE INDEX IF NOT EXISTS idx_lfsi_ledger_entry
    ON lunaerp_finance_statement_item (tenant_id, ledger_entry_id);

COMMENT ON TABLE lunaerp_finance_statement_item IS '财务管理-客户对账单明细行表';
COMMENT ON COLUMN lunaerp_finance_statement_item.id IS '对账单明细ID';
COMMENT ON COLUMN lunaerp_finance_statement_item.statement_id IS '所属对账单ID（关联 lunaerp_finance_statement.id）';
COMMENT ON COLUMN lunaerp_finance_statement_item.line_no IS '明细行序号';
COMMENT ON COLUMN lunaerp_finance_statement_item.ledger_entry_id IS '关联经营分录ID（引用 lunaerp_finance_ledger_entry.id）';
COMMENT ON COLUMN lunaerp_finance_statement_item.direction IS '借贷方向（DEBIT 借方 / CREDIT 贷方）';
COMMENT ON COLUMN lunaerp_finance_statement_item.biz_date IS '业务日期快照';
COMMENT ON COLUMN lunaerp_finance_statement_item.ref_type IS '源单类型快照';
COMMENT ON COLUMN lunaerp_finance_statement_item.ref_no IS '源单单号快照';
COMMENT ON COLUMN lunaerp_finance_statement_item.summary IS '摘要/品名或事由';
COMMENT ON COLUMN lunaerp_finance_statement_item.debit_amount IS '借方金额（元）';
COMMENT ON COLUMN lunaerp_finance_statement_item.credit_amount IS '贷方金额（元）';
COMMENT ON COLUMN lunaerp_finance_statement_item.sign_status IS '发货签收状态标记';
COMMENT ON COLUMN lunaerp_finance_statement_item.remark IS '行备注';
COMMENT ON COLUMN lunaerp_finance_statement_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_statement_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_statement_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_statement_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_statement_item.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_statement_item.tenant_id IS '租户编号';

-- =============================================================================
-- 5. 发票申请主表与行表
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_invoice_application (
    id                     int8          NOT NULL,
    invoice_no             varchar(32)   NOT NULL,
    customer_id            int8          NOT NULL,
    customer_name          varchar(128)  NOT NULL,
    currency               varchar(8)    NOT NULL DEFAULT 'CNY',
    tax_rate               varchar(16)   NOT NULL DEFAULT '13',
    invoice_kind           varchar(32)   NOT NULL,
    electronic             int2          NOT NULL DEFAULT 1,
    invoice_title          varchar(256)  NOT NULL,
    tax_no                 varchar(64)   NULL,
    address                varchar(512)  NULL,
    bank_info              varchar(256)  NULL,
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
    reject_reason          varchar(512)  NULL,
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

-- 申请单号租户内全量唯一（含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfia_invoice_no
    ON lunaerp_finance_invoice_application (tenant_id, invoice_no);

-- 登记发票号租户内非空唯一（含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfia_official_invoice_no
    ON lunaerp_finance_invoice_application (tenant_id, official_invoice_no)
    WHERE official_invoice_no IS NOT NULL;

-- 客户查询索引
CREATE INDEX IF NOT EXISTS idx_lfia_customer
    ON lunaerp_finance_invoice_application (tenant_id, customer_id);

-- 状态查询索引
CREATE INDEX IF NOT EXISTS idx_lfia_status
    ON lunaerp_finance_invoice_application (tenant_id, status);

COMMENT ON TABLE lunaerp_finance_invoice_application IS '财务管理-发票申请主表';
COMMENT ON COLUMN lunaerp_finance_invoice_application.id IS '发票申请ID';
COMMENT ON COLUMN lunaerp_finance_invoice_application.invoice_no IS '发票申请单号（IA+YYYY+4位流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.customer_id IS '客户ID（关联 lunaerp_baseinfo_customer.id）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.customer_name IS '客户名称快照';
COMMENT ON COLUMN lunaerp_finance_invoice_application.currency IS '币种';
COMMENT ON COLUMN lunaerp_finance_invoice_application.tax_rate IS '税率（字典 lunaerp_finance_tax_rate：13/9/6/3/1/0/EXEMPT）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.invoice_kind IS '发票种类（字典 lunaerp_finance_invoice_kind：数电专票/数电普票/增值税专票/增值税普票）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.electronic IS '是否电子发票（1是 0否）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.invoice_title IS '开票抬头快照';
COMMENT ON COLUMN lunaerp_finance_invoice_application.tax_no IS '纳税人识别号/统一社会信用代码快照';
COMMENT ON COLUMN lunaerp_finance_invoice_application.address IS '开票地址电话快照';
COMMENT ON COLUMN lunaerp_finance_invoice_application.bank_info IS '开户银行及账号快照';
COMMENT ON COLUMN lunaerp_finance_invoice_application.status IS '状态（DRAFT/SUBMITTED/ISSUED/REJECTED）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.official_invoice_no IS '正式发票号码（登记回填，20位，租户内唯一）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.invoice_code IS '发票代码（可空）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.invoice_date IS '开票日期（登记回填）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.attachment_url IS '发票文件/回单附件URL';
COMMENT ON COLUMN lunaerp_finance_invoice_application.total_amount_excl_tax IS '不含税金额合计（服务端按税率反算）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.total_tax IS '税额合计（服务端按税率反算）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.total_amount_incl_tax IS '价税合计（源发货单金额汇总）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.issue_by IS '开票登记人';
COMMENT ON COLUMN lunaerp_finance_invoice_application.issue_time IS '开票登记时间';
COMMENT ON COLUMN lunaerp_finance_invoice_application.submit_by IS '提交人';
COMMENT ON COLUMN lunaerp_finance_invoice_application.submit_time IS '提交时间';
COMMENT ON COLUMN lunaerp_finance_invoice_application.reject_reason IS '驳回原因';
COMMENT ON COLUMN lunaerp_finance_invoice_application.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_finance_invoice_application.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_invoice_application.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_invoice_application.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_invoice_application.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_invoice_application.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_invoice_application.tenant_id IS '租户编号';

CREATE TABLE IF NOT EXISTS lunaerp_finance_invoice_application_item (
    id                 int8          NOT NULL,
    application_id     int8          NOT NULL,
    line_no            int4          NOT NULL DEFAULT 1,
    shipment_id        int8          NOT NULL,
    shipment_no        varchar(32)   NOT NULL,
    shipment_amount    numeric(18,2) NOT NULL DEFAULT 0,
    amount             numeric(18,2) NOT NULL DEFAULT 0,
    remark             varchar(512)  NULL,
    creator            varchar(64)   NULL DEFAULT '',
    create_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater            varchar(64)   NULL DEFAULT '',
    update_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            int2          NOT NULL DEFAULT 0,
    tenant_id          int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_invoice_application_item') THEN
        ALTER TABLE lunaerp_finance_invoice_application_item ADD CONSTRAINT pk_lunaerp_finance_invoice_application_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_invoice_application_item_seq;

-- 发票申请明细所属申请索引
CREATE INDEX IF NOT EXISTS idx_lfiai_application
    ON lunaerp_finance_invoice_application_item (application_id);

-- 发货单引用索引
CREATE INDEX IF NOT EXISTS idx_lfiai_shipment
    ON lunaerp_finance_invoice_application_item (tenant_id, shipment_id);

COMMENT ON TABLE lunaerp_finance_invoice_application_item IS '财务管理-发票申请明细行表';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.id IS '申请明细ID';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.application_id IS '所属发票申请ID（关联 lunaerp_finance_invoice_application.id）';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.line_no IS '明细行序号';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.shipment_id IS '关联发货单ID（关联 lunaerp_business_shipment.id）';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.shipment_no IS '发货单号快照';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.shipment_amount IS '发货单价税合计快照（total_amount+freight_fee）';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.amount IS '本次申请开票金额（元）';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.remark IS '行备注';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_invoice_application_item.tenant_id IS '租户编号';

-- =============================================================================
-- 6. 收付款共享表 IN 侧扩列与客户档案加列
-- =============================================================================
-- 6.1 收付款主表 lunaerp_finance_payment 扩列
ALTER TABLE lunaerp_finance_payment ADD COLUMN IF NOT EXISTS receipt_type varchar(16) NULL;
ALTER TABLE lunaerp_finance_payment ADD COLUMN IF NOT EXISTS currency varchar(8) NULL DEFAULT 'CNY';
ALTER TABLE lunaerp_finance_payment ADD COLUMN IF NOT EXISTS exchange_rate numeric(20,8) NULL DEFAULT 1;
ALTER TABLE lunaerp_finance_payment ADD COLUMN IF NOT EXISTS receipt_date date NULL;
ALTER TABLE lunaerp_finance_payment ADD COLUMN IF NOT EXISTS bank_ref_no varchar(64) NULL;
ALTER TABLE lunaerp_finance_payment ADD COLUMN IF NOT EXISTS handler_name varchar(64) NULL;
ALTER TABLE lunaerp_finance_payment ADD COLUMN IF NOT EXISTS approval_status varchar(16) NULL DEFAULT 'PENDING';

COMMENT ON COLUMN lunaerp_finance_payment.receipt_type IS '收款类型（字典 lunaerp_finance_receipt_type：RECEIPT 货款 / ADVANCE 预收；OUT 侧为空）';
COMMENT ON COLUMN lunaerp_finance_payment.currency IS '币种（收款币种，OUT 侧一期人民币可空）';
COMMENT ON COLUMN lunaerp_finance_payment.exchange_rate IS '汇率（收款过账日汇率）';
COMMENT ON COLUMN lunaerp_finance_payment.receipt_date IS '收款日期';
COMMENT ON COLUMN lunaerp_finance_payment.bank_ref_no IS '银行水单/回单号';
COMMENT ON COLUMN lunaerp_finance_payment.handler_name IS '经手人姓名';
COMMENT ON COLUMN lunaerp_finance_payment.approval_status IS '审批状态（02号引擎五态：PENDING/APPROVING/APPROVED/REJECTED/CANCELLED；OUT 侧为空）';

CREATE INDEX IF NOT EXISTS idx_lfp_customer
    ON lunaerp_finance_payment (tenant_id, customer_id);

CREATE INDEX IF NOT EXISTS idx_lfp_approval_status
    ON lunaerp_finance_payment (tenant_id, approval_status);

-- 6.2 收付款核销子表 lunaerp_finance_payment_ref 扩列
ALTER TABLE lunaerp_finance_payment_ref ADD COLUMN IF NOT EXISTS entry_no varchar(64) NULL;
ALTER TABLE lunaerp_finance_payment_ref ADD COLUMN IF NOT EXISTS biz_date date NULL;
ALTER TABLE lunaerp_finance_payment_ref ADD COLUMN IF NOT EXISTS base_amount numeric(18,2) NULL DEFAULT 0;
ALTER TABLE lunaerp_finance_payment_ref ADD COLUMN IF NOT EXISTS ledger_entry_id int8 NULL;

COMMENT ON COLUMN lunaerp_finance_payment_ref.entry_no IS '应收分录单号快照（IN 侧 ref_type=AR_ENTRY 时记录）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.biz_date IS '业务日期快照';
COMMENT ON COLUMN lunaerp_finance_payment_ref.base_amount IS '折算本位币金额（人民币）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.ledger_entry_id IS '关联核销分录ID（引用 lunaerp_finance_ledger_entry.id）';

CREATE INDEX IF NOT EXISTS idx_lfpr_ledger_entry
    ON lunaerp_finance_payment_ref (tenant_id, ledger_entry_id);

-- 6.3 客户表 lunaerp_baseinfo_customer 扩 5 列
ALTER TABLE lunaerp_baseinfo_customer ADD COLUMN IF NOT EXISTS invoice_title varchar(256) NULL;
ALTER TABLE lunaerp_baseinfo_customer ADD COLUMN IF NOT EXISTS invoice_address varchar(512) NULL;
ALTER TABLE lunaerp_baseinfo_customer ADD COLUMN IF NOT EXISTS bank_name varchar(256) NULL;
ALTER TABLE lunaerp_baseinfo_customer ADD COLUMN IF NOT EXISTS bank_account_no varchar(64) NULL;
ALTER TABLE lunaerp_baseinfo_customer ADD COLUMN IF NOT EXISTS credit_days int4 NULL;

COMMENT ON COLUMN lunaerp_baseinfo_customer.invoice_title IS '开票抬头（缺省取公司名）';
COMMENT ON COLUMN lunaerp_baseinfo_customer.invoice_address IS '开票地址电话';
COMMENT ON COLUMN lunaerp_baseinfo_customer.bank_name IS '开户银行';
COMMENT ON COLUMN lunaerp_baseinfo_customer.bank_account_no IS '开户行账号';
COMMENT ON COLUMN lunaerp_baseinfo_customer.credit_days IS '信用账期天数（空=不设账期）';

-- =============================================================================
-- 7. 字典类型与数据：2272–2278 及 CUSTOMER_RECEIPT 审批单据类型
-- =============================================================================
DO $$
BEGIN
    -- DO 块判占用平移避让：确保自 3837 起不与已存在的其他字典数据冲突
    IF EXISTS (
        SELECT 1 FROM system_dict_data 
        WHERE id BETWEEN 3837 AND 3870 
          AND dict_type NOT IN (
              'lunaerp_finance_statement_status', 
              'lunaerp_finance_receipt_type', 
              'lunaerp_finance_ledger_category', 
              'lunaerp_finance_invoice_kind', 
              'lunaerp_finance_tax_rate', 
              'lunaerp_finance_account_type', 
              'lunaerp_finance_statement_confirm_type',
              'lunaerp_approval_biz_type'
          )
    ) THEN
        UPDATE system_dict_data 
        SET id = id + 100 
        WHERE id BETWEEN 3837 AND 3870 
          AND dict_type NOT IN (
              'lunaerp_finance_statement_status', 
              'lunaerp_finance_receipt_type', 
              'lunaerp_finance_ledger_category', 
              'lunaerp_finance_invoice_kind', 
              'lunaerp_finance_tax_rate', 
              'lunaerp_finance_account_type', 
              'lunaerp_finance_statement_confirm_type',
              'lunaerp_approval_biz_type'
          );
    END IF;
END $$;

-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2272, 2273, 2274, 2275, 2276, 2277, 2278)
   OR type IN (
       'lunaerp_finance_statement_status',
       'lunaerp_finance_receipt_type',
       'lunaerp_finance_ledger_category',
       'lunaerp_finance_invoice_kind',
       'lunaerp_finance_tax_rate',
       'lunaerp_finance_account_type',
       'lunaerp_finance_statement_confirm_type'
   );

DELETE FROM system_dict_data WHERE dict_type IN (
       'lunaerp_finance_statement_status',
       'lunaerp_finance_receipt_type',
       'lunaerp_finance_ledger_category',
       'lunaerp_finance_invoice_kind',
       'lunaerp_finance_tax_rate',
       'lunaerp_finance_account_type',
       'lunaerp_finance_statement_confirm_type'
   )
   OR id BETWEEN 3837 AND 3869;

-- 7.1 字典 2272：客户对账单状态
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2272, '客户对账单状态', 'lunaerp_finance_statement_status', 0, 'LunaERP 财务管理-客户对账单状态', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3837, 1, '草稿', 'DRAFT', 'lunaerp_finance_statement_status', 0, 'info', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3838, 2, '已发送', 'SENT', 'lunaerp_finance_statement_status', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3839, 3, '已确认', 'CONFIRMED', 'lunaerp_finance_statement_status', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3840, 4, '已驳回', 'REJECTED', 'lunaerp_finance_statement_status', 0, 'danger', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3841, 5, '已作废', 'VOID', 'lunaerp_finance_statement_status', 0, 'warning', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 7.2 字典 2273：收款类型
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2273, '收款单类型', 'lunaerp_finance_receipt_type', 0, 'LunaERP 财务管理-收款单类型', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3842, 1, '货款收款', 'RECEIPT', 'lunaerp_finance_receipt_type', 0, 'primary', '', '货款', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3843, 2, '预收款', 'ADVANCE', 'lunaerp_finance_receipt_type', 0, 'success', '', '预收', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 7.3 字典 2274：财务分录类别（一期全集 9 值）
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2274, '经营分录类别', 'lunaerp_finance_ledger_category', 0, 'LunaERP 财务管理-经营分录类别（一期全集）', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3844, 1, '发货应收', 'AR_INC', 'lunaerp_finance_ledger_category', 0, 'primary', '', '销售出库审核产生应收', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3845, 2, '发货应收红冲', 'AR_INC_REVERSAL', 'lunaerp_finance_ledger_category', 0, 'danger', '', '发货红冲反转应收', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3846, 3, '收款入账', 'RECEIPT', 'lunaerp_finance_ledger_category', 0, 'success', '', '资金账户收款入账', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3847, 4, '收款红冲', 'RECEIPT_REVERSAL', 'lunaerp_finance_ledger_category', 0, 'danger', '', '收款单红冲冲回资金与核销', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3848, 5, '收款核销', 'RECEIPT_WRITE_OFF', 'lunaerp_finance_ledger_category', 0, 'info', '', '收款核销应收', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3849, 6, '预收转核销', 'ADVANCE_WRITE_OFF', 'lunaerp_finance_ledger_category', 0, 'warning', '', '预收余额转核销应收', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3850, 7, '退货冲减', 'AR_RETURN_REVERSAL', 'lunaerp_finance_ledger_category', 0, 'danger', '', '销售退货冲减应收', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3851, 8, '供应商付款', 'OUT_SUPPLIER', 'lunaerp_finance_ledger_category', 0, 'primary', '', '应付侧供应商付款入账', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3852, 9, '供应商付款红冲', 'OUT_SUPPLIER_REVERSAL', 'lunaerp_finance_ledger_category', 0, 'danger', '', '应付侧供应商付款红冲', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 7.4 字典 2275：发票种类
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2275, '发票种类', 'lunaerp_finance_invoice_kind', 0, 'LunaERP 财务管理-发票种类', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3853, 1, '数电专票', 'ELEC_SPECIAL', 'lunaerp_finance_invoice_kind', 0, 'primary', '', '数电专票', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3854, 2, '数电普票', 'ELEC_NORMAL', 'lunaerp_finance_invoice_kind', 0, 'success', '', '数电普票', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3855, 3, '增值税专票', 'VAT_SPECIAL', 'lunaerp_finance_invoice_kind', 0, 'warning', '', '纸质增值税专用发票', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3856, 4, '增值税普票', 'VAT_NORMAL', 'lunaerp_finance_invoice_kind', 0, 'info', '', '纸质增值税普通发票', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 7.5 字典 2276：开票税率
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2276, '开票税率', 'lunaerp_finance_tax_rate', 0, 'LunaERP 财务管理-开票税率', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3857, 1, '13%', '13', 'lunaerp_finance_tax_rate', 0, 'primary', '', '纺织面料默认税率', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3858, 2, '9%', '9', 'lunaerp_finance_tax_rate', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3859, 3, '6%', '6', 'lunaerp_finance_tax_rate', 0, 'info', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3860, 4, '3%', '3', 'lunaerp_finance_tax_rate', 0, 'warning', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3861, 5, '1%', '1', 'lunaerp_finance_tax_rate', 0, 'warning', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3862, 6, '0%', '0', 'lunaerp_finance_tax_rate', 0, 'default', '', '外贸零税率', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3863, 7, '免税', 'EXEMPT', 'lunaerp_finance_tax_rate', 0, 'default', '', '免税', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 7.6 字典 2277：资金账户类型（域公共）
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2277, '资金账户类型', 'lunaerp_finance_account_type', 0, 'LunaERP 财务管理-资金账户类型（域公共）', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3864, 1, '现金', 'CASH', 'lunaerp_finance_account_type', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3865, 2, '银行存款', 'BANK', 'lunaerp_finance_account_type', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3866, 3, '微信支付', 'WECHAT', 'lunaerp_finance_account_type', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3867, 4, '支付宝', 'ALIPAY', 'lunaerp_finance_account_type', 0, 'info', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 7.7 字典 2278：对账单确认方式
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2278, '对账单确认方式', 'lunaerp_finance_statement_confirm_type', 0, 'LunaERP 财务管理-对账单确认方式', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3868, 1, '线上门户', 'ONLINE_PORTAL', 'lunaerp_finance_statement_confirm_type', 0, 'success', '', '客户登录门户自主确认', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3869, 2, '内部代客', 'ON_BEHALF', 'lunaerp_finance_statement_confirm_type', 0, 'primary', '', '财务线下凭证代客确认', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 7.8 审批单据类型增量：CUSTOMER_RECEIPT
DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_approval_biz_type' AND value = 'CUSTOMER_RECEIPT';
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3870, 6, '客户收款', 'CUSTOMER_RECEIPT', 'lunaerp_approval_biz_type', 0, 'primary', '', '客户收款单审批', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0')
ON CONFLICT DO NOTHING;

COMMIT;
-- @formatter:on

-- =============================================================================
-- 8. 审批默认配置与节点（CUSTOMER_RECEIPT 默认审批流）
-- =============================================================================
DO $$
DECLARE
    v_config_id int8 := 1070;
BEGIN
    DELETE FROM lunaerp_approval_config_node WHERE config_id IN (
        SELECT id FROM lunaerp_approval_config WHERE biz_type = 'CUSTOMER_RECEIPT'
    );
    DELETE FROM lunaerp_approval_config WHERE biz_type = 'CUSTOMER_RECEIPT';

    INSERT INTO lunaerp_approval_config (
        id, tenant_id, biz_type, name, status, remark, creator, create_time, updater, update_time, deleted
    ) VALUES (
        v_config_id, 1, 'CUSTOMER_RECEIPT', '客户收款默认审批流', 'ENABLED', '客户收款默认审批流程（财务主管审批）',
        'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
    );

    INSERT INTO lunaerp_approval_config_node (
        id, tenant_id, config_id, sort, name, node_type, approver_type, approver_ids,
        creator, create_time, updater, update_time, deleted
    ) VALUES (
        10701, 1, v_config_id, 1, '财务主管审批', 'SEQUENTIAL', 'ROLE', '2',
        'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
    );
END $$;

-- =============================================================================
-- 9. 菜单与权限注册
--    10720 客户对账、10721–10727 按钮
--    10745–10749 收款按钮（挂 10740 收付款菜单）
--    10770 发票申请、10771–10774 按钮
--    10780 汇率主档、10781–10782 按钮
-- =============================================================================
-- @formatter:off
BEGIN;

-- 9.1 确保 10700 根目录存在
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10700, '财务', '', 1, 107, 0, '/finance', 'ep:money', NULL, NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    sort = EXCLUDED.sort,
    path = EXCLUDED.path,
    icon = EXCLUDED.icon;

-- 9.2 客户对账菜单与按钮 (10720, 10721–10727)
DELETE FROM system_menu WHERE id IN (10720, 10721, 10722, 10723, 10724, 10725, 10726, 10727);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10720, '客户对账', 'lunaerp:finance:customer-reconciliation:query', 2, 2, 10700, 'customer-reconciliation', 'ep:tickets', 'lunaerp-elmui/finance/customer-reconciliation/index', 'LunaerpElmuiCustomerReconciliation', 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10721, '创建对账单', 'lunaerp:finance:customer-reconciliation:create', 3, 1, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10722, '编辑对账单', 'lunaerp:finance:customer-reconciliation:update', 3, 2, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10723, '删除对账单', 'lunaerp:finance:customer-reconciliation:delete', 3, 3, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10724, '发送客户', 'lunaerp:finance:customer-reconciliation:send', 3, 4, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10725, '作废对账单', 'lunaerp:finance:customer-reconciliation:void', 3, 5, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10726, '代客确认', 'lunaerp:finance:customer-reconciliation:confirm-on-behalf', 3, 6, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10727, '打印对账单', 'lunaerp:finance:customer-reconciliation:print', 3, 7, 10720, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 9.3 收付款菜单确认与收款按钮 (10740 保证存在, 10745–10749)
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10740, '收付款', 'lunaerp:finance:payment:query', 2, 4, 10700, 'payment', 'ep:coin', 'lunaerp-elmui/finance/payment/index', 'LunaerpElmuiPayment', 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    sort = EXCLUDED.sort,
    path = EXCLUDED.path,
    component = EXCLUDED.component;

DELETE FROM system_menu WHERE id IN (10745, 10746, 10747, 10748, 10749);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10745, '收款登记', 'lunaerp:finance:payment:create', 3, 5, 10740, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10746, '修改收款', 'lunaerp:finance:payment:update', 3, 6, 10740, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10747, '删除收款', 'lunaerp:finance:payment:delete', 3, 7, 10740, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10748, '预收转核销', 'lunaerp:finance:payment:writeoff', 3, 8, 10740, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10749, '收款红冲', 'lunaerp:finance:payment:reverse', 3, 9, 10740, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 9.4 发票申请菜单与按钮 (10770, 10771–10774)
DELETE FROM system_menu WHERE id IN (10770, 10771, 10772, 10773, 10774);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10770, '发票申请', 'lunaerp:finance:invoice:query', 2, 7, 10700, 'invoice-application', 'ep:document-copy', 'lunaerp-elmui/finance/invoice-application/index', 'LunaerpElmuiInvoiceApplication', 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10771, '创建申请', 'lunaerp:finance:invoice:create', 3, 1, 10770, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10772, '修改申请', 'lunaerp:finance:invoice:update', 3, 2, 10770, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10773, '删除申请', 'lunaerp:finance:invoice:delete', 3, 3, 10770, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10774, '发票登记与驳回', 'lunaerp:finance:invoice:issue', 3, 4, 10770, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 9.5 汇率主档菜单与按钮 (10780, 10781–10782)
DELETE FROM system_menu WHERE id IN (10780, 10781, 10782);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10780, '汇率', 'lunaerp:finance:exchange-rate:query', 2, 8, 10700, 'exchange-rate', 'ep:trend-charts', 'lunaerp-elmui/finance/exchange-rate/index', 'LunaerpElmuiExchangeRate', 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10781, '新增汇率', 'lunaerp:finance:exchange-rate:create', 3, 1, 10780, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10782, '停用汇率', 'lunaerp:finance:exchange-rate:update', 3, 2, 10780, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 9.6 授权管理员角色与超管角色
DELETE FROM system_role_menu WHERE menu_id IN (
    10720, 10721, 10722, 10723, 10724, 10725, 10726, 10727,
    10745, 10746, 10747, 10748, 10749,
    10770, 10771, 10772, 10773, 10774,
    10780, 10781, 10782
);

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (
    SELECT 10700 AS id UNION ALL
    SELECT 10720 UNION ALL SELECT 10721 UNION ALL SELECT 10722 UNION ALL SELECT 10723 UNION ALL SELECT 10724 UNION ALL SELECT 10725 UNION ALL SELECT 10726 UNION ALL SELECT 10727 UNION ALL
    SELECT 10740 UNION ALL SELECT 10745 UNION ALL SELECT 10746 UNION ALL SELECT 10747 UNION ALL SELECT 10748 UNION ALL SELECT 10749 UNION ALL
    SELECT 10770 UNION ALL SELECT 10771 UNION ALL SELECT 10772 UNION ALL SELECT 10773 UNION ALL SELECT 10774 UNION ALL
    SELECT 10780 UNION ALL SELECT 10781 UNION ALL SELECT 10782
) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2, 1000)
ON CONFLICT DO NOTHING;

COMMIT;
-- @formatter:on
