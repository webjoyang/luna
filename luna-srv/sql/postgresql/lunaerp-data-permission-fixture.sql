-- =============================================================================
-- 模块：LunaERP - 商业化 - §9.1 组织造数 fixture 种子数据（Issue 07）
-- 数据库：PostgreSQL 15
-- 内容：
--   1) 部门：D1（子部门 D1.1）、D2 平级
--   2) 用户与角色：
--      - boss (1000): ALL 档 (data_scope=1)
--      - m (1001): D1 主管 sales_manager, DEPT_AND_CHILD 档 (data_scope=4)
--      - a (1002): D1 业务员 salesperson, SELF 档 (data_scope=5)
--      - c (1003): D1.1 业务员 salesperson, SELF 档 (data_scope=5)
--      - d (1004): D2 业务员 salesperson, SELF 档 (data_scope=5)
--      - dept_only_user (1005): D1 DEPT_ONLY 档 (data_scope=3)
--      - deact_user (1006): D1 停用业务员 (status=1)
--   3) 4 张已存业务表（客户、合同、发货、联系人）造归属 a/c/d 的行各 ≥2（含不同状态），空归属行 ≥1
--   4) 报价单表（QuotationDO）用例文本与数据结构冻结，随 05 票施工同批补跑
-- 幂等性：按固定测试 ID 插入，带 ON CONFLICT (id) DO UPDATE / NOTHING，可反复重跑。
-- =============================================================================

-- =============================================================================
-- 1. 部门造数：D1 (101), D1.1 (102, 父部门 101), D2 (103, 平级父部门 0)
-- =============================================================================
INSERT INTO system_dept (id, name, parent_id, sort, leader_user_id, phone, email, status, tenant_id, creator, updater, deleted)
VALUES
    (101, '销售一部(D1)', 0, 1, 1001, '13800000101', 'd1@luna.test', 0, 1, '1', '1', 0),
    (102, '销售一部一科(D1.1)', 101, 1, NULL, '13800000102', 'd1_1@luna.test', 0, 1, '1', '1', 0),
    (103, '销售二部(D2)', 0, 2, 1004, '13800000103', 'd2@luna.test', 0, 1, '1', '1', 0)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    parent_id = EXCLUDED.parent_id,
    leader_user_id = EXCLUDED.leader_user_id,
    deleted = 0;

-- =============================================================================
-- 2. 角色与数据权限档位对齐
-- =============================================================================
INSERT INTO system_role (id, name, code, sort, data_scope, data_scope_dept_ids, status, type, remark, tenant_id, creator, updater, deleted)
VALUES
    (1001, '销售主管(D1)', 'sales_manager', 1, 4, '', 0, 2, '§9.1 测试角色: 本部门及以下', 1, '1', '1', 0),
    (1002, '业务员(通用)', 'salesperson', 2, 5, '', 0, 2, '§9.1 测试角色: 仅本人', 1, '1', '1', 0),
    (1003, '本部门角色', 'dept_only_role', 3, 3, '', 0, 2, '§9.1 测试角色: 本部门', 1, '1', '1', 0),
    (1000, '老板/财务(全部)', 'boss', 0, 1, '', 0, 2, '§9.1 测试角色: 全部', 1, '1', '1', 0)
ON CONFLICT (id) DO UPDATE SET
    data_scope = EXCLUDED.data_scope,
    data_scope_dept_ids = '',
    deleted = 0;

