-- =============================================================================
-- 模块：LunaERP - 生产管理 - 下达生产（Release Production）基建
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro
-- 可重复执行：DO 块判存加列、COMMENT ON COLUMN、菜单与权限先 DELETE 再 INSERT
-- =============================================================================

-- =============================================================================
-- 1. 表结构变更（加列与注释）
-- =============================================================================

-- 1.1 销售合同明细表：增加已下达生产数量
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lunaerp_business_sales_contract_item' AND column_name = 'released_qty'
    ) THEN
        ALTER TABLE lunaerp_business_sales_contract_item
            ADD COLUMN released_qty numeric(18,2) NOT NULL DEFAULT 0;
    END IF;
END $$;

COMMENT ON COLUMN lunaerp_business_sales_contract_item.released_qty IS '已下达生产数量（KG，缺省 0；下达时累加，下游单据修改/删除不回写）';

-- 1.2 胚布单颜色分配表：增加合同行关联
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lunaerp_production_greige_order_color' AND column_name = 'contract_line_id'
    ) THEN
        ALTER TABLE lunaerp_production_greige_order_color
            ADD COLUMN contract_line_id int8 NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lunaerp_production_greige_order_color' AND column_name = 'contract_line_no'
    ) THEN
        ALTER TABLE lunaerp_production_greige_order_color
            ADD COLUMN contract_line_no int4 NULL;
    END IF;
END $$;

COMMENT ON COLUMN lunaerp_production_greige_order_color.contract_line_id IS '来源合同行ID（lunaerp_business_sales_contract_item.id，逻辑关联）';
COMMENT ON COLUMN lunaerp_production_greige_order_color.contract_line_no IS '来源合同行号快照';

-- 1.3 染色单颜色明细表：增加合同行关联
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lunaerp_production_dyeing_color' AND column_name = 'contract_line_id'
    ) THEN
        ALTER TABLE lunaerp_production_dyeing_color
            ADD COLUMN contract_line_id int8 NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lunaerp_production_dyeing_color' AND column_name = 'contract_line_no'
    ) THEN
        ALTER TABLE lunaerp_production_dyeing_color
            ADD COLUMN contract_line_no int4 NULL;
    END IF;
END $$;

COMMENT ON COLUMN lunaerp_production_dyeing_color.contract_line_id IS '来源合同行ID（lunaerp_business_sales_contract_item.id，逻辑关联）';
COMMENT ON COLUMN lunaerp_production_dyeing_color.contract_line_no IS '来源合同行号快照';

-- =============================================================================
-- 2. 菜单与按钮权限（system_menu / system_role_menu）
--    销售合同菜单 10110 下注册「下达生产」与「下达生产查询」按钮权限
-- =============================================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'system_role_menu' AND column_name = 'id' AND column_default IS NULL
    ) AND EXISTS (
        SELECT 1 FROM pg_class WHERE relname = 'system_role_menu_seq'
    ) THEN
        ALTER TABLE system_role_menu ALTER COLUMN id SET DEFAULT nextval('system_role_menu_seq');
    END IF;
END $$;

-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10114, 10115);

-- 按钮：下达生产（写操作）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10114, '下达生产', 'lunaerp:production:release-production:release', 3, 4, 10110, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0');

-- 按钮：下达生产查询（读操作）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10115, '下达生产查询', 'lunaerp:production:release-production:query', 3, 5, 10110, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0');

-- 授权管理员与超管角色
DELETE FROM system_role_menu WHERE menu_id IN (10114, 10115);

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (
    SELECT 10114 AS id UNION ALL SELECT 10115
) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2)
ON CONFLICT DO NOTHING;

COMMIT;
-- @formatter:on
