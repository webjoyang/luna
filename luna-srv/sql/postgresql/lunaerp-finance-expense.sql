-- =============================================================================
-- 模块：LunaERP - 财务域 - 员工报销（SQL基建）
-- 工单：[2026-10-19] 01-sql-bootstrap（报销主从表 + 字典 2284 + 菜单 10750 + 审批流配置）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT，字典 data id 自 3900 起判占用平移避让。
-- 避让声明：错误码 1-030-024（报销 000–007）；
--           菜单 10750 报销（按钮 10751–10755）；
--           字典类型 2284；审批单据类型 expense。
-- DEPENDS ON: lunaerp-finance-customer-recon.sql, lunaerp-finance-account-ext.sql, lunaerp-approval.sql
-- =============================================================================

-- =============================================================================
-- 1. 表 1：报销单主表 lunaerp_finance_expense
--    单号 BX-YYYYMMDD-NNN，租户内全量唯一含软删行（ADR-0006 模式）；
--    附件 attachments 存 jsonb infra URL 有序数组；
--    审批通过即同事务统一通道过账（一步制），终态不可改删。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_expense (
    id                  int8          NOT NULL,
    expense_no          varchar(32)   NOT NULL,
    applicant_user_id   int8          NOT NULL,
    applicant_name      varchar(64)   NOT NULL,
    account_id          int8          NOT NULL,
    account_name        varchar(64)   NOT NULL,
    currency            varchar(8)    NOT NULL DEFAULT 'RMB',
    original_total      numeric(18,2) NOT NULL DEFAULT 0,
    exchange_rate       numeric(20,8) NOT NULL DEFAULT 1.00000000,
    base_total          numeric(18,2) NOT NULL DEFAULT 0,
    total_amount        numeric(18,2) NOT NULL DEFAULT 0,
    expense_date        date          NOT NULL,
    approval_status     varchar(16)   NOT NULL DEFAULT 'PENDING',
    submit_time         timestamp     NULL,
    approve_time        timestamp     NULL,
    attachments         jsonb         NOT NULL DEFAULT '[]'::jsonb,
    remark              varchar(1000) NULL,
    creator             varchar(64)   NULL DEFAULT '',
    create_time         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             varchar(64)   NULL DEFAULT '',
    update_time         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             int2          NOT NULL DEFAULT 0,
    tenant_id           int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_expense') THEN
        ALTER TABLE lunaerp_finance_expense ADD CONSTRAINT pk_lunaerp_finance_expense PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_expense_seq;

-- 索引 1: 租户内报销单号全量唯一（含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfe_expense_no
    ON lunaerp_finance_expense (tenant_id, expense_no);

-- 索引 2: 租户+申请人+审批状态（工作台与待办筛选）
CREATE INDEX IF NOT EXISTS idx_lfe_applicant_status
    ON lunaerp_finance_expense (tenant_id, applicant_user_id, approval_status);

-- 索引 3: 租户+付款账户（账户关联合同与流水回溯）
CREATE INDEX IF NOT EXISTS idx_lfe_account_id
    ON lunaerp_finance_expense (tenant_id, account_id);

COMMENT ON TABLE lunaerp_finance_expense IS '财务管理-员工费用报销主表';
COMMENT ON COLUMN lunaerp_finance_expense.id IS '报销单ID';
COMMENT ON COLUMN lunaerp_finance_expense.expense_no IS '报销单号（BX+YYYYMMDD+3位流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_expense.applicant_user_id IS '报销人用户ID（system_users.id）';
COMMENT ON COLUMN lunaerp_finance_expense.applicant_name IS '报销人姓名快照';
COMMENT ON COLUMN lunaerp_finance_expense.account_id IS '付款资金账户ID（lunaerp_finance_account.id）';
COMMENT ON COLUMN lunaerp_finance_expense.account_name IS '付款资金账户名称快照';
COMMENT ON COLUMN lunaerp_finance_expense.currency IS '币种（与账户币种联动一致）';
COMMENT ON COLUMN lunaerp_finance_expense.original_total IS '原币总金额（元，服务端重算 = Σ行金额）';
COMMENT ON COLUMN lunaerp_finance_expense.exchange_rate IS '审批通过日汇率快照（1外币 = 汇率本位币，RMB 为 1.00000000）';
COMMENT ON COLUMN lunaerp_finance_expense.base_total IS '本位币总金额（元，折算后金额）';
COMMENT ON COLUMN lunaerp_finance_expense.total_amount IS '报销总金额（元，账户为RMB时与original_total相同）';
COMMENT ON COLUMN lunaerp_finance_expense.expense_date IS '报销业务日期（取明细行内最大发生日期）';
COMMENT ON COLUMN lunaerp_finance_expense.approval_status IS '审批状态（02号引擎跨切面五态：PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）';
COMMENT ON COLUMN lunaerp_finance_expense.submit_time IS '提交审批时间';
COMMENT ON COLUMN lunaerp_finance_expense.approve_time IS '审批通过时间';
COMMENT ON COLUMN lunaerp_finance_expense.attachments IS '附件列表JSONB（有序文件URL数组，最多10个）';
COMMENT ON COLUMN lunaerp_finance_expense.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_finance_expense.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_expense.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_expense.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_expense.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_expense.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_expense.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 表 2：报销明细行表 lunaerp_finance_expense_item
--    报销费用分项明细（至少 1 行，金额 > 0）；
--    occur_date 不晚于提交日，remark 必填。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_expense_item (
    id                  int8          NOT NULL,
    expense_id          int8          NOT NULL,
    category            varchar(32)   NOT NULL,
    original_amount     numeric(18,2) NOT NULL DEFAULT 0,
    occur_date          date          NOT NULL,
    remark              varchar(1000) NOT NULL,
    creator             varchar(64)   NULL DEFAULT '',
    create_time         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             varchar(64)   NULL DEFAULT '',
    update_time         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             int2          NOT NULL DEFAULT 0,
    tenant_id           int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_expense_item') THEN
        ALTER TABLE lunaerp_finance_expense_item ADD CONSTRAINT pk_lunaerp_finance_expense_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_expense_item_seq;

-- 索引 1: 租户+主单ID（外键关联查询）
CREATE INDEX IF NOT EXISTS idx_lfei_expense_id
    ON lunaerp_finance_expense_item (tenant_id, expense_id);

COMMENT ON TABLE lunaerp_finance_expense_item IS '财务管理-员工费用报销明细行表';
COMMENT ON COLUMN lunaerp_finance_expense_item.id IS '报销明细ID';
COMMENT ON COLUMN lunaerp_finance_expense_item.expense_id IS '报销主单ID（lunaerp_finance_expense.id）';
COMMENT ON COLUMN lunaerp_finance_expense_item.category IS '费用类型（字典 lunaerp_expense_category）';
COMMENT ON COLUMN lunaerp_finance_expense_item.original_amount IS '费用金额（元，必须 > 0）';
COMMENT ON COLUMN lunaerp_finance_expense_item.occur_date IS '费用发生日期（不晚于提交日）';
COMMENT ON COLUMN lunaerp_finance_expense_item.remark IS '费用明细说明（必填）';
COMMENT ON COLUMN lunaerp_finance_expense_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_expense_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_expense_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_expense_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_expense_item.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_expense_item.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 字典：type 2284 报销费用类型 8 值 + 审批单据类型 expense
--    TRAVEL 差旅费 / OFFICE 办公费 / TRANSPORT 运输费 / ENTERTAIN 业务招待费 /
--    RENT 房租 / UTILITY 水电费 / COMM 通讯费 / OTHER 其他
-- =============================================================================
-- @formatter:off
DO $$
DECLARE
    v_type_id_cat int8;
    v_offset int8;
    v_biz_type_id int8;
BEGIN
    -- 3.1 清理已有字典类型与数据
    DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_expense_category';
    DELETE FROM system_dict_type WHERE type = 'lunaerp_expense_category';

    -- 3.2 字典类型 ID 避让判定（2284 判存平移）
    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2284) THEN
        v_type_id_cat := 2284;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), 2284), 2284) + 1 INTO v_type_id_cat FROM system_dict_type;
    END IF;

    -- 插入字典类型
    INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
    VALUES (v_type_id_cat, '报销费用类型', 'lunaerp_expense_category', 0, 'LunaERP 财务管理-报销费用类型（8值）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, NULL);

    -- 3.3 字典数据 ID 避让判定（自 3900 起步）
    IF NOT EXISTS (SELECT 1 FROM system_dict_data WHERE id BETWEEN 3900 AND 3929) THEN
        v_offset := 3900;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), 3929), 3929) + 1 INTO v_offset FROM system_dict_data;
    END IF;

    -- 插入字典数据项：2284 报销费用类型（8值）
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 0, 1, '差旅费', 'TRAVEL', 'lunaerp_expense_category', 0, 'primary', '', '交通与差旅住宿等', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 1, 2, '办公费', 'OFFICE', 'lunaerp_expense_category', 0, 'success', '', '日常办公用品耗材', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 2, 3, '运输费', 'TRANSPORT', 'lunaerp_expense_category', 0, 'info', '', '零担物流及市内配送', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 3, 4, '业务招待费', 'ENTERTAIN', 'lunaerp_expense_category', 0, 'warning', '', '客户接待与商务宴请', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 4, 5, '房租', 'RENT', 'lunaerp_expense_category', 0, 'primary', '', '厂房与办公场地租赁', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 5, 6, '水电费', 'UTILITY', 'lunaerp_expense_category', 0, 'info', '', '厂区办公水电能耗', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 6, 7, '通讯费', 'COMM', 'lunaerp_expense_category', 0, 'success', '', '网络通讯与话费补贴', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 7, 8, '其他', 'OTHER', 'lunaerp_expense_category', 0, 'default', '', '其他杂项报销费用', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 3.4 确保审批单据类型包含 expense
    IF NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'lunaerp_approval_biz_type' AND value = 'expense') THEN
        SELECT GREATEST(COALESCE(MAX(id), v_offset + 8), v_offset + 8) + 1 INTO v_biz_type_id FROM system_dict_data;
        INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
        VALUES (v_biz_type_id, 3, '费用报销', 'expense', 'lunaerp_approval_biz_type', 0, 'warning', '', 'LunaERP 审批引擎-费用报销审批', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);
    END IF;

