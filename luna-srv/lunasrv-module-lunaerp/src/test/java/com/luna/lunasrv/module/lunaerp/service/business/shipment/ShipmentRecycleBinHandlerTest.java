package com.luna.lunasrv.module.lunaerp.service.business.shipment;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentItemDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentRollMapper;
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
 * {@link ShipmentRecycleBinHandler} 单元测试
 *
 * @author luna
 */
public class ShipmentRecycleBinHandlerTest {

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private ShipmentItemMapper shipmentItemMapper;

    @Mock
    private ShipmentRollMapper shipmentRollMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ShipmentRecycleBinHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long SHIPMENT_ID = 8001L;
    private static final Long CUSTOMER_ID = 110L;
    private static final Long PRODUCT_ID = 120L;

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
    @DisplayName("getBizType 返回 SHIPMENT")
    void testGetBizType() {
        assertEquals("SHIPMENT", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询草稿发货单：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("SHIPMENT");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("SHIPMENT");
            item.setBizId(SHIPMENT_ID);
            item.setDisplayName("SH-20261001-001");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(shipmentMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("SH-20261001-001", result.getList().get(0).getDisplayName());
        verify(shipmentMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));
    }

    @Test
    @DisplayName("恢复校验：草稿存在且客户与明细产品正常")
    void testValidateRestore_success() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(SHIPMENT_ID);
        shipment.setCustomerId(CUSTOMER_ID);

        ShipmentItemDO item = new ShipmentItemDO();
        item.setShipmentId(SHIPMENT_ID);
        item.setProductId(PRODUCT_ID);

        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("客户C");

        ProductDO product = new ProductDO();
        product.setId(PRODUCT_ID);
        product.setName("特种面料");

        when(shipmentMapper.selectDeletedById(SHIPMENT_ID, TENANT_ID_1)).thenReturn(shipment);
        when(shipmentItemMapper.selectDeletedListByShipmentId(SHIPMENT_ID, TENANT_ID_1)).thenReturn(List.of(item));

        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);

        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(product);

        assertDoesNotThrow(() -> handler.validateRestore(SHIPMENT_ID));
    }

    @Test
    @DisplayName("恢复校验：记录不存在或非草稿抛 1_030_031_001")
    void testValidateRestore_notExists_throws1030031001() {
        when(shipmentMapper.selectDeletedById(SHIPMENT_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(SHIPMENT_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：客户在回收站抛 1_030_031_002")
    void testValidateRestore_customerInRecycle_throws1030031002() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(SHIPMENT_ID);
        shipment.setCustomerId(CUSTOMER_ID);

        CustomerDO deletedCustomer = new CustomerDO();
        deletedCustomer.setId(CUSTOMER_ID);
        deletedCustomer.setCompanyName("已删除客户C");

        when(shipmentMapper.selectDeletedById(SHIPMENT_ID, TENANT_ID_1)).thenReturn(shipment);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(deletedCustomer);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(SHIPMENT_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除客户C"));
    }

    @Test
    @DisplayName("恢复校验：明细产品在回收站抛 1_030_031_002")
    void testValidateRestore_productInRecycle_throws1030031002() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(SHIPMENT_ID);

        ShipmentItemDO item = new ShipmentItemDO();
        item.setProductId(PRODUCT_ID);

        ProductDO deletedProduct = new ProductDO();
        deletedProduct.setId(PRODUCT_ID);
        deletedProduct.setName("已删除面料");

        when(shipmentMapper.selectDeletedById(SHIPMENT_ID, TENANT_ID_1)).thenReturn(shipment);
        when(shipmentItemMapper.selectDeletedListByShipmentId(SHIPMENT_ID, TENANT_ID_1)).thenReturn(List.of(item));
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(deletedProduct);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(SHIPMENT_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除面料"));
    }

    @Test
    @DisplayName("恢复成功：级联复活明细与码单卷清单")
    void testRestore_success_withCascade() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(SHIPMENT_ID);

        when(shipmentMapper.selectDeletedById(SHIPMENT_ID, TENANT_ID_1)).thenReturn(shipment);
        when(shipmentItemMapper.selectDeletedListByShipmentId(SHIPMENT_ID, TENANT_ID_1)).thenReturn(Collections.emptyList());
        when(shipmentMapper.restoreShipment(eq(SHIPMENT_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);
        when(shipmentItemMapper.restoreByShipmentId(eq(SHIPMENT_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(2);
        when(shipmentRollMapper.restoreByShipmentId(eq(SHIPMENT_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(5);

        assertDoesNotThrow(() -> handler.restore(SHIPMENT_ID));

        verify(shipmentMapper).restoreShipment(eq(SHIPMENT_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(shipmentItemMapper).restoreByShipmentId(eq(SHIPMENT_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(shipmentRollMapper).restoreByShipmentId(eq(SHIPMENT_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("彻底删除成功：先删码单卷与明细，再删发货单主表")
    void testPurge_success_withCascade() {
        ShipmentDO shipment = new ShipmentDO();
        shipment.setId(SHIPMENT_ID);

        when(shipmentMapper.selectDeletedById(SHIPMENT_ID, TENANT_ID_1)).thenReturn(shipment);
        when(shipmentMapper.selectActiveReferenceCount(SHIPMENT_ID, TENANT_ID_1)).thenReturn(0L);
        when(shipmentRollMapper.purgeByShipmentId(SHIPMENT_ID, TENANT_ID_1)).thenReturn(5);
        when(shipmentItemMapper.purgeByShipmentId(SHIPMENT_ID, TENANT_ID_1)).thenReturn(2);
        when(shipmentMapper.purgeShipment(SHIPMENT_ID, TENANT_ID_1)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(SHIPMENT_ID));

        verify(shipmentRollMapper).purgeByShipmentId(SHIPMENT_ID, TENANT_ID_1);
        verify(shipmentItemMapper).purgeByShipmentId(SHIPMENT_ID, TENANT_ID_1);
        verify(shipmentMapper).purgeShipment(SHIPMENT_ID, TENANT_ID_1);
    }

}
