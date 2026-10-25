package com.luna.lunasrv.module.lunaerp.service.business.costcard.pricing;

import com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.COST_CARD_KG_METER_CONVERSION_MISSING;
import static com.luna.lunasrv.module.lunaerp.enums.CostCardQuotationErrorCodeConstants.QUOTATION_MARGIN_RATE_INVALID;

/**
 * 售前链 - 成本卡与报价单核心算价纯函数类（ADR-0039 / 纯无状态工具类）
 *
 * <p>核心特征：
 * 1. 纯计算无状态，不依赖 Spring 上下文或数据库 Mapper。
 * 2. 金额一律使用 {@link BigDecimal}，4 位小数四舍五入 (HALF_UP)。
 * 3. 比率/汇率保留 6 位小数四舍五入。
 * 4. 投入损耗率口径：采购投入毛额 = 净额 ÷ (1 − 损耗率)。
 *    损耗率负值钳制为 0，≥100% 钳制为 99.9%。
 * 5. 覆盖针织 KG / 梭织简易 METER / 梭织经纬精算 METER 三大模式。
 * 6. 双口径费用行归一与跨口径换算：克重/门幅缺失报 1_030_034_005。
 * 7. 报价单外币折算、毛利率正反算、整单加权毛利率汇总。
 *
 * @author luna
 */
public final class CostCardPricingCalculator {

    public static final int MONEY_SCALE = 4;
    public static final int RATE_SCALE = 6;
    public static final int QUANTITY_SCALE = 2;
    public static final int HIGH_SCALE = 12;

    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public static final BigDecimal HUNDRED = new BigDecimal("100");
    public static final BigDecimal THOUSAND_100K = new BigDecimal("100000");
    public static final BigDecimal MAX_LOSS_RATE = new BigDecimal("99.9");

    private CostCardPricingCalculator() {
        // 工具类私有构造
    }

    // ==================== 损耗与单项基础计算 ====================

    /**
     * 投入损耗放大计算：毛额 = 净耗用 ÷ (1 − 损耗率/100)
     *
     * <p>规则：
     * 1. wasteRate <= 0 或 null 时无损耗，返回原值（4位小数）。
     * 2. wasteRate >= 100 时钳制为 99.9%。
     */
    public static BigDecimal applyLoss(BigDecimal netCost, BigDecimal wasteRate) {
        if (netCost == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
        }
        if (wasteRate == null || wasteRate.compareTo(BigDecimal.ZERO) <= 0) {
            return netCost.setScale(MONEY_SCALE, ROUNDING);
        }
        BigDecimal rate = wasteRate.compareTo(MAX_LOSS_RATE) >= 0 ? MAX_LOSS_RATE : wasteRate;
        BigDecimal rateDecimal = rate.divide(HUNDRED, HIGH_SCALE, ROUNDING);
        BigDecimal divisor = BigDecimal.ONE.subtract(rateDecimal);
        return netCost.divide(divisor, MONEY_SCALE, ROUNDING);
    }

    /**
     * 高精度投入损耗放大（用于累加中间值，避免阶跃舍入误差）
     */
    public static BigDecimal applyLossHighPrecision(BigDecimal netCost, BigDecimal wasteRate) {
        if (netCost == null) {
            return BigDecimal.ZERO.setScale(HIGH_SCALE, ROUNDING);
        }
        if (wasteRate == null || wasteRate.compareTo(BigDecimal.ZERO) <= 0) {
            return netCost.setScale(HIGH_SCALE, ROUNDING);
        }
        BigDecimal rate = wasteRate.compareTo(MAX_LOSS_RATE) >= 0 ? MAX_LOSS_RATE : wasteRate;
        BigDecimal rateDecimal = rate.divide(HUNDRED, HIGH_SCALE, ROUNDING);
        BigDecimal divisor = BigDecimal.ONE.subtract(rateDecimal);
        return netCost.divide(divisor, HIGH_SCALE, ROUNDING);
    }

    /**
     * 计算每米重量（KG/M）：克重(GSM) × 门幅(CM) ÷ 100000
     */
    public static BigDecimal calculateKgPerMeter(BigDecimal weightGsm, BigDecimal widthCm) {
        if (weightGsm == null || widthCm == null) {
            return BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
        }
        BigDecimal gsm = weightGsm.max(BigDecimal.ZERO);
        BigDecimal width = widthCm.max(BigDecimal.ZERO);
        return gsm.multiply(width).divide(THOUSAND_100K, MONEY_SCALE, ROUNDING);
    }

