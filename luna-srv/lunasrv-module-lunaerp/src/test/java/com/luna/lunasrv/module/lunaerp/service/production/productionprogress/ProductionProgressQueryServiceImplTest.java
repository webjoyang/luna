package com.luna.lunasrv.module.lunaerp.service.production.productionprogress;

import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PRODUCTION_PROGRESS_CONTRACT_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link ProductionProgressQueryServiceImpl} 纯 Mockito 单元测试（issue 03 聚合口径）。
 *
 * <p>Service 只做内存过滤/分组/求和/排序/预警，全部批量 Mapper 方法被 mock 返回未加工 DO，
 * 因此无需初始化 MyBatis TableInfo。覆盖 7 条验收标准：</p>
 * <ol>
 *   <li>page 数量与单据侧一致（同一数据源聚合）</li>
 *   <li>join 反查取合同（入库/发货按流水关联源单、发染量从记录表）</li>
 *   <li>KG 过滤生效、CANCELLED 排除</li>
 *   <li>三条预警边界（≤7 临期、已过超期、延期缸、无预警不产生条目）</li>
 *   <li>排序与筛选（交期升序 nulls last、关键字、仅看预警）</li>
 *   <li>get 六环节完整结构与下钻</li>
 *   <li>合同不存在报错 1-030-019-000</li>
 * </ol>
 *
 * @author luna
 */
class ProductionProgressQueryServiceImplTest {

    @Mock private SalesContractMapper salesContractMapper;
    @Mock private SalesContractItemMapper salesContractItemMapper;
    @Mock private GreigeOrderMapper greigeOrderMapper;
    @Mock private WeavingOrderMapper weavingOrderMapper;
    @Mock private GreigeDispatchRecordMapper greigeDispatchRecordMapper;
    @Mock private DyeingOrderMapper dyeingOrderMapper;
    @Mock private DyeingColorMapper dyeingColorMapper;
    @Mock private ProgressFeedbackMapper progressFeedbackMapper;
    @Mock private InspectionOrderMapper inspectionOrderMapper;
    @Mock private InspectionOrderItemMapper inspectionOrderItemMapper;
    @Mock private StockInMapper stockInMapper;
    @Mock private StockLedgerMapper stockLedgerMapper;
    @Mock private ShipmentMapper shipmentMapper;
    @Mock private AdminUserApi adminUserApi;

    @InjectMocks
    private ProductionProgressQueryServiceImpl service;

