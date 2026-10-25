-- =============================================================================
-- 模块：LunaERP - 售前链 - 成本卡与智能报价（纱价主档 + 成本卡 + 报价单）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + CREATE UNIQUE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 避让声明：错误码 1-030-034（避让打色 032、出口单证 033）；菜单 10132-10140、10245-10248；
--           字典类型 2262-2268（避让打色 2255-2257、出口单证 2258-2261）；
--           字典数据 3770-3791（避让打色 3736-3749、出口单证 3750-3764）。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 0. 前置可空加列（3 ALTER：BOM 行 5 列测算参数，客户档案默认毛利率，销售合同报价来源）
-- =============================================================================
ALTER TABLE lunaerp_production_yarn_recipe_bom_item ADD COLUMN IF NOT EXISTS waste_rate numeric(7,2);
ALTER TABLE lunaerp_production_yarn_recipe_bom_item ADD COLUMN IF NOT EXISTS direction varchar(16);
ALTER TABLE lunaerp_production_yarn_recipe_bom_item ADD COLUMN IF NOT EXISTS density numeric(18,2);
ALTER TABLE lunaerp_production_yarn_recipe_bom_item ADD COLUMN IF NOT EXISTS crimp_rate numeric(7,2);
ALTER TABLE lunaerp_production_yarn_recipe_bom_item ADD COLUMN IF NOT EXISTS consumption_kg_per_meter numeric(18,6);

COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.waste_rate IS '投入损耗率%（如 5.00 表示 5%，可空）';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.direction IS '经纬方向（GENERAL 综合 / WARP 经向 / WEFT 纬向，可空）';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.density IS '密度（根/10cm 或根/inch，精算参数，可空）';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.crimp_rate IS '织缩率%（如 6.50 表示 6.5%，可空）';
COMMENT ON COLUMN lunaerp_production_yarn_recipe_bom_item.consumption_kg_per_meter IS '每米用量 KG/M（经纬精算直接用量，可空）';

ALTER TABLE lunaerp_baseinfo_customer ADD COLUMN IF NOT EXISTS default_margin_rate numeric(9,6);
COMMENT ON COLUMN lunaerp_baseinfo_customer.default_margin_rate IS '默认目标毛利率（小数如 0.150000 表示 15%，可空，空时回退全局 15%）';

ALTER TABLE lunaerp_business_sales_contract ADD COLUMN IF NOT EXISTS source_quotation_id int8;
ALTER TABLE lunaerp_business_sales_contract ADD COLUMN IF NOT EXISTS source_quotation_no varchar(32);
COMMENT ON COLUMN lunaerp_business_sales_contract.source_quotation_id IS '来源报价单ID（逻辑关联，追溯用，可空）';
COMMENT ON COLUMN lunaerp_business_sales_contract.source_quotation_no IS '来源报价单号快照（追溯用，可空）';

