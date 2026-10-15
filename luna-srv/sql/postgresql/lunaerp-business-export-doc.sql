-- =============================================================================
-- 模块：LunaERP - 业务管理 - 出口单证（单证套：PI 形式发票 / CI 商业发票 / PL 装箱单）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 0. 前置可空加列（销售合同 +4 列外贸条款，客户档案 +2 列英文名址）
-- =============================================================================
ALTER TABLE lunaerp_business_sales_contract ADD COLUMN IF NOT EXISTS incoterm varchar(16);
ALTER TABLE lunaerp_business_sales_contract ADD COLUMN IF NOT EXISTS port_of_loading varchar(64);
ALTER TABLE lunaerp_business_sales_contract ADD COLUMN IF NOT EXISTS port_of_discharge varchar(64);
ALTER TABLE lunaerp_business_sales_contract ADD COLUMN IF NOT EXISTS payment_term varchar(16);

COMMENT ON COLUMN lunaerp_business_sales_contract.incoterm IS '贸易术语（字典 lunaerp_incoterm：FOB/CIF/CFR/EXW 等，可空）';
COMMENT ON COLUMN lunaerp_business_sales_contract.port_of_loading IS '起运港（可空）';
COMMENT ON COLUMN lunaerp_business_sales_contract.port_of_discharge IS '目的港（可空）';
COMMENT ON COLUMN lunaerp_business_sales_contract.payment_term IS '付款方式（字典 lunaerp_payment_term：TT/LC/DP/DA/OA 等，可空）';

ALTER TABLE lunaerp_baseinfo_customer ADD COLUMN IF NOT EXISTS name_en varchar(128);
ALTER TABLE lunaerp_baseinfo_customer ADD COLUMN IF NOT EXISTS address_en varchar(512);

COMMENT ON COLUMN lunaerp_baseinfo_customer.name_en IS '客户英文名称（外销单据带出，可空）';
COMMENT ON COLUMN lunaerp_baseinfo_customer.address_en IS '客户英文地址（外销单据带出，可空）';

