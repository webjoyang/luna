package com.luna.lunasrv.module.lunaerp.exportdoc;

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
 * 针对出口单证 SQL 基建脚本 lunaerp-business-export-doc.sql 的单元测试与规范校验
 * 覆盖 issue: .scratch/export-docs/issues/01-sql-bootstrap-and-error-codes.md
 */
public class LunaerpExportDocSqlBootstrapTest {

    private String loadSqlContent() throws IOException {
        Path sqlPath = Path.of("sql/postgresql/lunaerp-business-export-doc.sql");
        if (!Files.exists(sqlPath)) {
            sqlPath = Path.of("../sql/postgresql/lunaerp-business-export-doc.sql");
        }
        if (!Files.exists(sqlPath)) {
            File root = new File("").getAbsoluteFile();
            while (root != null && !new File(root, "sql/postgresql/lunaerp-business-export-doc.sql").exists()) {
                root = root.getParentFile();
            }
            if (root != null) {
                sqlPath = new File(root, "sql/postgresql/lunaerp-business-export-doc.sql").toPath();
            }
        }
        assertTrue(Files.exists(sqlPath), "SQL 文件必须存在于 sql/postgresql/lunaerp-business-export-doc.sql");
        return Files.readString(sqlPath, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("验证 SQL 脚本存在且能被 PostgreSQL 语法解析器解析")
    void testSqlCanBeParsed() throws IOException {
        String sql = loadSqlContent();
        assertFalse(sql.isBlank(), "SQL 文件内容不能为空");

        String standardSql = sql.replaceAll("(?s)DO \\$\\$.*?\\$\\$;", "-- DO BLOCK OMITTED FOR PARSER;")
                                .replaceAll("(?i)(CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+[^;]+?)\\s+WHERE\\s+[^;]+;", "$1;")
                                .replaceAll("(?i)CREATE\\s+SEQUENCE\\s+IF\\s+NOT\\s+EXISTS", "CREATE SEQUENCE");
        List<SQLStatement> statements = SQLUtils.parseStatements(standardSql, DbType.postgresql);
        assertFalse(statements.isEmpty(), "至少应成功解析出 DDL/DML 语句");
    }

    @Test
    @DisplayName("验证销售合同与客户档案前置加列及注释")
    void testPrefactorColumns() throws IOException {
        String sql = loadSqlContent();

        // 销售合同 4 列
        assertTrue(sql.contains("ALTER TABLE lunaerp_business_sales_contract ADD COLUMN IF NOT EXISTS incoterm"),
                "销售合同必须添加 incoterm 列");
        assertTrue(sql.contains("ALTER TABLE lunaerp_business_sales_contract ADD COLUMN IF NOT EXISTS port_of_loading"),
                "销售合同必须添加 port_of_loading 列");
        assertTrue(sql.contains("ALTER TABLE lunaerp_business_sales_contract ADD COLUMN IF NOT EXISTS port_of_discharge"),
                "销售合同必须添加 port_of_discharge 列");
        assertTrue(sql.contains("ALTER TABLE lunaerp_business_sales_contract ADD COLUMN IF NOT EXISTS payment_term"),
                "销售合同必须添加 payment_term 列");

        // 客户档案 2 列
        assertTrue(sql.contains("ALTER TABLE lunaerp_baseinfo_customer ADD COLUMN IF NOT EXISTS name_en"),
                "客户档案必须添加 name_en 列");
        assertTrue(sql.contains("ALTER TABLE lunaerp_baseinfo_customer ADD COLUMN IF NOT EXISTS address_en"),
                "客户档案必须添加 address_en 列");
    }

    @Test
    @DisplayName("验证 3 张新表 DDL、主键约束与 TenantBaseDO 标准列")
    void testTablesAndColumns() throws IOException {
        String sql = loadSqlContent();

        String[] tables = {
            "lunaerp_business_export_doc",
            "lunaerp_business_export_doc_item",
            "lunaerp_business_export_seller_config"
        };

        for (String table : tables) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + table),
                    "必须包含 CREATE TABLE IF NOT EXISTS " + table);
            assertTrue(sql.contains("pk_" + table), "主键约束名必须包含 pk_" + table);
            assertTrue(sql.contains("ADD CONSTRAINT pk_" + table + " PRIMARY KEY (id)"),
                    "必须包含主键约束定义 pk_" + table);

            // TenantBaseDO 字段
            assertTrue(sql.contains("creator"), table + " 必须包含 creator 字段");
            assertTrue(sql.contains("create_time"), table + " 必须包含 create_time 字段");
            assertTrue(sql.contains("updater"), table + " 必须包含 updater 字段");
            assertTrue(sql.contains("update_time"), table + " 必须包含 update_time 字段");
            assertTrue(sql.contains("deleted"), table + " 必须包含 deleted 字段");
            assertTrue(sql.contains("tenant_id"), table + " 必须包含 tenant_id 字段");
        }

