package com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.SupplierReconCandidateItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.SupplierReconCandidatesRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.finance.supplierrecon.SupplierReconItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.item.InspectionOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.finance.supplierrecon.SupplierReconMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.item.InspectionOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInMapper;
import com.luna.lunasrv.module.lunaerp.enums.finance.PricingBasisEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.SupplierReconLineTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.SupplierReconSourceRefTypeEnum;
import com.luna.lunasrv.module.lunaerp.enums.finance.SupplierReconStatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 财务管理 - 供应商对账唯一取数服务实现
 *
 * 核心真相来源：从财务/采购视角，选定供应商与对账区间后，
 * 产出一份"未被任何有效对账单引用过"的候选明细集合（内存装配、不落库）。
 *
 * 老系统 P0 教训：取数口径收敛为单一服务、禁止他处复制口径。
 *
 * @author luna
 */
@Slf4j
@Service
public class SupplierReconSourceServiceImpl implements SupplierReconSourceService {

    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String IN_TYPE_PURCHASE = "PURCHASE";
    private static final String TYPE_GREIGE = "GREIGE";
    private static final String UNIT_KG = "KG";
    private static final String UNIT_M = "M";
    private static final String UNIT_METER = "METER";

    @Resource
    private InspectionOrderMapper inspectionOrderMapper;

    @Resource
    private InspectionOrderItemMapper inspectionOrderItemMapper;

    @Resource
    private StockInMapper stockInMapper;

    @Resource
    private StockInItemMapper stockInItemMapper;

    @Resource
    private SupplierReconMapper supplierReconMapper;

    @Resource
    private SupplierReconItemMapper supplierReconItemMapper;

    @Override
    public SupplierReconCandidatesRespVO getCandidateItems(Long supplierId, LocalDate periodStart, LocalDate periodEnd) {
        return getCandidateItems(supplierId, periodStart, periodEnd, null);
    }

