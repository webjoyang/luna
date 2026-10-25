-- =============================================================================
-- 模块：LunaERP - 财务域 - 收付款与付款申请（付款申请表 + 收付款单共享表 + 核销子表共享表）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 避让声明：错误码 1-030-023（收付款共享段先占 000–007，10 号收款侧避让 100–199）；
--           菜单 10740（按钮 10741–10743，不设 10744 删除付款单按钮——红冲二期）；
--           字典类型 2270、2271；字典数据 3834–3837。
-- 共享件收口与冲突挂账说明：
--   1. lunaerp_finance_payment 与 lunaerp_finance_payment_ref 为财务域收付款共享表：
--      - 主表由 10 号客户对账（IN 侧收款）与本 11 号供应商对账（OUT 侧付款）合并收口；
--      - 本脚本仅建立 OUT 侧本 spec 需要的最小列集（direction、supplier_id/customer_id 二选一、
--        account_id、pay_amount、prepay_used_amount、status COMPLETED/REVERSED 预留）；
--      - 付款单号前缀挂账待两端合并收口时定稿，不提前定死。
--   2. 核销子表 ref_type 一期 OUT 侧仅使用 'PAY_APPLY'，10 号 IN 侧若需核销对账单/合同则扩充 ref_type 值。
--   3. 字典类型 2270/2271：按 shared/GLOSSARY.md 规划取值；02 号审批引擎先行脚本 lunaerp-approval.sql
--      历史占用了 2270/2271，此处先 DELETE 再 INSERT 保证本域字典就位，挂账冲突清单待全域合并收口对齐。
--   4. 付款申请审批状态 approval_status 复用 02 号审批引擎体系；02 号实例状态为四态，业务单据审批态为五态
--      （PENDING 待提交 / APPROVING 审批中 / APPROVED 已通过 / REJECTED 已驳回 / CANCELLED 已撤回）。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 表 3：财务域 - 付款申请单 lunaerp_finance_payment_apply
--    单号 FK+YYYYMMDD+3 位日流水（ADR-0006 模式），租户内全量唯一含软删行；
--    类型 RECON 对账付款（必须挂 CONFIRMED 对账单，累计申请额 ≤ 确认金额）/ PREPAY 预付付款（必填用途）；
--    走 02 号审批引擎链，approval_status 五态；APPROVED 不触发过账（付款登记才扣款记账）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_payment_apply (
    id              int8          NOT NULL,
    apply_no        varchar(32)   NOT NULL,
    apply_type      varchar(16)   NOT NULL,
    supplier_id     int8          NOT NULL,
    supplier_name   varchar(128)  NOT NULL,
    recon_id        int8          NULL,
    recon_no        varchar(32)   NULL,
    amount          numeric(18,2) NOT NULL,
    purpose         varchar(500)  NULL,
    bank_name       varchar(128)  NULL,
    bank_account    varchar(64)   NULL,
    approval_status varchar(16)   NOT NULL DEFAULT 'PENDING',
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_payment_apply') THEN
        ALTER TABLE lunaerp_finance_payment_apply ADD CONSTRAINT pk_lunaerp_finance_payment_apply PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payment_apply_seq;

-- 申请单号租户内全量唯一含软删行
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfpa_apply_no
    ON lunaerp_finance_payment_apply (tenant_id, apply_no);

-- 供应商与审批状态查询索引
CREATE INDEX IF NOT EXISTS idx_lfpa_supplier_approval
    ON lunaerp_finance_payment_apply (tenant_id, supplier_id, approval_status);

-- 关联对账单索引
CREATE INDEX IF NOT EXISTS idx_lfpa_recon_id
    ON lunaerp_finance_payment_apply (tenant_id, recon_id);

COMMENT ON TABLE lunaerp_finance_payment_apply IS '财务管理-付款申请单表';
COMMENT ON COLUMN lunaerp_finance_payment_apply.id IS '付款申请单ID';
COMMENT ON COLUMN lunaerp_finance_payment_apply.apply_no IS '申请单号（FK+YYYYMMDD+3位日流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.apply_type IS '申请类型（字典 lunaerp_payment_apply_type：RECON 对账付款 / PREPAY 预付付款）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.supplier_id IS '供应商ID（lunaerp_baseinfo_supplier.id）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.supplier_name IS '供应商公司名快照';
COMMENT ON COLUMN lunaerp_finance_payment_apply.recon_id IS '关联合同对账单ID（RECON 必填，必须为 CONFIRMED 状态）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.recon_no IS '对账单号快照';
COMMENT ON COLUMN lunaerp_finance_payment_apply.amount IS '本次申请付款金额（元，必须 >0）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.purpose IS '预付用途备注（PREPAY 必填，如纱线采购定金）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.bank_name IS '收款银行名称快照（从供应商档案快照，脱敏位）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.bank_account IS '收款银行账号快照（从供应商档案快照，脱敏位）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.approval_status IS '审批状态（PENDING 草稿待提交 / APPROVING 审批中 / APPROVED 已通过 / REJECTED 已驳回 / CANCELLED 已撤回）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.remark IS '备注';
COMMENT ON COLUMN lunaerp_finance_payment_apply.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_payment_apply.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_payment_apply.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_payment_apply.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_payment_apply.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_payment_apply.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 表 4：财务域 - 收付款单表 lunaerp_finance_payment（共享表）
--    收款（客户）与付款（供应商）共用单据，按 direction=IN/OUT 分型；
--    本 spec 定义 OUT 侧最小列集：payment_no 全量唯一含软删行；
--    两步制出纳登记过账：扣资金账户、写付款单与核销明细、写账户流水与分录账；
--    已登记付款单不可修改删除（凭证），红冲二期。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_payment (
    id                 int8          NOT NULL,
    payment_no         varchar(32)   NOT NULL,
    direction          varchar(16)   NOT NULL,
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

-- 付款单号租户内全量唯一含软删行
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfp_payment_no
    ON lunaerp_finance_payment (tenant_id, payment_no);

-- 供应商索引
CREATE INDEX IF NOT EXISTS idx_lfp_supplier_id
    ON lunaerp_finance_payment (tenant_id, supplier_id);

COMMENT ON TABLE lunaerp_finance_payment IS '财务管理-收付款单主表（收付款共享表）';
COMMENT ON COLUMN lunaerp_finance_payment.id IS '收付款单ID';
COMMENT ON COLUMN lunaerp_finance_payment.payment_no IS '收付款单号（租户内全量唯一含软删行，前缀挂账收口定稿）';
COMMENT ON COLUMN lunaerp_finance_payment.direction IS '收付款方向（字典 lunaerp_finance_payment_direction：IN 收款 / OUT 付款）';
COMMENT ON COLUMN lunaerp_finance_payment.supplier_id IS '供应商ID（direction=OUT 付款时必填）';
COMMENT ON COLUMN lunaerp_finance_payment.customer_id IS '客户ID（direction=IN 收款时必填，10 号 IN 侧使用）';
COMMENT ON COLUMN lunaerp_finance_payment.account_id IS '资金账户ID（lunaerp_finance_account.id）';
COMMENT ON COLUMN lunaerp_finance_payment.pay_amount IS '本次资金实付/实收金额（元，扣减资金账户金额）';
COMMENT ON COLUMN lunaerp_finance_payment.prepay_used_amount IS '预付/预收抵扣金额（元，预付款抵扣额）';
COMMENT ON COLUMN lunaerp_finance_payment.status IS '状态（COMPLETED 登记完成 / REVERSED 红冲取消预留）';
COMMENT ON COLUMN lunaerp_finance_payment.remark IS '备注';
COMMENT ON COLUMN lunaerp_finance_payment.creator IS '登记人/创建者';
COMMENT ON COLUMN lunaerp_finance_payment.create_time IS '登记时间/创建时间';
COMMENT ON COLUMN lunaerp_finance_payment.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_payment.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_payment.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_payment.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 表 5：财务域 - 收付款核销明细表 lunaerp_finance_payment_ref（共享表）
--    记录收付款单对上游申请或应收/应付分录的分配核销明细；
--    OUT 侧 ref_type='PAY_APPLY'，ref_id=付款申请ID，allocate_amount=分配核销额。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_payment_ref (
    id              int8          NOT NULL,
    payment_id      int8          NOT NULL,
    ref_type        varchar(32)   NOT NULL DEFAULT 'PAY_APPLY',
    ref_id          int8          NOT NULL,
    apply_no        varchar(32)   NULL,
    allocate_amount numeric(18,2) NOT NULL DEFAULT 0,
    creator         varchar(64)   NULL DEFAULT '',
    create_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64)   NULL DEFAULT '',
    update_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2          NOT NULL DEFAULT 0,
    tenant_id       int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_payment_ref') THEN
        ALTER TABLE lunaerp_finance_payment_ref ADD CONSTRAINT pk_lunaerp_finance_payment_ref PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payment_ref_seq;

-- 关联业务单据索引
CREATE INDEX IF NOT EXISTS idx_lfpr_ref_id
    ON lunaerp_finance_payment_ref (tenant_id, ref_id);

-- 所属收付款单索引
CREATE INDEX IF NOT EXISTS idx_lfpr_payment_id
    ON lunaerp_finance_payment_ref (tenant_id, payment_id);

COMMENT ON TABLE lunaerp_finance_payment_ref IS '财务管理-收付款核销明细表（共享表）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.id IS '核销明细ID';
COMMENT ON COLUMN lunaerp_finance_payment_ref.payment_id IS '所属收付款单ID（lunaerp_finance_payment.id）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.ref_type IS '核销业务类型（PAY_APPLY 付款申请；10 号扩展 AR_ENTRY 应收分录）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.ref_id IS '关联业务单据ID（付款申请ID / 业务分录ID）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.apply_no IS '关联付款申请单号快照';
COMMENT ON COLUMN lunaerp_finance_payment_ref.allocate_amount IS '本次核销分配金额（元）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_payment_ref.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_payment_ref.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_payment_ref.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_payment_ref.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_payment_ref.tenant_id IS '租户编号';

-- =============================================================================
-- 4. 字典：2270 付款申请类型 + 2271 收付款方向（共享字典）+ 5态审批状态补建
--    注：02 号审批引擎先行脚本 lunaerp-approval.sql 历史占用了 dict_type 2270–2276。
--    此处严禁硬编码 DELETE id 2270/2271，必须按 type 删除；
--    并在插入时使用 DO 块动态检测：若 2270/2271 未被占用则使用 2270/2271，若已占用则顺延取空闲 ID；
--    同时安全补建 5 态单据审批字典 lunaerp_approval_status（PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）。
-- =============================================================================
-- @formatter:off
DO $$
DECLARE
    v_type_id_apply int8;
    v_type_id_direction int8;
    v_type_id_approval int8;
BEGIN
    -- 4.1 清理已有数据（按 type 清理，严禁硬编码 DELETE id 2270/2271 破坏审批引擎）
    DELETE FROM system_dict_data WHERE dict_type IN ('lunaerp_payment_apply_type', 'lunaerp_finance_payment_direction');
    DELETE FROM system_dict_type WHERE type IN ('lunaerp_payment_apply_type', 'lunaerp_finance_payment_direction');

    -- 4.2 付款申请类型 ID 判定（若 2270 未被占用则使用 2270；若被占用则向后顺延空闲 ID）
    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2270) THEN
        v_type_id_apply := 2270;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), 2276), 2276) + 1 INTO v_type_id_apply FROM system_dict_type;
    END IF;

    INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
    VALUES (v_type_id_apply, '付款申请类型', 'lunaerp_payment_apply_type', 0, 'LunaERP 财务管理-付款申请单类型', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0', NULL);

    -- 4.3 收付款方向 ID 判定
    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2271) THEN
        v_type_id_direction := 2271;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), 2276), v_type_id_apply) + 1 INTO v_type_id_direction FROM system_dict_type;
    END IF;

    INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
    VALUES (v_type_id_direction, '收付款方向', 'lunaerp_finance_payment_direction', 0, 'LunaERP 财务管理-收付款单方向', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0', NULL);

    -- 4.4 字典数据
    DELETE FROM system_dict_data WHERE id IN (3834, 3835, 3836, 3837);

    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES (3834, 1, '对账付款', 'RECON', 'lunaerp_payment_apply_type', 0, 'primary', '', NULL, 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0');
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES (3835, 2, '预付付款', 'PREPAY', 'lunaerp_payment_apply_type', 0, 'success', '', NULL, 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0');

    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES (3836, 1, '收款', 'IN', 'lunaerp_finance_payment_direction', 0, 'success', '', NULL, 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0');
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES (3837, 2, '付款', 'OUT', 'lunaerp_finance_payment_direction', 0, 'warning', '', NULL, 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0');

    -- 4.5 补充 5 态单据审批状态字典（若审批引擎未建立 lunaerp_approval_status 则安全补建）
    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE type = 'lunaerp_approval_status') THEN
        SELECT GREATEST(COALESCE(MAX(id), 2276), v_type_id_direction) + 1 INTO v_type_id_approval FROM system_dict_type;

        INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
        VALUES (v_type_id_approval, '业务审批状态', 'lunaerp_approval_status', 0, 'LunaERP 跨业务单据五态审批状态字典', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0', NULL);

        DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_approval_status' OR id BETWEEN 3838 AND 3842;

        INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
        VALUES (3838, 1, '待提交', 'PENDING', 'lunaerp_approval_status', 0, 'info', '', NULL, 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0');
        INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
        VALUES (3839, 2, '审批中', 'APPROVING', 'lunaerp_approval_status', 0, 'primary', '', NULL, 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0');
        INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
        VALUES (3840, 3, '已通过', 'APPROVED', 'lunaerp_approval_status', 0, 'success', '', NULL, 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0');
        INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
        VALUES (3841, 4, '已驳回', 'REJECTED', 'lunaerp_approval_status', 0, 'danger', '', NULL, 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0');
        INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
        VALUES (3842, 5, '已撤回', 'CANCELLED', 'lunaerp_approval_status', 0, 'info', '', NULL, 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0');
    END IF;
END $$;
-- @formatter:on

-- =============================================================================
-- 5. 菜单与权限：10700 财务一级目录（判存插入）+ 10740 收付款（Tab1 付款申请 + Tab2 付款登记）及按钮权限
-- =============================================================================
-- @formatter:off
BEGIN;

-- 5.1 保证 10700 财务一级目录存在（若尚未插入则在此插入，保证脚本独立可跑）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
)
SELECT 10700, '财务', '', 1, 107, 0, '/finance', 'ep:money', NULL, NULL,
       0, '1', '1', '1', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 10700);

-- 5.2 幂等删除已有菜单与按钮
DELETE FROM system_menu WHERE id IN (10740, 10741, 10742, 10743);

-- 二级菜单：10740 收付款
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10740, '收付款', 'lunaerp:finance:payment:query', 2, 4, 10700, 'payment', 'ep:coin', 'lunaerp-elmui/finance/payment/index', 'LunaerpFinancePayment',
    0, '1', '1', '1', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0'
);

-- 按钮权限：10741 发起申请 / 10742 删除申请 / 10743 付款登记（不设删除付款单按钮，红冲二期）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10741, '发起付款申请', 'lunaerp:finance:payment:apply', 3, 1, 10740, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0'
);

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10742, '删除付款申请', 'lunaerp:finance:payment:apply', 3, 2, 10740, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0'
);

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10743, '付款登记', 'lunaerp:finance:payment:register', 3, 3, 10740, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0'
);

-- 5.3 赋权系统管理员角色
CREATE SEQUENCE IF NOT EXISTS system_role_menu_seq;

DO $$
BEGIN
    PERFORM setval('system_role_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role_menu), 1));
END $$;

DELETE FROM system_role_menu WHERE menu_id IN (10700, 10740, 10741, 10742, 10743);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM (
    SELECT DISTINCT id FROM system_role
    WHERE code IN ('super_admin', 'admin') OR id IN (1, 2)
) r
CROSS JOIN (
    SELECT 10700 AS id UNION ALL SELECT 10740 UNION ALL SELECT 10741 UNION ALL SELECT 10742 UNION ALL SELECT 10743
) m;

COMMIT;
-- @formatter:on
