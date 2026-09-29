package com.luna.lunasrv.module.lunaerp.service.production.productionprogress;

import cn.hutool.core.collection.CollUtil;
import com.luna.lunasrv.framework.common.pojo.PageParam;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.common.util.object.BeanUtils;
import com.luna.lunasrv.module.lunaerp.controller.production.greigedispatch.vo.GreigeDispatchRecordRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo.ProductionProgressDetailRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo.ProductionProgressPageItemRespVO;
import com.luna.lunasrv.module.lunaerp.controller.production.productionprogress.vo.ProductionProgressPageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.color.DyeingColorDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigedispatch.GreigeDispatchRecordDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.item.InspectionOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.progressfeedback.ProgressFeedbackDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.WeavingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color.DyeingColorMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigedispatch.GreigeDispatchRecordMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.item.InspectionOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.progressfeedback.ProgressFeedbackMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.WeavingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import com.luna.lunasrv.module.system.api.user.AdminUserApi;
import com.luna.lunasrv.module.system.api.user.dto.AdminUserRespDTO;
import jakarta.annotation.Resource;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.luna.lunasrv.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PRODUCTION_PROGRESS_CONTRACT_NOT_EXISTS;

/**
 * 生产管理-生产进度查询 Service 实现类（只读聚合视图，ADR-0034）
 *
 * <p>核心口径（严格落地设计文档 contract-progress-architecture-design 4.2/4.3/4.4）：</p>
 * <ol>
 *   <li>合同为唯一锚点，按页内合同 ID 集合逐环节批量聚合（IN，无 N+1）；</li>
 *   <li>join 反查流水：入库/发货量按 related_doc_type + related_doc_id 关联源单取合同，不给 ledger 加列；</li>
 *   <li>发染量从发染记录表直聚（经胚布单映射合同，ADR-0033），不走流水；</li>
 *   <li>只聚合 KG（合同/明细/流水单位非 KG 不进合计，M 原样展示不折算）；</li>
 *   <li>一律排除 CANCELLED（合同与各执行单据同理）；</li>
 *   <li>胚布/成品入库按入库单 greigeOrderId / dyeingOrderId 非空区分；</li>
 *   <li>染色覆盖率/缸状态/延期复用进度反馈口径（ADR-0031，缸交期取染色单交货日期）；</li>
 *   <li>预警三条：临期（交期≤7天未发完）/ 超期（交期已过未发完）/ 延期缸。</li>
 * </ol>
 *
 * <p>所有 Mapper 批量方法返回未加工 DO，过滤/分组/求和/排序/预警全部在本类内存完成，
 * 保证 page 与 get 单一口径、且可用纯 Mockito 单测覆盖（无需初始化 TableInfo）。</p>
 *
 * @author luna
 */
@Service
@Validated
@Slf4j
public class ProductionProgressQueryServiceImpl implements ProductionProgressQueryService {

    @Resource
    private SalesContractMapper salesContractMapper;
    @Resource
    private SalesContractItemMapper salesContractItemMapper;
    @Resource
    private GreigeOrderMapper greigeOrderMapper;
    @Resource
    private WeavingOrderMapper weavingOrderMapper;
    @Resource
    private GreigeDispatchRecordMapper greigeDispatchRecordMapper;
    @Resource
    private DyeingOrderMapper dyeingOrderMapper;
    @Resource
    private DyeingColorMapper dyeingColorMapper;
    @Resource
    private ProgressFeedbackMapper progressFeedbackMapper;
    @Resource
    private InspectionOrderMapper inspectionOrderMapper;
    @Resource
    private InspectionOrderItemMapper inspectionOrderItemMapper;
    @Resource
    private StockInMapper stockInMapper;
    @Resource
    private StockLedgerMapper stockLedgerMapper;
    @Resource
    private ShipmentMapper shipmentMapper;
    @Resource
    private AdminUserApi adminUserApi;

    /**
     * 当前日期提供器（默认 LocalDate::now，支持单测注入固定时钟）
     */
    @Setter
    private Supplier<LocalDate> todayProvider = LocalDate::now;

    // ==================== 口径常量 ====================

    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String UNIT_KG = "KG";
    private static final String MODE_SELF = "SELF";

