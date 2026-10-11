package com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link SupplierReconSourceServiceImpl} 单元测试
 *
 * 纯 Mockito 单元测试，不启动 Spring、不连库。
 * 覆盖 Issue 02 全部验收标准：
 * 1. 承检方/供应商过滤
 * 2. ΣKG / ΣM / 卷数全等级不过滤聚合
 * 3. GREIGE 缺省 METER、FINISHED 缺省 KG
 * 4. 采购入库 PURCHASE 包含、退货 RETURN 严格排除
 * 5. 防重指纹：DRAFT/CONFIRMED 排除，CANCELLED 释放回池
 * 6. 区间起止边界条件（含端点）
 * 7. 空候选集合
 *
 * @author luna
 */
@ExtendWith(MockitoExtension.class)
class SupplierReconSourceServiceImplTest {

    @Mock
    private InspectionOrderMapper inspectionOrderMapper;

    @Mock
    private InspectionOrderItemMapper inspectionOrderItemMapper;

    @Mock
    private StockInMapper stockInMapper;

    @Mock
    private StockInItemMapper stockInItemMapper;

    @Mock
    private SupplierReconMapper supplierReconMapper;

    @Mock
    private SupplierReconItemMapper supplierReconItemMapper;

    @InjectMocks
    private SupplierReconSourceServiceImpl sourceService;

    private final Long supplierId = 1001L;
    private final LocalDate periodStart = LocalDate.of(2026, 9, 1);
    private final LocalDate periodEnd = LocalDate.of(2026, 9, 30);

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(assistant, InspectionOrderDO.class);
        TableInfoHelper.initTableInfo(assistant, StockInDO.class);
        TableInfoHelper.initTableInfo(assistant, SupplierReconDO.class);
        TableInfoHelper.initTableInfo(assistant, SupplierReconItemDO.class);

