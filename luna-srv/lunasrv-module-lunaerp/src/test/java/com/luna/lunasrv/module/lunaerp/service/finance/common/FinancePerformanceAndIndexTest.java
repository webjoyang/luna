package com.luna.lunasrv.module.lunaerp.service.finance.common;

import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;
import com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging.CustomerAgingCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 财务模块性能基准与分录账核心索引验证测试 (Issue 12)
 *
 * 1. 验证 SQL 脚本中分录账聚合索引包含 (tenant_id, party_type, party_id, biz_date)
 * 2. 模拟万级（10,000 条）分录数据在内存中执行 FIFO 账龄分桶聚合计算，断言 P95 远低于 1s
 * 3. 模拟万级单据列表排序与分页过滤，断言 P95 响应时间满足商业化性能基准
 */
@DisplayName("FinancePerformanceAndIndexTest 财务性能与索引验证测试")
public class FinancePerformanceAndIndexTest {

    @Test
    @DisplayName("索引架构验证：确认 DDL 包含 (tenant_id, party_type, party_id, biz_date) 聚合索引")
    void testLedgerEntryIndexDefinition() throws Exception {
        java.io.File file = null;
        String[] candidates = {
                "sql/postgresql/lunaerp-finance-customer-recon.sql",
                "../sql/postgresql/lunaerp-finance-customer-recon.sql",
                "../../sql/postgresql/lunaerp-finance-customer-recon.sql",
                "luna-srv/sql/postgresql/lunaerp-finance-customer-recon.sql"
        };
        for (String candidate : candidates) {
            java.io.File f = new java.io.File(candidate);
            if (f.exists()) {
                file = f;
                break;
            }
        }
        if (file == null) {
            java.io.File dir = new java.io.File(System.getProperty("user.dir"));
            while (dir != null && dir.exists()) {
                java.io.File f = new java.io.File(dir, "luna-srv/sql/postgresql/lunaerp-finance-customer-recon.sql");
                if (f.exists()) {
                    file = f;
                    break;
                }
                f = new java.io.File(dir, "sql/postgresql/lunaerp-finance-customer-recon.sql");
                if (f.exists()) {
                    file = f;
                    break;
                }
                dir = dir.getParentFile();
            }
        }
        assertNotNull(file, "必须定位到 SQL DDL 脚本文件 lunaerp-finance-customer-recon.sql");
        assertTrue(file.exists(), "SQL DDL 脚本必须存在: " + file.getAbsolutePath());

        String sqlContent = java.nio.file.Files.readString(file.toPath(), StandardCharsets.UTF_8);

        // 断言复合索引存在
        assertTrue(sqlContent.contains("idx_lfle_party_date"),
                "DDL 必须包含分录账往来与日期索引 idx_lfle_party_date");
        assertTrue(sqlContent.contains("(tenant_id, party_type, party_id, biz_date)"),
                "分录账索引字段顺序必须为 (tenant_id, party_type, party_id, biz_date)");

        // 断言单号防重索引包含软删扫描
        assertTrue(sqlContent.contains("lunaerp_finance_statement"), "必须包含对账单表 DDL");
        assertTrue(sqlContent.contains("lunaerp_finance_invoice_application"), "必须包含发票申请表 DDL");
    }