    /** 库存流水关联单据类型 / 业务动作类型（与 StockInServiceImpl / ShipmentServiceImpl 写入常量一致） */
    private static final String DOC_TYPE_STOCK_IN = "STOCK_IN";
    private static final String TXN_TYPE_STOCK_IN = "STOCK_IN";
    private static final String DOC_TYPE_SHIPMENT = "SHIPMENT";
    private static final String TXN_TYPE_SHIPMENT_OUT = "SHIPMENT_OUT";

    /** 预警标签与临期天数阈值 */
    private static final String ALERT_NEAR = "临期";
    private static final String ALERT_OVERDUE = "超期";
    private static final String ALERT_DELAYED_VAT = "延期缸";
    private static final long NEAR_DELIVERY_DAYS = 7L;

    // ==================== 对外接口 ====================

    @Override
    public PageResult<ProductionProgressPageItemRespVO> getProductionProgressPage(ProductionProgressPageReqVO reqVO) {
        LocalDate today = todayProvider.get();
        boolean includeCancelled = Boolean.TRUE.equals(reqVO.getIncludeCancelled());

        // 1. 候选合同（Mapper 下推硬过滤）+ 内存双重防御过滤（软删/状态/交期/关键字）
        List<SalesContractDO> candidates = salesContractMapper.selectListForProgress(
                reqVO.getKeyword(), reqVO.getDeliveryDateStart(), reqVO.getDeliveryDateEnd(),
                reqVO.getStatus(), includeCancelled);
        List<SalesContractDO> contracts = filterContracts(candidates, reqVO, includeCancelled);
        if (CollUtil.isEmpty(contracts)) {
            return PageResult.empty();
        }

        // 2. 排序：交期升序、无交期排最后，同期按合同ID升序（稳定）
        contracts.sort(Comparator
                .comparing(SalesContractDO::getDeliveryDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SalesContractDO::getId, Comparator.nullsLast(Comparator.naturalOrder())));

        // 3. 批量聚合全部候选（预警需聚合后才能判定，故先聚合再按 alertOnly 过滤、最后切片分页）
        Map<Long, ContractAggregate> aggMap = aggregate(contracts, today);

        // 4. 组装列表项（业务员姓名批量解析，避免 N+1）
        List<ProductionProgressPageItemRespVO> items = buildPageItems(contracts, aggMap);

        // 5. 仅看有预警
        if (Boolean.TRUE.equals(reqVO.getAlertOnly())) {
            items = items.stream()
                    .filter(i -> i.getAlertCount() != null && i.getAlertCount() > 0)
                    .collect(Collectors.toList());
        }

        // 6. 内存切片分页
        return paginate(items, reqVO);
    }

