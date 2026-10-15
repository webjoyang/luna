-- =============================================================================
-- 模块：LunaERP - 库存管理 - 入库单增加纱线采购单可选上游引用
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、deleted/tenant_id 标准列，列 COMMENT）
-- 可重复执行：ALTER TABLE ... ADD COLUMN IF NOT EXISTS + CREATE INDEX IF NOT EXISTS
-- 关联：Issue 01 (SQL 基建) / Issue 07 (入库单纱线采购单引用) / spec 数据模型 stock 增量
-- 说明：为入库单 lunaerp_stock_in 增加第 5 个可选上游单据引用（纱线采购单 ID + 快照），
--       旧数据 NULL 兼容，零回写。
-- =============================================================================

ALTER TABLE lunaerp_stock_in ADD COLUMN IF NOT EXISTS yarn_order_id int8;
ALTER TABLE lunaerp_stock_in ADD COLUMN IF NOT EXISTS yarn_order_no varchar(32);

COMMENT ON COLUMN lunaerp_stock_in.yarn_order_id IS '关联纱线采购单ID（lunaerp_production_yarn_order.id，可选，第 5 个上游引用）';
COMMENT ON COLUMN lunaerp_stock_in.yarn_order_no IS '纱线采购单号快照（追溯用，可选）';

-- 纱线采购单引用索引
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_yarn_order_id
    ON lunaerp_stock_in (tenant_id, yarn_order_id);