-- =============================================================================
-- 1. 新表 1：生产域 - 纱价表 lunaerp_production_yarn_price
--    记录规格 + 价类 + 生效日期 + 历史价格，元/KG；
--    部分唯一索引 WHERE deleted=0（删错可重录）；取价走 (spec_id, price_type, effective_date DESC)。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_production_yarn_price (
    id                 int8          NOT NULL,
    yarn_spec_id       int8          NOT NULL,
    yarn_type_id       int8          NOT NULL,
    spec_name          varchar(128)  NOT NULL,
    price_type         varchar(16)   NOT NULL DEFAULT 'PURCHASE',
    price              numeric(18,4) NOT NULL,
    effective_date     date          NOT NULL,
    source             varchar(16)   NOT NULL DEFAULT 'HAND',
    remark             varchar(256)  NULL,
    creator            varchar(64)   NULL DEFAULT '',
    create_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater            varchar(64)   NULL DEFAULT '',
    update_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            int2          NOT NULL DEFAULT 0,
    tenant_id          int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_production_yarn_price') THEN
        ALTER TABLE lunaerp_production_yarn_price ADD CONSTRAINT pk_lunaerp_production_yarn_price PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_production_yarn_price_seq;

-- 纱价同规格同价类同生效日期普通唯一（WHERE deleted=0，删错可重录）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lpyp_spec_type_date
    ON lunaerp_production_yarn_price (tenant_id, yarn_spec_id, price_type, effective_date)
    WHERE deleted = 0;

-- 取价与历史查询复合索引
CREATE INDEX IF NOT EXISTS idx_lpyp_lookup
    ON lunaerp_production_yarn_price (yarn_spec_id, price_type, effective_date DESC);
CREATE INDEX IF NOT EXISTS idx_lpyp_tenant_spec
    ON lunaerp_production_yarn_price (tenant_id, yarn_spec_id);

COMMENT ON TABLE lunaerp_production_yarn_price IS '生产管理-纱价主档表';
COMMENT ON COLUMN lunaerp_production_yarn_price.id IS '纱价记录ID';
COMMENT ON COLUMN lunaerp_production_yarn_price.yarn_spec_id IS '关联纱线规格ID';
COMMENT ON COLUMN lunaerp_production_yarn_price.yarn_type_id IS '冗余纱线材质ID（分组与快速筛选）';
COMMENT ON COLUMN lunaerp_production_yarn_price.spec_name IS '规格名称快照';
COMMENT ON COLUMN lunaerp_production_yarn_price.price_type IS '价类（字典 lunaerp_yarn_price_type：PURCHASE 采购参考价 / MARKET 市场参考价）';
COMMENT ON COLUMN lunaerp_production_yarn_price.price IS '单价，元/KG，必须 >0';
COMMENT ON COLUMN lunaerp_production_yarn_price.effective_date IS '生效日期';
COMMENT ON COLUMN lunaerp_production_yarn_price.source IS '来源（HAND 手工 / EXCEL 导入 / PURCHASE_AGG 采购聚合）';
COMMENT ON COLUMN lunaerp_production_yarn_price.remark IS '备注';
COMMENT ON COLUMN lunaerp_production_yarn_price.creator IS '创建者';
COMMENT ON COLUMN lunaerp_production_yarn_price.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_production_yarn_price.updater IS '更新者';
COMMENT ON COLUMN lunaerp_production_yarn_price.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_production_yarn_price.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_production_yarn_price.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 新表 2：业务域 - 成本卡主表 lunaerp_business_cost_card
--    卡号 CC{yyyy}{4位流水} 全量唯一含软删行（ADR-0004）；
--    部分唯一索引 WHERE status='EFFECTIVE' AND deleted=0（同产品同时仅一张生效卡）；
--    三态状态机 DRAFT / EFFECTIVE / EXPIRED。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_cost_card (
    id                         int8          NOT NULL,
    card_no                    varchar(32)   NOT NULL,
    product_id                 int8          NOT NULL,
    product_code               varchar(64)   NOT NULL,
    product_name               varchar(128)  NOT NULL,
    composition                varchar(256)  NULL,
    width_snapshot             varchar(64)   NULL,
    weight_snapshot            varchar(64)   NULL,
    fabric_construction        varchar(16)   NOT NULL,
    woven_calculation_mode     varchar(16)   NULL,
    yarn_price_type            varchar(16)   NOT NULL DEFAULT 'PURCHASE',
    greige_weight_gsm          numeric(18,2) NULL,
    greige_width_cm            numeric(18,2) NULL,
    kg_per_meter               numeric(18,6) NULL,
    tax_rate                   numeric(9,6)  NOT NULL DEFAULT 0,
    basis                      varchar(8)    NOT NULL,
    yarn_cost_per_kg           numeric(18,4) NOT NULL DEFAULT 0,
    yarn_cost_per_meter        numeric(18,4) NULL,
    total_fee_per_kg           numeric(18,4) NOT NULL DEFAULT 0,
    total_fee_per_meter        numeric(18,4) NULL,
    unit_cost_per_kg           numeric(18,4) NOT NULL DEFAULT 0,
    unit_cost_per_kg_with_tax  numeric(18,4) NOT NULL DEFAULT 0,
    unit_cost_per_meter        numeric(18,4) NULL,
    unit_cost_per_meter_with_tax numeric(18,4) NULL,
    missing_price_count        int4          NOT NULL DEFAULT 0,
    effective_date             date          NULL,
    status                     varchar(16)   NOT NULL DEFAULT 'DRAFT',
    remarks                    varchar(1000) NULL,
    creator                    varchar(64)   NULL DEFAULT '',
    create_time                timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                    varchar(64)   NULL DEFAULT '',
    update_time                timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                    int2          NOT NULL DEFAULT 0,
    tenant_id                  int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_business_cost_card') THEN
        ALTER TABLE lunaerp_business_cost_card ADD CONSTRAINT pk_lunaerp_business_cost_card PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_cost_card_seq;

-- 成本卡号租户内全量唯一（含软删行，ADR-0004）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lbcc_card_no
    ON lunaerp_business_cost_card (tenant_id, card_no);

-- 同产品同时仅一张 EFFECTIVE 生效卡（部分唯一索引）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lbcc_product_effective
    ON lunaerp_business_cost_card (tenant_id, product_id)
    WHERE status = 'EFFECTIVE' AND deleted = 0;

CREATE INDEX IF NOT EXISTS idx_lbcc_product_id
    ON lunaerp_business_cost_card (product_id);
CREATE INDEX IF NOT EXISTS idx_lbcc_status
    ON lunaerp_business_cost_card (status);

COMMENT ON TABLE lunaerp_business_cost_card IS '业务管理-成本卡主表';
COMMENT ON COLUMN lunaerp_business_cost_card.id IS '成本卡ID';
COMMENT ON COLUMN lunaerp_business_cost_card.card_no IS '成本卡号（CC{yyyy}NNNN，全量唯一含软删行）';
COMMENT ON COLUMN lunaerp_business_cost_card.product_id IS '产品档案逻辑关联ID';
COMMENT ON COLUMN lunaerp_business_cost_card.product_code IS '产品编号快照';
COMMENT ON COLUMN lunaerp_business_cost_card.product_name IS '品名快照';
COMMENT ON COLUMN lunaerp_business_cost_card.composition IS '成分快照';
COMMENT ON COLUMN lunaerp_business_cost_card.width_snapshot IS '门幅文本快照';
COMMENT ON COLUMN lunaerp_business_cost_card.weight_snapshot IS '克重文本快照';
COMMENT ON COLUMN lunaerp_business_cost_card.fabric_construction IS '织物结构（KNIT 针织 / WOVEN 梭织）';
COMMENT ON COLUMN lunaerp_business_cost_card.woven_calculation_mode IS '梭织模式（SIMPLE 简易 / PROFESSIONAL 经纬精算）';
COMMENT ON COLUMN lunaerp_business_cost_card.yarn_price_type IS '本卡取价价类（PURCHASE 采购参考价 / MARKET 市场参考价）';
COMMENT ON COLUMN lunaerp_business_cost_card.greige_weight_gsm IS '坯布克重 g/m²';
COMMENT ON COLUMN lunaerp_business_cost_card.greige_width_cm IS '坯布门幅 CM';
COMMENT ON COLUMN lunaerp_business_cost_card.kg_per_meter IS '每米重量 KG/M（克重×门幅/100000）';
COMMENT ON COLUMN lunaerp_business_cost_card.tax_rate IS '税率（如 0.13 表示 13%）';
COMMENT ON COLUMN lunaerp_business_cost_card.basis IS '计费基准口径（KG 针织 / METER 梭织）';
COMMENT ON COLUMN lunaerp_business_cost_card.yarn_cost_per_kg IS '纱线成本 元/KG';
COMMENT ON COLUMN lunaerp_business_cost_card.yarn_cost_per_meter IS '纱线成本 元/M';
COMMENT ON COLUMN lunaerp_business_cost_card.total_fee_per_kg IS '费用合计 元/KG';
COMMENT ON COLUMN lunaerp_business_cost_card.total_fee_per_meter IS '费用合计 元/M';
COMMENT ON COLUMN lunaerp_business_cost_card.unit_cost_per_kg IS '不含税单位成本 元/KG';
COMMENT ON COLUMN lunaerp_business_cost_card.unit_cost_per_kg_with_tax IS '含税单位成本 元/KG';
COMMENT ON COLUMN lunaerp_business_cost_card.unit_cost_per_meter IS '不含税单位成本 元/M';
COMMENT ON COLUMN lunaerp_business_cost_card.unit_cost_per_meter_with_tax IS '含税单位成本 元/M';
COMMENT ON COLUMN lunaerp_business_cost_card.missing_price_count IS '缺价纱线行数';
COMMENT ON COLUMN lunaerp_business_cost_card.effective_date IS '生效日期';
COMMENT ON COLUMN lunaerp_business_cost_card.status IS '状态（字典 lunaerp_cost_card_status：DRAFT/EFFECTIVE/EXPIRED）';
COMMENT ON COLUMN lunaerp_business_cost_card.remarks IS '备注';
COMMENT ON COLUMN lunaerp_business_cost_card.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_cost_card.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_cost_card.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_cost_card.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_cost_card.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_cost_card.tenant_id IS '租户编号';

-- =============================================================================
-- 3. 新表 3：业务域 - 成本卡纱线行子表 lunaerp_business_cost_card_yarn
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_cost_card_yarn (
    id                         int8          NOT NULL,
    card_id                    int8          NOT NULL,
    line_no                    int4          NOT NULL DEFAULT 1,
    yarn_type_id               int8          NOT NULL,
    yarn_spec_id               int8          NOT NULL,
    yarn_spec_name             varchar(128)  NOT NULL,
    direction                  varchar(16)   NULL DEFAULT 'GENERAL',
    ratio                      numeric(9,4)  NOT NULL DEFAULT 0,
    density                    numeric(18,2) NULL,
    crimp_rate                 numeric(7,2)  NULL,
    waste_rate                 numeric(7,2)  NULL DEFAULT 0,
    consumption_kg_per_meter   numeric(18,6) NULL,
    price                      numeric(18,4) NOT NULL DEFAULT 0,
    price_type                 varchar(16)   NULL,
    price_source               varchar(32)   NULL DEFAULT 'PRICE_TABLE',
    price_effective_date       date          NULL,
    contribution_per_kg        numeric(18,4) NOT NULL DEFAULT 0,
    contribution_per_meter     numeric(18,4) NULL,
    creator                    varchar(64)   NULL DEFAULT '',
    create_time                timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                    varchar(64)   NULL DEFAULT '',
    update_time                timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                    int2          NOT NULL DEFAULT 0,
    tenant_id                  int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_business_cost_card_yarn') THEN
        ALTER TABLE lunaerp_business_cost_card_yarn ADD CONSTRAINT pk_lunaerp_business_cost_card_yarn PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_cost_card_yarn_seq;
CREATE INDEX IF NOT EXISTS idx_lbccy_card_id ON lunaerp_business_cost_card_yarn (card_id);

COMMENT ON TABLE lunaerp_business_cost_card_yarn IS '业务管理-成本卡纱线明细行表';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.id IS '纱线明细行ID';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.card_id IS '所属成本卡ID';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.line_no IS '行号';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.yarn_type_id IS '纱线材质ID';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.yarn_spec_id IS '纱线规格ID';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.yarn_spec_name IS '纱线规格名称快照';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.direction IS '经纬方向（GENERAL 综合 / WARP 经向 / WEFT 纬向）';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.ratio IS '配比百分比（49 = 49%）';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.density IS '密度';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.crimp_rate IS '织缩率%';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.waste_rate IS '投入损耗率%';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.consumption_kg_per_meter IS '每米用量 KG/M';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.price IS '取价快照 元/KG';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.price_type IS '取价价类';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.price_source IS '价格来源（PRICE_TABLE 纱价表 / SPEC_REF_FALLBACK 规格参考价回退 / MANUAL 手工）';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.price_effective_date IS '取价生效日期';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.contribution_per_kg IS '行成本贡献 元/KG';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.contribution_per_meter IS '行成本贡献 元/M';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_cost_card_yarn.tenant_id IS '租户编号';

-- =============================================================================
-- 4. 新表 4：业务域 - 成本卡费用行子表 lunaerp_business_cost_card_fee
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_cost_card_fee (
    id                         int8          NOT NULL,
    card_id                    int8          NOT NULL,
    line_no                    int4          NOT NULL DEFAULT 1,
    fee_type                   varchar(24)   NOT NULL,
    fee_unit                   varchar(10)   NOT NULL,
    price                      numeric(18,4) NOT NULL DEFAULT 0,
    loss_rate                  numeric(7,2)  NOT NULL DEFAULT 0,
    contribution_per_kg        numeric(18,4) NULL,
    contribution_per_meter     numeric(18,4) NULL,
    remark                     varchar(256)  NULL,
    creator                    varchar(64)   NULL DEFAULT '',
    create_time                timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                    varchar(64)   NULL DEFAULT '',
    update_time                timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                    int2          NOT NULL DEFAULT 0,
    tenant_id                  int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_business_cost_card_fee') THEN
        ALTER TABLE lunaerp_business_cost_card_fee ADD CONSTRAINT pk_lunaerp_business_cost_card_fee PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_cost_card_fee_seq;
CREATE INDEX IF NOT EXISTS idx_lbccf_card_id ON lunaerp_business_cost_card_fee (card_id);

COMMENT ON TABLE lunaerp_business_cost_card_fee IS '业务管理-成本卡费用明细行表';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.id IS '费用明细行ID';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.card_id IS '所属成本卡ID';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.line_no IS '行号';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.fee_type IS '费用类型（字典 lunaerp_cost_card_fee_type：WEAVING/DYEING/SETTING/INSPECTION/PACKAGING/FREIGHT）';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.fee_unit IS '计费单位（字典 lunaerp_cost_fee_unit：PER_KG / PER_METER）';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.price IS '单价，必须 >= 0';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.loss_rate IS '投入损耗率%';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.contribution_per_kg IS '行费用贡献 元/KG';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.contribution_per_meter IS '行费用贡献 元/M';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.remark IS '备注';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_cost_card_fee.tenant_id IS '租户编号';

-- =============================================================================
-- 5. 新表 5：业务域 - 报价单主表 lunaerp_business_quotation
--    单号 QT{yyyy}{4位流水}，同号多版本共享；(tenant_id, quotation_no, version_no) 全量唯一含软删行；
--    部分唯一索引 WHERE status='DRAFT' AND deleted=0（同号至多一草稿）；
--    数据范围接线（ADR-0046）：salesperson_user_id / dept_id 部分索引；
--    四态状态机 DRAFT / CONFIRMED / SUPERSEDED / CANCELLED。
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_quotation (
    id                 int8          NOT NULL,
    quotation_no       varchar(32)   NOT NULL,
    version_no         int4          NOT NULL DEFAULT 1,
    customer_id        int8          NOT NULL,
    client_name        varchar(128)  NOT NULL,
    client_contact     varchar(64)   NULL,
    phone              varchar(32)   NULL,
    salesperson_user_id int8         NULL,
    dept_id            int8          NULL,
    quotation_date     date          NOT NULL,
    valid_from         date          NOT NULL,
    valid_to           date          NULL,
    currency           varchar(8)    NOT NULL DEFAULT 'RMB',
    exchange_rate      numeric(18,6) NOT NULL DEFAULT 1,
    total_quantity     numeric(18,2) NOT NULL DEFAULT 0,
    total_amount       numeric(18,4) NOT NULL DEFAULT 0,
    total_cost_amount  numeric(18,4) NOT NULL DEFAULT 0,
    avg_margin_rate    numeric(9,6)  NULL,
    status             varchar(16)   NOT NULL DEFAULT 'DRAFT',
    remarks            varchar(1000) NULL,
    creator            varchar(64)   NULL DEFAULT '',
    create_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater            varchar(64)   NULL DEFAULT '',
    update_time        timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted            int2          NOT NULL DEFAULT 0,
    tenant_id          int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_business_quotation') THEN
        ALTER TABLE lunaerp_business_quotation ADD CONSTRAINT pk_lunaerp_business_quotation PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_quotation_seq;

-- 报价单号+版本号全量唯一（含软删行，ADR-0004）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lbq_no_version
    ON lunaerp_business_quotation (tenant_id, quotation_no, version_no);

-- 同号至多一草稿（部分唯一索引）
CREATE UNIQUE INDEX IF NOT EXISTS uk_lbq_no_draft
    ON lunaerp_business_quotation (tenant_id, quotation_no)
    WHERE status = 'DRAFT' AND deleted = 0;

-- 销售归属链数据范围部分索引（ADR-0046）
CREATE INDEX IF NOT EXISTS idx_lbq_dept_id
    ON lunaerp_business_quotation (tenant_id, dept_id)
    WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lbq_salesperson
    ON lunaerp_business_quotation (tenant_id, salesperson_user_id)
    WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_lbq_customer_id
    ON lunaerp_business_quotation (customer_id);
CREATE INDEX IF NOT EXISTS idx_lbq_status
    ON lunaerp_business_quotation (status);
CREATE INDEX IF NOT EXISTS idx_lbq_quotation_date
    ON lunaerp_business_quotation (quotation_date);

COMMENT ON TABLE lunaerp_business_quotation IS '业务管理-报价单主表';
COMMENT ON COLUMN lunaerp_business_quotation.id IS '报价单ID';
COMMENT ON COLUMN lunaerp_business_quotation.quotation_no IS '报价单号（QT{yyyy}NNNN，同号多版本共享）';
COMMENT ON COLUMN lunaerp_business_quotation.version_no IS '版本号（首版为1，修订自增）';
COMMENT ON COLUMN lunaerp_business_quotation.customer_id IS '客户档案ID';
COMMENT ON COLUMN lunaerp_business_quotation.client_name IS '客户名快照';
COMMENT ON COLUMN lunaerp_business_quotation.client_contact IS '客户联系人快照';
COMMENT ON COLUMN lunaerp_business_quotation.phone IS '联系电话快照';
COMMENT ON COLUMN lunaerp_business_quotation.salesperson_user_id IS '归属业务员（数据范围 user 列）';
COMMENT ON COLUMN lunaerp_business_quotation.dept_id IS '归属部门（数据范围 dept 列，服务端推导）';
COMMENT ON COLUMN lunaerp_business_quotation.quotation_date IS '报价日期';
COMMENT ON COLUMN lunaerp_business_quotation.valid_from IS '有效期自';
COMMENT ON COLUMN lunaerp_business_quotation.valid_to IS '有效期至（null 表示长期有效）';
COMMENT ON COLUMN lunaerp_business_quotation.currency IS '报价币种（复用销售合同币种字典，缺省 RMB）';
COMMENT ON COLUMN lunaerp_business_quotation.exchange_rate IS '汇率（1 外币兑换人民币，RMB 为 1）';
COMMENT ON COLUMN lunaerp_business_quotation.total_quantity IS '总数量（服务端权威重算）';
COMMENT ON COLUMN lunaerp_business_quotation.total_amount IS '总金额（报价币种，服务端权威重算）';
COMMENT ON COLUMN lunaerp_business_quotation.total_cost_amount IS '总成本额（报价币种，脱敏字段）';
COMMENT ON COLUMN lunaerp_business_quotation.avg_margin_rate IS '整单加权毛利率（脱敏字段）';
COMMENT ON COLUMN lunaerp_business_quotation.status IS '状态（字典 lunaerp_quotation_status：DRAFT/CONFIRMED/SUPERSEDED/CANCELLED）';
COMMENT ON COLUMN lunaerp_business_quotation.remarks IS '备注';
COMMENT ON COLUMN lunaerp_business_quotation.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_quotation.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_quotation.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_quotation.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_quotation.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_quotation.tenant_id IS '租户编号';

-- =============================================================================
-- 6. 新表 6：业务域 - 报价明细子表 lunaerp_business_quotation_item
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_business_quotation_item (
    id                     int8          NOT NULL,
    quotation_id           int8          NOT NULL,
    line_no                int4          NOT NULL DEFAULT 1,
    product_id             int8          NULL,
    product_code           varchar(64)   NULL,
    product_name           varchar(128)  NOT NULL,
    customer_color         varchar(64)   NULL,
    composition            varchar(256)  NULL,
    width                  varchar(64)   NULL,
    weight                 varchar(64)   NULL,
    unit                   varchar(16)   NOT NULL DEFAULT 'KG',
    quantity               numeric(18,2) NOT NULL DEFAULT 0,
    cost_card_id           int8          NULL,
    cost_card_no           varchar(32)   NULL,
    cost_source            varchar(16)   NOT NULL DEFAULT 'CARD',
    cost_snapshot          text          NULL,
    cost_unit_price_rmb    numeric(18,4) NOT NULL DEFAULT 0,
    exchange_rate_snapshot numeric(18,6) NOT NULL DEFAULT 1,
    cost_unit_price        numeric(18,4) NOT NULL DEFAULT 0,
    target_margin_rate     numeric(9,6)  NULL,
    unit_price             numeric(18,4) NOT NULL DEFAULT 0,
    cost_amount            numeric(18,4) NOT NULL DEFAULT 0,
    amount                 numeric(18,4) NOT NULL DEFAULT 0,
    margin_rate            numeric(9,6)  NULL,
    low_margin_warning     boolean       NOT NULL DEFAULT false,
    remark                 varchar(256)  NULL,
    creator                varchar(64)   NULL DEFAULT '',
    create_time            timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater                varchar(64)   NULL DEFAULT '',
    update_time            timestamp     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted                int2          NOT NULL DEFAULT 0,
    tenant_id              int8          NOT NULL DEFAULT 0
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_business_quotation_item') THEN
        ALTER TABLE lunaerp_business_quotation_item ADD CONSTRAINT pk_lunaerp_business_quotation_item PRIMARY KEY (id);
    END IF;
END $$;

CREATE SEQUENCE IF NOT EXISTS lunaerp_business_quotation_item_seq;
CREATE INDEX IF NOT EXISTS idx_lbqi_quotation_id ON lunaerp_business_quotation_item (quotation_id);
CREATE INDEX IF NOT EXISTS idx_lbqi_cost_card_id ON lunaerp_business_quotation_item (cost_card_id);

COMMENT ON TABLE lunaerp_business_quotation_item IS '业务管理-报价单明细表';
COMMENT ON COLUMN lunaerp_business_quotation_item.id IS '报价明细ID';
COMMENT ON COLUMN lunaerp_business_quotation_item.quotation_id IS '所属报价单ID';
COMMENT ON COLUMN lunaerp_business_quotation_item.line_no IS '行号';
COMMENT ON COLUMN lunaerp_business_quotation_item.product_id IS '产品档案ID（手工录入时可空）';
COMMENT ON COLUMN lunaerp_business_quotation_item.product_code IS '产品编号快照';
COMMENT ON COLUMN lunaerp_business_quotation_item.product_name IS '品名快照';
COMMENT ON COLUMN lunaerp_business_quotation_item.customer_color IS '客户色号快照';
COMMENT ON COLUMN lunaerp_business_quotation_item.composition IS '成分快照';
COMMENT ON COLUMN lunaerp_business_quotation_item.width IS '门幅快照';
COMMENT ON COLUMN lunaerp_business_quotation_item.weight IS '克重快照';
COMMENT ON COLUMN lunaerp_business_quotation_item.unit IS '计价单位（KG/METER）';
COMMENT ON COLUMN lunaerp_business_quotation_item.quantity IS '数量';
COMMENT ON COLUMN lunaerp_business_quotation_item.cost_card_id IS '来源成本卡ID';
COMMENT ON COLUMN lunaerp_business_quotation_item.cost_card_no IS '来源成本卡号快照';
COMMENT ON COLUMN lunaerp_business_quotation_item.cost_source IS '成本来源（字典 lunaerp_quotation_cost_source：CARD 成本卡 / MANUAL 手工）';
COMMENT ON COLUMN lunaerp_business_quotation_item.cost_snapshot IS '成本卡摘要快照 JSON（脱敏时抹除）';
COMMENT ON COLUMN lunaerp_business_quotation_item.cost_unit_price_rmb IS '成本单价 RMB（脱敏字段）';
COMMENT ON COLUMN lunaerp_business_quotation_item.exchange_rate_snapshot IS '折币汇率快照';
COMMENT ON COLUMN lunaerp_business_quotation_item.cost_unit_price IS '折报价币成本单价（脱敏字段）';
COMMENT ON COLUMN lunaerp_business_quotation_item.target_margin_rate IS '目标毛利率（脱敏字段）';
COMMENT ON COLUMN lunaerp_business_quotation_item.unit_price IS '报价单价（报价币种）';
COMMENT ON COLUMN lunaerp_business_quotation_item.cost_amount IS '行成本额（服务端重算，脱敏字段）';
COMMENT ON COLUMN lunaerp_business_quotation_item.amount IS '行金额（服务端重算）';
COMMENT ON COLUMN lunaerp_business_quotation_item.margin_rate IS '实际毛利率（服务端重算，脱敏字段）';
COMMENT ON COLUMN lunaerp_business_quotation_item.low_margin_warning IS '是否低于最低毛利率预警阈值';
COMMENT ON COLUMN lunaerp_business_quotation_item.remark IS '备注';
COMMENT ON COLUMN lunaerp_business_quotation_item.creator IS '创建者';
COMMENT ON COLUMN lunaerp_business_quotation_item.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_business_quotation_item.updater IS '更新者';
COMMENT ON COLUMN lunaerp_business_quotation_item.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_business_quotation_item.deleted IS '是否删除（0未删 1已删）';
COMMENT ON COLUMN lunaerp_business_quotation_item.tenant_id IS '租户编号';

-- =============================================================================
-- 7. 菜单与按钮权限（system_menu，可重复执行）
--    10132 成本卡菜单挂 10100 销售目录（sort 0）+ 10133-10135 按钮
--    10136 报价单菜单挂 10100 销售目录（sort 0）+ 10137-10140 按钮
--    10245-10248 纱价按钮挂 10240 纱线档案菜单下（无独立一级菜单）
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (
    10132, 10133, 10134, 10135,
    10136, 10137, 10138, 10139, 10140,
    10245, 10246, 10247, 10248
);

-- 7.1 成本卡菜单与按钮
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10132, '成本卡', 'lunaerp:business:cost-card:query', 2, 0, 10100, 'cost-card', 'ep:money', 'lunaerp/business/cost-card/index', 'LunaerpBusinessCostCard', 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10133, '成本卡创建', 'lunaerp:business:cost-card:create', 3, 1, 10132, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10134, '成本卡修改', 'lunaerp:business:cost-card:update', 3, 2, 10132, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10135, '成本卡删除', 'lunaerp:business:cost-card:delete', 3, 3, 10132, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 7.2 报价单菜单与按钮
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10136, '报价单', 'lunaerp:business:quotation:query', 2, 0, 10100, 'quotation', 'ep:tickets', 'lunaerp/business/quotation/index', 'LunaerpBusinessQuotation', 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10137, '报价单创建', 'lunaerp:business:quotation:create', 3, 1, 10136, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10138, '报价单修改', 'lunaerp:business:quotation:update', 3, 2, 10136, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10139, '报价单删除', 'lunaerp:business:quotation:delete', 3, 3, 10136, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10140, '报价单打印', 'lunaerp:business:quotation:print', 3, 4, 10136, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 7.3 纱价按钮（挂 10240 纱线档案菜单下）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10245, '纱价查询', 'lunaerp:production:yarn-price:query', 3, 5, 10240, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10246, '纱价创建', 'lunaerp:production:yarn-price:create', 3, 6, 10240, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10247, '纱价修改', 'lunaerp:production:yarn-price:update', 3, 7, 10240, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10248, '纱价删除', 'lunaerp:production:yarn-price:delete', 3, 8, 10240, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 授权管理员与超管角色（PostgreSQL 无自增：id 显式取号，序列对齐现有最大 id 防主键冲突）
CREATE SEQUENCE IF NOT EXISTS system_role_menu_seq;
DO $$
BEGIN
    PERFORM setval('system_role_menu_seq', GREATEST((SELECT COALESCE(MAX(id), 0) FROM system_role_menu), 1));
END $$;

DELETE FROM system_role_menu WHERE menu_id IN (
    10132, 10133, 10134, 10135,
    10136, 10137, 10138, 10139, 10140,
    10245, 10246, 10247, 10248
);

INSERT INTO system_role_menu (id, role_id, menu_id, creator, create_time, updater, update_time, deleted, tenant_id)
SELECT nextval('system_role_menu_seq'), r.id, m.id, 'admin', CURRENT_TIMESTAMP, 'admin', CURRENT_TIMESTAMP, 0, 1
FROM system_role r
CROSS JOIN (
    SELECT 10132 AS id UNION ALL SELECT 10133 UNION ALL SELECT 10134 UNION ALL SELECT 10135 UNION ALL
    SELECT 10136 UNION ALL SELECT 10137 UNION ALL SELECT 10138 UNION ALL SELECT 10139 UNION ALL SELECT 10140 UNION ALL
    SELECT 10245 UNION ALL SELECT 10246 UNION ALL SELECT 10247 UNION ALL SELECT 10248
) m
WHERE r.code IN ('super_admin', 'admin') OR r.id IN (1, 2)
ON CONFLICT DO NOTHING;

COMMIT;
-- @formatter:on

-- =============================================================================
-- 8. 字典类型与字典数据（7 个字典：2262-2268，数据 3770-3791）
--    1) lunaerp_cost_card_status     成本卡状态（DRAFT/EFFECTIVE/EXPIRED）
--    2) lunaerp_cost_card_basis      成本卡基准口径（KG/METER）
--    3) lunaerp_cost_card_fee_type   成本卡费用类型（WEAVING/DYEING/SETTING/INSPECTION/PACKAGING/FREIGHT）
--    4) lunaerp_cost_fee_unit        成本费用计费口径（PER_KG/PER_METER）
--    5) lunaerp_yarn_price_type      纱价类型（PURCHASE/MARKET）
--    6) lunaerp_quotation_status     报价单状态（DRAFT/CONFIRMED/SUPERSEDED/CANCELLED/EXPIRED）
--    7) lunaerp_quotation_cost_source 报价成本来源（CARD/MANUAL）
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2262, 2263, 2264, 2265, 2266, 2267, 2268)
    OR type IN (
        'lunaerp_cost_card_status',
        'lunaerp_cost_card_basis',
        'lunaerp_cost_card_fee_type',
        'lunaerp_cost_fee_unit',
        'lunaerp_yarn_price_type',
        'lunaerp_quotation_status',
        'lunaerp_quotation_cost_source'
    );