    @Override
    public SupplierReconCandidatesRespVO getCandidateItems(Long supplierId, LocalDate periodStart, LocalDate periodEnd, Long excludeReconId) {
        if (supplierId == null) {
            return new SupplierReconCandidatesRespVO(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }
        if (periodStart != null && periodEnd != null && periodStart.isAfter(periodEnd)) {
            return new SupplierReconCandidatesRespVO(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        }

        // 1. 查询被 DRAFT / CONFIRMED 对账单引用的防重指纹（CANCELLED 对账单释放回池，排除 excludeReconId）
        Set<String> occupiedFingerprints = getOccupiedFingerprints(supplierId, excludeReconId);

        // 2. 加工费候选（PROCESS）：COMPLETED 检验单按承检方汇总
        List<SupplierReconCandidateItemRespVO> processCandidates = getProcessCandidates(supplierId, periodStart, periodEnd, occupiedFingerprints);

        // 3. 采购货款候选（PURCHASE）：COMPLETED 且 in_type=PURCHASE 入库明细
        List<SupplierReconCandidateItemRespVO> purchaseCandidates = getPurchaseCandidates(supplierId, periodStart, periodEnd, occupiedFingerprints);

        // 4. 合并全部候选明细
        List<SupplierReconCandidateItemRespVO> allCandidates = new ArrayList<>(processCandidates.size() + purchaseCandidates.size());
        allCandidates.addAll(processCandidates);
        allCandidates.addAll(purchaseCandidates);

        return new SupplierReconCandidatesRespVO(allCandidates, processCandidates, purchaseCandidates);
    }

    /**
     * 查询指定供应商名下有效对账单（DRAFT / CONFIRMED）已占用的指纹集合
     */
    private Set<String> getOccupiedFingerprints(Long supplierId, Long excludeReconId) {
        List<SupplierReconDO> activeRecons = supplierReconMapper.selectListBySupplierIdAndStatuses(
                supplierId,
                Arrays.asList(SupplierReconStatusEnum.DRAFT.getValue(), SupplierReconStatusEnum.CONFIRMED.getValue())
        );
        if (CollUtil.isEmpty(activeRecons)) {
            return Collections.emptySet();
        }
        if (excludeReconId != null) {
            activeRecons = activeRecons.stream()
                    .filter(r -> !excludeReconId.equals(r.getId()))
                    .toList();
        }
        if (CollUtil.isEmpty(activeRecons)) {
            return Collections.emptySet();
        }

        List<Long> activeReconIds = activeRecons.stream().map(SupplierReconDO::getId).toList();
        List<SupplierReconItemDO> activeItems = supplierReconItemMapper.selectListByReconIds(activeReconIds);
        if (CollUtil.isEmpty(activeItems)) {
            return Collections.emptySet();
        }

        Set<String> fingerprints = new HashSet<>();
        for (SupplierReconItemDO item : activeItems) {
            if (StringUtils.hasText(item.getSourceRefType()) && item.getSourceRefId() != null) {
                fingerprints.add(SupplierReconSourceService.buildFingerprint(
                        item.getSourceRefType(), item.getSourceRefId(), item.getSourceRefItemId()));
            }
        }
        return fingerprints;
    }

    /**
     * 获取加工费候选明细（一张检验单汇总一行）
     */
    private List<SupplierReconCandidateItemRespVO> getProcessCandidates(
            Long supplierId, LocalDate periodStart, LocalDate periodEnd, Set<String> occupiedFingerprints) {

        LambdaQueryWrapperX<InspectionOrderDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(InspectionOrderDO::getStatus, STATUS_COMPLETED)
                .eq(InspectionOrderDO::getInspectionFactoryId, supplierId)
                .betweenIfPresent(InspectionOrderDO::getOrderDate, periodStart, periodEnd);
        wrapper.orderByAsc(InspectionOrderDO::getOrderDate).orderByAsc(InspectionOrderDO::getId);

        List<InspectionOrderDO> inspectionOrders = inspectionOrderMapper.selectList(wrapper);
        if (CollUtil.isEmpty(inspectionOrders)) {
            return Collections.emptyList();
        }

        List<Long> orderIds = inspectionOrders.stream().map(InspectionOrderDO::getId).toList();
        List<InspectionOrderItemDO> allItems = inspectionOrderItemMapper.selectListByInspectionOrderIds(orderIds);
        Map<Long, List<InspectionOrderItemDO>> itemMap = CollUtil.isNotEmpty(allItems)
                ? allItems.stream().collect(Collectors.groupingBy(InspectionOrderItemDO::getInspectionOrderId))
                : Collections.emptyMap();

        List<SupplierReconCandidateItemRespVO> result = new ArrayList<>();
        for (InspectionOrderDO order : inspectionOrders) {
            String fingerprint = SupplierReconSourceService.buildFingerprint(
                    SupplierReconSourceRefTypeEnum.INSPECTION.getValue(), order.getId(), null);
            if (occupiedFingerprints.contains(fingerprint)) {
                continue;
            }

            List<InspectionOrderItemDO> items = itemMap.getOrDefault(order.getId(), Collections.emptyList());

            BigDecimal measuredWeightSum = BigDecimal.ZERO;
            BigDecimal lengthMSum = BigDecimal.ZERO;
            for (InspectionOrderItemDO item : items) {
                if (item.getMeasuredWeight() != null) {
                    measuredWeightSum = measuredWeightSum.add(item.getMeasuredWeight());
                }
                if (item.getLengthM() != null) {
                    lengthMSum = lengthMSum.add(item.getLengthM());
                }
            }
            measuredWeightSum = measuredWeightSum.setScale(2, RoundingMode.HALF_UP);
            lengthMSum = lengthMSum.setScale(2, RoundingMode.HALF_UP);
            int rollCount = items.size();

            // 缺省计价基准：GREIGE 缺省 METER，FINISHED 缺省 KG
            boolean isGreige = TYPE_GREIGE.equalsIgnoreCase(order.getType());
            String pricingBasis = isGreige ? PricingBasisEnum.METER.getValue() : PricingBasisEnum.KG.getValue();
            BigDecimal billQty = PricingBasisEnum.METER.getValue().equals(pricingBasis) ? lengthMSum : measuredWeightSum;

            SupplierReconCandidateItemRespVO candidate = SupplierReconCandidateItemRespVO.builder()
                    .lineType(SupplierReconLineTypeEnum.PROCESS.getValue())
                    .sourceRefType(SupplierReconSourceRefTypeEnum.INSPECTION.getValue())
                    .sourceRefId(order.getId())
                    .sourceRefItemId(null)
                    .inspectionOrderId(order.getId())
                    .inspectionOrderNo(order.getCode())
                    .inspectionType(order.getType())
                    .rollCount(rollCount)
                    .productId(order.getProductId())
                    .productCode(order.getProductCode())
                    .productName(order.getProductName())
                    .qtyKg(measuredWeightSum)
                    .qtyM(lengthMSum)
                    .pricingBasis(pricingBasis)
                    .billQty(billQty)
                    .unitPrice(null)
                    .lineAmount(BigDecimal.ZERO)
                    .orderDate(order.getOrderDate())
                    .build();

            result.add(candidate);
        }

        return result;
    }

    /**
     * 获取采购货款候选明细（一条入库明细一行，退货 RETURN 排除）
     */
    private List<SupplierReconCandidateItemRespVO> getPurchaseCandidates(
            Long supplierId, LocalDate periodStart, LocalDate periodEnd, Set<String> occupiedFingerprints) {

        LambdaQueryWrapperX<StockInDO> stockInWrapper = new LambdaQueryWrapperX<>();
        stockInWrapper.eq(StockInDO::getStatus, STATUS_COMPLETED)
                .eq(StockInDO::getInType, IN_TYPE_PURCHASE)
                .eq(StockInDO::getSupplierId, supplierId)
                .betweenIfPresent(StockInDO::getInDate, periodStart, periodEnd);
        stockInWrapper.orderByAsc(StockInDO::getInDate).orderByAsc(StockInDO::getId);

        List<StockInDO> stockIns = stockInMapper.selectList(stockInWrapper);
        if (CollUtil.isEmpty(stockIns)) {
            return Collections.emptyList();
        }

        List<Long> stockInIds = stockIns.stream().map(StockInDO::getId).toList();
        List<StockInItemDO> stockInItems = stockInItemMapper.selectListByStockInIds(stockInIds);
        if (CollUtil.isEmpty(stockInItems)) {
            return Collections.emptyList();
        }

        Map<Long, StockInDO> stockInMap = stockIns.stream()
                .collect(Collectors.toMap(StockInDO::getId, s -> s));

        List<SupplierReconCandidateItemRespVO> result = new ArrayList<>();
        for (StockInItemDO item : stockInItems) {
            StockInDO stockIn = stockInMap.get(item.getStockInId());
            if (stockIn == null) {
                continue;
            }

            String fingerprint = SupplierReconSourceService.buildFingerprint(
                    SupplierReconSourceRefTypeEnum.STOCK_IN_ITEM.getValue(), stockIn.getId(), item.getId());
            if (occupiedFingerprints.contains(fingerprint)) {
                continue;
            }

            BigDecimal qty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
            BigDecimal scaledQty = qty.setScale(2, RoundingMode.HALF_UP);
            String unit = item.getUnit();
            BigDecimal unitPrice = item.getUnitPrice();

            BigDecimal lineAmount;
            if (item.getQuantity() != null && unitPrice != null) {
                lineAmount = item.getQuantity().multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);
            } else if (item.getAmount() != null) {
                lineAmount = item.getAmount().setScale(2, RoundingMode.HALF_UP);
            } else {
                lineAmount = BigDecimal.ZERO;
            }

            String pricingBasis;
            BigDecimal qtyKg = null;
            BigDecimal qtyM = null;
            if (UNIT_KG.equalsIgnoreCase(unit)) {
                pricingBasis = PricingBasisEnum.KG.getValue();
                qtyKg = scaledQty;
            } else if (UNIT_M.equalsIgnoreCase(unit) || UNIT_METER.equalsIgnoreCase(unit)) {
                pricingBasis = PricingBasisEnum.METER.getValue();
                qtyM = scaledQty;
            } else {
                pricingBasis = unit != null ? unit : PricingBasisEnum.KG.getValue();
            }

            String productName = StringUtils.hasText(item.getProductName()) ? item.getProductName() : item.getYarnSpecName();

            SupplierReconCandidateItemRespVO candidate = SupplierReconCandidateItemRespVO.builder()
                    .lineType(SupplierReconLineTypeEnum.PURCHASE.getValue())
                    .sourceRefType(SupplierReconSourceRefTypeEnum.STOCK_IN_ITEM.getValue())
                    .sourceRefId(stockIn.getId())
                    .sourceRefItemId(item.getId())
                    .stockInId(stockIn.getId())
                    .stockInNo(stockIn.getInNo())
                    .yarnOrderId(stockIn.getYarnOrderId())
                    .yarnOrderNo(stockIn.getYarnOrderNo())
                    .productId(item.getProductId())
                    .productCode(item.getProductCode())
                    .productName(productName)
                    .batchNo(item.getBatchNo())
                    .quantity(scaledQty)
                    .unit(unit)
                    .qtyKg(qtyKg)
                    .qtyM(qtyM)
                    .pricingBasis(pricingBasis)
                    .billQty(scaledQty)
                    .unitPrice(unitPrice)
                    .lineAmount(lineAmount)
                    .orderDate(stockIn.getInDate())
                    .build();

            result.add(candidate);
        }

        return result;
    }

}
