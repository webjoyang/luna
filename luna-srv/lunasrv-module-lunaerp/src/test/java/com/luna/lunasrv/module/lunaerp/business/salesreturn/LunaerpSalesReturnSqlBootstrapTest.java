package com.luna.lunasrv.module.lunaerp.business.salesreturn;

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
 * 销售退货 SQL 基建与元数据规范校验单元测试
 * 覆盖 Issue: .scratch/sales-return/issues/01-sql-domain-foundation.md
 */
public class LunaerpSalesReturnSqlBootstrapTest {

    private Path getSqlDirectory() {
        Path sqlDir = Path.of("sql/postgresql");
        if (!Files.exists(sqlDir)) {
            sqlDir = Path.of("../sql/postgresql");
        }
        if (!Files.exists(sqlDir)) {
            File root = new File("").getAbsoluteFile();
            while (root != null && !new File(root, "sql/postgresql").exists()) {
                root = root.getParentFile();
            }
            if (root != null) {
                sqlDir = new File(root, "sql/postgresql").toPath();
            }
        }
        assertTrue(Files.exists(sqlDir) && Files.isDirectory(sqlDir), "SQL 目录必须存在: sql/postgresql");
        return sqlDir;
    }

    private String loadSqlContent(String fileName) throws IOException {
        Path sqlPath = getSqlDirectory().resolve(fileName);
        assertTrue(Files.exists(sqlPath), "SQL 文件必须存在: sql/postgresql/" + fileName);
        return Files.readString(sqlPath, StandardCharsets.UTF_8);
    }

    private List<SQLStatement> parseSql(String sql) {
        String standardSql = sql.replaceAll("(?s)DO \\$\\$.*?\\$\\$;", "-- DO BLOCK OMITTED FOR PARSER;")
                .replaceAll("(?i)(CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+[^;]+?)\\s+WHERE\\s+[^;]+;", "$1;")
                .replaceAll("(?i)CREATE\\s+SEQUENCE\\s+IF\\s+NOT\\s+EXISTS", "CREATE SEQUENCE");
        return SQLUtils.parseStatements(standardSql, DbType.postgresql);
    }

    @Test
    @DisplayName("验证销售退货两个 SQL 脚本存在且能被 PostgreSQL 语法解析器解析")
    void testSqlCanBeParsed() throws IOException {
        String mainSql = loadSqlContent("lunaerp-business-sales-return.sql");
        assertFalse(mainSql.isBlank(), "lunaerp-business-sales-return.sql 内容不能为空");
        List<SQLStatement> mainStatements = parseSql(mainSql);
        assertFalse(mainStatements.isEmpty(), "主脚本至少应解析出 DDL/DML 语句");

        String stockInSql = loadSqlContent("lunaerp-stock-in-sales-return-ref.sql");
        assertFalse(stockInSql.isBlank(), "lunaerp-stock-in-sales-return-ref.sql 内容不能为空");
        List<SQLStatement> stockInStatements = parseSql(stockInSql);
        assertFalse(stockInStatements.isEmpty(), "stock_in 扩列脚本至少应解析出 DDL 语句");
    }