DELETE FROM system_dict_data WHERE dict_type IN (
        'lunaerp_cost_card_status',
        'lunaerp_cost_card_basis',
        'lunaerp_cost_card_fee_type',
        'lunaerp_cost_fee_unit',
        'lunaerp_yarn_price_type',
        'lunaerp_quotation_status',
        'lunaerp_quotation_cost_source'
    )
    OR id BETWEEN 3770 AND 3791;

-- 8.1 成本卡状态
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2262, '成本卡状态', 'lunaerp_cost_card_status', 0, 'LunaERP 业务管理-成本卡状态', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3770, 1, '草稿', 'DRAFT', 'lunaerp_cost_card_status', 0, 'info', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3771, 2, '生效', 'EFFECTIVE', 'lunaerp_cost_card_status', 0, 'success', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3772, 3, '失效', 'EXPIRED', 'lunaerp_cost_card_status', 0, 'danger', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 8.2 成本卡基准口径
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2263, '成本卡基准口径', 'lunaerp_cost_card_basis', 0, 'LunaERP 业务管理-成本卡基准口径', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3773, 1, '按千克', 'KG', 'lunaerp_cost_card_basis', 0, 'primary', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3774, 2, '按米', 'METER', 'lunaerp_cost_card_basis', 0, 'success', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 8.3 成本卡费用类型
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2264, '成本卡费用类型', 'lunaerp_cost_card_fee_type', 0, 'LunaERP 业务管理-成本卡费用类型', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3775, 1, '织造', 'WEAVING', 'lunaerp_cost_card_fee_type', 0, 'primary', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3776, 2, '染色', 'DYEING', 'lunaerp_cost_card_fee_type', 0, 'success', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3777, 3, '定型', 'SETTING', 'lunaerp_cost_card_fee_type', 0, 'warning', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3778, 4, '检验', 'INSPECTION', 'lunaerp_cost_card_fee_type', 0, 'info', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3779, 5, '包装', 'PACKAGING', 'lunaerp_cost_card_fee_type', 0, 'default', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3780, 6, '运费', 'FREIGHT', 'lunaerp_cost_card_fee_type', 0, 'danger', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 8.4 成本费用计费口径
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2265, '成本费用计费单位', 'lunaerp_cost_fee_unit', 0, 'LunaERP 业务管理-成本费用计费单位', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3781, 1, '元/千克', 'PER_KG', 'lunaerp_cost_fee_unit', 0, 'primary', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3782, 2, '元/米', 'PER_METER', 'lunaerp_cost_fee_unit', 0, 'success', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 8.5 纱价类型
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2266, '纱价类型', 'lunaerp_yarn_price_type', 0, 'LunaERP 生产管理-纱价类型', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3783, 1, '采购参考价', 'PURCHASE', 'lunaerp_yarn_price_type', 0, 'primary', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3784, 2, '市场参考价', 'MARKET', 'lunaerp_yarn_price_type', 0, 'warning', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 8.6 报价单状态
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2267, '报价单状态', 'lunaerp_quotation_status', 0, 'LunaERP 业务管理-报价单状态', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3785, 1, '草稿', 'DRAFT', 'lunaerp_quotation_status', 0, 'info', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3786, 2, '已确认', 'CONFIRMED', 'lunaerp_quotation_status', 0, 'success', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3787, 3, '已修订', 'SUPERSEDED', 'lunaerp_quotation_status', 0, 'warning', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3788, 4, '已取消', 'CANCELLED', 'lunaerp_quotation_status', 0, 'danger', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3789, 5, '已失效', 'EXPIRED', 'lunaerp_quotation_status', 0, 'info', '', '仅展示派生态，禁止写入', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

-- 8.7 报价成本来源
INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2268, '报价成本来源', 'lunaerp_quotation_cost_source', 0, 'LunaERP 业务管理-报价成本来源', 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0', NULL);

INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3790, 1, '成本卡', 'CARD', 'lunaerp_quotation_cost_source', 0, 'primary', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3791, 2, '手工', 'MANUAL', 'lunaerp_quotation_cost_source', 0, 'warning', '', NULL, 'admin', '2026-10-10 00:00:00', '1', '2026-10-10 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 9. infra_config 参数配置种子：quotation.min-margin-rate
-- =============================================================================
DELETE FROM infra_config WHERE config_key = 'quotation.min-margin-rate';
INSERT INTO infra_config (
    id, category, type, name, config_key, value, visible, remark,
    creator, create_time, updater, update_time, deleted
) VALUES (
    102, 'biz', 2, '报价单最低毛利率预警阈值', 'quotation.min-margin-rate', '0', '1',
    '报价单行毛利率与整单毛利率低于该阈值时展示红字预警，缺省 0（0%）',
    'admin', CURRENT_TIMESTAMP, '1', CURRENT_TIMESTAMP, 0
);
