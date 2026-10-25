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
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderGenerateStockInReqVO;
import com.luna.lunasrv.module.lunaerp.controller.production.inspectionorder.vo.InspectionOrderGenerateStockInRespVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.InspectionOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.inspectionorder.item.InspectionOrderItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.weavingorder.ticket.WeavingTicketDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.InspectionOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.inspectionorder.item.InspectionOrderItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.weavingorder.ticket.WeavingTicketMapper;
import com.luna.lunasrv.module.lunaerp.service.production.weavingorder.ticket.WeavingTicketService;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnorder.YarnOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnorder.YarnOrderMapper;
import com.luna.lunasrv.framework.security.core.service.SecurityFrameworkService;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockledger.StockLedgerDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private YarnSpecMapper yarnSpecMapper;

    @Mock
    private SecurityFrameworkService securityFrameworkService;

    @Mock
    private InspectionOrderMapper inspectionOrderMapper;

    @Mock
    private InspectionOrderItemMapper inspectionOrderItemMapper;

    @Mock
    private WeavingTicketService weavingTicketService;

    @Mock
    private WeavingTicketMapper weavingTicketMapper;

    @Mock
    private YarnOrderMapper yarnOrderMapper;

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
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), InspectionOrderDO.class);
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), ""), InspectionOrderItemDO.class);

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

    private YarnOrderDO mockYarnOrder(Long id, String orderNo) {
        YarnOrderDO order = new YarnOrderDO();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setStatus("EFFECTIVE");
        return order;
    }

    private ProductDO mockProduct(Long id, String name, String code) {
        ProductDO product = new ProductDO();
        product.setId(id);
        product.setName(name);
        product.setCode(code);
        return product;
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

    @Test
    void createStockIn_withYarnOrder_success() {
        StockInSaveReqVO req = buildCreateReqVO();
        req.setYarnOrderId(888L);

        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(productMapper.selectById(1L)).thenReturn(mockProduct(1L, "全棉汗布", "P001"));
        when(yarnOrderMapper.selectById(888L)).thenReturn(mockYarnOrder(888L, "YO-20261015-001"));

        Long id = stockInService.createStockIn(req);

        ArgumentCaptor<StockInDO> captor = ArgumentCaptor.forClass(StockInDO.class);
        verify(stockInMapper).insert(captor.capture());
        StockInDO saved = captor.getValue();
        assertEquals(888L, saved.getYarnOrderId());
        assertEquals("YO-20261015-001", saved.getYarnOrderNo());

        // 零回写：不更新采购单，仅只读查询一次
        verify(yarnOrderMapper, times(1)).selectById(888L);
        verifyNoMoreInteractions(yarnOrderMapper);
    }

    @Test
    void createStockIn_yarnOrderNotExists_throwsException() {
        StockInSaveReqVO req = buildCreateReqVO();
        req.setYarnOrderId(999L);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(yarnOrderMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockIn(req));
        assertEquals(STOCK_IN_REFERENCE_NOT_FOUND.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("纱线采购单"));
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

    @Test
    void updateStockIn_changeYarnOrder_success() {
        StockInSaveReqVO req = buildCreateReqVO();
        req.setId(100L);
        req.setInNo("IN-20260924-001");
        req.setYarnOrderId(777L);

        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        existing.setInNo("IN-20260924-001");
        existing.setYarnOrderId(888L);
        existing.setYarnOrderNo("YO-20261015-001");

        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(productMapper.selectById(1L)).thenReturn(mockProduct(1L, "全棉汗布", "P001"));
        when(yarnOrderMapper.selectById(777L)).thenReturn(mockYarnOrder(777L, "YO-20261015-002"));

        stockInService.updateStockIn(req);

        ArgumentCaptor<StockInDO> captor = ArgumentCaptor.forClass(StockInDO.class);
        verify(stockInMapper).updateById(captor.capture());
        StockInDO updated = captor.getValue();
        assertEquals(777L, updated.getYarnOrderId());
        assertEquals("YO-20261015-002", updated.getYarnOrderNo());

        // 零回写：不更新采购单，仅只读查询一次
        verify(yarnOrderMapper, times(1)).selectById(777L);
        verifyNoMoreInteractions(yarnOrderMapper);
    }

    @Test
    void updateStockIn_clearYarnOrder_success() {
        StockInSaveReqVO req = buildCreateReqVO();
        req.setId(100L);
        req.setInNo("IN-20260924-001");
        req.setYarnOrderId(null);

        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        existing.setInNo("IN-20260924-001");
        existing.setYarnOrderId(888L);
        existing.setYarnOrderNo("YO-20261015-001");

        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(productMapper.selectById(1L)).thenReturn(mockProduct(1L, "全棉汗布", "P001"));

        stockInService.updateStockIn(req);

        ArgumentCaptor<StockInDO> captor = ArgumentCaptor.forClass(StockInDO.class);
        verify(stockInMapper).updateById(captor.capture());
        StockInDO updated = captor.getValue();
        assertNull(updated.getYarnOrderId());
        assertNull(updated.getYarnOrderNo());

        verifyNoInteractions(yarnOrderMapper);
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
    void getStockIn_withYarnOrder_success() {
        StockInDO stockIn = new StockInDO();
        stockIn.setId(100L);
        stockIn.setInNo("IN-20260924-001");
        stockIn.setStatus("DRAFT");
        stockIn.setWarehouseName("一号仓");
        stockIn.setYarnOrderId(888L);
        stockIn.setYarnOrderNo("YO-20261015-001");
        when(stockInMapper.selectById(100L)).thenReturn(stockIn);
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Collections.emptyList());

        StockInRespVO resp = stockInService.getStockIn(100L);
        assertNotNull(resp);
        assertEquals(888L, resp.getYarnOrderId());
        assertEquals("YO-20261015-001", resp.getYarnOrderNo());
    }

    @Test
    void getStockIn_withoutYarnOrder_nullCompatible() {
        StockInDO stockIn = new StockInDO();
        stockIn.setId(100L);
        stockIn.setInNo("IN-20260924-001");
        stockIn.setStatus("DRAFT");
        stockIn.setWarehouseName("一号仓");
        stockIn.setYarnOrderId(null);
        stockIn.setYarnOrderNo(null);
        when(stockInMapper.selectById(100L)).thenReturn(stockIn);
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Collections.emptyList());

        StockInRespVO resp = stockInService.getStockIn(100L);
        assertNotNull(resp);
        assertNull(resp.getYarnOrderId());
        assertNull(resp.getYarnOrderNo());
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

    // ==================== ADR-0032 入库明细与库位放置两级差异保存 ====================

    @Test
    void updateStockIn_path1_allItemsSubmittedWithId_updatesItemsAndRetainsPlacements() {
        // Path 1: 存量明细与放置全部原样携带 id 提交 -> updateById，零逻辑删除，零 insert
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        existing.setInNo("IN-20260924-001");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(warehouseLocationMapper.selectById(301L)).thenReturn(mockLocation(301L, 10L, "A-01", "A区01"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P-1001");
        product.setName("纯棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        // DB 存量：1 行明细 201 + 1 条放置 301
        StockInItemDO dbItem = new StockInItemDO();
        dbItem.setId(201L);
        dbItem.setStockInId(100L);
        dbItem.setProductId(1L);
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Collections.singletonList(dbItem));

        StockInItemPlacementDO dbPlacement = new StockInItemPlacementDO();
        dbPlacement.setId(301L);
        dbPlacement.setStockInId(100L);
        dbPlacement.setStockInItemId(201L);
        dbPlacement.setLocationId(301L);
        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(Collections.singletonList(dbPlacement));

        // 提交载荷
        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setId(100L);
        req.setInNo("IN-20260924-001");
        req.setWarehouseId(10L);

        StockInItemSaveReqVO itemReq = new StockInItemSaveReqVO();
        itemReq.setId(201L);
        itemReq.setItemType("PRODUCT");
        itemReq.setProductId(1L);
        itemReq.setQuantity(new BigDecimal("100.00"));
        itemReq.setUnit("KG");
        itemReq.setUnitPrice(new BigDecimal("20.00"));

        StockInItemPlacementSaveReqVO pReq = new StockInItemPlacementSaveReqVO();
        pReq.setId(301L);
        pReq.setLocationId(301L);
        pReq.setQuantity(new BigDecimal("100.00"));
        itemReq.setPlacements(Collections.singletonList(pReq));
        req.setItems(Collections.singletonList(itemReq));

        stockInService.updateStockIn(req);

        // 验证只调 updateById，不调 delete / insert
        verify(stockInItemMapper).updateById(any(StockInItemDO.class));
        verify(stockInItemPlacementMapper).updateById(any(StockInItemPlacementDO.class));
        verify(stockInItemMapper, never()).deleteByIds(any());
        verify(stockInItemMapper, never()).deleteByStockInId(anyLong());
        verify(stockInItemPlacementMapper, never()).deleteByIds(any());
        verify(stockInItemPlacementMapper, never()).deleteByStockInId(anyLong());
        verify(stockInItemMapper, never()).insert(any(StockInItemDO.class));
        verify(stockInItemPlacementMapper, never()).insert(any(StockInItemPlacementDO.class));
    }

    @Test
    void updateStockIn_path2_modifyItemFields_updatesItemAndAuthoritativelyRecalculatesAmount_retainsPlacements() {
        // Path 2: 修改明细字段（数量/单价） -> updateById，金额权威重算覆盖前端传入，其下放置记录原样保留
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(warehouseLocationMapper.selectById(301L)).thenReturn(mockLocation(301L, 10L, "A-01", "A区01"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P-1001");
        product.setName("纯棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        StockInItemDO dbItem = new StockInItemDO();
        dbItem.setId(201L);
        dbItem.setStockInId(100L);
        dbItem.setProductId(1L);
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Collections.singletonList(dbItem));

        StockInItemPlacementDO dbPlacement = new StockInItemPlacementDO();
        dbPlacement.setId(301L);
        dbPlacement.setStockInId(100L);
        dbPlacement.setStockInItemId(201L);
        dbPlacement.setLocationId(301L);
        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(Collections.singletonList(dbPlacement));

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setId(100L);
        req.setWarehouseId(10L);

        StockInItemSaveReqVO itemReq = new StockInItemSaveReqVO();
        itemReq.setId(201L);
        itemReq.setItemType("PRODUCT");
        itemReq.setProductId(1L);
        itemReq.setQuantity(new BigDecimal("80.00"));
        itemReq.setUnit("KG");
        itemReq.setUnitPrice(new BigDecimal("25.00"));
        itemReq.setAmount(new BigDecimal("9999.00")); // 前端传错金额

        StockInItemPlacementSaveReqVO pReq = new StockInItemPlacementSaveReqVO();
        pReq.setId(301L);
        pReq.setLocationId(301L);
        pReq.setQuantity(new BigDecimal("80.00"));
        itemReq.setPlacements(Collections.singletonList(pReq));
        req.setItems(Collections.singletonList(itemReq));

        stockInService.updateStockIn(req);

        ArgumentCaptor<StockInItemDO> itemCaptor = ArgumentCaptor.forClass(StockInItemDO.class);
        verify(stockInItemMapper).updateById(itemCaptor.capture());
        assertEquals(new BigDecimal("2000.00"), itemCaptor.getValue().getAmount()); // 80 * 25

        ArgumentCaptor<StockInDO> stockInCaptor = ArgumentCaptor.forClass(StockInDO.class);
        verify(stockInMapper).updateById(stockInCaptor.capture());
        assertEquals(new BigDecimal("2000.00"), stockInCaptor.getValue().getTotalAmount());

        verify(stockInItemPlacementMapper).updateById((StockInItemPlacementDO) any());
        verify(stockInItemPlacementMapper, never()).deleteByIds(any());
        verify(stockInItemPlacementMapper, never()).insert((StockInItemPlacementDO) any());
    }

    @Test
    void updateStockIn_path3_removeItem_logicallyDeletesItemAndItsPlacements_otherItemsUntouched() {
        // Path 3: 原有 2 行(201, 202)，提交时仅保留 201，删除 202；202 下放置 302 一并逻辑删除，201 与 301 正常保留
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(warehouseLocationMapper.selectById(301L)).thenReturn(mockLocation(301L, 10L, "A-01", "A区01"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P-1001");
        product.setName("纯棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        StockInItemDO dbItem1 = new StockInItemDO();
        dbItem1.setId(201L);
        dbItem1.setStockInId(100L);
        StockInItemDO dbItem2 = new StockInItemDO();
        dbItem2.setId(202L);
        dbItem2.setStockInId(100L);
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Arrays.asList(dbItem1, dbItem2));

        StockInItemPlacementDO dbP1 = new StockInItemPlacementDO();
        dbP1.setId(301L);
        dbP1.setStockInId(100L);
        dbP1.setStockInItemId(201L);
        StockInItemPlacementDO dbP2 = new StockInItemPlacementDO();
        dbP2.setId(302L);
        dbP2.setStockInId(100L);
        dbP2.setStockInItemId(202L);
        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(Arrays.asList(dbP1, dbP2));

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setId(100L);
        req.setWarehouseId(10L);

        StockInItemSaveReqVO itemReq = new StockInItemSaveReqVO();
        itemReq.setId(201L);
        itemReq.setItemType("PRODUCT");
        itemReq.setProductId(1L);
        itemReq.setQuantity(new BigDecimal("50.00"));
        itemReq.setUnit("KG");

        StockInItemPlacementSaveReqVO pReq = new StockInItemPlacementSaveReqVO();
        pReq.setId(301L);
        pReq.setLocationId(301L);
        pReq.setQuantity(new BigDecimal("50.00"));
        itemReq.setPlacements(Collections.singletonList(pReq));
        req.setItems(Collections.singletonList(itemReq));

        stockInService.updateStockIn(req);

        // 验证 202 及其放置 302 被逻辑删除
        verify(stockInItemMapper).deleteByIds(Collections.singleton(202L));
        verify(stockInItemPlacementMapper).deleteByIds(Collections.singleton(302L));
        // 验证 201 与 301 正常更新
        ArgumentCaptor<StockInItemDO> itemUpdateCaptor = ArgumentCaptor.forClass(StockInItemDO.class);
        verify(stockInItemMapper).updateById(itemUpdateCaptor.capture());
        assertEquals(201L, itemUpdateCaptor.getValue().getId());

        ArgumentCaptor<StockInItemPlacementDO> pUpdateCaptor = ArgumentCaptor.forClass(StockInItemPlacementDO.class);
        verify(stockInItemPlacementMapper).updateById(pUpdateCaptor.capture());
        assertEquals(301L, pUpdateCaptor.getValue().getId());
    }

    @Test
    void updateStockIn_path4_addNewItem_insertsItemAndItsPlacementsWithNewItemId() {
        // Path 4: 新增明细行（id=null）及其放置记录（id=null） -> 明细 insert 生成 id，放置记录的 stockInItemId 自动关联新明细
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(warehouseLocationMapper.selectById(301L)).thenReturn(mockLocation(301L, 10L, "A-01", "A区01"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P-1001");
        product.setName("纯棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        // DB 初始为空
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Collections.emptyList());
        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(Collections.emptyList());

        when(stockInItemMapper.insert(any(StockInItemDO.class))).thenAnswer(inv -> {
            StockInItemDO item = inv.getArgument(0);
            item.setId(205L);
            return 1;
        });

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setId(100L);
        req.setWarehouseId(10L);

        StockInItemSaveReqVO newItem = new StockInItemSaveReqVO();
        newItem.setItemType("PRODUCT");
        newItem.setProductId(1L);
        newItem.setQuantity(new BigDecimal("60.00"));
        newItem.setUnit("KG");

        StockInItemPlacementSaveReqVO newPlacement = new StockInItemPlacementSaveReqVO();
        newPlacement.setLocationId(301L);
        newPlacement.setQuantity(new BigDecimal("60.00"));
        newItem.setPlacements(Collections.singletonList(newPlacement));
        req.setItems(Collections.singletonList(newItem));

        stockInService.updateStockIn(req);

        // 验证明细 insert，放置 insert 且关联生成的 205L
        verify(stockInItemMapper).insert(any(StockInItemDO.class));
        ArgumentCaptor<StockInItemPlacementDO> pCaptor = ArgumentCaptor.forClass(StockInItemPlacementDO.class);
        verify(stockInItemPlacementMapper).insert(pCaptor.capture());
        assertEquals(205L, pCaptor.getValue().getStockInItemId());
        assertEquals(new BigDecimal("60.00"), pCaptor.getValue().getQuantity());
        verify(stockInItemMapper, never()).deleteByIds(any());
        verify(stockInItemPlacementMapper, never()).deleteByIds(any());
    }

    @Test
    void updateStockIn_placementIndependentCRUD_underRetainedItem() {
        // 保留明细行 201 时，其下放置记录独立增删改：修改 301、删除 302、新增一处放置
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(warehouseLocationMapper.selectById(301L)).thenReturn(mockLocation(301L, 10L, "A-01", "A区01"));
        when(warehouseLocationMapper.selectById(302L)).thenReturn(mockLocation(302L, 10L, "A-02", "A区02"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P-1001");
        product.setName("纯棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        StockInItemDO dbItem = new StockInItemDO();
        dbItem.setId(201L);
        dbItem.setStockInId(100L);
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Collections.singletonList(dbItem));

        StockInItemPlacementDO dbP1 = new StockInItemPlacementDO();
        dbP1.setId(301L);
        dbP1.setStockInId(100L);
        dbP1.setStockInItemId(201L);
        dbP1.setLocationId(301L);
        StockInItemPlacementDO dbP2 = new StockInItemPlacementDO();
        dbP2.setId(302L);
        dbP2.setStockInId(100L);
        dbP2.setStockInItemId(201L);
        dbP2.setLocationId(302L);
        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(Arrays.asList(dbP1, dbP2));

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setId(100L);
        req.setWarehouseId(10L);

        StockInItemSaveReqVO itemReq = new StockInItemSaveReqVO();
        itemReq.setId(201L);
        itemReq.setItemType("PRODUCT");
        itemReq.setProductId(1L);
        itemReq.setQuantity(new BigDecimal("100.00"));
        itemReq.setUnit("KG");

        // 修改 301
        StockInItemPlacementSaveReqVO updateP1 = new StockInItemPlacementSaveReqVO();
        updateP1.setId(301L);
        updateP1.setLocationId(301L);
        updateP1.setQuantity(new BigDecimal("70.00"));

        // 新增放置（无 id）
        StockInItemPlacementSaveReqVO newP = new StockInItemPlacementSaveReqVO();
        newP.setLocationId(302L);
        newP.setQuantity(new BigDecimal("30.00"));

        // 302 未提交，应被删除
        itemReq.setPlacements(Arrays.asList(updateP1, newP));
        req.setItems(Collections.singletonList(itemReq));

        stockInService.updateStockIn(req);

        // 明细 201 正常 update
        ArgumentCaptor<StockInItemDO> itemUpdateCaptor2 = ArgumentCaptor.forClass(StockInItemDO.class);
        verify(stockInItemMapper).updateById(itemUpdateCaptor2.capture());
        assertEquals(201L, itemUpdateCaptor2.getValue().getId());
        verify(stockInItemMapper, never()).deleteByIds(any());

        // 放置 301 update
        ArgumentCaptor<StockInItemPlacementDO> pUpdateCaptor2 = ArgumentCaptor.forClass(StockInItemPlacementDO.class);
        verify(stockInItemPlacementMapper).updateById(pUpdateCaptor2.capture());
        assertEquals(301L, pUpdateCaptor2.getValue().getId());
        assertEquals(0, new BigDecimal("70.00").compareTo(pUpdateCaptor2.getValue().getQuantity()));

        // 放置 302 delete
        verify(stockInItemPlacementMapper).deleteByIds(Collections.singleton(302L));

        // 新增放置 insert
        ArgumentCaptor<StockInItemPlacementDO> pInsertCaptor2 = ArgumentCaptor.forClass(StockInItemPlacementDO.class);
        verify(stockInItemPlacementMapper).insert(pInsertCaptor2.capture());
        assertEquals(201L, pInsertCaptor2.getValue().getStockInItemId());
        assertEquals(0, new BigDecimal("30.00").compareTo(pInsertCaptor2.getValue().getQuantity()));
    }

    @Test
    void updateStockIn_defensive_placementPayloadReferencesDeletedItemId_throwsException() {
        // 防御性：某明细 202 被删，但提交的放置 payload 显式设置 stockInItemId = 202 -> 报校验错误
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(warehouseLocationMapper.selectById(301L)).thenReturn(mockLocation(301L, 10L, "A-01", "A区01"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P-1001");
        product.setName("纯棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        StockInItemDO dbItem1 = new StockInItemDO();
        dbItem1.setId(201L);
        dbItem1.setStockInId(100L);
        StockInItemDO dbItem2 = new StockInItemDO();
        dbItem2.setId(202L);
        dbItem2.setStockInId(100L);
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Arrays.asList(dbItem1, dbItem2));
        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(Collections.emptyList());

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setId(100L);
        req.setWarehouseId(10L);

        // 仅保留 201（202 被删），但在 201 下放置挂了 stockInItemId=202
        StockInItemSaveReqVO itemReq = new StockInItemSaveReqVO();
        itemReq.setId(201L);
        itemReq.setItemType("PRODUCT");
        itemReq.setProductId(1L);
        itemReq.setQuantity(new BigDecimal("50.00"));
        itemReq.setUnit("KG");

        StockInItemPlacementSaveReqVO badP = new StockInItemPlacementSaveReqVO();
        badP.setLocationId(301L);
        badP.setQuantity(new BigDecimal("50.00"));
        badP.setStockInItemId(202L); // 引用了被删除的明细行 202
        itemReq.setPlacements(Collections.singletonList(badP));
        req.setItems(Collections.singletonList(itemReq));

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.updateStockIn(req));
        assertTrue(ex.getMessage().contains("已删除") || ex.getMessage().contains("202"));
        verify(stockInMapper, never()).updateById(any(StockInDO.class));
    }

    @Test
    void updateStockIn_defensive_existingPlacementBelongingToDeletedItem_passedUnderAnotherItem_throwsException() {
        // 防御性：已有放置 302 在 DB 中属于 202，202 被删，但提交时把 302 挂在 201 下 -> 报校验错误
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(warehouseLocationMapper.selectById(301L)).thenReturn(mockLocation(301L, 10L, "A-01", "A区01"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        product.setCode("P-1001");
        product.setName("纯棉汗布");
        when(productMapper.selectById(1L)).thenReturn(product);

        StockInItemDO dbItem1 = new StockInItemDO();
        dbItem1.setId(201L);
        dbItem1.setStockInId(100L);
        StockInItemDO dbItem2 = new StockInItemDO();
        dbItem2.setId(202L);
        dbItem2.setStockInId(100L);
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Arrays.asList(dbItem1, dbItem2));

        StockInItemPlacementDO dbP2 = new StockInItemPlacementDO();
        dbP2.setId(302L);
        dbP2.setStockInId(100L);
        dbP2.setStockInItemId(202L); // 属于 202
        dbP2.setLocationId(301L);
        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(Collections.singletonList(dbP2));

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setId(100L);
        req.setWarehouseId(10L);

        StockInItemSaveReqVO itemReq = new StockInItemSaveReqVO();
        itemReq.setId(201L);
        itemReq.setItemType("PRODUCT");
        itemReq.setProductId(1L);
        itemReq.setQuantity(new BigDecimal("50.00"));
        itemReq.setUnit("KG");

        StockInItemPlacementSaveReqVO badP = new StockInItemPlacementSaveReqVO();
        badP.setId(302L); // 属于已被删除的 202
        badP.setLocationId(301L);
        badP.setQuantity(new BigDecimal("50.00"));
        itemReq.setPlacements(Collections.singletonList(badP));
        req.setItems(Collections.singletonList(itemReq));

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.updateStockIn(req));
        assertTrue(ex.getMessage().contains("已删除") || ex.getMessage().contains("302"));
        verify(stockInMapper, never()).updateById(any(StockInDO.class));
    }

    @Test
    void updateStockIn_crossParent_itemIdNotBelongingToStockIn_throwsException() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        when(productMapper.selectById(1L)).thenReturn(product);

        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Collections.emptyList());

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setId(100L);
        req.setWarehouseId(10L);

        StockInItemSaveReqVO foreignItem = new StockInItemSaveReqVO();
        foreignItem.setId(999L); // 不属于本单
        foreignItem.setItemType("PRODUCT");
        foreignItem.setProductId(1L);
        foreignItem.setQuantity(new BigDecimal("10.00"));
        foreignItem.setUnit("KG");
        req.setItems(Collections.singletonList(foreignItem));

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.updateStockIn(req));
        assertTrue(ex.getMessage().contains("不属于当前入库单"));
    }

    @Test
    void updateStockIn_crossParent_duplicateItemId_throwsException() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        when(productMapper.selectById(1L)).thenReturn(product);

        StockInItemDO dbItem = new StockInItemDO();
        dbItem.setId(201L);
        dbItem.setStockInId(100L);
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Collections.singletonList(dbItem));

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setId(100L);
        req.setWarehouseId(10L);

        StockInItemSaveReqVO item1 = new StockInItemSaveReqVO();
        item1.setId(201L);
        item1.setItemType("PRODUCT");
        item1.setProductId(1L);
        item1.setQuantity(new BigDecimal("10.00"));
        item1.setUnit("KG");

        StockInItemSaveReqVO item2 = new StockInItemSaveReqVO();
        item2.setId(201L);
        item2.setItemType("PRODUCT");
        item2.setProductId(1L);
        item2.setQuantity(new BigDecimal("20.00"));
        item2.setUnit("KG");

        req.setItems(Arrays.asList(item1, item2));

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.updateStockIn(req));
        assertTrue(ex.getMessage().contains("重复提交"));
    }

    @Test
    void updateStockIn_crossParent_placementIdNotBelongingToStockIn_throwsException() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(warehouseLocationMapper.selectById(301L)).thenReturn(mockLocation(301L, 10L, "A-01", "A区01"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        when(productMapper.selectById(1L)).thenReturn(product);

        StockInItemDO dbItem = new StockInItemDO();
        dbItem.setId(201L);
        dbItem.setStockInId(100L);
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Collections.singletonList(dbItem));
        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(Collections.emptyList());

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setId(100L);
        req.setWarehouseId(10L);

        StockInItemSaveReqVO itemReq = new StockInItemSaveReqVO();
        itemReq.setId(201L);
        itemReq.setItemType("PRODUCT");
        itemReq.setProductId(1L);
        itemReq.setQuantity(new BigDecimal("10.00"));
        itemReq.setUnit("KG");

        StockInItemPlacementSaveReqVO foreignP = new StockInItemPlacementSaveReqVO();
        foreignP.setId(888L); // 库内不存在
        foreignP.setLocationId(301L);
        foreignP.setQuantity(new BigDecimal("10.00"));
        itemReq.setPlacements(Collections.singletonList(foreignP));
        req.setItems(Collections.singletonList(itemReq));

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.updateStockIn(req));
        assertTrue(ex.getMessage().contains("不属于当前入库单"));
    }

    @Test
    void updateStockIn_crossParent_placementBelongsToDifferentItem_throwsException() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setStatus("DRAFT");
        when(stockInMapper.selectById(100L)).thenReturn(existing);
        when(warehouseMapper.selectById(10L)).thenReturn(mockWarehouse(10L, "一号仓", "ACTIVE"));
        when(warehouseLocationMapper.selectById(301L)).thenReturn(mockLocation(301L, 10L, "A-01", "A区01"));

        ProductDO product = new ProductDO();
        product.setId(1L);
        when(productMapper.selectById(1L)).thenReturn(product);

        StockInItemDO dbItem1 = new StockInItemDO();
        dbItem1.setId(201L);
        dbItem1.setStockInId(100L);
        StockInItemDO dbItem2 = new StockInItemDO();
        dbItem2.setId(202L);
        dbItem2.setStockInId(100L);
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Arrays.asList(dbItem1, dbItem2));

        StockInItemPlacementDO dbP2 = new StockInItemPlacementDO();
        dbP2.setId(302L);
        dbP2.setStockInId(100L);
        dbP2.setStockInItemId(202L); // 属于 202
        dbP2.setLocationId(301L);
        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(Collections.singletonList(dbP2));

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setId(100L);
        req.setWarehouseId(10L);

        // 提交时 201 挂了本属于 202 的 302
        StockInItemSaveReqVO itemReq1 = new StockInItemSaveReqVO();
        itemReq1.setId(201L);
        itemReq1.setItemType("PRODUCT");
        itemReq1.setProductId(1L);
        itemReq1.setQuantity(new BigDecimal("10.00"));
        itemReq1.setUnit("KG");

        StockInItemPlacementSaveReqVO p2 = new StockInItemPlacementSaveReqVO();
        p2.setId(302L);
        p2.setLocationId(301L);
        p2.setQuantity(new BigDecimal("10.00"));
        itemReq1.setPlacements(Collections.singletonList(p2));

        StockInItemSaveReqVO itemReq2 = new StockInItemSaveReqVO();
        itemReq2.setId(202L);
        itemReq2.setItemType("PRODUCT");
        itemReq2.setProductId(1L);
        itemReq2.setQuantity(new BigDecimal("20.00"));
        itemReq2.setUnit("KG");

        req.setItems(Arrays.asList(itemReq1, itemReq2));

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.updateStockIn(req));
        assertTrue(ex.getMessage().contains("不属于当前明细行"));
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
    void cancelDraftStockIn_success() {
        when(stockInMapper.update(isNull(), any())).thenReturn(1);
        boolean result = stockInService.cancelDraftStockIn(100L);
        assertTrue(result);
        verify(stockInMapper).update(isNull(), any());
    }

    @Test
    void cancelDraftStockIn_failedOrNotFound() {
        when(stockInMapper.update(isNull(), any())).thenReturn(0);
        boolean result = stockInService.cancelDraftStockIn(100L);
        assertFalse(result);
        assertFalse(stockInService.cancelDraftStockIn(null));
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

    // ==================== 物品类型（纱线规格 / 产品互斥 / 混合单）测试 ====================

    @Test
    void createStockIn_yarnSpec_success() {
        WarehouseDO wh = new WarehouseDO();
        wh.setId(1L);
        wh.setName("原料1号仓");
        wh.setStatus("ACTIVE");
        when(warehouseMapper.selectById(1L)).thenReturn(wh);

        YarnSpecDO yarnSpec = new YarnSpecDO();
        yarnSpec.setId(301L);
        yarnSpec.setSpecName("32S 精梳棉纱");
        when(yarnSpecMapper.selectById(301L)).thenReturn(yarnSpec);

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setInType("PURCHASE");
        req.setWarehouseId(1L);
        req.setInDate(LocalDate.of(2026, 9, 26));

        StockInItemSaveReqVO itemReq = new StockInItemSaveReqVO();
        itemReq.setItemType("YARN_SPEC");
        itemReq.setYarnSpecId(301L);
        itemReq.setQuantity(new BigDecimal("500.00"));
        itemReq.setUnit("KG");
        itemReq.setUnitPrice(new BigDecimal("22.50"));
        req.setItems(Collections.singletonList(itemReq));

        Long id = stockInService.createStockIn(req);
        assertEquals(100L, id);

        ArgumentCaptor<StockInItemDO> itemCaptor = ArgumentCaptor.forClass(StockInItemDO.class);
        verify(stockInItemMapper).insert(itemCaptor.capture());
        StockInItemDO savedItem = itemCaptor.getValue();
        assertEquals("YARN_SPEC", savedItem.getItemType());
        assertEquals(301L, savedItem.getYarnSpecId());
        assertEquals("32S 精梳棉纱", savedItem.getYarnSpecName());
        assertNull(savedItem.getProductId());
        assertNull(savedItem.getProductCode());
        assertNull(savedItem.getProductName());
        assertEquals("KG", savedItem.getUnit());
        assertEquals(new BigDecimal("11250.00"), savedItem.getAmount());
    }

    @Test
    void createStockIn_itemMutex_bothNull_throwsMutexFailed() {
        WarehouseDO wh = new WarehouseDO();
        wh.setId(1L);
        wh.setName("原料1号仓");
        wh.setStatus("ACTIVE");
        when(warehouseMapper.selectById(1L)).thenReturn(wh);

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setInType("PURCHASE");
        req.setWarehouseId(1L);
        req.setInDate(LocalDate.now());

        StockInItemSaveReqVO itemReq = new StockInItemSaveReqVO();
        itemReq.setItemType("PRODUCT");
        itemReq.setQuantity(new BigDecimal("100.00"));
        itemReq.setUnit("KG");
        req.setItems(Collections.singletonList(itemReq));

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockIn(req));
        assertEquals(STOCK_IN_ITEM_MUTEX_FAILED.getCode(), ex.getCode());
    }

    @Test
    void createStockIn_itemMutex_bothProvided_throwsMutexFailed() {
        WarehouseDO wh = new WarehouseDO();
        wh.setId(1L);
        wh.setName("原料1号仓");
        wh.setStatus("ACTIVE");
        when(warehouseMapper.selectById(1L)).thenReturn(wh);

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setInType("PURCHASE");
        req.setWarehouseId(1L);
        req.setInDate(LocalDate.now());

        StockInItemSaveReqVO itemReq = new StockInItemSaveReqVO();
        itemReq.setItemType("PRODUCT");
        itemReq.setProductId(10L);
        itemReq.setYarnSpecId(301L);
        itemReq.setQuantity(new BigDecimal("100.00"));
        itemReq.setUnit("KG");
        req.setItems(Collections.singletonList(itemReq));

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockIn(req));
        assertEquals(STOCK_IN_ITEM_MUTEX_FAILED.getCode(), ex.getCode());
    }

    @Test
    void createStockIn_yarnSpec_unitNotKG_throwsYarnUnitInvalid() {
        WarehouseDO wh = new WarehouseDO();
        wh.setId(1L);
        wh.setName("原料1号仓");
        wh.setStatus("ACTIVE");
        when(warehouseMapper.selectById(1L)).thenReturn(wh);

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setInType("PURCHASE");
        req.setWarehouseId(1L);
        req.setInDate(LocalDate.now());

        StockInItemSaveReqVO itemReq = new StockInItemSaveReqVO();
        itemReq.setItemType("YARN_SPEC");
        itemReq.setYarnSpecId(301L);
        itemReq.setQuantity(new BigDecimal("100.00"));
        itemReq.setUnit("M"); // 纱线规格必须为 KG
        req.setItems(Collections.singletonList(itemReq));

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockIn(req));
        assertEquals(STOCK_IN_ITEM_YARN_UNIT_INVALID.getCode(), ex.getCode());
    }

    @Test
    void createStockIn_invalidItemType_throwsItemTypeInvalid() {
        WarehouseDO wh = new WarehouseDO();
        wh.setId(1L);
        wh.setName("原料1号仓");
        wh.setStatus("ACTIVE");
        when(warehouseMapper.selectById(1L)).thenReturn(wh);

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setInType("PURCHASE");
        req.setWarehouseId(1L);
        req.setInDate(LocalDate.now());

        StockInItemSaveReqVO itemReq = new StockInItemSaveReqVO();
        itemReq.setItemType("UNKNOWN_TYPE");
        itemReq.setQuantity(new BigDecimal("100.00"));
        itemReq.setUnit("KG");
        req.setItems(Collections.singletonList(itemReq));

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockIn(req));
        assertEquals(STOCK_IN_ITEM_TYPE_INVALID.getCode(), ex.getCode());
    }

    @Test
    void auditStockIn_yarnSpecPosting_success() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setInNo("IN-20260926-001");
        existing.setStatus("DRAFT");
        existing.setWarehouseId(1L);
        existing.setWarehouseName("原料1号仓");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        StockInItemDO item = new StockInItemDO();
        item.setId(201L);
        item.setStockInId(100L);
        item.setItemType("YARN_SPEC");
        item.setYarnSpecId(301L);
        item.setYarnSpecName("32S 精梳棉纱");
        item.setQuantity(new BigDecimal("350.00"));
        item.setUnit("KG");
        item.setUnitPrice(new BigDecimal("20.00"));
        item.setAmount(new BigDecimal("7000.00"));
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(Collections.singletonList(item));
        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(Collections.emptyList());

        stockInService.auditStockIn(100L);

        ArgumentCaptor<StockLedgerDO> ledgerCaptor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper).insert(ledgerCaptor.capture());
        StockLedgerDO ledger = ledgerCaptor.getValue();
        assertEquals("YARN_SPEC", ledger.getItemType());
        assertEquals(301L, ledger.getYarnSpecId());
        assertEquals("32S 精梳棉纱", ledger.getYarnSpecName());
        assertNull(ledger.getProductId());
        assertEquals("STOCK_IN", ledger.getTxnType());
        assertEquals(new BigDecimal("350.00"), ledger.getQuantity());
        assertEquals("KG", ledger.getUnit());
        assertEquals(new BigDecimal("7000.00"), ledger.getAmount());
    }

    @Test
    void cancelStockIn_yarnSpecReversal_success() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setInNo("IN-20260926-001");
        existing.setStatus("COMPLETED");
        existing.setWarehouseId(1L);
        existing.setWarehouseName("原料1号仓");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        StockLedgerDO orig = new StockLedgerDO();
        orig.setId(501L);
        orig.setItemType("YARN_SPEC");
        orig.setYarnSpecId(301L);
        orig.setYarnSpecName("32S 精梳棉纱");
        orig.setWarehouseId(1L);
        orig.setWarehouseName("原料1号仓");
        orig.setTxnType("STOCK_IN");
        orig.setQuantity(new BigDecimal("350.00"));
        orig.setUnit("KG");
        orig.setUnitPrice(new BigDecimal("20.00"));
        orig.setAmount(new BigDecimal("7000.00"));
        orig.setRelatedDocType("STOCK_IN");
        orig.setRelatedDocId(100L);
        orig.setRelatedDocNo("IN-20260926-001");
        when(stockLedgerMapper.selectListByRelatedDoc("STOCK_IN", 100L, "STOCK_IN"))
                .thenReturn(Collections.singletonList(orig));

        stockInService.cancelStockIn(100L);

        ArgumentCaptor<StockLedgerDO> ledgerCaptor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper).insert(ledgerCaptor.capture());
        StockLedgerDO reversal = ledgerCaptor.getValue();
        assertEquals("YARN_SPEC", reversal.getItemType());
        assertEquals(301L, reversal.getYarnSpecId());
        assertEquals("32S 精梳棉纱", reversal.getYarnSpecName());
        assertNull(reversal.getProductId());
        assertEquals("STOCK_IN_REVERSAL", reversal.getTxnType());
        assertEquals(new BigDecimal("-350.00"), reversal.getQuantity());
        assertEquals("KG", reversal.getUnit());
        assertEquals(new BigDecimal("-7000.00"), reversal.getAmount());
    }

    @Test
    void mixedItems_saveAuditAndReversal_success() {
        // 1. 创建混合单（1条产品明细 + 1条纱线明细）
        WarehouseDO wh = new WarehouseDO();
        wh.setId(1L);
        wh.setName("综合仓");
        wh.setStatus("ACTIVE");
        when(warehouseMapper.selectById(1L)).thenReturn(wh);

        ProductDO prod = new ProductDO();
        prod.setId(10L);
        prod.setCode("P001");
        prod.setName("纯棉汗布");
        when(productMapper.selectById(10L)).thenReturn(prod);

        YarnSpecDO yarnSpec = new YarnSpecDO();
        yarnSpec.setId(301L);
        yarnSpec.setSpecName("40S 涤棉纱");
        when(yarnSpecMapper.selectById(301L)).thenReturn(yarnSpec);

        StockInSaveReqVO req = new StockInSaveReqVO();
        req.setInType("PURCHASE");
        req.setWarehouseId(1L);
        req.setInDate(LocalDate.of(2026, 9, 26));

        StockInItemSaveReqVO item1 = new StockInItemSaveReqVO();
        item1.setItemType("PRODUCT");
        item1.setProductId(10L);
        item1.setQuantity(new BigDecimal("100.00"));
        item1.setUnit("M");
        item1.setUnitPrice(new BigDecimal("15.00"));

        StockInItemSaveReqVO item2 = new StockInItemSaveReqVO();
        item2.setItemType("YARN_SPEC");
        item2.setYarnSpecId(301L);
        item2.setQuantity(new BigDecimal("200.00"));
        item2.setUnit("KG");
        item2.setUnitPrice(new BigDecimal("25.00"));

        List<StockInItemSaveReqVO> items = new ArrayList<>();
        items.add(item1);
        items.add(item2);
        req.setItems(items);

        Long id = stockInService.createStockIn(req);
        assertEquals(100L, id);

        ArgumentCaptor<StockInItemDO> itemCaptor = ArgumentCaptor.forClass(StockInItemDO.class);
        verify(stockInItemMapper, times(2)).insert(itemCaptor.capture());
        List<StockInItemDO> savedItems = itemCaptor.getAllValues();
        assertEquals(2, savedItems.size());
        assertEquals("PRODUCT", savedItems.get(0).getItemType());
        assertEquals(10L, savedItems.get(0).getProductId());
        assertEquals("YARN_SPEC", savedItems.get(1).getItemType());
        assertEquals(301L, savedItems.get(1).getYarnSpecId());

        // 2. 审核混合单过账
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setInNo("IN-20260926-MIX");
        existing.setStatus("DRAFT");
        existing.setWarehouseId(1L);
        existing.setWarehouseName("综合仓");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        savedItems.get(0).setId(201L);
        savedItems.get(1).setId(202L);
        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(savedItems);
        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(Collections.emptyList());

        stockInService.auditStockIn(100L);

        ArgumentCaptor<StockLedgerDO> ledgerCaptor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper, times(2)).insert(ledgerCaptor.capture());
        List<StockLedgerDO> ledgers = ledgerCaptor.getAllValues();
        assertEquals(2, ledgers.size());
        assertEquals("PRODUCT", ledgers.get(0).getItemType());
        assertEquals(10L, ledgers.get(0).getProductId());
        assertEquals("YARN_SPEC", ledgers.get(1).getItemType());
        assertEquals(301L, ledgers.get(1).getYarnSpecId());

        // 3. 红冲混合单
        existing.setStatus("COMPLETED");
        ledgers.get(0).setId(501L);
        ledgers.get(0).setRelatedDocType("STOCK_IN");
        ledgers.get(0).setRelatedDocId(100L);
        ledgers.get(0).setRelatedDocNo("IN-20260926-MIX");
        ledgers.get(1).setId(502L);
        ledgers.get(1).setRelatedDocType("STOCK_IN");
        ledgers.get(1).setRelatedDocId(100L);
        ledgers.get(1).setRelatedDocNo("IN-20260926-MIX");
        when(stockLedgerMapper.selectListByRelatedDoc("STOCK_IN", 100L, "STOCK_IN")).thenReturn(ledgers);

        stockInService.cancelStockIn(100L);

        ArgumentCaptor<StockLedgerDO> reversalCaptor = ArgumentCaptor.forClass(StockLedgerDO.class);
        verify(stockLedgerMapper, times(4)).insert(reversalCaptor.capture());
        List<StockLedgerDO> allInserts = reversalCaptor.getAllValues();
        StockLedgerDO rev1 = allInserts.get(2);
        StockLedgerDO rev2 = allInserts.get(3);
        assertEquals("PRODUCT", rev1.getItemType());
        assertEquals(new BigDecimal("-100.00"), rev1.getQuantity());
        assertEquals("YARN_SPEC", rev2.getItemType());
        assertEquals(new BigDecimal("-200.00"), rev2.getQuantity());
    }

    // ==================== Issue 11: 审核过账回写布票 与 检验单生成入库单 ====================

    @Test
    void auditStockIn_withWeavingTickets_marksStocked() {
        StockInDO existing = new StockInDO();
        existing.setId(100L);
        existing.setInNo("IN-20260927-001");
        existing.setStatus("DRAFT");
        existing.setWarehouseId(10L);
        existing.setWarehouseName("成品仓");
        when(stockInMapper.selectById(100L)).thenReturn(existing);

        StockInItemDO item1 = new StockInItemDO();
        item1.setId(201L);
        item1.setStockInId(100L);
        item1.setProductId(1001L);
        item1.setWeavingTicketId(501L);
        item1.setQuantity(new BigDecimal("25.00"));
        item1.setUnit("KG");

        StockInItemDO item2 = new StockInItemDO();
        item2.setId(202L);
        item2.setStockInId(100L);
        item2.setProductId(1001L);
        item2.setPieceNo("TICK-002");
        item2.setQuantity(new BigDecimal("26.00"));
        item2.setUnit("KG");

        WeavingTicketDO ticket2 = new WeavingTicketDO();
        ticket2.setId(502L);
        ticket2.setCode("TICK-002");
        when(weavingTicketMapper.selectByCode("TICK-002")).thenReturn(ticket2);

        when(stockInItemMapper.selectListByStockInId(100L)).thenReturn(List.of(item1, item2));
        when(stockInItemPlacementMapper.selectListByStockInId(100L)).thenReturn(Collections.emptyList());

        stockInService.auditStockIn(100L);

        verify(weavingTicketService).markStocked(argThat(ids -> ids.contains(501L) && ids.contains(502L) && ids.size() == 2));
    }

    @Test
    void createStockInFromInspection_inspectionOrderNotFound_throwsException() {
        InspectionOrderGenerateStockInReqVO reqVO = new InspectionOrderGenerateStockInReqVO();
        reqVO.setInspectionOrderId(999L);
        reqVO.setWarehouseId(1L);

        when(inspectionOrderMapper.selectById(999L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockInFromInspection(reqVO));
        assertEquals(INSPECTION_ORDER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    void createStockInFromInspection_notCompleted_throwsException() {
        InspectionOrderGenerateStockInReqVO reqVO = new InspectionOrderGenerateStockInReqVO();
        reqVO.setInspectionOrderId(101L);
        reqVO.setWarehouseId(1L);

        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(101L);
        order.setStatus("ACTIVE");
        when(inspectionOrderMapper.selectById(101L)).thenReturn(order);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockInFromInspection(reqVO));
        assertEquals(INSPECTION_ORDER_STATUS_NOT_COMPLETED.getCode(), ex.getCode());
    }

    @Test
    void createStockInFromInspection_warehouseNotFoundOrNotActive_throwsException() {
        InspectionOrderGenerateStockInReqVO reqVO = new InspectionOrderGenerateStockInReqVO();
        reqVO.setInspectionOrderId(101L);
        reqVO.setWarehouseId(1L);

        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(101L);
        order.setStatus("COMPLETED");
        when(inspectionOrderMapper.selectById(101L)).thenReturn(order);

        when(warehouseMapper.selectById(1L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockInFromInspection(reqVO));
        assertEquals(STOCK_IN_REFERENCE_NOT_FOUND.getCode(), ex.getCode());

        WarehouseDO disabledWh = new WarehouseDO();
        disabledWh.setId(1L);
        disabledWh.setStatus("DISABLED");
        when(warehouseMapper.selectById(1L)).thenReturn(disabledWh);

        ServiceException ex2 = assertThrows(ServiceException.class, () -> stockInService.createStockInFromInspection(reqVO));
        assertEquals(STOCK_IN_REFERENCE_NOT_FOUND.getCode(), ex2.getCode());
    }

    @Test
    void createStockInFromInspection_noItemsOrAllDefective_throwsException() {
        InspectionOrderGenerateStockInReqVO reqVO = new InspectionOrderGenerateStockInReqVO();
        reqVO.setInspectionOrderId(101L);
        reqVO.setWarehouseId(1L);

        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(101L);
        order.setStatus("COMPLETED");
        when(inspectionOrderMapper.selectById(101L)).thenReturn(order);

        WarehouseDO wh = new WarehouseDO();
        wh.setId(1L);
        wh.setName("原料仓");
        wh.setStatus("ACTIVE");
        when(warehouseMapper.selectById(1L)).thenReturn(wh);

        // 1. 无明细
        when(inspectionOrderItemMapper.selectListByInspectionOrderId(101L)).thenReturn(Collections.emptyList());
        ServiceException ex1 = assertThrows(ServiceException.class, () -> stockInService.createStockInFromInspection(reqVO));
        assertEquals(INSPECTION_ORDER_NO_QUALIFIED_ITEMS.getCode(), ex1.getCode());

        // 2. 全部是 DEFECTIVE
        InspectionOrderItemDO defectiveItem = new InspectionOrderItemDO();
        defectiveItem.setId(201L);
        defectiveItem.setGrade("DEFECTIVE");
        when(inspectionOrderItemMapper.selectListByInspectionOrderId(101L)).thenReturn(List.of(defectiveItem));
        ServiceException ex2 = assertThrows(ServiceException.class, () -> stockInService.createStockInFromInspection(reqVO));
        assertEquals(INSPECTION_ORDER_NO_QUALIFIED_ITEMS.getCode(), ex2.getCode());
    }

    @Test
    void createStockInFromInspection_allAlreadyGenerated_throwsException() {
        InspectionOrderGenerateStockInReqVO reqVO = new InspectionOrderGenerateStockInReqVO();
        reqVO.setInspectionOrderId(101L);
        reqVO.setWarehouseId(1L);

        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(101L);
        order.setStatus("COMPLETED");
        when(inspectionOrderMapper.selectById(101L)).thenReturn(order);

        WarehouseDO wh = new WarehouseDO();
        wh.setId(1L);
        wh.setName("原料仓");
        wh.setStatus("ACTIVE");
        when(warehouseMapper.selectById(1L)).thenReturn(wh);

        InspectionOrderItemDO item = new InspectionOrderItemDO();
        item.setId(201L);
        item.setGrade("A");
        when(inspectionOrderItemMapper.selectListByInspectionOrderId(101L)).thenReturn(List.of(item));

        StockInItemDO existingStockInItem = new StockInItemDO();
        existingStockInItem.setId(301L);
        existingStockInItem.setStockInId(888L);
        existingStockInItem.setInspectionOrderItemId(201L);
        when(stockInItemMapper.selectListByInspectionOrderItemIds(anyCollection())).thenReturn(List.of(existingStockInItem));

        StockInDO parentStockIn = new StockInDO();
        parentStockIn.setId(888L);
        parentStockIn.setStatus("DRAFT");
        when(stockInMapper.selectById(888L)).thenReturn(parentStockIn);

        ServiceException ex = assertThrows(ServiceException.class, () -> stockInService.createStockInFromInspection(reqVO));
        assertEquals(INSPECTION_ORDER_NO_QUALIFIED_ITEMS.getCode(), ex.getCode());
    }

    @Test
    void createStockInFromInspection_success_filtersDefectiveAndSkipsGenerated() {
        InspectionOrderGenerateStockInReqVO reqVO = new InspectionOrderGenerateStockInReqVO();
        reqVO.setInspectionOrderId(101L);
        reqVO.setWarehouseId(1L);
        reqVO.setRemark("胚布入库");

        InspectionOrderDO order = new InspectionOrderDO();
        order.setId(101L);
        order.setCode("INS-20260927-001");
        order.setStatus("COMPLETED");
        order.setProductId(1001L);
        order.setProductCode("P001");
        order.setProductName("全棉汗布");
        order.setGreigeOrderId(501L);
        order.setSalesContractId(601L);
        order.setContractNo("SC-001");
        order.setInspectionFactoryId(701L);
        order.setInspectionFactoryName("盛泽染整一厂");
        when(inspectionOrderMapper.selectById(101L)).thenReturn(order);

        GreigeOrderDO greigeOrder = new GreigeOrderDO();
        greigeOrder.setId(501L);
        greigeOrder.setOrderNo("GO-2026-001");
        when(greigeOrderMapper.selectById(501L)).thenReturn(greigeOrder);

        WarehouseDO wh = new WarehouseDO();
        wh.setId(1L);
        wh.setName("胚布仓");
        wh.setStatus("ACTIVE");
        when(warehouseMapper.selectById(1L)).thenReturn(wh);

        // 4 items:
        // Item 1: Grade A (qualified)
        InspectionOrderItemDO item1 = new InspectionOrderItemDO();
        item1.setId(201L);
        item1.setGrade("A");
        item1.setPieceNo("TICK-001");
        item1.setWeavingTicketId(901L);
        item1.setMeasuredWeight(new BigDecimal("25.50"));
        item1.setVatNo("VAT-01");
        item1.setRollNo("R-01");

        // Item 2: Grade DEFECTIVE (should be skipped)
        InspectionOrderItemDO item2 = new InspectionOrderItemDO();
        item2.setId(202L);
        item2.setGrade("DEFECTIVE");
        item2.setPieceNo("TICK-002");

        // Item 3: Grade B, but already generated in an ACTIVE (DRAFT/COMPLETED) stock-in (should be skipped)
        InspectionOrderItemDO item3 = new InspectionOrderItemDO();
        item3.setId(203L);
        item3.setGrade("B");
        item3.setPieceNo("TICK-003");

        // Item 4: Grade B, already in a CANCELLED stock-in (should NOT be skipped)
        InspectionOrderItemDO item4 = new InspectionOrderItemDO();
        item4.setId(204L);
        item4.setGrade("B");
        item4.setPieceNo("TICK-004");
        item4.setWeavingTicketId(904L);
        item4.setLengthM(new BigDecimal("100.00"));
        item4.setVatNo("VAT-02");

        when(inspectionOrderItemMapper.selectListByInspectionOrderId(101L))
                .thenReturn(List.of(item1, item2, item3, item4));

        StockInItemDO generatedFor3 = new StockInItemDO();
        generatedFor3.setId(303L);
        generatedFor3.setStockInId(801L);
        generatedFor3.setInspectionOrderItemId(203L);

        StockInItemDO generatedFor4 = new StockInItemDO();
        generatedFor4.setId(304L);
        generatedFor4.setStockInId(802L);
        generatedFor4.setInspectionOrderItemId(204L);

        when(stockInItemMapper.selectListByInspectionOrderItemIds(anyCollection()))
                .thenReturn(List.of(generatedFor3, generatedFor4));

        StockInDO parent3 = new StockInDO();
        parent3.setId(801L);
        parent3.setStatus("COMPLETED");
        when(stockInMapper.selectById(801L)).thenReturn(parent3);

        StockInDO parent4 = new StockInDO();
        parent4.setId(802L);
        parent4.setStatus("CANCELLED"); // cancelled, so item 4 is re-eligible
        when(stockInMapper.selectById(802L)).thenReturn(parent4);

        InspectionOrderGenerateStockInRespVO respVO = stockInService.createStockInFromInspection(reqVO);

        assertNotNull(respVO);
        assertEquals(100L, respVO.getStockInId());
        assertEquals(2, respVO.getGeneratedCount());
        assertEquals(2, respVO.getSkippedCount()); // item 2 (defective) and item 3 (already in active stock-in)

        // Verify StockInDO
        ArgumentCaptor<StockInDO> stockInCaptor = ArgumentCaptor.forClass(StockInDO.class);
        verify(stockInMapper).insert(stockInCaptor.capture());
        StockInDO savedStockIn = stockInCaptor.getValue();
        assertEquals("PRODUCTION", savedStockIn.getInType());
        assertEquals("DRAFT", savedStockIn.getStatus());
        assertEquals(1L, savedStockIn.getWarehouseId());
        assertEquals("胚布仓", savedStockIn.getWarehouseName());
        assertEquals(101L, savedStockIn.getInspectionOrderId());
        assertEquals("INS-20260927-001", savedStockIn.getInspectionOrderNo());
        assertEquals(501L, savedStockIn.getGreigeOrderId());
        assertEquals("GO-2026-001", savedStockIn.getGreigeOrderNo());
        assertEquals(601L, savedStockIn.getSalesContractId());
        assertEquals("SC-001", savedStockIn.getContractNo());
        assertEquals(701L, savedStockIn.getSupplierId());
        assertEquals("盛泽染整一厂", savedStockIn.getSupplierName());
        assertEquals("胚布入库", savedStockIn.getRemark());

        // Verify StockInItemDO (2 items inserted: item 1 and item 4)
        ArgumentCaptor<StockInItemDO> itemCaptor = ArgumentCaptor.forClass(StockInItemDO.class);
        verify(stockInItemMapper, times(2)).insert(itemCaptor.capture());
        List<StockInItemDO> savedItems = itemCaptor.getAllValues();

        StockInItemDO si1 = savedItems.get(0);
        assertEquals("PRODUCT", si1.getItemType());
        assertEquals(1001L, si1.getProductId());
        assertEquals("P001", si1.getProductCode());
        assertEquals("全棉汗布", si1.getProductName());
        assertEquals("VAT-01", si1.getBatchNo());
        assertEquals("R-01", si1.getRollNo());
        assertEquals("TICK-001", si1.getPieceNo());
        assertEquals(901L, si1.getWeavingTicketId());
        assertEquals(201L, si1.getInspectionOrderItemId());
        assertEquals(new BigDecimal("25.50"), si1.getQuantity());
        assertEquals("KG", si1.getUnit());

        StockInItemDO si2 = savedItems.get(1);
        assertEquals("TICK-004", si2.getPieceNo());
        assertEquals(904L, si2.getWeavingTicketId());
        assertEquals(204L, si2.getInspectionOrderItemId());
        assertEquals(new BigDecimal("100.00"), si2.getQuantity());
        assertEquals("M", si2.getUnit());
    }

}

