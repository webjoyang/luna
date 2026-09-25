package com.luna.lunasrv.module.lunaerp.service.stock.stockin;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.module.lunaerp.controller.stock.stockin.vo.*;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.greigetype.GreigeTypeDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salescontract.SalesContractDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.greigetype.GreigeTypeMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salescontract.SalesContractMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemPlacementMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockledger.StockLedgerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouselocation.WarehouseLocationMapper;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemPlacementDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouselocation.WarehouseLocationDO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link StockInServiceImpl} 的纯 Mockito 单元测试
 *
 * 覆盖关键业务行为：
 * 1. 自动取号（IN-YYYYMMDD-NNN 按天取号）、查重与重试
 * 2. 手填单号查重（含已删除行，撞号报 1_030_011_001）
 * 3. 关联存在性校验与快照回填（仓库/供应商/合同/胚布单/染色单/胚布品种/产品）
 * 4. 聚合保存与服务端金额权威重算（零明细草稿允许）
 * 5. 库位分配与归属校验（placement 必须属于本单仓库，否则报 1_030_011_004）
 * 6. 状态机守卫：仅草稿态可编辑/删除/审核（非草稿报 1_030_011_002）
 * 7. 级联逻辑删除明细及放置记录
 * 8. 详情查询与分页查询
 * 9. 只读取号预览
 * 10. 审核过账（DRAFT→COMPLETED，按 placement 展开写正数流水；零明细报 1_030_011_005）
 * 11. 取消作废与红冲（DRAFT→CANCELLED 作废不写流水；COMPLETED→CANCELLED 红冲写负数流水）
 * 12. 权限分权（草稿作废 :update，审核与红冲 :audit）
 *
 * @author luna
 */
class StockInServiceImplTest {

    @Mock
    private StockInMapper stockInMapper;

    @Mock
    private StockInItemMapper stockInItemMapper;

    @Mock
    private StockInItemPlacementMapper stockInItemPlacementMapper;

    @Mock
    private StockLedgerMapper stockLedgerMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @Mock
    private WarehouseLocationMapper warehouseLocationMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private SalesContractMapper salesContractMapper;

    @Mock
    private GreigeOrderMapper greigeOrderMapper;

    @Mock
    private DyeingOrderMapper dyeingOrderMapper;

    @Mock
    private GreigeTypeMapper greigeTypeMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private SecurityFrameworkService securityFrameworkService;

