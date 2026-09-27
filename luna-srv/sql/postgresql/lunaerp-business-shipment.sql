-- =============================================================================
-- 模块：LunaERP - 业务管理 - 发货管理（发货单主表 + 发货明细 + 卷清单，明细与卷作为发货单聚合整体保存，见 ADR-0029 / ADR-0030）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 发货单主表 lunaerp_business_shipment（ADR-0029）
--    单号 SH-YYYYMMDD-NNN 按天取号、租户内全量唯一含软删行；
--    三态过账状态机 DRAFT/COMPLETED/CANCELLED；
--    客户必选、销售合同可选、一单一发货仓库（必选）；
--    关联方均为逻辑关联 + 快照字段，不建数据库物理外键；
--    总金额与总匹数由服务端根据明细权威重算。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_shipment (
    id                       int8          NOT NULL,
    shipment_no              varchar(32)   NOT NULL,
    status                   varchar(16)   NOT NULL DEFAULT 'DRAFT',
    customer_id              int8          NOT NULL,
    customer_name            varchar(128)  NULL,
    sales_contract_id        int8          NULL,
    contract_no              varchar(32)   NULL,
    warehouse_id             int8          NOT NULL,
    warehouse_name           varchar(128)  NULL,
    shipping_address         varchar(255)  NULL,
    receive_address          varchar(255)  NULL,
    carrier                  varchar(128)  NULL,
    tracking_number          varchar(64)   NULL,
    shipping_date            date          NULL,
    expected_delivery_date   date          NULL,
    freight_fee              numeric(18,2) NULL DEFAULT 0,
    total_amount             numeric(18,2) NULL DEFAULT 0,
    total_pieces             int4          NULL DEFAULT 0,
    salesperson_user_id      int8          NULL,
    salesperson_name         varchar(64)   NULL,
    currency                 varchar(8)    NOT NULL DEFAULT 'RMB',
    sign_status              varchar(16)   NOT NULL DEFAULT 'PENDING',
    signed_by_name           varchar(64)   NULL,
    signed_at                timestamp     NULL,
    sign_remark              varchar(500)  NULL,
    remark                   varchar(1000) NULL,
    creator                  varchar(64)   NULL DEFAULT '',
    create_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)   NULL DEFAULT '',
    update_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2          NOT NULL DEFAULT 0,
    tenant_id                int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_shipment') THEN
        ALTER TABLE lunaerp_business_shipment ADD CONSTRAINT pk_lb_shipment PRIMARY KEY (id);
    END IF;
END $$;

-- 发货单号租户内全量唯一（ADR-0029，仿 ADR-0004/0005/0015）：普通唯一索引、不带 WHERE deleted=0，
-- 按天取号、软删不释放编号。
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_shipment_tenant_no
    ON lunaerp_business_shipment (tenant_id, shipment_no);
CREATE INDEX IF NOT EXISTS idx_lb_shipment_status
    ON lunaerp_business_shipment (status);
CREATE INDEX IF NOT EXISTS idx_lb_shipment_customer_id
    ON lunaerp_business_shipment (customer_id);
CREATE INDEX IF NOT EXISTS idx_lb_shipment_sales_contract_id
    ON lunaerp_business_shipment (sales_contract_id);
