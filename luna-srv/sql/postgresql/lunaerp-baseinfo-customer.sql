-- =============================================================================
-- 模块：LunaERP - 基本信息 - 客户信息
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键与存量列迁移（company→company_name）用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 客户表 lunaerp_baseinfo_customer
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_baseinfo_customer (
    id                       int8         NOT NULL,
    company_name             varchar      NOT NULL,
    short_name               varchar      NULL,
    phone                    varchar      NULL,
    address                  varchar      NULL,
    client_type              varchar      NOT NULL,
    industry                 varchar      NULL,
    scale                    varchar      NULL,
    source                   varchar      NULL,
    cooperation_status       varchar      NULL,
    unified_social_credit_code varchar     NULL,
    remark                   varchar      NULL,
    status                   varchar      NULL DEFAULT 'ACTIVE',
    creator                  varchar(64)  NULL DEFAULT '',
    create_time              timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                  varchar(64)  NULL DEFAULT '',
    update_time              timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                  int2         NOT NULL DEFAULT 0,
    tenant_id                 int8        NOT NULL DEFAULT 0
);

-- 存量库升级：旧列 company 正名为 company_name（可重复执行）。
-- 判存 information_schema：旧列存在且新列不存在时才 RENAME，空库新表与已升级库重跑均跳过；
-- RENAME 仅改列名，NOT NULL 等约束与数据随列保留，不丢数据。
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'lunaerp_baseinfo_customer'
          AND column_name = 'company'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'lunaerp_baseinfo_customer'
          AND column_name = 'company_name'
    ) THEN
        ALTER TABLE lunaerp_baseinfo_customer RENAME COLUMN company TO company_name;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_customer') THEN
        ALTER TABLE lunaerp_baseinfo_customer ADD CONSTRAINT pk_lb_customer PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_customer_status
    ON lunaerp_baseinfo_customer (status);
CREATE INDEX IF NOT EXISTS idx_lb_customer_client_type
    ON lunaerp_baseinfo_customer (client_type);
CREATE INDEX IF NOT EXISTS idx_lb_customer_cooperation_status
    ON lunaerp_baseinfo_customer (cooperation_status);

COMMENT ON TABLE lunaerp_baseinfo_customer IS '基本信息-客户表';
COMMENT ON COLUMN lunaerp_baseinfo_customer.id IS '客户ID';
COMMENT ON COLUMN lunaerp_baseinfo_customer.company_name IS '公司名称（必填）';
COMMENT ON COLUMN lunaerp_baseinfo_customer.short_name IS '简称（为空时后端用 company_name 回填）';
COMMENT ON COLUMN lunaerp_baseinfo_customer.phone IS '联系电话';
COMMENT ON COLUMN lunaerp_baseinfo_customer.address IS '公司地址';
COMMENT ON COLUMN lunaerp_baseinfo_customer.client_type IS '客户类型（字典 lunaerp_customer_client_type：潜在/正式/流失）';
COMMENT ON COLUMN lunaerp_baseinfo_customer.industry IS '所属行业（字典 lunaerp_customer_industry）';
COMMENT ON COLUMN lunaerp_baseinfo_customer.scale IS '企业规模（字典 lunaerp_customer_scale）';
COMMENT ON COLUMN lunaerp_baseinfo_customer.source IS '客户来源（字典 lunaerp_customer_source）';
COMMENT ON COLUMN lunaerp_baseinfo_customer.cooperation_status IS '合作状态（字典 lunaerp_customer_cooperation_status）';
COMMENT ON COLUMN lunaerp_baseinfo_customer.unified_social_credit_code IS '统一社会信用代码';
COMMENT ON COLUMN lunaerp_baseinfo_customer.remark IS '备注';
COMMENT ON COLUMN lunaerp_baseinfo_customer.status IS '状态（ACTIVE 启用 / INACTIVE 停用），创建缺省 ACTIVE';
COMMENT ON COLUMN lunaerp_baseinfo_customer.creator IS '创建者';
COMMENT ON COLUMN lunaerp_baseinfo_customer.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_baseinfo_customer.updater IS '更新者';
COMMENT ON COLUMN lunaerp_baseinfo_customer.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_baseinfo_customer.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_baseinfo_customer.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_baseinfo_customer_seq;

-- =============================================================================
-- 3. 菜单与按钮（system_menu，可重复执行）
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10030, 10031, 10032, 10033);

-- 菜单：客户信息（parent_id=10000 基本信息）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10030, '客户信息', 'lunaerp:baseinfo:customer:query', 2, 3, 10000, 'customer', 'ep:office-building', 'lunaerp-elmui/baseinfo/customer/index', 'LunaerpElmuiBaseinfoCustomer', 0, '1', '1', '1', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
-- 按钮：客户创建/修改/删除（参照产品菜单模式：query 权限挂在菜单本身，按钮只放 create/update/delete）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10031, '客户创建', 'lunaerp:baseinfo:customer:create', 3, 1, 10030, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10032, '客户修改', 'lunaerp:baseinfo:customer:update', 3, 2, 10030, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10033, '客户删除', 'lunaerp:baseinfo:customer:delete', 3, 3, 10030, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 4. 字典类型与字典数据（system_dict_type / system_dict_data，可重复执行）
--    字段语义参照 oldweb 客户档案下拉项；颜色按状态语义对齐 ruoyi 习惯（success/info/warning/danger）
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2200, 2201, 2202, 2203, 2204);
DELETE FROM system_dict_data WHERE dict_type IN (
    'lunaerp_customer_client_type',
    'lunaerp_customer_industry',
    'lunaerp_customer_scale',
    'lunaerp_customer_source',
    'lunaerp_customer_cooperation_status'
);

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2200, '客户类型', 'lunaerp_customer_client_type', 0, 'LunaERP 客户档案-客户类型', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0', NULL);
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2201, '客户所属行业', 'lunaerp_customer_industry', 0, 'LunaERP 客户档案-所属行业', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0', NULL);
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2202, '客户企业规模', 'lunaerp_customer_scale', 0, 'LunaERP 客户档案-企业规模', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0', NULL);
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2203, '客户来源', 'lunaerp_customer_source', 0, 'LunaERP 客户档案-客户来源', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0', NULL);
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2204, '客户合作状态', 'lunaerp_customer_cooperation_status', 0, 'LunaERP 客户档案-合作状态', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0', NULL);

