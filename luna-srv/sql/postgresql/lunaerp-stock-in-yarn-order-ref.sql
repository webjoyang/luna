-- =============================================================================
-- 模块：LunaERP - 库存管理 - 入库单增加纱线采购单可选上游引用
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro
-- 可重复执行：ALTER TABLE ADD COLUMN IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             历史数据 NULL 兼容。
-- =============================================================================

-- =============================================================================
-- 1. lunaerp_stock_in 增加关联纱线采购单可选上游引用列
-- =============================================================================
ALTER TABLE lunaerp_stock_in ADD COLUMN IF NOT EXISTS yarn_order_id int8;
ALTER TABLE lunaerp_stock_in ADD COLUMN IF NOT EXISTS yarn_order_no varchar(32);

COMMENT ON COLUMN lunaerp_stock_in.yarn_order_id IS '关联纱线采购单ID（lunaerp_production_yarn_order.id，可选，零回写）';
COMMENT ON COLUMN lunaerp_stock_in.yarn_order_no IS '关联纱线采购单号快照（可选）';

-- =============================================================================
-- 2. 纱线采购单引用索引
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_lsi_yarn_order
    ON lunaerp_stock_in (tenant_id, yarn_order_id)
    WHERE deleted = 0;