        // 主表核心列与 meta JSON 列
        assertTrue(sql.contains("doc_no"), "主表必须包含 doc_no");
        assertTrue(sql.contains("pi_meta"), "主表必须包含 pi_meta");
        assertTrue(sql.contains("ci_meta"), "主表必须包含 ci_meta");
        assertTrue(sql.contains("pl_meta"), "主表必须包含 pl_meta");
        assertTrue(sql.contains("shipping_mark"), "主表必须包含 shipping_mark");

        // 明细表重量箱数体积列
        assertTrue(sql.contains("net_weight"), "明细表必须包含 net_weight");
        assertTrue(sql.contains("gross_weight"), "明细表必须包含 gross_weight");
        assertTrue(sql.contains("packages"), "明细表必须包含 packages");
        assertTrue(sql.contains("volume"), "明细表必须包含 volume");

        // 卖方配置表银行列
        assertTrue(sql.contains("bank_swift"), "卖方配置表必须包含 bank_swift");
    }

    @Test
    @DisplayName("验证 3 个主键序列")
    void testSequences() throws IOException {
        String sql = loadSqlContent();

        String[] sequences = {
            "lunaerp_business_export_doc_seq",
            "lunaerp_business_export_doc_item_seq",
            "lunaerp_business_export_seller_config_seq"
        };

        for (String seq : sequences) {
            assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS " + seq),
                    "必须包含序列 CREATE SEQUENCE IF NOT EXISTS " + seq);
        }
    }

    @Test
    @DisplayName("验证唯一索引（单证套号含软删行、卖方配置租户唯一）与辅助索引")
    void testIndexes() throws IOException {
        String sql = loadSqlContent();

        // 单证套号租户内唯一索引（含软删行，不带 WHERE deleted=0）
        assertTrue(sql.contains("uk_lbed_doc_no"), "必须包含单证套号唯一索引 uk_lbed_doc_no");
        assertTrue(sql.contains("ON lunaerp_business_export_doc (tenant_id, doc_no)"),
                "uk_lbed_doc_no 必须索引 (tenant_id, doc_no)");
        assertFalse(sql.contains("uk_lbed_doc_no\n    ON lunaerp_business_export_doc (tenant_id, doc_no) WHERE deleted = 0"),
                "单证套号唯一索引必须含软删行，不能带 WHERE deleted = 0");

        // 卖方配置租户唯一索引
        assertTrue(sql.contains("uk_lbesc_tenant"), "必须包含卖方配置租户唯一索引 uk_lbesc_tenant");
        assertTrue(sql.contains("ON lunaerp_business_export_seller_config (tenant_id)"),
                "uk_lbesc_tenant 必须索引 (tenant_id)");

        // 辅助索引
        assertTrue(sql.contains("idx_lbed_status"), "必须包含 status 索引");
        assertTrue(sql.contains("idx_lbed_customer_id"), "必须包含 customer_id 索引");
        assertTrue(sql.contains("idx_lbed_contract_id"), "必须包含 contract_id 索引");
        assertTrue(sql.contains("idx_lbed_shipment_id"), "必须包含 shipment_id 索引");
        assertTrue(sql.contains("idx_lbedi_doc_id"), "明细必须包含 doc_id 索引");
    }

    @Test
    @DisplayName("验证出口单证菜单 10126-10131 与按钮权限")
    void testMenus() throws IOException {
        String sql = loadSqlContent();

        // 菜单 10126
        assertTrue(sql.contains("10126"), "必须包含 10126 出口单证菜单");
        assertTrue(sql.contains("lunaerp:business:export-doc:query"), "必须包含 query 权限");
        assertTrue(sql.contains("lunaerp-elmui/business/export-doc/index"), "必须包含组件路径");

        // 按钮 10127-10131
        assertTrue(sql.contains("10127"), "必须包含 10127 单证创建按钮");
        assertTrue(sql.contains("lunaerp:business:export-doc:create"), "必须包含 create 权限");

        assertTrue(sql.contains("10128"), "必须包含 10128 单证修改按钮");
        assertTrue(sql.contains("lunaerp:business:export-doc:update"), "必须包含 update 权限");

        assertTrue(sql.contains("10129"), "必须包含 10129 单证删除按钮");
        assertTrue(sql.contains("lunaerp:business:export-doc:delete"), "必须包含 delete 权限");

        assertTrue(sql.contains("10130"), "必须包含 10130 单证打印按钮");
        assertTrue(sql.contains("lunaerp:business:export-doc:print"), "必须包含 print 权限");

        assertTrue(sql.contains("10131"), "必须包含 10131 卖方出口资料按钮");
        assertTrue(sql.contains("lunaerp:business:export-doc:seller-config"), "必须包含 seller-config 权限");
    }

    @Test
    @DisplayName("验证字典类型 2258-2261 与字典数据 3750-3764")
    void testDicts() throws IOException {
        String sql = loadSqlContent();

        // 字典类型
        assertTrue(sql.contains("2258"), "必须包含 2258 lunaerp_export_doc_status");
        assertTrue(sql.contains("lunaerp_export_doc_status"), "必须包含 lunaerp_export_doc_status 字典类型");

        assertTrue(sql.contains("2259"), "必须包含 2259 lunaerp_incoterm");
        assertTrue(sql.contains("lunaerp_incoterm"), "必须包含 lunaerp_incoterm 字典类型");

        assertTrue(sql.contains("2260"), "必须包含 2260 lunaerp_payment_term");
        assertTrue(sql.contains("lunaerp_payment_term"), "必须包含 lunaerp_payment_term 字典类型");

        assertTrue(sql.contains("2261"), "必须包含 2261 lunaerp_transport_mode");
        assertTrue(sql.contains("lunaerp_transport_mode"), "必须包含 lunaerp_transport_mode 字典类型");

        // 字典数据
        assertTrue(sql.contains("3750") && sql.contains("DRAFT"), "必须包含 DRAFT 草稿");
        assertTrue(sql.contains("3751") && sql.contains("FINALIZED"), "必须包含 FINALIZED 已定稿");

        assertTrue(sql.contains("3752") && sql.contains("FOB"), "必须包含 FOB");
        assertTrue(sql.contains("3753") && sql.contains("CIF"), "必须包含 CIF");
        assertTrue(sql.contains("3754") && sql.contains("CFR"), "必须包含 CFR");
        assertTrue(sql.contains("3755") && sql.contains("EXW"), "必须包含 EXW");

        assertTrue(sql.contains("3756") && sql.contains("TT"), "必须包含 TT");
        assertTrue(sql.contains("3757") && sql.contains("LC"), "必须包含 LC");
        assertTrue(sql.contains("3758") && sql.contains("DP"), "必须包含 DP");
        assertTrue(sql.contains("3759") && sql.contains("DA"), "必须包含 DA");
        assertTrue(sql.contains("3760") && sql.contains("OA"), "必须包含 OA");

        assertTrue(sql.contains("3761") && sql.contains("SEA"), "必须包含 SEA");
        assertTrue(sql.contains("3762") && sql.contains("AIR"), "必须包含 AIR");
        assertTrue(sql.contains("3763") && sql.contains("LAND"), "必须包含 LAND");
        assertTrue(sql.contains("3764") && sql.contains("EXPRESS"), "必须包含 EXPRESS");
    }
}
