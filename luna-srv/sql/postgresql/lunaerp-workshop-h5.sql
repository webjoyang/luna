-- =============================================================================
-- 模块：LunaERP - 生产管理 - 车间移动 H5（权限容器、角色与配置种子）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro
-- 可重复执行：DELETE 先清后插 / ON CONFLICT
-- =============================================================================
BEGIN;

-- [2026-10-21] 02-infra-config-dict-frontend-deps: 车间移动H5发纱默认仓库配置项种子
DELETE FROM infra_config WHERE config_key = 'luna.workshop.default-warehouse-id';
INSERT INTO infra_config (
    id, category, type, name, config_key, value, visible, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    103, 'biz', 2, '车间移动H5发纱默认仓库', 'luna.workshop.default-warehouse-id', '', '1',
    '车间移动H5发纱默认仓库ID，系统管理参数设置维护，未配置时前端强制手动选择',
    'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);

-- [2026-10-21] 03-weaving-ticket-exception-photos-column: 布票加异常照片列
ALTER TABLE lunaerp_production_weaving_ticket
    ADD COLUMN IF NOT EXISTS exception_photos jsonb;
COMMENT ON COLUMN lunaerp_production_weaving_ticket.exception_photos IS '异常现场照片 URL JSON 数组（车间 H5 异常登记上传，≤6 张，infra 文件服务）';

-- [2026-10-21] 04-inspection-item-photos-column: 录检明细加疵点照片列
ALTER TABLE lunaerp_production_inspection_order_item
    ADD COLUMN IF NOT EXISTS photos jsonb;
COMMENT ON COLUMN lunaerp_production_inspection_order_item.photos IS '疵点照片 URL JSON 数组（车间 H5 录检上传，≤6 张，infra 文件服务）';

-- [2026-10-21] 05-vatno-validation-relax-and-menu-role-seed: 录检明细缸号校验放宽，允许为 NULL
ALTER TABLE lunaerp_production_inspection_order_item
    ALTER COLUMN vat_no DROP NOT NULL;
COMMENT ON COLUMN lunaerp_production_inspection_order_item.vat_no IS '缸号（type=FINISHED 或 packageMode=ROLL 必填；type=GREIGE 且 packageMode=PIECE 可空）';

-- [2026-10-21] 09-backend-skeleton-my-tasks: 机台任务按挡车工与状态组合索引（支撑 my-tasks P95 < 1s 性能硬指标）
CREATE INDEX IF NOT EXISTS idx_lb_machine_task_worker_status
    ON lunaerp_production_machine_task (tenant_id, worker_user_id, status);

-- [2026-10-21] 05-vatno-validation-relax-and-menu-role-seed: 车间移动 H5 权限容器与按钮菜单
DELETE FROM system_menu WHERE id IN (10530, 10531, 10532, 10533);
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon,
    component, component_name, status, visible, keep_alive, always_show,
    creator, create_time, updater, update_time, deleted
) VALUES
(10530, '车间 H5', '', 2, 90, 10200, '', '#', NULL, NULL, 0, '0', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
(10531, '移动报工', 'lunaerp:workshop:h5:report', 3, 1, 10530, '', '#', NULL, NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
(10532, '移动质检', 'lunaerp:workshop:h5:inspection', 3, 2, 10530, '', '#', NULL, NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0),
(10533, '移动发纱', 'lunaerp:workshop:h5:dispatch', 3, 3, 10530, '', '#', NULL, NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0);

-- [2026-10-21] 05-vatno-validation-relax-and-menu-role-seed: 车间工人与检验员角色种子及权限关联（DO 块幂等）
CREATE SEQUENCE IF NOT EXISTS system_role_seq;
CREATE SEQUENCE IF NOT EXISTS system_role_menu_seq;

DO $$
DECLARE
    v_worker_role_id int8;
    v_inspector_role_id int8;
BEGIN
    PERFORM setval('system_role_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role), 1));
    PERFORM setval('system_role_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role_menu), 1));

    -- 1. workshop_worker (车间工人)
    IF EXISTS (SELECT 1 FROM system_role WHERE code = 'workshop_worker' AND tenant_id = 1) THEN
        UPDATE system_role
        SET name = '车间工人',
            status = 0,
            deleted = 0,
            updater = 'admin',
            update_time = CURRENT_TIMESTAMP
        WHERE code = 'workshop_worker' AND tenant_id = 1;
        SELECT id INTO v_worker_role_id FROM system_role WHERE code = 'workshop_worker' AND tenant_id = 1;
    ELSE
        INSERT INTO system_role (
            id, name, code, sort, data_scope, data_scope_dept_ids,
            status, type, remark, creator, create_time, updater, update_time, deleted, tenant_id
        ) VALUES (
            nextval('system_role_seq'), '车间工人', 'workshop_worker', 20, 1, '',
            0, 2, '车间移动H5工人角色（移动报工、移动发纱）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
        ) RETURNING id INTO v_worker_role_id;
    END IF;

    -- 2. workshop_inspector (车间检验员)
    IF EXISTS (SELECT 1 FROM system_role WHERE code = 'workshop_inspector' AND tenant_id = 1) THEN
        UPDATE system_role
        SET name = '车间检验员',
            status = 0,
            deleted = 0,
            updater = 'admin',
            update_time = CURRENT_TIMESTAMP
        WHERE code = 'workshop_inspector' AND tenant_id = 1;
        SELECT id INTO v_inspector_role_id FROM system_role WHERE code = 'workshop_inspector' AND tenant_id = 1;
    ELSE
        INSERT INTO system_role (
            id, name, code, sort, data_scope, data_scope_dept_ids,
            status, type, remark, creator, create_time, updater, update_time, deleted, tenant_id
        ) VALUES (
            nextval('system_role_seq'), '车间检验员', 'workshop_inspector', 21, 1, '',
            0, 2, '车间移动H5检验员角色（移动质检）', 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
        ) RETURNING id INTO v_inspector_role_id;
    END IF;

    -- 3. 授权 workshop_worker (10531, 10533)
    DELETE FROM system_role_menu WHERE role_id = v_worker_role_id AND menu_id IN (10530, 10531, 10532, 10533);
    INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
    VALUES
        (nextval('system_role_menu_seq'), v_worker_role_id, 10531, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1),
        (nextval('system_role_menu_seq'), v_worker_role_id, 10533, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1);

    -- 4. 授权 workshop_inspector (10532)
    DELETE FROM system_role_menu WHERE role_id = v_inspector_role_id AND menu_id IN (10530, 10531, 10532, 10533);
    INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
    VALUES
        (nextval('system_role_menu_seq'), v_inspector_role_id, 10532, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1);

    -- 5. super_admin / admin 同步赋全部 H5 权限（10530, 10531, 10532, 10533）
    DELETE FROM system_role_menu WHERE menu_id IN (10530, 10531, 10532, 10533)
      AND role_id IN (SELECT id FROM system_role WHERE code IN ('super_admin', 'admin') OR id IN (1, 2));

    INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
    SELECT nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
    FROM system_role r
    CROSS JOIN (SELECT id FROM system_menu WHERE id IN (10530, 10531, 10532, 10533)) m
    WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2);

END $$;

COMMIT;
