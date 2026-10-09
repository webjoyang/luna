-- =============================================================================
-- 模块：LunaERP - 菜单 IA 迁移 SQL（一级目录改名 + 幂等）
-- 数据库：PostgreSQL
-- 规范：Q18 目标分组名映射（Issue 01 & 04 & spec）
-- 包含 5 个一级目录改名：
--   10000: 基本信息 -> 基础信息
--   10100: 业务管理 -> 销售
--   10200: 生产管理 -> 生产
--   10300: 库存管理 -> 库存
--   10600: 样品管理 -> 样品
-- 保持 id, parent_id, path 不变
-- 幂等性：ON CONFLICT (id) DO UPDATE 与 UPDATE 结合，支持空库/存量库反复执行
-- =============================================================================
BEGIN;

-- 1. 10000: 基础信息
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10000, '基础信息', '', 1, 100, 0, '/baseinfo', 'ep:folder-opened', NULL, NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
)
ON CONFLICT (id) DO UPDATE SET
    name = '基础信息',
    updater = 'admin',
    update_time = CURRENT_TIMESTAMP;

-- 2. 10100: 销售
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10100, '销售', '', 1, 101, 0, '/business', 'ep:briefcase', NULL, NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
)
ON CONFLICT (id) DO UPDATE SET
    name = '销售',
    updater = 'admin',
    update_time = CURRENT_TIMESTAMP;

-- 3. 10200: 生产
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10200, '生产', '', 1, 102, 0, '/production', 'ep:set-up', NULL, NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
)
ON CONFLICT (id) DO UPDATE SET
    name = '生产',
    updater = 'admin',
    update_time = CURRENT_TIMESTAMP;

-- 4. 10300: 库存
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10300, '库存', '', 1, 103, 0, '/stock', 'ep:box', NULL, NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
)
ON CONFLICT (id) DO UPDATE SET
    name = '库存',
    updater = 'admin',
    update_time = CURRENT_TIMESTAMP;

-- 5. 10600: 样品
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10600, '样品', '', 1, 104, 0, '/sample', 'ep:collection', NULL, NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0
)
ON CONFLICT (id) DO UPDATE SET
    name = '样品',
    updater = 'admin',
    update_time = CURRENT_TIMESTAMP;

-- 6. 防御性校验与校准二级菜单归属：
-- 纱线采购(10230)、发染记录(10500) 确保归生产(10200)
UPDATE system_menu SET parent_id = 10200 WHERE id IN (10230, 10500) AND parent_id <> 10200;
-- 供应商信息(10050) 确保归基础信息(10000)
UPDATE system_menu SET parent_id = 10000 WHERE id = 10050 AND parent_id <> 10000;
-- 入库管理(10310) 确保归库存(10300)
UPDATE system_menu SET parent_id = 10300 WHERE id = 10310 AND parent_id <> 10300;

COMMIT;
