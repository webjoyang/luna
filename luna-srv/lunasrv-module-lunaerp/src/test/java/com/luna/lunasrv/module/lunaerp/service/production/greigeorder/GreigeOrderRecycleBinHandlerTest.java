package com.luna.lunasrv.module.lunaerp.service.production.greigeorder;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.greigeorder.GreigeOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.GreigeOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.greigeorder.color.GreigeOrderColorMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;

import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_HAS_ACTIVE_REFERENCE;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_RECORD_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.CommonErrorCodeConstants.RECYCLE_BIN_REFERENCE_IN_RECYCLE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link GreigeOrderRecycleBinHandler} 单元测试
 *
 * @author luna
 */
public class GreigeOrderRecycleBinHandlerTest {

    @Mock
    private GreigeOrderMapper greigeOrderMapper;

    @Mock
    private GreigeOrderColorMapper greigeOrderColorMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @InjectMocks
    private GreigeOrderRecycleBinHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long ORDER_ID = 3001L;
    private static final Long PRODUCT_ID = 200L;
    private static final Long SUPPLIER_ID = 300L;

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
    @DisplayName("getBizType 返回 GREIGE_ORDER")
    void testGetBizType() {
        assertEquals("GREIGE_ORDER", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询草稿胚布单：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("GREIGE_ORDER");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("GREIGE_ORDER");
            item.setBizId(ORDER_ID);
            item.setDisplayName("PB1001001");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(greigeOrderMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("PB1001001", result.getList().get(0).getDisplayName());
        verify(greigeOrderMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));
    }

    @Test
    @DisplayName("恢复校验：草稿存在且产品与供应商正常")
    void testValidateRestore_success() {
        GreigeOrderDO order = new GreigeOrderDO();
        order.setId(ORDER_ID);
        order.setProductId(PRODUCT_ID);
        order.setGreigeFactoryId(SUPPLIER_ID);

        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("高支棉布");

        SupplierDO supplier = new SupplierDO();
        supplier.setId(SUPPLIER_ID);
        supplier.setCompanyName("坯布织厂");

        when(greigeOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(null);
        when(supplierMapper.selectById(SUPPLIER_ID)).thenReturn(supplier);

        assertDoesNotThrow(() -> handler.validateRestore(ORDER_ID));
    }

    @Test
    @DisplayName("恢复校验：记录不存在或非草稿抛 1_030_031_001")
    void testValidateRestore_notExists_throws1030031001() {
        when(greigeOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：产品在回收站抛 1_030_031_002")
    void testValidateRestore_productInRecycle_throws1030031002() {
        GreigeOrderDO order = new GreigeOrderDO();
        order.setId(ORDER_ID);
        order.setProductId(PRODUCT_ID);

        ProductDO deletedProduct = new ProductDO();
        deletedProduct.setId(PRODUCT_ID);
        deletedProduct.setName("已删除产品");

        when(greigeOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(deletedProduct);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除产品"));
    }

    @Test
    @DisplayName("恢复校验：织厂在回收站抛 1_030_031_002")
    void testValidateRestore_greigeFactoryInRecycle_throws1030031002() {
        GreigeOrderDO order = new GreigeOrderDO();
        order.setId(ORDER_ID);
        order.setGreigeFactoryId(SUPPLIER_ID);

        SupplierDO deletedSupplier = new SupplierDO();
        deletedSupplier.setId(SUPPLIER_ID);
        deletedSupplier.setCompanyName("已删除织厂");

        when(greigeOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(supplierMapper.selectDeletedById(SUPPLIER_ID, TENANT_ID_1)).thenReturn(deletedSupplier);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除织厂"));
    }

    @Test
    @DisplayName("恢复成功：级联复活颜色明细")
    void testRestore_success_withCascade() {
        GreigeOrderDO order = new GreigeOrderDO();
        order.setId(ORDER_ID);

        when(greigeOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(greigeOrderMapper.restoreGreigeOrder(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);
        when(greigeOrderColorMapper.restoreByOrderId(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(2);

        assertDoesNotThrow(() -> handler.restore(ORDER_ID));

        verify(greigeOrderMapper).restoreGreigeOrder(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(greigeOrderColorMapper).restoreByOrderId(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("彻底删除引用守卫：存在活跃引用抛 1_030_031_003")
    void testPurge_hasActiveReference_throws1030031003() {
        GreigeOrderDO order = new GreigeOrderDO();
        order.setId(ORDER_ID);

        when(greigeOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(greigeOrderMapper.selectActiveReferenceCount(ORDER_ID, TENANT_ID_1)).thenReturn(2L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(ORDER_ID));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());

        verify(greigeOrderColorMapper, never()).purgeByOrderId(any(), any());
        verify(greigeOrderMapper, never()).purgeGreigeOrder(any(), any());
    }

    @Test
    @DisplayName("彻底删除成功：先删颜色明细，再删胚布主单")
    void testPurge_success_withCascade() {
        GreigeOrderDO order = new GreigeOrderDO();
        order.setId(ORDER_ID);

        when(greigeOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(greigeOrderMapper.selectActiveReferenceCount(ORDER_ID, TENANT_ID_1)).thenReturn(0L);
        when(greigeOrderColorMapper.purgeByOrderId(ORDER_ID, TENANT_ID_1)).thenReturn(2);
        when(greigeOrderMapper.purgeGreigeOrder(ORDER_ID, TENANT_ID_1)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(ORDER_ID));

        verify(greigeOrderColorMapper).purgeByOrderId(ORDER_ID, TENANT_ID_1);
        verify(greigeOrderMapper).purgeGreigeOrder(ORDER_ID, TENANT_ID_1);
    }

}