    // ==================== 成本卡全量算价 ====================

    /**
     * 成本卡权威全量算价
     *
     * @param param 算价入参
     * @return 算价结果汇总
     */
    public static CostCardPricingResult calculateCostCard(CostCardPricingParam param) {
        if (param == null) {
            param = CostCardPricingParam.builder().build();
        }

        boolean isWoven = "WOVEN".equalsIgnoreCase(param.getFabricConstruction());
        String basis = isWoven ? "METER" : "KG";
        boolean isProfessional = isWoven && "PROFESSIONAL".equalsIgnoreCase(param.getWovenCalculationMode());

        BigDecimal kgPerMeter = calculateKgPerMeter(param.getGreigeWeightGsm(), param.getGreigeWidthCm());
        boolean hasKgPerMeter = kgPerMeter != null && kgPerMeter.compareTo(BigDecimal.ZERO) > 0;
        BigDecimal metersPerKg = hasKgPerMeter
                ? BigDecimal.ONE.divide(kgPerMeter, MONEY_SCALE, ROUNDING)
                : BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);

        // 校验梭织跨口径前置依赖：梭织必须具备有效每米重量以支持双口径换算
        if (isWoven && !hasKgPerMeter) {
            throw ServiceExceptionUtil.exception(COST_CARD_KG_METER_CONVERSION_MISSING);
        }

        // 检查费用行是否包含需要跨口径换算但缺失条件的场景
        List<FeePricingRow> feeRows = param.getFeeRows() != null ? param.getFeeRows() : Collections.emptyList();
        if (!isWoven) {
            boolean hasPerMeterFee = feeRows.stream()
                    .anyMatch(f -> "PER_METER".equalsIgnoreCase(f.getFeeUnit()));
            if (hasPerMeterFee && !hasKgPerMeter) {
                throw ServiceExceptionUtil.exception(COST_CARD_KG_METER_CONVERSION_MISSING);
            }
        }

        // 1. 过滤有效纱线行
        List<YarnPricingRow> allYarns = param.getYarnRows() != null ? param.getYarnRows() : Collections.emptyList();
        List<YarnPricingRow> validYarns = new ArrayList<>();
        for (YarnPricingRow row : allYarns) {
            if (row != null && StringUtils.hasText(row.getYarnSpecName())) {
                validYarns.add(row);
            }
        }

        BigDecimal ratioTotal = BigDecimal.ZERO;
        int missingPriceCount = 0;
        for (YarnPricingRow row : validYarns) {
            BigDecimal r = row.getRatio() != null ? row.getRatio().max(BigDecimal.ZERO) : BigDecimal.ZERO;
            ratioTotal = ratioTotal.add(r);
            if (row.getPrice() == null || row.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                missingPriceCount++;
            }
        }
        ratioTotal = ratioTotal.setScale(MONEY_SCALE, ROUNDING);

        // 2. 纱线成本计算与行贡献
        List<YarnRowPricingResult> yarnResults = new ArrayList<>(validYarns.size());
        BigDecimal yarnCostPerKg = null;
        BigDecimal yarnCostPerMeter = null;

