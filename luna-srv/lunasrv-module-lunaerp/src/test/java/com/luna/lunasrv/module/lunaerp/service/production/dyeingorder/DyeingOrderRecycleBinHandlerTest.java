package com.luna.lunasrv.module.lunaerp.service.production.dyeingorder;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.luna.lunasrv.framework.common.exception.ServiceException;
import com.luna.lunasrv.framework.common.pojo.PageResult;
import com.luna.lunasrv.framework.tenant.core.context.TenantContextHolder;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecycleItemRespVO;
import com.luna.lunasrv.module.lunaerp.common.recycle.vo.RecyclePageReqVO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.customer.CustomerDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.baseinfo.supplier.SupplierDO;
import com.luna.lunasrv.module.lunaerp.dal.dataobject.production.dyeingorder.DyeingOrderDO;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.customer.CustomerMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.baseinfo.supplier.SupplierMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.DyeingOrderMapper;
import com.luna.lunasrv.module.lunaerp.dal.mysql.production.dyeingorder.color.DyeingColorMapper;
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
 * {@link DyeingOrderRecycleBinHandler} 单元测试
 *
 * @author luna
 */
public class DyeingOrderRecycleBinHandlerTest {

    @Mock
    private DyeingOrderMapper dyeingOrderMapper;

    @Mock
    private DyeingColorMapper dyeingColorMapper;

    @Mock
    private SupplierMapper supplierMapper;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private DyeingOrderRecycleBinHandler handler;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long ORDER_ID = 4001L;
    private static final Long FACTORY_ID = 500L;
    private static final Long CUSTOMER_ID = 600L;

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
    @DisplayName("getBizType 返回 DYEING_ORDER")
    void testGetBizType() {
        assertEquals("DYEING_ORDER", handler.getBizType());
    }

    @Test
    @DisplayName("分页查询草稿染色单：携带当前租户ID")
    void testPageDeleted_success() {
        RecyclePageReqVO req = new RecyclePageReqVO();
        req.setBizType("DYEING_ORDER");
        req.setPageNo(1);
        req.setPageSize(10);

        doAnswer(invocation -> {
            IPage<RecycleItemRespVO> page = invocation.getArgument(0);
            RecycleItemRespVO item = new RecycleItemRespVO();
            item.setBizType("DYEING_ORDER");
            item.setBizId(ORDER_ID);
            item.setDisplayName("RS20261001-001");
            page.setRecords(Collections.singletonList(item));
            page.setTotal(1L);
            return page;
        }).when(dyeingOrderMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));

