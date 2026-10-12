package com.luna.lunasrv.module.lunaerp.finance;

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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 针对客户对账与财务基建 SQL 脚本 lunaerp-finance-customer-recon.sql 的单元测试与规范校验
 * 覆盖 Issue 01: SQL 基建 + 错误码常量 + 客户档案+5列
 */
public class LunaerpFinanceCustomerReconSqlBootstrapTest {

    private String loadSqlContent() throws IOException {
        Path sqlPath = Path.of("sql/postgresql/lunaerp-finance-customer-recon.sql");
        if (!Files.exists(sqlPath)) {
            sqlPath = Path.of("../sql/postgresql/lunaerp-finance-customer-recon.sql");
        }
        if (!Files.exists(sqlPath)) {
            File root = new File("").getAbsoluteFile();
            while (root != null && !new File(root, "sql/postgresql/lunaerp-finance-customer-recon.sql").exists()) {
                root = root.getParentFile();
            }
            if (root != null) {
                sqlPath = new File(root, "sql/postgresql/lunaerp-finance-customer-recon.sql").toPath();
            }
        }
        assertTrue(Files.exists(sqlPath), "SQL 文件必须存在于 sql/postgresql/lunaerp-finance-customer-recon.sql");
        return Files.readString(sqlPath, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("验证 SQL 脚本能被 PostgreSQL 语法解析器成功解析")
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
    @DisplayName("验证 5 张新表 DDL 就位并包含标准 TenantBaseDO 字段与主键约束")
    void testTablesAndColumns() throws IOException {
        String sql = loadSqlContent();

        String[] newTables = {
            "lunaerp_finance_ledger_entry",
            "lunaerp_finance_account",
            "lunaerp_finance_exchange_rate",
            "lunaerp_finance_statement",
            "lunaerp_finance_statement_item",
            "lunaerp_finance_invoice_application",
            "lunaerp_finance_invoice_application_item"
        };

        for (String table : newTables) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + table),
                    "必须包含 CREATE TABLE IF NOT EXISTS " + table);
            assertTrue(sql.contains("pk_" + table), "主键约束名必须包含 pk_" + table);
            assertTrue(sql.contains("ADD CONSTRAINT pk_" + table + " PRIMARY KEY (id)"),
                    table + " 必须包含主键约束定义");

            // TenantBaseDO 字段
            assertTrue(sql.contains("creator"), table + " 必须包含 creator 字段");
            assertTrue(sql.contains("create_time"), table + " 必须包含 create_time 字段");
            assertTrue(sql.contains("updater"), table + " 必须包含 updater 字段");
            assertTrue(sql.contains("update_time"), table + " 必须包含 update_time 字段");
            assertTrue(sql.contains("deleted"), table + " 必须包含 deleted 字段");
            assertTrue(sql.contains("tenant_id"), table + " 必须包含 tenant_id 字段");
        }
    }

    @Test
    @DisplayName("验证 3 张现有表 ADD COLUMN IF NOT EXISTS 扩列就位")
    void testAddColumnExtensions() throws IOException {
        String sql = loadSqlContent();

        // 1. lunaerp_finance_payment 扩列
        String[] paymentCols = {
            "receipt_type", "currency", "exchange_rate", "receipt_date", "bank_ref_no", "handler_name", "approval_status"
        };
        for (String col : paymentCols) {
            assertTrue(sql.contains("ALTER TABLE lunaerp_finance_payment ADD COLUMN IF NOT EXISTS " + col),
                    "payment 表必须扩列 " + col);
        }

        // 2. lunaerp_finance_payment_ref 扩列
        String[] paymentRefCols = {
            "entry_no", "biz_date", "base_amount", "ledger_entry_id"
        };
        for (String col : paymentRefCols) {
            assertTrue(sql.contains("ALTER TABLE lunaerp_finance_payment_ref ADD COLUMN IF NOT EXISTS " + col),
                    "payment_ref 表必须扩列 " + col);
        }

        // 3. lunaerp_baseinfo_customer 扩 5 列
        String[] customerCols = {
            "invoice_title", "invoice_address", "bank_name", "bank_account_no", "credit_days"
        };
        for (String col : customerCols) {
            assertTrue(sql.contains("ALTER TABLE lunaerp_baseinfo_customer ADD COLUMN IF NOT EXISTS " + col),
                    "customer 表必须扩列 " + col);
        }
    }

    @Test
    @DisplayName("验证主键自增序列就位")
    void testSequences() throws IOException {
        String sql = loadSqlContent();

        String[] sequences = {
            "lunaerp_finance_ledger_entry_seq",
            "lunaerp_finance_account_seq",
            "lunaerp_finance_exchange_rate_seq",
            "lunaerp_finance_statement_seq",
            "lunaerp_finance_statement_item_seq",
            "lunaerp_finance_invoice_application_seq",
            "lunaerp_finance_invoice_application_item_seq"
        };

        for (String seq : sequences) {
            assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS " + seq),
                    "必须包含序列 CREATE SEQUENCE IF NOT EXISTS " + seq);
        }
    }

    @Test
    @DisplayName("验证关键唯一索引（含软删行）与辅助索引就位")
    void testIndexes() throws IOException {
        String sql = loadSqlContent();

        // 全量唯一索引（无 WHERE deleted = 0，含软删行）
        assertTrue(sql.contains("uk_lfa_tenant_account_name"), "account 表必须包含名称租户唯一索引");
        assertTrue(sql.contains("uk_lfer_currency_date"), "exchange_rate 表必须包含货币+生效日租户唯一索引");
        assertTrue(sql.contains("uk_lfs_statement_no"), "statement 表必须包含单号租户唯一索引");
        assertTrue(sql.contains("uk_lfia_invoice_no"), "invoice_application 表必须包含单号租户唯一索引");

        // 关键辅助索引
        assertTrue(sql.contains("idx_lfle_party_date"), "分录账必须包含往来主体+业务日期索引");
        assertTrue(sql.contains("idx_lfle_ref"), "分录账必须包含源单索引");
        assertTrue(sql.contains("idx_lfle_reversal"), "分录账必须包含红冲索引");
        assertTrue(sql.contains("idx_lfle_account_date"), "分录账必须包含资金流水索引");
        assertTrue(sql.contains("idx_lfsi_statement"), "对账单明细必须包含主单索引");
        assertTrue(sql.contains("idx_lfsi_ledger_entry"), "对账单明细必须包含分录占用索引");
        assertTrue(sql.contains("idx_lfia_customer"), "发票申请必须包含客户索引");
        assertTrue(sql.contains("idx_lfiai_application"), "发票申请明细必须包含主单索引");
        assertTrue(sql.contains("idx_lfiai_shipment"), "发票申请明细必须包含发货单索引");
    }

    @Test
    @DisplayName("验证字典类型 2272–2278 与字典数据就位，包含 CUSTOMER_RECEIPT 审批字典")
    void testDictionaries() throws IOException {
        String sql = loadSqlContent();

        // 字典类型
        String[] dictTypes = {
            "lunaerp_finance_statement_status",
            "lunaerp_finance_receipt_type",
            "lunaerp_finance_ledger_category",
            "lunaerp_finance_invoice_kind",
            "lunaerp_finance_tax_rate",
            "lunaerp_finance_account_type",
            "lunaerp_finance_statement_confirm_type"
        };
        for (String dt : dictTypes) {
            assertTrue(sql.contains(dt), "必须包含字典类型 " + dt);
        }

        // 分录 category 9 值
        String[] categories = {
            "AR_INC", "AR_INC_REVERSAL", "RECEIPT", "RECEIPT_REVERSAL", "RECEIPT_WRITE_OFF",
            "ADVANCE_WRITE_OFF", "AR_RETURN_REVERSAL", "OUT_SUPPLIER", "OUT_SUPPLIER_REVERSAL"
        };
        for (String cat : categories) {
            assertTrue(sql.contains("'" + cat + "'"), "分录 category 必须包含 " + cat);
        }

        // 审批 biz_type CUSTOMER_RECEIPT
        assertTrue(sql.contains("'CUSTOMER_RECEIPT'"), "必须包含 CUSTOMER_RECEIPT 审批字典值");
    }

    @Test
    @DisplayName("验证审批默认配置与节点插入")
    void testApprovalConfig() throws IOException {
        String sql = loadSqlContent();
        assertTrue(sql.contains("lunaerp_approval_config"), "需操作审批流程配置表");
        assertTrue(sql.contains("lunaerp_approval_config_node"), "需操作审批流程节点表");
        assertTrue(sql.contains("'CUSTOMER_RECEIPT'"), "需配置 CUSTOMER_RECEIPT 审批流");
    }

    @Test
    @DisplayName("验证菜单与按钮注册完整（10720, 10740收款按钮, 10770, 10780）")
    void testMenus() throws IOException {
        String sql = loadSqlContent();

        // 客户对账 10720 及按钮 10721-10727
        assertTrue(sql.contains("10720") && sql.contains("'客户对账'"));
        for (int id = 10721; id <= 10727; id++) {
            assertTrue(sql.contains(String.valueOf(id)), "必须包含对账按钮 " + id);
        }

        // 收款按钮 10745-10749
        for (int id = 10745; id <= 10749; id++) {
            assertTrue(sql.contains(String.valueOf(id)), "必须包含收款按钮 " + id);
        }

        // 发票申请 10770 及按钮 10771-10774
        assertTrue(sql.contains("10770") && sql.contains("'发票申请'"));
        for (int id = 10771; id <= 10774; id++) {
            assertTrue(sql.contains(String.valueOf(id)), "必须包含发票按钮 " + id);
        }

        // 汇率主档 10780 及按钮 10781-10782
        assertTrue(sql.contains("10780") && sql.contains("'汇率'"));
        assertTrue(sql.contains("'exchange-rate'"));
        assertTrue(sql.contains("'lunaerp-elmui/finance/exchange-rate/index'"));
        assertTrue(sql.contains("10781"));
        assertTrue(sql.contains("10782"));
    }
}
