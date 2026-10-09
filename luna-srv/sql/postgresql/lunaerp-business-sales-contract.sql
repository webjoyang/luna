-- =============================================================================
-- 模块：LunaERP - 业务管理 - 销售合同（主表 + 商品明细子表，明细作为合同聚合整体保存，见 ADR-0003）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 销售合同主表 lunaerp_business_sales_contract
--    total_quantity/total_amount 本票固定为 0，商品明细（02 票）接管后重算。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_sales_contract (
    id                       int8          NOT NULL,
    contract_no              varchar(32)   NOT NULL,
    customer_id              int8          NOT NULL,
    client_name              varchar(128)  NOT NULL,
    client_contact           varchar       NULL,
    client_address           varchar       NULL,
    client_contract_no       varchar       NULL,
    salesperson_user_id      int8          NULL,
    sign_date                date          NOT NULL,
    delivery_date            date          NULL,
    unit                     varchar       NOT NULL DEFAULT 'KG',
    currency                 varchar       NOT NULL DEFAULT 'RMB',
    trade_type               varchar       NOT NULL DEFAULT 'DOMESTIC',
    order_type               varchar       NOT NULL DEFAULT 'BULK',
    total_quantity           numeric(18,2) NOT NULL DEFAULT 0,
    total_amount             numeric(18,2) NOT NULL DEFAULT 0,
    remarks                  varchar(1000) NULL,
    status                   varchar       NOT NULL DEFAULT 'ACTIVE',
    creator                  varchar(64)   NULL DEFAULT '',
    create_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)   NULL DEFAULT '',
    update_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2          NOT NULL DEFAULT 0,
    tenant_id                int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_sales_contract') THEN
        ALTER TABLE lunaerp_business_sales_contract ADD CONSTRAINT pk_lb_sales_contract PRIMARY KEY (id);
    END IF;
END $$;

-- 合同编号租户内全量唯一（ADR-0004）：普通唯一索引、不带 WHERE deleted=0，
-- 软删不释放编号，避免对外单据编号被重用。勿"修正"为部分唯一索引。
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_sales_contract_tenant_no
    ON lunaerp_business_sales_contract (tenant_id, contract_no);
CREATE INDEX IF NOT EXISTS idx_lb_sales_contract_status
    ON lunaerp_business_sales_contract (status);
CREATE INDEX IF NOT EXISTS idx_lb_sales_contract_sign_date
    ON lunaerp_business_sales_contract (sign_date);

COMMENT ON TABLE lunaerp_business_sales_contract IS '业务管理-销售合同主表';
COMMENT ON COLUMN lunaerp_business_sales_contract.id IS '合同ID';
COMMENT ON COLUMN lunaerp_business_sales_contract.contract_no IS '合同编号（CH+年份+4位顺序号，可手填，租户内全量唯一含已删除行）';
COMMENT ON COLUMN lunaerp_business_sales_contract.customer_id IS '客户ID（lunaerp_baseinfo_customer.id，必填）';
COMMENT ON COLUMN lunaerp_business_sales_contract.client_name IS '客户名称快照（选中客户时保存，必填）';
COMMENT ON COLUMN lunaerp_business_sales_contract.client_contact IS '客户联系人';
COMMENT ON COLUMN lunaerp_business_sales_contract.client_address IS '客户地址';
COMMENT ON COLUMN lunaerp_business_sales_contract.client_contract_no IS '客户合同号';
COMMENT ON COLUMN lunaerp_business_sales_contract.salesperson_user_id IS '业务员用户ID（system_users.id，可空）';
COMMENT ON COLUMN lunaerp_business_sales_contract.sign_date IS '签订日期（必填，创建缺省当天）';
COMMENT ON COLUMN lunaerp_business_sales_contract.delivery_date IS '交货日期（可空）';
COMMENT ON COLUMN lunaerp_business_sales_contract.unit IS '单位（字典 lunaerp_sales_contract_unit：KG/M），创建缺省 KG';
COMMENT ON COLUMN lunaerp_business_sales_contract.currency IS '币种（字典 lunaerp_sales_contract_currency：RMB/USD/EUR/HKD），创建缺省 RMB';
COMMENT ON COLUMN lunaerp_business_sales_contract.trade_type IS '贸易类型（字典 lunaerp_sales_contract_trade_type：DOMESTIC/EXPORT），创建缺省 DOMESTIC';
COMMENT ON COLUMN lunaerp_business_sales_contract.order_type IS '订单属性（字典 lunaerp_sales_contract_order_type：BULK/SAMPLE），创建缺省 BULK';
COMMENT ON COLUMN lunaerp_business_sales_contract.total_quantity IS '总数量（本票固定 0，02 票按商品明细重算）';
COMMENT ON COLUMN lunaerp_business_sales_contract.total_amount IS '总金额（本票固定 0，02 票按商品明细重算）';
COMMENT ON COLUMN lunaerp_business_sales_contract.remarks IS '备注';
COMMENT ON COLUMN lunaerp_business_sales_contract.status IS '合同状态（字典 lunaerp_sales_contract_status：ACTIVE/COMPLETED/CANCELLED），创建缺省 ACTIVE';
COMMENT ON COLUMN lunaerp_business_sales_contract.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_sales_contract.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_sales_contract.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_sales_contract.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_sales_contract.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_business_sales_contract.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_business_sales_contract_seq;

