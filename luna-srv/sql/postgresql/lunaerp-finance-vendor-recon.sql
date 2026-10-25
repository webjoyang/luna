-- =============================================================================
-- 模块：LunaERP - 财务域 - 供应商对账（对账单主表 + 行快照表）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 避让声明：错误码 1-030-022（供应商对账独占 000–007）；菜单 10730（按钮 10731–10736）；
--           字典类型 2269；字典数据 3831–3833。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 表 1：财务域 - 供应商对账单主表 lunaerp_finance_supplier_recon
--    单号 SR+YYYYMMDD+3 位日流水，按天取号（ADR-0006 模式），租户内全量唯一含软删行；
--    三态状态机 DRAFT / CONFIRMED / CANCELLED（字典 2269，缺省 DRAFT）；
--    不落已申请/已付金额字段（只读实时聚合，杜绝老系统 paidAmount 占位 0 脱节 P0）；
--    金额一律 numeric(18,2)，仅人民币无币种/汇率列。全部继承 TenantBaseDO。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_supplier_recon (
    id            int8          NOT NULL,
    recon_no      varchar(32)   NOT NULL,
    supplier_id   int8          NOT NULL,
    supplier_name varchar(128)  NOT NULL,
    period_start  date          NOT NULL,
    period_end    date          NOT NULL,
    status        varchar(16)   NOT NULL DEFAULT 'DRAFT',
    total_amount  numeric(18,2) NOT NULL DEFAULT 0,
    confirmed_by  int8          NULL,
    confirmed_at  timestamp     NULL,
    cancelled_by  int8          NULL,
    cancelled_at  timestamp     NULL,
    remark        varchar(1000) NULL,
    creator       varchar(64)   NULL DEFAULT '',
    create_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       varchar(64)   NULL DEFAULT '',
    update_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       int2          NOT NULL DEFAULT 0,
    tenant_id     int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_supplier_recon') THEN
        ALTER TABLE lunaerp_finance_supplier_recon ADD CONSTRAINT pk_lunaerp_finance_supplier_recon PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_supplier_recon_seq;

-- 对账单号租户内全量唯一（含软删行，按天取号，软删不释放编号）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lfsr_recon_no
    ON lunaerp_finance_supplier_recon (tenant_id, recon_no);

-- 供应商与状态筛选索引
CREATE INDEX IF NOT EXISTS idx_lfsr_supplier_status
    ON lunaerp_finance_supplier_recon (tenant_id, supplier_id, status);

-- 对账区间复合索引
CREATE INDEX IF NOT EXISTS idx_lfsr_period
    ON lunaerp_finance_supplier_recon (tenant_id, period_start, period_end);

COMMENT ON TABLE lunaerp_finance_supplier_recon IS '财务管理-供应商对账单主表';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.id IS '对账单ID';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.recon_no IS '对账单号（SR+YYYYMMDD+3位流水，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.supplier_id IS '供应商ID（lunaerp_baseinfo_supplier.id）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.supplier_name IS '供应商公司名快照';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.period_start IS '对账区间起始日期（按源单单据日期过滤，含端点）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.period_end IS '对账区间截止日期（按源单单据日期过滤，含端点）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.status IS '状态（字典 lunaerp_supplier_recon_status：DRAFT/CONFIRMED/CANCELLED），缺省 DRAFT';
COMMENT ON COLUMN lunaerp_finance_supplier_recon.total_amount IS '对账总金额（元，服务端权威重算 = Σ行 line_amount，含负数调整项）';
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
-- 2. 表 2：财务域 - 供应商对账单行表 lunaerp_finance_supplier_recon_item
--    三类行：PROCESS 加工费行 / PURCHASE 采购货款行 / ADJUST 调整项行；
--    行表即快照，生成时从源单复制，零回写；确认后行数据冻结；
--    防重复取数靠 source_ref 指纹（DRAFT/CONFIRMED 占用，CANCELLED 释放）；
--    金额 numeric(18,2)、单价 numeric(18,4)、数量 numeric(18,2)。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_finance_supplier_recon_item (
    id                   int8          NOT NULL,
    recon_id             int8          NOT NULL,
    line_type            varchar(16)   NOT NULL,
    source_ref_type      varchar(32)   NULL,
    source_ref_id        int8          NULL,
    source_ref_item_id   int8          NULL,
    inspection_order_id  int8          NULL,
    inspection_order_no  varchar(32)   NULL,
    inspection_type      varchar(16)   NULL,
    stock_in_id          int8          NULL,
    stock_in_no          varchar(32)   NULL,
    stock_in_item_id     int8          NULL,
    yarn_order_id        int8          NULL,
    yarn_order_no        varchar(32)   NULL,
    product_id           int8          NULL,
    product_code         varchar(64)   NULL,
    product_name         varchar(128)  NULL,
    batch_no             varchar(64)   NULL,
    qty_kg               numeric(18,2) NULL,
    qty_m                numeric(18,2) NULL,
    qty_unit             varchar(16)   NULL,
    pricing_basis        varchar(16)   NULL,
    bill_qty             numeric(18,2) NULL,
    unit_price           numeric(18,4) NULL,
    line_amount          numeric(18,2) NOT NULL DEFAULT 0,
    adjust_reason        varchar(500)  NULL,
    remark               varchar(500)  NULL,
    creator              varchar(64)   NULL DEFAULT '',
    create_time          timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater              varchar(64)   NULL DEFAULT '',
    update_time          timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted              int2          NOT NULL DEFAULT 0,
    tenant_id            int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_finance_supplier_recon_item') THEN
        ALTER TABLE lunaerp_finance_supplier_recon_item ADD CONSTRAINT pk_lunaerp_finance_supplier_recon_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_supplier_recon_item_seq;

-- 行表三大核心索引
CREATE INDEX IF NOT EXISTS idx_lfsri_recon_id
    ON lunaerp_finance_supplier_recon_item (tenant_id, recon_id);

CREATE INDEX IF NOT EXISTS idx_lfsri_source_ref
    ON lunaerp_finance_supplier_recon_item (tenant_id, source_ref_type, source_ref_id);

CREATE INDEX IF NOT EXISTS idx_lfsri_stock_in_item
    ON lunaerp_finance_supplier_recon_item (tenant_id, stock_in_item_id);

COMMENT ON TABLE lunaerp_finance_supplier_recon_item IS '财务管理-供应商对账单行表（快照）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.id IS '对账单明细行ID';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.recon_id IS '所属对账单ID（lunaerp_finance_supplier_recon.id）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.line_type IS '行类型（PROCESS 加工费 / PURCHASE 采购货款 / ADJUST 调整项）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.source_ref_type IS '来源单据类型（INSPECTION 检验单 / STOCK_IN_ITEM 入库明细；ADJUST 为空）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.source_ref_id IS '来源单据主表ID（检验单ID / 入库单ID）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.source_ref_item_id IS '来源单据明细ID（入库明细ID，检验单为汇总行可空）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.inspection_order_id IS '加工源检验单ID（PROCESS 必填快照）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.inspection_order_no IS '加工源检验单号快照';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.inspection_type IS '检验单类型快照（GREIGE 胚布检验=织费 / FINISHED 成品检验=染费）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.stock_in_id IS '采购源入库单ID（PURCHASE 必填快照）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.stock_in_no IS '采购源入库单号快照';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.stock_in_item_id IS '采购源入库单明细ID（PURCHASE 必填快照）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.yarn_order_id IS '纱线采购单ID快照（可选，入库单带出）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.yarn_order_no IS '纱线采购单号快照（追溯用，可选）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.product_id IS '产品ID快照';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.product_code IS '产品编号快照';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.product_name IS '产品名称快照';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.batch_no IS '批次/缸号快照';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.qty_kg IS '千克数量（PROCESS 对应实称重量合计，PURCHASE KG 单位数量）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.qty_m IS '米数数量（PROCESS 对应检验米数合计，PURCHASE M 单位数量）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.qty_unit IS '数量单位（KG / M）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.pricing_basis IS '计价基准（KG 按千克 / METER 按米；GREIGE 缺省米，FINISHED 缺省公斤）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.bill_qty IS '结算计费数量（基准对应量，元数据/重算权威）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.unit_price IS '结算单价（元，手工填/入库明细带出可改）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.line_amount IS '行金额（元，line_amount = bill_qty × unit_price 服务端重算；ADJUST 直接填正负数）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.adjust_reason IS '调整原因（ADJUST 行必填，如质量扣款、运费补差、退货冲减）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.remark IS '备注';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_finance_supplier_recon_item.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 字典：2269 对账单状态 lunaerp_supplier_recon_status
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id = 2269 OR type = 'lunaerp_supplier_recon_status';
DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_supplier_recon_status' OR id IN (3831, 3832, 3833);

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2269, '供应商对账单状态', 'lunaerp_supplier_recon_status', 0, 'LunaERP 财务管理-供应商对账单状态', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3831, 1, '草稿', 'DRAFT', 'lunaerp_supplier_recon_status', 0, 'warning', '', NULL, 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3832, 2, '已确认', 'CONFIRMED', 'lunaerp_supplier_recon_status', 0, 'success', '', NULL, 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3833, 3, '已作废', 'CANCELLED', 'lunaerp_supplier_recon_status', 0, 'info', '', NULL, 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 4. 菜单与权限：10700 财务一级目录（判存插入）+ 10730 供应商对账菜单与按钮
-- =============================================================================
-- @formatter:off
BEGIN;

-- 4.1 确保 10700 财务一级目录就位（ADR-0037，sort 107，path /finance，icon ep:money）
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10700, '财务', '', 1, 107, 0, '/finance', 'ep:money', '', NULL,
    0, '1', '1', '1', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0'
) ON CONFLICT (id) DO NOTHING;

-- 4.2 幂等删除已有菜单与按钮
DELETE FROM system_menu WHERE id IN (10730, 10731, 10732, 10733, 10734, 10735, 10736);

-- 二级菜单：10730 供应商对账
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10730, '供应商对账', 'lunaerp:finance:supplierrecon:query', 2, 3, 10700, 'supplier-reconciliation', 'ep:files', 'lunaerp-elmui/finance/supplier-recon/index', 'LunaerpFinanceSupplierRecon',
    0, '1', '1', '1', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0'
);

