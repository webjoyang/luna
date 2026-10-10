-- =============================================================================
-- 模块：LunaERP - 商业化验收 - 销售归属链行级数据范围万级性能灌数脚本 (Issue 08 / 16 号 §2.2)
-- 数据库：PostgreSQL 15+
-- 规范：
--   1. 5 表各灌 1 万行（双租户 9000:1000、含 5% 软删行，16 号 §2.2 口径）
--   2. 时间跨度覆盖 ≥12 个月，覆盖各状态值（ACTIVE/COMPLETED/VOID、DRAFT/AUDITED 等）
--   3. 销售归属人与部门按 D1(101)/D1.1(102)/D2(103) 真实组织映射分布，含 ~5% 空归属行
--   4. 报价表（lunaerp_business_quotation）带表存在性守卫，随 05 票建表后自动生效，未建表则安全跳过并提示
--   5. 全程幂等：先按 ID 区间（2000000001 ~ 2000010000）清理旧数据，再批量写入，可重复执行
-- =============================================================================

BEGIN;

-- -----------------------------------------------------------------------------
-- 0. 清理存量性能测试数据（ID 区间 2000000001 ~ 2000010000）
-- -----------------------------------------------------------------------------
DELETE FROM lunaerp_baseinfo_customer_contact WHERE id BETWEEN 2000000001 AND 2000010000;
DELETE FROM lunaerp_business_shipment WHERE id BETWEEN 2000000001 AND 2000010000;
DELETE FROM lunaerp_business_sales_contract WHERE id BETWEEN 2000000001 AND 2000010000;
DELETE FROM lunaerp_baseinfo_customer WHERE id BETWEEN 2000000001 AND 2000010000;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'lunaerp_business_quotation'
    ) THEN
        DELETE FROM lunaerp_business_quotation WHERE id BETWEEN 2000000001 AND 2000010000;
    END IF;
END $$;

-- -----------------------------------------------------------------------------
-- 1. 客户表 (lunaerp_baseinfo_customer) 灌数 10,000 行
--    租户 1: 9000 行 (2000000001 ~ 2000009000)
--    租户 2: 1000 行 (2000009001 ~ 2000010000)
--    软删除 (deleted = 1): 500 行 (5%, 每 20 行 1 行)
--    时间跨度: 当前时间往前 365 天内均匀分布
-- -----------------------------------------------------------------------------
INSERT INTO lunaerp_baseinfo_customer (
    id,
    company_name,
    short_name,
    phone,
    address,
    client_type,
    industry,
    scale,
    source,
    cooperation_status,
    unified_social_credit_code,
    remark,
    status,
    creator,
    create_time,
    updater,
    update_time,
    deleted,
    tenant_id,
    name_en,
    address_en,
    salesperson_user_id,
    dept_id
)
SELECT
    s AS id,
    '测试客户公司-' || s AS company_name,
    '客户-' || s AS short_name,
    '138' || LPAD((s % 100000000)::text, 8, '0') AS phone,
    '江苏省苏州市工业园区科技路 ' || (s % 1000) || ' 号' AS address,
    CASE (s % 3)
        WHEN 0 THEN 'OFFICIAL'
        WHEN 1 THEN 'POTENTIAL'
        ELSE 'LOST'
    END AS client_type,
    'TEXTILE' AS industry,
    CASE (s % 3)
        WHEN 0 THEN 'LARGE'
        WHEN 1 THEN 'MEDIUM'
        ELSE 'SMALL'
    END AS scale,
    'EXHIBITION' AS source,
    CASE (s % 3)
        WHEN 0 THEN 'NORMAL'
        WHEN 1 THEN 'SUSPENDED'
        ELSE 'TERMINATED'
    END AS cooperation_status,
    '91320500' || LPAD(s::text, 10, '0') AS unified_social_credit_code,
    'perf-seed-10000' AS remark,
    CASE WHEN s % 10 = 0 THEN 'INACTIVE' ELSE 'ACTIVE' END AS status,
    '1000' AS creator,
    (CURRENT_TIMESTAMP - ((s % 365) || ' days')::INTERVAL) AS create_time,
    '1000' AS updater,
    (CURRENT_TIMESTAMP - ((s % 365) || ' days')::INTERVAL) AS update_time,
    CASE WHEN (s - 2000000000) % 20 = 0 THEN 1 ELSE 0 END AS deleted,
    CASE WHEN s <= 2000009000 THEN 1 ELSE 2 END AS tenant_id,
    'Customer Co ' || s AS name_en,
    'Suzhou Industrial Park No.' || (s % 1000) AS address_en,
    -- 归属人与部门映射分布：
    -- s % 20 = 19: 空归属 (5%)
    -- s % 4 = 0: 业务员 a (1002, 部门 101)
    -- s % 4 = 1: 业务员 c (1003, 部门 102)
    -- s % 4 = 2: 业务员 d (1004, 部门 103)
    -- s % 4 = 3: 仅本部门人员 (1005, 部门 101)
    CASE
        WHEN s % 20 = 19 THEN NULL
        WHEN s % 4 = 0 THEN 1002
        WHEN s % 4 = 1 THEN 1003
        WHEN s % 4 = 2 THEN 1004
        ELSE 1005
    END AS salesperson_user_id,
    CASE
        WHEN s % 20 = 19 THEN NULL
        WHEN s % 4 = 0 THEN 101
        WHEN s % 4 = 1 THEN 102
        WHEN s % 4 = 2 THEN 103
        ELSE 101
    END AS dept_id
