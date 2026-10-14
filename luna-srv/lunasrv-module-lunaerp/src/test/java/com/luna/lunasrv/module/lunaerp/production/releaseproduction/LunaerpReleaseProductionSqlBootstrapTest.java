package com.luna.lunasrv.module.lunaerp.production.releaseproduction;

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

import static org.junit.jupiter.api.Assertions.*;

/**
 * 针对合同「下达生产」SQL 变更脚本 lunaerp-production-release-production.sql 的单元测试与规范校验
 * 覆盖 issue: .scratch/release-production/issues/01-sql-and-do-fields.md
 */
public class LunaerpReleaseProductionSqlBootstrapTest {

    private String loadSqlContent() throws IOException {
        Path sqlPath = Path.of("sql/postgresql/lunaerp-production-release-production.sql");
        if (!Files.exists(sqlPath)) {
            sqlPath = Path.of("../sql/postgresql/lunaerp-production-release-production.sql");
        }
        if (!Files.exists(sqlPath)) {
            File root = new File("").getAbsoluteFile();
            while (root != null && !new File(root, "sql/postgresql/lunaerp-production-release-production.sql").exists()) {
                root = root.getParentFile();
            }
            if (root != null) {
                sqlPath = new File(root, "sql/postgresql/lunaerp-production-release-production.sql").toPath();
            }
        }
        assertTrue(Files.exists(sqlPath), "SQL 文件必须存在于 sql/postgresql/lunaerp-production-release-production.sql");
        return Files.readString(sqlPath, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("验证 SQL 脚本存在且能被 PostgreSQL 语法解析器解析")
    void testSqlCanBeParsed() throws IOException {
        String sql = loadSqlContent();
        assertFalse(sql.isBlank(), "SQL 文件内容不能为空");

        String standardSql = sql.replaceAll("(?s)DO \\$\\$.*?\\$\\$;", "-- DO BLOCK OMITTED FOR PARSER;")
                                .replaceAll("(?i)(CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+[^;]+?)\\s+WHERE\\s+[^;]+;", "$1;");
        List<SQLStatement> statements = SQLUtils.parseStatements(standardSql, DbType.postgresql);
        assertFalse(statements.isEmpty(), "至少应成功解析出 DDL/DML 语句");
    }

    @Test
    @DisplayName("验证 3 表加列采用 DO 块判列存在，保证脚本幂等可重复执行")
    void testDoBlocksAndAlterColumns() throws IOException {
        String sql = loadSqlContent();

        // 1. lunaerp_business_sales_contract_item.released_qty
        assertTrue(sql.contains("lunaerp_business_sales_contract_item"), "必须操作销售合同明细表");
        assertTrue(sql.contains("released_qty"), "合同明细表必须包含 released_qty 列");
        assertTrue(sql.contains("numeric(18,2) NOT NULL DEFAULT 0"), "released_qty 必须为 numeric(18,2) NOT NULL DEFAULT 0");

        // 2. lunaerp_production_greige_order_color.contract_line_id / contract_line_no
        assertTrue(sql.contains("lunaerp_production_greige_order_color"), "必须操作胚布单颜色行表");
        assertTrue(sql.contains("contract_line_id int8 NULL") || sql.contains("contract_line_id int8"), "胚布单颜色行表必须包含 contract_line_id");
        assertTrue(sql.contains("contract_line_no int4 NULL") || sql.contains("contract_line_no int4"), "胚布单颜色行表必须包含 contract_line_no");

        // 3. lunaerp_production_dyeing_color.contract_line_id / contract_line_no
        assertTrue(sql.contains("lunaerp_production_dyeing_color"), "必须操作染色单颜色明细表");

        // 4. DO 块与 information_schema 检查
        assertTrue(sql.contains("DO $$"), "必须包含 DO 块");
        assertTrue(sql.contains("information_schema.columns"), "DO 块内必须查询 information_schema.columns 判列是否存在");
    }

    @Test
    @DisplayName("验证所有新增列均有完备的 COMMENT ON COLUMN 说明")
    void testColumnComments() throws IOException {
        String sql = loadSqlContent();

        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_business_sales_contract_item.released_qty IS"),
                "需包含合同明细 released_qty 列注释");
        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_production_greige_order_color.contract_line_id IS"),
                "需包含胚布颜色行 contract_line_id 列注释");
        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_production_greige_order_color.contract_line_no IS"),
                "需包含胚布颜色行 contract_line_no 列注释");
        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_production_dyeing_color.contract_line_id IS"),
                "需包含染色颜色行 contract_line_id 列注释");
        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_production_dyeing_color.contract_line_no IS"),
                "需包含染色颜色行 contract_line_no 列注释");
    }

    @Test
    @DisplayName("验证辅助索引创建")
    void testIndexes() throws IOException {
        String sql = loadSqlContent();

        assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS idx_lp_greige_order_color_contract_line_id")
                        || sql.contains("idx_lp_greige_order_color_contract_line_id"),
                "需包含胚布单颜色行合同行关联索引");
        assertTrue(sql.contains("CREATE INDEX IF NOT EXISTS idx_lp_dyeing_color_contract_line_id")
                        || sql.contains("idx_lp_dyeing_color_contract_line_id"),
                "需包含染色单颜色明细合同行关联索引");
    }

    @Test
    @DisplayName("验证销售合同菜单 10110 下注册按钮权限 10114（下达生产）")
    void testMenuAndButtonPermission() throws IOException {
        String sql = loadSqlContent();

        // 按钮 10114 注册在 10110 下
        assertTrue(sql.contains("10114"), "必须注册按钮权限 10114");
        assertTrue(sql.contains("10110"), "按钮 10114 的 parent_id 必须为销售合同菜单 10110");
        assertTrue(sql.contains("下达生产"), "按钮名称应为「下达生产」");
        assertTrue(sql.contains("lunaerp:production:release-production:release"), "权限码必须包含 :release");
        assertTrue(sql.contains("lunaerp:production:release-production:query"), "SQL 中必须注明与包含 :query 权限说明");

        // 授权管理员与超管
        assertTrue(sql.contains("system_role_menu"), "必须将按钮授权写入 system_role_menu");
        assertTrue(sql.contains("system_role_menu_seq"), "system_role_menu 插入必须显式使用 nextval('system_role_menu_seq') 生成主键");
        assertTrue(sql.contains("super_admin"), "必须为 super_admin 授权");
    }

}
