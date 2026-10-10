package com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.COST_CARD_KG_METER_CONVERSION_MISSING;
import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.QUOTATION_MARGIN_RATE_INVALID;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CostCardPricingCalculator} 纯函数单元测试
 *
 * 覆盖：
 * 1. 针织模式（basis=KG）：公式、投入损耗、超100%损耗钳制、负损耗钳制、全零/缺价/空列表边界（对齐历史 TS product-yarn-cost.spec.ts）
 * 2. 梭织简易模式（basis=METER）：每米重量、每公斤米长、混纺单价与损耗
 * 3. 梭织经纬精算（basis=METER）：经纬分组每米用量精算、投入损耗
 * 4. 双口径费用行归一与跨口径换算（PER_KG / PER_METER 互转、克重门幅缺失 1_030_034_005 报错）
 * 5. 税率计算（不含税与含税）
 * 6. 报价单折币、毛利正反算、整单加权毛利计算与边界校验
 *
 * @author luna
 */
public class CostCardPricingCalculatorTest {

    private static BigDecimal bd(String val) {
        return val != null ? new BigDecimal(val) : null;
    }

    private static BigDecimal bd(double val) {
        return BigDecimal.valueOf(val);
    }

    // ==================== 1. 针织模式测试（对齐 TS 用例） ====================

    @Nested
    @DisplayName("针织模式（basis=KG）")
    class KnitModeTests {

