package com.luna.lunasrv.module.lunaerp.service.finance.invoice;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static com.luna.lunasrv.module.lunaerp.enums.finance.FinanceErrorCodeConstants.INVOICE_TAX_RATE_INVALID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link InvoiceTaxCalculator} 纯函数税额反算测试 (TDD Red -> Green)
 *
 * @author luna
 */
public class InvoiceTaxCalculatorTest {

    @Test
    @DisplayName("标准税率 13% 反算：113.00 价税合计 -> 100.00 不含税 + 13.00 税额")
    public void testCalculate_thirteenPercent() {
        BigDecimal totalInclTax = new BigDecimal("113.00");
        BigDecimal taxRate = new BigDecimal("13.00");

        InvoiceTaxCalculator.TaxCalculationResult result = InvoiceTaxCalculator.calculate(totalInclTax, taxRate);

        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.getTotalAmountExclTax());
        assertEquals(new BigDecimal("13.00"), result.getTotalTax());
        assertEquals(new BigDecimal("113.00"), result.getTotalAmountInclTax());
        assertEquals(new BigDecimal("13.00"), result.getTaxRate());
    }

    @Test
    @DisplayName("标准税率 13% 舍入反算：100.00 价税合计 -> 88.50 不含税 + 11.50 税额，拆分恒等式保证")
    public void testCalculate_roundingConsistency() {
        BigDecimal totalInclTax = new BigDecimal("100.00");
        BigDecimal taxRate = new BigDecimal("13.00");

        InvoiceTaxCalculator.TaxCalculationResult result = InvoiceTaxCalculator.calculate(totalInclTax, taxRate);

        // 100 / 1.13 = 88.495575... -> 88.50
        assertEquals(new BigDecimal("88.50"), result.getTotalAmountExclTax());
        // 100 - 88.50 = 11.50
        assertEquals(new BigDecimal("11.50"), result.getTotalTax());
        assertEquals(totalInclTax, result.getTotalAmountExclTax().add(result.getTotalTax()));
    }

    @ParameterizedTest(name = "税率 {0}% 反算测试")
    @CsvSource({
            "9.00, 109.00, 100.00, 9.00",
            "6.00, 106.00, 100.00, 6.00",
            "3.00, 103.00, 100.00, 3.00",
            "1.00, 101.00, 100.00, 1.00",
            "0.00, 100.00, 100.00, 0.00"
    })
    @DisplayName("多档税率验证 (9%/6%/3%/1%/0%)")
    public void testCalculate_variousRates(String rateStr, String inclTaxStr, String expectedExclTaxStr, String expectedTaxStr) {
        BigDecimal taxRate = new BigDecimal(rateStr);
        BigDecimal totalInclTax = new BigDecimal(inclTaxStr);

        InvoiceTaxCalculator.TaxCalculationResult result = InvoiceTaxCalculator.calculate(totalInclTax, taxRate);

        assertEquals(new BigDecimal(expectedExclTaxStr), result.getTotalAmountExclTax());
        assertEquals(new BigDecimal(expectedTaxStr), result.getTotalTax());
        assertEquals(totalInclTax, result.getTotalAmountExclTax().add(result.getTotalTax()));
    }

    @Test
    @DisplayName("税率解析支持 EXEMPT 与免税")
    public void testParseTaxRate_exempt() {
        BigDecimal rateExempt = InvoiceTaxCalculator.parseTaxRate("EXEMPT");
        assertEquals(BigDecimal.ZERO.setScale(2), rateExempt);

        BigDecimal rateZero = InvoiceTaxCalculator.parseTaxRate("0.00");
        assertEquals(new BigDecimal("0.00"), rateZero);

        BigDecimal rateThirteen = InvoiceTaxCalculator.parseTaxRate("13.00");
        assertEquals(new BigDecimal("13.00"), rateThirteen);
    }

    @Test
    @DisplayName("非法税率校验：负数税率抛出 INVOICE_TAX_RATE_INVALID")
    public void testCalculate_negativeTaxRate_throwException() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                InvoiceTaxCalculator.calculate(new BigDecimal("100.00"), new BigDecimal("-1.00")));
        assertEquals(INVOICE_TAX_RATE_INVALID.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("非法税率校验：超过 100% 抛出 INVOICE_TAX_RATE_INVALID")
    public void testCalculate_exceedHundredPercent_throwException() {
        ServiceException ex = assertThrows(ServiceException.class, () ->
                InvoiceTaxCalculator.calculate(new BigDecimal("100.00"), new BigDecimal("101.00")));
        assertEquals(INVOICE_TAX_RATE_INVALID.getCode(), ex.getCode());
    }
}