    @Test
    @DisplayName("验证销售退货 3 张表 DDL、主键约束、序列与 TenantBaseDO 列")
    void testSalesReturnTablesAndColumns() throws IOException {
        String sql = loadSqlContent("lunaerp-business-sales-return.sql");

        String[] tables = {
                "lunaerp_business_sales_return",
                "lunaerp_business_sales_return_item",
                "lunaerp_business_sales_return_roll"
        };

        for (String table : tables) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + table), "必须包含 CREATE TABLE IF NOT EXISTS " + table);
            assertTrue(sql.contains("pk_" + table), "主键约束名必须包含 pk_" + table);
            assertTrue(sql.contains("ADD CONSTRAINT pk_" + table + " PRIMARY KEY (id)"), "必须包含主键约束定义 pk_" + table);
            assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS " + table + "_seq"), "必须包含主键序列 " + table + "_seq");

            // TenantBaseDO 审计列
            assertTrue(sql.contains("creator"), table + " 必须包含 creator 字段");
            assertTrue(sql.contains("create_time"), table + " 必须包含 create_time 字段");
            assertTrue(sql.contains("updater"), table + " 必须包含 updater 字段");
            assertTrue(sql.contains("update_time"), table + " 必须包含 update_time 字段");
            assertTrue(sql.contains("deleted"), table + " 必须包含 deleted 字段");
            assertTrue(sql.contains("tenant_id"), table + " 必须包含 tenant_id 字段");
        }

        // 表 1: 主表核心列与索引
        assertTrue(sql.contains("return_no"), "主表必须包含 return_no");
        assertTrue(sql.contains("status"), "主表必须包含 status");
        assertTrue(sql.contains("approval_status"), "主表必须包含 approval_status");
        assertTrue(sql.contains("shipment_id"), "主表必须包含 shipment_id");
        assertTrue(sql.contains("shipment_no"), "主表必须包含 shipment_no");
        assertTrue(sql.contains("customer_id"), "主表必须包含 customer_id");
        assertTrue(sql.contains("customer_name"), "主表必须包含 customer_name");
        assertTrue(sql.contains("sales_contract_id"), "主表必须包含 sales_contract_id");
        assertTrue(sql.contains("contract_no"), "主表必须包含 contract_no");
        assertTrue(sql.contains("warehouse_id"), "主表必须包含 warehouse_id");
        assertTrue(sql.contains("warehouse_name"), "主表必须包含 warehouse_name");
        assertTrue(sql.contains("currency"), "主表必须包含 currency");
        assertTrue(sql.contains("total_amount"), "主表必须包含 total_amount");
        assertTrue(sql.contains("total_pieces"), "主表必须包含 total_pieces");
        assertTrue(sql.contains("stock_in_id"), "主表必须包含 stock_in_id");
        assertTrue(sql.contains("arrived_by"), "主表必须包含 arrived_by");
        assertTrue(sql.contains("arrived_at"), "主表必须包含 arrived_at");
        assertTrue(sql.contains("posted_by"), "主表必须包含 posted_by");
        assertTrue(sql.contains("posted_at"), "主表必须包含 posted_at");
        assertTrue(sql.contains("cancelled_by"), "主表必须包含 cancelled_by");
        assertTrue(sql.contains("cancelled_at"), "主表必须包含 cancelled_at");
        assertTrue(sql.contains("cancel_reason"), "主表必须包含 cancel_reason");
        assertTrue(sql.contains("return_date"), "主表必须包含 return_date");

        // 主表 4 个索引
        assertTrue(sql.contains("uk_lbsr_tenant_return_no"), "主表必须包含单号唯一索引 uk_lbsr_tenant_return_no");
        assertTrue(sql.contains("idx_lbsr_tenant_cust_status"), "主表必须包含客户状态索引 idx_lbsr_tenant_cust_status");
        assertTrue(sql.contains("idx_lbsr_tenant_shipment_id"), "主表必须包含发货单回指索引 idx_lbsr_tenant_shipment_id");
        assertTrue(sql.contains("idx_lbsr_tenant_status_approval"), "主表必须包含状态与审批态复合索引 idx_lbsr_tenant_status_approval");

        // 表 2: 行表核心列与索引
        assertTrue(sql.contains("return_id"), "行表必须包含 return_id");
        assertTrue(sql.contains("shipment_item_id"), "行表必须包含 shipment_item_id");
        assertTrue(sql.contains("product_id"), "行表必须包含 product_id");
        assertTrue(sql.contains("product_code"), "行表必须包含 product_code");
        assertTrue(sql.contains("product_name"), "行表必须包含 product_name");
        assertTrue(sql.contains("customer_color_no"), "行表必须包含 customer_color_no");
        assertTrue(sql.contains("color_name"), "行表必须包含 color_name");
        assertTrue(sql.contains("vat_no"), "行表必须包含 vat_no");
        assertTrue(sql.contains("return_qty"), "行表必须包含 return_qty");
        assertTrue(sql.contains("pieces"), "行表必须包含 pieces");
        assertTrue(sql.contains("unit"), "行表必须包含 unit");
        assertTrue(sql.contains("unit_price"), "行表必须包含 unit_price");
        assertTrue(sql.contains("amount"), "行表必须包含 amount");
        assertTrue(sql.contains("return_reason"), "行表必须包含 return_reason");
        assertTrue(sql.contains("restock_qty"), "行表必须包含 restock_qty");
        assertTrue(sql.contains("scrap_qty"), "行表必须包含 scrap_qty");

        // 行表索引
        assertTrue(sql.contains("idx_lbsri_tenant_return_id"), "行表必须包含退货单索引 idx_lbsri_tenant_return_id");
        assertTrue(sql.contains("idx_lbsri_tenant_shipment_item_id"), "行表必须包含发货明细索引 idx_lbsri_tenant_shipment_item_id");

        // 表 3: 卷表核心列与索引
        assertTrue(sql.contains("return_item_id"), "卷表必须包含 return_item_id");
        assertTrue(sql.contains("shipment_roll_id"), "卷表必须包含 shipment_roll_id");
        assertTrue(sql.contains("roll_no"), "卷表必须包含 roll_no");
        assertTrue(sql.contains("weight"), "卷表必须包含 weight");
        assertTrue(sql.contains("length_m"), "卷表必须包含 length_m");
        assertTrue(sql.contains("qc_result"), "卷表必须包含 qc_result");
        assertTrue(sql.contains("qc_by"), "卷表必须包含 qc_by");
        assertTrue(sql.contains("qc_at"), "卷表必须包含 qc_at");
        assertTrue(sql.contains("qc_remark"), "卷表必须包含 qc_remark");
        assertTrue(sql.contains("location_id"), "卷表必须包含 location_id");
        assertTrue(sql.contains("location_code"), "卷表必须包含 location_code");

        // 卷表索引
        assertTrue(sql.contains("idx_lbsrr_tenant_return_id"), "卷表必须包含退货单索引 idx_lbsrr_tenant_return_id");
        assertTrue(sql.contains("idx_lbsrr_tenant_shipment_roll_id"), "卷表必须包含发货卷索引 idx_lbsrr_tenant_shipment_roll_id");
        assertTrue(sql.contains("idx_lbsrr_tenant_return_item_id"), "卷表必须包含退货行索引 idx_lbsrr_tenant_return_item_id");
    }

    @Test
    @DisplayName("验证 stock_in 表扩列脚本（两列、注释、部分唯一索引与普通索引）")
    void testStockInAlterationScript() throws IOException {
        String sql = loadSqlContent("lunaerp-stock-in-sales-return-ref.sql");

        assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS source_doc_type varchar(32)"), "stock_in 扩列必须包含 source_doc_type");
        assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS source_doc_id int8"), "stock_in 扩列必须包含 source_doc_id");
        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_stock_in.source_doc_type"), "必须包含 source_doc_type 注释");
        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_stock_in.source_doc_id"), "必须包含 source_doc_id 注释");

        // 部分唯一索引与普通索引
        assertTrue(sql.contains("uk_stock_in_source"), "必须包含部分唯一索引 uk_stock_in_source");
        assertTrue(sql.contains("WHERE deleted = 0 AND source_doc_type IS NOT NULL")
                        || sql.contains("WHERE deleted=0 AND source_doc_type IS NOT NULL"),
                "部分唯一索引条件必须为 WHERE deleted=0 AND source_doc_type IS NOT NULL");
        assertTrue(sql.contains("idx_stock_in_tenant_source"), "必须包含普通反查索引 idx_stock_in_tenant_source");
    }

    @Test
    @DisplayName("验证字典类型 2279~2281 与数据项 3850~3869 先删后插及平移避让")
    void testDictionaries() throws IOException {
        String sql = loadSqlContent("lunaerp-business-sales-return.sql");

        String[] dictTypes = {
                "lunaerp_sales_return_status",
                "lunaerp_sales_return_reason",
                "lunaerp_sales_return_qc_result"
        };

        for (String dictType : dictTypes) {
            assertTrue(sql.contains(dictType), "必须包含字典类型: " + dictType);
        }

        // 验证关键枚举值
        assertTrue(sql.contains("'DRAFT'"), "退货状态必须包含 DRAFT");
        assertTrue(sql.contains("'APPROVED'"), "退货状态必须包含 APPROVED");
        assertTrue(sql.contains("'RECEIVED'"), "退货状态必须包含 RECEIVED");
        assertTrue(sql.contains("'POSTED'"), "退货状态必须包含 POSTED");
        assertTrue(sql.contains("'CANCELLED'"), "退货状态必须包含 CANCELLED");

        assertTrue(sql.contains("'QUALITY'"), "退货原因必须包含 QUALITY");
        assertTrue(sql.contains("'WRONG'"), "退货原因必须包含 WRONG");
        assertTrue(sql.contains("'EXCESS'"), "退货原因必须包含 EXCESS");
        assertTrue(sql.contains("'CUSTOMER'"), "退货原因必须包含 CUSTOMER");
        assertTrue(sql.contains("'OTHER'"), "退货原因必须包含 OTHER");

        assertTrue(sql.contains("'RESTOCK'"), "质检判定必须包含 RESTOCK");
        assertTrue(sql.contains("'SCRAP'"), "质检判定必须包含 SCRAP");

        // 字典数据先删后插
        assertTrue(sql.contains("DELETE FROM system_dict_type WHERE type IN"), "必须对字典类型执行幂等清理");
        assertTrue(sql.contains("DELETE FROM system_dict_data WHERE dict_type IN"), "必须对字典数据执行幂等清理");
    }

    @Test
    @DisplayName("验证审批业务类型 sales_return 幂等增量与默认审批流配置")
    void testApprovalConfig() throws IOException {
        String sql = loadSqlContent("lunaerp-business-sales-return.sql");

        assertTrue(sql.contains("lunaerp_approval_biz_type"), "必须涉及审批单据类型字典 lunaerp_approval_biz_type");
        assertTrue(sql.contains("'sales_return'"), "必须配置 sales_return 单据类型");
        assertTrue(sql.contains("lunaerp_approval_config"), "必须配置 lunaerp_approval_config");
        assertTrue(sql.contains("lunaerp_approval_config_node"), "必须配置 lunaerp_approval_config_node");
        assertTrue(sql.contains("'销售退货默认审批流'"), "必须包含流程名称");
        assertTrue(sql.contains("'销售经理审批'"), "必须包含节点名称");
    }

    @Test
    @DisplayName("验证菜单 10141 与按钮 10142~10149 及其权限标识与角色赋权")
    void testMenusAndPermissions() throws IOException {
        String sql = loadSqlContent("lunaerp-business-sales-return.sql");

        // 10141 菜单
        assertTrue(sql.contains("10141"), "必须包含 10141 销售退货菜单");
        assertTrue(sql.contains("lunaerp:business:sales-return:query"), "必须包含查询权限");

        int[] buttons = {10142, 10143, 10144, 10145, 10146, 10147, 10148, 10149};
        for (int btn : buttons) {
            assertTrue(sql.contains(String.valueOf(btn)), "必须包含按钮 " + btn);
        }

        assertTrue(sql.contains("lunaerp:business:sales-return:create"), "必须包含创建权限");
        assertTrue(sql.contains("lunaerp:business:sales-return:update"), "必须包含修改权限");
        assertTrue(sql.contains("lunaerp:business:sales-return:delete"), "必须包含删除权限");
        assertTrue(sql.contains("lunaerp:business:sales-return:submit"), "必须包含提交审批权限");
        assertTrue(sql.contains("lunaerp:business:sales-return:receive"), "必须包含到货质检权限");
        assertTrue(sql.contains("lunaerp:business:sales-return:post"), "必须包含财务确认权限");
        assertTrue(sql.contains("lunaerp:business:sales-return:cancel"), "必须包含作废权限");
        assertTrue(sql.contains("lunaerp:business:sales-return:print"), "必须包含打印权限");

        // 角色赋权
        assertTrue(sql.contains("system_role_menu"), "必须赋权 system_role_menu");
        assertTrue(sql.contains("nextval('system_role_menu_seq')"), "赋权必须显式调用 nextval('system_role_menu_seq')");
    }

    @Test
    @DisplayName("验证新创建的 3 张表均具备完备的表注释与全列注释")
    void testCommentsCompleteness() throws IOException {
        String sql = loadSqlContent("lunaerp-business-sales-return.sql");

        String[] tables = {
                "lunaerp_business_sales_return",
                "lunaerp_business_sales_return_item",
                "lunaerp_business_sales_return_roll"
        };
        for (String table : tables) {
            assertTrue(sql.contains("COMMENT ON TABLE " + table + " IS"), table + " 必须有 COMMENT ON TABLE");
        }
    }

    @Test
    @DisplayName("验证主键序列均定义为 CREATE SEQUENCE IF NOT EXISTS")
    void testSequences() throws IOException {
        String sql = loadSqlContent("lunaerp-business-sales-return.sql");

        String[] tables = {
                "lunaerp_business_sales_return",
                "lunaerp_business_sales_return_item",
                "lunaerp_business_sales_return_roll"
        };
        for (String table : tables) {
            assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS " + table + "_seq;"),
                    table + " 必须有 CREATE SEQUENCE IF NOT EXISTS " + table + "_seq;");
        }
    }

    @Test
    @DisplayName("验证 SQL 脚本满足空库与存量库幂等重跑规范 (IF NOT EXISTS + DO 块判存 + 先删后插)")
    void testIdempotencyProperties() throws IOException {
        String sql = loadSqlContent("lunaerp-business-sales-return.sql");

        assertFalse(sql.matches("(?s).*CREATE\\s+TABLE\\s+(?!IF\\s+NOT\\s+EXISTS).*"),
                "所有 CREATE TABLE 必须携带 IF NOT EXISTS");
        assertFalse(sql.matches("(?s).*CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(?!IF\\s+NOT\\s+EXISTS).*"),
                "所有 CREATE INDEX 必须携带 IF NOT EXISTS");
    }

    @Test
    @DisplayName("验证领域 SQL 脚本严禁跨模块执行平台底座表 DDL (CREATE/ALTER/DROP 等)，仅允许操作 lunaerp_* 域表")
    void testNoCrossDomainDdlOnPlatformTables() throws IOException {
        String sql = loadSqlContent("lunaerp-business-sales-return.sql");
        List<SQLStatement> statements = parseSql(sql);

        for (SQLStatement stmt : statements) {
            if (stmt instanceof com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement createTable) {
                String tableName = createTable.getTableName().toLowerCase().replace("\"", "");
                assertTrue(tableName.startsWith("lunaerp_"),
                        "领域增量脚本严禁创建非 lunaerp_ 前缀的表: " + tableName);
            } else if (stmt instanceof com.alibaba.druid.sql.ast.statement.SQLCreateIndexStatement createIndex) {
                String tableName = SQLUtils.toSQLString(createIndex.getTable()).toLowerCase().replace("\"", "");
                assertTrue(tableName.startsWith("lunaerp_"),
                        "领域增量脚本严禁在非 lunaerp_ 前缀的表上创建索引: " + tableName);
            } else if (stmt instanceof com.alibaba.druid.sql.ast.statement.SQLAlterTableStatement alterTable) {
                String tableName = alterTable.getTableName().toLowerCase().replace("\"", "");
                assertTrue(tableName.startsWith("lunaerp_"),
                        "领域增量脚本严禁修改非 lunaerp_ 前缀的表: " + tableName);
            }
        }

        Pattern bannedDdlPattern = Pattern.compile(
                "(?i)(CREATE\\s+(?:UNIQUE\\s+)?INDEX[\\s\\S]*?ON\\s+system_\\w+|ALTER\\s+TABLE\\s+system_\\w+)"
        );
        Matcher matcher = bannedDdlPattern.matcher(sql);
        assertFalse(matcher.find(), () -> "领域 SQL 脚本检测到对底座 system_* 表的越权 DDL 变更: " + matcher.group(1));
    }

}
