package com.luna.lunasrv.module.lunaerp.approval;

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
 * 针对审批引擎 SQL 基建脚本 lunaerp-approval.sql 的单元测试与规范校验
 * 覆盖 issue: .scratch/approval-engine/issues/01-sql-bootstrap.md
 */
public class LunaerpApprovalSqlBootstrapTest {

    private String loadSqlContent() throws IOException {
        // 项目工作目录下查找
        Path sqlPath = Path.of("sql/postgresql/lunaerp-approval.sql");
        if (!Files.exists(sqlPath)) {
            // 从模块所在子目录回退一层查找
            sqlPath = Path.of("../sql/postgresql/lunaerp-approval.sql");
        }
        if (!Files.exists(sqlPath)) {
            // 兜底绝对路径查找
            File root = new File("").getAbsoluteFile();
            while (root != null && !new File(root, "sql/postgresql/lunaerp-approval.sql").exists()) {
                root = root.getParentFile();
            }
            if (root != null) {
                sqlPath = new File(root, "sql/postgresql/lunaerp-approval.sql").toPath();
            }
        }
        assertTrue(Files.exists(sqlPath), "SQL 文件必须存在于 sql/postgresql/lunaerp-approval.sql");
        return Files.readString(sqlPath, StandardCharsets.UTF_8);
    }

    @Test
    @DisplayName("验证 SQL 脚本存在且能被 PostgreSQL 语法解析器解析")
    void testSqlCanBeParsed() throws IOException {
        String sql = loadSqlContent();
        assertFalse(sql.isBlank(), "SQL 文件内容不能为空");

        // DO $$ 块是 PostgreSQL PL/pgSQL 专有块，部分索引 WHERE 与 SEQUENCE IF NOT EXISTS 是 PG 专有扩展语法，标准化后送入 Druid
        String standardSql = sql.replaceAll("(?s)DO \\$\\$.*?\\$\\$;", "-- DO BLOCK OMITTED FOR PARSER;")
                                .replaceAll("(?i)(CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+[^;]+?)\\s+WHERE\\s+[^;]+;", "$1;")
                                .replaceAll("(?i)CREATE\\s+SEQUENCE\\s+IF\\s+NOT\\s+EXISTS", "CREATE SEQUENCE");
        List<SQLStatement> statements = SQLUtils.parseStatements(standardSql, DbType.postgresql);
        assertFalse(statements.isEmpty(), "至少应成功解析出 DDL/DML 语句");
    }

    @Test
    @DisplayName("验证 5 张表 DDL 就位并包含标准 TenantBaseDO 字段与预留字段")
    void testTablesAndColumns() throws IOException {
        String sql = loadSqlContent();

        String[] tables = {
            "lunaerp_approval_config",
            "lunaerp_approval_config_node",
            "lunaerp_approval_instance",
            "lunaerp_approval_task",
            "lunaerp_approval_record"
        };

        for (String table : tables) {
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS " + table),
                    "必须包含 CREATE TABLE IF NOT EXISTS " + table);
            // DO 块主键判存
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

        // 预留列及乐观锁检查
        assertTrue(sql.contains("flowable_proc_inst_id"), "instance 表必须包含 flowable_proc_inst_id 预留字段");
        assertTrue(sql.contains("flowable_task_id"), "task 表必须包含 flowable_task_id 预留字段");
        assertTrue(sql.contains("version"), "task 表必须包含 version 乐观锁字段");
    }

    @Test
    @DisplayName("验证 5 个主键序列就位")
    void testSequences() throws IOException {
        String sql = loadSqlContent();

        String[] sequences = {
            "lunaerp_approval_config_seq",
            "lunaerp_approval_config_node_seq",
            "lunaerp_approval_instance_seq",
            "lunaerp_approval_task_seq",
            "lunaerp_approval_record_seq"
        };

        for (String seq : sequences) {
            assertTrue(sql.contains("CREATE SEQUENCE IF NOT EXISTS " + seq),
                    "必须包含序列 CREATE SEQUENCE IF NOT EXISTS " + seq);
        }
    }