        @Test
        @DisplayName("空数组：全部输出 0，basis=KG")
        void testEmptyYarnList() {
            CostCardPricingResult res = CostCardPricingCalculator.calculateLegacyCompatible(
                    Collections.emptyList(), "KNIT", "SIMPLE",
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
            assertEquals("KG", res.getBasis());
            assertEquals(new BigDecimal("0.0000"), res.getRatioTotal());
            assertEquals(0, res.getMissingPriceCount());
            assertEquals(new BigDecimal("0.0000"), res.getYarnCostPerKg());
            assertEquals(new BigDecimal("0.0000"), res.getYarnCostPerMeter());
            assertEquals(new BigDecimal("0.0000"), res.getFeeTotalPerKg());
            assertEquals(new BigDecimal("0.0000"), res.getUnitCostExclTaxPerKg());
            assertEquals(new BigDecimal("0.0000"), res.getKgPerMeter());
            assertEquals(new BigDecimal("0.0000"), res.getMetersPerKg());
        }

        @Test
        @DisplayName("全零值：单价/比例/损耗率/加工费均为 0 时成本为 0，单价为 0 计入 missingPriceCount")
        void testAllZeroValues() {
            List<YarnPricingRow> yarns = Collections.singletonList(
                    YarnPricingRow.builder()
                            .yarnSpecName("40S棉")
                            .price(BigDecimal.ZERO)
                            .ratio(BigDecimal.ZERO)
                            .wasteRate(BigDecimal.ZERO)
                            .build()
            );
            CostCardPricingResult res = CostCardPricingCalculator.calculateLegacyCompatible(
                    yarns, "KNIT", "SIMPLE",
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
            assertEquals(new BigDecimal("0.0000"), res.getRatioTotal());
            assertEquals(new BigDecimal("0.0000"), res.getYarnCostPerKg());
            assertEquals(new BigDecimal("0.0000"), res.getUnitCostExclTaxPerKg());
            assertEquals(1, res.getMissingPriceCount());
        }

        @Test
        @DisplayName("多纱线含损耗：20×0.6/0.95 + 10×0.4 = 16.6316，加加工费 2.5 = 19.1316")
        void testMultipleYarnsWithLoss() {
            List<YarnPricingRow> yarns = Arrays.asList(
                    YarnPricingRow.builder().yarnSpecName("40S棉").price(bd("20")).ratio(bd("60")).wasteRate(bd("5")).build(),
                    YarnPricingRow.builder().yarnSpecName("100D涤").price(bd("10")).ratio(bd("40")).wasteRate(bd("0")).build()
            );
            CostCardPricingResult res = CostCardPricingCalculator.calculateLegacyCompatible(
                    yarns, "KNIT", "SIMPLE",
                    BigDecimal.ZERO, BigDecimal.ZERO, bd("2.5"), BigDecimal.ZERO, BigDecimal.ZERO
            );
            assertEquals("KG", res.getBasis());
            assertEquals(new BigDecimal("100.0000"), res.getRatioTotal());
            assertEquals(0, res.getMissingPriceCount());
            assertEquals(new BigDecimal("16.6316"), res.getYarnCostPerKg());
            assertEquals(new BigDecimal("2.5000"), res.getFeeTotalPerKg());
            assertEquals(new BigDecimal("19.1316"), res.getUnitCostExclTaxPerKg());
            assertEquals(new BigDecimal("0.0000"), res.getYarnCostPerMeter());
        }

        @Test
        @DisplayName("标准成本卡针织模式：未填写克重门幅时，米口径成本为 null")
        void testStandardCostCardKnitMissingKgPerMeterIsNull() {
            CostCardPricingParam param = CostCardPricingParam.builder()
                    .fabricConstruction("KNIT")
                    .yarnRows(Collections.singletonList(
                            YarnPricingRow.builder().yarnSpecName("40S棉").price(bd("20")).ratio(bd("100")).build()
                    ))
                    .build();
            CostCardPricingResult res = CostCardPricingCalculator.calculateCostCard(param);
            assertEquals(new BigDecimal("20.0000"), res.getYarnCostPerKg());
            assertNull(res.getYarnCostPerMeter());
            assertNull(res.getUnitCostExclTaxPerMeter());
        }

        @Test
        @DisplayName("无价格统计：missingPriceCount 正确统计 price <= 0 或 null")
        void testMissingPriceCount() {
            List<YarnPricingRow> yarns = Arrays.asList(
                    YarnPricingRow.builder().yarnSpecName("40S棉").price(bd("20")).ratio(bd("50")).build(),
                    YarnPricingRow.builder().yarnSpecName("100D涤").ratio(bd("30")).build(),
                    YarnPricingRow.builder().yarnSpecName("32S人棉").price(BigDecimal.ZERO).ratio(bd("20")).build()
            );
            CostCardPricingResult res = CostCardPricingCalculator.calculateLegacyCompatible(
                    yarns, "KNIT", "SIMPLE",
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
            assertEquals(2, res.getMissingPriceCount());
            assertEquals(new BigDecimal("100.0000"), res.getRatioTotal());
            // 20 × 0.5 = 10
            assertEquals(new BigDecimal("10.0000"), res.getYarnCostPerKg());
        }

        @Test
        @DisplayName("损耗率超 100% 钳制为 99.9%：10 / (1 - 0.999) = 10000")
        void testWasteRateClampedOver100() {
            List<YarnPricingRow> yarns = Collections.singletonList(
                    YarnPricingRow.builder().yarnSpecName("40S棉").price(bd("10")).ratio(bd("100")).wasteRate(bd("150")).build()
            );
            CostCardPricingResult res = CostCardPricingCalculator.calculateLegacyCompatible(
                    yarns, "KNIT", "SIMPLE",
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
            assertEquals(new BigDecimal("10000.0000"), res.getYarnCostPerKg());
        }

        @Test
        @DisplayName("负损耗率钳制为 0")
        void testNegativeWasteRateClampedToZero() {
            List<YarnPricingRow> yarns = Collections.singletonList(
                    YarnPricingRow.builder().yarnSpecName("40S棉").price(bd("20")).ratio(bd("100")).wasteRate(bd("-20")).build()
            );
            CostCardPricingResult res = CostCardPricingCalculator.calculateLegacyCompatible(
                    yarns, "KNIT", "SIMPLE",
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
            assertEquals(new BigDecimal("20.0000"), res.getYarnCostPerKg());
        }

        @Test
        @DisplayName("未填规格名的空行不参与计算")
        void testBlankSpecNameIgnored() {
            List<YarnPricingRow> yarns = Arrays.asList(
                    YarnPricingRow.builder().yarnSpecName("40S棉").price(bd("20")).ratio(bd("100")).build(),
                    YarnPricingRow.builder().yarnSpecName("   ").price(bd("99")).ratio(bd("50")).build()
            );
            CostCardPricingResult res = CostCardPricingCalculator.calculateLegacyCompatible(
                    yarns, "KNIT", "SIMPLE",
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
            assertEquals(new BigDecimal("100.0000"), res.getRatioTotal());
            assertEquals(0, res.getMissingPriceCount());
            assertEquals(new BigDecimal("20.0000"), res.getYarnCostPerKg());
        }
    }

    // ==================== 2. 梭织简易估算测试（对齐 TS 用例） ====================

    @Nested
    @DisplayName("梭织简易估算（basis=METER）")
    class WovenSimpleModeTests {

        @Test
        @DisplayName("每米重量=克重×门幅/100000，每米纱线成本=每米重量×混纺单价")
        void testWovenSimpleBasics() {
            List<YarnPricingRow> yarns = Arrays.asList(
                    YarnPricingRow.builder().yarnSpecName("40S棉").price(bd("20")).ratio(bd("60")).build(),
                    YarnPricingRow.builder().yarnSpecName("100D涤").price(bd("10")).ratio(bd("40")).build()
            );
            CostCardPricingResult res = CostCardPricingCalculator.calculateLegacyCompatible(
                    yarns, "WOVEN", "SIMPLE",
                    bd("150"), bd("160"), BigDecimal.ZERO, bd("1.5"), bd("0.3")
            );
            assertEquals("METER", res.getBasis());
            // 150 × 160 / 100000 = 0.2400
            assertEquals(new BigDecimal("0.2400"), res.getKgPerMeter());
            assertEquals(new BigDecimal("4.1667"), res.getMetersPerKg());
            // 混纺单价 20×0.6 + 10×0.4 = 16.0000，每米纱线成本 = 0.24 × 16 = 3.8400
            assertEquals(new BigDecimal("3.8400"), res.getYarnCostPerMeter());
            assertEquals(new BigDecimal("16.0000"), res.getYarnCostPerKg());
            assertEquals(new BigDecimal("1.8000"), res.getFeeTotalPerMeter());
            assertEquals(new BigDecimal("5.6400"), res.getUnitCostExclTaxPerMeter());
        }

        @Test
        @DisplayName("简易估算含损耗：混纺单价 20/0.95=21.05263158，每米成本 0.15×21.05263158=3.1579")
        void testWovenSimpleWithLoss() {
            List<YarnPricingRow> yarns = Collections.singletonList(
                    YarnPricingRow.builder().yarnSpecName("40S棉").price(bd("20")).ratio(bd("100")).wasteRate(bd("5")).build()
            );
            CostCardPricingResult res = CostCardPricingCalculator.calculateLegacyCompatible(
                    yarns, "WOVEN", "SIMPLE",
                    bd("100"), bd("150"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
            assertEquals(new BigDecimal("0.1500"), res.getKgPerMeter());
            assertEquals(new BigDecimal("3.1579"), res.getYarnCostPerMeter());
        }

        @Test
        @DisplayName("标准卡严格校验：梭织模式缺少克重门幅报 1_030_034_005")
        void testWovenStrictConversionMissing() {
            CostCardPricingParam param = CostCardPricingParam.builder()
                    .fabricConstruction("WOVEN")
                    .wovenCalculationMode("SIMPLE")
                    .greigeWeightGsm(BigDecimal.ZERO)
                    .greigeWidthCm(bd("160"))
                    .yarnRows(Collections.singletonList(
                            YarnPricingRow.builder().yarnSpecName("40S棉").price(bd("20")).ratio(bd("100")).build()
                    ))
                    .build();

            ServiceException ex = assertThrows(ServiceException.class, () -> CostCardPricingCalculator.calculateCostCard(param));
            assertEquals(COST_CARD_KG_METER_CONVERSION_MISSING.getCode(), ex.getCode());
        }
    }

    // ==================== 3. 梭织经纬精算测试（对齐 TS 用例） ====================

    @Nested
    @DisplayName("梭织经纬精算（basis=METER）")
    class WovenProfessionalModeTests {

        @Test
        @DisplayName("经纬精算：按每米用量计算，不使用配比：0.12×20 + 0.06×10 = 3.0000")
        void testWovenProfessionalBasics() {
            List<YarnPricingRow> yarns = Arrays.asList(
                    YarnPricingRow.builder().yarnSpecName("经纱").direction("WARP").price(bd("20")).ratio(bd("60")).consumptionKgPerMeter(bd("0.12")).build(),
                    YarnPricingRow.builder().yarnSpecName("纬纱").direction("WEFT").price(bd("10")).ratio(bd("40")).consumptionKgPerMeter(bd("0.06")).build()
            );
            CostCardPricingResult res = CostCardPricingCalculator.calculateLegacyCompatible(
                    yarns, "WOVEN", "PROFESSIONAL",
                    bd("150"), bd("160"), BigDecimal.ZERO, bd("1.5"), bd("0.3")
            );
            assertEquals("METER", res.getBasis());
            assertEquals(new BigDecimal("3.0000"), res.getYarnCostPerMeter());
            assertEquals(new BigDecimal("1.8000"), res.getFeeTotalPerMeter());
            assertEquals(new BigDecimal("4.8000"), res.getUnitCostExclTaxPerMeter());
            assertEquals(new BigDecimal("0.2400"), res.getKgPerMeter());
            // 3.0000 / 0.24 = 12.5000
            assertEquals(new BigDecimal("12.5000"), res.getYarnCostPerKg());
        }

        @Test
        @DisplayName("经纬精算含损耗：0.1 × 20 / 0.9 = 2.2222")
        void testWovenProfessionalWithLoss() {
            List<YarnPricingRow> yarns = Collections.singletonList(
                    YarnPricingRow.builder().yarnSpecName("经纱").direction("WARP").price(bd("20")).consumptionKgPerMeter(bd("0.1")).wasteRate(bd("10")).build()
            );
            CostCardPricingResult res = CostCardPricingCalculator.calculateLegacyCompatible(
                    yarns, "WOVEN", "PROFESSIONAL",
                    bd("100"), bd("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
            assertEquals(new BigDecimal("2.2222"), res.getYarnCostPerMeter());
        }

        @Test
        @DisplayName("经纬精算超 100% 损耗钳制：1 × 1 / (1 - 0.999) = 1000.0000")
        void testWovenProfessionalWasteRateClamped() {
            List<YarnPricingRow> yarns = Collections.singletonList(
                    YarnPricingRow.builder().yarnSpecName("经纱").direction("WARP").price(bd("1")).consumptionKgPerMeter(bd("1")).wasteRate(bd("999")).build()
            );
            CostCardPricingResult res = CostCardPricingCalculator.calculateLegacyCompatible(
                    yarns, "WOVEN", "PROFESSIONAL",
                    bd("100"), bd("100"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO
            );
            assertEquals(new BigDecimal("1000.0000"), res.getYarnCostPerMeter());
        }
    }

    // ==================== 4. 双口径费用行归一与跨口径换算 ====================

    @Nested
    @DisplayName("双口径费用行归一与跨口径换算")
    class DualGaugeFeeTests {

        @Test
        @DisplayName("针织双口径：克重门幅有效时，PER_KG 与 PER_METER 费用互转完整")
        void testKnitDualGaugeFeesSuccess() {
            CostCardPricingParam param = CostCardPricingParam.builder()
                    .fabricConstruction("KNIT")
                    .greigeWeightGsm(bd("200"))
                    .greigeWidthCm(bd("180"))
                    .taxRate(bd("0.13"))
                    .yarnRows(Collections.singletonList(
                            YarnPricingRow.builder().yarnSpecName("32S棉").price(bd("25")).ratio(bd("100")).wasteRate(bd("5")).build()
                    ))
                    .feeRows(Arrays.asList(
                            // 织造费：3 元/KG，损耗 0%
                            FeePricingRow.builder().feeType("WEAVING").feeUnit("PER_KG").price(bd("3")).lossRate(BigDecimal.ZERO).build(),
                            // 定型费：1.08 元/M，损耗 0% -> 每米重量 0.36，折合 /KG = 1.08 / 0.36 = 3 元/KG
                            FeePricingRow.builder().feeType("SETTING").feeUnit("PER_METER").price(bd("1.08")).lossRate(BigDecimal.ZERO).build()
                    ))
                    .build();

            CostCardPricingResult res = CostCardPricingCalculator.calculateCostCard(param);

            // 每米重量：200 × 180 / 100000 = 0.3600
            assertEquals(new BigDecimal("0.3600"), res.getKgPerMeter());
            // 纱线成本：25 / 0.95 = 26.3158 元/KG，折合 /M = 26.3158 × 0.36 = 9.4737 元/M
            assertEquals(new BigDecimal("26.3158"), res.getYarnCostPerKg());
            assertEquals(new BigDecimal("9.4737"), res.getYarnCostPerMeter());

            // 费用行1 (PER_KG 3)：/KG=3.0000，/M=3×0.36=1.0800
            FeeRowPricingResult f1 = res.getFeeRowResults().get(0);
            assertEquals(new BigDecimal("3.0000"), f1.getContributionPerKg());
            assertEquals(new BigDecimal("1.0800"), f1.getContributionPerMeter());

            // 费用行2 (PER_METER 1.08)：/M=1.0800，/KG=1.08/0.36=3.0000
            FeeRowPricingResult f2 = res.getFeeRowResults().get(1);
            assertEquals(new BigDecimal("3.0000"), f2.getContributionPerKg());
            assertEquals(new BigDecimal("1.0800"), f2.getContributionPerMeter());

            // 费用合计：/KG = 6.0000，/M = 2.1600
            assertEquals(new BigDecimal("6.0000"), res.getFeeTotalPerKg());
            assertEquals(new BigDecimal("2.1600"), res.getFeeTotalPerMeter());

            // 不含税单位成本：/KG = 26.3158 + 6 = 32.3158；/M = 9.4737 + 2.16 = 11.6337
            assertEquals(new BigDecimal("32.3158"), res.getUnitCostExclTaxPerKg());
            assertEquals(new BigDecimal("11.6337"), res.getUnitCostExclTaxPerMeter());

            // 含税单位成本 (13%税率)：32.3158 × 1.13 = 36.5169；11.6337 × 1.13 = 13.1461
            assertEquals(new BigDecimal("36.5169"), res.getUnitCostInclTaxPerKg());
            assertEquals(new BigDecimal("13.1461"), res.getUnitCostInclTaxPerMeter());
        }

        @Test
        @DisplayName("针织模式若含有 PER_METER 费用但缺失克重门幅，报 1_030_034_005")
        void testKnitWithPerMeterFeeMissingConversion() {
            CostCardPricingParam param = CostCardPricingParam.builder()
                    .fabricConstruction("KNIT")
                    .greigeWeightGsm(BigDecimal.ZERO)
                    .greigeWidthCm(BigDecimal.ZERO)
                    .yarnRows(Collections.singletonList(
                            YarnPricingRow.builder().yarnSpecName("32S棉").price(bd("25")).ratio(bd("100")).build()
                    ))
                    .feeRows(Collections.singletonList(
                            FeePricingRow.builder().feeType("SETTING").feeUnit("PER_METER").price(bd("1.5")).build()
                    ))
                    .build();

            ServiceException ex = assertThrows(ServiceException.class, () -> CostCardPricingCalculator.calculateCostCard(param));
            assertEquals(COST_CARD_KG_METER_CONVERSION_MISSING.getCode(), ex.getCode());
        }
    }

    // ==================== 5. 报价单折币与毛利纯函数 ====================

    @Nested
    @DisplayName("报价单折币、毛利正反算、整单加权毛利")
    class QuotationPricingTests {

        @Test
        @DisplayName("外币折算与目标毛利率反算报价：USD 汇率 7.2，RMB成本 72，目标毛利率 20%")
        void testQuotationFxAndMarginReverse() {
            QuotationItemPricingParam param = QuotationItemPricingParam.builder()
                    .costUnitPriceRmb(bd("72.0000"))
                    .exchangeRate(bd("7.200000"))
                    .targetMarginRate(bd("0.200000"))
                    .quantity(bd("1000.00"))
                    .unitPrice(null)
                    .build();

            QuotationItemPricingResult res = CostCardPricingCalculator.calculateQuotationItem(param);

            // 折报价币成本 = 72 / 7.2 = 10.0000 USD
            assertEquals(new BigDecimal("10.0000"), res.getCostUnitPrice());
            // 目标毛利 20% 反算单价 = 10 / (1 - 0.2) = 12.5000 USD
            assertEquals(new BigDecimal("12.5000"), res.getUnitPrice());
            // 行成本额 = 1000 × 10 = 10000.0000
            assertEquals(new BigDecimal("10000.0000"), res.getCostAmount());
            // 行金额 = 1000 × 12.5 = 12500.0000
            assertEquals(new BigDecimal("12500.0000"), res.getAmount());
            // 实际毛利率 = 1 - 10000 / 12500 = 0.200000 (20%)
            assertEquals(new BigDecimal("0.200000"), res.getMarginRate());
        }

        @Test
        @DisplayName("手填单价正算实际毛利率：成本 15，单价 20，毛利率 = 1 - 15/20 = 25%")
        void testQuotationManualPriceDirectMargin() {
            QuotationItemPricingParam param = QuotationItemPricingParam.builder()
                    .costUnitPriceRmb(bd("15.0000"))
                    .exchangeRate(bd("1.000000"))
                    .unitPrice(bd("20.0000"))
                    .quantity(bd("500.00"))
                    .build();

            QuotationItemPricingResult res = CostCardPricingCalculator.calculateQuotationItem(param);
            assertEquals(new BigDecimal("15.0000"), res.getCostUnitPrice());
            assertEquals(new BigDecimal("20.0000"), res.getUnitPrice());
            assertEquals(new BigDecimal("7500.0000"), res.getCostAmount());
            assertEquals(new BigDecimal("10000.0000"), res.getAmount());
            assertEquals(new BigDecimal("0.250000"), res.getMarginRate());
        }

        @Test
        @DisplayName("目标毛利率非法（>=100% 或 <= -100%）抛 1_030_034_106")
        void testInvalidMarginRateThrows() {
            QuotationItemPricingParam p1 = QuotationItemPricingParam.builder()
                    .costUnitPriceRmb(bd("10"))
                    .targetMarginRate(bd("1.000000"))
                    .build();
            ServiceException ex1 = assertThrows(ServiceException.class, () -> CostCardPricingCalculator.calculateQuotationItem(p1));
            assertEquals(QUOTATION_MARGIN_RATE_INVALID.getCode(), ex1.getCode());

            QuotationItemPricingParam p2 = QuotationItemPricingParam.builder()
                    .costUnitPriceRmb(bd("10"))
                    .targetMarginRate(bd("-1.000000"))
                    .build();
            ServiceException ex2 = assertThrows(ServiceException.class, () -> CostCardPricingCalculator.calculateQuotationItem(p2));
            assertEquals(QUOTATION_MARGIN_RATE_INVALID.getCode(), ex2.getCode());
        }

        @Test
        @DisplayName("整单加权毛利率汇总：两行金额加权计算")
        void testQuotationSummaryWeightedAverageMargin() {
            QuotationItemPricingResult item1 = QuotationItemPricingResult.builder()
                    .quantity(bd("1000.00"))
                    .amount(bd("20000.0000"))
                    .costAmount(bd("16000.0000"))
                    .build();
            QuotationItemPricingResult item2 = QuotationItemPricingResult.builder()
                    .quantity(bd("500.00"))
                    .amount(bd("10000.0000"))
                    .costAmount(bd("7000.0000"))
                    .build();

            QuotationSummaryPricingResult summary = CostCardPricingCalculator.calculateQuotationSummary(Arrays.asList(item1, item2));

            // 总数量 = 1500.00
            assertEquals(new BigDecimal("1500.00"), summary.getTotalQuantity());
            // 总金额 = 30000.0000
            assertEquals(new BigDecimal("30000.0000"), summary.getTotalAmount());
            // 总成本额 = 23000.0000
            assertEquals(new BigDecimal("23000.0000"), summary.getTotalCostAmount());
            // 整单加权毛利率 = (30000 - 23000) / 30000 = 7000 / 30000 = 0.233333
            assertEquals(new BigDecimal("0.233333"), summary.getAvgMarginRate());
        }

        @Test
        @DisplayName("总金额为 0 时整单毛利率为 null")
        void testQuotationSummaryZeroAmountNullMargin() {
            QuotationItemPricingResult item = QuotationItemPricingResult.builder()
                    .quantity(bd("0.00"))
                    .amount(bd("0.0000"))
                    .costAmount(bd("0.0000"))
                    .build();

            QuotationSummaryPricingResult summary = CostCardPricingCalculator.calculateQuotationSummary(Collections.singletonList(item));
            assertNull(summary.getAvgMarginRate());
            assertEquals(new BigDecimal("0.0000"), summary.getTotalAmount());
        }
    }
}