    @InjectMocks
    private StockInServiceImpl stockInService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), StockInDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), StockInItemDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), StockInItemPlacementDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), StockLedgerDO.class);

        when(securityFrameworkService.hasPermission(anyString())).thenReturn(true);
        when(stockInMapper.updateById(any(StockInDO.class))).thenReturn(1);
        when(stockLedgerMapper.insert(any(StockLedgerDO.class))).thenReturn(1);

        when(stockInMapper.insert(any(StockInDO.class))).thenAnswer(invocation -> {
            StockInDO dobj = invocation.getArgument(0);
            dobj.setId(100L);
            return 1;
        });
        when(stockInItemMapper.insert(any(StockInItemDO.class))).thenAnswer(invocation -> {
            StockInItemDO item = invocation.getArgument(0);
            if (item.getId() == null) {
                item.setId(200L);
            }
            return 1;
        });
        when(stockInItemPlacementMapper.insert(any(StockInItemPlacementDO.class))).thenAnswer(invocation -> {
            StockInItemPlacementDO placement = invocation.getArgument(0);
            if (placement.getId() == null) {
                placement.setId(300L);
            }
            return 1;
        });
    }

    private WarehouseDO mockWarehouse(Long id, String name, String status) {
        WarehouseDO warehouse = new WarehouseDO();
        warehouse.setId(id);
        warehouse.setName(name);
        warehouse.setStatus(status);
        return warehouse;
    }

    private WarehouseLocationDO mockLocation(Long id, Long warehouseId, String code, String name) {
        WarehouseLocationDO loc = new WarehouseLocationDO();
        loc.setId(id);
        loc.setWarehouseId(warehouseId);
        loc.setCode(code);
        loc.setName(name);
        loc.setStatus("ACTIVE");
        return loc;
    }

    private StockInSaveReqVO buildCreateReqVO() {
        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setInType("PURCHASE");
        req.setWarehouseId(10L);
        req.setInDate(LocalDate.now());
        req.setRemark("测试入库");

        List<StockInItemSaveReqVO> items = new ArrayList<>();
        StockInItemSaveReqVO item = new StockInItemSaveReqVO();
        item.setProductId(1L);
        item.setBatchNo("BATCH-01");
        item.setRollNo("R-01");
        item.setPieceCount(1);
        item.setQuantity(new BigDecimal("100.00"));
        item.setUnit("KG");
        item.setUnitPrice(new BigDecimal("25.00"));
        items.add(item);
        req.setItems(items);
        return req;
    }

    // ==================== 创建与取号 ====================

    @Test
    void createStockIn_autoNumber_success() {
        StockInSaveReqVO req = buildCreateReqVO();
        req.setSupplierId(20L);
        req.setSalesContractId(30L);
        req.setGreigeOrderId(40L);
        req.setDyeingOrderId(50L);
        req.setGreigeTypeId(60L);

        // Mock 仓库/供应商/合同/胚布单/染色单/胚布品种
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        SupplierDO supplier = new SupplierDO();
        supplier.setId(20L);
        supplier.setCompanyName("盛泽织造厂");
        when(supplierMapper.selectById(20L)).thenReturn(supplier);

        SalesContractDO contract = new SalesContractDO();
        contract.setId(30L);
        contract.setContractNo("CH20260001");
        when(salesContractMapper.selectById(30L)).thenReturn(contract);

        GreigeOrderDO greigeOrder = new GreigeOrderDO();
        greigeOrder.setId(40L);
        greigeOrder.setOrderNo("PB0924001");
        when(greigeOrderMapper.selectById(40L)).thenReturn(greigeOrder);

        DyeingOrderDO dyeingOrder = new DyeingOrderDO();
        dyeingOrder.setId(50L);
        when(dyeingOrderMapper.selectById(50L)).thenReturn(dyeingOrder);

        GreigeTypeDO greigeType = new GreigeTypeDO();
        greigeType.setId(60L);
        greigeType.setName("40S 纯棉单面");
        when(greigeTypeMapper.selectById(60L)).thenReturn(greigeType);

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P-1001");
        product.setName("纯棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        // 取号扫描：无历史单据
        when(stockInMapper.selectListByInNoPrefixIncludingDeleted(anyString())).thenReturn(Collections.emptyList());
        when(stockInMapper.selectByInNoIncludingDeleted(anyString())).thenReturn(null);

        Long id = stockInService.createStockIn(req);
        assertEquals(100L, id);

        ArgumentCaptor<StockInDO> captor = ArgumentCaptor.forClass(StockInDO.class);
        verify(stockInMapper).insert(captor.capture());
        StockInDO saved = captor.getValue();

        String expectedPrefix = "IN-" + LocalDate.now().format(DATE_FORMATTER) + "-001";
        assertEquals(expectedPrefix, saved.getInNo());
        assertEquals("DRAFT", saved.getStatus());
        assertEquals("一号仓", saved.getWarehouseName());
        assertEquals("盛泽织造厂", saved.getSupplierName());
        assertEquals("CH20260001", saved.getContractNo());
        assertEquals("PB0924001", saved.getGreigeOrderNo());
        assertEquals("40S 纯棉单面", saved.getGreigeTypeName());
        assertEquals(new BigDecimal("2500.00"), saved.getTotalAmount());

        // 验证明细保存
        ArgumentCaptor<StockInItemDO> itemCaptor = ArgumentCaptor.forClass(StockInItemDO.class);
        verify(stockInItemMapper).insert(itemCaptor.capture());
        StockInItemDO item0 = itemCaptor.getValue();
        assertEquals(100L, item0.getStockInId());
        assertEquals("P-1001", item0.getProductCode());
        assertEquals("纯棉汗布", item0.getProductName());
        assertEquals(new BigDecimal("2500.00"), item0.getAmount());
    }

    @Test
    void createStockIn_manualInNo_duplicate_throwsException() {
        StockInSaveReqVO req = buildCreateReqVO();
        req.setInNo("IN-20260924-888");

        StockInDO existing = new StockInDO();
        existing.setId(999L);
        existing.setInNo("IN-20260924-888");
        when(stockInMapper.selectByInNoIncludingDeleted("IN-20260924-888")).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockIn(req));
        assertEquals(STOCK_IN_NO_DUPLICATE.getCode(), ex.getCode());
        verify(stockInMapper, never()).insert(any(StockInDO.class));
    }

    @Test
    void createStockIn_autoNumber_withRetry_incrementsSeq() {
        StockInSaveReqVO req = buildCreateReqVO();
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        when(productMapper.selectById(1L)).thenReturn(product);

        String prefix = "IN-" + LocalDate.now().format(DATE_FORMATTER) + "-";
        StockInDO existing1 = new StockInDO();
        existing1.setId(1L);
        existing1.setInNo(prefix + "001");
        when(stockInMapper.selectListByInNoPrefixIncludingDeleted(prefix))
                .thenReturn(List.of(existing1));
        when(stockInMapper.selectByInNoIncludingDeleted(prefix + "002")).thenReturn(null);

        stockInService.createStockIn(req);

        ArgumentCaptor<StockInDO> captor = ArgumentCaptor.forClass(StockInDO.class);
        verify(stockInMapper).insert(captor.capture());
        assertEquals(prefix + "002", captor.getValue().getInNo());
    }

    @Test
    void createStockIn_emptyItems_allowed_totalAmountNull() {
        StockInSaveReqVO req = buildCreateReqVO();
        req.setItems(Collections.emptyList());
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(stockInMapper.selectListByInNoPrefixIncludingDeleted(anyString())).thenReturn(Collections.emptyList());

        Long id = stockInService.createStockIn(req);
        assertEquals(100L, id);

        ArgumentCaptor<StockInDO> captor = ArgumentCaptor.forClass(StockInDO.class);
        verify(stockInMapper).insert(captor.capture());
        assertNull(captor.getValue().getTotalAmount());
        verify(stockInItemMapper, never()).insert(any(StockInItemDO.class));
    }

    // ==================== 关联校验异常 ====================

    @Test
    void createStockIn_warehouseNotExists_throwsException() {
        StockInSaveReqVO req = buildCreateReqVO();
        when(warehouseMapper.selectById(10L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockIn(req));
        assertEquals(STOCK_IN_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void createStockIn_warehouseInactive_throwsException() {
        StockInSaveReqVO req = buildCreateReqVO();
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "INACTIVE"));

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockIn(req));
        assertEquals(STOCK_IN_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void createStockIn_supplierNotExists_throwsException() {
        StockInSaveReqVO req = buildCreateReqVO();
        req.setSupplierId(999L);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(supplierMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockIn(req));
        assertEquals(STOCK_IN_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void createStockIn_productNotExists_throwsException() {
        StockInSaveReqVO req = buildCreateReqVO();
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(productMapper.selectById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockIn(req));
        assertEquals(STOCK_IN_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
    }

    // ==================== 修改与状态守卫 ====================

    @Test
    void updateStockIn_draftStatus_success() {
        StockInSaveReqVO req = buildCreateReqVO();
        req.setId(100L);
        req.setInNo("IN-20260924-001");

        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        existing.setInNo("IN-20260924-001");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P-1001");
        product.setName("纯棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        stockInService.updateStockIn(req);

        verify(stockInMapper).updateById(any(StockInDO.class));
        verify(stockInItemPlacementMapper).deleteByStockInId(100L);
        verify(stockInItemMapper).deleteByStockInId(100L);
        verify(stockInItemMapper).insert(any(StockInItemDO.class));
    }

    @Test
    void updateStockIn_completedStatus_throwsException() {
        StockInSaveReqVO req = buildCreateReqVO();
        req.setId(100L);

        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("COMPLETED");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.updateStockIn(req));
        assertEquals(STOCK_IN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
        verify(stockInMapper, never()).updateById(any(StockInDO.class));
    }

    @Test
    void updateStockIn_notExists_throwsException() {
        StockInSaveReqVO req = buildCreateReqVO();
        req.setId(999L);
        when(stockInMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.updateStockIn(req));
        assertEquals(STOCK_IN_NOT_EXISTS.getCode(), ex.getCode());
    }

    // ==================== 删除与状态守卫 ====================

    @Test
    void deleteStockIn_draftStatus_success() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        stockInService.deleteStockIn(100L);

        verify(stockInMapper).deleteById(100L);
        verify(stockInItemMapper).deleteByStockInId(100L);
        verify(stockInItemPlacementMapper).deleteByStockInId(100L);
    }

    @Test
    void deleteStockIn_completedStatus_throwsException() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("COMPLETED");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.deleteStockIn(100L));
        assertEquals(STOCK_IN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
        verify(stockInMapper, never()).deleteById(anyLong());
        verify(stockInItemMapper, never()).deleteByStockInId(anyLong());
    }

    @Test
    void deleteStockIn_notExists_throwsException() {
        when(stockInMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.deleteStockIn(999L));
        assertEquals(STOCK_IN_NOT_EXISTS.getCode(), ex.getCode());
    }

    // ==================== 详情与取号预览 ====================

    @Test
    void getStockIn_success() {
        StockInDO stockIn = new StockInDO();
        stockIn.setId(100L);
        stockIn.setInNo("IN-20260924-001");
        stockIn.setStatus("DRAFT");
        stockIn.setWarehouseName("一号仓");
        when(stockInMapper.selectById(100L)).thenReturn(stockIn);

        StockInItemDO item = new StockInItemDO();
        item.setId(10L);
        item.setStockInId(100L);
        item.setProductCode("P-1001");
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(List.of(item));

        StockInRespVO resp = stockInService.getStockIn(100L);
        assertNotNull(resp);
        assertEquals("IN-20260924-001", resp.getInNo());
        assertEquals(1, resp.getItems().size());
        assertEquals("P-1001", resp.getItems().get(0).getProductCode());
    }

    @Test
    void getNextInNo_preview_success() {
        String prefix = "IN-" + LocalDate.now().format(DATE_FORMATTER) + "-";
        StockInDO existing = new StockInDO();
        existing.setId(1L);
        existing.setInNo(prefix + "005");
        when(stockInMapper.selectListByInNoPrefixIncludingDeleted(prefix))
                .thenReturn(List.of(existing));

        String nextNo = stockInService.getNextInNo(null);
        assertEquals(prefix + "006", nextNo);
    }

    @Test
    void getStockInPage_success() {
        StockInPageReqVO pageReqVO = new StockInPageReqVO();
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(10);
        pageReqVO.setInType("PURCHASE");
        pageReqVO.setKeyword("TEST");

        StockInDO row = new StockInDO();
        row.setId(1L);
        row.setInNo("IN-20260924-001");
        row.setInType("PURCHASE");
        row.setStatus("DRAFT");

        PageResult<StockInDO> pageResult = new PageResult<>(List.of(row), 1L);
        when(stockInMapper.selectPage(eq(pageReqVO), any())).thenReturn(pageResult);

        PageResult<StockInRespVO> result = stockInService.getStockInPage(pageReqVO);
        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
        assertEquals("IN-20260924-001", result.getList().get(0).getInNo());
    }

    // ==================== 库位分配与归属校验 ====================

    @Test
    void createStockIn_withPlacements_success() {
        StockInSaveReqVO req = buildCreateReqVO();
        StockInItemSaveReqVO item = req.getItems().get(0);
        item.setPieceCount(10);
        item.setQuantity(new BigDecimal("100.00"));

        List<StockInItemPlacementSaveReqVO> placements = new ArrayList<>();
        StockInItemPlacementSaveReqVO p1 = new StockInItemPlacementSaveReqVO();
        p1.setLocationId(301L);
        p1.setPieceCount(6);
        p1.setQuantity(new BigDecimal("60.00"));
        p1.setRemark("A区首层");
        placements.add(p1);

        StockInItemPlacementSaveReqVO p2 = new StockInItemPlacementSaveReqVO();
        p2.setLocationId(302L);
        p2.setPieceCount(4);
        p2.setQuantity(new BigDecimal("40.00"));
        p2.setRemark("A区二层");
        placements.add(p2);

        item.setPlacements(placements);

        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P-1001");
        product.setName("纯棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        when(warehouseLocationMapper.selectById(301L)).thenReturn(mockLocation(301L, 10L, "A-01", "A区01"));
        when(warehouseLocationMapper.selectById(302L)).thenReturn(mockLocation(302L, 10L, "A-02", "A区02"));

        when(stockInMapper.selectListByInNoPrefixIncludingDeleted(anyString())).thenReturn(Collections.emptyList());

        Long id = stockInService.createStockIn(req);
        assertEquals(100L, id);

        ArgumentCaptor<StockInItemPlacementDO> placementCaptor = ArgumentCaptor.forClass(StockInItemPlacementDO.class);
        verify(stockInItemPlacementMapper, times(2)).insert(placementCaptor.capture());
        List<StockInItemPlacementDO> savedPlacements = placementCaptor.getAllValues();
        assertEquals(2, savedPlacements.size());

        StockInItemPlacementDO savedP1 = savedPlacements.get(0);
        assertEquals(100L, savedP1.getStockInId());
        assertEquals(200L, savedP1.getStockInItemId());
        assertEquals(301L, savedP1.getLocationId());
        assertEquals("A-01", savedP1.getLocationCode());
        assertEquals(6, savedP1.getPieceCount());
        assertEquals(new BigDecimal("60.00"), savedP1.getQuantity());
        assertEquals("KG", savedP1.getUnit());

        StockInItemPlacementDO savedP2 = savedPlacements.get(1);
        assertEquals(100L, savedP2.getStockInId());
        assertEquals(200L, savedP2.getStockInItemId());
        assertEquals(302L, savedP2.getLocationId());
        assertEquals("A-02", savedP2.getLocationCode());
        assertEquals(4, savedP2.getPieceCount());
        assertEquals(new BigDecimal("40.00"), savedP2.getQuantity());
        assertEquals("KG", savedP2.getUnit());
    }

    @Test
    void createStockIn_partialPlacements_unequalTotals_allowed_success() {
        StockInSaveReqVO req = buildCreateReqVO();
        StockInItemSaveReqVO item = req.getItems().get(0);
        item.setPieceCount(10);
        item.setQuantity(new BigDecimal("100.00"));

        // 仅分配一条 6 匹 / 60 KG（不齐套/不等于明细匹数和数量），验证不报错、顺利创建（Spec L395）
        List<StockInItemPlacementSaveReqVO> placements = new ArrayList<>();
        StockInItemPlacementSaveReqVO p1 = new StockInItemPlacementSaveReqVO();
        p1.setLocationId(301L);
        p1.setPieceCount(6);
        p1.setQuantity(new BigDecimal("60.00"));
        placements.add(p1);
        item.setPlacements(placements);

        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P-1001");
        product.setName("纯棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        when(warehouseLocationMapper.selectById(301L)).thenReturn(mockLocation(301L, 10L, "A-01", "A区01"));
        when(stockInMapper.selectListByInNoPrefixIncludingDeleted(anyString())).thenReturn(Collections.emptyList());

        Long id = stockInService.createStockIn(req);
        assertEquals(100L, id);

        ArgumentCaptor<StockInItemPlacementDO> placementCaptor = ArgumentCaptor.forClass(StockInItemPlacementDO.class);
        verify(stockInItemPlacementMapper).insert(placementCaptor.capture());
        StockInItemPlacementDO saved = placementCaptor.getValue();
        assertEquals(100L, saved.getStockInId());
        assertEquals(301L, saved.getLocationId());
        assertEquals(6, saved.getPieceCount());
        assertEquals(new BigDecimal("60.00"), saved.getQuantity());
    }

    @Test
    void createStockIn_placementLocationNotExists_throwsException() {
        StockInSaveReqVO req = buildCreateReqVO();
        StockInItemSaveReqVO item = req.getItems().get(0);
        StockInItemPlacementSaveReqVO p = new StockInItemPlacementSaveReqVO();
        p.setLocationId(999L);
        p.setQuantity(new BigDecimal("10.00"));
        item.setPlacements(List.of(p));

        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        ProductDO product = new ProductDO();
        product.setId(1L);
        when(productMapper.selectById(1L)).thenReturn(product);

        when(warehouseLocationMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockIn(req));
        assertEquals(STOCK_IN_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
    }

    @Test
    void createStockIn_placementLocationNotInWarehouse_throwsException() {
        StockInSaveReqVO req = buildCreateReqVO();
        StockInItemSaveReqVO item = req.getItems().get(0);
        StockInItemPlacementSaveReqVO p = new StockInItemPlacementSaveReqVO();
        p.setLocationId(301L);
        p.setQuantity(new BigDecimal("10.00"));
        item.setPlacements(List.of(p));

        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        ProductDO product = new ProductDO();
        product.setId(1L);
        when(productMapper.selectById(1L)).thenReturn(product);

        // 库位 301 属于二号仓 20L，不属于本单仓库 10L
        when(warehouseLocationMapper.selectById(301L)).thenReturn(mockLocation(301L, 20L, "B-01", "B区01"));

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockIn(req));
        assertEquals(STOCK_IN_LOCATION_NOT_IN_WAREHOUSE.getCode(), ex.getCode());
        verify(stockInMapper, never()).insert(any(StockInDO.class));
    }

    @Test
    void updateStockIn_replacePlacements_success() {
        StockInSaveReqVO req = buildCreateReqVO();
        req.setId(100L);
        req.setInNo("IN-20260924-001");
        StockInItemSaveReqVO item = req.getItems().get(0);
        StockInItemPlacementSaveReqVO p = new StockInItemPlacementSaveReqVO();
        p.setLocationId(301L);
        p.setQuantity(new BigDecimal("50.00"));
        item.setPlacements(List.of(p));

        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        existing.setInNo("IN-20260924-001");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P-1001");
        product.setName("纯棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        when(warehouseLocationMapper.selectById(301L)).thenReturn(mockLocation(301L, 10L, "A-01", "A区01"));

        stockInService.updateStockIn(req);

        verify(stockInMapper).updateById(any(StockInDO.class));
        verify(stockInItemPlacementMapper).deleteByStockInId(100L);
        verify(stockInItemMapper).deleteByStockInId(100L);
        verify(stockInItemPlacementMapper).insert(any(StockInItemPlacementDO.class));
    }

    @Test
    void getStockIn_withPlacements_returnsNestedPlacements() {
        StockInDO stockIn = new StockInDO();
        stockIn.setId(100L);
        stockIn.setInNo("IN-20260924-001");
        stockIn.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(stockIn);

        StockInItemDO item = new StockInItemDO();
        item.setId(201L);
        item.setStockInId(100L);
        item.setProductId(1L);
        item.setProductCode("P-1001");
        item.setProductName("纯棉汗布");
        item.setQuantity(new BigDecimal("100.00"));
        item.setUnit("KG");
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(List.of(item));

        StockInItemPlacementDO p1 = new StockInItemPlacementDO();
        p1.setId(501L);
        p1.setStockInId(100L);
        p1.setStockInItemId(201L);
        p1.setLocationId(301L);
        p1.setLocationCode("A-01");
        p1.setPieceCount(6);
        p1.setQuantity(new BigDecimal("60.00"));
        p1.setUnit("KG");

        StockInItemPlacementDO p2 = new StockInItemPlacementDO();
        p2.setId(502L);
        p2.setStockInId(100L);
        p2.setStockInItemId(201L);
        p2.setLocationId(302L);
        p2.setLocationCode("A-02");
        p2.setPieceCount(4);
        p2.setQuantity(new BigDecimal("40.00"));
        p2.setUnit("KG");

        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(List.of(p1, p2));

        StockInRespVO resp = stockInService.getStockIn(100L);
        assertNotNull(resp);
        assertEquals(1, resp.getItems().size());
        StockInItemRespVO itemResp = resp.getItems().get(0);
        assertNotNull(itemResp.getPlacements());
        assertEquals(2, itemResp.getPlacements().size());
        assertEquals("A-01", itemResp.getPlacements().get(0).getLocationCode());
        assertEquals(new BigDecimal("60.00"), itemResp.getPlacements().get(0).getQuantity());
        assertEquals("A-02", itemResp.getPlacements().get(1).getLocationCode());
        assertEquals(new BigDecimal("40.00"), itemResp.getPlacements().get(1).getQuantity());
    }

    // ==================== Issue 05: 审核过账与红冲测试 ====================

    @Test
    void auditStockIn_draftWithPlacements_success() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setInNo("IN-20260925-001");
        existing.setStatus("DRAFT");
        existing.setWarehouseId(10L);
        existing.setWarehouseName("原料仓");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        StockInItemDO item = new StockInItemDO();
        item.setId(201L);
        item.setStockInId(100L);
        item.setProductId(1001L);
        item.setProductCode("P001");
        item.setProductName("涤纶纱");
        item.setBatchNo("B01");
        item.setQuantity(new BigDecimal("100.00"));
        item.setUnit("KG");
        item.setUnitPrice(new BigDecimal("10.00"));
        item.setAmount(new BigDecimal("1000.00"));
        item.setRemark("明细备注");
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(List.of(item));

        StockInItemPlacementDO p1 = new StockInItemPlacementDO();
        p1.setId(301L);
        p1.setStockInId(100L);
        p1.setStockInItemId(201L);
        p1.setLocationId(401L);
        p1.setLocationCode("L-01");
        p1.setPieceCount(5);
        p1.setQuantity(new BigDecimal("60.00"));
        p1.setUnit("KG");
        p1.setRemark("放置A");

        StockInItemPlacementDO p2 = new StockInItemPlacementDO();
        p2.setId(302L);
        p2.setStockInId(100L);
        p2.setStockInItemId(201L);
        p2.setLocationId(402L);
        p2.setLocationCode("L-02");
        p2.setPieceCount(4);
        p2.setQuantity(new BigDecimal("40.00"));
        p2.setUnit("KG");
        p2.setRemark("放置B");

        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(List.of(p1, p2));

        stockInService.auditStockIn(100L);

        // 验证主表状态更新为 COMPLETED
        ArgumentCaptor<StockInDO> updateCaptor = ArgumentCaptor.forClass(StockInDO.class);
        verify(stockInMapper).updateById(updateCaptor.capture());
        assertEquals("COMPLETED", updateCaptor.getValue().getStatus());

        // 验证写库存流水：按 placement 展开 2 条正数流水
        ArgumentCaptor<StockLedgerDO> ledgerCaptor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper, times(2)).insert(ledgerCaptor.capture());
        List<StockLedgerDO> ledgers = ledgerCaptor.getAllValues();

        StockLedgerDO l1 = ledgers.get(0);
        assertEquals(1001L, l1.getProductId());
        assertEquals("P001", l1.getProductCode());
        assertEquals("涤纶纱", l1.getProductName());
        assertEquals(10L, l1.getWarehouseId());
        assertEquals("原料仓", l1.getWarehouseName());
        assertEquals(401L, l1.getLocationId());
        assertEquals("L-01", l1.getLocationCode());
        assertEquals("B01", l1.getBatchNo());
        assertEquals("STOCK_IN", l1.getTxnType());
        assertEquals(new BigDecimal("60.00"), l1.getQuantity());
        assertEquals("KG", l1.getUnit());
        assertEquals(new BigDecimal("10.00"), l1.getUnitPrice());
        assertEquals(new BigDecimal("600.00"), l1.getAmount());
        assertEquals("STOCK_IN", l1.getRelatedDocType());
        assertEquals(100L, l1.getRelatedDocId());
        assertEquals("IN-20260925-001", l1.getRelatedDocNo());
        assertEquals("放置A", l1.getRemark());

        StockLedgerDO l2 = ledgers.get(1);
        assertEquals(402L, l2.getLocationId());
        assertEquals("L-02", l2.getLocationCode());
        assertEquals(new BigDecimal("40.00"), l2.getQuantity());
        assertEquals(new BigDecimal("400.00"), l2.getAmount());
        assertEquals("放置B", l2.getRemark());
    }

    @Test
    void auditStockIn_draftWithoutPlacements_success() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setInNo("IN-20260925-002");
        existing.setStatus("DRAFT");
        existing.setWarehouseId(10L);
        existing.setWarehouseName("原料仓");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        StockInItemDO item = new StockInItemDO();
        item.setId(202L);
        item.setStockInId(100L);
        item.setProductId(1002L);
        item.setProductCode("P002");
        item.setProductName("全棉布");
        item.setBatchNo("B02");
        item.setQuantity(new BigDecimal("80.00"));
        item.setUnit("M");
        item.setUnitPrice(new BigDecimal("15.00"));
        item.setAmount(new BigDecimal("1200.00"));
        item.setRemark("整批未分配库位");
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(List.of(item));
        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(Collections.emptyList());

        stockInService.auditStockIn(100L);

        // 验证主表状态更新为 COMPLETED
        ArgumentCaptor<StockInDO> updateCaptor = ArgumentCaptor.forClass(StockInDO.class);
        verify(stockInMapper).updateById(updateCaptor.capture());
        assertEquals("COMPLETED", updateCaptor.getValue().getStatus());

        // 验证写库存流水：无 placement，按明细行写入 1 条，库位为空
        ArgumentCaptor<StockLedgerDO> ledgerCaptor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper, times(1)).insert(ledgerCaptor.capture());
        StockLedgerDO ledger = ledgerCaptor.getValue();
        assertEquals(1002L, ledger.getProductId());
        assertNull(ledger.getLocationId());
        assertNull(ledger.getLocationCode());
        assertEquals("STOCK_IN", ledger.getTxnType());
        assertEquals(new BigDecimal("80.00"), ledger.getQuantity());
        assertEquals("M", ledger.getUnit());
        assertEquals(new BigDecimal("15.00"), ledger.getUnitPrice());
        assertEquals(new BigDecimal("1200.00"), ledger.getAmount());
        assertEquals("整批未分配库位", ledger.getRemark());
    }

    @Test
    void auditStockIn_draftEmptyItems_throws1030011005() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.auditStockIn(100L));
        assertEquals(STOCK_IN_ITEM_EMPTY.getCode(), ex.getCode());
        verify(stockLedgerMapper, never()).insert(any(StockLedgerDO.class));
        verify(stockInMapper, never()).updateById(any(StockInDO.class));
    }

    @Test
    void auditStockIn_completedStatus_throws1030011002() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("COMPLETED");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.auditStockIn(100L));
        assertEquals(STOCK_IN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
        verify(stockLedgerMapper, never()).insert(any(StockLedgerDO.class));
    }

    @Test
    void auditStockIn_cancelledStatus_throws1030011002() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("CANCELLED");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.auditStockIn(100L));
        assertEquals(STOCK_IN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
        verify(stockLedgerMapper, never()).insert(any(StockLedgerDO.class));
    }

    @Test
    void auditStockIn_notExists_throwsException() {
        when(stockInMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.auditStockIn(999L));
        assertEquals(STOCK_IN_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void cancelStockIn_draftStatus_success() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        stockInService.cancelStockIn(100L);

        ArgumentCaptor<StockInDO> updateCaptor = ArgumentCaptor.forClass(StockInDO.class);
        verify(stockInMapper).updateById(updateCaptor.capture());
        assertEquals("CANCELLED", updateCaptor.getValue().getStatus());

        // 草稿作废不写流水
        verify(stockLedgerMapper, never()).insert(any(StockLedgerDO.class));
    }

    @Test
    void cancelStockIn_completedStatus_redFlush_success() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setInNo("IN-20260925-005");
        existing.setStatus("COMPLETED");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        // 模拟已审核单据对应的 2 条原流水
        StockLedgerDO orig1 = new StockLedgerDO();
        orig1.setId(1001L);
        orig1.setProductId(1001L);
        orig1.setProductCode("P001");
        orig1.setProductName("涤纶纱");
        orig1.setWarehouseId(10L);
        orig1.setWarehouseName("原料仓");
        orig1.setLocationId(401L);
        orig1.setLocationCode("L-01");
        orig1.setBatchNo("B01");
        orig1.setTxnType("STOCK_IN");
        orig1.setQuantity(new BigDecimal("60.00"));
        orig1.setUnit("KG");
        orig1.setUnitPrice(new BigDecimal("10.00"));
        orig1.setAmount(new BigDecimal("600.00"));
        orig1.setRelatedDocType("STOCK_IN");
        orig1.setRelatedDocId(100L);
        orig1.setRelatedDocNo("IN-20260925-005");
        orig1.setRemark("放置A");

        StockLedgerDO orig2 = new StockLedgerDO();
        orig2.setId(1002L);
        orig2.setProductId(1001L);
        orig2.setProductCode("P001");
        orig2.setProductName("涤纶纱");
        orig2.setWarehouseId(10L);
        orig2.setWarehouseName("原料仓");
        orig2.setLocationId(402L);
        orig2.setLocationCode("L-02");
        orig2.setBatchNo("B01");
        orig2.setTxnType("STOCK_IN");
        orig2.setQuantity(new BigDecimal("40.00"));
        orig2.setUnit("KG");
        orig2.setUnitPrice(new BigDecimal("10.00"));
        orig2.setAmount(new BigDecimal("400.00"));
        orig2.setRelatedDocType("STOCK_IN");
        orig2.setRelatedDocId(100L);
        orig2.setRelatedDocNo("IN-20260925-005");
        orig2.setRemark("放置B");

        when(stockLedgerMapper.selectListByRelatedDoc("STOCK_IN", 100L, "STOCK_IN")).thenReturn(List.of(orig1, orig2));

        stockInService.cancelStockIn(100L);

        // 验证主表状态更新为已取消终态
        ArgumentCaptor<StockInDO> updateCaptor = ArgumentCaptor.forClass(StockInDO.class);
        verify(stockInMapper).updateById(updateCaptor.capture());
        assertEquals("CANCELLED", updateCaptor.getValue().getStatus());

        // 验证红冲：插入 2 条负数反向行
        ArgumentCaptor<StockLedgerDO> ledgerCaptor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper, times(2)).insert(ledgerCaptor.capture());
        List<StockLedgerDO> reversals = ledgerCaptor.getAllValues();

        StockLedgerDO r1 = reversals.get(0);
        assertEquals("STOCK_IN_REVERSAL", r1.getTxnType());
        assertEquals(new BigDecimal("-60.00"), r1.getQuantity());
        assertEquals(new BigDecimal("-600.00"), r1.getAmount());
        assertEquals(401L, r1.getLocationId());
        assertEquals("L-01", r1.getLocationCode());
        assertEquals("B01", r1.getBatchNo());
        assertEquals("放置A", r1.getRemark());

        StockLedgerDO r2 = reversals.get(1);
        assertEquals("STOCK_IN_REVERSAL", r2.getTxnType());
        assertEquals(new BigDecimal("-40.00"), r2.getQuantity());
        assertEquals(new BigDecimal("-400.00"), r2.getAmount());
        assertEquals(402L, r2.getLocationId());
        assertEquals("L-02", r2.getLocationCode());
        assertEquals("B01", r2.getBatchNo());
        assertEquals("放置B", r2.getRemark());

        // 原流水永不物理删除
        verify(stockLedgerMapper, never()).deleteById(anyLong());
    }

    @Test
    void cancelStockIn_cancelledStatus_throws1030011002() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("CANCELLED");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.cancelStockIn(100L));
        assertEquals(STOCK_IN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
        verify(stockLedgerMapper, never()).insert(any(StockLedgerDO.class));
    }

    @Test
    void cancelStockIn_draftNoUpdatePermission_throwsAccessDenied() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(securityFrameworkService.hasPermission("lunaerp:stock:stock-in:update")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> stockInService.cancelStockIn(100L));
        verify(stockInMapper, never()).updateById(any(StockInDO.class));
    }

    @Test
    void cancelStockIn_completedNoAuditPermission_throwsAccessDenied() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("COMPLETED");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(securityFrameworkService.hasPermission("lunaerp:stock:stock-in:audit")).thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> stockInService.cancelStockIn(100L));
        verify(stockInMapper, never()).updateById(any(StockInDO.class));
    }

    @Test
    void updateStockIn_cancelledStatus_throws1030011002() {
        StockInSaveReqVO req = buildCreateReqVO();
        req.setId(100L);

        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("CANCELLED");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.updateStockIn(req));
        assertEquals(STOCK_IN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

    @Test
    void deleteStockIn_cancelledStatus_throws1030011002() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("CANCELLED");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.deleteStockIn(100L));
        assertEquals(STOCK_IN_STATUS_NOT_ALLOWED.getCode(), ex.getCode());
    }

}

