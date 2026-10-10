-- =============================================================================
-- 模块：LunaERP - 生产管理 - 打色指示单（Color Instruction）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- =============================================================================

-- =============================================================================
-- 1. 打色指示单主表 lunaerp_production_color_instruction
--    客户/产品强关联快照；打色承接方供应商可空（空=本厂打色房自做，ADR-0022 自织先例）；
--    不产生库存/账务副作用。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_color_instruction (
    id                       int8          NOT NULL,
    instruction_no           varchar(32)   NOT NULL,
    customer_id              int8          NOT NULL,
    client_name              varchar(128)  NOT NULL,
    contact_person           varchar(64)   NULL,
    product_id               int8          NOT NULL,
    product_code             varchar(64)   NOT NULL,
    product_name             varchar(128)  NOT NULL,
    lab_factory_id           int8          NULL,
    lab_factory_name         varchar(128)  NULL,
    composition              varchar(256)  NULL,
    width                    varchar(64)   NULL,
    gsm                      varchar(64)   NULL,
    instruction_date         date          NOT NULL,
    delivery_date            date          NULL,
    status                   varchar(16)   NOT NULL DEFAULT 'PENDING',
    confirm_type             varchar(16)   NULL,
    customer_feedback        varchar(500)  NULL,
    confirmed_by             int8          NULL,
    confirmed_at             timestamp     NULL,
    void_reason              varchar(500)  NULL,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_color_instruction') THEN
        ALTER TABLE lunaerp_production_color_instruction ADD CONSTRAINT pk_lb_color_instruction PRIMARY KEY (id);
    END IF;
END $$;

-- 打色单号租户内全量唯一（ADR-0006）：普通唯一索引、不带 WHERE deleted=0，
-- 按天取号、软删不释放编号。
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_color_instruction_tenant_no
    ON lunaerp_production_color_instruction (tenant_id, instruction_no);
CREATE INDEX IF NOT EXISTS idx_lb_color_instruction_tenant_cust
    ON lunaerp_production_color_instruction (tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_lb_color_instruction_tenant_status
    ON lunaerp_production_color_instruction (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_lb_color_instruction_tenant_date
    ON lunaerp_production_color_instruction (tenant_id, instruction_date);

COMMENT ON TABLE lunaerp_production_color_instruction IS '生产管理-打色指示单主表';
COMMENT ON COLUMN lunaerp_production_color_instruction.id IS '打色指示单ID';
COMMENT ON COLUMN lunaerp_production_color_instruction.instruction_no IS '打色单号（DS+年月日+3位顺序号，按天取号，租户内全量唯一含已删除行，见 ADR-0006）';
COMMENT ON COLUMN lunaerp_production_color_instruction.customer_id IS '客户档案ID（lunaerp_baseinfo_customer.id，逻辑关联）';
COMMENT ON COLUMN lunaerp_production_color_instruction.client_name IS '客户名快照';
COMMENT ON COLUMN lunaerp_production_color_instruction.contact_person IS '客户联系人（文本，可含电话）';
COMMENT ON COLUMN lunaerp_production_color_instruction.product_id IS '产品档案ID（强关联）';
COMMENT ON COLUMN lunaerp_production_color_instruction.product_code IS '产品编号快照';
COMMENT ON COLUMN lunaerp_production_color_instruction.product_name IS '产品名称快照';
COMMENT ON COLUMN lunaerp_production_color_instruction.lab_factory_id IS '打色承接方供应商ID（lunaerp_baseinfo_supplier.id，空=本厂打色房自做）';
COMMENT ON COLUMN lunaerp_production_color_instruction.lab_factory_name IS '打色承接方公司名快照';
COMMENT ON COLUMN lunaerp_production_color_instruction.composition IS '成分快照';
COMMENT ON COLUMN lunaerp_production_color_instruction.width IS '门幅快照';
COMMENT ON COLUMN lunaerp_production_color_instruction.gsm IS '克重快照';
COMMENT ON COLUMN lunaerp_production_color_instruction.instruction_date IS '单据日期（必填，缺省当天）';
COMMENT ON COLUMN lunaerp_production_color_instruction.delivery_date IS '要求完成日（可空）';
COMMENT ON COLUMN lunaerp_production_color_instruction.status IS '单据状态（字典 lunaerp_color_instruction_status：PENDING 待打色 / IN_PROGRESS 打色中 / CONFIRMED 已确认 / IN_PRODUCTION 已投产 / VOID 已作废）';
COMMENT ON COLUMN lunaerp_production_color_instruction.confirm_type IS '确认类型（CUSTOMER 客户确认 / INTERNAL 内部确认）';
COMMENT ON COLUMN lunaerp_production_color_instruction.customer_feedback IS '客户反馈意见（确认或驳回时填）';
COMMENT ON COLUMN lunaerp_production_color_instruction.confirmed_by IS '确认人用户ID';
COMMENT ON COLUMN lunaerp_production_color_instruction.confirmed_at IS '确认时间';
COMMENT ON COLUMN lunaerp_production_color_instruction.void_reason IS '作废原因';
COMMENT ON COLUMN lunaerp_production_color_instruction.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_color_instruction.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_color_instruction.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_color_instruction.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_color_instruction.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_color_instruction.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_production_color_instruction.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 打色指示单颜色行子表 lunaerp_production_color_instruction_color
--    随单聚合、无独立操作界面；差异保存 + 行状态守卫；
--    客户色号与染色明细/发货明细全域同口径。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_color_instruction_color (
    id                       int8          NOT NULL,
    instruction_id           int8          NOT NULL,
    line_no                  int4          NOT NULL,
    customer_color_no        varchar(64)   NOT NULL,
    color_name               varchar(128)  NOT NULL,
    factory_color_no         varchar(64)   NULL,
    color_hex                varchar(16)   NULL,
    light_primary            varchar(32)   NOT NULL,
    light_secondary          varchar(32)   NULL,
    color_card_ref           varchar(128)  NULL,
    sample_photos            text          NULL,
    copies                   int4          NULL,
    round                    int4          NOT NULL DEFAULT 1,
    status                   varchar(16)   NOT NULL DEFAULT 'PENDING',
    reject_reason            varchar(500)  NULL,
    remark                   varchar(500)  NULL,
    creator                  varchar(64)   NULL DEFAULT '',
    create_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)   NULL DEFAULT '',
    update_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2          NOT NULL DEFAULT 0,
    tenant_id                int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_color_instruction_color') THEN
        ALTER TABLE lunaerp_production_color_instruction_color ADD CONSTRAINT pk_lb_color_instruction_color PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_color_inst_col_tenant_inst
    ON lunaerp_production_color_instruction_color (tenant_id, instruction_id);
CREATE INDEX IF NOT EXISTS idx_lb_color_inst_col_tenant_status
    ON lunaerp_production_color_instruction_color (tenant_id, status);

COMMENT ON TABLE lunaerp_production_color_instruction_color IS '生产管理-打色指示单颜色行子表';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.id IS '颜色行ID';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.instruction_id IS '打色指示单ID（lunaerp_production_color_instruction.id）';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.line_no IS '行号（服务端重排，同色号多轮相邻、最新轮在前）';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.customer_color_no IS '客户色号（与染色明细/发货明细同口径）';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.color_name IS '颜色名称';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.factory_color_no IS '厂内色号';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.color_hex IS '色值十六进制（如 #FF0000）';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.light_primary IS '主灯源（字典 lunaerp_color_light_source）';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.light_secondary IS '辅灯源（字典 lunaerp_color_light_source）';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.color_card_ref IS '色卡参考（如 Pantone TPX 19-4052）';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.sample_photos IS '样布照片 JSON 数组 [{"name":"...","url":"..."}]';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.copies IS '份数';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.round IS '打色轮次（缺省 1，驳回重打 +1）';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.status IS '行状态（字典 lunaerp_color_line_status：PENDING 待结果 / APPROVED 通过 / REJECTED 驳回）';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.reject_reason IS '驳回意见';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.remark IS '行备注';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_production_color_instruction_color.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 主键自增序列
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_color_instruction_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_color_instruction_color_seq;

-- =============================================================================
-- 4. 存量下游表追加显式引用列（胚布单、染色单）
-- =============================================================================
ALTER TABLE lunaerp_production_greige_order ADD COLUMN IF NOT EXISTS color_instruction_id int8 NULL;
ALTER TABLE lunaerp_production_greige_order ADD COLUMN IF NOT EXISTS color_instruction_no varchar(32) NULL;
COMMENT ON COLUMN lunaerp_production_greige_order.color_instruction_id IS '打色指示单ID（lunaerp_production_color_instruction.id，逻辑关联可空）';
COMMENT ON COLUMN lunaerp_production_greige_order.color_instruction_no IS '打色单号快照（可空）';

ALTER TABLE lunaerp_production_dyeing_order ADD COLUMN IF NOT EXISTS color_instruction_id int8 NULL;
ALTER TABLE lunaerp_production_dyeing_order ADD COLUMN IF NOT EXISTS color_instruction_no varchar(32) NULL;
COMMENT ON COLUMN lunaerp_production_dyeing_order.color_instruction_id IS '打色指示单ID（lunaerp_production_color_instruction.id，逻辑关联可空）';
COMMENT ON COLUMN lunaerp_production_dyeing_order.color_instruction_no IS '打色单号快照（可空）';

-- =============================================================================
-- 5. 菜单与按钮（挂 10200 生产管理目录）
--    10230: 打色指示单（type=2, sort=0，时序在胚布单 10210 之前）
--    10231-10235: 按钮位（创建/修改/删除/确认/打印）
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10230, 10231, 10232, 10233, 10234, 10235);

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10230, '打色指示单', 'lunaerp:production:color-instruction:query', 2, 0, 10200, 'color-instruction', 'ep:brush', 'lunaerp-elmui/production/color-instruction/index', 'LunaerpElmuiProductionColorInstruction', 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10231, '打色指示单创建', 'lunaerp:production:color-instruction:create', 3, 1, 10230, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10232, '打色指示单修改', 'lunaerp:production:color-instruction:update', 3, 2, 10230, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10233, '打色指示单删除', 'lunaerp:production:color-instruction:delete', 3, 3, 10230, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10234, '打色指示单确认', 'lunaerp:production:color-instruction:confirm', 3, 4, 10230, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10235, '打色指示单打印', 'lunaerp:production:color-instruction:print', 3, 5, 10230, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 授权管理员与超管角色
DELETE FROM system_role_menu WHERE menu_id IN (10230, 10231, 10232, 10233, 10234, 10235);

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (
    SELECT 10230 AS id UNION ALL SELECT 10231 UNION ALL SELECT 10232 UNION ALL
    SELECT 10233 UNION ALL SELECT 10234 UNION ALL SELECT 10235
) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2)
ON CONFLICT DO NOTHING;

COMMIT;
-- @formatter:on

-- =============================================================================
-- 6. 字典类型与字典数据（3 个字典：2255-2257，数据 3736-3749）
--    1) lunaerp_color_light_source        打色灯源（D65/TL84/CWF/A/UV/TL83）
--    2) lunaerp_color_line_status         打色单行状态（PENDING/APPROVED/REJECTED）
--    3) lunaerp_color_instruction_status  打色指示单状态（PENDING/IN_PROGRESS/CONFIRMED/IN_PRODUCTION/VOID）
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2255, 2256, 2257)
    OR type IN (
        'lunaerp_color_light_source',
        'lunaerp_color_line_status',
        'lunaerp_color_instruction_status'
    );

