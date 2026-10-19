package com.luna.lunasrv.module.lunaerp.service.business.salesreturn;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.product.ProductDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.salesreturn.SalesReturnItemDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.business.shipment.ShipmentDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.stock.warehouse.WarehouseDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.product.ProductMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnItemMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.salesreturn.SalesReturnRollMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.business.shipment.ShipmentMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.stock.warehouse.WarehouseMapper;
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
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.PRODUCT_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;
import static com.luna.lunasrv.module.lunaerp.enums.business.salesreturn.SalesReturnErrorCodeConstants.SALES_RETURN_SHIPMENT_NOT_COMPLETED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link SalesReturnRecycleBinHandler} 单元测试
 *
 * @author luna
 */
@DisplayName("业务管理 - 销售退货草稿回收站业务处理器测试")
public class SalesReturnRecycleBinHandlerTest {

    @Mock
    private SalesReturnMapper salesReturnMapper;

    @Mock
    private SalesReturnItemMapper salesReturnItemMapper;

    @Mock
    private SalesReturnRollMapper salesReturnRollMapper;

    @Mock
    private CustomerMapper customerMapper;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ShipmentMapper shipmentMapper;

    @Mock
    private WarehouseMapper warehouseMapper;

    @InjectMocks
    private SalesReturnRecycleBinHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long RETURN_ID = 9001L;
    private static final Long SHIPMENT_ID = 8001L;
    private static final Long CUSTOMER_ID = 110L;
    private static final Long WAREHOUSE_ID = 210L;
    private static final Long PRODUCT_ID = 310L;

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
    @DisplayName("getBizType 返回 SALES_RETURN")
    void testGetBizType() {
        assertEquals("SALES_RETURN", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询草稿退货单：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("SALES_RETURN");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("SALES_RETURN");
            item.setBizId(RETURN_ID);
            item.setDisplayName("RT-20261018-001");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(salesReturnMapper).selectRecyclePage(any(), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals(RETURN_ID, result.getList().get(0).getBizId());
        assertEquals("RT-20261018-001", result.getList().get(0).getDisplayName());
    }

    @Test
    @DisplayName("validateRestore: 目标记录在回收站中不存在时抛出异常")
    void testValidateRestore_notExists() {
        when(salesReturnMapper.selectDeletedById(RETURN_ID, TENANT_ID_1)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(RETURN_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateRestore: 关联客户在回收站中阻断恢复")
    void testValidateRestore_customerInRecycle() {
        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(RETURN_ID);
        ret.setCustomerId(CUSTOMER_ID);
        ret.setWarehouseId(WAREHOUSE_ID);
        ret.setShipmentId(SHIPMENT_ID);
        when(salesReturnMapper.selectDeletedById(RETURN_ID, TENANT_ID_1)).thenReturn(ret);

        CustomerDO delCust = new CustomerDO();
        delCust.setId(CUSTOMER_ID);
        delCust.setCompanyName("已删客户A");
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(delCust);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(RETURN_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateRestore: 关联客户已不存在阻断恢复")
    void testValidateRestore_customerNotFound() {
        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(RETURN_ID);
        ret.setCustomerId(CUSTOMER_ID);
        ret.setWarehouseId(WAREHOUSE_ID);
        ret.setShipmentId(SHIPMENT_ID);
        when(salesReturnMapper.selectDeletedById(RETURN_ID, TENANT_ID_1)).thenReturn(ret);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(RETURN_ID));
        assertEquals(CUSTOMER_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateRestore: 退货仓库不存在阻断恢复")
    void testValidateRestore_warehouseNotFound() {
        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(RETURN_ID);
        ret.setCustomerId(CUSTOMER_ID);
        ret.setWarehouseId(WAREHOUSE_ID);
        ret.setShipmentId(SHIPMENT_ID);
        when(salesReturnMapper.selectDeletedById(RETURN_ID, TENANT_ID_1)).thenReturn(ret);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(new CustomerDO());
        when(warehouseMapper.selectById(WAREHOUSE_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(RETURN_ID));
        assertEquals(WAREHOUSE_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateRestore: 关联发货单在回收站中阻断恢复")
    void testValidateRestore_shipmentInRecycle() {
        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(RETURN_ID);
        ret.setCustomerId(CUSTOMER_ID);
        ret.setWarehouseId(WAREHOUSE_ID);
        ret.setShipmentId(SHIPMENT_ID);
        when(salesReturnMapper.selectDeletedById(RETURN_ID, TENANT_ID_1)).thenReturn(ret);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(new CustomerDO());
        when(warehouseMapper.selectById(WAREHOUSE_ID)).thenReturn(new WarehouseDO());

        ShipmentDO delShipment = new ShipmentDO();
        delShipment.setId(SHIPMENT_ID);
        delShipment.setShipmentNo("SH-20261001-001");
        when(shipmentMapper.selectDeletedById(SHIPMENT_ID, TENANT_ID_1)).thenReturn(delShipment);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(RETURN_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateRestore: 关联发货单非 COMPLETED 状态阻断恢复")
    void testValidateRestore_shipmentNotCompleted() {
        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(RETURN_ID);
        ret.setCustomerId(CUSTOMER_ID);
        ret.setWarehouseId(WAREHOUSE_ID);
        ret.setShipmentId(SHIPMENT_ID);
        when(salesReturnMapper.selectDeletedById(RETURN_ID, TENANT_ID_1)).thenReturn(ret);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(new CustomerDO());
        when(warehouseMapper.selectById(WAREHOUSE_ID)).thenReturn(new WarehouseDO());

        when(shipmentMapper.selectDeletedById(SHIPMENT_ID, TENANT_ID_1)).thenReturn(null);
        ShipmentDO activeShipment = new ShipmentDO();
        activeShipment.setId(SHIPMENT_ID);
        activeShipment.setStatus("CANCELLED");
        when(shipmentMapper.selectById(SHIPMENT_ID)).thenReturn(activeShipment);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(RETURN_ID));
        assertEquals(SALES_RETURN_SHIPMENT_NOT_COMPLETED.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateRestore: 明细中产品在回收站中阻断恢复")
    void testValidateRestore_productInRecycle() {
        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(RETURN_ID);
        ret.setCustomerId(CUSTOMER_ID);
        ret.setWarehouseId(WAREHOUSE_ID);
        ret.setShipmentId(SHIPMENT_ID);
        when(salesReturnMapper.selectDeletedById(RETURN_ID, TENANT_ID_1)).thenReturn(ret);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(new CustomerDO());
        when(warehouseMapper.selectById(WAREHOUSE_ID)).thenReturn(new WarehouseDO());

        when(shipmentMapper.selectDeletedById(SHIPMENT_ID, TENANT_ID_1)).thenReturn(null);
        ShipmentDO activeShipment = new ShipmentDO();
        activeShipment.setId(SHIPMENT_ID);
        activeShipment.setStatus("COMPLETED");
        when(shipmentMapper.selectById(SHIPMENT_ID)).thenReturn(activeShipment);

        SalesReturnItemDO item = new SalesReturnItemDO();
        item.setId(101L);
        item.setProductId(PRODUCT_ID);
        when(salesReturnItemMapper.selectDeletedListByReturnId(RETURN_ID, TENANT_ID_1))
                .thenReturn(Collections.singletonList(item));

        ProductDO delProduct = new ProductDO();
        delProduct.setId(PRODUCT_ID);
        delProduct.setName("已删产品A");
        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(delProduct);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(RETURN_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("validateRestore: 明细中产品已不存在阻断恢复")
    void testValidateRestore_productNotFound() {
        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(RETURN_ID);
        ret.setCustomerId(CUSTOMER_ID);
        ret.setWarehouseId(WAREHOUSE_ID);
        ret.setShipmentId(SHIPMENT_ID);
        when(salesReturnMapper.selectDeletedById(RETURN_ID, TENANT_ID_1)).thenReturn(ret);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(new CustomerDO());
        when(warehouseMapper.selectById(WAREHOUSE_ID)).thenReturn(new WarehouseDO());

        when(shipmentMapper.selectDeletedById(SHIPMENT_ID, TENANT_ID_1)).thenReturn(null);
        ShipmentDO activeShipment = new ShipmentDO();
        activeShipment.setId(SHIPMENT_ID);
        activeShipment.setStatus("COMPLETED");
        when(shipmentMapper.selectById(SHIPMENT_ID)).thenReturn(activeShipment);

        SalesReturnItemDO item = new SalesReturnItemDO();
        item.setId(101L);
        item.setProductId(PRODUCT_ID);
        when(salesReturnItemMapper.selectDeletedListByReturnId(RETURN_ID, TENANT_ID_1))
                .thenReturn(Collections.singletonList(item));

        when(productMapper.selectDeletedById(PRODUCT_ID, TENANT_ID_1)).thenReturn(null);
        when(productMapper.selectById(PRODUCT_ID)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(RETURN_ID));
        assertEquals(PRODUCT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("restore: 正常恢复，父到子级联复活")
    void testRestore_success() {
        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(RETURN_ID);
        ret.setCustomerId(CUSTOMER_ID);
        ret.setWarehouseId(WAREHOUSE_ID);
        ret.setShipmentId(SHIPMENT_ID);
        when(salesReturnMapper.selectDeletedById(RETURN_ID, TENANT_ID_1)).thenReturn(ret);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(new CustomerDO());
        when(warehouseMapper.selectById(WAREHOUSE_ID)).thenReturn(new WarehouseDO());
        when(shipmentMapper.selectDeletedById(SHIPMENT_ID, TENANT_ID_1)).thenReturn(null);
        ShipmentDO activeShipment = new ShipmentDO();
        activeShipment.setStatus("COMPLETED");
        when(shipmentMapper.selectById(SHIPMENT_ID)).thenReturn(activeShipment);
        when(salesReturnItemMapper.selectDeletedListByReturnId(RETURN_ID, TENANT_ID_1))
                .thenReturn(Collections.emptyList());

        when(salesReturnMapper.restoreSalesReturn(eq(RETURN_ID), eq(TENANT_ID_1), any(), any()))
                .thenReturn(1);

        handler.restore(RETURN_ID);

        verify(salesReturnMapper).restoreSalesReturn(eq(RETURN_ID), eq(TENANT_ID_1), any(), any());
        verify(salesReturnItemMapper).restoreByReturnId(eq(RETURN_ID), eq(TENANT_ID_1), any(), any());
        verify(salesReturnRollMapper).restoreByReturnId(eq(RETURN_ID), eq(TENANT_ID_1), any(), any());
    }

    @Test
    @DisplayName("restore: 并发更新影响行数为 0 抛出异常")
    void testRestore_affectedZero() {
        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(RETURN_ID);
        ret.setCustomerId(CUSTOMER_ID);
        ret.setWarehouseId(WAREHOUSE_ID);
        ret.setShipmentId(SHIPMENT_ID);
        when(salesReturnMapper.selectDeletedById(RETURN_ID, TENANT_ID_1)).thenReturn(ret);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(new CustomerDO());
        when(warehouseMapper.selectById(WAREHOUSE_ID)).thenReturn(new WarehouseDO());
        when(shipmentMapper.selectDeletedById(SHIPMENT_ID, TENANT_ID_1)).thenReturn(null);
        ShipmentDO activeShipment = new ShipmentDO();
        activeShipment.setStatus("COMPLETED");
        when(shipmentMapper.selectById(SHIPMENT_ID)).thenReturn(activeShipment);
        when(salesReturnItemMapper.selectDeletedListByReturnId(RETURN_ID, TENANT_ID_1))
                .thenReturn(Collections.emptyList());

        when(salesReturnMapper.restoreSalesReturn(eq(RETURN_ID), eq(TENANT_ID_1), any(), any()))
                .thenReturn(0);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.restore(RETURN_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("hasActiveReference: 恒返回 false")
    void testHasActiveReference() {
        assertFalse(handler.hasActiveReference(RETURN_ID));
    }

    @Test
    @DisplayName("purge: 彻底物理删除，子到父级联删除")
    void testPurge_success() {
        SalesReturnDO ret = new SalesReturnDO();
        ret.setId(RETURN_ID);
        when(salesReturnMapper.selectDeletedById(RETURN_ID, TENANT_ID_1)).thenReturn(ret);
        when(salesReturnMapper.purgeSalesReturn(RETURN_ID, TENANT_ID_1)).thenReturn(1);

        handler.purge(RETURN_ID);

        verify(salesReturnRollMapper).purgeByReturnId(RETURN_ID, TENANT_ID_1);
        verify(salesReturnItemMapper).purgeByReturnId(RETURN_ID, TENANT_ID_1);
        verify(salesReturnMapper).purgeSalesReturn(RETURN_ID, TENANT_ID_1);
    }

    @Test
    @DisplayName("purge: 目标记录不存在时抛出异常")
    void testPurge_notExists() {
        when(salesReturnMapper.selectDeletedById(RETURN_ID, TENANT_ID_1)).thenReturn(null);
        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(RETURN_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

}
