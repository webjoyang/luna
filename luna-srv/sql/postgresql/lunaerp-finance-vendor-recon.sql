-- =============================================================================
-- 模块：LunaERP - 财务管理 - 供应商对账（主表 + 行表）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 避让声明：错误码 1-030-022（000–007）；菜单 10700（财务根）、10730（供应商对账）及按钮 10731–10736；
--           字典类型 2269（lunaerp_supplier_recon_status），字典数据 3830–3832。
-- =============================================================================

-- =============================================================================
-- 1. 新表 1：财务域 - 供应商对账单主表 lunaerp_finance_supplier_recon
--    单号 SR+YYYYMMDD+3位日流水，按天取号，租户内全量唯一含软删行（ADR-0006/0015）；
--    状态三态：DRAFT 草稿 / CONFIRMED 已确认 / CANCELLED 已作废；
--    不落已申请/已付金额字段（实时只读聚合，防脱节）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_supplier_recon (
    id                 int8          NOT NULL,
    recon_no           varchar(32)   NOT NULL,
    supplier_id        int8          NOT NULL,
    supplier_name      varchar(128)  NOT NULL,
    period_start       date          NOT NULL,
    period_end         date          NOT NULL,
    status             varchar(16)   NOT NULL DEFAULT 'DRAFT',
    total_amount       numeric(18,2) NOT NULL DEFAULT 0,
    confirmed_by       int8          NULL,
    confirmed_at       timestamp     NULL,
    cancelled_by       int8          NULL,
    cancelled_at       timestamp     NULL,
    remark             varchar(1000) NULL,
    creator            varchar(64)   NULL DEFAULT '',
    create_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater            varchar(64)   NULL DEFAULT '',
    update_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            int2          NOT NULL DEFAULT 0,
    tenant_id          int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_supplier_recon') THEN
        ALTER TABLE lunaerp_finance_supplier_recon ADD CONSTRAINT pk_lunaerp_finance_supplier_recon PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_supplier_recon_seq;

-- 对账单号租户内全量唯一（普通唯一索引，不带 WHERE deleted=0，含软删行）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfsr_recon_no
    ON lunaerp_finance_supplier_recon (tenant_id, recon_no);

-- 供应商与状态、对账区间复合查询索引
CREATE INDEX IF NOT EXISTS idx_lfsr_lookup
    ON lunaerp_finance_supplier_recon (tenant_id, supplier_id, status, period_start, period_end);

COMMENT ON TABLE lunaerp_finance_supplier_recon IS '财务管理-供应商对账单主表';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.id IS '对账单ID';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.recon_no IS '对账单号（SR+YYYYMMDD+3位日流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.supplier_id IS '供应商ID（关联 lunaerp_baseinfo_supplier.id）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.supplier_name IS '供应商名称快照';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.period_start IS '对账区间开始日期（含端点，按源单单据日期过滤）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.period_end IS '对账区间结束日期（含端点，按源单单据日期过滤）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.status IS '状态（字典 lunaerp_supplier_recon_status：DRAFT/CONFIRMED/CANCELLED）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.total_amount IS '确认金额合计（元，服务端权威重算，含负数调整项）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.confirmed_by IS '确认人用户ID';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.confirmed_at IS '确认时间';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.cancelled_by IS '作废人用户ID';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.cancelled_at IS '作废时间';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.remark IS '备注';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 新表 2：财务域 - 供应商对账单行表 lunaerp_finance_supplier_recon_item
--    行类型：PROCESS 加工费行 / PURCHASE 采购货款行 / ADJUST 调整项行；
--    快照表设计，生成时从源单复制，零回写源单；确认后整表冻结。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_supplier_recon_item (
    id                     int8          NOT NULL,
    recon_id               int8          NOT NULL,
    line_type              varchar(16)   NOT NULL,
    source_ref_type        varchar(32)   NULL,
    source_ref_id          int8          NULL,
    source_ref_item_id     int8          NULL,
    inspection_order_id    int8          NULL,
    inspection_order_no    varchar(32)   NULL,
    inspection_type        varchar(16)   NULL,
    stock_in_id            int8          NULL,
    stock_in_no            varchar(32)   NULL,
    yarn_order_id          int8          NULL,
    yarn_order_no          varchar(32)   NULL,
    product_id             int8          NULL,
    product_code           varchar(64)   NULL,
    product_name           varchar(128)  NULL,
    batch_no               varchar(64)   NULL,
    qty_kg                 numeric(18,2) NULL,
    qty_m                  numeric(18,2) NULL,
    pricing_basis          varchar(16)   NULL,
    bill_qty               numeric(18,2) NULL,
    unit_price             numeric(18,4) NULL,
    line_amount            numeric(18,2) NOT NULL DEFAULT 0,
    adjust_reason          varchar(500)  NULL,
    remark                 varchar(500)  NULL,
    creator                varchar(64)   NULL DEFAULT '',
    create_time            timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                varchar(64)   NULL DEFAULT '',
    update_time            timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                int2          NOT NULL DEFAULT 0,
    tenant_id              int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_supplier_recon_item') THEN
        ALTER TABLE lunaerp_finance_supplier_recon_item ADD CONSTRAINT pk_lunaerp_finance_supplier_recon_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_supplier_recon_item_seq;

-- 行表归属索引
CREATE INDEX IF NOT EXISTS idx_lfsri_recon
    ON lunaerp_finance_supplier_recon_item (tenant_id, recon_id);

-- 源指纹防重查询索引
CREATE INDEX IF NOT EXISTS idx_lfsri_source
    ON lunaerp_finance_supplier_recon_item (tenant_id, source_ref_type, source_ref_id);

-- 入库单明细防重查询索引
CREATE INDEX IF NOT EXISTS idx_lfsri_stock_in_item
    ON lunaerp_finance_supplier_recon_item (tenant_id, source_ref_item_id);

COMMENT ON TABLE lunaerp_finance_supplier_recon_item IS '财务管理-供应商对账单行表';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.id IS '对账单行ID';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.recon_id IS '关联对账单主表ID';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.line_type IS '行类型（PROCESS 加工费 / PURCHASE 采购货款 / ADJUST 调整项）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.source_ref_type IS '源单据类型（INSPECTION / STOCK_IN_ITEM，ADJUST 为空）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.source_ref_id IS '源单据主表ID（检验单ID / 入库单ID）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.source_ref_item_id IS '源单据明细ID（入库明细ID，检验单为汇总行时为空）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.inspection_order_id IS '检验单ID快照（PROCESS 行）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.inspection_order_no IS '检验单号快照（PROCESS 行）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.inspection_type IS '检验类型快照（GREIGE 胚布检验 / FINISHED 成品检验）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.stock_in_id IS '入库单ID快照（PURCHASE 行）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.stock_in_no IS '入库单号快照（PURCHASE 行）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.yarn_order_id IS '关联纱线采购单ID快照（PURCHASE 行，可选）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.yarn_order_no IS '关联纱线采购单号快照（PURCHASE 行，可选）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.product_id IS '产品ID快照';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.product_code IS '产品编号快照';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.product_name IS '产品名称快照';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.batch_no IS '批次/缸号快照';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.qty_kg IS '重量数量（KG，PROCESS：实称总重；PURCHASE：KG 单位量）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.qty_m IS '米数数量（M，PROCESS：实测总米数；PURCHASE：M 单位量）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.pricing_basis IS '计价基准（KG / METER）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.bill_qty IS '结算计费数量（基准对应量）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.unit_price IS '结算单价（元/KG 或 元/M）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.line_amount IS '行金额（元，bill_qty * unit_price，调整项可正可负）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.adjust_reason IS '调整原因（ADJUST 行必填）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.remark IS '行备注';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 字典类型与数据：2269 lunaerp_supplier_recon_status（3830–3832）
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id = 2269 OR type = 'lunaerp_supplier_recon_status';
DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_supplier_recon_status' OR id IN (3830, 3831, 3832);

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2269, '供应商对账单状态', 'lunaerp_supplier_recon_status', 0, 'LunaERP 财务管理-供应商对账单状态', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3830, 1, '草稿', 'DRAFT', 'lunaerp_supplier_recon_status', 0, 'warning', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3831, 2, '已确认', 'CONFIRMED', 'lunaerp_supplier_recon_status', 0, 'success', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3832, 3, '已作废', 'CANCELLED', 'lunaerp_supplier_recon_status', 0, 'info', '', NULL, 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 4. 菜单与权限：10700（财务根目录判存补全）、10730（供应商对账）及按钮 10731–10736
-- =============================================================================
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'system_role_menu' AND column_name = 'id' AND column_default IS NULL
    ) AND EXISTS (
        SELECT 1 FROM pg_class WHERE relname = 'system_role_menu_seq'
    ) THEN
        ALTER TABLE system_role_menu ALTER COLUMN id SET DEFAULT nextval('system_role_menu_seq');
    END IF;
END $$;

-- @formatter:off
BEGIN;

-- 4.1 根目录：财务（parent_id=0 顶级目录，sort=107，path='/finance'，icon='ep:money'）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10700, '财务', '', 1, 107, 0, '/finance', 'ep:money', NULL, NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0')
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    sort = EXCLUDED.sort,
    path = EXCLUDED.path,
    icon = EXCLUDED.icon;

-- 4.2 供应商对账菜单与按钮
DELETE FROM system_menu WHERE id IN (10730, 10731, 10732, 10733, 10734, 10735, 10736);

-- 供应商对账菜单
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10730, '供应商对账', 'lunaerp:finance:supplierrecon:query', 2, 3, 10700, 'supplier-reconciliation', 'ep:files', 'lunaerp-elmui/finance/supplier-recon/index', 'LunaerpElmuiSupplierRecon', 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 按钮：生成对账单 / 编辑重取 / 确认作废 / 生成付款申请 / 打印 / 删除
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10731, '生成对账单', 'lunaerp:finance:supplierrecon:create', 3, 1, 10730, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10732, '编辑/重取', 'lunaerp:finance:supplierrecon:update', 3, 2, 10730, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10733, '确认/作废', 'lunaerp:finance:supplierrecon:confirm', 3, 3, 10730, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10734, '生成付款申请', 'lunaerp:finance:supplierrecon:generate', 3, 4, 10730, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10735, '打印', 'lunaerp:finance:supplierrecon:print', 3, 5, 10730, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10736, '删除', 'lunaerp:finance:supplierrecon:delete', 3, 6, 10730, '', '#', '', NULL, 0, '1', '1', '1', 'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, '0');

-- 4.3 授权角色（管理员与角色 1 / 1000）
DELETE FROM system_role_menu WHERE menu_id IN (10730, 10731, 10732, 10733, 10734, 10735, 10736);

INSERT INTO system_role_menu (role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (
    SELECT 10700 AS id UNION ALL
    SELECT 10730 UNION ALL SELECT 10731 UNION ALL SELECT 10732 UNION ALL
    SELECT 10733 UNION ALL SELECT 10734 UNION ALL SELECT 10735 UNION ALL
    SELECT 10736
) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2, 1000)
ON CONFLICT DO NOTHING;

COMMIT;
-- @formatter:on
