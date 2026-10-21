package com.luna.lunasrv.module.lunaerp.production.workshop;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.luna.lunasrv.module.lunaerp.support.BaseSqlBootstrapTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 车间移动 H5 权限容器与配置种子 SQL 脚本单元测试与规范校验
 * 覆盖 Issue: .scratch/workshop-h5/issues/02-infra-config-dict-frontend-deps.md
 */
public class WorkshopH5SqlBootstrapTest extends BaseSqlBootstrapTest {

    private static final String SQL_FILE = "lunaerp-workshop-h5.sql";

    @Test
    @DisplayName("规约审查：验证 SQL 脚本包含标准工单与日期注释 (-- [YYYY-MM-DD])")
    void testHeaderDateComments() throws IOException {
        String sql = loadSqlContent(SQL_FILE);
        assertHeaderDateComment(sql, SQL_FILE);
    }

    @Test
    @DisplayName("验证 SQL 脚本存在且能被 PostgreSQL 语法解析器解析")
    void testSqlCanBeParsed() throws IOException {
        String sql = loadSqlContent(SQL_FILE);
        assertFalse(sql.isBlank(), SQL_FILE + " 内容不能为空");
        List<SQLStatement> statements = parseSql(sql);
        assertFalse(statements.isEmpty(), SQL_FILE + " 至少应解析出 DDL/DML 语句");
    }

    @Test
    @DisplayName("验证包含默认仓库配置项种子 luna.workshop.default-warehouse-id 且幂等可重复执行")
    void testDefaultWarehouseConfigSeed() throws IOException {
        String sql = loadSqlContent(SQL_FILE);
        assertTrue(sql.contains("luna.workshop.default-warehouse-id"), "必须包含参数键名 luna.workshop.default-warehouse-id");
        assertTrue(sql.contains("DELETE FROM infra_config WHERE config_key = 'luna.workshop.default-warehouse-id'"), "必须包含先清后插的幂等清理语句");
        assertTrue(sql.contains("INSERT INTO infra_config"), "必须包含插入 infra_config 语句");
        assertTrue(sql.contains("103"), "必须使用全局未占用参数主键 103");
    }

    @Test
    @DisplayName("验证包含布票加异常照片列 ALTER TABLE 与 COMMENT 且幂等可重复执行 (Issue 03)")
    void testWeavingTicketExceptionPhotosColumnAlter() throws IOException {
        String sql = loadSqlContent(SQL_FILE);
        assertTrue(sql.contains("ALTER TABLE lunaerp_production_weaving_ticket"), "必须包含 ALTER TABLE lunaerp_production_weaving_ticket 语句");
        assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS exception_photos jsonb"), "必须包含 ADD COLUMN IF NOT EXISTS exception_photos jsonb");
        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_production_weaving_ticket.exception_photos IS '异常现场照片 URL JSON 数组（车间 H5 异常登记上传，≤6 张，infra 文件服务）'"),
                "必须包含标准的字段 COMMENT");
    }

    @Test
    @DisplayName("验证包含录检明细加疵点照片列 ALTER TABLE 与 COMMENT 且幂等可重复执行 (Issue 04)")
    void testInspectionOrderItemPhotosColumnAlter() throws IOException {
        String sql = loadSqlContent(SQL_FILE);
        assertTrue(sql.contains("ALTER TABLE lunaerp_production_inspection_order_item"), "必须包含 ALTER TABLE lunaerp_production_inspection_order_item 语句");
        assertTrue(sql.contains("ADD COLUMN IF NOT EXISTS photos jsonb"), "必须包含 ADD COLUMN IF NOT EXISTS photos jsonb");
        assertTrue(sql.contains("COMMENT ON COLUMN lunaerp_production_inspection_order_item.photos IS '疵点照片 URL JSON 数组（车间 H5 录检上传，≤6 张，infra 文件服务）'"),
                "必须包含标准的字段 COMMENT");
    }

    @Test
    @DisplayName("验证录检明细缸号校验放宽 ALTER TABLE vat_no DROP NOT NULL (Issue 05)")
    void testInspectionOrderItemVatNoDropNotNull() throws IOException {
        String sql = loadSqlContent(SQL_FILE);
        assertTrue(sql.contains("ALTER TABLE lunaerp_production_inspection_order_item"), "必须包含 ALTER TABLE lunaerp_production_inspection_order_item 语句");
        assertTrue(sql.contains("ALTER COLUMN vat_no DROP NOT NULL"), "必须包含 ALTER COLUMN vat_no DROP NOT NULL 语句");
    }

    @Test
    @DisplayName("验证包含车间移动 H5 权限容器 10530 与三按钮 10531-10533 菜单种子且幂等 (Issue 05)")
    void testWorkshopH5MenuAndButtonsSeed() throws IOException {
        String sql = loadSqlContent(SQL_FILE);
        assertTrue(sql.contains("DELETE FROM system_menu WHERE id IN (10530, 10531, 10532, 10533)"), "必须包含先清后插的幂等清理语句");
        assertTrue(sql.contains("10530"), "必须包含菜单容器 10530");
        assertTrue(sql.contains("车间 H5"), "10530 名称必须为 '车间 H5'");
        assertTrue(sql.contains("10200"), "10530 父菜单必须为 10200");
        assertTrue(sql.contains("lunaerp:workshop:h5:report"), "必须包含移动报工按钮 10531 权限码");
        assertTrue(sql.contains("lunaerp:workshop:h5:inspection"), "必须包含移动质检按钮 10532 权限码");
        assertTrue(sql.contains("lunaerp:workshop:h5:dispatch"), "必须包含移动发纱按钮 10533 权限码");
    }

    @Test
    @DisplayName("验证包含车间工人与检验员角色种子及 super_admin/admin 权限同步 DO 块 (Issue 05)")
    void testWorkshopRoleSeedAndAdminSync() throws IOException {
        String sql = loadSqlContent(SQL_FILE);
        assertTrue(sql.contains("workshop_worker"), "必须包含角色 code workshop_worker");
        assertTrue(sql.contains("车间工人"), "必须包含角色名称 车间工人");
        assertTrue(sql.contains("workshop_inspector"), "必须包含角色 code workshop_inspector");
        assertTrue(sql.contains("车间检验员"), "必须包含角色名称 车间检验员");
        assertTrue(sql.contains("10531"), "workshop_worker 必须授权 10531");
        assertTrue(sql.contains("10533"), "workshop_worker 必须授权 10533");
        assertTrue(sql.contains("10532"), "workshop_inspector 必须授权 10532");
        assertTrue(sql.contains("super_admin"), "必须包含 super_admin / admin 同步赋权逻辑");
    }
}
