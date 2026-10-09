-- =============================================================================
-- 模块：LunaERP - 横切基础 - 轻量审批引擎
--      （审批流程定义、流程节点、审批实例、审批任务、审批操作记录）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，全列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，主键序列 IF NOT EXISTS，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查与迁移记录，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 审批流程定义表 lunaerp_approval_config
--    按 biz_type 一条启用流程；(tenant_id, biz_type) 启用态唯一（含软删行）
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_approval_config (
    id            int8          NOT NULL,
    tenant_id     int8          NOT NULL DEFAULT 0,
    biz_type      varchar(32)   NOT NULL,
    name          varchar(64)   NOT NULL,
    status        varchar(16)   NOT NULL DEFAULT 'ENABLED',
    remark        varchar(255)  NULL,
    creator       varchar(64)   NULL DEFAULT '',
    create_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       varchar(64)   NULL DEFAULT '',
    update_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       int2          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_approval_config') THEN
        ALTER TABLE lunaerp_approval_config ADD CONSTRAINT pk_lunaerp_approval_config PRIMARY KEY (id);
    END IF;
END $$;

-- 唯一索引含软删行：同 biz_type 仅一条 ENABLED 启用流程（不带 deleted=0 谓词，防止软删绕过启用排他）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lunaerp_approval_config_tenant_biz_enabled
    ON lunaerp_approval_config (tenant_id, biz_type)
    WHERE status = 'ENABLED';

CREATE INDEX IF NOT EXISTS idx_lunaerp_approval_config_tenant_status
    ON lunaerp_approval_config (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_lunaerp_approval_config_tenant_biz
    ON lunaerp_approval_config (tenant_id, biz_type);

COMMENT ON TABLE lunaerp_approval_config IS '审批引擎-审批流程定义表';
COMMENT ON COLUMN lunaerp_approval_config.id IS '流程配置ID';
COMMENT ON COLUMN lunaerp_approval_config.tenant_id IS '租户编号（TenantBaseDO）';
COMMENT ON COLUMN lunaerp_approval_config.biz_type IS '单据类型（sales_contract/payment/expense/payroll/sales_return，字典 lunaerp_approval_biz_type）';
COMMENT ON COLUMN lunaerp_approval_config.name IS '流程名称';
COMMENT ON COLUMN lunaerp_approval_config.status IS '状态（ENABLED 启用 / DISABLED 停用，同 biz_type 仅一条 ENABLED）';
COMMENT ON COLUMN lunaerp_approval_config.remark IS '备注说明';
COMMENT ON COLUMN lunaerp_approval_config.creator IS '创建者';
COMMENT ON COLUMN lunaerp_approval_config.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_approval_config.updater IS '更新者';
COMMENT ON COLUMN lunaerp_approval_config.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_approval_config.deleted IS '是否删除（0未删 1已删）';

-- =============================================================================
-- 2. 流程节点定义表 lunaerp_approval_config_node
--    节点顺序从 1 开始；审批人来源 USER/POST/ROLE/DEPT_LEADER/SUBMITTER_LEADER
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_approval_config_node (
    id            int8          NOT NULL,
    tenant_id     int8          NOT NULL DEFAULT 0,
    config_id     int8          NOT NULL,
    sort          int4          NOT NULL DEFAULT 1,
    name          varchar(64)   NOT NULL,
    node_type     varchar(16)   NOT NULL,
    approver_type varchar(16)   NOT NULL,
    approver_ids  varchar(512)  NULL,
    creator       varchar(64)   NULL DEFAULT '',
    create_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       varchar(64)   NULL DEFAULT '',
    update_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       int2          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_approval_config_node') THEN
        ALTER TABLE lunaerp_approval_config_node ADD CONSTRAINT pk_lunaerp_approval_config_node PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lunaerp_approval_config_node_config_sort
    ON lunaerp_approval_config_node (config_id, sort);

COMMENT ON TABLE lunaerp_approval_config_node IS '审批引擎-流程节点定义表';
COMMENT ON COLUMN lunaerp_approval_config_node.id IS '节点定义ID';
COMMENT ON COLUMN lunaerp_approval_config_node.tenant_id IS '租户编号（TenantBaseDO）';
COMMENT ON COLUMN lunaerp_approval_config_node.config_id IS '关联流程配置ID（lunaerp_approval_config.id）';
COMMENT ON COLUMN lunaerp_approval_config_node.sort IS '节点顺序（从1开始）';
COMMENT ON COLUMN lunaerp_approval_config_node.name IS '节点名称（如部门主管审批）';
COMMENT ON COLUMN lunaerp_approval_config_node.node_type IS '节点类型（SEQUENTIAL 逐级 / OR_SIGN 或签 / AND_SIGN 会签）';
COMMENT ON COLUMN lunaerp_approval_config_node.approver_type IS '审批人来源（USER 指定用户 / POST 指定岗位 / ROLE 指定角色 / DEPT_LEADER 部门负责人 / SUBMITTER_LEADER 提交人直属上级）';
COMMENT ON COLUMN lunaerp_approval_config_node.approver_ids IS '审批人ID标识（USER时存用户ID列表JSON；POST/ROLE时存岗位或角色ID）';
COMMENT ON COLUMN lunaerp_approval_config_node.creator IS '创建者';
COMMENT ON COLUMN lunaerp_approval_config_node.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_approval_config_node.updater IS '更新者';
COMMENT ON COLUMN lunaerp_approval_config_node.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_approval_config_node.deleted IS '是否删除（0未删 1已删）';

-- =============================================================================
-- 3. 审批实例表 lunaerp_approval_instance
--    对应 Flowable ACT_HI_PROCINST，发起时全流程节点与审批人解析快照存入 process_snapshot
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_approval_instance (
    id                     int8          NOT NULL,
    tenant_id              int8          NOT NULL DEFAULT 0,
    biz_type               varchar(32)   NOT NULL,
    biz_id                 int8          NOT NULL,
    biz_no                 varchar(64)   NULL,
    process_snapshot       text          NOT NULL,
    status                 varchar(16)   NOT NULL DEFAULT 'APPROVING',
    submitter_id           int8          NOT NULL,
    current_node_sort      int4          NOT NULL DEFAULT 1,
    flowable_proc_inst_id  varchar(64)   NULL,
    start_time             timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time               timestamp     NULL,
    creator                varchar(64)   NULL DEFAULT '',
    create_time            timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                varchar(64)   NULL DEFAULT '',
    update_time            timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                int2          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_approval_instance') THEN
        ALTER TABLE lunaerp_approval_instance ADD CONSTRAINT pk_lunaerp_approval_instance PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lunaerp_approval_instance_biz
    ON lunaerp_approval_instance (tenant_id, biz_type, biz_id);

CREATE INDEX IF NOT EXISTS idx_lunaerp_approval_instance_submitter
    ON lunaerp_approval_instance (tenant_id, submitter_id, status);

CREATE INDEX IF NOT EXISTS idx_lunaerp_approval_instance_status
    ON lunaerp_approval_instance (tenant_id, status);

COMMENT ON TABLE lunaerp_approval_instance IS '审批引擎-审批实例表';
COMMENT ON COLUMN lunaerp_approval_instance.id IS '审批实例ID';
COMMENT ON COLUMN lunaerp_approval_instance.tenant_id IS '租户编号（TenantBaseDO）';
COMMENT ON COLUMN lunaerp_approval_instance.biz_type IS '业务单据类型（sales_contract/payment/expense/payroll/sales_return）';
COMMENT ON COLUMN lunaerp_approval_instance.biz_id IS '业务单据ID';
COMMENT ON COLUMN lunaerp_approval_instance.biz_no IS '业务单号快照（展示用）';
COMMENT ON COLUMN lunaerp_approval_instance.process_snapshot IS '流程定义全量快照JSON（含每节点审批人ID列表，发起时解析冻结）';
COMMENT ON COLUMN lunaerp_approval_instance.status IS '实例状态（APPROVING 审批中 / APPROVED 已通过 / REJECTED 已驳回 / CANCELLED 已撤回）';
COMMENT ON COLUMN lunaerp_approval_instance.submitter_id IS '发起人用户ID';
COMMENT ON COLUMN lunaerp_approval_instance.current_node_sort IS '当前节点序号（-1表示流程已结束）';
COMMENT ON COLUMN lunaerp_approval_instance.flowable_proc_inst_id IS '预留 Flowable 流程实例ID（未来回迁BPM时回填）';
COMMENT ON COLUMN lunaerp_approval_instance.start_time IS '发起时间';
COMMENT ON COLUMN lunaerp_approval_instance.end_time IS '审批结束时间（终态时回填）';
COMMENT ON COLUMN lunaerp_approval_instance.creator IS '创建者';
COMMENT ON COLUMN lunaerp_approval_instance.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_approval_instance.updater IS '更新者';
COMMENT ON COLUMN lunaerp_approval_instance.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_approval_instance.deleted IS '是否删除（0未删 1已删）';

-- =============================================================================
-- 4. 审批任务表 lunaerp_approval_task
--    对应 ACT_RU_TASK + ACT_HI_TASKINST，含 version 乐观锁防会签并发覆盖
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_approval_task (
    id                int8          NOT NULL,
    tenant_id         int8          NOT NULL DEFAULT 0,
    instance_id       int8          NOT NULL,
    node_sort         int4          NOT NULL,
    node_name         varchar(64)   NOT NULL,
    node_type         varchar(16)   NOT NULL,
    approver_id       int8          NOT NULL,
    status            varchar(16)   NOT NULL DEFAULT 'PENDING',
    action            varchar(16)   NULL,
    start_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time          timestamp     NULL,
    flowable_task_id  varchar(64)   NULL,
    version           int4          NOT NULL DEFAULT 0,
    creator           varchar(64)   NULL DEFAULT '',
    create_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater           varchar(64)   NULL DEFAULT '',
    update_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           int2          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_approval_task') THEN
        ALTER TABLE lunaerp_approval_task ADD CONSTRAINT pk_lunaerp_approval_task PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lunaerp_approval_task_approver_status
    ON lunaerp_approval_task (tenant_id, approver_id, status);

CREATE INDEX IF NOT EXISTS idx_lunaerp_approval_task_instance_id
    ON lunaerp_approval_task (instance_id);

COMMENT ON TABLE lunaerp_approval_task IS '审批引擎-审批任务表';
COMMENT ON COLUMN lunaerp_approval_task.id IS '审批任务ID';
COMMENT ON COLUMN lunaerp_approval_task.tenant_id IS '租户编号（TenantBaseDO）';
COMMENT ON COLUMN lunaerp_approval_task.instance_id IS '关联审批实例ID（lunaerp_approval_instance.id）';
COMMENT ON COLUMN lunaerp_approval_task.node_sort IS '节点序号';
COMMENT ON COLUMN lunaerp_approval_task.node_name IS '节点名称快照';
COMMENT ON COLUMN lunaerp_approval_task.node_type IS '节点类型快照（SEQUENTIAL/OR_SIGN/AND_SIGN）';
COMMENT ON COLUMN lunaerp_approval_task.approver_id IS '当前审批人用户ID';
COMMENT ON COLUMN lunaerp_approval_task.status IS '任务状态（PENDING 待处理 / DONE 已处理）';
COMMENT ON COLUMN lunaerp_approval_task.action IS '处理动作（空 / DONE 通过 / REJECTED 驳回 / TRANSFERRED 转交）';
COMMENT ON COLUMN lunaerp_approval_task.start_time IS '任务到达时间';
COMMENT ON COLUMN lunaerp_approval_task.end_time IS '任务完成时间';
COMMENT ON COLUMN lunaerp_approval_task.flowable_task_id IS '预留 Flowable 任务ID（未来回迁BPM时回填）';
COMMENT ON COLUMN lunaerp_approval_task.version IS '乐观锁版本号（会签并发控制）';
COMMENT ON COLUMN lunaerp_approval_task.creator IS '创建者';
COMMENT ON COLUMN lunaerp_approval_task.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_approval_task.updater IS '更新者';
COMMENT ON COLUMN lunaerp_approval_task.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_approval_task.deleted IS '是否删除（0未删 1已删）';

-- =============================================================================
-- 5. 审批操作记录表 lunaerp_approval_record
--    对应 ACT_HI_ACTINST，记录实例上的每次操作（提交/通过/驳回/转交/撤回），审计追溯留痕
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_approval_record (
    id              int8          NOT NULL,
    tenant_id       int8          NOT NULL DEFAULT 0,
    instance_id     int8          NOT NULL,
    task_id         int8          NULL,
    operator_id     int8          NOT NULL,
    action          varchar(16)   NOT NULL,
    comment         varchar(512)  NULL,
    transfer_to_id  int8          NULL,
    operate_time    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    creator         varchar(64)   NULL DEFAULT '',
    create_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64)   NULL DEFAULT '',
    update_time     timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_approval_record') THEN
        ALTER TABLE lunaerp_approval_record ADD CONSTRAINT pk_lunaerp_approval_record PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lunaerp_approval_record_instance_time
    ON lunaerp_approval_record (instance_id, operate_time);

CREATE INDEX IF NOT EXISTS idx_lunaerp_approval_record_operator_action
    ON lunaerp_approval_record (tenant_id, operator_id, action);

COMMENT ON TABLE lunaerp_approval_record IS '审批引擎-审批操作记录表';
COMMENT ON COLUMN lunaerp_approval_record.id IS '操作记录ID';
COMMENT ON COLUMN lunaerp_approval_record.tenant_id IS '租户编号（TenantBaseDO）';
COMMENT ON COLUMN lunaerp_approval_record.instance_id IS '关联审批实例ID（lunaerp_approval_instance.id）';
COMMENT ON COLUMN lunaerp_approval_record.task_id IS '关联审批任务ID（lunaerp_approval_task.id，可空如撤回/提交操作）';
COMMENT ON COLUMN lunaerp_approval_record.operator_id IS '操作人用户ID';
COMMENT ON COLUMN lunaerp_approval_record.action IS '操作动作（SUBMIT 提交 / APPROVE 通过 / REJECT 驳回 / TRANSFER 转交 / WITHDRAW 撤回）';
COMMENT ON COLUMN lunaerp_approval_record.comment IS '审批意见/处理说明';
COMMENT ON COLUMN lunaerp_approval_record.transfer_to_id IS '转交接收人用户ID（action=TRANSFER 时必填）';
COMMENT ON COLUMN lunaerp_approval_record.operate_time IS '操作时间';
COMMENT ON COLUMN lunaerp_approval_record.creator IS '创建者';
COMMENT ON COLUMN lunaerp_approval_record.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_approval_record.updater IS '更新者';
COMMENT ON COLUMN lunaerp_approval_record.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_approval_record.deleted IS '是否删除（0未删 1已删）';

-- =============================================================================
-- 6. 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_approval_config_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_approval_config_node_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_approval_instance_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_approval_task_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_approval_record_seq;

-- =============================================================================
-- 7. 菜单与按钮（system_menu，可重复执行）
--    10800 「审批中心」一级目录（sort 109，path /approval）
--    10810 「待我审批」菜单（sort 10，path todo，权限 lunaerp:approval:task:query）
--    10820 「我已审批」菜单（sort 20，path done，权限 lunaerp:approval:task:query）
--    10830 「我发起的」菜单（sort 30，path mine，权限 lunaerp:approval:task:query）
--    10840 「审批链配置」菜单（sort 40，path config，权限 lunaerp:approval:config:query）
--    10841-10843 审批配置 创建/修改/删除 按钮权限
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10800, 10810, 10820, 10830, 10840, 10841, 10842, 10843)
   OR permission IN (
       'lunaerp:approval:config:create',
       'lunaerp:approval:config:update',
       'lunaerp:approval:config:delete'
   );

-- 一级目录：审批中心（parent_id=0 顶级目录，sort=109，path=/approval）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10800, '审批中心', '', 1, 109, 0, '/approval', 'ep:stamp', NULL, NULL,
    0, '1', '1', '1', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0'
);

-- 子菜单 1：待我审批（sort 10，path todo，权限 lunaerp:approval:task:query）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10810, '待我审批', 'lunaerp:approval:task:query', 2, 10, 10800, 'todo', 'ep:clock', 'approval/todo/index', 'ApprovalTodo',
    0, '1', '1', '1', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0'
);

-- 子菜单 2：我已审批（sort 20，path done，权限 lunaerp:approval:task:query）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10820, '我已审批', 'lunaerp:approval:task:query', 2, 20, 10800, 'done', 'ep:circle-check', 'approval/done/index', 'ApprovalDone',
    0, '1', '1', '1', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0'
);

-- 子菜单 3：我发起的（sort 30，path mine，权限 lunaerp:approval:task:query）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10830, '我发起的', 'lunaerp:approval:task:query', 2, 30, 10800, 'mine', 'ep:user', 'approval/mine/index', 'ApprovalMine',
    0, '1', '1', '1', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0'
);

