package com.luna.lunasrv.module.lunaerp.costcard;

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
 * 针对售前链成本卡与智能报价 SQL 基建脚本 lunaerp-business-cost-card-quotation.sql 的单元测试与规范校验
 * 覆盖 issue: .scratch/cost-card-quotation/issues/02-sql-bootstrap.md
 */
public class LunaerpCostCardQuotationSqlBootstrapTest {

    private String loadSqlContent() throws IOException {
        Path sqlPath = Path.of("sql/postgresql/lunaerp-business-cost-card-quotation.sql");
        if (!Files.exists(sqlPath)) {
            sqlPath = Path.of("../sql/postgresql/lunaerp-business-cost-card-quotation.sql");
        }
        if (!Files.exists(sqlPath)) {
            File root = new File("").getAbsoluteFile();
            while (root != null && !new File(root, "sql/postgresql/lunaerp-business-cost-card-quotation.sql").exists()) {
                root = root.getParentFile();
            }
            if (root != null) {
                sqlPath = new File(root, "sql/postgresql/lunaerp-business-cost-card-quotation.sql").toPath();
            }
        }
        assertTrue(Files.exists(sqlPath), "SQL 文件必须存在于 sql/postgresql/lunaerp-business-cost-card-quotation.sql");
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
    @DisplayName("验证 BOM 行、客户档案与销售合同前置加列及注释")
    void testPrefactorColumns() throws IOException {
        String sql = loadSqlContent();

        // BOM 行 5 列
        assertTrue(sql.contains("ALTER TABLE lunaerp_production_yarn_recipe_bom_item ADD COLUMN IF NOT EXISTS waste_rate"),
                "BOM 行必须添加 waste_rate 列");
        assertTrue(sql.contains("ALTER TABLE lunaerp_production_yarn_recipe_bom_item ADD COLUMN IF NOT EXISTS direction"),
                "BOM 行必须添加 direction 列");
        assertTrue(sql.contains("ALTER TABLE lunaerp_production_yarn_recipe_bom_item ADD COLUMN IF NOT EXISTS density"),
                "BOM 行必须添加 density 列");
        assertTrue(sql.contains("ALTER TABLE lunaerp_production_yarn_recipe_bom_item ADD COLUMN IF NOT EXISTS crimp_rate"),
                "BOM 行必须添加 crimp_rate 列");
        assertTrue(sql.contains("ALTER TABLE lunaerp_production_yarn_recipe_bom_item ADD COLUMN IF NOT EXISTS consumption_kg_per_meter"),
                "BOM 行必须添加 consumption_kg_per_meter 列");

        // 客户档案默认毛利率
        assertTrue(sql.contains("ALTER TABLE lunaerp_baseinfo_customer ADD COLUMN IF NOT EXISTS default_margin_rate"),
                "客户档案必须添加 default_margin_rate 列");

        // 销售合同来源两列
        assertTrue(sql.contains("ALTER TABLE lunaerp_business_sales_contract ADD COLUMN IF NOT EXISTS source_quotation_id"),
                "销售合同必须添加 source_quotation_id 列");
        assertTrue(sql.contains("ALTER TABLE lunaerp_business_sales_contract ADD COLUMN IF NOT EXISTS source_quotation_no"),
                "销售合同必须添加 source_quotation_no 列");
    }

    @Test
    @DisplayName("验证 6 张新表 DDL、主键约束与 TenantBaseDO 标准列")
    void testTablesAndColumns() throws IOException {
        String sql = loadSqlContent();

        String[] tables = {
            "lunaerp_production_yarn_price",
            "lunaerp_business_cost_card",
            "lunaerp_business_cost_card_yarn",
            "lunaerp_business_cost_card_fee",
            "lunaerp_business_quotation",
            "lunaerp_business_quotation_item"
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

        // 纱价核心列
        assertTrue(sql.contains("yarn_spec_id"), "纱价表必须包含 yarn_spec_id");
        assertTrue(sql.contains("price_type"), "纱价表必须包含 price_type");
        assertTrue(sql.contains("effective_date"), "纱价表必须包含 effective_date");

        // 成本卡核心列
        assertTrue(sql.contains("card_no"), "成本卡主表必须包含 card_no");
        assertTrue(sql.contains("basis"), "成本卡主表必须包含 basis");
        assertTrue(sql.contains("yarn_cost_per_kg"), "成本卡主表必须包含 yarn_cost_per_kg");
        assertTrue(sql.contains("unit_cost_per_kg"), "成本卡主表必须包含 unit_cost_per_kg");
        assertTrue(sql.contains("unit_cost_per_meter"), "成本卡主表必须包含 unit_cost_per_meter");
        assertTrue(sql.contains("missing_price_count"), "成本卡主表必须包含 missing_price_count");

        // 成本卡纱线行与费用行
        assertTrue(sql.contains("contribution_per_kg"), "纱线行/费用行必须包含 contribution_per_kg");
        assertTrue(sql.contains("contribution_per_meter"), "纱线行/费用行必须包含 contribution_per_meter");
        assertTrue(sql.contains("fee_type"), "费用行必须包含 fee_type");
        assertTrue(sql.contains("fee_unit"), "费用行必须包含 fee_unit");

        // 报价单主表与明细
        assertTrue(sql.contains("quotation_no"), "报价单主表必须包含 quotation_no");
        assertTrue(sql.contains("version_no"), "报价单主表必须包含 version_no");
        assertTrue(sql.contains("salesperson_user_id"), "报价单主表必须包含 salesperson_user_id");
        assertTrue(sql.contains("dept_id"), "报价单主表必须包含 dept_id");
        assertTrue(sql.contains("total_cost_amount"), "报价单主表必须包含 total_cost_amount");
        assertTrue(sql.contains("avg_margin_rate"), "报价单主表必须包含 avg_margin_rate");
        assertTrue(sql.contains("cost_snapshot"), "报价明细必须包含 cost_snapshot");
        assertTrue(sql.contains("cost_unit_price"), "报价明细必须包含 cost_unit_price");
        assertTrue(sql.contains("target_margin_rate"), "报价明细必须包含 target_margin_rate");
        assertTrue(sql.contains("margin_rate"), "报价明细必须包含 margin_rate");
        assertTrue(sql.contains("low_margin_warning"), "报价明细必须包含 low_margin_warning");
    }

    @Test
    @DisplayName("验证 6 个主键序列")
    void testSequences() throws IOException {
        String sql = loadSqlContent();

        String[] sequences = {
            "lunaerp_production_yarn_price_seq",
            "lunaerp_business_cost_card_seq",
            "lunaerp_business_cost_card_yarn_seq",
            "lunaerp_business_cost_card_fee_seq",
            "lunaerp_business_quotation_seq",
            "lunaerp_business_quotation_item_seq"
        };

        for (String seq : sequences) {
            assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS " + seq),
                    "必须包含序列 CREATE SEQUENCE IF NOT EXISTS " + seq);
        }
    }

    @Test
    @DisplayName("验证部分唯一索引与全量唯一索引")
    void testIndexes() throws IOException {
        String sql = loadSqlContent();

        // 成本卡号租户内全量唯一（含软删行）
        assertTrue(sql.contains("uk_lbcc_card_no"), "必须包含成本卡号唯一索引 uk_lbcc_card_no");
        assertTrue(sql.contains("ON lunaerp_business_cost_card (tenant_id, card_no)"),
                "uk_lbcc_card_no 必须索引 (tenant_id, card_no)");

        // 成本卡同产品一生效卡部分唯一索引
        assertTrue(sql.contains("uk_lbcc_product_effective"), "必须包含同产品一生效卡部分唯一索引 uk_lbcc_product_effective");
        assertTrue(sql.contains("WHERE status = 'EFFECTIVE' AND deleted = 0"),
                "uk_lbcc_product_effective 必须带条件 WHERE status = 'EFFECTIVE' AND deleted = 0");

        // 报价单号+版本号全量唯一
        assertTrue(sql.contains("uk_lbq_no_version"), "必须包含报价单号+版本号全量唯一索引 uk_lbq_no_version");

        // 报价单同号至多一草稿部分唯一索引
        assertTrue(sql.contains("uk_lbq_no_draft"), "必须包含同号一草稿部分唯一索引 uk_lbq_no_draft");
        assertTrue(sql.contains("WHERE status = 'DRAFT' AND deleted = 0"),
                "uk_lbq_no_draft 必须带条件 WHERE status = 'DRAFT' AND deleted = 0");

        // 纱价部分唯一索引
        assertTrue(sql.contains("uk_lpyp_spec_type_date"), "必须包含纱价部分唯一索引 uk_lpyp_spec_type_date");
        assertTrue(sql.contains("WHERE deleted = 0"), "纱价唯一索引必须带 WHERE deleted = 0");

        // 取价辅助查询索引
        assertTrue(sql.contains("idx_lpyp_lookup"), "必须包含取价查询索引 idx_lpyp_lookup");
        assertTrue(sql.contains("ON lunaerp_production_yarn_price (yarn_spec_id, price_type, effective_date DESC)"),
                "取价索引必须按 (yarn_spec_id, price_type, effective_date DESC)");

        // 数据范围部分索引（ADR-0046）
        assertTrue(sql.contains("idx_lbq_dept_id"), "必须包含部门数据范围索引 idx_lbq_dept_id");
        assertTrue(sql.contains("idx_lbq_salesperson"), "必须包含业务员数据范围索引 idx_lbq_salesperson");
    }

    @Test
    @DisplayName("验证成本卡与报价单菜单 10132-10140 与纱价按钮 10245-10248")
    void testMenus() throws IOException {
        String sql = loadSqlContent();

        // 成本卡 10132
        assertTrue(sql.contains("10132"), "必须包含 10132 成本卡菜单");
        assertTrue(sql.contains("lunaerp:business:cost-card:query"), "必须包含 cost-card:query 权限");
        assertTrue(sql.contains("lunaerp/business/cost-card/index"), "必须包含成本卡组件路径");
        assertTrue(sql.contains("LunaerpBusinessCostCard"), "必须包含组件名 LunaerpBusinessCostCard");

        // 按钮 10133-10135
        assertTrue(sql.contains("10133") && sql.contains("lunaerp:business:cost-card:create"), "必须包含成本卡创建按钮");
        assertTrue(sql.contains("10134") && sql.contains("lunaerp:business:cost-card:update"), "必须包含成本卡修改按钮");
        assertTrue(sql.contains("10135") && sql.contains("lunaerp:business:cost-card:delete"), "必须包含成本卡删除按钮");

        // 报价单 10136
        assertTrue(sql.contains("10136"), "必须包含 10136 报价单菜单");
        assertTrue(sql.contains("lunaerp:business:quotation:query"), "必须包含 quotation:query 权限");
        assertTrue(sql.contains("lunaerp/business/quotation/index"), "必须包含报价单组件路径");
        assertTrue(sql.contains("LunaerpBusinessQuotation"), "必须包含组件名 LunaerpBusinessQuotation");

        // 按钮 10137-10140
        assertTrue(sql.contains("10137") && sql.contains("lunaerp:business:quotation:create"), "必须包含报价单创建按钮");
        assertTrue(sql.contains("10138") && sql.contains("lunaerp:business:quotation:update"), "必须包含报价单修改按钮");
        assertTrue(sql.contains("10139") && sql.contains("lunaerp:business:quotation:delete"), "必须包含报价单删除按钮");
        assertTrue(sql.contains("10140") && sql.contains("lunaerp:business:quotation:print"), "必须包含报价单打印按钮");

        // 纱价按钮 10245-10248（挂在 10240 纱线档案菜单下）
        assertTrue(sql.contains("10245") && sql.contains("lunaerp:production:yarn-price:query"), "必须包含纱价查询按钮");
        assertTrue(sql.contains("10246") && sql.contains("lunaerp:production:yarn-price:create"), "必须包含纱价创建按钮");
        assertTrue(sql.contains("10247") && sql.contains("lunaerp:production:yarn-price:update"), "必须包含纱价修改按钮");
        assertTrue(sql.contains("10248") && sql.contains("lunaerp:production:yarn-price:delete"), "必须包含纱价删除按钮");
    }

    @Test
    @DisplayName("验证 7 个字典类型 2262-2268 与字典数据 3770-3791")
    void testDicts() throws IOException {
        String sql = loadSqlContent();

        // 字典类型
        assertTrue(sql.contains("2262") && sql.contains("lunaerp_cost_card_status"), "必须包含 2262 lunaerp_cost_card_status");
        assertTrue(sql.contains("2263") && sql.contains("lunaerp_cost_card_basis"), "必须包含 2263 lunaerp_cost_card_basis");
        assertTrue(sql.contains("2264") && sql.contains("lunaerp_cost_card_fee_type"), "必须包含 2264 lunaerp_cost_card_fee_type");
        assertTrue(sql.contains("2265") && sql.contains("lunaerp_cost_fee_unit"), "必须包含 2265 lunaerp_cost_fee_unit");
        assertTrue(sql.contains("2266") && sql.contains("lunaerp_yarn_price_type"), "必须包含 2266 lunaerp_yarn_price_type");
        assertTrue(sql.contains("2267") && sql.contains("lunaerp_quotation_status"), "必须包含 2267 lunaerp_quotation_status");
        assertTrue(sql.contains("2268") && sql.contains("lunaerp_quotation_cost_source"), "必须包含 2268 lunaerp_quotation_cost_source");

        // 字典数据值
        assertTrue(sql.contains("3770") && sql.contains("DRAFT"), "必须包含 3770 DRAFT");
        assertTrue(sql.contains("3771") && sql.contains("EFFECTIVE"), "必须包含 3771 EFFECTIVE");
        assertTrue(sql.contains("3772") && sql.contains("EXPIRED"), "必须包含 3772 EXPIRED");

        assertTrue(sql.contains("3773") && sql.contains("KG"), "必须包含 3773 KG");
        assertTrue(sql.contains("3774") && sql.contains("METER"), "必须包含 3774 METER");

        assertTrue(sql.contains("3775") && sql.contains("WEAVING"), "必须包含 3775 WEAVING");
        assertTrue(sql.contains("3776") && sql.contains("DYEING"), "必须包含 3776 DYEING");
        assertTrue(sql.contains("3777") && sql.contains("SETTING"), "必须包含 3777 SETTING");
        assertTrue(sql.contains("3778") && sql.contains("INSPECTION"), "必须包含 3778 INSPECTION");
        assertTrue(sql.contains("3779") && sql.contains("PACKAGING"), "必须包含 3779 PACKAGING");
        assertTrue(sql.contains("3780") && sql.contains("FREIGHT"), "必须包含 3780 FREIGHT");

        assertTrue(sql.contains("3781") && sql.contains("PER_KG"), "必须包含 3781 PER_KG");
        assertTrue(sql.contains("3782") && sql.contains("PER_METER"), "必须包含 3782 PER_METER");

        assertTrue(sql.contains("3783") && sql.contains("PURCHASE"), "必须包含 3783 PURCHASE");
        assertTrue(sql.contains("3784") && sql.contains("MARKET"), "必须包含 3784 MARKET");

        assertTrue(sql.contains("3785") && sql.contains("DRAFT"), "必须包含 3785 DRAFT");
        assertTrue(sql.contains("3786") && sql.contains("CONFIRMED"), "必须包含 3786 CONFIRMED");
        assertTrue(sql.contains("3787") && sql.contains("SUPERSEDED"), "必须包含 3787 SUPERSEDED");
        assertTrue(sql.contains("3788") && sql.contains("CANCELLED"), "必须包含 3788 CANCELLED");
        assertTrue(sql.contains("3789") && sql.contains("EXPIRED"), "必须包含 3789 EXPIRED");
        assertTrue(sql.contains("仅展示派生态，禁止写入"), "EXPIRED 标签备注必须注明「仅展示派生态，禁止写入」");

        assertTrue(sql.contains("3790") && sql.contains("CARD"), "必须包含 3790 CARD");
        assertTrue(sql.contains("3791") && sql.contains("MANUAL"), "必须包含 3791 MANUAL");
    }

    @Test
    @DisplayName("验证 infra_config 参数配置种子")
    void testInfraConfig() throws IOException {
        String sql = loadSqlContent();
        assertTrue(sql.contains("quotation.min-margin-rate"), "必须包含 quotation.min-margin-rate 参数配置");
        assertTrue(sql.contains("102"), "必须包含参数配置 id 102");
    }
}