-- 客户类型：潜在/正式/流失
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3500, 1, '潜在', 'POTENTIAL', 'lunaerp_customer_client_type', 0, 'info', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3501, 2, '正式', 'FORMAL', 'lunaerp_customer_client_type', 0, 'success', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3502, 3, '流失', 'CHURNED', 'lunaerp_customer_client_type', 0, 'danger', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');

-- 所属行业：纺织/服装/贸易/电商
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3503, 1, '纺织', 'TEXTILE', 'lunaerp_customer_industry', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3504, 2, '服装', 'GARMENT', 'lunaerp_customer_industry', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3505, 3, '贸易', 'TRADE', 'lunaerp_customer_industry', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3506, 4, '电商', 'ECOMMERCE', 'lunaerp_customer_industry', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');

-- 企业规模：大型/中型/小型/微型
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3507, 1, '大型', 'LARGE', 'lunaerp_customer_scale', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3508, 2, '中型', 'MEDIUM', 'lunaerp_customer_scale', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3509, 3, '小型', 'SMALL', 'lunaerp_customer_scale', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3510, 4, '微型', 'MICRO', 'lunaerp_customer_scale', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');

-- 客户来源：主动开发/客户转介绍/展会/线上咨询
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3511, 1, '主动开发', 'SELF_DEV', 'lunaerp_customer_source', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3512, 2, '客户转介绍', 'REFERRAL', 'lunaerp_customer_source', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3513, 3, '展会', 'EXHIBITION', 'lunaerp_customer_source', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3514, 4, '线上咨询', 'ONLINE', 'lunaerp_customer_source', 0, '', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');

-- 合作状态：未合作/合作中/已暂停/已结束
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3515, 1, '未合作', 'NONE', 'lunaerp_customer_cooperation_status', 0, 'info', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3516, 2, '合作中', 'COOPERATING', 'lunaerp_customer_cooperation_status', 0, 'success', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3517, 3, '已暂停', 'PAUSED', 'lunaerp_customer_cooperation_status', 0, 'warning', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3518, 4, '已结束', 'ENDED', 'lunaerp_customer_cooperation_status', 0, 'danger', '', NULL, 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 5. 客户联系人表 lunaerp_baseinfo_customer_contact
--    客户的独立子资源：customer_id 直接 int8（无历史包袱，不学 product-image 的 String productId）。
--    is_primary 业务允许同客户多条主联系人（不建唯一约束，由前端管理）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_baseinfo_customer_contact (
    id           int8         NOT NULL,
    customer_id  int8         NOT NULL,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_customer_contact') THEN
        ALTER TABLE lunaerp_baseinfo_customer_contact ADD CONSTRAINT pk_lb_customer_contact PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_customer_contact_customer
    ON lunaerp_baseinfo_customer_contact (customer_id);

COMMENT ON TABLE lunaerp_baseinfo_customer_contact IS '基本信息-客户联系人表';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.id IS '联系人ID';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.customer_id IS '客户ID（lunaerp_baseinfo_customer.id）';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.name IS '联系人姓名（必填）';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.position IS '职位';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.phone IS '联系电话';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.email IS '邮箱';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.remark IS '备注';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.is_primary IS '是否主联系人（1 是 / 0 否），业务允许同客户多条主联系人';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.creator IS '创建者';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.updater IS '更新者';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_baseinfo_customer_contact.tenant_id IS '租户编号';

-- 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
CREATE SEQUENCE IF NOT EXISTS lunaerp_baseinfo_customer_contact_seq;

-- =============================================================================
-- 6. 联系人菜单与按钮（system_menu，可重复执行）
--    10040 为隐藏菜单（type=2、visible='0'、无 component），仅作权限节点承载 query 权限；
--    10041-10043 为 create/update/delete 按钮节点。导航树不显示，角色管理可分配。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10040, 10041, 10042, 10043);

-- 隐藏权限节点：客户联系人（parent_id=10030 客户信息，无 component 不生成路由）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10040, '客户联系人', 'lunaerp:baseinfo:customer-contact:query', 2, 1, 10030, 'customer-contact', '#', '', NULL, 0, '0', '1', '1', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
-- 按钮：联系人创建/修改/删除（query 权限挂在 10040 节点本身，与客户菜单模式一致）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10041, '客户联系人创建', 'lunaerp:baseinfo:customer-contact:create', 3, 1, 10040, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10042, '客户联系人修改', 'lunaerp:baseinfo:customer-contact:update', 3, 2, 10040, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10043, '客户联系人删除', 'lunaerp:baseinfo:customer-contact:delete', 3, 3, 10040, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-18 00:00:00', '1', '2026-09-18 00:00:00', '0');

COMMIT;
-- @formatter:on