        if (!isWoven) {
            // 针织模式：basis=KG
            BigDecimal sumYarnPerKgHigh = BigDecimal.ZERO;
            for (YarnPricingRow row : validYarns) {
                BigDecimal p = row.getPrice() != null ? row.getPrice().max(BigDecimal.ZERO) : BigDecimal.ZERO;
                BigDecimal r = row.getRatio() != null ? row.getRatio().max(BigDecimal.ZERO) : BigDecimal.ZERO;
                BigDecimal net = p.multiply(r).divide(HUNDRED, HIGH_SCALE, ROUNDING);
                BigDecimal rowGrossHigh = applyLossHighPrecision(net, row.getWasteRate());
                sumYarnPerKgHigh = sumYarnPerKgHigh.add(rowGrossHigh);

                BigDecimal rowContributionKg = rowGrossHigh.setScale(MONEY_SCALE, ROUNDING);
                BigDecimal rowContributionM = hasKgPerMeter
                        ? rowContributionKg.multiply(kgPerMeter).setScale(MONEY_SCALE, ROUNDING)
                        : null;

                yarnResults.add(YarnRowPricingResult.builder()
                        .yarnSpecId(row.getYarnSpecId())
                        .rowContributionInBasis(rowContributionKg)
                        .contributionPerKg(rowContributionKg)
                        .contributionPerMeter(rowContributionM)
                        .build());
            }
            yarnCostPerKg = sumYarnPerKgHigh.setScale(MONEY_SCALE, ROUNDING);
            yarnCostPerMeter = hasKgPerMeter
                    ? yarnCostPerKg.multiply(kgPerMeter).setScale(MONEY_SCALE, ROUNDING)
                    : null;
        } else if (isProfessional) {
            // 梭织经纬精算：basis=METER
            BigDecimal sumYarnPerMeterHigh = BigDecimal.ZERO;
            for (YarnPricingRow row : validYarns) {
                BigDecimal p = row.getPrice() != null ? row.getPrice().max(BigDecimal.ZERO) : BigDecimal.ZERO;
                BigDecimal c = row.getConsumptionKgPerMeter() != null ? row.getConsumptionKgPerMeter().max(BigDecimal.ZERO) : BigDecimal.ZERO;
                BigDecimal net = p.multiply(c);
                BigDecimal rowGrossHigh = applyLossHighPrecision(net, row.getWasteRate());
                sumYarnPerMeterHigh = sumYarnPerMeterHigh.add(rowGrossHigh);

                BigDecimal rowContributionM = rowGrossHigh.setScale(MONEY_SCALE, ROUNDING);
                BigDecimal rowContributionKg = rowContributionM.divide(kgPerMeter, MONEY_SCALE, ROUNDING);

                yarnResults.add(YarnRowPricingResult.builder()
                        .yarnSpecId(row.getYarnSpecId())
                        .rowContributionInBasis(rowContributionM)
                        .contributionPerMeter(rowContributionM)
                        .contributionPerKg(rowContributionKg)
                        .build());
            }
            yarnCostPerMeter = sumYarnPerMeterHigh.setScale(MONEY_SCALE, ROUNDING);
            yarnCostPerKg = yarnCostPerMeter.divide(kgPerMeter, MONEY_SCALE, ROUNDING);
        } else {
            // 梭织简易估算：basis=METER
            BigDecimal sumBlendedHigh = BigDecimal.ZERO;
            for (YarnPricingRow row : validYarns) {
                BigDecimal p = row.getPrice() != null ? row.getPrice().max(BigDecimal.ZERO) : BigDecimal.ZERO;
                BigDecimal r = row.getRatio() != null ? row.getRatio().max(BigDecimal.ZERO) : BigDecimal.ZERO;
                BigDecimal net = p.multiply(r).divide(HUNDRED, HIGH_SCALE, ROUNDING);
                BigDecimal blendedPartHigh = applyLossHighPrecision(net, row.getWasteRate());
                sumBlendedHigh = sumBlendedHigh.add(blendedPartHigh);

                BigDecimal rowContributionKg = blendedPartHigh.setScale(MONEY_SCALE, ROUNDING);
                BigDecimal rowContributionM = kgPerMeter.multiply(blendedPartHigh).setScale(MONEY_SCALE, ROUNDING);

                yarnResults.add(YarnRowPricingResult.builder()
                        .yarnSpecId(row.getYarnSpecId())
                        .rowContributionInBasis(rowContributionM)
                        .contributionPerMeter(rowContributionM)
                        .contributionPerKg(rowContributionKg)
                        .build());
            }
            yarnCostPerMeter = kgPerMeter.multiply(sumBlendedHigh).setScale(MONEY_SCALE, ROUNDING);
            yarnCostPerKg = sumBlendedHigh.setScale(MONEY_SCALE, ROUNDING);
        }