    @Override
    public ProductionProgressDetailRespVO getProductionProgress(Long contractId) {
        SalesContractDO contract = salesContractMapper.selectById(contractId);
        if (contract == null || Boolean.TRUE.equals(contract.getDeleted())) {
            throw exception(PRODUCTION_PROGRESS_CONTRACT_NOT_EXISTS);
        }
        LocalDate today = todayProvider.get();

        // 复用同一聚合口径（单合同即单元素列表），保证 page/get 无第二套口径
        Map<Long, ContractAggregate> aggMap = aggregate(Collections.singletonList(contract), today);
        ContractAggregate agg = aggMap.getOrDefault(contractId, new ContractAggregate());

        ProductionProgressDetailRespVO vo = new ProductionProgressDetailRespVO();
        vo.setContractId(contract.getId());
        vo.setContractNo(contract.getContractNo());
        vo.setCustomerName(contract.getClientName());
        vo.setSalespersonName(resolveSalespersonName(contract.getSalespersonUserId()));
        vo.setDeliveryDate(contract.getDeliveryDate());
        vo.setStatus(contract.getStatus());
        vo.setUnit(contract.getUnit());

        // 胚布环节
        ProductionProgressDetailRespVO.GreigeSection greige = new ProductionProgressDetailRespVO.GreigeSection();
        greige.setDemandKg(scale2(agg.demandKg));
        greige.setPlannedKg(scale2(agg.greigePlannedKg));
        greige.setStockedInKg(scale2(agg.greigeInKg));
        greige.setSentToDyeingKg(scale2(agg.sentToDyeingKg));
        greige.setSelfWeaving(agg.selfWeaving);
        greige.setWeavingPlannedPieces(agg.weavingPlannedPieces);
        greige.setWeavingCompletedPieces(agg.weavingCompletedPieces);
        vo.setGreige(greige);

        // 染色环节
        ProductionProgressDetailRespVO.DyeingSection dyeing = new ProductionProgressDetailRespVO.DyeingSection();
        dyeing.setDemandKg(scale2(agg.demandKg));
        dyeing.setPlannedKg(scale2(agg.dyeingPlannedKg));
        dyeing.setVatCoverage(agg.vatCoverage);
        dyeing.setVatStatusSummary(agg.vatStatusSummary);
        dyeing.setDelayedVatCount(agg.delayedVatCount);
        vo.setDyeing(dyeing);

        // 检验环节
        ProductionProgressDetailRespVO.InspectionSection inspection = new ProductionProgressDetailRespVO.InspectionSection();
        inspection.setTotalItems(agg.inspectionTotalItems);
        inspection.setGradeSummary(agg.gradeSummary);
        vo.setInspection(inspection);

        // 入库环节
        ProductionProgressDetailRespVO.StockSection stock = new ProductionProgressDetailRespVO.StockSection();
        stock.setGreigeInKg(scale2(agg.greigeInKg));
        stock.setFinishedInKg(scale2(agg.finishedInKg));
        vo.setStock(stock);

        // 出货环节
        ProductionProgressDetailRespVO.ShipmentSection shipment = new ProductionProgressDetailRespVO.ShipmentSection();
        shipment.setShippedKg(scale2(agg.shippedKg));
        shipment.setUnshippedKg(scale2(agg.unshippedKg));
        shipment.setShipmentCount(agg.shipmentCount);
        vo.setShipment(shipment);

        vo.setAlerts(agg.alerts);

        // 发染记录下钻（按发染日期、ID 倒序）
        List<GreigeDispatchRecordDO> records = new ArrayList<>(agg.dispatchRecords);
        records.sort(Comparator
                .comparing(GreigeDispatchRecordDO::getDispatchDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(GreigeDispatchRecordDO::getId, Comparator.nullsLast(Comparator.reverseOrder())));
        vo.setGreigeDispatchRecords(BeanUtils.toBean(records, GreigeDispatchRecordRespVO.class));
        return vo;
    }

    // ==================== 聚合主流程 ====================

    /**
     * 按合同集合逐环节批量聚合（IN 批查，无 N+1）。返回 contractId → 聚合结果。
     */
    private Map<Long, ContractAggregate> aggregate(List<SalesContractDO> contracts, LocalDate today) {
        Map<Long, ContractAggregate> result = new LinkedHashMap<>();
        Map<Long, SalesContractDO> contractMap = new HashMap<>();
        for (SalesContractDO c : contracts) {
            if (c == null || c.getId() == null) {
                continue;
            }
            result.put(c.getId(), new ContractAggregate());
            contractMap.put(c.getId(), c);
        }
        List<Long> contractIds = new ArrayList<>(result.keySet());
        if (contractIds.isEmpty()) {
            return result;
        }

        aggregateDemand(contractIds, contractMap, result);
        Map<Long, Long> greigeOrderToContract = aggregateGreige(contractIds, result);
        aggregateWeaving(greigeOrderToContract, result);
        aggregateDispatch(greigeOrderToContract, result);
        aggregateDyeing(contractIds, result, today);
        aggregateInspection(contractIds, result);
        aggregateStockIn(contractIds, result);
        aggregateShipment(contractIds, result);

        // 收尾：覆盖率、未发量、预警
        for (Map.Entry<Long, ContractAggregate> entry : result.entrySet()) {
            ContractAggregate agg = entry.getValue();
            agg.vatCoverage = computeCoverage(agg.vatCoverageNumerator, agg.vatCoverageDenominator);
            agg.unshippedKg = agg.demandKg.subtract(agg.shippedKg);
            agg.alerts = computeAlerts(contractMap.get(entry.getKey()), agg, today);
        }
        return result;
    }

    /** 环节①合同需求量：仅 KG 单位合同累加明细数量，M 单位合同 demandKg=0（明细原样展示不折算）。 */
    private void aggregateDemand(List<Long> contractIds, Map<Long, SalesContractDO> contractMap,
                                 Map<Long, ContractAggregate> result) {
        List<SalesContractItemDO> items = salesContractItemMapper.selectListByContractIds(contractIds);
        for (SalesContractItemDO item : items) {
            if (isDeleted(item.getDeleted())) {
                continue;
            }
            SalesContractDO contract = contractMap.get(item.getContractId());
            ContractAggregate agg = result.get(item.getContractId());
            if (contract == null || agg == null || item.getQuantity() == null) {
                continue;
            }
            if (UNIT_KG.equalsIgnoreCase(contract.getUnit())) {
                agg.demandKg = agg.demandKg.add(item.getQuantity());
            }
        }
    }

    /** 环节②胚布：计划投胚量合计（排除 CANCELLED）；建立胚布单→合同映射；标记 SELF 自织单。 */
    private Map<Long, Long> aggregateGreige(List<Long> contractIds, Map<Long, ContractAggregate> result) {
        Map<Long, Long> greigeOrderToContract = new HashMap<>();
        List<GreigeOrderDO> greigeOrders = greigeOrderMapper.selectListByContractIds(contractIds);
        for (GreigeOrderDO g : greigeOrders) {
            if (isDeleted(g.getDeleted()) || isCancelled(g.getStatus()) || g.getSalesContractId() == null) {
                continue;
            }
            ContractAggregate agg = result.get(g.getSalesContractId());
            if (agg == null) {
                continue;
            }
            greigeOrderToContract.put(g.getId(), g.getSalesContractId());
            agg.activeGreigeOrderIds.add(g.getId());
            if (g.getPlannedQty() != null) {
                agg.greigePlannedKg = agg.greigePlannedKg.add(g.getPlannedQty());
            }
            if (MODE_SELF.equalsIgnoreCase(g.getProductionMode())) {
                agg.selfWeaving = true;
                agg.selfGreigeOrderIds.add(g.getId());
            }
        }
        return greigeOrderToContract;
    }

    /** 环节②-自织进度：仅 SELF 胚布单关联织造单，复用既有只读口径（Σ计划匹数；COMPLETED 计完工匹数）。 */
    private void aggregateWeaving(Map<Long, Long> greigeOrderToContract, Map<Long, ContractAggregate> result) {
        List<Long> selfGreigeIds = result.values().stream()
                .flatMap(a -> a.selfGreigeOrderIds.stream())
                .collect(Collectors.toList());
        if (selfGreigeIds.isEmpty()) {
            return;
        }
        List<WeavingOrderDO> weavingOrders = weavingOrderMapper.selectListByGreigeOrderIds(selfGreigeIds);
        for (WeavingOrderDO w : weavingOrders) {
            if (isDeleted(w.getDeleted()) || isCancelled(w.getStatus()) || w.getPlannedQty() == null) {
                continue;
            }
            Long contractId = greigeOrderToContract.get(w.getGreigeOrderId());
            ContractAggregate agg = contractId != null ? result.get(contractId) : null;
            if (agg == null) {
                continue;
            }
            agg.weavingPlannedPieces += w.getPlannedQty();
            if (STATUS_COMPLETED.equals(w.getStatus())) {
                agg.weavingCompletedPieces += w.getPlannedQty();
            }
        }
    }

    /** 环节②-发染量：从发染记录表直聚（经胚布单映射合同，ADR-0033），并留存记录供 get 下钻。 */
    private void aggregateDispatch(Map<Long, Long> greigeOrderToContract, Map<Long, ContractAggregate> result) {
        List<Long> activeGreigeIds = result.values().stream()
                .flatMap(a -> a.activeGreigeOrderIds.stream())
                .collect(Collectors.toList());
        if (activeGreigeIds.isEmpty()) {
            return;
        }
        List<GreigeDispatchRecordDO> records = greigeDispatchRecordMapper.selectListByGreigeOrderIds(activeGreigeIds);
        for (GreigeDispatchRecordDO d : records) {
            if (isDeleted(d.getDeleted())) {
                continue;
            }
            Long contractId = greigeOrderToContract.get(d.getGreigeOrderId());
            ContractAggregate agg = contractId != null ? result.get(contractId) : null;
            if (agg == null) {
                continue;
            }
            agg.dispatchRecords.add(d);
            if (d.getQuantity() != null) {
                agg.sentToDyeingKg = agg.sentToDyeingKg.add(d.getQuantity());
            }
        }
    }

    /** 环节③染色：投染量（KG 行）、覆盖率分母（颜色行数量）、覆盖率分子/缸状态/延期缸（进度反馈口径）。 */
    private void aggregateDyeing(List<Long> contractIds, Map<Long, ContractAggregate> result, LocalDate today) {
        Map<Long, Long> dyeingOrderToContract = new HashMap<>();
        Map<Long, DyeingOrderDO> dyeingOrderMap = new HashMap<>();
        List<DyeingOrderDO> dyeingOrders = dyeingOrderMapper.selectListByContractIds(contractIds);
        for (DyeingOrderDO d : dyeingOrders) {
            if (isDeleted(d.getDeleted()) || isCancelled(d.getStatus()) || d.getSalesContractId() == null) {
                continue;
            }
            if (!result.containsKey(d.getSalesContractId())) {
                continue;
            }
            dyeingOrderToContract.put(d.getId(), d.getSalesContractId());
            dyeingOrderMap.put(d.getId(), d);
        }
        if (dyeingOrderToContract.isEmpty()) {
            return;
        }
        List<Long> activeDyeingIds = new ArrayList<>(dyeingOrderToContract.keySet());

        // 颜色行：投染量（KG）+ 覆盖率分母（本单染色数量）
        List<DyeingColorDO> colors = dyeingColorMapper.selectListByOrderIds(activeDyeingIds);
        for (DyeingColorDO color : colors) {
            if (isDeleted(color.getDeleted())) {
                continue;
            }
            Long contractId = dyeingOrderToContract.get(color.getDyeingOrderId());
            ContractAggregate agg = contractId != null ? result.get(contractId) : null;
            if (agg == null) {
                continue;
            }
            if (UNIT_KG.equalsIgnoreCase(color.getDyeingInputUnit()) && color.getDyeingInputQty() != null) {
                agg.dyeingPlannedKg = agg.dyeingPlannedKg.add(color.getDyeingInputQty());
            }
            if (color.getQuantity() != null) {
                agg.vatCoverageDenominator = agg.vatCoverageDenominator.add(color.getQuantity());
            }
        }

        // 反馈缸：覆盖率分子 + 缸状态汇总 + 延期缸（延期判定复用 ProgressFeedbackServiceImpl 口径）
        List<ProgressFeedbackDO> vats = progressFeedbackMapper.selectListByDyeingOrderIds(activeDyeingIds);
        for (ProgressFeedbackDO vat : vats) {
            if (isDeleted(vat.getDeleted())) {
                continue;
            }
            Long contractId = dyeingOrderToContract.get(vat.getDyeingOrderId());
            ContractAggregate agg = contractId != null ? result.get(contractId) : null;
            if (agg == null) {
                continue;
            }
            if (vat.getQuantity() != null) {
                agg.vatCoverageNumerator = agg.vatCoverageNumerator.add(vat.getQuantity());
            }
            if (vat.getStatus() != null) {
                agg.vatStatusSummary.merge(vat.getStatus(), 1, Integer::sum);
            }
            DyeingOrderDO order = dyeingOrderMap.get(vat.getDyeingOrderId());
            if (order != null && isDelayed(order.getDeliveryDate(), vat.getStatus(), today)) {
                agg.delayedVatCount++;
            }
        }
    }

    /** 环节④检验：录检明细总数 + 等级汇总（排除 CANCELLED 检验单）。 */
    private void aggregateInspection(List<Long> contractIds, Map<Long, ContractAggregate> result) {
        Map<Long, Long> inspectionOrderToContract = new HashMap<>();
        List<InspectionOrderDO> inspectionOrders = inspectionOrderMapper.selectListByContractIds(contractIds);
        for (InspectionOrderDO io : inspectionOrders) {
            if (isDeleted(io.getDeleted()) || isCancelled(io.getStatus()) || io.getSalesContractId() == null) {
                continue;
            }
            if (result.containsKey(io.getSalesContractId())) {
                inspectionOrderToContract.put(io.getId(), io.getSalesContractId());
            }
        }
        if (inspectionOrderToContract.isEmpty()) {
            return;
        }
        List<InspectionOrderItemDO> items =
                inspectionOrderItemMapper.selectListByInspectionOrderIds(new ArrayList<>(inspectionOrderToContract.keySet()));
        for (InspectionOrderItemDO item : items) {
            if (isDeleted(item.getDeleted())) {
                continue;
            }
            Long contractId = inspectionOrderToContract.get(item.getInspectionOrderId());
            ContractAggregate agg = contractId != null ? result.get(contractId) : null;
            if (agg == null) {
                continue;
            }
            agg.inspectionTotalItems++;
            if (item.getGrade() != null) {
                agg.gradeSummary.merge(item.getGrade(), 1, Integer::sum);
            }
        }
    }

    /** 环节⑤入库：join STOCK_IN 流水（仅 KG），按入库单 greigeOrderId/dyeingOrderId 非空区分胚布/成品。 */
    private void aggregateStockIn(List<Long> contractIds, Map<Long, ContractAggregate> result) {
        Map<Long, StockInDO> stockInMap = new HashMap<>();
        List<StockInDO> stockIns = stockInMapper.selectListByContractIds(contractIds);
        for (StockInDO si : stockIns) {
            // 排除 CANCELLED：整单剔除（含其正/红冲流水），避免红冲双计；DRAFT 无流水自然为 0
            if (isDeleted(si.getDeleted()) || isCancelled(si.getStatus()) || si.getSalesContractId() == null) {
                continue;
            }
            if (result.containsKey(si.getSalesContractId())) {
                stockInMap.put(si.getId(), si);
            }
        }
        if (stockInMap.isEmpty()) {
            return;
        }
        List<StockLedgerDO> ledgers = stockLedgerMapper.selectListByRelatedDocIds(
                DOC_TYPE_STOCK_IN, new ArrayList<>(stockInMap.keySet()), TXN_TYPE_STOCK_IN);
        for (StockLedgerDO ledger : ledgers) {
            if (isDeleted(ledger.getDeleted()) || ledger.getQuantity() == null
                    || !UNIT_KG.equalsIgnoreCase(ledger.getUnit())) {
                continue;
            }
            StockInDO si = stockInMap.get(ledger.getRelatedDocId());
            ContractAggregate agg = si != null ? result.get(si.getSalesContractId()) : null;
            if (agg == null) {
                continue;
            }
            if (si.getGreigeOrderId() != null) {
                agg.greigeInKg = agg.greigeInKg.add(ledger.getQuantity());
            } else if (si.getDyeingOrderId() != null) {
                agg.finishedInKg = agg.finishedInKg.add(ledger.getQuantity());
            }
        }
    }

    /** 环节⑥出货：发货单数（排除 CANCELLED）+ join SHIPMENT_OUT 流水已发量（仅 KG，取绝对值）。 */
    private void aggregateShipment(List<Long> contractIds, Map<Long, ContractAggregate> result) {
        Map<Long, ShipmentDO> shipmentMap = new HashMap<>();
        List<ShipmentDO> shipments = shipmentMapper.selectListByContractIds(contractIds);
        for (ShipmentDO sh : shipments) {
            if (isDeleted(sh.getDeleted()) || isCancelled(sh.getStatus()) || sh.getSalesContractId() == null) {
                continue;
            }
            ContractAggregate agg = result.get(sh.getSalesContractId());
            if (agg == null) {
                continue;
            }
            agg.shipmentCount++;
            shipmentMap.put(sh.getId(), sh);
        }
        if (shipmentMap.isEmpty()) {
            return;
        }
        List<StockLedgerDO> ledgers = stockLedgerMapper.selectListByRelatedDocIds(
                DOC_TYPE_SHIPMENT, new ArrayList<>(shipmentMap.keySet()), TXN_TYPE_SHIPMENT_OUT);
        for (StockLedgerDO ledger : ledgers) {
            if (isDeleted(ledger.getDeleted()) || ledger.getQuantity() == null
                    || !UNIT_KG.equalsIgnoreCase(ledger.getUnit())) {
                continue;
            }
            ShipmentDO sh = shipmentMap.get(ledger.getRelatedDocId());
            ContractAggregate agg = sh != null ? result.get(sh.getSalesContractId()) : null;
            if (agg == null) {
                continue;
            }
            agg.shippedKg = agg.shippedKg.add(ledger.getQuantity().abs());
        }
    }

    // ==================== 预警与派生计算 ====================

    /**
     * 预警三条（设计文档 4.4）：① 临期（交期≤7天且未发完）② 超期（交期已过且未发完）③ 延期缸。
     * 已取消合同不产生预警；无预警返回空列表（不产生条目）。
     */
    private List<String> computeAlerts(SalesContractDO contract, ContractAggregate agg, LocalDate today) {
        List<String> alerts = new ArrayList<>();
        if (contract == null || isCancelled(contract.getStatus())) {
            return alerts;
        }
        LocalDate deliveryDate = contract.getDeliveryDate();
        boolean hasUnshipped = agg.unshippedKg.compareTo(BigDecimal.ZERO) > 0;
        if (deliveryDate != null && hasUnshipped) {
            long daysUntil = ChronoUnit.DAYS.between(today, deliveryDate);
            if (daysUntil < 0) {
                alerts.add(ALERT_OVERDUE);
            } else if (daysUntil <= NEAR_DELIVERY_DAYS) {
                alerts.add(ALERT_NEAR);
            }
        }
        if (agg.delayedVatCount > 0) {
            alerts.add(ALERT_DELAYED_VAT);
        }
        return alerts;
    }

    /**
     * 延期缸判定：染色单交货日期 < 今天 且 缸非 COMPLETED（复用 ProgressFeedbackServiceImpl 口径）。
     */
    private boolean isDelayed(LocalDate deliveryDate, String vatStatus, LocalDate today) {
        if (deliveryDate == null || STATUS_COMPLETED.equals(vatStatus)) {
            return false;
        }
        return deliveryDate.isBefore(today);
    }

    /**
     * 排产覆盖率 = Σ缸数量 / Σ颜色行染色数量 × 100（分母≤0 记 0.00；4 位 HALF_UP 后 ×100 再 2 位 HALF_UP）。
     */
    private BigDecimal computeCoverage(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal safeNumerator = numerator != null ? numerator : BigDecimal.ZERO;
        return safeNumerator.divide(denominator, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ==================== 列表组装 / 过滤 / 分页 ====================

    private List<ProductionProgressPageItemRespVO> buildPageItems(List<SalesContractDO> contracts,
                                                                  Map<Long, ContractAggregate> aggMap) {
        Set<Long> userIds = contracts.stream()
                .map(SalesContractDO::getSalespersonUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);

        List<ProductionProgressPageItemRespVO> items = new ArrayList<>(contracts.size());
        for (SalesContractDO c : contracts) {
            ContractAggregate agg = aggMap.getOrDefault(c.getId(), new ContractAggregate());
            ProductionProgressPageItemRespVO vo = new ProductionProgressPageItemRespVO();
            vo.setContractId(c.getId());
            vo.setContractNo(c.getContractNo());
            vo.setCustomerName(c.getClientName());
            AdminUserRespDTO user = c.getSalespersonUserId() != null ? userMap.get(c.getSalespersonUserId()) : null;
            vo.setSalespersonName(user != null ? user.getNickname() : null);
            vo.setDeliveryDate(c.getDeliveryDate());
            vo.setStatus(c.getStatus());
            vo.setUnit(c.getUnit());
            vo.setDemandKg(scale2(agg.demandKg));
            vo.setGreigePlannedKg(scale2(agg.greigePlannedKg));
            vo.setDyeingPlannedKg(scale2(agg.dyeingPlannedKg));
            vo.setSentToDyeingKg(scale2(agg.sentToDyeingKg));
            vo.setVatCoverage(agg.vatCoverage);
            vo.setGreigeInKg(scale2(agg.greigeInKg));
            vo.setFinishedInKg(scale2(agg.finishedInKg));
            vo.setShippedKg(scale2(agg.shippedKg));
            vo.setUnshippedKg(scale2(agg.unshippedKg));
            vo.setShipmentCount(agg.shipmentCount);
            vo.setAlerts(agg.alerts);
            vo.setAlertCount(agg.alerts.size());
            items.add(vo);
        }
        return items;
    }

    /**
     * 内存双重防御过滤：软删、状态（有 status 等值；否则按 includeCancelled 决定是否排除 CANCELLED）、
     * 交期范围（起/止含端点，无交期不满足范围条件而排除）、关键字（合同号/客户名，忽略大小写）。
     */
    private List<SalesContractDO> filterContracts(List<SalesContractDO> candidates,
                                                  ProductionProgressPageReqVO reqVO, boolean includeCancelled) {
        if (CollUtil.isEmpty(candidates)) {
            return Collections.emptyList();
        }
        String status = trimToNull(reqVO.getStatus());
        String lowerKeyword = reqVO.getKeyword() != null ? reqVO.getKeyword().trim().toLowerCase() : null;
        LocalDate from = reqVO.getDeliveryDateStart();
        LocalDate to = reqVO.getDeliveryDateEnd();
        return candidates.stream()
                .filter(c -> c != null && c.getId() != null && !isDeleted(c.getDeleted()))
                .filter(c -> status != null ? status.equals(c.getStatus())
                        : (includeCancelled || !isCancelled(c.getStatus())))
                .filter(c -> from == null || (c.getDeliveryDate() != null && !c.getDeliveryDate().isBefore(from)))
                .filter(c -> to == null || (c.getDeliveryDate() != null && !c.getDeliveryDate().isAfter(to)))
                .filter(c -> !StringUtils.hasText(lowerKeyword) || matchesKeyword(c, lowerKeyword))
                .collect(Collectors.toList());
    }

    private boolean matchesKeyword(SalesContractDO c, String lowerKeyword) {
        return (c.getContractNo() != null && c.getContractNo().toLowerCase().contains(lowerKeyword))
                || (c.getClientName() != null && c.getClientName().toLowerCase().contains(lowerKeyword));
    }

    private PageResult<ProductionProgressPageItemRespVO> paginate(List<ProductionProgressPageItemRespVO> items,
                                                                  ProductionProgressPageReqVO reqVO) {
        long total = items.size();
        Integer pageSize = reqVO.getPageSize();
        // 不分页（导出等场景）：返回全量
        if (pageSize != null && PageParam.PAGE_SIZE_NONE.equals(pageSize)) {
            return new PageResult<>(items, total);
        }
        int pageNo = reqVO.getPageNo() != null && reqVO.getPageNo() > 0 ? reqVO.getPageNo() : 1;
        int size = pageSize != null && pageSize > 0 ? pageSize : 10;
        // 用 long 计算起始下标，避免 (pageNo-1)*size 极端值下 int 溢出为负
        long fromIndexLong = (long) (pageNo - 1) * size;
        if (fromIndexLong >= total) {
            return new PageResult<>(Collections.emptyList(), total);
        }
        int fromIndex = (int) fromIndexLong;
        int toIndex = Math.min(fromIndex + size, (int) total);
        return new PageResult<>(new ArrayList<>(items.subList(fromIndex, toIndex)), total);
    }

    private String resolveSalespersonName(Long salespersonUserId) {
        if (salespersonUserId == null) {
            return null;
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(Collections.singleton(salespersonUserId));
        AdminUserRespDTO user = userMap.get(salespersonUserId);
        return user != null ? user.getNickname() : null;
    }

    // ==================== 通用小工具 ====================

    private static boolean isCancelled(String status) {
        return STATUS_CANCELLED.equals(status);
    }

    private static boolean isDeleted(Boolean deleted) {
        return Boolean.TRUE.equals(deleted);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String trimmed = s.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static BigDecimal scale2(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 单合同聚合中间态（不落库，仅内存传递给 VO 组装）。
     */
    private static class ContractAggregate {
        private BigDecimal demandKg = BigDecimal.ZERO;
        private BigDecimal greigePlannedKg = BigDecimal.ZERO;
        private BigDecimal dyeingPlannedKg = BigDecimal.ZERO;
        private BigDecimal sentToDyeingKg = BigDecimal.ZERO;
        private BigDecimal greigeInKg = BigDecimal.ZERO;
        private BigDecimal finishedInKg = BigDecimal.ZERO;
        private BigDecimal shippedKg = BigDecimal.ZERO;
        private BigDecimal unshippedKg = BigDecimal.ZERO;
        private BigDecimal vatCoverage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        private BigDecimal vatCoverageNumerator = BigDecimal.ZERO;
        private BigDecimal vatCoverageDenominator = BigDecimal.ZERO;
        private final Map<String, Integer> vatStatusSummary = new LinkedHashMap<>();
        private final Map<String, Integer> gradeSummary = new LinkedHashMap<>();
        private int delayedVatCount = 0;
        private int inspectionTotalItems = 0;
        private int shipmentCount = 0;
        private boolean selfWeaving = false;
        private int weavingPlannedPieces = 0;
        private int weavingCompletedPieces = 0;
        private final List<Long> activeGreigeOrderIds = new ArrayList<>();
        private final List<Long> selfGreigeOrderIds = new ArrayList<>();
        private final List<GreigeDispatchRecordDO> dispatchRecords = new ArrayList<>();
        private List<String> alerts = new ArrayList<>();
    }

}
