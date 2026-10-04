-- =============================================================================
-- 模块：LunaERP - 样品管理 - 样品分析（主表 + 字典 + 菜单权限，见 ADR-0036）
-- 数据库：PostgreSQL
-- 规范：芋道 ruoyi-vue-pro（id int8、creator/create_time/updater/update_time/
--       deleted/tenant_id 标准列，主键 ALTER TABLE ADD CONSTRAINT，列 COMMENT）
-- 可重复执行：CREATE TABLE IF NOT EXISTS + CREATE INDEX IF NOT EXISTS，
--             主键用 DO 块判存，菜单/字典先 DELETE 再 INSERT。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
-- =============================================================================

-- =============================================================================
-- 1. 样品分析主表 lunaerp_sample_analysis
--    包含来样属性、15 个机台工艺参数、成本报价及收费标量列、两 JSONB 列与登记人快照
-- =============================================================================
CREATE TABLE IF NOT EXISTS lunaerp_sample_analysis (
    id                       int8          NOT NULL,
    sample_analysis_no       varchar(32)   NOT NULL,
    customer_id              int8          NULL,
    client_name              varchar(255)  NULL,
    client_contact           varchar(128)  NULL,
    client_style_no          varchar(128)  NULL,
    product_name             varchar(255)  NOT NULL,
    composition              varchar(255)  NULL,
    width                    numeric(10,2) NULL,
    weight                   numeric(10,2) NULL,
    meter_length             numeric(10,2) NULL,
    process                  varchar(64)   NULL,
    dyeing_process           varchar(64)   NULL,
    machine_type             varchar(64)   NULL,
    output                   varchar(128)  NULL,
    flower_no                varchar(128)  NULL,
    white_width              varchar(128)  NULL,
    white_weight             varchar(128)  NULL,
    flower_width             varchar(128)  NULL,
    flower_height            varchar(128)  NULL,
    gear                     varchar(128)  NULL,
    cloth_rack               varchar(128)  NULL,
    start_speed              varchar(128)  NULL,
    feeder_disk              varchar(128)  NULL,
    theoretical_output       varchar(128)  NULL,
    stitch_length            varchar(128)  NULL,
    photos                   jsonb         NULL,
    stitch_arrange           jsonb         NULL,
    production_cost          numeric(18,2) NULL,
    dyeing_cost              numeric(18,2) NULL,
    other_cost               numeric(18,2) NULL,
    loss_rate                numeric(10,2) NULL,
    total_cost               numeric(18,2) NULL,
    quoted_price             numeric(18,2) NULL,
    quoted_remark            varchar(500)  NULL,
    is_paid                  bool          NOT NULL DEFAULT false,
    unit_price               numeric(18,2) NOT NULL DEFAULT 30,
    creator_id               int8          NULL,
    creator_name             varchar(64)   NULL,
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
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'pk_lunaerp_sample_analysis') THEN
        ALTER TABLE lunaerp_sample_analysis ADD CONSTRAINT pk_lunaerp_sample_analysis PRIMARY KEY (id);
    END IF;
END $$;

-- 样品编号租户内全量唯一（含已删除行，ADR-0036/ADR-0004）：普通唯一索引、不带 WHERE deleted=0
CREATE UNIQUE INDEX IF NOT EXISTS uk_lunaerp_sample_analysis_tenant_no
    ON lunaerp_sample_analysis (tenant_id, sample_analysis_no);