-- =============================================================================
-- 1b. 销售合同明细子表 lunaerp_business_sales_contract_item
--     明细是合同聚合的一部分（ADR-0003）：无独立 Controller/权限码，
--     写操作随合同保存整体替换，删除合同时按 contract_id 级联逻辑删除。
--     amount / total_quantity / total_amount 由服务端权威重算（行金额 = 数量×单价+其他费用）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_sales_contract_item (
    id                       int8          NOT NULL,
    contract_id              int8          NOT NULL,
    line_no                  int4          NOT NULL DEFAULT 0,
    product_code             varchar(64)   NULL,
    product_name             varchar(128)  NULL,
    customer_color           varchar(64)   NULL,
    composition              varchar(128)  NULL,
    width                    varchar(64)   NULL,
    weight                   varchar(64)   NULL,
    quantity                 numeric(18,2) NOT NULL DEFAULT 0,
    unit_price               numeric(18,2) NOT NULL DEFAULT 0,
    other_fees               numeric(18,2) NOT NULL DEFAULT 0,
    amount                   numeric(18,2) NOT NULL DEFAULT 0,
    remark                   varchar(256)  NULL,
    creator                  varchar(64)   NULL DEFAULT '',
    create_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)   NULL DEFAULT '',
    update_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2          NOT NULL DEFAULT 0,
    tenant_id                int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_sales_contract_item') THEN
        ALTER TABLE lunaerp_business_sales_contract_item ADD CONSTRAINT pk_lb_sales_contract_item PRIMARY KEY (id);
    END IF;
END $$;

-- 按合同查询明细的普通索引（明细随合同整体加载，按 line_no 排序）
CREATE INDEX IF NOT EXISTS idx_lb_sales_contract_item_contract_id
    ON lunaerp_business_sales_contract_item (contract_id);

COMMENT ON TABLE lunaerp_business_sales_contract_item IS '业务管理-销售合同明细子表';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.id IS '明细ID';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.contract_id IS '合同ID（lunaerp_business_sales_contract.id，必填）';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.line_no IS '行号（服务端按提交顺序重排，从 1 开始）';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.product_code IS '产品编号（本票手填输入，03 票接入产品库远程搜索）';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.product_name IS '品名';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.customer_color IS '客户色号';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.composition IS '成分';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.width IS '门幅';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.weight IS '克重';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.quantity IS '数量';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.unit_price IS '单价';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.other_fees IS '其他费用';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.amount IS '行金额（服务端重算：数量×单价+其他费用）';
COMMENT ON COLUMN lunaerp_business_sales_contract_item.remark IS '行备注';

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_sales_contract_item_seq;

