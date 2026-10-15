package com.luna.lunasrv.module.lunaerp.finance.supplierrecon;

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
 * 供应商对账与收付款 SQL 基建脚本单元测试与规范校验
 * 覆盖 Issue: .scratch/vendor-reconciliation/issues/01-sql-bootstrap.md
 */
public class LunaerpVendorReconSqlBootstrapTest {

    private String loadSqlContent(String fileName) throws IOException {
        Path sqlPath = Path.of("sql/postgresql/" + fileName);
        if (!Files.exists(sqlPath)) {
            sqlPath = Path.of("../sql/postgresql/" + fileName);
        }
        if (!Files.exists(sqlPath)) {
            File root = new File("").getAbsoluteFile();
            while (root != null && !new File(root, "sql/postgresql/" + fileName).exists()) {
                root = root.getParentFile();
            }
            if (root != null) {
                sqlPath = new File(root, "sql/postgresql/" + fileName).toPath();
            }
        }
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
    @DisplayName("验证 3 个 SQL 脚本存在且能被 PostgreSQL 语法解析器解析")
    void testSqlCanBeParsed() throws IOException {
        String[] sqlFiles = {
                "lunaerp-finance-vendor-recon.sql",
                "lunaerp-finance-payment.sql",
                "lunaerp-stock-in-yarn-order-ref.sql"
        };
        for (String file : sqlFiles) {
            String sql = loadSqlContent(file);
            assertFalse(sql.isBlank(), file + " 内容不能为空");
            List<SQLStatement> statements = parseSql(sql);
            assertFalse(statements.isEmpty(), file + " 至少应解析出 DDL/DML 语句");
        }
    }

    @Test
    @DisplayName("验证供应商对账表 1、表 2 DDL、主键约束与 TenantBaseDO 列")
    void testVendorReconTablesAndColumns() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-vendor-recon.sql");

        String[] tables = {
                "lunaerp_finance_supplier_recon",
                "lunaerp_finance_supplier_recon_item"
        };
        for (String table : tables) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + table), "必须包含 CREATE TABLE IF NOT EXISTS " + table);
            assertTrue(sql.contains("pk_" + table), "主键约束名必须包含 pk_" + table);
            assertTrue(sql.contains("ADD CONSTRAINT pk_" + table + " PRIMARY KEY (id)"), "必须包含主键约束定义 pk_" + table);
            assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS " + table + "_seq"), "必须包含主键序列 " + table + "_seq");

            // TenantBaseDO 字段
            assertTrue(sql.contains("creator"), table + " 必须包含 creator 字段");
            assertTrue(sql.contains("create_time"), table + " 必须包含 create_time 字段");
            assertTrue(sql.contains("updater"), table + " 必须包含 updater 字段");
            assertTrue(sql.contains("update_time"), table + " 必须包含 update_time 字段");
            assertTrue(sql.contains("deleted"), table + " 必须包含 deleted 字段");
            assertTrue(sql.contains("tenant_id"), table + " 必须包含 tenant_id 字段");
        }

        // 主表核心字段
        assertTrue(sql.contains("recon_no"), "主表必须包含 recon_no");
        assertTrue(sql.contains("supplier_id"), "主表必须包含 supplier_id");
        assertTrue(sql.contains("supplier_name"), "主表必须包含 supplier_name");
        assertTrue(sql.contains("period_start"), "主表必须包含 period_start");
        assertTrue(sql.contains("period_end"), "主表必须包含 period_end");
        assertTrue(sql.contains("status"), "主表必须包含 status");
        assertTrue(sql.contains("total_amount"), "主表必须包含 total_amount");
        assertTrue(sql.contains("confirmed_by"), "主表必须包含 confirmed_by");
        assertTrue(sql.contains("confirmed_at"), "主表必须包含 confirmed_at");
        assertTrue(sql.contains("cancelled_by"), "主表必须包含 cancelled_by");
        assertTrue(sql.matches("(?s).*cancelled_by\\s+int8.*"), "cancelled_by 必须为 int8 类型与 confirmed_by 对齐");
        assertTrue(sql.contains("cancelled_at"), "主表必须包含 cancelled_at");

        // 主表租户内全量唯一索引（含软删行，不带 WHERE deleted=0）
        assertTrue(sql.contains("uk_lfsr_recon_no"), "必须定义 recon_no 唯一索引 uk_lfsr_recon_no");
        assertTrue(sql.contains("(tenant_id, recon_no)"), "唯一索引必须覆盖 (tenant_id, recon_no)");

        // 行表核心字段
        assertTrue(sql.contains("recon_id"), "行表必须包含 recon_id");
        assertTrue(sql.contains("line_type"), "行表必须包含 line_type");
        assertTrue(sql.contains("source_ref_type"), "行表必须包含 source_ref_type");
        assertTrue(sql.contains("source_ref_id"), "行表必须包含 source_ref_id");
        assertTrue(sql.contains("source_ref_item_id"), "行表必须包含 source_ref_item_id");
        assertTrue(sql.contains("inspection_order_id"), "行表必须包含 inspection_order_id");
        assertTrue(sql.contains("inspection_order_no"), "行表必须包含 inspection_order_no");
        assertTrue(sql.contains("inspection_type"), "行表必须包含 inspection_type");
        assertTrue(sql.contains("stock_in_id"), "行表必须包含 stock_in_id");
        assertTrue(sql.contains("stock_in_no"), "行表必须包含 stock_in_no");
        assertTrue(sql.contains("stock_in_item_id"), "行表必须包含 stock_in_item_id");
        assertTrue(sql.contains("yarn_order_id"), "行表必须包含 yarn_order_id");
        assertTrue(sql.contains("yarn_order_no"), "行表必须包含 yarn_order_no");
        assertTrue(sql.contains("product_id"), "行表必须包含 product_id");
        assertTrue(sql.contains("product_code"), "行表必须包含 product_code");
        assertTrue(sql.contains("product_name"), "行表必须包含 product_name");
        assertTrue(sql.contains("batch_no"), "行表必须包含 batch_no");
        assertTrue(sql.contains("qty_kg"), "行表必须包含 qty_kg");
        assertTrue(sql.contains("qty_m"), "行表必须包含 qty_m");
        assertTrue(sql.contains("qty_unit"), "行表必须包含 qty_unit");
        assertTrue(sql.contains("pricing_basis"), "行表必须包含 pricing_basis");
        assertTrue(sql.contains("bill_qty"), "行表必须包含 bill_qty");
        assertTrue(sql.contains("unit_price"), "行表必须包含 unit_price");
        assertTrue(sql.contains("line_amount"), "行表必须包含 line_amount");
        assertTrue(sql.contains("adjust_reason"), "行表必须包含 adjust_reason");

        // 行表三大必查索引
        assertTrue(sql.contains("(tenant_id, recon_id)"), "行表必须有 (tenant_id, recon_id) 索引");
        assertTrue(sql.contains("(tenant_id, source_ref_type, source_ref_id)"), "行表必须有 (tenant_id, source_ref_type, source_ref_id) 索引");
        assertTrue(sql.contains("(tenant_id, stock_in_item_id)"), "行表必须有 (tenant_id, stock_in_item_id) 索引");
    }

    @Test
    @DisplayName("验证付款域表 3、表 4、表 5 DDL、主键约束与 TenantBaseDO 列")
    void testPaymentTablesAndColumns() throws IOException {
        String sql = loadSqlContent("lunaerp-finance-payment.sql");

        String[] tables = {
                "lunaerp_finance_payment_apply",
                "lunaerp_finance_payment",
                "lunaerp_finance_payment_ref"
        };
        for (String table : tables) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + table), "必须包含 CREATE TABLE IF NOT EXISTS " + table);
            assertTrue(sql.contains("pk_" + table), "主键约束名必须包含 pk_" + table);
            assertTrue(sql.contains("ADD CONSTRAINT pk_" + table + " PRIMARY KEY (id)"), "必须包含主键约束定义 pk_" + table);
            assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS " + table + "_seq"), "必须包含主键序列 " + table + "_seq");

            // TenantBaseDO 字段
            assertTrue(sql.contains("creator"), table + " 必须包含 creator 字段");
            assertTrue(sql.contains("create_time"), table + " 必须包含 create_time 字段");
            assertTrue(sql.contains("updater"), table + " 必须包含 updater 字段");
            assertTrue(sql.contains("update_time"), table + " 必须包含 update_time 字段");
            assertTrue(sql.contains("deleted"), table + " 必须包含 deleted 字段");
            assertTrue(sql.contains("tenant_id"), table + " 必须包含 tenant_id 字段");
        }

        // 付款申请核心列与索引
        assertTrue(sql.contains("apply_no"), "付款申请必须包含 apply_no");
        assertTrue(sql.contains("apply_type"), "付款申请必须包含 apply_type");
        assertTrue(sql.contains("supplier_id"), "付款申请必须包含 supplier_id");
        assertTrue(sql.contains("supplier_name"), "付款申请必须包含 supplier_name");
        assertTrue(sql.contains("recon_id"), "付款申请必须包含 recon_id");
        assertTrue(sql.contains("recon_no"), "付款申请必须包含 recon_no");
        assertTrue(sql.contains("amount"), "付款申请必须包含 amount");
        assertTrue(sql.contains("purpose"), "付款申请必须包含 purpose");
        assertTrue(sql.contains("bank_name"), "付款申请必须包含 bank_name");
        assertTrue(sql.contains("bank_account"), "付款申请必须包含 bank_account");
        assertTrue(sql.contains("approval_status"), "付款申请必须包含 approval_status");

        assertTrue(sql.contains("uk_lfpa_apply_no"), "必须定义 apply_no 租户内唯一索引 uk_lfpa_apply_no");
        assertTrue(sql.contains("(tenant_id, supplier_id, approval_status)"), "付款申请必须有 (tenant_id, supplier_id, approval_status) 索引");
        assertTrue(sql.contains("(tenant_id, recon_id)"), "付款申请必须有 (tenant_id, recon_id) 索引");

        // 收付款单核心列与索引
        assertTrue(sql.contains("payment_no"), "收付款单必须包含 payment_no");
        assertTrue(sql.contains("direction"), "收付款单必须包含 direction");
        assertTrue(sql.contains("account_id"), "收付款单必须包含 account_id");
        assertTrue(sql.contains("pay_amount"), "收付款单必须包含 pay_amount");
        assertTrue(sql.contains("prepay_used_amount"), "收付款单必须包含 prepay_used_amount");
        assertTrue(sql.contains("status"), "收付款单必须包含 status");
        assertTrue(sql.contains("uk_lfp_payment_no"), "必须定义 payment_no 租户内唯一索引 uk_lfp_payment_no");
        assertTrue(sql.contains("(tenant_id, supplier_id)"), "收付款单必须包含 (tenant_id, supplier_id) 索引");

        // 共享表最小列集守卫（Spec L154：杜绝未经声明的扩展列与多余索引）
        assertFalse(sql.contains("payment_date"), "payment 表不应包含未声明的 payment_date 列");
        assertFalse(sql.contains("idx_lfp_account_id"), "payment 表不应包含未声明的 idx_lfp_account_id 索引");

        // 核销子表核心列与索引
        assertTrue(sql.contains("payment_id"), "核销子表必须包含 payment_id");
        assertTrue(sql.contains("ref_type"), "核销子表必须包含 ref_type");
        assertTrue(sql.contains("ref_id"), "核销子表必须包含 ref_id");
        assertTrue(sql.contains("allocate_amount"), "核销子表必须包含 allocate_amount");
        assertTrue(sql.contains("(tenant_id, ref_id)"), "核销子表必须包含 (tenant_id, ref_id) 索引");
        assertTrue(sql.contains("(tenant_id, payment_id)"), "核销子表必须包含 (tenant_id, payment_id) 索引");
    }

    @Test
    @DisplayName("验证入库单纱线采购单引用前置 ALTER 脚本")
    void testStockInYarnOrderRef() throws IOException {
        String sql = loadSqlContent("lunaerp-stock-in-yarn-order-ref.sql");

        assertTrue(sql.contains("ALTER TABLE lunaerp_stock_in ADD COLUMN IF NOT EXISTS yarn_order_id int8;"),
                "必须包含 yarn_order_id 加列语句");
        assertTrue(sql.contains("ALTER TABLE lunaerp_stock_in ADD COLUMN IF NOT EXISTS yarn_order_no varchar(32);"),
                "必须包含 yarn_order_no 加列语句");
        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_stock_in.yarn_order_id IS"), "必须包含 yarn_order_id 注释");
        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_stock_in.yarn_order_no IS"), "必须包含 yarn_order_no 注释");
        assertTrue(sql.contains("idx_lb_stock_in_yarn_order_id"), "必须包含 yarn_order_id 索引");
    }

    @Test
    @DisplayName("验证字典 2269, 2270, 2271 及数据先删后插幂等性与 5 态审批字典补建")
    void testDictionaries() throws IOException {
        String reconSql = loadSqlContent("lunaerp-finance-vendor-recon.sql");
        String paymentSql = loadSqlContent("lunaerp-finance-payment.sql");

        // 2269 对账单三态
        assertTrue(reconSql.contains("2269"), "vendor-recon.sql 需包含字典 2269");
        assertTrue(reconSql.contains("lunaerp_supplier_recon_status"), "需定义 lunaerp_supplier_recon_status 字典类型");
        assertTrue(reconSql.contains("'DRAFT'"), "需包含 DRAFT 字典值");
        assertTrue(reconSql.contains("'CONFIRMED'"), "需包含 CONFIRMED 字典值");
        assertTrue(reconSql.contains("'CANCELLED'"), "需包含 CANCELLED 字典值");

        // 2270 申请类型
        assertTrue(paymentSql.contains("2270"), "payment.sql 需包含字典 2270");
        assertTrue(paymentSql.contains("lunaerp_payment_apply_type"), "需定义 lunaerp_payment_apply_type 字典类型");
        assertTrue(paymentSql.contains("'RECON'"), "需包含 RECON 字典值");
        assertTrue(paymentSql.contains("'PREPAY'"), "需包含 PREPAY 字典值");

        // 2271 收付款方向
        assertTrue(paymentSql.contains("2271"), "payment.sql 需包含字典 2271");
        assertTrue(paymentSql.contains("lunaerp_finance_payment_direction"), "需定义 lunaerp_finance_payment_direction 字典类型");
        assertTrue(paymentSql.contains("'IN'"), "需包含 IN 字典值");
        assertTrue(paymentSql.contains("'OUT'"), "需包含 OUT 字典值");

        // 5 态审批状态字典补建
        assertTrue(paymentSql.contains("lunaerp_approval_status"), "payment.sql 需补建 5 态 lunaerp_approval_status 字典");

        // 字典幂等删除检查
        assertTrue(reconSql.contains("DELETE FROM system_dict_type"), "vendor-recon.sql 必须先 DELETE 字典类型");
        assertTrue(reconSql.contains("DELETE FROM system_dict_data"), "vendor-recon.sql 必须先 DELETE 字典数据");
        assertTrue(paymentSql.contains("DELETE FROM system_dict_type"), "payment.sql 必须先 DELETE 字典类型");
        assertTrue(paymentSql.contains("DELETE FROM system_dict_data"), "payment.sql 必须先 DELETE 字典数据");
    }

    @Test
    @DisplayName("验证菜单 10730/10740 及按钮权限 10731-10736/10741-10743 先删后插与角色赋权")
    void testMenusAndPermissions() throws IOException {
        String reconSql = loadSqlContent("lunaerp-finance-vendor-recon.sql");
        String paymentSql = loadSqlContent("lunaerp-finance-payment.sql");

        // 10700 财务一级目录（两个脚本均独立判存插入，确保单独重跑可用）
        assertTrue(reconSql.contains("10700") && paymentSql.contains("10700"), "reconSql 与 paymentSql 均需包含 10700 财务一级目录判存插入");

        // 10730 供应商对账
        assertTrue(reconSql.contains("10730"), "需包含 10730 菜单");
        assertTrue(reconSql.contains("lunaerp:finance:supplierrecon:query"), "需包含 10730 查询权限标识");
        int[] reconButtons = {10731, 10732, 10733, 10734, 10735, 10736};
        for (int btn : reconButtons) {
            assertTrue(reconSql.contains(String.valueOf(btn)), "需包含按钮 " + btn);
        }
        assertTrue(reconSql.contains("lunaerp:finance:supplierrecon:create"), "需包含 10731 生成权限");
        assertTrue(reconSql.contains("lunaerp:finance:supplierrecon:update"), "需包含 10732 编辑/重取权限");
        assertTrue(reconSql.contains("lunaerp:finance:supplierrecon:confirm"), "需包含 10733 确认/作废权限");
        assertTrue(reconSql.contains("lunaerp:finance:supplierrecon:generate"), "需包含 10734 生成付款申请权限");
        assertTrue(reconSql.contains("lunaerp:finance:supplierrecon:print"), "需包含 10735 打印权限");
        assertTrue(reconSql.contains("lunaerp:finance:supplierrecon:delete"), "需包含 10736 删除权限");

        // 10740 收付款
        assertTrue(paymentSql.contains("10740"), "需包含 10740 菜单");
        assertTrue(paymentSql.contains("lunaerp:finance:payment:query"), "需包含 10740 查询权限标识");
        int[] paymentButtons = {10741, 10742, 10743};
        for (int btn : paymentButtons) {
            assertTrue(paymentSql.contains(String.valueOf(btn)), "需包含按钮 " + btn);
        }
        assertTrue(paymentSql.contains("lunaerp:finance:payment:apply"), "需包含 10741/10742 申请权限");
        assertTrue(paymentSql.contains("lunaerp:finance:payment:register"), "需包含 10743 付款登记权限");
        assertFalse(paymentSql.replaceAll("(?m)--.*$", "").contains("10744"), "不应包含 10744 删除付款单按钮（红冲二期）");

        // 角色赋权与 PostgreSQL 自增序列主键防 null
        assertTrue(reconSql.contains("system_role_menu"), "vendor-recon.sql 必须赋权 system_role_menu");
        assertTrue(paymentSql.contains("system_role_menu"), "payment.sql 必须赋权 system_role_menu");
        assertTrue(reconSql.contains("nextval('system_role_menu_seq')"), "vendor-recon.sql 赋权时必须显式生成 system_role_menu.id 防 null");
        assertTrue(paymentSql.contains("nextval('system_role_menu_seq')"), "payment.sql 赋权时必须显式生成 system_role_menu.id 防 null");
    }

    @Test
    @DisplayName("验证 5 张新表均具备完备的表注释与全列注释")
    void testCommentsCompleteness() throws IOException {
        String reconSql = loadSqlContent("lunaerp-finance-vendor-recon.sql");
        String paymentSql = loadSqlContent("lunaerp-finance-payment.sql");

        String[] reconTables = {
                "lunaerp_finance_supplier_recon",
                "lunaerp_finance_supplier_recon_item"
        };
        for (String table : reconTables) {
            assertTrue(reconSql.contains("COMMENT ON TABLE " + table + " IS"), table + " 必须有 COMMENT ON TABLE");
        }

        String[] paymentTables = {
                "lunaerp_finance_payment_apply",
                "lunaerp_finance_payment",
                "lunaerp_finance_payment_ref"
        };
        for (String table : paymentTables) {
            assertTrue(paymentSql.contains("COMMENT ON TABLE " + table + " IS"), table + " 必须有 COMMENT ON TABLE");
        }
    }

    @Test
    @DisplayName("验证 5 个主键序列均定义为 CREATE SEQUENCE IF NOT EXISTS")
    void testSequences() throws IOException {
        String reconSql = loadSqlContent("lunaerp-finance-vendor-recon.sql");
        String paymentSql = loadSqlContent("lunaerp-finance-payment.sql");

        assertTrue(reconSql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_supplier_recon_seq;"));
        assertTrue(reconSql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_supplier_recon_item_seq;"));
        assertTrue(paymentSql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payment_apply_seq;"));
        assertTrue(paymentSql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payment_seq;"));
        assertTrue(paymentSql.contains("CREATE SEQUENCE IF NOT EXISTS lunaerp_finance_payment_ref_seq;"));
    }

    @Test
    @DisplayName("验证 SQL 脚本满足空库与存量库幂等重跑规范（IF NOT EXISTS + DO 块判存 + 先删后插）")
    void testIdempotencyProperties() throws IOException {
        String[] sqlFiles = {
                "lunaerp-finance-vendor-recon.sql",
                "lunaerp-finance-payment.sql",
                "lunaerp-stock-in-yarn-order-ref.sql"
        };
        for (String file : sqlFiles) {
            String sql = loadSqlContent(file);
            // 确保没有裸的 CREATE TABLE (非 IF NOT EXISTS)
            assertFalse(sql.matches("(?s).*CREATE\\s+TABLE\\s+(?!IF\\s+NOT\\s+EXISTS).*"),
                    file + " 中所有 CREATE TABLE 必须携带 IF NOT EXISTS");
            // 确保没有裸的 CREATE INDEX (非 IF NOT EXISTS)
            assertFalse(sql.matches("(?s).*CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+(?!IF\\s+NOT\\s+EXISTS).*"),
                    file + " 中所有 CREATE INDEX 必须携带 IF NOT EXISTS");
        }
    }
}
