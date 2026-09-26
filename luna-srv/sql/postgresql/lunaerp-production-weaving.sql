-- =============================================================================
-- 模块：LunaERP - 生产管理 - 织造执行（织机档案、织造单、机台任务、布票）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 织机档案表 lunaerp_production_loom（Issue 02）
--    机台号 loom_no 服务端按年取号 LM+年份+4位流水（如 LM20260001）或手填；
--    租户内全量唯一含软删行（仿 ADR-0005，普通唯一索引无 WHERE deleted=0）；
--    状态 ACTIVE/INACTIVE，不建字典，缺省 ACTIVE；
--    删除守卫：被机台任务引用时禁止删除并引导停用（1-030-015-202），不提供物理删除。
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_loom_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_weaving_order_seq;

CREATE TABLE IF NOT EXISTS lunaerp_production_loom (
    id            int8          NOT NULL,
    loom_no       varchar(32)   NOT NULL,
    model         varchar(64)   NULL,
    status        varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    remark        varchar(500)  NULL,
    creator       varchar(64)   NULL DEFAULT '',
    create_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       varchar(64)   NULL DEFAULT '',
    update_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       int2          NOT NULL DEFAULT 0,
    tenant_id     int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_production_loom') THEN
        ALTER TABLE lunaerp_production_loom ADD CONSTRAINT pk_lb_production_loom PRIMARY KEY (id);
    END IF;
END $$;

-- 机台号租户内全量唯一（仿 ADR-0005，普通唯一索引、不带 WHERE deleted=0，软删不释放编号）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_production_loom_tenant_no
    ON lunaerp_production_loom (tenant_id, loom_no);
CREATE INDEX IF NOT EXISTS idx_lb_production_loom_status
    ON lunaerp_production_loom (status);

COMMENT ON TABLE lunaerp_production_loom IS '生产管理-织机档案表';
COMMENT ON COLUMN lunaerp_production_loom.id IS '织机ID';
COMMENT ON COLUMN lunaerp_production_loom.loom_no IS '机台号（LM+年份+4位流水或手填，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_production_loom.model IS '型号（例如 大圆机 34″·28G）';
COMMENT ON COLUMN lunaerp_production_loom.status IS '状态（ACTIVE 启用 / INACTIVE 停用），缺省 ACTIVE';
COMMENT ON COLUMN lunaerp_production_loom.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_loom.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_loom.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_loom.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_loom.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_loom.deleted IS '是否删除（0-未删，1-已删）';
COMMENT ON COLUMN lunaerp_production_loom.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 织造单主表 lunaerp_production_weaving_order（Issue 03）
--    从自织胚布单创建（productionMode=SELF），承载车间一次织造执行；
--    派生单号 ZZ-{胚布单号去符号}-{2位序号}，租户内全量唯一含软删行（ADR-0023）；
--    状态：DRAFT/ACTIVE/COMPLETED/CANCELLED。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_weaving_order (
    id                int8          NOT NULL,
    code              varchar(64)   NOT NULL,
    greige_order_id   int8          NOT NULL,
    greige_order_no   varchar(32)   NULL,
    contract_no       varchar(32)   NULL,
    product_id        int8          NOT NULL,
    product_code      varchar(64)   NULL,
    product_name      varchar(128)  NULL,
    composition       varchar(255)  NULL,
    gsm               varchar(64)   NULL,
    width             varchar(64)   NULL,
    planned_qty       int4          NOT NULL,
    planned_weight    numeric(18,2) NULL,
    status            varchar(16)   NOT NULL DEFAULT 'DRAFT',
    remark            varchar(500)  NULL,
    creator           varchar(64)   NULL DEFAULT '',
    create_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater           varchar(64)   NULL DEFAULT '',
    update_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           int2          NOT NULL DEFAULT 0,
    tenant_id         int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_production_weaving_order') THEN
        ALTER TABLE lunaerp_production_weaving_order ADD CONSTRAINT pk_lb_production_weaving_order PRIMARY KEY (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_weaving_order_tenant_code
    ON lunaerp_production_weaving_order (tenant_id, code);
CREATE INDEX IF NOT EXISTS idx_lb_weaving_order_greige_order_id
    ON lunaerp_production_weaving_order (greige_order_id);
CREATE INDEX IF NOT EXISTS idx_lb_weaving_order_status
    ON lunaerp_production_weaving_order (status);

COMMENT ON TABLE lunaerp_production_weaving_order IS '生产管理-织造单主表';
COMMENT ON COLUMN lunaerp_production_weaving_order.id IS '织造单ID';
COMMENT ON COLUMN lunaerp_production_weaving_order.code IS '织造单号（派生式：ZZ-{胚布单号去符号}-{2位序号}）';
COMMENT ON COLUMN lunaerp_production_weaving_order.greige_order_id IS '关联自织胚布单ID';
COMMENT ON COLUMN lunaerp_production_weaving_order.greige_order_no IS '胚布单号快照';
COMMENT ON COLUMN lunaerp_production_weaving_order.contract_no IS '合同号快照';
COMMENT ON COLUMN lunaerp_production_weaving_order.product_id IS '产品ID';
COMMENT ON COLUMN lunaerp_production_weaving_order.product_code IS '产品编码快照';
COMMENT ON COLUMN lunaerp_production_weaving_order.product_name IS '产品名称快照';
COMMENT ON COLUMN lunaerp_production_weaving_order.composition IS '成分快照';
COMMENT ON COLUMN lunaerp_production_weaving_order.gsm IS '克重快照';
COMMENT ON COLUMN lunaerp_production_weaving_order.width IS '门幅快照';
COMMENT ON COLUMN lunaerp_production_weaving_order.planned_qty IS '计划匹数（正整数）';
COMMENT ON COLUMN lunaerp_production_weaving_order.planned_weight IS '计划总重量 KG';
COMMENT ON COLUMN lunaerp_production_weaving_order.status IS '状态（DRAFT/ACTIVE/COMPLETED/CANCELLED）';
COMMENT ON COLUMN lunaerp_production_weaving_order.remark IS '备注';

-- =============================================================================
-- 3. 字典类型与数据（id=2238 lunaerp_weaving_order_status）
-- =============================================================================
DELETE FROM system_dict_type WHERE id = 2238;
DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_weaving_order_status';

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2238, '织造单状态', 'lunaerp_weaving_order_status', 0, 'LunaERP 生产管理-织造单状态', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES
(3630, 1, '待排产', 'DRAFT', 'lunaerp_weaving_order_status', 0, 'info', '', '新建初始状态，可改可删', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3631, 2, '生产中', 'ACTIVE', 'lunaerp_weaving_order_status', 0, 'primary', '', '布票首开工推导进入', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3632, 3, '已完工', 'COMPLETED', 'lunaerp_weaving_order_status', 0, 'success', '', '全部有效布票完工推导进入', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3633, 4, '已取消', 'CANCELLED', 'lunaerp_weaving_order_status', 0, 'danger', '', '手动终态', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

-- =============================================================================
-- 4. 菜单与权限注册
-- 10200 生产管理目录下：
--   10280 织造单菜单（query 权限）
--   10281 织造单新增按钮（create 权限）
--   10282 织造单修改按钮（update 权限）
--   10283 织造单删除按钮（delete 权限）
--   10284 织造单取消按钮（cancel 权限）
--   10285 织机档案菜单（query 权限）
--   10286 织机新增按钮（create 权限）
--   10287 织机修改按钮（update 权限）
--   10288 织机删除按钮（delete 权限）
-- =============================================================================
DELETE FROM system_menu WHERE id IN (10280, 10281, 10282, 10283, 10284, 10285, 10286, 10287, 10288);

-- 织造单菜单与按钮（sort 8）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10280, '织造单', 'lunaerp:production:weaving-order:query', 2, 8, 10200, 'weaving-order', 'ep:tickets', 'lunaerp-elmui/production/weaving-order/index', 'LunaerpElmuiProductionWeavingOrder', 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10281, '织造单新增', 'lunaerp:production:weaving-order:create', 3, 1, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10282, '织造单修改', 'lunaerp:production:weaving-order:update', 3, 2, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10283, '织造单删除', 'lunaerp:production:weaving-order:delete', 3, 3, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10284, '织造单取消', 'lunaerp:production:weaving-order:cancel', 3, 4, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

-- 织机档案菜单与按钮（sort 9）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10285, '织机档案', 'lunaerp:production:loom:query', 2, 9, 10200, 'loom', 'ep:cpu', 'lunaerp-elmui/production/loom/index', 'LunaerpElmuiProductionLoom', 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10286, '织机新增', 'lunaerp:production:loom:create', 3, 1, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10287, '织机修改', 'lunaerp:production:loom:update', 3, 2, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10288, '织机删除', 'lunaerp:production:loom:delete', 3, 3, 10285, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

-- 超级管理员（角色 1/2）自动赋予菜单权限
DELETE FROM system_role_menu WHERE menu_id IN (10280, 10281, 10282, 10283, 10284, 10285, 10286, 10287, 10288);
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (SELECT id FROM system_menu WHERE id IN (10280, 10281, 10282, 10283, 10284, 10285, 10286, 10287, 10288)) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2)
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 5. 机台任务表 lunaerp_production_machine_task（Issue 04）
--    织造单排产到某台织机的派工任务——"哪台机织多少匹"的载体；
--    派生编号：MT-{织造单号去符号}-{单内 2 位序号}，租户内全量唯一含软删行（ADR-0023）；
--    状态：PENDING/IN_PROGRESS/HALTED/COMPLETED；
--    单内任务计划匹数合计超出织造单计划匹数时警告不拦截。
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_machine_task_seq;

CREATE TABLE IF NOT EXISTS lunaerp_production_machine_task (
    id                int8          NOT NULL,
    code              varchar(64)   NOT NULL,
    weaving_order_id  int8          NOT NULL,
    loom_id           int8          NOT NULL,
    loom_no           varchar(32)   NOT NULL,
    worker_user_id    int8          NULL,
    worker_name       varchar(64)   NULL,
    planned_qty       int4          NOT NULL,
    actual_qty        int4          NOT NULL DEFAULT 0,
    planned_start     date          NULL,
    planned_end       date          NULL,
    status            varchar(16)   NOT NULL DEFAULT 'PENDING',
    halt_reason       varchar(255)  NULL,
    halt_at           timestamp     NULL,
    remark            varchar(500)  NULL,
    creator           varchar(64)   NULL DEFAULT '',
    create_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater           varchar(64)   NULL DEFAULT '',
    update_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           int2          NOT NULL DEFAULT 0,
    tenant_id         int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_production_machine_task') THEN
        ALTER TABLE lunaerp_production_machine_task ADD CONSTRAINT pk_lb_production_machine_task PRIMARY KEY (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_machine_task_tenant_code
    ON lunaerp_production_machine_task (tenant_id, code);
CREATE INDEX IF NOT EXISTS idx_lb_machine_task_weaving_order_id
    ON lunaerp_production_machine_task (weaving_order_id);
CREATE INDEX IF NOT EXISTS idx_lb_machine_task_loom_id
    ON lunaerp_production_machine_task (loom_id);
CREATE INDEX IF NOT EXISTS idx_lb_machine_task_status
    ON lunaerp_production_machine_task (status);

COMMENT ON TABLE lunaerp_production_machine_task IS '生产管理-机台任务表';
COMMENT ON COLUMN lunaerp_production_machine_task.id IS '机台任务ID';
COMMENT ON COLUMN lunaerp_production_machine_task.code IS '任务编号（派生式：MT-{织造单号去符号}-{单内 2 位序号}）';
COMMENT ON COLUMN lunaerp_production_machine_task.weaving_order_id IS '关联织造单ID';
COMMENT ON COLUMN lunaerp_production_machine_task.loom_id IS '关联织机ID（仅 ACTIVE 织机可选）';
COMMENT ON COLUMN lunaerp_production_machine_task.loom_no IS '机台号快照';
COMMENT ON COLUMN lunaerp_production_machine_task.worker_user_id IS '挡车工用户ID（关联 system User，可空，ADR-0021）';
COMMENT ON COLUMN lunaerp_production_machine_task.worker_name IS '挡车工姓名快照';
COMMENT ON COLUMN lunaerp_production_machine_task.planned_qty IS '计划匹数（正整数）';
COMMENT ON COLUMN lunaerp_production_machine_task.actual_qty IS '完工/实织匹数（布票聚合只读，缺省 0）';
COMMENT ON COLUMN lunaerp_production_machine_task.planned_start IS '计划开始日期（普通日期可空）';
COMMENT ON COLUMN lunaerp_production_machine_task.planned_end IS '计划结束日期（普通日期可空，end ≥ start）';
COMMENT ON COLUMN lunaerp_production_machine_task.status IS '任务状态（PENDING/IN_PROGRESS/HALTED/COMPLETED，缺省 PENDING）';
COMMENT ON COLUMN lunaerp_production_machine_task.halt_reason IS '停机原因';
COMMENT ON COLUMN lunaerp_production_machine_task.halt_at IS '停机时间';
COMMENT ON COLUMN lunaerp_production_machine_task.remark IS '备注';

-- =============================================================================
-- 6. 字典类型与数据（id=2239 lunaerp_weaving_task_status）
-- =============================================================================
DELETE FROM system_dict_type WHERE id = 2239;
DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_weaving_task_status';

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2239, '机台任务状态', 'lunaerp_weaving_task_status', 0, 'LunaERP 生产管理-机台任务状态', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES
(3640, 1, '待开始', 'PENDING', 'lunaerp_weaving_task_status', 0, 'info', '', '初始未开工状态', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3641, 2, '生产中', 'IN_PROGRESS', 'lunaerp_weaving_task_status', 0, 'primary', '', '布票开工推导进入', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3642, 3, '暂停异常', 'HALTED', 'lunaerp_weaving_task_status', 0, 'danger', '', '人工停机终态，须手动恢复解除', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3643, 4, '已完工', 'COMPLETED', 'lunaerp_weaving_task_status', 0, 'success', '', '全部有效布票完工推导进入', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