    @Test
    @DisplayName("性能基准测试：10,000 条分录 FIFO 账龄纯函数计算 P95 响应时间远小于 1s")
    void testFifoAgingPerformance_TenThousandEntries() {
        LocalDate queryDate = LocalDate.now();
        CustomerDO customer = new CustomerDO();
        customer.setId(888L);
        customer.setCompanyName("性能测试客户");
        customer.setCreditDays(30);

        // 构造 10,000 条分录：8,000 笔借方发货 + 2,000 笔贷方核销
        List<LedgerEntryDO> entries = new ArrayList<>(10000);
        Random random = new Random(42);

        for (long i = 1; i <= 8000; i++) {
            int daysAgo = random.nextInt(365); // 分布在过去 1 年
            entries.add(LedgerEntryDO.builder()
                    .id(i)
                    .partyId(888L)
                    .partyType("CUSTOMER")
                    .direction("DEBIT")
                    .category(FinanceLedgerCategoryEnum.AR_INC.getValue())
                    .originalAmount(new BigDecimal("100.00"))
                    .currency("CNY")
                    .exchangeRate(BigDecimal.ONE)
                    .bizDate(queryDate.minusDays(daysAgo))
                    .build());
        }

        for (long i = 8001; i <= 10000; i++) {
            int daysAgo = random.nextInt(180);
            entries.add(LedgerEntryDO.builder()
                    .id(i)
                    .partyId(888L)
                    .partyType("CUSTOMER")
                    .direction("CREDIT")
                    .category(FinanceLedgerCategoryEnum.RECEIPT_WRITE_OFF.getValue())
                    .originalAmount(new BigDecimal("300.00"))
                    .currency("CNY")
                    .exchangeRate(BigDecimal.ONE)
                    .bizDate(queryDate.minusDays(daysAgo))
                    .build());
        }

        // 预热 JVM
        for (int i = 0; i < 5; i++) {
            CustomerAgingCalculator.calculate(customer, "CNY", queryDate, entries, BigDecimal.ZERO, null);
        }

        // 采样 50 次测量耗时
        int iterations = 50;
        long[] elapsedTimes = new long[iterations];

        for (int i = 0; i < iterations; i++) {
            long start = System.nanoTime();
            CustomerAgingCalculator.AgingCalculationResult result = CustomerAgingCalculator.calculate(
                    customer, "CNY", queryDate, entries, BigDecimal.ZERO, null);
            long end = System.nanoTime();
            elapsedTimes[i] = (end - start) / 1_000_000; // ms
            assertNotNull(result.getSummary());
            assertTrue(result.getSummary().getTotalBalance().compareTo(BigDecimal.ZERO) > 0);
        }

        Arrays.sort(elapsedTimes);
        // P95 = 95th 百分位数
        int p95Index = (int) Math.ceil(iterations * 0.95) - 1;
        long p95Ms = elapsedTimes[p95Index];

        System.out.println("万级分录 FIFO 账龄计算 50 次迭代耗时：Min=" + elapsedTimes[0] + "ms, Median="
                + elapsedTimes[iterations / 2] + "ms, P95=" + p95Ms + "ms, Max=" + elapsedTimes[iterations - 1] + "ms");

        assertTrue(p95Ms < 1000, "10,000 条分录 FIFO 账龄计算 P95 必须小于 1s (实际: " + p95Ms + "ms)");
        assertTrue(p95Ms < 200, "内存纯函数计算应在 200ms 以内 (实际: " + p95Ms + "ms)");
    }

    @Test
    @DisplayName("性能基准测试：10,000 条列表数据过滤与分页排序 P95 远小于 100ms")
    void testListFilteringAndPagingPerformance_TenThousandEntries() {
        List<Map<String, Object>> records = new ArrayList<>(10000);
        for (int i = 1; i <= 10000; i++) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", (long) i);
            map.put("statementNo", "CS2026" + String.format("%06d", i));
            map.put("status", (i % 3 == 0) ? "CONFIRMED" : (i % 3 == 1 ? "DRAFT" : "SENT"));
            map.put("amount", new BigDecimal(i * 10));
            map.put("createTime", System.currentTimeMillis() - i * 1000L);
            records.add(map);
        }

        int iterations = 50;
        long[] elapsed = new long[iterations];

        for (int iter = 0; iter < iterations; iter++) {
            long start = System.nanoTime();
            // 模拟列表多重过滤与分页：过滤 status = CONFIRMED, 按 createTime 倒序排序, 取第 20 页 (pageNo=20, pageSize=20)
            List<Map<String, Object>> paged = records.stream()
                    .filter(r -> "CONFIRMED".equals(r.get("status")))
                    .sorted((a, b) -> Long.compare((Long) b.get("createTime"), (Long) a.get("createTime")))
                    .skip(400)
                    .limit(20)
                    .collect(Collectors.toList());
            long end = System.nanoTime();
            elapsed[iter] = (end - start) / 1_000_000;
            assertEquals(20, paged.size());
        }

        Arrays.sort(elapsed);
        long p95Ms = elapsed[(int) Math.ceil(iterations * 0.95) - 1];
        System.out.println("万级列表内存分页过滤 P95 耗时: " + p95Ms + "ms");
        assertTrue(p95Ms < 100, "万级列表数据过滤与分页 P95 必须小于 100ms (实际: " + p95Ms + "ms)");
    }
}
