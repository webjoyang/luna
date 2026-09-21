-- =============================================================================
-- 模块：LunaERP - 生产管理 - 检验单（主表与录检明细子表）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 检验单主表 lunaerp_production_inspection_order
--    单号 INS-YYYYMMDD-NNN 带连字符分隔、按天取号、租户内全量唯一含软删行（ADR-0015）；
--    承检方引用供应商档案并保存公司名快照；产品/胚布单/染色单/销售合同为逻辑关联，不建数据库外键。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_inspection_order (
    id                       int8          NOT NULL,
    code                     varchar(32)   NOT NULL,
    type                     varchar(16)   NOT NULL,
    status                   varchar(16)   NOT NULL DEFAULT 'DRAFT',
    product_id               int8          NOT NULL,
    product_code             varchar(64)   NULL,
    product_name             varchar(128)  NULL,
    greige_order_id          int8          NULL,
    dyeing_order_id          int8          NULL,
    inspection_factory_id    int8          NULL,
    inspection_factory_name  varchar(128)  NULL,
    sales_contract_id        int8          NULL,
    contract_no              varchar(32)   NULL,
    target_weight            numeric(18,2) NULL,
    remark                   varchar(1000) NULL,
    order_date               date          NOT NULL,
    creator                  varchar(64)   NULL DEFAULT '',
    create_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)   NULL DEFAULT '',
    update_time              timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2          NOT NULL DEFAULT 0,
    tenant_id                int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_inspection_order') THEN
        ALTER TABLE lunaerp_production_inspection_order ADD CONSTRAINT pk_lb_inspection_order PRIMARY KEY (id);
    END IF;
END $$;

-- 检验单号租户内全量唯一（ADR-0015，仿 ADR-0005/ADR-0006）：普通唯一索引、不带 WHERE deleted=0，
-- 按天取号、软删不释放编号。勿"修正"为部分唯一索引。
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_inspection_order_tenant_code
    ON lunaerp_production_inspection_order (tenant_id, code);
CREATE INDEX IF NOT EXISTS idx_lb_inspection_order_status
    ON lunaerp_production_inspection_order (status);
CREATE INDEX IF NOT EXISTS idx_lb_inspection_order_order_date
    ON lunaerp_production_inspection_order (order_date);
CREATE INDEX IF NOT EXISTS idx_lb_inspection_order_product_id
    ON lunaerp_production_inspection_order (product_id);
CREATE INDEX IF NOT EXISTS idx_lb_inspection_order_inspection_factory_id
    ON lunaerp_production_inspection_order (inspection_factory_id);

COMMENT ON TABLE lunaerp_production_inspection_order IS '生产管理-检验单主表';
COMMENT ON COLUMN lunaerp_production_inspection_order.id IS '检验单ID';
COMMENT ON COLUMN lunaerp_production_inspection_order.code IS '检验单号（INS-YYYYMMDD-NNN，按天取号，租户内全量唯一含软删行，ADR-0015）';
COMMENT ON COLUMN lunaerp_production_inspection_order.type IS '检验类型（GREIGE 胚布检验 / FINISHED 成品检验）';
COMMENT ON COLUMN lunaerp_production_inspection_order.status IS '检验单状态（字典 lunaerp_inspection_order_status：DRAFT/ACTIVE/COMPLETED/CANCELLED），创建缺省 DRAFT';
COMMENT ON COLUMN lunaerp_production_inspection_order.product_id IS '关联产品ID（lunaerp_baseinfo_product.id，必填）';
COMMENT ON COLUMN lunaerp_production_inspection_order.product_code IS '产品编码快照';
COMMENT ON COLUMN lunaerp_production_inspection_order.product_name IS '产品名称快照';
COMMENT ON COLUMN lunaerp_production_inspection_order.greige_order_id IS '关联胚布单ID（lunaerp_production_greige_order.id，type=GREIGE 时可选）';
COMMENT ON COLUMN lunaerp_production_inspection_order.dyeing_order_id IS '关联染色单ID（lunaerp_production_dyeing_order.id，type=FINISHED 时可选）';
COMMENT ON COLUMN lunaerp_production_inspection_order.inspection_factory_id IS '承检方供应商ID（lunaerp_baseinfo_supplier.id，可选）';
COMMENT ON COLUMN lunaerp_production_inspection_order.inspection_factory_name IS '承检方公司名快照';
COMMENT ON COLUMN lunaerp_production_inspection_order.sales_contract_id IS '关联销售合同ID（lunaerp_business_sales_contract.id，可选）';
COMMENT ON COLUMN lunaerp_production_inspection_order.contract_no IS '合同号快照';
COMMENT ON COLUMN lunaerp_production_inspection_order.target_weight IS '目标重量 KG';
COMMENT ON COLUMN lunaerp_production_inspection_order.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_inspection_order.order_date IS '单据日期（必填，创建缺省当天）';
COMMENT ON COLUMN lunaerp_production_inspection_order.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_inspection_order.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_inspection_order.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_inspection_order.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_inspection_order.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_production_inspection_order.tenant_id IS '租户编号';

