package com.luna.lunasrv.module.lunaerp.service.stock.stockin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.yarnspec.YarnSpecDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.stockin.StockInItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.yarnspec.YarnSpecMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInItemPlacementMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.stockin.StockInMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link StockInRecycleBinHandler} 单元测试
 *
 * @author luna
 */
public class StockInRecycleBinHandlerTest {

    @Mock
    private StockInMapper stockInMapper;

    @Mock
    private StockInItemMapper stockInItemMapper;

    @Mock
    private StockInItemPlacementMapper stockInItemPlacementMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private YarnSpecMapper yarnSpecMapper;

    @InjectMocks
    private StockInRecycleBinHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long STOCK_IN_ID = 6001L;
    private static final Long SUPPLIER_ID = 801L;
    private static final Long PRODUCT_ID = 802L;
    private static final Long YARN_SPEC_ID = 803L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        TenantContextHolder.setTenantId(TENANT_ID_1);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    @DisplayName("getBizType 返回 STOCK_IN")
    void testGetBizType() {
        assertEquals("STOCK_IN", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询草稿入库单：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("STOCK_IN");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("STOCK_IN");
            item.setBizId(STOCK_IN_ID);
            item.setDisplayName("IN-20261001-001");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(stockInMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("IN-20261001-001", result.getList().get(0).getDisplayName());
        verify(stockInMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));
    }

    @Test
    @DisplayName("恢复校验：草稿存在且供应商/产品/规格正常")
    void testValidateRestore_success() {
        StockInDO stockIn = new StockInDO();
        stockIn.setId(STOCK_IN_ID);
        stockIn.setSupplierId(SUPPLIER_ID);

        StockInItemDO item1 = new StockInItemDO();
        item1.setStockInId(STOCK_IN_ID);
        item1.setProductId(PRODUCT_ID);

        StockInItemDO item2 = new StockInItemDO();
        item2.setStockInId(STOCK_IN_ID);
        item2.setYarnSpecId(YARN_SPEC_ID);

        SupplierDO supplier = new SupplierDO();
        supplier.setId(SUPPLIER_ID);
        supplier.setCompanyName("原料供应商");

        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("双纱汗布");

        YarnSpecDO spec = new YarnSpecDO();
        spec.setId(YARN_SPEC_ID);
        spec.setSpecName("40S 棉纱");

        when(stockInMapper.selectDeletedById(STOCK_IN_ID, TENANT_ID_1)).thenReturn(stockIn);
        when(stockInItemMapper.selectDeletedListByStockInId(STOCK_IN_ID, TENANT_ID_1)).thenReturn(List.of(item1, item2));

        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(null);
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(supplier);

        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

        when(yarnSpecMapper.selectDeletedById(YARN_SPEC_ID, TENANT_ID_1)).thenReturn(null);
        when(yarnSpecMapper.selectById(YARN_SPEC_ID)).thenReturn(spec);

        assertDoesNotThrow(() -> handler.validateRestore(STOCK_IN_ID));
    }

    @Test
    @DisplayName("恢复校验：记录不存在或非草稿抛 1_030_031_001")
    void testValidateRestore_notExists_throws1030031001() {
        when(stockInMapper.selectDeletedById(STOCK_IN_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(STOCK_IN_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：供应商在回收站抛 1_030_031_002")
    void testValidateRestore_supplierInRecycle_throws1030031002() {
        StockInDO stockIn = new StockInDO();
        stockIn.setId(STOCK_IN_ID);
        stockIn.setSupplierId(SUPPLIER_ID);

        SupplierDO deletedSupplier = new SupplierDO();
        deletedSupplier.setId(SUPPLIER_ID);
        deletedSupplier.setCompanyName("已删除原料商");

        when(stockInMapper.selectDeletedById(STOCK_IN_ID, TENANT_ID_1)).thenReturn(stockIn);
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(deletedSupplier);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(STOCK_IN_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除原料商"));
    }

    @Test
    @DisplayName("恢复校验：明细产品在回收站抛 1_030_031_002")
    void testValidateRestore_productInRecycle_throws1030031002() {
        StockInDO stockIn = new StockInDO();
        stockIn.setId(STOCK_IN_ID);

        StockInItemDO item = new StockInItemDO();
        item.setProductId(PRODUCT_ID);

        ProductDO deletedProduct = new ProductDO();
        deletedProduct.setId(PRODUCT_ID);
        deletedProduct.setName("已删除布匹");

        when(stockInMapper.selectDeletedById(STOCK_IN_ID, TENANT_ID_1)).thenReturn(stockIn);
        when(stockInItemMapper.selectDeletedListByStockInId(STOCK_IN_ID, TENANT_ID_1)).thenReturn(List.of(item));
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(deletedProduct);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(STOCK_IN_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除布匹"));
    }

    @Test
    @DisplayName("恢复成功：级联复活明细与库位放置记录")
    void testRestore_success_withCascade() {
        StockInDO stockIn = new StockInDO();
        stockIn.setId(STOCK_IN_ID);

        when(stockInMapper.selectDeletedById(STOCK_IN_ID, TENANT_ID_1)).thenReturn(stockIn);
        when(stockInItemMapper.selectDeletedListByStockInId(STOCK_IN_ID, TENANT_ID_1)).thenReturn(Collections.emptyList());
        when(stockInMapper.restoreStockIn(eq(STOCK_IN_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);
        when(stockInItemMapper.restoreByStockInId(eq(STOCK_IN_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(2);
        when(stockInItemPlacementMapper.restoreByStockInId(eq(STOCK_IN_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(4);

        assertDoesNotThrow(() -> handler.restore(STOCK_IN_ID));

        verify(stockInMapper).restoreStockIn(eq(STOCK_IN_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(stockInItemMapper).restoreByStockInId(eq(STOCK_IN_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(stockInItemPlacementMapper).restoreByStockInId(eq(STOCK_IN_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("彻底删除成功：先删放置记录与明细，再删入库主单")
    void testPurge_success_withCascade() {
        StockInDO stockIn = new StockInDO();
        stockIn.setId(STOCK_IN_ID);

        when(stockInMapper.selectDeletedById(STOCK_IN_ID, TENANT_ID_1)).thenReturn(stockIn);
        when(stockInMapper.selectActiveReferenceCount(STOCK_IN_ID, TENANT_ID_1)).thenReturn(0L);
        when(stockInItemPlacementMapper.purgeByStockInId(STOCK_IN_ID, TENANT_ID_1)).thenReturn(4);
        when(stockInItemMapper.purgeByStockInId(STOCK_IN_ID, TENANT_ID_1)).thenReturn(2);
        when(stockInMapper.purgeStockIn(STOCK_IN_ID, TENANT_ID_1)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(STOCK_IN_ID));

        verify(stockInItemPlacementMapper).purgeByStockInId(STOCK_IN_ID, TENANT_ID_1);
        verify(stockInItemMapper).purgeByStockInId(STOCK_IN_ID, TENANT_ID_1);
        verify(stockInMapper).purgeStockIn(STOCK_IN_ID, TENANT_ID_1);
    }

}
