-- =============================================================================
-- 模块：LunaERP - 基本信息 - 供应商信息
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 供应商表 lunaerp_baseinfo_supplier
--    supplier_no 全量唯一（含软删行，ADR-0005）：UNIQUE (tenant_id, supplier_no) 不带 deleted 条件。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_baseinfo_supplier (
    id              int8         NOT NULL,
    supplier_no     varchar      NOT NULL,
    company_name    varchar      NOT NULL,
    short_name      varchar      NULL,
    phone           varchar      NULL,
    address         varchar      NULL,
    supplier_type   varchar      NULL,
    machine_type    varchar      NULL,
    remark          varchar      NULL,
    status          varchar      NULL DEFAULT 'ACTIVE',
    creator         varchar(64)  NULL DEFAULT '',
    create_time     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater         varchar(64)  NULL DEFAULT '',
    update_time     timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         int2         NOT NULL DEFAULT 0,
    tenant_id       int8         NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_supplier') THEN
        ALTER TABLE lunaerp_baseinfo_supplier ADD CONSTRAINT pk_lb_supplier PRIMARY KEY (id);
    END IF;
END $$;

-- 供应商编号租户内全量唯一（含已逻辑删除行，软删不释放编号，ADR-0005）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_supplier_tenant_no
    ON lunaerp_baseinfo_supplier (tenant_id, supplier_no);
CREATE INDEX IF NOT EXISTS idx_lb_supplier_status
    ON lunaerp_baseinfo_supplier (status);
CREATE INDEX IF NOT EXISTS idx_lb_supplier_type
    ON lunaerp_baseinfo_supplier (supplier_type);

COMMENT ON TABLE lunaerp_baseinfo_supplier IS '基本信息-供应商表';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.id IS '供应商ID';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.supplier_no IS '供应商编号（SUP+年份+4位序号，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.company_name IS '公司/档口名称（必填）';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.short_name IS '简称（为空时后端用 company_name 回填）';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.phone IS '联系电话';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.address IS '公司地址';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.supplier_type IS '供应商类型（字典 lunaerp_supplier_type，可空）';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.machine_type IS '主营机型/设备说明（自由文本）';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.remark IS '备注';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.status IS '状态（ACTIVE 启用 / INACTIVE 停用），创建缺省 ACTIVE';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.creator IS '创建者';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.updater IS '更新者';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_baseinfo_supplier.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_baseinfo_supplier_seq;

-- =============================================================================
-- 3. 菜单与按钮（system_menu，可重复执行）
--    10050 供应商信息菜单（parent_id=10000 基本信息）；10051-10053 创建/修改/删除按钮。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10050, 10051, 10052, 10053);

-- 菜单：供应商信息（parent_id=10000 基本信息，sort 取客户之后）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10050, '供应商信息', 'lunaerp:baseinfo:supplier:query', 2, 4, 10000, 'supplier', 'ep:shop', 'lunaerp-elmui/baseinfo/supplier/index', 'LunaerpElmuiBaseinfoSupplier', 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
-- 按钮：供应商创建/修改/删除（query 权限挂在菜单本身，按钮只放 create/update/delete）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10051, '供应商创建', 'lunaerp:baseinfo:supplier:create', 3, 1, 10050, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10052, '供应商修改', 'lunaerp:baseinfo:supplier:update', 3, 2, 10050, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10053, '供应商删除', 'lunaerp:baseinfo:supplier:delete', 3, 3, 10050, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 4. 供应商类型字典（system_dict_type / system_dict_data，可重复执行）
--    18 项经营类别（value 编码与 spec 一致），颜色按语义对齐 ruoyi 习惯。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id = 2220;
DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_supplier_type';

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2220, '供应商类型', 'lunaerp_supplier_type', 0, 'LunaERP 供应商档案-供应商类型（18 项经营类别）', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0', NULL);

