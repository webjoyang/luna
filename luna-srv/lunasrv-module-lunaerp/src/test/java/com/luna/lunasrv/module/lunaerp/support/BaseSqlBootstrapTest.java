package com.luna.lunasrv.module.lunaerp.support;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SQL 基建脚本离线测试公共抽象基类
 * <p>
 * 提供 PostgreSQL 脚本加载、Druid 语法树解析与规范断言能力：
 * 1. 验证 SQL 文件存在且语法解析通过；
 * 2. 自动校验符合规范的工单日期注释（-- [YYYY-MM-DD] <issue-id>）；
 * 3. 校验 TenantBaseDO 标准审计列与主键序列声明；
 * 4. 离线校验避免对本地真实 PostgreSQL 实例的运行期依赖。
 */
public abstract class BaseSqlBootstrapTest {

    private static final Pattern DATE_HEADER_PATTERN = Pattern.compile("--.*?\\[\\d{4}-\\d{2}-\\d{2}\\]");

    /**
     * 定位 sql/postgresql 目录路径
     */
    protected Path getSqlDirectory() {
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

    /**
     * 读取指定 SQL 脚本内容
     */
    protected String loadSqlContent(String fileName) throws IOException {
        Path sqlPath = getSqlDirectory().resolve(fileName);
        assertTrue(Files.exists(sqlPath), "SQL 文件必须存在: sql/postgresql/" + fileName);
        return Files.readString(sqlPath, StandardCharsets.UTF_8);
    }

    /**
     * 基于 Druid PostgreSQL 解析器解析 SQL 语句
     * 会剔除测试解析器不支持的 DO 块与部分索引 WHERE 子句
     */
    protected List<SQLStatement> parseSql(String sql) {
        String standardSql = sql.replaceAll("(?s)DO \\$\\$.*?\\$\\$;", "-- DO BLOCK OMITTED FOR PARSER;")
                .replaceAll("(?i)(CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+[^;]+?)\\s+WHERE\\s+[^;]+;", "$1;")
                .replaceAll("(?i)CREATE\\s+SEQUENCE\\s+IF\\s+NOT\\s+EXISTS", "CREATE SEQUENCE");
        return SQLUtils.parseStatements(standardSql, DbType.postgresql);
    }

    /**
     * 校验 SQL 脚本中是否包含规范的日期工单注释 (-- [YYYY-MM-DD] 工单号/说明)
     * 对应 luna-srv/sql/postgresql/README.md §I.2 规约
     */
    protected void assertHeaderDateComment(String sql, String fileName) {
        Matcher matcher = DATE_HEADER_PATTERN.matcher(sql);
        assertTrue(matcher.find(), fileName + " 必须包含符合规范的日期注释（如 -- [YYYY-MM-DD] 工单号/说明），见 luna-srv/sql/postgresql/README.md §I.2");
    }

    /**
     * 校验物理表是否包含 TenantBaseDO 标准 6 大审计列
     */
    protected void assertTenantBaseDoColumns(String sql, String tableName) {
        assertTrue(sql.contains("creator"), tableName + " 必须包含 creator 审计列");
        assertTrue(sql.contains("create_time"), tableName + " 必须包含 create_time 审计列");
        assertTrue(sql.contains("updater"), tableName + " 必须包含 updater 审计列");
        assertTrue(sql.contains("update_time"), tableName + " 必须包含 update_time 审计列");
        assertTrue(sql.contains("deleted"), tableName + " 必须包含 deleted 审计列");
        assertTrue(sql.contains("tenant_id"), tableName + " 必须包含 tenant_id 审计列");
    }

    /**
     * 校验物理表的主键约束与序列定义
     */
    protected void assertPrimaryKeySequence(String sql, String tableName) {
        assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + tableName), "必须包含 CREATE TABLE IF NOT EXISTS " + tableName);
        assertTrue(sql.contains("pk_" + tableName), "主键约束名必须包含 pk_" + tableName);
        assertTrue(sql.contains("ADD CONSTRAINT pk_" + tableName + " PRIMARY KEY (id)"), "必须包含主键约束定义 pk_" + tableName);
        assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS " + tableName + "_seq"), "必须包含主键序列 " + tableName + "_seq");
    }
}
