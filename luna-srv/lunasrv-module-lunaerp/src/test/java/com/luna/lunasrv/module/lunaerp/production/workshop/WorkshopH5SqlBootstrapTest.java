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
}
