package com.luna.lunasrv.module.lunaerp.datapermission;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 针对销售归属链数据范围接线 SQL 基建脚本 lunaerp-data-permission-wiring.sql 的单元测试与规范校验
 * 覆盖 issue: .scratch/data-permission-wiring/issues/01-sql-bootstrap.md
 */
public class LunaerpDataPermissionWiringSqlBootstrapTest {

    private String loadSqlContent() throws IOException {
        Path sqlPath = Path.of("sql/postgresql/lunaerp-data-permission-wiring.sql");
        if (!Files.exists(sqlPath)) {
            sqlPath = Path.of("../sql/postgresql/lunaerp-data-permission-wiring.sql");
        }
        if (!Files.exists(sqlPath)) {
            File root = new File("").getAbsoluteFile();
            while (root != null && !new File(root, "sql/postgresql/lunaerp-data-permission-wiring.sql").exists()) {
                root = root.getParentFile();
            }
            if (root != null) {
                sqlPath = new File(root, "sql/postgresql/lunaerp-data-permission-wiring.sql").toPath();
            }
        }
        assertTrue(Files.exists(sqlPath), "SQL 文件必须存在于 sql/postgresql/lunaerp-data-permission-wiring.sql");
        return Files.readString(sqlPath, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("验证 SQL 脚本存在且能被 PostgreSQL 语法解析器解析")
    void testSqlCanBeParsed() throws IOException {
        String sql = loadSqlContent();
        assertFalse(sql.isBlank(), "SQL 文件内容不能为空");

        // 部分索引 WHERE 子句为 PG 特有语法，Druid PG 方言解析时需做部分兼容预处理
        String standardSql = sql.replaceAll("(?i)(CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+[^;]+?)\\s+WHERE\\s+[^;]+;", "$1;")
                                .replaceAll("(?s)/\\*.*?\\*/", "")
                                .replaceAll("--.*?(\r?\n)", "\n");
        List<SQLStatement> statements = SQLUtils.parseStatements(standardSql, DbType.postgresql);
        assertFalse(statements.isEmpty(), "至少应成功解析出 DDL/DML 语句");
    }

    @Test
    @DisplayName("验证客户/合同/发货 3 表新增列 DDL 幂等就位")
    void testAddColumnsIdempotent() throws IOException {
        String sql = loadSqlContent();

        // 客户表加 salesperson_user_id 与 dept_id
        assertTrue(sql.contains("ALTER TABLE lunaerp_baseinfo_customer"), "需对客户表进行 ALTER TABLE");
        assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS salesperson_user_id int8 NULL"),
                "客户表需包含 ADD COLUMN IF NOT EXISTS salesperson_user_id int8 NULL");
        assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS dept_id int8 NULL"),
                "客户表需包含 ADD COLUMN IF NOT EXISTS dept_id int8 NULL");

        // 合同表加 dept_id
        assertTrue(sql.contains("ALTER TABLE lunaerp_business_sales_contract"), "需对销售合同表进行 ALTER TABLE");

        // 发货表加 dept_id
        assertTrue(sql.contains("ALTER TABLE lunaerp_business_shipment"), "需对发货表进行 ALTER TABLE");

        // 4 处 ADD COLUMN 均需带 IF NOT EXISTS
        Pattern pattern = Pattern.compile("(?i)ADD\\s+COLUMN\\s+IF\\s+NOT\\s+EXISTS\\s+(salesperson_user_id|dept_id)\\s+int8\\s+NULL");
        Matcher matcher = pattern.matcher(sql);
        int matchCount = 0;
        while (matcher.find()) {
            matchCount++;
        }
        assertEquals(4, matchCount, "必须有 4 处 ADD COLUMN IF NOT EXISTS (客户 2 列 + 合同 1 列 + 发货 1 列)");
    }

