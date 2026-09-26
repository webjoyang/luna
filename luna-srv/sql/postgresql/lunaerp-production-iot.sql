-- =============================================================================
-- 模块：LunaERP - 生产管理 - 织造执行 - IoT 接入（厂商凭证、设备绑定、实时快照）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 厂商凭证表 lunaerp_production_vendor_credential（ADR-0028）
--    每客户/采购批次一组厂商 API 凭证；
--    vendor_account 租户内唯一；
--    vendor_password_enc 加密存储，禁止明文回显；
--    scope 设备范围说明；status 启用/停用；last_sync_at 最后同步时间。
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_vendor_credential_seq;

CREATE TABLE IF NOT EXISTS lunaerp_production_vendor_credential (
    id                   int8          NOT NULL,
    vendor_account       varchar(64)   NOT NULL,
    vendor_password_enc  varchar(255)  NOT NULL,
    scope                varchar(255)  NULL,
    status               varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    last_sync_at         timestamp     NULL,
    creator              varchar(64)   NULL DEFAULT '',
    create_time          timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater              varchar(64)   NULL DEFAULT '',
    update_time          timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              int2          NOT NULL DEFAULT 0,
    tenant_id            int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_production_vendor_credential') THEN
        ALTER TABLE lunaerp_production_vendor_credential ADD CONSTRAINT pk_lb_production_vendor_credential PRIMARY KEY (id);
    END IF;
END $$;

-- 厂商账号租户内全量唯一（仿 ADR-0004/0005，普通唯一索引、不带 WHERE deleted=0，软删不释放账号）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_iot_vendor_account
    ON lunaerp_production_vendor_credential (tenant_id, vendor_account);
CREATE INDEX IF NOT EXISTS idx_lb_iot_vendor_credential_status
    ON lunaerp_production_vendor_credential (status);

COMMENT ON TABLE lunaerp_production_vendor_credential IS '生产管理-IoT厂商凭证表';
COMMENT ON COLUMN lunaerp_production_vendor_credential.id IS '凭证ID';
COMMENT ON COLUMN lunaerp_production_vendor_credential.vendor_account IS '厂商账号（租户内唯一）';
COMMENT ON COLUMN lunaerp_production_vendor_credential.vendor_password_enc IS '厂商密码密文（AES加密存储，禁止回显明文）';
COMMENT ON COLUMN lunaerp_production_vendor_credential.scope IS '设备范围说明';
COMMENT ON COLUMN lunaerp_production_vendor_credential.status IS '状态（ACTIVE 启用 / INACTIVE 停用），缺省 ACTIVE';
COMMENT ON COLUMN lunaerp_production_vendor_credential.last_sync_at IS '最后同步时间';
COMMENT ON COLUMN lunaerp_production_vendor_credential.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_vendor_credential.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_vendor_credential.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_vendor_credential.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_vendor_credential.deleted IS '是否删除（0-未删，1-已删）';
COMMENT ON COLUMN lunaerp_production_vendor_credential.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 设备绑定表 lunaerp_production_device_binding（ADR-0028）
--    厂商设备 -> 本系统织机映射，归属以绑定表为权威路由，防跨租户污染；
--    vendor_device_id 厂商侧设备唯一标识；
--    loom_id 绑定本系统织机ID；status 状态；last_sync_at 最后同步时间；
--    同设备重复绑定报 411。
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_device_binding_seq;

CREATE TABLE IF NOT EXISTS lunaerp_production_device_binding (
    id                int8          NOT NULL,
    vendor_device_id  varchar(64)   NOT NULL,
    loom_id           int8          NOT NULL,
    status            varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    last_sync_at      timestamp     NULL,
    creator           varchar(64)   NULL DEFAULT '',
    create_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater           varchar(64)   NULL DEFAULT '',
    update_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           int2          NOT NULL DEFAULT 0,
    tenant_id         int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_production_device_binding') THEN
        ALTER TABLE lunaerp_production_device_binding ADD CONSTRAINT pk_lb_production_device_binding PRIMARY KEY (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_iot_device_binding
    ON lunaerp_production_device_binding (tenant_id, vendor_device_id) WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_iot_device_loom
    ON lunaerp_production_device_binding (tenant_id, loom_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lb_iot_device_binding_status
    ON lunaerp_production_device_binding (status);

COMMENT ON TABLE lunaerp_production_device_binding IS '生产管理-IoT设备绑定表';
COMMENT ON COLUMN lunaerp_production_device_binding.id IS '绑定ID';
COMMENT ON COLUMN lunaerp_production_device_binding.vendor_device_id IS '厂商侧设备唯一标识';
COMMENT ON COLUMN lunaerp_production_device_binding.loom_id IS '本系统织机ID';
COMMENT ON COLUMN lunaerp_production_device_binding.status IS '状态（ACTIVE 启用 / INACTIVE 停用），缺省 ACTIVE';
COMMENT ON COLUMN lunaerp_production_device_binding.last_sync_at IS '最后同步时间';
COMMENT ON COLUMN lunaerp_production_device_binding.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_device_binding.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_device_binding.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_device_binding.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_device_binding.deleted IS '是否删除（0-未删，1-已删）';
COMMENT ON COLUMN lunaerp_production_device_binding.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 实时快照表 lunaerp_production_machine_realtime（ADR-0028）
--    (tenant_id, vendor_device_id) 唯一，只保留最新值（更新覆盖）；
--    不回写 loom.status（信息源不污染）；
--    字段含 status (running/idle/abnormal/disabled/offline)、
--    run_efficiency_pct (运转效率)、minute_circle_speed (圈速)、shift_circles (班次圈数)、
--    limit_speed (限制圈速)、run_minutes (运转分钟)、shift_minutes (在班分钟)、
--    staff_on_shift (在班人员)、device_sn (设备出厂编号)、fetched_at (采集时间)。
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_machine_realtime_seq;

CREATE TABLE IF NOT EXISTS lunaerp_production_machine_realtime (
    id                   int8          NOT NULL,
    vendor_device_id     varchar(64)   NOT NULL,
    device_sn            varchar(64)   NULL,
    status               varchar(16)   NOT NULL DEFAULT 'offline',
    run_efficiency_pct   numeric(5,2)  NULL,
    minute_circle_speed  int4          NULL,
    shift_circles        int4          NULL,
    limit_speed          int4          NULL,
    run_minutes          int4          NULL,
    shift_minutes        int4          NULL,
    staff_on_shift       varchar(64)   NULL,
    fetched_at           timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator              varchar(64)   NULL DEFAULT '',
    create_time          timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater              varchar(64)   NULL DEFAULT '',
    update_time          timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              int2          NOT NULL DEFAULT 0,
    tenant_id            int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_production_machine_realtime') THEN
        ALTER TABLE lunaerp_production_machine_realtime ADD CONSTRAINT pk_lb_production_machine_realtime PRIMARY KEY (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_iot_machine_realtime
    ON lunaerp_production_machine_realtime (tenant_id, vendor_device_id);
CREATE INDEX IF NOT EXISTS idx_lb_iot_machine_realtime_status
    ON lunaerp_production_machine_realtime (status);

COMMENT ON TABLE lunaerp_production_machine_realtime IS '生产管理-IoT设备实时快照表';
COMMENT ON COLUMN lunaerp_production_machine_realtime.id IS '快照ID';
COMMENT ON COLUMN lunaerp_production_machine_realtime.vendor_device_id IS '厂商侧设备唯一标识';
COMMENT ON COLUMN lunaerp_production_machine_realtime.device_sn IS '设备编号/出厂编号';
COMMENT ON COLUMN lunaerp_production_machine_realtime.status IS '实时状态（running/idle/abnormal/disabled/offline）';
COMMENT ON COLUMN lunaerp_production_machine_realtime.run_efficiency_pct IS '运转效率百分比（%）';
COMMENT ON COLUMN lunaerp_production_machine_realtime.minute_circle_speed IS '分钟圈速（RPM）';
COMMENT ON COLUMN lunaerp_production_machine_realtime.shift_circles IS '班次圈数';
COMMENT ON COLUMN lunaerp_production_machine_realtime.limit_speed IS '限制圈速';
COMMENT ON COLUMN lunaerp_production_machine_realtime.run_minutes IS '运转分钟';
COMMENT ON COLUMN lunaerp_production_machine_realtime.shift_minutes IS '在班分钟';
COMMENT ON COLUMN lunaerp_production_machine_realtime.staff_on_shift IS '在班人员';
COMMENT ON COLUMN lunaerp_production_machine_realtime.fetched_at IS '采集/录入时间';
COMMENT ON COLUMN lunaerp_production_machine_realtime.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_machine_realtime.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_machine_realtime.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_machine_realtime.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_machine_realtime.deleted IS '是否删除（0-未删，1-已删）';
COMMENT ON COLUMN lunaerp_production_machine_realtime.tenant_id IS '租户编号';

-- =============================================================================
-- 4. 权限与菜单注册（挂在 10285 织机档案菜单下，不开独立一级菜单）
-- =============================================================================
DELETE FROM system_menu WHERE id IN (10350, 10351, 10352, 10353, 10354, 10355, 10356, 10357, 10358, 10359, 10360);

-- 设备接入凭证权限
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10350, '设备接入凭证查询', 'lunaerp:production:iot:vendor-credential:query', 3, 10, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10351, '设备接入凭证新增', 'lunaerp:production:iot:vendor-credential:create', 3, 11, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10352, '设备接入凭证修改', 'lunaerp:production:iot:vendor-credential:update', 3, 12, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10353, '设备接入凭证删除', 'lunaerp:production:iot:vendor-credential:delete', 3, 13, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

-- 设备绑定权限
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10354, '设备绑定查询', 'lunaerp:production:iot:device-binding:query', 3, 14, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10355, '设备绑定新增', 'lunaerp:production:iot:device-binding:create', 3, 15, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10356, '设备绑定修改', 'lunaerp:production:iot:device-binding:update', 3, 16, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10357, '设备绑定删除', 'lunaerp:production:iot:device-binding:delete', 3, 17, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

-- 实时快照权限
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10358, '实时快照查询', 'lunaerp:production:iot:machine-realtime:query', 3, 18, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10359, '实时快照录入', 'lunaerp:production:iot:machine-realtime:record', 3, 19, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10360, '实时快照模拟采集', 'lunaerp:production:iot:machine-realtime:collect', 3, 20, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

-- 角色权限关联
DELETE FROM system_role_menu WHERE menu_id IN (10350, 10351, 10352, 10353, 10354, 10355, 10356, 10357, 10358, 10359, 10360);
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (SELECT id FROM system_menu WHERE id IN (10350, 10351, 10352, 10353, 10354, 10355, 10356, 10357, 10358, 10359, 10360)) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2)
ON CONFLICT DO NOTHING;

