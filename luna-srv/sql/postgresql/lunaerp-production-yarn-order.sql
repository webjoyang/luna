-- =============================================================================
-- 模块：LunaERP - 生产管理 - 纱线采购单与库存台账
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 5. 纱线库存通用流水台账 lunaerp_production_stock_ledger
--    通用流水台账表（去 source_location/target_location 两列，ADR-0012）。
--    append-only 流水，无 CRUD 端点，仅由后端服务内部调用 + 暴露余额查询接口。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_stock_ledger (
    id                int8          NOT NULL,
    yarn_spec_id      int8          NOT NULL,
    type              varchar(32)   NOT NULL,
    quantity          numeric(18,2) NOT NULL DEFAULT 0,
    related_doc_type  varchar(32)   NOT NULL,
    related_doc_id    int8          NOT NULL,
    batch_no          varchar(64)   NULL,
    remark            varchar(1000) NULL,
    creator           varchar(64)   NULL DEFAULT '',
    create_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater           varchar(64)   NULL DEFAULT '',
    update_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           int2          NOT NULL DEFAULT 0,
    tenant_id         int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_stock_ledger') THEN
        ALTER TABLE lunaerp_production_stock_ledger ADD CONSTRAINT pk_lb_stock_ledger PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_stock_ledger_spec_type
    ON lunaerp_production_stock_ledger (yarn_spec_id, type);
CREATE INDEX IF NOT EXISTS idx_lb_stock_ledger_related_doc
    ON lunaerp_production_stock_ledger (related_doc_type, related_doc_id);

COMMENT ON TABLE lunaerp_production_stock_ledger IS '生产管理-纱线库存通用流水台账';
COMMENT ON COLUMN lunaerp_production_stock_ledger.id IS '流水ID';
COMMENT ON COLUMN lunaerp_production_stock_ledger.yarn_spec_id IS '纱线规格ID（lunaerp_production_yarn_spec.id）';
COMMENT ON COLUMN lunaerp_production_stock_ledger.type IS '流水类型（STOCK_IN 入库 / STOCK_OUT 出库 / PROCESS_IN 生产入库 / PROCESS_OUT 生产出库，MVP仅用STOCK_IN）';
COMMENT ON COLUMN lunaerp_production_stock_ledger.quantity IS '变动数量（STOCK_IN正/STOCK_OUT负）';
COMMENT ON COLUMN lunaerp_production_stock_ledger.related_doc_type IS '关联单据类型（如 yarn_order）';
COMMENT ON COLUMN lunaerp_production_stock_ledger.related_doc_id IS '关联单据ID';
COMMENT ON COLUMN lunaerp_production_stock_ledger.batch_no IS '批号';
COMMENT ON COLUMN lunaerp_production_stock_ledger.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_stock_ledger.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_stock_ledger.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_stock_ledger.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_stock_ledger.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_stock_ledger.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_production_stock_ledger.tenant_id IS '租户编号';

-- 主键序列
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_stock_ledger_seq;

-- =============================================================================
-- 6. 纱线采购单主表 lunaerp_production_yarn_order
--    主表不放供应商（多供应商挂在明细行）。
--    source_instructions 存关联胚布指示单 ID 数组（JSON 字符串）。
--    采购单号按天取号，租户内全量唯一含软删行（ADR-0006 模式）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_yarn_order (
    id                  int8          NOT NULL,
    order_no            varchar(32)   NOT NULL,
    source_instructions text          NULL,
    order_date          date          NOT NULL DEFAULT CURRENT_DATE,
    delivery_date       date          NULL,
    loss_rate           numeric(6,2)  NULL DEFAULT 0,
    status              varchar(16)   NOT NULL DEFAULT 'DRAFT',
    remark              varchar(1000) NULL,
    creator             varchar(64)   NULL DEFAULT '',
    create_time         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater             varchar(64)   NULL DEFAULT '',
    update_time         timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             int2          NOT NULL DEFAULT 0,
    tenant_id           int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_yarn_order') THEN
        ALTER TABLE lunaerp_production_yarn_order ADD CONSTRAINT pk_lb_yarn_order PRIMARY KEY (id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_yarn_order_no
    ON lunaerp_production_yarn_order (tenant_id, order_no);
CREATE INDEX IF NOT EXISTS idx_lb_yarn_order_status
    ON lunaerp_production_yarn_order (status);
CREATE INDEX IF NOT EXISTS idx_lb_yarn_order_order_date
    ON lunaerp_production_yarn_order (order_date);

COMMENT ON TABLE lunaerp_production_yarn_order IS '生产管理-纱线采购单主表';
COMMENT ON COLUMN lunaerp_production_yarn_order.id IS '采购单ID';
COMMENT ON COLUMN lunaerp_production_yarn_order.order_no IS '采购单号（YP+年月日+3位日流水，按天取号，租户内全量唯一含已删除行）';
COMMENT ON COLUMN lunaerp_production_yarn_order.source_instructions IS '关联胚布指示单ID数组（JSON字符串，如 "[123,456]"）';
COMMENT ON COLUMN lunaerp_production_yarn_order.order_date IS '单据日期';
COMMENT ON COLUMN lunaerp_production_yarn_order.delivery_date IS '交货日期';
COMMENT ON COLUMN lunaerp_production_yarn_order.loss_rate IS '损耗率百分比数字（5 = 5%）';
COMMENT ON COLUMN lunaerp_production_yarn_order.status IS '状态（字典 lunaerp_yarn_order_status：DRAFT/ACTIVE/COMPLETED/CANCELLED）';
COMMENT ON COLUMN lunaerp_production_yarn_order.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_yarn_order.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_yarn_order.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_yarn_order.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_yarn_order.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_yarn_order.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_production_yarn_order.tenant_id IS '租户编号';

-- 主键序列
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_yarn_order_seq;

-- =============================================================================
-- 7. 纱线采购明细表 lunaerp_production_yarn_order_item
--    多供应商挂在明细行，保留三量字段（需求量/在库量/实采量），无 line_no。
--    随主表聚合保存整体替换（ADR-0003 模式）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_yarn_order_item (
    id             int8          NOT NULL,
    order_id       int8          NOT NULL,
    yarn_type_id   int8          NOT NULL,
    yarn_spec_id   int8          NOT NULL,
    supplier_id    int8          NULL,
    supplier_name  varchar(128)  NULL,
    ratio          numeric(6,2)  NULL DEFAULT 0,
    loss_rate      numeric(6,2)  NULL DEFAULT 0,
    base_qty       numeric(18,2) NULL DEFAULT 0,
    required_qty   numeric(18,2) NULL DEFAULT 0,
    stock_qty      numeric(18,2) NULL DEFAULT 0,
    purchase_qty   numeric(18,2) NULL DEFAULT 0,
    unit_price     numeric(18,4) NULL,
    total_amount   numeric(18,2) NULL DEFAULT 0,
    ref_unit_price numeric(18,4) NULL,
    remark         varchar(1000) NULL,
    creator        varchar(64)   NULL DEFAULT '',
    create_time    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater        varchar(64)   NULL DEFAULT '',
    update_time    timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted        int2          NOT NULL DEFAULT 0,
    tenant_id      int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_yarn_order_item') THEN
        ALTER TABLE lunaerp_production_yarn_order_item ADD CONSTRAINT pk_lb_yarn_order_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_yarn_order_item_order_id
    ON lunaerp_production_yarn_order_item (order_id);
CREATE INDEX IF NOT EXISTS idx_lb_yarn_order_item_yarn_spec_id
    ON lunaerp_production_yarn_order_item (yarn_spec_id);

COMMENT ON TABLE lunaerp_production_yarn_order_item IS '生产管理-纱线采购明细表';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.id IS '明细ID';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.order_id IS '所属采购单ID（lunaerp_production_yarn_order.id）';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.yarn_type_id IS '纱线材质ID（lunaerp_production_yarn_type.id）';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.yarn_spec_id IS '纱线规格ID（lunaerp_production_yarn_spec.id）';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.supplier_id IS '供应商ID（lunaerp_baseinfo_supplier.id）';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.supplier_name IS '供应商公司名快照';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.ratio IS '配比百分比数字（49 = 49%）';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.loss_rate IS '损耗率百分比数字（从主单复制，可逐行覆盖）';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.base_qty IS '基础需求量（Σ 胚布单成品量 × ratio/100）';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.required_qty IS '含损耗需求量 = base_qty × (1 + loss_rate/100)';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.stock_qty IS '在库量快照（测算时的 StockLedger 余额）';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.purchase_qty IS '实采量 = max(0, required_qty - stock_qty)';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.unit_price IS '单价（默认 ref_unit_price，可手改）';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.total_amount IS '金额 = unit_price × purchase_qty（服务端权威重算）';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.ref_unit_price IS '历史价回填（最近已下达采购单 unit_price > 规格 ref_price 兜底）';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_production_yarn_order_item.tenant_id IS '租户编号';

-- 主键序列
CREATE SEQUENCE IF NOT EXISTS lunaerp_production_yarn_order_item_seq;

-- =============================================================================
-- 8. 菜单与按钮（system_menu，可重复执行）
--    10200 生产管理目录下挂载 10230 纱线采购菜单（承载 query 权限）；10231-10233 创建/修改/删除按钮。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10230, 10231, 10232, 10233);

-- 菜单：纱线采购（query 权限挂在菜单本身，参照胚布单/染色单/配比BOM模式）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10230, '纱线采购', 'lunaerp:production:yarn-order:query', 2, 3, 10200, 'yarn-order', 'ep:shopping-cart', 'lunaerp-elmui/production/yarn-order/index', 'LunaerpElmuiProductionYarnOrder', 0, '1', '1', '1', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');
-- 按钮：纱线采购单创建/修改/删除
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10231, '纱线采购单创建', 'lunaerp:production:yarn-order:create', 3, 1, 10230, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10232, '纱线采购单修改', 'lunaerp:production:yarn-order:update', 3, 2, 10230, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10233, '纱线采购单删除', 'lunaerp:production:yarn-order:delete', 3, 3, 10230, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 9. 字典类型与字典数据（system_dict_type / system_dict_data，可重复执行）
--    dict_type id 2230，dict_data id 3580-3583（紧接染色单 3571-3574）。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2230);
DELETE FROM system_dict_data WHERE dict_type IN ('lunaerp_yarn_order_status');

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2230, '纱线采购单状态', 'lunaerp_yarn_order_status', 0, 'LunaERP 生产管理-纱线采购单状态', 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0', NULL);

-- 采购单状态：草稿/进行中/已完成/已取消（四态，ADR-0010）
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3580, 1, '草稿', 'DRAFT', 'lunaerp_yarn_order_status', 0, 'warning', '', NULL, 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3581, 2, '进行中', 'ACTIVE', 'lunaerp_yarn_order_status', 0, 'success', '', NULL, 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3582, 3, '已完成', 'COMPLETED', 'lunaerp_yarn_order_status', 0, 'info', '', NULL, 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3583, 4, '已取消', 'CANCELLED', 'lunaerp_yarn_order_status', 0, 'danger', '', NULL, 'admin', '2026-09-20 00:00:00', '1', '2026-09-20 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 10. 胚布指示单结构变更（集中管理纱线域变更，见 ADR-0011 / Issue 03）
-- 胚布指示单追加 product_id 字段强关联产品档案，用于纱线测算时查配比 BOM。
-- （既有 product_code/product_name 列已存在弱关联快照，不重复添加）
-- =============================================================================
ALTER TABLE lunaerp_production_greige_order ADD COLUMN IF NOT EXISTS product_id int8 NULL;
COMMENT ON COLUMN lunaerp_production_greige_order.product_id IS '产品档案ID（强关联，用于纱线测算时查配比BOM，见 ADR-0011）';
CREATE INDEX IF NOT EXISTS idx_lb_greige_order_product_id ON lunaerp_production_greige_order (product_id);
