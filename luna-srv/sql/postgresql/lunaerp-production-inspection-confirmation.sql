-- =============================================================================
-- 模块：LunaERP - 生产管理 - 质检确认
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT，岗位用 DO 块判存幂等更新/插入。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 质检确认表 lunaerp_production_inspection_confirmation
--    认定链 chain_key 标识同卷多轮认定与撤销改判留痕（首行预生成，撤销克隆沿用）；
--    来源检验单与录检明细为逻辑关联，不建数据库外键；卷信息快照入库；
--    责任认定为 OPERATOR 时必填挡车工（关联 system_users）；
--    所有行一律不可删除，误生成以免扣（WAIVED）结案。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_inspection_confirmation (
    id                       int8          NOT NULL,
    chain_key                varchar(48)   NOT NULL,
    source                   varchar(16)   NOT NULL,
    status                   varchar(16)   NOT NULL DEFAULT 'PENDING',
    inspection_order_id      int8          NULL,
    inspection_order_code    varchar(32)   NULL,
    inspection_order_item_id int8          NULL,
    roll_no                  varchar(64)   NOT NULL,
    vat_no                   varchar(64)   NULL,
    grade                    varchar(16)   NOT NULL,
    machine_no               varchar(64)   NULL,
    product_code             varchar(64)   NULL,
    product_name             varchar(128)  NULL,
    color_name               varchar(128)  NULL,
    color_no                 varchar(64)   NULL,
    style_no                 varchar(64)   NULL,
    measured_weight          numeric(18,2) NULL,
    length_m                 numeric(18,2) NULL,
    responsibility           varchar(16)   NULL,
    worker_user_id           int8          NULL,
    worker_name              varchar(64)   NULL,
    confirmed_by_user_id     int8          NULL,
    confirmed_by_name        varchar(64)   NULL,
    confirmed_at             timestamp     NULL,
    note                     varchar(1000) NULL,
    revoked_by_user_id       int8          NULL,
    revoked_by_name          varchar(64)   NULL,
    revoked_at               timestamp     NULL,
    creator                  varchar(64)   NULL DEFAULT '',
    create_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)   NULL DEFAULT '',
    update_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2          NOT NULL DEFAULT 0,
    tenant_id                int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_production_inspection_confirmation') THEN
        ALTER TABLE lunaerp_production_inspection_confirmation ADD CONSTRAINT pk_lb_production_inspection_confirmation PRIMARY KEY (id);
    END IF;
END $$;

-- 5 个普通索引（对齐 spec 索引定义）：
-- (tenant_id, status) - 列表按状态过滤
-- (tenant_id, chain_key) - 认定链详情追溯
-- (tenant_id, inspection_order_id, inspection_order_item_id) - 检验单生成幂等查重
-- (tenant_id, create_time) - 批量打印发现日筛选与列表默认排序
-- (tenant_id, confirmed_at) - 工资扣款汇总确认日聚合
CREATE INDEX IF NOT EXISTS idx_lb_insp_confirm_tenant_status
    ON lunaerp_production_inspection_confirmation (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_lb_insp_confirm_tenant_chain_key
    ON lunaerp_production_inspection_confirmation (tenant_id, chain_key);
CREATE INDEX IF NOT EXISTS idx_lb_insp_confirm_tenant_order_item
    ON lunaerp_production_inspection_confirmation (tenant_id, inspection_order_id, inspection_order_item_id);
CREATE INDEX IF NOT EXISTS idx_lb_insp_confirm_tenant_create_time
    ON lunaerp_production_inspection_confirmation (tenant_id, create_time);
CREATE INDEX IF NOT EXISTS idx_lb_insp_confirm_tenant_confirmed_at
    ON lunaerp_production_inspection_confirmation (tenant_id, confirmed_at);

COMMENT ON TABLE lunaerp_production_inspection_confirmation IS '生产管理-质检确认表';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.id IS '质检确认记录ID';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.chain_key IS '认定链标识（首行由服务端预生成，撤销克隆沿用首行值，同链多行同值）';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.source IS '来源（INSPECTION 检验单引入 / MANUAL 手工登记，内部枚举）';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.status IS '状态（字典 lunaerp_inspection_confirmation_status：PENDING 待确认 / CONFIRMED 已认定 / REVOKED 已撤销）';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.inspection_order_id IS '来源检验单ID（lunaerp_production_inspection_order.id，逻辑关联）';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.inspection_order_code IS '检验单号快照';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.inspection_order_item_id IS '来源录检明细ID（lunaerp_production_inspection_order_item.id，INSPECTION 必填）';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.roll_no IS '卷号快照（MANUAL 亦必填）';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.vat_no IS '缸号快照';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.grade IS '等级快照（复用字典 lunaerp_inspection_grade）';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.machine_no IS '机台号快照';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.product_code IS '产品编码快照';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.product_name IS '产品名称快照';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.color_name IS '颜色名称快照';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.color_no IS '产品色号快照';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.style_no IS '款号快照';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.measured_weight IS '实称重量 KG 快照';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.length_m IS '米数快照';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.responsibility IS '责任类型（字典 lunaerp_inspection_responsibility：EQUIPMENT 设备故障（免扣）/ OPERATOR 操作失误（扣款）/ WAIVED 免扣）';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.worker_user_id IS '挡车工用户ID（关联 system_users，OPERATOR 时必填）';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.worker_name IS '挡车工姓名快照';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.confirmed_by_user_id IS '认定人用户ID（当前登录用户）';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.confirmed_by_name IS '认定人姓名快照';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.confirmed_at IS '认定时间（手工登记可补录）';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.note IS '原因备注';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.revoked_by_user_id IS '撤销人用户ID';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.revoked_by_name IS '撤销人姓名快照';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.revoked_at IS '撤销时间（非空即 REVOKED 冻结行）';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.create_time IS '创建时间（发现日）';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_production_inspection_confirmation.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_inspection_confirmation_seq;

-- =============================================================================
-- 3. 字典类型与字典数据（system_dict_type / system_dict_data，可重复执行）
--    dict_type id 2236（质检确认状态）、2237（质检责任类型），
--    dict_data id 3603-3605（状态）、3606-3608（责任类型）。
-- =============================================================================
-- 避让已用值：若 2236 被 stock 误用，迁至 2245 空位
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM system_dict_type WHERE id = 2236 AND type != 'lunaerp_inspection_confirmation_status') THEN
        UPDATE system_dict_type SET id = 2245 WHERE id = 2236;
    END IF;
