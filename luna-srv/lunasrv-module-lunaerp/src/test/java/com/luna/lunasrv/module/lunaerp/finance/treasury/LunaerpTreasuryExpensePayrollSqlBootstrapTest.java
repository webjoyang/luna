package com.luna.lunasrv.module.lunaerp.finance.treasury;

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
 * 资金账户扩列、转账/其他收支、费用报销与工资初稿 SQL 基建脚本单元测试与规范校验
 * 覆盖 Issue: .scratch/treasury-expense-payroll/issues/01-sql-bootstrap.md
 */
public class LunaerpTreasuryExpensePayrollSqlBootstrapTest {

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
    @DisplayName("验证 3 个 SQL 脚本均存在且能被 PostgreSQL 语法解析器解析")
    void testSqlCanBeParsed() throws IOException {
        String[] files = {
                "lunaerp-finance-account-ext.sql",
                "lunaerp-finance-expense.sql",
                "lunaerp-finance-payroll.sql"
        };
        for (String file : files) {
            String sql = loadSqlContent(file);
            assertFalse(sql.isBlank(), file + " 内容不能为空");
            List<SQLStatement> statements = parseSql(sql);
            assertFalse(statements.isEmpty(), file + " 至少应解析出 DDL/DML 语句");
        }
    }

    @Test
    @DisplayName("验证资金账户与分录账 ALTER 扩列及转账单、其他收支单 DDL 与约束")
    void testAccountExtAlterAndTables() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-account-ext.sql");

        // 1. lunaerp_finance_account 扩 3 列
        assertTrue(sql.contains("ALTER TABLE lunaerp_finance_account"), "必须包含 ALTER TABLE lunaerp_finance_account");
        assertTrue(sql.contains("account_no"), "资金账户必须扩 account_no");
        assertTrue(sql.contains("account_nature"), "资金账户必须扩 account_nature");
        assertTrue(sql.contains("remark"), "资金账户必须扩 remark");
        assertTrue(sql.contains("@Sensitive(CONTACT)"), "account_no 列注释必须注明 @Sensitive(CONTACT)");

        // 2. lunaerp_finance_ledger_entry 扩 2 列
        assertTrue(sql.contains("ALTER TABLE lunaerp_finance_ledger_entry"), "必须包含 ALTER TABLE lunaerp_finance_ledger_entry");
        assertTrue(sql.contains("party_name"), "经营分录账必须扩 party_name");
        assertTrue(sql.contains("ref_no"), "经营分录账必须扩 ref_no");

        // 3. 表 1: lunaerp_finance_transfer
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS lunaerp_finance_transfer"), "必须建转账单表");
        assertTrue(sql.contains("pk_lunaerp_finance_transfer"), "必须包含 pk_lunaerp_finance_transfer 主键约束");
        assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_transfer_seq"), "必须包含 transfer 序列");
        assertTrue(sql.contains("uk_lft_transfer_no"), "必须包含 uk_lft_transfer_no 唯一索引");
        assertTrue(sql.contains("idx_lft_transfer_date"), "必须包含 idx_lft_transfer_date 索引");
        assertTrue(sql.contains("from_account_id"), "transfer 必须包含 from_account_id");
        assertTrue(sql.contains("from_account_name"), "transfer 必须包含 from_account_name 快照");
        assertTrue(sql.contains("to_account_id"), "transfer 必须包含 to_account_id");
        assertTrue(sql.contains("to_account_name"), "transfer 必须包含 to_account_name 快照");
        assertTrue(sql.contains("status"), "transfer 必须包含 status");

        // 4. 表 2: lunaerp_finance_other_txn
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS lunaerp_finance_other_txn"), "必须建其他收支单表");
        assertTrue(sql.contains("pk_lunaerp_finance_other_txn"), "必须包含 pk_lunaerp_finance_other_txn 主键约束");
        assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_other_txn_seq"), "必须包含 other_txn 序列");
        assertTrue(sql.contains("uk_lfot_txn_no"), "必须包含 uk_lfot_txn_no 唯一索引");
        assertTrue(sql.contains("idx_lfot_txn_date"), "必须包含 idx_lfot_txn_date 索引");
        assertTrue(sql.contains("direction"), "other_txn 必须包含 direction");
        assertTrue(sql.contains("category"), "other_txn 必须包含 category");
        assertTrue(sql.contains("original_amount"), "other_txn 必须包含 original_amount");
        assertTrue(sql.contains("exchange_rate"), "other_txn 必须包含 exchange_rate");
        assertTrue(sql.contains("base_amount"), "other_txn 必须包含 base_amount");