CREATE INDEX IF NOT EXISTS idx_lunaerp_sample_analysis_customer_id
    ON lunaerp_sample_analysis (tenant_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_lunaerp_sample_analysis_is_paid
    ON lunaerp_sample_analysis (tenant_id, is_paid);
CREATE INDEX IF NOT EXISTS idx_lunaerp_sample_analysis_create_time
    ON lunaerp_sample_analysis (tenant_id, create_time);

COMMENT ON TABLE lunaerp_sample_analysis IS '样品管理-样品分析主表';
COMMENT ON COLUMN lunaerp_sample_analysis.id IS '样品分析ID';
COMMENT ON COLUMN lunaerp_sample_analysis.sample_analysis_no IS '样品编号（SA+年份+4位顺序号，租户内全量唯一含已删除行）';
COMMENT ON COLUMN lunaerp_sample_analysis.customer_id IS '客户ID（lunaerp_baseinfo_customer.id，可空）';
COMMENT ON COLUMN lunaerp_sample_analysis.client_name IS '客户名称快照（手填或关联带出）';
COMMENT ON COLUMN lunaerp_sample_analysis.client_contact IS '客户联系人（自由文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.client_style_no IS '客户款号（自由文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.product_name IS '产品名称（唯一必填项，自由文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.composition IS '成分（自由文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.width IS '门幅（CM）';
COMMENT ON COLUMN lunaerp_sample_analysis.weight IS '克重（GSM）';
COMMENT ON COLUMN lunaerp_sample_analysis.meter_length IS '米长（每公斤米数，服务端计算，只读）';
COMMENT ON COLUMN lunaerp_sample_analysis.process IS '工序（字典 lunaerp_sample_process）';
COMMENT ON COLUMN lunaerp_sample_analysis.dyeing_process IS '双染工艺（字典 lunaerp_sample_dyeing_process）';
COMMENT ON COLUMN lunaerp_sample_analysis.machine_type IS '机型（字典 lunaerp_sample_machine_type）';
COMMENT ON COLUMN lunaerp_sample_analysis.output IS '实测产量（文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.flower_no IS '花号（文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.white_width IS '白胚门幅（文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.white_weight IS '白胚克重（文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.flower_width IS '花宽（文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.flower_height IS '花高（文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.gear IS '齿轮（文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.cloth_rack IS '布架（文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.start_speed IS '开机转数（文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.feeder_disk IS '送纱盘（文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.theoretical_output IS '理论产量（文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.stitch_length IS '线长（文本）';
COMMENT ON COLUMN lunaerp_sample_analysis.photos IS '照片 URL 列表（JSONB 数组，最多 9 张）';
COMMENT ON COLUMN lunaerp_sample_analysis.stitch_arrange IS '三角排列画板数据（JSONB，columns/rows/data）';
COMMENT ON COLUMN lunaerp_sample_analysis.production_cost IS '生产工费（元/KG）';
COMMENT ON COLUMN lunaerp_sample_analysis.dyeing_cost IS '染整费（元/KG）';
COMMENT ON COLUMN lunaerp_sample_analysis.other_cost IS '其他费用（元/KG）';
COMMENT ON COLUMN lunaerp_sample_analysis.loss_rate IS '损耗率（百分比数字，如 5 表示 5%）';
COMMENT ON COLUMN lunaerp_sample_analysis.total_cost IS '总成本（元/KG，服务端重算）';
COMMENT ON COLUMN lunaerp_sample_analysis.quoted_price IS '对外报价（元/米）';
COMMENT ON COLUMN lunaerp_sample_analysis.quoted_remark IS '报价备注';
COMMENT ON COLUMN lunaerp_sample_analysis.is_paid IS '月结付款状态（false 未付 / true 已付）';
COMMENT ON COLUMN lunaerp_sample_analysis.unit_price IS '月结单价（元/条，缺省 30）';
COMMENT ON COLUMN lunaerp_sample_analysis.creator_id IS '登记人用户ID（创建时写入快照）';
COMMENT ON COLUMN lunaerp_sample_analysis.creator_name IS '登记人姓名（创建时写入快照）';
COMMENT ON COLUMN lunaerp_sample_analysis.remark IS '备注';
COMMENT ON COLUMN lunaerp_sample_analysis.creator IS '创建者';
COMMENT ON COLUMN lunaerp_sample_analysis.create_time IS '创建时间';
COMMENT ON COLUMN lunaerp_sample_analysis.updater IS '更新者';
COMMENT ON COLUMN lunaerp_sample_analysis.update_time IS '更新时间';
COMMENT ON COLUMN lunaerp_sample_analysis.deleted IS '是否删除';
COMMENT ON COLUMN lunaerp_sample_analysis.tenant_id IS '租户编号';

-- =============================================================================
-- 2. 主键序列（MyBatis-Plus PostgreSQL INPUT 模式按 @KeySequence 取 nextval，可重复执行）
-- =============================================================================
CREATE SEQUENCE IF NOT EXISTS lunaerp_sample_analysis_seq;

-- =============================================================================
-- 3. 菜单与按钮（system_menu，可重复执行）
--    10600 样品管理顶级目录（sort 104）；10610 样品分析菜单承载 query 权限；
--    10611 创建、10612 修改、10613 删除、10614 收费四个按钮位。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_menu WHERE id IN (10600, 10610, 10611, 10612, 10613, 10614);

-- 顶级目录：样品管理（parent_id=0 顶级，sort=104）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10600, '样品管理', '', 1, 104, 0, '/sample', 'ep:collection', NULL, NULL, 0, '1', '1', '1', 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');

-- 菜单：样品分析（query 权限挂在菜单本身）
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10610, '样品分析', 'lunaerp:sample:sample-analysis:query', 2, 1, 10600, 'sample-analysis', 'ep:document', 'lunaerp-elmui/sample/sample-analysis/index', 'LunaerpElmuiSampleSampleAnalysis', 0, '1', '1', '1', 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');

-- 按钮：创建 / 修改 / 删除 / 收费
INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10611, '样品分析创建', 'lunaerp:sample:sample-analysis:create', 3, 1, 10610, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10612, '样品分析修改', 'lunaerp:sample:sample-analysis:update', 3, 2, 10610, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10613, '样品分析删除', 'lunaerp:sample:sample-analysis:delete', 3, 3, 10610, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');

INSERT INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, component_name, status, visible, keep_alive, always_show, creator, create_time, updater, update_time, deleted)
VALUES (10614, '样品分析收费', 'lunaerp:sample:sample-analysis:pay', 3, 4, 10610, '', '#', '', NULL, 0, '1', '1', '1', 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');

COMMIT;
-- @formatter:on

-- =============================================================================
-- 4. 字典类型与字典数据（system_dict_type / system_dict_data，可重复执行）
--    dict_type id 2251-2254，dict_data id 从 3709 起。
-- =============================================================================
-- @formatter:off
BEGIN;

DELETE FROM system_dict_type WHERE id IN (2251, 2252, 2253, 2254);
DELETE FROM system_dict_data WHERE dict_type IN (
    'lunaerp_sample_process',
    'lunaerp_sample_dyeing_process',
    'lunaerp_sample_machine_type',
    'lunaerp_sample_spin_method'
);

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2251, '样品分析工序', 'lunaerp_sample_process', 0, 'LunaERP 样品分析-工序', 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0', NULL);

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2252, '样品分析双染工艺', 'lunaerp_sample_dyeing_process', 0, 'LunaERP 样品分析-双染工艺', 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0', NULL);

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2253, '样品分析机型', 'lunaerp_sample_machine_type', 0, 'LunaERP 样品分析-机型', 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0', NULL);

INSERT INTO system_dict_type (id, name, type, status, remark, creator, create_time, updater, update_time, deleted, deleted_time)
VALUES (2254, '样品分析纺法', 'lunaerp_sample_spin_method', 0, 'LunaERP 样品分析-纺法（value存中文）', 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0', NULL);

-- 工序：染色 / 色织 / 印花 / 刺毛
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3709, 1, '染色', 'DYEING', 'lunaerp_sample_process', 0, 'primary', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3710, 2, '色织', 'YARN_DYED', 'lunaerp_sample_process', 0, 'success', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3711, 3, '印花', 'PRINTING', 'lunaerp_sample_process', 0, 'warning', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3712, 4, '刺毛', 'NAPPING', 'lunaerp_sample_process', 0, 'info', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');

-- 双染工艺：13 项
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3713, 1, '单染 T', 'SINGLE_T', 'lunaerp_sample_dyeing_process', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3714, 2, '单染 C', 'SINGLE_C', 'lunaerp_sample_dyeing_process', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3715, 3, '单染 R', 'SINGLE_R', 'lunaerp_sample_dyeing_process', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3716, 4, '单染 N', 'SINGLE_N', 'lunaerp_sample_dyeing_process', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3717, 5, '单染 M', 'SINGLE_M', 'lunaerp_sample_dyeing_process', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3718, 6, '单染天丝', 'SINGLE_TENCEL', 'lunaerp_sample_dyeing_process', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3719, 7, '双染 TR', 'DOUBLE_TR', 'lunaerp_sample_dyeing_process', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3720, 8, '双染 CVC', 'DOUBLE_CVC', 'lunaerp_sample_dyeing_process', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3721, 9, '双染 TC', 'DOUBLE_TC', 'lunaerp_sample_dyeing_process', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3722, 10, '双染 NR', 'DOUBLE_NR', 'lunaerp_sample_dyeing_process', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3723, 11, '双染 TN', 'DOUBLE_TN', 'lunaerp_sample_dyeing_process', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3724, 12, '双染 TM', 'DOUBLE_TM', 'lunaerp_sample_dyeing_process', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3725, 13, '三染 NTR', 'TRIPLE_NTR', 'lunaerp_sample_dyeing_process', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');

-- 机型：4 项
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3726, 1, '喷水织机', 'WATER_JET', 'lunaerp_sample_machine_type', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3727, 2, '大圆机', 'CIRCULAR_KNIT', 'lunaerp_sample_machine_type', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3728, 3, '经编机', 'WARP_KNIT', 'lunaerp_sample_machine_type', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3729, 4, '剑杆织机', 'RAPIER', 'lunaerp_sample_machine_type', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');

-- 纺法：6 项（value 与 label 同为中文）
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3730, 1, '环锭纺', '环锭纺', 'lunaerp_sample_spin_method', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3731, 2, '赛络纺', '赛络纺', 'lunaerp_sample_spin_method', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3732, 3, '紧密纺', '紧密纺', 'lunaerp_sample_spin_method', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3733, 4, '紧密赛络纺', '紧密赛络纺', 'lunaerp_sample_spin_method', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3734, 5, '气流纺', '气流纺', 'lunaerp_sample_spin_method', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');
INSERT INTO system_dict_data (id, sort, label, value, dict_type, status, color_type, css_class, remark, creator, create_time, updater, update_time, deleted)
VALUES (3735, 6, '涡流纺', '涡流纺', 'lunaerp_sample_spin_method', 0, '', '', NULL, 'admin', '2026-10-04 00:00:00', '1', '2026-10-04 00:00:00', '0');

COMMIT;
-- @formatter:on