        // 默认无占用指纹
        lenient().when(supplierReconMapper.selectListBySupplierIdAndStatuses(any(), any()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("1. 供应商过滤：只查询指定供应商的检验单和采购入库单")
    void testSupplierFiltering() {
        // supplierId 为空直接返回空对象
        SupplierReconCandidatesRespVO nullSupplierResult = sourceService.getCandidateItems(null, periodStart, periodEnd);
        assertNotNull(nullSupplierResult);
        assertTrue(nullSupplierResult.getItems().isEmpty());
        verifyNoInteractions(inspectionOrderMapper, stockInMapper);

        // 指定供应商ID查询
        when(inspectionOrderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(stockInMapper.selectList(any())).thenReturn(Collections.emptyList());

        SupplierReconCandidatesRespVO result = sourceService.getCandidateItems(supplierId, periodStart, periodEnd);
        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());

        // 验证传入查询条件的供应商ID
        verify(inspectionOrderMapper).selectList(any());
        verify(stockInMapper).selectList(any());
        verify(supplierReconMapper).selectListBySupplierIdAndStatuses(
                eq(supplierId),
                eq(Arrays.asList(SupplierReconStatusEnum.DRAFT.getValue(), SupplierReconStatusEnum.CONFIRMED.getValue()))
        );
    }

    @Test
    @DisplayName("2. 加工费候选聚合：ΣKG / ΣM / 卷数全等级计入，不过滤 A/B/C/DEFECTIVE")
    void testAggregateWeightLengthRolls() {
        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(10L);
        order.setCode("INS-20260915-001");
        order.setType("GREIGE");
        order.setStatus("COMPLETED");
        order.setInspectionFactoryId(supplierId);
        order.setProductId(201L);
        order.setProductCode("P20260901");
        order.setProductName("全棉汗布胚布");
        order.setOrderDate(LocalDate.of(2026, 9, 15));
        when(inspectionOrderMapper.selectList(any())).thenReturn(List.of(order));

        // 4 卷录检明细：不同等级全部计入
        InspectionOrderItemDO item1 = new InspectionOrderItemDO();
        item1.setId(101L);
        item1.setInspectionOrderId(10L);
        item1.setMeasuredWeight(new BigDecimal("20.50"));
        item1.setLengthM(new BigDecimal("100.00"));
        item1.setGrade("A");

        InspectionOrderItemDO item2 = new InspectionOrderItemDO();
        item2.setId(102L);
        item2.setInspectionOrderId(10L);
        item2.setMeasuredWeight(new BigDecimal("30.25"));
        item2.setLengthM(new BigDecimal("150.50"));
        item2.setGrade("B");

        InspectionOrderItemDO item3 = new InspectionOrderItemDO();
        item3.setId(103L);
        item3.setInspectionOrderId(10L);
        item3.setMeasuredWeight(new BigDecimal("25.00"));
        item3.setLengthM(new BigDecimal("120.00"));
        item3.setGrade("C");

        InspectionOrderItemDO item4 = new InspectionOrderItemDO();
        item4.setId(104L);
        item4.setInspectionOrderId(10L);
        item4.setMeasuredWeight(new BigDecimal("15.25"));
        item4.setLengthM(new BigDecimal("80.00"));
        item4.setGrade("DEFECTIVE");

        when(inspectionOrderItemMapper.selectListByInspectionOrderIds(List.of(10L)))
                .thenReturn(List.of(item1, item2, item3, item4));
        when(stockInMapper.selectList(any())).thenReturn(Collections.emptyList());

        SupplierReconCandidatesRespVO resp = sourceService.getCandidateItems(supplierId, periodStart, periodEnd);

        assertEquals(1, resp.getItems().size());
        assertEquals(1, resp.getProcessItems().size());
        assertEquals(0, resp.getPurchaseItems().size());

        SupplierReconCandidateItemRespVO candidate = resp.getProcessItems().get(0);
        assertEquals(SupplierReconLineTypeEnum.PROCESS.getValue(), candidate.getLineType());
        assertEquals("INSPECTION", candidate.getSourceRefType());
        assertEquals(10L, candidate.getSourceRefId());
        assertNull(candidate.getSourceRefItemId());
        assertEquals(10L, candidate.getInspectionOrderId());
        assertEquals("INS-20260915-001", candidate.getInspectionOrderNo());
        assertEquals("GREIGE", candidate.getInspectionType());
        assertEquals(4, candidate.getRollCount()); // 4 卷
        assertEquals(new BigDecimal("91.00"), candidate.getQtyKg()); // 20.50 + 30.25 + 25.00 + 15.25
        assertEquals(new BigDecimal("450.50"), candidate.getQtyM()); // 100.00 + 150.50 + 120.00 + 80.00
        assertEquals(PricingBasisEnum.METER.getValue(), candidate.getPricingBasis()); // GREIGE 缺省按米
        assertEquals(new BigDecimal("450.50"), candidate.getBillQty()); // bill_qty 对应米数
        assertNull(candidate.getUnitPrice()); // 检验单无单价，手工填
        assertEquals(BigDecimal.ZERO, candidate.getLineAmount());
    }

    @Test
    @DisplayName("3. 缺省计价基准：GREIGE 胚布缺省 METER，FINISHED 成品缺省 KG")
    void testPricingBasisDefault_GreigeMeterAndFinishedKg() {
        InspectionOrderDO greigeOrder = createInspectionOrder(11L, "INS-GREIGE-001", "GREIGE", supplierId, LocalDate.of(2026, 9, 10));
        InspectionOrderDO finishedOrder = createInspectionOrder(12L, "INS-FINISHED-001", "FINISHED", supplierId, LocalDate.of(2026, 9, 20));

        when(inspectionOrderMapper.selectList(any())).thenReturn(List.of(greigeOrder, finishedOrder));

        InspectionOrderItemDO itemGreige = new InspectionOrderItemDO();
        itemGreige.setInspectionOrderId(11L);
        itemGreige.setMeasuredWeight(new BigDecimal("100.00"));
        itemGreige.setLengthM(new BigDecimal("600.00"));

        InspectionOrderItemDO itemFinished = new InspectionOrderItemDO();
        itemFinished.setInspectionOrderId(12L);
        itemFinished.setMeasuredWeight(new BigDecimal("150.00"));
        itemFinished.setLengthM(new BigDecimal("800.00"));

        when(inspectionOrderItemMapper.selectListByInspectionOrderIds(List.of(11L, 12L)))
                .thenReturn(List.of(itemGreige, itemFinished));
        when(stockInMapper.selectList(any())).thenReturn(Collections.emptyList());

        SupplierReconCandidatesRespVO resp = sourceService.getCandidateItems(supplierId, periodStart, periodEnd);
        assertEquals(2, resp.getProcessItems().size());

        // GREIGE -> METER, bill_qty = 600.00
        SupplierReconCandidateItemRespVO c1 = resp.getProcessItems().get(0);
        assertEquals("GREIGE", c1.getInspectionType());
        assertEquals(PricingBasisEnum.METER.getValue(), c1.getPricingBasis());
        assertEquals(new BigDecimal("600.00"), c1.getBillQty());

        // FINISHED -> KG, bill_qty = 150.00
        SupplierReconCandidateItemRespVO c2 = resp.getProcessItems().get(1);
        assertEquals("FINISHED", c2.getInspectionType());
        assertEquals(PricingBasisEnum.KG.getValue(), c2.getPricingBasis());
        assertEquals(new BigDecimal("150.00"), c2.getBillQty());
    }

    @Test
    @DisplayName("4. 采购货款候选：PURCHASE 包含、带批次产品快照和单价，RETURN 严格排除")
    void testPurchaseIncludedAndReturnExcluded() {
        when(inspectionOrderMapper.selectList(any())).thenReturn(Collections.emptyList());

        StockInDO purchaseStockIn = createStockIn(301L, "IN-20260920-001", "PURCHASE", supplierId, LocalDate.of(2026, 9, 20), 401L, "YO-20260901-001");
        when(stockInMapper.selectList(any())).thenReturn(List.of(purchaseStockIn));

        StockInItemDO item = new StockInItemDO();
        item.setId(501L);
        item.setStockInId(301L);
        item.setProductId(601L);
        item.setProductCode("YARN-32S");
        item.setProductName("32支全棉纱");
        item.setBatchNo("BATCH-202609");
        item.setQuantity(new BigDecimal("500.00"));
        item.setUnit("KG");
        item.setUnitPrice(new BigDecimal("22.5000"));
        item.setAmount(new BigDecimal("11250.00"));

        when(stockInItemMapper.selectListByStockInIds(List.of(301L)))
                .thenReturn(List.of(item));

        SupplierReconCandidatesRespVO resp = sourceService.getCandidateItems(supplierId, periodStart, periodEnd);
        assertEquals(1, resp.getItems().size());
        assertEquals(0, resp.getProcessItems().size());
        assertEquals(1, resp.getPurchaseItems().size());

        SupplierReconCandidateItemRespVO candidate = resp.getPurchaseItems().get(0);
        assertEquals(SupplierReconLineTypeEnum.PURCHASE.getValue(), candidate.getLineType());
        assertEquals(SupplierReconSourceRefTypeEnum.STOCK_IN_ITEM.getValue(), candidate.getSourceRefType());
        assertEquals(301L, candidate.getSourceRefId());
        assertEquals(501L, candidate.getSourceRefItemId());
        assertEquals(301L, candidate.getStockInId());
        assertEquals("IN-20260920-001", candidate.getStockInNo());
        assertEquals(401L, candidate.getYarnOrderId());
        assertEquals("YO-20260901-001", candidate.getYarnOrderNo());
        assertEquals(601L, candidate.getProductId());
        assertEquals("YARN-32S", candidate.getProductCode());
        assertEquals("32支全棉纱", candidate.getProductName());
        assertEquals("BATCH-202609", candidate.getBatchNo());
        assertEquals(new BigDecimal("500.00"), candidate.getQuantity());
        assertEquals("KG", candidate.getUnit());
        assertEquals(PricingBasisEnum.KG.getValue(), candidate.getPricingBasis());
        assertEquals(new BigDecimal("500.00"), candidate.getBillQty());
        assertEquals(new BigDecimal("500.00"), candidate.getQtyKg());
        assertNull(candidate.getQtyM());
        assertEquals(new BigDecimal("22.5000"), candidate.getUnitPrice());
        assertEquals(new BigDecimal("11250.00"), candidate.getLineAmount());
    }

    @Test
    @DisplayName("5. 防重指纹排除与作废释放：DRAFT/CONFIRMED 排除，CANCELLED 对账单释放回池")
    void testFingerprintOccupiedDraftConfirmedExcluded_CancelledReleased() {
        // 模拟对账单：对账单 1 为 DRAFT，对账单 2 为 CONFIRMED
        // 注意：CANCELLED 对账单不会出现在 selectListBySupplierIdAndStatuses 返回的 active 列表中
        SupplierReconDO draftRecon = SupplierReconDO.builder().id(1L).status(SupplierReconStatusEnum.DRAFT.getValue()).build();
        SupplierReconDO confirmedRecon = SupplierReconDO.builder().id(2L).status(SupplierReconStatusEnum.CONFIRMED.getValue()).build();

        when(supplierReconMapper.selectListBySupplierIdAndStatuses(
                eq(supplierId),
                eq(Arrays.asList(SupplierReconStatusEnum.DRAFT.getValue(), SupplierReconStatusEnum.CONFIRMED.getValue()))
        )).thenReturn(List.of(draftRecon, confirmedRecon));

        // 对账单 1 引用了检验单 101L（INSPECTION:101）
        SupplierReconItemDO itemOccupied1 = SupplierReconItemDO.builder()
                .reconId(1L)
                .sourceRefType(SupplierReconSourceRefTypeEnum.INSPECTION.getValue())
                .sourceRefId(101L)
                .build();

        // 对账单 2 引用了入库明细 501L（STOCK_IN_ITEM:301:501）
        SupplierReconItemDO itemOccupied2 = SupplierReconItemDO.builder()
                .reconId(2L)
                .sourceRefType(SupplierReconSourceRefTypeEnum.STOCK_IN_ITEM.getValue())
                .sourceRefId(301L)
                .sourceRefItemId(501L)
                .build();

        when(supplierReconItemMapper.selectListByReconIds(List.of(1L, 2L)))
                .thenReturn(List.of(itemOccupied1, itemOccupied2));

        // 源检验单：101L（已被 DRAFT 引用）、102L（未被引用）、103L（曾被 CANCELLED 引用，现已释放）
        InspectionOrderDO order101 = createInspectionOrder(101L, "INS-101", "GREIGE", supplierId, LocalDate.of(2026, 9, 10));
        InspectionOrderDO order102 = createInspectionOrder(102L, "INS-102", "FINISHED", supplierId, LocalDate.of(2026, 9, 12));
        InspectionOrderDO order103 = createInspectionOrder(103L, "INS-103", "GREIGE", supplierId, LocalDate.of(2026, 9, 14));
        when(inspectionOrderMapper.selectList(any())).thenReturn(List.of(order101, order102, order103));

        InspectionOrderItemDO piece102 = new InspectionOrderItemDO();
        piece102.setInspectionOrderId(102L);
        piece102.setMeasuredWeight(new BigDecimal("50.00"));
        piece102.setLengthM(new BigDecimal("200.00"));

        InspectionOrderItemDO piece103 = new InspectionOrderItemDO();
        piece103.setInspectionOrderId(103L);
        piece103.setMeasuredWeight(new BigDecimal("60.00"));
        piece103.setLengthM(new BigDecimal("250.00"));

        when(inspectionOrderItemMapper.selectListByInspectionOrderIds(List.of(101L, 102L, 103L)))
                .thenReturn(List.of(piece102, piece103));

        // 源入库单：301L，包含明细 501L（已被 CONFIRMED 引用）和 502L（未被引用）
        StockInDO stockIn = createStockIn(301L, "IN-301", "PURCHASE", supplierId, LocalDate.of(2026, 9, 15), null, null);
        when(stockInMapper.selectList(any())).thenReturn(List.of(stockIn));

        StockInItemDO item501 = new StockInItemDO();
        item501.setId(501L);
        item501.setStockInId(301L);
        item501.setQuantity(new BigDecimal("100.00"));
        item501.setUnit("KG");

        StockInItemDO item502 = new StockInItemDO();
        item502.setId(502L);
        item502.setStockInId(301L);
        item502.setQuantity(new BigDecimal("200.00"));
        item502.setUnit("KG");

        when(stockInItemMapper.selectListByStockInIds(List.of(301L)))
                .thenReturn(List.of(item501, item502));

        SupplierReconCandidatesRespVO resp = sourceService.getCandidateItems(supplierId, periodStart, periodEnd);

        // 验证加工费候选：101L 被排除，102L 与 103L 进入候选（共 2 个）
        assertEquals(2, resp.getProcessItems().size());
        Set<Long> processIds = Set.of(resp.getProcessItems().get(0).getInspectionOrderId(), resp.getProcessItems().get(1).getInspectionOrderId());
        assertTrue(processIds.contains(102L));
        assertTrue(processIds.contains(103L));
        assertFalse(processIds.contains(101L));

        // 验证采购货款候选：501L 被排除，502L 进入候选（共 1 个）
        assertEquals(1, resp.getPurchaseItems().size());
        assertEquals(502L, resp.getPurchaseItems().get(0).getSourceRefItemId());

        // 总候选共 3 个
        assertEquals(3, resp.getItems().size());
    }

    @Test
    @DisplayName("6. 区间起止边界：含端点及异常区间校验")
    void testPeriodStartEndBoundaries() {
        // periodStart > periodEnd 异常区间直接返回空集合
        SupplierReconCandidatesRespVO invalidRangeResult = sourceService.getCandidateItems(
                supplierId, LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 1));
        assertNotNull(invalidRangeResult);
        assertTrue(invalidRangeResult.getItems().isEmpty());
        verifyNoInteractions(inspectionOrderMapper, stockInMapper);

        // 正常端点查询
        when(inspectionOrderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(stockInMapper.selectList(any())).thenReturn(Collections.emptyList());

        SupplierReconCandidatesRespVO resp = sourceService.getCandidateItems(supplierId, periodStart, periodEnd);
        assertNotNull(resp);
        assertTrue(resp.getItems().isEmpty());

        verify(inspectionOrderMapper).selectList(any());
        verify(stockInMapper).selectList(any());
    }

    @Test
    @DisplayName("7. 空候选集合：无任何源单时返回非空响应对象，各子列表为空列表")
    void testEmptyCandidateList() {
        when(inspectionOrderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(stockInMapper.selectList(any())).thenReturn(Collections.emptyList());

        SupplierReconCandidatesRespVO resp = sourceService.getCandidateItems(supplierId, periodStart, periodEnd);

        assertNotNull(resp);
        assertNotNull(resp.getItems());
        assertNotNull(resp.getProcessItems());
        assertNotNull(resp.getPurchaseItems());
        assertTrue(resp.getItems().isEmpty());
        assertTrue(resp.getProcessItems().isEmpty());
        assertTrue(resp.getPurchaseItems().isEmpty());
    }

    @Test
    @DisplayName("8. 指纹构建与解析工具方法正确性校验")
    void testBuildFingerprint() {
        // 检验单指纹（明细ID为空）
        String fpInspection = SupplierReconSourceService.buildFingerprint(
                SupplierReconSourceRefTypeEnum.INSPECTION.getValue(), 101L, null);
        assertEquals("INSPECTION:101", fpInspection);

        // 入库明细指纹
        String fpStockItem = SupplierReconSourceService.buildFingerprint(
                SupplierReconSourceRefTypeEnum.STOCK_IN_ITEM.getValue(), 201L, 301L);
        assertEquals("STOCK_IN_ITEM:201:301", fpStockItem);

        // 空入参安全处理
        assertEquals("", SupplierReconSourceService.buildFingerprint(null, 101L, null));
        assertEquals("", SupplierReconSourceService.buildFingerprint("INSPECTION", null, null));
    }

    private InspectionOrderDO createInspectionOrder(Long id, String code, String type, Long factoryId, LocalDate orderDate) {
        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(id);
        order.setCode(code);
        order.setType(type);
        order.setStatus("COMPLETED");
        order.setInspectionFactoryId(factoryId);
        order.setOrderDate(orderDate);
        return order;
    }

    private StockInDO createStockIn(Long id, String inNo, String inType, Long supplierId, LocalDate inDate, Long yarnOrderId, String yarnOrderNo) {
        StockInDO stockIn = new StockInDO();
        stockIn.setId(id);
        stockIn.setInNo(inNo);
        stockIn.setInType(inType);
        stockIn.setStatus("COMPLETED");
        stockIn.setSupplierId(supplierId);
        stockIn.setInDate(inDate);
        stockIn.setYarnOrderId(yarnOrderId);
        stockIn.setYarnOrderNo(yarnOrderNo);
        return stockIn;
    }

}