    @Test
    @DisplayName("验证唯一索引（含软删行）与辅助索引就位")
    void testIndexes() throws IOException {
        String sql = loadSqlContent();

        // 唯一索引：(tenant_id, biz_type) 启用态唯一（含软删行，无 deleted=0）
        assertTrue(sql.contains("uk_lunaerp_approval_config_tenant_biz_enabled"),
                "必须包含 config 启用态唯一索引 uk_lunaerp_approval_config_tenant_biz_enabled");
        assertTrue(sql.contains("WHERE status = 'ENABLED'"),
                "config 启用态唯一索引需限定 WHERE status = 'ENABLED'");
        assertFalse(sql.contains("WHERE status = 'ENABLED' AND deleted = 0"),
                "唯一索引必须含软删行，不能带 AND deleted = 0");

        // 辅助索引
        assertTrue(sql.contains("idx_lunaerp_approval_config_node_config_sort"), "node 需包含 (config_id, sort) 索引");
        assertTrue(sql.contains("idx_lunaerp_approval_instance_biz"), "instance 需包含 biz 索引");
        assertTrue(sql.contains("idx_lunaerp_approval_instance_submitter"), "instance 需包含 submitter 索引");
        assertTrue(sql.contains("idx_lunaerp_approval_instance_status"), "instance 需包含 status 索引");
        assertTrue(sql.contains("idx_lunaerp_approval_task_approver_status"), "task 需包含 approver+status 索引");
        assertTrue(sql.contains("idx_lunaerp_approval_task_instance_id"), "task 需包含 instance_id 索引");
        assertTrue(sql.contains("idx_lunaerp_approval_record_instance_time"), "record 需包含 instance+operate_time 索引");
        assertTrue(sql.contains("idx_lunaerp_approval_record_operator_action"), "record 需包含 operator+action 索引");
    }

    @Test
    @DisplayName("验证审批中心一级目录、4个子菜单与按钮权限注册")
    void testMenusAndButtons() throws IOException {
        String sql = loadSqlContent();

        // 一级目录
        assertTrue(sql.contains("10800"), "需包含 10800 审批中心顶级目录");
        assertTrue(sql.contains("'/approval'"), "顶级目录 path 应为 /approval");
        assertTrue(sql.contains("109"), "顶级目录 sort 应为 109");

        // 子菜单
        assertTrue(sql.contains("10810") && sql.contains("'待我审批'"), "需包含 10810 待我审批");
        assertTrue(sql.contains("10820") && sql.contains("'我已审批'"), "需包含 10820 我已审批");
        assertTrue(sql.contains("10830") && sql.contains("'我发起的'"), "需包含 10830 我发起的");
        assertTrue(sql.contains("10840") && sql.contains("'审批链配置'"), "需包含 10840 审批链配置");

        // 权限标识
        assertTrue(sql.contains("lunaerp:approval:task:query"), "待我/我已/我发起菜单需注册 lunaerp:approval:task:query");
        assertTrue(sql.contains("lunaerp:approval:config:query"), "配置菜单需注册 lunaerp:approval:config:query");
        assertTrue(sql.contains("10841") && sql.contains("lunaerp:approval:config:create"), "需注册 10841 创建按钮");
        assertTrue(sql.contains("10842") && sql.contains("lunaerp:approval:config:update"), "需注册 10842 修改按钮");
        assertTrue(sql.contains("10843") && sql.contains("lunaerp:approval:config:delete"), "需注册 10843 删除按钮");

        // 组件与路由名
        assertTrue(sql.contains("approval/todo/index") && sql.contains("ApprovalTodo"));
        assertTrue(sql.contains("approval/done/index") && sql.contains("ApprovalDone"));
        assertTrue(sql.contains("approval/mine/index") && sql.contains("ApprovalMine"));
        assertTrue(sql.contains("approval/config/index") && sql.contains("ApprovalConfig"));

        // 幂等性先删后插
        assertTrue(sql.contains("DELETE FROM system_menu WHERE id IN (10800, 10810, 10820, 10830, 10840, 10841, 10842, 10843)"));
    }

