-- =============================================================================
-- 模块：LunaERP - 库存管理（仓库、库位、入库单、明细、放置、库存流水台账）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 仓库表 lunaerp_stock_warehouse（ADR-0018）
--    仓库编号 WH+年份+4 位流水（WH20260001），租户内全量唯一含软删行，仿 ADR-0005；
--    状态 ACTIVE/INACTIVE，不建字典。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_stock_warehouse (
    id            int8          NOT NULL,
    warehouse_no  varchar(32)   NOT NULL,
    name          varchar(128)  NOT NULL,
    address       varchar(255)  NULL,
    status        varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    remark        varchar(500)  NULL,
    creator       varchar(64)   NULL DEFAULT '',
    create_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       varchar(64)   NULL DEFAULT '',
    update_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       int2          NOT NULL DEFAULT 0,
    tenant_id     int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_stock_warehouse') THEN
        ALTER TABLE lunaerp_stock_warehouse ADD CONSTRAINT pk_lb_stock_warehouse PRIMARY KEY (id);
    END IF;
END $$;

-- 仓库编号租户内全量唯一（仿 ADR-0005）：普通唯一索引、不带 WHERE deleted=0，按年取号、软删不释放编号
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_stock_warehouse_tenant_no
    ON lunaerp_stock_warehouse (tenant_id, warehouse_no);
CREATE INDEX IF NOT EXISTS idx_lb_stock_warehouse_status
    ON lunaerp_stock_warehouse (status);

COMMENT ON TABLE lunaerp_stock_warehouse IS '库存管理-仓库表';
COMMENT ON COLUMN lunaerp_stock_warehouse.id IS '仓库ID';
COMMENT ON COLUMN lunaerp_stock_warehouse.warehouse_no IS '仓库编号（WH+年份+4位流水，按年取号，租户内全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_stock_warehouse.name IS '仓库名称';
COMMENT ON COLUMN lunaerp_stock_warehouse.address IS '仓库地址';
COMMENT ON COLUMN lunaerp_stock_warehouse.status IS '状态（ACTIVE 启用 / INACTIVE 停用），缺省 ACTIVE';
COMMENT ON COLUMN lunaerp_stock_warehouse.remark IS '备注';
COMMENT ON COLUMN lunaerp_stock_warehouse.creator IS '创建者';
COMMENT ON COLUMN lunaerp_stock_warehouse.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_stock_warehouse.updater IS '更新者';
COMMENT ON COLUMN lunaerp_stock_warehouse.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_stock_warehouse.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_stock_warehouse.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 库位表 lunaerp_stock_warehouse_location（ADR-0018）
--    库位编码人工维护（如 A-01-02），同一仓库内唯一含软删行；
--    状态 ACTIVE/INACTIVE，不建字典。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_stock_warehouse_location (
    id            int8          NOT NULL,
    warehouse_id  int8          NOT NULL,
    code          varchar(32)   NOT NULL,
    name          varchar(64)   NULL,
    sort          int4          NULL DEFAULT 0,
    status        varchar(16)   NOT NULL DEFAULT 'ACTIVE',
    remark        varchar(500)  NULL,
    creator       varchar(64)   NULL DEFAULT '',
    create_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       varchar(64)   NULL DEFAULT '',
    update_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       int2          NOT NULL DEFAULT 0,
    tenant_id     int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_stock_warehouse_location') THEN
        ALTER TABLE lunaerp_stock_warehouse_location ADD CONSTRAINT pk_lb_stock_warehouse_location PRIMARY KEY (id);
    END IF;
END $$;

-- 库位编码在同仓库内唯一（含软删行，普通唯一索引，不带 WHERE deleted=0）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_stock_location_tenant_wh_code
    ON lunaerp_stock_warehouse_location (tenant_id, warehouse_id, code);
CREATE INDEX IF NOT EXISTS idx_lb_stock_location_warehouse_id
    ON lunaerp_stock_warehouse_location (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_lb_stock_location_status
    ON lunaerp_stock_warehouse_location (status);

COMMENT ON TABLE lunaerp_stock_warehouse_location IS '库存管理-库位表';
COMMENT ON COLUMN lunaerp_stock_warehouse_location.id IS '库位ID';
COMMENT ON COLUMN lunaerp_stock_warehouse_location.warehouse_id IS '所属仓库ID（lunaerp_stock_warehouse.id，必填）';
COMMENT ON COLUMN lunaerp_stock_warehouse_location.code IS '库位编码（同一仓库内唯一含软删行，如 A-01-02）';
COMMENT ON COLUMN lunaerp_stock_warehouse_location.name IS '库位名称';
COMMENT ON COLUMN lunaerp_stock_warehouse_location.sort IS '排序（缺省 0）';
COMMENT ON COLUMN lunaerp_stock_warehouse_location.status IS '状态（ACTIVE 启用 / INACTIVE 停用），缺省 ACTIVE';
COMMENT ON COLUMN lunaerp_stock_warehouse_location.remark IS '备注';
COMMENT ON COLUMN lunaerp_stock_warehouse_location.creator IS '创建者';
COMMENT ON COLUMN lunaerp_stock_warehouse_location.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_stock_warehouse_location.updater IS '更新者';
COMMENT ON COLUMN lunaerp_stock_warehouse_location.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_stock_warehouse_location.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_stock_warehouse_location.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 入库单主表 lunaerp_stock_in（ADR-0015, ADR-0016）
--    单号 IN-YYYYMMDD-NNN 按天取号、租户内全量唯一含软删行（ADR-0015）；
--    三态过账状态机 DRAFT/COMPLETED/CANCELLED（ADR-0016）；
--    仓库/供应商/合同/胚布单/染色单/胚布品种为逻辑关联，不建数据库外键。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_stock_in (
    id                 int8          NOT NULL,
    in_no              varchar(32)   NOT NULL,
    in_type            varchar(16)   NOT NULL,
    status             varchar(16)   NOT NULL DEFAULT 'DRAFT',
    warehouse_id       int8          NOT NULL,
    warehouse_name     varchar(128)  NULL,
    supplier_id        int8          NULL,
    supplier_name      varchar(128)  NULL,
    sales_contract_id  int8          NULL,
    contract_no        varchar(32)   NULL,
    greige_order_id    int8          NULL,
    greige_order_no    varchar(32)   NULL,
    dyeing_order_id    int8          NULL,
    greige_type_id     int8          NULL,
    greige_type_name   varchar(128)  NULL,
    total_amount       numeric(18,2) NULL,
    in_date            date          NOT NULL,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_stock_in') THEN
        ALTER TABLE lunaerp_stock_in ADD CONSTRAINT pk_lb_stock_in PRIMARY KEY (id);
    END IF;
END $$;

-- 入库单号租户内全量唯一（ADR-0015，仿 ADR-0005/0006）：普通唯一索引、不带 WHERE deleted=0，
-- 按天取号、软删不释放编号。
CREATE UNIQUE INDEX IF NOT EXISTS uk_lb_stock_in_tenant_in_no
    ON lunaerp_stock_in (tenant_id, in_no);
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_status
    ON lunaerp_stock_in (status);
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_in_date
    ON lunaerp_stock_in (in_date);
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_warehouse_id
    ON lunaerp_stock_in (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_supplier_id
    ON lunaerp_stock_in (supplier_id);
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_in_type
    ON lunaerp_stock_in (in_type);

COMMENT ON TABLE lunaerp_stock_in IS '库存管理-入库单主表';
COMMENT ON COLUMN lunaerp_stock_in.id IS '入库单ID';
COMMENT ON COLUMN lunaerp_stock_in.in_no IS '入库单号（IN-YYYYMMDD-NNN，按天取号，租户内全量唯一含软删行，ADR-0015）';
COMMENT ON COLUMN lunaerp_stock_in.in_type IS '入库类型（字典 lunaerp_stock_in_type：PURCHASE/PRODUCTION/RETURN/OTHER）';
COMMENT ON COLUMN lunaerp_stock_in.status IS '入库单状态（字典 lunaerp_stock_in_status：DRAFT/COMPLETED/CANCELLED），创建缺省 DRAFT';
COMMENT ON COLUMN lunaerp_stock_in.warehouse_id IS '入库仓库ID（lunaerp_stock_warehouse.id，必填）';
COMMENT ON COLUMN lunaerp_stock_in.warehouse_name IS '仓库名称快照';
COMMENT ON COLUMN lunaerp_stock_in.supplier_id IS '供应商ID（lunaerp_baseinfo_supplier.id，可选）';
COMMENT ON COLUMN lunaerp_stock_in.supplier_name IS '供应商公司名快照';
COMMENT ON COLUMN lunaerp_stock_in.sales_contract_id IS '关联合同ID（lunaerp_business_sales_contract.id，可选）';
COMMENT ON COLUMN lunaerp_stock_in.contract_no IS '合同号快照';
COMMENT ON COLUMN lunaerp_stock_in.greige_order_id IS '关联胚布单ID（lunaerp_production_greige_order.id，可选）';
COMMENT ON COLUMN lunaerp_stock_in.greige_order_no IS '胚布单号快照';
COMMENT ON COLUMN lunaerp_stock_in.dyeing_order_id IS '关联染色单ID（lunaerp_production_dyeing_order.id，可选，无单号快照）';
COMMENT ON COLUMN lunaerp_stock_in.greige_type_id IS '关联胚布品种ID（lunaerp_baseinfo_product_greige_type.id，可选）';
COMMENT ON COLUMN lunaerp_stock_in.greige_type_name IS '胚布品种名称快照';
COMMENT ON COLUMN lunaerp_stock_in.total_amount IS '金额合计（明细金额之和，服务端计算）';
COMMENT ON COLUMN lunaerp_stock_in.in_date IS '单据日期（必填，缺省当天）';
COMMENT ON COLUMN lunaerp_stock_in.remark IS '备注';
COMMENT ON COLUMN lunaerp_stock_in.creator IS '创建者';
COMMENT ON COLUMN lunaerp_stock_in.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_stock_in.updater IS '更新者';
COMMENT ON COLUMN lunaerp_stock_in.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_stock_in.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_stock_in.tenant_id IS '租户编号';

-- =============================================================================
-- 4. 入库明细表 lunaerp_stock_in_item（ADR-0003 聚合保存模式）
--    随入库单整单保存整体替换；删主表级联逻辑删除明细。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_stock_in_item (
    id            int8          NOT NULL,
    stock_in_id   int8          NOT NULL,
    item_type     varchar(16)   NOT NULL DEFAULT 'PRODUCT',
    product_id    int8          NULL,
    product_code  varchar(64)   NULL,
    product_name  varchar(128)  NULL,
    yarn_spec_id  int8          NULL,
    yarn_spec_name varchar(128) NULL,
    batch_no      varchar(64)   NULL,
    roll_no       varchar(64)   NULL,
    piece_count   int4          NULL DEFAULT 1,
    quantity      numeric(18,2) NOT NULL,
    unit          varchar(8)    NOT NULL,
    unit_price    numeric(18,2) NULL,
    amount        numeric(18,2) NULL,
    remark        varchar(500)  NULL,
    creator       varchar(64)   NULL DEFAULT '',
    create_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater       varchar(64)   NULL DEFAULT '',
    update_time   timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       int2          NOT NULL DEFAULT 0,
    tenant_id     int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_stock_in_item') THEN
        ALTER TABLE lunaerp_stock_in_item ADD CONSTRAINT pk_lb_stock_in_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_stock_in_item_stock_in_id
    ON lunaerp_stock_in_item (stock_in_id);
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_item_product_id
    ON lunaerp_stock_in_item (product_id);
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_item_yarn_spec_id
    ON lunaerp_stock_in_item (yarn_spec_id);
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_item_item_type
    ON lunaerp_stock_in_item (item_type);

COMMENT ON TABLE lunaerp_stock_in_item IS '库存管理-入库明细表';
COMMENT ON COLUMN lunaerp_stock_in_item.id IS '明细ID';
COMMENT ON COLUMN lunaerp_stock_in_item.stock_in_id IS '所属入库单ID（lunaerp_stock_in.id，必填）';
COMMENT ON COLUMN lunaerp_stock_in_item.item_type IS '物品类型（PRODUCT 产品 / YARN_SPEC 纱线规格），缺省 PRODUCT';
COMMENT ON COLUMN lunaerp_stock_in_item.product_id IS '关联产品ID（lunaerp_baseinfo_product.id，物品为 PRODUCT 时必填，与 yarn_spec_id 互斥）';
COMMENT ON COLUMN lunaerp_stock_in_item.product_code IS '产品编码快照';
COMMENT ON COLUMN lunaerp_stock_in_item.product_name IS '产品名称快照';
COMMENT ON COLUMN lunaerp_stock_in_item.yarn_spec_id IS '关联纱线规格ID（lunaerp_production_yarn_spec.id，物品为 YARN_SPEC 时必填，与 product_id 互斥）';
COMMENT ON COLUMN lunaerp_stock_in_item.yarn_spec_name IS '纱线规格名称快照';
COMMENT ON COLUMN lunaerp_stock_in_item.batch_no IS '批次号/缸号';
COMMENT ON COLUMN lunaerp_stock_in_item.roll_no IS '卷号';
COMMENT ON COLUMN lunaerp_stock_in_item.piece_count IS '匹数（缺省 1）';
COMMENT ON COLUMN lunaerp_stock_in_item.quantity IS '数量（必填，>0）';
COMMENT ON COLUMN lunaerp_stock_in_item.unit IS '单位（字典 lunaerp_stock_in_unit：KG/M）';
COMMENT ON COLUMN lunaerp_stock_in_item.unit_price IS '单价';
COMMENT ON COLUMN lunaerp_stock_in_item.amount IS '金额（quantity * unit_price，服务端重算）';
COMMENT ON COLUMN lunaerp_stock_in_item.remark IS '备注';
COMMENT ON COLUMN lunaerp_stock_in_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_stock_in_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_stock_in_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_stock_in_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_stock_in_item.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_stock_in_item.tenant_id IS '租户编号';

-- =============================================================================
-- 5. 库位分配放置表 lunaerp_stock_in_item_placement（ADR-0018）
--    每条明细可拆分放置于多个库位；随入库单聚合保存替换；
--    必须属于主单 warehouse_id（service 层校验）。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_stock_in_item_placement (
    id                int8          NOT NULL,
    stock_in_id       int8          NOT NULL,
    stock_in_item_id  int8          NOT NULL,
    location_id       int8          NOT NULL,
    location_code     varchar(32)   NULL,
    piece_count       int4          NULL,
    quantity          numeric(18,2) NOT NULL,
    unit              varchar(8)    NOT NULL,
    remark            varchar(500)  NULL,
    creator           varchar(64)   NULL DEFAULT '',
    create_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater           varchar(64)   NULL DEFAULT '',
    update_time       timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           int2          NOT NULL DEFAULT 0,
    tenant_id         int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lb_stock_in_item_placement') THEN
        ALTER TABLE lunaerp_stock_in_item_placement ADD CONSTRAINT pk_lb_stock_in_item_placement PRIMARY KEY (id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_lb_stock_placement_stock_in_id
    ON lunaerp_stock_in_item_placement (stock_in_id);
CREATE INDEX IF NOT EXISTS idx_lb_stock_placement_stock_in_item_id
    ON lunaerp_stock_in_item_placement (stock_in_item_id);
CREATE INDEX IF NOT EXISTS idx_lb_stock_placement_location_id
    ON lunaerp_stock_in_item_placement (location_id);

COMMENT ON TABLE lunaerp_stock_in_item_placement IS '库存管理-入库明细库位分配放置表';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.id IS '放置ID';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.stock_in_id IS '所属入库单ID（lunaerp_stock_in.id，冗余）';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.stock_in_item_id IS '所属入库明细ID（lunaerp_stock_in_item.id，必填）';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.location_id IS '关联库位ID（lunaerp_stock_warehouse_location.id，必填）';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.location_code IS '库位编码快照';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.piece_count IS '放置匹数';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.quantity IS '放置数量（必填，>0）';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.unit IS '单位（跟随明细，KG/M）';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.remark IS '备注';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.creator IS '创建者';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.updater IS '更新者';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_stock_in_item_placement.tenant_id IS '租户编号';

-- =============================================================================
-- 6. 库存流水台账表 lunaerp_stock_ledger（ADR-0017）
--    通用产品维度流水台账，只存流水不存余额；只增不改不删；
--    txn_type: STOCK_IN（入库正数）/ STOCK_IN_REVERSAL（红冲负数），内部枚举不入字典；
--    余额由 SUM(quantity) GROUP BY product_id, unit, warehouse_id 实时聚合。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_stock_ledger (
    id                int8          NOT NULL,
    item_type         varchar(16)   NOT NULL DEFAULT 'PRODUCT',
    product_id        int8          NULL,
    product_code      varchar(64)   NULL,
    product_name      varchar(128)  NULL,
    yarn_spec_id      int8          NULL,
    yarn_spec_name    varchar(128)  NULL,
    warehouse_id      int8          NOT NULL,
    warehouse_name    varchar(128)  NULL,
    location_id       int8          NULL,
    location_code     varchar(32)   NULL,
    batch_no          varchar(64)   NULL,
    txn_type          varchar(16)   NOT NULL,
    quantity          numeric(18,2) NOT NULL,
    unit              varchar(8)    NOT NULL,
    unit_price        numeric(18,2) NULL,
    amount            numeric(18,2) NULL,
    related_doc_type  varchar(32)   NOT NULL,
    related_doc_id    int8          NOT NULL,
    related_doc_no    varchar(32)   NULL,
    remark            varchar(500)  NULL,
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
        ALTER TABLE lunaerp_stock_ledger ADD CONSTRAINT pk_lb_stock_ledger PRIMARY KEY (id);
    END IF;
END $$;

-- 余额聚合主路径、红冲反查原流水、仓库维度与时间排序普通索引
CREATE INDEX IF NOT EXISTS idx_lb_stock_ledger_product_wh_unit
    ON lunaerp_stock_ledger (product_id, warehouse_id, unit);
CREATE INDEX IF NOT EXISTS idx_lb_stock_ledger_yarn_spec_wh_unit
    ON lunaerp_stock_ledger (yarn_spec_id, warehouse_id, unit);
CREATE INDEX IF NOT EXISTS idx_lb_stock_ledger_item_type
    ON lunaerp_stock_ledger (item_type);
CREATE INDEX IF NOT EXISTS idx_lb_stock_ledger_related_doc
    ON lunaerp_stock_ledger (related_doc_type, related_doc_id);
CREATE INDEX IF NOT EXISTS idx_lb_stock_ledger_warehouse_id
    ON lunaerp_stock_ledger (warehouse_id);
CREATE INDEX IF NOT EXISTS idx_lb_stock_ledger_create_time
    ON lunaerp_stock_ledger (create_time);

COMMENT ON TABLE lunaerp_stock_ledger IS '库存管理-库存流水台账表';
COMMENT ON COLUMN lunaerp_stock_ledger.id IS '流水ID';
COMMENT ON COLUMN lunaerp_stock_ledger.item_type IS '物品类型（PRODUCT 产品 / YARN_SPEC 纱线规格），缺省 PRODUCT';
COMMENT ON COLUMN lunaerp_stock_ledger.product_id IS '关联产品ID（lunaerp_baseinfo_product.id，物品为 PRODUCT 时必填，与 yarn_spec_id 互斥）';
COMMENT ON COLUMN lunaerp_stock_ledger.product_code IS '产品编码快照';
COMMENT ON COLUMN lunaerp_stock_ledger.product_name IS '产品名称快照';
COMMENT ON COLUMN lunaerp_stock_ledger.yarn_spec_id IS '关联纱线规格ID（lunaerp_production_yarn_spec.id，物品为 YARN_SPEC 时必填，与 product_id 互斥）';
COMMENT ON COLUMN lunaerp_stock_ledger.yarn_spec_name IS '纱线规格名称快照';
COMMENT ON COLUMN lunaerp_stock_ledger.warehouse_id IS '仓库ID（lunaerp_stock_warehouse.id，必填）';
COMMENT ON COLUMN lunaerp_stock_ledger.warehouse_name IS '仓库名称快照';
COMMENT ON COLUMN lunaerp_stock_ledger.location_id IS '库位ID（lunaerp_stock_warehouse_location.id，可空）';
COMMENT ON COLUMN lunaerp_stock_ledger.location_code IS '库位编码快照';
COMMENT ON COLUMN lunaerp_stock_ledger.batch_no IS '批次/缸号';
COMMENT ON COLUMN lunaerp_stock_ledger.txn_type IS '交易类型（STOCK_IN 入库 / STOCK_IN_REVERSAL 红冲），内部枚举不入字典';
COMMENT ON COLUMN lunaerp_stock_ledger.quantity IS '带符号数量（入库正、红冲负）';
COMMENT ON COLUMN lunaerp_stock_ledger.unit IS '单位（KG/M）';
COMMENT ON COLUMN lunaerp_stock_ledger.unit_price IS '单价快照';
COMMENT ON COLUMN lunaerp_stock_ledger.amount IS '金额快照（带符号，红冲负）';
COMMENT ON COLUMN lunaerp_stock_ledger.related_doc_type IS '关联单据类型（本期固定 STOCK_IN）';
COMMENT ON COLUMN lunaerp_stock_ledger.related_doc_id IS '关联入库单ID（lunaerp_stock_in.id）';
COMMENT ON COLUMN lunaerp_stock_ledger.related_doc_no IS '关联入库单号快照';
COMMENT ON COLUMN lunaerp_stock_ledger.remark IS '备注';
COMMENT ON COLUMN lunaerp_stock_ledger.creator IS '创建者';
COMMENT ON COLUMN lunaerp_stock_ledger.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_stock_ledger.updater IS '更新者';
COMMENT ON COLUMN lunaerp_stock_ledger.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_stock_ledger.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_stock_ledger.tenant_id IS '租户编号';

-- =============================================================================
-- 7. 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_stock_warehouse_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_stock_warehouse_location_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_stock_in_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_stock_in_item_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_stock_in_item_placement_seq;
CREATE SEQUENCE IF NOT EXISTS lunaerp_stock_ledger_seq;

-- =============================================================================
-- 8. 菜单与按钮（system_menu，可重复执行）
--    10300 顶级库存管理目录（sort 103，与 10000 基本信息、10100 业务管理、10200 生产管理平级）。
--    10310 入库管理菜单（挂载 query 权限）及 10311-10314 创建/修改/删除/审核按钮。
--    10320 库存查询菜单（挂载 query 权限，余额+流水两 Tab）。
--    10330 仓库管理菜单（挂载 query 权限，仓库/库位主从同页）及 10331-10336 仓库/库位 CRUD 按钮。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10300, 10310, 10311, 10312, 10313, 10314, 10320, 10330, 10331, 10332, 10333, 10334, 10335, 10336);

-- 目录：库存管理（parent_id=0 顶级目录）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10300, '库存管理', '', 1, 103, 0, '/stock', 'ep:box', NULL, NULL, 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');

-- 菜单：入库管理（query 权限挂在菜单本身）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10310, '入库管理', 'lunaerp:stock:stock-in:query', 2, 1, 10300, 'stock-in', 'ep:box', 'lunaerp-elmui/stock/stock-in/index', 'LunaerpElmuiStockStockIn', 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
-- 按钮：入库主表 创建/修改/删除/审核
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10311, '入库创建', 'lunaerp:stock:stock-in:create', 3, 1, 10310, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10312, '入库修改', 'lunaerp:stock:stock-in:update', 3, 2, 10310, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10313, '入库删除', 'lunaerp:stock:stock-in:delete', 3, 3, 10310, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10314, '入库审核', 'lunaerp:stock:stock-in:audit', 3, 4, 10310, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');

-- 菜单：库存查询（余额 + 流水两个 Tab，仅 query 权限）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10320, '库存查询', 'lunaerp:stock:inventory-balance:query', 2, 2, 10300, 'inventory-balance', 'ep:data-analysis', 'lunaerp-elmui/stock/inventory-balance/index', 'LunaerpElmuiStockInventoryBalance', 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');

-- 菜单：仓库管理（仓库/库位主从同页，query 权限挂在菜单本身）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10330, '仓库管理', 'lunaerp:stock:warehouse:query', 2, 3, 10300, 'warehouse', 'ep:office-building', 'lunaerp-elmui/stock/warehouse/index', 'LunaerpElmuiStockWarehouse', 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
-- 按钮：仓库 创建/修改/删除
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10331, '仓库创建', 'lunaerp:stock:warehouse:create', 3, 1, 10330, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10332, '仓库修改', 'lunaerp:stock:warehouse:update', 3, 2, 10330, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10333, '仓库删除', 'lunaerp:stock:warehouse:delete', 3, 3, 10330, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
-- 按钮：库位 创建/修改/删除（挂在 10330 仓库管理菜单下）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10334, '库位创建', 'lunaerp:stock:warehouse-location:create', 3, 10, 10330, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10335, '库位修改', 'lunaerp:stock:warehouse-location:update', 3, 11, 10330, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted) VALUES (10336, '库位删除', 'lunaerp:stock:warehouse-location:delete', 3, 12, 10330, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 9. 字典类型与字典数据（system_dict_type / system_dict_data，可重复执行）
--    dict_type id 2233（入库单状态）、2234（入库类型）、2235（入库单位），
--    dict_data id 3592-3594（状态）、3595-3598（类型）、3599-3600（单位）。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2233, 2234, 2235);
DELETE FROM system_dict_data WHERE dict_type IN ('lunaerp_stock_in_status', 'lunaerp_stock_in_type', 'lunaerp_stock_in_unit')
    OR id IN (3592, 3593, 3594, 3595, 3596, 3597, 3598, 3599, 3600);

-- 字典类型：入库单状态
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2233, '入库单状态', 'lunaerp_stock_in_status', 0, 'LunaERP 库存管理-入库单状态', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0', NULL);
-- 入库单状态：草稿(warning) / 已审核(success) / 已取消(danger)（三态过账状态机，ADR-0016）
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3592, 1, '草稿', 'DRAFT', 'lunaerp_stock_in_status', 0, 'warning', '', NULL, 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3593, 2, '已审核', 'COMPLETED', 'lunaerp_stock_in_status', 0, 'success', '', NULL, 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3594, 3, '已取消', 'CANCELLED', 'lunaerp_stock_in_status', 0, 'danger', '', NULL, 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');

-- 字典类型：入库类型
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2234, '入库类型', 'lunaerp_stock_in_type', 0, 'LunaERP 库存管理-入库类型', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0', NULL);
-- 入库类型：采购入库(primary) / 生产入库(success) / 退货入库(warning) / 其他入库(info)
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3595, 1, '采购入库', 'PURCHASE', 'lunaerp_stock_in_type', 0, 'primary', '', NULL, 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3596, 2, '生产入库', 'PRODUCTION', 'lunaerp_stock_in_type', 0, 'success', '', NULL, 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3597, 3, '退货入库', 'RETURN', 'lunaerp_stock_in_type', 0, 'warning', '', NULL, 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3598, 4, '其他入库', 'OTHER', 'lunaerp_stock_in_type', 0, 'info', '', NULL, 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');

-- 字典类型：入库单位
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2235, '入库单位', 'lunaerp_stock_in_unit', 0, 'LunaERP 库存管理-入库单位', 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0', NULL);
-- 入库单位：千克 / 米（库存域独立单位字典，不复用销售合同单位）
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3599, 1, '千克', 'KG', 'lunaerp_stock_in_unit', 0, '', '', NULL, 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3600, 2, '米', 'M', 'lunaerp_stock_in_unit', 0, '', '', NULL, 'admin', '2026-09-24 00:00:00', '1', '2026-09-24 00:00:00', '0');

-- 字典类型：库存物品类型（ADR-0026，使用 2247 避让发货域 2245/2246 与质检确认 2236/2237）
DELETE FROM system_dict_type WHERE id = 2247 OR type = 'lunaerp_stock_item_type';
DELETE FROM system_dict_data WHERE dict_type = 'lunaerp_stock_item_type' OR id IN (3601, 3602);
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time) VALUES (2247, '库存物品类型', 'lunaerp_stock_item_type', 0, 'LunaERP 库存管理-物品类型（产品/纱线规格）', 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0', NULL);
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3601, 1, '产品', 'PRODUCT', 'lunaerp_stock_item_type', 0, 'primary', '', NULL, 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted) VALUES (3602, 2, '纱线规格', 'YARN_SPEC', 'lunaerp_stock_item_type', 0, 'warning', '', NULL, 'admin', '2026-09-26 00:00:00', '1', '2026-09-26 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 10. 增量脚本：支持物品类型（产品 PRODUCT / 纱线规格 YARN_SPEC，ADR-0026）
--     入库明细表与库存流水表扩展 item_type, yarn_spec_id, yarn_spec_name；
--     product_id 变为可空（与 yarn_spec_id 互斥）。
-- =============================================================================
ALTER TABLE lunaerp_stock_in_item ADD COLUMN IF NOT EXISTS item_type varchar(16) NOT NULL DEFAULT 'PRODUCT';
ALTER TABLE lunaerp_stock_in_item ALTER COLUMN product_id DROP NOT NULL;
ALTER TABLE lunaerp_stock_in_item ADD COLUMN IF NOT EXISTS yarn_spec_id int8 NULL;
ALTER TABLE lunaerp_stock_in_item ADD COLUMN IF NOT EXISTS yarn_spec_name varchar(128) NULL;
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_item_yarn_spec_id ON lunaerp_stock_in_item (yarn_spec_id);
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_item_item_type ON lunaerp_stock_in_item (item_type);

ALTER TABLE lunaerp_stock_ledger ADD COLUMN IF NOT EXISTS item_type varchar(16) NOT NULL DEFAULT 'PRODUCT';
ALTER TABLE lunaerp_stock_ledger ALTER COLUMN product_id DROP NOT NULL;
ALTER TABLE lunaerp_stock_ledger ADD COLUMN IF NOT EXISTS yarn_spec_id int8 NULL;
ALTER TABLE lunaerp_stock_ledger ADD COLUMN IF NOT EXISTS yarn_spec_name varchar(128) NULL;
CREATE INDEX IF NOT EXISTS idx_lb_stock_ledger_yarn_spec_wh_unit ON lunaerp_stock_ledger (yarn_spec_id, warehouse_id, unit);
CREATE INDEX IF NOT EXISTS idx_lb_stock_ledger_item_type ON lunaerp_stock_ledger (item_type);

COMMENT ON COLUMN lunaerp_stock_in_item.item_type IS '物品类型（PRODUCT 产品 / YARN_SPEC 纱线规格），缺省 PRODUCT';
COMMENT ON COLUMN lunaerp_stock_in_item.yarn_spec_id IS '关联纱线规格ID（lunaerp_production_yarn_spec.id，物品为 YARN_SPEC 时必填，与 product_id 互斥）';
COMMENT ON COLUMN lunaerp_stock_in_item.yarn_spec_name IS '纱线规格名称快照';

COMMENT ON COLUMN lunaerp_stock_ledger.item_type IS '物品类型（PRODUCT 产品 / YARN_SPEC 纱线规格），缺省 PRODUCT';
COMMENT ON COLUMN lunaerp_stock_ledger.yarn_spec_id IS '关联纱线规格ID（lunaerp_production_yarn_spec.id，物品为 YARN_SPEC 时必填，与 product_id 互斥）';
COMMENT ON COLUMN lunaerp_stock_ledger.yarn_spec_name IS '纱线规格名称快照';

-- =============================================================================
-- 11. 增量脚本：支持检验单生成胚布入库单与布票血缘（ADR-0024, ADR-0025）
--     入库单主表扩展 inspection_order_id, inspection_order_no；
--     入库明细表扩展 piece_no, weaving_ticket_id, inspection_order_item_id。
-- =============================================================================
ALTER TABLE lunaerp_stock_in ADD COLUMN IF NOT EXISTS inspection_order_id int8 NULL;
ALTER TABLE lunaerp_stock_in ADD COLUMN IF NOT EXISTS inspection_order_no varchar(64) NULL;
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_inspection_order_id ON lunaerp_stock_in (inspection_order_id);

ALTER TABLE lunaerp_stock_in_item ADD COLUMN IF NOT EXISTS piece_no varchar(64) NULL;
ALTER TABLE lunaerp_stock_in_item ADD COLUMN IF NOT EXISTS weaving_ticket_id int8 NULL;
ALTER TABLE lunaerp_stock_in_item ADD COLUMN IF NOT EXISTS inspection_order_item_id int8 NULL;
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_item_weaving_ticket_id ON lunaerp_stock_in_item (weaving_ticket_id);
CREATE INDEX IF NOT EXISTS idx_lb_stock_in_item_inspection_order_item_id ON lunaerp_stock_in_item (inspection_order_item_id);

COMMENT ON COLUMN lunaerp_stock_in.inspection_order_id IS '关联检验单ID（lunaerp_production_inspection_order.id）';
COMMENT ON COLUMN lunaerp_stock_in.inspection_order_no IS '关联检验单号快照';
COMMENT ON COLUMN lunaerp_stock_in_item.piece_no IS '匹号';
COMMENT ON COLUMN lunaerp_stock_in_item.weaving_ticket_id IS '关联织造布票ID（lunaerp_production_weaving_ticket.id）';
COMMENT ON COLUMN lunaerp_stock_in_item.inspection_order_item_id IS '关联检验明细ID（lunaerp_production_inspection_order_item.id）';