CREATE INDEX IF NOT EXISTS idx_lb_shipment_warehouse_id
    ON lunaerp_business_shipment (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_lb_shipment_shipping_date
    ON lunaerp_business_shipment (shipping_date);
CREATE INDEX IF NOT EXISTS idx_lb_shipment_sign_status
    ON lunaerp_business_shipment (sign_status);

COMMENT ON TABLE lunaerp_business_shipment IS '业务管理-发货单主表';
COMMENT ON COLUMN lunaerp_business_shipment.id IS '发货单ID';
COMMENT ON COLUMN lunaerp_business_shipment.shipment_no IS '发货单号（SH-YYYYMMDD-NNN，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_business_shipment.status IS '发货状态（字典 lunaerp_shipment_status：DRAFT/COMPLETED/CANCELLED），缺省 DRAFT';
COMMENT ON COLUMN lunaerp_business_shipment.customer_id IS '客户ID（lunaerp_baseinfo_customer.id，必填）';
COMMENT ON COLUMN lunaerp_business_shipment.customer_name IS '客户名称快照';
COMMENT ON COLUMN lunaerp_business_shipment.sales_contract_id IS '销售合同ID（lunaerp_business_sales_contract.id，可空）';
COMMENT ON COLUMN lunaerp_business_shipment.contract_no IS '销售合同号快照';
COMMENT ON COLUMN lunaerp_business_shipment.warehouse_id IS '发货仓库ID（lunaerp_stock_warehouse.id，必填）';
COMMENT ON COLUMN lunaerp_business_shipment.warehouse_name IS '发货仓库名称快照';
COMMENT ON COLUMN lunaerp_business_shipment.shipping_address IS '发货地址';
COMMENT ON COLUMN lunaerp_business_shipment.receive_address IS '收货地址';
COMMENT ON COLUMN lunaerp_business_shipment.carrier IS '承运商/物流公司（自由文本，不建字典）';
COMMENT ON COLUMN lunaerp_business_shipment.tracking_number IS '物流单号';
COMMENT ON COLUMN lunaerp_business_shipment.shipping_date IS '发货日期';
COMMENT ON COLUMN lunaerp_business_shipment.expected_delivery_date IS '预计到货日期';
COMMENT ON COLUMN lunaerp_business_shipment.freight_fee IS '运费（独立于货物金额，缺省 0）';
COMMENT ON COLUMN lunaerp_business_shipment.total_amount IS '货物金额合计（Σ明细金额，服务端重算）';
COMMENT ON COLUMN lunaerp_business_shipment.total_pieces IS '总匹数（Σ明细匹数，服务端重算）';
COMMENT ON COLUMN lunaerp_business_shipment.salesperson_user_id IS '业务员用户ID（system_users.id，可空）';
COMMENT ON COLUMN lunaerp_business_shipment.salesperson_name IS '业务员姓名快照';
COMMENT ON COLUMN lunaerp_business_shipment.currency IS '币种（复用字典 lunaerp_sales_contract_currency：RMB/USD/EUR/HKD），缺省 RMB';
COMMENT ON COLUMN lunaerp_business_shipment.sign_status IS '签收状态（字典 lunaerp_shipment_sign_status：PENDING/SIGNED/REFUSED），缺省 PENDING';
COMMENT ON COLUMN lunaerp_business_shipment.signed_by_name IS '签收人姓名';
COMMENT ON COLUMN lunaerp_business_shipment.signed_at IS '签收时间';
COMMENT ON COLUMN lunaerp_business_shipment.sign_remark IS '签收备注';
COMMENT ON COLUMN lunaerp_business_shipment.remark IS '发货备注';
COMMENT ON COLUMN lunaerp_business_shipment.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_shipment.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_shipment.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_shipment.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_shipment.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_shipment.tenant_id IS '租户编号';

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_shipment_seq;

-- =============================================================================
-- 2. 发货明细表 lunaerp_business_shipment_item（ADR-0003, ADR-0029）
--    明细是发货单聚合的一部分：无独立 Controller/权限码，随整单保存整体替换；
--    删除发货单时按 shipment_id 级联逻辑删除；
--    每行必选产品档案，记录客户色号、缸号、数量、匹数、单价与行金额；
--    行金额 = 数量 × 单价，由服务端重算；
--    可关联来源入库明细（追溯用，零回写、不锁库）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_shipment_item (
    id                       int8          NOT NULL,
    shipment_id              int8          NOT NULL,
    product_id               int8          NOT NULL,
    product_code             varchar(64)   NULL,
    product_name             varchar(128)  NULL,
    customer_color_no        varchar(64)   NULL,
    color_name               varchar(64)   NULL,
    vat_no                   varchar(64)   NULL,
    quantity                 numeric(18,2) NOT NULL DEFAULT 0,
    pieces                   int4          NULL DEFAULT 1,
    unit                     varchar(8)    NOT NULL DEFAULT 'KG',
    unit_price               numeric(18,2) NULL DEFAULT 0,
    amount                   numeric(18,2) NULL DEFAULT 0,
    source_stock_in_item_id  int8          NULL,
    remark                   varchar(500)  NULL,
    creator                  varchar(64)   NULL DEFAULT '',
    create_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)   NULL DEFAULT '',
    update_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2          NOT NULL DEFAULT 0,
    tenant_id                int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_shipment_item') THEN
        ALTER TABLE lunaerp_business_shipment_item ADD CONSTRAINT pk_lb_shipment_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_shipment_item_shipment_id
    ON lunaerp_business_shipment_item (shipment_id);
CREATE INDEX IF NOT EXISTS idx_lb_shipment_item_product_id
    ON lunaerp_business_shipment_item (product_id);
CREATE INDEX IF NOT EXISTS idx_lb_shipment_item_source_stock_in_item_id
    ON lunaerp_business_shipment_item (source_stock_in_item_id);

COMMENT ON TABLE lunaerp_business_shipment_item IS '业务管理-发货明细表';
COMMENT ON COLUMN lunaerp_business_shipment_item.id IS '明细ID';
COMMENT ON COLUMN lunaerp_business_shipment_item.shipment_id IS '所属发货单ID（lunaerp_business_shipment.id，必填）';
COMMENT ON COLUMN lunaerp_business_shipment_item.product_id IS '产品ID（lunaerp_baseinfo_product.id，必填）';
COMMENT ON COLUMN lunaerp_business_shipment_item.product_code IS '产品编码快照';
COMMENT ON COLUMN lunaerp_business_shipment_item.product_name IS '产品名称快照';
COMMENT ON COLUMN lunaerp_business_shipment_item.customer_color_no IS '客户色号';
COMMENT ON COLUMN lunaerp_business_shipment_item.color_name IS '颜色名称';
COMMENT ON COLUMN lunaerp_business_shipment_item.vat_no IS '缸号/批次号（自由文本）';
COMMENT ON COLUMN lunaerp_business_shipment_item.quantity IS '发货数量（>0，必填）';
COMMENT ON COLUMN lunaerp_business_shipment_item.pieces IS '匹数（缺省 1）';
COMMENT ON COLUMN lunaerp_business_shipment_item.unit IS '单位（复用字典 lunaerp_stock_in_unit：KG/M），缺省 KG';
COMMENT ON COLUMN lunaerp_business_shipment_item.unit_price IS '单价';
COMMENT ON COLUMN lunaerp_business_shipment_item.amount IS '行金额（服务端重算：数量×单价）';
COMMENT ON COLUMN lunaerp_business_shipment_item.source_stock_in_item_id IS '来源入库明细ID（lunaerp_stock_in_item.id 快照，追溯用，零回写）';
COMMENT ON COLUMN lunaerp_business_shipment_item.remark IS '明细备注';
COMMENT ON COLUMN lunaerp_business_shipment_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_shipment_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_shipment_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_shipment_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_shipment_item.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_shipment_item.tenant_id IS '租户编号';

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_shipment_item_seq;

-- =============================================================================
-- 3. 卷清单表 lunaerp_business_shipment_roll（ADR-0030）
--    逐卷记录卷号、实称重量 KG、米数与备注；
--    独立子表存储，随发货单聚合保存整体替换；
--    shipment_id 冗余存储，便于级联删除与装箱清单平铺检索；
--    不设卷号数据库唯一约束（现场录入文本，业务自律）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_shipment_roll (
    id                       int8          NOT NULL,
    shipment_id              int8          NOT NULL,
    shipment_item_id         int8          NOT NULL,
    roll_no                  varchar(64)   NULL,
    weight                   numeric(18,2) NULL,
    length_m                 numeric(18,2) NULL,
    remark                   varchar(500)  NULL,
    creator                  varchar(64)   NULL DEFAULT '',
    create_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)   NULL DEFAULT '',
    update_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2          NOT NULL DEFAULT 0,
    tenant_id                int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_shipment_roll') THEN
        ALTER TABLE lunaerp_business_shipment_roll ADD CONSTRAINT pk_lb_shipment_roll PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_shipment_roll_shipment_id
    ON lunaerp_business_shipment_roll (shipment_id);
CREATE INDEX IF NOT EXISTS idx_lb_shipment_roll_shipment_item_id
    ON lunaerp_business_shipment_roll (shipment_item_id);

COMMENT ON TABLE lunaerp_business_shipment_roll IS '业务管理-发货卷清单表';
COMMENT ON COLUMN lunaerp_business_shipment_roll.id IS '卷记录ID';
COMMENT ON COLUMN lunaerp_business_shipment_roll.shipment_id IS '所属发货单ID（lunaerp_business_shipment.id，冗余便于级联删除与跨行平铺）';
COMMENT ON COLUMN lunaerp_business_shipment_roll.shipment_item_id IS '所属发货明细ID（lunaerp_business_shipment_item.id，必填）';
COMMENT ON COLUMN lunaerp_business_shipment_roll.roll_no IS '卷号';
COMMENT ON COLUMN lunaerp_business_shipment_roll.weight IS '实称重量 KG';
COMMENT ON COLUMN lunaerp_business_shipment_roll.length_m IS '米数（可空）';
COMMENT ON COLUMN lunaerp_business_shipment_roll.remark IS '卷备注';
COMMENT ON COLUMN lunaerp_business_shipment_roll.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_shipment_roll.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_shipment_roll.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_shipment_roll.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_shipment_roll.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_shipment_roll.tenant_id IS '租户编号';

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_shipment_roll_seq;

-- =============================================================================
-- 4. 菜单与按钮（system_menu，可重复执行）
--    10120 发货管理菜单挂在 10100 业务管理目录下（sort 2），承载 query 权限。
--    10121-10125 为发货管理下的按钮权限（创建/修改/删除/审核/打印）。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10120, 10121, 10122, 10123, 10124, 10125);

