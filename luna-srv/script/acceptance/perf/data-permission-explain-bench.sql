-- =============================================================================
-- 模块：LunaERP - 商业化验收 - 销售归属链 4 档数据范围 EXPLAIN 分析与基准实证 (Issue 08)
-- 数据库：PostgreSQL 15+
-- 规范：
--   1. ALL 档与 3 个受限档（DEPT_AND_CHILD / DEPT_ONLY / SELF）list 与 detail 查询
--   2. EXPLAIN 见 index scan / bitmap index scan；受限档 BitmapOr 合并两索引；seq scan 即失败
--   3. 联系人表通过 EXISTS 子查询继承客户归属，子查询走客户主键与 dp 部分索引
--   4. 调岗切面批量更新走 (tenant_id, salesperson_user_id) 索引，万级数据量毫秒级执行
--   5. 每档连续 100 次 list/detail 查询去预热取 P95 < 1000ms
-- =============================================================================

-- =============================================================================
-- 1. ALL 档（全部档，如 boss / finance / planner，data_scope = 1）
--    不追加 dept_id / salesperson_user_id 谓词，原有查询计划与执行效率完全不受影响
-- =============================================================================

-- 1.1 客户列表（ALL 档）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_baseinfo_customer
WHERE tenant_id = 1 AND deleted = 0
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- 1.2 客户详情（ALL 档直连 id）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_baseinfo_customer
WHERE id = 2000000001 AND tenant_id = 1 AND deleted = 0;

-- 1.3 销售合同列表（ALL 档）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_business_sales_contract
WHERE tenant_id = 1 AND deleted = 0
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- 1.4 销售合同详情（ALL 档直连 id）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_business_sales_contract
WHERE id = 2000000001 AND tenant_id = 1 AND deleted = 0;

-- 1.5 发货单列表（ALL 档）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_business_shipment
WHERE tenant_id = 1 AND deleted = 0
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- 1.6 发货单详情（ALL 档直连 id）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_business_shipment
WHERE id = 2000000001 AND tenant_id = 1 AND deleted = 0;

-- 1.7 客户联系人列表（ALL 档）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_baseinfo_customer_contact
WHERE tenant_id = 1 AND deleted = 0
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- =============================================================================
-- 2. DEPT_AND_CHILD 档（本部门及以下，如 sales_manager m，部门 101 及子部门 102，用户 1001）
--    谓词：(dept_id IN (101, 102) OR salesperson_user_id = 1001)
--    断言：走 BitmapOr，分别合并 idx_lb_xxx_dp_dept 与 idx_lb_xxx_dp_user 两个部分索引，绝不走 seq scan
-- =============================================================================

-- 2.1 客户列表（DEPT_AND_CHILD 档）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_baseinfo_customer
WHERE tenant_id = 1 AND deleted = 0
  AND (dept_id IN (101, 102) OR salesperson_user_id = 1001)
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- 2.2 客户详情（DEPT_AND_CHILD 档直连 id 可见性验证）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_baseinfo_customer
WHERE id = 2000000001 AND tenant_id = 1 AND deleted = 0
  AND (dept_id IN (101, 102) OR salesperson_user_id = 1001);

-- 2.3 销售合同列表（DEPT_AND_CHILD 档）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_business_sales_contract
WHERE tenant_id = 1 AND deleted = 0
  AND (dept_id IN (101, 102) OR salesperson_user_id = 1001)
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- 2.4 销售合同详情（DEPT_AND_CHILD 档直连 id）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_business_sales_contract
WHERE id = 2000000001 AND tenant_id = 1 AND deleted = 0
  AND (dept_id IN (101, 102) OR salesperson_user_id = 1001);

-- 2.5 发货单列表（DEPT_AND_CHILD 档）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_business_shipment
WHERE tenant_id = 1 AND deleted = 0
  AND (dept_id IN (101, 102) OR salesperson_user_id = 1001)
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- 2.6 发货单详情（DEPT_AND_CHILD 档直连 id）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_business_shipment
WHERE id = 2000000001 AND tenant_id = 1 AND deleted = 0
  AND (dept_id IN (101, 102) OR salesperson_user_id = 1001);

-- 2.7 客户联系人列表（DEPT_AND_CHILD 档，EXISTS 继承客户归属）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_baseinfo_customer_contact
WHERE tenant_id = 1 AND deleted = 0
  AND EXISTS (
      SELECT 1 FROM lunaerp_baseinfo_customer c
      WHERE c.id = lunaerp_baseinfo_customer_contact.customer_id
        AND c.tenant_id = 1 AND c.deleted = 0
        AND (c.dept_id IN (101, 102) OR c.salesperson_user_id = 1001)
  )
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- =============================================================================
-- 3. DEPT_ONLY 档（本部门，如 dept_only_user，部门 101，用户 1005）
--    谓词：(dept_id IN (101) OR salesperson_user_id = 1005)
--    断言：走 BitmapOr，分别合并 idx_lb_xxx_dp_dept 与 idx_lb_xxx_dp_user，绝不走 seq scan
-- =============================================================================