END $$;
-- @formatter:on

-- =============================================================================
-- 4. 审批引擎：expense 默认启用审批流配置
--    默认单节点逐级审批（财务主管角色审批）；保持幂等。
-- =============================================================================
DO $$
DECLARE
    v_config_id int8;
BEGIN
    DELETE FROM lunaerp_approval_config_node WHERE config_id IN (
        SELECT id FROM lunaerp_approval_config WHERE biz_type = 'expense'
    );
    DELETE FROM lunaerp_approval_config WHERE biz_type = 'expense';

    v_config_id := nextval('lunaerp_approval_config_seq');

    INSERT INTO lunaerp_approval_config (
        id, tenant_id, biz_type, name, status, remark, creator, create_time, updater, update_time, deleted
    ) VALUES (
        v_config_id, 1, 'expense', '员工报销默认审批流', 'ENABLED',
        '员工报销默认单节点审批流程（财务主管审批）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
    );

    INSERT INTO lunaerp_approval_config_node (
        id, tenant_id, config_id, sort, name, node_type, approver_type, approver_ids,
        creator, create_time, updater, update_time, deleted
    ) VALUES (
        nextval('lunaerp_approval_config_node_seq'), 1, v_config_id, 1, '财务主管审批',
        'SEQUENTIAL', 'ROLE', '2', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
    );