    @Test
    @DisplayName("验证 6 个字典类型与字典数据就位")
    void testDictionaries() throws IOException {
        String sql = loadSqlContent();

        // 6 个 dict_type
        String[] dictTypes = {
            "lunaerp_approval_biz_type",
            "lunaerp_approval_node_type",
            "lunaerp_approval_approver_type",
            "lunaerp_approval_instance_status",
            "lunaerp_approval_task_action",
            "lunaerp_approval_record_action"
        };
        for (String dt : dictTypes) {
            assertTrue(sql.contains(dt), "需定义字典类型 " + dt);
        }

        // 单据类型字典值
        assertTrue(sql.contains("'sales_contract'"));
        assertTrue(sql.contains("'payment'"));
        assertTrue(sql.contains("'expense'"));
        assertTrue(sql.contains("'payroll'"));
        assertTrue(sql.contains("'sales_return'"));

        // 节点类型字典值
        assertTrue(sql.contains("'SEQUENTIAL'"));
        assertTrue(sql.contains("'OR_SIGN'"));
        assertTrue(sql.contains("'AND_SIGN'"));

        // 审批人来源字典值
        assertTrue(sql.contains("'USER'"));
        assertTrue(sql.contains("'POST'"));
        assertTrue(sql.contains("'ROLE'"));
        assertTrue(sql.contains("'DEPT_LEADER'"));
        assertTrue(sql.contains("'SUBMITTER_LEADER'"));

        // 实例状态字典值
        assertTrue(sql.contains("'APPROVING'"));
        assertTrue(sql.contains("'APPROVED'"));
        assertTrue(sql.contains("'REJECTED'"));
        assertTrue(sql.contains("'CANCELLED'"));

        // 任务动作字典值
        assertTrue(sql.contains("'DONE'"));
        assertTrue(sql.contains("'REJECTED'"));
        assertTrue(sql.contains("'TRANSFERRED'"));

        // 记录动作字典值
        assertTrue(sql.contains("'SUBMIT'"));
        assertTrue(sql.contains("'APPROVE'"));
        assertTrue(sql.contains("'REJECT'"));
        assertTrue(sql.contains("'TRANSFER'"));
        assertTrue(sql.contains("'WITHDRAW'"));

        // 字典数据幂等先删后插
        assertTrue(sql.contains("DELETE FROM system_dict_type"));
        assertTrue(sql.contains("DELETE FROM system_dict_data"));
    }

    @Test
    @DisplayName("验证 5 张表均具备完备的表注释与全列注释")
    void testCommentsCompleteness() throws IOException {
        String sql = loadSqlContent();

        String[] tables = {
            "lunaerp_approval_config",
            "lunaerp_approval_config_node",
            "lunaerp_approval_instance",
            "lunaerp_approval_task",
            "lunaerp_approval_record"
        };

        for (String table : tables) {
            assertTrue(sql.contains("COMMENT ON TABLE " + table + " IS"),
                    table + " 必须有 COMMENT ON TABLE");
        }

        // 匹配全部 COMMENT ON COLUMN 数量，5张表共 11+13+17+18+14 = 73 列注释
        Pattern pattern = Pattern.compile("COMMENT ON COLUMN lunaerp_approval_[a-z_]+\\.[a-z_]+ IS");
        Matcher matcher = pattern.matcher(sql);
        int commentCount = 0;
        while (matcher.find()) {
            commentCount++;
        }
        assertEquals(73, commentCount, "5 张表所有列均需包含 COMMENT ON COLUMN（共 73 列）");
    }
}
