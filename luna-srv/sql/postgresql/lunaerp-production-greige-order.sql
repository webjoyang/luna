-- =============================================================================
-- 模块：LunaERP - 生产管理 - 胚布单（主表；颜色投胚分配子表随 04 票追加）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 胚布单主表 lunaerp_production_greige_order
--    坯布厂/染厂引用供应商档案并保存公司名快照（ADR-0007：引用不做供应商类型强过滤）；
--    合同/胚布品种为逻辑关联，不建数据库外键。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_greige_order (
    id                       int8          NOT NULL,
    order_no                 varchar(32)   NOT NULL,
    sales_contract_id        int8          NULL,
    contract_no              varchar(32)   NULL,
    greige_factory_id        int8          NOT NULL,
    greige_factory_name      varchar(128)  NOT NULL,
    dyeing_factory_id        int8          NULL,
    dyeing_factory_name      varchar(128)  NULL,
    order_date               date          NOT NULL,
    delivery_date            date          NULL,
    product_code             varchar(64)   NULL,
    product_name             varchar(128)  NULL,
    composition              varchar(128)  NULL,
    width                    varchar(64)   NULL,
    gsm                      varchar(64)   NULL,
    shrinkage                varchar(64)   NULL,
    piece_weight             numeric(10,2) NOT NULL DEFAULT 25,
    piece_count              int4          NULL,
    planned_qty              numeric(18,2) NOT NULL,
    finished_qty             numeric(18,2) NULL,
    suggested_loss           numeric(6,4)  NOT NULL DEFAULT 0,
    processing_fee           numeric(18,2) NULL,
    greige_type_id           varchar(64)   NULL,
    production_remark        varchar(1000) NULL,
    remark                   varchar(1000) NULL,
    status                   varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    creator                  varchar(64)   NULL DEFAULT '',
    create_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)   NULL DEFAULT '',
    update_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2          NOT NULL DEFAULT 0,
    tenant_id                int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_greige_order') THEN
        ALTER TABLE lunaerp_production_greige_order ADD CONSTRAINT pk_lb_greige_order PRIMARY KEY (id);
    END IF;
END $$;

-- 胚布单号租户内全量唯一（ADR-0006）：普通唯一索引、不带 WHERE deleted=0，
-- 按天取号、软删不释放编号。勿"修正"为部分唯一索引。
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_greige_order_tenant_no
    ON lunaerp_production_greige_order (tenant_id, order_no);
CREATE INDEX IF NOT EXISTS idx_lb_greige_order_status
    ON lunaerp_production_greige_order (status);
CREATE INDEX IF NOT EXISTS idx_lb_greige_order_order_date
    ON lunaerp_production_greige_order (order_date);

