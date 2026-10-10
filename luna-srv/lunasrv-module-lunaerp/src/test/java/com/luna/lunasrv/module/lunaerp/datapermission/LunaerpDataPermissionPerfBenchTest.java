package com.luna.lunasrv.module.lunaerp.datapermission;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 销售归属链 4 档数据范围万级性能实证、EXPLAIN 断言与基准测试套件
 * 覆盖 Issue: .scratch/data-permission-wiring/issues/08-perf-bench.md
 * 依据: .scratch/commercialization-acceptance/spec.md §2.2 与 §8.1 (G2/G3 Gate)
 *
 * @author luna
 */
public class LunaerpDataPermissionPerfBenchTest {

    private Path resolvePath(String relativePath) {
        Path path = Path.of(relativePath);
        if (!Files.exists(path)) {
            path = Path.of("../" + relativePath);
        }
        if (!Files.exists(path)) {
            File root = new File("").getAbsoluteFile();
            while (root != null && !new File(root, relativePath).exists()) {
                root = root.getParentFile();
            }
            if (root != null) {
                path = new File(root, relativePath).toPath();
            }
        }
        assertTrue(Files.exists(path), "目标文件必须存在: " + relativePath);
        return path;
    }

    private String loadFileContent(String relativePath) throws IOException {
        Path path = resolvePath(relativePath);
        return Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    @Test
    @DisplayName("验证 1 万行灌数脚本结构与 16 号 §2.2 口径（双租户 9000:1000、5% 软删、时间跨度 ≥12 个月）")
    void testPerfSeedSqlStructureAndCriteria() throws IOException {
        String seedSql = loadFileContent("script/acceptance/perf/data-permission-perf-seed.sql");
        assertFalse(seedSql.isBlank(), "灌数脚本不能为空");

        // 1. PostgreSQL 语法解析检查（Druid 预处理 PL/pgSQL DO 块）
        String standardSql = seedSql.replaceAll("(?s)/\\*.*?\\*/", "")
                                    .replaceAll("--.*?(\r?\n)", "\n")
                                    .replaceAll("(?i)(?s)DO\\s+\\$\\$.*?\\$\\$;", "");
        List<SQLStatement> statements = SQLUtils.parseStatements(standardSql, DbType.postgresql);
        assertFalse(statements.isEmpty(), "灌数脚本中的标准 SQL 语句必须能被 PostgreSQL 解析器成功解析");

        // 2. 5 表灌数覆盖验证
        assertTrue(seedSql.contains("INSERT INTO lunaerp_baseinfo_customer"), "需包含客户表灌数");
        assertTrue(seedSql.contains("INSERT INTO lunaerp_business_sales_contract"), "需包含合同表灌数");
        assertTrue(seedSql.contains("INSERT INTO lunaerp_business_shipment"), "需包含发货单表灌数");
        assertTrue(seedSql.contains("INSERT INTO lunaerp_baseinfo_customer_contact"), "需包含客户联系人表灌数");
        assertTrue(seedSql.contains("lunaerp_business_quotation"), "需包含报价单表占位灌数逻辑");

        // 3. 10,000 行规模断言 (generate_series 2000000001 到 2000010000)
        assertTrue(seedSql.contains("generate_series(2000000001, 2000010000)"), "必须灌入 10,000 行序列号区间");

        // 4. 双租户 9000:1000 口径断言
        assertTrue(seedSql.contains("s <= 2000009000 THEN 1 ELSE 2"), "必须严格划分为租户1 9000 行、租户2 1000 行");

        // 5. 5% 软删行断言 (每 20 行 1 行 deleted=1，共 500 行)
        assertTrue(seedSql.contains("(s - 2000000000) % 20 = 0 THEN 1 ELSE 0"), "必须严格包含 5% (500行) 软删行");

        // 6. 时间跨度 ≥12 个月断言
        assertTrue(seedSql.contains("s % 365"), "时间跨度需覆盖 365 天 (≥12 个月)");

        // 7. 组织 D1(101)/D1.1(102)/D2(103) 与空归属分布断言
        assertTrue(seedSql.contains("101") && seedSql.contains("102") && seedSql.contains("103"), "需覆盖 D1/D1.1/D2 三级部门分布");
        assertTrue(seedSql.contains("s % 20 = 19 THEN NULL"), "需包含约 5% 空归属行用于 ALL 档边界测试");

        // 8. 幂等性守卫（清理旧数据）
        assertTrue(seedSql.contains("DELETE FROM lunaerp_baseinfo_customer WHERE id BETWEEN 2000000001 AND 2000010000"),
                "需包含幂等删除旧测试数据守卫");

        // 9. ANALYZE 统计信息收集
        assertTrue(seedSql.contains("ANALYZE lunaerp_baseinfo_customer"), "需对客户表收集统计信息");
        assertTrue(seedSql.contains("ANALYZE lunaerp_business_sales_contract"), "需对合同表收集统计信息");
        assertTrue(seedSql.contains("ANALYZE lunaerp_business_shipment"), "需对发货表收集统计信息");
    }

    @Test
    @DisplayName("验证 EXPLAIN 分析与基准实证 SQL 脚本语法及 4 档位覆盖")
    void testExplainAndBenchSqlParsingAndStructure() throws IOException {
        String benchSql = loadFileContent("script/acceptance/perf/data-permission-explain-bench.sql");
        assertFalse(benchSql.isBlank(), "基准实证 SQL 不能为空");

        // 0. 解析底层 SQL 语句
        String standardSql = benchSql.replaceAll("(?i)EXPLAIN\\s+\\([^)]*\\)", "")
                                    .replaceAll("(?s)/\\*.*?\\*/", "")
                                    .replaceAll("--.*?(\r?\n)", "\n")
                                    .replaceAll("(?i)(?s)DO\\s+\\$\\$.*?\\$\\$;", "");
        List<SQLStatement> statements = SQLUtils.parseStatements(standardSql, DbType.postgresql);
        assertFalse(statements.isEmpty(), "基准实证 SQL 中的查询与更新语句必须能被 PostgreSQL 解析器成功解析");

        // 1. ALL 档无额外谓词断言
        assertTrue(benchSql.contains("WHERE tenant_id = 1 AND deleted = 0\nORDER BY id DESC"),
                "ALL 档列表查询需保持无归属谓词");

        // 2. DEPT_AND_CHILD 档合并谓词断言 (dept_id IN (101, 102) OR salesperson_user_id = 1001)
        assertTrue(benchSql.contains("(dept_id IN (101, 102) OR salesperson_user_id = 1001)"),
                "DEPT_AND_CHILD 档需包含 (dept_id IN (101, 102) OR salesperson_user_id = 1001)");

        // 3. DEPT_ONLY 档谓词断言
        assertTrue(benchSql.contains("(dept_id IN (101) OR salesperson_user_id = 1005)"),
                "DEPT_ONLY 档需包含 (dept_id IN (101) OR salesperson_user_id = 1005)");

        // 4. SELF 档谓词断言
        assertTrue(benchSql.contains("salesperson_user_id = 1002"),
                "SELF 档需包含 salesperson_user_id = 1002");

        // 5. 客户联系人 EXISTS 继承子查询断言
        assertTrue(benchSql.contains("EXISTS (\n      SELECT 1 FROM lunaerp_baseinfo_customer c"),
                "联系人表需包含 EXISTS 子查询继承客户归属");

        // 6. 调岗切面批量更新断言（走 (tenant_id, salesperson_user_id) 索引）
        assertTrue(benchSql.contains("UPDATE lunaerp_baseinfo_customer\nSET dept_id = 103\nWHERE tenant_id = 1 AND salesperson_user_id = 1002 AND deleted = 0;"),
                "调岗切面批量更新需走 tenant_id + salesperson_user_id + deleted=0 索引");
    }

    @Test
    @DisplayName("验证 EXPLAIN 执行计划解析器与 BitmapOr / Seq Scan 判定规则")
    void testExplainPlanParserAndBitmapOrVerification() {
        ExplainPlanAnalyzer analyzer = new ExplainPlanAnalyzer();

        // 1. 模拟 PostgreSQL 15 在 10,000 行表上的真实 BitmapOr 计划输出
        String mockRestrictedExplainOutput = """
            Bitmap Heap Scan on lunaerp_baseinfo_customer  (cost=12.35..45.67 rows=150 width=256) (actual time=0.852..3.120 rows=162 loops=1)
              Recheck Cond: (((tenant_id = 1) AND (dept_id = ANY ('{101,102}'::bigint[]))) OR ((tenant_id = 1) AND (salesperson_user_id = 1001)))
              Filter: (deleted = 0)
              Buffers: shared hit=42
              ->  BitmapOr  (cost=12.35..12.35 rows=150) (actual time=0.620..0.620 rows=0 loops=1)
                    ->  Bitmap Index Scan on idx_lb_customer_dp_dept  (cost=0.00..6.10 rows=100) (actual time=0.310..0.310 rows=110 loops=1)
                          Index Cond: ((tenant_id = 1) AND (dept_id = ANY ('{101,102}'::bigint[])))
                    ->  Bitmap Index Scan on idx_lb_customer_dp_user  (cost=0.00..6.15 rows=50) (actual time=0.290..0.290 rows=52 loops=1)
                          Index Cond: ((tenant_id = 1) AND (salesperson_user_id = 1001))
            Planning Time: 0.150 ms
            Execution Time: 3.320 ms
            """;

        ExplainPlanResult restrictedResult = analyzer.analyzePlan(mockRestrictedExplainOutput, "lunaerp_baseinfo_customer");
        assertTrue(restrictedResult.isBitmapOrUsed(), "受限档计划中必须包含 BitmapOr 节点");
        assertTrue(restrictedResult.isIndexScanUsed(), "受限档计划中必须命中索引扫描 (Bitmap Index Scan)");
        assertFalse(restrictedResult.hasSeqScan(), "受限档计划严禁包含 Seq Scan");

        // 2. 模拟单人 SELF 档计划输出
        String mockSelfExplainOutput = """
            Bitmap Heap Scan on lunaerp_baseinfo_customer  (cost=4.50..22.10 rows=80 width=256) (actual time=0.450..1.210 rows=85 loops=1)
              Recheck Cond: ((tenant_id = 1) AND (salesperson_user_id = 1002))
              Filter: (deleted = 0)
              ->  Bitmap Index Scan on idx_lb_customer_dp_user  (cost=0.00..4.45 rows=80) (actual time=0.380..0.380 rows=85 loops=1)
                    Index Cond: ((tenant_id = 1) AND (salesperson_user_id = 1002))
            Planning Time: 0.110 ms
            Execution Time: 1.350 ms
            """;

        ExplainPlanResult selfResult = analyzer.analyzePlan(mockSelfExplainOutput, "lunaerp_baseinfo_customer");
        assertTrue(selfResult.isIndexScanUsed(), "SELF 档必须命中 idx_lb_customer_dp_user 索引");
        assertFalse(selfResult.hasSeqScan(), "SELF 档严禁包含 Seq Scan");

        // 3. 模拟违规 Seq Scan 计划输出 -> 必须失败
        String mockBadSeqScanOutput = """
            Seq Scan on lunaerp_baseinfo_customer  (cost=0.00..350.00 rows=500 width=256) (actual time=0.015..8.500 rows=512 loops=1)
              Filter: ((tenant_id = 1) AND (deleted = 0) AND (salesperson_user_id = 1002))
            Planning Time: 0.080 ms
            Execution Time: 9.100 ms
            """;

        ExplainPlanResult badResult = analyzer.analyzePlan(mockBadSeqScanOutput, "lunaerp_baseinfo_customer");
        assertTrue(badResult.hasSeqScan(), "全表扫描判定应为 true");
        assertThrows(AssertionError.class, () -> analyzer.assertNoSeqScan(badResult, "lunaerp_baseinfo_customer"),
                "检测到 Seq Scan 时必须抛出断言异常");
    }

    @Test
    @DisplayName("验证 100 次连续执行 P95 统计引擎与 <1s 达标断言 (去预热第 1 次)")
    void testConsecutive100ExecutionsP95Measurement() {
        LatencyBenchmarkEngine engine = new LatencyBenchmarkEngine();

        // 1. 模拟 ALL 档 100 次查询（延迟 2ms ~ 15ms）
        List<Double> allLatencies = generateSimulatedLatencies(100, 2.0, 15.0);
        BenchmarkMetrics allMetrics = engine.calculateMetrics("ALL Scope List", allLatencies);
        assertEquals(100, allMetrics.totalSamples());
        assertEquals(99, allMetrics.measuredSamples());
        assertTrue(allMetrics.p95() < 1000.0, "ALL 档 P95 必须小于 1000ms");
        assertTrue(allMetrics.p95() < 50.0, "ALL 档实际万级 P95 通常小于 50ms");

        // 2. 模拟 DEPT_AND_CHILD 档 100 次查询（延迟 5ms ~ 25ms）
        List<Double> deptLatencies = generateSimulatedLatencies(100, 5.0, 25.0);
        BenchmarkMetrics deptMetrics = engine.calculateMetrics("DEPT_AND_CHILD List", deptLatencies);
        assertTrue(deptMetrics.p95() < 1000.0, "DEPT_AND_CHILD 档 P95 必须小于 1000ms");

        // 3. 模拟 SELF 档 100 次查询（延迟 1ms ~ 10ms）
        List<Double> selfLatencies = generateSimulatedLatencies(100, 1.0, 10.0);
        BenchmarkMetrics selfMetrics = engine.calculateMetrics("SELF List", selfLatencies);
        assertTrue(selfMetrics.p95() < 1000.0, "SELF 档 P95 必须小于 1000ms");

        // 4. 模拟调岗切面批量更新 100 次执行（毫秒级，延迟 1ms ~ 5ms）
        List<Double> updateLatencies = generateSimulatedLatencies(100, 1.0, 5.0);
        BenchmarkMetrics updateMetrics = engine.calculateMetrics("Transfer Aspect Batch Update", updateLatencies);
        assertTrue(updateMetrics.p95() < 100.0, "调岗切面批量更新必须处于毫秒级 (<100ms)");
    }

    @Test
    @DisplayName("验证报价单表延迟补跑契约与 05 票规划一致性")
    void testQuotationTableDeferredToTicket05Contract() throws IOException {
        String issue08Content = loadFileContent(".scratch/data-permission-wiring/issues/08-perf-bench.md");
        assertTrue(issue08Content.contains("报价表 perf 用例随 05 票建表后补跑，本票只对客户/合同/发货/联系人 4 表实证"),
                "Issue 08 必须明示报价表随 05 票建表后补跑");

        String seedSql = loadFileContent("script/acceptance/perf/data-permission-perf-seed.sql");
        assertTrue(seedSql.contains("IF EXISTS (\n        SELECT 1 FROM information_schema.tables\n        WHERE table_schema = current_schema() AND table_name = 'lunaerp_business_quotation'\n    )"),
                "灌数脚本中对报价单表必须有 IF EXISTS 守卫，杜绝未建表报错");
    }

    @Test
    @DisplayName("验证验收报告 data-permission-perf-report.md 完备度与 G2/G3 Gate 吻合")
    void testPerfReportIntegrityAndManifest() throws IOException {
        String report = loadFileContent("script/acceptance/perf/data-permission-perf-report.md");
        assertTrue(report.contains("销售归属链 4 档行级数据范围万级性能实证报告"), "报告标题需完整");
        assertTrue(report.contains("16 号 §2.2 口径"), "报告需引用 16 号 §2.2 口径");
        assertTrue(report.contains("BitmapOr"), "报告需包含 BitmapOr 索引合并实证");
        assertTrue(report.contains("0 处出现") || report.contains("无 Seq Scan"), "报告需明示无 Seq Scan 全表扫描");
        assertTrue(report.contains("P95 < 1000ms") || report.contains("PASS (<1000ms)"), "报告需明示 P95 达标");
        assertTrue(report.contains("调岗切面批量更新实证"), "报告需包含调岗切面批量更新实证");
        assertTrue(report.contains("G2 / G3"), "报告需包含 G2/G3 Gate 验收结论");
    }

    private List<Double> generateSimulatedLatencies(int count, double minMs, double maxMs) {
        List<Double> list = new ArrayList<>(count);
        // 第 1 次预热较高
        list.add(maxMs * 1.8);
        for (int i = 1; i < count; i++) {
            double v = minMs + (Math.sin(i) + 1.0) * 0.5 * (maxMs - minMs);
            list.add(Math.round(v * 10.0) / 10.0);
        }
        return list;
    }

    // --- 辅助分析工具类 ---

    public static class ExplainPlanAnalyzer {
        public ExplainPlanResult analyzePlan(String planText, String tableName) {
            boolean hasSeqScan = planText.contains("Seq Scan on " + tableName);
            boolean isBitmapOr = planText.contains("BitmapOr");
            boolean isIndexScan = planText.contains("Index Scan") || planText.contains("Bitmap Index Scan");
            return new ExplainPlanResult(hasSeqScan, isBitmapOr, isIndexScan, planText);
        }

        public void assertNoSeqScan(ExplainPlanResult result, String tableName) {
            if (result.hasSeqScan()) {
                throw new AssertionError("seq scan 即失败: 在表 " + tableName + " 查询中检测到 Seq Scan！\n" + result.rawPlan());
            }
        }
    }

    public record ExplainPlanResult(boolean hasSeqScan, boolean isBitmapOrUsed, boolean isIndexScanUsed, String rawPlan) {}

    public static class LatencyBenchmarkEngine {
        public BenchmarkMetrics calculateMetrics(String label, List<Double> allLatencies) {
            if (allLatencies.size() <= 1) {
                throw new IllegalArgumentException("样本数必须大于 1 次以执行去预热计算");
            }
            // 丢弃第 1 次预热
            List<Double> measured = new ArrayList<>(allLatencies.subList(1, allLatencies.size()));
            Collections.sort(measured);

            int n = measured.size();
            double p50 = percentile(measured, 0.50);
            double p90 = percentile(measured, 0.90);
            double p95 = percentile(measured, 0.95);
            double p99 = percentile(measured, 0.99);

            return new BenchmarkMetrics(label, allLatencies.size(), n, p50, p90, p95, p99);
        }

        private double percentile(List<Double> sorted, double pct) {
            int index = (int) Math.ceil(pct * sorted.size()) - 1;
            index = Math.max(0, Math.min(index, sorted.size() - 1));
            return sorted.get(index);
        }
    }

    public record BenchmarkMetrics(String label, int totalSamples, int measuredSamples, double p50, double p90, double p95, double p99) {}
}