-- 菜单：发货管理（query 权限挂在菜单本身，参照销售合同模式）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10120, '发货管理', 'lunaerp:business:shipment:query', 2, 2, 10100, 'shipment', 'ep:van', 'lunaerp-elmui/business/shipment/index', 'LunaerpElmuiBusinessShipment', 0, '1', '1', '1', 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0');

-- 按钮：发货主表 创建/修改/删除/审核/打印
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10121, '发货创建', 'lunaerp:business:shipment:create', 3, 1, 10120, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10122, '发货修改', 'lunaerp:business:shipment:update', 3, 2, 10120, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10123, '发货删除', 'lunaerp:business:shipment:delete', 3, 3, 10120, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10124, '发货审核', 'lunaerp:business:shipment:audit', 3, 4, 10120, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10125, '发货打印', 'lunaerp:business:shipment:print', 3, 5, 10120, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 5. 字典类型与字典数据（system_dict_type / system_dict_data，可重复执行）
--    dict_type id 2245（发货单状态）、2246（发货单签收状态），
--    dict_data id 3690-3692（发货状态）、3693-3695（签收状态）。
--    明细单位复用 lunaerp_stock_in_unit、币种复用 lunaerp_sales_contract_currency；
--    承运商与流水 txn_type 不入字典。
-- =============================================================================
-- 避让已用值：若 2245 已被非发货状态字典占用，平移至 2247（防重幂等）
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2245 AND type != 'lunaerp_shipment_status') THEN
        IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2247) THEN
            UPDATE system_dict_type SET id = 2247 WHERE id = 2245;
        ELSE
            DELETE FROM system_dict_type WHERE id = 2245;
        END IF;
    END IF;