FROM generate_series(2000000001, 2000010000) AS s;

-- -----------------------------------------------------------------------------
-- 2. 销售合同主表 (lunaerp_business_sales_contract) 灌数 10,000 行
--    合同号按租户唯一：uk_lb_sales_contract_tenant_no
-- -----------------------------------------------------------------------------
INSERT INTO lunaerp_business_sales_contract (
    id,
    contract_no,
    customer_id,
    client_name,
    client_contact,
    client_address,
    client_contract_no,
    salesperson_user_id,
    dept_id,
    sign_date,
    delivery_date,
    unit,
    currency,
    trade_type,
    order_type,
    total_quantity,
    total_amount,
    remarks,
    status,
    approval_status,
    creator,
    create_time,
    updater,
    update_time,
    deleted,
    tenant_id
)
SELECT
    s AS id,
    'SC-PERF-T' || (CASE WHEN s <= 2000009000 THEN '1' ELSE '2' END) || '-' || LPAD(s::text, 10, '0') AS contract_no,
    -- 关联同租户客户
    (CASE WHEN s <= 2000009000 THEN 2000000001 + (s % 9000) ELSE 2000009001 + (s % 1000) END) AS customer_id,
    '测试客户公司-' || (CASE WHEN s <= 2000009000 THEN 2000000001 + (s % 9000) ELSE 2000009001 + (s % 1000) END) AS client_name,
    '张经理' AS client_contact,
    '苏州市工业园区科技路 88 号' AS client_address,
    'CUST-SC-' || s AS client_contract_no,
    CASE
        WHEN s % 20 = 19 THEN NULL
        WHEN s % 4 = 0 THEN 1002
        WHEN s % 4 = 1 THEN 1003
        WHEN s % 4 = 2 THEN 1004
        ELSE 1005
    END AS salesperson_user_id,
    CASE
        WHEN s % 20 = 19 THEN NULL
        WHEN s % 4 = 0 THEN 101
        WHEN s % 4 = 1 THEN 102
        WHEN s % 4 = 2 THEN 103
        ELSE 101
    END AS dept_id,
    (CURRENT_DATE - ((s % 365) || ' days')::INTERVAL)::date AS sign_date,
    (CURRENT_DATE - ((s % 365) || ' days')::INTERVAL + interval '30 days')::date AS delivery_date,
    'KG' AS unit,
    'RMB' AS currency,
    'DOMESTIC' AS trade_type,
    'BULK' AS order_type,
    1000.00 + (s % 500) AS total_quantity,
    50000.00 + (s % 25000) AS total_amount,
    'perf-seed-contract' AS remarks,
    CASE (s % 3)
        WHEN 0 THEN 'ACTIVE'
        WHEN 1 THEN 'COMPLETED'
        ELSE 'VOID'
    END AS status,
    CASE (s % 4)
        WHEN 0 THEN 'APPROVED'
        WHEN 1 THEN 'DRAFT'
        WHEN 2 THEN 'PENDING'
        ELSE 'REJECTED'
    END AS approval_status,
    '1000' AS creator,
    (CURRENT_TIMESTAMP - ((s % 365) || ' days')::INTERVAL) AS create_time,
    '1000' AS updater,
    (CURRENT_TIMESTAMP - ((s % 365) || ' days')::INTERVAL) AS update_time,
    CASE WHEN (s - 2000000000) % 20 = 0 THEN 1 ELSE 0 END AS deleted,
    CASE WHEN s <= 2000009000 THEN 1 ELSE 2 END AS tenant_id
FROM generate_series(2000000001, 2000010000) AS s;

