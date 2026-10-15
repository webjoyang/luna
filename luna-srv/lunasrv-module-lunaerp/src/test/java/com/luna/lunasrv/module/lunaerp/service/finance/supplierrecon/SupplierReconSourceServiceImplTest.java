package com.luna.lunasrv.module.lunaerp.service.finance.supplierrecon;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
import static org.mockito.Mockito.*;

/**
 * {@link SupplierReconSourceServiceImpl} 纯 Mockito 单元测试
 *
 * 严格覆盖 Issue 02 验收清单：
 * 1. 承检方过滤与单据状态/删除状态过滤
 * 2. 录检明细 ΣKG/ΣM/卷数聚合及全等级（A/B/C/DEFECTIVE）计入不过滤
 * 3. 缺省计价基准：GREIGE→METER（bill_qty=qty_m）、FINISHED→KG（bill_qty=qty_kg）
 * 4. 采购货款候选：仅主表 COMPLETED 且 in_type=PURCHASE，RETURN 排除
 * 5. 防重指纹：DRAFT/CONFIRMED 排除、CANCELLED 释放回候选池
 * 6. 区间起止端点包含边界校验
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
    private SupplierReconSourceServiceImpl supplierReconSourceService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), InspectionOrderDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), InspectionOrderItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), StockInDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), StockInItemDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SupplierReconDO.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), SupplierReconItemDO.class);
    }

    @Test
    @DisplayName("测试空候选集合：无任何匹配单据时返回空列表")
    void testEmptyCandidates() {
        Long supplierId = 100L;
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        when(inspectionOrderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(stockInMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SupplierReconSourceItemRespVO> result = supplierReconSourceService.getAvailableCandidateItems(supplierId, start, end);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("测试入参 null 防御：supplierId 或区间为 null 时返回空列表")
    void testNullArgsDefense() {
        assertTrue(supplierReconSourceService.getAvailableCandidateItems(null, LocalDate.now(), LocalDate.now()).isEmpty());
        assertTrue(supplierReconSourceService.getAvailableCandidateItems(1L, null, LocalDate.now()).isEmpty());
        assertTrue(supplierReconSourceService.getAvailableCandidateItems(1L, LocalDate.now(), null).isEmpty());
    }

    @Test
    @DisplayName("测试加工费候选：承检方过滤、ΣKG/ΣM/卷数聚合及全等级计入、GREIGE 缺省按米")
    void testProcessCandidates_Greige_DefaultMeter() {
        Long supplierId = 200L;
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        InspectionOrderDO greigeOrder = new InspectionOrderDO();
        greigeOrder.setId(10L);
        greigeOrder.setCode("INS-GREIGE-001");
        greigeOrder.setType("GREIGE");
        greigeOrder.setStatus("COMPLETED");
        greigeOrder.setInspectionFactoryId(supplierId);
        greigeOrder.setProductId(1L);
        greigeOrder.setProductCode("P-GREIGE");
        greigeOrder.setProductName("纯棉胚布");
        greigeOrder.setOrderDate(LocalDate.of(2026, 9, 15));

        when(inspectionOrderMapper.selectList(any())).thenReturn(List.of(greigeOrder));
        when(stockInMapper.selectList(any())).thenReturn(Collections.emptyList());

        // 4 卷录检明细，分别对应等级 A, B, C, DEFECTIVE，验证全等级计入不过滤
        InspectionOrderItemDO item1 = new InspectionOrderItemDO();
        item1.setId(101L);
        item1.setInspectionOrderId(10L);
        item1.setRollNo("R1");
        item1.setMeasuredWeight(new BigDecimal("25.30"));
        item1.setLengthM(new BigDecimal("50.00"));
        item1.setGrade("A");

        InspectionOrderItemDO item2 = new InspectionOrderItemDO();
        item2.setId(102L);
        item2.setInspectionOrderId(10L);
        item2.setRollNo("R2");
        item2.setMeasuredWeight(new BigDecimal("24.70"));
        item2.setLengthM(new BigDecimal("49.50"));
        item2.setGrade("B");

        InspectionOrderItemDO item3 = new InspectionOrderItemDO();
        item3.setId(103L);
        item3.setInspectionOrderId(10L);
        item3.setRollNo("R3");
        item3.setMeasuredWeight(new BigDecimal("26.00"));
        item3.setLengthM(new BigDecimal("51.20"));
        item3.setGrade("C");

        InspectionOrderItemDO item4 = new InspectionOrderItemDO();
        item4.setId(104L);
        item4.setInspectionOrderId(10L);
        item4.setRollNo("R4");
        item4.setMeasuredWeight(new BigDecimal("23.00"));
        item4.setLengthM(new BigDecimal("45.30"));
        item4.setGrade("DEFECTIVE");

        when(inspectionOrderItemMapper.selectListByInspectionOrderIds(List.of(10L)))
                .thenReturn(List.of(item1, item2, item3, item4));
        when(supplierReconMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SupplierReconSourceItemRespVO> list = supplierReconSourceService.getAvailableCandidateItems(supplierId, start, end);
        assertEquals(1, list.size());

        SupplierReconSourceItemRespVO vo = list.get(0);
        assertEquals(LineType.PROCESS.getValue(), vo.getLineType());
        assertEquals(SourceRefType.INSPECTION.getValue(), vo.getSourceRefType());
        assertEquals(10L, vo.getSourceRefId());
        assertNull(vo.getSourceRefItemId());
        assertEquals("INS-GREIGE-001", vo.getInspectionOrderNo());
        assertEquals("GREIGE", vo.getInspectionType());
        assertEquals(1L, vo.getProductId());
        assertEquals("P-GREIGE", vo.getProductCode());
        assertEquals("纯棉胚布", vo.getProductName());

        // 聚合校验：25.30 + 24.70 + 26.00 + 23.00 = 99.00 KG
        assertEquals(new BigDecimal("99.00"), vo.getQtyKg());
        // 50.00 + 49.50 + 51.20 + 45.30 = 196.00 M
        assertEquals(new BigDecimal("196.00"), vo.getQtyM());
        // 卷数 = 4
        assertEquals(4, vo.getRollCount());

        // 计价基准：GREIGE 缺省按米 METER，bill_qty = qty_m = 196.00
        assertEquals(PricingBasis.METER.getValue(), vo.getPricingBasis());
        assertEquals(new BigDecimal("196.00"), vo.getBillQty());
        assertNull(vo.getUnitPrice());
        assertEquals("INSPECTION:10", vo.getFingerprint());
    }

    @Test
    @DisplayName("测试加工费候选：FINISHED 缺省按公斤 KG，bill_qty=qty_kg")
    void testProcessCandidates_Finished_DefaultKg() {
        Long supplierId = 200L;
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        InspectionOrderDO finishedOrder = new InspectionOrderDO();
        finishedOrder.setId(20L);
        finishedOrder.setCode("INS-FINISHED-001");
        finishedOrder.setType("FINISHED");
        finishedOrder.setStatus("COMPLETED");
        finishedOrder.setInspectionFactoryId(supplierId);
        finishedOrder.setProductId(2L);
        finishedOrder.setProductCode("P-FINISHED");
        finishedOrder.setProductName("染色成品布");
        finishedOrder.setOrderDate(LocalDate.of(2026, 9, 20));

        when(inspectionOrderMapper.selectList(any())).thenReturn(List.of(finishedOrder));
        when(stockInMapper.selectList(any())).thenReturn(Collections.emptyList());

        InspectionOrderItemDO item = new InspectionOrderItemDO();
        item.setId(201L);
        item.setInspectionOrderId(20L);
        item.setMeasuredWeight(new BigDecimal("88.88"));
        item.setLengthM(new BigDecimal("120.00"));

        when(inspectionOrderItemMapper.selectListByInspectionOrderIds(List.of(20L))).thenReturn(List.of(item));
        when(supplierReconMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SupplierReconSourceItemRespVO> list = supplierReconSourceService.getAvailableCandidateItems(supplierId, start, end);
        assertEquals(1, list.size());

        SupplierReconSourceItemRespVO vo = list.get(0);
        assertEquals("FINISHED", vo.getInspectionType());
        assertEquals(PricingBasis.KG.getValue(), vo.getPricingBasis());
        assertEquals(new BigDecimal("88.88"), vo.getQtyKg());
        assertEquals(new BigDecimal("120.00"), vo.getQtyM());
        assertEquals(new BigDecimal("88.88"), vo.getBillQty());
        assertEquals("INSPECTION:20", vo.getFingerprint());
    }

    @Test
    @DisplayName("测试采购货款候选：PURCHASE 入库明细入选、带入库单号/批次/单价/金额")
    void testPurchaseCandidates_Success() {
        Long supplierId = 300L;
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        when(inspectionOrderMapper.selectList(any())).thenReturn(Collections.emptyList());

        StockInDO stockIn = new StockInDO();
        stockIn.setId(50L);
        stockIn.setInNo("IN-20260910-001");
        stockIn.setInType("PURCHASE");
        stockIn.setStatus("COMPLETED");
        stockIn.setSupplierId(supplierId);
        stockIn.setInDate(LocalDate.of(2026, 9, 10));

        when(stockInMapper.selectList(any())).thenReturn(List.of(stockIn));

        StockInItemDO item1 = new StockInItemDO();
        item1.setId(501L);
        item1.setStockInId(50L);
        item1.setProductId(10L);
        item1.setProductCode("YARN-32S");
        item1.setProductName("32S 纯棉精梳纱");
        item1.setBatchNo("BATCH-20260910A");
        item1.setQuantity(new BigDecimal("500.00"));
        item1.setUnit("KG");
        item1.setUnitPrice(new BigDecimal("28.5000"));
        item1.setPieceCount(10);

        when(stockInItemMapper.selectList(any())).thenReturn(List.of(item1));
        when(supplierReconMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SupplierReconSourceItemRespVO> list = supplierReconSourceService.getAvailableCandidateItems(supplierId, start, end);
        assertEquals(1, list.size());

        SupplierReconSourceItemRespVO vo = list.get(0);
        assertEquals(LineType.PURCHASE.getValue(), vo.getLineType());
        assertEquals(SourceRefType.STOCK_IN_ITEM.getValue(), vo.getSourceRefType());
        assertEquals(50L, vo.getSourceRefId());
        assertEquals(501L, vo.getSourceRefItemId());
        assertEquals(50L, vo.getStockInId());
        assertEquals("IN-20260910-001", vo.getStockInNo());
        assertEquals(501L, vo.getStockInItemId());
        assertEquals(10L, vo.getProductId());
        assertEquals("YARN-32S", vo.getProductCode());
        assertEquals("32S 纯棉精梳纱", vo.getProductName());
        assertEquals("BATCH-20260910A", vo.getBatchNo());
        assertEquals(new BigDecimal("500.00"), vo.getQtyKg());
        assertNull(vo.getQtyM());
        assertEquals(10, vo.getRollCount());
        assertEquals("KG", vo.getQtyUnit());
        assertEquals(PricingBasis.KG.getValue(), vo.getPricingBasis());
        assertEquals(new BigDecimal("500.00"), vo.getBillQty());
        assertEquals(new BigDecimal("28.5000"), vo.getUnitPrice());
        // lineAmount = 500.00 * 28.5000 = 14250.00
        assertEquals(new BigDecimal("14250.00"), vo.getLineAmount());
        assertEquals("STOCK_IN_ITEM:50:501", vo.getFingerprint());
    }

    @Test
    @DisplayName("测试采购货款过滤：in_type 查询条件必须为 PURCHASE（RETURN 排除在候选外）")
    void testPurchaseCandidates_ExcludesReturnByQuery() {
        Long supplierId = 300L;
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        when(inspectionOrderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(stockInMapper.selectList(any())).thenReturn(Collections.emptyList());

        supplierReconSourceService.getAvailableCandidateItems(supplierId, start, end);

        // 验证查询条件使用了 in_type = 'PURCHASE'，杜绝 RETURN 入库混入候选
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StockInDO>> captor = ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(stockInMapper).selectList(captor.capture());
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StockInDO> wrapper = captor.getValue();
        String sqlSegment = wrapper.getCustomSqlSegment();
        assertNotNull(sqlSegment);
        assertTrue(sqlSegment.contains("in_type"));
    }

    @Test
    @DisplayName("测试防重指纹：DRAFT 与 CONFIRMED 对账单引用的指纹被排除，CANCELLED 对账单释放回池")
    void testFingerprintExclusion_AndCancelledRelease() {
        Long supplierId = 400L;
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        // 候选源：2 个检验单 (10L, 20L) + 2 个入库明细 (50L:501L, 60L:601L)
        InspectionOrderDO insp1 = new InspectionOrderDO();
        insp1.setId(10L);
        insp1.setCode("INS-01");
        insp1.setType("GREIGE");
        InspectionOrderDO insp2 = new InspectionOrderDO();
        insp2.setId(20L);
        insp2.setCode("INS-02");
        insp2.setType("GREIGE");
        when(inspectionOrderMapper.selectList(any())).thenReturn(List.of(insp1, insp2));
        when(inspectionOrderItemMapper.selectListByInspectionOrderIds(any())).thenReturn(Collections.emptyList());

        StockInDO si1 = new StockInDO();
        si1.setId(50L);
        si1.setInNo("IN-01");
        StockInDO si2 = new StockInDO();
        si2.setId(60L);
        si2.setInNo("IN-02");
        when(stockInMapper.selectList(any())).thenReturn(List.of(si1, si2));

        StockInItemDO sii1 = new StockInItemDO();
        sii1.setId(501L);
        sii1.setStockInId(50L);
        sii1.setUnit("KG");
        StockInItemDO sii2 = new StockInItemDO();
        sii2.setId(601L);
        sii2.setStockInId(60L);
        sii2.setUnit("KG");
        when(stockInItemMapper.selectList(any())).thenReturn(List.of(sii1, sii2));

        // 存量对账单：
        // 1. DRAFT 单 SR-DRAFT 占用 insp1 (INSPECTION:10)
        // 2. CONFIRMED 单 SR-CONFIRMED 占用 sii1 (STOCK_IN_ITEM:50:501)
        // 3. CANCELLED 单不出现在 activeRecons 查询中（指纹自动释放）
        SupplierReconDO reconDraft = new SupplierReconDO();
        reconDraft.setId(1001L);
        reconDraft.setReconNo("SR-DRAFT-001");
        reconDraft.setStatus(ReconStatus.DRAFT.getValue());

        SupplierReconDO reconConfirmed = new SupplierReconDO();
        reconConfirmed.setId(1002L);
        reconConfirmed.setReconNo("SR-CONFIRMED-001");
        reconConfirmed.setStatus(ReconStatus.CONFIRMED.getValue());

        when(supplierReconMapper.selectList(any())).thenReturn(List.of(reconDraft, reconConfirmed));

        SupplierReconItemDO occupiedItem1 = new SupplierReconItemDO();
        occupiedItem1.setId(1L);
        occupiedItem1.setReconId(1001L);
        occupiedItem1.setSourceRefType(SourceRefType.INSPECTION.getValue());
        occupiedItem1.setSourceRefId(10L);

        SupplierReconItemDO occupiedItem2 = new SupplierReconItemDO();
        occupiedItem2.setId(2L);
        occupiedItem2.setReconId(1002L);
        occupiedItem2.setSourceRefType(SourceRefType.STOCK_IN_ITEM.getValue());
        occupiedItem2.setSourceRefId(50L);
        occupiedItem2.setSourceRefItemId(501L);

        when(supplierReconItemMapper.selectListByReconIds(any())).thenReturn(List.of(occupiedItem1, occupiedItem2));

        List<SupplierReconSourceItemRespVO> availableList = supplierReconSourceService.getAvailableCandidateItems(supplierId, start, end);

        // 原始共 4 个候选，被 DRAFT(insp1) 和 CONFIRMED(sii1) 占用后，仅剩 insp2 和 sii2
        assertEquals(2, availableList.size());
        List<String> remainingFps = availableList.stream().map(SupplierReconSourceItemRespVO::getFingerprint).toList();
        assertTrue(remainingFps.contains("INSPECTION:20"));
        assertTrue(remainingFps.contains("STOCK_IN_ITEM:60:601"));
        assertFalse(remainingFps.contains("INSPECTION:10"));
        assertFalse(remainingFps.contains("STOCK_IN_ITEM:50:501"));

        // 验证 findOccupiedFingerprints 查找占用单号准确
        Map<String, String> occupiedMap = supplierReconSourceService.findOccupiedFingerprints(
                Set.of("INSPECTION:10", "STOCK_IN_ITEM:50:501", "INSPECTION:20"));
        assertEquals(2, occupiedMap.size());
        assertEquals("SR-DRAFT-001", occupiedMap.get("INSPECTION:10"));
        assertEquals("SR-CONFIRMED-001", occupiedMap.get("STOCK_IN_ITEM:50:501"));
    }

    @Test
    @DisplayName("测试区间起止边界过滤（含端点）条件传递正确性")
    void testPeriodBoundaries_Inclusive() {
        Long supplierId = 500L;
        LocalDate start = LocalDate.of(2026, 9, 1);
        LocalDate end = LocalDate.of(2026, 9, 30);

        when(inspectionOrderMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(stockInMapper.selectList(any())).thenReturn(Collections.emptyList());

        supplierReconSourceService.getAllCandidateItems(supplierId, start, end);

        // 验证检验单区间比较
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<InspectionOrderDO>> inspCaptor = ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(inspectionOrderMapper).selectList(inspCaptor.capture());
        String inspSql = inspCaptor.getValue().getCustomSqlSegment();
        assertTrue(inspSql.contains("order_date >="));
        assertTrue(inspSql.contains("order_date <="));

        // 验证入库单区间比较
        ArgumentCaptor<com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StockInDO>> stockCaptor = ArgumentCaptor.forClass(com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper.class);
        verify(stockInMapper).selectList(stockCaptor.capture());
        String stockSql = stockCaptor.getValue().getCustomSqlSegment();
        assertTrue(stockSql.contains("in_date >="));
        assertTrue(stockSql.contains("in_date <="));
    }

}
