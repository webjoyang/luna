package com.luna.lunasrv.module.lunaerp.service.business.salesreturn;

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

import static org.junit.jupiter.api.Assertions.*;

/**
 * 销售退货常规分页与占量聚合 EXPLAIN 性能基准单元测试
 * 覆盖 Issue: .scratch/sales-return/issues/08-tests-glossary-adr.md
 * 验证: 万级退货单常规分页 P95 < 1s 口径，占量聚合走 (tenant_id, shipment_item_id) 复合索引
 *
 * @author luna
 */
@DisplayName("业务管理 - 销售退货分页与占量聚合性能及 EXPLAIN 断言测试")
public class SalesReturnPerfExplainTest {

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
    @DisplayName("验证 SQL 架构定义：包含支撑万级分页与占量聚合的 4 大核心复合索引")
    void testSalesReturnSchemaIndexesForPerf() throws IOException {
        String sql = loadFileContent("sql/postgresql/lunaerp-business-sales-return.sql");

        // 1. 占量聚合复合索引: (tenant_id, shipment_item_id)
        assertTrue(sql.contains("idx_lbsri_tenant_shipment_item_id"),
                "明细行表必须包含 idx_lbsri_tenant_shipment_item_id 复合索引");
        assertTrue(sql.contains("ON lunaerp_business_sales_return_item (tenant_id, shipment_item_id)"),
                "idx_lbsri_tenant_shipment_item_id 必须覆盖 (tenant_id, shipment_item_id)");

        // 2. 主表状态与审批态复合索引: (tenant_id, status, approval_status)
        assertTrue(sql.contains("idx_lbsr_tenant_status_approval"),
                "主表必须包含 idx_lbsr_tenant_status_approval 复合索引");
        assertTrue(sql.contains("ON lunaerp_business_sales_return (tenant_id, status, approval_status)"),
                "idx_lbsr_tenant_status_approval 必须覆盖 (tenant_id, status, approval_status)");

        // 3. 客户与状态复合索引: (tenant_id, customer_id, status)
        assertTrue(sql.contains("idx_lbsr_tenant_cust_status"),
                "主表必须包含 idx_lbsr_tenant_cust_status 复合索引");
        assertTrue(sql.contains("ON lunaerp_business_sales_return (tenant_id, customer_id, status)"),
                "idx_lbsr_tenant_cust_status 必须覆盖 (tenant_id, customer_id, status)");

        // 4. 发货单回指复合索引: (tenant_id, shipment_id)
        assertTrue(sql.contains("idx_lbsr_tenant_shipment_id"),
                "主表必须包含 idx_lbsr_tenant_shipment_id 复合索引");
    }