-- 18 项供应商类型
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3550, 1, '纱线厂', 'YARN_MILL', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3551, 2, '坯布厂', 'GREIGE_MILL', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3552, 3, '工贸一体', 'INTEGRATED', 'lunaerp_supplier_type', 0, 'success', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3553, 4, '绣花厂', 'EMBROIDERY_MILL', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3554, 5, '染厂', 'DYEING_MILL', 'lunaerp_supplier_type', 0, 'warning', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3555, 6, '扎染', 'TIE_DYEING', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3556, 7, '印花厂', 'PRINTING_MILL', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3557, 8, '烫金厂', 'HOT_STAMPING_MILL', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3558, 9, '压花厂', 'EMBOSSING_MILL', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3559, 10, '复合厂', 'LAMINATING_MILL', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3560, 11, '外贸公司', 'FOREIGN_TRADE', 'lunaerp_supplier_type', 0, 'info', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3561, 12, '检验仓库', 'INSPECTION_WAREHOUSE', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3562, 13, '拉毛摇粒厂', 'BRUSHING_MILL', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3563, 14, '水洗厂', 'WASHING_MILL', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3564, 15, '小缸厂', 'SMALL_VAT_MILL', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3565, 16, '门市部', 'STORE', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3566, 17, '检测中心', 'TESTING_CENTER', 'lunaerp_supplier_type', 0, '', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3567, 18, '货运', 'FREIGHT', 'lunaerp_supplier_type', 0, 'danger', '', NULL, 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 5. 供应商联系人表 lunaerp_baseinfo_supplier_contact
--    供应商的独立子资源：supplier_id 直接 int8。
--    is_primary 业务允许同供应商多条主联系人（不建唯一约束，由前端管理）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_baseinfo_supplier_contact (
    id           int8         NOT NULL,
    supplier_id  int8         NOT NULL,
    name         varchar      NOT NULL,
    position     varchar      NULL,
    phone        varchar      NULL,
    email        varchar      NULL,
    remark       varchar      NULL,
    is_primary   int2         NOT NULL DEFAULT 0,
    creator      varchar(64)  NULL DEFAULT '',
    create_time  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater      varchar(64)  NULL DEFAULT '',
    update_time  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      int2         NOT NULL DEFAULT 0,
    tenant_id    int8         NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_supplier_contact') THEN
        ALTER TABLE lunaerp_baseinfo_supplier_contact ADD CONSTRAINT pk_lb_supplier_contact PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_supplier_contact_supplier
    ON lunaerp_baseinfo_supplier_contact (supplier_id);

COMMENT ON TABLE lunaerp_baseinfo_supplier_contact IS '基本信息-供应商联系人表';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.id IS '联系人ID';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.supplier_id IS '供应商ID（lunaerp_baseinfo_supplier.id）';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.name IS '联系人姓名（必填）';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.position IS '职位';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.phone IS '联系电话';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.email IS '邮箱';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.remark IS '备注';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.is_primary IS '是否主联系人（1 是 / 0 否），业务允许同供应商多条主联系人';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.creator IS '创建者';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.updater IS '更新者';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_baseinfo_supplier_contact.tenant_id IS '租户编号';

-- 主键序列
CREATE SEQUENCE IF NOT EXISTS lunaerp_baseinfo_supplier_contact_seq;

-- =============================================================================
-- 6. 联系人菜单与按钮（system_menu，可重复执行）
--    10060 为隐藏菜单（type=2、visible='0'、无 component），仅作权限节点承载 query 权限；
--    10061-10063 为 create/update/delete 按钮节点。导航树不显示，角色管理可分配。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10060, 10061, 10062, 10063);

-- 隐藏权限节点：供应商联系人（parent_id=10050 供应商信息，无 component 不生成路由）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10060, '供应商联系人', 'lunaerp:baseinfo:supplier-contact:query', 2, 1, 10050, 'supplier-contact', '#', '', NULL, 0, '0', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
-- 按钮：联系人创建/修改/删除（query 权限挂在 10060 节点本身，与供应商菜单模式一致）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10061, '供应商联系人创建', 'lunaerp:baseinfo:supplier-contact:create', 3, 1, 10060, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10062, '供应商联系人修改', 'lunaerp:baseinfo:supplier-contact:update', 3, 2, 10060, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10063, '供应商联系人删除', 'lunaerp:baseinfo:supplier-contact:delete', 3, 3, 10060, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-19 00:00:00', '1', '2026-09-19 00:00:00', '0');

COMMIT;
-- @formatter:on
