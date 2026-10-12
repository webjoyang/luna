package com.luna.lunasrv.module.lunaerp.service.finance.customerrecon.aging;

import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.customerrecon.vo.CustomerAgingSummaryRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.ledger.LedgerEntryDO;
import com.luna.lunasrv.module.lunaerp.enums.finance.FinanceLedgerCategoryEnum;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CustomerAgingCalculator} FIFO 账龄纯函数单元测试
 *
 * <p>涵盖：</p>
 * <ul>
 *   <li>FIFO 消耗顺序（跨月多笔收款/预收冲减/退货冲减，逐笔消耗最老 AR_INC）</li>
 *   <li>五桶边界值精准断言（30/31/60/61/90/91/180/181）</li>
 *   <li>credit_days 逾期判定（逾期判定成立与空值不判逾期）</li>
 *   <li>预收余额单列不进桶断言</li>
 *   <li>红冲分录对冲断言</li>
 * </ul>
 */
@DisplayName("CustomerAgingCalculator FIFO 账龄纯函数计算测试")
public class CustomerAgingCalculatorTest {

    private CustomerDO createCustomer(Long id, String name, Integer creditDays) {
        CustomerDO c = new CustomerDO();
        c.setId(id);
        c.setCompanyName(name);
        c.setCreditDays(creditDays);
        return c;
    }

    private LedgerEntryDO createEntry(Long id, Long customerId, String category, BigDecimal amount, LocalDate bizDate) {
        return LedgerEntryDO.builder()
                .id(id)
                .partyType("CUSTOMER")
                .partyId(customerId)
                .category(category)
                .originalAmount(amount)
                .currency("CNY")
                .bizDate(bizDate)
                .remark("测试分录" + id)
                .build();
    }

    @Test
    @DisplayName("FIFO 消耗顺序：跨月多笔收款/冲减从最老 AR_INC 逐笔消耗")
    void testFifoConsumptionOrder() {
        CustomerDO customer = createCustomer(1001L, "测试客户A", 30);
        LocalDate agingDate = LocalDate.of(2026, 10, 31);

        // 3 笔跨月发货应收：
        // AR1: 2026-07-01, 1000.00
        // AR2: 2026-08-01, 2000.00
        // AR3: 2026-09-01, 3000.00
        LedgerEntryDO ar1 = createEntry(1L, 1001L, FinanceLedgerCategoryEnum.AR_INC.getValue(), new BigDecimal("1000.00"), LocalDate.of(2026, 7, 1));
        LedgerEntryDO ar2 = createEntry(2L, 1001L, FinanceLedgerCategoryEnum.AR_INC.getValue(), new BigDecimal("2000.00"), LocalDate.of(2026, 8, 1));
        LedgerEntryDO ar3 = createEntry(3L, 1001L, FinanceLedgerCategoryEnum.AR_INC.getValue(), new BigDecimal("3000.00"), LocalDate.of(2026, 9, 1));

        // 3 笔应收减少分录（收款核销 500、预收转核销 1000、退货冲减 500，合计减少 2000）：
        // 应当先消耗完 AR1（1000），再消耗 AR2（1000），AR2 剩余 1000，AR3 剩余 3000
        LedgerEntryDO r1 = createEntry(101L, 1001L, FinanceLedgerCategoryEnum.RECEIPT_WRITE_OFF.getValue(), new BigDecimal("500.00"), LocalDate.of(2026, 8, 15));
        LedgerEntryDO r2 = createEntry(102L, 1001L, FinanceLedgerCategoryEnum.ADVANCE_WRITE_OFF.getValue(), new BigDecimal("1000.00"), LocalDate.of(2026, 9, 10));
        LedgerEntryDO r3 = createEntry(103L, 1001L, FinanceLedgerCategoryEnum.AR_RETURN_REVERSAL.getValue(), new BigDecimal("500.00"), LocalDate.of(2026, 9, 20));

        List<LedgerEntryDO> entries = List.of(ar1, ar2, ar3, r1, r2, r3);

        CustomerAgingCalculator.AgingCalculationResult result = CustomerAgingCalculator.calculate(
                customer, "CNY", agingDate, entries, BigDecimal.ZERO, null);

        CustomerAgingSummaryRespVO summary = result.getSummary();
        List<CustomerAgingDetailRespVO> details = result.getDetails();

        assertNotNull(summary);
        // 总未核销应收 = 6000 - 2000 = 4000
        assertEquals(new BigDecimal("4000.00"), summary.getTotalBalance());

        // 明细中 AR1 被全额消耗，只剩 AR2(剩余1000) 和 AR3(剩余3000)
        assertEquals(2, details.size());
        assertEquals(2L, details.get(0).getLedgerEntryId());
        assertEquals(new BigDecimal("1000.00"), details.get(0).getUnsettledAmount());
        assertEquals(new BigDecimal("1000.00"), details.get(0).getSettledAmount());

        assertEquals(3L, details.get(1).getLedgerEntryId());
        assertEquals(new BigDecimal("3000.00"), details.get(1).getUnsettledAmount());
        assertEquals(new BigDecimal("0.00"), details.get(1).getSettledAmount());
    }

