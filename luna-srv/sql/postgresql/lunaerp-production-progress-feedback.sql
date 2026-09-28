-- =============================================================================
-- 模块：LunaERP - 生产管理 - 进度反馈（外发染色缸级进度覆盖层与反馈记录流水）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 零快照决策（ADR-0031）：表上不含任何名称展示快照列（合同号/客户/染厂/产品/颜色），
--                         实时经染色单主表与颜色明细行查询取出。
-- 零外键约束：不建数据库外键、不建缸号数据库唯一约束（缸号由业务层按单+颜色行校验唯一）。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 反馈缸主表 lunaerp_production_progress_feedback
--    进度反馈的最小单位——某张染色单某个颜色行下投染的一缸（ADR-0031）。
--    持有稳定关联 dyeing_order_id + dyeing_color_id + vat_no；
--    状态：SCHEDULED 已排缸 / DYEING 染色中 / FIXING 固色中 / QUALITY_CHECK 质检中 / COMPLETED 已完成。
--    NOT_SCHEDULED 未排缸为覆盖层虚拟态，不持久化入库。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_progress_feedback (
    id                       int8          NOT NULL,
    dyeing_order_id          int8          NOT NULL,
    dyeing_color_id          int8          NOT NULL,
    vat_no                   varchar(64)   NOT NULL,
    quantity                 numeric(18,2) NOT NULL DEFAULT 0,
    pieces                   int4          NULL,
    status                   varchar(32)   NOT NULL DEFAULT 'SCHEDULED',
    scheduled_date           date          NULL,
    dyeing_started_date      date          NULL,
    fixing_started_date      date          NULL,
    quality_check_date       date          NULL,
    completed_date           date          NULL,
    delay_reason             varchar(1000) NULL,
    remark                   varchar(1000) NULL,
    creator                  varchar(64)   NULL DEFAULT '',
    create_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)   NULL DEFAULT '',
    update_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2          NOT NULL DEFAULT 0,
    tenant_id                int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_prog_fb') THEN
        ALTER TABLE lunaerp_production_progress_feedback ADD CONSTRAINT pk_lb_prog_fb PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_prog_fb_tenant_order_color
    ON lunaerp_production_progress_feedback (tenant_id, dyeing_order_id, dyeing_color_id);
CREATE INDEX IF NOT EXISTS idx_lb_prog_fb_status
    ON lunaerp_production_progress_feedback (status);

COMMENT ON TABLE lunaerp_production_progress_feedback IS '生产管理-进度反馈缸表（外发染色缸级进度覆盖层）';
COMMENT ON COLUMN lunaerp_production_progress_feedback.id IS '反馈缸ID';
COMMENT ON COLUMN lunaerp_production_progress_feedback.dyeing_order_id IS '关联染色单ID（lunaerp_production_dyeing_order.id，逻辑关联无外键）';
COMMENT ON COLUMN lunaerp_production_progress_feedback.dyeing_color_id IS '关联染色颜色明细ID（lunaerp_production_dyeing_color.id，逻辑关联无外键）';
COMMENT ON COLUMN lunaerp_production_progress_feedback.vat_no IS '缸号（同一染色单同一颜色行下由服务层校验唯一，不建数据库唯一约束）';
COMMENT ON COLUMN lunaerp_production_progress_feedback.quantity IS '本缸数量';
COMMENT ON COLUMN lunaerp_production_progress_feedback.pieces IS '匹数（可空）';
COMMENT ON COLUMN lunaerp_production_progress_feedback.status IS '进度状态（字典 lunaerp_progress_feedback_status：SCHEDULED/DYEING/FIXING/QUALITY_CHECK/COMPLETED，缺省 SCHEDULED）';
COMMENT ON COLUMN lunaerp_production_progress_feedback.scheduled_date IS '排缸日期（阶段一日期戳，建缸缺省当天，可人工修正）';
COMMENT ON COLUMN lunaerp_production_progress_feedback.dyeing_started_date IS '染色开始日期（阶段二日期戳，可人工修正）';
COMMENT ON COLUMN lunaerp_production_progress_feedback.fixing_started_date IS '固色开始日期（阶段三日期戳，可人工修正）';
COMMENT ON COLUMN lunaerp_production_progress_feedback.quality_check_date IS '质检日期（阶段四日期戳，可人工修正）';
COMMENT ON COLUMN lunaerp_production_progress_feedback.completed_date IS '完成日期（阶段五日期戳，可人工修正）';
COMMENT ON COLUMN lunaerp_production_progress_feedback.delay_reason IS '延期原因（自由文本，可空可改）';
COMMENT ON COLUMN lunaerp_production_progress_feedback.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_progress_feedback.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_progress_feedback.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_progress_feedback.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_progress_feedback.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_progress_feedback.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_production_progress_feedback.tenant_id IS '租户编号';

