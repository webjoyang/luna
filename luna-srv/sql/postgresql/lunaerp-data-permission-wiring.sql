-- =============================================================================
-- 模块：LunaERP - 商业化 - 销售归属链 4 档行级数据范围接线（ADR-0046，21 号票）
-- 数据库：PostgreSQL
-- 内容：1) 客户表加归属人/归属部门 2 列，销售合同/发货单各加归属部门 1 列
--       2) 3 表 × 2 个数据权限部分索引（dept / user，受限档 BitmapOr）
--       3) 存量行归属回填（只填空、不覆盖、按租户、可重跑）
--       4) 存量角色自定义档迁移 + 11 个标准业务角色 data_scope 出厂基线对齐
-- 可重复执行：ADD COLUMN IF NOT EXISTS + CREATE INDEX IF NOT EXISTS + 回填 UPDATE 全部 IS NULL 守卫，
--             角色对齐按 code 幂等 UPDATE；空库（10 段先建表）与存量库重跑均安全。
-- 注意：本脚本仅做语法自查，禁止在共享远程库直接执行。
--       报价单表（lunaerp_business_quotation）由 05 号成本卡报价脚本建表时直接带列与索引。
-- =============================================================================

-- =============================================================================
-- 1. 归属列（owner 列统一名 salesperson_user_id；归属部门统一名 dept_id）
-- =============================================================================
ALTER TABLE lunaerp_baseinfo_customer
    ADD COLUMN IF NOT EXISTS salesperson_user_id int8 NULL;
ALTER TABLE lunaerp_baseinfo_customer
    ADD COLUMN IF NOT EXISTS dept_id int8 NULL;

ALTER TABLE lunaerp_business_sales_contract
    ADD COLUMN IF NOT EXISTS dept_id int8 NULL;

ALTER TABLE lunaerp_business_shipment
    ADD COLUMN IF NOT EXISTS dept_id int8 NULL;

COMMENT ON COLUMN lunaerp_baseinfo_customer.salesperson_user_id IS '归属业务员（数据范围 user 列）；空=未归属，仅全部档可见';
COMMENT ON COLUMN lunaerp_baseinfo_customer.dept_id IS '归属部门（数据范围 dept 列）；服务端按归属人当前部门推导';
COMMENT ON COLUMN lunaerp_business_sales_contract.dept_id IS '归属部门（数据范围 dept 列）；服务端按归属业务员当前部门推导';
COMMENT ON COLUMN lunaerp_business_shipment.dept_id IS '归属部门（数据范围 dept 列）；服务端按归属业务员当前部门推导';