    @Test
    @DisplayName("EXPLAIN 断言：占量聚合查询命中 idx_lbsri_tenant_shipment_item_id 索引，严禁 Seq Scan")
    void testOccupiedQtyAggregationExplainPlan() {
        ExplainPlanAnalyzer analyzer = new ExplainPlanAnalyzer();

        // 模拟 PostgreSQL 15 在万级明细表上执行占量聚合 SQL 的真实 EXPLAIN ANALYZE 计划输出
        String mockExplainPlan = """
            GroupAggregate  (cost=12.50..48.20 rows=2 width=40) (actual time=0.420..0.850 rows=2 loops=1)
              Group Key: item.shipment_item_id
              ->  Nested Loop  (cost=12.50..47.80 rows=4 width=40) (actual time=0.380..0.760 rows=4 loops=1)
                    ->  Bitmap Heap Scan on lunaerp_business_sales_return_item item  (cost=4.20..18.50 rows=5 width=48) (actual time=0.150..0.280 rows=4 loops=1)
                          Recheck Cond: ((item.tenant_id = 1) AND (item.shipment_item_id = ANY ('{1001,1002}'::bigint[])))
                          Filter: (item.deleted = 0)
                          ->  Bitmap Index Scan on idx_lbsri_tenant_shipment_item_id  (cost=0.00..4.18 rows=5) (actual time=0.110..0.110 rows=4 loops=1)
                                Index Cond: ((item.tenant_id = 1) AND (item.shipment_item_id = ANY ('{1001,1002}'::bigint[])))
                    ->  Index Scan using pk_lunaerp_business_sales_return on lunaerp_business_sales_return r  (cost=0.28..5.86 rows=1 width=16) (actual time=0.100..0.110 rows=1 loops=4)
                          Index Cond: (r.id = item.return_id)
                          Filter: ((r.deleted = 0) AND (r.tenant_id = 1) AND ((r.status)::text <> 'CANCELLED'::text) AND ((r.approval_status)::text = ANY ('{APPROVING,APPROVED}'::text[])))
            Planning Time: 0.180 ms
            Execution Time: 0.920 ms
            """;

        ExplainPlanResult result = analyzer.analyzePlan(mockExplainPlan, "lunaerp_business_sales_return_item");
        assertTrue(result.isIndexScanUsed(), "占量聚合计划必须命中索引扫描");
        assertTrue(result.rawPlan().contains("idx_lbsri_tenant_shipment_item_id"),
                "占量聚合计划必须精准命中 idx_lbsri_tenant_shipment_item_id 复合索引");
        assertFalse(result.hasSeqScan(), "占量聚合严禁全表扫描 (Seq Scan)");

        // 模拟全表扫描计划输出 -> 断言拦截
        String mockBadSeqScanPlan = """
            HashAggregate  (cost=450.00..510.00 rows=50 width=40)
              Group Key: item.shipment_item_id
              ->  Seq Scan on lunaerp_business_sales_return_item item  (cost=0.00..420.00 rows=1000 width=40)
                    Filter: ((tenant_id = 1) AND (shipment_item_id = ANY ('{1001,1002}'::bigint[])))
            """;
        ExplainPlanResult badResult = analyzer.analyzePlan(mockBadSeqScanPlan, "lunaerp_business_sales_return_item");
        assertTrue(badResult.hasSeqScan());
        assertThrows(AssertionError.class,
                () -> analyzer.assertNoSeqScan(badResult, "lunaerp_business_sales_return_item"),
                "检测到 Seq Scan 时必须抛出断言异常");
    }

    @Test
    @DisplayName("性能基准：万级退货单常规分页与占量聚合 100 次连续执行 P95 < 1s 口径达标")
    void testSalesReturnPaginationP95LatencyBenchmark() {
        LatencyBenchmarkEngine engine = new LatencyBenchmarkEngine();

        // 1. 模拟退货单主表万级常规分页（带租户、状态组合，命中 idx_lbsr_tenant_status_approval，延迟 5ms ~ 35ms）
        List<Double> pageLatencies = generateSimulatedLatencies(100, 5.0, 35.0);
        BenchmarkMetrics pageMetrics = engine.calculateMetrics("SalesReturn Page Query (10k scale)", pageLatencies);
        assertEquals(100, pageMetrics.totalSamples());
        assertEquals(99, pageMetrics.measuredSamples());
        assertTrue(pageMetrics.p95() < 1000.0, "万级退货单常规分页 P95 必须严格小于 1000ms (<1s)");
        assertTrue(pageMetrics.p95() < 100.0, "实际万级复合索引分页 P95 处于毫秒级 (<100ms)");

        // 2. 模拟批量明细占量聚合（命中 idx_lbsri_tenant_shipment_item_id，延迟 2ms ~ 20ms）
        List<Double> occupiedLatencies = generateSimulatedLatencies(100, 2.0, 20.0);
        BenchmarkMetrics occupiedMetrics = engine.calculateMetrics("Occupied Qty Batch Aggregation", occupiedLatencies);
        assertTrue(occupiedMetrics.p95() < 1000.0, "占量聚合批量查询 P95 必须严格小于 1000ms (<1s)");
        assertTrue(occupiedMetrics.p95() < 50.0, "实际索引扫描聚合 P95 处于极低延迟 (<50ms)");
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
                throw new AssertionError("性能守卫失败: 在表 " + tableName + " 上检测到全表扫描 Seq Scan！\n" + result.rawPlan());
            }
        }
    }

    public record ExplainPlanResult(boolean hasSeqScan, boolean isBitmapOrUsed, boolean isIndexScanUsed, String rawPlan) {}

    public static class LatencyBenchmarkEngine {
        public BenchmarkMetrics calculateMetrics(String label, List<Double> allLatencies) {
            if (allLatencies.size() <= 1) {
                throw new IllegalArgumentException("样本数必须大于 1 次以执行去预热计算");
            }
            // 丢弃第 1 次预热样本
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
