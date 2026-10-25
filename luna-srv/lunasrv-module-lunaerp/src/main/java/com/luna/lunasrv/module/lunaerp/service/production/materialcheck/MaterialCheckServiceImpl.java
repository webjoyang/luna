package com.luna.lunasrv.module.lunaerp.service.production.materialcheck;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.*;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockledger.vo.InventoryBalanceRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder.YarnOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom.YarnRecipeBomItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom.YarnRecipeBomMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WEAVING_ORDER_NOT_EXISTS;

/**
 * 生产管理 - 齐套检查服务实现类（只读试算，ADR-0041）
 *
 * @author luna
 */
@Service
public class MaterialCheckServiceImpl implements MaterialCheckService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Resource
    private WeavingOrderMapper weavingOrderMapper;

    @Resource
    private YarnRecipeBomMapper yarnRecipeBomMapper;

    @Resource
    private YarnRecipeBomItemMapper yarnRecipeBomItemMapper;

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Resource
    private YarnTypeMapper yarnTypeMapper;

    @Resource
    private StockLedgerMapper stockLedgerMapper;

    @Resource
    private YarnOrderItemMapper yarnOrderItemMapper;

    @Override
    public MaterialCheckRespVO calculateMaterialCheck(MaterialCheckReqVO reqVO) {
        if (reqVO == null) {
            reqVO = new MaterialCheckReqVO();
        }

        // 1. 加载织造单并做存在性/租户隔离校验
        List<WeavingOrderDO> targetOrders = loadAndValidateWeavingOrders(reqVO.getWeavingOrderIds());
        if (CollUtil.isEmpty(targetOrders)) {
            return new MaterialCheckRespVO();
        }

        // 2. 批量加载相关产品的启用态配比 BOM 及明细
        Set<Long> productIds = targetOrders.stream().map(WeavingOrderDO::getProductId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, YarnRecipeBomDO> bomByProductId = batchLoadActiveBoms(productIds);
        Map<Long, List<YarnRecipeBomItemDO>> itemsByBomId = batchLoadBomItems(
                bomByProductId.values().stream().map(YarnRecipeBomDO::getId).filter(Objects::nonNull).collect(Collectors.toList()));

        // 3. 区分非正常单据（缺BOM / 缺计划重量）与正常试算单据
        List<MaterialCheckMissingOrderVO> missingBomOrders = new ArrayList<>();
        List<MaterialCheckMissingOrderVO> missingWeightOrders = new ArrayList<>();
        // yarnSpecId -> 贡献列表
        Map<Long, List<MaterialCheckContributionVO>> contributionsBySpecId = new LinkedHashMap<>();

        for (WeavingOrderDO order : targetOrders) {
            YarnRecipeBomDO bom = bomByProductId.get(order.getProductId());
            List<YarnRecipeBomItemDO> bomItems = (bom != null) ? itemsByBomId.get(bom.getId()) : null;

            boolean hasBom = CollUtil.isNotEmpty(bomItems);
            boolean hasWeight = order.getPlannedWeight() != null && order.getPlannedWeight().compareTo(BigDecimal.ZERO) > 0
                    && order.getPlannedQty() != null && order.getPlannedQty() > 0;

            if (!hasBom) {
                missingBomOrders.add(buildMissingOrderVO(order, "产品未配置启用态配比BOM或明细为空"));
            }
            if (!hasWeight) {
                missingWeightOrders.add(buildMissingOrderVO(order, "织造单缺少计划重量或计划匹数"));
            }

            // 只要缺少 BOM 或缺少重量，均不污染正常聚合行
            if (!hasBom || !hasWeight) {
                continue;
            }

            // 正常计算各纱线规格需求量
            for (YarnRecipeBomItemDO item : bomItems) {
                Long yarnSpecId = item.getYarnSpecId();
                if (yarnSpecId == null) {
                    continue;
                }
                BigDecimal ratio = item.getRatio() != null ? item.getRatio() : BigDecimal.ZERO;
                BigDecimal orderRequirement = order.getPlannedWeight().multiply(ratio).divide(HUNDRED, 2, RoundingMode.HALF_UP);

                MaterialCheckContributionVO contrib = new MaterialCheckContributionVO();
                contrib.setWeavingOrderId(order.getId());
                contrib.setWeavingOrderCode(order.getCode());
                contrib.setProductId(order.getProductId());
                contrib.setProductCode(order.getProductCode());
                contrib.setProductName(order.getProductName());
                contrib.setPlannedQty(order.getPlannedQty());
                contrib.setPlannedWeight(order.getPlannedWeight().setScale(2, RoundingMode.HALF_UP));
                contrib.setRatio(ratio.setScale(2, RoundingMode.HALF_UP));
                contrib.setRequiredQty(orderRequirement);

                contributionsBySpecId.computeIfAbsent(yarnSpecId, k -> new ArrayList<>()).add(contrib);
            }
        }

        // 4. 若无任何正常纱线需求，直接返回分区提示
        if (contributionsBySpecId.isEmpty()) {
            MaterialCheckRespVO respVO = new MaterialCheckRespVO();
            respVO.setMissingBomOrders(missingBomOrders);
            respVO.setMissingWeightOrders(missingWeightOrders);
            return respVO;
        }

        // 5. 批量查询纱线规格与材质档案
        Set<Long> yarnSpecIds = contributionsBySpecId.keySet();
        Map<Long, YarnSpecDO> specMap = batchLoadYarnSpecs(yarnSpecIds);
        Set<Long> yarnTypeIds = specMap.values().stream().map(YarnSpecDO::getYarnTypeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, YarnTypeDO> typeMap = batchLoadYarnTypes(yarnTypeIds);

        // 6. 批量聚合可用库存与在途采购量（防 N+1）
        Map<Long, BigDecimal> stockMap = batchLoadStockBalances(yarnSpecIds, reqVO.getWarehouseIds());
        Map<Long, BigDecimal> inTransitMap = batchLoadInTransitQuantities(yarnSpecIds);

        // 7. 计算聚合行
        List<MaterialCheckRowVO> rows = new ArrayList<>();
        for (Map.Entry<Long, List<MaterialCheckContributionVO>> entry : contributionsBySpecId.entrySet()) {
            Long yarnSpecId = entry.getKey();
            List<MaterialCheckContributionVO> contributions = entry.getValue();

            BigDecimal requiredQty = contributions.stream()
                    .map(MaterialCheckContributionVO::getRequiredQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal availableQty = stockMap.getOrDefault(yarnSpecId, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal inTransitQty = inTransitMap.getOrDefault(yarnSpecId, BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            BigDecimal shortageExcludeInTransit = clampToZero(requiredQty.subtract(availableQty));
            BigDecimal shortageIncludeInTransit = clampToZero(requiredQty.subtract(availableQty).subtract(inTransitQty));
            BigDecimal suggestedPurchaseQty = shortageIncludeInTransit;

            YarnSpecDO spec = specMap.get(yarnSpecId);
            YarnTypeDO type = (spec != null && spec.getYarnTypeId() != null) ? typeMap.get(spec.getYarnTypeId()) : null;

            MaterialCheckRowVO row = new MaterialCheckRowVO();
            row.setYarnSpecId(yarnSpecId);
            row.setYarnSpecName(spec != null ? spec.getSpecName() : null);
            row.setYarnTypeId(spec != null ? spec.getYarnTypeId() : null);
            row.setYarnTypeName(type != null ? type.getName() : null);
            row.setRequiredQty(requiredQty);
            row.setAvailableQty(availableQty);
            row.setInTransitQty(inTransitQty);
            row.setShortageExcludeInTransit(shortageExcludeInTransit);
            row.setShortageIncludeInTransit(shortageIncludeInTransit);
            row.setSuggestedPurchaseQty(suggestedPurchaseQty);
            row.setContributions(contributions);

            rows.add(row);
        }

        // 8. 汇总指标计算（按 includeInTransit 开关）
        boolean includeInTransit = Boolean.TRUE.equals(reqVO.getIncludeInTransit());
        int shortageSpecCount = 0;
        BigDecimal totalShortageQty = BigDecimal.ZERO;

        for (MaterialCheckRowVO row : rows) {
            BigDecimal shortage = includeInTransit ? row.getShortageIncludeInTransit() : row.getShortageExcludeInTransit();
            if (shortage.compareTo(BigDecimal.ZERO) > 0) {
                shortageSpecCount++;
                totalShortageQty = totalShortageQty.add(shortage);
            }
        }
        totalShortageQty = totalShortageQty.setScale(2, RoundingMode.HALF_UP);

        MaterialCheckRespVO respVO = new MaterialCheckRespVO();
        respVO.setRows(rows);
        respVO.setMissingBomOrders(missingBomOrders);
        respVO.setMissingWeightOrders(missingWeightOrders);
        respVO.setShortageSpecCount(shortageSpecCount);
        respVO.setTotalShortageQty(totalShortageQty);
        return respVO;
    }

    // ==================== 内部加载方法（防 N+1 批量模式） ====================

    private List<WeavingOrderDO> loadAndValidateWeavingOrders(List<Long> weavingOrderIds) {
        if (CollUtil.isNotEmpty(weavingOrderIds)) {
            // 去重
            Set<Long> uniqueIds = new HashSet<>(weavingOrderIds);
            List<WeavingOrderDO> foundOrders = weavingOrderMapper.selectBatchIds(uniqueIds);
            if (foundOrders == null || foundOrders.size() != uniqueIds.size()) {
                throw exception(WEAVING_ORDER_NOT_EXISTS);
            }
            // 过滤仅保留 DRAFT / ACTIVE 单据（COMPLETED / CANCELLED 排除）
            return foundOrders.stream()
                    .filter(o -> STATUS_DRAFT.equals(o.getStatus()) || STATUS_ACTIVE.equals(o.getStatus()))
                    .collect(Collectors.toList());
        }

        // 缺省查当前租户全部 DRAFT / ACTIVE 织造单
        return weavingOrderMapper.selectList(new LambdaQueryWrapperX<WeavingOrderDO>()
                .in(WeavingOrderDO::getStatus, Arrays.asList(STATUS_DRAFT, STATUS_ACTIVE))
                .orderByDesc(WeavingOrderDO::getId));
    }

    private Map<Long, YarnRecipeBomDO> batchLoadActiveBoms(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<YarnRecipeBomDO> boms = yarnRecipeBomMapper.selectList(new LambdaQueryWrapperX<YarnRecipeBomDO>()
                .inOrEmpty(YarnRecipeBomDO::getProductId, productIds)
                .eq(YarnRecipeBomDO::getStatus, STATUS_ACTIVE));
        if (CollUtil.isEmpty(boms)) {
            return Collections.emptyMap();
        }
        return boms.stream().collect(Collectors.toMap(YarnRecipeBomDO::getProductId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, List<YarnRecipeBomItemDO>> batchLoadBomItems(Collection<Long> bomIds) {
        if (CollUtil.isEmpty(bomIds)) {
            return Collections.emptyMap();
        }
        List<YarnRecipeBomItemDO> items = yarnRecipeBomItemMapper.selectListByRecipeBomIds(bomIds);
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyMap();
        }
        return items.stream().collect(Collectors.groupingBy(YarnRecipeBomItemDO::getRecipeBomId));
    }

    private Map<Long, YarnSpecDO> batchLoadYarnSpecs(Collection<Long> specIds) {
        if (CollUtil.isEmpty(specIds)) {
            return Collections.emptyMap();
        }
        List<YarnSpecDO> specs = yarnSpecMapper.selectBatchIds(specIds);
        if (CollUtil.isEmpty(specs)) {
            return Collections.emptyMap();
        }
        return specs.stream().collect(Collectors.toMap(YarnSpecDO::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, YarnTypeDO> batchLoadYarnTypes(Collection<Long> typeIds) {
        if (CollUtil.isEmpty(typeIds)) {
            return Collections.emptyMap();
        }
        List<YarnTypeDO> types = yarnTypeMapper.selectBatchIds(typeIds);
        if (CollUtil.isEmpty(types)) {
            return Collections.emptyMap();
        }
        return types.stream().collect(Collectors.toMap(YarnTypeDO::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, BigDecimal> batchLoadStockBalances(Collection<Long> yarnSpecIds, List<Long> warehouseIds) {
        if (CollUtil.isEmpty(yarnSpecIds)) {
            return Collections.emptyMap();
        }
        List<InventoryBalanceRespVO> balances = stockLedgerMapper.sumQuantityByYarnSpecIdsAndWarehouseIds(yarnSpecIds, warehouseIds);
        if (CollUtil.isEmpty(balances)) {
            return Collections.emptyMap();
        }
        return balances.stream()
                .filter(b -> b.getYarnSpecId() != null)
                .collect(Collectors.toMap(
                        InventoryBalanceRespVO::getYarnSpecId,
                        b -> b.getQuantity() != null ? b.getQuantity() : BigDecimal.ZERO,
                        BigDecimal::add
                ));
    }

    private Map<Long, BigDecimal> batchLoadInTransitQuantities(Collection<Long> yarnSpecIds) {
        if (CollUtil.isEmpty(yarnSpecIds)) {
            return Collections.emptyMap();
        }
        List<YarnOrderItemDO> inTransitList = yarnOrderItemMapper.sumInTransitQtyByYarnSpecIds(yarnSpecIds);
        if (CollUtil.isEmpty(inTransitList)) {
            return Collections.emptyMap();
        }
        return inTransitList.stream()
                .filter(t -> t.getYarnSpecId() != null)
                .collect(Collectors.toMap(
                        YarnOrderItemDO::getYarnSpecId,
                        t -> t.getPurchaseQty() != null ? t.getPurchaseQty() : BigDecimal.ZERO,
                        BigDecimal::add
                ));
    }

    private BigDecimal clampToZero(BigDecimal val) {
        if (val == null || val.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return val.setScale(2, RoundingMode.HALF_UP);
    }

    private MaterialCheckMissingOrderVO buildMissingOrderVO(WeavingOrderDO order, String reason) {
        MaterialCheckMissingOrderVO vo = new MaterialCheckMissingOrderVO();
        vo.setWeavingOrderId(order.getId());
        vo.setWeavingOrderCode(order.getCode());
        vo.setProductId(order.getProductId());
        vo.setProductCode(order.getProductCode());
        vo.setProductName(order.getProductName());
        vo.setPlannedQty(order.getPlannedQty());
        vo.setPlannedWeight(order.getPlannedWeight() != null ? order.getPlannedWeight().setScale(2, RoundingMode.HALF_UP) : null);
        vo.setReason(reason);
        return vo;
    }

}
