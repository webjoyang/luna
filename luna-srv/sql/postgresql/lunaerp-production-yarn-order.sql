-- =============================================================================
-- 模块：LunaERP - 生产管理 - 纱线采购单与库存台账
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 5. 纱线库存通用流水台账 lunaerp_production_stock_ledger
--    通用流水台账表（去 source_location/target_location 两列，ADR-0012）。
--    append-only 流水，无 CRUD 端点，仅由后端服务内部调用 + 暴露余额查询接口。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_stock_ledger (
    id                int8          NOT NULL,
    yarn_spec_id      int8          NOT NULL,
    type              varchar(32)   NOT NULL,
    quantity          numeric(18,2) NOT NULL DEFAULT 0,
    related_doc_type  varchar(32)   NOT NULL,
    related_doc_id    int8          NOT NULL,
    batch_no          varchar(64)   NULL,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_stock_ledger') THEN
        ALTER TABLE lunaerp_production_stock_ledger ADD CONSTRAINT pk_lb_stock_ledger PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_stock_ledger_spec_type
    ON lunaerp_production_stock_ledger (yarn_spec_id, type);
CREATE INDEX IF NOT EXISTS idx_lb_stock_ledger_related_doc
    ON lunaerp_production_stock_ledger (related_doc_type, related_doc_id);

COMMENT ON TABLE lunaerp_production_stock_ledger IS '生产管理-纱线库存通用流水台账';
COMMENT ON COLUMN lunaerp_production_stock_ledger.id IS '流水ID';
COMMENT ON COLUMN lunaerp_production_stock_ledger.yarn_spec_id IS '纱线规格ID（lunaerp_production_yarn_spec.id）';
COMMENT ON COLUMN lunaerp_production_stock_ledger.type IS '流水类型（STOCK_IN 入库 / STOCK_OUT 出库 / PROCESS_IN 生产入库 / PROCESS_OUT 生产出库，MVP仅用STOCK_IN）';
COMMENT ON COLUMN lunaerp_production_stock_ledger.quantity IS '变动数量（STOCK_IN正/STOCK_OUT负）';
COMMENT ON COLUMN lunaerp_production_stock_ledger.related_doc_type IS '关联单据类型（如 YARN_ORDER）';
COMMENT ON COLUMN lunaerp_production_stock_ledger.related_doc_id IS '关联单据ID';
COMMENT ON COLUMN lunaerp_production_stock_ledger.batch_no IS '批号';
COMMENT ON COLUMN lunaerp_production_stock_ledger.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_stock_ledger.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_stock_ledger.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_stock_ledger.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_stock_ledger.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_stock_ledger.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_production_stock_ledger.tenant_id IS '租户编号';

-- 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_stock_ledger_seq;
