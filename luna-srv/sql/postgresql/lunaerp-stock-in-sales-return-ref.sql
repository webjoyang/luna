-- =============================================================================
-- 模块：LunaERP - 库存管理 - 入库单增加销售退货等通用来源单据引用
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、deleted/tenant_id 标准列，列 COMMENT）
-- 可重复执行：ALTER TABLE ... ADD COLUMN IF NOT EXISTS + CREATE INDEX IF NOT EXISTS
-- 关联：Issue 01 (SQL 基建) / ADR-0042 / spec 数据模型 stock 增量
-- 说明：为入库单 lunaerp_stock_in 增加通用来源两列（source_doc_type / source_doc_id），
--       并增加部分唯一索引 uk_stock_in_source（1:1 回指约束），旧数据 NULL 兼容。
-- =============================================================================

ALTER TABLE lunaerp_stock_in ADD COLUMN IF NOT EXISTS source_doc_type varchar(32);
ALTER TABLE lunaerp_stock_in ADD COLUMN IF NOT EXISTS source_doc_id int8;

COMMENT ON COLUMN lunaerp_stock_in.source_doc_type IS '来源单据类型（SALES_RETURN 等通用业务来源）';
COMMENT ON COLUMN lunaerp_stock_in.source_doc_id IS '来源单据ID（lunaerp_business_sales_return.id 等）';

-- 部分唯一索引：同一种来源单据至多对应一张未删除入库单，手工新建（NULL）不受约束
CREATE UNIQUE INDEX IF NOT EXISTS uk_stock_in_source
    ON lunaerp_stock_in (source_doc_type, source_doc_id)
    WHERE deleted = 0 AND source_doc_type IS NOT NULL;

-- 普通索引：租户下按来源快速反查
CREATE INDEX IF NOT EXISTS idx_stock_in_tenant_source
    ON lunaerp_stock_in (tenant_id, source_doc_type, source_doc_id);