-- 子菜单 4：审批链配置（sort 40，path config，权限 lunaerp:approval:config:query）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10840, '审批链配置', 'lunaerp:approval:config:query', 2, 40, 10800, 'config', 'ep:operation', 'approval/config/index', 'ApprovalConfig',
    0, '1', '1', '1', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0'
);

-- 按钮：审批链配置 创建 / 修改 / 删除
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10841, '流程创建', 'lunaerp:approval:config:create', 3, 1, 10840, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0'
);

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10842, '流程修改', 'lunaerp:approval:config:update', 3, 2, 10840, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0'
);

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10843, '流程删除', 'lunaerp:approval:config:delete', 3, 3, 10840, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0'
);

-- 授权管理员与超管角色
DELETE FROM system_role_menu WHERE menu_id IN (10800, 10810, 10820, 10830, 10840, 10841, 10842, 10843);
INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (
    SELECT 10800 AS id UNION ALL SELECT 10810 UNION ALL SELECT 10820 UNION ALL SELECT 10830 UNION ALL
    SELECT 10840 UNION ALL SELECT 10841 UNION ALL SELECT 10842 UNION ALL SELECT 10843
) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2)
ON CONFLICT DO NOTHING;

COMMIT;
-- @formatter:on

-- =============================================================================
-- 8. 字典类型与字典数据（system_dict_type / system_dict_data，可重复执行）
--    dict_type id 2270-2275，dict_data id 3801-3825
--    1) lunaerp_approval_biz_type       单据类型（sales_contract/payment/expense/payroll/sales_return）
--    2) lunaerp_approval_node_type      节点类型（SEQUENTIAL/OR_SIGN/AND_SIGN）
--    3) lunaerp_approval_approver_type  审批人来源（USER/POST/ROLE/DEPT_LEADER/SUBMITTER_LEADER）
--    4) lunaerp_approval_instance_status 实例状态（APPROVING/APPROVED/REJECTED/CANCELLED）
--    5) lunaerp_approval_task_action    任务动作（DONE/REJECTED/TRANSFERRED）
--    6) lunaerp_approval_record_action  记录动作（APPROVE/REJECT/TRANSFER/WITHDRAW/SUBMIT）
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2270, 2271, 2272, 2273, 2274, 2275)
   OR type IN (
       'lunaerp_approval_biz_type',
       'lunaerp_approval_node_type',
       'lunaerp_approval_approver_type',
       'lunaerp_approval_instance_status',
       'lunaerp_approval_task_action',
       'lunaerp_approval_record_action'
   );

