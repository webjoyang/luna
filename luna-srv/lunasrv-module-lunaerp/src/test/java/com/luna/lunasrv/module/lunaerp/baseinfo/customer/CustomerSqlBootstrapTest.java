package com.luna.lunasrv.module.lunaerp.baseinfo.customer;

import com.alibaba.druid.sql.ast.SQLStatement;
import com.luna.lunasrv.module.lunaerp.support.BaseSqlBootstrapTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户基础信息与门户权限按钮 SQL 脚本校验
 */
public class CustomerSqlBootstrapTest extends BaseSqlBootstrapTest {

    @Test
    @DisplayName("验证客户 SQL 脚本能被 PostgreSQL 语法解析器正常解析")
    void testSqlCanBeParsed() throws IOException {
        String sql = loadSqlContent("lunaerp-baseinfo-customer.sql");
        assertFalse(sql.isBlank(), "lunaerp-baseinfo-customer.sql 内容不能为空");
        List<SQLStatement> statements = parseSql(sql);
        assertFalse(statements.isEmpty(), "至少应解析出 DDL/DML 语句");
    }

    @Test
    @DisplayName("验证客户门户按钮 10150 权限码定义与幂等 DELETE/INSERT")
    void testPortalTokenButtonSql() throws IOException {
        String sql = loadSqlContent("lunaerp-baseinfo-customer.sql");
        assertTrue(sql.contains("10150"), "必须包含按钮 10150");
        assertTrue(sql.contains("lunaerp:baseinfo:customer:portal-token"), "必须包含权限码 lunaerp:baseinfo:customer:portal-token");
        assertTrue(sql.contains("DELETE FROM system_menu WHERE id = 10150"), "必须包含幂等 DELETE 语句");
        assertTrue(sql.contains("10030"), "必须挂在 10030 客户信息菜单下");
    }

}
