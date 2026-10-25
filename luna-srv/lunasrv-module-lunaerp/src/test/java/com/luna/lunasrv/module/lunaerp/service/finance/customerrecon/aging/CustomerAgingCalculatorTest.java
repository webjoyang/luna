package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 客户账龄纯函数计算单元测试（Seam A）
 *
 * @author luna
 */
public class CustomerAgingCalculatorTest {

    @Test
    @DisplayName("FIFO 消耗测试：跨月多笔收款/冲减按发生时间逐笔消耗最老 AR_INC")
    public void testFifoConsumption_OrderAndMultipleWriteOffs() {
        LocalDate queryDate = LocalDate.of(2026, 4, 1);

        CustomerAgingCalculator.ArEntry entry1 = new CustomerAgingCalculator.ArEntry(
                1L, LocalDate.of(2026, 1, 10), new BigDecimal("1000.00"), "SHIPMENT_ITEM", 101L, "SH001", "产品A");
        CustomerAgingCalculator.ArEntry entry2 = new CustomerAgingCalculator.ArEntry(
                2L, LocalDate.of(2026, 2, 10), new BigDecimal("2000.00"), "SHIPMENT_ITEM", 102L, "SH002", "产品B");
        CustomerAgingCalculator.ArEntry entry3 = new CustomerAgingCalculator.ArEntry(
                3L, LocalDate.of(2026, 3, 10), new BigDecimal("1500.00"), "SHIPMENT_ITEM", 103L, "SH003", "产品C");

        // 3 笔减少分录，总计 1800
        CustomerAgingCalculator.CreditReduction red1 = new CustomerAgingCalculator.CreditReduction(
                11L, LocalDate.of(2026, 1, 20), "RECEIPT_WRITE_OFF", new BigDecimal("600.00"));
        CustomerAgingCalculator.CreditReduction red2 = new CustomerAgingCalculator.CreditReduction(
                12L, LocalDate.of(2026, 2, 20), "ADVANCE_WRITE_OFF", new BigDecimal("800.00"));
        CustomerAgingCalculator.CreditReduction red3 = new CustomerAgingCalculator.CreditReduction(
                13L, LocalDate.of(2026, 3, 20), "AR_RETURN_REVERSAL", new BigDecimal("400.00"));

        CustomerAgingCalculator.AgingResult result = CustomerAgingCalculator.calculate(
                100L,
                "测试客户",
                null, // creditDays 为空
                queryDate,
                List.of(entry1, entry2, entry3),
                List.of(red1, red2, red3),
                BigDecimal.ZERO
        );

        // 合计未核销应收 = 4500 - 1800 = 2700
        assertEquals(new BigDecimal("2700.00"), result.getTotalArBalance());

        // 验证下钻明细：Entry 1 完全消耗(0)；Entry 2 消耗 800 剩余 1200；Entry 3 消耗 0 剩余 1500
        List<CustomerAgingCalculator.AgingDetailItem> items = result.getDetailItems();
        assertEquals(2, items.size()); // 剩余金额 > 0 的明细项

        CustomerAgingCalculator.AgingDetailItem item1 = items.get(0);
        assertEquals(2L, item1.getLedgerEntryId());
        assertEquals(new BigDecimal("2000.00"), item1.getOriginalAmount());
        assertEquals(new BigDecimal("800.00"), item1.getWrittenOffAmount());
        assertEquals(new BigDecimal("1200.00"), item1.getRemainingAmount());
        assertEquals(LocalDate.of(2026, 2, 10), item1.getBizDate());
        assertEquals(50, item1.getAgingDays()); // 2026-02-10 到 2026-04-01 为 50 天
        assertEquals("31-60", item1.getBucket());

        CustomerAgingCalculator.AgingDetailItem item2 = items.get(1);
        assertEquals(3L, item2.getLedgerEntryId());
        assertEquals(new BigDecimal("1500.00"), item2.getOriginalAmount());
        assertEquals(new BigDecimal("0.00"), item2.getWrittenOffAmount());
        assertEquals(new BigDecimal("1500.00"), item2.getRemainingAmount());
        assertEquals(LocalDate.of(2026, 3, 10), item2.getBizDate());
        assertEquals(22, item2.getAgingDays()); // 2026-03-10 到 2026-04-01 为 22 天
        assertEquals("0-30", item2.getBucket());

        // 五桶汇总验证
        assertEquals(new BigDecimal("1500.00"), result.getBucket0To30());
        assertEquals(new BigDecimal("1200.00"), result.getBucket31To60());
        assertEquals(BigDecimal.ZERO.setScale(2), result.getBucket61To90());
        assertEquals(BigDecimal.ZERO.setScale(2), result.getBucket91To180());
        assertEquals(BigDecimal.ZERO.setScale(2), result.getBucket181Plus());
    }

