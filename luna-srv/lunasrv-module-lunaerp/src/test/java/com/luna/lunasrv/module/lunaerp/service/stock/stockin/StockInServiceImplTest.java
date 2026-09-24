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
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemPlacementMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

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
 * 5. 状态机守卫：仅草稿态可编辑/删除（非草稿报 1_030_011_002）
 * 6. 级联逻辑删除明细
 * 7. 详情查询与分页查询
 * 8. 只读取号预览
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
    private WarehouseMapper warehouseMapper;

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

        when(stockInMapper.insert(any(StockInDO.class))).thenAnswer(invocation -> {
            StockInDO dobj = invocation.getArgument(0);
            dobj.setId(100L);
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
        ArgumentCaptor<List<StockInItemDO>> itemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(stockInItemMapper).insertBatch(itemsCaptor.capture());
        List<StockInItemDO> savedItems = itemsCaptor.getValue();
        assertEquals(1, savedItems.size());
        StockInItemDO item0 = savedItems.get(0);
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
        verify(stockInItemMapper, never()).insertBatch(anyList());
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
        verify(stockInItemMapper).deleteByStockInId(100L);
        verify(stockInItemMapper).insertBatch(anyList());
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

}