-- -----------------------------------------------------------------------------
-- 3. 发货单主表 (lunaerp_business_shipment) 灌数 10,000 行
-- -----------------------------------------------------------------------------
INSERT INTO lunaerp_business_shipment (
    id,
    shipment_no,
    status,
    customer_id,
    customer_name,
    sales_contract_id,
    contract_no,
    warehouse_id,
    warehouse_name,
    shipping_address,
    receive_address,
    carrier,
    tracking_number,
    shipping_date,
    expected_delivery_date,
    freight_fee,
    total_amount,
    total_pieces,
    salesperson_user_id,
    salesperson_name,
    dept_id,
    currency,
    sign_status,
    signed_by_name,
    signed_at,
    sign_remark,
    remark,
    creator,
    create_time,
    updater,
    update_time,
    deleted,
    tenant_id
)
SELECT
    s AS id,
    'SH-PERF-T' || (CASE WHEN s <= 2000009000 THEN '1' ELSE '2' END) || '-' || LPAD(s::text, 10, '0') AS shipment_no,
    CASE (s % 3)
        WHEN 0 THEN 'COMPLETED'
        WHEN 1 THEN 'DRAFT'
        ELSE 'CANCELLED'
    END AS status,
    (CASE WHEN s <= 2000009000 THEN 2000000001 + (s % 9000) ELSE 2000009001 + (s % 1000) END) AS customer_id,
    '测试客户公司-' || (CASE WHEN s <= 2000009000 THEN 2000000001 + (s % 9000) ELSE 2000009001 + (s % 1000) END) AS customer_name,
    s AS sales_contract_id,
    'SC-PERF-T' || (CASE WHEN s <= 2000009000 THEN '1' ELSE '2' END) || '-' || LPAD(s::text, 10, '0') AS contract_no,
    1 AS warehouse_id,
    '一号成品仓' AS warehouse_name,
    '苏州市高新区长江路 1 号' AS shipping_address,
    '苏州市工业园区科技路 88 号' AS receive_address,
    '顺丰速运' AS carrier,
    'SF' || LPAD(s::text, 12, '0') AS tracking_number,
    (CURRENT_DATE - ((s % 365) || ' days')::INTERVAL)::date AS shipping_date,
    (CURRENT_DATE - ((s % 365) || ' days')::INTERVAL + interval '3 days')::date AS expected_delivery_date,
    150.00 AS freight_fee,
    25000.00 + (s % 10000) AS total_amount,
    50 + (s % 50) AS total_pieces,
    CASE
        WHEN s % 20 = 19 THEN NULL
        WHEN s % 4 = 0 THEN 1002
        WHEN s % 4 = 1 THEN 1003
        WHEN s % 4 = 2 THEN 1004
        ELSE 1005
    END AS salesperson_user_id,
    CASE
        WHEN s % 20 = 19 THEN NULL
        WHEN s % 4 = 0 THEN '业务员a'
        WHEN s % 4 = 1 THEN '业务员c'
        WHEN s % 4 = 2 THEN '业务员d'
        ELSE '本部门业务员'
    END AS salesperson_name,
    CASE
        WHEN s % 20 = 19 THEN NULL
        WHEN s % 4 = 0 THEN 101
        WHEN s % 4 = 1 THEN 102
        WHEN s % 4 = 2 THEN 103
        ELSE 101
    END AS dept_id,
    'RMB' AS currency,
    CASE (s % 3)
        WHEN 0 THEN 'SIGNED'
        WHEN 1 THEN 'PENDING'
        ELSE 'REJECTED'
    END AS sign_status,
    CASE WHEN s % 3 = 0 THEN '王收货' ELSE NULL END AS signed_by_name,
    CASE WHEN s % 3 = 0 THEN (CURRENT_TIMESTAMP - ((s % 365) || ' days')::INTERVAL + interval '2 days') ELSE NULL END AS signed_at,
    'perf-shipment-sign' AS sign_remark,
    'perf-seed-shipment' AS remark,
    '1000' AS creator,
    (CURRENT_TIMESTAMP - ((s % 365) || ' days')::INTERVAL) AS create_time,
    '1000' AS updater,
    (CURRENT_TIMESTAMP - ((s % 365) || ' days')::INTERVAL) AS update_time,
    CASE WHEN (s - 2000000000) % 20 = 0 THEN 1 ELSE 0 END AS deleted,
    CASE WHEN s <= 2000009000 THEN 1 ELSE 2 END AS tenant_id
FROM generate_series(2000000001, 2000010000) AS s;