    @Test
    @DisplayName("五桶边界值精准校验：30/31/60/61/90/91/180/181 天")
    void testFiveBucketBoundaries() {
        CustomerDO customer = createCustomer(1001L, "边界测试客户", null);
        LocalDate agingDate = LocalDate.of(2026, 10, 31);

        List<LedgerEntryDO> entries = new ArrayList<>();
        // 构造刚好处于 30, 31, 60, 61, 90, 91, 180, 181 天的 AR_INC 分录
        // 30天: 2026-10-01 (10-31 - 10-01 = 30) -> 0-30桶
        entries.add(createEntry(1L, 1001L, "AR_INC", new BigDecimal("10.00"), agingDate.minusDays(30)));
        // 31天: 2026-09-30 (10-31 - 09-30 = 31) -> 31-60桶
        entries.add(createEntry(2L, 1001L, "AR_INC", new BigDecimal("20.00"), agingDate.minusDays(31)));
        // 60天: 2026-09-01 (10-31 - 09-01 = 60) -> 31-60桶
        entries.add(createEntry(3L, 1001L, "AR_INC", new BigDecimal("30.00"), agingDate.minusDays(60)));
        // 61天: 2026-08-31 (10-31 - 08-31 = 61) -> 61-90桶
        entries.add(createEntry(4L, 1001L, "AR_INC", new BigDecimal("40.00"), agingDate.minusDays(61)));
        // 90天: 2026-08-02 (10-31 - 08-02 = 90) -> 61-90桶
        entries.add(createEntry(5L, 1001L, "AR_INC", new BigDecimal("50.00"), agingDate.minusDays(90)));
        // 91天: 2026-08-01 (10-31 - 08-01 = 91) -> 91-180桶
        entries.add(createEntry(6L, 1001L, "AR_INC", new BigDecimal("60.00"), agingDate.minusDays(91)));
        // 180天: 2026-05-04 (10-31 - 05-04 = 180) -> 91-180桶
        entries.add(createEntry(7L, 1001L, "AR_INC", new BigDecimal("70.00"), agingDate.minusDays(180)));
        // 181天: 2026-05-03 (10-31 - 05-03 = 181) -> 181+桶
        entries.add(createEntry(8L, 1001L, "AR_INC", new BigDecimal("80.00"), agingDate.minusDays(181)));

        CustomerAgingCalculator.AgingCalculationResult result = CustomerAgingCalculator.calculate(
                customer, "CNY", agingDate, entries, BigDecimal.ZERO, null);

        CustomerAgingSummaryRespVO summary = result.getSummary();
        assertNotNull(summary);

        // 验证 0-30 桶：仅 30天 对应 10.00
        assertEquals(new BigDecimal("10.00"), summary.getBucket0To30());

        // 验证 31-60 桶：31天(20.00) + 60天(30.00) = 50.00
        assertEquals(new BigDecimal("50.00"), summary.getBucket31To60());

        // 验证 61-90 桶：61天(40.00) + 90天(50.00) = 90.00
        assertEquals(new BigDecimal("90.00"), summary.getBucket61To90());

        // 验证 91-180 桶：91天(60.00) + 180天(70.00) = 130.00
        assertEquals(new BigDecimal("130.00"), summary.getBucket91To180());

        // 验证 181+ 桶：181天(80.00) = 80.00
        assertEquals(new BigDecimal("80.00"), summary.getBucket181Plus());

        // 总计 = 10 + 50 + 90 + 130 + 80 = 360.00
        assertEquals(new BigDecimal("360.00"), summary.getTotalBalance());
    }