-- =============================================================================
-- 1. 出口单证套主表 lunaerp_business_export_doc
--    单号 ED{yyyy}{4位流水}（按年取号、租户内全量唯一含软删行，见 ADR-0004）；
--    两态状态机 DRAFT/FINALIZED；无审批流；
--    共享抬头作为单一真相来源，三单证叠加专属 JSON meta。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_export_doc (
    id                 int8          NOT NULL,
    doc_no             varchar(32)   NOT NULL,
    status             varchar(16)   NOT NULL DEFAULT 'DRAFT',
    source_type        varchar(16)   NOT NULL DEFAULT 'MANUAL',
    contract_id        int8          NULL,
    contract_no        varchar(32)   NULL,
    shipment_id        int8          NULL,
    shipment_no        varchar(32)   NULL,
    customer_id        int8          NOT NULL,
    customer_name      varchar(128)  NULL,
    seller_name        varchar(256)  NULL,
    seller_address     varchar(512)  NULL,
    seller_bank_info   varchar(512)  NULL,
    buyer_name         varchar(256)  NULL,
    buyer_address      varchar(512)  NULL,
    buyer_bank_info    varchar(512)  NULL,
    incoterm           varchar(16)   NULL,
    port_of_loading    varchar(64)   NULL,
    port_of_discharge  varchar(64)   NULL,
    country_of_dest    varchar(64)   NULL,
    transport_mode     varchar(16)   NULL,
    payment_term       varchar(16)   NULL,
    currency           varchar(8)    NOT NULL DEFAULT 'USD',
    shipping_mark      varchar(500)  NULL,
    pi_meta            varchar(2000) NULL,
    ci_meta            varchar(2000) NULL,
    pl_meta            varchar(2000) NULL,
    remarks            varchar(1000) NULL,
    creator            varchar(64)   NULL DEFAULT '',
    create_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater            varchar(64)   NULL DEFAULT '',
    update_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            int2          NOT NULL DEFAULT 0,
    tenant_id          int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_business_export_doc') THEN
        ALTER TABLE lunaerp_business_export_doc ADD CONSTRAINT pk_lunaerp_business_export_doc PRIMARY KEY (id);
    END IF;
END $$;

-- 单证套号租户内全量唯一（ADR-0004，含软删行，不带 WHERE deleted=0）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lbed_doc_no
    ON lunaerp_business_export_doc (tenant_id, doc_no);
CREATE INDEX IF NOT EXISTS idx_lbed_status
    ON lunaerp_business_export_doc (status);
CREATE INDEX IF NOT EXISTS idx_lbed_customer_id
    ON lunaerp_business_export_doc (customer_id);
CREATE INDEX IF NOT EXISTS idx_lbed_contract_id
    ON lunaerp_business_export_doc (contract_id);
CREATE INDEX IF NOT EXISTS idx_lbed_shipment_id
    ON lunaerp_business_export_doc (shipment_id);

COMMENT ON TABLE lunaerp_business_export_doc IS '业务管理-出口单证套主表';
COMMENT ON COLUMN lunaerp_business_export_doc.id IS '单证套ID';
COMMENT ON COLUMN lunaerp_business_export_doc.doc_no IS '单证套号（ED{yyyy}{4位流水}，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_business_export_doc.status IS '单证状态（字典 lunaerp_export_doc_status：DRAFT/FINALIZED），缺省 DRAFT';
COMMENT ON COLUMN lunaerp_business_export_doc.source_type IS '来源类型（MANUAL 手工 / CONTRACT 合同 / SHIPMENT 发货单），缺省 MANUAL';
COMMENT ON COLUMN lunaerp_business_export_doc.contract_id IS '来源合同ID（可空）';
COMMENT ON COLUMN lunaerp_business_export_doc.contract_no IS '来源合同号快照';
COMMENT ON COLUMN lunaerp_business_export_doc.shipment_id IS '来源发货单ID（可空）';
COMMENT ON COLUMN lunaerp_business_export_doc.shipment_no IS '来源发货单号快照';
COMMENT ON COLUMN lunaerp_business_export_doc.customer_id IS '买方客户ID（必填）';
COMMENT ON COLUMN lunaerp_business_export_doc.customer_name IS '买方客户名称快照';
COMMENT ON COLUMN lunaerp_business_export_doc.seller_name IS '卖方英文名（签章/抬头，缺省取租户卖方配置）';
COMMENT ON COLUMN lunaerp_business_export_doc.seller_address IS '卖方英文地址';
COMMENT ON COLUMN lunaerp_business_export_doc.seller_bank_info IS '卖方收款银行信息';
COMMENT ON COLUMN lunaerp_business_export_doc.buyer_name IS '买方英文名（缺省取客户档案 name_en）';
COMMENT ON COLUMN lunaerp_business_export_doc.buyer_address IS '买方英文地址（缺省取客户档案 address_en）';
COMMENT ON COLUMN lunaerp_business_export_doc.buyer_bank_info IS '买方银行信息（随单手填）';
COMMENT ON COLUMN lunaerp_business_export_doc.incoterm IS '贸易术语（字典 lunaerp_incoterm：FOB/CIF/CFR/EXW）';
COMMENT ON COLUMN lunaerp_business_export_doc.port_of_loading IS '起运港';
COMMENT ON COLUMN lunaerp_business_export_doc.port_of_discharge IS '目的港';
COMMENT ON COLUMN lunaerp_business_export_doc.country_of_dest IS '目的国';
COMMENT ON COLUMN lunaerp_business_export_doc.transport_mode IS '运输方式（字典 lunaerp_transport_mode：SEA/AIR/LAND/EXPRESS）';
COMMENT ON COLUMN lunaerp_business_export_doc.payment_term IS '付款方式（字典 lunaerp_payment_term：TT/LC/DP/DA/OA）';
COMMENT ON COLUMN lunaerp_business_export_doc.currency IS '币种（复用字典 lunaerp_sales_contract_currency），缺省 USD';
COMMENT ON COLUMN lunaerp_business_export_doc.shipping_mark IS '唛头';
COMMENT ON COLUMN lunaerp_business_export_doc.pi_meta IS '形式发票 PI 专属元数据 JSON';
COMMENT ON COLUMN lunaerp_business_export_doc.ci_meta IS '商业发票 CI 专属元数据 JSON';
COMMENT ON COLUMN lunaerp_business_export_doc.pl_meta IS '装箱单 PL 专属元数据 JSON';
COMMENT ON COLUMN lunaerp_business_export_doc.remarks IS '备注';
COMMENT ON COLUMN lunaerp_business_export_doc.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_export_doc.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_export_doc.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_export_doc.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_export_doc.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_export_doc.tenant_id IS '租户编号';

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_export_doc_seq;

-- =============================================================================
-- 2. 出口单证明细行表 lunaerp_business_export_doc_item
--    三单共用货物明细，随单证套聚合整体保存（ADR-0003）
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_export_doc_item (
    id             int8          NOT NULL,
    doc_id         int8          NOT NULL,
    line_no        int4          NOT NULL,
    name_cn        varchar(128)  NULL,
    name_en        varchar(128)  NULL,
    hs_code        varchar(32)   NULL,
    spec           varchar(256)  NULL,
    quantity       numeric(18,2) NULL DEFAULT 0,
    unit           varchar(8)    NULL,
    unit_price     numeric(18,2) NULL DEFAULT 0,
    amount         numeric(18,2) NULL DEFAULT 0,
    net_weight     numeric(18,2) NULL DEFAULT 0,
    gross_weight   numeric(18,2) NULL DEFAULT 0,
    packages       int4          NULL DEFAULT 0,
    volume         numeric(18,3) NULL DEFAULT 0,
    origin         varchar(32)   NULL DEFAULT 'CHINA',
    remark         varchar(256)  NULL,
    creator        varchar(64)   NULL DEFAULT '',
    create_time    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater        varchar(64)   NULL DEFAULT '',
    update_time    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        int2          NOT NULL DEFAULT 0,
    tenant_id      int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_business_export_doc_item') THEN
        ALTER TABLE lunaerp_business_export_doc_item ADD CONSTRAINT pk_lunaerp_business_export_doc_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lbedi_doc_id
    ON lunaerp_business_export_doc_item (doc_id);

COMMENT ON TABLE lunaerp_business_export_doc_item IS '业务管理-出口单证明细行表';
COMMENT ON COLUMN lunaerp_business_export_doc_item.id IS '明细行ID';
COMMENT ON COLUMN lunaerp_business_export_doc_item.doc_id IS '所属单证套ID（lunaerp_business_export_doc.id）';
COMMENT ON COLUMN lunaerp_business_export_doc_item.line_no IS '行序';
COMMENT ON COLUMN lunaerp_business_export_doc_item.name_cn IS '中文品名';
COMMENT ON COLUMN lunaerp_business_export_doc_item.name_en IS '英文品名';
COMMENT ON COLUMN lunaerp_business_export_doc_item.hs_code IS '海关编码 HS CODE';
COMMENT ON COLUMN lunaerp_business_export_doc_item.spec IS '规格（成分/门幅/颜色）';
COMMENT ON COLUMN lunaerp_business_export_doc_item.quantity IS '数量';
COMMENT ON COLUMN lunaerp_business_export_doc_item.unit IS '单位（KG/M 等）';
COMMENT ON COLUMN lunaerp_business_export_doc_item.unit_price IS '单价';
COMMENT ON COLUMN lunaerp_business_export_doc_item.amount IS '金额（quantity * unit_price，服务端重算）';
COMMENT ON COLUMN lunaerp_business_export_doc_item.net_weight IS '净重 KG';
COMMENT ON COLUMN lunaerp_business_export_doc_item.gross_weight IS '毛重 KG';
COMMENT ON COLUMN lunaerp_business_export_doc_item.packages IS '箱数/件数';
COMMENT ON COLUMN lunaerp_business_export_doc_item.volume IS '体积 CBM';
COMMENT ON COLUMN lunaerp_business_export_doc_item.origin IS '原产地，缺省 CHINA';
COMMENT ON COLUMN lunaerp_business_export_doc_item.remark IS '行备注';
COMMENT ON COLUMN lunaerp_business_export_doc_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_export_doc_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_export_doc_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_export_doc_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_export_doc_item.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_export_doc_item.tenant_id IS '租户编号';

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_export_doc_item_seq;

-- =============================================================================
-- 3. 卖方出口配置表 lunaerp_business_export_seller_config
--    一租户一行，租户级隔离（不能使用全局 @TenantIgnore 的 infra_config）
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_export_seller_config (
    id             int8          NOT NULL,
    seller_name    varchar(256)  NOT NULL,
    seller_address varchar(512)  NULL,
    bank_name      varchar(256)  NULL,
    bank_account   varchar(128)  NULL,
    bank_swift     varchar(32)   NULL,
    bank_address   varchar(512)  NULL,
    remark         varchar(256)  NULL,
    creator        varchar(64)   NULL DEFAULT '',
    create_time    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater        varchar(64)   NULL DEFAULT '',
    update_time    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        int2          NOT NULL DEFAULT 0,
    tenant_id      int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_business_export_seller_config') THEN
        ALTER TABLE lunaerp_business_export_seller_config ADD CONSTRAINT pk_lunaerp_business_export_seller_config PRIMARY KEY (id);
    END IF;
END $$;

-- 租户唯一索引：一租户一行配置
CREATE UNIQUE INDEX IF NOT EXISTS uk_lbesc_tenant
    ON lunaerp_business_export_seller_config (tenant_id);

COMMENT ON TABLE lunaerp_business_export_seller_config IS '业务管理-卖方出口配置表';
COMMENT ON COLUMN lunaerp_business_export_seller_config.id IS '配置ID';
COMMENT ON COLUMN lunaerp_business_export_seller_config.seller_name IS '卖方英文名称（抬头/签章）';
COMMENT ON COLUMN lunaerp_business_export_seller_config.seller_address IS '卖方英文地址';
COMMENT ON COLUMN lunaerp_business_export_seller_config.bank_name IS '收款银行名称';
COMMENT ON COLUMN lunaerp_business_export_seller_config.bank_account IS '收款银行账号 A/C';
COMMENT ON COLUMN lunaerp_business_export_seller_config.bank_swift IS '收款银行 SWIFT 码';
COMMENT ON COLUMN lunaerp_business_export_seller_config.bank_address IS '收款银行地址';
COMMENT ON COLUMN lunaerp_business_export_seller_config.remark IS '备注';
COMMENT ON COLUMN lunaerp_business_export_seller_config.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_export_seller_config.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_export_seller_config.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_export_seller_config.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_export_seller_config.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_export_seller_config.tenant_id IS '租户编号';

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_export_seller_config_seq;

-- =============================================================================
-- 4. 菜单与按钮权限（system_menu，可重复执行）
--    10126 出口单证菜单挂在 10100 销售目录下（sort 3），承载 query 权限。
--    10127-10131 为出口单证下的操作与配置按钮权限。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10126, 10127, 10128, 10129, 10130, 10131);

-- 菜单：出口单证（query 权限挂在菜单本身，参照销售合同/发货单模式）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10126, '出口单证', 'lunaerp:business:export-doc:query', 2, 3, 10100, 'export-doc', 'ep:document', 'lunaerp-elmui/business/export-doc/index', 'LunaerpElmuiBusinessExportDoc', 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 按钮：创建/修改/删除/打印/卖方出口资料
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10127, '单证创建', 'lunaerp:business:export-doc:create', 3, 1, 10126, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10128, '单证修改', 'lunaerp:business:export-doc:update', 3, 2, 10126, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10129, '单证删除', 'lunaerp:business:export-doc:delete', 3, 3, 10126, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10130, '单证打印', 'lunaerp:business:export-doc:print', 3, 4, 10126, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10131, '卖方出口资料', 'lunaerp:business:export-doc:seller-config', 3, 5, 10126, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 授权管理员与超管角色（PostgreSQL 无自增：id 显式取号，序列对齐现有最大 id 防主键冲突）
CREATE SEQUENCE IF NOT EXISTS system_role_menu_seq;
DO $$
BEGIN
    PERFORM setval('system_role_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role_menu), 1));
END $$;

DELETE FROM system_role_menu WHERE menu_id IN (10126, 10127, 10128, 10129, 10130, 10131);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (
    SELECT 10126 AS id UNION ALL SELECT 10127 UNION ALL SELECT 10128 UNION ALL
    SELECT 10129 UNION ALL SELECT 10130 UNION ALL SELECT 10131
) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2)
ON CONFLICT DO NOTHING;

COMMIT;
-- @formatter:on

-- =============================================================================
-- 5. 字典类型与字典数据（system_dict_type / system_dict_data，可重复执行）
--    dict_type id 2258-2261，dict_data id 3750-3764
--    1) lunaerp_export_doc_status  出口单证状态（DRAFT/FINALIZED）
--    2) lunaerp_incoterm           贸易术语（FOB/CIF/CFR/EXW）
--    3) lunaerp_payment_term       付款方式（TT/LC/DP/DA/OA）
--    4) lunaerp_transport_mode     运输方式（SEA/AIR/LAND/EXPRESS）
-- =============================================================================
DO $$
BEGIN
    -- 避让已用值：若 2258-2261 已被非本模块字典占用，进行安全处理（防重幂等）
    IF EXISTS (SELECT 1 FROM system_dict_type WHERE id IN (2258, 2259, 2260, 2261)
                  AND type NOT IN ('lunaerp_export_doc_status', 'lunaerp_incoterm', 'lunaerp_payment_term', 'lunaerp_transport_mode')) THEN
        NULL;
    END IF;