        PageResult<RecycleItemRespVO> result = handler.pageDeleted(req);
        assertNotNull(result);
        assertEquals(1, result.getTotal());
        assertEquals("RS20261001-001", result.getList().get(0).getDisplayName());
        verify(dyeingOrderMapper).selectRecyclePage(any(IPage.class), eq(TENANT_ID_1));
    }

    @Test
    @DisplayName("恢复校验：草稿存在且染厂与客户正常")
    void testValidateRestore_success() {
        DyeingOrderDO order = new DyeingOrderDO();
        order.setId(ORDER_ID);
        order.setDyeingFactoryId(FACTORY_ID);
        order.setCustomerId(CUSTOMER_ID);

        SupplierDO factory = new SupplierDO();
        factory.setId(FACTORY_ID);
        factory.setCompanyName("染整一厂");

        CustomerDO customer = new CustomerDO();
        customer.setId(CUSTOMER_ID);
        customer.setCompanyName("客户B");

        when(dyeingOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(supplierMapper.selectDeletedById(FACTORY_ID, TENANT_ID_1)).thenReturn(null);
        when(supplierMapper.selectById(FACTORY_ID)).thenReturn(factory);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(null);
        when(customerMapper.selectById(CUSTOMER_ID)).thenReturn(customer);

        assertDoesNotThrow(() -> handler.validateRestore(ORDER_ID));
    }

    @Test
    @DisplayName("恢复校验：记录不存在或非草稿抛 1_030_031_001")
    void testValidateRestore_notExists_throws1030031001() {
        when(dyeingOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_RECORD_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    @DisplayName("恢复校验：染厂在回收站抛 1_030_031_002")
    void testValidateRestore_dyeingFactoryInRecycle_throws1030031002() {
        DyeingOrderDO order = new DyeingOrderDO();
        order.setId(ORDER_ID);
        order.setDyeingFactoryId(FACTORY_ID);

        SupplierDO deletedFactory = new SupplierDO();
        deletedFactory.setId(FACTORY_ID);
        deletedFactory.setCompanyName("已删除染厂");

        when(dyeingOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(supplierMapper.selectDeletedById(FACTORY_ID, TENANT_ID_1)).thenReturn(deletedFactory);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除染厂"));
    }

    @Test
    @DisplayName("恢复校验：客户在回收站抛 1_030_031_002")
    void testValidateRestore_customerInRecycle_throws1030031002() {
        DyeingOrderDO order = new DyeingOrderDO();
        order.setId(ORDER_ID);
        order.setCustomerId(CUSTOMER_ID);

        CustomerDO deletedCustomer = new CustomerDO();
        deletedCustomer.setId(CUSTOMER_ID);
        deletedCustomer.setCompanyName("已删除客户B");

        when(dyeingOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(customerMapper.selectDeletedById(CUSTOMER_ID, TENANT_ID_1)).thenReturn(deletedCustomer);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.validateRestore(ORDER_ID));
        assertEquals(RECYCLE_BIN_REFERENCE_IN_RECYCLE.getCode(), ex.getCode());
        assertTrue(ex.getMessage().contains("已删除客户B"));
    }

    @Test
    @DisplayName("恢复成功：级联复活颜色明细")
    void testRestore_success_withCascade() {
        DyeingOrderDO order = new DyeingOrderDO();
        order.setId(ORDER_ID);

        when(dyeingOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(dyeingOrderMapper.restoreDyeingOrder(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(1);
        when(dyeingColorMapper.restoreByDyeingOrderId(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class)))
                .thenReturn(2);

        assertDoesNotThrow(() -> handler.restore(ORDER_ID));

        verify(dyeingOrderMapper).restoreDyeingOrder(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
        verify(dyeingColorMapper).restoreByDyeingOrderId(eq(ORDER_ID), eq(TENANT_ID_1), any(), any(LocalDateTime.class));
    }

    @Test
    @DisplayName("彻底删除引用守卫：存在活跃引用抛 1_030_031_003")
    void testPurge_hasActiveReference_throws1030031003() {
        DyeingOrderDO order = new DyeingOrderDO();
        order.setId(ORDER_ID);

        when(dyeingOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(dyeingOrderMapper.selectActiveReferenceCount(ORDER_ID, TENANT_ID_1)).thenReturn(1L);

        ServiceException ex = assertThrows(ServiceException.class, () -> handler.purge(ORDER_ID));
        assertEquals(RECYCLE_BIN_HAS_ACTIVE_REFERENCE.getCode(), ex.getCode());

        verify(dyeingColorMapper, never()).purgeByDyeingOrderId(any(), any());
        verify(dyeingOrderMapper, never()).purgeDyeingOrder(any(), any());
    }

    @Test
    @DisplayName("彻底删除成功：先删颜色明细，再删染色主单")
    void testPurge_success_withCascade() {
        DyeingOrderDO order = new DyeingOrderDO();
        order.setId(ORDER_ID);

        when(dyeingOrderMapper.selectDeletedById(ORDER_ID, TENANT_ID_1)).thenReturn(order);
        when(dyeingOrderMapper.selectActiveReferenceCount(ORDER_ID, TENANT_ID_1)).thenReturn(0L);
        when(dyeingColorMapper.purgeByDyeingOrderId(ORDER_ID, TENANT_ID_1)).thenReturn(2);
        when(dyeingOrderMapper.purgeDyeingOrder(ORDER_ID, TENANT_ID_1)).thenReturn(1);

        assertDoesNotThrow(() -> handler.purge(ORDER_ID));

        verify(dyeingColorMapper).purgeByDyeingOrderId(ORDER_ID, TENANT_ID_1);
        verify(dyeingOrderMapper).purgeDyeingOrder(ORDER_ID, TENANT_ID_1);
    }

}