-- 3.1 客户列表（DEPT_ONLY 档）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_baseinfo_customer
WHERE tenant_id = 1 AND deleted = 0
  AND (dept_id IN (101) OR salesperson_user_id = 1005)
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- 3.2 销售合同列表（DEPT_ONLY 档）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_business_sales_contract
WHERE tenant_id = 1 AND deleted = 0
  AND (dept_id IN (101) OR salesperson_user_id = 1005)
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- 3.3 发货单列表（DEPT_ONLY 档）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_business_shipment
WHERE tenant_id = 1 AND deleted = 0
  AND (dept_id IN (101) OR salesperson_user_id = 1005)
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- 3.4 客户联系人列表（DEPT_ONLY 档，EXISTS 继承）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_baseinfo_customer_contact
WHERE tenant_id = 1 AND deleted = 0
  AND EXISTS (
      SELECT 1 FROM lunaerp_baseinfo_customer c
      WHERE c.id = lunaerp_baseinfo_customer_contact.customer_id
        AND c.tenant_id = 1 AND c.deleted = 0
        AND (c.dept_id IN (101) OR c.salesperson_user_id = 1005)
  )
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- =============================================================================
-- 4. SELF 档（本人，如 salesperson a，用户 1002）
--    谓词：salesperson_user_id = 1002
--    断言：走 idx_lb_xxx_dp_user 部分索引，单索引直接 Index Scan / Bitmap Index Scan
-- =============================================================================

-- 4.1 客户列表（SELF 档）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_baseinfo_customer
WHERE tenant_id = 1 AND deleted = 0
  AND salesperson_user_id = 1002
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- 4.2 销售合同列表（SELF 档）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_business_sales_contract
WHERE tenant_id = 1 AND deleted = 0
  AND salesperson_user_id = 1002
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- 4.3 发货单列表（SELF 档）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_business_shipment
WHERE tenant_id = 1 AND deleted = 0
  AND salesperson_user_id = 1002
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- 4.4 客户联系人列表（SELF 档，EXISTS 继承）
EXPLAIN (ANALYZE, BUFFERS, COSTS)
SELECT * FROM lunaerp_baseinfo_customer_contact
WHERE tenant_id = 1 AND deleted = 0
  AND EXISTS (
      SELECT 1 FROM lunaerp_baseinfo_customer c
      WHERE c.id = lunaerp_baseinfo_customer_contact.customer_id
        AND c.tenant_id = 1 AND c.deleted = 0
        AND c.salesperson_user_id = 1002
  )
ORDER BY id DESC
LIMIT 20 OFFSET 0;

-- =============================================================================
-- 5. 调岗切面批量更新实证（与 04 票联动验证）
--    当业务员 a (1002) 从部门 101 调整到 103 时，同事务批量更新 4 表 dept_id
--    断言：走 (tenant_id, salesperson_user_id) 索引，万级数据量毫秒级执行，无 seq scan
-- =============================================================================

-- 5.1 客户表调岗批量更新
EXPLAIN (ANALYZE, BUFFERS, COSTS)
UPDATE lunaerp_baseinfo_customer
SET dept_id = 103
WHERE tenant_id = 1 AND salesperson_user_id = 1002 AND deleted = 0;

-- 5.2 合同表调岗批量更新
EXPLAIN (ANALYZE, BUFFERS, COSTS)
UPDATE lunaerp_business_sales_contract
SET dept_id = 103
WHERE tenant_id = 1 AND salesperson_user_id = 1002 AND deleted = 0;

-- 5.3 发货单表调岗批量更新
EXPLAIN (ANALYZE, BUFFERS, COSTS)
UPDATE lunaerp_business_shipment
SET dept_id = 103
WHERE tenant_id = 1 AND salesperson_user_id = 1002 AND deleted = 0;

-- 5.4 报价单表调岗批量更新（存在性守卫）
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = current_schema() AND table_name = 'lunaerp_business_quotation'
    ) THEN
        EXECUTE 'EXPLAIN (ANALYZE, BUFFERS, COSTS) UPDATE lunaerp_business_quotation SET dept_id = 103 WHERE tenant_id = 1 AND salesperson_user_id = 1002 AND deleted = 0;';
    END IF;
END $$;