DELETE FROM system_dict_data WHERE dict_type IN (
       'lunaerp_approval_biz_type',
       'lunaerp_approval_node_type',
       'lunaerp_approval_approver_type',
       'lunaerp_approval_instance_status',
       'lunaerp_approval_task_action',
       'lunaerp_approval_record_action'
   )
   OR id BETWEEN 3801 AND 3825;

-- 8.1 字典类型：单据类型
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2270, '审批单据类型', 'lunaerp_approval_biz_type', 0, 'LunaERP 审批引擎-单据类型', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3801, 1, '销售合同', 'sales_contract', 'lunaerp_approval_biz_type', 0, 'primary', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3802, 2, '付款申请', 'payment', 'lunaerp_approval_biz_type', 0, 'success', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3803, 3, '费用报销', 'expense', 'lunaerp_approval_biz_type', 0, 'warning', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3804, 4, '工资单', 'payroll', 'lunaerp_approval_biz_type', 0, 'info', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3805, 5, '销售退货', 'sales_return', 'lunaerp_approval_biz_type', 0, 'danger', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');

-- 8.2 字典类型：节点类型
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2271, '审批节点类型', 'lunaerp_approval_node_type', 0, 'LunaERP 审批引擎-节点类型', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3806, 1, '逐级审批', 'SEQUENTIAL', 'lunaerp_approval_node_type', 0, 'primary', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3807, 2, '或签', 'OR_SIGN', 'lunaerp_approval_node_type', 0, 'warning', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3808, 3, '会签', 'AND_SIGN', 'lunaerp_approval_node_type', 0, 'success', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');

