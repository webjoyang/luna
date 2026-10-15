package com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.finance.supplierrecon.vo.SupplierReconSourceItemRespVO;
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
import com.luna.lunasrv.module.lunaerp.enums.finance.LineType;
import com.luna.lunasrv.module.lunaerp.enums.finance.PricingBasis;
import com.luna.lunasrv.module.lunaerp.enums.finance.ReconStatus;
import com.luna.lunasrv.module.lunaerp.enums.finance.SourceRefType;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 供应商对账唯一取数服务实现类
 *
 * <p>老系统 P0 教训落地件：取数口径收敛为单一服务、禁止他处复制口径。
 * 纯内存候选装配、不落库、零回写。</p>
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class SupplierReconSourceServiceImpl implements SupplierReconSourceService {

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
    public List<SupplierReconSourceItemRespVO> getAvailableCandidateItems(Long supplierId, LocalDate periodStart, LocalDate periodEnd) {
        List<SupplierReconSourceItemRespVO> allCandidates = getAllCandidateItems(supplierId, periodStart, periodEnd);
        if (CollUtil.isEmpty(allCandidates)) {
            return Collections.emptyList();
        }

        Set<String> fingerprints = allCandidates.stream()
                .map(SupplierReconSourceItemRespVO::getFingerprint)
                .collect(Collectors.toSet());

        Map<String, String> occupiedMap = findOccupiedFingerprints(fingerprints);
        if (occupiedMap.isEmpty()) {
            return allCandidates;
        }

        return allCandidates.stream()
                .filter(item -> !occupiedMap.containsKey(item.getFingerprint()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SupplierReconSourceItemRespVO> getAllCandidateItems(Long supplierId, LocalDate periodStart, LocalDate periodEnd) {
        if (supplierId == null || periodStart == null || periodEnd == null) {
            return Collections.emptyList();
        }

        List<SupplierReconSourceItemRespVO> result = new ArrayList<>();

        // 1. 加工费候选（PROCESS）：仅 status=COMPLETED 检验单，承检方=供应商，单据日期 ∈ 区间（含端点）
        List<SupplierReconSourceItemRespVO> processCandidates = loadProcessCandidates(supplierId, periodStart, periodEnd);
        result.addAll(processCandidates);

        // 2. 采购货款候选（PURCHASE）：仅主表 COMPLETED 且 in_type=PURCHASE 的入库明细，单据日期 ∈ 区间（含端点），RETURN 排除
        List<SupplierReconSourceItemRespVO> purchaseCandidates = loadPurchaseCandidates(supplierId, periodStart, periodEnd);
        result.addAll(purchaseCandidates);

        return result;
    }

    @Override
    public Map<String, String> findOccupiedFingerprints(Collection<String> fingerprints) {
        if (CollUtil.isEmpty(fingerprints)) {
            return Collections.emptyMap();
        }

        // 查找所有有效对账单（DRAFT / CONFIRMED），CANCELLED 对账单的指纹自动释放回池
        List<SupplierReconDO> activeRecons = supplierReconMapper.selectList(new LambdaQueryWrapperX<SupplierReconDO>()
                .in(SupplierReconDO::getStatus, Arrays.asList(ReconStatus.DRAFT.getValue(), ReconStatus.CONFIRMED.getValue())));
        if (CollUtil.isEmpty(activeRecons)) {
            return Collections.emptyMap();
        }

        Map<Long, String> reconNoMap = activeRecons.stream()
                .collect(Collectors.toMap(SupplierReconDO::getId, SupplierReconDO::getReconNo, (existing, replacement) -> existing));

        List<SupplierReconItemDO> reconItems = supplierReconItemMapper.selectListByReconIds(reconNoMap.keySet());
        if (CollUtil.isEmpty(reconItems)) {
            return Collections.emptyMap();
        }

        Map<String, String> occupiedMap = new HashMap<>();
        for (SupplierReconItemDO item : reconItems) {
            if (item.getSourceRefType() == null || item.getSourceRefId() == null) {
                continue;
            }
            String fp = buildFingerprint(item.getSourceRefType(), item.getSourceRefId(), item.getSourceRefItemId());
            if (fingerprints.contains(fp)) {
                occupiedMap.put(fp, reconNoMap.get(item.getReconId()));
            }
        }
        return occupiedMap;
    }

    @Override
    public String buildFingerprint(String sourceRefType, Long sourceRefId, Long sourceRefItemId) {
        if (SourceRefType.INSPECTION.getValue().equalsIgnoreCase(sourceRefType) || sourceRefItemId == null) {
            return sourceRefType + ":" + sourceRefId;
        }
        return sourceRefType + ":" + sourceRefId + ":" + sourceRefItemId;
    }

    /**
     * 加载加工费候选行（按检验单汇总一行）
     */
    private List<SupplierReconSourceItemRespVO> loadProcessCandidates(Long supplierId, LocalDate periodStart, LocalDate periodEnd) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InspectionOrderDO> wrapper = new LambdaQueryWrapperX<InspectionOrderDO>()
                .eq(InspectionOrderDO::getStatus, "COMPLETED")
                .eq(InspectionOrderDO::getInspectionFactoryId, supplierId)
                .ge(InspectionOrderDO::getOrderDate, periodStart)
                .le(InspectionOrderDO::getOrderDate, periodEnd)
                .orderByAsc(InspectionOrderDO::getOrderDate)
                .orderByAsc(InspectionOrderDO::getId);
        List<InspectionOrderDO> inspectionOrders = inspectionOrderMapper.selectList(wrapper);
        if (CollUtil.isEmpty(inspectionOrders)) {
            return Collections.emptyList();
        }

        List<Long> orderIds = inspectionOrders.stream().map(InspectionOrderDO::getId).toList();
        List<InspectionOrderItemDO> allItems = inspectionOrderItemMapper.selectListByInspectionOrderIds(orderIds);
        Map<Long, List<InspectionOrderItemDO>> itemMap = CollUtil.isNotEmpty(allItems)
                ? allItems.stream().collect(Collectors.groupingBy(InspectionOrderItemDO::getInspectionOrderId))
                : Collections.emptyMap();

        List<SupplierReconSourceItemRespVO> list = new ArrayList<>(inspectionOrders.size());
        for (InspectionOrderDO order : inspectionOrders) {
            List<InspectionOrderItemDO> items = itemMap.getOrDefault(order.getId(), Collections.emptyList());

            // 录检明细 Σmeasured_weight(KG)、Σlength_m(M)、卷数；全等级（A/B/C/DEFECTIVE）计入不过滤
            BigDecimal totalWeight = BigDecimal.ZERO;
            BigDecimal totalLength = BigDecimal.ZERO;
            for (InspectionOrderItemDO it : items) {
                if (it.getMeasuredWeight() != null) {
                    totalWeight = totalWeight.add(it.getMeasuredWeight());
                }
                if (it.getLengthM() != null) {
                    totalLength = totalLength.add(it.getLengthM());
                }
            }
            totalWeight = totalWeight.setScale(2, RoundingMode.HALF_UP);
            totalLength = totalLength.setScale(2, RoundingMode.HALF_UP);
            int rollCount = items.size();

            // 计价基准：GREIGE 缺省 METER（米），FINISHED 缺省 KG（公斤）
            boolean isGreige = "GREIGE".equalsIgnoreCase(order.getType());
            String pricingBasis = isGreige ? PricingBasis.METER.getValue() : PricingBasis.KG.getValue();
            BigDecimal billQty = isGreige ? totalLength : totalWeight;

            SupplierReconSourceItemRespVO vo = new SupplierReconSourceItemRespVO();
            vo.setLineType(LineType.PROCESS.getValue());
            vo.setSourceRefType(SourceRefType.INSPECTION.getValue());
            vo.setSourceRefId(order.getId());
            vo.setSourceRefItemId(null);
            vo.setInspectionOrderId(order.getId());
            vo.setInspectionOrderNo(order.getCode());
            vo.setInspectionType(order.getType());
            vo.setProductId(order.getProductId());
            vo.setProductCode(order.getProductCode());
            vo.setProductName(order.getProductName());
            vo.setQtyKg(totalWeight);
            vo.setQtyM(totalLength);
            vo.setRollCount(rollCount);
            vo.setQtyUnit(isGreige ? "M" : "KG");
            vo.setPricingBasis(pricingBasis);
            vo.setBillQty(billQty);
            vo.setUnitPrice(null); // 加工费单价手工填，前端可按产品加工费预填
            vo.setLineAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            vo.setFingerprint(buildFingerprint(SourceRefType.INSPECTION.getValue(), order.getId(), null));

            list.add(vo);
        }
        return list;
    }

    /**
     * 加载采购货款候选行（一条入库明细一行，RETURN 排除）
     */
    private List<SupplierReconSourceItemRespVO> loadPurchaseCandidates(Long supplierId, LocalDate periodStart, LocalDate periodEnd) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StockInDO> wrapper = new LambdaQueryWrapperX<StockInDO>()
                .eq(StockInDO::getStatus, "COMPLETED")
                .eq(StockInDO::getInType, "PURCHASE") // 退货入库 RETURN 不进候选
                .eq(StockInDO::getSupplierId, supplierId)
                .ge(StockInDO::getInDate, periodStart)
                .le(StockInDO::getInDate, periodEnd)
                .orderByAsc(StockInDO::getInDate)
                .orderByAsc(StockInDO::getId);
        List<StockInDO> stockIns = stockInMapper.selectList(wrapper);
        if (CollUtil.isEmpty(stockIns)) {
            return Collections.emptyList();
        }

        List<Long> stockInIds = stockIns.stream().map(StockInDO::getId).toList();
        List<StockInItemDO> stockInItems = stockInItemMapper.selectList(new LambdaQueryWrapperX<StockInItemDO>()
                .in(StockInItemDO::getStockInId, stockInIds)
                .orderByAsc(StockInItemDO::getId));
        if (CollUtil.isEmpty(stockInItems)) {
            return Collections.emptyList();
        }

        Map<Long, StockInDO> stockInMap = stockIns.stream()
                .collect(Collectors.toMap(StockInDO::getId, s -> s, (existing, replacement) -> existing));

        List<SupplierReconSourceItemRespVO> list = new ArrayList<>(stockInItems.size());
        for (StockInItemDO item : stockInItems) {
            StockInDO stockIn = stockInMap.get(item.getStockInId());
            if (stockIn == null) {
                continue;
            }

            BigDecimal qty = item.getQuantity() != null ? item.getQuantity().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            String unit = item.getUnit();
            boolean isKg = "KG".equalsIgnoreCase(unit);
            BigDecimal qtyKg = isKg ? qty : null;
            BigDecimal qtyM = !isKg ? qty : null;
            String pricingBasis = isKg ? PricingBasis.KG.getValue() : PricingBasis.METER.getValue();
            BigDecimal billQty = qty;
            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal lineAmount = billQty.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP);

            SupplierReconSourceItemRespVO vo = new SupplierReconSourceItemRespVO();
            vo.setLineType(LineType.PURCHASE.getValue());
            vo.setSourceRefType(SourceRefType.STOCK_IN_ITEM.getValue());
            vo.setSourceRefId(stockIn.getId());
            vo.setSourceRefItemId(item.getId());
            vo.setStockInId(stockIn.getId());
            vo.setStockInNo(stockIn.getInNo());
            vo.setStockInItemId(item.getId());
            vo.setProductId(item.getProductId());
            vo.setProductCode(item.getProductCode());
            vo.setProductName(item.getProductName());
            vo.setBatchNo(item.getBatchNo());
            vo.setQtyKg(qtyKg);
            vo.setQtyM(qtyM);
            vo.setRollCount(item.getPieceCount());
            vo.setQtyUnit(unit);
            vo.setPricingBasis(pricingBasis);
            vo.setBillQty(billQty);
            vo.setUnitPrice(unitPrice);
            vo.setLineAmount(lineAmount);
            vo.setFingerprint(buildFingerprint(SourceRefType.STOCK_IN_ITEM.getValue(), stockIn.getId(), item.getId()));

            list.add(vo);
        }
        return list;
    }

}
