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
-- PostgreSQL 无自增：id 显式取号，序列对齐现有最大 id 防主键冲突
CREATE SEQUENCE IF NOT EXISTS system_role_menu_seq;
DO $$
BEGIN
    PERFORM setval('system_role_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role_menu), 1));
END $$;

INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, 10300, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
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

-- 5. infra_config 参数配置种子：luna.portal.base-url（客户门户前端基准URL，发货单二维码签收用，默认空）
DELETE FROM infra_config WHERE config_key = 'luna.portal.base-url';
INSERT INTO infra_config (
    id, category, type, name, config_key, value, visible, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    101, 'biz', 2, '客户门户基准地址', 'luna.portal.base-url', '', '1',
    '客户门户前端基准URL，发货单二维码签收用，14号前可空，不阻断发货单打印仅二维码提示',
    'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);

-- 6. 回收站菜单与权限（id 10900，挂芋道系统管理目录 parent_id=1，path: recycle-bin）
-- 按钮权限：query(10901) / restore(10902) / purge(10903)
DELETE FROM system_menu WHERE id IN (10900, 10901, 10902, 10903)
   OR permission IN ('lunaerp:common:recycle:query', 'lunaerp:common:recycle:restore', 'lunaerp:common:recycle:purge');

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10900, '回收站', '', 2, 95, 1, 'recycle-bin', 'ep:delete', 'lunaerp/common/recycle/index', 'LunaerpRecycleBin',
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10901, '回收站查询', 'lunaerp:common:recycle:query', 3, 1, 10900, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10902, '回收站恢复', 'lunaerp:common:recycle:restore', 3, 2, 10900, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10903, '回收站彻底删除', 'lunaerp:common:recycle:purge', 3, 3, 10900, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);

DELETE FROM system_role_menu WHERE menu_id IN (10900, 10901, 10902, 10903);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (SELECT 10900 AS id UNION ALL SELECT 10901 UNION ALL SELECT 10902 UNION ALL SELECT 10903) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2)
ON CONFLICT DO NOTHING;

-- 7. 审批类站内信模板种子（system_notify_template，ON CONFLICT (code) 可重复执行）
-- W0 实装 3 个：LUNA_APPROVAL_TODO（各审批人）、LUNA_APPROVAL_APPROVED（发起人）、LUNA_APPROVAL_REJECTED（发起人，含意见）
CREATE UNIQUE INDEX IF NOT EXISTS uk_system_notify_template_code ON system_notify_template (code);

INSERT INTO system_notify_template (
    id, name, code, nickname, content, type, params, status, remark,
    creator, create_time, updater, update_time, deleted
) VALUES
(
    2001, '审批待办通知', 'LUNA_APPROVAL_TODO', '系统通知',
    '您有新的审批待办：{bizName} 单号 {bizNo}，提交人 {submitterName}',
    2, '["bizName", "bizNo", "submitterName"]', 0, '单据流转到达审批节点，通知该节点各审批人（会签每人一条）',
    'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
),
(
    2002, '审批通过通知', 'LUNA_APPROVAL_APPROVED', '系统通知',
    '您的审批已通过：{bizName} 单号 {bizNo}',
    2, '["bizName", "bizNo"]', 0, '审批实例终态通过，通知提交人',
    'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
),
(
    2003, '审批驳回通知', 'LUNA_APPROVAL_REJECTED', '系统通知',
    '您的审批被驳回：{bizName} 单号 {bizNo}，驳回意见：{reason}',
    2, '["bizName", "bizNo", "reason"]', 0, '审批实例终态驳回，通知提交人（附带驳回意见）',
    'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    nickname = EXCLUDED.nickname,
    content = EXCLUDED.content,
    type = EXCLUDED.type,
    params = EXCLUDED.params,
    status = EXCLUDED.status,
    remark = EXCLUDED.remark,
    updater = EXCLUDED.updater,
    update_time = EXCLUDED.update_time,
    deleted = 0;

COMMIT;

