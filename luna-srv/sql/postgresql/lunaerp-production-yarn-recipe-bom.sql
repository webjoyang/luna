-- =============================================================================
-- 模块：LunaERP - 生产管理 - 纱线配比 BOM（主从表）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 纱线配比 BOM 主表 lunaerp_production_yarn_recipe_bom
--    通过 product_id 强关联产品档案（ADR-0011），一产品一 BOM。
--    唯一索引 (tenant_id, product_id) 含软删行，软删不释放产品配比。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_yarn_recipe_bom (
    id           int8          NOT NULL,
    product_id   int8          NOT NULL,
    product_code varchar(64)   NOT NULL,
    product_name varchar(128)  NOT NULL,
    remark       varchar(1000) NULL,
    status       varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    creator      varchar(64)   NULL DEFAULT '',
    create_time  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater      varchar(64)   NULL DEFAULT '',
    update_time  timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      int2          NOT NULL DEFAULT 0,
    tenant_id    int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_yarn_recipe_bom') THEN
        ALTER TABLE lunaerp_production_yarn_recipe_bom ADD CONSTRAINT pk_lb_yarn_recipe_bom PRIMARY KEY (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_yarn_recipe_bom_product
    ON lunaerp_production_yarn_recipe_bom (tenant_id, product_id);
CREATE INDEX IF NOT EXISTS idx_lb_yarn_recipe_bom_status
    ON lunaerp_production_yarn_recipe_bom (status);

COMMENT ON TABLE lunaerp_production_yarn_recipe_bom IS '生产管理-纱线配比BOM主表';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom.id IS '配比BOM ID';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom.product_id IS '产品档案ID（lunaerp_baseinfo_product.id，租户内一产品一BOM）';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom.product_code IS '产品编码快照';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom.product_name IS '产品名称快照';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom.status IS '状态（ACTIVE 启用 / INACTIVE 停用），创建缺省 ACTIVE';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 纱线配比 BOM 明细表 lunaerp_production_yarn_recipe_bom_item
--    配比明细随主表聚合保存整体替换（ADR-0003模式），无 line_no。
--    ratio 为百分比数字（49 = 49%），不强制和为 100。
--    ref_price 为参考价快照，可空时测算兜底取规格 ref_price。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_yarn_recipe_bom_item (
    id            int8          NOT NULL,
    recipe_bom_id int8          NOT NULL,
    yarn_type_id  int8          NOT NULL,
    yarn_spec_id  int8          NOT NULL,
    ratio         numeric(6,2)  NOT NULL DEFAULT 0,
    spin_method   varchar(64)   NULL,
    remark        varchar(1000) NULL,
    ref_price     numeric(18,4) NULL,
    creator       varchar(64)   NULL DEFAULT '',
    create_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       varchar(64)   NULL DEFAULT '',
    update_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       int2          NOT NULL DEFAULT 0,
    tenant_id     int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_yarn_recipe_bom_item') THEN
        ALTER TABLE lunaerp_production_yarn_recipe_bom_item ADD CONSTRAINT pk_lb_yarn_recipe_bom_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_yarn_recipe_bom_item_recipe_bom_id
    ON lunaerp_production_yarn_recipe_bom_item (recipe_bom_id);

COMMENT ON TABLE lunaerp_production_yarn_recipe_bom_item IS '生产管理-纱线配比BOM明细表';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.id IS '明细ID';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.recipe_bom_id IS '所属配比BOM ID（lunaerp_production_yarn_recipe_bom.id）';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.yarn_type_id IS '纱线材质ID（lunaerp_production_yarn_type.id）';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.yarn_spec_id IS '纱线规格ID（lunaerp_production_yarn_spec.id）';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.ratio IS '配比百分比数字（49 = 49%）';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.spin_method IS '纺纱工艺（可空）';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.ref_price IS '参考单价（可空，测算兜底取规格 ref_price）';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_yarn_recipe_bom_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_yarn_recipe_bom_item_seq;

-- =============================================================================
-- 4. 菜单与按钮（system_menu，可重复执行）
--    10200 生产管理目录下挂载 10250 纱线配比BOM菜单；10251-10253 创建/修改/删除按钮。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10250, 10251, 10252, 10253);

-- 菜单：纱线配比BOM（query 权限挂在菜单本身，参照胚布单/染色单/纱线档案模式）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10250, '纱线配比BOM', 'lunaerp:production:yarn-recipe-bom:query', 2, 5, 10200, 'yarn-recipe-bom', 'ep:operation', 'lunaerp-elmui/production/yarn-recipe-bom/index', 'LunaerpElmuiProductionYarnRecipeBom', 0, '1', '1', '1', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');
-- 按钮：纱线配比BOM创建/修改/删除
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10251, '纱线配比BOM创建', 'lunaerp:production:yarn-recipe-bom:create', 3, 1, 10250, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10252, '纱线配比BOM修改', 'lunaerp:production:yarn-recipe-bom:update', 3, 2, 10250, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10253, '纱线配比BOM删除', 'lunaerp:production:yarn-recipe-bom:delete', 3, 3, 10250, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');

COMMIT;
-- @formatter:on
