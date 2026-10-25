-- =============================================================================
-- 模块：LunaERP - 业务管理域 - 销售退货（SQL基建）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT，字典 data id 自 3850 起判占用平移避让。
-- 避让声明：错误码 1-030-037（销售退货，000–014）；
--           菜单 10141（按钮 10142–10149，挂 10100 业务管理/销售目录 sort 4）；
--           字典类型 2279–2281；审批单据类型 sales_return。
-- DEPENDS ON: lunaerp-business-shipment.sql, lunaerp-stock.sql, lunaerp-approval.sql
-- =============================================================================

-- =============================================================================
-- 1. 表 1：销售退货主表 lunaerp_business_sales_return
--    单号 RT-YYYYMMDD-NNN，租户内全量唯一含软删行；
--    双列状态机（业务五态 DRAFT/APPROVED/RECEIVED/POSTED/CANCELLED + 02 审批五态）；
--    一单严格对一发货单（COMPLETED）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_sales_return (
    id                int8          NOT NULL,
    return_no         varchar(32)   NOT NULL,
    status            varchar(16)   NOT NULL DEFAULT 'DRAFT',
    approval_status   varchar(16)   NOT NULL DEFAULT 'PENDING',
    shipment_id       int8          NOT NULL,
    shipment_no       varchar(32)   NOT NULL,
    customer_id       int8          NOT NULL,
    customer_name     varchar(128)  NOT NULL,
    sales_contract_id int8          NULL,
    contract_no       varchar(32)   NULL,
    warehouse_id      int8          NOT NULL,
    warehouse_name    varchar(128)  NOT NULL,
    currency          varchar(8)    NOT NULL DEFAULT 'RMB',
    total_amount      numeric(18,2) NOT NULL DEFAULT 0,
    total_pieces      int4          NOT NULL DEFAULT 0,
    stock_in_id       int8          NULL,
    arrived_by        int8          NULL,
    arrived_at        timestamp     NULL,
    posted_by         int8          NULL,
    posted_at         timestamp     NULL,
    cancelled_by      int8          NULL,
    cancelled_at      timestamp     NULL,
    cancel_reason     varchar(500)  NULL,
    return_date       date          NOT NULL DEFAULT CURRENT_DATE,
    remark            varchar(1000) NULL,
    creator           varchar(64)   NULL DEFAULT '',
    create_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater           varchar(64)   NULL DEFAULT '',
    update_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           int2          NOT NULL DEFAULT 0,
    tenant_id         int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_business_sales_return') THEN
        ALTER TABLE lunaerp_business_sales_return ADD CONSTRAINT pk_lunaerp_business_sales_return PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_sales_return_seq;

-- 索引 1: 租户下单号全量唯一（含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lbsr_tenant_return_no
    ON lunaerp_business_sales_return (tenant_id, return_no);

-- 索引 2: 租户+客户+状态筛选
CREATE INDEX IF NOT EXISTS idx_lbsr_tenant_cust_status
    ON lunaerp_business_sales_return (tenant_id, customer_id, status);

-- 索引 3: 租户+发货单回指
CREATE INDEX IF NOT EXISTS idx_lbsr_tenant_shipment_id
    ON lunaerp_business_sales_return (tenant_id, shipment_id);

-- 索引 4: 租户+业务状态+审批状态复合索引
CREATE INDEX IF NOT EXISTS idx_lbsr_tenant_status_approval
    ON lunaerp_business_sales_return (tenant_id, status, approval_status);

COMMENT ON TABLE lunaerp_business_sales_return IS '业务管理-客户销售退货主表（ADR-0042）';
COMMENT ON COLUMN lunaerp_business_sales_return.id IS '退货单ID';
COMMENT ON COLUMN lunaerp_business_sales_return.return_no IS '退货单号（RT-YYYYMMDD-NNN，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_business_sales_return.status IS '业务状态（字典 lunaerp_sales_return_status：DRAFT/APPROVED/RECEIVED/POSTED/CANCELLED）';
COMMENT ON COLUMN lunaerp_business_sales_return.approval_status IS '审批状态（02号引擎跨切面五态：PENDING/APPROVING/APPROVED/REJECTED/CANCELLED）';
COMMENT ON COLUMN lunaerp_business_sales_return.shipment_id IS '关联发货单ID（lunaerp_business_shipment.id，必选 COMPLETED 发货单）';
COMMENT ON COLUMN lunaerp_business_sales_return.shipment_no IS '发货单号快照';
COMMENT ON COLUMN lunaerp_business_sales_return.customer_id IS '客户ID（lunaerp_baseinfo_customer.id，头单快照）';
COMMENT ON COLUMN lunaerp_business_sales_return.customer_name IS '客户名称快照';
COMMENT ON COLUMN lunaerp_business_sales_return.sales_contract_id IS '销售合同ID（lunaerp_business_sales_contract.id，头单快照，可空）';
COMMENT ON COLUMN lunaerp_business_sales_return.contract_no IS '销售合同号快照';
COMMENT ON COLUMN lunaerp_business_sales_return.warehouse_id IS '退货入库仓库ID（lunaerp_stock_warehouse.id，缺省=原发货仓库）';
COMMENT ON COLUMN lunaerp_business_sales_return.warehouse_name IS '退货仓库名称快照';
COMMENT ON COLUMN lunaerp_business_sales_return.currency IS '币种（RMB/USD/EUR/HKD，头单快照）';
COMMENT ON COLUMN lunaerp_business_sales_return.total_amount IS '退货总金额（元，服务端权威重算 = Σ行金额）';
COMMENT ON COLUMN lunaerp_business_sales_return.total_pieces IS '退货总匹数（服务端权威重算 = Σ行匹数）';
COMMENT ON COLUMN lunaerp_business_sales_return.stock_in_id IS '回指入库单ID（lunaerp_stock_in.id，审批通过回写）';
COMMENT ON COLUMN lunaerp_business_sales_return.arrived_by IS '到货质检确认人ID';
COMMENT ON COLUMN lunaerp_business_sales_return.arrived_at IS '到货质检确认时间（RECEIVED 状态落）';
COMMENT ON COLUMN lunaerp_business_sales_return.posted_by IS '财务过账确认人ID';
COMMENT ON COLUMN lunaerp_business_sales_return.posted_at IS '财务过账确认时间（POSTED 状态落）';
COMMENT ON COLUMN lunaerp_business_sales_return.cancelled_by IS '作废人ID';
COMMENT ON COLUMN lunaerp_business_sales_return.cancelled_at IS '作废时间';
COMMENT ON COLUMN lunaerp_business_sales_return.cancel_reason IS '作废原因说明';
COMMENT ON COLUMN lunaerp_business_sales_return.return_date IS '退货单据日期（缺省当天）';
COMMENT ON COLUMN lunaerp_business_sales_return.remark IS '头级总备注';
COMMENT ON COLUMN lunaerp_business_sales_return.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_sales_return.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_sales_return.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_sales_return.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_sales_return.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_sales_return.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 表 2：销售退货明细行 lunaerp_business_sales_return_item
--    强制发货明细血缘（shipment_item_id 必填），防老系统空血缘 P0；
--    双量管理：return_qty（计费量）与 restock_qty/scrap_qty（实物到货质检量）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_sales_return_item (
    id                int8          NOT NULL,
    return_id         int8          NOT NULL,
    shipment_item_id  int8          NOT NULL,
    product_id        int8          NOT NULL,
    product_code      varchar(64)   NOT NULL,
    product_name      varchar(128)  NOT NULL,
    customer_color_no varchar(64)   NULL,
    color_name        varchar(64)   NULL,
    vat_no            varchar(64)   NULL,
    return_qty        numeric(18,2) NOT NULL DEFAULT 0,
    pieces            int4          NOT NULL DEFAULT 1,
    unit              varchar(8)    NOT NULL DEFAULT 'KG',
    unit_price        numeric(18,4) NOT NULL DEFAULT 0,
    amount            numeric(18,2) NOT NULL DEFAULT 0,
    return_reason     varchar(16)   NOT NULL,
    restock_qty       numeric(18,2) NULL DEFAULT 0,
    scrap_qty         numeric(18,2) NULL DEFAULT 0,
    remark            varchar(500)  NULL,
    creator           varchar(64)   NULL DEFAULT '',
    create_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater           varchar(64)   NULL DEFAULT '',
    update_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           int2          NOT NULL DEFAULT 0,
    tenant_id         int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_business_sales_return_item') THEN
        ALTER TABLE lunaerp_business_sales_return_item ADD CONSTRAINT pk_lunaerp_business_sales_return_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_sales_return_item_seq;

-- 索引 1: 租户+退货单外键反查
CREATE INDEX IF NOT EXISTS idx_lbsri_tenant_return_id
    ON lunaerp_business_sales_return_item (tenant_id, return_id);

-- 索引 2: 租户+发货明细外键（防超退聚合核心索引，P95 < 1s 守护）
CREATE INDEX IF NOT EXISTS idx_lbsri_tenant_shipment_item_id
    ON lunaerp_business_sales_return_item (tenant_id, shipment_item_id);

COMMENT ON TABLE lunaerp_business_sales_return_item IS '业务管理-客户销售退货明细行（强制发货血缘）';
COMMENT ON COLUMN lunaerp_business_sales_return_item.id IS '退货明细ID';
COMMENT ON COLUMN lunaerp_business_sales_return_item.return_id IS '所属退货单ID（lunaerp_business_sales_return.id）';
COMMENT ON COLUMN lunaerp_business_sales_return_item.shipment_item_id IS '发货明细行血缘ID（lunaerp_business_shipment_item.id，强制必填）';
COMMENT ON COLUMN lunaerp_business_sales_return_item.product_id IS '产品ID（lunaerp_baseinfo_product.id）';
COMMENT ON COLUMN lunaerp_business_sales_return_item.product_code IS '产品编号快照';
COMMENT ON COLUMN lunaerp_business_sales_return_item.product_name IS '产品名称快照';
COMMENT ON COLUMN lunaerp_business_sales_return_item.customer_color_no IS '客户色号快照';
COMMENT ON COLUMN lunaerp_business_sales_return_item.color_name IS '颜色名称快照';
COMMENT ON COLUMN lunaerp_business_sales_return_item.vat_no IS '缸号快照';
COMMENT ON COLUMN lunaerp_business_sales_return_item.return_qty IS '退货计费数量（手填，参与防超退守卫）';
COMMENT ON COLUMN lunaerp_business_sales_return_item.pieces IS '退货匹数（缺省1）';
COMMENT ON COLUMN lunaerp_business_sales_return_item.unit IS '计费单位（KG/M，从发货行快照）';
COMMENT ON COLUMN lunaerp_business_sales_return_item.unit_price IS '退货单价（元，缺省带原发货单价，允许改折让价）';
COMMENT ON COLUMN lunaerp_business_sales_return_item.amount IS '行金额（元，服务端重算 = return_qty * unit_price）';
COMMENT ON COLUMN lunaerp_business_sales_return_item.return_reason IS '行级退货原因（字典 lunaerp_sales_return_reason：QUALITY/WRONG/EXCESS/CUSTOMER/OTHER）';
COMMENT ON COLUMN lunaerp_business_sales_return_item.restock_qty IS '可二次入库数量（到货质检服务端重算：RESTOCK 卷按行单位汇总）';
COMMENT ON COLUMN lunaerp_business_sales_return_item.scrap_qty IS '废品数量（到货质检服务端落：SCRAP 卷按行单位汇总）';
COMMENT ON COLUMN lunaerp_business_sales_return_item.remark IS '明细备注';
COMMENT ON COLUMN lunaerp_business_sales_return_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_sales_return_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_sales_return_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_sales_return_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_sales_return_item.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_sales_return_item.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 表 3：销售退货卷明细 lunaerp_business_sales_return_roll
--    可选发货卷血缘（勾选原卷带回快照，散退手填卷号）；
--    到货时逐卷判定质检结果（RESTOCK/SCRAP），RESTOCK 卷分配目标库位。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_sales_return_roll (
    id               int8          NOT NULL,
    return_id        int8          NOT NULL,
    return_item_id   int8          NOT NULL,
    shipment_roll_id int8          NULL,
    roll_no          varchar(64)   NOT NULL,
    weight           numeric(18,2) NULL,
    length_m         numeric(18,2) NULL,
    qc_result        varchar(16)   NULL,
    qc_by            int8          NULL,
    qc_at            timestamp     NULL,
    qc_remark        varchar(500)  NULL,
    location_id      int8          NULL,
    location_code    varchar(64)   NULL,
    remark           varchar(500)  NULL,
    creator          varchar(64)   NULL DEFAULT '',
    create_time      timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater          varchar(64)   NULL DEFAULT '',
    update_time      timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          int2          NOT NULL DEFAULT 0,
    tenant_id        int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_business_sales_return_roll') THEN
        ALTER TABLE lunaerp_business_sales_return_roll ADD CONSTRAINT pk_lunaerp_business_sales_return_roll PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_sales_return_roll_seq;

-- 索引 1: 租户+退货单外键反查
CREATE INDEX IF NOT EXISTS idx_lbsrr_tenant_return_id
    ON lunaerp_business_sales_return_roll (tenant_id, return_id);

-- 索引 2: 租户+发货卷血缘
CREATE INDEX IF NOT EXISTS idx_lbsrr_tenant_shipment_roll_id
    ON lunaerp_business_sales_return_roll (tenant_id, shipment_roll_id);

-- 索引 3: 租户+退货明细行外键
CREATE INDEX IF NOT EXISTS idx_lbsrr_tenant_return_item_id
    ON lunaerp_business_sales_return_roll (tenant_id, return_item_id);

COMMENT ON TABLE lunaerp_business_sales_return_roll IS '业务管理-客户销售退货卷明细（逐卷质检与库位）';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.id IS '退货卷ID';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.return_id IS '所属退货单ID（lunaerp_business_sales_return.id）';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.return_item_id IS '所属退货明细ID（lunaerp_business_sales_return_item.id）';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.shipment_roll_id IS '关联原发货卷ID（lunaerp_business_shipment_roll.id，可选，散退为空）';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.roll_no IS '卷号（勾选原发货卷带回可改，散退手填必填）';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.weight IS '实称重量（KG，与 length_m 至少填一项）';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.length_m IS '米数（M）';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.qc_result IS '质检结果判定（字典 lunaerp_sales_return_qc_result：RESTOCK/SCRAP，草稿期为空）';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.qc_by IS '质检判定人ID';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.qc_at IS '质检判定时间';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.qc_remark IS '质检处置说明';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.location_id IS '目标库位ID（仅 RESTOCK 卷分配，可空先入账后上架）';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.location_code IS '目标库位编码快照';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.remark IS '卷备注';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_sales_return_roll.tenant_id IS '租户编号';

-- =============================================================================
-- 4. 字典：type 2279–2281 + 审批业务类型 sales_return
--    2279: lunaerp_sales_return_status 销售退货业务状态
--    2280: lunaerp_sales_return_reason 销售退货原因
--    2281: lunaerp_sales_return_qc_result 销售退货质检判定
--    按 DO 块检测 2279–2281 与 3850 起步数据 ID 是否冲突并平移避让。
-- =============================================================================
-- @formatter:off
DO $$
DECLARE
    v_type_id_status int8;
    v_type_id_reason int8;
    v_type_id_qc int8;
    v_offset int8;
    v_biz_type_id int8;
BEGIN
    -- 4.1 清理已有同类型字典
    DELETE FROM system_dict_data WHERE dict_type IN (
        'lunaerp_sales_return_status',
        'lunaerp_sales_return_reason',
        'lunaerp_sales_return_qc_result'
    );
    DELETE FROM system_dict_type WHERE type IN (
        'lunaerp_sales_return_status',
        'lunaerp_sales_return_reason',
        'lunaerp_sales_return_qc_result'
    );

    -- 4.2 字典类型 ID 避让判定（2279-2281 判存平移）
    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2279) THEN
        v_type_id_status := 2279;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), 2281), 2281) + 1 INTO v_type_id_status FROM system_dict_type;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2280) THEN
        v_type_id_reason := 2280;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), v_type_id_status), v_type_id_status) + 1 INTO v_type_id_reason FROM system_dict_type;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2281) THEN
        v_type_id_qc := 2281;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), v_type_id_reason), v_type_id_reason) + 1 INTO v_type_id_qc FROM system_dict_type;
    END IF;

    -- 4.3 字典数据 ID 梯次避让判定（3850–3869 段）
    IF NOT EXISTS (SELECT 1 FROM system_dict_data WHERE id BETWEEN 3850 AND 3869) THEN
        v_offset := 3850;
    ELSE
        SELECT GREATEST(COALESCE(MAX(id), 3869), 3869) + 1 INTO v_offset FROM system_dict_data;
    END IF;

    -- 4.4 插入字典类型
    INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted)
    VALUES
    (v_type_id_status, '销售退货业务状态', 'lunaerp_sales_return_status', 0, 'LunaERP 销售退货-业务状态', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
    (v_type_id_reason, '销售退货原因', 'lunaerp_sales_return_reason', 0, 'LunaERP 销售退货-行级退货原因', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
    (v_type_id_qc, '销售退货质检判定', 'lunaerp_sales_return_qc_result', 0, 'LunaERP 销售退货-逐卷质检判定', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0');

    -- 4.5 插入字典数据项
    INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
    VALUES
    -- 状态 2279
    (v_offset + 0, 1, '草稿', 'DRAFT', 'lunaerp_sales_return_status', 0, 'warning', '', '草稿态', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
    (v_offset + 1, 2, '已审批', 'APPROVED', 'lunaerp_sales_return_status', 0, 'primary', '', '已审批待到货', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
    (v_offset + 2, 3, '已收货', 'RECEIVED', 'lunaerp_sales_return_status', 0, 'warning', '', '已到货质检待财务确认', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
    (v_offset + 3, 4, '已过账', 'POSTED', 'lunaerp_sales_return_status', 0, 'success', '', '财务已确认过账终态', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
    (v_offset + 4, 5, '已作废', 'CANCELLED', 'lunaerp_sales_return_status', 0, 'info', '', '已作废终态', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),

    -- 原因 2280
    (v_offset + 5, 1, '质量问题', 'QUALITY', 'lunaerp_sales_return_reason', 0, 'danger', '', '产品质量瑕疵', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
    (v_offset + 6, 2, '发错货', 'WRONG', 'lunaerp_sales_return_reason', 0, 'warning', '', '发错品种或颜色', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
    (v_offset + 7, 3, '多发', 'EXCESS', 'lunaerp_sales_return_reason', 0, 'primary', '', '多发退回', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
    (v_offset + 8, 4, '客户原因', 'CUSTOMER', 'lunaerp_sales_return_reason', 0, 'info', '', '客户自身原因', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
    (v_offset + 9, 5, '其他', 'OTHER', 'lunaerp_sales_return_reason', 0, 'info', '', '其他退货原因', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),

    -- 质检判定 2281
    (v_offset + 10, 1, '可二次入库', 'RESTOCK', 'lunaerp_sales_return_qc_result', 0, 'success', '', '可二次销售入库', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
    (v_offset + 11, 2, '废品', 'SCRAP', 'lunaerp_sales_return_qc_result', 0, 'danger', '', '判废处置不入库', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

    -- 4.6 审批单据类型字典增量行：sales_return
    IF NOT EXISTS (SELECT 1 FROM system_dict_data WHERE dict_type = 'lunaerp_approval_biz_type' AND value = 'sales_return') THEN
        SELECT GREATEST(COALESCE(MAX(id), 3800), 3800) + 1 INTO v_biz_type_id FROM system_dict_data;
        INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
        VALUES (v_biz_type_id, 5, '销售退货', 'sales_return', 'lunaerp_approval_biz_type', 0, 'danger', '', 'LunaERP 审批引擎-销售退货单审批', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);
    END IF;
END $$;
-- @formatter:on

-- =============================================================================
-- 5. 审批引擎：sales_return 默认启用审批流配置
--    默认单节点逐级审批（销售经理角色审批）；保持幂等。
-- =============================================================================
DO $$
DECLARE
    v_config_id int8;
BEGIN
    DELETE FROM lunaerp_approval_config_node WHERE config_id IN (
        SELECT id FROM lunaerp_approval_config WHERE biz_type = 'sales_return'
    );
    DELETE FROM lunaerp_approval_config WHERE biz_type = 'sales_return';

    v_config_id := nextval('lunaerp_approval_config_seq');

    INSERT INTO lunaerp_approval_config (
        id, tenant_id, biz_type, name, status, remark, creator, create_time, updater, update_time, deleted
    ) VALUES (
        v_config_id, 1, 'sales_return', '销售退货默认审批流', 'ENABLED',
        '销售退货默认单节点审批流程（销售经理审批）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
    );

    INSERT INTO lunaerp_approval_config_node (
        id, tenant_id, config_id, sort, name, node_type, approver_type, approver_ids,
        creator, create_time, updater, update_time, deleted
    ) VALUES (
        nextval('lunaerp_approval_config_node_seq'), 1, v_config_id, 1, '销售经理审批',
        'SEQUENTIAL', 'ROLE', '2', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
    );
END $$;

-- =============================================================================
-- 6. 菜单与权限：10141 销售退货（挂 10100 业务管理/销售目录 sort 4）+ 按钮 10142–10149
-- =============================================================================
-- 确保父目录 10100 存在
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10100, '销售', '', 1, 101, 0, '/business', 'ep:briefcase', NULL, NULL,
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'
) ON CONFLICT (id) DO NOTHING;

DELETE FROM system_menu WHERE id IN (
    10141, 10142, 10143, 10144, 10145, 10146, 10147, 10148, 10149
);

-- 二级菜单：10141 销售退货
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10141, '销售退货', 'lunaerp:business:sales-return:query', 2, 4, 10100, 'sales-return', 'ep:refresh-left', 'lunaerp-elmui/business/sales-return/index', 'LunaerpElmuiBusinessSalesReturn',
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'
);

-- 按钮：10142–10149
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES
(10142, '退货创建', 'lunaerp:business:sales-return:create', 3, 1, 10141, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10143, '退货修改', 'lunaerp:business:sales-return:update', 3, 2, 10141, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10144, '退货删除', 'lunaerp:business:sales-return:delete', 3, 3, 10141, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10145, '退货提交审批', 'lunaerp:business:sales-return:submit', 3, 4, 10141, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10146, '到货质检', 'lunaerp:business:sales-return:receive', 3, 5, 10141, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10147, '财务确认', 'lunaerp:business:sales-return:post', 3, 6, 10141, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10148, '退货作废', 'lunaerp:business:sales-return:cancel', 3, 7, 10141, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0'),
(10149, '退货打印', 'lunaerp:business:sales-return:print', 3, 8, 10141, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0');

-- 赋权系统管理员角色
CREATE SEQUENCE IF NOT EXISTS system_role_menu_seq;

DO $$
BEGIN
    PERFORM setval('system_role_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role_menu), 1));
END $$;

DELETE FROM system_role_menu WHERE menu_id IN (
    10141, 10142, 10143, 10144, 10145, 10146, 10147, 10148, 10149
);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM (
    SELECT DISTINCT id FROM system_role
    WHERE code IN ('super_admin', 'admin') OR id IN (1, 2)
) r
CROSS JOIN (
    SELECT 10141 AS id
    UNION ALL SELECT 10142 UNION ALL SELECT 10143 UNION ALL SELECT 10144
    UNION ALL SELECT 10145 UNION ALL SELECT 10146 UNION ALL SELECT 10147
    UNION ALL SELECT 10148 UNION ALL SELECT 10149
) m;
