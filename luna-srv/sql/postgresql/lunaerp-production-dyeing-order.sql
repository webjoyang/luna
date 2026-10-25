-- =============================================================================
-- 模块：LunaERP - 生产管理 - 染色单（主表与颜色明细子表）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 染色单主表 lunaerp_production_dyeing_order
--    承染方引用供应商档案并保存公司名快照；销售合同/客户为逻辑关联，不建数据库外键。
--    无业务编号（ADR-0009），对外标识靠 id + 合同号快照。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_dyeing_order (
    id                       int8          NOT NULL,
    sales_contract_id        int8          NULL,
    contract_no              varchar(32)   NULL,
    customer_id              int8          NULL,
    client_name              varchar(128)  NULL,
    dyeing_factory_id        int8          NOT NULL,
    dyeing_factory_name      varchar(128)  NOT NULL,
    order_date               date          NOT NULL,
    delivery_date            date          NULL,
    product_code             varchar(64)   NULL,
    product_name             varchar(128)  NULL,
    composition              varchar(128)  NULL,
    width                    varchar(64)   NULL,
    gsm                      varchar(64)   NULL,
    shrinkage                varchar(64)   NULL,
    loss                     varchar(64)   NULL,
    ph                       varchar(64)   NULL,
    fastness_wash            varchar(64)   NULL,
    fastness_soap            varchar(64)   NULL,
    fastness_dry_rub         varchar(64)   NULL,
    fastness_wet_rub         varchar(64)   NULL,
    fastness_light           varchar(64)   NULL,
    fastness_perspiration    varchar(64)   NULL,
    fastness_dry_clean       varchar(64)   NULL,
    fastness_six_fiber       varchar(64)   NULL,
    color_instruction_id     int8          NULL,
    color_instruction_no     varchar(32)   NULL,
    other_req                varchar(1000) NULL,
    remark                   varchar(1000) NULL,
    status                   varchar(16)   NOT NULL DEFAULT 'DRAFT',
    creator                  varchar(64)   NULL DEFAULT '',
    create_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)   NULL DEFAULT '',
    update_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2          NOT NULL DEFAULT 0,
    tenant_id                int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_dyeing_order') THEN
        ALTER TABLE lunaerp_production_dyeing_order ADD CONSTRAINT pk_lb_dyeing_order PRIMARY KEY (id);
    END IF;
END $$;

-- 存量表结构变更与默认值补齐（可重复执行）
ALTER TABLE lunaerp_production_dyeing_order ADD COLUMN IF NOT EXISTS color_instruction_id int8 NULL;
ALTER TABLE lunaerp_production_dyeing_order ADD COLUMN IF NOT EXISTS color_instruction_no varchar(32) NULL;

CREATE INDEX IF NOT EXISTS idx_lb_dyeing_order_status
    ON lunaerp_production_dyeing_order (status);
CREATE INDEX IF NOT EXISTS idx_lb_dyeing_order_order_date
    ON lunaerp_production_dyeing_order (order_date);
CREATE INDEX IF NOT EXISTS idx_lb_dyeing_order_sales_contract_id
    ON lunaerp_production_dyeing_order (sales_contract_id);
CREATE INDEX IF NOT EXISTS idx_lb_dyeing_order_color_instruction_id
    ON lunaerp_production_dyeing_order (color_instruction_id);

