-- =============================================================================
-- 模块：LunaERP - 生产管理 - 发染记录（胚布→染厂流转记录，PROCESS_OUT 过账）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单先 DELETE 再 INSERT。
-- 设计依据：ADR-0033（发染记录挂胚布单，PROCESS_OUT 过账、不足拦截、受限编辑）
-- =============================================================================

-- =============================================================================
-- 1. 发染记录表 lunaerp_production_greige_dispatch_record
--    挂胚布单的轻量流转记录：一次发运一条，产品快照取胚布单品种；
--    创建写 PROCESS_OUT 负流水（扣胚布库存），删除写 PROCESS_OUT_REVERSAL 正红冲；
--    可补填指向染色单与染厂，数量/日期/匹数/仓库创建后锁定。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_greige_dispatch_record (
    id                       int8          NOT NULL,
    greige_order_id          int8          NOT NULL,
    dyeing_order_id          int8          NULL,
    dyeing_factory_id        int8          NULL,
    dyeing_factory_name      varchar(255)  NULL,
    warehouse_id             int8          NOT NULL,
    warehouse_name           varchar(255)  NOT NULL,
    product_id               int8          NOT NULL,
    product_code             varchar(64)   NOT NULL,
    product_name             varchar(255)  NOT NULL,
    dispatch_date            date          NOT NULL,
    quantity                 numeric(18,2) NOT NULL,
    pieces                   int4          NULL,
    stock_ledger_id          int8          NULL,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_greige_dispatch') THEN
        ALTER TABLE lunaerp_production_greige_dispatch_record ADD CONSTRAINT pk_lb_greige_dispatch PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_greige_dispatch_tenant_greige_order
    ON lunaerp_production_greige_dispatch_record (tenant_id, greige_order_id);
CREATE INDEX IF NOT EXISTS idx_lb_greige_dispatch_tenant_dyeing_order
    ON lunaerp_production_greige_dispatch_record (tenant_id, dyeing_order_id);
CREATE INDEX IF NOT EXISTS idx_lb_greige_dispatch_ledger
    ON lunaerp_production_greige_dispatch_record (stock_ledger_id);

COMMENT ON TABLE lunaerp_production_greige_dispatch_record IS '生产管理-发染记录（胚布→染厂流转，PROCESS_OUT 过账）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.id IS '发染记录ID';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.greige_order_id IS '关联胚布单ID（lunaerp_production_greige_order.id，必填，逻辑关联无外键）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.dyeing_order_id IS '关联染色单ID（lunaerp_production_dyeing_order.id，可空，可后续补填）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.dyeing_factory_id IS '承染染厂供应商ID（lunaerp_baseinfo_supplier.id，可空，可后续补填）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.dyeing_factory_name IS '染厂公司名快照（选中供应商时保存）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.warehouse_id IS '出库仓库ID（lunaerp_stock_warehouse.id，必填，仅可选启用仓）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.warehouse_name IS '仓库名称快照（创建时保存）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.product_id IS '产品ID（lunaerp_baseinfo_product.id，快照自胚布单）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.product_code IS '产品编码快照（快照自胚布单）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.product_name IS '产品名称快照（快照自胚布单）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.dispatch_date IS '发染日期（必填，创建后锁定不可改）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.quantity IS '发染数量 KG（必填，创建后锁定不可改）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.pieces IS '匹数（可空，创建后锁定不可改）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.stock_ledger_id IS '关联库存流水ID（lunaerp_stock_ledger.id，创建时写入的 PROCESS_OUT 流水）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.remark IS '备注（可改）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_production_greige_dispatch_record.tenant_id IS '租户编号';

CREATE SEQUENCE IF NOT EXISTS lunaerp_production_greige_dispatch_record_seq
    START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- =============================================================================
-- 2. 菜单：生产管理 → 发染记录
-- =============================================================================
DELETE FROM system_menu WHERE permission LIKE 'lunaerp:greige-dispatch-record:%';

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES ('发染记录', '', 2, 50, (SELECT id FROM (SELECT id FROM system_menu WHERE name = '生产管理' AND type = 1 ORDER BY id LIMIT 1) t), 'greige-dispatch-record', 'ep:van', 'production/greigeDispatchRecord/index', 'GreigeDispatchRecord', 0, true, true, true, '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0, 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES ('发染记录查询', 'lunaerp:greige-dispatch-record:query', 3, 1, (SELECT id FROM (SELECT id FROM system_menu WHERE permission = 'lunaerp:greige-dispatch-record:query' OR (name = '发染记录' AND type = 2) ORDER BY id DESC LIMIT 1) t), '', '', '', '', 0, true, true, true, '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0, 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES ('发染记录创建', 'lunaerp:greige-dispatch-record:create', 3, 2, (SELECT id FROM (SELECT id FROM system_menu WHERE permission = 'lunaerp:greige-dispatch-record:query' OR (name = '发染记录' AND type = 2) ORDER BY id DESC LIMIT 1) t), '', '', '', '', 0, true, true, true, '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0, 0);

INSERT INTO system_menu (name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES ('发染记录删除', 'lunaerp:greige-dispatch-record:delete', 3, 3, (SELECT id FROM (SELECT id FROM system_menu WHERE permission = 'lunaerp:greige-dispatch-record:query' OR (name = '发染记录' AND type = 2) ORDER BY id DESC LIMIT 1) t), '', '', '', '', 0, true, true, true, '1', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0, 0);
