-- =============================================================================
-- 模块：LunaERP - 财务域 - 资金账户扩列 / 账户间转账 / 其他收支（SQL基建）
-- 工单：[2026-10-19] 01-sql-bootstrap（9 新表 + 2 处 ALTER 扩列 + 字典 + 菜单 + 错误码）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + ADD COLUMN IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT，字典 data id 自 3870 起判占用平移避让。
-- 避让声明：错误码 1-030-020（续占 105–110）；
--           菜单 10710 资金账户（按钮 10711–10713/10716/10717）+ 10715 收支总览；
--           字典类型 2282/2283/2287，向 2274 追加 6 值。
-- DEPENDS ON: lunaerp-finance-customer-recon.sql, lunaerp-approval.sql
-- =============================================================================

-- =============================================================================
-- 1. 资金账户表扩列：lunaerp_finance_account +3 列
--    account_no: 账号/微信标识/支付宝标识（@Sensitive(CONTACT)）
--    account_nature: 账户性质（字典 2282：PUBLIC 公户 / PRIVATE 私户，缺省 PUBLIC）
--    remark: 备注
-- =============================================================================
ALTER TABLE lunaerp_finance_account
    ADD COLUMN IF NOT EXISTS account_no varchar(64) NULL,
    ADD COLUMN IF NOT EXISTS account_nature varchar(16) NOT NULL DEFAULT 'PUBLIC',
    ADD COLUMN IF NOT EXISTS remark varchar(500) NULL;

COMMENT ON COLUMN lunaerp_finance_account.account_no IS '账号/微信标识/支付宝标识（BANK/WECHAT/ALIPAY 必填，CASH 可空，@Sensitive(CONTACT)）';
COMMENT ON COLUMN lunaerp_finance_account.account_nature IS '账户性质（字典 lunaerp_finance_account_nature：PUBLIC 公户 / PRIVATE 私户，缺省 PUBLIC）';
COMMENT ON COLUMN lunaerp_finance_account.remark IS '备注说明';

-- =============================================================================
-- 2. 经营分录账表扩列：lunaerp_finance_ledger_entry +2 列
--    party_name: 往来主体名称快照（免 join system_users）
--    ref_no: 来源单号快照（BX/GZ/GF/ZZ/QT 等，流水页免跳查）
--    索引沿用 10 号，不另加。
-- =============================================================================
ALTER TABLE lunaerp_finance_ledger_entry
    ADD COLUMN IF NOT EXISTS party_name varchar(64) NULL,
    ADD COLUMN IF NOT EXISTS ref_no varchar(32) NULL;

COMMENT ON COLUMN lunaerp_finance_ledger_entry.party_name IS '往来主体名称快照（EMPLOYEE 员工昵称/NONE 时空，免 join system_users）';
COMMENT ON COLUMN lunaerp_finance_ledger_entry.ref_no IS '业务来源单号快照（BX/GZ/GF/ZZ/QT 等，流水页免跳查）';