-- =============================================================================
-- 3. 菜单与按钮（system_menu，可重复执行）
--    10100 业务管理目录与 10000 基本信息平级；10110 销售合同菜单承载 query 权限。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10100, 10110, 10111, 10112, 10113);

-- 目录：销售（parent_id=0 顶级，与基础信息 10000 平级）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10100, '销售', '', 1, 101, 0, '/business', 'ep:briefcase', NULL, NULL, 0, '1', '1', '1', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
-- 菜单：销售合同（query 权限挂在菜单本身，参照产品/客户菜单模式）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10110, '销售合同', 'lunaerp:business:sales-contract:query', 2, 1, 10100, 'sales-contract', 'ep:document', 'lunaerp-elmui/business/sales-contract/index', 'LunaerpElmuiBusinessSalesContract', 0, '1', '1', '1', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
-- 按钮：合同创建/修改/删除
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10111, '销售合同创建', 'lunaerp:business:sales-contract:create', 3, 1, 10110, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10112, '销售合同修改', 'lunaerp:business:sales-contract:update', 3, 2, 10110, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10113, '销售合同删除', 'lunaerp:business:sales-contract:delete', 3, 3, 10110, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 4. 字典类型与字典数据（system_dict_type / system_dict_data，可重复执行）
--    dict_type id 2210-2214，dict_data id 从 3530 起（紧接客户模块 3515-3518）。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2210, 2211, 2212, 2213, 2214);
DELETE FROM system_dict_data WHERE dict_type IN (
    'lunaerp_sales_contract_status',
    'lunaerp_sales_contract_trade_type',
    'lunaerp_sales_contract_order_type',
    'lunaerp_sales_contract_unit',
    'lunaerp_sales_contract_currency'
);

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2210, '销售合同状态', 'lunaerp_sales_contract_status', 0, 'LunaERP 销售合同-状态', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0', NULL);
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2211, '销售合同贸易类型', 'lunaerp_sales_contract_trade_type', 0, 'LunaERP 销售合同-贸易类型', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0', NULL);
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2212, '销售合同订单属性', 'lunaerp_sales_contract_order_type', 0, 'LunaERP 销售合同-订单属性', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0', NULL);
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2213, '销售合同单位', 'lunaerp_sales_contract_unit', 0, 'LunaERP 销售合同-单位', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0', NULL);
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2214, '销售合同币种', 'lunaerp_sales_contract_currency', 0, 'LunaERP 销售合同-币种', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0', NULL);

-- 合同状态：进行中/已完成/已取消
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3530, 1, '进行中', 'ACTIVE', 'lunaerp_sales_contract_status', 0, 'success', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3531, 2, '已完成', 'COMPLETED', 'lunaerp_sales_contract_status', 0, 'info', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3532, 3, '已取消', 'CANCELLED', 'lunaerp_sales_contract_status', 0, 'danger', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');

-- 贸易类型：内销/外销
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3533, 1, '内销', 'DOMESTIC', 'lunaerp_sales_contract_trade_type', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3534, 2, '外销', 'EXPORT', 'lunaerp_sales_contract_trade_type', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');

-- 订单属性：大货/样布
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3535, 1, '大货', 'BULK', 'lunaerp_sales_contract_order_type', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3536, 2, '样布', 'SAMPLE', 'lunaerp_sales_contract_order_type', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');

-- 单位：千克/米
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3537, 1, '千克', 'KG', 'lunaerp_sales_contract_unit', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3538, 2, '米', 'M', 'lunaerp_sales_contract_unit', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');

-- 币种：人民币/美元/欧元/港币
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3539, 1, '人民币', 'RMB', 'lunaerp_sales_contract_currency', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3540, 2, '美元', 'USD', 'lunaerp_sales_contract_currency', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3541, 3, '欧元', 'EUR', 'lunaerp_sales_contract_currency', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3542, 4, '港币', 'HKD', 'lunaerp_sales_contract_currency', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');

COMMIT;
-- @formatter:on
