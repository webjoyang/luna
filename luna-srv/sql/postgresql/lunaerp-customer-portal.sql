-- =============================================================================
-- 模块：LunaERP - 客户门户 (Customer Portal)
-- 数据库：PostgreSQL
-- 规范：ruoyi-vue-pro 权限按钮定义
-- 说明：管理端客户档案「客户门户链接」权限按钮（ID: 10150，父菜单 10030）
-- 幂等脚本：先 DELETE 再 INSERT
-- =============================================================================
BEGIN;

DELETE FROM system_menu WHERE id = 10150;

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id,
    path, icon, component, component_name,
    status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
) VALUES (
    10150, '客户门户链接', 'lunaerp:baseinfo:customer:portal-token', 3, 50, 10030,
    '', '#', '', NULL,
    0, '1', '1', '1',
    'admin', '2026-10-12 00:00:00', '1', '2026-10-12 00:00:00', '0'
);

COMMIT;