    @Test
    @DisplayName("五桶边界值测试：30 / 31 / 60 / 61 / 90 / 91 / 180 / 181 边界归属精准验证")
    public void testFiveBuckets_BoundaryValues() {
        LocalDate queryDate = LocalDate.of(2026, 7, 1);

        // 8 个边界发货日期
        CustomerAgingCalculator.ArEntry e30 = new CustomerAgingCalculator.ArEntry(
                1L, queryDate.minusDays(30), new BigDecimal("10.00"), "SHIPMENT_ITEM", 1L, "SH1", "P1");
        CustomerAgingCalculator.ArEntry e31 = new CustomerAgingCalculator.ArEntry(
                2L, queryDate.minusDays(31), new BigDecimal("20.00"), "SHIPMENT_ITEM", 2L, "SH2", "P2");
        CustomerAgingCalculator.ArEntry e60 = new CustomerAgingCalculator.ArEntry(
                3L, queryDate.minusDays(60), new BigDecimal("40.00"), "SHIPMENT_ITEM", 3L, "SH3", "P3");
        CustomerAgingCalculator.ArEntry e61 = new CustomerAgingCalculator.ArEntry(
                4L, queryDate.minusDays(61), new BigDecimal("80.00"), "SHIPMENT_ITEM", 4L, "SH4", "P4");
        CustomerAgingCalculator.ArEntry e90 = new CustomerAgingCalculator.ArEntry(
                5L, queryDate.minusDays(90), new BigDecimal("160.00"), "SHIPMENT_ITEM", 5L, "SH5", "P5");
        CustomerAgingCalculator.ArEntry e91 = new CustomerAgingCalculator.ArEntry(
                6L, queryDate.minusDays(91), new BigDecimal("320.00"), "SHIPMENT_ITEM", 6L, "SH6", "P6");
        CustomerAgingCalculator.ArEntry e180 = new CustomerAgingCalculator.ArEntry(
                7L, queryDate.minusDays(180), new BigDecimal("640.00"), "SHIPMENT_ITEM", 7L, "SH7", "P7");
        CustomerAgingCalculator.ArEntry e181 = new CustomerAgingCalculator.ArEntry(
                8L, queryDate.minusDays(181), new BigDecimal("1280.00"), "SHIPMENT_ITEM", 8L, "SH8", "P8");

        CustomerAgingCalculator.AgingResult result = CustomerAgingCalculator.calculate(
                100L, "测试客户", null, queryDate,
                List.of(e181, e180, e91, e90, e61, e60, e31, e30),
                List.of(), // 无冲减
                BigDecimal.ZERO
        );

        // 0-30 桶：仅 e30 (10.00)
        assertEquals(new BigDecimal("10.00"), result.getBucket0To30());
        // 31-60 桶：e31 (20.00) + e60 (40.00) = 60.00
        assertEquals(new BigDecimal("60.00"), result.getBucket31To60());
        // 61-90 桶：e61 (80.00) + e90 (160.00) = 240.00
        assertEquals(new BigDecimal("240.00"), result.getBucket61To90());
        // 91-180 桶：e91 (320.00) + e180 (640.00) = 960.00
        assertEquals(new BigDecimal("960.00"), result.getBucket91To180());
        // 181+ 桶：e181 (1280.00)
        assertEquals(new BigDecimal("1280.00"), result.getBucket181Plus());

        // 合计
        assertEquals(new BigDecimal("2550.00"), result.getTotalArBalance());
    }

    @Test
    @DisplayName("逾期判定测试：发货日 + credit_days < 查询日；credit_days 空值不判逾期")
    public void testOverdue_CreditDaysNullAndValues() {
        LocalDate queryDate = LocalDate.of(2026, 7, 1);
        LocalDate shipmentDate = LocalDate.of(2026, 6, 1); // 账龄 30 天

        CustomerAgingCalculator.ArEntry entry = new CustomerAgingCalculator.ArEntry(
                1L, shipmentDate, new BigDecimal("1000.00"), "SHIPMENT_ITEM", 1L, "SH1", "产品");

        // 1. creditDays 为空：不判逾期
        CustomerAgingCalculator.AgingResult rNull = CustomerAgingCalculator.calculate(
                100L, "客户A", null, queryDate, List.of(entry), List.of(), BigDecimal.ZERO);
        assertFalse(rNull.getDetailItems().get(0).getIsOverdue());
        assertEquals(BigDecimal.ZERO.setScale(2), rNull.getOverdueAmount());
        assertNull(rNull.getDetailItems().get(0).getDueDate());

        // 2. creditDays = 60（到期日 2026-07-31 > 2026-07-01）：未逾期
        CustomerAgingCalculator.AgingResult rNotOverdue = CustomerAgingCalculator.calculate(
                100L, "客户A", 60, queryDate, List.of(entry), List.of(), BigDecimal.ZERO);
        assertFalse(rNotOverdue.getDetailItems().get(0).getIsOverdue());
        assertEquals(BigDecimal.ZERO.setScale(2), rNotOverdue.getOverdueAmount());
        assertEquals(LocalDate.of(2026, 7, 31), rNotOverdue.getDetailItems().get(0).getDueDate());
        assertEquals(0, rNotOverdue.getDetailItems().get(0).getOverdueDays());

        // 3. creditDays = 30（到期日 2026-07-01 == 2026-07-01）：当天不算逾期（严格 < 查询日）
        CustomerAgingCalculator.AgingResult rDueToday = CustomerAgingCalculator.calculate(
                100L, "客户A", 30, queryDate, List.of(entry), List.of(), BigDecimal.ZERO);
        assertFalse(rDueToday.getDetailItems().get(0).getIsOverdue());
        assertEquals(BigDecimal.ZERO.setScale(2), rDueToday.getOverdueAmount());
        assertEquals(0, rDueToday.getDetailItems().get(0).getOverdueDays());

        // 4. creditDays = 29（到期日 2026-06-30 < 2026-07-01）：已逾期 1 天
        CustomerAgingCalculator.AgingResult rOverdue = CustomerAgingCalculator.calculate(
                100L, "客户A", 29, queryDate, List.of(entry), List.of(), BigDecimal.ZERO);
        assertTrue(rOverdue.getDetailItems().get(0).getIsOverdue());
        assertEquals(new BigDecimal("1000.00"), rOverdue.getOverdueAmount());
        assertEquals(LocalDate.of(2026, 6, 30), rOverdue.getDetailItems().get(0).getDueDate());
        assertEquals(1, rOverdue.getDetailItems().get(0).getOverdueDays());
    }