-- -----------------------------------------------------------------------------
-- 4. 客户联系人表 (lunaerp_baseinfo_customer_contact) 灌数 10,000 行
--    零归属列，EXISTS 继承客户归属
-- -----------------------------------------------------------------------------
INSERT INTO lunaerp_baseinfo_customer_contact (
    id,
    customer_id,
    name,
    position,
    phone,
    email,
    remark,
    is_primary,
    creator,
    create_time,
    updater,
    update_time,
    deleted,
    tenant_id
)
SELECT
    s AS id,
    (CASE WHEN s <= 2000009000 THEN 2000000001 + (s % 9000) ELSE 2000009001 + (s % 1000) END) AS customer_id,
    '联系人-' || s AS name,
    CASE (s % 4)
        WHEN 0 THEN '采购经理'
        WHEN 1 THEN '销售总监'
        WHEN 2 THEN '商务代表'
        ELSE '仓库主管'
    END AS position,
    '139' || LPAD((s % 100000000)::text, 8, '0') AS phone,
    'contact_' || s || '@example.com' AS email,
    'perf-seed-contact' AS remark,
    CASE WHEN s % 5 = 0 THEN 1 ELSE 0 END AS is_primary,
    '1000' AS creator,
    (CURRENT_TIMESTAMP - ((s % 365) || ' days')::INTERVAL) AS create_time,
    '1000' AS updater,
    (CURRENT_TIMESTAMP - ((s % 365) || ' days')::INTERVAL) AS update_time,
    CASE WHEN (s - 2000000000) % 20 = 0 THEN 1 ELSE 0 END AS deleted,
    CASE WHEN s <= 2000009000 THEN 1 ELSE 2 END AS tenant_id
FROM generate_series(2000000001, 2000010000) AS s;

-- -----------------------------------------------------------------------------
-- 5. 报价单表 (lunaerp_business_quotation) 灌数 10,000 行（带存在性守卫）
--    随 05 票建表后生效；若表尚未建立则抛出提示跳过
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'lunaerp_business_quotation'
    ) THEN
        INSERT INTO lunaerp_business_quotation (
            id,
            quotation_no,
            customer_id,
            client_name,
            salesperson_user_id,
            dept_id,
            quotation_date,
            valid_until,
            currency,
            total_amount,
            status,
            remark,
            creator,
            create_time,
            updater,
            update_time,
            deleted,
            tenant_id
        )
        SELECT
            s AS id,
            'QUO-PERF-T' || (CASE WHEN s <= 2000009000 THEN '1' ELSE '2' END) || '-' || LPAD(s::text, 10, '0') AS quotation_no,
            (CASE WHEN s <= 2000009000 THEN 2000000001 + (s % 9000) ELSE 2000009001 + (s % 1000) END) AS customer_id,
            '测试客户公司-' || (CASE WHEN s <= 2000009000 THEN 2000000001 + (s % 9000) ELSE 2000009001 + (s % 1000) END) AS client_name,
            CASE
                WHEN s % 20 = 19 THEN NULL
                WHEN s % 4 = 0 THEN 1002
                WHEN s % 4 = 1 THEN 1003
                WHEN s % 4 = 2 THEN 1004
                ELSE 1005
            END AS salesperson_user_id,
            CASE
                WHEN s % 20 = 19 THEN NULL
                WHEN s % 4 = 0 THEN 101
                WHEN s % 4 = 1 THEN 102
                WHEN s % 4 = 2 THEN 103
                ELSE 101
            END AS dept_id,
            (CURRENT_DATE - ((s % 365) || ' days')::INTERVAL)::date AS quotation_date,
            (CURRENT_DATE - ((s % 365) || ' days')::INTERVAL + interval '30 days')::date AS valid_until,
            'RMB' AS currency,
            30000.00 + (s % 15000) AS total_amount,
            CASE (s % 3)
                WHEN 0 THEN 'APPROVED'
                WHEN 1 THEN 'DRAFT'
                ELSE 'REJECTED'
            END AS status,
            'perf-seed-quotation' AS remark,
            '1000' AS creator,
            (CURRENT_TIMESTAMP - ((s % 365) || ' days')::INTERVAL) AS create_time,
            '1000' AS updater,
            (CURRENT_TIMESTAMP - ((s % 365) || ' days')::INTERVAL) AS update_time,
            CASE WHEN (s - 2000000000) % 20 = 0 THEN 1 ELSE 0 END AS deleted,
            CASE WHEN s <= 2000009000 THEN 1 ELSE 2 END AS tenant_id
        FROM generate_series(2000000001, 2000010000) AS s;
        -- 表 lunaerp_business_quotation 灌入 10,000 行性能数据
    ELSE
        -- 表 lunaerp_business_quotation 尚未建表（随 05 票建表后补跑），本票对客户/合同/发货/联系人 4 表实证
        NULL;
    END IF;
END $$;

COMMIT;

-- -----------------------------------------------------------------------------
-- 6. ANALYZE 收集统计信息，确保 PG 查询优化器获得精确直方图与基数估计
-- -----------------------------------------------------------------------------
ANALYZE lunaerp_baseinfo_customer;
ANALYZE lunaerp_business_sales_contract;
ANALYZE lunaerp_business_shipment;
ANALYZE lunaerp_baseinfo_customer_contact;
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'lunaerp_business_quotation'
    ) THEN
        EXECUTE 'ANALYZE lunaerp_business_quotation';
    END IF;
END $$;