-- =============================================================================
-- 3. 表 1：账户间转账单 lunaerp_finance_transfer
--    单号 ZZ-YYYYMMDD-NNN，租户内全量唯一含软删行（ADR-0006 模式）；
--    双账户 id/name 快照列，status COMPLETED 固定（登记即生效，错账红冲二期）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_transfer (
    id                 int8          NOT NULL,
    transfer_no        varchar(32)   NOT NULL,
    from_account_id    int8          NOT NULL,
    from_account_name  varchar(64)   NOT NULL,
    to_account_id      int8          NOT NULL,
    to_account_name    varchar(64)   NOT NULL,
    currency           varchar(8)    NOT NULL DEFAULT 'RMB',
    amount             numeric(18,2) NOT NULL DEFAULT 0,
    transfer_date      date          NOT NULL,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_transfer') THEN
        ALTER TABLE lunaerp_finance_transfer ADD CONSTRAINT pk_lunaerp_finance_transfer PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_transfer_seq;

-- 索引 1: 租户内转账单号全量唯一（含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lft_transfer_no
    ON lunaerp_finance_transfer (tenant_id, transfer_no);

-- 索引 2: 租户+转账日期
CREATE INDEX IF NOT EXISTS idx_lft_transfer_date
    ON lunaerp_finance_transfer (tenant_id, transfer_date);

COMMENT ON TABLE lunaerp_finance_transfer IS '财务管理-资金账户间转账单表';
COMMENT ON COLUMN lunaerp_finance_transfer.id IS '转账单ID';
COMMENT ON COLUMN lunaerp_finance_transfer.transfer_no IS '转账单号（ZZ+YYYYMMDD+3位流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_transfer.from_account_id IS '转出资金账户ID（lunaerp_finance_account.id）';
COMMENT ON COLUMN lunaerp_finance_transfer.from_account_name IS '转出账户名称快照';
COMMENT ON COLUMN lunaerp_finance_transfer.to_account_id IS '转入资金账户ID（lunaerp_finance_account.id）';
COMMENT ON COLUMN lunaerp_finance_transfer.to_account_name IS '转入账户名称快照';
COMMENT ON COLUMN lunaerp_finance_transfer.currency IS '转账币种（必须同币种转账）';
COMMENT ON COLUMN lunaerp_finance_transfer.amount IS '转账金额（元，必须 > 0）';
COMMENT ON COLUMN lunaerp_finance_transfer.transfer_date IS '转账业务日期';
COMMENT ON COLUMN lunaerp_finance_transfer.status IS '状态（COMPLETED 固定登记即完成，错账红冲二期）';
COMMENT ON COLUMN lunaerp_finance_transfer.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_finance_transfer.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_transfer.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_transfer.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_transfer.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_transfer.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_transfer.tenant_id IS '租户编号';

-- =============================================================================
-- 4. 表 2：其他收支单 lunaerp_finance_other_txn
--    单号 QT-YYYYMMDD-NNN，租户内全量唯一含软删行；
--    direction/category/currency/original_amount/exchange_rate/base_amount；
--    登记即写分录过账，登记后不可修改删除（凭证属性，错账红冲二期）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_other_txn (
    id                 int8          NOT NULL,
    txn_no             varchar(32)   NOT NULL,
    account_id         int8          NOT NULL,
    account_name       varchar(64)   NOT NULL,
    direction          varchar(16)   NOT NULL,
    category           varchar(32)   NOT NULL,
    currency           varchar(8)    NOT NULL DEFAULT 'RMB',
    original_amount    numeric(18,2) NOT NULL DEFAULT 0,
    exchange_rate      numeric(20,8) NOT NULL DEFAULT 1.00000000,
    base_amount        numeric(18,2) NOT NULL DEFAULT 0,
    txn_date           date          NOT NULL,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_other_txn') THEN
        ALTER TABLE lunaerp_finance_other_txn ADD CONSTRAINT pk_lunaerp_finance_other_txn PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_other_txn_seq;

-- 索引 1: 租户内其他收支单号全量唯一（含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfot_txn_no
    ON lunaerp_finance_other_txn (tenant_id, txn_no);

-- 索引 2: 租户+业务日期
CREATE INDEX IF NOT EXISTS idx_lfot_txn_date
    ON lunaerp_finance_other_txn (tenant_id, txn_date);

COMMENT ON TABLE lunaerp_finance_other_txn IS '财务管理-其他收支单表（无单据收支凭证）';
COMMENT ON COLUMN lunaerp_finance_other_txn.id IS '其他收支单ID';
COMMENT ON COLUMN lunaerp_finance_other_txn.txn_no IS '其他收支单号（QT+YYYYMMDD+3位流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_other_txn.account_id IS '收支资金账户ID（lunaerp_finance_account.id）';
COMMENT ON COLUMN lunaerp_finance_other_txn.account_name IS '资金账户名称快照';
COMMENT ON COLUMN lunaerp_finance_other_txn.direction IS '收支方向（DEBIT 流入 / CREDIT 流出）';
COMMENT ON COLUMN lunaerp_finance_other_txn.category IS '收支类别（字典 lunaerp_other_txn_category）';
COMMENT ON COLUMN lunaerp_finance_other_txn.currency IS '币种（外币账户按登记日 SPOT 取价，RMB 为 1.00000000）';
COMMENT ON COLUMN lunaerp_finance_other_txn.original_amount IS '原币金额（元，必须 > 0）';
COMMENT ON COLUMN lunaerp_finance_other_txn.exchange_rate IS '汇率（1外币 = 汇率本位币）';
COMMENT ON COLUMN lunaerp_finance_other_txn.base_amount IS '本位币金额（元）';
COMMENT ON COLUMN lunaerp_finance_other_txn.txn_date IS '收支业务日期';
COMMENT ON COLUMN lunaerp_finance_other_txn.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_finance_other_txn.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_other_txn.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_other_txn.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_other_txn.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_other_txn.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_other_txn.tenant_id IS '租户编号';

-- =============================================================================
-- 5. 字典：type 2282、2283、2287 落地 + 向 2274 经营分录类别追加 6 值
--    2282: lunaerp_finance_account_nature 资金账户性质（公户/私户）
--    2283: lunaerp_finance_account_status 资金账户状态（启用/停用）
--    2287: lunaerp_other_txn_category 其他收支类别（6值）
--    2274 追加: OUT_EXPENSE / OUT_SALARY / TRANSFER_IN / TRANSFER_OUT / IN_OTHER / OUT_OTHER
-- =============================================================================
-- @formatter:off
DO $$
DECLARE
    v_type_id_nature int8;
    v_type_id_status int8;
    v_type_id_other_txn int8;
    v_offset int8;
BEGIN
    -- 5.1 清理新增字典类型与已有数据
    DELETE FROM system_dict_data WHERE dict_type IN (
        'lunaerp_finance_account_nature',
        'lunaerp_finance_account_status',
        'lunaerp_other_txn_category'
    );
    DELETE FROM system_dict_type WHERE type IN (
        'lunaerp_finance_account_nature',
        'lunaerp_finance_account_status',
        'lunaerp_other_txn_category'
    );

    -- 5.2 字典类型 ID 避让判定（2282/2283/2287 判存平移）
    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2282) THEN
        v_type_id_nature := 2282;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), 2282), 2282) + 1 INTO v_type_id_nature FROM system_dict_type;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2283) THEN
        v_type_id_status := 2283;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), v_type_id_nature), v_type_id_nature) + 1 INTO v_type_id_status FROM system_dict_type;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2287) THEN
        v_type_id_other_txn := 2287;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), v_type_id_status), v_type_id_status) + 1 INTO v_type_id_other_txn FROM system_dict_type;
    END IF;

    -- 插入字典类型
    INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
    VALUES
        (v_type_id_nature, '资金账户性质', 'lunaerp_finance_account_nature', 0, 'LunaERP 财务管理-资金账户性质（公户/私户）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, NULL),
        (v_type_id_status, '资金账户状态', 'lunaerp_finance_account_status', 0, 'LunaERP 财务管理-资金账户状态（启用/停用）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, NULL),
        (v_type_id_other_txn, '其他收支类别', 'lunaerp_other_txn_category', 0, 'LunaERP 财务管理-其他收支单类别（6值）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, NULL);

    -- 5.3 字典数据 ID 避让判定（自 3870 起步，判占用平移）
    IF NOT EXISTS (SELECT 1 FROM system_dict_data WHERE id BETWEEN 3870 AND 3899) THEN
        v_offset := 3870;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), 3899), 3899) + 1 INTO v_offset FROM system_dict_data;
    END IF;

    -- 插入字典数据：2282 账户性质（PUBLIC 公户 / PRIVATE 私户）
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 0, 1, '公户', 'PUBLIC', 'lunaerp_finance_account_nature', 0, 'primary', '', '企业法定公户', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 1, 2, '私户', 'PRIVATE', 'lunaerp_finance_account_nature', 0, 'warning', '', '经营辅助私户', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 插入字典数据：2283 账户状态（ENABLE 启用 / DISABLE 停用）
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 2, 1, '启用', 'ENABLE', 'lunaerp_finance_account_status', 0, 'success', '', '正常可用', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 3, 2, '停用', 'DISABLE', 'lunaerp_finance_account_status', 0, 'info', '', '停用受控', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 插入字典数据：2287 其他收支类别（6值）
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 4, 1, '利息收入', 'INTEREST_IN', 'lunaerp_other_txn_category', 0, 'primary', '', '资金利息流入', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 5, 2, '废品收入', 'WASTE_IN', 'lunaerp_other_txn_category', 0, 'success', '', '次废布处置款', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 6, 3, '其他收入', 'OTHER_IN', 'lunaerp_other_txn_category', 0, 'info', '', '营业外杂项流入', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 7, 4, '手续费', 'FEE_OUT', 'lunaerp_other_txn_category', 0, 'warning', '', '银行与支付通道费', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 8, 5, '工本费', 'COST_OUT', 'lunaerp_other_txn_category', 0, 'warning', '', '开户或票据工本支出', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 9, 6, '其他支出', 'OTHER_OUT', 'lunaerp_other_txn_category', 0, 'danger', '', '营业外杂项流出', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 5.4 向 2274 经营分录类别追加 6 值（幂等先删后插，不破坏既有 9 个值）
    DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_finance_ledger_category'
        AND value IN ('OUT_EXPENSE', 'OUT_SALARY', 'TRANSFER_IN', 'TRANSFER_OUT', 'IN_OTHER', 'OUT_OTHER');

    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 10, 10, '员工报销支出', 'OUT_EXPENSE', 'lunaerp_finance_ledger_category', 0, 'warning', '', '费用报销付款分录', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 11, 11, '员工工资发放', 'OUT_SALARY', 'lunaerp_finance_ledger_category', 0, 'warning', '', '工资登记发放分录', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 12, 12, '账户转账转入', 'TRANSFER_IN', 'lunaerp_finance_ledger_category', 0, 'primary', '', '转入方分录', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 13, 13, '账户转账转出', 'TRANSFER_OUT', 'lunaerp_finance_ledger_category', 0, 'info', '', '转出方分录', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 14, 14, '其他收入入账', 'IN_OTHER', 'lunaerp_finance_ledger_category', 0, 'success', '', '其他业务收入分录', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 15, 15, '其他支出流出', 'OUT_OTHER', 'lunaerp_finance_ledger_category', 0, 'danger', '', '其他业务支出分录', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

