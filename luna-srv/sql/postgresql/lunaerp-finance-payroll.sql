-- =============================================================================
-- 模块：LunaERP - 财务域 - 工资初稿与次品扣款联动（SQL基建）
-- 工单：[2026-10-19] 01-sql-bootstrap（工资域 5 张表 + 字典 2285/2286 + 菜单 10760 + 审批流配置）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT，字典 data id 自 3930 起判占用平移避让。
-- 避让声明：错误码 1-030-025（工资 000–012）；
--           菜单 10760 工资（按钮 10761–10768）；
--           字典类型 2285/2286；审批单据类型 payroll。
-- DEPENDS ON: lunaerp-finance-customer-recon.sql, lunaerp-finance-account-ext.sql,
--             lunaerp-production-inspection-confirmation.sql, lunaerp-approval.sql
-- =============================================================================

-- =============================================================================
-- 1. 表 1：工资单主表 lunaerp_finance_payroll
--    单号 GZ+YYYYMM+3位月流水，租户内全量唯一含软删行（ADR-0006 模式）；
--    期间部分唯一索引：(tenant_id, period) WHERE deleted=0 AND approval_status <> 'CANCELLED'；
--    两步制发放：审批通过 Handler 为空，发放登记才过账；PAID 状态不落库，由发放批次动态派生。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_payroll (
    id                  int8          NOT NULL,
    payroll_no          varchar(32)   NOT NULL,
    period              char(7)       NOT NULL,
    title               varchar(128)  NOT NULL,
    approval_status     varchar(16)   NOT NULL DEFAULT 'PENDING',
    earning_total       numeric(18,2) NOT NULL DEFAULT 0,
    deduction_total     numeric(18,2) NOT NULL DEFAULT 0,
    net_amount          numeric(18,2) NOT NULL DEFAULT 0,
    submit_time         timestamp     NULL,
    approve_time        timestamp     NULL,
    cancelled_by        varchar(64)   NULL,
    cancelled_at        timestamp     NULL,
    cancel_reason       varchar(500)  NULL,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_payroll') THEN
        ALTER TABLE lunaerp_finance_payroll ADD CONSTRAINT pk_lunaerp_finance_payroll PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payroll_seq;

-- 索引 1: 租户内工资单号全量唯一（含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfp_payroll_no
    ON lunaerp_finance_payroll (tenant_id, payroll_no);

-- 索引 2: 同租户同期间仅允许一张有效工资单（非已删除、非已作废）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfp_tenant_period
    ON lunaerp_finance_payroll (tenant_id, period)
    WHERE deleted = 0 AND approval_status <> 'CANCELLED';

COMMENT ON TABLE lunaerp_finance_payroll IS '财务管理-员工工资单主表';
COMMENT ON COLUMN lunaerp_finance_payroll.id IS '工资单ID';
COMMENT ON COLUMN lunaerp_finance_payroll.payroll_no IS '工资单号（GZ+YYYYMM+3位流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_payroll.period IS '工资期间（YYYY-MM格式年月）';
COMMENT ON COLUMN lunaerp_finance_payroll.title IS '工资单标题（缺省"{period} 工资表"）';
COMMENT ON COLUMN lunaerp_finance_payroll.approval_status IS '审批状态（02号引擎跨切面五态：PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）';
COMMENT ON COLUMN lunaerp_finance_payroll.earning_total IS '应发总计（元，服务端权威重算 = Σ员工应发）';
COMMENT ON COLUMN lunaerp_finance_payroll.deduction_total IS '扣款总计（元，服务端权威重算 = Σ员工扣款）';
COMMENT ON COLUMN lunaerp_finance_payroll.net_amount IS '实发总计（元，服务端权威重算 = 应发总计 - 扣款总计）';
COMMENT ON COLUMN lunaerp_finance_payroll.submit_time IS '提交审批时间';
COMMENT ON COLUMN lunaerp_finance_payroll.approve_time IS '审批通过时间';
COMMENT ON COLUMN lunaerp_finance_payroll.cancelled_by IS '作废操作人';
COMMENT ON COLUMN lunaerp_finance_payroll.cancelled_at IS '作废时间';
COMMENT ON COLUMN lunaerp_finance_payroll.cancel_reason IS '作废原因（作废时必填）';
COMMENT ON COLUMN lunaerp_finance_payroll.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_finance_payroll.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_payroll.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_payroll.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_payroll.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_payroll.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_payroll.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 表 2：员工汇总行表 lunaerp_finance_payroll_employee
--    每个员工在工资单中一行，全员实发不可为负（025_011）；
--    (tenant_id, payroll_id, user_id) 唯一。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_payroll_employee (
    id                  int8          NOT NULL,
    payroll_id          int8          NOT NULL,
    user_id             int8          NOT NULL,
    nickname            varchar(64)   NOT NULL,
    earning_total       numeric(18,2) NOT NULL DEFAULT 0,
    deduction_total     numeric(18,2) NOT NULL DEFAULT 0,
    net_amount          numeric(18,2) NOT NULL DEFAULT 0,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_payroll_employee') THEN
        ALTER TABLE lunaerp_finance_payroll_employee ADD CONSTRAINT pk_lunaerp_finance_payroll_employee PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payroll_employee_seq;

-- 索引 1: 同工资单下员工唯一（含软删行，前缀覆盖 tenant_id, payroll_id 查询）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfpe_payroll_user
    ON lunaerp_finance_payroll_employee (tenant_id, payroll_id, user_id);

COMMENT ON TABLE lunaerp_finance_payroll_employee IS '财务管理-工资单员工汇总行表';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.id IS '员工汇总行ID';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.payroll_id IS '工资单主表ID（lunaerp_finance_payroll.id）';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.user_id IS '员工用户ID（system_users.id）';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.nickname IS '员工昵称快照';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.earning_total IS '员工应发合计（元，服务端权威重算 = Σ款项行应发）';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.deduction_total IS '员工扣款合计（元，服务端权威重算 = Σ款项行扣款）';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.net_amount IS '员工实发合计（元，服务端权威重算 = 应发合计 - 扣款合计，不可为负）';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.remark IS '员工备注说明';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_payroll_employee.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 表 3：款项行表 lunaerp_finance_payroll_line_item
--    统一款项明细（收入 EARNING / 扣款 DEDUCTION）；
--    计件项支持 qty 与 unit_price，amount > 0 服务端严格重算。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_payroll_line_item (
    id                  int8          NOT NULL,
    payroll_id          int8          NOT NULL,
    employee_row_id     int8          NOT NULL,
    item_kind           varchar(16)   NOT NULL,
    item_type           varchar(32)   NOT NULL,
    qty                 numeric(18,2) NULL,
    unit_price          numeric(18,2) NULL,
    amount              numeric(18,2) NOT NULL DEFAULT 0,
    remark              varchar(500)  NULL,
    creator             varchar(64)   NULL DEFAULT '',
    create_time         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             varchar(64)   NULL DEFAULT '',
    update_time         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             int2          NOT NULL DEFAULT 0,
    tenant_id           int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_payroll_line_item') THEN
        ALTER TABLE lunaerp_finance_payroll_line_item ADD CONSTRAINT pk_lunaerp_finance_payroll_line_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payroll_line_item_seq;

-- 索引 1: 租户+工资单ID+员工汇总行ID（级联款项明细查询）
CREATE INDEX IF NOT EXISTS idx_lfpli_payroll_emp
    ON lunaerp_finance_payroll_line_item (tenant_id, payroll_id, employee_row_id);

COMMENT ON TABLE lunaerp_finance_payroll_line_item IS '财务管理-工资单款项明细行表';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.id IS '款项明细行ID';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.payroll_id IS '工资单主表ID（lunaerp_finance_payroll.id）';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.employee_row_id IS '员工汇总行ID（lunaerp_finance_payroll_employee.id）';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.item_kind IS '款项大类（EARNING 收入 / DEDUCTION 扣款）';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.item_type IS '款项类型（EARNING 字典 2285；DEDUCTION 字典 2286）';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.qty IS '计件数量（可空，计件项适用）';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.unit_price IS '计件单价（元，可空，计件项适用）';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.amount IS '款项金额（元，必须 > 0，有数量单价时服务端自动计算）';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.remark IS '款项说明';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_payroll_line_item.tenant_id IS '租户编号';

-- =============================================================================
-- 4. 表 4：次品引用子表 lunaerp_finance_payroll_defect_ref
--    次品扣款逐卷快照追溯（挂 DEDUCTION/DEFECT 款项行）；
--    普通索引（作废释放语义，不建唯一索引）；不回写质检表。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_payroll_defect_ref (
    id                  int8          NOT NULL,
    line_item_id        int8          NOT NULL,
    user_id             int8          NOT NULL,
    nickname            varchar(64)   NOT NULL,
    confirmation_id     int8          NOT NULL,
    confirmation_no     varchar(64)   NOT NULL,
    roll_no             varchar(64)   NOT NULL,
    grade               varchar(16)   NOT NULL,
    confirmed_at        timestamp     NOT NULL,
    deduct_price        numeric(18,2) NOT NULL DEFAULT 0,
    creator             varchar(64)   NULL DEFAULT '',
    create_time         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             varchar(64)   NULL DEFAULT '',
    update_time         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             int2          NOT NULL DEFAULT 0,
    tenant_id           int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_payroll_defect_ref') THEN
        ALTER TABLE lunaerp_finance_payroll_defect_ref ADD CONSTRAINT pk_lunaerp_finance_payroll_defect_ref PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payroll_defect_ref_seq;

-- 索引 1: 租户+款项行ID（查看次品明细行）
CREATE INDEX IF NOT EXISTS idx_lfpdr_line_item
    ON lunaerp_finance_payroll_defect_ref (tenant_id, line_item_id);

-- 索引 2: 租户+质检确认ID（普通索引，指纹排重与作废释放查询）
CREATE INDEX IF NOT EXISTS idx_lfpdr_confirmation
    ON lunaerp_finance_payroll_defect_ref (tenant_id, confirmation_id);

COMMENT ON TABLE lunaerp_finance_payroll_defect_ref IS '财务管理-工资次品扣款引用追溯子表';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.id IS '次品引用ID';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.line_item_id IS '款项明细行ID（lunaerp_finance_payroll_line_item.id）';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.user_id IS '责任工人用户ID';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.nickname IS '责任工人姓名快照';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.confirmation_id IS '质检确认记录ID（lunaerp_production_inspection_confirmation.id）';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.confirmation_no IS '质检确认单号快照';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.roll_no IS '次品布卷号快照';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.grade IS '次品布卷等级快照（如 C/等外品）';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.confirmed_at IS '质检确认时间快照';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.deduct_price IS '扣款单价快照（元/卷）';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_payroll_defect_ref.tenant_id IS '租户编号';

-- =============================================================================
-- 5. 表 5：工资发放批次表 lunaerp_finance_payroll_payment
--    出纳多次分批发放登记，单号 GF+YYYYMMDD+3位流水（全量唯一含软删行）；
--    不挂员工、不入 10/11 共享 payment 表；登记即写分录过账，批次不可改删。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_payroll_payment (
    id                  int8          NOT NULL,
    payment_no          varchar(32)   NOT NULL,
    payroll_id          int8          NOT NULL,
    payroll_no          varchar(32)   NOT NULL,
    period              char(7)       NOT NULL,
    account_id          int8          NOT NULL,
    account_name        varchar(64)   NOT NULL,
    currency            varchar(8)    NOT NULL DEFAULT 'RMB',
    original_amount     numeric(18,2) NOT NULL DEFAULT 0,
    exchange_rate       numeric(20,8) NOT NULL DEFAULT 1.00000000,
    base_amount         numeric(18,2) NOT NULL DEFAULT 0,
    pay_date            date          NOT NULL,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_payroll_payment') THEN
        ALTER TABLE lunaerp_finance_payroll_payment ADD CONSTRAINT pk_lunaerp_finance_payroll_payment PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payroll_payment_seq;

-- 索引 1: 租户内发放单号全量唯一（含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfpp_payment_no
    ON lunaerp_finance_payroll_payment (tenant_id, payment_no);

-- 索引 2: 租户+工资单ID（查询单据发放批次明细）
CREATE INDEX IF NOT EXISTS idx_lfpp_payroll_id
    ON lunaerp_finance_payroll_payment (tenant_id, payroll_id);

COMMENT ON TABLE lunaerp_finance_payroll_payment IS '财务管理-工资发放批次记录表';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.id IS '发放批次ID';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.payment_no IS '发放单号（GF+YYYYMMDD+3位流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.payroll_id IS '关联工资单ID（lunaerp_finance_payroll.id）';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.payroll_no IS '关联工资单号快照';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.period IS '工资期间（YYYY-MM）';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.account_id IS '出纳付款资金账户ID（lunaerp_finance_account.id）';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.account_name IS '付款资金账户名称快照';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.currency IS '付款币种（一期缺省 RMB）';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.original_amount IS '本次发放金额（元，必须 > 0，累计不超过实发合计）';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.exchange_rate IS '汇率快照（1外币 = 汇率本位币，RMB 为 1.00000000）';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.base_amount IS '本位币发放金额（元）';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.pay_date IS '发放业务日期';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.remark IS '备注说明（记录工资单号与批次序号）';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.creator IS '创建者/出纳操作人';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_payroll_payment.tenant_id IS '租户编号';

-- =============================================================================
-- 6. 字典：type 2285 工资收入项、2286 工资扣款项 + 审批单据类型 payroll
--    2285: BASE 基本工资 / PIECE 计件工资 / COMMISSION 提成 / BONUS 奖金 / SUBSIDY 补贴 / OTHER 其他
--    2286: DEFECT 次品扣款(danger) / OTHER 其他扣款
-- =============================================================================
-- @formatter:off
DO $$
DECLARE
    v_type_id_earning int8;
    v_type_id_deduction int8;
    v_offset int8;
    v_biz_type_id int8;
BEGIN
    -- 6.1 清理已有字典类型与数据
    DELETE FROM system_dict_data WHERE dict_type IN (
        'lunaerp_payroll_earning_type',
        'lunaerp_payroll_deduction_type'
    );
    DELETE FROM system_dict_type WHERE type IN (
        'lunaerp_payroll_earning_type',
        'lunaerp_payroll_deduction_type'
    );

    -- 6.2 字典类型 ID 避让判定（2285/2286 判存平移）
    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2285) THEN
        v_type_id_earning := 2285;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), 2285), 2285) + 1 INTO v_type_id_earning FROM system_dict_type;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2286) THEN
        v_type_id_deduction := 2286;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), v_type_id_earning), v_type_id_earning) + 1 INTO v_type_id_deduction FROM system_dict_type;
    END IF;

    -- 插入字典类型
    INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
    VALUES
        (v_type_id_earning, '工资收入款项类型', 'lunaerp_payroll_earning_type', 0, 'LunaERP 财务管理-工资单收入款项字典（6值）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, NULL),
        (v_type_id_deduction, '工资扣款款项类型', 'lunaerp_payroll_deduction_type', 0, 'LunaERP 财务管理-工资单扣款款项字典（2值）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, NULL);

    -- 6.3 字典数据 ID 避让判定（自 3930 起步）
    IF NOT EXISTS (SELECT 1 FROM system_dict_data WHERE id BETWEEN 3930 AND 3959) THEN
        v_offset := 3930;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), 3959), 3959) + 1 INTO v_offset FROM system_dict_data;
    END IF;

    -- 插入字典数据项：2285 工资收入项（6值）
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 0, 1, '基本工资', 'BASE', 'lunaerp_payroll_earning_type', 0, 'primary', '', '底薪标准', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 1, 2, '计件工资', 'PIECE', 'lunaerp_payroll_earning_type', 0, 'success', '', '按件计酬', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 2, 3, '提成', 'COMMISSION', 'lunaerp_payroll_earning_type', 0, 'warning', '', '销售或生产绩效提成', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 3, 4, '奖金', 'BONUS', 'lunaerp_payroll_earning_type', 0, 'danger', '', '全勤或卓越奖金', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 4, 5, '补贴', 'SUBSIDY', 'lunaerp_payroll_earning_type', 0, 'info', '', '岗位或餐费交通津贴', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 5, 6, '其他收入', 'OTHER', 'lunaerp_payroll_earning_type', 0, 'default', '', '杂项补贴', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 插入字典数据项：2286 工资扣款项（2值）
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
        (v_offset + 6, 1, '次品扣款', 'DEFECT', 'lunaerp_payroll_deduction_type', 0, 'danger', '', '质检次品责任扣款', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
        (v_offset + 7, 2, '其他扣款', 'OTHER', 'lunaerp_payroll_deduction_type', 0, 'info', '', '请假或迟到扣除', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 6.4 确保审批单据类型包含 payroll
    IF NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'lunaerp_approval_biz_type' AND value = 'payroll') THEN
        SELECT GREATEST(COALESCE(MAX(id), v_offset + 8), v_offset + 8) + 1 INTO v_biz_type_id FROM system_dict_data;
        INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
        VALUES (v_biz_type_id, 4, '工资单', 'payroll', 'lunaerp_approval_biz_type', 0, 'info', '', 'LunaERP 审批引擎-工资单审批', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);
    END IF;

END $$;
-- @formatter:on

-- =============================================================================
-- 7. 审批引擎：payroll 默认启用审批流配置
--    默认单节点逐级审批（财务主管角色审批）；保持幂等。
-- =============================================================================
DO $$
DECLARE
    v_config_id int8;
BEGIN
    DELETE FROM lunaerp_approval_config_node WHERE config_id IN (
        SELECT id FROM lunaerp_approval_config WHERE biz_type = 'payroll'
    );
    DELETE FROM lunaerp_approval_config WHERE biz_type = 'payroll';

    v_config_id := nextval('lunaerp_approval_config_seq');

    INSERT INTO lunaerp_approval_config (
        id, tenant_id, biz_type, name, status, remark, creator, create_time, updater, update_time, deleted
    ) VALUES (
        v_config_id, 1, 'payroll', '工资单默认审批流', 'ENABLED',
        '工资单默认单节点审批流程（财务主管审批）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
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
-- 8. 菜单与权限：10760 工资（挂 10700 财务目录 sort 7）+ 按钮 10761–10768
-- =============================================================================
-- @formatter:off
BEGIN;

-- 8.1 确保 10700 财务一级目录存在
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
)
SELECT 10700, '财务', '', 1, 107, 0, '/finance', 'ep:money', NULL, NULL,
       0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'
WHERE NOT EXISTS (SELECT 1 FROM system_menu WHERE id = 10700);

-- 8.2 幂等删除已有工资菜单与按钮
DELETE FROM system_menu WHERE id IN (
    10760, 10761, 10762, 10763, 10764, 10765, 10766, 10767, 10768
);

-- 二级菜单：10760 工资
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10760, '工资核算', 'lunaerp:finance:payroll:query', 2, 7, 10700, 'payroll', 'ep:wallet', 'lunaerp-elmui/finance/payroll/index', 'LunaerpFinancePayroll',
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'
);

