-- =============================================================================
-- 模块：LunaERP - 生产管理 - 纱线档案（材质主表与规格子表）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/seed 数据先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 纱线材质主表 lunaerp_production_yarn_type
--    材质类型不入字典（ADR-0011），直接 seed 7 项主数据。
--    唯一索引 (tenant_id, name) 含软删行，软删不释放材质名。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_yarn_type (
    id          int8         NOT NULL,
    name        varchar(64)  NOT NULL,
    sort        int4         NOT NULL DEFAULT 0,
    status      varchar(16)  NOT NULL DEFAULT 'ACTIVE',
    creator     varchar(64)  NULL DEFAULT '',
    create_time timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     varchar(64)  NULL DEFAULT '',
    update_time timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     int2         NOT NULL DEFAULT 0,
    tenant_id   int8         NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_yarn_type') THEN
        ALTER TABLE lunaerp_production_yarn_type ADD CONSTRAINT pk_lb_yarn_type PRIMARY KEY (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_yarn_type_name
    ON lunaerp_production_yarn_type (tenant_id, name);
CREATE INDEX IF NOT EXISTS idx_lb_yarn_type_status
    ON lunaerp_production_yarn_type (status);

COMMENT ON TABLE lunaerp_production_yarn_type IS '生产管理-纱线材质主表';
COMMENT ON COLUMN lunaerp_production_yarn_type.id IS '材质ID';
COMMENT ON COLUMN lunaerp_production_yarn_type.name IS '材质名称（棉/涤/人棉/氨纶/锦纶/粘胶/混纺，租户内唯一）';
COMMENT ON COLUMN lunaerp_production_yarn_type.sort IS '排序值';
COMMENT ON COLUMN lunaerp_production_yarn_type.status IS '状态（ACTIVE 启用 / INACTIVE 停用），创建缺省 ACTIVE';
COMMENT ON COLUMN lunaerp_production_yarn_type.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_yarn_type.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_yarn_type.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_yarn_type.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_yarn_type.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_production_yarn_type.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 纱线规格子表 lunaerp_production_yarn_spec
--    挂在材质之下，采购明细行直接关联的实体。
--    唯一索引 (tenant_id, yarn_type_id, standard_name)。
--    ref_price 是测算时历史价回填的兜底来源（ADR-0011）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_yarn_spec (
    id            int8          NOT NULL,
    yarn_type_id  int8          NOT NULL,
    spec_name     varchar(128)  NOT NULL,
    standard_name varchar(128)  NOT NULL,
    spin_method   varchar(64)   NULL,
    luster        varchar(64)   NULL,
    ref_price     numeric(18,4) NULL,
    remark        varchar(1000) NULL,
    status        varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    creator       varchar(64)   NULL DEFAULT '',
    create_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       varchar(64)   NULL DEFAULT '',
    update_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       int2          NOT NULL DEFAULT 0,
    tenant_id     int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_yarn_spec') THEN
        ALTER TABLE lunaerp_production_yarn_spec ADD CONSTRAINT pk_lb_yarn_spec PRIMARY KEY (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_yarn_spec_standard_name
    ON lunaerp_production_yarn_spec (tenant_id, yarn_type_id, standard_name);
CREATE INDEX IF NOT EXISTS idx_lb_yarn_spec_yarn_type_id
    ON lunaerp_production_yarn_spec (yarn_type_id);
CREATE INDEX IF NOT EXISTS idx_lb_yarn_spec_status
    ON lunaerp_production_yarn_spec (status);

COMMENT ON TABLE lunaerp_production_yarn_spec IS '生产管理-纱线规格子表';
COMMENT ON COLUMN lunaerp_production_yarn_spec.id IS '规格ID';
COMMENT ON COLUMN lunaerp_production_yarn_spec.yarn_type_id IS '所属材质ID（lunaerp_production_yarn_type.id）';
COMMENT ON COLUMN lunaerp_production_yarn_spec.spec_name IS '规格名称（展示用，如"32S 棉"）';
COMMENT ON COLUMN lunaerp_production_yarn_spec.standard_name IS '标准名称（租户内+材质内唯一）';
COMMENT ON COLUMN lunaerp_production_yarn_spec.spin_method IS '纺纱工艺（环锭纺/转杯纺/喷气纺等）';
COMMENT ON COLUMN lunaerp_production_yarn_spec.luster IS '光泽（半光/全光/消光等）';
COMMENT ON COLUMN lunaerp_production_yarn_spec.ref_price IS '参考单价（测算历史价回填兜底）';
COMMENT ON COLUMN lunaerp_production_yarn_spec.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_yarn_spec.status IS '状态（ACTIVE 启用 / INACTIVE 停用），创建缺省 ACTIVE';
COMMENT ON COLUMN lunaerp_production_yarn_spec.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_yarn_spec.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_yarn_spec.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_yarn_spec.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_yarn_spec.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_production_yarn_spec.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_yarn_type_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_yarn_spec_seq;

-- =============================================================================
-- 4. 材质 seed 7 项（棉/涤/人棉/氨纶/锦纶/粘胶/混纺，可重复执行）
--    先 DELETE WHERE id IN (1..7) 再 INSERT 保证可重复执行。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM lunaerp_production_yarn_type WHERE id IN (1, 2, 3, 4, 5, 6, 7);

INSERT INTO lunaerp_production_yarn_type (id, name, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES (1, '棉', 1, 'ACTIVE', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', 0, 0);
INSERT INTO lunaerp_production_yarn_type (id, name, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES (2, '涤', 2, 'ACTIVE', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', 0, 0);
INSERT INTO lunaerp_production_yarn_type (id, name, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES (3, '人棉', 3, 'ACTIVE', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', 0, 0);
INSERT INTO lunaerp_production_yarn_type (id, name, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES (4, '氨纶', 4, 'ACTIVE', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', 0, 0);
INSERT INTO lunaerp_production_yarn_type (id, name, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES (5, '锦纶', 5, 'ACTIVE', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', 0, 0);
INSERT INTO lunaerp_production_yarn_type (id, name, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES (6, '粘胶', 6, 'ACTIVE', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', 0, 0);
INSERT INTO lunaerp_production_yarn_type (id, name, sort, status, creator, create_time, updater, update_time, deleted, tenant_id) VALUES (7, '混纺', 7, 'ACTIVE', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', 0, 0);

COMMIT;
-- @formatter:on

-- =============================================================================
-- 5. 菜单与按钮（system_menu，可重复执行）
--    10200 生产管理目录下挂载 10240 纱线档案菜单；10241-10244 查询/创建/修改/删除按钮。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10240, 10241, 10242, 10243, 10244);

-- 菜单：纱线档案（query 权限挂在菜单本身，参照胚布单/染色单模式）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10240, '纱线档案', 'lunaerp:production:yarn-type:query', 2, 4, 10200, 'yarn-master', 'ep:files', 'lunaerp-elmui/production/yarn-master/index', 'LunaerpElmuiProductionYarnMaster', 0, '1', '1', '1', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');
-- 按钮：纱线档案查询/创建/修改/删除（规格操作复用同菜单权限码）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10241, '纱线档案查询', 'lunaerp:production:yarn-type:query', 3, 1, 10240, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10242, '纱线档案创建', 'lunaerp:production:yarn-type:create', 3, 2, 10240, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10243, '纱线档案修改', 'lunaerp:production:yarn-type:update', 3, 3, 10240, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10244, '纱线档案删除', 'lunaerp:production:yarn-type:delete', 3, 4, 10240, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');

COMMIT;
-- @formatter:on