-- 按钮权限：10731 生成 / 10732 编辑重取 / 10733 确认作废 / 10734 生成付款申请 / 10735 打印 / 10736 删除
INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10731, '生成对账单', 'lunaerp:finance:supplierrecon:create', 3, 1, 10730, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0'
);

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10732, '编辑对账单', 'lunaerp:finance:supplierrecon:update', 3, 2, 10730, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0'
);

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10733, '确认作废', 'lunaerp:finance:supplierrecon:confirm', 3, 3, 10730, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0'
);

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10734, '生成付款申请', 'lunaerp:finance:supplierrecon:generate', 3, 4, 10730, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0'
);

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10735, '打印对账单', 'lunaerp:finance:supplierrecon:print', 3, 5, 10730, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0'
);

INSERT INTO system_menu (
    id, name, permission, type, sort, parent_id, path, icon, component, component_name,
    status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted
) VALUES (
    10736, '删除对账单', 'lunaerp:finance:supplierrecon:delete', 3, 6, 10730, '', '#', '', NULL,
    0, '1', '1', '1', 'admin', '2026-10-15 00:00:00', '1', '2026-10-15 00:00:00', '0'
);

-- 4.3 赋权系统管理员角色
CREATE SEQUENCE IF NOT EXISTS system_role_menu_seq;

DO $$
BEGIN
    PERFORM setval('system_role_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role_menu), 1));
END $$;

DELETE FROM system_role_menu WHERE menu_id IN (10700, 10730, 10731, 10732, 10733, 10734, 10735, 10736);
INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM (
    SELECT DISTINCT id FROM system_role
    WHERE code IN ('super_admin', 'admin') OR id IN (1, 2)
) r
CROSS JOIN (
    SELECT 10700 AS id UNION ALL SELECT 10730 UNION ALL SELECT 10731 UNION ALL SELECT 10732 UNION ALL
    SELECT 10733 UNION ALL SELECT 10734 UNION ALL SELECT 10735 UNION ALL SELECT 10736
) m;

COMMIT;
-- @formatter:on