END $$;

-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2258, 2259, 2260, 2261)
    OR type IN (
        'lunaerp_export_doc_status',
        'lunaerp_incoterm',
        'lunaerp_payment_term',
        'lunaerp_transport_mode'
    );

DELETE FROM system_dict_data WHERE dict_type IN (
        'lunaerp_export_doc_status',
        'lunaerp_incoterm',
        'lunaerp_payment_term',
        'lunaerp_transport_mode'
    )
    OR id BETWEEN 3750 AND 3764;

-- 5.1 字典类型：出口单证状态
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2258, '出口单证状态', 'lunaerp_export_doc_status', 0, 'LunaERP 业务管理-出口单证状态', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3750, 1, '草稿', 'DRAFT', 'lunaerp_export_doc_status', 0, 'warning', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3751, 2, '已定稿', 'FINALIZED', 'lunaerp_export_doc_status', 0, 'success', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 5.2 字典类型：贸易术语
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2259, '贸易术语', 'lunaerp_incoterm', 0, 'LunaERP 业务管理-贸易术语 Incoterm', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3752, 1, 'FOB', 'FOB', 'lunaerp_incoterm', 0, 'primary', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3753, 2, 'CIF', 'CIF', 'lunaerp_incoterm', 0, 'success', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3754, 3, 'CFR', 'CFR', 'lunaerp_incoterm', 0, 'info', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3755, 4, 'EXW', 'EXW', 'lunaerp_incoterm', 0, 'warning', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 5.3 字典类型：付款方式
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2260, '外贸付款方式', 'lunaerp_payment_term', 0, 'LunaERP 业务管理-外贸付款方式', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3756, 1, 'T/T', 'TT', 'lunaerp_payment_term', 0, 'primary', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3757, 2, 'L/C', 'LC', 'lunaerp_payment_term', 0, 'success', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3758, 3, 'D/P', 'DP', 'lunaerp_payment_term', 0, 'info', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3759, 4, 'D/A', 'DA', 'lunaerp_payment_term', 0, 'warning', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3760, 5, 'O/A', 'OA', 'lunaerp_payment_term', 0, 'danger', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 5.4 字典类型：运输方式
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2261, '运输方式', 'lunaerp_transport_mode', 0, 'LunaERP 业务管理-运输方式', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3761, 1, '海运', 'SEA', 'lunaerp_transport_mode', 0, 'primary', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3762, 2, '空运', 'AIR', 'lunaerp_transport_mode', 0, 'success', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3763, 3, '陆运', 'LAND', 'lunaerp_transport_mode', 0, 'info', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3764, 4, '快递', 'EXPRESS', 'lunaerp_transport_mode', 0, 'warning', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

COMMIT;
-- @formatter:on
