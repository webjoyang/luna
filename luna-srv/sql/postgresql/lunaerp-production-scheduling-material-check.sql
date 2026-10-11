-- =============================================================================
-- 模块：LunaERP - 生产管理 - 轻量排产与齐套检查（Scheduling & Material Check）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（菜单先 DELETE 再 INSERT，可重复执行）
-- 设计依据：ADR-0041（轻量排产与齐套只读视图）
--
-- 【无表结构变更】
--   零新表、零加列、零字典、零序列：
--   - 排产中心 = 现有机台任务之上的查询视图层与 Service 冲突校验层
--   - 齐套检查 = 织造单需求与库存/在途的只读实时试算投影
--   - 染缸日历 = 进度反馈页内日历视图扩展（不占菜单不加权限）
--
-- 【菜单号段说明】
--   生产管理目录 = 10200
--   10510: 排产中心（sort 12，权限码 lunaerp:production:weaving-order-task:query）
--   10520: 齐套检查（sort 13，权限码 lunaerp:production:material-check:query）
--
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 序列兼容处理（system_role_menu_seq）
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

-- =============================================================================
-- 2. 菜单与权限（system_menu / system_role_menu，可重复执行）
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10510, 10520);

-- 菜单：排产中心（挂生产管理目录 10200，组件路径 lunaerp-elmui/production/scheduling/index）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10510, '排产中心', 'lunaerp:production:weaving-order-task:query', 2, 12, 10200, 'scheduling', 'ep:calendar', 'lunaerp-elmui/production/scheduling/index', 'LunaerpElmuiScheduling', 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0');

-- 菜单：齐套检查（挂生产管理目录 10200，组件路径 lunaerp-elmui/production/material-check/index）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10520, '齐套检查', 'lunaerp:production:material-check:query', 2, 13, 10200, 'material-check', 'ep:files', 'lunaerp-elmui/production/material-check/index', 'LunaerpElmuiMaterialCheck', 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, '0');

-- 授权管理员与超管角色
DELETE FROM system_role_menu WHERE menu_id IN (10510, 10520);

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (
    SELECT 10510 AS id UNION ALL SELECT 10520
) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2)
ON CONFLICT DO NOTHING;

COMMIT;
-- @formatter:on