COMMENT ON TABLE lunaerp_production_dyeing_order IS '生产管理-染色单主表';
COMMENT ON COLUMN lunaerp_production_dyeing_order.id IS '染色单ID';
COMMENT ON COLUMN lunaerp_production_dyeing_order.sales_contract_id IS '销售合同ID（lunaerp_business_sales_contract.id，逻辑关联可空）';
COMMENT ON COLUMN lunaerp_production_dyeing_order.contract_no IS '合同号快照（选中合同时保存）';
COMMENT ON COLUMN lunaerp_production_dyeing_order.customer_id IS '客户ID（lunaerp_baseinfo_customer.id，从合同带出可空）';
COMMENT ON COLUMN lunaerp_production_dyeing_order.client_name IS '客户名快照（从合同带出）';
COMMENT ON COLUMN lunaerp_production_dyeing_order.dyeing_factory_id IS '承染方供应商ID（lunaerp_baseinfo_supplier.id，必填）';
COMMENT ON COLUMN lunaerp_production_dyeing_order.dyeing_factory_name IS '承染方公司名快照（必填）';
COMMENT ON COLUMN lunaerp_production_dyeing_order.order_date IS '单据日期（必填，创建缺省当天）';
COMMENT ON COLUMN lunaerp_production_dyeing_order.delivery_date IS '交货日期（可空）';
COMMENT ON COLUMN lunaerp_production_dyeing_order.product_code IS '产品编码';
COMMENT ON COLUMN lunaerp_production_dyeing_order.product_name IS '产品名称';
COMMENT ON COLUMN lunaerp_production_dyeing_order.composition IS '成分';
COMMENT ON COLUMN lunaerp_production_dyeing_order.width IS '幅宽';
COMMENT ON COLUMN lunaerp_production_dyeing_order.gsm IS '克重';
COMMENT ON COLUMN lunaerp_production_dyeing_order.shrinkage IS '缩水率';
COMMENT ON COLUMN lunaerp_production_dyeing_order.loss IS '损耗率';
COMMENT ON COLUMN lunaerp_production_dyeing_order.ph IS 'pH值';
COMMENT ON COLUMN lunaerp_production_dyeing_order.fastness_wash IS '水洗牢度';
COMMENT ON COLUMN lunaerp_production_dyeing_order.fastness_soap IS '皂洗牢度';
COMMENT ON COLUMN lunaerp_production_dyeing_order.fastness_dry_rub IS '干摩擦牢度';
COMMENT ON COLUMN lunaerp_production_dyeing_order.fastness_wet_rub IS '湿摩擦牢度';
COMMENT ON COLUMN lunaerp_production_dyeing_order.fastness_light IS '日晒牢度';
COMMENT ON COLUMN lunaerp_production_dyeing_order.fastness_perspiration IS '汗渍牢度';
COMMENT ON COLUMN lunaerp_production_dyeing_order.fastness_dry_clean IS '干洗牢度';
COMMENT ON COLUMN lunaerp_production_dyeing_order.fastness_six_fiber IS '六千沾色牢度';
COMMENT ON COLUMN lunaerp_production_dyeing_order.color_instruction_id IS '打色指示单ID（lunaerp_production_color_instruction.id，逻辑关联可空，同客户+产品）';
COMMENT ON COLUMN lunaerp_production_dyeing_order.color_instruction_no IS '打色单号快照（保存时同步记录）';
COMMENT ON COLUMN lunaerp_production_dyeing_order.other_req IS '其他要求';
COMMENT ON COLUMN lunaerp_production_dyeing_order.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_dyeing_order.status IS '染色单状态（字典 lunaerp_dyeing_order_status：DRAFT/ACTIVE/COMPLETED/CANCELLED），创建缺省 DRAFT';
COMMENT ON COLUMN lunaerp_production_dyeing_order.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_dyeing_order.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_dyeing_order.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_dyeing_order.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_dyeing_order.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_production_dyeing_order.tenant_id IS '租户编号';

-- =============================================================================
-- 1b. 染色颜色明细子表 lunaerp_production_dyeing_color
--     颜色行是染色单聚合的一部分（ADR-0003）：无独立 Controller/Service/权限码，
--     写操作随染色单保存整体替换，删除染色单时级联逻辑删除。零明细合法。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_dyeing_color (
    id                       int8          NOT NULL,
    dyeing_order_id          int8          NOT NULL,
    line_no                  int4          NOT NULL DEFAULT 0,
    customer_color_no        varchar(64)   NULL,
    color_name               varchar(128)  NULL,
    quantity                 numeric(18,2) NOT NULL DEFAULT 0,
    pieces                   int4          NULL,
    dyeing_input_qty         numeric(18,2) NULL,
    dyeing_input_unit        varchar(16)   NULL,
    contract_qty             numeric(18,2) NULL,
    creator                  varchar(64)   NULL DEFAULT '',
    create_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)   NULL DEFAULT '',
    update_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2          NOT NULL DEFAULT 0,
    tenant_id                int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_dyeing_color') THEN
        ALTER TABLE lunaerp_production_dyeing_color ADD CONSTRAINT pk_lb_dyeing_color PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_dyeing_color_order_id
    ON lunaerp_production_dyeing_color (dyeing_order_id);

