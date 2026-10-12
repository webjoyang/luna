-- =============================================================================
-- 模块：LunaERP - 客户门户 (Customer Portal)
-- 数据库：PostgreSQL
-- 规范：ruoyi-vue-pro 权限按钮定义
-- 说明：管理端客户档案「客户门户链接」权限按钮（ID: 10150，父菜单 10030）
-- 幂等脚本：先 DELETE 再 INSERT
-- =============================================================================
BEGIN;

DELETE FROM system_menu WHERE id = 10150;

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
) VALUES (
    10150, '客户门户链接', 'lunaerp:baseinfo:customer:portal-token', 3, 50, 10030,
    '', '#', '', NULL,
    0, '1', '1', '1',
    'admin', '2026-10-12 00:00:00', '1', '2026-10-12 00:00:00', '0'
);

COMMIT;

-- =============================================================================
-- 模块：LunaERP - 客户对账单（主表 + 明细行表，满足 10 号契约与门户端点）
-- 表：lunaerp_finance_statement / lunaerp_finance_statement_item
-- =============================================================================

CREATE TABLE IF NOT EXISTS lunaerp_finance_statement (
    id                 int8          NOT NULL,
    statement_no       varchar(32)   NOT NULL,
    customer_id        int8          NOT NULL,
    customer_name      varchar(128)  NOT NULL,
    currency           varchar(16)   NOT NULL DEFAULT 'CNY',
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
    confirm_method     varchar(32)   NULL,
    reject_reason      varchar(500)  NULL,
    reply_remark       varchar(500)  NULL,
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

CREATE UNIQUE INDEX IF NOT EXISTS uk_lfs_statement_no
    ON lunaerp_finance_statement (tenant_id, statement_no);

CREATE INDEX IF NOT EXISTS idx_lfs_customer_status
    ON lunaerp_finance_statement (tenant_id, customer_id, status);

COMMENT ON TABLE lunaerp_finance_statement IS '财务管理-客户对账单主表';
COMMENT ON COLUMN lunaerp_finance_statement.id IS '对账单ID';
COMMENT ON COLUMN lunaerp_finance_statement.statement_no IS '对账单号';
COMMENT ON COLUMN lunaerp_finance_statement.customer_id IS '客户ID';
COMMENT ON COLUMN lunaerp_finance_statement.customer_name IS '客户名称快照';
COMMENT ON COLUMN lunaerp_finance_statement.status IS '状态：DRAFT/SENT/CONFIRMED/REJECTED/VOID';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_type IS '确认类型：ONLINE_PORTAL/ON_BEHALF';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_by_name IS '确认人姓名';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_time IS '确认时间';
COMMENT ON COLUMN lunaerp_finance_statement.confirm_ip IS '确认IP';
COMMENT ON COLUMN lunaerp_finance_statement.reject_reason IS '驳回原因';

CREATE TABLE IF NOT EXISTS lunaerp_finance_statement_item (
    id                 int8          NOT NULL,
    statement_id       int8          NOT NULL,
    line_no            int4          NOT NULL DEFAULT 1,
    ledger_entry_id    int8          NULL,
    direction          varchar(16)   NOT NULL,
    biz_date           date          NOT NULL,
    ref_type           varchar(32)   NULL,
    ref_no             varchar(64)   NULL,
    summary            varchar(255)  NULL,
    debit_amount       numeric(18,2) NOT NULL DEFAULT 0,
    credit_amount      numeric(18,2) NOT NULL DEFAULT 0,
    sign_status        varchar(16)   NULL,
    remark             varchar(500)  NULL,
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

CREATE INDEX IF NOT EXISTS idx_lfsi_statement_id
    ON lunaerp_finance_statement_item (statement_id);

CREATE INDEX IF NOT EXISTS idx_lfsi_ledger_entry_id
    ON lunaerp_finance_statement_item (tenant_id, ledger_entry_id);

COMMENT ON TABLE lunaerp_finance_statement_item IS '财务管理-客户对账单明细行表';
COMMENT ON COLUMN lunaerp_finance_statement_item.id IS '对账单明细ID';
COMMENT ON COLUMN lunaerp_finance_statement_item.statement_id IS '对账单ID';
COMMENT ON COLUMN lunaerp_finance_statement_item.direction IS '方向：DEBIT借方/CREDIT贷方';