-- =============================================================================
-- 3. 用户造数：boss(1000), m(1001), a(1002), c(1003), d(1004), dept_only_user(1005), deact_user(1006)
-- =============================================================================
INSERT INTO system_users (id, username, password, nickname, remark, dept_id, post_ids, email, mobile, sex, avatar, status, login_ip, login_date, tenant_id, creator, updater, deleted)
VALUES
    (1000, 'boss', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.B.V3.c8E9C0qY8R7Q90aGzW', '老板/总经理', 'ALL档测试账号', 101, '', 'boss@luna.test', '13900001000', 1, '', 0, '', NULL, 1, '1', '1', 0),
    (1001, 'sales_manager_m', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.B.V3.c8E9C0qY8R7Q90aGzW', '销售主管m', 'D1主管:DEPT_AND_CHILD', 101, '', 'm@luna.test', '13900001001', 1, '', 0, '', NULL, 1, '1', '1', 0),
    (1002, 'salesperson_a', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.B.V3.c8E9C0qY8R7Q90aGzW', '业务员a', 'D1业务员:SELF', 101, '', 'a@luna.test', '13900001002', 1, '', 0, '', NULL, 1, '1', '1', 0),
    (1003, 'salesperson_c', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.B.V3.c8E9C0qY8R7Q90aGzW', '业务员c', 'D1.1业务员:SELF', 102, '', 'c@luna.test', '13900001003', 1, '', 0, '', NULL, 1, '1', '1', 0),
    (1004, 'salesperson_d', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.B.V3.c8E9C0qY8R7Q90aGzW', '业务员d', 'D2业务员:SELF', 103, '', 'd@luna.test', '13900001004', 1, '', 0, '', NULL, 1, '1', '1', 0),
    (1005, 'dept_only_user', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.B.V3.c8E9C0qY8R7Q90aGzW', '本部门用户', 'D1本部门:DEPT_ONLY', 101, '', 'dept_only@luna.test', '13900001005', 1, '', 0, '', NULL, 1, '1', '1', 0),
    (1006, 'salesperson_deact', '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.B.V3.c8E9C0qY8R7Q90aGzW', '已停用业务员', 'D1停用业务员(status=1)', 101, '', 'deact@luna.test', '13900001006', 1, '', 1, '', NULL, 1, '1', '1', 0)
ON CONFLICT (id) DO UPDATE SET
    dept_id = EXCLUDED.dept_id,
    status = EXCLUDED.status,
    deleted = 0;

-- 绑定用户角色
INSERT INTO system_user_role (id, user_id, role_id, tenant_id, creator, updater, deleted)
VALUES
    (10001, 1000, 1000, 1, '1', '1', 0),
    (10002, 1001, 1001, 1, '1', '1', 0),
    (10003, 1002, 1002, 1, '1', '1', 0),
    (10004, 1003, 1002, 1, '1', '1', 0),
    (10005, 1004, 1002, 1, '1', '1', 0),
    (10006, 1005, 1003, 1, '1', '1', 0),
    (10007, 1006, 1002, 1, '1', '1', 0)
ON CONFLICT (id) DO UPDATE SET
    user_id = EXCLUDED.user_id,
    role_id = EXCLUDED.role_id,
    deleted = 0;

-- =============================================================================
-- 4. 业务表 1：客户表 (lunaerp_baseinfo_customer)
--    a(1002, dept 101) 2 条 (ACTIVE, INACTIVE)
--    c(1003, dept 102) 2 条 (ACTIVE, INACTIVE)
--    d(1004, dept 103) 2 条 (ACTIVE, INACTIVE)
--    空归属 1 条 (salesperson_user_id NULL, dept_id NULL)
--    停用人员 1 条 (salesperson_user_id 1006, dept_id 101)
-- =============================================================================
INSERT INTO lunaerp_baseinfo_customer (id, company_name, short_name, status, salesperson_user_id, dept_id, tenant_id, creator, updater, deleted)
VALUES
    (10001, '织造集团A-1', '客户A1', 'ACTIVE', 1002, 101, 1, '1002', '1002', 0),
    (10002, '织造集团A-2', '客户A2', 'INACTIVE', 1002, 101, 1, '1002', '1002', 0),
    (10003, '纺织公司C-1', '客户C1', 'ACTIVE', 1003, 102, 1, '1003', '1003', 0),
    (10004, '纺织公司C-2', '客户C2', 'INACTIVE', 1003, 102, 1, '1003', '1003', 0),
    (10005, '印染企业D-1', '客户D1', 'ACTIVE', 1004, 103, 1, '1004', '1004', 0),
    (10006, '印染企业D-2', '客户D2', 'INACTIVE', 1004, 103, 1, '1004', '1004', 0),
    (10007, '历史公海客户-空归属', '客户NULL', 'ACTIVE', NULL, NULL, 1, '1', '1', 0),
    (10008, '停用业务员历史客户', '客户DEACT', 'ACTIVE', 1006, 101, 1, '1006', '1006', 0)
ON CONFLICT (id) DO UPDATE SET
    company_name = EXCLUDED.company_name,
    salesperson_user_id = EXCLUDED.salesperson_user_id,
    dept_id = EXCLUDED.dept_id,
    status = EXCLUDED.status,
    deleted = 0;

-- =============================================================================
-- 5. 业务表 2：销售合同表 (lunaerp_business_sales_contract)
--    a(1002, dept 101) 2 条 (ACTIVE, COMPLETED)
--    c(1003, dept 102) 2 条 (ACTIVE, COMPLETED)
--    d(1004, dept 103) 2 条 (ACTIVE, COMPLETED)
--    空归属 1 条 (salesperson_user_id NULL, dept_id NULL)
--    停用人员 1 条 (salesperson_user_id 1006, dept_id 101)
-- =============================================================================
INSERT INTO lunaerp_business_sales_contract (id, contract_no, customer_id, client_name, salesperson_user_id, dept_id, status, sign_date, tenant_id, creator, updater, deleted)
VALUES
    (20001, 'CH2026-A001', 10001, '织造集团A-1', 1002, 101, 'ACTIVE', CURRENT_DATE, 1, '1002', '1002', 0),
    (20002, 'CH2026-A002', 10002, '织造集团A-2', 1002, 101, 'COMPLETED', CURRENT_DATE, 1, '1002', '1002', 0),
    (20003, 'CH2026-C001', 10003, '纺织公司C-1', 1003, 102, 'ACTIVE', CURRENT_DATE, 1, '1003', '1003', 0),
    (20004, 'CH2026-C002', 10004, '纺织公司C-2', 1003, 102, 'COMPLETED', CURRENT_DATE, 1, '1003', '1003', 0),
    (20005, 'CH2026-D001', 10005, '印染企业D-1', 1004, 103, 'ACTIVE', CURRENT_DATE, 1, '1004', '1004', 0),
    (20006, 'CH2026-D002', 10006, '印染企业D-2', 1004, 103, 'COMPLETED', CURRENT_DATE, 1, '1004', '1004', 0),
    (20007, 'CH2026-NULL01', 10007, '历史公海客户-空归属', NULL, NULL, 'ACTIVE', CURRENT_DATE, 1, '1', '1', 0),
    (20008, 'CH2026-DEACT01', 10008, '停用业务员历史客户', 1006, 101, 'ACTIVE', CURRENT_DATE, 1, '1006', '1006', 0)
ON CONFLICT (id) DO UPDATE SET
    contract_no = EXCLUDED.contract_no,
    customer_id = EXCLUDED.customer_id,
    salesperson_user_id = EXCLUDED.salesperson_user_id,
    dept_id = EXCLUDED.dept_id,
    status = EXCLUDED.status,
    deleted = 0;

-- =============================================================================
-- 6. 业务表 3：发货单表 (lunaerp_business_shipment)
--    a(1002, dept 101) 2 条 (DRAFT, COMPLETED)
--    c(1003, dept 102) 2 条 (DRAFT, COMPLETED)
--    d(1004, dept 103) 2 条 (DRAFT, COMPLETED)
--    空归属 1 条 (salesperson_user_id NULL, dept_id NULL)
--    停用人员 1 条 (salesperson_user_id 1006, dept_id 101)
-- =============================================================================
INSERT INTO lunaerp_business_shipment (id, shipment_no, customer_id, customer_name, sales_contract_id, contract_no, warehouse_id, warehouse_name, salesperson_user_id, dept_id, status, tenant_id, creator, updater, deleted)
VALUES
    (30001, 'SH-20261010-A01', 10001, '织造集团A-1', 20001, 'CH2026-A001', 1, '1号成品仓', 1002, 101, 'DRAFT', 1, '1002', '1002', 0),
    (30002, 'SH-20261010-A02', 10002, '织造集团A-2', 20002, 'CH2026-A002', 1, '1号成品仓', 1002, 101, 'COMPLETED', 1, '1002', '1002', 0),
    (30003, 'SH-20261010-C01', 10003, '纺织公司C-1', 20003, 'CH2026-C001', 1, '1号成品仓', 1003, 102, 'DRAFT', 1, '1003', '1003', 0),
    (30004, 'SH-20261010-C02', 10004, '纺织公司C-2', 20004, 'CH2026-C002', 1, '1号成品仓', 1003, 102, 'COMPLETED', 1, '1003', '1003', 0),
    (30005, 'SH-20261010-D01', 10005, '印染企业D-1', 20005, 'CH2026-D001', 1, '1号成品仓', 1004, 103, 'DRAFT', 1, '1004', '1004', 0),
    (30006, 'SH-20261010-D02', 10006, '印染企业D-2', 20006, 'CH2026-D002', 1, '1号成品仓', 1004, 103, 'COMPLETED', 1, '1004', '1004', 0),
    (30007, 'SH-20261010-NULL1', 10007, '历史公海客户-空归属', 20007, 'CH2026-NULL01', 1, '1号成品仓', NULL, NULL, 'DRAFT', 1, '1', '1', 0),
    (30008, 'SH-20261010-DEACT1', 10008, '停用业务员历史客户', 20008, 'CH2026-DEACT01', 1, '1号成品仓', 1006, 101, 'DRAFT', 1, '1006', '1006', 0)
ON CONFLICT (id) DO UPDATE SET
    shipment_no = EXCLUDED.shipment_no,
    customer_id = EXCLUDED.customer_id,
    sales_contract_id = EXCLUDED.sales_contract_id,
    salesperson_user_id = EXCLUDED.salesperson_user_id,
    dept_id = EXCLUDED.dept_id,
    status = EXCLUDED.status,
    deleted = 0;

-- =============================================================================
-- 7. 业务表 4：客户联系人表 (lunaerp_baseinfo_customer_contact)
--    依附于各客户（通过 EXISTS (SELECT 1 FROM lunaerp_baseinfo_customer c WHERE c.id = customer_id) 继承可见性）
--    a 客户联系人: 40001 (cust 10001), 40002 (cust 10002)
--    c 客户联系人: 40003 (cust 10003), 40004 (cust 10004)
--    d 客户联系人: 40005 (cust 10005), 40006 (cust 10006)
--    空归属客户联系人: 40007 (cust 10007)
--    停用客户联系人: 40008 (cust 10008)
-- =============================================================================
INSERT INTO lunaerp_baseinfo_customer_contact (id, customer_id, name, position, phone, email, is_primary, tenant_id, creator, updater, deleted)
VALUES
    (40001, 10001, '张总(A1联系人)', '总经理', '13700000001', 'zhang_a1@luna.test', 1, 1, '1002', '1002', 0),
    (40002, 10002, '李主管(A2联系人)', '采购主管', '13700000002', 'li_a2@luna.test', 0, 1, '1002', '1002', 0),
    (40003, 10003, '王经理(C1联系人)', '销售经理', '13700000003', 'wang_c1@luna.test', 1, 1, '1003', '1003', 0),
    (40004, 10004, '赵专员(C2联系人)', '跟单专员', '13700000004', 'zhao_c2@luna.test', 0, 1, '1003', '1003', 0),
    (40005, 10005, '孙厂长(D1联系人)', '厂长', '13700000005', 'sun_d1@luna.test', 1, 1, '1004', '1004', 0),
    (40006, 10006, '周调度(D2联系人)', '生产调度', '13700000006', 'zhou_d2@luna.test', 0, 1, '1004', '1004', 0),
    (40007, 10007, '吴公海(NULL联系人)', '联络人', '13700000007', 'wu_null@luna.test', 1, 1, '1', '1', 0),
    (40008, 10008, '郑历史(DEACT联系人)', '联络人', '13700000008', 'zheng_deact@luna.test', 1, 1, '1006', '1006', 0)
ON CONFLICT (id) DO UPDATE SET
    customer_id = EXCLUDED.customer_id,
    name = EXCLUDED.name,
    phone = EXCLUDED.phone,
    deleted = 0;

-- =============================================================================
-- 8. 报价单表（lunaerp_business_quotation，QuotationDO）说明：
--    随 05 票 cost-card-quotation 施工补跑，用例文本在 spec §9.2 与 §11 冻结：
--    QuotationDO 增加 dept_id 与 2 个 dp 索引 (idx_lb_quotation_dp_dept, idx_lb_quotation_dp_user)，
--    并入 DeptDataPermissionRule 4 档过滤。
-- =============================================================================
