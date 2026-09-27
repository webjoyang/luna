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

-- =============================================================================
-- 7. 织造布票表 lunaerp_production_weaving_ticket（Issue 05）
--    织造执行最底层的每匹实体——"每匹一码"，票号即二维码内容；
--    派生编号：BT-{机台任务号去符号}-{4 位流水}，单内流水计数，全量唯一含软删行（ADR-0023、ADR-0024）；
--    状态：GENERATED/STARTED/COMPLETED/INSPECTED/STOCKED/EXCEPTION/VOID，缺省 GENERATED。
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_weaving_ticket_seq;

CREATE TABLE IF NOT EXISTS lunaerp_production_weaving_ticket (
    id                int8          NOT NULL,
    code              varchar(64)   NOT NULL,
    weaving_order_id  int8          NOT NULL,
    machine_task_id   int8          NOT NULL,
    planned_machine   varchar(32)   NULL,
    actual_machine    varchar(32)   NULL,
    planned_weight    numeric(18,2) NULL,
    worker_user_id    int8          NULL,
    worker_name       varchar(64)   NULL,
    material_batch_no varchar(64)   NULL,
    status            varchar(16)   NOT NULL DEFAULT 'GENERATED',
    exception_type    varchar(32)   NULL,
    exception_note    varchar(500)  NULL,
    started_at        timestamp     NULL,
    completed_at      timestamp     NULL,
    void_reason       varchar(255)  NULL,
    voided_at         timestamp     NULL,
    creator           varchar(64)   NULL DEFAULT '',
    create_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater           varchar(64)   NULL DEFAULT '',
    update_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           int2          NOT NULL DEFAULT 0,
    tenant_id         int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_production_weaving_ticket') THEN
        ALTER TABLE lunaerp_production_weaving_ticket ADD CONSTRAINT pk_lb_production_weaving_ticket PRIMARY KEY (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_weaving_ticket_tenant_code
    ON lunaerp_production_weaving_ticket (tenant_id, code);
CREATE INDEX IF NOT EXISTS idx_lb_weaving_ticket_weaving_order_id
    ON lunaerp_production_weaving_ticket (weaving_order_id);
CREATE INDEX IF NOT EXISTS idx_lb_weaving_ticket_machine_task_id
    ON lunaerp_production_weaving_ticket (machine_task_id);
CREATE INDEX IF NOT EXISTS idx_lb_weaving_ticket_status
    ON lunaerp_production_weaving_ticket (status);

COMMENT ON TABLE lunaerp_production_weaving_ticket IS '生产管理-织造布票表';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.id IS '布票ID';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.code IS '布票编号（派生式：BT-{机台任务号去符号}-{4 位流水}，单内流水计数，全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.weaving_order_id IS '关联织造单ID';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.machine_task_id IS '关联机台任务ID';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.planned_machine IS '预排机台号快照';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.actual_machine IS '实际上机机台号（开工时录入/确认）';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.planned_weight IS '单匹计划重量 KG（生成时按 织造单总重÷计划匹数 计算带入）';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.worker_user_id IS '挡车工用户ID（关联 system User）';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.worker_name IS '挡车工姓名快照';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.material_batch_no IS '原料批号';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.status IS '状态（GENERATED/STARTED/COMPLETED/INSPECTED/STOCKED/EXCEPTION/VOID）';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.exception_type IS '异常类型（YARN_BREAK/CLOTH_DAMAGE/YARN_SHORTAGE/MACHINE_CHANGE/WORKER_CHANGE）';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.exception_note IS '异常说明';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.started_at IS '开工时间';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.completed_at IS '完工时间';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.void_reason IS '作废原因';
COMMENT ON COLUMN lunaerp_production_weaving_ticket.voided_at IS '作废时间';

-- =============================================================================
-- 8. 字典类型与数据（id=2240 lunaerp_weaving_ticket_status、id=2244 lunaerp_weaving_exception_type）
-- =============================================================================
DELETE FROM system_dict_type WHERE id = 2240;
DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_weaving_ticket_status';

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2240, '布票状态', 'lunaerp_weaving_ticket_status', 0, 'LunaERP 生产管理-织造布票状态', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES
(3650, 1, '已生成', 'GENERATED', 'lunaerp_weaving_ticket_status', 0, 'info', '', '初始生成状态', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3651, 2, '已开工', 'STARTED', 'lunaerp_weaving_ticket_status', 0, 'primary', '', '机台扫码开工', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3652, 3, '已完工', 'COMPLETED', 'lunaerp_weaving_ticket_status', 0, 'success', '', '机台扫码完工', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3653, 4, '已检验', 'INSPECTED', 'lunaerp_weaving_ticket_status', 0, 'warning', '', '检验单明细回写', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3654, 5, '已入库', 'STOCKED', 'lunaerp_weaving_ticket_status', 0, 'success', '', '胚布入库审核回写', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3655, 6, '异常', 'EXCEPTION', 'lunaerp_weaving_ticket_status', 0, 'danger', '', '报修断纱异常', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3656, 7, '作废', 'VOID', 'lunaerp_weaving_ticket_status', 0, 'danger', '', '人工终态作废', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

DELETE FROM system_dict_type WHERE id = 2244;
DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_weaving_exception_type';

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2244, '织造异常类型', 'lunaerp_weaving_exception_type', 0, 'LunaERP 生产管理-织造异常类型', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES
(3670, 1, '断纱', 'YARN_BREAK', 'lunaerp_weaving_exception_type', 0, 'danger', '', '织造断纱', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3671, 2, '坏布', 'CLOTH_DAMAGE', 'lunaerp_weaving_exception_type', 0, 'danger', '', '布面破损瑕疵', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3672, 3, '缺纱停机', 'YARN_SHORTAGE', 'lunaerp_weaving_exception_type', 0, 'warning', '', '车间缺纱', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3673, 4, '换机', 'MACHINE_CHANGE', 'lunaerp_weaving_exception_type', 0, 'info', '', '更换上机机台', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3674, 5, '换人', 'WORKER_CHANGE', 'lunaerp_weaving_exception_type', 0, 'info', '', '更换挡车工', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

-- =============================================================================
-- 9. 领纱单主表 lunaerp_production_yarn_requisition（Issue 09）
--    织造单领用纱线的申请单，挂织造单（一单一申请，可多次申请）；
--    派生编号：YS-{织造单号去符号}-{单内 2 位序号}，全量唯一含软删行（ADR-0027）；
--    状态：DRAFT 草稿 / ACTIVE 待发纱 / PARTIAL 部分发纱 / DISPATCHED 已发纱 / CLOSED 已关闭，缺省 DRAFT。
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_yarn_requisition_seq;

CREATE TABLE IF NOT EXISTS lunaerp_production_yarn_requisition (
    id                int8          NOT NULL,
    code              varchar(64)   NOT NULL,
    weaving_order_id  int8          NOT NULL,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_production_yarn_requisition') THEN
        ALTER TABLE lunaerp_production_yarn_requisition ADD CONSTRAINT pk_lb_production_yarn_requisition PRIMARY KEY (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_yarn_req_tenant_code
    ON lunaerp_production_yarn_requisition (tenant_id, code);
CREATE INDEX IF NOT EXISTS idx_lb_yarn_req_weaving_order_id
    ON lunaerp_production_yarn_requisition (weaving_order_id);
CREATE INDEX IF NOT EXISTS idx_lb_yarn_req_status
    ON lunaerp_production_yarn_requisition (status);

COMMENT ON TABLE lunaerp_production_yarn_requisition IS '生产管理-领纱单主表';
COMMENT ON COLUMN lunaerp_production_yarn_requisition.id IS '领纱单ID';
COMMENT ON COLUMN lunaerp_production_yarn_requisition.code IS '领纱单号（派生式：YS-{织造单号去符号}-{单内 2 位序号}）';
COMMENT ON COLUMN lunaerp_production_yarn_requisition.weaving_order_id IS '关联织造单ID';
COMMENT ON COLUMN lunaerp_production_yarn_requisition.status IS '状态（DRAFT 草稿 / ACTIVE 待发纱 / PARTIAL 部分发纱 / DISPATCHED 已发纱 / CLOSED 已关闭）';
COMMENT ON COLUMN lunaerp_production_yarn_requisition.remark IS '备注';

-- =============================================================================
-- 10. 领纱明细表 lunaerp_production_yarn_requisition_item（Issue 09）
--     独立子资源处理（仿 ADR-0014）：有独立端点与权限码，逐行维护、逐行发纱；
--     字段：yarn_type_id + yarn_spec_id + yarn_spec_name 快照、required_qty、dispatched_qty（缺省 0）、unit=KG、remark。
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_yarn_requisition_item_seq;

CREATE TABLE IF NOT EXISTS lunaerp_production_yarn_requisition_item (
    id                int8          NOT NULL,
    requisition_id    int8          NOT NULL,
    yarn_type_id      int8          NOT NULL,
    yarn_spec_id      int8          NOT NULL,
    yarn_spec_name    varchar(128)  NOT NULL,
    required_qty      numeric(18,2) NOT NULL,
    dispatched_qty    numeric(18,2) NOT NULL DEFAULT 0,
    unit              varchar(16)   NOT NULL DEFAULT 'KG',
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_production_yarn_requisition_item') THEN
        ALTER TABLE lunaerp_production_yarn_requisition_item ADD CONSTRAINT pk_lb_production_yarn_requisition_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_yarn_req_item_req_id
    ON lunaerp_production_yarn_requisition_item (requisition_id);
CREATE INDEX IF NOT EXISTS idx_lb_yarn_req_item_spec_id
    ON lunaerp_production_yarn_requisition_item (yarn_spec_id);

COMMENT ON TABLE lunaerp_production_yarn_requisition_item IS '生产管理-领纱明细表';
COMMENT ON COLUMN lunaerp_production_yarn_requisition_item.id IS '明细ID';
COMMENT ON COLUMN lunaerp_production_yarn_requisition_item.requisition_id IS '关联领纱单ID';
COMMENT ON COLUMN lunaerp_production_yarn_requisition_item.yarn_type_id IS '纱线材质ID';
COMMENT ON COLUMN lunaerp_production_yarn_requisition_item.yarn_spec_id IS '纱线规格ID';
COMMENT ON COLUMN lunaerp_production_yarn_requisition_item.yarn_spec_name IS '纱线规格名称快照';
COMMENT ON COLUMN lunaerp_production_yarn_requisition_item.required_qty IS '需求量（KG）';
COMMENT ON COLUMN lunaerp_production_yarn_requisition_item.dispatched_qty IS '已发量（KG，缺省 0）';
COMMENT ON COLUMN lunaerp_production_yarn_requisition_item.unit IS '单位，固定 KG';
COMMENT ON COLUMN lunaerp_production_yarn_requisition_item.remark IS '备注';

-- =============================================================================
-- 11. 字典类型与数据（id=2243 lunaerp_yarn_requisition_status）
-- =============================================================================
DELETE FROM system_dict_type WHERE id = 2243;
DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_yarn_requisition_status';

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2243, '领纱单状态', 'lunaerp_yarn_requisition_status', 0, 'LunaERP 生产管理-领纱单状态', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES
(3680, 1, '草稿', 'DRAFT', 'lunaerp_yarn_requisition_status', 0, 'info', '', '初始草稿状态，可编辑可测算', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3681, 2, '待发纱', 'ACTIVE', 'lunaerp_yarn_requisition_status', 0, 'primary', '', '提交后进入待发纱', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3682, 3, '部分发纱', 'PARTIAL', 'lunaerp_yarn_requisition_status', 0, 'warning', '', '部分行或部分数量已发纱', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3683, 4, '已发纱', 'DISPATCHED', 'lunaerp_yarn_requisition_status', 0, 'success', '', '全部行数量已发齐', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0'),
(3684, 5, '已关闭', 'CLOSED', 'lunaerp_yarn_requisition_status', 0, 'danger', '', '手动终态', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

-- =============================================================================
-- 12. 领纱单与明细权限注册（不开独立菜单，挂在 10280 织造单下）
-- =============================================================================
DELETE FROM system_menu WHERE id IN (10289, 10290, 10291, 10292, 10293, 10294, 10295, 10296, 10297, 10298);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10289, '领纱单查询', 'lunaerp:production:weaving-order-requisition:query', 3, 5, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10290, '领纱单新增', 'lunaerp:production:weaving-order-requisition:create', 3, 6, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10291, '领纱单修改', 'lunaerp:production:weaving-order-requisition:update', 3, 7, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10292, '领纱单提交', 'lunaerp:production:weaving-order-requisition:submit', 3, 8, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10293, '领纱单关闭', 'lunaerp:production:weaving-order-requisition:close', 3, 9, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10294, '领纱单删除', 'lunaerp:production:weaving-order-requisition:delete', 3, 10, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10295, '领纱明细查询', 'lunaerp:production:weaving-order-requisition-item:query', 3, 11, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10296, '领纱明细新增', 'lunaerp:production:weaving-order-requisition-item:create', 3, 12, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10297, '领纱明细修改', 'lunaerp:production:weaving-order-requisition-item:update', 3, 13, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10298, '领纱明细删除', 'lunaerp:production:weaving-order-requisition-item:delete', 3, 14, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10299, '领纱明细发纱与撤销', 'lunaerp:production:weaving-order-requisition-item:dispatch', 3, 15, 10280, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-27 00:00:00', '1', '2026-09-27 00:00:00', '0');

DELETE FROM system_role_menu WHERE menu_id IN (10289, 10290, 10291, 10292, 10293, 10294, 10295, 10296, 10297, 10298, 10299);
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (SELECT id FROM system_menu WHERE id IN (10289, 10290, 10291, 10292, 10293, 10294, 10295, 10296, 10297, 10298, 10299)) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2)
ON CONFLICT DO NOTHING;