-- =============================================================================
-- 1b. 反馈记录流水表 lunaerp_production_progress_feedback_note
--     反馈缸下的跟单沟通流水（ADR-0014 独立子资源）：
--     记录文字内容、问题标记、闭环时间；流水只增不改，闭环问题记录禁删。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_progress_feedback_note (
    id                       int8          NOT NULL,
    feedback_id              int8          NOT NULL,
    content                  text          NOT NULL,
    issue_flag               boolean       NOT NULL DEFAULT false,
    resolved_at              timestamp     NULL,
    creator                  varchar(64)   NULL DEFAULT '',
    create_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)   NULL DEFAULT '',
    update_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2          NOT NULL DEFAULT 0,
    tenant_id                int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_prog_fb_note') THEN
        ALTER TABLE lunaerp_production_progress_feedback_note ADD CONSTRAINT pk_lb_prog_fb_note PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_prog_fb_note_feedback_id
    ON lunaerp_production_progress_feedback_note (feedback_id);

COMMENT ON TABLE lunaerp_production_progress_feedback_note IS '生产管理-进度反馈记录流水表（独立子资源，只增不改）';
COMMENT ON COLUMN lunaerp_production_progress_feedback_note.id IS '记录ID';
COMMENT ON COLUMN lunaerp_production_progress_feedback_note.feedback_id IS '所属反馈缸ID（lunaerp_production_progress_feedback.id，逻辑关联无外键）';
COMMENT ON COLUMN lunaerp_production_progress_feedback_note.content IS '反馈沟通内容（非空文本）';
COMMENT ON COLUMN lunaerp_production_progress_feedback_note.issue_flag IS '是否标记为问题（true 是，false 否）';
COMMENT ON COLUMN lunaerp_production_progress_feedback_note.resolved_at IS '问题闭环时间（可空，闭环后写入）';
COMMENT ON COLUMN lunaerp_production_progress_feedback_note.creator IS '记录人';
COMMENT ON COLUMN lunaerp_production_progress_feedback_note.create_time IS '记录时间';
COMMENT ON COLUMN lunaerp_production_progress_feedback_note.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_progress_feedback_note.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_progress_feedback_note.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_production_progress_feedback_note.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_progress_feedback_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_progress_feedback_note_seq;

-- =============================================================================
-- 3. 菜单与按钮（system_menu，可重复执行）
--    10200 生产管理目录下挂载 10400 进度反馈菜单（承载 query 权限）。
--    10401-10405 为进度反馈下的按钮权限（建缸/编辑/推进/删除/反馈记录）。
--    【跳号原因说明】：
--    10200 目录下 10280 织造单后的 10289-10299 已被织造领纱单与领纱明细按钮占用；
--    10300 起为库存管理顶级目录（10300+）；
--    因此进度反馈选用 10400 段（10400 菜单 + 10401-10405 按钮）。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10400, 10401, 10402, 10403, 10404, 10405);

-- 菜单：进度反馈（query 权限挂在菜单本身，组件路径 lunaerp-elmui/production/progress-feedback/index）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10400, '进度反馈', 'lunaerp:production:progress-feedback:query', 2, 10, 10200, 'progress-feedback', 'ep:odometer', 'lunaerp-elmui/production/progress-feedback/index', 'LunaerpElmuiProductionProgressFeedback', 0, '1', '1', '1', 'admin', '2026-09-28 00:00:00', '1', '2026-09-28 00:00:00', '0');