        // TenantBaseDO 审计列检查
        String[] tables = {"lunaerp_finance_transfer", "lunaerp_finance_other_txn"};
        for (String table : tables) {
            assertTrue(sql.contains("COMMENT ON TABLE " + table + " IS"), table + " 必须有表注释");
        }
    }

    @Test
    @DisplayName("验证费用报销主表与明细行表 DDL、JSONB 附件与索引")
    void testExpenseTables() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-expense.sql");

        // 表 3: lunaerp_finance_expense
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS lunaerp_finance_expense"), "必须建报销主表");
        assertTrue(sql.contains("pk_lunaerp_finance_expense"), "必须包含主键 pk_lunaerp_finance_expense");
        assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_expense_seq"), "必须包含 expense 序列");
        assertTrue(sql.contains("uk_lfe_expense_no"), "必须包含 uk_lfe_expense_no 唯一索引");
        assertTrue(sql.contains("idx_lfe_applicant_status"), "必须包含 idx_lfe_applicant_status 索引");
        assertTrue(sql.contains("idx_lfe_account_id"), "必须包含 idx_lfe_account_id 索引");
        assertTrue(sql.contains("applicant_user_id"), "expense 必须包含 applicant_user_id");
        assertTrue(sql.contains("account_id"), "expense 必须包含 account_id");
        assertTrue(sql.contains("attachments"), "expense 必须包含 attachments");
        assertTrue(sql.contains("jsonb"), "attachments 必须为 jsonb 类型");
        assertTrue(sql.contains("approval_status"), "expense 必须包含 approval_status");

        // 表 4: lunaerp_finance_expense_item
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS lunaerp_finance_expense_item"), "必须建报销明细行表");
        assertTrue(sql.contains("pk_lunaerp_finance_expense_item"), "必须包含主键 pk_lunaerp_finance_expense_item");
        assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_expense_item_seq"), "必须包含 item 序列");
        assertTrue(sql.contains("idx_lfei_expense_id"), "必须包含 idx_lfei_expense_id 索引");
        assertTrue(sql.contains("category"), "item 必须包含 category");
        assertTrue(sql.contains("original_amount"), "item 必须包含 original_amount");
        assertTrue(sql.contains("occur_date"), "item 必须包含 occur_date");
    }

    @Test
    @DisplayName("验证工资域 5 张表 DDL、部分唯一索引与普通索引规约")
    void testPayrollTables() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-payroll.sql");

        // 表 5: lunaerp_finance_payroll
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS lunaerp_finance_payroll"), "必须建工资主表");
        assertTrue(sql.contains("pk_lunaerp_finance_payroll"), "必须包含主键 pk_lunaerp_finance_payroll");
        assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payroll_seq"), "必须包含 payroll 序列");
        assertTrue(sql.contains("uk_lfp_payroll_no"), "必须包含 uk_lfp_payroll_no 唯一索引");
        assertTrue(sql.contains("uk_lfp_tenant_period"), "必须包含 uk_lfp_tenant_period 部分唯一索引");
        assertTrue(sql.contains("WHERE deleted = 0 AND approval_status <> 'CANCELLED'"), "部分唯一索引必须过滤 deleted=0 且非 CANCELLED");
        assertTrue(sql.contains("period"), "payroll 必须包含 period");
        assertTrue(sql.contains("earning_total"), "payroll 必须包含 earning_total");
        assertTrue(sql.contains("deduction_total"), "payroll 必须包含 deduction_total");
        assertTrue(sql.contains("net_amount"), "payroll 必须包含 net_amount");

        // 表 6: lunaerp_finance_payroll_employee
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS lunaerp_finance_payroll_employee"), "必须建员工汇总行表");
        assertTrue(sql.contains("pk_lunaerp_finance_payroll_employee"), "必须包含主键 pk_lunaerp_finance_payroll_employee");
        assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payroll_employee_seq"), "必须包含 employee 序列");
        assertTrue(sql.contains("uk_lfpe_payroll_user"), "必须包含 uk_lfpe_payroll_user 唯一索引");

        // 表 7: lunaerp_finance_payroll_line_item
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS lunaerp_finance_payroll_line_item"), "必须建款项明细表");
        assertTrue(sql.contains("pk_lunaerp_finance_payroll_line_item"), "必须包含主键 pk_lunaerp_finance_payroll_line_item");
        assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payroll_line_item_seq"), "必须包含 line_item 序列");
        assertTrue(sql.contains("idx_lfpli_payroll_emp"), "必须包含 idx_lfpli_payroll_emp 索引");
        assertTrue(sql.contains("item_kind"), "line_item 必须包含 item_kind");
        assertTrue(sql.contains("item_type"), "line_item 必须包含 item_type");

        // 表 8: lunaerp_finance_payroll_defect_ref
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS lunaerp_finance_payroll_defect_ref"), "必须建次品引用子表");
        assertTrue(sql.contains("pk_lunaerp_finance_payroll_defect_ref"), "必须包含主键 pk_lunaerp_finance_payroll_defect_ref");
        assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payroll_defect_ref_seq"), "必须包含 defect_ref 序列");
        assertTrue(sql.contains("idx_lfpdr_line_item"), "必须包含 idx_lfpdr_line_item 索引");
        assertTrue(sql.contains("idx_lfpdr_confirmation"), "必须包含 idx_lfpdr_confirmation 索引");
        // 规约：defect_ref 确认记录是普通索引（作废释放语义，不建唯一索引）
        assertFalse(sql.contains("UNIQUE INDEX uk_lfpdr_confirmation"), "次品引用记录绝不能建唯一索引");

        // 表 9: lunaerp_finance_payroll_payment
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS lunaerp_finance_payroll_payment"), "必须建工资发放批次表");
        assertTrue(sql.contains("pk_lunaerp_finance_payroll_payment"), "必须包含主键 pk_lunaerp_finance_payroll_payment");
        assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payroll_payment_seq"), "必须包含 payment 序列");
        assertTrue(sql.contains("uk_lfpp_payment_no"), "必须包含 uk_lfpp_payment_no 唯一索引");
        assertTrue(sql.contains("idx_lfpp_payroll_id"), "必须包含 idx_lfpp_payroll_id 索引");
    }

    @Test
    @DisplayName("验证字典 2282~2287 落地及 2274 经营分录类别追加 6 值")
    void testDictionaries() throws IOException {
        String accountSql = loadSqlContent("lunaerp-finance-account-ext.sql");
        String expenseSql = loadSqlContent("lunaerp-finance-expense.sql");
        String payrollSql = loadSqlContent("lunaerp-finance-payroll.sql");

        // 2274 追加 6 值
        String[] ledgerCats = {"OUT_EXPENSE", "OUT_SALARY", "TRANSFER_IN", "TRANSFER_OUT", "IN_OTHER", "OUT_OTHER"};
        for (String cat : ledgerCats) {
            assertTrue(accountSql.contains(cat), "2274 必须追加 " + cat);
        }

        // 2282 账户性质
        assertTrue(accountSql.contains("lunaerp_finance_account_nature"), "必须声明 2282 lunaerp_finance_account_nature");
        assertTrue(accountSql.contains("'PUBLIC'"), "2282 必须包含 PUBLIC");
        assertTrue(accountSql.contains("'PRIVATE'"), "2282 必须包含 PRIVATE");

        // 2283 账户状态
        assertTrue(accountSql.contains("lunaerp_finance_account_status"), "必须声明 2283 lunaerp_finance_account_status");
        assertTrue(accountSql.contains("'ENABLE'"), "2283 必须包含 ENABLE");
        assertTrue(accountSql.contains("'DISABLE'"), "2283 必须包含 DISABLE");

        // 2287 其他收支类别
        assertTrue(accountSql.contains("lunaerp_other_txn_category"), "必须声明 2287 lunaerp_other_txn_category");
        String[] otherCats = {"INTEREST_IN", "WASTE_IN", "OTHER_IN", "FEE_OUT", "COST_OUT", "OTHER_OUT"};
        for (String cat : otherCats) {
            assertTrue(accountSql.contains(cat), "2287 必须包含 " + cat);
        }

        // 2284 报销费用类型
        assertTrue(expenseSql.contains("lunaerp_expense_category"), "必须声明 2284 lunaerp_expense_category");
        String[] expCats = {"TRAVEL", "OFFICE", "TRANSPORT", "ENTERTAIN", "RENT", "UTILITY", "COMM", "OTHER"};
        for (String cat : expCats) {
            assertTrue(expenseSql.contains(cat), "2284 必须包含 " + cat);
        }

        // 2285 工资收入项
        assertTrue(payrollSql.contains("lunaerp_payroll_earning_type"), "必须声明 2285 lunaerp_payroll_earning_type");
        String[] earnings = {"BASE", "PIECE", "COMMISSION", "BONUS", "SUBSIDY", "OTHER"};
        for (String e : earnings) {
            assertTrue(payrollSql.contains(e), "2285 必须包含 " + e);
        }

        // 2286 工资扣款项
        assertTrue(payrollSql.contains("lunaerp_payroll_deduction_type"), "必须声明 2286 lunaerp_payroll_deduction_type");
        assertTrue(payrollSql.contains("'DEFECT'"), "2286 必须包含 DEFECT");
        assertTrue(payrollSql.contains("'OTHER'"), "2286 必须包含 OTHER");

        // 审批单据类型
        assertTrue(expenseSql.contains("'expense'"), "报销脚本必须确保包含审批单据类型 expense");
        assertTrue(payrollSql.contains("'payroll'"), "工资脚本必须确保包含审批单据类型 payroll");
    }

    @Test
    @DisplayName("验证菜单与按钮权限：10710 (按钮 10711-10713/10716/10717), 10715, 10750 (10751-10755), 10760 (10761-10768)")
    void testMenusAndPermissions() throws IOException {
        String accountSql = loadSqlContent("lunaerp-finance-account-ext.sql");
        String expenseSql = loadSqlContent("lunaerp-finance-expense.sql");
        String payrollSql = loadSqlContent("lunaerp-finance-payroll.sql");

        // 10710 资金账户及按钮
        assertTrue(accountSql.contains("10710"), "必须包含 10710 资金账户菜单");
        assertTrue(accountSql.contains("lunaerp:finance:account:query"), "必须包含资金账户查询权限");
        int[] accountBtns = {10711, 10712, 10713, 10716, 10717};
        for (int btn : accountBtns) {
            assertTrue(accountSql.contains(String.valueOf(btn)), "必须包含资金账户按钮 " + btn);
        }
        assertTrue(accountSql.contains("lunaerp:finance:account:create"), "必须包含创建资金账户权限");
        assertTrue(accountSql.contains("lunaerp:finance:account:update"), "必须包含更新资金账户权限");
        assertTrue(accountSql.contains("lunaerp:finance:account:delete"), "必须包含删除资金账户权限");
        assertTrue(accountSql.contains("lunaerp:finance:account:transfer"), "必须包含账户间转账权限");
        assertTrue(accountSql.contains("lunaerp:finance:account:other-txn"), "必须包含其他收支权限");

        // 10715 收支总览
        assertTrue(accountSql.contains("10715"), "必须包含 10715 收支总览菜单");
        assertTrue(accountSql.contains("lunaerp:finance:overview:query"), "必须包含收支总览查询权限");

        // 10780 汇率菜单 sort 校验与校正
        assertTrue(accountSql.contains("UPDATE system_menu SET sort = 9 WHERE id = 10780"), "必须校正 10780 sort 顺序为 9");

        // 10750 报销菜单及按钮
        assertTrue(expenseSql.contains("10750"), "必须包含 10750 报销管理菜单");
        assertTrue(expenseSql.contains("lunaerp:finance:expense:query"), "必须包含报销查询权限");
        int[] expenseBtns = {10751, 10752, 10753, 10754, 10755};
        for (int btn : expenseBtns) {
            assertTrue(expenseSql.contains(String.valueOf(btn)), "必须包含报销按钮 " + btn);
        }
        assertTrue(expenseSql.contains("lunaerp:finance:expense:create"), "必须包含新建报销权限");
        assertTrue(expenseSql.contains("lunaerp:finance:expense:update"), "必须包含编辑报销权限");
        assertTrue(expenseSql.contains("lunaerp:finance:expense:submit"), "必须包含提交报销权限");
        assertTrue(expenseSql.contains("lunaerp:finance:expense:delete"), "必须包含删除报销权限");
        assertTrue(expenseSql.contains("lunaerp:finance:expense:print"), "必须包含打印报销单权限");

        // 10760 工资菜单及按钮
        assertTrue(payrollSql.contains("10760"), "必须包含 10760 工资核算菜单");
        assertTrue(payrollSql.contains("lunaerp:finance:payroll:query"), "必须包含工资查询权限");
        int[] payrollBtns = {10761, 10762, 10763, 10764, 10765, 10766, 10767, 10768};
        for (int btn : payrollBtns) {
            assertTrue(payrollSql.contains(String.valueOf(btn)), "必须包含工资按钮 " + btn);
        }
        assertTrue(payrollSql.contains("lunaerp:finance:payroll:create"), "必须包含新建工资单权限");
        assertTrue(payrollSql.contains("lunaerp:finance:payroll:update"), "必须包含编辑工资单权限");
        assertTrue(payrollSql.contains("lunaerp:finance:payroll:submit"), "必须包含提交工资单权限");
        assertTrue(payrollSql.contains("lunaerp:finance:payroll:import-defect"), "必须包含导入次品扣款权限");
        assertTrue(payrollSql.contains("lunaerp:finance:payroll:pay"), "必须包含发放登记权限");
        assertTrue(payrollSql.contains("lunaerp:finance:payroll:cancel"), "必须包含作废工资单权限");
        assertTrue(payrollSql.contains("lunaerp:finance:payroll:delete"), "必须包含删除工资单权限");
        assertTrue(payrollSql.contains("lunaerp:finance:payroll:print"), "必须包含打印工资表权限");

        // 角色赋权
        for (String sql : new String[]{accountSql, expenseSql, payrollSql}) {
            assertTrue(sql.contains("system_role_menu"), "必须赋权 system_role_menu");
            assertTrue(sql.contains("nextval('system_role_menu_seq')"), "必须显式调用 nextval('system_role_menu_seq') 防主键 null");
        }
    }

    @Test
    @DisplayName("验证默认审批流配置 expense 与 payroll 具备单节点财务主管审批")
    void testApprovalConfigs() throws IOException {
        String expenseSql = loadSqlContent("lunaerp-finance-expense.sql");
        String payrollSql = loadSqlContent("lunaerp-finance-payroll.sql");

        assertTrue(expenseSql.contains("biz_type = 'expense'"), "必须配置 expense 审批流");
        assertTrue(expenseSql.contains("'员工报销默认审批流'"), "必须包含员工报销审批流名称");
        assertTrue(expenseSql.contains("'财务主管审批'"), "报销必须包含财务主管审批节点");

        assertTrue(payrollSql.contains("biz_type = 'payroll'"), "必须配置 payroll 审批流");
        assertTrue(payrollSql.contains("'工资单默认审批流'"), "必须包含工资单审批流名称");
        assertTrue(payrollSql.contains("'财务主管审批'"), "工资单必须包含财务主管审批节点");
    }

    @Test
    @DisplayName("验证所有新创建的表具备完备的表与全列注释")
    void testCommentsCompleteness() throws IOException {
        String accountSql = loadSqlContent("lunaerp-finance-account-ext.sql");
        String expenseSql = loadSqlContent("lunaerp-finance-expense.sql");
        String payrollSql = loadSqlContent("lunaerp-finance-payroll.sql");

        String[] accountTables = {"lunaerp_finance_transfer", "lunaerp_finance_other_txn"};
        for (String table : accountTables) {
            assertTrue(accountSql.contains("COMMENT ON TABLE " + table + " IS"), table + " 必须有 COMMENT ON TABLE");
        }

        String[] expenseTables = {"lunaerp_finance_expense", "lunaerp_finance_expense_item"};
        for (String table : expenseTables) {
            assertTrue(expenseSql.contains("COMMENT ON TABLE " + table + " IS"), table + " 必须有 COMMENT ON TABLE");
        }

        String[] payrollTables = {
                "lunaerp_finance_payroll",
                "lunaerp_finance_payroll_employee",
                "lunaerp_finance_payroll_line_item",
                "lunaerp_finance_payroll_defect_ref",
                "lunaerp_finance_payroll_payment"
        };
        for (String table : payrollTables) {
            assertTrue(payrollSql.contains("COMMENT ON TABLE " + table + " IS"), table + " 必须有 COMMENT ON TABLE");
        }
    }

    @Test
    @DisplayName("验证领域 SQL 脚本严禁跨模块执行平台底座表 DDL (CREATE/ALTER/DROP 等)，仅允许操作 lunaerp_* 域表")
    void testNoCrossDomainDdlOnPlatformTables() throws IOException {
        String[] files = {
                "lunaerp-finance-account-ext.sql",
                "lunaerp-finance-expense.sql",
                "lunaerp-finance-payroll.sql"
        };

        for (String file : files) {
            String sql = loadSqlContent(file);
            List<SQLStatement> statements = parseSql(sql);

            for (SQLStatement stmt : statements) {
                if (stmt instanceof com.alibaba.druid.sql.ast.statement.SQLCreateTableStatement createTable) {
                    String tableName = createTable.getTableName().toLowerCase().replace("\"", "");
                    assertTrue(tableName.startsWith("lunaerp_"),
                            file + " 严禁创建非 lunaerp_ 前缀的表: " + tableName);
                } else if (stmt instanceof com.alibaba.druid.sql.ast.statement.SQLCreateIndexStatement createIndex) {
                    String tableName = SQLUtils.toSQLString(createIndex.getTable()).toLowerCase().replace("\"", "");
                    assertTrue(tableName.startsWith("lunaerp_"),
                            file + " 严禁在非 lunaerp_ 前缀的表上创建索引: " + tableName);
                } else if (stmt instanceof com.alibaba.druid.sql.ast.statement.SQLAlterTableStatement alterTable) {
                    String tableName = alterTable.getTableName().toLowerCase().replace("\"", "");
                    assertTrue(tableName.startsWith("lunaerp_"),
                            file + " 严禁修改非 lunaerp_ 前缀的表: " + tableName);
                }
            }

            Pattern bannedDdlPattern = Pattern.compile(
                    "(?i)(CREATE\\s+(?:UNIQUE\\s+)?INDEX[\\s\\S]*?ON\\s+system_\\w+|ALTER\\s+TABLE\\s+system_\\w+)"
            );
            Matcher matcher = bannedDdlPattern.matcher(sql);
            assertFalse(matcher.find(), () -> file + " 检测到对底座 system_* 表的越权 DDL 变更: " + matcher.group(1));
        }
    }

    @Test
    @DisplayName("验证 SQL 脚本满足空库与存量库幂等重跑规范 (IF NOT EXISTS + DO 块判存 + 先删后插)")
    void testIdempotencyProperties() throws IOException {
        String[] files = {
                "lunaerp-finance-account-ext.sql",
                "lunaerp-finance-expense.sql",
                "lunaerp-finance-payroll.sql"
        };

        for (String file : files) {
            String sql = loadSqlContent(file);
            assertFalse(sql.matches("(?s).*CREATE\\s+TABLE\\s+(?!IF\\s+NOT\\s+EXISTS).*"),
                    file + ": 所有 CREATE TABLE 必须携带 IF NOT EXISTS");
            assertFalse(sql.matches("(?s).*CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(?!IF\\s+NOT\\s+EXISTS).*"),
                    file + ": 所有 CREATE INDEX 必须携带 IF NOT EXISTS");
        }
    }
}