    /** 固定当天，保证临期/超期/延期判定绝对确定 */
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 28);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service.setTodayProvider(() -> TODAY);
    }

    // ==================== 验收①⑤：空列表 / 排序 / 分页 ====================

    @Test
    @DisplayName("无候选合同时返回空分页（total=0）")
    void pageReturnsEmptyWhenNoContracts() {
        when(salesContractMapper.selectListForProgress(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(Collections.emptyList());

        PageResult<ProductionProgressPageItemRespVO> page = service.getProductionProgressPage(req());

        assertEquals(0L, page.getTotal());
        assertTrue(page.getList().isEmpty());
    }

    @Test
    @DisplayName("排序：交期升序、无交期排最后、同期按合同ID升序")
    void sortingByDeliveryDateNullsLast() {
        SalesContractDO c1 = contract(1L, "CH1", "KG", "ACTIVE", TODAY.plusDays(10), null);
        SalesContractDO c2 = contract(2L, "CH2", "KG", "ACTIVE", TODAY.plusDays(2), null);
        SalesContractDO c3 = contract(3L, "CH3", "KG", "ACTIVE", null, null);
        SalesContractDO c4 = contract(4L, "CH4", "KG", "ACTIVE", TODAY.plusDays(2), null);
        when(salesContractMapper.selectListForProgress(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new ArrayList<>(Arrays.asList(c1, c2, c3, c4)));

        List<ProductionProgressPageItemRespVO> list = service.getProductionProgressPage(req()).getList();

        // c2/c4 同为 +2 天（按ID升序 c2 先），再 c1(+10)，最后 c3(无交期)
        assertEquals(Arrays.asList(2L, 4L, 1L, 3L),
                list.stream().map(ProductionProgressPageItemRespVO::getContractId).collect(Collectors.toList()));
    }

    @Test
    @DisplayName("分页：total 为过滤后全量，列表按 pageNo/pageSize 内存切片")
    void paginationSlicesInMemory() {
        List<SalesContractDO> contracts = new ArrayList<>();
        for (long i = 1; i <= 5; i++) {
            contracts.add(contract(i, "CH" + i, "KG", "ACTIVE", TODAY.plusDays(i), null));
        }
        when(salesContractMapper.selectListForProgress(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(contracts);

        ProductionProgressPageReqVO reqVO = req();
        reqVO.setPageNo(2);
        reqVO.setPageSize(2);
        PageResult<ProductionProgressPageItemRespVO> page = service.getProductionProgressPage(reqVO);

        assertEquals(5L, page.getTotal());
        assertEquals(2, page.getList().size());
        // 交期升序 → id 1..5；第 2 页取 id 3、4
        assertEquals(Arrays.asList(3L, 4L),
                page.getList().stream().map(ProductionProgressPageItemRespVO::getContractId).collect(Collectors.toList()));
    }

    @Test
    @DisplayName("筛选：关键字（合同号/客户名）与仅看预警生效")
    void keywordAndAlertOnlyFilters() {
        SalesContractDO c1 = contract(1L, "CH2026A", "KG", "ACTIVE", TODAY.minusDays(1), null); // 超期预警
        c1.setClientName("甲纺织");
        SalesContractDO c2 = contract(2L, "CH2026B", "KG", "ACTIVE", TODAY.plusDays(60), null); // 无预警
        c2.setClientName("乙面料");
        when(salesContractMapper.selectListForProgress(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new ArrayList<>(Arrays.asList(c1, c2)));
        when(salesContractItemMapper.selectListByContractIds(any()))
                .thenReturn(Arrays.asList(item(11L, 1L, "1000"), item(12L, 2L, "2000")));

        // 关键字命中客户名
        ProductionProgressPageReqVO kwReq = req();
        kwReq.setKeyword("乙面");
        List<ProductionProgressPageItemRespVO> kwList = service.getProductionProgressPage(kwReq).getList();
        assertEquals(1, kwList.size());
        assertEquals(2L, kwList.get(0).getContractId());

        // 仅看预警：只有超期的 c1 保留
        ProductionProgressPageReqVO alertReq = req();
        alertReq.setAlertOnly(true);
        List<ProductionProgressPageItemRespVO> alertList = service.getProductionProgressPage(alertReq).getList();
        assertEquals(1, alertList.size());
        assertEquals(1L, alertList.get(0).getContractId());
        assertTrue(alertList.get(0).getAlerts().contains("超期"));
    }

    // ==================== 验收③：KG 过滤 / CANCELLED 排除 ====================

    @Test
    @DisplayName("demandKg 只累加 KG 单位合同明细，M 单位合同为 0")
    void demandKgCountsOnlyKgUnitContracts() {
        SalesContractDO kg = contract(1L, "CH-KG", "KG", "ACTIVE", TODAY.plusDays(60), null);
        SalesContractDO m = contract(2L, "CH-M", "M", "ACTIVE", TODAY.plusDays(60), null);
        when(salesContractMapper.selectListForProgress(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new ArrayList<>(Arrays.asList(kg, m)));
        when(salesContractItemMapper.selectListByContractIds(any())).thenReturn(Arrays.asList(
                item(11L, 1L, "600"), item(12L, 1L, "400"),   // KG 合同：600+400=1000
                item(21L, 2L, "100"), item(22L, 2L, "200"))); // M 合同：不进合计

        Map<Long, ProductionProgressPageItemRespVO> byId = pageById(req());

        assertAmount("1000.00", byId.get(1L).getDemandKg());
        assertAmount("0.00", byId.get(2L).getDemandKg());
    }

    @Test
    @DisplayName("胚布计划量排除 CANCELLED 与软删单据")
    void greigePlannedKgExcludesCancelledAndDeleted() {
        SalesContractDO c = contract(1L, "CH1", "KG", "ACTIVE", TODAY.plusDays(60), null);
        when(salesContractMapper.selectListForProgress(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new ArrayList<>(Collections.singletonList(c)));
        GreigeOrderDO active = greigeOrder(101L, 1L, "900", "ACTIVE", "OUTSOURCE");
        GreigeOrderDO cancelled = greigeOrder(102L, 1L, "999", "CANCELLED", "OUTSOURCE");
        GreigeOrderDO deleted = greigeOrder(103L, 1L, "888", "ACTIVE", "OUTSOURCE");
        deleted.setDeleted(true);
        when(greigeOrderMapper.selectListByContractIds(any()))
                .thenReturn(Arrays.asList(active, cancelled, deleted));

        ProductionProgressPageItemRespVO vo = pageById(req()).get(1L);

        assertAmount("900.00", vo.getGreigePlannedKg());
    }

    // ==================== 验收②：join 反查取合同 ====================

    @Test
    @DisplayName("入库量：join STOCK_IN 流水按 greigeOrderId/dyeingOrderId 区分胚布/成品，仅 KG，排除 CANCELLED")
    void stockInSplitsGreigeFinishedKgOnlyExcludesCancelled() {
        SalesContractDO c = contract(1L, "CH1", "KG", "ACTIVE", TODAY.plusDays(60), null);
        when(salesContractMapper.selectListForProgress(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new ArrayList<>(Collections.singletonList(c)));
        // SI1 胚布入库(COMPLETED)、SI2 成品入库(COMPLETED)、SI3 胚布入库(CANCELLED)
        when(stockInMapper.selectListByContractIds(any())).thenReturn(Arrays.asList(
                stockIn(201L, 1L, 101L, null, "COMPLETED"),
                stockIn(202L, 1L, null, 301L, "COMPLETED"),
                stockIn(203L, 1L, 103L, null, "CANCELLED")));
        when(stockLedgerMapper.selectListByRelatedDocIds(eq("STOCK_IN"), any(), eq("STOCK_IN"))).thenReturn(Arrays.asList(
                ledger(9001L, "STOCK_IN", 201L, "STOCK_IN", "500", "KG"),   // 胚布 +500
                ledger(9002L, "STOCK_IN", 201L, "STOCK_IN", "50", "M"),     // M 忽略
                ledger(9003L, "STOCK_IN", 202L, "STOCK_IN", "300", "KG"),   // 成品 +300
                ledger(9004L, "STOCK_IN", 203L, "STOCK_IN", "999", "KG"))); // 已取消单，被剔除

        ProductionProgressPageItemRespVO vo = pageById(req()).get(1L);

        assertAmount("500.00", vo.getGreigeInKg());
        assertAmount("300.00", vo.getFinishedInKg());
    }

    @Test
    @DisplayName("发货量：join SHIPMENT_OUT 流水取绝对值仅 KG，发货单数排除 CANCELLED，未发量=需求-已发")
    void shippedKgFromLedgerAbsExcludesCancelled() {
        SalesContractDO c = contract(1L, "CH1", "KG", "ACTIVE", TODAY.plusDays(60), null);
        when(salesContractMapper.selectListForProgress(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new ArrayList<>(Collections.singletonList(c)));
        when(salesContractItemMapper.selectListByContractIds(any()))
                .thenReturn(Collections.singletonList(item(11L, 1L, "1000")));
        when(shipmentMapper.selectListByContractIds(any())).thenReturn(Arrays.asList(
                shipment(401L, 1L, "COMPLETED"),
                shipment(402L, 1L, "CANCELLED")));
        when(stockLedgerMapper.selectListByRelatedDocIds(eq("SHIPMENT"), any(), eq("SHIPMENT_OUT"))).thenReturn(Arrays.asList(
                ledger(9101L, "SHIPMENT", 401L, "SHIPMENT_OUT", "-400", "KG"),  // |−400|=400
                ledger(9102L, "SHIPMENT", 402L, "SHIPMENT_OUT", "-999", "KG"))); // 已取消，被剔除

        ProductionProgressPageItemRespVO vo = pageById(req()).get(1L);

        assertAmount("400.00", vo.getShippedKg());
        assertEquals(1, vo.getShipmentCount());       // 仅 COMPLETED 计数
        assertAmount("600.00", vo.getUnshippedKg());  // 1000 - 400
    }

    @Test
    @DisplayName("发染量从发染记录表直聚（经胚布单映射合同），不走流水")
    void sentToDyeingKgFromDispatchRecords() {
        SalesContractDO c = contract(1L, "CH1", "KG", "ACTIVE", TODAY.plusDays(60), null);
        when(salesContractMapper.selectListForProgress(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new ArrayList<>(Collections.singletonList(c)));
        when(greigeOrderMapper.selectListByContractIds(any()))
                .thenReturn(Collections.singletonList(greigeOrder(101L, 1L, "900", "ACTIVE", "OUTSOURCE")));
        when(greigeDispatchRecordMapper.selectListByGreigeOrderIds(any())).thenReturn(Arrays.asList(
                dispatchRecord(501L, 101L, "700", TODAY.minusDays(3)),
                dispatchRecord(502L, 101L, "100", TODAY.minusDays(1))));

        ProductionProgressPageItemRespVO vo = pageById(req()).get(1L);

        assertAmount("800.00", vo.getSentToDyeingKg());
    }

    // ==================== 自织进度 / 染色覆盖率（get 明细） ====================

    @Test
    @DisplayName("自织进度：仅 SELF 胚布单关联织造单，计划匹数合计、COMPLETED 计完工匹数")
    void weavingProgressFromSelfGreigeOrdersOnly() {
        SalesContractDO c = contract(1L, "CH1", "KG", "ACTIVE", TODAY.plusDays(60), null);
        when(salesContractMapper.selectById(1L)).thenReturn(c);
        // G1 自织、G2 外发（不聚合织造）
        when(greigeOrderMapper.selectListByContractIds(any())).thenReturn(Arrays.asList(
                greigeOrder(101L, 1L, "500", "ACTIVE", "SELF"),
                greigeOrder(102L, 1L, "400", "ACTIVE", "OUTSOURCE")));
        when(weavingOrderMapper.selectListByGreigeOrderIds(any())).thenReturn(Arrays.asList(
                weavingOrder(601L, 101L, 60, "COMPLETED"),
                weavingOrder(602L, 101L, 40, "ACTIVE"),
                weavingOrder(603L, 101L, 99, "CANCELLED")));

        ProductionProgressDetailRespVO vo = service.getProductionProgress(1L);

        assertTrue(vo.getGreige().getSelfWeaving());
        assertEquals(100, vo.getGreige().getWeavingPlannedPieces());   // 60+40（排除取消）
        assertEquals(60, vo.getGreige().getWeavingCompletedPieces());  // 仅 COMPLETED
    }

    @Test
    @DisplayName("染色：覆盖率=Σ缸量/Σ颜色行量，缸状态汇总，延期缸（染色单交期已过且缸未完工）")
    void dyeingCoverageStatusSummaryAndDelayedVat() {
        SalesContractDO c = contract(1L, "CH1", "KG", "ACTIVE", TODAY.plusDays(60), null);
        when(salesContractMapper.selectById(1L)).thenReturn(c);
        // 染色单交期已过（TODAY-2）→ 非 COMPLETED 缸延期
        when(dyeingOrderMapper.selectListByContractIds(any()))
                .thenReturn(Collections.singletonList(dyeingOrder(301L, 1L, TODAY.minusDays(2), "ACTIVE")));
        when(dyeingColorMapper.selectListByOrderIds(any())).thenReturn(Arrays.asList(
                dyeingColor(701L, 301L, "600", "650", "KG"),   // 分母 600，投染 650
                dyeingColor(702L, 301L, "400", "350", "KG"),   // 分母 400，投染 350
                dyeingColor(703L, 301L, "999", "999", "M")));  // M 行投染不进合计；分母仍计入
        when(progressFeedbackMapper.selectListByDyeingOrderIds(any())).thenReturn(Arrays.asList(
                vat(801L, 301L, "500", "DYEING"),       // 延期
                vat(802L, 301L, "300", "COMPLETED"),    // 不延期
                vat(803L, 301L, "200", "SCHEDULED")));  // 延期

        ProductionProgressDetailRespVO.DyeingSection dyeing = service.getProductionProgress(1L).getDyeing();

        // 分子 500+300+200=1000；分母 600+400+999=1999 → 1000/1999*100=50.03
        assertAmount("50.03", dyeing.getVatCoverage());
        assertAmount("1000.00", dyeing.getPlannedKg()); // 650+350（M 行 999 不计）
        assertEquals(2, dyeing.getDelayedVatCount());
        assertEquals(1, dyeing.getVatStatusSummary().get("DYEING"));
        assertEquals(1, dyeing.getVatStatusSummary().get("COMPLETED"));
        assertEquals(1, dyeing.getVatStatusSummary().get("SCHEDULED"));
    }

    // ==================== 验收④：预警三条边界 ====================

    @Test
    @DisplayName("预警边界：≤7 临期、=8 不临期、已过超期、延期缸独立触发、无预警不产生条目")
    void alertBoundaries() {
        SalesContractDO near = contract(1L, "CH-NEAR", "KG", "ACTIVE", TODAY.plusDays(7), null);   // 临期(边界=7)
        SalesContractDO near8 = contract(2L, "CH-NEAR8", "KG", "ACTIVE", TODAY.plusDays(8), null);  // 不临期
        SalesContractDO over = contract(3L, "CH-OVER", "KG", "ACTIVE", TODAY.minusDays(1), null);   // 超期
        SalesContractDO vat = contract(4L, "CH-VAT", "KG", "ACTIVE", TODAY.plusDays(30), null);     // 延期缸
        SalesContractDO none = contract(5L, "CH-NONE", "M", "ACTIVE", TODAY.plusDays(30), null);    // 无预警(M→demand0)
        when(salesContractMapper.selectListForProgress(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new ArrayList<>(Arrays.asList(near, near8, over, vat, none)));
        // 1/2/3/4 号 KG 合同有需求 → 未发量>0；5 号 M 合同 demand=0 → 未发量=0
        when(salesContractItemMapper.selectListByContractIds(any())).thenReturn(Arrays.asList(
                item(11L, 1L, "1000"), item(12L, 2L, "1000"), item(13L, 3L, "1000"),
                item(14L, 4L, "1000"), item(15L, 5L, "1000")));
        // 4 号合同延期缸：染色单交期已过 + 非 COMPLETED 缸
        when(dyeingOrderMapper.selectListByContractIds(any()))
                .thenReturn(Collections.singletonList(dyeingOrder(401L, 4L, TODAY.minusDays(3), "ACTIVE")));
        when(dyeingColorMapper.selectListByOrderIds(any()))
                .thenReturn(Collections.singletonList(dyeingColor(411L, 401L, "100", "100", "KG")));
        when(progressFeedbackMapper.selectListByDyeingOrderIds(any()))
                .thenReturn(Collections.singletonList(vat(421L, 401L, "50", "DYEING")));

        Map<Long, ProductionProgressPageItemRespVO> byId = pageById(req());

        assertTrue(byId.get(1L).getAlerts().contains("临期"));
        assertEquals(1, byId.get(1L).getAlertCount());
        assertTrue(byId.get(2L).getAlerts().isEmpty());   // +8 天不临期、未超期、无延期缸
        assertEquals(0, byId.get(2L).getAlertCount());
        assertTrue(byId.get(3L).getAlerts().contains("超期"));
        assertTrue(byId.get(4L).getAlerts().contains("延期缸"));
        assertFalse(byId.get(4L).getAlerts().contains("临期")); // 交期+30 不临期
        assertTrue(byId.get(5L).getAlerts().isEmpty());   // M 合同 demand=0 → 未发量 0，无预警
    }

    // ==================== 验收⑥：get 六环节完整结构与下钻 ====================

    @Test
    @DisplayName("get 返回六环节完整结构、预警、业务员姓名与发染记录下钻")
    void getReturnsSixSectionsAndDrilldown() {
        SalesContractDO c = contract(1L, "CH1", "KG", "ACTIVE", TODAY.plusDays(3), 55L);
        c.setClientName("甲纺织");
        when(salesContractMapper.selectById(1L)).thenReturn(c);
        when(salesContractItemMapper.selectListByContractIds(any()))
                .thenReturn(Collections.singletonList(item(11L, 1L, "1000")));
        when(greigeOrderMapper.selectListByContractIds(any()))
                .thenReturn(Collections.singletonList(greigeOrder(101L, 1L, "900", "ACTIVE", "OUTSOURCE")));
        when(greigeDispatchRecordMapper.selectListByGreigeOrderIds(any())).thenReturn(Arrays.asList(
                dispatchRecord(501L, 101L, "300", TODAY.minusDays(2)),
                dispatchRecord(502L, 101L, "200", TODAY.minusDays(5))));
        when(dyeingOrderMapper.selectListByContractIds(any()))
                .thenReturn(Collections.singletonList(dyeingOrder(301L, 1L, TODAY.plusDays(3), "ACTIVE")));
        when(dyeingColorMapper.selectListByOrderIds(any()))
                .thenReturn(Collections.singletonList(dyeingColor(701L, 301L, "800", "800", "KG")));
        when(progressFeedbackMapper.selectListByDyeingOrderIds(any()))
                .thenReturn(Collections.singletonList(vat(801L, 301L, "400", "DYEING")));
        when(inspectionOrderMapper.selectListByContractIds(any()))
                .thenReturn(Collections.singletonList(inspectionOrder(901L, 1L, "COMPLETED")));
        when(inspectionOrderItemMapper.selectListByInspectionOrderIds(any())).thenReturn(Arrays.asList(
                inspectionItem(911L, 901L, "A"), inspectionItem(912L, 901L, "A"),
                inspectionItem(913L, 901L, "B"), inspectionItem(914L, 901L, "DEFECTIVE")));
        when(stockInMapper.selectListByContractIds(any())).thenReturn(Arrays.asList(
                stockIn(201L, 1L, 101L, null, "COMPLETED"),
                stockIn(202L, 1L, null, 301L, "COMPLETED")));
        when(stockLedgerMapper.selectListByRelatedDocIds(eq("STOCK_IN"), any(), eq("STOCK_IN"))).thenReturn(Arrays.asList(
                ledger(9001L, "STOCK_IN", 201L, "STOCK_IN", "600", "KG"),
                ledger(9002L, "STOCK_IN", 202L, "STOCK_IN", "500", "KG")));
        when(shipmentMapper.selectListByContractIds(any()))
                .thenReturn(Collections.singletonList(shipment(401L, 1L, "COMPLETED")));
        when(stockLedgerMapper.selectListByRelatedDocIds(eq("SHIPMENT"), any(), eq("SHIPMENT_OUT")))
                .thenReturn(Collections.singletonList(ledger(9101L, "SHIPMENT", 401L, "SHIPMENT_OUT", "-250", "KG")));
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(55L);
        user.setNickname("张三");
        when(adminUserApi.getUserMap(any())).thenReturn(Collections.singletonMap(55L, user));

        ProductionProgressDetailRespVO vo = service.getProductionProgress(1L);

        // 合同头
        assertEquals(1L, vo.getContractId());
        assertEquals("CH1", vo.getContractNo());
        assertEquals("甲纺织", vo.getCustomerName());
        assertEquals("张三", vo.getSalespersonName());
        assertEquals("KG", vo.getUnit());
        // 胚布环节
        assertAmount("1000.00", vo.getGreige().getDemandKg());
        assertAmount("900.00", vo.getGreige().getPlannedKg());
        assertAmount("600.00", vo.getGreige().getStockedInKg());
        assertAmount("500.00", vo.getGreige().getSentToDyeingKg());
        assertFalse(vo.getGreige().getSelfWeaving());
        // 染色环节
        assertAmount("800.00", vo.getDyeing().getPlannedKg());
        assertAmount("50.00", vo.getDyeing().getVatCoverage()); // 400/800*100
        assertEquals(0, vo.getDyeing().getDelayedVatCount());    // 交期+3 未过
        // 检验环节
        assertEquals(4, vo.getInspection().getTotalItems());
        assertEquals(2, vo.getInspection().getGradeSummary().get("A"));
        assertEquals(1, vo.getInspection().getGradeSummary().get("DEFECTIVE"));
        // 入库环节
        assertAmount("600.00", vo.getStock().getGreigeInKg());
        assertAmount("500.00", vo.getStock().getFinishedInKg());
        // 出货环节
        assertAmount("250.00", vo.getShipment().getShippedKg());
        assertAmount("750.00", vo.getShipment().getUnshippedKg()); // 1000-250
        assertEquals(1, vo.getShipment().getShipmentCount());
        // 预警：交期+3 且未发完 → 临期
        assertTrue(vo.getAlerts().contains("临期"));
        // 发染记录下钻：按日期倒序（501 在前）
        assertNotNull(vo.getGreigeDispatchRecords());
        assertEquals(2, vo.getGreigeDispatchRecords().size());
        assertEquals(501L, vo.getGreigeDispatchRecords().get(0).getId());
    }

    // ==================== 验收⑦：合同不存在 ====================

    @Test
    @DisplayName("get 合同不存在或已软删时报 1-030-019-000")
    void getThrowsWhenContractNotExists() {
        when(salesContractMapper.selectById(999L)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.getProductionProgress(999L));
        assertEquals(PRODUCTION_PROGRESS_CONTRACT_NOT_EXISTS.getCode(), ex.getCode());

        SalesContractDO deleted = contract(1000L, "CH-X", "KG", "ACTIVE", TODAY, null);
        deleted.setDeleted(true);
        when(salesContractMapper.selectById(1000L)).thenReturn(deleted);
        assertThrows(ServiceException.class, () -> service.getProductionProgress(1000L));
    }

    @Test
    @DisplayName("page 列表项透出合同头字段与业务员姓名（批量解析）")
    void pageItemExposesHeaderAndSalesperson() {
        SalesContractDO c = contract(1L, "CH1", "KG", "ACTIVE", TODAY.plusDays(60), 55L);
        c.setClientName("甲纺织");
        when(salesContractMapper.selectListForProgress(any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new ArrayList<>(Collections.singletonList(c)));
        AdminUserRespDTO user = new AdminUserRespDTO();
        user.setId(55L);
        user.setNickname("李四");
        when(adminUserApi.getUserMap(any())).thenReturn(Collections.singletonMap(55L, user));

        ProductionProgressPageItemRespVO vo = pageById(req()).get(1L);

        assertEquals("CH1", vo.getContractNo());
        assertEquals("甲纺织", vo.getCustomerName());
        assertEquals("李四", vo.getSalespersonName());
        assertEquals("ACTIVE", vo.getStatus());
        assertEquals("KG", vo.getUnit());
        // 无来源单据时各量为 0、无预警
        assertAmount("0.00", vo.getDemandKg());
        assertAmount("0.00", vo.getShippedKg());
        assertEquals(TODAY.plusDays(60), vo.getDeliveryDate());
        assertTrue(vo.getAlerts().isEmpty());
    }

    // ==================== 辅助：请求 / 分页索引 / 断言 ====================

    private ProductionProgressPageReqVO req() {
        ProductionProgressPageReqVO reqVO = new ProductionProgressPageReqVO();
        reqVO.setPageNo(1);
        reqVO.setPageSize(50);
        return reqVO;
    }

    private Map<Long, ProductionProgressPageItemRespVO> pageById(ProductionProgressPageReqVO reqVO) {
        return service.getProductionProgressPage(reqVO).getList().stream()
                .collect(Collectors.toMap(ProductionProgressPageItemRespVO::getContractId, Function.identity()));
    }

    private void assertAmount(String expected, BigDecimal actual) {
        assertNotNull(actual, "金额不应为 null");
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "期望 " + expected + " 实际 " + actual.toPlainString());
    }

    // ==================== 辅助：DO 工厂 ====================

    private SalesContractDO contract(Long id, String no, String unit, String status,
                                     LocalDate deliveryDate, Long salespersonUserId) {
        SalesContractDO c = new SalesContractDO();
        c.setId(id);
        c.setContractNo(no);
        c.setClientName("客户-" + id);
        c.setUnit(unit);
        c.setStatus(status);
        c.setDeliveryDate(deliveryDate);
        c.setSalespersonUserId(salespersonUserId);
        return c;
    }

    private SalesContractItemDO item(Long id, Long contractId, String qty) {
        SalesContractItemDO i = new SalesContractItemDO();
        i.setId(id);
        i.setContractId(contractId);
        i.setQuantity(new BigDecimal(qty));
        return i;
    }

    private GreigeOrderDO greigeOrder(Long id, Long contractId, String plannedQty, String status, String mode) {
        GreigeOrderDO g = new GreigeOrderDO();
        g.setId(id);
        g.setSalesContractId(contractId);
        g.setPlannedQty(new BigDecimal(plannedQty));
        g.setStatus(status);
        g.setProductionMode(mode);
        return g;
    }

    private WeavingOrderDO weavingOrder(Long id, Long greigeOrderId, Integer plannedQty, String status) {
        WeavingOrderDO w = new WeavingOrderDO();
        w.setId(id);
        w.setGreigeOrderId(greigeOrderId);
        w.setPlannedQty(plannedQty);
        w.setStatus(status);
        return w;
    }

    private GreigeDispatchRecordDO dispatchRecord(Long id, Long greigeOrderId, String qty, LocalDate date) {
        GreigeDispatchRecordDO d = new GreigeDispatchRecordDO();
        d.setId(id);
        d.setGreigeOrderId(greigeOrderId);
        d.setQuantity(new BigDecimal(qty));
        d.setDispatchDate(date);
        return d;
    }

    private DyeingOrderDO dyeingOrder(Long id, Long contractId, LocalDate deliveryDate, String status) {
        DyeingOrderDO d = new DyeingOrderDO();
        d.setId(id);
        d.setSalesContractId(contractId);
        d.setDeliveryDate(deliveryDate);
        d.setStatus(status);
        return d;
    }

    private DyeingColorDO dyeingColor(Long id, Long dyeingOrderId, String quantity, String inputQty, String inputUnit) {
        DyeingColorDO c = new DyeingColorDO();
        c.setId(id);
        c.setDyeingOrderId(dyeingOrderId);
        c.setQuantity(new BigDecimal(quantity));
        c.setDyeingInputQty(new BigDecimal(inputQty));
        c.setDyeingInputUnit(inputUnit);
        return c;
    }

    private ProgressFeedbackDO vat(Long id, Long dyeingOrderId, String qty, String status) {
        ProgressFeedbackDO v = new ProgressFeedbackDO();
        v.setId(id);
        v.setDyeingOrderId(dyeingOrderId);
        v.setQuantity(new BigDecimal(qty));
        v.setStatus(status);
        return v;
    }

    private InspectionOrderDO inspectionOrder(Long id, Long contractId, String status) {
        InspectionOrderDO o = new InspectionOrderDO();
        o.setId(id);
        o.setSalesContractId(contractId);
        o.setStatus(status);
        return o;
    }

    private InspectionOrderItemDO inspectionItem(Long id, Long inspectionOrderId, String grade) {
        InspectionOrderItemDO i = new InspectionOrderItemDO();
        i.setId(id);
        i.setInspectionOrderId(inspectionOrderId);
        i.setGrade(grade);
        return i;
    }

    private StockInDO stockIn(Long id, Long contractId, Long greigeOrderId, Long dyeingOrderId, String status) {
        StockInDO s = new StockInDO();
        s.setId(id);
        s.setSalesContractId(contractId);
        s.setGreigeOrderId(greigeOrderId);
        s.setDyeingOrderId(dyeingOrderId);
        s.setStatus(status);
        return s;
    }

    private StockLedgerDO ledger(Long id, String docType, Long docId, String txnType, String qty, String unit) {
        StockLedgerDO l = new StockLedgerDO();
        l.setId(id);
        l.setRelatedDocType(docType);
        l.setRelatedDocId(docId);
        l.setTxnType(txnType);
        l.setQuantity(new BigDecimal(qty));
        l.setUnit(unit);
        return l;
    }

    private ShipmentDO shipment(Long id, Long contractId, String status) {
        ShipmentDO s = new ShipmentDO();
        s.setId(id);
        s.setSalesContractId(contractId);
        s.setStatus(status);
        return s;
    }

}