-- 按钮：进度反馈建缸/编辑/推进/删除/反馈记录
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10401, '建缸', 'lunaerp:production:progress-feedback:create', 3, 1, 10400, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-28 00:00:00', '1', '2026-09-28 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10402, '编辑', 'lunaerp:production:progress-feedback:update', 3, 2, 10400, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-28 00:00:00', '1', '2026-09-28 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10403, '推进', 'lunaerp:production:progress-feedback:advance', 3, 3, 10400, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-28 00:00:00', '1', '2026-09-28 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10404, '删除', 'lunaerp:production:progress-feedback:delete', 3, 4, 10400, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-28 00:00:00', '1', '2026-09-28 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10405, '反馈记录', 'lunaerp:production:progress-feedback:note', 3, 5, 10400, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-28 00:00:00', '1', '2026-09-28 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 4. 字典类型与字典数据（system_dict_type / system_dict_data，可重复执行）
--    dict_type id 2247（进度反馈状态），dict_data id 3696-3701。
--    含六态：未排缸 NOT_SCHEDULED（虚拟态，仅用于渲染不持久化）/ 已排缸 SCHEDULED /
--            染色中 DYEING / 固色中 FIXING / 质检中 QUALITY_CHECK / 已完成 COMPLETED。
--    单位复用 lunaerp_sales_contract_unit。
-- =============================================================================
-- 避让已用值：若 2247 已被非进度反馈状态字典占用（如 ADR-0026 历史占用的 lunaerp_stock_item_type），平移至 2248（防重幂等）
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2247 AND type != 'lunaerp_progress_feedback_status') THEN
        IF NOT EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2248) THEN
            UPDATE system_dict_type SET id = 2248 WHERE id = 2247;
        ELSE
            DELETE FROM system_dict_type WHERE id = 2247;
        END IF;
    END IF;
END $$;

-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2247) OR type IN ('lunaerp_progress_feedback_status');
DELETE FROM system_dict_data WHERE dict_type IN ('lunaerp_progress_feedback_status')
    OR id IN (3696, 3697, 3698, 3699, 3700, 3701);

-- 字典类型：进度反馈状态
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2247, '进度反馈状态', 'lunaerp_progress_feedback_status', 0, 'LunaERP 生产管理-进度反馈状态', 'admin', '2026-09-28 00:00:00', '1', '2026-09-28 00:00:00', '0', NULL);

-- 进度反馈状态：六态（含虚拟未排缸）
-- 1. NOT_SCHEDULED 未排缸（虚拟态，仅用于前端 DictTag 渲染与筛选，不持久化）
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3696, 1, '未排缸', 'NOT_SCHEDULED', 'lunaerp_progress_feedback_status', 0, 'info', '', '虚拟态，仅用于渲染不持久化', 'admin', '2026-09-28 00:00:00', '1', '2026-09-28 00:00:00', '0');
-- 2. SCHEDULED 已排缸（建缸缺省态）
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3697, 2, '已排缸', 'SCHEDULED', 'lunaerp_progress_feedback_status', 0, 'primary', '', '已排缸（建缸缺省态）', 'admin', '2026-09-28 00:00:00', '1', '2026-09-28 00:00:00', '0');
-- 3. DYEING 染色中
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3698, 3, '染色中', 'DYEING', 'lunaerp_progress_feedback_status', 0, 'warning', '', '染色中', 'admin', '2026-09-28 00:00:00', '1', '2026-09-28 00:00:00', '0');
-- 4. FIXING 固色中
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3699, 4, '固色中', 'FIXING', 'lunaerp_progress_feedback_status', 0, 'warning', '', '固色中', 'admin', '2026-09-28 00:00:00', '1', '2026-09-28 00:00:00', '0');
-- 5. QUALITY_CHECK 质检中
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3700, 5, '质检中', 'QUALITY_CHECK', 'lunaerp_progress_feedback_status', 0, 'warning', '', '质检中', 'admin', '2026-09-28 00:00:00', '1', '2026-09-28 00:00:00', '0');
-- 6. COMPLETED 已完成
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3701, 6, '已完成', 'COMPLETED', 'lunaerp_progress_feedback_status', 0, 'success', '', '已完成', 'admin', '2026-09-28 00:00:00', '1', '2026-09-28 00:00:00', '0');

COMMIT;
-- @formatter:on