        // 3. 费用行双口径计算
        List<FeeRowPricingResult> feeResults = new ArrayList<>(feeRows.size());
        BigDecimal feeTotalPerKg = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
        BigDecimal feeTotalPerMeter = hasKgPerMeter || isWoven
                ? BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING)
                : null;

        for (FeePricingRow fee : feeRows) {
            BigDecimal p = fee.getPrice() != null ? fee.getPrice().max(BigDecimal.ZERO) : BigDecimal.ZERO;
            BigDecimal gross = applyLoss(p, fee.getLossRate());

            BigDecimal cPerKg;
            BigDecimal cPerMeter;

            if ("PER_METER".equalsIgnoreCase(fee.getFeeUnit())) {
                cPerMeter = gross;
                cPerKg = hasKgPerMeter
                        ? gross.divide(kgPerMeter, MONEY_SCALE, ROUNDING)
                        : null;
            } else {
                // 默认 PER_KG
                cPerKg = gross;
                cPerMeter = hasKgPerMeter
                        ? gross.multiply(kgPerMeter).setScale(MONEY_SCALE, ROUNDING)
                        : null;
            }

            if (cPerKg != null) {
                feeTotalPerKg = feeTotalPerKg.add(cPerKg);
            }
            if (feeTotalPerMeter != null && cPerMeter != null) {
                feeTotalPerMeter = feeTotalPerMeter.add(cPerMeter);
            }

            feeResults.add(FeeRowPricingResult.builder()
                    .feeType(fee.getFeeType())
                    .feeUnit(fee.getFeeUnit())
                    .grossPrice(gross)
                    .contributionPerKg(cPerKg)
                    .contributionPerMeter(cPerMeter)
                    .build());
        }

        // 4. 汇总单位成本（不含税与含税）
        BigDecimal taxRate = param.getTaxRate() != null && param.getTaxRate().compareTo(BigDecimal.ZERO) >= 0
                ? param.getTaxRate()
                : BigDecimal.ZERO.setScale(RATE_SCALE, ROUNDING);
        BigDecimal taxMultiplier = BigDecimal.ONE.add(taxRate);

        BigDecimal unitCostExclTaxPerKg = (yarnCostPerKg != null)
                ? yarnCostPerKg.add(feeTotalPerKg).setScale(MONEY_SCALE, ROUNDING)
                : null;
        BigDecimal unitCostInclTaxPerKg = (unitCostExclTaxPerKg != null)
                ? unitCostExclTaxPerKg.multiply(taxMultiplier).setScale(MONEY_SCALE, ROUNDING)
                : null;

        BigDecimal unitCostExclTaxPerMeter = (yarnCostPerMeter != null && feeTotalPerMeter != null)
                ? yarnCostPerMeter.add(feeTotalPerMeter).setScale(MONEY_SCALE, ROUNDING)
                : null;
        BigDecimal unitCostInclTaxPerMeter = (unitCostExclTaxPerMeter != null)
                ? unitCostExclTaxPerMeter.multiply(taxMultiplier).setScale(MONEY_SCALE, ROUNDING)
                : null;

        return CostCardPricingResult.builder()
                .basis(basis)
                .kgPerMeter(kgPerMeter)
                .metersPerKg(metersPerKg)
                .ratioTotal(ratioTotal)
                .missingPriceCount(missingPriceCount)
                .yarnCostPerKg(yarnCostPerKg)
                .yarnCostPerMeter(yarnCostPerMeter)
                .feeTotalPerKg(feeTotalPerKg)
                .feeTotalPerMeter(feeTotalPerMeter)
                .unitCostExclTaxPerKg(unitCostExclTaxPerKg)
                .unitCostExclTaxPerMeter(unitCostExclTaxPerMeter)
                .unitCostInclTaxPerKg(unitCostInclTaxPerKg)
                .unitCostInclTaxPerMeter(unitCostInclTaxPerMeter)
                .taxRate(taxRate)
                .yarnRowResults(yarnResults)
                .feeRowResults(feeResults)
                .build();
    }

    // ==================== 报价单折算与毛利率 ====================

    /**
     * 计算报价单单行金额与毛利率
     */
    public static QuotationItemPricingResult calculateQuotationItem(QuotationItemPricingParam param) {
        if (param == null) {
            param = QuotationItemPricingParam.builder().build();
        }

        BigDecimal rmbCost = param.getCostUnitPriceRmb() != null
                ? param.getCostUnitPriceRmb().max(BigDecimal.ZERO)
                : BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);

        BigDecimal fxRate = param.getExchangeRate() != null && param.getExchangeRate().compareTo(BigDecimal.ZERO) > 0
                ? param.getExchangeRate()
                : BigDecimal.ONE;

        // 折报价币成本 = 人民币成本 ÷ 汇率
        BigDecimal costUnitPrice = rmbCost.divide(fxRate, MONEY_SCALE, ROUNDING);

        BigDecimal qty = param.getQuantity() != null
                ? param.getQuantity().max(BigDecimal.ZERO).setScale(QUANTITY_SCALE, ROUNDING)
                : BigDecimal.ZERO.setScale(QUANTITY_SCALE, ROUNDING);

        BigDecimal costAmount = qty.multiply(costUnitPrice).setScale(MONEY_SCALE, ROUNDING);

        // 报价单价确定：若传了 unitPrice 则使用；否则若传了 targetMarginRate 则反算
        BigDecimal unitPrice = param.getUnitPrice();
        if (unitPrice == null && param.getTargetMarginRate() != null) {
            BigDecimal margin = param.getTargetMarginRate();
            // 校验合法性：必须介于 -100% 与 100% 之间且不能等于 100%
            if (margin.compareTo(BigDecimal.ONE) >= 0 || margin.compareTo(new BigDecimal("-1")) <= 0) {
                throw ServiceExceptionUtil.exception(QUOTATION_MARGIN_RATE_INVALID);
            }
            BigDecimal divisor = BigDecimal.ONE.subtract(margin);
            unitPrice = costUnitPrice.divide(divisor, MONEY_SCALE, ROUNDING);
        } else if (unitPrice == null) {
            unitPrice = BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING);
        } else {
            unitPrice = unitPrice.max(BigDecimal.ZERO).setScale(MONEY_SCALE, ROUNDING);
        }

        BigDecimal amount = qty.multiply(unitPrice).setScale(MONEY_SCALE, ROUNDING);

        // 实际毛利率 = 1 − 成本额 ÷ 金额（金额为 0 或成本额为 0 且金额为 0 时为 null）
        BigDecimal marginRate = null;
        if (amount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal costDivAmount = costAmount.divide(amount, RATE_SCALE, ROUNDING);
            marginRate = BigDecimal.ONE.subtract(costDivAmount).setScale(RATE_SCALE, ROUNDING);
        }

        return QuotationItemPricingResult.builder()
                .costUnitPrice(costUnitPrice)
                .unitPrice(unitPrice)
                .costAmount(costAmount)
                .amount(amount)
                .marginRate(marginRate)
                .quantity(qty)
                .build();
    }

    /**
     * 计算报价单整单汇总与加权毛利率
     */
    public static QuotationSummaryPricingResult calculateQuotationSummary(List<QuotationItemPricingResult> items) {
        if (items == null || items.isEmpty()) {
            return QuotationSummaryPricingResult.builder()
                    .totalQuantity(BigDecimal.ZERO.setScale(QUANTITY_SCALE, ROUNDING))
                    .totalAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING))
                    .totalCostAmount(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING))
                    .avgMarginRate(null)
                    .build();
        }

        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalCostAmount = BigDecimal.ZERO;

        for (QuotationItemPricingResult item : items) {
            if (item != null) {
                if (item.getQuantity() != null) {
                    totalQty = totalQty.add(item.getQuantity());
                }
                if (item.getAmount() != null) {
                    totalAmount = totalAmount.add(item.getAmount());
                }
                if (item.getCostAmount() != null) {
                    totalCostAmount = totalCostAmount.add(item.getCostAmount());
                }
            }
        }

        totalQty = totalQty.setScale(QUANTITY_SCALE, ROUNDING);
        totalAmount = totalAmount.setScale(MONEY_SCALE, ROUNDING);
        totalCostAmount = totalCostAmount.setScale(MONEY_SCALE, ROUNDING);

        BigDecimal avgMarginRate = null;
        if (totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            avgMarginRate = totalAmount.subtract(totalCostAmount).divide(totalAmount, RATE_SCALE, ROUNDING);
        }

        return QuotationSummaryPricingResult.builder()
                .totalQuantity(totalQty)
                .totalAmount(totalAmount)
                .totalCostAmount(totalCostAmount)
                .avgMarginRate(avgMarginRate)
                .build();
    }

    // ==================== 历史 TS 单测兼容适配器 ====================

    /**
     * 兼容历史 TS product-yarn-cost.ts 行为的包装方法（用于回归验证既有前端用例）
     */
    public static CostCardPricingResult calculateLegacyCompatible(
            List<YarnPricingRow> specs,
            String fabricConstruction,
            String wovenCalculationMode,
            BigDecimal greigeWeightGsm,
            BigDecimal greigeWidthCm,
            BigDecimal knitProcessingFeePerKg,
            BigDecimal weavingFeePerMeter,
            BigDecimal otherProcessingFeePerMeter) {

        List<FeePricingRow> fees = new ArrayList<>();
        if ("KNIT".equalsIgnoreCase(fabricConstruction)) {
            if (knitProcessingFeePerKg != null && knitProcessingFeePerKg.compareTo(BigDecimal.ZERO) > 0) {
                fees.add(FeePricingRow.builder()
                        .feeType("WEAVING")
                        .feeUnit("PER_KG")
                        .price(knitProcessingFeePerKg)
                        .lossRate(BigDecimal.ZERO)
                        .build());
            }
        } else {
            if (weavingFeePerMeter != null && weavingFeePerMeter.compareTo(BigDecimal.ZERO) > 0) {
                fees.add(FeePricingRow.builder()
                        .feeType("WEAVING")
                        .feeUnit("PER_METER")
                        .price(weavingFeePerMeter)
                        .lossRate(BigDecimal.ZERO)
                        .build());
            }
            if (otherProcessingFeePerMeter != null && otherProcessingFeePerMeter.compareTo(BigDecimal.ZERO) > 0) {
                fees.add(FeePricingRow.builder()
                        .feeType("OTHER")
                        .feeUnit("PER_METER")
                        .price(otherProcessingFeePerMeter)
                        .lossRate(BigDecimal.ZERO)
                        .build());
            }
        }

        CostCardPricingParam param = CostCardPricingParam.builder()
                .fabricConstruction(fabricConstruction)
                .wovenCalculationMode(wovenCalculationMode)
                .greigeWeightGsm(greigeWeightGsm)
                .greigeWidthCm(greigeWidthCm)
                .taxRate(BigDecimal.ZERO)
                .yarnRows(specs)
                .feeRows(fees)
                .build();

        // 历史 TS 允许克重门幅为 0，特此处理兼容返回
        BigDecimal kgPerMeter = calculateKgPerMeter(greigeWeightGsm, greigeWidthCm);
        boolean isWoven = "WOVEN".equalsIgnoreCase(fabricConstruction);
        if (isWoven && kgPerMeter.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal ratioTotal = BigDecimal.ZERO;
            int missingPriceCount = 0;
            if (specs != null) {
                for (YarnPricingRow row : specs) {
                    if (row != null && StringUtils.hasText(row.getYarnSpecName())) {
                        ratioTotal = ratioTotal.add(row.getRatio() != null ? row.getRatio().max(BigDecimal.ZERO) : BigDecimal.ZERO);
                        if (row.getPrice() == null || row.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                            missingPriceCount++;
                        }
                    }
                }
            }
            return CostCardPricingResult.builder()
                    .basis("METER")
                    .kgPerMeter(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING))
                    .metersPerKg(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING))
                    .ratioTotal(ratioTotal.setScale(MONEY_SCALE, ROUNDING))
                    .missingPriceCount(missingPriceCount)
                    .yarnCostPerKg(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING))
                    .yarnCostPerMeter(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING))
                    .feeTotalPerKg(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING))
                    .feeTotalPerMeter(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING))
                    .unitCostExclTaxPerKg(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING))
                    .unitCostExclTaxPerMeter(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING))
                    .unitCostInclTaxPerKg(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING))
                    .unitCostInclTaxPerMeter(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING))
                    .taxRate(BigDecimal.ZERO.setScale(RATE_SCALE, ROUNDING))
                    .build();
        }

        CostCardPricingResult res = calculateCostCard(param);
        if (!isWoven && res.getYarnCostPerMeter() == null) {
            res.setYarnCostPerMeter(BigDecimal.ZERO.setScale(MONEY_SCALE, ROUNDING));
        }
        return res;
    }

}