    @Test
    @DisplayName("预收余额单列不进桶测试：预收余额独立展示，不从五桶及应收合计中扣减")
    public void testAdvanceBalance_IsolatedNotEnteringBuckets() {
        LocalDate queryDate = LocalDate.of(2026, 7, 1);

        CustomerAgingCalculator.ArEntry entry = new CustomerAgingCalculator.ArEntry(
                1L, queryDate.minusDays(15), new BigDecimal("1000.00"), "SHIPMENT_ITEM", 1L, "SH1", "产品");

        BigDecimal advanceBalance = new BigDecimal("3000.00");

        CustomerAgingCalculator.AgingResult result = CustomerAgingCalculator.calculate(
                100L, "客户A", 30, queryDate, List.of(entry), List.of(), advanceBalance);

        // 五桶合计应收仍为 1000.00
        assertEquals(new BigDecimal("1000.00"), result.getTotalArBalance());
        assertEquals(new BigDecimal("1000.00"), result.getBucket0To30());
        // 预收单列展示
        assertEquals(new BigDecimal("3000.00"), result.getAdvanceBalance());
    }

    @Test
    @DisplayName("发货红冲净额前置测试：AR_INC 有 AR_INC_REVERSAL 时以净额参与 FIFO")
    public void testArIncReversal_NettedBeforeFifo() {
        LocalDate queryDate = LocalDate.of(2026, 7, 1);

        // Entry 1 发生额 1000，但红冲了 1000 -> 净额为 0，完全排除
        CustomerAgingCalculator.ArEntry entry1 = new CustomerAgingCalculator.ArEntry(
                1L, queryDate.minusDays(40), new BigDecimal("1000.00"), "SHIPMENT_ITEM", 1L, "SH1", "产品1",
                new BigDecimal("1000.00"));

        // Entry 2 发生额 2000，红冲了 500 -> 净额 1500
        CustomerAgingCalculator.ArEntry entry2 = new CustomerAgingCalculator.ArEntry(
                2L, queryDate.minusDays(20), new BigDecimal("2000.00"), "SHIPMENT_ITEM", 2L, "SH2", "产品2",
                new BigDecimal("500.00"));

        CustomerAgingCalculator.AgingResult result = CustomerAgingCalculator.calculate(
                100L, "客户A", null, queryDate, List.of(entry1, entry2), List.of(), BigDecimal.ZERO);

        assertEquals(new BigDecimal("1500.00"), result.getTotalArBalance());
        assertEquals(new BigDecimal("1500.00"), result.getBucket0To30());
        assertEquals(BigDecimal.ZERO.setScale(2), result.getBucket31To60());
        assertEquals(1, result.getDetailItems().size());
        CustomerAgingCalculator.AgingDetailItem detailItem = result.getDetailItems().get(0);
        assertEquals(2L, detailItem.getLedgerEntryId());
        assertEquals(new BigDecimal("2000.00"), detailItem.getOriginalAmount());
        // writtenOffAmount 包含红冲额 500.00，满足 originalAmount - writtenOffAmount == remainingAmount 勾稽关系
        assertEquals(new BigDecimal("500.00"), detailItem.getWrittenOffAmount());
        assertEquals(new BigDecimal("1500.00"), detailItem.getRemainingAmount());
        assertEquals(detailItem.getOriginalAmount().subtract(detailItem.getWrittenOffAmount()), detailItem.getRemainingAmount());
    }
}