DELETE FROM system_dict_data WHERE dict_type IN (
        'lunaerp_color_light_source',
        'lunaerp_color_line_status',
        'lunaerp_color_instruction_status'
    )
    OR id BETWEEN 3736 AND 3749;

-- 6.1 打色灯源
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2255, '打色灯源', 'lunaerp_color_light_source', 0, 'LunaERP 生产管理-打色灯源', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3736, 1, 'D65', 'D65', 'lunaerp_color_light_source', 0, 'primary', '', '标准日光', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3737, 2, 'TL84', 'TL84', 'lunaerp_color_light_source', 0, 'success', '', '三基色荧光', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3738, 3, 'CWF', 'CWF', 'lunaerp_color_light_source', 0, 'warning', '', '冷白荧光', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3739, 4, 'A', 'A', 'lunaerp_color_light_source', 0, 'info', '', '白炽灯', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3740, 5, 'UV', 'UV', 'lunaerp_color_light_source', 0, 'danger', '', '紫外灯', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3741, 6, 'TL83', 'TL83', 'lunaerp_color_light_source', 0, 'default', '', '暖白荧光', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 6.2 打色单行状态
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2256, '打色单行状态', 'lunaerp_color_line_status', 0, 'LunaERP 生产管理-打色单行状态', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3742, 1, '待打色', 'PENDING', 'lunaerp_color_line_status', 0, 'info', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3743, 2, '通过', 'APPROVED', 'lunaerp_color_line_status', 0, 'success', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3744, 3, '驳回', 'REJECTED', 'lunaerp_color_line_status', 0, 'danger', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 6.3 打色指示单状态
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2257, '打色指示单状态', 'lunaerp_color_instruction_status', 0, 'LunaERP 生产管理-打色指示单状态', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3745, 1, '待打色', 'PENDING', 'lunaerp_color_instruction_status', 0, 'info', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3746, 2, '打色中', 'IN_PROGRESS', 'lunaerp_color_instruction_status', 0, 'warning', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3747, 3, '已确认', 'CONFIRMED', 'lunaerp_color_instruction_status', 0, 'success', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3748, 4, '已投产', 'IN_PRODUCTION', 'lunaerp_color_instruction_status', 0, 'primary', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3749, 5, '已作废', 'VOID', 'lunaerp_color_instruction_status', 0, 'danger', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

COMMIT;
-- @formatter:on