COMMENT ON TABLE lunaerp_production_greige_order IS '生产管理-胚布单主表';
COMMENT ON COLUMN lunaerp_production_greige_order.id IS '胚布单ID';
COMMENT ON COLUMN lunaerp_production_greige_order.order_no IS '胚布单号（PB+月日+3位顺序号，按天取号，可手填，租户内全量唯一含已删除行，见 ADR-0006）';
COMMENT ON COLUMN lunaerp_production_greige_order.sales_contract_id IS '销售合同ID（lunaerp_business_sales_contract.id，逻辑关联可空）';
COMMENT ON COLUMN lunaerp_production_greige_order.contract_no IS '合同号快照（选中合同时保存）';
COMMENT ON COLUMN lunaerp_production_greige_order.greige_factory_id IS '承织坯布厂供应商ID（lunaerp_baseinfo_supplier.id，必填，见 ADR-0007）';
COMMENT ON COLUMN lunaerp_production_greige_order.greige_factory_name IS '坯布厂公司名快照（选中供应商时保存，必填）';
COMMENT ON COLUMN lunaerp_production_greige_order.dyeing_factory_id IS '指定染厂供应商ID（lunaerp_baseinfo_supplier.id，可空，见 ADR-0007）';
COMMENT ON COLUMN lunaerp_production_greige_order.dyeing_factory_name IS '染厂公司名快照（选中供应商时保存）';
COMMENT ON COLUMN lunaerp_production_greige_order.order_date IS '单据日期（必填，创建缺省当天）';
COMMENT ON COLUMN lunaerp_production_greige_order.delivery_date IS '交货日期（可空）';
COMMENT ON COLUMN lunaerp_production_greige_order.product_code IS '产品编号';
COMMENT ON COLUMN lunaerp_production_greige_order.product_name IS '产品名称';
COMMENT ON COLUMN lunaerp_production_greige_order.composition IS '成分';
COMMENT ON COLUMN lunaerp_production_greige_order.width IS '幅宽（保留文本，兼容"160cm 有效"等写法）';
COMMENT ON COLUMN lunaerp_production_greige_order.gsm IS '克重（保留文本）';
COMMENT ON COLUMN lunaerp_production_greige_order.shrinkage IS '缩水率（描述性文本）';
COMMENT ON COLUMN lunaerp_production_greige_order.piece_weight IS '匹重 KG（缺省 25）';
COMMENT ON COLUMN lunaerp_production_greige_order.piece_count IS '匹数（前端按 ceil(投胚量÷匹重) 自动算，可手改；服务端不重算）';
COMMENT ON COLUMN lunaerp_production_greige_order.planned_qty IS '计划投胚量 KG（工厂经验口径，与颜色折算投胚需求之和不强制相等）';
COMMENT ON COLUMN lunaerp_production_greige_order.finished_qty IS '下单成品数量 KG';
COMMENT ON COLUMN lunaerp_production_greige_order.suggested_loss IS '预设损耗（小数，0.05 = 5%，缺省 0）';
COMMENT ON COLUMN lunaerp_production_greige_order.processing_fee IS '加工费';
COMMENT ON COLUMN lunaerp_production_greige_order.greige_type_id IS '所属胚布品种（胚布品种档案 id 为字符串，手选不自动识别）';
COMMENT ON COLUMN lunaerp_production_greige_order.production_remark IS '生产工艺备注';
COMMENT ON COLUMN lunaerp_production_greige_order.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_greige_order.status IS '胚布单状态（字典 lunaerp_greige_order_status：ACTIVE/COMPLETED/CANCELLED），创建缺省 ACTIVE';
COMMENT ON COLUMN lunaerp_production_greige_order.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_greige_order.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_greige_order.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_greige_order.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_greige_order.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_production_greige_order.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_greige_order_seq;

-- =============================================================================
-- 3. 菜单与按钮（system_menu，可重复执行）
--    10200 生产管理目录与 10000 基本信息、10100 业务管理平级；10210 胚布单菜单承载 query 权限。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10200, 10210, 10211, 10212, 10213);

-- 目录：生产管理（parent_id=0 顶级，与基本信息 10000 / 业务管理 10100 平级）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10200, '生产管理', '', 1, 102, 0, '/production', 'ep:set-up', NULL, NULL, 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
-- 菜单：胚布单（query 权限挂在菜单本身，参照产品/客户菜单模式）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10210, '胚布单', 'lunaerp:production:greige-order:query', 2, 1, 10200, 'greige-order', 'ep:threads', 'lunaerp-elmui/production/greige-order/index', 'LunaerpElmuiProductionGreigeOrder', 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
-- 按钮：胚布单创建/修改/删除（02/03/05 票的后端端点与前端按钮使用）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10211, '胚布单创建', 'lunaerp:production:greige-order:create', 3, 1, 10210, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10212, '胚布单修改', 'lunaerp:production:greige-order:update', 3, 2, 10210, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10213, '胚布单删除', 'lunaerp:production:greige-order:delete', 3, 3, 10210, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 4. 字典类型与字典数据（system_dict_type / system_dict_data，可重复执行）
--    dict_type id 2221（紧接供应商 2220），dict_data id 3568-3570（紧接供应商 3550-3567）。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2221);
DELETE FROM system_dict_data WHERE dict_type IN ('lunaerp_greige_order_status');

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2221, '胚布单状态', 'lunaerp_greige_order_status', 0, 'LunaERP 胚布单-状态', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0', NULL);

-- 胚布单状态：进行中/已完成/已取消（与销售合同状态口径一致）
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3568, 1, '进行中', 'ACTIVE', 'lunaerp_greige_order_status', 0, 'success', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3569, 2, '已完成', 'COMPLETED', 'lunaerp_greige_order_status', 0, 'info', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3570, 3, '已取消', 'CANCELLED', 'lunaerp_greige_order_status', 0, 'danger', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');

COMMIT;
-- @formatter:on
