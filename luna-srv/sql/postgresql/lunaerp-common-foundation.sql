-- =============================================================================
-- 模块：LunaERP - 商业地基通用件 - 敏感字段查看权限
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（system_menu 表）
-- 可重复执行：DELETE 先清后插
-- =============================================================================
BEGIN;

-- 清理旧的菜单记录（幂等）
DELETE FROM system_menu WHERE id IN (10950, 10951) OR permission IN ('lunaerp:sensitive:amount', 'lunaerp:sensitive:contact');

-- 1. 金额敏感权限（挂系统管理目录 id=1 下，type=3 按钮权限，无页面）
-- 默认授予建议：租户管理员、财务、老板角色（在角色管理中可勾选授权）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10950, '敏感数据-金额查看', 'lunaerp:sensitive:amount', 3, 90, 1, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);

-- 2. 联系人敏感权限（挂系统管理目录 id=1 下，type=3 按钮权限，无页面）
-- 默认授予建议：业务员（配合数据范围）、销售主管、管理员（在角色管理中可勾选授权）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10951, '敏感数据-联系人查看', 'lunaerp:sensitive:contact', 3, 91, 1, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);

-- 3. 布票打印权限（挂在 10280 织造单下，type=3 按钮权限）
DELETE FROM system_menu WHERE id = 10300 OR permission = 'lunaerp:production:weaving-ticket:print';
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10300, '布票打印', 'lunaerp:production:weaving-ticket:print', 3, 16, 10280, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);

DELETE FROM system_role_menu WHERE menu_id = 10300;
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, 10300, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2)
ON CONFLICT DO NOTHING;

-- 4. infra_config 参数配置种子：luna.h5.base-url（车间移动H5前端基准URL，布票二维码生成用，默认空）
DELETE FROM infra_config WHERE config_key = 'luna.h5.base-url';
INSERT INTO infra_config (
    id, category, type, name, config_key, value, visible, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    100, 'biz', 2, '车间移动H5基准地址', 'luna.h5.base-url', '', '1',
    '车间移动H5前端基准URL，布票二维码生成用，缺失时打印接口强校验拦截',
    'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);

COMMIT;