    @Test
    @DisplayName("验证 4 个新增列的 COMMENT 注释完备")
    void testColumnComments() throws IOException {
        String sql = loadSqlContent();

        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_baseinfo_customer.salesperson_user_id IS"),
                "需包含客户表 salesperson_user_id 注释");
        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_baseinfo_customer.dept_id IS"),
                "需包含客户表 dept_id 注释");
        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_business_sales_contract.dept_id IS"),
                "需包含合同表 dept_id 注释");
        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_business_shipment.dept_id IS"),
                "需包含发货表 dept_id 注释");
    }

    @Test
    @DisplayName("验证 6 个 dp 部分索引就位 (WHERE deleted=0)")
    void testDataPermissionPartialIndexes() throws IOException {
        String sql = loadSqlContent();

        String[] expectedIndexes = {
            "idx_lb_customer_dp_dept",
            "idx_lb_customer_dp_user",
            "idx_lb_sales_contract_dp_dept",
            "idx_lb_sales_contract_dp_user",
            "idx_lb_shipment_dp_dept",
            "idx_lb_shipment_dp_user"
        };

        for (String idx : expectedIndexes) {
            assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS " + idx),
                    "需包含 CREATE INDEX IF NOT EXISTS " + idx);
        }

        // 索引定义结构校验
        assertTrue(sql.contains("ON lunaerp_baseinfo_customer (tenant_id, dept_id) WHERE deleted = 0"));
        assertTrue(sql.contains("ON lunaerp_baseinfo_customer (tenant_id, salesperson_user_id) WHERE deleted = 0"));
        assertTrue(sql.contains("ON lunaerp_business_sales_contract (tenant_id, dept_id) WHERE deleted = 0"));
        assertTrue(sql.contains("ON lunaerp_business_sales_contract (tenant_id, salesperson_user_id) WHERE deleted = 0"));
        assertTrue(sql.contains("ON lunaerp_business_shipment (tenant_id, dept_id) WHERE deleted = 0"));
        assertTrue(sql.contains("ON lunaerp_business_shipment (tenant_id, salesperson_user_id) WHERE deleted = 0"));
    }

    @Test
    @DisplayName("验证存量回填全部带目标列 IS NULL 守卫与租户感知兜底逻辑")
    void testBackfillIdempotenceAndNullGuards() throws IOException {
        String sql = loadSqlContent();

        // 客户 owner 填空：creator -> 租户兜底 admin
        assertTrue(sql.contains("c.salesperson_user_id IS NULL"), "客户 owner 回填需有 IS NULL 守卫");
        assertTrue(sql.contains("c2.creator ~ '^[0-9]+$'"), "客户 creator 关联需包含数字正则守卫");
        assertTrue(sql.contains("su.id = NULLIF(c2.creator, '')::int8"), "客户 creator 转 int8 安全强转");

        // 兜底用户按 CTE 动态定位：优先 tenant_admin，否则最小 id，且带 NULLS LAST 防止 PostgreSQL NULL 排在 TRUE 之前
        assertTrue(sql.contains("DISTINCT ON (u.tenant_id)"), "兜底用户需按租户 DISTINCT ON");
        assertTrue(sql.contains("(r.code = 'tenant_admin') DESC NULLS LAST"),
                "兜底用户排序需包含 (r.code = 'tenant_admin') DESC NULLS LAST");
        assertTrue(sql.contains("ur.deleted = 0"), "用户角色关联需过滤 deleted = 0");

        // 根部门兜底：parent_id = 0 AND deleted = 0 取最小 id
        assertTrue(sql.contains("parent_id = 0 AND deleted = 0"), "根部门按 parent_id = 0 AND deleted = 0");

        // 客户 dept 填空：owner dept -> creator dept -> 根部门
        assertTrue(sql.contains("c.dept_id IS NULL"), "客户 dept 回填需有 IS NULL 守卫");
        assertTrue(sql.contains("c.salesperson_user_id = u.id"), "客户 dept 需按 salesperson_user_id 关联");
        assertTrue(sql.contains("rd.tenant_id = c.tenant_id"), "客户 dept 兜底需匹配租户");

        // 合同 dept 填空：已有 salesperson dept -> creator dept -> 兜底用户 dept -> 根部门
        assertTrue(sql.contains("t.dept_id IS NULL"), "合同/发货 dept 回填需有 IS NULL 守卫");
        assertTrue(sql.contains("t.salesperson_user_id = u.id"), "合同/发货 dept 优先按 salesperson 部门推导");
        assertTrue(sql.contains("t.creator ~ '^[0-9]+$'"), "合同/发货 creator 需有数字正则守卫");
        assertTrue(sql.contains("fo.tenant_id = t.tenant_id"), "合同/发货兜底用户需匹配租户");

        // 确认禁止写死任何字面量 ID（例如硬编码 id = 1 或 id = 100）
        Pattern hardcodedAdminPattern = Pattern.compile("(?i)WHERE\\s+.*\\b(user_id|salesperson_user_id)\\s*=\\s*1\\b");
        assertFalse(hardcodedAdminPattern.matcher(sql).find(), "禁止在回填逻辑中写死 user_id = 1");
    }

    @Test
    @DisplayName("验证角色 data_scope 基线与自定义档迁移逻辑")
    void testRoleDataScopeAlignment() throws IOException {
        String sql = loadSqlContent();

        // 存量自定义档收口：common -> 5 (SELF), test-dp -> 1 (ALL)
        assertTrue(sql.contains("SET data_scope = 5, data_scope_dept_ids = ''"),
                "common 角色需迁移至 5 并清空 data_scope_dept_ids");
        assertTrue(sql.contains("WHERE code = 'common' AND deleted = 0 AND data_scope = 2"),
                "common 角色迁移需限定 code='common' 且 data_scope=2");

        assertTrue(sql.contains("SET data_scope = 1, data_scope_dept_ids = ''"),
                "test-dp 角色需迁移至 1 并清空 data_scope_dept_ids");
        assertTrue(sql.contains("WHERE code = 'test-dp' AND deleted = 0 AND data_scope = 2"),
                "test-dp 角色迁移需限定 code='test-dp' 且 data_scope=2");

        // 11 个种子角色基线：
        // 7 个 ALL 档角色 (data_scope = 1)
        String[] allRoles = {"boss", "finance", "planner", "purchaser", "warehouse_manager", "warehouse_keeper", "inspector"};
        for (String role : allRoles) {
            assertTrue(sql.contains("'" + role + "'"), "需包含 ALL 档角色 " + role);
        }

        // 1 个 DEPT_AND_CHILD 档角色 (data_scope = 4)
        assertTrue(sql.contains("WHERE deleted = 0 AND code = 'sales_manager'"),
                "sales_manager 需设为 data_scope = 4");

        // 3 个 SELF 档角色 (data_scope = 5)
        String[] selfRoles = {"salesperson", "workshop_worker", "workshop_inspector"};
        for (String role : selfRoles) {
            assertTrue(sql.contains("'" + role + "'"), "需包含 SELF 档角色 " + role);
        }

        // 所有对齐操作均置 data_scope_dept_ids = ''
        Pattern deptIdsClearPattern = Pattern.compile("data_scope_dept_ids\\s*=\\s*''");
        Matcher deptMatcher = deptIdsClearPattern.matcher(sql);
        int deptClearCount = 0;
        while (deptMatcher.find()) {
            deptClearCount++;
        }
        assertTrue(deptClearCount >= 5, "所有角色对齐语句均需清空 data_scope_dept_ids");
    }

    @Test
    @DisplayName("验证报价单表 lunaerp_business_quotation 不在本脚本中进行 ALTER")
    void testQuotationTableExcluded() throws IOException {
        String sql = loadSqlContent();
        assertFalse(sql.contains("ALTER TABLE lunaerp_business_quotation"),
                "报价单两表 DDL 由 05 票建表脚本自带，本脚本禁止 ALTER");
    }
}