END $$;

-- =============================================================================
-- 5. 菜单与权限：10750 报销（挂 10700 财务目录 sort 6）+ 按钮 10751–10755
-- =============================================================================
-- @formatter:off
BEGIN;

-- 5.1 确保 10700 财务一级目录存在
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
)
SELECT 10700, '财务', '', 1, 107, 0, '/finance', 'ep:money', NULL, NULL,
       0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 10700);

-- 5.2 幂等删除已有报销菜单与按钮
DELETE FROM system_menu WHERE id IN (
    10750, 10751, 10752, 10753, 10754, 10755
);

-- 二级菜单：10750 报销
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10750, '报销管理', 'lunaerp:finance:expense:query', 2, 6, 10700, 'expense', 'ep:tickets', 'lunaerp-elmui/finance/expense/index', 'LunaerpFinanceExpense',
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'
);

-- 按钮权限：10751–10755
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES
(10751, '新建报销', 'lunaerp:finance:expense:create', 3, 1, 10750, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10752, '编辑报销', 'lunaerp:finance:expense:update', 3, 2, 10750, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10753, '提交审批', 'lunaerp:finance:expense:submit', 3, 3, 10750, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10754, '删除报销', 'lunaerp:finance:expense:delete', 3, 4, 10750, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10755, '打印报销单', 'lunaerp:finance:expense:print', 3, 5, 10750, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0');

-- 5.3 赋权系统管理员角色
CREATE SEQUENCE IF NOT EXISTS system_role_menu_seq;

DO $$
BEGIN
    PERFORM setval('system_role_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role_menu), 1));
END $$;

DELETE FROM system_role_menu WHERE menu_id IN (
    10750, 10751, 10752, 10753, 10754, 10755
);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM (
    SELECT DISTINCT id FROM system_role
    WHERE code IN ('super_admin', 'admin') OR id IN (1, 2)
) r
CROSS JOIN (
    SELECT 10750 AS id
    UNION ALL SELECT 10751
    UNION ALL SELECT 10752
    UNION ALL SELECT 10753
    UNION ALL SELECT 10754
    UNION ALL SELECT 10755
) m;

COMMIT;
-- @formatter:on
