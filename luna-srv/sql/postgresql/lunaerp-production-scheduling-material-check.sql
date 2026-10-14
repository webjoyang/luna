-- =============================================================================
-- 模块：LunaERP - 生产管理 - 排产中心与齐套检查（轻量排产只读视图与齐套试算看板）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（菜单与角色权限先 DELETE 再 INSERT，可重复执行）
-- 设计依据：ADR-0041（轻量排产与齐套：只读视图+既有实体校验，不建排产表/染缸台账/库存冻结）
--
-- 【零 DDL 架构决策】
--   零建表、零加列、零字典、零序列：
--   1. 排产甘特/待排池均为既有机台任务、织机档案、织造单的动态聚合视图，不建排产单/排产计划表；
--   2. 齐套检查为配比 BOM、库存台账与纱线采购单的实时试算，不落库、不冻结库存；
--   3. 排缸日历为进度反馈页内月历视图，复用 progress-feedback:query 既有权限，不占菜单；
--   4. 排产中心不新增按钮位，页内派工/改期/转机复用既有 weaving-order-task:create/update 权限位；
--   5. 齐套检查纯只读，无按钮位。
--
-- 【菜单号段与排序说明】
--   生产管理目录 = 10200；
--   10510 排产中心（sort=12，排在生产进度 10410/sort=11 之后）；
--   10520 齐套检查（sort=13，排在排产中心之后）。
-- =============================================================================

-- @formatter:off
BEGIN;

-- =============================================================================
-- 1. 菜单注册与清理（system_menu，可重复执行）
-- =============================================================================
DELETE FROM system_menu WHERE id IN (10510, 10520)
   OR permission IN ('lunaerp:production:weaving-order-task:query', 'lunaerp:production:material-check:query');

-- 10510 菜单：排产中心（挂生产管理目录 10200）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10510, '排产中心', 'lunaerp:production:weaving-order-task:query', 2, 12, 10200, 'scheduling', 'ep:calendar',
    'lunaerp-elmui/production/scheduling/index', 'LunaerpElmuiProductionScheduling',
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);

-- 10520 菜单：齐套检查（挂生产管理目录 10200）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10520, '齐套检查', 'lunaerp:production:material-check:query', 2, 13, 10200, 'material-check', 'ep:files',
    'lunaerp-elmui/production/material-check/index', 'LunaerpElmuiProductionMaterialCheck',
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);

-- =============================================================================
-- 2. 角色授权（system_role_menu，为 super_admin / admin 超管与管理员授权）
-- =============================================================================
DELETE FROM system_role_menu WHERE menu_id IN (10510, 10520);

INSERT INTO system_role_menu (
    id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id
)
SELECT
    nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM (
    SELECT DISTINCT id FROM system_role
    WHERE code IN ('super_admin', 'admin') OR id IN (1, 2)
) r
CROSS JOIN (
    SELECT id FROM system_menu WHERE id IN (10510, 10520)
) m;

COMMIT;
-- @formatter:on