    @Test
    @DisplayName("逾期判定：发货日 + credit_days < 查询日；空值不判逾期")
    void testCreditDaysOverdueLogic() {
        LocalDate agingDate = LocalDate.of(2026, 10, 31);

        // Case 1: creditDays 为 30
        CustomerDO customerWithCredit = createCustomer(1001L, "有账期客户", 30);

        // 发货日 2026-09-15: 到期日 10-15 < 10-31 -> 逾期 16 天，金额 1000
        LedgerEntryDO arOverdue = createEntry(1L, 1001L, "AR_INC", new BigDecimal("1000.00"), LocalDate.of(2026, 9, 15));
        // 发货日 2026-10-15: 到期日 11-14 > 10-31 -> 未逾期，金额 2000
        LedgerEntryDO arNotOverdue = createEntry(2L, 1001L, "AR_INC", new BigDecimal("2000.00"), LocalDate.of(2026, 10, 15));

        CustomerAgingCalculator.AgingCalculationResult res1 = CustomerAgingCalculator.calculate(
                customerWithCredit, "CNY", agingDate, List.of(arOverdue, arNotOverdue), BigDecimal.ZERO, null);

        assertEquals(new BigDecimal("3000.00"), res1.getSummary().getTotalBalance());
        assertEquals(new BigDecimal("1000.00"), res1.getSummary().getOverdueAmount());
        assertTrue(res1.getDetails().get(0).getIsOverdue());
        assertEquals(16L, res1.getDetails().get(0).getOverdueDays());
        assertFalse(res1.getDetails().get(1).getIsOverdue());
        assertEquals(0L, res1.getDetails().get(1).getOverdueDays());

        // Case 2: creditDays 为 null（未设账期）-> 即使 1 年前的单子也不判逾期
        CustomerDO customerNoCredit = createCustomer(1002L, "无账期客户", null);
        LedgerEntryDO arOld = createEntry(3L, 1002L, "AR_INC", new BigDecimal("5000.00"), LocalDate.of(2025, 1, 1));

        CustomerAgingCalculator.AgingCalculationResult res2 = CustomerAgingCalculator.calculate(
                customerNoCredit, "CNY", agingDate, List.of(arOld), BigDecimal.ZERO, null);

        assertEquals(new BigDecimal("5000.00"), res2.getSummary().getTotalBalance());
        assertEquals(new BigDecimal("0.00"), res2.getSummary().getOverdueAmount());
        assertFalse(res2.getDetails().get(0).getIsOverdue());
        assertNull(res2.getDetails().get(0).getDueDate());
    }

    @Test
    @DisplayName("预收余额单列不进桶：预收余额独立展示，五桶仅包含未核销应收")
    void testAdvanceBalanceSeparateColumn() {
        CustomerDO customer = createCustomer(1001L, "预收客户", 30);
        LocalDate agingDate = LocalDate.of(2026, 10, 31);

        // 仅有一笔发货 1000.00（发生在 10 天前，落在 0-30 桶）
        LedgerEntryDO ar = createEntry(1L, 1001L, "AR_INC", new BigDecimal("1000.00"), agingDate.minusDays(10));
        BigDecimal advanceBalance = new BigDecimal("8888.00");

        CustomerAgingCalculator.AgingCalculationResult result = CustomerAgingCalculator.calculate(
                customer, "CNY", agingDate, List.of(ar), advanceBalance, null);

        CustomerAgingSummaryRespVO summary = result.getSummary();
        assertNotNull(summary);

        // 应收合计与 0-30 桶仅为 1000.00
        assertEquals(new BigDecimal("1000.00"), summary.getTotalBalance());
        assertEquals(new BigDecimal("1000.00"), summary.getBucket0To30());
        assertEquals(new BigDecimal("0.00"), summary.getBucket31To60());

        // 预收余额单列 8888.00，不进入五桶
        assertEquals(new BigDecimal("8888.00"), summary.getAdvanceBalance());
    }

    @Test
    @DisplayName("发货红冲对冲：AR_INC_REVERSAL 关联原单时直接扣减原单发生额")
    void testArIncReversalNetted() {
        CustomerDO customer = createCustomer(1001L, "红冲客户", 30);
        LocalDate agingDate = LocalDate.of(2026, 10, 31);

        LedgerEntryDO ar1 = createEntry(1L, 1001L, "AR_INC", new BigDecimal("5000.00"), LocalDate.of(2026, 10, 1));
        // 红冲 2000
        LedgerEntryDO arRev = createEntry(2L, 1001L, "AR_INC_REVERSAL", new BigDecimal("2000.00"), LocalDate.of(2026, 10, 5));
        arRev.setReversalOfId(1L);

        CustomerAgingCalculator.AgingCalculationResult result = CustomerAgingCalculator.calculate(
                customer, "CNY", agingDate, List.of(ar1, arRev), BigDecimal.ZERO, null);

        assertEquals(new BigDecimal("3000.00"), result.getSummary().getTotalBalance());
        assertEquals(1, result.getDetails().size());
        assertEquals(new BigDecimal("3000.00"), result.getDetails().get(0).getUnsettledAmount());
    }

}