END $$;

-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2245, 2246)
    OR type IN ('lunaerp_shipment_status', 'lunaerp_shipment_sign_status');
DELETE FROM system_dict_data WHERE dict_type IN ('lunaerp_shipment_status', 'lunaerp_shipment_sign_status')
    OR id IN (3690, 3691, 3692, 3693, 3694, 3695);

-- 字典类型：发货单状态
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2245, '发货单状态', 'lunaerp_shipment_status', 0, 'LunaERP 业务管理-发货单状态', 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0', NULL);

-- 发货单状态：草稿(warning) / 已发货(success) / 已取消(danger)
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3690, 1, '草稿', 'DRAFT', 'lunaerp_shipment_status', 0, 'warning', '', NULL, 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3691, 2, '已发货', 'COMPLETED', 'lunaerp_shipment_status', 0, 'success', '', NULL, 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3692, 3, '已取消', 'CANCELLED', 'lunaerp_shipment_status', 0, 'danger', '', NULL, 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0');

-- 字典类型：发货单签收状态
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2246, '发货单签收状态', 'lunaerp_shipment_sign_status', 0, 'LunaERP 业务管理-发货单签收状态', 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0', NULL);

-- 签收状态：待签收(info) / 已签收(success) / 拒签(danger)
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3693, 1, '待签收', 'PENDING', 'lunaerp_shipment_sign_status', 0, 'info', '', NULL, 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3694, 2, '已签收', 'SIGNED', 'lunaerp_shipment_sign_status', 0, 'success', '', NULL, 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3695, 3, '拒签', 'REFUSED', 'lunaerp_shipment_sign_status', 0, 'danger', '', NULL, 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0');

COMMIT;
-- @formatter:on
