-- =============================================================================
-- 模块：LunaERP - 基本信息 - 产品信息（产品 / 产品图片 / 产品标签 / 标签关联 / 胚布品种）
-- 数据库：PostgreSQL
-- 来源：oldserver/prisma/schema.prisma
--       Product(899-1004) / ProductTag(1008-1024) / ProductTagOnProduct(1027-1041)
--       GreigeType(1047-1081) / ProductImage(1108-1117)
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 产品表 lunaerp_baseinfo_product
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_baseinfo_product (
    id                    int8         NOT NULL,
    code                  varchar      NOT NULL,
    normalized_code       varchar      NULL,
    name                  varchar      NOT NULL,
    type                  varchar      NOT NULL,
    composition           varchar      NOT NULL,
    width_cm              float8       NOT NULL,
    weight_gsm            float8       NOT NULL,
    cover_image_url       varchar      NULL,
    supplier_id           varchar      NULL,
    status                varchar      NULL DEFAULT 'ACTIVE',
    is_public             int2         NULL DEFAULT 0,
    is_featured           int2         NULL DEFAULT 0,
    featured_order        int4         NULL DEFAULT 0,
    yarn_specs            text         NULL,
    machine_type          varchar      NULL,
    output                varchar      NULL,
    remark                varchar      NULL,
    supplier_code         varchar      NULL,
    temp_code             varchar      NULL,
    craft_type            varchar      NULL,
    color                 varchar      NULL,
    greige_type_id        varchar      NULL,
    pattern_no            varchar      NULL,
    standard_length       varchar      NULL,
    conversion_rate       float8       NULL,
    cost_items            text         NULL,
    cost_price            float8       NULL,
    cost_status           varchar      NULL DEFAULT 'MISSING',
    cost_updated_at       timestamp    NULL,
    cost_updated_by       varchar      NULL,
    ai_design_url         varchar      NULL,
    ai_design_category    varchar      NULL,
    ai_design_description varchar      NULL,
    capture_draft_id      varchar      NULL,
    business_ownership    varchar      NULL,
    created_by_id         varchar      NULL,
    updated_by_id         varchar      NULL,
    creator               varchar(64)  NULL DEFAULT '',
    create_time           timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater               varchar(64)  NULL DEFAULT '',
    update_time           timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted               int2         NOT NULL DEFAULT 0,
    tenant_id             int8         NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_product') THEN
        ALTER TABLE lunaerp_baseinfo_product ADD CONSTRAINT pk_lb_product PRIMARY KEY (id);
    END IF;
END $$;

-- 租户内产品编号唯一（仅未删除行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_product_tenant_code
    ON lunaerp_baseinfo_product (tenant_id, code) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lb_product_status
    ON lunaerp_baseinfo_product (status);
CREATE INDEX IF NOT EXISTS idx_lb_product_type
    ON lunaerp_baseinfo_product (type);
CREATE INDEX IF NOT EXISTS idx_lb_product_greige_type_id
    ON lunaerp_baseinfo_product (greige_type_id);

COMMENT ON TABLE lunaerp_baseinfo_product IS '基本信息-产品表';
COMMENT ON COLUMN lunaerp_baseinfo_product.id IS '产品ID';
COMMENT ON COLUMN lunaerp_baseinfo_product.code IS '产品编号（租户级唯一）';
COMMENT ON COLUMN lunaerp_baseinfo_product.normalized_code IS '产品编号规范值（全角转半角/去噪/大写，供拍照识别精确匹配）';
COMMENT ON COLUMN lunaerp_baseinfo_product.name IS '产品名称';
COMMENT ON COLUMN lunaerp_baseinfo_product.type IS '产品类型';
COMMENT ON COLUMN lunaerp_baseinfo_product.composition IS '成分（如 NR / TR / CVC）';
COMMENT ON COLUMN lunaerp_baseinfo_product.width_cm IS '成品门幅（厘米）';
COMMENT ON COLUMN lunaerp_baseinfo_product.weight_gsm IS '成品克重（g/m²）';
COMMENT ON COLUMN lunaerp_baseinfo_product.cover_image_url IS '封面图片URL';
COMMENT ON COLUMN lunaerp_baseinfo_product.supplier_id IS '供应商ID（历史 UUID 字符串）';
COMMENT ON COLUMN lunaerp_baseinfo_product.status IS '状态（ACTIVE 在售 / DRAFT 草稿 / ARCHIVED 归档 / DELETED 已删除）';
COMMENT ON COLUMN lunaerp_baseinfo_product.is_public IS '是否官网公开（0 否 1 是）';
COMMENT ON COLUMN lunaerp_baseinfo_product.is_featured IS '是否首页推荐（0 否 1 是）';
COMMENT ON COLUMN lunaerp_baseinfo_product.featured_order IS '首页推荐排序';
COMMENT ON COLUMN lunaerp_baseinfo_product.yarn_specs IS '纱支明细（JSON 字符串数组：纱支/纺法/比例/备注/单价）';
COMMENT ON COLUMN lunaerp_baseinfo_product.machine_type IS '机型';
COMMENT ON COLUMN lunaerp_baseinfo_product.output IS '产量';
COMMENT ON COLUMN lunaerp_baseinfo_product.remark IS '备注';
COMMENT ON COLUMN lunaerp_baseinfo_product.supplier_code IS '原厂编号';
COMMENT ON COLUMN lunaerp_baseinfo_product.temp_code IS '临时编号';
COMMENT ON COLUMN lunaerp_baseinfo_product.craft_type IS '工艺类型';
COMMENT ON COLUMN lunaerp_baseinfo_product.color IS '颜色';
COMMENT ON COLUMN lunaerp_baseinfo_product.greige_type_id IS '所属胚布品种ID（历史 UUID 字符串，可空）';
COMMENT ON COLUMN lunaerp_baseinfo_product.pattern_no IS '花型号';
COMMENT ON COLUMN lunaerp_baseinfo_product.standard_length IS '标准米长';
COMMENT ON COLUMN lunaerp_baseinfo_product.conversion_rate IS '算长系数（1KG = ?米/码）';
COMMENT ON COLUMN lunaerp_baseinfo_product.cost_items IS '成本项明细（JSON 字符串数组：供应商/工序/单价/损耗率）';
COMMENT ON COLUMN lunaerp_baseinfo_product.cost_price IS '汇总成本价（由成本项计算，冗余存储）';
COMMENT ON COLUMN lunaerp_baseinfo_product.cost_status IS '成本状态（MISSING 缺失 / FILLED 已填写）';
COMMENT ON COLUMN lunaerp_baseinfo_product.cost_updated_at IS '成本最后更新时间';
COMMENT ON COLUMN lunaerp_baseinfo_product.cost_updated_by IS '成本最后更新人ID';
COMMENT ON COLUMN lunaerp_baseinfo_product.ai_design_url IS 'AI 设计图URL';
COMMENT ON COLUMN lunaerp_baseinfo_product.ai_design_category IS 'AI 设计分类（如 CASUAL BLAZER）';
COMMENT ON COLUMN lunaerp_baseinfo_product.ai_design_description IS 'AI 设计描述（推理说明/风格关键词）';
COMMENT ON COLUMN lunaerp_baseinfo_product.capture_draft_id IS '手机拍布建档草稿ID（幂等键，手工建档为空）';
COMMENT ON COLUMN lunaerp_baseinfo_product.business_ownership IS '事业部归属（TRADE 贸易 / FACTORY 工厂）';
COMMENT ON COLUMN lunaerp_baseinfo_product.created_by_id IS '创建人ID';
COMMENT ON COLUMN lunaerp_baseinfo_product.updated_by_id IS '更新人ID';
COMMENT ON COLUMN lunaerp_baseinfo_product.creator IS '创建者';
COMMENT ON COLUMN lunaerp_baseinfo_product.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_baseinfo_product.updater IS '更新者';
COMMENT ON COLUMN lunaerp_baseinfo_product.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_baseinfo_product.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_baseinfo_product.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 产品图片表 lunaerp_baseinfo_product_image
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_baseinfo_product_image (
    id          int8         NOT NULL,
    product_id  varchar      NOT NULL,
    url         varchar      NOT NULL,
    sort_order  int4         NULL DEFAULT 0,
    creator     varchar(64)  NULL DEFAULT '',
    create_time timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     varchar(64)  NULL DEFAULT '',
    update_time timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     int2         NOT NULL DEFAULT 0,
    tenant_id   int8         NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_product_image') THEN
        ALTER TABLE lunaerp_baseinfo_product_image ADD CONSTRAINT pk_lb_product_image PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_product_image_product
    ON lunaerp_baseinfo_product_image (product_id, sort_order);

COMMENT ON TABLE lunaerp_baseinfo_product_image IS '基本信息-产品图片表';
COMMENT ON COLUMN lunaerp_baseinfo_product_image.id IS '图片ID';
COMMENT ON COLUMN lunaerp_baseinfo_product_image.product_id IS '产品ID（历史 UUID 字符串）';
COMMENT ON COLUMN lunaerp_baseinfo_product_image.url IS '图片URL';
COMMENT ON COLUMN lunaerp_baseinfo_product_image.sort_order IS '排序序号（详情页展示顺序）';
COMMENT ON COLUMN lunaerp_baseinfo_product_image.creator IS '创建者';
COMMENT ON COLUMN lunaerp_baseinfo_product_image.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_baseinfo_product_image.updater IS '更新者';
COMMENT ON COLUMN lunaerp_baseinfo_product_image.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_baseinfo_product_image.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_baseinfo_product_image.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 产品标签表 lunaerp_baseinfo_product_tag
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_baseinfo_product_tag (
    id            int8         NOT NULL,
    name          varchar      NOT NULL,
    color         varchar      NULL DEFAULT 'graphite',
    created_by_id varchar      NULL,
    creator       varchar(64)  NULL DEFAULT '',
    create_time   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       varchar(64)  NULL DEFAULT '',
    update_time   timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       int2         NOT NULL DEFAULT 0,
    tenant_id     int8         NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_product_tag') THEN
        ALTER TABLE lunaerp_baseinfo_product_tag ADD CONSTRAINT pk_lb_product_tag PRIMARY KEY (id);
    END IF;
END $$;

-- 租户内标签名唯一（仅未删除行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_product_tag_tenant_name
    ON lunaerp_baseinfo_product_tag (tenant_id, name) WHERE deleted = 0;

COMMENT ON TABLE lunaerp_baseinfo_product_tag IS '基本信息-产品自定义标签表（租户级字典）';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag.id IS '标签ID';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag.name IS '标签名称（同租户内同名共用颜色）';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag.color IS '标签颜色（默认 graphite）';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag.created_by_id IS '创建人ID';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag.creator IS '创建者';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag.updater IS '更新者';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag.tenant_id IS '租户编号';

-- =============================================================================
-- 4. 产品-标签关联表 lunaerp_baseinfo_product_tag_rel
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_baseinfo_product_tag_rel (
    id          int8         NOT NULL,
    product_id  varchar      NOT NULL,
    tag_id      varchar      NOT NULL,
    sort_order  int4         NULL DEFAULT 0,
    creator     varchar(64)  NULL DEFAULT '',
    create_time timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater     varchar(64)  NULL DEFAULT '',
    update_time timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted     int2         NOT NULL DEFAULT 0,
    tenant_id   int8         NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_product_tag_rel') THEN
        ALTER TABLE lunaerp_baseinfo_product_tag_rel ADD CONSTRAINT pk_lb_product_tag_rel PRIMARY KEY (id);
    END IF;
END $$;

-- 同一产品与标签仅允许一条有效关联（独立 id 主键，非复合主键）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_product_tag_rel
    ON lunaerp_baseinfo_product_tag_rel (product_id, tag_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lb_product_tag_rel_tag
    ON lunaerp_baseinfo_product_tag_rel (tag_id);

COMMENT ON TABLE lunaerp_baseinfo_product_tag_rel IS '基本信息-产品标签关联表';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag_rel.id IS '关联ID';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag_rel.product_id IS '产品ID（历史 UUID 字符串）';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag_rel.tag_id IS '标签ID（历史 UUID 字符串）';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag_rel.sort_order IS '排序序号（详情页展示顺序）';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag_rel.creator IS '创建者';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag_rel.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag_rel.updater IS '更新者';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag_rel.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag_rel.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_baseinfo_product_tag_rel.tenant_id IS '租户编号';

-- =============================================================================
-- 5. 胚布品种表 lunaerp_baseinfo_product_greige_type
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_baseinfo_product_greige_type (
    id           int8         NOT NULL,
    code         varchar      NOT NULL,
    name         varchar      NOT NULL,
    match_key    varchar      NOT NULL,
    fabric_type  varchar      NOT NULL,
    composition  varchar      NOT NULL,
    yarn_spec    varchar      NOT NULL,
    weight_gsm   float8       NOT NULL,
    width_cm     float8       NOT NULL,
    craft_type   varchar      NULL,
    machine_type varchar      NULL,
    remark       varchar      NULL,
    status       varchar      NULL DEFAULT 'ACTIVE',
    creator      varchar(64)  NULL DEFAULT '',
    create_time  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater      varchar(64)  NULL DEFAULT '',
    update_time  timestamp    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      int2         NOT NULL DEFAULT 0,
    tenant_id    int8         NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_product_greige_type') THEN
        ALTER TABLE lunaerp_baseinfo_product_greige_type ADD CONSTRAINT pk_lb_product_greige_type PRIMARY KEY (id);
    END IF;
END $$;

-- 租户内编号 / 识别键唯一（仅未删除行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_greige_type_tenant_code
    ON lunaerp_baseinfo_product_greige_type (tenant_id, code) WHERE deleted = 0;
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_greige_type_tenant_match_key
    ON lunaerp_baseinfo_product_greige_type (tenant_id, match_key) WHERE deleted = 0;

COMMENT ON TABLE lunaerp_baseinfo_product_greige_type IS '基本信息-胚布品种（坯型）表';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.id IS '胚布品种ID';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.code IS '品种编号（租户内流水号，如 GT-0001，不随属性变化）';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.name IS '显示名称（如 30S NR罗马布 260g×160cm）';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.match_key IS '识别键（布种+成分+纱支+坯克重+坯门幅拼接，用于查重与自动匹配）';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.fabric_type IS '布种（取字典 FABRIC_TYPE）';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.composition IS '成分（如 NR / TR / CVC）';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.yarn_spec IS '纱支（如 30S）';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.weight_gsm IS '坯克重（g/m²，注意与成品克重不同）';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.width_cm IS '坯门幅（厘米，注意与成品门幅不同）';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.craft_type IS '工艺类型（选填）';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.machine_type IS '机型（选填）';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.remark IS '备注';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.status IS '状态（ACTIVE 启用 / INACTIVE 停用）';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.creator IS '创建者';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.updater IS '更新者';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_baseinfo_product_greige_type.tenant_id IS '租户编号';

-- =============================================================================
-- 5.6 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_baseinfo_product_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_baseinfo_product_image_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_baseinfo_product_tag_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_baseinfo_product_tag_rel_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_baseinfo_product_greige_type_seq;

-- =============================================================================
-- 6. 菜单与按钮（system_menu，可重复执行）
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10000, 10010, 10011, 10012, 10013, 10020, 10021, 10022, 10023);

-- 目录：基本信息
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10000, '基本信息', '', 1, 100, 0, '/baseinfo', 'ep:folder-opened', NULL, NULL, 0, '1', '1', '1', 'admin', '2026-09-13 00:00:00', '1', '2026-09-13 00:00:00', '0');

-- 菜单：产品信息
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10010, '产品信息', 'lunaerp:baseinfo:product:query', 2, 1, 10000, 'product', 'ep:goods', 'lunaerp/baseinfo/product/index', 'LunaerpBaseinfoProduct', 0, '1', '1', '1', 'admin', '2026-09-13 00:00:00', '1', '2026-09-13 00:00:00', '0');
-- 按钮：产品创建/修改/删除
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10011, '产品创建', 'lunaerp:baseinfo:product:create', 3, 1, 10010, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-13 00:00:00', '1', '2026-09-13 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10012, '产品修改', 'lunaerp:baseinfo:product:update', 3, 2, 10010, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-13 00:00:00', '1', '2026-09-13 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10013, '产品删除', 'lunaerp:baseinfo:product:delete', 3, 3, 10010, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-13 00:00:00', '1', '2026-09-13 00:00:00', '0');

-- 菜单：胚布品种
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10020, '胚布品种', 'lunaerp:baseinfo:product:greige:query', 2, 2, 10000, 'greige-type', 'ep:files', 'lunaerp/baseinfo/greige-type/index', 'LunaerpBaseinfoGreigeType', 0, '1', '1', '1', 'admin', '2026-09-13 00:00:00', '1', '2026-09-13 00:00:00', '0');
-- 按钮：胚布品种创建/修改/删除
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10021, '胚布品种创建', 'lunaerp:baseinfo:product:greige:create', 3, 1, 10020, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-13 00:00:00', '1', '2026-09-13 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10022, '胚布品种修改', 'lunaerp:baseinfo:product:greige:update', 3, 2, 10020, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-13 00:00:00', '1', '2026-09-13 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10023, '胚布品种删除', 'lunaerp:baseinfo:product:greige:delete', 3, 3, 10020, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-13 00:00:00', '1', '2026-09-13 00:00:00', '0');

COMMIT;
-- @formatter:on
