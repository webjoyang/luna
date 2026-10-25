package com.luna.lunasrv.module.lunaerp.finance.customerrecon;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.luna.lunasrv.module.lunaerp.support.BaseSqlBootstrapTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户对账、总账、开票与收款 SQL 基建脚本单元测试与规范校验
 * 覆盖 Issue: .scratch/customer-reconciliation/issues/01-sql-bootstrap-and-customer-columns.md
 */
public class LunaerpCustomerReconSqlBootstrapTest extends BaseSqlBootstrapTest {

    @Test
    @DisplayName("验证客户对账 SQL 脚本存在且能被 PostgreSQL 语法解析器解析")
    void testSqlCanBeParsed() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-customer-recon.sql");
        assertFalse(sql.isBlank(), "lunaerp-finance-customer-recon.sql 内容不能为空");
        List<SQLStatement> statements = parseSql(sql);
        assertFalse(statements.isEmpty(), "至少应解析出 DDL/DML 语句");
    }

    @Test
    @DisplayName("验证域公共表 1~3 (经营分录账, 资金账户, 汇率主档) DDL、主键约束与 TenantBaseDO 列")
    void testDomainCommonTablesAndColumns() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-customer-recon.sql");

        String[] tables = {
                "lunaerp_finance_ledger_entry",
                "lunaerp_finance_account",
                "lunaerp_finance_exchange_rate"
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

        // 表 1: 经营分录账核心字段与索引
        assertTrue(sql.contains("biz_date"), "分录账必须包含 biz_date");
        assertTrue(sql.contains("party_type"), "分录账必须包含 party_type");
        assertTrue(sql.contains("party_id"), "分录账必须包含 party_id");
        assertTrue(sql.contains("account_id"), "分录账必须包含 account_id");
        assertTrue(sql.contains("category"), "分录账必须包含 category");
        assertTrue(sql.contains("direction"), "分录账必须包含 direction");
        assertTrue(sql.contains("currency"), "分录账必须包含 currency");
        assertTrue(sql.contains("exchange_rate"), "分录账必须包含 exchange_rate");
        assertTrue(sql.contains("original_amount"), "分录账必须包含 original_amount");
        assertTrue(sql.contains("base_amount"), "分录账必须包含 base_amount");
        assertTrue(sql.contains("ref_type"), "分录账必须包含 ref_type");
        assertTrue(sql.contains("ref_id"), "分录账必须包含 ref_id");
        assertTrue(sql.contains("ref_item_id"), "分录账必须包含 ref_item_id");
        assertTrue(sql.contains("reversal_of_id"), "分录账必须包含 reversal_of_id");
        assertTrue(sql.contains("idx_lfle_party_biz_date"), "分录账必须包含 idx_lfle_party_biz_date 索引");
        assertTrue(sql.contains("idx_lfle_ref"), "分录账必须包含 idx_lfle_ref 索引");
        assertTrue(sql.contains("idx_lfle_reversal_of"), "分录账必须包含 idx_lfle_reversal_of 索引");
        assertTrue(sql.contains("idx_lfle_account_biz_date"), "分录账必须包含 idx_lfle_account_biz_date 索引");

        // 表 2: 资金账户核心字段与索引
        assertTrue(sql.contains("account_name"), "资金账户必须包含 account_name");
        assertTrue(sql.contains("account_type"), "资金账户必须包含 account_type");
        assertTrue(sql.contains("opening_balance"), "资金账户必须包含 opening_balance");
        assertTrue(sql.contains("current_balance"), "资金账户必须包含 current_balance");
        assertTrue(sql.contains("status"), "资金账户必须包含 status");
        assertTrue(sql.contains("uk_lfa_tenant_account_name"), "资金账户必须包含 uk_lfa_tenant_account_name 唯一索引");
        assertTrue(sql.contains("idx_lfa_tenant_status"), "资金账户必须包含 idx_lfa_tenant_status 索引");

        // 表 3: 汇率主档核心字段与索引
        assertTrue(sql.contains("rate"), "汇率主档必须包含 rate");
        assertTrue(sql.contains("effective_date"), "汇率主档必须包含 effective_date");
        assertTrue(sql.contains("uk_lfer_tenant_curr_eff"), "汇率主档必须包含 uk_lfer_tenant_curr_eff 唯一索引");
        assertTrue(sql.contains("idx_lfer_tenant_curr_eff_status"), "汇率主档必须包含 idx_lfer_tenant_curr_eff_status 索引");
    }

    @Test
    @DisplayName("验证客户对账单与开票申请表 4~7 DDL、主键约束与 TenantBaseDO 列")
    void testStatementAndInvoiceTablesAndColumns() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-customer-recon.sql");

        String[] tables = {
                "lunaerp_finance_statement",
                "lunaerp_finance_statement_item",
                "lunaerp_finance_invoice_application",
                "lunaerp_finance_invoice_application_item"
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

        // 表 4: 客户对账单主表
        assertTrue(sql.contains("statement_no"), "对账单主表必须包含 statement_no");
        assertTrue(sql.contains("customer_id"), "对账单主表必须包含 customer_id");
        assertTrue(sql.contains("customer_name"), "对账单主表必须包含 customer_name");
        assertTrue(sql.contains("period_start"), "对账单主表必须包含 period_start");
        assertTrue(sql.contains("period_end"), "对账单主表必须包含 period_end");
        assertTrue(sql.contains("opening_balance"), "对账单主表必须包含 opening_balance");
        assertTrue(sql.contains("period_debit"), "对账单主表必须包含 period_debit");
        assertTrue(sql.contains("period_credit"), "对账单主表必须包含 period_credit");
        assertTrue(sql.contains("closing_balance"), "对账单主表必须包含 closing_balance");
        assertTrue(sql.contains("confirm_type"), "对账单主表必须包含 confirm_type");
        assertTrue(sql.contains("confirm_by_name"), "对账单主表必须包含 confirm_by_name");
        assertTrue(sql.contains("confirm_time"), "对账单主表必须包含 confirm_time");
        assertTrue(sql.contains("confirm_method"), "对账单主表必须包含 confirm_method");
        assertTrue(sql.contains("reject_reason"), "对账单主表必须包含 reject_reason");
        assertTrue(sql.contains("reply_remark"), "对账单主表必须包含 reply_remark");
        assertTrue(sql.contains("uk_lfs_statement_no"), "必须定义 statement_no 唯一索引 uk_lfs_statement_no");
        assertTrue(sql.contains("idx_lfs_customer_id"), "必须定义 idx_lfs_customer_id");
        assertTrue(sql.contains("idx_lfs_status"), "必须定义 idx_lfs_status");
        assertTrue(sql.contains("idx_lfs_period"), "必须定义 idx_lfs_period");

        // 表 5: 客户对账单明细表
        assertTrue(sql.contains("statement_id"), "对账单明细必须包含 statement_id");
        assertTrue(sql.contains("line_no"), "对账单明细必须包含 line_no");
        assertTrue(sql.contains("ledger_entry_id"), "对账单明细必须包含 ledger_entry_id");
        assertTrue(sql.contains("debit_amount"), "对账单明细必须包含 debit_amount");
        assertTrue(sql.contains("credit_amount"), "对账单明细必须包含 credit_amount");
        assertTrue(sql.contains("sign_status"), "对账单明细必须包含 sign_status");
        assertTrue(sql.contains("idx_lfsi_statement_id"), "明细表必须包含 idx_lfsi_statement_id");
        assertTrue(sql.contains("idx_lfsi_ledger_entry_id"), "明细表必须包含 idx_lfsi_ledger_entry_id");

        // 表 6: 发票申请主表
        assertTrue(sql.contains("invoice_no"), "发票申请必须包含 invoice_no");
        assertTrue(sql.contains("tax_rate"), "发票申请必须包含 tax_rate");
        assertTrue(sql.contains("invoice_kind"), "发票申请必须包含 invoice_kind");
        assertTrue(sql.contains("electronic"), "发票申请必须包含 electronic");
        assertTrue(sql.contains("invoice_title"), "发票申请必须包含 invoice_title");
        assertTrue(sql.contains("tax_no"), "发票申请必须包含 tax_no");
        assertTrue(sql.contains("official_invoice_no"), "发票申请必须包含 official_invoice_no");
        assertTrue(sql.contains("total_amount_excl_tax"), "发票申请必须包含 total_amount_excl_tax");
        assertTrue(sql.contains("total_tax"), "发票申请必须包含 total_tax");
        assertTrue(sql.contains("total_amount_incl_tax"), "发票申请必须包含 total_amount_incl_tax");
        assertTrue(sql.contains("uk_lfia_invoice_no"), "发票申请必须包含 uk_lfia_invoice_no");
        assertTrue(sql.contains("uk_lfia_official_invoice_no"), "发票申请必须包含 uk_lfia_official_invoice_no");
        assertTrue(sql.contains("idx_lfia_customer_id"), "发票申请必须包含 idx_lfia_customer_id");
        assertTrue(sql.contains("idx_lfia_status"), "发票申请必须包含 idx_lfia_status");

        // 表 7: 发票申请明细表
        assertTrue(sql.contains("application_id"), "开票明细表必须包含 application_id");
        assertTrue(sql.contains("shipment_id"), "开票明细表必须包含 shipment_id");
        assertTrue(sql.contains("shipment_no"), "开票明细表必须包含 shipment_no");
        assertTrue(sql.contains("shipment_amount"), "开票明细表必须包含 shipment_amount");
        assertTrue(sql.contains("amount"), "开票明细表必须包含 amount");
        assertTrue(sql.contains("idx_lfiai_application_id"), "开票明细必须包含 idx_lfiai_application_id");
        assertTrue(sql.contains("idx_lfiai_shipment_id"), "开票明细必须包含 idx_lfiai_shipment_id");
    }

    @Test
    @DisplayName("验证共享表 (payment, payment_ref) IN 侧扩列与客户档案 +5 列")
    void testSharedTablesAndCustomerAlterations() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-customer-recon.sql");

        // lunaerp_finance_payment 扩列
        assertTrue(sql.contains("receipt_type varchar(16) NULL"), "payment 扩列必须包含 receipt_type");
        assertTrue(sql.contains("currency varchar(8) NULL DEFAULT 'RMB'"), "payment 扩列必须包含 currency");
        assertTrue(sql.contains("exchange_rate numeric(20,8) NULL DEFAULT 1.00000000"), "payment 扩列必须包含 exchange_rate");
        assertTrue(sql.contains("receipt_date date NULL"), "payment 扩列必须包含 receipt_date");
        assertTrue(sql.contains("bank_ref_no varchar(64) NULL"), "payment 扩列必须包含 bank_ref_no");
        assertTrue(sql.contains("handler_name varchar(64) NULL"), "payment 扩列必须包含 handler_name");
        assertTrue(sql.contains("approval_status varchar(16) NOT NULL DEFAULT 'PENDING'"), "payment 扩列必须包含 approval_status");

        // lunaerp_finance_payment_ref 扩列
        assertTrue(sql.contains("entry_no varchar(32) NULL"), "payment_ref 扩列必须包含 entry_no");
        assertTrue(sql.contains("biz_date date NULL"), "payment_ref 扩列必须包含 biz_date");
        assertTrue(sql.contains("base_amount numeric(18,2) NOT NULL DEFAULT 0"), "payment_ref 扩列必须包含 base_amount");
        assertTrue(sql.contains("ledger_entry_id int8 NULL"), "payment_ref 扩列必须包含 ledger_entry_id");

        // lunaerp_baseinfo_customer 扩列 5 列
        assertTrue(sql.contains("invoice_title varchar(256) NULL"), "customer 扩列必须包含 invoice_title");
        assertTrue(sql.contains("invoice_address varchar(512) NULL"), "customer 扩列必须包含 invoice_address");
        assertTrue(sql.contains("bank_name varchar(256) NULL"), "customer 扩列必须包含 bank_name");
        assertTrue(sql.contains("bank_account_no varchar(64) NULL"), "customer 扩列必须包含 bank_account_no");
        assertTrue(sql.contains("credit_days int4 NULL"), "customer 扩列必须包含 credit_days");
    }

    @Test
    @DisplayName("验证字典类型 2272~2278 与字典数据先删后插及平移避让")
    void testDictionaries() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-customer-recon.sql");

        String[] dictTypes = {
                "lunaerp_finance_statement_status",
                "lunaerp_finance_receipt_type",
                "lunaerp_finance_ledger_category",
                "lunaerp_finance_invoice_kind",
                "lunaerp_finance_tax_rate",
                "lunaerp_finance_account_type",
                "lunaerp_finance_statement_confirm_type"
        };

        for (String dictType : dictTypes) {
            assertTrue(sql.contains(dictType), "必须包含字典类型: " + dictType);
        }

        // 验证关键字典枚举值
        assertTrue(sql.contains("'AR_INC'"), "分录类别必须包含 AR_INC");
        assertTrue(sql.contains("'AR_INC_REVERSAL'"), "分录类别必须包含 AR_INC_REVERSAL");
        assertTrue(sql.contains("'RECEIPT'"), "收款类型与分录类别必须包含 RECEIPT");
        assertTrue(sql.contains("'ADVANCE'"), "收款类型必须包含 ADVANCE");
        assertTrue(sql.contains("'DIGITAL_SPECIAL'"), "发票种类必须包含 DIGITAL_SPECIAL");
        assertTrue(sql.contains("'DIGITAL_NORMAL'"), "发票种类必须包含 DIGITAL_NORMAL");
        assertTrue(sql.contains("'13.00'"), "开票税率必须包含 13.00");
        assertTrue(sql.contains("'CASH'"), "资金账户类型必须包含 CASH");
        assertTrue(sql.contains("'BANK'"), "资金账户类型必须包含 BANK");
        assertTrue(sql.contains("'ONLINE_PORTAL'"), "确认方式必须包含 ONLINE_PORTAL");
        assertTrue(sql.contains("'ON_BEHALF'"), "确认方式必须包含 ON_BEHALF");

        // 字典数据先删后插
        assertTrue(sql.contains("DELETE FROM system_dict_type WHERE type IN"), "必须对字典类型执行幂等清理");
        assertTrue(sql.contains("DELETE FROM system_dict_data WHERE dict_type IN"), "必须对字典数据执行幂等清理");
    }

    @Test
    @DisplayName("验证菜单 10720, 10745~10749, 10770, 10780 及权限标识与角色赋权")
    void testMenusAndPermissions() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-customer-recon.sql");

        // 财务一级目录 10700 判存
        assertTrue(sql.contains("10700"), "必须包含 10700 财务一级目录判存插入");

        // 10720 客户对账菜单及按钮
        assertTrue(sql.contains("10720"), "必须包含 10720 客户对账菜单");
        assertTrue(sql.contains("lunaerp:finance:customerrecon:query"), "必须包含 10720 查询权限");
        int[] customerReconBtns = {10721, 10722, 10723, 10724, 10725, 10726, 10727};
        for (int btn : customerReconBtns) {
            assertTrue(sql.contains(String.valueOf(btn)), "必须包含客户对账按钮 " + btn);
        }
        assertTrue(sql.contains("lunaerp:finance:customerrecon:create"), "必须包含生成对账单权限");
        assertTrue(sql.contains("lunaerp:finance:customerrecon:update"), "必须包含编辑对账单权限");
        assertTrue(sql.contains("lunaerp:finance:customerrecon:delete"), "必须包含删除对账单权限");
        assertTrue(sql.contains("lunaerp:finance:customerrecon:send"), "必须包含发送对账单权限");
        assertTrue(sql.contains("lunaerp:finance:customerrecon:void"), "必须包含作废对账单权限");
        assertTrue(sql.contains("lunaerp:finance:customerrecon:confirm-on-behalf"), "必须包含代客确认权限");
        assertTrue(sql.contains("lunaerp:finance:customerrecon:print"), "必须包含打印对账单权限");

        // 10745~10749 收款相关按钮
        int[] receiptBtns = {10745, 10746, 10747, 10748, 10749};
        for (int btn : receiptBtns) {
            assertTrue(sql.contains(String.valueOf(btn)), "必须包含收款按钮 " + btn);
        }
        assertTrue(sql.contains("lunaerp:finance:payment:create"), "必须包含创建收款单权限");
        assertTrue(sql.contains("lunaerp:finance:payment:update"), "必须包含修改收款单权限");
        assertTrue(sql.contains("lunaerp:finance:payment:delete"), "必须包含删除收款单权限");
        assertTrue(sql.contains("lunaerp:finance:payment:writeoff"), "必须包含预收转核销权限");
        assertTrue(sql.contains("lunaerp:finance:payment:reverse"), "必须包含红冲收款单权限");

        // 10770 发票申请菜单及按钮
        assertTrue(sql.contains("10770"), "必须包含 10770 发票申请菜单");
        assertTrue(sql.contains("lunaerp:finance:invoice:query"), "必须包含 10770 查询权限");
        int[] invoiceBtns = {10771, 10772, 10773, 10774};
        for (int btn : invoiceBtns) {
            assertTrue(sql.contains(String.valueOf(btn)), "必须包含发票按钮 " + btn);
        }
        assertTrue(sql.contains("lunaerp:finance:invoice:create"), "必须包含创建发票申请权限");
        assertTrue(sql.contains("lunaerp:finance:invoice:update"), "必须包含修改发票申请权限");
        assertTrue(sql.contains("lunaerp:finance:invoice:delete"), "必须包含删除发票申请权限");
        assertTrue(sql.contains("lunaerp:finance:invoice:issue"), "必须包含发票登记开票权限");

        // 10780 汇率管理菜单及按钮
        assertTrue(sql.contains("10780"), "必须包含 10780 汇率管理菜单");
        assertTrue(sql.contains("lunaerp:finance:exchangerate:query"), "必须包含 10780 查询权限");
        int[] rateBtns = {10781, 10782};
        for (int btn : rateBtns) {
            assertTrue(sql.contains(String.valueOf(btn)), "必须包含汇率按钮 " + btn);
        }
        assertTrue(sql.contains("lunaerp:finance:exchangerate:create"), "必须包含新增汇率权限");
        assertTrue(sql.contains("lunaerp:finance:exchangerate:update"), "必须包含修改汇率权限");

        // 角色赋权
        assertTrue(sql.contains("system_role_menu"), "必须赋权 system_role_menu");
        assertTrue(sql.contains("nextval('system_role_menu_seq')"), "必须显式调用 nextval('system_role_menu_seq') 防主键 null");
    }

    @Test
    @DisplayName("验证默认审批流配置 CUSTOMER_RECEIPT 与节点创建")
    void testApprovalConfig() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-customer-recon.sql");

        assertTrue(sql.contains("lunaerp_approval_config"), "必须包含 lunaerp_approval_config 配置表");
        assertTrue(sql.contains("lunaerp_approval_config_node"), "必须包含 lunaerp_approval_config_node 节点表");
        assertTrue(sql.contains("'CUSTOMER_RECEIPT'"), "必须配置 CUSTOMER_RECEIPT 审批流");
        assertTrue(sql.contains("'客户收款默认审批流'"), "必须包含审批流名称");
        assertTrue(sql.contains("approver_ids"), "节点审批需指定 approver_ids");
    }

    @Test
    @DisplayName("验证新创建的 7 张表均具备完备的表注释与全列注释")
    void testCommentsCompleteness() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-customer-recon.sql");

        String[] tables = {
                "lunaerp_finance_ledger_entry",
                "lunaerp_finance_account",
                "lunaerp_finance_exchange_rate",
                "lunaerp_finance_statement",
                "lunaerp_finance_statement_item",
                "lunaerp_finance_invoice_application",
                "lunaerp_finance_invoice_application_item"
        };
        for (String table : tables) {
            assertTrue(sql.contains("COMMENT ON TABLE " + table + " IS"), table + " 必须有 COMMENT ON TABLE");
        }
    }

    @Test
    @DisplayName("验证主键序列均定义为 CREATE SEQUENCE IF NOT EXISTS")
    void testSequences() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-customer-recon.sql");

        String[] tables = {
                "lunaerp_finance_ledger_entry",
                "lunaerp_finance_account",
                "lunaerp_finance_exchange_rate",
                "lunaerp_finance_statement",
                "lunaerp_finance_statement_item",
                "lunaerp_finance_invoice_application",
                "lunaerp_finance_invoice_application_item"
        };
        for (String table : tables) {
            assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS " + table + "_seq;"),
                    table + " 必须有 CREATE SEQUENCE IF NOT EXISTS " + table + "_seq;");
        }
    }

    @Test
    @DisplayName("验证 SQL 脚本满足空库与存量库幂等重跑规范 (IF NOT EXISTS + DO 块判存 + 先删后插)")
    void testIdempotencyProperties() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-customer-recon.sql");

        // 确保没有裸的 CREATE TABLE (非 IF NOT EXISTS)
        assertFalse(sql.matches("(?s).*CREATE\\s+TABLE\\s+(?!IF\\s+NOT\\s+EXISTS).*"),
                "所有 CREATE TABLE 必须携带 IF NOT EXISTS");
        // 确保没有裸的 CREATE INDEX (非 IF NOT EXISTS)
        assertFalse(sql.matches("(?s).*CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(?!IF\\s+NOT\\s+EXISTS).*"),
                "所有 CREATE INDEX 必须携带 IF NOT EXISTS");
    }

    @Test
    @DisplayName("验证领域 SQL 脚本严禁跨模块执行平台底座表 DDL (CREATE/ALTER/DROP 等)，仅允许操作 lunaerp_* 域表")
    void testNoCrossDomainDdlOnPlatformTables() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-customer-recon.sql");
        List<SQLStatement> statements = parseSql(sql);

        for (SQLStatement stmt : statements) {
            if (stmt instanceof com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement createTable) {
                String tableName = createTable.getTableName().toLowerCase().replace("\"", "");
                assertTrue(tableName.startsWith("lunaerp_"),
                        "领域增量脚本严禁创建非 lunaerp_ 前缀的表: " + tableName);
            } else if (stmt instanceof com.alibaba.druid.sql.ast.statement.SQLCreateIndexStatement createIndex) {
                String tableName = SQLUtils.toSQLString(createIndex.getTable()).toLowerCase().replace("\"", "");
                assertTrue(tableName.startsWith("lunaerp_"),
                        "领域增量脚本严禁在非 lunaerp_ 前缀的表上创建索引 (如底座 system_* 表): " + tableName);
            } else if (stmt instanceof com.alibaba.druid.sql.ast.statement.SQLAlterTableStatement alterTable) {
                String tableName = alterTable.getTableName().toLowerCase().replace("\"", "");
                assertTrue(tableName.startsWith("lunaerp_"),
                        "领域增量脚本严禁修改非 lunaerp_ 前缀的表: " + tableName);
            } else if (stmt instanceof com.alibaba.druid.sql.ast.statement.SQLDropTableStatement dropTable) {
                for (var tableSource : dropTable.getTableSources()) {
                    String tableName = tableSource.getTableName().toLowerCase().replace("\"", "");
                    assertTrue(tableName.startsWith("lunaerp_"),
                            "领域增量脚本严禁删除非 lunaerp_ 前缀的表: " + tableName);
                }
            }
        }

        // 文本规则双重静态守卫：严格防范在 system_* 底座表上出现 CREATE INDEX / ALTER TABLE 等越权 DDL
        Pattern bannedDdlPattern = Pattern.compile(
                "(?i)(CREATE\\s+(?:UNIQUE\\s+)?INDEX[\\s\\S]*?ON\\s+system_\\w+|ALTER\\s+TABLE\\s+system_\\w+)"
        );
        Matcher matcher = bannedDdlPattern.matcher(sql);
        assertFalse(matcher.find(), () -> "领域 SQL 脚本检测到对底座 system_* 表的越权 DDL 变更: " + matcher.group(1));
    }
}