COMMENT ON TABLE lunaerp_production_dyeing_color IS '生产管理-染色单颜色明细子表';
COMMENT ON COLUMN lunaerp_production_dyeing_color.id IS '明细ID';
COMMENT ON COLUMN lunaerp_production_dyeing_color.dyeing_order_id IS '染色单ID（lunaerp_production_dyeing_order.id，必填）';
COMMENT ON COLUMN lunaerp_production_dyeing_color.line_no IS '行号（从 1 开始）';
COMMENT ON COLUMN lunaerp_production_dyeing_color.customer_color_no IS '客户色号';
COMMENT ON COLUMN lunaerp_production_dyeing_color.color_name IS '颜色名称';
COMMENT ON COLUMN lunaerp_production_dyeing_color.quantity IS '本单染色数量（成品口径）';
COMMENT ON COLUMN lunaerp_production_dyeing_color.pieces IS '匹数';
COMMENT ON COLUMN lunaerp_production_dyeing_color.dyeing_input_qty IS '投染数（含损耗的实际投入量）';
COMMENT ON COLUMN lunaerp_production_dyeing_color.dyeing_input_unit IS '投染数单位（字典 lunaerp_sales_contract_unit：KG/M）';
COMMENT ON COLUMN lunaerp_production_dyeing_color.contract_qty IS '合同数量（从合同明细带出，只读对比）';

-- =============================================================================
-- 2. 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_dyeing_order_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_dyeing_color_seq;

-- =============================================================================
-- 3. 菜单与按钮（system_menu，可重复执行）
--    10200 生产管理目录下挂载 10220 染色单菜单（承载 query 权限）。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10220, 10221, 10222, 10223);

-- 菜单：染色单（query 权限挂在菜单本身，参照胚布单/销售合同模式）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10220, '染色单', 'lunaerp:production:dyeing-order:query', 2, 2, 10200, 'dyeing-order', 'ep:brush', 'lunaerp-elmui/production/dyeing-order/index', 'LunaerpElmuiProductionDyeingOrder', 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
-- 按钮：染色单创建/修改/删除
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10221, '染色单创建', 'lunaerp:production:dyeing-order:create', 3, 1, 10220, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10222, '染色单修改', 'lunaerp:production:dyeing-order:update', 3, 2, 10220, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10223, '染色单删除', 'lunaerp:production:dyeing-order:delete', 3, 3, 10220, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 4. 字典类型与字典数据（system_dict_type / system_dict_data，可重复执行）
--    dict_type id 2222（紧接胚布单 2221），dict_data id 3571-3574（紧接胚布单 3568-3570）。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2222);
DELETE FROM system_dict_data WHERE dict_type IN ('lunaerp_dyeing_order_status');

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2222, '染色单状态', 'lunaerp_dyeing_order_status', 0, 'LunaERP 染色单-状态', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0', NULL);

-- 染色单状态：草稿/进行中/已完成/已取消（四态，ADR-0008）
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3571, 1, '草稿', 'DRAFT', 'lunaerp_dyeing_order_status', 0, 'warning', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3572, 2, '进行中', 'ACTIVE', 'lunaerp_dyeing_order_status', 0, 'success', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3573, 3, '已完成', 'COMPLETED', 'lunaerp_dyeing_order_status', 0, 'info', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3574, 4, '已取消', 'CANCELLED', 'lunaerp_dyeing_order_status', 0, 'danger', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');

COMMIT;
-- @formatter:on
