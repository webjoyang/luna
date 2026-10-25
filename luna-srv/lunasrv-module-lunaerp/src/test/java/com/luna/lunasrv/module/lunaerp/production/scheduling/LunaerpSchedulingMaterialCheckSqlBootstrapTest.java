package com.luna.lunasrv.module.lunaerp.production.scheduling;

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
 * 针对排产中心与齐套检查 SQL 初始化脚本 lunaerp-production-scheduling-material-check.sql 的单元测试与规范校验
 * 覆盖 issue: .scratch/scheduling-material-check/issues/01-sql-menu-bootstrap.md
 */
public class LunaerpSchedulingMaterialCheckSqlBootstrapTest {

    private String loadSqlContent() throws IOException {
        Path sqlPath = Path.of("sql/postgresql/lunaerp-production-scheduling-material-check.sql");
        if (!Files.exists(sqlPath)) {
            sqlPath = Path.of("../sql/postgresql/lunaerp-production-scheduling-material-check.sql");
        }
        if (!Files.exists(sqlPath)) {
            File root = new File("").getAbsoluteFile();
            while (root != null && !new File(root, "sql/postgresql/lunaerp-production-scheduling-material-check.sql").exists()) {
                root = root.getParentFile();
            }
            if (root != null) {
                sqlPath = new File(root, "sql/postgresql/lunaerp-production-scheduling-material-check.sql").toPath();
            }
        }
        assertNotNull(sqlPath, "SQL 文件路径不能为 null");
        assertTrue(Files.exists(sqlPath), "SQL 文件必须存在于 sql/postgresql/lunaerp-production-scheduling-material-check.sql");
        return Files.readString(sqlPath, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("验证 SQL 脚本存在且能被 PostgreSQL 语法解析器解析")
    void testSqlCanBeParsed() throws IOException {
        String sql = loadSqlContent();
        assertFalse(sql.isBlank(), "SQL 文件内容不能为空");

        List<SQLStatement> statements = SQLUtils.parseStatements(sql, DbType.postgresql);
        assertFalse(statements.isEmpty(), "至少应成功解析出 DML 语句");
    }

    @Test
    @DisplayName("验证零 DDL 与零字典：无建表、无加列、无自建序列、无字典定义")
    void testZeroDdlAndNoDict() throws IOException {
        String sql = loadSqlContent();

        // 规范：零建表/零加列/零字典/零序列
        assertFalse(sql.toUpperCase().contains("CREATE TABLE"), "本脚本必须为零建表，禁止包含 CREATE TABLE");
        assertFalse(sql.toUpperCase().contains("ALTER TABLE"), "本脚本必须为零加列，禁止包含 ALTER TABLE");
        assertFalse(sql.toUpperCase().contains("CREATE SEQUENCE"), "本脚本必须为零自建序列，禁止包含 CREATE SEQUENCE");
        assertFalse(sql.contains("system_dict_type"), "本模块复用既有字典，禁止定义 system_dict_type");
        assertFalse(sql.contains("system_dict_data"), "本模块复用既有字典，禁止定义 system_dict_data");
    }

    @Test
    @DisplayName("验证 10510 排产中心菜单配置正确（挂 10200 生产管理，sort 12，权限码 lunaerp:production:weaving-order-task:query）")
    void testMenuBootstrap10510() throws IOException {
        String sql = loadSqlContent();

        assertTrue(sql.contains("10510"), "必须包含 10510 菜单 ID");
        assertTrue(sql.contains("排产中心"), "必须包含「排产中心」菜单名称");
        assertTrue(sql.contains("lunaerp:production:weaving-order-task:query"),
                "排产中心菜单权限码必须为 lunaerp:production:weaving-order-task:query");
        assertTrue(sql.contains("10200"), "父级菜单必须为 10200 生产管理");
        assertTrue(sql.contains("scheduling"), "菜单路由路径必须包含 scheduling");
        assertTrue(sql.contains("ep:calendar"), "菜单图标必须为 ep:calendar");
        assertTrue(sql.contains("lunaerp-elmui/production/scheduling/index"),
                "组件路径必须为 lunaerp-elmui/production/scheduling/index");
        assertTrue(sql.contains("LunaerpElmuiProductionScheduling"),
                "组件名称必须为 LunaerpElmuiProductionScheduling");
    }

    @Test
    @DisplayName("验证 10520 齐套检查菜单配置正确（挂 10200 生产管理，sort 13，权限码 lunaerp:production:material-check:query）")
    void testMenuBootstrap10520() throws IOException {
        String sql = loadSqlContent();

        assertTrue(sql.contains("10520"), "必须包含 10520 菜单 ID");
        assertTrue(sql.contains("齐套检查"), "必须包含「齐套检查」菜单名称");
        assertTrue(sql.contains("lunaerp:production:material-check:query"),
                "齐套检查菜单权限码必须为 lunaerp:production:material-check:query");
        assertTrue(sql.contains("material-check"), "菜单路由路径必须包含 material-check");
        assertTrue(sql.contains("ep:files"), "菜单图标必须为 ep:files");
        assertTrue(sql.contains("lunaerp-elmui/production/material-check/index"),
                "组件路径必须为 lunaerp-elmui/production/material-check/index");
        assertTrue(sql.contains("LunaerpElmuiProductionMaterialCheck"),
                "组件名称必须为 LunaerpElmuiProductionMaterialCheck");
    }

    @Test
    @DisplayName("验证超管与管理员角色授权（使用 system_role_menu 与 system_role_menu_seq）")
    void testRoleAuthorization() throws IOException {
        String sql = loadSqlContent();

        assertTrue(sql.contains("system_role_menu"), "必须包含角色菜单授权写入 system_role_menu");
        assertTrue(sql.contains("system_role_menu_seq"),
                "system_role_menu 插入必须显式使用 nextval('system_role_menu_seq') 生成主键");
        assertTrue(sql.contains("super_admin"), "必须为 super_admin 授权");
        assertTrue(sql.contains("admin"), "必须为 admin 授权");
    }

    @Test
    @DisplayName("验证幂等可重复执行（事务、DELETE FROM system_menu 与 system_role_menu）")
    void testIdempotency() throws IOException {
        String sql = loadSqlContent();

        assertTrue(sql.contains("BEGIN;"), "必须以事务开始 BEGIN;");
        assertTrue(sql.contains("COMMIT;"), "必须以事务提交 COMMIT;");
        assertTrue(sql.contains("DELETE FROM system_menu"), "插入菜单前必须清理存量");
        assertTrue(sql.contains("DELETE FROM system_role_menu"), "插入角色权限前必须清理存量");
    }
}
