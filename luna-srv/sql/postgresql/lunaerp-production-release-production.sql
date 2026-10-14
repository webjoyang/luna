-- =============================================================================
-- 模块：LunaERP - 生产管理 - 合同「下达生产」工作台（Release Production Workbench）
-- 数据库：PostgreSQL
-- 规范：
--   1. 幂等可重复执行：ALTER TABLE 采用 DO 块判列存在；菜单与权限采用 DELETE + INSERT 重新写入。
--   2. 3 张既有表横切加列（零新表）：
--      - lunaerp_business_sales_contract_item: released_qty numeric(18,2) NOT NULL DEFAULT 0（已下达生产数量）
--      - lunaerp_production_greige_order_color: contract_line_id int8 NULL, contract_line_no int4 NULL
--      - lunaerp_production_dyeing_color: contract_line_id int8 NULL, contract_line_no int4 NULL
--   3. 销售合同菜单 10110 下注册按钮权限 10114：下达生产
--      对应权限码：lunaerp:production:release-production:query 与 :release（按钮挂合同菜单、权限码归生产域）
--   4. 加列不影响既有数据（released_qty 缺省 0，关联列可空，符合 ADR-0040 下游零回写）
-- =============================================================================

-- =============================================================================
-- 1. 销售合同明细表：加 released_qty 已下达生产数量（缺省 0，不可空）
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lunaerp_business_sales_contract_item' AND column_name = 'released_qty'
    ) THEN
        ALTER TABLE lunaerp_business_sales_contract_item ADD COLUMN released_qty numeric(18,2) NOT NULL DEFAULT 0;
    END IF;
END $$;

COMMENT ON COLUMN lunaerp_business_sales_contract_item.released_qty IS '已下达生产数量（KG，缺省 0；下达时累加，下游单据修改/删除不回写 ADR-0040）';

-- =============================================================================
-- 2. 胚布单颜色行子表：加 contract_line_id 与 contract_line_no（可空，逻辑关联不建外键）
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lunaerp_production_greige_order_color' AND column_name = 'contract_line_id'
    ) THEN
        ALTER TABLE lunaerp_production_greige_order_color ADD COLUMN contract_line_id int8 NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lunaerp_production_greige_order_color' AND column_name = 'contract_line_no'
    ) THEN
        ALTER TABLE lunaerp_production_greige_order_color ADD COLUMN contract_line_no int4 NULL;
    END IF;
END $$;

COMMENT ON COLUMN lunaerp_production_greige_order_color.contract_line_id IS '来源合同行 ID（lunaerp_business_sales_contract_item.id），逻辑关联不建外键';
COMMENT ON COLUMN lunaerp_production_greige_order_color.contract_line_no IS '来源合同行号快照';

CREATE INDEX IF NOT EXISTS idx_lp_greige_order_color_contract_line_id
    ON lunaerp_production_greige_order_color (contract_line_id);

-- =============================================================================
-- 3. 染色单颜色明细子表：加 contract_line_id 与 contract_line_no（可空，逻辑关联不建外键）
-- =============================================================================
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lunaerp_production_dyeing_color' AND column_name = 'contract_line_id'
    ) THEN
        ALTER TABLE lunaerp_production_dyeing_color ADD COLUMN contract_line_id int8 NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lunaerp_production_dyeing_color' AND column_name = 'contract_line_no'
    ) THEN
        ALTER TABLE lunaerp_production_dyeing_color ADD COLUMN contract_line_no int4 NULL;
    END IF;
END $$;

COMMENT ON COLUMN lunaerp_production_dyeing_color.contract_line_id IS '来源合同行 ID（lunaerp_business_sales_contract_item.id），逻辑关联不建外键';
COMMENT ON COLUMN lunaerp_production_dyeing_color.contract_line_no IS '来源合同行号快照';

CREATE INDEX IF NOT EXISTS idx_lp_dyeing_color_contract_line_id
    ON lunaerp_production_dyeing_color (contract_line_id);

-- =============================================================================
-- 4. 菜单与按钮权限（system_menu，可重复执行）
--    销售合同菜单 10110 下注册按钮权限 10114：下达生产
--    权限码：lunaerp:production:release-production:query 与 :release
--    按钮挂合同菜单（10110），权限码归生产域
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id = 10114;

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10114, '下达生产', 'lunaerp:production:release-production:release', 3, 4, 10110, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0'
);

-- 授权管理员与超管角色
DELETE FROM system_role_menu WHERE menu_id = 10114;
INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, 10114, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM (
    SELECT DISTINCT id FROM system_role
    WHERE code IN ('super_admin', 'admin') OR id IN (1, 2)
) r;

COMMIT;
-- @formatter:on
