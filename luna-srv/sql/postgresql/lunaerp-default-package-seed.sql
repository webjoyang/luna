-- =============================================================================
-- 模块：LunaERP - 租户模块开通网关 - 默认「核心包」套餐种子数据 SQL
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（system_tenant_package / system_tenant 表）
-- 幂等性：事务内 UPSERT（ON CONFLICT DO UPDATE），可重复执行
-- 包含 9 大模块集合：
--   1. 基础信息（10000 及所有子菜单/按钮）
--   2. 销售（10100 及所有子菜单/按钮）
--   3. 生产（10200 及所有子菜单/按钮）
--   4. 库存（10300 及所有子菜单/按钮）
--   5. 样品（10600 及所有子菜单/按钮）
--   6. 财务（10700 及规划子菜单）
--   7. 审批中心（10800 及规划子菜单）
--   8. 系统管理（1 及所有子菜单/按钮）
--   9. 首页（静态路由内置）
-- =============================================================================
BEGIN;

-- 1. 清理可能同名但 id 冲突的旧核心包记录（保证套餐名唯一）
DELETE FROM system_tenant_package WHERE name = '核心包' AND id <> 100;

-- 2. 递归收集核心包所覆盖的 9 个一级目录整树（已落地的全部子菜单与按钮 + 待落地的 10700/10800）
WITH RECURSIVE module_menus AS (
    -- 根节点：系统管理(1)、基础信息(10000)、销售(10100)、生产(10200)、库存(10300)、样品(10600)、财务(10700)、审批中心(10800)
    SELECT id
    FROM system_menu
    WHERE id IN (1, 10000, 10100, 10200, 10300, 10600, 10700, 10800)
      AND deleted = 0
    UNION ALL
    -- 递归向下关联所有子菜单与按钮
    SELECT m.id
    FROM system_menu m
    JOIN module_menus mm ON m.parent_id = mm.id
    WHERE m.deleted = 0
),
core_menu_ids AS (
    SELECT id FROM module_menus
    UNION
    -- 确保财务(10700)与审批中心(10800)在尚未建表落库前，其根 ID 亦已预置在核心包中
    SELECT unnest(ARRAY[10700, 10800]::bigint[]) AS id
),
agg_menu AS (
    SELECT COALESCE(json_agg(id ORDER BY id)::text, '[]') AS menu_ids_json
    FROM core_menu_ids
)
INSERT INTO system_tenant_package (
    id, name, status, remark, menu_ids, creator, create_time, updater, update_time, deleted
)
SELECT
    100,
    '核心包',
    0,
    '系统默认核心包套餐，包含基础信息、销售、生产、库存、样品、财务、审批中心、系统管理及首页',
    menu_ids_json,
    'admin',
    CURRENT_TIMESTAMP,
    'admin',
    CURRENT_TIMESTAMP,
    0
FROM agg_menu
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    status = EXCLUDED.status,
    remark = EXCLUDED.remark,
    menu_ids = EXCLUDED.menu_ids,
    updater = EXCLUDED.updater,
    update_time = EXCLUDED.update_time,
    deleted = 0;

-- 3. 新租户默认绑定核心包配置（system_tenant.package_id 默认值设为 100）
ALTER TABLE system_tenant ALTER COLUMN package_id SET DEFAULT 100;

-- 4. 存量非系统演示租户（如小租户 121、测试租户 122）若仍为普通套餐(111)，同步切换为核心包(100)
UPDATE system_tenant
SET package_id = 100, updater = 'admin', update_time = CURRENT_TIMESTAMP
WHERE id IN (121, 122) AND package_id = 111;

COMMIT;
