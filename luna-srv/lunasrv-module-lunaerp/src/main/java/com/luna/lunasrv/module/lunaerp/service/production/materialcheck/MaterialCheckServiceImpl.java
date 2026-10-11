package com.luna.lunasrv.module.lunaerp.service.production.materialcheck;

import com.luna.lunasrv.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckContributionVO;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.materialcheck.vo.MaterialCheckRowVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnInTransitDTO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnrecipebom.YarnRecipeBomItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarntype.YarnTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.YarnStockBalanceDTO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder.YarnOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom.YarnRecipeBomItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnrecipebom.YarnRecipeBomMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarntype.YarnTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WEAVING_ORDER_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WEAVING_ORDER_STATUS_NOT_ALLOW;

/**
 * 生产管理 - 齐套检查 Service 实现类（只读试算，ADR-0041）
 *
 * <p>核心口径：
 * <ol>
 *   <li>需求量：Σ(plannedWeight × ratio / 100) 按 yarnSpecId 跨单合并（2 位小数 HALF_UP），展开行保留单据贡献；</li>
 *   <li>无 BOM 单整单入 missingBomOrders、缺计划重量或匹数单整单入 missingWeightOrders，均不污染聚合行；</li>
 *   <li>可用库存：台账实时余额 SUM(quantity)，支持可选仓库筛选，缺省全仓合计（批量 IN + GROUP BY，禁止 N+1）；</li>
 *   <li>在途量：ACTIVE 纱线采购单 purchase_qty 合计（主子表一条 SQL 批量聚合）；</li>
 *   <li>缺口（不含在途）= max(0, 需求 - 可用)；</li>
 *   <li>缺口（含在途）= max(0, 需求 - 可用 - 在途)；</li>
 *   <li>建议采购量 = 缺口（含在途）。</li>
 * </ol>
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class MaterialCheckServiceImpl implements MaterialCheckService {

    @Resource
    private WeavingOrderMapper weavingOrderMapper;

    @Resource
    private YarnRecipeBomMapper yarnRecipeBomMapper;

    @Resource
    private YarnRecipeBomItemMapper yarnRecipeBomItemMapper;

    @Resource
    private StockLedgerMapper stockLedgerMapper;

    @Resource
    private YarnOrderItemMapper yarnOrderItemMapper;

    @Resource
    private YarnSpecMapper yarnSpecMapper;

    @Resource
    private YarnTypeMapper yarnTypeMapper;

    @Override
    public MaterialCheckRespVO runMaterialCheck(MaterialCheckReqVO reqVO) {
        if (reqVO == null) {
            reqVO = new MaterialCheckReqVO();
        }

        // 1. 加载织造单
        List<WeavingOrderDO> orders = loadWeavingOrders(reqVO.getWeavingOrderIds());
        if (orders.isEmpty()) {
            return emptyResp();
        }

        // 2. 批量加载 BOM 与配比明细
        Set<Long> productIds = orders.stream()
                .map(WeavingOrderDO::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, YarnRecipeBomDO> bomByProductId = Collections.emptyMap();
        Map<Long, List<YarnRecipeBomItemDO>> itemsByBomId = Collections.emptyMap();

        if (!productIds.isEmpty()) {
            List<YarnRecipeBomDO> boms = yarnRecipeBomMapper.selectListByProductIds(productIds);
            if (boms != null && !boms.isEmpty()) {
                bomByProductId = boms.stream()
                        .collect(Collectors.toMap(YarnRecipeBomDO::getProductId, b -> b, (b1, b2) -> b1));
                Set<Long> bomIds = boms.stream().map(YarnRecipeBomDO::getId).collect(Collectors.toSet());
                List<YarnRecipeBomItemDO> items = yarnRecipeBomItemMapper.selectListByRecipeBomIds(bomIds);
                if (items != null && !items.isEmpty()) {
                    itemsByBomId = items.stream().collect(Collectors.groupingBy(YarnRecipeBomItemDO::getRecipeBomId));
                }
            }
        }

        // 3. 区分无法试算单与有效试算单
        List<MaterialCheckRespVO.MissingOrderVO> missingBomOrders = new ArrayList<>();
        List<MaterialCheckRespVO.MissingOrderVO> missingWeightOrders = new ArrayList<>();
        Map<Long, List<MaterialCheckContributionVO>> contributionsBySpec = new LinkedHashMap<>();

        for (WeavingOrderDO order : orders) {
            boolean missingWeight = (order.getPlannedWeight() == null
                    || order.getPlannedWeight().compareTo(BigDecimal.ZERO) <= 0
                    || order.getPlannedQty() == null
                    || order.getPlannedQty() <= 0);

            YarnRecipeBomDO bom = order.getProductId() != null ? bomByProductId.get(order.getProductId()) : null;
            List<YarnRecipeBomItemDO> bomItems = bom != null ? itemsByBomId.get(bom.getId()) : null;
            boolean missingBom = (bom == null || bomItems == null || bomItems.isEmpty());

            if (missingWeight) {
                missingWeightOrders.add(toMissingOrderVO(order, "计划总重量或计划匹数为空/非正数"));
            }
            if (missingBom) {
                missingBomOrders.add(toMissingOrderVO(order, "未配置纱线配比BOM"));
            }

            // 缺 BOM 或缺重量单均不污染聚合行
            if (!missingWeight && !missingBom) {
                for (YarnRecipeBomItemDO item : bomItems) {
                    Long yarnSpecId = item.getYarnSpecId();
                    if (yarnSpecId == null) {
                        continue;
                    }
                    BigDecimal ratio = item.getRatio() != null ? item.getRatio() : BigDecimal.ZERO;
                    // 单匹计划重量 = plannedWeight / plannedQty；某纱线需求 KG = plannedWeight × ratio / 100（2位小数 HALF_UP）
                    BigDecimal demandQty = order.getPlannedWeight().multiply(ratio)
                            .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                    MaterialCheckContributionVO contribution = MaterialCheckContributionVO.builder()
                            .weavingOrderId(order.getId())
                            .weavingOrderCode(order.getCode())
                            .productId(order.getProductId())
                            .productCode(order.getProductCode())
                            .productName(order.getProductName())
                            .plannedWeight(order.getPlannedWeight())
                            .ratio(ratio)
                            .demandQty(demandQty)
                            .build();

                    contributionsBySpec.computeIfAbsent(yarnSpecId, k -> new ArrayList<>()).add(contribution);
                }
            }
        }

        // 如果没有可聚合的纱线规格行
        if (contributionsBySpec.isEmpty()) {
            return MaterialCheckRespVO.builder()
                    .rows(Collections.emptyList())
                    .missingBomOrders(missingBomOrders)
                    .missingWeightOrders(missingWeightOrders)
                    .shortageSpecCount(0)
                    .totalShortageQty(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                    .build();
        }

        // 4. 批量查询库存余额与在途量（禁止 N+1）
        Set<Long> yarnSpecIds = contributionsBySpec.keySet();

        List<YarnStockBalanceDTO> stockBalances = stockLedgerMapper.sumBalanceByYarnSpecIds(yarnSpecIds, reqVO.getWarehouseIds());
        Map<Long, BigDecimal> stockMap = (stockBalances != null)
                ? stockBalances.stream().collect(Collectors.toMap(YarnStockBalanceDTO::getYarnSpecId, YarnStockBalanceDTO::getBalance, (a, b) -> a))
                : Collections.emptyMap();

        List<YarnInTransitDTO> inTransitList = yarnOrderItemMapper.sumInTransitByYarnSpecIds(yarnSpecIds);
        Map<Long, BigDecimal> inTransitMap = (inTransitList != null)
                ? inTransitList.stream().collect(Collectors.toMap(YarnInTransitDTO::getYarnSpecId, YarnInTransitDTO::getInTransitQty, (a, b) -> a))
                : Collections.emptyMap();

        // 5. 批量查询纱线规格与材质档案
        List<YarnSpecDO> yarnSpecs = yarnSpecMapper.selectBatchIds(yarnSpecIds);
        Map<Long, YarnSpecDO> yarnSpecMap = (yarnSpecs != null)
                ? yarnSpecs.stream().collect(Collectors.toMap(YarnSpecDO::getId, s -> s, (s1, s2) -> s1))
                : Collections.emptyMap();

        Set<Long> yarnTypeIds = (yarnSpecs != null)
                ? yarnSpecs.stream().map(YarnSpecDO::getYarnTypeId).filter(Objects::nonNull).collect(Collectors.toSet())
                : Collections.emptySet();
        List<YarnTypeDO> yarnTypes = yarnTypeIds.isEmpty() ? Collections.emptyList() : yarnTypeMapper.selectBatchIds(yarnTypeIds);
        Map<Long, YarnTypeDO> yarnTypeMap = (yarnTypes != null)
                ? yarnTypes.stream().collect(Collectors.toMap(YarnTypeDO::getId, t -> t, (t1, t2) -> t1))
                : Collections.emptyMap();

        // 6. 组装聚合行
        List<MaterialCheckRowVO> rows = new ArrayList<>(yarnSpecIds.size());
        for (Map.Entry<Long, List<MaterialCheckContributionVO>> entry : contributionsBySpec.entrySet()) {
            Long yarnSpecId = entry.getKey();
            List<MaterialCheckContributionVO> contribs = entry.getValue();

            BigDecimal demandQty = contribs.stream()
                    .map(MaterialCheckContributionVO::getDemandQty)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .setScale(2, RoundingMode.HALF_UP);

            BigDecimal stockQty = stockMap.getOrDefault(yarnSpecId, BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            BigDecimal inTransitQty = inTransitMap.getOrDefault(yarnSpecId, BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);

            // 缺口（不含在途）= max(0, 需求 - 可用)
            BigDecimal shortageWithoutInTransit = demandQty.subtract(stockQty).max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            // 缺口（含在途）= max(0, 需求 - 可用 - 在途)
            BigDecimal shortageWithInTransit = demandQty.subtract(stockQty).subtract(inTransitQty).max(BigDecimal.ZERO)
                    .setScale(2, RoundingMode.HALF_UP);
            // 建议采购量 = 缺口（含在途）
            BigDecimal suggestedPurchaseQty = shortageWithInTransit;

            YarnSpecDO spec = yarnSpecMap.get(yarnSpecId);
            YarnTypeDO type = (spec != null && spec.getYarnTypeId() != null) ? yarnTypeMap.get(spec.getYarnTypeId()) : null;

            String specName = null;
            if (spec != null) {
                specName = StringUtils.hasText(spec.getSpecName()) ? spec.getSpecName() : spec.getStandardName();
            }

            MaterialCheckRowVO row = MaterialCheckRowVO.builder()
                    .yarnTypeId(spec != null ? spec.getYarnTypeId() : null)
                    .yarnTypeName(type != null ? type.getName() : null)
                    .yarnSpecId(yarnSpecId)
                    .yarnSpecName(specName)
                    .demandQty(demandQty)
                    .stockQty(stockQty)
                    .inTransitQty(inTransitQty)
                    .shortageWithoutInTransit(shortageWithoutInTransit)
                    .shortageWithInTransit(shortageWithInTransit)
                    .suggestedPurchaseQty(suggestedPurchaseQty)
                    .contributions(contribs)
                    .build();

            rows.add(row);
        }

        // 排序：优先按材质ID升序，再按规格ID升序
        rows.sort(Comparator.comparing(MaterialCheckRowVO::getYarnTypeId, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(MaterialCheckRowVO::getYarnSpecId, Comparator.nullsLast(Comparator.naturalOrder())));

        // 7. 汇总徽标计算（口径受 includeInTransit 控制）
        boolean incTransit = Boolean.TRUE.equals(reqVO.getIncludeInTransit());
        long shortageSpecCount = rows.stream()
                .filter(r -> (incTransit ? r.getShortageWithInTransit() : r.getShortageWithoutInTransit()).compareTo(BigDecimal.ZERO) > 0)
                .count();

        BigDecimal totalShortageQty = rows.stream()
                .map(r -> incTransit ? r.getShortageWithInTransit() : r.getShortageWithoutInTransit())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return MaterialCheckRespVO.builder()
                .rows(rows)
                .missingBomOrders(missingBomOrders)
                .missingWeightOrders(missingWeightOrders)
                .shortageSpecCount((int) shortageSpecCount)
                .totalShortageQty(totalShortageQty)
                .build();
    }

    private List<WeavingOrderDO> loadWeavingOrders(List<Long> weavingOrderIds) {
        if (weavingOrderIds != null && !weavingOrderIds.isEmpty()) {
            List<WeavingOrderDO> orders = weavingOrderMapper.selectBatchIds(weavingOrderIds);
            if (orders == null || orders.size() != new HashSet<>(weavingOrderIds).size()) {
                throw exception(WEAVING_ORDER_NOT_EXISTS);
            }
            for (WeavingOrderDO order : orders) {
                if (!"DRAFT".equals(order.getStatus()) && !"ACTIVE".equals(order.getStatus())) {
                    throw exception(WEAVING_ORDER_STATUS_NOT_ALLOW,
                            "织造单【" + order.getCode() + "】状态不允许试算，仅支持待排产（DRAFT）或生产中（ACTIVE）");
                }
            }
            return orders;
        }

        // 缺省查全部 DRAFT / ACTIVE 织造单
        return weavingOrderMapper.selectActiveOrDraftList();
    }

    private MaterialCheckRespVO.MissingOrderVO toMissingOrderVO(WeavingOrderDO order, String reason) {
        return MaterialCheckRespVO.MissingOrderVO.builder()
                .weavingOrderId(order.getId())
                .weavingOrderCode(order.getCode())
                .productId(order.getProductId())
                .productCode(order.getProductCode())
                .productName(order.getProductName())
                .plannedQty(order.getPlannedQty())
                .plannedWeight(order.getPlannedWeight())
                .reason(reason)
                .build();
    }

    private MaterialCheckRespVO emptyResp() {
        return MaterialCheckRespVO.builder()
                .rows(Collections.emptyList())
                .missingBomOrders(Collections.emptyList())
                .missingWeightOrders(Collections.emptyList())
                .shortageSpecCount(0)
                .totalShortageQty(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP))
                .build();
    }

}