-- =============================================================================
-- 1b. 录检明细表 lunaerp_production_inspection_order_item
--     录检明细作为独立子资源处理（ADR-0014）：有自己的 Controller/Service/Mapper/DO，
--     逐卷增删改，不随主表整体保存替换；删主表时级联逻辑删除明细。
--     (vat_no, roll_no) 唯一性由 service 层校验，不建 DB 唯一约束；本期不建 defects 字段。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_inspection_order_item (
    id                       int8          NOT NULL,
    inspection_order_id      int8          NOT NULL,
    roll_no                  varchar(64)   NOT NULL,
    vat_no                   varchar(64)   NOT NULL,
    measured_weight          numeric(18,2) NULL,
    length_m                 numeric(18,2) NULL,
    machine_no               varchar(64)   NULL,
    grade                    varchar(16)   NULL,
    color_name               varchar(128)  NULL,
    color_no                 varchar(64)   NULL,
    style_no                 varchar(64)   NULL,
    product_code             varchar(64)   NULL,
    product_name             varchar(128)  NULL,
    piece_count              int4          NULL DEFAULT 1,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_inspection_order_item') THEN
        ALTER TABLE lunaerp_production_inspection_order_item ADD CONSTRAINT pk_lb_inspection_order_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_inspection_order_item_order_id
    ON lunaerp_production_inspection_order_item (inspection_order_id);
CREATE INDEX IF NOT EXISTS idx_lb_inspection_order_item_vat_roll
    ON lunaerp_production_inspection_order_item (vat_no, roll_no);

COMMENT ON TABLE lunaerp_production_inspection_order_item IS '生产管理-检验单录检明细表';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.id IS '明细ID';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.inspection_order_id IS '检验单ID（lunaerp_production_inspection_order.id，必填）';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.roll_no IS '卷号（必填）';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.vat_no IS '缸号（必填）';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.measured_weight IS '实称重量 KG';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.length_m IS '米数';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.machine_no IS '机台号';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.grade IS '等级（字典 lunaerp_inspection_grade：A/B/C/DEFECTIVE）';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.color_name IS '颜色名称';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.color_no IS '产品色号';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.style_no IS '款号';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.product_code IS '产品编码快照（为后续同步预留）';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.product_name IS '产品名称快照（为后续同步预留）';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.piece_count IS '匹数（缺省 1）';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_production_inspection_order_item.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_inspection_order_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_inspection_order_item_seq;

-- =============================================================================
-- 3. 菜单与按钮（system_menu，可重复执行）
--    10200 生产管理目录下挂载 10260 检验单菜单（承载 query 权限）。
--    10261-10263 为主表创建/修改/删除按钮；10264 为同步入库按钮（预留占位，前端默认禁用，ADR-0013）；
--    10265-10268 为录检明细 query/create/update/delete 按钮（仿供应商联系人 10060-10063 同构）。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10260, 10261, 10262, 10263, 10264, 10265, 10266, 10267, 10268);

-- 菜单：检验单（query 权限挂在菜单本身，参照胚布单/染色单模式）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10260, '检验单', 'lunaerp:production:inspection-order:query', 2, 4, 10200, 'inspection-order', 'ep:document-checked', 'lunaerp-elmui/production/inspection-order/index', 'LunaerpElmuiProductionInspectionOrder', 0, '1', '1', '1', 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
-- 按钮：检验单主表创建/修改/删除
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10261, '检验单创建', 'lunaerp:production:inspection-order:create', 3, 1, 10260, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10262, '检验单修改', 'lunaerp:production:inspection-order:update', 3, 2, 10260, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10263, '检验单删除', 'lunaerp:production:inspection-order:delete', 3, 3, 10260, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
-- 按钮：检验单同步入库（预留占位，前端默认禁用，ADR-0013）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10264, '检验单同步入库', 'lunaerp:production:inspection-order:sync', 3, 4, 10260, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
-- 按钮：录检明细 query/create/update/delete（type=3 直接挂在 10260 菜单下，仿供应商联系人 10060-10063 同构）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10265, '录检明细', 'lunaerp:production:inspection-order-item:query', 3, 10, 10260, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10266, '录检明细创建', 'lunaerp:production:inspection-order-item:create', 3, 11, 10260, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10267, '录检明细修改', 'lunaerp:production:inspection-order-item:update', 3, 12, 10260, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10268, '录检明细删除', 'lunaerp:production:inspection-order-item:delete', 3, 13, 10260, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 4. 字典类型与字典数据（system_dict_type / system_dict_data，可重复执行）
--    dict_type id 2231（状态）、2232（等级），dict_data id 3584-3587（状态）、3588-3591（等级）。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2231, 2232);
DELETE FROM system_dict_data WHERE dict_type IN ('lunaerp_inspection_order_status', 'lunaerp_inspection_grade');

-- 字典类型：检验单状态
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2231, '检验单状态', 'lunaerp_inspection_order_status', 0, 'LunaERP 生产管理-检验单状态', 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0', NULL);
-- 检验单状态：草稿/录检中/录检完成/已取消（四态，仿染色单/纱线采购单四态）
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3584, 1, '草稿', 'DRAFT', 'lunaerp_inspection_order_status', 0, 'warning', '', NULL, 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3585, 2, '录检中', 'ACTIVE', 'lunaerp_inspection_order_status', 0, 'success', '', NULL, 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3586, 3, '录检完成', 'COMPLETED', 'lunaerp_inspection_order_status', 0, 'info', '', NULL, 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3587, 4, '已取消', 'CANCELLED', 'lunaerp_inspection_order_status', 0, 'danger', '', NULL, 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');

-- 字典类型：检验等级
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2232, '检验等级', 'lunaerp_inspection_grade', 0, 'LunaERP 生产管理-检验等级', 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0', NULL);
-- 检验等级：一等品/二等品/三等品/等外品
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3588, 1, '一等品', 'A', 'lunaerp_inspection_grade', 0, 'success', '', NULL, 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3589, 2, '二等品', 'B', 'lunaerp_inspection_grade', 0, 'info', '', NULL, 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3590, 3, '三等品', 'C', 'lunaerp_inspection_grade', 0, 'warning', '', NULL, 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3591, 4, '等外品', 'DEFECTIVE', 'lunaerp_inspection_grade', 0, 'danger', '', NULL, 'admin', '2026-09-21 00:00:00', '1', '2026-09-21 00:00:00', '0');

COMMIT;
-- @formatter:on