-- 按钮权限：10761–10768
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES
(10761, '新建工资单', 'lunaerp:finance:payroll:create', 3, 1, 10760, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10762, '编辑工资单', 'lunaerp:finance:payroll:update', 3, 2, 10760, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10763, '提交审批', 'lunaerp:finance:payroll:submit', 3, 3, 10760, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10764, '导入次品', 'lunaerp:finance:payroll:import-defect', 3, 4, 10760, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10765, '发放登记', 'lunaerp:finance:payroll:pay', 3, 5, 10760, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10766, '作废工资单', 'lunaerp:finance:payroll:cancel', 3, 6, 10760, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10767, '删除工资单', 'lunaerp:finance:payroll:delete', 3, 7, 10760, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10768, '打印工资表', 'lunaerp:finance:payroll:print', 3, 8, 10760, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0');

-- 8.3 赋权系统管理员角色
CREATE SEQUENCE IF NOT EXISTS system_role_menu_seq;

DO $$
BEGIN
    PERFORM setval('system_role_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role_menu), 1));
END $$;

DELETE FROM system_role_menu WHERE menu_id IN (
    10760, 10761, 10762, 10763, 10764, 10765, 10766, 10767, 10768
);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM (
    SELECT DISTINCT id FROM system_role
    WHERE code IN ('super_admin', 'admin') OR id IN (1, 2)
) r
CROSS JOIN (
    SELECT 10760 AS id
    UNION ALL SELECT 10761
    UNION ALL SELECT 10762
    UNION ALL SELECT 10763
    UNION ALL SELECT 10764
    UNION ALL SELECT 10765
    UNION ALL SELECT 10766
    UNION ALL SELECT 10767
    UNION ALL SELECT 10768
) m;

COMMIT;
-- @formatter:on