END $$;

-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2236, 2237) OR type IN ('lunaerp_inspection_confirmation_status', 'lunaerp_inspection_responsibility');
DELETE FROM system_dict_data WHERE dict_type IN ('lunaerp_inspection_confirmation_status', 'lunaerp_inspection_responsibility')
    OR id IN (3603, 3604, 3605, 3606, 3607, 3608);

-- 字典类型：质检确认状态
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2236, '质检确认状态', 'lunaerp_inspection_confirmation_status', 0, 'LunaERP 生产管理-质检确认状态', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0', NULL);

-- 字典数据：质检确认状态（待确认 / 已认定 / 已撤销）
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES
(3603, 1, '待确认', 'PENDING', 'lunaerp_inspection_confirmation_status', 0, 'warning', '', '待车间领班责任认定', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0'),
(3604, 2, '已认定', 'CONFIRMED', 'lunaerp_inspection_confirmation_status', 0, 'success', '', '已完成责任认定，作为扣款凭据', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0'),
(3605, 3, '已撤销', 'REVOKED', 'lunaerp_inspection_confirmation_status', 0, 'info', '', '已撤销改判并冻结留痕', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 字典类型：质检责任类型
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2237, '质检责任类型', 'lunaerp_inspection_responsibility', 0, 'LunaERP 生产管理-质检责任类型', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0', NULL);

-- 字典数据：质检责任类型（设备故障（免扣） / 操作失误（扣款） / 免扣）
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES
(3606, 1, '设备故障（免扣）', 'EQUIPMENT', 'lunaerp_inspection_responsibility', 0, 'info', '', '设备原因导致，免于扣款', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0'),
(3607, 2, '操作失误（扣款）', 'OPERATOR', 'lunaerp_inspection_responsibility', 0, 'danger', '', '挡车工操作失误，扣减工资', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0'),
(3608, 3, '免扣', 'WAIVED', 'lunaerp_inspection_responsibility', 0, 'success', '', '免扣不追责结案', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 4. 挡车工岗位 seed（system_post，ADR-0021，可重复执行）
--    按岗位编码 LUNA_WORKER 幂等维护：存在则更新名称与启用状态，不存在则插入新记录；不固定 ID。
-- =============================================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM system_post WHERE code = 'LUNA_WORKER') THEN
        UPDATE system_post
        SET name = '挡车工',
            status = 0,
            deleted = 0,
            updater = '1',
            update_time = CURRENT_TIMESTAMP
        WHERE code = 'LUNA_WORKER';
    ELSE
        INSERT INTO system_post (
            id, code, name, sort, status, remark,
            creator, create_time, updater, update_time, deleted, tenant_id
        ) VALUES (
            nextval('system_post_seq'), 'LUNA_WORKER', '挡车工', 20, 0, '车间质检责任认定挡车工岗位',
            'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0, 1
        );
    END IF;
END $$;

-- =============================================================================
-- 5. 菜单与按钮（system_menu，可重复执行）
--    10200 生产管理目录下挂载 10270 质检确认菜单（承载 query 权限，sort=7）。
--    10271 手工登记(:create) / 10272 生成质检确认(:generate) /
--    10273 责任认定(:confirm) / 10274 批量打印(:print) / 10275 工资扣款汇总(:payroll) 五个按钮。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10270, 10271, 10272, 10273, 10274, 10275);

-- 菜单：质检确认（query 权限挂在菜单本身，参照检验单/胚布单模式）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10270, '质检确认', 'lunaerp:production:inspection-confirmation:query', 2, 7, 10200, 'inspection-confirmation', 'ep:circle-check', 'lunaerp-elmui/production/inspection-confirmation/index', 'LunaerpElmuiProductionInspectionConfirmation', 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 按钮：手工登记 / 生成质检确认 / 责任认定 / 批量打印 / 工资扣款汇总
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES
(10271, '手工登记', 'lunaerp:production:inspection-confirmation:create', 3, 1, 10270, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0'),
(10272, '生成质检确认', 'lunaerp:production:inspection-confirmation:generate', 3, 2, 10270, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0'),
(10273, '责任认定', 'lunaerp:production:inspection-confirmation:confirm', 3, 3, 10270, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0'),
(10274, '批量打印', 'lunaerp:production:inspection-confirmation:print', 3, 4, 10270, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0'),
(10275, '工资扣款汇总', 'lunaerp:production:inspection-confirmation:payroll', 3, 5, 10270, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 超级管理员（角色 1/2）自动赋予菜单与按钮权限
CREATE SEQUENCE IF NOT EXISTS system_role_menu_seq;
DO $$
BEGIN
    PERFORM setval('system_role_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role_menu), 1));
END $$;

DELETE FROM system_role_menu WHERE menu_id IN (10270, 10271, 10272, 10273, 10274, 10275);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (SELECT id FROM system_menu WHERE id IN (10270, 10271, 10272, 10273, 10274, 10275)) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2);

COMMIT;
-- @formatter:on