-- 8.3 字典类型：审批人来源
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2272, '审批人来源', 'lunaerp_approval_approver_type', 0, 'LunaERP 审批引擎-审批人来源', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3809, 1, '指定用户', 'USER', 'lunaerp_approval_approver_type', 0, 'primary', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3810, 2, '指定岗位', 'POST', 'lunaerp_approval_approver_type', 0, 'info', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3811, 3, '指定角色', 'ROLE', 'lunaerp_approval_approver_type', 0, 'warning', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3812, 4, '部门负责人', 'DEPT_LEADER', 'lunaerp_approval_approver_type', 0, 'success', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3813, 5, '提交人直属上级', 'SUBMITTER_LEADER', 'lunaerp_approval_approver_type', 0, 'danger', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');

-- 8.4 字典类型：实例状态
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2273, '审批实例状态', 'lunaerp_approval_instance_status', 0, 'LunaERP 审批引擎-实例状态', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3814, 1, '审批中', 'APPROVING', 'lunaerp_approval_instance_status', 0, 'primary', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3815, 2, '已通过', 'APPROVED', 'lunaerp_approval_instance_status', 0, 'success', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3816, 3, '已驳回', 'REJECTED', 'lunaerp_approval_instance_status', 0, 'danger', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3817, 4, '已撤回', 'CANCELLED', 'lunaerp_approval_instance_status', 0, 'info', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');

