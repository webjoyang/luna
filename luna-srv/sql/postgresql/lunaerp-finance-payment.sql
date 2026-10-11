-- =============================================================================
-- 模块：LunaERP - 财务管理 - 付款申请与收付款（付款申请 + 共享收付款单 + 核销子表）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 避让声明：错误码 1-030-023（000–007 先占）；菜单 10740（收付款）及按钮 10741–10743；
--           字典类型 2270（lunaerp_payment_apply_type）、2271（lunaerp_finance_payment_direction），
--           字典数据 3833–3836。
-- 共享表声明：lunaerp_finance_payment 与 lunaerp_finance_payment_ref 为财务域收付款共享表。
--             本脚本按供应商对账与付款（OUT 侧）定义最小列集。客户对账（IN 侧）合并时在同一表扩列
--             （例如 customer_id 等列在此已预置，方向由 direction 区分：IN 收款 / OUT 付款）。
-- =============================================================================

-- =============================================================================
-- 1. 新表 3：财务域 - 付款申请单 lunaerp_finance_payment_apply
--    单号 FK+YYYYMMDD+3位日流水，租户内全量唯一含软删行（ADR-0006/0015）；
--    类型：RECON 对账付款 / PREPAY 预付付款；走 02 号审批引擎。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_payment_apply (
    id                 int8          NOT NULL,
    apply_no           varchar(32)   NOT NULL,
    apply_type         varchar(16)   NOT NULL,
    supplier_id        int8          NOT NULL,
    supplier_name      varchar(128)  NOT NULL,
    recon_id           int8          NULL,
    recon_no           varchar(32)   NULL,
    amount             numeric(18,2) NOT NULL,
    purpose            varchar(500)  NULL,
    bank_name          varchar(128)  NULL,
    bank_account       varchar(64)   NULL,
    approval_status    varchar(16)   NOT NULL DEFAULT 'PENDING',
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_payment_apply') THEN
        ALTER TABLE lunaerp_finance_payment_apply ADD CONSTRAINT pk_lunaerp_finance_payment_apply PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payment_apply_seq;

-- 申请单号租户内全量唯一（普通唯一索引，不带 WHERE deleted=0，含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfpa_apply_no
    ON lunaerp_finance_payment_apply (tenant_id, apply_no);

-- 供应商与审批状态查询索引
CREATE INDEX IF NOT EXISTS idx_lfpa_lookup
    ON lunaerp_finance_payment_apply (tenant_id, supplier_id, approval_status);

-- 挂靠对账单查询索引
CREATE INDEX IF NOT EXISTS idx_lfpa_recon
    ON lunaerp_finance_payment_apply (tenant_id, recon_id);

COMMENT ON TABLE lunaerp_finance_payment_apply IS '财务管理-付款申请单表';
COMMENT ON COLUMN lunaerp_finance_payment_apply.id IS '付款申请ID';
COMMENT ON COLUMN lunaerp_finance_payment_apply.apply_no IS '付款申请单号（FK+YYYYMMDD+3位日流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.apply_type IS '申请类型（字典 lunaerp_payment_apply_type：RECON 对账付款 / PREPAY 预付付款）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.supplier_id IS '供应商ID（关联 lunaerp_baseinfo_supplier.id）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.supplier_name IS '供应商公司名快照';
COMMENT ON COLUMN lunaerp_finance_payment_apply.recon_id IS '关联对账单ID（RECON 类型必填，关联 lunaerp_finance_supplier_recon.id）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.recon_no IS '关联对账单号快照';
COMMENT ON COLUMN lunaerp_finance_payment_apply.amount IS '本次申请付款金额（元，必须 >0）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.purpose IS '付款用途（PREPAY 类型必填，如纱线采购定金）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.bank_name IS '开户行名称快照（展示用，脱敏字段）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.bank_account IS '银行账号快照（展示用，脱敏字段）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.approval_status IS '审批状态（02号引擎状态：PENDING待提交/APPROVING审批中/APPROVED已通过/REJECTED已驳回/CANCELLED已撤回）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.remark IS '备注';
COMMENT ON COLUMN lunaerp_finance_payment_apply.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_payment_apply.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_payment_apply.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_payment_apply.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_payment_apply.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 新表 4：财务域 - 收付款单主表 lunaerp_finance_payment（共享表）
--    单号 PAY+YYYYMMDD+3位日流水，租户内全量唯一含软删行；
--    本表为应收（IN 客户收款）与应付（OUT 供应商付款）共享表；
--    本脚本预置 OUT 侧最小字段集及 IN 侧预留字段（如 customer_id）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_payment (
    id                 int8          NOT NULL,
    payment_no         varchar(32)   NOT NULL,
    direction          varchar(16)   NOT NULL DEFAULT 'OUT',
    supplier_id        int8          NULL,
    customer_id        int8          NULL,
    account_id         int8          NOT NULL,
    pay_amount         numeric(18,2) NOT NULL DEFAULT 0,
    prepay_used_amount numeric(18,2) NOT NULL DEFAULT 0,
    status             varchar(16)   NOT NULL DEFAULT 'COMPLETED',
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_payment') THEN
        ALTER TABLE lunaerp_finance_payment ADD CONSTRAINT pk_lunaerp_finance_payment PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payment_seq;

-- 付款单号租户内全量唯一（普通唯一索引，不带 WHERE deleted=0，含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfp_payment_no
    ON lunaerp_finance_payment (tenant_id, payment_no);

-- 供应商与付款查询索引
CREATE INDEX IF NOT EXISTS idx_lfp_supplier
    ON lunaerp_finance_payment (tenant_id, supplier_id);

COMMENT ON TABLE lunaerp_finance_payment IS '财务管理-收付款单主表（应收IN/应付OUT共享表）';
COMMENT ON COLUMN lunaerp_finance_payment.id IS '收付款单ID';
COMMENT ON COLUMN lunaerp_finance_payment.payment_no IS '收付款单号（PAY+YYYYMMDD+3位日流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_payment.direction IS '收付款方向（字典 lunaerp_finance_payment_direction：IN 收款 / OUT 付款）';
COMMENT ON COLUMN lunaerp_finance_payment.supplier_id IS '供应商ID（OUT 付款时使用，关联 lunaerp_baseinfo_supplier.id）';
COMMENT ON COLUMN lunaerp_finance_payment.customer_id IS '客户ID（IN 收款时使用，关联 lunaerp_baseinfo_customer.id）';
COMMENT ON COLUMN lunaerp_finance_payment.account_id IS '资金账户ID（关联 lunaerp_finance_account.id）';
COMMENT ON COLUMN lunaerp_finance_payment.pay_amount IS '本次资金实收/实付金额（元）';
COMMENT ON COLUMN lunaerp_finance_payment.prepay_used_amount IS '预收/预付款抵扣金额（元，缺省0）';
COMMENT ON COLUMN lunaerp_finance_payment.status IS '状态（COMPLETED 已登记过账 / REVERSED 已红冲预留）';
COMMENT ON COLUMN lunaerp_finance_payment.remark IS '备注';
COMMENT ON COLUMN lunaerp_finance_payment.creator IS '创建者/登记人';
COMMENT ON COLUMN lunaerp_finance_payment.create_time IS '创建时间/登记时间';
COMMENT ON COLUMN lunaerp_finance_payment.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_payment.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_payment.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_payment.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 新表 5：财务域 - 收付款核销子表 lunaerp_finance_payment_ref（共享表）
--    记录收付款单与上游申请/发货明细之间的核销分配关系；
--    OUT 侧 ref_type=PAY_APPLY 核销付款申请；IN 侧 ref_type=AR_ENTRY 核销应收分录。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_payment_ref (
    id                 int8          NOT NULL,
    payment_id         int8          NOT NULL,
    ref_type           varchar(32)   NOT NULL DEFAULT 'PAY_APPLY',
    ref_id             int8          NOT NULL,
    apply_no           varchar(32)   NULL,
    allocate_amount    numeric(18,2) NOT NULL DEFAULT 0,
    creator            varchar(64)   NULL DEFAULT '',
    create_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater            varchar(64)   NULL DEFAULT '',
    update_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            int2          NOT NULL DEFAULT 0,
    tenant_id          int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_payment_ref') THEN
        ALTER TABLE lunaerp_finance_payment_ref ADD CONSTRAINT pk_lunaerp_finance_payment_ref PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payment_ref_seq;

-- 核销子表付款单索引
CREATE INDEX IF NOT EXISTS idx_lfpr_payment
    ON lunaerp_finance_payment_ref (tenant_id, payment_id);

-- 核销子表目标单据引用索引
CREATE INDEX IF NOT EXISTS idx_lfpr_ref
    ON lunaerp_finance_payment_ref (tenant_id, ref_id);

COMMENT ON TABLE lunaerp_finance_payment_ref IS '财务管理-收付款核销子表（应收IN/应付OUT共享表）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.id IS '核销明细ID';
COMMENT ON COLUMN lunaerp_finance_payment_ref.payment_id IS '所属收付款单ID（关联 lunaerp_finance_payment.id）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.ref_type IS '核销目标类型（PAY_APPLY 付款申请 / AR_ENTRY 应收分录）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.ref_id IS '核销目标记录ID（付款申请ID / 经营分录ID）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.apply_no IS '付款申请单号快照（PAY_APPLY 时记录）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.allocate_amount IS '本次核销分配金额（元）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_payment_ref.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_payment_ref.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_payment_ref.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_payment_ref.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.tenant_id IS '租户编号';

-- =============================================================================
-- 4. 字典类型与数据：2270（付款申请类型）、2271（收付款方向共享字典）
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2270, 2271)
    OR type IN ('lunaerp_payment_apply_type', 'lunaerp_finance_payment_direction');

DELETE FROM system_dict_data WHERE dict_type IN ('lunaerp_payment_apply_type', 'lunaerp_finance_payment_direction')
    OR id BETWEEN 3833 AND 3836;

-- 4.1 付款申请类型
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2270, '付款申请类型', 'lunaerp_payment_apply_type', 0, 'LunaERP 财务管理-付款申请类型', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3833, 1, '对账付款', 'RECON', 'lunaerp_payment_apply_type', 0, 'primary', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3834, 2, '预付付款', 'PREPAY', 'lunaerp_payment_apply_type', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 4.2 收付款方向（共享字典）
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2271, '收付款方向', 'lunaerp_finance_payment_direction', 0, 'LunaERP 财务管理-收付款方向（共享字典）', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3835, 1, '收款', 'IN', 'lunaerp_finance_payment_direction', 0, 'success', '', '应收侧收款', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3836, 2, '付款', 'OUT', 'lunaerp_finance_payment_direction', 0, 'warning', '', '应付侧付款', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 5. 菜单与权限：10740（收付款）及按钮 10741–10743
-- =============================================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'system_role_menu' AND column_name = 'id' AND column_default IS NULL
    ) AND EXISTS (
        SELECT 1 FROM pg_class WHERE relname = 'system_role_menu_seq'
    ) THEN
        ALTER TABLE system_role_menu ALTER COLUMN id SET DEFAULT nextval('system_role_menu_seq');
    END IF;
END $$;

-- @formatter:off
BEGIN;

-- 5.1 确保 10700 根目录存在
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10700, '财务', '', 1, 107, 0, '/finance', 'ep:money', NULL, NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    sort = EXCLUDED.sort,
    path = EXCLUDED.path,
    icon = EXCLUDED.icon;

-- 5.2 收付款菜单与按钮
DELETE FROM system_menu WHERE id IN (10740, 10741, 10742, 10743);

-- 收付款菜单（Tab1 付款申请 + Tab2 付款登记）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10740, '收付款', 'lunaerp:finance:payment:query', 2, 4, 10700, 'payment', 'ep:coin', 'lunaerp-elmui/finance/payment/index', 'LunaerpElmuiPayment', 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 按钮：发起申请 / 删除申请 / 付款登记（付款单写账凭证不可改删，红冲二期，不设删除付款单按钮）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10741, '发起申请', 'lunaerp:finance:payment:apply', 3, 1, 10740, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10742, '删除申请', 'lunaerp:finance:payment:apply', 3, 2, 10740, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10743, '付款登记', 'lunaerp:finance:payment:register', 3, 3, 10740, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 5.3 授权角色（管理员与角色 1 / 1000）
DELETE FROM system_role_menu WHERE menu_id IN (10740, 10741, 10742, 10743);

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (
    SELECT 10700 AS id UNION ALL
    SELECT 10740 UNION ALL SELECT 10741 UNION ALL SELECT 10742 UNION ALL SELECT 10743
) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2, 1000)
ON CONFLICT DO NOTHING;

COMMIT;
-- @formatter:on