-- =============================================================================
-- 2. 数据权限部分索引：受限档谓词 (dept_id IN (...) OR salesperson_user_id=?) 走 BitmapOr
-- =============================================================================
CREATE INDEX IF NOT EXISTS idx_lb_customer_dp_dept
    ON lunaerp_baseinfo_customer (tenant_id, dept_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lb_customer_dp_user
    ON lunaerp_baseinfo_customer (tenant_id, salesperson_user_id) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_lb_sales_contract_dp_dept
    ON lunaerp_business_sales_contract (tenant_id, dept_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lb_sales_contract_dp_user
    ON lunaerp_business_sales_contract (tenant_id, salesperson_user_id) WHERE deleted = 0;

CREATE INDEX IF NOT EXISTS idx_lb_shipment_dp_dept
    ON lunaerp_business_shipment (tenant_id, dept_id) WHERE deleted = 0;
CREATE INDEX IF NOT EXISTS idx_lb_shipment_dp_user
    ON lunaerp_business_shipment (tenant_id, salesperson_user_id) WHERE deleted = 0;

-- =============================================================================
-- 3. 存量回填（只填空列、不覆盖非空；按 tenant 维度定位兜底，不写死用户/部门 id）
--    兜底用户：该租户启用用户中优先 tenant_admin，否则最小 id 用户
--    兜底部门：该租户根部门（parent_id=0），否则兜底用户当前部门
--    creator 为 varchar 用户 id 串：数字串方可关联 system_users
-- =============================================================================

-- 3.1 客户归属人：creator 用户 -> 租户兜底用户
WITH fallback_owner AS (
    SELECT DISTINCT ON (u.tenant_id) u.tenant_id, u.id AS user_id
    FROM system_users u
    LEFT JOIN system_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
    LEFT JOIN system_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.code = 'tenant_admin'
    WHERE u.deleted = 0
    ORDER BY u.tenant_id, (r.code = 'tenant_admin') DESC NULLS LAST, u.id
)
UPDATE lunaerp_baseinfo_customer c
SET salesperson_user_id = x.owner_id
FROM (
    SELECT c2.id,
           COALESCE(su.id, fo.user_id) AS owner_id
    FROM lunaerp_baseinfo_customer c2
    LEFT JOIN system_users su
           ON su.tenant_id = c2.tenant_id
          AND su.deleted = 0
          AND c2.creator ~ '^[0-9]+$'
          AND su.id = NULLIF(c2.creator, '')::int8
    LEFT JOIN fallback_owner fo ON fo.tenant_id = c2.tenant_id
) x
WHERE c.id = x.id
  AND c.salesperson_user_id IS NULL
  AND x.owner_id IS NOT NULL;

-- 3.2 客户归属部门：归属人当前部门（含 creator 与兜底用户两种来源）
UPDATE lunaerp_baseinfo_customer c
SET dept_id = u.dept_id
FROM system_users u
WHERE c.dept_id IS NULL
  AND c.salesperson_user_id = u.id
  AND u.deleted = 0
  AND u.dept_id IS NOT NULL;

-- 3.3 客户归属部门：creator 用户部门
UPDATE lunaerp_baseinfo_customer c
SET dept_id = su.dept_id
FROM system_users su
WHERE c.dept_id IS NULL
  AND su.deleted = 0
  AND su.tenant_id = c.tenant_id
  AND c.creator ~ '^[0-9]+$'
  AND su.id = NULLIF(c.creator, '')::int8
  AND su.dept_id IS NOT NULL;

-- 3.4 客户归属部门兜底：租户根部门
WITH root_dept AS (
    SELECT DISTINCT ON (tenant_id) tenant_id, id AS dept_id
    FROM system_dept
    WHERE parent_id = 0 AND deleted = 0
    ORDER BY tenant_id, id
)
UPDATE lunaerp_baseinfo_customer c
SET dept_id = rd.dept_id
FROM root_dept rd
WHERE c.dept_id IS NULL
  AND rd.tenant_id = c.tenant_id
  AND rd.dept_id IS NOT NULL;

-- 3.5 销售合同归属部门：已有归属业务员当前部门
UPDATE lunaerp_business_sales_contract t
SET dept_id = u.dept_id
FROM system_users u
WHERE t.dept_id IS NULL
  AND t.salesperson_user_id = u.id
  AND u.deleted = 0
  AND u.dept_id IS NOT NULL;

-- 3.6 销售合同归属部门：creator 用户部门
UPDATE lunaerp_business_sales_contract t
SET dept_id = su.dept_id
FROM system_users su
WHERE t.dept_id IS NULL
  AND su.deleted = 0
  AND su.tenant_id = t.tenant_id
  AND t.creator ~ '^[0-9]+$'
  AND su.id = NULLIF(t.creator, '')::int8
  AND su.dept_id IS NOT NULL;

-- 3.7 销售合同归属部门：租户兜底用户部门
WITH fallback_owner AS (
    SELECT DISTINCT ON (u.tenant_id) u.tenant_id, u.id AS user_id, u.dept_id
    FROM system_users u
    LEFT JOIN system_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
    LEFT JOIN system_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.code = 'tenant_admin'
    WHERE u.deleted = 0
    ORDER BY u.tenant_id, (r.code = 'tenant_admin') DESC NULLS LAST, u.id
)
UPDATE lunaerp_business_sales_contract t
SET dept_id = fo.dept_id
FROM fallback_owner fo
WHERE t.dept_id IS NULL
  AND fo.tenant_id = t.tenant_id
  AND fo.dept_id IS NOT NULL;

-- 3.8 销售合同归属部门兜底：租户根部门
WITH root_dept AS (
    SELECT DISTINCT ON (tenant_id) tenant_id, id AS dept_id
    FROM system_dept
    WHERE parent_id = 0 AND deleted = 0
    ORDER BY tenant_id, id
)
UPDATE lunaerp_business_sales_contract t
SET dept_id = rd.dept_id
FROM root_dept rd
WHERE t.dept_id IS NULL
  AND rd.tenant_id = t.tenant_id
  AND rd.dept_id IS NOT NULL;

-- 3.9 发货单归属部门：已有归属业务员当前部门
UPDATE lunaerp_business_shipment t
SET dept_id = u.dept_id
FROM system_users u
WHERE t.dept_id IS NULL
  AND t.salesperson_user_id = u.id
  AND u.deleted = 0
  AND u.dept_id IS NOT NULL;

-- 3.10 发货单归属部门：creator 用户部门
UPDATE lunaerp_business_shipment t
SET dept_id = su.dept_id
FROM system_users su
WHERE t.dept_id IS NULL
  AND su.deleted = 0
  AND su.tenant_id = t.tenant_id
  AND t.creator ~ '^[0-9]+$'
  AND su.id = NULLIF(t.creator, '')::int8
  AND su.dept_id IS NOT NULL;

-- 3.11 发货单归属部门：租户兜底用户部门
WITH fallback_owner AS (
    SELECT DISTINCT ON (u.tenant_id) u.tenant_id, u.id AS user_id, u.dept_id
    FROM system_users u
    LEFT JOIN system_user_role ur ON ur.user_id = u.id AND ur.deleted = 0
    LEFT JOIN system_role r ON r.id = ur.role_id AND r.deleted = 0 AND r.code = 'tenant_admin'
    WHERE u.deleted = 0
    ORDER BY u.tenant_id, (r.code = 'tenant_admin') DESC NULLS LAST, u.id
)
UPDATE lunaerp_business_shipment t
SET dept_id = fo.dept_id
FROM fallback_owner fo
WHERE t.dept_id IS NULL
  AND fo.tenant_id = t.tenant_id
  AND fo.dept_id IS NOT NULL;

-- 3.12 发货单归属部门兜底：租户根部门
WITH root_dept AS (
    SELECT DISTINCT ON (tenant_id) tenant_id, id AS dept_id
    FROM system_dept
    WHERE parent_id = 0 AND deleted = 0
    ORDER BY tenant_id, id
)
UPDATE lunaerp_business_shipment t
SET dept_id = rd.dept_id
FROM root_dept rd
WHERE t.dept_id IS NULL
  AND rd.tenant_id = t.tenant_id
  AND rd.dept_id IS NOT NULL;

-- 3.13 归属校正工具（默认注释，仅排障/补刷时整段手动执行：
--       按归属人当前部门全量重刷 4 表 dept_id，与调岗切面效果等价；
--       非只填空，会覆盖既有值，执行前请确认）：
-- UPDATE lunaerp_baseinfo_customer t SET dept_id = u.dept_id
--   FROM system_users u WHERE t.salesperson_user_id = u.id AND u.deleted = 0 AND t.deleted = 0;
-- UPDATE lunaerp_business_sales_contract t SET dept_id = u.dept_id
--   FROM system_users u WHERE t.salesperson_user_id = u.id AND u.deleted = 0 AND t.deleted = 0;
-- UPDATE lunaerp_business_shipment t SET dept_id = u.dept_id
--   FROM system_users u WHERE t.salesperson_user_id = u.id AND u.deleted = 0 AND t.deleted = 0;
-- UPDATE lunaerp_business_quotation t SET dept_id = u.dept_id
--   FROM system_users u WHERE t.salesperson_user_id = u.id AND u.deleted = 0 AND t.deleted = 0;

-- =============================================================================
-- 4. 角色数据范围对齐（system_role.data_scope：1 全部/4 本部门及以下/3 本部门/5 本人）
--    4.1 存量自定义档（2）迁移并清空自定义部门；4.2 标准业务角色出厂基线（角色未 seed 时影响 0 行）
-- =============================================================================

-- 4.1 存量平台角色：common 普通角色 -> 仅本人（最小权限）；test-dp 测试角色 -> 全部
UPDATE system_role
SET data_scope = 5, data_scope_dept_ids = ''
WHERE code = 'common' AND deleted = 0 AND data_scope = 2;

UPDATE system_role
SET data_scope = 1, data_scope_dept_ids = ''
WHERE code = 'test-dp' AND deleted = 0 AND data_scope = 2;

-- 4.2 11 个标准业务角色基线（ADR-0046 / 16 号验收 §6.1；仅本租户 1 种子体系）
UPDATE system_role
SET data_scope = 1, data_scope_dept_ids = ''
WHERE deleted = 0
  AND code IN ('boss', 'finance', 'planner', 'purchaser',
               'warehouse_manager', 'warehouse_keeper', 'inspector');

UPDATE system_role
SET data_scope = 4, data_scope_dept_ids = ''
WHERE deleted = 0 AND code = 'sales_manager';

UPDATE system_role
SET data_scope = 5, data_scope_dept_ids = ''
WHERE deleted = 0
  AND code IN ('salesperson', 'workshop_worker', 'workshop_inspector');