-- 8.5 字典类型：任务动作
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2274, '审批任务动作', 'lunaerp_approval_task_action', 0, 'LunaERP 审批引擎-任务动作', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3818, 1, '通过', 'DONE', 'lunaerp_approval_task_action', 0, 'success', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3819, 2, '驳回', 'REJECTED', 'lunaerp_approval_task_action', 0, 'danger', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3820, 3, '转交', 'TRANSFERRED', 'lunaerp_approval_task_action', 0, 'warning', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');

-- 8.6 字典类型：记录动作
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2275, '审批记录动作', 'lunaerp_approval_record_action', 0, 'LunaERP 审批引擎-记录动作', 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3821, 1, '提交审批', 'SUBMIT', 'lunaerp_approval_record_action', 0, 'primary', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3822, 2, '通过', 'APPROVE', 'lunaerp_approval_record_action', 0, 'success', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3823, 3, '驳回', 'REJECT', 'lunaerp_approval_record_action', 0, 'danger', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3824, 4, '转交', 'TRANSFER', 'lunaerp_approval_record_action', 0, 'warning', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3825, 5, '撤回', 'WITHDRAW', 'lunaerp_approval_record_action', 0, 'info', '', NULL, 'admin', '2026-10-09 00:00:00', '1', '2026-10-09 00:00:00', '0');

COMMIT;
-- @formatter:on