END $$;
-- @formatter:on

-- =============================================================================
-- 6. 菜单与权限：10710 资金账户（按钮 10711–10713/10716/10717）+ 10715 收支总览
-- =============================================================================
-- @formatter:off
BEGIN;

-- 6.1 确保 10700 财务一级目录存在
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
)
SELECT 10700, '财务', '', 1, 107, 0, '/finance', 'ep:money', NULL, NULL,
       0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 10700);

-- 6.2 确保 10710 资金账户二级菜单存在（若 10 号未交付，则在此安全创建）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
)
SELECT 10710, '资金账户', 'lunaerp:finance:account:query', 2, 1, 10700, 'account', 'ep:credit-card', 'lunaerp-elmui/finance/account/index', 'LunaerpFinanceAccount',
       0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 10710);

-- 6.3 幂等删除已有总览菜单与资金账户按钮
DELETE FROM system_menu WHERE id IN (
    10715,
    10711, 10712, 10713, 10716, 10717
);

-- 二级菜单：10715 收支总览
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10715, '收支总览', 'lunaerp:finance:overview:query', 2, 2, 10700, 'overview', 'ep:data-analysis', 'lunaerp-elmui/finance/overview/index', 'LunaerpFinanceOverview',
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'
);

-- 按钮权限：10710 资金账户按钮
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES
(10711, '新增账户', 'lunaerp:finance:account:create', 3, 1, 10710, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10712, '编辑账户', 'lunaerp:finance:account:update', 3, 2, 10710, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10713, '删除账户', 'lunaerp:finance:account:delete', 3, 3, 10710, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10716, '账户间转账', 'lunaerp:finance:account:transfer', 3, 4, 10710, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10717, '其他收支', 'lunaerp:finance:account:other-txn', 3, 5, 10710, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0');

-- 6.4 校正 10780 汇率菜单 sort 顺序为 9
-- 说明：10 号 customer-reconciliation spec 曾将 10780 写为 sort 8，与 10770 发票申请冲突，按总表校正为 9
UPDATE system_menu SET sort = 9 WHERE id = 10780 AND sort = 8;

-- 6.5 赋权系统管理员角色
CREATE SEQUENCE IF NOT EXISTS system_role_menu_seq;

DO $$
BEGIN
    PERFORM setval('system_role_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role_menu), 1));
END $$;

DELETE FROM system_role_menu WHERE menu_id IN (
    10710, 10711, 10712, 10713, 10715, 10716, 10717
);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM (
    SELECT DISTINCT id FROM system_role
    WHERE code IN ('super_admin', 'admin') OR id IN (1, 2)
) r
CROSS JOIN (
    SELECT 10710 AS id
    UNION ALL SELECT 10711
    UNION ALL SELECT 10712
    UNION ALL SELECT 10713
    UNION ALL SELECT 10715
    UNION ALL SELECT 10716
    UNION ALL SELECT 10717
) m;

COMMIT;
-- @formatter:on
